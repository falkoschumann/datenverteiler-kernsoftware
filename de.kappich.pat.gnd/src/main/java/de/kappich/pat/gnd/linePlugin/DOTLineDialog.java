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
package de.kappich.pat.gnd.linePlugin;


import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTDefinitionDialogFrame;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItemManager;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDefinitionComponent;
import de.kappich.pat.gnd.displayObjectToolkit.DOTItemManager.DisplayObjectTypeItemWithInterval;
import de.kappich.pat.gnd.pluginInterfaces.DOTDefinitionDialog;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.utils.SpringUtilities;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Der Definitionsdialog für Darstellungstypen von Linienobjekten.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8092 $
 *
 */
public class DOTLineDialog implements DOTDefinitionDialog {
	
	private static final Debug _debug = Debug.getLogger();
	
	private final DOTDefinitionDialogFrame _dotDefinitionDialogFrame;
	
	private final ClientDavInterface _connection;
	
	private final DataModel _configuration;
	
	/**
	 * Konstruiert den Dialog.
	 * 
	 * @param dotDefinitionDialogFrame der umgebende Rahmen
	 */
	DOTLineDialog( final DOTDefinitionDialogFrame dotDefinitionDialogFrame) {
		_dotDefinitionDialogFrame = dotDefinitionDialogFrame;
		_connection = _dotDefinitionDialogFrame.getConnection();
		_configuration = _connection.getDataModel();
		if ( !(dotDefinitionDialogFrame.getDisplayObjectType() instanceof DOTLine)) {
			throw new IllegalArgumentException();
		}
		// Die Strategie ist bei den folgenden Membern wie folgt: es werden alle Center-Panels,
		// also alle statischen und nicht-statischen initialisiert, damit beim Umschalten möglichst
		// wenig Informationen verloren gehen. Das Scratch-DOT dient den Dialogen zunächst zur
		// Initialisierung, und anschließend werden alle Änderungen, die duch die Dialoge
		// vorgenommen werden, dort nachgezogen. Beim Speichern müssen die Informationen aus
		// beiden Quellen kombiniert werden.
		initAllPanelsAndTables();
	}

	private void initAllPanelsAndTables() {
		initStaticColorCenterPanel();
		initStaticDistanceCenterPanel();
		initStaticStrokeWidthCenterPanel();
		initNonStaticColorCenterPanel();
		initNonStaticDistanceCenterPanel();
		initNonStaticStrokeWidthCenterPanel();
	}
	
	private void addButtonListeners( final DOTProperty property,
			final JButton newDOTItemButton, 
			final JButton deleteDOTItemButton,
			final JButton showConflictsButton) {
		
		@SuppressWarnings("serial")
        class DOTItemDialog extends JDialog {
			
			final private DOTProperty _property;
			
			private JComboBox _colorComboBox = null;
			
			private JSpinner _distanceSpinner = null;
			
			private JSpinner _strokeWidthSpinner = null;
			
			final private DynamicDefinitionComponent _dynamicDefinitionComponent;
			
			
			DOTItemDialog () {
				super();
				_property = property;
				
				JLabel aLabel = null;
				if ( _property == DOTProperty.FARBE) {
					aLabel = new JLabel("Farbe: ");
					_colorComboBox = new JComboBox( ColorManager.getInstance().getColorNames());
				} else if ( _property == DOTProperty.ABSTAND) {
					aLabel = new JLabel("Abstand in Pixeln: ");
					SpinnerNumberModel distanceSpinnerModel = new SpinnerNumberModel(0, -100, 1000, 1);
					_distanceSpinner = new JSpinner( distanceSpinnerModel);
				} else if ( _property == DOTProperty.STRICHBREITE) {
					aLabel = new JLabel("Strichbreite in Pixeln: ");
					SpinnerNumberModel strokeWidthSpinnerModel = new SpinnerNumberModel(0.5, 0.0, 1000.0, 0.1);
					_strokeWidthSpinner = new JSpinner( strokeWidthSpinnerModel);
				}
				
				_dynamicDefinitionComponent = new DynamicDefinitionComponent(_configuration, 
						new DOTLinePlugin());
				final Object propertyValue = initDynamicDefinitionComponent();
				
				JPanel panel = new JPanel();
				panel.setLayout( new SpringLayout());
				if ( _property == DOTProperty.FARBE) {
					panel.add( aLabel);
					panel.add( _colorComboBox);
					if ( propertyValue != null) {
						_colorComboBox.setSelectedItem( propertyValue);
					}
				} else if ( _property == DOTProperty.ABSTAND) {
					panel.add( aLabel);
					panel.add( _distanceSpinner);
					if ( propertyValue != null) {
						_distanceSpinner.setValue( propertyValue);
					}
				} else if ( _property == DOTProperty.STRICHBREITE) {
					panel.add( aLabel);
					panel.add( _strokeWidthSpinner);
					if ( propertyValue != null) {
						_strokeWidthSpinner.setValue( propertyValue);
					}
				}
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
				SpringUtilities.makeCompactGrid(panel, 2, 20, 5);
				
				JButton saveButton = new JButton("Speichern");
				saveButton.setEnabled( _dotDefinitionDialogFrame.isEditable());
				JButton cancelButton = new JButton("Dialog schließen");
				
				JPanel buttonsPanel = new JPanel();
				buttonsPanel.setLayout(new SpringLayout());
				
				buttonsPanel.add(saveButton);
				buttonsPanel.add(cancelButton);
				
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
				SpringUtilities.makeCompactGrid(buttonsPanel, 2, 20, 5);
				addButtonListeners( saveButton, cancelButton);
				
				setTitle("GND: Darstellungsfestlegung für Linien");
				setLayout(new BorderLayout());
				add( new JScrollPane( panel), BorderLayout.NORTH);
				add( new JScrollPane( _dynamicDefinitionComponent), BorderLayout.CENTER);
				add( buttonsPanel, BorderLayout.SOUTH);
				pack();
				setSize(700, 550);
				final Rectangle bounds = getBounds();
				setLocation(new Point((int)(bounds.getHeight() / 1.5 + 300), (int)(bounds.getWidth() / 1.5)));
			}
			
			public void runDialog() {
				setVisible( true);
			}
			
			@SuppressWarnings("unchecked")
            private Object initDynamicDefinitionComponent() {
				JTable workWithThisTable = null;
				if ( _property == DOTProperty.FARBE) {
					workWithThisTable = _nonStaticColorCenterPanelTable;
				} else if ( _property == DOTProperty.ABSTAND) {
					workWithThisTable = _nonStaticDistanceCenterPanelTable;
				} else if ( _property == DOTProperty.STRICHBREITE) {
					workWithThisTable = _nonStaticStrokeWidthCenterPanelTable;
				} 
				
				int selectedRow = workWithThisTable.getSelectedRow();
				if ( selectedRow == -1 ) {
					if (workWithThisTable.getModel().getRowCount() > 0) {
						selectedRow = 0;
					} else {
						return null;
					}
				}
				selectedRow = workWithThisTable.convertRowIndexToModel( selectedRow);
				final TableModel model = workWithThisTable.getModel();
				DynamicDOTItemManager dynamicDOTItemManager = (DynamicDOTItemManager) model;
				final DisplayObjectTypeItemWithInterval dotItemWithInterval = dynamicDOTItemManager.get( selectedRow);
				_dynamicDefinitionComponent.fillComponents(dotItemWithInterval);
				return dotItemWithInterval.getItem().getPropertyValue();
			}
						
			private void addButtonListeners( JButton saveButton, JButton cancelButton) {
				ActionListener actionListenerSave = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						String colorName = null;
						if ( _property == DOTProperty.FARBE) {
							final Object colorItem = _colorComboBox.getSelectedItem();
							if ( colorItem == null ) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte wählen Sie eine Farbe aus!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
							colorName = (String) colorItem;
						}
						
						Integer distance = null;
						if ( _property == DOTProperty.ABSTAND) {
							distance = (Integer)_distanceSpinner.getValue();
						}
						
						Double strokeWidth = null;
						if ( _property == DOTProperty.STRICHBREITE) {
							strokeWidth = (Double)_strokeWidthSpinner.getValue();
							if ( strokeWidth.isNaN()) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte korrigieren Sie den Strichbreitenwert!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						
						final String description = _dynamicDefinitionComponent.getInfoText();
						if ( description == null) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte geben Sie einen Info-Text ein!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						final String attributeGroupName = _dynamicDefinitionComponent.getAttributeGroupName();
						if ( attributeGroupName == null ) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte wählen Sie eine Attributgruppe aus!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						final String aspectName = _dynamicDefinitionComponent.getAspectName();
						if ( aspectName == null ) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte wählen Sie einen Aspekt aus!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						final String attributeName = _dynamicDefinitionComponent.getAttributeName();
						if ( attributeName == null ) {
							int error = _dynamicDefinitionComponent.checkAttributeName();
							if ( error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte wählen Sie ein Attribut aus!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							} else if ( error == 2) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Der Attributname ist ungültig!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						
						final Double fromValue = _dynamicDefinitionComponent.getFromValue();
						if ( fromValue == null || fromValue.isNaN()) {
							int error = _dynamicDefinitionComponent.checkFromValue();
							if ( error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte tragen Sie einen Von-Wert ein!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							} else if ( (error == 2) || (error == 3)) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte korrigieren Sie den Von-Wert!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						
						final Double toValue = _dynamicDefinitionComponent.getToValue();
						if ( toValue == null || toValue.isNaN()) {
							int error = _dynamicDefinitionComponent.checkToValue();
							if ( error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte tragen Sie einen Bis-Wert ein!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							} else if ( (error == 2) || (error == 3)) {
								JOptionPane.showMessageDialog(
										new JFrame(),
										"Bitte korrigieren Sie den Bis-Wert!",
										"Fehler",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						
						if ( (fromValue != null) && (toValue !=null) && (fromValue > toValue)) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Der Von-Wert ist größer als der Bis-Wert!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						Object propertyValue = null;
						if ( _property == DOTProperty.FARBE) {
							propertyValue = colorName;
						} else if ( _property == DOTProperty.ABSTAND) {
							propertyValue = distance;
						} else if ( _property == DOTProperty.STRICHBREITE) {
							propertyValue = strokeWidth;
						}
						DynamicDOTItem dItem = new DynamicDOTItem( 
								attributeGroupName,
								aspectName,
								attributeName,
								description,
								propertyValue);
						_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfDynamicProperty(
								null, _property, dItem, fromValue, toValue);
						_dotDefinitionDialogFrame.setSomethingChanged( true);
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
		
		ActionListener actionListenerNewButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DOTItemDialog dotItemDialog = new DOTItemDialog();
				dotItemDialog.runDialog();
			}
		};
		newDOTItemButton.addActionListener( actionListenerNewButton);
		
		ActionListener actionListenerDeleteButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final DOTProperty currentProperty = _dotDefinitionDialogFrame.getSelectedProperty();
				JTable workForThisTable = null;
				if ( currentProperty == DOTProperty.FARBE) {
					workForThisTable = _nonStaticColorCenterPanelTable;
				} else if ( currentProperty == DOTProperty.ABSTAND) {
					workForThisTable = _nonStaticDistanceCenterPanelTable;
				} else if ( currentProperty == DOTProperty.STRICHBREITE) {
					workForThisTable = _nonStaticStrokeWidthCenterPanelTable;
				} 
				final int[] selectedRows = workForThisTable.getSelectedRows();
				if ( selectedRows.length == 0 ) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final TableModel model = workForThisTable.getModel();
				DynamicDOTItemManager dynamicDOTItemManager = (DynamicDOTItemManager) model;
				Set<Integer> selectedModelIndexes = new HashSet<Integer>();
				for ( int j = 0; j < selectedRows.length; j++) {
					selectedModelIndexes.add(workForThisTable.convertRowIndexToModel(selectedRows[j]));
				}
				final Object[] array = selectedModelIndexes.toArray();
				for ( int i = array.length -1; i >= 0; i--) {
					dynamicDOTItemManager.remove((Integer) array[i]);
					_dotDefinitionDialogFrame.setSomethingChanged( true);
				}
			}
		};
		deleteDOTItemButton.addActionListener( actionListenerDeleteButton);
		
		ActionListener actionListenerShowConflictsButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final DOTProperty currentProperty = _dotDefinitionDialogFrame.getSelectedProperty();
				JTable workForThisTable = null;
				if ( currentProperty == DOTProperty.FARBE) {
					workForThisTable = _nonStaticColorCenterPanelTable;
				} else if ( currentProperty == DOTProperty.ABSTAND) {
					workForThisTable = _nonStaticDistanceCenterPanelTable;
				} else if ( currentProperty == DOTProperty.STRICHBREITE) {
					workForThisTable = _nonStaticStrokeWidthCenterPanelTable;
				} 
				workForThisTable.clearSelection();
				
				DOTLine dotLine = (DOTLine) _dotDefinitionDialogFrame.getScratchDisplayObjectType();
				final Set<Integer> conflictingRows = dotLine.getConflictingRows(property);
				ListSelectionModel listSelectionModel = workForThisTable.getSelectionModel();
				for ( Integer row : conflictingRows) {
					Integer rowInView = workForThisTable.convertRowIndexToView(row);
					listSelectionModel.addSelectionInterval(rowInView, rowInView);
				}
				workForThisTable.setSelectionModel( listSelectionModel);
			}
		};
		showConflictsButton.addActionListener( actionListenerShowConflictsButton);
	}
	
	private void addListSelectionListener(final JTable table, final JButton deleteDOTItemButton) {
		final ListSelectionModel selectionModel = table.getSelectionModel();
		ListSelectionListener listSelectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = table.getSelectedRow();
				if ( selectedRow == -1) {
					deleteDOTItemButton.setEnabled( false);
				} else {
					deleteDOTItemButton.setEnabled( _dotDefinitionDialogFrame.isEditable());
				}
            }
		};
		selectionModel.addListSelectionListener( listSelectionListener);
	}
	
	private JPanel _staticColorCenterPanel = null;
	private JPanel _staticDistanceCenterPanel = null;
	private JPanel _staticStrokeWidthCenterPanel = null;
	final private JComboBox _staticColorBox = new JComboBox( ColorManager.getInstance().getColorNames());
	final private JSpinner _staticDistanceSpinner = new JSpinner( new SpinnerNumberModel(0, -100, 1000, 1));
	final private JSpinner _staticStrokeWidthSpinner = new JSpinner( new SpinnerNumberModel(0.5, 0.0, 1000.0, 0.1));
	
	final private JPanel _nonStaticColorCenterPanel = new JPanel();
	final private JPanel _nonStaticDistanceCenterPanel = new JPanel();
	final private JPanel _nonStaticStrokeWidthCenterPanel = new JPanel();
	final private JTable _nonStaticColorCenterPanelTable = new JTable();
	final private JTable _nonStaticDistanceCenterPanelTable = new JTable();
	final private JTable _nonStaticStrokeWidthCenterPanelTable = new JTable();
	
	public JPanel getDOTItemDefinitionPanel() {
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		if ( property == null) {
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Bitte wählen Sie eine Eigenschaft aus!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if ( _dotDefinitionDialogFrame.isPropertyStatic( null, property)) {
			return getStaticCenterPanel();
		} else {
			return getNonStaticCenterPanel();
		}
    }

	public void saveDisplayObjectType() {
		DOTLine newDOTLine = (DOTLine) _dotDefinitionDialogFrame.getScratchDisplayObjectType().getCopy( null);
		final String name = _dotDefinitionDialogFrame.getNameText();
		if ( (name == null) || (name.length() == 0)) {
			JOptionPane.showMessageDialog(
					new JFrame(),
					"Bitte geben Sie einen Namen an!",
					"Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if ( !_dotDefinitionDialogFrame.isReviseOnly()) {
			if ( _dotDefinitionDialogFrame.getDotManager().containsDisplayObjectType( name)) {
				JOptionPane.showMessageDialog(
						new JFrame(),
						"Ein Darstellungstyp mit diesem Namen existiert bereits!",
						"Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		newDOTLine.setName( name);
		newDOTLine.setInfo( _dotDefinitionDialogFrame.getInfoText());
		_dotDefinitionDialogFrame.getDotManager().saveDisplayObjectType(newDOTLine);
		_dotDefinitionDialogFrame.setDisplayObjectType( newDOTLine, true);
    }
	
	private JPanel getStaticCenterPanel () {
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		final String propertyName = property.toString();
		if ( propertyName.equals( "Farbe")) {
			return _staticColorCenterPanel;
		} else if ( propertyName.equals( "Abstand")) {
			return _staticDistanceCenterPanel;
		} else if ( propertyName.equals( "Strichbreite")) {
			return _staticStrokeWidthCenterPanel;
		} else {
			_debug.warning( "DOTLineDialog: Die Eigenschaft " + propertyName + " wird nicht unterstützt.");
			return null;
		}
	}
	
	private void initStaticColorCenterPanel() {
		JLabel colorLabel = new JLabel("Farbe: ");
		_staticColorBox.setMaximumSize( new Dimension( 200, 25));
		_staticColorBox.setEnabled(_dotDefinitionDialogFrame.isEditable());
	
		if ( _dotDefinitionDialogFrame.isPropertyStatic( null, DOTProperty.FARBE)) {
			final String colorName = (String) _dotDefinitionDialogFrame.getValueOfStaticProperty( null, DOTProperty.FARBE);
			_staticColorBox.setSelectedItem( colorName);
		} else {
			_staticColorBox.getModel().setSelectedItem( DOTLine.DEFAULT_COLOR_NAME);
		}
		
		ItemListener itemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				final Object selectedItem = _staticColorBox.getSelectedItem();
				_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfStaticProperty( null, DOTProperty.FARBE, selectedItem);
            }
		};
		_staticColorBox.addItemListener( itemListener);
		
		_staticColorCenterPanel = new JPanel();
		_staticColorCenterPanel.setLayout(new SpringLayout());
		_staticColorCenterPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		_staticColorCenterPanel.add( colorLabel);
		_staticColorCenterPanel.add( _staticColorBox);
		SpringUtilities.makeCompactGrid(_staticColorCenterPanel, 2, 5, 5);
		
	}
	
	private void initStaticDistanceCenterPanel() {
		JLabel distanceLabel = new JLabel("Abstand in Pixeln: ");
		_staticDistanceSpinner.setMaximumSize( new Dimension( 200, 20));
		_staticDistanceSpinner.setEnabled(_dotDefinitionDialogFrame.isEditable());
		
		if ( _dotDefinitionDialogFrame.isPropertyStatic( null,  DOTProperty.ABSTAND)) {
			final Integer distance = (Integer) _dotDefinitionDialogFrame.getValueOfStaticProperty( null,  DOTProperty.ABSTAND);
			_staticDistanceSpinner.setValue( distance);
		} else {
			_staticDistanceSpinner.setValue( DOTLine.DEFAULT_DISTANCE);
		}
		
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				final Integer value = (Integer) _staticDistanceSpinner.getValue();
				_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfStaticProperty( null, DOTProperty.ABSTAND, value);
			}
		};
		_staticDistanceSpinner.addChangeListener( changeListener);
		
		_staticDistanceCenterPanel = new JPanel();
		_staticDistanceCenterPanel.setLayout(new SpringLayout());
		_staticDistanceCenterPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		_staticDistanceCenterPanel.add( distanceLabel);
		_staticDistanceCenterPanel.add( _staticDistanceSpinner);
		SpringUtilities.makeCompactGrid(_staticDistanceCenterPanel, 2, 5, 5);
	}
	
	private void initStaticStrokeWidthCenterPanel() {
		JLabel strokeWidthLabel = new JLabel ("Strichbreite in Pixeln: ");
		_staticStrokeWidthSpinner.setMaximumSize( new Dimension( 200, 20));
		_staticStrokeWidthSpinner.setEnabled(_dotDefinitionDialogFrame.isEditable());
		
		if ( _dotDefinitionDialogFrame.isPropertyStatic( null, DOTProperty.STRICHBREITE)) {
			final Double strokeWidth = (Double) _dotDefinitionDialogFrame.getValueOfStaticProperty( null, DOTProperty.STRICHBREITE);
			_staticStrokeWidthSpinner.setValue( strokeWidth);
		} else {
			_staticStrokeWidthSpinner.setValue( DOTLine.DEFAULT_STROKE_WIDTH);
		}
		
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				final Double value = (Double) _staticStrokeWidthSpinner.getValue();
				_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfStaticProperty( null, DOTProperty.STRICHBREITE, value);
			}
		};
		_staticStrokeWidthSpinner.addChangeListener( changeListener);
		
		_staticStrokeWidthCenterPanel = new JPanel();
		_staticStrokeWidthCenterPanel.setLayout(new SpringLayout());
		_staticStrokeWidthCenterPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		_staticStrokeWidthCenterPanel.add( strokeWidthLabel);
		_staticStrokeWidthCenterPanel.add( _staticStrokeWidthSpinner);
		SpringUtilities.makeCompactGrid(_staticStrokeWidthCenterPanel, 2, 5, 5);
	}
		
	private JPanel getNonStaticCenterPanel() {
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		if ( property == DOTProperty.FARBE) {
			return _nonStaticColorCenterPanel;
		} else if ( property == DOTProperty.ABSTAND) {
			return _nonStaticDistanceCenterPanel;
		} else if ( property == DOTProperty.STRICHBREITE) {
			return _nonStaticStrokeWidthCenterPanel;
		} else {
			_debug.warning( "DOTLineDialog: Die Eigenschaft " + property.toString() + " wird nicht unterstützt.");
			return null;
		}
	}
	
	private void initNonStaticColorCenterPanel () {
		initANonStaticCenterPanel( DOTProperty.FARBE, _nonStaticColorCenterPanel, _nonStaticColorCenterPanelTable);
	}
	
	private void initNonStaticDistanceCenterPanel () {
		initANonStaticCenterPanel( DOTProperty.ABSTAND, _nonStaticDistanceCenterPanel, _nonStaticDistanceCenterPanelTable);
	}
	
	private void initNonStaticStrokeWidthCenterPanel () {
		initANonStaticCenterPanel( DOTProperty.STRICHBREITE, _nonStaticStrokeWidthCenterPanel, _nonStaticStrokeWidthCenterPanelTable);
	}
		
	private void initANonStaticCenterPanel ( 
			final DOTProperty property, 
			JPanel thePanel, 
			JTable theTable) {
		DOTLine dotLine = (DOTLine) _dotDefinitionDialogFrame.getScratchDisplayObjectType();
		final DynamicDOTItemManager tableModel = (DynamicDOTItemManager) dotLine.getTableModel( property);
		theTable.setModel(tableModel);
		
		class NumberComparator implements Comparator<Number> {
			public int compare(Number o1, Number o2) {
				final double d1 = o1.doubleValue();
				final double d2 = o2.doubleValue();
				if ( d1 < d2) {
					return -1;
				} 
				if ( d1 == d2) {
					return 0;
				}
				return 1;
	        }
		}
		TableRowSorter<DynamicDOTItemManager> tableRowSorter = new TableRowSorter<DynamicDOTItemManager>();
		tableRowSorter.setModel( tableModel);
		tableRowSorter.setComparator( 4, new NumberComparator());
		tableRowSorter.setComparator( 5, new NumberComparator());
		theTable.setRowSorter( tableRowSorter);
		
		JButton newDOTItemButton = new JButton("Neue Zeile");
		newDOTItemButton.setEnabled( _dotDefinitionDialogFrame.isEditable());
		JButton deleteDOTItemButton = new JButton("Löschen");
		deleteDOTItemButton.setEnabled( false);
		JButton showConflictsButton = new JButton("Zeige Konflikte");
		
		addButtonListeners(property, newDOTItemButton, deleteDOTItemButton, showConflictsButton);
		addListSelectionListener( theTable, deleteDOTItemButton);
		
		JPanel dotButtonsPanel = new JPanel();
		dotButtonsPanel.setLayout(new SpringLayout());
		
		dotButtonsPanel.add(newDOTItemButton);
		dotButtonsPanel.add(deleteDOTItemButton);
		dotButtonsPanel.add(showConflictsButton);
		
		dotButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(dotButtonsPanel, 1, 5, 20);
		
		thePanel.setLayout(new SpringLayout());
		thePanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		thePanel.add( new JScrollPane( theTable));
		thePanel.add( dotButtonsPanel);
		SpringUtilities.makeCompactGrid(thePanel, 2, 20, 5);
	}

	public JPanel getAdditionalCharacteristicsPanel( final DisplayObjectType displayObjectType) {
	    return null;
    }
}
