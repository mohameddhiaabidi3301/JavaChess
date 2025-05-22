package main;
import engine.Position;
import engine.MoveGen;
import engine.PrecompMoves;
import engine.MagicBitboards;

import java.util.Arrays;

import debug.DebugRender;

public class Main {
	public static void main(String[] args) {
		System.out.println("Hello World");
		
		Position.initOccupancy();
		PrecompMoves.init();
		System.out.println(Position.allOccupied);
		
		//DebugRender.renderLong(Position.whiteOccupied);
		//DebugRender.renderMoveArray(MoveGen.pseudoPawns((byte)4, (byte)4, "black"));
		
		//DebugRender.renderLong(MagicBitboards.genBishopBlockerMask(4, 4));
		//System.out.println(MagicBitboards.getSetBits(0xFFL << 8).toString());
		
		MagicSearchThread t1 = new MagicSearchThread(0, 15);
		t1.start();
		MagicSearchThread t2 = new MagicSearchThread(16, 31);
		t2.start();
		MagicSearchThread t3 = new MagicSearchThread(32, 47);
		t3.start();
		MagicSearchThread t4 = new MagicSearchThread(48, 63);
		t4.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
			t4.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

