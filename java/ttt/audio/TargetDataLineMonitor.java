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
 * Created on 20.11.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Control.Type;


public class TargetDataLineMonitor implements TargetDataLine {

    public TargetDataLineMonitor(TargetDataLine targetDataLine, AudioMonitorPanel volumeLevelComponent) {
        this.targetDataLine = targetDataLine;
        this.volumeLevelComponent = volumeLevelComponent;
    }

    private TargetDataLine targetDataLine;
    private float meanSampleValue = 0;
    private AudioMonitorPanel volumeLevelComponent;

    public int read(byte[] buffer, int offset, int len) {
        int i = targetDataLine.read(buffer, offset, len);

        while (offset < len) {
            // caculates the frame samples from the given byte array
            // http://www.jsresources.org/faq_audio.html#reconstruct_samples
            float sample = ((buffer[offset + 0] & 0xFF) | (buffer[offset + 1] << 8)) / 32768.0F;
            if (sample < 0)
                sample *= (-1);
            meanSampleValue += sample;
            offset += 2;
        }
        meanSampleValue /= (buffer.length / 2);

        if (volumeLevelComponent != null)
            volumeLevelComponent.setPeakPercentage(meanSampleValue * 2);

        return i;
    }

    public void open(AudioFormat format) throws LineUnavailableException {
        targetDataLine.open(format);
    }

    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        targetDataLine.open(format, bufferSize);
    }

    public int available() {
        return targetDataLine.available();
    }

    public void drain() {
        targetDataLine.drain();
    }

    public void flush() {
        targetDataLine.flush();
    }

    public int getBufferSize() {
        return targetDataLine.getBufferSize();
    }

    public AudioFormat getFormat() {
        return targetDataLine.getFormat();
    }

    public int getFramePosition() {
        return targetDataLine.getFramePosition();
    }

    public float getLevel() {
        return targetDataLine.getLevel();
    }

    public long getLongFramePosition() {
        return targetDataLine.getLongFramePosition();
    }

    public long getMicrosecondPosition() {
        return targetDataLine.getMicrosecondPosition();
    }

    public boolean isActive() {
        return targetDataLine.isActive();
    }

    public boolean isRunning() {
        return targetDataLine.isRunning();
    }

    public void addLineListener(LineListener listener) {
        targetDataLine.addLineListener(listener);
    }

    public void close() {
        targetDataLine.close();
    }

    public Control getControl(Type control) {
        return targetDataLine.getControl(control);
    }

    public Control[] getControls() {
        return targetDataLine.getControls();
    }

    public javax.sound.sampled.Line.Info getLineInfo() {
        return targetDataLine.getLineInfo();
    }

    public boolean isControlSupported(Type control) {
        return targetDataLine.isControlSupported(control);
    }

    public boolean isOpen() {
        return targetDataLine.isOpen();
    }

    public void open() throws LineUnavailableException {
        targetDataLine.open();
    }

    public void removeLineListener(LineListener listener) {
        targetDataLine.removeLineListener(listener);
    }

    public void start() {
        targetDataLine.start();
    }

    public void stop() {
        targetDataLine.stop();
    }
}
