package engine;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Position {
	public static ArrayList<Object> lookupBoard = new ArrayList<Object>(64) {
		{
			add("whiteRooks"); add("whiteKnights"); add("whiteBishops"); add("whiteQueens"); add("whiteKing"); add("whiteBishops"); add("whiteKnights"); add("whiteRooks");
			add("whitePawns"); add("whitePawns"); add("whitePawns"); add("whitePawns"); add("whitePawns"); add("whitePawns"); add("whitePawns"); add("whitePawns");
			add(null); add(null); add(null); add(null); add(null); add(null); add(null); add(null);
			add(null); add(null); add(null); add(null); add(null); add(null); add(null); add(null);
			add(null); add(null); add(null); add(null); add(null); add(null); add(null); add(null);
			add(null); add(null); add(null); add(null); add(null); add(null); add(null); add(null);
			add("blackPawns"); add("blackPawns"); add("blackPawns"); add("blackPawns"); add("blackPawns"); add("blackPawns"); add("blackPawns"); add("blackPawns");
			add("blackRooks"); add("blackKnights"); add("blackBishops"); add("blackQueens"); add("blackKing"); add("blackBishops"); add("blackKnights"); add("blackRooks");
		}
	};
	
	public static HashMap<String, Long> bitboards = new HashMap<String, Long>();
	static {
		bitboards.put("whitePawns",   0xFFL << 8);
		bitboards.put("whiteRooks",   (1L << 0L) | (1L << 7L));
		bitboards.put("whiteKnights", (1L << 1L) | (1L << 6L));
		bitboards.put("whiteBishops", (1L << 2L) | (1L << 5L));
		bitboards.put("whiteQueens",  (1L << 3L));
		bitboards.put("whiteKing",    (1L << 4L));

		bitboards.put("blackPawns",   0XFFL << 48);
		bitboards.put("blackRooks",   (1L << 56L) | (1L << 63L));
		bitboards.put("blackKnights", (1L << 57L) | (1L << 62L));
		bitboards.put("blackBishops", (1L << 58L) | (1L << 61L));
		bitboards.put("blackQueens",  (1L << 59L));
		bitboards.put("blackKing",    (1L << 60L));
	}
	
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
		for (String boardName : bitboards.keySet()) {
			if (whiteNames.contains(boardName)) {
				whiteOccupied |= bitboards.get(boardName);
			} else {
				blackOccupied |= bitboards.get(boardName);
			}
		}
		
		allOccupied = whiteOccupied | blackOccupied;
	}
}