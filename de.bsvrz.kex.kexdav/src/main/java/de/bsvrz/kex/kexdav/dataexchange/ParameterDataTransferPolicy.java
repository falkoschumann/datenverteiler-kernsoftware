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

package de.bsvrz.kex.kexdav.dataexchange;

import de.bsvrz.kex.kexdav.main.Constants;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Diese Klasse wird  benutzt, um bei beidseitigen Parameter-Daten-Übetragungen festzustellen, wenn beide Seiten annährend gleichzeitig Parameter senden. Dann
 * wird das lokale System priorisiert und ein unendlichen hin und her-wechseln der Daten verhindert.
 * <p/>
 * Funktionsweise der Klasse: Es gibt 2 innere {@link DataTransferPolicy}-Klassen, die jeweils für den Datenverkehr in eine Richtung zuständig sind. Falls Daten
 * eintreffen, wird über einen ThreadPool der Task {@link #_task} gestartet, der sich um die eingetroffenen Daten kümmert.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9274 $
 */
@SuppressWarnings({"ObjectEquality"})
public final class ParameterDataTransferPolicy {

	/** Die Transfer-Klasse für die Verbindung Lokal nach Remote */
	private final Policy _localRemotePolicy;

	/** Die Transfer-Klasse für Remote nach Lokal */
	private final Policy _remoteLocalPolicy;

	/** Der Task, der die Daten weiterleitet und dabei verhindert, dass in beide Richtungen annährend gleichzeitg Daten übertragen werden. */
	private final Runnable _task = new Runnable() {

		private long _lastSendTime = System.currentTimeMillis();

		private Policy _mainPolicy = null;

		public void run() {
			try {
				// Auf this synchronisieren: Den Task immer nur einmal gleichzeitig ausführen.
				// Falls der task mehrmals ausgeführt wird, müssen die weiteren Threads warten.
				// Da nach dem Warten remoteData und localData fast immer null sind (die Daten wurden ja schon verarbeitet),
				// wird nichts weiter dadurch passieren.
				synchronized(this) {

					// Aktuelle Daten holen
					DataTransferPolicy.DataPackage localData = _localRemotePolicy.retrieveData();
					DataTransferPolicy.DataPackage remoteData = _remoteLocalPolicy.retrieveData();

					// Zuerst auf Daten aus dem Lokalsystem prüfen (dieses bevorzugen)
					if(localData != null) {

						// Falls zuletzt aus dem Remote-System gesendet wurde, muss gegebenenfalls gewartet werden,
						// damit nicht kurz hintereinander aus beiden Systemen gesendet wird. (Das ist ja der Sinn der Klasse.)
						if(_mainPolicy == _remoteLocalPolicy) {
							sleepIfNeeded();
							// Evtl. sind nach der Wartezeit schon neue Daten da. Dann einfach die neuen Daten nehmen und die alten verwerfen.
							final DataTransferPolicy.DataPackage newData = _localRemotePolicy.retrieveData();

							// neue Daten aus dem Remote-System sind jetzt erstmal egal, wir wollen dem Remote-system ja jetzt neue Parameter schicken
							_remoteLocalPolicy.setData(null);
							
							localData = newData != null ? newData : localData;
						}

						// Daten senden und speichern, dass zuletzt vom Lokalsystem gesendet wurde
						_localRemotePolicy.sendData(localData);
						_lastSendTime = System.currentTimeMillis();
						_mainPolicy = _localRemotePolicy;
					}
					else if(remoteData != null) {
						if(_mainPolicy == _localRemotePolicy) {
							sleepIfNeeded();
							// Evtl. sind nach der Wartezeit neue Daten da. Dann einfach die nehmen.
							final DataTransferPolicy.DataPackage newData = _remoteLocalPolicy.retrieveData();
							localData = _localRemotePolicy.retrieveData();
							// Neue lokale Daten werden bevorzugt behandelt.
							if(localData != null) {
								// Daten senden.
								// Speichern, dass zuletzt vom Lokalsystem gesendet wurde, ist nicht nötig weil _mainPolicy ist schon == _localRemotePolicy
								_localRemotePolicy.sendData(localData);
								_lastSendTime = System.currentTimeMillis();
								return;
							}
							// Ansonsten mit den Remote-Daten weitermachen
							remoteData = newData != null ? newData : remoteData;
						}
						// Daten senden und speichern, dass zuletzt vom Remotesystem gesendet wurde
						_remoteLocalPolicy.sendData(remoteData);
						_lastSendTime = System.currentTimeMillis();
						_mainPolicy = _remoteLocalPolicy;
					}
				}
			}
			catch(InterruptedException ignored) {
			}
		}

		private void sleepIfNeeded() throws InterruptedException {
			final long sleepTime = _lastSendTime + Constants.ParameterExchangeReverseDelay - System.currentTimeMillis();
			if(sleepTime > 0) {
				Thread.sleep(sleepTime);
			}
		}
	};

	private DataTransferPolicy.DataPackage _currentData = null;


	static final ThreadPoolExecutor _threadPool = new ThreadPoolExecutor(
			0,
			Integer.MAX_VALUE,
			60L,
			TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(),
			new ThreadFactory() {

				private final ThreadFactory _threadFactory = Executors.defaultThreadFactory();

				public Thread newThread(final Runnable r) {
					final Thread thread = _threadFactory.newThread(r);
					thread.setName("Parameterupdate (" + thread.getName() + ")");
					thread.setDaemon(true);
					return thread;
				}
			}
	);

	/**
	 * Konstruktor
	 * @param localRemoteDataPipe Datenleitung Lokal nach Remote
	 * @param remoteLocalDataPipe Datenleitung Remote nach Lokal
	 */
	public ParameterDataTransferPolicy(final LowLevelDataPipe localRemoteDataPipe, final LowLevelDataPipe remoteLocalDataPipe) {
		_localRemotePolicy = new Policy(localRemoteDataPipe);
		_remoteLocalPolicy = new Policy(remoteLocalDataPipe);
	}

	/**
	 * Gibt das Verbindungsverfahren für die Lokal-Remote-Datenleitung zurück
	 * @return Verbindungsverfahren, das sicherstellt, dass es zu keinem wechselseitigen Parameteraustausch kommt.
	 */
	public DataTransferPolicy getLocalRemotePolicy() {
		return _localRemotePolicy;
	}

	/**
	 * Gibt das Verbindungsverfahren für die Remote-Lokal-Datenleitung zurück
	 * @return Verbindungsverfahren, das sicherstellt, dass es zu keinem wechselseitigen Parameteraustausch kommt.
	 */
	public DataTransferPolicy getRemoteLocalPolicy() {
		return _remoteLocalPolicy;
	}

	class Policy extends DataTransferPolicy {

		private DataPackage _data = null;

		public Policy(final LowLevelDataPipe lowLevelDataPipe) {
			super(lowLevelDataPipe);
		}

		@Override
		protected void handleData(final DataPackage dataPackage) {
			setData(dataPackage);
		}

		public synchronized DataPackage retrieveData() {
			final DataPackage data = _data;
			_data = null;
			return data;
		}

		public synchronized void setData(final DataPackage data) {
			if(_currentData != null && _currentData.equals(data)) return;
			_data = data;
			if(data != null) {
				_currentData = data;
				if(!_threadPool.getQueue().contains(_task)){
					_threadPool.execute(_task);
				}
			}
		}
	}
}
