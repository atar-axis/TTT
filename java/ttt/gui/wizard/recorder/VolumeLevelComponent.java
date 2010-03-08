/* VolumeLevelComponent.java
 * Created on 10. Mai 2007, 11:42
 * @author Christian Gruber Bakk.techn.
 */

package ttt.gui.wizard.recorder;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;


public class VolumeLevelComponent extends JComponent{
    
    // one rectangle shows the color gradient while
    // the second one is responsible to show percentage of the first
    private Rectangle2D gradientRec;
    private Rectangle2D overlappingRec;
    
    private Color startColor = Color.GREEN;
    private Color endColor = Color.RED;
    
    private double peakPercentage = 0;
    
    
    /**
     * Sets the current volume input peak of the volumebar
     * @param percent relative volume level
     */
    public void setPeakPercentage(double percent) {  
        peakPercentage = percent;
        repaint();
    }
    
    /**
     * Paints the volumebar - depends on its component size
     * @param g 
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        gradientRec = new Rectangle2D.Float(0f,0f,this.getWidth(),this.getHeight());
        
        float xCoordOverlappingRec = (float)(this.getWidth()*peakPercentage);
        float yCoordOverlappingRec = 0f;
        float widthOverlappingRec = (float)(this.getWidth()*(1-peakPercentage));
        float heightOverlappingRec = this.getHeight();
        
        overlappingRec = new Rectangle2D.Float(xCoordOverlappingRec,yCoordOverlappingRec,
                widthOverlappingRec,heightOverlappingRec);
        
        Graphics2D graphics = (Graphics2D)g; 
        GradientPaint gp = new GradientPaint(0,0, startColor,
                1.2f * this.getWidth(),0, endColor, false);
        
        graphics.setPaint(gp);
        graphics.fill(gradientRec);
        graphics.setPaint(Color.GRAY);
        graphics.fill(overlappingRec);
    }
}


