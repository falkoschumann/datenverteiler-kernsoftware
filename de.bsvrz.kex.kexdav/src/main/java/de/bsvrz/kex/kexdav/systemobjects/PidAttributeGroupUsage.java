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

package de.bsvrz.kex.kexdav.systemobjects;

/**
 * Diese Klasse speichert eine Attributgruppenverwendung als Pids zum Datenaustausch zwischen Konfigurationen/Datenverteilern
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9047 $
 */
public class PidAttributeGroupUsage {

	private final String _attributeGroupPid;

	private final String _aspectPid;

	PidAttributeGroupUsage(final String attributeGroupPid, final String aspectPid) {
		if(attributeGroupPid == null) throw new IllegalArgumentException("attributeGroupPid ist null");
		if(aspectPid == null) throw new IllegalArgumentException("aspectPid ist null");

		_attributeGroupPid = attributeGroupPid;
		_aspectPid = aspectPid;
	}

	/**
	 * Gibt die Attributgruppen-Pid zurück
	 * @return die Attributgruppen-Pid
	 */
	public String getAttributeGroupPid() {
		return _attributeGroupPid;
	}

	/**
	 * Gibt die Aspekt-Pid zurück
	 * @return die Aspekt-Pid
	 */
	public String getAspectPid() {
		return _aspectPid;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final PidAttributeGroupUsage other = (PidAttributeGroupUsage)o;

		if(!_aspectPid.equals(other._aspectPid)) return false;
		if(!_attributeGroupPid.equals(other._attributeGroupPid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _attributeGroupPid.hashCode();
		result = 31 * result + _aspectPid.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PidAttributeGroupUsage{" + _attributeGroupPid + ", " + _aspectPid + '}';
	}
}
