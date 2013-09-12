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


import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;

import java.awt.Color;
import java.util.prefs.Preferences;

/**
 * Ein DynamicDOTItem ist die kleinste Einheit bei der Verkapselung der Verwaltung 
 * der Informationen zu einer veränderlichen Größe. Dazu kennt das Item Attributgruppe,
 * Aspekt und Attribut, die die Dynamik beschreiben, besitzt eine Kurzbeschreibung
 * seiner Information und kennt gegebenenfalls den Wert für die dynamische Eigenschaft
 * (z.B. eine Zahlwert für Strichbreite, eine Farbe oder einen Text).
 * Die Implementation besteht ausschließlich aus Gettern, Settern und einfachen
 * Dienstleistungsmethoden wie dem Abspeichern in den Präferenzen. 
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
public class DynamicDOTItem implements DisplayObjectTypeItem, Comparable<Object> {
	
	/**
	 * Konstruiert ein DynamicDOTItem aus den übergebenen Informationen. Ein DynamicDOTItem ist
	 * gültig, wenn der übergebene Wert <code>propertyValue</code> vom Typ Integer, Double,
	 * String oder Color ist. Ist er nicht von diesem Typ, so wird derzeit eine IllegalArgumentException
	 * ausgelöst, da es sich höchstwahrscheinlich um eine unvollständige Erweiterung des Kodes
	 * handelt: die vier Klassen sind gerade die Wertebereiche aller im Moment definierten
	 * {@link DOTProperty Eigenschaften}.
	 * 
	 * @param attributeGroup die Attributgruppe
	 * @param aspect der Aspekt
	 * @param attributeName der Attributname
	 * @param description die Beschreibung
	 * @param propertyValue der Eigenschaftswert
	 */
	public DynamicDOTItem( String attributeGroup, 
			String aspect, 
			String attributeName,
			String description,
			Object propertyValue) {
		_attributeGroup = attributeGroup;
		_aspect = aspect;
		_attributeName = attributeName;
		_description = description;
		_propertyValue = propertyValue;
		if ( _propertyValue == null) {
			_propertyValueClass = null;
		} else if ( _propertyValue instanceof Integer) {
			_propertyValueClass = "Integer";
		} else if ( _propertyValue instanceof Double) {
			_propertyValueClass = "Double";
		} else if ( _propertyValue instanceof String) {
			_propertyValueClass = "String";
		} else if ( _propertyValue instanceof Color) {
			_propertyValueClass = "Color";
		} else {
			_isValid = false;
			throw new IllegalArgumentException( "Die Klasse des Property-Werts wird nicht unterstützt.");
		}
		_isValid = true;
	}
	
	/**
	 * Gibt die Attributgruppe zurück.
	 * 
	 * @return die Attributgruppe 
	 */
	public String getAttributeGroup() {
		return _attributeGroup;
	}
	/**
	 * Gibt den Aspekt zurück.
	 * 
	 * @return den Aspekt
	 */
	public String getAspect() {
		return _aspect;
	}
	
	/**
	 * Gibt den Namen des Attributs zurück.
	 * 
	 *  @return den Attributnamen
	 */
	public String getAttributeName() {
		return _attributeName;
	}
	
	/**
	 * Gibt die Beschreibung zurück.
	 * 
	 * @return die Beschreibung
	 */
	public String getDescription() {
		return _description;
	}
	
	/**
	 * Gibt den Wert der Eigenschaft zurück. 
	 * 
	 * @return den Eigenschaftswert
	 */
	public Object getPropertyValue() {
		return _propertyValue;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn das Objekt gültig ist, und <code>false</code> sonst. 
	 * 
	 * @return ist das Item gültig?
	 */
	public boolean isValid() {
		return _isValid;
	}
	
	/**
	 * Die Implementation vergleicht die 5 Bestandteile der Items mit equals() von String oder Object.
	 * 
	 * @return <code>true</code> genau dann, wenn Gleichheit vorliegt
	 */
	@Override
	public boolean equals( Object o) {
		if ( !(o instanceof DynamicDOTItem )) {
			return false;
		}
		if ( this == o) {
			return true;
		}
		DynamicDOTItem d = (DynamicDOTItem) o;
		if ( !_attributeGroup.equals( d._attributeGroup) || !_aspect.equals( d._aspect) || 
				!_attributeName.equals(d._attributeName) || !_description.equals(d._description)) {
			return _propertyValue.equals( d._propertyValue);
		} else {
			return true;
		}
	}
	
	/**
	 * Addiert die Hashcodes von Attributgruppenname, Aspektname und Attributname.
	 * 
	 * @return die Summe der Hashcodes
	 */
	@Override
	public int hashCode () {
		return (_attributeGroup.hashCode() + _aspect.hashCode() + _attributeName.hashCode());
	}
	
	/**
	 * Eine einfache Selbstbeschreibung.
	 * 
	 * @return die Selbstbeschreibung
	 */
	@Override
	public String toString() {
		return getClass().getName() + "[ attributeGroup=" + _attributeGroup + ", aspect=" 
		+ _aspect + ", attributename=" + _attributeName + ", description=" + _description
		+ ", propertyValue=" + _propertyValue.toString()+ "]";
	}
	
	public int compareTo(Object o) {
		DynamicDOTItem otherItem = (DynamicDOTItem) o;
		final int atgHash = _attributeGroup.hashCode() - otherItem._attributeGroup.hashCode();
		if ( atgHash != 0) {
			return atgHash;
		} else {
			int aspectHash = _aspect.hashCode() - otherItem._aspect.hashCode();
			if ( aspectHash != 0) {
				return aspectHash;
			} else {
				int anHash = _attributeName.hashCode() - otherItem._attributeName.hashCode();
				if ( anHash != 0) {
					return anHash;
				}
				int descHashCode = _description.hashCode() - otherItem._description.hashCode();
				if ( descHashCode != 0) {
					return descHashCode;
				}
				return _propertyValue.hashCode() - otherItem._propertyValue.hashCode();
			}
		}
	}
	/**
	 * Speichert das Item unter dem übergebenen Knoten ab.
	 * 
	 * @param prefs der Knoten, unter dem die Speicherung beginnt
	 */
	public void putPreferences( Preferences prefs) {
		prefs.put(ATTRIBUTE_GROUP, _attributeGroup);
		prefs.put(ASPECT, _aspect);
		prefs.put(ATTRIBUTE_NAME, _attributeName);
		prefs.put(DESCRIPTION, _description);
		prefs.put(PROPERTY_VALUE_CLASS, _propertyValueClass);
		if ( _propertyValueClass.equals( "Integer")) {
			prefs.putInt( PROPERTY_VALUE, (Integer) _propertyValue);
		} else if ( _propertyValueClass.equals( "Double")) {
			prefs.putDouble( PROPERTY_VALUE, (Double) _propertyValue);
		} else if ( _propertyValueClass.equals( "String")) {
			prefs.put( PROPERTY_VALUE, (String) _propertyValue);
		} else if ( _propertyValueClass.equals( "Color")) {
			Color color = (Color) _propertyValue;
			prefs.putInt(COLOR_RED, color.getRed());
			prefs.putInt(COLOR_BLUE, color.getBlue());
			prefs.putInt(COLOR_GREEN, color.getGreen());
			prefs.putInt(COLOR_ALPHA, color.getAlpha());
		} else {
			throw new IllegalArgumentException( "Ein DynamicDOTItem kann nicht in den Preferences gespeichert werden.");
		}
	}
	
	/**
	 * Initialisiert das Item aus dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Initialisierung beginnt
	 */
	public DynamicDOTItem(Preferences prefs) {
		_attributeGroup = prefs.get(ATTRIBUTE_GROUP, "");
		_aspect = prefs.get(ASPECT, "");
		_attributeName = prefs.get(ATTRIBUTE_NAME, "");
		_description = prefs.get( DESCRIPTION, "");
		_propertyValueClass = prefs.get( PROPERTY_VALUE_CLASS, "");
		if ( _propertyValueClass.length() == 0) {
			_propertyValue = null;
			_isValid = false;
			throw new IllegalAccessError("Initialisierung aus den Preferences fehlgeschlagen!");
		}
		if ( _propertyValueClass.equals( "Integer")) {
			_propertyValue = prefs.getInt( PROPERTY_VALUE, -1);
		} else if ( _propertyValueClass.equals( "Double")) {
			_propertyValue = prefs.getDouble( PROPERTY_VALUE, Double.MIN_VALUE);
		} else if ( _propertyValueClass.equals( "String")) {
			_propertyValue = prefs.get( PROPERTY_VALUE, "");
		} else if ( _propertyValueClass.equals( "Color")) {
			int red = prefs.getInt( COLOR_RED, -1);
			int blue = prefs.getInt( COLOR_BLUE, -1);
			int green = prefs.getInt( COLOR_GREEN, -1);
			int alpha = prefs.getInt( COLOR_ALPHA, -1);
			if ( (red != -1) && (blue != -1) && (green != -1) && (alpha != -1)) {
				_propertyValue = new Color( red, blue, green, alpha);
			} else {
				_propertyValue = null;
				_isValid = false;
				throw new IllegalAccessError("Initialisierung aus den Preferences fehlgeschlagen!");
			}
		} else {
			_propertyValue = null;
			_isValid = false;
			throw new IllegalAccessError("Initialisierung aus den Preferences fehlgeschlagen!");
		}
		_isValid = true;
	}
	
	/**
	 * Estellt ein neues Item mit denselben Werten.
	 * 
	 * @return die Kopie
	 */
	public DynamicDOTItem getCopy() {
		DynamicDOTItem newDOTItem = new DynamicDOTItem( getAttributeGroup(), getAspect(), 
				getAttributeName(), getDescription(), getPropertyValue());
		return newDOTItem;
	}
	
	final private String _attributeGroup;
	final private String _aspect;
	final private String _attributeName;
	final private String _description;
	final private Object _propertyValue;
	final private String _propertyValueClass;
	final private boolean _isValid;
	
	private static final String ATTRIBUTE_GROUP = "ATTRIBUTE_GROUP";
	private static final String ASPECT = "ASPECT";
	private static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
	private static final String DESCRIPTION = "DESCRIPTION";
	private static final String PROPERTY_VALUE = "PROPERTY_VALUE";
	private static final String PROPERTY_VALUE_CLASS = "PROPERTY_VALUE_CLASS";
	private static final String COLOR_RED = "COLOR_RED";
	private static final String COLOR_BLUE = "COLOR_BLUE";
	private static final String COLOR_GREEN = "COLOR_GREEN";
	private static final String COLOR_ALPHA = "COLOR_ALPHA";
	
	public static final DynamicDOTItem NO_DATA_ITEM = new DynamicDOTItem(null, null, null, null, null);
	public static final DynamicDOTItem NO_SOURCE_ITEM = new DynamicDOTItem(null, null, null, null, null);
}
