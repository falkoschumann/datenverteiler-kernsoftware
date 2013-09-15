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
 * Ein DOTSubscriptionData kapselt die für eine Anmeldung auf dynamische Daten
 * notwendige Informationen, also Attributgrupope und Aspekt.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
public class DOTSubscriptionData implements Comparable<Object>{
	
	
	/**
	 * Ein DOTSubscriptionData kapselt die für eine Anmeldung auf dynamische Daten
	 * notwendige Informationen, d.h. Attributgrupope und Aspekt.
	 * 
     * @param attributeGroup
     * @param aspect
     */
    public DOTSubscriptionData(String attributeGroup, String aspect) {
	    super();
	    _attributeGroup = attributeGroup;
	    _aspect = aspect;
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
     * Setzt die Attributgruppe.
     * 
     * @param attributeGroup die Attributgruppe
     */
    public void setAttributeGroup(String attributeGroup) {
    	_attributeGroup = attributeGroup;
    }
	/**
     * Gibt den Aspekt zurück.  
     * 
     * @return der Aspekt
     */
    public String getAspect() {
    	return _aspect;
    }
	/**
     * Setzt den Aspekt. 
     * 
     * @param aspect der Aspekt
     */
    public void setAspect(String aspect) {
    	_aspect = aspect;
    }
    
    /**
     * Vergleicht Attributgruppe und Aspekt mit String.equals().
     * 
     * @return <code>true</code> genau dann, wenn Attributgruppe und Aspekt gleich sind
     */
    @Override
    public boolean equals(Object o) {
    	if ( !(o instanceof DOTSubscriptionData)) {
    		return false;
    	}
    	DOTSubscriptionData d = (DOTSubscriptionData) o;
    	if ( !(_attributeGroup.equals(d._attributeGroup)) || !(_aspect.equals(d._aspect))) {
    		return false;
    	}
    	return true;
    }
    
    /**
     * Addiert die Hashcodes von Attributgruppe und Aspekt.
     * 
     * @return Summe der Hashcodes
     */
    @Override
    public int hashCode() {
    	return (_attributeGroup.hashCode() + _aspect.hashCode());
    }
    
    /**
     * Eine einfache Selbstbeschreibung.
     * 
     * @return die Selbstbeschreibung
     */
    @Override
    public String toString() {
    	return "DOTSubscriptionData[AttributeGroup=" + _attributeGroup + ", Aspect=" + _aspect + "]";
    }
    
	public int compareTo(Object o) {
		DOTSubscriptionData d = (DOTSubscriptionData) o;
		int atgHash = _attributeGroup.hashCode() - d._attributeGroup.hashCode();
	    if ( atgHash != 0) {
	    	return atgHash;
	    } else {
	    	return _aspect.hashCode() - d._aspect.hashCode();
	    }
    }
    
    private String _attributeGroup;
	private String _aspect;
}
