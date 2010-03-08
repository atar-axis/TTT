package ttt.editor.tttEditor;


import java.awt.Color;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class containing data which is required by various subclasses of type <code>Message</code>
 * in order that they might be processed.  The data can be entered at
 * initialization from reading the desktop file, or calculated from data
 * which has been read.
 */
public class Header {

    protected String versionMsg;
    
    protected ColorModel colorModel;
    protected Color[] colors;
    protected Color bg = Color.black;
    protected Color fg = Color.black;
        
    protected int framebufferHeight, framebufferWidth;
    protected int bitsPerPixel;
    protected int depth;
    protected boolean bigEndian;
    protected boolean trueColour;
    protected int redMax;
    protected int greenMax;
    protected int blueMax;
    protected int redShift;
    protected int greenShift;
    protected int blueShift;
    /**
     * The stored name of the desktop recording.
     */
    protected String desktopName;
    
    /**
     * The start time of the recording - which may be used to obtain a date and time.
     */
    protected long startTime;
    
    /**
     * The synchronization ratio for the current playback - that is, the number
     * by which a recorded timestamp must be multiplied in order to get the timestamp
     * being used for playback.  This means that desktop may be effectively
     * "shortened" to coincide with the length of other media - usually audio.
     */
    protected double synchRatio = 1.0;
    
    private Area fullArea;
    
    
    
    public void writeVersionMessageToOutputStream(DataOutputStream out) throws IOException {
        if (versionMsg.startsWith("RFB"))
            versionMsg = Parameters.defaultVersionMessage;
        out.writeBytes(versionMsg);
    }
    
    public void writeServerInitToOutputStream(DataOutputStream out) throws IOException {
        out.writeShort(framebufferWidth);
        out.writeShort(framebufferHeight);
        out.writeByte(bitsPerPixel);
        out.writeByte(depth);
        
        out.writeByte(bigEndian ? 1 : 0);
        out.writeByte(trueColour ? 1 : 0);
        
        out.writeShort(redMax);
        out.writeShort(greenMax);
        out.writeShort(blueMax);
        out.writeByte(redShift);
        out.writeByte(greenShift);
        out.writeByte(blueShift);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeInt(desktopName.length());
        out.write(desktopName.getBytes());
    }
    
    
    public void readVersionMessageFromInputStream(DataInputStream in) throws IOException {
        byte[] b = new byte[12];
        in.readFully(b);
        versionMsg = new String(b);
        System.out.print("Server Version: " + versionMsg);
        if (b[11] != '\n')
            System.out.println();
    }
    
    
    public void readServerInitFromInputStream(DataInputStream in) throws IOException {
        framebufferWidth = in.readUnsignedShort();
	framebufferHeight = in.readUnsignedShort();
	bitsPerPixel = in.readUnsignedByte();
	depth = in.readUnsignedByte();
	bigEndian = (in.readUnsignedByte() != 0);
	trueColour = (in.readUnsignedByte() != 0);
	redMax = in.readUnsignedShort();
	greenMax = in.readUnsignedShort();
	blueMax = in.readUnsignedShort();
	redShift = in.readUnsignedByte();
	greenShift = in.readUnsignedByte();
	blueShift = in.readUnsignedByte();
	// padding
	in.skipBytes(3);
	
        int nameLength = in.readInt();
	byte[] name = new byte[nameLength];
	in.readFully(name);
	desktopName = new String(name);
        
        fullArea = new Area(new Rectangle(framebufferWidth, framebufferHeight));
        
        
        switch (bitsPerPixel) {
        case 8:
            colorModel = new DirectColorModel(8, 7, (7 << 3), (3 << 6));
            colors = new Color[256];
            for (int i = 0; i < 256; i++)
                colors[i] = new Color(colorModel.getRGB(i));
            break;
        case 16:
            colorModel = new DirectColorModel(16, 31, 31 << 5, 63 << 10);
            colors = new Color[65536];
            for (int i = 0; i < 65536; i++)
                colors[i] = new Color(colorModel.getRGB(i));
            break;
        case 24:
        case 32:
            // 32-bit input -> 24-bit output
            colorModel = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
            colors = null;
        }
    }
    
    
    /**
     * Gets an object represeing the area that would be covered by a full screen update -
     * useful for comparing the amount of the screen affected by a particular
     * <code>FramebufferMesssage</code> or group of messages.
     * @return the full area
     */
    public Area getFullArea() {
        return fullArea;
    }
        
}
