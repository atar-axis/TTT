package ttt.helper;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Helper class which contains static methods for converting and resizing images.
 * The contained {@link #getScaledInstance(BufferedImage, int, int, Object, boolean)
 *  resizing method} is faster than calling Image.getScaledInstance().
 * @author Thomas Doehring
 */
public final class ImageHelper {

	/**
	 * converts a BufferedImage to a JPG with given quality
	 * @param image  the BufferedImage to convert
	 * @param quality  the JPG quality as float (from 0.0 to 1.0)
	 * @return  JPG as Byte-Array
	 * @throws Exception
	 */
	public static byte[] convertImageToJPGBytes(BufferedImage image, float quality)
	  throws Exception {
		
		Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpg");
		ImageWriter iw = null;
		if (it.hasNext()) iw = it.next();
		if (iw == null) return null;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		iw.setOutput(ImageIO.createImageOutputStream(baos));
		byte[] ret = null;
		
		JPEGImageWriteParam jpgParam = new JPEGImageWriteParam(Locale.getDefault());
		jpgParam.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
		jpgParam.setCompressionQuality(quality);
		try {
			iw.write(null, new IIOImage(image, null, null), null);
			iw.dispose();

			baos.flush();
			ret = baos.toByteArray();
			baos.close();
		} catch (IOException ioe) {
			throw new Exception("Image conversion failed");
		}
		return ret;
	}
	
    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     * @see <a href="http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html">
     * java.net:The Perils of Image.getScaledInstance()</a>
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }
        
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * creates a BufferedImage from a binary representation of an image. Codecs supported are
     * those, that the ImageIO-API supports.
     * @param data  byte-Array of the image
     * @return the BufferedImage from the image
     */
	public static BufferedImage createImageFromBytes(byte[] data) {
		try {
			return ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(data)));
		} catch (IOException ioe) { /* ignore */ }
		return null;
	}
}
