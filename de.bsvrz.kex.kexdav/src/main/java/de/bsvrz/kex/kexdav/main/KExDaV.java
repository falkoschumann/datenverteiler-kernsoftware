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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.DavConnectionListener;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.dataplugin.AttributeGroupPair;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.management.DebugObserver;
import de.bsvrz.kex.kexdav.management.KExDaVManager;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.management.MessageSenderObserver;
import de.bsvrz.kex.kexdav.parameterloader.ConnectionParameter;
import de.bsvrz.kex.kexdav.parameterloader.ParameterLoader;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.File;
import java.util.*;

/**
 * Hauptklasse KExDaV
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12677 $
 */
public class KExDaV {

	private static final Debug _debug = Debug.getLogger();

	private final ClientDavInterface _localConnection;

	private final SystemObject _kexDavObject;

	private final File _authenticationFile;

	private ParameterLoader _parameterLoader = null;

	private final Map<ConnectionParameter, RemoteDaVConnection> _connectionMap = new HashMap<ConnectionParameter, RemoteDaVConnection>();

	private final KExDaVManager _manager = new KExDaVManager(this);

	private final Map<AttributeGroupPair, KExDaVDataPlugin> _plugins;

	private boolean _terminateOnError = true;

	/**
	 * Konstruktor für das Haupt-KExDaV-Objekt
	 *
	 * @param connection         Lokale Verbindung
	 * @param kexDavObject       KExDaV-Objekt
	 * @param authenticationFile Datei mit Passwörtern
	 * @param plugins            Plugins zum Datenaustausch
	 */
	public KExDaV(
			final ClientDavInterface connection,
			final SystemObject kexDavObject,
			final File authenticationFile,
			final Map<AttributeGroupPair, KExDaVDataPlugin> plugins) {
		if(connection == null) throw new IllegalArgumentException("connection ist null");
		if(kexDavObject == null) throw new IllegalArgumentException("kexDavObject ist null");
		if(authenticationFile == null) throw new IllegalArgumentException("authenticationFile ist null");
		if(plugins == null) throw new IllegalArgumentException("plugins ist null");

		createExceptionHandler();

		_plugins = plugins;
		_localConnection = connection;

		_localConnection.addConnectionListener(
				new DavConnectionListener() {
					public void connectionClosed(final ClientDavInterface clientDavInterface) {
						stop();
					}
				}
		);

		_kexDavObject = kexDavObject;
		_authenticationFile = authenticationFile;
		_manager.addObserver(new DebugObserver());
		_manager.addObserver(new MessageSenderObserver());
	}

	/** Erstellt einen Exceptionhandler, der bei einem unbehandelten Fehler KExDaV beendet und eien Betriebsmeldung verschickt. */
	private void createExceptionHandler() {
		final Thread.UncaughtExceptionHandler oldExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(oldExceptionHandler));
	}

	/** Startet das Laden der Parameter und damit den Datenaustausch */
	public synchronized void start() {
		if(_parameterLoader == null) {
			_parameterLoader = new ParameterLoader(_localConnection, _kexDavObject, _manager, this);
		}
	}

	/** Beendet das Laden der Parameter und bricht alle Verbindungen ab */
	public synchronized void stop() {
		if(_parameterLoader != null) {
			_parameterLoader.stopDataListener();
			_parameterLoader = null;
		}
		try {
			setNewParameters(Collections.<RemoteDaVParameter>emptyList());
		}
		catch(MissingAreaException e) {
			// Darf nicht auftreten. Wenn keine Parameter da sind, kann auch kein KB fehlen...
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Setzt die Parameter und startet damit den Austausch der Daten, Mengen und Objekte
	 *
	 * @param remoteDaVs Parameterspezifikation der Remote-Datenverteiler-Verbindungen samt auszutauschender Daten
	 *
	 * @throws MissingAreaException falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	public synchronized void setNewParameters(final Collection<RemoteDaVParameter> remoteDaVs) throws MissingAreaException {
		final Collection<ConnectionParameter> connectionParameters = new HashSet<ConnectionParameter>(remoteDaVs.size());
		for(final RemoteDaVParameter remoteDav : remoteDaVs) {
			connectionParameters.add(remoteDav.getConnectionParameters());
		}

		// Ungültige Verbindungen entfernen
		removeOldConnections(connectionParameters);

		// Neue Verbindungen aufbauen und ggf. vorhandenen neue Parameter mitteilen
		addNewConnectionsAndRefreshParameters(remoteDaVs);
	}

	/**
	 * Entfernt Verbindungen, die nicht mehr bestehen sollen
	 * @param remoteDaVs Datenverteiler-Verbindungs-Parameter der Verbindungen, die stehen gelassen werden sollen
	 */
	private void removeOldConnections(final Collection<ConnectionParameter> remoteDaVs) {
		for(Iterator<Map.Entry<ConnectionParameter, RemoteDaVConnection>> iterator = _connectionMap.entrySet().iterator(); iterator.hasNext(); ) {
			final Map.Entry<ConnectionParameter, RemoteDaVConnection> entry = iterator.next();
			if(!remoteDaVs.contains(entry.getKey())) {
				entry.getValue().stop();
				iterator.remove();
			}
		}
	}

	/**
	 * Fügt neue Verbindungen hinzu und aktualisiert beide den Vorhandenen die Parameter
	 * @param remoteDaVs neue Parameter
	 * @throws MissingAreaException Falls ein Konfigurationsbereich fehlt
	 */
	private void addNewConnectionsAndRefreshParameters(final Iterable<RemoteDaVParameter> remoteDaVs) throws MissingAreaException {
		for(final RemoteDaVParameter dav : remoteDaVs) {
			try{
				final RemoteDaVConnection existingConnection = _connectionMap.get(dav.getConnectionParameters());
				if(existingConnection == null) {
					final RemoteDaVConnection remoteDaVConnection = new RemoteDaVConnection(
							dav, _authenticationFile, _localConnection, _manager, _plugins
					);
					_connectionMap.put(dav.getConnectionParameters(), remoteDaVConnection);
					remoteDaVConnection.start();
				}
				else {
					existingConnection.setNewParameter(dav);
				}
			}
			catch(RuntimeException e){
				_manager.addMessage(Message.newInfo("Fehler beim Initialisieren der Verbindung mit dem Remote-System", e));
			}
		}
	}

	/**
	 * Löst den Parameteraustausch per Trigger aus
	 *
	 * @param specification Spezifikation eines Remote-Datenverteilers
	 * @param direction     Richtung
	 */
	public synchronized void triggerParameterExchange(final KExDaVSpecification specification, final Direction direction) {
		for(final Map.Entry<ConnectionParameter, RemoteDaVConnection> entry : _connectionMap.entrySet()) {
			if(specification.matches(entry.getKey())) {
				entry.getValue().triggerParameterExchange(direction);
			}
		}
	}

	@Override
	public String toString() {
		return "KExDaV{" + "_kexDavObject=" + _kexDavObject + '}';
	}

	/**
	 * Setzt, ob KExDav bei einem schweren Fehler beendet werden soll (Standardmäßig aktiviert)
	 *
	 * @param terminateOnError true wenn KExDaV beendet werden soll
	 */
	public void setTerminateOnError(final boolean terminateOnError) {
		_terminateOnError = terminateOnError;
	}

	/**
	 * Beendet KExDaV
	 */
	public void terminate() {
		if(_terminateOnError) {
			_manager.addMessage(Message.newError("KExDaV wird wegen eines schweren Fehlers beendet."));
			System.exit(1);
		}
		else {
			stop();
		}
	}

	/**
	 * Gibt das verwendete KExDaV-Objekt zurück
	 * @return KExDaV-Objekt
	 */
	public SystemObject getKExDaVObject() {
		return _kexDavObject;
	}

	private class ExceptionHandler implements Thread.UncaughtExceptionHandler {

		private final Thread.UncaughtExceptionHandler _oldExceptionHandler;

		/**
		 * Erstellt einen ExceptionHandler
		 * @param oldExceptionHandler alter Exceptionhandler
		 */
		public ExceptionHandler(final Thread.UncaughtExceptionHandler oldExceptionHandler) {
			_oldExceptionHandler = oldExceptionHandler;
		}

		public void uncaughtException(final Thread t, final Throwable e) {
			try {
				_manager.addMessage(Message.newError("Unbehandelter Fehler in " + t.getName(), e));
				terminate();
			}
			finally {
				if(_oldExceptionHandler != null){
					_oldExceptionHandler.uncaughtException(t, e);
				}
				else{
					_debug.error("Unbehandelter Fehler in " + t.getName(), e);
				}
			}
		}

		@Override
		public String toString() {
			return "ExceptionHandler{}";
		}
	}
}
