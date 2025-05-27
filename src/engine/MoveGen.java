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
		
		int[] moves = new int[4];
		int moveCount = 0;
		
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
				moves[moveCount++] = pushMove;
			}
		}
		
		if (interEmpty == true && pendingDouble != -1) {
			moves[moveCount++] = pendingDouble;
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
				
				moves[moveCount++] = move;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[] pseudoKnights(byte row, byte col, String color) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves.get("knightMoves")[square];
		String originBoard = (color == "white") ? "whiteKnights" : "blackKnights";
		byte originBoardKey = (byte)Position.nameKeyConversion.get(originBoard);
		
		int[] moves = new int[precomputedMoves.length];
		int moveCount = 0;
		
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
			
			moves[moveCount++] = mainMove;
		}
		
		return moves;
	}
	
	public static int[] pseudoKings(byte row, byte col, String color) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves.get("kingMoves")[square];
		String originBoard = (color == "white") ? "whiteKing" : "blackKing";
		byte originBoardKey = (byte)Position.nameKeyConversion.get(originBoard);
		
		int[] moves = new int[precomputedMoves.length];
		int moveCount = 0;
		
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
			
			moves[moveCount++] = mainMove;
		}
		
		return moves;
	}
}


