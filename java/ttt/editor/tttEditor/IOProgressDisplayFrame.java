package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * A <code>JInternalFrame</code> which can be used by the TTTEditor in conjunction with an <code>OutputDisplayPanel</code>
 * to display progress output while files are being read or written.
 */
public class IOProgressDisplayFrame extends JInternalFrame implements ActionListener {
    
    private JButton okButton = new JButton("Ok");
    private JTextArea textArea;
    private OutputDisplayPanel outputDisplay;
    
    /**
     * Class constructor.<br>
     * Registering a text area with an <code>OutputDisplay</code>
     * allows the text area to obtain output from the standard
     * output / error streams.
     * @param outputDisplay the <code>OutputDisplayPanel</code> which is being
     * used for outputting from the standard output stream.
     */
    public IOProgressDisplayFrame(OutputDisplayPanel outputDisplay) {
        super("Progress");
        
        this.outputDisplay = outputDisplay;
        
        textArea = new JTextArea(30, 40);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        outputDisplay.addTextArea(textArea);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        okButton.setEnabled(false);
        okButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
    }
    
    /**
     * Notifies the <code>IOProgressDisplayFrame</code> that the
     * associated task is complete, and the OK Button should
     * be enabled.
     */
    public void setCompleted() {
        okButton.setEnabled(true);
    }
    
    public void actionPerformed(ActionEvent event) {
        dispose();
    }
    
    
    /**
     * Overrides the dispose() method of <code>JInternalFrame</code>
     * to de-register the text area with the <code>OutputDisplayPanel</code>
     */
    public void dispose() {
        outputDisplay.removeTextArea(textArea);
        super.dispose();
    }
    
}
