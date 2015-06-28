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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;

/**
 * Basis-Interface für eine Netzwerkverbindung Dav-Dav oder Dav-App
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public interface CommunicationInterface {

	/**
	 * Gibt die Id des verbundenen Benutzers zurück
	 * @return die Id des verbundenen Benutzers
	 */
	long getRemoteUserId();

	/**
	 * Versendet ein Daten-Telegramm übr diese Verbindung
	 * @param telegram Telegramm
	 * @param toCentralDistributor
	 * true: In Richtung des Zentraldatenverteilers, beim Sender-Senke-Datenfluss.
	 * false: Aus Richtung des Zentraldatenverteilers, beim Quelle-Empfänger-Datenfluss.
	 */
	void sendData(ApplicationDataTelegram telegram, boolean toCentralDistributor);

	/**
	 * Gibt die Id der Verbindung bzw. des Kommunikationspartners zurück.
	 * <ul>
	 *     <li>Bei Applikationen: Applikations-Id</li>
	 *     <li>Bei Datenverteilern: Datenverteiler-Id</li>
	 * </ul>
	 * @return Id
	 */
	long getId();
}
