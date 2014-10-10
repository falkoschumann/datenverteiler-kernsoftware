/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.main.config.SystemObject;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public interface RegionManager {

	/**
	 * Gibt die Region-Klasse zurück die zu dem angeforderten Systemobjekt gehört.
	 *
	 * @param systemObject Systemobjekt, das eine Region repräsentiert
	 *
	 * @return Region-Klasse die Abfragen auf eine Region ermöglicht
	 */
	Region getRegion(SystemObject systemObject);

	/**
	 * Wird aufgerufen un dem AccessControlManager zu informieren, dass ein verwaltetes Objekt sich geändert hat. Der AccessControlManager wird daraufhin nach
	 * Benutzer-Objekten suchen, die dieses Objekt verwenden und an den {@link de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager} eine Benachrichtigung senden, dass
	 * sich die Rechte des Benutzers geändert haben und eventuelle vorhandene Anmeldungen entfernt werden müssen.
	 *
	 * @param object Objekt das sich geändert hat
	 */
	void objectChanged(DataLoader object);

	/**
	 * Um immer einen konsistenten Zustand zu haben, darf immer nur ein DataLoader gleichzeitig pro RegionManager geupdatet werden. Dazu wird auf dieses
	 * dummy-Objekt synchronisiert
	 *
	 * @return Objekt auf das Synchronisiert werden soll
	 */
	Object getUpdateLock();
}
