/* OptionPanelDescriptor.java
 * Created on 25. April 2007, 11:46
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.recorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import ttt.LectureProfile;
import ttt.wizard.framework.WizardController;
import ttt.wizard.framework.WizardPanelDescriptor;


public class OptionPanelDescriptor extends WizardPanelDescriptor
        implements ActionListener, ItemListener{
    
    private String InfoText_EN = "This Panel contains information about the " +
            "automatically created file- and title name. Therefore you must specify " +
            "a lecture and teacher. The filename will be created by using date and lecture." +
            "If you choose a lecture " +
            "which was already in use all corresponding data will be loaded. \n\n" +
            "In the video selection you can specify if video recording should be " +
            "enabled. If yes, the next panel helps you to adjust your camera.";
    
    private String InfoText_DE = "Das TeleTeachingTool speichert Aufnahmen in sog. „Profilen“ ab. " +
            "In einem Profil werden unter anderem Angaben zum Name der Lehrveranstaltung " +
            "bzw. des Vortrags, zum Lehrenden und zum erzeugten " +
            "Dateinamen gespeichert. Der Name der Aufnahme " +
            "ergibt sich aus dem Namen der Lehrveranstaltung und dem aktuellen " +
            "Datum. \n\nProfile sind somit bei Vortragsreihen bzw. Lehrveranstaltungen " +
            "mit mehreren Lehrterminen sinnvoll. Finden mehrere Aufnahmen zum " +
            "gleichen Datum statt, so werden sie durch eine fortlaufende " +
            "Nummerierung am Dateiende angegeben. Durch Wahl eines Profils " +
            "werden die betreffenden Profileinstellungen geladen. \n\nÜber das " +
            "Mistkübel-Icon können Sie alle vorhandenen Profile löschen. "+
            "\n\nWenn Ihre Aufnahme neben dem Desktop-Screening und der Audiaufzeichnung " +
            "auch eine Videoaufzeichnung (Digitalkamera, Webcam, …) beinhalten soll,  " +
            "so aktivieren Sie das Kästchen „incl. Video“";
    
    private String lecture_EN = "Lecture:";
    private String lecture_DE = "Vortrag:";
    private String teacher_EN = "Teacher:";
    private String teacher_DE = "Vortragender:";
    private String title_EN = "Title:";
    private String title_DE = "Titel:";
    private String filename_EN = "Filename:";
    private String filename_DE = "Dateiname:";
    private String path_EN = "Recording Path:";
    private String path_DE = "Speicherort:";
    private String item_EN = "<choose or create profile>";
    private String item_DE = "<Bitte wählen oder erstellen Sie ein Profil>";
    
    
    private String m_NextComponent;
    private OptionPanel m_OptionPanel;
    private String m_Language;
    static Preferences m_UserPrefs = Preferences.userRoot().node("/TTT");
    
    /** Creates a new instance of OptionPanelDescriptor */
    public OptionPanelDescriptor(String language) {
        
        m_OptionPanel = new OptionPanel();
        m_OptionPanel.addActionListener(this);
        m_OptionPanel.addItemChangedListener(this);
        m_Language = language;
        m_NextButtonEnable = false;
        
        setObjectIdentifier("OPTIONPANEL");
        setComponent(m_OptionPanel);
        
        if(m_Language == "DE"){
            m_OptionPanel.InfoTextArea.setText(InfoText_DE);
            m_OptionPanel.jLabelLecture.setText(lecture_DE);
            m_OptionPanel.jLabelTeacher.setText(teacher_DE);
            m_OptionPanel.jLabelTitle.setText(title_DE);
            m_OptionPanel.jLabelFilename.setText(filename_DE);
            m_OptionPanel.jLabelRecordingPath.setText(path_DE);
            m_OptionPanel.jComboBoxLecture.getEditor().setItem(item_DE);
            m_OptionPanel.jButtonDeleteProfiles.setToolTipText("Profile löschen");
            //m_OptionPanel.jButtonExportProfiles.setToolTipText("Profile exportieren");
            //m_OptionPanel.jButtonImportProfiles.setToolTipText("Profile importieren");
            
        } else{
            m_OptionPanel.InfoTextArea.setText(InfoText_EN);
            m_OptionPanel.InfoTextArea.setText(InfoText_EN);
            m_OptionPanel.jLabelLecture.setText(lecture_EN);
            m_OptionPanel.jLabelTeacher.setText(teacher_EN);
            m_OptionPanel.jLabelTitle.setText(title_EN);
            m_OptionPanel.jLabelFilename.setText(filename_EN);
            m_OptionPanel.jLabelRecordingPath.setText(path_EN);
            m_OptionPanel.jComboBoxLecture.getEditor().setItem(item_EN);
            m_OptionPanel.jButtonDeleteProfiles.setToolTipText("Delete Profiles");
            //m_OptionPanel.jButtonExportProfiles.setToolTipText("Export Profiles");
            //m_OptionPanel.jButtonImportProfiles.setToolTipText("Import Profiles");
        }
    }
    
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        
        if(cmd.equals("comboBoxChangedTeacher")) {
            String title = LectureProfile.getDefaultTitle((String)m_OptionPanel.jComboBoxLecture.getSelectedItem(),
                    (String)m_OptionPanel.jComboBoxTeacher.getSelectedItem());
            m_OptionPanel.jLabelTitlefiled.setText(title);
            
            
            try{
                boolean lectureEmpty = m_OptionPanel.jComboBoxLecture.getSelectedItem().toString().equalsIgnoreCase("");
                boolean teacherEmpty = m_OptionPanel.jComboBoxTeacher.getSelectedItem().toString().equalsIgnoreCase("");
                
                if(lectureEmpty || teacherEmpty){
                    m_NextButtonEnable = false;
                    restoreButtonsToPanelState();
                } else{
                    m_NextButtonEnable = true;
                    restoreButtonsToPanelState();
                }
            } catch(NullPointerException ex){
                ex.printStackTrace();
            }
        }
        
        
        else if (cmd.equals("comboBoxChangedLecture")) {
            
            LectureProfile profile = LectureProfile.getProfile((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
            
            if (profile != null) {
                m_OptionPanel.jComboBoxLecture.setSelectedItem(profile.getLecture());
                m_OptionPanel.jComboBoxTeacher.setSelectedItem(profile.getTeacher());
            }
            
            String filename = LectureProfile.getDefaultFilename((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
            String title = LectureProfile.getDefaultTitle((String)m_OptionPanel.jComboBoxLecture.getSelectedItem(),
                    (String)m_OptionPanel.jComboBoxTeacher.getSelectedItem());
            
            m_OptionPanel.jLabelFilenameField.setText(filename);
            m_OptionPanel.jLabelTitlefiled.setText(title);
            
            boolean lectureEmpty = m_OptionPanel.jComboBoxLecture.getSelectedItem().toString().equalsIgnoreCase("");
            boolean teacherEmpty = m_OptionPanel.jComboBoxTeacher.getSelectedItem().toString().equalsIgnoreCase("");
            
            if(lectureEmpty || teacherEmpty){
                m_NextButtonEnable = false;
                restoreButtonsToPanelState();
            } else{
                m_NextButtonEnable = true;
                restoreButtonsToPanelState();
            }
        }
        
        else if (cmd.equals("jButtonFileChooser")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Set Recording Path");
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setCurrentDirectory(new File(m_UserPrefs.get("record_path", ".")));
            
            int returnVal = fileChooser.showDialog(m_OptionPanel, "Ok");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileChooser.getSelectedFile();
                    m_UserPrefs.put("record_path", file.getCanonicalPath());
                    m_OptionPanel.jLabelRecordingPathField.setText(file.getCanonicalPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,"Cannot open file: " + ex);
                }
            }
        }
        
        else if (cmd.equals("deleteProfiles")){
            
            int decision;
            if(m_Language == "EN"){
                decision =
                        JOptionPane.showConfirmDialog(null,"Delete all existing Profiles?",
                        "Warning", JOptionPane.WARNING_MESSAGE);
            } else{
                decision =
                        JOptionPane.showConfirmDialog(null,"Alle Profile löschen?",
                        "Warnung", JOptionPane.WARNING_MESSAGE);
            }
            
            if(decision == 0) {
                LectureProfile.clearProfiles();
                m_OptionPanel.jComboBoxLecture.removeAllItems();
                m_OptionPanel.jComboBoxTeacher.removeAllItems();
            }
        }
        
        else if (cmd.equals("exportProfiles")){
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("TTTProfiles.xml"));
            
            if(m_Language == "EN")
                fileChooser.setDialogTitle("Export Profiles To ...");
            else
                fileChooser.setDialogTitle("Profile exportieren nach ...");
            
            
            // file filter
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    String fname = f.getName().toLowerCase();
                    return fname.endsWith(".xml") || f.isDirectory();
                }
                
                public String getDescription() {
                    return "XML Lecture Profiles";
                }
            });
            
            int returnVal = fileChooser.showSaveDialog(m_OptionPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                LectureProfile.exportProfiles(fileChooser.getSelectedFile());
            }
        }
        
        else if (cmd.equals("importProfiles")){
            JFileChooser fileChooser = new JFileChooser();
            if(m_Language == "EN")
                fileChooser.setDialogTitle("Import Profiles");
            else
                fileChooser.setDialogTitle("Profile importieren");
            File file = new File(fileChooser.getCurrentDirectory().getAbsolutePath() + File.separator
                    + "TTTProfiles.xml");
            if (file.exists())
                fileChooser.setSelectedFile(file);
            // file filter
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    String fname = f.getName().toLowerCase();
                    return fname.endsWith(".xml") || f.isDirectory();
                }
                public String getDescription() {
                    return "XML Lecture Profiles";
                }
            });
            
            int returnVal = fileChooser.showOpenDialog(m_OptionPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                LectureProfile.importProfiles(fileChooser.getSelectedFile());
            }
            
            String[] lectures = LectureProfile.getLectures();
            for(String lecture : lectures){
                m_OptionPanel.jComboBoxLecture.addItem(lecture);
            }
            
            String[] teachers = LectureProfile.getTeachers();
            for(String teacher : teachers){
                m_OptionPanel.jComboBoxTeacher.addItem(teacher);
            }
        }
    }
    
    /**
     * If the videopanel is selected set the m_NextPanel to the video panel
     * else to the info panel
     */
    public void itemStateChanged(ItemEvent e) {
        if (m_OptionPanel.jCheckBoxVideo.isSelected())
            m_NextComponent = "VIDEOPANEL";
        else
            m_NextComponent = "INFOPANEL";
    }
    
    public Object getLastComponentDescriptor(){
        return "AUDIOPANEL";
    }
    
    /**
     * The next panel depends if the video chackbox is selected
     * @return the object identifier of the video or info panel
     */
    public Object getNextComponentDescriptor() {
        return m_NextComponent;
    }
    
    /**
     * Resets the buttons to the actual state of the panel
     */
    public void beforeDisplayingPanel() {
        
        this.restoreButtonsToPanelState();
        
        if (m_OptionPanel.jCheckBoxVideo.isSelected())
            m_NextComponent = "VIDEOPANEL";
        else
            m_NextComponent = "INFOPANEL";
        
        //Look if recording path exists
        String filePath = m_UserPrefs.get("record_path", ".");
        File path = new File(filePath);    
        if(!path.exists()){
            if(m_Language == "EN"){
                JOptionPane.showMessageDialog(m_OptionPanel, "Recording Path Not Existing! Select Valid Path",
                        "Recording Path Error", JOptionPane.ERROR_MESSAGE);
                m_OptionPanel.jLabelRecordingPathField.setText("SELECT VALID RECORDING PATH!");
            }
            
            else{
                JOptionPane.showMessageDialog(m_OptionPanel, "Der Pfad für die " +
                        "Speicherung der Aufnahme existiert nicht! Bitte geben Sie einen gültigen Pfad an!",
                        "Aufnahmepfad Error", JOptionPane.ERROR_MESSAGE);
                m_OptionPanel.jLabelRecordingPathField.setText("BITTE AUFNAHMEPFAD AUSWÄHLEN!");
            }
        }
        
        
    }
    
    public void afterDisplayingPanel() {
        
        // store profile
        LectureProfile lectureProfile = LectureProfile.getProfile((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
        if (lectureProfile == null)
            lectureProfile = new LectureProfile((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
        
        final LectureProfile profile = lectureProfile;
        
        profile.setTeacher((String)m_OptionPanel.jComboBoxTeacher.getSelectedItem());
        profile.setHost("localhost");
        profile.setPort(5900);
        profile.setRecordEnabled(true);
        if (m_OptionPanel.jCheckBoxVideo.isSelected())
            profile.setRecordVideoEnabled(true);
        else
            profile.setRecordVideoEnabled(false);
        profile.setShowRecordPlayRecordWarning(false);
        profile.storeProfile();
        
        WizardController controller = getWizardController();
        controller.setProfile(profile);
    }
    
    /**
     * Gets the lecture profile from the inputs, stores and returns it.
     * The returned profile will be used to start the recorder.
     * @return profile of the lecture
     */
    public LectureProfile getLectureProfile(){
        
        // store profile
        LectureProfile lectureProfile = LectureProfile.getProfile((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
        if (lectureProfile == null)
            lectureProfile = new LectureProfile((String)m_OptionPanel.jComboBoxLecture.getSelectedItem());
        
        final LectureProfile profile = lectureProfile;
        
        profile.setTeacher((String)m_OptionPanel.jComboBoxTeacher.getSelectedItem());
        profile.setHost("localhost");
        profile.setPort(5900);
        profile.setRecordEnabled(true);
        if (m_OptionPanel.jCheckBoxVideo.isSelected())
            profile.setRecordVideoEnabled(true);
        else
            profile.setRecordVideoEnabled(false);
        profile.setShowRecordPlayRecordWarning(false);
        profile.storeProfile();
        
        return profile;
    }
    
}
