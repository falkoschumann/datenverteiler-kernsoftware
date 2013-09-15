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
package de.kappich.pat.gnd.layerManagement;

import de.kappich.pat.gnd.displayObjectToolkit.DOTCollection;
import de.kappich.pat.gnd.displayObjectToolkit.DOTManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTSubscriptionData;
import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.utils.SpringUtilities;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.dav.daf.main.impl.config.DafConfigurationObjectType;
import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Der Dialog zur Definition und Bearbeitung von Layern.
 *  
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
@SuppressWarnings("serial")
public class LayerDefinitionDialog extends JFrame {
	
	final private ClientDavInterface _connection;
	
	final private DataModel _configuration;
	
	private Layer _scratchLayer;
	
	private Layer _unchangableOriginalLayer;
	
	private boolean _editable;
	
	private boolean _nameChangable;
	
	final private JTextField _nameTextField = new JTextField();
	
	final private JTextField _infoTextField = new JTextField();
	
	final private JTable _dotTable  = new JTable();
	
	final private JComboBox _geoReferenceObjectTypesComboBox;
	
	final private List<EditableListenButton> _listeningButtons = new ArrayList<EditableListenButton>();
	
	final private EditableListenButton _newDOTButton = new EditableListenButton("Neue Zeile");
	
	final private JButton _deleteDOTButton = new JButton("Zeile löschen");
	
	private boolean _somethingChanged = false;
	
	private final static Debug _debug = Debug.getLogger();
	
	/**
	 * Konstruktor zum Anlegen eines neuen Layereditors.
	 * 
	 * @param connection die Datenverteiler-Verbindung
	 * @param layer ein Layer
	 * @param editable ist der Layer verändebar?
	 * @param nameChangable ist der Name und damit die Identität änderbar?
	 * @param title der Titel des Fensters
	 */
	public LayerDefinitionDialog( final ClientDavInterface connection, 
			final Layer layer, final boolean editable, final boolean nameChangable,
			final String title) {
		super( title);
		_connection = connection;
		_configuration = _connection.getDataModel();
		if ( layer == null ) {
			_debug.error("Ein LayerDefinitionDialog kann nicht mit einem Null-Layer konstruiert werden.");
			dispose();
		}
		_scratchLayer = layer.getCopy();
		_unchangableOriginalLayer = _scratchLayer.getCopy();
		_editable = editable;
		_nameChangable = nameChangable;
		
		setLayout(new BorderLayout());
		
		Dimension labelSize = new Dimension(100, 20);
		
		// Oberer Teil mit Name, Info und geometrie-Klasse
		
		JLabel nameLabel = new JLabel("Name: ");
		nameLabel.setPreferredSize( labelSize);
		_nameTextField.setText( _scratchLayer.getName());
		_nameTextField.setEditable( nameChangable);
		
		JLabel infoLabel = new JLabel("Info: ");
		infoLabel.setPreferredSize( labelSize);
		_infoTextField.setText( _scratchLayer.getInfo());
		
		JLabel typeLabel = new JLabel("Typ: ");
		typeLabel.setPreferredSize( labelSize);
		_geoReferenceObjectTypesComboBox = new JComboBox( getGeoReferenceObjectTypes());
		_geoReferenceObjectTypesComboBox.setPreferredSize(new Dimension(200, 25));
		
		setSelectedItemForType();
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new SpringLayout());
		upperPanel.add(nameLabel);
		upperPanel.add(_nameTextField);
		upperPanel.add(infoLabel);
		upperPanel.add(_infoTextField);
		upperPanel.add(typeLabel);
		upperPanel.add(_geoReferenceObjectTypesComboBox);
		
		upperPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(upperPanel, 2, 5, 5);
		add( upperPanel, BorderLayout.NORTH);
		
		// Mittelteil für die DOTs
		
		setTableProperties( _scratchLayer.getDotCollection());
		addListSelectionListener();
		
		addDOTButtonListener();
		
		JPanel dotButtonsPanel = new JPanel();
		dotButtonsPanel.setLayout(new SpringLayout());
		
		dotButtonsPanel.add( _newDOTButton);
		dotButtonsPanel.add( _deleteDOTButton);
		
		dotButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(dotButtonsPanel, 1, 5, 20);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new SpringLayout());
		centerPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		centerPanel.add( new JScrollPane( _dotTable));
		centerPanel.add( dotButtonsPanel);
		SpringUtilities.makeCompactGrid(centerPanel, 2, 20, 5);
		
		add( centerPanel, BorderLayout.CENTER);
		
		// Untere Teil mit Buttons
		
		EditableListenButton saveButton = new EditableListenButton("Layer speichern");
		JButton cancelButton = new JButton("Dialog schließen");
		JButton helpButton = new JButton("Hilfe");
		addButtonListener( saveButton, cancelButton, helpButton);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new SpringLayout());
		
		buttonsPanel.add(saveButton);
		buttonsPanel.add(cancelButton);
		buttonsPanel.add(helpButton);
		
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(buttonsPanel, 3, 20, 5);
		add(buttonsPanel, BorderLayout.SOUTH);
		
		addChangeListeners();
		addFrameListener();
		
		setEditable(_editable, _nameChangable);
		
		final Rectangle bounds = getBounds();
		setLocation(new Point((int)(bounds.getHeight() / 1.5 + 660), (int)(bounds.getWidth() / 1.5)));
		pack();
		setSize(650, 350);
		setVisible(true);
	}
	
	private void addChangeListeners() {
		ActionListener nameTextFieldListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_somethingChanged = true;
			}
		};
		_nameTextField.addActionListener( nameTextFieldListener);
		
		ActionListener infoTextFieldListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_somethingChanged = true;
			}
		};
		_infoTextField.addActionListener( infoTextFieldListener);
		
		ActionListener geoObjectComboBoxListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_somethingChanged = true;
			}
		};
		_geoReferenceObjectTypesComboBox.addActionListener( geoObjectComboBoxListener);
		
		TableModelListener tableModelListener = new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				_somethingChanged = true;
			}
		};
		_dotTable.getModel().addTableModelListener( tableModelListener);
	}
	
	private interface LayersEditingStateListener {
		void layersEditingStateChanged( boolean editable);
	}
	
	private class EditableListenButton extends JButton implements LayersEditingStateListener {
		
		EditableListenButton ( String text) {
			super( text);
			_listeningButtons.add( this);
			
		}
		public void layersEditingStateChanged( boolean editable) {
			setEnabled( editable);
		}
	}
	
	private Object[] getGeoReferenceObjectTypes() {
		final SystemObjectType geoReferenceObjectType = _configuration.getType("typ.geoReferenzObjekt");
		final List<SystemObjectType> geoReferenceObjectTypes = new ArrayList<SystemObjectType>();
		geoReferenceObjectTypes.addAll( geoReferenceObjectType.getSubTypes());
		for( int i = 0; i < geoReferenceObjectTypes.size(); i++) {
			geoReferenceObjectTypes.addAll( getGeoReferenceObjectTypes(geoReferenceObjectTypes.get(i)));
		}
		// Merkwürdigerweise enthält geoReferenceObjectTypes manche Einträge zweimal! Deshalb jetzt
		// alles in ein Set. Vorher wurde hier sortiert, was auch nicht besser ist.
		Comparator<SystemObjectType> comparator = new Comparator<SystemObjectType>() {
			public int compare(SystemObjectType o1, SystemObjectType o2) {
				return o1.toString().compareTo(o2.toString());
			}
		};
		final Set<SystemObjectType> geoReferenceObjectTypeSet = new TreeSet<SystemObjectType>( comparator);
		for ( SystemObjectType systemObjectType : geoReferenceObjectTypes) {
			geoReferenceObjectTypeSet.add( systemObjectType);
		}
		return geoReferenceObjectTypeSet.toArray();
	}
	
	private List<SystemObjectType> getGeoReferenceObjectTypes(SystemObjectType systemObjectType) {
		return systemObjectType.getSubTypes();
	}
	
	private void setTableProperties( DOTCollection dotCollection) {
		_dotTable.setModel( dotCollection);
		_dotTable.setRowHeight( 25);
		_dotTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int vColIndex = 0;
		TableColumn col = _dotTable.getColumnModel().getColumn(vColIndex);
		col.setPreferredWidth( 327);
		_deleteDOTButton.setEnabled( false);
	}
	
	/**
	 * Setzt die Felder des Layereditors mit den Informationen des übergebenen Layers und
	 * aktiviert die Veränderbarkeit gemäß der zwei boolschen Werte.
	 * 
	 * @param layer ein Layer
	 * @param editable ist der Layer veränderbar?
	 * @param nameChangable ist der Name und damit die Identität des Layers änderbar?
	 */
	public void setLayer(Layer layer, boolean editable, boolean nameChangable) {
		_scratchLayer = layer;
		_unchangableOriginalLayer = _scratchLayer.getCopy();
		_editable = editable;
		_nameTextField.setText( _scratchLayer.getName());
		_infoTextField.setText( _scratchLayer.getInfo());
		setTableProperties( _scratchLayer.getDotCollection());
		setSelectedItemForType();
		setEditable(editable, nameChangable);
		for ( EditableListenButton elButton : _listeningButtons) {
			elButton.layersEditingStateChanged( _editable);
		}
		_somethingChanged = false;
	}
	
	/**
	 * Setzt den Wert der internen Variable, die darüber entscheidet, ob die Informationen
	 * des angezeigten Layers veränderbar sind, und macht Textfelder veränderbar oder nicht,
	 * aktiviert bzw. deaktiviert Knöpfe usw.
	 * 
	 * @param editable ist der Layer veränderbar?
	 * @param nameChangable ist der Name und damit die Identität des Layers änderbar?
	 */
	public void setEditable(boolean editable, boolean nameChangable) {
		_editable = editable;
		_nameChangable = nameChangable;
		for ( EditableListenButton elButton : _listeningButtons) {
			elButton.layersEditingStateChanged( _editable);
		}
		_nameTextField.setEditable(_nameChangable);
		_infoTextField.setEditable(_editable);
		_geoReferenceObjectTypesComboBox.setEnabled( _editable);
		_dotTable.setRowSelectionAllowed( _editable);
		_newDOTButton.setEnabled( _editable);
	}
	
	private void setSelectedItemForType () {
		final String geoReferenceType = _scratchLayer.getGeoReferenceType();
		if ( geoReferenceType.length() != 0) {
			final SystemObject object = _configuration.getObject(geoReferenceType);
			if ( object != null) {
				_geoReferenceObjectTypesComboBox.setSelectedItem( object);
			}
		}
	}
	
	private void addDOTButtonListener() {
		
		class DOTDialog extends JDialog {
			
			final private JComboBox _comboBox;
			
			final private JTextField _fromTextField;
			
			final private JTextField _toTextField;
			
			DOTDialog () {
				super(LayerDefinitionDialog.this, true);
				
				JLabel dotLabel = new JLabel("Darstellungstyp: ");
				_comboBox = new JComboBox( DOTManager.getInstance().getDOTNames());
				
				JLabel fromLabel = new JLabel("Von 1: ");
				_fromTextField = new JTextField();
				JLabel toLabel = new JLabel("Bis 1: ");
				_toTextField = new JTextField();
				
				int selectedRow = _dotTable.getSelectedRow();
				if ( selectedRow == -1) {
					if ( _dotTable.getModel().getRowCount() > 0) {
						selectedRow = 0;
					}
				}
				if ( selectedRow > -1 ) {
					final DOTCollection dotCollection = _scratchLayer.getDotCollection();
					_comboBox.setSelectedItem( dotCollection.getValueAt( selectedRow, 0));
					final Integer fromValueAt = (Integer) dotCollection.getValueAt( selectedRow, 1);
					_fromTextField.setText( fromValueAt.toString());
					final Integer toValueAt = (Integer) dotCollection.getValueAt( selectedRow, 2);
					_toTextField.setText( toValueAt.toString());
				} else {
					_fromTextField.setText( "2147483647");
					_toTextField.setText( "1");
				}
				
				JPanel panel = new JPanel();
				panel.setLayout( new SpringLayout());
				panel.add( dotLabel);
				panel.add( _comboBox);
				panel.add( fromLabel);
				panel.add( _fromTextField);
				panel.add( toLabel);
				panel.add( _toTextField);
				SpringUtilities.makeCompactGrid(panel, 2, 20, 5);
				
				EditableListenButton saveButton = new EditableListenButton("Daten übernehmen");
				JButton cancelButton = new JButton("Dialog schließen");
				
				JPanel buttonsPanel = new JPanel();
				buttonsPanel.setLayout(new SpringLayout());
				
				buttonsPanel.add(saveButton);
				buttonsPanel.add(cancelButton);
				
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
				SpringUtilities.makeCompactGrid(buttonsPanel, 2, 20, 5);
				addButtonListeners( saveButton, cancelButton);
				
				setTitle("GND: Darstellungsfestlegung eines Layers");
				setLayout(new BorderLayout());
				add( panel, BorderLayout.NORTH);
				add( buttonsPanel, BorderLayout.SOUTH);
				pack();
				setMinimumSize( new Dimension( 480, 160));
				final Rectangle bounds = getBounds();
				setLocation(new Point((int)(bounds.getHeight() / 1.5 + 720), (int)(bounds.getWidth() / 1.5)));
			}
			
			public void runDialog() {
				setVisible( true);
			}
			
			private void addButtonListeners(JButton saveButton, JButton cancelButton) {
				ActionListener actionListenerSave = new ActionListener() {
					
					public void actionPerformed(ActionEvent e) {
						final Object item = _comboBox.getSelectedItem();
						if ( item != null && (item instanceof String)) {
							DisplayObjectType type = DOTManager.getInstance().getDisplayObjectType((String) item);
							int lowerScale = Integer.valueOf(_fromTextField.getText()).intValue();
							int upperScale = Integer.valueOf(_toTextField.getText()).intValue();
							if ( lowerScale < upperScale) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Der Von-Wert darf nicht kleiner als der Bis-Wert sein!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
							_scratchLayer.getDotCollection().addDisplayObjectType(type, lowerScale, upperScale);
						}
					}
				};
				saveButton.addActionListener(actionListenerSave);
				
				ActionListener actionListenerCancel = new ActionListener() {
					
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				};
				cancelButton.addActionListener(actionListenerCancel);
			}
		}
		
		ActionListener actionListenerNewDOT = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				DOTDialog dotDialog = new DOTDialog();
				dotDialog.runDialog();
			}
		};
		_newDOTButton.addActionListener(actionListenerNewDOT);
		
		ActionListener actionListenerDeleteDOT = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				final int[] selectedRows = _dotTable.getSelectedRows();
				if ( selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie mindestens eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				for ( int i = selectedRows.length -1; i >= 0; i--) {
					final int selectedRow = selectedRows[i];
					final TableModel model = _dotTable.getModel();
					DOTCollection dotCollection = (DOTCollection) model;
					final String dotName = (String) dotCollection.getValueAt( selectedRow, 0);
					DisplayObjectType type = DOTManager.getInstance().getDisplayObjectType( dotName);
					if ( type != null) {
						int lowerScale = (Integer) dotCollection.getValueAt( selectedRow, 1);
						int upperScale = (Integer) dotCollection.getValueAt( selectedRow, 2);
						_scratchLayer.getDotCollection().removeDisplayObjectType(type, lowerScale, upperScale);
					}
				}
			}
		};
		_deleteDOTButton.addActionListener(actionListenerDeleteDOT);
	}
	
	private void addButtonListener( final JButton saveButton, 
			final JButton cancelButton, final JButton helpButton) {
		
		ActionListener actionListenerSave = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveLayer();
			}
		};
		saveButton.addActionListener(actionListenerSave);
		
		ActionListener actionListenerCancel = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( _editable && _somethingChanged) {
					if ( !askForSaveChanges()) {	// Es wird nicht gespeichert: Änderungen rückgängig machen!
						setLayer(_unchangableOriginalLayer.getCopy(), _editable, _nameChangable);
					}
				}
				setVisible( false);
				dispose();
			}
		};
		cancelButton.addActionListener(actionListenerCancel);
		
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#layerDefinitionDialog");
			}
		};
		helpButton.addActionListener(actionListenerHelp);
	}
	
	private void saveLayer () {
		if ( !_editable) {	// Sollte nie passieren, da der Button disabled ist.
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Dieser Layer ist nicht veränderbar!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String layerName = _nameTextField.getText();
		if ( layerName.length() == 0) {
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Bitte geben Sie einen Namen ein!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String infoText = _infoTextField.getText();
		if ( infoText.isEmpty()) {
			infoText = null;
		}
		final DafConfigurationObjectType selectedItem = 
			(DafConfigurationObjectType) _geoReferenceObjectTypesComboBox.getSelectedItem();
		if ( selectedItem == null) {
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Bitte wählen Sie einen Typ aus!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		final DOTCollection dotCollection = (DOTCollection) _dotTable.getModel();
		if ( dotCollection.isEmpty()) {
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Bitte geben Sie mindestens einen Darstellungstypen an!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		for ( DisplayObjectType dot : dotCollection.values()) {
			final DisplayObjectTypePlugin dotPlugin = dot.getDisplayObjectTypePlugin();
			if ( !dotPlugin.isSystemObjectTypeSupported(_configuration, selectedItem)) {
				JOptionPane.showMessageDialog(
						new JFrame(),
						"Der Darstellungstyp '" + dot.getName() + "' passt nicht zu dem Geometrietyp '" + 
						selectedItem.getName() + "'!",
						"Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		final List<AttributeGroup> attributeGroups = selectedItem.getAttributeGroups();
		for ( DisplayObjectType dot : dotCollection.values()) {
			final Set<DOTSubscriptionData> subscriptionData = dot.getSubscriptionData();
			for ( DOTSubscriptionData singleSubscriptionData : subscriptionData) {
				final String attributeGroupPID = singleSubscriptionData.getAttributeGroup();
				final AttributeGroup attributeGroup = _configuration.getAttributeGroup(attributeGroupPID);
				if ( !attributeGroups.contains( attributeGroup)) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Die Objektgruppe '" + selectedItem.getName() + "' kann nicht mit der Attributgruppe '" +
							attributeGroupPID + "' des Darstellungstyps '" + dot.getName() + "' verwendet werden.",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		final Layer existingLayer = LayerManager.getInstance().getLayer( layerName);
		if ( existingLayer != null) {
			if ((LayerManager.getInstance().isChangeable(existingLayer))) {
				Object[] options = {"Layer überschreiben", "Speichern abbrechen"};
				int n = JOptionPane.showOptionDialog(
						new JFrame(),
						"Soll der bestehende Layer mit diesem Namen wirklich überschrieben werden?",
						"Layer speichern",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[1]);
				if ( n != 0) {
					return;
				}
			} else  {
				JOptionPane.showMessageDialog(
						new JFrame(),
						"Der bestehende Layer darf nicht überschrieben werden!",
						"Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		Layer newLayer = new Layer( layerName, infoText, selectedItem.getPid());
		
		newLayer.setDotCollection(dotCollection);

		if ( existingLayer != null ) {
			LayerManager.getInstance().changeLayer( newLayer);
		} else {
			LayerManager.getInstance().addLayer(newLayer);
		}
		_scratchLayer = newLayer;
		_unchangableOriginalLayer = _scratchLayer.getCopy();
		_somethingChanged = false;
	}
	
	private void addFrameListener() {
		class FrameListener extends WindowAdapter {
			@Override
            public void windowClosing(WindowEvent e) {
				if ( _somethingChanged && _editable) {
					askForSaveChanges();
				}
			}
		}
		addWindowListener( new FrameListener());
	}
	
	private boolean askForSaveChanges() {
		Object[] options = {"Änderungen speichern", "Nicht speichern"};
		int n = JOptionPane.showOptionDialog(
				new JFrame(),
				"Änderungen speichern?",
				"Es wurden Änderungen an dem Layer vorgenommen.",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[1]);
		if ( n == 0) {
			saveLayer();
			setVisible( false);
			dispose();
			return true;
		}
		return false;
	}
	
	private void addListSelectionListener() {
		final ListSelectionModel selectionModel = _dotTable.getSelectionModel();
		// vordefinierte Darstellungsobjekttypen dürfen nicht bearbeitet oder gelöscht werden
		ListSelectionListener listSelectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = _dotTable.getSelectedRow();
				if ( selectedRow == -1) {
					_deleteDOTButton.setEnabled( false);
				} else {
					_deleteDOTButton.setEnabled( _editable);
				}
            }
		};
		selectionModel.addListSelectionListener( listSelectionListener);
	}
}
