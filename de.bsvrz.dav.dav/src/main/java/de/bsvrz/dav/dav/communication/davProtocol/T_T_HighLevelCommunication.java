/*
 * Copyright 2009 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

package de.bsvrz.dav.dav.communication.davProtocol;

import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.SendDataObject;
import de.bsvrz.dav.daf.communication.lowLevel.ConnectionInterface;
import de.bsvrz.dav.daf.communication.lowLevel.HighLevelCommunicationCallbackInterface;
import de.bsvrz.dav.daf.communication.lowLevel.LowLevelCommunicationInterface;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.*;
import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.util.Longs;
import de.bsvrz.dav.dav.main.AuthentificationComponent;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.HighLevelTransmitterManagerInterface;
import de.bsvrz.dav.dav.main.LowLevelConnectionsManagerInterface;
import de.bsvrz.dav.dav.main.ServerConnectionProperties;
import de.bsvrz.dav.dav.main.Transmitter;
import de.bsvrz.dav.dav.subscriptions.RemoteCentralSubscription;
import de.bsvrz.dav.dav.subscriptions.RemoteSourceSubscription;
import de.bsvrz.dav.dav.subscriptions.RemoteSubscription;
import de.bsvrz.dav.dav.subscriptions.SendingSubscription;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.LinkedList;
import java.util.ListIterator;


/**
 * Diese Klasse stellt die Funktionalitäten für die Kommunikation zwischen zwei Datenverteilern zur Verfügung. Hier wird die Verbindung zwischen zwei DAV
 * aufgebaut, sowie die Authentifizierung durchgeführt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12020 $
 */
public class T_T_HighLevelCommunication implements T_T_HighLevelCommunicationInterface, HighLevelCommunicationCallbackInterface {

	private static final Debug _debug = Debug.getLogger();

	/** Die Id des über diesen Kanal verbundenen Datenverteiler */
	private long _connectedTransmitterId;

	/** Die Id des Remotebenutzers */
	private long _remoteUserId;

	/** Die erste Ebene der Kommunikation */
	private LowLevelCommunicationInterface _lowLevelCommunication;

	/** Die Eigenschaften dieser Verbindung */
	private ServerConnectionProperties _properties;

	/** Die unterstützten Versionen des Datenverteilers */
	private int[] _versions;

	/** Die Version, mit der die Kommunikation erfolgt */
	private int _version;

	/** Die Authentifizierungskomponente */
	private AuthentificationComponent _authentificationComponent;

	/** Temporäre Liste der Systemtelegramme für interne Synchronisationszwecke. */
	private final LinkedList<DataTelegram> _syncSystemTelegramList;

	/** Temporäre Liste der Telegramme, die vor die Initialisierung eingetroffen sind. */
	private final LinkedList<DataTelegram> _fastTelegramsList;

	/** Gewichtung dieser Verbindung */
	private short _weight;

	/** Signalisiert ob die Initialisierungsphase abgeschlossen ist */
	private boolean _initComplete = false;

	/** Die Information ob auf die Konfiguration gewartet werden muss. */
	private boolean _waitForConfiguration;

	/** Objekt zur internen Synchronization */
	private Integer _sync;

	/** Objekt zur internen Synchronization */
	private Integer _authentificationSync;

	/** Legt fest ob eine gegen Authentifizierung notwendig ist. */
	private boolean _isAcceptedConnection;

	/** Signalisiert dass diese Verbindung terminiert ist */
	private volatile boolean _closed = false;

	private Object _closedLock = new Object();

	/** Benutzername mit dem sich dieser Datenverteiler beim anderen Datenverteiler authentifizieren soll */
	private String _authentifyAsUser;

	private final HighLevelTransmitterManagerInterface _transmitterManager;

	private final LowLevelConnectionsManagerInterface _lowLevelConnectionsManager;

	/** Passwort des Benutzers mit dem sich dieser Datenverteiler beim anderen Datenverteiler authentifizieren soll */
	private String _authentifyWithPassword;

//	private final Set<RemoteSubscription> _remoteSubscriptions = new HashSet<RemoteSubscription>();

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param properties             Eigenschaften dieser Verbindung
	 * @param lowLevelConnectionsManager
	 * @param weight                 Gewichtung dieser Verbindung
	 * @param waitForConfiguration   true: auf die KOnfiguration muss gewartet werden, false: Konfiguration ist vorhanden
	 * @param authentifyAsUser       Benutzername mit dem sich dieser Datenverteiler beim anderen Datenverteiler authentifizieren soll
	 * @param authentifyWithPassword Passwort des Benutzers mit dem sich dieser Datenverteiler beim anderen Datenverteiler authentifizieren soll
	 */
	public T_T_HighLevelCommunication(
			ServerConnectionProperties properties,
			HighLevelTransmitterManagerInterface transmitterManager, final LowLevelConnectionsManagerInterface lowLevelConnectionsManager, short weight,
			boolean waitForConfiguration,
			final String authentifyAsUser,
			final String authentifyWithPassword) {
		_transmitterManager = transmitterManager;
		_lowLevelConnectionsManager = lowLevelConnectionsManager;
		_authentifyWithPassword = authentifyWithPassword;
		_authentifyAsUser = authentifyAsUser;
		_connectedTransmitterId = -1;
		_versions = new int[1];
		_versions[0] = 2;
		_weight = weight;
		_properties = properties;
		_lowLevelCommunication = _properties.getLowLevelCommunication();
		_authentificationComponent = _properties.getAuthentificationComponent();
		_syncSystemTelegramList = new LinkedList<DataTelegram>();
		_fastTelegramsList = new LinkedList<DataTelegram>();
		_waitForConfiguration = waitForConfiguration;
		_sync = hashCode();
		_authentificationSync = hashCode();
		_isAcceptedConnection = true;
		_lowLevelCommunication.setHighLevelComponent(this);
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um eine logische Verbindung zwischen zwei Datenverteilern herzustellen. Zunächst wird die
	 * Protokollversion verhandelt. In einem Systemtelegramm ?TransmitterProtocolVersionRequest? werden die unterstützten Versionen über die Telegrammverwaltung an
	 * den zweiten Datenverteiler gesendet. Auf die Antwort wird eine gewisse Zeit gewartet (maximale Wartezeit auf synchrone Antworten). Wenn die Antwort
	 * innerhalb diese Zeit nicht angekommen bzw. keine der Protokollversionen vom anderen Datenverteiler unterstützt wird, wird eine CommunicationErrorAusnahme
	 * erzeugt. <br>Danach erfolgt die Authentifizierung: Über die Telegrammverwaltung wird ein Telegramm? TransmitterAuthentificationTextRequest? zum anderen
	 * Datenverteiler gesendet, um einen Schlüssel für die Authentifizierung anzufordern. Die ID des sendenden Datenverteilers wird den ServerConnectionProperties
	 * entnommen. Auf die Antwort ?TransmitterAuthentificationTextAnswer? wird eine gewisse Zeit gewartet (maximale Wartezeit auf synchrone Antworten). Wenn die
	 * Antwort nicht innerhalb dieser Zeit angekommen ist, wird eine CommunicationError-Ausnahme erzeugt. Das Passwort, welches in den ServerConnectionProperties
	 * spezifiziert ist, wird mit diesem Schlüssel und dem spezifizierten Authentifizierungsverfahren verschlüsselt. Aus dem Authentifizierungsverfahrennamen, dem
	 * verschlüsselten Passwort und dem Benutzernamen wird ein ?TransmitterAuthentificationRequest?-Telegramm gebildet und mittels Telegrammverwaltung zum anderen
	 * Datenverteiler gesendet. Auf die Antwort ?TransmitterAuthentificationAnswer? wird eine gewisse Zeit gewartet (maximale Wartezeit auf synchrone Antworten).
	 * Wenn die Antwort nicht innerhalb dieser Zeit angekommen ist oder konnte die Authentifizierung nicht erfolgreich abgeschlossen werden, so wird eine
	 * CommunicationError-Ausnahme erzeugt <br>Danach geht diese Methode geht in den Wartezustand, bis der andere Datenverteiler sich in umgekehrter Richtung auch
	 * erfolgreich authentifiziert hat. Dabei durchläuft der andere Datenverteiler das gleiche Prozedere wie zuvor beschrieben. <br>Im nächsten Schritt verhandeln
	 * die Datenverteiler die Keep-alive-Parameter und die Durchsatzprüfungsparameter (Verbindungsparameter). Ein ?TransmitterComParametersRequest? wird zum
	 * anderen Datenverteiler gesendet. Auch hier wird eine gewisse Zeit auf die Antwort ?TransmitterComParametersAnswer? gewartet (maximale Wartezeit auf
	 * synchrone Antworten). Wenn die Antwort nicht innerhalb dieser Zeit angekommen ist, wird eine CommunicationError-Ausnahme erzeugt. Sonst ist der
	 * Verbindungsaufbau erfolgreich abund der Austausch von Daten kann sicher durchgeführt werden.
	 *
	 * @throws CommunicationError , wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind
	 */
	public final void connect() throws CommunicationError {
		_syncSystemTelegramList.clear();
		_isAcceptedConnection = false;
		// Protokollversion verhandeln
		TransmitterProtocolVersionRequest protocolVersionRequest = new TransmitterProtocolVersionRequest(_versions);
		sendTelegram(protocolVersionRequest);

		TransmitterProtocolVersionAnswer protocolVersionAnswer = (TransmitterProtocolVersionAnswer)waitForAnswerTelegram(
				DataTelegram.TRANSMITTER_PROTOCOL_VERSION_ANSWER_TYPE, "Antwort auf Verhandlung der Protokollversionen"
		);
		_version = protocolVersionAnswer.getPreferredVersion();
		int i = 0;
		for(; i < _versions.length; ++i) {
			if(_version == _versions[i]) {
				break;
			}
		}
		if(i >= _versions.length) {
			throw new CommunicationError("Der Datenverteiler unterstüzt keine der gegebenen Versionen.\n");
		}

		_remoteUserId = 0;

		authentify();

		synchronized(_authentificationSync) {
			try {
				while(_remoteUserId == 0) {
					if(_closed) return;
					_authentificationSync.wait(1000);
				}
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
				return;
			}
		}
		if(_remoteUserId < 0) return;

		// Timeouts Parameter verhandeln
		TransmitterComParametersRequest comParametersRequest = new TransmitterComParametersRequest(
				_properties.getKeepAliveSendTimeOut(), _properties.getKeepAliveReceiveTimeOut()
		);
		sendTelegram(comParametersRequest);
		TransmitterComParametersAnswer comParametersAnswer = (TransmitterComParametersAnswer)waitForAnswerTelegram(
				DataTelegram.TRANSMITTER_COM_PARAMETER_ANSWER_TYPE, "Antwort auf Verhandlung der Kommunikationsparameter"
		);
		_lowLevelCommunication.updateKeepAliveParameters(
				comParametersAnswer.getKeepAliveSendTimeOut(), comParametersAnswer.getKeepAliveReceiveTimeOut()
		);
	}

	public LowLevelConnectionsManagerInterface getLowLevelConnectionsManager() {
		return _lowLevelConnectionsManager;
	}

	/** @return Liefert <code>true</code> zurück, falls die Verbindung geschlossen wurde, sonst <code>false</code>. */
	boolean isClosed() {
		return _closed;
	}


	private DataTelegram waitForAnswerTelegram(final byte telegramType, final String descriptionOfExpectedTelegram) throws CommunicationError {
		long waitingTime = 0;
		long startTime = System.currentTimeMillis();
		long sleepTime = 10;
		final String expected = (" Erwartet wurde: " + descriptionOfExpectedTelegram);
		while(waitingTime < CommunicationConstant.MAX_WAITING_TIME_FOR_SYNC_RESPONCE) {
			try {
				synchronized(_syncSystemTelegramList) {
					if(_closed) throw new CommunicationError("Verbindung terminiert." + expected);
					_syncSystemTelegramList.wait(sleepTime);
					if(sleepTime < 1000) sleepTime *= 2;
					ListIterator<DataTelegram> iterator = _syncSystemTelegramList.listIterator(0);
					while(iterator.hasNext()) {
						final DataTelegram telegram = iterator.next();
						if(telegram != null) {
							if(telegram.getType() == telegramType) {
								iterator.remove();
								return telegram;
							}
							else {
								System.out.println(telegram.parseToString());
							}
						}
					}
				}
				waitingTime = System.currentTimeMillis() - startTime;
			}
			catch(InterruptedException ex) {
				throw new CommunicationError("Interrupt." + expected);
			}
		}
		throw new CommunicationError("Der Datenverteiler antwortet nicht." + expected);
	}


	@Override
	public final long getTelegramTime(final long maxWaitingTime) throws CommunicationError {
		long time = System.currentTimeMillis();
		TransmitterTelegramTimeRequest telegramTimeRequest = new TransmitterTelegramTimeRequest(time);
		sendTelegram(telegramTimeRequest);

		TransmitterTelegramTimeAnswer telegramTimeAnswer = null;
		long waitingTime = 0, startTime = System.currentTimeMillis();
		long sleepTime = 10;
		while(waitingTime < maxWaitingTime) {
			try {
				synchronized(_syncSystemTelegramList) {
					if(_closed) throw new CommunicationError("Verbindung terminiert. Erwartet wurde: Antwort auf eine Telegrammlaufzeitermittlung");
					_syncSystemTelegramList.wait(sleepTime);
					if(sleepTime < 1000) sleepTime *= 2;
					ListIterator<DataTelegram> iterator = _syncSystemTelegramList.listIterator(0);
					while(iterator.hasNext()) {
						final DataTelegram telegram = iterator.next();
						if((telegram != null) && (telegram.getType() == DataTelegram.TRANSMITTER_TELEGRAM_TIME_ANSWER_TYPE)) {
							if(((TransmitterTelegramTimeAnswer)telegram).getTelegramStartTime() == time) {
								telegramTimeAnswer = (TransmitterTelegramTimeAnswer)telegram;
								iterator.remove();
								break;
							}
						}
					}
					if(telegramTimeAnswer != null) {
						break;
					}
				}
				waitingTime = System.currentTimeMillis() - startTime;
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
				throw new CommunicationError("Thread wurde unterbrochen.", ex);
			}
		}
		if(telegramTimeAnswer == null) {
			return -1;
		}
		return telegramTimeAnswer.getRoundTripTime();
	}

	@Override
	public final long getRemoteNodeId() {
		return _connectedTransmitterId;
	}

	@Override
	public final int getThroughputResistance() {
		return _weight;
	}

	@Override
	public final void sendRoutingUpdate(RoutingUpdate[] routingUpdates) {
		if(routingUpdates == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		sendTelegram(new TransmitterBestWayUpdate(routingUpdates));
	}

	@Override
	public final long getRemoteUserId() {
		return _remoteUserId;
	}

	@Override
	public final long getId() {
		return _connectedTransmitterId;
	}

	/**
	 * Gibt die Information zurück, ob diese Verbindung von dem anderen Datenverteiler akzeptiert wurde.
	 *
	 * @return true: verbindung wurde akzeptiert, false: Verbindung wurde nicht akzeptiert.
	 */
	public final boolean isAcceptedConnection() {
		return _isAcceptedConnection;
	}

	/** @return  */
	public final String getRemoteAdress() {
		if(_lowLevelCommunication == null) {
			return null;
		}
		ConnectionInterface connection = _lowLevelCommunication.getConnectionInterface();
		if(connection == null) {
			return null;
		}
		return connection.getMainAdress();
	}


	/**
	 * Diese Methode gibt die Subadresse des Kommunikationspartners zurück.
	 *
	 * @return die Subadresse des Kommunikationspartners
	 */
	public final int getRemoteSubadress() {
		if(_lowLevelCommunication == null) {
			return -1;
		}
		ConnectionInterface connection = _lowLevelCommunication.getConnectionInterface();
		if(connection == null) {
			return -1;
		}
		return connection.getSubAdressNumber();
	}

	@Override
	public void continueAuthentification() {
		synchronized(_sync) {
			_waitForConfiguration = false;
			_sync.notify();
		}
	}

	@Override
	public void terminate(boolean error, String message) {
		final DataTelegram terminationTelegram;
		if(error) {
			terminationTelegram = new TerminateOrderTelegram(message);
		}
		else {
			terminationTelegram = new ClosingTelegram();
		}
		terminate(error, message, terminationTelegram);
	}

	public final void terminate(boolean error, String message, DataTelegram terminationTelegram) {
		synchronized(_closedLock) {
			if(_closed) return;
			_closed = true;
		}
		synchronized(this) {
			String debugMessage = "Verbindung zum Datenverteiler " + getId() + " wird terminiert. Ursache: " + message;
			if(error) {
				_debug.error(debugMessage);
			}
			else {
				_debug.info(debugMessage);
			}
			if(_lowLevelCommunication != null) {
				_lowLevelCommunication.disconnect(error, message, terminationTelegram);
			}
			_transmitterManager.connectionTerminated(this);
		}
	}


	@Override
	public void disconnected(boolean error, final String message) {
		terminate(error, message);
	}

	@Override
	public void updateConfigData(SendDataObject receivedData) {
		throw new UnsupportedOperationException("updateConfigData nicht implementiert");
	}


	public void sendTelegram(DataTelegram telegram) {
		if(Transmitter._debugLevel > 5) System.err.println("T_T  -> " + telegram.toShortDebugString());
		_lowLevelCommunication.send(telegram);
	}

	public void sendTelegrams(DataTelegram[] telegrams) {
		_lowLevelCommunication.send(telegrams);
	}

	@Override
	public void update(DataTelegram telegram) {
		if(Transmitter._debugLevel > 5) {
			System.err.println("T_T <-  " + (telegram == null ? "null" : telegram.toShortDebugString()));
		}
		if(telegram == null) {
			return;
		}
		switch(telegram.getType()) {
			case DataTelegram.TRANSMITTER_PROTOCOL_VERSION_REQUEST_TYPE: {
				TransmitterProtocolVersionRequest protocolVersionRequest = (TransmitterProtocolVersionRequest)telegram;
				int version = getPrefferedVersion(protocolVersionRequest.getVersions());
				TransmitterProtocolVersionAnswer protocolVersionAnswer = new TransmitterProtocolVersionAnswer(version);
				sendTelegram(protocolVersionAnswer);
				break;
			}
			case DataTelegram.TRANSMITTER_PROTOCOL_VERSION_ANSWER_TYPE: {
				synchronized(_syncSystemTelegramList) {
					_syncSystemTelegramList.add(telegram);
					_syncSystemTelegramList.notifyAll();
				}
				break;
			}
			case DataTelegram.TRANSMITTER_AUTHENTIFICATION_TEXT_REQUEST_TYPE: {
				TransmitterAuthentificationTextRequest authentificationTextRequest = (TransmitterAuthentificationTextRequest)telegram;
				if(_waitForConfiguration) {
					synchronized(_sync) {
						try {
							while(_waitForConfiguration) {
								if(_closed) return;
								_sync.wait(1000);
							}
						}
						catch(InterruptedException ex) {
							ex.printStackTrace();
							return;
						}
					}
				}
				final long remoteTransmitterId = authentificationTextRequest.getTransmitterId();
				_debug.info("Datenverteiler " + remoteTransmitterId + " möchte sich authentifizieren");
				final T_T_HighLevelCommunication transmitterConnection;

				transmitterConnection = _lowLevelConnectionsManager.getTransmitterConnection(remoteTransmitterId);

				if(transmitterConnection != null && transmitterConnection.isAcceptedConnection()) {
					final String message = "Neue Verbindung zum Datenverteiler " + remoteTransmitterId
					                       + " wird terminiert, weil noch eine andere Verbindung zu diesem Datenverteiler besteht.";
					_debug.warning(message);
					terminate(true, message);
					return;
				}
				_connectedTransmitterId = remoteTransmitterId;
				_lowLevelCommunication.setRemoteName("DAV " + _connectedTransmitterId);
				_weight = _transmitterManager.getWeight(_connectedTransmitterId);
				_authentifyAsUser = _transmitterManager.getUserNameForAuthentication(_connectedTransmitterId);
				_authentifyWithPassword = _transmitterManager.getPasswordForAuthentication(_connectedTransmitterId);
				String text = _authentificationComponent.getAuthentificationText(Long.toString(_connectedTransmitterId));
				TransmitterAuthentificationTextAnswer authentificationTextAnswer = new TransmitterAuthentificationTextAnswer(text);
				sendTelegram(authentificationTextAnswer);
				_lowLevelConnectionsManager.updateTransmitterId(this);
				break;
			}
			case DataTelegram.TRANSMITTER_AUTHENTIFICATION_TEXT_ANSWER_TYPE: {
				synchronized(_syncSystemTelegramList) {
					_syncSystemTelegramList.add(telegram);
					_syncSystemTelegramList.notifyAll();
				}
				break;
			}
			case DataTelegram.TRANSMITTER_AUTHENTIFICATION_REQUEST_TYPE: {
				TransmitterAuthentificationRequest authentificationRequest = (TransmitterAuthentificationRequest)telegram;
				String userName = authentificationRequest.getUserName();
				try {
					_remoteUserId = _lowLevelConnectionsManager.login(
							userName,
							authentificationRequest.getUserPassword(),
							_authentificationComponent.getAuthentificationText(Long.toString(_connectedTransmitterId)),
							_authentificationComponent.getAuthentificationProcess(),
							""
					);
					if(_remoteUserId > -1) {
						_debug.info("Datenverteiler " + _connectedTransmitterId + " hat sich als '" + userName + "' erfolgreich authentifiziert");
						TransmitterAuthentificationAnswer authentificationAnswer = new TransmitterAuthentificationAnswer(
								true, _properties.getDataTransmitterId()
						);
						sendTelegram(authentificationAnswer);
						synchronized(_authentificationSync) {
							_authentificationSync.notifyAll();
						}
						if(_isAcceptedConnection) {
							Runnable runnable = new Runnable() {
								@Override
								public void run() {
									try {
										authentify();
									}
									catch(CommunicationError ex) {
										ex.printStackTrace();
									}
								}
							};
							Thread thread = new Thread(runnable);
							thread.start();
						}
					}
					else {
						synchronized(_authentificationSync) {
							_authentificationSync.notifyAll();
						}
						_debug.info("Datenverteiler " + _connectedTransmitterId + " hat vergeblich versucht sich als '" + userName + "' zu authentifizieren");
						TransmitterAuthentificationAnswer authentificationAnswer = new TransmitterAuthentificationAnswer(false, -1);
						sendTelegram(authentificationAnswer);
					}
				}
				catch(ConfigurationException ex) {
					ex.printStackTrace();
					terminate(
							true, "Fehler während der Authentifizierung eines anderen Datenverteilers beim Zugriff auf die Konfiguration: " + ex.getMessage()
					);
					return;
				}
				break;
			}
			case DataTelegram.TRANSMITTER_AUTHENTIFICATION_ANSWER_TYPE: {
				synchronized(_syncSystemTelegramList) {
					_syncSystemTelegramList.add(telegram);
					_syncSystemTelegramList.notifyAll();
				}
				break;
			}
			case DataTelegram.TRANSMITTER_COM_PARAMETER_REQUEST_TYPE: {
				TransmitterComParametersRequest comParametersRequest = (TransmitterComParametersRequest)telegram;
				long keepAliveSendTimeOut = comParametersRequest.getKeepAliveSendTimeOut();
				if(keepAliveSendTimeOut < 5000) keepAliveSendTimeOut = 5000;
				long keepAliveReceiveTimeOut = comParametersRequest.getKeepAliveReceiveTimeOut();
				if(keepAliveReceiveTimeOut < 6000) keepAliveReceiveTimeOut = 6000;

				TransmitterComParametersAnswer comParametersAnswer = null;
//				if(keepAliveSendTimeOut < keepAliveReceiveTimeOut) {
//					long tmp = keepAliveSendTimeOut;
//					keepAliveSendTimeOut = keepAliveReceiveTimeOut;
//					keepAliveReceiveTimeOut = tmp;
//				}
				comParametersAnswer = new TransmitterComParametersAnswer(keepAliveSendTimeOut, keepAliveReceiveTimeOut);
				sendTelegram(comParametersAnswer);
				_lowLevelCommunication.updateKeepAliveParameters(keepAliveSendTimeOut, keepAliveReceiveTimeOut);
				completeInitialisation();
				break;
			}
			case DataTelegram.TRANSMITTER_COM_PARAMETER_ANSWER_TYPE: {
				synchronized(_syncSystemTelegramList) {
					_syncSystemTelegramList.add(telegram);
					_syncSystemTelegramList.notifyAll();
				}
				break;
			}
			case DataTelegram.TRANSMITTER_TELEGRAM_TIME_REQUEST_TYPE: {
				TransmitterTelegramTimeRequest telegramTimeRequest = (TransmitterTelegramTimeRequest)telegram;
				sendTelegram(new TransmitterTelegramTimeAnswer(telegramTimeRequest.getTelegramRequestTime()));
				break;
			}
			case DataTelegram.TRANSMITTER_TELEGRAM_TIME_ANSWER_TYPE: {
				synchronized(_syncSystemTelegramList) {
					_syncSystemTelegramList.add(telegram);
					_syncSystemTelegramList.notifyAll();
				}
				break;
			}
			case DataTelegram.TRANSMITTER_DATA_SUBSCRIPTION_TYPE: {
				if(_initComplete) {
					TransmitterDataSubscription subscription = (TransmitterDataSubscription)telegram;
					_transmitterManager.handleTransmitterSubscription(this, subscription);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_DATA_SUBSCRIPTION_RECEIPT_TYPE: {
				if(_initComplete) {
					TransmitterDataSubscriptionReceipt receipt = (TransmitterDataSubscriptionReceipt)telegram;
					_transmitterManager.handleTransmitterSubscriptionReceipt(this, receipt);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_DATA_UNSUBSCRIPTION_TYPE: {
				if(_initComplete) {
					TransmitterDataUnsubscription unsubscription = (TransmitterDataUnsubscription)telegram;
					_transmitterManager.handleTransmitterUnsubscription(this, unsubscription);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_BEST_WAY_UPDATE_TYPE: {
				if(_initComplete) {
					TransmitterBestWayUpdate transmitterBestWayUpdate = (TransmitterBestWayUpdate)telegram;
					_transmitterManager.updateBestWay(this, transmitterBestWayUpdate);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_LISTS_SUBSCRIPTION_TYPE: {
				if(_initComplete) {
					TransmitterListsSubscription transmitterListsSubscription = (TransmitterListsSubscription)telegram;
					_transmitterManager.handleListsSubscription(this, transmitterListsSubscription);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_LISTS_UNSUBSCRIPTION_TYPE: {
				if(_initComplete) {
					TransmitterListsUnsubscription transmitterListsUnsubscription = (TransmitterListsUnsubscription)telegram;
					_transmitterManager.handleListsUnsubscription(this, transmitterListsUnsubscription);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_LISTS_DELIVERY_UNSUBSCRIPTION_TYPE: {
				if(_initComplete) {
					TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = (TransmitterListsDeliveryUnsubscription)telegram;
					_transmitterManager.handleListsDeliveryUnsubscription(this, transmitterListsDeliveryUnsubscription);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_LISTS_UPDATE_TYPE:
			case DataTelegram.TRANSMITTER_LISTS_UPDATE_2_TYPE: {
				if(_initComplete) {
					TransmitterListsUpdate transmitterListsUpdate = (TransmitterListsUpdate)telegram;
					_transmitterManager.handleListsUpdate(transmitterListsUpdate);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TRANSMITTER_DATA_TELEGRAM_TYPE: {
				if(_initComplete) {
					TransmitterDataTelegram transmitterDataTelegram = (TransmitterDataTelegram)telegram;
						_transmitterManager.handleDataTelegram(this, transmitterDataTelegram);
				}
				else {
					synchronized(_fastTelegramsList) {
						_fastTelegramsList.add(telegram);
					}
				}
				break;
			}
			case DataTelegram.TERMINATE_ORDER_TYPE: {
				TerminateOrderTelegram terminateOrderTelegram = (TerminateOrderTelegram)telegram;
				terminate(true, "Verbindung wurde vom anderen Datenverteiler terminiert. Ursache: " + terminateOrderTelegram.getCause(), null);
			}
			case DataTelegram.CLOSING_TYPE: {
				terminate(false, "Verbindung wurde vom anderen Datenverteiler geschlossen", null);
				break;
			}
			case DataTelegram.TRANSMITTER_KEEP_ALIVE_TYPE: {
				break;
			}
			default: {
				break;
			}
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um die Initialisierung einer Verbindung abzuschließen. Zuerst wird eine Instanz der
	 * Anmeldungsverwaltung für diese Verbindung erzeugt und zur Anmeldeverwaltung hinzugefügt. Danach wird die addWayMethode der Wegverwaltung aufgerufen, um
	 * einen Eintrag für den verbundenen Datenverteiler zu erzeugen. Danach werden die Telegramme bearbeitet, die nicht zum Etablieren dieser Verbindung dienen und
	 * vor Fertigstellung der Initialisierung angekommen sind (Online-Daten, Wegeanmeldungen, Listenanmeldungen usw.).
	 *
	 * @return true: Initialisierung abgeschlossen, false: Initialisierung nicht abgeschlossen
	 *
	 * 
	 */
	public final boolean completeInitialisation() {
		if(!_initComplete) {
				_transmitterManager.addWay(this);

			_initComplete = true;
			synchronized(_fastTelegramsList) {
				int size = _fastTelegramsList.size();
				if(size > 0) {
					for(int i = 0; i < size; ++i) {
						update(_fastTelegramsList.removeFirst());
					}
				}
			}
		}
		return _initComplete;
	}

	/**
	 * Gibt die höchhste unterstützte Version aus den gegebenen Versionen oder -1, wenn keine von den gegebenen Versionen unterstützt wird, zurück.
	 *
	 * @param versions Feld der Versionen
	 *
	 * @return die höchste unterstützte version oder -1
	 */
	private int getPrefferedVersion(int[] versions) {

		if(_versions == null) {
			return -1;
		}
		for(int i = 0; i < versions.length; ++i) {
			for(int j = 0; j < _versions.length; ++j) {
				if(versions[i] == _versions[j]) {
					return versions[i];
				}
			}
		}
		return -1;
	}


	/**
	 * Erledigt den Authentifizierungsprozess.
	 *
	 * TBD: In korrektes Englisch (authenticate o.ä.)  umbenennen?
	 *
	 * @throws de.bsvrz.dav.daf.main.CommunicationError,
	 *          wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind
	 */
	private void authentify() throws CommunicationError {
		// Authentifikationstext holen
		TransmitterAuthentificationTextRequest authentificationTextRequest = new TransmitterAuthentificationTextRequest(
				_properties.getDataTransmitterId()
		);
		sendTelegram(authentificationTextRequest);
		TransmitterAuthentificationTextAnswer authentificationTextAnswer = (TransmitterAuthentificationTextAnswer)waitForAnswerTelegram(
				DataTelegram.TRANSMITTER_AUTHENTIFICATION_TEXT_ANSWER_TYPE, "Aufforderung zur Authentifizierung"
		);
		byte[] encriptedUserPassword = authentificationTextAnswer.getEncryptedPassword(
				_properties.getAuthentificationProcess(), _authentifyWithPassword
		);

		// User Authentifizierung
		String authentificationProcessName = _properties.getAuthentificationProcess().getName();
		TransmitterAuthentificationRequest authentificationRequest = new TransmitterAuthentificationRequest(
				authentificationProcessName, _authentifyAsUser, encriptedUserPassword
		);
		sendTelegram(authentificationRequest);

		TransmitterAuthentificationAnswer authentificationAnswer = (TransmitterAuthentificationAnswer)waitForAnswerTelegram(
				DataTelegram.TRANSMITTER_AUTHENTIFICATION_ANSWER_TYPE, "Antwort auf eine Authentifizierungsanfrage"
		);
		if(!authentificationAnswer.isSuccessfullyAuthentified()) {
			throw new CommunicationError("Die Authentifizierung beim anderen Datenverteiler ist fehlgeschlagen");
		}
		_connectedTransmitterId = authentificationAnswer.getCommunicationTransmitterId();
		_lowLevelCommunication.setRemoteName("DAV " + _connectedTransmitterId);
		_lowLevelConnectionsManager.updateTransmitterId(this);

	}

	@Override
	public String toString() {
		return "[" + _connectedTransmitterId + "]";
	}

	public void subscribeToRemote(RemoteCentralSubscription remoteCentralSubscription) {
//		_remoteSubscriptions.add(remoteCentralSubscription);
		TransmitterSubscriptionType transmitterSubscriptionType = null;
		if(remoteCentralSubscription instanceof RemoteSourceSubscription) {
			// Auf eine Quelle meldet man sich als Empfänger an
			transmitterSubscriptionType = TransmitterSubscriptionType.Receiver;
		}
		else {
			// Auf eine Senke meldet man sich als Sender an
			transmitterSubscriptionType = TransmitterSubscriptionType.Sender;
		}
		TransmitterDataSubscription telegram = new TransmitterDataSubscription(
				remoteCentralSubscription.getBaseSubscriptionInfo(), transmitterSubscriptionType.toByte(), Longs.asArray(remoteCentralSubscription.getPotentialDistributors())
		);
		sendTelegram(telegram);
	}


	public void unsubscribeToRemote(RemoteCentralSubscription remoteCentralSubscription) {
		TransmitterSubscriptionType transmitterSubscriptionType = null;
		if(remoteCentralSubscription instanceof RemoteSourceSubscription) {
			// Auf eine Quelle meldet man sich als Empfänger an
			transmitterSubscriptionType = TransmitterSubscriptionType.Receiver;
		}
		else {
			// Auf eine Senke meldet man sich als Sender an
			transmitterSubscriptionType = TransmitterSubscriptionType.Sender;
		}
		TransmitterDataUnsubscription telegram = new TransmitterDataUnsubscription(
				remoteCentralSubscription.getBaseSubscriptionInfo(), transmitterSubscriptionType.toByte(), Longs.asArray(remoteCentralSubscription.getPotentialDistributors())
		);
		sendTelegram(telegram);
	}

	@Override
	public final void sendData(ApplicationDataTelegram telegram, final boolean toCentralDistributor) {
		TransmitterDataTelegram transmitterDataTelegram = new TransmitterDataTelegram(telegram, toCentralDistributor ? (byte)0 : (byte)1);
		sendTelegram(transmitterDataTelegram);
	}

	@Override
	public void sendReceipt(
			final long centralTransmitterId,
			final ConnectionState state,
			final TransmitterSubscriptionType receiver,
			RemoteSubscription remoteReceiverSubscription) {
		final byte statusByte;
		switch(state) {
			case TO_REMOTE_OK:
				statusByte = TransmitterSubscriptionsConstants.POSITIV_RECEIP;
				break;
			case TO_REMOTE_NOT_RESPONSIBLE:
				statusByte = TransmitterSubscriptionsConstants.NEGATIV_RECEIP;
				break;
			case TO_REMOTE_NOT_ALLOWED:
				statusByte = TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT;
				break;
			case TO_REMOTE_MULTIPLE:
				statusByte = TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP;
				break;
			default:
				throw new IllegalArgumentException("Status: " + state);
		}
		TransmitterDataSubscriptionReceipt receipt = new TransmitterDataSubscriptionReceipt(
				remoteReceiverSubscription.getBaseSubscriptionInfo(),
				receiver.toByte(), statusByte, centralTransmitterId,
				Longs.asArray(remoteReceiverSubscription.getPotentialDistributors())
		);
		sendTelegram(receipt);
	}
}
