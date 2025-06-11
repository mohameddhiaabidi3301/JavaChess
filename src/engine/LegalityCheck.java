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
		// Horizontals
			{1, 0}, // E - 0
			{-1, 0}, // W - 1
		// Verticals
			{0, 1}, // N - 2
			{0, -1}, // S - 3
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
	
	public static int[] getPinnedPieces(byte colorId) {
		int[] pins = new int[64];
		Arrays.fill(pins, -1);
		
		int kingPosition = (colorId == 0) ? Position.whiteKingPos : Position.blackKingPos;
		int[][] opponentAttacks = (colorId == 0) ? Position.attacks[1] : Position.attacks[0];
		long myPieces = (colorId == 0) ? Position.whiteOccupied : Position.blackOccupied;
		
		System.out.println(Arrays.toString(opponentAttacks[30]));
		
		for (int i = 0; i < cardinals.length; i++) {
			int[] direction = cardinals[i];
			
			long blockerMask = kingBlockerMap[kingPosition][i];
			long relevantMyPieces = blockerMask & myPieces;
			int[] myPiecesOnLine = MagicBitboards.getSetBits(relevantMyPieces);
			
			long attackerCardinals;
			if (direction[0] != 0 && direction[1] != 0) {
			    attackerCardinals = Position.cardinalThreats[(colorId == 0) ? 3 : 1] & blockerMask;
			} else {
			    attackerCardinals = Position.cardinalThreats[(colorId == 0) ? 2 : 0] & blockerMask;
			}
			
			int[] attackerPiecesOnLine = MagicBitboards.getSetBits(attackerCardinals);
			
			if (myPiecesOnLine.length > 0 && attackerPiecesOnLine.length > 0) {
				final int ix = i;
				int[] sortedMy = Arrays.stream(myPiecesOnLine).boxed()
				        .sorted(Comparator.comparingInt(piece -> kingBlockerOrder[kingPosition][ix][piece]))
				        .mapToInt(j -> j)
				        .toArray();
				
				int[] sortedOpponent = Arrays.stream(attackerPiecesOnLine).boxed()
						.sorted(Comparator.comparingInt(piece -> kingBlockerOrder[kingPosition][ix][piece]))
						.mapToInt(j -> j)
						.toArray();
				
				
				byte closestMy = (byte) sortedMy[0];
				byte closestOpponent = (byte)sortedOpponent[0];
				long attackLine = MagicBitboards.lineBB(closestMy, closestOpponent);
				
				if (kingBlockerOrder[kingPosition][ix][closestMy] < kingBlockerOrder[kingPosition][ix][closestOpponent]) {
					if ((attackLine & Position.allOccupied) == 0 && (MagicBitboards.lineBB((byte)kingPosition, (byte)closestMy) & Position.allOccupied) == 0 ) {
						if (pins[closestMy] != -1) {
							pins[closestMy] = -2; // Set it to -2 since a double pinned piece cannot move
						} else if (pins[closestMy] != -2) {
							pins[closestMy] = closestOpponent;
						}

						System.out.println(String.format("Detected pin for color %d, at square %d: %s, Attacker: %s", colorId, closestMy, Position.guilookupBoard[closestMy], Position.guilookupBoard[closestOpponent]));
					}
				}
			}
		}
		
		return pins;
	}
	
	public static int[] legal(int[] moveArray) {
		int[] filteredMoves = new int[moveArray.length];
		byte legalCount = 0;
		
		for (int i = 0; i < moveArray.length; i++) {
			int move = moveArray[i];
			byte from = (byte)(move & 0X3F);
			byte to = (byte)((move >>> 6) & 0x3F);
			byte pieceId = (byte)((move >>> 12) & 0xF);
			byte color = (byte)((move >>> 31) & 1L);
			
			int kingPosition = (color == 0) ? Position.whiteKingPos : Position.blackKingPos;
			int[][] opponentAttacks = (color == 0) ? Position.attacks[1] : Position.attacks[0];
			long myPieces = (color == 0) ? Position.whiteOccupied : Position.blackOccupied;
			
			boolean inCheck = opponentAttacks[kingPosition] != null;
			int[] kingAttackers = opponentAttacks[kingPosition];
			int[] pinnedPieces = Position.pins[color];
			
			if (pieceId == 6 || pieceId == 12) { // King Move
				if (opponentAttacks[to] != null) {
					continue;
				};
			} else { // Non King Move
				// Double check, only king can move
				if (inCheck && kingAttackers.length > 1) {
					continue;
				}
				
				if (pinnedPieces[from] == -2) continue; // Piece Double Pinned
				if (pinnedPieces[from] != -1) { // Piece is Pinned, extra checks
					byte attackerLocation = (byte)pinnedPieces[from];
					long pinRay = MagicBitboards.lineBB(from, attackerLocation);
					
					// Pinned piece must move along pin line or capture attacker
					if (((to & pinRay) == 0) && to != attackerLocation) continue;
				}
				
				if (inCheck) { // Must capture piece or get itself in pin line
					if (to != kingAttackers[0]) {
						byte attackerType = Position.engineLookup[kingAttackers[0]];
						System.out.println(attackerType);
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