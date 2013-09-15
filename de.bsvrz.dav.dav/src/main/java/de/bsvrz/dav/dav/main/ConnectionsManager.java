/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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

import de.bsvrz.dav.daf.communication.lowLevel.AuthentificationProcess;
import de.bsvrz.dav.daf.communication.lowLevel.ConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.LowLevelCommunication;
import de.bsvrz.dav.daf.communication.lowLevel.ParameterizedConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.ServerConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.AttributeGroupAspectCombination;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveUnsubscriptionTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendUnsubscriptionTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscriptionReceipt;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsDeliveryUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsSubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsUnsubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterListsUpdate;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionsConstants;
import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.main.ConnectionException;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.InconsistentLoginException;
import de.bsvrz.dav.daf.main.InitialisationNotCompleteException;
import de.bsvrz.dav.daf.main.MissingParameterException;
import de.bsvrz.dav.daf.main.NormalCloser;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.SystemTerminator;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.ClientApplication;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DynamicObject;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.main.impl.ConfigurationManager;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo;
import de.bsvrz.dav.dav.communication.accessControl.AccessControlPlugin;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.util.accessControl.AccessControlManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.dav.dav.util.accessControl.UserRightsChangeHandler;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Diese Komponente startet alle notwendigen Aktionen, um eine bestimmte Aufgabe zu erledigen durch den Aufruf einer ihrer Methoden. Die Aktionen werden
 * natürlich nur durchgeführt, wenn der Initiator auch eine Berechtigung dafür hat. Die Berechtigung entnimmt man der Initiatorrolle. Wenn die Rolle nicht lokal
 * vorhanden ist, wird sie aus der Konfiguration ausgelesen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10161 $
 */
public class ConnectionsManager implements ConnectionsManagerInterface, DistributionInterface, UserRightsChangeHandler {

	private static final Debug _debug = Debug.getLogger();

	private static final short DEFAULT_WEIGHT = 1;

	/** Die Startparameters dieses Datenverteilers */
	private final ServerDavParameters _serverDavParameters;

	/** Die Startparameters dieses Datenverteilers als Applikation */
	private final ClientDavParameters _clientDavParameters;

	/** Die Serverkommunikationskomponente der Datenverteiler, die Applikationsverbindungen akzeptiert. */
	private final ServerConnectionInterface _applicationsServerConnection;

	/** Die Serverkommunikationskomponente der Datenverteiler, die Datenverteilersverbindungen akzeptiert. */
	private ServerConnectionInterface _transmittersServerConnection;

	/** Der Authentifikationskomponente */
	private final AuthentificationComponent _authentificationComponent;

	/** Interne Thread zur Kommunikation */
	private TransmitterConnectionsSubscriber _transmitterConnectionsSubscriber;

	/** Interne Thread zur Kommunikation */
	private ApplicationConnectionsSubscriber _applicationConnectionsSubscriber;

	/** Liste der Datenverteilerverbindungen */
	private List<T_T_HighLevelCommunication> _transmitterConnections;

	/** Liste der Applikationsverbindungen */
	private final List<T_A_HighLevelCommunication> _applicationConnections = new CopyOnWriteArrayList<T_A_HighLevelCommunication>();

	/**
	 * Dieser Thread verschickt die Statusinformationen über angemeldete Applikationen. Nur wenn alle benötigten ATG: _pidAngemeldeteApplikationen,
	 * _pidAngemeldeteDatenidentifikationen, _pidTelegrammLaufzeiten vorhanden sind, wird der Thread initialisiert und gestartet.
	 * <p/>
	 * Ist die Variable <code>null</code>, so wurde der Thread nicht initialisiert(die genannten ATG´s fehlen) und der Mechanismus steht nicht zur Verfügung.
	 */
	private ApplicationStatusUpdater _applicationStatusUpdater = null;

	/** Liste der auf eine positive Quitung wartenden Anmeldungen, die über einen neuen weg umgeleitet werden müssen. */
	private ArrayList<ExchangeSubscriptionAction> _pendingSubscriptionToRedirect;

	/** Information ob die Konfigurationsverbindung erfolgreich ist. */
	private boolean _configurationAvaillable;

	/** Die Id des Datenverteilers */
	private final long _transmitterId;

	/** Die Netztopologie dieses Datenverteilers */
	private TransmitterConnectionInfo _transmitterConnectionInfos[];

	/** Die Liste der nicht etablierten Verbindungen */
	private Vector<TransmitterConnectionInfo> missedConnectionInfos;

	/** Überwacht die Nachbarnverbindungen und leitet Ersatzverbindungen wenn nötig */
	private TransmitterConnectionsMonitor _connectionsMonitor;

	/** Interne Variable (Closing state) */
	private volatile boolean _closing = false;

	/** Die eigene DAF-Verbindung */
	ClientDavConnection selfClientDavConnection;

	/** Das DataModel zur Konfigurationsabfrage */
	DataModel dataModel = null;

	/** Der Anmeldemanager */
	private final SubscriptionsManager subscriptionsManager;

	/** Der Objekte-Attributesgruppen-Aspekte-Listen-Manager */
	private final ListsManager listsManager;

	/** Der Bestwege-Manager */
	private final BestWayManager bestWayManager;

	/** Der Cache Manager */
	final CacheManager cacheManager;

	/** Der Authentifikationsmanager */
	final AuthentificationManager authentificationManager;

	/** Die ID des Konfigurationsverantwortlichen der Konfiguration. */
	private long _configurationId;

	/**
	 * ATG, die benötigt wird um einen Datensatz zu verschicken, der alle angemeldeten Applikationen enthält. Ist diese ATG nicht vorhanden, so wird der
	 * Datenverteiler gestartet ohne diese Funktionalität zur Verfügung zu stellen.
	 */
	private static final String _pidAngemeldeteApplikationen = "atg.angemeldeteApplikationen";

	/**
	 * ATG, die benötigt wird um einen Datensatz zu verschicken, der die angemeldeten Daten aller angemeldeten Applikationen enthält. Ist diese ATG nicht
	 * vorhanden, so wird der Datenverteiler gestartet ohne diese Funktionalität zur Verfügung zu stellen.
	 */
	private static final String _pidAngemeldeteDatenidentifikationen = "atg.angemeldeteDatenidentifikationen";

	/**
	 * ATG, die benötigt wird um einen Datensatz zu verschicken, der die Telegrammlaufzeiten aller angemeldeten Applikationen enthält. Ist diese ATG nicht
	 * vorhanden, so wird der Datenverteiler gestartet ohne diese Funktionalität zur Verfügung zu stellen.
	 */
	private static final String _pidTelegrammLaufzeiten = "atg.telegrammLaufzeiten";

	/**
	 * ATG, die benötigt wird um Terminierungsanfragen für Applikationen und Dav-Verbindungen zu erhalten. Ist diese ATG nicht vorhanden, so wird der
	 * Datenverteiler gestartet ohne diese Funktionalität zur Verfügung zu stellen.
	 */
	private static final String _pidTerminierung = "atg.terminierung";

	/** Klasse die die Zugriffsrechte der Onlinedaten verwaltet */
	private AccessControlManager _accessControlManager;

	private final Map<Long, List<AccessControlPlugin>> _pluginFilterMap = new ConcurrentHashMap<Long, List<AccessControlPlugin>>(16, 0.75f, 1);

	private volatile boolean _pluginFilterMapInitialized = false;

	private volatile DavTransactionManager _davTransactionManager = null;

	private DavDavRequester _davRequestManager;

	private boolean _waitForParamDone = false;

	private Object _waitForParamLock = new Object();

	/** Dieser Konstruktor wird für Tests benötigt. */
	public ConnectionsManager() {
		subscriptionsManager = null;
		listsManager = null;
		bestWayManager = null;
		cacheManager = null;
		authentificationManager = null;
		_transmitterId = -12;
		_serverDavParameters = null;
		_clientDavParameters = null;
		_applicationsServerConnection = null;
		_authentificationComponent = null;
	}

	/**
	 * Gibt den Transaktions-Manager zurück
	 * @return Transaktions-Manager oder null falls noch nicht initialisiert
	 */
	public DavTransactionManager getDavTransactionManager() {
		return _davTransactionManager;
	}

	/**
	 * Erzeugt eine neue Verbindungsverwaltung für den Datenverteiler.
	 *
	 * @param serverDavParameters Die Parameter sind u. a. die Adressen und Subadressen der Kommunikationskanäle
	 *
	 * @throws CommunicationError         wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind
	 * @throws MissingParameterException  wenn notwendige Verbindungsparameter nicht spezifiziert wurden
	 * @throws InconsistentLoginException bei einem fehlgeschlagenen Authentifizierungsversuch
	 * @throws ConnectionException        wenn eine Verbindung nicht aufgebaut werden konnte
	 */
	public ConnectionsManager(ServerDavParameters serverDavParameters)
			throws CommunicationError, MissingParameterException, InconsistentLoginException, ConnectionException {

		// Initialisation
		_serverDavParameters = serverDavParameters;
		_clientDavParameters = _serverDavParameters.getClientDavParameters();
		_transmitterId = _serverDavParameters.getDataTransmitterId();

		_transmitterConnections = new CopyOnWriteArrayList<T_T_HighLevelCommunication>();
		missedConnectionInfos = new Vector<TransmitterConnectionInfo>();
		_configurationAvaillable = false;
		subscriptionsManager = new SubscriptionsManager(this);
		listsManager = new ListsManager(this);
		bestWayManager = new BestWayManager(this.getTransmitterId(), this, listsManager);
		listsManager.setBestWayManager(bestWayManager);
		cacheManager = new CacheManager();
		authentificationManager = new AuthentificationManager(this);
		_pendingSubscriptionToRedirect = new ArrayList<ExchangeSubscriptionAction>();

		try {
			// Get the low level protocoll
			String communicationProtocolName = _serverDavParameters.getLowLevelCommunicationName();
			if(communicationProtocolName == null) {
				throw new InitialisationNotCompleteException("Kommunikationsprotokollname ungültig.");
			}
			Class<? extends ServerConnectionInterface> communicationProtocolClass = Class.forName(communicationProtocolName).asSubclass(
					ServerConnectionInterface.class
			);
			if(communicationProtocolClass == null) {
				throw new InitialisationNotCompleteException("Kommunikationsprotokollname ungültig.");
			}

			//Start the listener at the application port
			_applicationsServerConnection = communicationProtocolClass.newInstance();
			// Falls vorhanden und möglich Parameter für das Kommunikationsinterface weitergeben
			final String communicationParameters = _serverDavParameters.getLowLevelCommunicationParameters();
			if(communicationParameters.length() != 0 && _applicationsServerConnection instanceof ParameterizedConnectionInterface) {
				ParameterizedConnectionInterface parameterizedConnectionInterface = (ParameterizedConnectionInterface)_applicationsServerConnection;
				parameterizedConnectionInterface.setParameters(communicationParameters);
			}
			_applicationsServerConnection.connect(_serverDavParameters.getApplicationConnectionsSubAddress());

			// Get the authentification process
			String authentificationName = _serverDavParameters.getAuthentificationProcessName();
			if(authentificationName == null) {
				throw new InitialisationNotCompleteException("Unbekanntes Authentifikationsverfahren.");
			}
			Class<? extends AuthentificationProcess> authentificationClass = Class.forName(authentificationName).asSubclass(AuthentificationProcess.class);
			if(authentificationClass == null) {
				throw new InitialisationNotCompleteException("Unbekanntes Authentifikationsverfahren.");
			}
			AuthentificationProcess authentificationProcess = authentificationClass.newInstance();
			_authentificationComponent = new AuthentificationComponent(authentificationProcess);

			// Start the application connections receiver thread
			_applicationConnectionsSubscriber = new ApplicationConnectionsSubscriber();
			_applicationConnectionsSubscriber.start();

			// Start the client application of this transmitter
			selfClientDavConnection = new ClientDavConnection(_clientDavParameters);
			long waitingTime = 0, startTime = System.currentTimeMillis();
			long maxWaitingTime = CommunicationConstant.MAX_WAITING_TIME_FOR_CONNECTION;
			while(waitingTime < maxWaitingTime) {
				try {
					selfClientDavConnection.connect();
					selfClientDavConnection.setCloseHandler(new NormalCloser());
					// try to login. if login successfull then a configuration is found
					// otherwise keep trying till the threshold time is overdue
					selfClientDavConnection.login();
					dataModel = selfClientDavConnection.getDataModel();
					_configurationId = selfClientDavConnection.getLocalConfigurationAuthority().getId();
					_configurationAvaillable = true;
					break;
				}
				catch(CommunicationError ex) {
					_debug.warning("Es konnte keine Verbindung zur Konfiguration hergestellt werden", ex);
					try {
						selfClientDavConnection.disconnect(false, "");
						Thread.sleep(CommunicationConstant.SLEEP_TIME_WAITING_FOR_CONNECTION);
						waitingTime = System.currentTimeMillis() - startTime;
					}
					catch(InterruptedException e) {
						close(true, e.getMessage());
						return;
					}
				}
				catch(ConfigurationException ex) {
					_debug.error("Es konnte keine Verbindung zur Konfiguration hergestellt werden", ex);
					try {
						selfClientDavConnection.disconnect(false, "");
						Thread.sleep(CommunicationConstant.SLEEP_TIME_WAITING_FOR_CONNECTION);
						waitingTime = System.currentTimeMillis() - startTime;
					}
					catch(InterruptedException e) {
						close(true, e.getMessage());
						return;
					}
				}
			}
			// if no configuration is found then close everything.
			if(waitingTime > maxWaitingTime) {
				close(true, "Zeitgrenze überschritten. Es konnte keine Konfigurationsverbindung aufgebaut werden");
				return;
			}

			// Otherwise tell every waiting connection that they can continue there authentification process.

			selfClientDavConnection.setCloseHandler(new SystemTerminator());

			// Prüfen, ob eine gültige Datenverteiler-ID angegeben wurde

			final SystemObject davObject = dataModel.getObject(_transmitterId);
			if(davObject == null || !davObject.isOfType("typ.datenverteiler")) {
				final StringBuilder message = new StringBuilder();
				message.append("Das Konfigurationsobjekt zu der via Aufrufparameter -datenverteilerId= angegebenen Objekt-ID ");
				message.append(_transmitterId);
				message.append(" ");
				if(davObject == null) {
					message.append("wurde nicht gefunden");
				}
				else {
					message.append("ist nicht vom typ.datenverteiler, sondern vom ").append(davObject.getType().getPidOrNameOrId());
				}
				message.append(".\nFolgende Datenverteiler sind der Konfiguration bekannt:\n");
				final SystemObjectType davType = dataModel.getType("typ.datenverteiler");
				final List<SystemObject> davs = davType.getElements();
				Formatter formatter = new Formatter();
				formatter.format("%40s %22s %s\n", "PID", "ID", "NAME");
				for(SystemObject dav : davs) {
					formatter.format("%40s %22d %s\n", dav.getPid(), dav.getId(), dav.getName());
				}
				message.append(formatter.toString());
				_debug.error(message.toString());
				try {
					Thread.sleep(2000);
				}
				catch(InterruptedException ignored) {
				}
				throw new IllegalArgumentException("Ungültige Datenverteiler-ID " + _transmitterId);
			}

			_accessControlManager = authentificationManager.initializeAccessControlManager(selfClientDavConnection);

			_davTransactionManager = new DavTransactionManager(selfClientDavConnection, subscriptionsManager);

			_davRequestManager = new DavDavRequester(selfClientDavConnection, _davTransactionManager);

			initializeAccessControlPlugins(_serverDavParameters.getAccessControlPlugins());

			_debug.fine("Verbindungsaufbau für normale Applikationen ermöglichen");

			if(dataModel.getObject(_pidAngemeldeteApplikationen) == null || dataModel.getObject(_pidAngemeldeteDatenidentifikationen) == null
			   || dataModel.getObject(_pidTelegrammLaufzeiten) == null) {
				// Eine ATG fehlt -> Der Status der angemeldeten Applikationen kann nicht zur Verfügung gestellt werden.
				_debug.warning(
						"Eine Attributgruppe zum Versenden des Status einer Applikation ist nicht vorhanden. Der Status der Applikationen kann nicht zur Verfügung gestellt werden, der Datenverteiler wird ohne diese Funktion gestartet. Für diese Funktion sind folgende Attributgruppen notwendig: "
						+ _pidAngemeldeteApplikationen + " " + _pidAngemeldeteDatenidentifikationen + " " + _pidTelegrammLaufzeiten + " ."
				);
			}
			else {
				_applicationStatusUpdater = new ApplicationStatusUpdater();
				_applicationStatusUpdater.start();
			}

			if(dataModel.getObject(_pidTerminierung) != null) {
				initializeTerminationQueries();
			}
			else {
				_debug.warning(
						"Die Attributgruppe zum Empfangen von Terminierungsanfragen ist nicht vorhanden. Der Datenverteiler wird ohne diese Funktion gestartet. Für diese Funktion ist der Bereich kb.systemModellGlobal in Version 24 oder größer notwendig."
				);
			}

			synchronized(_applicationConnections) {
				for(int i = 0; i < _applicationConnections.size(); ++i) {
					T_A_HighLevelCommunicationInterface connection = _applicationConnections.get(
							i
					);
					if(connection != null) {
						connection.continueAuthentification();
					}
				}
			}

			// Informationen zum Distributionspaket des Datenverteilers werden publiziert
			try {
				final AttributeGroup infoAtg = dataModel.getAttributeGroup("atg.distributionspaketReleaseInfo");
				if(infoAtg == null) {
					_debug.info(
							"Informationen zum Distributionspaket des Datenverteilers können nicht publiziert werden, weil das Datenmodell "
							+ "(kb.systemModellGlobal) noch nicht aktualisiert wurde."
					);
				}
				else {
					final Aspect aspect = dataModel.getAspect("asp.standard");
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
					final Data data = selfClientDavConnection.createData(infoAtg);
					data.getTextValue("Name").setText(packageName);
					data.getTextValue("Release").setText(release);
					data.getTextValue("Version").setText(revision);
					data.getTextValue("Stand").setText(compileTime);
					data.getTextValue("Lizenz").setText(licence);
					data.getTextValue("Abhängigkeiten").setText(dependsOnCompiled);
					data.getTextValue("QuellcodeAbhängigkeiten").setText(dependsOnSource);
					data.getTextValue("BibliothekAbhängigkeiten").setText(dependsOnLib);
					final ResultData result = new ResultData(davObject, dataDescription, System.currentTimeMillis(), data);
					_debug.info("Informationen zum Distributionspaket des Datenverteilers werden publiziert", result);
					final ClientSenderInterface senderInterface = new ClientSenderInterface() {
						public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
						}
						public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
							return false;
						}
					};
					selfClientDavConnection.subscribeSource(senderInterface, result);
				}
			}
			catch(Exception e) {
				_debug.info("Informationen zum Distributionspaket des Datenverteilers konnten nicht publiziert werden", e);
			}

			if(_serverDavParameters.getWaitForParamApp()) waitForParamReady(_serverDavParameters.getParamAppIncarnationName());

			try {
				final long delay = _serverDavParameters.getInitialInterDavServerDelay();
				_debug.config("Verbindungen von und zu anderen Datenverteilern werden in " + delay + " ms zugelassen");
				Thread.sleep(delay);
			}
			catch(InterruptedException e) {
				throw new RuntimeException(e); 
			}

			_debug.finer("Datenverteiler-ID", _transmitterId);
			_debug.config("Verbindungen von und zu anderen Datenverteilern werden jetzt zugelassen");

			try {
				_serverDavParameters.setConfigurationId(_configurationId);
//				serverDavParameters.setConfigurationId(dataModel.getConfigurationAuthorityId());
				// Get the net topologie
				ConfigurationManager configurationManager = ((DafDataModel)dataModel).getConfigurationManager();
				if(configurationManager != null) {
					int davDavSubadress = -1;
					_transmitterConnectionInfos = configurationManager.getTransmitterConnectionInfo(_transmitterId);
					for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
						_debug.finer("Datenverteilerverbindung", _transmitterConnectionInfos[i].parseToString());
						if(_transmitterConnectionInfos[i] != null) {
							TransmitterInfo transmitterInfo_1 = _transmitterConnectionInfos[i].getTransmitter_1();
							TransmitterInfo transmitterInfo_2 = _transmitterConnectionInfos[i].getTransmitter_2();
							long id1 = transmitterInfo_1.getTransmitterId();
							long id2 = transmitterInfo_2.getTransmitterId();
							if(id1 == id2) {
								close(true, "Inkonsistente Netztopologie (Verbindung von Datenverteiler[" + id1 + "] zu sich selbst");
							}
							int subAdresse1 = transmitterInfo_1.getSubAdress();
							int subAdresse2 = transmitterInfo_2.getSubAdress();
							for(int j = i + 1; j < _transmitterConnectionInfos.length; ++j) {
								if(_transmitterConnectionInfos[j] != null) {
									long tmpId1 = _transmitterConnectionInfos[j].getTransmitter_1().getTransmitterId();
									long tmpId2 = _transmitterConnectionInfos[j].getTransmitter_2().getTransmitterId();
									if((id1 == tmpId1) && (id2 == tmpId2)) {
										close(
												true,
												"Inkonsistente Netztopologie (Mehrfache Verbindung zwichen Datenverteiler[" + id1 + "] und Datenverteiler["
												+ id2 + "] möglich)"
										);
									}
								}
							}
							if(id1 == _transmitterId) {
								if(davDavSubadress == -1) {
									davDavSubadress = subAdresse1;
								}
								else if(davDavSubadress != subAdresse1) {
									close(
											true, "Inkonsistente Netztopologie (Es wurden dem Datenverteiler[" + id1 + "] verschiedene Subadressen zugewiesen"
									);
								}
							}
							if(id2 == _transmitterId) {
								if(davDavSubadress == -1) {
									davDavSubadress = subAdresse2;
								}
								else if(davDavSubadress != subAdresse2) {
									close(
											true, "Inkonsistente Netztopologie (Es wurden dem Datenverteiler[" + id2 + "] verschiedene Subadressen zugewiesen"
									);
								}
							}
						}
					}

					if(davDavSubadress == -1) {
						davDavSubadress = this._serverDavParameters.getTransmitterConnectionsSubAddress();
					}
					//Start the listener at the transmitter port
					_transmittersServerConnection = communicationProtocolClass.newInstance();
					// Falls vorhanden und möglich Parameter für das Kommunikationsinterface weitergeben
					final String communicationParameters2 = _serverDavParameters.getLowLevelCommunicationParameters();
					if(communicationParameters2.length() != 0 && _transmittersServerConnection instanceof ParameterizedConnectionInterface) {
						ParameterizedConnectionInterface parameterizedConnectionInterface = (ParameterizedConnectionInterface)_transmittersServerConnection;
						parameterizedConnectionInterface.setParameters(communicationParameters2);
					}

					int subAddressToListenFor = davDavSubadress;
					if(subAddressToListenFor == 100000) {
						// Zu Testzwecken wird die Portnummer mit dem Wert 100000 serverseitig durch 8088 und clientseitig durch 8081 ersetzt
						subAddressToListenFor = 8088;
					}

					subAddressToListenFor += _serverDavParameters.getTransmitterConnectionsSubAddressOffset();

					_transmittersServerConnection.connect(subAddressToListenFor);
					// Start the transmitter connections receiver thread
					_transmitterConnectionsSubscriber = new TransmitterConnectionsSubscriber();
					_transmitterConnectionsSubscriber.start();

					connectToNeighbours();
					_connectionsMonitor = new TransmitterConnectionsMonitor();
					_connectionsMonitor.start();
				}
			}
			catch(RuntimeException ex) {
				close(true, ex.getMessage());
			}
		}
		catch(ClassNotFoundException ex) {
			throw new InitialisationNotCompleteException("Fehler beim Erzeugen der Datenverteilerkommunikation", ex);
		}
		catch(InstantiationException ex) {
			throw new InitialisationNotCompleteException("Fehler beim Erzeugen der Datenverteilerkommunikation", ex);
		}
		catch(IllegalAccessException ex) {
			throw new InitialisationNotCompleteException("Fehler beim Erzeugen der Datenverteilerkommunikation", ex);
		}
		catch(InterruptedException e) {
			throw new InitialisationNotCompleteException("Fehler beim Erzeugen der Datenverteilerkommunikation", e);
		}
	}

	/**
	 * Wartet auf die Applikationsfertigmeldung der lokalen Parametrierung.
	 * @param paramAppIncarnationName Inkarnationsname der Parametrierung deren Applikationsfertigmeldung abgewartet werden soll oder <code>null</code>
	 * falls auf eine beliebige Parametrierung gewartet werden soll.
	 * @throws InterruptedException
	 */
	private void waitForParamReady(final String paramAppIncarnationName) throws InterruptedException {
		synchronized(_waitForParamLock) {
			if(_waitForParamDone) return;
			DataModel config = selfClientDavConnection.getDataModel();
			AttributeGroup atg = config.getAttributeGroup("atg.applikationsFertigmeldung");
			Aspect aspect = config.getAspect("asp.standard");
			if(atg == null || aspect==null) {
				_debug.warning("Datenmodell für Applikationsfertigmeldungen nicht verfügbar. Es wird nicht auf die Parametrierung gewartet");
				_waitForParamDone = true;
				return;
			}
			DataDescription readyMessageDataDescription = new DataDescription(atg, aspect);
			if(paramAppIncarnationName == null) {
				_debug.info("Warte auf Applikationsfertigmeldung der Parametrierung mit beliebigem Inkarnationsnamen.");
			}
			else {
				_debug.info("Warte auf Applikationsfertigmeldung der Parametrierung mit Inkarnationsnamen " + paramAppIncarnationName);
			}
			while(true) {
				SystemObjectType paramAppType = config.getType("typ.parametrierungsApplikation");
				List<SystemObject> paramApps = paramAppType.getElements();
				for(SystemObject paramApp : paramApps) {
					ResultData result = selfClientDavConnection.getData(paramApp, readyMessageDataDescription, 30000);
					Data data = result.getData();
					if(data != null) {
						if(data.getTextValue("InitialisierungFertig").getValueText().equals("Ja")) {
							if(paramAppIncarnationName == null || data.getTextValue("Inkarnationsname").getValueText().equals(paramAppIncarnationName))
								_debug.info("Parametrierung ist fertig");
							_waitForParamDone = true;
							return;
						}
					}
				}
				Thread.sleep(1000);
			}
		}
	}

	/**
	 * Initialisiert die angegebenen Zugriffssteuerungs-Plugins, die den Datenverkehr filtern und so zusätzliche Rechteprüfungen vornehmen können
	 *
	 * @param classNames Liste mit Plugin-Klassen-Namen
	 */
	private void initializeAccessControlPlugins(final List<String> classNames) {

	/** Liste mit Plugins, die den Datenverkehr abfangen und filtern um die Zugriffsrechte zu prüfen */
	final List<AccessControlPlugin> accessControlPluginList = new ArrayList<AccessControlPlugin>();
		for(String pluginClassName : classNames) {
			try {
				final Class<?> aClass = Class.forName(pluginClassName);
				final Class<? extends AccessControlPlugin> pluginClass = aClass.asSubclass(AccessControlPlugin.class);
				final AccessControlPlugin plugin = pluginClass.newInstance();
				plugin.initialize(_accessControlManager, selfClientDavConnection);
				accessControlPluginList.add(plugin);
				_debug.info("Zugriffssteuerungs-Plugin wurde geladen: " + pluginClassName);
			}
			catch(ClassNotFoundException e) {
				_debug.warning("Konnte Plugin nicht finden: " + pluginClassName, e);
			}
			catch(ClassCastException e) {
				_debug.warning("Klasse implementiert Plugin-Interface nicht: " + pluginClassName, e);
			}
			catch(InstantiationException e) {
				_debug.warning("Konnte Plugin-Instanz nicht erstellen (Pluginklasse darf nicht abstrakt oder Interface sein): " + pluginClassName, e);
			}
			catch(IllegalAccessException e) {
				_debug.warning("Konnte Plugin-Instanz nicht erstellen (Konstruktor nicht öffentlich?): " + pluginClassName, e);
			}
		}
		final Map<Long, List<AccessControlPlugin>> temporaryMap = new HashMap<Long, List<AccessControlPlugin>>();

		for(AccessControlPlugin plugin : accessControlPluginList) {
			for(AttributeGroupUsage attributeGroupUsage : plugin.getAttributeGroupUsagesToFilter()) {
				final List<AccessControlPlugin> entry = temporaryMap.get(attributeGroupUsage.getId());
				if(entry == null) {
					final ArrayList<AccessControlPlugin> newEntry = new ArrayList<AccessControlPlugin>();
					newEntry.add(plugin);
					temporaryMap.put(attributeGroupUsage.getId(), newEntry);
				}
				else {
					entry.add(plugin);
				}
			}
		}

		_pluginFilterMap.putAll(temporaryMap);
	}

	/**
	 * Gibt eine Map zurück, die als Key die AttributeGroupUsage-ID speichert und als Value alle zuständigen Plugins in einer Liste enthält
	 *
	 * @return eine unveränderliche Map vom Typ <code>Map<Long, List<AccessControlPluginInterface>></code> (leer falls die Rechteprüfung deaktivert ist).
	 */
	public Map<Long, List<AccessControlPlugin>> getPluginFilterMap() {
		if(!getServerDavParameters().isUserRightsCheckingEnabled()) return Collections.emptyMap();
		return Collections.unmodifiableMap(_pluginFilterMap);
	}

	public DataModel getDataModel() {
		return dataModel;
	}

	public final long getTransmitterId() {
		return _transmitterId;
	}

	/** Verbindet mit den Nachbarndatenverteiler */
	private void connectToNeighbours() {
		if(_transmitterConnectionInfos == null) {
			return;
		}
		synchronized(missedConnectionInfos) {
			for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
				if(_transmitterConnectionInfos[i].isExchangeConnection()) {
					continue;
				}
				if(!_transmitterConnectionInfos[i].isActiveConnection()) {
					continue;
				}
				TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
				if(t1.getTransmitterId() == _transmitterId) {
					if(!connectToMainTransmitter(_transmitterConnectionInfos[i])) {
						connectToAlternativeTransmitters(_transmitterConnectionInfos[i]);
						missedConnectionInfos.add(_transmitterConnectionInfos[i]);
					}
				}
			}
		}
	}

	/**
	 * Startet den Verbindungsaufbau zwischen zwei direkt benachbarten Datenverteilern. Beim Verbindungsaufbau zwischen zwei DAV werden durch die Angabe der beiden
	 * Kommunikationspartner, die Wichtung der Verbindung, die Angabe, welche(r) Datenverteiler die Verbindung aufbaut und die Spezifikation von Ersatzverbindungen
	 * festgelegt, um welche Art von Verrbindung es sich handelt.
	 *
	 * @param transmitterConnectionInfo Enthält Informationen zu der Verbindungart zwischen zwei Datenverteilern.
	 *
	 * @return true: Verbindung hergestellt, false: Verbindung nicht hergestellt
	 *
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo,short,long,String)
	 */
	private boolean connectToMainTransmitter(TransmitterConnectionInfo transmitterConnectionInfo) {
		TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
		short weight = transmitterConnectionInfo.getWeight();
		long waitingTime = transmitterConnectionInfo.getConnectionTimeThreshold();
		return connectToTransmitter(t2, weight, waitingTime, transmitterConnectionInfo.getUserName());
	}

	/**
	 * Startet den Ersatzverbindungsaufbau zwischen zwei nicht direkt benachbarten Datenverteilern. Beim Verbindungsaufbau zwischen zwei DAV werden durch die
	 * Angabe der beiden Kommunikationspartner, die Wichtung der Verbindung, die Angabe, welche(r) Datenverteiler die Verbindung aufbaut und die Spezifikation von
	 * Ersatzverbindungen festgelegt, um welche Art von Verrbindung es sich handelt. Ob Ersatzverbindungen automatisch etabliert werden sollen, wird durch das
	 * autoExchangeTransmitterDetection Flag festgelegt.
	 *
	 * @param transmitterConnectionInfo Enthält Informationen zu der Verbindungart zwischen zwei Datenverteilern.
	 *
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo,short,long,String)
	 */
	private void connectToAlternativeTransmitters(TransmitterConnectionInfo transmitterConnectionInfo) {
		TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
		if(transmitterConnectionInfo.isAutoExchangeTransmitterDetectionOn()) {
			List<TransmitterConnectionInfo> infos = getInvolvedTransmitters(t2);
			for(TransmitterConnectionInfo info : infos) {
				TransmitterInfo transmitterInfo = info.getTransmitter_2();
				short weight = info.getWeight();
				long time = info.getConnectionTimeThreshold();
				if(transmitterInfo != null) {
					connectToTransmitter(transmitterInfo, weight, time, transmitterConnectionInfo.getUserName());
				}
			}
		}
		else {
			TransmitterInfo infos[] = transmitterConnectionInfo.getExchangeTransmitterList();
			if(infos != null) {
				for(TransmitterInfo info : infos) {
					TransmitterConnectionInfo tmpTransmitterConnectionInfo = null;
					for(TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
						if(_transmitterConnectionInfo.isExchangeConnection()
						   && (_transmitterConnectionInfo.getTransmitter_1().getTransmitterId() == _transmitterId) && (
								_transmitterConnectionInfo.getTransmitter_2().getTransmitterId() == info.getTransmitterId())) {
							tmpTransmitterConnectionInfo = _transmitterConnectionInfo;
							break;
						}
					}
					if(tmpTransmitterConnectionInfo != null) {
						short weight = tmpTransmitterConnectionInfo.getWeight();
						long time = tmpTransmitterConnectionInfo.getConnectionTimeThreshold();
						connectToTransmitter(info, weight, time, tmpTransmitterConnectionInfo.getUserName());
					}
				}
			}
		}
	}

	/**
	 * Startet den Verbindungsaufbau zwischen zwei Datenverteilern. Falls keine Verbindung etabliert werden konnte, wird eine entsprechende Exception gefangen
	 *
	 * @param t_info   Information zum Datenverteiler
	 * @param weight   Die Information wird von der Wegverwaltung benutzt, wenn eine Verbindung bewertet wird.
	 * @param time     Zeitspanne in der versucht werden soll eine Verbindung aufzubauen, in Millisekunden. Maximale Wartezeit eine Sekunde.
	 * @param userName Benutzername mit dem die Authentifizierung durchgeführt werden soll.
	 *
	 * @return true, wenn Verbindung hergestellt werden konnte; false, wenn Verbindung nicht hergestellt werden konnte.
	 *
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo,short,String,String)
	 */
	private boolean connectToTransmitter(TransmitterInfo t_info, short weight, long time, String userName) {
		String password;
		if(userName.equals("")) {
			userName = _serverDavParameters.getUserName();
			password = _serverDavParameters.getUserPassword();
		}
		else {
			password = _serverDavParameters.getStoredPassword(userName);
			if(password == null) {
				_debug.error(
						"Passwort des Benutzers " + userName + " konnte nicht ermittelt werden. Es wird gebraucht für Datenverteilerkopplung mit " + t_info
				);
				return false;
			}
		}
		_debug.info("Starte Datenverteilerkopplung als Benutzer " + userName + " zu ", t_info);
		_debug.finer(" time", time);
		_debug.finer(" weight", weight);
		long waitingTime = time;
		if(waitingTime < 1000) {
			waitingTime = 1000;
		}
		long startTime = System.currentTimeMillis();
		do {
			try {
				connectToTransmitter(t_info, weight, userName, password);
				return true;
			}
			catch(ConnectionException ex) {
				_debug.warning("Verbindung zum " + t_info + " konnte nicht aufgebaut werden", ex);
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return false;
				}
				waitingTime -= (System.currentTimeMillis() - startTime);
			}
			catch(CommunicationError ex) {
				_debug.warning("Verbindung zum " + t_info + " konnte nicht aufgebaut werden", ex);
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException e) {
					return false;
				}
				waitingTime -= (System.currentTimeMillis() - startTime);
			}
		}
		while(waitingTime > 0);
		return false;
	}

	/**
	 * Etabliert Verbindung zwischen zwei Datenverteilern. Falls keine Verbindung aufgebaut werden konnte, wird eine entsprechende Ausnahme geworfen.
	 *
	 * @param t_info   Informationen zum Datenverteiler.
	 * @param weight   Die Information wird von der Wegverwaltung benutzt, wenn eine Verbindung bewertet wird.
	 * @param userName Benutzername mit dem die Authentifizierung durchgeführt werden soll.
	 * @param password Passwort des Benutzers mit dem die Authentifizierung durchgeführt werden soll.
	 *
	 * @throws ConnectionException wenn eine Verbindung nicht aufgebaut werden konnte
	 * @throws CommunicationError  wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind
	 */
	private void connectToTransmitter(TransmitterInfo t_info, short weight, final String userName, final String password)
			throws ConnectionException, CommunicationError {
		if(_transmittersServerConnection == null) {
			throw new IllegalArgumentException("Die Verwaltung ist nicht richtig initialisiert.");
		}

		long tId = t_info.getTransmitterId();

		int subAddressToConnectTo = t_info.getSubAdress();
		if(subAddressToConnectTo == 100000) {
			// Zu Testzwecken wird die Portnummer mit dem Wert 100000 serverseitig durch 8088 und clientseitig durch 8081 ersetzt
			subAddressToConnectTo = 8081;
		}
		subAddressToConnectTo += _serverDavParameters.getTransmitterConnectionsSubAddressOffset();

		for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
			if(transmitterConnection != null) {
				if(transmitterConnection.getId() == tId) {
					return;
				}
				else {
					String adress = transmitterConnection.getRemoteAdress();
					int subAdress = transmitterConnection.getRemoteSubadress();
					if((adress != null) && (adress.equals(t_info.getAdress())) && (subAddressToConnectTo == subAdress)) {
						return;
					}
				}
			}
		}
		ConnectionInterface connection = _transmittersServerConnection.getPlainConnection();
		LowLevelCommunication lowLevelCommunication = new LowLevelCommunication(
				connection,
				_serverDavParameters.getDavCommunicationOutputBufferSize(),
				_serverDavParameters.getDavCommunicationInputBufferSize(),
				_serverDavParameters.getSendKeepAliveTimeout(),
				_serverDavParameters.getReceiveKeepAliveTimeout(),
				LowLevelCommunication.NORMAL_MODE,
				false
		);
		lowLevelCommunication.connect(t_info.getAdress(), subAddressToConnectTo);
		ServerConnectionProperties properties = new ServerConnectionProperties(
				lowLevelCommunication, _authentificationComponent, _serverDavParameters
		);
		T_T_HighLevelCommunication highLevelCommunication = new T_T_HighLevelCommunication(
				properties, subscriptionsManager, ConnectionsManager.this, bestWayManager, weight, !_configurationAvaillable, userName, password
		);
		highLevelCommunication.connect();
		_transmitterConnections.add(highLevelCommunication);
		highLevelCommunication.completeInitialisation();

		_debug.info("Verbindungsaufbau zum " + t_info + " war erfolgreich");
	}

	/**
	 * Erstellt ein Array, das die Informationen über die benachbarten Datenverteiler des übergebenen Datenverteilers enthält.
	 *
	 * @param t_info Information zum Datenverteiler
	 *
	 * @return Liste mit benachbarten Datenverteilern
	 */
	private List<TransmitterConnectionInfo> getInvolvedTransmitters(TransmitterInfo t_info) {
		ArrayList<TransmitterConnectionInfo> list = new ArrayList<TransmitterConnectionInfo>();
		for(int i = 0; i < _transmitterConnectionInfos.length; i++) {
			if((_transmitterConnectionInfos[i] == null) || _transmitterConnectionInfos[i].isExchangeConnection()) {
				continue;
			}
			TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
			if(t1.getTransmitterId() == t_info.getTransmitterId()) {
				TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
				if(t2 != null) {
					for(int j = 0; j < _transmitterConnectionInfos.length; ++j) {
						if(_transmitterConnectionInfos[j] == null) {
							continue;
						}
						if(_transmitterConnectionInfos[j].isExchangeConnection()) {
							TransmitterInfo _t1 = _transmitterConnectionInfos[j].getTransmitter_1();
							TransmitterInfo _t2 = _transmitterConnectionInfos[j].getTransmitter_2();
							if((_t1.getTransmitterId() == _transmitterId) && (_t2.getTransmitterId() == t2.getTransmitterId())) {
								list.add(_transmitterConnectionInfos[j]);
								break;
							}
						}
					}
				}
			}
		}
		return list;
	}

	/**
	 * Gibt die ID der Applikation zurück
	 *
	 * @param applicationTypePid die Pid des Applikationstyps
	 * @param applicationName    der Applikationsname
	 *
	 * @return die Applikation ID
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationChangeException
	 *          signalisiert Fehler bei Konfigurationsänderungen
	 */
	public long getApplicationId(String applicationTypePid, String applicationName) throws ConfigurationChangeException {
		if((applicationTypePid == null) || (applicationName == null)) {
			throw new IllegalArgumentException("Übergabeparameter ist null");
		}
		if(applicationTypePid.equals(_clientDavParameters.getApplicationTypePid()) && applicationName.equals(_clientDavParameters.getApplicationName())) {
			return _transmitterId;
		}
		// Ask the configuration
		if(dataModel != null) {
			SystemObjectType type = dataModel.getType(applicationTypePid);
			if(type instanceof DynamicObjectType) {
				DynamicObject application = null;
				String configAreaPidForApplicationObjects = _serverDavParameters.getConfigAreaPidForApplicationObjects();
				if(configAreaPidForApplicationObjects != null && !configAreaPidForApplicationObjects.equals("")) {
					ConfigurationArea configurationArea = dataModel.getConfigurationArea(configAreaPidForApplicationObjects);
					if(configurationArea == null) {
						_debug.warning("Angegebener Konfigurationsbereich für Applikationsobjekte '" + configAreaPidForApplicationObjects + "' nicht gefunden. "
						               + "Es wird der Defaultbereich der Konfiguration verwendet");
					}
					else {
						try {
							application = configurationArea.createDynamicObject((DynamicObjectType)type, "", applicationName);
						}
						catch(Exception e) {
							_debug.warning("Applikationsobjekt konnte nicht im angegebenen Konfigurationsbereich für Applikationsobjekte '"
							               + configAreaPidForApplicationObjects + "' erzeugt werden. Es wird der Defaultbereich der Konfiguration verwendet", e);
						}

					}
				}
				if(application == null) {
					application = dataModel.createDynamicObject(type, "", applicationName);
				}
				if(application != null) {
					return application.getId();
				}
			}
		}
		return -1L;
	}

	/**
	 * Gibt die ID der Konfiguration mit der gegebenen Pid zurück
	 *
	 * @param configurationPid Die Pid der Konfiguration
	 *
	 * @return die Id der Konfiguration
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationException
	 *          signalisiert Fehler bei Konfigurationsänderungen
	 */
	public long getConfigurationId(String configurationPid) throws ConfigurationException {
		// if local configuration
		Object objects[] = _serverDavParameters.getLocalModeParameter();
		if(objects != null) {
			String _configurationPid = (String)objects[0];
			if(configurationPid.equals(_configurationPid)) {
				return (Long)objects[1];
			}
		}
		if(CommunicationConstant.LOCALE_CONFIGURATION_PID_ALIASE.equals(configurationPid)) {
			if(dataModel == null) {
				throw new IllegalStateException("Konfiguration ist noch nicht angemeldet.");
			}
			return _configurationId;
//			return dataModel.getConfigurationAuthorityId();
		}
		// Ask the configuration
		if(dataModel != null) {
			SystemObject configuration = dataModel.getObject(configurationPid);
			if(configuration != null) {
				return configuration.getId();
			}
		}
		return -1L;
	}

	/**
	 * Fragt die Konfiguration, ob der übergegebene User mit dem Passwort ein berechtigter Benutzer ist oder nicht.
	 *
	 * @param userName                Benutzername
	 * @param encriptedPassword       verschlüsseltes Passwort
	 * @param authentificationText    Zufallstext
	 * @param authentificationProcess Namen des Authentifizierungsverfahrens
	 * @param typePid                 ID des Typs
	 *
	 * @return die Benutzerid wenn er berechtigt ist sonst -1
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationException
	 *          Fehler bei Konfigurationsänderungen
	 */
	public long isValidUser(
			String userName, byte[] encriptedPassword, String authentificationText, AuthentificationProcess authentificationProcess, String typePid)
			throws ConfigurationException {
		// if local configuration
		if(CommunicationConstant.CONFIGURATION_TYPE_PID.equals(typePid)) {
			if(userName.equals(_serverDavParameters.getConfigurationUserName())) {
				String password = _serverDavParameters.getConfigurationUserPassword();
				byte _encriptedPassword[] = authentificationProcess.encrypt(password, authentificationText);
				if(Arrays.equals(encriptedPassword, _encriptedPassword)) {
					return 0;
				}
			}
		}
		else if(CommunicationConstant.PARAMETER_TYPE_PID.equals(typePid)) {
			if(userName.equals(_serverDavParameters.getParameterUserName())) {
				String password = _serverDavParameters.getParameterUserPassword();
				byte _encriptedPassword[] = authentificationProcess.encrypt(password, authentificationText);
				if(Arrays.equals(encriptedPassword, _encriptedPassword)) {
					return 0;
				}
			}
		}
		else if(_serverDavParameters.isLocalMode()) {
			if(userName.equals(_clientDavParameters.getUserName())) {
				String password = _clientDavParameters.getUserPassword();
				byte _encriptedPassword[] = authentificationProcess.encrypt(password, authentificationText);
				if(Arrays.equals(encriptedPassword, _encriptedPassword)) {
					return _transmitterId;
				}
			}
		}

		// Ask the configuration
		if(dataModel != null) {
			ConfigurationManager configurationManager = ((DafDataModel)dataModel).getConfigurationManager();
			if(configurationManager == null) {
				throw new ConfigurationException("Der Konfigurationsmanager ist null");
			}
			else {

				// In diesem Fall wird die Klasse verschickt, die benutzt wurde um das Passwort zu verschlüsseln.
				// Auf der Gegenseite müßte dann genau diese Klasse verwendet werden, um ebenfalls das Passwort
				// zu verschlüsseln.
				// Dies wurde entfernt (Achim Wullenkord), da das Verschlüsslungsverfahren unverschlüsselt übertragen
				// wird und somit Problemlos geändert werden könnte und dann auf der Gegenseite eine Klasse benutzt werden
				// könnte, die immer "true" zurückgibt.
				// Jetzt wird das Verfahren übertragen und die Konfiguration bestimmt selbst (über eine Factory) welche
				// Klasse zum verschlüsseln benutzt wird (das übertragenen Verschlüsslungsverfahren bestimmt dabei die Klasse
				// die die Factory erzeugt)
//				long userId = configurationManager.isValidUser(
//				        userName, encriptedPassword, authentificationText, authentificationProcess.getClass().getName()
//				);

				// Es wird nicht die Klasse mit Klassennamen verschickt, sondern das angewandte Verfahren
				long userId = configurationManager.isValidUser(
						userName, encriptedPassword, authentificationText, authentificationProcess.getName()
				);

				if(userId > -1) {
					// Warten auf die Fertigmeldung der Parametrierung
					try {
						if(_serverDavParameters.getWaitForParamApp()) waitForParamReady(_serverDavParameters.getParamAppIncarnationName());
					}
					catch(InterruptedException e) {
						_debug.info("Warten auf Parametrierung wurde unterbrochen", e);
						return -1L;
					}
					authentificationManager.addUser(userId);
				}
				return userId;
			}
		}
		return -1L;
	}

	/** Setzt dass die Konfigurationsverbindung erfolgreich hergestellt ist */
	public final void setLocalConfigurationAvaillable() {
		//configurationAvaillable = true;
		synchronized(_applicationConnections) {
			for(int i = 0; i < _applicationConnections.size(); ++i) {
				T_A_HighLevelCommunicationInterface connection = _applicationConnections.get(i);
				if(connection != null) {
					String applicationTypePid = connection.getApplicationTypePid();
					String applicationName = connection.getApplicationName();
					if((applicationTypePid == null) || (applicationName == null)) {
						continue;
					}
					if(applicationTypePid.equals(_clientDavParameters.getApplicationTypePid())
					   && applicationName.equals(_clientDavParameters.getApplicationName())) {
						connection.continueAuthentification();
						break;
					}
				}
			}
		}
	}

	/**
	 * Aktualisiert die Anmeldungen eines Datenverteiler oder einer Applikation bezüglich der neuen Rechte.
	 *
	 * @param userId Identität des Benutzers
	 */
	public final synchronized void handleUserRightsChanged(long userId) {
		OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
		List<T_A_HighLevelCommunication> applications = getApplicationConnections(userId);
		if((applications != null) && (applications.size() > 0)) {
			for(int j = applications.size() - 1; j > -1; --j) {
				T_A_HighLevelCommunication connection = applications.get(j);
				if(connection != null) {
					long connectionId = connection.getId();
					SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
					if(subscriptionsFromApplicationStorage != null) {
						InAndOutSubscription subscriptions[] = subscriptionsManager.getSubscriptionsFromSource(connectionId);
						if((subscriptions == null) || (subscriptions.length == 0)) {
							continue;
						}
						for(int k = 0; k < subscriptions.length; ++k) {
							InAndOutSubscription subscription = subscriptions[k];
							if(subscription == null) {
								continue;
							}
							final BaseSubscriptionInfo info = subscription._baseSubscriptionInfo;
							UserAction userAction = getStateFromApplicationSubscription(subscription._role, info, subscriptionsFromApplicationStorage);

							boolean notMainTransmitter =
									(subscriptionsManager.getSendingComponent(info) == null) && (subscriptionsManager.getReceivingComponent(
											info
									) == null);

							boolean actionAllowed = authentificationManager.isActionAllowed(userId, info, userAction);
							if(!actionAllowed) {
								byte oldReceip = subscription._state;
								subscription._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
								if(oldReceip == subscription._state) {
									continue;
								}
								if(subscription._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
									ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(info);
									if(receiveSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
										long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
										ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
												info, dataNumber, false, (byte)3, null, null, 1, 0, System.currentTimeMillis()
										);
										connection.sendData(dataToSend);
										subscriptionsManager.removeSubscriptionRequest(subscription);
										subscriptionsFromApplicationStorage.unsubscribeReceiveData(subscription._baseSubscriptionInfo);
									}
								}
								else {
									SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
									if(sendSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
										connection.triggerSender(info, (byte)2); // Stop sending. No rights for this action
										subscriptionsManager.removeSubscriptionRequest(subscription);
										subscriptionsFromApplicationStorage.unsubscribeSendData(subscription._baseSubscriptionInfo);
									}
								}
								cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
								if(subscription._outSubscriptions != null) {
									for(int h = 0; h < subscription._outSubscriptions.length; ++h) {
										if(subscription._outSubscriptions[h] != null) {
											subscriptionHelper.handleOutUnsubscription(subscription, subscription._outSubscriptions[h]);
										}
									}
									subscription._outSubscriptions = null;
									subscription._currentTransmittersToConsider = null;
								}
								InAndOutSubscription involvedSubscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
										subscription
								);
								if(involvedSubscriptions != null) {
									if(notMainTransmitter) { // not main transmitter
										for(int h = 0; h < involvedSubscriptions.length; ++h) {
											if((involvedSubscriptions[h] != null) && (involvedSubscriptions[h] != subscription)) {
												cleanPendingSubscriptionToRedirect(involvedSubscriptions[h], subscriptionHelper);
												workInvolvedSubscription(involvedSubscriptions[h], subscriptionHelper);
											}
										}
									}
								}
							}
							else {
								// FIXME Unsinniger/Unvollständiger Code, getInterrestedSendingComponent() liefert nicht alle lokalen interessierten Verbindungen (siehe Testfall)
								byte oldReceip = subscription._state;
								subscription._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
								if(notMainTransmitter) { // not main transmitter
									cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
									workInvolvedSubscription(subscription, subscriptionHelper);
								}
								else {
									SubscriptionsFromRemoteStorage sendingSubscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedSendingComponent(
											info
									);
									if(sendingSubscriptionsFromRemoteStorages != null) {
										for(int i = 0; i < sendingSubscriptionsFromRemoteStorages.length; ++i) {
											if((sendingSubscriptionsFromRemoteStorages[i] != null) && (sendingSubscriptionsFromRemoteStorages[i].getType()
											                                                           == SubscriptionsFromRemoteStorage.T_A)) {
												SendSubscriptionInfo _sendSubscriptionInfo = ((SubscriptionsFromApplicationStorage)sendingSubscriptionsFromRemoteStorages[i]).getSendSubscription(
														info
												);
												if((_sendSubscriptionInfo != null) && (_sendSubscriptionInfo.isRequestSupported())) {
													if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
														T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)sendingSubscriptionsFromRemoteStorages[i].getConnection();
														if(_connection != null) {
															_sendSubscriptionInfo.setLastTriggerRequest((byte)0);
															_connection.triggerSender(info, (byte)0);// start sending
															handleApplicationSendSubscription(_connection, new SendSubscriptionTelegram(_sendSubscriptionInfo));
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		List<T_T_HighLevelCommunication> transmitters = getTransmitterConnections(userId);
		if((transmitters != null) && (transmitters.size() > 0)) {
			for(int j = transmitters.size() - 1; j > -1; --j) {
				T_T_HighLevelCommunication connection = transmitters.get(j);
				if(connection != null) {
					long connectionId = connection.getId();
					InAndOutSubscription subscriptions[] = subscriptionsManager.getSubscriptionsFromSource(connectionId);
					if((subscriptions == null) || (subscriptions.length == 0)) {
						continue;
					}
					for(int k = 0; k < subscriptions.length; ++k) {
						InAndOutSubscription subscription = subscriptions[k];
						if(subscription == null) {
							continue;
						}
						BaseSubscriptionInfo info = subscription._baseSubscriptionInfo;
						boolean notMainTransmitter = (subscriptionsManager.getSendingComponent(info) == null) && (subscriptionsManager.getReceivingComponent(
								info
						) == null);
						//FIXME: Korrekt? Was ist mit Quelle/Senke? Insbesondere wenn notMainTransmitter true ist?
						boolean actionAllowed = authentificationManager.isActionAllowed(
								userId, info, (subscription._role == 0) ? UserAction.SENDER : UserAction.RECEIVER
						);
						if(!actionAllowed) {
							byte oldReceip = subscription._state;
							subscription._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
							if(oldReceip == subscription._state) {
								continue;
							}
							TransmitterDataSubscriptionReceipt noRightsReceipt = new TransmitterDataSubscriptionReceipt(
									info, subscription._role, subscription._state, subscription._mainTransmitter, subscription._transmittersToConsider
							);
							connection.sendTelegram(noRightsReceipt);
							cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
							if(subscription._outSubscriptions != null) {
								for(int h = 0; h < subscription._outSubscriptions.length; ++h) {
									if(subscription._outSubscriptions[h] != null) {
										subscriptionHelper.handleOutUnsubscription(subscription, subscription._outSubscriptions[h]);
									}
								}
								subscription._outSubscriptions = null;
								subscription._currentTransmittersToConsider = null;
							}
							InAndOutSubscription involvedSubscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
									subscription
							);
							if(involvedSubscriptions != null) {
								if(notMainTransmitter) {
									for(int h = 0; h < involvedSubscriptions.length; ++h) {
										if((involvedSubscriptions[h] != null) && (involvedSubscriptions[h] != subscription)) {
											cleanPendingSubscriptionToRedirect(involvedSubscriptions[h], subscriptionHelper);
											workInvolvedSubscription(involvedSubscriptions[h], subscriptionHelper);
										}
									}
								}
							}
						}
						else {
							byte oldReceip = subscription._state;
							subscription._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
							if(notMainTransmitter) { // not main transmitter
								cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
								workInvolvedSubscription(subscription, subscriptionHelper);
							}
							else {
								if(subscription._transmittersToConsider == null) {
									continue;
								}
								boolean found = false;
								for(int kk = 0; kk < subscription._transmittersToConsider.length; ++kk) {
									if(subscription._transmittersToConsider[kk] == _transmitterId) {
										found = true;
									}
								}
								if(found) {
									TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
											subscription._baseSubscriptionInfo,
											subscription._role,
											TransmitterSubscriptionsConstants.POSITIV_RECEIP,
											_transmitterId,
											subscription._transmittersToConsider
									);
									T_T_HighLevelCommunication _connection = getTransmitterConnection(subscription._source);
									if(_connection != null) {
										_connection.sendTelegram(positivReceipt);
										if(oldReceip != TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
											SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
											if(subscriptionsFromDavStorage != null) {
												TransmitterDataSubscription transmitterDataSubscription = new TransmitterDataSubscription(
														subscription._baseSubscriptionInfo, subscription._role, subscription._transmittersToConsider
												);
												if(subscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
													subscriptionsFromDavStorage.subscribeSendData(transmitterDataSubscription);
												}
												else {
													subscriptionsFromDavStorage.subscribeReceiveData(transmitterDataSubscription);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		subscriptionHelper.flushOutUnSubscription(this);
	}

	/**
	 * Leitet die Anmeldungen an den beteiligten Datenverteiler weiter.
	 * <p/>
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen um ein Anmeldungstelegramm eines Datenverteilers abzuarbeiten. Ein Eintrag mit der
	 * Anmeldungsinformationen wird erstellt und in eine Verwaltungstabelle aufgenommen.
	 *
	 * @param connection   Schnittstelle "Datenverteiler-Datenverteiler"
	 * @param subscription Anmeldung von Daten des DAV für Empfänger oder Sender
	 */
	public final synchronized void handleTransmitterSubscription(
			T_T_HighLevelCommunicationInterface connection, TransmitterDataSubscription subscription) {
		byte subscriptionState = subscription.getSubscriptionState();
		BaseSubscriptionInfo info = subscription.getBaseSubscriptionInfo();
		long ids[] = subscription.getTransmitters();
		if(ids == null) {
			TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
					info, subscriptionState, TransmitterSubscriptionsConstants.NEGATIV_RECEIP, -1, ids
			);
			connection.sendTelegram(negativReceipt);
		}
		else {
			long sourceTransmitter = connection.getId();
			long sourceUserId = connection.getRemoteUserId();
			InAndOutSubscription request = new InAndOutSubscription(sourceTransmitter, subscription);
			// if there is a subscription posted to the source then unsubscribe it
			InAndOutSubscription postedSubscriptions[] = subscriptionsManager.getSameSubscriptionPostedTo(
					info, subscriptionState, sourceTransmitter, ids
			);
			if(postedSubscriptions != null) {
				for(int i = 0; i < postedSubscriptions.length; ++i) {
					if((postedSubscriptions[i] != null) && (postedSubscriptions[i]._outSubscriptions != null)) {
						cleanPendingSubscriptionToRedirect(postedSubscriptions[i]);
						for(int j = 0; j < postedSubscriptions[i]._outSubscriptions.length; ++j) {
							if(postedSubscriptions[i]._outSubscriptions[j] != null) {
								TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
										postedSubscriptions[i]._baseSubscriptionInfo,
										postedSubscriptions[i]._role,
										postedSubscriptions[i]._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										postedSubscriptions[i]._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_unsubscription);
								}
							}
						}
						postedSubscriptions[i]._outSubscriptions = null;
						postedSubscriptions[i]._currentTransmittersToConsider = null;
						postedSubscriptions[i]._state = -1;
					}
				}
			}
			else {
				for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
					ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
							i
					);
					if(exchangeSubscriptionAction != null) {
						InAndOutSubscription newRequest = exchangeSubscriptionAction.newSubscription;
						if((newRequest != null) && (newRequest._baseSubscriptionInfo.equals(info)) && (newRequest._outSubscriptions != null)) {
							for(int j = 0; j < newRequest._outSubscriptions.length; ++j) {
								if(newRequest._outSubscriptions[j] != null) {
									if(newRequest._outSubscriptions[j]._targetTransmitter == sourceTransmitter) {
										if(newRequest._outSubscriptions[j]._transmittersToConsider != null
										   && newRequest._outSubscriptions[j]._transmittersToConsider.length == ids.length) {
											boolean subSet = true;
											for(int k = 0; k < ids.length; ++k) {
												boolean found = false;
												for(int h = 0; h < newRequest._outSubscriptions[j]._transmittersToConsider.length; ++h) {
													if(newRequest._outSubscriptions[j]._transmittersToConsider[h] == ids[k]) {
														found = true;
														break;
													}
												}
												if(!found) {
													subSet = false;
													break;
												}
											}
											if(subSet) {
												cleanPendingSubscriptionToRedirect(exchangeSubscriptionAction.oldSubscription);
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			InAndOutSubscription _request = subscriptionsManager.addSubscriptionRequest(request, _transmitterId);
			if(_request != request) {
				if(_request._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
					TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, TransmitterSubscriptionsConstants.POSITIV_RECEIP, _request._mainTransmitter, ids
					);
					connection.sendTelegram(positivReceipt);
				}
				else if(_request._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT) {
					TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT, _request._mainTransmitter, ids
					);
					connection.sendTelegram(positivReceipt);
				}
				else if(_request._state == TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP) {
					TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP, _request._mainTransmitter, ids
					);
					connection.sendTelegram(positivReceipt);
				}
				else if(_request._state == TransmitterSubscriptionsConstants.NEGATIV_RECEIP) {
					TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, TransmitterSubscriptionsConstants.NEGATIV_RECEIP, -1, ids
					);
					connection.sendTelegram(negativReceipt);
				}
				return;
			}

			boolean found = false;
			for(int i = 0; i < ids.length; ++i) {
				if(ids[i] == _transmitterId) {
					found = true;
					break;
				}
			}
			// Main transmitter
			if(found) {
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = subscriptionsManager.getReceivingComponent(info);
				if(subscriptionsFromApplicationStorage == null) {
					subscriptionsFromApplicationStorage = subscriptionsManager.getSendingComponent(info);
				}
				if(subscriptionsFromApplicationStorage != null) {
					UserAction userAction = getStateFromApplicationSubscription(subscriptionState, info, subscriptionsFromApplicationStorage);
					boolean actionAllowed = authentificationManager.isActionAllowed(sourceUserId, info, userAction);
					final byte newState = actionAllowed
					                      ? TransmitterSubscriptionsConstants.POSITIV_RECEIP
					                      : TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
					TransmitterDataSubscriptionReceipt receiptTelegram = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, newState, _transmitterId, ids
					);
					connection.sendTelegram(receiptTelegram);
					request._state = newState;
					if(actionAllowed) {
						request._mainTransmitter = _transmitterId;
						if(!subscriptionsManager.isSubscriptionRequestAvaillable(request)) {
							SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)connection.getSubscriptionsFromRemoteStorage();
							if(subscriptionsFromDavStorage != null) {
								if(subscriptionState == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending ?
									subscriptionsFromDavStorage.subscribeSendData(subscription);
								}
								else { // receiving ?
									subscriptionsFromDavStorage.subscribeReceiveData(subscription);
									SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
									if(sendSubscriptionInfo != null) {
										if(sendSubscriptionInfo.isRequestSupported()) {
											if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
												T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)subscriptionsFromApplicationStorage.getConnection();
												if(_connection != null) {
													// FIXME: Wieso doppelte Prüfung?
													// FIXME: Anscheinend einmal die entfernte Anmeldung und einmal die lokale, allerdings ist der Code ziemlich schwer verständlich
													// FIXME: Zudem wäre es wahrscheinlich sinnvoll, die Prüfungen getrennt vorzunehmen
													if(authentificationManager.isActionAllowed(
															_connection.getRemoteUserId(),
															info,
															subscriptionsFromApplicationStorage.canSend(info) ? UserAction.SENDER : UserAction.SOURCE
													)) {
														sendSubscriptionInfo.setLastTriggerRequest((byte)0);
														_connection.triggerSender(info, (byte)0);
													}
												}
											}
										}
									}
									TransmitterDataTelegram dataToSend[] = cacheManager.getCurrentDataForTransmitter(info);
									if(dataToSend != null) {
										connection.sendTelegrams(dataToSend);
									}
								}
							}
						}
					}
					return;
				}
			}
			InAndOutSubscription inAndOutSubscription = subscriptionsManager.isSuccessfullySubscribed(
					info, subscriptionState, _transmitterId, ids
			);
			byte receip = 0;
			if(inAndOutSubscription != null) {
				receip = inAndOutSubscription._state;
			}
			if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
				request._state = receip;
				request._mainTransmitter = inAndOutSubscription._mainTransmitter;
				TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
						info, subscriptionState, receip, inAndOutSubscription._mainTransmitter, ids
				);
				connection.sendTelegram(positivReceipt);
				if(!subscriptionsManager.isSubscriptionRequestAvaillable(request)) {
					SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)connection.getSubscriptionsFromRemoteStorage();
					if(subscriptionsFromDavStorage != null) {
						if(subscriptionState == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
							subscriptionsFromDavStorage.subscribeSendData(subscription);
						}
						else { // receiving
							subscriptionsFromDavStorage.subscribeReceiveData(subscription);
							TransmitterDataTelegram dataToSend[] = cacheManager.getCurrentDataForTransmitter(info);
							if(dataToSend != null) {
								connection.sendTelegrams(dataToSend);
							}
						}
					}
				}
			}
			else if(subscriptionsManager.isUnsuccessfullySubscribed(info, subscriptionState, _transmitterId, ids)) {
				TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
						info, subscriptionState, TransmitterSubscriptionsConstants.NEGATIV_RECEIP, -1, ids
				);
				connection.sendTelegram(negativReceipt);
				request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
				request._mainTransmitter = -1;
			}
			else {
				makeOutgoingRequests(request);
				request._mainTransmitter = -1;
				if(request._outSubscriptions == null) {
					request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
					TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
							info, subscriptionState, TransmitterSubscriptionsConstants.NEGATIV_RECEIP, -1, ids
					);
					connection.sendTelegram(negativReceipt);
				}
				else if(subscriptionsManager.needToSendSubscription(_transmitterId, request)) {
					request._state = (byte)-1;
					for(int j = 0; j < request._outSubscriptions.length; ++j) {
						if(request._outSubscriptions[j] != null) {
							TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
									info, subscriptionState, request._outSubscriptions[j]._transmittersToConsider
							);
							T_T_HighLevelCommunication _connection = getTransmitterConnection(
									request._outSubscriptions[j]._targetTransmitter
							);
							if(_connection != null) {
								_connection.sendTelegram(_subscription);
							}
						}
					}
				}
				else {
					request._outSubscriptions = null;
				}
			}
		}
	}

	private UserAction getStateFromApplicationSubscription(
			final byte subscriptionState, final BaseSubscriptionInfo info, final SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage) {
		if(subscriptionState == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
			if(subscriptionsFromApplicationStorage.canSend(info)) {
				return UserAction.SOURCE;
			}
			return UserAction.SENDER;
		}
		else if(subscriptionState == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
			if(subscriptionsFromApplicationStorage.canReceive(info)) {
				return UserAction.DRAIN;
			}
			return UserAction.RECEIVER;
		}
		else {
			throw new IllegalArgumentException("Unbekannte Anmeldung: " + subscriptionState);
		}
	}


	private UserAction getStateFromReceiveSubscription(
			final ReceiveSubscriptionInfo receiveSubscriptionInfo) {
		if(receiveSubscriptionInfo.isDrain()) {
			return UserAction.DRAIN;
		}
		return UserAction.RECEIVER;
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, um ein Abmeldungstelegramm eines Datenverteilers zu bearbeiten.Der Eintrag mit den
	 * Abmeldungsinformationen wird aus der Verwaltungstabelle entfernt. Wenn ein anderer Eintrag mit den gleichen Anmeldeinformationen vorhanden ist, werden
	 * diesem die Anmeldeinformationen des abzumeldenden Eintrags u?bertragen. Diese Informationen geben unter anderem Aufschluss daru?ber, zu welchem anderen
	 * Datenverteiler eine Folgeanmeldung gesendet wurde. Auch wenn eine Umleitung für den abzumeldenden Ein-trag besteht, muss diese durch den neuen Eintrag
	 * ersetzt werden.
	 *
	 * @param connection     Schnittstelle "Datenverteiler-Datenverteiler"
	 * @param unsubscription Abmeldung von Daten des DAV für Empfänger oder Sender
	 */
	public final synchronized void handleTransmitterUnsubscription(
			T_T_HighLevelCommunicationInterface connection, TransmitterDataUnsubscription unsubscription) {
		long sourceTransmitterId = connection.getId();
		byte subscriptionState = unsubscription.getSubscriptionState();
		BaseSubscriptionInfo info = unsubscription.getBaseSubscriptionInfo();
		long transmitterIds[] = unsubscription.getTransmitters();
		if((info != null) && (transmitterIds != null)) {
			InAndOutSubscription subscription = subscriptionsManager.removeSubscriptionRequest(
					sourceTransmitterId, info, subscriptionState, transmitterIds, _transmitterId
			);
			if(subscription != null) {
				boolean done = false;
				if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
					InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
							subscription
					);
					if(alternativeSubscription != null) {
						alternativeSubscription._outSubscriptions = subscription._outSubscriptions;
						alternativeSubscription._state = subscription._state;
						alternativeSubscription._mainTransmitter = subscription._mainTransmitter;
						alternativeSubscription._transmittersToConsider = subscription._transmittersToConsider;
						alternativeSubscription._currentTransmittersToConsider = subscription._currentTransmittersToConsider;
						for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
							ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
									i
							);
							if(exchangeSubscriptionAction != null) {
								if(exchangeSubscriptionAction.oldSubscription == subscription) {
									exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
								}
							}
						}
						done = true;
					}
					if(!subscriptionsManager.isSubscriptionRequestAvaillable(subscription)) {
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)connection.getSubscriptionsFromRemoteStorage();
						if(subscriptionsFromDavStorage != null) {
							if(subscriptionState == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
								subscriptionsFromDavStorage.unsubscribeSendData(info);
							}
							else {
								subscriptionsFromDavStorage.unsubscribeReceiveData(info);
							}
						}
					}
				}

				// no need to process: an alternative subscription exists
				if(done) {
					return;
				}

				OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
				cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
				if(subscription._outSubscriptions != null) {
					for(int i = 0; i < subscription._outSubscriptions.length; ++i) {
						if(subscription._outSubscriptions[i] != null) {
							subscriptionHelper.handleOutUnsubscription(subscription, subscription._outSubscriptions[i]);
						}
					}
					subscription._outSubscriptions = null;
					subscription._currentTransmittersToConsider = null;
				}
				if((subscriptionsManager.getSendingComponent(info) == null) && (subscriptionsManager.getReceivingComponent(info)
				                                                                == null)) { // not main transmitter
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(subscription);
					if(subscriptions != null) {
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								workInvolvedSubscription(subscriptions[i], subscriptionHelper);
							}
						}
					}
				}
				else {
					SubscriptionsFromRemoteStorage[] receivingSubscriptionsFromRemoteStorages = subscriptionsManager.getInterrestedReceivingComponent(
							info
					);
					SubscriptionsFromRemoteStorage[] sendingSubscriptionsFromRemoteStorages = subscriptionsManager.getInterrestedSendingComponent(info);
					if((receivingSubscriptionsFromRemoteStorages == null) && (sendingSubscriptionsFromRemoteStorages != null)) { // no one interrested
						for(int i = 0; i < sendingSubscriptionsFromRemoteStorages.length; ++i) {
							if((sendingSubscriptionsFromRemoteStorages[i] != null) && (sendingSubscriptionsFromRemoteStorages[i].getType()
							                                                           == SubscriptionsFromRemoteStorage.T_A)) {
								SendSubscriptionInfo _sendSubscriptionInfo = ((SubscriptionsFromApplicationStorage)sendingSubscriptionsFromRemoteStorages[i]).getSendSubscription(
										info
								);
								if((_sendSubscriptionInfo != null) && (_sendSubscriptionInfo.isRequestSupported())) {
									if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
										T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)sendingSubscriptionsFromRemoteStorages[i].getConnection();
										if(_connection != null) {
											_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
											_connection.triggerSender(info, (byte)1);// stop sending
										}
									}
								}
							}
						}
					}
					else if((sendingSubscriptionsFromRemoteStorages == null) && (receivingSubscriptionsFromRemoteStorages != null)) { // no one interrested
						long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
						ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
								info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
						);
						for(int i = 0; i < receivingSubscriptionsFromRemoteStorages.length; ++i) {
							if((receivingSubscriptionsFromRemoteStorages[i] != null) && (receivingSubscriptionsFromRemoteStorages[i].getType()
							                                                             == SubscriptionsFromRemoteStorage.T_A)) {
								ReceiveSubscriptionInfo _receiveSubscriptionInfo = ((SubscriptionsFromApplicationStorage)receivingSubscriptionsFromRemoteStorages[i]).getReceiveSubscription(
										info
								);
								if(_receiveSubscriptionInfo != null) {
									if(_receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
										if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
											continue;
										}
									}
									T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)receivingSubscriptionsFromRemoteStorages[i].getConnection();
									if(_connection != null) {
										_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
										_receiveSubscriptionInfo.setLastErrorState((byte)2);
										_connection.sendData(dataToSend);
									}
								}
							}
						}
					}
				}
				subscriptionHelper.flushOutUnSubscription(this);

				// if no one interrested delete cached Data
				if(!subscriptionsManager.isSubscriptionRequestAvaillable(info)) {
					cacheManager.clean(info);
				}
			}
		}
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, um eine anstehende Anmeldung zu quittieren.
	 *
	 * @param connection      Schnittstelle "Datenverteiler-Datenverteiler"
	 * @param receiptTelegram Quittung des DAV für eine an ihn gerichtete Datenanmeldung.
	 */
	public final synchronized void handleTransmitterSubscriptionReceip(
			T_T_HighLevelCommunicationInterface connection, TransmitterDataSubscriptionReceipt receiptTelegram) {
		long targetId = connection.getId();
		InAndOutSubscription subscriptions[] = subscriptionsManager.getSubscriptionRequestToUpdate(targetId, receiptTelegram);
		if(subscriptions == null) {
			handleTransmitterSubscriptionReceip(
					null,
					targetId,
					receiptTelegram.getBaseSubscriptionInfo(),
					receiptTelegram.getMainTransmitterId(),
					receiptTelegram.getTransmitters(),
					receiptTelegram.getSubscriptionState(),
					receiptTelegram.getReceipt()
			);
		}
		else {
			for(int i = 0; i < subscriptions.length; ++i) {
				handleTransmitterSubscriptionReceip(
						subscriptions[i],
						targetId,
						receiptTelegram.getBaseSubscriptionInfo(),
						receiptTelegram.getMainTransmitterId(),
						receiptTelegram.getTransmitters(),
						receiptTelegram.getSubscriptionState(),
						receiptTelegram.getReceipt()
				);
			}
			handleTransmitterSubscriptionReceip(
					null,
					targetId,
					receiptTelegram.getBaseSubscriptionInfo(),
					receiptTelegram.getMainTransmitterId(),
					receiptTelegram.getTransmitters(),
					receiptTelegram.getSubscriptionState(),
					receiptTelegram.getReceipt()
			);
		}
	}

	/**
	 * Hilfsmethode zu <code>final synchronized void handleTransmitterSubscriptionReceip(T_T_HighLevelCommunication connection, TransmitterDataSubscriptionReceipt
	 * receipTelegram) </code>
	 *
	 * @param subscription      Anmeldung TBD weitere Beschreibung, da InAndOutSubscription noch nicht kommentiert
	 * @param targetId          Id des Kommunikationpartners
	 * @param info              Datenidentifikation
	 * @param mainTransmitterId Identität des Haupt-DAV
	 * @param transmitterIds    Liste von erreichbaren transmittern
	 * @param subscriptionState Status der Anmeldung
	 * @param receip            Status der Quittung
	 *
	 * @see #handleTransmitterSubscriptionReceip(de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface,
	 *      de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscriptionReceipt)
	 */
	final void handleTransmitterSubscriptionReceip(
			InAndOutSubscription subscription,
			long targetId,
			BaseSubscriptionInfo info,
			long mainTransmitterId,
			long transmitterIds[],
			byte subscriptionState,
			byte receip) {
		if(Transmitter._debugLevel > 20) {
			System.err.println("subscription = " + subscription);
			System.err.println("targetId = " + targetId);
			System.err.println("info = " + info);
			System.err.println("mainTransmitterId = " + mainTransmitterId);
//			System.err.println("transmitterIds = " + Arrays.asList(transmitterIds));
			System.err.println("subscriptionState = " + subscriptionState);
			System.err.println("receip = " + receip);
		}
		if(subscription == null) {
			if(Transmitter._debugLevel > 20) {
				System.err.println("pendingSubscriptionToRedirect.size () = " + _pendingSubscriptionToRedirect.size());
			}
			for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
				ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
						i
				);
				if(exchangeSubscriptionAction != null) {
					if(subscriptionsManager.updateExchangeSubscriptionRequest(
							exchangeSubscriptionAction.newSubscription, targetId, info, transmitterIds, mainTransmitterId, subscriptionState, receip
					)) {
						InAndOutSubscription request = exchangeSubscriptionAction.newSubscription;
						if(request != null) {
							if(receip == TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP) {
								_pendingSubscriptionToRedirect.remove(i);
								InAndOutSubscription oldSubscription = exchangeSubscriptionAction.oldSubscription;
								InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
										oldSubscription._baseSubscriptionInfo
								);
								if(subscriptions != null) {
									for(int j = 0; j < subscriptions.length; ++j) {
										if(subscriptions[j] != null) {
											handleInvalidSubscription(subscriptions[j], mainTransmitterId);
										}
									}
								}
								return;
							}

							if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
								request._mainTransmitter = mainTransmitterId;
							}

							boolean allReceipted = true;
							int postiveResponces = 0;
							for(int j = 0; j < request._outSubscriptions.length; ++j) {
								if(request._outSubscriptions[j] != null) {
									if(request._outSubscriptions[j]._state == -1) {
										allReceipted = false;
										break;
									}
									else if(request._outSubscriptions[j]._state == 1) {
										++postiveResponces;
									}
								}
							}

							if(allReceipted) {
								if(postiveResponces == 1) {
									request._state = receip;
									if(request._outSubscriptions != null) {
										for(int j = request._outSubscriptions.length - 1; j > -1; --j) {
											if((request._outSubscriptions[j] != null) && (request._outSubscriptions[j]._state != 1)) {
												TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
														request._baseSubscriptionInfo, request._role, request._outSubscriptions[j]._transmittersToConsider
												);
												T_T_HighLevelCommunication _connection = getTransmitterConnection(
														request._outSubscriptions[j]._targetTransmitter
												);
												if(_connection != null) {
													_connection.sendTelegram(_unsubscription);
												}
												request._outSubscriptions[j] = null;
											}
										}
									}
									if(exchangeSubscriptionAction.isRedirectionSafe()) {// sender or source
										_pendingSubscriptionToRedirect.remove(i);
										InAndOutSubscription oldSubscription = exchangeSubscriptionAction.oldSubscription;
										if(oldSubscription != null) {
											for(int j = 0; j < oldSubscription._outSubscriptions.length; ++j) {
												if(oldSubscription._outSubscriptions[j] != null) {
													TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
															oldSubscription._baseSubscriptionInfo,
															oldSubscription._role,
															oldSubscription._outSubscriptions[j]._transmittersToConsider
													);
													T_T_HighLevelCommunication _connection = getTransmitterConnection(
															oldSubscription._outSubscriptions[j]._targetTransmitter
													);
													if(_connection != null) {
														_connection.sendTelegram(_unsubscription);
													}
												}
											}
											oldSubscription._state = exchangeSubscriptionAction.newSubscription._state;
											oldSubscription._mainTransmitter = exchangeSubscriptionAction.newSubscription._mainTransmitter;
											oldSubscription._currentTransmittersToConsider = exchangeSubscriptionAction.newSubscription._currentTransmittersToConsider;
											oldSubscription._outSubscriptions = exchangeSubscriptionAction.newSubscription._outSubscriptions;
										}
									}
								}
								else if(postiveResponces > 1) {
									_pendingSubscriptionToRedirect.remove(i);
									InAndOutSubscription oldSubscription = exchangeSubscriptionAction.oldSubscription;
									InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
											oldSubscription._baseSubscriptionInfo
									);
									if(subscriptions != null) {
										for(int j = 0; j < subscriptions.length; ++j) {
											if(subscriptions[j] != null) {
												handleInvalidSubscription(subscriptions[j], mainTransmitterId);
											}
										}
									}
								}
								else {
									_pendingSubscriptionToRedirect.remove(i);
									InAndOutSubscription newSubscription = exchangeSubscriptionAction.newSubscription;
									if(newSubscription != null) {
										for(int j = 0; j < newSubscription._outSubscriptions.length; ++j) {
											if(newSubscription._outSubscriptions[j] != null) {
												TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
														newSubscription._baseSubscriptionInfo,
														newSubscription._role,
														newSubscription._outSubscriptions[j]._transmittersToConsider
												);
												T_T_HighLevelCommunication _connection = getTransmitterConnection(
														newSubscription._outSubscriptions[j]._targetTransmitter
												);
												if(_connection != null) {
													_connection.sendTelegram(_unsubscription);
												}
											}
										}
									}
								}
							}
						}
						return;
					}
				}
			}
		}
		else {
			if(receip == TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP) {
				InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
						subscription._baseSubscriptionInfo
				);
				if(subscriptions != null) {
					if(Transmitter._debugLevel > 20) {
						System.err.println("E subscriptions = " + Arrays.asList(subscriptions));
					}
					for(int i = 0; i < subscriptions.length; ++i) {
						if(subscriptions[i] != null) {
							handleInvalidSubscription(subscriptions[i], mainTransmitterId);
						}
					}
				}
				return;
			}

			boolean allReceipted = true;
			byte _receip = -1;
			int postiveResponces = 0;
			for(int j = 0; j < subscription._outSubscriptions.length; ++j) {
				if(subscription._outSubscriptions[j] != null) {
					if(subscription._outSubscriptions[j]._state == -1) {
						allReceipted = false;
						break;
					}
					else if(subscription._outSubscriptions[j]._state == 1) {
						++postiveResponces;
						_receip = _receip < 1 ? 1 : _receip;
					}
					else if(subscription._outSubscriptions[j]._state == 2) {
						++postiveResponces;
						_receip = _receip < 2 ? 2 : _receip;
					}
					else if(subscription._outSubscriptions[j]._state == 3) {
						++postiveResponces;
						_receip = _receip < 3 ? 3 : _receip;
					}
				}
			}

			if(Transmitter._debugLevel > 20) {
				System.err.println("allReceipted = " + allReceipted);
				System.err.println("_receip = " + _receip);
				System.err.println("receip = " + receip);
				System.err.println("postiveResponces = " + postiveResponces);
			}
			if(allReceipted) {
				// Alle Quittungen auf Anmeldungen bei potentiellen Zentral-Datenverteilern erhalten
				if(postiveResponces == 1) {
					// Eine der Quittungen war positiv: Zentraldatenverteiler wurde damit identifiziert
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(subscription, true);
					if(subscriptions != null) {
						if(Transmitter._debugLevel > 20) {
							System.err.println("C subscriptions = " + Arrays.asList(subscriptions));
						}
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								handlePositiveReceip(subscriptions[i], _receip, mainTransmitterId);
							}
						}
					}
				}
				else if(postiveResponces > 1) {
					// Mehrere Zentraldatenverteiler, d.h. an mehreren Datenverteilern hat sich eine Quelle, bzw. Senke angemeldet => Ungültige Anmeldung.
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
							subscription._baseSubscriptionInfo
					);
					if(subscriptions != null) {
						if(Transmitter._debugLevel > 20) {
							System.err.println("D subscriptions = " + Arrays.asList(subscriptions));
						}
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								handleInvalidSubscription(subscriptions[i], mainTransmitterId);
							}
						}
					}
				}
				else { // negativ receip
					// Keiner der potentiellen Dav ist Zentraldatenverteiler
					if(Transmitter._debugLevel > 20) {
						System.err.println("#######################################################################");
						System.err.println("subscription.receip= " + subscription._state);
						System.err.println("receip= " + receip);
					}
					if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
						// rs: von einem anderen DAV negative Quittung zu einer vorher positiv quittierten Anmeldung
						// rs: erhalten, also als abgemeldet vermerken
						if(subscription._fromTransmitter) {
							T_T_HighLevelCommunication _connection = getTransmitterConnection(subscription._source);
							if(_connection != null) {
								if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
									if(!subscriptionsManager.isSubscriptionRequestAvaillable(subscription)) {
										SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
										if(subscriptionsFromDavStorage != null) {
											if(subscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
												if(Transmitter._debugLevel > 20) System.err.println("unsubscribeSendData");
												subscriptionsFromDavStorage.unsubscribeSendData(subscription._baseSubscriptionInfo);
											}
											else {
												if(Transmitter._debugLevel > 20) System.err.println("unsubscribeReceiveData");
												subscriptionsFromDavStorage.unsubscribeReceiveData(subscription._baseSubscriptionInfo);
											}
										}
									}
								}
							}
						}
						InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedPositiveSubscriptionRequests(
								subscription
						);
						if(subscriptions != null) {
							if(Transmitter._debugLevel > 20) {
								System.err.println("A subscriptions = " + Arrays.asList(subscriptions));
							}
							OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
							for(int i = 0; i < subscriptions.length; ++i) {
								if(subscriptions[i] != null) {
									cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
									workInvolvedSubscription(subscriptions[i], subscriptionHelper);
								}
							}
							subscriptionHelper.flushOutUnSubscription(this);
						}
					}
					// Egal, ob vorher eine positive Quittung vorlag oder nicht müssen alle betroffenen Applikation und Datenverteiler, von denen die
					// Anmeldung gestellt wurde, benachrichtigt werden.
					InAndOutSubscription[] subscriptions;
					subscriptions = subscriptionsManager.getInvolvedSubscriptionRequests(subscription, true);
					if(subscriptions != null) {
						if(Transmitter._debugLevel > 20) {
							System.err.println("B2 subscriptions = " + Arrays.asList(subscriptions));
						}
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								handleNegativeReceip(subscriptions[i]);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Hilfsmethode zu <code>final void handleTransmitterSubscriptionReceip(InAndOutSubscription subscription, long targetId, BaseSubscriptionInfo info, long
	 * mainTransmitterId, long transmitterIds[],byte subscriptionState,byte receip)</code>. Behandelt eine negative Quittung einer Anmeldung.
	 *
	 * @param subscription Anmeldung einer Applikation oder eines anderen Datenverteilers bei diesem Datenverteiler, die nicht oder nicht mehr befriedigt werden
	 *                     kann.
	 *
	 * @see #handleTransmitterSubscriptionReceip(InAndOutSubscription,long,de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo,long,long[],byte,byte)
	 */
	private final void handleNegativeReceip(InAndOutSubscription subscription) {
		if(Transmitter._debugLevel > 20) System.err.println("++handleNegativeReceip subscription = " + subscription);
		if(subscription == null) {
			return;
		}
		if(subscription._state == TransmitterSubscriptionsConstants.NEGATIV_RECEIP) {
			return;
		}
		subscription._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
		subscription._mainTransmitter = -1;
		if(subscription._fromTransmitter) {
			TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
					subscription._baseSubscriptionInfo,
					subscription._role,
					TransmitterSubscriptionsConstants.NEGATIV_RECEIP,
					-1L,
					subscription._transmittersToConsider
			);
			T_T_HighLevelCommunication _connection = getTransmitterConnection(subscription._source);
			if(_connection != null) {
				_connection.sendTelegram(negativReceipt);
			}
		}
		else {
			T_A_HighLevelCommunication _connection = getApplicationConnection(subscription._source);
			if(_connection != null) {
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage != null) {
					if(subscription._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
						ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(
								subscription._baseSubscriptionInfo
						);
						if(receiveSubscriptionInfo != null) {
							long dataNumber = cacheManager.getCurrentDataIndex(subscription._baseSubscriptionInfo) + 1;
							if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
								if(receiveSubscriptionInfo.getLastErrorState() == 2) {
									return;
								}
							}
							ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
									subscription._baseSubscriptionInfo, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
							);
							receiveSubscriptionInfo.setLastDataIndex(dataNumber);
							receiveSubscriptionInfo.setLastErrorState((byte)2);
							_connection.sendData(dataToSend);
						}
					}
					else {
						SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(
								subscription._baseSubscriptionInfo
						);
						if(sendSubscriptionInfo != null) {
							if(sendSubscriptionInfo.isRequestSupported()) {
								if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
									sendSubscriptionInfo.setLastTriggerRequest((byte)1);
									_connection.triggerSender(subscription._baseSubscriptionInfo, (byte)1); // stop Sending no one interrested
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Hilfsmehode zu <code>private final void handleNegativeReceip(...) </code> Behandelt eine mehr als einmal positiv quittierte Anmeldung
	 *
	 * @param subscription      Anmeldung TBD weitere Beschreibung, da InAndOutSubscription noch nicht kommentiert
	 * @param mainTransmitterId ID des Zulieferer-Datenverteiler
	 *
	 * @see #handleTransmitterSubscriptionReceip(InAndOutSubscription,long,de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo,long,long[],byte,byte)
	 */
	private void handleInvalidSubscription(InAndOutSubscription subscription, long mainTransmitterId) {
		if(subscription == null) {
			return;
		}

		if(subscription._state == TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP) {
			return;
		}
		if(subscription._outSubscriptions != null) {
			for(int j = subscription._outSubscriptions.length - 1; j > -1; --j) {
				if(subscription._outSubscriptions[j] != null) {
					TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
							subscription._baseSubscriptionInfo, subscription._role, subscription._outSubscriptions[j]._transmittersToConsider
					);
					T_T_HighLevelCommunication _connection = getTransmitterConnection(
							subscription._outSubscriptions[j]._targetTransmitter
					);
					if(_connection != null) {
						_connection.sendTelegram(_unsubscription);
					}
				}
			}
		}
		subscription._outSubscriptions = null;
		subscription._currentTransmittersToConsider = null;

		subscription._state = TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP;
		subscription._mainTransmitter = mainTransmitterId;
		if(subscription._fromTransmitter) {
			TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
					subscription._baseSubscriptionInfo,
					subscription._role,
					TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP,
					subscription._mainTransmitter,
					subscription._transmittersToConsider
			);
			T_T_HighLevelCommunication _connection = getTransmitterConnection(subscription._source);
			if(_connection != null) {
				_connection.sendTelegram(positivReceipt);
			}
		}
		else {
			T_A_HighLevelCommunication _connection = getApplicationConnection(subscription._source);
			if(_connection != null) {
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage != null) {
					if(subscription._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
						ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(
								subscription._baseSubscriptionInfo
						);
						if(receiveSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
							long dataNumber = cacheManager.getCurrentDataIndex(subscription._baseSubscriptionInfo) + 1;
							ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
									subscription._baseSubscriptionInfo, dataNumber, false, (byte)8, null, null, 1, 0, System.currentTimeMillis()
							);
							_connection.sendData(dataToSend);
							subscriptionsManager.removeSubscriptionRequest(subscription);
							subscriptionsFromApplicationStorage.unsubscribeReceiveData(subscription._baseSubscriptionInfo);
						}
					}
					else {
						SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(
								subscription._baseSubscriptionInfo
						);
						if(sendSubscriptionInfo != null) {  //Fixme: Unnötige Prüfung?
							_connection.triggerSender(subscription._baseSubscriptionInfo, (byte)3);
							subscriptionsManager.removeSubscriptionRequest(subscription);
							subscriptionsFromApplicationStorage.unsubscribeSendData(subscription._baseSubscriptionInfo);
						}
					}
				}
			}
		}
	}

	/**
	 * Hilfsmethode zur Behandelung einer positive Quittung einer Anmeldung.
	 *
	 * @param subscription      Anmeldung TBD weitere Beschreibung, da InAndOutSubscription noch nicht kommentiert
	 * @param receip            Status der Quittung
	 * @param mainTransmitterId ID des Zulieferer-Datenverteiler
	 *
	 * @see #handleTransmitterSubscriptionReceip(InAndOutSubscription,long,de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo,long,long[],byte,byte)
	 */
	private void handlePositiveReceip(InAndOutSubscription subscription, byte receip, long mainTransmitterId) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(subscription._state == receip) {
			return;
		}
		subscription._state = receip;
		subscription._mainTransmitter = mainTransmitterId;
		if(subscription._outSubscriptions != null) {
			if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
				for(int i = subscription._outSubscriptions.length - 1; i > -1; --i) {
					if((subscription._outSubscriptions[i] != null) && (subscription._outSubscriptions[i]._state != 1)) {
						TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
								subscription._baseSubscriptionInfo, subscription._role, subscription._outSubscriptions[i]._transmittersToConsider
						);
						T_T_HighLevelCommunication _connection = getTransmitterConnection(
								subscription._outSubscriptions[i]._targetTransmitter
						);
						if(_connection != null) {
							_connection.sendTelegram(_unsubscription);
						}
						subscription._outSubscriptions[i] = null;
					}
				}
			}
			else if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT) {
				for(int i = subscription._outSubscriptions.length - 1; i > -1; --i) {
					if(subscription._outSubscriptions[i] != null) {
						TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
								subscription._baseSubscriptionInfo, subscription._role, subscription._outSubscriptions[i]._transmittersToConsider
						);
						T_T_HighLevelCommunication _connection = getTransmitterConnection(
								subscription._outSubscriptions[i]._targetTransmitter
						);
						if(_connection != null) {
							_connection.sendTelegram(_unsubscription);
						}
					}
				}
				subscription._outSubscriptions = null;
				subscription._currentTransmittersToConsider = null;
			}
		}
		if(subscription._fromTransmitter) {
			TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
					subscription._baseSubscriptionInfo, subscription._role, receip, subscription._mainTransmitter, subscription._transmittersToConsider
			);
			T_T_HighLevelCommunication _connection = getTransmitterConnection(subscription._source);
			if(_connection != null) {
				_connection.sendTelegram(positivReceipt);
				if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
					SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
					if(subscriptionsFromDavStorage != null) {
						TransmitterDataSubscription transmitterDataSubscription = new TransmitterDataSubscription(
								subscription._baseSubscriptionInfo, subscription._role, subscription._transmittersToConsider
						);
						if(subscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
							subscriptionsFromDavStorage.subscribeSendData(transmitterDataSubscription);
						}
						else {
							subscriptionsFromDavStorage.subscribeReceiveData(transmitterDataSubscription);
						}
					}
				}
			}
		}
		else {
			T_A_HighLevelCommunication _connection = getApplicationConnection(subscription._source);
			if(_connection != null) {
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage != null) {
					if(subscription._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) { // receiver
						if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT) {
							ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(
									subscription._baseSubscriptionInfo
							);
							if(receiveSubscriptionInfo != null) {
								long dataNumber = cacheManager.getCurrentDataIndex(subscription._baseSubscriptionInfo) + 1;
								if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
									if(receiveSubscriptionInfo.getLastErrorState() == 3) {
										return;
									}
								}
								receiveSubscriptionInfo.setLastDataIndex(dataNumber);
								receiveSubscriptionInfo.setLastErrorState((byte)3);
								ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
										subscription._baseSubscriptionInfo, dataNumber, false, (byte)3, null, null, 1, 0, System.currentTimeMillis()
								);
								_connection.sendData(dataToSend);
								subscriptionsManager.removeSubscriptionRequest(subscription);
								subscriptionsFromApplicationStorage.unsubscribeReceiveData(subscription._baseSubscriptionInfo);
							}
						}
					}
					else { // sender
						SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(
								subscription._baseSubscriptionInfo
						);
						if(sendSubscriptionInfo != null) {
							if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
								if(sendSubscriptionInfo.isRequestSupported()) {
									if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
										final UserAction userAction = getStateFromApplicationSubscription(
												subscription._role, subscription._baseSubscriptionInfo, subscriptionsFromApplicationStorage
										);
										if(authentificationManager.isActionAllowed(
												_connection.getRemoteUserId(), subscription._baseSubscriptionInfo, userAction
										)) {
											sendSubscriptionInfo.setLastTriggerRequest((byte)0);
											_connection.triggerSender(subscription._baseSubscriptionInfo, (byte)0); // start sending
										}
									}
								}
							}
							else if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT) {
								_connection.triggerSender(subscription._baseSubscriptionInfo, (byte)2); // stop sending no right
								subscriptionsManager.removeSubscriptionRequest(subscription);
								if(subscriptionsFromApplicationStorage != null) {
									subscriptionsFromApplicationStorage.unsubscribeSendData(subscription._baseSubscriptionInfo);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Leitet die Anmeldungen an den beteiligten Datenverteiler weiter. Diese Methode wird von der Protokollsteuerung DaV-DAF aufgerufen, wenn sich eine
	 * Applikation als Empfänger oder Senke anmmelden möchte. Ist keine andere Quelle oder Senke im System feststellbar, werden die Sendeanmeldungen bearbeitet,
	 * die an diesem Datenverteiler vorliegen.
	 *
	 * @param connection   Schnittstelle zwischen DAV und DAF
	 * @param subscription Anmeldung als Emfänger oder Senke
	 */
	public synchronized void handleApplicationReceiveSubscription(
			T_A_HighLevelCommunication connection, ReceiveSubscriptionTelegram subscription) {
		ReceiveSubscriptionInfo receiveSubscriptionInfo = subscription.getReceiveSubscriptionInfo();
		if(receiveSubscriptionInfo != null) {
			BaseSubscriptionInfo info = receiveSubscriptionInfo.getBaseSubscriptionInfo();
			if(info != null) {
				if(receiveSubscriptionInfo.isDrain()) {
					// Wenn eine andere Quelle oder Senke vorhanden ist, dann leeren Datensatz versenden.
					if((subscriptionsManager.getSendingComponent(info) != null) || (subscriptionsManager.getReceivingComponent(info) != null)) {
						long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
						ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
								info, dataNumber, false, (byte)8, null, null, 1, 0, System.currentTimeMillis()
						);
						connection.sendData(dataToSend);
						return;
					}
				}
				long sourceUserId = connection.getRemoteUserId();
				final UserAction userAction = getStateFromReceiveSubscription(receiveSubscriptionInfo);
				boolean actionAllowed = authentificationManager.isActionAllowed(
						sourceUserId, info, userAction
				);
				// Keine Zugriffsrechte vorhanden -> Leeren Datensatz versenden
				if(!actionAllowed) {
					long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
					ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
							info, dataNumber, false, (byte)3, null, null, 1, 0, System.currentTimeMillis()
					);
					connection.sendData(dataToSend);
					return;
				}
				// subscribe for receiving
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage1 = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage1 != null) {
					subscriptionsFromApplicationStorage1.subscribeReceiveData(subscription);
					// Es wurde erfolgreich eine neue Datenidentifikation durch eine Applikation angemeldet. Dies muss publiziert werden.
					if(_applicationStatusUpdater != null) {
						_applicationStatusUpdater.applicationSubscribedNewConnection(connection);
					}
				}
				long initiator = connection.getId();
				long potentialTransmitters[] = listsManager.getPotentialTransmitters(info);
				InAndOutSubscription request = new InAndOutSubscription(
						initiator, info, TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION, potentialTransmitters
				);
				InAndOutSubscription oldRequest = subscriptionsManager.addSubscriptionRequest(request, _transmitterId);
				if(oldRequest != request) {
					if(oldRequest._outSubscriptions != null) {
						for(int j = 0; j < oldRequest._outSubscriptions.length; ++j) {
							if(oldRequest._outSubscriptions[j] != null) {
								TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
										info, oldRequest._role, oldRequest._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										oldRequest._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_unsubscription);
								}
							}
						}
						oldRequest._outSubscriptions = null;
						oldRequest._currentTransmittersToConsider = null;
					}
				}

				if(receiveSubscriptionInfo.isDrain()) {
					request._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
					request._mainTransmitter = _transmitterId;
					cacheManager.clean(info);
					boolean noOneInterrested = true;
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(info);
					if(subscriptions != null) {
						for(int i = 0; i < subscriptions.length; ++i) {
							// see if there is an other source or drain in the system
							if((subscriptions[i] != null) && (subscriptions[i]._outSubscriptions != null) && (subscriptions[i]._state
							                                                                                  == TransmitterSubscriptionsConstants.POSITIV_RECEIP)) {
								long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
								ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
										info, dataNumber, false, (byte)8, null, null, 1, 0, System.currentTimeMillis()
								);
								connection.sendData(dataToSend);
								subscriptionsManager.removeSubscriptionRequest(request);
								if(subscriptionsFromApplicationStorage1 != null) {
									subscriptionsFromApplicationStorage1.unsubscribeReceiveData(info);
								}
								return;
							}
						}
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								cleanPendingSubscriptionToRedirect(subscriptions[i]);
								if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
									noOneInterrested = false;
								}
								if(subscriptions[i]._outSubscriptions != null) {
									for(int j = 0; j < subscriptions[i]._outSubscriptions.length; ++j) {
										if(subscriptions[i]._outSubscriptions[j] != null) {
											TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
													info, subscriptions[i]._role, subscriptions[i]._outSubscriptions[j]._transmittersToConsider
											);
											T_T_HighLevelCommunication _connection = getTransmitterConnection(
													subscriptions[i]._outSubscriptions[j]._targetTransmitter
											);
											if(_connection != null) {
												_connection.sendTelegram(_unsubscription);
											}
										}
									}
									subscriptions[i]._outSubscriptions = null;
									subscriptions[i]._currentTransmittersToConsider = null;
								}

								subscriptions[i]._state = 2;

								if(subscriptions[i]._fromTransmitter) { // transmitter
									T_T_HighLevelCommunication _connection = getTransmitterConnection(subscriptions[i]._source);
									if(_connection != null) {
										actionAllowed = authentificationManager.isActionAllowed(
												_connection.getRemoteUserId(), info, userAction
										);
										subscriptions[i]._state = actionAllowed
										                          ? TransmitterSubscriptionsConstants.POSITIV_RECEIP
										                          : TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
										TransmitterDataSubscriptionReceipt receiptTelegram = new TransmitterDataSubscriptionReceipt(
												info, subscriptions[i]._role, subscriptions[i]._state, _transmitterId, subscriptions[i]._transmittersToConsider
										);
										_connection.sendTelegram(receiptTelegram);
									}
									if(actionAllowed) {
										subscriptions[i]._mainTransmitter = _transmitterId;
										SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
										if(subscriptionsFromDavStorage != null) {
											TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
													info, subscriptions[i]._role, subscriptions[i]._transmittersToConsider
											);
											if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
												subscriptionsFromDavStorage.subscribeSendData(_subscription);
											}
											else {
												subscriptionsFromDavStorage.subscribeReceiveData(_subscription);
											}
										}
									}
								}
								else { // application
									T_A_HighLevelCommunication _connection = getApplicationConnection(subscriptions[i]._source);
									if(_connection != null) {
										actionAllowed = authentificationManager.isActionAllowed(
												_connection.getRemoteUserId(), info, userAction
										);
										subscriptions[i]._state = actionAllowed
										                          ? TransmitterSubscriptionsConstants.POSITIV_RECEIP
										                          : TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
										if(actionAllowed) {
											subscriptions[i]._mainTransmitter = _transmitterId;
											if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
												SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
												if(subscriptionsFromApplicationStorage != null) {
													SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
													if(sendSubscriptionInfo != null) {
														if(sendSubscriptionInfo.isRequestSupported()) {
															if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
																sendSubscriptionInfo.setLastTriggerRequest((byte)0);
																_connection.triggerSender(info, (byte)0); // start sending
															}
														}
													}
												}
											}
										}
										else {
											subscriptions[i]._mainTransmitter = _transmitterId;
											SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
											if(subscriptionsFromApplicationStorage != null) {
												if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
													SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
													if(sendSubscriptionInfo != null) {  //Fixme: Unnötige Prüfung?
														_connection.triggerSender(info, (byte)2); // stop sending no rights
														subscriptionsManager.removeSubscriptionRequest(subscriptions[i]);
														if(subscriptionsFromApplicationStorage != null) {
															subscriptionsFromApplicationStorage.unsubscribeSendData(subscriptions[i]._baseSubscriptionInfo);
														}
													}
												}
												else {
													ReceiveSubscriptionInfo _receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(
															info
													);
													if(_receiveSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
														long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
														ApplicationDataTelegram data = new ApplicationDataTelegram(
																info, dataNumber, false, (byte)3, null, null, 1, 0, System.currentTimeMillis()
														);
														_connection.sendData(data);
														subscriptionsManager.removeSubscriptionRequest(subscriptions[i]);
														if(subscriptionsFromApplicationStorage != null) {
															subscriptionsFromApplicationStorage.unsubscribeReceiveData(subscriptions[i]._baseSubscriptionInfo);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
					if(noOneInterrested) {
						long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
						if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
							if(receiveSubscriptionInfo.getLastErrorState() == 2) {
								return;
							}
						}
						receiveSubscriptionInfo.setLastDataIndex(dataNumber);
						receiveSubscriptionInfo.setLastErrorState((byte)2);
						ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
								info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
						);
						connection.sendData(dataToSend);
					}
					listsManager.addInfo(info);
				}
				else {
					// get the sending component (See if main transmitter)
					SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = subscriptionsManager.getSendingComponent(info);
					if(subscriptionsFromApplicationStorage == null) {
						subscriptionsFromApplicationStorage = subscriptionsManager.getReceivingComponent(info);
					}
					if(subscriptionsFromApplicationStorage == null) {
						InAndOutSubscription inAndOutSubscription = subscriptionsManager.isSuccessfullySubscribed(
								info, TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION
						);
						byte state = 0;
						if(inAndOutSubscription != null) {
							state = inAndOutSubscription._state;
						}
						if(state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
							request._state = state;
							request._mainTransmitter = inAndOutSubscription._mainTransmitter;
							try {
								subscriptionsManager.sendDataToT_A_Component(subscriptionsFromApplicationStorage1, info);
							}
							catch(ConfigurationException ex) {
								ex.printStackTrace();
							}
						}
						else if(subscriptionsManager.isUnsuccessfullySubscribed(
								info, TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION
						)) {
							request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
							request._mainTransmitter = -1;
							long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
							if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
								if(receiveSubscriptionInfo.getLastErrorState() == 2) {
									return;
								}
							}
							receiveSubscriptionInfo.setLastDataIndex(dataNumber);
							receiveSubscriptionInfo.setLastErrorState((byte)2);
							ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
									info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
							);
							connection.sendData(dataToSend);
						}
						else {
							makeOutgoingRequests(request);
							request._mainTransmitter = -1;
							if(request._outSubscriptions == null) {
								request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
								request._mainTransmitter = -1;
								long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
								if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
									if(receiveSubscriptionInfo.getLastErrorState() == 2) {
										return;
									}
								}
								receiveSubscriptionInfo.setLastDataIndex(dataNumber);
								receiveSubscriptionInfo.setLastErrorState((byte)2);
								ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
										info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
								);
								connection.sendData(dataToSend);
							}
							else if(subscriptionsManager.needToSendSubscription(_transmitterId, request)) {
								request._state = (byte)-1;
								for(int j = 0; j < request._outSubscriptions.length; ++j) {
									if(request._outSubscriptions[j] != null) {
										TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
												info,
												TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION,
												request._outSubscriptions[j]._transmittersToConsider
										);
										T_T_HighLevelCommunication _connection = getTransmitterConnection(
												request._outSubscriptions[j]._targetTransmitter
										);
										if(_connection != null) {
											_connection.sendTelegram(_subscription);
										}
									}
								}
							}
							else {
								request._outSubscriptions = null;
							}
						}
					}
					else {
						request._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
						request._mainTransmitter = _transmitterId;
						try {
							subscriptionsManager.sendDataToT_A_Component(subscriptionsFromApplicationStorage1, info);
						}
						catch(ConfigurationException ex) {
							ex.printStackTrace();
						}

						SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedSendingComponent(info);
						if(subscriptionsFromRemoteStorages == null) { // Kein sender
							long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
							if(receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
								if(receiveSubscriptionInfo.getLastErrorState() == 2) {
									return;
								}
							}
							receiveSubscriptionInfo.setLastDataIndex(dataNumber);
							receiveSubscriptionInfo.setLastErrorState((byte)2);
							ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
									info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
							);
							connection.sendData(dataToSend);
						}

						SubscriptionsFromRemoteStorage tmpSubscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedReceivingComponent(info);
						if((tmpSubscriptionsFromRemoteStorages != null) && (tmpSubscriptionsFromRemoteStorages.length == 1)) {
							InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(info);
							if(subscriptions != null) {
								for(int i = 0; i < subscriptions.length; ++i) {
									if(subscriptions[i] != null) {
										if(!subscriptions[i]._fromTransmitter) { // Application
											if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
												T_A_HighLevelCommunication _connection = getApplicationConnection(subscriptions[i]._source);
												if(_connection != null) {
													SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
													if(tmpSubscriptionsFromApplicationStorage != null) {
														SendSubscriptionInfo tmpSendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
																info
														);
														if(tmpSendSubscriptionInfo != null) {
															if(tmpSendSubscriptionInfo.isRequestSupported()) {
																if(tmpSendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
																	if(authentificationManager.isActionAllowed(
																			_connection.getRemoteUserId(),
																			info,
																			tmpSubscriptionsFromApplicationStorage.canSend(info)
																			? UserAction.SOURCE
																			: UserAction.SENDER
																			// FIXME: War vorher anscheinend falsch. Korrektur wirklich richtig?
																	)) {
																		tmpSendSubscriptionInfo.setLastTriggerRequest((byte)0);
																		_connection.triggerSender(info, (byte)0); // start sending
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Meldet die Daten ab und leitet die Abmeldungen, wenn nötig, an die beteiligten Datenverteiler weiter. Diese Methode wird von der Protokollsteuerung DaV-DAF
	 * aufgerufen, wenn sich eine Applikation als Empfänger oder Senke abmelden möchte. Im Falle einer Senkenabmeldung wird die Basisanmeldeinformation aus der
	 * Zuliefererdatenverwaltung entfernt, so dass dieser Datenverteiler nicht mehr der Zentraldatenverteiler der spezifizierten Daten ist. Danach werden alle von
	 * dieser gelöschten Anmeldung abhängigen Anmeldungen bearbeitet. Im Falle einer Empfängerabmeldug existieren zwei Möglichkeiten: Bei einer lokalen Quelle wird
	 * die Quelle aufgefordert das Senden der Daten einzustellen. Wenn die Quell- oder die Sendeapplikation bei einem anderen Datenverteiler angemeldet ist, dann
	 * wird zuerst überprüft, ob bei diesem Datenverteiler eine andere Empfangsanmeldung auf diese Daten vorhanden ist. Wenn keine Anmeldung für die spezifizierten
	 * Daten existiert, dann wird der aktuelle Datensatz aus dem Datenbestand entfernt.
	 *
	 * @param connection     Schnittstelle zwischen DAV und DAF
	 * @param unsubscription Abmeldetelegram der Applikation
	 */
	public synchronized void handleApplicationReceiveUnsubscription(
			T_A_HighLevelCommunication connection, ReceiveUnsubscriptionTelegram unsubscription) {

		long applicationId = connection.getId();
		byte subscriptionState = TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION;
		BaseSubscriptionInfo info = unsubscription.getUnSubscriptionInfo();
		InAndOutSubscription subscription = subscriptionsManager.removeSubscriptionRequest(
				applicationId, info, subscriptionState
		);
		SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
		// Wenn das Objekt null, mache nichts
		if(subscriptionsFromApplicationStorage != null) {
			ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.unsubscribeReceiveData(info);
			// Die Anmeldung der Applikation wurde zurück genommen -> Andere darüber informieren
			if(_applicationStatusUpdater != null) {
				_applicationStatusUpdater.applicationUnsubscribeConnection(connection);
			}

			// Wenn das Objekt null, mache nichts
			if(receiveSubscriptionInfo != null) {
				if(receiveSubscriptionInfo.isDrain()) {
					listsManager.removeInfo(info);
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(info);
					if(subscriptions != null) {
						OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
								workInvolvedSubscription(subscriptions[i], subscriptionHelper);
							}
						}
						subscriptionHelper.flushOutUnSubscription(this);
					}
				}
				else if(subscription != null) {
					if(subscription._outSubscriptions == null) {
						SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedReceivingComponent(info);
						if(subscriptionsFromRemoteStorages == null) { // no one interrested
							subscriptionsFromRemoteStorages = subscriptionsManager.getInterrestedSendingComponent(info);
							if(subscriptionsFromRemoteStorages != null) {
								for(int i = 0; i < subscriptionsFromRemoteStorages.length; ++i) {
									if((subscriptionsFromRemoteStorages[i] != null) && (subscriptionsFromRemoteStorages[i].getType()
									                                                    == SubscriptionsFromRemoteStorage.T_A)) {
										SendSubscriptionInfo _sendSubscriptionInfo = ((SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorages[i]).getSendSubscription(
												info
										);
										if((_sendSubscriptionInfo != null) && (_sendSubscriptionInfo.isRequestSupported())) {
											if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
												T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)subscriptionsFromRemoteStorages[i].getConnection();
												if(_connection != null) {
													_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
													_connection.triggerSender(info, (byte)1);// stop sending
												}
											}
										}
									}
								}
							}
						}
					}
					else {
						boolean done = false;
						if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
							InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
									subscription
							);
							if(alternativeSubscription != null) {
								alternativeSubscription._outSubscriptions = subscription._outSubscriptions;
								alternativeSubscription._state = subscription._state;
								alternativeSubscription._mainTransmitter = subscription._mainTransmitter;
								alternativeSubscription._transmittersToConsider = subscription._transmittersToConsider;
								alternativeSubscription._currentTransmittersToConsider = subscription._currentTransmittersToConsider;
								for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
									ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
											i
									);
									if(exchangeSubscriptionAction != null) {
										if(exchangeSubscriptionAction.oldSubscription == subscription) {
											exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
										}
									}
								}
								done = true;
							}
						}
						if(done) {
							return;
						}

						OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
						for(int i = 0; i < subscription._outSubscriptions.length; ++i) {
							if(subscription._outSubscriptions[i] != null) {
								subscriptionHelper.handleOutUnsubscription(subscription, subscription._outSubscriptions[i]);
							}
						}
						subscription._outSubscriptions = null;
						subscription._currentTransmittersToConsider = null;
						cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
						InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(subscription);
						if(subscriptions != null) {
							for(int i = 0; i < subscriptions.length; ++i) {
								if(subscriptions[i] != null) {
									cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
									workInvolvedSubscription(subscriptions[i], subscriptionHelper);
								}
							}
						}
						subscriptionHelper.flushOutUnSubscription(this);
					}
				}
				// if no one interrested delete cached Data
				if(!subscriptionsManager.isSubscriptionRequestAvaillable(info)) {
					cacheManager.clean(info);
				}
			}
		}
	}

	/**
	 * Leitet die Anmeldungen an den beteiligten Datenverteiler weiter. Diese Methode wird von der Protokollsteuerung DaV-DAF aufgerufen, wenn sich eine
	 * Applikation als Quelle oder Sender anmelden möchte. Wenn sich die Applikation als Quelle anmelden möchte, dann wird zuerst geprüft, ob an diesem
	 * Datenverteiler bereits eine andere Applikation als Quelle oder Senke für das gleiche Datum angemeldet ist. Wenn durch die Empfangsanmeldung Folgeanmeldungen
	 * bei anderen Datenverteiler vorhanden waren, dann werden diese abgemeldet. Stammt diese Empfangsanmeldung von einem anderen Datenverteiler, so wird eine
	 * positive Quittung an diesen Datenverteiler gesendet.
	 *
	 * @param connection   Schnittstelle zwischen DAV und DAF
	 * @param subscription Anmeldetelegramm der Applikation
	 */
	public synchronized void handleApplicationSendSubscription(
			T_A_HighLevelCommunication connection, SendSubscriptionTelegram subscription) {

		if(_applicationStatusUpdater != null) {
			_applicationStatusUpdater.applicationSubscribedNewConnection(connection);
		}

		SendSubscriptionInfo sendSubscriptionInfo = subscription.getSendSubscriptionInfo();
		if(sendSubscriptionInfo != null) {
			BaseSubscriptionInfo info = sendSubscriptionInfo.getBaseSubscriptionInfo();
			if(info != null) {
				if(sendSubscriptionInfo.isSource()) {
					if((subscriptionsManager.getSendingComponent(info) != null) || (subscriptionsManager.getReceivingComponent(info) != null)) {
						connection.triggerSender(info, (byte)3); // Stop sending more than one main application
						return;
					}
				}
				long sourceUserId = connection.getRemoteUserId();
				boolean actionAllowed = authentificationManager.isActionAllowed(
						sourceUserId, info, sendSubscriptionInfo.isSource() ? UserAction.SOURCE : UserAction.SENDER
				);
				if(!actionAllowed) {
					connection.triggerSender(info, (byte)2); // Stop sending. No rights for this action
					return;
				}
				// Subscribe for sending
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage1 = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage1 != null) {
					subscriptionsFromApplicationStorage1.subscribeSendData(subscription);
					// Es wurde erfolgreich eine neue Datenidentifikation durch eine Applikation angemeldet. Dies muss publiziert werden.
					if(_applicationStatusUpdater != null) {
						_applicationStatusUpdater.applicationSubscribedNewConnection(connection);
					}
				}
				long initiator = connection.getId();
				long potentialTransmitters[] = listsManager.getPotentialTransmitters(info);
				InAndOutSubscription request = new InAndOutSubscription(
						initiator, info, TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION, potentialTransmitters
				);
				InAndOutSubscription oldRequest = subscriptionsManager.addSubscriptionRequest(request, _transmitterId);
				if(oldRequest != request) {
					if(oldRequest._outSubscriptions != null) {
						for(int j = 0; j < oldRequest._outSubscriptions.length; ++j) {
							if(oldRequest._outSubscriptions[j] != null) {
								TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
										info, oldRequest._role, oldRequest._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										oldRequest._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_unsubscription);
								}
							}
						}
						oldRequest._outSubscriptions = null;
						oldRequest._currentTransmittersToConsider = null;
					}
				}
				if(sendSubscriptionInfo.isSource()) {
					request._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
					request._mainTransmitter = _transmitterId;
					cacheManager.clean(info);
					boolean noOneInterrested = true;
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(info);
					for(int i = 0; i < subscriptions.length; ++i) {
						// see if there is an other source or drain in the system
						if((subscriptions[i] != null) && (subscriptions[i]._outSubscriptions != null) && (subscriptions[i]._state
						                                                                                  == TransmitterSubscriptionsConstants.POSITIV_RECEIP)) {
							connection.triggerSender(info, (byte)3); // Stop sending more than one main application
							subscriptionsManager.removeSubscriptionRequest(request);
							if(subscriptionsFromApplicationStorage1 != null) {
								subscriptionsFromApplicationStorage1.unsubscribeSendData(info);
							}
							return;
						}
					}
					for(int i = 0; i < subscriptions.length; ++i) {
						if(subscriptions[i] != null) {
							cleanPendingSubscriptionToRedirect(subscriptions[i]);
							if(subscriptions[i]._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
								noOneInterrested = false;
							}
							if(subscriptions[i]._outSubscriptions != null) {
								for(int j = 0; j < subscriptions[i]._outSubscriptions.length; ++j) {
									if(subscriptions[i]._outSubscriptions[j] != null) {
										TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
												info, subscriptions[i]._role, subscriptions[i]._outSubscriptions[j]._transmittersToConsider
										);
										T_T_HighLevelCommunication _connection = getTransmitterConnection(
												subscriptions[i]._outSubscriptions[j]._targetTransmitter
										);
										if(_connection != null) {
											_connection.sendTelegram(_unsubscription);
										}
									}
								}
								subscriptions[i]._outSubscriptions = null;
								subscriptions[i]._currentTransmittersToConsider = null;
							}

							subscriptions[i]._state = (byte)2;

							if(subscriptions[i]._fromTransmitter) { // transmitter
								T_T_HighLevelCommunication _connection = getTransmitterConnection(subscriptions[i]._source);
								if(_connection != null) {
									actionAllowed = authentificationManager.isActionAllowed(
											_connection.getRemoteUserId(), info, getActionSourceSubscription(subscriptions[i])
									);
									subscriptions[i]._state = actionAllowed
									                          ? TransmitterSubscriptionsConstants.POSITIV_RECEIP
									                          : TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
									TransmitterDataSubscriptionReceipt receiptTelegram = new TransmitterDataSubscriptionReceipt(
											info, subscriptions[i]._role, subscriptions[i]._state, _transmitterId, subscriptions[i]._transmittersToConsider
									);
									_connection.sendTelegram(receiptTelegram);
								}
								if(actionAllowed) {
									subscriptions[i]._mainTransmitter = _transmitterId;
									SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
									if(subscriptionsFromDavStorage != null) {
										TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
												info, subscriptions[i]._role, subscriptions[i]._transmittersToConsider
										);
										if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
											subscriptionsFromDavStorage.subscribeSendData(_subscription);
										}
										else {
											subscriptionsFromDavStorage.subscribeReceiveData(_subscription);
										}
									}
								}
							}
							else { // application
								T_A_HighLevelCommunication _connection = getApplicationConnection(subscriptions[i]._source);
								if(_connection != null) {
									actionAllowed = authentificationManager.isActionAllowed(
											_connection.getRemoteUserId(), info, getActionSourceSubscription(subscriptions[i])
									);
									subscriptions[i]._state = actionAllowed
									                          ? TransmitterSubscriptionsConstants.POSITIV_RECEIP
									                          : TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
									if(actionAllowed) {
										subscriptions[i]._mainTransmitter = _transmitterId;
										if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
											if(subscriptions.length > 1) {
												SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
												if(subscriptionsFromApplicationStorage != null) {
													SendSubscriptionInfo _sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
													if(_sendSubscriptionInfo != null) {
														if(_sendSubscriptionInfo.isRequestSupported()) {
															if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
																_sendSubscriptionInfo.setLastTriggerRequest((byte)0);
																_connection.triggerSender(info, (byte)0); // start sending
															}
														}
													}
												}
											}
										}
									}
									else {
										subscriptions[i]._mainTransmitter = _transmitterId;
										SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
										if(subscriptionsFromApplicationStorage != null) {
											if(subscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
												SendSubscriptionInfo _sendSubscriptionInfo = subscriptionsFromApplicationStorage.getSendSubscription(info);
												if(_sendSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
													_connection.triggerSender(info, (byte)2); // stop sending no rights
													subscriptionsManager.removeSubscriptionRequest(subscriptions[i]);
													if(subscriptionsFromApplicationStorage != null) {
														subscriptionsFromApplicationStorage.unsubscribeSendData(info);
													}
												}
											}
											else {
												ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(
														info
												);
												if(receiveSubscriptionInfo != null) { //Fixme: Unnötige Prüfung?
													long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
													ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
															info, dataNumber, false, (byte)3, null, null, 1, 0, System.currentTimeMillis()
													);
													_connection.sendData(dataToSend);
													subscriptionsManager.removeSubscriptionRequest(subscriptions[i]);
													if(subscriptionsFromApplicationStorage != null) {
														subscriptionsFromApplicationStorage.unsubscribeReceiveData(info);
													}
												}
											}
										}
									}
								}
							}
						}
					}
					if(noOneInterrested) {
						if(sendSubscriptionInfo.isRequestSupported()) {
							if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
								sendSubscriptionInfo.setLastTriggerRequest((byte)1);
								connection.triggerSender(info, (byte)1); // Stop sending no one interrested
							}
						}
					}
					listsManager.addInfo(info);
				}
				else {
					// get the receiving component (See if main transmitter)
					SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = subscriptionsManager.getReceivingComponent(info);
					if(subscriptionsFromApplicationStorage == null) {
						subscriptionsFromApplicationStorage = subscriptionsManager.getSendingComponent(info);
					}
					if(subscriptionsFromApplicationStorage == null) {
						InAndOutSubscription inAndOutSubscription = subscriptionsManager.isSuccessfullySubscribed(
								info, TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION
						);
						byte state = 0;
						if(inAndOutSubscription != null) {
							state = inAndOutSubscription._state;
						}
						if(state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
							request._state = state;
							request._mainTransmitter = inAndOutSubscription._mainTransmitter;
							if(sendSubscriptionInfo.isSenderRequestSupported()) {
								if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
									sendSubscriptionInfo.setLastTriggerRequest((byte)0);
									connection.triggerSender(info, (byte)0); // Start sending
								}
							}
						}
						else if(subscriptionsManager.isUnsuccessfullySubscribed(
								info, TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION
						)) {
							request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
							request._mainTransmitter = -1;
							if(sendSubscriptionInfo.isSenderRequestSupported()) {
								if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
									sendSubscriptionInfo.setLastTriggerRequest((byte)1);
									connection.triggerSender(info, (byte)1); // Stop sending. No one interrested
								}
							}
						}
						else {
							makeOutgoingRequests(request);
							request._mainTransmitter = -1;
							if(request._outSubscriptions == null) {
								request._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
								if(sendSubscriptionInfo.isSenderRequestSupported()) {
									if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
										sendSubscriptionInfo.setLastTriggerRequest((byte)1);
										connection.triggerSender(info, (byte)1); // Stop sending. No one interrested
									}
								}
							}
							else if(subscriptionsManager.needToSendSubscription(_transmitterId, request)) {
								request._state = (byte)-1;
								for(int j = 0; j < request._outSubscriptions.length; ++j) {
									if(request._outSubscriptions[j] != null) {
										TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
												info,
												TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION,
												request._outSubscriptions[j]._transmittersToConsider
										);
										T_T_HighLevelCommunication _connection = getTransmitterConnection(
												request._outSubscriptions[j]._targetTransmitter
										);
										if(_connection != null) {
											_connection.sendTelegram(_subscription);
										}
									}
								}
							}
							else {
								request._outSubscriptions = null;
							}
						}
					}
					else {
						request._state = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
						request._mainTransmitter = _transmitterId;
						SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedReceivingComponent(info);
						if(subscriptionsFromRemoteStorages == null) { // Kein empfänger
							if(sendSubscriptionInfo.isRequestSupported()) {
								if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
									sendSubscriptionInfo.setLastTriggerRequest((byte)1);
									connection.triggerSender(info, (byte)1); // Stop sending no one interrested
								}
							}
						}
						else {
							if(sendSubscriptionInfo.isSenderRequestSupported()) {
								if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
									sendSubscriptionInfo.setLastTriggerRequest((byte)0);
									connection.triggerSender(info, (byte)0); // Start sending
								}
							}
						}
					}
				}
			}
		}
	}

	private UserAction getActionSourceSubscription(final InAndOutSubscription subscription) {
		if(subscription._role == 1) {
			// Empfänger
			return UserAction.RECEIVER;
		}
		else{
			return UserAction.SOURCE;
			
		}
	}

	/**
	 * Meldet die Daten ab und leitet die Abmeldungen, wenn nötig, an den beteiligten Datenverteiler. Diese Methode wird von der Protokollsteuerung DaV-DAF
	 * aufgerufen, wenn sich eine Applikation als Quelle oder Sender abmelden möchte. Im Falle einer Quellenabmeldung wird die Basisanmeldeinformation aus der
	 * Zuliefererdatenverwaltung entfernt, so dass dieser Datenverteiler nicht mehr der Zentraldatenverteiler der spezifizierten Daten ist. Danach werden alle von
	 * dieser gelöschten Anmeldung abhängigen Anmeldungen bearbeitet. Im Falle einer Senderabmeldung existieren zwei Möglichkeiten: Bei einer lokalen Senke wird
	 * die Senke benachrichtigt, dass kein Sender mer vorhanden ist. Wenn die Senken- oder die Empfangsapplikation bei einem anderen Datenverteiler angemeldet ist,
	 * dann wird zuerst ü?berpru? ob bei diesem Datenverteiler eine andere Sendeanmeldung auf diese Daten vorhanden ist. Wenn keine Anmeldung für die
	 * spezifizierten Daten existiert, dann wird der aktuelle Datensatz aus dem bestand entfernt.
	 *
	 * @param connection     Schnittstelle zwischen DAV und DAF
	 * @param unsubscription Abmeldetelegram der Applikation
	 */
	public synchronized void handleApplicationSendUnsubscription(
			T_A_HighLevelCommunication connection, SendUnsubscriptionTelegram unsubscription) {

		if(_applicationStatusUpdater != null) {
			_applicationStatusUpdater.applicationUnsubscribeConnection(connection);
		}

		long applicationId = connection.getId();
		byte subscriptionState = TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION;
		BaseSubscriptionInfo info = unsubscription.getUnSubscriptionInfo();
		InAndOutSubscription subscription = subscriptionsManager.removeSubscriptionRequest(
				applicationId, info, subscriptionState
		);

		SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage1 = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
		if(subscriptionsFromApplicationStorage1 != null) {
			SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage1.unsubscribeSendData(info);
			// Die Anmeldung der Applikation wurde zurück genommen -> Andere darüber informieren
			if(_applicationStatusUpdater != null) {
				_applicationStatusUpdater.applicationUnsubscribeConnection(connection);
			}
			if(sendSubscriptionInfo != null) {
				if(sendSubscriptionInfo.isSource()) {
					listsManager.removeInfo(info);
					InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(info);
					if(subscriptions != null) {
						OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
						for(int i = 0; i < subscriptions.length; ++i) {
							if(subscriptions[i] != null) {
								cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
								workInvolvedSubscription(subscriptions[i], subscriptionHelper);
							}
						}
						subscriptionHelper.flushOutUnSubscription(this);
					}
				}
				else if(subscription != null) {
					if(subscription._outSubscriptions == null) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsManager.getReceivingComponent(
								info
						);
						if(subscriptionsFromApplicationStorage != null) { // a drain?
							SubscriptionsFromRemoteStorage _subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedSendingComponent(info);
							if(_subscriptionsFromRemoteStorages == null) { //the one and only sender?
								T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)subscriptionsFromApplicationStorage.getConnection();
								if(_connection != null) {
									ReceiveSubscriptionInfo _receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(info);
									if(_receiveSubscriptionInfo != null) {
										long dataNumber = cacheManager.getCurrentDataIndex(info) + 1;
										if(_receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
											if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
												return;
											}
										}
										_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
										_receiveSubscriptionInfo.setLastErrorState((byte)2);
										ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
												info, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
										);
										_connection.sendData(dataToSend);
									}
								}
							}
						}
					}
					else {
						boolean done = false;
						if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
							InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
									subscription
							);
							if(alternativeSubscription != null) {
								alternativeSubscription._outSubscriptions = subscription._outSubscriptions;
								alternativeSubscription._state = subscription._state;
								alternativeSubscription._mainTransmitter = subscription._mainTransmitter;
								alternativeSubscription._transmittersToConsider = subscription._transmittersToConsider;
								alternativeSubscription._currentTransmittersToConsider = subscription._currentTransmittersToConsider;
								for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
									ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
											i
									);
									if(exchangeSubscriptionAction != null) {
										if(exchangeSubscriptionAction.oldSubscription == subscription) {
											exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
										}
									}
								}
								done = true;
							}
						}
						if(done) {
							return;
						}

						OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
						for(int i = 0; i < subscription._outSubscriptions.length; ++i) {
							if(subscription._outSubscriptions[i] != null) {
								subscriptionHelper.handleOutUnsubscription(subscription, subscription._outSubscriptions[i]);
							}
						}
						subscription._outSubscriptions = null;
						subscription._currentTransmittersToConsider = null;
						cleanPendingSubscriptionToRedirect(subscription, subscriptionHelper);
						InAndOutSubscription subscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(subscription);
						if(subscriptions != null) {
							for(int i = 0; i < subscriptions.length; ++i) {
								if(subscriptions[i] != null) {
									cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
									workInvolvedSubscription(subscriptions[i], subscriptionHelper);
								}
							}
						}
						subscriptionHelper.flushOutUnSubscription(this);
					}
				}
				// if no one interrested delete cached Data
				if(!subscriptionsManager.isSubscriptionRequestAvaillable(info)) {
					cacheManager.clean(info);
				}
			}
		}
	}

	/**
	 * Aktualisiert die Liste der auf eine positive Quitung wartenden Anmeldungen, die über einen neuen weg umgeleitet werden müssen.
	 *
	 * @param subscription Eintrag mit den Anmeldungsinformationen
	 */
	private void cleanPendingSubscriptionToRedirect(InAndOutSubscription subscription) {
		for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
			ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(i);
			if(exchangeSubscriptionAction != null) {
				if(exchangeSubscriptionAction.oldSubscription == subscription) {
					_pendingSubscriptionToRedirect.remove(i);
					InAndOutSubscription newSubscription = exchangeSubscriptionAction.newSubscription;
					if(newSubscription != null) {
						for(int j = 0; j < newSubscription._outSubscriptions.length; ++j) {
							if(newSubscription._outSubscriptions[j] != null) {
								TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
										newSubscription._baseSubscriptionInfo,
										newSubscription._role,
										newSubscription._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										newSubscription._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_unsubscription);
								}
							}
						}
					}
					break;
				}
			}
		}
	}

	/**
	 * Aktualisiert die Liste mit austehenden Anmeldungen TBD: noch nicht gut beschrieben
	 *
	 * @param subscription       Eintrag mit den Anmeldungsinformationen
	 * @param subscriptionHelper
	 */
	private void cleanPendingSubscriptionToRedirect(
			InAndOutSubscription subscription, OutSubscriptionsHelper subscriptionHelper) {
		if(Transmitter._debugLevel > 20) {
			System.err.println(
					"ConnectionsManager.cleanPendingSubscriptionToRedirect pendingSubscriptionToRedirect.size() = " + _pendingSubscriptionToRedirect.size()
			);
		}
		for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
			ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
					i
			);
			if(exchangeSubscriptionAction != null) {
				if(exchangeSubscriptionAction.oldSubscription == subscription) {
					_pendingSubscriptionToRedirect.remove(i);
					InAndOutSubscription newSubscription = exchangeSubscriptionAction.newSubscription;
					if(newSubscription != null) {
						for(int j = 0; j < newSubscription._outSubscriptions.length; ++j) {
							if(newSubscription._outSubscriptions[j] != null) {
								subscriptionHelper.handleOutUnsubscription(newSubscription, newSubscription._outSubscriptions[j]);
							}
						}
					}
					break;
				}
			}
		}
	}

	/**
	 * Behandelt eine Anmeldung nach einer Abmeldeaktion...
	 *
	 * @param involvedSubscription Eintrag mit den Anmeldungsinformationen
	 * @param subscriptionHelper
	 */
	private void workInvolvedSubscription(
			InAndOutSubscription involvedSubscription, OutSubscriptionsHelper subscriptionHelper) {
		if(Transmitter._debugLevel > 20) {
			System.err.println("ConnectionsManager.workInvolvedSubscription involvedSubscription = " + involvedSubscription);
		}
		if(involvedSubscription != null) {
			if(involvedSubscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
				//positiver status
				if(involvedSubscription._fromTransmitter) {
					T_T_HighLevelCommunication _connection = getTransmitterConnection(involvedSubscription._source);
					if(_connection != null) {
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
						if(subscriptionsFromDavStorage != null) {
							if(!subscriptionsManager.isSubscriptionRequestAvaillable(involvedSubscription)) {
								if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) {
									// anmeldestatus == Senderanmeldung
									subscriptionsFromDavStorage.unsubscribeSendData(involvedSubscription._baseSubscriptionInfo);
								}
								else {
									// anmeldestatus == emfängeranmeldung
									subscriptionsFromDavStorage.unsubscribeReceiveData(involvedSubscription._baseSubscriptionInfo);
								}
							}
						}
					}
				}
			}

			if(involvedSubscription._outSubscriptions != null) {
				for(int j = 0; j < involvedSubscription._outSubscriptions.length; ++j) {
					if(involvedSubscription._outSubscriptions[j] != null) {
						subscriptionHelper.handleOutUnsubscription(involvedSubscription, involvedSubscription._outSubscriptions[j]);
					}
				}
			}
			involvedSubscription._outSubscriptions = null;
			involvedSubscription._currentTransmittersToConsider = null;
			involvedSubscription._state = -1;

			long potentialTransmitters[] = null;
			if(involvedSubscription._fromTransmitter) {
				long ids[] = involvedSubscription._transmittersToConsider;
				boolean found = false;
				if(ids != null) {
					for(int i = 0; i < ids.length; ++i) {
						if(ids[i] == _transmitterId) {
							found = true;
							break;
						}
					}
				}
				if(found) {
					potentialTransmitters = new long[ids.length - 1];
					for(int i = 0, j = 0; i < ids.length; ++i) {
						if(ids[i] == _transmitterId) {
							continue;
						}
						potentialTransmitters[j++] = ids[i];
					}
				}
				else {
					potentialTransmitters = ids;
				}
			}
			else {
				potentialTransmitters = listsManager.getPotentialTransmitters(involvedSubscription._baseSubscriptionInfo);
			}
//			if(Transmitter._debugLevel > 20) {
//				System.err.println("potentialTransmitters = " + Arrays.toString(potentialTransmitters));
//			}
			if(potentialTransmitters == null) {
				involvedSubscription._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
				if(involvedSubscription._fromTransmitter) {
					TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
							involvedSubscription._baseSubscriptionInfo,
							involvedSubscription._role,
							TransmitterSubscriptionsConstants.NEGATIV_RECEIP,
							-1,
							involvedSubscription._transmittersToConsider
					);
					T_T_HighLevelCommunication _connection = getTransmitterConnection(involvedSubscription._source);
					if(_connection != null) {
						_connection.sendTelegram(negativReceipt);
					}
				}
				else {
					T_A_HighLevelCommunication _connection = getApplicationConnection(involvedSubscription._source);
					if(_connection != null) {
						SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
						if(tmpSubscriptionsFromApplicationStorage != null) {
							if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
								SendSubscriptionInfo _sendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
										involvedSubscription._baseSubscriptionInfo
								);
								if(_sendSubscriptionInfo != null) {
									if(_sendSubscriptionInfo.isRequestSupported()) {
										if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
											_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
											_connection.triggerSender(involvedSubscription._baseSubscriptionInfo, (byte)1);// stop sending
										}
									}
								}
							}
							else { // receiving
								ReceiveSubscriptionInfo _receiveSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getReceiveSubscription(
										involvedSubscription._baseSubscriptionInfo
								);
								if(_receiveSubscriptionInfo != null) {
									long dataNumber = cacheManager.getCurrentDataIndex(involvedSubscription._baseSubscriptionInfo) + 1;
//									

									final long lastDataIndex = _receiveSubscriptionInfo.getLastDataIndex();
									if(lastDataIndex == dataNumber) {
										if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
											return;
										}
									}
									_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
									_receiveSubscriptionInfo.setLastErrorState((byte)2);
									ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
											involvedSubscription._baseSubscriptionInfo, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
									);
									_connection.sendData(dataToSend);
								}
							}
						}
					}
				}
				return;
			}

			InAndOutSubscription inAndOutSubscription = subscriptionsManager.isSuccessfullySubscribed(
					involvedSubscription._baseSubscriptionInfo, involvedSubscription._role, _transmitterId, potentialTransmitters
			);
			byte receip = 0;
			if(inAndOutSubscription != null) {
				receip = inAndOutSubscription._state;
			}
			if(receip == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
				involvedSubscription._state = receip;
				involvedSubscription._mainTransmitter = inAndOutSubscription._mainTransmitter;
				if(involvedSubscription._fromTransmitter) {
					TransmitterDataSubscriptionReceipt positivReceipt = new TransmitterDataSubscriptionReceipt(
							involvedSubscription._baseSubscriptionInfo,
							involvedSubscription._role,
							receip,
							inAndOutSubscription._mainTransmitter,
							involvedSubscription._transmittersToConsider
					);
					T_T_HighLevelCommunication _connection = getTransmitterConnection(involvedSubscription._source);
					if(_connection != null) {
						_connection.sendTelegram(positivReceipt);
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)_connection.getSubscriptionsFromRemoteStorage();
						if(subscriptionsFromDavStorage != null) {
							TransmitterDataSubscription dataSubscription = new TransmitterDataSubscription(
									involvedSubscription._baseSubscriptionInfo, involvedSubscription._role, involvedSubscription._transmittersToConsider
							);
							if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
								subscriptionsFromDavStorage.subscribeSendData(dataSubscription);
							}
							else { // receiving
								subscriptionsFromDavStorage.subscribeReceiveData(dataSubscription);
								TransmitterDataTelegram dataToSend[] = cacheManager.getCurrentDataForTransmitter(
										involvedSubscription._baseSubscriptionInfo
								);
								if(dataToSend != null) {
									_connection.sendTelegrams(dataToSend);
								}
							}
						}
					}
				}
				else {
					if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
						T_A_HighLevelCommunication _connection = getApplicationConnection(involvedSubscription._source);
						if(_connection != null) {
							SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
							if(tmpSubscriptionsFromApplicationStorage != null) {
								SendSubscriptionInfo _sendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
										involvedSubscription._baseSubscriptionInfo
								);
								if(_sendSubscriptionInfo != null) {
									if(_sendSubscriptionInfo.isRequestSupported()) {
										if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)0) {
											if(authentificationManager.isActionAllowed(
													_connection.getRemoteUserId(),
													involvedSubscription._baseSubscriptionInfo,
													tmpSubscriptionsFromApplicationStorage.canSend(involvedSubscription._baseSubscriptionInfo)
													? UserAction.SENDER
													: UserAction.SOURCE
											)) {
												_sendSubscriptionInfo.setLastTriggerRequest((byte)0);
												_connection.triggerSender(involvedSubscription._baseSubscriptionInfo, (byte)0);// start sending
											}
										}
									}
								}
							}
						}
					}
					else { // receiving
						T_A_HighLevelCommunication _connection = getApplicationConnection(involvedSubscription._source);
						if(_connection != null) {
							try {
								subscriptionsManager.sendDataToT_A_Component(
										(SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage(),
										involvedSubscription._baseSubscriptionInfo
								);
							}
							catch(ConfigurationException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
			else if(subscriptionsManager.isUnsuccessfullySubscribed(
					involvedSubscription._baseSubscriptionInfo, involvedSubscription._role, _transmitterId, potentialTransmitters
			)) {
				involvedSubscription._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
				involvedSubscription._mainTransmitter = -1;
				if(involvedSubscription._fromTransmitter) {
					TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
							involvedSubscription._baseSubscriptionInfo,
							involvedSubscription._role,
							TransmitterSubscriptionsConstants.NEGATIV_RECEIP,
							-1,
							involvedSubscription._transmittersToConsider
					);
					T_T_HighLevelCommunication _connection = getTransmitterConnection(involvedSubscription._source);
					if(_connection != null) {
						_connection.sendTelegram(negativReceipt);
					}
				}
				else {
					T_A_HighLevelCommunication _connection = getApplicationConnection(involvedSubscription._source);
					if(_connection != null) {
						SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
						if(tmpSubscriptionsFromApplicationStorage != null) {
							if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
								SendSubscriptionInfo _sendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
										involvedSubscription._baseSubscriptionInfo
								);
								if(_sendSubscriptionInfo != null) {
									if(_sendSubscriptionInfo.isRequestSupported()) {
										if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
											_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
											_connection.triggerSender(involvedSubscription._baseSubscriptionInfo, (byte)1);// stop sending
										}
									}
								}
							}
							else { // receiving
								ReceiveSubscriptionInfo _receiveSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getReceiveSubscription(
										involvedSubscription._baseSubscriptionInfo
								);
								if(_receiveSubscriptionInfo != null) {
									long dataNumber = cacheManager.getCurrentDataIndex(involvedSubscription._baseSubscriptionInfo) + 1;
									if(_receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
										if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
											return;
										}
									}
									_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
									_receiveSubscriptionInfo.setLastErrorState((byte)2);
									ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
											involvedSubscription._baseSubscriptionInfo, dataNumber, false, (byte)2, null, null, 1, 0, System.currentTimeMillis()
									);
									_connection.sendData(dataToSend);
								}
							}
						}
					}
				}
			}
			else {
				makeOutgoingRequests(involvedSubscription);
				involvedSubscription._mainTransmitter = -1;
				if(involvedSubscription._outSubscriptions == null) {
					involvedSubscription._currentTransmittersToConsider = null;
					involvedSubscription._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
					if(involvedSubscription._fromTransmitter) {
						// rs: Anmeldung stammt von einem fremden DaV, also negative Quittung versenden
						TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
								involvedSubscription._baseSubscriptionInfo,
								involvedSubscription._role,
								TransmitterSubscriptionsConstants.NEGATIV_RECEIP,
								-1,
								involvedSubscription._transmittersToConsider
						);
						T_T_HighLevelCommunication _connection = getTransmitterConnection(involvedSubscription._source);
						if(_connection != null) {
							_connection.sendTelegram(negativReceipt);
						}
					}
					else {
						// rs: Anmeldung stammt von einer lokalen Applikation, also negative Sendesteuerung bzw. "keine Quelle" versenden
						if(Transmitter._debugLevel > 20) System.err.println("ConnectionsManager.workInvolvedSubscription T_A");
						T_A_HighLevelCommunication _connection = getApplicationConnection(involvedSubscription._source);
						if(_connection != null) {
							SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
							if(tmpSubscriptionsFromApplicationStorage != null) {
								if(involvedSubscription._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
									SendSubscriptionInfo _sendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
											involvedSubscription._baseSubscriptionInfo
									);
									if(_sendSubscriptionInfo != null) {
										if(_sendSubscriptionInfo.isRequestSupported()) {
											if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
												_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
												if(Transmitter._debugLevel > 20) {
													System.err.println("ConnectionsManager.workInvolvedSubscription triggerSender");
												}
												_connection.triggerSender(involvedSubscription._baseSubscriptionInfo, (byte)1);// stop sending
											}
										}
									}
								}
								else { // receiving
									ReceiveSubscriptionInfo _receiveSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getReceiveSubscription(
											involvedSubscription._baseSubscriptionInfo
									);
									if(_receiveSubscriptionInfo != null) {
										long dataNumber = cacheManager.getCurrentDataIndex(involvedSubscription._baseSubscriptionInfo) + 1;
										if(_receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
											if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
												return;
											}
										}
										_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
										_receiveSubscriptionInfo.setLastErrorState((byte)2);
										ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
												involvedSubscription._baseSubscriptionInfo,
												dataNumber,
												false,
												(byte)2,
												null,
												null,
												1,
												0,
												System.currentTimeMillis()
										);
										_connection.sendData(dataToSend);
									}
								}
							}
						}
					}
				}
				else if(subscriptionsManager.needToSendSubscription(_transmitterId, involvedSubscription)) {
					involvedSubscription._state = (byte)-1;
					for(int j = 0; j < involvedSubscription._outSubscriptions.length; ++j) {
						if(involvedSubscription._outSubscriptions[j] != null) {
							subscriptionHelper.handleOutSubscription(involvedSubscription, involvedSubscription._outSubscriptions[j]);
						}
					}
				}
				else {
					involvedSubscription._outSubscriptions = null;
				}
			}
		}
	}

	/**
	 * Erzeugt die notwendigen Informationen zu den Anmeldungen, die verschickt werden.
	 * <p/>
	 * Es wird eine Liste angelegt, mit allen potentiellen Zulieferdaentenverteiler eines bestimmten Datums. Wenn es sich um eine DAV-DAV Verbindung handelt, wird
	 * die komplette Liste mit Ausnahme der eigenen Adresse(<code>transmitterId</code>) in die Liste der potentiellen DAVs kopiert. Handelt es sich nicht um eine
	 * Transmitterverbindung, so wird die Liste der erreichbaren Transmitter über <code>listsManager.getPotentialTransmitters()</code> geholt. Anscließend wird die
	 * Routine <code>bestWayManager</code> aufgerufen, um die besten Wege von DAV zu DAV zu finden. Anscjhließend werden die von einem DAV erreichbaren DAVs in die
	 * Liste <code>outSubscription</code> gespeichert.
	 *
	 * @param request Anmeldungsinformationen
	 */
	private void makeOutgoingRequests(InAndOutSubscription request) {
		if(request == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		long[] potentialTransmitters;
		if(request._fromTransmitter) {
			long[] ids = request._transmittersToConsider;
			boolean found = false;
			if(ids != null) {
				for(int i = 0; i < ids.length; ++i) {
					if(ids[i] == _transmitterId) {
						found = true;
						break;
					}
				}
			}
			if(found) {
				potentialTransmitters = new long[ids.length - 1];
				int j = 0;
				for(int i = 0; i < ids.length; ++i) {
					if(ids[i] == _transmitterId) {
						continue;
					}
					potentialTransmitters[j++] = ids[i];
				}
			}
			else {
				potentialTransmitters = ids;
			}
		}
		else {
			potentialTransmitters = listsManager.getPotentialTransmitters(request._baseSubscriptionInfo);
		}

		request._currentTransmittersToConsider = potentialTransmitters;
		if(potentialTransmitters == null || potentialTransmitters.length == 0) {
			request._outSubscriptions = null;
		}
		else {
			Hashtable<Long, ArrayList<Long>> tmpTable = new Hashtable<Long, ArrayList<Long>>();
			ArrayList<Long> keys = new ArrayList<Long>();
//
			for(int j = 0; j < potentialTransmitters.length; ++j) {
				Long tmpLong = potentialTransmitters[j];
				if(potentialTransmitters[j] == _transmitterId) {
					continue;
				}
				if(potentialTransmitters[j] == request._source) {
					continue;
				}
				long hightway = bestWayManager.getBestWay(potentialTransmitters[j]);
				if((hightway < 0) || (getTransmitterConnection(hightway) == null) || (hightway == request._source)) {
					continue;
				}
				Long target = hightway;
				ArrayList<Long> list = tmpTable.get(target);
				if(list == null) {
					list = new ArrayList<Long>();
					tmpTable.put(target, list);
					keys.add(target);
				}
				list.add(tmpLong);
			}
			InAndOutSubscription.OutSubscription outSubscriptions[] = null;
			int size = keys.size();
			if(size > 0) {
				outSubscriptions = new InAndOutSubscription.OutSubscription[size];
				for(int j = 0; j < size; ++j) {
					Long key = keys.get(j);
					ArrayList<Long> list = tmpTable.get(key);
					if(list != null) {
						int listSize = list.size();
						if(listSize > 0) {
							long[] _ids = new long[listSize];
							for(int k = 0; k < listSize; ++k) {
								_ids[k] = list.get(k);
							}
							outSubscriptions[j] = request.newOutSubscription(key, _ids, (byte)-1);
						}
					}
				}
			}
			request._outSubscriptions = outSubscriptions;
			if(outSubscriptions == null) {
				request._currentTransmittersToConsider = null;
			}
		}
	}

	/**
	 * Entfernt der gegebene Applikationsverbindung aus der Verbindungsverwaltung.
	 * <p/>
	 * Diese Methode wird von der Protokollsteuerung DaV-DAF aufgerufen, wenn eine Applikation terminiert. Der Repräsentant der Applikationsverbindung wird aus der
	 * Verbindungsverwaltung entfernt. Handelt es sich bei der Applikation um die lokale Konfiguration, dann wird dieser Datenverteiler terminiert. Die
	 * close-Methode wird aufgerufen. Ansonsten werden zuerst alle von der Applikation stammenden Anmeldungen aus der Anmeldungsverwaltung entfernt und einzelnen
	 * bearbeitet. Wenn eine Anmeldung eine Quell - oder Senderanmeldung ist, dann werden die Aktionen durchgeführt, die unter handleApplicationSendUnsubscription
	 * beschrieben sind. Handelt es sich um eine Senken- oder Empfangsanmeldung, dann werden die Aktionen durchgeführt, die unter
	 * handleApplicationReceiveUnsubscription beschrieben sind.
	 *
	 * @param connection die zu entfernende Verbindung zwischen DAV und Application
	 */
	public void unsubscribeConnection(T_A_HighLevelCommunicationInterface connection) {
		if(connection == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(_closing) return;
		if(connection.isConfiguration()) {
			close(true, "Die Konfiguration ist nicht mehr vorhanden. Der Datenverteiler wird jetzt beendet");
		}
		else {
			final SystemObject applicationObject;
			synchronized(this) {
				if(_closing) return;
				long source = connection.getId();
				applicationObject = removeApplicationConnection(source);
				InAndOutSubscription subscriptions[] = subscriptionsManager.removeSubscriptionRequests(source);
				SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)connection.getSubscriptionsFromRemoteStorage();
				if(subscriptionsFromApplicationStorage != null) {
					if(subscriptions != null) {
						OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
						// Alle Anmeldungen der betroffenen Applikation betrachten
						for(int i = 0; i < subscriptions.length; ++i) {
							if((subscriptions[i] != null) && (subscriptions[i]._baseSubscriptionInfo != null)) {
								if(subscriptions[i]._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
									// Empfangsanmeldung seitens der betroffenen Anmeldung
									ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.unsubscribeReceiveData(
											subscriptions[i]._baseSubscriptionInfo
									);
									if(receiveSubscriptionInfo != null) {
										if(receiveSubscriptionInfo.isDrain()) {
											listsManager.removeInfo(subscriptions[i]._baseSubscriptionInfo);
											InAndOutSubscription[] subscriptionsToWork = subscriptionsManager.getInvolvedSubscriptionRequests(
													subscriptions[i]._baseSubscriptionInfo
											);
											if(subscriptionsToWork != null) {
												for(int j = 0; j < subscriptionsToWork.length; ++j) {
													if(subscriptionsToWork[j] != null) {
														cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
														workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
													}
												}
											}
										}
										else {
											SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedReceivingComponent(
													subscriptions[i]._baseSubscriptionInfo
											);
											if(subscriptionsFromRemoteStorages == null) {
												SubscriptionsFromApplicationStorage senderSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsManager.getSendingComponent(
														subscriptions[i]._baseSubscriptionInfo
												);
												if(senderSubscriptionsFromApplicationStorage != null) {
													SendSubscriptionInfo sendSubscriptionInfo = senderSubscriptionsFromApplicationStorage.getSendSubscription(
															subscriptions[i]._baseSubscriptionInfo
													);
													if((sendSubscriptionInfo != null) && (sendSubscriptionInfo.isRequestSupported())) {
														if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
															T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)senderSubscriptionsFromApplicationStorage.getConnection();
															if(_connection != null) {
																sendSubscriptionInfo.setLastTriggerRequest((byte)1);
																_connection.triggerSender(subscriptions[i]._baseSubscriptionInfo, (byte)1);
															}
														}
													}
												}
											}
											if(subscriptions[i]._outSubscriptions != null) {
												boolean done = false;
												if(subscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
													InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
															subscriptions[i]
													);
													if(alternativeSubscription != null) {
														alternativeSubscription._outSubscriptions = subscriptions[i]._outSubscriptions;
														alternativeSubscription._state = subscriptions[i]._state;
														alternativeSubscription._mainTransmitter = subscriptions[i]._mainTransmitter;
														alternativeSubscription._transmittersToConsider = subscriptions[i]._transmittersToConsider;
														alternativeSubscription._currentTransmittersToConsider = subscriptions[i]._currentTransmittersToConsider;
														for(int j = _pendingSubscriptionToRedirect.size() - 1; j > -1; --j) {
															ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
																	j
															);
															if(exchangeSubscriptionAction != null) {
																if(exchangeSubscriptionAction.oldSubscription == subscriptions[i]) {
																	exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
																}
															}
														}
														done = true;
													}
												}
												if(done) {
													continue;
												}
												for(int j = 0; j < subscriptions[i]._outSubscriptions.length; ++j) {
													if(subscriptions[i]._outSubscriptions[j] != null) {
														subscriptionHelper.handleOutUnsubscription(subscriptions[i], subscriptions[i]._outSubscriptions[j]);
													}
												}
												subscriptions[i]._outSubscriptions = null;
												subscriptions[i]._currentTransmittersToConsider = null;
												cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
												InAndOutSubscription[] subscriptionsToWork = subscriptionsManager.getInvolvedSubscriptionRequests(
														subscriptions[i]
												);
												// FIXME: Doppelter Code
												if(subscriptionsToWork != null) {
													for(int j = 0; j < subscriptionsToWork.length; ++j) {
														if(subscriptionsToWork[j] != null) {
															cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
															workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
														}
													}
												}
											}
										}
									}
								}
								else {
									// Betrachtete Anmeldung der betroffenen Applikation wurde als Quelle oder Sender gemacht
									SendSubscriptionInfo sendSubscriptionInfo = subscriptionsFromApplicationStorage.unsubscribeSendData(
											subscriptions[i]._baseSubscriptionInfo
									);
									if(sendSubscriptionInfo != null) {
										if(sendSubscriptionInfo.isSource()) {
											// Betrachtete Anmeldung der betroffenen Applikation wurde als Quelle gemacht
											listsManager.removeInfo(subscriptions[i]._baseSubscriptionInfo);
											InAndOutSubscription[] subscriptionsToWork = subscriptionsManager.getInvolvedSubscriptionRequests(
													subscriptions[i]._baseSubscriptionInfo
											);
											if(subscriptionsToWork != null) {
												// Abhängige Anmeldungen von anderen Applikationen bzw. Datenverteilern betrachten
												for(int j = 0; j < subscriptionsToWork.length; ++j) {
													if(subscriptionsToWork[j] != null) {
														cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
														workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
													}
												}
											}
										}
										else {
											if(subscriptions[i]._outSubscriptions != null) {
												boolean done = false;
												if(subscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
													InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
															subscriptions[i]
													);
													if(alternativeSubscription != null) {
														alternativeSubscription._outSubscriptions = subscriptions[i]._outSubscriptions;
														alternativeSubscription._state = subscriptions[i]._state;
														alternativeSubscription._mainTransmitter = subscriptions[i]._mainTransmitter;
														alternativeSubscription._transmittersToConsider = subscriptions[i]._transmittersToConsider;
														alternativeSubscription._currentTransmittersToConsider = subscriptions[i]._currentTransmittersToConsider;
														for(int j = _pendingSubscriptionToRedirect.size() - 1; j > -1; --j) {
															ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
																	j
															);
															if(exchangeSubscriptionAction != null) {
																if(exchangeSubscriptionAction.oldSubscription == subscriptions[i]) {
																	exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
																}
															}
														}
														done = true;
													}
												}
												if(done) {
													continue;
												}
												for(int j = 0; j < subscriptions[i]._outSubscriptions.length; ++j) {
													if(subscriptions[i]._outSubscriptions[j] != null) {
														subscriptionHelper.handleOutUnsubscription(subscriptions[i], subscriptions[i]._outSubscriptions[j]);
													}
												}
												subscriptions[i]._outSubscriptions = null;
												subscriptions[i]._currentTransmittersToConsider = null;
												cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
												InAndOutSubscription[] subscriptionsToWork = subscriptionsManager.getInvolvedSubscriptionRequests(
														subscriptions[i]
												);
												if(subscriptionsToWork != null) {
													for(int j = 0; j < subscriptionsToWork.length; ++j) {
														if(subscriptionsToWork[j] != null) {
															cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
															workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
														}
													}
												}
											}
										}
									}
								}
								// if no one interrested delete cached Data
								if(!subscriptionsManager.isSubscriptionRequestAvaillable(subscriptions[i]._baseSubscriptionInfo)) {
									cacheManager.clean(subscriptions[i]._baseSubscriptionInfo);
								}
							}
						}
						subscriptionHelper.flushOutUnSubscription(this);
					}
				}
			}
			if(applicationObject != null) {
				try {
					applicationObject.invalidate();
				}
				catch(ConfigurationChangeException e) {
					_debug.warning("Applikationsobjekt " + applicationObject + " konnte nicht nicht gelöscht werden", e);
				}
			}
		}
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn ein Datenverteiler terminiert. Der Repräsentant der  Datenverteilerverbindung wird
	 * aus der Verbindungsverwaltung entfernt. Danach werden alle von der spezifizierten Datenverteiler stammenden Anmeldungen ermittelt und einzeln bearbeitet. Es
	 * werden für jede einzelne diese Anmeldungen alle abhängigen Anmeldungen ermittelt und jede einzelne von ihnen wird wie folgt bearbeitet: Bei einer
	 * Empfangsanmeldung wird ein spezieller Datensatz generiert, aus dem hervorgeht, dass keine Quelle mehr vorhanden ist und zum Initiator weitergeleitet. Wenn
	 * diese Verbindung eine Hauptverbindung, also keine Ersatzverbindung, ist, dann wird ihr Repräsentant zur Verbindungsverwaltung hinzugefügt. Ein Thread im
	 * Hintergrund startet dann bei Bedarf die Ersatzverbindungen und überwacht, ob diese Hauptverbindung später wieder verfügbar ist. Dann werden die
	 * Ersatzverbindungen automatisch wieder abgebaut
	 *
	 * @param connection die zu entfernende Verbindung zwischen DAV und DAV
	 */
	public final void unsubscribeConnection(T_T_HighLevelCommunicationInterface connection) {
		if(connection == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(_closing || (_transmitterConnections == null)) {
			return;
		}
		long connectedTransmitterId = connection.getId();
		_debug.info("Verbindung zum Datenverteiler " + connectedTransmitterId + " abgebaut");
		synchronized(this) {
			removeTransmitterConnection(connectedTransmitterId);
			bestWayManager.handleDisconnection(connection);

			_debug.fine("Anmeldungen bei Datenverteiler " + connectedTransmitterId + " werden aufgegeben");

			OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
			InAndOutSubscription subscriptions[] = subscriptionsManager.removeSubscriptionRequests(connectedTransmitterId);
			if(subscriptions != null) {
				for(int i = 0; i < subscriptions.length; ++i) {
					if((subscriptions[i] != null) && (subscriptions[i]._baseSubscriptionInfo != null)) {
						if(subscriptions[i]._outSubscriptions != null) {
							boolean done = false;
							if(subscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
								InAndOutSubscription alternativeSubscription = subscriptionsManager.getAlternativeSubscriptionRequest(
										subscriptions[i]
								);
								if(alternativeSubscription != null) {
									alternativeSubscription._outSubscriptions = subscriptions[i]._outSubscriptions;
									alternativeSubscription._state = subscriptions[i]._state;
									alternativeSubscription._mainTransmitter = subscriptions[i]._mainTransmitter;
									alternativeSubscription._transmittersToConsider = subscriptions[i]._transmittersToConsider;
									alternativeSubscription._currentTransmittersToConsider = subscriptions[i]._currentTransmittersToConsider;
									for(int j = _pendingSubscriptionToRedirect.size() - 1; j > -1; --j) {
										ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
												j
										);
										if(exchangeSubscriptionAction != null) {
											if(exchangeSubscriptionAction.oldSubscription == subscriptions[i]) {
												exchangeSubscriptionAction.oldSubscription = alternativeSubscription;
											}
										}
									}
									done = true;
								}
							}
							if(done) {
								continue;
							}
							for(int j = 0; j < subscriptions[i]._outSubscriptions.length; ++j) {
								if(subscriptions[i]._outSubscriptions[j] != null) {
									subscriptionHelper.handleOutUnsubscription(subscriptions[i], subscriptions[i]._outSubscriptions[j]);
								}
							}
							subscriptions[i]._outSubscriptions = null;
							subscriptions[i]._currentTransmittersToConsider = null;
						}

						cleanPendingSubscriptionToRedirect(subscriptions[i], subscriptionHelper);
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)connection.getSubscriptionsFromRemoteStorage();
						if(subscriptionsFromDavStorage != null) {
							if(subscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
								if(subscriptions[i]._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
									subscriptionsFromDavStorage.unsubscribeReceiveData(subscriptions[i]._baseSubscriptionInfo);
								}
								else {
									subscriptionsFromDavStorage.unsubscribeSendData(subscriptions[i]._baseSubscriptionInfo);
								}
							}
						}

						SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = subscriptionsManager.getInterrestedReceivingComponent(
								subscriptions[i]._baseSubscriptionInfo
						);
						if(subscriptionsFromRemoteStorages == null) {
							SubscriptionsFromApplicationStorage senderSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsManager.getSendingComponent(
									subscriptions[i]._baseSubscriptionInfo
							);
							if(senderSubscriptionsFromApplicationStorage != null) {
								SendSubscriptionInfo sendSubscriptionInfo = senderSubscriptionsFromApplicationStorage.getSendSubscription(
										subscriptions[i]._baseSubscriptionInfo
								);
								if((sendSubscriptionInfo != null) && (sendSubscriptionInfo.isRequestSupported())) {
									if(sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
										T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)senderSubscriptionsFromApplicationStorage.getConnection();
										if(_connection != null) {
											sendSubscriptionInfo.setLastTriggerRequest((byte)1);
											_connection.triggerSender(subscriptions[i]._baseSubscriptionInfo, (byte)1);
										}
									}
								}
							}
						}

						if((subscriptionsManager.getSendingComponent(subscriptions[i]._baseSubscriptionInfo) == null) && (
								subscriptionsManager.getReceivingComponent(subscriptions[i]._baseSubscriptionInfo) == null)) { // not main transmitter
							InAndOutSubscription subscriptionsToWork[] = subscriptionsManager.getInvolvedSubscriptionRequests(
									subscriptions[i]
							);
							if(subscriptionsToWork != null) {
								for(int j = 0; j < subscriptionsToWork.length; ++j) {
									if(subscriptionsToWork[j] != null) {
										cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
										workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
									}
								}
							}
						}

						// if no one interrested delete cached Data
						if(!subscriptionsManager.isSubscriptionRequestAvaillable(subscriptions[i]._baseSubscriptionInfo)) {
							cacheManager.clean(subscriptions[i]._baseSubscriptionInfo);
						}
					}
				}
			}

			long connectionId = connection.getId();
			subscriptions = subscriptionsManager.getAffectedOutgoingSubscriptions(connectionId);
			if(subscriptions != null) {
				for(int i = 0; i < subscriptions.length; ++i) {
					if(subscriptions[i] != null) {
						InAndOutSubscription subscriptionsToWork[] = subscriptionsManager.getInvolvedSubscriptionRequests(
								subscriptions[i]
						);
						if(subscriptionsToWork != null) {
							for(int j = 0; j < subscriptionsToWork.length; ++j) {
								if(subscriptionsToWork[j] != null) {
									if(subscriptionsToWork[j]._role == TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION) {
										if(subscriptionsToWork[j]._fromTransmitter) {
											T_T_HighLevelCommunication _connection = getTransmitterConnection(
													subscriptionsToWork[j]._source
											);
											if(_connection != null) {
												long dataNumber = cacheManager.getCurrentDataIndex(subscriptionsToWork[j]._baseSubscriptionInfo) + 1;
												TransmitterDataTelegram dataToSend = new TransmitterDataTelegram(
														subscriptionsToWork[j]._baseSubscriptionInfo,
														dataNumber,
														false,
														(byte)2,
														null,
														null,
														1,
														0,
														System.currentTimeMillis(),
														(byte)1
												);
												_connection.sendTelegram(dataToSend);
											}
										}
										else {
											T_A_HighLevelCommunication _connection = getApplicationConnection(
													subscriptionsToWork[j]._source
											);
											if(_connection != null) {
												SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
												if(tmpSubscriptionsFromApplicationStorage != null) {
													ReceiveSubscriptionInfo receiveSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getReceiveSubscription(
															subscriptionsToWork[j]._baseSubscriptionInfo
													);
													if(receiveSubscriptionInfo != null) {
														long dataNumber = cacheManager.getCurrentDataIndex(subscriptionsToWork[j]._baseSubscriptionInfo) + 1;
														if(receiveSubscriptionInfo.getLastDataIndex() != dataNumber
														   || receiveSubscriptionInfo.getLastErrorState() != 2) {
															receiveSubscriptionInfo.setLastDataIndex(dataNumber);
															receiveSubscriptionInfo.setLastErrorState((byte)2);
															ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
																	subscriptionsToWork[j]._baseSubscriptionInfo,
																	dataNumber,
																	false,
																	(byte)2,
																	null,
																	null,
																	1,
																	0,
																	System.currentTimeMillis()
															);
															_connection.sendData(dataToSend);
														}
													}
												}
											}
										}
									}

									cleanPendingSubscriptionToRedirect(subscriptionsToWork[j], subscriptionHelper);
									workInvolvedSubscription(subscriptionsToWork[j], subscriptionHelper);
								}
							}
						}
					}
				}
			}
			subscriptionHelper.flushOutUnSubscription(this);
		}

		_debug.fine("Liste der unterbrochenen Verbindungen wird aktualisiert für Datenverteiler " + connectedTransmitterId);

		synchronized(missedConnectionInfos) {
			if(_transmitterConnectionInfos != null) {
				// see if we need to make a substitute for this connection
				for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
					if((_transmitterConnectionInfos[i] == null) || _transmitterConnectionInfos[i].isExchangeConnection()) {
						continue;
					}
					TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
					if((t1 != null) && (t1.getTransmitterId() == _transmitterId)) {
						TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
						if((t2 != null) && (t2.getTransmitterId() == connectedTransmitterId)) {
							missedConnectionInfos.add(_transmitterConnectionInfos[i]);
							missedConnectionInfos.notifyAll();
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc} Diese Methode wird von der Protokollsteuerung DaV-DaV, der Anmeldungsverwaltung und von dieser Komponente selber aufgerufen. Sie dient als
	 * Hilfsfunktion, um eine in der Verbindungsverwaltung registrierte Datenverteilerverbindung ausfindig zu machen.
	 */
	public T_T_HighLevelCommunication getTransmitterConnection(long communicationTransmitterId) {
		for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
			if(transmitterConnection != null) {
				if(transmitterConnection.getId() == communicationTransmitterId) {
					return transmitterConnection;
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DAF, der Anmeldungsverwaltung und von dieser Komponente selber aufgerufen. Sie dient als Hilfsfunktion, um
	 * eine in der Verbindungsverwaltung registrierte Applikationsverbindung ausfindig zu machen.
	 *
	 * @param applicationId die Id der verbundenen Applikation
	 *
	 * @return registrierte Applikationsverbindung
	 */
	final T_A_HighLevelCommunication getApplicationConnection(long applicationId) {
		for(T_A_HighLevelCommunication applicationConnection : _applicationConnections) {
			if(applicationConnection != null) {
				if(applicationConnection.getId() == applicationId) {
					return applicationConnection;
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn eine Anmeldung einer Zuliefererinformation eines Datenverteilers angekommen ist. Sie
	 * ruft die Methode subscribe der Zuliefererdatenverwaltung auf.
	 *
	 * @param connection                   Verbindung zwischen DAV und DAV
	 * @param transmitterListsSubscription AnmeldungsListe des Nachbardatenverteilers
	 */
	public final synchronized void handleListsSubscription(
			T_T_HighLevelCommunicationInterface connection, TransmitterListsSubscription transmitterListsSubscription) {
		if((connection == null) || (transmitterListsSubscription == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(listsManager != null) {
			listsManager.subscribe(connection.getId(), transmitterListsSubscription.getTransmitterList());
		}
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn eine Abmeldung einer Zuliefererinformation eines Datenverteilers angekommen ist. Sie
	 * ruft die Methode unsubscribe der Zuliefererdatenverwaltung auf.
	 *
	 * @param connection                     Verbindung zwischen DAV und DAV
	 * @param transmitterListsUnsubscription AbmeldungsListe des Nachbardatenverteilers
	 */
	public final synchronized void handleListsUnsubscription(
			T_T_HighLevelCommunicationInterface connection, TransmitterListsUnsubscription transmitterListsUnsubscription) {
		if((connection == null) || (transmitterListsUnsubscription == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(listsManager != null) {
			listsManager.unsubscribe(connection.getId(), transmitterListsUnsubscription.getTransmitterList());
		}
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn eine Kündigung einer Anmeldung einer Zuliefererinformation eines Datenverteilers
	 * angekommen ist. Sie ruft die Methode unsubscribeDeliverer der Anmeldelistenverwaltung auf.
	 *
	 * @param connection Verbindung zwischen DAV und DAV
	 * @param transmitterListsDeliveryUnsubscription
	 *                   Aufforderung zur Abmeldung der Zulieferdaten
	 */
	public final synchronized void handleListsDeliveryUnsubscription(
			T_T_HighLevelCommunicationInterface connection, TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription) {
		if((connection == null) || (transmitterListsDeliveryUnsubscription == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(listsManager != null) {
			listsManager.unsubscribeDeliverer(connection.getId(), transmitterListsDeliveryUnsubscription.getTransmitterList());
		}
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn eine Aktualisierung der Zuliefererinformationen von einen Datenverteiler angekommen
	 * ist. Zuerst wird die Methode updateEntry der Zuliefererverwaltung aufgerufen. Als Rückgabewert liefert diese Methode Listen der neu hinzugekommenen Objekte
	 * und Attributgruppen- und Aspektkombinationen. Dann werden alle nicht positiv quittierten Anmeldungen ermittelt, deren Basisanmeldeinformation mit den neuen
	 * registrierten Objekten und Kombinationen Gemeinsamkeiten hat. Für jede einzelne Anmeldung werden dann die potentiellen Zentraldatenverteiler ermittelt. Hat
	 * sich die Liste der Zentraldatenverteiler verändert, dann werden die Folgeanmeldungen bei den nicht mehr relevanten Zentraldatenverteilern abgemeldet und bei
	 * den neu ermittelten angemeldet.
	 *
	 * @param connection             Verbindung zwischen DAV und DAV
	 * @param transmitterListsUpdate Aufforderung zur Aktualisierung der Zulieferinformaion
	 */
	public final synchronized void handleListsUpdate(
			T_T_HighLevelCommunicationInterface connection, TransmitterListsUpdate transmitterListsUpdate) {
		if((connection == null) || (transmitterListsUpdate == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(listsManager != null) {
			listsManager.updateEntry(transmitterListsUpdate);
			long ids[] = transmitterListsUpdate.getObjectsToAdd();
			AttributeGroupAspectCombination combinations[] = transmitterListsUpdate.getAttributeGroupAspectsToAdd();
			if((ids != null) || (combinations != null)) {
				OutSubscriptionsHelper subscriptionHelper = new OutSubscriptionsHelper();
				InAndOutSubscription involvedSubscriptions[] = subscriptionsManager.getInvolvedSubscriptionRequests(
						ids, combinations
				);
				if(involvedSubscriptions != null) {
					for(int i = 0; i < involvedSubscriptions.length; ++i) {
						if(involvedSubscriptions[i] != null) {
							cleanPendingSubscriptionToRedirect(involvedSubscriptions[i]);
							boolean done = false;
							if((involvedSubscriptions[i]._outSubscriptions != null) && (involvedSubscriptions[i]._state
							                                                            <= TransmitterSubscriptionsConstants.NEGATIV_RECEIP)) {
								InAndOutSubscription subscriptionCopy = new InAndOutSubscription(involvedSubscriptions[i]);
								makeOutgoingRequests(subscriptionCopy);
								if(subscriptionCopy._outSubscriptions == null) {
									for(int j = 0; j < involvedSubscriptions[i]._outSubscriptions.length; ++j) {
										if(involvedSubscriptions[i]._outSubscriptions[j] != null) {
											subscriptionHelper.handleOutUnsubscription(
													involvedSubscriptions[i], involvedSubscriptions[i]._outSubscriptions[j]
											);
										}
									}
									involvedSubscriptions[i]._outSubscriptions = null;
									involvedSubscriptions[i]._currentTransmittersToConsider = null;
									involvedSubscriptions[i]._state = -1;
									involvedSubscriptions[i]._mainTransmitter = -1;
									done = true;
								}
								else {
									boolean changed = false;
									ArrayList<InAndOutSubscription.OutSubscription> outSubscriptionsUnion = new ArrayList<InAndOutSubscription.OutSubscription>();
									for(int j = 0; j < involvedSubscriptions[i]._outSubscriptions.length; ++j) {
										if(involvedSubscriptions[i]._outSubscriptions[j] != null) {
											boolean notFound = true;
											for(int k = 0; k < subscriptionCopy._outSubscriptions.length; ++k) {
												if(subscriptionCopy._outSubscriptions[k] != null) {
													// to do : elaborate the transmitter to consider list.
													if((involvedSubscriptions[i]._outSubscriptions[j]._targetTransmitter
													    == subscriptionCopy._outSubscriptions[k]._targetTransmitter) && Arrays.equals(
															involvedSubscriptions[i]._outSubscriptions[j]._transmittersToConsider,
															subscriptionCopy._outSubscriptions[k]._transmittersToConsider
													)) {
														subscriptionCopy._outSubscriptions[k] = null;
														notFound = false;
														break;
													}
												}
											}
											if(notFound) {
												subscriptionHelper.handleOutUnsubscription(
														involvedSubscriptions[i], involvedSubscriptions[i]._outSubscriptions[j]
												);
												changed = true;
											}
											else {
												outSubscriptionsUnion.add(involvedSubscriptions[i]._outSubscriptions[j]);
											}
										}
									}
									for(int j = 0; j < subscriptionCopy._outSubscriptions.length; ++j) {
										if(subscriptionCopy._outSubscriptions[j] != null) {
											outSubscriptionsUnion.add(subscriptionCopy._outSubscriptions[j]);
											subscriptionHelper.handleOutSubscription(subscriptionCopy, subscriptionCopy._outSubscriptions[j]);
											changed = true;
										}
									}
									int size = outSubscriptionsUnion.size();
									if(size > 0) {
										involvedSubscriptions[i]._outSubscriptions = new InAndOutSubscription.OutSubscription[size];
										for(int j = 0; j < size; ++j) {
											involvedSubscriptions[i]._outSubscriptions[j] = outSubscriptionsUnion.get(
													j
											);
										}
									}
									if(changed) {
										involvedSubscriptions[i]._currentTransmittersToConsider = subscriptionCopy._currentTransmittersToConsider;
										involvedSubscriptions[i]._state = -1;
									}
									done = true;
								}
							}
							if(done) {
								continue;
							}

							makeOutgoingRequests(involvedSubscriptions[i]);
							involvedSubscriptions[i]._mainTransmitter = -1;
							if(involvedSubscriptions[i]._outSubscriptions == null) {
								involvedSubscriptions[i]._currentTransmittersToConsider = null;
								if(involvedSubscriptions[i]._state != TransmitterSubscriptionsConstants.NEGATIV_RECEIP) {
									involvedSubscriptions[i]._state = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
									if(involvedSubscriptions[i]._fromTransmitter) {
										TransmitterDataSubscriptionReceipt negativReceipt = new TransmitterDataSubscriptionReceipt(
												involvedSubscriptions[i]._baseSubscriptionInfo,
												involvedSubscriptions[i]._role,
												TransmitterSubscriptionsConstants.NEGATIV_RECEIP,
												-1,
												involvedSubscriptions[i]._transmittersToConsider
										);
										T_T_HighLevelCommunication _connection = getTransmitterConnection(
												involvedSubscriptions[i]._source
										);
										if(_connection != null) {
											_connection.sendTelegram(negativReceipt);
										}
									}
									else {
										T_A_HighLevelCommunication _connection = getApplicationConnection(
												involvedSubscriptions[i]._source
										);
										if(_connection != null) {
											SubscriptionsFromApplicationStorage tmpSubscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)_connection.getSubscriptionsFromRemoteStorage();
											if(tmpSubscriptionsFromApplicationStorage != null) {
												if(involvedSubscriptions[i]._role == TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION) { // sending
													SendSubscriptionInfo _sendSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getSendSubscription(
															involvedSubscriptions[i]._baseSubscriptionInfo
													);
													if(_sendSubscriptionInfo != null) {
														if(_sendSubscriptionInfo.isRequestSupported()) {
															if(_sendSubscriptionInfo.getLastTriggerRequest() != (byte)1) {
																_sendSubscriptionInfo.setLastTriggerRequest((byte)1);
																_connection.triggerSender(
																		involvedSubscriptions[i]._baseSubscriptionInfo, (byte)1
																);// stop sending
															}
														}
													}
												}
												else { // receiving
													ReceiveSubscriptionInfo _receiveSubscriptionInfo = tmpSubscriptionsFromApplicationStorage.getReceiveSubscription(
															involvedSubscriptions[i]._baseSubscriptionInfo
													);
													if(_receiveSubscriptionInfo != null) {
														long dataNumber = cacheManager.getCurrentDataIndex(involvedSubscriptions[i]._baseSubscriptionInfo) + 1;
														if(_receiveSubscriptionInfo.getLastDataIndex() == dataNumber) {
															if(_receiveSubscriptionInfo.getLastErrorState() == 2) {
																continue;
															}
														}
														_receiveSubscriptionInfo.setLastDataIndex(dataNumber);
														_receiveSubscriptionInfo.setLastErrorState((byte)2);
														ApplicationDataTelegram dataToSend = new ApplicationDataTelegram(
																involvedSubscriptions[i]._baseSubscriptionInfo,
																dataNumber,
																false,
																(byte)2,
																null,
																null,
																1,
																0,
																System.currentTimeMillis()
														);
														_connection.sendData(dataToSend);
													}
												}
											}
										}
									}
								}
							}
							else if(subscriptionsManager.needToSendSubscription(_transmitterId, involvedSubscriptions[i])) {
								involvedSubscriptions[i]._state = (byte)-1;
								for(int j = 0; j < involvedSubscriptions[i]._outSubscriptions.length; ++j) {
									if(involvedSubscriptions[i]._outSubscriptions[j] != null) {
										subscriptionHelper.handleOutSubscription(
												involvedSubscriptions[i], involvedSubscriptions[i]._outSubscriptions[j]
										);
									}
								}
							}
							else {
								involvedSubscriptions[i]._outSubscriptions = null;
							}
						}
					}
				}

				// missed redirections
				involvedSubscriptions = subscriptionsManager.getInvolvedPositiveSubscriptionRequests(ids, combinations);
				if(involvedSubscriptions != null) {
					for(int i = 0; i < involvedSubscriptions.length; ++i) {
						if(involvedSubscriptions[i] != null) {
							long oldWay = -1;
							long bestWay = bestWayManager.getBestWay(involvedSubscriptions[i]._mainTransmitter);

							boolean mustBeRedirected = false;
							for(int j = 0; j < involvedSubscriptions[i]._outSubscriptions.length; ++j) {
								if((involvedSubscriptions[i]._outSubscriptions[j] != null) && (involvedSubscriptions[i]._outSubscriptions[j]._targetTransmitter
								                                                               != bestWay)) {
									oldWay = involvedSubscriptions[i]._outSubscriptions[j]._targetTransmitter;
									mustBeRedirected = true;
									break;
								}
							}
							if(mustBeRedirected) {
								for(int j = _pendingSubscriptionToRedirect.size() - 1; j > -1; --j) {
									ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
											j
									);
									if(exchangeSubscriptionAction != null) {
										if(exchangeSubscriptionAction.oldSubscription == involvedSubscriptions[i]) {
											if(bestWay == exchangeSubscriptionAction.newRootingTransmitterId) {
												mustBeRedirected = false;
											}
											else {
												_pendingSubscriptionToRedirect.remove(j);
												for(int k = 0; k < exchangeSubscriptionAction.newSubscription._outSubscriptions.length; ++k) {
													if(exchangeSubscriptionAction.newSubscription._outSubscriptions[k] != null) {
														subscriptionHelper.handleOutUnsubscription(
																exchangeSubscriptionAction.newSubscription,
																exchangeSubscriptionAction.newSubscription._outSubscriptions[k]
														);
													}
												}
											}
											break;
										}
									}
								}
								if(mustBeRedirected) {
									InAndOutSubscription request = new InAndOutSubscription(involvedSubscriptions[i]);
									makeOutgoingRequests(request);
									if(request._outSubscriptions != null) {
										long dataIndex = cacheManager.getCurrentDataIndex(involvedSubscriptions[i]._baseSubscriptionInfo);
										_pendingSubscriptionToRedirect.add(
												new ExchangeSubscriptionAction(
														involvedSubscriptions[i], request, oldWay, bestWay, involvedSubscriptions[i]._mainTransmitter, dataIndex
												)
										);
										for(int j = 0; j < request._outSubscriptions.length; ++j) {
											if(request._outSubscriptions[j] != null) {
												subscriptionHelper.handleOutSubscription(request, request._outSubscriptions[j]);
											}
										}
									}
								}
							}
						}
					}
				}
				subscriptionHelper.flushOutUnSubscription(this);
			}
		}
	}

	/**
	 * Diese Methode wird von der Anmeldungsverwaltung aufgerufen, wenn ein neuer Datensatz angekommen ist. Hier wird überprüft, ob ein Umleitungsantrag auf dem
	 * spezifizierten Datenverteiler connectedTransmitterId basiert, d. h., dass entweder der alte oder der neue Weg über diesen Datenverteiler läuft. Der Antrag
	 * wird dann mit dem Index des Datensatzes dataIndex aktualisiert. Wenn der Datensatzindex über den neuen Weg gleich oder um eins höher als der Datensatzindex
	 * über den alten Weg ist, dann wird die Umleitung als erfolgreich aufgebaut angesehen. Die Folgeanmeldungen über den alten Weg werden abgemeldet, und der
	 * Umleitungsantrag wird aus der Umleitungsverwaltung entfernt.
	 *
	 * @param info                   Datenidentifikation
	 * @param connectedTransmitterId ID des DAV
	 * @param dataIndex              Datensatzindex
	 *
	 * @return true wenn Datensatzindex gleich oder um eins höher als der Datensatzindex über den alten Weg ist, sonst false.
	 */
	final synchronized boolean updatePendingSubscription(
			BaseSubscriptionInfo info, long connectedTransmitterId, long dataIndex) {
		for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
			ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
					i
			);
			if(exchangeSubscriptionAction != null) {
				if(exchangeSubscriptionAction.oldSubscription._baseSubscriptionInfo.equals(info)) {
					boolean canSend = exchangeSubscriptionAction.setDataIndex(connectedTransmitterId, dataIndex);
					if(exchangeSubscriptionAction.isRedirectionSafe()) {
						_pendingSubscriptionToRedirect.remove(i);
						InAndOutSubscription oldSubscription = exchangeSubscriptionAction.oldSubscription;
						if(oldSubscription != null) {
							for(int j = 0; j < oldSubscription._outSubscriptions.length; ++j) {
								if(oldSubscription._outSubscriptions[j] != null) {
									TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
											oldSubscription._baseSubscriptionInfo,
											oldSubscription._role,
											oldSubscription._outSubscriptions[j]._transmittersToConsider
									);
									T_T_HighLevelCommunication _connection = getTransmitterConnection(
											oldSubscription._outSubscriptions[j]._targetTransmitter
									);
									if(_connection != null) {
										_connection.sendTelegram(_unsubscription);
									}
								}
							}
							oldSubscription._state = exchangeSubscriptionAction.newSubscription._state;
							oldSubscription._mainTransmitter = exchangeSubscriptionAction.newSubscription._mainTransmitter;
							oldSubscription._currentTransmittersToConsider = exchangeSubscriptionAction.newSubscription._currentTransmittersToConsider;
							oldSubscription._outSubscriptions = exchangeSubscriptionAction.newSubscription._outSubscriptions;
						}
						return true;
					}
					return canSend;
				}
			}
		}
		return true;
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung aufgerufen, um einer Verbindung ein Gewicht zuzuweisen. Die Information wird von der Wegverwaltung benutzt,
	 * wenn eine Verbindung bewertet wird.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Gewichtung der Verbindung
	 */
	public final short getWeight(long connectedTransmitterId) {
		short weight = DEFAULT_WEIGHT;
		if(_transmitterConnectionInfos != null) {
			for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
				TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
				if(t1 != null) {
					if(t1.getTransmitterId() == _transmitterId) {
						TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
						if((t2 != null) && (t2.getTransmitterId() == connectedTransmitterId)) {
							weight = _transmitterConnectionInfos[i].getWeight();
						}
					}
					else if(t1.getTransmitterId() == connectedTransmitterId) {
						TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
						if((t2 != null) && (t2.getTransmitterId() == _transmitterId)) {
							weight = _transmitterConnectionInfos[i].getWeight();
						}
					}
				}
			}
		}
		return weight;
	}

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung von diesem Datenverteiler zum angegebenen Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	private TransmitterConnectionInfo getTransmitterConnectionInfo(long connectedTransmitterId) {
		if(_transmitterConnectionInfos != null) {
			for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
				_debug.finest("getTransmitterConnectionInfo: prüfe", _transmitterConnectionInfos[i].parseToString());
				TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
				if(t1.getTransmitterId() == _transmitterId) {
					TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
					if(t2.getTransmitterId() == connectedTransmitterId) {
						_debug.finer("getTransmitterConnectionInfo: gefunden", _transmitterConnectionInfos[i].parseToString());
						return _transmitterConnectionInfos[i];
					}
				}
			}
		}
		_debug.finer("getTransmitterConnectionInfo: nicht gefunden", connectedTransmitterId);
		return null;
	}

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung vom angegebenen Datenverteiler zu diesem Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	private TransmitterConnectionInfo getRemoteTransmitterConnectionInfo(long connectedTransmitterId) {
		if(_transmitterConnectionInfos != null) {
			for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
				_debug.finest("getRemoteTransmitterConnectionInfo: prüfe", _transmitterConnectionInfos[i].parseToString());
				TransmitterInfo t1 = _transmitterConnectionInfos[i].getTransmitter_1();
				TransmitterInfo t2 = _transmitterConnectionInfos[i].getTransmitter_2();
				if(t2.getTransmitterId() == _transmitterId && t1.getTransmitterId() == connectedTransmitterId) {
					_debug.finer("getRemoteTransmitterConnectionInfo: gefunden", _transmitterConnectionInfos[i].parseToString());
					return _transmitterConnectionInfos[i];
				}
			}
		}
		_debug.finer("getRemoteTransmitterConnectionInfo: nicht gefunden", connectedTransmitterId);
		return null;
	}

	/**
	 * Bestimmt den Benutzername der zur Authentifizierung beim angegebenen Datenverteiler benutzt werden soll. Wenn der Benutzername in der Topologie nicht
	 * vorgegeben ist, dann wird der Standardbenutzername des Datenverteilers benutzt.
	 *
	 * @param connectedTransmitterId Objekt-ID des anderen Datenverteilers.
	 *
	 * @return Benutzername für die Authentifizierung beim anderen Datenverteiler.
	 */
	public String getUserNameForAuthentification(long connectedTransmitterId) {
		String userName = "";
		final TransmitterConnectionInfo info = getTransmitterConnectionInfo(connectedTransmitterId);
		if(info != null) {
			userName = info.getUserName();
		}
		else {
			final TransmitterConnectionInfo remoteInfo = getRemoteTransmitterConnectionInfo(connectedTransmitterId);
			if(remoteInfo != null) {
				userName = remoteInfo.getRemoteUserName();
			}
			else {
				_debug.warning("Keine Verbindungsinfo für Verbindung zum Datenverteiler " + connectedTransmitterId + " gefunden.");
			}
		}
		if(userName.equals("")) {
			userName = _serverDavParameters.getUserName();
			_debug.warning(
					"Kein Benutzername für die Authentifizierung beim Datenverteiler " + connectedTransmitterId + " gefunden."
					+ " Es wird der Default-Benutzername " + userName + " benutzt."
			);
		}
		return userName;
	}

	/**
	 * Bestimmt das Benutzerpasswort das zur Authentifizierung beim angegebenen Datenverteiler benutzt werden soll. Wenn der Benutzername in der Topologie nicht
	 * vorgegeben ist, dann wird das Passwort des Standardbenutzers des Datenverteilers zurückgegeben.
	 *
	 * @param connectedTransmitterId Objekt-ID des anderen Datenverteilers.
	 *
	 * @return Passwort für die Authentifizierung beim anderen Datenverteiler.
	 */
	public String getPasswordForAuthentification(long connectedTransmitterId) {
		String userName = "";
		final TransmitterConnectionInfo info = getTransmitterConnectionInfo(connectedTransmitterId);
		if(info != null) {
			userName = info.getUserName();
		}
		else {
			final TransmitterConnectionInfo remoteInfo = getRemoteTransmitterConnectionInfo(connectedTransmitterId);
			if(remoteInfo != null) {
				userName = remoteInfo.getRemoteUserName();
			}
		}
		if(userName.equals("")) {
			return _serverDavParameters.getUserPassword();
		}
		return _serverDavParameters.getStoredPassword(userName);
	}

	public synchronized final void updateDestinationRoute(
			long transmitterid, RoutingConnectionInterface oldConnection, RoutingConnectionInterface newConnection) {
		if(oldConnection == null) {
			return;
		}
		if(newConnection == null) {
			return;
		}

		long oldRouterId = oldConnection.getRemoteNodeId();
		long newRouterId = newConnection.getRemoteNodeId();

		if(oldRouterId == newRouterId) {
			return;
		}

		for(int i = _pendingSubscriptionToRedirect.size() - 1; i > -1; --i) {
			ExchangeSubscriptionAction exchangeSubscriptionAction = _pendingSubscriptionToRedirect.get(
					i
			);
			if(exchangeSubscriptionAction != null) {
				if(exchangeSubscriptionAction.transmitterId == transmitterid) {
					_pendingSubscriptionToRedirect.remove(i);
					InAndOutSubscription request = exchangeSubscriptionAction.newSubscription;
					if(request != null) {
						for(int j = 0; j < request._outSubscriptions.length; ++j) {
							if(request._outSubscriptions[j] != null) {
								TransmitterDataUnsubscription _unsubscription = new TransmitterDataUnsubscription(
										request._baseSubscriptionInfo, request._role, request._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										request._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_unsubscription);
								}
							}
						}
					}
				}
			}
		}

		InAndOutSubscription involvedSubscriptions[] = subscriptionsManager.getSubscriptionRequestsToRedirect(
				transmitterid, oldRouterId
		);
		if(involvedSubscriptions != null) {
			for(int i = 0; i < involvedSubscriptions.length; ++i) {
				if(involvedSubscriptions[i] != null) {
					InAndOutSubscription request = new InAndOutSubscription(involvedSubscriptions[i]);
					makeOutgoingRequests(request);
					if(request._outSubscriptions != null) {
						long dataIndex = cacheManager.getCurrentDataIndex(involvedSubscriptions[i]._baseSubscriptionInfo);
						_pendingSubscriptionToRedirect.add(
								new ExchangeSubscriptionAction(
										involvedSubscriptions[i], request, oldRouterId, newRouterId, transmitterid, dataIndex
								)
						);
						for(int j = 0; j < request._outSubscriptions.length; ++j) {
							if(request._outSubscriptions[j] != null) {
								TransmitterDataSubscription _subscription = new TransmitterDataSubscription(
										request._baseSubscriptionInfo, request._role, request._outSubscriptions[j]._transmittersToConsider
								);
								T_T_HighLevelCommunication _connection = getTransmitterConnection(
										request._outSubscriptions[j]._targetTransmitter
								);
								if(_connection != null) {
									_connection.sendTelegram(_subscription);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Diese Methode wird von der Datenverteilerapplikation aufgerufen wenn ein Datenverteiler heruntergefahren wird. Zuerst werden die Threads terminiert, die für
	 * das Erstellen der Kommunikationskanäle zwischen Applikation bzw. Datenverteiler und diesem Datenverteiler zuständig sind. Danach werden alle bereits
	 * vorhandenen Verbindungen zu anderen Applikationen und Datenverteilern terminiert, so dass diese auch informiert werden, dass dieser Datenverteiler nicht
	 * mehr zur Verfügung steht.
	 *
	 * @param error   True: ein Fehler ist aufgetreten
	 * @param message genauere Beschreibung des Fehlers
	 */
	public final void close(boolean error, String message) {
		if(_closing) return;
		_closing = true;
		synchronized(this) {
			String debugMessage = "Der Datenverteiler wird beendet. Ursache: " + message;
			if(error) {
				_debug.error(debugMessage);
			}
			else {
				_debug.warning(debugMessage);
			}
			if(_applicationConnectionsSubscriber != null) {
				_applicationConnectionsSubscriber.interrupt();
				_applicationConnectionsSubscriber = null;
			}
			if(_transmitterConnectionsSubscriber != null) {
				_transmitterConnectionsSubscriber.interrupt();
				_transmitterConnectionsSubscriber = null;
			}
			if(_applicationsServerConnection != null) {
				_applicationsServerConnection.disconnect();
			}
			if(_transmittersServerConnection != null) {
				_transmittersServerConnection.disconnect();
			}

			if(listsManager != null) {
				listsManager.close();
			}

			synchronized(_transmitterConnections) {
				for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
					if(transmitterConnection != null) {
						transmitterConnection.terminate(error, message);
					}
				}
				_transmitterConnections.clear();
			}

			synchronized(_applicationConnections) {
				for(T_A_HighLevelCommunication connection : _applicationConnections) {
					if(connection != null) {
						connection.terminate(error, message);
					}
				}
				_applicationConnections.clear();
			}
		}
	}


	/**
	 * Erstellt eine Liste der DAV-DAV-Verbindungen für den entsperchenden Benutzer.
	 *
	 * @param userId ID des Benutzers
	 *
	 * @return Liste der Verbindungen
	 */
	private List<T_T_HighLevelCommunication> getTransmitterConnections(long userId) {
		ArrayList<T_T_HighLevelCommunication> result = new ArrayList<T_T_HighLevelCommunication>();
		for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
			if(transmitterConnection != null) {
				if(transmitterConnection.getRemoteUserId() == userId) {
					result.add(transmitterConnection);
				}
			}
		}
		return result;
	}

	/**
	 * Erstellt eine Liste der DAV-DAF-Verbindungen für den entsprechenden Benutzer.
	 *
	 * @param userId ID des Benutzers
	 *
	 * @return Liste der Applikationsverbindungen
	 */
	private final List<T_A_HighLevelCommunication> getApplicationConnections(long userId) {
		ArrayList<T_A_HighLevelCommunication> result = new ArrayList<T_A_HighLevelCommunication>();
		for(T_A_HighLevelCommunication applicationConnection : _applicationConnections) {
			if(applicationConnection != null) {
				if(applicationConnection.getRemoteUserId() == userId) {
					result.add(applicationConnection);
				}
			}
		}
		return result;
	}

	/**
	 * Entfernt Verbindung zwischen zwei DAV
	 *
	 * @param communicationTransmitterId Id des verbundenen Datenverteilers
	 */
	private void removeTransmitterConnection(long communicationTransmitterId) {
		synchronized(_transmitterConnections) {
			for(int i = _transmitterConnections.size() - 1; i > -1; --i) {
				T_T_HighLevelCommunication highLevelCommunication = (T_T_HighLevelCommunication)_transmitterConnections.get(i);
				if(highLevelCommunication != null) {
					if(highLevelCommunication.getId() == communicationTransmitterId) {
						_transmitterConnections.remove(i);
						authentificationManager.removeUser(highLevelCommunication.getRemoteUserId());
						break;
					}
				}
			}
		}
	}

	/**
	 * Entfernt Verbindung zwischen DAV und DAF
	 *
	 * @param applicationId Id der verbundenen Applikation
	 *
	 * @return Applikationsobjekt, das der angegebenen Applikation zugeordnet ist.
	 */
	private final SystemObject removeApplicationConnection(long applicationId) {
		boolean foundApplication = false;
		synchronized(_applicationConnections) {
			for(int i = _applicationConnections.size() - 1; i > -1; --i) {
				T_A_HighLevelCommunication highLevelCommunication = _applicationConnections.get(i);
				if(highLevelCommunication != null) {
					if(highLevelCommunication.getId() == applicationId) {
						final T_A_HighLevelCommunication removedHighLevelCommunication = _applicationConnections.remove(i);
						authentificationManager.removeUser(highLevelCommunication.getRemoteUserId());

						// Nur wenn der Thread auch vorhanden ist
						if(_applicationStatusUpdater != null) {
							_applicationStatusUpdater.applicationRemoved(removedHighLevelCommunication);
							_applicationConnections.notifyAll();
						}
						foundApplication = true;
						break;
					}
				}
			}
		}
		if(foundApplication) {
			if(dataModel != null && applicationId != 0 && applicationId != -1) {
				final SystemObject applicationObject = dataModel.getObject(applicationId);
				if(applicationObject != null && applicationObject instanceof ClientApplication) {
					return applicationObject;
				}
			}
		}
		return null;
	}

	/** Diese Methode ruft <code>subscriptionsManager.printSubscriptions</code> auf, welche die Anmeldeinformationen in den OutputSream schreibt. */
	public void printSubscriptions() {
		subscriptionsManager.printSubscriptions();
	}

	/**
	 * Setzt den Datenverteilersbetriebsmodus auf den Lokalen Modus.
	 *
	 * @param configurationPid Pid der Konfigurationsapplikation
	 * @param configurationId  Id der Konfigurationsapplikation
	 */
	public void setLocaleModeParameter(final String configurationPid, final long configurationId) {
		_serverDavParameters.setLocalModeParameter(configurationPid, configurationId);
	}

	/**
	 * Gibt die configurationPid des Lokalen Modus zurück.
	 *
	 * @return <code>configurationPid</code>
	 */
	public String getLocaleModeConfigurationPid() {
		final Object[] localeModeParameter = _serverDavParameters.getLocalModeParameter();
		if(localeModeParameter == null) return "";
		return (String)localeModeParameter[0];
	}

	/**
	 * Gibt die configurationId des Lokalen Modus zurück.
	 *
	 * @return <code>configurationId</code>
	 */
	public long getLocaleModeConfigurationId() {
		final Object[] localeModeParameter = _serverDavParameters.getLocalModeParameter();
		if(localeModeParameter == null) return -1;
		return (Long)localeModeParameter[1];
	}

	void dumpRoutingTable() {
		bestWayManager.dumpRoutingTable();
	}

	void dumpSubscriptionsLists() {
		listsManager.dumpSubscriptionLists();
	}

	DavDavRequester getDavRequestManager() {
		return _davRequestManager;
	}

	/** Diese Subklasse startet einen Thread, der eine Application bei einem Datenverteiler anmeldet. */
	class ApplicationConnectionsSubscriber extends Thread {

		public ApplicationConnectionsSubscriber() {
			super("ApplicationConnectionsSubscriber");
			//XXX setPriority(Thread.MIN_PRIORITY);
		}

		/** The run method that loops through */
		public final void run() {
			if(_applicationsServerConnection == null) {
				return;
			}
			while(!isInterrupted()) {
				// Blocks until a connection occurs:
				final ConnectionInterface connection = _applicationsServerConnection.accept();
				Runnable runnable = new Runnable() {
					public void run() {
						if(connection == null) {
							return;
						}
						try {
							LowLevelCommunication lowLevelCommunication = new LowLevelCommunication(
									connection,
									_serverDavParameters.getAppCommunicationOutputBufferSize(),
									_serverDavParameters.getAppCommunicationInputBufferSize(),
									_serverDavParameters.getSendKeepAliveTimeout(),
									_serverDavParameters.getReceiveKeepAliveTimeout(),
									LowLevelCommunication.NORMAL_MODE,
									true
							);
							ServerConnectionProperties properties = new ServerConnectionProperties(
									lowLevelCommunication, _authentificationComponent, _serverDavParameters
							);
							T_A_HighLevelCommunication highLevelCommunication = new T_A_HighLevelCommunication(
									properties, subscriptionsManager, ConnectionsManager.this, !_configurationAvaillable
							);
							highLevelCommunication.setApplicationStatusUpdater(_applicationStatusUpdater);

							_applicationConnections.add(highLevelCommunication);
						}
						catch(ConnectionException ex) {
							ex.printStackTrace();
						}
					}
				};
				Thread thread = new Thread(runnable);
				thread.start();
				yield();
			}
		}
	}

	/** Diese Subklasse startet eine Thread, der eine DAV bei einem DAV anmeldet. */

	class TransmitterConnectionsSubscriber extends Thread {

		public TransmitterConnectionsSubscriber() {
			super("TransmitterConnectionsSubscriber");
			//XXX setPriority(Thread.MIN_PRIORITY);
		}

		/** The run method that loops through */
		public final void run() {
			if(_transmittersServerConnection == null) {
				return;
			}
			while(!isInterrupted()) {
				// Blocks until a connection occurs:
				final ConnectionInterface connection = _transmittersServerConnection.accept();
				Runnable runnable = new Runnable() {
					public void run() {
						if(connection == null) {
							return;
						}
						else {
							try {
								boolean connectionExists = false;
								for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
									if(transmitterConnection != null) {
										String adress = transmitterConnection.getRemoteAdress();
										int subAdress = transmitterConnection.getRemoteSubadress();
										if((adress != null) && (adress.equals(connection.getMainAdress())) && (subAdress == connection.getSubAdressNumber())) {
											connectionExists = true;
											break;
										}
									}
								}
								if(!connectionExists) {
									LowLevelCommunication lowLevelCommunication = new LowLevelCommunication(
											connection,
											_serverDavParameters.getDavCommunicationOutputBufferSize(),
											_serverDavParameters.getDavCommunicationInputBufferSize(),
											_serverDavParameters.getSendKeepAliveTimeout(),
											_serverDavParameters.getReceiveKeepAliveTimeout(),
											LowLevelCommunication.NORMAL_MODE,
											true
									);
									ServerConnectionProperties properties = new ServerConnectionProperties(
											lowLevelCommunication, _authentificationComponent, _serverDavParameters
									);
									T_T_HighLevelCommunication highLevelCommunication = new T_T_HighLevelCommunication(
											properties,
											subscriptionsManager,
											ConnectionsManager.this,
											bestWayManager,
											DEFAULT_WEIGHT,
											!_configurationAvaillable,
											"",
											""
									);
									_transmitterConnections.add(highLevelCommunication);
								}
							}
							catch(ConnectionException ex) {
								ex.printStackTrace();
							}
						}
					}
				};
				Thread thread = new Thread(runnable);
				thread.start();
				yield();
			}
		}
	}

	class TransmitterConnectionsMonitor extends Thread {

		public TransmitterConnectionsMonitor() {
			super("TransmitterConnectionsMonitor");
			//XXX setPriority(Thread.MIN_PRIORITY);
		}

		/** The run method that loops through */
		public final void run() {
			if(missedConnectionInfos == null) {
				return;
			}
			while(!isInterrupted()) {
				try {
					synchronized(missedConnectionInfos) {
						for(int i = missedConnectionInfos.size() - 1; i > -1; --i) {
							TransmitterConnectionInfo transmitterConnectionInfo = missedConnectionInfos.get(i);
							if(transmitterConnectionInfo != null) {
								if(connectToMainTransmitter(transmitterConnectionInfo)) {
									missedConnectionInfos.remove(i);

									if(transmitterConnectionInfo.isAutoExchangeTransmitterDetectionOn()) {
										TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
										List<TransmitterConnectionInfo> infos = getInvolvedTransmitters(t2);
										for(TransmitterConnectionInfo info : infos) {
											try {
												TransmitterInfo transmitterInfo = info.getTransmitter_2();
												if(transmitterInfo != null) {
													T_T_HighLevelCommunication connection = getTransmitterConnection(transmitterInfo.getTransmitterId());
													if((connection != null) && (!connection.isAcceptedConnection())) {
														connection.terminate(
																false,
																"Automatisch ermittelte Ersatzverbindung wird nicht mehr benötigt, weil ursprüngliche Verbindung wiederhergestellt wurde"
														);
													}
												}
											}
											catch(Exception e) {
												e.printStackTrace();
											}
										}
									}
									else {
										TransmitterInfo infos[] = transmitterConnectionInfo.getExchangeTransmitterList();
										if(infos != null) {
											for(int j = 0; j < infos.length; ++j) {
												TransmitterConnectionInfo transmitterConnectionInfoToDisconnect = null;
												for(int k = 0; k < _transmitterConnectionInfos.length; ++k) {
													if(_transmitterConnectionInfos[k].isExchangeConnection()
													   && (_transmitterConnectionInfos[k].getTransmitter_1().getTransmitterId() == _transmitterId) && (
															_transmitterConnectionInfos[k].getTransmitter_2().getTransmitterId()
															== infos[j].getTransmitterId())) {
														transmitterConnectionInfoToDisconnect = _transmitterConnectionInfos[k];
														break;
													}
												}
												if(transmitterConnectionInfoToDisconnect != null) {
													try {
														T_T_HighLevelCommunication connection = getTransmitterConnection(infos[j].getTransmitterId());
														if((connection != null) && (!connection.isAcceptedConnection())) {
															connection.terminate(
																	false,
																	"Konfigurierte Ersatzverbindung wird nicht mehr benötigt, weil ursprüngliche Verbindung wiederhergestellt wurde"
															);
														}
													}
													catch(Exception e) {
														e.printStackTrace();
													}
												}
											}
										}
									}
								}
								else {
									connectToAlternativeTransmitters(transmitterConnectionInfo);
								}
							}
						}
						missedConnectionInfos.wait(10000);
					}
				}
				catch(InterruptedException ex) {
					return;
				}
				yield();
			}
		}
	}

	/**
	 * Bestimmt die Einstellungen des Datenverteilers, die über Aufrufargumente verändert werden können.
	 *
	 * @return Einstellungen des Datenverteilers.
	 */
	public ServerDavParameters getServerDavParameters() {
		return _serverDavParameters;
	}

	/**
	 * Dieser Thread verschickt den Status(angemeldete Appliktionen, Telegrammlaufzeiten, durch Applikationen angemeldete Datenidentifikationen) aller angemeldeten
	 * Applikationen.
	 */
	public final class ApplicationStatusUpdater extends Thread {

		/** Enthält alle Applikationen, die an/ab gemeldet werden sollen. Die Elemente werden nach Fifo bearbeitet. */
		private final List<ApplicationConnection> _application = Collections.synchronizedList(new ArrayList<ApplicationConnection>());

		private final SystemObject _davObject;

		/** Zum verschicken aller angemeldeten Applikationen */
		private final SourceApplicationUpdater _applicationUpdater = new SourceApplicationUpdater(this);

		/** DataDescription, mit der eine Quelle zum verschicken aller angemeldeten Applikationen angemeldet wird. */
		private final DataDescription _applicationUpdaterDataDescription;

		/** Zum verschicken aller Telegrammlaufzeiten der angemeldeten Applikationen */
		private final SourceApplicationUpdater _applicationRuntime = new SourceApplicationUpdater(this);

		/** DataDescription, mit der eine Quelle zum verschicken der Telegrammlaufzeiten der angemeldeten Applikationen. */
		private final DataDescription _applicationRuntimeDataDescription;

		/** Die DataDescription, die zum Versandt von Anmeldungen einer Applikation benutzt wird. */
		private final DataDescription _applicationDataDescriptionDD;

		/** Verschickt zyklisch alle angemeldeten Applikationen und deren Telegrammlaufzeiten. */
		private final Timer _timer = new Timer("Status der Applikation zyklisch verschicken", true);

		/**
		 * Wird für eine Applikation eine neue Datenidentifikation angemeldet, so wird dies über einen TimerTask publiziert. Der TimerTask, der diese Aufgabe
		 * übernimmt, wird hier gespeichert. Der TimerTask kann jederzeit unterbrochen werden und durch einen neuen TimerTask ersetzt werden. Auch wenn der TimerTask
		 * bereits ausgeführt wurde befindet er sich weiterhin in dieser Map und kann durch einen neuen TimerTask ersetzt werden.
		 * <p/>
		 * Als Schlüssel dient die HighLevelCommu, dies entspricht der Applikation, deren Anmeldungen verschickt werden sollen. Als Value wird ein TimerTask
		 * zurückgegeben, der alle angemeldeten Datenidentifikationen der Applikation verschickt, sobald der Thread ausgeführt wird. Der Thread kann bereits
		 * ausgeführt worden sein oder aber noch ausgeführt werden (oder befindet sich in Bearbeitung). Wurde noch kein TimerTask angelegt, so wird <code>null</code>
		 * zurückgegeben.
		 */
		private final Map<T_A_HighLevelCommunication, TimerTask> _threadsForDataIdentificationUpdates = new HashMap<T_A_HighLevelCommunication, TimerTask>();

		/**
		 * Key = Applikation, die bereits Daten angemeldet hat; Value = ClientSenderInterface(SourceApplicationUpdater). Damit kann später die Verbindung wieder
		 * abgemeldet werden und es wird ein mehrfaches anmelden verhindert. Gleichzeit kann das Objekt befragt werden, ob überhaupt gesendet werden soll.
		 */
		private final Map<T_A_HighLevelCommunication, SourceApplicationUpdater> _subcribedDataIdentifications = new HashMap<T_A_HighLevelCommunication, SourceApplicationUpdater>();

		public ApplicationStatusUpdater() {
			try {
				this.setDaemon(true);

				setName("Applikationsstatus-Updater");

				_davObject = dataModel.getObject(_transmitterId);

				// Als Quelle für alle angemeldeten Applikationen anmelden

				final AttributeGroup applicationUpdaterATG = dataModel.getAttributeGroup(_pidAngemeldeteApplikationen);
				final Aspect applicationUpdaterAspect = dataModel.getAspect("asp.standard");
				_applicationUpdaterDataDescription = new DataDescription(applicationUpdaterATG, applicationUpdaterAspect);
				selfClientDavConnection.subscribeSender(_applicationUpdater, _davObject, _applicationUpdaterDataDescription, SenderRole.source());

				// Als Quelle für Telegrammlaufzeiten zu allen angemeldeten Applikationen anmelden

				final AttributeGroup applicationTelegramRuntimeATG = dataModel.getAttributeGroup(_pidTelegrammLaufzeiten);
				final Aspect applicationTelegramRuntimeAspect = dataModel.getAspect("asp.messwerte");
				_applicationRuntimeDataDescription = new DataDescription(applicationTelegramRuntimeATG, applicationTelegramRuntimeAspect);
				selfClientDavConnection.subscribeSender(_applicationRuntime, _davObject, _applicationRuntimeDataDescription, SenderRole.source());

				// Datenidentifikation zum verschicken von "Datenidentifikationen, die zu einer Applikation gehören" anlegen
				_applicationDataDescriptionDD = new DataDescription(
						dataModel.getAttributeGroup(_pidAngemeldeteDatenidentifikationen), dataModel.getAspect("asp.standard")
				);
			}
			catch(OneSubscriptionPerSendData oneSubscriptionPerSendData) {
				final String errorMessage = "Der Thread zur Publizierung des Status aller angemeldeten Applikationen konnte nicht gestartet werden.";
				_debug.error(
						errorMessage, oneSubscriptionPerSendData
				);
				throw new IllegalStateException(errorMessage);
			}
		}


		/**
		 * Prüft, ob das übergebene Objekt gleich dem Objekt ist, das die Applikation des DaV darstellt, die sich selbst beim DaV anmeldet oder aber die
		 * Konfiguration.
		 * <p/>
		 * Wurde bisher noch kein DaV-Objekt gesucht/gefunden und das übergebene Objekt ist das DaV-Objekt, so wird das Objekt einen internen Variablen zugewiesen und
		 * <code>true</code> zurück gegeben.
		 *
		 * @param unknownObject Objekt, das vielleicht das DaV-Objekt darstellen könnte.
		 *
		 * @return true = Es handelt sich um das DaV-Objekt oder das Konfigurationsobjekt -> Das Objekt muss ausgeblendet werden; false = sonst
		 */
		private boolean isSpecialTreatedApplicationObject(T_A_HighLevelCommunication unknownObject) {

			// 1) Die Konfiguration besitzt weder ein Systemobjekt noch einen Benutzer
			// 2) Das Applikationsobjekt des Datenverteilers ist vom Typ "typ.datenverteiler"

			final long applicationId = unknownObject.getId();
			if(applicationId == 0 || applicationId == -1) return true;
			final SystemObject unknownSystemobject = dataModel.getObject(applicationId);
			final SystemObject unknownUser = dataModel.getObject(unknownObject.getRemoteUserId());

			if(unknownSystemobject == null) {
				if(unknownUser == null) {
					return true;
				}
				else {
					_debug.error("Es gibt kein Systemobjekt, aber einen Benutzer: " + unknownUser);
					return true;
				}
			}
			else {
				// Das Systemobjekt ist vorhanden, hat es den richtigen Typ ?
				return !(unknownSystemobject instanceof ClientApplication);
			}
		}

		/** Es wird ein Datensatz verschickt, der alle Applikationen enthält, die gerade am DaV angemeldet sind. */
		private void sendApplicationUpdate() {
			ResultData resultData = null;
			Vector<T_A_HighLevelCommunication> applicationConnectionsCopy = null;
			if(_applicationUpdater.sendData()) {
				// Es gibt einen Empfänger für die Daten
				applicationConnectionsCopy = new Vector<T_A_HighLevelCommunication>(_applicationConnections);
			}
			if(applicationConnectionsCopy != null) {
				// Es gibt einen Empfänger für die Daten
				final Data data = selfClientDavConnection.createData(_applicationUpdaterDataDescription.getAttributeGroup());

				// Alle Applikationen eintragen, die derzeit angemeldet sind.
				final Data.Array subscribedApplications = data.getItem("angemeldeteApplikation").asArray();
				// Diese größe paßt vielleicht nicht. Das Applikationsobjekt für die Konfiguration und das Applikationsobjekt für den DaV muss noch entfernt werden.
				subscribedApplications.setLength(applicationConnectionsCopy.size());

				// Index, an dem ein neues Element in das Array eingefügt werden muss
				int dataIndex = 0;

				for(T_A_HighLevelCommunication applicationConnection : applicationConnectionsCopy) {
					if(!isSpecialTreatedApplicationObject(applicationConnection)) {
						// atl.angemeldeteApplikation
						final Data listEntry = subscribedApplications.getItem(dataIndex);

						final SystemObject subscribedApplicationSystemObject = dataModel.getObject(applicationConnection.getId());
						listEntry.getItem("applikation").asReferenceValue().setSystemObject(subscribedApplicationSystemObject);

						final SystemObject subscribedUserSystemObject = dataModel.getObject(applicationConnection.getRemoteUserId());
						listEntry.getItem("benutzer").asReferenceValue().setSystemObject(subscribedUserSystemObject);

						listEntry.getItem("seit").asTimeValue().setMillis(applicationConnection.getConnectionCreatedTime());

						listEntry.getItem("sendepufferzustand").asTextValue().setText(applicationConnection.getSendBufferState());

						dataIndex++;
					}
					else {
						// Dieses Applikationsobjekt darf nicht mit verschickt werden. Also das ursprüngliche Array verkleinern.
						subscribedApplications.setLength(subscribedApplications.getLength() - 1);
					}
				} // for, über alle angemeldeten Applikationen

				resultData = new ResultData(_davObject, _applicationUpdaterDataDescription, System.currentTimeMillis(), data);
				try {
					selfClientDavConnection.sendData(resultData);
				}
				catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
					String message = "Telegramm konnte wegen fehlender Sendesteuerung nicht versendet werden."
					                 + " Dies kann hier wegen der Anmeldung als Quelle nicht vorkommen";
					_debug.error(message, sendSubscriptionNotConfirmed);
					throw new RuntimeException(message, sendSubscriptionNotConfirmed);
				}
			}
		}

		/** Verschickt ein Telegramm, das für alle angemeldeten Applikationen die Telegrammlaufzeit vom DaV zur Applikation enthält. */
		private void sendApplicationTelegramRuntimeUpdate() {
			if(_applicationRuntime.sendData()) {
				// Es gibt einen Empfänger für die Daten

				final ArrayList<T_A_HighLevelCommunication> applicationConnections;
				applicationConnections = new ArrayList<T_A_HighLevelCommunication>(_applicationConnections);

				final Data data = selfClientDavConnection.createData(_applicationRuntimeDataDescription.getAttributeGroup());
				final Data.Array subscribedApplications = data.getItem("telegrammLaufzeit").asArray();
				subscribedApplications.setLength(applicationConnections.size());

				// Index, an dem ein neues Element in das Array eingefügt werden muss
				int dataIndex = 0;

				for(T_A_HighLevelCommunication applicationConnection : applicationConnections) {
					if(!isSpecialTreatedApplicationObject(applicationConnection)) {
						// atl.telegrammLaufzeit
						final Data listEntry = subscribedApplications.getItem(dataIndex);

						final SystemObject subscribedApplicationSystemObject = dataModel.getObject(applicationConnection.getId());
						listEntry.getItem("applikation").asReferenceValue().setSystemObject(subscribedApplicationSystemObject);

						final int maxWaitingTime = 30000;
						long roundTripTime = 0;
						try {
							roundTripTime = applicationConnection.getTelegrammTime(maxWaitingTime);
							if(roundTripTime < 0) roundTripTime = maxWaitingTime;
						}
						catch(CommunicationError communicationError) {
							roundTripTime = maxWaitingTime;
							_debug.warning(
									"Fehler bei der Ermittlung der Telegrammlaufzeit: betroffene Applikation: "
									+ dataModel.getObject(applicationConnection.getId())
							);
						}
						listEntry.getItem("laufzeit").asUnscaledValue().set(roundTripTime);
						dataIndex++;
					}
					else {
						// Dieses Applikationsobjekt darf nicht mit verschickt werden. Also das ursprüngliche Array verkleinern.
						subscribedApplications.setLength(subscribedApplications.getLength() - 1);
					}
				} // for, über alle angemeldeten Applikationen

				final ResultData resultData = new ResultData(_davObject, _applicationRuntimeDataDescription, System.currentTimeMillis(), data);
				try {
					selfClientDavConnection.sendData(resultData);
				}
				catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
					String message = "Telegramm konnte wegen fehlender Sendesteuerung nicht versendet werden."
					                 + " Dies kann hier wegen der Anmeldung als Quelle nicht vorkommen";
					_debug.error(message, sendSubscriptionNotConfirmed);
					throw new RuntimeException(message, sendSubscriptionNotConfirmed);
				}
			}
		}

		/**
		 * Verschickt einen Datensatz mit der ATG "atg.angemeldeteDatenidentifikationen".
		 *
		 * @param application Applikation, deren angemeldete Datenidentifiaktionen propagiert werden sollen.
		 */
		private void sendDataDescriptionUpdate(T_A_HighLevelCommunication application) {
			SourceApplicationUpdater updater = null;
			synchronized(_subcribedDataIdentifications) {
				// Dieses Objekt hat die Sendesteuerung
				updater = _subcribedDataIdentifications.get(application);
			}
			if(updater != null) {
				if(updater.sendData()) {
					// Es gibt Empfänger für die Daten

					// Dieses Objekt besitzt alle Anmeldungen (Sender/Empfänger) und die entsprechenden Rollen(Quelle,Senke,Sender,Empfänger)
					final SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = application.getSubscriptionsFromRemoteStorage();

					// T_T sollte nicht gebraucht werden, da nur angemeldete Applikationen auf dem neusten Stand gehalten werden sollen.
					if(subscriptionsFromRemoteStorage instanceof SubscriptionsFromApplicationStorage) {

						final Data data = selfClientDavConnection.createData(_applicationDataDescriptionDD.getAttributeGroup());
						final Data.Array subscribedApplications = data.getItem("angemeldeteDatenidentifikation").asArray();

						// Alle angemldeten Datenidentifikation der Applikation anfordern

						final SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;

						// Alle Sendeanmeldungen (Quelle oder Sender)
						final List<SendSubscriptionInfo> allSendingSubscriptions = subscriptionsFromApplicationStorage.getSendingSubscriptions();
						final List<ReceiveSubscriptionInfo> allReceivingSubscription = subscriptionsFromApplicationStorage.getReceivingSubscription();

						subscribedApplications.setLength(allSendingSubscriptions.size() + allReceivingSubscription.size());

						// Index, an dem ein neues Element in das Array eingefügt werden muss
						int dataIndex = 0;

						// Alle Anmeldungen der Applikation eintrage, die Daten empfangen.
						for(ReceiveSubscriptionInfo receiveSubscriptionInfo : allReceivingSubscription) {

							// Es handelt sich um Anmeldungen, die Daten empfangen. Handelt es sich um eine Senke oder einen Empfänger?

							// Den nächsten Eintrag aus der Liste wählen
							final Data listEntry = subscribedApplications.getItem(dataIndex);
							dataIndex++;

							final String role;
							if(receiveSubscriptionInfo.isDrain()) {
								role = "Senke";
							}
							else {
								role = "Empfänger";
							}

							inscribeDataDescription(listEntry, receiveSubscriptionInfo.getBaseSubscriptionInfo(), role);
						}// for über alle Empfangsanmeldungen

						for(SendSubscriptionInfo sendingSubscription : allSendingSubscriptions) {
							// Es handelt sich um eine Anmeldung, die Daten verschickt. Sender oder Quelle ?

							// Den nächsten Eintrag aus der Liste wählen
							final Data listEntry = subscribedApplications.getItem(dataIndex);
							dataIndex++;

							final String role;
							if(sendingSubscription.isSource()) {
								role = "Quelle";
							}
							else {
								role = "Sender";
							}

							inscribeDataDescription(listEntry, sendingSubscription.getBaseSubscriptionInfo(), role);
						}// for über alle Sendeanmeldungen

						// Es sind alle Anmeldungen am Data vermerkt, der Datensatz kann verschickt werden.

						final SystemObject systemObjectApplication = dataModel.getObject(application.getId());

						final ResultData resultData = new ResultData(
								systemObjectApplication, _applicationDataDescriptionDD, System.currentTimeMillis(), data
						);

						try {
							selfClientDavConnection.sendData(resultData);
						}
						catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
							String message = "Telegramm konnte wegen fehlender Sendesteuerung nicht versendet werden."
							                 + " Dies kann hier wegen der Anmeldung als Quelle nicht vorkommen";
							_debug.error(message, sendSubscriptionNotConfirmed);
							throw new RuntimeException(message, sendSubscriptionNotConfirmed);
						}
					}
				}
			}
		}

		/**
		 * Schreibt in den übergebenen Datensatz(atl.angemeldeteDatenidentifikation) alle Daten benötigten Daten.
		 *
		 * @param data                 Datensatz (atl.angemeldeteDatenidentifikation)
		 * @param baseSubscriptionInfo Enthält das SystemObject(Id), die verwendete ATGV (Id) und die Simulationsvariante
		 * @param role                 Verwendete Rolle, siehe auch att.rolleAnmeldung. Zulässige Werte: Quelle, Sender, Senke, Empfänger
		 */
		private void inscribeDataDescription(
				final Data data, final BaseSubscriptionInfo baseSubscriptionInfo, final String role) {
			final SystemObject subscribedObject = dataModel.getObject(baseSubscriptionInfo.getObjectID());
			data.getItem("objekt").asReferenceValue().setSystemObject(subscribedObject);

			final AttributeGroupUsage usage = dataModel.getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification());
			data.getItem("attributgruppenverwendung").asReferenceValue().setSystemObject(usage);

			data.getItem("simulationsvariante").asUnscaledValue().set(baseSubscriptionInfo.getSimulationVariant());

			data.getItem("rolle").asTextValue().setText(role);
		}

		/**
		 * Erstellt einen TimerTask, der alls 60 Sekunden alle angemeldeten Applikationen und deren Telegrammlaufzeiten verschickt (wenn ein Empfänger angemeldet
		 * ist).
		 */
		private void createPeriodicUpdateTask() {
			final TimerTask task = new TimerTask() {
				public void run() {
					try {
						sendApplicationUpdate();
					}
					catch(Exception e) {
						_debug.warning("Fehler beim Versand der Statusinformation mit angemeldeten Applikationen", e);
						return;
					}
					try {
						sendApplicationTelegramRuntimeUpdate();
					}
					catch(Exception e) {
						_debug.warning("Fehler beim Versand der Statusinformation mit den Telegrammlaufzeiten der Applikationen", e);
					}
				}
			};
			// In 60 Sekunden starten, alle 60 Sekunden wiederholen
			_timer.schedule(task, 60000, 60000);
		}

		/**
		 * Meldet für eine neue Applikation eine Quelle an, die Datensätze vom mit der ATG "atg.angemeldeteDatenidentifikationen" verschickt.
		 * <p/>
		 * Gleichzeitig wird das Objekt in alle benötigten Datenstrukturen eingetragen.
		 * <p/>
		 * Hat bereits eine Anmeldung stattgefunden, wird nichts gemacht (zum Beispiel, wenn weiter DataDescription für eine Applikation angemeldet werden).
		 *
		 * @param newApplication enthält alle Daten zum anmelden.
		 */
		private void subscribeDataDescriptionSource(T_A_HighLevelCommunication newApplication) {
			synchronized(_subcribedDataIdentifications) {
				if(_subcribedDataIdentifications.containsKey(newApplication) == false) {
					// Die besonderen Applikationen(Konfiguration, Objekt, das den DaV darstellt) verschicken ihren Status nicht
					if(isSpecialTreatedApplicationObject(newApplication) == false) {
						// Bisher hat keine Anmeldung stattgefunden.
						final AttributeGroup attributeGroup = dataModel.getAttributeGroup(_pidAngemeldeteDatenidentifikationen);
						final Aspect aspect = dataModel.getAspect("asp.standard");

						final DataDescription dataDescription = new DataDescription(attributeGroup, aspect);

						final SystemObject applicationObject = dataModel.getObject(newApplication.getId());

						final SourceApplicationUpdater sourceApplicationUpdater = new SourceApplicationUpdater(this);

						try {
							selfClientDavConnection.subscribeSender(sourceApplicationUpdater, applicationObject, dataDescription, SenderRole.source());
							_subcribedDataIdentifications.put(newApplication, sourceApplicationUpdater);
						}
						catch(OneSubscriptionPerSendData oneSubscriptionPerSendData) {
							_debug.error(
									"Für eine Applikation kann keine Quelle angemeldet werden, die alle angemeldeten Datenidentifikationen der Applikation publiziert.",
									oneSubscriptionPerSendData
							);
						}
					}
				}
			}
		}

		/**
		 * Meldet die Datensätze der ATG "atg.angemeldeteDatenidentifikationen" wieder ab und entfernt die Objekte aus allen Datenstrukturen. Wurden die Daten bereits
		 * abgemeldet wird nichts gemacht.
		 * <p/>
		 * Eventuell angemeldete TimerTasks werden unterbrochen und entfernt.
		 *
		 * @param removedApplication Objekt, deren Quellenanmeldung zurückgenommen werden soll.
		 */
		private void unsubscribeDataDescriptionSource(T_A_HighLevelCommunication removedApplication) {
			SourceApplicationUpdater updater = null;
			synchronized(_subcribedDataIdentifications) {
				updater = _subcribedDataIdentifications.remove(removedApplication);
			}
			if(updater != null) {
				// Das Objekt war noch vorhanden, also können die Daten abgemeldet werden.
				final AttributeGroup attributeGroup = dataModel.getAttributeGroup(_pidAngemeldeteDatenidentifikationen);
				final Aspect aspect = dataModel.getAspect("asp.standard");

				final DataDescription dataDescription = new DataDescription(attributeGroup, aspect);

				final SystemObject applicationObject = dataModel.getObject(removedApplication.getId());

				selfClientDavConnection.unsubscribeSender(updater, applicationObject, dataDescription);
			}

			// Falls es noch einen TimerTask gibt, der die angemeldeten Datenidentifikationen verschicken will, wird dieser beendet.
			// Dies ist nötig, weil die Applikation sich vom DaV abgemeldet hat.
			synchronized(_threadsForDataIdentificationUpdates) {
				final TimerTask taskForDataIdentificationUpdate = _threadsForDataIdentificationUpdates.remove(removedApplication);
				if(taskForDataIdentificationUpdate != null) {
					taskForDataIdentificationUpdate.cancel();
				}
			}
		}

		public void run() {

			// Alle angemeldeten Applikationen und die Telegrammlaufzeit werden zyklisch alle 60 Sekunden verschickt.
			createPeriodicUpdateTask();

			while(isInterrupted() == false) {
				final ApplicationConnection newApplicationDetected;
				synchronized(_applicationConnections) {
					// Solange warten, bis es eine Applikation gibt, die bearbeitet werden muss
					while(_application.size() == 0) {
						// Wenn <code>_applicationConnections</code> sich ändert, wird in _application die Applikation abgelegt, die geändert wurde.
						try {
							_applicationConnections.wait();
						}
						catch(InterruptedException e) {
							_debug.error("Der Thread wurde mit Interrupt beendet", e);
							// Da der Thread beendet wurde, wird die while-Schleife verlassen ohne Daten zu verschicken (vielleicht gibt es zu diesem
							// Zeitpunkt auch nichts, was verschickt werden soll).
							return;
						}
					} // while, nichts zu tun
					//Fifo
					newApplicationDetected = _application.remove(0);
				} // synch neue Applikationen

				final T_A_HighLevelCommunication newApplicationConnection = newApplicationDetected.getApplicationConnection();

				if(newApplicationDetected.isAdded()) {
					// 1) Es wurde eine Applikation hinzugefügt -> Quelle für die angemeldetenDatenidentifikationen der Applikation anmelden.
					subscribeDataDescriptionSource(newApplicationConnection);
				}
				else {
					// 1) Quelle für angemeldetenDatenidentifikationen dieser Applikation abmelden und den TimerTask beenden(falls vorhanden)
					unsubscribeDataDescriptionSource(newApplicationConnection);
				}

				// Alle benachrichtigen, dass eine Applikation hinzugefügt oder entfernt wurde
				sendApplicationUpdate();
			}
		}

		/**
		 * Fügt eine neue Applikation den Datenstrukturen hinzu und der Thread, der Aktualisierungsdatensätze verschickt, wird aufgeweckt.
		 *
		 * @param applicationConnection Neue Applikation
		 */
		public void applicationAdded(final T_A_HighLevelCommunication applicationConnection) {
			synchronized(_applicationConnections) {
				_application.add(new ApplicationConnection(applicationConnection, true));
				// Es wurde eine Applikation hinzugefügt, der Thread muss aufwachen und ein Telegramm verschicken
				_applicationConnections.notifyAll();
			}
		}

		/**
		 * Speichert die entfernte Applikation und weckt den wartende Thread auf, der daraufhin eine Datensatz mit den aktuell angemeldeten Applikationen verschickt.
		 *
		 * @param applicationConnection Applikation, die entfernt wurde
		 */
		public void applicationRemoved(final T_A_HighLevelCommunication applicationConnection) {
			synchronized(_applicationConnections) {
				_application.add(new ApplicationConnection(applicationConnection, false));
				// Es wurde eine Applikation entfernt, der Thread muss aufwachen und ein Telegramm verschicken
				_applicationConnections.notifyAll();
			}
		}

		/**
		 * Diese Methode wird aufgerufen, wenn eine Applikation eine neue Datenidentifikation anmeldet.
		 * <p/>
		 * Für diese Applikation wird ein Datensatz verschickt, der alle angemeldeten Datenidentifikationen enthält.
		 *
		 * @param application Applikation, die neue Daten anmeldet.
		 */
		public void applicationSubscribedNewConnection(final T_A_HighLevelCommunication application) {
			publishConnectionChanged(application);
		}

		/**
		 * Diese Methode wird aufgerufen, wenn eine Applikation eine  Datenidentifikation abmeldet.
		 * <p/>
		 * Für diese Applikation wird ein Datensatz verschickt, der alle angemeldeten Datenidentifikationen enthält.
		 *
		 * @param application Applikation, die eine Datenidentifikation abmeldet.
		 */
		public void applicationUnsubscribeConnection(final T_A_HighLevelCommunication application) {
			publishConnectionChanged(application);
		}

		/**
		 * Wird aufgerufen, wenn eine Applikation einen Datenidentifikation an/abmeldet. Es wird ein TimerTask angelegt, der nach einer gewissen Zeit alle
		 * angemeldeten Datenidentifikationen der übergebene Applikation publiziert. Gibt es bereits einen TimerTask, der diese Aufgabe übernehmen möchte, so wird
		 * dieser gestoppt und durch einen neuen TimerTask ersetzt, der wieder eine gewisse Zeit wartet.
		 * <p/>
		 * Dadruch wird verhindert, dass sehr viele Anmeldungen eine Flut von Update-Telegrammen auslöst. Jede neue Anmeldung verzögert das Update-Telegramm, erst
		 * wenn alle Anmeldungen durchgeführt wurden, wird das Telegramm verschickt.
		 *
		 * @param application Applikation, die neue Daten an oder abmeldet.
		 */
		private void publishConnectionChanged(final T_A_HighLevelCommunication application) {
			synchronized(_threadsForDataIdentificationUpdates) {
				final TimerTask taskForThisApplication = _threadsForDataIdentificationUpdates.get(application);

				if(taskForThisApplication != null) {
					// Es gibt bereits einen Task, dieser kann bereits abgelaufen sein oder sich noch in Bearbeitung befinden.
					// (Auch wenn der Task bereits ausgeführt wurde, schadet dieser Aufruf nicht)
					taskForThisApplication.cancel();
				}

				// Es muss ein neuer Task angelegt werden
				final TimerTask task = new TimerTask() {
					public void run() {
						try {
							sendDataDescriptionUpdate(application);
						}
						catch(Exception e) {
							_debug.warning("Probleme beim Versenden von Zustandsinformation über angemeldete Datenidentifikationen", e);
						}
					}
				};
				_threadsForDataIdentificationUpdates.put(application, task);
				// In 5 Sekunden den Task starten, findet eine weitere Änderung statt, wird dieser Task mit cancel gestoppt und ein neuer angelegt
				_timer.schedule(task, 5000);
			}
		}
	}


	public void initializeTerminationQueries() {

		final SystemObject _davObject;
		final TerminationQueryReceiver _receiver = new TerminationQueryReceiver();
		DataDescription _terminationQueryDataDescription;

		_davObject = dataModel.getObject(_transmitterId);

		final AttributeGroup atgTerminierung = dataModel.getAttributeGroup(_pidTerminierung);
		final Aspect aspAnfrage = dataModel.getAspect("asp.anfrage");
		_terminationQueryDataDescription = new DataDescription(atgTerminierung, aspAnfrage);

		selfClientDavConnection.subscribeReceiver(_receiver, _davObject, _terminationQueryDataDescription, ReceiveOptions.normal(), ReceiverRole.drain());
	}

	/** Verarbeitet Anfragen, Anwendungen oder Dav-Verbindungen zu terminieren, oder den Datenverteiler zu beenden */
	private class TerminationQueryReceiver implements ClientReceiverInterface {

		public void update(final ResultData[] results) {
			for(ResultData result : results) {
				try {
					final Data data = result.getData();
					if(data != null) {
						final Data.ReferenceArray apps = data.getReferenceArray("Applikationen");
						final Data.ReferenceArray davs = data.getReferenceArray("Datenverteiler");
						for(Data.ReferenceValue app : apps.getReferenceValues()) {
							removeAppConnection(app.getId());
						}
						for(Data.ReferenceValue dav : davs.getReferenceValues()) {
							removeDavConnection(dav.getId());
						}
					}
				}
				catch(Exception e) {
					_debug.error("Fehler beim Ausführen einer Terminierungsanfrage:", e);
				}
			}
		}

		private void removeAppConnection(final long applicationId) {
			_debug.info("Erhalte Terminierungsanweisung für Anwendung-ID: " + applicationId);
			for(T_A_HighLevelCommunication applicationConnection : _applicationConnections) {
				if(applicationConnection.getId() == applicationId) {
					applicationConnection.terminate(false, "Terminierungsanfrage über Datenverteiler");
					return;
				}
			}
			_debug.warning("Es konnte zur Terminierung keine verbundene Anwendung mit der ID " + applicationId + " gefunden werden.");
		}

		private void removeDavConnection(final long id) {
			_debug.info("Erhalte Terminierungsanweisung für Datenverteiler-ID: " + id);
			if(id == _transmitterId) {
				close(false, "Terminierungsanfrage über Datenverteiler");
				return;
			}
			for(T_T_HighLevelCommunication transmitterConnection : _transmitterConnections) {
				if(transmitterConnection.getId() == id) {
					transmitterConnection.terminate(false, "Terminierungsanfrage über Datenverteiler");
					return;
				}
			}
			_debug.warning("Es konnte zur Terminierung kein verbundener Datenverteiler mit der ID " + id + " gefunden werden.");
		}
	}

	private static final class ApplicationConnection {

		final boolean _added;

		final T_A_HighLevelCommunication _applicationConnection;

		/**
		 * @param applicationConnection Verbindung der Applikation
		 * @param added                 <code>true</code>, wenn die Verbindung zum Datenverteiler hinzugeüfgt wurde. <code>false</code>, sonst.
		 */
		public ApplicationConnection(final T_A_HighLevelCommunication applicationConnection, final boolean added) {
			_applicationConnection = applicationConnection;
			_added = added;
		}


		public boolean isAdded() {
			return _added;
		}

		public T_A_HighLevelCommunication getApplicationConnection() {
			return _applicationConnection;
		}
	}

	private final class SourceApplicationUpdater implements ClientSenderInterface {

		private byte _state = ClientSenderInterface.STOP_SENDING;

		private final ApplicationStatusUpdater _sender;


		public SourceApplicationUpdater(final ApplicationStatusUpdater sender) {
			_sender = sender;
		}

		public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
			synchronized(this) {

				if(_state != ClientSenderInterface.START_SENDING && state == ClientSenderInterface.START_SENDING) {
					// Der Zustand wird von "nicht senden" auf "senden" geändert -> Daten verschicken
					if(dataDescription.getAttributeGroup().getPid().equals(_pidAngemeldeteApplikationen)) {
						final Thread helper = new Thread("StatusinfoversandApplikationen") {
							public void run() {
								try {
									_sender.sendApplicationUpdate();
								}
								catch(Exception e) {
									_debug.warning("Fehler beim Versand der Statusinformation bzgl. der angemeldeten Applikationen", e);
								}
							}
						};
						helper.setDaemon(true);
						helper.start();
					}
					else if(dataDescription.getAttributeGroup().getPid().equals(_pidTelegrammLaufzeiten)) {
						final Thread helper = new Thread("StatusinfoversandTelegrammlaufzeiten") {
							public void run() {
								try {
									_sender.sendApplicationTelegramRuntimeUpdate();
								}
								catch(Exception e) {
									_debug.warning("Fehler beim Versand der Statusinformation bzgl. der Telegrammlaufzeiten", e);
								}
							}
						};
						helper.setDaemon(true);
						helper.start();
					}
					else if(dataDescription.getAttributeGroup().getPid().equals(_pidAngemeldeteDatenidentifikationen)) {
						// Die beteiligte T_A_HighLevel finden
						final T_A_HighLevelCommunication application = getApplicationConnection(object.getId());
						// Abfrage, falls die Application entfernt wurde
						if(application != null) {
							final Thread helper = new Thread("StatusinfoversandAnmeldungen") {
								public void run() {
									try {
										_sender.sendDataDescriptionUpdate(application);
									}
									catch(Exception e) {
										_debug.warning("Fehler beim Versand der Statusinformation bzgl. der Anmeldungen einer Applikation", e);
									}
								}
							};
							helper.setDaemon(true);
							helper.start();
						}
					}
				}
				_state = state;
			}
		}

		public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
			return true;
		}

		public boolean sendData() {
			synchronized(this) {
				if(_state == ClientSenderInterface.START_SENDING) {
					return true;
				}
				else {
					return false;
				}
			}
		}
	}

	ListsManager getListsManager() {
		return listsManager;
	}
}
