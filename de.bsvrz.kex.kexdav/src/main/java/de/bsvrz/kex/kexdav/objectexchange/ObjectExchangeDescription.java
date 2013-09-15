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

import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

/**
 * Beschreibung zum Objekt-Austausch
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9217 $
 */
public class ObjectExchangeDescription {

	private final ObjectSpecification _objectSpecification;

	private final Direction _strategy;

	/**
	 * Erstellt eine neue Objektaustauschbeschreibung
	 * @param objectSpecification Pid
	 * @param strategy Richtung
	 */
	public ObjectExchangeDescription(final ObjectSpecification objectSpecification, final Direction strategy) {
		_objectSpecification = objectSpecification;
		_strategy = strategy;
	}

	/**
	 * Gibt die Objekt-Spezifikation zurück
	 * @return die Objekt-Spezifikation
	 */
	public ObjectSpecification getObjectSpecification() {
		return _objectSpecification;
	}

	/**
	 * Gibt die Austauschrichtung zurück
	 * @return die Austauschrichtung
	 */
	public Direction getStrategy() {
		return _strategy;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ObjectExchangeDescription other = (ObjectExchangeDescription)o;

		if(!_objectSpecification.equals(other._objectSpecification)) return false;
		if(_strategy != other._strategy) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _objectSpecification.hashCode();
		result = 31 * result + _strategy.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ObjectExchangeDescription{" + "_pid='" + _objectSpecification + '\'' + ", _strategy=" + _strategy + '}';
	}
}
