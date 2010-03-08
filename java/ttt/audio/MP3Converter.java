package ttt.audio;

/*
 * MODIFIED VERSION OF
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

// Extended by Peter Ziewer, 2001
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.StopAtTimeEvent;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.swing.JOptionPane;

import ttt.Constants;
import ttt.TTT;
import ttt.record.Recording;

/**
 * A sample program to transcode an input source to an output location with different data formats.
 */
public class MP3Converter implements ControllerListener, DataSinkListener {

    private Processor p;
    private boolean loop;

    static boolean renameTo(String source, String dest) {
        boolean renamed = false;

        // File (or directory) with old name
        File file = new File(source);

        // File (or directory) with new name
        File file2 = new File(dest);

        System.out.println("Rename: " + file + "\n    to: " + file2);

        // NOTE: previously used mp3 file handlers may prevent renaming under Windows
        // try several times to renaming file
        int count = 0;
        while (!renamed && count++ < 10) {
            // Rename file (or directory)
            renamed = file.renameTo(file2);
            if (!renamed) {
                try {
                    // force Garbage Collection to free file handlers
                    Runtime.getRuntime().gc();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!renamed)
            try {
                System.out.println(" failed - try to copy and delete instead:");
                // NOTE: sometimes File.renameTo fails under Windows but copy and delete works
                // try to copy and delete instead
                renamed = renameByCopy(source, dest);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        if (renamed)
            System.out.println(" done");
        else
            System.out.println(" failed");
        return renamed;
    }

    static boolean renameByCopy(String source, String dest) throws Exception {
        // File (or directory) with old name
        File file = new File(source);

        // File (or directory) with new name
        File file2 = new File(dest);

        System.out.print("Rename:\n    " + file + "\nto\n    " + file2);
        System.out.print("    copy:\n    " + file + "\nto\n    " + file2);
        copyFile(file, file2);
        System.out.print("    delete:\n    " + file);
        return file.delete();
    }

    static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    public static String getAudioFormat(Recording recording) {
        try {
            Format format = MP3Converter.getFormat(Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(),
                    Constants.AUDIO_FILE).getCanonicalPath());
            return format.getEncoding();
        } catch (IOException e) {}
        return "failed";
    }

    public static boolean checkAndConvertAudioFile(Recording recording) {
        return checkAndConvertAudioFile(recording, AudioFormat.MPEGLAYER3)
                || checkAndConvertAudioFile(recording, AudioFormat.MPEG);
    }

    public static boolean checkAndConvertAudioFile(Recording recording, String audioFormat) {
        try {
            Format format = MP3Converter.getFormat(Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(),
                    Constants.AUDIO_FILE).getCanonicalPath());

            Format format2 = new AudioFormat(audioFormat);
            if (!format.matches(format2)) {
                System.out.println("    converting audio to " + format.getEncoding() + " audio ... ");
                return MP3Converter.convert(Constants.getExistingFile(recording.fileDesktop.getCanonicalPath(),
                        Constants.AUDIO_FILE));
            }
        } catch (IOException e) {
            System.out.println("    failed");
            e.printStackTrace();
            TTT.showMessage("MP3 audio conversation failed.\nError: " + e, "TTT2Flash", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    static public boolean convert(File file) throws IOException {
        // Transcode with the specified parameters.
        MP3Converter transcode = new MP3Converter();

        String input_file = file.getCanonicalPath();
        String output_file;
        boolean rename = !(input_file.endsWith(".mp2") || input_file.endsWith(".MP2") || input_file.endsWith(".wav") || input_file
                .endsWith(".WAV"));
        if (rename)
            output_file = input_file + ".mp3";
        else
            output_file = input_file.substring(0, input_file.length() - 4) + ".mp3";

        System.out.println("IN:  " + input_file);
        System.out.println("OUT: " + output_file);

        if (transcode.convertInternal(input_file, output_file)) {
            // done if mp2 audio was stored in file with ending .mp2
            if (!rename)
                return true;
            // rename if mp2 audio was stored in file with ending .mp3
            else {
                // rename files
                System.out.println("    renaming mp3 files");
                String filename = input_file;

                File backup = new File(filename + ".orig");
                int count = 0;
                while (backup.exists()) {
                    backup = new File(filename + ".bak" + (count == 0 ? "" : "." + count));
                    count++;
                }

                System.out.println("Filename:\t" + filename);
                System.out.println("Backup:\t" + backup);

                // rename old to backup
                if (renameTo(filename, backup.getCanonicalPath())) {
                    // rename new to valid ending
                    if (renameTo(filename + ".mp3", filename)) {
                        return true;
                    } else {
                        System.out.println("RENAMING FAILED!!!");
                        throw new IOException("file renaming failed - fail probably in use");
                    }
                } else {
                    System.out.println("RENAMING FAILED!!!");
                    throw new IOException("file renaming failed - fail probably in use");
                }
            }
        } else {
            System.out.println("Transcoding failed");
        }
        return false;
    }

    static public Format getFormat(String mp3Input) {
        // Transcode with the specified parameters.
        MP3Converter transcode = new MP3Converter();
        return transcode.getFormatInternal(mp3Input);
    }

    Format getFormatInternal(String mp3Input) {
        File file = new File(mp3Input);
        MediaLocator inML = null;
        try {
            inML = new MediaLocator("file:" + file.getCanonicalPath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            p = Manager.createProcessor(inML);
        } catch (Exception e) {
            System.out.println("Yikes!  Cannot create a processor from the given url: " + e);
            if (p != null)
                p.close();
            return null;
        }

        p.addControllerListener(this);

        // Put the Processor into configured state.
        p.configure();
        if (!waitForState(p, Processor.Configured)) {
            System.out.println("Failed to configure the processor.");
            if (p != null)
                p.close();
            return null;
        }

        Format format = p.getTrackControls()[0].getFormat();
        if (p != null) {
            p.removeControllerListener(this);
            p.close();
        }
        return format;
    }

    /**
     * Given a source media locator, destination media locator and an array of formats, this method will transcode the
     * source to the dest into the specified formats.
     */
    boolean convertInternal(String input_file, String output_file) {
        MediaLocator inML = new MediaLocator("file:" + input_file);
        MediaLocator outML = new MediaLocator("file:" + output_file);

        try {
            System.out.println("- create processor for: " + inML);
            p = Manager.createProcessor(inML);
        } catch (Exception e) {
            System.out.println("Yikes!  Cannot create a processor from the given url: " + e);
            return false;
        }

        p.addControllerListener(this);

        // Put the Processor into configured state.
        p.configure();
        if (!waitForState(p, Processor.Configured)) {
            System.out.println("Failed to configure the processor.");
            return false;
        }

        System.out.println("- current format: " + p.getTrackControls()[0].getFormat());

        // Set the output content descriptor based on the media locator.
        setContentDescriptor(p, outML);

        // Program the tracks to the given output formats.
        if (!setTrackFormats(p))
            return false;

        // We are done with programming the processor. Let's just realize the it.
        p.realize();
        if (!waitForState(p, Processor.Realized)) {
            System.out.println("Failed to realize the processor.");
            return false;
        }

        // Now, we'll need to create a DataSink.
        DataSink dsink;
        if ((dsink = createDataSink(p, outML)) == null) {
            System.out.println("Failed to create a DataSink for the given output MediaLocator: " + outML);
            return false;
        }

        dsink.addDataSinkListener(this);
        fileDone = false;

        System.out.println("start transcoding...");

        // OK, we can now start the actual transcoding.
        try {
            p.start();
            dsink.start();
        } catch (IOException e) {
            System.out.println("IO error during transcoding");
            return false;
        }

        // Wait for EndOfStream event.
        waitForFileDone();

        // Cleanup.
        try {
            dsink.close();
        } catch (Exception e) {}
        p.removeControllerListener(this);

        System.out.println("...done transcoding.");

        return true;
    }

    /**
     * Set the content descriptor based on the given output MediaLocator.
     */
    void setContentDescriptor(Processor p, MediaLocator outML) {

        ContentDescriptor cd;

        // If the output file maps to a content type,
        // we'll try to set it on the processor.

        if ((cd = fileExtToCD(outML.getRemainder())) != null) {

            System.out.println("- set content descriptor to: " + cd);

            if (cd.toString().endsWith("RTP")) {
                loop = true;
                System.out.println("- repeat playing in a loop");
            }

            if ((p.setContentDescriptor(cd)) == null) {

                // The processor does not support the output content
                // type. But we can set the content type to RAW and
                // see if any DataSink supports it.

                p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
            }
        }
    }

    /**
     * Set the target transcode format on the processor.
     */
    boolean setTrackFormats(Processor p) {
        TrackControl tcs[];

        if ((tcs = p.getTrackControls()) == null) {
            // The processor does not support any track control.
            System.out.println("The Processor cannot transcode the tracks to the given formats");
            return false;
        }

        AudioFormat format = new AudioFormat(AudioFormat.MPEGLAYER3);
        System.out.println("- set track format to: " + format);

        if (!setEachTrackFormat(p, tcs, format)) {
            System.out.println("Cannot transcode any track to: " + format);
            return false;
        }

        return true;
    }

    /**
     * We'll loop through the tracks and try to find a track that can be converted to the given format.
     */
    boolean setEachTrackFormat(Processor p, TrackControl tcs[], Format fmt) {

        Format supported[];
        Format f;

        for (int i = 0; i < tcs.length; i++) {

            supported = tcs[i].getSupportedFormats();

            if (supported == null)
                continue;

            for (int j = 0; j < supported.length; j++) {
                System.out.println(supported[j]);

                if (fmt.matches(supported[j]) && (f = fmt.intersects(supported[j])) != null
                        && tcs[i].setFormat(f) != null) {

                    // Success.
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Create the DataSink.
     */
    DataSink createDataSink(Processor p, MediaLocator outML) {

        DataSource ds;

        if ((ds = p.getDataOutput()) == null) {
            System.out.println("Something is really wrong: the processor does not have an output DataSource");
            return null;
        }

        DataSink dsink;

        try {
            System.out.println("- create DataSink for: " + outML);
            dsink = Manager.createDataSink(ds, outML);
            dsink.open();
        } catch (Exception e) {
            System.out.println("Cannot create the DataSink: " + e);
            return null;
        }
        return dsink;
    }

    Object waitSync = new Object();
    boolean stateTransitionOK = true;

    /**
     * Block until the processor has transitioned to the given state. Return false if the transition failed.
     */
    boolean waitForState(Processor p, int state) {
        synchronized (waitSync) {
            try {
                while (p.getState() < state && stateTransitionOK)
                    waitSync.wait();
            } catch (Exception e) {}
        }
        return stateTransitionOK;
    }

    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent
                || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            if (loop && evt.getSourceController() instanceof Processor) {
                ((Processor) evt.getSourceController()).setMediaTime(new Time(0));
                ((Processor) evt.getSourceController()).start();
                System.out.print(" ");
            } else
                evt.getSourceController().close();
        } else if (evt instanceof MediaTimeSetEvent) {
            // System.out.println("- mediaTime set: " +
            // ((MediaTimeSetEvent)evt).getMediaTime().getSeconds());
        } else if (evt instanceof StopAtTimeEvent) {
            System.out.println("- stop at time: " + ((StopAtTimeEvent) evt).getMediaTime().getSeconds());
            evt.getSourceController().close();
        }
    }

    Object waitFileSync = new Object();
    boolean fileDone = false;
    boolean fileSuccess = true;

    /**
     * Block until file writing is done.
     */
    boolean waitForFileDone() {
        System.out.print("  ");
        synchronized (waitFileSync) {
            try {
                while (!fileDone) {
                    waitFileSync.wait(1000);
                    System.out.print(".");
                }
            } catch (Exception e) {}
        }
        System.out.println("");
        return fileSuccess;
    }

    /**
     * Event handler for the file writer.
     */
    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
            }
        }
    }

    /**
     * Convert a file name to a content type. The extension is parsed to determine the content type.
     */
    ContentDescriptor fileExtToCD(String name) {

        String ext;
        int p;

        // Extract the file extension.
        if ((p = name.lastIndexOf('.')) < 0)
            return null;

        ext = (name.substring(p + 1)).toLowerCase();

        String type;

        // Use the MimeManager to get the mime type from the file extension.
        if (ext.equals("mp3")) {
            type = FileTypeDescriptor.MPEG_AUDIO;
        } else {
            if ((type = com.sun.media.MimeManager.getMimeType(ext)) == null)
                return null;
            type = ContentDescriptor.mimeTypeToPackageName(type);
        }

        return new FileTypeDescriptor(type);
    }

    /**
     * Main program
     */
    public static void main(String[] args) {
        // Transcode with the specified parameters.
        MP3Converter transcode = new MP3Converter();

        String input_file = args[0];
        String output_file = (args.length == 1 ? args[0] + ".mp3" : args[1]);

        if (!transcode.convertInternal(input_file, output_file)) {
            System.out.println("Transcoding failed");
        }

        System.exit(0);
    }
}
