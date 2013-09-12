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

import de.kappich.pat.gnd.displayObjectToolkit.DOTItemManager.DisplayObjectTypeItemWithInterval;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.utils.SpringUtilities;

import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.Attribute;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.AttributeListDefinition;
import de.bsvrz.dav.daf.main.config.AttributeSet;
import de.bsvrz.dav.daf.main.config.AttributeType;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DoubleAttributeType;
import de.bsvrz.dav.daf.main.config.IntegerAttributeType;
import de.bsvrz.dav.daf.main.config.IntegerValueRange;
import de.bsvrz.dav.daf.main.config.IntegerValueState;
import de.bsvrz.dav.daf.main.config.ReferenceAttributeType;
import de.bsvrz.dav.daf.main.config.StringAttributeType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.dav.daf.main.config.TimeAttributeType;
import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * Das Panel, mit dessen Hilfe man die Informationen einer Anmeldung auf dynamische Daten definiert.
 * Eine dynamische Eigenschaft is eine Zuordnung von Anmeldedaten, bestehend aus Attributgruppe, Aspekt,
 * Attribut/Status, und gegebenenfalls unteren und oberen Schranken der Attributwerte zu jeweils einem 
 * Wert der Eigenschaft, z.B. einer Farbe. Dieses Panel stellt die Funktionalität für die fehlerfreie
 * und bequeme Zusammenstellung der Anmeldedaten zur Verfügung, und wird in den Plugins jeweils in
 * den Defintionsdialogen der Darstellungstypen verwendet.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
@SuppressWarnings("serial")
public class DynamicDefinitionComponent extends JPanel {
	
	private final DataModel _configuration;
	
	private final DisplayObjectTypePlugin _dotPlugin;
	
	final private JTextField _infoField = new JTextField();
	
	final private JCheckBox _objectTypeFilterCheckBox = new JCheckBox();
	
	final private JComboBox _objectTypeFilterComboBox = new JComboBox();
	
	final private JComboBox _attributeGroupComboBox = new JComboBox();
	
	final private JComboBox _aspectComboBox = new JComboBox();
	
	final private JComboBox _attributeNameComboBox = new JComboBox();
	
	final private JTextField _attributeNameTextField = new JTextField();
	
	final private JComboBox _fromUnscaledComboBox  = new JComboBox();
	
	final private JComboBox _fromStateComboBox = new JComboBox();
	
	final private JComboBox _toUnscaledComboBox = new JComboBox();
	
	final private JComboBox _toStateComboBox  = new JComboBox();
	
	// AttributeGroup-Pid -> Objekt-Typ
	final private Map<String, SystemObjectType> _objectTypeMap = new HashMap<String, SystemObjectType>();
	
	final public static String LEERE_DATEN_STATUS = "<keine Daten (übergreifender Status)>";
	final public static String KEINE_DATEN_STATUS = "<keine Daten von der Quelle>";
	final public static String KEINE_QUELLE_STATUS = "<keine Quelle>";
	final public static String KEINE_RECHTE_STATUS = "<keine Rechte>";
	
	final private static Debug _debug = Debug.getLogger();
	
	/**
	 * Konstruiert ein Objekt der Klasse und initialisiert die Auswahlboxen.
	 * 
	 * @param configuration die Konfiguration
	 * @param dotPlugin das Darstellungstypen-Plugin
	 */
	public DynamicDefinitionComponent ( final DataModel configuration, 
			final DisplayObjectTypePlugin dotPlugin ) {
		super();
		_configuration = configuration;
		_dotPlugin = dotPlugin;
		
		JLabel infoLabel = new JLabel("Info: ");
		JLabel objectTypLabel = new JLabel("Objekttypfilter: ");
		JPanel objectTypePanel = getObjectTypePanel();
		JLabel atgLabel = new JLabel("Attributgruppe: ");
		final SystemObjectType objectType = _configuration.getType( _dotPlugin.getGeometryType());
		Collection<String> atgs = new TreeSet<String>();
		getAttributeGroups( objectType, atgs);
		_attributeGroupComboBox.setModel( new DefaultComboBoxModel( atgs.toArray()));
		addATGItemListener();
		JLabel aspectLabel = new JLabel("Aspekt: ");
		JLabel attributeNameLabel = new JLabel("Attributname/Status: ");
		JLabel aDummyLabel = new JLabel("");
		addAttributeNameListener();
		JLabel fromRawLabel = new JLabel("Von-Wert unskaliert: ");
		JLabel fromStateLabel = new JLabel("       skaliert/Zustand: ");
		JLabel toRawLabel = new JLabel("Bis-Wert unskaliert: ");
		JLabel toStateLabel = new JLabel("       skaliert/Zustand: ");
		addFromToListeners();
		
		setTooltips();
		
		setLayout( new SpringLayout());
		
		add( infoLabel);
		add( _infoField);
		add( objectTypLabel);
		add( objectTypePanel);
		add( atgLabel);
		add( _attributeGroupComboBox);
		add( aspectLabel);
		add( _aspectComboBox);
		add( attributeNameLabel);
		add( _attributeNameComboBox);
		add( aDummyLabel);
		add( _attributeNameTextField);
		add( fromRawLabel);
		add( _fromUnscaledComboBox);
		add( fromStateLabel);
		add( _fromStateComboBox);
		add( toRawLabel);
		add( _toUnscaledComboBox);
		add( toStateLabel);
		add( _toStateComboBox);
		
		final Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
		setBorder(BorderFactory.createTitledBorder(border, "Definition der Anmeldung"));
		SpringUtilities.makeCompactGrid( this, 2, 20, 5);
	}
	
	/*
	 * Setzen der Tooltipps im Konstruktor
	 */
	private void setTooltips() {
		_infoField.setToolTipText( "Bitte geben Sie eine kurze, aussagekräftige Beschreibung ein, die in der Legende benutzt werden kann.");
		_objectTypeFilterCheckBox.setToolTipText( "Das Hinzuschalten des Objektfilters dient nur der Einschränkung der möglichen Attributgruppen.");
		_objectTypeFilterComboBox.setToolTipText( "Das Hinzuschalten des Objektfilters dient nur der Einschränkung der möglichen Attributgruppen.");
	}
			
	/**
	 * Mit dieser Methode werden die Textfelder und Comboboxen mit Inhalten, die
	 * aus dem übergebenen Item und Interval stammen, gefüllt.
	 * 
	 * @param dotItemWithInterval ein Item mit zugehörigem Intervall
	 */
	@SuppressWarnings("unchecked")
	public void fillComponents( final DisplayObjectTypeItemWithInterval dotItemWithInterval) {
		DynamicDOTItem dynamicItem = (DynamicDOTItem) dotItemWithInterval.getItem();
		_infoField.setText( dynamicItem.getDescription());
		_attributeGroupComboBox.setSelectedItem( dynamicItem.getAttributeGroup());
		_aspectComboBox.setSelectedItem( dynamicItem.getAspect());
		
		// Attributnamen sind etwas kniffelig
		final String attributeName = dynamicItem.getAttributeName();
		final Integer similarIndex = getIndexForSimilarObjectInAttributeNameComboBox( attributeName);
		if ( similarIndex != null )  {
			_attributeNameComboBox.setSelectedIndex( similarIndex);
		}
		_attributeNameTextField.setText( attributeName);
		
		// Von- und Bis-Felder auch:
		final Integer lowerBoundAsInt = dotItemWithInterval.getInterval().getLowerBound().intValue();
		updateBoundDisplayed( dotItemWithInterval, lowerBoundAsInt, _fromUnscaledComboBox, _fromStateComboBox);
		final Integer upperBoundAsInt = dotItemWithInterval.getInterval().getUpperBound().intValue();
		updateBoundDisplayed( dotItemWithInterval, upperBoundAsInt, _toUnscaledComboBox, _toStateComboBox);
	}
			
	/*
	 * Bestandteil von fillComponents(); setzt die Von- und Bis-Felder.
	 */
	@SuppressWarnings("unchecked")
	private void updateBoundDisplayed ( 
			final DisplayObjectTypeItemWithInterval dotItemWithInterval,
			final Integer theBound,
			final JComboBox unscaledComboBox, 
			final JComboBox stateComboBox ) {
		AttributeGroup atg = _configuration.getAttributeGroup( (String) _attributeGroupComboBox.getSelectedItem());
		final Attribute attribute = getAttribute( atg, (String) _attributeNameComboBox.getSelectedItem());
		if ( attribute != null) {
			final AttributeType attributeType = attribute.getAttributeType();
			if ( attributeType instanceof IntegerAttributeType) {
				final String theBoundAsString = theBound.toString();
				final ComboBoxModel model = unscaledComboBox.getModel();
				boolean selectionMade = false;
				for ( int index = 0; index < model.getSize(); index++) {
					final String s = (String) model.getElementAt(index);
					if ( s.equals( theBoundAsString)) {
						unscaledComboBox.setSelectedIndex( index);
						selectionMade = true;
						break;
					}
				}
				if ( !selectionMade) {
					unscaledComboBox.addItem( theBoundAsString);
					final String anObject = "<vom Benutzer definierter Wert>";
					stateComboBox.addItem(anObject);
					unscaledComboBox.setSelectedItem( theBoundAsString);
					stateComboBox.setSelectedItem(anObject);
				}
			} else {
				final Double lowerBound = (Double) dotItemWithInterval.getInterval().getLowerBound();
				final String lowerBoundAsString = lowerBound.toString().replace('.', ',');
				unscaledComboBox.addItem( lowerBoundAsString);
				final String anObject = "<vom Benutzer definierter Wert>";
				stateComboBox.addItem(anObject);
				unscaledComboBox.setSelectedItem( lowerBoundAsString);
				stateComboBox.setSelectedItem(anObject);
			}
		}
	}
			
	/*
	 * Wird in fillComponents() benutzt, um den Index zur Selektion in der _attributeNameComboBox
	 * zu bekommen.
	 */
	private Integer getIndexForSimilarObjectInAttributeNameComboBox( final String attributeName) {
		if ( attributeName == null) {
			return null;
		}
		final String reducedAttributeName = deleteArraysFromString( attributeName, false);
		if ( reducedAttributeName == null) {
			return null;
		}
		final ComboBoxModel model = _attributeNameComboBox.getModel();
		final int size = model.getSize();
		for ( int i = 0; i < size; i++) {
			final String r = deleteArraysFromString( (String) model.getElementAt( i), true);
			if ( (r != null) && r.equals( reducedAttributeName)) {
				return i;
			}
		}
		return null;
	}
			
	/*
	 * Entfernt die Arrays-Beschreibungen aus dem String s, der ein komplexer Attributname sein kann.
	 */
	private String deleteArraysFromString( final String s, boolean rangeDescription) {
		String r = s;
		int indexOfOpenBracket = r.indexOf('[');
		while ( indexOfOpenBracket != -1) {
			int indexClosedBracket = r.indexOf(']');
			if ( indexOfOpenBracket > indexClosedBracket) { // String ist nicht wohl formatiert
				return null;
			}
			String t = r.substring( indexOfOpenBracket+1, indexClosedBracket);
			if ( rangeDescription) {
				if ( !t.substring(0,3).equals("0..") ) {
					return null;
				}
				String tsub = t.substring(3);
				if ( !(tsub.equals(".") || tsub.matches("[0-9]++"))) {
					return null;
				}
			} else {
				if ( !t.matches("[0-9]++")) {
					return null;
				}
			}
			r = r.substring(0, indexOfOpenBracket) + r.substring( indexClosedBracket+1);
			indexOfOpenBracket = r.indexOf('[');
		}
		return r;
	}
			
	/*
	 * Berechnet aus den übergebenen Informationen das Object der Klasse Attribut.
	 * Der String attributeDescriptionString kann ein komplexer Attributname sein.
	 */
	private Attribute getAttribute( 
			final AttributeSet attributeSet, 
			String attributeDescriptionString) {
		int indexOfOpenBracket = attributeDescriptionString.indexOf('[');
		int indexOfPoint = attributeDescriptionString.indexOf('.');
		int c = 0;	// 1 = [, 2 = .
		if ( (indexOfOpenBracket == -1) && (indexOfPoint == -1)) {
			return attributeSet.getAttribute( attributeDescriptionString);
		} else if ( (indexOfOpenBracket >= 0) && (indexOfPoint == -1) ) {
			c = 1;
		} else if ( (indexOfOpenBracket == -1) && (indexOfPoint >= 0)) {
			c = 2;
		} else if ( (indexOfOpenBracket >= 0) && (indexOfPoint >= 0)) {
			if ( indexOfOpenBracket < indexOfPoint ) {
				c = 1;
			} else {
				c = 2;
			}
		}
		if ( c == 1) {
			String attributeName = attributeDescriptionString.substring(0, indexOfOpenBracket);
			final Attribute attribute = attributeSet.getAttribute(attributeName);
			if ( attribute == null) {
				return null;
			}
			if ( !attribute.isArray()) {
				return null;
			}
			int indexOfClosingBracket = attributeDescriptionString.indexOf(']');
			if ( indexOfClosingBracket == -1) {
				return null;
			}
			String remainingDescription = attributeDescriptionString.substring(indexOfClosingBracket+2);
			if ( remainingDescription.length() == 0) {
				return attribute;
			} else {
				final AttributeType attributeType = attribute.getAttributeType();
				if ( attributeType instanceof AttributeListDefinition) {
					AttributeListDefinition ald = (AttributeListDefinition) attributeType;
					return getAttribute( ald, remainingDescription);
				} else {
					return null;
				}
			}
		} else {
			String attributeName = attributeDescriptionString.substring(0, indexOfPoint);
			final Attribute attribute = attributeSet.getAttribute(attributeName);
			if ( attribute == null) {
				return null;
			}
			String remainingDescription = attributeDescriptionString.substring(indexOfPoint+1);
			if ( remainingDescription.isEmpty()) {
				return attribute;
			}
			final AttributeType attributeType = attribute.getAttributeType();
			if ( attributeType instanceof AttributeListDefinition) {
				AttributeListDefinition ald = (AttributeListDefinition) attributeType;
				return getAttribute( ald, remainingDescription);
			} else {
				return null;
			}
		}
	}
			
	/*
	 * Erzeugt das Subpanel, welches die Objekttypefilter-Checkbox und die zugehörige
	 * Combobox enthält, und auch deren Abhängigkeit unter einander und zur _attributeGroupComboBox
	 * modelliert.
	 */
	private JPanel getObjectTypePanel() {
		_objectTypeFilterCheckBox.setSelected( false);
		_objectTypeFilterComboBox.setEnabled( false);
		
		Collection<String> ots = new TreeSet<String>();
		getObjectTypes( ots);
		DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel( ots.toArray());
		_objectTypeFilterComboBox.setModel( defaultComboBoxModel);
		
		class AttributeGroupUpdater {
			
			public void update() {
				final Object selectedObject = _objectTypeFilterComboBox.getSelectedItem();
				final Object selectedATG = _attributeGroupComboBox.getSelectedItem();
				if ( (selectedObject != null) && (selectedATG != null)) {
					final String selectedATGAsString = (String) selectedATG;
					final SystemObjectType type = _objectTypeMap.get( selectedATGAsString);
					if ( type != null) {
						final String selectedObjectAsString = (String) selectedObject;
						if ( selectedObjectAsString.equals( type.getPid())) {
							return; // Kein Update notwendig!
						}
					}
				}
				SystemObjectType selectedObjectType;
				if ( _objectTypeFilterCheckBox.isSelected() && (selectedObject != null)) {
					selectedObjectType = _configuration.getType( (String) selectedObject);
				} else {
					selectedObjectType = _configuration.getType( "typ.linie");
				}
				Collection<String> atgs = new TreeSet<String> ();
				getAttributeGroups( selectedObjectType, atgs);
				DefaultComboBoxModel atgComboBoxModel = (DefaultComboBoxModel) _attributeGroupComboBox.getModel();
				atgComboBoxModel.removeAllElements();
				for ( Object anObject : atgs.toArray()) {
					atgComboBoxModel.addElement(anObject);
				}
				if ( atgComboBoxModel.getIndexOf( selectedATG) != -1) {
					atgComboBoxModel.setSelectedItem( selectedATG);
				} else if ( atgComboBoxModel.getSize()>0) {
					atgComboBoxModel.setSelectedItem( atgComboBoxModel.getElementAt( 0));
				} else {
					_aspectComboBox.removeAllItems();
					_attributeNameComboBox.removeAllItems();
					_attributeNameTextField.setText( "");
					_fromStateComboBox.removeAllItems();
					_fromUnscaledComboBox.removeAllItems();
					_toStateComboBox.removeAllItems();
					_toUnscaledComboBox.removeAllItems();
				}
			}
		}
		
		ActionListener actionListenerCheckBox = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final boolean checkBoxSelected = _objectTypeFilterCheckBox.isSelected();
				if ( checkBoxSelected) {
					final Object selectedAtg = _attributeGroupComboBox.getSelectedItem();
					if ( selectedAtg != null) {
						final SystemObjectType type = _objectTypeMap.get( (String) selectedAtg);
						if ( type != null) {
							_objectTypeFilterComboBox.setSelectedItem( type.getPid());
							_objectTypeFilterComboBox.setEnabled( checkBoxSelected);
							return;
						}
					}
				}
				_objectTypeFilterComboBox.setEnabled( checkBoxSelected);
			}
		};
		_objectTypeFilterCheckBox.addActionListener( actionListenerCheckBox);
		
		ActionListener actionListenerComboBox = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AttributeGroupUpdater updater = new AttributeGroupUpdater ();
				updater.update();
			}
		};
		_objectTypeFilterComboBox.addActionListener( actionListenerComboBox);
		
		JPanel theObjectPanel = new JPanel();
		theObjectPanel.setLayout( new SpringLayout());
		
		theObjectPanel.add( _objectTypeFilterCheckBox);
		theObjectPanel.add( _objectTypeFilterComboBox);
		SpringUtilities.makeCompactGrid(theObjectPanel, 2, 0, 0);
		return theObjectPanel;
	}
			
	/*
	 * Bestimmt rekursiv die Namen aller Systemobjekttypen, die in der Objekt-
	 * Combobox erscheinen sollen. Die Initialisierung beginnt derzeit bei
	 * 'typ.typ'; man könnte sie auch tiefer beginnen lassen, wenn man eine
	 * kleinere Menge haben möchte.
	 */
	private void getObjectTypes( Collection<String> ots) {
		SystemObjectType typeType = _configuration.getType( "typ.typ");
		final List<SystemObject> objects = typeType.getObjects();
		for ( SystemObject object : objects) {
			ots.add( object.getPid());
		}
	}
			
	/*
	 * Bestimmt ausgehend von dem übergebenen SystemObjectType rekursiv die Menge aller 
	 * Attributgruppennamen, die in der entsprechenden Combobox auftauchen sollen.
	 */
	private void getAttributeGroups( SystemObjectType type, Collection<String> atgs) {
		final List<SystemObjectType> subTypes = type.getSubTypes();
		if ( !subTypes.isEmpty()) {
			for ( SystemObjectType objectType : subTypes) {
				getAttributeGroups(objectType, atgs);
			}
		} else {
			final List<AttributeGroup> attributeGroups = type.getAttributeGroups();
			for ( AttributeGroup attributeGroup : attributeGroups) {
				final Collection<AttributeGroupUsage> attributeGroupUsages = attributeGroup.getAttributeGroupUsages();
				for ( AttributeGroupUsage attributeGroupUsage : attributeGroupUsages) {
					if ( !attributeGroupUsage.isConfigurating()) {
						atgs.add( attributeGroup.getPid());
						_objectTypeMap.put( attributeGroup.getPid(), type);
						break;
					}
				}
			}
		}
	}
	
	/*
	 * Fügt der _attributeGroupComboBox den Listener hinzu, der im Falle der Änderung der
	 * Selektion dieser Box in der Aspekt-Combobox und bei den Attributnamen die nötigen
	 * Änderungen durchführt.
	 */
	private void addATGItemListener () {
		ItemListener itemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED) {
					AttributeGroup atg = _configuration.getAttributeGroup( (String) e.getItem());
					SortedSet<String> aspectSet = new TreeSet<String>();
					for ( Aspect aspect : atg.getAspects()) {
						aspectSet.add( aspect.getPid());
					}
					_aspectComboBox.setModel( new DefaultComboBoxModel( aspectSet.toArray()));
					_attributeNameComboBox.removeAllItems();
					_attributeNameTextField.setText( "");
					for ( Attribute attribute : atg.getAttributes()) {
						addAttributeInformation( attribute, "");
					}
					_attributeNameComboBox.addItem( LEERE_DATEN_STATUS);
					_attributeNameComboBox.addItem( KEINE_DATEN_STATUS);
					_attributeNameComboBox.addItem( KEINE_QUELLE_STATUS);
					_attributeNameComboBox.addItem( KEINE_RECHTE_STATUS);
					if ( _attributeNameComboBox.getModel().getSize() > 0) {
						_attributeNameComboBox.setSelectedIndex( 0);
					}
				}
			}
			
			private void addAttributeInformation ( final Attribute attribute, final String prefix) {
				String newPrefix = prefix + attribute.getName();
				if ( attribute.isArray()) {
					Integer maxCount = attribute.getMaxCount();
					if ( maxCount >0) {
						maxCount--;
						newPrefix += "[0.." + maxCount.toString() + "]";
					} else {
						newPrefix += "[0...]";
					}
				} 
				final AttributeType attributeType = attribute.getAttributeType();
				if ( (attributeType instanceof IntegerAttributeType) || 
						(attributeType instanceof DoubleAttributeType)) {
					_attributeNameComboBox.addItem( prefix + attribute.getName());
				} else if ( (attributeType instanceof StringAttributeType) || 
						(attributeType instanceof TimeAttributeType) ||
						(attributeType instanceof ReferenceAttributeType) ) {
					return;		// werden im Moment nicht angezeigt; leider kann man einzelne
					// Items einer JComboBox nicht disablen.
				} else if (  attributeType instanceof AttributeListDefinition) {
					newPrefix += ".";
					final AttributeListDefinition ald = (AttributeListDefinition) attributeType;
					for ( Attribute subAttribute : ald.getAttributes()) {
						addAttributeInformation( subAttribute, newPrefix);
					}
				}
			}
		};
		_attributeGroupComboBox.addItemListener( itemListener);
	}
	
	/*
	 * Fügt der _attributeNameComboBox den Listener hinzu, der bei Änderunge der Selektion 
	 * in _attributeNameTextField und den Von- und Bis-Feldern die nötigen Änderungen durchführt.
	 */
	private void addAttributeNameListener() {
		ItemListener comboBoxItemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED) {
					final String selectedItem = (String) _attributeNameComboBox.getSelectedItem();
					if ( selectedItem.length() == 0) {
						return;
					}
					_attributeNameTextField.setText( selectedItem);
					if ( selectedItem.contains("[")) {
						_attributeNameTextField.setEditable(true);
						_attributeNameTextField.setToolTipText( "Bitte bearbeiten sie die Feldgrenzen!");
					} else {
						_attributeNameTextField.setEditable(false);
						_attributeNameTextField.setToolTipText( "Keine Bearbeitung nötig!");
					}
					// Von- und Bis-Werte:
					AttributeGroup atg = _configuration.getAttributeGroup( (String) _attributeGroupComboBox.getSelectedItem());
					final Attribute attribute = getAttribute( atg, selectedItem);
					final AttributeType attributeType;
					if ( attribute != null) {
						attributeType = attribute.getAttributeType();
					} else { // selectedItem = "<keine Daten>" oder = "<keine Quelle>"
						attributeType = null;
					}
					if ( (attributeType != null) && (attributeType instanceof IntegerAttributeType)) {
						IntegerAttributeType iat = (IntegerAttributeType) attributeType;
						final List<IntegerValueState> states = iat.getStates();
						final IntegerValueRange range = iat.getRange();
						
						Collection<String> unscaledStrings = new ArrayList<String>();
						Collection<String> stateStrings = new ArrayList<String>();
						if ( !states.isEmpty() && (range != null)) {
							_fromUnscaledComboBox.setEditable( true);
							_toUnscaledComboBox.setEditable( true);
							Long rangeMinimum = range.getMinimum();
							Long rangeMaximum = range.getMaximum();
							boolean minimumIsState = false;
							boolean maximumIsState = false;
							for ( IntegerValueState state : states) {
								final Long value = state.getValue();
								if ( value == rangeMinimum) {
									minimumIsState = true;
								}
								if ( value == rangeMaximum) {
									maximumIsState = true;
								}
								unscaledStrings.add( value.toString());
								final String name = state.getName();
								if ( name.isEmpty()) {
									stateStrings.add( value.toString());
								} else {
									stateStrings.add( name);
								}
							}
							final double conversionFactor = range.getConversionFactor();
							if ( !minimumIsState) {
								unscaledStrings.add( rangeMinimum.toString());
								String s;
								if ( conversionFactor != 1.) {
									final Double minimumScaled = conversionFactor * rangeMinimum;
									s = minimumScaled.toString().replace('.', ',');
								} else {
									s = rangeMinimum.toString();
								}
								final String unit = range.getUnit();
								if ( unit != null && !unit.isEmpty()) {
									s += " " + unit;
								}
								s += " (Minimum des Bereichs)";
								stateStrings.add( s);
							}
							if ( !maximumIsState) {
								unscaledStrings.add( rangeMaximum.toString());
								String s;
								if ( conversionFactor != 1.) {
									final Double maximumScaled = conversionFactor * rangeMaximum;
									s = maximumScaled.toString().replace('.', ',');
								} else {
									s = rangeMaximum.toString();
								}
								final String unit = range.getUnit();
								if ( unit != null && !unit.isEmpty()) {
									s += " " + unit;
								}
								s += " (Maximum des Bereichs)";
								stateStrings.add( s);
							}
						} else {
							if ( !states.isEmpty()) {	// kein Range!
								_fromUnscaledComboBox.setEditable( false);
								_toUnscaledComboBox.setEditable( false);
								for ( IntegerValueState state : states) {
									final Long value = state.getValue();
									unscaledStrings.add( value.toString());
									final String name = state.getName();
									if ( name.isEmpty()) {
										stateStrings.add( value.toString());
									} else {
										stateStrings.add( name);
									}
								}
							}
							if ( range != null) {	// keine Zustände!
								_fromUnscaledComboBox.setEditable( true);
								_toUnscaledComboBox.setEditable( true);
								Long rangeMinimum = range.getMinimum();
								unscaledStrings.add( rangeMinimum.toString());
								final double conversionFactor = range.getConversionFactor();
								
								String s;
								if ( conversionFactor != 1.) {
									final Double minimumScaled = conversionFactor * rangeMinimum;
									s = minimumScaled.toString().replace('.', ',');
								} else {
									s = rangeMinimum.toString();
								}
								final String unit = range.getUnit();
								if ( unit != null && !unit.isEmpty()) {
									s += " " + unit;
								}
								s += " (Minimum des Bereichs)";
								stateStrings.add( s);
								
								Long rangeMaximum = range.getMaximum();
								unscaledStrings.add( rangeMaximum.toString());
								
								if ( conversionFactor != 1.) {
									final Double maximumScaled = conversionFactor * rangeMaximum;
									s = maximumScaled.toString().replace('.', ',');
								} else {
									s = rangeMaximum.toString();
								}
								if ( unit != null && !unit.isEmpty()) {
									s += " " + unit;
								}
								s += " (Maximum des Bereichs)";
								stateStrings.add( s);
							}
						}
						if ( unscaledStrings.size() != stateStrings.size()) {
							_debug.warning("DynamicDefinitionComponent: Anzahl Rohwerte ungleich Anzahl Zustände.");
						}
						_fromUnscaledComboBox.removeAllItems();
						_fromUnscaledComboBox.setModel( new DefaultComboBoxModel( unscaledStrings.toArray()));
						_toUnscaledComboBox.removeAllItems();
						_toUnscaledComboBox.setModel( new DefaultComboBoxModel( unscaledStrings.toArray()));
						_fromStateComboBox.removeAllItems();
						_fromStateComboBox.setModel( new DefaultComboBoxModel( stateStrings.toArray()));
						_toStateComboBox.removeAllItems();
						_toStateComboBox.setModel( new DefaultComboBoxModel( stateStrings.toArray()));
						if ( !unscaledStrings.isEmpty()) {
							_fromUnscaledComboBox.setSelectedIndex( 0);
							_fromStateComboBox.setSelectedIndex( 0);
							_toUnscaledComboBox.setSelectedIndex( unscaledStrings.size()-1);
							_toStateComboBox.setSelectedIndex( stateStrings.size()-1);
						}
					} else {
						_fromUnscaledComboBox.removeAllItems();
						_toUnscaledComboBox.removeAllItems();
						_fromStateComboBox.removeAllItems();
						_toStateComboBox.removeAllItems();
						_fromUnscaledComboBox.setEditable( false);
						_toUnscaledComboBox.setEditable( false);
					}
				}
			}
		};
		_attributeNameComboBox.addItemListener( comboBoxItemListener);
	}
			
	/*
	 * Fügt den Von- und Bis-Feldern die Listener hinzu, die die Logik dieser Felder untereinander
	 * beinhalten.
	 */
	private void addFromToListeners() {
		class ChangeOtherComboBoxListener implements ItemListener {
			
			final JComboBox _eventComboBox;
			final JComboBox _otherComboBox;
			
			public ChangeOtherComboBoxListener( final JComboBox eventComboBox, 
					final JComboBox otherComboBox) {
				_eventComboBox = eventComboBox;
				_otherComboBox = otherComboBox;
			}
			
			public void itemStateChanged(ItemEvent e) {
				if ( e.getStateChange() == ItemEvent.SELECTED) {
					final int selectedIndex = _eventComboBox.getSelectedIndex();
					if ( (selectedIndex != -1) && (selectedIndex <= _otherComboBox.getModel().getSize())) {
						if ( _otherComboBox.getSelectedIndex() != selectedIndex) {	// Um Pingpong zu vermeiden!
							_otherComboBox.setSelectedIndex( selectedIndex);
						}
					}
				}
			}
		}
		_fromUnscaledComboBox.addItemListener( new ChangeOtherComboBoxListener( _fromUnscaledComboBox, _fromStateComboBox));
		_fromStateComboBox.addItemListener( new ChangeOtherComboBoxListener( _fromStateComboBox, _fromUnscaledComboBox));
		_toUnscaledComboBox.addItemListener( new ChangeOtherComboBoxListener( _toUnscaledComboBox, _toStateComboBox));
		_toStateComboBox.addItemListener( new ChangeOtherComboBoxListener( _toStateComboBox, _toUnscaledComboBox));
		
		_fromUnscaledComboBox.setEditor( new BasicComboBoxEditor());
		
		class EditingListener implements ActionListener {
			
			final JComboBox _editedComboBox;
			final JComboBox _otherComboBox;
			
			public EditingListener ( final JComboBox editedComboBox, 
					final JComboBox otherComboBox) {
				_editedComboBox = editedComboBox;
				_otherComboBox = otherComboBox;
			}
			
			public void actionPerformed(ActionEvent e) {
				if ( e.getActionCommand() == "comboBoxEdited") {
					final Object item = _editedComboBox.getEditor().getItem();
					_editedComboBox.addItem( item);
					final String anObject = "<vom Benutzer definierter Wert>";
					_otherComboBox.addItem( anObject);
					_editedComboBox.setSelectedItem( item);
					_otherComboBox.setSelectedItem( anObject);
				}
			}
		}
		_fromUnscaledComboBox.addActionListener( new EditingListener( _fromUnscaledComboBox, _fromStateComboBox));
		_toUnscaledComboBox.addActionListener( new EditingListener( _toUnscaledComboBox, _toStateComboBox));
	}
	
	/**
	 * Gibt den Infotext zurück, wenn dieser nicht leer ist, oder <code>null</code>.
	 * 
	 * @return der Infotext
	 */
	public String getInfoText() {
		String description = _infoField.getText();
		if ( description == null || (description.length() == 0)) {
			return null;
		}
		return description;
	}
	
	/**
	 * Gibt den Namen der Attribtgruppe zurück, wenn dieser nicht leer ist, oder <code>null</code>.
	 * 
	 * @return der Attributgruppenname
	 */
	public String getAttributeGroupName() {
		final Object agItem = _attributeGroupComboBox.getSelectedItem();
		if ( agItem == null ) {
			return null;
		}
		return (String) agItem;
	}
	
	/**
	 * Gibt den Namen des Aspekts zurück, wenn dieser nicht leer ist, oder <code>null</code>.
	 * 
	 * @return der Aspektname
	 */
	public String getAspectName() {
		final Object aspectItem = _aspectComboBox.getSelectedItem();
		if ( aspectItem == null ) {
			return null;
		}
		return (String) aspectItem;
	}
	
	/**
	 * Gibt den Attributnamen zurück, wenn dieser nicht leer ist und keine Array-Beschreibungsteile
	 * enthält, oder <code>null</code>.
	 * 
	 * @return der Attributname
	 */
	public String getAttributeName() {
		final String attributeName = _attributeNameTextField.getText();
		if ( attributeName == null || (attributeName.length() == 0)) {
			return null;
		}
		final String attributeNameFromComboBox = (String) _attributeNameComboBox.getSelectedItem();
		final String a1 = deleteArraysFromString( attributeName, false);
		final String a2 = deleteArraysFromString( attributeNameFromComboBox, true);
		if ( (a1 == null) || (a2 == null) || !a1.equals( a2)) {
			return null;
		}
		if ( attributeName.contains( "..")) {
			return null;
		}
		return attributeName;
	}
	
	/**
	 * Gibt eine 0 zurück, wenn _attributeNameTextField einen korrekten (komplexen) Attributnamen
	 * enthält. Gibt eine 1 zurück, wenn _attributeNameTextField keinen oder einen leeren String
	 * enthält. Gibt eine 2 zurück, wenn _attributeNameTextField einen Attributnamen mit Array-
	 * Beschreibungsteilen enthält.
	 * 
	 * @return 0, 1 oder 2
	 */
	public int checkAttributeName() {
		final String attributeName = _attributeNameTextField.getText();
		if ( attributeName == null || (attributeName.length() == 0)) {
			return 1;
		}
		final String attributeNameFromComboBox = (String) _attributeNameComboBox.getSelectedItem();
		final String a1 = deleteArraysFromString( attributeName, false);
		final String a2 = deleteArraysFromString( attributeNameFromComboBox, true);
		if ( (a1 == null) || (a2 == null) || !a1.equals( a2)) {
			return 2;
		}
		return 0;
	}
	
	/**
	 * Gibt den Von-Wert zurück oder <code>null</code>. Letzteres geschieht wenn _fromUnscaledComboBox
	 * keinen, einen leeren oder einen nicht in einen Double umwandelbaren Wert enthält.
	 * 
	 * @return der Von-Wert oder <code>null</code>
	 */
	public Double getFromValue () {
		final Object selectedItem = _fromUnscaledComboBox.getSelectedItem();
		if ( selectedItem == null) {
			return null;
		}
		final String fromAsString = selectedItem.toString().replace(',', '.');
		if ( fromAsString == null || (fromAsString.length() == 0)) {
			return null;
		}
		Double fromValue;
		try {
			fromValue = Double.valueOf( fromAsString);
		} catch ( NumberFormatException exeption) {
			return null;
		}
		if ( fromValue.isNaN()) {
			return null;
		}	
		return fromValue;
	}
	
	/**
	 * Überprüft, ob _fromUnscaledComboBox einen in einen vernünftigen Wert enthält, und gibt
	 * in diesem Fall eine 0 zurück. Gibt eine 1 zurück, wenn kein Von-Wert eingetragen wurde,
	 * eine 2 oder 3 wenn der Wert sich nicht in einen Double wandeln läßt.
	 * 
	 * @return 0, 1, 2 oder 3
	 */
	public int checkFromValue () {
		final Object selectedItem = _fromUnscaledComboBox.getSelectedItem();
		if ( selectedItem == null) {
			if ( !attributeNameIsState()) {
				return 1;
			} else {
				return 0;
			}
		}
		final String fromAsString = selectedItem.toString().replace(',', '.');
		if ( fromAsString == null) {
			if ( !attributeNameIsState()) {
				return 1;
			} else {
				return 0;
			}
		}
		if ( fromAsString.length() == 0) {
			return 1;
		}
		Double fromValue;
		try {
			fromValue = Double.valueOf( fromAsString);
		} catch ( NumberFormatException exeption) {
			return 2;
		}
		if ( fromValue.isNaN()) {
			return 3;
		}	
		return 0;
	}
	
	/**
	 * Gibt den Bis-Wert zurück oder <code>null</code>. Letzteres geschieht wenn _fromUnscaledComboBox
	 * keinen, einen leeren oder einen nicht in einen Double umwandelbaren Wert enthält.
	 * 
	 * @return den Bis-Wert oder <code>null</code>
	 */
	public Double getToValue () {
		final Object selectedItem = _toUnscaledComboBox.getSelectedItem();
		if ( selectedItem == null) {
			return null;
		}
		final String toAsString = selectedItem.toString().replace(',', '.');
		if ( toAsString == null || (toAsString.length() == 0)) {
			return null;
		}
		Double toValue;
		try {
			toValue = Double.valueOf( toAsString);
		} catch ( NumberFormatException exeption) {
			return null;
		}
		if ( toValue.isNaN()) {
			return null;
		}	
		return toValue;
	}
	
	/**
	 * Überprüft, ob _fromUnscaledComboBox einen in einen vernünftigen Wert enthält, und gibt
	 * in diesem Fall eine 0 zurück. Gibt eine 1 zurück, wenn kein Von-Wert eingetragen wurde,
	 * eine 2 oder 3 wenn der Wert sich nicht in einen Double wandeln läßt.
	 * 
	 * @return 0, 1, 2 oder 3
	 */
	public int checkToValue () {
		final Object selectedItem = _toUnscaledComboBox.getSelectedItem();
		if ( selectedItem == null) {
			if ( !attributeNameIsState()) {
				return 1;
			} else {
				return 0;
			}
		}
		final String toAsString = selectedItem.toString().replace(',', '.');
		if ( toAsString == null) {
			if ( !attributeNameIsState()) {
				return 1;
			} else {
				return 0;
			}
		}
		Double toValue;
		try {
			toValue = Double.valueOf( toAsString);
		} catch ( NumberFormatException exeption) {
			return 2;
		}
		if ( toValue.isNaN()) {
			return 3;
		}
		return 0;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn das in _attributeNameComboBox selektierte Objekt einen
	 * der Stati für leere Daten, keine Daten, keine Quelle oder keine Rechte beschreibt.
	 * 
	 * @return <code>true</code> genau dann, wenn es 'keine-Daten-Status' selektiert ist
	 */
	public boolean attributeNameIsState() {
		final Object selectedItem = _attributeNameComboBox.getSelectedItem();
		if ( selectedItem != null) {
			final String selectedItemAsString = (String) selectedItem;
			if ( selectedItemAsString.equals( LEERE_DATEN_STATUS) ||
					selectedItemAsString.equals( KEINE_DATEN_STATUS) ||
					selectedItemAsString.equals( KEINE_QUELLE_STATUS) ||
					selectedItemAsString.equals( KEINE_RECHTE_STATUS)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn der übergebene String einen der Stati für leere Daten, 
	 * keine Daten, keine Quelle oder keine Rechte beschreibt.
	 * 
	 * @return <code>true</code> genau dann, wenn der String einen 'keine-Daten-Status' beschreibt
	 */
	public static boolean attributeNameIsState( final String attributeName) {
		if ( attributeName == null) {
			return false;
		}
		if ( attributeName.equals( LEERE_DATEN_STATUS) ||
				attributeName.equals( KEINE_DATEN_STATUS) ||
				attributeName.equals( KEINE_QUELLE_STATUS) ||
				attributeName.equals( KEINE_RECHTE_STATUS)) {
			return true;
		}
		return false;
	}
}
