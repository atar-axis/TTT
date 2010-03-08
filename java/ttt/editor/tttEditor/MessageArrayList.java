package ttt.editor.tttEditor;


import java.util.ArrayList;


/**
 * An <code>ArrayList</code> with added methods specifically tailored
 * to dealing with <code>Message</code>s
 */
public class MessageArrayList<E extends Message> extends ArrayList<E> {
     

    /**
     * Adds a message to the correct place <code>MessageArrayList</code> depending upon timestamp.
     * @param entry the message to be added
     * @return <code>true</code> (as per the general contract of Collection.add).
     */
    public boolean add(E entry){
        if (entry.getTimestamp() >= getLastTimestamp())
            super.add(entry);
        else
            for (int count = 0; count < size(); count++ ) {
                if (entry.getTimestamp() < get(count).getTimestamp()) {
                    super.add(count, entry);
                    break;
                }
            }
        return true;
    }


    /**
     * Gets the last timestamp of any message in the list.
     * @return  the timestamp (milliseconds)
     */
    public int getLastTimestamp() {
        if (size() > 0)
            return get(size()-1).getTimestamp();
        else
            return -1;
    }
    
    /**
     * Gets the last timestamp of any message in the list, without synchronization.
     * @return  the timestamp (milliseconds)
     */
    public int getLastTimestampWithoutSync() {
        if (size() > 0)
            return get(size()-1).getTimestampWithoutSync();
        else
            return -1;
    }
}
    
