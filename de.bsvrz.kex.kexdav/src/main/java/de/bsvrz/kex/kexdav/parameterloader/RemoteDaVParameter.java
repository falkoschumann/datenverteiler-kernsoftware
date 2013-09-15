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

import java.util.Collection;
import java.util.Collections;

/**
 * Spezifikationen für eine Datenverteiler-Verbindung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9100 $
 */
public class RemoteDaVParameter {

	private final long _reconnectionDelay;

	private final SystemObject _localArea;

	private final Collection<AreaParameter> _localAreaParameters;

	private final String _remoteArea;

	private final Collection<AreaParameter> _remoteAreaParameters;

	private final Collection<ExchangeDataParameter> _exchangeDataParameters;

	private final Collection<ExchangeParameterParameter> _exchangeParameterParameters;

	private final Collection<? extends ObjectCollectionFactory> _exchangeObjectsLocalRemote;

	private final Collection<? extends ObjectCollectionFactory> _exchangeObjectsRemoteLocal;

	private final Collection<ExchangeSetParameter> _exchangeSetParameters;

	private final ConnectionParameter _connectionParameter;

	/**
	 * Konstruktor
	 * @param connectionParameter Verbindungsparameter
	 * @param reconnectionDelay Dauer, die vor einem erneuten Verbindungsversuch gewartet wird
	 * @param localArea Lokaler Default-Bereich
	 * @param localAreaParameters Lokale Bereiche
	 * @param remoteArea Remote Default-Bereich
	 * @param remoteAreaParameters Remote-Bereiche
	 * @param exchangeDataParameters Datenaustauschparameter
	 * @param exchangeParameterParameters Parameteraustauschparameter
	 * @param exchangeObjectsLocalRemote Objektaustauschaprameter
	 * @param exchangeObjectsRemoteLocal Objektaustauschparameter
	 * @param exchangeSetParameters Mengenaustauschparameter
	 */
	public RemoteDaVParameter(
			final ConnectionParameter connectionParameter, final long reconnectionDelay, final SystemObject localArea,
			final Collection<AreaParameter> localAreaParameters,
			final String remoteArea,
			final Collection<AreaParameter> remoteAreaParameters,
			final Collection<ExchangeDataParameter> exchangeDataParameters,
			final Collection<ExchangeParameterParameter> exchangeParameterParameters,
			final Collection<? extends ObjectCollectionFactory> exchangeObjectsLocalRemote,
			final Collection<? extends ObjectCollectionFactory> exchangeObjectsRemoteLocal,
			final Collection<ExchangeSetParameter> exchangeSetParameters) {
		_connectionParameter = connectionParameter;
		_reconnectionDelay = reconnectionDelay;
		_localArea = localArea;
		_localAreaParameters = localAreaParameters;
		_remoteArea = remoteArea;
		_remoteAreaParameters = remoteAreaParameters;
		_exchangeDataParameters = exchangeDataParameters;
		_exchangeParameterParameters = exchangeParameterParameters;
		_exchangeObjectsLocalRemote = exchangeObjectsLocalRemote;
		_exchangeObjectsRemoteLocal = exchangeObjectsRemoteLocal;
		_exchangeSetParameters = exchangeSetParameters;
	}

	/**
	 * Gibt den lokalen Standardbereich zurück
	 * @return den lokalen Standardbereich
	 */
	public SystemObject getLocalArea() {
		return _localArea;
	}

	/**
	 * Gibt die lokalen zusätzlichen Bereiche zurück
	 * @return die lokalen zusätzlichen Bereiche
	 */
	public Collection<AreaParameter> getLocalAreaParameters() {
		return Collections.unmodifiableCollection(_localAreaParameters);
	}

	/**
	 * Gibt den Remote-Standardbereich zurück
	 * @return den Remote-Standardbereich
	 */
	public String getRemoteArea() {
		return _remoteArea;
	}

	/**
	 * Gibt die remote zusätzlichen Bereiche zurück
	 * @return die remote zusätzlichen Bereiche
	 */
	public Collection<AreaParameter> getRemoteAreaParameters() {
		return Collections.unmodifiableCollection(_remoteAreaParameters);
	}

	/**
	 * Gibt die Datenaustauschparameter zurück
	 * @return die Datenaustauschparameter
	 */
	public Collection<ExchangeDataParameter> getExchangeDataParameters() {
		return Collections.unmodifiableCollection(_exchangeDataParameters);
	}

	/**
	 * Gibt die Parameteraustauschparameter zurück
	 * @return die Parameteraustauschparameter
	 */
	public Collection<ExchangeParameterParameter> getExchangeParameterParameters() {
		return Collections.unmodifiableCollection(_exchangeParameterParameters);
	}

	/**
	 * Gibt die Objektaustauschparameter von Lokal nach Remote zurück
	 * @return die Objektaustauschparameter von Lokal nach Remote
	 */
	public Collection<ObjectCollectionFactory> getExchangeObjectsLocalRemote() {
		return Collections.unmodifiableCollection(_exchangeObjectsLocalRemote);
	}

	/**
	 * Gibt die Objektaustauschparameter von Remote nach Lokal zurück
	 * @return die Objektaustauschparameter von Remote nach Lokal
	 */
	public Collection<ObjectCollectionFactory> getExchangeObjectsRemoteLocal() {
		return Collections.unmodifiableCollection(_exchangeObjectsRemoteLocal);
	}

	/**
	 * Gibt die Mengenaustauschparameter zurück
	 * @return die Mengenaustauschparameter
	 */
	public Collection<ExchangeSetParameter> getExchangeSetParameters() {
		return Collections.unmodifiableCollection(_exchangeSetParameters);
	}

	/**
	 * Gibt die Verbindungsparameter zurück
	 * @return die Verbindungsparameter
	 */
	public ConnectionParameter getConnectionParameters() {
		return _connectionParameter;
	}

	/**
	 * Gibt die Dauer zwischen Verbindungsversuchen zurück
	 * @return die Dauer zwischen Verbindungsversuchen
	 */
	public long getReconnectionDelay() {
		return _reconnectionDelay;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final RemoteDaVParameter other = (RemoteDaVParameter)o;

		if(_reconnectionDelay != other._reconnectionDelay) return false;
		if(!_connectionParameter.equals(other._connectionParameter)) return false;
		if(!_exchangeDataParameters.equals(other._exchangeDataParameters)) return false;
		if(!_exchangeObjectsLocalRemote.equals(other._exchangeObjectsLocalRemote)) return false;
		if(!_exchangeObjectsRemoteLocal.equals(other._exchangeObjectsRemoteLocal)) return false;
		if(!_exchangeParameterParameters.equals(other._exchangeParameterParameters)) return false;
		if(!_exchangeSetParameters.equals(other._exchangeSetParameters)) return false;
		if(_localArea != null ? !_localArea.equals(other._localArea) : other._localArea != null) return false;
		if(!_localAreaParameters.equals(other._localAreaParameters)) return false;
		if(!_remoteArea.equals(other._remoteArea)) return false;
		if(!_remoteAreaParameters.equals(other._remoteAreaParameters)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int)(_reconnectionDelay ^ (_reconnectionDelay >>> 32));
		result = 31 * result + (_localArea != null ? _localArea.hashCode() : 0);
		result = 31 * result + _localAreaParameters.hashCode();
		result = 31 * result + _remoteArea.hashCode();
		result = 31 * result + _remoteAreaParameters.hashCode();
		result = 31 * result + _exchangeDataParameters.hashCode();
		result = 31 * result + _exchangeParameterParameters.hashCode();
		result = 31 * result + _exchangeObjectsLocalRemote.hashCode();
		result = 31 * result + _exchangeObjectsRemoteLocal.hashCode();
		result = 31 * result + _exchangeSetParameters.hashCode();
		result = 31 * result + _connectionParameter.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "RemoteDaVParameter{" + "_reconnectionDelay=" + _reconnectionDelay + ", _localArea=" + _localArea + ", _localAreaParameters="
		       + _localAreaParameters + ", _remoteArea='" + _remoteArea + '\'' + ", _remoteAreaParameters=" + _remoteAreaParameters
		       + ", _exchangeDataParameters=" + _exchangeDataParameters + ", _exchangeParameterParameters=" + _exchangeParameterParameters
		       + ", _exchangeObjectsLocalRemote=" + _exchangeObjectsLocalRemote + ", _exchangeObjectsRemoteLocal=" + _exchangeObjectsRemoteLocal
		       + ", _exchangeSetParameters=" + _exchangeSetParameters + ", _connectionParameter=" + _connectionParameter + '}';
	}
}
