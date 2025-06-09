package engine;

@FunctionalInterface
public interface PSInterface {
	int[] apply(byte row, byte col, String color);
}