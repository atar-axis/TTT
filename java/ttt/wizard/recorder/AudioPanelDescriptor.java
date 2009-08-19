/* AudioPanelDescriptor.java
 * Created on 24. April 2007, 15:19
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.recorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ttt.wizard.framework.WizardPanelDescriptor;

public class AudioPanelDescriptor extends WizardPanelDescriptor implements ChangeListener, ActionListener {

    private String InfoText_EN = "Welcome! This Wizard will guide you through some basic "
            + "steps before recording with the TeleTeachingTool. \n\nBelow the "
            + "audiobar shows the actual recording volume. Use the slider " + "to adjust the recording volume during "
            + "speaking. If the audiobar does not "
            + "show amplitude assure that in the Windows Audio Recording Settings "
            + "the microphone is selected. You can open it with the button next to the slider.";

    private String InfoText_DE = "Willkommen! Dieser Wizard hilft Ihnen, grundlegende Einstellungen "
            + "vor einer Aufnahme mit dem TeleTeachingTool zu treffen. \n\nIm "
            + "unteren Bereich zeigt ein Balken den aktuellen Tonaufnahmepegel an. "
            + "Mit Hilfe des darunter liegenden Reglers kann dieser auf einen optimalen "
            + "Wert eingestellt werden. Wenn der Balken "
            + "keinen Ausschlag zeigt, versichern Sie sich, dass in den Windows Ton Aufnahme "
            + "Einstellungen das Mikrofon als Quelle ausgewählt ist. Mit Hilfe des "
            + "Buttons neben dem Regler können die Einstellungen geöffnet werden.";

    private String good_EN = "    good";

    private String good_DE = "    gut";

    private String perfect_EN = "perfect";

    private String perfect_DE = "perfekt";

    private String high_EN = "too high    ";

    private String high_DE = "zu hoch    ";

    private String volume_EN = "Recording Volume";

    private String volume_DE = "Aufnahme Lautstärke";

    private AudioPanel m_AudioPanel;
    private IRecVolSettings m_RecVolSettings;

    /** Creates a new instance of AudioPanelDescriptor */
    public AudioPanelDescriptor(String language) {

        m_BackButtonEnable = false;
        m_AudioPanel = new AudioPanel();
        m_AudioPanel.addChangeListener(this);
        m_AudioPanel.addActionListener(this);
        setObjectIdentifier("AUDIOPANEL");
        setComponent(m_AudioPanel);

        if (language == "DE") {
            m_AudioPanel.InfoTextArea.setText(InfoText_DE);
            m_AudioPanel.goodLabel.setText(good_DE);
            m_AudioPanel.perfectLabel.setText(perfect_DE);
            m_AudioPanel.highLabel.setText(high_DE);
            m_AudioPanel.volumeLabel.setText(volume_DE);
        } else {
            m_AudioPanel.InfoTextArea.setText(InfoText_EN);
            m_AudioPanel.goodLabel.setText(good_EN);
            m_AudioPanel.perfectLabel.setText(perfect_EN);
            m_AudioPanel.highLabel.setText(high_EN);
            m_AudioPanel.volumeLabel.setText(volume_EN);
        }
    }

    /**
     * There is no panel before the audiopanel - return null
     * 
     * @return null
     */
    public Object getLastComponentDescriptor() {
        return null;
    }

    public Object getNextComponentDescriptor() {
        return "OPTIONPANEL";
    }

    /**
     * Gets the windows recording volume, sets the volume slider and shows the volume peak
     */
    public void beforeDisplayingPanel() {

        this.restoreButtonsToPanelState();

        // get windows recording volume
        try {

            m_RecVolSettings = new WindowsRecVolSettings();
            m_AudioPanel.jSliderRecVolume.setValue(m_RecVolSettings.getRecVol());

        } catch (LineUnavailableException ex) {
            m_AudioPanel.jSliderRecVolume.setEnabled(false);
        }

        // create audioformat and start LineInVolumePeak component
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050.0F, 16, 1, 2, 22050.0F, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine targetDataLine;

        try {

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);

            LineInVolumePeak lineInPeakComponent = new LineInVolumePeak(targetDataLine,
                    m_AudioPanel.volumeLevelComponent1);

            lineInPeakComponent.start();

        } catch (LineUnavailableException ex) {

            JOptionPane.showMessageDialog(null, "No Microphone Input Found!" + " Programm Will Exit");
            System.exit(-1);
        }

    }

    /**
     * Updates the windows volume recording settings if the volume slider has changed
     * 
     * @param e
     *            event of the volume slider
     */
    public void stateChanged(ChangeEvent e) {

        JSlider slider = (JSlider) e.getSource();
        try {
            m_RecVolSettings.setRecVol(slider.getValue());
        } catch (LineUnavailableException ex) {
            slider.setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        try {
            // start windows recording sound controls
            String start = "sndvol32 /r";
            Runtime.getRuntime().exec(start);
        } catch (IOException ex) {
            System.err.println("Could Not Find Windows Sound Recording Dialog");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
