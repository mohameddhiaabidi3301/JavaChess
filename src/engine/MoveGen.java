package engine;
import java.util.HashMap;
import debug.DebugRender;
import java.util.ArrayList;
import java.util.Arrays;
public class MoveGen {
	public static int[] pseudoPawns(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		byte square = (byte)(row * 8 + col);
		byte originBoardKey = (byte)((color == 0) ? 1 : 7);
		
		int[] pushes = PrecompMoves.precomputedMoves[color == 0 ? 0 : 1][square];
		int[] captures = PrecompMoves.precomputedMoves[color == 0 ? 2 : 3][square];
		
		int[] moves = new int[4];
		int moveCount = 0;
		
		boolean interEmpty = false;
		int pendingDouble = -1;
		
		byte promotionRow = (byte)((color == 0) ? 7 : 0);
		for (int move : pushes) {
			byte from = (byte)(move & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			byte targetRow = (byte)(to >> 3);
			
			if ((chessPosition.allOccupied & (1L << to)) != 0) continue;
			byte targetBoardKey = 0; // Has to be zero, since if there's a capture its discarded
			
			byte dist = (byte)(Math.abs(to-from));
			if (dist > 8) {
				int doubleMove = move; // from
				doubleMove |= (originBoardKey << 12); // originBoard
				doubleMove |= (targetBoardKey << 16); // captured
				
				doubleMove |= (1L << 30); // Is double pawn move
				doubleMove |= (color << 31); // Color Key
				
				
				pendingDouble = doubleMove;
			} else {
				int pushMove = move; // from
				pushMove |= (originBoardKey << 12);
				pushMove |= (targetBoardKey << 16);
				pushMove |= (color << 31);
				
				if (targetRow == promotionRow) pushMove |= (1L << 27); // Promotion Flag
				
				interEmpty = true;
				moves[moveCount++] = pushMove;
			}
		}
		
		if (interEmpty == true && pendingDouble != -1) {
			moves[moveCount++] = pendingDouble;
		}
		
		for (int move : captures) {
			byte to = (byte)((move >>> 6) & 0x3F);
			byte toRow = (byte)(to >> 3);
			
			long opponentBits = (color == 0) ? chessPosition.blackOccupied : chessPosition.whiteOccupied;
			boolean isEnPassant = (to == chessPosition.enPassantTarget);
			
			if ((opponentBits & (1L << to)) != 0 || (isEnPassant && chessPosition.enPassantColor != color)) {
				byte targetBoardKey = chessPosition.engineLookup[to];
	
				move |= (originBoardKey << 12);
				move |= (targetBoardKey << 16);
				
				if (toRow == promotionRow) move |= (1L << 27); // Promotion Flag
				
				move |= ((isEnPassant ? 1 : 0) << 29);
				move |= (color << 31);
				
				moves[moveCount++] = move;
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[] pseudoKnights(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves[4][square];
		byte originBoardKey = (byte)(color == 0 ? 2 : 8);
		
		int[] moves = new int[precomputedMoves.length];
		int moveCount = 0;
		long myOccupied = (color == 0) ? chessPosition.whiteOccupied : chessPosition.blackOccupied;
		
		for (int move : precomputedMoves) {
			byte from = (byte)((move) & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			byte captureKey = chessPosition.engineLookup[to];
			
			if ((myOccupied & (1L << to)) != 0 && !includeSelfCaptures) continue;
			
			int mainMove = move;
			mainMove |= (originBoardKey << 12);
			mainMove |= (captureKey << 16);
	
			mainMove |= (color << 31);
			
			moves[moveCount++] = mainMove;
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[] pseudoKings(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		byte square = (byte)(row * 8 + col);
		int[] precomputedMoves = PrecompMoves.precomputedMoves[5][square];
		byte originBoardKey = (byte)(color == 0 ? 6 : 12);
		
		int[] moves = new int[precomputedMoves.length + 2];
		int moveCount = 0;
		long myOccupied = (color == 0) ? chessPosition.whiteOccupied : chessPosition.blackOccupied;
		
		byte castleShortIndex = (byte)((color == 0) ? 0 : 2);
		byte castleLongIndex = (byte)(castleShortIndex + 1);
		
		for (int move : precomputedMoves) {
			byte from = (byte)((move) & 0x3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			byte captureKey = chessPosition.engineLookup[to];
			
			if ((myOccupied & (1L << to)) != 0 && !includeSelfCaptures) continue;
			
			int mainMove = move;
			mainMove |= (originBoardKey << 12);
			mainMove |= (captureKey << 16);
			mainMove |= (color << 31);
			
			moves[moveCount++] = mainMove;
		}
		
		long myRooks = (color == 0) ? chessPosition.bitboards[4] : chessPosition.bitboards[10];
		if ((row == 0 || row == 7) && (col == 4)) {
			if (chessPosition.castlingRights[castleShortIndex]) {
				byte expectedRookLocation = (byte)(square + 3);
				boolean rookAtLocation = ((myRooks & (1L << expectedRookLocation)) != 0);
				
				if (chessPosition.engineLookup[square + 1] == 0 && chessPosition.engineLookup[square + 2] == 0 && rookAtLocation) {
					int newMove = square;
					newMove |= ((square + 2) << 6);
					newMove |= (originBoardKey << 12);
					newMove |= (castleShortIndex << 20); // Castling type
					newMove |= (1 << 28); // Castling indicator
					
					newMove |= (color << 31);
					moves[moveCount++] = newMove;
				}
			}
			
			if (chessPosition.castlingRights[castleLongIndex]) {
				byte expectedRookLocation = (byte)(square - 4);
				boolean rookAtLocation = ((myRooks & (1L << expectedRookLocation)) != 0);
				
				if (chessPosition.engineLookup[square - 1] == 0 && chessPosition.engineLookup[square - 2] == 0 && chessPosition.engineLookup[square - 3] == 0 && rookAtLocation) {
					int newMove = square;
					newMove |= ((square - 2) << 6);
					newMove |= (originBoardKey << 12);
					newMove |= (castleLongIndex << 20); // Castling Type
					newMove |= (1 << 28); // Castling indicator
					
					newMove |= (color << 31);
					moves[moveCount++] = newMove;
				}
			}
		}
		
		return Arrays.copyOf(moves, moveCount);
	}
	
	public static int[] pseudoBishops(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		byte square = (byte)(row * 8 + col);
		long myOccupied = (color == 0) ? chessPosition.whiteOccupied : chessPosition.blackOccupied;
		
		long blockerMask = MagicBitboards.bishopMasks[square];
		byte shift = MagicBitboards.bishopShifts[square];
		
		long relevantBlockerMask = (blockerMask & chessPosition.allOccupied);
		
		long magic = MagicBitboards.bishopMagics[square];
		long product = (relevantBlockerMask * magic);
		int hashIndex = (int)(product >>> shift);
		
		int[] precomputedMoves = MagicBitboards.bishopLookupTable[square][hashIndex];
		int[] finalMoves = new int[precomputedMoves.length];
		byte moveCount = 0;
		
		byte fromKey = (byte)((color == 0) ? 3 : 9);
		
		for (int move : precomputedMoves) {
			byte to = (byte)((move >>> 6) & 0x3F);
			
			if ((myOccupied & (1L << to)) == 0 || includeSelfCaptures) {
				byte toKey = chessPosition.engineLookup[to];
				
				move |= (fromKey << 12);
				move |= (toKey << 16);
				
				move |= (color << 31);
				
				finalMoves[moveCount++] = move;
			}
		}
		
		return Arrays.copyOf(finalMoves, moveCount);
	}
	
	public static int[] pseudoRooks(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		byte square = (byte)(row * 8 + col);
		long myOccupied = (color == 0) ? chessPosition.whiteOccupied : chessPosition.blackOccupied;
		
		long blockerMask = MagicBitboards.rookMasks[square];
		byte shift = MagicBitboards.rookShifts[square];
		
		long relevantBlockerMask = blockerMask & chessPosition.allOccupied;
				
		long magic = MagicBitboards.rookMagics[square];
		long product = (relevantBlockerMask * magic);
		int hashIndex = (int)(product >>> shift);
		
		int[] precomputedMoves = MagicBitboards.rookLookupTable[square][hashIndex];
		int[] finalMoves = new int[precomputedMoves.length];
		byte moveCount = 0;
		
		byte fromKey = (byte)((color == 0) ? 4 : 10);
		
		for (int move : precomputedMoves) {
			byte to = (byte)((move >>> 6) & 0x3F);
			
			if ((myOccupied & (1L << to)) == 0 || includeSelfCaptures) {
				byte toKey = chessPosition.engineLookup[to];
				
				move |= (fromKey << 12);
				move |= (toKey << 16);
				
				move |= (color << 31);
				
				finalMoves[moveCount++] = move;
			}
		}
		
		return Arrays.copyOf(finalMoves, moveCount);
	}
	
	public static int[] pseudoQueens(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition) {
		int[] rookMoves = pseudoRooks(row, col, color, includeSelfCaptures, chessPosition);
		int[] bishopMoves = pseudoBishops(row, col, color, includeSelfCaptures, chessPosition);
		int[] bothMoves = new int[rookMoves.length + bishopMoves.length];
		
		int moveCount = 0;
		int fromKey = (color == 0) ? 5 : 11;
		for (int move : rookMoves) {
			move &= ~(0xF << 12);
			move |= (fromKey << 12);
			bothMoves[moveCount++] = move;
		}
		
		for (int move : bishopMoves) {
			move &= ~(0xF << 12);
			move |= (fromKey << 12);
			bothMoves[moveCount++] = move;
		}
		
		return Arrays.copyOf(bothMoves, moveCount);
	}
}