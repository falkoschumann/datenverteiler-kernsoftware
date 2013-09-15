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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.dataexchange.DataCopyException;
import de.bsvrz.kex.kexdav.dataplugin.BasicKExDaVDataPlugin;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.main.KExDaVException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.systemobjects.ExistenceListener;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.MissingKExDaVAttributeGroupException;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;
import de.bsvrz.kex.kexdav.systemobjects.PidAttributeGroupUsage;

import java.util.HashMap;
import java.util.Map;

/**
 * Diese Klasse kapselt zwei korrespondierenden Objekten auf unterschiedlichen Datenverteilern und unterstützt das automatische Kopieren, Löschen, usw. des
 * Objektes in eine vorgegebene Richtung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9238 $
 */
public class CopyableCorrespondingObject extends CorrespondingObject {

	private ExistenceListener _sourceListener = null;

	private final Direction _strategy;

	private final ObjectManagerInterface _objectManagerInterface;

	private final ManagerInterface _manager;

	private boolean _started = false;

	private final KExDaVDataPlugin _configurationDataPlugin = new BasicKExDaVDataPlugin();

	/**
	 * Konstruktor
	 * @param objectManagerInterface Objekt-Verwaltung
	 * @param manager KExDaV-Verwaltung
	 * @param objectSpecification Spezifikation zur Bestimmung des Objekts
	 * @param strategy Austauschrichtung
	 */
	CopyableCorrespondingObject(
			final ObjectManagerInterface objectManagerInterface, final ManagerInterface manager, final ObjectSpecification objectSpecification, final Direction strategy) {
		super(objectSpecification, objectManagerInterface, manager);
		_objectManagerInterface = objectManagerInterface;
		_manager = manager;
		_strategy = strategy;
	}

	/**
	 * Startet den Austausch des Objekts
	 * @throws MissingAreaException Ein benötigter KB fehlt
	 */
	void start() throws MissingAreaException {
		if(_started) return;
		final KExDaVObject localObject = getLocalObject();
		final KExDaVObject remoteObject = getRemoteObject();
		switch(_strategy) {
			case LocalToRemote:
				createCopyObjectListener(localObject, remoteObject);
				if(localObject.exists() && !remoteObject.exists()) {
					createObject(localObject, remoteObject);
				}
				else if(localObject.exists() && remoteObject.exists()) {
					copyConfigurationData(localObject, remoteObject);
				}
				break;
			case RemoteToLocal:
				createCopyObjectListener(remoteObject, localObject);
				if(remoteObject.exists() && !localObject.exists()) {
					createObject(remoteObject, localObject);
				}
				else if(remoteObject.exists() && localObject.exists()) {
					copyConfigurationData(remoteObject, localObject);
				}
				break;
		}
		_started = true;
	}

	/**
	 * Kopiert Konfigurationsdaten
	 * @param source Quell-Objekt
	 * @param target Objekt, den die Konfigurationsdaten hinzugefügt werden sollen
	 */
	private void copyConfigurationData(final KExDaVObject source, final KExDaVObject target) {
		try {
			final SystemObject wrappedObject = target.getWrappedObject();
			if(wrappedObject != null && getTargetConfigurationArea(target.getType()).equals(wrappedObject.getConfigurationArea())) {
				target.setConfigurationData(getConfigurationData(source, target.getConnection()));
			}
		}
		catch(KExDaVException e) {
			_manager.addMessage(Message.newError(e));
		}
		catch(ConfigurationChangeException e) {
			_manager.addMessage(Message.newMajor("Fehler beim Setzen von Konfigurationsdaten", e));
		}
	}

	/**
	 * Beendet den Austausch des Objekts
	 */
	void stop() {
		if(!_started) return;
		switch(_strategy) {
			case LocalToRemote:
				getLocalObject().removeExistenceListener(_sourceListener);
				break;
			case RemoteToLocal:
				getRemoteObject().removeExistenceListener(_sourceListener);
				break;
		}
		_started = false;
	}

	/**
	 * Erstellt den Listener, der überwacht ob ein Objekt erstellt oder gelöscht wird und daraufhin das Objekt ebenfalls kopiert oder löscht
	 * @param source Objekt, das auf Änderungen überwacht wird
	 * @param target Objekt das ggf. erstellt oder gelöscht wird
	 */
	private void createCopyObjectListener(final KExDaVObject source, final KExDaVObject target) {
		if(_sourceListener == null) {
			_sourceListener = new ExistenceListener() {
				public void objectInvalidated(final KExDaVObject object) {
					try {
						invalidateObject(target);
					}
					catch(ConfigurationChangeException e) {
						_manager.addMessage(Message.newMajor("Kann ein dynamisches Objekt nicht löschen", e));
					}
				}

				public void objectCreated(final KExDaVObject object) {
					try {
						createObject(source, target);
					}
					catch(MissingAreaException e) {
						_manager.addMessage(
								Message.newMajor("Kann ein dynamisches Objekt nicht erstellen", e)
						);
					}
				}
			};
		}

		source.addExistenceListener(_sourceListener);
	}

	/**
	 * Löscht ein Objekt
	 * @param target Objekt
	 * @throws ConfigurationChangeException Konfiguration weigert sich den Auftrag auszuführen
	 */
	private void invalidateObject(final KExDaVObject target) throws ConfigurationChangeException {
		// das Ziel-Objekt sollte nur gelöscht werden, wenn es sich auch im Austauschbereich befindet. Sonst würden hier womöglich dynamische Objekte gelöscht,
		// die gar nicht ausgetauscht werden sollten und noch gebraucht werden
		try {
			final SystemObject wrappedObject = target.getWrappedObject();
			if(wrappedObject == null) return; // Nichts machen, das Objekt wurde anscheinend schon gelöscht
			if(getTargetConfigurationArea(target.getType()).equals(wrappedObject.getConfigurationArea())) target.invalidate(false);
		}
		catch(MissingAreaException ignored) {
			// Nichts machen, es gibt keinen Austauschbereich, folglich gibt es auch keinen Bereich, aus dem das Objekt gelöscht werden müsste
		}
		catch(MissingObjectException ignored) {
			// Nichts machen, das Objekt wurde anscheinend schon gelöscht
		}
		catch(MissingKExDaVAttributeGroupException e) {
			// Nichts machen, es können keine Objekte übertragen werden, also ist unwahrscheinlich das dieses Objekt hätte gelöscht werden sollen
		}
	}

	private void createObject(final KExDaVObject source, final KExDaVObject target) throws MissingAreaException {
		if(source.isConfigurationObject()) {
			return;
		}
		try {
			final Map<PidAttributeGroupUsage, Data> convertedData = getConfigurationData(source, target.getConnection());
			final ConfigurationArea configurationArea = getTargetConfigurationArea(source.getType());
			target.create(configurationArea, source.getPid(), source.getType(), source.getName(), convertedData, source.getId(), source.getConfigurationAuthority());
		}
		catch(MissingObjectException e) {
			_manager.addMessage(Message.newMajor("Kann ein dynamisches Objekt nicht erstellen: " + source.getObjectSpecification().toString(), e));
		}
		catch(ConfigurationChangeException e) {
			_manager.addMessage(Message.newMajor("Kann ein dynamisches Objekt nicht erstellen: " + source.getObjectSpecification().toString(), e));
		}
		catch(MissingKExDaVAttributeGroupException e) {
			_manager.addMessage(Message.newMajor("Kann ein dynamisches Objekt nicht erstellen: " + source.getObjectSpecification().toString(), e));
		}
	}

	private Map<PidAttributeGroupUsage, Data> getConfigurationData(final KExDaVObject source, final ClientDavInterface targetConnection)
			throws MissingObjectException {
		final Map<PidAttributeGroupUsage, KExDaVAttributeGroupData> allConfigurationData = source.getAllConfigurationData();
		final Map<PidAttributeGroupUsage, Data> convertedData = new HashMap<PidAttributeGroupUsage, Data>(allConfigurationData.size());
		for(final Map.Entry<PidAttributeGroupUsage, KExDaVAttributeGroupData> entry : allConfigurationData.entrySet()) {
			final KExDaVAttributeGroupData newData = new KExDaVAttributeGroupData(
					targetConnection, entry.getKey().getAttributeGroupPid(), _manager
			);
			try {
				_configurationDataPlugin.process(entry.getValue(), newData, _objectManagerInterface, _manager);
			}
			catch(DataCopyException e) {
				_manager.addMessage(Message.newMajor("Kann Konfigurationsdaten nicht kopieren", e));
			}
			convertedData.put(entry.getKey(), newData);
		}
		return convertedData;
	}

	private ConfigurationArea getTargetConfigurationArea(final String type) throws MissingAreaException {
		ConfigurationArea configurationArea = null;
		if(_strategy == Direction.LocalToRemote) {
			configurationArea = _objectManagerInterface.getConfigurationAreaRemote(type);
		}
		else if(_strategy == Direction.RemoteToLocal) {
			configurationArea = _objectManagerInterface.getConfigurationAreaLocal(type);
		}
		return configurationArea;
	}

	/**
	 * Gibt die Richtung des Objektaustausches zurück
	 *
	 * @return Richtung
	 */
	public Direction getStrategy() {
		return _strategy;
	}
}
