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
		
		pins[0] = LegalityCheck.getPinnedPieces((byte)0, this);
		pins[1] = LegalityCheck.getPinnedPieces((byte)1, this);
		
		testPins[0] = LegalityCheck.getPinnedPieceBitboard((byte)0, this);
		testPins[1] = LegalityCheck.getPinnedPieceBitboard((byte)1, this);
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

	long[] psuedoAttacks = new long[2];
	long[][] squareToAttacks = new long[2][64];
	long[] testLegalCaptures = new long[2]; // Filtered version of psuedoAttacks
	long[] testPins = new long[2];
	
	byte whiteKingAttacker = -1;
	byte blackKingAttacker = -1;
	boolean whiteKingAttackerIsRay = false;
	boolean blackKingAttackerIsRay = false;
	
	boolean whiteInDoubleCheck = false;
	boolean blackInDoubleCheck = false;
	
	public void updateAttacksTEST(byte color) {
		psuedoAttacks[color] = 0L;
		testLegalCaptures[color] = 0L;
		squareToAttacks[color] = new long[64];
		
		if (color == 0) {
			blackKingAttacker = -1;
			blackInDoubleCheck = false;
			blackKingAttackerIsRay = false;
			whiteMaterialValue = 0;
		} else {
			whiteKingAttacker = -1;
			whiteInDoubleCheck = false;
			whiteKingAttackerIsRay = false;
			blackMaterialValue = 0;
		}
		
		int[] pieceLocations = (color == 0 ? MagicBitboards.getSetBits(whiteOccupied) : MagicBitboards.getSetBits(blackOccupied));
		byte kingPos = (color == 0 ? whiteKingPos : blackKingPos);
		byte opponentKingPos = (color == 0 ? blackKingPos : whiteKingPos);
		boolean inCheck = (psuedoAttacks[color] & (1L << kingPos)) != 0;
		byte ourAttacker = (color == 0 ? whiteKingAttacker : blackKingAttacker);
		boolean ourAttackerIsRay = (color == 0 ? whiteKingAttackerIsRay : blackKingAttackerIsRay);
		allOccupied ^= (1L << opponentKingPos);
		
		for (int square : pieceLocations) {
			byte type = (byte)(engineLookup[square] > 6 ? engineLookup[square] - 6 : engineLookup[square]);
			long pieceAttacks = 0L;
			long legalAttacks = 0L;

			if (type == 3 || type == 4) { // Rooks and Bishops, magic bitboards
				long blockerMask = type == 3 ? MagicBitboards.bishopMasks[square] : MagicBitboards.rookMasks[square];
				byte shift = type == 3 ? MagicBitboards.bishopShifts[square] :  MagicBitboards.rookShifts[square];
				
				long relevantBlockers = blockerMask & allOccupied;
				long magic = type == 3 ? MagicBitboards.bishopMagics[square] : MagicBitboards.rookMagics[square];
				long product = (relevantBlockers * magic);
				int hash = (int)(product >>> shift);
				
				pieceAttacks = (type == 3 ? MagicBitboards.bishopBitTableLookup[square][hash] :
					MagicBitboards.rookBitTableLookup[square][hash]);
			} else if (type == 5) { // Queens
				long rookAttacks = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
				long bishopAttacks = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
				
				long queenAttacks = rookAttacks | bishopAttacks;
				pieceAttacks = (queenAttacks);
			} else if (type == 1) { // Pawns
				pieceAttacks = MagicBitboards.pawnAttackMasks[color][square];
			} else { // Knights and Kings
				pieceAttacks = MagicBitboards.globalMasks[type][square];
			}

			legalAttacks = pieceAttacks;
			if (inCheck) {				
				if (type == 6) {
					legalAttacks |= (MagicBitboards.globalMasks[type][square] &~ (psuedoAttacks[1-color] | (color == 0 ? whiteOccupied : blackOccupied)));
				} else {
					legalAttacks &= ((ourAttackerIsRay ? MagicBitboards.lineBB(kingPos, ourAttacker) : 0));
					legalAttacks |= (((type == 1 ? MagicBitboards.pawnAttackMasks[color][square] : MagicBitboards.globalMasks[type][square])) &~ (color == 0 ? whiteOccupied : blackOccupied)) & (1L << ourAttacker);
				}
			} else if ((testPins[color] & (1L << square)) != 0) {
				legalAttacks &= MagicBitboards.specialLineBB(kingPos, (byte)square);
			}
			
			if (type == 6) legalAttacks &= ~psuedoAttacks[1-color];
			if ((pieceAttacks & (1L << opponentKingPos)) != 0) {
				boolean isRay = (type == 3 || type == 4 || type == 5);
				if (color == 0) {
					blackInDoubleCheck = blackKingAttacker != -1;
					
					blackKingAttacker = (byte)(square);
					blackKingAttackerIsRay = isRay;
				} else {
					whiteInDoubleCheck = whiteKingAttacker != -1;
					
					whiteKingAttacker = (byte)(square);
					whiteKingAttackerIsRay = isRay;
				}
			}
			
			psuedoAttacks[color] |= pieceAttacks;
			testLegalCaptures[color] |= legalAttacks;
			if (type != 1) {
				if (color == 0) whiteMaterialValue += EvaluateBoard.valueMap[engineLookup[square]]; else blackMaterialValue += EvaluateBoard.valueMap[engineLookup[square]];
			}
			
			for (int target : MagicBitboards.getSetBits(pieceAttacks)) {
				squareToAttacks[color][target] |= (1L << square);
			}
		}
		
		allOccupied ^= (1L << opponentKingPos);
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
	
	public int[] getLegalMovesForPiece(byte square, byte color) {
		int[] moves = new int[128];
		int moveCount = 0;

		long myOccupancy = (color == 0) ? whiteOccupied : blackOccupied;
		long opponentOccupancy = (color == 0) ? blackOccupied : whiteOccupied;
		int[] pieceLocations = MagicBitboards.getSetBits(myOccupancy);
		long debugBoard = 0L;
		
		byte kingPos = (color == 0 ? whiteKingPos : blackKingPos);
		byte opponentKingPos = (color == 0 ? blackKingPos : whiteKingPos);
		boolean inCheck = (psuedoAttacks[1 - color] & (1L << kingPos)) != 0;
		byte ourAttacker = (color == 0 ? whiteKingAttacker : blackKingAttacker);
		boolean ourAttackerIsRay = (color == 0 ? whiteKingAttackerIsRay : blackKingAttackerIsRay);
		boolean kingImobilized = color == 0 ? whiteInDoubleCheck : blackInDoubleCheck;
		
		byte mainType = engineLookup[square];
		byte subType = (byte)(mainType > 6 ? mainType - 6 : mainType);
		long psuedoMoves = 0L;
		
		if (subType == 3) {
			psuedoMoves = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
		} else if (subType == 4) {
			psuedoMoves = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
		} else if (subType == 5) {
			long rookAttacks = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
			long bishopAttacks = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
			
			psuedoMoves = rookAttacks | bishopAttacks;
		} else if (subType == 1) {
			psuedoMoves = MagicBitboards.pawnAttackMasks[color][square] & opponentOccupancy;
			
			byte moveDir = (byte)(color == 0 ? 8 : -8);
			if (((allOccupied & (1L << (square + moveDir))) == 0) && MagicBitboards.withinBounds((square + moveDir) / 8, 0, 0, 7)) {
				psuedoMoves |= (1L << (square + moveDir));

				if ((color == 0 && (square / 8 == 1)) || (color == 1 && (square / 8 == 6))) {
					if ((allOccupied & (1L << square + moveDir * 2)) == 0) {
						psuedoMoves |= (1L << (square + moveDir * 2));
					}
				}
			}
		} else {
			psuedoMoves = MagicBitboards.globalMasks[subType][square];
		}
		
		psuedoMoves &= ~(myOccupancy);
		if (inCheck) {				
			if (subType == 6) {
				psuedoMoves |= (MagicBitboards.globalMasks[subType][square] &~ (psuedoAttacks[1-color] | myOccupancy));
			} else {
				long savedMoves = psuedoMoves;
				psuedoMoves &= ((ourAttackerIsRay ? MagicBitboards.lineBB(kingPos, ourAttacker) : 0));
				psuedoMoves |= (savedMoves & (1L << ourAttacker));
			}
		} else if ((testPins[color] & (1L << square)) != 0) {
			psuedoMoves &= MagicBitboards.specialLineBB(kingPos, (byte)square);
		}
		
		byte castleShortIndex = (byte)((color == 0) ? 0 : 2);
		byte castleLongIndex = (byte)(castleShortIndex + 1);
		
		if (kingImobilized && subType != 6) psuedoMoves = 0L;
		if (subType == 6) {
			psuedoMoves &= ~(psuedoAttacks[1 - color]);
			
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			long myRooks = (color == 0) ? 4 : 10;
			
			if ((row == 0 || row == 7) && (col == 4) && !inCheck) {
				if (castlingRights[castleShortIndex]) {
					
					if ((psuedoAttacks[1 - color] & (1L << (square + 1))) == 0 && engineLookup[square + 1] == 0 && engineLookup[square + 2] == 0 && engineLookup[square + 3] == myRooks) {
						psuedoMoves |= ((1L << (square + 2)) &~ psuedoAttacks[1- color]);
					}
				}
				
				if (castlingRights[castleLongIndex]) {
					
					if ((psuedoAttacks[1 - color] & (1L << (square - 1))) == 0 && (psuedoAttacks[1 - color] & (1L << (square - 2))) == 0 && engineLookup[square - 1] == 0 && engineLookup[square - 2] == 0 && engineLookup[square - 3] == 0 && engineLookup[square - 4] == myRooks) {
						psuedoMoves |= ((1L << (square - 2)) &~ psuedoAttacks[1- color]);
					}
				}
			}
		}
		
		for (int target : MagicBitboards.getSetBits(psuedoMoves)) {
			int move = square;
			move |= (target << 6);
			move |= engineLookup[square] << 12;
			move |= engineLookup[target] << 16;
			move |= (color << 31);
			
			if (subType == 1) {
				if ((color == 0 && (target / 8 == 7)) || (color == 1 && (target / 8 == 0))) {
					move |= (1 << 27);
				}
			} else if (subType == 6) {
				byte dif = (byte)(target - square);
				
				if (dif == 2 || dif == -2) {
					move |= ((dif == 2 ? castleShortIndex : castleLongIndex) << 20);
					move |= (1 << 28);
				}
			}
			
			moves[moveCount++] = move;
			debugBoard |= (1L << target);
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public int[] getAllLegalMoves(byte color) {
		int[] moves = new int[128];
		int moveCount = 0;

		long myOccupancy = (color == 0) ? whiteOccupied : blackOccupied;
		long opponentOccupancy = (color == 0) ? blackOccupied : whiteOccupied;
		int[] pieceLocations = MagicBitboards.getSetBits(myOccupancy);
		long debugBoard = 0L;
		
		byte kingPos = (color == 0 ? whiteKingPos : blackKingPos);
		byte opponentKingPos = (color == 0 ? blackKingPos : whiteKingPos);
		boolean inCheck = (psuedoAttacks[1 - color] & (1L << kingPos)) != 0;
		byte ourAttacker = (color == 0 ? whiteKingAttacker : blackKingAttacker);
		boolean ourAttackerIsRay = (color == 0 ? whiteKingAttackerIsRay : blackKingAttackerIsRay);
		boolean kingImobilized = color == 0 ? whiteInDoubleCheck : blackInDoubleCheck;
		
		for (int square : pieceLocations) { 
			byte mainType = engineLookup[square];
			byte subType = (byte)(mainType > 6 ? mainType - 6 : mainType);
			long psuedoMoves = 0L;
			
			if (subType == 3) {
				psuedoMoves = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
			} else if (subType == 4) {
				psuedoMoves = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
			} else if (subType == 5) {
				long rookAttacks = MagicBitboards.rookBitTableLookup[square][(int)(((MagicBitboards.rookMasks[square] & allOccupied) * MagicBitboards.rookMagics[square]) >>> MagicBitboards.rookShifts[square])];
				long bishopAttacks = MagicBitboards.bishopBitTableLookup[square][(int)(((MagicBitboards.bishopMasks[square] & allOccupied) * MagicBitboards.bishopMagics[square]) >>> MagicBitboards.bishopShifts[square])];
				
				psuedoMoves = rookAttacks | bishopAttacks;
			} else if (subType == 1) {
				psuedoMoves = MagicBitboards.pawnAttackMasks[color][square] & opponentOccupancy;
				
				byte moveDir = (byte)(color == 0 ? 8 : -8);
				if (((allOccupied & (1L << (square + moveDir))) == 0) && MagicBitboards.withinBounds((square + moveDir) / 8, 0, 0, 7)) {
					psuedoMoves |= (1L << (square + moveDir));

					if ((color == 0 && (square / 8 == 1)) || (color == 1 && (square / 8 == 6))) {
						if ((allOccupied & (1L << square + moveDir * 2)) == 0) {
							psuedoMoves |= (1L << (square + moveDir * 2));
						}
					}
				}
			} else {
				psuedoMoves = MagicBitboards.globalMasks[subType][square];
			}
			
			psuedoMoves &= ~(myOccupancy);
			if (inCheck) {				
				if (subType == 6) {
					psuedoMoves |= (MagicBitboards.globalMasks[subType][square] &~ (psuedoAttacks[1-color] | myOccupancy));
				} else {
					long savedMoves = psuedoMoves;
					psuedoMoves &= ((ourAttackerIsRay ? MagicBitboards.lineBB(kingPos, ourAttacker) : 0));
					psuedoMoves |= (savedMoves & (1L << ourAttacker));
				}
			} else if ((testPins[color] & (1L << square)) != 0) {
				psuedoMoves &= MagicBitboards.specialLineBB(kingPos, (byte)square);
			}
			
			byte castleShortIndex = (byte)((color == 0) ? 0 : 2);
			byte castleLongIndex = (byte)(castleShortIndex + 1);
			
			if (kingImobilized && subType != 6) psuedoMoves = 0L;
			if (subType == 6) {
				psuedoMoves &= ~(psuedoAttacks[1 - color]);
				
				byte row = (byte)(square / 8);
				byte col = (byte)(square % 8);
				long myRooks = (color == 0) ? 4 : 10;
				
				if ((row == 0 || row == 7) && (col == 4) && !inCheck) {
					if (castlingRights[castleShortIndex]) {
						
						if ((psuedoAttacks[1 - color] & (1L << (square + 1))) == 0 && engineLookup[square + 1] == 0 && engineLookup[square + 2] == 0 && engineLookup[square + 3] == myRooks) {
							psuedoMoves |= ((1L << (square + 2)) &~ psuedoAttacks[1- color]);
						}
					}
					
					if (castlingRights[castleLongIndex]) {
						
						if ((psuedoAttacks[1 - color] & (1L << (square - 1))) == 0 && (psuedoAttacks[1 - color] & (1L << (square - 2))) == 0 && engineLookup[square - 1] == 0 && engineLookup[square - 2] == 0 && engineLookup[square - 3] == 0 && engineLookup[square - 4] == myRooks) {
							psuedoMoves |= ((1L << (square - 2)) &~ psuedoAttacks[1- color]);
						}
					}
				}
			}
			
			for (int target : MagicBitboards.getSetBits(psuedoMoves)) {
				int move = square;
				move |= (target << 6);
				move |= engineLookup[square] << 12;
				move |= engineLookup[target] << 16;
				move |= (color << 31);
				
				if (subType == 1) {
					if ((color == 0 && (target / 8 == 7)) || (color == 1 && (target / 8 == 0))) {
						move |= (1L << 27);
					}
				} else if (subType == 6) {
					byte dif = (byte)(target - square);
					
					if (dif == 2 || dif == -2) {
						move |= ((dif == 2 ? castleShortIndex : castleLongIndex) << 20);
						move |= (1 << 28);
					}
				}
				
				moves[moveCount++] = move;
				debugBoard |= (1L << target);
			}
		}
		
		//DebugRender.renderLong(debugBoard);
		return Arrays.copyOf(moves, moveCount);
	}
	
	public int[] getCapturesChecksPromotions(byte color) {
		int[] moves = new int[64];
		int moveCount = 0;
		
		long opponentOccupancy = (color == 0) ? blackOccupied : whiteOccupied;
		long myOccupied = (color == 0) ? whiteOccupied : blackOccupied;
		long myAttacks = testLegalCaptures[color];
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
				if ((testLegalCaptures[color] & (1L << checkTarget)) == 0) continue;
				
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
				if ((testLegalCaptures[color] & (1L << checkTarget)) == 0) continue;
				
				int move = square;
				move |= (checkTarget << 6);
				move |= ((color == 0 ? 4 : 10) << 12);
				move |= (0 << 16); // Must not be a capture
				move |= (color << 31);
				
				moves[moveCount++] = move;
			}
		}
		
		for (int square : MagicBitboards.getSetBits(bitboards[color == 0 ? 5 : 11])) {
			long ourRange = MagicBitboards.rookAttackMasks[square] | MagicBitboards.bishopAttackMasks[square];
			long canCheckSquares = MagicBitboards.rookAttackMasks[opponentKingPos] | MagicBitboards.bishopAttackMasks[opponentKingPos];
			long checkingMoves = (ourRange & canCheckSquares) &~ allOccupied; // Exclude Captures
			
			for (int checkTarget : MagicBitboards.getSetBits(checkingMoves)) {
				if ((MagicBitboards.lineBB((byte)square, (byte)checkTarget) & allOccupied) != 0) continue;
				if ((MagicBitboards.lineBB((byte)checkTarget, (byte)opponentKingPos) & allOccupied) != 0) continue;
				if ((testLegalCaptures[color] & (1L << checkTarget)) == 0) continue;
				
				int move = square;
				move |= (checkTarget << 6);
				move |= ((color == 0 ? 5 : 11) << 12);
				move |= (0 << 16); // Must not be a capture
				move |= (color << 31);
				
				moves[moveCount++] = move;
			}
		}
		
		for (int square : MagicBitboards.getSetBits(bitboards[color == 0 ? 1 : 7])) {
			int dir = (color == 0) ? 8 : -8;
			int subPromotionRow = (color == 0) ? 6 : 1;
			boolean onPromotionRow = (square / 8) == subPromotionRow;
			if (!onPromotionRow) continue;
			
			if ((((allOccupied & (1L << (square + dir))) == 0))) {
				int move = square;
				move |= ((square + dir) << 6);
				move |= ((color == 0 ? 1 : 7) << 12);
				move |= (0 << 16); // Cant be a capture
				move |= (1 << 27); // Promotion
				move |= (color << 31);
				moves[moveCount++] = move;
			}
	
			long pawnCaptures = MagicBitboards.pawnAttackMasks[color][square] & opponentOccupancy;
			for (int capt : MagicBitboards.getSetBits(pawnCaptures)) {
				int move = square;
				move |= (capt << 6);
				move |= ((color == 0 ? 1 : 7) << 12);
				move |= (engineLookup[capt] << 16);
				move |= (1 << 27); // promotion
				move |= (color << 31);
				moves[moveCount++] = move;
			}
		}
		
		// Captures
		for (int squareAttacked : MagicBitboards.getSetBits(testLegalCaptures[color] & opponentOccupancy)) {
			long attackers = squareToAttacks[color][squareAttacked];
			
			for (int origin : MagicBitboards.getSetBits(attackers)) {
				int move = origin;
				move |= (squareAttacked << 6);
				move |= (engineLookup[origin] << 12);
				move |= (engineLookup[squareAttacked] << 16);
				move |= (color << 31);
				
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
		updateAttacksTEST((byte)0);
		updateAttacksTEST((byte)1);
		
		testPins[0] = LegalityCheck.getPinnedPieceBitboard((byte)0, this);
		testPins[1] = LegalityCheck.getPinnedPieceBitboard((byte)1, this);		
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
	public int[] moveHistory = new int[256];
	public int currentMoveCount = 0;
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
				bitboards[ourRookIndex] &= ~(1L << (from - 4));
				bitboards[ourRookIndex] |= (1L << (from - 1));
				
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
		moveHistory[currentMoveCount++] = move;
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
		currentMoveCount--;
	}
	
	public void toggleNullMove() {
		sideToMove = (byte)(1 - sideToMove);
		zobristHash ^= whiteToMoveKey;
	}
	
	public int getEval(int side) {		
		int whiteMaterialValue = 0;
		int blackMaterialValue = 0;

		int[] whiteLocations = MagicBitboards.getSetBits(whiteOccupied);
		int[] blackLocations = MagicBitboards.getSetBits(blackOccupied);
		
		int phase = 0;
		int score = 0;
		
		// Material and phase calculation
		for (int square : whiteLocations) {
			byte pieceType = engineLookup[square];
			
			score += EvaluateBoard.valueMap[pieceType];	
			phase += EvaluateBoard.phaseMap[pieceType];
		}
		
		for (int square : blackLocations) {
			byte pieceType = engineLookup[square];
			
			score -= EvaluateBoard.valueMap[pieceType];
			phase += EvaluateBoard.phaseMap[pieceType - 6];
		}
		
		return side == 0 ? score : -score;
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


