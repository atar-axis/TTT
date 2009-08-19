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
 * Created on 10.01.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.messages;

import java.util.ArrayList;

// non blocking message handling using buffering
public class BufferedMessageConsumer implements MessageConsumer, Runnable {

    private MessageConsumer messageConsumer;
    private Thread thread;

    // creates a non blocking message consumer
    // messages are buffered
    // message handlimng is done in an own thread
    public BufferedMessageConsumer(MessageConsumer consumer) {
        messageConsumer = consumer;
        start();
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    // message buffer
    private ArrayList<Message> messages = new ArrayList<Message>();

    // TODO: thing about values and setters
    // maximum number of messages to be buffered
    public int bufferSize = 1000;
    // yield executing thread to allow message consumption
    public int yieldSize = 100;

    // non blocking message handling
    // until buffer is full
    public void handleMessage(Message message) {
        while (messages.size() > bufferSize)
            Thread.yield();

        // buffer message
        synchronized (messages) {
            messages.add(message);
        }
        // wake thread
        synchronized (this) {
            notify();
        }

        // fairness: give chance to consume
        if (messages.size() > yieldSize)
            Thread.yield();
    }

    // TODO: only for simulation - remove
    private int bandwidth = 1000000;
    private int count = 0;

    // handle messages in buffer
    public void run() {
        try {
            while (true) {
                // handle all messages
                while (!messages.isEmpty()) {
                    Message message;
                    synchronized (messages) {
                        message = messages.remove(0);
                    }
                    try {
                        // System.out.println(message);
                        if (count++ % 1000 == 0)
                            System.out.print(".");

                        messageConsumer.handleMessage(message);
                    } catch (Exception e) {
                        // TODO: handle or close or whatever
                        System.out.println("Error while message handling:");
                        System.out.println(message);
                        e.printStackTrace();
                        System.out.println("Trying to continue anyway.");
                    }

                    // TODO: remove
                    // simulate bandwidth limitation
                    if (!true && message instanceof Message) {
                        count = ((Message) message).getSize();
                        try {
                            int wait = 1000 * count / bandwidth;
                            if (wait > 100)
                                System.out.println(count + " bytes - wait " + (wait) + " msec");
                            Thread.sleep(wait);
                        } catch (InterruptedException e) {}
                    }
                }
                // sleep until receiving new message
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
            }
        } catch (Exception e) {
            // TODO: handle or close or whatever
            System.out.println("player crashed");
            e.printStackTrace();
        }
    }
}
