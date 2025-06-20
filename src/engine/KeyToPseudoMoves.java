package engine;
// Utlizes the PSInterface.java
public class KeyToPseudoMoves {
	public static final PSInterface[] pseudoMap = new PSInterface[] {
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoPawns(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoKnights(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoBishops(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoRooks(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoQueens(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoKings(row, col, color, includeSelfCaptures)),
		
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoPawns(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoKnights(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoBishops(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoRooks(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoQueens(row, col, color, includeSelfCaptures)),
		(row, col, color, includeSelfCaptures) -> (MoveGen.pseudoKings(row, col, color, includeSelfCaptures)),
	};
}