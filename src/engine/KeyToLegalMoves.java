package engine;
// Utlizes the PSInterface.java
public class KeyToLegalMoves {
	public static final PSInterface[] pseudoMap = new PSInterface[] {
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoPawns(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoKnights(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoBishops(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoRooks(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoQueens(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoKings(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoPawns(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoKnights(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoBishops(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoRooks(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoQueens(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
		(row, col, color, includeSelfCaptures, chessPosition) -> LegalityCheck.legal(MoveGen.pseudoKings(row, col, color, includeSelfCaptures, chessPosition), chessPosition),
	};
}