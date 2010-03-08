package ttt.messages;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;

import org.w3c.dom.Element;

import com.anotherbigidea.flash.movie.Image;
import com.anotherbigidea.flash.movie.ImageUtil;
import com.anotherbigidea.flash.movie.Instance;
import com.anotherbigidea.flash.movie.Shape;

import ttt.Constants;
import ttt.helper.Base64Codec;
import ttt.helper.ImageHelper;
import ttt.postprocessing.flash.FlashContext;

/**
 * Annotation which displays an image.
 * 
 * @author Thomas Doehring
 */
public class ImageAnnotation extends Annotation {

	//* binary representation of image, must be JPG due to flash writing
	private byte[] imgData = null;
	// image object for drawing
	private BufferedImage bImage = null;
	
	private int posX = 0;
	private int posY = 0;
	
	public ImageAnnotation(int timestamp, int pX, int pY, byte[] data)
	{
		this.timestamp = timestamp;
		this.posX = pX;
		this.posY = pY;
		this.imgData = data;
		
		this.bImage = ImageHelper.createImageFromBytes(data);
	}
	
	public ImageAnnotation(int timestamp, java.io.DataInputStream in) throws IOException {
		this.timestamp = timestamp;
		posX = in.readUnsignedShort();
		posY = in.readUnsignedShort();
		int length = in.readInt();
		imgData = new byte[length];
		int bytesRead = 0;
		while(bytesRead < length) {
			bytesRead += in.read(imgData, bytesRead, imgData.length - bytesRead);
		}
		
		this.bImage = ImageHelper.createImageFromBytes(imgData);
	}
	
	public ImageAnnotation(Element xmlNode) {
		posX = Integer.parseInt(xmlNode.getAttribute("posX"));
		posY = Integer.parseInt(xmlNode.getAttribute("posY"));
		imgData = Base64Codec.decode(xmlNode.getAttribute("data"));
		this.bImage = ImageHelper.createImageFromBytes(imgData);
	}
	
	@Override
	public boolean contains(int x, int y) {
		return ((x >= posX) && (x <= x + bImage.getWidth()) &&
				(y >= posY) && (y <= posY + bImage.getHeight()));
	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle(posX, posY, bImage.getWidth(), bImage.getHeight());
	}

	@Override
	public int getEncoding() {
		return Constants.AnnotationImage;
	}

	@Override
	public String getXMLString() {
		StringBuilder sb = new StringBuilder("<ImageAnnotation posX=\"");
		sb.append(posX).append("\" posY=\"").append(posY).append("\" data=\"");
		sb.append(Base64Codec.encodeToString(imgData)).append("\" />\n");
		return sb.toString();
	}

	@Override
	public void paint(Graphics2D g) {
		g.drawImage(bImage, posX, posY, null);
	}

	@Override
	public int getSize() {
		return imgData.length + 9;
	}

	@Override
	public void write(DataOutputStream out, int writeTimestamp)
			throws IOException {
		writeHeader(out, writeTimestamp);
		out.writeShort(posX);
		out.writeShort(posY);
		out.writeInt(imgData.length);
		out.write(imgData);
	}
	
	@Override
	public void writeToFlash(FlashContext flashContext) throws IOException {
		flashContext.symbolCount++;
		flashContext.checkNextFrame(this.getTimestamp());
		
		flashContext.recording.graphicsContext.enableRefresh(false);
		
		Image img = new Image.JPEG(this.imgData);
		Shape shape = ImageUtil.shapeForImage(img, (double)this.bImage.getWidth(), (double)this.bImage.getHeight());
		
		Instance instanceImg = flashContext.frame.placeSymbol(shape, this.posX, this.posY, flashContext.annotationsDepth++);
		
		flashContext.addAnnotations(this, instanceImg);
	}
}
