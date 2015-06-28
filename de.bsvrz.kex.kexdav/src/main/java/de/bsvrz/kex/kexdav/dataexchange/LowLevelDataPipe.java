/*
 * Copyright 2014 by Kappich Systemberatung Aachen
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
 * @version $Revision: 12676 $
 */
public abstract class LowLevelDataPipe {
	protected final KExDaVObject _source;
	protected final KExDaVObject _target;
	protected final String _atgSource;
	protected final String _atgTarget;
	protected final String _aspSource;
	protected final String _aspTarget;
	protected final short _simulationVariantSource;
	protected final short _simulationVariantTarget;
	protected final ReceiveOptions _receiveOptions;
	protected final KExDaVDataPlugin _plugin;
	protected final ReceiverRole _receiverRole;
	protected final SenderRole _senderRole;
	protected final ObjectManagerInterface _objectManagerInterface;
	protected final ManagerInterface _manager;
	protected final KExDaVReceiver _receiver = new MyReceiver();
	protected final KExDaVSender _sender = new MySender();
	protected DataTransferPolicy _policy;
	protected volatile boolean _hasSender = false;
	protected volatile boolean _hasReceiver = false;
	protected volatile boolean _stopOnNextData = false;

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
	public static LowLevelDataPipe createLowLevelDataPipe(
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
		if(senderRole == SenderRole.source()) {
			return new LowLevelDataPipeSource(source, target, atgSource, atgTarget, aspSource, aspTarget, simulationVariantSource, simulationVariantTarget, receiveOptions, receiverRole, senderRole, plugin, objectManagerInterface, manager);
		}
		else {
			return new LowLevelDataPipeDrain(source, target, atgSource, atgTarget, aspSource, aspTarget, simulationVariantSource, simulationVariantTarget, receiveOptions, receiverRole, senderRole, plugin, objectManagerInterface, manager);
		}
	}

	protected LowLevelDataPipe(final ManagerInterface manager, final String aspTarget, final ObjectManagerInterface objectManagerInterface, final String atgSource, final KExDaVObject target, final short simulationVariantTarget, final String atgTarget, final short simulationVariantSource, final String aspSource, final KExDaVDataPlugin plugin, final ReceiverRole receiverRole, final SenderRole senderRole, final ReceiveOptions receiveOptions, final KExDaVObject source) {
		_manager = manager;
		_aspTarget = aspTarget;
		_objectManagerInterface = objectManagerInterface;
		_atgSource = atgSource;
		_target = target;
		_simulationVariantTarget = simulationVariantTarget;
		_atgTarget = atgTarget;
		_simulationVariantSource = simulationVariantSource;
		_aspSource = aspSource;
		_plugin = plugin;
		_receiverRole = receiverRole;
		_policy = new BasicTransferPolicy(this);
		_senderRole = senderRole;
		_receiveOptions = receiveOptions;
		_source = source;
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
	public abstract void start();

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
			_target.unsubscribeSender(_sender);
			_hasSender = false;
		}
	}

	/** Führt nur einen Datenaustausch durch */
	public void startOneTime() {
		_stopOnNextData = true;
		start();
	}

	/**
	 * Sendet die Daten weiter an den Empfänger. Wird von der {@link #_policy} aufgerufen.
	 *
	 * @param sourceData Daten
	 * @param dataState  Datenzustand
	 * @param dataTime   Datenzeit
	 */
	abstract void sendDataToReceiver(KExDaVAttributeGroupData sourceData, DataState dataState, long dataTime);

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

	private class MyReceiver implements KExDaVReceiver {

		public void update(final KExDaVAttributeGroupData sourceData, final DataState dataState, final long dataTime) {
			if(!_hasReceiver) return;

			_policy.handleData(sourceData, dataState, dataTime);
		}
	}

	private class MySender implements KExDaVSender {

		@Override
		public void update(final byte state) {
			updateSendControl(state);
		}
	}

	protected abstract void updateSendControl(final byte state);
}
