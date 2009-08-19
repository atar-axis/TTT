package ttt.messaging.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import ttt.messages.Annotation;

/**
 * used by {@link JPollList} (to be exact: by the custom list renderer) to display
 * a quick poll entry in the list.
 * @author Thomas Doehring
 */
public final class QuickPollPanel extends PollPanel {

	public final static long serialVersionUID = 1L;
	
	int color;
	
	@Override
	public void calculateHeight(Graphics2D g) {
		int height = (BAR_HEIGHT + 5) * votes.length + 15;
		setPreferredSize(new Dimension(0, height));
	}
	
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		int width = getWidth();
		int height = getHeight();
		
		// draw background in quick poll color
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		g.setColor(Annotation.annotationColors[color + 3]);
		g.fillRect(0, 0, width, height);
		
		// draw heading
		g.setColor(Color.black);
		g.drawString("Quick Poll", 10, 10);
		
		// draw bars
		int y = 15;
		for(int i = 0; i < votes.length; i++) {
			drawResultBar(g, y, i);
			y += BAR_HEIGHT + 5;
		}
		
		if(selected) {
			drawActionImages(g);
		}

		drawStatusMarking(g);

		// draw bottom border line
		g.setColor(Color.black);
		g.drawLine(0, y-1, width, y-1);
	}
}
