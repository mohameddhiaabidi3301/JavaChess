package main;
import engine.Position;
import engine.LegalityCheck;
import engine.MoveGen;
import engine.ZobristHash;
import engine.PrecompMoves;
import engine.MagicBitboards;
import engine.Minimax;

import java.util.Arrays;
import debug.DebugRender;
import gui.Board;

public class Main {
	private static void searchMagics() {
		int[] range1 = {1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 13, 17, 18, 19, 21, 25, 37, 43, 44};
		int[] range2 = {4, 5, 6, 1, 2, 3, 8, 9, 11, 12, 13, 17, 18, 19, 21, 25, 37, 43, 44};
		int[] range3 = {11, 12, 13, 17, 18, 19, 21, 25, 37, 43, 44, 4, 5, 6, 1, 2, 3, 8, 9};
		
		MagicSearchThread t1 = new MagicSearchThread(range1);   // 8 positions (0-7)
		t1.start();
		MagicSearchThread t2 = new MagicSearchThread(range2);  // 8 positions (9-16)
		t2.start();
		MagicSearchThread t3 = new MagicSearchThread(range3); // 8 positions (17-24)
		t3.start();
		MagicSearchThread t4 = new MagicSearchThread(range1); // 8 positions (25-32)
		t4.start();
		MagicSearchThread t5 = new MagicSearchThread(range2); // 8 positions (33-40)
		t5.start();
		MagicSearchThread t6 = new MagicSearchThread(range3); // 7 positions (41-47)
		t6.start();
	
		try {
		    t1.join(); t2.join(); t3.join(); t4.join();
		    t5.join(); t6.join();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Hello World");
		
		Position.initOccupancy();
		PrecompMoves.init();
		ZobristHash.initZobrist();
		LegalityCheck.init();
		
		//DebugRender.renderLong(Position.whiteOccupied);
		//DebugRender.renderMoveArray(MoveGen.pseudoPawns((byte)4, (byte)4, "black"));
		//DebugRender.renderLong(Position.bitboards[6]);
		
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(7, 3));
		//System.out.println(MagicBitboards.getSetBits(0xFFL << 8).toString());
		
		MagicBitboards.initBishopLookups();
		MagicBitboards.initRookLookups();
		MagicBitboards.initPrecomputedLineBB();
		
		Position.attacks[0] = Position.getAttacks((byte)0, true);
		Position.attacks[1] = Position.getAttacks((byte)1, true);
		
		Position.pins[0] = LegalityCheck.getPinnedPieces((byte)0);
		Position.pins[1] = LegalityCheck.getPinnedPieces((byte)1);
		
		Position.attacks[0] = Position.getAttacks((byte)0, false);
		Position.attacks[1] = Position.getAttacks((byte)1, false);
		
		
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(5, 6));
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(5, 6) & Position.allOccupied);
		//DebugRender.renderMoveArray(MagicBitboards.simulateRook(5, 6, Position.allOccupied & MagicBitboards.genRookBlockerMask(5, 6)));
		//DebugRender.renderMoveArray(MoveGen.pseudoRooks((byte)5, (byte)6, "black"));
		
		//searchMagics();
		Board.init();
	}
}