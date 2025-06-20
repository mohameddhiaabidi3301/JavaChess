package engine;

import debug.DebugRender;

public class EvaluateBoard {
	public static int[] valueMap = {
		0, // No Piece
		100, // whitePawns
		300, // whiteKnights
		300, // whiteBishops
		500, // whiteRooks
		900, // whiteQueens
		0, // whiteKing
		100, // blackPawns
		300, // blackKnights
		300, // blackBishops
		500, // blackRooks
		900, // blackQueens
		0 // blackKing
	};
	
	private static int[][] pst = {
			{}, // None
			{
				9, 9, 9, 9, 9, 9, 9, 9,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 1, 1, 2, 2, 1, 1, 1,
				0, 1, 2, 3, 3, 2, 1, 1,
				0, 1, 2, 3, 3, 2, 1, 1,
				0, 1, 1, 2, 2, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1,
				9, 9, 9, 9, 9, 9, 9, 9,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				0, 0, 0, 2, 2, 0, 0, 0,
				0, 0, 0, 2, 2, 0, 0, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 2, 0, 0, 0, 0, 2, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				1, 0, 0, 2, 2, 0, 0, 1,
				1, 0, 0, 2, 2, 0, 0, 1,
				0, 0, 2, 2, 2, 2, 0, 0,
				0, 2, 0, 0, 0, 0, 2, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
			},
			{
				0, 0, 0, 2, 0, 2, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 2, 0, 2, 0, 0,
			},
			{
				0, 0, 0, 2, 0, 2, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1, 1, 1,
				0, 0, 0, 2, 0, 2, 0, 0,
			},
			{
				2, 2, 2, 2, 2, 2, 2, 2,
				2, 2, 2, 2, 2, 2, 2, 2,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				2, 2, 2, 2, 2, 2, 2, 2,
				2, 2, 2, 2, 2, 2, 2, 2,
			},
	};
	
	public static int isGameOver() {
		boolean whiteCanMove = false;
		boolean blackCanMove = false;
		
		int[] whiteLocations = MagicBitboards.getSetBits(Position.whiteOccupied);
		int[] blackLocations = MagicBitboards.getSetBits(Position.blackOccupied);
		
		for (int square : whiteLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			
			if (KeyToLegalMoves.pseudoMap[Position.engineLookup[square] - 1].apply(row, col, (byte)0, false).length > 0) {
				whiteCanMove = true;
				break;
			}
		}
		
		for (int square : blackLocations) {
			byte row = (byte)(square / 8);
			byte col = (byte)(square % 8);
			
			if (KeyToLegalMoves.pseudoMap[Position.engineLookup[square] - 1].apply(row, col, (byte)0, false).length > 0) {
				blackCanMove = true;
				break;
			}
		}
		
		boolean whiteInCheck = Position.attacks[1][Position.whiteKingPos] != null;
		boolean blackInCheck = Position.attacks[0][Position.blackKingPos] != null;
		
		if (!whiteCanMove) return (whiteInCheck) ? -100000 : 0;
		if (!blackCanMove) return (blackInCheck) ? 100000 : 0;
		
		return -1; // Game Ongoing
	}
	
	public static int getEval() {		
		int whiteMaterialValue = 0;
		int blackMaterialValue = 0;
		
		short whitePSTBonus = 0;
		short blackPSTBonus = 0;
		
		int[] whiteLocations = MagicBitboards.getSetBits(Position.whiteOccupied);
		int[] blackLocations = MagicBitboards.getSetBits(Position.blackOccupied);
		
		for (int square : whiteLocations) {
			byte pieceType = Position.engineLookup[square];
			whiteMaterialValue += valueMap[pieceType];
			whitePSTBonus += pst[pieceType][square];
		}
		
		for (int square : blackLocations) {
			byte pieceType = Position.engineLookup[square];
			blackMaterialValue += valueMap[pieceType];
			blackPSTBonus += pst[pieceType - 6][square];
		}
		
		if (whiteLocations.length + blackLocations.length <= 14) { // Endgame
			int kingDistance = (
					(Math.abs(Position.whiteKingPos / 8) - Math.abs(Position.blackKingPos / 8))
					+ (Math.abs(Position.whiteKingPos % 8) - Math.abs(Position.blackKingPos % 8))
			) * 4;
			
			if (whiteMaterialValue - blackMaterialValue > 20) { // Whites winning
				whitePSTBonus -= kingDistance;
				blackPSTBonus += kingDistance;
			} else if (blackMaterialValue - whiteMaterialValue < -20) { // Blacks winning
				whitePSTBonus += kingDistance;
				blackPSTBonus -= kingDistance;
			}
		}
		
		return (whiteMaterialValue + whitePSTBonus) - (blackMaterialValue + blackPSTBonus);
	}
}