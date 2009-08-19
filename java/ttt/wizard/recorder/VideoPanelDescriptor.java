/* VideoPanelDescriptor.java
 * Created on 24. April 2007, 15:01
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.recorder;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import ttt.wizard.framework.WizardPanelDescriptor;

public class VideoPanelDescriptor extends WizardPanelDescriptor implements ActionListener {

    private String m_InfoText_EN = "Below you should see the video stream of your "
            + "attached camera. You can adjust the camera to get an impression "
            + "of the area which will be captured. If no device or stream can be "
            + "found you can go on and video capturing will be disabled.";

    private String m_InfoText_DE = "Im Video-Fenster wird das erhaltene Videosignal "
            + "angezeigt. Die angeschlossene Kamera kann nun auf den Ausschnitt der "
            + "aufgenommen werden soll, ausgerichtet werden. "
            + "Wenn kein Videogerät gefunden wird, kann der Wizard trotzdem fortgeführt werden. "
            + "Die Videoaufzeichnung wird in diesem Fall deaktiviert.";

    private String queringDevice_EN = "Querying capture devices... "
            + "JMF Registry is looking for Audio and Video Capture devices." + "This might take up to 15 minutes";

    private String queringDevice_DE = "Suche nach einer Videoquelle... "
            + "JMF Registry wird aktualisiert. Dies kann bis zu 15 Minuten dauern.";

    private String decisionQuering_EN = "This action may take up to 15 minutes "
            + "\nwithout responding. Continue detection?";

    private String decisionQuering_DE = "Die Suche kann bis zu 15 Minuten dauern. "
            + "Während dieser Zeit reagiert die Anwendung nicht. Fortsetzen?";

    public static final String IDENTIFIER = "VIDEOPANEL";
    private VideoPanel m_VideoPanel;
    private Vector m_VideoDevices;
    private CaptureDeviceInfo m_CaptureDeviceInfo;
    // private Player videoPlayer;
    private String m_language;

    /**
     * Creates a new instance of VideoPanelDescriptor
     */
    public VideoPanelDescriptor(String language) {

        m_language = language;
        m_CaptureDeviceInfo = null;
        m_VideoPanel = new VideoPanel();
        m_VideoPanel.diplayVideoPanel1.detectVideoDevicesButton.addActionListener(this);
        m_VideoPanel.diplayVideoPanel1.retryButton.addActionListener(this);
        m_VideoPanel.diplayVideoPanel1.setLanguage(language);

        setObjectIdentifier(IDENTIFIER);
        setComponent(m_VideoPanel);

        if (language == "DE")
            m_VideoPanel.InfoTextArea.setText(m_InfoText_DE);
        else
            m_VideoPanel.InfoTextArea.setText(m_InfoText_EN);
    }

    public Object getLastComponentDescriptor() {
        return "OPTIONPANEL";
    }

    public Object getNextComponentDescriptor() {
        return "INFOPANEL";
    }

    /**
     * Before displaying the panel reset all buttons and load the video
     */
    public void beforeDisplayingPanel() {

        this.restoreButtonsToPanelState();
        loadVideo();
    }

    /**
     * Gives all resources of the player free - used for example if the video is reloaded
     */
    public void deallocatePlayerResource() {
        m_VideoPanel.diplayVideoPanel1.givePlayerResourcesFree();
    }

    /**
     * Called if the retry button is used
     * 
     * @param e
     *            event of the retry button
     */
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == m_VideoPanel.diplayVideoPanel1.retryButton) {

            // set capture device null because no new video will be loaded
            m_CaptureDeviceInfo = null;
            loadVideo();
        }

        else if (e.getSource() == m_VideoPanel.diplayVideoPanel1.detectVideoDevicesButton) {

            // inform user about processing
            JDialog dialog = new JDialog(this.getWizardController());
            dialog.setSize(200, 100);
            dialog.setResizable(false);
            JTextArea text = new JTextArea();
            text.setEditable(false);
            Font font = new Font("Arial Unicode MS 14 Plain", Font.PLAIN, 12);
            text.setFont(font);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setSize(200, 100);
            text.setBackground(java.awt.SystemColor.control);

            if (m_language == "EN") {
                dialog.setTitle("Detect Capture Devices");
                text.setText(queringDevice_EN);
            } else {
                dialog.setTitle("Suche Videoquelle");
                text.setText(queringDevice_DE);
            }

            dialog.add(text);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

            int decision;
            if (m_language == "EN") {
                decision = JOptionPane.showConfirmDialog(null, decisionQuering_EN, "Inane warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                decision = JOptionPane.showConfirmDialog(null, decisionQuering_DE, "Warnung",
                        JOptionPane.WARNING_MESSAGE);
            }

            if (decision == 0) {
                detectVideoDevices();
                // set capture device null because no new video will be loaded
                m_CaptureDeviceInfo = null;
                loadVideo();
            }

            // end processing dialog
            dialog.setVisible(false);
        }
    }

    /**
     * loads the video - this is done in an a new thread because it might take more time
     */
    private void loadVideo() {

        if (m_CaptureDeviceInfo == null) {
            boolean deviceFound = findCaptureDevices();

            // if at least one device was found try to get the video stream
            if (deviceFound) {
                m_CaptureDeviceInfo = (CaptureDeviceInfo) m_VideoDevices.firstElement();

                SwingWorker worker = new SwingWorker<Boolean, Void>() {
                    protected Boolean doInBackground() throws Exception {
                        m_VideoPanel.diplayVideoPanel1.showVideo(m_CaptureDeviceInfo);
                        return true;
                    }
                };
                worker.execute();
            }

            // no devices were found
            else {
                m_VideoPanel.diplayVideoPanel1.callNoDevicePanel();
            }
        }
    }

    // finds all capture devices and filters out all video capture devices
    private boolean findCaptureDevices() {
        Vector m_Devices = CaptureDeviceManager.getDeviceList(null);
        m_VideoDevices = new Vector();

        // get all videodevices
        if (m_Devices != null && m_Devices.size() > 0) {
            int deviceCount = m_Devices.size();
            Format[] formats;
            for (int i = 0; i < deviceCount; i++) {
                m_CaptureDeviceInfo = (CaptureDeviceInfo) m_Devices.elementAt(i);
                formats = m_CaptureDeviceInfo.getFormats();
                for (int j = 0; j < formats.length; j++) {
                    if (formats[j] instanceof VideoFormat) {
                        m_VideoDevices.addElement(m_CaptureDeviceInfo);
                        break;
                    }
                }
            }
        }
        if (m_VideoDevices.size() == 0) {
            return false;
        } else
            return true;
    }

    /**
     * Returns if the video has successfully loaded
     * 
     * @return if the video has loaded and can be captured by the recorder
     */
    public boolean isVideoEnable() {
        if (m_VideoPanel.diplayVideoPanel1.showingPanel.isVisible())
            return true;
        else
            return false;
    }

    // FIXME this part of the code is from the jmf to register all found devices
    // It takes up to 15 minutes to finish this task. During this time no response
    // to the user can be given because if this part is in an own thread the java
    // virtual mashine gets crazy and fly away.
    private void detectVideoDevices() {
        Class auto = null;
        Class autoPlus = null;
        try {
            auto = Class.forName("VFWAuto");
        } catch (Exception e) {}
        if (auto == null) {
            try {
                auto = Class.forName("SunVideoAuto");
            } catch (Exception ee) {}
            try {
                autoPlus = Class.forName("SunVideoPlusAuto");
            } catch (Exception ee) {}
        }

        try {
            Object instance = auto.newInstance();
            if (autoPlus != null) {
                Object instancePlus = autoPlus.newInstance();
            }
            System.out.println("Finished detecting video capture devices");
        } catch (ThreadDeath td) {
            System.out.println("Capture device detection failed!");
            throw td;
        } catch (Throwable t) {
            System.out.println("Capture device detection failed!");
        }

    }
}
