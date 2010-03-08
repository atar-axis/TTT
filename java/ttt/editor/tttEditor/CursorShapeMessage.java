package ttt.editor.tttEditor;


import java.awt.image.MemoryImageSource;
import java.awt.Image;


/**
 * A <code>Message</code> which stores the shape of the cursor at a 
 * given timestamp.
 */
public class CursorShapeMessage extends CursorMessage {
        
    
    private SoftCursor cursor;

    
    /**
     * Class constructor
     * @param timestamp in milliseconds
     * @param encoding the precise type of <code>Message</code>
     * @param data byte array containing the main data for the <code>Message</code>
     * @param header object containing data specific to the TTTFile, and
     * necessary for processing any message
     */
    public CursorShapeMessage(int timestamp, int encoding, byte [] data, Header header) {
        super(timestamp, encoding, data, header);
        cursor = createNewCursor(header);
    }

    
    /**
     * Get a cursor with the shape dictated by this message.
     * The returned cursor will have a position (0, 0) and should be immediately
     * updated with the correct cursor position.
     * @return a cursor of the shape encoded by this <code>Message</code>.
     */
    public SoftCursor getCursor() {
        return cursor;
    }

    
    
    //creates the softCursorPixels array for the given cursor
    private SoftCursor createNewCursor(Header header) {
        
        int[] softCursorPixels;
        int xhot, yhot, width, height;
        
        
        xhot = getUnsignedShort();
        yhot = getUnsignedShort();
        width = getUnsignedShort();
        height = getUnsignedShort();

        
        int bytesPerRow = (width + 7) / 8;
        int bytesMaskData = bytesPerRow * height;

        if (width * height == 0)
            return null;

        // Ignore cursor shape data if requested by user.


        // Decode cursor pixel data.
        softCursorPixels = new int[width * height];

        if (encoding == ProtocolConstants.EncodingTTTXCursor || encoding == ProtocolConstants.EncodingXCursor) {
            // Read foreground and background colors of the cursor.
            byte[] rgb = new byte[6];
            readFully(rgb);
            int[] colors = { (0xFF000000 | (rgb[3] & 0xFF) << 16 | (rgb[4] & 0xFF) << 8 | (rgb[5] & 0xFF)),
                    (0xFF000000 | (rgb[0] & 0xFF) << 16 | (rgb[1] & 0xFF) << 8 | (rgb[2] & 0xFF)) };

            // Read pixel and mask data.
            byte[] pixBuf = new byte[bytesMaskData];
            readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            readFully(maskBuf);

            // Decode pixel data into softCursorPixels[].
            byte pixByte, maskByte;
            int x, y, n, result;
            int i = 0;
            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    pixByte = pixBuf[y * bytesPerRow + x];
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            result = colors[pixByte >> n & 1];
                        } else {
                            result = 0; // Transparent pixel
                        }
                        softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        result = colors[pixBuf[y * bytesPerRow + x] >> n & 1];
                    } else {
                        result = 0; // Transparent pixel
                    }
                    softCursorPixels[i++] = result;
                }
            }

        } else {
            byte[] pixBuf = new byte[width * height * header.bitsPerPixel / 8];
            readFully(pixBuf);
            byte[] maskBuf = new byte[bytesMaskData];
            readFully(maskBuf);

            // Decode pixel data into softCursorPixels[].
            byte maskByte;
            int x, y, n, result;
            int i = 0;

            for (y = 0; y < height; y++) {
                for (x = 0; x < width / 8; x++) {
                    maskByte = maskBuf[y * bytesPerRow + x];
                    for (n = 7; n >= 0; n--) {
                        if ((maskByte >> n & 1) != 0) {
                            if (header.bitsPerPixel == 8) {
                                result = header.colorModel.getRGB(pixBuf[i]);
                            } else if (header.bitsPerPixel == 16) {
                                // get rgb value
                                int rgb = (pixBuf[i * 2 + 0] & 0xFF) << 8 | (pixBuf[i * 2 + 1] & 0xFF);
                                // shift rgb values and stretch them to byte
                                // size
                                int r = (rgb & header.redMax) * 255 / header.redMax;
                                int g = ((rgb >> header.greenShift) & header.greenMax) * 255
                                        / header.greenMax;
                                int b = ((rgb >> header.blueShift) & header.blueMax) * 255 / header.blueMax;

                                result = 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
                            } else if (header.bitsPerPixel == 24) {
                                result = 0xFF000000 | (pixBuf[i * 3 + 0] & 0xFF) << 16
                                        | (pixBuf[i * 3 + 1] & 0xFF) << 8 | (pixBuf[i * 3 + 2] & 0xFF);
                            } else { // 32 bit
                                result = 0xFF000000 | (pixBuf[i * 4 + 1] & 0xFF) << 16
                                        | (pixBuf[i * 4 + 2] & 0xFF) << 8 | (pixBuf[i * 4 + 3] & 0xFF);
                            }
                        } else {
                            result = 0; // Transparent pixel
                        }
                        softCursorPixels[i++] = result;
                    }
                }
                for (n = 7; n >= 8 - width % 8; n--) {
                    if ((maskBuf[y * bytesPerRow + x] >> n & 1) != 0) {
                        if (header.bitsPerPixel == 8) {
                            result = header.colorModel.getRGB(pixBuf[i]);
                        } else if (header.bitsPerPixel == 16) {
                            // get rgb value
                            int rgb = (pixBuf[i * 2 + 0] & 0xFF) << 8 | (pixBuf[i * 2 + 1] & 0xFF);
                            // shift rgb values and stretch them to byte size
                            int r = (rgb & header.redMax) * 255 / header.redMax;
                            int g = ((rgb >> header.greenShift) & header.greenMax) * 255 / header.greenMax;
                            int b = ((rgb >> header.blueShift) & header.blueMax) * 255 / header.blueMax;

                            result = 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
                        } else if (header.bitsPerPixel == 24) {
                            result = 0xFF000000 | (pixBuf[i * 3] & 0xFF) << 16 | (pixBuf[i * 3 + 1] & 0xFF) << 8
                                    | (pixBuf[i * 3 + 2] & 0xFF);
                        } else { // 32 bit
                            result = 0xFF000000 | (pixBuf[i * 4 + 1] & 0xFF) << 16 | (pixBuf[i * 4 + 2] & 0xFF) << 8
                                    | (pixBuf[i * 4 + 3] & 0xFF);
                        }
                    } else {
                        result = 0; // Transparent pixel
                    }
                    softCursorPixels[i++] = result;
                }
            }
        }
        
        MemoryImageSource softCursorSource = new MemoryImageSource(width, height, softCursorPixels, 0, width);
        Image cursorImage = TTTEditor.getInstance().createImage(softCursorSource);
        
        return new SoftCursor(cursorImage, width, height, xhot, yhot);
    } 
    
}
