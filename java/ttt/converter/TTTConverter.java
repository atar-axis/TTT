package ttt.converter;

import ttt.Constants;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.record.Recording;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastianstein on 05.12.16.
 * <p>
 * Converter to convert a old ttt file to the new streaming structure
 */
public class TTTConverter {
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
        compressData(containerList);
        writeFile(compressHeader(recording, containerList), containerList, to);
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
                actualContainer = new FullFrameContainer();
                containerList.add(actualContainer);
            }
            actualContainer.addMessage(message);
        }
        return containerList;
    }

    private static void compressData(ArrayList<FullFrameContainer> containerList) throws IOException {
        int offset = 0;
        for (FullFrameContainer container : containerList) {
            container.setOffset(offset);
            System.out.println("deflated container, offset: " + offset);
            offset += container.writeMessages();
        }
    }

    private static byte[] compressHeader(Recording recording, ArrayList<FullFrameContainer> containerList) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(data));

        // write init
        recording.writeInit(out);

        // write start time
        out.writeLong(recording.getProtocolPreferences().starttime);

        // write extensions
        recording.writeExtensions(out);

        // write list of full frames with (compressed) offsets
        out.writeInt(containerList.size());
        for (FullFrameContainer container : containerList) {
            container.writeFullFrameHeader(out);
        }

        out.close();
        data.close();
        return data.toByteArray();
    }

    private static void writeFile(byte[] header, ArrayList<FullFrameContainer> containerList, String to) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(to);
        DataOutputStream out = new DataOutputStream(fileOut);

        // write version message
        out.write(Constants.VersionMessageTTTStream.getBytes());
        // write header size
        out.writeInt(header.length + 16);

        // write compressed header to file
        out.write(header);

        // write compressed messages to file
        for (FullFrameContainer container : containerList) {
            container.writeData(out);
        }
        System.out.println("written file " + to);

        out.close();
    }
}