/* IRecVolSettings.java
 * Created on 14. Mai 2007, 10:43
 * @author Christian Gruber Bakk.techn.
 */

package ttt.wizard.recorder;

import javax.sound.sampled.LineUnavailableException;

/**
 * Interface for the systems recording volume
 */
public interface IRecVolSettings {
    /**
     * Returns the recording volume of the system
     * @return the actual recording volume of the system (0-100)
     * @throws javax.sound.sampled.LineUnavailableException if no input line can be found
     */
    public int getRecVol() throws LineUnavailableException ;
    /**
     * Sets the recording volume of the system
     * @param Volume volume level between 0 and 100
     * @throws javax.sound.sampled.LineUnavailableException if no recording line can be found
     */
    public void setRecVol(float Volume) throws LineUnavailableException ;  
}