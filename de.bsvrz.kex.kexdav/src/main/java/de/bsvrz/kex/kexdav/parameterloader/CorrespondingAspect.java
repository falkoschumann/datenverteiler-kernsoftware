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
 * Speichert zum Datenaustausch den Aspekt auf der Lokal und Remote-Seite
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9084 $
 */
public class CorrespondingAspect {

	private final String _aspectLocalPid;

	private final String _aspectRemotePid;

	/**
	 * Erstellt einen neuen Aspekt
	 * @param item Data-Wert aus dem KExDaV-Datenmodell
	 */
	public CorrespondingAspect(final Data item) {
		_aspectLocalPid = item.getReferenceValue("AspektLokal").getSystemObjectPid();
		String s = item.getTextValue("AspektRemote").getText();
		if(s.length() == 0) {
			s = _aspectLocalPid;
		}
		_aspectRemotePid = s;
	}

		/**
	 * Erstellt einen neuen Aspekt
		 * @param aspLocal Lokaler Aspekt
		 * @param aspRemote Remote-Aspekt

		 */
	public CorrespondingAspect(final String aspLocal, final String aspRemote) {
		_aspectLocalPid = aspLocal;
		_aspectRemotePid = aspRemote;
	}

	/**
	 * Gibt den lokalen Aspekt zurück
	 *
	 * @return den lokalen Aspekt
	 */
	public String getAspectLocalPid() {
		return _aspectLocalPid;
	}

	/**
	 * Gibt den Remote-Aspekt zurück
	 *
	 * @return den Remote-Aspekt
	 */
	public String getAspectRemotePid() {
		return _aspectRemotePid;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final CorrespondingAspect other = (CorrespondingAspect)o;

		if(!_aspectLocalPid.equals(other._aspectLocalPid)) return false;
		if(!_aspectRemotePid.equals(other._aspectRemotePid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _aspectLocalPid.hashCode();
		result = 31 * result + _aspectRemotePid.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "CorrespondingAspect{" + "_aspectLocal=" + _aspectLocalPid + ", _aspectRemotePid='" + _aspectRemotePid + '\'' + '}';
	}
}
