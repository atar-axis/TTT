package ttt.converter;

import ttt.Constants;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.record.Recording;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastianstein on 05.12.16.
 * <p>
 * Converter to convert a old ttt file to the new streaming structure
 */
public class TTTConverter {
    private static int fullFramesPerChunk = 1;

    public static void main(String[] arguments) throws IOException {
        if (arguments.length < 0) {
            printHelp();
            return;
        }
        switch (arguments[0]) {
            case "-h":
                printHelp();
                break;
            default:
                if (arguments.length < 2) {
                    System.out.println("Wrong number of inputs");
                    break;
                }
                String from = arguments[0];
                String to = arguments[1];
                Recording recording = new Recording(new File(from), false);
                //ArrayList<Message> fullFrames = getFullFrames(recording);
                //System.out.println(fullFrames.toString());
                convertRecording(recording, to);
        }
    }

    /**
     * get a List of FullFrames from a Recording
     *
     * @param recording the Recording which FullFrames have to be extracted
     * @return a ArrayList of FullFrames
     */
    private static ArrayList<Message> getFullFrames(Recording recording) {
        ArrayList<Message> fullFrames = new ArrayList<>();
        for (Message message : recording.getMessages().getMessages()) {
            if (message.getEncoding() != Constants.EncodingHextile) {
                continue;
            }
            Rectangle bounds = ((HextileMessage) message).getBounds();
            if (bounds.width == recording.getProtocolPreferences().framebufferWidth && bounds.height == recording.getProtocolPreferences().framebufferHeight) {
                fullFrames.add(message);
            }
        }
        return fullFrames;
    }

    private static void convertRecording(Recording recording, String to) throws IOException {
        ArrayList<FullFrameContainer> containerList = new ArrayList<>();
        FullFrameContainer actualContainer = new FullFrameContainer();
        containerList.add(actualContainer);
        for (Message message : recording.getMessages().getMessages()) {
            if (message.getEncoding() != Constants.EncodingHextile) {
                actualContainer.addMessage(message);
                continue;
            }
            Rectangle bounds = ((HextileMessage) message).getBounds();
            if (bounds.width == recording.getProtocolPreferences().framebufferWidth && bounds.height == recording.getProtocolPreferences().framebufferHeight) {
                if (actualContainer.getFullFrameCount() >= fullFramesPerChunk) {
                    actualContainer = new FullFrameContainer();
                    containerList.add(actualContainer);
                }
                actualContainer.addFullFrame(message);
            } else {
                actualContainer.addMessage(message);
            }
        }

        int offset = 0;
        for (FullFrameContainer container : containerList) {
            container.setOffset(offset);
            offset += container.writeMessages();
            System.out.println("deflated container, offset: " + offset);
        }

        FileOutputStream fileOut = new FileOutputStream(to);
        DataOutputStream out = new DataOutputStream(fileOut);
        // progressMonitor.setProgress(1);

        // write header
        //
        // write version message
        out.write(Constants.VersionMessageTTT.getBytes());

        // write compressed messages to file
        out = new DataOutputStream(new DeflaterOutputStream(fileOut));
        recording.writeInit(out);

        recording.writeExtensions(out);

        //TODO move start time
        // NOTE: badly designed - should be placed before extensions as part
        // of init
        out.writeLong(recording.getProtocolPreferences().starttime);

        // write fullFrameList
        for (FullFrameContainer container : containerList) {
            container.writeFullFrameHeader(out);
        }
        out = new DataOutputStream(fileOut);
        // write body
        for (FullFrameContainer container : containerList) {
            container.writeData(out);
        }
        System.out.println("written file");

        out.close();
    }

    private static void printHelp() {
        System.out.println("Help for TTTConverter:");
        System.out.println("'-h' for help");
        System.out.println("'from' 'to' to convert the file 'from' and save it in the file 'to'");
    }
}
