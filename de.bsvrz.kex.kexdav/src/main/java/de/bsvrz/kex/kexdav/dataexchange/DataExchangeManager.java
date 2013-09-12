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

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.correspondingObjects.CorrespondingObjectManager;
import de.bsvrz.kex.kexdav.main.AbstractKExDaVExchange;
import de.bsvrz.kex.kexdav.main.KExDaVException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.parameterloader.DataExchangeIdentification;
import de.bsvrz.kex.kexdav.parameterloader.ExchangeDataParameter;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;

import java.util.HashSet;
import java.util.Set;

/**
 * Verwaltung der Onlinedaten-Austausche pro Datenverteilerverbindung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public class DataExchangeManager extends AbstractKExDaVExchange<DataExchangeDescription, DataExchange> {

	private final CorrespondingObjectManager _correspondingObjectManager;

	/**
	 * Konstruktor
	 *
	 * @param parameter                  Parameter
	 * @param manager                    Verwaltung
	 * @param correspondingObjectManager Verwaltung korrespondierender Objekte
	 */
	public DataExchangeManager(
			final RemoteDaVParameter parameter, final ManagerInterface manager, final CorrespondingObjectManager correspondingObjectManager) {
		super(parameter, manager);
		_correspondingObjectManager = correspondingObjectManager;
	}

	@Override
	protected void notifyNewExchangeDescriptions(final Set<DataExchangeDescription> newExchangeDescriptions) {
//		final List<String> pids = new ArrayList<String>();
//		for(DataExchangeDescription newExchangeDescription : newExchangeDescriptions) {
//			pids.add(newExchangeDescription.getObjectPid());
//		}
//		DafDataModel dataModel = (DafDataModel)_correspondingObjectManager.getRemoteConnection().getDataModel();
	}

	@Override
	protected DataExchange createExchange(final DataExchangeDescription description) throws KExDaVException {
		final DataExchange dataExchange = new DataExchange(description, _correspondingObjectManager, getManager());
		dataExchange.start();
		return dataExchange;
	}

	@Override
	protected void removeExchange(final DataExchange exchange) {
		exchange.stop();
	}

	@Override
	protected Set<DataExchangeDescription> getExchangeDescriptionsFromNewParameters(final RemoteDaVParameter parameters) {
		final Set<DataExchangeDescription> result = new HashSet<DataExchangeDescription>();
		for(final ExchangeDataParameter dataParameter : parameters.getExchangeDataParameters()) {
			for(final SystemObject systemObject : dataParameter.getObjectRegionSet().getAllObjects(_correspondingObjectManager.getLocalConnection().getDataModel().getBaseTypes())) {
				if(systemObject.getPid().length() > 0) {
					for(final DataExchangeIdentification identification : dataParameter.getIdentificationList()) {
						final DataExchangeDescription description = new DataExchangeDescription(
								systemObject.getPid(),
								identification.getDirection(),
								identification.getAttributeGroup(),
								identification.getAspect(),
								identification.getSimLocal(),
								identification.getSimRemote(),
								identification.isDelayed(),
								identification.isDelta()
						);
						result.add(description);
					}
				}
			}
		}
		return result;
	}
}
