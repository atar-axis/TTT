/**
 * 
 * @author sigl ludwig
 *
 */

package ttt.videoRecorder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.media.MediaLocator;


public class VideoCreator {	
  
 public void create(String Path) throws IOException{
		try {
			
			FileInputStream fis = new FileInputStream(Path); //looks for the 'end' of a jpg
			InputStream fis2 = new  FileInputStream(Path); //actually fills the buffer
			int i;		
			String v;
			Vector<byte[]> InFiles = new Vector<byte[]>();
			byte[] buffer;
			StringBuffer strContent = new StringBuffer("");
			int FrameRate = 15;					
			
			Boolean first = true;
			while((i=  fis.read()) != -1){
				
				 strContent.append((char)i);
				 v = strContent.toString();
			
			if(v.contains("FileEnd")){
						buffer = new byte[v.length()-7];
						if(!first){
							//skipping fileEnd
						fis2.read(buffer, 0, 7);
						}
						fis2.read(buffer, 0, buffer.length);
						first = false;
																
						System.out.println(buffer.length);
						InFiles.add(buffer);
						strContent = new StringBuffer("");									
						}
			//calculates the framerate
			if(v.contains("ElapsedTime")){
				v = v.substring(0,v.length()-12);
			FrameRate =	InFiles.size() / Integer.parseInt(v);
				}				
			}
			
			 Path = Path.substring(0, Path.length()-4); //remove the ".bjpg" from path
			
			 MediaLocator outML = new MediaLocator("file:"+Path+".mov");	 
			 JpegImagesToMovie test = new JpegImagesToMovie();
			 test.doIt(160, 120, FrameRate, InFiles, outML);
			}
		 catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		
		
	}
 
 
 
}
