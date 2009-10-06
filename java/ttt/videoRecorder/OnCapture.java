/**
 * 
 * @author sigl ludwig
 *
 */
package ttt.videoRecorder;



import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import javax.swing.ImageIcon;

import com.lti.civil.Image;
import com.lti.civil.awt.AWTImageConverter;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

//
// Gets called every time a new picture is taken. it saves them under the recordpath set direction
//
public class OnCapture {	

	static private String RecordPath = "C:\\TTTImplement\\";

	public static String getRecordPath(){
		return OnCapture.RecordPath;
	}
	
	public void onNewImage(Image image, String RecordPath)
	{	
		OnCapture.RecordPath = RecordPath;
		final BufferedImage bimg;
		try
		{
			bimg = AWTImageConverter.toBufferedImage(image);
		}
		catch (Exception e)		
		{	e.printStackTrace();
			return;
		}

		try
		{
			File ImageFile = new File( RecordPath+".bjpg");
			
			FileOutputStream fos = new FileOutputStream (ImageFile, true);
			
			JPEGImageEncoder jpeg =  JPEGCodec.createJPEGEncoder(fos);
			
			JPEGEncodeParam EncodePar =  jpeg.getDefaultJPEGEncodeParam(bimg);			
			
			EncodePar.setQuality(0.1f, true);
			
			jpeg.setJPEGEncodeParam(EncodePar);		
		
			jpeg.encode(bimg);			
			
				
			fos.close(); 
			FileWriter fw = new FileWriter(ImageFile, true);
			fw.write(" FileEnd");
			fw.close();
			
			ImageIcon myImage2 = new ImageIcon();
			myImage2.setImage(bimg);
			VideoRecorder.myLabel.setIcon(myImage2);	
		}
		catch (Exception e)
		{				
			e.printStackTrace();
		}
	}
}
