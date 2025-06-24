package engine;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.function.IntUnaryOperator;

import debug.DebugRender;

public class Minimax {
	public static int[] getComputerMove(int minDepth, int maxMs, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		int[] move = iterativeDeepening(minDepth, maxMs, false);
		
		long endTime = System.nanoTime();
		int millisecondsEllapsed = (int)((endTime - startTime) / 1_000_000);
		
		System.out.println("Took: " + (millisecondsEllapsed) + " milliseconds to compute move");
		
		return move;
	}
	
	private static final int[] valueMap = EvaluateBoard.valueMap;
	private static int moveScoreHeuristic(int move) {
		byte to = (byte)((move >>> 6) & 0x3F);
		byte pieceType = (byte)((move >>> 12) & 0xF);
		byte captureType = (byte)((move >>> 16) & 0xF);
		byte color = (byte)((move >>> 31) & 1);
		
		int ourValue = valueMap[pieceType];
		int captureValue = valueMap[captureType];
		int score = 0;
		
		if (captureValue > ourValue) {
			score += captureValue - ourValue;
		}
		
		int[] attackers = Position.attacks[1 - color][to];
		int[] defenders = Position.attacks[color][to];
		
		if (Position.attacks[color][to] == null && Position.attacks[1 - color][to] != null) {
			score -= ourValue; // Hanged Piece
		}
		
		if (attackers != null && defenders != null) {
			if (attackers.length > defenders.length) {
				score -= ourValue;
			}
		}
		
		return score;
	}
	
	public static void sortByScore(int[] arr, int[] previousBestMove, int depth, int ply) {
	    int n = arr.length;
	    int[] scores = new int[n];

	    // Step 1: Precompute scores
	    for (int i = 0; i < n; i++) {
	        scores[i] = moveScoreHeuristic(arr[i]);
	        if (previousBestMove != null && previousBestMove[0] == arr[i]) scores[i] = 1000;
	        else if (killerMoves[ply][0] == arr[i] || killerMoves[ply][1] == arr[i]) scores[i] = 900;
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
	
	private static final int FLAG_EXACT = 0;
	private static final int FLAG_LOWERBOUND = 1; // (beta cutoff), cache value must be >= beta
	private static final int FLAG_UPPERBOUND = 2; // (alpha cutoff), cache value must be <= alpha
	private static final int FLAG_TIMEOUT = 3;
	
	// minDepth takes priority, if there is extra time it will run until maxTime (ms)
	private static int[] iterativeDeepening(int minDepth, int maxTime, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		// Clear killer moves
		Arrays.stream(killerMoves).forEach(km -> Arrays.fill(km, -1));

		int[] previousBestMove = null;
		for (int currentDepth = 0; currentDepth >= 0; currentDepth++) {
			long timeElapsedMs = (System.nanoTime() - startTime) / 1_000_000;
			
			if (currentDepth > minDepth && timeElapsedMs > maxTime) {
				break;
			}
			
			int[] newMove = minimax(currentDepth, isMaximizing, Integer.MIN_VALUE, Integer.MAX_VALUE, previousBestMove, startTime, maxTime, 0, 0);
			
			if (newMove[2] == FLAG_TIMEOUT) {
				System.out.println("Timeout on move: " + Arrays.toString(newMove) + " (" + currentDepth + ")");
				break;
			}
			
			previousBestMove = newMove;			
			System.out.println("(" + ((System.nanoTime() - startTime) / 1_000_000) + " ms)" + "Move at depth " + currentDepth + ": " + Position.logMove(previousBestMove[0]) + ": " + Arrays.toString(previousBestMove));
			
		}
		
		return previousBestMove;
	}
	
	private static final int TT_SIZE = 1 << 22;
	private static long[] zobristKeys = new long[TT_SIZE];
	private static int[][] zobristValues = new int[TT_SIZE][4];
	
	private static int[][] killerMoves = new int[128][2];
			
	private static int[] minimax(int depth, boolean isMaximizing, int alpha, int beta, int[] previousBestMove, long itdStartTime, int maxTime, int quiessenceCount, int ply) {
		if (depth == 0) {
			return new int[] {-1, EvaluateBoard.getEval(), -1, depth};
		}
		
		int TT_INDEX = (int)(ZobristHash.hash & (TT_SIZE - 1));
		if (zobristKeys[TT_INDEX] == ZobristHash.hash) {
			int[] data = Arrays.copyOf(zobristValues[TT_INDEX], 4);
			int flag = data[2];
			int cacheScore = data[1];
			int cacheDepth = data[3];
			
			if (depth <= cacheDepth) {
				if (flag == FLAG_EXACT) {
					return data;
				} else if (flag == FLAG_LOWERBOUND && cacheScore >= beta) {
					return data;
				} else if (flag == FLAG_UPPERBOUND && cacheScore <= alpha) {
					return data;
				}
			}
		}
		
		boolean inEndGame = (Long.bitCount(Position.whiteOccupied) + Long.bitCount(Position.blackOccupied)) <= 14;
		boolean usInCheck = (isMaximizing ? Position.attacks[1][Position.whiteKingPos] != null : Position.attacks[0][Position.blackKingPos] != null);
	
		if (depth >= 3 && quiessenceCount == 0 && !usInCheck && !inEndGame) {
			int R = 2; // Depth Reduction
			Position.toggleNullMove(); // Symmetric
			
			int[] nullScore = minimax(depth - R - 1, !isMaximizing, -beta, -beta + 1, null, itdStartTime, maxTime, quiessenceCount, ply + 1);
			Position.toggleNullMove();
			
			if (nullScore[1] >= beta) {
				return new int[] {-1, beta, FLAG_LOWERBOUND, depth}; // Beta cutoff
			}
		}
		
		int[] moves = (isMaximizing) ? Position.getAllLegalMoves((byte)0) : Position.getAllLegalMoves((byte)1);
		int[] bestMove = new int[] {-1, (isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE), -1, depth};
		sortByScore(moves, previousBestMove, depth, ply); // Order the PBM first because its likely good
		
		if (moves.length == 0) {
			if (usInCheck) {
				return new int[] {-1, isMaximizing ? -100_000 + ply : 100_000 - ply, -1, depth};
			} else {
				return new int[] {-1, 0, -1, depth};
			}
		}
		
		int cutoffFlag = FLAG_EXACT; // Default
		for (int i = 0; i < moves.length; i++) {
			int move = moves[i];
			byte to = (byte)((move >>> 6) & 0x3F);
			boolean isCapture = ((move >>> 16) & 0xF) != 0;
			boolean isPromotion = ((move >>> 27) & 1) != 0;
			//boolean isHighValuePiece = (((move >>> 12) & 0xF) % 6) >= 4;
			
			boolean extendQuiessence = ((isCapture || isPromotion) && quiessenceCount < 4);
			int nextDepth = (extendQuiessence) ? depth : depth - 1;
			int nextQuiessence = (extendQuiessence) ? quiessenceCount + 1 : quiessenceCount;
			
			if ((System.nanoTime() - itdStartTime) / 1_000_000 > maxTime) {
				if (bestMove[0] == -1) {
			        return new int[] {-1, 0, FLAG_TIMEOUT, depth};
			    }
				
				bestMove[2] = FLAG_TIMEOUT;
				return bestMove;
			}
			
			if (!isPromotion) {
				Position.makeMove(move, true);
				
				int[] score = minimax(nextDepth, !isMaximizing, alpha, beta, null, itdStartTime, maxTime, nextQuiessence, ply + 1);
				Position.unmakeMove(move);
				
				if (isMaximizing) {
					 if (score[1] > bestMove[1]) {
						 bestMove[1] = score[1];
						 bestMove[0] = move;
						 alpha = Math.max(alpha, score[1]);
					 }
				} else {
					if (score[1] < bestMove[1]) {
						bestMove[1] = score[1];
						bestMove[0] = move;
						beta = Math.min(beta, score[1]);
					}
				}
				
				if (beta <= alpha) {
					cutoffFlag = (isMaximizing) ? FLAG_LOWERBOUND : FLAG_UPPERBOUND;
					
					if (!isCapture && cutoffFlag == FLAG_LOWERBOUND) { // Beta cutoff on non-capture
						if (killerMoves[ply][0] != move) {
					        killerMoves[ply][1] = killerMoves[ply][0];
					        killerMoves[ply][0] = move;
					    }
					}
					
					break;
				};
			} else {
				byte[] pkKeys = (isMaximizing ? Position.whitePromotions : Position.blackPromotions);
				
				for (byte key : pkKeys) {
					int promotionMove = (move & ~(0xF << 22)) | (key << 22);
					
					Position.makeMove(promotionMove, true);
					
					int[] score = minimax(nextDepth, !isMaximizing, alpha, beta, null, itdStartTime, maxTime, nextQuiessence, ply + 1);
					Position.unmakeMove(promotionMove);
					
					if (isMaximizing) {
						 if (score[1] > bestMove[1]) {
							 bestMove[1] = score[1];
							 bestMove[0] = promotionMove;
							 alpha = Math.max(alpha, score[1]);
						 }
					} else {
						if (score[1] < bestMove[1]) {
							bestMove[1] = score[1];
							bestMove[0] = promotionMove;
							beta = Math.min(beta, score[1]);
						}
					}
					
					if (beta <= alpha) {
						cutoffFlag = (isMaximizing) ? FLAG_LOWERBOUND : FLAG_UPPERBOUND;
						break;
					};
				}
			}
		}
		
		bestMove[2] = cutoffFlag;
		
		if (bestMove[2] != FLAG_TIMEOUT) {
			if (zobristKeys[TT_INDEX] == ZobristHash.hash) {
				if (zobristValues[TT_INDEX][3] <= depth) {
					zobristValues[TT_INDEX] = bestMove;
				}
			} else {
				zobristKeys[TT_INDEX] = ZobristHash.hash;
				zobristValues[TT_INDEX] = bestMove;
			}
		}
		
		return bestMove;
	}
}