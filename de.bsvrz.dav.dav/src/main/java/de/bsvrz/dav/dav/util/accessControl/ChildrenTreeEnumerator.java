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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hilfsklasse, die von einem DataLoader-Objekt die referenzierten Unterklassen ermittelt und auf eventuelle Rekursionsprobleme hinweist
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
class ChildrenTreeEnumerator {

	private final List<DataLoader> _alreadyVisitedList = new ArrayList<DataLoader>();

	private final List<DataLoader> _alreadyFinishedList = new ArrayList<DataLoader>();

	private AccessControlManager _accessControlManager;

	private DataLoader _node;

	/**
	 * @param accessControlManager AccessControlManager, der über eventuelle Rekursionsprobleme informiert wird
	 * @param node                 Objekt das nach Kindelementen gefragt wird
	 */
	public ChildrenTreeEnumerator(final AccessControlManager accessControlManager, final DataLoader node) {
		_accessControlManager = accessControlManager;
		_node = node;
	}

	/**
	 * Gibt alle Kindelemente eines Objekts zurück
	 *
	 * @return Liste mit Kindelementen
	 */
	protected List<DataLoader> enumerateChildren() {
		enumerateChildrenInternal(_node);
		return Collections.unmodifiableList(_alreadyFinishedList);
	}

	/**
	 * Interne rekursiv aufgerufene Hilfsfunktion zum Auflisten von Kindelementen
	 *
	 * @param node Objekt das nach Kindelementen gefragt wird
	 */
	private void enumerateChildrenInternal(DataLoader node) {
		if(_alreadyFinishedList.contains(node)) return;
		if(_alreadyVisitedList.contains(node)) {
			final ArrayList<DataLoader> trace = new ArrayList<DataLoader>(_alreadyVisitedList);
			trace.removeAll(_alreadyFinishedList);
			trace.add(node);
			_accessControlManager.notifyInfiniteRecursion(node, _alreadyVisitedList.get(_alreadyVisitedList.size() - 1), trace);
		}
		else {
			_alreadyVisitedList.add(node);
			for(DataLoader dataLoader : node.getChildObjects()) {
				enumerateChildrenInternal(dataLoader);
			}
		}
		_alreadyFinishedList.add(node);
	}

	@Override
	public String toString() {
		return "ChildrenTreeEnumerator{" + "_node=" + _node + '}';
	}
}
