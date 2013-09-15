/*
 * Copyright 2009 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.kappich.pat.gnd.
 * 
 * de.kappich.pat.gnd is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.gnd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.gnd; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.pat.gnd.gnd;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.colorManagement.ColorDialog;
import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.coorTransform.GeoTransformation;
import de.kappich.pat.gnd.displayObjectToolkit.*;
import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.gnd.MapPane.MapScaleListener;
import de.kappich.pat.gnd.layerManagement.LayerManager;
import de.kappich.pat.gnd.layerManagement.LayerManagerDialog;
import de.kappich.pat.gnd.notice.NoticeManager;
import de.kappich.pat.gnd.viewManagement.*;

import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.prefs.*;


/**
 * Ein GenericNetDisplay-Objekt ist ein Top-Level-Fenster, das eine Menüzeile besitzt und die Kartendarstellung zeigt. Diese Klasse wird wahlweise aus RunGND
 * heraus instanziiert (Stand-Alone-Anwendung) oder aus dem GNDPlugin (Plugin in einer anderen Anwendung, z.B. dem GTM).
 * <p/>
 * Die Initialisierung eines Objekts erfolgt mit Hilfe einer {@link View Ansicht}, die eine geordnete Reihenfolge von Layern enthält. Diese Layer werden in der
 * Reihenfolge von oben nach unten in der Legende angeben und in der Kartenansicht gezeichnet. Um die Konfigurationsdaten und eventuell dynamische Daten
 * erhalten zu können, bekommt ein GenericNetDisplay-Objekt eine Datenverteilerverbindung übergeben. Eine Liste von Systemobjekten beeinflußt den
 * Kartenausschnitt, mit dem die Kartenansicht anfänglich gezeigt wird: ist die Liste leer, so wird die ganze Karte gezeichnet, andernfalls wird aus den
 * Koordinaten der übergebenen Systemobjekte ein diese Objekte umfassendes Rechteck berechnet und angezeigt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9358 $
 */
@SuppressWarnings("serial")
public class GenericNetDisplay extends JFrame {

	private View _view = null;

	private ClientDavInterface _connection = null;

	private List<SystemObject> _systemObjects;

	private boolean _standAlone = false;

	private JSplitPane _splitPane = null;

	private LegendPane _legendPane = null;

	private MapPane _mapPane = null;

	private Icon _logo = null;

	private ViewManagerDialog _viewManagerDialog = null;

	private LayerManagerDialog _layerManagerDialog = null;

	private DOTManagerDialog _dotManagerDialog = null;

	private boolean _isDoubleBuffered;

	private boolean _isAntiAliasingOn;

	private boolean _isMapsTooltipOn;

	private String _startViewName = null;

	private Double _screenResolution = null;

	private static String RESOLUTION = "Resolution";

	private static String STARTVIEWNAME = "StartViewName";

	final private List<ResolutionListener> _resolutionListeners = new ArrayList<ResolutionListener>();

	final private List<SelectionListener> _selectionListeners = new ArrayList<SelectionListener>();

	final private static Debug _debug = Debug.getLogger();

	public JList _objectList;

	private JTextArea _noticeTextArea;

	private NoticeManager _noticeManager = null;

	private DynamicListModel _selectedObjects;

	private boolean _systemEdit;

	/** Instanz dieser Klasse. */
	private static GenericNetDisplay _instance;

	/**
	 * Konstruiert ein Objekt aus den übergebenen Informationen. Der Stand-Alone-Wert gibt der Netzdarstellung die Information, ob sie sich als eigenständige
	 * Anwendung betrachten kann oder nicht. Im Falle des Schließens des Fensters wird sie im Stand-Alone-Fall komplett beendet.
	 *
	 * @param view          eine Ansicht
	 * @param connection    eine Datenverteiler-Verbindung
	 * @param systemObjects eine Liste mit Systemobjekten
	 * @param standAlone    <code>true</code> genau dann, wenn sie sich als eigenständige Anwendung betrachten soll
	 */
	public GenericNetDisplay(
			final View view, final ClientDavInterface connection, final List<SystemObject> systemObjects, final boolean standAlone) {

		super();

		_standAlone = standAlone;
		_connection = connection;
		_view = view;
		_systemObjects = systemObjects;

		_noticeManager = new NoticeManager(getPreferenceStartPath().node("notices"), _connection.getDataModel());

		long t0 = System.currentTimeMillis();
		readPreferences();

		final String property = System.getProperty("de.kappich.pat.gnd.ZentralMeridian");
		if(property == null) {
			GeoTransformation.setCentralMeridianFromOutside(9.0);
		}
		else {
			final double centralMeriadian = Double.parseDouble(property);
			if((centralMeriadian < -9.) && (centralMeriadian > 27.)) {
				GeoTransformation.setCentralMeridianFromOutside(9.0);
			}
			else {
				GeoTransformation.setCentralMeridianFromOutside(centralMeriadian);
			}
		}

		if(_standAlone && (_startViewName != null) && ViewManager.getInstance().hasView(_startViewName)) {
			_view = ViewManager.getInstance().getView(_startViewName);
		}

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if((screenSize.getWidth() >= 1000) && (screenSize.getHeight() >= 780)) {
			setPreferredSize(new Dimension(800, 700));
			setLocation(50, 50);
		}
		else {
			setPreferredSize(new Dimension((int)screenSize.getWidth() - 50, (int)screenSize.getHeight() - 50));
			setLocation(0, 0);
		}
		if(_standAlone) {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		_isDoubleBuffered = true;
		_isAntiAliasingOn = true;
		_isMapsTooltipOn = false;

		createMenu();

		try {
			_logo = new ImageIcon(HelpPage.class.getResource("kappich-logo.png"));
		}
		catch(Exception ex) {
			_logo = null;
			_debug.info("Generische Netzdarstellung: das Kappich-Logo konnte nicht geladen werden.");
		}

		setSplitPaneFromView(_view);

		addShutDownHook();

		long dt = System.currentTimeMillis() - t0;
		System.out.println("dt = " + dt);
		_instance = this;
	}

	/**
	 * Gibt die Instanz der Klasse zurück.
	 *
	 * @return Instanz der Klasse.
	 */
	public static GenericNetDisplay getInstance() {

		return _instance;
	}

	/**
	 * Gibt <code>true</code> zurück, wenn das GenericNetDisplay-Objekt sich als eigenständige Anwendung betrachtet.
	 *
	 * @return gibt <code>true</code> zurück, wenn das sich this als eigenständige Anwendung betrachtet
	 */
	public boolean isStandAlone() {

		return _standAlone;
	}

	/**
	 * Gibt die aktuelle Ansicht zurück.
	 *
	 * @return aktuelle Ansicht
	 */
	public View getView() {

		return _view;
	}

	private void addShutDownHook() {

		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(
				new Thread() {

					@Override
					public void run() {

						_connection.disconnect(false, "Ende.");
					}
				}
		);
	}

	/**
	 * Diese Methode zeigt die übergebene Ansicht in der Kartendarstellung des GenericNetDisplay-Objekts, d.h. in seiner Legende und der Kartenansicht.
	 *
	 * @param view die neue Ansicht
	 */
	public void setSplitPaneFromView(View view) {

		if(_splitPane != null) {
			final Component[] components = _splitPane.getComponents();
			for(Component component : components) {
				if(component instanceof MapPane) {
					final MapPane oldMapPane = (MapPane)component;
					oldMapPane.clearEverything();
				}
			}
			remove(_splitPane);
		}
		_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		_splitPane.setOneTouchExpandable(true);
		_splitPane.setDividerLocation(250);
//		_splitPane.setDividerSize(10);
		_splitPane.setBackground(Color.WHITE);

		_view = view;
		JPanel scaleAndLegendPanel = new JPanel();
		scaleAndLegendPanel.setLayout(new BorderLayout());

		class ScaleTextField extends JTextField implements MapPane.MapScaleListener {

			public ScaleTextField() {

				setEditable(false);
			}

			public void mapScaleChanged(double scale) {

				setText("1:" + (int)scale);
			}
		}

		if(_logo != null) {
			JLabel logoLabel = new JLabel(_logo);
			logoLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			JPanel borderPanel = new JPanel(new BorderLayout());
			borderPanel.add(logoLabel, BorderLayout.CENTER);
			borderPanel.setBackground(Color.WHITE);
			JLabel captionLabel = new JLabel("Legende:");
			captionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			captionLabel.setBackground(Color.WHITE);
			borderPanel.add(captionLabel, BorderLayout.SOUTH);
			scaleAndLegendPanel.add(borderPanel, BorderLayout.NORTH);
		}
		else {
			JLabel captionLabel = new JLabel("Legende:");
			captionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			captionLabel.setBackground(Color.WHITE);
			scaleAndLegendPanel.add(captionLabel, BorderLayout.NORTH);
		}

		// Kartenansicht zurerst
		_mapPane = new MapPane(this, _view);
		_mapPane.setTooltip(_isMapsTooltipOn);
		_splitPane.setRightComponent(_mapPane);

		// Legende erst jetzt, weil sie den Maßstab benötigt
		ScaleTextField scaleTextField = new ScaleTextField();
		scaleTextField.setBorder(new TitledBorder("Maßstab"));
		scaleAndLegendPanel.add(scaleTextField, BorderLayout.SOUTH);
		_legendPane = new LegendPane(_view);
		scaleAndLegendPanel.add(makeLeftPanel(), BorderLayout.CENTER);
		_splitPane.setLeftComponent(scaleAndLegendPanel);

		add(_splitPane);
		pack();
		setVisible(false);	// Sonst kann man beim Wechseln der Ansicht ein repaint auslösen ...
		_mapPane.init();
		_legendPane.init(_mapPane.getMapScale());

		if(_logo == null) {
			setTitle("Kappich Systemberatung - Generische Netzdarstellung - " + view.getName());
		}
		else {
			setTitle("Generische Netzdarstellung - " + view.getName());
		}
		_splitPane.revalidate();

		_legendPane.setMapScale(_mapPane.getMapScale());
		Collection<MapScaleListener> mapScaleListeners = new ArrayList<MapScaleListener>();
		mapScaleListeners.add(scaleTextField);
		mapScaleListeners.add(_legendPane);
		_mapPane.addMapScaleListeners(mapScaleListeners);

		scaleTextField.setText("1:" + _mapPane.getMapScale().intValue());

		setVisible(true);

		ViewDialog.closeAll();
	}

	private JComponent makeLeftPanel() {

		_objectList = new JList();
		_selectedObjects = new DynamicListModel();
		_objectList.setModel(_selectedObjects);

		/**
		 *  Hack: Eigentlich sollte man den {@link ListSelectionListener} benutzen, aber der reagiert auch, wenn durch Zoomen o.ä. der dargestellte Inhalt geändert wird.
		 *  Daher wird hier manuell bei Tastatur oder Mauseingaben das ausgewählte Element geändert
		 */
		ObjectListListener l = new ObjectListListener();
		_objectList.addMouseListener(l);
		_objectList.addKeyListener(l);
		_objectList.addMouseListener(
				new MouseAdapter() {

					@Override
					public void mouseClicked(final MouseEvent e) {

						final int selectedIndex = _objectList.getSelectedIndex();
						if(selectedIndex != -1) {
							if(e.getClickCount() == 2) {
								_mapPane.focusOnObject(_selectedObjects.getObject(selectedIndex));
							}
						}
					}
				}
		);
		_objectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_noticeTextArea = new JTextArea();
		_noticeTextArea.getDocument().addDocumentListener(
				new DocumentListener() {

					public void insertUpdate(final DocumentEvent e) {

						saveNotice();
					}

					public void removeUpdate(final DocumentEvent e) {

						saveNotice();
					}

					public void changedUpdate(final DocumentEvent e) {

						saveNotice();
					}

					private void saveNotice() {

						final String text = _noticeTextArea.getText();
						if(_noticeTextArea.isEditable() && !_systemEdit) {
							for(final SystemObject object : _mapPane.getSelectedSystemObjects()) {
								_noticeManager.setNotice(object, text);
							}
						}
						SwingUtilities.invokeLater(
								new Runnable() {

									public void run() {

										_mapPane.redraw();
									}
								}
						);
					}
				}
		);
		_noticeTextArea.setWrapStyleWord(true);
		_noticeTextArea.setLineWrap(true);
		selectionChanged();
		JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(_objectList), new JScrollPane(_noticeTextArea));
		jSplitPane.setOneTouchExpandable(true);
		jSplitPane.setResizeWeight(0.5);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(_legendPane), jSplitPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.5);
		return splitPane;
	}

	/**
	 * Gibt die Datenverteilerverbindung zurück.
	 *
	 * @return die Datenverteilerverbindung
	 */
	public ClientDavInterface getConnection() {

		return _connection;
	}

	private void createMenu() {

		JMenuBar menuBar = new JMenuBar();
		// Datei-Menue
		JMenu fileMenu = new JMenu("Datei");
		fileMenu.setMnemonic(KeyEvent.VK_D);
		fileMenu.getAccessibleContext().setAccessibleDescription("Das Datei-Menue");

		JMenuItem menuItem;

		menuItem = new JMenuItem("Speichern");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Speichert die Kartendarstellung.");
		fileMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						try {
							saveGrafic();
						}
						catch(IOException e2) {
							JOptionPane.showMessageDialog(
									null, "Fehler beim Speichern", "Fehlermeldung", JOptionPane.ERROR_MESSAGE
							);
						}
					}
				}
		);

		menuItem = new JMenuItem("Drucken");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Druckt die Kartendarstellung.");
		fileMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						printNetDisplay();
					}
				}
		);

		fileMenu.addSeparator();

		if(_standAlone) {
			menuItem = new JMenuItem("Beenden");
			menuItem.getAccessibleContext().setAccessibleDescription("Beendet das Programm.");
		}
		else {
			menuItem = new JMenuItem("GND schließen");
			menuItem.getAccessibleContext().setAccessibleDescription("Schließt nur dieses GND-Fenster");
		}
		fileMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						if(_standAlone) {
							System.exit(0);
						}
						else {
							dispose();
						}
					}
				}
		);

		// Ansicht-Menue
		JMenu viewMenu = new JMenu("Ansichten");
		viewMenu.setMnemonic(KeyEvent.VK_A);
		viewMenu.getAccessibleContext().setAccessibleDescription("Das Ansicht-Menue");

		menuItem = new JMenuItem("Ansichtsverwaltung");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Startet den Ansichtsverwaltungs-Dialog.");
		viewMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						if(_viewManagerDialog == null) {
							_viewManagerDialog = new ViewManagerDialog(GenericNetDisplay.this, false);
						}
						_viewManagerDialog.showDialog();
					}
				}
		);

		viewMenu.add(new JSeparator());

		menuItem = new JMenuItem("Aktuelle Ansicht");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Startet den Ansicht-Verwalten-Dialog.");
		viewMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						final String title = "GND: aktuelle Ansicht bearbeiten";
						ViewDialog.runDialog(
								ViewManager.getInstance(), _view, true, ViewManager.getInstance().isChangeable(_view), false, title
						);
					}
				}
		);

		// Layer-Menue
		JMenu layerMenu = new JMenu("Layer");
		layerMenu.setMnemonic(KeyEvent.VK_L);
		layerMenu.getAccessibleContext().setAccessibleDescription("Das Ansicht-Menue");

		menuItem = new JMenuItem("Layer-Verwaltung");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Startet den Layer-Verwalten-Dialog.");
		layerMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						if(_layerManagerDialog == null) {
							_layerManagerDialog = new LayerManagerDialog(_connection);
						}
						else {
							_layerManagerDialog.showDialog();
						}
					}
				}
		);

		// DOT-Menue
		JMenu dotMenu = new JMenu("Darstellungstypen");
		dotMenu.setMnemonic(KeyEvent.VK_T);
		dotMenu.getAccessibleContext().setAccessibleDescription("Das Darstellungstypen-Menue");

		menuItem = new JMenuItem("Darstellungstypenverwaltung");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Startet den Darstellungstypen-Verwalten-Dialog.");
		dotMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						if(_dotManagerDialog == null) {
							_dotManagerDialog = new DOTManagerDialog(DOTManager.getInstance(), _connection);
						}
						else {
							_dotManagerDialog.showDialog();
						}
					}
				}
		);

		// Extras-Menue
		JMenu noticeMenu = new JMenu("Notizen");
		noticeMenu.setMnemonic(KeyEvent.VK_N);
		noticeMenu.getAccessibleContext().setAccessibleDescription("Das Notizen-Menue");

		menuItem = new JMenuItem("Notizen exportieren");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Exportiert alle Notizen.");
		menuItem.setEnabled(true);
		noticeMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						final JFileChooser fileChooser = new JFileChooser();
						final List<String> extensions = new ArrayList<String>();
						extensions.add("csv");
						MyFileNameExtensionFilter filter = new MyFileNameExtensionFilter("CSV-Datei", extensions);
						fileChooser.setFileFilter(filter);
						fileChooser.setDialogTitle("GND: Notizen exportieren");
						fileChooser.setApproveButtonText("Exportieren");


						File csvFile;
						while(true) {
							int showSaveDialog = fileChooser.showSaveDialog(GenericNetDisplay.this);
							if(!(showSaveDialog == JFileChooser.CANCEL_OPTION)) {
								File selectedFile = fileChooser.getSelectedFile();
								String path = selectedFile.getPath();

								if(!path.toLowerCase().endsWith(".csv")) {
									path += ".csv";
								}
								csvFile = new File(path);

								if(csvFile.exists()) {
									int n = JOptionPane.showConfirmDialog(
											new JFrame(),
											"Die Datei '" + csvFile.getName() + "' existiert bereits.\nDatei überschreiben?",
											"Warning",
											JOptionPane.YES_NO_OPTION
									);
									if(n == JOptionPane.YES_OPTION) {
										break;
									}
								}
								else {
									break;
								}
							}
							else {
								return;
							}
						}

						try {
							_noticeManager.exportToFile(csvFile);
						}
						catch(IOException e1) {
							JOptionPane.showMessageDialog(GenericNetDisplay.this, "Fehler beim Exportieren der Notizen: " + e1.getMessage());
							_debug.warning("Kann Notizen nicht exportieren", e1);
						}
					}
				}
		);


		menuItem = new JMenuItem("Notizen löschen");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Löscht alle Notizen.");
		if(_standAlone) {
			menuItem.setEnabled(true);
		}
		else {
			menuItem.setEnabled(false);
		}
		noticeMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						Object[] options = {"Löschen", "Abbrechen"};
						int n = JOptionPane.showOptionDialog(
								new JFrame(),
								"Alle Notizen werden unwiderruflich gelöscht.",
								"Notizen löschen",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]
						);
						if(n == 0) {
							_noticeManager.clear();
						}
					}
				}
		);
		// Extras-Menue
		JMenu extrasMenu = new JMenu("Extras");
		extrasMenu.setMnemonic(KeyEvent.VK_X);
		extrasMenu.getAccessibleContext().setAccessibleDescription("Das Extras-Menue");

		menuItem = new JMenuItem("Farbenverwaltung");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Öffnet den Farbdefinitionsdialog.");
		menuItem.setEnabled(true);
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						ColorDialog colorDialog = new ColorDialog();
						colorDialog.setVisible(true);
					}
				}
		);

		extrasMenu.add(new JSeparator());

		menuItem = new JMenuItem("Präferenzen exportieren");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Exportiert alle Benutzer-Einstellungen.");
		menuItem.setEnabled(true);
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						final JFileChooser fileChooser = new JFileChooser();
						final List<String> extensions = new ArrayList<String>();
						extensions.add("xml");
						MyFileNameExtensionFilter filter = new MyFileNameExtensionFilter("XML-Datei", extensions);
						fileChooser.setFileFilter(filter);
						fileChooser.setDialogTitle("GND: Präferenzen exportieren");
						fileChooser.setApproveButtonText("Exportieren");

						File xmlFile;
						while(true) {
							int showSaveDialog = fileChooser.showSaveDialog(GenericNetDisplay.this);
							if(!(showSaveDialog == JFileChooser.CANCEL_OPTION)) {
								File selectedFile = fileChooser.getSelectedFile();
								String path = selectedFile.getPath();

								if(!path.toLowerCase().endsWith(".xml")) {
									path += ".xml";
								}
								xmlFile = new File(path);

								if(xmlFile.exists()) {
									int n = JOptionPane.showConfirmDialog(
											new JFrame(),
											"Die Datei '" + xmlFile.getName() + "' existiert bereits.\nDatei überschreiben?",
											"Warning",
											JOptionPane.YES_NO_OPTION
									);
									if(n == JOptionPane.YES_OPTION) {
										break;
									}
								}
								else {
									break;
								}
							}
							else {
								return;
							}
						}

						Preferences gndPrefs = Preferences.userRoot().node("de/kappich/pat/gnd");
						try {
							gndPrefs.exportSubtree(new FileOutputStream(xmlFile));
						}
						catch(FileNotFoundException e1) {
							
							throw new UnsupportedOperationException("Catch-Block nicht implementiert - FileNotFoundException", e1);
						}
						catch(IOException e1) {
							
							throw new UnsupportedOperationException("Catch-Block nicht implementiert - IOException", e1);
						}
						catch(BackingStoreException e1) {
							
							throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e1);
						}
					}
				}
		);

		menuItem = new JMenuItem("Präferenzen importieren");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Importiert alle Benutzer-Einstellungen.");
		menuItem.setEnabled(true);
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						Object[] options = {"Weiter", "Abbrechen", "Hilfe"};
						int n = JOptionPane.showOptionDialog(
								new JFrame(),
								"Nach dem Import ist eine erneute Initialisierung oder ein Neustart nötig.",
								"Präferenzen importieren",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]
						);
						if(n == 2) {
							HelpPage.openHelp("#theExtrasMenu");
						}

						if(n != 0) {
							return;
						}
						n = JOptionPane.showOptionDialog(
								new JFrame(),
								"Ihre alten Präferrenzen werden komplett gelöscht. Noch können Sie sie sichern.",
								"Präferenzen importieren",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]
						);
						if(n != 0) {
							return;
						}

						final JFileChooser fileChooser = new JFileChooser();
						final List<String> extensions = new ArrayList<String>();
						extensions.add("xml");
						MyFileNameExtensionFilter filter = new MyFileNameExtensionFilter("XML-Datei", extensions);
						fileChooser.setFileFilter(filter);
						fileChooser.setDialogTitle("GND: Präferenzen importieren");
						fileChooser.setApproveButtonText("Importieren");

						int returnVal = fileChooser.showOpenDialog(GenericNetDisplay.this);

						if(returnVal == JFileChooser.APPROVE_OPTION) {
							File file = fileChooser.getSelectedFile();
							try {
								clearPreferences();
								Preferences.importPreferences(new FileInputStream(file));
							}
							catch(FileNotFoundException e1) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - FileNotFoundException", e1);
							}
							catch(IOException e1) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - IOException", e1);
							}
							catch(InvalidPreferencesFormatException e1) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - InvalidPreferencesFormatException", e1);
							}
							readPreferences();
							ColorManager.refreshInstance();
							DOTManager.refreshInstance();
							if(_dotManagerDialog != null) {
								_dotManagerDialog.closeDialog();
								_dotManagerDialog = new DOTManagerDialog(DOTManager.getInstance(), _connection);
							}
							LayerManager.refreshInstance();
							if(_layerManagerDialog != null) {
								_layerManagerDialog.closeDialog();
								_layerManagerDialog = new LayerManagerDialog(_connection);
							}
							ViewManager.refreshInstance();
							if(_viewManagerDialog != null) {
								_viewManagerDialog.closeDialog();
							}
							_viewManagerDialog = new ViewManagerDialog(GenericNetDisplay.this, true);
							_viewManagerDialog.showDialog();
						}
					}

					public void clearPreferences() {

						Preferences gndPrefs = Preferences.userRoot().node("de/kappich/pat/gnd");
						try {
							gndPrefs.clear();
						}
						catch(BackingStoreException e) {
							
							throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
						}
						clearChildren(gndPrefs);
					}

					private void clearChildren(Preferences prefs) {

						String[] childrenNames;
						try {
							childrenNames = prefs.childrenNames();
						}
						catch(BackingStoreException e) {
							
							throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
						}
						for(String childName : childrenNames) {
							Preferences childPrefs = prefs.node(prefs.absolutePath() + "/" + childName);
							try {
								childPrefs.clear();
							}
							catch(BackingStoreException e) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
							}
							clearChildren(childPrefs);
							try {
								childPrefs.removeNode();
							}
							catch(BackingStoreException e) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
							}
						}
					}
				}
		);

		menuItem = new JMenuItem("Präferenzen löschen");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Löscht alle Benutzer-Einstellungen.");
		if(_standAlone) {
			menuItem.setEnabled(true);
		}
		else {
			menuItem.setEnabled(false);
		}
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						Object[] options = {"Löschen und beenden", "Nicht löschen", "Hilfe"};
						int n = JOptionPane.showOptionDialog(
								new JFrame(),
								"Alle Benutzereinstellungen werden unwiderruflich gelöscht und das Programm beendet.",
								"Präferenzen löschen",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]
						);
						if(n == 0) {
							Preferences gndPrefs = Preferences.userRoot().node("de/kappich/pat/gnd");
							try {
								gndPrefs.removeNode();
								System.exit(0);
							}
							catch(BackingStoreException ex2) {
								
								throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", ex2);
							}
						}
						else if(n == 2) {
							HelpPage.openHelp("#theExtrasMenu");
						}
					}
				}
		);

		extrasMenu.add(new JSeparator());

		menuItem = new JMenuItem("Ausgewählte Objekte an den GTM übergeben");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Übergibt dem GTM die ausgewählten Objekte.");
		if(_standAlone) {
			menuItem.setEnabled(false);
		}
		else {
			menuItem.setEnabled(true);
		}
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						notifySelectionListenersSelectionChanged(_mapPane.getSelectedSystemObjects());
					}
				}
		);

		extrasMenu.add(new JSeparator());

		JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem("Doppelpufferung");
		checkBoxMenuItem.getAccessibleContext().setAccessibleDescription("Schaltet die Doppelpufferung ein oder aus.");
		checkBoxMenuItem.setEnabled(true);
		checkBoxMenuItem.setSelected(_isDoubleBuffered);
		extrasMenu.add(checkBoxMenuItem);
		checkBoxMenuItem.addItemListener(
				new ItemListener() {

					public void itemStateChanged(ItemEvent e) {

						if(e.getStateChange() == ItemEvent.SELECTED) {
							_mapPane.setDoubleBuffered(true);
						}
						else {
							_mapPane.setDoubleBuffered(false);
						}
					}
				}
		);

		checkBoxMenuItem = new JCheckBoxMenuItem("Antialiasing");
		checkBoxMenuItem.getAccessibleContext().setAccessibleDescription("Schaltet das Antialising ein oder aus.");
		checkBoxMenuItem.setEnabled(true);
		checkBoxMenuItem.setSelected(_isAntiAliasingOn);
		extrasMenu.add(checkBoxMenuItem);
		checkBoxMenuItem.addItemListener(
				new ItemListener() {

					public void itemStateChanged(ItemEvent e) {

						if(e.getStateChange() == ItemEvent.SELECTED) {
							_mapPane.setAntialising(true);
							_mapPane.repaint();
						}
						else {
							_mapPane.setAntialising(false);
							_mapPane.repaint();
						}
					}
				}
		);

		extrasMenu.add(new JSeparator());

		checkBoxMenuItem = new JCheckBoxMenuItem("Tooltipp auf der Karte");
		checkBoxMenuItem.getAccessibleContext().setAccessibleDescription("Schaltet den Tooltipp auf der ein oder aus.");
		checkBoxMenuItem.setEnabled(true);
		checkBoxMenuItem.setSelected(_isMapsTooltipOn);
		extrasMenu.add(checkBoxMenuItem);
		checkBoxMenuItem.addItemListener(
				new ItemListener() {

					public void itemStateChanged(ItemEvent e) {

						if(e.getStateChange() == ItemEvent.SELECTED) {
							_mapPane.setTooltip(true);
						}
						else {
							_mapPane.setTooltip(false);
						}
					}
				}
		);

		extrasMenu.add(new JSeparator());

		menuItem = new JMenuItem("Bildschirmauflösung einstellen");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Bildschirmauflösung in Dots-per-Inch");
		menuItem.setEnabled(true);
		extrasMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent outerE) {

						final JFrame resolutionFrame = new JFrame("Bildschirmauflösung einstellen");

						final JLabel textLabel = new JLabel("Geben sie den Wert für die Bildschirmauflösung in Dots per Inch an: ");
						Double dpi = getScreenResolution();
						if(dpi == null) {
							dpi = new Double(Toolkit.getDefaultToolkit().getScreenResolution());
						}
						if(dpi == null || dpi.isNaN() || dpi.isInfinite()) {
							dpi = 72.;
						}
						final Double currentResolution = dpi;	// Wir brauchen diese Variable als 'final' in der nächsten Zeile!
						final SpinnerNumberModel numberModel = new SpinnerNumberModel((double)currentResolution, 1., 1000., 0.1);
						final JSpinner resolutionSpinner = new JSpinner(numberModel);

						final JPanel resolutionPanel = new JPanel();
						resolutionPanel.setLayout(new BoxLayout(resolutionPanel, BoxLayout.X_AXIS));
						resolutionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
						resolutionPanel.add(textLabel);
						resolutionPanel.add(Box.createRigidArea(new Dimension(10, 10)));
						resolutionPanel.add(resolutionSpinner);

						final JPanel acceptPanel = new JPanel();
						final JButton acceptButton = new JButton("Übernehmen");
						final JButton clearButton = new JButton("Zurücksetzen");
						acceptButton.setToolTipText("Wert zur Maßstabsberechnung übernehmen");
						clearButton.setToolTipText("Benutzer-Präferenz löschen und berechneten Wert anzeigen");
						acceptPanel.add(acceptButton);
						acceptPanel.add(Box.createRigidArea(new Dimension(10, 10)));
						acceptPanel.add(clearButton);

						final ActionListener acceptActionListener = new ActionListener() {

							public void actionPerformed(ActionEvent InnerE) {

								final Double newScreenResolution = (Double)resolutionSpinner.getValue();
								if(newScreenResolution < 10.) {
									JOptionPane.showMessageDialog(
											null, "Bitte geben Sie einen Wert größer oder gleich 10 an.", "Fehlermeldung", JOptionPane.ERROR_MESSAGE
									);
									return;
								}
								if(newScreenResolution > 10000.) {
									JOptionPane.showMessageDialog(
											null, "Bitte geben Sie einen Wert kleiner oder gleich 10000 an.", "Fehlermeldung", JOptionPane.ERROR_MESSAGE
									);
									return;
								}
								final Double oldScreenResolution = getScreenResolution();
								_screenResolution = newScreenResolution;
								writeResolutionPreference(newScreenResolution);
								notifyResolutionListenersResolutionChanged(newScreenResolution, oldScreenResolution);
								resolutionFrame.dispose();
							}
						};
						acceptButton.addActionListener(acceptActionListener);

						final ActionListener clearActionListener = new ActionListener() {

							public void actionPerformed(ActionEvent InnerE) {

								final Double olddpi = getScreenResolution();
								clearResolutionPreference();
								Double newdpi = new Double(Toolkit.getDefaultToolkit().getScreenResolution());
								if(newdpi == null || newdpi.isNaN() || newdpi.isInfinite()) {
									newdpi = 72.;
								}
								numberModel.setValue(newdpi);
								_screenResolution = newdpi;
								notifyResolutionListenersResolutionChanged(newdpi, olddpi);
							}
						};
						clearButton.addActionListener(clearActionListener);

						final JPanel dummyPanel = new JPanel();
						dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.Y_AXIS));
						dummyPanel.add(resolutionPanel);
						dummyPanel.add(acceptPanel);
						dummyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

						resolutionFrame.add(dummyPanel);
						resolutionFrame.pack();
						resolutionFrame.setLocation(50, 50);
						resolutionFrame.setVisible(true);
					}
				}
		);


		// Hilfe-Menue
		JMenu helpMenu = new JMenu("Hilfe");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.getAccessibleContext().setAccessibleDescription("Das Hilfe-Menue");

		menuItem = new JMenuItem("Online-Hilfe öffnen");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Öffnet ein Browser-Fenster mit der Online-Hilfe");
		menuItem.setEnabled(true);
		helpMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent event) {

						HelpPage.openHelp("#logo");
					}
				}
		);

		menuItem = new JMenuItem("Online-Hilfe speichern");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.getAccessibleContext().setAccessibleDescription("Speichert die Online-Hilfe in einer HTML-Datei");
		menuItem.setEnabled(true);
		helpMenu.add(menuItem);
		menuItem.addActionListener(
				new ActionListener() {

					public void actionPerformed(ActionEvent e) {

						final JFileChooser fileChooser = new JFileChooser();
						final List<String> extensions = new ArrayList<String>();
						extensions.add("html");
						extensions.add("htm");
						MyFileNameExtensionFilter filter = new MyFileNameExtensionFilter("HTML-Datei", extensions);
						fileChooser.setFileFilter(filter);
						fileChooser.setDialogTitle("GND: Online-Hilfe speichern");
						fileChooser.setApproveButtonText("Speichern");

						String path = "";
						File htmlFile = new File(path);

						while(true) {
							int showSaveDialog = fileChooser.showSaveDialog(null);
							if(!(showSaveDialog == JFileChooser.CANCEL_OPTION)) {
								File selectedFile = fileChooser.getSelectedFile();
								path = selectedFile.getPath();

								if(!path.toLowerCase().endsWith(".html")) {
									path += ".html";
								}

								htmlFile = new File(path);

								if(htmlFile.exists()) {
									int n = JOptionPane.showConfirmDialog(
											new JFrame(),
											"Die Datei '" + htmlFile.getName() + "' existiert bereits.\nDatei überschreiben?",
											"Warning",
											JOptionPane.YES_NO_OPTION
									);
									if(n == JOptionPane.YES_OPTION) {
										break;
									}
								}
								else {
									break;
								}
							}
							else {
								return;
							}
						}
						File directory = new File(path.replace(".html", "") + "-Dateien");
						// Ordner anlegen mit dem Namen des Files
						if(!directory.mkdir()) {
							JOptionPane.showMessageDialog(
									null,
									"Die Verzeichnis '" + htmlFile.getName() + "' konnte nicht angelegt werden.",
									"Fehlermeldung",
									JOptionPane.ERROR_MESSAGE
							);
							return;
						}
						Map<String, BufferedImage> images = HelpPage.getImages();

						for(String name : images.keySet()) {
							BufferedImage image = images.get(name);
							try {
								ImageIO.write(image, "png", new File(directory.getPath() + "/" + name));
							}
							catch(IOException eWrite) {
								eWrite.printStackTrace();
							}
						}
						try {
							StringBuilder completeText = new StringBuilder();
							final URL helpPage = HelpPage.class.getResource("documentation.html");
							URLConnection urlConnection = helpPage.openConnection();
							BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "ISO-8859-1"));

							String inputLine;
							String lineSeparator = System.getProperty("line.separator");
							while((inputLine = reader.readLine()) != null) {
								completeText.append(inputLine).append(lineSeparator);
							}

							BufferedWriter bufferedWriter = new BufferedWriter(
									new OutputStreamWriter(new FileOutputStream(htmlFile), "ISO-8859-1")
							);
							bufferedWriter.write(completeText.toString().replace("<img src=\"", "<img src=\"" + directory.getName() + "/"));
							bufferedWriter.close();
						}
						catch(IOException eWrite) {
							eWrite.printStackTrace();
						}
					}
				}
		);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(layerMenu);
		menuBar.add(dotMenu);
		menuBar.add(noticeMenu);
		menuBar.add(extrasMenu);
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
	}

	/** Methode zum Drucken der Kartenansicht. */
	public void printNetDisplay() {

		PrinterJob printerJob = PrinterJob.getPrinterJob();

		// Default Seitenformat festlegen
		PageFormat pageFormat = printerJob.pageDialog(printerJob.defaultPage());
		// Grafik mit eingestelltem Seitenformat drucken
		printerJob.setPrintable(_mapPane, pageFormat);

		PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
		printAttributes.add(new PrinterResolution(600, 600, PrinterResolution.DPI));

		// Drucken
		if(printerJob.printDialog()) {
			class PrintThread extends Thread {

				private final PrinterJob _printerJob;

				private final PrintRequestAttributeSet _printAttributes;

				PrintThread(
						final PrinterJob job, final PrintRequestAttributeSet attributes) {

					_printerJob = job;
					_printAttributes = attributes;
				}

				@Override
				public void run() {

					try {
						_printerJob.print(_printAttributes);
					}
					catch(Exception pe) {
						JOptionPane.showMessageDialog(
								null, "Fehler beim Drucken", "Fehlermeldung", JOptionPane.ERROR_MESSAGE
						);
					}
				}
			}
			PrintThread printThread = new PrintThread(printerJob, printAttributes);
			printThread.run();
		}
	}

	/**
	 * Methode zum Abspeichern  der Grafik.
	 *
	 * @throws IOException wird geworfen, wenn etwas beim Speichern fehlschlägt
	 */
	public void saveGrafic() throws IOException {

		JFileChooser fileChooser = new JFileChooser();
		final List<String> extensions = new ArrayList<String>();
		extensions.add("png");
		MyFileNameExtensionFilter filter = new MyFileNameExtensionFilter("PNG Images", extensions);
		fileChooser.setFileFilter(filter);
		fileChooser.setDialogTitle("Netzdarstellung speichern");
		fileChooser.setVisible(true);

		// Dialog zum Speichern einer Datei
		fileChooser.showSaveDialog(fileChooser);

		// Höhe und Breite mithilfe des Rechteckes aus der Grafik werden festgelegt
		Rectangle imageRectangle = _mapPane.getBounds();
		int width = imageRectangle.width;
		int height = imageRectangle.height;

		Dimension dimension = new Dimension(width, height);

		GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage image = graphicsConfiguration.createCompatibleImage((int)dimension.getWidth(), (int)dimension.getHeight());

		// Grafik wird erstellt
		// Grafik wird abgespeichert
		final Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.WHITE);
		Rectangle rect = new Rectangle(0, 0, width, height);
		g2d.fill(rect);
		_mapPane.paintComponent(g2d);
		g2d.drawImage(image, width, height, Color.WHITE, fileChooser);
		//Grafik wird im .png Format abgespeichert
		final File selectedFile = fileChooser.getSelectedFile();
		if(selectedFile == null) {
			return;
		}
		if(selectedFile.getName().endsWith("png")) {
			ImageIO.write(image, "png", selectedFile);
		}
		else {
			ImageIO.write(image, "png", new File(selectedFile + ".png"));
		}
	}

	/**
	 * Gibt die Systemobjekte, die im Konstruktor angegeben wurden, zurück.
	 *
	 * @return gibt die Systemobjekte, die im Konstruktor angegeben wurden, zurück
	 */
	public List<SystemObject> getSystemObjects() {

		return _systemObjects;
	}

	/**
	 * Gibt <code>true</code> zurück, wenn die Kartenansicht mit Doppelpufferung ( double buffering) betrieben wird.
	 *
	 * @return <code>true</code> genau dann, wenn die Doppelpufferung aktiv ist
	 */
	@Override
	public boolean isDoubleBuffered() {

		return _isDoubleBuffered;
	}

	/**
	 * Gibt <code>true</code> zurück, wenn die Kartenansicht mit Anti-Aliasing betrieben wird.
	 *
	 * @return <code>true</code> genau dann, wenn die Anti-Aliasing aktiv ist
	 */
	public boolean isAntiAliasingOn() {

		return _isAntiAliasingOn;
	}

	/**
	 * Gibt <code>true</code> zurück, wenn die Kartenansicht mit Tooltipp betrieben wird.
	 *
	 * @return <code>true</code> genau dann, wenn der Tooltipp der Kartenansicht aktiv ist
	 */
	public boolean isMapsTooltipOn() {

		return _isMapsTooltipOn;
	}

	/**
	 * Gibt die Bildschirmauflösung zurück, mit deren Hilfe das GenericNetDisplay den Maßstab zu bestimmen versucht.
	 *
	 * @return die aktuelle Bildschirmauflösung
	 */
	public Double getScreenResolution() {

		return _screenResolution;
	}

	/**
	 * Gibt den Ausgangknoten zum Abspeichern der Benutzer-Präferenzen zurück.
	 *
	 * @return den Ausgangknoten zum Abspeichern der Benutzer-Präferenzen
	 */
	private static Preferences getPreferenceStartPath() {

		return Preferences.userRoot().node("de/kappich/pat/gnd/GND");
	}

	/**
	 * Holt die Bildschirmauflösung aus den Präferenzen, wenn sie dort hinterlegt ist, oder berechnet sie andernfalls.
	 *
	 * @return die Bildschirmauflösung
	 */
	public static Double getScreenResolutionFromPreferences() {

		final Preferences gndPrefs = getPreferenceStartPath();
		Preferences resolutionPrefs = gndPrefs.node(gndPrefs.absolutePath() + "/resolution");
		return resolutionPrefs.getDouble(RESOLUTION, new Double(Toolkit.getDefaultToolkit().getScreenResolution()));
	}

	/**
	 * Holt den Namen der Startansicht aus den Präferenzen, wenn er dort hinterlegt ist.
	 *
	 * @return der Name der Startansicht
	 */
	public static String getStartViewNameFromPreferences() {

		final Preferences gndPrefs = getPreferenceStartPath();
		Preferences startViewPrefs = gndPrefs.node(gndPrefs.absolutePath() + "/startViewName");
		return startViewPrefs.get(STARTVIEWNAME, null);
	}

	/*
		 * Liest die Benutzer-Präferenzen ein.
		 */
	private void readPreferences() {

		_screenResolution = getScreenResolutionFromPreferences();
		_startViewName = getStartViewNameFromPreferences();
	}

	/*
		 * Speichert die Benutzer-Präferenzen zur Bildschirmauflösung.
		 */
	private void writeResolutionPreference(final Double screenResolution) {

		Preferences gndPrefs = getPreferenceStartPath();
		Preferences resolutionPrefs = gndPrefs.node(gndPrefs.absolutePath() + "/resolution");
		resolutionPrefs.putDouble(RESOLUTION, screenResolution);
	}

	/*
		 * Löscht die Benutzer-Präferenzen zur Bildschirmauflösung.
		 */
	private void clearResolutionPreference() {

		Preferences gndPrefs = getPreferenceStartPath();
		Preferences resolutionPrefs = gndPrefs.node(gndPrefs.absolutePath() + "/resolution");
		try {
			resolutionPrefs.removeNode();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
	}

	/**
	 * Speichert den Namen der Startansicht in den Benutzer-Präferenzen.
	 *
	 * @param startViewName der Name der Startansicht
	 */
	public void writeStartViewNamePreference(final String startViewName) {

		Preferences gndPrefs = getPreferenceStartPath();
		Preferences startViewPrefs = gndPrefs.node(gndPrefs.absolutePath() + "/startViewName");
		startViewPrefs.put(STARTVIEWNAME, startViewName);
	}

	public void setVisibleObjects(final Set<SystemObject> displayObjects) {

		Collection<SystemObject> objectsWithNotice = _noticeManager.getObjectsWithNotice();
		List<SystemObject> visibleNoticeObjects = new ArrayList<SystemObject>();
		for(SystemObject object : objectsWithNotice) {
			if(displayObjects.contains(object)) {
				visibleNoticeObjects.add(object);
			}
		}
		_selectedObjects.setElements(visibleNoticeObjects);

		// Bewirkt, dass das ausgewählte Objekt wieder in der liste selektiert wird
		final Collection<SystemObject> selectedSystemObjects = _mapPane.getSelectedSystemObjects();
		_objectList.setSelectedIndices(_selectedObjects.getElementIndizes(selectedSystemObjects));
	}

	public void selectionChanged() {

		String text = "Ein Objekt auswählen um eine Notiz einzugeben";
		final Collection<SystemObject> selectedSystemObjects = _mapPane.getSelectedSystemObjects();
		final boolean validObjectSelected = selectedSystemObjects.size() == 1;
		if(validObjectSelected) {
			for(final SystemObject systemObject : selectedSystemObjects) {
				text = _noticeManager.getNotice(systemObject).getMessage();
			}
		}
		_noticeTextArea.setEditable(validObjectSelected);
		if(!validObjectSelected) {
			_noticeTextArea.setForeground(Color.gray);
			_noticeTextArea.setBackground(Color.white);
			_noticeTextArea.setFont(_noticeTextArea.getFont().deriveFont(Font.ITALIC));
		}
		else {
			_noticeTextArea.setForeground(Color.black);
			_noticeTextArea.setBackground(Color.white);
			_noticeTextArea.setFont(_noticeTextArea.getFont().deriveFont(Font.PLAIN));
		}
		_systemEdit = true;
		_noticeTextArea.setText(text);
		_systemEdit = false;
		_objectList.setSelectedIndices(_selectedObjects.getElementIndizes(selectedSystemObjects));
	}

	public NoticeManager getNoticeManager() {

		return _noticeManager;
	}

	/**
	 * Eine Listener-Interface für Objekte, die sich auf Änderungen der Bildschirmauflösung anmelden wollen.
	 *
	 * @author Kappich Systemberatung
	 * @version $Revision: 9358 $
	 */
	interface ResolutionListener {

		/**
		 * Diese Methode wird aufgerufen, wenn die Auflösung geändert wird. Aufgrund der Übergabe des alten und neuen Wertes können auch relative Änderungen vollzogen
		 * werden.
		 *
		 * @param newValue die neue Bildschirmauflösung
		 * @param oldValue die alte Bildschirmauflösung
		 */
		void resolutionChanged(final Double newValue, final Double oldValue);
	}

	/**
	 * Fügt das übergebene Objekt der Liste der auf Änderungen der Bildschirmauflösung angemeldeten Objekte hinzu.
	 *
	 * @param listener der hinzuzufügende Listener
	 */
	public void addResolutionListener(final ResolutionListener listener) {

		_resolutionListeners.add(listener);
	}

	/**
	 * Entfernt das übergebene Objekt aus der Liste der auf Änderungen der Bildschirmauflösung angemeldeten Objekte und gibt <code>true</code> zurück, wenn dies
	 * erfolgreich war, und <code>false</code> sonst.
	 *
	 * @param listener der zu entfernende Listener
	 *
	 * @return <code>true</code> genau dann, wenn der Listener entfernt wurde
	 */
	public boolean removeResolutionListener(final ResolutionListener listener) {

		return _resolutionListeners.remove(listener);
	}

	/**
	 * Benachrichtigt alle Objekte, die auf Änderungen der Bildschirmauflösung angemeldet sind.
	 *
	 * @param newResolution die neue Bildschirmauflösung
	 * @param oldResulotion die alte Bildschirmauflösung
	 */
	private void notifyResolutionListenersResolutionChanged(final Double newResolution, final Double oldResulotion) {

		for(ResolutionListener listener : _resolutionListeners) {
			listener.resolutionChanged(newResolution, oldResulotion);
		}
	}

	/**
	 * Fügt das übergebene Objekt der Liste der auf Änderungen der Selektion angemeldeten Objekte hinzu.
	 *
	 * @param listener der neue Listener
	 */
	public void addSelectionListener(final SelectionListener listener) {

		_selectionListeners.add(listener);
	}

	/**
	 * Entfernt das übergebene Objekt aus der Liste der auf Änderungen der Selektion angemeldeten Objekte.
	 *
	 * @param listener der zu entfernende Listener
	 */
	public boolean removeSelectionListener(final SelectionListener listener) {

		return _selectionListeners.remove(listener);
	}

	/**
	 * Benachrichtigt alle Objekte, die auf Änderungen der Selektion angemeldet sind.
	 *
	 * @param systemObjects die neue Selektion
	 */
	private void notifySelectionListenersSelectionChanged(final Collection<SystemObject> systemObjects) {

		for(SelectionListener listener : _selectionListeners) {
			listener.setSelectedObjects(systemObjects);
		}
	}

	private class MyFileNameExtensionFilter extends FileFilter {

		// Ähnlich zu FileNameExtensionFilter aus java.swing.JFileChooser, aber der ist Java 1.6!
		final String _description;

		final List<String> _extensions = new ArrayList<String>();

		public MyFileNameExtensionFilter(
				final String description, final List<String> extensions) {

			_description = description;
			if(extensions == null || (extensions.size() == 0)) {
				throw new IllegalArgumentException(
						"The extensions must be non-null and not empty"
				);
			}
			for(String extension : extensions) {
				_extensions.add(extension.toLowerCase(Locale.ENGLISH));
			}
		}

		@Override
		public boolean accept(File f) {

			if(f != null) {
				if(f.isDirectory()) {
					return true;
				}
				String fileName = f.getName();
				int i = fileName.lastIndexOf('.');
				if(i > 0 && i < fileName.length() - 1) {
					String desiredExtension = fileName.substring(i + 1).toLowerCase(Locale.ENGLISH);
					for(String extension : _extensions) {
						if(desiredExtension.equals(extension)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public String getDescription() {

			return _description;
		}
	}

	/**
	 * Diese Methode macht die externen Plugins bekannt, indem die vollständigen Namen der Klassen, die DisplayObjectTypePlugin implementieren, übergeben werden.
	 * Sie muss vor dem ersten Zugriff auf Teile dieser Plugins aufgerufen werden; der beste Moment dafür ist, bevor der erste Konstruktor von GenericNetDisplay
	 * aufgerufen wird, denn sonst könnte schon die Initialisierung aus den Präferenzen scheitern. Die Arbeit wird an den PluginManager delegiert. Durch das
	 * Anbieten dieser Methode muss der Benutzer (also z.B. GTM oder RunGND) der GND nur mit GenericNetDisplay arbeiten.
	 *
	 * @param plugins die neuen externen Plugins
	 */
	public static void addPlugins(final List<String> plugins) {

		if(plugins == null || (plugins.size() == 0)) {
			return;
		}
		PluginManager.addPlugins(plugins);
	}

	private class ObjectListListener extends MouseAdapter implements KeyListener {

		@Override
		public void mouseClicked(final MouseEvent e) {
			valueChanged();
		}

		public void valueChanged() {
			final int selectedIndex = _objectList.getSelectedIndex();
			_mapPane.selectObject(_selectedObjects.getObject(selectedIndex));
		}

		public void keyTyped(final KeyEvent e) {
		}

		public void keyPressed(final KeyEvent e) {
			final int selectedIndex = _objectList.getSelectedIndex();
			_mapPane.selectObject(_selectedObjects.getObject(selectedIndex));
		}

		public void keyReleased(final KeyEvent e) {
			final int selectedIndex = _objectList.getSelectedIndex();
			_mapPane.selectObject(_selectedObjects.getObject(selectedIndex));
		}
	}
}
