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

package ttt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import ttt.messages.Message;

// TODO: rework this class - it's just a copy and paste and hack version from old TTT

/**
 * @author Peter Ziewer (University of Trier,Germany)
 * 
 * receives UDP packets and creates InputStream
 */
public class UDPInputStream {

    DatagramSocket socket;
    InputStream in;
    InetAddress serverAddress;
    int serverPort;
    ProtocolPreferences prefs;

    /**
     * Constructor for UDPInputStream.
     */
    public UDPInputStream(InetAddress localAddress, String host, int port, String serverAddress, int serverPort,
            ProtocolPreferences prefs) throws IOException {
        // System.out.println(host+":"+port+" from "+serverAddress+":"+serverPort);

        this.prefs = prefs;

        this.serverAddress = InetAddress.getByName(serverAddress);
        this.serverPort = serverPort;

        InetAddress address = InetAddress.getByName(host);

        if (address.isMulticastAddress()) {
            socket = new MulticastSocket(port);
            ((MulticastSocket) socket).joinGroup(address);
        } else {
            try {
                // try to open port on dedicated local address (supporting multiple network adapters)
                System.out.println("try to open " + localAddress);
                socket = new DatagramSocket(port, localAddress);

            } catch (Exception e) {
                // default local address
                System.out.println("Failed to open port " + port + " on " + localAddress
                        + " - Trying default network adapter instead");
                socket = new DatagramSocket(port);
            }
        }
        // TODO: availabe test, unicast reconnect if needed
        // socket.setSoTimeout(35000);
        socket.setSoTimeout(1000);
        socket.setReceiveBufferSize(1024 * 64);
        System.out.println("UDP Socket ReceiveBufferSize: " + socket.getReceiveBufferSize());
    }

    boolean terminate;

    public void close() throws IOException {
        socket.close();
        terminate = true;
    }

    // /**
    // * @see java.io.InputStream#read()
    // */
    // public int read() throws IOException {
    // try {
    // int b = in.read();
    // if (b >= 0)
    // return b;
    // // input is empty, receive new packet
    // getDatagram();
    // // read again
    // return read();
    // } catch (Exception e) {
    // getDatagram();
    // return read();
    // }
    // }
    //
    // public void drop() {
    // in = new ByteArrayInputStream(new byte[0]);
    // }

    // bytes 0-3 contain message size
    byte[] buffer = new byte[1024 * 64 + 1];
    byte[] unpack = new byte[1024 * 70];
    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
    Inflater inflater = new Inflater();
    byte count_expected = -1;

    int timeoutCount = 0;

    // receive udp packet
    ArrayList<Message> getMessages() throws IOException {
        // receive next packet
        // accept only packets from server
        do {
            // set packet length to maximum
            packet.setLength(buffer.length);
            while (!terminate)
                try {
                    // wait for packet
                    socket.receive(packet);
                    break;
                } catch (InterruptedIOException e) {
                    // timeout
                    timeoutCount++;
                    if (timeoutCount > 15) {
                        // too many timeouts
                        System.out.println("Too many timeouts. Reachability Test not implemented yet.");
                        // try {
                        // // test if server is alive
                        //
                        // String host;
                        // int port;
                        // if (TTT.getInstance().connection instanceof UDPConnectionTCPInit) {
                        // UDPConnectionTCPInit connection = (UDPConnectionTCPInit) (TTT.getInstance().connection);
                        // host = connection.host;
                        // port = connection.port;
                        // } else {
                        // throw new IOException("No output stream available.");
                        // }
                        // Socket socket = new Socket(host, port);
                        // socket.getOutputStream().write("reachability".getBytes());
                        // socket.close();
                        //
                        // // server is alive
                        // Object[] options = { "Continue waiting", "Reconnect", "Disconnect" };
                        // int answer = JOptionPane.showOptionDialog(TTT.getInstance().viewerFrame.getViewer(),
                        // "Time Out ! Server is running.", "Time Out", JOptionPane.YES_NO_CANCEL_OPTION,
                        // JOptionPane.ERROR_MESSAGE, null, options, options[1]);
                        //
                        // if (answer == JOptionPane.YES_OPTION)
                        // timeoutCount = 0;
                        // if (answer == JOptionPane.NO_OPTION)
                        // TTT.getInstance().reconnect();
                        // if (answer == JOptionPane.CANCEL_OPTION)
                        // TTT.getInstance().disconnect();
                        //
                        // } catch (IOException ioe) {
                        // // server unreachable
                        // Object[] options = { "Continue waiting", "Disconnect" };
                        // int answer = JOptionPane
                        // .showOptionDialog(
                        // TTT.getInstance().viewerFrame.getViewer(),
                        // TTT.getInstance().connection instanceof UDPConnectionTCPInit ? "Time Out ! Server
                        // unreachable."
                        // : "Time Out !", "Time Out", JOptionPane.YES_NO_OPTION,
                        // JOptionPane.ERROR_MESSAGE, null, options, options[1]);
                        //
                        // if (answer == JOptionPane.YES_OPTION)
                        // timeoutCount = 0;
                        // if (answer == JOptionPane.NO_OPTION)
                        // TTT.getInstance().disconnect();
                        // }
                    }
                }
            // System.out.println("packet from "+packet.getAddress().getHostName()+" port "+packet.getPort()+" size
            // "+packet.getLength());
        } while ((packet.getPort() != serverPort || !packet.getAddress().equals(serverAddress)) && !terminate);

        // done - return empty list
        if (terminate)
            return new ArrayList<Message>();

        // reset timeout counter
        timeoutCount = 0;

        int len = packet.getLength() - 1; // msg size without header

        // compare packet counter to detect packet loss
        byte count = (byte) (buffer[0] & 127); // header

        // System.out.print(count+"\t");
        // System.out.println(count+"\t"+count_expected);
        if (!terminate && count > count_expected && count_expected >= 0)
            System.out.println("Packet loss detected. (Got #" + count + " instead of #" + count_expected + ")");
        if (!terminate && count < count_expected && count_expected >= 0)
            System.out.println("Packet too late detected. (Got #" + count + " instead of #" + count_expected + ")");

        if (count + 1 > count_expected)
            count_expected = ++count;
        // count_expected = ++count;

        // unpack buffer if needed
        // System.out.println(count+".\t"+len+((buffer[0] & 128)!=0?" packed":""));
        byte[] unpacked;

        if ((buffer[0] & 128) == 0) {
            // buffer is not packed
            unpacked = buffer;
        } else {
            // buffer is packed
            // unpack
            inflater.reset();
            inflater.setInput(buffer, 1, len);
            try {
                len = inflater.inflate(unpack, 1, unpack.length - 1);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            unpacked = unpack;
        }

        // System.out.println(" read "+ len+" bytes "+(len>5?Constants.encodingToString(unpacked[5]):""));

        // create new InputStream
        in = new ByteArrayInputStream(unpacked, 1, len);
        // System.out.print("length: "+len+"\tavailable: "+in.available());

        // TODO:
        // read and buffer messages in own thread
        // deal with wrong packet order (replay instead of dropping late messages)

        ArrayList<Message> messages = new ArrayList<Message>();
        while (in.available() > 0)
            messages.add(Message.readMessage(new DataInputStream(in), prefs));

        // System.out.println("\tleft "+in.available());
        return messages;
    }
}