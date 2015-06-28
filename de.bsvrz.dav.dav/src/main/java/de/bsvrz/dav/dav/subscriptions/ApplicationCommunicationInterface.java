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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;

/**
 * Basis-Interface für eine Kommunikation vom Datenverteiler zu einer Applikation
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public interface ApplicationCommunicationInterface extends CommunicationInterface {

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um eine Sendesteuerung an die Applikation weiterzuleiten. Aus den übergebenen Parametern wird
	 * ein {@link de.bsvrz.dav.daf.communication.lowLevel.telegrams.RequestSenderDataTelegram}-Array gebildet und über die Telegrammverwaltung an die Applikation gesendet.
	 *
	 * @param data  Anmeldeinformationen
	 * @param state Benachrichtigungscode
	 *
	 * @see de.bsvrz.dav.daf.communication.lowLevel.telegrams.RequestSenderDataTelegram
	 */
	void triggerSender(BaseSubscriptionInfo data, byte state);
}
