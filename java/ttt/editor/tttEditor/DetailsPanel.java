package ttt.editor.tttEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.util.Date;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

/**
 * A <code>JPanel</code> which can display formatted file
 * details, given an appropriate <code>FileConnector</code>
 */
public class DetailsPanel extends JPanel {
    
    /**
     * Class constructor.
     * @param connector the <code>FileConnector</code> whose details
     * should be shown.
     */
    public DetailsPanel(FileConnector connector) {
        super(new BorderLayout());
        TTTFileData fileData = connector.getFileData();
        File desktopFile = connector.getDesktopFile();
        File videoFile = connector.getVideoFile();
        File audioFile = connector.getAudioFile();
        
        Component files = getFileDetails(desktopFile, videoFile, audioFile);
        Component duration = getDurationDetails(fileData.index,
                connector.getVideoViewer(), connector.getAudioViewer());
        Component desktop = getDesktopDetails(fileData.header);
        Component messages = getMessageDetails(fileData.index);
        Component indexes = getIndexDetails(fileData.extensions, fileData.index);
        Component search = getSearchDetails(fileData.index);
        
        JPanel gridPanel = new JPanel(new GridLayout(1,2));
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());
        leftPanel.add(desktop, BorderLayout.CENTER);
        leftPanel.add(messages, BorderLayout.SOUTH);
        rightPanel.add(duration, BorderLayout.NORTH);
        rightPanel.add(indexes, BorderLayout.CENTER);
        rightPanel.add(search, BorderLayout.SOUTH);
        gridPanel.add(leftPanel);
        gridPanel.add(rightPanel);
        add(files, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
    }
    
 
    private Component getFileDetails(File desktopFile, File videoFile, File audioFile) {
        JPanel fileBox = new JPanel();
        fileBox.setBorder(new TitledBorder("Files"));
        JTextArea textArea = new JTextArea();
        if (desktopFile != null)
            textArea.append(desktopFile.getName());
        else
            textArea.append("No desktop file present.");
        if (videoFile != null)
            textArea.append("\n" + videoFile.getName());
        else
            textArea.append("\nNo video file present.");
        if (audioFile != null)
            textArea.append("\n" + audioFile.getName());
        else
            textArea.append("\nNo audio file present.");
        textArea.setOpaque(false);
        textArea.setEditable(false);
        fileBox.add(textArea);
        return fileBox;
    }
    
    private Component getDurationDetails(Index index, VideoViewer videoViewer, AudioViewer audioViewer) {
        JPanel durationPanel = new JPanel();
        durationPanel.setBorder(new TitledBorder("Durations"));
        JTextArea textArea = new JTextArea();
        textArea.append("Desktop:\t" + TTTEditor.getStringFromTime(index.getLastMessageTimestamp(), true));
        if (videoViewer != null) {
            int videoDuration = (int)(videoViewer.getPlayer().getDuration().getSeconds() * 1000);
            textArea.append("\nVideo:\t" + TTTEditor.getStringFromTime(videoDuration, true));
        }
        if (audioViewer != null) {
            int audioDuration = (int)(audioViewer.getPlayer().getDuration().getSeconds() * 1000);
            textArea.append("\nAudio:\t" + TTTEditor.getStringFromTime(audioDuration, true));
        }
        textArea.setOpaque(false);
        textArea.setEditable(false);
        durationPanel.add(textArea);
        return durationPanel;
    }
    
    private Component getDesktopDetails(Header header) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBorder(new TitledBorder("Desktop details"));
        
        JTextArea textArea = new JTextArea();
        textArea.append("TTT Version:\t" + header.versionMsg + "\n");
        textArea.append("Desktop name:\t" + header.desktopName + "\n");
        textArea.append("Recorded:\t" + new Date(header.startTime) + "\n");
        textArea.append("Desktop size:\t" + header.framebufferWidth
                + " * " + header.framebufferHeight + "\n");
        if (header.colors != null)
            textArea.append("No. colours:\t" + header.colors.length + "\n");
        else
            textArea.append("Colours:\t\t24 BIT");
        textArea.append("Bits per pixel:\t" + header.bitsPerPixel);
        
        textArea.setOpaque(false);
        textArea.setEditable(false);
        detailsPanel.add(textArea);
        
        return detailsPanel;
    }
    
    private Component getMessageDetails(Index index) {
        JPanel messagesPanel = new JPanel();
        messagesPanel.setBorder(new TitledBorder("Messages"));
        JTextArea textArea = new JTextArea("Total number of messages at start up: "
                + index.getTotalMessageCount());
        textArea.setOpaque(false);
        textArea.setEditable(false);
        messagesPanel.add(textArea);
        return messagesPanel;
    }
    
    //whether original file contains an index extension
    //and current index size
    private Component getIndexDetails(List<byte[]> extensions, Index index) {
        JPanel indexPanel = new JPanel();
        indexPanel.setBorder(new TitledBorder("Index"));
        JTextArea textArea = new JTextArea();
        if (extensions == null)
            textArea.append("No extensions have been saved.");
        else {
            int entries = -1;
            for (int i = 0; i < extensions.size(); i++) {
                byte[] extension = (byte[])extensions.get(i);
                if (extension[0] == ProtocolConstants.EXTENSION_INDEX_TABLE) {
                    int ch1 = extension[1];
                    int ch2 = extension[2];
                    entries = (ch1 << 8) + (ch2 << 0);;
                    break;
                }
            }
            if (entries != -1)
                textArea.append("Index with " + entries + " entries saved in extension.");
            else {
                textArea.append("No index saved in extension.");
                textArea.append("\n\nIndex created with " + index.size() + (index.size() == 1 ? " entry" : " entries"));
            }
        }
        textArea.setOpaque(false);
        textArea.setEditable(false);
        indexPanel.add(textArea);
        return indexPanel;
    }
    
    private Component getSearchDetails(Index index) {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder("Search"));
        JTextArea textArea = new JTextArea();
        if (index.isSearchable())
            textArea.append("Search text is available.");
        else
            textArea.append("No search text has been saved.");
        textArea.setOpaque(false);
        textArea.setEditable(false);
        searchPanel.add(textArea);
        return searchPanel;
    }
    
}
