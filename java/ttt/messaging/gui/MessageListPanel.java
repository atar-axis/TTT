package ttt.messaging.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * provides drawing and other helper methods for the subclasses 
 * {@link MessageListTextPanel} and {@link MessageListSheetPanel}
 * @author Thomas Doehring
 */
public abstract class MessageListPanel extends JPanel {

	protected int listWidth = 200;
	protected int oneThird = 66;
	protected int twoThird = 133;
	
	protected boolean deferred = false;
	protected boolean selected = false;
	
	protected String name;
	
	protected final static int SEPARATOR_HEIGHT = 8;

	// Action images
    protected static BufferedImage IMG_DEFER = null;
    protected static BufferedImage IMG_TRASH = null;
    protected static BufferedImage IMG_LEFT = null;
    static {
		try {
			IMG_DEFER = ImageIO.read(MessageListPanel.class.getResource("../../../resources/msg_defer.png"));
			IMG_TRASH = ImageIO.read(MessageListPanel.class.getResource("../../../resources/msg_messagedelete.png"));
			IMG_LEFT = ImageIO.read(MessageListPanel.class.getResource("../../../resources/msg_left.png"));
		} catch (IOException ioe) { /* ignore */ }
    }

    // text font
    protected static Font FONT_TEXT = ((Font)UIManager.get("Label.font")).deriveFont(16f);
    // protected static Font FONT_NAME = ((Font)UIManager.get("Label.font")).deriveFont(16f).deriveFont(Font.ITALIC);
    protected static Font FONT_NAME = FONT_TEXT.deriveFont(Font.ITALIC);
    
    protected static Color COL_DEFER = new Color(200,200,250);
    protected static Color COL_DEFER_TRANSP = new Color(100,100,255,200);
    protected static Color COL_BG = Color.white;
    
    /**
     * draws text lines. Automatically wraps lines so that text fits into list.
     * @param g  Graphics2D reference
     * @param txt  text to draw
     * @param col  text color
     * @param maxLineCount  draw at maximum that number of lines
     * @return number of drawn text lines
     */
    protected int drawTextLines(Graphics2D g, String txt, String name, Color col, int maxLineCount) {
		AttributedString attrString = null;
		
		if (name != null) {
			int txtStart = name.length() + 2;
			attrString = new AttributedString(name + ": " + txt);
			attrString.addAttribute(TextAttribute.FONT, FONT_NAME, 0, txtStart);
			attrString.addAttribute(TextAttribute.FONT, FONT_TEXT, txtStart, txt.length() + txtStart);
			
		} else {
			attrString = new AttributedString(txt);
			attrString.addAttribute(TextAttribute.FONT, FONT_TEXT);
		}
		attrString.addAttribute(TextAttribute.FOREGROUND, col);

		AttributedCharacterIterator charIt = attrString.getIterator();
		FontRenderContext fontRenderContext = g.getFontRenderContext();
		LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);

		Insets insets = getInsets();
		int textWidth = getWidth() - insets.left - insets.right;
		int lineCount = 0;
		int y = insets.top;
		
		while(measurer.getPosition() < charIt.getEndIndex()) {
			TextLayout textLayout = measurer.nextLayout(textWidth);
			y += textLayout.getAscent();
			// draw
			textLayout.draw(g, insets.left, y);
			y += textLayout.getDescent() + textLayout.getLeading();
			
			if(!selected && ++lineCount >= maxLineCount) break;
		}
		
		return y;
    }
    
    /**
     * draws the transparent action buttons
     * @param g Graphics2D to paint on
     */
    protected void drawActionImages(Graphics2D g) {
    	int height = getHeight() - SEPARATOR_HEIGHT;
    	int width = getWidth();
    	
		g.setColor(new Color(0,0,0,50));
		g.fillRect(oneThird - 2, 0, 4, height);
		g.fillRect(twoThird - 2, 0, 4, height);

		g.drawImage(IMG_LEFT, oneThird /2 - 12, height - 26, null);
		g.drawImage(IMG_TRASH, width / 2 - 12, height - 26, null);
		g.drawImage(IMG_DEFER, (width + twoThird) / 2 - 12, height - 26, null);
    }
    
    /**
     * Draws the indicator, that the message has more content.
     * ATM two black triangles and white transparent rects
     * @param g Graphics2D to paint on
     */
    protected void drawMoreContentIndicator(Graphics2D g) {
    	int h = getHeight() - SEPARATOR_HEIGHT;
    	g.setColor(new Color(255,255,255,100));
    	g.fillRect(oneThird - 10, h - 10, 20, 10);
    	g.fillRect(twoThird - 10, h - 10, 20, 10);
		g.setColor(Color.black);
		g.fillPolygon(new int[] { oneThird - 5, oneThird + 5, oneThird, oneThird - 5 },
				new int[] { h - 8, h - 8, h - 3, h - 8 }, 4);
		g.fillPolygon(new int[] { twoThird - 5, twoThird + 5, twoThird, twoThird - 5 },
				new int[] { h - 8, h - 8, h - 3, h - 8 }, 4);
    }
    
    /**
     * Draws the separator 
     * @param g Graphics reference
     */
    protected void drawMessageSeparator(Graphics2D g) {
    	int h = getHeight() - SEPARATOR_HEIGHT;
    	g.setColor(new Color(255,255,200));
    	g.fillRect(0, h, getWidth(), SEPARATOR_HEIGHT);
    	g.setColor(Color.black);
    	// g.drawRect(0, h, getWidth(), SEPARATOR_HEIGHT-1);
    	g.drawLine(0, h, getWidth(), h);
    	g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
    }
    
    public void setDeferred(boolean def) { this.deferred = def; }
    public void setSelected(boolean sel) { this.selected = sel; }
    
    public void setListWidth(int width) {
    	this.listWidth = width;
    	this.oneThird = width / 3;
    	this.twoThird = width * 2 / 3;
    }
    
    public abstract void calculateHeight(Graphics2D g);
    
    protected int calculateTextHeight(Graphics2D g, String text, int MAX_HEIGHT) {

		AttributedString attrString = new AttributedString(text);
		attrString.addAttribute(TextAttribute.FONT, FONT_TEXT);
		attrString.addAttribute(TextAttribute.FOREGROUND, Color.black);

		// margins
		Insets insets = getInsets();
		int width = listWidth - insets.right - insets.left;
		int y = insets.top;

		AttributedCharacterIterator charIt = attrString.getIterator();
		FontRenderContext fontRenderContext = g.getFontRenderContext();
		LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);
		int lasty = 0;
		while(measurer.getPosition() < charIt.getEndIndex() && y < MAX_HEIGHT) {
			TextLayout textLayout = measurer.nextLayout(width);
			lasty = y;
			y += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
		}
		if (y > MAX_HEIGHT) return lasty;
		return y;
	}
}
