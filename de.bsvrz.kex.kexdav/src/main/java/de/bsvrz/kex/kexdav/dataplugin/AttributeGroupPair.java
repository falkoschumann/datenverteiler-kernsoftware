/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.dataplugin;

/**
 * Spezifikation eines Attributgruppenpaares für die Benutzung in Daten-Konvertierungs-Plugins
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9132 $
 */
public class AttributeGroupPair {

	private final String _fromAttributeGroup;

	private final String _toAttributeGroup;

	/**
	 * Erzeugt ein neues Attributgruppenpaar
	 * @param fromAttributeGroup Start-Attributgruppe
	 * @param toAttributeGroup Ziel-Attributgruppe
	 */
	public AttributeGroupPair(final String fromAttributeGroup, final String toAttributeGroup) {
		if(fromAttributeGroup == null) throw new IllegalArgumentException("fromAttributeGroup ist null");
		if(toAttributeGroup == null) throw new IllegalArgumentException("toAttributeGroup ist null");
		_fromAttributeGroup = fromAttributeGroup;
		_toAttributeGroup = toAttributeGroup;
	}

	/**
	 * Gibt die Startattributgruppe als String zurück
	 * @return die Startattributgruppe als String
	 */
	public String getFromAttributeGroup() {
		return _fromAttributeGroup;
	}

	/**
	 * Gibt die Zielattributgruppe als String zurück
	 * @return die Zielattributgruppe als String
	 */
	public String getToAttributeGroup() {
		return _toAttributeGroup;
	}

	@Override
	public String toString() {
		return "Konvertierung von " + _fromAttributeGroup + " nach " + _toAttributeGroup;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final AttributeGroupPair other = (AttributeGroupPair)o;

		if(!_fromAttributeGroup.equals(other._fromAttributeGroup)) return false;
		if(!_toAttributeGroup.equals(other._toAttributeGroup)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _fromAttributeGroup.hashCode();
		result = 31 * result + _toAttributeGroup.hashCode();
		return result;
	}
}
