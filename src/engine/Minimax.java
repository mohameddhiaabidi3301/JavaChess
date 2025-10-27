package engine;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.IntUnaryOperator;

import debug.DebugRender;
import main.Main;

public class Minimax {
	public static final byte FLAG_EXACT = 0; // No cutoffs were performed
	public static final byte FLAG_LOWERBOUND = 1; // (beta cutoff), cache value must be >= beta
	public static final byte FLAG_UPPERBOUND = 2; // (alpha cutoff), cache value must be <= alpha
	public static final byte FLAG_TIMEOUT = 3; // Used if the search timed out
	
	public static final int MATE_SCORE = 999_999;
	public static final int DRAW_SCORE = 0;
	
	public static final int MAX_QUIESCENCE_PLY = 10;
	
	private static final int[] valueMap = EvaluateBoard.valueMap;
	static int[][] killerMoves = new int[128][2]; // Moves that caused beta cutoff
	static int[][] counterHeuristic = new int[64][64]; // The last best replies to each move
	static int[][][] historyHeuristic = new int[2][64][64];
	static int[] pvLine = new int[128];
	
	public static int getComputerMove(int minDepth, int maxMs) {
		long startTime = System.nanoTime();
		
		int move = iterativeDeepening(minDepth, maxMs);
		
		long endTime = System.nanoTime();
		int millisecondsEllapsed = (int)((endTime - startTime) / 1_000_000);
		
		System.out.println("Took: " + (millisecondsEllapsed) + " milliseconds to compute move");
		
		return move;
	}
	
	private static int moveScoreHeuristic(int move, int ply) {
		int score = 0;
		byte to = (byte)((move >>> 6) & 0x3F);
		byte captureType = (byte) ((move >>> 16) & 0xF);
		byte fromType = (byte)((move >>> 12) & 0xF);
		byte color = (byte)((move >>> 31) & 1);
		
		if (pvLine[ply] == move) score += 99_999;
		if (captureType != 0) {
			score += 10_000 + (valueMap[captureType] - valueMap[fromType]);
			if ((Main.globalPosition.psuedoAttacks[1 - color] & (1L << to)) == 0) {
				score += 5_000; // Good undefended capture
			}
		};
		if (killerMoves[ply][0] == move || killerMoves[ply][1] == move) {
			score += 10_000;
		}
		
		return score;
	}
	
	public static void sortByScore(int[] arr, int ply) {
	    int n = arr.length;
	    int[] scores = new int[n];

	    // Step 1: Precompute scores
	    for (int i = 0; i < n; i++) {
	        scores[i] = moveScoreHeuristic(arr[i], ply);
	    }
	    
	    // Step 2: In-place insertion sort (descending by score)
	    for (int i = 1; i < n; i++) {
	        int key = arr[i];
	        int keyScore = scores[i];
	        int j = i - 1;

	        while (j >= 0 && scores[j] < keyScore) {
	            arr[j + 1] = arr[j];
	            scores[j + 1] = scores[j];
	            j--;
	        }
	        arr[j + 1] = key;
	        scores[j + 1] = keyScore;
	    }
	}
	
	private static final int INFINITY = 2_000_000;
	
	// minDepth takes priority, if there is extra time it will run until maxTime (ms)
	private static int iterativeDeepening(int minDepth, int maxTime) {
		long startTime = System.nanoTime();
		
		int previousBestMove = -1;
		for (byte currentDepth = 0; currentDepth >= 0; currentDepth++) {
			long timeElapsedMs = (System.nanoTime() - startTime) / 1_000_000;
			
			if (currentDepth > minDepth && timeElapsedMs >= maxTime - 1) {
				break;
			}
			
			TT.nextRootSearch();
			int negascore = negamax(currentDepth, -INFINITY, INFINITY, 0);
			int[] bestMoveInfo = TT.probe(Main.globalPosition.zobristHash, -INFINITY, INFINITY, currentDepth, currentDepth);
			
			int newMove = -1;
			int score = 0;
			
			if (bestMoveInfo != null) {
				newMove = bestMoveInfo[1];
				score = bestMoveInfo[0];
			}
			
			previousBestMove = newMove;
			System.out.println("(" + ((System.nanoTime() - startTime) / 1_000_000) + " ms)" + "Move at depth " + currentDepth + ": " + Position.logMove(previousBestMove) + ": " + score);
			
			TT.set(Main.globalPosition.zobristHash, currentDepth, negascore, newMove, FLAG_EXACT, true);
			if (currentDepth > 50) break;
		}
		
		return previousBestMove;
	}
	
	static long[] repetitionHistory = new long[256];
	static int historyPly = 0;
	
	private static boolean isThreefoldRepetition() {
		int count = 0;
		for (int i = 0; i < historyPly; i++) {
			if (repetitionHistory[i] == Main.globalPosition.zobristHash) {
				count++;
			}
			
			if (count >= 3) return true;
		}
		return false;
	}
	
	private final static int DELTA_PRUNING_MARGIN = 200;
	public static int quiescence(int alpha, int beta, int ply, int localPly) {
		byte sideToMove = Main.globalPosition.sideToMove;
		int standPat = Main.globalPosition.getEval(sideToMove);
		
		if (localPly >= 10) {
			return Main.globalPosition.getEval(sideToMove);
		}
		
		if (standPat >= beta) {
			return beta;
		} else if (standPat > alpha) {
			alpha = standPat;
		}
		
		boolean inCheck = (Main.globalPosition.psuedoAttacks[1 - sideToMove] & (1L << (sideToMove == 0 ? Main.globalPosition.whiteKingPos : Main.globalPosition.blackKingPos))) != 0;
		int[] moves = inCheck ? Main.globalPosition.getAllLegalMoves(sideToMove) :
			Main.globalPosition.getCapturesChecksPromotions(sideToMove);
		
		if (moves.length == 0) {
			if (inCheck) {
				return -MATE_SCORE + ply;
			} else {
				return standPat;
			}
		}
		
		int bestScore = -INFINITY;
		for (int move : moves) {
			byte promotionFlag = (byte)((move >>> 27) & 1);
			byte captureType = (byte)((move >>> 16) & 0xF);
			
			if (promotionFlag == 0 && captureType != 0 && !inCheck && standPat + valueMap[captureType] + DELTA_PRUNING_MARGIN < alpha) {
				continue;
			}
			
			if (promotionFlag != 0) move |= (sideToMove == 0 ? 5 : 11) << 22;
			
			Main.globalPosition.makeMove(move, true);
			int eval = -quiescence(-beta, -alpha, ply + 1, localPly + 1);
			Main.globalPosition.unmakeMove(move);
			
			if (eval > bestScore) {
				bestScore = eval;
				alpha = Math.max(eval, alpha);
			}
			
			if (alpha >= beta) {
				break;
			}
		}

		return bestScore;
	}
	
	// ALPHA - Lowerbound, Best Score we've found so far so any new score needs to be higher
	// BETA - Upperbound, Since the opponent wants to minimize, this is the lowest score the opponent found
	// They wont allow us to play it since its too good, needs to be lower than upperbound
	// Score: alpha <= real_score <= beta, if alpha >= beta bounds collapse so prune, wont go down the route
	public static int negamax(byte depth, int alpha, int beta, int ply) {
		byte sideToMove = Main.globalPosition.sideToMove;
		
		int[] moves = Main.globalPosition.getAllLegalMoves(sideToMove);
		int bestEval = -INFINITY;
		int bestMove = -1;
		
		int[] knownScore = TT.probe(Main.globalPosition.zobristHash, alpha, beta, depth, ply);
		if (knownScore != null) {
			pvLine[ply] = knownScore[1];
			return knownScore[0];
		}
		
		boolean inCheck = (Main.globalPosition.psuedoAttacks[1 - sideToMove] & (1L << (sideToMove == 0 ? Main.globalPosition.whiteKingPos : Main.globalPosition.blackKingPos))) != 0;
		if (moves.length == 0) {
			if (inCheck) {
				return -MATE_SCORE + ply;
			} else {
				return 0;
			}
		}
		
		if (isThreefoldRepetition()) {
			return 0;
		}
		
		if (depth <= 0) {
			return quiescence(alpha, beta, ply, 0);
		}
		
		int R = 2;
		if (ply > 0 && depth > R + 1 && !inCheck && Main.globalPosition.phase >= 7) {
			Main.globalPosition.toggleNullMove();
			int score = -negamax((byte)(depth - 1 - R), -beta, -alpha, ply + 1);
			Main.globalPosition.toggleNullMove();
			
			if (score >= beta) {
				return beta;
			}
		}
		
		sortByScore(moves, ply);
		int originalAlpha = alpha;
		byte index = 0;
		for (int move : moves) {
			byte promotionFlag = (byte)((move >>> 27) & 1);
			if (promotionFlag != 0) move |= (sideToMove == 0 ? 5 : 11) << 22;
			
			int score;
			Main.globalPosition.makeMove(move, true);
			
			if (pvLine[ply] != move && depth >= 3 && index >= 5 && ((move >>> 16) & 0xF) == 0 && promotionFlag == 0 && !inCheck) {
				int reduce = moves.length > 7 ? 2 : 1;
				score = -negamax((byte)(depth - 1 - reduce), -beta, -alpha, ply + 1);
				
				if (score > alpha) {
					score = -negamax((byte)(depth - 1), -beta, -alpha, ply + 1);
				}
			} else {
				score = -negamax((byte)(depth - 1), -beta, -alpha, ply + 1); // Swap & Negate since the bounds flip and score perspective flips
			}
			
			Main.globalPosition.unmakeMove(move);
			
			if (score > bestEval) {
				bestEval = score;
				bestMove = move;
				
				pvLine[ply] = move;
				//System.arraycopy(pvLine, ply + 1, pvLine, ply + 1, pvLine.length - (ply + 1));
				
				alpha = Math.max(alpha, bestEval);
			}
			
			if (alpha >= beta) {
				boolean isCapture = ((move >>> 16) & 0xF) != 0;
				
				if (!isCapture && promotionFlag == 0 && killerMoves[ply][0] != move) {
					killerMoves[ply][1] = killerMoves[ply][0];
					killerMoves[ply][0] = move;
				}
				
				break;
			}
			
			index++;
		}
		
		byte flag = FLAG_EXACT;
		if (bestEval <= originalAlpha) {
			flag = FLAG_UPPERBOUND;
		} else if (bestEval >= beta) {
			flag = FLAG_LOWERBOUND;
		}
		
		TT.set(Main.globalPosition.zobristHash, depth, bestEval, bestMove, flag, false);
		return bestEval;
	}
	
 	public static int[] runPerft(int depth, int ply, boolean isWhite, int previousMove) {
		int[] moves = isWhite ? Main.globalPosition.getAllLegalMoves((byte)0) : Main.globalPosition.getAllLegalMoves((byte)1);
		if (depth == 0) {
			byte targetKey = (byte)((previousMove >>> 16) & 0xF);
			
			boolean isCapture = targetKey != 0;
			boolean isEnPassant = ((byte)(previousMove >>> 29) & 1L) != 0;
			boolean isCastle = ((byte)(previousMove >>> 28) & 1L) != 0;
			byte promotionKey = (byte)((previousMove >>> 22) & 0xF);
			boolean isPromotion = promotionKey != 0;
			
			return new int[] {1, isCapture ? 1 : 0, isEnPassant ? 1 : 0, isCastle ? 1 : 0, isPromotion ? 1 : 0, Main.globalPosition.moveCausesCheck(previousMove) ? 1 : 0};
		}
		
		// Captures, Enpassants, Castles, Promotions, Checks
		int[] totalMoveCount = {0, 0, 0, 0, 0, 0};
		for (int move : moves) {
			// Make move
			Main.globalPosition.makeMove(move, true);
			int[] count = runPerft(depth - 1, ply + 1, !isWhite, move);
			Main.globalPosition.unmakeMove(move);
			
			for (int i = 0; i < totalMoveCount.length; i++) {
				totalMoveCount[i] += count[i];
			}
		}
		
		return totalMoveCount;
	}
}


