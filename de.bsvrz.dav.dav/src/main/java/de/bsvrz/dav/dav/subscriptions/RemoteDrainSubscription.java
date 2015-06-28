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
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.SubscriptionsManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Anmeldung als Sender auf eine Senke bei einem entfernten Zentral-Datenverteiler.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11467 $
 */
public class RemoteDrainSubscription implements RemoteReceivingSubscription, RemoteCentralSubscription {

	private final SubscriptionsManager _subscriptionsManager;

	private long _centralDistributor = -1;

	private final TransmitterCommunicationInterface _transmitterCommunication;

	private final BaseSubscriptionInfo _baseSubscriptionInfo;

	private ReceiverState _receiverState = ReceiverState.UNKNOWN;

	private ConnectionState _connectionState = ConnectionState.TO_REMOTE_WAITING;

	private final Set<Long> _potentialCentralDistributors = new HashSet<Long>();

	public RemoteDrainSubscription(
			final SubscriptionsManager subscriptionsManager,
			final BaseSubscriptionInfo baseSubscriptionInfo,
			final TransmitterCommunicationInterface connectionToRemoteDav) {
		_subscriptionsManager = subscriptionsManager;
		_baseSubscriptionInfo = baseSubscriptionInfo;
		_transmitterCommunication = connectionToRemoteDav;
	}

	@Override
	public final void subscribe() {
		_transmitterCommunication.subscribeToRemote(this);
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

	@Override
	public boolean isDrain() {
		return true;
	}

	@Override
	public long getCentralDistributorId() {
		return _centralDistributor;
	}

	@Override
	public ReceiveOptions getReceiveOptions() {
		return ReceiveOptions.delayed();
	}

	@Override
	public void sendDataTelegram(final ApplicationDataTelegram applicationDataTelegram) {
		_transmitterCommunication.sendData(applicationDataTelegram, true);
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

	@Override
	public void sendStateTelegram(final ReceiverState receiverState) {

	}

	@Override
	public BaseSubscriptionInfo getBaseSubscriptionInfo() {
		return _baseSubscriptionInfo;
	}

	@Override
	public boolean isAllowed(){
		return _subscriptionsManager.isActionAllowed(getUserId(), _baseSubscriptionInfo, UserAction.DRAIN);
	}

	@Override
	public long getUserId() {
		if(_transmitterCommunication == null) return -1;
		return _transmitterCommunication.getRemoteUserId();
	}

	@Override
	public long getNodeId() {
		return _centralDistributor;
	}

	@Override
	public TransmitterCommunicationInterface getCommunication() {
		return _transmitterCommunication;
	}

	@Override
	public void setRemoteState(final long mainTransmitterId, final ConnectionState state) {
		_connectionState = state;
		_centralDistributor = mainTransmitterId;
	}

	@Override
	public ConnectionState getConnectionState() {
		return _connectionState;
	}

	@Override
	public void unsubscribe() {
		_transmitterCommunication.unsubscribeToRemote(this);
		setState(ReceiverState.UNKNOWN, -1);
		setRemoteState(-1, ConnectionState.TO_REMOTE_WAITING);
	}

	@Override
	public String toString() {
		return "Ausgehende Anmeldung (" + _receiverState + ", " + _connectionState + ")" +
				" auf " + _subscriptionsManager.subscriptionToString(_baseSubscriptionInfo) +
				" zur Senke über " + _transmitterCommunication +
		        " (Benutzer=" + _subscriptionsManager.objectToString(getUserId()) + ")";
	}
}
