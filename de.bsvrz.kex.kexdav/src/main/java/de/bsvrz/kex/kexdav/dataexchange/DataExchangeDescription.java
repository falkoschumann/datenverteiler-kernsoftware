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

package de.bsvrz.kex.kexdav.dataexchange;

import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAspect;
import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAttributeGroup;

/**
 * Spezifikationen zum Austausch von Online-Daten
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
class DataExchangeDescription {

	private final String _objectPid;

	private final DataExchangeStrategy _direction;

	private final CorrespondingAttributeGroup _attributeGroup;

	private final CorrespondingAspect _aspect;

	private final short _simLocal;

	private final short _simRemote;

	private final boolean _delayed;

	private final boolean _delta;


	/**
	 * Erstellt ein Modul zum Austausch von Onlinedaten von einem Objekt
	 * @param objectPid Korrespondierendes Objekt
	 * @param direction Richtung des Datenaustausches
	 * @param attributeGroup Attributgruppe
	 * @param aspect Aspekt
	 * @param simLocal Simulationsvariante Lokal
	 * @param simRemote Simulationsvariante Remote
	 * @param delayed Auch nachgelieferte Daten übertragen?
	 * @param delta Nur geänderte Daten übertragen?
	 */
	public DataExchangeDescription(
			final String objectPid,
			final DataExchangeStrategy direction,
			final CorrespondingAttributeGroup attributeGroup,
			final CorrespondingAspect aspect,
			final short simLocal,
			final short simRemote,
			final boolean delayed,
			final boolean delta) {

		_objectPid = objectPid;
		_direction = direction;
		_attributeGroup = attributeGroup;
		_aspect = aspect;
		_simLocal = simLocal;
		_simRemote = simRemote;
		_delayed = delayed;
		_delta = delta;
	}

	/**
	 * Gibt das Systemobjekt zurück
	 * @return Systemobjekt
	 */
	public String getObjectPid() {
		return _objectPid;
	}

	/**
	 * Gibt die Austauschrichtung zurück
	 * @return die Austauschrichtung
	 */
	public DataExchangeStrategy getDirection() {
		return _direction;
	}

	/**
	 * Gibt die Attributgruppe zurück
	 * @return die Attributgruppe
	 */
	public CorrespondingAttributeGroup getAttributeGroup() {
		return _attributeGroup;
	}

	/**
	 * Gibt den Aspekt zurück
	 * @return den Aspekt
	 */
	public CorrespondingAspect getAspect() {
		return _aspect;
	}

	/**
	 * Gibt die Lokale Simulationsvariante zurück
	 * @return die Lokale Simulationsvariante
	 */
	public short getSimLocal() {
		return _simLocal;
	}

	/**
	 * Gibt die Remote-Simulationsvariante zurück
	 * @return die Remote-Simulationsvariante
	 */
	public short getSimRemote() {
		return _simRemote;
	}

	/**
	 * Gibt zurück ob auch nachgelieferte Daten übertragen werden sollen
	 * @return true wenn Delayed-Daten übertragen werden sollen
	 */
	public boolean isDelayed() {
		return _delayed;
	}

	/**
	 * Gibt zurück ob nur geänderte Daten übertragen werden sollen
	 * @return true wenn nur geänderte Daten übertragen werden sollen
	 */
	public boolean isDelta() {
		return _delta;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final DataExchangeDescription other = (DataExchangeDescription)o;

		if(_delayed != other._delayed) return false;
		if(_delta != other._delta) return false;
		if(_simLocal != other._simLocal) return false;
		if(_simRemote != other._simRemote) return false;
		if(!_aspect.equals(other._aspect)) return false;
		if(!_attributeGroup.equals(other._attributeGroup)) return false;
		if(_direction != other._direction) return false;
		if(!_objectPid.equals(other._objectPid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _objectPid.hashCode();
		result = 31 * result + _direction.hashCode();
		result = 31 * result + _attributeGroup.hashCode();
		result = 31 * result + _aspect.hashCode();
		result = 31 * result + (int)_simLocal;
		result = 31 * result + (int)_simRemote;
		result = 31 * result + (_delayed ? 1 : 0);
		result = 31 * result + (_delta ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "DataExchangeDescription{" + "_object=" + _objectPid + ", _direction=" + _direction + ", _attributeGroup=" + _attributeGroup + ", _aspect="
		       + _aspect + ", _simLocal=" + _simLocal + ", _simRemote=" + _simRemote + ", _delayed=" + _delayed + ", _delta=" + _delta + '}';
	}
}
