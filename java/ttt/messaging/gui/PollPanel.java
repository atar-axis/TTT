package ttt.messaging.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

import ttt.Constants;

/**
 * super class of {@link FullPollPanel} and {@link QuickPollPanel}, which are used to
 * display the poll entry in the poll list.
 * {@code PollPanel} provides common attributes and methods to draw part of the polls.
 * @author Thomas Doehring
 */
public abstract class PollPanel extends JPanel {

    final static Font FONT_TEXT = ((Font)UIManager.get("Label.font")).deriveFont(12f);
	final static int BAR_HEIGHT = 10;

	static BufferedImage IMG_UNLOCK;
	static BufferedImage IMG_LOCK;
	static BufferedImage IMG_LEFT;
	static {
		try {
			IMG_UNLOCK = ImageIO.read(Constants.getResourceUrl("msg_unlock.png"));
			IMG_LOCK = ImageIO.read(Constants.getResourceUrl("msg_lock.png"));
			IMG_LEFT = ImageIO.read(Constants.getResourceUrl("msg_left.png"));
		} catch (Exception e) { /* ignore */ }
	}
	
	int[] votes;
	int[] votesPromilles;
	boolean closed;
	boolean selected;
	
	int listWidth = 200;
		
	/**
	 * draw the result bar of an answer of the poll
	 * @param g  Graphics2D reference
	 * @param y  offset in vertical direction
	 * @param voteIndex  number of answer
	 */
	protected void drawResultBar(Graphics2D g, int y, int voteIndex) {
		int width = getWidth();
		
		int length = votesPromilles[voteIndex] * (width - 50) / 1000 + 2;
		g.setColor(Color.blue);
		g.fillRect(10, y, length, BAR_HEIGHT);
		g.setColor(Color.black);
		g.drawString(String.valueOf(votes[voteIndex]), width - 30, y+9);
	}
	
	/**
	 * draw closed or open marking (red or green border).
	 * @param g  Graphics2D reference
	 */
	protected void drawStatusMarking(Graphics2D g) {
		Stroke tmp = g.getStroke();
		if(closed) g.setColor(new Color(255,0,0,100));
		else g.setColor(new Color(0,255,0,100));
		g.setStroke(new BasicStroke(6));
		g.drawRect(0, 0, getWidth(), getHeight());
		g.setStroke(tmp);
	}
	
	/**
	 * draw a text with automatic wrapping.
	 * @param g  Graphics2D reference
	 * @param txt  the text
	 * @param col  color of the text
	 * @param top  offset in vertical direction
	 * @return  height of text in pixel
	 */
    protected int drawTextLines(Graphics2D g, String txt, Color col, int top) {
		AttributedString attrString = new AttributedString(txt);
		attrString.addAttribute(TextAttribute.FONT, FONT_TEXT);
		attrString.addAttribute(TextAttribute.FOREGROUND, col);

		AttributedCharacterIterator charIt = attrString.getIterator();
		FontRenderContext fontRenderContext = g.getFontRenderContext();
		LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);

		Insets insets = getInsets();
		int textWidth = getWidth() - insets.left - insets.right;
		int y = top;
		
		while(measurer.getPosition() < charIt.getEndIndex()) {
			TextLayout textLayout = measurer.nextLayout(textWidth);
			y += textLayout.getAscent();
			// draw
			textLayout.draw(g, insets.left, y);
			y += textLayout.getDescent() + textLayout.getLeading();
			
			// if(!selected && ++lineCount >= maxLineCount) break;
		}
		
		return y;
    }

    /**
     * calculate the height of a text with automatic word wrapping.
     * @param g  Graphics2D
     * @param text  the text
     * @return  height of text in pixel
     */
    protected int calculateTextHeight(Graphics2D g, String text) {

		AttributedString attrString = new AttributedString(text);
		attrString.addAttribute(TextAttribute.FONT, FONT_TEXT);
		attrString.addAttribute(TextAttribute.FOREGROUND, Color.black);

		// margins
		Insets insets = getInsets();
		int width = listWidth - insets.right - insets.left;
		int y = 0;

		AttributedCharacterIterator charIt = attrString.getIterator();
		FontRenderContext fontRenderContext = g.getFontRenderContext();
		LineBreakMeasurer measurer = new LineBreakMeasurer(charIt, fontRenderContext);
		while(measurer.getPosition() < charIt.getEndIndex()) {
			TextLayout textLayout = measurer.nextLayout(width);
			y += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
		}
		return y;
	}
    
    /**
     * draw the action images for the two action areas (open/close poll, show poll in TTT).
     * @param g  Graphics2D
     */
    protected void drawActionImages(Graphics2D g) {
    	int height = getHeight();
    	int width = getWidth();
    	
    	// separator
    	g.setColor(new Color(0,0,0,50));
    	g.fillRect(width/2 - 2, 0, 4, height);
    	
    	// buttons
    	g.drawImage(IMG_LEFT, width/4 - 12, height - 28, null);
    	if (closed) {
    		g.drawImage(IMG_UNLOCK, width*3/4 - 12, height - 28, null);
    	} else {
    		g.drawImage(IMG_LOCK, width*3/4 - 12, height - 28, null);
    	}
    }
	
    /**
     * caclulate the height of the poll entry and update the panel size.
     * @param g  Graphics2D
     */
	public abstract void calculateHeight(Graphics2D g);
}
