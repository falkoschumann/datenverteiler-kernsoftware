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

import de.bsvrz.kex.kexdav.dataexchange.DataExchangeStrategy;

/**
 * Spezifikation zum Datenaustausch
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9084 $
 */
public class DataExchangeIdentification {

	private final CorrespondingAttributeGroup _attributeGroup;

	private final CorrespondingAspect _aspect;

	private final short _simLocal;

	private final short _simRemote;

	private final boolean _delta;

	private final boolean _delayed;

	private final DataExchangeStrategy _direction;

	/**
	 * Konstruktor
	 * @param attributeGroup Attributgruppe
	 * @param aspect Aspekt
	 * @param simLocal Lokale Simulationsvariante
	 * @param simRemote Remote Simulationsvariante
	 * @param delta Delta
	 * @param delayed Delayed
	 * @param direction Austauschrichtung
	 */
	public DataExchangeIdentification(
			final CorrespondingAttributeGroup attributeGroup,
			final CorrespondingAspect aspect,
			final short simLocal,
			final short simRemote,
			final boolean delta,
			final boolean delayed,
			final DataExchangeStrategy direction) {

		_attributeGroup = attributeGroup;
		_aspect = aspect;
		_simLocal = simLocal;
		_simRemote = simRemote;
		_delta = delta;
		_delayed = delayed;
		_direction = direction;
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
	 * Gibt die lokale Simulationsvariante zurück
	 * @return die lokale Simulationsvariante
	 */
	public short getSimLocal() {
		return _simLocal;
	}

	/**
	 * Gibt die remote-Simulationsvariante zurück
	 * @return die remote-Simulationsvariante
	 */
	public short getSimRemote() {
		return _simRemote;
	}

	/**
	 * Gibt zurück, ob nur geänderte Daten ausgetauscht werden sollen
	 * @return true wenn nur geänderte Daten ausgetauscht werden sollen
	 */
	public boolean isDelta() {
		return _delta;
	}

	/**
	 * Gibt zurück ob auch nachgelieferte Daten ausgetauscht werden sollen
	 * @return true wenn auch nachgelieferte Daten ausgetauscht werden sollen
	 */
	public boolean isDelayed() {
		return _delayed;
	}

	/**
	 * Gibt die Richtung des Datenaustausches zurück
	 * @return die Richtung des Datenaustausches
	 */
	public DataExchangeStrategy getDirection() {
		return _direction;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final DataExchangeIdentification other = (DataExchangeIdentification)o;

		if(_delayed != other._delayed) return false;
		if(_delta != other._delta) return false;
		if(_simLocal != other._simLocal) return false;
		if(_simRemote != other._simRemote) return false;
		if(!_aspect.equals(other._aspect)) return false;
		if(!_attributeGroup.equals(other._attributeGroup)) return false;
		if(_direction != other._direction) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _attributeGroup.hashCode();
		result = 31 * result + _aspect.hashCode();
		result = 31 * result + _simLocal;
		result = 31 * result + _simRemote;
		result = 31 * result + (_delta ? 1 : 0);
		result = 31 * result + (_delayed ? 1 : 0);
		result = 31 * result + _direction.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "DataExchangeIdentification{" + "_attributeGroup=" + _attributeGroup + ", _aspect=" + _aspect + ", _simLocal=" + _simLocal + ", _simRemote="
		       + _simRemote + ", _delta=" + _delta + ", _delayed=" + _delayed + ", _direction=" + _direction + '}';
	}
}
