package ttt.editor.tttEditor;
/**Abstract class for screen update messages.
 */
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Abstract class for a <code>Message</code> which repaints all or part of
 * the screen.
 */
public abstract class FramebufferMessage extends Message {

    /**
     * x-coordinate of the top-left corner of the rectangle of the screen
     * covered by this frame buffer message
     */
    protected int x;

    /**
     * y-coordinate of the top-left corner of the rectangle of the screen
     * covered by this frame buffer message
     */
    protected int y;

    /**
     * width of the rectangle covered by this frame buffer message
     */
    protected int width;

    /**
     * height of the rectangle covered by this frame buffer message
     */
    protected int height;
    
    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param area sum of the amount of the screen covered by this <code<FramebufferMessage</code>,
     * and any other similar messages previously initialized 
     * with the same timestamp - used for generating an index.
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public FramebufferMessage(int timestamp, int encoding, int area, byte [] data, Header header) {
        super(timestamp, encoding, data, header);
        this.area = area;
    }
        
    /**
     * Method for painting the screen data contained within this message - is
     * used by sub-classes when they are processed.
     * @param g <code>Graphics</code> context upon which to paint the contents
     * of this <code>FramebufferMessage</code>
     */
    public abstract void paint(Graphics g);
    
    
    /**
     * Gets the area of the screen which can be painted using this <code>Message</code>
     * @return a <code>Rectangle</code> which covers the same area as the
     * <code>Message</code>
     */
    public Rectangle getAffectedArea() {
        return new Rectangle(x, y, width, height);
    }    
    
    
}
