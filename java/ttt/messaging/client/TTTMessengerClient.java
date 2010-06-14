package ttt.messaging.client;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.Socket;

import javax.swing.*;

import ttt.Constants;
import ttt.messaging.gui.LoginDialog;

/**
 * Main class of Messaging Client. Shows a {@code JFrame} with the GUI.
 * At creation a login dialog is displayed into which the name/IP of the messaging
 * server has to be entered.
 * @author Thomas Doehring
 */
public class TTTMessengerClient extends JFrame {
	
	public static final long serialVersionUID = 1L;

	JButton btnSend;
	JPanel btnPanel;
	JButton btnConnect;
	JPanel cnctPanel;
	JTextArea msgText;
	JScrollPane msgPane;
	JTabbedPane tabPane;
	JLabel lblImage;
	
	JTextArea txtArea;
	JTextArea pollTxt;
	
	ImageIcon imgSheet = null;
	byte[] bufferedImage = null;
	
	private ClientConnection cc;
	private ClientController clCtrl;
	
	private boolean connected = false;
	public boolean isConnected() { return this.connected; }
	
	public TTTMessengerClient() {
		
		super.setTitle("TTT Messenger Client");

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
        // create annotation drawing panel
		JAnnotationPanel drawPanel = new JAnnotationPanel();
		drawPanel.setMinimumSize(new Dimension(800,600));
		drawPanel.setPreferredSize(new Dimension(800,600));
		drawPanel.setMaximumSize(new Dimension(800,600));
		drawPanel.setSize(new Dimension(800,600));
		JScrollPane drawScrollPanel = new JScrollPane(drawPanel);
		drawScrollPanel.setMaximumSize(new Dimension(800,600));
		
		// create text input box
		JPanel pnlTxt = new JPanel();
		pnlTxt.setLayout(new BoxLayout(pnlTxt, BoxLayout.LINE_AXIS));
		
		txtArea = new JTextArea();
		JScrollPane scrollTxt = new JScrollPane(txtArea);
		// scrollTxt.setMinimumSize(new Dimension(0,59));
		scrollTxt.setMaximumSize(new Dimension(Short.MAX_VALUE,60));
		
		pnlTxt.add(scrollTxt);
		
		JButton btn1 = new JButton(Constants.getIcon("msgclient_sendtext.png"));
		btn1.setActionCommand("send text");
		btn1.setToolTipText("send text");
		pnlTxt.add(btn1);
		
		JButton btn2 = new JButton(Constants.getIcon("msgclient_sendtextsheet.png"));
		btn2.setActionCommand("send text+sheet");
		btn2.setToolTipText("send text and sheet");
		pnlTxt.add(btn2);
		
		JButton btn3 = new JButton(Constants.getIcon("msgclient_sendsheet.png"));
		btn3.setActionCommand("send sheet");
		btn3.setToolTipText("send sheet");
		pnlTxt.add(btn3);
						
		// create controller
		this.clCtrl = new ClientController(drawPanel, this);
		btn1.addActionListener(clCtrl);
		btn2.addActionListener(clCtrl);
		btn3.addActionListener(clCtrl);

		// show login dialog & try to connect
		LoginDialog dlgLog = new LoginDialog();
		int result = JOptionPane.showConfirmDialog(this, dlgLog, "Login to TTT Messaging", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			dispose();
			return;
		} else {
			Socket s = null;
			// try to connect to given server
			boolean trying = true;
			while(trying) {
				try {
					s = new Socket(dlgLog.getServer(),dlgLog.getPort());
					trying = false;
				} catch (IOException ioe) {
					result = JOptionPane.showConfirmDialog(this, "Could not open connection.\n" + ioe.toString() + "\nTry again?", "Messenger Connection", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (result == JOptionPane.NO_OPTION) trying = false;
					s = null;
				}
			}
			if (s == null) {
				// could not connect and no more tries -> exit
				dispose();
				return;
			}
			cc = new ClientConnection(s, this.clCtrl);
			cc.start();
			String name = dlgLog.getUserName();
			if(name.trim().length() > 0) cc.sendName(name.trim());
		}

		// create draw tools
		JClientPaintControls paintCtrl = new JClientPaintControls(drawPanel, clCtrl);		
		getContentPane().add(paintCtrl, BorderLayout.NORTH);
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, drawScrollPanel, pnlTxt);		
		getContentPane().add(split, BorderLayout.CENTER);
		
		// close connection on closing
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we){
				if (cc != null) cc.close();
			}
		});
		
		setSize(1024,768);
		setVisible(true);
		
		// query size of VNC session from server to adjust annotation panel size
		cc.querySize();
		
		connected = true;
	}
	
	/**
	 * Main method for starting client without starting TTT before
	 * @param args  command line options (none supported)
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new TTTMessengerClient();
			}
		});
	}
	
	/**
	 * get the message text from text area.
	 * Used by {@link ClientController}.
	 * @return  message text
	 */
	public String getMessageText() {
		return txtArea.getText();
	}
}
