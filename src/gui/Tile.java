package gui;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class Tile extends JPanel {
	Color borderColor;
	Color tileColor;
	
	Tile(Color tileColor, Color borderColor, int size) {
		this.borderColor = borderColor;
		this.tileColor = tileColor;
		
		setPreferredSize(new Dimension(size, size));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.setColor(tileColor);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(borderColor);
		g.drawRect(0, 0, getWidth(), getHeight());
	}
}
