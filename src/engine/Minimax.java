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
	public static int[] getComputerMove(int minDepth, int maxMs, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		int[] move = iterativeDeepening(minDepth, maxMs, false);
		
		long endTime = System.nanoTime();
		int millisecondsEllapsed = (int)((endTime - startTime) / 1_000_000);
		
		System.out.println("Took: " + (millisecondsEllapsed) + " milliseconds to compute move");
		
		return move;
	}
	
	private static final int[] valueMap = EvaluateBoard.valueMap;
	static int[][] killerMoves = new int[128][2];
	
	private static void sortLVA(int[] arr) {
		int i = 1;
		
		while (i < arr.length) {
			int j = i;
			while (j > 0 && (valueMap[Main.globalPosition.engineLookup[arr[i-1]]] > valueMap[Main.globalPosition.engineLookup[arr[i]]])) {
				int temp = arr[j];
				arr[j] = arr[j-1];
				arr[j-1] = temp;
				
				j--;
			}
			i++;
		}
	}
	
	private static int SEE(int origin, int targetSquare, byte startingColor) {
		int[] ourAttackers = Main.globalPosition.attacks[startingColor][targetSquare];
		int[] opponentAttackers = Main.globalPosition.attacks[1 - startingColor][targetSquare];
		sortLVA(ourAttackers);
		sortLVA(opponentAttackers);
		
		byte currentColor = (byte)(1 - startingColor);
		int score = 0;
		int usedMy = 0;
		int usedOpponents = 0;
		
		while (usedMy < ourAttackers.length && usedOpponents < opponentAttackers.length) {
			if (currentColor == startingColor) {
				score += opponentAttackers[valueMap[Main.globalPosition.engineLookup[usedOpponents]]];
				
				usedOpponents++;
			} else {
				score -= ourAttackers[valueMap[Main.globalPosition.engineLookup[usedMy]]];
				
				usedMy++;
			}
		}
		
		return score;
	}
	
	private static int moveScoreHeuristic(int move, int pvMove, int ply) {
		byte to = (byte)((move >>> 6) & 0x3F);
		byte pieceType = (byte)((move >>> 12) & 0xF);
		byte captureType = (byte)((move >>> 16) & 0xF);
		byte color = (byte)((move >>> 31) & 1);
		
		int ourValue = valueMap[pieceType];
		int captureValue = valueMap[captureType];
		int score = 0;
		
		if (captureType != 0) {
			// ~90,000 Max
			score += (captureValue * 100 - ourValue); // All Captures looked upon favorably, but best first
		}
		
		if (pvMove == move) score += 1_000_000;
		if (move == killerMoves[ply][0] || move == killerMoves[ply][1]) score += 900_000;
		
		return score;
	}
	
	public static void sortByScore(int[] arr, int pvMove, int ply) {
	    int n = arr.length;
	    int[] scores = new int[n];

	    // Step 1: Precompute scores
	    for (int i = 0; i < n; i++) {
	        scores[i] = moveScoreHeuristic(arr[i], pvMove, ply);
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
	
	public static final int FLAG_EXACT = 0;
	public static final int FLAG_LOWERBOUND = 1; // (beta cutoff), cache value must be >= beta
	public static final int FLAG_UPPERBOUND = 2; // (alpha cutoff), cache value must be <= alpha
	public static final int FLAG_TIMEOUT = 3;
	
	// minDepth takes priority, if there is extra time it will run until maxTime (ms)
	private static int[] iterativeDeepening(int minDepth, int maxTime, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		int[] previousBestMove = null;
		for (int currentDepth = 0; currentDepth >= 0; currentDepth++) {
			long timeElapsedMs = (System.nanoTime() - startTime) / 1_000_000;
			
			if (currentDepth > minDepth && timeElapsedMs >= maxTime - 1) {
				break;
			}
			
			int[] newMove = minimax(currentDepth, isMaximizing, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
			
			previousBestMove = newMove;			
			System.out.println("(" + ((System.nanoTime() - startTime) / 1_000_000) + " ms)" + "Move at depth " + currentDepth + ": " + Position.logMove(previousBestMove[0]) + ": " + Arrays.toString(previousBestMove));
			
			if (currentDepth > 50) break;
		}
		
		return previousBestMove;
	}
	
	static int tableSize = 1 << 26;
	static long[] zobristKeys = new long[tableSize];
	static int[][] zobristValues = new int[tableSize][4];
	
	private static int[] minimax(int depth, boolean isMaximizing, int alpha, int beta, int ply) {
		if (depth == 0) {
			return new int[] {-1, Main.globalPosition.getEval(), -1, depth};
		}
		
		int zobristIndex = (int)(Main.globalPosition.zobristHash & (tableSize - 1));
		if (zobristKeys[zobristIndex] == Main.globalPosition.zobristHash) {
			int[] storedValue = zobristValues[zobristIndex];
			int TT_FLAG = storedValue[2];
			int TT_DEPTH = storedValue[3];
			int TT_SCORE = storedValue[1];
			
			if (TT_DEPTH >= depth) {
				if (TT_FLAG == FLAG_EXACT) {
					return storedValue;
				} else if (TT_FLAG == FLAG_LOWERBOUND && TT_SCORE >= beta) {
					return storedValue;
				} else if (TT_FLAG == FLAG_UPPERBOUND && TT_SCORE <= alpha) {
					return storedValue;
				}
			}
		}
		
		int[] legalMoves = Main.globalPosition.getAllLegalMoves((byte)(isMaximizing ? 0 : 1));
		int[] bestMove = new int[]{-1, isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE, -1, depth};
		int pvMove = zobristKeys[zobristIndex] == Main.globalPosition.zobristHash ? zobristValues[zobristIndex][0] : -1;
		sortByScore(legalMoves, pvMove, ply);
		
		int moveIndex = 0;
		for (int move : legalMoves) {
			boolean isCapture = (byte)((move >>> 16) & 0xF) != 0;
			byte promotionKey = (byte)((move >>> 22) & 0xF);
			
			Main.globalPosition.makeMove(move, true);
			int[] scoreResult = minimax(depth - 1, !isMaximizing, alpha, beta, ply + 1);
			Main.globalPosition.unmakeMove(move);
			
			if (isMaximizing) {
				if (scoreResult[1] > bestMove[1]) {
					bestMove = scoreResult;
					bestMove[0] = move;
					
					alpha = Math.max(alpha, scoreResult[1]);
				}
			} else {
				if (scoreResult[1] < bestMove[1]) {
					bestMove = scoreResult;
					bestMove[0] = move;
					
					beta = Math.min(beta, scoreResult[1]);
				}
			}
			
			if (alpha >= beta) {
				bestMove[2] = isMaximizing ? FLAG_LOWERBOUND : FLAG_UPPERBOUND;
				
				// Move caused cutoff, add killer
				if (!isCapture && promotionKey == 0) {
					killerMoves[ply][1] = killerMoves[ply][0];
					killerMoves[ply][0] = move;
				}
				
				break;
			} else {
				bestMove[2] = FLAG_EXACT;
			};
			
			moveIndex++;
		}
		
		if (zobristKeys[zobristIndex] != Main.globalPosition.zobristHash) {
			zobristKeys[zobristIndex] = Main.globalPosition.zobristHash;
			zobristValues[zobristIndex] = bestMove.clone();
		} else {
			if (zobristValues[zobristIndex][3] < depth) {
				zobristKeys[zobristIndex] = Main.globalPosition.zobristHash;
				zobristValues[zobristIndex] = bestMove.clone();
			}
		}
		
		return bestMove;
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