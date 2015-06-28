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

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.kex.kexdav.correspondingObjects.ObjectManagerInterface;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;

/**
 * Implementierung der {@link de.bsvrz.kex.kexdav.dataexchange.LowLevelDataPipe}, bei der zuerst ein Sender angemeldet wird
 * und wo dann je nach Sendesteuerung des Senders die Senke beim anderen Datenverteiler an- oder abgemeldet wird.
 *
 * Wird außerdem für Verbindungen von Sender nach Empfänger (für den bidirektionalen Parameteraustausch) benutzt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12676 $
 */
public class LowLevelDataPipeDrain extends LowLevelDataPipe {

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
	protected LowLevelDataPipeDrain(
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
		super(manager, aspTarget, objectManagerInterface, atgSource, target, simulationVariantTarget, atgTarget, simulationVariantSource, aspSource, plugin, receiverRole, senderRole, receiveOptions, source);
		if(source == null) throw new IllegalArgumentException("source ist null");
		if(target == null) throw new IllegalArgumentException("target ist null");
		if(atgSource == null) throw new IllegalArgumentException("atgSource ist null");
		if(atgTarget == null) throw new IllegalArgumentException("atgTarget ist null");
		if(aspSource == null) throw new IllegalArgumentException("aspSource ist null");
		if(aspTarget == null) throw new IllegalArgumentException("aspTarget ist null");
		if(plugin == null) throw new IllegalArgumentException("plugin ist null");
		if(manager == null) throw new IllegalArgumentException("manager ist null");
	}

	public void start() {
		if(_hasSender) return;
		_manager.addMessage(Message.newInfo("Starte Sender: " + this));
		_hasSender = true;
		try {
			
			try {
				Thread.sleep(10);
			}
			catch(InterruptedException ignored) {
			}
			if(!_target.registerSender(_atgTarget, _aspTarget, _simulationVariantTarget, _senderRole, _sender)){
				_hasSender = false;
			}
		}
		catch(MissingObjectException e) {
			// Senderobjekt (oder atg/asp) existiert nicht
			_manager.addMessage(Message.newError(e));
			_hasSender = false;
		}
	}

	@Override
	protected void updateSendControl(final byte state) {
		boolean stateOK = (state == ClientSenderInterface.START_SENDING);
		if(stateOK && !_hasReceiver) {
			_hasReceiver = true;
			try {
				_manager.addMessage(Message.newInfo("Starte Empfänger: " + this));
				if(!_source.registerReceiver(_atgSource, _aspSource, _simulationVariantSource, _receiverRole, _receiveOptions, _receiver)){
					_hasReceiver = false;
				}
			}
			catch(MissingObjectException e) {
				// Empfängerobjekt (oder atg/asp) existiert nicht
				_manager.addMessage(Message.newError(e));
				_hasReceiver = false;
			}
		}
		else if(!stateOK && _hasReceiver) {
			_target.unsubscribeReceiver(_receiver);
			_hasReceiver = false;
		}
	}

	protected void sendDataToReceiver(final KExDaVAttributeGroupData sourceData, final DataState dataState, final long dataTime) {


		if(dataState == DataState.NO_RIGHTS){
			_manager.addMessage(Message.newMajor("Keine Rechte zum Empfang von Daten: " +  this));
		}
		else if(dataState == DataState.INVALID_SUBSCRIPTION){
			_manager.addMessage(Message.newMajor("Ungültige Anmeldung des Empfängers: " +  this));
		}

		if(!_hasReceiver) {
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
			_target.sendData(_sender, targetData, dataTime);
			if(targetData != null && _stopOnNextData) {
				stop();
				_stopOnNextData = false;
			}
		}
		else {
			_manager.addMessage(
					Message.newMajor(
							"Ein Datensatz konnte nicht übertragen werden, da für erforderliche Attribute keine Daten bereitstehen: \n"
							+ LowLevelDataPipeDrain.this.toString() + "\n" + targetData
					)
			);
			_target.sendData(_sender, null, dataTime);
		}
	}
}
