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
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAspect;
import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAttributeGroup;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVObject;
import de.bsvrz.kex.kexdav.systemobjects.PidSpecification;

/**
 * Modul zum Austausch von Online-Daten (diese Klasse gibt es einmal pro Attributgruppe und Objekt)
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12677 $
 */
public class DataExchange {

	private final LowLevelDataPipe _lowLevelDataPipe;

	/**
	 * Erstellt ein Modul zum Austausch von Onlinedaten von einem Objekt
	 *
	 * @param description            Parameter zum Datenaustausch
	 * @param objectManagerInterface Interface zum {@link de.bsvrz.kex.kexdav.correspondingObjects.CorrespondingObjectManager}
	 * @param manager                KExDaV-Manager an den Warnungen usw. geschickt werden können
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingPluginException Falls kein Plugin gefunden werden konnte um die Attributgruppen zu konvertieren
	 */
	public DataExchange(
			final DataExchangeDescription description, final ObjectManagerInterface objectManagerInterface, final ManagerInterface manager)
			throws MissingPluginException {
		final KExDaVObject source;
		final KExDaVObject target;
		final String atgSource;
		final String atgTarget;
		final String aspSource;
		final String aspTarget;
		final short simulationVariantSource;
		final short simulationVariantTarget;
		final DataExchangeStrategy direction = description.getDirection();
		final CorrespondingObject object = objectManagerInterface.getObject(new PidSpecification(description.getObjectPid()));
		final CorrespondingAttributeGroup attributeGroup = description.getAttributeGroup();
		final CorrespondingAspect aspect = description.getAspect();
		final short simLocal = description.getSimLocal();
		final short simRemote = description.getSimRemote();
		if(direction == DataExchangeStrategy.SourceReceiver || direction == DataExchangeStrategy.SenderDrain) {
			source = object.getLocalObject();
			target = object.getRemoteObject();
			atgSource = attributeGroup.getAtgLocalPid();
			atgTarget = attributeGroup.getAtgRemotePid();
			aspSource = aspect.getAspectLocalPid();
			aspTarget = aspect.getAspectRemotePid();
			simulationVariantSource = simLocal;
			simulationVariantTarget = simRemote;
		}
		else {
			source = object.getRemoteObject();
			target = object.getLocalObject();
			atgSource = attributeGroup.getAtgRemotePid();
			atgTarget = attributeGroup.getAtgLocalPid();
			aspSource = aspect.getAspectRemotePid();
			aspTarget = aspect.getAspectLocalPid();
			simulationVariantSource = simRemote;
			simulationVariantTarget = simLocal;
		}

		_lowLevelDataPipe = LowLevelDataPipe.createLowLevelDataPipe(
				source,
				target,
				atgSource,
				atgTarget,
				aspSource,
				aspTarget,
				simulationVariantSource,
				simulationVariantTarget,
				description.isDelta() ? ReceiveOptions.delta() : description.isDelayed() ? ReceiveOptions.delayed() : ReceiveOptions.normal(),
				direction == DataExchangeStrategy.SenderDrain || direction == DataExchangeStrategy.DrainSender ? ReceiverRole.drain() : ReceiverRole
						.receiver(),
				direction == DataExchangeStrategy.SourceReceiver || direction == DataExchangeStrategy.ReceiverSource
						? SenderRole.source()
						: SenderRole.sender(),
				objectManagerInterface.getPlugIn(atgSource, atgTarget),
				objectManagerInterface,
				manager
		);
	}

	/**
	 * Startet den Datenaustausch
	 */
	public void start() {
		_lowLevelDataPipe.start();
	}

	/**
	 * Stoppt den Datenaustausch
	 */
	public void stop() {
		_lowLevelDataPipe.stop();
	}

	@Override
	public String toString() {
		return _lowLevelDataPipe.toString();
	}
}
