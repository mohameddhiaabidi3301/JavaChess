package engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.io.*;
import java.util.*;

public class MagicBitboards {
	private static long[] rookMagics = {
	    0x8A80104000800020L, 0x0084020100804000L, 0x00800A1000048020L, 0xC4100020B1000200L,
	    0x0400440002080420L, 0x0A8004002A801200L, 0x0840140C80400100L, 0x010000820C412300L,
	    0x0010800212400820L, 0x0008050190002800L, 0x0001080800102000L, 0x0041080080201001L,
	    0x020820040800890AL, 0x0010800200008440L, 0x03200800418A0022L, 0x0250060600201100L,
	    0x4440002400860020L, 0x1004402800084000L, 0x00041404C0140004L, 0x5000400908001400L,
	    0x0000020841000830L, 0x00830A0101000500L, 0x014040A002804040L, 0x4400101008854220L,
	    0xE008025220022600L, 0x0440244008603000L, 0x0008024004009000L, 0x0801009002100002L,
	    0x0400200200010811L, 0x3204020044012400L, 0x0002100088200100L, 0x020800A004091041L,
	    0x000210C224200241L, 0x00200A0C02040080L, 0x004D8028104C0800L, 0x813C0A0002900012L,
	    0x0008104200208020L, 0x240400A000A04080L, 0x0802199100100042L, 0x062C4C0020100280L,
	    0x0020104280800820L, 0x20C8010080A80200L, 0x1114084080464008L, 0x2000025430001805L,
	    0x1404C4A100110008L, 0x0000008400012008L, 0x3045140080022010L, 0x8040028410080100L,
	    0x0220200310204820L, 0x0200082244048202L, 0x00090984C0208022L, 0x8000110120040900L,
	    0x9000402400080084L, 0x2402100100038020L, 0x0098400600008028L, 0x000111000040200CL,
	    0x0102402208108102L, 0x0440041482204101L, 0x4004402000040811L, 0x804A000810402002L,
	    0xc7760028206d103aL, 0x0440341108009002L, 0x0000008825084204L, 0x2084002112428402L
	};
	
	private static long[] bishopMagics = {
	    0x0080810410820200L, 0x2010520422401000L, 0xa7d0045780625099L, 0x1001050002610001L,
	    0x9000908280000000L, 0x20080442A0000001L, 0x0221A80045080800L, 0x000060200A404000L,
	    0x0020100894408080L, 0x0800084021404602L, 0x0040804100298014L, 0x5080201060400011L,
	    0x49000620A0000000L, 0x8000001200300000L, 0x4000008241100060L, 0x0000040920160200L,
	    0x0042002000240090L, 0x000484100420A804L, 0x0008000102000910L, 0x04880010A8100202L,
	    0x0004018804040402L, 0x0202100108281120L, 0xC201162010101042L, 0x0240088022010B80L,
	    0x008301600C240814L, 0x000028100E142050L, 0x0020880000838110L, 0x00410800040204A0L,
	    0x2012002206008040L, 0x004402881900A008L, 0x14A80004804C1080L, 0xA004814404800F02L,
	    0x00C0180230101600L, 0x000C905200020080L, 0x060400080010404AL, 0x00040401080C0100L,
	    0x0020121010140040L, 0x0000500080000861L, 0x8202090241002020L, 0x2008022008002108L,
	    0x0200402401042000L, 0x0002E03210042000L, 0x0110040080422400L, 0x908404C0584040C0L,
	    0x1000204202240408L, 0x8002002200200200L, 0x2002008101081414L, 0x0002080021098404L,
	    0x0060110080680000L, 0x1080048108420000L, 0x0400184014100000L, 0x008081A004012240L,
	    0x00110080448182A0L, 0xA4002000604A4000L, 0x0004002811049020L, 0x00024A0410A10220L,
	    0x0808090089013000L, 0x0C80800400805800L, 0x0001020100061618L, 0x1202820040501008L,
	    0x413010050C100405L, 0x0004248204042020L, 0x0044004408280110L, 0x6010220080600502L
	};
	
	public static int[][][] rookLookupTable = new int[64][][];
	public static int[][][] bishopLookupTable = new int[64][][];
	
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
	
	public static int[] simulateRook(int row, int col, long blockerPermutation) {
		int[][] moveDirections = {
				{0, 1},
				{1, 0},
				{0, -1},
				{-1, 0},
		};
		
		int[] moves = new int[14];
		int moveCount = 0;
		int square = row * 8 + col;
		
		for (int[] direction : moveDirections) {
			int terminalRow = getTerminalPoint(direction[0], row);
			int terminalCol = getTerminalPoint(direction[1], col);
			
			if (row == terminalRow && col == terminalCol) continue;
			
			for (int i = 1; i < 8; i++) {
				int targetRow = row + (direction[0] * i);
				int targetCol = col + (direction[1] * i);
				int targetSquare = targetRow * 8 + targetCol;
				if (i == 1 && !withinBounds(targetRow, targetCol, 0, 7)) break;
				
				int move = square;
				move |= (targetSquare << 6);
				moves[moveCount++] = move;
				
				if (targetRow == terminalRow && targetCol == terminalCol) break;
				if (((blockerPermutation & (targetSquare)) == 1)) break;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[] simulateBishop(int row, int col, long blockerPermutation) {
		int[][] moveDirections = {
				{1, 1},
				{1, -1},
				{-1, 1},
				{-1, -1},
		};
		
		int[] moves = new int[14];
		int moveCount = 0;
		int square = row * 8 + col;
		
		for (int[] direction : moveDirections) {
			int terminalRow = getTerminalPoint(direction[0], row);
			int terminalCol = getTerminalPoint(direction[1], col);
			
			for (int i = 1; i < 8; i++) {
				int targetRow = row + (direction[0] * i);
				int targetCol = col + (direction[1] * i);
				int targetSquare = targetRow * 8 + targetCol;
				
				if (i == 1 && !withinBounds(targetRow, targetCol, 0, 7)) break;
				if (!withinBounds(targetRow, targetCol, 1, 6)) break;
				
				int move = square;
				move |= (targetSquare << 6);
				moves[moveCount++] = move;
				
				if (((blockerPermutation & (1 << targetSquare)) == 1)) break;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static void initRookLookups() {
		for (int square = 0; square < 64; square++) {
			int row = (int)Math.floor(square / 8);
			int col = square % 8;
			
			long blockerMask = genRookBlockerMask(row, col);
			ArrayList<Integer> blockerLocations = getSetBits(blockerMask);
			int numBlockers = blockerLocations.size();
			int numPermutations = 1 << numBlockers;
			int shift = 64 - numBlockers;
			long magic = rookMagics[square];
			
			rookLookupTable[square] = new int[numPermutations][];
			
			for (int j = 0; j < numPermutations; j++) {
				long permutation = 0L;

				for (int k = 0; k < numBlockers; k++) {
					if (((j >> k) & 1L) != 0) {
						permutation |= (1L << blockerLocations.get(k));
					}
				}
				
				int[] pseudoMoves = simulateRook(row, col, permutation);
				long product = permutation * magic;
				int hashIndex = (int)((product >>> shift));
				
				if (rookLookupTable[square][hashIndex] != null) {
				    System.err.println("Warning: Overwriting existing entry at [" + square + "][" + hashIndex + "]");
				}
				
				rookLookupTable[square][hashIndex] = pseudoMoves;
			}
		}
	}
	
	public static void initBishopLookups() {
		for (int square = 0; square < 64; square++) {
			int row = (int)Math.floor(square / 8);
			int col = square % 8;
			
			long blockerMask = genRookBlockerMask(row, col);
			ArrayList<Integer> blockerLocations = getSetBits(blockerMask);
			int numBlockers = blockerLocations.size();
			int numPermutations = 1 << numBlockers;
			int shift = 64 - numBlockers;
			long magic = rookMagics[square];
			
			bishopLookupTable[square] = new int[numPermutations][];
			
			for (int j = 0; j < numPermutations; j++) {
				long permutation = 0L;

				for (int k = 0; k < numBlockers; k++) {
					if (((j >> k) & 1L) != 0) {
						permutation |= (1L << blockerLocations.get(k));
					}
				}
				
				int[] pseudoMoves = simulateBishop(row, col, permutation);
				long product = permutation * magic;
				int hashIndex = (int)((product >>> shift));
				
				if (bishopLookupTable[square][hashIndex] != null) {
				    System.err.println("Warning: Overwriting existing entry at [" + square + "][" + hashIndex + "]");
				}
				
				bishopLookupTable[square][hashIndex] = pseudoMoves;
			}
		}
	}
	
	public static void calculateMagics(int rangeMin, int rangeMax) {
	    long[] calculatedRookMagics = new long[64];
	    long[] calculatedBishopMagics = new long[64];
	    Random rng = new Random();
	    
	    // Create output file with timestamp
	    String filename = String.format("magics_range_%d_%d_%d.txt", rangeMin, rangeMax, System.currentTimeMillis());
	    
	    try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
	        writer.println(String.format("=== Magic Number Search Results for Range %d-%d ===", rangeMin, rangeMax));
	        writer.println("Started at: " + new Date());
	        writer.println();
	        writer.flush(); // Ensure header is written immediately
	        
	        for (int square = rangeMin; square <= rangeMax; square++) {
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
	                        long permutation = 0L;

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
	                        
	                        String result = String.format("[%d][%s] - %s - Took %d attempts, %.2f minutes", 
	                            square, (isRook ? "Rook" : "Bishop"), Long.toHexString(magic), attempt, minutesElapsed);
	                        
	                        // Print to console
	                        System.out.println(result);
	                        
	                        // Write to file immediately
	                        writer.println(result);
	                        writer.flush(); // Force write to disk immediately
	                        
	                        if (isRook) {
	                            calculatedRookMagics[square] = magic;
	                        } else {
	                            calculatedBishopMagics[square] = magic;
	                        }
	                        break;
	                    }

	                    if (attempt % 10000000 == 0) {
	                        String progress = String.format("[%d][%s] %d / %d, Attempt %d", 
	                            square, (isRook ? "Rook" : "Bishop"), bestAttempt, numPermutations, attempt);
	                        System.out.println(progress);
	                        
	                        // Also log progress to file
	                        writer.println("PROGRESS: " + progress);
	                        writer.flush();
	                    }
	                }
	            }
	        }
	        
	        // Write final summary
	        writer.println();
	        writer.println("=== FINAL SUMMARY ===");
	        writer.println("Completed at: " + new Date());
	        writer.println();
	        writer.println("Rook Magics:");
	        for (int i = rangeMin; i <= rangeMax; i++) {
	            if (calculatedRookMagics[i] != 0) {
	                writer.println(String.format("rook_magics[%d] = 0x%sL;", i, Long.toHexString(calculatedRookMagics[i])));
	            }
	        }
	        writer.println();
	        writer.println("Bishop Magics:");
	        for (int i = rangeMin; i <= rangeMax; i++) {
	            if (calculatedBishopMagics[i] != 0) {
	                writer.println(String.format("bishop_magics[%d] = 0x%sL;", i, Long.toHexString(calculatedBishopMagics[i])));
	            }
	        }
	        
	    } catch (IOException e) {
	        System.err.println("Error writing to file: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    // Still print to console for immediate feedback
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