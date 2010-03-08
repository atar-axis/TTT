package ttt.editor.tttEditor;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;



/**
 * a <code>JPanel</code> which allows the user to view details for an
 * <code>Index</code>, including search text, annotations and timestamps.
 * Annotations can also be deleted.
 */
public class IndexDetailDisplayComponent extends JPanel implements MouseListener,
        IndexListener, PlaybackEventListener, TreeSelectionListener, ActionListener {
    
    private Index index;
    private PlaybackController playbackController;
    
    //for displaying indexes and annotations
    private JTree tree;
    
    //for index editing options
    private IndexOptionPanel indexOptionPanel;
    
    //for displaying search text
    private JLabel searchTextLabel;
    private JTextArea searchTextArea;
    private final String noText = "No index entry currently selected.";
    
    /**
     * Class constructor.
     * @param ind the <code>Index</code> containing the data to be displayed.
     * @param playbackControl the <code>PlaybackController</code> with which to interact.
     */
    public IndexDetailDisplayComponent(Index ind, PlaybackController playbackControl) {
        super(new BorderLayout());
        
        this.index = ind;
        this.playbackController = playbackControl;
        index.addIndexListener(this);
        playbackController.addPlaybackEventListener(this);
        
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Index");
        for (int count = 0; count < index.size(); count++) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(index.get(count));
            top.add(treeNode);
            LinkedList<AnnotationMessage> annotations = index.get(count).getAnnotations();
            while(annotations.size() > 0) {
                DefaultMutableTreeNode annotation = new DefaultMutableTreeNode(annotations.remove());
                treeNode.add(annotation);
            }
        }
        
        tree = new JTree(top);
        //1 click selects, 2 clicks moves time - so expanding a node should take 3 clicks
        //(or user can simply click the little handle once)
        tree.setToggleClickCount(3);
        
        tree.addMouseListener(this);
        tree.addTreeSelectionListener(this);
        
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        createPopupMenus();
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tree, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.setPreferredSize(new Dimension(150,150));
        upperPanel.add(scrollPane, BorderLayout.CENTER);
        
        //area displaying the search text for the selected index entry
        searchTextArea = new JTextArea();
        searchTextArea.setLineWrap(true);
        searchTextArea.setWrapStyleWord(true);
        searchTextArea.setToolTipText("Searchable text for the selected slide");
        searchTextArea.setEditable(false);
        JScrollPane searchTextScrollPane = new JScrollPane(searchTextArea);
        
        //label to say which index is selected
        searchTextLabel = new JLabel(noText);
        
        JPanel lowerPanel = new JPanel(new BorderLayout());
        lowerPanel.add(searchTextScrollPane, BorderLayout.CENTER);
        lowerPanel.add(searchTextLabel, BorderLayout.NORTH);
        lowerPanel.setPreferredSize(new Dimension(200, 200));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(upperPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setBottomComponent(lowerPanel);
        
        indexOptionPanel = new IndexOptionPanel();
        indexOptionPanel.deleteButton.addActionListener(this);
        indexOptionPanel.editButton.addActionListener(this);
        indexOptionPanel.insertButton.addActionListener(this);
        
        add(splitPane, BorderLayout.CENTER);
        add(indexOptionPanel, BorderLayout.SOUTH);
    }
    
    
    private JPopupMenu annotationPopup = new JPopupMenu();
    private JMenuItem removeAnnotation = new JMenuItem("Remove Annotation");
    private JMenuItem pointAnnotation = new JMenuItem("Show point on screen");
    
    private JPopupMenu indexPopup = new JPopupMenu();
    private JMenuItem removeIndex = new JMenuItem("Remove Index Division");
    private JMenuItem editIndex = new JMenuItem("Edit Index");
    
    private void createPopupMenus() {
        annotationPopup.add(removeAnnotation);
        removeAnnotation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int option =
                        JOptionPane.showInternalConfirmDialog(
                        TTTEditor.getInstance().getDesktopPane(),
                        "Are you sure you wish to remove this annotation?\nThis request cannot be undone.",
                        "Delete annotation", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.NO_OPTION)
                    return;
                AnnotationMessage message = (AnnotationMessage)lastSelectedNode.getUserObject();
                index.removeAnnotation(message);
                playbackController.processRemovedAnnotation(message);
                
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode)lastSelectedNode.getParent();
                parent.remove(lastSelectedNode);
                lastSelectedNode = null;
                model.reload(parent);
            }
        });
        
        annotationPopup.add(pointAnnotation);
        pointAnnotation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                AnnotationMessage message = (AnnotationMessage)lastSelectedNode.getUserObject();
                playbackController.requestDrawRemoveCrosshairs(message);
            }
        });
        
        
        indexPopup.add(removeIndex);
        removeIndex.addActionListener(this);
        
        indexPopup.add(editIndex);
        
        editIndex.addActionListener(this);
    }
    
    
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == removeIndex || source == indexOptionPanel.deleteButton) {
            deleteIndex();
            return;
        }
        if (source == editIndex || source == indexOptionPanel.editButton) {
            editIndex();
            return;
        }
        if (source == indexOptionPanel.insertButton) {
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
    
    private void editIndex() {
        Object object;
        if (lastSelectedNode == null ||
                (object = lastSelectedNode.getUserObject()) == null) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "No index selected!", "Index edit error", JOptionPane.ERROR_MESSAGE);
                    return;
        }
        IndexEntry entry;
        if (object instanceof IndexEntry)
            entry = (IndexEntry)object;
        else {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)lastSelectedNode.getParent();
            entry = (IndexEntry)parent.getUserObject();
        }
        entry.displayEditPane();
        searchTextArea.setText(entry.getSearchableText());
    }
    
    private void deleteIndex() {
        Object object;
        if (lastSelectedNode == null ||
                (object = lastSelectedNode.getUserObject()) == null) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "No index selected!", "Index delete error", JOptionPane.ERROR_MESSAGE);
                    return;
        }
        IndexEntry entry;
        if (object instanceof IndexEntry)
            entry = (IndexEntry)object;
        else {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)lastSelectedNode.getParent();
            entry = (IndexEntry)parent.getUserObject();
        }
        int option =
                JOptionPane.showInternalConfirmDialog(
                TTTEditor.getInstance().getDesktopPane(),
                "Are you sure you wish to remove index " + entry.getNumber() +
                "?\nThis request cannot be undone.",
                "Delete index", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.NO_OPTION)
            return;
        playbackController.removeIndex(entry);
    }
    
    
    public void valueChanged(TreeSelectionEvent event) {
        TreePath path = event.getNewLeadSelectionPath();
        if (path == null) {
            lastSelectedNode = null;
            searchTextArea.setText("");
            searchTextLabel.setText(noText);
            return;
        }
        lastSelectedNode = (DefaultMutableTreeNode)path.getLastPathComponent();

        Object object = lastSelectedNode.getUserObject();
        //search up path until IndexEntry found
        while (!(object instanceof IndexEntry)) {
            path = path.getParentPath();
            object = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
        }
        IndexEntry entry = (IndexEntry)object;
        searchTextArea.setText(entry.getSearchableText());
        searchTextLabel.setText("Searchable Text: Index entry " + entry.getNumber());
    }
    
    
    
    /**
     * Sets the playback position to the timestamp of the selected object
     * if it has been clicked more than once (as single clicking only selects
     * the element, and the user may not wish to jump to that time).
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1)
            setPlaybackPosition(e);
    }
    
    /**
     * Triggers the popup menu for the selected object if available, and if
     * the mouse press is the appropriate popup trigger for the current platform.
     */
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
    }
    
    
    private void setPlaybackPosition(MouseEvent e) {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)(selPath.getLastPathComponent());
        Object object = node.getUserObject();
        
        if (object instanceof IndexEntry) {
            int index = ((IndexEntry)object).getNumber() - 1;
            playbackController.setIndex(index);
        } else {
            int mediaTimeMS = ((Message)object).getTimestamp();
            playbackController.setTimeAdjusted(mediaTimeMS);
        }
    }
    
        
    /**
     * Triggers the popup menu for the selected object if available, and if
     * the mouse release is the appropriate popup trigger for the current platform.
     */
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            showPopup(e);
    }
    
    
    private DefaultMutableTreeNode lastSelectedNode = null;
    
    private void showPopup(MouseEvent e) {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null)
            return;
        tree.setSelectionPath(selPath);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)(selPath.getLastPathComponent());
        lastSelectedNode = node;
        Object object = node.getUserObject();
        
        if (object instanceof IndexEntry)
            indexPopup.show(e.getComponent(), e.getX(), e.getY());
        else {
            if (((AnnotationMessage)object).isRemove()) {
                //enable it if should be enabled: a remove annotation and other options enabled
                if (removeAnnotation.isEnabled())
                    pointAnnotation.setEnabled(true);
            } else
                pointAnnotation.setEnabled(false);
            annotationPopup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    
    /**
     * Does nothing.
     */
    public void mouseEntered(MouseEvent e) {}
    
    /**
     * Does nothing.
     */
    public void mouseExited(MouseEvent e) {}
    
    
    
    public void setIndex(int newIndex) {
        if (!isVisible()) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
            DefaultMutableTreeNode indexNode = (DefaultMutableTreeNode)root.getChildAt(newIndex);
            TreePath path = new TreePath(indexNode.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }
    
    public void indexEntryRemoved(int index) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
        DefaultMutableTreeNode removedNode = (DefaultMutableTreeNode)root.getChildAt(index);
        DefaultMutableTreeNode previousNode = (DefaultMutableTreeNode)root.getChildAt(index - 1);
        
        while (removedNode.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)removedNode.getFirstChild();
            removedNode.remove(0);
            previousNode.add(child);
        }
        root.remove(removedNode);
        ((DefaultTreeModel)tree.getModel()).reload();
    }
    
    
    public void indexEntryAdded(int index, IndexEntry entry) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
        root.removeAllChildren();
        for (int count = 0; count < this.index.size(); count++) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(this.index.get(count));
            root.add(treeNode);
            LinkedList<AnnotationMessage> annotations = this.index.get(count).getAnnotations();
            while(annotations.size() > 0) {
                DefaultMutableTreeNode annotation = new DefaultMutableTreeNode(annotations.remove());
                treeNode.add(annotation);
            }
        }
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        model.reload();
        
        
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode)model.getChild(model.getRoot(), entry.getNumber() - 1);
        TreePath path = new TreePath(newNode.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        
        /*
         * [This code may have been more efficient, had it worked]
         *
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(entry);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        DefaultMutableTreeNode previousNode = (DefaultMutableTreeNode)root.getChildAt(index - 1);
        root.insert(newNode, index);
        
        for (int count = 0; count < previousNode.getChildCount(); count++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)previousNode.getChildAt(count);
            AnnotationMessage ann = (AnnotationMessage)child.getUserObject();
            if (ann.getTimestamp() > entry.getTimestamp()) {
                previousNode.remove(count);
                newNode.add(child);
            }
        }
        model.reload();
        tree.setSelectionPath(new TreePath(newNode.getPath()));
         */
    }
    
    
    public void setPlayStatus(boolean playing) {
        removeAnnotation.setEnabled(!playing);
        pointAnnotation.setEnabled(!playing);
    }
    
    public void thumbnailsGenerated() {}
    
    public void setTime(int newMediaTimeMS) {}
    
}
