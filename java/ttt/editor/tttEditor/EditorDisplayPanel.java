package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;



/**
 * A panel which can create and display a variety of other components used
 * for viewing indexes or markers, set out internally on a <code>JTabbedPane</code>.
 * The other components displayed are two <code>IndexDisplayComponent</code>s (one for search
 * and one for thumbnail / annotation display), an <code>IndexDetailDisplayComponent</code>
 * and a <code>MarkerDisplayComponent</code>.
 */
public class EditorDisplayPanel extends JPanel {
    
    /**
     * Class contructor.
     * @param index the <code>Index</code> of the current file
     * @param markers the <code>MarkerList</code> used by the current file
     * @param playbackController the <code>PlaybackController</code> used currently
     */
    public EditorDisplayPanel(Index index, MarkerList markers, PlaybackController playbackController) {
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        IndexDisplayComponent thumbnailDisplay =
                new IndexDisplayComponent(index, playbackController);
        tabbedPane.add("Thumbs", thumbnailDisplay);
        tabbedPane.setToolTipTextAt(0, "View thumbnails for each index");
        
        IndexDisplayComponent searchDisplay = 
                new IndexDisplayComponent(index, playbackController,
                IndexDisplayComponent.SEARCH);
        tabbedPane.add("Search", searchDisplay);
        tabbedPane.setToolTipTextAt(1, "View/edit search text for each index");
        
        IndexDetailDisplayComponent detailDisplay = 
                new IndexDetailDisplayComponent(index, playbackController);
        tabbedPane.add("Detail", detailDisplay);
        tabbedPane.setToolTipTextAt(2, "View/delete annotations and index divisions");
        
        MarkerDisplayComponent markerDisplay = 
                new MarkerDisplayComponent(markers, playbackController);
        tabbedPane.add("Marker", markerDisplay);
        tabbedPane.setToolTipTextAt(3, "View/edit file processing divisions");
        tabbedPane.setBackgroundAt(3, Color.RED);
        
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        
    }
    
}