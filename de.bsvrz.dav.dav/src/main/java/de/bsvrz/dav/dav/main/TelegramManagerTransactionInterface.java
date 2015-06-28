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

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;

/**
 * Schnittstelle für die Transaktionen, die der Subscriptionsmanager implementieren sollte
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11413 $
 */
public interface TelegramManagerTransactionInterface {

	/**
	 * Sendet ein Telegram. Wird vom DavTransactionManager benutzt, um manuell Telegramme mit einem vorgegebenen Datenindex zu senden.
	 * @param isSource true wenn Quelle
	 * @param dataTelegrams Telegramm (oder mehrere falls gesplittet)
	 */
	void sendTelegramsFromTransaction(
			boolean isSource, ApplicationDataTelegram[] dataTelegrams);

	/**
	 * Gibt den nächsten Datenindex für eine gegebene Anmeldung zurück
	 *
	 * @param info Anmeldungs-Info
	 *
	 * @return Ein Datenindex
	 *
	 */
	long getNextDataIndex(BaseSubscriptionInfo info);
}
