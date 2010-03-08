// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Created on 12.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import ttt.Constants;
import ttt.gui.GraphicsContext;
import ttt.postprocessing.flash.FlashContext;

import com.anotherbigidea.flash.SWFConstants;
import com.anotherbigidea.flash.movie.ImageUtil;

public class HextileMessage extends FramebufferUpdateMessage {
    // reading from stream is easier then reading from byte array
    private ByteArrayInputStream byteArrayInputStream;
    private DataInputStream is;

    // constructor
    public HextileMessage(int timestamp, int x, int y, int width, int height, byte[] data) {
        super(timestamp, x, y, width, height);
        // encoded message data without header
        this.data = data;

        // create streams to read data
        // NOTE: don't use buffered streams, because this will lead to heavy memory usage;
        byteArrayInputStream = new ByteArrayInputStream(this.data);
        is = new DataInputStream(byteArrayInputStream);
    }

    // read from TTT input stream
    public HextileMessage(int timestamp, DataInputStream in, int size) throws IOException {
        this(timestamp, in.readShort(), in.readShort(), in.readShort(), in.readShort(), FramebufferUpdateMessage
                .readBytes(in, size - 8));
    }

    // constructor - reading message from RFB input stream and directly draw rectangle to graphics context
    public HextileMessage(int timestamp, int x, int y, int width, int height, GraphicsContext graphicsContext,
            DataInputStream is) throws IOException {
        this(timestamp, x, y, width, height,
                handleAndBufferHextileRect(graphicsContext, is, x, y, width, height, false));
    }

    public int getEncoding() {
        return Constants.EncodingHextile;
    }

    // draw rectangle to graphics context
    public void paint(GraphicsContext graphicsContext) {
        try {
            byteArrayInputStream.reset();
            // if(TTT.debug){
            // int intensity = 255*getTimestamp()/(100*60000);
            // graphicsContext.memGraphics.setColor(new Color(intensity,intensity,intensity));
            // graphicsContext.memGraphics.fillRect(x, y, width, height);
            // }else
            handleHextileRect(graphicsContext, is, null, x, y, width, height, updateFlag);
            // if(TTT.debug){
            // graphicsContext.memGraphics.setColor(new
            // Color(random.nextInt(256),random.nextInt(256),random.nextInt(256)));
            // graphicsContext.memGraphics.fillRect(x, y, width, height);
            // }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Random random = new Random();

    static private byte[] handleAndBufferHextileRect(GraphicsContext graphicsContext, DataInputStream is, int x, int y,
            int w, int h, boolean updateFlag) throws IOException {
        // create buffer
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream buffer = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        // handle
        handleHextileRect(graphicsContext, is, buffer, x, y, w, h, updateFlag);

        // write buffer
        buffer.flush();
        return byteArrayOutputStream.toByteArray();
    }

    // These colors should be kept between handleHextileSubrect() calls.
    static private void handleHextileRect(GraphicsContext graphicsContext, DataInputStream is, DataOutputStream os,
            int x, int y, int w, int h, boolean updateFlag) throws IOException {

        // scan hextiles
        for (int ty = y; ty < y + h; ty += 16) {
            int th = 16;
            if (y + h - ty < 16)
                th = y + h - ty;

            for (int tx = x; tx < x + w; tx += 16) {
                int tw = 16;
                if (x + w - tx < 16)
                    tw = x + w - tx;
                handleHextileSubrect(graphicsContext, is, os, tx, ty, tw, th, updateFlag);
            }
        }
        // // mark rectangle
        // graphicsContext.memGraphics.setColor(Color.CYAN);
        // graphicsContext.memGraphics.drawRect(x, y, w, h);

        // Finished, now let's show it.
        // if (raw)
        // graphicsContext.handleUpdatedPixels(x, y, w, h);
        graphicsContext.refresh(x, y, w, h);
    }

    // Handle one tile in the Hextile-encoded data.
    static private void handleHextileSubrect(GraphicsContext graphicsContext, DataInputStream is, DataOutputStream os,
            int tx, int ty, int tw, int th, boolean updateFlag) throws IOException {
        int subencoding = is.readUnsignedByte();

        // buffering
        if (os != null)
            os.writeByte(subencoding);

        // Is it a raw-encoded sub-rectangle?
        if ((subencoding & Constants.HextileRaw) != 0) {
            handleRawRect(graphicsContext, is, os, tx, ty, tw, th);
            return;
        }

        // TODO: what if bytesPerPixel of message differ from graphicsContext???????????
        // Read and draw the background if specified.
        byte[] cbuf = new byte[graphicsContext.prefs.bytesPerPixel];

        if ((subencoding & Constants.HextileBackgroundSpecified) != 0) {
            is.readFully(cbuf);

            // buffering
            if (os != null)
                os.write(cbuf);

            // store encoded background color
            graphicsContext.setBackground(cbuf, 0);
        }

        fillRect(graphicsContext, tx, ty, tw, th, graphicsContext.hextile_bg_encoded, updateFlag);

        // Read the foreground color if specified.
        if ((subencoding & Constants.HextileForegroundSpecified) != 0) {
            is.readFully(cbuf);

            // buffering
            if (os != null)
                os.write(cbuf);

            // store encoded foreground color
            graphicsContext.setForeground(cbuf, 0);
        }

        // Done with this tile if there is no sub-rectangles.
        if ((subencoding & Constants.HextileAnySubrects) == 0)
            return;

        int nSubrects = is.readUnsignedByte();
        int bufsize = nSubrects * 2;

        if ((subencoding & Constants.HextileSubrectsColoured) != 0) {
            bufsize += nSubrects * graphicsContext.prefs.bytesPerPixel;
        }
        byte[] buf = new byte[bufsize];
        is.readFully(buf);

        // buffering
        if (os != null) {
            os.writeByte(nSubrects);
            os.write(buf);
        }

        int b1, b2, sx, sy, sw, sh;
        int i = 0;

        for (int j = 0; j < nSubrects; j++) {
            if ((subencoding & Constants.HextileSubrectsColoured) != 0) {
                // store encoded foreground color
                graphicsContext.setForeground(buf, i);

                i += graphicsContext.prefs.bytesPerPixel;
            }
            // decode subrect
            b1 = buf[i++] & 0xFF;
            b2 = buf[i++] & 0xFF;
            sx = tx + (b1 >> 4);
            sy = ty + (b1 & 0xf);
            sw = (b2 >> 4) + 1;
            sh = (b2 & 0xf) + 1;

            fillRect(graphicsContext, sx, sy, sw, sh, graphicsContext.hextile_fg_encoded, updateFlag);
        }
    }

    // handle raw encoded sub-rectangle
    static private void handleRawRect(GraphicsContext graphicsContext, DataInputStream is, DataOutputStream os, int x,
            int y, int w, int h) throws IOException {

        /***************************************************************************************************************
         * TTT 2 Flash
         **************************************************************************************************************/
        // TODO: Flash code should not be placed here
        // check if it is the first frame of the Movieclip or not
        if (firstFrameOfClip) {
            isRawRectFirstFrame = true;
        } else {
            isRawRect = true;
        }
        /***************************************************************************************************************
         * END OF TTT 2 Flash
         **************************************************************************************************************/

        switch (graphicsContext.prefs.bytesPerPixel) {
        case 1:
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(graphicsContext.pixels8, dy * graphicsContext.prefs.framebufferWidth + x, w);

                // buffering
                if (os != null)
                    os.write(graphicsContext.pixels8, dy * graphicsContext.prefs.framebufferWidth + x, w);
            }
            break;

        case 2:
            byte[] buf = new byte[w * 2];
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(buf);

                // buffering
                if (os != null)
                    os.write(buf);

                int offset = dy * graphicsContext.prefs.framebufferWidth + x;
                if (graphicsContext.prefs.bigEndian)
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 2] & 0xFF) << 8 | (buf[i * 2 + 1] & 0xFF);
                else
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 2 + 1] & 0xFF) << 8 | (buf[i * 2] & 0xFF);

            }
            break;

        default:
            buf = new byte[w * 4];
            for (int dy = y; dy < y + h; dy++) {
                is.readFully(buf);

                // buffering
                if (os != null)
                    os.write(buf);

                int offset = dy * graphicsContext.prefs.framebufferWidth + x;
                if (graphicsContext.prefs.bigEndian)
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 4 + 1] & 0xFF) << 16
                                | (buf[i * 4 + 2] & 0xFF) << 8 | (buf[i * 4 + 3] & 0xFF);
                else
                    for (int i = 0; i < w; i++)
                        graphicsContext.pixels[offset + i] = (buf[i * 4 + 2] & 0xFF) << 16
                                | (buf[i * 4 + 1] & 0xFF) << 8 | (buf[i * 4] & 0xFF);
            }
        }

        // TODO: to call or not to call???
        // NOTE: raw mode calls update for whole rectangle, not for subrectangles
        graphicsContext.handleUpdatedPixels(x, y, w, h);
    }

    // paint sub-reactangle
    static private void fillRect(GraphicsContext graphicsContext, int x, int y, int w, int h, byte[] colorField,
            boolean updateFlag) {
        if (graphicsContext.paint_to_offscreen_image) {
            Color color = graphicsContext.decodeColor(colorField);
            if (updateFlag)
                color = getGrayscale(color);
            graphicsContext.memGraphics.setColor(color);
            graphicsContext.memGraphics.fillRect(x, y, w, h);
        } else {
            int color = graphicsContext.getEncodedColor(colorField);

            for (int i = y; i < y + h; i++) {
                int offset = i * graphicsContext.prefs.framebufferWidth + x;
                for (int j = 0; j < w; j++) {
                    // TODO: add 8bit support
                    graphicsContext.pixels[offset + j] = color;
                }
            }
        }
    }

    static Color getGrayscale(Color color) {
        int argb = color.getRGB();
        int a = argb >> 24 & 0xff;
        int r = argb >> 16 & 0xff;
        int g = argb >> 8 & 0xff;
        int b = argb & 0xff;
        int gray = (r + b + g) / 3;
        r -= (int) ((r - gray) * 0.75);
        g -= (int) ((g - gray) * 0.75);
        b -= (int) ((b - gray) * 0.75);

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        color = Color.getHSBColor(hsb[0], hsb[1], hsb[2] * 0.95f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /*******************************************************************************************************************
     * TTT 2 Flash
     ******************************************************************************************************************/

    public static int depthOfHextileRects = 0;
    static boolean isRawRect = false;
    public static boolean isRawRectFirstFrame = false;
    static boolean firstFrameOfClip;
    public static boolean isNotFullImage = false;
    public static boolean isHextileRect = false;

    public void writeToFlashTester(FlashContext flashContext) throws IOException {

        flashContext.recording.graphicsContext.enableRefresh(false);

        // rset input stream and handle message data
        byteArrayInputStream.reset();
        handleHextileRect(flashContext.recording.graphicsContext, is, null, x, y, width, height, updateFlag);

        // If the hextile rect contains raw coded subrects, it is always converted with 32 bits
        int colorDepth = SWFConstants.BITMAP_FORMAT_32_BIT;

        // converts the updates rectangles only if the height and the width are greater than zero
        if (width > 0 && height > 0) {
            BufferedImage bufImage = (BufferedImage) flashContext.recording.graphicsContext.memImage;
            BufferedImage subImage = bufImage.getSubimage(x, y, width, height);
            // create a Define Bit Lossless image for subImage
            com.anotherbigidea.flash.movie.Image.Lossless img = ImageUtil.createLosslessImage(subImage, colorDepth,
                    true);
            // create a shape that uses the image as a fill
            // (images cannot be placed directly. They can only be used as shape fills)
            com.anotherbigidea.flash.movie.Shape shapeImage = ImageUtil.shapeForImage(img,
                    (double) subImage.getWidth(), (double) subImage.getHeight());
            // place hextile rect on the stage
            flashContext.frame.placeSymbol(shapeImage, x, y, depthOfHextileRects++);

            if (FlashContext.flash_debug) {
                // Shape shape = new Shape();
                // shape.defineFillStyle(new com.anotherbigidea.flash.structs.Color((int) ((Math.random()*225))+30,
                // (int) ((Math.random()*225))+30, (int) ((Math.random()*225))+30));
                // shape.defineLineStyle(2.0, new com.anotherbigidea.flash.structs.Color(0, 0, 0));
                // shape.setRightFillStyle(1);
                // shape.setLineStyle(1);
                // shape.move(0, 0);
                // shape.line(width, 0);
                // shape.line(width, height);
                // shape.line(0, height);
                // shape.line(0, 0);
                // flashContext.frame.placeSymbol(shape, x, y, depthOfHextileRects++);
            }
        }
    }

    public void writeToFlash(FlashContext flashContext) throws IOException {
    	writeToFlashORIG(flashContext);
    	//writeToFlashNEWER(flashContext);
    }

    public void writeToFlashORIG(FlashContext flashContext) throws IOException {

        firstFrameOfClip = flashContext.isFirstFrameOfClip;
        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());
        flashContext.recording.graphicsContext.enableRefresh(false);

        // rset input stream and handle message data
        byteArrayInputStream.reset();
        handleHextileRect(flashContext.recording.graphicsContext, is, null, x, y, width, height, updateFlag);

        // If the hextile rect contains raw coded subrects, it is always converted with 32 bits
        int colorDepth;
        if (isRawRect) {
            colorDepth = SWFConstants.BITMAP_FORMAT_32_BIT;
            isRawRect = false;
        } else {
            colorDepth = FlashContext.colorDepth;
        }

        // Check if it is not the first frame of the Movieclip
        if (!firstFrameOfClip) {

            // converts the updates rectangles only if the height and the width are greater than zero
            if (width > 0 && height > 0) {
                BufferedImage bufImage = (BufferedImage) flashContext.recording.graphicsContext.memImage;
                BufferedImage subImage = bufImage.getSubimage(x, y, width, height);
                
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(subImage,"jpeg",baos);

                com.anotherbigidea.flash.movie.Image img = 
                    new com.anotherbigidea.flash.movie.Image.JPEG(baos.toByteArray());
                
                // create a Define Bit Lossless image for subImage -- way to much memory consumation
                //                com.anotherbigidea.flash.movie.Image.Lossless img = ImageUtil.createLosslessImage(subImage, colorDepth,
                //                        true);
                
                // create a shape that uses the image as a fill
                // (images cannot be placed directly. They can only be used as shape fills)
                com.anotherbigidea.flash.movie.Shape shapeImage = ImageUtil.shapeForImage(img, (double) subImage
                        .getWidth(), (double) subImage.getHeight());
                // place hextile rect on the stage
                flashContext.frame.placeSymbol(shapeImage, x, y, depthOfHextileRects++);
            }
        } else {
            isHextileRect = true;
            // examines whether the size of the new hextile rect is just as large as the framebuffer
            if (height != flashContext.recording.getProtocolPreferences().framebufferHeight
                    || width != flashContext.recording.getProtocolPreferences().framebufferWidth) {
                isNotFullImage = true;
            } else {
                isNotFullImage = false;
            }
        }
    }

    public void writeToFlashNEWER(FlashContext flashContext) throws IOException {

        firstFrameOfClip = flashContext.isFirstFrameOfClip;
        flashContext.symbolCount++;
        flashContext.checkNextFrame(this.getTimestamp());
        flashContext.recording.graphicsContext.enableRefresh(false);

        // reset input stream and handle message data
        byteArrayInputStream.reset();
        handleHextileRect(flashContext.recording.graphicsContext, is, null, x, y, width, height, updateFlag);

        // If the hextile rect contains raw coded subrects, it is always converted with 32 bits
        int colorDepth;
        if (isRawRect) {
            colorDepth = SWFConstants.BITMAP_FORMAT_32_BIT;
            isRawRect = false;
        } else {
            colorDepth = FlashContext.colorDepth;
        }

        if (firstFrameOfClip || !(width > 0 && height > 0)
	    ){
	    // 28. June 2009 Petter: Bugfix for invalid page changes
	    //   || flashContext.frame.getFrameNumber() != flashContext.hextileInstanceFrameNr) {

            // write cached shape
            if (flashContext.hextileBounds != null) {
                BufferedImage bufImage = (BufferedImage) flashContext.recording.graphicsContext.memImage;

                BufferedImage subImage = bufImage.getSubimage(flashContext.hextileBounds.x,
                        flashContext.hextileBounds.y, flashContext.hextileBounds.width,
                        flashContext.hextileBounds.height);

                // subImage mit JPEG codieren!
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(subImage,"jpeg",baos);
                
                com.anotherbigidea.flash.movie.Image img = 
                    new com.anotherbigidea.flash.movie.Image.JPEG(baos.toByteArray());

                // create a shape that uses the image as a fill
                // (images cannot be placed directly. They can only be used as shape fills)
                com.anotherbigidea.flash.movie.Shape shapeImage = ImageUtil.shapeForImage(img, 
				 (double) subImage.getWidth(),(double) subImage.getHeight());
                flashContext.hextileShapeImage = shapeImage;

                // place hextile rect on the stage
                flashContext.hextileInstanceFrame.placeSymbol(flashContext.hextileShapeImage,
                        flashContext.hextileBounds.x, flashContext.hextileBounds.y, depthOfHextileRects++);
            }
            flashContext.hextileShapeImage = null;
            flashContext.hextileBounds = null;
            flashContext.hextileInstanceFrame = null;
            flashContext.hextileInstanceFrameNr = -1;
        }

        // Check if it is not the first frame of the Movieclip
        if (!firstFrameOfClip) {

            // converts the updates rectangles only if the height and the width are greater than zero
            if (width > 0 && height > 0) {
                Rectangle bounds = getBounds();
                // MODIFIED by Ziewer - 01-10-2008
                // INSERTED:
                // handle errors caused by resolution change during recording
                if (bounds.x > flashContext.recording.graphicsContext.prefs.framebufferWidth
                        || bounds.y > flashContext.recording.graphicsContext.prefs.framebufferHeight) {
                    System.out.println("Bad bounds ignored: " + this);
                    bounds = null;
                } else if (bounds.x + bounds.width > flashContext.recording.graphicsContext.prefs.framebufferWidth
                        || bounds.y + bounds.height > flashContext.recording.graphicsContext.prefs.framebufferHeight) {
                    bounds.width = flashContext.recording.graphicsContext.prefs.framebufferWidth - bounds.x;
                    bounds.height = flashContext.recording.graphicsContext.prefs.framebufferHeight - bounds.y;
                    if (bounds.width <= 0 || bounds.height <= 0)
                        System.out.println("Bad bounds ignored: " + this);
                    bounds = null;
                }
                // END MODIFIED

                if (flashContext.hextileBounds != null) {
                    // compare size of cached shape with size of combined shape (combining cached and this shape)
                    int area = flashContext.hextileBounds.width * flashContext.hextileBounds.height;
                    bounds.add(flashContext.hextileBounds);
                    int diff = bounds.width * bounds.height - area;
                    // large difference -> do not combine
                    if (diff > FlashContext.COMBINING_HEXTILES_THRESHOLD) {
                        // write cached shape
                        if (flashContext.hextileBounds != null) {
                            BufferedImage bufImage = (BufferedImage) flashContext.recording.graphicsContext.memImage;
                            BufferedImage subImage = bufImage.getSubimage(flashContext.hextileBounds.x,
                                    flashContext.hextileBounds.y, flashContext.hextileBounds.width,
                                    flashContext.hextileBounds.height);

			    // subImage mit JPEG codieren!
			    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    javax.imageio.ImageIO.write(subImage,"jpeg",baos);
                
			    com.anotherbigidea.flash.movie.Image img = 
				new com.anotherbigidea.flash.movie.Image.JPEG(baos.toByteArray());
                            // create a shape that uses the image as a fill
                            // (images cannot be placed directly. They can only be used as shape fills)
                            com.anotherbigidea.flash.movie.Shape shapeImage = ImageUtil.shapeForImage(img,
                                    (double) subImage.getWidth(), (double) subImage.getHeight());
                            flashContext.hextileShapeImage = shapeImage;

                            // place hextile rect on the stage
                            flashContext.hextileInstanceFrame.placeSymbol(flashContext.hextileShapeImage,
                                    flashContext.hextileBounds.x, flashContext.hextileBounds.y, depthOfHextileRects++);
                            flashContext.hextileShapeImage = null;
                            flashContext.hextileBounds = null;
                            flashContext.hextileInstanceFrame = null;
                            flashContext.hextileInstanceFrameNr = -1;
                        }
                        // cache this shape
                        bounds = getBounds();
                        // System.out.println("\tRESET");
                    } else {
                        // combine shapes
                        // System.out.println("COMBINE "+flashContext.frame.getFrameNumber());
                    }
                }
                flashContext.hextileBounds = bounds;
                flashContext.hextileInstanceFrame = flashContext.frame;
                flashContext.hextileInstanceFrameNr = flashContext.movieClip.getFrameCount();
            }
        } else {
            isHextileRect = true;
            // examines whether the size of the new hextile rect is just as large as the framebuffer
            if (height != flashContext.recording.getProtocolPreferences().framebufferHeight
                    || width != flashContext.recording.getProtocolPreferences().framebufferWidth) {
                isNotFullImage = true;
            } else {
                isNotFullImage = false;
            }
        }
    }
}
