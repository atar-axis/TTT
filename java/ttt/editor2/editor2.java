package ttt.editor2;

import java.awt.Color;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import java.util.prefs.Preferences;

import javax.media.Manager;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.DimensionUIResource;

import ttt.TTT;

import ttt.gui.GraphicsContext;

import ttt.player.PlaybackControls;

import ttt.record.Recording;

/**
 * Main class for TTTEditor.
 */
@SuppressWarnings("serial")
public class editor2 extends JFrame {

	static final String version = "18.08.2010";

	// added 19.01.2006 by Peter Ziewer

	// user preferences
	static Preferences userPrefs = Preferences.userRoot().node("/TTTEditor");

	private static editor2 instance;

	// markers for trimming the recording
	private int startMarker;
	private int endMarker;

	static boolean consoleOutput;

	// used to position each new internal frame added to the desktop
	private int initialLocation = 0;

	// no more than this number of files can be opened at the same time
	private final int maxFileCount = 1;

	// The layer of the JDesktopPane to which DesktopViewers are added
	public final int DESKTOP_LAYER = 1;

	private Recording recording;
	private int filesopen = 0;

	private boolean desktopPresent = false;
	private boolean audioPresent = false;
	private boolean videoPresent = false;
	private File videoFile;
	private File audioFile;
	private File desktopFile;

	// //////////////////////////////////////
	// GUI elements go here
	// //////////////////////////////////////
	private OutputDisplayPanel outputDisplay = new OutputDisplayPanel();

	// the JDesktopPane, to which JInternalFrames may be added
	private JDesktopPane desktopPane;

	// the TTTEditorGlassPane, used for adding internal frames
	// that block from clicking other frames
	private TTTEditorGlassPane glassPane;

	// scrollpane used to separate help from the rest of the desktop
	private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	// Menu bar setup
	private JMenuBar menuBar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenuItem openItem = new JMenuItem("Open");
	private JMenuItem saveAsItem = new JMenuItem("Save As...");
	private JMenuItem saveItem = new JMenuItem("Save");
	private JMenuItem concatItem = new JMenuItem("Concatenate");
	private JMenuItem closeItem = new JMenuItem("Close");
	private JMenuItem exitItem = new JMenuItem("Exit");

	
	//Player/Editor frame setup
	private JPanel MarkerPanel = new JPanel();
	private JButton firstMarker = new JButton("First");
	private JButton lastMarker = new JButton("Last");

	JTextField markerFirstText = new JTextField(
			"New Starting point at: XXXXX");
	JTextField markerLastText = new JTextField(
			"New Ending point at: XXXXX");

	private JInternalFrame playerFrame = new JInternalFrame();
	private JPanel playerPane = new JPanel();

	// ////////////////////////////
	// GUI elements end here
	// ////////////////////////////

	/**
	 * Class Constructor.
	 */
	public editor2() {
		super("TTTEditor, Version: " + version);

		desktopPane = new JDesktopPane();
		desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		desktopPane.setBackground(Color.GRAY);
		glassPane = new TTTEditorGlassPane();
		setGlassPane(glassPane);

		splitPane.setLeftComponent(desktopPane);
		splitPane.setResizeWeight(1);
		splitPane.setDividerSize(0);
		setContentPane(splitPane);

		createMenuBar();

		// allows movies to be rendered in swing containers
		Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));

		// attempt to close all open files before closing
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				tryExit();
			}
		});		
		
		setVisible(true);
		setExtendedState(MAXIMIZED_BOTH);
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		editor2.getInstance();
	}

	/**
	 * creates the menu bar, and adds it to the JFrame
	 */
	private void createMenuBar() {
		fileMenu.setMnemonic(KeyEvent.VK_F);

		// open a file
		openItem.setMnemonic(KeyEvent.VK_O);
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		openItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		fileMenu.add(openItem);

		// save currently selected file with current name
		saveItem.setMnemonic(KeyEvent.VK_S);
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));
		saveItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveActiveFile();
			}
		});
		saveItem.setEnabled(true);
		fileMenu.add(saveItem);

		// save currently selected file with a new name
		saveAsItem.setMnemonic(KeyEvent.VK_A);
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.CTRL_MASK));
		saveAsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveActiveFileAs(true, false);
			}
		});
		saveAsItem.setEnabled(true);
		fileMenu.add(saveAsItem);

		// join 2 or more files together
		concatItem.setMnemonic(KeyEvent.VK_N);
		concatItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.CTRL_MASK));
		concatItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.gc();
				File[] desktopFiles = TTTProcessor.getFilesForConcat();
				if (desktopFiles == null)
					return;
				if (desktopFiles.length < 2) {
					JOptionPane
							.showInternalMessageDialog(
									editor2.getInstance().getDesktopPane(),
									"You need to choose more than one file to concatenate",
									"Invalid selection",
									JOptionPane.ERROR_MESSAGE);
					return;
				}
				Concatenator concat = new Concatenator(desktopFiles);
				concat.start();
			}
		});
		fileMenu.add(concatItem);

		// close currently selected file
		closeItem.setMnemonic(KeyEvent.VK_C);
		closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.CTRL_MASK));
		closeItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(recording != null){
				closeFile();
				}
			}
		});
		closeItem.setEnabled(true);
		fileMenu.add(closeItem);

		// exit the program - close all files first
		exitItem.setMnemonic(KeyEvent.VK_X);
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
				ActionEvent.CTRL_MASK));
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tryExit();
			}
		});
		fileMenu.add(exitItem);

		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
	}

	/**
	 * exits if all files can be closed
	 */
	private void tryExit() {
		// don't do anything if glass pane is visible
		if (glassPane.isVisible()) {
			return;
		}else if(recording != null){
			closeAllFiles();			
		}
		close();
	}

	/**
	 * Presents the user with a file chooser to open a new set of files.
	 * 
	 * @return <code>true</code> if a file is opened, <code>false</code>
	 *         otherwise.
	 */
	public boolean open() {
		// don't allow too many files to be opened
		if (filesopen == maxFileCount) {
			JOptionPane
					.showInternalMessageDialog(
							desktopPane,
							"It is not possible to open any more files.\n"
									+ "Please close some already opened files, and try again.");
			return false;
		}

		// use last opened path as start location for the filechooser
		String lastUsedPath = editor2.userPrefs.get("last used path", null);
		JFileChooser fileChooser = lastUsedPath != null ? new JFileChooser(
				lastUsedPath) : new JFileChooser();

		int returnValue = fileChooser.showOpenDialog(editor2.getInstance());

		if (returnValue == JFileChooser.APPROVE_OPTION) {

			// save used path
			editor2.userPrefs.put("last used path", fileChooser
					.getSelectedFile().getAbsoluteFile().getParent());

			// Open file and create edit Panel
			open(fileChooser.getSelectedFile());
		}

		return false;
	}

	/**
	 * Opens a file and shows it in the Editor
	 * 
	 * @param file
	 *            the file to open
	 * @return returns false if something goes wrong
	 */
	public boolean open(File file) {
		// ensure max space available before trying to open a new file
		System.gc();
		IOProgressDisplayFrame progress = new IOProgressDisplayFrame(editor2
				.getInstance().getOutputDisplayPanel());
		try {
			// look for audio, desktop and video
			if (connectFiles(file)) {

				addToGlassPane(progress);
				
				
				
			MarkerPanel = new JPanel();
			firstMarker = new JButton("First");
			lastMarker = new JButton("Last");

			markerFirstText = new JTextField("New Starting point at: XXXXX");
			markerLastText = new JTextField("New Ending point at: XXXXX");

			playerFrame = new JInternalFrame("Editor");
			playerPane = new JPanel();
				
				
				// open the file
				recording = new Recording(file.getAbsolutePath());
				// get the desktopfile component
				GraphicsContext graphicsContext = recording
						.getGraphicsContext();
				// get the controls
				final PlaybackControls playbackControls = recording
						.getPlaybackControls();

				firstMarker.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						startMarker = playbackControls.getTimeSliderValue();
						markerFirstText.setText("New Starting point at: "
						+ getStringFromTime(startMarker));
					}
				});

				lastMarker.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						endMarker = playbackControls.getTimeSliderValue();
						markerLastText.setText("New Ending point at: "
								+ getStringFromTime(endMarker));
					}
				});
				// gui stuff
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				MarkerPanel.add(firstMarker, c);
				MarkerPanel.add(markerFirstText, c);
				c.gridy = 1;
				MarkerPanel.add(lastMarker, c);
				MarkerPanel.add(markerLastText, c);
				c.gridy = 0;
			
				graphicsContext.setScaleToFit(true);
				graphicsContext.setPreferredSize(new DimensionUIResource(300,
						300));

				graphicsContext.refresh();

				JScrollPane scrollPane = new JScrollPane(graphicsContext);

				graphicsContext.setScaleFactor(0.7f);

				playerPane.setLayout(new GridBagLayout());
				
				playerPane.add(playbackControls, c);
				c.gridy = 1;
				playerPane.add(scrollPane, c);
				c.gridy = 2;
				playerPane.add(MarkerPanel, c);

				playerFrame.add(playerPane);

				
				updateGUI(playerFrame);
				progress.setCompleted();
			}

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		filesopen++;
		return true;
	}

	/**
	 * get the current editor instance or create one if there isn't one
	 * 
	 * @return returns the active instance of the editor2
	 */
	public static editor2 getInstance() {
		if (instance == null) {
			instance = new editor2();
		}
		return instance;
	}

	/**
	 * Adds the viewers passed to the JDesktopPane. VideoViewer may be null if
	 * no video is available.
	 * 
	 * @param desktop
	 *            the <code>DesktopViewer</code> to be displayed
	 * @param video
	 *            the <code>VideoViewer</code> to be displayed
	 */
	public void updateGUI(JInternalFrame desktop) {
		if (initialLocation < getWidth() - 40
				&& initialLocation < this.getHeight() - 40)
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
	}

	/**
	 * looks for desktop, audio and video files and present a dialog to choose
	 * which may be edited
	 * 
	 * @param file
	 *            the desktop file
	 * @return
	 */
	public boolean connectFiles(File file) {
		audioFile = TTTFileUtilities.checkForAudio(file);
		videoFile = TTTFileUtilities.checkForVideo(file);
		audioPresent = (audioFile != null);
		videoPresent = (videoFile != null);
		desktopPresent = (file != null);
		if(desktopPresent){
			desktopFile = file;
		}

		SelectionPanel selectionPanel = new SelectionPanel(file);

		result = editor2.showInternalConfirmDialog(selectionPanel, "Confirm",
				JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION) {
			desktopPresent = desktopCheckBox.isSelected();
			audioPresent = audioCheckBox.isSelected();
			videoPresent = videoCheckBox.isSelected();
			// If none are selected, notify the user and return false to
			// indicate that file connection has been
			// unsuccessful
			if (!desktopPresent && !audioPresent && !videoPresent) {
				JOptionPane.showInternalMessageDialog(editor2.getInstance()
						.getDesktopPane(), "No files have been selected.");
				return false;
			}
			// If at least one of the 3 types of media are not selected, warn
			// user and give the option of continuing or
			// not
			// Note: could amend this if the lack of video is thought of as not
			// too serious
			else if (!desktopPresent || !audioPresent || !videoPresent) {
				String warningString = "Failing to edit audio, video and desktop together "
						+ "\nmay mean that it will not be possible to synchronize them later."
						+ "\n\nAre you sure you wish to proceed?";
				// If the user wishes to continue, return true to indicate that
				// the file connection has been successful
				// (although not all 3 files have been selected)
				if (editor2.showInternalConfirmDialog(warningString, "Confirm",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					// ensure files are ignored if not selected
					if (!videoCheckBox.isSelected())
						videoFile = null;
					if (!audioCheckBox.isSelected())
						audioFile = null;
					return true;
				}
				// If the user has selected not to proceed with opening the
				// files, return false
				else
					return false;
			}
			// If all 3 files are available and have been selected, return true
			// for a successful file connection
			else {
				return true;
			}
		}
		// If the user has not selected to open the files, return false
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
	 * Provides the desktop pane being currently used by this class for
	 * displaying viewers.
	 * 
	 * @return the <code>JDesktopPane</code> used by this application to contain
	 *         JInternalFrames.
	 */
	public JDesktopPane getDesktopPane() {
		return desktopPane;
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
	 * Close the current file
	 * 	
	 * @return <code>true</code> if the file is closed, <code>false</code>
	 *         otherwise.
	 */
	public boolean closeFile() {
		int confirm = JOptionPane
				.showInternalConfirmDialog(editor2.getInstance()
						.getDesktopPane(), "Do you want to save changes to "
						+ desktopFile.getName() + "?", "Close file",
						JOptionPane.YES_NO_CANCEL_OPTION);
		// if user cancels, don't do anything and don't try to exit
		if (confirm == JOptionPane.CANCEL_OPTION) {
			return false;
		}
		// tell saveFile to do the exiting, to prevent exiting too soon before
		// file is written (on another writing thread)
		if (confirm == JOptionPane.YES_OPTION) {
			saveFile(null);
		}

		playerFrame.setVisible(false);
		playerFrame.dispose();		
		recording.close();
		recording = null;
		filesopen--;
		System.gc();
		
		return true;
	}

	/**
	 * Closes the files which are currently open and remove the viewers.
	 * Provides opportunity to save the files first, which allows the user to
	 * cancel and so prevent all files being closed. If the user presses cancel,
	 * the whole operation stops and no more files are closed.
	 * 
	 * @return <code>true</code> if there are files open which may be closed,
	 *         <code>false</code> otherwise. Does NOT test to see if the files
	 *         are closed or not.
	 */
	public boolean closeAllFiles() {
		if(closeFile()){
		return true;
		}
		return false;
	}

	/**
	 * Allows user to save the currently active file with a new name.
	 * 
	 * @return <code>true</code> if all the files are successfully saved,
	 *         <code>false</code> otherwise.
	 * @param processTrim
	 *            whether the file should be trimmed when it is saved (if
	 *            suitable markers are present)
	 * @param subDivide
	 *            whether the file should be sub-divided when it is saved
	 *            (assuming suitable markers are present)
	 */
	protected boolean saveActiveFileAs(boolean processTrim, boolean subDivide) {
		if (recording == null) {
			return false;
		}
		return saveFile(null);
	}

	/**
	 * Tries to save the currently active file - does not prompt for a file name
	 * unless the proper file name to use is not obvious.
	 * 
	 * @return <code>true</code> if a file is saved, <code>false</code>
	 *         otherwise
	 */
	protected boolean saveActiveFile() {
		if (recording == null) {
			return false;
		}

		try {
			return saveFile(new File(recording.getDirectory()+ recording.getFileBase() + ".ttt"));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Allows user to save the specified file with a new name.
	 * 
	 * @return <code>true</code> if all the files is successfully saved,
	 *         <code>false</code> otherwise.
	 * @param file
	 *            the TTT file to use as output
	 */
	public boolean saveFile(File file) {
		if (recording != null) {
			File recordFile;
			try {
				recordFile = new File(recording.getDirectory()
						+ recording.getFileBase() + ".ttt");

				boolean fileConfirmed = true;
				while (file == null || !fileConfirmed) {
					file = TTTFileUtilities
							.showSaveFileInternalDialog(recordFile);
					if (file == null)
						return false;

					if (file.exists() && !Parameters.createBackups) {
						int selection = JOptionPane.showInternalConfirmDialog(
								desktopPane,
								"Are you sure you wish to overwrite\n"
										+ file.getName() + "?",
								"Confirm overwrite", JOptionPane.YES_NO_OPTION);
						if (selection == JOptionPane.YES_OPTION)
							fileConfirmed = true;
						else
							fileConfirmed = false;
					}
				}
				System.out.println("file: " + file.getAbsolutePath());
				
				if(startMarker < endMarker){
					
				TTTProcessor.trim(recording, file, startMarker, endMarker,
						false);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}
	
	// Closing the editor frame
	private void close() {
	    instance = null;
		this.setVisible(false);
		this.dispose();
		TTT.getInstance();
	}

	/**
	 * Method (from TTT) which generates a formatted string representing a given
	 * time.
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
	 * Method (from TTT) which generates a formatted string representing a given
	 * time.
	 * <p />
	 * String returned will be in the form:-<br />
	 * <li><code>10:11</code> (min and sec)</li>
	 * <li><code>10:11.12</code> (min, sec and ms)</li> </ul>
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
		return (negative ? "-" : "")
				+ ((min < 10) && !negative ? "0" : "")
				+ min
				+ ":"
				+ (sec < 10 ? "0" : "")
				+ sec
				+ (includeMilliseconds ? "." + (msec < 100 ? "0" : "")
						+ (msec < 10 ? "0" : "") + msec : "");
	}

	public static String getStringFromTime(long nanosec,
			boolean includeMilliseconds) {
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
	 * <li><code>10:11.12</code> (TTT string, min, sec and ms)</li>
	 * </ul>
	 * 
	 * @return time extracted from the string, in milliseconds.
	 * @param value
	 *            String representing a suitably formatted time.
	 * @throws java.lang.NumberFormatException
	 */
	public static int getTimeFromString(String value)
			throws NumberFormatException {
		value = value.trim();
		int time = 0;
		if (value.endsWith("min"))
			time = 60000 * Integer.parseInt(value.substring(0,
					value.length() - 3));
		else if (value.endsWith("m"))
			time = 60000 * Integer.parseInt(value.substring(0,
					value.length() - 1));
		else if (value.endsWith("sec"))
			time = 1000 * Integer.parseInt(value.substring(0,
					value.length() - 3));
		else if (value.endsWith("s"))
			time = 1000 * Integer.parseInt(value.substring(0,
					value.length() - 1));
		else {
			int dumpf = value.indexOf(':');
			if (dumpf > 0) {
				time = 60000 * Integer.parseInt(value.substring(0, dumpf++));
				int dumpfer = value.indexOf('.');
				if (dumpfer > 0) {
					time += 1000 * Integer.parseInt(value.substring(dumpf,
							dumpfer++));
					time += Integer.parseInt(value.substring(dumpfer));
				} else
					time += 1000 * Integer.parseInt(value.substring(dumpf));
			} else
				time = Integer.parseInt(value);
		}
		return time;
	}

	// added 03.04.2006 by Peter Ziewer
	// bug in SUN Java 1.5 (Windows and Linux)
	// see Bug ID 6178755: REGRESSION: JOptionPane's showInternal*Dialog methods
	// never return
	// workaround: InternalDialog used by open must be invoked in event
	// dispatching thread
	private static int result = 0;

	public static int showInternalConfirmDialog(final Object message,
			final String title, final int optionType) {
		return showInternalConfirmDialog(message, title, optionType,
				JOptionPane.QUESTION_MESSAGE);
	}

	public static int showInternalConfirmDialog(final Object message,
			final String title, final int optionType, final int messageType) {

		// bug in SUN Java 1.5 (Windows and Linux)
		// see Bug ID 6178755: REGRESSION: JOptionPane's showInternal*Dialog
		// methods never return
		// workaround: InternalDialog used by open must be invoked in event
		// dispatching thread
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				result = JOptionPane.showInternalConfirmDialog(editor2
						.getInstance().getDesktopPane(), message, title,
						optionType, messageType);
			} else
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						result = JOptionPane.showInternalConfirmDialog(editor2
								.getInstance().getDesktopPane(), message,
								title, optionType, messageType);
					}
				});
		} catch (Exception e) {
			result = JOptionPane.CLOSED_OPTION;
		}

		return result;
	}

	/**
	 * for concatenating
	 */
	class Concatenator extends Thread {

		File[] desktopFiles;

		Concatenator(File[] desktopFiles) {
			this.desktopFiles = desktopFiles;
		}

		public void run() {
			IOProgressDisplayFrame progress = new IOProgressDisplayFrame(
					outputDisplay);
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

	private JCheckBox desktopCheckBox = new JCheckBox("Desktop");
	private JCheckBox audioCheckBox = new JCheckBox("Audio");
	private JCheckBox videoCheckBox = new JCheckBox("Video");

	/**
	 * SelectionPanel is used by the dialog confirming which files are to be
	 * opened. It includes check boxes for all the available audio/desktop/video
	 * files, to be used for confirmation as to whether they should be opened or
	 * not
	 */
	private class SelectionPanel extends JPanel {
		GridBagConstraints c = new GridBagConstraints();

		final String noFileText = "No file found";
		JTextPane videoPane;
		JTextPane desktopTextPane;
		JTextPane audioPane;

		SelectionPanel(File file) {
			super(new GridBagLayout());

			desktopTextPane = new JTextPane();
			audioPane = new JTextPane();
			videoPane = new JTextPane();

			if (file.exists()) {
				desktopTextPane.setText(file.getName());
				desktopCheckBox.setSelected(true);
				desktopCheckBox.setEnabled(false);
			} else {
				desktopCheckBox.setEnabled(false);
				desktopTextPane.setText(noFileText);
			}

			if (audioPresent) {
				audioPane.setText(TTTFileUtilities.checkForAudio(file)
						.getName());
				audioCheckBox.setSelected(true);
			} else {
				audioCheckBox.setEnabled(false);
				audioPane.setText(noFileText);
			}

			if (videoPresent) {
				videoPane.setText(TTTFileUtilities.checkForVideo(file)
						.getName());
				videoCheckBox.setSelected(true);
			} else {
				videoCheckBox.setEnabled(false);
				videoPane.setText(noFileText);
			}

			desktopTextPane.setEnabled(false);
			desktopTextPane.setDisabledTextColor(Color.BLACK);
			desktopTextPane.setOpaque(false);
			audioPane.setEnabled(false);
			audioPane.setDisabledTextColor(Color.BLACK);
			audioPane.setOpaque(false);
			videoPane.setEnabled(false);
			videoPane.setDisabledTextColor(Color.BLACK);
			videoPane.setOpaque(false);

			setBorder(new CompoundBorder(
					new EmptyBorder(20, 20, 20, 20),
					new TitledBorder("Choose which files to open in the editor")));
			c.weightx = 0.5;
			c.weighty = 0.5;
			c.anchor = GridBagConstraints.LINE_START;
			c.gridx = 0;
			c.gridy = 1;
			add(desktopCheckBox, c);
			c.gridx = 1;
			c.gridy = 1;
			add(desktopTextPane, c);
			c.gridx = 0;
			c.gridy = 2;
			add(audioCheckBox, c);
			c.gridx = 1;
			c.gridy = 2;
			add(audioPane, c);
			c.gridx = 0;
			c.gridy = 3;
			add(videoCheckBox, c);
			c.gridx = 1;
			c.gridy = 3;
			add(videoPane, c);
		}
	}

}
