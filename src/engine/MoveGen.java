package engine;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class MoveGen {
	public static int[] pseudoPawns(byte row, byte col, String color) {
		byte square = (byte)(row * 8 + col);
		String originBoard = (color == "white") ? "whitePawns" : "blackPawns";
		
		String pushKey = (color == "white") ? "whitePawnPushes" : "blackPawnPushes";
		int[] pushes = PrecompMoves.precomputedMoves.get(pushKey)[square];
		
		String captureKey = (color == "white") ? "whitePawnCaptures" : "blackPawnCaptures";
		int[] captures = PrecompMoves.precomputedMoves.get(captureKey)[square];
		
		ArrayList<Integer> moves = new ArrayList<Integer>();
		boolean interEmpty = false;
		int pendingDouble = -1;
		
		byte promotionRow = (byte)((color == "white") ? 7 : 0);
		for (int move : pushes) {
			byte from = (byte)(move & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			if ((Position.allOccupied & (1L << to)) != 0) continue;
			
			byte originBoardKey = Position.nameKeyConversion.get(originBoard);
			byte targetBoardKey = 0; // Has to be zero, since if there's a capture its discarded
			
			byte dist = (byte)(Math.abs(to-from));
			byte moveDir = (byte)(color == "white" ? 1 : -1);
			
			if (dist > 8) {
				int doubleMove = move; // from
				doubleMove |= (originBoardKey << 16); // originBoard
				doubleMove |= (targetBoardKey << 20); // captured
				doubleMove |= (0000 << 24); // isPromotion
				doubleMove |= (0000 << 28); // isCastle
				doubleMove |= (0000 << 32); // isEnPassant
				
				
				pendingDouble = doubleMove;
			} else {
				int pushMove = move; // from
				pushMove |= (originBoardKey << 16);
				pushMove |= (targetBoardKey << 20);
				pushMove |= (0000 << 24);
				pushMove |= (0000 << 28);
				pushMove |= (0000 << 32);
				
				interEmpty = true;
				moves.add(pushMove);
			}
		}
		
		if (interEmpty == true && pendingDouble != -1) {
			moves.add(pendingDouble);
		}
		
		for (int move : captures) {
			byte from = (byte)(move & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			byte originBoardKey = Position.nameKeyConversion.get(originBoard);
			long opponentBits = (color == "white") ? Position.blackOccupied : Position.whiteOccupied;
			
			if ((opponentBits & (1L << to)) != 0) {
				byte targetBoardKey = Position.nameKeyConversion.get(Position.lookupBoard.get(to));
	
				move |= (originBoardKey << 16);
				move |= (targetBoardKey << 20);
				move |= (0000 << 24);
				move |= (0000 << 28);
				move |= (0000 << 32);
				
				moves.add(move);
			}
		}
		
		int[] primitiveMoves = new int[moves.size()];
		for (int i = 0; i < moves.size(); i++) {
			primitiveMoves[i] = moves.get(i);
		}
		
		for (int move : primitiveMoves) {
			byte from = (byte)(move & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			
			System.out.println(move);
			System.out.println(from + ", " + to);
		}
		
		return primitiveMoves;
	}
	
	public static int[] pseudoKnights(byte row, byte col, String color) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves.get("knightMoves")[square];
		String originBoard = (color == "white") ? "whiteKnights" : "blackKnights";
		byte originBoardKey = (byte)Position.nameKeyConversion.get(originBoard);
		
		ArrayList<Integer> moves = new ArrayList<Integer>();
		
		for (int move : precomputedMoves) {
			byte from = (byte)((move) & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			
			Object captured = (Object)Position.lookupBoard.get(to);
			byte captureKey = (captured != null) ? Position.nameKeyConversion.get(captured) : 0;
			
			int mainMove = move;
			mainMove |= (originBoardKey << 16);
			mainMove |= (captureKey << 20);
			mainMove |= (0000 << 24);
			mainMove |= (0000 << 28);
			mainMove |= (0000 << 32);
			
			moves.add(mainMove);
		}
		
		int[] primitiveMoves = new int[moves.size()];
		for (int i = 0; i < moves.size(); i++) {
			primitiveMoves[i] = moves.get(i);
			
			byte from = (byte)((primitiveMoves[i]) & 0x3f);
			byte to = (byte)((primitiveMoves[i] >>> 6) & 0x3f);
			
			System.out.println(from + ", " + to + ": " + primitiveMoves[i]);
		}
		
		return primitiveMoves;
	}
	
	public static int[] pseudoKings(byte row, byte col, String color) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves.get("kingMoves")[square];
		String originBoard = (color == "white") ? "whiteKing" : "blackKing";
		byte originBoardKey = (byte)Position.nameKeyConversion.get(originBoard);
		
		ArrayList<Integer> moves = new ArrayList<Integer>();
		
		for (int move : precomputedMoves) {
			byte from = (byte)((move) & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			
			Object captured = (Object)Position.lookupBoard.get(to);
			byte captureKey = (captured != null) ? Position.nameKeyConversion.get(captured) : 0;
			
			int mainMove = move;
			mainMove |= (originBoardKey << 16);
			mainMove |= (captureKey << 20);
			mainMove |= (0000 << 24);
			mainMove |= (0000 << 28);
			mainMove |= (0000 << 32);
			
			moves.add(mainMove);
		}
		
		int[] primitiveMoves = new int[moves.size()];
		for (int i = 0; i < moves.size(); i++) {
			primitiveMoves[i] = moves.get(i);
			
			byte from = (byte)((primitiveMoves[i]) & 0x3f);
			byte to = (byte)((primitiveMoves[i] >>> 6) & 0x3f);
			
			System.out.println(from + ", " + to + ": " + primitiveMoves[i]);
		}
		
		return primitiveMoves;
	}
}


