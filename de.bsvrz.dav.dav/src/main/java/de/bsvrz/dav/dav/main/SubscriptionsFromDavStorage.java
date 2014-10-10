/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscription;


/**
 * Speichert alle Anmeldungen, die über eine Kommunikationsverbindung mit einem anderen Datenverteiler empfangen wurden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class SubscriptionsFromDavStorage extends SubscriptionsFromRemoteStorage {

	public SubscriptionsFromDavStorage(ServerHighLevelCommunication _connection) {
		super(_connection);
	}

	/**
	 * Diese Methode registriert eine Senderanmeldung.
	 *
	 * @param subscription Die zu registrierende Senderanmeldung
	 */
	public final void subscribeSendData(TransmitterDataSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Der Übergabeparameter ist null");
		}
		if(subscription.getSubscriptionType() != 0) {
			throw new IllegalArgumentException("Die Anmeldung ist keine Sendeanmeldung");
		}
		BaseSubscriptionInfo baseSubscriptionInfo = subscription.getBaseSubscriptionInfo();
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		sendSubscriptionTable.put(baseSubscriptionInfo, subscription);
	}

	/**
	 * Diese Methode meldet eine Senderanmeldung ab.
	 *
	 * @param baseSubscriptionInfo die abzumeldende Senderanmeldung
	 */
	public final void unsubscribeSendData(BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		sendSubscriptionTable.remove(baseSubscriptionInfo);
	}

	/**
	 * Diese Methode registriert eine Empfangsanmeldung.
	 *
	 * @param subscription Die zu registrierende Empfangsanmeldung
	 */
	public final void subscribeReceiveData(TransmitterDataSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Der Übergabeparameter ist null");
		}
		if(subscription.getSubscriptionType() != 1) {
			throw new IllegalArgumentException("Die Anmeldung ist keine Empfangsanmeldung");
		}
		BaseSubscriptionInfo baseSubscriptionInfo = subscription.getBaseSubscriptionInfo();
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		receiveSubscriptionTable.put(baseSubscriptionInfo, subscription);
	}

	/**
	 * Meldet eine Empfangsanmeldung ab.
	 *
	 * @param baseSubscriptionInfo Die abzumeldende Empfangsanmeldung abzumeldende
	 */
	public final void unsubscribeReceiveData(BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		receiveSubscriptionTable.remove(baseSubscriptionInfo);
	}

	/**
	 * Gibt die Registrierte Empfangsanmeldung, wenn vorhanden, zurück
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return die Registrierte Empfangsanmeldung oder <code>null</code>, falls diese nicht vorhanden ist.
	 */
	final TransmitterDataSubscription getReceiveSubscription(BaseSubscriptionInfo info) {
		return (TransmitterDataSubscription)receiveSubscriptionTable.get(info);
	}

	/**
	 * Gibt die Registrierte Empfangsanmeldung, wenn vorhanden, zurück
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return die Registrierte Senderanmeldung
	 */
	final TransmitterDataSubscription getSendSubscription(BaseSubscriptionInfo info) {
		return (TransmitterDataSubscription)sendSubscriptionTable.get(info);
	}

	@Override
	final int getType() {
		return T_T;
	}
}
