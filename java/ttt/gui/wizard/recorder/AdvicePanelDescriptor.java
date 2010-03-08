/* AdvicePanelDescriptor.java
 * Created on 29. August 2007, 08:10
 * Christian Gruber Bakk.techn
 */

package ttt.gui.wizard.recorder;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import ttt.gui.wizard.framework.WizardPanelDescriptor;

public class AdvicePanelDescriptor extends WizardPanelDescriptor implements ItemListener{
    
    private AdvicePanel m_AdvicePanel;
    
    private boolean m_showNextTime;
    
    private String m_HandlingText_DE = "Sie haben alle Einstellungen für eine " +
            "Aufnahme getroffen. Nach dem Beenden des Wizards wird ein rotes Icon " +
            "in der Taskleiste angezeigt. \n\nUm eine Aufnahme zu starten oder zu stoppen, " +
            "öffnen Sie mit der rechten Maustaste das rote Icon in der Taskbar und wählen " +
            "Sie „Aufnahme Starten“ oder „Stoppen“.";
    
    private String m_HandlingText_EN = "You have made all decision to start recording. " +
            "After this wizard a new red icon will be shown in the taskbar." +
            "To start click on the red icon and select „Start recording“. " +
            "To stop click on the red icon again and select „Stop recording“.";
    
    private String m_VNC_DE = "Wenn Sie das VNC Server Passwort noch nicht geändert haben, " +
            "doppel klicken Sie nach dem Beenden des Wizards auf das VNC Icon in der Taskbar und " +
            "setzten Sie ein Passwort unter Authentication. Danach " +
            "beenden und starten Sie den TTRecorder neu.";
     
    private String m_VNC_EN = "If you have not set the VNC Server password double " +
            "click on the VNC taskbar icon after the wizard has closed and set " +
            "the password in the field Authentication. Afterwards " +
            "quit and start the TTRecorder anew.";
    
    private String m_ShowPanelNextTime_DE = "Dieses Panel das nächste Mal nicht mehr anzeigen.";
    
    private String m_ShowPanelNextTime_EN = "Do not show this panel next time.";
    
    /** Creates a new instance of AdvicePanelDescriptor */
    public AdvicePanelDescriptor(String language) {
        
        m_FinishEnable = true;
        m_AdvicePanel = new AdvicePanel();
        m_showNextTime = true;
        
        setObjectIdentifier("ADVICEPANEL");
        setComponent(m_AdvicePanel);
        
        if(language == "EN"){
            m_AdvicePanel.handlingTextArea.setText(m_HandlingText_EN);
            m_AdvicePanel.showNextTimeCheckBox.setText(m_ShowPanelNextTime_EN);
            m_AdvicePanel.vncTextArea.setText(m_VNC_EN);
        } else{
            m_AdvicePanel.handlingTextArea.setText(m_HandlingText_DE);
            m_AdvicePanel.showNextTimeCheckBox.setText(m_ShowPanelNextTime_DE);
            m_AdvicePanel.vncTextArea.setText(m_VNC_DE);
        }
        
        m_AdvicePanel.addCheckBoxListener(this);   
    }
    
    /**
     * There is no panel before the audiopanel - return null
     * @return null
     */
    public Object getLastComponentDescriptor(){
        return "INFOPANEL";
    }
    
    public Object getNextComponentDescriptor() {
        return FINISH;
    }
    
    public void beforeDisplayingPanel() {
        this.restoreButtonsToPanelState();
    }

    public void itemStateChanged(ItemEvent e) {
        if(m_AdvicePanel.showNextTimeCheckBox.isSelected())
            m_showNextTime = false;
        else
            m_showNextTime = true;
    }
    
    public boolean isShowNextTimeSelected(){
        return m_showNextTime;
    }
    
}
