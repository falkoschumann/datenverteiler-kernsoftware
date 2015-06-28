/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.main;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.dataplugin.AttributeGroupPair;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.parameterloader.ConnectionParameter;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;
import de.bsvrz.kex.kexdav.util.AdjustableTimer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Verbindung zu einem Remote-Datenverteiler
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12913 $
 */
public class RemoteDaVConnection {

	private RemoteDaVParameter _parameters;

	private final ManagerInterface _manager;

	private final Map<AttributeGroupPair, KExDaVDataPlugin> _plugins;

	private final File _authenticationFile;

	private final ClientDavInterface _localConnection;

	private ClientDavInterface _remoteConnection = null;

	private KExDaVExchange _kExDaVExchange = null;

	private final ConnectionParameter _connectionParameters;

	private volatile AdjustableTimer _reconnectionTimer = null;

	/**
	 * Konstruktor
	 *
	 * @param parameters         Datenverteiler-Parameter
	 * @param authenticationFile Datei mit Passwörtern
	 * @param localConnection    lokale Verbindung
	 * @param manager            KExDaV-Verwaltung
	 * @param plugins            Plugins zum Datenaustausch
	 */
	public RemoteDaVConnection(
			final RemoteDaVParameter parameters,
			final File authenticationFile,
			final ClientDavInterface localConnection,
			final ManagerInterface manager, final Map<AttributeGroupPair, KExDaVDataPlugin> plugins) {
		if(parameters == null) throw new IllegalArgumentException("parameters ist null");
		if(authenticationFile == null) throw new IllegalArgumentException("authenticationFile ist null");
		if(!authenticationFile.exists()) throw new IllegalArgumentException("authenticationFile existiert nicht");
		if(localConnection == null) throw new IllegalArgumentException("localConnection ist null");
		if(manager == null) throw new IllegalArgumentException("manager ist null");
		if(plugins == null) throw new IllegalArgumentException("plugins ist null");
		_parameters = parameters;
		_manager = manager;
		_plugins = plugins;
		_connectionParameters = parameters.getConnectionParameters();
		_authenticationFile = authenticationFile;
		_localConnection = localConnection;
	}

	/**
	 * Versucht eine Verbindung herzustellen und startet den Datenaustausch. Falls keine Verbindung hergestellt werden kann, wird periodisch versucht die Verbindung neu herzustellen.
	 *
	 * @throws MissingAreaException ein benötigter Konfigurationsbereich fehlt
	 */
	public synchronized void start() throws MissingAreaException {
		if(_remoteConnection != null) {
			_manager.addMessage(Message.newInfo("Starte Verbindungsaufbau mit " + _connectionParameters.getDavPid() + ", Verbindung steht aber bereits."));
			return;
		}
		ClientDavInterface remoteConnection = null;
		try {
			_manager.addMessage(Message.newInfo("Starte Verbindungsaufbau mit " + _connectionParameters.getDavPid()));
			final ClientDavParameters parameters = new ClientDavParameters();
			parameters.setApplicationName(Constants.RemoteApplicationName);
			parameters.setDavCommunicationAddress(_connectionParameters.getIP());
			parameters.setDavCommunicationSubAddress(_connectionParameters.getPort());
			parameters.setUserName(_connectionParameters.getUser());
			parameters.setUserPassword(getUserPassword(_connectionParameters.getDavPid(), _connectionParameters.getUser(), _authenticationFile));
			parameters.setConfigurationPath(_localConnection.getClientDavParameters().getConfigurationPath());
			remoteConnection = new ClientDavConnection(parameters);
			remoteConnection.setCloseHandler(
					new ApplicationCloseActionHandler() {
						public void close(final String error) {
							_manager.addMessage(Message.newMajor("Verbindung zu " + _connectionParameters.getDavPid() + " wurde terminiert: " + error));
							stop();
							startReconnectTimer();
						}
					}
			);
			remoteConnection.enableExplicitApplicationReadyMessage();
			remoteConnection.connect();
			_manager.addMessage(Message.newInfo("Verbindung mit " + _connectionParameters.getDavPid() + " hergestellt. Starte Authentifizierung."));
			remoteConnection.login();
			if(!remoteConnection.getLocalDav().getPid().equals(_connectionParameters.getDavPid())) {
				throw new IllegalArgumentException(
						"Der Datenverteiler sollte die Pid '" + _connectionParameters.getDavPid() + "' haben, die Pid war aber '"
						+ remoteConnection.getLocalDav().getPid() + "'."
				);
			}
			remoteConnection.sendApplicationReadyMessage();
			_manager.addMessage(Message.newInfo("Erfolgreich verbunden mit " + _connectionParameters.getDavPid()));
			_manager.addMessage(Message.newInfo("Lade Konfigurationsobjekte"));
			remoteConnection.getDataModel().getType("typ.konfigurationsObjekt").getObjects();
			_manager.addMessage(Message.newInfo("Konfigurationsobjekte geladen"));
		}
		catch(Exception e) {
			if(remoteConnection != null) {
				remoteConnection.setCloseHandler(null);
				remoteConnection.disconnect(true, "Fehler beim Aufbau einer KExDaV-Remote-Verbindung");
			}
			_manager.addMessage(Message.newError("Kann nicht zum Remote-Datenverteiler verbinden: " + _connectionParameters.getDavPid(), e));
			stop();
			startReconnectTimer();
			return;
		}
		_remoteConnection = remoteConnection;
		_kExDaVExchange = new KExDaVExchange(_parameters, _localConnection, _remoteConnection, _manager, _plugins);
		_kExDaVExchange.start();
	}

	/**
	 * Startet den Wiederverbindungs-Timer
	 */
	private void startReconnectTimer() {
		_reconnectionTimer = new AdjustableTimer(
				getReconnectionDelay(_parameters), new Runnable() {
					public void run() {
						try {
							start();
						}
						catch(MissingAreaException e) {
							_manager.addMessage(Message.newError(e));
						}
					}
				}
		);
	}

	/**
	 * Gibt das Passwort für die Dav-authentifizierung zurück
	 * @param davPid Datenverteiler-Pid
	 * @param user Benutzer
	 * @param authFile passwd-datei
	 * @return Passwort
	 * @throws MissingParameterException Falls die passwd kein solches Passwort enthält
	 */
	private static String getUserPassword(final String davPid, final String user, final File authFile) throws MissingParameterException {
		try {
			final String userPassword;
			final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(authFile));
			try {
				final Properties properties = new Properties();
				properties.load(inStream);
				final String key = user + "@" + davPid;
				userPassword = properties.getProperty(key);
				if((userPassword == null) || (userPassword.length() == 0)) {
					throw new MissingParameterException(
							"Das Passwort für den Benutzer " + key + " ist in der Authentifizierungsdatei " + authFile.getAbsolutePath() + " nicht vorhanden"
					);
				}
			}
			catch(IOException ex) {
				throw new MissingParameterException("Spezifizierte Authentifizierungsdatei kann nicht gelesen werden", ex);
			}
			finally {
				inStream.close();
			}
			return userPassword;
		}
		catch(IOException ex) {
			throw new MissingParameterException("Spezifizierte Authentifizierungsdatei kann nicht gelesen werden", ex);
		}
	}

	/** Beendet die Verbindung und stoppt die automatische Verbindungsaufnahme bis zu einem erneuten Aufruf von {@link #start()} */
	public synchronized void stop() {
		_manager.addMessage(Message.newInfo("Stoppe Verbindung mit " + _connectionParameters.getDavPid()));
		if(_reconnectionTimer != null){
			_reconnectionTimer.cancel();
		}
		if(_remoteConnection != null) {
			_kExDaVExchange.stop();
			_kExDaVExchange = null;
			_remoteConnection.disconnect(false, "Terminierung einer KExDaV-Remote-Verbindung");
			_remoteConnection = null;
		}
	}

	/**
	 * Wird aufgerufen, wenn neue Parameter eintreffen
	 *
	 * @param parameter Parameter
	 *
	 * @throws MissingAreaException ein benötigter Konfigurationsbereich fehlt
	 */
	public synchronized void setNewParameter(final RemoteDaVParameter parameter) throws MissingAreaException {
		if(_kExDaVExchange != null){
			_kExDaVExchange.setParameter(parameter);
		}
		else {
			final AdjustableTimer reconnectionTimer = _reconnectionTimer;
			if(reconnectionTimer != null) {
				reconnectionTimer.adjustDelay(getReconnectionDelay(parameter));
			}
		}
		_parameters = parameter;
	}

	/**
	 * Gibt die Wiederverbindungs-Wartezeit zurück
	 * @return Wartezeit
	 * @param parameters
	 */
	private synchronized long getReconnectionDelay(final RemoteDaVParameter parameters) {
		long ms = parameters.getReconnectionDelay();
		if(ms < 1000) ms = 1000;
		return ms;
	}

	/**
	 * Löst den einmaligen Austausch von Parameterdaten aus
	 *
	 * @param direction Richtung
	 */
	public synchronized void triggerParameterExchange(final Direction direction) {
		if(_kExDaVExchange != null) {
			_kExDaVExchange.triggerParameterExchange(direction);
		}
	}

	/**
	 * Gibt zurück, ob die Verbindung hergestellt ist
	 * @return True wenn die Verbindung hergestellt ist
	 */
	public synchronized boolean isConnected() {
		return _remoteConnection != null;
	}

	@Override
	public synchronized String toString() {
		return "RemoteDaVConnection{" + "_connectionParameters=" + _connectionParameters + ", _kExDaVExchange=" + _kExDaVExchange + '}';
	}
}
