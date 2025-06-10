package engine;

import debug.DebugRender;

public class LegalityCheck {
	private static int getTerminalPoint(int vector1, int current) {
		if (vector1 == 1) return 7;
		if (vector1 == -1) return 0;
		
		return current;
	}
	
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

	public static void init() {
		for (int square = 0; square < 64; square++) {
			int row = square / 8;
			int col = square % 8;
			
			for (int directionIndex = 0; directionIndex < cardinals.length; directionIndex++) {
				int[] direction = cardinals[directionIndex];
				long newMask = 0L;
				
				int curRow = row + direction[0];
				int curCol = col + direction[1];
				while (withinBounds(curRow, curCol, 0, 7)) {
					newMask |= (1L << (curRow * 8 + curCol));
					
					curRow += direction[0];
					curCol += direction[1];
				}
				
				kingBlockerMap[square][directionIndex] = newMask;
			}
		}
		
		DebugRender.renderLong(kingBlockerMap[27][4]);
	}
}