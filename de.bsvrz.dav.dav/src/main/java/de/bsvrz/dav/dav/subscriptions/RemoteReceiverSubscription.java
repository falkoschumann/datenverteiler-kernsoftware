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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionType;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.SubscriptionsManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Anmeldung eines entfernten Empfängers auf diesen Datenverteiler (der möglicherweise Zentraldatenverteiler ist)
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11462 $
 */
public class RemoteReceiverSubscription implements RemoteReceivingSubscription {

	private final SubscriptionsManager _subscriptionsManager;

	private final TransmitterCommunicationInterface _transmitterCommunication;

	private final BaseSubscriptionInfo _baseSubscriptionInfo;

	private final Set<Long> _potentialCentralDistributors = new HashSet<Long>();

	private ReceiverState _receiverState;

	private ConnectionState _lastSendState = null;

	private DataState _lastSendDataState = null;

	public RemoteReceiverSubscription(
			final SubscriptionsManager subscriptionsManager,
			final TransmitterCommunicationInterface transmitterCommunication,
			final BaseSubscriptionInfo baseSubscriptionInfo, final Collection<Long> transmitterIds) {
		_subscriptionsManager = subscriptionsManager;
		_transmitterCommunication = transmitterCommunication;
		_baseSubscriptionInfo = baseSubscriptionInfo;
		_potentialCentralDistributors.addAll(transmitterIds);
	}

	@Override
	public boolean isDrain() {
		return false;
	}

	@Override
	public ReceiveOptions getReceiveOptions() {
		return ReceiveOptions.delayed();
	}

	@Override
	public void sendDataTelegram(final ApplicationDataTelegram applicationDataTelegram) {
		_transmitterCommunication.sendData(applicationDataTelegram, false);
	}

	@Override
	public ReceiverState getState() {
		return _receiverState;
	}

	@Override
	public void setState(final ReceiverState receiverState, final long centralTransmitterId) {
		_receiverState = receiverState;
		ConnectionState receip;
		switch(receiverState) {
			case SENDERS_AVAILABLE:
				receip = ConnectionState.TO_REMOTE_OK;
				break;
			case MULTIPLE_REMOTE_LOCK:
				receip = ConnectionState.TO_REMOTE_MULTIPLE;
				break;
			case NOT_ALLOWED:
				receip = ConnectionState.TO_REMOTE_NOT_ALLOWED;
				break;
			case WAITING:
				return;
			default:
				receip = ConnectionState.TO_REMOTE_NOT_RESPONSIBLE;
				break;
		}
		if(!_potentialCentralDistributors.contains(centralTransmitterId)){
			// Es ist möglicherweise ein anderer Datenverteiler zuständig als angegeben.
			// Hier ein "nicht verantwortlich" zurückmelden, damit keine "Schleifen" entstehen
			receip = ConnectionState.TO_REMOTE_NOT_RESPONSIBLE;
		}
		if(_lastSendState == receip) return;
		_lastSendState = receip;
		_transmitterCommunication.sendReceipt(centralTransmitterId, receip, TransmitterSubscriptionType.Receiver, this);
	}

	/**
	 * Sendet an einen Empfänger falls nötig im Falle eines geänderten Anmeldestatus einen leeren Datensatz mit dem entsprechenden Inhalt.
	 *
	 * @param receiverState         Empfängerstatus
	 */
	@Override
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
		if(_lastSendDataState == newState) return; // Keine mehrfachen Statusnachrichten senden
		_lastSendDataState = newState;

		byte errorFlag = (byte)(newState.getCode() - 1);

		long dataIndex = _subscriptionsManager.getCurrentDataIndex(_baseSubscriptionInfo);

		_transmitterCommunication.sendData(
				new ApplicationDataTelegram(
						_baseSubscriptionInfo, dataIndex + 1, false, errorFlag, null, null, 1, 0, System.currentTimeMillis()
				), false
		);
	}

	@Override
	public void unsubscribe() {
		setState(ReceiverState.UNKNOWN, -1);
	}

	@Override
	public BaseSubscriptionInfo getBaseSubscriptionInfo() {
		return _baseSubscriptionInfo;
	}

	@Override
	public boolean isAllowed(){
		return _subscriptionsManager.isActionAllowed(getUserId(), _baseSubscriptionInfo, UserAction.RECEIVER);
	}

	@Override
	public long getUserId() {
		return _transmitterCommunication.getRemoteUserId();
	}

	@Override
	public long getNodeId() {
		return _transmitterCommunication.getId();
	}

	@Override
	public ConnectionState getConnectionState() {
		return ConnectionState.FROM_REMOTE_OK;
	}

	@Override
	public long getCentralDistributorId() {
		return -1;
	}

	@Override
	public TransmitterCommunicationInterface getCommunication() {
		return _transmitterCommunication;
	}

	@Override
	public String toString() {
		return "Eingehende Anmeldung (" +_receiverState + ") als " +  "Empfänger" +
				" auf " + _subscriptionsManager.subscriptionToString(_baseSubscriptionInfo) +
				" über " + _transmitterCommunication +
		       " (Benutzer=" + _subscriptionsManager.objectToString(getUserId()) + ")";
	}


	@Override
	public Set<Long> getPotentialDistributors() {
		return Collections.unmodifiableSet(_potentialCentralDistributors);
	}

	@Override
	public void setPotentialDistributors(final Collection<Long> value) {
		_potentialCentralDistributors.clear();
		_potentialCentralDistributors.addAll(value);
	}

	@Override
	public void addPotentialDistributor(final long transmitterId) {
		_potentialCentralDistributors.add(transmitterId);
	}

	@Override
	public void removePotentialDistributor(final long transmitterId) {
		_potentialCentralDistributors.remove(transmitterId);
	}
}
