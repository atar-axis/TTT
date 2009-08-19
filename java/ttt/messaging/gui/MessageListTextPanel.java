package ttt.messaging.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * used for the rendering of text messages in message list
 * @author Thomas Doehring
 */
public class MessageListTextPanel extends MessageListPanel {

	public final static long serialVersionUID = 1L;
	
	protected final static int MAX_NONSELECTED_HEIGHT = 50;

	protected String text;
	
	public MessageListTextPanel() {
		setOpaque(true);
	}
	
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		int width = getWidth();
		int height = getHeight();

		// background
		if (deferred) g.setColor(COL_DEFER);
		else g.setColor(COL_BG);
		g.fillRect(0, 0, width, height);
		
		// text
		if(selected) {
			drawTextLines(g, text, name, Color.black, 100);
			drawActionImages(g);
		} else {
			drawTextLines(g, text, name, Color.black, 2);
			drawMoreContentIndicator(g);
		}
		
		// message separator
		drawMessageSeparator(g);
	}
	
	@Override
	public void calculateHeight(Graphics2D g) {
		int height = 0;
		if(selected) {
			height = calculateTextHeight(g, text, 1000) + 20 + SEPARATOR_HEIGHT;
		} else {
			height = calculateTextHeight(g, text, MAX_NONSELECTED_HEIGHT) + 8 + SEPARATOR_HEIGHT;
		}
		setPreferredSize(new Dimension(0, height));
	}
}
