package ttt.editor.tttEditor;

import javax.swing.JInternalFrame;
import javax.media.Player;
import javax.media.MediaLocator;
import javax.media.Manager;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.io.File;

/**
 * A JInternalFrame for displaying a movie file.
 */
public class VideoViewer extends JInternalFrame {
    
    private Player player = null;
    private FileConnector fileConnector = null;

    /**
     * Class constructor.
     * @param fileConnector the <code>FileConnector</code> which requires this <code>VideoViewer</code>
     * @param videoFile the video file to be opened
     */
    public VideoViewer(File videoFile, FileConnector fileConnector) {
       super("Video", false, false, false, true);
        this.fileConnector = fileConnector;
        
        try {
                MediaLocator mediaLocator = new MediaLocator(videoFile.toURI().toURL());
                
                
              player = Manager.createRealizedPlayer(mediaLocator);
               
        }
        catch (Exception e) {      
            System.err.println("Exception: " + e);
            return;
        }        
        updateVideoPanel();
    }

        
    
    /**
     * Gets the <code>Player</code>
     * @return  the <code>Player</code> responsible for the video playback.
     */
    public Player getPlayer() {
        return player;
    }
    
    
    /**
     * Get the <code>FileConnector</code> which this <code>VideoViewer</code> is
     * a part of.
     * @return the <code>FileConnector</code> responsible for creating this
     * <code>VideoViewer</code>
     */
    public FileConnector getFileConnector() {
        return fileConnector;
    }
    
    
    //update the display with the video content
    private void updateVideoPanel() {
        Component visualComponent = player.getVisualComponent();
        if (visualComponent != null) {
            getContentPane().add(visualComponent, BorderLayout.CENTER);
            Dimension preferredSize = visualComponent.getPreferredSize();
            setSize(preferredSize);
        }
    }
    
}