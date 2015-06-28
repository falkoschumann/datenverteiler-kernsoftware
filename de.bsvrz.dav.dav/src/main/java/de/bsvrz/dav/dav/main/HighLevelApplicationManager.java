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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.*;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;

/**
 * Klasse, die Telegramme von den Applikations-Verbindungen entgegennimmt und entsprechend weiterleitet und verarbeitet
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public class HighLevelApplicationManager {

	private final HighLevelConnectionsManagerInterface _connectionsManager;

	private volatile ApplicationObjectManager _applicationObjectManager = null;

	private final TelegramManager _telegramManager;

	private final HighLevelSubscriptionsManager _subscriptionsManager;

	private ApplicationStatusUpdater _applicationStatusUpdater = null;

	/**
	 * Initialisiert einen neuen HighLevelApplicationManager
	 * @param connectionsManager Connections-Manager
	 */
	public HighLevelApplicationManager(final HighLevelConnectionsManagerInterface connectionsManager) {
		_connectionsManager = connectionsManager;
		_telegramManager = connectionsManager.getTelegramManager();
		_subscriptionsManager = _telegramManager.getSubscriptionsManager();
	}

	/**
	 * Wird aufgerufen, wenn die Konfiguration verfügbar wird.
	 * @param selfClientDavConnection Verbindung zur Konfiguration
	 * @param configAreaPidForApplicationObjects
	 */
	public void setConfigurationAvailable(final SelfClientDavConnection selfClientDavConnection, final String configAreaPidForApplicationObjects) {
		_applicationObjectManager = new ApplicationObjectManager(_connectionsManager, selfClientDavConnection.getConnection(), configAreaPidForApplicationObjects);
		_applicationStatusUpdater = new ApplicationStatusUpdater(_connectionsManager, selfClientDavConnection.getConnection());
	}

	/**
	 * Behandelt eine Anmeldung zum Senden von Daten
	 * @param communication Verbindung
	 * @param sendSubscriptionTelegram Anmeldetelegramm
	 */
	public void handleSendSubscription(final T_A_HighLevelCommunication communication, final SendSubscriptionTelegram sendSubscriptionTelegram) {
		_subscriptionsManager.addLocalSendSubscription(communication, sendSubscriptionTelegram.getSendSubscriptionInfo());
	}

	/**
	 * Behandelt eine Abmeldung zum Senden von Daten
	 * @param communication Verbindung
	 * @param sendUnsubscriptionTelegram Abmeldetelegramm
	 */
	public void handleSendUnsubscription(
			final T_A_HighLevelCommunication communication, final SendUnsubscriptionTelegram sendUnsubscriptionTelegram) {
		_subscriptionsManager.removeLocalSendSubscription(communication, sendUnsubscriptionTelegram.getUnSubscriptionInfo());
	}

	/**
	 * Behandelt eine Anmeldung zum Empfangen von Daten
	 * @param communication Verbindung
	 * @param receiveSubscriptionTelegram Anmeldetelegramm
	 */
	public void handleReceiveSubscription(
			final T_A_HighLevelCommunication communication, final ReceiveSubscriptionTelegram receiveSubscriptionTelegram) {
		_subscriptionsManager.addLocalReceiveSubscription(communication, receiveSubscriptionTelegram.getReceiveSubscriptionInfo());
	}

	/**
	 * Behandelt eine Abmeldung zum Empfangen von Daten
	 * @param communication Verbindung
	 * @param receiveUnsubscriptionTelegram Abmeldetelegramm
	 */
	public void handleReceiveUnsubscription(
			final T_A_HighLevelCommunication communication, final ReceiveUnsubscriptionTelegram receiveUnsubscriptionTelegram) {
		_subscriptionsManager.removeLocalReceiveSubscriptions(communication, receiveUnsubscriptionTelegram.getUnSubscriptionInfo());
	}

	/**
	 * Behandelt ein ankommendes Daten-Telegramm
	 * @param communication Verbindung
	 * @param applicationDataTelegram Daten-Telegramm
	 */
	public void handleDataTelegram(final T_A_HighLevelCommunication communication, final ApplicationDataTelegram applicationDataTelegram) {
		_telegramManager.handleDataTelegram(communication, applicationDataTelegram, false);
	}

	/**
	 * Gibt die Konfigurations-Id zu einer Pid zurück
	 * @param configurationPid Pid eines Konfigurationsverantwortlichen
	 * @return die Id der Konfiguration oder -1 falls kein Objekt gefunden werden konnte
	 */
	public long getConfigurationId(final String configurationPid) {
		return _connectionsManager.getConfigurationId(configurationPid);
	}

	/**
	 * Gibt die ID einer Applikation zurück und erstellt gegebenenfalls ein Systemobjekt
	 *
	 * @param communication
	 * @param applicationTypePid die Pid des Applikationstyps
	 * @param applicationName    der Applikationsname
	 *
	 * @return die Applikation ID oder -1 bei einem Problem
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationChangeException signalisiert Fehler bei Konfigurationsänderungen
	 */
	public long createNewApplication(final T_A_HighLevelCommunication communication, final String applicationTypePid, final String applicationName) throws ConfigurationChangeException {
		if(applicationTypePid == null) throw new IllegalArgumentException("applicationTypePid ist null");
		if(applicationName == null) throw new IllegalArgumentException("applicationName ist null");

		if(applicationTypePid.equals(_connectionsManager.getTransmitterTypePid()) && applicationName.equals(_connectionsManager.getTransmitterApplicationName())) {
			return _connectionsManager.getTransmitterId();
		}
		if(_applicationObjectManager != null) {
			long application = _applicationObjectManager.createApplication(applicationTypePid, applicationName);
			if(_applicationStatusUpdater != null) _applicationStatusUpdater.applicationAdded(communication);
			return application;
		}
		return -1;
	}


	/**
	 * Wird nach dem Verbindungsabbruch zu einer Applikation aufgerufen
	 * @param communication Verbindung, die terminiert wurde
	 */
	public void removeApplication(final T_A_HighLevelCommunication communication) {

		// Während der Datenverteiler sich gerade beendet, nicht versuchen Applikationsobjekte zu löschen oder ähnliches.
		// da die Konfigurationsverbindung schon getrennt ist oder jeden Moment getrennt werden kann und es beim Löschauftrag
		// dazu führen kann, dass der Datenverteiler beim Warten auf die Konfiguration hängen bleibt.
		if(_connectionsManager.isClosing()) return;

		if(communication.isConfiguration()){
			_connectionsManager.shutdown(true, "Die Konfiguration ist nicht mehr vorhanden. Der Datenverteiler wird jetzt beendet");
			return;
		}
		if(_applicationObjectManager != null) {
			_applicationObjectManager.removeApplication(communication.getId());
		}
		if(_applicationStatusUpdater != null) {
			_applicationStatusUpdater.applicationRemoved(communication);
		}
		_connectionsManager.removeConnection(communication);
	}

	public ApplicationStatusUpdater getApplicationStatusUpdater() {
		return _applicationStatusUpdater;
	}
}
