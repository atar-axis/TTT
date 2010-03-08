package ttt.editor.tttEditor;


/**
 * Abstract class for <code>Message</code>s which involve the cursor 
 * in some way - either in changing its shape or position.
 */
public abstract class CursorMessage extends Message {
    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public CursorMessage(int timestamp, int encoding, Header header) {
        super(timestamp, encoding, header);
    }


    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public CursorMessage(int timestamp, int encoding, byte [] data, Header header) {
        super(timestamp, encoding, data, header);
    }    
        
}
