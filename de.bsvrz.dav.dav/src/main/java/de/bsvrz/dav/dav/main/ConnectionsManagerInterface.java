/*
 * Copyright 2007 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;

/**
 * Schnittstelle zum Zugriff auf die Verbindungsverwaltung im Datenverteiler.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5068 $
 */
public interface ConnectionsManagerInterface {

	/**
	 * Bestimmt die Objekt-ID des lokalen Datenverteilers.
	 *
	 * @return Objekt-ID des lokalen Datenverteilers
	 */
	long getTransmitterId();

	/**
	 * Diese Methode ermittelt eine in der Verbindungsverwaltung registrierte Datenverteilerverbindung zu einem bestimmten anderen Datenverteiler.
	 *
	 * @param communicationTransmitterId Objekt-ID des Datenverteilers der über die gesuchte Verbindung erreicht werden kann.
	 *
	 * @return Verbindung zum gewünschten Datenverteiler oder <code>null</code>, wenn keine direkte Verbindung zum angegebenen Datenverteiler vorhanden ist.
	 */
	T_T_HighLevelCommunicationInterface getTransmitterConnection(long communicationTransmitterId);
}
