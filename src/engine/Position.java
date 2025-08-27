package engine;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import debug.DebugRender;
public class Position {
	// Static Fields, Global data
	public static final byte[] whitePromotions = {5, 4, 2, 3};
	public static final byte[] blackPromotions = {11, 10, 8, 9};
	
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
	
	private static final HashMap<Character, Integer> fenLookup = new HashMap<Character, Integer>();
	static {
		fenLookup.put('P', 1);
		fenLookup.put('N', 2);
		fenLookup.put('B', 3);
		fenLookup.put('R', 4);
		fenLookup.put('Q', 5);
		fenLookup.put('K', 6);
		
		fenLookup.put('p', 7);
		fenLookup.put('n', 8);
		fenLookup.put('b', 9);
		fenLookup.put('r', 10);
		fenLookup.put('q', 11);
		fenLookup.put('k', 12);
	}
	
	public static String logMove(int move) {
		if (move == -1) {
			return "Null Move";
		}
		
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte pieceType = (byte)((move >>> 12) & 0xF);
		byte captureType = (byte)((move >>> 16) & 0xF);
		
		byte ogRow = (byte) (from / 8);
		byte ogCol = (byte) (from % 8);
		
		byte toRow = (byte) (to / 8);
		byte toCol = (byte) (to % 8);
		
		return String.format("%s from (%d, %d) to (%d, %d), %s", 
				allNames[pieceType - 1], ogRow, ogCol, toRow, toCol, (captureType != 0) ? "Captures " + allNames[captureType - 1] : "");
	}
	
	// End of static fields
	public Position(String fenString) { // Initializes a Position w FEN (Use Clone Method for Engine)
		loadPositionFromFEN(fenString);
		initOccupancy();
		
		PrecompMoves.init();
		initializeZobristHash();
		
		pseudolegalAttacks[0] = getPseudoLegalAttacks((byte)0);
		pseudolegalAttacks[1] = getPseudoLegalAttacks((byte)1);
		
		pins[0] = LegalityCheck.getPinnedPieces((byte)0, this);
		pins[1] = LegalityCheck.getPinnedPieces((byte)1, this);
		
		attacks[0] = getAttacks((byte)0, false);
		attacks[1] = getAttacks((byte)1, false);
	}
	
	public Position() {}
	public Position clonePosition() { // Clones position, carries over only engine necessary parts
		Position pos = new Position();
		
		pos.zobristHash = zobristHash;
		pos.sideToMove = sideToMove;
		pos.enPassantTarget = enPassantTarget;
		pos.enPassantColor = enPassantColor;
		
		pos.castlingRights = new boolean[] {
			castlingRights[0],
			castlingRights[1],
			castlingRights[2],
			castlingRights[3],
		};
		
		pos.bitboards = new long[13];
		System.arraycopy(bitboards, 0, pos.bitboards, 0, 13);
		
		pos.engineLookup = new byte[64];
		System.arraycopy(engineLookup, 0, pos.engineLookup, 0, 64);
		
		// Enable to debug (visualize) the clone, not necessary in the engine
		//pos.guilookupBoard = new String[64];
		//System.arraycopy(guilookupBoard, 0, pos.guilookupBoard, 0, 64);
		
		pos.attacks = new int[2][][];
		pos.pins = new int[2][];
		
		for (int i = 0; i < attacks.length; i++) {
			pos.attacks[i] = new int[attacks[i].length][];
			
			for (int j = 0; j < attacks[i].length; j++) {
				if (attacks[i][j] == null) continue;
				
				pos.attacks[i][j] = new int[attacks[i][j].length];
				System.arraycopy(attacks[i][j], 0, pos.attacks[i][j], 0, attacks[i][j].length);
			}
		}
		
		for (int i = 0; i < pins.length; i++) {
			pos.pins[i] = new int[pins[i].length];
			System.arraycopy(pins[i], 0, pos.pins[i], 0, pos.pins[i].length);
		}
		
		pos.initOccupancy();
		
		return pos;
	}
	
	// FOR GUI ONLY, NOT FOR ENGINE
	public String[] guilookupBoard = {
		"", "", "", "", "", "", "", "",
		"", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	    "", "", "", "", "", "", "", "",
	};
	
	public byte[] engineLookup = {
		0, 0, 0, 0, 0, 0, 0, 0,     // empty
		0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	    0, 0, 0, 0, 0, 0, 0, 0,     // empty
	};
	
	public long[] bitboards = {
	    0L,                                      // 0 - no piece
	    0L,                         // 1 - whitePawns
	    0L,                   // 2 - whiteKnights
	    0L,                   // 3 - whiteBishops
	    0L,                   // 4 - whiteRooks
	    0L,                               // 5 - whiteQueens
	    0L,                               // 6 - whiteKing
	   
	    0L,                             // 7 - blackPawns
	    0L,                 // 8 - blackKnights
	    0L,                 // 9 - blackBishops
	    0L,                 // 10 - blackRooks
	    0L,                              // 11 - blackQueens
	    0L,                              // 12 - blackKing
	};
	
	public long[] cardinalThreats = {
		0L, // whiteRookSliders - rooks | queens
		0L, // whiteBishopSliders - bishops | queens
		
		0L, // blackRookSliders - rooks | queens
		0L, // blackBishopSliders - bishops | queens
	};
	
	public long zobristHash = 0L;
	
	public long whiteOccupied = 0L;
	public long blackOccupied = 0L;
	public long allOccupied = 0L;
	
	public byte whiteKingPos = 4;
	public byte blackKingPos = 60;
	public byte sideToMove = 0;
	
	public byte enPassantTarget = -1;
	public byte enPassantColor = -1;
	
	public boolean[] castlingRights = {
		false,
		false,
		false,
		false,
	};
	
	private void loadPositionFromFEN(String FEN) { // Loads the FEN string into this position instance, NOT to be used in engine
		// rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - Default Fen
		
		engineLookup = new byte[64];
		guilookupBoard = new String[64];
		for (int i = 0; i < bitboards.length; i++) bitboards[i] = 0L;
		
		String[] segments = FEN.split(" ");
		String[] boardState = segments[0].split("/");
		String moveColor = segments[1];
		String fenCastles = segments[2];
		
		int rowCount = 7;
		for (String row : boardState) {
			int curCol = 0;
			int strIndex = 0;

			while (curCol < 8 && strIndex < row.length()) {
			    char piece = row.charAt(strIndex++);

			    if (Character.isDigit(piece)) {
			        curCol += piece - '0'; // skip empty squares
			    } else {
			        int boardIndex = fenLookup.get(piece);
			        int square = rowCount * 8 + curCol;

			        bitboards[boardIndex] |= (1L << square);
			        engineLookup[square] = (byte)boardIndex;
			        guilookupBoard[square] = allNames[boardIndex - 1];
			        
			        if (boardIndex == 6) whiteKingPos = (byte)square;
			        if (boardIndex == 12) blackKingPos = (byte)square;

			        curCol++;
			    }
			}
			
			rowCount--;
		}
		
		if (moveColor.equals("w")) {
			sideToMove = 0;
		} else sideToMove = 1;
		
		if (fenCastles.indexOf('K') != -1) {
			castlingRights[0] = true;
		}
		
		if (fenCastles.indexOf('Q') != -1) {
			castlingRights[1] = true;
		}
		
		if (fenCastles.indexOf('k') != -1) {
			castlingRights[2] = true;
		}
		
		if (fenCastles.indexOf('q') != -1) {
			castlingRights[3] = true;
		}
	}
	
	private void initOccupancy() {
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
	
	public short whiteMoveCount = 0;
	public short blackMoveCount = 0;
	public long whiteAttackBitboard = 0L;
	public long blackAttackBitboard = 0L;
	public short whiteMaterialValue = 0;
	public short blackMaterialValue = 0;
	
	public int[][] getPseudoLegalAttacks(byte color) {
		int[][] attackBoard = new int[64][];
		byte myKingPos = (byte)(color == 0 ? blackKingPos : whiteKingPos);
		
		int[] pieceLocations = (color == 0 ? MagicBitboards.getSetBits(whiteOccupied) : MagicBitboards.getSetBits(blackOccupied));
		for (int square : pieceLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			byte pieceType = engineLookup[square];

			int[] moves = null;
			if (pieceType == 1 || pieceType == 7) {
				moves = PrecompMoves.precomputedMoves[color == 0 ? 2 : 3][square];	
			} else if (pieceType == 6 || pieceType == 12) {
				moves = PrecompMoves.precomputedMoves[5][square];
			} else {
				allOccupied &= ~(1L << myKingPos);
				moves = KeyToPseudoMoves.pseudoMap[pieceType - 1].apply(row, col, color, true, this);
				
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
	
	long[] psuedoAttacks = new long[2];
	long[] testLegalCaptures = new long[2]; // Filtered version of psuedoAttacks
	long[] testPins = new long[2];
	
	public long getAttackersForSquare(byte color, byte square) {
		long mask = 0L;
		
		for (int pieceLocation : MagicBitboards.getSetBits(color == 0 ? whiteOccupied : blackOccupied)) {
			byte type = (byte)(engineLookup[pieceLocation] % 6);
			
			if (type == 3 || type == 4 || type == 5) { // Sliding pieces
				if ((MagicBitboards.lineBB((byte)pieceLocation, square) & square) == 0) {
					mask |= (1L << pieceLocation);
				}
			} else if (type == 1 && ((MagicBitboards.pawnAttackMasks[color][square] & (1L << square)) != 0)) {
				mask |= (1L << pieceLocation);
			} else if (((MagicBitboards.globalMasks[type][square] & (1L << square)) != 0)) {
				mask |= (1L << pieceLocation);
			}
		};
		
		return mask;
	}
	
	public void updateAttacksTEST(byte color) {
		psuedoAttacks[color] = 0L;
		testLegalCaptures[color] = 0L;
		
		int[] pieceLocations = (color == 0 ? MagicBitboards.getSetBits(whiteOccupied) : MagicBitboards.getSetBits(blackOccupied));
		byte kingPos = (color == 0 ? whiteKingPos : blackKingPos);
		boolean inCheck = (psuedoAttacks[color] & (1L << kingPos)) != 0;
		byte ourAttacker = (byte)(inCheck ? MagicBitboards.getSetBits(getAttackersForSquare((byte)(1 - color), kingPos))[0] : -1);
		
		for (int square : pieceLocations) {
			byte type = (byte)(engineLookup[square] % 6);
			
			if (type == 3 || type == 4) { // Rooks and Bishops, magic bitboards
				long blockerMask = type == 3 ? MagicBitboards.bishopMasks[square] : MagicBitboards.rookMasks[square];
				byte shift = type == 3 ? MagicBitboards.bishopShifts[square] :  MagicBitboards.rookShifts[square];
				
				long relevantBlockers = blockerMask & allOccupied;
				long magic = type == 3 ? MagicBitboards.bishopMagics[square] : MagicBitboards.rookMagics[square];
				long product = (relevantBlockers * magic);
				int hash = (int)(product >>> shift);
				
				long attacks = type == 3 ? MagicBitboards.bishopBitTableLookup[square][hash] :
					MagicBitboards.rookBitTableLookup[square][hash];
				
				if (inCheck) {
					
				}
				
				psuedoAttacks[color] |= (attacks);
			} else if (type == 5) {
				long rookAttacks = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
				long bishopAttacks = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
				
				long queenAttacks = rookAttacks | bishopAttacks;
				psuedoAttacks[color] |= (queenAttacks);
			} else if (type == 1) {
				psuedoAttacks[color] |= MagicBitboards.pawnAttackMasks[color][square];
			} else {
				psuedoAttacks[color] |= MagicBitboards.globalMasks[type][square];
			}
		}
		
		DebugRender.renderLong(psuedoAttacks[color]);
	}
	
	public int[][] getAttacks(byte color, boolean isInitialization) {
		int[][] attackBoard = new int[64][];
		byte myKingPos = (byte)(color == 0 ? blackKingPos : whiteKingPos);
		
		int[] pieceLocations = (color == 0 ? MagicBitboards.getSetBits(whiteOccupied) : MagicBitboards.getSetBits(blackOccupied));
		if (color == 0) whiteMoveCount = 0; else blackMoveCount = 0;
		if (color == 0) whiteAttackBitboard = 0L; else blackAttackBitboard = 0L;
		if (color == 0) whiteMaterialValue = 0; else blackMaterialValue = 0;
		psuedoAttacks[color] = 0L;
		
		for (int square : pieceLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			byte pieceType = engineLookup[square];
			if (color == 0 && pieceType != 1 && pieceType != 7) whiteMaterialValue += EvaluateBoard.valueMap[pieceType]; else blackMaterialValue += EvaluateBoard.valueMap[pieceType];
			
			int[] moves = null;
			if (pieceType == 1 || pieceType == 7) {
				moves = isInitialization ? 
						PrecompMoves.precomputedMoves[color == 0 ? 2 : 3][square] :
						LegalityCheck.legal(PrecompMoves.precomputedMoves[color == 0 ? 2 : 3][square], this);
			} else if (pieceType == 6 || pieceType == 12) {
				moves = PrecompMoves.precomputedMoves[5][square];
			} else {
				allOccupied &= ~(1L << myKingPos);
				moves = isInitialization ? 
					KeyToPseudoMoves.pseudoMap[pieceType - 1].apply(row, col, color, true, this) : 
					KeyToLegalMoves.pseudoMap[pieceType - 1].apply(row, col, color, true, this);
				
				allOccupied |= (1L << myKingPos);
			}
			
			if (color == 0) whiteMoveCount += moves.length; else blackMoveCount += moves.length;
			
			for (int move : moves) {
				byte from = (byte)(move & 0x3F);
				byte to = (byte)((move >>> 6) & 0x3F);
				
				if (color == 0) whiteAttackBitboard |= (1L << to); else blackAttackBitboard |= (1L << to);
				
				if (attackBoard[to] == null) attackBoard[to] = new int[0];
				int[] newArray = new int[attackBoard[to].length + 1];
				
				System.arraycopy(attackBoard[to], 0, newArray, 0, attackBoard[to].length);
				newArray[newArray.length - 1] = from;
				
				attackBoard[to] = newArray;
			}
			
		}
		
		return attackBoard;
	}
	
	public int getSmallestAttacker(byte square, byte STM) {
		final int[] valueMap = EvaluateBoard.valueMap;
		int[] attackers = this.attacks[STM][square];
		int smallestAttacker = attackers[0];
		
		for (int i = 0; i < attackers.length; i++) {
			if (valueMap[engineLookup[attackers[i]]] < valueMap[engineLookup[smallestAttacker]]) {
				smallestAttacker = attackers[i];
			}
		}
		
		return smallestAttacker;
	}
	
	public int[] getAllLegalMoves(byte color) {
		int[] moves = new int[128];
		int moveCount = 0;

		long myOccupancy = (color == 0) ? whiteOccupied : blackOccupied;
		int[] pieceLocations = MagicBitboards.getSetBits(myOccupancy);
		
		for (int square : pieceLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			
			byte pieceType = engineLookup[square];
			
			int[] subMoves = KeyToLegalMoves.pseudoMap[pieceType - 1].apply(row, col, color, false, this);
			System.arraycopy(subMoves, 0, moves, moveCount, subMoves.length);
			
			moveCount += subMoves.length;
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public int[] getAllCapturesChecks(byte color) {
		int[] moves = new int[64];
		int moveCount = 0;
		
		long opponentOccupancy = (color == 0) ? blackOccupied : whiteOccupied;
		long myOccupied = (color == 0) ? whiteOccupied : blackOccupied;
		long myAttacks = (color == 0) ? whiteAttackBitboard : blackAttackBitboard;
		long captures = myAttacks & opponentOccupancy;
		
		// Checks
		byte opponentKingPos = color == 0 ? blackKingPos : whiteKingPos;
		byte myKingPos = color == 0 ? whiteKingPos : blackKingPos;
		
		for (int square : MagicBitboards.getSetBits(bitboards[color == 0 ? 3 : 9])) {
			long ourRange = MagicBitboards.bishopAttackMasks[square];
			long canCheckSquares = MagicBitboards.bishopAttackMasks[opponentKingPos];
			long checkingMoves = (ourRange & canCheckSquares) &~ allOccupied; // Exclude Captures
			
			for (int checkTarget : MagicBitboards.getSetBits(checkingMoves)) {
				if ((MagicBitboards.lineBB((byte)square, (byte)checkTarget) & allOccupied) != 0) continue;
				if ((MagicBitboards.lineBB((byte)checkTarget, (byte)opponentKingPos) & allOccupied) != 0) continue;
				
				int move = square;
				move |= (checkTarget << 6);
				move |= ((color == 0 ? 3 : 9) << 12);
				move |= (0 << 16); // Must not be a capture
				move |= (color << 31);
				
				moves[moveCount++] = move;
			}
		}
		
		for (int square : MagicBitboards.getSetBits(bitboards[color == 0 ? 4 : 10])) {
			long ourRange = MagicBitboards.rookAttackMasks[square];
			long canCheckSquares = MagicBitboards.rookAttackMasks[opponentKingPos];
			long checkingMoves = (ourRange & canCheckSquares) &~ allOccupied; // Exclude Captures
			
			for (int checkTarget : MagicBitboards.getSetBits(checkingMoves)) {
				if ((MagicBitboards.lineBB((byte)square, (byte)checkTarget) & allOccupied) != 0) continue;
				if ((MagicBitboards.lineBB((byte)checkTarget, (byte)opponentKingPos) & allOccupied) != 0) continue;
				
				int move = square;
				move |= (checkTarget << 6);
				move |= ((color == 0 ? 4 : 10) << 12);
				move |= (0 << 16); // Must not be a capture
				move |= (color << 31);
				
				moves[moveCount++] = move;
			}
		}
		
		int[] legalMoves = LegalityCheck.legal(Arrays.copyOf(moves, moveCount), this);
		moves = new int[64];
		System.arraycopy(legalMoves, 0, moves, 0, legalMoves.length);
		moveCount = legalMoves.length;
		
		// Captures
		for (int target : MagicBitboards.getSetBits(captures)) {
			int[] pieces = attacks[color][target];
			
			for (int location : pieces) {
				int move = location;
				move |= (target << 6);
				move |= (engineLookup[location] << 12);
				move |= (engineLookup[target] << 16);
				move |= (color << 31);
				
				if (engineLookup[location] == 1 || engineLookup[location] == 7) {
					move |= ((target == enPassantTarget ? 1 : 0) << 29);
					move |= (((target / 8 == 0 || target / 8 == 7) ? 1 : 0) << 27);
				}
				
				moves[moveCount++] = move;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public int[][][] attacks = new int[2][][];
	public int[][][] pseudolegalAttacks = new int[2][][];
	public int[][] pins = new int[2][];
	
	private Deque<byte[]> enPassantStack = new ArrayDeque<byte[]>();
	private Deque<boolean[]> castleStack = new ArrayDeque<boolean[]>();
	private Deque<long[]> zobristStack = new ArrayDeque<long[]>();

	public boolean moveCausesCheck(int move) { // Checks if a move will cause a new check, before calling the actual move
		if (move == -1) return false; 
		
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
		
		int[] newMoves = KeyToLegalMoves.pseudoMap[originKey - 1].apply(row, col, color, false, this);
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
	private void updateAttackAndPins() {
		attacks[0] = getAttacks((byte)0, false);
		attacks[1] = getAttacks((byte)1, false);
		
		pseudolegalAttacks[0] = getPseudoLegalAttacks((byte)0);
		pseudolegalAttacks[1] = getPseudoLegalAttacks((byte)1);
		
		pins[0] = LegalityCheck.getPinnedPieces((byte)0, this);
		pins[1] = LegalityCheck.getPinnedPieces((byte)1, this);
	}
	
	private void logCastleStack() {
		System.out.println("_______________________");
		System.out.println("Castle Stack Length: " + castleStack.size());
		
		for (boolean[] rights : castleStack) {
			System.out.println(Arrays.toString(rights));
		}
		System.out.println("_________________________");
	}
	
	public int lastGuiMove = -1;
	public void makeMove(int move, boolean isEngine) {
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
		boolean[] savedCTR = new boolean[] {
			castlingRights[0],
			castlingRights[1],
			castlingRights[2],
			castlingRights[3],
		};
		
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
			
			zobristStack.add(new long[] {zobristHash});
		} else {
			lastGuiMove = move;
		}
		
		// White: 0, Black: 1
		if (color == 0) {
			whiteOccupied &= ~(1L << from);
			whiteOccupied |= (1L << to);
			
			if (isCapture) {
				blackOccupied &= ~(1L << to);
				bitboards[targetKey] &= ~(1L << to);
				
				if (targetKey == 10 && to == 63) castlingRights[2] = false;
				if (targetKey == 10 && to == 56) castlingRights[3] = false;
			};
		} else {
			blackOccupied &= ~(1L << from);
			blackOccupied |= (1L << to);
			
			if (isCapture) {
				whiteOccupied &= ~(1L << to);
				bitboards[targetKey] &= ~(1L << to);
				
				if (targetKey == 4 && to == 0) castlingRights[1] = false;
				if (targetKey == 4 && to == 7) castlingRights[0] = false;
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
		
		// Rook Move
		if (originKey == 4 || originKey == 10) {
			if (color == 0) {
				if (from == 7) castlingRights[0] = false;
				if (from == 0) castlingRights[1] = false;
			} else if (color == 1) {
				if (from == 63) castlingRights[2] = false;
				if (from == 56) castlingRights[3] = false;
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
		}
		
		if (promotionKey != 0) {
			bitboards[originKey] &= ~(1L << to);
			bitboards[promotionKey] |= (1L << to);
			
			engineLookup[to] = promotionKey;
			guilookupBoard[to] = allNames[promotionKey - 1];
		}
		
		// Zobrist
		updateZobrist(move, savedEPTarget, savedCTR);
		
		// Side to Move
		sideToMove = (byte)(1 - sideToMove);
		
		// Cardinal Updating
		cardinalThreats[0] = bitboards[4] | bitboards[5];
		cardinalThreats[1] = bitboards[3] | bitboards[5];
		
		cardinalThreats[2] = bitboards[10] | bitboards[11];
		cardinalThreats[3] = bitboards[9] | bitboards[11];
		
		updateAttackAndPins();
		Minimax.repetitionHistory[Minimax.historyPly++] = zobristHash;
	}
	
	public void unmakeMove(int move) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte targetKey = (byte)((move >>> 16) & 0xF);
		byte promotionKey = (byte)((move >>> 22) & 0xF);
		
		byte color = (byte)((move >>> 31) & 1L);
		boolean isCapture = targetKey != 0;
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
		
		boolean[] castleData = castleStack.removeLast();
		castlingRights[0] = castleData[0];
		castlingRights[1] = castleData[1];
		castlingRights[2] = castleData[2];
		castlingRights[3] = castleData[3];
		
		byte[] epData = enPassantStack.removeLast();
		enPassantTarget = epData[0];
		enPassantColor = epData[1];
		
		// Side to Move and Zobrist
		sideToMove = (byte)(1 - sideToMove);
		zobristHash = zobristStack.removeLast()[0];
		
		// Cardinal Updating
		cardinalThreats[0] = bitboards[4] | bitboards[5];
		cardinalThreats[1] = bitboards[3] | bitboards[5];
		
		cardinalThreats[2] = bitboards[10] | bitboards[11];
		cardinalThreats[3] = bitboards[9] | bitboards[11];
		
		updateAttackAndPins();
		Minimax.historyPly--;
	}
	
	public void toggleNullMove() {
		sideToMove = (byte)(1 - sideToMove);
		zobristHash ^= whiteToMoveKey;
	}
	
	public int getEval() {		
		int whiteMaterialValue = 0;
		int blackMaterialValue = 0;
		int whitePST = 0;
		int blackPST = 0;
		int whiteBonus = 0;
		int blackBonus = 0;
		
		int[] whiteLocations = MagicBitboards.getSetBits(whiteOccupied);
		int[] blackLocations = MagicBitboards.getSetBits(blackOccupied);
		
		// Material and PST
		for (int square : whiteLocations) {
			byte pieceType = engineLookup[square];
			
			whiteMaterialValue += EvaluateBoard.valueMap[pieceType];
			whitePST += EvaluateBoard.pst[pieceType][square];
			
			if (pieceType == 1) { // Pawn Bonus for defending other pieces (especially pawns) (chain)
				whitePST += Long.bitCount(MagicBitboards.pawnAttackMasks[0][square] & whiteOccupied) * 2;
				whitePST += Long.bitCount(MagicBitboards.pawnAttackMasks[0][square] & bitboards[1]) * 2; // Extra bonus for pawns
				whitePST += (square / 8); // Encourage pawn pushes
			}
		}
		
		for (int square : blackLocations) {
			byte pieceType = engineLookup[square];
			
			blackMaterialValue += EvaluateBoard.valueMap[pieceType];
			blackPST += EvaluateBoard.pst[pieceType - 6][square];
			
			if (pieceType == 7) { // Pawn Bonus for defending other pieces (especially pawns) (chain)
				blackPST += Long.bitCount(MagicBitboards.pawnAttackMasks[0][square] & whiteOccupied) * 2;
				blackPST += Long.bitCount(MagicBitboards.pawnAttackMasks[0][square] & bitboards[1]) * 2; // Extra bonus for pawns
				blackPST += 7 - (square / 8);
			}
		}
		
		// Bonus for side to move and check
		if (sideToMove == 0) whiteBonus += 15; else blackBonus += 15;
		if ((whiteAttackBitboard & (1L << blackKingPos)) != 0) whiteBonus += 15;
		if ((blackAttackBitboard & (1L << whiteKingPos)) != 0) blackBonus += 10;
		
		// Encourage keeping the king safe, queen map can be used for open files
		int whiteKingSafety = 0;
		int blackKingSafety = 0;
		
		boolean isEndGame = (whiteMaterialValue + blackMaterialValue <= 1300);
		if (!isEndGame) {
			whiteKingSafety -= KeyToPseudoMoves.pseudoMap[5].apply((byte)(whiteKingPos / 8), (byte)(whiteKingPos % 8), (byte)0, false, this).length;
			blackKingSafety -= KeyToPseudoMoves.pseudoMap[5].apply((byte)(blackKingPos / 8), (byte)(blackKingPos % 8), (byte)1, false, this).length;
		}
		
		return 
			((whiteMaterialValue - blackMaterialValue) * 2) +
			((whitePST - blackPST) * 1) +
			((whiteBonus - blackBonus) * 1) +
			((whiteKingSafety - blackKingSafety) * 1)
		;
	}
	
	// ZOBRIST HASHING
	// Indexes matches the ones used in positions | bitboards
	private static long[][] zobristKeys = new long[13][];
	private static long[] enPassantKeys = new long[64];
	private static long[] castlingKeys = new long[4];
	public static long whiteToMoveKey; // Public so a null move can toggle the side to move
	
	public static void initGlobalZobristKeys() {
		Random rng = new Random();
				
		// Pieces Keys
		for (int i = 0; i < 13; i++) {
			zobristKeys[i] = new long[64];
			for (int j = 0; j < 64; j++) {
				zobristKeys[i][j] = rng.nextLong();
			}
		}
		
		// En-Passant Key
		for (int i = 0; i < 64; i++) {
			enPassantKeys[i] = rng.nextLong();
		}
		
		// Side to move Key
		whiteToMoveKey = rng.nextLong();
		
		// Castling Keys
		for (int i = 0; i < 4; i++) {
			castlingKeys[i] = rng.nextLong();
		}
	}
	
	public void initializeZobristHash() {
		// Initializing Hash
		int[] pieceLocations = MagicBitboards.getSetBits(allOccupied);
		for (int square : pieceLocations) {
			int pieceType = engineLookup[square];
			long hashValue = zobristKeys[pieceType][square];
			
			zobristHash ^= hashValue;
		}
		
		for (int castleRightIndex = 0; castleRightIndex < 4; castleRightIndex++) {
			if (castlingRights[castleRightIndex]) {
				zobristHash ^= castlingKeys[castleRightIndex];
			}
		}
		
		if (sideToMove == 0) {zobristHash ^= whiteToMoveKey;};
		if (enPassantTarget != -1) {zobristHash ^= enPassantKeys[enPassantTarget];};
	}
	
	public void updateZobrist(int move, int previousEnPassantTarget, boolean[] previousCTR) {
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
		
		if (promotionKey != 0) {
			zobristHash ^= zobristKeys[originKey][from];
			zobristHash ^= zobristKeys[promotionKey][to];
		} else {
			zobristHash ^= zobristKeys[originKey][from];
			zobristHash ^= zobristKeys[originKey][to];
		}
		
		if (isEnPassant) {
			byte captureSquare = (byte)((color == 0) ? to - 8 : to + 8);
			byte captureType = (byte)(color == 0 ? 7 : 1);
			zobristHash ^= zobristKeys[captureType][captureSquare];
		} else if (isCapture) {;
			zobristHash ^= zobristKeys[targetKey][to];
		}
		
		zobristHash ^= whiteToMoveKey;
		
		if (previousEnPassantTarget != -1) {
			zobristHash ^= enPassantKeys[previousEnPassantTarget];
		}
		if (enPassantTarget != -1) {
			zobristHash ^= enPassantKeys[enPassantTarget];
		}
		
		for (byte i = 0; i < 4; i++) {
			if (previousCTR[i]) { // Clear current keys
				zobristHash ^= castlingKeys[i];
			}
			
			if (castlingRights[i]) {
				zobristHash ^= castlingKeys[i];
			}
		}
		
		if (isCastle) {
			boolean isCastleShort = castleType % 2 == 0;
			byte rookType = (byte)((color == 0) ? 4 : 10);
			if (isCastleShort) {
				zobristHash ^= zobristKeys[rookType][from + 3];
				zobristHash ^= zobristKeys[rookType][from + 1];
			} else {
				zobristHash ^= zobristKeys[rookType][from - 4];
				zobristHash ^= zobristKeys[rookType][from - 1];
			}
		}
	}
	
	// Debug
	
	public void checkPositionValidity() {
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

