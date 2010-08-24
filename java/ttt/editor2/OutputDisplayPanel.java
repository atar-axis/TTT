/*
 * OutputDisplayPanel.java
 *
 * Created on 18 September 2005, 21:12
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 *
 *
 * Code is an edited version of that posted at http://www.rgagnon.com/javadetails/java-0435.html,
 * which is by Real Gagnon and William Denniss.
 *
 */

package ttt.editor2;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

/**
 * A <code>JPanel</code> for displaying text from <code>System.out</code> and <code>System.err</code>
 */
public class OutputDisplayPanel extends JPanel {

    private Vector<JTextArea> textAreas = new Vector<JTextArea>();

    private PrintStream printStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()));

    /**
     * Class constructor
     */
    public OutputDisplayPanel() {
        super(new BorderLayout());

        // modified 19.01.2006 by Peter Ziewer
        if (!editor2.consoleOutput) {
            System.setOut(printStream);
            System.setErr(printStream);
        }

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        addTextArea(textArea);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Add a <code>JTextArea</code> to the list contained within this <code>OutputDisplayArea</code>. All
     * <code>JTextArea</code>s in the list will have any text from the standard output streams appended to them.
     * 
     * @param textArea
     *            the <code>JTextArea</code> to be added
     */
    public void addTextArea(JTextArea textArea) {
        textAreas.add(textArea);
    }

    /**
     * Remove a <code>JTextArea</code> from the list held by the <code>OutputDisplayPanel</code.
     * @param textArea the <code>JTextArea</code> to be removed
     * from the list of <code>JTextArea</code>s
     * contained in the <code>OutputDisplayPanel</code>
     */
    public void removeTextArea(JTextArea textArea) {
        textAreas.remove(textArea);
    }

    class FilteredStream extends FilterOutputStream {
        // ADDED 24.10.2007 by Ziewer
        // hack for doubled output (console and log-window)
        private PrintStream originalOut = System.out;
        // end ADDED 24.10.2007 by Ziewer
        
        public FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        public void write(byte b[]) throws IOException {
            // ADDED 24.10.2007 by Ziewer
            originalOut.write(b);
            // end ADDED 24.10.2007 by Ziewer
            
            String aString = new String(b);
            for (int i = 0; i < textAreas.size(); i++) {
                JTextArea textArea = textAreas.get(i);
                textArea.append(aString);
                // if in a scroll pane, causes scrolling to newly entered text
                textArea.setCaretPosition(textArea.getText().length() - 1);
            }
        }

        public void write(byte b[], int off, int len) throws IOException {
            // ADDED 24.10.2007 by Ziewer
            originalOut.write(b, off, len);
            // end ADDED 24.10.2007 by Ziewer
            
            String aString = new String(b, off, len);
            for (int i = 0; i < textAreas.size(); i++) {
                JTextArea textArea = textAreas.get(i);
                textArea.append(aString);
                // if in a scroll pane, causes scrolling to newly entered text
                textArea.setCaretPosition(textArea.getText().length() - 1);
            }

            /*
             * if (logFile) { FileWriter writer = new FileWriter(fileName, true); writer.write(aString); writer.close(); }
             */
        }
    }

}
