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

import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAttributeGroup;

/**
 * Spezifikation für den Parameteraustausch
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public class ParameterExchangeDescription {

	private final String _objectPid;

	private final ParameterExchangeStrategy _strategy;

	private final CorrespondingAttributeGroup _atg;

	private final short _simLocal;

	private final short _simRemote;

	private final boolean _delta;

	/**
	 * Spezifikation für den Parameteraustausch
	 * @param objectPid Objekt
	 * @param strategy Strategie
	 * @param atg Attributgruppe
	 * @param simLocal SV lokal
	 * @param simRemote SV remote
	 * @param delta Nur geänderte Daten übertragen?
	 */
	public ParameterExchangeDescription(
			final String objectPid,
			final ParameterExchangeStrategy strategy,
			final CorrespondingAttributeGroup atg,
			final short simLocal,
			final short simRemote,
			final boolean delta) {

		_objectPid = objectPid;
		_strategy = strategy;
		_atg = atg;
		_simLocal = simLocal;
		_simRemote = simRemote;
		_delta = delta;
	}

	/**
	 * Gibt das Objekt zurück
	 * @return das Objekt
	 */
	public String getObjectPid() {
		return _objectPid;
	}

	/**
	 * Gibt die Strategie zurück
	 * @return die Strategie
	 */
	public ParameterExchangeStrategy getStrategy() {
		return _strategy;
	}

	/**
	 * Gibt die Attributgruppe zurück
	 * @return die Attributgruppe
	 */
	public CorrespondingAttributeGroup getAtg() {
		return _atg;
	}

	/**
	 * Gibt die lokale Simulationsvariante zurück
	 * @return die lokale Simulationsvariante
	 */
	public short getSimLocal() {
		return _simLocal;
	}

	/**
	 * Gibt die Remote Simulationsvariante zurück
	 * @return die Remote Simulationsvariante
	 */
	public short getSimRemote() {
		return _simRemote;
	}

	/**
	 * Gibt den Delta-Parameter zurück
	 * @return true wenn nur geänderte Daten übertragen werden sollen
	 */
	public boolean isDelta() {
		return _delta;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ParameterExchangeDescription other = (ParameterExchangeDescription)o;

		if(_delta != other._delta) return false;
		if(_simLocal != other._simLocal) return false;
		if(_simRemote != other._simRemote) return false;
		if(!_atg.equals(other._atg)) return false;
		if(!_objectPid.equals(other._objectPid)) return false;
		if(_strategy != other._strategy) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _objectPid.hashCode();
		result = 31 * result + _strategy.hashCode();
		result = 31 * result + _atg.hashCode();
		result = 31 * result + (int)_simLocal;
		result = 31 * result + (int)_simRemote;
		result = 31 * result + (_delta ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ParameterExchangeDescription{" + "_object=" + _objectPid + ", _strategy=" + _strategy + ", _atg=" + _atg + ", _simLocal=" + _simLocal
		       + ", _simRemote=" + _simRemote + ", _delta=" + _delta + '}';
	}
}
