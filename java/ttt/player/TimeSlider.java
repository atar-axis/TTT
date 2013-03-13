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
 * Created on 06.02.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt.player;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ttt.Constants;
import ttt.record.Recording;

/***********************************************************************************************************************
 * using own listener handling, to allow setting a value without releasing an
 * event
 **********************************************************************************************************************/

public class TimeSlider extends JSlider {

	// own change event including timestamp and adjustment flag
	private MyChangeEvent changeEvent = new MyChangeEvent(this);

	// label to display time
	private JLabel timeLabel;

	public TimeSlider(final Recording recording, JLabel timeLabel) {
		super(HORIZONTAL, 0, recording.getDuration(), 0);
		setBackground(Color.WHITE);
		setMajorTickSpacing(10 * 60000);
		setMinorTickSpacing(5 * 60000);
		setPaintTicks(true);
		setToolTipText("Adjust playback time");

		// label to display time
		this.timeLabel = timeLabel;

		// pass event handling to recording
		addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				recording.sliderStateChanged(e);// , recording);
				// recording.sliderStateChanged(e);

			}
		});

		// handle clicking on the slider track to set playback time
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {

				// only set the timeSlider to the selected value if
				// significantly different from current value
				// don't want it being set if the user is only trying to click
				// on the thumb
				int newValue = getMaximum() / getWidth() * e.getX();

				int difference = newValue - getValue();
				if (difference < 0) {
					difference = -difference;
				}
				if (difference > (getMaximum() - getMinimum()) / 100) {
					setValue((int) newValue);
				}
				// set focus back to main compinent
				recording.graphicsContext.requestFocusInWindow();

			}
		});
	}

	// /////////////////////////////////////////////////////////
	// listener handling
	// /////////////////////////////////////////////////////////

	// use own listener handling to control event firing
	private ArrayList<ChangeListener> changedListeners = new ArrayList<ChangeListener>();

	// register listener
	public void addChangeListener(ChangeListener listener) {
		changedListeners.add(listener);
	}

	// unregister listener
	public void removeChangeListener(ChangeListener listener) {
		changedListeners.remove(listener);
	}

	// fire event
	// NOTE: cannot override fireStateChanged(), because this will be called by
	// other JSlider methods
	private void myFireStateChanged() {
		for (int i = 0; i < changedListeners.size(); i++)
			changedListeners.get(i).stateChanged(changeEvent);
	}

	// /////////////////////////////////////////////////////////////////////////
	// setting value and firing events
	// /////////////////////////////////////////////////////////////////////////

	// override set value
	public void setValue(int value) {
		setValue(value, true);
	}

	// set value with or without firing an event
	//synchronized --- causes Freeze
        public void setValue(int value, boolean fireEvent) {

		// ignore during adjustment
		if (!fireEvent && getValueIsAdjusting())
			return;

		// ignore event caused by clicking on slider or if player paused
		// (causing one msec steps)
		switch (getValue() - value) {
		case -1:
		case 0:
		case 1:
			return;
		}

		// update slider and label
		super.setValue(value);
		if (timeLabel != null)
			timeLabel.setText(Constants.getStringFromTime(value, false));

		// fire event with given value
		if (fireEvent) {
			changeEvent.time = value;
			myFireStateChanged();
		}
	}

	// adjusting
	public void setValueIsAdjusting(boolean adjusting) {
		super.setValueIsAdjusting(adjusting);

		changeEvent.adjusting = adjusting;
		// event is only needed for end of adjustment (causes setting audio
		// time)
		if (!adjusting)
			myFireStateChanged();
	}

	// ///////////////////////////////////////////////////////////////////////
	// change event class
	// ///////////////////////////////////////////////////////////////////////

	// change event including timestamp and adjustment flag
	public class MyChangeEvent extends ChangeEvent {
		public int time;
		public boolean adjusting;

		public MyChangeEvent(Object object) {
			super(object);
		}
	}
}
