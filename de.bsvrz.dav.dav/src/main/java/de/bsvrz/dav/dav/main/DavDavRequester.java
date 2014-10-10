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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DavRequester;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.dav.subscriptions.ReceivingSubscription;
import de.bsvrz.dav.dav.subscriptions.SendingSubscription;
import de.bsvrz.dav.dav.subscriptions.Subscription;
import de.bsvrz.dav.dav.subscriptions.SubscriptionInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementiert die Schnittstelle Applikation-Dav (siehe {@link #DavRequester}) auf Datenverteiler-Seite
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */

public class DavDavRequester extends DavRequester {

	/** Verweis auf DavTransactionManager */
	private final DavTransactionManager _davTransactionManager;

	private final HighLevelSubscriptionsManager _highLevelSubscriptionsManager;

	private final SystemObject _localDav;

	/**
	 * Erstellt einen neuen DavDavRequester
	 * @param connection Verbindung zum Datenverteiler
	 * @param davTransactionManager DavTransactionManager
	 * @param highLevelSubscriptionsManager
	 */
	public DavDavRequester(
			final ClientDavConnection connection,
			final DavTransactionManager davTransactionManager,
			final HighLevelSubscriptionsManager highLevelSubscriptionsManager) {
		super(
				connection,
				connection.getDataModel().getAspect("asp.antwort"),
				connection.getDataModel().getAspect("asp.anfrage")
		);
		_davTransactionManager = davTransactionManager;
		_highLevelSubscriptionsManager = highLevelSubscriptionsManager;

		// Folgender Code wäre falsch, da getLocalDav bei Remote-Dav-Verbindungen (also Datenverteilern ohne eigener Konfiguration)
		// Den Konfigurations-Dav ausspucken würde und nicht den hier verwendeten
//		_localDav = _connection.getLocalDav();
		
		_localDav =_highLevelSubscriptionsManager.getTelegramManager().getConnectionsManager().getDavObject();
		
		subscribeDrain(_localDav);
	}

	@Override
	protected void onReceive(final Data data) {
		long answerKind = ANSWER_OK;
		String errorString = null;
		long sender = -1;
		long requestId = 0;
		try {
			sender = data.getReferenceValue("Absender").getId();
			requestId = data.getUnscaledValue("AnfrageIndex").longValue();
			final int requestKind = (int)data.getUnscaledValue("AnfrageTyp").longValue();
			final byte[] bytes = data.getUnscaledArray("Daten").getByteArray();
			switch(requestKind) {
				// je nach Anfragetyp die entsprechende Aktion durchführen
				case SUBSCRIBE_TRANSMITTER_SOURCE:
					// Anmeldung einer Transaktionsquelle
					_davTransactionManager.handleSubscribeTransactionSource(bytes);
					break;
				case SUBSCRIBE_TRANSMITTER_DRAIN:
					// Anmeldung einer Transaktionssenke
					_davTransactionManager.handleSubscribeTransactionDrain(bytes);
					break;
				case SUBSCRIPTION_INFO:
				{
					final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
					try{
						final BaseSubscriptionInfo info = new BaseSubscriptionInfo();
						info.read(dataInputStream);
						final SubscriptionInfo subscriptionInfo = _highLevelSubscriptionsManager.getSubscriptionInfo(info);
						final byte[] result;
						if(subscriptionInfo == null) {
							result = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
						}
						else {
							result = subscriptionInfo.serializeToBytes();
						}
						sendBytes(sender, requestId, SUBSCRIPTION_INFO, result, _localDav);
						return;
					}
					finally {
						dataInputStream.close();
					}
				}
				case APP_SUBSCRIPTION_INFO:
				{
					final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
					try {
						long applicationId = dataInputStream.readLong();
						Collection<? extends Subscription> subscriptions = _highLevelSubscriptionsManager.getAllSubscriptions(applicationId);
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						DataOutputStream dataOutputStream = new DataOutputStream(out);
						final List<SendingSubscription> sendingSubscriptions = new ArrayList<SendingSubscription>();
						final List<ReceivingSubscription> receivingSubscriptions = new ArrayList<ReceivingSubscription>();
						for(final Subscription subscriptionInfo : subscriptions) {
							if(subscriptionInfo instanceof SendingSubscription) {
								sendingSubscriptions.add((SendingSubscription)subscriptionInfo);
							}
							else if(subscriptionInfo instanceof ReceivingSubscription) {
								receivingSubscriptions.add((ReceivingSubscription)subscriptionInfo);
							}
						}
						dataOutputStream.writeInt(sendingSubscriptions.size());
						for(final SendingSubscription sendingSubscription : sendingSubscriptions) {
							dataOutputStream.writeLong(sendingSubscription.getBaseSubscriptionInfo().getObjectID());
							dataOutputStream.writeLong(sendingSubscription.getBaseSubscriptionInfo().getUsageIdentification());
							dataOutputStream.writeShort(sendingSubscription.getBaseSubscriptionInfo().getSimulationVariant());
							dataOutputStream.writeBoolean(sendingSubscription.isSource());
							dataOutputStream.writeBoolean(sendingSubscription.isRequestSupported());
							dataOutputStream.writeInt(sendingSubscription.getState().ordinal());
						}
						dataOutputStream.writeInt(receivingSubscriptions.size());
						for(final ReceivingSubscription receivingSubscription : receivingSubscriptions) {
							dataOutputStream.writeLong(receivingSubscription.getBaseSubscriptionInfo().getObjectID());
							dataOutputStream.writeLong(receivingSubscription.getBaseSubscriptionInfo().getUsageIdentification());
							dataOutputStream.writeShort(receivingSubscription.getBaseSubscriptionInfo().getSimulationVariant());
							dataOutputStream.writeBoolean(receivingSubscription.isDrain());
							dataOutputStream.writeBoolean(receivingSubscription.getReceiveOptions().withDelayed());
							dataOutputStream.writeBoolean(receivingSubscription.getReceiveOptions().withDelta());
							dataOutputStream.writeInt(receivingSubscription.getState().ordinal());
						}
						sendBytes(sender, requestId, APP_SUBSCRIPTION_INFO, out.toByteArray(), _localDav);
						return;
					}
					finally {
						dataInputStream.close();
					}
				}
				default:
					answerKind = ANSWER_ERROR;
					errorString = "Ungültige Anfrage: " + requestKind;
			}
		}
		catch(Exception e){
			_debug.info("Beim Verarbeiten einer Nachricht trat ein Fehler auf", e);
			errorString = "Fehler beim Verarbeiten der Anfrage: " + e;
			answerKind = ANSWER_ERROR;
		}

		try {
			if(sender != -1){
				// Antwort senden
				if(answerKind == ANSWER_ERROR) {
					sendError(sender, requestId, errorString, _localDav);
				}
				else {
					sendBytes(sender, requestId, ANSWER_OK, new byte[0], _localDav);
				}
			}
		}
		catch(IOException e) {
			_debug.warning("Beim Versenden einer Antwort trat ein Fehler auf", e);
		}
	}
}
