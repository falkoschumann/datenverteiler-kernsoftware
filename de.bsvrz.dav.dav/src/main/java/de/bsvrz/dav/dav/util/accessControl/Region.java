/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kapselt eine Region bei der Rechteverwaltung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public class Region extends DataLoader implements ObjectCollectionParent {
	private static final String ATG_REGION = "atg.region";

	private static final String ASPECT_PID = "asp.parameterSoll";

	/** Block "Enthaltene Objekte" */
	private ObjectSet _includedObjects = null;

	/** Block "Ausgeschlossene Objekte" */
	private ObjectSet _excludedObjects = null;

	private final RegionManager _regionManager;

	private final Collection<RegionChangeListener> _regionChangeListeners = new CopyOnWriteArrayList<RegionChangeListener>();

	/** Rekursiv referenzierte Regionen, die ignoriert werden */
	private final List<Region> _disabledInnerRegions = new ArrayList<Region>();

	private final ObjectCollectionChangeListener _objectCollectionChangeListener = new ObjectCollectionChangeListener() {
		@Override
		public void blockChanged() {
			notifyRegionChanged();
		}
	};

	/**
	 * Hilfsfunktion, die gebraucht wird um zu testen, ob eine Mengenangabe leer ist. Ein String bestehend aus Leerraum wird hier als leer gewertet.
	 *
	 * @param s String der getestet wird
	 *
	 * @return true wenn der String null oder leer ist
	 */
	static boolean isStringNullOrBlank(final String s) {
		return s == null || s.trim().length() == 0;
	}

	/**
	 * Gibt die Objekttypen zurück, die in beiden Listen sind. Ist in Liste 1 z.B. DynamischerObjektTyp und FahrstreifenTyp und in Liste 2 BenutzerTyp,
	 * FahrStreifenTyp und MessQuerschnittTyp wird BenutzerTyp und FahrStreifenTyp zurückgegeben
	 *
	 * @param typesA Liste 1
	 * @param typesB Liste 2
	 *
	 * @return Objekttypen zurück, die in beiden Listen sind
	 */
	static Collection<SystemObjectType> mergeTypes(final Iterable<? extends SystemObjectType> typesA, final Iterable<? extends SystemObjectType> typesB) {
		final Set<SystemObjectType> result = new HashSet<SystemObjectType>();
		for(final SystemObjectType typeA : typesA) {
			for(final SystemObjectType typeB : typesB) {
				if(typeA.equals(typeB)) {
					result.add(typeA);
				}
				else if(typeA.inheritsFrom(typeB)) {
					result.add(typeA);
				}
				else if(typeB.inheritsFrom(typeA)) {
					result.add(typeB);
				}
			}
		}
		return result;
	}

	/**
	 * Erstellt eine neue Region
	 *
	 * @param systemObject         Systemobjekt, das die Daten dieser Region enthält
	 * @param connection           Verbindung zum Datenverteiler
	 * @param accessControlManager Klasse, die Berechtigungsobjekte verwaltet
	 */
	public Region(final SystemObject systemObject, final ClientDavInterface connection, final RegionManager accessControlManager) {
		super(connection, ATG_REGION, ASPECT_PID, accessControlManager.getUpdateLock());
		_regionManager = accessControlManager;
		startDataListener(systemObject);
	}

	/**
	 * Fügt einen Listener hinzu, der Objekte benachrichtigt, wenn diese Region geändert wird
	 *
	 * @param object Callback-Interface das benachrichtigt wird
	 */
	protected void addRegionChangeListener(final RegionChangeListener object) {
		if(_regionChangeListeners.size() == 0) startChangeListener();
		_regionChangeListeners.add(object);
	}

	/**
	 * Prüft, ob ein angegebenes Systemobjekt in der Region enthalten ist
	 *
	 * @param object Zu prüfendes SystemObjekt
	 *
	 * @return true wenn es enthalten ist
	 */
	public boolean contains(final SystemObject object) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try{
			// Standardmäßig false zurückliefern wenn keine Werte gesetzt sind
			if(_includedObjects == null || _excludedObjects == null) return false;
			// Region enthält Objekte, wenn diese in "Enthaltene Objekte" sind, aber nicht in "Ausgeschlossene Objekte".
			return _includedObjects.contains(object) && !_excludedObjects.contains(object);
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public void deactivateInvalidChild(final DataLoader node) {
		// Wir fügen die Region in eine Art Blacklist ein um unnötige Komplexität zu vermeiden
		_writeLock.lock();
		try{
			_disabledInnerRegions.add((Region)node);
		}
		finally {
			_writeLock.unlock();
		}
	}

	/**
	 * Gibt alle Objekte in der Region zurück. Der Aufruf sollte, falls möglich, vermieden werden, da der Vorgang je nach Definition der Region sehr lange dauern
	 * kann
	 *
	 * @param types Objekttypen, die beachtet werden sollen
	 * @return Liste mit Systemobjekten in der Region
	 */
	public Collection<SystemObject> getAllObjects(final Collection<? extends SystemObjectType> types) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try{
			if(_includedObjects == null || _excludedObjects == null) return Collections.emptyList();
			final List<SystemObject> objectList = _includedObjects.getAllObjects(types);
			final List<SystemObject> result = new ArrayList<SystemObject>();
			for(final SystemObject systemObject : objectList) {
				if(!_excludedObjects.contains(systemObject)) {
					result.add(systemObject);
				}
			}
			return result;
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	protected Collection<DataLoader> getChildObjects() {
		_readLock.lock();
		try{
			final Collection<DataLoader> loaderArrayList = new HashSet<DataLoader>();
			// Die referenzierten Regionen der einzelnen Blöcke zusammenfügen
			if(_includedObjects != null) loaderArrayList.addAll(_includedObjects.getRegions());
			if(_excludedObjects != null) loaderArrayList.addAll(_excludedObjects.getRegions());

			// Deaktivierte Kindelemente ausschließen
			loaderArrayList.removeAll(_disabledInnerRegions);

			return loaderArrayList;
		}
		finally {
			_readLock.unlock();
		}
	}

	/**
	 * Entfernt einen mit {@link #addRegionChangeListener(RegionChangeListener)} hinzugefügten Listener wieder
	 *
	 * @param object Callback-Interface das benachrichtigt wird
	 */
	protected void removeRegionChangeListener(final RegionChangeListener object) {
		if(_regionChangeListeners.remove(object)) {
			if(_regionChangeListeners.size() == 0) stopChangeListener();
		}
	}

	@Override
	protected void update(final Data data) {
		stopChangeListener();
		_writeLock.lock();
		try{
			reactivateInvalidChildren();
			if(_includedObjects != null) _includedObjects.dispose();
			if(_excludedObjects != null) _excludedObjects.dispose();
			_includedObjects = null;
			_excludedObjects = null;
			if(data != null) {
				final Data.Array included = data.getArray("EnthalteneObjekte");
				final Data.Array excluded = data.getArray("AusgeschlosseneObjekte");
				_includedObjects = new ObjectSet(this, getConnection(), included, false);
				_excludedObjects = new ObjectSet(this, getConnection(), excluded, false);
			}
			_regionManager.objectChanged(this);
		}
		finally {
			_writeLock.unlock();
		}
		notifyRegionChanged();
		if(_regionChangeListeners.size() > 0) startChangeListener();
	}

	/**
	 * Stoppt interne Listener, die Änderungen an dieser Region überwachen. Z.B. können sich in AuswahlRegion und Auswahlbereich neue dynamische Objekte ergeben,
	 * oder die Elemente von Mengen könnten sich ändern.
	 */
	private void stopChangeListener() {
		_readLock.lock();
		try{
			if(_includedObjects != null) _includedObjects.removeChangeListener(_objectCollectionChangeListener);
			if(_excludedObjects != null) _excludedObjects.removeChangeListener(_objectCollectionChangeListener);
		}
		finally {
			_readLock.unlock();
		}
	}

	/**
	 * Signalisiert allen deaktivierten referenzierten Regionen, dass diese Region geändert wurde. Wird gebraucht um Rekursionen aufzulösen. Aktiviert alle mit
	 * {@link #deactivateInvalidChild(DataLoader)} deaktivierten Elemente wieder.
	 */
	void reactivateInvalidChildren() {
		_writeLock.lock();
		try {
			final Iterable<Region> tmpArray = new ArrayList<Region>(_disabledInnerRegions);
			_disabledInnerRegions.clear();
			for(final Region disabledInnerRegion : tmpArray) {
				disabledInnerRegion.reactivateInvalidChildren();
			}
		}
		finally {
			_writeLock.unlock();
		}
	}

	/** Benachrichtigt alle angemeldete Listener über Änderungen an diesem Objekt */
	private void notifyRegionChanged() {
		for(final RegionChangeListener regionChangeListener : _regionChangeListeners) {
			regionChangeListener.regionChanged(this);
		}
	}

	/**
	 * Startet interne Listener, die Änderungen an dieser Region überwachen. Z.B. können sich in AuswahlRegion und Auswahlbereich neue dynamische Objekte ergeben,
	 * oder die Elemente von Mengen könnten sich ändern.
	 */
	private void startChangeListener() {
		_readLock.lock();
		try{
			if(_includedObjects != null) _includedObjects.addChangeListener(_objectCollectionChangeListener);
			if(_excludedObjects != null) _excludedObjects.addChangeListener(_objectCollectionChangeListener);
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public boolean isDisabled(final Region region) {
		return _disabledInnerRegions.contains(region);
	}

	@Override
	public Region getRegion(final SystemObject regionObject) {
		return _regionManager.getRegion(regionObject);
	}

	protected ObjectSet getIncludedObjects() {
		return _includedObjects;
	}

	protected ObjectSet getExcludedObjects() {
		return _excludedObjects;
	}
}
