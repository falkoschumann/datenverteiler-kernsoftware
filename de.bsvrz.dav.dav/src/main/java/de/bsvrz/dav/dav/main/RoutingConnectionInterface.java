/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.RoutingUpdate;

/**
 * Dieses Interface deklariert Methoden, die Informationen zu Verbindungen liefern.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public interface RoutingConnectionInterface {

	/**
	 * Diese Methode gibt die ID des Zielknotens zurück.
	 *
	 * @return ID des Zielknotens
	 */
	public long getRemoteNodeId();

	/**
	 * Diese Methode gibt die Gewichtung der Verbindung zurück.
	 *
	 * @return Gewichtung der Verbindung
	 */
	public int getThroughputResistance();

	/**
	 * Diese Methode findet die besten Wege.
	 *
	 * @param routingUpdates Aktualisierung der Verbindung
	 */
	public void sendRoutingUpdate(RoutingUpdate[] routingUpdates);
}
