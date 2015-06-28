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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionInfo;
import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.config.AttributeGroupUsageIdentifications;
import de.bsvrz.dav.daf.util.Longs;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.subscriptions.*;
import de.bsvrz.dav.dav.util.accessControl.AccessControlManager;
import de.bsvrz.dav.dav.util.accessControl.DataLoader;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.dav.dav.util.accessControl.UserInfo;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Diese Klasse bietet Funktionen um die Anmeldungen zu verwalten. Bei neuen Datenanmeldungen werden entsprechend neue Anmeldeklassen ({@link
 * de.bsvrz.dav.dav.subscriptions.SubscriptionInfo}) erstellt, welche den Status der angemeldeten Applikationen und Datenverteiler ermitteln und entsprechend
 * veröffentlichen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public class HighLevelSubscriptionsManager extends AbstractSubscriptionsManager {

	/** Verbindungsverwaltung */
	private final HighLevelConnectionsManagerInterface _connectionsManager;

	/** Legt fest, ob die Rechteprüfung aktiviert ist */
	private final ServerDavParameters.UserRightsChecking _userRightsChecking;

	/** Verwaltungsklasse für Zugriffsrechte */
	private volatile AccessControlManager _accessControlManager = null;

	/** Verwaltungsklasse für Telegramme */
	private final TelegramManager _telegramManager;

	/** Eigene Applikation-Verbindung zum Datenverteiler */
	private volatile ClientDavConnection _connection = null;

	private ApplicationStatusUpdater _applicationStatusUpdater;

	private static final Debug _debug = Debug.getLogger();

	/**
	 * Initialisiert den Anmeldungsmanager
	 *
	 * @param telegramManager    Telegramm-Manager
	 * @param userRightsChecking Ob die Zugriffsrechteprüfung aktiviert ist
	 */
	public HighLevelSubscriptionsManager(final TelegramManager telegramManager, final ServerDavParameters.UserRightsChecking userRightsChecking) {
		_telegramManager = telegramManager;
		_connectionsManager = telegramManager.getConnectionsManager();
		_userRightsChecking = userRightsChecking;
	}

	/**
	 * Initialisiert Funktionen, die von der Konfiguration anhängen (z.B. Rechteprüfung)
	 *
	 * @param selfClientDavConnection eigene Datenverteiler-Verbindung
	 * @param applicationStatusUpdater Klasse, die angemeldete Datensätze verschickt
	 */
	public void setConfigurationAvailable(final SelfClientDavConnection selfClientDavConnection, final ApplicationStatusUpdater applicationStatusUpdater) {
		_connection = selfClientDavConnection.getConnection();
		if(_userRightsChecking != ServerDavParameters.UserRightsChecking.Disabled) {
			_accessControlManager = new AccessControlManager(_connection, this, _userRightsChecking == ServerDavParameters.UserRightsChecking.NewDataModel);
		}
		_applicationStatusUpdater = applicationStatusUpdater;
	}

	@Override
	public void addLocalSendSubscription(
			final ApplicationCommunicationInterface application, final SendSubscriptionInfo sendSubscriptionInfo) {
		super.addLocalSendSubscription(application, sendSubscriptionInfo);
		if(_applicationStatusUpdater != null) _applicationStatusUpdater.applicationSubscribedNewConnection(application);
	}

	@Override
	public void removeLocalSendSubscription(
			final ApplicationCommunicationInterface application, final BaseSubscriptionInfo baseSubscriptionInfo) {
		super.removeLocalSendSubscription(application, baseSubscriptionInfo);
		if(_applicationStatusUpdater != null) _applicationStatusUpdater.applicationSubscribedNewConnection(application);
	}

	@Override
	public void addLocalReceiveSubscription(
			final ApplicationCommunicationInterface application, final ReceiveSubscriptionInfo receiveSubscriptionInfo) {
		super.addLocalReceiveSubscription(application, receiveSubscriptionInfo);
		if(_applicationStatusUpdater != null) _applicationStatusUpdater.applicationUnsubscribeConnection(application);

	}

	@Override
	public void removeLocalReceiveSubscriptions(
			final ApplicationCommunicationInterface application, final BaseSubscriptionInfo baseSubscriptionInfo) {
		super.removeLocalReceiveSubscriptions(application, baseSubscriptionInfo);
		if(_applicationStatusUpdater != null) _applicationStatusUpdater.applicationUnsubscribeConnection(application);
	}

	/**
	 * Führt eine Rechteprüfung durch
	 *
	 * @param userId Benutzer-ID
	 * @param info   Anmeldeinfo
	 * @param action Aktion
	 *
	 * @return true wenn die Aktion erlaubt ist, sonst false
	 */
	@Override
	public final boolean isActionAllowed(final long userId, final BaseSubscriptionInfo info, final UserAction action) {
		if(_userRightsChecking == ServerDavParameters.UserRightsChecking.Disabled) {
			return true;
		}

		// Konfiguration oder Parametrierung oder interne Datenverteiler-Applikation beim lokalen Betrieb
		if(userId == 0 || userId == _connectionsManager.getTransmitterId()) {
			return true;
		}

		if(AttributeGroupUsageIdentifications.isConfigurationRequest(info.getUsageIdentification())) {
			// Konfigurationsanfragen sind immer als Sender erlaubt
			if(action == UserAction.SENDER) return true;
		}

		if(AttributeGroupUsageIdentifications.isConfigurationReply(info.getUsageIdentification())) {
			// Konfigurationsantworten sind immer als Senke erlaubt
			if(action == UserAction.DRAIN) return true;
		}

		// Andere Anmeldungen
		if(_accessControlManager == null) {
			// das sollte nicht passieren, da für lokale Datenverteilerverbindung und Konfiguration oben Ausnahmefälle eingerichtet sind
			// und sich andere Verbindungen erst anmelden dürfen, wenn die lokale Datenverteilerverbindung steht und also auch der AccessControlManager da ist.
			throw new IllegalStateException("AccessControlManager wurde noch nicht initialisiert");
		}

		final UserInfo userInfo = _accessControlManager.getUser(userId);

		if(userInfo != null && userInfo.maySubscribeData(info, action)) return true;

		_debug.warning("Anmeldung als " + action + " auf " + subscriptionToString(info) + " durch " + (userInfo == null ? userId : userInfo)
		+ " ist nicht erlaubt.");

		return false;
	}

	/**
	 * Formatiert eine baseSubscriptionInfo zu einem String, der sich zur Darstellung in Fehlerausgaben u.ä. eignet
	 *
	 * @param baseSubscriptionInfo baseSubscriptionInfo
	 *
	 * @return Ein Text der Form [objectPid, atgPid, aspPid] mit möglichen Abweichungen je nach Anmeldung und Zustand
	 */
	public String subscriptionToString(final BaseSubscriptionInfo baseSubscriptionInfo) {
		if(_connection == null) return baseSubscriptionInfo.toString();
		final DataModel dataModel = _connection.getDataModel();
		final SystemObject object = dataModel.getObject(baseSubscriptionInfo.getObjectID());
		final AttributeGroupUsage usage = dataModel.getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification());
		final short simulationVariant = baseSubscriptionInfo.getSimulationVariant();

		if(object == null || usage == null) return baseSubscriptionInfo.toString();

		return "[" + object.getPidOrNameOrId() + ", " + usage.getAttributeGroup().getPidOrNameOrId() + ", " + usage.getAspect().getPidOrNameOrId() + (
				simulationVariant != 0
				? (", " + simulationVariant)
				: "") + "]";
	}

	/**
	 * Formatiert eine Id zu einem Text der sich in Fehlerausgaben und ähnlichem eignet
	 *
	 * @param objectId Id
	 *
	 * @return je nach Verfügbarkeit und Zustand des Datenmodells Pid, Name oder Id des Objekts
	 */
	public String objectToString(final long objectId) {
		if(_connection == null) return "[" + objectId + "]";
		final DataModel dataModel = _connection.getDataModel();
		final SystemObject user = dataModel.getObject(objectId);
		if(user == null) return "[" + objectId + "]";
		return user.getPidOrNameOrId();
	}

	@Override
	public long getThisTransmitterId() {
		return _telegramManager.getConnectionsManager().getTransmitterId();
	}

	/**
	 * Gibt den AccessControlManager zurück
	 *
	 * @return AccessControlManager
	 */
	AccessControlManager getAccessControlManager() {
		return _accessControlManager;
	}

	/**
	 * Wird aufgerufen, wenn dieser Datenverteiler für eine Anmeldung Zentraldatenverteiler geworden ist, z.B. um die Anmeldelisten zu aktualisieren
	 *
	 * @param baseSubscriptionInfo
	 */
	@Override
	public void notifyIsNewCentralDistributor(final BaseSubscriptionInfo baseSubscriptionInfo) {
		_telegramManager.notifyIsNewCentralDistributor(baseSubscriptionInfo);
	}

	/**
	 * Wird aufgerufen, wenn dieser Datenverteiler für eine Anmeldung nicht mehr Zentraldatenverteiler ist, z.B. um die Anmeldelisten zu aktualisieren
	 *
	 * @param baseSubscriptionInfo
	 */
	@Override
	public void notifyWasCentralDistributor(final BaseSubscriptionInfo baseSubscriptionInfo) {
		_telegramManager.notifyWasCentralDistributor(baseSubscriptionInfo);
	}

	public TelegramManager getTelegramManager() {
		return _telegramManager;
	}

	@Override
	public List<Long> getPotentialCentralDistributors(final BaseSubscriptionInfo baseSubscriptionInfo) {
		return Longs.asList(_connectionsManager.getPotentialCentralDistributors(baseSubscriptionInfo));
	}

	@Override
	public T_T_HighLevelCommunicationInterface getBestConnectionToRemoteDav(final long remoteDav) {
		return _connectionsManager.getBestConnectionToRemoteDav(remoteDav);
	}

	/**
	 * Wird bei einem Update der Anmeldelisten aufgerufen. Anmeldungen, die die betreffenden Objekte oder Attributgruppenverwendungen
	 *
	 * @param objectIds Neue Objekte
	 * @param attributeGroupAspectCombinations
	 *                  Neue Attributgruppenverwendungen
	 */
	public void handleListsUpdate(final long[] objectIds, final AttributeGroupAspectCombination[] attributeGroupAspectCombinations) {
		

		Set<Long> objectsToUpdate;
		Set<Long> atgusToUpdate;
		if(objectIds == null) {
			objectsToUpdate = Collections.emptySet();
		}
		else {
			objectsToUpdate = new HashSet<Long>(objectIds.length);
			for(long objectId : objectIds) {
				objectsToUpdate.add(objectId);
			}
		}
		if(attributeGroupAspectCombinations == null) {
			atgusToUpdate = Collections.emptySet();
		}
		else {
			atgusToUpdate = new HashSet<Long>(attributeGroupAspectCombinations.length);
			for(AttributeGroupAspectCombination attributeGroupAspectCombination : attributeGroupAspectCombinations) {
				atgusToUpdate.add(attributeGroupAspectCombination.getAtgUsageIdentification());
			}
		}
		for(SubscriptionInfo subscriptionInfo : _subscriptions.values()) {
			BaseSubscriptionInfo baseSubscriptionInfo = subscriptionInfo.getBaseSubscriptionInfo();
			if(objectsToUpdate.contains(baseSubscriptionInfo.getObjectID()) || atgusToUpdate.contains(baseSubscriptionInfo.getUsageIdentification())) {
				subscriptionInfo.updateRemoteConnections();
			}
		}
	}

	public Collection<? extends Subscription> getAllSubscriptions(final long applicationId) {
		T_A_HighLevelCommunicationInterface applicationConnection = _connectionsManager.getApplicationConnectionFromId(applicationId);
		if(applicationConnection == null) return Collections.emptyList();
		return getAllSubscriptions(applicationConnection);
	}

	/**
	 * Meldet einen lokalen Senker oder eine lokale Quelle ab.
	 *
	 * @param application      Anwendung
	 * @param subscriptionInfo subscriptionInfo
	 */
	@Override
	public void removeLocalSendSubscriptions(
			final ApplicationCommunicationInterface application, final SubscriptionInfo subscriptionInfo) {
		if(application == null) throw new IllegalArgumentException("application ist null");
		if(subscriptionInfo == null) throw new IllegalArgumentException("subscriptionInfo ist null");

		List<SendingSubscription> sendingSubscriptions = subscriptionInfo.removeSendingSubscriptions(application);
		for(SendingSubscription sendingSubscription : sendingSubscriptions) {
			_telegramManager.notifySubscriptionRemoved((LocalSendingSubscription)sendingSubscription);
		}
	}

	/**
	 * Meldet einen lokalen Empfänger oder eine lokale Senke ab.
	 *
	 * @param application      Anwendung
	 * @param subscriptionInfo subscriptionInfo
	 */
	@Override
	public void removeLocalReceiveSubscriptions(
			final ApplicationCommunicationInterface application, final SubscriptionInfo subscriptionInfo) {
		if(application == null) throw new IllegalArgumentException("application ist null");
		if(subscriptionInfo == null) throw new IllegalArgumentException("subscriptionInfo ist null");

		final List<ReceivingSubscription> receivingSubscriptions = subscriptionInfo.removeReceivingSubscriptions(application);
		for(ReceivingSubscription receivingSubscription : receivingSubscriptions) {
			_telegramManager.notifySubscriptionRemoved((LocalReceivingSubscription)receivingSubscription);
		}
	}

	@Override
	public void initializeUser(final long userId) {
		_debug.fine("Lade Benutzerrechte für Benutzer", userId);
		if(_accessControlManager != null) {
			UserInfo user = _accessControlManager.getUser(userId);
			if(user instanceof DataLoader) {
				((DataLoader) user).waitForInitializationTree();
			}
		}
	}
}
