package engine;
// Utlizes the PSInterface.java
public class KeyToLegalMoves {
	public static final PSInterface[] pseudoMap = new PSInterface[] {
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoPawns(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoKnights(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoBishops(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoRooks(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoQueens(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoKings(row, col, color)),
		
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoPawns(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoKnights(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoBishops(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoRooks(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoQueens(row, col, color)),
		(row, col, color) -> LegalityCheck.legal(MoveGen.pseudoKings(row, col, color)),
	};
}