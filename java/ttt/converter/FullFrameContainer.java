package ttt.converter;

import ttt.Constants;
import ttt.gui.GraphicsContext;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.messages.WhiteboardMessage;
import ttt.record.Recording;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastianstein on 05.12.16.
 */
class FullFrameContainer {
    public ArrayList<Message> messages = new ArrayList<>();
    private ByteArrayOutputStream data;
    private int offset;

    void addMessage(Message message) {
        this.messages.add(message);
    }

    void setOffset(int offset) {
        this.offset = offset;
    }

    int getOffset() {
        return this.offset;
    }

    void writeFullFrameHeader(DataOutputStream os) throws IOException {
        if (messages.size() <= 0) {
            return;
        }
        os.writeInt(this.messages.get(0).getTimestamp());
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

    static ArrayList<FullFrameContainer> createContainer(Recording recording) throws IOException {
        ArrayList<FullFrameContainer> containerList = new ArrayList<>();
        ArrayList<Message> tmpMessages = new ArrayList<>();
        ArrayList<Message> messages = recording.getMessages().getMessages();

        for (Message message : messages) {
            if (isFullFrame(recording, message)) {
                containerList.addAll(generateContainer(recording, tmpMessages));
                tmpMessages.clear();
            }
            tmpMessages.add(message);
        }
        containerList.addAll(generateContainer(recording, tmpMessages));
        return containerList;
    }

    private static ArrayList<FullFrameContainer> generateContainer(Recording recording, ArrayList<Message> messages) throws IOException {
        ArrayList<FullFrameContainer> containerList = new ArrayList<>();
        FullFrameContainer container = new FullFrameContainer();
        int count = 0;

        for (Message message : messages) {
            if (message.getEncoding() == Constants.EncodingHextile) {
                count += message.getSize();
            } else {
                count += message.getSize() * 20;
            }
        }

        // TODO improve data size based heuristic
        if (count > 1000000) {
            int size = (int) Math.floor(count / Math.floor(count / 700000));
            count = 0;
            for (Message message : messages) {
                if (message.getEncoding() == Constants.EncodingHextile) {
                    count += message.getSize();
                } else {
                    count += message.getSize() * 20;
                }
                if (count >= size) {
                    TTTConverter.log(Constants.getStringFromTime(container.messages.get(0).getTimestamp()) + ": " + count, 1);
                    count = 0;
                    containerList.add(container);
                    container = generateNewFullFrame(recording, container.messages);
                }
                container.addMessage(message);
            }
            TTTConverter.log(Constants.getStringFromTime(container.messages.get(0).getTimestamp()) + ": " + count, 1);
            containerList.add(container);
        } else {
            for (Message message : messages) {
                container.addMessage(message);
            }
            TTTConverter.log(Constants.getStringFromTime(container.messages.get(0).getTimestamp()) + ": " + count, 1);
            containerList.add(container);
        }
        return containerList;
    }

    private static FullFrameContainer generateNewFullFrame(Recording recording, ArrayList<Message> previousMessages) throws IOException {
        FullFrameContainer container = new FullFrameContainer();
        Message tmpMessage;
        int timestamp = previousMessages.get(previousMessages.size() - 1).getTimestamp();
        GraphicsContext context = new GraphicsContext(recording.getProtocolPreferences());
        for (Message previous : previousMessages) {
            if (previous != null) {
                previous.paint(context);
            }
        }

        // create new fullFrame
        Message prevFullFrame = previousMessages.get(0);
        if (prevFullFrame.getEncoding() == Constants.EncodingWhiteboard) {
            TTTConverter.log("generate whiteboard message", 1);
            WhiteboardMessage prev = (WhiteboardMessage) previousMessages.get(0);
            tmpMessage = new WhiteboardMessage(timestamp, prev.getPageNumber(), recording.getProtocolPreferences());
            container.addMessage(tmpMessage);
        } else {
            TTTConverter.log("generate hextile message", 1);
            tmpMessage = new HextileMessage(timestamp, 0, 0, recording.getProtocolPreferences().framebufferWidth, recording.getProtocolPreferences().framebufferHeight, context);
            container.addMessage(tmpMessage);
        }

        // TODO test annotations (new deleteAll?)
        container.messages.add(new DeleteAllAnnotation(timestamp));
        container.messages.addAll(Arrays.asList(context.getCurrentAnnotationsAsArray()));

        // TODO test cursor
        tmpMessage = (context.getCurrentCursorMessage());
        if (tmpMessage != null) {
            container.addMessage(tmpMessage);
        }
        tmpMessage = (context.getCurrentCursorPositionMessage());
        if (tmpMessage != null) {
            container.addMessage(tmpMessage);
        }

        // TODO update Timestamps
        for (Message m : container.messages) {
            m.setTimestamp(timestamp);
        }
        return container;
    }

    private static boolean isFullFrame(Recording recording, Message message) {
        boolean is = false;
        int encoding = message.getEncoding();
        switch (encoding < 0 ? encoding : encoding & Constants.EncodingMask) {
            case Constants.AnnotationRectangle:
            case Constants.AnnotationHighlight:
            case Constants.AnnotationLine:
            case Constants.AnnotationFreehand:
            case Constants.AnnotationImage:
            case Constants.AnnotationText:
            case Constants.AnnotationDelete:
            case Constants.AnnotationDeleteAll:
                break;
            case Constants.EncodingTTTCursorPosition:
            case Constants.EncodingTTTRichCursor:
            case Constants.EncodingTTTXCursor:
                break;
            case Constants.EncodingWhiteboard:
                if (message instanceof WhiteboardMessage) {
                    is = true;
                }
                break;
            case Constants.EncodingHextile:
                if (message instanceof HextileMessage) {
                    Rectangle bounds = ((HextileMessage) message).getBounds();
                    is = bounds.width == recording.getProtocolPreferences().framebufferWidth && bounds.height == recording.getProtocolPreferences().framebufferHeight;
                }
                break;
            case Constants.EncodingRaw:
            case Constants.EncodingInterlacedRaw:
                break;
            default:
                TTTConverter.log("Message with unknown encoding: " + Constants.encodingToString(encoding), 1);
        }
        return is;
    }
}
