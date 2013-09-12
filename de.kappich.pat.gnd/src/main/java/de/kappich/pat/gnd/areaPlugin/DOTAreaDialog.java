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
package de.kappich.pat.gnd.areaPlugin;

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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Der Definitionsdialog für Darstellungstypen von Flächenobjekten.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8092 $
 *
 */
public class DOTAreaDialog implements DOTDefinitionDialog {
	
	private final DOTDefinitionDialogFrame _dotDefinitionDialogFrame;
	
	private final ClientDavInterface _connection;
	
	private final DataModel _configuration;
	
	private JPanel _staticColorCenterPanel = null;
	final private JComboBox _staticColorBox = new JComboBox( ColorManager.getInstance().getColorNames());
	final private JPanel _nonStaticColorCenterPanel = new JPanel();
	final private JTable _nonStaticColorCenterPanelTable = new JTable();
	
	/**
	 * Konstruiert den Dialog.
	 */
	public DOTAreaDialog( final DOTDefinitionDialogFrame dotDefinitionDialogFrame) {
		_dotDefinitionDialogFrame = dotDefinitionDialogFrame;
		_connection = _dotDefinitionDialogFrame.getConnection();
		_configuration = _connection.getDataModel();
		if ( !(dotDefinitionDialogFrame.getDisplayObjectType() instanceof DOTArea)) {
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
		initNonStaticColorCenterPanel();
	}
	
	private void initStaticColorCenterPanel() {
		JLabel colorLabel = new JLabel("Farbe: ");
		_staticColorBox.setMaximumSize( new Dimension( 200, 25));
	
		if ( _dotDefinitionDialogFrame.isPropertyStatic( null, DOTProperty.FARBE)) {
			final String colorName = (String) _dotDefinitionDialogFrame.getValueOfStaticProperty( null, DOTProperty.FARBE);
			_staticColorBox.setSelectedItem( colorName);
		} else {
			_staticColorBox.getModel().setSelectedItem( DOTArea.DEFAULT_COLOR_NAME);
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

	private void initNonStaticColorCenterPanel () {
		initANonStaticCenterPanel( DOTProperty.FARBE, _nonStaticColorCenterPanel, _nonStaticColorCenterPanelTable);
	}
	
	private void initANonStaticCenterPanel ( 
			final DOTProperty property, 
			JPanel thePanel, 
			JTable theTable) {
		DOTArea dotArea = (DOTArea) _dotDefinitionDialogFrame.getScratchDisplayObjectType();
		final DynamicDOTItemManager tableModel = (DynamicDOTItemManager) dotArea.getTableModel( property);
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
	
	private JPanel getStaticCenterPanel () {
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		final String propertyName = property.toString();
		if ( propertyName.equals( "Farbe")) {
			return _staticColorCenterPanel;
		} else {
			return null;
		}
	}
	
	private JPanel getNonStaticCenterPanel() {
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		if ( property == DOTProperty.FARBE) {
			return _nonStaticColorCenterPanel;
		} else {
			return null;
		}
	}

	public void saveDisplayObjectType() {
		DOTArea newDOTArea = (DOTArea) _dotDefinitionDialogFrame.getScratchDisplayObjectType().getCopy( null);
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
		newDOTArea.setName( name);
		newDOTArea.setInfo( _dotDefinitionDialogFrame.getInfoText());
		_dotDefinitionDialogFrame.getDotManager().saveDisplayObjectType(newDOTArea);
		_dotDefinitionDialogFrame.setDisplayObjectType( newDOTArea, true);
    }
	
	public JPanel getAdditionalCharacteristicsPanel(DisplayObjectType displayObjectType) {
		return null;
    }
	
	private void addButtonListeners( final DOTProperty property,
			final JButton newDOTItemButton, 
			final JButton deleteDOTItemButton,
			final JButton showConflictsButton) {
		
		@SuppressWarnings("serial")
        class DOTItemDialog extends JDialog {
			
			final private DOTProperty _property;
			private JComboBox _colorComboBox = null;
			final private DynamicDefinitionComponent _dynamicDefinitionComponent;
			
			DOTItemDialog () {
				super();
				_property = property;
				
				JLabel aLabel = null;
				if ( _property == DOTProperty.FARBE) {
					aLabel = new JLabel("Farbe: ");
					_colorComboBox = new JComboBox( ColorManager.getInstance().getColorNames());
				}
				
				_dynamicDefinitionComponent = new DynamicDefinitionComponent(_configuration, 
						new DOTAreaPlugin());
				final Object propertyValue = initDynamicDefinitionComponent();
				
				JPanel panel = new JPanel();
				panel.setLayout( new SpringLayout());
				if ( _property == DOTProperty.FARBE) {
					panel.add( aLabel);
					panel.add( _colorComboBox);
					if ( propertyValue != null) {
						_colorComboBox.setSelectedItem( propertyValue);
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
				
				setTitle("GND: Darstellungsfestlegung für Flächen");
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
						if ( fromValue == null ) {
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
						if ( toValue == null ) {
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
				}
				workForThisTable.clearSelection();
				
				DOTArea dotArea = (DOTArea) _dotDefinitionDialogFrame.getScratchDisplayObjectType();
				final Set<Integer> conflictingRows = dotArea.getConflictingRows(property);
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
}
