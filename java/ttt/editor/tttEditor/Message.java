package ttt.editor.tttEditor;
import java.io.DataOutputStream;
import java.io.IOException;


/**Abstract class for Messages.
 */
public abstract class Message {
        
    /**
     * Byte array containing the main data of this <code>Message</code>.
     */
    protected byte [] data;
    /**
     * Timestamp in ms.
     */
    protected int timestamp;
    /**
     * Message encoding, as defined in <code>ProtocolConstants</code>
     */
    protected int encoding;
    /**
     * If this <code>Message</code> is a <code>FramebufferMessage</code>,
     * the area of the screen covered by this <code>Message</code> and any
     * previous <code>FramebufferMessage</code>s at with the same timestamp.
     * Used when generating an <code>Index</code>, as it can usually be assumed
     * that an index division is appropriate whenever a large area of the screen
     * is updated at one time.
     */
    protected int area = 0;
    /**
     * The <code>Header</code> object used by all <code>Message</code>s in
     * this TTT file, and which contains data which may be needed for the
     * processing of the message.
     */
    protected Header header;
    

    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public Message(int timestamp, int encoding, byte [] data, Header header) {
        this.timestamp = timestamp;
        this.encoding = encoding;
        this.data = data;
        this.header = header;
    }
    
    /**
     * Class constructor for a <code>Message</code> which does not require
     * a byte array.  Other variables may be defined in subclasses.
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public Message(int timestamp, int encoding, Header header) {
        this.timestamp = timestamp;
        this.encoding = encoding;
        this.header = header;
    }

    
    /**
     * Gets the timestamp
     * @return the timestamp in ms, synchronized if synchronization flag in 
     * <code>Parameters</code> is set to <code>true</code>
     */
    public int getTimestamp() {
        if (Parameters.synchronize)
            return (int)(timestamp * header.synchRatio);
        else
            return timestamp;
    }
    
    
    /**
     * Sets the timestamp, adjusts it if synchronization on
     * @param newTimestamp the timestamp in ms, synchronized if synchronization flag in 
     * <code>Parameters</code> is set to <code>true</code>
     */
    public void setTimestamp(int newTimestamp) {
        if (Parameters.synchronize)
            timestamp = (int)(timestamp / header.synchRatio + 0.5);
        else
            timestamp = newTimestamp;
    }
    
    
    /**
     * Gets the timestamp stored in the <code>Message</code>
     * @return the timestamp in ms actually stored, regardless of whether
     * synchronization is set or not
     */
    public int getTimestampWithoutSync() {
        return timestamp;
    }
    
    
    /**
     * The endoding of the <code>Message</code>
     * @return the endoding
     */
    public int getEncoding() {
        return encoding;
    }
    
    
    /**
     * Gets a string containing the message encoding and a formatted
     * representation of the timestamp.
     * @return string representation of <code>Message</code>
     */
    public String toString() {
        return ("Message encoding: " + encoding + ",\tTimestamp:  " + TTTEditor.getStringFromTime(timestamp,true));
    }

    //write message data with / without timestamp
    /**
     * Write the message to a specified output stream, including the timestamp if
     * desired.
     * @param out the stream to which the <code>Message</code> should be written
     * @param includeTimestamp <code>true</code> if the timestamp should be written along with
     * the rest of the message data, <code>false</code> if it should
     * be omitted.
     * @throws java.io.IOException 
     */
    public void writeMessage(DataOutputStream out, boolean includeTimestamp) throws IOException {
        if (includeTimestamp) {
            //size
            out.writeInt(data.length + 5);
            out.writeByte(encoding | ProtocolConstants.EncodingTimestamp);
            out.writeInt(timestamp);
        }
        else {
            //size
            out.writeInt(data.length + 1);
            out.writeByte(encoding);
        }
        
        //special treatment for text Annotations
        if(this.getEncoding() == ProtocolConstants.TEXT){
        	((AnnotationMessage)this).writeTextAnnotaion(out, true);        	
        }else{
        out.write(data);}
    }
    
    //write message data with timestamp
    /**
     * Write the message to a specified output stream, including the timestamp by
     * default.
     * @param out the stream to which the <code>Message</code> should be written
     * @throws java.io.IOException 
     */
    public void writeMessage(DataOutputStream out) throws IOException {
        writeMessage(out, true);
    }
    
    
///////////////////////////////////////////////////////////////////////////    
    //*****Used for obtaining bytes from array in an ordered way*****
///////////////////////////////////////////////////////////////////////////    
    
    /**
     * Byte pointer used by the <code>Message</code> when reading data
     * from the contained byte array.
     */
    protected int bytePointer = 0;
    
    //get data from byte array
    /**
     * interpret the next byte of the byte array as an unsigned 8-bit number.
     * @return the next byte of this input stream, interpreted as an unsigned
     * 8-bit number.
     */
    protected int getUnsignedByte() {
        int ch;
        if (bytePointer < data.length)
            ch = (data[bytePointer] & 0xff);
        else
            ch = -1;
        if (ch < 0)
            throw new IndexOutOfBoundsException("Error reading from byte array: " + toString());
        bytePointer++;
	return ch; 
    }
    
    
    /**
     * get the next byte of the byte array as.
     * @return the next byte of this input stream as a signed 8-bit <code>byte</code>.
     */
    protected int getByte() {
        int ch;
        if (bytePointer < data.length)
            ch = data[bytePointer];
        else
            ch = -1;
        bytePointer++;
	return ch; 
    }
    
    
    /**
     * Interpret the next 2 bytes of the byte array as an integer.
     * @return the next two bytes of this input stream, interpreted as an unsigned
     * 16-bit integer.
     */
    protected int getUnsignedShort() {
        int ch1 = getUnsignedByte();
        int ch2 = getUnsignedByte();
        if ((ch1 | ch2) < 0)
            throw new IndexOutOfBoundsException("Error reading from byte array: " + toString());
        return (ch1 << 8) + (ch2 << 0);
    }
    
    
    /**
     * Reads a specified amount from the byte array into a second array.
     * @param b the buffer into which the data is read.
     * @param off the start offset of the data
     * @param len the number of bytes to read
     */
    protected void readFully(byte b[], int off, int len) {
	if (len < 0)
	    throw new IndexOutOfBoundsException("Error reading from byte array: " + toString());
	int n = 0;
	while (n < len) {
            b[n + off] = data[bytePointer];
            bytePointer++;
	    n++;
	}
    }
    
    
    /**
     * Reads from the byte array into another array, until the second
     * array is full.
     * @param b array into which the data should be read
     */
    protected void readFully(byte b[]) {
	int n = 0;
        while (n < b.length) {
            b[n] = data[bytePointer];
            bytePointer++;
	    n++;
	}
    }
    
    
//should I return an int to say how many bytes skipped?  Like in the InputStream version...?
    /**
     * Increases the value of the byte pointer.
     * @param n number of bytes to be skipped
     */
    protected void skipBytes(int n) {
        bytePointer += n;
    }
    
    
    
    /*
    used when testing....
    protected void finalize() throws Throwable {
        System.out.println("I'm a message, and I'm going now: " + toString());
        super.finalize();
    }
     */
     
    
    
    
}