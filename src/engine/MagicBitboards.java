package engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MagicBitboards {
	private int[] rookMagics = {};
	private int[] bishopMagics = {};
	
	private static int getTerminalPoint(int vector1, int current) {
		if (vector1 == 1) return 6;
		if (vector1 == -1) return 1;
		
		return current;
	}
	
	private static boolean withinBounds(int row, int col, int rangeMin, int rangeMax) {
		if (row < rangeMin || row > rangeMax) return false;
		if (col < rangeMin || col > rangeMax) return false;
		
		return true;
	}
	
	public static long genRookBlockerMask(int row, int col) {
		long mask = 0L;
		int[][] moveDirections = {
				{0, 1},
				{1, 0},
				{0, -1},
				{-1, 0},
		};
		
		for (int[] direction : moveDirections) {
			int terminalRow = getTerminalPoint(direction[0], row);
			int terminalCol = getTerminalPoint(direction[1], col);
			
			if (row == terminalRow && col == terminalCol) continue;
			
			for (int i = 1; i < 8; i++) {
				int targetRow = row + (direction[0] * i);
				int targetCol = col + (direction[1] * i);
				int square = targetRow * 8 + targetCol;
				if (i == 1 && !withinBounds(targetRow, targetCol, 0, 7)) break;
				
				mask |= (1L << square);
				
				if (targetRow == terminalRow && targetCol == terminalCol) break;
			}
		}
		
		return mask;
	}
	
	public static long genBishopBlockerMask(int row, int col) {
		long mask = 0L;
		int[][] moveDirections = {
				{1, 1},
				{1, -1},
				{-1, 1},
				{-1, -1},
		};
		
		for (int[] direction : moveDirections) {
			int terminalRow = getTerminalPoint(direction[0], row);
			int terminalCol = getTerminalPoint(direction[1], col);
			
			for (int i = 1; i < 8; i++) {
				int targetRow = row + (direction[0] * i);
				int targetCol = col + (direction[1] * i);
				int square = targetRow * 8 + targetCol;
				if (i == 1 && !withinBounds(targetRow, targetCol, 0, 7)) break;
				if (!withinBounds(targetRow, targetCol, 1, 6)) break;
				
				mask |= (1L << square);
			}
		}
		
		return mask;
	}
	
	public static void initRookLookups() {
		
	}
	
	public static void initBishopLookups() {
		
	}
	
	public static void calculateMagics(int rangeMin, int rangeMax) {
		long[] calculatedRookMagics = new long[64];
		long[] calculatedBishopMagics = new long[64];
		Random rng = new Random();
		
 		for (int square = rangeMin; square < rangeMax; square++) {
			int row = (int)Math.floor(square / 8);
			int col = square % 8;
			
			for (int t = 0; t <= 1; t++) {
				boolean isRook = (t == 0) ? true : false;
				long blockerMask = (isRook) ? genRookBlockerMask(row, col) : genBishopBlockerMask(row, col);
				ArrayList<Integer> blockerLocations = getSetBits(blockerMask);
				int numBlockers = blockerLocations.size();
				int numPermutations = 1 << numBlockers;
				int shift = 64 - numBlockers;
				
				int bestAttempt = -1;
				long startTime = System.nanoTime();
				for (int attempt = 0; attempt < 500000000; attempt++) {
					long magic = rng.nextLong();
					Set<Integer> foundIndexes = new HashSet<Integer>();
					boolean success = true;
					
					for (int j = 0; j < numPermutations; j++) {
						long permutation = 0;
						
						for (int k = 0; k < numBlockers; k++) {
							if (((j >> k) & 1L) != 0) {
								permutation |= (1L << blockerLocations.get(k));
							}
						}
						
						long product = permutation * magic;
						int hashIndex = (int)((product >>> shift));
						
						if (foundIndexes.contains(hashIndex)) {
							success = false;
							
							if (j > bestAttempt) {
								bestAttempt = j;
							}
							
							break;
						} else {
							foundIndexes.add(hashIndex);
						}
					}
					
					if (success) {
						long endTime = System.nanoTime();
						long elapsedTime = endTime - startTime;
						double minutesElapsed = elapsedTime / 60_000_000_000.0;
						System.out.println(String.format("[%d][%s] - %s - Took %d attempts, %.2f minutes", square, (isRook ? "Rook" : "Bishop"), Long.toHexString(magic), attempt, minutesElapsed));
						
						if (isRook) {
							calculatedRookMagics[square] = magic;
						} else {
							calculatedBishopMagics[square] = magic;
						}
						
						break;
					}
					
					if (attempt % 10000000 == 0) {
						System.out.println(String.format("[%d][%s] %d / %d, Attempt %d", square, (isRook ? "Rook" : "Bishop"), bestAttempt, numPermutations, attempt));
					}
				}
			}
		}
 		
 		System.out.println(String.format("Results for range %d - %d. Rooks:", rangeMin, rangeMax));
 		System.out.println(Arrays.toString(calculatedRookMagics));
 		System.out.println("___Bishops:_____");
 		System.out.println(Arrays.toString(calculatedBishopMagics));
	}
	
	public static ArrayList<Integer> getSetBits(long bits) {
		ArrayList<Integer> setBits = new ArrayList<Integer>();
		
		while (bits != 0) {
			int idx = Long.numberOfTrailingZeros(bits);
			setBits.add(idx);
			bits &= bits - 1;
		}
		
		return setBits;
	}
}