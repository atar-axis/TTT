package ttt;

/*
 *  MODIFIED VERSION OF
 *  
 * @(#)AVReceive2.java	1.3 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

import java.awt.Window;
import java.net.InetAddress;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.control.BufferControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SessionEvent;

/**
 * AVReceive2 to receive RTP transmission using the new RTP API.
 */
public class AVReceiveSimple implements ReceiveStreamListener, SessionListener, ControllerListener {
    String sessions = null;
    RTPManager mgrs = null;
    Vector playerWindows = null;
    Player player = null;

    boolean dataReceived = false;
    Object dataSync = new Object();
    Window rootFrame;

    public AVReceiveSimple(String sessions) {
        this.sessions = sessions;
    }

    protected boolean initialize() {

        try {
            InetAddress ipAddr;
            SessionAddress localAddr = new SessionAddress();
            SessionAddress destAddr;

            playerWindows = new Vector();

            SessionLabel session;

            // Parse the session addresses.
            try {
                session = new SessionLabel(sessions);
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to parse the session address given: " + sessions);
                return false;
            }

            System.err.println("  - Open RTP session for: addr: " + session.addr + " port: " + session.port + " ttl: "
                    + session.ttl);

            mgrs = (RTPManager) RTPManager.newInstance();
            mgrs.addSessionListener(this);
            mgrs.addReceiveStreamListener(this);

            ipAddr = InetAddress.getByName(session.addr);

            if (ipAddr.isMulticastAddress()) {
                // local and remote address pairs are identical:
                localAddr = new SessionAddress(ipAddr, session.port, session.ttl);
                destAddr = new SessionAddress(ipAddr, session.port, session.ttl);
            } else {
                localAddr = new SessionAddress(InetAddress.getLocalHost(), session.port);
                destAddr = new SessionAddress(ipAddr, session.port);
            }

            mgrs.initialize(localAddr);

            // You can try out some other buffer size to see
            // if you can get better smoothness.
            BufferControl bc = (BufferControl) mgrs.getControl("javax.media.control.BufferControl");
            if (bc != null)
                bc.setBufferLength(350);

            mgrs.addTarget(destAddr);

        } catch (Exception e) {
            System.err.println("Cannot create the RTP Session: " + e.getMessage());
            return false;
        }

        // Wait for data to arrive before moving on.

        long then = System.currentTimeMillis();
        long waitingPeriod = 10000; // wait for a maximum of 10 secs.

        try {
            synchronized (dataSync) {
                while (!dataReceived && System.currentTimeMillis() - then < waitingPeriod) {
                    if (!dataReceived)
                        System.err.println("  - Waiting for RTP data to arrive...");
                    dataSync.wait(1000);
                }
            }
        } catch (Exception e) {}

        if (!dataReceived) {
            System.err.println("No RTP data was received.");
            close();
            return false;
        }

        return true;
    }

    public boolean isDone() {
        return playerWindows.size() == 0;
    }

    /**
     * Close the players and the session managers.
     */
    protected void close() {
        try {
            player.close();
        } catch (Exception e) {}

        // close the RTP session.
        // for (int i = 0; i < mgrs.length; i++) {
        if (mgrs != null) {
            mgrs.removeTargets("Closing session from AVReceive");
            mgrs.dispose();
            mgrs = null;
        }
        // }
    }

    /**
     * SessionListener.
     */
    public synchronized void update(SessionEvent evt) {}

    /**
     * ReceiveStreamListener
     */
    public synchronized void update(ReceiveStreamEvent evt) {

        ReceiveStream stream = evt.getReceiveStream(); // could be null.

        if (evt instanceof RemotePayloadChangeEvent) {

            System.err.println("  - Received an RTP PayloadChangeEvent.");
            System.err.println("Sorry, cannot handle payload change.");
            System.exit(0);

        } else if (evt instanceof NewReceiveStreamEvent) {

            try {
                stream = ((NewReceiveStreamEvent) evt).getReceiveStream();
                DataSource ds = stream.getDataSource();

                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.media.rtp.RTPControl");
                if (ctl != null) {
                    System.err.println("  - Recevied new RTP stream: " + ctl.getFormat());
                } else
                    System.err.println("  - Recevied new RTP stream");

                // create a player by passing datasource to the Media Manager
                player = javax.media.Manager.createPlayer(ds);
                if (player == null)
                    return;

                player.addControllerListener(this);
                player.realize();

                // Notify intialize() that a new stream had arrived.
                synchronized (dataSync) {
                    dataReceived = true;
                    dataSync.notifyAll();
                }

            } catch (Exception e) {
                System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
                return;
            }

        }
    }

    /**
     * ControllerListener for the Players.
     */
    public synchronized void controllerUpdate(ControllerEvent ce) {}

    /**
     * A utility class to parse the session addresses.
     */
    class SessionLabel {

        public String addr = null;
        public int port;
        public int ttl = 1;

        SessionLabel(String session) throws IllegalArgumentException {

            int off;
            String portStr = null, ttlStr = null;

            if (session != null && session.length() > 0) {
                while (session.length() > 1 && session.charAt(0) == '/')
                    session = session.substring(1);

                // Now see if there's a addr specified.
                off = session.indexOf('/');
                if (off == -1) {
                    if (!session.equals(""))
                        addr = session;
                } else {
                    addr = session.substring(0, off);
                    session = session.substring(off + 1);
                    // Now see if there's a port specified
                    off = session.indexOf('/');
                    if (off == -1) {
                        if (!session.equals(""))
                            portStr = session;
                    } else {
                        portStr = session.substring(0, off);
                        session = session.substring(off + 1);
                        // Now see if there's a ttl specified
                        off = session.indexOf('/');
                        if (off == -1) {
                            if (!session.equals(""))
                                ttlStr = session;
                        } else {
                            ttlStr = session.substring(0, off);
                        }
                    }
                }
            }

            if (addr == null)
                throw new IllegalArgumentException();

            if (portStr != null) {
                try {
                    Integer integer = Integer.valueOf(portStr);
                    if (integer != null)
                        port = integer.intValue();
                } catch (Throwable t) {
                    throw new IllegalArgumentException();
                }
            } else
                throw new IllegalArgumentException();

            if (ttlStr != null) {
                try {
                    Integer integer = Integer.valueOf(ttlStr);
                    if (integer != null)
                        ttl = integer.intValue();
                } catch (Throwable t) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    public static AVReceiveSimple createPlayer(String sessionAddress) {

        AVReceiveSimple avReceive = new AVReceiveSimple(sessionAddress);
        if (!avReceive.initialize()) {
            System.err.println("Failed to initialize the sessions.");
            avReceive = null;
        }
        return avReceive;
    }
}
