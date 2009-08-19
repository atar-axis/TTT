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
 * Created on 21.11.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Player;
import javax.media.Processor;
import javax.media.format.VideoFormat;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class VideoMonitorPanel extends JPanel {

    public static void main(String[] args) throws Exception {
        // default video players are created as heavy componentes - we have to change this
        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, Boolean.TRUE);

        VideoMonitorPanel videoPanel = new VideoMonitorPanel();
        JFrame frame = new JFrame("Video Monitor");
        frame.getContentPane().add(videoPanel);
        frame.pack();
        frame.setVisible(true);
    }

    public VideoMonitorPanel() throws Exception {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        Vector captureDevices = CaptureDeviceManager.getDeviceList(new VideoFormat(null));
        if (captureDevices.isEmpty())
            throw new Exception("No video capture device available");

        // get first device
        // TODO: give choice
        CaptureDeviceInfo captureDeviceInfo = (CaptureDeviceInfo) captureDevices.get(0);
        videoProcessor = Manager.createProcessor(captureDeviceInfo.getLocator());
        videoProcessor.configure();
        while (videoProcessor.getState() < Processor.Configured)
            Thread.sleep(100);
        videoProcessor.getTrackControls()[0].setFormat(new VideoFormat(null, new Dimension(176, 144),
                Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED));
        videoProcessor.realize();
        while (videoProcessor.getState() < Processor.Realized)
            Thread.sleep(100);
        player = Manager.createRealizedPlayer(videoProcessor.getDataOutput());
        videoProcessor.start();
        player.start();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        panel.add(player.getVisualComponent());
        add(panel);
    }

    private Processor videoProcessor;
    private Player player;

    public void stop() {
        player.close();
        videoProcessor.close();
    }

    // finds all capture devices and filters out all video capture devices
    CaptureDeviceInfo findCaptureDevices() {
        Vector m_Devices = CaptureDeviceManager.getDeviceList(null);
        Vector<CaptureDeviceInfo> m_VideoDevices = new Vector<CaptureDeviceInfo>();

        // get all videodevices
        if (m_Devices != null && m_Devices.size() > 0) {
            int deviceCount = m_Devices.size();
            Format[] formats;
            for (int i = 0; i < deviceCount; i++) {
                CaptureDeviceInfo m_CaptureDeviceInfo = (CaptureDeviceInfo) m_Devices.elementAt(i);
                formats = m_CaptureDeviceInfo.getFormats();
                for (int j = 0; j < formats.length; j++) {
                    if (formats[j] instanceof VideoFormat) {
                        m_VideoDevices.addElement(m_CaptureDeviceInfo);
                        break;
                    }
                }
            }
        }
        if (m_VideoDevices.size() == 0) {
            return null;
        } else
            return m_VideoDevices.get(0);
    }
}
