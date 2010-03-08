package ttt.editor.tttEditor;


import java.util.Vector;

import javax.swing.AbstractListModel;


/**
 * A <code>ListModel</code> to hold <code>Marker<code>s.
 */
public class MarkerList extends AbstractListModel {
    
    /**
     * the amount of space required between markers in ms -
     * if the markers are any closer, they cannot be processed
     */
    public final int MARKER_SPACE_REQUIRED = 5000;
    
    /**
     * Marker type
     */
    public final static int START_MARKER = 0;
    /**
     * Marker type
     */
    public final static int END_MARKER = 1;
    /**
     * Marker type
     */
    public final static int DIVIDE_MARKER = 2;
    
    //vector for containing the markers
    private Vector<Marker> vector;
    
    //used for synch
    private Header header;
    
    //playback is only allowed to the max time set and starts
    //at min time set
    private PlaybackController playbackController;
    
    /**
     * Class constructor.
     * @param playbackController the <code>PlaybackController</code> to interact with.
     * @param header the <code>Header</code> object containing the required synchronization ratio.
     */
    public MarkerList(PlaybackController playbackController, Header header) {
        this.header = header;
        this.playbackController = playbackController;
        
        vector = new Vector<Marker>(2);
        vector.add(new Marker(START_MARKER, 0));
        fireIntervalAdded(this, 0, 0);
        vector.add(new Marker(END_MARKER, playbackController.getMaxPossibleDurationMS()));
        fireIntervalAdded(this, 1, 1);
    }
    
    
    
    /**
     * Get only the markers which fall between the start and end times, including the
     * start and end markers themselves.
     * @return a <code>Vector</code> of legal markers.
     */
    public Vector<Marker> getLegalMarkers() {
        Vector<Marker> legal = new Vector<Marker>();
        int start = -1;
        int end = -1;
        for (int i = 0; i < vector.size(); i++) {
            Marker marker = vector.get(i);
            if (marker.getMarkerType() == START_MARKER)
                start = i;
            else if (marker.getMarkerType() == END_MARKER)
                end = i;
        }
        if (start < 0 || end < 0 || start > end)
            return legal;
        legal.addAll(vector.subList(start, end + 1));
        return legal;
    }
    
    
    /**
     * Tests whether trim start and end are too close together to be properly processed, or it
     * processing is possible.
     * @return <code>true</code> is the markers are too close to be processed, <code>false</code> otherwise.
     */
    public boolean trimsTooClose() {
        if (getTrimEndTime() - getTrimStartTime() < MARKER_SPACE_REQUIRED)
            return true;
        else
            return false;
    }
    
    /**
     * Tests whether markers are too close together to be properly processed, or it
     * processing is possible.
     * @return <code>true</code> is the markers are too close to be processed, <code>false</code> otherwise.
     */
    public boolean markersTooClose() {
        for (int i = 1; i < vector.size(); i++) {
            if (vector.get(i).getTimestamp() - vector.get(i-1).getTimestamp() < MARKER_SPACE_REQUIRED)
                return true;
        }
        return false;
    }
    
    
    //test whether the whole file is selected
    /**
     * Get whether the start and end markers are at their maximum values, or
     * whether they are marking areas as needing trimmed.
     * @return <code>true</code> if there are areas marked to be trimmed, <code>false</code> otherwise
     */
    public boolean isUntrimmed() {
        int start = getTrimStartTime();
        if (start > 0)
            return false;
        int end = getTrimEndTime();
        if (end < playbackController.getMaxPossibleDurationMS())
            return false;
        return true;
        
    }
    
    
    /**
     * Get subdivision markers have been set.
     * @return <code>true</code> if there are subdivision markers present and within range, <code>false</code> otherwise
     */
    public boolean subdivisionsPresent() {
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i).getMarkerType() == DIVIDE_MARKER)
                if (vector.get(i).isMarkerLegal())
                    return true;
        }
        return false;
    }
    

    /**
     * Get the timestamp of the start marker
     * @return the trim start time
     */
    public int getTrimEndTime() {
        for (int i = 0; i < vector.size(); i++) {
            Marker marker = vector.get(i);
            if (marker.getMarkerType() == END_MARKER)
                return marker.getTimestamp();
        }
        return -1;
    }
    

    /**
     * Get the timestamp of the end marker
     * @return the trim end time
     */
    public int getTrimStartTime() {
        for (int i = 0; i < vector.size(); i++) {
        Marker marker = vector.get(i);
            if (marker.getMarkerType() == START_MARKER)
                return marker.getTimestamp();
        }
        return -1;
    }
    
    
    /**
     * Add a division marker
     * @param timestamp the time (in ms) at which the division should be added
     * @return <code>true</code> if the division is added, <code>false</code> otherwise.
     * If a marker already exists at the given time, none will be added.
     */
    public boolean addDivision(int timestamp) {
        return add(DIVIDE_MARKER, timestamp);
    }
    
        
    
    /**
     * Remove a specified marker.
     * @param index the position of the marker in the list
     * @return <code>true</code> if the marker is removed successfully, <code>false</code> otherwise.
     * If the index is out of range, or referred to a start or end marker, none
     * will be removed.
     */
    public boolean remove(int index) {
        if (index < 0 || index > vector.size() - 1)
            return false;
        
        //do not remove start or end markers
        if (vector.get(index).getMarkerType() == START_MARKER ||
                vector.get(index).getMarkerType() == END_MARKER)
            return false;
        
        vector.remove(index);
        fireIntervalRemoved(this, index, index);
        return true;
    }
    
    
    /**
     * Remove a specified marker.
     * @param marker the marker to be removed
     * @return <code>true</code> if the marker is removed successfully, <code>false</code> otherwise.
     * If the index is out of range, or referred to a start or end marker, none
     * will be removed.
     */
    public boolean remove(Marker marker) {
        //do not remove start or end markers
        if (marker.getMarkerType() == START_MARKER ||
                marker.getMarkerType() == END_MARKER)
            return false;
        
        for (int i = 0; i < vector.size(); i++) {
            if (marker.equals(vector.get(i))) {
                vector.remove(i);
                fireIntervalRemoved(this, i, i);
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Add a marker
     * @param markerType the marker type to be added
     * @param timestamp the time (in ms) at which to add the marker
     * @return <code>true</code> if the division is added, <code>false</code> otherwise.
     * If a marker already exists at the given time, none will be added.
     */
    public boolean add(int markerType, int timestamp) {
        if (markerType == START_MARKER)
            return setTrimStartTime(timestamp);
        if (markerType == END_MARKER)
            return setTrimStartTime(timestamp);
        
        Marker marker = new Marker(markerType, timestamp);
        
        for (int i = 0; i < vector.size(); i++) {
            //marker already present - don't add
            if (timestamp == vector.get(i).getTimestamp())
                return false;
            //add marker
            if (timestamp < vector.get(i).getTimestamp()) {
                vector.insertElementAt(marker, i);
                fireIntervalAdded(this, i, i);
                return true;
            }
        }
        vector.add(marker);
        fireIntervalAdded(this, vector.size() - 1, vector.size() - 1);
        return true;
    }
    
    
    /**
     * Set the trim start time.  Moves the position of the current start marker.
     * Also adjusts the position of the current end marker, if it would otherwise
     * be before the position of the new start marker.
     * @param trimStartMS the new trim start time
     * @return <code>true</code> if the time is successfully set
     */
    public boolean setTrimStartTime(int trimStartMS) {
        for (int i = 0; i < vector.size(); i++) {
            //remove old start marker
            if (vector.get(i).getMarkerType() == START_MARKER) {
                //do nothing - start already set
                if (vector.get(i).getTimestamp() == trimStartMS)
                    return true;
                vector.remove(i);
                fireIntervalRemoved(this, i, i);
            }
        }
        
        playbackController.setActualStartTime(trimStartMS);
        boolean insertEnd = false;
        
        for (int i = 0; i < vector.size(); i++) {
            //marker already present - don't add
            if (trimStartMS == vector.get(i).getTimestamp()) {
                if (vector.get(i).getMarkerType() == END_MARKER) {
                    vector.insertElementAt(new Marker(START_MARKER, trimStartMS), i);
                    fireIntervalAdded(this, i, i);
                    return true;
                }
                else {
                    vector.get(i).setMarkerType(START_MARKER);
                    fireContentsChanged(this, i, i);
                    if (insertEnd) {
                        vector.insertElementAt(new Marker(END_MARKER, trimStartMS), i + 1);
                        fireIntervalAdded(this, i+1, i+1);
                    }
                    return true;
                }
            }
            //add marker
            if (trimStartMS < vector.get(i).getTimestamp()) {
                vector.insertElementAt(new Marker(START_MARKER, trimStartMS), i);
                fireIntervalAdded(this, i, i);
                if (insertEnd) {
                    vector.insertElementAt(new Marker(END_MARKER, trimStartMS), i + 1);
                    fireIntervalAdded(this, i+1, i+1);
                }
                return true;
            }
            if (vector.get(i).getMarkerType() == END_MARKER) {
                vector.remove(i);
                fireIntervalRemoved(this, i, i);
                insertEnd = true;
                i--;
            }
        }
        vector.add(new Marker(START_MARKER, trimStartMS));
        fireIntervalAdded(this, vector.size() - 1, vector.size() - 1);
        vector.add(new Marker(END_MARKER, trimStartMS));
        fireIntervalAdded(this, vector.size() - 1, vector.size() - 1);
        
        return true;
    }
    
    
    
    /**
     * Set the trim end time.  Moves the position of the current end marker.
     * Also adjusts the position of the current start marker, if it would otherwise
     * be after the position of the new end marker.
     * @param trimEndMS the new trim end time
     * @return <code>true</code> if the time is successfully set
     */
    public boolean setTrimEnd(int trimEndMS) {
        for (int i = vector.size() - 1; i >= 0; i--) {
            //remove old start marker
            if (vector.get(i).getMarkerType() == END_MARKER) {
                //do nothing - start already set
                if (vector.get(i).getTimestamp() == trimEndMS)
                    return true;
                vector.remove(i);
                fireIntervalRemoved(this, i, i);
            }
        }
        
        playbackController.setActualEndTime(trimEndMS);
        boolean insertStart = false;
        
        for (int i = vector.size() - 1; i >= 0; i--) {
            //marker already present - don't add
            if (trimEndMS == vector.get(i).getTimestamp()) {
                if (vector.get(i).getMarkerType() == START_MARKER) {
                    vector.insertElementAt(new Marker(END_MARKER, trimEndMS), i + 1);
                    fireIntervalAdded(this, i + 1, i + 1);
                    return true;
                }
                else {
                    vector.get(i).setMarkerType(END_MARKER);
                    fireContentsChanged(this, i, i);
                    if (insertStart) {
                        vector.insertElementAt(new Marker(START_MARKER, trimEndMS), i);
                        fireIntervalAdded(this, i, i);
                    }
                    return true;
                }
            }
            //add marker
            if (trimEndMS > vector.get(i).getTimestamp()) {
                vector.insertElementAt(new Marker(END_MARKER, trimEndMS), i + 1);
                fireIntervalAdded(this, i + 1, i + 1);
                if (insertStart) {
                    vector.insertElementAt(new Marker(START_MARKER, trimEndMS), i + 1);
                    fireIntervalAdded(this, i + 1, i + 1);
                }
                return true;
            }
            if (vector.get(i).getMarkerType() == START_MARKER) {
                vector.remove(i);
                fireIntervalRemoved(this, i, i);
                insertStart = true;
            }
        }
        vector.add(0, new Marker(END_MARKER, trimEndMS));
        fireIntervalAdded(this, 0, 0);
        vector.add(0, new Marker(START_MARKER, trimEndMS));
        fireIntervalAdded(this, 0, 0);
        
        return true;
    }
    

    public Marker getElementAt(int index) {
        return vector.get(index);
    }
    
    
    public int getSize() {
        return vector.size();
    }
    
    private void fireMarkerAdjustedEvent(Marker marker) {
        int index = vector.indexOf(marker);
        fireContentsChanged(this, index, index);
    }
    
    class Marker {
        
        private int markerType;
        private int timestamp;
        
        Marker(int markerType, int timestamp) {
            this.markerType = markerType;
            
            if (Parameters.synchronize)
                this.timestamp = (int)(timestamp / header.synchRatio + 0.5);
            else
                this.timestamp = timestamp;
        }
        
        boolean isMarkerLegal() {
            if (markerType == START_MARKER || markerType == END_MARKER)
                return true;
            
            return (getTimestamp() > getTrimStartTime() &&
                    getTimestamp() < getTrimEndTime());
        }
        
        int getTimestamp() {
            if (Parameters.synchronize)
                return (int)(timestamp * header.synchRatio);
            else
                return timestamp;
        }
        
        int getMarkerType() {
            return markerType;
        }
        
        void setTimestamp(int timestamp) {
            if (Parameters.synchronize)
                this.timestamp = (int)(timestamp / header.synchRatio + 0.5);
            else
                this.timestamp = timestamp;
            fireMarkerAdjustedEvent(this);
        }
        
        void setMarkerType(int markerType) {
            this.markerType = markerType;
            fireMarkerAdjustedEvent(this);
        }
        
    }
    
}