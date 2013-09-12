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
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.DynamicObject;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.ObjectTimeSpecification;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.dav.daf.util.HashBagMap;
import de.bsvrz.kex.kexdav.dataplugin.AttributeGroupPair;
import de.bsvrz.kex.kexdav.dataplugin.BasicKExDaVDataPlugin;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.main.Constants;
import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.MissingKExDaVAttributeGroupException;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Verwaltung korrespondierender Objekte, der Konfigurationsbereiche um diese abzuspeichern, und der Plug-Ins um die Attributgruppen zu konvertieren
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9309 $
 */
public class CorrespondingObjectManager implements ObjectManagerInterface {

	private final HashBagMap<ObjectSpecification, CorrespondingObject> _objectMap = new HashBagMap<ObjectSpecification, CorrespondingObject>();

	private Map<String, ConfigurationArea> _localAreas = Collections.emptyMap();

	private Map<String, ConfigurationArea> _remoteAreas = Collections.emptyMap();

	private ConfigurationArea _localDefaultArea = null;

	private ConfigurationArea _remoteDefaultArea = null;

	private final ClientDavInterface _localConnection;

	private final ClientDavInterface _remoteConnection;

	private final ManagerInterface _manager;

	private final Map<AttributeGroupPair, KExDaVDataPlugin> _plugins;

	/**
	 * Erstellt eine Verwaltung korrespondierender Objekte. Diese sollte es einmal pro Remote-Datenverteiler geben.
	 *
	 * @param localConnection  Lokale Verbindung
	 * @param remoteConnection Remote-Verbindung
	 * @param manager          KExDaV-Verwaltung
	 * @param plugins   Plugins die pro Attributgruppenkonvertierung verwendet werden
	 */
	public CorrespondingObjectManager(
			final ClientDavInterface localConnection,
			final ClientDavInterface remoteConnection,
			final ManagerInterface manager,
			final Map<AttributeGroupPair, KExDaVDataPlugin> plugins) {
		_localConnection = localConnection;
		_remoteConnection = remoteConnection;
		_manager = manager;
		_plugins = plugins;
	}

	/**
	 * Erstellt einen Austausch dynamischer Objekte
	 *
	 * @param objectSpecification       Pid des Objektes
	 * @param direction Richtung des Austausches
	 *
	 * @return die Klasse des Korrespondierenden Objektes
	 *
	 * @throws MissingAreaException Falls kein Konfigurationsbereich gefunden werden konnte, um das Objekt auf dem Zielsystem anzulegen
	 */
	public synchronized CopyableCorrespondingObject createObjectExchange(final ObjectSpecification objectSpecification, final Direction direction) throws MissingAreaException {
		if(objectSpecification == null) throw new IllegalArgumentException("objectSpecification ist null");
		if(direction == null) throw new IllegalArgumentException("objectExchangeStrategy ist null");
		final Collection<CorrespondingObject> objects = _objectMap.get(objectSpecification);
		for(final CorrespondingObject object : objects) {
			if(object instanceof CopyableCorrespondingObject) {
				CopyableCorrespondingObject copyableCorrespondingObject = (CopyableCorrespondingObject)object;
				if(copyableCorrespondingObject.getStrategy() == direction) {
					return copyableCorrespondingObject;
				}
			}
		}
		final CopyableCorrespondingObject newObject = new CopyableCorrespondingObject(this, _manager, objectSpecification, direction);
		_objectMap.add(objectSpecification, newObject);
		newObject.start();
		return newObject;
	}

	/**
	 * Entfernt einen Objektaustausch
	 *
	 * @param objectSpecification       Pid
	 * @param direction Richtung
	 *
	 * @return True wenn erfolgreich entfernt, False falls nicht vorhanden
	 */
	public synchronized boolean removeObjectExchange(final ObjectSpecification objectSpecification, final Direction direction) {
		if(objectSpecification == null) throw new IllegalArgumentException("objectSpecification ist null");
		if(direction == null) throw new IllegalArgumentException("objectExchangeStrategy ist null");
		final Collection<CorrespondingObject> objects = _objectMap.get(objectSpecification);
		for(final CorrespondingObject object : objects) {
			if(object instanceof CopyableCorrespondingObject) {
				CopyableCorrespondingObject copyableCorrespondingObject = (CopyableCorrespondingObject)object;
				if(copyableCorrespondingObject.getStrategy() == direction) {
					copyableCorrespondingObject.stop();
					_objectMap.remove(objectSpecification, object);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gibt zu einer Pid ein korrespondierendes Objekt zurück, ohne dieses zu kopieren o.ä.
	 *
	 * @param objectSpecification Pid
	 *
	 * @return die Klasse des korrespondierenden Objektes
	 */
	public CorrespondingObject getObject(final ObjectSpecification objectSpecification) {
		if(objectSpecification == null) throw new IllegalArgumentException("objectSpecification ist null");
		final Collection<CorrespondingObject> objects = _objectMap.get(objectSpecification);
		if(objects.size() > 0) return objects.iterator().next();
		CorrespondingObject correspondingObject = new CorrespondingObject(objectSpecification, this, _manager);
		_objectMap.add(objectSpecification, correspondingObject);
		return correspondingObject;
	}

	public void copyObjectIfNecessary(final ObjectSpecification objectSpecification, final ClientDavInterface sourceConnection, final ClientDavInterface targetConnection) {

		final SystemObject targetObject = objectSpecification.getObject(targetConnection.getDataModel());
		if(targetObject != null && targetObject.isValid()) {
			// Hier nichts machen und kein Objekt austauschen, weil es schon da ist.
			return;
		}

		try {
			if(targetConnection.equals(_remoteConnection)) {
				if(!sourceConnection.equals(_localConnection)) throw new IllegalStateException();
				createObjectExchange(objectSpecification, Direction.LocalToRemote).start();
			}
			else {
				if(!sourceConnection.equals(_remoteConnection)) throw new IllegalStateException();

				createObjectExchange(objectSpecification, Direction.RemoteToLocal).start();
			}
		}
		catch(MissingAreaException e) {
			_manager.addMessage(Message.newError(e));
		}
	}

	public ClientDavInterface getLocalConnection() {
		return _localConnection;
	}

	public ClientDavInterface getRemoteConnection() {
		return _remoteConnection;
	}

	public ConfigurationArea getConfigurationAreaRemote(final String typePid) throws MissingAreaException {
		final ConfigurationArea configurationArea = _remoteAreas.get(typePid);
		if(configurationArea != null) {
			return configurationArea;
		}
		if(_remoteDefaultArea == null) {
			throw new MissingAreaException("Es wurde für das Remote-System kein Konfigurationsbereich angegeben um Objekte vom Typ " + typePid + " anzulegen.");
		}
		return _remoteDefaultArea;
	}

	public ConfigurationArea getConfigurationAreaLocal(final String typePid) throws MissingAreaException {
		final ConfigurationArea configurationArea = _localAreas.get(typePid);
		if(configurationArea != null) {
			return configurationArea;
		}
		if(_localDefaultArea == null) {
			throw new MissingAreaException("Es wurde für das lokale System kein Konfigurationsbereich angegeben um Objekte vom Typ " + typePid + " anzulegen.");
		}
		return _localDefaultArea;
	}

	public KExDaVDataPlugin getPlugIn(final String atgSource, final String atgTarget) throws MissingPluginException {
		if(atgSource.equals(atgTarget)) {
			return new BasicKExDaVDataPlugin();
		}
		final AttributeGroupPair attributeGroupPair = new AttributeGroupPair(atgSource, atgTarget);
		final KExDaVDataPlugin plugin = _plugins.get(attributeGroupPair);
		if(plugin != null) return plugin;
		throw new MissingPluginException("Kann kein Plugin für die " + attributeGroupPair.toString() + " finden.");
	}

	/**
	 * Setzt die Konfigurationsbereiche, in denen die Objekte angelegt werden sollen
	 *
	 * @param localDefaultArea  Standardbereich Lokal (oder null für keinen Standardbereich)
	 * @param remoteDefaultArea Standardbereich Remote (oder null für keinen Standardbereich)
	 * @param localAreas        Lokale zusätzliche Bereiche nach Typ
	 * @param remoteAreas       Remote zusätzliche Bereiche nach Typ
	 */
	public void setConfigurationAreas(
			final ConfigurationArea localDefaultArea,
			final ConfigurationArea remoteDefaultArea,
			final Map<String, ConfigurationArea> localAreas,
			final Map<String, ConfigurationArea> remoteAreas) {
		_localDefaultArea = localDefaultArea;
		_remoteDefaultArea = remoteDefaultArea;
		_localAreas = localAreas;
		_remoteAreas = remoteAreas;
		removeIllegalObjects(true, _remoteDefaultArea,  _remoteAreas.values());
		removeIllegalObjects(false, _localDefaultArea, _localAreas.values());
	}

	/**
	 * Löscht Objekte aus den Austauschbereichen, die dort nichts zu suchen haben.
	 * @param onRemoteSystem true wenn das Objekt auf dem Remotesystem, false wenn es auf dem Lokalsystem gelöscht werden soll
	 * @param targetArea Standardbereich
	 * @param additionalTargetAreas Zusätzliche Bereiche
	 */
	private void removeIllegalObjects(
			final boolean onRemoteSystem, final ConfigurationArea targetArea, final Collection<ConfigurationArea> additionalTargetAreas) {
		final Collection<ConfigurationArea> targetAreas = new HashSet<ConfigurationArea>(additionalTargetAreas);
		if(targetArea != null) targetAreas.add(targetArea);
		try {

			for(final ConfigurationArea area : targetAreas) {
				final DynamicObjectType dynamicObjectType = (DynamicObjectType)area.getDataModel().getType(Constants.Pids.TypeDynamicObject);
				for(final SystemObject systemObject : area.getObjects(Arrays.<SystemObjectType>asList(dynamicObjectType), ObjectTimeSpecification.valid())) {
					removeObject(onRemoteSystem, systemObject);
				}
			}
		}
		catch(MissingKExDaVAttributeGroupException e) {
			_manager.addMessage(Message.newMinor(e.getMessage()));
		}
	}

	/**
	 * Löscht ein Objekt. Objekte werden nur gelöscht, wenn sie ausgetauscht wurden, dynamische Objekte sind und eine Pid haben.
	 * @param onRemoteSystem true wenn das Objekt auf dem Remotesystem, false wenn es auf dem Lokalsystem gelöscht werden soll
	 * @param systemObject Objekt.
	 */
	private void removeObject(final boolean onRemoteSystem, final SystemObject systemObject) throws MissingKExDaVAttributeGroupException {
		if(systemObject instanceof DynamicObject) {
			final CorrespondingObject correspondingObject = getObject(ObjectSpecification.create(systemObject, _manager));
			// Falls das Objekt im Austauschbereich nur in einem System existiert (also im Austauschbereich) -> Löschen
			try {
				final KExDaVObject remoteObject = correspondingObject.getRemoteObject();
				final KExDaVObject localObject = correspondingObject.getLocalObject();
				if(onRemoteSystem && !localObject.exists()) {
					remoteObject.invalidate(false);
				}
				else if(!onRemoteSystem && !remoteObject.exists()) {
					localObject.invalidate(false);
				}
			}
			catch(ConfigurationChangeException e) {
				_manager.addMessage(Message.newMajor("Kann ein dynamisches Objekt nicht löschen", e));
			}
		}
	}

	@Override
	public String toString() {
		return "CorrespondingObjectManager{" + "_localDefaultArea=" + _localDefaultArea + ", _remoteDefaultArea=" + _remoteDefaultArea + ", _localAreas="
		       + _localAreas + ", _remoteAreas=" + _remoteAreas + '}';
	}

	/** Beendet alle von dieser Klasse verwendeten Austausche */
	public synchronized void clear() {
		for(final CorrespondingObject correspondingObject : _objectMap.valueSet()) {
			if(correspondingObject instanceof CopyableCorrespondingObject) {
				CopyableCorrespondingObject copyableCorrespondingObject = (CopyableCorrespondingObject)correspondingObject;
				copyableCorrespondingObject.stop();
			}
		}
		_objectMap.clear();
	}
}
