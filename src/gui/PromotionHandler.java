package gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
public class PromotionHandler extends JPanel {
	PromotionHandler(int tileSize, String color) {
		// Promotion Managing
		super.setLayout(null);
		
		JPanel queenPrm = new Piece(color + "Queens", tileSize);
		JPanel rookPrm = new Piece(color + "Rooks", tileSize);
		JPanel bishopPrm = new Piece(color + "Bishops", tileSize);
		JPanel knightPrm = new Piece(color + "Knights", tileSize);
		
		queenPrm.setBounds(0, 0, tileSize, tileSize);
		rookPrm.setBounds(0, tileSize, tileSize, tileSize);
		bishopPrm.setBounds(0, tileSize * 2, tileSize, tileSize);
		knightPrm.setBounds(0, tileSize * 3, tileSize, tileSize);
		
		super.add(queenPrm);
		super.add(rookPrm);
		super.add(bishopPrm);
		super.add(knightPrm);
		queenPrm.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Board.promotionRecieved(color + "Queens");
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});
		
		rookPrm.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Board.promotionRecieved(color + "Rooks");
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});
		
		bishopPrm.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Board.promotionRecieved(color + "Bishops");
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});
		
		knightPrm.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Board.promotionRecieved(color + "Knights");
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});
	}
}