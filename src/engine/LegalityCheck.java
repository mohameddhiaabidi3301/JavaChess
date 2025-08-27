package engine;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;
import debug.DebugRender;
public class LegalityCheck {	
	private static boolean withinBounds(int row, int col, int rangeMin, int rangeMax) {
		if (row < rangeMin || row > rangeMax) return false;
		if (col < rangeMin || col > rangeMax) return false;
		
		return true;
	}
	
	private static final int[][] cardinals = {
		// Verticals
			{1, 0}, // N - 0
			{-1, 0}, // S - 1
		// Horizontals
			{0, 1}, // E - 2
			{0, -1}, // W - 3
		// Diagonals
			{1, 1}, // NE - 4
			{1, -1}, // NW - 5
			{-1, 1}, // SE - 6
			{-1, -1}, // SW - 7
	};
	
	private static long[][] kingBlockerMap = new long[64][8];
	private static int[][][] kingBlockerOrder = new int[64][8][64];
	public static void init() {
		for (int square = 0; square < 64; square++) {
			int row = square / 8;
			int col = square % 8;
			
			for (int directionIndex = 0; directionIndex < cardinals.length; directionIndex++) {
				int[] direction = cardinals[directionIndex];
				long newMask = 0L;
				
				int curRow = row + direction[0];
				int curCol = col + direction[1];
				int idx = 0;
				while (withinBounds(curRow, curCol, 0, 7)) {
					newMask |= (1L << (curRow * 8 + curCol));
					
					kingBlockerOrder[square][directionIndex][curRow * 8 + curCol] = idx;
					curRow += direction[0];
					curCol += direction[1];
					idx++;
				}
				
				kingBlockerMap[square][directionIndex] = newMask;
			}
		}
	}
	
	public static long getPinnedPieceBitboard(byte color, Position chessPosition) {
		byte accessColor = (byte)(color == 0 ? 6 : 0);
		byte myKingPos = (color == 0 ? chessPosition.whiteKingPos : chessPosition.blackKingPos);
		long opponentRayPieces = (chessPosition.bitboards[3 + accessColor] | chessPosition.bitboards[4 + accessColor] | chessPosition.bitboards[5 + accessColor]) & MagicBitboards.queenMasks[myKingPos];
		long allPinsMask = 0L;
		
		for (int square : MagicBitboards.getSetBits(opponentRayPieces)) {
			long ray = MagicBitboards.lineBB((byte)square, myKingPos) & chessPosition.allOccupied;
			long blockers = ray & chessPosition.allOccupied;
			
			if (Long.bitCount(blockers) == 1) {
				if ((blockers & (color == 0 ? chessPosition.whiteOccupied : chessPosition.blackOccupied)) != 0) {
					allPinsMask |= blockers;
				}
			}
		}
		
		return allPinsMask;
	}
	
	public static int[] getPinnedPieces(byte colorId, Position chessPosition) {
		int[] pinBoard = new int[64];
		Arrays.fill(pinBoard, -1);
		
		int kingPosition = (colorId == 0) ? chessPosition.whiteKingPos : chessPosition.blackKingPos;
		long myPieces = (colorId == 0) ? chessPosition.whiteOccupied : chessPosition.blackOccupied;
		
		for (int i = 0; i < cardinals.length; i++) {
			int[] direction = cardinals[i];
			
			long blockerMask = kingBlockerMap[kingPosition][i];
			long relevantMyPieces = blockerMask & myPieces;
			int[] myPiecesOnLine = MagicBitboards.getSetBits(relevantMyPieces);
			
			long attackerCardinals;
			if (direction[0] != 0 && direction[1] != 0) {
			    attackerCardinals = chessPosition.cardinalThreats[(colorId == 0) ? 3 : 1] & blockerMask;
			} else {
			    attackerCardinals = chessPosition.cardinalThreats[(colorId == 0) ? 2 : 0] & blockerMask;
			}
			
			int[] attackerPiecesOnLine = MagicBitboards.getSetBits(attackerCardinals);
			
			if (myPiecesOnLine.length > 0 && attackerPiecesOnLine.length > 0) {
				final int ix = i;
				
				int closestMy = myPiecesOnLine[0];
				int closestMyOrder = kingBlockerOrder[kingPosition][ix][closestMy];
				
				for (int location : myPiecesOnLine) {
					if (kingBlockerOrder[kingPosition][ix][location] < closestMyOrder) {
						closestMy = location;
						closestMyOrder = kingBlockerOrder[kingPosition][ix][location];
					}
				}
				
				int closestOpponent = attackerPiecesOnLine[0];
				int closestOpponentOrder = kingBlockerOrder[kingPosition][ix][closestOpponent];
				
				for (int location : attackerPiecesOnLine) {
					if (kingBlockerOrder[kingPosition][ix][location] < closestOpponentOrder) {
						closestOpponent = location;
						closestOpponentOrder = kingBlockerOrder[kingPosition][ix][location];
					}
				}
				
				long attackLine = MagicBitboards.lineBB((byte)closestMy, (byte)closestOpponent);
				
				if (kingBlockerOrder[kingPosition][ix][closestMy] < kingBlockerOrder[kingPosition][ix][closestOpponent]) {					
					if ((attackLine & chessPosition.allOccupied) == 0 && (MagicBitboards.lineBB((byte)kingPosition, (byte)closestMy) & chessPosition.allOccupied) == 0 ) {
						if (pinBoard[closestMy] != -1) {
							pinBoard[closestMy] = -2; // Set it to -2 since a double pinned piece cannot move
						} else if (pinBoard[closestMy] != -2) {
							pinBoard[closestMy] = closestOpponent;
						}
					}
				}
			}
		}
		
		return pinBoard;
	}
	
	public static int[] legal(int[] moveArray, Position chessPosition) {
		if (moveArray.length == 0) return moveArray;
		
		int[] filteredMoves = new int[moveArray.length];
		byte legalCount = 0;
		
		byte color = (byte)((moveArray[0] >>> 31) & 1);
		byte from = (byte)(moveArray[0] & 0x3F);
		byte pieceId = (byte)((moveArray[0] >>> 12) & 0xF);
		
		int kingPosition = (color == 0) ? chessPosition.whiteKingPos : chessPosition.blackKingPos;
		int[][] opponentAttacks = (color == 0) ? chessPosition.pseudolegalAttacks[1] : chessPosition.pseudolegalAttacks[0];
		boolean inCheck = opponentAttacks[kingPosition] != null;
		int[] kingAttackers = opponentAttacks[kingPosition];
		int[] pinnedPieces = chessPosition.pins[color];
		
		for (int i = 0; i < moveArray.length; i++) {
			int move = moveArray[i];
	
			byte to = (byte)((move >>> 6) & 0x3F);
			boolean isCastle = ((byte)(move >>> 28) & 1L) != 0;
			byte castleType = (byte)((move >>> 20) & 3L);
			
			if (pieceId == 6 || pieceId == 12) { // King Move
				if (opponentAttacks[to] != null) {
					continue;
				};
				
				if (isCastle) { // Cant castle through check or while in check
					if (inCheck) continue;
					
					if (castleType % 2 == 0) { // Castle Short
						if (opponentAttacks[from + 1] != null || opponentAttacks[from + 2] != null) {
							continue;
						}
					} else { // Castle Long
						if (opponentAttacks[from - 1] != null || opponentAttacks[from - 2] != null) {
							continue;
						}
					}
				}
			} else { // Non King Move
				// Double check, only king can move
				if (inCheck && kingAttackers.length > 1) {
					continue;
				}
				
				if (pinnedPieces[from] == -2) continue; // Piece Double Pinned
				if (pinnedPieces[from] != -1) { // Piece is Pinned, extra checks
					byte attackerLocation = (byte)pinnedPieces[from];
					long pinRayFront = MagicBitboards.lineBB(from, attackerLocation);
					long pinRayBehind = MagicBitboards.lineBB((byte)from, (byte)kingPosition);
					
					// Pinned piece must move along pin line or capture attacker
					if (((1L << to) & pinRayFront) == 0 && to != attackerLocation) {
						if (((1L << to) & pinRayBehind) == 0 && to != attackerLocation) {
							continue;
						}
					}
				}
				
				if (inCheck) { // Must capture piece or get itself in pin line
					if (to != kingAttackers[0]) {
						byte attackerType = chessPosition.engineLookup[kingAttackers[0]];
	
						if (attackerType % 6 != 3 && attackerType % 6 != 4 && attackerType % 6 != 5) {
							// Not a rook or bishop or queen type
							continue;
						} else {
							// Must be in the line of its vision
							long attackLine = MagicBitboards.lineBB((byte)kingPosition, (byte)kingAttackers[0]);
						
							if ((attackLine & (1L << to)) == 0) {
								continue;
							}
						}
					};
				}
			}
			
			filteredMoves[legalCount++] = move;
		}
		
		return Arrays.copyOf(filteredMoves, legalCount);
	}
}