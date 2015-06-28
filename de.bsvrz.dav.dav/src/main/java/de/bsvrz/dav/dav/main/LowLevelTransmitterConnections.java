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
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.impl.ConfigurationManager;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public final class LowLevelTransmitterConnections {

	private static final Debug _debug = Debug.getLogger();

	private static final short DEFAULT_WEIGHT = 1;

	/**
	 * Wartezeit in ms, bevor versucht wird, eine abgebrochene Verbindung wiederherzustellen
	 */
	private int _reconnectionDelay = 10000;

	/** Threadpool, der nicht etablierte Verbindungen aufbaut */
	private ScheduledExecutorService _transmitterReconnectService = Executors.newScheduledThreadPool(1);

	/** Die Serverkommunikationskomponente der Datenverteiler, die Datenverteilersverbindungen akzeptiert. */
	@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})/* Dieses Feld ist Semi-final und braucht nicht synchronisiert zu werden */
	private ServerConnectionInterface _transmittersServerConnection = null;

	/** Infos zur Verbindungsherstellung mit anderen Datenverteilern */
	@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"}) /* Dieses Feld ist Semi-final und braucht nicht synchronisiert zu werden */
	private TransmitterConnectionInfo[] _transmitterConnectionInfos = null;

	private final Set<T_T_HighLevelCommunication> _unsortedTransmitterConnections = Collections.synchronizedSet(new HashSet<T_T_HighLevelCommunication>());

	/** Liste der Datenverteilerverbindungen */
	private final Map<Long, T_T_HighLevelCommunication> _transmitterConnections = new ConcurrentHashMap<Long, T_T_HighLevelCommunication>();

	/** Interne Thread zur Kommunikation */
	private TransmitterConnectionsSubscriber _transmitterConnectionsSubscriber;

	private final HighLevelTransmitterManager _transmitterManager;

	private final ServerDavParameters _serverDavParameters;

	private final LowLevelConnectionsManagerInterface _lowLevelConnectionsManager;

	private final long _myTransmitterId;

	public LowLevelTransmitterConnections(
			final HighLevelTransmitterManager transmitterManager, final Class<? extends ServerConnectionInterface> communicationProtocolClass,
			final ServerDavParameters serverDavParameters,
			final LowLevelConnectionsManagerInterface lowLevelConnectionsManager) {
		_transmitterManager = transmitterManager;
		_serverDavParameters = serverDavParameters;
		_lowLevelConnectionsManager = lowLevelConnectionsManager;
		_myTransmitterId = transmitterManager.getMyTransmitterId();
		_transmitterConnectionsSubscriber = new TransmitterConnectionsSubscriber();
	    long reconnectInterDavDelay = _serverDavParameters.getReconnectInterDavDelay();
	    if(reconnectInterDavDelay < 0 || reconnectInterDavDelay > Integer.MAX_VALUE){
		    throw new IllegalArgumentException("Ungültige Wiederverbindungs-Wartezeit: " + reconnectInterDavDelay + "ms");
	    }
	    _reconnectionDelay = (int) reconnectInterDavDelay;
    }

	/** Startet Transmitter-Anmeldungen. Kann nicht direkt im Konstruktor aufgerufen werden, da dieses Objekt existieren muss,
	 * wenn die ersten Verbindungen gestartet werden. Siehe {@link LowLevelConnectionsManager#LowLevelConnectionsManager(ServerDavParameters)}
	 * @param communicationProtocolClass Kommunikationsprotokoll
	 * @param dataModel Datenmodell
	 * @throws InstantiationException -
	 * @throws IllegalAccessException -
	 * @throws CommunicationError -
	 */
	public void startTransmitterConnections(
			final Class<? extends ServerConnectionInterface> communicationProtocolClass, final DataModel dataModel)
			throws InstantiationException, IllegalAccessException, CommunicationError {
		try {

			final ConfigurationManager configurationManager = ((DafDataModel)dataModel).getConfigurationManager();
			if(configurationManager != null) {
				_transmitterConnectionInfos = configurationManager.getTransmitterConnectionInfo(_myTransmitterId);

				_transmittersServerConnection = communicationProtocolClass.newInstance();

				final String communicationParameters2 = _serverDavParameters.getLowLevelCommunicationParameters();
				if(communicationParameters2.length() != 0 && _transmittersServerConnection instanceof ParameterizedConnectionInterface) {
					final ParameterizedConnectionInterface connectionInterface = (ParameterizedConnectionInterface)_transmittersServerConnection;
					connectionInterface.setParameters(communicationParameters2);
				}				

				_transmittersServerConnection.connect(getListenSubadress());

				_transmitterConnectionsSubscriber = new TransmitterConnectionsSubscriber();
				_transmitterConnectionsSubscriber.start();

				connectToNeighbours();
			}
		}
		catch(RuntimeException ex) {
			close(true, ex.getMessage());
		}
	}

	private int getListenSubadress() {
		int davDavSubadress = analyseConnectionInfosAndGetSubadress();

		if(davDavSubadress == -1) {
			davDavSubadress = this._serverDavParameters.getTransmitterConnectionsSubAddress();
		}
		int subAddressToListenFor = davDavSubadress;
		if(subAddressToListenFor == 100000) {
			// Zu Testzwecken wird die Portnummer mit dem Wert 100000 serverseitig durch 8088 und clientseitig durch 8081 ersetzt
			subAddressToListenFor = 8088;
		}

		subAddressToListenFor += _serverDavParameters.getTransmitterConnectionsSubAddressOffset();
		return subAddressToListenFor;
	}

	private int analyseConnectionInfosAndGetSubadress() {
		int davDavSubadress = -1;
		for(int i = 0; i < _transmitterConnectionInfos.length; ++i) {
			_debug.finer("Datenverteilerverbindung", _transmitterConnectionInfos[i].parseToString());
			if(_transmitterConnectionInfos[i] != null) {
				final TransmitterInfo transmitterInfo_1 = _transmitterConnectionInfos[i].getTransmitter_1();
				final TransmitterInfo transmitterInfo_2 = _transmitterConnectionInfos[i].getTransmitter_2();
				final long id1 = transmitterInfo_1.getTransmitterId();
				final long id2 = transmitterInfo_2.getTransmitterId();
				if(id1 == id2) {
					close(true, "Inkonsistente Netztopologie (Verbindung von Datenverteiler[" + id1 + "] zu sich selbst");
				}
				final int subAdresse1 = transmitterInfo_1.getSubAdress();
				final int subAdresse2 = transmitterInfo_2.getSubAdress();
				for(int j = i + 1; j < _transmitterConnectionInfos.length; ++j) {
					if(_transmitterConnectionInfos[j] != null) {
						final long tmpId1 = _transmitterConnectionInfos[j].getTransmitter_1().getTransmitterId();
						final long tmpId2 = _transmitterConnectionInfos[j].getTransmitter_2().getTransmitterId();
						if((id1 == tmpId1) && (id2 == tmpId2)) {
							close(
									true,
									"Inkonsistente Netztopologie (Mehrfache Verbindung zwichen Datenverteiler[" + id1 + "] und Datenverteiler[" + id2
									+ "] möglich)"
							);
						}
					}
				}
				if(id1 == _myTransmitterId) {
					if(davDavSubadress == -1) {
						davDavSubadress = subAdresse1;
					}
					else if(davDavSubadress != subAdresse1) {
						close(
								true, "Inkonsistente Netztopologie (Es wurden dem Datenverteiler[" + id1 + "] verschiedene Subadressen zugewiesen"
						);
					}
				}
				if(id2 == _myTransmitterId) {
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
		return davDavSubadress;
	}

	/** Verbindet mit den Nachbardatenverteilern */
	private void connectToNeighbours() {
		if(_transmitterConnectionInfos == null) {
			return;
		}
		for(final TransmitterConnectionInfo info : _transmitterConnectionInfos) {
			if(info.isExchangeConnection()) {
				continue;
			}
			if(!info.isActiveConnection()) {
				continue;
			}
			TransmitterInfo t1 = info.getTransmitter_1();
			if(t1.getTransmitterId() == _myTransmitterId) {
				if(!connectToMainTransmitter(info)) {
					connectToAlternativeTransmitters(info);
					scheduleTransmitterConnect(info, _reconnectionDelay, TimeUnit.MILLISECONDS);
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
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo, short, long, String)
	 */
	private boolean connectToMainTransmitter(final TransmitterConnectionInfo transmitterConnectionInfo) {
		final TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
		final short weight = transmitterConnectionInfo.getWeight();
		final long waitingTime = transmitterConnectionInfo.getConnectionTimeThreshold();
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
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo, short, long, String)
	 */
	private void connectToAlternativeTransmitters(final TransmitterConnectionInfo transmitterConnectionInfo) {
		final TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
		if(transmitterConnectionInfo.isAutoExchangeTransmitterDetectionOn()) {
			final List<TransmitterConnectionInfo> infos = getInvolvedTransmitters(t2);
			for(final TransmitterConnectionInfo info : infos) {
				final TransmitterInfo transmitterInfo = info.getTransmitter_2();
				final short weight = info.getWeight();
				final long time = info.getConnectionTimeThreshold();
				if(transmitterInfo != null) {
					connectToTransmitter(transmitterInfo, weight, time, transmitterConnectionInfo.getUserName());
				}
			}
		}
		else {
			final TransmitterInfo[] infos = transmitterConnectionInfo.getExchangeTransmitterList();
			if(infos != null) {
				for(final TransmitterInfo info : infos) {
					TransmitterConnectionInfo tmpTransmitterConnectionInfo = null;
					for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
						if(_transmitterConnectionInfo.isExchangeConnection()
						   && (_transmitterConnectionInfo.getTransmitter_1().getTransmitterId() == _myTransmitterId) && (
								_transmitterConnectionInfo.getTransmitter_2().getTransmitterId() == info.getTransmitterId())) {
							tmpTransmitterConnectionInfo = _transmitterConnectionInfo;
							break;
						}
					}
					if(tmpTransmitterConnectionInfo != null) {
						final short weight = tmpTransmitterConnectionInfo.getWeight();
						final long time = tmpTransmitterConnectionInfo.getConnectionTimeThreshold();
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
	 * @see #connectToTransmitter(de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterInfo, short, String, String)
	 */
	private boolean connectToTransmitter(final TransmitterInfo t_info, final short weight, final long time, String userName) {
		final String password;
		if("".equals(userName)) {
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
		final long startTime = System.currentTimeMillis();
		do {
			try {
				connectToTransmitter(t_info, weight, userName, password);
				return true;
			}
			catch(ConnectionException ex) {
				_debug.warning("Verbindung zum " + t_info + " konnte nicht aufgebaut werden", ex);
				if(System.getProperty("agent.name") != null) {
					// Wenn aus Testumgebung gestartet
					System.out.println("Verbindung zum " + t_info + " konnte nicht aufgebaut werden: " + ex);
					ex.printStackTrace();
				}
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
				if(System.getProperty("agent.name") != null) {
					// Wenn aus Testumgebung gestartet
					System.out.println("Verbindung zum " + t_info + " konnte nicht aufgebaut werden: " + ex);
					ex.printStackTrace();
				}
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

	/**
	 * Etabliert Verbindung zwischen zwei Datenverteilern. Falls keine Verbindung aufgebaut werden konnte, wird eine entsprechende Ausnahme geworfen.
	 *
	 * @param t_info   Informationen zum Datenverteiler.
	 * @param weight   Die Information wird von der Wegverwaltung benutzt, wenn eine Verbindung bewertet wird.
	 * @param userName Benutzername mit dem die Authentifizierung durchgeführt werden soll.
	 * @param password Passwort des Benutzers mit dem die Authentifizierung durchgeführt werden soll.
	 *
	 * @throws de.bsvrz.dav.daf.main.ConnectionException
	 *          wenn eine Verbindung nicht aufgebaut werden konnte
	 * @throws de.bsvrz.dav.daf.main.CommunicationError
	 *          wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind
	 */
	private void connectToTransmitter(final TransmitterInfo t_info, final short weight, final String userName, final String password)
			throws ConnectionException, CommunicationError {
		if(_transmittersServerConnection == null) {
			throw new IllegalArgumentException("Die Verwaltung ist nicht richtig initialisiert.");
		}

		final long tId = t_info.getTransmitterId();

		int subAddressToConnectTo = t_info.getSubAdress();
		if(subAddressToConnectTo == 100000) {
			// Zu Testzwecken wird die Portnummer mit dem Wert 100000 serverseitig durch 8088 und clientseitig durch 8081 ersetzt
			subAddressToConnectTo = 8081;
		}
		subAddressToConnectTo += _serverDavParameters.getTransmitterConnectionsSubAddressOffset();

		for(final T_T_HighLevelCommunication transmitterConnection : _transmitterConnections.values()) {
			if(transmitterConnection != null) {
				if(transmitterConnection.getId() == tId) {
					return;
				}
				else {
					final String adress = transmitterConnection.getRemoteAdress();
					final int subAdress = transmitterConnection.getRemoteSubadress();
					if((adress != null) && (adress.equals(t_info.getAdress())) && (subAddressToConnectTo == subAdress)) {
						return;
					}
				}
			}
		}
		final T_T_HighLevelCommunication highLevelCommunication = startTransmitterConnection(t_info, weight, userName, password, subAddressToConnectTo);
		addConnection(highLevelCommunication);
		highLevelCommunication.connect();
		highLevelCommunication.completeInitialisation();

		_debug.info("Verbindungsaufbau zum " + t_info + " war erfolgreich");
	}

	private void addConnection(final T_T_HighLevelCommunication highLevelCommunication) {
		_unsortedTransmitterConnections.add(highLevelCommunication);
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung aufgerufen, um einer Verbindung ein Gewicht zuzuweisen. Die Information wird von der Wegverwaltung benutzt,
	 * wenn eine Verbindung bewertet wird.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Gewichtung der Verbindung
	 */
	public final short getWeight(final long connectedTransmitterId) {
		short weight = DEFAULT_WEIGHT;
		if(_transmitterConnectionInfos != null) {
			for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
				final TransmitterInfo t1 = _transmitterConnectionInfo.getTransmitter_1();
				if(t1 != null) {
					if(t1.getTransmitterId() == _myTransmitterId) {
						final TransmitterInfo t2 = _transmitterConnectionInfo.getTransmitter_2();
						if((t2 != null) && (t2.getTransmitterId() == connectedTransmitterId)) {
							weight = _transmitterConnectionInfo.getWeight();
						}
					}
					else if(t1.getTransmitterId() == connectedTransmitterId) {
						final TransmitterInfo t2 = _transmitterConnectionInfo.getTransmitter_2();
						if((t2 != null) && (t2.getTransmitterId() == _myTransmitterId)) {
							weight = _transmitterConnectionInfo.getWeight();
						}
					}
				}
			}
		}
		return weight;
	}

	/**
	 * Erstellt ein Array, das die Informationen über die benachbarten Datenverteiler des übergebenen Datenverteilers enthält.
	 *
	 * @param t_info Information zum Datenverteiler
	 *
	 * @return Liste mit benachbarten Datenverteilern
	 */
	private List<TransmitterConnectionInfo> getInvolvedTransmitters(final TransmitterInfo t_info) {
		final List<TransmitterConnectionInfo> list = new ArrayList<TransmitterConnectionInfo>();
		for(final TransmitterConnectionInfo _transmitterConnectionInfo1 : _transmitterConnectionInfos) {
			if((_transmitterConnectionInfo1 == null) || _transmitterConnectionInfo1.isExchangeConnection()) {
				continue;
			}
			final TransmitterInfo t1 = _transmitterConnectionInfo1.getTransmitter_1();
			if(t1.getTransmitterId() == t_info.getTransmitterId()) {
				final TransmitterInfo t2 = _transmitterConnectionInfo1.getTransmitter_2();
				if(t2 != null) {
					for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
						if(_transmitterConnectionInfo == null) {
							continue;
						}
						if(_transmitterConnectionInfo.isExchangeConnection()) {
							final TransmitterInfo _t1 = _transmitterConnectionInfo.getTransmitter_1();
							final TransmitterInfo _t2 = _transmitterConnectionInfo.getTransmitter_2();
							if((_t1.getTransmitterId() == _myTransmitterId) && (_t2.getTransmitterId() == t2.getTransmitterId())) {
								list.add(_transmitterConnectionInfo);
								break;
							}
						}
					}
				}
			}
		}
		return list;
	}

	private void startTransmitterConnection(final ConnectionInterface connection) throws ConnectionException {
		final LowLevelCommunication lowLevelCommunication = createLowLevelConnection(connection, true);
		final ServerConnectionProperties properties = new ServerConnectionProperties(
				lowLevelCommunication, _lowLevelConnectionsManager.getLowLevelAuthentication().getAuthenticationComponent(), _serverDavParameters
		);
		final T_T_HighLevelCommunication highLevelCommunication = createTransmitterHighLevelCommunication(DEFAULT_WEIGHT, "", "", properties);
		addConnection(highLevelCommunication);
	}


	private T_T_HighLevelCommunication startTransmitterConnection(
			final TransmitterInfo t_info, final short weight, final String userName, final String password, final int subAddressToConnectTo)
			throws ConnectionException {
		final ConnectionInterface connection = _transmittersServerConnection.getPlainConnection();
		final LowLevelCommunication lowLevelCommunication = createLowLevelConnection(connection, false);
		final ServerConnectionProperties properties = new ServerConnectionProperties(
				lowLevelCommunication, _lowLevelConnectionsManager.getLowLevelAuthentication().getAuthenticationComponent(), _serverDavParameters
		);
		lowLevelCommunication.connect(t_info.getAdress(), subAddressToConnectTo);
		return createTransmitterHighLevelCommunication(weight, userName, password, properties);
	}

	private T_T_HighLevelCommunication createTransmitterHighLevelCommunication(
			final short weight, final String userName, final String password, final ServerConnectionProperties properties) {
		return new T_T_HighLevelCommunication(
				properties, _transmitterManager, _lowLevelConnectionsManager, weight, false, userName, password
		);
	}

	T_T_HighLevelCommunication getTransmitterConnection(final long transmitterId) {

		for(T_T_HighLevelCommunication unsortedTransmitterConnection : _unsortedTransmitterConnections) {
			if(unsortedTransmitterConnection.getId() == transmitterId){
				return unsortedTransmitterConnection;
			}
		}

		return _transmitterConnections.get(transmitterId);
	}

	public void close(final boolean error, final String message) {
		_transmitterReconnectService.shutdown();
		_transmitterConnectionsSubscriber.interrupt();
		if(_transmittersServerConnection != null) {
			_transmittersServerConnection.disconnect();
		}
		for(T_T_HighLevelCommunication unsortedTransmitterConnection : _unsortedTransmitterConnections) {
			unsortedTransmitterConnection.terminate(error,  message);
		}
		for(final T_T_HighLevelCommunication transmitterConnection : _transmitterConnections.values()) {
			transmitterConnection.terminate(error, message);
		}
		_transmitterConnections.clear();
	}

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung von diesem Datenverteiler zum angegebenen Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	public TransmitterConnectionInfo getTransmitterConnectionInfo(final long connectedTransmitterId) {
		if(_transmitterConnectionInfos != null) {
			for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
				_debug.finest("getTransmitterConnectionInfo: prüfe", _transmitterConnectionInfo.parseToString());
				final TransmitterInfo t1 = _transmitterConnectionInfo.getTransmitter_1();
				if(t1.getTransmitterId() == _myTransmitterId) {
					final TransmitterInfo t2 = _transmitterConnectionInfo.getTransmitter_2();
					if(t2.getTransmitterId() == connectedTransmitterId) {
						_debug.finer("getTransmitterConnectionInfo: gefunden", _transmitterConnectionInfo.parseToString());
						return _transmitterConnectionInfo;
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
	public TransmitterConnectionInfo getRemoteTransmitterConnectionInfo(final long connectedTransmitterId) {
		if(_transmitterConnectionInfos != null) {
			for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
				_debug.finest("getRemoteTransmitterConnectionInfo: prüfe", _transmitterConnectionInfo.parseToString());
				final TransmitterInfo t1 = _transmitterConnectionInfo.getTransmitter_1();
				final TransmitterInfo t2 = _transmitterConnectionInfo.getTransmitter_2();
				if(t2.getTransmitterId() == _myTransmitterId && t1.getTransmitterId() == connectedTransmitterId) {
					_debug.finer("getRemoteTransmitterConnectionInfo: gefunden", _transmitterConnectionInfo.parseToString());
					return _transmitterConnectionInfo;
				}
			}
		}
		_debug.finer("getRemoteTransmitterConnectionInfo: nicht gefunden", connectedTransmitterId);
		return null;
	}

	public boolean removeTransmitterConnection(final T_T_HighLevelCommunication transmitterCommunication) {

		// Unsortierte (uninitialisierte) Verbindungen direkt entfernen. Wiederaufbau ist sinnlos, da Remote-Dav-Id unbekannt.
		if(_unsortedTransmitterConnections.remove(transmitterCommunication)) return true;

		boolean result = _transmitterConnections.remove(transmitterCommunication.getId()) != null;

		// Verbindung abgebrochen -> Wiederaufzubauen, falls keine Ersatzverbindung
		if(_transmitterConnectionInfos != null) {
			for(final TransmitterConnectionInfo info : _transmitterConnectionInfos) {
				if((info == null) || info.isExchangeConnection()) {
					continue;
				}
				TransmitterInfo t1 = info.getTransmitter_1();
				if((t1 != null) && (t1.getTransmitterId() == _myTransmitterId)) {
					TransmitterInfo t2 = info.getTransmitter_2();
					if((t2 != null) && (t2.getTransmitterId() == transmitterCommunication.getId())) {
						scheduleTransmitterConnect(info, _reconnectionDelay, TimeUnit.MILLISECONDS);
						break;
					}
				}
			}
		}

		return result;
	}

	public synchronized Collection<T_T_HighLevelCommunication> getTransmitterConnections() {
		final List<T_T_HighLevelCommunication> result = new ArrayList<T_T_HighLevelCommunication>();
		result.addAll(_unsortedTransmitterConnections);
		result.addAll(_transmitterConnections.values());
		return result;
	}

	public synchronized void updateId(final T_T_HighLevelCommunication communication) {
		if(_unsortedTransmitterConnections.remove(communication)) {
			_transmitterConnections.put(communication.getId(), communication);
		}
	}

	class TransmitterConnectionsSubscriber extends Thread {

		public TransmitterConnectionsSubscriber() {
			super("TransmitterConnectionsSubscriber");
		}

		/** The run method that loops through */
		@Override
		public final void run() {
			if(_transmittersServerConnection == null) {
				return;
			}
			while(!isInterrupted()) {
				// Blocks until a connection occurs:
				final ConnectionInterface connection = _transmittersServerConnection.accept();
				final Runnable runnable = new Runnable() {
					@Override
					public void run() {
						if(connection != null) {
							try {
								boolean connectionExists = false;
								for(final T_T_HighLevelCommunication transmitterConnection : _transmitterConnections.values()) {
									if(transmitterConnection != null) {
										final String adress = transmitterConnection.getRemoteAdress();
										final int subAdress = transmitterConnection.getRemoteSubadress();
										if((adress != null) && (adress.equals(connection.getMainAdress())) && (subAdress == connection.getSubAdressNumber())) {
											connectionExists = true;
											break;
										}
									}
								}
								if(!connectionExists) {
									startTransmitterConnection(connection);
								}
							}
							catch(ConnectionException ex) {
								ex.printStackTrace();
							}
						}
					}
				};
				final Thread thread = new Thread(runnable);
				thread.start();
			}
		}
	}


	private void disableReplacementConnection(final TransmitterConnectionInfo transmitterConnectionInfo) {
		if(transmitterConnectionInfo.isAutoExchangeTransmitterDetectionOn()) {
			final TransmitterInfo t2 = transmitterConnectionInfo.getTransmitter_2();
			final List<TransmitterConnectionInfo> infos = getInvolvedTransmitters(t2);
			for(final TransmitterConnectionInfo info : infos) {
				try {
					final TransmitterInfo transmitterInfo = info.getTransmitter_2();
					if(transmitterInfo != null) {
						terminateReplacementConnection(transmitterInfo, true);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		else {
			final TransmitterInfo[] infos = transmitterConnectionInfo.getExchangeTransmitterList();
			if(infos != null) {
				for(final TransmitterInfo info : infos) {
					TransmitterConnectionInfo transmitterConnectionInfoToDisconnect = null;
					for(final TransmitterConnectionInfo _transmitterConnectionInfo : _transmitterConnectionInfos) {
						if(_transmitterConnectionInfo.isExchangeConnection()
						   && (_transmitterConnectionInfo.getTransmitter_1().getTransmitterId() == _myTransmitterId) && (
								_transmitterConnectionInfo.getTransmitter_2().getTransmitterId() == info.getTransmitterId())) {
							transmitterConnectionInfoToDisconnect = _transmitterConnectionInfo;
							break;
						}
					}
					if(transmitterConnectionInfoToDisconnect != null) {
						try {
							terminateReplacementConnection(info, false);
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void terminateReplacementConnection(final TransmitterInfo transmitterInfo, final boolean automatic) {
		final T_T_HighLevelCommunication connection = getTransmitterConnection(transmitterInfo.getTransmitterId());
		if((connection != null) && (!connection.isAcceptedConnection())) {
			connection.terminate(
					false,
					(automatic ? "Automatisch ermittelte " : "Konfigurierte ")
					+ "Ersatzverbindung wird nicht mehr benötigt, weil ursprüngliche Verbindung wiederhergestellt wurde"
			);
		}
	}

	class TransmitterReconnectionTask implements Runnable {

		private final TransmitterConnectionInfo _transmitterConnectionInfo;

		public TransmitterReconnectionTask(TransmitterConnectionInfo transmitterConnectionInfo) {
			_transmitterConnectionInfo = transmitterConnectionInfo;
		}

		/**
		 * Behandelt den Verbindungsaufbau mit einem entfernten Datenverteiler (Transmitter)
		 */
		@Override
		public final void run() {
			if(_lowLevelConnectionsManager.isClosing()) return;

			Thread.currentThread().setName("TransmitterConnectionsMonitor");
			//XXX setPriority(Thread.MIN_PRIORITY);

			if(_transmitterConnectionInfo != null) {
				if(connectToMainTransmitter(_transmitterConnectionInfo)) {
					// Verbindung erfolgreich wiederhergestellt, Ersatzverbindungen (falls vorhanden) entfernen.
					disableReplacementConnection(_transmitterConnectionInfo);
				}
				else {
					try {
						// Verbindung kann nicht aufgebaut werden, sicherstellen, dass eventuelle Ersatzverbindungen aufgebaut werden.
						connectToAlternativeTransmitters(_transmitterConnectionInfo);
					}
					finally {
						// Nach ein paar Sekunden neuen Verbindungsversuch starten.
						scheduleTransmitterConnect(_transmitterConnectionInfo, _reconnectionDelay, TimeUnit.MILLISECONDS);
					}
				}

			}
		}
	}

	private void scheduleTransmitterConnect(final TransmitterConnectionInfo transmitterConnectionInfo, final int delay, final TimeUnit timeUnit) {
		if(_lowLevelConnectionsManager.isClosing()) return;
		_transmitterReconnectService.schedule(new TransmitterReconnectionTask(transmitterConnectionInfo), delay, timeUnit);
	}

}
