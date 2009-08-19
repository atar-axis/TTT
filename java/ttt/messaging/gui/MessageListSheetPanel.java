package ttt.messaging.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ttt.messaging.TTTSheetMessage;

/**
 * used to paint a sheet message entry in the message list.
 * @author Thomas Doehring
 */
public final class MessageListSheetPanel extends MessageListPanel {
	
	public final static long serialVersionUID = 1L;

	protected final static int MAX_NON_SELECTED_HEIGHT = 90;
	protected final static int DEFERRED_BORDER_SIZE = 7;
	
	protected TTTSheetMessage message; 
	
	public MessageListSheetPanel() {
		setOpaque(true);
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		int width = getWidth();
		int height = getHeight();
		
		int y = 0;
		BufferedImage sheetThumb = message.getThumbnail(listWidth);
		
		if (!selected) {
			// draw thumbnail of sheet msg
			g.drawImage(sheetThumb, 0, 0, null);
			// draw name and two lines of text (if avail.)
			if (message.hasText()) {
				drawTextLines(g, message.getText(), name, Color.black, 2);
			} else if (name != null) {
				drawTextLines(g, " ", name, Color.black, 2);
			}
			drawMoreContentIndicator(g);

		} else {
			if (message.hasText()){
				y = drawTextLines(g, message.getText(), name, Color.black, 100);
			} else if (name != null) {
				y = drawTextLines(g, " ", name, Color.black, 100);
			}
			g.drawImage(sheetThumb, getInsets().left, y, null);
			
			drawActionImages(g);
		}
		
		if(deferred) {
			// draw transparent 'deferred' border
			g.setColor(COL_DEFER_TRANSP);
			g.fillRect(0, 0, width, DEFERRED_BORDER_SIZE);    // TOP
			g.fillRect(width-DEFERRED_BORDER_SIZE, DEFERRED_BORDER_SIZE, DEFERRED_BORDER_SIZE, height-2*DEFERRED_BORDER_SIZE);  //  RIGHT
			g.fillRect(0, height-DEFERRED_BORDER_SIZE, width, DEFERRED_BORDER_SIZE);    // BOTTOM
			g.fillRect(0, DEFERRED_BORDER_SIZE, DEFERRED_BORDER_SIZE, height-2*DEFERRED_BORDER_SIZE);   // LEFT
		}
		
		// message separator
		drawMessageSeparator(g);
		// bottom border line
		g.setColor(Color.black);
		g.drawLine(0, height-1, width, height - 1);
	}
	
	@Override
	public void calculateHeight(Graphics2D g) {
		int height = 0;
		if(selected) {
			// if selected height = all text + thumbnail
			if (message.hasText()) {
				if(message.getUserName().length() > 0) {
					height = calculateTextHeight(g, message.getUserName() + ": " + message.getText(), 1000);
				} else {
					height = calculateTextHeight(g, message.getText(), 1000);
				}
			} else if (message.getUserName().length() > 0) {
				height = calculateTextHeight(g, message.getUserName(), 1000);				
			}
			height += message.getThumbnail(listWidth).getHeight();
		} else {
			// if not selected height = MAX(text,thumbnail) limited by MAX_NON_SELECTED_HEIGHT
			if (message.hasText()) {
				height = calculateTextHeight(g, message.getText(), MAX_NON_SELECTED_HEIGHT);
			}
			height = Math.max(height, message.getThumbnail(listWidth).getHeight());
			height = Math.min(height, MAX_NON_SELECTED_HEIGHT);
			// height += 8;
		}
		height += 8;  // for separator
		setPreferredSize(new Dimension(0,height));
	}
}
