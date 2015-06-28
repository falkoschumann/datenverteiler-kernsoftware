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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionType;
import de.bsvrz.dav.daf.util.HashBagMap;
import de.bsvrz.dav.dav.subscriptions.*;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public abstract class AbstractSubscriptionsManager implements SubscriptionsManager {

	private static final Debug _debug = Debug.getLogger();

	/** HashMap mit der Zuordnung einer Anmeldeklasse zu einer BaseSubscriptionInfo. */
	protected final ConcurrentHashMap<BaseSubscriptionInfo, SubscriptionInfo> _subscriptions = new ConcurrentHashMap<BaseSubscriptionInfo, SubscriptionInfo>(128);

	protected final DataIndexCounter<BaseSubscriptionInfo> _dataIndexCounter = new DataIndexCounter<BaseSubscriptionInfo>();

	public static String dataIndexToString(final long c) {
		long time = c >>> 32;
		long index = (c >>> 2) & 0x3fffffff;
		long ds = c & 3;
		return time + "#" + index + "#" + ds;
	}

	/**
	 * Gibt zu einer baseSubscriptionInfo die zugehörige Anmeldungsinfo zurück oder erstellt diese falls sie nicht existiert. Nachdem die Benutzung des Objekts
	 * beendet ist, muss {@link de.bsvrz.dav.dav.subscriptions.SubscriptionInfo#close()} aufgerufen werden, damit eventuelle Aufräumarbeiten erledigt werden können.
	 *
	 * @param baseSubscriptionInfo baseSubscriptionInfo
	 *
	 * @return Anmeldungsklasse
	 */
	@Override
	public synchronized SubscriptionInfo openSubscriptionInfo(final BaseSubscriptionInfo baseSubscriptionInfo) {
		SubscriptionInfo result = _subscriptions.get(baseSubscriptionInfo);
		if(result != null){
			result.open();
			return result;
		}
		final SubscriptionInfo newInfo = new SubscriptionInfo(this, baseSubscriptionInfo);
		final SubscriptionInfo subscriptionInfo = _subscriptions.putIfAbsent(baseSubscriptionInfo, newInfo);
		result = subscriptionInfo != null ? subscriptionInfo : newInfo;
		result.open();
		return result;
	}

	/**
	 * Gibt zu einer baseSubscriptionInfo die zugehörige Anmeldungsinfo zurück. Nachdem die Benutzung des Objekts
	 * beendet ist, muss (sofern Rückgabewert != null) {@link de.bsvrz.dav.dav.subscriptions.SubscriptionInfo#close()} aufgerufen werden, damit eventuelle Aufräumarbeiten erledigt werden können.
	 *
	 * @param baseSubscriptionInfo baseSubscriptionInfo
	 *
	 * @return Anmeldungsklasseoder null falls nicht existent
	 */
	@Override
	public synchronized SubscriptionInfo openExistingSubscriptionInfo(final BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) return null;
		SubscriptionInfo result = _subscriptions.get(baseSubscriptionInfo);
		if(result == null) return null;
		result.open();
		return result;
	}

	/**
	 * Gibt zu einer baseSubscriptionInfo die zugehörige Anmeldungsinfo zurück.
	 *
	 * @param baseSubscriptionInfo baseSubscriptionInfo
	 *
	 * @return Anmeldungsklasse oder null falls zu dieser baseSubscriptionInfo keine Anmeldungsinfo vorliegt
	 */
	@Override
	public SubscriptionInfo getSubscriptionInfo(final BaseSubscriptionInfo baseSubscriptionInfo) {
		if(baseSubscriptionInfo == null) return null;
		return _subscriptions.get(baseSubscriptionInfo);
	}

	/**
	 * Meldet einen lokalen Sender oder eine lokale Quelle an
	 *
	 * @param application          Verbindung
	 * @param sendSubscriptionInfo Anmeldeparameter
	 */
	public void addLocalSendSubscription(final ApplicationCommunicationInterface application, final SendSubscriptionInfo sendSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = openSubscriptionInfo(sendSubscriptionInfo.getBaseSubscriptionInfo());
		List<SendingSubscription> previousSubscriptions = subscriptionInfo.getSendingSubscriptions(application);
		final LocalSendingSubscription localSendingSubscription = new LocalSendingSubscription(
				this,
				sendSubscriptionInfo.getBaseSubscriptionInfo(),
				sendSubscriptionInfo.isSource(),
				sendSubscriptionInfo.isRequestSupported(),
				application
		);
		subscriptionInfo.addSendingSubscription(localSendingSubscription);
		for(SendingSubscription sendingSubscription : previousSubscriptions) {
			subscriptionInfo.removeSendingSubscription(sendingSubscription);
		}
		subscriptionInfo.close();
	}

	/**
	 * Meldet einen lokalen Sender oder eine lokale Quelle ab. Gibt eine Warnung aus, falls die angegebene Anmeldung nicht besteht
	 *
	 * @param application          Verbindung
	 * @param baseSubscriptionInfo Objekt und Attributgruppenverwendung
	 */
	public void removeLocalSendSubscription(final ApplicationCommunicationInterface application, final BaseSubscriptionInfo baseSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = openExistingSubscriptionInfo(baseSubscriptionInfo);
		if(subscriptionInfo == null) {
			_debug.warning("Erhalte Abmeldeauftrag für Datenidentifikation, die nicht angemeldet ist: " + baseSubscriptionInfo);
			return;
		}
		try {
			removeLocalSendSubscriptions(application, subscriptionInfo);
		}
		finally {
			subscriptionInfo.close();
		}
	}

	/**
	 * Meldet einen lokalen Empfänger oder eine lokale Senke an
	 *
	 * @param application             Verbindung
	 * @param receiveSubscriptionInfo Anmeldeparameter
	 */
	public void addLocalReceiveSubscription(final ApplicationCommunicationInterface application, final ReceiveSubscriptionInfo receiveSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = openSubscriptionInfo(receiveSubscriptionInfo.getBaseSubscriptionInfo());
		List<ReceivingSubscription> previousSubscriptions = subscriptionInfo.getReceivingSubscriptions(application);
		final LocalReceivingSubscription localReceivingSubscription = new LocalReceivingSubscription(
				this,
				receiveSubscriptionInfo.getBaseSubscriptionInfo(),
				receiveSubscriptionInfo.isDrain(),
				receiveSubscriptionInfo.getReceiveOptions(),
				application
		);
		subscriptionInfo.addReceivingSubscription(localReceivingSubscription);
		for(ReceivingSubscription receivingSubscription : previousSubscriptions) {
			subscriptionInfo.removeReceivingSubscription(receivingSubscription);
		}
		subscriptionInfo.close();
	}

	/**
	 * Meldet einen lokalen Empfänger oder eine lokale Senke ab. Gibt eine Warnung aus, falls die angegebene Anmeldung nicht besteht
	 *
	 * @param application          Verbindung
	 * @param baseSubscriptionInfo Objekt und Attributgruppenverwendung
	 */
	public void removeLocalReceiveSubscriptions(final ApplicationCommunicationInterface application, final BaseSubscriptionInfo baseSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = openExistingSubscriptionInfo(baseSubscriptionInfo);
		if(subscriptionInfo == null) {
			_debug.warning("Erhalte Abmeldeauftrag für Datenidentifikation, die nicht angemeldet ist: " + baseSubscriptionInfo);
			return;
		}
		try {
			removeLocalReceiveSubscriptions(application, subscriptionInfo);
		}
		finally {
			subscriptionInfo.close();
		}
	}

	/**
	 * Prüft von allen Anmeldungen die den Benutzer betreffen die Rechte erneut
	 *
	 * @param userId Id des Benutzers
	 */
	@Override
	public void handleUserRightsChanged(final long userId) {
		for(SubscriptionInfo subscriptionInfo : _subscriptions.values()) {
			subscriptionInfo.handleUserRightsChanged(userId);
		}
	}

	/**
	 * Meldet einen lokalen Senker oder eine lokale Quelle ab.
	 *
	 * @param application      Anwendung
	 * @param subscriptionInfo subscriptionInfo
	 */
	public void removeLocalSendSubscriptions(
			final ApplicationCommunicationInterface application, final SubscriptionInfo subscriptionInfo) {
		if(application == null) throw new IllegalArgumentException("application ist null");
		if(subscriptionInfo == null) throw new IllegalArgumentException("subscriptionInfo ist null");

		subscriptionInfo.removeSendingSubscriptions(application);
	}

	/**
	 * Meldet einen lokalen Empfänger oder eine lokale Senke ab.
	 *
	 * @param application      Anwendung
	 * @param subscriptionInfo subscriptionInfo
	 */
	public void removeLocalReceiveSubscriptions(
			final ApplicationCommunicationInterface application, final SubscriptionInfo subscriptionInfo) {
		if(application == null) throw new IllegalArgumentException("application ist null");
		if(subscriptionInfo == null) throw new IllegalArgumentException("subscriptionInfo ist null");

		subscriptionInfo.removeReceivingSubscriptions(application);
	}

	/**
	 * Entfernt von einer Verbindung alle Anmeldungen (sinnvoll z.B. bei Terminierung der Verbindung)
	 *
	 * @param communication Verbindung
	 */
	public void removeAllSubscriptions(final ApplicationCommunicationInterface communication) {
		for(SubscriptionInfo subscriptionInfo : _subscriptions.values()) {
			removeLocalReceiveSubscriptions(communication, subscriptionInfo);
			removeLocalSendSubscriptions(communication, subscriptionInfo);
		}
	}

	/**
	 * Entfernt von einer Verbindung alle Anmeldungen (sinnvoll z.B. bei Terminierung der Verbindung)
	 *
	 * @param communication Verbindung
	 */
	public void removeAllSubscriptions(final TransmitterCommunicationInterface communication) {
		for(SubscriptionInfo subscriptionInfo : _subscriptions.values()) {
			subscriptionInfo.removeReceivingSubscriptions(communication);
			subscriptionInfo.removeSendingSubscriptions(communication);
		}
	}

	@Override
	public void handleTransmitterSubscriptionReceipt(
			final TransmitterCommunicationInterface communication,
			final TransmitterSubscriptionType transmitterSubscriptionType,
			final BaseSubscriptionInfo baseSubscriptionInfo,
			final ConnectionState connectionState,
			final long mainTransmitterId) {
		final SubscriptionInfo subscriptionInfo = openSubscriptionInfo(baseSubscriptionInfo);
		if(transmitterSubscriptionType == TransmitterSubscriptionType.Sender) {
			subscriptionInfo.setRemoteDrainSubscriptionStatus(communication, connectionState, mainTransmitterId);
		}
		else {
			subscriptionInfo.setRemoteSourceSubscriptionStatus(communication, connectionState, mainTransmitterId);
		}
		subscriptionInfo.close();
	}

	@Override
	public void updateDestinationRoute(
			final long transmitterId, final TransmitterCommunicationInterface oldConnection, final TransmitterCommunicationInterface newConnection) {
		Collection<? extends Subscription> oldSubscriptions = getAllSubscriptions(oldConnection);
		for(Subscription oldSubscription : oldSubscriptions) {
			if(oldSubscription instanceof RemoteCentralSubscription) {
				RemoteCentralSubscription centralSubscription = (RemoteCentralSubscription)oldSubscription;
				if(centralSubscription.getCentralDistributorId() == transmitterId) {
					getSubscriptionInfo(centralSubscription.getBaseSubscriptionInfo()).updateBestWay(transmitterId, oldConnection, newConnection);
				}
			}
		}
	}

	@Override
	public long getNextDataIndex(final BaseSubscriptionInfo baseSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = getSubscriptionInfo(baseSubscriptionInfo);
		if(subscriptionInfo == null) return 0;
		synchronized(subscriptionInfo){
			return subscriptionInfo.getNextDataIndex(_dataIndexCounter.increment(baseSubscriptionInfo));
		}
	}

	@Override
	public long getCurrentDataIndex(final BaseSubscriptionInfo baseSubscriptionInfo) {
		final SubscriptionInfo subscriptionInfo = getSubscriptionInfo(baseSubscriptionInfo);
		if(subscriptionInfo == null) return 0;
		synchronized(subscriptionInfo){
			long number = _dataIndexCounter.get(baseSubscriptionInfo);
			if(number == -1) return 0;
			return subscriptionInfo.getCurrentDataIndex(number);
		}
	}

	/**
	 * Gibt den nächsten Datenindex für die angegebene Anmeldung zurück und zählt den Index entsprechend hoch
	 * @param subscriptionInfo Anmeldung
	 * @return Datenindex
	 */
	public long getNextDataIndex(final SubscriptionInfo subscriptionInfo) {
		if(subscriptionInfo == null) return 0;
		synchronized(subscriptionInfo){
			return subscriptionInfo.getNextDataIndex(_dataIndexCounter.increment(subscriptionInfo.getBaseSubscriptionInfo()));
		}
	}

	@Override
	public synchronized void removeSubscriptionInfo(final SubscriptionInfo subscriptionInfo) {
		assert subscriptionInfo.isEmpty();
		_subscriptions.remove(subscriptionInfo.getBaseSubscriptionInfo());
	}

	@Override
	public void connectToRemoteDrains(final SubscriptionInfo subscriptionInfo, final Set<Long> distributorsToUse) {
		HashBagMap<TransmitterCommunicationInterface, Long> connections = getCentralDistributorConnections(distributorsToUse);
		for(Map.Entry<TransmitterCommunicationInterface, Collection<Long>> entry : connections.entrySet()) {
			long remoteUserId = entry.getKey().getRemoteUserId();

			if(!isActionAllowed(remoteUserId, subscriptionInfo.getBaseSubscriptionInfo(), UserAction.DRAIN)){
				// Bei fehlenden Benutzerrechten Anmeldung gar nicht erst versuchen
				continue;
			}

			RemoteDrainSubscription subscription = subscriptionInfo.getOrCreateRemoteDrainSubscription(entry.getKey());
			subscription.setPotentialDistributors(entry.getValue());
			subscription.subscribe();
		}
	}

	@Override
	public void connectToRemoteSources(final SubscriptionInfo subscriptionInfo, final Set<Long> distributorsToUse) {
		HashBagMap<TransmitterCommunicationInterface, Long> connections = getCentralDistributorConnections(distributorsToUse);
		for(Map.Entry<TransmitterCommunicationInterface, Collection<Long>> entry : connections.entrySet()) {
			long remoteUserId = entry.getKey().getRemoteUserId();

			if(!isActionAllowed(remoteUserId, subscriptionInfo.getBaseSubscriptionInfo(), UserAction.SOURCE)){
				// Bei fehlenden Benutzerrechten Anmeldung gar nicht erst versuchen
				continue;
			}

			RemoteSourceSubscription subscription = subscriptionInfo.getOrCreateRemoteSourceSubscription(entry.getKey());
			subscription.setPotentialDistributors(entry.getValue());
			subscription.subscribe();
		}
	}

	/**
	 * Gibt eine HashBagMap zurück, die angibt, über welche lokalen Datenverteilerverbindungen welche Zentraldatenverteiler am besten erreicht werden.
	 * Die Keys der Rückgabe sind die lokalen Verbindungen, die Values sind die über diesen Key erreichbaren Zentraldatenverteiler zum angegebenen
	 * BaseSubscriptionInfo. Ein Zentraldatenverteiler kommt immer nur in einem Key-Value-paar vor, selbst wenn er über mehrere lokale Verbindungen zu
	 * erreichen ist. Es wird hierbei die optimale lokale Verbindung ausgewählt (Siehe {@link BestWayManager}).
	 *
	 * @param baseSubscriptionInfo Anmeldeinformation
	 *
	 * @return HashBagMap, siehe Funktionsbeschreibung
	 */
	private HashBagMap<TransmitterCommunicationInterface, Long> getCentralDistributorConnections(final BaseSubscriptionInfo baseSubscriptionInfo) {
		return getCentralDistributorConnections(getPotentialCentralDistributors(baseSubscriptionInfo));
	}

	/**
	 * Gibt eine HashBagMap zurück, die angibt, über welche lokalen Datenverteilerverbindungen welche Zentraldatenverteiler am besten erreicht werden.
	 * Die Keys der Rückgabe sind die lokalen Verbindungen, die Values sind die über diesen Key erreichbaren Zentraldatenverteiler. Ein Zentraldatenverteiler kommt
	 * immer nur in einem Key-Value-paar vor, selbst wenn er über mehrere lokale Verbindungen zu
	 * erreichen ist. Es wird hierbei die optimale lokale Verbindung ausgewählt (Siehe {@link de.bsvrz.dav.dav.main.BestWayManager}).
	 *
	 * @param potentialCentralDistributors IDs der Zentraldatenverteiler, welche erreicht werden sollen
	 *
	 * @return HashBagMap, siehe Funktionsbeschreibung
	 */
	private HashBagMap<TransmitterCommunicationInterface, Long> getCentralDistributorConnections(final Collection<Long> potentialCentralDistributors) {
		final HashBagMap<TransmitterCommunicationInterface, Long> wayMap = new HashBagMap<TransmitterCommunicationInterface, Long>();
		for(long dav : potentialCentralDistributors) {
			TransmitterCommunicationInterface bestConnectionToRemoteDav = getBestConnectionToRemoteDav(dav);
			if(bestConnectionToRemoteDav != null){
				wayMap.add(bestConnectionToRemoteDav, dav);
			}
		}
		// wayMap enthält jetzt eine Map wo die lokal verbundenen Datenverteilern den evtl. dahinterliegenden potentiellen Zentraldatenverteilern zugeordnet
		// sind.
		return wayMap;
	}

	
	public Collection<? extends Subscription> getAllSubscriptions(final CommunicationInterface communicationInterface) {
		final List<Subscription> result = new ArrayList<Subscription>();
		for(SubscriptionInfo subscriptionInfo : _subscriptions.values()) {
			result.addAll(subscriptionInfo.getSendingSubscriptions(communicationInterface));
			result.addAll(subscriptionInfo.getReceivingSubscriptions(communicationInterface));
		}
		return result;
	}

	public void initializeUser(final long userId){
	}

}
