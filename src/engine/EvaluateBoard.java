package engine;

import debug.DebugRender;

public class EvaluateBoard {
	public static int[] valueMap = {
		0, // No Piece
		100, // whitePawns
		300, // whiteKnights
		310, // whiteBishops
		500, // whiteRooks
		900, // whiteQueens
		0, // whiteKing
		100, // blackPawns
		300, // blackKnights
		310, // blackBishops
		500, // blackRooks
		900, // blackQueens
		0 // blackKing
	};
	
	private static int[][] pst = {
			{}, // None
			{
				9, 9, 9, 9, 9, 9, 9, 9,
				1, 1, 1, 1, 1, 2, 2, 2,
				0, 1, 1, 2, 2, 1, 1, 1,
				0, 1, 3, 4, 4, 3, 1, 1,
				0, 1, 3, 4, 4, 3, 1, 1,
				0, 1, 1, 2, 2, 1, 1, 1,
				2, 2, 2, 1, 1, 2, 2, 2,
				9, 9, 9, 9, 9, 9, 9, 9,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 1, 1, 0, 0, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				0, 0, 0, 1, 1, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 2, 0, 0, 0, 0, 2, 0,
				0, 0, 2, 2, 2, 2, 0, 0,
				1, 1, 1, 2, 2, 1, 1, 1,
				1, 1, 1, 2, 2, 1, 1, 1,
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
				0, 0, 0, 0, 0, 0, 0, 0,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 1, 2, 2, 1, 0, 0,
				1, 1, 1, 2, 2, 1, 1, 1,
				1, 1, 1, 2, 2, 1, 1, 1,
				0, 0, 1, 2, 2, 1, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
			},
			{
				2, 3, 5, 2, 1, 2, 5, 3,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				2, 3, 5, 2, 1, 2, 5, 3,
			},
			{
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 2, 1, 1, 1, 1, 2, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 1, 2, 2, 2, 2, 1, 0,
				0, 2, 1, 1, 1, 1, 2, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
			}
	};
	
	public static int getEval() {		
		int whiteMaterialValue = 0;
		int blackMaterialValue = 0;
		
		short whitePSTBonus = 0;
		short blackPSTBonus = 0;
		short whiteMobilityBonus = Position.whiteMoveCount; // These include defending own pieces which is also good
		short blackMobilityBonus = Position.blackMoveCount;
		
		short whiteKingSafetyScore = 0;
		short blackKingSafetyScore = 0;
		
		whiteKingSafetyScore += Long.bitCount(Position.whiteOccupied & PrecompMoves.precomputedMasks[0][Position.whiteKingPos]);
		blackKingSafetyScore += Long.bitCount(Position.blackOccupied & PrecompMoves.precomputedMasks[0][Position.blackKingPos]);
		
		whiteKingSafetyScore -= KeyToPseudoMoves.pseudoMap[4].apply(
			(byte)(Position.whiteKingPos / 8), (byte)(Position.whiteKingPos % 8), (byte)0, false)
		.length;
		blackKingSafetyScore -= KeyToPseudoMoves.pseudoMap[4].apply(
				(byte)(Position.blackKingPos / 8), (byte)(Position.blackKingPos % 8), (byte)1, false)
		.length;
		
		short kingSafetyDiff = (short)((whiteKingSafetyScore - blackKingSafetyScore) * 2);
	
		int[] whiteLocations = MagicBitboards.getSetBits(Position.whiteOccupied);
		int[] blackLocations = MagicBitboards.getSetBits(Position.blackOccupied);
		boolean inEndGame = whiteLocations.length + blackLocations.length <= 16;
		
		for (int square : whiteLocations) {
			byte pieceType = Position.engineLookup[square];
			whiteMaterialValue += valueMap[pieceType];
			whitePSTBonus += pst[pieceType + (inEndGame ? 1 : 0)][square];
			
			if (pieceType != 1 && square <= 55) { // 
				if ((MagicBitboards.pawnAttackMasks[0][square + 8] & (Position.bitboards[7])) != 0) {
					whitePSTBonus -= 3;
				}
			}
		}
		
		for (int square : blackLocations) {
			byte pieceType = Position.engineLookup[square];
			blackMaterialValue += valueMap[pieceType];
			blackPSTBonus += pst[pieceType - 6 + (inEndGame ? 1 : 0)][square];
			
			if (pieceType != 7 && square >= 8) { // 
				if ((MagicBitboards.pawnAttackMasks[1][square - 8] & (Position.bitboards[1])) != 0) {
					blackPSTBonus -= 3;
				}
			}
		}
		
		if (Position.sideToMove == 0) whitePSTBonus += 3;
		if (Position.sideToMove == 1) blackPSTBonus += 3;
		
		if (Position.attacks[0][Position.blackKingPos] != null) whitePSTBonus += 3;
		if (Position.attacks[1][Position.whiteKingPos] != null) blackPSTBonus += 3;
		
		if (inEndGame) { // Endgame
			int manhattan = (
				Math.abs((Position.whiteKingPos / 8) - (Position.blackKingPos / 8))
				+
				Math.abs((Position.whiteKingPos % 8) - (Position.blackKingPos % 8))
			) * 6;
			
			if ((whiteMaterialValue - blackMaterialValue) <= -100) { // Black Winning
				whitePSTBonus += manhattan;
				blackPSTBonus -= manhattan;
				
				int numKingMoves = KeyToLegalMoves.pseudoMap[5].apply((byte)(Position.whiteKingPos / 8), (byte)(Position.whiteKingPos % 8), (byte)0, false).length;
				whitePSTBonus += numKingMoves;
				blackPSTBonus -= numKingMoves;
			} else if ((whiteMaterialValue - blackMaterialValue) >= 100) { // White Winning
				whitePSTBonus -= manhattan;
				blackPSTBonus += manhattan;
				
				int numKingMoves = KeyToLegalMoves.pseudoMap[5].apply((byte)(Position.blackKingPos / 8), (byte)(Position.blackKingPos % 8), (byte)1, false).length;
				whitePSTBonus -= numKingMoves;
				blackPSTBonus += numKingMoves;
			}
		}
		
		return 
				(whiteMaterialValue + whitePSTBonus + (whiteMobilityBonus / 5))
				- (blackMaterialValue + blackPSTBonus + (blackMobilityBonus / 5))
				+ kingSafetyDiff;
	}
}