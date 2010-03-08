package ttt.editor.tttEditor;


import java.awt.Point;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A <code>Message</code> which stores the coordinates of where the cursor
 * should be at a given timestamp.
 */
public class CursorMoveMessage extends CursorMessage {
    
    private Point cursorPosition;
    
    /**
     * Class constructor
     * @param x the new x-coordinate for the cursor
     * @param y the new y-coordinate for the cursor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public CursorMoveMessage(int timestamp, int encoding, int x, int y, Header header) {
        super(timestamp, encoding, header);
        cursorPosition = new Point(x, y);
    }
    
    
    /**
     * Overrides the writeMessage method of the <code>Message</code> class,
     * as <code>CursorMoveMessage</code> contains no data array - only a
     * <code>Point</code> containing a position.
     * @param out the stream to which the <code>Message</code> should be written
     * @param includeTimestamp <code>true</code> if the timestamp should be written along with
     * the rest of the message data, <code>false</code> if it should
     * be omitted.
     * @throws java.io.IOException 
     */
    public void writeMessage(DataOutputStream out, boolean includeTimestamp) throws IOException {        
        if (includeTimestamp) {
            //size
            out.writeInt(9);
            out.writeByte(encoding | ProtocolConstants.EncodingTimestamp);
            out.writeInt(timestamp);
        }
        else {
            //size
            out.writeInt(5);
            out.writeByte(encoding);
        }
        out.writeShort(cursorPosition.x);
        out.writeShort(cursorPosition.y);
    }
    
    
    /**
     * Get the cursor position contained within this message.
     * @return the cursor position.
     */
    public Point getCursorPosition() {
        return cursorPosition;
    }
    
}
