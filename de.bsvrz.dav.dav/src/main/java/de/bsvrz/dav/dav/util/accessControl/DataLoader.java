/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstrakte Klasse, die allgemeine Funktionen bietet (Parameter-)Daten eines Systemobjekts zu laden, auf dessen Initialisierung zu warten usw.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public abstract class DataLoader {

	/** Empfänger */
	private final ClientReceiverInterface _receiver;

	/** Datenbeschreibung */
	private DataDescription _dataDescription = null;

	/** Gekapseltes Systemobjekt */
	private SystemObject _systemObject = null;

	/** Dav-Verbindung */
	private final ClientDavInterface _connection;

	/** Attributgruppen-Pid */
	private final String _attributeGroupPid;

	/** Aspekt-Pid */
	private final String _aspectPid;

	/** Debug */
	protected final Debug _debug = Debug.getLogger();

	/** Ob auf das Eintreffen von Daten gewartet werden soll, oder ob auch das Eintreffen von "keine Daten" oder "keine Quelle" ausreichen soll, mit dem Initialisieren aufzuhören */
	private volatile boolean _waitForData = true; // Bein Anlegen des Objektes zunächst auf Daten warten

	/** Ob der Receiver bereits etwas empfangen hat (auch true wenn "keine Daten" oder "keine Quelle") */
	private volatile boolean _isInitialized = false;

	/** Ob auch wirklich Daten da sind (nicht true wenn "keine Daten" oder "keine Quelle") */
	private volatile boolean _hasData = false;

	/** Objekt, das für notifyAll() benutzt wird um das Eintreffen von Daten zu signalisieren */
	private final Object _updateNotifier = new Object();

	/** Wie lange auf Daten gewartet wird (in ms) */
	private static final int TIMEOUT = 1000 * 15;

	private final ReadWriteLock _readWriteLock = new ReentrantReadWriteLock();

	/**
	 * Lock, das zum Lesen von Daten benutzt wird. Um Deadlocks zu verhindern und gleichzeitige Abfragen aus Performancegründen zu ermöglichen
	 * sollte dieses ReadLock bei allen lesenden Zugriffen verwendet werden. (Durch das Update einer Rolle/Region wird enumerateChildren bei anderen DataLoadern ausgeführt, wenn nebenläufig noch eine Abfrage nach Benutzerrechten läuft und und beide Threads ein exklusives Lock verwenden, gibt es Probleme. Da der Thread, in dem das Update durchgeführt wird, zwingend eine exklusives Lock (ein WriteLock) braucht, müssen alle Abfragen nach Benutzerrechten nicht exklusiv sein (ReadLock). Da immer nur ein Objekt gleichzeitig geupdatet wird, kommt es nicht zu Deadlocks durch mehrere WriteLocks)
	 */
	protected final Lock _readLock = _readWriteLock.readLock();
	protected final Lock _writeLock = _readWriteLock.writeLock();

	private volatile long _noDataTime = System.currentTimeMillis();

	private volatile DataState _dataState = null;

	/**
	 * Erstellt ein neues Objekt, für das Daten aktualisiert werden sollen.
	 *
	 * @param connection        Verbindung zum Datenverteiler
	 * @param attributeGroupPid Attributgruppe
	 * @param aspectPid         Aspekt
	 * @param lock
	 */
	public DataLoader(
			final ClientDavInterface connection, final String attributeGroupPid, final String aspectPid, final Object lock) {
		_connection = connection;
		_attributeGroupPid = attributeGroupPid;
		_aspectPid = aspectPid;
		_receiver = new ClientReceiverInterface() {
			@Override
			public final void update(final ResultData[] results) {
				boolean hasData = false;
				synchronized(lock) {
					if(results != null) {
						for(final ResultData result : results) {
							if(result != null) {
								if(result.hasData() && result.getData() != null) {
									DataLoader.this.update(result.getData());
									hasData = true;
								}
								else {
									DataLoader.this.update(null);
									_noDataTime = System.currentTimeMillis();
									hasData = false;
								}
								_dataState = result.getDataState();
							}
						}
					}
				}
				synchronized(_updateNotifier) {
					_hasData = hasData;
					_isInitialized = true;
					_updateNotifier.notifyAll();
				}
			}
		};
	}

	/**
	 * Wird aufgerufen, wenn neue Daten eingetroffen sind. Implementierende Klassen sollten hier das Data-Objekt verarbeiten.
	 *
	 * @param data Data-Objekt entsprechend Attributgruppe und Aspekt
	 */
	protected abstract void update(final Data data);

	/**
	 * Gibt die untergeordneten Objekte zurück. Z.B. die Rollen und Regionen bei der Berechtigungsklasse oder die Berechtigungsklassen beim Benutzer. Wird
	 * gebraucht um Rekursionen zu erkennen und um den {@link de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager} über geänderte Benutzerrechte zu informieren. Achtung: Es
	 * werden nur die direkten Kinder zurückzugeben, nicht die "Enkel" usw. - Will man alle "Enkel" usw. haben muss man diese Funktion rekursiv aufrufen.<br>
	 * Hinweis: Mit {@link #deactivateInvalidChild(DataLoader)} deaktivierte Kindelemente werden nicht aufgeführt.
	 *
	 * @return Liste mit untergeordneten Objekten
	 */
	protected abstract Collection<DataLoader> getChildObjects();

	/**
	 * Startet das Aktualisieren der Daten über das ClientReceiverInterface. Wartet bis die Daten geladen wurden. Nachdem diese Methode aufgerufen wurde, sollte
	 * das Objekt also initialisiert sein. Bei jedem Eintreffen von Daten wird die {@link #update(de.bsvrz.dav.daf.main.Data)}-Methode aufgerufen (auch nach dem
	 * ersten Aufruf der Methode). Um das Laden der Daten anzuhalten ist {@link #stopDataListener()} aufzurufen.
	 *
	 * @param systemObject Objekt für das die Daten geholt werden sollen
	 */
	protected final void startDataListener(final SystemObject systemObject) {
		_systemObject = systemObject;
		final DataModel dataModel = _systemObject.getDataModel();
		final AttributeGroup attributeGroup = (AttributeGroup)dataModel.getObject(_attributeGroupPid);
		if(attributeGroup == null) {
			throw new IllegalArgumentException("Keine gültige Attributgruppe: " + _attributeGroupPid);
		}
		final Aspect aspect = (Aspect)dataModel.getObject(_aspectPid);
		if(aspect == null) {
			throw new IllegalArgumentException("Keine gültiger Aspekt: " + _aspectPid);
		}
		_dataDescription = new DataDescription(attributeGroup, aspect);
		_connection.subscribeReceiver(
				_receiver, _systemObject, _dataDescription, ReceiveOptions.normal(), ReceiverRole.receiver()
		);
	}

	/** Beendet das Aktualisieren der Daten über das ClientReceiverInterface */
	public void stopDataListener() {
		_connection.unsubscribeReceiver(
				_receiver, _systemObject, _dataDescription
		);
	}

	/**
	 * Wenn es ein Problem mit der Rekursion gibt, wird dieses Objekt hiermit angewiesen den Verweis auf das angegebene (Unter-)Objekt zu deaktivieren.
	 * Beispielsweise könnte eine Rolle angewiesen werden, eine innere Rolle zu deaktivieren, weil sie identisch mit der eigentlichen Rolle ist.
	 *
	 * @param node Das zu entfernende Kindobjekt
	 */
	protected abstract void deactivateInvalidChild(final DataLoader node);

	@Override
	public String toString() {
		if(_systemObject == null) return this.getClass().getSimpleName();
		return _systemObject.getPidOrId();
	}

	/**
	 * Bietet auf Wunsch eine ausführlichere String-Darstellung des Objekts (Allerdings auf Kosten der Verarbeitungszeit)
	 * @param verbose ausführlichere Darstellung wenn true
	 * @return mehrzeiliger String
	 */
	public String toString(final boolean verbose) {
		return toString(verbose, 0);
	}

	/**
	 * Bietet auf Wunsch eine ausführlichere String-Darstellung des Objekts (Allerdings auf Kosten der Verarbeitungszeit)
	 * @param verbose ausführlichere Darstellung wenn true
	 * @param depth Tiefe für Einrückung zur Darstellung
	 * @return mehrzeiliger String
	 */
	String toString(final boolean verbose, final int depth) {
		if(!verbose) return toString();
		final StringBuilder builder = new StringBuilder();

		for(int i = 0 ; i < depth ; i++)
			builder.append("   ");
		builder.append(toString());
		builder.append('\n');
		for(final DataLoader dataLoader : getChildObjects()) {
			builder.append(dataLoader.toString(true, depth+1));
		}
		return builder.toString();
	}

	/**
	 * Gibt die Verbindung zum Datenverteiler zurück
	 *
	 * @return die Verbindung zum Datenverteiler
	 */
	ClientDavInterface getConnection() {
		return _connection;
	}

	/**
	 * Prüft, ob dieses DataLoader-Objekt mit dem Laden der Daten fertig ist
	 *
	 * @return true wenn die Daten geladen wurden, d.h. die Update-Methode mindestens einmal aufgerufen wurde.
	 */
	public boolean isInitialized() {
		synchronized(_updateNotifier) {
			return _isInitialized  && (!_waitForData || _hasData);
		}
	}

	/**
	 * Gibt zurück, wie lange keine Daten eingetroffen sind
	 * @return Zeit seit der keine Daten da sind oder -1 falls Daten da sind
	 */
	public long getNoDataTime(){
		if(_hasData) return -1;
		return System.currentTimeMillis() - _noDataTime;
	}

	/**
	 * Gibt den aktuellen Zustand zurück
	 * @return Zustand (null falls noch keine Meldung von Datenverteiler gekommen ist)
	 */
	public DataState getDataState(){
		return _dataState;
	}

	/**
	 * Gibt das Systemobjekt zurück
	 * @return Systemobjekt
	 */
	public SystemObject getSystemObject() {
		return _systemObject;
	}

	/**
	 * Wartet bis dieses Objekt mit dem Laden fertig ist, aber maximal die in {@link #TIMEOUT} angegebene Zeit. Bei Anfragen an dieses Objekt, sollte zu erst diese
	 * Funktion aufgerufen werden um sicherzustellen, das das Objekt bereits Daten erhalten hat.
	 */
	public void waitForInitialization() {
		synchronized(_updateNotifier) {
			final long startTime = System.currentTimeMillis();
			while(!isInitialized()) {
				try {
					_updateNotifier.wait(1000);
				}
				catch(InterruptedException ignored) {
				}
				if(isInitialized()) return;
				if(System.currentTimeMillis() - startTime > TIMEOUT) {
					_debug.warning("Konnte keine Parameter für " + toString() + " laden. Es werden keine Berechtigungen zugeteilt.");
					_waitForData = false;
					return;
				}
			}
		}
	}


	/**
	 * Wartet bis dieses Objekt und alle Kindobjekte mit dem Laden fertig sind, aber pro Objekt maximal die in {@link #TIMEOUT} angegebene Zeit.
	 */
	public void waitForInitializationTree() {
		waitForInitialization();
		for(DataLoader dataLoader: getChildObjects()) {
			dataLoader.waitForInitializationTree();
		}
	}

	/**
	 * Wird für Tests usw. gebraucht um dem Objekt zu sagen, dass es nicht initialisiert ist, also bei der nächsten Anfrage darauf warten soll, bis neue Daten
	 * eintreffen
	 */
	void invalidate() {
		synchronized(_updateNotifier) {
			_isInitialized = false; // signalisieren, dass neue Daten geladen werden sollen
			_waitForData = true;
			_hasData = false;
		}
	}
}
