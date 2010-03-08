package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;


/**
 * Dialog containing help files for the TTTEditor.
 */
public class HelpPanel extends JPanel implements HyperlinkListener, ActionListener {
    
    /**
     * Link to the index help page
     */
    public final static String INDEX = "doc/index.html";
    /**
     * Link to the options help page
     */
    public final static String OPTIONS = "doc/options.html";
    
    //references
    public final static String OPTIONS_TICKS = "ticks";
    public final static String OPTIONS_MARKERS = "markers";
    public final static String OPTIONS_INDEXES = "indexes";
    public final static String OPTIONS_SYNCH = "synch";
    public final static String OPTIONS_BACKUPS = "backups";
    public final static String OPTIONS_EXTENSIONS = "extensions";
    public final static String OPTIONS_THUMBS = "thumbs";
    
        
    private JEditorPane editorPane;
    private final URL urlCopy =  this.getClass().getResource("resources/copy16.gif");
    
    //references to previous pages used for back button
    private Vector<URL> backPages = new Vector<URL>();
    
    private URL currentPage = null;
    
    
    /**
     * Class constructor.
     */
    public HelpPanel() {
        this(INDEX);
    }
    
    private JButton indexButton, copyButton, backButton;
    
    /**
     * Class constructor.
     * @param target target URL, which should be a string defined in the class e.g.
     * <code>HelpDialog.INDEX</code>
     */
    public HelpPanel(String target) {
        super(new BorderLayout());
        
        BorderLayout topLayout = new BorderLayout();
        topLayout.setHgap(8);
        JPanel topPanel = new JPanel(topLayout);
        topPanel.setBorder(new EmptyBorder(4,4,4,4));
        
        backButton = new JButton("Back");
        backButton.setToolTipText("Previous page");
        backButton.addActionListener(this);
        backButton.setEnabled(false);
        topPanel.add(backButton, BorderLayout.WEST);
        
        indexButton = new JButton("Index");
        indexButton.addActionListener(this);
        topPanel.add(indexButton, BorderLayout.CENTER);
        
        ImageIcon copyIcon = new ImageIcon(urlCopy);
        copyButton = new JButton(copyIcon);
        copyButton.setToolTipText("Copy selected text");
        copyButton.addActionListener(this);
        topPanel.add(copyButton, BorderLayout.EAST);
        
        editorPane = new JEditorPane();
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(editorPane);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        
        editorPane.setEditable(false);
        editorPane.addHyperlinkListener(this);
        
        setHelpPage(target);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    
    private boolean movingBack = false;
    
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == indexButton) {
            setHelpPage(INDEX);
            return;
        }
        if (event.getSource() == copyButton) {
            editorPane.copy();
            return;
        }
        if (event.getSource() == backButton) {
            if (backPages.size() > 0) {
                movingBack = true;
                //set to last back page
                URL target = backPages.remove(backPages.size() - 1);
                setHelpPage(target);
                movingBack = false;
            }
            //can't go back further, disable button
            if (backPages.size() < 1)
                backButton.setEnabled(false);
        }
    }
    
    
    /**
     * Set the help page
     * @param target a String, which can be converted to a <code>URL</code>, representing the help page to use
     */
    public void setHelpPage(String target) {
        URL helpURL = this.getClass().getResource(target);
        setHelpPage(helpURL);
    }
    
    
    /**
     * Set the help page
     * @param helpURL the help page to use
     */
    public void setHelpPage(URL helpURL) {
        
        //don't do anything if page already displaying
        if (currentPage != null && helpURL.equals(currentPage))
            return;
        
        try {
            editorPane.setPage(helpURL);
            
            if (currentPage != null) {
                if (!movingBack) {
                    if (backPages.size() == 0 || !currentPage.equals(backPages.lastElement()))
                        backPages.add(currentPage);
                    //enable button
                    if (backPages.size() == 1)
                        backButton.setEnabled(true);
                }
            }
            
            currentPage = helpURL;
            final String reference = helpURL.getRef();
            if (reference != null)
                setReferenceOnPage(reference);
            
        } catch(Exception e) {
            System.err.println("Couldn't create help URL: " + e);
        }
    }
    
    
    /**
     * Set the reference on the current help page to scroll to.
     * @param ref the reference on the current help page
     */
    public void setReferenceOnPage(String ref) {
        if (ref == null)
            return;
        
        final String reference = ref;
        if (reference != null)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    editorPane.scrollToReference(reference);
                }
            });
    }
    
    
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane)event.getSource();
            if (event instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)event;
                HTMLDocument doc = (HTMLDocument)pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
                
            } else {
                setHelpPage(event.getURL());
            }
        }
    }
    
}
