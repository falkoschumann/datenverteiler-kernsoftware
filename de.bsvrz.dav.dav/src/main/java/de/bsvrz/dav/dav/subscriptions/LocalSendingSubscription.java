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

package de.bsvrz.dav.dav.subscriptions;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.RequestSenderDataTelegram;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.SubscriptionsManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 11471 $
 */
public class LocalSendingSubscription implements LocalSubscription, SendingSubscription {

	private final SubscriptionsManager _subscriptionsManager;

	private final long _applicationId;

	private final BaseSubscriptionInfo _baseSubscriptionInfo;

	private final boolean _source;

	private final boolean _requestSupported;

	private final ApplicationCommunicationInterface _applicationConnection;

	private SenderState _senderState = SenderState.UNKNOWN;

	private static final Debug _debug = Debug.getLogger();

	public LocalSendingSubscription(
			final SubscriptionsManager subscriptionsManager,
			final BaseSubscriptionInfo baseSubscriptionInfo,
			final boolean source,
			final boolean requestSupported,
			final ApplicationCommunicationInterface applicationConnection) {
		_subscriptionsManager = subscriptionsManager;
		_applicationId = applicationConnection.getId();
		_baseSubscriptionInfo = baseSubscriptionInfo;
		_source = source;
		_requestSupported = requestSupported;
		_applicationConnection = applicationConnection;
	}

	@Override
	public long getNodeId() {
		return _applicationId;
	}

	@Override
	public long getCentralDistributorId() {
		if(!_source) return -1;
		return _subscriptionsManager.getThisTransmitterId();
	}

	@Override
	public void unsubscribe() {
	}

	@Override
	public ConnectionState getConnectionState() {
		return ConnectionState.FROM_LOCAL_OK;
	}

	@Override
	public boolean isSource() {
		return _source;
	}

	@Override
	public boolean isRequestSupported() {
		return _requestSupported;
	}

	@Override
	public SenderState getState() {
		return _senderState;
	}

	@Override
	public void setState(final SenderState senderState, final long centralTransmitterId) {
		boolean wasInvalid = !_senderState.isValidSender();
		if(_senderState == senderState) return;
		// Sendesteuerungstelegramme werden im Normalfall nur verschickt, wenn der Empfänger diese auch haben will.
		// Im Fehlerfall (keine Rechte, ungültige Anmeldung) werden diese aber immer verschickt (siehe TestClientDavConnectionSendControl)
		// auch, wenn nach einem Fehler wieder Senden erlaubt/erwünscht ist.
		switch(senderState) {
			case RECEIVERS_AVAILABLE:
				if(wasInvalid || _requestSupported) _applicationConnection.triggerSender(_baseSubscriptionInfo,  RequestSenderDataTelegram.START_SENDING);
				break;
			case NO_RECEIVERS:
				if(wasInvalid || _requestSupported) _applicationConnection.triggerSender(_baseSubscriptionInfo, RequestSenderDataTelegram.STOP_SENDING);
				break;
			case NOT_ALLOWED:
				_applicationConnection.triggerSender(_baseSubscriptionInfo, RequestSenderDataTelegram.STOP_SENDING_NO_RIGHTS);
				break;
			case INVALID_SUBSCRIPTION:
			case MULTIPLE_REMOTE_LOCK:
				_applicationConnection.triggerSender(_baseSubscriptionInfo, RequestSenderDataTelegram.STOP_SENDING_NOT_A_VALID_SUBSCRIPTION);
				break;
		}
		_senderState = senderState;
	}

	@Override
	public BaseSubscriptionInfo getBaseSubscriptionInfo() {
		return _baseSubscriptionInfo;
	}

	@Override
	public boolean isAllowed() {
		return _subscriptionsManager.isActionAllowed(
				_applicationConnection.getRemoteUserId(), getBaseSubscriptionInfo(), _source ? UserAction.SOURCE : UserAction.SENDER
		);
	}

	@Override
	public String toString() {
		return "Lokale Anmeldung (" + _senderState + ") als " + (_source ? "Quelle" : "Sender") +
				" auf " + _subscriptionsManager.subscriptionToString(_baseSubscriptionInfo) +
				" von " + _applicationConnection.toString() +
		       " (Benutzer=" + _subscriptionsManager.objectToString(_applicationConnection.getRemoteUserId()) + ")";
	}

	@Override
	public ApplicationCommunicationInterface getCommunication() {
		return _applicationConnection;
	}

	@Override
	public long getUserId() {
		return _applicationConnection.getRemoteUserId();
	}
}
