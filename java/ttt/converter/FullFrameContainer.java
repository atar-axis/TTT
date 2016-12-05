package ttt.converter;

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

    void addFullFrame(Message fullFrame) {
        this.fullFrames.add(fullFrame);
        this.addMessage(fullFrame);
    }

    void addMessage(Message message) {
        // fallback to prevent mistakes
        if(this.fullFrames.size() == 0){
            this.fullFrames.add(message);
        }
        this.messages.add(message);
    }

    int getFullFrameCount() {
        return this.fullFrames.size();
    }

    void setOffset(int offset) {
        this.offset = offset;
    }

    int getOffset() {
        return this.offset;
    }

    void writeFullFrameHeader(DataOutputStream os) throws IOException {
        if (fullFrames.size() <= 0) {
            return;
        }
        os.writeInt(this.fullFrames.get(0).getTimestamp());
        os.writeInt(this.offset);
    }

    int writeMessages() throws IOException {
        int lastTimestamp = -1; // causes first message including timestamp
        this.data = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(new DeflaterOutputStream(data));
        for (Message message : messages) {
            // write message (with timestamp if needed)
            int timestamp = message.getTimestamp();
            if (timestamp == lastTimestamp) {
                message.write(os, Message.NO_TIMESTAMP);
            } else {
                message.write(os, timestamp);
            }
            lastTimestamp = timestamp;
        }
        os.close();
        this.data.close();
        return this.data.size();
    }

    void writeData(DataOutputStream os) throws IOException {
        os.write(this.data.toByteArray());
    }
}
