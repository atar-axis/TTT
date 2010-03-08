package ttt.editor.tttEditor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

/**
 * <code>FramebufferMessage</code> which uses raw encoding - each pixel is
 * encoded.
 */
public class RawMessage extends FramebufferMessage{
    
    
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
    public RawMessage(int timestamp, int encoding, int area, byte [] data, Header header) {
        super(timestamp, encoding, area, data, header);
        x = getUnsignedShort();
        y = getUnsignedShort();
        width = getUnsignedShort();
        height = getUnsignedShort();
    }
    
        

//PB: Lot of editing - could well be horribly wrong
//Still need to test
    /**
     * Method for painting the screen data contained within this message.
     *  <br />
     * NOTE:- This method may not have been tested.
     * @param g <code>Graphics</code> context upon which to paint the contents
     * of this <code>FramebufferMessage</code>
     */
    public void paint(Graphics g) {

        bytePointer = 8;
        
        switch (header.bitsPerPixel) {
        case 8:
            byte [] dataArray8 = new byte[width * height];
            MemoryImageSource pixelsSource = new MemoryImageSource(width, height, header.colorModel, dataArray8, 0, width);
            Image rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
            pixelsSource.setAnimated(true);            
            readFully(dataArray8);
            pixelsSource.newPixels();
            g.drawImage(rawPixelsImage, x, y, null);
            break;
            
        case 16:
            int [] dataArray = new int[width * height];
            pixelsSource = new MemoryImageSource(width, height, header.colorModel, dataArray, 0, width);
            rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
            pixelsSource.setAnimated(true);
            
            for (int j = 0; j < height; j++) {
                int offset = j * width;
                for (int count = 0; count < width; count++) {
                    dataArray[offset + count] = getUnsignedShort();
                }
            }
            pixelsSource.newPixels();
            g.drawImage(rawPixelsImage, x , y, null);
            break;
        case 24:
            dataArray = new int[width * height];
            pixelsSource = new MemoryImageSource(width, height, header.colorModel, dataArray, 0, width);
            rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
            pixelsSource.setAnimated(true);
            
            for (int j = 0; j < height; j++) {
                int offset = j * width;
                for (int count = 0; count < width; count++) {
                    dataArray[offset + count] = (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF);
                }
            }
            pixelsSource.newPixels();
            g.drawImage(rawPixelsImage, x , y, null);
            break;
        case 32:
            dataArray = new int[width * height];
            pixelsSource = new MemoryImageSource(width, height, header.colorModel, dataArray, 0, width);
            rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
            pixelsSource.setAnimated(true);
            
            for (int j = 0; j < height; j++) {
                int offset = j * width;
                for (int count = 0; count < width; count++) {
                    skipBytes(1);
                    dataArray[offset + count] = (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF);
                }
            }
            pixelsSource.newPixels();
            g.drawImage(rawPixelsImage, x , y, null);
            break;
        }
    }
        
    
}
