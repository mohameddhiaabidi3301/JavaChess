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
		
		int[] attackers = Main.globalPosition.attacks[1 - color][to];
		int[] defenders = Main.globalPosition.attacks[color][to];
		
		if (Main.globalPosition.attacks[color][to] == null && Main.globalPosition.attacks[1 - color][to] != null) {
			score -= ourValue; // Hanged Piece
		}
		
		if (attackers != null && defenders != null) {
			if (attackers.length > defenders.length) {
				score -= ourValue;
			}
		}
		
		return score;
	}
	
	public static void sortByScore(int[] arr, int[] previousBestMove, int depth, int ply, int[][] killerMoves) {
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
			
			int[] newMove = initiateRootThreadedSearch(currentDepth, isMaximizing, Integer.MIN_VALUE, Integer.MAX_VALUE, previousBestMove, startTime, maxTime, 0, 0);
			
			if (newMove[2] == FLAG_TIMEOUT) {
				System.out.println("Timeout on move: " + Arrays.toString(newMove) + " (" + currentDepth + ")");
				break;
			}
			
			previousBestMove = newMove;			
			System.out.println("(" + ((System.nanoTime() - startTime) / 1_000_000) + " ms)" + "Move at depth " + currentDepth + ": " + Position.logMove(previousBestMove[0]) + ": " + Arrays.toString(previousBestMove));
			
			if (currentDepth > 50) break;
		}
		
		return previousBestMove;
	}
	
	static final int TT_SIZE = 1 << 22;
	static final AtomicLongArray zobristKeys = new AtomicLongArray(TT_SIZE);
	static int[][] zobristValues = new int[TT_SIZE][4];
	
	private static int[] initiateRootThreadedSearch(int depth, boolean isMaximizing, int alpha, int beta, int[] previousBestMove, long itdStartTime, int maxTime, int quiessenceCount, int ply) {
		int[] legalMoves = Main.globalPosition.getAllLegalMoves((byte)(isMaximizing ? 0 : 1));
		SearchThread[] threads = new SearchThread[legalMoves.length];
		CountDownLatch latch = new CountDownLatch(legalMoves.length);
		
		for (int i = 0; i < legalMoves.length; i++) {
			int move = legalMoves[i];
			
			SearchThread newThread = new SearchThread(move, depth-1, isMaximizing, alpha, beta, previousBestMove, itdStartTime, maxTime, quiessenceCount, ply + 1, latch);
			newThread.start();
			threads[i] = newThread;
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int[] bestMove = new int[] {-1, (isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE), FLAG_TIMEOUT, depth};
		for (int i = 0; i < threads.length; i++) {
			SearchThread thread = threads[i];
			int[] move = thread.getBestMove();
			if (move[2] == FLAG_TIMEOUT) continue;
			
			if (isMaximizing && move[1] > bestMove[1]) {
				bestMove = move;
			} else if (!isMaximizing && move[1] < bestMove[1]) {
				bestMove = move;
			}
		}
		
		return bestMove;
	}
}