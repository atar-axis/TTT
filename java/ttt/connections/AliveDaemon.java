// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

package ttt.connections;

/**
 * @author Peter Ziewer (University of Trier,Germany)
 * sending alive messages to server periodically  
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class AliveDaemon extends TimerTask {
	DatagramSocket socket;
	DatagramPacket packet;
	byte[] msg = {(byte) 42 };
	Timer timer;

	AliveDaemon(InetAddress address, int port) {
		System.out.println("AliveDaemon: " + address + ":" + port);

		try {
			socket = new DatagramSocket();
			packet = new DatagramPacket(msg, 0, address, port);
			socket.send(packet);

			timer = new Timer(true);
			timer.scheduleAtFixedRate(this, 30000, 60000);
		} catch (Exception e) {
			System.err.println("Couldn't start AliveDaemon: " + e);
		}
	}

	void terminate() throws IOException {
		timer.cancel();

		// send terminate message
		packet.setLength(1);
		socket.send(packet);
	}

	public void run() {
		try {
			socket.send(packet);
		} catch (Exception e) {
			System.err.println(e);
			javax.swing.JOptionPane.showMessageDialog(null, "Server is done. Exiting.");
		}
	}
}
