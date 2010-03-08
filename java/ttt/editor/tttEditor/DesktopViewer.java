package ttt.editor.tttEditor;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import javax.swing.JPanel;



/**
 * A JInternalFrame for displaying desktop content from a ttt file.
 * In addition to displaying the index, options for controlling playback and viewing/editing
 * markers and indexes are provided.
 */
public class DesktopViewer extends JInternalFrame {
    
    private ControlPanel controlPanel;
    
    private FileConnector fileConnector = null;

    /**
     * Class constructor.
     * @param desktopPanel the <code>DesktopPanel</code> upon which to paint the desktop
     * @param fileData the data obtained from reading a TTT file
     * @param playbackController the <code>PlaybackController</code> being used
     * by this <code>FileConnection</code>
     * @param markers the <code>MarkerList</code> to be used for editing
     * @param fileConnector the <code>FileConnector</code> which contains
     * this <code>DesktopViewer</code>
     */
    public DesktopViewer(TTTFileData fileData, PlaybackController playbackController,
            DesktopPanel desktopPanel, MarkerList markers, FileConnector fileConnector) {
        super("Desktop", true, true, true, true);
        this.fileConnector = fileConnector;

        this.addInternalFrameListener(TTTEditor.getDesktopViewerListener());
        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        
        //initialize variables
        controlPanel = new ControlPanel(playbackController, markers, fileData.index, desktopPanel);
        
        //create GUI components for DesktopViewer
        getContentPane().setLayout(new BorderLayout());
        JSplitPane splitPane = createSplitPane(playbackController, desktopPanel, fileData.index, markers);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        
        setTitle(fileData.header.desktopName);
        setMinimumSize(controlPanel.getPreferredSize());
                
        playbackController.start();
    }
    
    
     /**
      * Overrides dispose() method of <code>JInternalFrame</code> to include
      * removal of the volume slider from the screen.
      */    
    public void dispose() {
        controlPanel.removeVolumeSlider();
        super.dispose();
    }
    
    
    
    /**
     * Refresh the display - typically in response to a change in synchronization status.
     * [Changing synchronization may also change the length of playback, which must
     * be reflected in displays such as timelines.]
     */
    protected void refreshSlider() {
        controlPanel.refreshSlider();
    }
    
        
/*
 * Methods for creating the GUI
 **********************************************************************/

    private JSplitPane createSplitPane(PlaybackController playbackController, DesktopPanel desktopPanel,
            Index index, MarkerList markers) {
        JScrollPane desktopScrollPane = new JScrollPane();
        
        //allows the desktop to be centered
        JPanel desktopHolderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        desktopHolderPanel.add(desktopPanel);
        
        desktopScrollPane.setViewportView(desktopHolderPanel);
        desktopScrollPane.getViewport().setBackground(Color.LIGHT_GRAY);
        //prevents the scroll bars being painfully slow
        desktopScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        desktopScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        
        EditorDisplayPanel indexDisplayComponent = new EditorDisplayPanel(index, markers, playbackController);
        indexDisplayComponent.setMinimumSize(new Dimension(200, 300));
        indexDisplayComponent.setPreferredSize(new Dimension(300, 300));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, desktopScrollPane, indexDisplayComponent);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1.0);
                
        //revise preferred size for scroll pane
        desktopScrollPane.setMinimumSize(new Dimension(300, 300));
        desktopPanel.setZoomLevel(Parameters.ZOOM_TO_FIT);
        
        return splitPane;
    }
    
        
        
    /**
     * Get the <code>FileConnector</code> which this is a part of.
     * @return the <code>FileConnector</code> which contains
     * this <code>DesktopViewer</code>
     */
    public FileConnector getFileConnector() {
        return fileConnector;
    }
        
}