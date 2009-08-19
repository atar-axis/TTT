// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.


package ttt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Allows save and platform independent execution of commands. <br>
 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
 * See this <a href="http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=2">link</a> for more information about using exec().<br>  
 * Created on 16. August 2009, 17:25
 * @author Christof Angermueller 
 * 
 */
public class Exec {
	
	private OutputStream outputStream;
	private OutputStream errorStream;
	private Process process = null;
	private StreamCleaner streamCleaner;
	
	
	public Exec() {
		outputStream = null;
		errorStream = null;
	}
	
	
	/**
	 * @param outputStream Default output stream of the executed process.
	 * @param errorStream  Error stream of the executed process.
	 */
	public Exec(OutputStream outputStream, OutputStream errorStream) {
		this.outputStream = outputStream;
		this.errorStream = errorStream;
	}
	
	
	public static void main(String[] cmd) throws Exception {
		new Exec(System.out,System.out).exec(cmd);
	}
	
	
	/**
	 * Gets the command of an application if it is installed.<br>
	 * The application is supposed to be in the default directory for executable programs of the operating system or in the directory of this class.
	 * @return Command of the application or null if the application is not found.
	 */
	public static String getCommand(String application) {
	
		try {
			Runtime.getRuntime().exec(application);	//Check first, if the application is located in a default directory for executable programs of the operating system
			return application;
		} catch (IOException e) {
			try {
				URL url = Constants.class.getResource("");
				System.out.println(url);
				File appplicationPath;
				if (url.getProtocol().compareTo("jar") == 0) {
					//If the class is located in a jar file, the application is supposed to be in the directory of this jar file
					url = new URL(url.getPath());
					appplicationPath = new File( URLDecoder.decode(url.getPath()));
					while (!appplicationPath.getName().endsWith("!")) {
						appplicationPath = appplicationPath.getParentFile();
					}
					appplicationPath = appplicationPath.getParentFile();
				} else {
					//Otherwise the application is supposed to be in the directory of this class
					appplicationPath = new File(URLDecoder.decode(url.getPath()));
				}
				String path = appplicationPath.getPath() + File.separator + application;
				Runtime.getRuntime().exec(new String[] {path});
				return path;
			} catch (Exception ex) {
				return null;
			}
		}
	}
	
	
	/**
	 * Creates ByteArrayOutputStream for output stream and error stream of the executed process.
	 */
	public ByteArrayOutputStream createListenerStream() {
		outputStream = new ByteArrayOutputStream();
		errorStream = outputStream;
		return (ByteArrayOutputStream)outputStream;
	}
	
	
	/**
	 * 
	 * @return ByteArrayOutputStream created by createListenerStream.
	 */
	public ByteArrayOutputStream getListenerStream() {
		return (ByteArrayOutputStream)outputStream;
	}
	
	
	/**
	 * 
	 * @return Current exec process or null if no process is running.
	 */
	public Process getProcess() {
		return process;
	}
	
	
	/**
	 * Aborts the current exec process
	 */
	public void abort() {
		if (streamCleaner.aborted == false) {
			streamCleaner.abort();
			process.destroy();			
		}
	}
	
	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String cmd) throws Exception {
		process = Runtime.getRuntime().exec(cmd);
		return waitFor();
	}

	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String cmd, String[] envp) throws Exception {
		process = Runtime.getRuntime().exec(cmd, envp);
		return waitFor();
	}
	
	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String cmd, String[] envp, File dir) throws Exception {
		process = Runtime.getRuntime().exec(cmd, envp, dir);
		return waitFor();
	}
	
	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String[] cmd) throws Exception {
		process = Runtime.getRuntime().exec(cmd);
		return waitFor();
	}
	
	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String[] cmd, String[] envp) throws Exception {
		process = Runtime.getRuntime().exec(cmd, envp);
		return waitFor();
	}
	
	
	/**
	 * Extends exec() of the class Runtime by cleaning the output- and error-stream of the executed process in order to avoid that the process goes idle.<br>
	 */
	public int exec(String[] cmd, String[] envp, File dir) throws Exception {
		process = Runtime.getRuntime().exec(cmd, envp, dir);
		return waitFor();
	}
	
	
	/**
	 * Waits until termination of the exec() process and cleans the output-stream.
	 * @return process.exitValue()
	 * @throws Exception
	 */
	private int waitFor() throws Exception {
		
		streamCleaner = new StreamCleaner();
		streamCleaner.run();
		int i = process.waitFor();
		streamCleaner.abort();
		process = null;
		return i;
	}
		
	
	/**
	 * Thread for cleaning the output- and error-stream of the exec() process.
	 * @author client-01
	 *
	 */
	private class StreamCleaner extends Thread {
		
		private boolean aborted;
			
		
		/**
		 * Starts cleaning the streams.<br>
		 * Both process.getInputStream() and process.getErrorStream() can contain the default output of the exec() process and thus must be cleaned.<br> 
		 */
		public void run() {

			aborted = false;
			try {
				while (aborted == false) {	//loop until stream of the exec() process is available (can take a moment after evoking exec()) or aborted.
					if (process.getInputStream().available() > 0) {	//Only clean stream if .available() > 0 in order to avoid locking.
						cleanStream(process.getInputStream(),outputStream);
						break;
					}
					if (process.getErrorStream().available() > 0) {	//Only clean stream if .available() > 0 in order to avoid locking.
						cleanStream(process.getErrorStream(),errorStream);
						break;
					}
					sleep(10);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * Cleans the streams.
		 * @param inputStream
		 * @param outputStream
		 * @throws IOException
		 */
		private void cleanStream(InputStream inputStream, OutputStream outputStream) throws IOException {
			
			int i;
			if (outputStream == null) {
				while (aborted == false && (i = inputStream.read()) != -1) {
				}
			} else {
				while (aborted == false && (i = inputStream.read()) != -1) {
					outputStream.write(i);
				}
			}
		}
		
		
		/**
		 * Aborts the thread.
		 */
		public void abort() {
			aborted = true;
		}
	}
}