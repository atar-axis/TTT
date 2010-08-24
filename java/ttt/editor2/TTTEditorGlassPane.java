/*
 * TTTEditorGlassPane.java
 *
 * Created on 26 September 2005, 11:47
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package ttt.editor2;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * A <code>JPanel</code> which may be used as a glass pane,
 * so that <code>JInternalFrame</code>s may be displayed and
 * block any clicks which try to access other frames until the
 * selected frame itself is dismissed.
 * @author Peter Bankhead
 */
public class TTTEditorGlassPane extends JPanel {
    
    //used to contain any internal frames which are added
    //so that they can be centered
    private JPanel panel;
    
    /**
     * Class constructor.
     */
    public TTTEditorGlassPane() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setOpaque(false);
        setVisible(false);
        
        panel = new JPanel();
        panel.setOpaque(false);
        add(Box.createGlue());
        add(panel);
        add(Box.createGlue());
        
        // Associate dummy mouse listeners
        // Otherwise mouse events pass through
        MouseInputAdapter adapter = new MouseInputAdapter(){};
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        
    }
    
    
    /**
     * Adds a <code>JInternalFrame</code> to the
     * glass pane.  Also creates a listener for the
     * internal frame, so that the glass pane will
     * make itself invisible after the frame is closed.
     * @param internalFrame the internal frame to add
     */
    public void add(final JInternalFrame internalFrame) {
        internalFrame.setSize(internalFrame.getPreferredSize());
        panel.add(internalFrame);
        internalFrame.setVisible(true);
        //make invisible when internal frame is closed
        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                // ADDED 24.10.2007 by Ziewer
                // since Java 6 the components are no longer removed automatically, which causes the Editor to block
                panel.remove(internalFrame);
                // end ADDED 24.10.2007
                tryToMakeInvisible();
            }
        });
        setVisible(true);
    }
    
    private void tryToMakeInvisible() {
        if (panel.getComponentCount() < 1)
            setVisible(false);
    }
    
}
