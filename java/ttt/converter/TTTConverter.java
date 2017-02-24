package ttt.converter;

import ttt.Constants;
import ttt.TTT;
import ttt.record.Recording;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastianstein on 05.12.16.
 * <p>
 * Converter to convert a old ttt file to the new streaming structure
 */
public class TTTConverter {
    // 0: error
    // 1: debug
    // 2: verbose
    private static int logLevel = 0;
    private static DateFormat dateFormat = new SimpleDateFormat(/*"dd.MM.yy " +*/ "HH:mm:ss");

    public static void main(String[] arguments) throws IOException {
        if(TTTConverter.logLevel <= 1){
            TTT.verbose = false;
        }
        if(TTTConverter.logLevel <= 0){
            TTT.debug = false;
        }

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
        TTTConverter.log("Converting File " + from + " to " + to, 0);
        Recording recording = new Recording(new File(from), false);
        ArrayList<FullFrameContainer> containerList = FullFrameContainer.createContainer(recording);
        compressData(containerList);
        writeFile(compressHeader(recording, containerList), containerList, to);
        TTTConverter.log("Finished Converting. Result: " + to, 0);
    }

    private static void compressData(ArrayList<FullFrameContainer> containerList) throws IOException {
        TTTConverter.log("Compress Messages", 1);
        int offset = 0;
        for (FullFrameContainer container : containerList) {
            container.setOffset(offset);
            offset += container.writeMessages();
        }
    }

    private static byte[] compressHeader(Recording recording, ArrayList<FullFrameContainer> containerList) throws IOException {
        TTTConverter.log("Compress Header", 1);
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
        TTTConverter.log("Write File", 1);
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

        out.close();
    }

    public static void log(String message, int logLevel){
        if(logLevel <= TTTConverter.logLevel){
            System.out.println(dateFormat.format(new Date()) + ": " + message);
        }
    }
}