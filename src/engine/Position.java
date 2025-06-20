package engine;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import debug.DebugRender;
public class Position {
	// FOR GUI ONLY, NOT FOR ENGINE
	public static String[] guilookupBoard = {
	    "whiteRooks", "whiteKnights", "whiteBishops", "whiteQueens", "whiteKing", "whiteBishops", "whiteKnights", "whiteRooks",
	    "whitePawns", "whitePawns", "whitePawns", "whitePawns", "whitePawns", "whitePawns", "whitePawns", "whitePawns",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "blackPawns", "blackPawns", "blackPawns", "blackPawns", "blackPawns", "blackPawns", "blackPawns", "blackPawns",
	    "blackRooks", "blackKnights", "blackBishops", "blackQueens", "blackKing", "blackBishops", "blackKnights", "blackRooks"
	};
	
	public static byte[] engineLookup = {
	    4, 2, 3, 5, 6, 3, 2, 4,     // white back rank
	    1, 1, 1, 1, 1, 1, 1, 1,     // white pawns
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    7, 7, 7, 7, 7, 7, 7, 7,     // black pawns
	    10, 8, 9, 11, 12, 9, 8, 10  // black back rank
	};
	public static long[] bitboards = {
	    0L,                                      // 0 - no piece
	    0xFFL << 8,                              // 1 - whitePawns
	    (1L << 1) | (1L << 6),                   // 2 - whiteKnights
	    (1L << 2) | (1L << 5),                   // 3 - whiteBishops
	    (1L << 0) | (1L << 7),                   // 4 - whiteRooks
	    (1L << 3),                               // 5 - whiteQueens
	    (1L << 4),                               // 6 - whiteKing
	   
	    0xFFL << 48,                             // 7 - blackPawns
	    (1L << 57) | (1L << 62),                 // 8 - blackKnights
	    (1L << 58) | (1L << 61),                 // 9 - blackBishops
	    (1L << 56) | (1L << 63),                 // 10 - blackRooks
	    (1L << 59),                              // 11 - blackQueens
	    (1L << 60)                               // 12 - blackKing
	};
	
	public static long[] cardinalThreats = {
		0L, // whiteRookSliders - rooks | queens
		0L, // whiteBishopSliders - bishops | queens
		
		0L, // blackRookSliders - rooks | queens
		0L, // blackBishopSliders - bishops | queens
	};
	
	public static byte[] whitePromotions = {5, 4, 2, 3};
	public static byte[] blackPromotions = {11, 10, 8, 9};
	
	public static long whiteOccupied = 0L;
	public static long blackOccupied = 0L;
	public static long allOccupied = 0L;
	
	public static byte whiteKingPos = 4;
	public static byte blackKingPos = 60;
	public static byte sideToMove = 0;
	
	public static byte enPassantTarget = -1;
	public static byte enPassantColor = -1;
	
	public static boolean[] castlingRights = {
		true, // white castle short
		true, // white castle long
		true, // black castle short
		true // black castle long
	};
	
	public static String[] allNames = {"whitePawns", "whiteKnights", "whiteBishops", "whiteRooks", "whiteQueens", "whiteKing", "blackPawns", "blackKnights", "blackBishops", "blackRooks", "blackQueens", "blackKing"};
	
	public static HashMap<String, Byte> nameKeyConversion = new HashMap<String, Byte>();
	static {
		// 0 REPRESENTS NO BOARD, subtract index by 1 to access valid board in allNames;
		nameKeyConversion.put("whitePawns", (byte)1);
		nameKeyConversion.put("whiteKnights", (byte)2);
		nameKeyConversion.put("whiteBishops", (byte)3);
		nameKeyConversion.put("whiteRooks", (byte)4);
		nameKeyConversion.put("whiteQueens", (byte)5);
		nameKeyConversion.put("whiteKing", (byte)6);
		
		nameKeyConversion.put("blackPawns", (byte)7);
		nameKeyConversion.put("blackKnights", (byte)8);
		nameKeyConversion.put("blackBishops", (byte)9);
		nameKeyConversion.put("blackRooks", (byte)10);
		nameKeyConversion.put("blackQueens", (byte)11);
		nameKeyConversion.put("blackKing", (byte)12);
	}
	
	public static void initOccupancy() {
		for (int i = 0; i < 13; i++) {
			long board = bitboards[i];
			
			if (i <= 6) {
				whiteOccupied |= board;
			} else {
				blackOccupied |= board;
			}
		}
		
		allOccupied = whiteOccupied | blackOccupied;
		
		cardinalThreats[0] = bitboards[4] | bitboards[5];
		cardinalThreats[1] = bitboards[3] | bitboards[5];
		
		cardinalThreats[2] = bitboards[10] | bitboards[11];
		cardinalThreats[3] = bitboards[9] | bitboards[11];
	}
	
	public static int[][] getAttacks(byte color, boolean isInitialization) {
		String colorParam = (color == 0) ? "white" : "black";
		int[][] attackBoard = new int[64][];
		byte myKingPos = (byte)(color == 0 ? blackKingPos : whiteKingPos);
		
		int[] pieceLocations = (color == 0 ? MagicBitboards.getSetBits(whiteOccupied) : MagicBitboards.getSetBits(blackOccupied));
		
		for (int square : pieceLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			byte pieceType = engineLookup[square];
			
			int[] moves = null;
			if (pieceType == 1 || pieceType == 7) {
				moves = isInitialization ? 
						PrecompMoves.precomputedMoves.get(colorParam + "PawnCaptures")[square] :
						LegalityCheck.legal(PrecompMoves.precomputedMoves.get(colorParam + "PawnCaptures")[square]);
			} else {
				allOccupied &= ~(1L << myKingPos);
				moves = isInitialization ? 
					KeyToPseudoMoves.pseudoMap[pieceType - 1].apply(row, col, color, true) : 
					KeyToLegalMoves.pseudoMap[pieceType - 1].apply(row, col, color, true);
				
				allOccupied |= (1L << myKingPos);
			}
			
			for (int move : moves) {
				byte from = (byte)(move & 0x3F);
				byte to = (byte)((move >>> 6) & 0x3F);
				
				if (attackBoard[to] == null) attackBoard[to] = new int[0];
				int[] newArray = new int[attackBoard[to].length + 1];
				
				System.arraycopy(attackBoard[to], 0, newArray, 0, attackBoard[to].length);
				newArray[newArray.length - 1] = from;
				
				attackBoard[to] = newArray;
			}
			
		}
		
		return attackBoard;
	}
	
	public static int[] getAllLegalMoves(byte color) {
		int[] moves = new int[128];
		int moveCount = 0;
		byte rangeStart = (byte)((color == 0) ? 1 : 7);
		byte rangeMax = (byte)((color == 0) ? 6 : 12);
		
		long myOccupancy = (color == 0) ? whiteOccupied : blackOccupied;
		int[] pieceLocations = MagicBitboards.getSetBits(myOccupancy);
		
		for (int square : pieceLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			
			byte pieceType = engineLookup[square];
			
			if (pieceType >= rangeStart && pieceType <= rangeMax) {
				int[] subMoves = KeyToLegalMoves.pseudoMap[pieceType - 1].apply(row, col, color, false);
				System.arraycopy(subMoves, 0, moves, moveCount, subMoves.length);
				
				moveCount += subMoves.length;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[][][] attacks = new int[2][][];
	public static int[][] pins = new int[2][];
	
	public static Deque<byte[]> enPassantStack = new ArrayDeque<byte[]>();
	private static Deque<boolean[]> castleStack = new ArrayDeque<boolean[]>();
	private static Deque<long[]> zobristStack = new ArrayDeque<long[]>();

	public static boolean moveCausesCheck(int move) { // Checks if a move will cause a new check, before calling the actual move
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte color = (byte)((move >>> 31) & 1L);
		
		byte row = (byte)(to / 8);
		byte col = (byte)(to % 8);
		
		byte opponentKingPosition = (color == 0) ? blackKingPos : whiteKingPos;
		int[][] myAttacks = (color == 0) ? attacks[0] : attacks[1];
		
		boolean opponentCurrentlyInCheck = myAttacks[opponentKingPosition] != null;
		if (opponentCurrentlyInCheck) return false;
		
		int[] newMoves = KeyToLegalMoves.pseudoMap[originKey - 1].apply(row, col, color, false);
		for (int subMove : newMoves) {
			byte subTo = (byte)((subMove >>> 6) & 0x3F);
			
			if (subTo == opponentKingPosition) {
				return true;
			}
		}
		
		return false;
	}
	
	// Get attacks
	// Get Pins
	// set pins and then filter attacks with pins
	private static void updateAttackAndPins() {
		attacks[0] = Position.getAttacks((byte)0, false);
		attacks[1] = Position.getAttacks((byte)1, false);
		
		pins[0] = LegalityCheck.getPinnedPieces((byte)0);
		pins[1] = LegalityCheck.getPinnedPieces((byte)1);
	}
	
	public static void makeMove(int move, boolean isEngine) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte targetKey = (byte)((move >>> 16) & 0xF);
		byte promotionKey = (byte)((move >>> 22) & 0xF);
		
		byte color = (byte)((move >>> 31) & 1L);
		boolean isCapture = targetKey != 0;
		boolean isDoublePawn = ((byte)((move >>> 30) & 1L)) != 0;
		boolean isEnPassant = ((byte)(move >>> 29) & 1L) != 0;
		boolean isCastle = ((byte)(move >>> 28) & 1L) != 0;
		byte castleType = (byte)((move >>> 20) & 3L);
		
		byte savedEPTarget = enPassantTarget;
		boolean[] savedCTR = {
			castlingRights[0],
			castlingRights[1],
			castlingRights[2],
			castlingRights[3],
		};
		
		//System.out.println(String.format("Move, From: %d, To: %d, OriginKey %d, targetKey: %d, Double Pawn: %b, IsEnPassant: %b, isCastle: %b, castleType: %d Color: %d", from, to, originKey, targetKey, isDoublePawn, isEnPassant, isCastle, castleType, color));
		
		// StackData
		if (isEngine) {
			castleStack.add(new boolean[] {
				castlingRights[0],
				castlingRights[1],
				castlingRights[2],
				castlingRights[3],
			});
			
			enPassantStack.add(new byte[] {
				enPassantTarget, enPassantColor,	
			});
			
			zobristStack.add(new long[] {ZobristHash.hash});
		}
		
		// White: 0, Black: 1
		if (color == 0) {
			whiteOccupied &= ~(1L << from);
			whiteOccupied |= (1L << to);
			
			if (isCapture) {
				blackOccupied &= ~(1L << to);
				bitboards[targetKey] &= ~(1L << to);
			};
		} else {
			blackOccupied &= ~(1L << from);
			blackOccupied |= (1L << to);
			
			if (isCapture) {
				whiteOccupied &= ~(1L << to);
				bitboards[targetKey] &= ~(1L << to);
			}
		}
		
		allOccupied &= ~(1L << from);
		allOccupied |= (1L << to);
		
		// Specific Origin Board
		bitboards[originKey] &= ~(1L << from);
		bitboards[originKey] |= (1L << to);
		
		// Lookup Boards
		guilookupBoard[from] = "";
		guilookupBoard[to] = allNames[originKey - 1];
		
		engineLookup[from] = 0;
		engineLookup[to] = originKey;
		
		// King Move
		if (originKey == 6 || originKey == 12) {
			if (color == 0) {
				whiteKingPos = to;
				
				castlingRights[0] = false;
				castlingRights[1] = false;
			} else {
				blackKingPos = to;
				
				castlingRights[2] = false;
				castlingRights[3] = false;
			}
		}
		
		// Specials
		if (isDoublePawn) {
			byte moveDirection = (byte)((color == 0) ? 8 : -8);
			
			enPassantColor = color;
			enPassantTarget = (byte)(to - moveDirection);
		} else {enPassantTarget = -1;}
		
		if (isEnPassant) {
			byte moveDirection = (byte)((color == 0) ? 8 : -8);
			byte captureTarget = (byte)(to - moveDirection);
			byte opponentSPBoard = engineLookup[captureTarget];
			
			bitboards[opponentSPBoard] &= ~((1L << captureTarget));
			if (color == 0) {
				blackOccupied &= ~((1L << captureTarget));
			} else {whiteOccupied &= ~((1L << captureTarget));}
			
			allOccupied &= ~((1L << captureTarget));
			
			engineLookup[captureTarget] = 0;
			guilookupBoard[captureTarget] = "";
		}
		
		byte ourRookIndex = (byte)((color == 0) ? 4 : 10);
		String rookName = (color == 0) ? "whiteRooks" : "blackRooks";
		if (isCastle) {
			boolean isShortCastle = (castleType % 2 == 0);
			
			if (isShortCastle) {
				bitboards[ourRookIndex] &= ~(1L << (from + 3));
				bitboards[ourRookIndex] |= (1L << (from + 1));
				
				if (color == 0) {
					whiteOccupied &= ~(1L << (from + 3));
					whiteOccupied |= (1L << (from + 1));
				} else {
					blackOccupied &= ~(1L << (from + 3));
					blackOccupied |= (1L << (from + 1));
				}
				
				allOccupied |= (1L << (from + 1));
				allOccupied &= ~(1L << (from + 3));
				
				engineLookup[from + 3] = 0;
				engineLookup[from + 1] = ourRookIndex;
				
				guilookupBoard[from + 3] = "";
				guilookupBoard[from + 1] = rookName;
			} else {
				bitboards[ourRookIndex] &= ~(1L << from - 4);
				bitboards[ourRookIndex] |= (1L << from - 1);
				
				if (color == 0) {
					whiteOccupied &= ~(1L << (from - 4));
					whiteOccupied |= (1L << (from - 1));
				} else {
					blackOccupied &= ~(1L << (from - 4));
					blackOccupied |= (1L << (from - 1));
				}
				
				allOccupied |= (1L << (from - 1));
				allOccupied &= ~(1L << (from - 4));
				
				engineLookup[from - 4] = 0;
				engineLookup[from - 1] = ourRookIndex;
				
				guilookupBoard[from - 4] = "";
				guilookupBoard[from - 1] = rookName;
			}
			
			castlingRights[castleType] = false;
		}
		
		if (promotionKey != 0) {
			bitboards[originKey] &= ~(1L << to);
			bitboards[promotionKey] |= (1L << to);
			
			engineLookup[to] = promotionKey;
			guilookupBoard[to] = allNames[promotionKey - 1];
		}
		
		// Zobrist
		ZobristHash.updateZobrist(move, savedEPTarget, savedCTR);
		
		// Side to Move
		sideToMove = (byte)(1 - sideToMove);
		
		// Cardinal Updating
		cardinalThreats[0] = bitboards[4] | bitboards[5];
		cardinalThreats[1] = bitboards[3] | bitboards[5];
		
		cardinalThreats[2] = bitboards[10] | bitboards[11];
		cardinalThreats[3] = bitboards[9] | bitboards[11];
		
		updateAttackAndPins();
	}
	
	public static void unmakeMove(int move) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte targetKey = (byte)((move >>> 16) & 0xF);
		byte promotionKey = (byte)((move >>> 22) & 0xF);
		
		byte color = (byte)((move >>> 31) & 1L);
		boolean isCapture = targetKey != 0;
		boolean isDoublePawn = ((byte)((move >>> 30) & 1L)) != 0;
		boolean isEnPassant = ((byte)(move >>> 29) & 1L) != 0;
		boolean isCastle = ((byte)(move >>> 28) & 1L) != 0;
		byte castleType = (byte)((move >>> 20) & 3L);
		//System.out.println(String.format("Move, From: %d, To: %d, OriginKey %d, targetKey: %d, Double Pawn: %b, IsEnPassant: %b, isCastle: %b, castleType: %d Color: %d", from, to, originKey, targetKey, isDoublePawn, isEnPassant, isCastle, castleType, color));
		
		if (promotionKey != 0) {
			bitboards[promotionKey] &= ~(1L << to);
		}
		
		// White: 0, Black: 1
		if (color == 0) {
			whiteOccupied &= ~(1L << to);
			whiteOccupied |= (1L << from);
			
			if (isCapture) {
				blackOccupied |= (1L << to);
			} 
		} else {
			blackOccupied &= ~(1L << to);
			blackOccupied |= (1L << from);
			
			if (isCapture) {
				whiteOccupied |= (1L << to);	
			}
		}
		
		allOccupied |= (1L << from);
		allOccupied &= ~(1L << to);
		
		if (isCapture) {
			engineLookup[to] = targetKey;
			guilookupBoard[to] = allNames[targetKey - 1];
			
			bitboards[targetKey] |= (1L << to);
			allOccupied |= (1L << to);
		} else {
			engineLookup[to] = 0;
			guilookupBoard[to] = "";
		}
		
		// Specific Origin Board
		bitboards[originKey] &= ~(1L << to);
		bitboards[originKey] |= (1L << from);
		
		// Lookup Boards
		guilookupBoard[from] = allNames[originKey - 1];
		engineLookup[from] = originKey;

		// King Move
		if (originKey == 6 || originKey == 12) {
			if (color == 0) {
				whiteKingPos = from;
			} else blackKingPos = from;
		}
		
		// Specials
		if (isEnPassant) {
			byte moveDirection = (byte)((color == 0) ? 8 : -8);
			byte captureTarget = (byte)(to - moveDirection);
			byte opponentSPBoard = (byte)(color == 0 ? 7 : 1);
			
			bitboards[opponentSPBoard] |= ((1L << captureTarget));
			if (color == 0) {
				blackOccupied |= ((1L << captureTarget));
			} else {whiteOccupied |= ((1L << captureTarget));}
			
			allOccupied |= ((1L << captureTarget));
			
			engineLookup[captureTarget] = opponentSPBoard;
			guilookupBoard[captureTarget] = allNames[opponentSPBoard - 1];
		}
		
		byte ourRookIndex = (byte)((color == 0) ? 4 : 10);
		String rookName = (color == 0) ? "whiteRooks" : "blackRooks";
		if (isCastle) {
			boolean isShortCastle = (castleType % 2 == 0);
			
			if (isShortCastle) {
				bitboards[ourRookIndex] |= (1L << (from + 3));
				bitboards[ourRookIndex] &= ~(1L << (from + 1));
				
				if (color == 0) {
					whiteOccupied |= (1L << (from + 3));
					whiteOccupied &= ~(1L << (from + 1));
				} else {
					blackOccupied |= (1L << (from + 3));
					blackOccupied &= ~(1L << (from + 1));
				}
				
				allOccupied &= ~(1L << (from + 1));
				allOccupied |= (1L << (from + 3));
				
				engineLookup[from + 3] = ourRookIndex;
				engineLookup[from + 1] = 0;
				
				guilookupBoard[from + 3] = rookName;
				guilookupBoard[from + 1] = "";
			} else {
				bitboards[ourRookIndex] |= (1L << from - 4);
				bitboards[ourRookIndex] &= ~(1L << from - 1);
				
				if (color == 0) {
					whiteOccupied |= (1L << (from - 4));
					whiteOccupied &= ~(1L << (from - 1));
				} else {
					blackOccupied |= (1L << (from - 4));
					blackOccupied &= ~(1L << (from - 1));
				}
				
				allOccupied &= ~(1L << (from - 1));
				allOccupied |= (1L << (from - 4));
				
				engineLookup[from - 4] = ourRookIndex;
				engineLookup[from - 1] = 0;
				
				guilookupBoard[from - 4] = rookName;
				guilookupBoard[from - 1] = "";
			}
		}
		
		boolean[] castleData = castleStack.pop();
		castlingRights[0] = castleData[0];
		castlingRights[1] = castleData[1];
		castlingRights[2] = castleData[2];
		castlingRights[3] = castleData[3];
		
		byte[] epData = enPassantStack.pop();
		enPassantTarget = epData[0];
		enPassantColor = epData[1];
		
		// Side to Move and Zobrist
		sideToMove = (byte)(1 - sideToMove);
		ZobristHash.hash = zobristStack.pop()[0];
		
		// Cardinal Updating
		cardinalThreats[0] = bitboards[4] | bitboards[5];
		cardinalThreats[1] = bitboards[3] | bitboards[5];
		
		cardinalThreats[2] = bitboards[10] | bitboards[11];
		cardinalThreats[3] = bitboards[9] | bitboards[11];
		
		updateAttackAndPins();
	}
	
	// Debug
	
	public static void checkPositionValidity() {
		int[] whitePieceLocations = MagicBitboards.getSetBits(whiteOccupied);
		int[] blackPieceLocations = MagicBitboards.getSetBits(blackOccupied);
		
		for (int square : whitePieceLocations) {
			if ((allOccupied & (1L << square)) == 0) {
				System.err.println("All occupied and whiteOccupied mismatch detected");
			}
		}
		
		for (int square : blackPieceLocations) {
			if ((allOccupied & (1L << square)) == 0) {
				System.err.println("All occupied and blackOccupied mismatch detected");
			}
		}
		
		for (int i = 0; i < bitboards.length; i++) {
			long board = bitboards[i];
			int[] locations = MagicBitboards.getSetBits(board);
			String boardName = (i != 0) ? allNames[i - 1] : "";
			
			for (int square : locations) {
				if (engineLookup[square] != i) {
					System.err.println("Mismatch between " + boardName + " and engineLookup: " + engineLookup[square]);
				}
				
				if (guilookupBoard[square] != boardName) {
					System.err.println("Mismatch between " + boardName + " and guilookup: " + guilookupBoard[square]);
				}
			}
		}
	};
	
	
}