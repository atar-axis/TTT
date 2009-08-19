package ttt.messaging.gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicListUI;

import ttt.messaging.*;
import ttt.messaging.server.MessagingController;

/**
 * a custom list for displaying the received messages from users.
 * It uses a custom list model {@link TTTMessageListModel}.
 * The custom renderer uses {MessageListTextPanel} and {MessageListSheetPanel} for
 * rendering the messages.
 * The list uses an extended BasicListUI, which is used to call the protected method
 * {@code updateLayoutState()} so that the heights of the list entries are refreshed.
 * This is needed because in normal state, a list entry shows only part and only when
 * selected, all content of the message is displayed. Therefore, the height of entries
 * changes. 
 * @author Thomas Doehring
 */
public class JTTTMessageList extends JList {
	
	public final static long serialVersionUID = 1L;

	private MyBasicListUI myUI;
	private MessagingController controller;
	
	public JTTTMessageList(int initialWidth) {
		super(new TTTMessageListModel());
		
		myUI = new MyBasicListUI();
		setUI(myUI);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		MessageCellRenderer mcr = new MessageCellRenderer();
		setCellRenderer(mcr);
		
		// refresh heights when size of list is changed.
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				triggerRepaint();
			}
		});
		
		// refresh heights when selection changes
		addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				if (!lse.getValueIsAdjusting()) {
					triggerRepaint();
				}
			}
		});
		
		// process mouse events in this class
		enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_DRAGGED);
	}
	
	public TTTMessageListModel getModel() {
		return (TTTMessageListModel)super.getModel();
	}
	
	public void setController(MessagingController ctrl) {
		this.controller = ctrl;
	}
	
	
	@Override
	protected void processMouseEvent(MouseEvent e) {

		if(e.getID() == MouseEvent.MOUSE_PRESSED) {
			
			int idx = locationToIndex(e.getPoint());
			if (idx != -1) {
				Rectangle bounds = getCellBounds(idx,idx);

				// check if really clicked in bounds of entry
				// (needed because last list entry would be selected also when
				// clicked beneath it).
				if(!bounds.contains(e.getPoint())) {
					clearSelection();
					e.consume();

				} else if (idx == getSelectedIndex()) {
					// clicked on already selected entry -> do action
					if(e.getX() < getWidth() / 3) {
						// show message in TTT session
						controller.showMessage((TTTMessage)getSelectedValue());

					} else if (e.getX() < (getWidth() * 2 / 3)) {
						// delete message
						clearSelection();
						getModel().deleteMessage(idx);

					} else {
						// defer message
						clearSelection();
						getModel().deferMessage(idx);
					}
					e.consume();				
				}
			}
		} else if(e.getID() == MouseEvent.MOUSE_RELEASED) {
			int idx = locationToIndex(e.getPoint());
			if (idx != -1) {
				Rectangle bounds = getCellBounds(idx,idx);

				if(!bounds.contains(e.getPoint())) {
					// clearSelection();
					e.consume();
				}
			}
		}
		if(!e.isConsumed()) super.processMouseEvent(e);
	}
	
	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		// filter mouse drag events so that only click and no drags select entries
		if (e.getID() == MouseEvent.MOUSE_DRAGGED && 
				((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)) {
			e.consume();
		} else {
			super.processMouseMotionEvent(e);
		}
	}

	/**
	 * trigger the refresh of the heights of the list entries
	 */
	private void triggerRepaint() {
		myUI.triggerHeightUpdate();
		repaint();
//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				myUI.triggerHeightUpdate();
//				// revalidate();
//				repaint();
//			}
//		});
	}
	
	/**
	 * custom ListUI to access protected member {@code updateLayoutState()} which
	 * refreshes the cached heights of the entries in the ListUI.
	 */
	class MyBasicListUI extends BasicListUI {
		
		public void triggerHeightUpdate() {
			updateLayoutState();
			
		}		
	}
	
	class MessageCellRenderer implements ListCellRenderer
	{
		MessageListTextPanel txtRenderer;
		MessageListSheetPanel sheetRenderer;
		
		public MessageCellRenderer() {
			txtRenderer = new MessageListTextPanel();
			sheetRenderer = new MessageListSheetPanel();
		}
		
		// @Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			// commit the properties of the messages to the corresponding renderer and return it
			if(value instanceof TTTTextMessage) {
				TTTTextMessage txtMsg = (TTTTextMessage)value;
				txtRenderer.text = txtMsg.getText();
				if (txtMsg.hasUserName()) txtRenderer.name = txtMsg.getUserName();
				else txtRenderer.name = null;
				// WORKAROUND: isSelected does not work when list fetches the components for height calculation
				// txtRenderer.isSelected = isSelected;
				txtRenderer.selected = (list.getSelectedIndex() == index);
				txtRenderer.deferred = txtMsg.isDeferred();
				txtRenderer.setListWidth(list.getWidth());
				txtRenderer.calculateHeight((Graphics2D)list.getGraphics());
				
				return txtRenderer;
			}
			else if (value instanceof TTTSheetMessage) {
				TTTSheetMessage sheetMsg = (TTTSheetMessage)value;
				sheetRenderer.message = sheetMsg;
				if (sheetMsg.hasUserName()) sheetRenderer.name = sheetMsg.getUserName();
				else sheetRenderer.name = null;
				sheetRenderer.selected = (list.getSelectedIndex() == index);
				sheetRenderer.deferred = sheetMsg.isDeferred();
				sheetRenderer.setListWidth(list.getWidth());
				sheetRenderer.calculateHeight((Graphics2D)list.getGraphics());
				
				return sheetRenderer;
			}
			
			return null;
		}

	}

}
