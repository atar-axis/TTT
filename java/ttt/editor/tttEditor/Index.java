package ttt.editor.tttEditor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ProgressMonitor;
import javax.swing.JOptionPane;

import java.util.LinkedList;
import java.util.List;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/*
 * Note:- this contains code to remove annotations which do not remove anything
 *        (these will then not be saved in any files output)
 *        this code could be removed if it is a bad idea for use with other players
 *        to take out any remove all events (e.g. if it slows down seeking), although it
 *        is probably always safe to take out ineffective single remove events
 *
 */

/**
 * Class for storing, accessing and modifying <code>IndexEntry</code>s, and used by the
 * <code>PlaybackController</code> to obtain the appropriate <code>Message</code>s during playback.
 */
public class Index {

    private IndexEntry firstEntry = null;
    private IndexEntry lastEntry = null;

    private Header header;
    private int size = 0;
    private boolean thumbnails_available = false;
    private boolean searchable = false;

    /**
     * Creates a new instance of <code>Index</code>, reading the index entries from the file. In order for the
     * <code>Index</code> to be useful, <code>Message</code>s will need to be added using the
     * <code>addMessagesToIndexes</code> method.
     * 
     * @param in
     *            the <code>DataInputStream</code> from which to read the index extension
     * @param header
     *            the <code>Header</code> object required for some methods
     * @throws java.io.IOException
     */
    public Index(DataInputStream in, Header header) throws IOException {

        this.header = header;
        thumbnails_available = true;
        // header
        int number_of_table_entries = in.readShort();
        for (int i = 0; i < number_of_table_entries; i++) {
            // timestamp
            int timestamp = in.readInt();
            // title
            int titleLength = in.readByte();
            byte[] titleArray = new byte[titleLength];
            in.readFully(titleArray);
            String title = new String(titleArray);
            // searchable text
            int searchableLength = in.readInt();
            if (searchableLength > 0) {
                searchable = true;
            }
            byte[] searchableArray = new byte[searchableLength];
            in.readFully(searchableArray);

            String searchableText = new String(searchableArray);

            // thumbnail
            BufferedImage image = null;
            try {
                image = readThumbnail(in);
            } catch (IOException e) {
                System.out.println("Problem reading thumbnail");
            }

            // add index entry
            addIndex(title, timestamp, searchableText, image);
        }
        System.out.println("\nRead index with " + size + " entries.\n");
    }

    /**
     * Creates a new instance of <code>Index</code>, generated from a list of messages. These messages are then
     * divided among the appropriate <code>IndexEntries</code> and initialized, so that the <code>Index</code> is
     * ready to use.
     * 
     * @param messages
     *            the <code>LinkedList</code> of <code>Message</code>s which will be added to the
     *            <code>Index</code>
     * @param header
     *            the <code>Header</code> object required for some methods
     */
    public Index(LinkedList<Message> messages, Header header) {

        int minSlideArea = (int) (header.framebufferHeight * header.framebufferWidth * 0.2); // 20% of
        // framebufferwidth *
        // framebufferheight
        int minSlideDiffMsecs = 5000;

        this.header = header;
        // build slide index
        // containing all message which area covers minSlideArea and a time
        // interval of at least minSlideDiffMsecs

        // new protocol
        // build index based on area
        int timestamp = Integer.MIN_VALUE + 1;
        int animationCount = 0;
        int lastMessageTimestamp = -1;
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message.area > minSlideArea) {
                if (message.getTimestampWithoutSync() - timestamp > minSlideDiffMsecs || size == 0) {
                    if (animationCount > 0 && animationCount < 5 && lastMessageTimestamp >= 0) {
                        // no animation, take last message of sequence
                        // (animations take first message of sequence as
                        // index)
                        if (size > 0)
                            adjustIndex(size - 1, "", lastMessageTimestamp);
                        else
                            // first index
                            addIndex("", lastMessageTimestamp);
                    }

                    animationCount = 0;

                    addIndex("", message.getTimestampWithoutSync());
                } else {
                    // distinguish animations from multiple slide
                    // changes
                    animationCount++;
                    lastMessageTimestamp = message.getTimestampWithoutSync();
                }

                timestamp = message.getTimestampWithoutSync();
            }
        }

        // fix last index if needed
        if (animationCount > 0 && animationCount < 5 && lastMessageTimestamp >= 0 && size > 0) {
            // no animation, take last message of sequence
            // (animations take first message of sequence as index)
            adjustIndex(size - 1, "", lastMessageTimestamp);
        }

        int lastIndexTimestamp = get(size - 1).getTimestampWithoutSync();
        if ((size > 0) && (lastIndexTimestamp >= messages.get(messages.size() - 1).getTimestampWithoutSync())) {
            // remove last index if it uses timestamp of last message
            remove(size - 1);
        }
        // add index at beginning if needed
        if (size == 0 || get(0).getTimestampWithoutSync() > 2000) {
            addIndex("Start", 0);
            System.out.println("\nAdded index at beginning.");
        }
        System.out.println("\nGenerated index with " + size + " entries.\n");
    }

    /**
     * Adds a <code>LinkedList</code> of messages to the appropriate <code>IndexEntries</code>. This is necessary
     * if the index divisions have been read from a file. This method assumes that the <code>LinkedList</code>
     * contains messages which are already sorted in order of timestamp.
     * 
     * @param messages
     *            a <code>LinkedList</code> containing all the messages
     */
    public void addMessagesToIndexes(LinkedList<Message> messages) {
        System.out.println("Adding messages to index....");

        // note removal of unnecessary annotations
        LinkedList<AnnotationMessage> tempAnnotations = new LinkedList<AnnotationMessage>();

        int indexNumber = 0;
        int nextIndexTimestamp = 0;

        IndexEntry currentIndex = get(indexNumber);
        if (size > 1)
            nextIndexTimestamp = get(1).getTimestamp();

        while (indexNumber < size - 1) {
            // modified by Ziewer - 23.06.2006
            // fixes crash, caused otherwise if last index contains only redundant (and therfore removed) messages
            if (messages.isEmpty())
                break;
            // modified end

            Message currentMessage = messages.remove();

            // ///////////////////////////////////////////////////////////////////////////
            /*
             * following section can be taken away: it removes ineffective annotation remove messages from the lists,
             * which is good for my player but may not be for all players
             * 
             * Does not remove if final message, since this may be used for calculating desktop duration
             */
            if (currentMessage instanceof AnnotationMessage && indexNumber != size - 1 && !messages.isEmpty()) {
                AnnotationMessage annotation = (AnnotationMessage) currentMessage;
                // don't add remove if it doesn't remove anything
                if (annotation.isRemove()) {
                    int size = tempAnnotations.size();
                    annotation.processRemove(tempAnnotations);
                    if (size == tempAnnotations.size())
                        continue;
                }
                // do not add remove all message if there is nothing to remove
                else if (annotation.isRemoveAll())
                    if (tempAnnotations.size() == 0)
                        continue;
                    else
                        tempAnnotations.clear();
                // always add ordinary (non-remove) annotations
                else 
                    tempAnnotations.add(annotation);
            }

            // /////////////////////////////////////////////////////////////////////////////////

            if (currentMessage.getTimestamp() >= nextIndexTimestamp) {
                indexNumber++;
                currentIndex = get(indexNumber);
                if (indexNumber < size - 1)
                    nextIndexTimestamp = get(indexNumber + 1).getTimestamp();
            }
            // added by Ziewer in 01.06.2007
            // whiteboard is impliced remove all event (for new recordings)
            if(currentMessage instanceof BlankPageMessage)
                tempAnnotations.clear();
            // end added

            currentIndex.addMessage(currentMessage);
        }
        while (messages.size() > 0) {
            currentIndex.addMessage(messages.remove());
        }

        initialize();
        System.out.println("----------------Done adding messages");
    }

    // initializes each IndexEntry
    private void initialize() {
        IndexEntry e = firstEntry;
        while (e != null) {
            e.initialize();
            e = e.nextEntry;
        }
    }

    // reads a thumbnail image from an input stream
    private BufferedImage readThumbnail(DataInputStream in) throws IOException {
        int image_size = in.readInt();
        if (image_size == 0) {
            // thumbnail not available
            thumbnails_available = false;
            return null;
        } else {
            // thumbnail available
            byte[] image_array = new byte[image_size];
            in.readFully(image_array);
            return ImageIO.read(new ByteArrayInputStream(image_array));
        }
    }

    /**
     * Queries whether the index is searchable. Only useful upon creation when determining whether or not to try to read
     * a search text file - isn't updated in response to the user inputting search text
     * 
     * @return <code>true</code> if the index contains searchable text, <code>false</code> otherwise
     */
    protected boolean isSearchable() {
        return searchable;
    }

    /**
     * Reads searchable text for each <code>IndexEntry</code> from a file.
     * 
     * @param file
     *            a text file containing searchable text
     */
    public void readSearchBaseFromFile(File file) {
        try {
            // read file
            BufferedReader in = new BufferedReader(new FileReader(file));

            char[] characters = new char[(int) file.length()];

        
			in.read(characters);
            int begin = 0;
            int count = 0;
            for (int i = 0; i < characters.length; i++) {
                if (characters[i] == 12) {
                    System.out.print(".");
                    String text = new String(characters, begin, i + 1 - begin);

                    if (count < size)
                        (get(count)).setSearchableText(text);
                    count++;
                    begin = i + 1;
                }
            }
            if (count != size) {
                System.out.println(" expected " + size + " entries - found " + count);
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(), "Searchbase "
                        + (count < size ? "incomplete" : "not suitable") + "\nFound " + count + " entries (expected "
                        + size + ")", "Searchbase incomplete", JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println(" ok");
            }
            searchable = true;

        } catch (IOException e) {
            System.out.println("failed (" + e + ")");
        }
    }

    // create a new IndexEntry without any searchable text or thumbnail
    private void addIndex(String title, int timestamp) {
        addIndex(title, timestamp, null, null);
    }

    // create a new IndexEntry
    private void addIndex(String title, int timestamp, String searchableText, Image thumbnail) {
        IndexEntry entry = new IndexEntry(title, timestamp, searchableText, thumbnail, header);
        addIndex(entry);
    }

    // add an IndexEntry - used duringn initialization
    private void addIndex(IndexEntry entry) {
        size++;
        // if there are currently no entries
        if (firstEntry == null) {
            firstEntry = entry;
            lastEntry = firstEntry;
            return;
        } else {
            IndexEntry currentEntry = null;
            for (currentEntry = firstEntry; currentEntry != null; currentEntry = currentEntry.nextEntry) {
                if (entry.getTimestamp() < currentEntry.getTimestamp()) {
                    entry.nextEntry = currentEntry;
                    entry.previousEntry = currentEntry.previousEntry;
                    currentEntry.previousEntry = entry;
                    if (entry.previousEntry == null)
                        firstEntry = entry;
                    else
                        entry.previousEntry.nextEntry = entry;
                    return;
                }
            }
            // insert as the last entry
            lastEntry.nextEntry = entry;
            entry.previousEntry = lastEntry;
            lastEntry = entry;
        }
    }

    // adjust a previously edited index - change title and timestamp
    // NOTE:- This method should only be used when creating the index at the beginning,
    // if used later, it will screw up message lists etc. and not try to fix them
    private void adjustIndex(int oldIndex, String title, int timestamp) {
        adjustIndex(oldIndex, title, timestamp, null, null);
    }

    // adjust a previously edited index - change title, timestamp, searchableText and thumbnail
    // NOTE:- This method should only be used when creating the index at the beginning,
    // if used later, it will screw up message lists etc. and not try to fix them
    private void adjustIndex(int oldIndex, String title, int timestamp, String searchableText, Image thumbnail) {
        if (oldIndex >= size)
            return;

        IndexEntry e = get(oldIndex);
        e.setTimestampWithoutSynch(timestamp);
        e.setSearchableText(searchableText);
        e.setTitle(title);
        e.setThumbnail(thumbnail);
    }

    /**
     * Completely remove a sequence of index entries at the beginning of a tile, and all their messages. This is useful
     * when trimming the beginning of a file.
     * 
     * @param entry
     *            the first index entry which should be kept
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public boolean deleteFirstIndexesCompletely(IndexEntry entry) {
        // no need to do anything if already the first entry
        if (entry.equals(firstEntry))
            return false;
        size -= entry.getNumber() - 1;
        firstEntry = entry;
        entry.setAsFirstEntry();
        return true;
    }

    /**
     * Completely remove a sequence of index entries at the end of a tile, and all their messages. This is useful when
     * trimming the end of a file.
     * 
     * @param entry
     *            the first index entry which should be removed
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public boolean deleteLastIndexesCompletely(IndexEntry entry) {
        // cannot remove the first entry
        if (entry.equals(firstEntry))
            return false;
        lastEntry = entry.previousEntry;
        lastEntry.nextEntry = null;
        size = lastEntry.getNumber();
        return true;
    }

    // added by Ziewer 30.03.2006
    // add final DeleteAll annotation with final timestamp
    // NOTE: may be redundent
    public void addFinalTimestamp(int timestamp) {
        if (lastEntry != null)
            lastEntry.addMessage(new AnnotationMessage(timestamp, ProtocolConstants.REMOVE_ALL, new byte[0],
                    new Header()));
    }

    // remove an IndexEntry (does nothing with messages)
    private void remove(int index) {
        if (index == 0) {
            removeFirst();
            return;
        }
        if (index == size - 1) {
            removeLast();
            return;
        }
        if (index < size) {
            size--;
            IndexEntry currentIndex = get(index);
            currentIndex.previousEntry.nextEntry = currentIndex.nextEntry;
            currentIndex.nextEntry.previousEntry = currentIndex.previousEntry;
        }
    }

    // remove the first IndexEntry
    private void removeFirst() {
        if (firstEntry != null) {
            size--;
            firstEntry = firstEntry.nextEntry;
            if (firstEntry != null)
                firstEntry.previousEntry = null;
            else
                lastEntry = null;
        }
    }

    // remove the last IndexEntry
    private void removeLast() {
        if (lastEntry != null) {
            size--;
            lastEntry = lastEntry.previousEntry;
            if (lastEntry != null)
                lastEntry.nextEntry = null;
            else
                firstEntry = null;
        }
    }

    /**
     * Gets a particular <code>IndexEntry</code> from the <code>Index</code>.
     * 
     * @param index
     *            position of the <code>IndexEntry</code> which should be returned
     * @return the <code>IndexEntry</code>
     */
    public IndexEntry get(int index) {
        if (index >= size)
            return null;

        IndexEntry e = firstEntry;
        for (int count = 0; count < index; count++) {
            e = e.nextEntry;
        }
        return e;
    }

    /**
     * Gets the total number of <code>Message</code>s contained in the entire <code>Index</code>; that is, the sum
     * of the <code>Message</code>s in each <code>IndexEntry</code>.
     * 
     * @return the total number of <code>Message</code>s
     */
    public int getTotalMessageCount() {
        int count = 0;
        IndexEntry entry = firstEntry;
        while (entry != null) {
            count += entry.getMessages().size();
            entry = entry.nextEntry;
        }
        return count;
    }

    /**
     * Queries whether thumbnails are available for the index.
     * 
     * @return <true> if thumbnails are stored in each <code>IndexEntry</code>, false otherwise
     */
    public boolean thumbnailsAvailable() {
        return thumbnails_available;
    }

    /**
     * Gets the number of <code>IndexEntries</code> in the <code>Index</code>.
     * 
     * @return the size of the Index.
     */
    public int size() {
        return size;
    }

    /**
     * Gets the timestamp of the last <code>Message</code> in the last <code>IndexEntry</code>.
     * 
     * @return the timestamp of the final <code>IndexEntry</code> (milliseconds)
     */
    public int getLastMessageTimestamp() {
        if (lastEntry != null)
            return lastEntry.getLastTimestamp();
        else
            return -1;
    }

    /**
     * Gets the timestamp of the last <code>Message</code> in the last <code>IndexEntry</code>, regardless of
     * whether or not synchronization is turned on.
     * 
     * @return the timestamp of the final <code>IndexEntry</code> (milliseconds)
     */
    public int getLastMessageTimestampWithoutSync() {
        if (lastEntry != null)
            return lastEntry.getLastTimestampWithoutSync();
        else
            return -1;
    }

    // creates an array of all available IndexEntries
    /**
     * Get the <code>IndexEntry</code>s of this <code>Index</code> in the form of an array.
     * 
     * @return an array of <code>IndexEntry</code>s
     */
    public IndexEntry[] getArray() {
        IndexEntry[] indexEntryArray = new IndexEntry[size];
        for (int count = 0; count < size; count++)
            indexEntryArray[count] = get(count);
        return indexEntryArray;
    }

    /**
     * Given a time in milliseconds, returns a reference to the IndexEntry which is current at that time.
     * 
     * @param mediaTimeMS
     *            the current media time (in milliseconds)
     * @return the position of the <code>IndexEntry</code>
     */
    public int getIndexFromTime(int mediaTimeMS) {
        IndexEntry e = lastEntry;
        for (int count = size - 1; count >= 0; count--) {
            if (e.getTimestamp() <= mediaTimeMS)
                return count;
            e = e.previousEntry;
        }
        // go to first index
        return 0;
    }

    /**
     * Generate thumbnails for each <code>IndexEntry</code>
     */
    public void createAllThumbnails() {
        int indexNo = 1;
        boolean blankPageOn = false;

        ProgressMonitor progressMonitor = new ProgressMonitor(TTTEditor.getInstance(), "Generating thumbnails...",
                "Index 1", 1, size);
        progressMonitor.setProgress(1);

        Image mainImage = TTTEditor.getInstance().createImage(header.framebufferWidth, header.framebufferHeight);
        Graphics mainGraphics = mainImage.getGraphics();
        IndexEntry e = firstEntry;
        while (e != null) {
            ArrayList<Message> messages = new ArrayList<Message>();
            e.getInitialIndexMessages(messages);
            e.resetMessagePointer();
            for (int count = 0; count < messages.size(); count++)
                if (messages.get(count) instanceof FramebufferMessage)
                    ((FramebufferMessage) messages.get(count)).paint(mainGraphics);
                else if (messages.get(count) instanceof BlankPageMessage)
                    blankPageOn = (((BlankPageMessage) messages.get(count)).getPageNumber() > 0);
            // not really any need to do this at this stage...?
            // if blankPageOn at the end, there will just be a white rectangle to be seen
            if (blankPageOn)
                mainGraphics.fillRect(0, 0, header.framebufferWidth, header.framebufferHeight);
            Image thumb = mainImage.getScaledInstance(header.framebufferWidth / Parameters.thumbnail_scale_factor,
                    header.framebufferHeight / Parameters.thumbnail_scale_factor, Image.SCALE_SMOOTH);
            e.setThumbnail(thumb);
            e = e.nextEntry;
            indexNo++;
            progressMonitor.setNote("Index " + indexNo);
            progressMonitor.setProgress(indexNo);
        }
        progressMonitor.close();
        thumbnails_available = true;
    }

    /**
     * Generate a thumbnail which displays a miniature version of what would be displayed on screen at a particular
     * playback time.
     * 
     * @param mediaTimeMS
     *            the playback time of the desired thumbnail in milliseconds
     * @return the thumbnail image generated
     */
    protected Image createThumbnailForTime(int mediaTimeMS) {
        Image mainImage = TTTEditor.getInstance().createImage(header.framebufferWidth, header.framebufferHeight);
        Graphics mainGraphics = mainImage.getGraphics();
        boolean blankPageOn = false;

        IndexEntry e = get(getIndexFromTime(mediaTimeMS));
        // be careful not to throw off message pointer
        int messagePointer = e.getMessagePointer();
        ArrayList<Message> messages = new ArrayList<Message>();
        e.getSeekingMessages(mediaTimeMS, messages);
        e.setMessagePointer(messagePointer);
        for (int count = 0; count < messages.size(); count++)
            if (messages.get(count) instanceof FramebufferMessage)
                ((FramebufferMessage) messages.get(count)).paint(mainGraphics);
            else if (messages.get(count) instanceof BlankPageMessage)
                blankPageOn = (((BlankPageMessage) messages.get(count)).getPageNumber() > 0);
        // not really any need to do this at this stage...?
        // if blankPageOn at the end, there will just be a white rectangle to be seen
        if (blankPageOn)
            mainGraphics.fillRect(0, 0, header.framebufferWidth, header.framebufferHeight);
        Image thumb = mainImage.getScaledInstance(header.framebufferWidth / Parameters.thumbnail_scale_factor,
                header.framebufferHeight / Parameters.thumbnail_scale_factor, Image.SCALE_SMOOTH);
        return thumb;
    }

    /**
     * Remove an <code>IndexEntry</code> from the <code>Index</code>. Appends any <code>Message</code>s and
     * searchable text from this <code>IndexEntry</code> to the previous <code>IndexEntry</code>. <br />
     * It is not possible to delete the first <code>IndexEntry</code>.
     * 
     * @param entry
     *            the <code>IndexEntry</code> to be removed
     * @return <code>true</code> if the <code>IndexEntry</code> is successfully removed, <code>false</code>otherwise
     */
    public boolean removeIndex(IndexEntry entry) {
        int index = entry.getNumber() - 1;
        if (index < 1)
            return false;

        // add any searchable text to the previous index entry
        String oldSearchableText = entry.getSearchableText();
        if (!oldSearchableText.equals(""))
            get(index - 1).appendSearchableText(oldSearchableText);

        List<Message> messages = entry.getMessages();
        get(index - 1).appendMessages(messages);
        remove(index);

        fireIndexRemovedEvent(index);
        return true;
    }

    private ArrayList<IndexListener> indexListeners = new ArrayList<IndexListener>();

    /**
     * add an <code>IndexListener</code> to the list held by the <code>Index</code>
     * 
     * @param i
     *            the <code>IndexListener</code> to be added
     */
    public void addIndexListener(IndexListener i) {
        indexListeners.add(i);
    }

    /**
     * removes all <code>IndexListener</code>s currently registered
     */
    public void resetIndexListeners() {
        indexListeners.clear();
    }

    /**
     * Notify listeners that an <code>IndexEntry</code> has been removed from the <code>Index</code>
     * 
     * @param index
     *            the former position of the <code>IndexEntry</code> which was removed
     */
    public void fireIndexRemovedEvent(int index) {
        for (int count = 0; count < indexListeners.size(); count++)
            indexListeners.get(count).indexEntryRemoved(index);
    }

    /**
     * Notify listeners that an <code>IndexEntry</code> has been added to the <code>Index</code>
     * 
     * @param index
     *            the position of the newly inserted <code>IndexEntry</code>
     * @param entry
     *            the <code>IndexEntry</code> which has been inserted
     */
    public void fireIndexAddedEvent(int index, IndexEntry entry) {
        for (int count = 0; count < indexListeners.size(); count++)
            indexListeners.get(count).indexEntryAdded(index, entry);
    }

    // TODO: consider effects of 2 indexes being very close to one another
    // with no messages in between - perhaps better then to adjust current index?
    /**
     * Inserts a new <code>IndexEntry</code> at a specified time (in ms). This will not succeed if there is already an
     * <code>IndexEntry</code> present with that timestamp. This method exists so that an index may be added when
     * trimming, so that the trimmed file starts at a new index entry. For adding during general playback, the
     * <code>PlaybackController</code> should be used.
     * 
     * @param timestamp
     *            the timestamp of the <code>IndexEntry</code> which should be inserted
     * @return the <code>IndexEntry</code> which was inserted if successful, <code>null</code> otherwise
     */
    protected IndexEntry insertIndex(int timestamp) {
        // cannot insert before first entry
        if (timestamp <= firstEntry.getTimestamp())
            return null;
        return insertIndex(createPreviewIndexEntry(timestamp));
    }

    /**
     * Inserts a new <code>IndexEntry</code>.
     * 
     * @param newEntry
     *            the entry to be inserted.
     * @return the <code>IndexEntry</code> which was inserted if successful, <code>null</code> otherwise
     */
    protected IndexEntry insertIndex(IndexEntry newEntry) {
        if (newEntry == null)
            return null;

        addIndex(newEntry);

        IndexEntry previousEntry = newEntry.previousEntry;
        MessageArrayList<Message> messages = previousEntry.removeMessagesAfterTime(newEntry.getTimestampWithoutSync());
        newEntry.setMessages(messages);

        previousEntry.resetMessagePointer();
        previousEntry.initialize();
        newEntry.initialize();

        fireIndexAddedEvent(newEntry.getNumber() - 1, newEntry);
        return newEntry;
    }

    /**
     * Create an <code>IndexEntry</code> with a given timestamp. Messages are not added to the entry until it is added
     * to the <code>Index</code> itself - this method only creates a preview entry with the thumbnail and search text
     * which would be in the actual entry.
     * 
     * @param timestamp
     *            the timestamp of the new entry to be created
     * @return the new <code>IndexEntry</code>
     */
    public IndexEntry createPreviewIndexEntry(int timestamp) {
        IndexEntry currentIndex = get(getIndexFromTime(timestamp));

        // if already an index at that time, do nothing
        if (currentIndex.getTimestamp() == timestamp) {
            return null;
        }

        String searchableText = get(getIndexFromTime(timestamp)).getSearchableText();

        // unsynching timestamp
        if (Parameters.synchronize)
            timestamp = (int) (timestamp / header.synchRatio + 0.5);

        Image thumb = null;
        if (thumbnails_available)
            thumb = createThumbnailForTime(timestamp);

        return new IndexEntry("", timestamp, searchableText, thumb, header);
    }

    /**
     * Remove an annotation completely
     * 
     * @param message
     *            the <code>AnnotationMessage</code> to be removed
     */
    public void removeAnnotation(AnnotationMessage message) {
        int timestamp = message.getTimestamp();
        IndexEntry entry = get(getIndexFromTime(timestamp));
        entry.removeAnnotation(message);
    }

    /*******************************************************************************************************************
     * Methods for writing data to file / getting data for writing
     ******************************************************************************************************************/

    /**
     * Write an index extension for this <code>Index</code> to a specified <code>DataOutputStream</code>
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the extension should be written
     * @param includeThumbs
     *            <code>true</code> if thumbnails should be included in the extension (if available,
     *            <code>false</code> if they should be omitted. <br />
     *            Thumbnails will not be generated if they are not already present, even if <code>true</code> is
     *            passed.
     * @throws java.io.IOException
     */
    public void writeIndexExtension(DataOutputStream out, boolean includeThumbs) throws IOException {
        // use buffer to that length can be determined
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream buffer = new DataOutputStream(byteArrayOutputStream);

        int entryCount = getIndexExtensionDataForBuffer(buffer, 0, includeThumbs);

        // length (adding on length of extension tag and count)
        out.writeInt(byteArrayOutputStream.size() + 3);
        // write encoding
        out.writeByte(ProtocolConstants.EXTENSION_INDEX_TABLE);
        // number of entries
        out.writeShort(entryCount);
        // extension data
        out.write(byteArrayOutputStream.toByteArray());
    }

    /**
     * Write an index extension for this <code>Index</code> to a specified <code>DataOutputStream</code>
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the extension should be written
     * @throws java.io.IOException
     */
    public void writeIndexExtension(DataOutputStream out) throws IOException {
        writeIndexExtension(out, true);
    }

    /**
     * Write only the elements of an index extension which come from specific <code>IndexEntry</code>s to a buffer -
     * that means without the extension encoding or extension size data at the beginning.
     * 
     * @param buffer
     *            the buffer to which to write the extension
     * @param offset
     *            the value which should be added to the timestamps of each entry written
     * @param includeThumbs
     *            <code>true</code> if thumbnails should be included in the extension (if available,
     *            <code>false</code> if they should be omitted. <br />
     *            Thumbnails will not be generated if they are not already present, even if <code>true</code> is
     *            passed.
     * @throws java.io.IOException
     * @return the number of index entries included in this extension
     */
    public int getIndexExtensionDataForBuffer(DataOutputStream buffer, int offset, boolean includeThumbs)
            throws IOException {
        int entryCount = 0;
        IndexEntry entry = firstEntry;
        while (entry != null) {
            // write entry
            entry.writeEntryForExtension(buffer, offset, includeThumbs);
            entry = entry.nextEntry;
            entryCount++;
        }
        buffer.flush();

        return entryCount;
    }

    /**
     * Write only the elements of an index extension which come from specific <code>IndexEntry</code>s to a buffer -
     * that means without the extension encoding or extension size data at the beginning. Limit the
     * <code>IndexEntry</code>s to be included.
     * 
     * @param buffer
     *            the buffer to which to write the extension
     * @param startEntry
     *            the <code>IndexEntry</code> which should be used as the first entry in the extension
     * @param endTimeMS
     *            the timestamp of the last <code>Message</code> which will be written: therefore the last
     *            <code>IndexEntry</code> should have a timestamp less than this. It should not have a timestamp equal
     *            to this, as that would result in an <code>IndexEntry</code> being created with no duration.
     * @param includeThumbs
     *            <code>true</code> if thumbnails should be included in the extension (if available,
     *            <code>false</code> if they should be omitted. <br />
     *            Thumbnails will not be generated if they are not already present, even if <code>true</code> is
     *            passed.
     * @throws java.io.IOException
     * @return the number of index entries included in this extension
     */
    public int getIndexExtensionDataForBuffer(DataOutputStream buffer, IndexEntry startEntry, int endTimeMS,
            boolean includeThumbs) throws IOException {

        // write extensions
        int entryCount = 0;
        IndexEntry entry = startEntry;
        int timeOffset = -startEntry.getTimestamp();
        while (entry != null && entry.getTimestamp() < endTimeMS) {
            // write entry
            entry.writeEntryForExtension(buffer, timeOffset, includeThumbs);
            entry = entry.nextEntry;
            entryCount++;
        }
        buffer.flush();

        // no. of entries being output
        return entryCount;
    }

    /**
     * Write all the <code>Message</code>s contained in all the <code>IndexEntry</code>s to the given
     * <code>DataOutputStream</code> <br />
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the <code>Message</code>s should be written
     * @throws java.io.IOException
     */
    public void writeAllMessages(DataOutputStream out) throws IOException {
        writeAllMessages(out, 0);
    }

    // add an offset - useful when concatenating
    /**
     * Write all the <code>Message</code>s contained in all the <code>IndexEntry</code>s to the given
     * <code>DataOutputStream</code>, adjusting their timestamps with the given offset. <br />
     * The offset is useful when concatenating files. <br />
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the <code>Message</code>s should be written
     * @param timestampOffset
     *            the value that should be added to each message timestamp which is written
     * @throws java.io.IOException
     */
    public void writeAllMessages(DataOutputStream out, int timestampOffset) throws IOException {
        writeMessages(out, firstEntry, Integer.MAX_VALUE, timestampOffset);
    }

    // writes messages beginning at start entry, ending at particular time
    // advisable to begin with start entry since this ensures index extension correct
    /**
     * Write all <code>Message</code>s beginning with the timestamp of the specified <code>IndexEntry</code> and
     * ending with the specified end time to the given <code>DataOutputStream</code>
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the <code>Message</code>s should be written <br />
     * @param startEntry
     *            the <code>IndexEntry</code> at which to begin writing <code>Message</code>s
     * @param endTimeMS
     *            the timestamp of the last <code>Message</code> to be written
     * @throws java.io.IOException
     */
    public void writeMessages(DataOutputStream out, IndexEntry startEntry, int endTimeMS) throws IOException {
        writeMessages(out, startEntry, endTimeMS, 0);
    }

    /**
     * Write all <code>Message</code>s beginning with the timestamp of the specified <code>IndexEntry</code> and
     * ending with the specified end time to the given <code>DataOutputStream</code>, adjusting their timestamps with
     * the given offset. <br />
     * Note that the timestamps will already be independently adjusted if the first is not 0: the adjustment specified
     * is applied after this. <br />
     * This makes it possible to append these <code>Message</code>s to a file which is already being written, without
     * any large gaps in time. <br />
     * 
     * @param out
     *            the <code>DataOutputStream</code> to which the <code>Message</code>s should be written
     * @param startEntry
     *            the <code>IndexEntry</code> at which to begin writing <code>Message</code>s
     * @param endTimeMS
     *            the timestamp of the last <code>Message</code> to be written
     * @param timestampOffset
     *            the value that should be added to each message timestamp which is written
     * @throws java.io.IOException
     */
    public void writeMessages(DataOutputStream out, IndexEntry startEntry, int endTimeMS, int timestampOffset)
            throws IOException {
        IndexEntry.counter = 0;
        IndexEntry entry = startEntry;

        if (!startEntry.equals(firstEntry)) {
            startEntry.writeInitializationMessages(out, timestampOffset);
            timestampOffset -= startEntry.getTimestamp();
        }
        while (entry != null && entry.getTimestamp() <= endTimeMS) {
            int number = entry.getNumber();
            System.out.println("Writing index: " + number);
            // write entry
            entry.writeMessages(out, endTimeMS, timestampOffset);

            entry = entry.nextEntry;
        }

        System.out.println("No. of written messages: " + IndexEntry.counter);
    }

}