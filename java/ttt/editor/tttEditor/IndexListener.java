package ttt.editor.tttEditor;


/**
 * The listener interface for receiving notification of an index change. 
 * The class that is interested in processing in response to an index change
 * implements this interface, and the object created with that
 * class is registered with a component, using the component's
 * <code>addIndexListener</code> method.
 */
public interface IndexListener {
    
    /**
     * Invoked when an <code>IndexEntry</code> is removed from the <code>Index</code>
     * @param index the position of the <code>IndexEntry</code> which has been removed
     */
    public void indexEntryRemoved(int index);
    
    /**
     * Invoked when an <code>IndexEntry</code> is added to the <code>Index</code>
     * @param index the position of the <code>IndexEntry</code> which has been added
     * @param entry the <code>IndexEntry</code> which has been added
     */
    public void indexEntryAdded(int index, IndexEntry entry);
    
}
