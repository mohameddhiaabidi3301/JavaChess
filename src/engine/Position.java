package engine;
import java.util.HashMap;
import java.util.ArrayList;

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
	
	public static HashMap<String, Long> bitboards = new HashMap<String, Long>() {
		{
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
	};
	
	public static byte whiteKingPos = 4;
	public static byte blackKingPos = 60;
}