package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


/**
 * Small option panel with options to insert, edit and delete indexes.
 * Only defines the layout and design - action listeners must be
 * added to the buttons to provide functionality.
 */
public class IndexOptionPanel extends JPanel {

    /**
     * Button for inserting an index entry.
     */
    protected JButton insertButton;

    /**
     * Button for deleting an index entry.
     */
    protected JButton deleteButton;

    /**
     * Button for editing an index entry.
     */
    protected JButton editButton;
    
    //optional icons - but they aren't very nice
//    private final URL urlAdd = this.getClass().getResource("resources/index16.gif");
//    private final URL urlDelete = this.getClass().getResource("resources/index_delete16.gif");
//    private final URL urlEdit = this.getClass().getResource("resources/index_edit16.gif");
    
    
    /**
     * Class contructor.
     */
    public IndexOptionPanel() {
        super(new BorderLayout());
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
        insertButton = new JButton("Insert");
        insertButton.setToolTipText("Insert an index entry at current playback point");
        deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete the currently selected index entry");
        editButton = new JButton("Edit");
        editButton.setToolTipText("Edit the currently selected index entry");
        
        buttonPanel.add(insertButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);
        
        JLabel titleLabel = new JLabel("Index Options:");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }
    
    /**
     * Method to optionally enable buttons to be disabled depending upon playback status.
     * @param playing <code>true</code> if currently playing, <code>false</code> otherwise
     */
    public void setEnabledByPlaying(boolean playing) {
        insertButton.setEnabled(!playing);
        deleteButton.setEnabled(!playing);
    }
        
}
