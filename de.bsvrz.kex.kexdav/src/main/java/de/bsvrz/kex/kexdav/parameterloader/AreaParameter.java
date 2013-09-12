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

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.*;

/**
 * Parameter zur Festlegung der zusätzlichen Austausch-Bereiche
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9193 $
 */
public class AreaParameter {

	private final String _configurationAreaPid;

	private final List<SystemObjectType> _types;

	/**
	 * Erstellt einen neuen AreaParameter
	 * @param configurationAreaPid Pid des Konfigurationsbereichs
	 * @param types zugeordnete Typen
	 */
	AreaParameter(final String configurationAreaPid, final SystemObject[] types) {
		_configurationAreaPid = configurationAreaPid;
		final List<SystemObjectType> list = new ArrayList<SystemObjectType>(types.length);
		for(final SystemObject type : types) {
			list.add((SystemObjectType)type);
		}
		_types = list;
	}

	/**
	 * Gibt den Konfigurationsbereich zurück
	 * @return den Konfigurationsbereich
	 */
	public String getConfigurationAreaPid() {
		return _configurationAreaPid;
	}

	/**
	 * Gibt die Typen zurück
	 * @return die Typen
	 */
	public List<SystemObjectType> getTypes() {
		return Collections.unmodifiableList(_types);
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final AreaParameter other = (AreaParameter)o;

		if(!_configurationAreaPid.equals(other._configurationAreaPid)) return false;
		if(!_types.equals(other._types)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _configurationAreaPid.hashCode();
		result = 31 * result + _types.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "AreaParameter{" + "kb='" + _configurationAreaPid + '\'' + ", types=" + _types + '}';
	}
}
