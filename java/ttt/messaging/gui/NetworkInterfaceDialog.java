package ttt.messaging.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * displays all available network interfaces which have an IP.
 * interfaces with loopback, local and site local addresses are initially hidden.
 * @author Thomas Doehring
 */
public class NetworkInterfaceDialog extends JPanel {

	static final long serialVersionUID = 1L;
	
	private ArrayList<InetAddress> ipList;
	private ArrayList<JRadioButton> rbList;
	
	private JCheckBox cbHide;
	//WEB: private JCheckBox cbWeb;
	private JPanel panelIP;
	
	public NetworkInterfaceDialog() {
		super();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		panelIP = new JPanel();
		panelIP.setBorder(BorderFactory.createTitledBorder("choose the network interface for messaging"));
		
		gatherIPs(true);
		
		cbHide = new JCheckBox("hide loopback/link local/site local addresses");
		cbHide.setSelected(true);
		cbHide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(cbHide.isSelected()) {
					InetAddress ip;
					for(int i = 0; i < rbList.size(); i++) {
						ip = ipList.get(i);
						if(ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress()) {
							rbList.get(i).setVisible(false);
						}
					}
				} else {
					for (int i = 0; i < rbList.size(); i++) {
						rbList.get(i).setVisible(true);
					}
				}
			}		
		});
		
		panelIP.add(cbHide);
		add(panelIP);

		//WEB: option for starting web messaging
//		JPanel panelWeb = new JPanel();
//		panelWeb.setBorder(BorderFactory.createTitledBorder("Web Messaging"));
//		cbWeb = new JCheckBox("start web messaging");
//		cbWeb.setSelected(false);
//		panelWeb.add(cbWeb);
//		add(panelWeb);
	}

	private void gatherIPs(boolean filterLocal) {
		
		ipList = new ArrayList<InetAddress>();
		rbList = new ArrayList<JRadioButton>();
		
		try {
			Enumeration<NetworkInterface> enIFs = NetworkInterface.getNetworkInterfaces();

			ButtonGroup grp = new ButtonGroup();
			
			// go through all network interfaces and check their IPs
			// if IP avaible -> create radio button entry
			while(enIFs.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface)enIFs.nextElement();
				
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while(ips.hasMoreElements()) {
					InetAddress ip = ips.nextElement();
					
					if(ip instanceof Inet4Address) {
						boolean localIP = ip.isLinkLocalAddress() || ip.isLoopbackAddress() || ip.isSiteLocalAddress();

						ipList.add(ip);

						JRadioButton rb = new JRadioButton(ip.toString().substring(1) + " - " + ni.getDisplayName());
						rb.setVisible(!(filterLocal && localIP));
						rbList.add(rb);
						grp.add(rb);
					}
				}
			}
			
			panelIP.setLayout(new GridLayout(rbList.size()+1,1));
			
			for (JRadioButton rb : rbList) {
				panelIP.add(rb);
			}
			
		} catch (java.net.SocketException se) { /* ignore */ }
	}
	
	/**
	 * get the IP the user has chosen.
	 * @return  the selected IP
	 */
	public InetAddress getSelectedIP() {
		InetAddress ip = null;
		for(int i = 0; i < rbList.size(); i++) {
			if (rbList.get(i).isSelected()) {
				ip = ipList.get(i);
			}
		}
		return ip;
	}
	
//WEB:	
//	public boolean startWebMessaging() {
//		return this.cbWeb.isSelected();
//	}
}
