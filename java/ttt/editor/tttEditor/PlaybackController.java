package ttt.editor.tttEditor;

import javax.media.Player;
import javax.media.GainControl;
import javax.media.Time;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


/**
 * <code>Thread</code> whose primary function is to control
 * the playback of desktop, either alone or along with audio,
 * video or both.
 */
public class PlaybackController extends Thread implements IndexListener {
    
    //boolean variables for current playback status
    private boolean timeSeek = false;
    private boolean indexUpdated = false;
    private boolean playing = false;
    private boolean annotationsUpdated = false;
    private boolean creatingThumbnails = false;
    private boolean addingIndex = false;
    private boolean removingIndex = false;
    
    //current time / active index
    private int currentIndex = 0;
    private int currentMediaTimeMS = 0;
    
    //controller Player if available
    private Player mainPlayer = null;
    
    //timer to be used to control if there is no Player
    private Timer desktopTimer;
    
    //used to measure time between time change events being fired
    //can prevent firing events too often
    private int lastTimeChangeEventTime = 0;
    
    //index from which to get messages
    private Index index = null;
    
    //desktop panel to send messages to for processing
    private DesktopPanel desktopPanel = null;

    //permissable times - actual times are set by user (initially at maximum values)
    //absolute duration is the maximum permissable playback time
    private int actualStartTime = 0;
    private int actualEndTime;
    private int absoluteDuration;
    
    //used for synch
    private Header header;
    
    //use this to send messages to DesktopPanel
    private LinkedList<Message> currentMessageList = new LinkedList<Message>();
    
    //registered event listeners
    private ArrayList<PlaybackEventListener> playbackEventListeners = new ArrayList<PlaybackEventListener>();
    
    /**
     * Class constructor.
     * <br />
     * Calculates playback time using its own timer, and
     * obtains the maximum duration from the <code>TTTFileData</code>
     * @param fileData the <code>TTTFileData</code> object containing
     * the <code>Index</code> and <code>Header</code>
     * required for playback
     * @param desktopPanel the component upon which to display the desktop
     */
    public PlaybackController(TTTFileData fileData, DesktopPanel desktopPanel) {
        this(fileData, desktopPanel, null, fileData.index.getLastMessageTimestamp());
    }
    
    /**
     * Class constructor.
     * @param fileData the <code>TTTFileData</code> object containing
     * the <code>Index</code> and <code>Header</code>
     * required for playback
     * @param desktopPanel the component upon which to display the desktop
     * @param mainPlayer the main media (audio or video) to be used
     * for calculating current playback time
     * @param duration the maximum duration of playback, in milliseconds
     */
    public PlaybackController(TTTFileData fileData, DesktopPanel desktopPanel, Player mainPlayer, int duration) {
        this.index = fileData.index;
        this.header = fileData.header;
        this.desktopPanel = desktopPanel;
        this.mainPlayer = mainPlayer;
        this.absoluteDuration = duration;
        actualEndTime = duration;
        
        internalSyncStatus = Parameters.synchronize;
        
        //if there is no main player, create a timer to use instead
        if (mainPlayer == null) {
            desktopTimer = new Timer(50, null);
            desktopTimer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    incrementMediaTime();
                }
            });
            desktopTimer.start();
        }
        
        //used for initilization - so that first index is shown
        indexUpdated = true;
        
        index.addIndexListener(this);
    }

    //increment the media time - used only if there is no media Player available
    private synchronized void incrementMediaTime() {
        if (playing)
            currentMediaTimeMS = currentMediaTimeMS + 50;
    }

    
    /**
     * Adds a <code>PlaybackEventListener</code> to the list
     * stored internally.
     * @param p the <code>PlaybackEventListener</code> to be added
     */
    public void addPlaybackEventListener(PlaybackEventListener p) {
        playbackEventListeners.add(p);
    }
    
    
    
    /**
     * Notify listeners that current media time has changed.
     * This method is normally called when playback has stopped, or more
     * than a second has passed since the last notification (so that clocks
     * may be displayed with second precision).
     */
    protected void fireTimeChangeEvent() {
        lastTimeChangeEventTime = currentMediaTimeMS;
        for (int count = 0; count < playbackEventListeners.size(); count++)
            playbackEventListeners.get(count).setTime(currentMediaTimeMS);
    }
    
    /**
     * Notify listeners that the currently active index has changed.
     */
    protected void fireIndexChangeEvent() {
        for (int count = 0; count < playbackEventListeners.size(); count++)
            playbackEventListeners.get(count).setIndex(currentIndex);
    }
    
    /**
     * Notify listeners of the current playback status (playing or not).
     */
    protected void firePlaybackStatusEvent() {
        for (int count = 0; count < playbackEventListeners.size(); count++)
            playbackEventListeners.get(count).setPlayStatus(playing);
    }
    
    /**
     * Notify listeners that the thumbnails have been generated.
     */
    protected void fireThumbnailsGeneratedEvent() {
        for (int count = 0; count < playbackEventListeners.size(); count++)
            playbackEventListeners.get(count).thumbnailsGenerated();
    }
    
    
    /**
     * Tells the <code>PlaybackController</code> to stop its
     * run() method, any <code>Timer</code>s and also close
     * any <code>Player</code>s.
     */
    public void terminate() {
        threadRunning = false;
        if (mainPlayer == null) {
            desktopTimer.stop();
            desktopTimer = null;
        }
        else {
            mainPlayer.stop();
            mainPlayer.deallocate();
            mainPlayer.close();
            mainPlayer = null;
        }
        desktopPanel = null;
        index = null;
        tempEntry = null;
        header = null;
    }
    
    
    /**
     * Set the preferred start time, as input by the user.
     * @param startTime the preferred start time
     */
    public synchronized void setActualStartTime(int startTime) {
        actualStartTime = startTime;
        if (currentMediaTimeMS < actualStartTime)
            setTimeAdjusted(actualStartTime);
    }
    
    
    /**
     * Set the preferred end time, as input by the user.
     * @param endTime the preferred end time
     */
    public synchronized void setActualEndTime(int endTime) {
        /*
        if (mainPlayer != null)
            actualEndTime = Math.min(endTime, (int)(mainPlayer.getDuration().getSeconds() * 1000));
        else
         */
        actualEndTime = endTime;
        if (currentMediaTimeMS > actualEndTime)
            setTimeAdjusted(actualEndTime);
    }
    
    
    /**
     * Get the <code>GainControl</code> if this controller uses a <code>Player</code> for playback,
     * and that player has a gain control.
     * @return the <code>GainControl</code> if available
     */
    public GainControl getGainControls() {
        if (mainPlayer != null)
            return mainPlayer.getGainControl();
        else
            return null;
    }
    
    
    /**
     * Set the preferred end time, as input by the user.
     * @return the preferred end time
     */
    public int getActualEndTime() {
        return actualEndTime;
    }
    
    /**
     * Set the preferred start time, as input by the user.
     * @return the preferred start time
     */
    public int getActualStartTime() {
        return actualStartTime;
    }
    
    
    
    /**
     * Get the maximum possible playback time.  This is
     * the longest time out of the lengths of desktop and video,
     * unless synchronization is used, in which case it will
     * be the maximum synchronized time.
     * @return the duration in milliseconds
     */
    public int getMaxPossibleDurationMS() {
        if (Parameters.synchronize)
            return (int)(absoluteDuration * header.synchRatio);
        else
            return absoluteDuration;
    }
    
    
    
    /*
     * Method for playing / pausing
     **********************************************************************/    
    
    //used to record current synch status, as far as the playback controller is aware
    //means that when updateSynchStatus is used, no changes need to be made if the new synch
    //status if no different from the one currently stored here
    private boolean internalSyncStatus = false;
    
    /**
     * Inform the <code>PlaybackController</code> that the
     * synchronization status may have changed.
     */
    protected synchronized void updateSyncStatus() {
        if (Parameters.synchronize == internalSyncStatus)
            return;
        internalSyncStatus = Parameters.synchronize;
        int timeMS;
        if (internalSyncStatus)
            timeMS = (int)(currentMediaTimeMS * header.synchRatio);
        else
            timeMS = (int)(currentMediaTimeMS / header.synchRatio);
        setTimeAdjusted(timeMS);
    }
    
    /**
     * If playing, set pause playback.  Otherwise start playback.
     */
    public synchronized void togglePlaying() {
        if (playing)
            setPaused();
        else
            setPlaying();
    }
    
    /**
     * Start playback.
     */
    public synchronized void setPlaying() {
        if (!playing) {
            if (mainPlayer != null)
                mainPlayer.start();
            playing = true;
            firePlaybackStatusEvent();
        }
    }

    /**
     * Pause playback.
     */
    public synchronized void setPaused() {
        if (playing) {
            if (mainPlayer != null)
                mainPlayer.stop();
            playing = false;
            firePlaybackStatusEvent();
            //ensures the most up-to-date time is available during pausing,
            //with millisecond accuracy,
            //as time change events are usually only posted every second
            fireTimeChangeEvent();
        }
    }
    
    /**
     * Stop playback, and reset the time to the beginning.
     * The start time is dictated by the position of the trim markers.
     */
    public synchronized void stopPlayer() {
        setPaused();
        setTimeAdjusted(actualStartTime);
    }
    
        
    /*
     * Methods used for obtaining correct message lists for playback
     **********************************************************************/    
    
    /**
     * Set the now playing index to the next index.
     * If the current index is the final index, nothing
     * happens.
     */
    public synchronized void incrementIndex() {
        //if adjustment comes after an unprocessed random seek, discard seek
        if (timeSeek)
            timeSeek = false;
        
        currentIndex++;
        if (currentIndex >= index.size())
            currentIndex = index.size() - 1;
        else {
            setIndex(currentIndex);
        }
    }
    
    /**
     * Set the now playing index to the previous index.
     * If the current index is the first index, nothing
     * happens.
     */
    public synchronized void decrementIndex() {
        //if adjustment comes after an unprocessed random seek, discard seek
        if (timeSeek)
            timeSeek = false;
        
        currentIndex--;
        if (currentIndex < 0)
            currentIndex = 0;
        else {
            setIndex(currentIndex);
        }
    }
    
    /**
     * Set the current index
     * @param newIndex the new index
     */
    public synchronized void setIndex(int newIndex) {
        //check range
        if (newIndex > index.size() || newIndex < 0)
            return;
        
        //reset pointer in current index
        index.get(currentIndex).resetMessagePointer();
        
        //if adjustment comes after an unprocessed random seek, discard seek
        if (timeSeek)
            timeSeek = false;
        
        //update variables
        currentIndex = newIndex;
        currentMediaTimeMS = index.get(newIndex).getTimestamp();
        if (mainPlayer != null) {
            double seconds = (double)currentMediaTimeMS / 1000;
            mainPlayer.setMediaTime(new Time(seconds));
        }
        indexUpdated = true;
    }
    
    
    /**
     * Set the current index
     * @param newIndex the new index entry
     */
    public synchronized void setIndex(IndexEntry newIndex) {
        if (newIndex == null)
            return;
        setIndex(newIndex.getNumber() - 1);
    }
    
    
    /**
     * set the current playback time
     * @param mediaTimeMS the new playback time
     */
    public synchronized void setTimeAdjusted(int mediaTimeMS) {
    //if adjustment comes after an unprocessed index update, discard index update
        if (indexUpdated)
            indexUpdated = false;
       
        index.get(currentIndex).resetMessagePointer();
        
        currentMediaTimeMS = mediaTimeMS;
        int newIndex = index.getIndexFromTime(currentMediaTimeMS);
        if (newIndex != currentIndex) {
            currentIndex = newIndex;
            fireIndexChangeEvent();
        }
        
        if (mainPlayer != null) {
            double seconds = (double)currentMediaTimeMS / 1000;
            mainPlayer.setMediaTime(new Time(seconds));
        }
        timeSeek = true;
    }
    
    
    
    private synchronized void getPlaybackMessages() {
        if (indexUpdated) {
            desktopPanel.resetAnnotationList();
            desktopPanel.resetBlankPage();
            indexUpdated = false;
            currentMessageList.clear();
            index.get(currentIndex).getInitialIndexMessages(currentMessageList);
            return;
        }
        if (timeSeek) {
            desktopPanel.resetAnnotationList();
            desktopPanel.resetBlankPage();
            timeSeek = false;
            index.get(currentIndex).getSeekingMessages(currentMediaTimeMS, currentMessageList);
            return;
        }
        if (annotationsUpdated) {
            desktopPanel.resetAnnotationList();
            index.get(currentIndex).resetMessagePointer();
            LinkedList<Message> newAnnotations = new LinkedList<Message>();
            index.get(currentIndex).getSeekingMessages(currentMediaTimeMS, newAnnotations);
            while (newAnnotations.size() > 0) {
                Message msg = newAnnotations.remove();
                if (msg instanceof AnnotationMessage)
                    currentMessageList.add(msg);
            }
            annotationsUpdated = false;
            return;
        }
        
        int newIndex = index.getIndexFromTime(currentMediaTimeMS);
        if (currentIndex != newIndex) {
            currentIndex = newIndex;
            fireIndexChangeEvent();
        }
        index.get(currentIndex).getPlayingMessages(currentMediaTimeMS, currentMessageList);
    }

    
    
    private synchronized void updateMessageList() {
        if (indexUpdated || timeSeek) {
            if (indexUpdated)
                fireIndexChangeEvent();
            fireTimeChangeEvent();
            currentMessageList.clear();
            getPlaybackMessages();
        } else if (playing || annotationsUpdated) {
            if (mainPlayer != null)
                currentMediaTimeMS = (int)(mainPlayer.getMediaTime().getSeconds() * 1000);
            getPlaybackMessages();
            
            //fire a time change event approx. every second, or when time changes drastically
            if (Math.abs(currentMediaTimeMS - lastTimeChangeEventTime) > 900)
                fireTimeChangeEvent();
        }
    }
    
    /**
     * Request that thumbnails be created for the index.
     * Playback may be paused until this is complete.
     * (This method only sets a flag which should be picked up in the run
     * method for further processing.)
     */
    public synchronized void createThumbnails() {
        creatingThumbnails = true;
    }
    
    /**
     * Request that an index entry be added.
     * Playback may be paused until this is complete.
     * (This method only sets a flag which should be picked up in the run
     * method for further processing.)
     * @param entry the index entry to be added
     */
    public synchronized void insertIndex(IndexEntry entry) {
        addingIndex = true;
        tempEntry = entry;
    }
    
    //an index entry either to be added or removed
    private IndexEntry tempEntry = null;
    
    /**
     * Request that an index entry be removed.
     * Playback may be paused until this is complete.
     * (This method only sets a flag which should be picked up in the run
     * method for further processing.)
     * @param entry the entry to be removed.
     */
    public synchronized void removeIndex(IndexEntry entry) {
        removingIndex = true;
        tempEntry = entry;
    }
    
    private synchronized void processRemoveIndexEntry() {
        removingIndex = false;
        boolean wasPlaying = playing;
        if (wasPlaying)
            setPaused();
        
        index.removeIndex(tempEntry);
        tempEntry = null;
        
        if (wasPlaying)
            setPlaying();
    }
    
    private synchronized void processAddIndexEntry() {
        addingIndex = false;
        boolean wasPlaying = playing;
        if (wasPlaying)
            setPaused();
        index.insertIndex(tempEntry);
        tempEntry = null;
        
        if (wasPlaying)
            setPlaying();
    }
    
    
    private void checkTimeLegal() {
        if (actualStartTime == actualEndTime) {
            stopPlayer();
            return;
        }
        if (currentMediaTimeMS < actualStartTime)
            setTimeAdjusted(actualStartTime);
        if (currentMediaTimeMS >= actualEndTime) {
            stopPlayer();
            return;
        }
        if (mainPlayer != null && mainPlayer.getDuration().getSeconds() <= mainPlayer.getMediaTime().getSeconds())
            stopPlayer();
    }
    
    
    private boolean threadRunning = true;

    /**
     * Run method for the thread.
     */
    public void run() {
        processMessages:
            while (threadRunning) {
                if (creatingThumbnails) {
                    if (!playing)
                        index.createAllThumbnails();
                    else {
                        setPaused();
                        index.createAllThumbnails();
                        setPlaying();
                    }
                    creatingThumbnails = false;
                    fireThumbnailsGeneratedEvent();
                }
                
                if (addingIndex)
                    processAddIndexEntry();
                if(removingIndex)
                    processRemoveIndexEntry();
                
                
                if (playing || indexUpdated || timeSeek || annotationsUpdated) {
                    
                    //if trying to play before the chosen start time, skip forward to start time
                    if (playing)
                        checkTimeLegal();
                    
                    updateMessageList();
                    
                    if (currentMessageList.size() > 0) {
                        while (currentMessageList.size() > 0) {
                            Message msg = currentMessageList.remove();
                            desktopPanel.processMessage(msg);
                            if (indexUpdated || timeSeek)
                                continue processMessages;
                        }
                        //performs full screen repaint, though only after a batch of messages are processed
                        //(avoids showing messages too soon, which will later be covered by others)
                        desktopPanel.repaint();
                    }
                }
                try {
                    sleep(50);
                } catch(InterruptedException e) {
                    System.out.println("Thread interrupted: " + e.toString());
                }
            }
    }
    
    
    //doesn't matter if removed index is after the current index
    public void indexEntryRemoved(int index) {
        if (currentIndex >= index) {
            currentIndex--;
            fireIndexChangeEvent();
        }
        //to be safe, treat as seeking next time
        setTimeAdjusted(currentMediaTimeMS);

    }

    //doesn't matter if added index is after the current index
    public void indexEntryAdded(int index, IndexEntry entry) {
        if (currentIndex > index || this.index.get(currentIndex).getTimestamp() > entry.getTimestamp()) {
            currentIndex++;
            fireIndexChangeEvent();
        }
        //to be safe, treat as seeking next time
        setTimeAdjusted(currentMediaTimeMS);

    }
        
    /**
     * Gets the current playback time.
     * @return the current playback time in milliseconds
     */
    public int getCurrentMediaTimeMS() {
        return currentMediaTimeMS;
    }
    
    /**
     * Request that the <code>DesktopPanel</code> displays
     * a crosshair on screen for the point touched by
     * a REMOVE message.
     * This will only occus if the <code>PlaybackController</code>
     * is not busy processing messages or playing, as the crosshair
     * only appears until the next repaint() is called on the
     * desktopPanel.
     * @param message the remove message
     */
    public void requestDrawRemoveCrosshairs(AnnotationMessage message) {
        if (currentMessageList.size() == 0 && !playing)
            desktopPanel.drawRemoveCrosshairs(message);        
    }
    
    /**
     * Called after an annotation is removed so that the
     * annotations currently displayed are updated if
     * necessary.
     * @param message the annotation that was removed
     */
    public synchronized void processRemovedAnnotation(AnnotationMessage message) {
        //no need to do anything if annotation cannot yet have been displayed
        if (message.getTimestamp() > currentMediaTimeMS)
            return;
        //if is a remove all, will need to get totally new annotation list
        //to be certain it is accurate - therefore get full update and add only annotations to
        //current list to be processed and set to desktopPanel
        if (message.isRemove() || message.isRemoveAll())
            annotationsUpdated = true;
        //otherwise can just remove the annotation from the screen
        //(if it is there)
        else
            desktopPanel.removeAnnotationFromScreen(message);
    }
    
}