package ttt.editor.tttEditor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Stroke;

import ttt.messages.Annotation;

public class TextShape extends ColoredShape{


	private Color borderC;
	final static Color COL_BACKGROUND = new Color(240, 240, 255, 160);
	final static Font FONT = new Font("SansSerif", Font.BOLD, 16);
	private String text;
	private int TextColor;
	int x;
	int y;
	int x2;
	int y2;

	/**
	 * Constructor.
	 * <p>
	 * Generates a rectangular, colored shape with a border,using the given points as upper left and lower right corner
	 * (which is which depends on the actual coordinates). 
	 * 
	 * @param x1 X coordinate of first corner.
	 * @param y1 Y coordinate of first corner.
	 * @param x2 X coordinate of second corner.
	 * @param y2 Y coordinate of second corner.
	 * @param coreColor Color of the rectangle.
	 * @param borderColor Color of the border.
	 * @param borderWidth Width of the border.
	 */
	public TextShape(int x1, int y1, int x2, int y2, Color coreColor, Color borderColor, int borderWidth, String Text, int TextColor) {
		super(null, coreColor);
		this.text = Text;
		this.x = x1;
		this.y = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.TextColor = TextColor;
		borderC = borderColor;
	}
		
	@Override
	public void paintShape(Graphics g, Stroke pen) {

		FontMetrics fm = g.getFontMetrics(FONT);

		if (text.length() == 0) {
			g.setColor(Color.red);
			g.drawRect(x, y, 10, fm.getHeight());
			
		} else {
			 {
				String[] lines = text.split("\n",10);
				int y = this.y;

				g.setFont(FONT);
				
				g.setColor(COL_BACKGROUND);
				//g.fillRect(this.x, this.y, x2, y2);
				fillRect(g, this.x, this.y, x2, y2);
				
				g.setColor(Annotation.annotationColors[TextColor]);

				for (String line : lines) {
					if (line.length() > 0) {
						g.drawString(line, x, y + fm.getAscent());
					}
					y += fm.getHeight();
				}
			}
		}
	}

	private void fillRect(Graphics g,int startx, int starty, int endx, int endy) {
		g.fillRect(startx, starty, endx - startx, starty - endy );		
	}

	/**
	 * @return The border's color.
	 */
	public Color getBorderColor() {
		return borderC;
	}

	/**
	 * @param color The new border color.
	 */
	public void setBorderColor(Color color) {
		borderC = color;
	}
}
