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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionTelegram;

import java.util.*;


/**
 * Speichert alle Anmeldungen, die über eine Kommunikationsverbindung mit einer Applikation empfangen wurden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class SubscriptionsFromApplicationStorage extends SubscriptionsFromRemoteStorage {

	public SubscriptionsFromApplicationStorage(ServerHighLevelCommunication connection) {
		super(connection);
	}

	/**
	 * Registriert eine Senderanmeldung.
	 *
	 * @param sendSubscriptionTelegram der anzumeldende Sender
	 */
	public final void subscribeSendData(SendSubscriptionTelegram sendSubscriptionTelegram) {
		if(sendSubscriptionTelegram == null) {
			throw new IllegalArgumentException("Der Übergabeparameter ist null");
		}
		SendSubscriptionInfo sendSubscriptionInfo = sendSubscriptionTelegram.getSendSubscriptionInfo();
		if(sendSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Sendeanmeldeinformation ist null");
		}
		BaseSubscriptionInfo baseSubscriptionInfo = sendSubscriptionInfo.getBaseSubscriptionInfo();
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		sendSubscriptionTable.put(baseSubscriptionInfo, sendSubscriptionInfo);
	}


	/**
	 * Meldet eine Senderanmeldung ab.
	 *
	 * @param baseSubscriptionInfo die abzumeldende Senderanmeldung
	 *
	 * @return die abgemeldete Senderanmeldung
	 */
	public final SendSubscriptionInfo unsubscribeSendData(BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		return (SendSubscriptionInfo)sendSubscriptionTable.remove(baseSubscriptionInfo);
	}

	/**
	 * Registriert eine Empfangsanmeldung.
	 *
	 * @param receiveSubscriptionTelegram die anzumeldende Emfangsanmeldung
	 */
	public final void subscribeReceiveData(ReceiveSubscriptionTelegram receiveSubscriptionTelegram) {
		if(receiveSubscriptionTelegram == null) {
			throw new IllegalArgumentException("Der Übergabeparameter ist null");
		}
		ReceiveSubscriptionInfo receiveSubscriptionInfo = receiveSubscriptionTelegram.getReceiveSubscriptionInfo();
		if(receiveSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Empfangsanmeldeinformation ist null");
		}
		BaseSubscriptionInfo baseSubscriptionInfo = receiveSubscriptionInfo.getBaseSubscriptionInfo();
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		receiveSubscriptionTable.put(baseSubscriptionInfo, receiveSubscriptionInfo);
	}

	/**
	 * Meldet eine Empfangsanmeldung ab.
	 *
	 * @param baseSubscriptionInfo die abbzumeldende Empfangsanmeldung
	 *
	 * @return die abgemeldete Empfangsanmeldung
	 */
	public final ReceiveSubscriptionInfo unsubscribeReceiveData(BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) {
			throw new IllegalArgumentException("Die Basisanmeldeinformation ist null");
		}
		return (ReceiveSubscriptionInfo)receiveSubscriptionTable.remove(baseSubscriptionInfo);
	}

	/**
	 * Überprüft, ob eine Applikation sich als Quelle für die Daten angemeldet hat.
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return true: Applikation ist Quelle für spezifiziertes Datum, false: Applikation ist keine Quelle für spezifiziertes Datum.
	 */
	final boolean canSend(BaseSubscriptionInfo info) {
		SendSubscriptionInfo sendSubscriptionInfo = (SendSubscriptionInfo)sendSubscriptionTable.get(info);
		if(sendSubscriptionInfo != null) {
			if(sendSubscriptionInfo.isSource()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Überprüft ob eine Applikation sich als Senke für spezifiziertes Datum angemeldet hat.
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return true: Applikation ist Senke für spezifiziertes Datum, false: Applikation ist keine Senke für spezifiziertes Datum.
	 */
	final boolean canReceive(BaseSubscriptionInfo info) {
		ReceiveSubscriptionInfo receiveSubscriptionInfo = (ReceiveSubscriptionInfo)receiveSubscriptionTable.get(info);
		if(receiveSubscriptionInfo != null) {
			if(receiveSubscriptionInfo.isDrain()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gibt die registrierte Empfangsanmeldung, wenn vorhanden, zurück.
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return Empfangsanmeldung
	 */
	final ReceiveSubscriptionInfo getReceiveSubscription(BaseSubscriptionInfo info) {
		return (ReceiveSubscriptionInfo)receiveSubscriptionTable.get(info);
	}

	/**
	 * Gibt die registrierte Sendeanmeldung, wenn vorhanden, zurück.
	 *
	 * @param info Basisanmeldeinformation
	 *
	 * @return Sendeanmeldung
	 */

	final SendSubscriptionInfo getSendSubscription(BaseSubscriptionInfo info) {
		return (SendSubscriptionInfo)sendSubscriptionTable.get(info);
	}


	@Override
	final int getType() {
		return T_A;
	}

	/**
	 * Gibt alle Anmeldungen zurück, die Daten verschicken. Die Rolle (Sender, Quelle) kann mit {@link #canSend(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)} in
	 * Erfahrung gebracht werden.
	 *
	 * @return Alle Anmeldungen, die Daten verschicken.
	 */
	public List<SendSubscriptionInfo> getSendingSubscriptions() {
		final List<SendSubscriptionInfo> result = new ArrayList<SendSubscriptionInfo>();
		synchronized(sendSubscriptionTable) {
			// Die Daten der Map müssen erst gecastet werden.

			final Collection allSendSubscription = sendSubscriptionTable.values();

			for(Object oneSendingSubscription : allSendSubscription) {
				if(oneSendingSubscription instanceof SendSubscriptionInfo) {
					result.add((SendSubscriptionInfo)oneSendingSubscription);
				}
			}
		}
		return result;
	}

	/**
	 * Gibt alle Anmeldungen zurück, die Daten empfangen. Die Rolle (Empfänger, Senke) kann mit {@link #canReceive(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)} in
	 * Erfahrung gebracht werden.
	 *
	 * @return Alle Anmeldungen, die Daten empfangen.
	 */
	public List<ReceiveSubscriptionInfo> getReceivingSubscription(){
		final List<ReceiveSubscriptionInfo> result = new ArrayList<ReceiveSubscriptionInfo>();

		synchronized(receiveSubscriptionTable){
			final Collection allReceivingSubscriptions = receiveSubscriptionTable.values();

			for(Object oneReceivingSubscription : allReceivingSubscriptions) {
				if(oneReceivingSubscription instanceof ReceiveSubscriptionInfo) {
					result.add((ReceiveSubscriptionInfo)oneReceivingSubscription);
				}
			}
		}

		return result;
	}
}
