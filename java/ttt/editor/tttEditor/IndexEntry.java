package ttt.editor.tttEditor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;

import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A class containing and providing access to the messages for a certain period of playback time, along with searchable
 * text, a thumbnail and a title.
 */
public class IndexEntry {

    IndexEntry previousEntry = null, nextEntry = null;

    private int timestamp;
    private ImageIcon thumbnail;
    private String title;
    private SimpleSearch search;

    private int messagePointer = 0;

    // the messages with timestamps which fall within the bounds of this index entry
    private MessageArrayList<Message> messages = new MessageArrayList<Message>();
    // the annotation messages required to paint on the thumbnail for this entry
    private MessageArrayList<AnnotationMessage> annotationsForThumbs;
    // the additional entries, taken from previous entries, which are needed to ensure
    // this entry can paint itself completely when first displayed
    private List<Message> additionalMessages;

    private Header header;

    /**
     * Class constructor.
     * 
     * @param title
     *            index entry title
     * @param timestamp
     *            entry timestamp in milliseconds
     * @param searchableText
     *            searchable text for the entry
     * @param thumbnail
     *            thumbnail image
     * @param header
     *            <code>Header</code> object containing useful file data
     */
    public IndexEntry(String title, int timestamp, String searchableText, Image thumbnail, Header header) {
        setTimestampWithoutSynch(timestamp);
        setTitle(title);
        setSearchableText(searchableText);
        setThumbnail(thumbnail);
        this.header = header;
    }

    /*******************************************************************************************************************
     * Methods to create, modify and initialize message arrays
     ******************************************************************************************************************/

    void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * Ensures that the <code>IndexEntry</code> has all the <code>Message</code>s it requires to paint itself when
     * it begins. If it does not contain sufficient messages with the same timestamp as the <code>IndexEntry</code>
     * itself (e.g. enough <code>FramebufferMessage</code>s to cover the entire screen, or a
     * <code>BlankPageMessage</code> to confirm whether the whiteboard is currently turned on or off, or
     * <code>CursorMessage</code>s to confirm the shape and position of the cursor), it must ask the previous
     * <code>IndexEntry</code> for the remaining messages, if it is present.
     */
    public void initialize() {
        Area initiallyCoveredArea = new Area();

        boolean blankPageNeeded = true;
        boolean removeAllNeeded = true;
        boolean cursorShapeNeeded = true;
        boolean cursorPositionNeeded = true;

        int count = 0;
        // code expected to get necessary framebuffer messages even when a blank page is turned on
        // which is strictly unnecessary if the index spans the whole of the time the blank page is on
        // (but it is not certain that this shall always be the case - particularly if the user can change index
        // divisions)
        while (count < messages.size() && messages.get(count).getTimestamp() <= timestamp) {
            // if a BlankPageMessage is at the very beginning, no need to search for an older one
            if (messages.get(count) instanceof BlankPageMessage) {
                blankPageNeeded = false;
                count++;
                continue;
            }
            // add up all the areas of covered by FrameBufferUpdateMessages to see if they fill the whole screen
            if (messages.get(count) instanceof FramebufferMessage) {
                initiallyCoveredArea.add(new Area(((FramebufferMessage) messages.get(count)).getAffectedArea()));
                count++;
                continue;
            }
            // if a removeAll annotation is at the very beginning, no need to search for older annotations
            if (messages.get(count) instanceof AnnotationMessage && removeAllNeeded) {
                if (((AnnotationMessage) messages.get(count)).isRemoveAll())
                    removeAllNeeded = false;
                count++;
                continue;
            }
            if (messages.get(count) instanceof CursorShapeMessage) {
                cursorShapeNeeded = false;
                count++;
                continue;
            }
            if (messages.get(count) instanceof CursorMoveMessage) {
                cursorPositionNeeded = false;
                count++;
                continue;
            }
            count++;
        }

        if (previousEntry != null) {
            additionalMessages = previousEntry.getCompletingMessages(initiallyCoveredArea, removeAllNeeded,
                    blankPageNeeded, cursorShapeNeeded, cursorPositionNeeded);
        }
        // /REVISE THIS LAST BIT...
        annotationsForThumbs = getAnnotationsForThumbnail();
    }

    // returns any remaining Messages which a subsequent IndexEntry requires for initial display.
    // the parameters are used to indicate which Messages are still needed.
    // if it is true that the IndexEntries have been initialized in order (and it should be), then
    // there is no need to check previous entries to get any Messages which are not present here:
    // as each entry requires the same information, if this entry does not have all the messages then
    // the entries before it must also be lacking those messages, and so they can safely be assumed
    // not to exist.
    private LinkedList<Message> getCompletingMessages(Area currentArea, boolean annotationsNeeded,
            boolean blankPageNeeded, boolean cursorShapeNeeded, boolean cursorPositionNeeded) {

        boolean frameBufferMessagesNeeded = (!currentArea.equals(header.getFullArea()));
        LinkedList<Message> newMessages = new LinkedList<Message>();

        // check the main Message array
        for (int count = messages.size() - 1; count >= 0; count--) {
            if (!frameBufferMessagesNeeded && !annotationsNeeded && !blankPageNeeded && !cursorShapeNeeded
                    && !cursorPositionNeeded)
                return newMessages;
            if (messages.get(count) instanceof FramebufferMessage && frameBufferMessagesNeeded) {
                Rectangle affectedRect = ((FramebufferMessage) messages.get(count)).getAffectedArea();
                // if the rectangle covered by the framebuffer update message is already covered by a later
                // message, then ignore it
                if (currentArea.contains(affectedRect))
                    continue;
                // if framebuffer update message offers something new, add it to the array and add the area
                newMessages.addFirst(messages.get(count));
                currentArea.add(new Area(affectedRect));
                // test is screen is full
                if (currentArea.equals(header.getFullArea()))
                    frameBufferMessagesNeeded = false;
                continue;
            }
            // add BlankPageMessage to array: stop looking for blank page messages
            if (messages.get(count) instanceof BlankPageMessage && blankPageNeeded) {
                newMessages.addFirst(messages.get(count));
                blankPageNeeded = false;
                continue;
            }
            // if AnnotationMessage is a remove all, don't add it and stop looking
            // otherwise add it to the new message list
            if (messages.get(count) instanceof AnnotationMessage && annotationsNeeded) {
                if (((AnnotationMessage) messages.get(count)).isRemoveAll())
                    annotationsNeeded = false;
                else
                    newMessages.addFirst(messages.get(count));
                continue;
            }
            if (messages.get(count) instanceof CursorShapeMessage && cursorShapeNeeded) {
                cursorShapeNeeded = false;
                newMessages.addFirst(messages.get(count));
                continue;
            }
            if (messages.get(count) instanceof CursorMoveMessage && cursorPositionNeeded) {
                cursorPositionNeeded = false;
                newMessages.addFirst(messages.get(count));
            }
        }

        // adds any messages still required from stored additionalMessages (if applicable)
        if (additionalMessages != null) {
            for (int count = additionalMessages.size() - 1; count >= 0; count--) {
                if (!frameBufferMessagesNeeded && !annotationsNeeded && !blankPageNeeded && !cursorShapeNeeded
                        && !cursorPositionNeeded)
                    return newMessages;
                if (additionalMessages.get(count) instanceof FramebufferMessage && frameBufferMessagesNeeded) {
                    Rectangle affectedRect = ((FramebufferMessage) additionalMessages.get(count)).getAffectedArea();
                    // if the rectangle covered by the framebuffer update message is already covered by a later
                    // message, then ignore it
                    if (currentArea.contains(affectedRect))
                        continue;
                    // if framebuffer update message offers something new, add it to the array and add the area
                    newMessages.addFirst(additionalMessages.get(count));
                    currentArea.add(new Area(affectedRect));
                    // test is screen is full
                    if (currentArea.equals(header.getFullArea()))
                        frameBufferMessagesNeeded = false;
                    continue;
                }
                // add BlankPageMessage to array: stop looking for blank page messages
                if (additionalMessages.get(count) instanceof BlankPageMessage && blankPageNeeded) {
                    newMessages.addFirst(additionalMessages.get(count));
                    blankPageNeeded = false;
                    continue;
                }
                // if AnnotationMessage is a remove all, don't add it and stop looking
                // otherwise add it to the new message list
                if (additionalMessages.get(count) instanceof AnnotationMessage && annotationsNeeded) {
                    if (((AnnotationMessage) additionalMessages.get(count)).isRemoveAll())
                        annotationsNeeded = false;
                    else
                        newMessages.addFirst(additionalMessages.get(count));
                    continue;
                }

                if (additionalMessages.get(count) instanceof CursorShapeMessage && cursorShapeNeeded) {
                    cursorShapeNeeded = false;
                    newMessages.addFirst(additionalMessages.get(count));
                    continue;
                }
                if (additionalMessages.get(count) instanceof CursorMoveMessage && cursorPositionNeeded) {
                    cursorPositionNeeded = false;
                    newMessages.addFirst(additionalMessages.get(count));
                }
            }
        }
        return newMessages;
    }

    private MessageArrayList<AnnotationMessage> getAnnotationsForThumbnail() {

        MessageArrayList<AnnotationMessage> annotations = new MessageArrayList<AnnotationMessage>();

        if (additionalMessages != null) {
            for (int count = 0; count < additionalMessages.size(); count++) {
                if (!(additionalMessages.get(count) instanceof AnnotationMessage))
                    continue;
                annotations.add((AnnotationMessage) additionalMessages.get(count));
            }
        }

        boolean firstAnnotationFound = false;

        for (int count = 0; count < messages.size(); count++) {
            if (!(messages.get(count) instanceof AnnotationMessage))
                continue;
            AnnotationMessage currentAnnotation = (AnnotationMessage) messages.get(count);
            if (currentAnnotation.isRemoveAll())
                // if the first annotation message is a remove all with a nearby timestamp,
                // clear current list and continue looking for annotations - do not want
                // to show no annotations just because the screen was cleared near the start
                // of the index, even if others are added later
                if (firstAnnotationFound || currentAnnotation.getTimestamp() - timestamp > 1000)
                    return annotations;
                else
                    annotations.clear();
            else
                annotations.add(currentAnnotation);
            firstAnnotationFound = true;
        }

        return annotations;
    }

    /*******************************************************************************************************************
     * Methods for dealing with adding / removing IndexEntries
     ******************************************************************************************************************/

    /**
     * Remove messages from the main message list of this entry which have a timestamp equal to or greater than the
     * specified time.
     * 
     * @param timeMS
     *            the timestamp of the first message to be passed
     * @return the desired messages, or <code>null</code> if the passed time is equal to the timestamp of this entry.
     *         In this case, getMessages should have been used instead. The purpose of this method is to remove messages
     *         in order to create a new index entry, and so not all messages should be taken.
     */
    public MessageArrayList<Message> removeMessagesAfterTime(int timeMS) {
        if (timeMS == getTimestamp())
            return null;
        MessageArrayList<Message> newMessages = new MessageArrayList<Message>();
        int count = 0;
        while (count < messages.size() && messages.get(count).getTimestamp() < timeMS)
            count++;

        while (count < messages.size())
            newMessages.add(messages.remove(count));

        return newMessages;
    }

    /**
     * Append the specified messages to the main message list for this index.
     * 
     * @param newMessages
     *            the messages to be appended
     */
    public void appendMessages(List<Message> newMessages) {
        messages.addAll(newMessages);
    }

    void setMessages(MessageArrayList<Message> messages) {
        this.messages = messages;
    }

    /**
     * Get the main message list for this entry.
     * 
     * @return a <code>MessageArrayList</code> containing all the messages of this entry (excluding any messages
     *         obtained from other entries for initialization purposes)
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Adjusts the timestamps of the index entry and all its containing messages. NOTE:- All index entries should be
     * changed!
     * 
     * @param offset
     *            the amount by which to change the timestamps. Generally this will be a negative number, as the method
     *            is useful for trimming the beginning of a TTT file; later messages need to be moved back to fill in
     *            the gap.
     */
    private void remapTimestamps(int offset) {
        timestamp += offset;
        for (int i = 0; i < messages.size(); i++)
            messages.get(i).timestamp += offset;
        if (nextEntry != null)
            nextEntry.remapTimestamps(offset);
    }

    /**
     * Tells this index entry that it is now the first. It then removes any references to previous entries, and moves
     * messages from its start-up message list (which it has obtained from previous messages) to its main message list.
     * It also shifts all of the timestamps of all messages and index entries in the entire index back to fill in any
     * gap at the beginning of the file, if the timestamp of this entry is greater than 0.
     */
    protected void setAsFirstEntry() {
        previousEntry = null;
        if (additionalMessages != null) {
            // set timestamps of required messages to zero
            for (int i = 0; i < additionalMessages.size(); i++) {
                additionalMessages.get(i).timestamp = timestamp;
            }
            // add messages to list
            messages.addAll(0, additionalMessages);
            additionalMessages.clear();
        }
        if (timestamp > 0)
            remapTimestamps(-timestamp);
    }

    /*******************************************************************************************************************
     * Method for getting thumbnails with annotations
     ******************************************************************************************************************/

    /**
     * Get the thumbnail, possibly with annotations added.
     * 
     * @param showAnnotations
     *            paint annotations on the thumbnail. If <code>false</code>, the value of
     *            <code>includeHighlights</code> is irrelevant
     * @param includeHighlights
     *            include highlights among annotations painted on the thumbnail. If <code>false</code>, only
     *            non-highlight annotations are painted.
     * @return the thumbnail with any required annotations
     */
    public ImageIcon getThumbnail(boolean showAnnotations, boolean includeHighlights) {
        if (!showAnnotations || thumbnail == null)
            return thumbnail;

        Image newThumb = TTTEditor.getInstance().createImage(thumbnail.getIconWidth(), thumbnail.getIconHeight());
        Graphics2D g2 = (Graphics2D) (newThumb.getGraphics());
        g2.drawImage(thumbnail.getImage(), 0, 0, null);
        double horizontalScale = (double) thumbnail.getIconWidth() / header.framebufferWidth;
        double verticalScale = (double) thumbnail.getIconHeight() / header.framebufferHeight;
        g2.scale(horizontalScale, verticalScale);
        if (showAnnotations) {
            for (int count2 = 0; count2 < annotationsForThumbs.size(); count2++) {
                AnnotationMessage nextAnnotation = annotationsForThumbs.get(count2);
                if (includeHighlights || !nextAnnotation.isHighlight())
                    nextAnnotation.paint(g2, null);
            }
        }
        return new ImageIcon(newThumb);
    }

    /*******************************************************************************************************************
     * Methods for playback
     ******************************************************************************************************************/

    /**
     * Reset the internal message pointer to 0. The message pointer is used to keep track of the last message passed
     * during normal playback.
     */
    public void resetMessagePointer() {
        messagePointer = 0;
        indexActive = false;
    }

    /**
     * Get the value of the internal message pointer. The message pointer is used to keep track of the last message
     * passed during normal playback.
     * 
     * @return the current value of the message pointer
     */
    protected int getMessagePointer() {
        return messagePointer;
    }

    /**
     * Set the value of the internal message pointer. The message pointer is used to keep track of the last message
     * passed during normal playback.
     * 
     * @param pointer
     *            the new value for the message pointer
     */
    protected void setMessagePointer(int pointer) {
        if (pointer > messages.size())
            messagePointer = 0;
        else
            messagePointer = pointer;
    }

    // whether the entry thinks it is active or not
    // is tied closely to message pointer, but at times message pointer
    // may be zero but the index is active.
    // However if the entry is not active, then its message pointer should not
    // remain at a value other than zero
    private boolean indexActive = false;

    /**
     * Add the initial index messages to a list passed as a parameter. The initial index messages are all the messages
     * needed to paint the entry as soon as it should appear. This includes all the messages with the same timestamp as
     * the entry, plus any messages from previous entries which are needed to ensure that a full screen update is
     * possible and the annotation list / cursor / whiteboard status is up-to-date.
     * 
     * @param currentMessages
     *            the list to which the messages should be added
     */
    public void getInitialIndexMessages(List<Message> currentMessages) {
//        LinkedList<Message> newMessages;
        if (additionalMessages != null)
            currentMessages.addAll(additionalMessages);

        int messagePointer = 0;
        while (messagePointer < messages.size() && messages.get(messagePointer).getTimestamp() <= getTimestamp()) {
            currentMessages.add(messages.get(messagePointer));
            messagePointer++;
        }
        indexActive = true;
    }

    /**
     * Add all the messages remaining in this entry after the position of the message pointer to a list passed as a
     * parameter. This is useful when playback shifts to a new entry, as this method can be called by the new entry to
     * ensure that no messages are left unprocessed.
     * 
     * @param currentMessages
     *            the list to which the messages should be added
     */
    protected void getRemainingMessages(List<Message> currentMessages) {
        while (messagePointer < messages.size()) {
            currentMessages.add(messages.get(messagePointer));
            messagePointer++;
        }
        messagePointer = 0;
        indexActive = false;
    }

    /**
     * Add all messages falling between the last message passed and the specified time to a list passed as a parameter.
     * This may require getting messages from the previous index entry.
     * 
     * @param mediaTime
     *            the current playback time in milliseconds
     * @param currentMessages
     *            the list to which the messages should be added
     */
    public void getPlayingMessages(int mediaTime, List<Message> currentMessages) {
        if (!indexActive && previousEntry != null)
            previousEntry.getRemainingMessages(currentMessages);

        while (messagePointer < messages.size() && messages.get(messagePointer).getTimestamp() <= mediaTime) {
            currentMessages.add(messages.get(messagePointer));
            messagePointer++;
        }
        indexActive = true;
    }

    /**
     * Add all the messages needed for displaying the screen at the given time to a list passed as a parameter. <br />
     * Implementation note:- currently this method works by combining the initial index messages with all the messages
     * contained in this entry up to the specified time. Although this does not require much calculation, in many cases
     * it may result in too many messages being passed. Since indexes are generally short the problem is not very great,
     * but in some instances it may cause unnecessary delays and could be improved.
     * 
     * @param mediaTime
     *            the current playback time in milliseconds
     * @param currentMessages
     *            the list to which the messages should be added
     */
    public void getSeekingMessages(int mediaTime, List<Message> currentMessages) {
        // think about if the index lasts more than 2 minutes
        if (additionalMessages != null)
            currentMessages.addAll(additionalMessages);
        messagePointer = 0;

        while (messagePointer < messages.size() && messages.get(messagePointer).getTimestamp() <= mediaTime) {
            currentMessages.add(messages.get(messagePointer));
            messagePointer++;
        }
        indexActive = true;
    }

    /*******************************************************************************************************************
     * Methods for search
     ******************************************************************************************************************/

    /**
     * Search the text stored in the entry.
     * 
     * @param word
     *            the text to search for
     * @return <code>true</code> if the entry contains the search text, <code>false</code> otherwise
     */
    public boolean search(String word) {
        if (search != null)
            return search.contains(word);

        return false;
    }

    /**
     * Set the searchable text in the entry.
     * 
     * @param text
     *            the new searchable text
     */
    public void setSearchableText(String text) {
        search = new SimpleSearch(text);
    }

    /**
     * Add more text to the end of the searchable text already stored by the entry.
     * 
     * @param newText
     *            the text to append
     */
    public void appendSearchableText(String newText) {
        if (search != null) {
            String oldText = search.getText();
            String text = oldText + "\n" + newText;
            search = new SimpleSearch(text);
        } else
            search = new SimpleSearch(newText);
    }

    /**
     * Get the searchable text stored in the entry
     * 
     * @return a string representation of the searchable text stored by the entry
     */
    public String getSearchableText() {
        if (search == null)
            return "";
        else
            return new String(search.getText());
    }

    /*******************************************************************************************************************
     * Methods for thumbnails
     ******************************************************************************************************************/

    /**
     * Get the thumbnail for the entry.
     * 
     * @return the thumbnail stored by the entry; may be <code>null</code> if no thumbnail available.
     */
    public ImageIcon getThumbnail() {
        return thumbnail;
    }

    /**
     * Set the thumbnail for the entry.
     * 
     * @param thumbnail
     *            the new thumbnail image
     */
    public void setThumbnail(Image thumbnail) {
        if (thumbnail != null)
            this.thumbnail = new ImageIcon(thumbnail, getTimeStampAsString());
        else
            this.thumbnail = null;
    }

    /*******************************************************************************************************************
     * Methods for timestamp
     ******************************************************************************************************************/

    /**
     * Get a string representation of the timestamp of this entry, synchronizing it as necessary.
     * 
     * @return a string representation of the timestamp of this entry.
     */
    public String getTimeStampAsString() {
        // generates nice time String
        int timest = getTimestamp();
        boolean negative = timest < 0;
        if (negative)
            timest = -getTimestamp();

        int sec = timest / 1000 % 60;
        int min = timest / 60000;
        return (negative ? "-" : "") + ((min < 10) && !negative ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    /**
     * Get the last timestamp of the last message in this entry, adjusting it if synchronization is turned on.
     * 
     * @return the last timestamp of the last message held by this entry.
     */
    public int getLastTimestamp() {
        return messages.getLastTimestamp();
    }

    /**
     * Get the last timestamp of the last message in this entry as it is stored, regardless of whether synchronization
     * is turned on.
     * 
     * @return the last timestamp of the last message held by this entry.
     */
    public int getLastTimestampWithoutSync() {
        return messages.getLastTimestampWithoutSync();
    }

    /**
     * Get the timestamp of the entry, adjusting it as necessary if synchronization is turned on.
     * 
     * @return the timestamp of the entry
     */
    public int getTimestamp() {
        if (Parameters.synchronize)
            return (int) (timestamp * header.synchRatio);
        else
            return timestamp;
    }

    /**
     * Get the timestamp stored by the entry as it is, regardless of whether synchronization is turned on.
     * 
     * @return the timestamp of the entry
     */
    public int getTimestampWithoutSync() {
        return timestamp;
    }

    /**
     * Set the timestamp to the specified value, regardless of whether synchronization is turned on.
     * 
     * @param timestamp
     *            the new timestamp
     */
    public void setTimestampWithoutSynch(int timestamp) {
        this.timestamp = timestamp;
    }

    /*******************************************************************************************************************
     * Methods for title
     ******************************************************************************************************************/

    /**
     * Get the title of the entry
     * 
     * @return the title of the entry
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the entry.
     * 
     * @param title
     *            the new title
     */
    public void setTitle(String title) {
        if (title == null) {
            this.title = "";
        } else {
            if (title.length() > 255)
                title = title.substring(255);
            this.title = title;
        }
    }

    /*******************************************************************************************************************
     * Other methods
     ******************************************************************************************************************/

    // calculate index number dynamically
    // first index has number 1
    /**
     * Get the number of this entry. This is always 1 greater than the position of the entry in the <code>Index</code>,
     * since the first entry has a number of 1 but a position of 0.
     * 
     * @return the number of this entry.
     */
    public int getNumber() {
        if (previousEntry == null)
            return 1;
        else
            return previousEntry.getNumber() + 1;
    }

    /**
     * Overrides <code>toString()</code> method of class <code>Object</code>
     * 
     * @return formatted string including the index number and timestamp.
     */
    public String toString() {
        if (Parameters.synchronize)
            return "Index " + getNumber() + ": " + title + " "
                    + TTTEditor.getStringFromTime((int) (timestamp * header.synchRatio));
        else
            return "Index " + getNumber() + ": " + title + " " + TTTEditor.getStringFromTime(timestamp);
    }

    /**
     * Get the annotations contained within the main message list of this entry.
     * 
     * @return a list of annotations for this entry
     */
    protected LinkedList<AnnotationMessage> getAnnotations() {
        LinkedList<AnnotationMessage> annotations = new LinkedList<AnnotationMessage>();
        for (int count = 0; count < messages.size(); count++)
            if (messages.get(count) instanceof AnnotationMessage)
                annotations.add((AnnotationMessage) messages.get(count));
        return annotations;
    }

    /*******************************************************************************************************************
     * Methods for removing an annotation
     ******************************************************************************************************************/

    /**
     * Remove an annotation from the main message list of this entry. Should also check that the annotation does not
     * occur in other message lists of other entries, and if it does they should also be amended.
     * 
     * @param message
     *            the annotation to be removed
     * @return <code>true</code> if the annotation is successfully removed, <code>false</code> otherwise. If the
     *         annotation does not occur in the entry, <code>false</code> will be returned.
     */
    public boolean removeAnnotation(AnnotationMessage message) {
        boolean successful = false;
        if (annotationsForThumbs != null)
            successful = (messages.remove(message) | annotationsForThumbs.remove(message));
        else
            successful = messages.remove(message);

        if (!successful)
            return false;
        else
            // initialize to make sure message/thumbnail lists are correct
            initialize();

        if (nextEntry != null)
            nextEntry.removeBackwardReferencesToMessage(message);
        return true;
    }

    private void removeBackwardReferencesToMessage(Message message) {
        boolean successful = false;
        if (annotationsForThumbs != null)
            successful = annotationsForThumbs.remove(message);
        if (additionalMessages != null)
            if (!successful)
                successful = additionalMessages.remove(message);
            else
                additionalMessages.remove(message);

        // initialize to make sure message/thumbnail lists are correct
        initialize();

        // only check next entry if is to be found somewhere in current entry
        if (successful && nextEntry == null)
            nextEntry.removeBackwardReferencesToMessage(message);
    }

    /*******************************************************************************************************************
     * Methods / class for editing or previewing a new entry
     ******************************************************************************************************************/

    /**
     * Display an internal dialog with options to edit details of the index entry. <br />
     * [May be preferable in the future to return the edit panel instead, since internal frames may not always be used]
     */
    public void displayEditPane() {
        IndexEntryComponent component = new IndexEntryComponent();
        int selection = JOptionPane.showInternalConfirmDialog(TTTEditor.getInstance().getDesktopPane(), component,
                "Index Entry: " + getNumber(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (selection == JOptionPane.OK_OPTION)
            component.commitChanges();
    }

    /**
     * Get a panel to be used for previewing an entry before adding it to the real <code>Index</code>.
     * 
     * @return a <code>JPanel</code> containing the main visible data stored in the entry - thumbnail, title,
     *         timestamp and searchable text.
     */
    public IndexEntryComponent getPreviewPanel() {
        return new IndexEntryComponent(true);
    }

    public class IndexEntryComponent extends JPanel {

        JTextField titleField;
        JTextArea searchArea;

        IndexEntryComponent() {
            this(false);
        }

        IndexEntryComponent(boolean preview) {
            super(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();

            JLabel titleLabel = new JLabel("Title: ");
            titleField = new JTextField(title);
            titleField.setColumns(20);
            JLabel timeLabel = new JLabel("Timestamp: ");
            JLabel timestampLabel = new JLabel(getTimestamp() + " ms");

            c.weightx = 0.5;
            c.weighty = 0.5;
            c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(5, 0, 5, 0);
            c.gridx = 0;
            c.gridy = 1;
            add(titleLabel, c);
            c.gridx = 1;
            add(titleField, c);
            c.gridx = 0;
            c.gridy = 2;
            add(timeLabel, c);
            c.gridx = 1;
            add(timestampLabel, c);

            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 3;
            c.anchor = GridBagConstraints.CENTER;
            JLabel thumbLabel = new JLabel();
            thumbLabel.setHorizontalAlignment(JLabel.CENTER);
            if (thumbnail == null)
                thumbLabel.setText("No thumbnail available");
            else
                thumbLabel.setIcon(thumbnail);

            add(thumbLabel, c);

            c.anchor = GridBagConstraints.LINE_START;
            c.gridy = 4;

            if (preview) {
                c.insets = new Insets(5, 0, 0, 0);
                JLabel searchLabel1 = new JLabel("Search text copied from previous index entry - ");
                add(searchLabel1, c);
                c.gridy = 5;
                c.insets = new Insets(0, 0, 5, 0);
                JLabel searchLabel2 = new JLabel("  both entries may need to be corrected manually");
                add(searchLabel2, c);
                c.insets = new Insets(5, 0, 5, 0);
                c.gridy = 6;
            }

            searchArea = new JTextArea();
            searchArea.setLineWrap(true);
            searchArea.setWrapStyleWord(true);
            searchArea.setToolTipText("Edit searchable text for current slide");
            searchArea.setText(getSearchableText());
            searchArea.setCaretPosition(0);
            JScrollPane textScrollPane = new JScrollPane();
            textScrollPane.setViewportView(searchArea);
            textScrollPane.setPreferredSize(new Dimension(350, 250));

            add(textScrollPane, c);
        }

        public void commitChanges() {
            title = titleField.getText();
            String newSearchText = searchArea.getText();
            if (newSearchText != null && !newSearchText.equals(getSearchableText()))
                setSearchableText(newSearchText);
        }

    }

    /*******************************************************************************************************************
     * Methods for writing to an output stream
     ******************************************************************************************************************/

    // ONLY USED DURING DEVELOPMENT - COUNTS TOTAL MESSAGES WRITTEN
    // CONSIDER DELETING
    // COULD JUST RETURN NUMBER IN METHOD?
    /**
     * A counter which is incremented each time a message is written, and so may be used to determine how many messages
     * have been written when outputting to a file - it should be reset before use.
     */
    public static int counter = 0;

    /**
     * Write all the messages contained in the main message list of this entry to the given output stream.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the messages should be written
     * @throws java.io.IOException
     */
    public void writeMessages(DataOutputStream out) throws IOException {
        writeMessages(out, Integer.MAX_VALUE, 0);
    }

    /**
     * Write all the messages contained in the main message list of this entry to the given output stream, adjusting
     * their timestamps by adding the specified offset.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the messages should be written
     * @param timestampOffset the value which should be added to the timestamps of each message written
     * @throws java.io.IOException
     */
    public void writeMessages(DataOutputStream out, int timestampOffset) throws IOException {
        writeMessages(out, Integer.MAX_VALUE, timestampOffset);
    }

    /**
     * Write the messages required for initialization to the given output stream with a timestamp of 0. This is useful
     * when writing a file which begins at an entry other than the first entry, as it ensures that all the necessary
     * messages are present at the start of the new file being written.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the messages should be written
     * @throws java.io.IOException
     */
    public void writeInitializationMessages(DataOutputStream out) throws IOException {
        writeInitializationMessages(out, 0);
    }

    /**
     * Write the messages required for initialization to the given output stream with a timestamp equal to the offset
     * specified. This is useful when concatening a file which begins at an entry other than the first entry, as it
     * ensures that all the necessary messages are present at the start of the new file being written.
     * 
     * @param timestampOffset
     *            the value which should be used as the first timestamp
     * @param out
     *            the <code>DataOutputStream</cpde> to which the messages should be written
     * @throws java.io.IOException
     */
    public void writeInitializationMessages(DataOutputStream out, int timestampOffset) throws IOException {
        if (additionalMessages == null || additionalMessages.size() < 1)
            return;
        Iterator<Message> iterator = additionalMessages.iterator();
        Message msg = iterator.next();
        // change timestamp temporarily
        // TODO: put a method in message to enable writing with different timestamp
        int timestamp = msg.timestamp;       
        msg.timestamp = timestampOffset;  
   
        msg.writeMessage(out, true);    
        
        msg.timestamp = timestamp;
        while (iterator.hasNext()) {
            msg = iterator.next();
            
            msg.writeMessage(out, false);
            
            counter++;
        }
    }

    /**
     * Write all the messages contained in the main message list of this entry to the given output stream, adjusting
     * their timestamps by adding the specified offset, and adding no messages with timestamps greater than the
     * specified end time.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the messages should be written
     * @param endTimeMS the timestamp after which no messages should be written
     * @param timestampOffset the value which should be added to the timestamps of each message written
     * @throws java.io.IOException
     */
    public void writeMessages(DataOutputStream out, int endTimeMS, int timestampOffset) throws IOException {
        int lastTimestamp = -1; // ensures first timestamp will be written
        // int lastEncoding = -1;
        Iterator<Message> iterator = messages.iterator();
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            // stop writing if end point reached
            if (msg.getTimestamp() > endTimeMS) {
                return;
            }

            // change timestamp temporarily
            int timestamp = msg.timestamp;
            msg.timestamp = msg.getTimestamp() + timestampOffset;
            msg.writeMessage(out, (lastTimestamp != msg.timestamp));
            lastTimestamp = msg.timestamp;
            // lastEncoding = msg.encoding;
            // return timestamp to what it was
            msg.timestamp = timestamp;
            counter++;
        }
    }

    /**
     * Write the index extension data for this entry.
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the extension data should be written
     * @param timeOffset
     *            the value which should be added to the timestamps of each entry written
     * @param includeThumbs
     *            <code>true</code> if thumbnails should be included in the extension, <code>false</code> otherwise
     * @throws java.io.IOException
     */
    public void writeEntryForExtension(DataOutputStream out, int timeOffset, boolean includeThumbs) throws IOException {
        // timestamp (int)
        out.writeInt(getTimestamp() + timeOffset);

        // title length (byte)
        out.writeByte(title.length());
        // title (String / bytes)
        out.writeBytes(title);

        // searchable length (int)
        String searchableText = getSearchableText();
        out.writeInt(searchableText.length());
        // searchable text (String / bytes)
        out.writeBytes(searchableText);

        // image (size and bytes)
        if (includeThumbs)
            writeThumbnail(out);
        else
            out.writeInt(0);
    }

    /**
     * Write the index extension data for this entry.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the extension data should be written
     * @param includeThumbs <code>true</code> if thumbnails should be included in the extension, <code>false</code> otherwise
     * @throws java.io.IOException
     */
    public void writeEntryForExtension(DataOutputStream out, boolean includeThumbs) throws IOException {
        // timestamp (int)
        out.writeInt(getTimestamp());

        // title length (byte)
        out.writeByte(title.length());
        // title (String / bytes)
        out.writeBytes(title);

        // searchable length (int)
        String searchableText = getSearchableText();
        out.writeInt(searchableText.length());
        // searchable text (String / bytes)
        out.writeBytes(searchableText);

        // image (size and bytes)
        if (includeThumbs)
            writeThumbnail(out);
        else
            out.writeInt(0);
    }

    // include thumbs
    /**
     * Write the index extension data for this entry.
     * 
     * @param out
     *            the <code>DataOutputStream</cpde> to which the extension data should be written
     * @throws java.io.IOException
     */
    public void writeEntryForExtension(DataOutputStream out) throws IOException {
        writeEntryForExtension(out, true);
    }

    private void writeThumbnail(DataOutputStream out) throws IOException {
        // write thumbnail image
        if (thumbnail == null)
            // no thumbnail available
            out.writeInt(0);
        else {
            // buffer to determine size of image
            BufferedImage bufferedImage = new BufferedImage(thumbnail.getIconWidth(), thumbnail.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(thumbnail.getImage(), 0, 0, null);
            ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", imageOut);
            imageOut.flush();
            // write size + image
            out.writeInt(imageOut.size());
            out.write(imageOut.toByteArray());
        }
    }

}