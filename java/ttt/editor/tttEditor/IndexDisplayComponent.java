package ttt.editor.tttEditor;


import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Creates a <code>JComponent</code> which can display an <code>Index</code>
 * and has an area at the top either to create thumbnails / show annotations
 * on already generated thumbnails (an 'annotation' display), or to enter
 * search text (a 'search' display).
 */
public class IndexDisplayComponent extends JPanel
        implements ActionListener, CaretListener, IndexListener, ChangeListener,
                        ListSelectionListener, PlaybackEventListener {
    
    private PlaybackController playbackController;
    private Index index;
    
    //list for displaying indexes
    private JList list;
    private DefaultListModel listModel;
    
    //for displaying index options
    private IndexOptionPanel indexOptionPanel;
    
    /**
     * Int used to indicate that the created <code>IndexDisplayComponent</code>
     * should accept search input.
     */
    public static final int SEARCH = 0;
    /**
     * Int used to indicate that the created <code>IndexDisplayComponent</code>
     * should allow the showing of annotations.
     */
    public static final int ANNOTATION = 1;
    
    /**
     * Class constructor.  Creates an <code>IndexDisplayComponent</code> which
     * can show annotations.
     * @param ind the <code>Index</code> containing the data to be displayed.
     * @param playbackControl the <code>PlaybackController</code> with which to interact.
     */
    public IndexDisplayComponent(Index ind, PlaybackController playbackControl) {
        this(ind, playbackControl, ANNOTATION);
    }
    
    /**
     * Class constructor.
     * @param componentType the type of <code>IndexDisplayComponent</code> which should be created.
     * Valid arguments are <code>IndexDisplayComponent.SEARCH</code>
     * and <code>IndexDisplayComponent.ANNOTATION</code>.
     * @param ind the <code>Index</code> containing the data to be displayed.
     * @param playbackControl the <code>PlaybackController</code> with which to interact.
     */
    public IndexDisplayComponent(Index ind, PlaybackController playbackControl, int componentType) {
        super(new BorderLayout());
        
        this.index = ind;
        this.playbackController = playbackControl;
        
        playbackController.addPlaybackEventListener(this);
        
        listModel = new DefaultListModel();
        IndexEntry [] indexEntries = index.getArray();
        for (int i = 0; i < indexEntries.length; i++)
            listModel.addElement(indexEntries[i]);
        list = new JList(listModel);
        if (index.thumbnailsAvailable())
            list.setCellRenderer(new ThumbnailCellRenderer());
        else
            list.setCellRenderer(new PlainCellRenderer());
        list.addListSelectionListener(this);
        
        JScrollPane indexScrollPane = new JScrollPane();
        indexScrollPane.setViewportView(list);  
        
        indexOptionPanel = new IndexOptionPanel();
        indexOptionPanel.deleteButton.addActionListener(this);
        indexOptionPanel.editButton.addActionListener(this);
        indexOptionPanel.insertButton.addActionListener(this);
        
        if (componentType == SEARCH) {
            showAnnotations = false;
            add(getSearchPanel(), BorderLayout.NORTH);
        }
        else
            add(getAnnotationPanel(), BorderLayout.NORTH);
        
        add(indexScrollPane, BorderLayout.CENTER);
        add(indexOptionPanel, BorderLayout.SOUTH);
        
        index.addIndexListener(this);
    }
    
    
    /*
     * Annotation display
     **************************************/
    
    private JPopupMenu popupMenu;
    private boolean showAnnotations = true;
    private boolean showHighlights = true;
    private JPanel topPanel;
    private JButton annotationButton;
    private JButton highlightButton;
    
    private Component getAnnotationPanel() {
        topPanel = new JPanel(new BorderLayout());
        popupMenu = new JPopupMenu();
        annotationButton = new JButton();
        annotationButton.addActionListener(this);
        createHighlightPopup();
        highlightButton = new JButton("?");
        highlightButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        if (index.thumbnailsAvailable()) {
            annotationButton.setText("Hide Annotations");
            topPanel.add(highlightButton, BorderLayout.EAST);
        } else
            annotationButton.setText("Generate Thumbnails");
        topPanel.add(annotationButton, BorderLayout.CENTER);
        return topPanel;
    }
    
    private void createHighlightPopup() {
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem showButton = new JRadioButtonMenuItem("Show Highlights");
        JRadioButtonMenuItem hideButton = new JRadioButtonMenuItem("Hide Highlights");
        buttonGroup.add(showButton);
        buttonGroup.add(hideButton);
        showButton.setSelected(true);
        showButton.addChangeListener(this);
        popupMenu.add(showButton);
        popupMenu.add(hideButton);
    }
    
    public void stateChanged(ChangeEvent e) {
        showHighlights = ((JRadioButtonMenuItem)e.getSource()).isSelected();
        list.repaint();
    }
    
    private void annotationButtonPressed() {
        String buttonText = annotationButton.getText();
        if (buttonText.equals("Show Annotations")) {
            showAnnotations = true;
            annotationButton.setText("Hide Annotations");
            list.repaint();
            return;
        }
        if (buttonText.equals("Hide Annotations")) {
            showAnnotations = false;
            annotationButton.setText("Show Annotations");
            list.repaint();
            return;
        }
        if (buttonText.equals("Generate Thumbnails")) {
            annotationButton.setText("Hide Annotations");
            playbackController.createThumbnails();
            annotationButton.setEnabled(false);
        }
    }
    
    
    /*
     * Search display
     **************************************/
    
    //for searching
    private JTextField searchField;
    private String recentSearchText = "";
    
    private Component getSearchPanel() {
        searchField = new JTextField();
        searchField.setToolTipText("Enter search string");
        searchField.addCaretListener(this);
        
        JLabel searchLabel = new JLabel("Search: ");
        
        JPanel searchInputPanel = new JPanel();
        searchInputPanel.setLayout(new BorderLayout());
        searchInputPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        searchInputPanel.add(searchLabel, BorderLayout.WEST);
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        return searchInputPanel;
    }
    
    /**
     * Updates the search list, if appropriate, in response to search text as it is entered
     * by the user.
     */
    public void caretUpdate(CaretEvent event) {
        String searchText = searchField.getText();
        if (searchText.equals(recentSearchText))
            return;
        recentSearchText = searchText;
        
        //perhaps not exactly the most efficient approach...
        listModel.clear();
        IndexEntry entry = index.get(0);
        while (entry != null) {
            if (entry.search(searchText))
                listModel.addElement(entry);
            entry = entry.nextEntry;
        }
    }
    
    
    /*
     * Playback / index adjustment methods
     **************************************/
    
    //used to distinguish between the selected index being changed programmatically
    //and it being changed by the user
    private boolean automaticallyAdjustingIndex = false;
    private boolean userChangedIndex = false;
    
    public synchronized void setIndex(int newIndex) {
        if (userChangedIndex)
            return;
        //don't try to set the active index if searching
        if (searchField != null && !searchField.getText().equals("")) {
            return;
        }
        //set the active index
        if (newIndex != list.getSelectedIndex()) {
            automaticallyAdjustingIndex = true;
            list.setSelectedIndex(newIndex);
            list.ensureIndexIsVisible(newIndex);
            repaint();
            automaticallyAdjustingIndex = false;
        }
    }
    
    public void indexEntryRemoved(int index) {
        String text = null;
        if (searchField != null) {
            text = searchField.getText();
            searchField.setText("");
        }
        
        int oldSelectedIndex = list.getSelectedIndex();
        listModel.remove(index);
        setIndex(oldSelectedIndex);
        
        if (searchField != null && text != null && text.length() > 0)
            searchField.setText(text);
    }
    

    public void indexEntryAdded(int index, IndexEntry entry) {
        String text = null;
        if (searchField != null) {
            text = searchField.getText();
            searchField.setText("");
        }
        
        
        listModel.add(index, entry);
        setIndex(index);
        
        if (searchField != null && text != null && text.length() > 0)
            searchField.setText(text);
    }
    
    public void setPlayStatus(boolean playing) {}
    
    
    public void thumbnailsGenerated() {
        list.setCellRenderer(new ThumbnailCellRenderer());
        list.ensureIndexIsVisible(list.getSelectedIndex());
        if (annotationButton != null) {
            annotationButton.setEnabled(true);
            topPanel.add(highlightButton, BorderLayout.EAST);
            topPanel.validate();
        }
    }
    
    public void setTime(int newMediaTimeMS) {}
    
    
    /*
     * Cell renderers
     **************************************/
    
    class ThumbnailCellRenderer implements ListCellRenderer {
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Object object = list.getModel().getElementAt(index);
            IndexEntry entry;
            
            if (object instanceof IndexEntry)
                entry = (IndexEntry)object;
            else
                return null;
            
            JPanel panel = new JPanel();
            if (isSelected)
                panel.setBackground(new Color(200, 200, 200));
            else
                panel.setBackground(new Color(240, 240, 240));
            panel.setToolTipText("Index " + entry.getNumber() + ": " + entry.getTimeStampAsString());
            panel.setBorder(new TitledBorder(new EtchedBorder(), "Slide: " + entry.getNumber()));
            
            JLabel label = new JLabel();
            
            ImageIcon icon = entry.getThumbnail(showAnnotations, showHighlights);
            if (icon != null) {
                label.setIcon(icon);
            } else
                label.setText("Index: " + TTTEditor.getStringFromTime(entry.getTimestamp(),true));
            
            panel.add(label);
            return panel;
        }
    }
    
    
    class PlainCellRenderer implements ListCellRenderer {
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Object object = list.getModel().getElementAt(index);
            IndexEntry entry;
            
            if (object instanceof IndexEntry)
                entry = (IndexEntry)object;
            else
                return null;
            
            JPanel panel = new JPanel();
            if (isSelected)
                panel.setBackground(new Color(200, 200, 200));
            else
                panel.setBackground(new Color(240, 240, 240));
            panel.setToolTipText("Index " + entry.getNumber() + ": " + entry.getTimeStampAsString());
            panel.setBorder(new TitledBorder(new EtchedBorder(), "Slide: " + entry.getNumber()));
            
            JLabel label = new JLabel();
            
            label.setText("Index: " + TTTEditor.getStringFromTime(entry.getTimestamp(),true));
            
            panel.add(label);
            return panel;
        }
    }
    
    
    /*
     * Action listener methods
     **************************************/
    
    public void valueChanged(ListSelectionEvent event) {
        if (!automaticallyAdjustingIndex) {
            userChangedIndex = true;
            playbackController.setIndex((IndexEntry)list.getSelectedValue());
            userChangedIndex = false;
        }
    }
    
    
    /*
     * Action listener methods
     **************************************/
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == annotationButton) {
            annotationButtonPressed();
            return;
        }
        if (e.getSource() == indexOptionPanel.deleteButton) {
            deleteButtonPressed();
            return;
        }
        if (e.getSource() == indexOptionPanel.editButton) {
            editButtonPressed();
            return;
        }
        if (e.getSource() == indexOptionPanel.insertButton) {
            IndexEntry previewEntry = index.createPreviewIndexEntry(playbackController.getCurrentMediaTimeMS());
            if (previewEntry == null) {
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                        "Unable to create an index at the current playback time:\nAn index is already there.", "Index insertion error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Object previewPanel = previewEntry.getPreviewPanel();
            int selection = JOptionPane.showInternalConfirmDialog(TTTEditor.getInstance().getDesktopPane(), 
                    previewPanel, "Index entry preview", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (selection == JOptionPane.OK_OPTION) {
                ((IndexEntry.IndexEntryComponent)previewPanel).commitChanges();
                playbackController.insertIndex(previewEntry);
            }
        }
    }
    
    private void editButtonPressed() {
        if (list.getSelectedIndex() < 0) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "No index selected!", "Index edit error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object value = list.getSelectedValue();
        if (value instanceof IndexEntry) {
            IndexEntry entry = (IndexEntry)value;
            entry.displayEditPane();
        }
    }
    
    private void deleteButtonPressed() {
        if (list.getSelectedIndex() < 0) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "No index selected!", "Index delete error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int option =
                JOptionPane.showInternalConfirmDialog(
                TTTEditor.getInstance().getDesktopPane(),
                "Are you sure you wish to remove this index?\nThis request cannot be undone.",
                "Delete index", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.NO_OPTION)
            return;
        playbackController.removeIndex((IndexEntry)list.getSelectedValue());
    }
    
}