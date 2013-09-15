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
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.kex.kexdav.correspondingObjects.ObjectManagerInterface;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;

/**
 * Empfängt Daten von einem Objekt in einem Datenverteiler und sendet diese an das gleiche oder ein anderes Objekt (vorzugsweise auf einem anderen
 * Datenverteiler), evtl. unter Benutzung einer anderen Attributgruppe und eines anderen Aspekts sowie unter Zuhilfenahme eines Plugins, das die Daten
 * gegebenenfalls anpasst, falls z.B. unterschiedliche Attributgruppen vorliegen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9294 $
 */
public class LowLevelDataPipe {

	private final KExDaVObject _source;

	private final KExDaVObject _target;

	private final String _atgSource;

	private final String _atgTarget;

	private final String _aspSource;

	private final String _aspTarget;

	private final short _simulationVariantSource;

	private final short _simulationVariantTarget;

	private final ReceiveOptions _receiveOptions;

	private final KExDaVDataPlugin _plugin;

	private volatile boolean _hasSender = false;

	private volatile boolean _hasReceiver = false;

	private final KExDaVReceiver _receiver = new MyReceiver();

	private final ReceiverRole _receiverRole;

	private final SenderRole _senderRole;

	private final ObjectManagerInterface _objectManagerInterface;

	private final ManagerInterface _manager;

	private volatile boolean _stopOnNextData = false;

	private DataTransferPolicy _policy;

	/**
	 * Erstellt eine Datenverbindungsklasse
	 *
	 * @param source                  Quell-Objekt
	 * @param target                  Ziel-Objekt
	 * @param atgSource               Quell-Atg
	 * @param atgTarget               Ziel-Atg
	 * @param aspSource               Quell-Aspekt
	 * @param aspTarget               Ziel-Aspekt
	 * @param simulationVariantSource Quell-Simulationsvariante
	 * @param simulationVariantTarget Ziel-Simulationsvariante
	 * @param receiveOptions          Nur geänderte Daten übertragen?
	 * @param receiverRole            Art der Anmeldung im Quellsystem
	 * @param senderRole              Art der Anmeldung im Zielsystem
	 * @param plugin                  Modul, das das Kopieren und gegebenenfalls anpassen der Daten übernimmt. Im einfachsten Fall eine Instanz des {@link
	 *                                de.bsvrz.kex.kexdav.dataplugin.BasicKExDaVDataPlugin}.
	 * @param objectManagerInterface  Verwaltung korrespondierender Objekte (optional)
	 * @param manager                 Callback für Ereignisse und Warnungen
	 */
	public LowLevelDataPipe(
			final KExDaVObject source,
			final KExDaVObject target,
			final String atgSource,
			final String atgTarget,
			final String aspSource,
			final String aspTarget,
			final short simulationVariantSource,
			final short simulationVariantTarget,
			final ReceiveOptions receiveOptions,
			final ReceiverRole receiverRole,
			final SenderRole senderRole,
			final KExDaVDataPlugin plugin,
			final ObjectManagerInterface objectManagerInterface,
			final ManagerInterface manager) {
		if(source == null) throw new IllegalArgumentException("source ist null");
		if(target == null) throw new IllegalArgumentException("target ist null");
		if(atgSource == null) throw new IllegalArgumentException("atgSource ist null");
		if(atgTarget == null) throw new IllegalArgumentException("atgTarget ist null");
		if(aspSource == null) throw new IllegalArgumentException("aspSource ist null");
		if(aspTarget == null) throw new IllegalArgumentException("aspTarget ist null");
		if(plugin == null) throw new IllegalArgumentException("plugin ist null");
		if(manager == null) throw new IllegalArgumentException("manager ist null");
		_manager = manager;
		_source = source;
		_target = target;
		_atgSource = atgSource;
		_atgTarget = atgTarget;
		_aspSource = aspSource;
		_aspTarget = aspTarget;
		_simulationVariantSource = simulationVariantSource;
		_simulationVariantTarget = simulationVariantTarget;
		_receiveOptions = receiveOptions;
		_receiverRole = receiverRole;
		_senderRole = senderRole;
		_plugin = plugin;
		_objectManagerInterface = objectManagerInterface;
		_policy = new BasicTransferPolicy(this);
	}

	/**
	 * Setzt das Austauschverfahren (sinnvollerweise zu setzen, bevor der Datenaustausch gestartet wird)
	 *
	 * @param policy Austauschverfahren
	 */
	public void setPolicy(final DataTransferPolicy policy) {
		_policy = policy;
	}

	/** Startet den Datentransfer */
	public void start() {
		if(_hasReceiver) return;
		_manager.addMessage(Message.newInfo("Starte Empfänger: " + this));
		_hasReceiver = true;
		try {
			
			try {
				Thread.sleep(10);
			}
			catch(InterruptedException ignored) {
			}
			if(!_source.registerReceiver(_atgSource, _aspSource, _simulationVariantSource, _receiverRole, _receiveOptions, _receiver)){
				_hasReceiver = false;
			}
		}
		catch(MissingObjectException e) {
			// Senderobjekt (oder atg/asp) existiert nicht
			_manager.addMessage(Message.newError(e));
			_hasReceiver = false;
		}
	}

	/** Stoppt den Datentransfer */
	public void stop() {
		_manager.addMessage(Message.newInfo("Stoppe Datenaustausch: " + this));
		
		try {
			Thread.sleep(10);
		}
		catch(InterruptedException ignored) {
		}
		if(_hasReceiver) {
			_source.unsubscribeReceiver(_receiver);
			_hasReceiver = false;
		}
		if(_hasSender) {
			_target.unsubscribeSender(this);
			_hasSender = false;
		}
	}

	/** Führt nur einen Datenaustausch durch */
	public void startOneTime() {
		_stopOnNextData = true;
		start();
	}

	private class MyReceiver implements KExDaVReceiver {

		public void update(final KExDaVAttributeGroupData sourceData, final DataState dataState, final long dataTime) {
			if(!_hasReceiver) return;

			_policy.handleData(sourceData, dataState, dataTime);
		}
	}

	/**
	 * Sendet die Daten weiter an den Empfänger. Wird von der {@link #_policy} aufgerufen.
	 *
	 * @param sourceData Daten
	 * @param dataState  Datenzustand
	 * @param dataTime   Datenzeit
	 */
	void sendDataToReceiver(final KExDaVAttributeGroupData sourceData, final DataState dataState, final long dataTime) {
		if(dataState != DataState.NO_SOURCE && !_hasSender) {
			try {
				_manager.addMessage(Message.newInfo("Starte Sender: " + this));
				if(!_target.registerSender(_atgTarget, _aspTarget, _simulationVariantTarget, _senderRole, this)){
					return;
				}
			}
			catch(MissingObjectException e) {
				// Empfängerobjekt (oder atg/asp) existiert nicht
				_manager.addMessage(Message.newError(e));
				return;
			}
			_hasSender = true;
		}
		else if(dataState == DataState.NO_SOURCE && _hasSender) {
			_target.unsubscribeSender(this);
			_hasSender = false;
		}

		if(!_hasSender) {
			return;
		}

		KExDaVAttributeGroupData targetData = null;
		if(sourceData != null) {
			try {
				targetData = new KExDaVAttributeGroupData(_target.getConnection(), _atgTarget, _manager);

				_plugin.process(sourceData, targetData, _objectManagerInterface, _manager);
			}
			catch(MissingObjectException e) {
				// Attributgruppe fehlt
				_manager.addMessage(Message.newError(e));
			}
			catch(DataCopyException e) {
				// Data konnte nicht kopiert werden. Kann z.B. von Plugins ausgelöst werden oder tritt auf wenn die Attributgruppen in grob
				// unterschiedlichen, inkompatiblen Versionen vorlegen (z.B. ein Attribut ist einmal ein Array und einmal ein einfacher Wert)
				// Falls einfach nur Werte fehlen wird weiter unten eine weniger schwerwiegende Meldung ausgelöst.
				_manager.addMessage(Message.newMajor("Kann Daten nicht kopieren", e));
			}
		}
		if(targetData == null || targetData.isDefined()) {
			_target.sendData(this, targetData, dataTime);
			if(targetData != null && _stopOnNextData) {
				stop();
				_stopOnNextData = false;
			}
		}
		else {
			_manager.addMessage(
					Message.newMajor(
							"Ein Datensatz konnte nicht übertragen werden, da für erforderliche Attribute keine Daten bereitstehen: \n"
							+ LowLevelDataPipe.this.toString() + "\n" + targetData
					)
			);
			_target.sendData(this, null, dataTime);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final LowLevelDataPipe other = (LowLevelDataPipe)o;

		if(_simulationVariantSource != other._simulationVariantSource) return false;
		if(_simulationVariantTarget != other._simulationVariantTarget) return false;
		if(!_aspSource.equals(other._aspSource)) return false;
		if(!_aspTarget.equals(other._aspTarget)) return false;
		if(!_atgSource.equals(other._atgSource)) return false;
		if(!_atgTarget.equals(other._atgTarget)) return false;
		if(!_plugin.equals(other._plugin)) return false;
		if(!_receiveOptions.equals(other._receiveOptions)) return false;
		if(!_receiver.equals(other._receiver)) return false;
		if(!_receiverRole.equals(other._receiverRole)) return false;
		if(!_senderRole.equals(other._senderRole)) return false;
		if(!_source.equals(other._source)) return false;
		if(!_target.equals(other._target)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _source.hashCode();
		result = 31 * result + _target.hashCode();
		result = 31 * result + _atgSource.hashCode();
		result = 31 * result + _atgTarget.hashCode();
		result = 31 * result + _aspSource.hashCode();
		result = 31 * result + _aspTarget.hashCode();
		result = 31 * result + (int)_simulationVariantSource;
		result = 31 * result + (int)_simulationVariantTarget;
		result = 31 * result + _receiveOptions.hashCode();
		result = 31 * result + _plugin.hashCode();
		result = 31 * result + _receiver.hashCode();
		result = 31 * result + _receiverRole.hashCode();
		result = 31 * result + _senderRole.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return _source + ":" + _atgSource + ":" + _aspSource + ":" + _simulationVariantSource + " => " + _target + ":" + _atgTarget + ":" + _aspTarget + ":"
		       + _simulationVariantTarget;
	}
}
