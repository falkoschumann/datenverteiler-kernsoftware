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

package de.bsvrz.dav.dav.communication.davProtocol;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.DataTelegram;
import de.bsvrz.dav.dav.main.RoutingConnectionInterface;
import de.bsvrz.dav.dav.main.ServerHighLevelCommunication;
import de.bsvrz.dav.dav.subscriptions.TransmitterCommunicationInterface;


/**
 * Dieses Interface erweitert die Interfaces {@link de.bsvrz.dav.dav.main.ServerHighLevelCommunication} und {@link de.bsvrz.dav.dav.main.RoutingConnectionInterface} um die Funktionalität zum Senden von
 * Telegrammen von einem Datenverteiler zum Nächsten (DaV-DaV).
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public interface T_T_HighLevelCommunicationInterface extends ServerHighLevelCommunication, RoutingConnectionInterface, TransmitterCommunicationInterface {

	/**
	 * Diese Methode wird von der Verbindungsverwaltung, der Zuliefererverwaltung und der Anmeldungsverwaltung aufgerufen. Sie sendet über die Telegrammverwaltung
	 * ein Telegramm zu einem anderen Datenverteiler.
	 *
	 * @param telegram Grundtyp eines Telegramms
	 */
	public void sendTelegram(DataTelegram telegram);

	/**
	 * Mehrere Telegramme können en bloc versandt werden.
	 *
	 * @param telegrams Feld von zu sendenden Telegrammen
	 *
	 * @see #sendTelegram(de.bsvrz.dav.daf.communication.lowLevel.telegrams.DataTelegram)
	 */
	public void sendTelegrams(DataTelegram[] telegrams);
}
