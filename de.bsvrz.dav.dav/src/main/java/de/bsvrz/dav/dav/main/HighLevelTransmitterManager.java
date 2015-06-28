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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.AttributeGroupAspectCombination;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterBestWayUpdate;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscriptionReceipt;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsDeliveryUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsSubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsUpdate;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionType;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionsConstants;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.daf.util.Longs;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.subscriptions.ReceivingSubscription;
import de.bsvrz.dav.dav.subscriptions.RemoteReceiverSubscription;
import de.bsvrz.dav.dav.subscriptions.RemoteSenderSubscription;
import de.bsvrz.dav.dav.subscriptions.SendingSubscription;
import de.bsvrz.dav.dav.subscriptions.SubscriptionInfo;
import de.bsvrz.dav.dav.subscriptions.TransmitterCommunicationInterface;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.List;

/**
 *
 * Klasse, die Dav-Dav-Verbindungen verwaltet und Telegramme von T_T-Verbindungen entgegen nimmt und entsprechende Updates bei den verantwortlichen Klassen auslöst.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11475 $
 */
public class HighLevelTransmitterManager implements DistributionInterface, HighLevelTransmitterManagerInterface {
	private static final Debug _debug = Debug.getLogger();
	
	private final long _myTransmitterId;

	private final HighLevelConnectionsManagerInterface _connectionsManager;

	private final TelegramManager _telegramManager;

	private final HighLevelSubscriptionsManager _subscriptionsManager;

	private ListsManager _listsManager;

	private BestWayManager _bestWayManager;

	public HighLevelTransmitterManager(final HighLevelConnectionsManagerInterface connectionsManager, final ListsManager listsManager) {
		_connectionsManager = connectionsManager;
		_myTransmitterId = connectionsManager.getTransmitterId();
		_telegramManager = connectionsManager.getTelegramManager();
		_subscriptionsManager = _telegramManager.getSubscriptionsManager();
		_listsManager = listsManager;
		_bestWayManager = new BestWayManager(_myTransmitterId, this, _listsManager);
		_listsManager.setBestWayManager(_bestWayManager);
	}

	public long getMyTransmitterId() {
		return _myTransmitterId;
	}

	@Override
	public void connectionTerminated(final T_T_HighLevelCommunication communication) {
		_bestWayManager.handleDisconnection(communication);
		_connectionsManager.removeConnection(communication);
	}


	@Override
	public String getUserNameForAuthentication(final long connectedTransmitterId) {
		String userName = "";
		final TransmitterConnectionInfo info = _connectionsManager.getTransmitterConnectionInfo(connectedTransmitterId);
		if(info != null) {
			userName = info.getUserName();
		}
		else {
			final TransmitterConnectionInfo remoteInfo = _connectionsManager.getRemoteTransmitterConnectionInfo(connectedTransmitterId);
			if(remoteInfo != null) {
				userName = remoteInfo.getRemoteUserName();
			}
			else {
				_debug.warning("Keine Verbindungsinfo für Verbindung zum Datenverteiler " + connectedTransmitterId + " gefunden.");
			}
		}
		if("".equals(userName)) {
			userName = _connectionsManager.getUserName();
			_debug.warning(
					"Kein Benutzername für die Authentifizierung beim Datenverteiler " + connectedTransmitterId + " gefunden."
					+ " Es wird der Default-Benutzername " + userName + " benutzt."
			);
		}
		return userName;
	}


	@Override
	public String getPasswordForAuthentication(final long connectedTransmitterId) {
		String userName = "";
		final TransmitterConnectionInfo info = _connectionsManager.getTransmitterConnectionInfo(connectedTransmitterId);
		if(info != null) {
			userName = info.getUserName();
		}
		else {
			final TransmitterConnectionInfo remoteInfo = _connectionsManager.getRemoteTransmitterConnectionInfo(connectedTransmitterId);
			if(remoteInfo != null) {
				userName = remoteInfo.getRemoteUserName();
			}
		}
		if("".equals(userName)) {
			return _connectionsManager.getUserPassword();
		}
		return _connectionsManager.getStoredPassword(userName);
	}

	@Override
	public short getWeight(final long transmitterId) {
		return _connectionsManager.getWeight(transmitterId);
	}

	@Override
	public void handleDataTelegram(final T_T_HighLevelCommunication communication, final TransmitterDataTelegram transmitterDataTelegram) {
		_telegramManager.handleDataTelegram(communication, transmitterDataTelegram);
	}

	@Override
	public void handleListsUpdate(final TransmitterListsUpdate transmitterListsUpdate) {
		_listsManager.updateEntry(transmitterListsUpdate);
		long[] ids = transmitterListsUpdate.getObjectsToAdd();
		AttributeGroupAspectCombination[] combinations = transmitterListsUpdate.getAttributeGroupAspectsToAdd();
		if(ids != null || combinations != null){
			_subscriptionsManager.handleListsUpdate(ids, combinations);
		}
	}

	@Override
	public void handleListsDeliveryUnsubscription(
			final T_T_HighLevelCommunicationInterface communication, final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription) {
		_listsManager.unsubscribeDeliverer(communication.getId(), transmitterListsDeliveryUnsubscription.getTransmitterList());
	}

	@Override
	public void handleListsUnsubscription(
			final ServerHighLevelCommunication communication, final TransmitterListsUnsubscription transmitterListsUnsubscription) {
		_listsManager.unsubscribe(communication.getId(), transmitterListsUnsubscription.getTransmitterList());
	}

	@Override
	public void handleListsSubscription(
			final ServerHighLevelCommunication communication, final TransmitterListsSubscription transmitterListsSubscription) {
		_listsManager.subscribe(communication.getId(), transmitterListsSubscription.getTransmitterList());
	}

	@Override
	public void handleTransmitterSubscription(final T_T_HighLevelCommunicationInterface communication, final TransmitterDataSubscription subscription) {
		long[] ids = subscription.getTransmitters();
		if(ids == null) {
			TransmitterDataSubscriptionReceipt negativeReceipt = new TransmitterDataSubscriptionReceipt(
					subscription.getBaseSubscriptionInfo(), subscription.getSubscriptionType(), TransmitterSubscriptionsConstants.NEGATIV_RECEIP, -1, null
			);
			communication.sendTelegram(negativeReceipt);
			return;
		}
		BaseSubscriptionInfo baseSubscriptionInfo = subscription.getBaseSubscriptionInfo();
		final SubscriptionInfo subscriptionInfo = _subscriptionsManager.openSubscriptionInfo(baseSubscriptionInfo);
		TransmitterSubscriptionType subscriptionType;
		try {
			subscriptionType = TransmitterSubscriptionType.fromByte(subscription.getSubscriptionType());
		}
		catch(IllegalArgumentException e) {
			_debug.warning("Ungültige Anmeldung", e);
			subscriptionInfo.close();
			return;
		}
		if(subscriptionType == TransmitterSubscriptionType.Sender) {
			subscriptionInfo.updateOrCreateRemoteSenderSubscription(communication, Longs.asList(ids));
		}
		else {
			subscriptionInfo.updateOrCreateRemoteReceiverSubscription(communication, Longs.asList(ids));
		}
		subscriptionInfo.close();
	}

	@Override
	public void handleTransmitterUnsubscription(
			final T_T_HighLevelCommunicationInterface communication, final TransmitterDataUnsubscription unsubscription) {
		BaseSubscriptionInfo baseSubscriptionInfo = unsubscription.getBaseSubscriptionInfo();
		final SubscriptionInfo subscriptionInfo = _subscriptionsManager.openExistingSubscriptionInfo(baseSubscriptionInfo);
		if(subscriptionInfo == null) {
//			_debug.warning("Erhalte Abmeldeauftrag für Datenidentifikation, die nicht angemeldet ist: " + _subscriptionsManager.subscriptionToString(baseSubscriptionInfo));
			return;
		}
		TransmitterSubscriptionType subscriptionType = TransmitterSubscriptionType.fromByte(unsubscription.getSubscriptionType());
		if(subscriptionType == TransmitterSubscriptionType.Sender) {
			List<SendingSubscription> sendingSubscriptions = subscriptionInfo.getSendingSubscriptions(communication);
			for(SendingSubscription sendingSubscription : sendingSubscriptions) {
				if(sendingSubscription instanceof RemoteSenderSubscription) {
					RemoteSenderSubscription subscription = (RemoteSenderSubscription)sendingSubscription;
					subscriptionInfo.removeSendingSubscription(subscription);
				}
			}
		}
		else {
			List<ReceivingSubscription> receivingSubscriptions = subscriptionInfo.getReceivingSubscriptions(communication);
			for(ReceivingSubscription receivingSubscription : receivingSubscriptions) {
				if(receivingSubscription instanceof RemoteReceiverSubscription) {
					RemoteReceiverSubscription subscription = (RemoteReceiverSubscription)receivingSubscription;
					subscriptionInfo.removeReceivingSubscription(subscription);
				}
			}
		}
		subscriptionInfo.close();
	}

	@Override
	public void handleTransmitterSubscriptionReceipt(
			final T_T_HighLevelCommunicationInterface communication, final TransmitterDataSubscriptionReceipt receipt) {
		TransmitterSubscriptionType transmitterSubscriptionType;
		ConnectionState connectionState;
		try {
			transmitterSubscriptionType = TransmitterSubscriptionType.fromByte(receipt.getSubscriptionState());
			connectionState = ConnectionState.fromByte(receipt.getReceipt());
		}
		catch(IllegalArgumentException e) {
			_debug.warning("Ungültige Anmeldungsquittung", e);
			return;
		}
		_subscriptionsManager.handleTransmitterSubscriptionReceipt(
				communication, transmitterSubscriptionType, receipt.getBaseSubscriptionInfo(), connectionState, receipt.getMainTransmitterId()
		);
	}

	@Override
	public void addWay(final T_T_HighLevelCommunication communication) {
		_bestWayManager.addWay(communication);
	}

	@Override
	public void updateBestWay(final T_T_HighLevelCommunication communication, final TransmitterBestWayUpdate transmitterBestWayUpdate) {
		_bestWayManager.update(communication, transmitterBestWayUpdate);
	}

	@Override
	public void updateDestinationRoute(final long transmitterId, final RoutingConnectionInterface oldConnection, final RoutingConnectionInterface newConnection) {
		_subscriptionsManager.updateDestinationRoute(transmitterId, (TransmitterCommunicationInterface)oldConnection, (TransmitterCommunicationInterface)newConnection);
	}


	public long[] getPotentialCentralDistributors(final BaseSubscriptionInfo baseSubscriptionInfo) {
		return _listsManager.getPotentialCentralDavs(baseSubscriptionInfo);
	}

	public T_T_HighLevelCommunicationInterface getBestConnectionToRemoteDav(final long remoteDav) {
		long bestWay = _bestWayManager.getBestWay(remoteDav);
		if(bestWay == -1) return null;
		return _connectionsManager.getTransmitterConnectionFromId(bestWay);
	}
}
