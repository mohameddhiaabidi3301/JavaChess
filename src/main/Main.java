package main;
import engine.Position;
import engine.MoveGen;
import engine.PrecompMoves;
import engine.MagicBitboards;
import java.util.Arrays;
import debug.DebugRender;
import gui.Board;

public class Main {
	private static void searchMagics() {
		MagicSearchThread t1 = new MagicSearchThread(25, 26);   // 8 positions (0-7)
		t1.start();

		MagicSearchThread t2 = new MagicSearchThread(25, 26);  // 8 positions (9-16)  
		t2.start();

		MagicSearchThread t3 = new MagicSearchThread(25, 26); // 8 positions (17-24)
		t3.start();

		MagicSearchThread t4 = new MagicSearchThread(25, 26); // 8 positions (25-32)
		t4.start();

		MagicSearchThread t5 = new MagicSearchThread(25, 26); // 8 positions (33-40)
		t5.start();

		MagicSearchThread t6 = new MagicSearchThread(25, 26); // 7 positions (41-47)
		t6.start();

		MagicSearchThread t7 = new MagicSearchThread(25, 26); // 5 positions (52-56)
		t7.start();

		MagicSearchThread t8 = new MagicSearchThread(25, 26); // 3 positions (61-63)
		t8.start();

		try {
		    t1.join(); t2.join(); t3.join(); t4.join();
		    t5.join(); t6.join(); t7.join(); t8.join();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Hello World");
		
		Position.initOccupancy();
		PrecompMoves.init();
		
		//DebugRender.renderLong(Position.whiteOccupied);
		//DebugRender.renderMoveArray(MoveGen.pseudoPawns((byte)4, (byte)4, "black"));
		
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(7, 3));
		//System.out.println(MagicBitboards.getSetBits(0xFFL << 8).toString());
		
		MagicBitboards.initBishopLookups();
		MagicBitboards.initRookLookups();
		
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(5, 6));
		//DebugRender.renderLong(MagicBitboards.genRookBlockerMask(5, 6) & Position.allOccupied);
		//DebugRender.renderMoveArray(MagicBitboards.simulateRook(5, 6, Position.allOccupied & MagicBitboards.genRookBlockerMask(5, 6)));
		//DebugRender.renderMoveArray(MoveGen.pseudoRooks((byte)5, (byte)6, "black"));
		
		//searchMagics();
		
		Board.init();
	}
}


