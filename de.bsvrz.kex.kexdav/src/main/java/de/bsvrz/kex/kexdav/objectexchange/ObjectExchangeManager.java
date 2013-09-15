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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.dav.dav.util.accessControl.*;
import de.bsvrz.kex.kexdav.correspondingObjects.*;
import de.bsvrz.kex.kexdav.main.*;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.parameterloader.*;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

import java.util.*;

/**
 * Verwaltung Objektaustausche
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public class ObjectExchangeManager extends AbstractKExDaVExchange<ObjectExchangeDescription, CopyableCorrespondingObject>
		implements ObjectCollectionParent, ObjectCollectionChangeListener, RegionManager {

	private final CorrespondingObjectManager _correspondingObjectManager;

	private final Collection<ObjectCollection> _objectCollections = new HashSet<ObjectCollection>();

	private final Map<ClientDavInterface, Map<SystemObject, Region>> _regions = new HashMap<ClientDavInterface, Map<SystemObject, Region>>();

	/**
	 * Konstruktor
	 *
	 * @param parameter                  Parameter
	 * @param manager                    Verwaltung
	 * @param correspondingObjectManager Verwaltung korrespondierende Objekte
	 */
	public ObjectExchangeManager(
			final RemoteDaVParameter parameter, final ManagerInterface manager, final CorrespondingObjectManager correspondingObjectManager) {
		super(parameter, manager);
		_correspondingObjectManager = correspondingObjectManager;
	}

	@Override
	protected CopyableCorrespondingObject createExchange(final ObjectExchangeDescription description) throws KExDaVException {
		return _correspondingObjectManager.createObjectExchange(
				description.getObjectSpecification(), description.getStrategy()
		);
	}

	@Override
	protected void removeExchange(final CopyableCorrespondingObject exchange) {
		_correspondingObjectManager.removeObjectExchange(exchange.getObjectSpecification(), exchange.getStrategy());
	}

	@Override
	protected Set<ObjectExchangeDescription> getExchangeDescriptionsFromNewParameters(final RemoteDaVParameter parameters) {
		for(final ObjectCollection objectCollection : _objectCollections) {
			objectCollection.removeChangeListener(this);
		}
		_objectCollections.clear();
		final Set<ObjectExchangeDescription> result = new HashSet<ObjectExchangeDescription>();
		for(final ObjectCollectionFactory objectParameter : parameters.getExchangeObjectsLocalRemote()) {
			for(final SystemObject systemObject : getSystemObjectsFromParameter(objectParameter, _correspondingObjectManager.getLocalConnection())) {
					result.add(new ObjectExchangeDescription(ObjectSpecification.create(systemObject, getManager()), Direction.LocalToRemote));
			}
		}
		for(final ObjectCollectionFactory objectParameter : parameters.getExchangeObjectsRemoteLocal()) {
			for(final SystemObject systemObject : getSystemObjectsFromParameter(objectParameter, _correspondingObjectManager.getRemoteConnection())) {
					result.add(new ObjectExchangeDescription(ObjectSpecification.create(systemObject, getManager()), Direction.RemoteToLocal));
			}
		}
		return result;
	}

	private Collection<SystemObject> getSystemObjectsFromParameter(final ObjectCollectionFactory parameter, final ClientDavInterface connection) {
		final ObjectCollection objectCollection;
		try {
			objectCollection = parameter.createObjectCollection(this, connection);
		}
		catch(MissingObjectException e) {
			getManager().addMessage(Message.newError(e));
			return Collections.emptyList();
		}
		objectCollection.addChangeListener(this);
		_objectCollections.add(objectCollection);
		return objectCollection.getAllObjects(Arrays.asList(connection.getDataModel().getType(Constants.Pids.TypeDynamicObject)));
	}

	public boolean isDisabled(final Region region) {
		return false;
	}

	public synchronized Region getRegion(final SystemObject regionObject) {
		final ClientDavInterface connection = ((DafDataModel)regionObject.getDataModel()).getConnection();
		Map<SystemObject, Region> regions = _regions.get(connection);
		if(regions == null) {
			regions = new HashMap<SystemObject, Region>();
			_regions.put(connection, regions);
		}

		Region region = regions.get(regionObject);
		if(region == null) {
			region = new Region(regionObject, connection, this);
			regions.put(regionObject, region);
		}
		return region;
	}

	public void objectChanged(final DataLoader object) {

	}

	public Object getUpdateLock() {
		return this;
	}

	public void blockChanged() {
		try {
			start();
		}
		catch(MissingAreaException e) {
			getManager().addMessage(Message.newError(e));
		}
	}
}
