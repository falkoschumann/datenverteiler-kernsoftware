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
import de.bsvrz.dav.daf.communication.lowLevel.ServerConnectionInterface;
import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Klasse, die die Verbindungen des Datenverteilers verwaltet. Diese Klasse initialisiert die LowLevelApplicationConnections,
 * LowLevelTransmitterConnections, stellt eine Klasse für die Passwort-Prüfung von Benutzern bereit und initialisiert schließlich den
 * HighLevelConnectionsManager.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11473 $
 */
public class LowLevelConnectionsManager implements LowLevelConnectionsManagerInterface {

	private static final Debug _debug = Debug.getLogger();

	private final ServerDavParameters _serverDavParameters;

	private final long _transmitterId;

	private final long _configurationId;

	private final LowLevelApplicationConnections _lowLevelApplicationConnections;

	private final LowLevelTransmitterConnections _lowLevelTransmitterConnections;

	private final SelfClientDavConnection _selfClientDavConnection;

	private final SystemObject _myTransmitter;

	private final ClientDavParameters _clientDavParameters;

	private final LowLevelAuthentication _lowLevelAuthentication;

	private volatile boolean _closing = false;

	private final HighLevelConnectionsManager _highLevelConnectionsManager;

	private boolean _waitForParamDone = false;

	private final Object _waitForParamLock = new Object();

	/**
	 * Erzeugt eine neue Verbindungsverwaltung für den Datenverteiler.
	 *
	 * @param serverDavParameters Die Parameter sind u. a. die Adressen und Subadressen der Kommunikationskanäle
	 *
	 */
	public LowLevelConnectionsManager(final ServerDavParameters serverDavParameters) throws DavInitializationException {

		try {
			// Grundlegende Initialisierungen
			_debug.fine("Starte LowLevelConnectionsManager");
			_serverDavParameters = serverDavParameters;
			_clientDavParameters = _serverDavParameters.getClientDavParameters();
			_transmitterId = _serverDavParameters.getDataTransmitterId();

			// HighLevelConnectionsManager bereitstellen.
			_debug.fine("DatenverteilerID = " + _transmitterId);
			_highLevelConnectionsManager = new HighLevelConnectionsManager(this, _serverDavParameters.getUserRightsChecking());

			// Kommunikations und Authentifizierungs-Klassen für die HighLevelCommunication-Klassen initialisieren
			final Class<? extends ServerConnectionInterface> communicationProtocolClass = getCommunicationsProtocolClass();
			final AuthentificationComponent authenticationComponent = getAuthenticationComponent();

			// Komponente zur Authentifizierung bereitstellen. Es gibt zwar noch keine Konfiguration, aber die lokale Datenverteilerverbindung muss angemeldet
			// werden, um überhaupt auf die Konfiguration zugreifen zu können. Um dieses Henne-Ei-problem zu umgehen hat die LowLevelAuthentication eine
			// Spezialbehandlung damit lokale Datenverteilerverbindung, Konfiguration und Parametrierung ohne vorhandene Konfiguration angemeldet werden können.
			_lowLevelAuthentication = new LowLevelAuthentication(_serverDavParameters, _clientDavParameters, _transmitterId, authenticationComponent);

			// Kommunikation mit Applikationen starten
			_lowLevelApplicationConnections = new LowLevelApplicationConnections(
					_highLevelConnectionsManager.getApplicationManager(),
			        this,
			        communicationProtocolClass, _serverDavParameters
			);

			// Eigene Datenverteilerverbindung starten
			_debug.fine("Starte Kommunikation mit eigener Datenverteiler-Verbindung");
			_selfClientDavConnection = new SelfClientDavConnection(_clientDavParameters);
			_selfClientDavConnection.getConnection().addConnectionListener(new DavConnectionListener() {
				@Override
				public void connectionClosed(final ClientDavInterface connection) {
					shutdown(true, "Datenverteilerverbindung verloren");
				}
			});

			_highLevelConnectionsManager.setSelfClientDavConnection(_selfClientDavConnection);

			// Jetzt auch die Anmeldung für andere Anwendungen erlauben
			_debug.fine("Verbindungsaufbau für normale Applikationen ermöglichen");
			_lowLevelAuthentication.setSelfClientDavConnection(_selfClientDavConnection);
			_lowLevelApplicationConnections.continueAuthentication();

			_configurationId = _selfClientDavConnection.getConnection().getLocalConfigurationAuthority().getId();
			_serverDavParameters.setConfigurationId(_configurationId);

			_myTransmitter = getTransmitterObject(_selfClientDavConnection.getDataModel());

			if(_serverDavParameters.getWaitForParamApp()) try {
				waitForParamReady(_serverDavParameters.getParamAppIncarnationName());
			}
			catch(InterruptedException e) {
				_debug.warning("Thread wurde beim Warten auf Parametrierung unterbrochen", e);
			}

			final long delay = _serverDavParameters.getInitialInterDavServerDelay();
			_debug.config("Verbindungen von und zu anderen Datenverteilern werden in " + delay + " ms zugelassen");
			try {
				Thread.sleep(delay);
			}
			catch(InterruptedException e) {
				_debug.warning("Thread wurde beim Warten auf Verbindungsaufbau zu anderen Datenverteilern unterbrochen", e);
			}

			_debug.finer("Datenverteiler-ID", _transmitterId);
			_debug.config("Verbindungen von und zu anderen Datenverteilern werden jetzt zugelassen");

			// Datenverteiler-Verbindungen vorbereiten
			_lowLevelTransmitterConnections = new LowLevelTransmitterConnections(
					_highLevelConnectionsManager.getTransmitterManager(), communicationProtocolClass, _serverDavParameters, this
			);

			// Erst jetzt die Datenverteilerverbindungen starten. Würde dieser Aufruf im Konstruktor von LowLevelTransmitterConnections stattfinden,
			// wäre das Field _lowLevelTransmitterConnections noch nicht initialisiert.
			_lowLevelTransmitterConnections.startTransmitterConnections(communicationProtocolClass, _selfClientDavConnection.getDataModel());
		}
		catch(ClassNotFoundException e) {
			throw new DavInitializationException("Eine angegebene Klasse konnte nicht gefunden werden", e);
		}
		catch(CommunicationError e) {
			throw new DavInitializationException("Beim Starten des Datenverteilers trat ein Kommunikationsfehler auf", e);
		}
		catch(MissingParameterException e) {
			throw new DavInitializationException("Ein notwendiger Parameter wurde nicht angegeben", e);
		}
		catch(IllegalAccessException e) {
			throw new DavInitializationException("Das Kommunikationsprotokoll konnte nicht initialisiert werden", e);
		}
		catch(InstantiationException e) {
			throw new DavInitializationException("Das Kommunikationsprotokoll konnte nicht initialisiert werden", e);
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
			DataModel config = _selfClientDavConnection.getDataModel();
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
					ResultData result = _selfClientDavConnection.getConnection().getData(paramApp, readyMessageDataDescription, 30000);
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

	public HighLevelConnectionsManager getHighLevelConnectionsManager() {
		return _highLevelConnectionsManager;
	}

	/**
	 * Gibt das Transmitter-Objekt zurück. Wirft eine IllegalArgumentException und produziert entsprechende Debug-Ausgabe wenn die angefragte DatenverteilerID zu
	 * keinem Datenverteiler der Konfiguration passt.
	 *
	 * @param dataModel Datenmodell
	 *
	 * @return Das Objekt, dass diesen Datenverteiler repräsentiert
	 */
	private SystemObject getTransmitterObject(final DataModel dataModel) {
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
			final Formatter formatter = new Formatter();
			formatter.format("%40s %22s %s\n", "PID", "ID", "NAME");
			for(final SystemObject dav : davs) {
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
		return davObject;
	}

	private AuthentificationComponent getAuthenticationComponent() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		final String authentificationName = _serverDavParameters.getAuthentificationProcessName();
		if(authentificationName == null) {
			throw new InitialisationNotCompleteException("Unbekanntes Authentifizierungsverfahren.");
		}
		final Class<? extends AuthentificationProcess> authenticationClass = Class.forName(authentificationName).asSubclass(AuthentificationProcess.class);
		if(authenticationClass == null) {
			throw new InitialisationNotCompleteException("Unbekanntes Authentifizierungsverfahren.");
		}
		final AuthentificationProcess authenticationProcess = authenticationClass.newInstance();
		return new AuthentificationComponent(authenticationProcess);
	}

	private Class<? extends ServerConnectionInterface> getCommunicationsProtocolClass() throws ClassNotFoundException {
		final String communicationProtocolName = _serverDavParameters.getLowLevelCommunicationName();
		if(communicationProtocolName == null) {
			throw new InitialisationNotCompleteException("Kommunikationsprotokollname ungültig.");
		}
		final Class<? extends ServerConnectionInterface> communicationProtocolClass = Class.forName(communicationProtocolName).asSubclass(
				ServerConnectionInterface.class
		);
		if(communicationProtocolClass == null) {
			throw new InitialisationNotCompleteException("Kommunikationsprotokollname ungültig.");
		}
		return communicationProtocolClass;
	}

	@Override
	public final long getTransmitterId() {
		return _transmitterId;
	}

	/**
	 * @param error   True: ein Fehler ist aufgetreten
	 * @param message genauere Beschreibung des Fehlers
	 */
	@Override
	public final void shutdown(final boolean error, final String message) {
		// Außerhalb von synchronized ausführen, um Deadlocks zu vermeiden, wenn mehrere Threads aus unterschiedlichen
		// Quellen (mit unterschiedlichen Locks) gleichzeitig Shutdown ausführen.
		if(_closing) return;
		
		synchronized(this) {
			_closing = true;

			final String debugMessage = "Der Datenverteiler wird beendet. Ursache: " + message;
			if(error) {
				_debug.error(debugMessage);
			}
			else {
				_debug.warning(debugMessage);
			}
			if(_lowLevelTransmitterConnections != null) _lowLevelTransmitterConnections.close(error, message);
			_lowLevelApplicationConnections.close(error, message);
			try {
				_selfClientDavConnection.getConnection().disconnect(error, message);
			}
			catch(Exception e) {
				_debug.fine("Beende lokale Datenverteiler-Verbindung", e);
			}
		}
	}

	@Override
	public SelfClientDavConnection getSelfClientDavConnection() {
		return _selfClientDavConnection;
	}

	@Override
	public SystemObject getMyTransmitter() {
		return _myTransmitter;
	}

	@Override
	public String toString() {
		return "LowLevelConnectionsManager{" + "_myTransmitter=" + _myTransmitter + ", _configurationId=" + _configurationId + '}';
	}

	@Override
	public void removeConnection(final T_A_HighLevelCommunication connection) {
		_lowLevelApplicationConnections.removeApplicationConnection(connection);
	}


	@Override
	public void removeConnection(final T_T_HighLevelCommunication connection) {
		_lowLevelTransmitterConnections.removeTransmitterConnection(connection);
	}

	@Override
	public String getLocalModeConfigurationPid() {
		final Object[] localeModeParameter = _serverDavParameters.getLocalModeParameter();
		if(localeModeParameter == null) return "";
		return (String)localeModeParameter[0];
	}

	@Override
	public long getLocalModeConfigurationId() {
		final Object[] localeModeParameter = _serverDavParameters.getLocalModeParameter();
		if(localeModeParameter == null) return -1;
		return (Long)localeModeParameter[1];
	}

	@Override
	public LowLevelAuthenticationInterface getLowLevelAuthentication() {
		return _lowLevelAuthentication;
	}

	@Override
	public void setLocalModeParameter(final String configurationPid, final long configurationId) {
		_serverDavParameters.setLocalModeParameter(configurationPid, configurationId);
	}

	@Override
	public void setLocalConfigurationAvailable() {
		_lowLevelApplicationConnections.continueAuthentication(_clientDavParameters.getApplicationTypePid(), _clientDavParameters.getApplicationName());
	}

	@Override
	public ServerDavParameters getServerDavParameters() {
		return _serverDavParameters;
	}

	@Override
	public ClientDavParameters getClientDavParameters() {
		return _clientDavParameters;
	}

	@Override
	public T_T_HighLevelCommunication getTransmitterConnection(final long transmitterId) {
		if(_lowLevelTransmitterConnections == null) return null;
		return _lowLevelTransmitterConnections.getTransmitterConnection(transmitterId);
	}

	@Override
	public T_A_HighLevelCommunication getApplicationConnection(final long applicationId) {
		return _lowLevelApplicationConnections.getApplicationConnection(applicationId);
	}

	@Override
	public short getWeight(final long transmitterId) {
		return _lowLevelTransmitterConnections.getWeight(transmitterId);
	}

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung von diesem Datenverteiler zum angegebenen Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	@Override
	public TransmitterConnectionInfo getTransmitterConnectionInfo(final long connectedTransmitterId) {
		return _lowLevelTransmitterConnections.getTransmitterConnectionInfo(connectedTransmitterId);
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
		return _lowLevelTransmitterConnections.getRemoteTransmitterConnectionInfo(connectedTransmitterId);
	}

	@Override
	public void updateApplicationId(final T_A_HighLevelCommunication communication) {
		_lowLevelApplicationConnections.updateId(communication);
	}

	@Override
	public void updateTransmitterId(final T_T_HighLevelCommunication communication) {
		_lowLevelTransmitterConnections.updateId(communication);
	}

	@Override
	public Collection<T_A_HighLevelCommunication> getApplicationConnections() {
		return _lowLevelApplicationConnections.getApplicationConnections();
	}

	@Override
	public Collection<T_T_HighLevelCommunication> getTransmitterConnections() {
		if(_lowLevelTransmitterConnections == null) return Collections.emptyList();
		return _lowLevelTransmitterConnections.getTransmitterConnections();
	}

	@Override
	public boolean isClosing() {
		return _closing;
	}

	@Override
	public long login(final String userName, final byte[] userPassword, final String authentificationText, final AuthentificationProcess authentificationProcess, final String applicationTypePid) {
		long userId = _lowLevelAuthentication.isValidUser(
				userName, userPassword, authentificationText, authentificationProcess, applicationTypePid
		);
		if(userId > 0 && userId != _transmitterId) {
			try {
				if(_serverDavParameters.getWaitForParamApp()) waitForParamReady(_serverDavParameters.getParamAppIncarnationName());
			}
			catch(InterruptedException e) {
				_debug.info("Warten auf Parametrierung wurde unterbrochen", e);
				return -1L;
			}
			_highLevelConnectionsManager.userLoggedIn(userId);
		}
		return userId;
	}
}
