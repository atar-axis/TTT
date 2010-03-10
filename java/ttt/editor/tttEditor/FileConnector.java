package ttt.editor.tttEditor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.Player;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import ttt.video.VideoCreator;

/**
 * Used to search for valid files to be opened, and to connect and initialize those files, as well as to perform any
 * necessary clean-up when the files are closed.
 */
public class FileConnector {

    // the files currently being used - media files may be null
    private File desktopFile;
    private File audioFile;
    private File videoFile;

    // whether each type of file has been found
    private boolean desktopPresent = false;
    private boolean audioPresent = false;
    private boolean videoPresent = false;

    // viewers used by this file connector
    private VideoViewer videoViewer = null;
    private AudioViewer audioViewer = null;
    private DesktopViewer desktopViewer = null;

    // other objects required for file connection
    private TTTFileData desktopFileData = null;
    private MarkerList markerList = null;
    private PlaybackController playbackController = null;

    // notifies the file connector that the program wishes to close this connection,
    // so the connector should be removed when it is written
    private boolean closeAfterWriting = false;
    String basicFileName;
    /**
     * Class constructor.
     * 
     * @param file
     *            A TTT (desktop) file.
     */
    public FileConnector(File file) {

        // gets the file name (path) without the suffix - used to search for other files
         basicFileName = file.getAbsolutePath();
        int suffixIndex = basicFileName.lastIndexOf('.');
        basicFileName = basicFileName.substring(0, suffixIndex);

        // determine what sort of file has been passed, and begin processing accordingly
        int fileType = TTTFileUtilities.getFileType(file);

        if (fileType != TTTFileUtilities.DESKTOP) {
            System.out.println("Invalid file chosen");
            return;
        }

        desktopFile = file;
        desktopPresent = true;
        	
        audioFile = TTTFileUtilities.checkForAudio(basicFileName);
        if (audioFile != null){
            audioPresent = true;}
        
        videoFile = TTTFileUtilities.checkForVideo(basicFileName);
        if (videoFile == null) {
        	//check for bjpg video. If found ask to convert.
        	File newVideo = new File(TTTFileUtilities.getBasicFileName(file) + Parameters.unprocessedVideoEndings[0]);
            if (newVideo.exists()){        		
                if (TTTEditor.showInternalConfirmDialog("Unprocessed Video File detected. Unprocessed Video Files can't be Edited. Do you want to Process it now? (Recommended this may take a while)",
                        "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                {
                videoFile = newVideo;
                VideoCreator newVid = new VideoCreator();         
				newVid.setCallingFileConnector(this);
				newVid.create(newVideo.getAbsolutePath());						  				
              }
        	}
        }        
        if (videoFile != null) {
            videoPresent = true;
            }
        
    }

    /**
     * Presents a dialog box for the user to select which of the available files should be opened. If 3 files (audio,
     * video and desktop) are not all selected, a warning dialog is presented to the user to confirm that they wish to
     * proceed. If no files are selected, the user is notified of this through a dialog and <code>false</code> is
     * returned to indicate that the connection was unsuccessful.
     * 
     * @return <code>true</code> if there are files available and the user makes appropriate choices (that is, they
     *         confirm at least one file to be opened), <code>false</code> otherwise
     */
    private int result; // placed here to be accessable in anonymous Runnable (see below)

 
 public SelectionPanel selectionPanel; 
 
    public boolean connectFiles() {
        if (desktopFile == null)
            return false;

        selectionPanel  = new SelectionPanel();

                result = TTTEditor.showInternalConfirmDialog(
                        selectionPanel, "Confirm", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            desktopPresent = desktopCheckBox.isSelected();
            audioPresent = audioCheckBox.isSelected();
            videoPresent = videoCheckBox.isSelected();
            // If none are selected, notify the user and return false to indicate that file connection has been
            // unsuccessful
            if (!desktopPresent && !audioPresent && !videoPresent) {
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                        "No files have been selected.");
                return false;
            }
            // If at least one of the 3 types of media are not selected, warn user and give the option of continuing or
            // not
            // Note: could amend this if the lack of video is thought of as not too serious
            else if (!desktopPresent || !audioPresent || !videoPresent) {
                String warningString = "Failing to edit audio, video and desktop together "
                        + "\nmay mean that it will not be possible to synchronize them later."
                        + "\n\nAre you sure you wish to proceed?";
                // If the user wishes to continue, return true to indicate that the file connection has been successful
                // (although not all 3 files have been selected)
                if (TTTEditor.showInternalConfirmDialog( warningString,
                        "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    // ensure files are ignored if not selected
                    if (!videoCheckBox.isSelected())
                        videoFile = null;
                    if (!audioCheckBox.isSelected())
                        audioFile = null;
                    return true;
                }
                // If the user has selected not to proceed with opening the files, return false
                else
                    return false;
            }
            // If all 3 files are available and have been selected, return true for a successful file connection
            else {
                return true;
            }
        }
        // If the user has not selected to open the files, return false
        return false;
    }

    /**
     * Get the <code>VideoViewer</code> used by this <code>FileConnector</code>.
     * 
     * @return the <code>VideoViewer</code>, may be <code>null</code>.
     */
    public VideoViewer getVideoViewer() {
        return videoViewer;
    }

    //notify that the bjpg has been processed into .mov
    public void setVideo(){
    	  videoFile = TTTFileUtilities.checkForVideo(basicFileName);
          if (videoFile != null) {
              videoPresent = true;
              }
          if (videoPresent) {        	    
           selectionPanel.videoPane.setText(".mov");        
              videoCheckBox.setSelected(true);
              videoCheckBox.setEnabled(true);
          } else {
              videoCheckBox.setEnabled(false);
              selectionPanel.videoPane.setText("No file found");
          }
    }
    
    /**
     * Get the <code>AudioViewer</code> used by this <code>FileConnector</code>.
     * 
     * @return the <code>AudioViewer</code>, may be <code>null</code>.
     */
    public AudioViewer getAudioViewer() {
        return audioViewer;
    }

    /**
     * Starts the reading of the desktop file, using another thread. When complete, creates <code>AudioViewer</code>
     * and <code>VideoViewer</code> as required, designates the <code>mainPlayer</code> and calls
     * <code>prefetch()</code> on that <code>Player</code>.
     */
    public void readFiles() {
        Reader reader = new Reader();
        reader.start();
    }

    /**
     * Called after the all appropriate files had been read and players prefetched, calculates durations and creates the
     * <code>DesktopViewer</code> to be used for displaying the desktop. Also requests that the <code>TTTEditor</code>
     * displays the available viewers.
     * 
     * @param fileData
     *            the <code>TTTFileData</code> read from the desktop file.
     */
    protected void completeInitialization(TTTFileData fileData) {

        if (fileData == null) {
            requestRemoveConnector();
            return;
        }

        this.desktopFileData = fileData;

        System.out.println("Calculating duration...");

        // initialize the players
        Player mainPlayer = initializePlayers();

        // check sync
        int max_duration = calculateMaximumDuration();
        if (audioViewer != null) {
            int audioDuration = (int) (audioViewer.getPlayer().getDuration().getSeconds() * 1000);
            if (audioDuration > 0 && audioDuration + 3000 < max_duration) {
                fileData.header.synchRatio = (double) audioDuration / max_duration;
                System.out.println("\nNOTE: Setting synchronization possible.");
            }
        }

        System.out.println("Creating viewers...");

        // create and display objects / viewers
        DesktopPanel desktopPanel = new DesktopPanel(fileData.header.framebufferWidth,
                fileData.header.framebufferHeight);

        playbackController = new PlaybackController(fileData, desktopPanel, mainPlayer, max_duration);
        markerList = new MarkerList(playbackController, fileData.header);
        desktopViewer = new DesktopViewer(fileData, playbackController, desktopPanel, markerList, this);

        if (videoViewer != null)
            videoViewer.setTitle(fileData.header.desktopName);

        if (SwingUtilities.isEventDispatchThread())
            TTTEditor.getInstance().updateGUI(desktopViewer, videoViewer);
        else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TTTEditor.getInstance().updateGUI(desktopViewer, videoViewer);
                }
            });
        }
    }

    private int desktopDuration, audioDuration, videoDuration;

    // gets the maximum from video / desktop durations
    private int calculateMaximumDuration() {
        System.out.println("\n\t  duration\tdifference");
        
        desktopDuration = desktopFileData.index.getLastMessageTimestamp();
        
        int max_duration = Math.max(desktopDuration, Math.max(videoDuration, audioDuration));        
        
        System.out.println("desktop: " + TTTEditor.getStringFromTime(desktopDuration, true) +"\t "+TTTEditor.getStringFromTime(desktopDuration-max_duration, true) );
        
        if (audioViewer != null)
            System.out.println("audio:\t " + TTTEditor.getStringFromTime(audioDuration, true) +"\t "+TTTEditor.getStringFromTime(audioDuration-max_duration, true) );
        else
            System.out.println("no audio");
        
        if (videoViewer != null)
            System.out.println("video:\t " + TTTEditor.getStringFromTime(videoDuration, true) +"\t "+TTTEditor.getStringFromTime(videoDuration-max_duration, true) );
        else
            System.out.println("no video");
        
        System.out.println();

        // modified by Ziewer 24.03.2006
        return Math.max(desktopDuration, Math.max(videoDuration, audioDuration));
        // return desktopDuration > videoDuration ? desktopDuration : videoDuration;
    }

    // initializes the players and returns the main (controlling) player
    private Player initializePlayers() {
        System.out.println("Initializing players...");

        Player mainPlayer = null;
        /*if (videoPresent) {
            System.out.println("    Video Player: "+videoFile);
            videoViewer = new VideoViewer(videoFile, this);
            videoDuration = (int) (videoViewer.getPlayer().getDuration().getSeconds() * 1000);

            mainPlayer = videoViewer.getPlayer();
        }*/
        if (audioPresent) {
            System.out.println("    Audio Player: "+audioFile);
            audioViewer = new AudioViewer(audioFile);
            audioDuration = (int) (audioViewer.getPlayer().getDuration().getSeconds() * 1000);

            mainPlayer = audioViewer.getPlayer();
            if (videoPresent) {
                try {
                    mainPlayer.addController(videoViewer.getPlayer());
                } catch (Exception e) {
                    System.out.println("Unable to add controller" + e);
                }
            }
        }
        if (mainPlayer != null) {
            System.out.println("    Prefetching main player");
            mainPlayer.prefetch();
        }

        return mainPlayer;
    }

    private void requestRemoveConnector() {
        TTTEditor.getInstance().removeFileConnector(this);
    }

    /**
     * Pauses any <code>Player</code>s currently playing in this file connection.
     */
    public void pausePlayer() {
        if (playbackController != null)
            playbackController.setPaused();
    }

    /**
     * Refresh the display in response to a change in synchronization status - changing synchronization may also change
     * the length of playback, which must be reflected in displays such as timelines.
     */
    protected void refreshSynchStatus() {
        playbackController.updateSyncStatus();
        desktopViewer.refreshSlider();
    }

    /**
     * Performs any processing necessary when closing files which have been connected using this
     * <code>FileConnector</code>. Stops, deallocates and closes any <code>Player</code>s. Calls the
     * <code>terminate</code> method of any <code>DesktopViewer</code> currently being used, and calls
     * <code>stopRunning</code> on any <code>PlaybackController</code>.
     */
    public synchronized void endConnection() {
        if (desktopViewer != null) {
            desktopViewer.dispose();
            TTTEditor.getInstance().getDesktopPane().remove(desktopViewer);
            desktopViewer = null;
        }
        if (videoViewer != null) {
            videoViewer.dispose();
            TTTEditor.getInstance().getDesktopPane().remove(videoViewer);
            videoViewer = null;
        }
        if (playbackController != null)
            playbackController.terminate();
        playbackController = null;
        // the index may be being retained, if a file has just been processed
        // so only remove the listeners registered to it
        desktopFileData.index.resetIndexListeners();
        detailsPanel = null;
        audioViewer = null;
    }

    /**
     * Gets the desktop title.
     * 
     * @return The titled of the desktop, as stored in the saved file.
     */
    public String getDesktopTitle() {
        return desktopFileData.header.desktopName;
    }

    /**
     * Sets the desktop title for dislay in both the <code>DesktopViewer</code> and in the <code>VideoViewer</code>,
     * and also for future saving
     * 
     * @param title
     *            the title to be stored
     */
    public void setDesktopTitle(String title) {
        desktopFileData.header.desktopName = title;
        desktopViewer.setTitle(title);
        if (videoViewer != null)
            videoViewer.setTitle(title);
    }

    /**
     * Get the <code>MarkerList</code> used by this desktop file for editing purposes.
     * 
     * @return the required <code>MarkerList</code>
     */
    public MarkerList getMarkers() {
        return markerList;
    }

    // save the complete destkop file without trimming
    private void saveFullDesktopFile(File file, boolean closeFile) {
        closeAfterWriting = closeFile;
        try {
            // create backup file
            if (file.exists() && Parameters.createBackups) {
                TTTFileUtilities.renameForBackup(file);
            }
            Writer writer = new Writer(file);
            writer.start();
        } catch (Exception e) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "Error writing file to disk.\n" + e, "Save error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // writing is complete - file connection can now be closed if desired
    private void writingComplete() {
        if (closeAfterWriting) {
            endConnection();
            TTTEditor.getInstance().removeFileConnector(this);
            closeAfterWriting = false;
        }
    }

    /**
     * Attempts to save the files involved in this <code>FileConnector</code>, including any edits which have been
     * made as desired.
     * 
     * @param outputFile
     *            the file used for output (should end with appropriate desktop suffix - this will be amended for any
     *            media files which are available)
     * @param processTrim
     *            <code>true</code> if the <code>Marker</code>s for start and end times should be heeded in the new
     *            files which are output, </code>false</code> if they should be ignored and the whole file output.
     * @param subDivide
     *            <code>true</code> if the <code>Marker</code>s for subdivision times should be heeded in the new
     *            files which are output, </code>false</code> if they should be ignored and only one desktop file (and
     *            corresponding media files) created.
     * @param closeAfterWriting
     *            <code>true</code> if the connection should be ended and viewers closed after the file is saved,
     *            <code>false</code> otherwise.
     */
    protected synchronized void saveFile(File outputFile, boolean processTrim, boolean subDivide,
            boolean closeAfterWriting) {
        // create a file with proper ttt desktop suffix
        outputFile = TTTFileWriter.forceDesktopFileToDefaultEnding(outputFile);

        // cannot subdivide if no markers set for that
        if (!markerList.subdivisionsPresent())
            subDivide = false;

        // if only saving the file without processing
        if (!processTrim || markerList.isUntrimmed())
            if (!subDivide) {
                saveFullDesktopFile(outputFile, closeAfterWriting);
                if (!outputFile.getAbsolutePath().equals(desktopFile.getAbsolutePath()))
                    copyAvailableMediaFiles(outputFile);
                return;
            }

        // test if processing is possible, or if the markers are too close together
        if (subDivide) {
            if (markerList.markersTooClose()) {
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                        "Cannot save file:\nSome of the markers are too close together.\n" + "There must be at least "
                                + markerList.MARKER_SPACE_REQUIRED + "ms between each.", "Save Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            if (markerList.trimsTooClose()) {
                JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                        "Cannot save file:\nThe trim start and end times are too close together.\n"
                                + "There must be at least " + markerList.MARKER_SPACE_REQUIRED + "ms between them.",
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // start file processing
        try {

            // don't end the connection if subdividing - keep current file open
            endConnection();

            this.closeAfterWriting = closeAfterWriting;

            // if the file is being saved over a previous file, need to rename the media files
            // so that the proper name can be used to write to
            if (!subDivide
                    && TTTFileUtilities.getBasicFileName(outputFile).equals(
                            TTTFileUtilities.getBasicFileName(desktopFile))) {
                if (videoFile != null) {
                    videoFile = TTTFileUtilities.renameForBackupProcessing(videoFile);
                }
                if (audioFile != null)
                    audioFile = TTTFileUtilities.renameForBackupProcessing(audioFile);
            }

            // start the actual file writing
            Processor processor = new Processor(this, outputFile, subDivide);
            processor.start();
        } catch (Exception e) {
            JOptionPane.showInternalMessageDialog(TTTEditor.getInstance().getDesktopPane(),
                    "Error writing file to disk.\n" + e, "Save error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // if full file is being saved with no processing, just copy media files
    // to new name (if appropriate) rather than re-generating them
    private void copyAvailableMediaFiles(File file) {

        String outputName = TTTFileUtilities.getBasicFileName(file);

        if (videoFile != null) {
            try {
                copyMediaFile(videoFile, outputName);
            } catch (IOException e) {
                System.err.println("Unable to copy video file!" + e);
            }
        }

        if (audioFile != null) {
            try {
                copyMediaFile(audioFile, outputName);
            } catch (IOException e) {
                System.err.println("Unable to copy audio file!" + e);
                e.printStackTrace();
            }
        }

    }

    // copy a media file as it is, giving it a new name
    private void copyMediaFile(File mediaFile, String outputName) throws IOException {
        String outputSuffix;

        outputSuffix = TTTFileUtilities.getFileSuffix(mediaFile);
        if (outputName.equals(TTTFileUtilities.getBasicFileName(mediaFile))) {
            // being saved to same name - create backup or do nothing
            if (Parameters.createBackups) {
                TTTFileUtilities.renameForBackup(mediaFile);
                copyFile(mediaFile, new File(outputName + outputSuffix));
            }
        } else {
            File outputFile = new File(outputName + outputSuffix);
            if (outputFile.exists()) {
                if (Parameters.createBackups) {
                    TTTFileUtilities.renameForBackup(mediaFile);
                    copyFile(mediaFile, new File(outputName + outputSuffix));
                } else {
                    int selection = TTTEditor.showInternalConfirmDialog(
                            outputFile.getName() + " exists.\nDo you wish to overwrite it?", "Save file",
                            JOptionPane.YES_NO_OPTION);
                    if (selection == JOptionPane.YES_OPTION)
                        copyFile(mediaFile, outputFile);
                }
            } else
                copyFile(mediaFile, outputFile);
        }
    }

    // copy any file
    private void copyFile(File inputFile, File outputFile) throws IOException {
        InputStream in = new FileInputStream(inputFile);
        OutputStream out = new FileOutputStream(outputFile);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Gets the file containing the desktop.
     * 
     * @return The TTT file used by the <code>FileConnector</code>
     */
    public File getDesktopFile() {
        return desktopFile;
    }

    /**
     * Get the <code>DesktopViewer</code> used by this <code>FileConnector</code>.
     * 
     * @return the <code>DesktopViewer</code>.
     */
    public DesktopViewer getDesktopViewer() {
        return desktopViewer;
    }

    /**
     * Get the file from which the video data has been read.
     * 
     * @return the <code>File</code> used by the video of this <code>FileConnector</code>. May be null.
     */
    public File getVideoFile() {
        return videoFile;
    }

   
    
    /**
     * Get the file from which the audio data has been read.
     * 
     * @return the <code>File</code> used by the audio of this <code>FileConnector</code>. May be null.
     */
    public File getAudioFile() {
        return audioFile;
    }

    /**
     * Get the TTT file data for this FileConnection.
     * 
     * @return a <code>TTTFileData</code> object containing the data for the current desktop file
     */
    public TTTFileData getFileData() {
        return desktopFileData;
    }

    // panel containing details about this file connection
    private DetailsPanel detailsPanel = null;

    /**
     * Get a <code>JPanel</code> which contains formatted output with details relating to the files which are used by
     * this <code>FileConnector</code>, including specific details of the TTT recording.
     * 
     * @return the <code>DetailsPanel</code> relating to this <code>FileConnector</code>
     */
    public DetailsPanel getDetailsPanel() {
        if (detailsPanel == null)
            detailsPanel = new DetailsPanel(this);
        return detailsPanel;
    }

    // SelectionPanel is used by the dialog confirming which files are to be opened.
    // It includes check boxes for all the available audio/desktop/video files,
    // to be used for confirmation as to whether they should be opened or not
    private JCheckBox desktopCheckBox = new JCheckBox("Desktop");
    private JCheckBox audioCheckBox = new JCheckBox("Audio");
    private JCheckBox videoCheckBox = new JCheckBox("Video");

    private class SelectionPanel extends JPanel {
    	GridBagConstraints c = new GridBagConstraints();
    	    	
        final String noFileText = "No file found";
        JTextPane videoPane;
        JTextPane desktopTextPane;
        JTextPane audioPane;
        SelectionPanel() {
            super(new GridBagLayout());
            

             desktopTextPane = new JTextPane();
             audioPane = new JTextPane();
            videoPane = new JTextPane();

            if (desktopPresent) {
                desktopTextPane.setText(desktopFile.getName());
                desktopCheckBox.setSelected(true);
                desktopCheckBox.setEnabled(false);
            } else {
                desktopCheckBox.setEnabled(false);
                desktopTextPane.setText(noFileText);
            }

            if (audioPresent) {
                audioPane.setText(audioFile.getName());
                audioCheckBox.setSelected(true);
            } else {
                audioCheckBox.setEnabled(false);
                audioPane.setText(noFileText);
            }

            if (videoPresent) {
                videoPane.setText(videoFile.getName());
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

            setBorder(new CompoundBorder(new EmptyBorder(20, 20, 20, 20), new TitledBorder(
                    "Choose which files to open in the editor")));
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

    // for reading desktop files
    class Reader extends Thread {

        private TTTFileData fileData = null;
        private IOProgressDisplayFrame progress = new IOProgressDisplayFrame(TTTEditor.getInstance()
                .getOutputDisplayPanel());

        public void run() {
            try {
            	TTTEditor.getInstance().addToGlassPane(progress);
                fileData = TTTFileReader.readFile(desktopFile);  
                completeInitialization(fileData);
            } catch (Exception e) {
                System.out.println("Unable to read file: " + e);
                e.printStackTrace();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        requestRemoveConnector();
                    }
                });
                progress.setCompleted();
                return;
            } catch (OutOfMemoryError error) {
                progress.setCompleted();               
                requestRemoveConnector();
                return;
            }
            // complete the initialization, returning to the event thread
            System.out.println("\nDone reading file.\n\n");
            progress.dispose();
        }
    }

    // for saving a file where no processing is involved
    // only the desktop file will be saved
    class Writer extends Thread {

        private File file;
        private IOProgressDisplayFrame progress;

        Writer(File file) {
            this.file = file;
            progress = new IOProgressDisplayFrame(TTTEditor.getInstance().getOutputDisplayPanel());
        }

        public void run() {
            try {
                TTTEditor.getInstance().addToGlassPane(progress);
                TTTFileWriter.writeFile(desktopFileData, file);
                writingComplete();
                System.out.println("\nDone!\n\nPress OK to continue.\n\n\n");
            } catch (Exception e) {
                System.out.println("Unable to write file." + e);
                e.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progress.setCompleted();
                    }
                });
            }
        }

    }

    // for saving a file where trimming / subdividing is also involved
    class Processor extends Thread {

        private File file;
        private FileConnector fileConnector;
        private boolean divide;
        private IOProgressDisplayFrame progress;

        Processor(FileConnector fileConnector, File outputFile, boolean divide) {
            this.fileConnector = fileConnector;
            this.file = outputFile;
            this.divide = divide;
            progress = new IOProgressDisplayFrame(TTTEditor.getInstance().getOutputDisplayPanel());
        }

        public void run() {
            try {
                TTTEditor.getInstance().addToGlassPane(progress);

                if (divide)
                    TTTProcessor.subDivide(fileConnector, file);
                else {
                    TTTProcessor.trim(fileConnector, file);
                    if (!closeAfterWriting)
                        correctFiles();
                }
                System.out.println("Writing complete!");
                if (!closeAfterWriting)
                    if (divide)
                        requestRemoveConnector();
                    else
                        completeInitialization(desktopFileData);
            } catch (Exception e) {
                System.out.println("Unable to write file." + e);
                e.printStackTrace();
            } catch (OutOfMemoryError error) {
                progress.setCompleted();
                return;
            } finally {
                writingComplete();
                System.out.println("\nPress OK to continue.\n\n\n");
                progress.setCompleted();
            }
        }

        // corrects file names of saved files,
        // updates the files stored currently in this FileConnector
        // THE RENAMING DOES NOT ACTUALLY WORK AT THIS TIME...
        private void correctFiles() {
            // if saving over previous files (without using new names),
            // correct backups and stored files in FileConnector
            if (TTTFileUtilities.getBasicFileName(file).equals(TTTFileUtilities.getBasicFileName(desktopFile))) {
                // DOESN'T WORK!!!!
                if (audioFile != null) {
                    if (Parameters.createBackups) {
                        // removes the final suffix
                        File audioBackup = new File(TTTFileUtilities.getBasicFileName(audioFile));
                        audioFile.renameTo(audioBackup);
                    } else {
                        audioFile.delete();
                    }
                }
                // DOESN'T WORK!!!!
                if (videoFile != null) {
                    if (Parameters.createBackups) {
                        // removes the final suffix
                        File videoBackup = new File(TTTFileUtilities.getBasicFileName(videoFile));
                        videoFile.renameTo(videoBackup);
                    } else {
                        videoFile.delete();
                    }
                }
            }
            desktopFile = file;
            if (audioFile != null) {
                // ADDED 25.10.2007 by Ziewer
                // NOTE: also change in TTTProcessor.trim()
                // if available, use same encoding as source 
                String ending = Parameters.audioEndings[0];
                for (String audioEncoding : Parameters.audioEndings) {
                    if(audioFile.getName().endsWith(audioEncoding)) {
                            ending=audioEncoding;
                            break;
                    }
                }
                File newAudio = new File(TTTFileUtilities.getBasicFileName(file) + ending);
                // end ADDED 25.10.2007
                // WAS:
                // File newAudio = new File(TTTFileUtilities.getBasicFileName(file) + Parameters.audioEndings[0]);
                if (newAudio.exists())
                    audioFile = newAudio;
            }
            if (videoFile != null) {
                File newVideo = new File(TTTFileUtilities.getBasicFileName(file) + Parameters.videoEndings[0]);
                if (newVideo.exists())
                    videoFile = newVideo;
            }
            

            // ensure details panel is set to null, so will be recreated with current data if ever called for
            detailsPanel = null;
        }
    }

}
