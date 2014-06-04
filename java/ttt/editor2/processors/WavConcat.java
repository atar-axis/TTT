package ttt.editor2.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * 
 * @author Ludwig This class is meant to concat wav files.
 */
public class WavConcat {
	

	private File[] InFiles;
	private File OutFile;
	
	byte[] header = new byte[44]; // stores the header for the wav. (wav header size is are 44 byte)

	byte[] InBuffer; //reads each file and writes them then into the OutBuffer
	byte[] Offset = new byte[44]; //used to skip the .wav header of the files
	FileInputStream fileIn;
	int Position = 44; //where to paste the new byte in the OutBuffer
	long[] durations; //saves the duration of each AudioIn File

	/**
	 * 
	 * @param AudioIn The in Array of the Audio Files
	 * @param AudioOut	The out file were to write the new Audio file to
	 */
	
	public WavConcat(File[] AudioIn, File AudioOut) {
		InFiles = AudioIn;
	
		for (File i : AudioIn) {
			if(!i.toString().endsWith(".wav")) {
				System.err.println("Error! Can only Concat wav files! : " + i.toString() + " is not.");
			}
		}			
		OutFile = AudioOut;
		durations = new long[AudioIn.length];
	}

	/**
	 * Assumes doit was already called
	 * 
	 * @return return the duration of the each audio file in AudioIn
	 */
	public long[] getDurationNanoSeconds() {
		return durations;
	}
	


	int count = 0;

	/**
	 * Calculate the duration from the header and save it.
	 */
	private void saveDuration() {
		for(File i: InFiles) {
			try {
				InBuffer = new byte[(int) (i.length() - Offset.length)];
				fileIn = new FileInputStream(i);

				fileIn.read(Offset); 
		
		//Duration = chunk/ bitrate (chunk = informationsize)
		//Reading out Bitrate
		byte[] rate = new byte[4];
		rate[3] = Offset[28];
		rate[2] = Offset[29];
		rate[1] = Offset[30];
		rate[0] = Offset[31];

		//Reading chunk
		byte[] chunk = new byte[4];
		chunk[3] = Offset[4];
		chunk[2] = Offset[5];
		chunk[1] = Offset[6];
		chunk[0] = Offset[7];

		durations[count++] = TimeUnit.SECONDS.toNanos((convertByteArrayToInt(chunk) / convertByteArrayToInt(rate)));
		System.out.println(count+ "th Audiofile with length  " + durations[count-1]/60000000000l +" sec");
			}catch (FileNotFoundException e) {
				System.err.println("Error reading file: " + i.toString());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Error reading file: " + i.toString());
				e.printStackTrace();
			}
			}
		}
	
	

		private int convertByteArrayToInt(byte[] bytebuf) {
	 		
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.put(bytebuf);
			 			
			return buffer.getInt(0);
		}
	

	/**
	 * Read the AudioIn concat them and write into the AudioOut
	 */
	public boolean doIt() {
		
		saveDuration();
		
		int framelength=0;
		Vector<AudioInputStream> vec = new Vector<AudioInputStream>();
		try {
		    for (int i=0;i<InFiles.length;i++){
			AudioInputStream clip = AudioSystem.getAudioInputStream(InFiles[i]);
			framelength+=clip.getFrameLength();
			vec.add(clip);
		    }

		

		    AudioInputStream appendedFiles = 
			new AudioInputStream(
					     new SequenceInputStream(vec.elements()),
					     vec.firstElement().getFormat(), 
					     framelength);
		    
		    AudioSystem.write(appendedFiles, 
				      AudioFileFormat.Type.WAVE, 
				      OutFile);
		} catch (Exception e) {
		    e.printStackTrace();
		}


         return true;		
	}
}


