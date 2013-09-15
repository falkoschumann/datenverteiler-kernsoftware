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
package de.kappich.pat.gnd.displayObjectToolkit;

import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.pluginInterfaces.DOTDefinitionDialog;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.pointPlugin.DOTPoint;
import de.kappich.pat.gnd.pointPlugin.DOTPointPainter;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveForm;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveFormType;
import de.kappich.pat.gnd.utils.SpringUtilities;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;

/**
 * Der äußere Rahmen aller Dialoge zur Darstellungstypendefinition. Dieser Dialog liefert ein Fenster,
 * in dem schon die wesentlichen Teile zur Darstellungstypdefinition enthalten sind. Die 
 * plugin-spezifischen Panels werden von den Implementationen von {@link DOTDefinitionDialog} geliefert.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8094 $
 *
 */
@SuppressWarnings("serial")
public class DOTDefinitionDialogFrame extends JFrame {
	
	final private DOTManagerDialog _dotManagerDialog;
	
	final private ClientDavInterface _connection;
	
	final private DOTManager _dotManager;
	
	private DisplayObjectType _displayObjectType;
	
	private DisplayObjectType _scratchDisplayObjectType;
	// Eine tiefe Kopie des _displayObjectType, in der alle für die Dialoge 
	// notwendigen Änderungen eingetragen werden, z.B. die Statisch/Dynamisch-
	// Informationen oder die TableModels.
	// Würde man dies im Original machen, so wäre der aktuelle Stand immer
	// auch der nach dem Drücken des (Gesamt-)Speichern-Knopfs.
	// Das (Gesamt-)Speichern wird (noch) nicht auf Basis dieser Kopie,
	// sondern auf Basis der sichtbaren Einträge u.a. in den Sub-Componenten
	// durchgeführt.
	
	private boolean _editable;
	
	private boolean _reviseOnly;
	
	final private JTextField _nameTextField = new JTextField();
	
	final private JTextField _infoTextField = new JTextField();
	
	final private JComboBox _primitiveFormComboBox = new JComboBox();
	
	final private JTextField _primitiveFormTypeTextField = new JTextField();
	
	final private JTextField _primitiveFormInfoTextField = new JTextField();
	
	final private JTextField _positionX = new JTextField();
	
	final private JTextField _positionY = new JTextField();
	
	final private JButton _editPrimitiveFormButton = new JButton("Bearbeiten");
	
	final private JButton _copyPrimitiveFormButton = new JButton("Kopieren");
	
	final private JButton _newPrimitiveFormButton = new JButton("Neu");
	
	final private JButton _deletePrimitiveFormButton = new JButton("Löschen");
	
	final private JComboBox _propertyComboBox = new JComboBox();
	
	final private JCheckBox _staticCheckBox = new JCheckBox("");
	
	private boolean _somethingChanged = false;
	
	final private JButton _saveButton = new JButton("Darstellungstyp speichern");
	
	private DOTDefinitionDialog _dotDefinitionDialog;
	
	private JPanel _centerPanel = null;
	
	private Dimension _frameSize = new Dimension(950, 600);
	
	private static Debug _debug = Debug.getLogger();
	
	// Und nun alles für die speziellen Informationen:
	
	private JPanel _specialInformationDefinitionPanel = new JPanel();
	
	private JPanel _specialInformationPanel = null;
	
	final private JPanel _specialInformationRectangle = new JPanel();
	final private JTextField _siHeight = new JTextField();
	final private JTextField _siWidth = new JTextField();
	
	final private JPanel _specialInformationCircle = new JPanel();
	final private JTextField _siRadius = new JTextField();
	
	final private JPanel _specialInformationSemicircle = new JPanel();
	final private JTextField _siSemiRadius = new JTextField();
	final private JTextField _siOrientation = new JTextField();
	
	final private JPanel _specialInformationTextdarstellung = new JPanel();
	
	final private JPanel _specialInformationPoint = new JPanel();
	
	/**
	 * Konstruktor für ein vollkommen leeres Objekt.
	 */
	
	public DOTDefinitionDialogFrame() {
		_dotManagerDialog = null;
		_connection = null;
		_dotManager = null;
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	/**
	 * Konstruktor für ein funktionsfähiges Objekt.
	 * 
	 * @param dotManagerDialog ein Dialog der Darstellungstypen-Verwaltung
	 * @param connection die Datenverteiler-Verbindung
	 * @param dotManager die Darstellungstypen-Verwaltung
	 * @param displayObjectType ein Darstellungstyp
	 * @param editable ist der Darstellungstyp veränderbar
	 * @param reviseOnly ist die Identität des Darstellungstyps unveränderlich 
	 * @param title die anzuzeigende Titelzeile
	 */
	public DOTDefinitionDialogFrame(
			final DOTManagerDialog dotManagerDialog,
			final ClientDavInterface connection,
			final DOTManager dotManager, 
			final DisplayObjectType displayObjectType, 
			final boolean editable,
			final boolean reviseOnly,
			final String title) {
		super( title);
		_dotManagerDialog = dotManagerDialog;
		_connection = connection;
		if ( dotManager == null) {
			_debug.warning("Ein DOTDefinitionDialogFrame kann nicht mit einem Null-DOTManager konstruiert werden.");
			dispose();
		}
		_dotManager = dotManager;
		if ( displayObjectType == null) {
			_debug.warning("Ein DOTDefinitionDialog kann nicht mit einem Null-Darstellungstyp konstruiert werden.");
			dispose();
		}
		_displayObjectType = displayObjectType;
		_scratchDisplayObjectType = _displayObjectType.getCopy( null);
		_editable = editable;
		_reviseOnly = reviseOnly;
		
		_staticCheckBox.setEnabled(_editable);
		
		addToolTips();	// Dies muss schon recht frühzeitig geschehen, weil die hier festgelegten Tooltipps auch
						// von dem Dialog, der neue Grundfiguren anlegt (PrimitiveFormDialog) verwendet werden.
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());
		
		Dimension labelSize = new Dimension(100, 20);
		
		final DisplayObjectTypePlugin displayObjectTypePlugin = _displayObjectType.getDisplayObjectTypePlugin();
		_dotDefinitionDialog = displayObjectTypePlugin.getDialog( this);
		
		// Oberer Teil mit Name, Info und ggbnflls dem AdditionalCgharacteristicsPanel
		
		JLabel nameLabel = new JLabel("Name: ");
		nameLabel.setPreferredSize( labelSize);
		_nameTextField.setText( _displayObjectType.getName());
		_nameTextField.setEditable( !_reviseOnly && _editable);
		
		ActionListener nameActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_scratchDisplayObjectType.setName( _nameTextField.getText());
				_somethingChanged = true;
			}
		};
		_nameTextField.addActionListener( nameActionListener);
		
		JLabel infoLabel = new JLabel("Info: ");
		infoLabel.setPreferredSize( labelSize);
		_infoTextField.setText( _displayObjectType.getInfo());
		_infoTextField.setEditable( _editable);
		
		ActionListener infoActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_scratchDisplayObjectType.setInfo( _infoTextField.getText());
				_somethingChanged = true;
			}
		};
		_infoTextField.addActionListener( infoActionListener);
		
		JPanel upperPanelUpperUpperPart = new JPanel();
		upperPanelUpperUpperPart.setLayout(new SpringLayout());
		upperPanelUpperUpperPart.add(nameLabel);
		upperPanelUpperUpperPart.add(_nameTextField);
		upperPanelUpperUpperPart.add(infoLabel);
		upperPanelUpperUpperPart.add(_infoTextField);
		upperPanelUpperUpperPart.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
		SpringUtilities.makeCompactGrid(upperPanelUpperUpperPart, 2, 5, 5);
		
		JPanel upperPanelUpperLowerPart = _dotDefinitionDialog.getAdditionalCharacteristicsPanel( 
				_displayObjectType);
		
		JPanel upperPanelLowerUpperPart = getPrimitiveFormPanel();
		
		JPanel upperPanelLowerLowerPart = getComboBoxPanel();
			
		JPanel upperPanelUpperPart = new JPanel();
		upperPanelUpperPart.setLayout(new BoxLayout( upperPanelUpperPart, BoxLayout.Y_AXIS));
		upperPanelUpperPart.setBorder( BorderFactory.createTitledBorder( "Übergreifend"));
		upperPanelUpperPart.add( upperPanelUpperUpperPart);
		if ( upperPanelUpperLowerPart != null) {
			upperPanelUpperPart.add( upperPanelUpperLowerPart);
		}
		
		JPanel upperPanelLowerPart = new JPanel();
		upperPanelLowerPart.setLayout(new BoxLayout( upperPanelLowerPart, BoxLayout.Y_AXIS));
		if ( _displayObjectType.getDisplayObjectTypePlugin().getPrimitiveFormTypes().length > 0) {
			upperPanelLowerPart.add( upperPanelLowerUpperPart);
		}
		upperPanelLowerPart.add( upperPanelLowerLowerPart);
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new BoxLayout( upperPanel, BoxLayout.Y_AXIS));
		upperPanel.add( upperPanelUpperPart);
		upperPanel.add( upperPanelLowerPart);
		
		add( upperPanel, BorderLayout.NORTH);
		
		// Der Mittelteil
		
		_centerPanel = _dotDefinitionDialog.getDOTItemDefinitionPanel();
		if ( _centerPanel != null) {
			add( _centerPanel, BorderLayout.CENTER);
		}
		
		// Unterer Teil mit Buttons
		
		ActionListener saveActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_dotDefinitionDialog.saveDisplayObjectType();
			}
		};
		_saveButton.addActionListener( saveActionListener);
		
		JButton cancelButton = new JButton("Dialog schließen");
		
		ActionListener cancelActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( _somethingChanged && _saveButton.isEnabled()) {
					Object[] options = {"Ja", "Nein"};
					int n = JOptionPane.showOptionDialog(
							new JFrame(),
							"Wollen Sie den Dialog wirklich ohne zu Speichern beenden?",
							"Es wurden Änderungen vorgenommen.",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);
					if ( (n == 1) || (n == -1) ) {
						return;
					}
				}
				if ( _dotManagerDialog != null) {
					_dotManagerDialog.dialogDisposed( DOTDefinitionDialogFrame.this);
				}
				dispose();
			}
		};
		cancelButton.addActionListener( cancelActionListener);
		
		class MyWindowListener extends WindowAdapter {
			
			public MyWindowListener() {
				super();
			}
			
			@Override
            public void windowClosing( WindowEvent e) {
				if ( _somethingChanged && _saveButton.isEnabled()) {
					Object[] options = {"Ja", "Nein"};
					int n = JOptionPane.showOptionDialog(
							new JFrame(),
							"Wollen Sie den Dialog wirklich ohne zu Speichern beenden?",
							"Es wurden Änderungen vorgenommen.",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);
					if ( n != 0) {
						return;
					} else {
						setVisible(false);
						dispose();
					}
				} else {
					setVisible(false);
					dispose();
				}
			}
		}
		
		MyWindowListener myWindowListener = new MyWindowListener();
		this.addWindowListener( myWindowListener);
		
		JButton helpButton = new JButton("Hilfe");
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#dotDefinitionDialog");
			}
		};
		helpButton.addActionListener( actionListenerHelp);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new SpringLayout());
		
		buttonsPanel.add(_saveButton);
		buttonsPanel.add(cancelButton);
		buttonsPanel.add(helpButton);
		
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(buttonsPanel, 3, 20, 5);
		add(buttonsPanel, BorderLayout.SOUTH);
		
		setEditable(_editable);

		final Rectangle bounds = getBounds();
		setLocation(new Point((int)(bounds.getHeight() / 1.5 + 660), (int)(bounds.getWidth() / 1.5)));
		pack();
		setSize(_frameSize);
		setVisible(true);
	}
	
	/**
	 * Setzt den Darstellungstypen des Dialogs. Mit der boolschen Variable wird angegeben, ob der
	 * Dialog veränderbar ist oder nicht.
	 * 
	 * @param displayObjectType ein Darstellungstyp
	 * @param editable ist der Darstellungstyp veränderbar
	 */
	public void setDisplayObjectType(DisplayObjectType displayObjectType, boolean editable) {
		if ( _dotManagerDialog != null) {
			_dotManagerDialog.dialogChanged( _displayObjectType, displayObjectType);
		}
		_displayObjectType = displayObjectType;
		_scratchDisplayObjectType = _displayObjectType.getCopy( null);
		_editable = editable;
		_nameTextField.setText( _scratchDisplayObjectType.getName());
		_infoTextField.setText( _scratchDisplayObjectType.getInfo());
		_somethingChanged = false;
	}
	
	/**
	 * Gibt den Darstellungstypen, mit dem der Dialog initialisiert wurde oder der mit
	 * setDisplayObjectType() zuletzt gesetzt wurde, zurück.
	 * 
	 * @return der Darstellungstyp
	 */
	public DisplayObjectType getDisplayObjectType() {
		return _displayObjectType;
	}
	
	
	/**
	 * Gibt den Darstellungstypen des Dialogs zurück, der auch alle vom Benutzer seit der
	 * Initialisierung des Dialogs bzw. dem letzten Aufruf von setDisplayObjectType() gemachten
	 * Änderungen enthält.
	 * 
	 * @return der vom Benutzer veränderbare Darstellungstyp
     */
    public DisplayObjectType getScratchDisplayObjectType() {
    	return _scratchDisplayObjectType;
    }

	/**
	 * Legt fest, ob der angezeigte Darstellungstyp veränderbar ist oder nicht.
	 * 
	 * @param editable der neue Wert
	 */
	public void setEditable(boolean editable) {
		_editable = editable;
		_saveButton.setEnabled( _editable);
	}
	
	private String getAsString( final Double x, final int precision) {
		boolean negative = (x<0.);
		final Double y;
		if ( negative) {
			y = -x * precision + 0.000000001;
		} else {
			y = x * precision + 0.000000001;
		}
		Integer i = y.intValue();
		Integer j = i / precision;
		Integer k = i % precision;
		String s;
		if ( negative) {
			s = '-' + j.toString() + "," + k.toString();
		} else {
			s = j.toString() + "," + k.toString();
		}
		return s;
	}
	
	private JPanel getPrimitiveFormPanel () {
		_primitiveFormTypeTextField.setEditable( false);
		_primitiveFormInfoTextField.setEditable( false);
		_positionX.setEditable( false);
		_positionX.setPreferredSize( new Dimension(50, 30));
		_positionY.setEditable( false);
		_positionY.setPreferredSize( new Dimension(50, 30));
		
		final ItemListener primitiveFormListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED) {
					String primitiveFormName = (String) _primitiveFormComboBox.getSelectedItem();
					_primitiveFormTypeTextField.setText(_scratchDisplayObjectType.getPrimitiveFormType(primitiveFormName));
					_primitiveFormInfoTextField.setText(_scratchDisplayObjectType.getPrimitiveFormInfo(primitiveFormName));
					DOTPoint dotPoint;
					try {
						dotPoint = (DOTPoint) _scratchDisplayObjectType; // Böse! Muss sauberer werden.
					} catch ( ClassCastException ex) {
						_debug.error("DOTDefinitionDialogFrame: falsches Plugin.");
						return;
					}
					PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm(primitiveFormName);
					final Double x = primitiveForm.getTranslation().getX();
					_positionX.setText( getAsString( x, 10));
					final Double y = primitiveForm.getTranslation().getY();
					_positionY.setText( getAsString( y, 10));
					final DisplayObjectTypePlugin pluginObject = getPluginObject();
					updateSpecialInformationPanel();
					_editPrimitiveFormButton.setEnabled( _editable);
					_copyPrimitiveFormButton.setEnabled( _editable);
					_deletePrimitiveFormButton.setEnabled( _editable);
					_staticCheckBox.setEnabled( _editable);
					updatePropertyComboBox( pluginObject);
					updateStaticCheckBox();
					updateCenterPanel();
				}
			}
		};
		_primitiveFormComboBox.addItemListener( primitiveFormListener);
		
		updatePrimitiveFormData();
		
		_primitiveFormComboBox.setBorder( BorderFactory.createTitledBorder( "Name"));
		_primitiveFormTypeTextField.setBorder( BorderFactory.createTitledBorder( "Typ"));
		_primitiveFormInfoTextField.setBorder( BorderFactory.createTitledBorder( "Info"));
		
		JPanel postionPanel = getPositionPanel();
		
		// Nun die obere Zeile:
		JPanel theUpperPanel = new JPanel();
		theUpperPanel.setLayout( new SpringLayout());
		theUpperPanel.add( _primitiveFormComboBox);
		theUpperPanel.add( _primitiveFormTypeTextField);
		theUpperPanel.add( _primitiveFormInfoTextField);
		theUpperPanel.add( postionPanel);
		SpringUtilities.makeCompactGrid(theUpperPanel, 4, 0, 5);
		
		initSpecialInformationPanel();
		updateSpecialInformationPanel();
		JPanel buttonsPanel = getButtonsPanel();

		// Nun die untere Zeile:
		JPanel theLowerPanel = new JPanel();
		theLowerPanel.setLayout( new SpringLayout());
		_specialInformationDefinitionPanel.setBorder( BorderFactory.createTitledBorder( "Definition"));
		if ( _specialInformationPanel != null) {	// das Panel kann null sein
			_specialInformationDefinitionPanel.add( _specialInformationPanel);
		}
		theLowerPanel.add( _specialInformationDefinitionPanel);
		theLowerPanel.add( buttonsPanel);
		SpringUtilities.makeCompactGrid(theLowerPanel, 2, 0, 5);
		
		JPanel thePanel = new JPanel();
		thePanel.setLayout( new BoxLayout( thePanel, BoxLayout.Y_AXIS));
		thePanel.setBorder( BorderFactory.createTitledBorder( "Grundfigur"));
		thePanel.add( theUpperPanel);
		thePanel.add( theLowerPanel);
		
		addEditCopyNewDeleteListeners();
		return thePanel;
	}
	
	private JPanel getPositionPanel() {
		JPanel positionPanel = new JPanel();
		JLabel xLabel = new JLabel( "x: ");
		JLabel yLabel = new JLabel( "y: ");
		positionPanel.add( xLabel);
		positionPanel.add( _positionX);
		positionPanel.add( yLabel);
		positionPanel.add( _positionY);
		positionPanel.setBorder( BorderFactory.createTitledBorder( "Position"));
		return positionPanel;
	}
	
	private void initSpecialInformationPanel () {
		// Für ein Recteck
		JLabel heightLabel = new JLabel( "Höhe: ");
		JLabel widthLabel = new JLabel( "Breite: ");
		
		_specialInformationRectangle.setLayout( new SpringLayout());
		_specialInformationRectangle.add( heightLabel);
		_siHeight.setEditable( false);
		_siHeight.setPreferredSize( new Dimension(60, 30));
		_specialInformationRectangle.add( _siHeight);
		_specialInformationRectangle.add( widthLabel);
		_siWidth.setEditable( false);
		_siWidth.setPreferredSize( new Dimension(60, 30));
		_specialInformationRectangle.add( _siWidth);
		SpringUtilities.makeCompactGrid(_specialInformationRectangle, 4, 5, 5);
		
		// Für einen Kreis
		JLabel radiusLabel = new JLabel( "Radius: ");
		
		_specialInformationCircle.setLayout( new SpringLayout());
		_specialInformationCircle.add( radiusLabel);
		_siRadius.setEditable( false);
		_siRadius.setPreferredSize( new Dimension(60, 30));
		_specialInformationCircle.add( _siRadius);
		SpringUtilities.makeCompactGrid(_specialInformationCircle, 2, 5, 5);
		
		// Für einen Halbkreis
		JLabel semiRadiusLabel = new JLabel( "Radius: ");
		JLabel orientationLabel = new JLabel( "Orientierung: ");
		
		_specialInformationSemicircle.setLayout( new SpringLayout());
		_specialInformationSemicircle.add( semiRadiusLabel);
		_siSemiRadius.setEditable( false);
		_siSemiRadius.setPreferredSize( new Dimension(60, 30));
		_specialInformationSemicircle.add( _siSemiRadius);
		_specialInformationSemicircle.add( orientationLabel);
		_siOrientation.setEditable( false);
		_siOrientation.setPreferredSize( new Dimension(200,30));
		_specialInformationSemicircle.add( _siOrientation);
		SpringUtilities.makeCompactGrid(_specialInformationSemicircle, 4, 5, 5);
		
		// Für einen Text: im Moment nichts
		
		// Für einen Punkt: im Moment nichts
	}
	
	private JPanel getButtonsPanel() {
		JPanel buttonsPanel = new JPanel();
		boolean enableAllOperations;
		final DisplayObjectTypePlugin plugin = _scratchDisplayObjectType.getDisplayObjectTypePlugin();
		if ( plugin.getName().equals( "Punkt")) {
			enableAllOperations = (_primitiveFormComboBox.getModel().getSize() != 0) && _editable;
		} else {
			enableAllOperations = _editable;
		}
		_editPrimitiveFormButton.setEnabled( enableAllOperations);
		_copyPrimitiveFormButton.setEnabled( enableAllOperations);
		_newPrimitiveFormButton.setEnabled( _editable);
		_deletePrimitiveFormButton.setEnabled( enableAllOperations);
		_staticCheckBox.setEnabled( enableAllOperations);
		buttonsPanel.add( _editPrimitiveFormButton);
		buttonsPanel.add( _copyPrimitiveFormButton);
		buttonsPanel.add( Box.createRigidArea( new Dimension(20,20)));
		buttonsPanel.add( _newPrimitiveFormButton);
		buttonsPanel.add( _deletePrimitiveFormButton);
		buttonsPanel.setBorder( BorderFactory.createTitledBorder( "Operationen"));
		return buttonsPanel;
	}
	
	private void addEditCopyNewDeleteListeners() {
		
		class PrimitiveFormDialog {
			final Dimension _dialogSize = new Dimension(680, 240);
			final JDialog _newPrimitiveFormDialog = new JDialog( DOTDefinitionDialogFrame.this, true);
			final JTextField _newPrimitiveFormNameTextField = new JTextField();
			final JComboBox _newPrimitiveFormTypeComboBox = new JComboBox();
			final JTextField _infoOfPrimitiveFormNameTextField = new JTextField();
			final JSpinner _positionXSpinner = new JSpinner( new SpinnerNumberModel(0., -10000.0, 10000., 0.1));
			final JSpinner _positionYSpinner = new JSpinner( new SpinnerNumberModel(0., -10000.0, 10000., 0.1));
			final JButton _savePrimitiveFormButton = new JButton( "Speichern");
			final JButton _leaveButton = new JButton( "Dialog beenden");
			
			// Und nun alles für das variable Panel:
			final private JPanel _pfdDefinitionPanel = new JPanel();
			private JPanel _pfdSpecialInformationPanel = new JPanel();
			final private JPanel _pfdSpecialInformationRectangle = new JPanel();
			final private JSpinner _pfdSiHeight = new JSpinner( new SpinnerNumberModel(5., 0.0, 10000., 0.1));
			final private JSpinner _pfdSiWidth = new JSpinner( new SpinnerNumberModel(5., 0.0, 10000., 0.1));
			final private JPanel _pfdSpecialInformationCircle = new JPanel();
			final private JSpinner _pfdSiRadius = new JSpinner( new SpinnerNumberModel(5., 0.0, 10000., 0.1));
			final private JPanel _pfdSpecialInformationSemicircle = new JPanel();
			final private JSpinner _pfdSiSemiRadius = new JSpinner( new SpinnerNumberModel(5., 0.0, 10000., 0.1));
			final private JComboBox _pfdSiOrientation = new JComboBox();
			final private JPanel _pfdSpecialInformationText = new JPanel();
			final private JPanel _pfdSpecialInformationPoint = new JPanel();
			
			public String getName() {
				return _newPrimitiveFormNameTextField.getText();
			}
			
			public void setName( final String name) {
				_newPrimitiveFormNameTextField.setText( name);
			}
			
			public void disableNameField() {
				_newPrimitiveFormNameTextField.setEditable( false);
			}
			
			public String getType() {
				return (String) _newPrimitiveFormTypeComboBox.getSelectedItem();
			}
			
			public void setType ( final String type) {
				_newPrimitiveFormTypeComboBox.setSelectedItem( type);
			}
			
			public void disableTypeField() {
				_newPrimitiveFormTypeComboBox.setEnabled( false);
			}
			
			public String getInfo() {
				return _infoOfPrimitiveFormNameTextField.getText();
			}
			
			public void setInfo(final String info) {
				_infoOfPrimitiveFormNameTextField.setText( info);
			}
			
			public Double getX() {
				return (Double) _positionXSpinner.getValue();
			}
			
			public void setX( final Double x) {
				_positionXSpinner.setValue( x);
			}
			
			public Double getY() {
				return (Double) _positionYSpinner.getValue();
			}
			
			public void setY( final Double y) {
				_positionYSpinner.setValue( y);
			}
			
			public Map<String,Object> getSpecificInformation() {
				String type  = getType();
				Map<String,Object> specificInformation = new HashMap<String,Object>();
				if  ( type.equals( PrimitiveFormType.RECHTECK.getName())) {
					specificInformation.put( PrimitiveForm.HEIGHT, (Double) _pfdSiHeight.getValue());
					specificInformation.put( PrimitiveForm.WIDTH, (Double) _pfdSiWidth.getValue());
				} else if ( type.equals( PrimitiveFormType.KREIS.getName())) {
					specificInformation.put( PrimitiveForm.RADIUS, (Double) _pfdSiRadius.getValue());
				} else if ( type.equals( PrimitiveFormType.HALBKREIS.getName())) {
					specificInformation.put( PrimitiveForm.RADIUS, (Double) _pfdSiSemiRadius.getValue());
					specificInformation.put( PrimitiveForm.ORIENTATION, (String) _pfdSiOrientation.getSelectedItem());
				} else if ( type.equals( PrimitiveFormType.PUNKT.getName())) {
				} else if (type.equals( PrimitiveFormType.TEXTDARSTELLUNG.getName())) {
				} else {
					_debug.warning("PrimitiveFormDialog: unbekannter Typ '" + type + "'.");
				}
				return specificInformation;
			}
			
			public void dispose() {
				_newPrimitiveFormDialog.dispose();
			}
			
			public void setSize() {
				_newPrimitiveFormDialog.setSize( _dialogSize);
			}
			
			public void setVisible( boolean b) {
				_newPrimitiveFormDialog.setVisible( b);
			}
			
			public void pack() {
				_newPrimitiveFormDialog.pack();
			}
			
			public void setTextOfSaveButton( final String text) {
				_savePrimitiveFormButton.setText( text);
			}
			
			public void setListenerToSaveButton ( final ActionListener l) {
				final ActionListener[] actionListeners = _savePrimitiveFormButton.getActionListeners();
				for ( ActionListener actionListener : actionListeners) {
					_savePrimitiveFormButton.removeActionListener(actionListener);
				}
				_savePrimitiveFormButton.addActionListener( l);
			}
			
			public void storeNewPrimitiveForm() {
				final String newPrimitiveFormName = getName();
				if ( (newPrimitiveFormName == null) || (newPrimitiveFormName.length() == 0)) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte geben Sie einen Namen für die Grundfigur an!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String newPrimitiveFormType = getType();
				if ( newPrimitiveFormType == null) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie einen Typ aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String infoOfPrimitiveFormName = getInfo();
				
				DOTPoint dotPoint;
				try {
					dotPoint = (DOTPoint) _scratchDisplayObjectType;	// Das ist böse, ganz böse. Aber ich habe keine Zeit!
				} catch ( ClassCastException ex) {
					_debug.error("DOTDefinitionDialogFrame: falsches Plugin.");
					return;
				}
				PrimitiveForm primitiveForm = new PrimitiveForm( newPrimitiveFormName,
						PrimitiveFormType.getPrimitiveFormType( newPrimitiveFormType), 
						infoOfPrimitiveFormName,
						new Point2D.Double( getX(), getY()), 
						getSpecificInformation());
				
				if ( dotPoint.getPrimitiveForm( primitiveForm.getName()) != null) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Eine Grundfigur mit diesem Namen existiert bereits!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				dotPoint.addPrimitiveForm(primitiveForm);
				updatePrimitiveFormData();
				_primitiveFormComboBox.setSelectedItem( primitiveForm.getName());
				updatePropertyComboBox(_scratchDisplayObjectType.getDisplayObjectTypePlugin());
				updateStaticCheckBox();
				updateCenterPanel();
				dispose();
			}
			
			private void addToolTips() {
				_newPrimitiveFormNameTextField.setToolTipText( _nameTextField.getToolTipText());
				_newPrimitiveFormTypeComboBox.setToolTipText( _primitiveFormTypeTextField.getToolTipText());
				_infoOfPrimitiveFormNameTextField.setToolTipText( _primitiveFormInfoTextField.getToolTipText());
				_positionXSpinner.setToolTipText( _positionX.getToolTipText());
				_positionYSpinner.setToolTipText( _positionY.getToolTipText());
				_savePrimitiveFormButton.setToolTipText( "Die Grundfigur speichern");
				_leaveButton.setToolTipText( "Diesen Dialog ohne zu speichern verlassen.");
				_pfdDefinitionPanel.setToolTipText( _specialInformationDefinitionPanel.getToolTipText());
				_pfdSiHeight.setToolTipText( _siHeight.getToolTipText());
				_pfdSiWidth.setToolTipText( _siWidth.getToolTipText());
				_pfdSiRadius.setToolTipText( _siRadius.getToolTipText());
				_pfdSiSemiRadius.setToolTipText( _siSemiRadius.getToolTipText());
				_pfdSiOrientation.setToolTipText( _siOrientation.getToolTipText());
			}
			
			public void clearDialog() {
				_newPrimitiveFormNameTextField.setText("");
				if ( _newPrimitiveFormTypeComboBox.getModel().getSize()>0) {
					_newPrimitiveFormTypeComboBox.setSelectedIndex( 0);
				}
				_infoOfPrimitiveFormNameTextField.setText( "");
				_positionXSpinner.setValue( 0.);
				_positionYSpinner.setValue( 0.);
				_savePrimitiveFormButton.setText( "Speichern");
				_leaveButton.setText( "Dialog beenden");
				_pfdSiHeight.setValue( 5.);
				_pfdSiWidth.setValue( 5.);
				_pfdSiRadius.setValue( 5.);
				_pfdSiSemiRadius.setValue( 5.);
			}
			
			PrimitiveFormDialog( final String title) {
				_newPrimitiveFormNameTextField.setBorder( BorderFactory.createTitledBorder( "Name"));
				_newPrimitiveFormNameTextField.setPreferredSize( new Dimension(100, 30));
				_newPrimitiveFormTypeComboBox.setBorder( BorderFactory.createTitledBorder( "Typ"));
				_infoOfPrimitiveFormNameTextField.setBorder( BorderFactory.createTitledBorder( "Info"));
				_infoOfPrimitiveFormNameTextField.setPreferredSize( new Dimension(100, 30));
				_pfdDefinitionPanel.setBorder( BorderFactory.createTitledBorder( "Definition"));
				String[] pfTypes =_scratchDisplayObjectType.getDisplayObjectTypePlugin().getPrimitiveFormTypes();
				ComboBoxModel comboBoxModel = new DefaultComboBoxModel( pfTypes);
				_newPrimitiveFormTypeComboBox.setModel( comboBoxModel);
				final JPanel choicePanelUpperHalf = new JPanel();
				choicePanelUpperHalf.setLayout( new SpringLayout());
				choicePanelUpperHalf.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				choicePanelUpperHalf.add( _newPrimitiveFormNameTextField);
				choicePanelUpperHalf.add( _newPrimitiveFormTypeComboBox);
				choicePanelUpperHalf.add( _infoOfPrimitiveFormNameTextField);
				JPanel positionPanel = getPFDPositionPanel();
				choicePanelUpperHalf.add( positionPanel);
				SpringUtilities.makeCompactGrid( choicePanelUpperHalf, 4, 20, 5);
				
				final JPanel choicePanelLowerHalf = new JPanel();
				choicePanelLowerHalf.setLayout( new SpringLayout());
				choicePanelLowerHalf.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				_pfdDefinitionPanel.add( _pfdSpecialInformationPanel);
				initPFDSpecialInformationPanels();
				choicePanelLowerHalf.add( _pfdDefinitionPanel);
				_leaveButton.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				final JPanel buttonsInChoicePanelsLowerHalfPanel = new JPanel();
				buttonsInChoicePanelsLowerHalfPanel.setLayout( 
						new BoxLayout( buttonsInChoicePanelsLowerHalfPanel, BoxLayout.X_AXIS));
				buttonsInChoicePanelsLowerHalfPanel.setBorder( BorderFactory.createTitledBorder("Operationen"));
				buttonsInChoicePanelsLowerHalfPanel.add(_savePrimitiveFormButton);
				buttonsInChoicePanelsLowerHalfPanel.add( Box.createRigidArea( new Dimension(10,10)));
				buttonsInChoicePanelsLowerHalfPanel.add(_leaveButton);
				choicePanelLowerHalf.add( buttonsInChoicePanelsLowerHalfPanel);
				SpringUtilities.makeCompactGrid( choicePanelLowerHalf, 2, 20, 5);
				
				final JPanel allPanel = new JPanel();
				allPanel.setLayout( new BoxLayout( allPanel, BoxLayout.Y_AXIS));
				allPanel.add( choicePanelUpperHalf);
				allPanel.add( choicePanelLowerHalf);
				allPanel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
				
				_newPrimitiveFormDialog.setTitle( title);
				_newPrimitiveFormDialog.add( allPanel);
				_newPrimitiveFormDialog.setLocation( 900, 80);
				addToolTips();
			}
			
			private JPanel getPFDPositionPanel() {
				JPanel pfdPositionPanel = new JPanel();
				pfdPositionPanel.setBorder( BorderFactory.createTitledBorder( "Position"));
				pfdPositionPanel.setLayout( new BoxLayout(pfdPositionPanel, BoxLayout.X_AXIS));
				
				JLabel xLabel = new JLabel("x: ");
				pfdPositionPanel.add( xLabel);
				pfdPositionPanel.add( _positionXSpinner);
				JLabel yLabel = new JLabel("y: ");
				pfdPositionPanel.add( yLabel);
				pfdPositionPanel.add( _positionYSpinner);
				
				return pfdPositionPanel;
			}
			
			private void initPFDSpecialInformationPanels() {
				// Für ein Rechteck
				JLabel heightLabel = new JLabel( "Höhe: ");
				JLabel widthLabel = new JLabel( "Breite: ");
				_pfdSpecialInformationRectangle.setLayout( new SpringLayout());
				_pfdSpecialInformationRectangle.add( heightLabel);
				_pfdSpecialInformationRectangle.add( _pfdSiHeight);
				_pfdSpecialInformationRectangle.add( widthLabel);
				_pfdSpecialInformationRectangle.add( _pfdSiWidth);
				SpringUtilities.makeCompactGrid(_pfdSpecialInformationRectangle, 4, 5, 5);
				
				// Für einen Kreis
				JLabel radiusLabel = new JLabel( "Radius: ");
				_pfdSpecialInformationCircle.setLayout( new SpringLayout());
				_pfdSpecialInformationCircle.add( radiusLabel);
				_pfdSpecialInformationCircle.add( _pfdSiRadius);
				SpringUtilities.makeCompactGrid(_pfdSpecialInformationCircle, 2, 5, 5);
				
				// Für einen Halbkreis
				JLabel semiRadiusLabel = new JLabel( "Radius: ");
				JLabel orientationLabel = new JLabel( "Orientierung: ");
				_pfdSiOrientation.setModel( new DefaultComboBoxModel( SEMI_CIRCLE_ITEMS));
				_pfdSpecialInformationSemicircle.setLayout( new SpringLayout());
				_pfdSpecialInformationSemicircle.add( semiRadiusLabel);
				_pfdSpecialInformationSemicircle.add( _pfdSiSemiRadius);
				_pfdSpecialInformationSemicircle.add( orientationLabel);
				_pfdSpecialInformationSemicircle.add( _pfdSiOrientation);
				SpringUtilities.makeCompactGrid(_pfdSpecialInformationSemicircle, 4, 5, 5);
				
				// Für einen Text: derzeit nichts
				
				// Für einen Punkt: derzeit nichts
				
				// Und nun noch den Listener, der das Definitions-Panel aktualisiert
				ActionListener pfTypeListener = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String selectedType = (String) _newPrimitiveFormTypeComboBox.getSelectedItem();
						_pfdDefinitionPanel.removeAll();
						if ( selectedType.equals( "Punkt")) {
							_positionXSpinner.setValue( 0.);
							_positionYSpinner.setValue( 0.);
							_positionXSpinner.setEnabled( false);
							_positionYSpinner.setEnabled( false);
							_pfdSpecialInformationPanel = _pfdSpecialInformationPoint;
						} else {
							_positionXSpinner.setEnabled( true);
							_positionYSpinner.setEnabled( true);
							if ( selectedType.equals( "Rechteck")) {
								_pfdSpecialInformationPanel = _pfdSpecialInformationRectangle;
							} else if ( selectedType.equals( "Kreis")) {
								_pfdSpecialInformationPanel = _pfdSpecialInformationCircle;
							} else if ( selectedType.equals( "Halbkreis")) {
								_pfdSpecialInformationPanel = _pfdSpecialInformationSemicircle;
							} else if ( selectedType.equals( "Textdarstellung")) {
								_pfdSpecialInformationPanel = _pfdSpecialInformationText;
							} else {
								_pfdSpecialInformationPanel = new JPanel();
								_debug.warning( "PrimitiveFormDialog: unbekannter Typ '" + selectedType + "'.");
							}
						}
						_pfdDefinitionPanel.add( _pfdSpecialInformationPanel);
						pack();		// Diese beiden Anweisungen besorgen das 'ordentliche' Neuzeichnen;
						setSize();	// jedwede Anwebdung eines repaint blieb erfolglos.
					}
				};
				_newPrimitiveFormTypeComboBox.addActionListener( pfTypeListener);
			}
			
			private void updatePfdSpecialInformationPanel() {
				if ( !_scratchDisplayObjectType.getDisplayObjectTypePlugin().getName().equals( "Punkt")) {
					return;
				}
				DOTPoint dotPoint = (DOTPoint) _scratchDisplayObjectType; // Böse! Muss sauberer werden.
				final PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm( (String) _primitiveFormComboBox.getSelectedItem());
				final String type = (String) _newPrimitiveFormTypeComboBox.getSelectedItem();
				_pfdDefinitionPanel.removeAll();
				if ( type.equals( "Punkt")) {
					_positionXSpinner.setValue( 0.);
					_positionYSpinner.setValue( 0.);
					_positionXSpinner.setEnabled( false);
					_positionYSpinner.setEnabled( false);
					_pfdSpecialInformationPanel = _pfdSpecialInformationPoint;
				} else {
					_positionXSpinner.setEnabled( true);
					_positionYSpinner.setEnabled( true);
					if ( type.equals( PrimitiveFormType.RECHTECK.getName())) {
						final Double h = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.HEIGHT);
						_pfdSiHeight.setValue( h);
						final Double w = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.WIDTH);
						_pfdSiWidth.setValue( w);
						_pfdSpecialInformationPanel = _pfdSpecialInformationRectangle;
					} else if ( type.equals( PrimitiveFormType.KREIS.getName())) {
						final Double r = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.RADIUS);
						_pfdSiRadius.setValue( r);
						_pfdSpecialInformationPanel = _pfdSpecialInformationCircle;
					} else if ( type.equals( PrimitiveFormType.HALBKREIS.getName())) {
						final Double r = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.RADIUS);
						_pfdSiSemiRadius.setValue( r);
						final String o = (String) primitiveForm.getSpecificInformation( PrimitiveForm.ORIENTATION);
						_pfdSiOrientation.setSelectedItem( o);
						_pfdSpecialInformationPanel = _pfdSpecialInformationSemicircle;
					}
				}
				_pfdDefinitionPanel.add( _pfdSpecialInformationPanel);
			}
			
			public void saveSpecialInformation() {
				if ( !_scratchDisplayObjectType.getDisplayObjectTypePlugin().getName().equals( "Punkt")) {
					return;
				}
				DOTPoint dotPoint = (DOTPoint) _scratchDisplayObjectType; // Böse! Muss sauberer werden.
				final String primitiveFormName = _newPrimitiveFormNameTextField.getText();
				final PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm(primitiveFormName);
				final String type = (String) _newPrimitiveFormTypeComboBox.getSelectedItem();
				if ( type.equals( PrimitiveFormType.RECHTECK.getName())) {
					final Object height = _pfdSiHeight.getValue();
					primitiveForm.setSpecificInformation( PrimitiveForm.HEIGHT, (Double) height);
					_siHeight.setText( getAsString( (Double) height, 10));
					final Object width = _pfdSiWidth.getValue();
					primitiveForm.setSpecificInformation( PrimitiveForm.WIDTH, (Double) width);
					_siWidth.setText( getAsString( (Double) width, 10));
				} else if ( type.equals( PrimitiveFormType.KREIS.getName())) {
					final SpinnerModel model = _pfdSiRadius.getModel();
					final SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
					final Double correctSpinnerValue = correctSpinnerValue( _pfdSiRadius.getValue(), numberModel.getStepSize());
					primitiveForm.setSpecificInformation( PrimitiveForm.RADIUS, correctSpinnerValue);
					_siRadius.setText( correctSpinnerValue.toString().replace('.', ','));
				} else if ( type.equals( PrimitiveFormType.HALBKREIS.getName())) {
					final SpinnerModel model = _pfdSiSemiRadius.getModel();
					final SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
					final Double correctSpinnerValue = correctSpinnerValue( _pfdSiSemiRadius.getValue(), numberModel.getStepSize());
					primitiveForm.setSpecificInformation( PrimitiveForm.RADIUS, correctSpinnerValue);
					_siSemiRadius.setText( correctSpinnerValue.toString().replace('.', ','));
					final String orientation = (String) _pfdSiOrientation.getSelectedItem();
					primitiveForm.setSpecificInformation( PrimitiveForm.ORIENTATION, orientation);
					_siOrientation.setText( orientation);
				}
			}
			
			private Double correctSpinnerValue( final Object value, final Number stepSize) {
				final Double dValue = (Double) value;
				final double stepSizeDoubleValue = stepSize.doubleValue();
				if ( stepSizeDoubleValue>1.) {
					return dValue;
				}
				final Double reciprocalStepSize = 1./ stepSizeDoubleValue;
				final long reciprocalStepSizeLong = Math.round( reciprocalStepSize);
				final Double dValueIncreased = dValue * reciprocalStepSizeLong;
				final long iValueIncreased = Math.round( dValueIncreased);
				final double d = ((double) iValueIncreased) / reciprocalStepSizeLong;
				return d;
			}
			
		}
		
		ActionListener editPrimitiveFormListener = new ActionListener() {
			PrimitiveFormDialog pfDialog = new PrimitiveFormDialog( "GND: Bestehende Grundfigur bearbeiten");
			public void actionPerformed(ActionEvent e) {
				if ( !_scratchDisplayObjectType.getDisplayObjectTypePlugin().getName().equals( "Punkt")) {
					return;
				}
				pfDialog.setTextOfSaveButton( "Daten übernehmen");
				final Object selectedItem = _primitiveFormComboBox.getSelectedItem();
				if ( selectedItem == null) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Grundfigur aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String name = (String) selectedItem;
				if ( name.length() == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Grundfigur aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				pfDialog.setName( name);
				pfDialog.disableNameField();
				pfDialog.setType( _primitiveFormTypeTextField.getText());
				pfDialog.disableTypeField();
				pfDialog.setInfo( _primitiveFormInfoTextField.getText());
				Double xAsDouble = 0.;
				try {
					xAsDouble = Double.parseDouble( _positionX.getText().replace(',', '.'));
				} catch (Exception ex) {
					_debug.warning("editPrimitiveFormListener: Zeichenkette kann nicht in Zahl gewandelt werden", ex);
				}
				pfDialog.setX( xAsDouble);
				Double yAsDouble = 0.;
				try {
					yAsDouble = Double.parseDouble( _positionX.getText().replace(',', '.'));
				} catch (Exception ex) {
					_debug.warning("editPrimitiveFormListener: Zeichenkette kann nicht in Zahl gewandelt werden", ex);
				}
				pfDialog.setY( yAsDouble);
				pfDialog.updatePfdSpecialInformationPanel();
				ActionListener saveButtonListener = new ActionListener() {
					public void actionPerformed(ActionEvent f) {
						DOTPoint dotPoint = (DOTPoint) _scratchDisplayObjectType; // Das ist böse!
						final PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm((String)_primitiveFormComboBox.getSelectedItem());
						final String info = pfDialog.getInfo();
						primitiveForm.setInfo( info);
						_primitiveFormInfoTextField.setText(info);
						primitiveForm.setInfo(info);
						final Double x = pfDialog.getX();
						_positionX.setText( getAsString(x, 10));
						final Double y = pfDialog.getY();
						_positionY.setText( getAsString(y, 10));
						primitiveForm.setTranslation( new Point2D.Double(x, y));
						pfDialog.saveSpecialInformation();
						pfDialog.dispose();
					}
				};
				pfDialog.setListenerToSaveButton( saveButtonListener);
				
				pfDialog.pack();
				pfDialog.setSize();
				pfDialog.setVisible( true);
			}
		};
		_editPrimitiveFormButton.addActionListener( editPrimitiveFormListener);
		
		ActionListener copyPrimitiveFormListener = new ActionListener() {
			PrimitiveFormDialog pfDialog = new PrimitiveFormDialog( "GND: Kopierte Grundfigur bearbeiten");
			public void actionPerformed(ActionEvent e) {
				pfDialog.setTextOfSaveButton( "Grundfigur anlegen");
				final Object selectedItem = _primitiveFormComboBox.getSelectedItem();
				if ( selectedItem == null) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Grundfigur aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String name = (String) selectedItem;
				if ( name.length() == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Grundfigur aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				pfDialog.setName( name);
				pfDialog.setType( _primitiveFormTypeTextField.getText());
				pfDialog.setInfo( _primitiveFormInfoTextField.getText());
				pfDialog.setX( Double.parseDouble( _positionX.getText().replace(',', '.')));
				pfDialog.setY( Double.parseDouble( _positionY.getText().replace(',', '.')));
				pfDialog.updatePfdSpecialInformationPanel();
				ActionListener okayListener = new ActionListener() {
					public void actionPerformed(ActionEvent f) {
						pfDialog.storeNewPrimitiveForm();
					}
				};
				pfDialog.setListenerToSaveButton( okayListener);
				
				pfDialog.pack();
				pfDialog.setSize();
				pfDialog.setVisible( true);
			}
		};
		_copyPrimitiveFormButton.addActionListener( copyPrimitiveFormListener);
		
		
		ActionListener newPrimitiveFormListener = new ActionListener() {
			PrimitiveFormDialog pfDialog = new PrimitiveFormDialog( "GND: Neue Grundfigur anlegen");
			public void actionPerformed(ActionEvent e) {
				pfDialog.clearDialog();
				pfDialog.setTextOfSaveButton( "Grundfigur anlegen");
				ActionListener okayListener = new ActionListener() {
					public void actionPerformed(ActionEvent f) {
						JButton button = (JButton) f.getSource();
						if ( button.isValid()) {	
							// Ab dem 2ten Anlegen kam ein 2ter ActionEvent, der sich nur darin
							// unterschied, dass der Button invalid war. Kann sein, dass das nur
							// darauf zurückzuführen war, dass bei jeder äußeren Action wieder
							// ein neuer ActionListener hinzukam.
							pfDialog.storeNewPrimitiveForm();
							_editPrimitiveFormButton.setEnabled( _editable);
							_copyPrimitiveFormButton.setEnabled( _editable);
							_deletePrimitiveFormButton.setEnabled( _editable);
							_staticCheckBox.setEnabled( _editable);
						}
					}
				};
				pfDialog.setListenerToSaveButton( okayListener);
				
				pfDialog.pack();
				pfDialog.setSize();
				pfDialog.setVisible( true);
			}
		};
		_newPrimitiveFormButton.addActionListener( newPrimitiveFormListener);
		
		ActionListener deletePrimitiveFormListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String primitiveFormName = (String) _primitiveFormComboBox.getSelectedItem();
				if ( primitiveFormName != null) {
					_scratchDisplayObjectType.removePrimitiveForm( primitiveFormName);
					updatePrimitiveFormData();
					updatePropertyComboBox( _scratchDisplayObjectType.getDisplayObjectTypePlugin());
					updateStaticCheckBox();
					updateCenterPanel();
				}
			}
		};
		_deletePrimitiveFormButton.addActionListener( deletePrimitiveFormListener);
	}
	
	private JPanel getComboBoxPanel() {
		_propertyComboBox.setPreferredSize(  new Dimension( 200, 25));
		_staticCheckBox.setPreferredSize( new Dimension( 100, 25));
		
		// ToDo: Wie bekommt man das Folgende automatisch hin?
		final DisplayObjectTypePlugin displayObjectTypePlugin = _displayObjectType.getDisplayObjectTypePlugin();
		updatePropertyComboBox( displayObjectTypePlugin);
		updateStaticCheckBox();
		
		ItemListener propertyListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED) {
					updateStaticCheckBox();
					updateCenterPanel();
				}
			}
		};
		_propertyComboBox.addItemListener( propertyListener);
		
		ActionListener staticCheckBoxListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String primitiveFormName = (String) _primitiveFormComboBox.getSelectedItem();
				DOTProperty property = (DOTProperty) _propertyComboBox.getSelectedItem();
				if ( primitiveFormName == null) {
					_scratchDisplayObjectType.setPropertyStatic( null, property, _staticCheckBox.isSelected());
				} else {
					_scratchDisplayObjectType.setPropertyStatic( primitiveFormName, property, _staticCheckBox.isSelected());
				}
				updateCenterPanel();
			}
		};
		_staticCheckBox.addActionListener( staticCheckBoxListener);
		
		_propertyComboBox.setBorder( BorderFactory.createTitledBorder( "Eigenschaft"));

		JPanel buttonPanel = new JPanel();
		buttonPanel.add( _staticCheckBox);
		buttonPanel.setBorder( BorderFactory.createTitledBorder( "Statisch"));
		
		JPanel thePanel = new JPanel();
		thePanel.setLayout( new SpringLayout());
		thePanel.add( _propertyComboBox);
		thePanel.add( buttonPanel);
		thePanel.add( Box.createRigidArea( new Dimension(250,25)));
		SpringUtilities.makeCompactGrid(thePanel, thePanel.getComponentCount(), 10, 10);
		
		return thePanel;
	}
	
	private DisplayObjectTypePlugin getPluginObject() {
		if ( _displayObjectType != null) {
			return _displayObjectType.getDisplayObjectTypePlugin();
		}
		return null;
	}
	
	private void updatePrimitiveFormData() {
		if ( !_scratchDisplayObjectType.getDisplayObjectTypePlugin().getName().equals( "Punkt")) {
			return;
		}
		final Set<String> primitiveFormNames = _scratchDisplayObjectType.getPrimitiveFormNames();
		ComboBoxModel comboBoxModel = new DefaultComboBoxModel( primitiveFormNames.toArray());
		_primitiveFormComboBox.setModel(comboBoxModel);
		if ( comboBoxModel.getSize() > 0) {
			final String primitiveFormName = (String) comboBoxModel.getSelectedItem();
			_primitiveFormTypeTextField.setText( 
					_scratchDisplayObjectType.getPrimitiveFormType( primitiveFormName));
			_primitiveFormInfoTextField.setText( _scratchDisplayObjectType.getPrimitiveFormInfo(primitiveFormName));
			DOTPoint dotPoint = (DOTPoint) _scratchDisplayObjectType; // Böse! Muss sauberer werden.
			PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm(primitiveFormName);
			final Double x = primitiveForm.getTranslation().getX();
			_positionX.setText( getAsString(x, 10));
			final Double y = primitiveForm.getTranslation().getY();
			_positionY.setText( getAsString(y, 10));
		} else {
			_primitiveFormTypeTextField.setText( null);
			_primitiveFormInfoTextField.setText( null);
			_positionX.setText( null);
			_positionY.setText( null);
			_editPrimitiveFormButton.setEnabled( false);
			_copyPrimitiveFormButton.setEnabled( false);
			_deletePrimitiveFormButton.setEnabled( false);
			_staticCheckBox.setEnabled( false);
		}
		updateSpecialInformationPanel();
	}
	
	private void updateSpecialInformationPanel() {
		if ( !_scratchDisplayObjectType.getDisplayObjectTypePlugin().getName().equals( "Punkt")) {
			return;
		}
		DOTPoint dotPoint = (DOTPoint) _scratchDisplayObjectType; // Böse! Muss sauberer werden.
		final PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm( (String) _primitiveFormComboBox.getSelectedItem());
		final String type = _primitiveFormTypeTextField.getText();
		_specialInformationDefinitionPanel.removeAll();
		_specialInformationPanel = null;
		if ( type.equals( "Punkt")) {
			_specialInformationPanel = _specialInformationPoint;
		} else if ( type.equals( PrimitiveFormType.TEXTDARSTELLUNG.getName())) {
			_specialInformationPanel = _specialInformationTextdarstellung;
		} else if ( type.equals( PrimitiveFormType.RECHTECK.getName())) {
			final Double h = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.HEIGHT);
			_siHeight.setText( getAsString(h, 10));
			final Double w = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.WIDTH);
			_siWidth.setText( getAsString(w, 10));
			_specialInformationPanel = _specialInformationRectangle;
		} else if ( type.equals( PrimitiveFormType.KREIS.getName())) {
			final Double r = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.RADIUS);
			_siRadius.setText( r.toString().replace('.', ','));
			_specialInformationPanel = _specialInformationCircle;
		} else if ( type.equals( PrimitiveFormType.HALBKREIS.getName())) {
			final Double r = (Double) primitiveForm.getSpecificInformation( PrimitiveForm.RADIUS);
			_siSemiRadius.setText( r.toString().replace('.', ','));
			final String o = (String) primitiveForm.getSpecificInformation( PrimitiveForm.ORIENTATION);
			_siOrientation.setText(o);
			_specialInformationPanel = _specialInformationSemicircle;
		}
		if ( _specialInformationPanel != null) {
			_specialInformationDefinitionPanel.add( _specialInformationPanel);
		}
	}
	
	private void updatePropertyComboBox( final DisplayObjectTypePlugin displayObjectTypePlugin) {
		String primitiveFormName = _primitiveFormTypeTextField.getText();
		final DOTProperty[] properties;
		if ( primitiveFormName.equals( "")) {
			properties = displayObjectTypePlugin.getProperties( null);
		} else {
			properties = displayObjectTypePlugin.getProperties( primitiveFormName);
		}
		_propertyComboBox.removeAllItems();
		if ( properties != null) {
			for ( DOTProperty property : properties) {
				_propertyComboBox.addItem( property);
			}
		}
	}
	
	private void updateStaticCheckBox() {
		String primitiveFormName = (String) _primitiveFormComboBox.getSelectedItem();
		DOTProperty property = (DOTProperty) _propertyComboBox.getSelectedItem();
		if ( property != null) {
			if ( (primitiveFormName == null) || (primitiveFormName.equals( ""))) {
				_staticCheckBox.setSelected( _scratchDisplayObjectType.isPropertyStatic( null, property));
			} else if ( primitiveFormName != null){
				_staticCheckBox.setSelected( _scratchDisplayObjectType.isPropertyStatic( primitiveFormName, property));
			} else {
				_staticCheckBox.setSelected( true);
			}
		}
	}

	/**
     * Gibt <code>true</code> zurück, wenn die Identität des Darstellungstyps, also der Name,
     * nicht verändert werden kann, oder <code>false</code> andernfalls.
     * 
     * @return <code>true</code> genau dann, wenn die Identität nicht verändert werden kann
     */
    public boolean isReviseOnly() {
    	return _reviseOnly;
    }

	/**
     * Legt fest, ob die Identität des Darstellungstyps unverändert bleiben muss (<code>true</code>), 
     * oder aber nicht.
     * 
     * @param reviseOnly der neue Wert
     */
    public void setReviseOnly(boolean reviseOnly) {
    	_reviseOnly = reviseOnly;
    }

	/**
     * Gibt die Datenverteiler-Verbindung zurück.
     * 
     * @return die Datenverteiler-Verbindung
     */
    public ClientDavInterface getConnection() {
    	return _connection;
    }

	/**
     * Gibt die Darstellungstypen-Verwaltung zurück. Wäre im Moment entbehrlich, weil der DOTManager als Singleton
     * implementiert ist.
     * 
     * @return die Darstellungstypen-Verwaltung
     */
    public DOTManager getDotManager() {
    	return _dotManager;
    }

	/**
     * Gibt <code>true</code> zurück, wenn der übergebene Darstellungstyp veränderbar ist, und <code>false</code>
     * sonst.
     * 
     * @return ist der Darstellungstyp veränderbar
     */
    public boolean isEditable() {
    	return _editable;
    }

	/**
     * Gibt den aktuellen Inhalt des Namensfeldes zurück.
     * 
     * @return gibt den aktuellen Inhalt des Namensfeldes zurück
     */
    public String getNameText() {
    	return _nameTextField.getText();
    }

	/**
     * Gibt den aktuellen Inhalt des Info-Feldes zurück.
     * 
     * @return gibt den aktuellen Inhalt des Info-Feldes zurück
     */
    public String getInfoText() {
    	return _infoTextField.getText();
    }
    
    /**
     * Gibt den Namen der in der Auswahlbox selektierten Grundfigur zurück.
     * 
     * @return gibt den Namen der in der Auswahlbox selektierten Grundfigur zurück
     */
    public String getSelectedPrimitiveForm() {
    	return (String) _primitiveFormComboBox.getSelectedItem();
    }
    
    /**
     * Gibt die in der Auswahlbox selektierte Eigenschaft zurück.
     * 
     * @return gibt die in der Auswahlbox selektierte Eigenschaft zurück
     */
    public DOTProperty getSelectedProperty() {
		return (DOTProperty) _propertyComboBox.getSelectedItem();
    }

	/**
     * Gibt den Status der Statisch-Checkbox zurück.
     * 
     * @return gibt den Status der Statisch-Checkbox zurück
     */
    public boolean getStaticCheckBoxState() {
    	return _staticCheckBox.isSelected();
    }

	/**
     * Notiere, dass sich etwas verändert hat.
     * 
     * @param somethingChanged der neue Wert
     */
    public void setSomethingChanged(boolean somethingChanged) {
    	_somethingChanged = somethingChanged;
    }
    
    /**
     * Gibt an, ob die übergebene Eigenschaft statisch ist. Gehört die Eigenschaft zu einer
     * Grundfigur, so muss deren Name übergeben werden, sonst ist das erste Argument <code>null</code>. 
     * 
     * @param der Name einer Grundfigur oder <code>null</code>
     * @param eine Visulaisierungs-Eigenschaft
     * @return statisch oder dynamisch
     */
    public Boolean isPropertyStatic( String primitiveFormName, DOTProperty property) {
    	return _scratchDisplayObjectType.isPropertyStatic( primitiveFormName, property);
    }
    
    /**
     * Gibt den Wert der übergebenen Eigenschaftzurück. Gehört die Eigenschaft zu einer
     * Grundfigur, so muss deren Name übergeben werden, sonst ist das erste Argument <code>null</code>.
     * 
     * @param der Name einer Grundfigur oder <code>null</code>
     * @param eine Visulaisierungs-Eigenschaft
     * @return der Wert der statischen Eigenschaft oder null, wenn ein solcher nicht existiert
     */
    public Object getValueOfStaticProperty ( String primitiveFormName, DOTProperty property) {
    	return _scratchDisplayObjectType.getValueOfStaticProperty( primitiveFormName, property);
    }
    
    private void updateCenterPanel() {
    	if ( _dotDefinitionDialog == null) {	// im Konstruktor sind wir noch nicht so weit
    		return;
    	}
    	if ( _centerPanel != null) {
    		remove( _centerPanel);
    	}
    	_centerPanel = _dotDefinitionDialog.getDOTItemDefinitionPanel();
    	if ( _centerPanel != null) {
    		add( _centerPanel, BorderLayout.CENTER);
    	}
    	pack();
    	setSize(_frameSize);
    }
    
    private void addToolTips () {
    	_nameTextField.setToolTipText( "Der Name des Darstellungstyps");
    	_infoTextField.setToolTipText( "Eine Kurzinformation zu diesem DOT");
    	_primitiveFormComboBox.setToolTipText( "Der Name der Grundfigur");
    	_primitiveFormTypeTextField.setToolTipText( "Der Typ der Grundfigur");
    	_primitiveFormInfoTextField.setToolTipText( "Eine Kurzinformation zu der Grundfigur");
    	_positionX.setToolTipText( "Relative x-Koordinate des Mittelpunkts (außer beim Typ Text)");
    	_positionY.setToolTipText( "Relative y-Koordinate des Mittelpunkts (außer beim Typ Text)");
    	_editPrimitiveFormButton.setToolTipText( "Diese Grundfigur bearbeiten.");
    	_copyPrimitiveFormButton.setToolTipText( "Diese Grundfigur kopieren und die Kopie bearbeiten.");
    	_newPrimitiveFormButton.setToolTipText( "Eine neue Grundfigur anlegen.");
    	_deletePrimitiveFormButton.setToolTipText( "Diese Grundfigur löschen.");
    	_propertyComboBox.setToolTipText( "Eine Eigenschaft der Grundfigur.");
    	_staticCheckBox.setToolTipText( "Die Eigenschaft ist statisch oder dynamisch.");
    	_saveButton.setToolTipText( "Diesen Darstellungstyp speichern.");
    	_specialInformationDefinitionPanel.setToolTipText( "Typabhängige Definitionsmerkmale der Grundfigur.");
    	_siHeight.setToolTipText( "Die Höhe des Rechtecks.");
    	_siWidth.setToolTipText( "Die Breite des Rechtecks.");
    	_siRadius.setToolTipText( "Der Radius des Kreises.");
    	_siSemiRadius.setToolTipText( "Der Radius des Halbkreises.");
    	_siOrientation.setToolTipText( "Der Orientierung des Halbkreises.");
    }
    
	final private static String[] SEMI_CIRCLE_ITEMS = { DOTPointPainter.LINKER_HALBKREIS,
		DOTPointPainter.OBERER_HALBKREIS, DOTPointPainter.RECHTER_HALBKREIS,
		DOTPointPainter.UNTERER_HALBKREIS};	// Böse! S.o.
}

