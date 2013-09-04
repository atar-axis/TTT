// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universit�t M�nchen
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ttt.audio.AudioRecorder;
import ttt.connections.TTTConnection;
import ttt.editor.tttEditor.TTTEditor;
import ttt.editor2.Editor2;
import ttt.gui.GradientPanel;
import ttt.gui.NumberField;
import ttt.gui.XMLHandler;
import ttt.helper.LibraryChecker;
import ttt.messaging.client.TTTMessengerClient;
import ttt.messaging.gui.NetworkInterfaceDialog;
import ttt.messaging.server.TTTMessaging;
import ttt.player.Player;
import ttt.postprocessing.KeyGen;
import ttt.postprocessing.PostProcessorPanel;
import ttt.record.LectureProfile;
import ttt.record.LectureProfileDialog;
import ttt.record.Recording;

public class TTT extends JFrame {
	static final String version = "04.09.2013";

	public static boolean debug = !true;
	public static boolean verbose = true;

	static private JFileChooser fileChooser;
	
	// user preferences
	public static Preferences userPrefs = Preferences.userRoot().node("/TTT");
	static {
		// show tool tips after 500 msec
		ToolTipManager.sharedInstance().setInitialDelay(500);

		// show tool tips for 10 sec
		ToolTipManager.sharedInstance().setDismissDelay(10000);
	}

	public static TTT ttt;

	private JDesktopPane desktop;

	public static boolean enabledNativeLookAndFeel = false;

	private TTT() {

		super("TeleTeachingTool - Version " + version);

		// TODO: handle CTRL-C
		// NOTE: Recorder uses its own handler
		// Runtime.getRuntime().addShutdownHook(new ShutDownHook(this));

		enabledNativeLookAndFeel = userPrefs.getBoolean(
				"enabledNativeLookAndFeel", false);
		if (enabledNativeLookAndFeel) {
			activateNativeLook();
		}

		fileChooser = new JFileChooser();

		if (verbose)
			System.out.println(ABOUT);

		setJMenuBar(createMenu());
		
		desktop = new JDesktopPane();
		setContentPane(desktop);
		
		desktop.setVisible(true);
		// attempt to close all open files before closing
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// TODO: handle window closing
				System.exit(0);
			}
		});
	}

	// ////////////////////////////////////////////////////////
	// Output messages
	// ////////////////////////////////////////////////////////

	// output debug messages for development purpose
	public static void setDebug(boolean debug) {
		TTT.debug = debug;
	}

	// output status messages for users
	public static void setVerbose(boolean verbose) {
		TTT.verbose = verbose;
	}

	// ////////////////////////////////////////////////////////
	// Menu
	// ////////////////////////////////////////////////////////

	private JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();

		// deactivate F10 as key to open first menu, because F10 has other
		// function in Paint Controls
		menuBar.getInputMap(JMenuBar.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0, false), "none");

		// ////////////////////////////////////////////////////////////////////////
		// Player Menu
		// ////////////////////////////////////////////////////////////////////////

		JMenu menu = new JMenu("Student");
		menu.setMnemonic(KeyEvent.VK_S);
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem("open...");
		menuItem.setToolTipText("open and play a ttt recording");
		menuItem.setMnemonic(KeyEvent.VK_O);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		menuItem.setIcon(Constants.getIcon("16x16/fileopen.png"));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final File file = showFileDialog();

				if (file != null)
					// run outside of event dispatching thread
					new Thread(new Runnable() {
						public void run() {
							createFilePlayer(file);
						}
					}).start();
			}
		});
		menu.add(menuItem);

		JMenu submenu = new JMenu("connect...");
		submenu.setToolTipText("connect to a ttt server");
		submenu.setMnemonic(KeyEvent.VK_C);
		submenu.setIcon(Constants.getIcon("16x16/network.png"));
		menu.add(submenu);

		menuItem = new JMenuItem("connect to ttt server");
		menuItem.setToolTipText("specify a custom ttt server");
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// run outside of event dispatching thread
				new Thread(new Runnable() {
					public void run() {
						connectTTT();
					}
				}).start();
			}
		});
		submenu.add(menuItem);

		submenu.addSeparator();

		// TODO: automated listing of known servers
		menuItem = new JMenuItem("teleteaching.uni-trier.de:33229, multicast");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					createOnlinePlayer(InetAddress
							.getByName("teleteaching.uni-trier.de"), 33229,
							Constants.Multicast);
				} catch (UnknownHostException e) {
				}
			}
		});
		submenu.add(menuItem);
		menuItem = new JMenuItem("teleteaching.uni-trier.de:33229, unicast");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					createOnlinePlayer(InetAddress
							.getByName("teleteaching.uni-trier.de"), 33229,
							Constants.Unicast);
				} catch (UnknownHostException e) {
				}
			}
		});
		submenu.add(menuItem);
		menuItem = new JMenuItem("tele1.uni-trier.de:33229, multicast");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					createOnlinePlayer(InetAddress
							.getByName("tele1.uni-trier.de"), 33229,
							Constants.Multicast);
				} catch (UnknownHostException e) {
				}
			}
		});
		submenu.add(menuItem);
		menuItem = new JMenuItem("tele1.uni-trier.de:33229, unicast");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					createOnlinePlayer(InetAddress
							.getByName("tele1.uni-trier.de"), 33229,
							Constants.Unicast);
				} catch (UnknownHostException e) {
				}
			}
		});
		submenu.add(menuItem);

		// MODMSG
		menu.addSeparator();
		menuItem = new JMenuItem("Messaging Client");
		menuItem
				.setToolTipText("send messages to teacher and participate in polls");
		menuItem.setIcon(Constants.getIcon("16x16/mail_new3.png"));
		menuItem.addActionListener(new ActionListener() {
			// @Override
			public void actionPerformed(ActionEvent e) {
				TTT.this.setVisible(false);
				TTTMessengerClient client = new TTTMessengerClient();
				if (client.isConnected()) {
					client.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent e) {
							TTT.this.setVisible(true);
						}
					});
				} else {
					client.setVisible(false);
					client.dispose();
					TTT.this.setVisible(true);
				}
			}
		});
		menu.add(menuItem);

		// ////////////////////////////////////////////////////////////////////////
		// Teacher Menu
		// ////////////////////////////////////////////////////////////////////////

		menu = new JMenu("Teacher");
		menu.setMnemonic(KeyEvent.VK_T);
		menuBar.add(menu);

		menuItem = new JMenuItem("Start Recording");
		menuItem.setToolTipText("start presenter with recording feature");
		menuItem.setIcon(Constants.getIcon("16x16/mix_record.png"));
		menuItem.setMnemonic(KeyEvent.VK_R);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// run outside of event dispatching thread
				new Thread(new Runnable() {
					public void run() {
						connect();
					}
				}).start();
			}
		});
		menu.add(menuItem);

		menuItem = new JMenuItem(" ");
		menuItem.setEnabled(false);
		menu.add(menuItem);

		menu.addSeparator();
		menu.addSeparator();

		menuItem = new JMenuItem("Options:");
		menuItem.setEnabled(false);
		menu.add(menuItem);

		menuItem = new JMenuItem("set recording folder");
		menuItem.setToolTipText("specify where to store recordings");
		menuItem.setIcon(Constants.getIcon("16x16/folder.png"));
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Choose recording folder");
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setCurrentDirectory(new File(userPrefs.get(
						"record_path", ".")));

				// ask user
				int returnVal = fileChooser.showDialog(getInstance(),
						"set recording folder");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						File file = fileChooser.getSelectedFile();
						// userPrefs.put("record_path",
						// file.getCanonicalFile().getParent());
						System.out.println("\nRecord path: "
								+ file.getCanonicalPath());
						userPrefs.put("record_path", file.getCanonicalPath());
					} catch (IOException e) {
						showMessage("Cannot open file: " + e);
					}
				}
			}
		});
		menu.add(menuItem);

		//
		// audio recording mode
		//
		submenu = new JMenu("audio recording mode");
		submenu.setMnemonic(KeyEvent.VK_A);
		// menu.add(submenu);
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				int audio_rec_mode = Integer.parseInt(event.getActionCommand());
				TTT.userPrefs.putInt("audio_rec_mode", audio_rec_mode);
			}
		};
		ButtonGroup buttonGroup = new ButtonGroup();

		menuItem = new JMenuItem("must be set BEFORE starting the presenter");
		menuItem.setEnabled(false);
		submenu.add(menuItem);

		// wav
		menuItem = new JRadioButtonMenuItem("linear audio (WAV)");
		menuItem.setActionCommand("" + AudioRecorder.WAV_RECORD_MODE);
		menuItem.addActionListener(actionListener);
		menuItem.setSelected(true);
		buttonGroup.add(menuItem);
		submenu.add(menuItem);

		//
		// Profiles submenu
		//
		submenu = new JMenu("lecture profiles");
		submenu.setMnemonic(KeyEvent.VK_L);
		submenu.setIcon(Constants.getIcon("16x16/bookmark_folder.png"));
		menu.add(submenu);

		menuItem = new JMenuItem("clear lecture profiles");
		menuItem.setToolTipText("delete all lecture profiles");
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (JOptionPane.YES_OPTION == JOptionPane
						.showInternalConfirmDialog(desktop,
								"Delete all Lecture Profiles?",
								"TTT: Lecture Profiles",
								JOptionPane.YES_NO_OPTION))
					LectureProfile.clearProfiles();
			}
		});
		submenu.add(menuItem);

		menuItem = new JMenuItem("export lecture profiles");
		menuItem.setToolTipText("writes XML document containing profiles");
		menuItem.setMnemonic(KeyEvent.VK_E);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setSelectedFile(new File("TTTProfiles.xml"));

				// file filter
				fileChooser
						.setFileFilter(new javax.swing.filechooser.FileFilter() {
							public boolean accept(File f) {
								String fname = f.getName().toLowerCase();
								return fname.endsWith(".xml")
										|| f.isDirectory();
							}

							public String getDescription() {
								return "XML Lecture Profiles";
							}
						});

				// ask user
				int returnVal = fileChooser.showSaveDialog(TTT.getInstance());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					LectureProfile
							.exportProfiles(fileChooser.getSelectedFile());
				}
			}
		});
		submenu.add(menuItem);

		menuItem = new JMenuItem("import lecture profiles");
		menuItem.setToolTipText("reads XML document containing profiles");
		menuItem.setMnemonic(KeyEvent.VK_I);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser fileChooser = new JFileChooser();

				File file = new File(fileChooser.getCurrentDirectory()
						.getAbsolutePath()
						+ File.separator + "TTTProfiles.xml");
				if (file.exists())
					fileChooser.setSelectedFile(file);

				// file filter
				fileChooser
						.setFileFilter(new javax.swing.filechooser.FileFilter() {
							public boolean accept(File f) {
								String fname = f.getName().toLowerCase();
								return fname.endsWith(".xml")
										|| f.isDirectory();
							}

							public String getDescription() {
								return "XML Lecture Profiles";
							}
						});

				// ask user
				int returnVal = fileChooser.showOpenDialog(TTT.getInstance());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					LectureProfile
							.importProfiles(fileChooser.getSelectedFile());
				}
			}
		});
		submenu.add(menuItem);

		// ////////////////////////////////////////////////////////////////////////
		// Post-Processing Menu
		// ////////////////////////////////////////////////////////////////////////

		menu = new JMenu("Post Processing");
		menu.setMnemonic(KeyEvent.VK_P);
		menuBar.add(menu);

		menuItem = new JMenuItem("open...");
		menuItem.setToolTipText("open ttt recording for post processing");
		menuItem.setIcon(Constants.getIcon("16x16/fileopen.png"));
		menuItem.setMnemonic(KeyEvent.VK_O);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
				ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// NOTE: post processing dialog is designed with NetBeans and
				// requires Swing Layout Extension Library
				// TODO: redesign without Swing Layout Extension Library
				if (!LibraryChecker.isSwingLayoutExtensionsLibraryInstalled())
					return;

				final File file = showFileDialog();
				if (file != null) {
					// get out of Event Dispatiching Thread
					new Thread(new Runnable() {
						public void run() {
							try {
								System.out
										.println("\nEntering post processing");
								// open recording
								boolean verbose = TTT.verbose;
								setVerbose(false);
								Recording recording = new Recording(file, false);
								userPrefs.put("last_opened_recording", file
										.getCanonicalPath());
								setVerbose(verbose);

								// post processing
								JInternalFrame frame = new JInternalFrame(
										"TTT: Post Processing");

								// internal frame
								frame.setContentPane(new PostProcessorPanel(
										recording));
								frame.pack();
								frame.setResizable(true);

								// post processing
								TTT ttt = getInstance();
								ttt.showTTT();
								ttt.addInternalFrameCentered(frame);
							} catch (Exception e) {
								e.printStackTrace();
								showMessage("Post proccesing failed.\nError: "
										+ e, "TTT: Post Processing",
										JOptionPane.ERROR_MESSAGE);

							}
						}
					}).start();
				}
			}
		});
		menu.add(menuItem);

		// additional menus

		menuItem = new JMenuItem(" ");
		menuItem.setEnabled(false);
		menu.add(menuItem);

		menuItem = new JMenuItem("debug xml searchbase file");
		menuItem.setToolTipText("open and test xml file");
		menuItem.setIcon(Constants.getIcon("16x16/db_update.png"));
		menuItem.setMnemonic(KeyEvent.VK_D);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final File file = showXMLFileDialog();
				if (file != null) {
					// get out of Event Dispatiching Thread
					new Thread(new Runnable() {
						public void run() {
							try {
								// Use an instance of ourselves as the SAX event
								// handler
								XMLHandler handler = new XMLHandler();
								handler.output = true;

								// Use the default (non-validating) parser
								SAXParserFactory factory = SAXParserFactory
										.newInstance();
								try {
									// Parse the input
									SAXParser saxParser = factory
											.newSAXParser();
									saxParser.parse(file, handler);
									showMessage("No errors detected in '"
											+ file + "'",
											"TTT: Testing XML Searchbase File",
											JOptionPane.INFORMATION_MESSAGE);

								} catch (Throwable t) {
									t.printStackTrace();
									showMessage(
											"XML file '"
													+ file
													+ "' contains errors.\nError: "
													+ t
													+ "\n\nSee console output for details.",
											"TTT: Testing XML Searchbase File",
											JOptionPane.ERROR_MESSAGE);
								}
							} catch (Exception e) {
							}
						}
					}).start();
				}
			}
		});
		menu.add(menuItem);

		menu.addSeparator();
		menu.addSeparator();

		menuItem = new JMenuItem("Options");
		menuItem.setEnabled(false);
		menu.add(menuItem);

		// advanced post processing options
		submenu = new JMenu("post processing options");
		submenu.setIcon(Constants.getIcon("16x16/configure.png"));
		submenu.setMnemonic(KeyEvent.VK_O);
		menu.add(submenu);

		menuItem = new JCheckBoxMenuItem(
				"generate additional flash movie without controls",
				TTT.userPrefs.getBoolean("write_flash_without_controls", false));
		menuItem
				.setToolTipText("useful if converting flash movie to other formats");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TTT.userPrefs.putBoolean("write_flash_without_controls",
						((JCheckBoxMenuItem) e.getSource()).isSelected());
			}
		});
		submenu.add(menuItem);

		// options for ssh/sftp transfer
		submenu = new JMenu("ssh options");
		submenu.setMnemonic(KeyEvent.VK_S);
		submenu.setIcon(Constants.getIcon("16x16/encrypted.png"));
		menu.add(submenu);

		menuItem = new JMenuItem("generate ssh keys");
		menuItem.setToolTipText("generate new ssh keys");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KeyGen.generateKeys();
			}
		});
		submenu.add(menuItem);

		menuItem = new JMenuItem("set privat ssh key file");
		menuItem
				.setToolTipText("specify the file which contains your private ssh key");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser
						.setDialogTitle("Choose your private key (ex. ~/.ssh/id_dsa)");
				chooser.setFileHidingEnabled(false);
				int returnVal = chooser.showOpenDialog(TTT.getInstance());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					TTT.userPrefs.put("ssh_private_key", chooser
							.getSelectedFile().getAbsolutePath());
					System.out
							.println("Private ssh key file set to '"
									+ chooser.getSelectedFile()
											.getAbsolutePath() + "'");
				}
			}
		});
		submenu.add(menuItem);

		menuItem = new JMenuItem("set known hosts file");
		menuItem
				.setToolTipText("specify the file which contains all known hosts/computers");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser
						.setDialogTitle("Choose known hosts file (ex. ~/.ssh/known_hosts)");
				chooser.setFileHidingEnabled(false);
				int returnVal = chooser.showOpenDialog(TTT.getInstance());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					TTT.userPrefs.put("ssh_known_hosts", chooser
							.getSelectedFile().getAbsolutePath());
					System.out
							.println("Known hosts file set to '"
									+ chooser.getSelectedFile()
											.getAbsolutePath() + "'");
				}
			}
		});
		submenu.add(menuItem);

		// menuItem = new JMenuItem("post processing wizard");
		// menuItem.setToolTipText("create thumbnail, full text search, HTML script, ...");
		// menuItem.setMnemonic(KeyEvent.VK_W);
		// menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
		// ActionEvent.CTRL_MASK));
		// menuItem.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent arg0) {
		// // get out of Event Dispatiching Thread
		// new Thread(new Runnable() {
		// public void run() {
		// // post processing
		// TTT ttt = getInstance();
		// ttt.showTTT();
		// ttt.addInternalFrameCentered(PostProcessor.createInternalPostProcessor());
		// }
		// }).start();
		// }
		// });
		// menu.add(menuItem);

		// ////////////////////////////////////////////////////////////////////////
		// Editor
		// ////////////////////////////////////////////////////////////////////////

		JMenu menu2 = new JMenu("Editor");

		menuBar.add(menu2);

		menuItem = new JMenuItem("Open Editor");
		// Open Editor
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (enabledNativeLookAndFeel) {
					try {
						UIManager.setLookAndFeel(UIManager
								.getCrossPlatformLookAndFeelClassName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				TTTEditor.OpenEditorAndShowFileDialog();

			}
		});
		menuItem.setToolTipText("run TTT Editor to edit recorded lectures");
		menuItem.setIcon(Constants.getIcon("16x16/configure.png"));
		menuItem.setMnemonic(KeyEvent.VK_F);
		menu2.add(menuItem);

		menuItem = new JMenuItem("Open Editor2 Beta");
		// Open Editor
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				addInternalFrame(Editor2.getInstance());
				Editor2.getInstance().show();
				try {
					Editor2.getInstance().setMaximum(true);
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				}
			}
		});

		menuItem.setToolTipText("run Editor to edit recorded lectures");
		menuItem.setIcon(Constants.getIcon("16x16/configure.png"));
		menuItem.setMnemonic(KeyEvent.VK_F);
		menu2.add(menuItem);

		// ////////////////////////////////////////////////////////////////////////
		// Extras
		// ////////////////////////////////////////////////////////////////////////

		menu = new JMenu("Extras");
		menu.setMnemonic(KeyEvent.VK_E);
		menuBar.add(menu);

		menuItem = new JMenuItem("reset all options");
		menuItem.setToolTipText("remove any user settings");
		menuItem.setIcon(Constants.getIcon("16x16/reload.png"));
		menuItem.setMnemonic(KeyEvent.VK_R);
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// ask user
				if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
						getInstance(),
						"Continue to delete and reset all user settings",
						"Reset all options", JOptionPane.OK_CANCEL_OPTION))
					try {
						// remove all keys
						String[] keys = userPrefs.keys();
						for (String key : keys)
							userPrefs.remove(key);
						userPrefs.flush();
						userPrefs.sync();
					} catch (Exception e) {
					}
			}
		});

		// MODMSG
		menu.addSeparator();
		menuItem = new JMenuItem("Messenger Server");
		menuItem
				.setToolTipText("starts messaging server (only avaible in teaching mode)");
		menuItem.setIcon(Constants.getIcon("16x16/mail_new3.png"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
				ActionEvent.CTRL_MASK));
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				startMessaging();
			}
		});

		final JMenuItem menuLItem = new JMenuItem("Activate Native Look");
		if (enabledNativeLookAndFeel) {
			menuLItem.setText("Deactivate Native Look");
		}

		menuLItem
				.setToolTipText("Activating 'Native Look' looks better but may cause instability on some systems. Also may require a restart to take full effect.");
		menu.add(menuLItem);
		menuLItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				enabledNativeLookAndFeel = !enabledNativeLookAndFeel;

				if (enabledNativeLookAndFeel) {
					activateNativeLook();
					menuLItem.setText("Deactivate Native Look");
				} else {
					activateCrossPlattformLook();
					menuLItem.setText("Activate Native Look");
				}

				userPrefs.putBoolean("enabledNativeLookAndFeel",
						enabledNativeLookAndFeel);
			}
		});

		menuBar.add(Box.createHorizontalGlue());

		menu = new JMenu("Help");
		menuItem.setMnemonic(KeyEvent.VK_H);
		menu.setIcon(Constants.getIcon("16x16/help.png"));
		menuBar.add(menu);

		menuItem = new JMenuItem("About TeleTeachingTool");
		menuItem.setMnemonic(KeyEvent.VK_A);
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showMessage(ABOUT, "About TeleTeachingTool",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		menuItem = new JMenuItem("License");
		menuItem.setMnemonic(KeyEvent.VK_L);
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showMessage(LICENSE,
						"TeleTeachingTool - GNU General Public License",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		return menuBar;
	}

	protected void activateCrossPlattformLook() {
		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
			this.paintAll(this.getGraphics());
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		} catch (InstantiationException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {

			e.printStackTrace();
		}

	}

	protected void activateNativeLook() {
		
		try {

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			this.paintAll(this.getGraphics());
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		} catch (InstantiationException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {

			e.printStackTrace();
		}
	}

	static File showXMLFileDialog() {
		// set last used file/directory
		// TODO: set path and filter only once (filechoosercreator)
		File lastRec = new File(userPrefs.get("last_opened_recording", ""));
		fileChooser.setCurrentDirectory(lastRec.getParentFile());

		// file filter
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String fname = f.getName().toLowerCase();
				return fname.endsWith(".xml") || f.isDirectory();
			}

			public String getDescription() {
				return "TeleTeachingTool Recordings";
			}
		});

		// ask user
		int returnVal = fileChooser.showOpenDialog(TTT.getInstance());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile();
		}
		return null;
	}

	public static File showFileDialog() {
		// set last used file/directory
		// TODO: set path and filter only once (filechoosercreator)
		File lastRec = new File(userPrefs.get("last_opened_recording", ""));
		if (lastRec.exists())
			fileChooser.setSelectedFile(lastRec);
		else
			fileChooser.setCurrentDirectory(lastRec.getParentFile());

		// file filter
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String fname = f.getName().toLowerCase();
				return fname.endsWith(".ttt") || f.isDirectory();
			}

			public String getDescription() {
				return "TeleTeachingTool Recordings";
			}
		});

		// ask user
		int returnVal = fileChooser.showOpenDialog(TTT.getInstance());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile();
		}
		return null;
	}

	private void connect() {
		LectureProfileDialog.showLectureProfileDialog();
	}

	private void connectTTT() {
		final JInternalFrame frame = new JInternalFrame("TTT: Connect Dialog");

		// VNC Server input fields for host and port
		final JTextField hostField = new JTextField(userPrefs.get(
				"last_used_host", ""), 12);
		final NumberField portField = new NumberField(5);
		try {
			portField.setText(""
					+ Integer.parseInt(userPrefs.get("last_used_port", "")));
		} catch (NumberFormatException e) {
		}

		JPanel vncServerPanel = new JPanel();
		vncServerPanel.setOpaque(false);
		vncServerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		vncServerPanel.add(new JLabel("Host:"));
		vncServerPanel.add(hostField);
		vncServerPanel.add(new JLabel("Port:"));
		vncServerPanel.add(portField);

		// recorder
		final JCheckBox multicastCheckBox = new JCheckBox("enable multicast");
		multicastCheckBox.setOpaque(false);
		multicastCheckBox.setSelected(userPrefs.getBoolean("multicast_client",
				true));

		// packing together
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(new JLabel("Connect to VNC Server:"));
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(indentation(vncServerPanel));
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(indentation(multicastCheckBox));

		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					// get user input values
					final int port = portField.getNumber() >= 100 ? portField
							.getNumber() : (portField.getNumber() + 5900);

					final InetAddress hostAddress = InetAddress
							.getByName(hostField.getText());

					final boolean multicast = multicastCheckBox.isSelected();
					userPrefs.putBoolean("multicast_client", multicast);
					// remember values
					userPrefs.put("last_used_host", hostAddress.getHostName());
					userPrefs.put("last_used_port", "" + port);

					// close dialog
					frame.dispose();

					// start player
					createOnlinePlayer(hostAddress, port,
							multicast ? Constants.Multicast : Constants.Unicast);

				} catch (Exception e) {
					// close dialog
					frame.dispose();

					showMessage("Cannot open connection:\n" + "    Error: " + e);
				}
			}
		};

		hostField.addActionListener(actionListener);
		portField.addActionListener(actionListener);

		// buttons
		JButton[] buttons = new JButton[2];

		buttons[0] = new JButton("ok");
		buttons[0].addActionListener(actionListener);

		buttons[1] = new JButton("cancel");
		buttons[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// close dialog
				frame.dispose();
			}
		});

		// show dialog
		frame.setContentPane(createTab(panel, buttons));
		frame.pack();
		frame.setVisible(true);
		ttt.addInternalFrameCentered(frame);
	}

	static JComponent createTab(JComponent component, JButton[] buttons) {
		JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 40));
		mainPanel.setOpaque(false);
		mainPanel.add(component);

		JPanel buttonPanel = new JPanel(
				new FlowLayout(FlowLayout.RIGHT, 10, 10));
		buttonPanel.setOpaque(false);

		for (int i = 0; i < buttons.length; i++)
			buttonPanel.add(buttons[i]);

		JPanel panel = new GradientPanel();
		panel.setLayout(new BorderLayout());
		panel.add(mainPanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);

		return panel;
	}

	static Component indentation(Component component) {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(Box.createRigidArea(new Dimension(30, 0)));
		panel.add(component);
		return panel;
	}

	public static void showMessage(final Object message) {
		showMessage(message, "TTT", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showMessage(final Object message, final String title,
			final int messageType) {
		// console output
		if (message instanceof String) {
			System.out.println();
			System.out.println(message);
			System.out.println();
		}

		// main frame not visible
		if (ttt == null || !ttt.isVisible())
			JOptionPane.showMessageDialog(null, message, title, messageType);

		// bug in SUN Java 1.5 (Windows and Linux)
		// see Bug ID 6178755: REGRESSION: JOptionPane's showInternal*Dialog
		// methods never return
		// workaround: InternalDialog used by open must be invoked in event
		// dispatching thread
		else {
			// popup is not visible in fullscreen mode
			// hence return to wiondowed mode first
			leaveFullscreen();

			if (SwingUtilities.isEventDispatchThread())
				JOptionPane.showInternalMessageDialog(getInstance().desktop,
						message, title, messageType);
			else
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							JOptionPane.showInternalMessageDialog(
									getInstance().desktop, message, title,
									messageType);
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
		}
	}

	// version informations
	// mixed format with blanks and tabs for shell and dialog output
	static final String ABOUT = "TeleTeachingTool   -   Version " + TTT.version
			+ "\n\n" + "   Author:  \tPeter Ziewer\n"
			+ "                Technische Universität München\n"
			+ "                Germany\n" + "   Email:    \tziewer@in.tum.de\n"
			+ "   Web:      \thttp://ttt.in.tum.de\n"
			+ "   Web:      \thttp://ttt.uni-trier.de\n\n"
			+ "   TTT Protocol Version " + Constants.VersionMessageTTT
			+ "   RFB Protocol Version " + Constants.VersionMessageRFB
			+ "   Java Version " + System.getProperty("java.version") + " ("
			+ System.getProperty("java.vm.version") + ")\n"
			+ "   Java Vendor: " + System.getProperty("java.vendor") + "\n"
			+ "   JMF Version  " + getJMFVersion() + "\n"
			+ "   Operating System: " + System.getProperty("os.name") + " ("
			+ System.getProperty("os.version") + ")\n" + "\n\n"
			+ "   This software may be redistributed under the terms\n"
			+ "   of the GNU General Public License (version 3 or later)\n"
			+ "   see <http://www.gnu.org/licenses/>\n";

	static final String LICENSE = "TeleTeachingTool - Presentation Recording With Automated Indexing\n"
			+ "\n"
			+ "Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München\n"
			+ "\n"
			+ "This file is part of TeleTeachingTool.\n"
			+ "TeleTeachingTool is free software: you can redistribute it and/or modify\n"
			+ "it under the terms of the GNU General Public License as published by\n"
			+ "the Free Software Foundation, either version 3 of the License, or\n"
			+ "(at your option) any later version.\n"
			+ "\n"
			+ "TeleTeachingTool is distributed in the hope that it will be useful,\n"
			+ "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
			+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
			+ "GNU General Public License for more details.\n"
			+ "\n"
			+ "You should have received a copy of the GNU General Public License\n"
			+ "along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.\n";

	@SuppressWarnings("unchecked")
	public static String getJMFVersion() {
		try {
			Class c = Class.forName("javax.media.Manager");
			Method m = c.getMethod("getVersion");
			return (String) m.invoke(null, (Object[]) null);
		} catch (NoClassDefFoundError e) {
			return "NOT INSTALLED";
		} catch (ClassNotFoundException e) {
			return "NOT INSTALLED";
		} catch (SecurityException e) {

		} catch (NoSuchMethodException e) {

		} catch (IllegalArgumentException e) {

		} catch (IllegalAccessException e) {

		} catch (InvocationTargetException e) {

		}
		return "NOT INSTALLED";
	}

	public void showTTT() {
		// show and maximize frame
		if (!isVisible()) {
			// set size (if user de-maximizes)
			int width = 1024;
			int height = 768;

			try {
				// set size to screen size (if possible)
				Dimension screenSize = Toolkit.getDefaultToolkit()
						.getScreenSize();
				if (0 < screenSize.width)
					width = screenSize.width;
				if (0 < screenSize.height)
					height = screenSize.height;
			} catch (Exception e) {
			}

			try {
				// set size to size of first display (if possible)
				// NOTE: useful for multi display settings
				GraphicsEnvironment ge = GraphicsEnvironment
						.getLocalGraphicsEnvironment();
				GraphicsDevice[] gs = ge.getScreenDevices();

				DisplayMode dm = gs[0].getDisplayMode();

				if (0 < dm.getWidth() && dm.getWidth() < width)
					width = dm.getWidth();
				if (0 < dm.getHeight() && dm.getHeight() < height)
					height = dm.getHeight();
			} catch (Exception e) {
			}

			setSize(width, height);
			setExtendedState(MAXIMIZED_BOTH);

			setIconImage(Constants.getIcon("ttt16.png").getImage());
			setVisible(true);
			getContentPane().setBackground(Color.WHITE);

		}
	}



	public static TTT getInstance() {
		if (ttt == null) {
			ttt = new TTT();
		}
		return ttt;
	}

	public static Component getRootComponent() {
		if (ttt == null || !ttt.isVisible())
			return null;
		else
			return getInstance();
	}

	// ///////////////////////////////////////////////////////
	// internal frames
	// ///////////////////////////////////////////////////////

	// TODO: position management
	private int position;

	public void addInternalFrameCentered(JInternalFrame frame) {
		frame.pack();

		frame.setFrameIcon(Constants.getIcon("ttt16.png"));

		frame.setVisible(true);
		Rectangle rect = TTT.getInstance().desktop.getBounds();
		TTT.getInstance().addInternalFrame(frame,
				rect.width / 2 - frame.getWidth() / 2,
				rect.height / 2 - frame.getHeight() / 2, 10);
		// TODO: think about layering
		ttt.desktop.setLayer(frame, 10);
		frame.toFront();
	}

	void addInternalFrame(JInternalFrame internalFrame, int x, int y, int layer) {
		ttt.desktop.setLayer(internalFrame, layer);
		addInternalFrame(internalFrame, x, y);
	}

	private void addInternalFrame(JInternalFrame internalFrame, int x, int y) {
		internalFrame.setLocation(x, y);

		// reduce size if too huge
		int width = internalFrame.getWidth();
		int height = internalFrame.getHeight();
		if (x + internalFrame.getWidth() > ttt.desktop.getWidth())
			width = ttt.desktop.getWidth() - x;
		if (y + internalFrame.getHeight() > ttt.desktop.getHeight())
			height = ttt.desktop.getHeight() - y;
		if (width != internalFrame.getWidth()
				|| height != internalFrame.getHeight())
			internalFrame.setSize(width, height);
		internalFrame.setFrameIcon(Constants.getIcon("ttt16.png"));
		ttt.desktop.add(internalFrame);
		try {
			internalFrame.setSelected(true);
		} catch (PropertyVetoException e) {
		}
	}

	// TODO: visibility
	void addInternalFrame(JInternalFrame internalFrame) {
		addInternalFrame(internalFrame, position, 0);
	}
	

	public static void leaveFullscreen() {
		fullscreen(null);
	}

	public static void fullscreen(Container panel) {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();

		if (panel == null) {
			// Exit full-screen mode
			Window window = gs.getFullScreenWindow();
			if (window != null)
				window.dispose();

			gs.setFullScreenWindow(null);
		}

		else {
			// Create a window for full-screen mode; add a button to leave
			// full-screen mode
			JFrame frame = new JFrame(gs.getDefaultConfiguration());
			frame.setUndecorated(true);
			frame.add(panel);

			// Enter full-screen mode
			gs.setFullScreenWindow(frame);
			// win.validate();
		}
	}

	public Component getGlassPane() {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		Window window = gs.getFullScreenWindow();
		if (window != null && window instanceof JFrame)
			return ((JFrame) window).getGlassPane();
		else
			return super.getGlassPane();
	}

	// ///////////////////////////////////////////////////////
	// Creators
	// ///////////////////////////////////////////////////////
	public static Player presenter;

	public static void createPresenter(LectureProfile lectureProfile) {
		try {
			TTT ttt = getInstance();
			ttt.showTTT();

			presenter = new Player(lectureProfile);
			// MOD TD
			ttt.runningPlayer = presenter;

			if (!(lectureProfile.isRecordEnabled() && lectureProfile
					.isLoopbackRecorder())) {
				// show presenter/recorder in internal frame
				ttt.addInternalFrame(presenter);
			} else {
				// show recorder controls only

				// make TTT windows invisible
				TTT.getInstance().setVisible(false);

				// create simple window with recorder controls
				final JWindow window = new JWindow();
				window.setAlwaysOnTop(true);
				window.getContentPane().add(presenter.getContentPane());

				// make recorder controls draggable
				window.getContentPane().addMouseMotionListener(
						new MouseMotionListener() {
							private Point position_offset;
							private boolean dragging = false;

							public void mouseMoved(MouseEvent arg0) {
								dragging = false;
							}

							public void mouseDragged(MouseEvent event) {
								if (!dragging) {
									// start dragging
									position_offset = event.getPoint();
									dragging = true;
								} else {
									// drag - set new window position
									Point point = event.getPoint();
									point.translate(-position_offset.x,
											-position_offset.y);
									Point location = window.getLocation();
									location.translate(point.x, point.y);

									// check minimal/maximal positions
									if (location.x < 0)
										location.x = 0;
									if (location.y < 0)
										location.y = 0;
									Dimension resolution = Toolkit
											.getDefaultToolkit()
											.getScreenSize();
									if (location.x + window.getWidth() > resolution.width)
										location.x = resolution.width
												- window.getWidth();
									if (location.y + window.getHeight() > resolution.height)
										location.y = resolution.height
												- window.getHeight();

									window.setLocation(location);
								}
							}
						});

				window.pack();
				window.setVisible(true);

				if (lectureProfile.isShowRecordControlsInfo()) {
					// display information about recording controls
					JPanel infoPanel = new JPanel();
					infoPanel.setLayout(new BoxLayout(infoPanel,
							BoxLayout.Y_AXIS));
					infoPanel.setOpaque(false);
					infoPanel.add(new JLabel("TTT Record Controls:"));
					infoPanel.add(new JLabel(" "));
					infoPanel.add(new JLabel(
							"The controls are placed in the top left corner"));
					infoPanel
							.add(new JLabel(
									"but can be dragged to any other position on screen."));
					infoPanel.add(new JLabel(" "));
					infoPanel.add(new JLabel("record", Constants
							.getIcon("Record24.png"), JLabel.LEFT));
					infoPanel.add(new JLabel("stop recording", Constants
							.getIcon("Stop24.gif"), JLabel.LEFT));
					infoPanel.add(new JLabel("display TTT main window",
							Constants.getIcon("ZoomIn24.gif"), JLabel.LEFT));
					infoPanel.add(new JLabel("turn off TTT main window",
							Constants.getIcon("ZoomOut24.gif"), JLabel.LEFT));

					infoPanel.add(new JLabel(" "));
					JCheckBox showAgainCheckBox = new JCheckBox(
							"Don't show this message again.");
					infoPanel.add(showAgainCheckBox);

					JOptionPane.showMessageDialog(window, infoPanel,
							"TTT Recorder", JOptionPane.INFORMATION_MESSAGE);

					if (showAgainCheckBox.isSelected()) {
						lectureProfile.setShowRecordControlsInfo(false);
						lectureProfile.storeProfile();
					}
				}
			}
		} catch (Exception e) {
			showMessage("Recorder failed:\n" + "    Server:\t"
					+ lectureProfile.getHost() + " Port: "
					+ lectureProfile.getPort() + "\n" + "    Error:\t" + e);
			e.printStackTrace();
		}
	}

	public static void createFilePlayer(File file) {
		String fileName = null;
		try {
			fileName = file.getCanonicalPath();
			userPrefs.put("last_opened_recording", fileName);
			TTT ttt = getInstance();
			ttt.showTTT();
			ttt.addInternalFrame(new Player(fileName));
		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Player failed:\n"
					+ (fileName == null ? "" : "    File:\t" + fileName + "\n")
					+ "    Error:\t" + e);
		}
	}

	public static void createOnlinePlayer(InetAddress hostAddress, int port,
			int accessMode) {
		try {
			// create connection and player
			TTTConnection connection = new TTTConnection(hostAddress, port,
					accessMode);

			Player player = new Player(connection);

			// show player
			TTT ttt = getInstance();
			ttt.showTTT();
			ttt.addInternalFrame(player);

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Player failed:\n" + "Server: " + hostAddress
					+ " Port: " + port + "\n" + "    Error:\t" + e);
		}
	}	

	public static void main(String[] args) throws NumberFormatException,
			IOException {

		// parse command line options
		for (int i = 0; i < args.length; ++i) {
			if (args[i].length() == 2 && args[i].charAt(0) == '-') {
				switch (args[i].charAt(1)) {
				case 'n':
					enabledNativeLookAndFeel = false;
					break;
				default:
					System.out.println("Usage: ttt [options] [recording]\n"
							+ (char) 9 + "-n" + (char) 9
							+ "Disable native look and feel\n" + (char) 9
							+ "-h" + (char) 9 + "Show this message");
					System.exit(0);
				}
			} else {
				createFilePlayer(new File(args[i]));
				break;
			}
		}
		getInstance().showTTT();
	}

	// MOD TD
	/**
	 * stores Player instance for messaging
	 */
	private Player runningPlayer = null;
	private JInternalFrame ipFrame = null;

	private TTTMessaging messaging = null;

	public void startMessaging() {
		// check if Player and RfbProtocol is available
		RfbProtocol prot = null;
		if (runningPlayer != null) {
			prot = runningPlayer.getRfbProtocol();
		}
		if (prot == null) {
			JOptionPane
					.showMessageDialog(ttt,
							"Messaging can only be started, if a teaching session is active!");
		} else {
			java.net.InetAddress ip = null;
			boolean startWebMessaging = false;
			int result = 0;
			while (ip == null && !(result == JOptionPane.CANCEL_OPTION)) {
				NetworkInterfaceDialog dlgNI = new NetworkInterfaceDialog();
				result = JOptionPane.showConfirmDialog(TTT.this, dlgNI,
						"select messaging network interface",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				ip = dlgNI.getSelectedIP();
				// WEB startWebMessaging = dlgNI.startWebMessaging();
			}

			if (result == JOptionPane.OK_OPTION) {
				messaging = new TTTMessaging(prot, ip, startWebMessaging);
				messaging.setLocation(Math.max(0, ttt.desktop.getWidth()
						- messaging.getWidth()), 100);
				ttt.desktop.add(messaging);

				ipFrame = new JInternalFrame("Messaging IP Address");
				JLabel lblIP = new JLabel(ip.toString().substring(1));
				lblIP.setFont(((Font) UIManager.get("Label.font"))
						.deriveFont(16f));
				ipFrame.add(lblIP);
				ipFrame.pack();
				ipFrame.setLocation(800, 200);
				ipFrame.setVisible(true);

				messaging.addInternalFrameListener(new InternalFrameAdapter() {
					public void internalFrameClosed(InternalFrameEvent arg0) {
						ipFrame.dispose();
						ipFrame = null;
					}
				});

				ttt.desktop.add(ipFrame, new Integer(1000));
			}
		}
	}

	public static boolean isEnabledNativeLookAndFeel() {
		return enabledNativeLookAndFeel;
	}
}
