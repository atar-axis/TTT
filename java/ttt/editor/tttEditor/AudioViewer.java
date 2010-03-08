package ttt.editor.tttEditor;

import java.io.File;
import javax.media.Player;
import javax.media.Manager;
import javax.media.MediaLocator;

/**
 * A class containing a <code>Player</code> for audio files.
 */
public class AudioViewer {
    
    private Player player = null;
    
    /**
     * Class constructor.
     * @param  audioFile   the audio file to be opened
     */
    public AudioViewer(File audioFile) {
        try {
                MediaLocator mediaLocator = new MediaLocator(audioFile.toURI().toURL());
                
                player = Manager.createRealizedPlayer(mediaLocator);
                player.getGainControl().setLevel(0.7f);
        }
        catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }
    
    /**
     * Gets the <code>Player</code>
     * @return the <code>Player</code> responsible for the audio playback.
     */
    public Player getPlayer() {
        return player;
    }
    
}
