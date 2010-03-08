package ttt.editor.tttEditor;


/**
 * Unknown Message.
 * Unknown messages should be retained and written to any new file, as they may have some
 * meaning in a more recent version of the TTT.
 */

public class UnknownMessage extends Message {
    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param header object containing data specific to the TTTFile
     */
    public UnknownMessage(int timestamp, int encoding, byte[] data, Header header) {
        super(timestamp, encoding, data, header);
    }
    
    /** Processes the message.
     * As the message types is unknown, this simply prints output 
     * to that effect using the standard output stream. */
    public void processMessage() {
        System.out.println("Unknown message found: Will be ignored");
    }
    
}
