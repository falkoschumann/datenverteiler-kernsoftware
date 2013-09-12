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

import de.bsvrz.kex.kexdav.main.Direction;

/**
 * Spezifikation zum Austausch von Mengen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9218 $
 */
public class ExchangeSetParameter {

	private final String _localObject;

	private final String _localSet;

	private final String _remoteObject;

	private final String _remoteSet;

	private final Direction _direction;

	/**
	 * Erstellt einen Mengenaustauschparameter
	 * @param localObject Lokales Objekt
	 * @param localSet Lokale Menge
	 * @param remoteObject Remote-Objekt
	 * @param remoteSet Remote-Menge
	 * @param direction Richtung
	 */
	public ExchangeSetParameter(
			final String localObject, final String localSet, final String remoteObject, final String remoteSet, final Direction direction) {
		_localObject = localObject;
		_localSet = localSet;
		_remoteObject = remoteObject;
		_remoteSet = remoteSet;
		_direction = direction;
	}

	/**
	 * Gibt das lokale Objekt zurück
	 * @return das lokale Objekt
	 */
	public String getLocalObject() {
		return _localObject;
	}

	/**
	 * Gibt die lokale Menge zurück
	 * @return die lokale Menge
	 */
	public String getLocalSet() {
		return _localSet;
	}

	/**
	 * Gibt das remote Objekt zurück
	 * @return das remote Objekt
	 */
	public String getRemoteObject() {
		return _remoteObject;
	}

	/**
	 * Gibt die remote Menge zurück
	 * @return die remote Menge
	 */
	public String getRemoteSet() {
		return _remoteSet;
	}

	/**
	 * Gibt die Austausch-Richtung zurück
	 * @return die Austausch-Richtung
	 */
	public Direction getDirection() {
		return _direction;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ExchangeSetParameter other = (ExchangeSetParameter)o;

		if(!_localObject.equals(other._localObject)) return false;
		if(!_localSet.equals(other._localSet)) return false;
		if(!_remoteObject.equals(other._remoteObject)) return false;
		if(!_remoteSet.equals(other._remoteSet)) return false;
		if(_direction != other._direction) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _localObject.hashCode();
		result = 31 * result + _localSet.hashCode();
		result = 31 * result + _remoteObject.hashCode();
		result = 31 * result + _remoteSet.hashCode();
		result = 31 * result + _direction.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ExchangeSetParameter{" + "_localObject='" + _localObject + '\'' + ", _localSet='" + _localSet + '\'' + ", _remoteObject='" + _remoteObject
		       + '\'' + ", _remoteSet='" + _remoteSet + '\'' + ", _strategy=" + _direction + '}';
	}
}
