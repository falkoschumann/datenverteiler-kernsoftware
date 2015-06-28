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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.SubscriptionsManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 11454 $
 */
public class LocalReceivingSubscription implements LocalSubscription, ReceivingSubscription {

	private static final Debug _debug = Debug.getLogger();

	private final SubscriptionsManager _subscriptionsManager;

	private final long _applicationId;

	private final BaseSubscriptionInfo _baseSubscriptionInfo;

	private final boolean _drain;

	private final ReceiveOptions _receiveOptions;

	private final ApplicationCommunicationInterface _applicationConnection;

	private DataState _lastSendDataState = null;

	private ReceiverState _receiverState = ReceiverState.UNKNOWN;

	public LocalReceivingSubscription(
			final SubscriptionsManager subscriptionsManager,
			final BaseSubscriptionInfo baseSubscriptionInfo,
			final boolean drain,
			final ReceiveOptions receiveOptions,
			final ApplicationCommunicationInterface applicationConnection) {
		_subscriptionsManager = subscriptionsManager;
		_applicationId = applicationConnection.getId();
		_baseSubscriptionInfo = baseSubscriptionInfo;
		_drain = drain;
		_receiveOptions = receiveOptions;
		_applicationConnection = applicationConnection;
	}


	@Override
	public long getNodeId() {
		return _applicationId;
	}

	@Override
	public ConnectionState getConnectionState() {
		return ConnectionState.FROM_LOCAL_OK;
	}

	@Override
	public long getCentralDistributorId() {
		if(!_drain) return -1;
		return _subscriptionsManager.getThisTransmitterId();
	}

	@Override
	public void unsubscribe() {
	}

	@Override
	public ApplicationCommunicationInterface getCommunication() {
		return _applicationConnection;
	}

	@Override
	public boolean isDrain() {
		return _drain;
	}

	@Override
	public ReceiveOptions getReceiveOptions() {
		return _receiveOptions;
	}

	@Override
	public void sendDataTelegram(final ApplicationDataTelegram applicationDataTelegram) {
		_applicationConnection.sendData(applicationDataTelegram, false);
		_lastSendDataState = null;
	}

	@Override
	public ReceiverState getState() {
		return _receiverState;
	}

	@Override
	public void setState(final ReceiverState receiverState, final long centralTransmitterId) {
		if(_receiverState == receiverState) return;
		_receiverState = receiverState;
	}

	/**
	 * Sendet an einen Empfänger falls nötig im Falle eines geänderten Anmeldestatus einen leeren Datensatz mit dem entsprechenden Inhalt.
	 *
	 * @param receiverState         Empfängerstatus
	 */
	public void sendStateTelegram(final ReceiverState receiverState) {
		final DataState newState;
		switch(receiverState) {
			case NO_SENDERS:
				newState = DataState.NO_SOURCE;
				break;
			case NOT_ALLOWED:
				newState = DataState.NO_RIGHTS;
				break;
			case INVALID_SUBSCRIPTION:
				newState = DataState.INVALID_SUBSCRIPTION;
				break;
			default:
				throw new IllegalArgumentException(receiverState.toString());
		}
		if(_lastSendDataState == newState) return; // Keine mehrfachen gleichen Statusnachrichten senden
		_lastSendDataState = newState;

		byte errorFlag = (byte)(newState.getCode() - 1);

		long dataIndex = _subscriptionsManager.getCurrentDataIndex(_baseSubscriptionInfo);

		_applicationConnection.sendData(
				new ApplicationDataTelegram(
						_baseSubscriptionInfo, dataIndex + 1, false, errorFlag, null, null, 1, 0, System.currentTimeMillis()
				),
		        false
		);
	}


	@Override
	public BaseSubscriptionInfo getBaseSubscriptionInfo() {
		return _baseSubscriptionInfo;
	}

	@Override
	public boolean isAllowed() {
		return _subscriptionsManager.isActionAllowed(_applicationConnection.getRemoteUserId(), getBaseSubscriptionInfo(), _drain ? UserAction.DRAIN : UserAction.RECEIVER);
	}

	@Override
	public long getUserId() {
		return _applicationConnection.getRemoteUserId();
	}

	@Override
	public String toString() {
		return "Lokale Anmeldung (" + _receiverState + ") als " + (_drain ? "Senke" : "Empfänger") +
				" auf " + _subscriptionsManager.subscriptionToString(_baseSubscriptionInfo) +
				" von " + _applicationConnection.toString() +
				" (Benutzer=" + _subscriptionsManager.objectToString(_applicationConnection.getRemoteUserId()) + ")";
	}


}
