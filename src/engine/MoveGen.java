package engine;
import java.util.HashMap;
import java.util.ArrayList;

public class MoveGen {
	private static HashMap<String, ArrayList<Integer>> precomputedMoves = new HashMap<String, ArrayList<Integer>>();
	
	public static void precomputeMoves() {
		precomputedMoves.put("whitePawnPushes", new ArrayList<Integer>(64));
		precomputedMoves.put("whitePawnCaptures", new ArrayList<Integer>(64));
		precomputedMoves.put("blackPawnPushes", new ArrayList<Integer>(64));
		precomputedMoves.put("blackPawnCaptures", new ArrayList<Integer>(64));
		
		precomputedMoves.put("knightMoves", new ArrayList<Integer>(64));
		precomputedMoves.put("kingMoves", new ArrayList<Integer>(64));
		
		// Pawns
		for (byte square = 0; square < 64; square++) {
			byte row = (byte)Math.floor(square / 8);
			byte col = (byte)((byte)square % 8);
			
			for (byte c = 0; c <= 1; c++) {
				String color = (c == 0) ? "white" : "black";
				byte moveDir = (byte)((color == "white") ? 1 : -1);
				byte promotionRow = (byte)((color == "white") ? 7 : 0);
				String pushKey = color + "PawnPushes";
				String captureKey = color + "PawnCaptures";
				
				if (row != promotionRow) {
					
				}
			}
		}
	}
	
	public static int[] pseudoPawns(byte row, byte col, String color) {
		
	}
}