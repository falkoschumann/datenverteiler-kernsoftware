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

package de.bsvrz.kex.kexdav.objectexchange;

import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.kex.kexdav.main.Direction;

/**
 * Beschreibung zum Austausch von dynamischen Mengen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9066 $
 */
public class SetExchangeDescription {

	private final ConfigurationObject _localObject;

	private final ConfigurationObject _remoteObject;

	private final String _localSetName;

	private final String _remoteSetName;

	private final Direction _strategy;

	/**
	 * Konstruktor
	 * @param localObject lokales Objekt
	 * @param remoteObject remote Objekt
	 * @param localSetName lokale Menge
	 * @param remoteSetName remote Menge
	 * @param strategy Austauschrichtung
	 */
	public SetExchangeDescription(
			final ConfigurationObject localObject,
			final ConfigurationObject remoteObject,
			final String localSetName,
			final String remoteSetName,
			final Direction strategy) {

		_localObject = localObject;
		_remoteObject = remoteObject;
		_strategy = strategy;
		_localSetName = localSetName;
		_remoteSetName = remoteSetName;
	}

	/**
	 * Gibt das lokale Objekt zurück
	 * @return das lokale Objekt
	 */
	public ConfigurationObject getLocalObject() {
		return _localObject;
	}

	/**
	 * Gibt das Remote Objekt zurück
	 * @return das Remote Objekt
	 */
	public ConfigurationObject getRemoteObject() {
		return _remoteObject;
	}

	/**
	 * Gibt die Richtung zurück
	 * @return die Richtung
	 */
	public Direction getStrategy() {
		return _strategy;
	}

	/**
	 * Gibt die lokale Menge zurück
	 * @return die lokale Menge
	 */
	public String getLocalSetName() {
		return _localSetName;
	}

	/**
	 * Gibt die Remote-Menge zurück
	 * @return die Remote-Menge
	 */
	public String getRemoteSetName() {
		return _remoteSetName;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final SetExchangeDescription other = (SetExchangeDescription)o;

		if(!_localObject.equals(other._localObject)) return false;
		if(!_localSetName.equals(other._localSetName)) return false;
		if(!_remoteObject.equals(other._remoteObject)) return false;
		if(!_remoteSetName.equals(other._remoteSetName)) return false;
		if(_strategy != other._strategy) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _localObject.hashCode();
		result = 31 * result + _remoteObject.hashCode();
		result = 31 * result + _localSetName.hashCode();
		result = 31 * result + _remoteSetName.hashCode();
		result = 31 * result + _strategy.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SetExchangeDescription{" + "_localObject=" + _localObject + ", _remoteObject=" + _remoteObject + ", _localSetName='" + _localSetName + '\''
		       + ", _remoteSetName='" + _remoteSetName + '\'' + ", _strategy=" + _strategy + '}';
	}
}
