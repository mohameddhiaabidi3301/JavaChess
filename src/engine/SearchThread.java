package engine;

import java.util.Arrays;

import main.Main;

public class SearchThread extends Thread {
	int initializerMove;
	int depth;
	boolean isMaximizing;
	int alpha;
	int beta;
	int[] previousBestMove;
	long itdStartTime;
	int maxTime;
	int quiessenceCount;
	int ply;
	Position localPosition;
	
	private int[][] killerMoves = new int[128][2];
	public SearchThread(int initializerMove, int depth, boolean isMaximizing, int alpha, int beta, int[] previousBestMove, long itdStartTime, int maxTime, int quiessenceCount, int ply) {
		this.initializerMove = initializerMove;
		this.depth = depth;
		this.isMaximizing = isMaximizing;
		this.alpha = alpha;
		this.beta = beta;
		this.previousBestMove = previousBestMove;
		this.itdStartTime = itdStartTime;
		this.maxTime = maxTime;
		this.quiessenceCount = quiessenceCount;
		this.ply = ply;
		this.localPosition = Main.globalPosition.clonePosition();
	}
	
	public int[] minimax(int initializerMove, int depth, boolean isMaximizing, int alpha, int beta, int[] previousBestMove, long itdStartTime, int maxTime, int quiessenceCount, int ply) {
		if (ply == 0 && initializerMove != -1) {
			localPosition.makeMove(initializerMove, true);
		}
		
		if (depth == 0) {
			return new int[] {-1, localPosition.getEval(), -1, depth};
		}
		
		int TT_INDEX = (int)(localPosition.zobristHash & (Minimax.TT_SIZE - 1));
		if (Minimax.zobristKeys[TT_INDEX] == localPosition.zobristHash) {
			if ((System.nanoTime() - itdStartTime) / 1_000_000 > maxTime) {
				return new int[] {-1, 0, Minimax.FLAG_TIMEOUT, depth};
			}
			
			int[] data = Arrays.copyOf(Minimax.zobristValues[TT_INDEX], 4);
			int flag = data[2];
			int cacheScore = data[1];
			int cacheDepth = data[3];
			
			if (depth <= cacheDepth) {
				if (flag == Minimax.FLAG_EXACT) {
					return data;
				} else if (flag == Minimax.FLAG_LOWERBOUND && cacheScore >= beta) {
					return data;
				} else if (flag == Minimax.FLAG_UPPERBOUND && cacheScore <= alpha) {
					return data;
				}
			}
		}
		
		boolean inEndGame = (Long.bitCount(localPosition.whiteOccupied) + Long.bitCount(localPosition.blackOccupied)) <= 14;
		boolean usInCheck = (isMaximizing ? localPosition.attacks[1][localPosition.whiteKingPos] != null : localPosition.attacks[0][localPosition.blackKingPos] != null);
	
		if (depth >= 3 && quiessenceCount == 0 && !usInCheck && !inEndGame) {
			if ((System.nanoTime() - itdStartTime) / 1_000_000 > maxTime) {
				return new int[] {-1, 0, Minimax.FLAG_TIMEOUT, depth};
			}
			
			int R = 2; // Depth Reduction
			localPosition.toggleNullMove(); // Symmetric
			
			int[] nullScore = minimax(-1, depth - R - 1, !isMaximizing, -beta, -beta + 1, null, itdStartTime, maxTime, quiessenceCount, ply + 1);
			localPosition.toggleNullMove();
			
			if (nullScore[1] >= beta) {
				return new int[] {-1, beta, Minimax.FLAG_LOWERBOUND, depth}; // Beta cutoff
			}
		}
		
		int[] moves = (isMaximizing) ? localPosition.getAllLegalMoves((byte)0) : localPosition.getAllLegalMoves((byte)1);
		int[] bestMove = new int[] {-1, (isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE), -1, depth};
		Minimax.sortByScore(moves, previousBestMove, depth, ply, killerMoves); // Order the PBM first because its likely good
		
		if (moves.length == 0) {
			if (usInCheck) {
				return new int[] {-1, isMaximizing ? -100_000 + ply : 100_000 - ply, -1, depth};
			} else {
				return new int[] {-1, 0, -1, depth};
			}
		}
		
		int cutoffFlag = Minimax.FLAG_EXACT; // Default
		for (int i = 0; i < moves.length; i++) {
			int move = moves[i];
			byte to = (byte)((move >>> 6) & 0x3F);
			boolean isCapture = ((move >>> 16) & 0xF) != 0;
			boolean isPromotion = ((move >>> 27) & 1) != 0;
			//boolean isHighValuePiece = (((move >>> 12) & 0xF) % 6) >= 4;
			
			boolean extendQuiessence = ((isCapture || isPromotion) && quiessenceCount < 4);
			int nextDepth = (extendQuiessence) ? depth : depth - 1;
			int nextQuiessence = (extendQuiessence) ? quiessenceCount + 1 : quiessenceCount;
			
			if ((System.nanoTime() - itdStartTime) / 1_000_000 > maxTime) {
				return new int[] {-1, 0, Minimax.FLAG_TIMEOUT, depth};
			}
			
			if (!isPromotion) {
				localPosition.makeMove(move, true);
				
				int[] score = minimax(-1, nextDepth, !isMaximizing, alpha, beta, null, itdStartTime, maxTime, nextQuiessence, ply + 1);
				localPosition.unmakeMove(move);
				
				if (isMaximizing) {
					 if (score[1] > bestMove[1]) {
						 bestMove[1] = score[1];
						 bestMove[0] = move;
						 alpha = Math.max(alpha, score[1]);
					 }
				} else {
					if (score[1] < bestMove[1]) {
						bestMove[1] = score[1];
						bestMove[0] = move;
						beta = Math.min(beta, score[1]);
					}
				}
				
				if (beta <= alpha) {
					cutoffFlag = (isMaximizing) ? Minimax.FLAG_LOWERBOUND : Minimax.FLAG_UPPERBOUND;
					
					if (!isCapture && cutoffFlag == Minimax.FLAG_LOWERBOUND) { // Beta cutoff on non-capture
						if (killerMoves[ply][0] != move) {
					        killerMoves[ply][1] = killerMoves[ply][0];
					        killerMoves[ply][0] = move;
					    }
					}
					
					break;
				};
			} else {
				byte[] pkKeys = (isMaximizing ? Position.whitePromotions : Position.blackPromotions);
				
				for (byte key : pkKeys) {
					int promotionMove = (move & ~(0xF << 22)) | (key << 22);
					
					localPosition.makeMove(promotionMove, true);
					
					int[] score = minimax(-1, nextDepth, !isMaximizing, alpha, beta, null, itdStartTime, maxTime, nextQuiessence, ply + 1);
					localPosition.unmakeMove(promotionMove);
					
					if (isMaximizing) {
						 if (score[1] > bestMove[1]) {
							 bestMove[1] = score[1];
							 bestMove[0] = promotionMove;
							 alpha = Math.max(alpha, score[1]);
						 }
					} else {
						if (score[1] < bestMove[1]) {
							bestMove[1] = score[1];
							bestMove[0] = promotionMove;
							beta = Math.min(beta, score[1]);
						}
					}
					
					if (beta <= alpha) {
						cutoffFlag = (isMaximizing) ? Minimax.FLAG_LOWERBOUND : Minimax.FLAG_UPPERBOUND;
						break;
					};
				}
			}
		}
		
		bestMove[2] = cutoffFlag;
		
		if (bestMove[2] != Minimax.FLAG_TIMEOUT) {
			if (Minimax.zobristKeys[TT_INDEX] == localPosition.zobristHash) {
				if (Minimax.zobristValues[TT_INDEX][3] <= depth) {
					Minimax.zobristValues[TT_INDEX] = bestMove;
				}
			} else {
				Minimax.zobristKeys[TT_INDEX] = localPosition.zobristHash;
				Minimax.zobristValues[TT_INDEX] = bestMove;
			}
		}
		
		return bestMove;
	}
	
	private static int[] threadBestMove;
	
	@Override
	public void run() {
		threadBestMove = minimax(initializerMove, depth, isMaximizing, alpha, beta, previousBestMove, itdStartTime, maxTime, quiessenceCount, ply);
		Minimax.threadsCompleted.incrementAndGet();
	}
	
	public int[] getBestMove() {
		return threadBestMove;
	}
}