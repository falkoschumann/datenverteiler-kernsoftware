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

package de.bsvrz.kex.kexdav.objectexchange;

import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.correspondingObjects.CorrespondingObjectManager;
import de.bsvrz.kex.kexdav.main.AbstractKExDaVExchange;
import de.bsvrz.kex.kexdav.main.KExDaVException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.parameterloader.ExchangeSetParameter;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;

import java.util.HashSet;
import java.util.Set;

/**
 * Mengenaustauschverwaltung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9218 $
 */
public class SetExchangeManager extends AbstractKExDaVExchange<SetExchangeDescription, DynamicSetExchange> {

	private final CorrespondingObjectManager _correspondingObjectManager;

	/**
	 * Konstruktor
	 *
	 * @param parameter                  Parameter
	 * @param manager                    Verwaltung
	 * @param correspondingObjectManager Verwaltung korrespondierender Objekte
	 */
	public SetExchangeManager(
			final RemoteDaVParameter parameter, final ManagerInterface manager, final CorrespondingObjectManager correspondingObjectManager) {
		super(parameter, manager);
		_correspondingObjectManager = correspondingObjectManager;
	}

	@Override
	protected DynamicSetExchange createExchange(final SetExchangeDescription description) throws KExDaVException {
		final DynamicSetExchange setExchange;
		setExchange = new DynamicSetExchange(
				description.getLocalObject(),
				description.getRemoteObject(),
				description.getLocalSetName(),
				description.getRemoteSetName(),
				description.getStrategy(),
				_correspondingObjectManager,
				getManager()
		);

		setExchange.start();
		return setExchange;
	}

	@Override
	protected void removeExchange(final DynamicSetExchange exchange) {
		exchange.stop();
	}

	@Override
	protected Set<SetExchangeDescription> getExchangeDescriptionsFromNewParameters(final RemoteDaVParameter parameters) {
		final Set<SetExchangeDescription> result = new HashSet<SetExchangeDescription>();
		for(final ExchangeSetParameter setParameter : parameters.getExchangeSetParameters()) {
			final SystemObject localObject = _correspondingObjectManager.getLocalConnection().getDataModel().getObject(setParameter.getLocalObject());
			final SystemObject remoteObject = _correspondingObjectManager.getRemoteConnection().getDataModel().getObject(setParameter.getRemoteObject());
			if(remoteObject != null && localObject != null && remoteObject instanceof ConfigurationObject && localObject instanceof ConfigurationObject) {
				result.add(
						new SetExchangeDescription(
								(ConfigurationObject)localObject,
								(ConfigurationObject)remoteObject,
								setParameter.getLocalSet(),
								setParameter.getRemoteSet(),
								setParameter.getDirection()
						)
				);
			}
			else {
				getManager().addMessage(
						Message.newMajor(
								"Ein Konfigurationsobjekt für den Mengenaustausch ist ungültig. Betroffener Austausch: " + setParameter.getLocalObject()
								+ " <--> " + setParameter.getRemoteObject()
						)
				);
			}
		}
		return result;
	}
}
