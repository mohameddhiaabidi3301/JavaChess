package main;
import engine.Position;
import engine.LegalityCheck;
import engine.MoveGen;
import engine.PrecompMoves;
import engine.MagicBitboards;
import engine.Minimax;

import java.util.Arrays;
import debug.DebugRender;
import gui.Board;

public class Main {
	public static Position globalPosition;
	
	public static void main(String[] args) {
		// Default FEN: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq
		// Test Endgame 1: 8/3k4/3rb3/pp2p4/3R4/8/PP3B2/3K4 w KQkq
		// Test Engame 2 White Winning: 8/3p1bk1/4p1b1/8/8/3R1Q2/4K3/8 w - - 0 1
		// Test engame 3 8/3k4/8/8/8/8/4Q3/6K1 w - - 0 1
		
		LegalityCheck.init();
		MagicBitboards.initBishopLookups();
		MagicBitboards.initRookLookups();
		MagicBitboards.initPrecomputedLineBB();
		MagicBitboards.initMagicMasks();
		
		Position.initGlobalZobristKeys(); // Zobrist Keys are global across all threads
		Position globalPos = new Position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq");

		globalPosition = globalPos;
		Board.init();
		
		globalPosition.updateAttacksTEST((byte)0);
		globalPosition.updateAttacksTEST((byte)1);
		
//		boolean isMaximizing = true;
//		for (int i = 0; i < 200; i++) {
//			int[] computerMove = Minimax.getComputerMove(1, 500, isMaximizing);
//			 
//			System.out.println(Arrays.toString(computerMove));
//			Main.globalPosition.makeMove(computerMove[0], false);
//			
//			Board.renderAllPieces();
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			
//			isMaximizing = !isMaximizing;
//		}
	}
}


