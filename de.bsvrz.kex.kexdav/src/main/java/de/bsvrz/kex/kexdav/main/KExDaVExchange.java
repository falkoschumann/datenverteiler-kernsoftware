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

package de.bsvrz.kex.kexdav.main;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.kex.kexdav.correspondingObjects.CorrespondingObjectManager;
import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.dataexchange.*;
import de.bsvrz.kex.kexdav.dataplugin.AttributeGroupPair;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.objectexchange.ObjectExchangeManager;
import de.bsvrz.kex.kexdav.objectexchange.SetExchangeManager;
import de.bsvrz.kex.kexdav.parameterloader.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Diese Klasse kümmert sich darum, beim Eintreffen neuer Parameter die einzelnen Klassen zum Austausch von Daten, Parametern, Objekten usw. zu instantiieren
 * und gegebenenfalls wieder zu entsorgen. Diese Klasse gibt es einmal pro Remote-DaV-Verbindung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9270 $
 */
class KExDaVExchange {

	private RemoteDaVParameter _parameter;

	private final ClientDavInterface _localConnection;

	private final ClientDavInterface _remoteConnection;

	private final CorrespondingObjectManager _correspondingObjectManager;

	private final ManagerInterface _manager;

	private final DataExchangeManager _dataExchangeManager;

	private final ParameterExchangeManager _parameterExchangeManager;

	private final ObjectExchangeManager _objectExchangeManager;

	private final SetExchangeManager _setExchangeManager;

	/**
	 * Erstellt eine neue KExDaVExchange-Klasse
	 *
	 * @param parameter        Parameter, die die Daten und Objekte spezifizieren, die ausgetauscht werden sollen
	 * @param localConnection  Lokale Verbindung
	 * @param remoteConnection Entfernte (Remote-)Verbindung
	 * @param manager          Verwaltungsobjekt an das Warnungen und sonstiger Status gesendet wird
	 * @param plugins          Plugins zum Datenaustausch
	 */
	public KExDaVExchange(
			final RemoteDaVParameter parameter,
			final ClientDavInterface localConnection,
			final ClientDavInterface remoteConnection,
			final ManagerInterface manager,
			final Map<AttributeGroupPair, KExDaVDataPlugin> plugins) {
		_parameter = parameter;
		_localConnection = localConnection;
		_remoteConnection = remoteConnection;
		_manager = manager;

		_correspondingObjectManager = new CorrespondingObjectManager(localConnection, remoteConnection, manager, plugins);
		_dataExchangeManager = new DataExchangeManager(parameter, manager, _correspondingObjectManager);
		_parameterExchangeManager = new ParameterExchangeManager(parameter, manager, _correspondingObjectManager);
		_objectExchangeManager = new ObjectExchangeManager(parameter, manager, _correspondingObjectManager);
		_setExchangeManager = new SetExchangeManager(parameter, manager, _correspondingObjectManager);
	}

	/**
	 * Startet den Austausch
	 *
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException
	 *          falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	public void start() throws MissingAreaException {
		refreshParameters(_parameter);
	}

	/** Stoppt den Austausch */
	public void stop() {
		_correspondingObjectManager.clear();
		_dataExchangeManager.stop();
		_parameterExchangeManager.stop();
		_objectExchangeManager.stop();
		_setExchangeManager.stop();
	}

	/**
	 * Löst den Trigger für den Parameteraustausch aus
	 * @param direction Austauschrichtung
	 */
	public void triggerParameterExchange(final Direction direction) {
		for(final ParameterExchange exchange : _parameterExchangeManager.getExchangeMap().values()) {
			exchange.triggerExchange(direction);
		}
	}

	/**
	 * Wird aufgerufen, falls neue Parameter eintreffen
	 *
	 * @param newParameters Neue Parameter, die die auszutauschenden Daten und Objekte festlegen
	 *
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException
	 *          falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	public void setParameter(final RemoteDaVParameter newParameters) throws MissingAreaException {
		refreshParameters(newParameters);
		_parameter = newParameters;
	}

	/**
	 * Aktualisiert die Parameter
	 * @param parameters Neue Parameter
	 * @throws MissingAreaException Ein benötigter KB fehlt
	 */
	private void refreshParameters(final RemoteDaVParameter parameters) throws MissingAreaException {
		refreshConfigurationAreas(parameters);
		_dataExchangeManager.setParameter(parameters);
		_parameterExchangeManager.setParameter(parameters);
		_objectExchangeManager.setParameter(parameters);
		_setExchangeManager.setParameter(parameters);
	}

	/**
	 * Aktualisiert die Konfigurationsbereiche und gibt diese an den {@link CorrespondingObjectManager} weiter
	 * @param parameters Parameter
	 * @throws MissingAreaException Es sing ungültige Konfigurationsbereiche in den Parametern
	 */
	private void refreshConfigurationAreas(final RemoteDaVParameter parameters) throws MissingAreaException {

		ConfigurationArea localArea = null;
		if(parameters.getLocalArea() instanceof ConfigurationArea) {
			localArea = (ConfigurationArea)parameters.getLocalArea();
		}


		if(localArea == null) {
			_manager.addMessage(Message.newMinor("Es wurde kein Default-Konfigurationsbereich für die lokale Verbindung angegeben"));
		}

		ConfigurationArea remoteArea = null;
		if(parameters.getRemoteArea().length() == 0) {
			_manager.addMessage(Message.newMinor("Es wurde kein Default-Konfigurationsbereich für die Remote-Verbindung angegeben"));
		}
		else {
			remoteArea = _remoteConnection.getDataModel().getConfigurationArea(parameters.getRemoteArea());
			if(remoteArea == null) {
				throw new MissingAreaException("Es wurde ein ungültiger Default-Konfigurationsbereich für die Remote-Verbindung angegeben");
			}
		}

		final Map<String, ConfigurationArea> localAreas = new HashMap<String, ConfigurationArea>(parameters.getLocalAreaParameters().size());
		final Map<String, ConfigurationArea> remoteAreas = new HashMap<String, ConfigurationArea>(parameters.getRemoteAreaParameters().size());

		for(final AreaParameter parameter : parameters.getLocalAreaParameters()) {

			final ConfigurationArea configurationArea = _localConnection.getDataModel().getConfigurationArea(parameter.getConfigurationAreaPid());
			if(configurationArea != null) {
				for(final SystemObjectType type : parameter.getTypes()) {
					checkIfWritable(_localConnection, configurationArea);
					localAreas.put(type.getPid(), configurationArea);
				}
			}
			else {
				throw new MissingAreaException(
						"Es wurde ein ungültiger Konfigurationsbereich für die lokale Verbindung angegeben: " + parameter.getConfigurationAreaPid()
				);
			}
		}

		for(final AreaParameter parameter : parameters.getRemoteAreaParameters()) {

			final ConfigurationArea configurationArea = _remoteConnection.getDataModel().getConfigurationArea(parameter.getConfigurationAreaPid());
			if(configurationArea != null) {
				for(final SystemObjectType type : parameter.getTypes()) {
					checkIfWritable(_remoteConnection, configurationArea);
					remoteAreas.put(type.getPid(), configurationArea);
				}
			}
			else {
				throw new MissingAreaException(
						"Es wurde ein ungültiger Konfigurationsbereich für die Remote-Verbindung angegeben: " + parameter.getConfigurationAreaPid()
				);
			}
		}

		checkIfWritable(_localConnection, localArea);
		checkIfWritable(_remoteConnection, remoteArea);

		_correspondingObjectManager.setConfigurationAreas(localArea, remoteArea, localAreas, remoteAreas);
	}

	/**
	 * Prüft ob ein Konfigurationsbereich beschreibbar ist und wirfst sonst einen Fehler
	 * @param connection Verbindung
	 * @param configurationArea KB
	 * @throws MissingAreaException Falls der KB nicht beschreibbar ist
	 */
	private static void checkIfWritable(final ClientDavInterface connection, final ConfigurationArea configurationArea) throws MissingAreaException {
		if(configurationArea == null) return;
		if(!configurationArea.getConfigurationAuthority().equals(connection.getLocalConfigurationAuthority())) {
			throw new MissingAreaException(
					"Es wurde ein nicht beschreibbarer Konfigurationsbereich angegeben: " + configurationArea
			);
		}
	}

	@Override
	public String toString() {
		return "KExDaVExchange{" + "_parameter=" + _parameter + '}';
	}
}
