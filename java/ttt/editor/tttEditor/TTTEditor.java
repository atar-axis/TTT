package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.media.Manager;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


import ttt.TTT;

/**
 * Main class for TTTEditor.
 */
@SuppressWarnings("serial")
public class TTTEditor extends JFrame {
    static final String version = "22.05.2009";

    // added 19.01.2006 by Peter Ziewer

    // user preferences
    static Preferences userPrefs = Preferences.userRoot().node("/TTTEditor");

    // the JDesktopPane, to which JInternalFrames may be added
    private JDesktopPane desktopPane;

    // the TTTEditorGlassPane, used for adding internal frames
    // that block from clicking other frames
    private TTTEditorGlassPane glassPane;

    // for displaying help (HTML content)
    private HelpPanel helpPanel;

    // scrollpane used to separate help from the rest of the desktop
    private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    // Current instance of the TTTEditor
    private static TTTEditor instance;

    // no more than this number of files can be opened at the same time
    private final int maxFileCount = 3;
    private Vector<FileConnector> fileConnectors = new Vector<FileConnector>(maxFileCount);

    /**
     * The layer of the JDesktopPane to which DesktopViewers are added
     */
    public final int DESKTOP_LAYER = 1;
    /**
     * The layer of the JDesktopPane to which VideoViewers are added
     */
    public final int VIDEO_LAYER = 2;

    // used to request exit if all files can be closed
    private boolean exitWhenAllClosed = false;

    // desktop viewer listener, used for dealing with selections and iconifying/closing frames
    private static DesktopViewerListener desktopViewerListener;

    // used for the side bar, containing the help and log displays
    private JPanel sideBar;
    private JTabbedPane sideTabbedPane;
    private OutputDisplayPanel outputDisplay = new OutputDisplayPanel();
    private final int HELP_TAB_INDEX = 0;
    private final int LOG_TAB_INDEX = 1;

    /**
     * Class Constructor.
     */
    public TTTEditor() {
        super("TTTEditor, Version: " + version);
       
        System.out.println(Parameters.ABOUT);
      
        // create the file chooser at this stage - takes some time
        TTTFileUtilities.createFileChooser();
        
        desktopViewerListener = new DesktopViewerListener(this);

        desktopPane = new JDesktopPane();
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktopPane.setBackground(Color.GRAY);

        glassPane = new TTTEditorGlassPane();
        setGlassPane(glassPane);
     
        sideBar = new JPanel(new BorderLayout());
       
        //helpPanel = new HelpPanel();

		//helpPanel.setMinimumSize(new Dimension(300, 300));
		//helpPanel.setPreferredSize(new Dimension(300, 300));
		//helpPanel.setMaximumSize(new Dimension(300, 300));
       
        sideTabbedPane = new JTabbedPane();
        sideTabbedPane.insertTab("Help", null, helpPanel, "Help info", HELP_TAB_INDEX);
        sideTabbedPane.insertTab("Log", null, outputDisplay, "View log", LOG_TAB_INDEX);

        JButton sideBarCloseButton = new JButton("Close");
        sideBarCloseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                splitPane.setDividerSize(0);
                sideBar.setVisible(false);
            }
        });
        sideBar.add(sideTabbedPane, BorderLayout.CENTER);
        sideBar.add(sideBarCloseButton, BorderLayout.SOUTH);

        splitPane.setLeftComponent(desktopPane);
        splitPane.setRightComponent(sideBar);
        splitPane.setResizeWeight(1);
        splitPane.setDividerSize(0);
        setContentPane(splitPane);
        sideBar.setVisible(false);
       
        createMenuBar();

        // allows movies to be rendered in swing containers
        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));

        // removed 19.01.2006 by Peter Ziewer
        // maximaize after setVisible instead
        //
        // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // screenSize.width -= 100;
        // screenSize.height -= 150;
        // setSize(screenSize);

        // attempt to close all open files before closing
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                tryExit();
            }
        });
        setVisible(true);
        // added 19.01.2006 by Peter Ziewer
        setExtendedState(MAXIMIZED_BOTH);
    }

    // Menu bar setup
    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu("File");
    private JMenu detailsMenu = new JMenu("Details");
    private JMenu optionMenu = new JMenu("Options");
    private JMenu helpMenu = new JMenu("Help");
    private JMenuItem openItem = new JMenuItem("Open");
    private JMenuItem saveAsItem = new JMenuItem("Save As...");
    private JMenuItem saveItem = new JMenuItem("Save");
    private JMenuItem divideItem = new JMenuItem("Save & sub-divide");
    private JMenuItem concatItem = new JMenuItem("Concatenate");
    private JMenuItem closeItem = new JMenuItem("Close");
    private JMenuItem exitItem = new JMenuItem("Exit");
    private JMenuItem detailsItem = new JMenuItem("Show file details");
    private JMenuItem titleItem = new JMenuItem("Set desktop title");
    private JMenuItem optionItem = new JMenuItem("Show options");
    private JMenuItem helpItem = new JMenuItem("Help");
    private JMenuItem aboutItem = new JMenuItem("About");

    private JMenuItem outputItem = new JMenuItem("Display log");

    // creates the menu bar, and adds it to the JFrame
    private void createMenuBar() {
        fileMenu.setMnemonic(KeyEvent.VK_F);
        detailsMenu.setMnemonic(KeyEvent.VK_D);
        optionMenu.setMnemonic(KeyEvent.VK_O);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        // open a file
        /*
         * URL urlOpen = this.getClass().getResource("resources/open16.gif"); ImageIcon openIcon = new
         * ImageIcon(urlOpen); openItem.setIcon(openIcon);
         */
        openItem.setMnemonic(KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open();
            }
        });
        fileMenu.add(openItem);

        // save currently selected file with current name
        /*
         * URL urlSave = this.getClass().getResource("resources/save16.gif"); ImageIcon saveIcon = new
         * ImageIcon(urlSave); saveItem.setIcon(saveIcon);
         */
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveActiveFile();
            }
        });
        saveItem.setEnabled(false);
        fileMenu.add(saveItem);

        // save currently selected file with a new name
        saveAsItem.setMnemonic(KeyEvent.VK_A);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        saveAsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveActiveFileAs(true, false);
            }
        });
        saveAsItem.setEnabled(false);
        fileMenu.add(saveAsItem);

        // divide 1 file into several
        divideItem.setMnemonic(KeyEvent.VK_D);
        divideItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        divideItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveActiveFileAs(true, true);
            }
        });
        divideItem.setEnabled(false);
        fileMenu.add(divideItem);

        // join 2 or more files together
        concatItem.setMnemonic(KeyEvent.VK_N);
        concatItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        concatItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // don't allow concat if files are already open
                if (fileConnectors.size() > 0)
                    JOptionPane.showInternalMessageDialog(desktopPane,
                            "You must close all open files before concatenating", "Concatenate",
                            JOptionPane.ERROR_MESSAGE);
                else {
                    System.gc();
                    File[] desktopFiles = TTTProcessor.getFilesForConcat();
                    if (desktopFiles == null)
                        return;
                    if (desktopFiles.length < 2) {
                        JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                                "You need to choose more than one file to concatenate", "Invalid selection",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Concatenator concat = new Concatenator(desktopFiles);
                    concat.start();
                }
            }
        });
        fileMenu.add(concatItem);

        // close currently selected file
        closeItem.setMnemonic(KeyEvent.VK_C);
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeActiveFile();
            }
        });
        closeItem.setEnabled(false);
        fileMenu.add(closeItem);

        // exit the program - close all files first
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tryExit();
            }
        });
        fileMenu.add(exitItem);

        // view file details
        detailsItem.setMnemonic(KeyEvent.VK_D);
        detailsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        detailsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JInternalFrame frame = getActiveFrame();
                if (frame == null) {
                    JOptionPane.showInternalMessageDialog(desktopPane, "No file selected", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                FileConnector connector = getFileConnectorFromFrame(frame);
                DetailsPanel detailsPanel = new DetailsPanel(connector);
                JOptionPane.showInternalMessageDialog(desktopPane, detailsPanel, "Details: "
                        + connector.getDesktopTitle(), JOptionPane.PLAIN_MESSAGE);
            } 
        });
        detailsItem.setEnabled(false);
        detailsMenu.add(detailsItem);

        // used for setting a new desktop title to be saved in the header
        titleItem.setMnemonic(KeyEvent.VK_T);
        titleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        titleItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JInternalFrame frame = getActiveFrame();
                if (frame == null) {
                    JOptionPane.showInternalMessageDialog(desktopPane, "No file selected", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                FileConnector connector = getFileConnectorFromFrame(frame);
                String oldTitle = connector.getDesktopTitle();
                String newTitle = (String) JOptionPane.showInternalInputDialog(desktopPane, "Input new desktop title",
                        "Desktop Title", JOptionPane.PLAIN_MESSAGE, null, null, oldTitle);
                if (newTitle != null && !oldTitle.equals(newTitle) && !newTitle.equals(""))
                    connector.setDesktopTitle(newTitle);
            }
        });
        titleItem.setEnabled(false);
        detailsMenu.add(titleItem);

        // change options
        optionItem.setMnemonic(KeyEvent.VK_O);
        optionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
        optionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OptionPanel optionPanel = new OptionPanel();
                int selection = JOptionPane.showInternalConfirmDialog(desktopPane, optionPanel, "Options",
                        JOptionPane.OK_CANCEL_OPTION);
                if (selection == JOptionPane.OK_OPTION) {
                    optionPanel.commitSelections();
                    for (int i = 0; i < fileConnectors.size(); i++)
                        fileConnectors.get(i).refreshSynchStatus();
                }
            }
        });
        optionMenu.add(optionItem);

        // help
        /*
         * URL urlHelp = this.getClass().getResource("resources/Help16.gif"); ImageIcon helpIcon = new
         * ImageIcon(urlHelp); helpItem.setIcon(helpIcon);
         */
        helpItem.setMnemonic(KeyEvent.VK_H);
        helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });
        helpMenu.add(helpItem);

        // log
        outputItem.setMnemonic(KeyEvent.VK_O);
        outputItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        outputItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLog();
            }
        });
        helpMenu.add(outputItem);

        // about
        /*
         * URL urlAbout = this.getClass().getResource("resources/About16.gif"); ImageIcon aboutIcon = new
         * ImageIcon(urlAbout); aboutItem.setIcon(aboutIcon);
         */
        aboutItem.setMnemonic(KeyEvent.VK_A);
        aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showInternalMessageDialog(desktopPane, Parameters.ABOUT, "About TTTEditor",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(detailsMenu);
        menuBar.add(optionMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // exits if all files can be closed
    private void tryExit() {
        // don't do anything if glass pane is visible
        if (glassPane.isVisible()){
        	return;
        	}
        // exit normally if no files are active
        if (fileConnectors.size() < 1){        
        	close();
        }// try to close all files - if this succeeds, will exit
        else {
            exitWhenAllClosed = true;
            closeAllFiles();
        }
    }

    // get the frame which the user will consider active
    // this will either be the highlighted frame, or the last frame which the user selected.
    // if no such frame exists, returns the first DesktopViewer available, or null if none is found
    private JInternalFrame getActiveFrame() {
        JInternalFrame frame = desktopPane.getSelectedFrame();
        if (frame != null && (frame instanceof DesktopViewer || frame instanceof VideoViewer))
            return frame;
        frame = desktopViewerListener.getLastActiveViewer();
        if (frame != null) {
            try {
                frame.setSelected(true);
            } catch (Exception e) {
                System.out.println("Attempt to select file vetoed: " + e);
                e.printStackTrace();
            }
            ;
            return frame;
        }
        if (desktopPane.getAllFramesInLayer(DESKTOP_LAYER).length > 0) {
            return desktopPane.getAllFramesInLayer(DESKTOP_LAYER)[0];
        }
        // no frame is to be found
        return null;
    }

    /**
     * Get the <code>DesktopViewerListener</code> in use.
     * 
     * @return The <code>DesktopViewerListener</code> used by <CODE>DesktopViewers</CODE> to deal focus and related
     *         issues.
     */
    protected static DesktopViewerListener getDesktopViewerListener() {
        return desktopViewerListener;
    }

    /**
     * Presents the user with a file chooser to open a new set of files.
     * 
     * @return <code>true</code> if a file is opened, <code>false</code> otherwise.
     */
    // modified 19.01.2006 by Peter Ziewer
    // extended open() to open(String file)
    public boolean open() {
        return open(null);
    }

    public boolean open(String file) {
        // ensure max space available before trying to open a new file
        System.gc();

        // pause any players currently playing
        if (fileConnectors.size() != 0) {
            for (int count = 0; count < fileConnectors.size(); count++)
                fileConnectors.get(count).pausePlayer();
        }
     
        // don't allow too many files to be opened
        if (fileConnectors.size() == maxFileCount) {
            JOptionPane.showInternalMessageDialog(desktopPane, "It is not possible to open any more files.\n"
                    + "Please close some already opened files, and try again.");
            return false;
        }
      
        // if trying to open the same file (or sound/video/desktop of that file), nothing happens
        // otherwise, nothing happens until user confirms which files to open -
        // ensuring it is possible to cancel if changes mind
        File newFile = null;
        if (file != null){
            newFile = new File(file);
        }
        
        while (cannotOpenFile(newFile)) {
            newFile = TTTFileUtilities.showOpenFileInternalDialog(true);
            if (newFile == null)
                return false;
        }

        // added 19.01.2006 by Peter Ziewer
        // use same path after restart
        TTTEditor.userPrefs.put("last used path", newFile.getAbsoluteFile().getParent());

        // connect the files
        FileConnector newFileConnector = new FileConnector(newFile);
        boolean connectionSuccessful = newFileConnector.connectFiles();
        if (connectionSuccessful) {
            fileConnectors.add(newFileConnector);
            newFileConnector.readFiles();
            // if this is the first file to be opened, enable options
            if (fileConnectors.size() == 1)
                setEnableFileOpenedOptions(true);
            return true;
        }
        return false;
    }

    /**
     * Adds an internal frame to the glass pane.
     * 
     * @param frame
     *            the <code>JInternalFrame</code> that should be added.
     */
    public void addToGlassPane(JInternalFrame frame) {
        glassPane.add(frame);
    }

    /**
     * Provides the desktop pane being currently used by this class for displaying viewers.
     * 
     * @return the <code>JDesktopPane</code> used by this application to contain JInternalFrames.
     */
    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    /**
     * Provides the help panel being currently used by this class.
     * 
     * @return the <code>HelpPanel</code> used by this application
     */
    public HelpPanel getHelpPanel() {
        return helpPanel;
    }

    /**
     * Provides the output display panel being currently used by this class.
     * 
     * @return the <code>OutputDisplayPanel</code> used by this application
     */
    public OutputDisplayPanel getOutputDisplayPanel() {
        return outputDisplay;
    }

    /**
     * Sets the TTTEditor help to visible, if it is not already
     */
    public void showHelp() {
        if (!sideBar.isVisible()) {
            splitPane.resetToPreferredSizes();
            sideBar.setVisible(true);
            splitPane.setDividerSize(4);
        }
        sideTabbedPane.setSelectedIndex(HELP_TAB_INDEX);
    }

    /**
     * Sets the TTTEditor log to visible, if it is not already
     */
    public void showLog() {
        if (!sideBar.isVisible()) {
            splitPane.resetToPreferredSizes();
            sideBar.setVisible(true);
            splitPane.setDividerSize(4);
        }
        sideTabbedPane.setSelectedIndex(LOG_TAB_INDEX);
    }

    // used to position each new internal frame added to the desktop
    private int initialLocation = 0;

    /**
     * Adds the viewers passed to the JDesktopPane. VideoViewer may be null if no video is available.
     * 
     * @param desktop
     *            the <code>DesktopViewer</code> to be displayed
     * @param video
     *            the <code>VideoViewer</code> to be displayed
     */
    public void updateGUI(DesktopViewer desktop, VideoViewer video) {
        if (initialLocation < getWidth() - 40 && initialLocation < this.getHeight() - 40)
            initialLocation += 25;
        else
            initialLocation = 0;
        if (desktop != null) {
            desktop.setSize(desktop.getPreferredSize());
            desktop.setLocation(initialLocation, initialLocation);
            desktopPane.add(desktop);
            desktopPane.setLayer(desktop, DESKTOP_LAYER);
            desktop.setVisible(true);
            try {
                desktop.setMaximum(true);
            } catch (Exception e) {
                System.out.println("Unable to maximize desktop: " + e);
            }
        }
        if (video != null) {
            video.setLocation(initialLocation, initialLocation);
            video.setSize(video.getPreferredSize());
            desktopPane.add(video);
            desktopPane.setLayer(video, VIDEO_LAYER);
            video.setVisible(true);
        }
    }

    /**
     * Method to return the current instance of TTTEditor. If there is no current instance, a new instance is created
     * and returned.
     * 
     * @return the current instance of TTTEditor.
     */
    public static TTTEditor getInstance() {
        if (instance == null) {        	
            instance = new TTTEditor();
        }
        return instance;
    }

    //Open ttteditor and show fileselectDialog
    public static void OpenEditorAndShowFileDialog(){
    	getInstance().open();    	
    }
    
    // determine appropriate file connector for a given JInternalFrame
    // returns null if the frame is not a desktop or video viewer
    private FileConnector getFileConnectorFromFrame(JInternalFrame frame) {
        if (frame instanceof DesktopViewer)
            return ((DesktopViewer) frame).getFileConnector();
        else if (frame instanceof VideoViewer)
            return ((VideoViewer) frame).getFileConnector();
        else
            return null;
    }

    // Compares a file taken as an argument with currently open files
    // Returns true if they are the same, false if they are different
    // Returns true if the new file is null
    private boolean cannotOpenFile(File newFile) {
        if (newFile == null)
            return true;
        for (int count = 0; count < fileConnectors.size(); count++) {
            if (newFile.equals(fileConnectors.get(count).getDesktopFile())) {
                JOptionPane.showInternalMessageDialog(desktopPane, "File already open!", "File open error",
                        JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    /**
     * Ends the connection, removes from the list of FileConnectors. Asks the user whether the file should first be
     * saved.
     * 
     * @param connector
     *            <code>FileConnector</code> containing the files to be closed.
     * @return <code>true</code> if the file is closed, <code>false</code> otherwise.
     */
    public boolean closeFile(FileConnector connector) {

        int confirm = JOptionPane.showInternalConfirmDialog(TTTEditor.getInstance().getDesktopPane(),
                "Do you want to save changes to " + connector.getDesktopFile().getName() + "?", "Close file",
                JOptionPane.YES_NO_CANCEL_OPTION);
        // if user cancels, don't do anything and don't try to exit
        if (confirm == JOptionPane.CANCEL_OPTION) {
            if (exitWhenAllClosed)
                exitWhenAllClosed = false;
            return false;
        }

        // tell saveFile to do the exiting, to prevent exiting too soon before
        // file is written (on another writing thread)
        if (confirm == JOptionPane.YES_OPTION) {
            return saveFile(connector, null, true, false, true);
        }

        connector.endConnection();
        removeFileConnector(connector);

        return true;
    }

    /**
     * Checks for active file and tries to close it, first allowing the user to save it if desired. When a file is
     * closed, another is activated - so if any files are available one should always be active.
     * 
     * @return <code>true</code> if all the file is successfully closed, <code>false</code> otherwise.
     */
    public boolean closeActiveFile() {
        JInternalFrame frame = getActiveFrame();

        // if no selected file
        // (should not occur - option should be disabled if no files to close)
        if (frame == null)
            // if only one frame is available, it is clear which file should be closed
            if (desktopPane.getAllFrames().length == 1)
                frame = desktopPane.getAllFrames()[0];
            else {
                // unable to know which file should be closed
                JOptionPane.showInternalMessageDialog(desktopPane, "Please select a file to close");
                return false;
            }

        FileConnector connector = getFileConnectorFromFrame(frame);
        return closeFile(connector);
    }

    /**
     * Closes the files which are currently open and remove the viewers. Provides opportunity to save the files first,
     * which allows the user to cancel and so prevent all files being closed. If the user presses cancel, the whole
     * operation stops and no more files are closed.
     * 
     * @return <code>true</code> if there are files open which may be closed, <code>false</code> otherwise. Does NOT
     *         test to see if the files are closed or not.
     */
    public boolean closeAllFiles() {
        if (fileConnectors.size() > 0) {
            closeFile(fileConnectors.lastElement());
            return true;
        } else
            return false;
    }

    /**
     * Allows user to save the currently active file with a new name.
     * 
     * @return <code>true</code> if all the files are successfully saved, <code>false</code> otherwise.
     * @param processTrim
     *            whether the file should be trimmed when it is saved (if suitable markers are present)
     * @param subDivide
     *            whether the file should be sub-divided when it is saved (assuming suitable markers are present)
     */
    protected boolean saveActiveFileAs(boolean processTrim, boolean subDivide) {
        JInternalFrame frame = getActiveFrame();
        if (frame == null) {
            JOptionPane.showInternalMessageDialog(desktopPane, "No file selected!", "Save error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        FileConnector connector = getFileConnectorFromFrame(frame);
        return saveFile(connector, null, processTrim, subDivide, false);
    }

    /**
     * Tries to save the currently active file - does not prompt for a file name unless the proper file name to use is
     * not obvious.
     * 
     * @return <code>true</code> if a file is saved, <code>false</code> otherwise
     */
    protected boolean saveActiveFile() {
        JInternalFrame frame = getActiveFrame();
        if (frame == null) {
            JOptionPane.showInternalMessageDialog(desktopPane, "No file selected!", "Save error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        FileConnector connector = getFileConnectorFromFrame(frame);
        File file = connector.getDesktopFile();

        // if file has correct desktop ending, try to save it with same name
        // otherwise prompt user for confirmation of file name to be saved
        if (file.getAbsolutePath().endsWith(Parameters.desktopEndings[0]))
            return saveFile(connector, file, true, false, false);
        else
            return saveFile(connector, null, true, false, false);
    }

    /**
     * Allows user to save the specified file with a new name.
     * 
     * @return <code>true</code> if all the files is successfully saved, <code>false</code> otherwise.
     * @param file
     *            the TTT file to use as output
     * @param processTrim
     *            whether the file should be trimmed when it is saved (if suitable markers are present)
     * @param subDivide
     *            whether the file should be trimmed when it is saved (if suitable markers are present)
     * @param connector
     *            the <code>FileConnector</code> containing the files to be saved.
     * @param closeAfterWriting
     *            <code>true</code> if the connection should be ended and viewers closed after the file is saved,
     *            <code>false</code> otherwise.
     */
    public boolean saveFile(FileConnector connector, File file, boolean processTrim, boolean subDivide,
            boolean closeAfterWriting) {
        boolean fileConfirmed = true;
        while (file == null || !fileConfirmed) {
            File selectedFile = connector.getDesktopFile();
            selectedFile = TTTFileWriter.forceDesktopFileToDefaultEnding(selectedFile);

            file = TTTFileUtilities.showSaveFileInternalDialog(selectedFile);
            if (file == null)
                return false;

            if (file.exists() && !Parameters.createBackups) {
                int selection = JOptionPane.showInternalConfirmDialog(desktopPane,
                        "Are you sure you wish to overwrite\n" + file.getName() + "?", "Confirm overwrite",
                        JOptionPane.YES_NO_OPTION);
                if (selection == JOptionPane.YES_OPTION)
                    fileConfirmed = true;
                else
                    fileConfirmed = false;
            }
        }
        connector.saveFile(file, processTrim, subDivide, closeAfterWriting);
        return true;
    }

    // allows enabling/disabling of options which only make sense when a file is open
    private void setEnableFileOpenedOptions(boolean enable) {
        saveAsItem.setEnabled(enable);
        saveItem.setEnabled(enable);
        closeItem.setEnabled(enable);
        divideItem.setEnabled(enable);
        detailsItem.setEnabled(enable);
        titleItem.setEnabled(enable);
    }

    /**
     * Removes a specified <code>FileConnector</code> from the list of <code>FileConnector</code>s held by the
     * <code>TTTEditor</code>. <br />
     * No effort is made to close any files open within the <code>FileConnector</code>
     * 
     * @param connector
     *            the <code>FileConnector</code> to be removed
     */
    protected void removeFileConnector(FileConnector connector) {
        fileConnectors.remove(connector);

        // set next file as active
        if (fileConnectors.size() > 0) {
            JInternalFrame[] frames = desktopPane.getAllFramesInLayer(DESKTOP_LAYER);
            JInternalFrame nextFrame = frames[frames.length - 1];
            try {
                nextFrame.setIcon(false);
                nextFrame.setSelected(true);
            } catch (Exception e) {
                System.out.println("Exception selecting next internal frame: " + e.toString());
            }
        } else
            // if no more files are open, some options should be disabled
            setEnableFileOpenedOptions(false);

        if (exitWhenAllClosed) {
            if (fileConnectors.size() < 1){
         close();
            }
            else
                closeAllFiles();
        }

        System.out.println("Connector removed: length = " + fileConnectors.size());
    }

    //Closing the editor frame
    private void close(){
    	instance = null;
    	this.setVisible(false);
        this.dispose();   	
        TTT.getInstance();
	if(TTT.enabledNativeLookAndFeel){
    	   try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {		
			e.printStackTrace();
		} 
       }
    }
    
    /**
     * Method (from TTT) which generates a formatted string representing a given time.
     * 
     * @param msec
     *            the time in milliseconds.
     * @return String containing formatted time, to second accuracy.
     */
    public static String getStringFromTime(int msec) {
        // generates nice time String
        return getStringFromTime(msec, false);
    }

    /**
     * Method (from TTT) which generates a formatted string representing a given time.
     * <p />
     * String returned will be in the form:-<br />
     * <li><code>10:11</code> (min and sec)</li>
     * <li><code>10:11.12</code> (min, sec and ms)</li>
     * </ul>
     * 
     * @param msec
     *            the time in milliseconds.
     * @param includeMilliseconds
     *            returned string should have millisecond accuracy.
     * @return String containing formatted time, to second accuracy.
     */
    public static String getStringFromTime(int msec, boolean includeMilliseconds) {
        // generates nice time String
        boolean negative = msec < 0;
        if (negative)
            msec = -msec;

        int sec = msec / 1000 % 60;
        int min = msec / 60000;
        msec = msec % 1000;
        return (negative ? "-" : "") + ((min < 10) && !negative ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec
                + (includeMilliseconds ? "." + (msec < 100 ? "0" : "") + (msec < 10 ? "0" : "") + msec : "");
    }

    public static String getStringFromTime(long nanosec, boolean includeMilliseconds) {
        return getStringFromTime((int) (nanosec / 1000000), includeMilliseconds);
    }

    /**
     * Method (from TTT) which obtains a time from a String.
     * <p />
     * String may be in any of the following formats:- <break/>
     * <ul>
     * <li><code>10min (minutes)</li>
     * <li><code>10m</code> (minutes)</li>
     * <li><code>10sec</code> (seconds)</li>
     * <li><code>10s</code> (seconds)</li>
     * <li><code>10:11</code> (TTT string, min and sec)</li>
     * <li><code>10:11.12</code> (TTT string, min, sec and ms)</li></ul>
     * @return time extracted from the string, in milliseconds.
     * @param value String representing a suitably formatted time.
     * @throws java.lang.NumberFormatException
     */
    public static int getTimeFromString(String value) throws NumberFormatException {
        value = value.trim();
        int time = 0;
        if (value.endsWith("min"))
            time = 60000 * Integer.parseInt(value.substring(0, value.length() - 3));
        else if (value.endsWith("m"))
            time = 60000 * Integer.parseInt(value.substring(0, value.length() - 1));
        else if (value.endsWith("sec"))
            time = 1000 * Integer.parseInt(value.substring(0, value.length() - 3));
        else if (value.endsWith("s"))
            time = 1000 * Integer.parseInt(value.substring(0, value.length() - 1));
        else {
            int dumpf = value.indexOf(':');
            if (dumpf > 0) {
                time = 60000 * Integer.parseInt(value.substring(0, dumpf++));
                int dumpfer = value.indexOf('.');
                if (dumpfer > 0) {
                    time += 1000 * Integer.parseInt(value.substring(dumpf, dumpfer++));
                    time += Integer.parseInt(value.substring(dumpfer));
                } else
                    time += 1000 * Integer.parseInt(value.substring(dumpf));
            } else
                time = Integer.parseInt(value);
        }
        return time;
    }

    /**
     * Main Method
     */
    // modified 19.01.2006 by Peter Ziewer
    // added command line argument handling
    static boolean consoleOutput;

    // added 03.04.2006 by Peter Ziewer
    // bug in SUN Java 1.5 (Windows and Linux)
    // see Bug ID 6178755: REGRESSION: JOptionPane's showInternal*Dialog methods never return
    // workaround: InternalDialog used by open must be invoked in event dispatching thread
    private static int result = 0;

//    public static int showInternalConfirmDialog(final Object message) {
//        return showInternalConfirmDialog(message, "Warning", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);
//    }

    public static int showInternalConfirmDialog(final Object message, final String title, final int optionType) {
        return showInternalConfirmDialog(message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public static int showInternalConfirmDialog(final Object message, final String title, final int optionType,
            final int messageType) {

        // bug in SUN Java 1.5 (Windows and Linux)
        // see Bug ID 6178755: REGRESSION: JOptionPane's showInternal*Dialog methods never return
        // workaround: InternalDialog used by open must be invoked in event dispatching thread
        try {
            if (SwingUtilities.isEventDispatchThread())
                result = JOptionPane.showInternalConfirmDialog(TTTEditor.getInstance().getDesktopPane(), message,
                        title, optionType, messageType);
            else
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        result = JOptionPane.showInternalConfirmDialog(TTTEditor.getInstance().getDesktopPane(),
                                message, title, optionType, messageType);
                    }
                });
        } catch (Exception e) {
            result = JOptionPane.CLOSED_OPTION;
        }

        return result;
    }
    // end 03.04.2006 by Peter Ziewer

    public static void main(final String[] args) {
        // REMOVED 24.10.07 by Ziewer
        // INSETAD: doubled output (console and log-window) in OutputDisplayPanel
        //        for (int i = 0; i < args.length; i++)
        //            if (args[i].equalsIgnoreCase("-verbose") || args[i].equalsIgnoreCase("-v"))
        //                consoleOutput = true; // must be set prior to instanciation of TTTEditor

        // instanciate TTTEditor
        TTTEditor.getInstance();

        for (int i = 0; i < args.length; i++)
            if (!(args[i].equalsIgnoreCase("-verbose") || args[i].equalsIgnoreCase("-v"))) {
                TTTEditor.getInstance().open(args[i]);
            }
    }

    // for concatenating
    class Concatenator extends Thread {

        File[] desktopFiles;

        Concatenator(File[] desktopFiles) {
            this.desktopFiles = desktopFiles;
        }

        public void run() {
            IOProgressDisplayFrame progress = new IOProgressDisplayFrame(outputDisplay);
            addToGlassPane(progress);
            try {
                TTTProcessor.concat(desktopFiles);
            } catch (Exception e) {
                System.out.println("Concatenate failed!" + e);
                e.printStackTrace();
                progress.setCompleted();
                return;
            } catch (OutOfMemoryError error) {
                progress.setCompleted();
                return;
            } finally {
                System.out.println("\nPress OK to continue.\n\n\n");
                progress.setCompleted();
            }
        }
    }

}