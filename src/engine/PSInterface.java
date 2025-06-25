package engine;

@FunctionalInterface
public interface PSInterface {
	int[] apply(byte row, byte col, byte color, boolean includeSelfCaptures, Position chessPosition);
}