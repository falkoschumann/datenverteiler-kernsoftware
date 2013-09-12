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
import de.bsvrz.kex.kexdav.parameterloader.CorrespondingAttributeGroup;
import de.bsvrz.kex.kexdav.parameterloader.ExchangeParameterParameter;
import de.bsvrz.kex.kexdav.parameterloader.ParameterExchangeIdentification;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameteraustauschverwaltung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public class ParameterExchangeManager extends AbstractKExDaVExchange<ParameterExchangeDescription, ParameterExchange> {

	private final CorrespondingObjectManager _correspondingObjectManager;

	/**
	 * Konstruktor
	 *
	 * @param parameter                  Parameter
	 * @param manager                    Verwaltung
	 * @param correspondingObjectManager Verwaltung korrespondierender Objekte
	 */
	public ParameterExchangeManager(
			final RemoteDaVParameter parameter, final ManagerInterface manager, final CorrespondingObjectManager correspondingObjectManager) {
		super(parameter, manager);
		_correspondingObjectManager = correspondingObjectManager;
	}

	@Override
	protected ParameterExchange createExchange(final ParameterExchangeDescription description) throws KExDaVException {
		final ParameterExchange parameterExchange = new ParameterExchange(description, _correspondingObjectManager, getManager());
		parameterExchange.start();
		return parameterExchange;
	}

	@Override
	protected void removeExchange(final ParameterExchange exchange) {
		exchange.stop();
	}

	@Override
	protected Set<ParameterExchangeDescription> getExchangeDescriptionsFromNewParameters(final RemoteDaVParameter parameters) {
		Collection<ExchangeParameterParameter> exchangeParameterParameters = parameters.getExchangeParameterParameters();
		final Set<ParameterExchangeDescription> result = new HashSet<ParameterExchangeDescription>(exchangeParameterParameters.size());
		for(final ExchangeParameterParameter parameterParameter : exchangeParameterParameters) {
			for(final SystemObject systemObject : parameterParameter.getObjectRegionSet().getAllObjects(_correspondingObjectManager.getLocalConnection().getDataModel().getBaseTypes())) {
				if(systemObject.getPid().length() > 0) {
					for(final ParameterExchangeIdentification identification : parameterParameter.getIdentificationList()) {
						for(final CorrespondingAttributeGroup attributeGroup : identification.getAttributeGroups()) {
							final ParameterExchangeDescription description = new ParameterExchangeDescription(
									systemObject.getPid(),
									identification.getStrategy(),
									attributeGroup,
									identification.getSimLocal(),
									identification.getSimRemote(),
									identification.isDelta()
							);
							result.add(description);
						}
					}
				}
			}
		}
		return result;
	}
}
