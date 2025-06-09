package engine;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
	
	public static long whiteOccupied = 0L;
	public static long blackOccupied = 0L;
	public static long allOccupied = 0L;
	
	public static byte whiteKingPos = 4;
	public static byte blackKingPos = 60;
	
	public static Set<String> whiteNames = new HashSet<String>();
	public static Set<String> blackNames = new HashSet<String>();
	static {
		whiteNames.add("whitePawns"); // 0
		whiteNames.add("whiteKnights"); // 1
		whiteNames.add("whiteBishops"); // 2
		whiteNames.add("whiteRooks"); // 3
		whiteNames.add("whiteQueens"); // 4
		whiteNames.add("whiteKing"); // 5
		
		blackNames.add("blackPawns"); // 6
		blackNames.add("blackKnights"); // 7
		blackNames.add("blackBishops"); // 8
		blackNames.add("blackRooks"); // 9
		blackNames.add("blackQueens"); // 10
		blackNames.add("blackKing"); // 11
	}
	
	
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
		for (int i = 0; i < 12; i++) {
			long board = bitboards[i];
			if (i < 6) {
				whiteOccupied |= board;
			} else {
				blackOccupied |= board;
			}
		}
		
		allOccupied = whiteOccupied | blackOccupied;
	}
	
	public static void makeMove(int move) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 12) & 0xF);
		byte targetKey = (byte)((move >>> 16) & 0xF);
		byte color = (byte)((move >>> 31) & 1L);
		boolean isCapture = targetKey != 0;
		
		System.out.println(String.format("Move, From: %d, To: %d, OriginKey %d, targetKey: %d, Color: %d", from, to, originKey, targetKey, color));
		
		// White: 0, Black: 1
		if (color == 0) {
			whiteOccupied &= ~(1L << from);
			whiteOccupied |= (1L << to);
			
			if (isCapture) blackOccupied &= ~(1L << to); 
		} else {
			blackOccupied &= ~(1L << from);
			blackOccupied |= (1L << to);
			
			if (isCapture) whiteOccupied &= ~(1L << to); 
		}
		
		allOccupied &= ~(1L << from);
		allOccupied |= (1L << to);
		
		// Specific Origin Board
		bitboards[originKey] = bitboards[originKey - 1] & ~(1L << from);
		bitboards[originKey] = bitboards[originKey - 1] |= (1L << to);
		
		// Lookup Boards
		guilookupBoard[from] = "";
		guilookupBoard[to] = allNames[originKey - 1];
		
		engineLookup[from] = 0;
		engineLookup[to] = (byte)(originKey);
	}
}