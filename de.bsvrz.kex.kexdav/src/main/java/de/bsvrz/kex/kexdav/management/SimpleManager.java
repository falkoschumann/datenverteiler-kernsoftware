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

package de.bsvrz.kex.kexdav.management;

import de.bsvrz.dav.daf.main.config.SystemObject;

/**
 * Einfache Implementierung des {@link ManagerInterface}s, mit der nur einfache Debug-Meldungen ausgegeben werden
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9655 $
 */
public class SimpleManager extends AbstractObservable implements ManagerInterface{

	private static final SimpleManager ourInstance = new SimpleManager();

	private SystemObject _kExDaVObject = null;

	/**
	 * Gibt die Instanz des SimpleManager zurück
	 * @return SimpleManager
	 */
	public static SimpleManager getInstance() {
		return ourInstance;
	}

	private SimpleManager() {
		super.addObserver(new DebugObserver());
	}

	public SystemObject getKExDaVObject() {
		return _kExDaVObject;
	}
	public void setKExDaVObject(final SystemObject kExDaVObject) {
		_kExDaVObject = kExDaVObject;
	}
}
