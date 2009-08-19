package ttt.messaging.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * used by {@link JPollList} (to be exact: by the custom list renderer) to display
 * a full poll entry in the list.
 * @author Thomas Doehring
 *
 */
public final class FullPollPanel extends PollPanel {
	
	public final static long serialVersionUID = 1L;

	String question;
	String[] answers;
	
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		int width = getWidth();
		int height = getHeight();
		
		// white background
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		
		// draw question
		int y = drawTextLines(g, question, Color.black, 0);
		y += 10;
		
		// draw answers and their bars
		for (int i = 0; i < answers.length; i++) {
			y = drawTextLines(g, answers[i], Color.black, y);
			drawResultBar(g, y, i);
			y += BAR_HEIGHT + 10;
		}

		if(selected) drawActionImages(g);
		
		drawStatusMarking(g);
		
		g.setColor(Color.black);
		g.drawLine(0, height-1, width, height-1);
	}
	
	@Override
	public void calculateHeight(Graphics2D g) {
		// calculate heights of text lines
		int height = calculateTextHeight(g, question);
		height += 10;
		for(int i = 0; i < answers.length; i++) {
			height += calculateTextHeight(g, answers[i]);
			height += BAR_HEIGHT + 10;
		}
		setPreferredSize(new Dimension(0,height-5));
	}

}
