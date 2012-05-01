package ttt.postprocessing.html5;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ttt.TTT;

public class Html5PlayerCreator {
	private static final String PLAYER_ZIP_ARCHIVE = "resources/webplayer.zip";

	public static void createPlayer(File html5dir) throws IOException {
		if (!html5dir.isDirectory()) {
			html5dir.mkdir();
		}
		
		//get path to zipfile, independent of the TTT being in a jar file or in source format.
		URL zipfile = new URL(TTT.class.getResource("Constants.class").toString().replaceFirst("ttt/Constants.class", PLAYER_ZIP_ARCHIVE));
		
		InputStream is = zipfile.openStream();
		BufferedInputStream bis=new BufferedInputStream(is);
		ZipInputStream zis=new ZipInputStream(bis);
		
		//unzip the zip folder containing the player files
		try {
			byte[] buffer = new byte[2048];
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (!ze.isDirectory()) {
					String fileName = ze.getName();
					File newFile = new File(html5dir + File.separator + fileName);
	
					//create directories
					new File(newFile.getParent()).mkdirs();
	
					FileOutputStream fos = new FileOutputStream(newFile);             
	
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
	
					fos.close();
				}
			}
			
		} catch (IOException e) {
			throw e;
		} finally {
			zis.closeEntry();
			zis.close();
		}
	}
	
	//used for copying audio files
	public static void copyFile(File in, File out) throws IOException {
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			// magic number for Windows, 64Mb - 32Kb)
			int maxCount = (64 * 1024 * 1024) - (32 * 1024);
			long size = inChannel.size();
			long position = 0;
			while (position < size) {
				position += inChannel
						.transferTo(position, maxCount, outChannel);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}
}
