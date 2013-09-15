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


/**
 * Ein PrimitiveFormPropertyPair kapselt ein Paar bestehend aus einer Grundfigur 
 * ({@link PrimitiveForm}), die durch ihren Namen angegeben wird, und einer 
 * Visualisierungs-Eigenschaft ({@link DOTProperty}).
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
public class PrimitiveFormPropertyPair {
	
	/**
	 * Konstruiert ein Objekt aus den übergebenen Daten. Der Name der Grundfigur darf 
	 * <code>null</code> sein, die Eigenschaft nicht.
	 * 
	 * @param primitiveFormName ein Grundfigurname oder <code>null</code>
	 * @param property eine Visualisierungseigenschaft
	 */
	public PrimitiveFormPropertyPair(String primitiveFormName, DOTProperty property) {
		super();
		if ( property == null) {
			throw new IllegalArgumentException("DisplayObject.ObjectPropertyPair muss mit echter DOTProperty konstruiert werden");
		}
		_primitiveFormName = primitiveFormName;
		_property = property;
	}
	
	/**
	 * Der Getter für den Namen der Grundfigur.
	 * 
	 * @return der Grundfigurname oder <code>null</code>
	 */
	public String getPrimitiveFormName() {
		return _primitiveFormName;
	}
	
	/**
	 * Der Getter für die Eigenschaft.
	 * 
	 * @return die Eigenschaft
	 */
	public DOTProperty getProperty() {
		return _property;
	}
	
	/**
	 * Eine einfache Selbstbeschreibung.
	 * 
	 * @return die Selbstbeschreibung
	 */
	@Override
	public String toString() {
		String s = "[ObjectPropertyPair: o=" + _primitiveFormName + ", property=" + _property.toString() + "]";
		return s;
	}
	
	/**
	 * Komponentenweiser Vergleich.
	 * 
	 * @return <code>true</code> genau dann, wenn Gleichheit vorliegt
	 */
	@Override
	public boolean equals( Object o) {
		if ( !(o instanceof PrimitiveFormPropertyPair)) {
			return false;
		}
		PrimitiveFormPropertyPair opp = (PrimitiveFormPropertyPair) o;
		if ( _primitiveFormName == null) {
			if ( opp._primitiveFormName != null) {
				return false;
			} else {
				return _property.equals( opp._property);
			}
		} else {
			if ( _primitiveFormName.equals( opp._primitiveFormName)) {
				return _property.equals( opp._property);
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Ein Hashcode aus den Komponenten
	 * 
	 * @return der Hashcode
	 */
	@Override
	public int hashCode() {
		if ( _primitiveFormName == null) {
			return _property.hashCode();
		} else {
			return _primitiveFormName.hashCode() + _property.hashCode();
		}
	}
	
	final private String _primitiveFormName;
	final private DOTProperty _property;
}
