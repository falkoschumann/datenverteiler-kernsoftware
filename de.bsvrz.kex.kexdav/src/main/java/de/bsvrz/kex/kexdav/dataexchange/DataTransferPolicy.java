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

import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.kex.kexdav.main.Constants;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;
import de.bsvrz.sys.funclib.concurrent.UnboundedQueue;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Eine abstrakte Klasse, die das Verfahren angibt, mit der Daten zwischen 2 Datenverteilern ausgetauscht werden. Diese Klasse ist nicht für die Kopie des
 * Data-Objektes zuständig, darum kümmert sich das {@link de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin}, mit ihr könnte man stattdessen z.B. verhindern, dass
 * leere Daten übertragen werden oder eine Art Rechteprüfung implementieren. Derzeit wird diese Klasse benutzt, um bei beidseitigen
 * Parameter-Daten-Übertragungen festzustellen, wenn beide Seiten annährend gleichzeitig Parameter senden. Dann wird das lokale System priorisiert und ein
 * unendlichen hin und her-wechseln der Daten verhindert. Siehe dazu {@link ParameterDataTransferPolicy}. Außerdem werden die beiden Datenverteilersysteme über
 * den Threadpool entkoppelt, sodass z.B. ein hängenbleiben im sendData() die Empfangsqueue des anderen Datenverteilers nicht blockiert.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9274 $
 */
public abstract class DataTransferPolicy {

	private final LowLevelDataPipe _lowLevelDataPipe;

	private static final UnboundedQueue<DataAndPipe> _queue = new UnboundedQueue<DataAndPipe>();

	private static volatile long _lastWarnTime = 0;

	private static final Debug _debug = Debug.getLogger();

	static {
		final Thread dataUpdateThread = new Thread(
				new Runnable() {
					public void run() {
						while(true) {
							try {
								final DataAndPipe dataAndPipe = _queue.take();
								final DataPackage dataPackage = dataAndPipe.getDataPackage();
								dataAndPipe.getDataPipe().sendDataToReceiver(
										dataPackage.getData(),
										dataPackage.getDataState(),
										dataPackage.getDataTime()
								);
								final int queueSize = _queue.size();
								if(queueSize > Constants.WarnSendQueueCapacity && System.currentTimeMillis() > _lastWarnTime + Constants.WarnSendQueueInterval){
									_lastWarnTime = System.currentTimeMillis();
									_debug.warning(
											"In der Sende-Warteschlange befinden sich über " + Constants.WarnSendQueueCapacity + " Datensätze (" + queueSize
											+ "). Vermutlich nimmt ein Datenverteiler die Daten nicht schnell genug ab."
									);
								}
							}
							catch(InterruptedException e) {
								throw new IllegalStateException(e);
							}
						}
					}
				}
		);
		dataUpdateThread.setName("Datentransfer");
		dataUpdateThread.setDaemon(true);
		dataUpdateThread.start();
	}

	/**
	 * Konstruktor
	 *
	 * @param lowLevelDataPipe Zugehöriger Datenkanal, in den die Daten eingespeist werden sollen
	 */
	public DataTransferPolicy(final LowLevelDataPipe lowLevelDataPipe) {
		_lowLevelDataPipe = lowLevelDataPipe;
	}

	/**
	 * Wird aufgerufen, wenn Daten eintreffen
	 *
	 * @param sourceData Daten (können null sein)
	 * @param dataState  Daten-Zustand
	 * @param dataTime   Daten-Zeit
	 */
	public final void handleData(final KExDaVAttributeGroupData sourceData, final DataState dataState, final long dataTime) {
		handleData(new DataPackage(sourceData, dataState, dataTime));
	}

	/**
	 * Template-Methode, die die Daten weiterverarbeiten soll
	 *
	 * @param dataPackage Datenpaket
	 */
	protected abstract void handleData(final DataPackage dataPackage);

	/**
	 * Sendet die Daten an das Zielsystem
	 *
	 * @param dataPackage Datenpaket
	 */
	protected final void sendData(final DataPackage dataPackage) {
		_queue.put(new DataAndPipe(dataPackage, _lowLevelDataPipe));
	}

	class DataPackage {

		private final KExDaVAttributeGroupData _data;

		private final DataState _dataState;

		private final long _dataTime;

		private final long _creationTime;

		/**
		 * Klasse, die ein Datenpaket speichert (Ähnlich einem {@link de.bsvrz.dav.daf.main.ResultData})
		 *
		 * @param data      Datum
		 * @param dataState Datenzustand
		 * @param dataTime  Datenzeit
		 */
		public DataPackage(final KExDaVAttributeGroupData data, final DataState dataState, final long dataTime) {
			_data = data;
			_dataState = dataState;
			_dataTime = dataTime;
			_creationTime = System.currentTimeMillis();
		}

		/**
		 * Gibt das Datum zurück
		 *
		 * @return das Datum
		 */
		public KExDaVAttributeGroupData getData() {
			return _data;
		}

		/**
		 * Gibt den Zustand zurück
		 *
		 * @return den Zustand
		 */
		public DataState getDataState() {
			return _dataState;
		}

		/**
		 * Gibt die Datenzeit zurück
		 *
		 * @return die Datenzeit
		 */
		public long getDataTime() {
			return _dataTime;
		}

		/**
		 * Gibt die Zeit der Ankunft des Datums bei KExDaV zurück
		 *
		 * @return die Zeit der Ankunft des Datums bei KExDaV
		 */
		public long getCreationTime() {
			return _creationTime;
		}

		@Override
		public boolean equals(final Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			final DataPackage other = (DataPackage)o;

			if(_data != null ? other._data == null || !_data.toString().equals(other._data.toString()) : other._data != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return _data != null ? _data.toString().hashCode() : 0;
		}

		@Override
		public String toString() {
			return "DataPackage{" + "_data=" + _data + ", _dataState=" + _dataState + ", _dataTime=" + _dataTime + ", _creationTime=" + _creationTime + '}';
		}
	}

	private class DataAndPipe {
		private final DataPackage _dataPackage;
		private final LowLevelDataPipe _dataPipe;

		private DataAndPipe(final DataPackage dataPackage, final LowLevelDataPipe dataPipe) {
			_dataPackage = dataPackage;
			_dataPipe = dataPipe;
		}

		private DataPackage getDataPackage() {
			return _dataPackage;
		}

		private LowLevelDataPipe getDataPipe() {
			return _dataPipe;
		}
	}
}
