package engine;
// Utlizes the PSInterface.java
public class KeyToPseudoMoves {
	public static final PSInterface[] pseudoMap = new PSInterface[] {
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoPawns(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoKnights(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoBishops(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoRooks(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoQueens(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoKings(row, col, color, includeSelfCaptures, chessPosition)),
		
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoPawns(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoKnights(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoBishops(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoRooks(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoQueens(row, col, color, includeSelfCaptures, chessPosition)),
		(row, col, color, includeSelfCaptures, chessPosition) -> (MoveGen.pseudoKings(row, col, color, includeSelfCaptures, chessPosition)),
	};
}