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

package de.kappich.pat.gnd.pointPlugin;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTDefinitionDialogFrame;
import de.kappich.pat.gnd.displayObjectToolkit.DOTItemManager.DisplayObjectTypeItemWithInterval;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItemManager;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDefinitionComponent;
import de.kappich.pat.gnd.displayObjectToolkit.PrimitiveFormPropertyPair;
import de.kappich.pat.gnd.pluginInterfaces.DOTDefinitionDialog;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveForm;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveFormType;
import de.kappich.pat.gnd.utils.SpringUtilities;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 * Der Definitionsdialog für Darstellungstypen von Punktobjekten.
 * <p/>
 * DOTPointDialog implementiert das Interface DOTDefinitionDialog für das Punkt-Plugin. Hierzu interagiert es intensiv und software-technisch unsauber mit dem
 * umschließenden DOTDefinitionDialogFrame.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11698 $
 */
public class DOTPointDialog implements DOTDefinitionDialog {

	private final DOTDefinitionDialogFrame _dotDefinitionDialogFrame;

	private final ClientDavInterface _connection;

	private final DataModel _configuration;

	private final JSpinner _translationFactorSpinner = new JSpinner();

	private final JCheckBox _joinByLineCheckBox = new JCheckBox();

	private final Map<PrimitiveFormPropertyPair, JPanel> _staticPanels = new HashMap<PrimitiveFormPropertyPair, JPanel>();

	private final Map<PrimitiveFormPropertyPair, JPanel> _dynamicPanels = new HashMap<PrimitiveFormPropertyPair, JPanel>();

	private final Map<PrimitiveFormPropertyPair, JTable> _dynamicTables = new HashMap<PrimitiveFormPropertyPair, JTable>();

	DOTPointDialog() {
		_dotDefinitionDialogFrame = new DOTDefinitionDialogFrame();
		_connection = null;
		_configuration = null;
	}

	/**
	 * Konstruiert einen DOTPointDialog.
	 *
	 * @param dotDefinitionDialogFrame das umgebende Fenster
	 */
	DOTPointDialog(final DOTDefinitionDialogFrame dotDefinitionDialogFrame) {
		_dotDefinitionDialogFrame = dotDefinitionDialogFrame;
		_connection = _dotDefinitionDialogFrame.getConnection();
		_configuration = _connection.getDataModel();
		if(!(dotDefinitionDialogFrame.getDisplayObjectType() instanceof DOTPoint)) {
			throw new IllegalArgumentException();
		}
		addToolTips();
	}

	public JPanel getAdditionalCharacteristicsPanel(final DisplayObjectType displayObjectType) {
		JLabel translationFactorLabel = new JLabel("Verschiebungsfaktor: ");
		SpinnerModel spinnerModel = new SpinnerNumberModel(100, 0, 5000, 1);
		_translationFactorSpinner.setModel(spinnerModel);
		_translationFactorSpinner.setMaximumSize(new Dimension(60, 30));
		_translationFactorSpinner.setEnabled(_dotDefinitionDialogFrame.isEditable());
		JPanel translationPanel = new JPanel();
		translationPanel.setLayout(new BoxLayout(translationPanel, BoxLayout.X_AXIS));
		translationPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
		translationPanel.add(translationFactorLabel);
		translationPanel.add(_translationFactorSpinner);

		JLabel joinByLineLabel = new JLabel("Verbindungslinie: ");
		_joinByLineCheckBox.setSelected(false);
		_joinByLineCheckBox.setEnabled(_dotDefinitionDialogFrame.isEditable());
		JPanel joinByLinePanel = new JPanel();
		joinByLinePanel.setLayout(new BoxLayout(joinByLinePanel, BoxLayout.X_AXIS));
		joinByLinePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		joinByLinePanel.add(joinByLineLabel);
		joinByLinePanel.add(_joinByLineCheckBox);

		JPanel invisiblePanel = new JPanel();
		invisiblePanel.add(new JTextField());
		invisiblePanel.setVisible(false);

		JPanel thePanel = new JPanel();
		thePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		thePanel.setLayout(new SpringLayout());
		thePanel.add(translationPanel);
		thePanel.add(joinByLinePanel);
		thePanel.add(invisiblePanel);
		SpringUtilities.makeCompactGrid(thePanel, 3, 5, 5);

		if(displayObjectType != null) {
			DOTPoint dotPoint = (DOTPoint)displayObjectType;
			_translationFactorSpinner.setValue(dotPoint.getTranslationFactor());
			_joinByLineCheckBox.setSelected(dotPoint.isJoinByLine());
		}
		addChangeListeners(); // Erst jetzt, denn sonst werden die Setzungen von _translationFactorSpinner und _joinByLineCheckBox schon verarbeitet!

		return thePanel;
	}

	private void addChangeListeners() {
		ChangeListener translationChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				_dotDefinitionDialogFrame.setSomethingChanged(true);
			}
		};
		_translationFactorSpinner.addChangeListener(translationChangeListener);

		ChangeListener joinByLineChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				_dotDefinitionDialogFrame.setSomethingChanged(true);
			}
		};
		_joinByLineCheckBox.addChangeListener(joinByLineChangeListener);
	}

	public JPanel getDOTItemDefinitionPanel() {
		DOTPoint dotPoint = (DOTPoint)_dotDefinitionDialogFrame.getScratchDisplayObjectType();
		final PrimitiveForm primitiveForm = dotPoint.getPrimitiveForm(
				_dotDefinitionDialogFrame.getSelectedPrimitiveForm()
		);
		final DOTProperty property = _dotDefinitionDialogFrame.getSelectedProperty();
		if((primitiveForm == null) || (property == null)) {
			return null;
		}
		PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair(primitiveForm.getName(), property);
		final boolean isStatic = _dotDefinitionDialogFrame.getStaticCheckBoxState();
		if(isStatic) {
			if(!_staticPanels.containsKey(pfpPair)) {
				_staticPanels.put(pfpPair, createStaticCenterPanel(primitiveForm, property));
			}
			return _staticPanels.get(pfpPair);
		}
		else {
			if(!_dynamicPanels.containsKey(pfpPair)) {
				_dynamicPanels.put(pfpPair, createDynamicCenterPanel(primitiveForm, property));
			}
			return _dynamicPanels.get(pfpPair);
		}
	}

	public void saveDisplayObjectType() {
		DOTPoint newDOTPoint = (DOTPoint)_dotDefinitionDialogFrame.getScratchDisplayObjectType().getCopy(null);
		final String name = _dotDefinitionDialogFrame.getNameText();
		if((name == null) || (name.length() == 0)) {
			JOptionPane.showMessageDialog(
					new JFrame(), "Bitte geben Sie einen Namen an!", "Fehler", JOptionPane.ERROR_MESSAGE
			);
			return;
		}
		if(!_dotDefinitionDialogFrame.isReviseOnly()) {
			if(_dotDefinitionDialogFrame.getDotManager().containsDisplayObjectType(name)) {
				JOptionPane.showMessageDialog(
						new JFrame(), "Ein Darstellungstyp mit diesem Namen existiert bereits!", "Fehler", JOptionPane.ERROR_MESSAGE
				);
				return;
			}
		}
		newDOTPoint.setName(name);
		newDOTPoint.setInfo(_dotDefinitionDialogFrame.getInfoText());
		final Object value = _translationFactorSpinner.getValue();
		if(value instanceof Number) {
			final Number number = (Number)value;
			newDOTPoint.setTranslationFactor(number.doubleValue());
		}
		newDOTPoint.setJoinByLine(_joinByLineCheckBox.isSelected());
		_dotDefinitionDialogFrame.getDotManager().saveDisplayObjectType(newDOTPoint);
		_dotDefinitionDialogFrame.setDisplayObjectType(newDOTPoint, true);
	}

	private JPanel createStaticCenterPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		final PrimitiveFormType primitiveFormType = primitiveForm.getType();
		if(primitiveFormType.equals(PrimitiveFormType.PUNKT)) {
			if(property.equals(DOTProperty.DURCHMESSER)) {
				return createStaticDiameterPanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.FARBE)) {
				return createStaticColorPanel(primitiveForm, property);
			}
		}
		else if(primitiveFormType.equals(PrimitiveFormType.RECHTECK) ||
		        primitiveFormType.equals(PrimitiveFormType.KREIS) ||
		        primitiveFormType.equals(PrimitiveFormType.HALBKREIS)) {
			if(property.equals(DOTProperty.FUELLUNG)) {
				return createStaticColorPanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.STRICHBREITE)) {
				return createStaticStrokeWidthPanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.TRANSPARENZ)) {
				return createStaticTransparencyPanel(primitiveForm, property);
			}
		}
		else if(primitiveFormType.equals(PrimitiveFormType.TEXTDARSTELLUNG)) {
			if(property.equals(DOTProperty.FARBE)) {
				return createStaticColorPanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.GROESSE)) {
				return createStaticTextsizePanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.TEXT)) {
				return createStaticTextPanel(primitiveForm, property);
			}
			else if(property.equals(DOTProperty.TEXTSTIL)) {
				return createStaticTextstylePanel(primitiveForm, property);
			}
		}
		return null;
	}

	@SuppressWarnings("serial")
	class StaticPanel extends JPanel {

		/**
		 * Konstruiert aus Grundfigur und Eigenschaft ein Objekt.
		 *
		 * @param primitiveForm die Grundfigur
		 * @param property      die Eigenschaft
		 */
		public StaticPanel(final PrimitiveForm primitiveForm, final DOTProperty property) {
			super();
			_primitiveForm = primitiveForm;
			_property = property;
		}

		/**
		 * Gibt die einzige Combobox oder <code>null</code> zurück.
		 *
		 * @return die einzige Combobox oder <code>null</code>
		 */
		public JComboBox getComboBox() {
			final Component[] components = getComponents();
			for(Component component : components) {
				if(component instanceof JComboBox) {
					return (JComboBox)component;
				}
			}
			return null;
		}

		private void setValue(final JComponent component) {
			final String primitiveFormName = _primitiveForm.getName();
			if(_dotDefinitionDialogFrame.isPropertyStatic(primitiveFormName, _property)) {
				Object object = _dotDefinitionDialogFrame.getValueOfStaticProperty(primitiveFormName, _property);
				if(component instanceof JComboBox) {
					final String currentValue;
					if(object instanceof Color) {
						currentValue = ColorManager.getInstance().getColorName((Color)object);
					}
					else if(object instanceof Integer) {
						Integer i = (Integer)object;
						if(i.equals(Font.PLAIN)) {
							currentValue = STANDARD_FONT;
						}
						else if(i.equals(Font.BOLD)) {
							currentValue = BOLD_FONT;
						}
						else if(i.equals(Font.ITALIC)) {
							currentValue = ITALIC_FONT;
						}
						else {
							currentValue = "Unbekannter Font";
						}
					}
					else {
						currentValue = (String)object;
					}
					JComboBox comboBox = (JComboBox)component;
					comboBox.setSelectedItem(currentValue);
				}
				else if(component instanceof JSpinner) {
					JSpinner spinner = (JSpinner)component;
					if(object instanceof Integer) {
						spinner.setValue((Integer)object);
					}
					else {
						spinner.setValue((Double)object);
					}
				}
			}
			else {
				if(component instanceof JComboBox) {
					JComboBox comboBox = (JComboBox)component;
					comboBox.setSelectedItem(DOTPoint.getDefaultValue(_property));
				}
				else if(component instanceof JSpinner) {
					JSpinner spinner = (JSpinner)component;
					spinner.setValue(DOTPoint.getDefaultValue(_property));
				}
			}
		}

		private void addListener(final JComponent component) {
			if(component instanceof JComboBox) {
				ItemListener itemListener = new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						JComboBox comboBox = (JComboBox)component;
						final Object selectedItem = comboBox.getSelectedItem();
						_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfStaticProperty(
								_primitiveForm.getName(), _property, selectedItem
						);
					}
				};
				JComboBox comboBox = (JComboBox)component;
				comboBox.addItemListener(itemListener);
			}
			else if(component instanceof JSpinner) {
				javax.swing.event.ChangeListener changeListener = new javax.swing.event.ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						JSpinner spinner = (JSpinner)component;
						final Object value = spinner.getValue();
						_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfStaticProperty(
								_primitiveForm.getName(), _property, value
						);
					}
				};
				JSpinner spinner = (JSpinner)component;
				spinner.addChangeListener(changeListener);
			}
		}

		final private PrimitiveForm _primitiveForm;

		final private DOTProperty _property;
	}

	private StaticPanel createStaticDiameterPanel(
			final PrimitiveForm primitiveForm, final DOTProperty property) {
		return createStaticSpinnerPanel(
				primitiveForm, property, "Durchmesser: ", getNewDiameterSpinnerModel()
		);
	}

	private StaticPanel createStaticColorPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		return createStaticComboBoxPanel(
				primitiveForm, property, "Farbe: ", ColorManager.getInstance().getColorNames()
		);
	}

	private StaticPanel createStaticStrokeWidthPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		return createStaticSpinnerPanel(
				primitiveForm, property, "Strichbreite: ", getNewStrokeWidthSpinnerModel()
		);
	}

	private StaticPanel createStaticTransparencyPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		return createStaticSpinnerPanel(
				primitiveForm, property, "Tranzparenz in %: ", getNewTransparencySpinnerModel()
		);
	}

	private StaticPanel createStaticTextsizePanel(PrimitiveForm primitiveForm, DOTProperty property) {
		return createStaticSpinnerPanel(
				primitiveForm, property, "Schriftgröße: ", getNewTextSizeSpinnerModel()
		);
	}

	private StaticPanel createStaticTextPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		StaticPanel theTextPanel = createStaticComboBoxPanel(
				primitiveForm, property, "Text: ", DOTPointPainter.STATIC_TEXT_ITEMS
		);
		final JComboBox theComboBox = theTextPanel.getComboBox();
		if(theComboBox == null) {
			return theTextPanel;
		}

		ItemListener comboBoxItemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					if(theComboBox.getSelectedIndex() == DOTPointPainter.STATIC_TEXT_ITEMS.length - 1) {
						theComboBox.setEditable(true);
					}
					else {
						theComboBox.setEditable(false);
					}
				}
			}
		};
		theComboBox.addItemListener(comboBoxItemListener);

		ActionListener editorActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int length = DOTPointPainter.STATIC_TEXT_ITEMS.length;
				final int index = length - 1;
				final ComboBoxEditor editor = theComboBox.getEditor();
				String editedItem = (String)editor.getItem();
				theComboBox.insertItemAt(editedItem, index);
				final DefaultComboBoxModel model = (DefaultComboBoxModel)theComboBox.getModel();
				if(model.getSize() > length) { // Sieht komisch aus, aber der direkte Weg ging nicht.
					model.removeElementAt(length);
				}
			}
		};
		theComboBox.getEditor().addActionListener(editorActionListener);

		return theTextPanel;
	}

	private StaticPanel createStaticTextstylePanel(PrimitiveForm primitiveForm, DOTProperty property) {
		return createStaticComboBoxPanel(primitiveForm, property, "Textstil: ", FONT_ITEMS);
	}

	private StaticPanel createStaticComboBoxPanel(
			PrimitiveForm primitiveForm, DOTProperty property, String labelText, Object[] items) {
		JLabel label = new JLabel(labelText);
		JComboBox comboBox = new JComboBox(items);
		comboBox.setMaximumSize(new Dimension(300, 25));
		comboBox.setEnabled(_dotDefinitionDialogFrame.isEditable());

		StaticPanel thePanel = new StaticPanel(primitiveForm, property);
		thePanel.setMaximumSize(new Dimension(300, 25));
		thePanel.setLayout(new SpringLayout());
		thePanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		thePanel.add(label);
		thePanel.add(comboBox);
		SpringUtilities.makeCompactGrid(thePanel, 2, 5, 5);

		thePanel.setValue(comboBox);
		thePanel.addListener(comboBox);

		return thePanel;
	}

	private StaticPanel createStaticSpinnerPanel(
			PrimitiveForm primitiveForm, DOTProperty property, String labelText, SpinnerModel spinnerModel) {
		JLabel label = new JLabel(labelText);
		JSpinner spinner = new JSpinner(spinnerModel);
		spinner.setMaximumSize(new Dimension(200, 20));
		spinner.setEnabled(_dotDefinitionDialogFrame.isEditable());

		StaticPanel thePanel = new StaticPanel(primitiveForm, property);
		thePanel.setLayout(new SpringLayout());
		thePanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		thePanel.add(label);
		thePanel.add(spinner);
		SpringUtilities.makeCompactGrid(thePanel, 2, 5, 5);

		thePanel.setValue(spinner);
		thePanel.addListener(spinner);

		return thePanel;
	}

	private JPanel createDynamicCenterPanel(PrimitiveForm primitiveForm, DOTProperty property) {
		final JTable theTable = new JTable();
		PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair(primitiveForm.getName(), property);
		_dynamicTables.put(pfpPair, theTable);
		DOTPoint dotPoint = (DOTPoint)_dotDefinitionDialogFrame.getScratchDisplayObjectType();
		final DynamicDOTItemManager tableModel = (DynamicDOTItemManager)dotPoint.getTableModel(primitiveForm, property);
		theTable.setModel(tableModel);

		class NumberComparator implements Comparator<Number> {

			public int compare(Number o1, Number o2) {
				final double d1 = o1.doubleValue();
				final double d2 = o2.doubleValue();
				if(d1 < d2) {
					return -1;
				}
				if(d1 == d2) {
					return 0;
				}
				return 1;
			}
		}
		TableRowSorter<DynamicDOTItemManager> tableRowSorter = new TableRowSorter<DynamicDOTItemManager>();
		tableRowSorter.setModel(tableModel);
		tableRowSorter.setComparator(4, new NumberComparator());
		tableRowSorter.setComparator(5, new NumberComparator());
		theTable.setRowSorter(tableRowSorter);

		JButton newDOTItemButton = new JButton("Neue Zeile");
		newDOTItemButton.setEnabled(_dotDefinitionDialogFrame.isEditable());
		JButton deleteDOTItemButton = new JButton("Zeile löschen");
		deleteDOTItemButton.setEnabled(false);
		JButton showConflictsButton = new JButton("Zeige Konflikte");

		addButtonListeners(primitiveForm, property, newDOTItemButton, deleteDOTItemButton, showConflictsButton);
		addListSelectionListener(theTable, deleteDOTItemButton);

		JPanel dotButtonsPanel = new JPanel();
		dotButtonsPanel.setLayout(new SpringLayout());

		dotButtonsPanel.add(newDOTItemButton);
		dotButtonsPanel.add(deleteDOTItemButton);
		dotButtonsPanel.add(showConflictsButton);

		dotButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(dotButtonsPanel, 1, 5, 20);

		JPanel thePanel = new JPanel();
		thePanel.setLayout(new SpringLayout());
		thePanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
		thePanel.add(new JScrollPane(theTable));
		thePanel.add(dotButtonsPanel);
		SpringUtilities.makeCompactGrid(thePanel, 2, 20, 5);

		return thePanel;
	}

	private void addButtonListeners(
			final PrimitiveForm primitiveForm,
			final DOTProperty property,
			final JButton newDOTItemButton,
			final JButton deleteDOTItemButton,
			final JButton showConflictsButton) {

		@SuppressWarnings("serial")
		class DOTItemDialog extends JDialog {

			final private PrimitiveForm _primitiveForm;

			final private DOTProperty _property;

			private JComboBox _colorComboBox = new JComboBox(ColorManager.getInstance().getColorNames());

			private JComboBox _textStyleComboBox = new JComboBox(FONT_ITEMS);

			private JComboBox _textComboBox = new JComboBox(DOTPointPainter.DYNAMIC_TEXT_ITEMS);

			private JSpinner _diameterSpinner = new JSpinner(getNewDiameterSpinnerModel());

			private JSpinner _strokeWidthSpinner = new JSpinner(getNewStrokeWidthSpinnerModel());

			private JSpinner _transparencySpinner = new JSpinner(getNewTransparencySpinnerModel());

			private JSpinner _textSizeSpinner = new JSpinner(getNewTextSizeSpinnerModel());

			final private DynamicDefinitionComponent _dynamicDefinitionComponent;

			JComponent _component = null;

			@SuppressWarnings("unchecked")
			private Object initDynamicDefinitionComponent() {
				final String selectedPrimitiveFormName = _dotDefinitionDialogFrame.getSelectedPrimitiveForm();
				final DOTProperty currentProperty = _dotDefinitionDialogFrame.getSelectedProperty();
				PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair(selectedPrimitiveFormName, currentProperty);
				final JTable workWithThisTable = _dynamicTables.get(pfpPair);

				int selectedRow = workWithThisTable.getSelectedRow();
				if(selectedRow == -1) {
					if(workWithThisTable.getModel().getRowCount() > 0) {
						selectedRow = 0;
					}
					else {
						return null;
					}
				}
				selectedRow = workWithThisTable.convertRowIndexToModel(selectedRow);
				final TableModel model = workWithThisTable.getModel();
				DynamicDOTItemManager dynamicDOTItemManager = (DynamicDOTItemManager)model;
				final DisplayObjectTypeItemWithInterval dotItemWithInterval = dynamicDOTItemManager.get(selectedRow);
				_dynamicDefinitionComponent.fillComponents(dotItemWithInterval);
				return dotItemWithInterval.getItem().getPropertyValue();
			}

			private void addButtonListeners(JButton saveButton, JButton cancelButton) {
				ActionListener actionListenerSave = new ActionListener() {
					public void actionPerformed(ActionEvent e) {

						Object propertyValue = null;
						String errorString = null;
						if(_property == DOTProperty.DURCHMESSER) {
							propertyValue = _diameterSpinner.getValue();
							errorString = "Bitte wählen Sie einen Durchmesser aus!";
						}
						else if(_property == DOTProperty.FARBE || _property == DOTProperty.FUELLUNG) {
							propertyValue = _colorComboBox.getSelectedItem();
							errorString = "Bitte wählen Sie eine Farbe aus!";
						}
						else if(_property == DOTProperty.GROESSE) {
							propertyValue = _textSizeSpinner.getValue();
							errorString = "Bitte wählen Sie eine Schriftgröße aus!";
						}
						else if(_property == DOTProperty.STRICHBREITE) {
							propertyValue = _strokeWidthSpinner.getValue();
							errorString = "Bitte wählen Sie eine Strichbreite  aus!";
						}
						else if(_property == DOTProperty.TEXT) {
							propertyValue = _textComboBox.getSelectedItem();
							errorString = "Bitte wählen Sie einen Text aus!";
						}
						else if(_property == DOTProperty.TEXTSTIL) {
							propertyValue = _textStyleComboBox.getSelectedItem();
							errorString = "Bitte wählen Sie einen Textstil aus!";
						}
						else if(_property == DOTProperty.TRANSPARENZ) {
							propertyValue = _transparencySpinner.getValue();
							errorString = "Bitte wählen Sie eine Tranparenz aus!";
						}
						else {
							errorString = "DOTPointDialog.addButtonListeners(): unbehandelte Eigenschaft!";
						}
						if(propertyValue == null) {
							JOptionPane.showMessageDialog(
									new JFrame(), errorString, "Fehler", JOptionPane.ERROR_MESSAGE
							);
							return;
						}

						final String description = _dynamicDefinitionComponent.getInfoText();
						if(description == null) {
							JOptionPane.showMessageDialog(
									new JFrame(), "Bitte geben Sie einen Info-Text ein!", "Fehler", JOptionPane.ERROR_MESSAGE
							);
							return;
						}

						final String attributeGroupName = _dynamicDefinitionComponent.getAttributeGroupName();
						if(attributeGroupName == null) {
							JOptionPane.showMessageDialog(
									new JFrame(), "Bitte wählen Sie eine Attributgruppe aus!", "Fehler", JOptionPane.ERROR_MESSAGE
							);
							return;
						}

						final String aspectName = _dynamicDefinitionComponent.getAspectName();
						if(aspectName == null) {
							JOptionPane.showMessageDialog(
									new JFrame(), "Bitte wählen Sie einen Aspekt aus!", "Fehler", JOptionPane.ERROR_MESSAGE
							);
							return;
						}

						final String attributeName = _dynamicDefinitionComponent.getAttributeName();
						if(attributeName == null) {
							int error = _dynamicDefinitionComponent.checkAttributeName();
							if(error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Bitte wählen Sie ein Attribut aus!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
							else if(error == 2) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Der Attributname ist ungültig!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
						}

						final Double fromValue = _dynamicDefinitionComponent.getFromValue();
						if(fromValue == null) {
							int error = _dynamicDefinitionComponent.checkFromValue();
							if(error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Bitte tragen Sie einen Von-Wert ein!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
							else if((error == 2) || (error == 3)) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Bitte korrigieren Sie den Von-Wert!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
						}

						final Double toValue = _dynamicDefinitionComponent.getToValue();
						if(toValue == null) {
							int error = _dynamicDefinitionComponent.checkToValue();
							if(error == 1) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Bitte tragen Sie einen Bis-Wert ein!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
							else if((error == 2) || (error == 3)) {
								JOptionPane.showMessageDialog(
										new JFrame(), "Bitte korrigieren Sie den Bis-Wert!", "Fehler", JOptionPane.ERROR_MESSAGE
								);
								return;
							}
						}

						if((fromValue != null) && (toValue != null) && fromValue > toValue) {
							JOptionPane.showMessageDialog(
									new JFrame(), "Der Von-Wert ist größer als der Bis-Wert!", "Fehler", JOptionPane.ERROR_MESSAGE
							);
							return;
						}

						DynamicDOTItem dItem = new DynamicDOTItem(
								attributeGroupName, aspectName, attributeName, description, propertyValue
						);
						_dotDefinitionDialogFrame.getScratchDisplayObjectType().setValueOfDynamicProperty(
								_primitiveForm.getName(), _property, dItem, fromValue, toValue
						);
						_dotDefinitionDialogFrame.setSomethingChanged(true);
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

			DOTItemDialog() {
				super();
				_primitiveForm = primitiveForm;
				_property = property;

				_dynamicDefinitionComponent = new DynamicDefinitionComponent(
						_configuration, new DOTPointPlugin()
				);
				final Object propertyValue = initDynamicDefinitionComponent();

				final JPanel panel = new JPanel();
				panel.setLayout(new SpringLayout());

				JLabel aLabel = null;
				if(_property == DOTProperty.DURCHMESSER) {
					aLabel = new JLabel("Durchmesser: ");
					if(propertyValue != null) {
						_diameterSpinner.setValue(propertyValue);
					}
					_component = _diameterSpinner;
				}
				else if(_property == DOTProperty.FARBE || _property == DOTProperty.FUELLUNG) {
					aLabel = new JLabel("Farbe: ");
					if(propertyValue != null) {
						_colorComboBox.setSelectedItem(propertyValue);
					}
					_component = _colorComboBox;
				}
				else if(_property == DOTProperty.GROESSE) {
					aLabel = new JLabel("Schriftgröße: ");
					if(propertyValue != null) {
						_textSizeSpinner.setValue(propertyValue);
					}
					_component = _textSizeSpinner;
				}
				else if(_property == DOTProperty.STRICHBREITE) {
					aLabel = new JLabel("Strichbreite: ");
					if(propertyValue != null) {
						_strokeWidthSpinner.setValue(propertyValue);
					}
					_component = _strokeWidthSpinner;
				}
				else if(_property == DOTProperty.TEXT) {
					aLabel = new JLabel("Text: ");
					prepareTextComboBox(propertyValue);
					_component = _textComboBox;
				}
				else if(_property == DOTProperty.TEXTSTIL) {
					aLabel = new JLabel("Textstil: ");
					if(propertyValue != null) {
						_textStyleComboBox.setSelectedItem(propertyValue);
					}
					_component = _textStyleComboBox;
				}
				else if(_property == DOTProperty.TRANSPARENZ) {
					aLabel = new JLabel("Tranzparenz: ");
					if(propertyValue != null) {
						_transparencySpinner.setValue(propertyValue);
					}
					_component = _transparencySpinner;
				}
				panel.add(aLabel);
				panel.add(_component);

				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
				SpringUtilities.makeCompactGrid(panel, 2, 20, 5);

				final JButton saveButton = new JButton("Speichern");
				saveButton.setEnabled(_dotDefinitionDialogFrame.isEditable());
				final JButton cancelButton = new JButton("Dialog schließen");

				final JPanel buttonsPanel = new JPanel();
				buttonsPanel.setLayout(new SpringLayout());

				buttonsPanel.add(saveButton);
				buttonsPanel.add(cancelButton);

				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
				SpringUtilities.makeCompactGrid(buttonsPanel, 2, 20, 5);
				addButtonListeners(saveButton, cancelButton);

				setTitle("GND: Darstellungsfestlegung für Punkte");
				setLayout(new BorderLayout());
				add(new JScrollPane(panel), BorderLayout.NORTH);
				add(new JScrollPane(_dynamicDefinitionComponent), BorderLayout.CENTER);
				add(buttonsPanel, BorderLayout.SOUTH);
				pack();
				setSize(700, 550);
				final Rectangle bounds = getBounds();
				setLocation(new Point((int)(bounds.getHeight() / 1.5 + 300), (int)(bounds.getWidth() / 1.5)));
			}

			private void prepareTextComboBox(final Object propertyValue) {
				// _textComboBox wird in der letzten Zeile editierbar gemacht, und es wird
				// der übergebene Wert in diese Zeile eingetragen und selektiert, wenn er nicht
				// einem der anderen Einträge entspricht.

				if(propertyValue != null) {
					String propertyValueAsString = (String)propertyValue;
					final int size = _textComboBox.getModel().getSize();
					boolean propertyValueFound = false;
					for(int i = 0; i < size - 1; i++) {
						if(propertyValueAsString.equals((String)_textComboBox.getItemAt(i))) {
							propertyValueFound = true;
							_textComboBox.setSelectedIndex(i);
							break;
						}
					}
					if(!propertyValueFound) {
						_textComboBox.removeItemAt(size - 1);
						_textComboBox.addItem(propertyValueAsString);
						_textComboBox.setSelectedIndex(size - 1);
					}
				}

				ItemListener textComboBoxItemListener = new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if(e.getStateChange() == ItemEvent.SELECTED) {
							if(_textComboBox.getSelectedIndex() == DOTPointPainter.DYNAMIC_TEXT_ITEMS.length - 1) {
								_textComboBox.setEditable(true);
							}
							else {
								_textComboBox.setEditable(false);
							}
						}
					}
				};
				_textComboBox.addItemListener(textComboBoxItemListener);

				ActionListener editorActionListener = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						final int length = DOTPointPainter.DYNAMIC_TEXT_ITEMS.length;
						final int index = length - 1;
						final ComboBoxEditor editor = _textComboBox.getEditor();
						String editedItem = (String)editor.getItem();
						_textComboBox.insertItemAt(editedItem, index);
						final DefaultComboBoxModel model = (DefaultComboBoxModel)_textComboBox.getModel();
						if(model.getSize() > length) { // Sieht komisch aus, aber der direkte Weg ging nicht.
							model.removeElementAt(length);
						}
					}
				};
				_textComboBox.getEditor().addActionListener(editorActionListener);
			}

			public void runDialog() {
				setVisible(true);
			}
		}

		ActionListener actionListenerNewButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DOTItemDialog dotItemDialog = new DOTItemDialog();
				dotItemDialog.runDialog();
			}
		};
		newDOTItemButton.addActionListener(actionListenerNewButton);

		ActionListener actionListenerDeleteButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String selectedPrimitiveFormName = _dotDefinitionDialogFrame.getSelectedPrimitiveForm();
				final DOTProperty currentProperty = _dotDefinitionDialogFrame.getSelectedProperty();
				PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair(selectedPrimitiveFormName, currentProperty);
				final JTable workForThisTable = _dynamicTables.get(pfpPair);
				final int[] selectedRows = workForThisTable.getSelectedRows();
				if(selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(), "Bitte wählen Sie eine Zeile aus!", "Fehler", JOptionPane.ERROR_MESSAGE
					);
					return;
				}
				final TableModel model = workForThisTable.getModel();
				DynamicDOTItemManager dynamicDOTItemManager = (DynamicDOTItemManager)model;
				Set<Integer> selectedModelIndexes = new TreeSet<Integer>();
				for(int j = 0; j < selectedRows.length; j++) {
					selectedModelIndexes.add(workForThisTable.convertRowIndexToModel(selectedRows[j]));
				}
				final Object[] array = selectedModelIndexes.toArray();
				for(int i = array.length - 1; i >= 0; i--) {
					dynamicDOTItemManager.remove((Integer)array[i]);
					_dotDefinitionDialogFrame.setSomethingChanged(true);
				}
			}
		};
		deleteDOTItemButton.addActionListener(actionListenerDeleteButton);

		ActionListener actionListenerShowConflictsButton = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String selectedPrimitiveFormName = _dotDefinitionDialogFrame.getSelectedPrimitiveForm();
				final DOTProperty currentProperty = _dotDefinitionDialogFrame.getSelectedProperty();
				PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair(selectedPrimitiveFormName, currentProperty);
				final JTable workForThisTable = _dynamicTables.get(pfpPair);
				workForThisTable.clearSelection();

				DOTPoint dotPoint = (DOTPoint)_dotDefinitionDialogFrame.getScratchDisplayObjectType();
				final PrimitiveForm selectedPrimitiveForm = dotPoint.getPrimitiveForm(selectedPrimitiveFormName);
				final Set<Integer> conflictingRows = dotPoint.getConflictingRows(selectedPrimitiveForm, currentProperty);
				ListSelectionModel listSelectionModel = workForThisTable.getSelectionModel();
				for(Integer row : conflictingRows) {
					Integer rowInView = workForThisTable.convertRowIndexToView(row);
					listSelectionModel.addSelectionInterval(rowInView, rowInView);
				}
				workForThisTable.setSelectionModel(listSelectionModel);
			}
		};
		showConflictsButton.addActionListener(actionListenerShowConflictsButton);
	}

	private void addListSelectionListener(final JTable table, final JButton deleteDOTItemButton) {
		final ListSelectionModel selectionModel = table.getSelectionModel();
		ListSelectionListener listSelectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = table.getSelectedRow();
				if(selectedRow == -1) {
					deleteDOTItemButton.setEnabled(false);
				}
				else {
					deleteDOTItemButton.setEnabled(_dotDefinitionDialogFrame.isEditable());
				}
			}
		};
		selectionModel.addListSelectionListener(listSelectionListener);
	}

	private void addToolTips() {
		_translationFactorSpinner.setToolTipText("Für Punkte, die auf einer Straße liegen, wird die Darstellung orthogonal um diese Länge verschoben.");
		_joinByLineCheckBox.setToolTipText("Hier wird festgelegt, ob die verschobene Darstellung mit dem Lagepunkt durch eine Linie verbunden wird.");
	}

	private SpinnerNumberModel getNewDiameterSpinnerModel() {
		return new SpinnerNumberModel(0, 0, 10000, 1);
	}

	private SpinnerNumberModel getNewStrokeWidthSpinnerModel() {
		return new SpinnerNumberModel(0.5, 0.0, 10000.0, 0.1);
	}

	private SpinnerNumberModel getNewTransparencySpinnerModel() {
		return new SpinnerNumberModel(0, 0, 100, 1);
	}

	private SpinnerNumberModel getNewTextSizeSpinnerModel() {
		return new SpinnerNumberModel(20, 0, 10000, 1);
	}

	final public static String STANDARD_FONT = "Standard";

	final public static String ITALIC_FONT = "Kursiv";

	final public static String BOLD_FONT = "Fett";

	final private static Object[] FONT_ITEMS = {STANDARD_FONT, ITALIC_FONT, BOLD_FONT};
}
