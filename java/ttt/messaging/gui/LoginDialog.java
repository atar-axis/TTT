package ttt.messaging.gui;

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ttt.NumberField;

/**
 * GUI for a login dialog. Asks for host name and port and for an optional user name.
 * @author Thomas Doehring
 */
public class LoginDialog extends JPanel {

	static final long serialVersionUID = 1L;
	
	JTextField txtServer;
	NumberField nrPort;
	JTextField txtName;
	
	public LoginDialog() {
		super();
		
		setLayout(new GridBagLayout());
		GridBagConstraints cBag = new GridBagConstraints();
		
		JLabel lblServer = new JLabel("Server");
		cBag.gridx = 0;
		cBag.gridy = 0;
		cBag.anchor = GridBagConstraints.WEST;
		add(lblServer, cBag);
		
		JLabel lblPort = new JLabel("Port");
		cBag.gridx = 1;
		cBag.gridy = 0;
		add(lblPort, cBag);
		
		txtServer = new JTextField(30);
		txtServer.setText("");
		txtServer.setToolTipText("name or ip of messaging server");
		cBag.gridx = 0;
		cBag.gridy = 1;
		add(txtServer, cBag);
		
		nrPort = new NumberField(5);
		nrPort.setText("7777");
		nrPort.setToolTipText("port of messaging server");
		cBag.gridx = 1;
		cBag.gridy = 1;
		add(nrPort, cBag);
		
		JLabel lblName = new JLabel("Name:");
		cBag.gridx = 0;
		cBag.gridy = 2;
		cBag.insets = new Insets(20,0,0,0);
		add(lblName, cBag);
		
		txtName = new JTextField(10);
		txtName.setToolTipText("enter a name (optional, displayed in message list)");
		cBag.gridx = 0;
		cBag.gridy = 3;
		cBag.insets = new Insets(0,0,0,0);
		add(txtName, cBag);
	}

	// getters for the entered informations
	public String getServer() { return this.txtServer.getText(); }
	public int getPort() { return this.nrPort.getNumber(); }
	public String getUserName() { return this.txtName.getText(); }
}
