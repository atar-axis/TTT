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
                convertRecording(from, to);
        }
    }

    private static void printHelp() {
        System.out.println("Help for TTTConverter:");
        System.out.println("'TTTConverter -h' for help");
        System.out.println("'TTTConverter from.ttt to.ttt' to convert the file 'from.ttt' and save it in the file 'to.ttt'");
    }

    private static void convertRecording(String from, String to) throws IOException {
        Recording recording = new Recording(new File(from), false);
        ArrayList<FullFrameContainer> containerList = createContainer(recording);
        deflateData(containerList);
        writeFile(recording, containerList, to);
    }

    private static ArrayList<FullFrameContainer> createContainer(Recording recording) {
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
        return containerList;
    }

    private static void deflateData(ArrayList<FullFrameContainer> containerList) throws IOException {
        int offset = 0;
        for (FullFrameContainer container : containerList) {
            container.setOffset(offset);
            offset += container.writeMessages();
            System.out.println("deflated container, offset: " + offset);
        }
    }

    private static void writeFile(Recording recording, ArrayList<FullFrameContainer> containerList, String to) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(to);
        DataOutputStream out = new DataOutputStream(fileOut);

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

        // write body
        out = new DataOutputStream(fileOut);
        for (FullFrameContainer container : containerList) {
            container.writeData(out);
        }
        System.out.println("written file");

        out.close();
    }
}