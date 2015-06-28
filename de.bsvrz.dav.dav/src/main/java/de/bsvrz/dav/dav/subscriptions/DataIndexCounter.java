/*
 * Copyright 2013 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.subscriptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Klasse, die zu Objekten einen Datenindex hochzählt
 *
 * Funktioniert ähnlich wie eine Map&lt;E, Long&gt;, ist aber einfacher zu benutzen.
 *
 * Die verwendete Map ist threadsicher, der Index zu einer bestimmten Datenidentifikation darf aber nur von einem
 * Thread gleichzeitig hochgezählt werden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12132 $
 */
public class DataIndexCounter<E> {

	// Primitivimplentierung, optimierbar falls nötig
	private final Map<E, Long> _innerMap = new ConcurrentHashMap<E, Long>();

	public DataIndexCounter() {
	}

	public long get(E obj){
		Long val = _innerMap.get(obj);
		if(val == null){
			return -1;
		}
		return val;
	}

	public long increment(E obj){
		// AbstactSubscriptionManager synchronisiert auf die Anmeldung, daher ist der folgende Code sicher:
		long inc = get(obj) + 1;
		_innerMap.put(obj, inc);
		return inc;
	}

	@Override
	public String toString() {
		return _innerMap.toString();
	}
}
