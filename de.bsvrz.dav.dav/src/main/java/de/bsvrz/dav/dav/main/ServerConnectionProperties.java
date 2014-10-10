/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * 
 * This file is part of de.bsvrz.dav.dav.
 * 
 * de.bsvrz.dav.dav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.dav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dav.dav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.communication.lowLevel.ConnectionProperties;
import de.bsvrz.dav.daf.communication.lowLevel.LowLevelCommunicationInterface;

/**
 * Diese Klasse stellt die Parameter einer Verbindung zwischen zwei Servern zur Verfügung. Sie repräsentiert die Eigenschaften dieser Verbindung.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class ServerConnectionProperties extends ConnectionProperties {

	/** Die Authentificationskomponente des Servers */
	private AuthentificationComponent _authentificationComponent;

	/** Die ID des lokalen Datenverteilers */
	private long _dataTransmitterId;

	/** lokale Konfiguration = true */
	private boolean _localConfiguration;

	/** Die Konfigurations-Id */
	private long _configurationId;

	/** Die Konfigurations-Pid */
	private String _configurationPid;

	/** Datenverteiler Adresse für die Konfigurationsanbindung */
	private String _configDataTransmitterAdress;

	/** Datenverteiler Subadresse für die Konfigurationsanbindung */
	private int _configDataTransmitterSubAdress;


	/**
	 * Dieser Konstruktor wird für Tests benötigt.
	 */
	public ServerConnectionProperties() {
		super();
	}

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param lowLevelCommunication     Objekt der untersten Kommunikationsebene
	 * @param authentificationComponent Komponente zur Authentifizierung
	 * @param serverDavParameters       serverseitige Parameter des Datenverteilers
	 */
	public ServerConnectionProperties(
			LowLevelCommunicationInterface lowLevelCommunication, AuthentificationComponent authentificationComponent, ServerDavParameters serverDavParameters
	) {
		super(
				lowLevelCommunication,
				authentificationComponent.getAuthentificationProcess(),
				serverDavParameters.getUserName(),
				serverDavParameters.getUserPassword(),
				serverDavParameters.getSendKeepAliveTimeout(),
				serverDavParameters.getReceiveKeepAliveTimeout(),
				serverDavParameters.getDavCommunicationOutputBufferSize(),
				serverDavParameters.getDavCommunicationInputBufferSize()
		);
		if((authentificationComponent == null) || (serverDavParameters == null)) {
			throw new IllegalArgumentException("Falsche Startparameter");
		}
		_authentificationComponent = authentificationComponent;
		_dataTransmitterId = serverDavParameters.getDataTransmitterId();
		_localConfiguration = serverDavParameters.isLocalMode();
		if(_localConfiguration) {
			Object[] objs = serverDavParameters.getLocalModeParameter();
			if(objs != null) {
				_configurationPid = (String)objs[0];
				_configurationId = ((Long)objs[1]).longValue();
			}
		}
		else {
			Object[] objs = serverDavParameters.getRemoteModeParameter();
			if(objs != null) {
				_configDataTransmitterAdress = (String)objs[0];
				_configDataTransmitterSubAdress = ((Integer)objs[1]).intValue();
				_configurationPid = (String)objs[2];
			}
		}
	}

	/**
	 * Gibt die Authentifikationskomponente zurück.
	 *
	 * @return die Authentifikationskomponente
	 */
	public AuthentificationComponent getAuthentificationComponent() {
		return _authentificationComponent;
	}

	/**
	 * Gibt die Id des Datenverteilers zurück.
	 *
	 * @return die Datenverteiler-Id
	 */
	public final long getDataTransmitterId() {
		return _dataTransmitterId;
	}

	/**
	 * Setzt die Id des Datenverteilers auf den neuen Wert.
	 *
	 * @param dvId neue Datenverteiler-Id
	 */
	public final void setDataTransmitterId(long dvId) {
		_dataTransmitterId = dvId;
	}

	/**
	 * Gibt die Information zurück, ob der Datenverteiler auf eine lokale Anmeldung einer Konfigurationsapplikation warten muss.
	 *
	 * @return <code>true</code> - im lokalen Konfigurationsbetrieb. <br><code>false</code> - im 'remote' Konfigurationsbetrieb.
	 */
	public final boolean isLocalMode() {
		return _localConfiguration;
	}

	/**
	 * Gibt die Konfigurationsparameter des lokalen Modus zurück.
	 *
	 * @return die Pid und die Id der Konfigurationsapplikation
	 */
	public final Object[] getLocalModeParameter() {
		Object[] objs = null;
		if(_localConfiguration) {
			objs = new Object[2];
			objs[0] = _configurationPid;
			objs[1] = new Long(_configurationId);
		}
		return objs;
	}

	/**
	 * Setzt den Datenverteilersbetriebsmodus auf den Lokalen Modus.
	 *
	 * @param configPid die Pid der Konfigurationsapplikation
	 * @param configId  die Id der Konfigurationsapplikation
	 */
	public final void setLocalModeParameter(String configPid, long configId) {
		_localConfiguration = true;
		_configurationPid = configPid;
		_configurationId = configId;
	}

	/**
	 * Gibt die Konfigurationsparameter des Remote-Modus zurück.
	 *
	 * @return Konfigurationsparameter des Remote-Modus
	 */
	public final Object[] getRemoteModeParameter() {
		Object[] objs = null;
		if(!_localConfiguration) {
			objs = new Object[3];
			objs[0] = _configDataTransmitterAdress;
			objs[1] = new Integer(_configDataTransmitterSubAdress);
			objs[2] = _configurationPid;
		}
		return objs;
	}

	/**
	 * Setzt den Datenverteilerbetriebsmodus auf den Remote-Modus.
	 *
	 * @param configDataTransmitterAdress    die Adresse des Datenverteilers an dem die Konfiguration angemeldet ist.
	 * @param configDataTransmitterSubAdress die Subadresse des Datenverteilers an dem die Konfiguration angemeldet ist.
	 * @param configurationPid               PID der Konfiguration
	 */
	public final void setRemoteModeParameter(String configDataTransmitterAdress, int configDataTransmitterSubAdress, String configurationPid) {
		_localConfiguration = false;
		_configDataTransmitterAdress = configDataTransmitterAdress;
		_configDataTransmitterSubAdress = configDataTransmitterSubAdress;
		_configurationPid = configurationPid;
	}
}
