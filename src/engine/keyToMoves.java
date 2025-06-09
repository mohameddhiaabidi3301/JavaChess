package engine;

// Utlizes the PSInterface.java
public class keyToMoves {
	public static final PSInterface[] pseudoMap = new PSInterface[] {
		(row, col, color) -> MoveGen.pseudoPawns(row, col, color),
		(row, col, color) -> MoveGen.pseudoKnights(row, col, color),
		(row, col, color) -> MoveGen.pseudoBishops(row, col, color),
		(row, col, color) -> MoveGen.pseudoRooks(row, col, color),
		(row, col, color) -> MoveGen.pseudoQueens(row, col, color),
		(row, col, color) -> MoveGen.pseudoKings(row, col, color),
		
		(row, col, color) -> MoveGen.pseudoPawns(row, col, color),
		(row, col, color) -> MoveGen.pseudoKnights(row, col, color),
		(row, col, color) -> MoveGen.pseudoBishops(row, col, color),
		(row, col, color) -> MoveGen.pseudoRooks(row, col, color),
		(row, col, color) -> MoveGen.pseudoQueens(row, col, color),
		(row, col, color) -> MoveGen.pseudoKings(row, col, color),
	};
}