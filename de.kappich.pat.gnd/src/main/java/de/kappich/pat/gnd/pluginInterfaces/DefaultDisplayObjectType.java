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
package de.kappich.pat.gnd.pluginInterfaces;

import de.kappich.pat.gnd.displayObjectToolkit.DOTManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DOTSubscriptionData;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItemManager;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDefinitionComponent;
import de.kappich.pat.gnd.displayObjectToolkit.DOTItemManager.DisplayObjectTypeItemWithInterval;
import de.kappich.pat.gnd.gnd.LegendTreeNodes;
import de.kappich.pat.gnd.utils.Interval;

import de.bsvrz.dav.daf.main.DataState;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.table.TableModel;

/**
 * Ein DefaultDisplayObjectType ist eine abstrakte Klasse, die eine teilweise Implementation von DisplayObjectType ist.
 * Die Grundfigur im Interface DisplayObjectType wird allerdings in dieser Implementation stets ignoriert. Subklassen,
 * für die dies Verhalten ideal ist, sind DOTLine, DOTArea und DOTComplex.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
abstract public class DefaultDisplayObjectType implements DisplayObjectType, DOTManager.DOTChangeListener {
	
	/**
	 * Ein DefaultDisplayObjectType ist keine funktional vollständige Implementation von DisplayObjectType,
	 * sondern beinhaltet die Gemeinsamkeiten der Implementation von DOTArea, DOTComplex und DOTLine.
	 */
	protected DefaultDisplayObjectType () {
		initCollections();
		DOTManager.getInstance().addDOTChangeListener( this);
	}
	
	private void initCollections() {
		final DisplayObjectTypePlugin displayObjectTypePlugin = getDisplayObjectTypePlugin();
		final DOTProperty[] properties = displayObjectTypePlugin.getProperties( null);
		for(DOTProperty dotProperty : properties) {
			_isStaticMap.put( dotProperty, true);
			_staticPropertyValues.put(dotProperty, getDefaultValue( dotProperty));
			_dynamicDOTItemManagers.put( dotProperty, new DynamicDOTItemManager());
		}
	}
	
	public String getName() {
		return _name;
	}
	
	public void setName(String name) {
		_name = name;
	}
	
	public String getInfo() {
		return _info;
	}
	
	public void setInfo(String info) {
		_info = info;
	}
	
	public Set<String> getPrimitiveFormNames() {
		return new HashSet<String>();
	}
	
	public String getPrimitiveFormType(String primitiveFormName) {
		if ( primitiveFormName != null) {
			return null;
		}
		return "";
	}
	
	public String getPrimitiveFormInfo(String primitiveFormName) {
		if ( primitiveFormName != null) {
			return null;
		}
		return "";
	}
	
	public void removePrimitiveForm(String primitiveFormName) {
	}
	
	public List<DOTProperty> getDynamicProperties(String primitiveFormName) {
		if ( primitiveFormName != null) {
			return null;
		}
		List<DOTProperty> dynamicPropertyList = new ArrayList<DOTProperty>();
		for ( DOTProperty property : _isStaticMap.keySet()) {
			if ( !_isStaticMap.get( property)) {
				dynamicPropertyList.add( property);
			}
		}
		return dynamicPropertyList;
	}
	
	public Boolean isPropertyStatic(String primitiveFormName, DOTProperty property) {
		return _isStaticMap.get( property);
	}
	
	/*
	 * Die Default-Implementation ignoriert den Grundfigurnamen und berücksichtigt, falls der übergebene
	 * Boolean <code>true</code> ist, ob die Eigenschaft bisher dynamisch war, und setzt ihren Wert
	 * auf den Defaultwert.
	 */
	public void setPropertyStatic( String primitiveFormName, DOTProperty property, boolean b) {
		if ( b) {
			final boolean wasDynamic = _dynamicDOTItemManagers.containsKey( property);
			if ( wasDynamic) {
				Object value;
				if ( property == DOTProperty.FARBE) {
					value = DEFAULT_COLOR_NAME;
				} else if ( property == DOTProperty.ABSTAND) {
					value = DEFAULT_DISTANCE;
				} else if ( property == DOTProperty.STRICHBREITE) {
					value = DEFAULT_STROKE_WIDTH;
				} else {
					throw new IllegalArgumentException();
				}
				_staticPropertyValues.put( property, value);
			}
		}
		_isStaticMap.put( property, b);
	}
	
	public Object getValueOfStaticProperty( String primitiveFormName, DOTProperty property) {
		if ( _isStaticMap.containsKey(property)) {
			return _staticPropertyValues.get( property);
		} else {
			if ( property == DOTProperty.FARBE) {
				return DEFAULT_COLOR_NAME;
			} else if ( property == DOTProperty.ABSTAND) {
				return DEFAULT_DISTANCE;
			} else if ( property == DOTProperty.STRICHBREITE) {
				return DEFAULT_STROKE_WIDTH;
			} 
			return null;
		}
	}
	
	public void setValueOfStaticProperty( String primitiveFormName, DOTProperty property, Object value) {
		_staticPropertyValues.put( property, value);
	}
	
	public void setValueOfDynamicProperty( String primitiveFormName,
			DOTProperty property, 
			DisplayObjectTypeItem dItem, 
			Double lowerBound, 
			Double upperBound) {
		if ( property == null) {
			return;
		}
		DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		if ( dynamicDOTItemManager == null) {
			return;
		}
		if ( !(dItem instanceof DynamicDOTItem)) {
			return;
		}
		DynamicDOTItem ldItem = (DynamicDOTItem) dItem;
		final Interval<Double> interval;
		if ( lowerBound == null || upperBound == null) {
			interval = new Interval<Double> ( Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		} else {
			interval = new Interval<Double> ( lowerBound, upperBound);
		}
		dynamicDOTItemManager.insert( interval, ldItem);
	}
	
	public DisplayObjectType getCopy( String name) {
		DefaultDisplayObjectType copy = (DefaultDisplayObjectType) getDisplayObjectTypePlugin().getDisplayObjectType();
		if ( name != null) {
			copy.setName(name);
		} else {
			copy.setName(_name);
		}
		copy.setInfo( _info);
		for ( DOTProperty property : _isStaticMap.keySet()) {
			Boolean newBoolean = _isStaticMap.get( property);
			copy._isStaticMap.put( property, newBoolean);
		}
		for ( DOTProperty property : _staticPropertyValues.keySet()) {
			Object object = _staticPropertyValues.get( property);
			Object newObject = null;
			if ( object instanceof Integer) {
				newObject = new Integer( (Integer) object);
			} else if (object instanceof Double) {
				newObject = new Double ( (Double) object);
			} else if (object instanceof String) {
				newObject = new String( (String) object);
			} else if (object instanceof Color) {
				final Color color = (Color) object;
				newObject = new Color (color.getRed(), color.getBlue(), color.getGreen(), color.getAlpha());
			}
			copy._staticPropertyValues.put( property, newObject);
		}
		for ( DOTProperty property : _dynamicDOTItemManagers.keySet()) {
			DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
			DynamicDOTItemManager newDynamicDOTItemManager = dynamicDOTItemManager.getCopy();
			copy._dynamicDOTItemManagers.put( property, newDynamicDOTItemManager);
		}
		return copy;
	}
	
	public void putPreferences (Preferences prefs) {
		deletePreferences(prefs);
		Preferences classPrefs = prefs.node(prefs.absolutePath() + "/" + getClass().getSimpleName());
		Preferences objectPrefs = classPrefs.node( classPrefs.absolutePath() + "/" + getName());
		Preferences infoPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/info");
		infoPrefs.put(INFO, getInfo());
		
		Preferences staticPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/static");
		Preferences dynamicPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/dynamic");
		for ( DOTProperty property : _isStaticMap.keySet()) {
			if ( _isStaticMap.get( property)) {
				// Eine Statische Property wird als dynamische ohne Anmeldungsdaten weggeschrieben. 
				Preferences propertyPrefs;
				if ( property == DOTProperty.FARBE) {
					propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/color");
				} else if ( property == DOTProperty.ABSTAND) {
					propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/distance");
				} else if ( property == DOTProperty.STRICHBREITE) {
					propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/strokewidth");
				} else {
					throw new IllegalArgumentException();
				}
				final DynamicDOTItem dynamicDOTItem = new DynamicDOTItem("", "", "", "",
						_staticPropertyValues.get( property));
				dynamicDOTItem.putPreferences(propertyPrefs);
			} else {
				Preferences propertyPrefs;
				if ( property == DOTProperty.FARBE) {
					propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/color");
				} else if ( property == DOTProperty.ABSTAND) {
					propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/distance");
				} else if ( property == DOTProperty.STRICHBREITE) {
					propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/strokewidth");
				} else {
					throw new IllegalArgumentException();
				}
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
				int i = 0;
				for ( TreeMap<Interval<Double>, DynamicDOTItem> treeMap : dynamicDOTItemManager.getTreeMaps()) {
					for ( Interval<Double> interval: treeMap.keySet()) {
						Preferences objectForItemPrefs = propertyPrefs.node( propertyPrefs.absolutePath() + "/interval" + i);
						objectForItemPrefs.putDouble(LOWER_BOUND, interval.getLowerBound());
						objectForItemPrefs.putDouble(UPPER_BOUND, interval.getUpperBound());
						DynamicDOTItem dynamicItem = (DynamicDOTItem)treeMap.get(interval);
						if ( (dynamicItem == null) && (treeMap.size() >= 1)) {	
							// weil es mit Double.NEGATIVE_INFINITY nicht geht, das get(); das >= ist wichtig!
							dynamicItem = treeMap.values().toArray( new DynamicDOTItem[1])[0];
						}
						dynamicItem.putPreferences( objectForItemPrefs);
						i++;
					}
				}
			}
		}
	}
	
	public void initializeFromPreferences(Preferences prefs) {
		_name = prefs.name();
		Preferences infoPrefs = prefs.node( prefs.absolutePath() + "/info");
		_info = infoPrefs.get(INFO, "");
		
		Preferences staticPrefs = prefs.node( prefs.absolutePath() + "/static");
		String[] staticChilds;
		try {
			staticChilds = staticPrefs.childrenNames();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
		for ( String staticChild : staticChilds) {
			DOTProperty property;
			Preferences propertyPrefs;
			if ( staticChild.equals( "color")) {
				property = DOTProperty.FARBE;
				propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/color");
			} else if ( staticChild.equals( "distance")) {
				property = DOTProperty.ABSTAND;
				propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/distance");
			} else if ( staticChild.equals( "strokewidth")) {
				property = DOTProperty.STRICHBREITE;
				propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/strokewidth");
			} else {
				continue;
			}
			_isStaticMap.put( property, true);
			final DynamicDOTItem dynamicDOTItem = new DynamicDOTItem( propertyPrefs);
			_staticPropertyValues.put( property, dynamicDOTItem.getPropertyValue());
		}
		
		Preferences dynamicPrefs = prefs.node( prefs.absolutePath() + "/dynamic");
		String[] dynamicChilds;
		try {
			dynamicChilds = dynamicPrefs.childrenNames();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
		for ( String dynamicChild : dynamicChilds) {
			DOTProperty property;
			Preferences propertyPrefs;
			if ( dynamicChild.equals( "color")) {
				property = DOTProperty.FARBE;
				propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/color");
			} else if ( dynamicChild.equals( "distance")) {
				property = DOTProperty.ABSTAND;
				propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/distance");
			} else if ( dynamicChild.equals( "strokewidth")) {
				property = DOTProperty.STRICHBREITE;
				propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/strokewidth");
			} else {
				continue;
			}
			_isStaticMap.put( property, false);
			DynamicDOTItemManager dynamicDOTItemManager = new DynamicDOTItemManager();
			_dynamicDOTItemManagers.put( property, dynamicDOTItemManager);
			
			String[] intervalNames;
			try {
				intervalNames = propertyPrefs.childrenNames();
			}
			catch(BackingStoreException e) {
				
				throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
			}
			for ( String child: intervalNames) {
				if ( child.startsWith("interval")) {
					Preferences objectItemPrefs = propertyPrefs.node( propertyPrefs.absolutePath() + "/" + child);
					DynamicDOTItem dynamicItem = new DynamicDOTItem( objectItemPrefs);
					setValueOfDynamicProperty( null, property, dynamicItem, 
							objectItemPrefs.getDouble(LOWER_BOUND, Double.MAX_VALUE),
							objectItemPrefs.getDouble(UPPER_BOUND, Double.MIN_VALUE));
				}
			}
		}
	}
	
	public void deletePreferences (Preferences prefs) {
		Preferences classPrefs = prefs.node(prefs.absolutePath() + "/" + getClass().getSimpleName());
		Preferences objectPrefs = classPrefs.node( classPrefs.absolutePath() + "/" + getName());
		try {
			objectPrefs.removeNode();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
	}
	
	
	
	/**
	 * Gibt ein TableModel für die übergebene Eigenschaft zurück.
	 *  
	 * @param property die Eigenschaft
	 * @return ein TableModel
	 */
	public TableModel getTableModel( DOTProperty property) {
		DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		return dynamicDOTItemManager;
	}
	
	/**
	 * Liefert die Menge von Zeilen-Indizes der Zeilen, die mit mindestens einer anderen
	 * einen Konflikt haben. Ein Konflikt liegt dann vor, wenn ein dynamischer Wert sowohl zu 
	 * der einer als auch der anderen Zeile passt; eine Zeile entspricht hier einem DisplayObjectTypeItem.
	 * 
	 * @param property die Eigenschaft
	 * @return eine Menge von Zeilen-Indizes
	 */
	public Set<Integer> getConflictingRows( DOTProperty property) {
		final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		if ( dynamicDOTItemManager == null) {
			return null;
		}
		return dynamicDOTItemManager.getConflictingRows();
	}
	
	/**
	 * Liefert die Einträge der Legende zurück.
	 * 
	 * @return eine Teilbaum für die Legende
	 */
	public LegendTreeNodes getLegendTreeNodes() {
		LegendTreeNodes legendTreeNodes = new LegendTreeNodes();
		final List<DOTProperty> dynamicProperties = getDynamicProperties( null);
		final DOTProperty colorProperty = DOTProperty.FARBE;
		final int size = dynamicProperties.size();
		if ( size == 0) {	// keine dynamischen Eigenschaften
			final String colorName = (String) getValueOfStaticProperty( null, colorProperty);
			LegendTreeNodes.LegendTreeNode node = new LegendTreeNodes.LegendTreeNode( colorName, null);
			legendTreeNodes.add( node, 0);
		}
		else {
			final Boolean colorPropertyIsStatic = isPropertyStatic( null, colorProperty);
			if ( (size == 1) && (colorPropertyIsStatic != null) &&!colorPropertyIsStatic ) {
				// nur die Farbe ist dynamisch
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( colorProperty);
				for ( TreeMap<Interval<Double>, DynamicDOTItem> treeMap : dynamicDOTItemManager.getTreeMaps()) {
					for ( DisplayObjectTypeItem dotItem : treeMap.values()) {
						LegendTreeNodes.LegendTreeNode node = new LegendTreeNodes.LegendTreeNode( dotItem.getDescription(), null);
						legendTreeNodes.add( node, 0);
					}
				}
			} else {	// alles andere wird eher 'generisch' behandelt
				LegendTreeNodes.LegendTreeNode node = null;
				int depth = 0;
				int newDepth;
				for ( DOTProperty property : dynamicProperties) {
					newDepth = 0;
					if ( node != null) {
						legendTreeNodes.add(node, depth-newDepth);
					}
					node = new LegendTreeNodes.LegendTreeNode( property.toString(), null);
					depth = newDepth;
					final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
					final int itemSize = dynamicDOTItemManager.size();
					for ( int rowIndex = 0; rowIndex < itemSize; rowIndex++) {
						newDepth = 1;
						legendTreeNodes.add(node, depth-newDepth);
						String description = dynamicDOTItemManager.get( rowIndex).getItem().getDescription();
						node = new LegendTreeNodes.LegendTreeNode( description, null);
						depth = newDepth;
					}
				}
				newDepth = 0;
				if ( node != null) {
					legendTreeNodes.add(node, depth-newDepth);
				}
			}
		}
		return legendTreeNodes;
	}
	
	public Set<DOTSubscriptionData> getSubscriptionData() {
		Set<DOTSubscriptionData> sdSet = new HashSet<DOTSubscriptionData>();
		for ( DOTProperty property : _isStaticMap.keySet()) {
			if ( !_isStaticMap.get( property)) {
				sdSet.addAll( _dynamicDOTItemManagers.get( property).getSubscriptionData());
			}
		}
		return sdSet;
	}
	
	public List<String> getAttributeNames( String primitiveFormName, DOTProperty property, 
			DOTSubscriptionData subscriptionData) {
		final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		if ( dynamicDOTItemManager == null) {
			return new ArrayList<String>();
		}
		final List<String> list = dynamicDOTItemManager.getAttributeNames( subscriptionData);
		if ( list != null) {
			return list;
		}
		return new ArrayList<String>();
	}
	
	private boolean hasProperty( final DOTProperty property) {
		return _isStaticMap.containsKey( property);
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getUsedColors() {
		Set<String> usedColors = new HashSet<String>();
		if ( hasProperty( DOTProperty.FARBE)) {
			if ( isPropertyStatic( null, DOTProperty.FARBE)) {
				final String colorName = (String) getValueOfStaticProperty( null, DOTProperty.FARBE);
				usedColors.add( colorName.toLowerCase());
			} else {
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( DOTProperty.FARBE);
				final int size = dynamicDOTItemManager.size();
				for ( int index = 0; index < size; index++) {
					final DisplayObjectTypeItemWithInterval dotItemWithInterval = dynamicDOTItemManager.get(index);
					final String colorName = (String) dotItemWithInterval.getItem().getPropertyValue();
					usedColors.add( colorName.toLowerCase());
				}
			}
		}
		if ( hasProperty( DOTProperty.FUELLUNG)) {
			if ( isPropertyStatic( null, DOTProperty.FUELLUNG)) {
				final String colorName = (String) getValueOfStaticProperty( null, DOTProperty.FUELLUNG);
				usedColors.add( colorName.toLowerCase());
			} else {
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( DOTProperty.FUELLUNG);
				final int size = dynamicDOTItemManager.size();
				for ( int index = 0; index < size; index++) {
					final DisplayObjectTypeItemWithInterval dotItemWithInterval = dynamicDOTItemManager.get(index);
					final String colorName = (String) dotItemWithInterval.getItem().getPropertyValue();
					usedColors.add( colorName.toLowerCase());
				}
			}
		}
		return usedColors;
	}
	
	/**
	 * Gibt den Default-Wert der Eigenschaft zurück.
	 * 
	 * @param property eine Eigenschaft
	 * @return der Default-Wert
	 */
	private static Object getDefaultValue( DOTProperty property) {
		if ( property == null) {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): die übergebene Property ist null");
		}
		if ( property == DOTProperty.FARBE || property == DOTProperty.FUELLUNG) {
			return DEFAULT_COLOR_NAME;
		} else if ( property == DOTProperty.ABSTAND) {
			return DEFAULT_DISTANCE;
		} else if ( property == DOTProperty.DURCHMESSER) {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): nicht definierter Default-Wert für " + property.toString());
		} else if ( property == DOTProperty.GROESSE) {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): nicht definierter Default-Wert für " + property.toString());
		} else if ( property == DOTProperty.STRICHBREITE) {
			return DEFAULT_STROKE_WIDTH;
		} else if ( property == DOTProperty.TEXT) {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): nicht definierter Default-Wert für " + property.toString());
		} else if ( property == DOTProperty.TEXTSTIL) {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): nicht definierter Default-Wert für " + property.toString());
		} else if ( property == DOTProperty.TRANSPARENZ) {
			return DEFAULT_TRANSPARENCY;
		} else {
			throw new IllegalArgumentException("DefaultDisplayObject.getDefaultValue(): nicht definierter Default-Wert für " + property.toString());
		}
	}
	
	public DisplayObjectTypeItem isValueApplicable( 
			String primitiveFormName, DOTProperty property,
			DOTSubscriptionData subscriptionData, 
			String attributeName, double value) {
		if ( _isStaticMap.get( property)) {
			return null;
		}
		final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		if ( dynamicDOTItemManager == null) {
			return null;
		}
		final List<String> list = dynamicDOTItemManager.getAttributeNames( subscriptionData);
		if ( list == null) {
			return null;
		}
		if ( !list.contains( attributeName)) {
			return null;
		}
		final TreeMap<Interval<Double>, DynamicDOTItem> treeMap = 
			dynamicDOTItemManager.get( dynamicDOTItemManager.getKeyString( subscriptionData, attributeName));
		if ( treeMap == null) {
			return null;
		}
		Interval<Double> valInterval = new Interval<Double>( value, value);
		final Entry<Interval<Double>, DynamicDOTItem> floorEntry = treeMap.floorEntry(valInterval);
		if ( floorEntry != null) {
			final Interval<Double> floorKey = floorEntry.getKey();
			if ((floorKey.getLowerBound() <= value) && (value <= floorKey.getUpperBound())) {
				return floorEntry.getValue();
			}
		}
		final Entry<Interval<Double>, DynamicDOTItem> ceilingEntry = treeMap.ceilingEntry(valInterval);
		if ( ceilingEntry != null) {
			final Interval<Double> ceilingKey = ceilingEntry.getKey();
			if ((ceilingKey.getLowerBound() <= value) && (value <= ceilingKey.getUpperBound())) {
				return ceilingEntry.getValue();
			}
		}
		return null;
	}
	
	public DisplayObjectTypeItem getDisplayObjectTypeItemForState(
			final String primitiveFormName, final DOTProperty property,
			final DOTSubscriptionData subscriptionData, final DataState dataState) {
		if ( _isStaticMap.get( property)) {
			return null;
		}
		final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
		final String keyString1;
		if ( dataState.equals( DataState.NO_DATA)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_DATEN_STATUS);
		} else if ( dataState.equals( DataState.NO_SOURCE)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_QUELLE_STATUS);
		} else if ( dataState.equals( DataState.NO_RIGHTS)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_RECHTE_STATUS);
		} else {
			keyString1 = null;
		}
		if ( keyString1 != null) {	// einer der Substati hat gezogen ...
			final TreeMap<Interval<Double>, DynamicDOTItem> treeMap1 = 
				dynamicDOTItemManager.get( keyString1);
			if ( (treeMap1 != null) && (treeMap1.size() == 1)) { // ... und ist definiert
				return treeMap1.values().toArray( new DisplayObjectTypeItem[1])[0];
			}
		}
		// den übergreifenden Status überprüfen
		final String keyString2 = dynamicDOTItemManager.getKeyString( subscriptionData, 
				DynamicDefinitionComponent.LEERE_DATEN_STATUS);
		final TreeMap<Interval<Double>, DynamicDOTItem> treeMap2 = 
			dynamicDOTItemManager.get( keyString2);
		if ( (treeMap2 != null) && (treeMap2.size() == 1)) { // ... er ist definiert
			return treeMap2.values().toArray( new DisplayObjectTypeItem[1])[0];
		} else {	// ... dann bleibt nur der Default
			return DynamicDOTItem.NO_DATA_ITEM;
		}
	}
	
	public void displayObjectTypeAdded(DisplayObjectType displayObjectType) {
	}
	
	public void displayObjectTypeChanged(DisplayObjectType displayObjectType) {
		if ( displayObjectType.equals( this)) {
			return;
		}
		if ( displayObjectType.getName().equals( _name)) {
			DefaultDisplayObjectType defaultDisplayObjectType = (DefaultDisplayObjectType) displayObjectType;
			_info = displayObjectType.getInfo();
			_isStaticMap.clear();
			for ( DOTProperty property : defaultDisplayObjectType._isStaticMap.keySet()) {
				_isStaticMap.put( property, defaultDisplayObjectType._isStaticMap.get( property));
			}
			_staticPropertyValues.clear();
			for ( DOTProperty property : defaultDisplayObjectType._staticPropertyValues.keySet()) {
				_staticPropertyValues.put( property, defaultDisplayObjectType._staticPropertyValues.get( property));
			}
			_dynamicDOTItemManagers.clear();
			for ( DOTProperty property : defaultDisplayObjectType._dynamicDOTItemManagers.keySet()) {
				_dynamicDOTItemManagers.put( property, defaultDisplayObjectType._dynamicDOTItemManagers.get( property));
			}
		}
	}
	
	public void displayObjectTypeRemoved(String displayObjectTypeName) {
	}
	
	@Override
	public boolean equals( Object o) {
		if ( !(o instanceof DefaultDisplayObjectType )) {
			return false;
		}
		final DefaultDisplayObjectType d = (DefaultDisplayObjectType) o;
		if ( !getDisplayObjectTypePlugin().getName().equals( d.getDisplayObjectTypePlugin().getName())) {
			return false;	// Subklasse stimmt nicht!
		}
		if ( !_name.equals(d._name)) {
			return false;
		}
		if ( !_info.equals(d._info)) {
			return false;
		}
		// Erst die Größen vergleichen ...
		if ( _isStaticMap.size() != d._isStaticMap.size()) {
			return false;
		}
		if ( _staticPropertyValues.size() != d._staticPropertyValues.size()) {
			return false;
		}
		if (_dynamicDOTItemManagers.size() != d._dynamicDOTItemManagers.size()) {
			return false;
		}
		// und dann die Container:
		for ( DOTProperty key : _isStaticMap.keySet()) {
			final boolean b1 = _isStaticMap.get(key);
			final boolean b2 = d._isStaticMap.get(key);
			if ( b1 != b2) {
				return false;
			}
		}
		for ( DOTProperty key : _staticPropertyValues.keySet()) {
			final Object dObject = d._staticPropertyValues.get( key);
			if ( dObject == null) {
				return false;
			}
			final Object object = _staticPropertyValues.get(key);
			if ( key == DOTProperty.FARBE) {
				String dColor = (String) dObject;
				String color = (String) object;
				if ( !dColor.equals( color)) {
					return false;
				}
			} else if ( key == DOTProperty.ABSTAND) {
				Integer dInt = (Integer) dObject;
				Integer myInt = (Integer) object;
				if ( dInt != myInt) {
					return false;
				}
			} else if ( key == DOTProperty.STRICHBREITE) {
				Double dDouble = (Double) dObject;
				Double myDouble = (Double) object;
				if ( dDouble != myDouble) {
					return false;
				}
			}
		}
		for ( DOTProperty key : _dynamicDOTItemManagers.keySet()) {
			final DynamicDOTItemManager dMan = d._dynamicDOTItemManagers.get(key);
			if ( dMan == null) {
				return false;
			}
			final DynamicDOTItemManager myMan = _dynamicDOTItemManagers.get(key);
			if ( !dMan.equals( myMan)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return _name.hashCode();
	}
	
	@Override
	public String toString() {
		String s = "[" + getClass().getName() + ":" + _name + " (" + _info + "), [statisch:";
		for ( DOTProperty property : _isStaticMap.keySet()) {
			if ( _isStaticMap.get( property)) {
				s += property.toString() + "=" + _staticPropertyValues.get( property).toString();
			}
		}
		s += "], [dynamisch:";
		
		for ( DOTProperty property : _isStaticMap.keySet()) {
			if ( !_isStaticMap.get( property)) {
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
				for ( TreeMap<Interval<Double>, DynamicDOTItem> treeMap : dynamicDOTItemManager.getTreeMaps() ) {
					final Set<Interval<Double>> keySet = treeMap.keySet();
					for ( Interval<Double> key : keySet) {
						s += "[Interval:" + key.getLowerBound() + "," + key.getUpperBound() + "]";
						DynamicDOTItem dotfv = (DynamicDOTItem) treeMap.get(key);
						s += "[" + dotfv.toString() + "]";
					}
					s += "]";
				}
			}
		}
		s += "]]";
		return s; 
	}
	
	/* Abstrakte Methoden */
	
	/**
	 * Jede nicht-abstrakte Subklasse muss ihre Plugin-Selbstbeschreibung angeben können.
	 * 
	 * @return die zugehörige Plugin-Selbstbeschreibung
	 */
	abstract public DisplayObjectTypePlugin getDisplayObjectTypePlugin();
	
	protected String _name = "";
	protected String _info = "";
	// Die folgende Map definiert, welche Eigenschaften statisch sind. Für die beiden folgenden
	// Maps gilt: _staticPropertyValues enthält für jede statische Property einen Eintrag,
	// während _dynamicDOTItemManagers für jede (!) Property einen Eintrag enthält, denn
	// die DynamicDOTItemManager-Objekte in dieser Map sind in DOTLineDialog ja die TableModels.
	final protected Map<DOTProperty, Boolean> _isStaticMap = new HashMap<DOTProperty, Boolean>();
	final protected Map<DOTProperty, Object> _staticPropertyValues = new HashMap<DOTProperty, Object>();
	final protected Map<DOTProperty, DynamicDOTItemManager> _dynamicDOTItemManagers = 
		new HashMap<DOTProperty, DynamicDOTItemManager>();
	
	public static final String DEFAULT_COLOR_NAME = "hellgrau";
	public static final Integer DEFAULT_DISTANCE = 0;
	public static final Double DEFAULT_STROKE_WIDTH = 1.;
	public static final Integer DEFAULT_TRANSPARENCY = new Integer(0);
	
	private static final String LOWER_BOUND = "LOWER_BOUND";
	private static final String UPPER_BOUND = "UPPER_BOUND";
	private static final String INFO = "INFO";
}
