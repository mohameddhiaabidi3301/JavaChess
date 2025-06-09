package gui;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class PreviewTile extends JPanel {
	int size;
	
	PreviewTile(int size) {
		this.size = size;
		setPreferredSize(new Dimension(size, size));
		setOpaque(false);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.fillArc(size / 2, size / 2, size, size, 0, 360);
	}
}
