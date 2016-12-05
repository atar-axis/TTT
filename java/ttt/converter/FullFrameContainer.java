package ttt.converter;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import ttt.messages.Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastianstein on 05.12.16.
 */
class FullFrameContainer {
    private ArrayList<Message> fullFrames = new ArrayList<>();
    private ArrayList<Message> messages = new ArrayList<>();
    private ByteArrayOutputStream data;
    private int offset;

    void addFullFrame(Message fullFrame){
        this.fullFrames.add(fullFrame);
        this.addMessage(fullFrame);
    }

    void addMessage(Message message){
        this.messages.add(message);
    }

    int getFullFrameCount(){
        return this.fullFrames.size();
    }

    void setOffset(int offset){
        this.offset = offset;
    }

    void writeFullFrameHeader(DataOutputStream os) throws IOException {
        if(fullFrames.size() <= 0){
            return;
        }
        os.writeInt(this.fullFrames.get(0).getTimestamp());
        os.writeInt(this.offset);
    }

    int writeMessages() throws IOException {
        int lastTimestamp = -1; // causes first message including timestamp
        this.data = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(new DeflaterOutputStream(data));
        for (int i = 0; i < messages.size(); i++) {
            Message message = (Message) messages.get(i);

            // write message (with timestamp if needed)
            int timestamp = message.getTimestamp();

            if (timestamp == lastTimestamp)
                message.write(os, Message.NO_TIMESTAMP);
            else
                message.write(os, timestamp);

            lastTimestamp = timestamp;

            if (i % 1000 == 0)
                System.out.print(".");
        }
        os.close();
        this.data.close();
        return this.data.size();
    }

    void writeData(DataOutputStream os) throws IOException {
        os.write(this.data.toByteArray());
    }
}
