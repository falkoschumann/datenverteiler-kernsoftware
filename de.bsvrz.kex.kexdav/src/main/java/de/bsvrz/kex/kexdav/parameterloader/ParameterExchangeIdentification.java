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

import de.bsvrz.kex.kexdav.dataexchange.ParameterExchangeStrategy;

import java.util.Collections;
import java.util.List;

/**
 * Parameteraustausch-Spezifikation
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9084 $
 */
public class ParameterExchangeIdentification {

	private final List<CorrespondingAttributeGroup> _attributeGroups;

	private final short _simLocal;

	private final short _simRemote;

	private final boolean _delta;

	private final ParameterExchangeStrategy _strategy;

	/**
	 * Konstruktor
	 * @param attributeGroups Attributgruppen
	 * @param simLocal Lokale Simulationsvariante
	 * @param simRemote Remote Simulationsvariante
	 * @param delta Nur geänderte Parameter übertragen?
	 * @param strategy Strategie
	 */
	public ParameterExchangeIdentification(
			final List<CorrespondingAttributeGroup> attributeGroups,
			final short simLocal,
			final short simRemote,
			final boolean delta,
			final ParameterExchangeStrategy strategy) {
		_attributeGroups = attributeGroups;
		_simLocal = simLocal;
		_simRemote = simRemote;
		_delta = delta;
		_strategy = strategy;
	}

	/**
	 * Gibt die Attributgruppen zurück
	 * @return die Attributgruppen
	 */
	public List<CorrespondingAttributeGroup> getAttributeGroups() {
		return Collections.unmodifiableList(_attributeGroups);
	}

	/**
	 * Gibt die lokale Simulationsvariante zurück
	 * @return die lokale Simulationsvariante
	 */
	public short getSimLocal() {
		return _simLocal;
	}

	/**
	 * Gibt die remote Simulationsvariante zurück
	 * @return die remote Simulationsvariante
	 */
	public short getSimRemote() {
		return _simRemote;
	}

	/**
	 * Gibt den Deltapararameter zurück
	 * @return true wenn nur geänderte Daten ausgetauscht werden sollen
	 */
	public boolean isDelta() {
		return _delta;
	}

	/**
	 * Gibt die Strategie zurück
	 * @return die Strategie
	 */
	public ParameterExchangeStrategy getStrategy() {
		return _strategy;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ParameterExchangeIdentification other = (ParameterExchangeIdentification)o;

		if(_delta != other._delta) return false;
		if(_simLocal != other._simLocal) return false;
		if(_simRemote != other._simRemote) return false;
		if(!_attributeGroups.equals(other._attributeGroups)) return false;
		if(_strategy != other._strategy) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _attributeGroups.hashCode();
		result = 31 * result + _simLocal;
		result = 31 * result + _simRemote;
		result = 31 * result + (_delta ? 1 : 0);
		result = 31 * result + _strategy.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ParameterExchangeIdentification{" + "_attributeGroups=" + _attributeGroups + ", _simLocal=" + _simLocal + ", _simRemote=" + _simRemote
		       + ", _delta=" + _delta + ", _strategy=" + _strategy + '}';
	}
}
