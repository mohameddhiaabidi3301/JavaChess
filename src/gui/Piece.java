package gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import engine.Position;

public class Piece extends JPanel {
	private static final String[] paths = {
		"", 
		"src/gui/Images/whitePawn.png", 
		"src/gui/Images/whiteKnight.png",
		"src/gui/Images/whiteBishop.png",
		"src/gui/Images/whiteRook.png",
		"src/gui/Images/whiteQueen.png",
		"src/gui/Images/whiteKing.png",
		
		"src/gui/Images/blackPawn.png",
		"src/gui/Images/blackKnight.png",
		"src/gui/Images/blackBishop.png",
		"src/gui/Images/blackRook.png",
		"src/gui/Images/blackQueen.png",
		"src/gui/Images/blackKing.png"
	};
	String pieceType;
	
	private static BufferedImage getImage(String path) {
		BufferedImage img = null;
		
		try {
			img = ImageIO.read(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return img;
	}
	
	Piece(String pieceType, int tileSize) {
		this.pieceType = pieceType;
		
		setPreferredSize(new Dimension(tileSize, tileSize));
		setOpaque(false);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (Position.nameKeyConversion.containsKey(this.pieceType)) {
			byte pathIndex = Position.nameKeyConversion.get(this.pieceType);
			
			String path = paths[pathIndex];
			BufferedImage img = getImage(path);
			
			//g.fillRect(0, 0, 10, 10);
			g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
		}
	}
}