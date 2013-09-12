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

package de.bsvrz.kex.kexdav.parameterloader;

import de.bsvrz.dav.daf.main.Data;

/**
 * Speichert zum Datenaustausch die Attributgruppe auf der Lokal und Remote-Seite
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9218 $
 */
public class CorrespondingAttributeGroup {

	private final String _atgLocalPid;

	private final String _atgRemotePid;

	/**
	 * Erstellt eine Attributgruppen-Angabe
	 *
	 * @param item Data-Objekt aus dem KExDaV-Datenmodell
	 */
	public CorrespondingAttributeGroup(final Data item) {
		_atgLocalPid = item.getReferenceValue("AtgLokal").getSystemObjectPid();
		String s = item.getTextValue("AtgRemote").getText();
		if(s.length() == 0) {
			s = _atgLocalPid;
		}
		_atgRemotePid = s;
	}

	/**
	 * Erstellt eine Attributgruppen-Angabe
	 *
	 * @param atgLocalPid Lokale Pid
	 * @param atgRemote   Remote-Pid
	 */
	public CorrespondingAttributeGroup(final String atgLocalPid, final String atgRemote) {
		_atgLocalPid = atgLocalPid;
		_atgRemotePid = atgRemote;
	}

	/**
	 * Gibt die Attributgruppe auf der Lokalen Seite zurück
	 *
	 * @return die Attributgruppe auf der Lokalen Seite
	 */
	public String getAtgLocalPid() {
		return _atgLocalPid;
	}

	/**
	 * Gibt die  Remote-Attributgruppe als Pid zurück
	 *
	 * @return die  Remote-Attributgruppe als Pid
	 */
	public String getAtgRemotePid() {
		return _atgRemotePid;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final CorrespondingAttributeGroup other = (CorrespondingAttributeGroup)o;

		if(!_atgLocalPid.equals(other._atgLocalPid)) return false;
		if(!_atgRemotePid.equals(other._atgRemotePid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _atgLocalPid.hashCode();
		result = 31 * result + _atgRemotePid.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "CorrespondingAttributeGroup{" + "_atgLocal=" + _atgLocalPid + ", _atgRemotePid='" + _atgRemotePid + '\'' + '}';
	}
}
