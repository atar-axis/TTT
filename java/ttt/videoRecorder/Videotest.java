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



public class Videotest {

public static void main(String args[]) throws IOException {
	
	//VideoRecorder Test = new VideoRecorder();	Test.Start();
	blub();
	
			}
public static void blub() throws IOException{
	try {
		FileInputStream fis = new FileInputStream("C:\\imgbuff\\out5.bjpg");
		InputStream fis2 = new  FileInputStream("C:\\imgbuff\\out5.bjpg");
		int i;
		String v;
		Vector<byte[]> InFiles = new Vector<byte[]>();
		byte[] buffer;
		StringBuffer strContent = new StringBuffer("");

				
		int offset = 0;
Boolean first = true;
		while((i=  fis.read()) != -1){
			
			 strContent.append((char)i);
			 v = strContent.toString();
		
		if(v.contains("FileEnd")){
					buffer = new byte[v.length()-7];
					if(!first){
					fis2.read(buffer, offset, 7);}
					fis2.read(buffer, offset, buffer.length);
					first = false;
					offset = 0;													
					System.out.println(buffer.length);
					//InFiles.add(v.substring(0,v.length()-7));
					InFiles.add(buffer);
					strContent = new StringBuffer("");									
					}
		}
		
		
		
		
		
		
		 JpegImagesToMovie test;	 
		 
		 MediaLocator outML;
		 
		 String mediaFile = "file:C:\\imgbuff\\x4.mov";
		outML = new MediaLocator(mediaFile);	 
		 test = new JpegImagesToMovie();
		 test.doIt(160, 120, 15, 			
				InFiles
				 , outML);
		}
	 catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
}
}


