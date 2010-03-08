package ttt.video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

//handles the incoming pictures for linuxcam and windowscam
public abstract class CaptureHandler {
	public abstract void onNewImage(byte[] image, String RecordPath, float Quality);
	
	public byte[] compressJpegFile(byte[] inbytes,
			float compressionQuality) {
		//1.0f equals no compression
		if (Float.compare(compressionQuality, 1.0f) != 0) { 
			try {

				BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(inbytes));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				JPEGImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
				iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwparam.setCompressionQuality(compressionQuality);
				ImageWriter writer = null;
				Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
				if (iter.hasNext()) {
					writer = iter.next();
				}
				MemoryCacheImageOutputStream mciis= new MemoryCacheImageOutputStream(baos);
				writer.setOutput(mciis);
				writer.write(null,new IIOImage(bimg,null,null),iwparam);

				byte[] bytesOut = baos.toByteArray();

				baos.close();
				return bytesOut;
			} catch (IOException ioe) {
				System.out.println("write error: " + ioe.getMessage());
				return null;
			}
		}
		return inbytes;
	}
}
