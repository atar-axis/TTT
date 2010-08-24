package ttt.editor2.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Ludwig This class is meant to concat wav files.
 */
public class WavConcat {
	

	private File[] InFiles;
	private File OutFile;
	
	byte[] header = new byte[44]; // stores the header for the wav. (wav header size is are 44 byte)
	byte[] OutBuffer; //Stores the complete outfile
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
		OutBuffer = new byte[calculateOutFileSize()];
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
	

	/**
	 * Read the AudioIn concat them and write into the AudioOut
	 */
	public boolean doIt() {
		try {
			OutFile.createNewFile();
		} catch (IOException e) {
			System.err.println("Couldn't create the OutFile.");
			e.printStackTrace();
			return false;
		}

		saveHeader();
		readFilesIntoOutBuffer();
		ManipulateHeader();

		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(OutFile);

			fileOut.write(OutBuffer);
			fileOut.close();
		} catch (FileNotFoundException e) {
			// Shouldn't happen
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("Couldn't write file");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Writes the new new header for the wav file
	 */
	private void ManipulateHeader() {
		//for details information about wav headers see: https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
		//write the whole header Into OutBuffer
		for (int i = 0; i < header.length; i++) {
			OutBuffer[i] = header[i];
		}

		//Change the size information
		byte[] t = new byte[4];
		t = convertIntToByteArray((OutBuffer.length - 44));
		int SubChunk = convertByteArrayToInt(t);
		OutBuffer[40] = t[3];
		OutBuffer[41] = t[2];
		OutBuffer[42] = t[1];
		OutBuffer[43] = t[0];

		t = convertIntToByteArray(SubChunk + 36);
		OutBuffer[4] = t[3];
		OutBuffer[5] = t[2];
		OutBuffer[6] = t[1];
		OutBuffer[7] = t[0];
	}

	/**
	 * save the header information
	 */
	private void saveHeader() {
		try {
		fileIn = new FileInputStream(InFiles[0]);
			fileIn.read(header); //save the header
		} catch (FileNotFoundException e) {
			System.err.println("Error reading file: " + InFiles[0].toString());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error reading file: " + InFiles[0].toString());
			e.printStackTrace();
		}
	}

	/**
	 * read each file and write them into the OutBuffer
	 */
	private void readFilesIntoOutBuffer() {
		for(File i: InFiles) {
			try {
				InBuffer = new byte[(int) (i.length() - Offset.length)];
				fileIn = new FileInputStream(i);

				fileIn.read(Offset); //skip file header
				saveDuration();

				fileIn.read(InBuffer);

				for (int j = 0; j < InBuffer.length; j++) {
					OutBuffer[j + Position] = InBuffer[j];
				}

				Position += InBuffer.length;

		} catch (FileNotFoundException e) {
				System.err.println("Error reading file: " + i.toString());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Error reading file: " + i.toString());
				e.printStackTrace();
			}
		}
	}


	int count = 0;

	/**
	 * Calculate the duration from the header and save it.
	 */
	private void saveDuration() {
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
	}

	private int calculateOutFileSize(){
		int size = 0;
		for(File i: InFiles) {
			size += i.length();
		}
		return size;
	} 
	
	public byte[] convertIntToByteArray(int val) {
   		
	  ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(val);

		return buffer.array();
	}

	public int convertByteArrayToInt(byte[] bytebuf) {
 		
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put(bytebuf);
		 			
		return buffer.getInt(0);
	}
}


