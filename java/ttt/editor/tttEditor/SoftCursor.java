package ttt.editor.tttEditor;

import java.awt.Image;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Rectangle;



/**
 * A cursor object containing shape and position, which is used by the TTT.
 */
public class SoftCursor {

    private Image cursorImage = null;
    private Point cursorLocation = new Point(0,0);
    private int hotX, hotY, width, height;
    
    
    /**
     * Class constructor
     * @param cursorImage the cursor image
     * @param width the width of the cursor bounds
     * @param height the height of the cursor bounds
     * @param hotX the horizontal distance of the point where the cursor is located
     * to the top left corner of the cursor bounds (in pixels)
     * @param hotY the vertical distance of the point where the cursor is located
     * to the top left corner of the cursor bounds (in pixels)
     */
    public SoftCursor(Image cursorImage, int width, int height, int hotX, int hotY) {
        this.cursorImage = cursorImage;
        this.width = width;
        this.height = height;
        this.hotX = hotX;
        this.hotY = hotY;
    }
    
    
    /**
     * Get the location of the cursor currently stored.
     * @return the current location
     */
    public Point getLocation() {
        return cursorLocation;
    }
    
    /**
     * Set a new location for the cursor.
     * @param newLocation the new location
     */
    public void setLocation(Point newLocation) {
        cursorLocation = newLocation;
    }
    
    /**
     * Get the smallest <code>Rectangle</code> which completely encloses the cursor.
     * @return the cursor bounds
     */
    public Rectangle getCursorBounds() {
        return new Rectangle(cursorLocation.x - hotX, cursorLocation.y - hotY, width, height);
    }
    
    
    /**
     * Paints the cursor
     * @param g the <code>Graphics</code> context upon which to paint the cursor
     */
    public void paint(Graphics g) {
        if (cursorImage != null)
            g.drawImage(cursorImage, cursorLocation.x - hotX, cursorLocation.y - hotY, null);
    }
    
}