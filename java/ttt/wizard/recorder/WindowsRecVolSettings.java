/* WindowsRecVolSettings.java
 * Created on 14. Mai 2007, 10:47
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.recorder;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Port;

/**
 * The WindowsRecVolSettings is responsible for the windows recording volume settings.
 * With the setter and getter methods the recording volume can be adjusted.
 */
public class WindowsRecVolSettings implements IRecVolSettings{
    
    private FloatControl volCtrl;
    private Port line;
    
    /**
     * Tries to get a mixer control for the microphone input line
     * Throws a LineUnavailableException if no line for microphones was found
     * @throws javax.sound.sampled.LineUnavailableException if no input line was found
     */
    public WindowsRecVolSettings() throws LineUnavailableException{
        
        if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {  
            line = (Port) AudioSystem.getLine(Port.Info.MICROPHONE);
            line.open();
            
            //System.out.println("Port Controls: " +line.getControls().length);
            //System.out.println("PORT CONTROL: " +line.getControls()[0].getClass());
            
            for(int controlnumber = 0; controlnumber < line.getControls().length; controlnumber++) {
                if(line.getControls()[controlnumber] instanceof CompoundControl) {
                    CompoundControl control = (CompoundControl)line.getControls()[controlnumber];
                    
                    for(int miccontrol =0; miccontrol<control.getMemberControls().length; miccontrol++) {
                        //System.out.println("Miccontrols: " +control.getMemberControls()[miccontrol].toString());
                        //System.out.println("Control Type String: "+control.getMemberControls()[miccontrol].getType().toString());
                        
                        if(control.getMemberControls()[miccontrol].getType().toString().equalsIgnoreCase("VOLUME")) {
                            
                            volCtrl = (FloatControl) control.getMemberControls()[miccontrol];
                        }
                    }
                }
            }  
        } else{
            throw new LineUnavailableException();
        }
    }
    
    /**
     * Returns the current recording volume value of the microphone line
     * Throws a LineUnavailable Exception if no line for microphones was found
     * @return the recording volume between 0 and 100
     * @throws javax.sound.sampled.LineUnavailableException if no inputline is found
     */
    public int getRecVol() throws LineUnavailableException {
        if(volCtrl != null) {
            float volume = new Float(volCtrl.getValue()*100);
            return (int)volume;
        } else
            throw new LineUnavailableException();
    }
    
    /**
     * Sets the new volume value of the microphone line
     * Throws a LineUnavailable Exception if no line for microphones was found
     * @param volume the new volume value bewtween 0 and 100
     * @throws javax.sound.sampled.LineUnavailableException if no inputline was found
     */
    public void setRecVol(float volume) throws LineUnavailableException {
        if(volCtrl != null){
            volCtrl.setValue(volume/100);
        } else
            throw new LineUnavailableException();
    }
    
}
