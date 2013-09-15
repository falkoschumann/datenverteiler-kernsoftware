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

package de.bsvrz.kex.kexdav.correspondingObjects;

import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

/**
 * Korrespondierendes Objekt auf 2 Datenverteilern
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9217 $
 */
public class CorrespondingObject {

	private final KExDaVObject _localObject;

	private final KExDaVObject _remoteObject;

	private final ObjectSpecification _objectSpecification;

	/**
	 * Erstellt ein neues Korrespondierendes Objekt
	 * @param objectSpecification Pid
	 * @param objectManagerInterface Verwaltung korrespondierender Objekte
	 * @param manager Verwaltung KExDaV
	 */
	public CorrespondingObject(
			final ObjectSpecification objectSpecification, final ObjectManagerInterface objectManagerInterface, final ManagerInterface manager) {
		if(objectSpecification == null) throw new IllegalArgumentException("pid ist null");
		if(objectManagerInterface == null) throw new IllegalArgumentException("objectManagerInterface ist null");
		if(manager == null) throw new IllegalArgumentException("manager ist null");
		_objectSpecification = objectSpecification;
		_localObject = new KExDaVObject(objectSpecification, objectManagerInterface.getLocalConnection(), manager);
		_remoteObject = new KExDaVObject(objectSpecification, objectManagerInterface.getRemoteConnection(), manager);
	}

	/**
	 * Gibt das Objekt auf dem lokalen System zurück
	 * @return Klasse, die Informationen über das Objekt wie Existenz usw. bietet.
	 */
	public KExDaVObject getLocalObject() {
		return _localObject;
	}

	/**
	 * Gibt das Remote-Objekt zurück
	 * @return Klasse, die Informationen über das Objekt wie Existenz usw. bietet.
	 */
	public KExDaVObject getRemoteObject() {
		return _remoteObject;
	}

	/**
	 * Gibt die Objekt-Spezifikation zurück
	 * @return die Objekt-Spezifikation durch die das Objekt ausgewählt wird
	 */
	public ObjectSpecification getObjectSpecification() {
		return _objectSpecification;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final CorrespondingObject other = (CorrespondingObject)o;

		if(!_objectSpecification.equals(other._objectSpecification)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return _objectSpecification.hashCode();
	}

	@Override
	public String toString() {
		return _objectSpecification.toString();
	}
}
