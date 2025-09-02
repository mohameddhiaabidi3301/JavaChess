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
	
	public static int getComputerMove(int minDepth, int maxMs, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		int move = iterativeDeepening(minDepth, maxMs, isMaximizing);
		
		long endTime = System.nanoTime();
		int millisecondsEllapsed = (int)((endTime - startTime) / 1_000_000);
		
		System.out.println("Took: " + (millisecondsEllapsed) + " milliseconds to compute move");
		
		return move;
	}
	
	private static int moveScoreHeuristic(int move, int ply) {
		int score = 0;
		byte captureType = (byte) ((move >>> 16) & 0xF);
		
		if (pvLine[ply] == move) score += 999_999;
		if (captureType != 0) score += 10_000;
		
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
	
	// minDepth takes priority, if there is extra time it will run until maxTime (ms)
	private static int iterativeDeepening(int minDepth, int maxTime, boolean isMaximizing) {
		long startTime = System.nanoTime();
		
		int previousBestMove = -1;
		for (byte currentDepth = 0; currentDepth >= 0; currentDepth++) {
			long timeElapsedMs = (System.nanoTime() - startTime) / 1_000_000;
			
			if (currentDepth > minDepth && timeElapsedMs >= maxTime - 1) {
				break;
			}
			
			negaMax(currentDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, (byte)(isMaximizing ? 0 : 1), (byte)(0));
			int[] bestMoveInfo = TT.probe(Main.globalPosition.zobristHash, Integer.MIN_VALUE, Integer.MAX_VALUE, (byte)(currentDepth));
			
			int newMove = -1;
			int score = 0;
			
			if (bestMoveInfo != null) {
				newMove = bestMoveInfo[1];
				score = bestMoveInfo[0];
			}
			
			previousBestMove = newMove;
			System.out.println("(" + ((System.nanoTime() - startTime) / 1_000_000) + " ms)" + "Move at depth " + currentDepth + ": " + Position.logMove(previousBestMove) + ": " + score);
			
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
			
			if (count >= 2) return true;
		}
		return false;
	}
	
	// alpha: best score we can guarantee so far (lower bound)
	// beta: worst score the opponent can hold us to (upper bound)
	
	public static int quiescenceSearch(byte ply, int alpha, int beta, byte color) {
		int standPat = Main.globalPosition.getEval() * (1 - (color << 1));
		
		if (ply > MAX_QUIESCENCE_PLY) {
			return standPat;
		}
		
		if (standPat >= beta) return standPat;
		alpha = Math.max(alpha, standPat);
		
		int[] allMoves = Main.globalPosition.getAllCapturesChecks(color);
		sortByScore(allMoves, ply);
		
		int bestScore = standPat;
		for (int move : allMoves) {
			Main.globalPosition.makeMove(move, true);
			int score = -quiescenceSearch((byte)(ply + 1), -beta, -alpha, (byte)(color ^ 1));
			Main.globalPosition.unmakeMove(move);
			
			if (score > bestScore) {
				bestScore = score;
			}
			
			alpha = Math.max(alpha, score);
			if (alpha >= beta) break;
		}
		
		return bestScore;
	}
	
	private static int[] pvLine = new int[128];
	public static int negaMax(byte depth, int alpha, int beta, byte color, byte ply) {
		if (depth <= 0) {
			return quiescenceSearch(ply, alpha, beta, color);
		};
		
		int[] existingEntry = TT.probe(Main.globalPosition.zobristHash, alpha, beta, depth);
		if (existingEntry != null) {
			pvLine[ply] = existingEntry[1];
			return existingEntry[0];
		}
		
		int alphaOrigin = alpha;
		int betaOrigin = beta;
		
		int bestScore = Integer.MIN_VALUE;
		int pvMove = -1;
	
		int[] legalMoves = Main.globalPosition.getAllLegalMoves(color);
		sortByScore(legalMoves, ply);
		
		for (int move : legalMoves) {
			Main.globalPosition.makeMove(move, true);
			int score = -negaMax((byte)(depth - 1), -beta, -alpha, (byte)(color ^ 1), (byte)(ply + 1));
			Main.globalPosition.unmakeMove(move);
			
			if (score > bestScore) {
				bestScore = score;
				pvMove = move;
			}
			
			alpha = Math.max(score, alpha);
			if (alpha >= beta) break;
		}
		
		byte flag;
		long zobristHash = Main.globalPosition.zobristHash;
		if (bestScore <= alphaOrigin) { // We were unable to find a move better than what we had: Fail-low: Upperbound
			flag = FLAG_UPPERBOUND;
		} else if (bestScore >= betaOrigin) { // Fail high, exceeded opponent threshold they wouldn't go down this node: Lowerbound
			flag = FLAG_LOWERBOUND;
		} else { // Exact, bestscore is within alphaOrigin and betaOrigin normal bounds
			flag = FLAG_EXACT;
		}
		
		TT.set(Main.globalPosition.zobristHash, depth, bestScore, pvMove, flag);
		
		return bestScore;
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


