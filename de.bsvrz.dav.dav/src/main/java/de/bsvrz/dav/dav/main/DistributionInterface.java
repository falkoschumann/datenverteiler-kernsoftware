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

/**
 * Dieses Interface definiert die Schnittstelle, um die Route zum Ziel (DAV/DAF) zu aktualisieren.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5068 $
 */
public interface DistributionInterface {

	/**
	 * Wird aufgerufen, wenn die Kommunikation zu einem anderen Datenverteiler über eine andere Verbindung erfolgen sollte.
	 * @param transmitterId ID des betroffenen Datenverteilers.
	 * @param oldConnection Verbindung über die bisher mit dem betroffenen Datenverteiler kommuniziert wurde.
	 * @param newConnection Verbindung über die in Zukunft mit dem betroffenen Datenverteiler kommuniziert werden soll.
	 */
	public void updateDestinationRoute(long transmitterId, RoutingConnectionInterface oldConnection, RoutingConnectionInterface newConnection);
}
