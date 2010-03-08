package ttt.editor.tttEditor;
import java.io.DataOutputStream;
import java.io.IOException;



/**
 * <code>Message</code> used by the TTT to control whether or not the
 * whiteboard is turned on or off; when the whiteboard is turned on,
 * the normal desktop screen display is replaced by a white space
 * upon which annotations can be drawn.
 */
public class BlankPageMessage extends Message {
    
    private int pageNumber;
    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param pageNumber the page number of this <code>BlankPageMessage</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */ 
    public BlankPageMessage(int timestamp, int encoding, int pageNumber, Header header) {
        super(timestamp, encoding, header);
        this.pageNumber = pageNumber;
        this.area = header.framebufferWidth * header.framebufferHeight;
    }
    

    /**
     * Overrides the writeMessage method of the <code>Message</code> class,
     * as <code>BlankPageMessage</code> contains no data array - only an
     * integer containing a page number.
     * @param out the stream to which the <code>Message</code> should be written
     * @param includeTimestamp <code>true</code> if the timestamp should be written along with
     * the rest of the message data, <code>false</code> if it should
     * be omitted.
     * @throws java.io.IOException 
     */
    public void writeMessage(DataOutputStream out, boolean includeTimestamp) throws IOException {        
        if (includeTimestamp) {
            //size
            out.writeInt(6);
            out.writeByte(encoding | ProtocolConstants.EncodingTimestamp);
            out.writeInt(timestamp);
        }
        else {
            //size
            out.writeInt(2);
            out.writeByte(encoding);
        }
        out.writeByte(pageNumber);
    }
    
    
    /**
     * Gets the page number of the blank page.
     * For playback purposes it is generally only necessary to know whether the
     * whiteboard has been turned on or off - a page number of -1 indicates that
     * the whiteboard has been turned off, a positive integer that it has been
     * turned on.
     * @return the page number of the blank page.
     */
    public int getPageNumber() {
        return pageNumber;
    }
    
}
