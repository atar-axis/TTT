package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A <code>JPanel</code> containing formatted options relating
 * to playback, display and saving with the TTTEditor.
 * @author Peter Bankhead
 */
public class OptionPanel extends JPanel implements ChangeListener, MouseListener, ActionListener {
    
    JCheckBox synchCheck;
    JCheckBox extensionCheck, thumbCheck, backupCheck;
    JCheckBox tickCheck, markerCheck, indexCheck;
    
    private JPopupMenu popup;
    
    /** Creates a new instance of OptionInternalFrame */
    public OptionPanel() {
        super(new GridLayout(1, 2));
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
                
        Box synchBox = new Box(BoxLayout.Y_AXIS);
        synchBox.setBorder(new CompoundBorder(emptyBorder, 
                new TitledBorder("Synchronization")));
        synchCheck = new JCheckBox("Synchronize timestamps", Parameters.synchronize);
        synchCheck.addMouseListener(this);
        synchBox.add(synchCheck);
        
        Box saveBox = new Box(BoxLayout.Y_AXIS);
        saveBox.setBorder(new CompoundBorder(emptyBorder, 
                new TitledBorder("Save options")));
        backupCheck = new JCheckBox("Save backups", Parameters.createBackups);
        backupCheck.addMouseListener(this);
        extensionCheck = new JCheckBox("Save index extension with desktop", Parameters.saveIndex);
        extensionCheck.addMouseListener(this);
        extensionCheck.addChangeListener(this);
        thumbCheck = new JCheckBox("Save thumbnails with extension", Parameters.saveThumbs);
        thumbCheck.addMouseListener(this);
        thumbCheck.setEnabled(extensionCheck.isSelected());
        saveBox.add(backupCheck);
        saveBox.add(extensionCheck);
        saveBox.add(thumbCheck);
        
        Box displayBox = new Box(BoxLayout.Y_AXIS);
        displayBox.setBorder(new CompoundBorder(emptyBorder, 
                new TitledBorder("Display options")));
        tickCheck = new JCheckBox("Show ticks on timeline", Parameters.displayTicksOnTimeline);
        tickCheck.addMouseListener(this);
        markerCheck = new JCheckBox("Show markers on timeline", Parameters.displayMarkersOnTimeline);
        markerCheck.addMouseListener(this);
        indexCheck = new JCheckBox("Show indexes on timeline", Parameters.displayIndexesOnTimeline);
        indexCheck.addMouseListener(this);
        Component glue = Box.createGlue();
        displayBox.add(glue);
        displayBox.add(tickCheck);
        glue = Box.createGlue();
        displayBox.add(glue);
        displayBox.add(markerCheck);
        glue = Box.createGlue();
        displayBox.add(glue);
        displayBox.add(indexCheck);
        glue = Box.createGlue();
        displayBox.add(glue);
        
        rightPanel.add(synchBox, BorderLayout.NORTH);
        rightPanel.add(saveBox, BorderLayout.SOUTH);
        
        JPanel textPanel = new JPanel();
        textPanel.setBorder(new EmptyBorder(2,2,2,2));
        String text = "Right-click an option for more information.";
        JLabel textLabel = new JLabel(text);
        textPanel.add(textLabel);
        
        leftPanel.add(textPanel, BorderLayout.NORTH);
        leftPanel.add(displayBox, BorderLayout.CENTER);
        
        
        add(leftPanel);
        add(rightPanel);
        
        popup = new JPopupMenu();
        JMenuItem infoItem = new JMenuItem("What's this?");
        popup.add(infoItem);
        infoItem.addActionListener(this);
    }

    
    /**
     * Update the values of associated variables in <code>Parameters</code>
     * to reflect the state of the check boxes in this panel.
     */
    public void commitSelections() {
        Parameters.synchronize = synchCheck.isSelected();
        Parameters.createBackups = backupCheck.isSelected();
        Parameters.saveIndex = extensionCheck.isSelected();
        if (thumbCheck.isEnabled())
            Parameters.saveThumbs = thumbCheck.isSelected();
        else
            Parameters.saveThumbs = false;
        Parameters.displayTicksOnTimeline = tickCheck.isSelected();
        Parameters.displayMarkersOnTimeline = markerCheck.isSelected();
        Parameters.displayIndexesOnTimeline = indexCheck.isSelected();
    }
    
    
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() == extensionCheck) {
            thumbCheck.setEnabled(extensionCheck.isSelected());
        }
    }
    
    
    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            showPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            showPopup(e);
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e){}
    
    

    public void actionPerformed(ActionEvent e) {
        HelpPanel helpPanel = TTTEditor.getInstance().getHelpPanel();
        if (helpPanel == null)
            return;
        helpPanel.setHelpPage(HelpPanel.OPTIONS);
        
        JPopupMenu menu = (JPopupMenu)((JMenuItem)e.getSource()).getParent();
        Component invoker = menu.getInvoker();
        
        if (invoker == tickCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_TICKS);
        else if (invoker == markerCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_MARKERS);
        else if (invoker == indexCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_INDEXES);
        else if (invoker == synchCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_SYNCH);
        else if (invoker == backupCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_BACKUPS);
        else if (invoker == extensionCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_EXTENSIONS);
        else if (invoker == thumbCheck)
            helpPanel.setReferenceOnPage(HelpPanel.OPTIONS_THUMBS);
        
        TTTEditor.getInstance().showHelp();
    }
    
        
    private void showPopup(MouseEvent e) {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }
}