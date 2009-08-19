package ttt.messaging.gui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicListUI;

import ttt.messaging.server.MessagingController;
import ttt.messaging.poll.FullPoll;
import ttt.messaging.poll.Poll;
import ttt.messaging.poll.QuickPoll;

/**
 * A list for displaying polls. Uses an own ListModel {@link PollListModel}.
 * The custom renderer uses {@link FullPollPanel} and {@link QuickPollPanel} for
 * displaying the list entries.
 * The list uses an extended BasicListUI, which is used to call the protected method
 * {@code updateLayoutState()} so that the heights of the list entries are refreshed.
 * @author Thomas Doehring
 */
public class JPollList extends JList {

	public final static long serialVersionUID = 1L;
	
	private MessagingController controller;
	private MyBasicListUI myUI;

	public JPollList() {
		super(new PollListModel());
		
		myUI = new MyBasicListUI();
		setUI(myUI);
		PollListCellRenderer renderer = new PollListCellRenderer();
		setCellRenderer(renderer);
		// trigger refresh of list entries' heights when list is resized
		addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e) {
				triggerRepaint();
			}
		});
		
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// process mouse events within this class
		enableEvents(MouseEvent.MOUSE_EVENT_MASK  | MouseEvent.MOUSE_DRAGGED);
	}
	
	public void setController(MessagingController ctrl) {
		this.controller = ctrl;
	}
	
	@Override
	public PollListModel getModel() {
		return (PollListModel)super.getModel();
	}
	
	@Override
	protected void processMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			
			int idx = locationToIndex(e.getPoint());
			if (idx != -1) {
				Rectangle bounds = getCellBounds(idx,idx);
				
				// have to check if event is really in the bounds of the entry
				// (last entry would be selected even if click is beneath it)
				if(!bounds.contains(e.getPoint())) {
					clearSelection();
					e.consume();
					
				} else if (idx == getSelectedIndex()){
					// clicked on already selected item -> do action
					if(e.getX() < (getWidth() / 2)) {
						controller.showPoll((Poll)getSelectedValue());
					} else {
						getModel().togglePollStatus(idx);
					}
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
		// consume drag events so that selection of entries only occurs with clicks
		if (e.getID() == MouseEvent.MOUSE_DRAGGED && 
				((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)) {
			e.consume();
		} else {
			super.processMouseMotionEvent(e);
		}
	}

	/**
	 * refresh heights of entries
	 */
	private void triggerRepaint() {
		SwingUtilities.invokeLater(new Runnable() {
			// @Override
			public void run() {
				myUI.triggerHeightUpdate();
				revalidate();
				repaint();
			}
		});
	}
	
	/**
	 * used to access the protected member {@code updateLayoutState()} of 
	 * {@code BasicListUI}.
	 * @author Thomas Doehring
	 *
	 */
	class MyBasicListUI extends BasicListUI {
		public void triggerHeightUpdate() {
			updateLayoutState();
		}		
	}
	
	/**
	 * custom list renderer
	 * @author Thomas Doehring
	 */
	class PollListCellRenderer implements ListCellRenderer {
		
		FullPollPanel fullPollRenderer;
		QuickPollPanel quickPollRenderer;
				
		public PollListCellRenderer() {
			fullPollRenderer = new FullPollPanel();
			quickPollRenderer = new QuickPollPanel();
		}
		
		// @Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			// commit the properties of the poll to the corresponding renderer and
			// return it, so that it's paint method can be called
			if (value instanceof QuickPoll) {
				QuickPoll qp = (QuickPoll)value;
				quickPollRenderer.closed = !qp.isOpen();
				quickPollRenderer.selected = isSelected;
				quickPollRenderer.color = qp.getColor();
				quickPollRenderer.votes = qp.getResult();
				quickPollRenderer.votesPromilles = qp.getPromilleResult();
				quickPollRenderer.listWidth = list.getWidth();
				quickPollRenderer.calculateHeight((Graphics2D)list.getGraphics());
				return quickPollRenderer;
			} else {
				FullPoll fp = (FullPoll)value;
				fullPollRenderer.closed = !fp.isOpen();
				fullPollRenderer.selected = isSelected;
				fullPollRenderer.question = fp.getQuestion();
				fullPollRenderer.answers = fp.getAnswers();
				fullPollRenderer.votes = fp.getResult();
				fullPollRenderer.votesPromilles = fp.getPromilleResult();
				fullPollRenderer.listWidth = list.getWidth();
				fullPollRenderer.calculateHeight((Graphics2D)list.getGraphics());
				return fullPollRenderer;
			}
		}

	}
}
