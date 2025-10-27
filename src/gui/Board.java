package gui;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


import debug.DebugRender;
import engine.Position;
import main.Main;
import engine.EvaluateBoard;
import engine.KeyToLegalMoves;
import engine.Minimax;

import javax.swing.JLayeredPane;


public class Board {
	static public JFrame board = new JFrame("Board");
	static private JLayeredPane layerPane = new JLayeredPane();
	static private JPanel tilePanel = new JPanel();
	static private JPanel tileFXPanel = new JPanel();
	static private JPanel piecePanel = new JPanel();
	static private JPanel previewPanel = new JPanel();
	
	static final int tileSize = 60;
	static final int halfTileSize = tileSize / 2;
	static private final Color lightColor = Color.white;
	static private final Color darkColor = new Color(179, 142, 78);
	
	static record ClickData(int row, int col, int square, String pieceName, byte pieceId, String pieceColor, byte colorKey, JPanel component, int[] legalMoves, int pieceUIxOrigin, int pieceUIyOrigin) {};
	
	private static int waitingForPromotion = -1;
	private static JPanel promotionComponent;
	
	private static ClickData activeDrag = null;
	private static ClickData getDataAtPos(int clickX, int clickY) {
		int row = 7 - (clickY / tileSize);
		int col = (clickX / tileSize);
		int square = row * 8 + col;
		
		if (row < 0 || row > 7) return null;
		if (col < 0 || col > 7) return null;
		
		String color = (Main.globalPosition.engineLookup[square] <= 6) ? "white" : "black";
		byte colorKey = (byte)((color == "white") ? 0 : 1);
		
		int[] moves = new int[0];
		if (Main.globalPosition.engineLookup[square] != 0) {
			moves = Main.globalPosition.getLegalMovesForPiece((byte)square, colorKey);
		}
		
		JPanel component = (JPanel)piecePanel.getComponentAt(clickX, clickY);
		
		if (component == null) {
			System.err.println("No Component Found at (" + clickX + ", " + clickY + ")");
			return null;
		}
		
		//System.out.println("Clicked at (" + clickX + ", " + clickY + ") [" + row + ", " + col + ", " + square + "], " + Position.lookupBoard.get(square));
		return new ClickData(row, col, square, Main.globalPosition.guilookupBoard[square], Main.globalPosition.engineLookup[square], color, colorKey, component, moves, component.getX(), component.getY());
	}
	
	private static void renderPreviews(int[] moveArray) {
		for (int move : moveArray) {
			int to = ((move >>> 6) & 0x3F);
			int row = 7 - (to / 8);
			int col = (to % 8);
			int square = (row * 8 + col);
			
			JPanel newPreview = new PreviewTile(30);
			previewPanel.add(newPreview);
			newPreview.setBounds(col * tileSize, row * tileSize, tileSize, tileSize);
		}
	}
	
	private static void clearPreviews() {
		previewPanel.removeAll();
		previewPanel.revalidate();
		previewPanel.repaint();
	}
	
	public static void renderAllPieces() {
		piecePanel.removeAll();
		tileFXPanel.removeAll();
		
		int lastPlayedMove = Main.globalPosition.lastGuiMove;
		
		if (lastPlayedMove != -1) {
			int lastFrom = ((lastPlayedMove & 0x3F));
			int lastTo = ((lastPlayedMove >>> 6) & 0x3F);
			Color fromColor = new Color(255, 253, 139, 130);
			Color toColor = new Color(235, 235, 71, 130);
			
			JPanel fromTile = new Tile(fromColor, fromColor, tileSize);
			tileFXPanel.add(fromTile);
			fromTile.setBounds((lastFrom % 8) * tileSize, (7 - (lastFrom / 8)) * tileSize, tileSize, tileSize);
			
			JPanel toTile = new Tile(toColor, toColor, tileSize);
			tileFXPanel.add(toTile);
			toTile.setBounds((lastTo % 8) * tileSize, (7 - (lastTo / 8)) * tileSize, tileSize, tileSize);
		}

		for (int square = 0; square < 64; square++) {
			String piece = Main.globalPosition.guilookupBoard[square];
			if (piece == null || piece.isEmpty()) continue;
			
			int row = square / 8;
			int col = square % 8;

			JPanel newPiece = new Piece(piece, tileSize);			
			piecePanel.add(newPiece);
			newPiece.setBounds(col * tileSize, (7 - row) * tileSize, tileSize, tileSize);
		}
		
		tileFXPanel.revalidate();
		tileFXPanel.repaint();
		
		piecePanel.revalidate();
		piecePanel.repaint();
	}
	
	public static void promotionRecieved(String selection) {
		byte selectionKey = Position.nameKeyConversion.get(selection);
		
		waitingForPromotion |= (selectionKey << 22);
		Main.globalPosition.makeMove(waitingForPromotion, false);
		
		int computerMove = Minimax.getComputerMove(2, 500);
		
		System.out.println(computerMove);			
		Main.globalPosition.makeMove(computerMove, false);
		
		renderAllPieces();
		
		layerPane.remove(promotionComponent);
		
		waitingForPromotion = -1;
	}
	
	public static void init() {
		Dimension boardSize = new Dimension(tileSize * 8, tileSize * 8);
		layerPane.setPreferredSize(boardSize);
		
		tilePanel.setBounds(0, 0, tileSize * 8, tileSize * 8);
		tilePanel.setLayout(new GridLayout(8, 8));
		
		tileFXPanel.setBounds(0, 0, tileSize * 8, tileSize * 8);
		tileFXPanel.setLayout(null);
		
		piecePanel.setBounds(0, 0, tileSize * 8, tileSize * 8);
		piecePanel.setLayout(null);
		
		previewPanel.setBounds(0, 0, tileSize * 8, tileSize * 8);
		previewPanel.setLayout(null);
		
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int square = row * 8 + (7 - col);
				
				Color tileColor = ((row + col) % 2 == 0) ? lightColor : darkColor;
				Color borderColor = tileColor;
				
				JPanel newTile = new Tile(tileColor, borderColor, tileSize);
				tilePanel.add(newTile);
				
				Object pieceType = Main.globalPosition.guilookupBoard[63 - square];
				JPanel newPiece = new Piece((String)pieceType, tileSize);

				piecePanel.add(newPiece);
				newPiece.setBounds(col * tileSize, row * tileSize, tileSize, tileSize);
			}
		}
		
		layerPane.add(tilePanel, Integer.valueOf(0));
		layerPane.add(tileFXPanel, Integer.valueOf(1));
		layerPane.add(piecePanel, Integer.valueOf(2));
		layerPane.add(previewPanel, Integer.valueOf(3));
		board.add(layerPane);
		
		board.pack();
		board.setVisible(true);
		
		piecePanel.setOpaque(false);
		previewPanel.setOpaque(false);
		tileFXPanel.setOpaque(false);
		tilePanel.setOpaque(false);
		
		board.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		layerPane.addMouseListener(new MouseListener() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				Point converted = SwingUtilities.convertPoint(layerPane, e.getPoint(), piecePanel);
				int clickX = converted.x;
				int clickY = converted.y;
				
				ClickData data = getDataAtPos(clickX, clickY);
				if (waitingForPromotion != -1) return;
				if (data == null) return;
				if (data.colorKey != Main.globalPosition.sideToMove) return;
				if (data.pieceName == null) return;
				
				if (data != null && !data.pieceName.isEmpty()) {
					activeDrag = data;
					renderPreviews(data.legalMoves);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				Point converted = SwingUtilities.convertPoint(layerPane, e.getPoint(), piecePanel);
				int releaseX = converted.x;
				int releaseY = converted.y;
				ClickData releaseData = getDataAtPos(releaseX, releaseY);
				
				boolean foundMatch = false;
				if (releaseData != null && activeDrag != null) {
					int row = releaseData.row;
					int col = releaseData.col;
					int square = (row * 8 + col);
					
					for (int move : activeDrag.legalMoves) {
						byte to = (byte)((move >>> 6) & 0x3F);
						byte pieceId = activeDrag.pieceId;
						int promotionRow = (activeDrag.pieceColor == "white") ? 7 : 0;
						boolean isPawn = pieceId == 1 || pieceId == 7;
						boolean isPromotion = (to / 8 == promotionRow && isPawn);
						boolean isCapture = ((move >>> 16) & 0xF) != 0;
						
						if (square == to) {
							if (!isPromotion) {
								JPanel activeComponent = (JPanel)activeDrag.component;
								activeComponent.setBounds(col * tileSize, (7 - row) * tileSize, tileSize, tileSize);
								
								foundMatch = true;
								Main.globalPosition.makeMove(move, false);		
													
								
								int computerMove = Minimax.getComputerMove(2, 500);
								 
								System.out.println(computerMove);
								Main.globalPosition.makeMove(computerMove, false);
								
								renderAllPieces();
								
								break;
							} else {
								foundMatch = true;
								waitingForPromotion = move;
								
								int yOffset = (activeDrag.pieceColor == "white") ? 0 : -180;
								
								JPanel newPromotionUI = new PromotionHandler(tileSize, activeDrag.pieceColor);
								newPromotionUI.setBounds((to % 8) * tileSize, (7 - (to / 8)) * tileSize + yOffset, tileSize, tileSize * 4);
								layerPane.add(newPromotionUI, JLayeredPane.POPUP_LAYER);
								promotionComponent = newPromotionUI;
								
								JPanel activeComponent = (JPanel)activeDrag.component;
								activeComponent.setBounds(col * tileSize, (7 - row) * tileSize, tileSize, tileSize);
							}
						}
					}
				}
				
				if (!foundMatch && activeDrag != null) {
					int pieceUIxOrigin = activeDrag.pieceUIxOrigin;
					int pieceUIyOrigin = activeDrag.pieceUIyOrigin;
		
					JPanel activeComponent = (JPanel)activeDrag.component;
					activeComponent.setBounds(pieceUIxOrigin, pieceUIyOrigin, tileSize, tileSize);
				}


				activeDrag = null;
				clearPreviews();
			}


			@Override
			public void mouseEntered(MouseEvent e) {}


			@Override
			public void mouseExited(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {}
		});
		
		layerPane.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point converted = SwingUtilities.convertPoint(layerPane, e.getPoint(), piecePanel);
				int mouseX = converted.x;
				int mouseY = converted.y;
				
				if (activeDrag != null) {
					JPanel updateComponent = (JPanel)activeDrag.component;
					updateComponent.setBounds(mouseX - halfTileSize, mouseY - halfTileSize, updateComponent.getWidth(), updateComponent.getHeight());
				}
			}


			@Override
			public void mouseMoved(MouseEvent e) {}
		});
	}
}



