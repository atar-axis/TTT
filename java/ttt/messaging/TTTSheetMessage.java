package ttt.messaging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import ttt.helper.Base64Codec;
import ttt.helper.ImageHelper;
import ttt.messages.*;

/**
 * A sheet message can contain an image (sheet image, i.e. the image data of the VNC session
 * of the lecturer), annotations and/or a text.
 * @author Thomas Doehring
 */
public class TTTSheetMessage extends TTTMessage {

	private int sheetWidth = 800;
	private int sheetHeight = 600;
	private byte[] imgData = null;
	private BufferedImage sheetImage = null;
	private Annotation[] annotations = null;
	private String text = null;
	
	// cache thumbnail
	private BufferedImage thumbCache;

	public TTTSheetMessage(byte[] imgData) {
		this.imgData = imgData;
		this.sheetImage = ImageHelper.createImageFromBytes(imgData);
	}
	
	public TTTSheetMessage(Annotation[] annots) {
		this.annotations = annots;
	}
	
	public TTTSheetMessage(BufferedImage sheetImage, Annotation[] annotations) {
		this.sheetImage = sheetImage;
		this.annotations = annotations;
	}
	
	// getters and setters
	public BufferedImage getImage() { return sheetImage; }
	public void setImage(BufferedImage img) { this.sheetImage = img; }
	public byte[] getImageData() { return this.imgData; }
	public Annotation[] getAnnotations() { return annotations; }
	public void setAnnotations(Annotation[] annots) { this.annotations = annots; }
	public String getText() { return this.text; }
	public void setText(String text) { this.text = text; }
	
	public boolean isWhiteboardSheet() { return (sheetImage == null); }
	public boolean hasAnnotations() { return (annotations != null); }
	public boolean hasText() { return (text != null); }
	
	/**
	 * creates the tttmessage XML string representation
	 * @param annotated  With annotations?
	 * @return tttmessage XML string
	 */
	public String toXMLString(boolean annotated) {
		StringBuilder sb = new StringBuilder("<tttmessage type=\"content\">\n  <sheet>\n");
		if (sheetImage != null) {
			// convert image to base64 string
			try {
				float quality = (float)(ttt.messaging.server.TTTMessaging.JPEG_QUALITY / 100f);
				byte[] data = ImageHelper.convertImageToJPGBytes(sheetImage, quality);
				String sData = Base64Codec.encodeToString(data);
				sb.append("    <bgimage data=\"").append(sData).append("\" />\n");
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		if (annotated && annotations != null) {
			sb.append("<annotations>\n");
			for (int i = 0; i < annotations.length; i++) {
				sb.append(annotations[i].getXMLString());
			}
			sb.append("</annotations>\n");
		}
		sb.append("  </sheet>\n</tttmessage>");
		return sb.toString();
	}
	
	/**
	 * creates a thumbnail of the sheet image and/or the annotations.
	 * @param width  width of needed thumbnail
	 * @return  thumbnail as BufferedImage with width and correct aspect ratio
	 */
	public BufferedImage getThumbnail(int width) {
		// cached image usable?
		if (thumbCache != null && thumbCache.getWidth() == width) {
			return thumbCache;
		}
		
		// generate new thumbnail
		BufferedImage newThumb = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = newThumb.createGraphics();
		
		// draw sheet 
		if(sheetImage != null) {
			g.drawImage(sheetImage, 0, 0, null);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, sheetWidth, sheetHeight);
		}
		
		// draw annotations
		if (annotations != null) {
			for (Annotation ann : annotations) {
				ann.paint(g);
			}
		}
		
		// calculate height
		int height = width * sheetHeight / sheetWidth;
		// scale down to thumb size
		thumbCache = ImageHelper.getScaledInstance(newThumb, width, height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
		return thumbCache;
	}
	
	/**
	 * sheet size must be set for messages which only contains annotations
	 * (whiteboards created by client), so that the annotations can be rendered
	 * onto a suitably sized empty (white) image.
	 * Set by {@link ttt.messaging.server.ServerMessageParser}
	 */
	public void setSheetSize(Dimension d) {
		this.sheetHeight = d.height;
		this.sheetWidth = d.width;
	}
}
