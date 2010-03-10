package ttt.editor.tttEditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import javax.swing.ProgressMonitorInputStream;




/**
 * Reads data from TTT file.
 */
public class TTTFileReader {
    
    
    
    /**
     * Read data completely from a TTT (desktop) file.
     * Also allows old .vnc files which have been recorded
     * with the TTT to be read and converted to the new TTT format.
     * @return <code>TTTFileData</code> containing all the data of the file,
     * or <code>null</code> if fill could not be found.
     * @param desktopFile the TTT file to be read.
     * @throws java.io.IOException
     */
    public static TTTFileData readFile(File desktopFile) throws IOException {
        String fileName = desktopFile.getAbsolutePath();
        fileName.toLowerCase();
        if (fileName.endsWith(".ttt"))
            return readTTTFile(desktopFile);
        else if (fileName.endsWith(".vnc"))
            return readVNCFile(desktopFile);
        else
            throw new IOException("Cannot read selected file: illegal file suffix");
    }
    
    
    private static TTTFileData readVNCFile(File desktopFile) throws IOException {
        System.out.println("Transcoding to TTT format - please wait...");
        InputStream in_raw;
        try {
            in_raw = new FileInputStream(desktopFile);
        } catch(FileNotFoundException e) {
            System.err.println("Desktop file not found: " + e.toString());
            return null;
        }
        ProgressMonitorInputStream progress_in = new ProgressMonitorInputStream(
                TTTEditor.getInstance(), "Reading file data...", in_raw);
        progress_in.getProgressMonitor().setMillisToPopup(0);
        progress_in.getProgressMonitor().setMillisToDecideToPopup(0);
        DataInputStream in = new DataInputStream(progress_in);
        
        TTTFileData fileData = new TTTFileData();
        LinkedList<Message> messages = new LinkedList<Message>();
        
        //read data from file
        try {
            
            //process file header
            fileData.header = new Header();
            fileData.header.startTime = in.readLong();
            fileData.header.readVersionMessageFromInputStream(in);
            
            in.skipBytes(4);
            
            //read compressed data
            fileData.header.readServerInitFromInputStream(in);
            
            readVNCMessages(in, fileData.header, messages);
            
            //if no index has been read from the file, generate an index from the messages
            if (fileData.index == null)
                fileData.index = new Index(messages, fileData.header);
            
            //add messages from the TTT file to the index
            fileData.index.addMessagesToIndexes(messages);
            
            //if the file is not searchable, and a corresponding text file exists,
            //pass text file to index for reading
            if (!fileData.index.isSearchable()) {
                System.out.print("Reading search base: ");
                //remove suffix
                String fileName = desktopFile.getAbsolutePath();
                File file = new File(fileName.substring(0, fileName.length() - 4) + ".txt");
                if (file.exists()) {
                    System.out.println(file.getName());
                    fileData.index.readSearchBaseFromFile(file);
                } else
                    System.out.println("No search text file found.");
            }
            
            return fileData;
        } catch(IOException e) {
            progress_in.close();
            throw e;
        }
    }
    
    
    private static TTTFileData readTTTFile(File desktopFile) throws IOException {
        InputStream in_raw;
        try {
            in_raw = new FileInputStream(desktopFile);
        } catch(FileNotFoundException e) {
            System.err.println("Desktop file note found: " + e.toString());
            return null;
        }
        ProgressMonitorInputStream progress_in = new ProgressMonitorInputStream(
                TTTEditor.getInstance(), "Reading file data...", in_raw);
        progress_in.getProgressMonitor().setMillisToPopup(0);
        progress_in.getProgressMonitor().setMillisToDecideToPopup(0);
        DataInputStream in = new DataInputStream(progress_in);
        
        TTTFileData fileData = new TTTFileData();
        LinkedList<Message> messages = new LinkedList<Message>();
        
        //read data from file
        
        try {
            //process file header
            fileData.header = new Header();
            fileData.header.readVersionMessageFromInputStream(in);
            
            //read compressed data
            in = new DataInputStream(new InflaterInputStream(progress_in));
            fileData.header.readServerInitFromInputStream(in);
            readExtensions(in, fileData);
            
            fileData.header.startTime = in.readLong();
            readTTTMessages(in, fileData.header, messages);
         
            //if no index has been read from the file, generate an index from the messages
            if (fileData.index == null)
                fileData.index = new Index(messages, fileData.header);
            
            //add messages from the TTT file to the index
            fileData.index.addMessagesToIndexes(messages);
            
            //if the file is not searchable, and a corresponding text file exists,
            //pass text file to index for reading
            if (!fileData.index.isSearchable()) {
                System.out.print("Reading search base: ");
                //remove suffix
                String fileName = desktopFile.getAbsolutePath();
                File file = new File(fileName.substring(0, fileName.length() - 4) + ".txt");
                if (file.exists()) {
                    System.out.println(file.getName());
                    fileData.index.readSearchBaseFromFile(file);
                } else
                    System.out.println("No search text file found.");
            }
            return fileData;
        } catch(IOException e) {
            progress_in.close();
            throw e;
        }
    }
    
  
/*
 * Methods to read data from the file
 **********************************************************************/
    
    //read any extensions, and add to the TTTFileData object
    private static void readExtensions(DataInputStream in, TTTFileData fileData) throws IOException {
        int length;
        fileData.extensions = new ArrayList<byte[]>();
        while ((length = in.readInt()) > 0) {
            byte[] extension = new byte[length];
            in.readFully(extension);
            System.out.println("Extension: Tag[" + extension[0] + "] " + length + " bytes");
            fileData.extensions.add(extension);
        }
        System.out.println(fileData.extensions.size() + " extensions found.");
        
        //parse extensions
        for (int i = 0; i < fileData.extensions.size(); i++) {
            byte[] extension = fileData.extensions.get(i);
            DataInputStream ext_in = new DataInputStream(new ByteArrayInputStream(extension));
            int tag = ext_in.readByte();
            switch (tag) {
                case ProtocolConstants.EXTENSION_INDEX_TABLE:
                    System.out.println("Reading Index Table");
                    try {
                        //create an index from the extension
                        fileData.index = new Index(ext_in, fileData.header);
                    } catch(Exception e) {System.out.println("Problem reading index extension");}
                    break;
                default:
                    System.out.println("UNKNOWN EXTENSION ([" + tag + "] " + extension.length + " bytes)");
                    break;
            }
        }
    }
    
    
    
    //read all messages from the file, add them to passed <code>List</code>
//NOTE - CONVERTS TO TTT FORMAT
//IGNORES CURSORS!!!
    private static void readVNCMessages(DataInputStream in, Header header, List<Message> messages) throws IOException {
        
        Runtime rt = Runtime.getRuntime();
        System.out.println("Total memory = " + rt.totalMemory() + ", free memory = " + rt.freeMemory());
        System.out.print("loading ");
        
        int count = 0;
        try {
            Message message = null;
            while (true) {
                int timestamp = in.readInt();
                int size = in.readInt();
                byte type = in.readByte();
                size--;
                switch(type) {
                    case ProtocolConstants.SetColourMapEntries:
                        in.skipBytes(3);
                        in.skipBytes(in.readUnsignedShort() * 6);
                        continue;
                    case ProtocolConstants.Bell:
                        continue;
                    case ProtocolConstants.ServerCutText:
                        in.skipBytes(3);
                        in.skipBytes(in.readInt());
                        continue;
                }
                if (type != ProtocolConstants.FramebufferUpdate) {
                    System.err.println("Unknown RFB message type (" + type + ") - attempt to skip");
                    continue;
                }
                //else is a framebuffer update
                in.skipBytes(1);
                size--;
                int updateNRects = in.readUnsignedShort();
                size -= 2;
                
                for (int i = 0; i < updateNRects; i++) {
                    
                    
                    size -= 4;
                    byte[] msgArea = new byte[8];
                    in.readFully(msgArea);
                    int encoding = in.readInt();
                    byte[] msg;
                    
                    //int representing the accumulated area of screen updated
                    //at one given time
                    int area = 0;
//System.out.println("Timestamp:\t" + timestamp + "\t\tEncoding:\t" + encoding);
                    
                    switch(encoding) {
                        case (ProtocolConstants.EncodingLastRect) :
                            break;
                        case ProtocolConstants.EncodingHextile:
                            int offset = 4;
                            int width = (((msgArea[offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            int height = (((msgArea[++offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            area = width * height;
                            
                            // cumulate areas in last message with same timestamp
                            if (message != null && message.getTimestamp() == timestamp) {
                                area += message.area;
                                message.area = 0;
                            }
                            
                            msg = getHextileMessageBytes(in, header, msgArea);
                            
                            message = new HextileMessage(timestamp, encoding, area, msg, header);
                            messages.add(message);
                            
                            size -= msg.length;
                            break;
                        case ProtocolConstants.EncodingRaw:
                            offset = 4;
                            width = (((msgArea[offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            height = (((msgArea[++offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            area = width * height;
                            
                            //length of raw message (in bytes) is area * bytes per pixel
                            msg = new byte[area * header.bitsPerPixel / 8];
                            System.arraycopy(msgArea, 0, msg, 0, msgArea.length);
                            in.readFully(msg, msgArea.length, msg.length - msgArea.length);
                            
                            // cumulate areas in last message with same timestamp
                            if (message != null && message.getTimestamp() == timestamp) {
                                area += message.area;
                                message.area = 0;
                            }
                            message = new RawMessage(timestamp, encoding, area, msg, header);
                            messages.add(message);
                            
                            size -= msg.length;
                            break;
                        case ProtocolConstants.EncodingCopyRect:
                            offset = 4;
                            width = (((msgArea[offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            height = (((msgArea[++offset] & 127) | (msgArea[offset] & 128)) << 8)
                            | ((msgArea[++offset] & 127) | (msgArea[offset] & 128));
                            area = width * height;
                            //length of a CopyRect method is 12 bytes
                            msg = new byte[12];
                            System.arraycopy(msgArea, 0, msg, 0, msgArea.length);
                            in.readFully(msg, msgArea.length, msg.length - msgArea.length);
                            // cumulate areas in last message with same timestamp
                            if (message != null && message.getTimestamp() == timestamp) {
                                area += message.area;
                                message.area = 0;
                            }
                            message = new CopyRectMessage(timestamp, encoding, area, msg, header);
                            messages.add(message);
                            
                            size -= msg.length;
                            break;
                        default:
//SHOULD DEAL PROPERLY WITH CURSORS (but doesn't)
                            System.out.println("Unknown message found: messages will be skipped");
                            msg = new byte[size];
                            System.arraycopy(msgArea, 0, msg, 0, msgArea.length);
                            in.readFully(msg, msgArea.length, msg.length - msgArea.length);
                            //cannot process other sub-messages, as unclear how long the
                            //unknown message should be - must discard
                            i = updateNRects;
                            
                            message = new UnknownMessage(timestamp, encoding, msg, header);
                            messages.add(message);
                            break;
                    }
                    count++;
                }
            }
        } catch (EOFException e) {
            System.out.println("Total messages: " + count);
        }
        in.close();
    }
    
    
    //read all messages from the file, add them to passed <code>List</code>
    private static void readTTTMessages(DataInputStream in, Header header, List<Message> messages) throws IOException {
        
        Runtime rt = Runtime.getRuntime();
        System.out.println("Total memory = " + rt.totalMemory() + ", free memory = " + rt.freeMemory());
        System.out.print("loading ");
        
        int count = 0;
        
        try {
            // new recording (ending .ttt)
            // read all messages
            int timestamp = 0;
            Message message = null;
            boolean initialCursorFound = false;
            
            boolean firstMessage = true;
            
            while (true) {            
                // read header
                int size = in.readInt();
                int encoding = in.readByte();
                size--;
           
                
                if ((encoding & ProtocolConstants.EncodingTimestamp) != 0) {
                    // timestamp bit set -> message contains timestamp
                    timestamp = in.readInt();
                    encoding &= 127;
                    //	remove timestamp bit
                    size -= 4; // size - timestamp
                } // else keep previous timestamp
                
       
                
                // fix inconsistent timstamps
                if (message != null && message.getTimestamp() > timestamp)
                    timestamp = message.getTimestamp();
                if (timestamp < 0)
                    timestamp = 0;
                
                // always start at 0
                if (firstMessage) {
                    timestamp = 0;
                    firstMessage = false;
                }
                
                //int representing the accumulated area of screen updated
                //at one given time
                int area = 0;
             
                switch(encoding) {
                    case ProtocolConstants.EncodingHextile:                    	
                        byte [] msg = new byte[size];
                        in.readFully(msg);
                        int offset = 4;
                        int width = (((msg[offset] & 127) | (msg[offset] & 128)) << 8)
                        | ((msg[++offset] & 127) | (msg[offset] & 128));
                        int height = (((msg[++offset] & 127) | (msg[offset] & 128)) << 8)
                        | ((msg[++offset] & 127) | (msg[offset] & 128));
                        area = width * height;
                        // cumulate areas in last message with same timestamp
                        if (message != null && message.getTimestamp() == timestamp) {
                            area += message.area;
                            message.area = 0;
                        }
                        message = new HextileMessage(timestamp, encoding, area, msg, header);
                        messages.add(message);
                        break;
                    case ProtocolConstants.EncodingRaw:                    	 
                        msg = new byte[size];                
                        in.readFully(msg);                    	
                        message = new RawMessage(timestamp, encoding, area, msg, header);                    
                        messages.add(message);                       
                        break;
                    case ProtocolConstants.EncodingCopyRect:
                        msg = new byte[size];
                        in.readFully(msg);
                        message = new CopyRectMessage(timestamp, encoding, area, msg, header);
                        messages.add(message);
                        break;
                    case ProtocolConstants.EncodingTTTCursorPosition: 
                    int x = in.readUnsignedShort();
                    int y = in.readUnsignedShort();
                    message = new CursorMoveMessage(timestamp, encoding, x, y, header);
                    messages.add(message);
                    break;
                    case ProtocolConstants.EncodingTTTXCursor: 
                    case ProtocolConstants.EncodingTTTRichCursor:
                        msg = new byte[size];
                        in.readFully(msg);
                        message = new CursorShapeMessage(timestamp, encoding, msg, header);
                        //Process the first cursor shape message found
                        //so that cursor has an initial shape
                        if (!initialCursorFound) {
                            initialCursorFound = true;
                            if (messages.size() > 0) {
                                byte [] msgCopy = msg.clone();
//insert a clone of the first cursor shape message at the beginning, so that the cursor has an initial shape
//Copying the whole message and inserting it here aids when seeking randomly: without this, then a later cursor encoding
//would be used if the user moves back to the beginning from a late point in the file
//NOTE: the position of the cursor will still be different when seeking, unless a messsage is added to place it in the top left at the beginning
//however this is less important, as when the cursor is relevant it will be moved anyway (but the shape not necessarily changed).
//In that respect, initializing the shape matters more than initializing the position.
                                CursorShapeMessage messageCopy = new CursorShapeMessage(0, encoding, msgCopy, header);
                                messages.add(0, messageCopy);
                                System.out.println("Cursor shape message added at beginning.");
                            }
                        }
                        //add message as normal
                        //not strictly necessary as shape already set to same, but perhaps in the future
                        //shape would be changed at an earlier point
                        messages.add(message);
                        break;
                    case ProtocolConstants.LINE: 
                    case ProtocolConstants.RECT: 
                    case ProtocolConstants.HIGHLIGHT: 
                    case ProtocolConstants.REMOVE_ALL: 
                    case ProtocolConstants.REMOVE:                    
                    case ProtocolConstants.FREE:                    	
                        msg = new byte[size];                     	
                        in.readFully(msg);
                        message = new AnnotationMessage(timestamp, encoding, msg, header);
                        messages.add(message);
                        break;
                    case ProtocolConstants.TEXT: 
                    	
                    	 
                    	int color = in.readUnsignedByte();
                		int posX = in.readUnsignedShort();
                		int posY = in.readUnsignedShort();
                		int maxWidth = in.readUnsignedShort();
                    	int length = in.readUnsignedShort();
                    	byte[] bText = new byte[length];
                    	
                		int bytesRead = 0;                		
                		while(bytesRead < length) {   
                			bytesRead += in.read(bText, bytesRead, bText.length - bytesRead);
                		}       		
                		byte[] data = new byte[5+length];
                		
                	
                		
                		data[0] = (byte) color; 
                		short h = (short) posX;
                		data[1] = (byte) h;
                		h = (short) posY;
                		data[2] = (byte) h;
                		h = (short) maxWidth;
                		data[3] = (byte) h;
                		h = (short) length;
                		data[4] = (byte) h;
                		
                		for(int i = 5; i < length;i++){
                			data[i] = bText[i];
                		}

                		
                		String decodeString = new String(bText);
                		 message = new AnnotationMessage(timestamp, color, posX, posY, maxWidth, decodeString, encoding, data, header);
                         messages.add(message);
                    	break;
                        
                        
                    case ProtocolConstants.EncodingBlankPage:
                        message = new BlankPageMessage(timestamp, encoding, in.readByte(), header);
                        messages.add(message);
                        break;
                   
                    case ProtocolConstants.EncodingRecording: break;
//consider handling ProtocolConstants.EncodingRecording - whatever it means
                    default:
                        msg = new byte[size];
                        in.readFully(msg);
                        message = new UnknownMessage(timestamp, encoding, msg, header);
                        messages.add(message);
                        break;
                }
                count++;       
            }
        } catch (EOFException e) {        	
            System.out.println("Total messages: " + count);
        }
        in.close();
    }
    
    
    
    
    
    
    
    
    
    
    //mimics processing of a hextile message, but instead writes bytes to an
    //array which may then be returned
    private static byte[] getHextileMessageBytes(DataInputStream in, Header header, byte[] msgArea) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        byteOut.write(msgArea);
        
        int areaOffset = 0;
//        int x = (((msgArea[areaOffset] & 127) | (msgArea[areaOffset] & 128)) << 8)
//        | ((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128));
//        int y = (((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128)) << 8)
//        | ((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128));
        int width = (((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128)) << 8)
        | ((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128));
        int height = (((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128)) << 8)
        | ((msgArea[++areaOffset] & 127) | (msgArea[areaOffset] & 128));
        
        for (int ty = 0; ty < height; ty += 16) {
            
            int th = 16;
            if (height - ty < 16)
                th = height - ty;
            
            for (int tx = 0; tx < width; tx += 16) {
                int tw = 16;
                if (width - tx < 16)
                    tw = width - tx;
                
                int subencoding = in.readUnsignedByte();
                byteOut.write(subencoding);
                
                if ((subencoding & HextileMessage.HextileRaw) != 0) {
//                    int [] dataArray = new int[tw * th];
                    
                    for (int j = 0; j < th; j++) {
//                        int offset = j * tw;
                        for (int count = 0; count < tw; count++) {
                            for (int i = 0; i < header.bitsPerPixel / 8; i++)
                                byteOut.write(in.readUnsignedByte());
                        }
                    }
                    continue;
                }
                if ((subencoding & HextileMessage.HextileBackgroundSpecified) != 0) {
                    for (int i = 0; i < header.bitsPerPixel / 8; i++)
                        byteOut.write(in.readUnsignedByte());
                }
                
                
                if ((subencoding & HextileMessage.HextileForegroundSpecified) != 0) {
                    for (int i = 0; i < header.bitsPerPixel / 8; i++)
                        byteOut.write(in.readUnsignedByte());
                }
                
                if ((subencoding & HextileMessage.HextileAnySubrects) == 0)
                    continue;
                
                int nSubrects = in.readUnsignedByte();
                byteOut.write(nSubrects);
                
//                int b1, b2, sx, sy, sw, sh;
                if ((subencoding & HextileMessage.HextileSubrectsColoured) != 0) {
                    
                    for (int j = 0; j < nSubrects; j++) {
                        for (int i = 0; i < header.bitsPerPixel / 8; i++)
                            byteOut.write(in.readUnsignedByte());
                        
                        byteOut.write(in.readUnsignedByte());
                        byteOut.write(in.readUnsignedByte());
                    }
                } else {
                    for (int j = 0; j < nSubrects; j++) {
                        byteOut.write(in.readUnsignedByte());
                        byteOut.write(in.readUnsignedByte());
                    }
                }
            }
        }
        return byteOut.toByteArray();
    }
}
