package ttt.editor.tttEditor;

/**Screen update message, uses Hextile Encoding.
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

/**
 * <code>FramebufferMessage</code> which uses hextile encoding.
 */
public class HextileMessage extends FramebufferMessage {

    protected final static int HextileRaw = (1 << 0);
    protected final static int HextileBackgroundSpecified = (1 << 1);
    protected final static int HextileForegroundSpecified = (1 << 2);
    protected final static int HextileAnySubrects = (1 << 3);
    protected final static int HextileSubrectsColoured = (1 << 4);

    // could be removed - not relevant for recorded files?
    boolean update = false;

    /**
     * Class constructor
     * 
     * @param timestamp
     *            in milliseconds
     * @param encoding
     *            the precise type of <code>Message</code>
     * @param data
     *            byte array containing the main data for the <code>Message</code>
     * @param area
     *            sum of the amount of the screen covered by this <code<FramebufferMessage</code>, and any other
     *            similar messages previously initialized with the same timestamp - used for generating an index.
     * @param header
     *            object containing data specific to the TTTFile, and necessary for processing any message
     */
    public HextileMessage(int timestamp, int encoding, int area, byte[] data, Header header) {
        super(timestamp, encoding, area, data, header);
        x = getUnsignedShort();
        y = getUnsignedShort();
        width = getUnsignedShort();
        height = getUnsignedShort();
    }

    /**
     * Method for painting the screen data contained within this message.
     * 
     * @param g
     *            <code>Graphics</code> context upon which to paint the contents of this
     *            <code>FramebufferMessage</code>
     */
    public synchronized void paint(Graphics g) {
        bytePointer = 8;
        switch (header.bitsPerPixel) {
        case 8:
            paintHextileRect8(g);
            break;
        case 16:
            paintHextileRect16(g);
            // handleHextileRect16(g);
            break;
        case 24:
            paintHextileRect24(g);
            break;
        case 32:
            paintHextileRect32(g);
            break;
        }
    }

    private void paintHextileRect8(Graphics g) {
        // 8 bits per pixel

        for (int ty = 0; ty < height; ty += 16) {

            int th = 16;
            if (height - ty < 16)
                th = height - ty;

            for (int tx = 0; tx < width; tx += 16) {

                int tw = 16;
                if (width - tx < 16)
                    tw = width - tx;

                int subencoding = getUnsignedByte();

                // Is it a raw-encoded sub-rectangle?
                if ((subencoding & HextileRaw) != 0) {
                    byte[] dataArray = new byte[tw * th];
                    MemoryImageSource pixelsSource = new MemoryImageSource(tw, th, header.colorModel, dataArray, 0, tw);
                    Image rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
                    pixelsSource.setAnimated(true);

                    readFully(dataArray);

                    pixelsSource.newPixels();
                    g.drawImage(rawPixelsImage, x + tx, y + ty, null);
                    continue;
                }
                // Read and draw the background if specified.
                if ((subencoding & HextileBackgroundSpecified) != 0) {
                    header.bg = header.colors[getUnsignedByte()];
                }
                g.setColor(update ? getGrayscale(header.bg) : header.bg);
                g.fillRect(x + tx, y + ty, tw, th);

                // Read the foreground color if specified.
                if ((subencoding & HextileForegroundSpecified) != 0) {
                    header.fg = header.colors[getUnsignedByte()];
                }

                // Done with this tile if there is no sub-rectangles.
                if ((subencoding & HextileAnySubrects) == 0)
                    continue;

                int nSubrects = getUnsignedByte();

                int b1, b2, sx, sy, sw, sh;
                // BGR233 (8-bit color) version.
                if ((subencoding & HextileSubrectsColoured) != 0) {
                    for (int j = 0; j < nSubrects; j++) {
                        header.fg = header.colors[getUnsignedByte()];
                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.setColor(update ? getGrayscale(header.fg) : header.fg);
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                } else {
                    g.setColor(update ? getGrayscale(header.fg) : header.fg);
                    for (int j = 0; j < nSubrects; j++) {
                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                }
            }
        }
    }

    private void paintHextileRect24(Graphics g) {
        // 24 bits per pixel

        for (int ty = 0; ty < height; ty += 16) {

            int th = 16;
            if (height - ty < 16)
                th = height - ty;

            for (int tx = 0; tx < width; tx += 16) {

                int tw = 16;
                if (width - tx < 16)
                    tw = width - tx;

                int subencoding = getUnsignedByte();

                // Is it a raw-encoded sub-rectangle?
                if ((subencoding & HextileRaw) != 0) {
                    int[] dataArray = new int[tw * th];
                    MemoryImageSource pixelsSource = new MemoryImageSource(tw, th, header.colorModel, dataArray, 0, tw);
                    Image rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
                    pixelsSource.setAnimated(true);

                    for (int j = 0; j < th; j++) {
                        int offset = j * tw;
                        for (int count = 0; count < tw; count++) {
                            if (header.bigEndian)
                                dataArray[offset + count] = (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                        | (getByte() & 0xFF);
                            else
                                dataArray[offset + count] = (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                        | (getByte() & 0xFF) << 16;

                        }
                    }
                    pixelsSource.newPixels();
                    g.drawImage(rawPixelsImage, x + tx, y + ty, null);
                    continue;
                }
                // Read and draw the background if specified.
                if ((subencoding & HextileBackgroundSpecified) != 0) {
                    if (header.bigEndian)
                        header.bg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF));
                    else
                        header.bg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF) << 16);
                }

                g.setColor(update ? getGrayscale(header.bg) : header.bg);
                g.fillRect(x + tx, y + ty, tw, th);

                // Read the foreground color if specified.
                if ((subencoding & HextileForegroundSpecified) != 0) {
                    if (header.bigEndian)
                        header.fg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF));
                    else
                        header.fg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF) << 16);
                }

                // Done with this tile if there is no sub-rectangles.
                if ((subencoding & HextileAnySubrects) == 0)
                    continue;

                int nSubrects = getUnsignedByte();

                int b1, b2, sx, sy, sw, sh;
                // Full-color (24-bit) version.
                if ((subencoding & HextileSubrectsColoured) != 0) {

                    for (int j = 0; j < nSubrects; j++) {
                        if (header.bigEndian)
                            header.fg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF));
                        else
                            header.fg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF) << 16);

                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.setColor(update ? getGrayscale(header.fg) : header.fg);
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                } else {
                    g.setColor(update ? getGrayscale(header.fg) : header.fg);
                    for (int j = 0; j < nSubrects; j++) {
                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                }
            }
        }
    }

    private void paintHextileRect32(Graphics g) {
        // 32 bits per pixel

        for (int ty = 0; ty < height; ty += 16) {

            int th = 16;
            if (height - ty < 16)
                th = height - ty;

            for (int tx = 0; tx < width; tx += 16) {

                int tw = 16;
                if (width - tx < 16)
                    tw = width - tx;

                int subencoding = getUnsignedByte();

                // Is it a raw-encoded sub-rectangle?
                if ((subencoding & HextileRaw) != 0) {
                    int[] dataArray = new int[tw * th];
                    MemoryImageSource pixelsSource = new MemoryImageSource(tw, th, header.colorModel, dataArray, 0, tw);
                    Image rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
                    pixelsSource.setAnimated(true);

                    for (int j = 0; j < th; j++) {
                        int offset = j * tw;
                        for (int count = 0; count < tw; count++) {
                            if (header.bigEndian) {
                                skipBytes(1);
                                dataArray[offset + count] = (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                        | (getByte() & 0xFF);
                            } else {
                                dataArray[offset + count] = (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                        | (getByte() & 0xFF) << 16;
                                skipBytes(1);
                            }
                        }

                    }
                    pixelsSource.newPixels();
                    g.drawImage(rawPixelsImage, x + tx, y + ty, null);
                    continue;
                }
                // Read and draw the background if specified.
                if ((subencoding & HextileBackgroundSpecified) != 0) {
                    if (header.bigEndian) {
                        skipBytes(1);
                        header.bg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF));
                    } else {
                        header.bg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF) << 16);
                        skipBytes(1);
                    }
                }

                g.setColor(update ? getGrayscale(header.bg) : header.bg);
                g.fillRect(x + tx, y + ty, tw, th);

                // Read the foreground color if specified.
                if ((subencoding & HextileForegroundSpecified) != 0) {
                    if (header.bigEndian) {
                        skipBytes(1);
                        header.fg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF));
                    } else {
                        header.fg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                | (getByte() & 0xFF) << 16);
                        skipBytes(1);
                    }
                }

                // Done with this tile if there is no sub-rectangles.
                if ((subencoding & HextileAnySubrects) == 0)
                    continue;

                int nSubrects = getUnsignedByte();

                int b1, b2, sx, sy, sw, sh;
                // Full-color (24-bit) version.
                if ((subencoding & HextileSubrectsColoured) != 0) {

                    for (int j = 0; j < nSubrects; j++) {
                        if (header.bigEndian) {
                            skipBytes(1);
                            header.fg = new Color(0xFF000000 | (getByte() & 0xFF) << 16 | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF));
                        } else {
                            header.fg = new Color(0xFF000000 | (getByte() & 0xFF) | (getByte() & 0xFF) << 8
                                    | (getByte() & 0xFF) << 16);
                            skipBytes(1);
                        }

                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.setColor(update ? getGrayscale(header.fg) : header.fg);
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                } else {
                    g.setColor(update ? getGrayscale(header.fg) : header.fg);
                    for (int j = 0; j < nSubrects; j++) {
                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                }
            }
        }
    }

    private void paintHextileRect16(Graphics g) {
        // 16 bits per pixel

        // PB: think of the t as meaning "temporary"
        // PB: think of the r as meaning "raw"

        for (int ty = 0; ty < height; ty += 16) {

            // PB: sets the height of the hextile,
            // which will be 16 unless e.g. at edge of the screen
            int th = 16;
            if (height - ty < 16)
                th = height - ty;

            for (int tx = 0; tx < width; tx += 16) {

                // PB: sets the width of the hextile,
                // which will be 16 unless e.g. at edge of the screen
                int tw = 16;
                if (width - tx < 16)
                    tw = width - tx;

                int subencoding = getUnsignedByte();

                // Is it a raw-encoded sub-rectangle?
                if ((subencoding & HextileRaw) != 0) {
                    int[] dataArray = new int[tw * th];
                    MemoryImageSource pixelsSource = new MemoryImageSource(tw, th, header.colorModel, dataArray, 0, tw);
                    Image rawPixelsImage = TTTEditor.getInstance().createImage(pixelsSource);
                    pixelsSource.setAnimated(true);

                    for (int j = 0; j < th; j++) {
                        int offset = j * tw;
                        for (int count = 0; count < tw; count++) {
                            if (header.bigEndian)
                                dataArray[offset + count] =  (getByte() & 0xFF) << 8 | (getByte() & 0xFF);
                            else
                                dataArray[offset + count] = (getByte() & 0xFF) | (getByte() & 0xFF) << 8;
                        }
                    }

                    pixelsSource.newPixels();
                    g.drawImage(rawPixelsImage, x + tx, y + ty, null);
                    continue;
                }
                // Read and draw the background if specified.
                if ((subencoding & HextileBackgroundSpecified) != 0) {
                    if(header.bigEndian)
                        header.bg = header.colors[(getByte() & 0xFF) << 8 | (getByte() & 0xFF)];
                    else
                        header.bg = header.colors[(getByte() & 0xFF) | (getByte() & 0xFF) << 8];
                }

                g.setColor(update ? getGrayscale(header.bg) : header.bg);
                g.fillRect(x + tx, y + ty, tw, th);

                // Read the foreground color if specified.
                if ((subencoding & HextileForegroundSpecified) != 0) {
                    if(header.bigEndian)
                        header.fg = header.colors[(getByte() & 0xFF) << 8 | (getByte() & 0xFF)];
                    else
                        header.fg = header.colors[(getByte() & 0xFF) | (getByte() & 0xFF) << 8];
                }

                // Done with this tile if there is no sub-rectangles.
                if ((subencoding & HextileAnySubrects) == 0)
                    continue;

                int nSubrects = getUnsignedByte();

                int b1, b2, sx, sy, sw, sh;
                // Full-color (16-bit) version.
                if ((subencoding & HextileSubrectsColoured) != 0) {
                    for (int j = 0; j < nSubrects; j++) {
                        if(header.bigEndian)
                            header.fg = header.colors[(getByte() & 0xFF) << 8 | (getByte() & 0xFF)];
                        else
                            header.fg = header.colors[(getByte() & 0xFF) | (getByte() & 0xFF) << 8];

                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.setColor(update ? getGrayscale(header.fg) : header.fg);
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                } else {
                    g.setColor(update ? getGrayscale(header.fg) : header.fg);
                    for (int j = 0; j < nSubrects; j++) {
                        b1 = getUnsignedByte();
                        b2 = getUnsignedByte();
                        sx = tx + (b1 >> 4);
                        sy = ty + (b1 & 0xf);
                        sw = (b2 >> 4) + 1;
                        sh = (b2 & 0xf) + 1;
                        g.fillRect(x + sx, y + sy, sw, sh);
                    }
                }
            }
        }
    }

    // Not relevant to editor - could be deleted
    private static Color getGrayscale(Color color) {
        int argb = color.getRGB();
        int a = argb >> 24 & 0xff;
        int r = argb >> 16 & 0xff;
        int g = argb >> 8 & 0xff;
        int b = argb & 0xff;
        int gray = (r + b + g) / 3;
        r -= (int) ((r - gray) * 0.75);
        g -= (int) ((g - gray) * 0.75);
        b -= (int) ((b - gray) * 0.75);
        // return new Color(r,g,b,a);

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        color = Color.getHSBColor(hsb[0], hsb[1], hsb[2] * 0.95f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

}