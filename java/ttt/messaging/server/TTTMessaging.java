package ttt.messaging.server;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import ttt.Constants;
import ttt.RfbProtocol;
import ttt.messaging.gui.FullPollCreateDialog;
import ttt.messaging.gui.JPollList;
import ttt.messaging.gui.JTTTMessageList;
import ttt.messaging.gui.QuickPollCreateDialog;
import ttt.messaging.server.TTTMessengerServer;
//WEB import ttt.messaging.server.WebMessaging;

/**
 * Main class of messaging server.
 * Constructs the GUI in the form of an internal frame and starts the messaging server.
 */
public class TTTMessaging extends JInternalFrame implements Closeable {
	
	public final static long serialVersionUID = 1L;
	
	// jpg quality for transmitted sheet images 
	public static final int JPEG_QUALITY = 90;

	private TTTMessengerServer server;
	private MessagingController controller;
	//WEB private ttt.messaging.server.WebMessaging webServer;
	
	/**
	 * starts the messaging.
	 */
	public TTTMessaging(RfbProtocol tttL, java.net.InetAddress ip, boolean startWebMessaging) {		
		super("Messaging");
		
		JTabbedPane tabPane = new JTabbedPane();
		
		// ------------------ MESSAGING PANEL ------------------------- //
		JPanel msgPane = new JPanel(new BorderLayout());
		
		JToolBar toolbar = new JToolBar("Messaging Bar");
		
		JButton btnDelAll = new JButton(Constants.getIcon("msg_messagedelall.png"));
		btnDelAll.setBorderPainted(false);
		btnDelAll.setToolTipText("delete all message");
		btnDelAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				controller.deleteAllMessages();
			}
		});
		toolbar.add(btnDelAll);
		
		JButton btnDelNonDef = new JButton(Constants.getIcon("msg_messagedelnondefer.png"));
		btnDelNonDef.setBorderPainted(false);
		btnDelNonDef.setToolTipText("delete all non deferred messages");
		btnDelNonDef.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				controller.deleteAllNonDeferred();
			}
		});
		toolbar.add(btnDelNonDef);
		
		JButton btnBlock = new JButton(Constants.getIcon("msg_userblock.png"));
		btnBlock.setBorderPainted(false);
		btnBlock.setToolTipText("block user");
		btnBlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				controller.blockUser();
			}
		});
		toolbar.add(btnBlock);

		// Message List
		JTTTMessageList msgList = new JTTTMessageList(220 - getInsets().left - getInsets().right);
		
		JScrollPane scroll = new JScrollPane(msgList);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		msgPane.add(toolbar, BorderLayout.NORTH);
		msgPane.add(scroll, BorderLayout.CENTER);

		tabPane.addTab(null, Constants.getIcon("msg_messages.png"), msgPane, "Messages");
		
		// ------------------------- POLL GUI ---------------------------- //
		
		JPanel pollPane = new JPanel(new BorderLayout());
		
		JToolBar pollTools = new JToolBar();
		
		JButton btnNewFullPoll = new JButton(Constants.getIcon("msg_fullpoll.png"));
		btnNewFullPoll.setBorderPainted(false);
		btnNewFullPoll.setToolTipText("create new full poll");
		btnNewFullPoll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// display creation dialog and if successful add poll to list
				FullPollCreateDialog fpDlg = new FullPollCreateDialog();
				int result = JOptionPane.showConfirmDialog(TTTMessaging.this, fpDlg, "create new FullPoll", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (result == JOptionPane.OK_OPTION) {
					if (fpDlg.getAnswers() != null) {
						controller.createFullPoll(fpDlg.getQuestion(), fpDlg.getAnswers());
					} else {
						JOptionPane.showMessageDialog(TTTMessaging.this, "You have to enter at least two answers!");
					}
				}
			}
		});
		pollTools.add(btnNewFullPoll);
		
		JButton btnNewQuickPoll = new JButton(Constants.getIcon("msg_quickpoll.png"));
		btnNewQuickPoll.setBorderPainted(false);
		btnNewQuickPoll.setToolTipText("create new quick poll");
		btnNewQuickPoll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				QuickPollCreateDialog qpDlg = new QuickPollCreateDialog();
				int result = JOptionPane.showConfirmDialog(TTTMessaging.this, qpDlg, "create new QuickPoll", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (result == JOptionPane.OK_OPTION) {
					controller.createQuickPoll(qpDlg.getColor(), qpDlg.getNumber());
				}
			}
		});
		pollTools.add(btnNewQuickPoll);
		
		pollPane.add(pollTools, BorderLayout.NORTH);
		
		JPollList pollList = new JPollList();
		pollPane.add(pollList, BorderLayout.CENTER);
		
		tabPane.addTab(null, Constants.getIcon("msg_polls.png"), pollPane, "Polls");

		
		
		getContentPane().add(tabPane, BorderLayout.CENTER);
		
		// *** create Controller & Server and link together everything *** //
		controller = new MessagingController(msgList, pollList.getModel(), tttL);
		msgList.setController(controller);
		pollList.setController(controller);
		server = new TTTMessengerServer(controller,ip);
		server.start();
		
		/*//WEB
		if(startWebMessaging) {
			webServer = new WebMessaging(controller);
		}
		*/
		
		// close everything if internal frame is closed
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				server.stop();
			}
		});

		setSize(220,600);
		setClosable(true);
		setResizable(true);
		setVisible(true);
	}
	
	/**
	 * stop and close everything.
	 */
	public void close() {
		server.stop();
		//WEB if(webServer != null) webServer.stop();
		setVisible(false);
		dispose();
	}
}
