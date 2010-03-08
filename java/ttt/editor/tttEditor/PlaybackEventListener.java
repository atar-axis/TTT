package ttt.editor.tttEditor;

/**
 * The listener interface for receiving notification of a playback change. 
 * The class that is interested in processing in response to a playback change
 * implements this interface, and the object created with that
 * class is registered with a component, using the component's
 * <code>addPlaybackListener</code> method.
 */
public interface PlaybackEventListener {
    
    /**
     * Invoked when playback is either started or stopped.
     * @param playing <code>true</code> if playing, <code>false</code> otherwise
     */
    public void setPlayStatus(boolean playing);
    
    /**
     * Invoked whenever playback shifts to a new <code>IndexEntry</code>
     * @param newIndex the position of the currently active <code>IndexEntry</code>
     * in the <code>Index</code>
     */
    public void setIndex(int newIndex);
    
    /**
     * Invoked whenever the playback time has changed significantly enough
     * to warrent notification.  Typically this may be every second or so,
     * or when the <code>PlaybackController</code> deems it necessary.
     * @param newMediaTimeMS the current playback time in milliseconds
     */
    public void setTime(int newMediaTimeMS);
    
    /**
     * Invoked whenever thumbnails have been generated for the <code>Index</code>.
     */
    public void thumbnailsGenerated();
}
