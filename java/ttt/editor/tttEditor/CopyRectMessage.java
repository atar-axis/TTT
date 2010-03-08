package ttt.editor.tttEditor;

import java.awt.Graphics;


/**
 * <code>FramebufferMessage</code> which copies a rectangle of the screen
 * which has already been painted, and places the copy at a different
 * point on the screen.
 */
public class CopyRectMessage extends FramebufferMessage {
    
    private int sourceX, sourceY;
    
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
    public CopyRectMessage(int timestamp, int encoding, int area, byte [] data, Header header) {
        super(timestamp, encoding, area, data, header);
        bytePointer = 0;
        //no real need for data array at all?
        //Would then require many parameters though, perhaps confusing
        x = getUnsignedShort();
        y = getUnsignedShort();
        width = getUnsignedShort();
        height = getUnsignedShort();
        sourceX = getUnsignedShort();
        sourceY = getUnsignedShort();
    }
            
    
    /**
     * Method for painting the screen data contained within this message.
     * <br />
     * NOTE:- This method may not have been tested.
     * @param g <code>Graphics</code> context upon which to paint the contents
     * of this <code>FramebufferMessage</code>
     */
    public void paint(Graphics g) {
        g.copyArea(sourceX, sourceY, width, height, x - sourceX, y - sourceY);
    }
        
}
