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
		LegalityCheck.init();
		MagicBitboards.initBishopLookups();
		MagicBitboards.initRookLookups();
		MagicBitboards.initPrecomputedLineBB();
		MagicBitboards.initMagicMasks();
		
		Position.initGlobalZobristKeys(); // Zobrist Keys are global across all threads
		Position globalPos = new Position("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq");

		globalPosition = globalPos;
		Board.init();
	}
}