package ttt.editor.tttEditor;

import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;

/**
 * <code>InternalFrameListener</code> for <code>DesktopViewer</code>s, which
 * helps in tracking which desktop viewer is selected by the user, and also
 * controls the display of associated <code>VideoViewer</code>s upon
 * iconifying and de-iconifying.
 */
public class DesktopViewerListener implements InternalFrameListener {
    
    private TTTEditor editor;
    
    //used when focus is on no frame but frame-dependent option selected
    //- helps choose suitable frame
    private DesktopViewer lastActiveViewer = null;
    
    
    public DesktopViewerListener(TTTEditor editor) {
        this.editor = editor;
    }
    
    /**
     * Get the last active <code>DesktopViewer</code> if possible to determine,
     * and if the viewer has not subsequently been closed.
     * @return the last active viewer, if it can be determined, otherwise <code>null</code>
     */
    protected DesktopViewer getLastActiveViewer() {
        return lastActiveViewer;
    }
    
    /**
     * Calls the <code>closeFile</code> method of <code>TTTEditor</code> on the
     * selected file, so as to allow the opportunity for other processing to be
     * done before (possibly) closing.
     */
    public void internalFrameClosing(InternalFrameEvent e) {
        DesktopViewer viewer = (DesktopViewer)e.getInternalFrame();
        editor.closeFile(viewer.getFileConnector());
    }
    
    
    /**
     * If closed frame was last active one, reset last active to null
     */
    public void internalFrameClosed(InternalFrameEvent e) {
        if (lastActiveViewer != null && lastActiveViewer.equals(e.getInternalFrame()))
            lastActiveViewer = null;
    }
    
    public void internalFrameOpened(InternalFrameEvent e) {}
    
    /**
     * Make the associated <code>VideoViewer</code> invisible, if available.
     */
    public void internalFrameIconified(InternalFrameEvent e) {
      //  FileConnector connector = ((DesktopViewer)e.getInternalFrame()).getFileConnector();
        //VideoViewer video = connector.getVideoViewer();
        /*if (video != null)
            video.setVisible(false);*/
    }
    
    /**
     * Make the associated <code>VideoViewer</code> visible, if available.
     */
    public void internalFrameDeiconified(InternalFrameEvent e) {
      //  FileConnector connector = ((DesktopViewer)e.getInternalFrame()).getFileConnector();
       // VideoViewer video = connector.getVideoViewer();
      /*  if (video != null)
            video.setVisible(true);*/
    }
    
    /**
     * Set as the last active viewer, bring <code>VideoViewer</code> to the front
     * in its layer.
     */
    public void internalFrameActivated(InternalFrameEvent e) {
        lastActiveViewer = (DesktopViewer)e.getInternalFrame();
      //  FileConnector connector = 
        //        lastActiveViewer.getFileConnector();
      //  VideoViewer video = connector.getVideoViewer();
     /*   if (video != null)
            editor.getDesktopPane().setPosition(video, 0);*/
    }
    
    public void internalFrameDeactivated(InternalFrameEvent e) {}
    
}
