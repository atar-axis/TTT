// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
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

/*
 * Created on 01.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Dimension;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.awt.*;

import javax.swing.ImageIcon;

public class Constants {

    public static final String VersionMessageTTT = "TTT 001.001\n";
    public static final String VersionMessageTTTStream = "TTT 001.002\n";
    public static final String VersionMessageRFB = "RFB 003.003\n";

    // TODO: check values of defer variables - allow user preferences
    // Fine tuning options. Copied defaults from UltraVNC Viewer
    // NOTE: deferXXXUpdates has no effect using Swing
    public static int deferScreenUpdates = 20;
    public static int deferCursorUpdates = 10;
    public static int deferUpdateRequests = 50;

    // server to client messages
    public final static int FramebufferUpdate = 0;
    public static final int SetColourMapEntries = 1;
    public static final int Bell = 2;
    public static final int ServerCutText = 3;

    // client to server messages
    public final static int SetPixelFormat = 0;
    public static final int FixColourMapEntries = 1;
    public static final int SetEncodings = 2;
    public static final int FramebufferUpdateRequest = 3;
    public static final int KeyboardEvent = 4;
    public static final int PointerEvent = 5;
    public static final int ClientCutText = 6;

    // encodings and message tags only used by TTT protocol
    // since TTT 001.000
    public static final int EncodingTTTCursorPosition = 17;
    public static final int EncodingTTTXCursor = 18;
    public static final int EncodingTTTRichCursor = 19;
    public static final int AnnotationRectangle = 20;
    public static final int AnnotationLine = 21;
    public static final int AnnotationFreehand = 22;
    public static final int AnnotationHighlight = 23;
    public static final int AnnotationDelete = 24;
    public static final int AnnotationDeleteAll = 25;
    public static final int AnnotationImage = 26;    // MODMSG
    public static final int AnnotationText = 27;     // MODMSG
    public static final int EncodingRecording = 30;
    // encoding flags (can be combined with other encodings)
    public static final int EncodingFlagUpdate = 64;
    public static final int EncodingFlagTimestamp = 128;
    // never used in any official release (replaced by EncodingBlank aka EncodingWitheboard)
    public static final int EncodingBlankPageOn = 31;
    public static final int EncodingBlankPageOff = 32;
    // since TTT 001.001
    public static final int EncodingWhiteboard = 33;
    // encoding mask (all, but encoding flags)
    public static final int EncodingMask = 63;

    // encodings and message tags used by both, TTT and RFB protocol
    public final static int EncodingRaw = 0;
    public static final int EncodingCopyRect = 1;
    public static final int EncodingRRE = 2;
    public static final int EncodingCoRRE = 4;
    public static final int EncodingHextile = 5;
    public static final int EncodingZlib = 6;
    public static final int EncodingTight = 7;
    public final static int EncodingInterlacedRaw = 42; // TODO: think about number

    public static final int EncodingUnknown = -1;

    // sub encodings
    public final static int HextileRaw = (1 << 0);
    public final static int HextileBackgroundSpecified = (1 << 1);
    public final static int HextileForegroundSpecified = (1 << 2);
    public final static int HextileAnySubrects = (1 << 3);
    public final static int HextileSubrectsColoured = (1 << 4);

    // only used by RFB protocol
    public final static int EncodingCompressLevel0 = 0xFFFFFF00;
    public static final int EncodingQualityLevel0 = 0xFFFFFFE0;
    public static final int EncodingXCursor = 0xFFFFFF10;
    public static final int EncodingRichCursor = 0xFFFFFF11;
    public static final int EncodingPointerPos = 0xFFFFFF18;
    public static final int EncodingLastRect = 0xFFFFFF20;
    public static final int EncodingNewFBSize = 0xFFFFFF21;

    // initialisation
    public static final int ConnectionFailed = 0;
    public static final int ConnectionOK = 1;
    static final int ViewOnly = 0;
    static final int FullAccess = 1;
    static final int Multicast = 2;
    public static final int Unicast = 3;
    public static final int NoAuthentication = 1;
    public static final int Authentication = 2;
    public static final int AuthenticationOK = 0;
    public static final int AuthenticationFailed = 1;
    public static final int AuthenticationTooManny = 2;

    // client init
    static final int NonShared = 0;
    public static final int Shared = 1;

    public static int[] encodings = { /* EncodingCopyRect, */EncodingHextile, EncodingPointerPos, EncodingRichCursor,
            EncodingXCursor };
    public static int[] encodingsWithoutCursorEncodings = { /* EncodingCopyRect, */EncodingHextile };
    public static int defaultColorDepth = 16; // 24;
    static Dimension defaultVideoRecordingSize = new Dimension(176, 144);
    static Dimension defaultVideoTransmissionSize = new Dimension(176, 144);

    static public void writeSetPixelFormatMessage(OutputStream out, ProtocolPreferences prefs) throws IOException {
        // lock to avoid changes
        synchronized (prefs) {
            byte[] buffer;

            buffer = new byte[20];

            buffer[0] = (byte) Constants.SetPixelFormat;
            buffer[4] = (byte) prefs.bitsPerPixel;
            buffer[5] = (byte) prefs.depth;
            buffer[6] = (byte) (prefs.bigEndian ? 1 : 0);
            buffer[7] = (byte) (prefs.trueColour ? 1 : 0);
            buffer[8] = (byte) ((prefs.redMax >> 8) & 0xff);
            buffer[9] = (byte) (prefs.redMax & 0xff);
            buffer[10] = (byte) ((prefs.greenMax >> 8) & 0xff);
            buffer[11] = (byte) (prefs.greenMax & 0xff);
            buffer[12] = (byte) ((prefs.blueMax >> 8) & 0xff);
            buffer[13] = (byte) (prefs.blueMax & 0xff);
            buffer[14] = (byte) prefs.redShift;
            buffer[15] = (byte) prefs.greenShift;
            buffer[16] = (byte) prefs.blueShift;

            out.write(buffer);
        }
    }

    static public void writeSetEncodingsMessage(OutputStream out, ProtocolPreferences prefs) throws IOException {
        // lock to avoid changes
        synchronized (prefs) {
            int length = prefs.encodings.length;

            byte[] buffer = new byte[4 + 4 * length];

            buffer[0] = (byte) Constants.SetEncodings;
            buffer[2] = (byte) ((length >> 8) & 0xff);
            buffer[3] = (byte) (length & 0xff);

            for (int i = 0; i < length; i++) {
                buffer[4 + 4 * i] = (byte) ((prefs.encodings[i] >> 24) & 0xff);
                buffer[5 + 4 * i] = (byte) ((prefs.encodings[i] >> 16) & 0xff);
                buffer[6 + 4 * i] = (byte) ((prefs.encodings[i] >> 8) & 0xff);
                buffer[7 + 4 * i] = (byte) (prefs.encodings[i] & 0xff);
            }

            out.write(buffer);
        }
    }

    static public void writeFramebufferUpdateRequestMessage(OutputStream out, int x, int y, int w, int h,
            boolean incremental) throws IOException {
        byte[] buffer = new byte[10];

        buffer[0] = (byte) Constants.FramebufferUpdateRequest;
        buffer[1] = (byte) (incremental ? 1 : 0);
        buffer[2] = (byte) ((x >> 8) & 0xff);
        buffer[3] = (byte) (x & 0xff);
        buffer[4] = (byte) ((y >> 8) & 0xff);
        buffer[5] = (byte) (y & 0xff);
        buffer[6] = (byte) ((w >> 8) & 0xff);
        buffer[7] = (byte) (w & 0xff);
        buffer[8] = (byte) ((h >> 8) & 0xff);
        buffer[9] = (byte) (h & 0xff);

        out.write(buffer);
    }

    // /////////////////////////////////////////
    // icons
    // /////////////////////////////////////////

    // TODO: set as option
    public static int default_thumbnail_scale_factor = 6;

    // /////////////////////////////////////////
    // handling of file endings
    // /////////////////////////////////////////
    public static String[] desktopEndings = { ".ttt", ".TTT" }; // TODO: support/convert old recordings: , ".vnc", ".VNC" };
    public static String[] videoEndings = { ".mov", ".MOV", ".avi", ".AVI" };
    public static String[] audioEndings = { ".mp3", ".MP3", ".mp2", ".MP2", ".wav", ".WAV" };
    public static String[] searchbaseEndings = { ".xml", ".XML", ".txt", ".TXT" };

    public static final int DESKTOP_FILE = 1;
    public static final int AUDIO_FILE = 2;
    public static final int VIDEO_FILE = 3;
    public static final int SEARCHBASE_FILE = 4;

    public static File getExistingFile(String filename, int fileType) throws FileNotFoundException {
        if (filename == null)
            throw new FileNotFoundException("(no filename specified)");

        // select endings
        String[] endings;
        switch (fileType) {
        default:
        case DESKTOP_FILE:
            endings = desktopEndings;
            break;
        case AUDIO_FILE:
            endings = audioEndings;
            break;
        case VIDEO_FILE:
            endings = videoEndings;
            break;
        case SEARCHBASE_FILE:
            endings = searchbaseEndings;
            break;
        }

        // compare existing ending with possible endings
        for (int i = 0; i < endings.length; i++)
            if (filename.endsWith(endings[i])) {
                File file = new File(filename);
                if (file.exists())
                    return file;
            }

        // add possible endings
        for (int i = 0; i < endings.length; i++) {
            File file = new File(filename + endings[i]);
            if (file.exists())
                return file;
        }

        // remove existing ending and add possible endings
        int pos = filename.lastIndexOf('.');
        String filebase = null;
        if (pos > 0) {
            filebase = filename.substring(0, pos);
            // add endings
            for (int i = 0; i < endings.length; i++) {
                File file = new File(filebase + endings[i]);
                if (file.exists())
                    return file;
            }
        }

        // TODO: additional search in zip archive or something like that
        // URL url = this.getClass().getClassLoader().getResource(fileName + Constants.audioEndings[i]);

        String endingsString = filebase != null ? filebase : filename;
        for (int i = 0; i < endings.length; i++) {
            if (i > 0)
                endingsString += "/";
            endingsString += endings[i];
        }
        throw new FileNotFoundException(endingsString);
    }

    // ////////////////////////////////////////////////
    // handling of time string
    // ////////////////////////////////////////////////
    public static String getStringFromTime(int msec) {
        // generates nice time String
        return getStringFromTime(msec, true);
    }

    public static String getStringFromTime(int msec, boolean includeMilliseconds) {
        // generates nice time String
        boolean negative = msec < 0;
        if (negative)
            msec = -msec;

        int sec = msec / 1000 % 60;
        int min = msec / 60000;
        msec = msec % 1000;
        return (negative ? "-" : "") + ((min < 10) && !negative ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec
                + (includeMilliseconds ? "." + (msec < 100 ? "0" : "") + (msec < 10 ? "0" : "") + msec : "");
    }

    public static int getTimeFromString(String value) throws NumberFormatException {
        value = value.trim();
        int time = 0;
        if (value.endsWith("min"))
            time = 60000 * Integer.parseInt(value.substring(0, value.length() - 3));
        else if (value.endsWith("m"))
            time = 60000 * Integer.parseInt(value.substring(0, value.length() - 1));
        else if (value.endsWith("sec"))
            time = 1000 * Integer.parseInt(value.substring(0, value.length() - 3));
        else if (value.endsWith("s"))
            time = 1000 * Integer.parseInt(value.substring(0, value.length() - 1));
        else {
            int dumpf = value.indexOf(':');
            if (dumpf > 0) {
                time = 60000 * Integer.parseInt(value.substring(0, dumpf++));
                int dumpfer = value.indexOf('.');
                if (dumpfer > 0) {
                    time += 1000 * Integer.parseInt(value.substring(dumpf, dumpfer++));
                    time += Integer.parseInt(value.substring(dumpfer));
                } else
                    time += 1000 * Integer.parseInt(value.substring(dumpf));
            } else
                time = Integer.parseInt(value);
        }
        return time;
    }

    // /////////////////////////////////////////////////////////////////////
    // toString helper
    // /////////////////////////////////////////////////////////////////////

    public static String encodingsToString(int[] encodings) {
        // boolean useCursorEncodings = "true".equals(Preferences.get("useCursorEncoding", "true"));
        StringBuffer string = new StringBuffer("      WriteSetEncodings:");
        for (int i = 0; i < encodings.length; i++) {
            string.append("        " + (i + 1) + ". ");
            string.append(encodingToString(encodings[i]));
        }
        return string.toString();
    }

    public static String messageTypeToString(int type, boolean serverToClientMessage) {
        if (serverToClientMessage) {
            switch (type) {
            case FramebufferUpdate:
                return "FramebufferUpdate";
            case SetColourMapEntries:
                return "SetColourMapEntries";
            case Bell:
                return "Bell";
            case ServerCutText:
                return "ServerCutText";
            default:
                return "Unknown Server to Client Message";
            }
        } else {
            switch (type) {
            case SetPixelFormat:
                return "SetPixelFormat";
            case FixColourMapEntries:
                return "FixColourMapEntries";
            case SetEncodings:
                return "SetEncodings";
            case FramebufferUpdateRequest:
                return "FramebufferUpdateRequest";
            case KeyboardEvent:
                return "KeyEvent";
            case PointerEvent:
                return "PointerEvent";
            case ClientCutText:
                return "ClientCutText";
            default:
                return "Unknown Client to Server Message";
            }
        }
    }

    public static String encodingToString(int encoding) {
        // NOTE: dont mask special RFB encodings
        switch (encoding < 0 ? encoding : encoding & EncodingMask) {
        case EncodingRaw:
            return "EncodingRaw";
        case EncodingInterlacedRaw:
            return "EncodingInterlacedRaw";
        case EncodingCopyRect:
            return "EncodingCopyRect";
        case EncodingRRE:
            return "EncodingRRE";
        case EncodingCoRRE:
            return "EncodingCoRRE";
        case EncodingHextile:
            return "EncodingHextile";
        case EncodingZlib:
            return "EncodingZlib";
        case EncodingTight:
            return "EncodingTight";

        case Constants.EncodingRichCursor:
            return "RichCursor";
        case Constants.EncodingXCursor:
            return "XCursor";
        case Constants.EncodingPointerPos:
            return "PointerPosition";
        case Constants.EncodingLastRect:
            return "LastRect";
        case Constants.EncodingNewFBSize:
            return "NewFrameBufferSize";
        case Constants.EncodingCompressLevel0:
            return "CompressLevel";
        case Constants.EncodingQualityLevel0:
            return "QualityLevel";

        case EncodingTTTCursorPosition:
            return "EncodingTTTCursorPosition";
        case EncodingTTTXCursor:
            return "EncodingTTTXCursor";
        case EncodingTTTRichCursor:
            return "EncodingTTTRichCursor";
        case AnnotationRectangle:
            return "AnnotationRectangle";
        case AnnotationLine:
            return "AnnotationLine";
        case AnnotationFreehand:
            return "AnnotationFreehand";
        case AnnotationText:                // MODMSG
        	return "AnnotationText";
        case AnnotationImage:               // MODMSG
        	return "AnnotationImage";
        case AnnotationHighlight:
            return "AnnotationHighlight";
        case AnnotationDelete:
            return "AnnotationDelete";
        case AnnotationDeleteAll:
            return "AnnotationDeleteAll";
        case EncodingRecording:
            return "EncodingRecording";

        case EncodingBlankPageOn:
            return "EncodingBlankPageOn";
        case EncodingBlankPageOff:
            return "EncodingBlankPageOff";
        case EncodingWhiteboard:
            return "EncodingBlankPage";

        default:
            return "unknown Encoding [" + encoding + "]";
        }
    }

    static boolean umlaut_reduction = !true;

    // TODO: rework
    public static String reduce(String text) {
        // removes special characters
        // shifts all characters to lower case
        //
        StringBuffer stringBuffer = new StringBuffer();
        boolean blank = false;
        for (int pos = 0; pos < text.length(); pos++) {
            int character = text.charAt(pos);
            char ch = Character.toLowerCase((char) character);
            if (Character.isLetterOrDigit(ch)) {
                if (umlaut_reduction) {
                	//removed to avoid compilation error
                    // NOTE: problematical for underlining
//                    switch (ch) {
//                    case '�':
//                        stringBuffer.append("ae");
//                        break;
//                    case '�':
//                        stringBuffer.append("oe");
//                        break;
//                    case '�':
//                        stringBuffer.append("ue");
//                        break;
//                    case '�':
//                        stringBuffer.append("ss");
//                        break;
//                    default:
//                        stringBuffer.append(ch);
//                        break;
//                    }
                } else
                    stringBuffer.append(ch);
                blank = false; // character added
            } else if (!blank) {
                stringBuffer.append(' ');
                blank = true; // only append one blank
            }
        }
        return new String(stringBuffer);
    }

    static public final int EXTENSION_INDEX_TABLE = 1;
    static public final int EXTENSION_SEARCHBASE_TABLE_WITH_COORDINATES = 2;
    
    // /////////////////////////////////////////
    // file system operations
    // /////////////////////////////////////////
    
    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
    }
    
    public static  URL getResourceUrl(String fileName){
    	
    	try {
			return new URL(TTT.class.getResource("Constants.class").toString().replaceFirst("ttt/Constants.class", "resources/"));
		} catch (MalformedURLException e) {
			System .out.println("File not found: " + fileName);
			return null;
		}
    }
    
    
  
    /**
     * Creates and returns an Image Icon
     * 
     * @param iconName just the File name of the Image
     * @return The corresponding ImageIcon
     */
    public static ImageIcon getIcon(String iconName) {   
		String iconPath = TTT.class.getResource("Constants.class").toString()
				.replaceFirst("ttt/Constants.class", "resources/");
		
		try {						
			ImageIcon icon = new ImageIcon(new URL((iconPath + iconName)));
		
		
		 
			if(icon.getImage() == null){
				throw new IOException();
			}			
			return icon;
		}  catch (IOException e) {
			System.out.println("Couldn't read Icon: " + iconPath + iconName);
			//draw a Replacement Icon
			return createBrokenIcon();
		} 
	}
    
    /**
     * Generates a 16x16 icon 
     * @return
     */
	public static ImageIcon createBrokenIcon() {
		int width = 24;
		int height = 24;

		int[] pixels = new int[height * width];
		int index = 0;

		int alpha = 255;
		int red = 255;
		int green = 0;
		int blue = 0;
		// thickness of the stroke
		int thick = 2;
		int leftbound = 6;
		int size = 24;
		int rectsize = size - leftbound;
		for (int y = 0; y < height; y++) {

			for (int x = 0; x < width; x++) {

				boolean rectbound = y > leftbound && y < rectsize
						&& x > leftbound && x < rectsize;
				// make stroke from top left to bottom right
				if (rectbound && (0 <= (x - y) && (x - y) < thick)) {
					// '<<' is the java bitshifting operator
					pixels[index++] = (alpha << 24) | (red << 16)
							| (green << 8) | blue;
					// make stroke from top right to bottom left
				} else if (rectbound
						&& ((x + y) > (size - thick) && ((x + y) <= size))) {
					pixels[index++] = (alpha << 24) | (red << 16)
							| (green << 8) | blue;
					// make top and bottom line
				} else if (rectbound
						&& (x < leftbound + thick | x > rectsize - thick)) {
					pixels[index++] = (alpha << 24) | (red << 16)
							| (green << 8) | blue;
					// make top and bottem
				} else if (rectbound
						&& (y < leftbound + thick | y > rectsize - thick)) {
					pixels[index++] = (alpha << 24) | (red << 16)
							| (green << 8) | blue;
				}

				else {
					pixels[index++] = (0 << 24) | (0 << 16) | (0 << 8) | blue;
				}
			}
		}

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return new ImageIcon(toolkit.createImage(new MemoryImageSource(width,
				height, pixels, 0, width)));

	}
}
