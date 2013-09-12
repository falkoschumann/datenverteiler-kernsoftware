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

package de.bsvrz.kex.kexdav.objectexchange;

import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.MutableSet;
import de.bsvrz.dav.daf.main.config.MutableSetChangeListener;
import de.bsvrz.dav.daf.main.config.ObjectSet;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.correspondingObjects.CopyableCorrespondingObject;
import de.bsvrz.kex.kexdav.correspondingObjects.CorrespondingObjectManager;
import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Klasse zum Austausch von veränderlichen Mengen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public class DynamicSetExchange {

	private final MutableSet _source;

	private final MutableSet _target;

	private final MutableSetChangeListener _listener = new SetListener();

	private final Direction _strategy;

	private final CorrespondingObjectManager _correspondingObjectManager;

	private final ManagerInterface _manager;

	private final String _myDisplayNameTarget;

	private final String _myDisplayNameSource;

	/**
	 * Konstruktor
	 *
	 * @param localObject                Lokales Onjekt
	 * @param remoteObject               Remote-Objekt
	 * @param localSetName               Lokaler Mengenname
	 * @param remoteSetName              Remote-Mengenname
	 * @param strategy                   Austauschrichtung
	 * @param correspondingObjectManager Verwaltung korrespondierender Objekte
	 * @param manager                    KExDaV-Manager an den Benachrichtigungen usw. geschickt werden können
	 *
	 * @throws MissingObjectException Falls ein notwendiges Objekt oder eine Menge Fehlt
	 */
	public DynamicSetExchange(
			final ConfigurationObject localObject,
			final ConfigurationObject remoteObject,
			final String localSetName,
			final String remoteSetName,
			final Direction strategy,
			final CorrespondingObjectManager correspondingObjectManager,
			final ManagerInterface manager) throws MissingObjectException {
		_strategy = strategy;
		_correspondingObjectManager = correspondingObjectManager;
		_manager = manager;
		if(strategy == Direction.LocalToRemote) {
			_source = getSet(localObject, localSetName);
			_target = getSet(remoteObject, remoteSetName);

			_myDisplayNameTarget = remoteObject.getPidOrNameOrId() + ":" + remoteSetName;
			_myDisplayNameSource = localObject.getPidOrNameOrId() + ":" + localSetName;
		}
		else {
			_source = getSet(remoteObject, remoteSetName);
			_target = getSet(localObject, localSetName);

			_myDisplayNameTarget = localObject.getPidOrNameOrId() + ":" + localSetName;
			_myDisplayNameSource = remoteObject.getPidOrNameOrId() + ":" + remoteSetName;
		}
	}

	/**
	 * Ermittelt eine Menge
	 * @param object Objekt
	 * @param setName Mengenname
	 * @return Menge
	 * @throws MissingObjectException die Menge kann nicht gefunden werden oder ist nicht änderbar
	 */
	private static MutableSet getSet(final ConfigurationObject object, final String setName) throws MissingObjectException {
		final ObjectSet objectSet = object.getObjectSet(setName);
		if(objectSet == null) {
			throw new MissingObjectException("Die angegebene Menge existiert nicht: " + object + ":" + setName);
		}
		if(objectSet instanceof MutableSet) {
			return (MutableSet)objectSet;
		}
		throw new MissingObjectException("Die angegebene Menge ist nicht veränderbar: " + object + ":" + setName);
	}

	/**
	 * Startet den Austausch
	 *
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException
	 *          Falls ein Konfigurationsbereich fehlt
	 */
	public void start() throws MissingAreaException {
		_source.addChangeListener(_listener);
		final List<SystemObject> sourceElements = _source.getElements();
		final Collection<ObjectSpecification> pids = new HashSet<ObjectSpecification>(sourceElements.size());
		for(final SystemObject object : sourceElements) {
			final String pid = object.getPid();
			final ObjectSpecification objectSpecification = ObjectSpecification.create(object, _manager);
			addObject(objectSpecification);
			pids.add(objectSpecification);
		}
		for(final SystemObject object : _target.getElements()) {
			if(matchesNone(object, pids)){
				removeObject(object);
			}
		}
	}

	/**
	 * Prüft ob das Objekt in den Spezifikationen enthalten ist
	 * @param object Objekt
	 * @param objectSpecifications Spezifikationen
	 * @return false wenn das objekt zu mindestens einer ObjectSpecification passt, sonst true
	 */
	private static boolean matchesNone(final SystemObject object, final Collection<ObjectSpecification> objectSpecifications) {
		for(final ObjectSpecification objectSpecification : objectSpecifications) {
			if (objectSpecification.matches(object)) return false;
		}
		return true;
	}

	/** Beendet den Austausch */
	public void stop() {
		_source.removeChangeListener(_listener);
	}

	/**
	 * Fügt der Menge ein Objekt hinzu
	 * @param objectSpecification Objekt-Spezifizierung
	 * @throws MissingAreaException Es fehlt ein KB für das Objekt
	 */
	private void addObject(final ObjectSpecification objectSpecification) throws MissingAreaException {

		final CopyableCorrespondingObject co;

		co = _correspondingObjectManager.createObjectExchange(objectSpecification, _strategy);

		final SystemObject newObject = _strategy == Direction.LocalToRemote ? co.getRemoteObject().getWrappedObject() : co.getLocalObject().getWrappedObject();
		if(newObject == null) return;
		try {
			_target.add(newObject);
		}
		catch(ConfigurationChangeException e) {
			_manager.addMessage(Message.newMajor("Kann dynamische Menge nicht verändern: " + _myDisplayNameTarget, e));
		}
	}

	/**
	 * Löscht ein Objekt aus der Menge
	 * @param object Objekt
	 */
	private void removeObject(final SystemObject object) {
		try {
			final SystemObject systemObject;
			if(object.getDataModel() == _target.getDataModel()){
				systemObject = object;
			}
			else{
				systemObject = ObjectSpecification.create(object, _manager).getObject(_target.getDataModel());
				if(systemObject == null) return;
			}
			_target.remove(systemObject);
		}
		catch(Exception e) {
			_manager.addMessage(Message.newMajor("Kann dynamische Menge nicht verändern: " + _myDisplayNameTarget, e));
		}
	}


	private class SetListener implements MutableSetChangeListener {

		public void update(final MutableSet set, final SystemObject[] addedObjects, final SystemObject[] removedObjects) {
			for(final SystemObject object : addedObjects) {
				try {
					addObject(ObjectSpecification.create(object, _manager));
				}
				catch(MissingAreaException e) {
					_manager.addMessage(Message.newError(e));
				}
			}
			for(final SystemObject removedElement : removedObjects) {
				removeObject(removedElement);
			}
		}
	}

	@Override
	public String toString() {
		return "DynamicSetExchange{" + "_source=" + _myDisplayNameSource + ", _target=" + _myDisplayNameTarget + ", _strategy=" + _strategy + '}';
	}
}
