package engine;

import java.util.Arrays;
import debug.DebugRender;

public class Minimax {
	private static int[] evaluateMove(int move, boolean extendWithQuiessence, int depth, boolean isMaximizing, int alpha, int beta, int quiessenceCount, int ply, int[] bestMove) {
		Position.makeMove(move, true);
		
		int nextDepth = extendWithQuiessence ? depth : depth - 1;
		int nextQuiessence = extendWithQuiessence ? quiessenceCount + 1 : quiessenceCount;
		
		int[] scoreData = minimax(nextDepth, !isMaximizing, alpha, beta, nextQuiessence, ply);
		Position.unmakeMove(move);
		
		if (!isMaximizing) {
			if (scoreData[1] < bestMove[1]) {
				bestMove = new int[] {
					move,
					scoreData[1],
					nextDepth,
					-1,
					nextQuiessence,
					ply,
					alpha,
					beta,
				};
			}
		} else {
			if (scoreData[1] > bestMove[1]) {
				bestMove = new int[] {
					move,
					scoreData[1],
					nextDepth,
					-1,
					nextQuiessence,
					ply,
					alpha,
					beta,
				};
			}
		}
		
		return bestMove;
	}
	
	private static int staticExchangeEvaluation(byte targetSquare, int captureValue, byte sideToMove) {
		int[] whiteAttackers = (Position.attacks[0][targetSquare]); // Sorts locations from least to most value
		int[] blackAttackers = (Position.attacks[1][targetSquare]); // Sorts locations from least to most value
		if (whiteAttackers == null) whiteAttackers = new int[0]; else whiteAttackers = insertionSortPieceValue(whiteAttackers);
		if (blackAttackers == null) blackAttackers = new int[0]; else blackAttackers = insertionSortPieceValue(blackAttackers);
		
		int[] pieceValues = EvaluateBoard.valueMap;
		
		int[] used = new int[64];
		
		int side = sideToMove;
		int gainIndex = 0;
		int[] gains = new int[32];
		gains[0] = captureValue;
		
		while (true) {
			int[] currentAttackers = (side == 0) ? whiteAttackers : blackAttackers;
			int nextAttacker = -1;
			
			for (int pos : currentAttackers) {
				if (used[pos] == 0) {
					nextAttacker = pos;
					break;
				}
			}
			
			if (nextAttacker == -1) break;
			
			int gain = pieceValues[Position.engineLookup[nextAttacker]];
			used[nextAttacker] = 1;
			gainIndex++;
			gains[gainIndex] = gain - gains[gainIndex - 1];
		
			side = 1 - side;
		}
		
		for (int i = gainIndex - 1; i >= 0; i--) {
			gains[i] = Math.max(-gains[i + 1], gains[i]);
		}
		
		return gains[0];
	}
	
	private static int scoreMoveHueristic(int move, boolean inQuiessence) {
		byte to = (byte)((move >>> 6) & 0x3F);
		byte pieceType = (byte)((move >>> 12) & 0xF);
		byte captureType = (byte)((move >>> 16) & 0xF);
		byte promotionFlag = (byte)((move >>> 27) & 1);
		byte color = (byte)((move >>> 31) & 1);
		
		int ourPieceValue = EvaluateBoard.valueMap[pieceType];
		int capturePieceValue = EvaluateBoard.valueMap[captureType];
		
		byte eval = 0;
		
		if (captureType != 0) {
			if (inQuiessence) {
				int see = staticExchangeEvaluation(to, capturePieceValue, color);
				if (see > 0) eval += see;
				else eval -= 7;
			} else {
				eval += 1;
			}
		}
		
		if (promotionFlag != 0) eval += EvaluateBoard.valueMap[((move >>> 22) & 0xF)] + 5;
		
		if (ourPieceValue < capturePieceValue) eval += 
				(capturePieceValue - ourPieceValue);
		
		if (captureType != 0) {
			int[][] opponentDefends = (color == 0) ? Position.attacks[1] : Position.attacks[0];
			if (ourPieceValue > capturePieceValue && opponentDefends[to] != null) { // Attacking defended piece
				int[] attackers = opponentDefends[to];
				
				for (int location : attackers) {
					if (EvaluateBoard.valueMap[Position.engineLookup[location]] < ourPieceValue) { // The piece we attack is defended by a lesser value opponent peice (hanged)
						eval -= 10;
						break;
					}
				}
			}
		}
		
		return eval;
	}
	
	private static int[] insertionSortOnHeuristic(int[] moveArray, boolean inQuiessence) {
		for (byte i = 1; i < moveArray.length; i++) {
			int key = moveArray[i];
			byte j = (byte)(i - 1);
			
			while (j >= 0 && scoreMoveHueristic(moveArray[j], inQuiessence) < scoreMoveHueristic(key, inQuiessence)) {
				moveArray[j + 1] = moveArray[j];
				j = (byte)(j - 1);
			}
			
			moveArray[j + 1] = key;
		}
		
		return moveArray;
 	}
	
	private static int[] insertionSortPieceValue(int[] pieceLocations) {
		for (byte i = 1; i < pieceLocations.length; i++) {
			int key = pieceLocations[i];
			byte j = (byte)(i - 1);
			
			while (j >= 0 && EvaluateBoard.valueMap[Position.engineLookup[pieceLocations[j]]] > EvaluateBoard.valueMap[Position.engineLookup[key]]) {
				pieceLocations[j + 1] = pieceLocations[j];
				j = (byte)(j - 1);
			}
			
			pieceLocations[j + 1] = key;
		}
		
		return pieceLocations;
 	}
	
	public static int[] getComputerMove(boolean isMaximizing) {
		long startTime = System.nanoTime();
		int[] move = minimax(3, isMaximizing, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
		long endTime = System.nanoTime();
		int millisecondsEllapsed = (int)((endTime - startTime) / 1_000_000);
		
		System.out.println("Took: " + (millisecondsEllapsed) + " milliseconds to compute move");
		
		return move;
	}
	
	private static int[] minimax(int depth, boolean isMaximizing, int alpha, int beta, int quiessenceCount, int ply) {
		int gameScoreState = EvaluateBoard.isGameOver();

		if (depth == 0) {
			return new int[] {-1, EvaluateBoard.getEval(), depth, -1, quiessenceCount, ply, alpha, beta};
		}
		
		if (gameScoreState != -1) {
			byte sign = (byte)(isMaximizing ? -1 : 1);
			return new int[] {-1, gameScoreState + (ply * sign), depth, -1, quiessenceCount, ply, alpha, beta};
		}
		
		int[] moveArray = (!isMaximizing ? Position.getAllLegalMoves((byte)1) : Position.getAllLegalMoves((byte)0));
		int[] bestMove = {-1, !isMaximizing ? Integer.MAX_VALUE : Integer.MIN_VALUE, depth, -1, quiessenceCount, ply, alpha, beta};
		byte ourKingPos = (!isMaximizing ? Position.blackKingPos : Position.whiteKingPos);
		
		boolean usInCheck = (!isMaximizing
			? Position.attacks[0][ourKingPos] != null :
			Position.attacks[1][ourKingPos] != null);
		boolean inQuiessence = (quiessenceCount > 0);
		
		moveArray = insertionSortOnHeuristic(moveArray, inQuiessence);		
		for (int i = 0; i < moveArray.length; i++) {
			int move = moveArray[i];
			boolean isCapture = ((move >>> 16) & 0xF) != 0;
			boolean isPromotion = ((move >>> 27) & 1) != 0;
			
			boolean extendWithQuiessence = (isCapture || isPromotion || usInCheck) && quiessenceCount < 6;
			byte[] potentialPromotionKeys = (isMaximizing ? Position.whitePromotions : Position.blackPromotions);
			
			if (!isPromotion) {
				int[] eval = evaluateMove(move, extendWithQuiessence, depth, isMaximizing, alpha, beta, quiessenceCount, ply + 1, bestMove);
				bestMove = eval;
				
				if (!isMaximizing) {beta = Math.min(beta, eval[1]);}
				else alpha = Math.max(alpha, eval[1]);
				
				if (beta <= alpha) break;
			} else {
				for (byte pk : potentialPromotionKeys) {
					int promotionMove = ((move & ~(0xF << 22)) | (pk << 22));
					
					int[] eval = evaluateMove(promotionMove, extendWithQuiessence, depth, isMaximizing, alpha, beta, quiessenceCount, ply + 1, bestMove);
					bestMove = eval;
					
					if (!isMaximizing) {beta = Math.min(beta, eval[1]);}
					else alpha = Math.max(alpha, eval[1]);
					
					if (beta <= alpha) break;
				}
			}
		}
		
		return bestMove;
	}
}