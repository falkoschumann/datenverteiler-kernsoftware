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

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstrakte Klasse, die die Listener-Funktionen der ObjectSelectionBlock-Klassen ("AuswahlBereich", "AuswahlRegion" usw.) kapselt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
abstract class AbstractObjectSelectionBlock implements ObjectSelectionBlock {

	private final CopyOnWriteArrayList<ObjectCollectionChangeListener> _objectCollectionChangeListeners = new CopyOnWriteArrayList<ObjectCollectionChangeListener>();

	/**
	 * Fügt einen neuen Listener hinzu, der über Änderungen an dieser Objektauswahl informiert wird
	 * @param object Listener Listener
	 */
	@Override
	public void addChangeListener(ObjectCollectionChangeListener object) {
		if(_objectCollectionChangeListeners.size() == 0) startChangeListener();
		_objectCollectionChangeListeners.add(object);
	}

	/**
	 * Entfernt einen Listener wieder
	 * @param object Listener Listener
	 */
	@Override
	public void removeChangeListener(ObjectCollectionChangeListener object) {
		_objectCollectionChangeListeners.remove(object);
		if(_objectCollectionChangeListeners.size() == 0) stopChangeListener();
	}

	@Override
	public void dispose() {
		_objectCollectionChangeListeners.clear();
		stopChangeListener();
	}

	/**
	 * Wird benutzt um Listener über Änderungen dieses Blocks zu informieren.
	 */
	protected void notifyBlockChanged() {
		for(final ObjectCollectionChangeListener objectCollectionChangeListener : _objectCollectionChangeListeners) {
			objectCollectionChangeListener.blockChanged();
		}
	}

	/** Startet, falls nötig, eventuelle interne Listener, die den Änderungsstatus der verwalteten Objekte überwachen */
	void startChangeListener() {
	}

	/** Stoppt die mit {@link #startChangeListener()} gestarteten Überwachungen */
	void stopChangeListener() {
	}
}
