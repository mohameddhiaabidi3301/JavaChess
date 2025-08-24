package engine;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.*;
import java.util.*;
import debug.DebugRender;


public class MagicBitboards {
	public static long[] rookMagics = {
	    0x8A80104000800020L, 0x40004010002000L, 0x220008b200208040L, 0xc1001000a0884500L,
	    0x200020004100820L, 0x1100040001000208L, 0xf4001d0a300e8804L, 0x010000820C412300L,
	    0x9603800195e4c000L, 0x8b324000c3a01008L, 0x56003046012181L, 0xe4910038d0010060L,
	    0x67b005018002d00L, 0x38a0010181a00bcL, 0x6cac00287e2c1017L, 0x95e600054e14058fL,
	    0x8ee5e88003854007L, 0xfa8bc04005201009L, 0xaa78420014820022L, 0x382af20008420060L,
	    0x517e8e0020aa0046L, 0xea3d280110406c20L, 0x75ad40050010802L, 0xde16e2000ac5941bL,
	    0x5e5c00680028e63L, 0xbfc270500400280L, 0x6b9600c2002186f0L, 0xe831ea0200207041L,
	    0x152a22860010a600L, 0xc2d6007200344810L, 0x2ec7f67c00074810L, 0xf225770e000a8b5cL,
	    0x6a08c0028880062bL, 0x8fb22000c4c01003L, 0x16752002c1003303L, 0xcb4790163001001L,
	    0x1c95003411001801L, 0x278a009812003094L, 0xbae85a0d24002830L, 0xd9c849095e000c84L,
	    0x3f1b278440048009L, 0xad1000e01547c001L, 0x9f21a00043010033L, 0xd5b0620109c20010L,
	    0xbb4db85e000e0012L, 0x14ca0051142a0018L, 0x16a2c8b03a0c0003L, 0xf367384d05960004L,
	    0x56cd8a010947a200L, 0x4782028500406200L, 0xf0dc910460034700L, 0x2906e08a00c01200L,
	    0xab4b60e692007600L, 0x6c76006c38d08200L, 0x58377e17af986c00L, 0xf3d45f24014c8600L,
	    0xda91c0a082065302L, 0x0440041482204101L, 0x97b500600052c13bL, 0X71bf266a0020404eL,
	    0xc7760028206d103aL, 0xbbf20048100c4126L, 0x148a3b101627980cL, 0xa4b1440528c3830aL
	};
	
	public static long[] bishopMagics = {
	    0xf15f9fe3eee599e0L, 0xbd18700c82055265L, 0xf6901c1589e19668L, 0xa987df5edf026612L,
	    0x1044146086e4a645L, 0xff63101a3090fa6cL, 0xf4e8e904202051d4L, 0xd77a0103411018abL,
	    0x18c60cb86a380e0dL, 0xfb4eca2e184e0084L, 0x2afa10088604c163L, 0xd017143402835cebL,
	    0xd06eac10448bdc90L, 0xdb73b4242088978eL, 0x6341a1180d303846L, 0xe57af6f405e9c540L,
	    0xc8447e58600c3c43L, 0x99604d4e58622480L, 0x5cd803d0088fe0c9L, 0x1ece81a80603c095L,
	    0x26ac03d683a01144L, 0x2fe900da00621210L, 0x39fa10340613143dL, 0xc9ca15a5cb041eabL,
	    0x5308b865207a302aL, 0x5346206458880990L, 0x42218811900264a0L, 0x00410800040204A0L,
	    0x2012002206008040L, 0xfbf90f001a034118L, 0xd8c504033f24011eL, 0xb6965249fa030c07L,
	    0x1de2480cd240d054L, 0x34610837925ef46aL, 0xfa5a0503057003c9L, 0x00040401080C0100L,
	    0x0020121010140040L, 0xe71ba68200d90105L, 0x52e908188ed21a07L, 0x46eb94e9662e0276L,
	    0xcf36efc6fb0e604bL, 0x8e2942718066063L, 0x617228e804020802L, 0xc2f1daa088011d15L,
	    0x8518144a92009c01L, 0x7b4e9d8d06007900L, 0x6df4181809114543L, 0xd5aba3f409536c35L,
	    0x68b13dc4c3dc8bfL, 0xc3cd84042a06bdc8L, 0xa99a1cce080c4080L, 0x7c20b9ea61881696L,
	    0x8c4b0913360200c4L, 0xe517c0c8796900f1L, 0xc3a0349548050b3aL, 0xc8c8321812087076L,
	    0x991e02010c222e3bL, 0x302ea4e2031c2063L, 0xab2b569b9d183833L, 0xf4714b913a841401L,
	    0xf4fb4b2c1f8db3ecL, 0x3f41003f302b86daL, 0x4c68e02a96162c0cL, 0x622004bf08059088L,
	};
	
	public static int[][][] rookLookupTable = new int[64][][];
	public static int[][][] bishopLookupTable = new int[64][][];
	
	public static long[] rookMasks = new long[64];
	public static long[] bishopMasks = new long[64];
	
	public static byte[] rookShifts = new byte[64];
	public static byte[] bishopShifts = new byte[64];
	
	private static int getTerminalPoint(int vector1, int current) {
		if (vector1 == 1) return 6;
		if (vector1 == -1) return 1;
		
		return current;
	}
	
	private static int getFullTerminalPoint(int vector1, int current) {
		if (vector1 == 1) return 7;
		if (vector1 == -1) return 0;
		
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
			int terminalRow = getFullTerminalPoint(direction[0], row);
			int terminalCol = getFullTerminalPoint(direction[1], col);
			
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
				if ((blockerPermutation & (1L << targetSquare)) != 0) break;
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
			for (int i = 1; i < 8; i++) {
				int targetRow = row + (direction[0] * i);
				int targetCol = col + (direction[1] * i);
				int targetSquare = targetRow * 8 + targetCol;
				long dataAtTarget = ((blockerPermutation & (1L << targetSquare)));
				
				if (i == 1 && !withinBounds(targetRow, targetCol, 0, 7)) break;
				if (!withinBounds(targetRow, targetCol, 0, 7)) break;
				
				int move = square;
				move |= (targetSquare << 6);
				moves[moveCount++] = move;
				
				if (dataAtTarget != 0) {
					break;
				}
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static void initRookLookups() {
		int collisionCount = 0;
		for (int square = 0; square < 64; square++) {
			int row = (int)Math.floor(square / 8);
			int col = square % 8;
			
			long blockerMask = genRookBlockerMask(row, col);
			int[] blockerLocations = getSetBits(blockerMask);
			int numBlockers = blockerLocations.length;
			int numPermutations = 1 << numBlockers;
			byte shift = (byte)(64 - numBlockers);
			long magic = rookMagics[square];
			
			rookLookupTable[square] = new int[numPermutations][];
			rookMasks[square] = blockerMask;
			rookShifts[square] = shift;
			
			for (int j = 0; j < numPermutations; j++) {
				long permutation = 0L;


				for (int k = 0; k < numBlockers; k++) {
					if (((j >> k) & 1L) != 0) {
						permutation |= (1L << blockerLocations[k]);
					}
				}
				
				int[] pseudoMoves = simulateRook(row, col, permutation);
				long product = permutation * magic;
				int hashIndex = (int)((product >>> shift));
				
				if (rookLookupTable[square][hashIndex] != null) {
				    System.err.println("Warning: Overwriting existing entry at [" + square + "][" + hashIndex + "]");
				    collisionCount++;
				}
				
				rookLookupTable[square][hashIndex] = pseudoMoves;
			}
		}
		
		if (collisionCount > 0) {
			System.err.println("A total of " + collisionCount + " collisions from the rook magics");
		}
	}
	
	public static void initBishopLookups() {
		int collisionCount = 0;
		for (int square = 0; square < 64; square++) {
			int row = (int)Math.floor(square / 8);
			int col = square % 8;
			
			long blockerMask = genBishopBlockerMask(row, col);
			int[] blockerLocations = getSetBits(blockerMask);
			int numBlockers = blockerLocations.length;
			int numPermutations = 1 << numBlockers;
			byte shift = (byte)(64 - numBlockers);
			long magic = bishopMagics[square];
			
			bishopLookupTable[square] = new int[numPermutations][];
			bishopMasks[square] = blockerMask;
			bishopShifts[square] = shift;
			
			for (int j = 0; j < numPermutations; j++) {
				long permutation = 0L;


				for (int k = 0; k < numBlockers; k++) {
					if (((j >> k) & 1L) != 0) {
						permutation |= (1L << blockerLocations[k]);
					}
				}
				
				int[] pseudoMoves = simulateBishop(row, col, permutation);
				long product = permutation * magic;
				int hashIndex = (int)((product >>> shift));
				
				if (bishopLookupTable[square][hashIndex] != null) {
				    System.err.println("Warning: Overwriting existing entry at [" + square + "][" + hashIndex + "] Permutation Number: " + j);
				    collisionCount++;
				}
				
				bishopLookupTable[square][hashIndex] = pseudoMoves;
			}
		}
		
		if (collisionCount > 0) {
			System.err.println("A total of " + collisionCount + " collisions from the bishop magics");
		}
	}
	
	public static void calculateMagics(int[] squaresToSearch) {
	    long[] calculatedRookMagics = new long[64];
	    long[] calculatedBishopMagics = new long[64];
	    Random rng = new Random();
	    
	    // Create output file with timestamp
	    String filename = String.format("magics[%s]_%d.txt", Arrays.toString(squaresToSearch), System.currentTimeMillis());
	    
	    try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
	        writer.println(String.format("=== Magic Number Search Results for [%s] ===", Arrays.toString(squaresToSearch)));
	        writer.println("Started at: " + new Date());
	        writer.println();
	        writer.flush(); // Ensure header is written immediately
	        
	        for (int square : squaresToSearch) {
	            int row = (int)Math.floor(square / 8);
	            int col = square % 8;


	            for (int t = 0; t <= 0; t++) {
	                boolean isRook = (t == 0) ? true : false;
	                long blockerMask = (isRook) ? genRookBlockerMask(row, col) : genBishopBlockerMask(row, col);
	                int[] blockerLocations = getSetBits(blockerMask);
	                int numBlockers = blockerLocations.length;
	                int numPermutations = 1 << numBlockers;
	                int shift = 64 - numBlockers;


	                int bestAttempt = -1;
	                long bestLong = -1;
	                long startTime = System.nanoTime();
	                
	                for (int attempt = 0; attempt < 500000000; attempt++) {
	                    long magic = rng.nextLong();
	                    Set<Integer> foundIndexes = new HashSet<Integer>();
	                    boolean success = true;

	                    for (int j = 0; j < numPermutations; j++) {
	                        long permutation = 0L;

	                        for (int k = 0; k < numBlockers; k++) {
	                            if (((j >> k) & 1L) != 0) {
	                                permutation |= (1L << blockerLocations[k]);
	                            }
	                        }

	                        long product = permutation * magic;
	                        int hashIndex = (int)((product >>> shift));

	                        if (foundIndexes.contains(hashIndex)) {
	                            success = false;
	                            if (j > bestAttempt) {
	                            	bestLong = magic;
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
	                        String progress = String.format("[%d][%s] %d / %d, (0x%sL) Attempt %d", 
	                            square, (isRook ? "Rook" : "Bishop"), bestAttempt, numPermutations, Long.toHexString(bestLong), attempt);
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
	        for (int i : squaresToSearch) {
	            if (calculatedRookMagics[i] != 0) {
	                writer.println(String.format("rook_magics[%d] = 0x%sL;", i, Long.toHexString(calculatedRookMagics[i])));
	            }
	        }
	        writer.println();
	        writer.println("Bishop Magics:");
	        for (int i : squaresToSearch) {
	            if (calculatedBishopMagics[i] != 0) {
	                writer.println(String.format("bishop_magics[%d] = 0x%sL;", i, Long.toHexString(calculatedBishopMagics[i])));
	            }
	        }
	        
	    } catch (IOException e) {
	        System.err.println("Error writing to file: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    // Still print to console for immediate feedback
	    System.out.println(String.format("Results for range [%s]. Rooks:", Arrays.toString(squaresToSearch)));
	    System.out.println(Arrays.toString(calculatedRookMagics));
	    System.out.println("___Bishops:_____");
	    System.out.println(Arrays.toString(calculatedBishopMagics));
	}
	
	public static int[] getSetBits(long bits) {
		int[] setBits = new int[64];
		int amount = 0;
		
		while (bits != 0) {
			int idx = Long.numberOfTrailingZeros(bits);
			setBits[amount++] = idx;
			bits &= bits - 1;
		}
		
		return Arrays.copyOf(setBits, amount);
	}
	
	public static long[] columnMasks = new long[64];
	public static long[][] pawnAttackMasks = new long[2][64];
	public static long[] queenMasks = new long[64];
	public static long[] rookAttackMasks = new long[64];
	public static long[] bishopAttackMasks = new long[64];
	public static long[] knightMasks = new long[64];
	
	public static void initMagicMasks() {
		// Column masks
		long column = (1L | 1L << 8 | 1L << 16 | 1L << 24 | 1L << 32 | 1L << 40 | 1L << 48 | 1L << 56);
		for (int square = 0; square < 64; square++) {
			int squareCol = square % 8;
			
			columnMasks[square % 8] = column << squareCol;
		}
		
		for (int square = 0; square < 64; square++) {
			int row = square / 8;
			int col = square % 8;
			
			// Pawns
			for (int c = 0; c <= 1; c++) {
				long mask = 0L;
				int moveDir = (c == 0) ? 1 : -1;
				
				if (withinBounds(row + moveDir, col - 1, 0, 7)) {
					mask |= (1L << ((row + moveDir) * 8 + (col - 1)));
				}
				
				if (withinBounds(row + moveDir, col + 1, 0 , 7)) {
					mask |= (1L << ((row + moveDir) * 8 + (col + 1)));
				}
				
				pawnAttackMasks[c][square] = mask;
			}
			
			// Queens
			long queenMask = 0L;
			int[][] queenDirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
			
			for (int[] direction : queenDirs) {
				for (int i = 1; i < 8; i++) {
					int targetRow = row + direction[0] * i;
					int targetCol = col + direction[1] * i;
					
					if (withinBounds(targetRow, targetCol, 0, 7)) {
						queenMask |= (1L << (targetRow * 8 + targetCol));
					} else break;
				}
			}
			
			// Knights
			long knightMask = 0L;
			int[][] knightDirs = {{-2, -1}, {-2, 1}, {2, -1}, {2, 1},
					{-1, -2}, {-1, 2}, {1, -2}, {1, 2}};
			
			for (int[] direction : knightDirs) {
				int targetRow = row + direction[0];
				int targetCol = col + direction[1];
				
				if (withinBounds(targetRow, targetCol, 0, 7)) {
					knightMask |= (1L << (targetRow * 8 + targetCol));
				}
 			}
			
			long bishopMask = 0L;
			int[][] bishopDirs = {{1, 1}, {-1, -1}, {-1, 1}, {1, -1}};
			for (int[] direction : bishopDirs) {
				for (int i = 1; i < 8; i++) {
					int targetRow = row + direction[0] * i;
					int targetCol = col + direction[1] * i;
					
					if (withinBounds(targetRow, targetCol, 0, 7)) {
						bishopMask |= (1L << (targetRow * 8 + targetCol));
					} else break;
				}
			}
			
			long rookMask = 0L;
			int[][] rookDirs = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
			for (int[] direction : rookDirs) {
				for (int i = 1; i < 8; i++) {
					int targetRow = row + direction[0] * i;
					int targetCol = col + direction[1] * i;
					
					if (withinBounds(targetRow, targetCol, 0, 7)) {
						rookMask |= (1L << (targetRow * 8 + targetCol));
					} else break;
				}
			}
			
			queenMasks[square] = queenMask;
			knightMasks[square] = knightMask;
			bishopAttackMasks[square] = bishopMask;
			rookAttackMasks[square] = rookMask;
			
			/*
			 * int test = 39; if (square == test) DebugRender.renderLong(queenMask); if
			 * (square == test) DebugRender.renderLong(knightMask); if (square == test)
			 * DebugRender.renderLong(bishopMask); if (square == test)
			 * DebugRender.renderLong(rookMask);
			 */
		}
	}
	
	private static long[][] lines = new long[64][64];
	public static void initPrecomputedLineBB() {
		for (byte start = 0; start < 64; start++) {
			for (byte end = 0; end < 64; end++) {
				lines[start][end] = generateLine(start, end);
			}
		}
	}
	
	private static long generateLine(byte squareStart, byte squareEnd) {
		long bitboard = 0L;
		
		byte startRow = (byte)(squareStart / 8);
		byte startCol = (byte)(squareStart % 8);
		
		byte endRow = (byte)(squareEnd / 8);
		byte endCol = (byte)(squareEnd % 8);
		
		int dx = (int)Math.signum(endRow - startRow);
		int dy = (int)Math.signum(endCol - startCol);
		int curRow = startRow + dx;
		int curCol = startCol + dy;
		
		//System.out.println(String.format("%d, %d, to %d, %d: [%d, %d]", startRow, startCol, endRow, endCol, dx, dy));
		
		if (startRow == endRow) {
			while (curCol != endCol) {
				bitboard |= (1L << (curRow * 8 + curCol));
				curCol += dy;
			}
		} else if (startCol == endCol) {
			while (curRow != endRow) {
				bitboard |= (1L << (curRow * 8 + curCol));
				curRow += dx;
			}
		} else {
			while (curRow != endRow && curCol != endCol) {
				bitboard |= (1L << (curRow * 8 + curCol));
				
				curRow += dx;
				curCol += dy;
			}
		}
		
		return bitboard;
	}
	
	public static long lineBB(byte squareStart, byte squareEnd) {
		return lines[squareStart][squareEnd];
	}
}