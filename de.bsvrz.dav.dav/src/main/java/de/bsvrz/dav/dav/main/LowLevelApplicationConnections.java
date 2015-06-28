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

import de.bsvrz.dav.daf.communication.lowLevel.ConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.LowLevelCommunication;
import de.bsvrz.dav.daf.communication.lowLevel.ParameterizedConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.ServerConnectionInterface;
import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.main.ConnectionException;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Diese Klasse verwaltet Applikations-Verbindung auf unterster Protokoll-Ebene. Es wird über das ServerConnectionInterface auf neue Applikationsverbindungen
 * gewartet und aus dieser wird eine neue T_A_HighLevelCommunication-Klasse erzeugt und gespeichert. Dabei werden gegebenenfalls Verbindungsaufbau auf
 * Protokollebene, Authentifizierung usw. durchgeführt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public final class LowLevelApplicationConnections {

	private final ApplicationConnectionsSubscriber _applicationConnectionsSubscriber;

	private final ServerConnectionInterface _applicationsServerConnection;

	private final Map<Long, T_A_HighLevelCommunication> _applicationConnections = new ConcurrentHashMap<Long, T_A_HighLevelCommunication>(32, 0.75f, 6);

	private final Set<T_A_HighLevelCommunication> _unsortedApplicationConnections = Collections.synchronizedSet(new HashSet<T_A_HighLevelCommunication>());

	private final HighLevelApplicationManager _applicationManager;

	private final LowLevelConnectionsManagerInterface _lowLevelConnectionsManager;

	private final ServerDavParameters _serverDavParameters;

	private boolean _configurationAvailable = false;

	private static final Debug _debug = Debug.getLogger();

	/**
	 * Startet eine neue Klasse, die Applikations-Verbindung auf unterster Protokoll-Ebene entgegennimmt und verwaltet.
	 *
	 *
	 *
	 *
	 * @param lowLevelConnectionsManager    Authentifizierungs-Modul
	 *
	 * @param communicationProtocolClass Kommunikationsprotokoll-Klasse wie TCP/IP
	 * @param serverDavParameters        Server-Datenverteiler-Parameter
	 * @throws CommunicationError     Kommunikationsfehler
	 * @throws IllegalAccessException Fehler beim Instantiieren der Protokoll-Klasse
	 * @throws InstantiationException Fehler beim Instantiieren der Protokoll-Klasse
	 */
	public LowLevelApplicationConnections(
			final HighLevelApplicationManager applicationManager, final LowLevelConnectionsManagerInterface lowLevelConnectionsManager, final Class<? extends ServerConnectionInterface> communicationProtocolClass,
			final ServerDavParameters serverDavParameters) throws CommunicationError, IllegalAccessException, InstantiationException {
		_applicationManager = applicationManager;
		_lowLevelConnectionsManager = lowLevelConnectionsManager;

		_serverDavParameters = serverDavParameters;


		// Startet die Netzwerkschnittstelle, die auf Verbindungen wartet
		_applicationsServerConnection = startApplicationConnectionListener(communicationProtocolClass);

		// Den Thread starten, der auf dieser Netzwerkschnittstelle auf ankommende Verbindungen wartet
		_applicationConnectionsSubscriber = new ApplicationConnectionsSubscriber();
		_applicationConnectionsSubscriber.start();
	}

	private ServerConnectionInterface startApplicationConnectionListener(final Class<? extends ServerConnectionInterface> communicationProtocolClass)
			throws InstantiationException, IllegalAccessException, CommunicationError {
		//Start the listener at the application port
		final ServerConnectionInterface applicationsServerConnection;
		applicationsServerConnection = communicationProtocolClass.newInstance();
		// Falls vorhanden und möglich Parameter für das Kommunikationsinterface weitergeben
		final String communicationParameters = _serverDavParameters.getLowLevelCommunicationParameters();
		if(communicationParameters.length() != 0 && applicationsServerConnection instanceof ParameterizedConnectionInterface) {
			final ParameterizedConnectionInterface parameterizedConnection = (ParameterizedConnectionInterface)applicationsServerConnection;
			parameterizedConnection.setParameters(communicationParameters);
		}
		applicationsServerConnection.connect(_serverDavParameters.getApplicationConnectionsSubAddress());

		return applicationsServerConnection;
	}

	public void continueAuthentication() {
		for(final T_A_HighLevelCommunication connection : getApplicationConnections()) {
			if(connection != null) {
				connection.continueAuthentification();
			}
		}
		_configurationAvailable = true;
	}

	/**
	 * Setzt dass die Konfigurationsverbindung erfolgreich hergestellt ist
	 *
	 * @param _applicationTypePid
	 * @param _applicationName
	 */
	public void continueAuthentication(final String _applicationTypePid, final String _applicationName) {
		for(final T_A_HighLevelCommunication _applicationConnection : getApplicationConnections()) {
			final String applicationTypePid = _applicationConnection.getApplicationTypePid();
			final String applicationName = _applicationConnection.getApplicationName();
			if((applicationTypePid == null) || (applicationName == null)) {
				continue;
			}
			if(applicationTypePid.equals(_applicationTypePid) && applicationName.equals(_applicationName)) {
				_applicationConnection.continueAuthentification();
				break;
			}
		}
	}

	T_A_HighLevelCommunication getApplicationConnection(final long applicationId) {
		return _applicationConnections.get(applicationId);
	}

	public synchronized void close(final boolean error, final String message) {

		_applicationConnectionsSubscriber.interrupt();
		if(_applicationsServerConnection != null) {
			_applicationsServerConnection.disconnect();
		}

		for(final T_A_HighLevelCommunication applicationConnection : _unsortedApplicationConnections) {
			applicationConnection.terminate(error, message);
		}
		_unsortedApplicationConnections.clear();
		for(final T_A_HighLevelCommunication applicationConnection : _applicationConnections.values()) {
			applicationConnection.terminate(error, message);
		}
		_applicationConnections.clear();
	}

	public synchronized void updateId(final T_A_HighLevelCommunication communication) {
		final boolean removed = _unsortedApplicationConnections.remove(communication);
		if(!removed) {
			// Kann möglicherweise vorkommen, wenn Applikation zwischenzeitlich abgemeldet wird
			_debug.fine("Fehler beim Updaten einer Id: " + communication);
			return;
		}
		_applicationConnections.put(communication.getId(), communication);
	}

	public synchronized Collection<T_A_HighLevelCommunication> getApplicationConnections() {
		final List<T_A_HighLevelCommunication> result = new ArrayList<T_A_HighLevelCommunication>();
		result.addAll(_unsortedApplicationConnections);
		result.addAll(_applicationConnections.values());
		return result;
	}

	/** Diese Subklasse startet einen Thread, der eine Application bei einem Datenverteiler anmeldet. */
	private class ApplicationConnectionsSubscriber extends Thread {

		public ApplicationConnectionsSubscriber() {
			super("ApplicationConnectionsSubscriber");
		}

		/** The run method that loops through */
		@Override
		public final void run() {
			if(_applicationsServerConnection == null) {
				return;
			}
			while(!isInterrupted()) {
				final ConnectionInterface connection = _applicationsServerConnection.accept();
				final Thread thread = new Thread(
						new Runnable() {
							@Override
							public void run() {
								if(connection == null) {
									return;
								}
								try {
									startApplicationConnection(connection);
								}
								catch(ConnectionException ex) {
									ex.printStackTrace();
								}
							}
						}
				);
				thread.start();
			}
		}
	}

	private void startApplicationConnection(final ConnectionInterface connection) throws ConnectionException {
		final LowLevelCommunication lowLevelCommunication = createLowLevelConnection(connection, true);
		final ServerConnectionProperties properties = new ServerConnectionProperties(
				lowLevelCommunication, _lowLevelConnectionsManager.getLowLevelAuthentication().getAuthenticationComponent(), _serverDavParameters
		);
		final T_A_HighLevelCommunication highLevelCommunication = new T_A_HighLevelCommunication(
				properties, _applicationManager, _lowLevelConnectionsManager, !_configurationAvailable
		);

		addApplicationConnection(highLevelCommunication);
	}

	private void addApplicationConnection(final T_A_HighLevelCommunication highLevelCommunication) {
		_unsortedApplicationConnections.add(highLevelCommunication);
	}

	public boolean removeApplicationConnection(final T_A_HighLevelCommunication applicationCommunication) {
		if(_unsortedApplicationConnections.remove(applicationCommunication)) return true;
		return _applicationConnections.remove(applicationCommunication.getId()) != null;
	}

	private LowLevelCommunication createLowLevelConnection(final ConnectionInterface connection, final boolean connected) throws ConnectionException {
		return new LowLevelCommunication(
				connection,
				_serverDavParameters.getDavCommunicationOutputBufferSize(),
				_serverDavParameters.getDavCommunicationInputBufferSize(),
				_serverDavParameters.getSendKeepAliveTimeout(),
				_serverDavParameters.getReceiveKeepAliveTimeout(),
				LowLevelCommunication.NORMAL_MODE,
				connected
		);
	}

	@Override
	public String toString() {
		return "LowLevelApplicationConnections{" + "_configurationAvailable=" + _configurationAvailable + ", _applicationConnections=" + _applicationConnections
		       + '}';
	}
}
