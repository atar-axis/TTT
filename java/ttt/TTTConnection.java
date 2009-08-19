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

/*
 * Created on 01.12.2005
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import javax.media.Player;
import javax.swing.JOptionPane;

import ttt.messages.Message;
import ttt.messages.MessageConsumer;
import ttt.messages.MessageProducer;

public class TTTConnection implements Connection, MessageProducer, Runnable, Closeable, VolumeControl {

    // I/O
    private Socket socket;
    private ReconnectingInputStream userInputStream;
    private int accessMode = Constants.Unicast;
    private AVReceiveSimple[] players;
    private AVReceiveSimple audioPlayer; // , videoPlayer;
    private AliveDaemon aliveDaemon;

    // initialization pereferences
    private ProtocolPreferences prefs;

    public TTTConnection(InetAddress host, int port, int accessMode) {
        prefs = new ProtocolPreferences();
        prefs.host = host;
        prefs.port = port;
        this.accessMode = accessMode;
    }

    // returns component which displays video
    public Component getVideoComponent() {
        // search video component
        if (players != null)
            for (AVReceiveSimple player : players) {
                Component component = player.player.getVisualComponent();
                if (component != null)
                    return component;
            }
        return null;
    }

    // TODO: add switching to another host
    // TODO: catch errors, like host not found

    // connect to given vnc server
    public synchronized void connect() throws IOException {
        // cleanup (if needed)
        close();

        // init
        // TODO: maybe NoInputStream and NoOutputsStream is needed to avoid NullPointerExceptions

        // I/O stream used during initialisation
        DataInputStream initializationDataInputStream = null;
        OutputStream initializationOutputstream = null;
        InputStream initializationInputStream = null;

        // connect
        while (socket == null) {
            System.out.println("    Connect to " + prefs.host + " : " + prefs.port);
            // open socket and I/O streams
            // TODO: timeout handling
            try {
                socket = new Socket(prefs.host, prefs.port);
            } catch (IOException e) {
                System.out.println("    Connection failed: " + e);

                // failed: wait and retry (no dialog during automatic reconnect)
                if (reconnecting) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                    continue;
                }

                // ask to retry
                else if (JOptionPane.showConfirmDialog(TTT.getInstance(), "Connection failed: " + e + "\nTry again?",
                        "Connection Failure", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    continue;

                // or quit
                throw e;
            }
            initializationInputStream = new BufferedInputStream(socket.getInputStream(), 16384);
            initializationDataInputStream = new DataInputStream(initializationInputStream);
            initializationOutputstream = socket.getOutputStream();

            // read server version
            byte[] b = new byte[12];
            initializationDataInputStream.readFully(b);
            prefs.versionMsg = new String(b);
            checkVersionMessage(prefs.versionMsg);

            // send client version
            initializationOutputstream.write(Constants.VersionMessageTTT.getBytes());

            // read status
            int connectionStatus = initializationDataInputStream.readInt();
            switch (connectionStatus) {
            case Constants.ConnectionFailed:
                byte[] reason = new byte[initializationDataInputStream.readUnsignedByte()];
                initializationDataInputStream.readFully(reason);
                throw new IOException("Connection failed: " + new String(reason));

            case Constants.ConnectionOK:
                break;

            default:
                throw new IOException("Connection failed: Unknown server response " + connectionStatus);
            }

            // request access mode
            initializationOutputstream.write(accessMode);

            // authentication - try again if failed
            if (!authentication(initializationDataInputStream, initializationOutputstream))
                continue;
        }

        // read server pixel format and name
        readServerInit(initializationDataInputStream);

        // read color table for shapes
        // read number of colors
        int colorCount = initializationDataInputStream.readUnsignedByte();
        if (colorCount > 0) {
            // TODO: not supported (colors are read, but will be ignored)
            // init new colos table
            Color[] colors = new Color[colorCount];
            // read color table
            for (int i = 0; i < colorCount; i++) {
                colors[i] = new Color(initializationDataInputStream.readInt(), true);
            }
        }

        // ttt stream
        String ip = initializationDataInputStream.readUnsignedByte() + "."
                + initializationDataInputStream.readUnsignedByte() + "."
                + initializationDataInputStream.readUnsignedByte() + "."
                + initializationDataInputStream.readUnsignedByte();
        InetAddress address = InetAddress.getByName(ip);
        int udpPort = initializationDataInputStream.readUnsignedShort();
        int localPort = initializationDataInputStream.readUnsignedShort();
        int aliveControllerPort = initializationDataInputStream.readUnsignedShort();

        System.out.println("UDP IP: " + address.getHostName() + " port " + udpPort + " (from port " + localPort + ")");
        if (accessMode == Constants.Unicast)
            System.out.println("    AliveController: " + address.getHostName() + " port " + aliveControllerPort);

        // additional media stream
        int number = initializationDataInputStream.readUnsignedByte();
        if (number > 0) {
            players = new AVReceiveSimple[number];

            for (int i = 0; i < number; i++) {
                String mediaIP = initializationDataInputStream.readUnsignedByte() + "."
                        + initializationDataInputStream.readUnsignedByte() + "."
                        + initializationDataInputStream.readUnsignedByte() + "."
                        + initializationDataInputStream.readUnsignedByte();
                InetAddress mediaAddress = InetAddress.getByName(mediaIP);
                int mediaPort = initializationDataInputStream.readUnsignedShort();
                System.out.println("RTP IP: " + mediaAddress.getHostName() + " port " + mediaPort);
                // start media player
                players[i] = AVReceiveSimple.createPlayer(mediaIP + "/" + mediaPort);

                if (players[i] == null) {
                    if (JOptionPane.showConfirmDialog(null, "Cannot create audio/video player. Continue anyway?",
                            "Connection failed:", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                        // connected = false;
                        throw new IOException("Cannot create audio/video player.");
                    }
                    System.out.println("ERROR: Cannot create audio/video player. - Try to continue anyway.");
                }
            }
            for (int i = 0; i < number; i++)
                if (players[i] != null) {
                    System.out.print("Initializing player #" + (i + 1) + ": ");
                    while (players[i].player.getState() < Player.Realized)
                        try {
                            System.out.print(".");
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                    if (players[i].player.getVisualComponent() != null) {
                        System.out.println(" video ok");
                    } else {
                        audioPlayer = players[i];
                        System.out.println(" audio ok");
                    }
                    players[i].player.start();
                }
        }

        String host = prefs.host.getHostAddress();
        if (address.isMulticastAddress()) {
            udpIn = new UDPInputStream(socket.getLocalAddress(), ip, udpPort, host, localPort, prefs);
        } else {
            udpIn = new UDPInputStream(socket.getLocalAddress(), host, udpPort, host, localPort, prefs);
            // start alive daemon
            aliveDaemon = new AliveDaemon(InetAddress.getByName(host), aliveControllerPort);
        }

        System.out.println(prefs);

        if (udpIn != null) {
            new Thread(this).start();
        } else {
            // TODO: quit? reconnect? ask user?
        }

        initializationDataInputStream = null;
        initializationOutputstream = null;
    }

    // reconnect to server
    // self repairing connection after error detection
    private boolean reconnecting;

    public synchronized void reconnect() {
        reconnecting = true;
        while (reconnecting)
            try {
                // try to connect
                connect();

                // reconnected
                reconnecting = false;

            } catch (IOException e) {
                // failed - wait and retry
                try {
                    // TODO: dynamically adopt timeout
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {}
            }
    }

    private boolean closing;

    // close whatever need to be closed
    public void close() {
        closing = true;

        if (userInputStream != null)
            try {
                userInputStream.close();
            } catch (Exception e) {}
        userInputStream = null;

        if (socket != null)
            try {
                socket.close();
            } catch (IOException e) {}
        socket = null;

        if (udpIn != null)
            try {
                udpIn.close();
            } catch (Exception e) {}
        udpIn = null;

        if (aliveDaemon != null)
            aliveDaemon.cancel();
        aliveDaemon = null;

        if (players != null)
            for (AVReceiveSimple player : players) {
                if (player != null)
                    player.close();
            }
        players = null;
    }

    /*******************************************************************************************************************
     * Initialisation *
     ******************************************************************************************************************/

    // read server version
    private void checkVersionMessage(String versionMsg) throws IOException {
        if (versionMsg == null || versionMsg.length() != 12)
            throw new IOException("Bad server version (" + versionMsg + ")");

        byte[] b = versionMsg.getBytes();

        // examine server version:
        // starting with TTT or RFB
        // followed by one blank
        // followed by 3 digits (major version number)
        // followed by one dot
        // followed by 3 digits (minor version number)
        // followed by one newline
        if ((!(((b[0] == 'T') && (b[1] == 'T') && (b[2] == 'T')) /*
                                                                     * || ((b[0] == 'R') && (b[1] == 'F') && (b[2] ==
                                                                     * 'B'))
                                                                     */
        )) || (b[3] != ' ') || (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9') || (b[6] < '0')
                || (b[6] > '9') || (b[7] != '.') || (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
                || (b[10] < '0') || (b[10] > '9') || (b[11] != '\n')) {
            throw new IOException("Bad server version (" + new String(b) + ")");
        }

        // output
        System.out.print("      Client TTT Version: " + Constants.VersionMessageTTT);
        System.out.print("      Client RFB Version: " + Constants.VersionMessageRFB);
        System.out.print("      Server Version: " + versionMsg);
        if (b[11] != '\n')
            System.out.println();

        // check TTT version number
        if (versionMsg.startsWith("TTT")) {
            int serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
            int serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');

            byte[] bb = Constants.VersionMessageTTT.getBytes();
            int clientMajor = (bb[4] - '0') * 100 + (bb[5] - '0') * 10 + (bb[6] - '0');
            int clientMinor = (bb[8] - '0') * 100 + (bb[9] - '0') * 10 + (bb[10] - '0');

            if (serverMajor > clientMajor || (serverMajor == clientMajor && serverMinor > clientMinor)) {
                JOptionPane.showMessageDialog(null, "Newer Protocol Version detected: " + versionMsg
                        + "\n\tClient Version: " + Constants.VersionMessageTTT
                        + "\n\tUpgrade your version of TeleTeachingTool at http://TTT.Uni-Trier.de",
                        "Newer Protocol Version Detected", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // authentication
    private boolean authentication(DataInputStream initializationDataInputStream,
            OutputStream initializationOutputstream) throws IOException {
        int authScheme = initializationDataInputStream.readInt();
        switch (authScheme) {
        case Constants.ConnectionFailed:
            byte[] reason = new byte[initializationDataInputStream.readInt()];
            initializationDataInputStream.readFully(reason);
            throw new IOException(new String(reason));

        case Constants.NoAuthentication:
            System.out.println("      No authentication needed");
            return true;

        case Constants.Authentication:
            byte[] challenge = new byte[16];
            initializationDataInputStream.readFully(challenge);

            if (prefs.password == null)
                prefs.password = PasswordDialog.getPassword();

            if (prefs.password == null)
                throw new IOException("Aborted by user.");

            if (prefs.password.length() > 8)
                prefs.password = prefs.password.substring(0, 8);
            // Truncate to 8 chars

            byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 };
            System.arraycopy(prefs.password.getBytes(), 0, key, 0, prefs.password.length());

            DesCipher des = new DesCipher(key);

            des.encrypt(challenge, 0, challenge, 0);
            des.encrypt(challenge, 8, challenge, 8);

            initializationOutputstream.write(challenge);

            int authResult = initializationDataInputStream.readInt();

            switch (authResult) {
            case Constants.AuthenticationOK:
                System.out.println("      Authentication succeeded");
                return true;

            case Constants.AuthenticationFailed:
                System.out.println("      Authentication failed: Wrong password");
                // clear password, close connection and try again (reconnect)
                prefs.password = null;
                if (socket != null)
                    socket.close();
                socket = null;
                return false;

            case Constants.AuthenticationTooManny:
                System.out.println("      Authentication failed: Too many tries");
                throw new IOException("Authentication failed: Too many tries");

            default:
                System.out.println("      Unknown authentication result: " + String.valueOf(authResult));
                throw new IOException("Unknown authentication result: " + String.valueOf(authResult));
            }

        default:
            System.out.println("      Unknown authentication result, authScheme: " + String.valueOf(authScheme));
            throw new IOException("Unknown authentication result, authScheme: " + String.valueOf(authScheme));
        }
    }

    // read server initialisation
    private void readServerInit(DataInputStream initializationDataInputStream) throws IOException {
        prefs.framebufferWidth = initializationDataInputStream.readUnsignedShort();
        prefs.framebufferHeight = initializationDataInputStream.readUnsignedShort();
        prefs.bitsPerPixel = initializationDataInputStream.readUnsignedByte();
        switch (prefs.bitsPerPixel) {
        case 8:
            prefs.bytesPerPixel = 1;
            break;
        case 16:
            prefs.bytesPerPixel = 2;
            break;
        case 24:
            prefs.bytesPerPixel = 3;
            break;
        case 32:
        default:
            prefs.bytesPerPixel = 4;
            break;
        }
        prefs.depth = initializationDataInputStream.readUnsignedByte();
        prefs.bigEndian = (initializationDataInputStream.readUnsignedByte() != 0);
        prefs.trueColour = (initializationDataInputStream.readUnsignedByte() != 0);
        prefs.redMax = initializationDataInputStream.readUnsignedShort();
        prefs.greenMax = initializationDataInputStream.readUnsignedShort();
        prefs.blueMax = initializationDataInputStream.readUnsignedShort();
        prefs.redShift = initializationDataInputStream.readUnsignedByte();
        prefs.greenShift = initializationDataInputStream.readUnsignedByte();
        prefs.blueShift = initializationDataInputStream.readUnsignedByte();
        // padding
        initializationDataInputStream.skipBytes(3);
        int nameLength = initializationDataInputStream.readInt();
        byte[] name = new byte[nameLength];
        initializationDataInputStream.readFully(name);
        prefs.name = new String(name);
    }

    /*******************************************************************************************************************
     * implement MessageProducer
     ******************************************************************************************************************/

    private ArrayList<MessageConsumer> messageConsumers = new ArrayList<MessageConsumer>();

    // register listener
    public void addMessageConsumer(MessageConsumer messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.add(messageConsumer);
        }
    }

    // unregister listener
    public void removeMessageConsumer(MessageConsumer messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.remove(messageConsumer);
        }
    }

    // notify all listeners
    public void deliverMessage(Message message) {
        synchronized (messageConsumers) {
            int i = 0;
            while (i < messageConsumers.size()) {
                // TODO: maybe clone message
                // TODO: error handling
                try {
                    messageConsumers.get(i).handleMessage(message);
                } catch (Exception e) {
                    // TODO: maybe: remove consumer (if multiple errors?)
                }
                i++;
            }
        }
    }

    /*******************************************************************************************************************
     * Input / Output
     ******************************************************************************************************************/

    public ProtocolPreferences getProtocolPreferences() {
        return prefs;
    }

    public InputStream getInputStream() {
        return userInputStream;
    }

    public OutputStream getOutputStream() {
        // TODO: needed?
        return null;
    }

    private UDPInputStream udpIn;

    public void run() {
        while (udpIn != null) {
            try {
                ArrayList<Message> messages = udpIn.getMessages();

                for (int i = 0; i < messages.size(); i++) {
                    Message message = messages.get(i);
                    // System.out.println(message);
                    if (message != null)
                        deliverMessage(message);
                }

            } catch (IOException e) {
                if (closing)
                    break;
                else
                    // TODO: ask to reconnect
                    // TODO: untested
                    reconnect();
            }
        }
    }

    private class ReconnectingInputStream extends InputStream {
        private InputStream inputStream;
        private boolean running = true;

        public ReconnectingInputStream(InputStream in) {
            inputStream = in;
        }

        synchronized void setInputStream(InputStream in) {
            inputStream = in;
        }

        public void close() throws IOException {
            running = false;
            super.close();
        }

        // private int count;

        synchronized public int read() {
            while (running)
                try {
                    return inputStream.read();
                } catch (SocketException e) {
                    if (running) {
                        System.out.println("Lost connection: Trying to reconnect");
                        reconnect();
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading: Reading failed");
                    return -1;
                }
            return -1;
        }
    }

    // //////////////////////////////////////////////////////////////////
    // volume control
    // //////////////////////////////////////////////////////////////////
    public int getVolumeLevel() {
        if (audioPlayer != null && audioPlayer.player != null)
            return (int) (100 * audioPlayer.player.getGainControl().getLevel());
        else
            return 0;
    }

    public void setVolumeLevel(int volume) {
        if (audioPlayer != null && audioPlayer.player != null)
            audioPlayer.player.getGainControl().setLevel(volume / 100f);
    }

    public boolean getMute() {
        if (audioPlayer != null && audioPlayer.player != null)
            return audioPlayer.player.getGainControl().getMute();
        else
            return true;
    }

    public void setMute(boolean mute) {
        if (audioPlayer != null && audioPlayer.player != null)
            audioPlayer.player.getGainControl().setMute(mute);
    }
}
