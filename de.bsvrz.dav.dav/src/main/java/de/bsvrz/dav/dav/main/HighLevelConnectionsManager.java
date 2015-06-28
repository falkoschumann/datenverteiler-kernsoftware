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
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.List;

/**
 * Diese Klasse stellt das Bindeglied zwischen dem {@link LowLevelConnectionsManager} und den Funktionen der oberen Ebene wie dem {@link
 * HighLevelApplicationManager}, {@link HighLevelTransmitterManager} und dem {@link TelegramManager} dar. Zusätzlich bietet diese Klasse Funktionen von
 * allgemeinem Interesse zur Verbindungsverwaltung, wie z.B. das Herausfinden einer Datenverteiler- oder Applikations-Verbindung von einer Id, das Terminieren
 * des Datenverteilers, das Terminieren von Verbindungen usw., zudem bindet diese Klasse den {@link TerminationQueryReceiver} ein, der clientseitige
 * Terminierungsanfragen entgegennimmt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public final class HighLevelConnectionsManager implements HighLevelConnectionsManagerInterface {

	private static final Debug _debug = Debug.getLogger();

	/** Klasse die Anwendungsverbindungen verwaltet und Telegramme verarbeitet */
	private final HighLevelApplicationManager _highLevelApplicationManager;

	/**
	 * Klasse, die Datenverteiler-Verbindungen verwaltet, Telegramme verarbeitet und Anmeldelisten, Beste-Wege usw. verwaltet. Ist null bis eine lokale
	 * Datenverteilerverbindung vorliegt.
	 */
	private volatile HighLevelTransmitterManager _highLevelTransmitterManager = null;

	/** Datenmodell. Ist null solange noch keine Verbindung zur Konfiguration hergestellt werden konnte. */
	private volatile DafDataModel _dataModel = null;

	/** Lokale Datenverteilerverbindung. Ist null solange noch keine Verbindung zur Konfiguration hergestellt werden konnte. */
	private volatile ClientDavConnection _connection;

	/**
	 * Der zugehörige Telegramm-Manager, der dafür sorgt, dass ankommende Anmelde, Abmelde und Daten-Telegramme (evtl. als Zentraldatenverteiler) entsprechend
	 * verarbeitet werden. Dazu bindet der TelegramManager auch den {@link HighLevelSubscriptionsManager} ein.
	 */
	private final TelegramManager _telegramManager;

	/** Referenz auf den {@link LowLevelConnectionsManager}, der die einzelnen Applikations- und Datenverteiler-Verbindungen auf unterer Ebene speichert. */
	private final LowLevelConnectionsManagerInterface _lowLevelConnectionsManager;

	/** Typ-Pid der lokalen Datenverteilerverbindung */
	private final String _transmitterTypePid;

	/** Applikationsname der lokalen Datenverteilerverbindung */
	private final String _transmitterApplicationName;

	/** Anmeldename des Datenverteilers z.B. zur Authentifizierung bei anderen Datenverteilern */
	private final String _userName;

	/** Passwort des Datenverteilers z.B. zur Authentifizierung bei anderen Datenverteilern */
	private final String _userPassword;

	/**
	 * ATG, die benötigt wird um Terminierungsanfragen für Applikationen und Dav-Verbindungen zu erhalten. Ist diese ATG nicht vorhanden, so wird der
	 * Datenverteiler gestartet ohne diese Funktionalität zur Verfügung zu stellen.
	 */
	private static final String _pidTerminierung = "atg.terminierung";

	/**
	 * Objekt, das das eigene Datenverteilerobjekt darstellt. Warnung: _connection.getLocalDav() liefert nicht den lokalen Datenverteiler, sondern den mit der Konfiguration, wenn der
	 * Datenverteiler keine eigene Konfiguration besitzt.
	 */
	private ConfigurationObject _davObject;

	private ListsManager _listsManager;

	/**
	 * Initialisiert den HighLevelConnectionsManager
	 *
	 * @param lowLevelConnectionsManager LowLevelConnectionsManager
	 * @param userRightsChecking
	 */
	public HighLevelConnectionsManager(final LowLevelConnectionsManagerInterface lowLevelConnectionsManager, final ServerDavParameters.UserRightsChecking userRightsChecking) {
		_lowLevelConnectionsManager = lowLevelConnectionsManager;
		_transmitterTypePid = _lowLevelConnectionsManager.getClientDavParameters().getApplicationTypePid();
		_transmitterApplicationName = _lowLevelConnectionsManager.getClientDavParameters().getApplicationName();
		_userName = _lowLevelConnectionsManager.getServerDavParameters().getUserName();
		_userPassword = _lowLevelConnectionsManager.getServerDavParameters().getUserPassword();
		_telegramManager = new TelegramManager(this, userRightsChecking);
		_highLevelApplicationManager = new HighLevelApplicationManager(this);
		_listsManager = new ListsManager(this);
	}

	/**
	 * Setzt die eigene Datenverteilerverbindung und initialisiert damit weitere Funktionen
	 *
	 * @param selfClientDavConnection selfClientDavConnection
	 */
	public void setSelfClientDavConnection(final SelfClientDavConnection selfClientDavConnection) {
		_dataModel = selfClientDavConnection.getDataModel();
		_connection = selfClientDavConnection.getConnection();
		_davObject = (ConfigurationObject)_dataModel.getObject(getTransmitterId());
		initializeTerminationQueries();
		publishReleaseInfo();
		_highLevelApplicationManager.setConfigurationAvailable(
				selfClientDavConnection,
		         _lowLevelConnectionsManager.getServerDavParameters().getConfigAreaPidForApplicationObjects()
		);
		_highLevelTransmitterManager = new HighLevelTransmitterManager(this, _listsManager);
		_telegramManager.setConfigurationAvailable(selfClientDavConnection, _highLevelApplicationManager.getApplicationStatusUpdater());
	}

	/** Initialisiert die Anfragen zur Terminierung. Dazu muss die eigene Datenverteilerverbindung bestehen. */
	private void initializeTerminationQueries() {
		if(_dataModel.getObject(_pidTerminierung) != null) {

			final SystemObject davObject;
			final ClientReceiverInterface receiver = new TerminationQueryReceiver(this);
			final DataDescription terminationQueryDataDescription;

			davObject = _dataModel.getObject(getTransmitterId());

			final AttributeGroup atgTerminierung = _dataModel.getAttributeGroup(_pidTerminierung);
			final Aspect aspAnfrage = _dataModel.getAspect("asp.anfrage");
			terminationQueryDataDescription = new DataDescription(atgTerminierung, aspAnfrage);

			_connection.subscribeReceiver(
					receiver, davObject, terminationQueryDataDescription, ReceiveOptions.normal(), ReceiverRole.drain()
			);
		}
		else {
			_debug.warning(
					"Die Attributgruppe zum Empfangen von Terminierungsanfragen ist nicht vorhanden. Der Datenverteiler wird ohne diese Funktion gestartet. Für diese Funktion ist der Bereich kb.systemModellGlobal in Version 24 oder größer notwendig."
			);
		}
	}

	private void publishReleaseInfo() {
		// Informationen zum Distributionspaket des Datenverteilers werden publiziert
		try {
			final AttributeGroup infoAtg = _dataModel.getAttributeGroup("atg.distributionspaketReleaseInfo");
			if(infoAtg == null) {
				_debug.info(
						"Informationen zum Distributionspaket des Datenverteilers können nicht publiziert werden, weil das Datenmodell "
						+ "(kb.systemModellGlobal) noch nicht aktualisiert wurde."
				);
			}
			else {
				final Aspect aspect = _dataModel.getAspect("asp.standard");
				final DataDescription dataDescription = new DataDescription(infoAtg, aspect);
				final String packageName = "de.bsvrz.dav.dav";
				String release = "unbekannt";
				String revision = "unbekannt";
				String compileTime = "unbekannt";
				String licence = "unbekannt";
				String dependsOnCompiled = "unbekannt";
				String dependsOnSource = "unbekannt";
				String dependsOnLib = "unbekannt";
				try {
					final Class<?> infoClass = Class.forName(packageName + ".PackageRuntimeInfo");
					release = (String)infoClass.getMethod("getRelease").invoke(null);
					revision = (String)infoClass.getMethod("getRevision").invoke(null);
					compileTime = (String)infoClass.getMethod("getCompileTime").invoke(null);
					licence = (String)infoClass.getMethod("getLicence").invoke(null);
					dependsOnCompiled = (String)infoClass.getMethod("getDependsOnCompiled").invoke(null);
					dependsOnSource = (String)infoClass.getMethod("getDependsOnSource").invoke(null);
					dependsOnLib = (String)infoClass.getMethod("getDependsOnLib").invoke(null);
				}
				catch(Exception e) {
					_debug.info("Informationen zum Distributionspaket des Datenverteilers konnten nicht ermittelt werden", e);
				}
				final Data data = _connection.createData(infoAtg);
				data.getTextValue("Name").setText(packageName);
				data.getTextValue("Release").setText(release);
				data.getTextValue("Version").setText(revision);
				data.getTextValue("Stand").setText(compileTime);
				data.getTextValue("Lizenz").setText(licence);
				data.getTextValue("Abhängigkeiten").setText(dependsOnCompiled);
				data.getTextValue("QuellcodeAbhängigkeiten").setText(dependsOnSource);
				data.getTextValue("BibliothekAbhängigkeiten").setText(dependsOnLib);
				final ResultData result = new ResultData(_davObject, dataDescription, System.currentTimeMillis(), data);
				_debug.info("Informationen zum Distributionspaket des Datenverteilers werden publiziert", result);
				final ClientSenderInterface senderInterface = new ClientSenderInterface() {
					@Override
					public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
					}
					@Override
					public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
						return false;
					}
				};
				_connection.subscribeSource(senderInterface, result);
			}
		}
		catch(Exception e) {
			_debug.info("Informationen zum Distributionspaket des Datenverteilers konnten nicht publiziert werden", e);
		}
	}

	/**
	 * Gibt die Applikationsverwaltung zurück
	 *
	 * @return Applikationsverwaltung
	 */
	public HighLevelApplicationManager getApplicationManager() {
		return _highLevelApplicationManager;
	}

	/**
	 * Gibt die Verwaltung für andere Datenverteilerverbindungen zurück
	 *
	 * @return Datenverteiler-Verwaltung
	 *
	 * @throws IllegalStateException wenn noch keine verbindung Konfiguration besteht und deswegen noch keine Datenverteiler-Datenverteiler-Verbindungen vorgesehen
	 *                               sind.
	 */
	public HighLevelTransmitterManager getTransmitterManager() {
		if(_highLevelTransmitterManager == null) {
			throw new IllegalStateException(
					"Der HighLevelTransmitterManager wurde noch nicht initialisiert, weil noch keine Konfiguration verbunden ist."
			);
		}
		return _highLevelTransmitterManager;
	}

	/**
	 * Wird aufgerufen, wenn die Verbindung zu einer Applikation terminiert wurde. Hierbei werden verbliebene Anmeldungen entfernt und verschiedene Aufräumarbeiten erledigt.
	 *
	 * @param communication Applikationsverbindung
	 */
	@Override
	public void removeConnection(final T_A_HighLevelCommunication communication) {
		_lowLevelConnectionsManager.removeConnection(communication);
		_telegramManager.getSubscriptionsManager().removeAllSubscriptions(communication);
	}

	/**
	 * Wird aufgerufen, wenn die Verbindung zu einem datenverteiler terminiert wurde. Hierbei werden verbliebene Anmeldungen entfernt und verschiedene Aufräumarbeiten erledigt.
	 *
	 * @param communication Datenverteiler-Verbindung
	 */
	@Override
	public void removeConnection(final T_T_HighLevelCommunication communication) {
		_lowLevelConnectionsManager.removeConnection(communication);
		_telegramManager.getSubscriptionsManager().removeAllSubscriptions(communication);
	}

	/**
	 * Gibt die ID der Konfiguration mit der gegebenen Pid zurück
	 *
	 * @param configurationPid Die Pid der Konfiguration
	 *
	 * @return die Id der Konfiguration oder -1 falls kein Objekt gefunden werden konnte
	 *
	 * @throws IllegalStateException falls noch keine Verbindung zur Konfiguration besteht
	 */
	@Override
	public long getConfigurationId(final String configurationPid) {
		final Object[] objects = _lowLevelConnectionsManager.getServerDavParameters().getLocalModeParameter();
		if(objects != null) {
			final String _configurationPid = (String)objects[0];
			if(configurationPid.equals(_configurationPid)) {
				return (Long)objects[1];
			}
		}
		if(CommunicationConstant.LOCALE_CONFIGURATION_PID_ALIASE.equals(configurationPid)) {
			if(_dataModel == null) {
				throw new IllegalStateException("Konfiguration ist noch nicht angemeldet.");
			}
			return _dataModel.getConfigurationAuthorityId();
		}

		if(_dataModel != null) {
			final SystemObject configuration = _dataModel.getObject(configurationPid);
			if(configuration != null) {
				return configuration.getId();
			}
		}
		return -1L;
	}

	/** @return Gibt die Typ-Pid des lokalen Applikationsobjektes zurück */
	@Override
	public String getTransmitterTypePid() {
		return _transmitterTypePid;
	}

	/** @return Gibt den Namen der lokalen Applikation zurück */
	@Override
	public String getTransmitterApplicationName() {
		return _transmitterApplicationName;
	}

	/** @return Gibt die Id dieses Datenverteilers zurück */
	@Override
	public long getTransmitterId() {
		return _lowLevelConnectionsManager.getTransmitterId();
	}

	/** @return Gibt den Benutzernamen zurück, der z.B. bei der Authentifizierung bei anderen Datenverteilern benutzt wird */
	@Override
	public String getUserName() {
		return _userName;
	}

	/** @return Gibt das Passwort zurück, das z.B. für die Authentifizierung bei anderen Datenverteilern benutzt wird */
	@Override
	public String getUserPassword() {
		return _userPassword;
	}

	/**
	 * Gibt das gespeicherte Passwort für einen bestimmten Benutzer aus der Passwort-Datei zurück
	 *
	 * @param userName Benutzername
	 *
	 * @return Passwort oder null falls kein Passwort für diesen benutzer ermittelt werden konnte
	 */
	@Override
	public String getStoredPassword(final String userName) {
		return _lowLevelConnectionsManager.getServerDavParameters().getStoredPassword(userName);
	}

	/**
	 * Gibt das gewicht zwischen der Verbindung zwischen diesem Datenverteiler und einem anderen direkt verbundenen Datenverteiler zurück.
	 *
	 * @param transmitterId ID des anderen Datenverteilers
	 *
	 * @return Gewicht
	 */
	@Override
	public short getWeight(final long transmitterId) {
		return _lowLevelConnectionsManager.getWeight(transmitterId);
	}

	/**
	 * Gibt das TransmitterConnectionInfo-Objekt zu einem Datenverteiler zurück
	 *
	 * @param connectedTransmitterId Verbundener Datenverteiler
	 *
	 * @return Verbindungsinformationen
	 */
	@Override
	public TransmitterConnectionInfo getTransmitterConnectionInfo(final long connectedTransmitterId) {
		return _lowLevelConnectionsManager.getTransmitterConnectionInfo(connectedTransmitterId);
	}

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung vom angegebenen Datenverteiler zu diesem Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	@Override
	public TransmitterConnectionInfo getRemoteTransmitterConnectionInfo(final long connectedTransmitterId) {
		return _lowLevelConnectionsManager.getRemoteTransmitterConnectionInfo(connectedTransmitterId);
	}

	/**
	 * Gibt den TelegramManager zurück
	 *
	 * @return TelegramManager
	 */
	@Override
	public TelegramManager getTelegramManager() {
		return _telegramManager;
	}

	/**
	 * Bestimmt anhand einer Id die Verbindung dieser angemeldeten Applikation
	 *
	 * @param applicationId ID
	 *
	 * @return Applikationsverbindung oder null falls die Applikation nicht verbunden ist bzw. noch keine Id zugewiesen bekommen hat.
	 */
	@Override
	public T_A_HighLevelCommunication getApplicationConnectionFromId(final long applicationId) {
		return _lowLevelConnectionsManager.getApplicationConnection(applicationId);
	}

	/**
	 * Bestimmt anhand einer Id die Verbindung diesem angemeldeten Datenverteiler
	 *
	 * @param transmitterId ID
	 *
	 * @return Datenverteilerverbindung oder null falls der Datenverteiler nicht verbunden ist bzw. noch keine Id zugewiesen bekommen hat.
	 */
	@Override
	public T_T_HighLevelCommunication getTransmitterConnectionFromId(final long transmitterId) {
		return _lowLevelConnectionsManager.getTransmitterConnection(transmitterId);
	}

	/**
	 * Gibt alle Applikationsverbindungen zurück
	 *
	 * @return alle Applikationsverbindungen
	 */
	@Override
	public Collection<T_A_HighLevelCommunication> getAllApplicationConnections() {
		return _lowLevelConnectionsManager.getApplicationConnections();
	}

	/**
	 * Gibt alle Datenverteilerverbindungen zurück
	 *
	 * @return alle Datenverteilerverbindungen
	 */
	@Override
	public Collection<T_T_HighLevelCommunication> getAllTransmitterConnections() {
		return _lowLevelConnectionsManager.getTransmitterConnections();
	}

	/**
	 * Beendet den Datenverteiler
	 *
	 * @param isError Zum signalisieren, dass ein Fehler aufgetreten ist: true, sonst false
	 * @param message Nach Bedarf eine Fehlermeldung o.ä. zur Ursache des Terminierungsbefehls
	 */
	@Override
	public void shutdown(final boolean isError, final String message) {
		_lowLevelConnectionsManager.shutdown(isError, message);
	}

	/**
	 * Gibt eine Liste mit den per Kommandozeile festgelegten Zugriffssteuerungs-Plugin-Klassennamen zurück
	 *
	 * @return Liste mit den Zugriffssteuerungs-Plugin-Klassennamen
	 */
	@Override
	public List<String> getAccessControlPluginsClassNames() {
		return _lowLevelConnectionsManager.getServerDavParameters().getAccessControlPlugins();
	}

	@Override
	public ConfigurationObject getDavObject() {
		return _davObject;
	}

	@Override
	public long[] getPotentialCentralDistributors(final BaseSubscriptionInfo baseSubscriptionInfo) {
		if(_highLevelTransmitterManager == null) return new long[0];
		return _highLevelTransmitterManager.getPotentialCentralDistributors(baseSubscriptionInfo);
	}

	@Override
	public T_T_HighLevelCommunicationInterface getBestConnectionToRemoteDav(final long remoteDav) {
		if(_highLevelTransmitterManager == null) return null;
		return _highLevelTransmitterManager.getBestConnectionToRemoteDav(remoteDav);
	}

	@Override
	public void updateListsNewLocalSubscription(final BaseSubscriptionInfo baseSubscriptionInfo) {
		_listsManager.addInfo(baseSubscriptionInfo);
	}

	@Override
	public void updateListsRemovedLocalSubscription(final BaseSubscriptionInfo baseSubscriptionInfo) {
		_listsManager.removeInfo(baseSubscriptionInfo);
	}

	@Override
	public boolean isClosing() {
		return _lowLevelConnectionsManager.isClosing();
	}

	@Override
	public HighLevelSubscriptionsManager getSubscriptionsManager() {
		return getTelegramManager().getSubscriptionsManager();
	}

	public void userLoggedIn(final long userId) {

		if(_telegramManager != null){
			_telegramManager.getSubscriptionsManager().initializeUser(userId);
		}
	}
}
