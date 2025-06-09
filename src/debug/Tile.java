package debug;
import javax.swing.*;
import java.awt.*;

public class Tile extends JPanel {
	Color tileColor;
	Color borderColor;
	String display;
	
	Tile(Color tileColor, Color borderColor, String display) {
		this.tileColor = tileColor;
		this.borderColor = borderColor;
		this.display = display;
		setPreferredSize(new Dimension(60, 60));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(this.tileColor);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(this.borderColor);
		g.drawRect(0, 0, getWidth(), getHeight());
		g.drawString(display, 10, 27);
	}
}