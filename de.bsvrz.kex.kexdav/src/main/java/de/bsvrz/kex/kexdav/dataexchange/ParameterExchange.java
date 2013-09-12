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

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.kex.kexdav.correspondingObjects.*;
import de.bsvrz.kex.kexdav.dataplugin.BasicKExDaVDataPlugin;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAttributeGroup;
import de.bsvrz.kex.kexdav.systemobjects.PidSpecification;

import static de.bsvrz.kex.kexdav.main.Constants.Pids.*;

/**
 * Klasse zum Austausch von Parameterdaten einer Datenidentifikation eines Objektes. (diese Klasse gibt es einmal pro Attributgruppe und Objekt)
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public class ParameterExchange {

	private static final KExDaVDataPlugin _defaultPlugIn = new BasicKExDaVDataPlugin();

	private LowLevelDataPipe _pipeLocalRemote = null;

	private LowLevelDataPipe _pipeRemoteLocal = null;

	private boolean _isUsingTrigger = false;

	/**
	 * Erstellt einen neuen Parameterdaten-Austausch
	 *
	 * @param description            Beschreibung über Objekt, Attributgruppe usw.
	 * @param objectManagerInterface Verwaltung korrespondierender Objekte
	 * @param manager                KExDaV-Manager-Objekt, an das Benachrichtigungen gesendet werden können
	 */
	public ParameterExchange(
			final ParameterExchangeDescription description, final ObjectManagerInterface objectManagerInterface, final ManagerInterface manager) {
		final CorrespondingObject object = objectManagerInterface.getObject(new PidSpecification(description.getObjectPid()));
		final CorrespondingAttributeGroup atg = description.getAtg();
		final short simLocal = description.getSimLocal();
		final short simRemote = description.getSimRemote();
		final boolean delta = description.isDelta();
		switch(description.getStrategy()) {
			case LocalManagementRemoteReadWrite:
				// Schreibverbindung in Gegenrichtung
				_pipeRemoteLocal = new LowLevelDataPipe(
						object.getRemoteObject(),
						object.getLocalObject(),
						atg.getAtgRemotePid(),
						atg.getAtgLocalPid(),
						AspectParameterTarget,
						AspectParameterTarget,
						simRemote,
						simLocal,
						delta ? ReceiveOptions.delta() : ReceiveOptions.normal(),
						ReceiverRole.drain(),
						SenderRole.sender(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				// Kein break, jetzt folgt die normale Verbindung
			case LocalManagementRemoteRead:
				_pipeLocalRemote = new LowLevelDataPipe(
						object.getLocalObject(),
						object.getRemoteObject(),
						atg.getAtgLocalPid(),
						atg.getAtgRemotePid(),
						AspectParameterDesired,
						AspectParameterDesired,
						simLocal,
						simRemote,
						delta ? ReceiveOptions.delta() : ReceiveOptions.normal(),
						ReceiverRole.receiver(),
						SenderRole.source(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				break;

			case RemoteManagementLocalReadWrite:
				// Schreibverbindung in Gegenrichtung
				_pipeLocalRemote = new LowLevelDataPipe(
						object.getLocalObject(),
						object.getRemoteObject(),
						atg.getAtgLocalPid(),
						atg.getAtgRemotePid(),
						AspectParameterTarget,
						AspectParameterTarget,
						simLocal,
						simRemote,
						delta ? ReceiveOptions.delta() : ReceiveOptions.normal(),
						ReceiverRole.drain(),
						SenderRole.sender(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				// Kein break, jetzt folgt die normale Verbindung
			case RemoteManagementLocalRead:
				_pipeRemoteLocal = new LowLevelDataPipe(
						object.getRemoteObject(),
						object.getLocalObject(),
						atg.getAtgRemotePid(),
						atg.getAtgLocalPid(),
						AspectParameterDesired,
						AspectParameterDesired,
						simRemote,
						simLocal,
						delta ? ReceiveOptions.delta() : ReceiveOptions.normal(),
						ReceiverRole.receiver(),
						SenderRole.source(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				break;

			case LocalAndRemoteManagementWithTrigger:

				_isUsingTrigger = true;
				// kein break, die gleichen Datenkanäle erstellen, wie im LocalAndRemoteManagement-modus
			case LocalAndRemoteManagement:
				_pipeLocalRemote = new LowLevelDataPipe(
						object.getLocalObject(),
						object.getRemoteObject(),
						atg.getAtgLocalPid(),
						atg.getAtgRemotePid(),
						AspectParameterDesired,
						AspectParameterTarget,
						simLocal,
						simRemote,
						ReceiveOptions.delta(),
						// Delta hier nicht beachten: TAnf 4.4.2.5
						ReceiverRole.receiver(),
						SenderRole.sender(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				_pipeRemoteLocal = new LowLevelDataPipe(
						object.getRemoteObject(),
						object.getLocalObject(),
						atg.getAtgRemotePid(),
						atg.getAtgLocalPid(),
						AspectParameterDesired,
						AspectParameterTarget,
						simRemote,
						simLocal,
						ReceiveOptions.delta(),
						// Delta hier nicht beachten: TAnf 4.4.2.5
						ReceiverRole.receiver(),
						SenderRole.sender(),
						_defaultPlugIn,
						objectManagerInterface,
						manager
				);
				break;
		}

		if(description.getStrategy() == ParameterExchangeStrategy.LocalAndRemoteManagement) {
			// Transfer-Richtlinie einrichten, mit der der ständige wechselseitige Austausch von Parametern verhindert wird.
			final ParameterDataTransferPolicy parameterDataTransferPolicy = new ParameterDataTransferPolicy(_pipeLocalRemote, _pipeRemoteLocal);
			_pipeLocalRemote.setPolicy(parameterDataTransferPolicy.getLocalRemotePolicy());
			_pipeRemoteLocal.setPolicy(parameterDataTransferPolicy.getRemoteLocalPolicy());
		}
	}

	/** Startet den Parameteraustausch. Keine Wirkung bei triggerndem Parameteraustausch */
	public void start() {
		if(_isUsingTrigger) return;
		if(_pipeLocalRemote != null) {
			_pipeLocalRemote.start();
		}
		if(_pipeRemoteLocal != null) {
			_pipeRemoteLocal.start();
		}
	}

	/** Stoppt den Parameteraustausch. Keine Wirkung bei triggerndem Parameteraustausch */
	public void stop() {
		if(_isUsingTrigger) return;
		if(_pipeLocalRemote != null) {
			_pipeLocalRemote.stop();
		}
		if(_pipeRemoteLocal != null) {
			_pipeRemoteLocal.stop();
		}
	}

	/**
	 * Triggert den Parameteraustausch
	 * @param direction Richtung
	 */
	public void triggerExchange(Direction direction) {
		if(!_isUsingTrigger) return;
		if(direction == Direction.LocalToRemote && _pipeLocalRemote != null) {
			_pipeLocalRemote.startOneTime();
		}
		else if(direction == Direction.RemoteToLocal && _pipeRemoteLocal != null) {
			_pipeRemoteLocal.startOneTime();
		}
	}
}
