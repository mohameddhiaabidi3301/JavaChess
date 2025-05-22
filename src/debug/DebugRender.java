package debug;
import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.HashSet;

public class DebugRender {
	public static void renderLong(long bits) {
		JFrame mainFrame = new JFrame("Debug");
		mainFrame.setSize(new Dimension(640, 640));
		
		JPanel board = new JPanel();
		mainFrame.add(board);
		board.setSize(new Dimension(640, 640));
		board.setLayout(new GridLayout(8, 8));
		
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int square = row * 8 + col;
				String msg = Integer.toString(square);
				
				long bitStatus = (long)(bits & (1L << square));
				Color tileColor = (bitStatus != 0) ? Color.white : Color.black;
				Color borderColor = (bitStatus != 0) ? Color.black : Color.white;
				
				JPanel newTile = new Tile(tileColor, borderColor, msg);
				board.add(newTile);
			}
		}
		
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
	}
	
	public static void renderMove(int move) {
		byte from = (byte)(move & 0x3F);
		byte to = (byte)((move >>> 6) & 0x3F);
		byte originKey = (byte)((move >>> 16) & 0xF);
		byte captureKey = (byte)((move >>> 20) & 0xF);
		
		JFrame mainFrame = new JFrame("Debug");
		mainFrame.setSize(new Dimension(640, 640));
		
		JPanel board = new JPanel();
		mainFrame.add(board);
		board.setSize(new Dimension(640, 640));
		board.setLayout(new GridLayout(8, 8));
		
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int square = row * 8 + col;
				
				Color tileColor = Color.black;
				Color borderColor = Color.white;
				String msg = Integer.toString(square);
				
				if (square == from) {
					tileColor = Color.blue;
					borderColor = Color.black;
					msg = "From: (" + square + ")";
				} else if (square == to) {
					tileColor = Color.green;
					borderColor = Color.black;
					msg = "To: (" + square + ")";
				}
				
				JPanel newTile = new Tile(tileColor, borderColor, msg);
				board.add(newTile);
			}
		}
		
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		
		System.out.println(from + ", " + to + ", " + originKey + ", " + captureKey);
	}
	
	public static void renderMoveArray(int[] moves) {
		Set<Integer> targets = new HashSet<Integer>();
		int from = -1;
		
		for (int move : moves) {
			int origin = (int)(move & 0x3F);
			int to = (int)((move >>> 6) & 0x3F);
			targets.add(to);
			if (from == -1) from = origin;
		}
		
		JFrame mainFrame = new JFrame("Debug");
		mainFrame.setSize(new Dimension(640, 640));
		
		JPanel board = new JPanel();
		mainFrame.add(board);
		board.setSize(new Dimension(640, 640));
		board.setLayout(new GridLayout(8, 8));
		
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int square = row * 8 + col;
				
				Color tileColor = Color.black;
				Color borderColor = Color.white;
				String msg = Integer.toString(square);
				
				if (square == from) {
					tileColor = Color.blue;
					borderColor = Color.black;
					msg = "From: (" + square + ")";
				} else if (targets.contains(square)) {
					tileColor = Color.green;
					borderColor = Color.black;
					msg = "To: (" + square + ")";
				}
				
				JPanel newTile = new Tile(tileColor, borderColor, msg);
				board.add(newTile);
			}
		}
		
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
	}
}