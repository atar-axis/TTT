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

/*
 * Created on 15.03.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import ttt.messages.Annotation;
import ttt.messages.CursorMessage;
import ttt.messages.CursorPositionMessage;
import ttt.messages.DeleteAllAnnotation;
import ttt.messages.FramebufferUpdateMessage;
import ttt.messages.HextileMessage;
import ttt.messages.Message;
import ttt.messages.WhiteboardMessage;

public class Messages {

    private ArrayList<Message> messages = new ArrayList<Message>();
    private Recording recording;

    public Messages(Recording recording) {
        this.recording = recording;
    }

    public void close() {
        messages.clear();
        messages = null;
        recording = null;
    }

    public int size() {
        return messages.size();
    }

    public Message get(int i) {
        return messages.get(i);
    }

    /*******************************************************************************************************************
     * read messages *
     ******************************************************************************************************************/

    private int count;
    private int total;
    private boolean containsCursorMessages;
    private boolean containsAnnotations;
    private boolean containsWhiteboard;

    // private int sec = 0;
    // private int messagesPerSecond = 0;

    // read all messages and store them
    void readMessages(DataInputStream in) throws IOException {
        try {
            if (TTT.verbose)
                System.out.println("Loading ");

            // // NOTE: only working for old recordings (new recorder doesn't have update stripes)
            // // detect message loss (e.g. caused by network failure)
            // int check_density = 0;

            int timestamp = 0;

            while (true) {
                // TODO: show progress
                // progressMonitor.setProgress(totalSize);
                Message message = Message.readMessage(in, recording.prefs);
                if (message == null)
                    continue;

                // TODO: maybe adding additional timestamp is better suited
                // always start at 0
                if (messages.size() == 0)
                    message.setTimestamp(0);

                // use previous timestamp if unset
                // TODO: maybe zero and unset timestamp should be distinguished
                if (message.getTimestamp() == 0)
                    message.setTimestamp(timestamp);

                // fix inconsistent timestamps
                if (message.getTimestamp() < timestamp) {
                    if (TTT.verbose && message.getTimestamp() + 1000 < timestamp)
                        System.out.println("fixing incosistent timestamp ["
                                + Constants.getStringFromTime(message.getTimestamp()) + " -> "
                                + Constants.getStringFromTime(timestamp) + "\tdiff "
                                + Constants.getStringFromTime(timestamp - message.getTimestamp()) + "]\t" + message);
                    message.setTimestamp(timestamp);
                }

                // keep timestamp
                timestamp = message.getTimestamp();

                // add message to message array
                messages.add(message);

                // // messages per second
                // if (timestamp / 1000 == sec) {
                // messagesPerSecond++;
                // } else {
                // sec++;
                // if (messagesPerSecond > 0)
                // System.out.println(Constants.getStringFromTime(sec * 1000) + "\t" + messagesPerSecond);
                // messagesPerSecond = 0;
                // }

                // set flags
                switch (message.getEncoding()) {
                case Constants.AnnotationRectangle:
                case Constants.AnnotationHighlight:
                case Constants.AnnotationLine:
                case Constants.AnnotationFreehand:
                case Constants.AnnotationImage:          // MODMSG
                case Constants.AnnotationText:           // MODMSG
                    containsAnnotations = true;
                    break;

                case Constants.EncodingWhiteboard:
                    containsWhiteboard = true;
                    break;

                case Constants.EncodingTTTRichCursor:
                case Constants.EncodingTTTXCursor:
                    containsCursorMessages = true;
                    break;

                case Constants.EncodingHextile:
                    total += ((HextileMessage) message).getCoveredArea();
                    break;
                }

                if (TTT.verbose)
                    if (count++ % 1000 == 0)
                        System.out.print(".");
            }

        } catch (EOFException e) {
            in.close();
        }

        if (TTT.verbose)
            System.out.println(" " + messages.size() + " messages loaded.");

        if (TTT.debug)
            if (TTT.verbose)
                System.out.println("raw framebuffer usage would be " + (long) total * recording.prefs.bytesPerPixel
                        / 1024 / 1024 + " Mbytes messages (" + (total / 1024 / 1024) + " megapixels)");

        // delete all annotations at beginning (if needed)
        boolean insert = true;
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message.getTimestamp() > 0)
                break;
            else if (message.getEncoding() == Constants.AnnotationDeleteAll) {
                insert = false;
                break;
            }
        }
        if (insert)
            messages.add(0, new DeleteAllAnnotation(0));

    }

    public int getNumberOfPixels() {
        return total;
    }

    /*******************************************************************************************************************
     * write messages
     ******************************************************************************************************************/

    void writeMessages(DataOutputStream out) throws IOException {
        // write all messages
        System.out.print("writing ");

        int lastTimestamp = -1; // causes first message including timestamp

        for (int i = 0; i < messages.size(); i++) {
            Message message = (Message) messages.get(i);

            // write message (with timestamp if needed)
            int timestamp = message.getTimestamp();

            if (timestamp == lastTimestamp)
                message.write(out, Message.NO_TIMESTAMP);
            else
                message.write(out, timestamp);

            lastTimestamp = timestamp;

            if (i % 1000 == 0)
                System.out.print(".");
        }
        out.close();
        System.out.println(" done.");
    }

    /*******************************************************************************************************************
     * keyframes *
     ******************************************************************************************************************/

    synchronized public void setTime_full_frame_check(int time) {
        // long t = System.currentTimeMillis();

        boolean[][] covered = new boolean[recording.prefs.framebufferWidth][recording.prefs.framebufferHeight];
        int coveredCounter = 0;

        // if not set newly, this causes a call of stop() in run() beacuse end of recording reached
        int newNext = messages.size();
        int full = recording.prefs.framebufferWidth * recording.prefs.framebufferHeight;

        int totalCounter = 0;

        ArrayList<Message> collected = new ArrayList<Message>();

        // graphicsContext.memGraphics.setColor(Color.MAGENTA);
        // graphicsContext.memGraphics.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);

        boolean whiteboard = !containsWhiteboard;
        boolean cursor = !containsCursorMessages;
        boolean cursorPosition = !containsCursorMessages;
        boolean deleteAll = !containsAnnotations;

        // a little preview (500msec) can be useful to get a more complete frame
        // because framebuffer updates or annotations on whiteboard can be delayed
        // TODO: verify if always better in pause mode
        // TODO: verify if useful if not in pause mode
        if (recording.paused())
            time += 500;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);

            // skip future
            if (message.getTimestamp() > time) {
                newNext = i;
                continue;
            }
            totalCounter++;

            // find latest cursor
            // NOTE: Cursor(Positions)Messages are FrameBufferUpdateMessages, but do not cover desktop
            if (message instanceof CursorMessage) {
                if (!cursor) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    cursor = true;
                }
            } else if (message instanceof CursorPositionMessage) {
                if (!cursorPosition) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    cursorPosition = true;
                }
            }

            // find latest whiteboard status
            else if (message instanceof WhiteboardMessage) {
                if (!whiteboard) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    whiteboard = true;

                    // TODO: think about following optimazation
                    // does not work for continous playback after whiteboard, because offscreen image is not updated
                    // // if whiteboard is enabled, previous framebufferupdates are irrelevant
                    // if (((WhiteboardMessage) message).isWhiteboardEnabled()) coveredCounter = full;
                }
            }

            // message is part of keyframe, because it covers unset pixels
            else if (message instanceof FramebufferUpdateMessage) {
                if (coveredCounter < full) {

                    // update covered area
                    Rectangle rectangle = ((FramebufferUpdateMessage) message).getBounds();
                    if (message instanceof WhiteboardMessage)
                        rectangle = new Rectangle(0, 0, recording.prefs.framebufferWidth,
                                recording.prefs.framebufferHeight);

                    // graphicsContext.memGraphics.setColor(Color.CYAN);
                    // graphicsContext.memGraphics.fillRect(rectangle.x,rectangle.y,rectangle.width,rectangle.height);

                    boolean add = false;

                    // set covered pixels
                    for (int y = 0; y < rectangle.height; y++)
                        for (int x = 0; x < rectangle.width; x++)
                            try {
                                if (!covered[rectangle.x + x][rectangle.y + y]) {
                                    // add only once (outside loop)
                                    add = true;

                                    // mark pixel
                                    coveredCounter++;
                                    covered[rectangle.x + x][rectangle.y + y] = true;
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                // possibly caused by changing the screen resolution (which is not supported by the TTT)
                            }
                    // add at beginning
                    if (add)
                        collected.add(0, message);
                }
            }

            // all annotation since last deleteAll
            else if (message instanceof Annotation) {
                if (!deleteAll) {

                    // add at beginning
                    collected.add(0, message);

                    // if deleteAll all previous annotations are irrelevant
                    deleteAll = message instanceof DeleteAllAnnotation;
                }
            }

            // add all other messages
            else {
                // add at beginning
                collected.add(0, message);
            }

            // finished if gathered everything needed for keyframe or if reached current playback time
            if (i == recording.next_message
                    || (coveredCounter == full && deleteAll && cursor && cursorPosition && whiteboard))
                break;

            // clear whiteboard at beginning
            if (i == 0 && !whiteboard)
                collected.add(0, new WhiteboardMessage(0, 0, recording.prefs));

            // clear annotations at beginning
            if (i == 0 && !deleteAll)
                collected.add(0, new DeleteAllAnnotation(0));
        }

        // t = System.currentTimeMillis() - t;
        // System.out.println("Keyframe computation: "+Constants.getStringFromTime((int) t));
        // t = System.currentTimeMillis();

        // now paint collected messages (keyframe)
        for (int i = 0; i < collected.size(); i++) {
            // System.out.println(collected.get(i));
            collected.get(i).paint(recording.graphicsContext);
        }

        // set next message for run loop
        recording.next_message = newNext != -1 ? newNext : 0;

        // t = System.currentTimeMillis() - t;
        // System.out.print("\t" + Constants.getStringFromTime((int) t));
        // System.out.println("\t"+collected.size() + "\t(" + totalCounter + ")\t"
        // + Constants.getStringFromTime(time - collected.get(0).getTimestamp()) + " "
        // + ((collected.size() > 0 && time - collected.get(0).getTimestamp() > 120000)) + "\t- ");
    }

    synchronized public void setTime_full_frame_check_regarding_stripes(int time) {
        // long t = System.currentTimeMillis();

        // if not set newly, this causes a call of stop() in run() beacuse end of recording reached
        int newNext = messages.size();

        int totalCounter = 0;

        ArrayList<Message> collected = new ArrayList<Message>();

        // graphicsContext.memGraphics.setColor(Color.MAGENTA);
        // graphicsContext.memGraphics.fillRect(0, 0, prefs.framebufferWidth, prefs.framebufferHeight);

        boolean covered = false;
        boolean whiteboard = !containsWhiteboard;
        boolean cursor = !containsCursorMessages;
        boolean cursorPosition = !containsCursorMessages;
        boolean deleteAll = !containsAnnotations;

        // a little preview (500msec) can be useful to get a more complete frame
        // because framebuffer updates or annotations on whiteboard can be delayed
        // TODO: verify if always better in pause mode
        // TODO: verify if useful if not in pause mode
        if (recording.paused())
            time += 500;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);

            // skip future
            if (message.getTimestamp() > time) {
                newNext = i;
                continue;
            }
            totalCounter++;

            // find latest cursor
            // NOTE: Cursor(Positions)Messages are FrameBufferUpdateMessages, but do not cover desktop
            if (message instanceof CursorMessage) {
                if (!cursor) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    cursor = true;
                }
            } else if (message instanceof CursorPositionMessage) {
                if (!cursorPosition) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    cursorPosition = true;
                }
            }

            // find latest whiteboard status
            else if (message instanceof WhiteboardMessage) {
                if (!whiteboard) {
                    // add at beginning
                    collected.add(0, message);
                    // mark as finished
                    whiteboard = true;

                    // TODO: think about following optimazation
                    // does not work for continous playback after whiteboard, because offscreen image is not updated
                    // // if whiteboard is enabled, previous framebufferupdates are irrelevant
                    // if (((WhiteboardMessage) message).isWhiteboardEnabled()) coveredCounter = full;
                }
            }

            // message is part of keyframe, because it covers unset pixels
            else if (message instanceof FramebufferUpdateMessage) {
                if (!covered) {
                    if (message.getTimestamp() > time - 125000)
                        // within 2 minutes
                        // add at beginning
                        collected.add(0, message);
                    else
                        // any pixel should be covered by stripes now
                        covered = true;
                }
            }

            // all annotation since last deleteAll
            else if (message instanceof Annotation) {
                if (!deleteAll) {

                    // add at beginning
                    collected.add(0, message);

                    // if deleteAll all previous annotations are irrelevant
                    deleteAll = message instanceof DeleteAllAnnotation;
                }
            }

            // add all other messages
            else {
                // add at beginning
                collected.add(0, message);
            }

            // finished if gathered everything needed for keyframe or if reached current playback time
            if (i == recording.next_message || (covered && deleteAll && cursor && cursorPosition && whiteboard))
                break;

            // clear whiteboard at beginning
            if (i == 0 && !whiteboard)
                collected.add(0, new WhiteboardMessage(0, 0, recording.prefs));

            // clear annotations at beginning
            if (i == 0 && !deleteAll)
                collected.add(0, new DeleteAllAnnotation(0));
        }

        // t = System.currentTimeMillis() - t;
        // System.out.println("Keyframe computation: "+Constants.getStringFromTime((int) t));
        // t = System.currentTimeMillis();

        // now paint collected messages (keyframe)
        for (int i = 0; i < collected.size(); i++) {
            // System.out.println(collected.get(i));
            collected.get(i).paint(recording.graphicsContext);
        }

        // set next message for run loop
        recording.next_message = newNext != -1 ? newNext : 0;

        // t = System.currentTimeMillis() - t;
        // System.out.print("\t" + Constants.getStringFromTime((int) t));
        // System.out.println("\t"+collected.size() + "\t(" + totalCounter + ")\t"
        // + Constants.getStringFromTime(time - collected.get(0).getTimestamp()) + " "
        // + ((collected.size() > 0 && time - collected.get(0).getTimestamp() > 120000)) + "\t- ");
    }

    /*******************************************************************************************************************
     * Statistics *
     ******************************************************************************************************************/
    public void statistics() {
        int[] encoding_count = new int[256];

        for (int i = 0; i < messages.size(); i++) {
            Message message = (Message) messages.get(i);
            encoding_count[message.getEncoding()]++;
        }

        // output
        String[] encoding = new String[256];
        for (int i = 0; i < encoding.length; i++)
            encoding[i] = "Unknown [" + i + "]";

        encoding[Constants.EncodingTTTCursorPosition] = "CursorPosition";
        encoding[Constants.EncodingTTTXCursor] = "XCursor";
        encoding[Constants.EncodingTTTRichCursor] = "RichCursor";
        encoding[Constants.EncodingFlagUpdate] = "Update FILTER";
        encoding[Constants.EncodingFlagTimestamp] = "Timestamp FILTER";
        encoding[Constants.AnnotationRectangle] = "Rectangle";
        encoding[Constants.AnnotationLine] = "Line";
        encoding[Constants.AnnotationFreehand] = "Free";
        encoding[Constants.AnnotationHighlight] = "Highlight";
        encoding[Constants.AnnotationImage] = "Image";      // MODMSG
        encoding[Constants.AnnotationText] = "Text";        // MODMSG
        encoding[Constants.AnnotationDelete] = "Delete";
        encoding[Constants.AnnotationDeleteAll] = "Delete all";
        encoding[Constants.EncodingRecording] = "Recording FILTER";
        encoding[Constants.EncodingRaw] = "Raw";
        encoding[Constants.EncodingCopyRect] = "CopyRect";
        encoding[Constants.EncodingRRE] = "RRE";
        encoding[Constants.EncodingCoRRE] = "CoRRE";
        encoding[Constants.EncodingHextile] = "Hextile";
        encoding[Constants.EncodingZlib] = "Zlib";
        encoding[Constants.EncodingTight] = "Tight";
        encoding[Constants.EncodingWhiteboard] = "BlankPage";

        System.out.println();
        System.out.println("STATISTICS:");
        System.out.println();
        System.out.println("Qty:\tEncoding:");
        for (int i = 0; i < encoding.length; i++) {
            if (encoding_count[i] > 0)
                System.out.println(" " + encoding_count[i] + "\t " + encoding[i]);
        }
        System.out.println();
        int annotations = encoding_count[Constants.AnnotationRectangle] + encoding_count[Constants.AnnotationLine]
                + encoding_count[Constants.AnnotationFreehand] + encoding_count[Constants.AnnotationHighlight]
                + encoding_count[Constants.AnnotationDelete] + encoding_count[Constants.AnnotationDeleteAll];
        System.out.println(" " + annotations + "\t annotations");
        System.out.println();
    }

    // TODO: more checks
    public void checkDuration(int duration) {
        if (messages.get(messages.size() - 1).getTimestamp() < duration) {
            System.out.println("expand duration of desktop recording");
            messages.add(new DeleteAllAnnotation(duration));
        }
    }
}
