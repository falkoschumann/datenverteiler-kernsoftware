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

package de.bsvrz.kex.kexdav.parameterloader;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.ReferenceAttributeType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.dav.util.accessControl.*;
import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.dataexchange.*;
import de.bsvrz.kex.kexdav.main.*;
import de.bsvrz.kex.kexdav.management.KExDaVManager;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.main.Direction;
import de.bsvrz.kex.kexdav.systemobjects.*;
import de.bsvrz.sys.funclib.concurrent.UnboundedQueue;

import java.util.*;

/**
 * Diese Klasse liest die Parameter von dem KExDaV-Objekt ein und gibt die Parameter weiter
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9270 $
 */
public class ParameterLoader extends DataLoader implements ObjectCollectionParent, ObjectCollectionChangeListener, RegionManager {

	private static final Object _lock = new Object();

	private final ClientDavInterface _connection;

	private final KExDaVManager _manager;

	private final KExDaV _kExDaV;

	private final Map<SystemObject, Region> _regionMap = new HashMap<SystemObject, Region>();

	private Set<RemoteDaVParameter> _parameters = null;

	private final KExDaVObject _parameterPublisher;

	private Data _parameterData = null;

	private long _lastTriggerTime = System.currentTimeMillis();

	private final UnboundedQueue<Data> _dataQueue = new UnboundedQueue<Data>();

	/**
	 * Erstellt ein neues Objekt für das Daten aktualisiert werden sollen.
	 *
	 * @param connection   Verbindung zum Datenverteiler
	 * @param systemObject KExDaV-SystemObjekt, für das Parameter geladen werden sollen
	 * @param manager      Manager-Klasse an die Benachrichtigungen und Warnungen geschickt werden können
	 * @param kExDaV       Hauptklasse KExDaV, wird über neue Parameter benachrichtigt
	 */
	public ParameterLoader(
			final ClientDavInterface connection, final SystemObject systemObject, final KExDaVManager manager, final KExDaV kExDaV) {
		super(connection, Constants.Pids.AtgSpecificationKExDaV, Constants.Pids.AspectParameterDesired, _lock);
		_connection = connection;
		_manager = manager;
		_kExDaV = kExDaV;
		_parameterPublisher = new KExDaVObject(new IdSpecification(systemObject.getId(), _manager), connection, manager);
		try {
			_parameterPublisher.registerSender(
					Constants.Pids.AtgSpecificationKExDaV, Constants.Pids.AspectParameterActual, (short)-1, SenderRole.source(), this
			);
			_parameterPublisher.registerReceiver(
					Constants.Pids.AtgTriggerKExDaV,
					Constants.Pids.AspectRequest,
					(short)-1,
					ReceiverRole.drain(),
					ReceiveOptions.delta(),
					new KExDaVReceiver() {
						public void update(final KExDaVAttributeGroupData data, final DataState dataState, final long dataTime) {
							if(dataState == DataState.INVALID_SUBSCRIPTION) {
								_manager.addMessage(
										Message.newError(
												"Kann nicht als Senke auf Trigger-Attributgruppe anmelden. Möglicherweise läuft bereits eine KExDaV-Applikation."
										)
								);
								_kExDaV.terminate();
							}
							else if(dataState == DataState.DATA){
								triggerParameterExchange(data, dataTime);
							}
						}
					}
			);
		}
		catch(MissingObjectException e) {
			_manager.addMessage(Message.newError(e));
			_kExDaV.terminate();
		}
		startUpdateThread();
		startDataListener(systemObject);
	}

	private void startUpdateThread() {
		final Thread thread = new Thread(
				new Runnable() {
					public void run() {
						Data data = null;
						while(true) {
							try {
								data = _dataQueue.take();
								updateFromQueue(data);
							}
							catch(InterruptedException e) {
								_manager.addMessage(Message.newError("Der Parameter-Update-Thread wurde unterbrochen", e));
								_kExDaV.terminate();
							}
						}
					}
				}
		);
		thread.setDaemon(true);
		thread.setName("Parameter-Update");
		thread.start();
	}

	private void triggerParameterExchange(final KExDaVAttributeGroupData data, final long dataTime) {
		if(dataTime > _lastTriggerTime){
			_kExDaV.triggerParameterExchange(new TriggerSpecification(data), getSetStrategy(data.getItem("AustauschRichtung").asUnscaledValue().intValue()));
			_lastTriggerTime = dataTime;
		}
	}

	@Override
	protected void update(final Data data) {
		if(data != null) {
			_dataQueue.put(data);
		}
	}

	private void updateFromQueue(final Data data) {
		final Set<RemoteDaVParameter> remoteDaVParameters = new HashSet<RemoteDaVParameter>();
		final Data.Array remoteDaVArray = data.getArray("RemoteDaV");
		for(int i = 0; i < remoteDaVArray.getLength(); i++) {
			final RemoteDaVParameter daVParameters = parseRemoteDaV(remoteDaVArray.getItem(i));
			if(daVParameters.getConnectionParameters().isActive()) {
				remoteDaVParameters.add(daVParameters);
			}
		}
		try {
			_kExDaV.setNewParameters(remoteDaVParameters);
		}
		catch(MissingAreaException e) {
			// Die Parameter sind ungültig und können nicht gesetzt werden, weil ein Konfigurationsbereich fehlt.
			_manager.addMessage(Message.newError(e));
			if(_parameterData == null) {
				_kExDaV.terminate();
			}
			else {
				// Alte Parameterdaten erneut veröffentlichen, TAnf 4.6.1
				publishParameters(_parameterData);
			}
			return;
		}
		// Die neuen Parameter veröffentlichen. Nur wenn keine MissingAreaException aufgetreten ist, dann würden die alten Parameter weiterverwendet werden.
		_parameters = remoteDaVParameters;
		_parameterData = data;
		publishParameters(_parameterData);
	}

	/**
	 * Veröffentlicht die aktuellen Parameter unter dem Aspekt ParameterIst
	 *
	 * @param data Aktuelle Daten
	 */
	private void publishParameters(final Data data) {
		_parameterPublisher.sendData(this, data, System.currentTimeMillis());
	}

	private RemoteDaVParameter parseRemoteDaV(final Data item) {
		final String ip = item.getTextValue("IP-Adresse").getText();
		final long port = item.getScaledValue("Portnummer").longValue();
		final String user = item.getTextValue("Benutzer").getText();
		final String davPid = item.getTextValue("DaV-Pid").getText();
		final long reconnectionDelay = item.getTimeValue("Wartezeit").getMillis();
		final boolean active = item.getTextValue("Aktiv").getText().equals("Ja");
		final SystemObject localArea = item.getReferenceValue("LokalAustauschDefault").getSystemObject();
		final String remoteArea = item.getTextValue("RemoteAustauschDefault").getText();
		final Collection<AreaParameter> localAreaParameters = parseAreaParameters(item.getArray("LokalAustauschBereiche"));
		final Collection<AreaParameter> remoteAreaParameters = parseAreaParameters(item.getArray("RemoteAustauschBereiche"));
		final Collection<ExchangeDataParameter> exchangeDataParameters = parseDataParameters(item.getArray("AustauschOnlinedaten"));
		final Collection<ExchangeParameterParameter> exchangeParameterParameters = parseParameterParameters(item.getArray("AustauschParameterdaten"));
		final Collection<ObjectCollectionFactory> objectParametersLocalRemote = parseObjectParameters(
				item.getArray(
						"AustauschDynamischeObjekteLokalNachRemote"
				)
		);
		final Collection<ObjectCollectionFactory> objectParametersRemoteLocal = parseObjectParameters(
				item.getArray(
						"AustauschDynamischeObjekteRemoteNachLokal"
				)
		);
		final Collection<ExchangeSetParameter> exchangeSetParameters = parseSetParameters(item.getArray("AustauschDynamischeMengen"));
		return new RemoteDaVParameter(
				new ConnectionParameter(
						ip, (int)port, user, davPid, active
				),
				reconnectionDelay,
				localArea,
				localAreaParameters,
				remoteArea,
				remoteAreaParameters,
				exchangeDataParameters,
				exchangeParameterParameters,
				objectParametersLocalRemote,
				objectParametersRemoteLocal,
				exchangeSetParameters
		);
	}

	private static Collection<AreaParameter> parseAreaParameters(final Data.Array array) {
		final Collection<AreaParameter> areaParameters = new ArrayList<AreaParameter>();
		for(int i = 0; i < array.getLength(); i++) {
			areaParameters.add(parseAreaParameter(array.getItem(i)));
		}
		return areaParameters;
	}

	private static AreaParameter parseAreaParameter(final Data data) {
		final String kb = getPid(data, "Konfigurationsbereich");
		final SystemObject[] types = data.getReferenceArray("Typ").getSystemObjectArray();
		return new AreaParameter(kb, types);
	}

	private static String getPid(final Data data, final String item) {
		return getPid(data.getItem(item));
	}

	private static String getPid(final Data data) {
		if(data.getAttributeType() instanceof ReferenceAttributeType) {
			return data.asReferenceValue().getSystemObjectPid();
		}
		else {
			return data.asTextValue().getText();
		}
	}

	private Collection<ExchangeDataParameter> parseDataParameters(final Data.Array array) {
		final Collection<ExchangeDataParameter> dataParameters = new ArrayList<ExchangeDataParameter>();
		for(int i = 0; i < array.getLength(); i++) {
			dataParameters.add(parseDataParameter(array.getItem(i)));
		}
		return dataParameters;
	}

	private ExchangeDataParameter parseDataParameter(final Data data) {
		final List<DataExchangeIdentification> identificationList = new ArrayList<DataExchangeIdentification>();
		final Data.Array dataArray = data.getArray("Onlinedaten");
		for(int i = 0; i < dataArray.getLength(); i++) {
			identificationList.add(parseDataIdentification(dataArray.getItem(i)));
		}
		final ObjectCollection objectCollection = new ObjectSet(this, _connection, data, false);
		objectCollection.addChangeListener(this);
		return new ExchangeDataParameter(
				objectCollection, identificationList
		);
	}

	private static DataExchangeIdentification parseDataIdentification(final Data item) {
		return new DataExchangeIdentification(
				new CorrespondingAttributeGroup(item),
				new CorrespondingAspect(item),
				item.getScaledValue("SimLokal").shortValue(),
				item.getScaledValue("SimRemote").shortValue(),
				item.getTextValue("Delta").getText().equals("Ja"),
				item.getTextValue("Nachgeliefert").getText().equals("Ja"),
				getOnlineDirection(item.getUnscaledValue("Richtung").intValue())
		);
	}

	private static DataExchangeStrategy getOnlineDirection(final int dir) {
		switch(dir) {
			case 1:
				return DataExchangeStrategy.SourceReceiver;
			case 2:
				return DataExchangeStrategy.SenderDrain;
			case 3:
				return DataExchangeStrategy.DrainSender;
			case 4:
				return DataExchangeStrategy.ReceiverSource;
		}
		throw new IllegalArgumentException();
	}

	private static ParameterExchangeStrategy getParameterStrategy(final int dir) {
		switch(dir) {
			case 1:
				return ParameterExchangeStrategy.LocalManagementRemoteRead;
			case 2:
				return ParameterExchangeStrategy.LocalManagementRemoteReadWrite;
			case 3:
				return ParameterExchangeStrategy.RemoteManagementLocalRead;
			case 4:
				return ParameterExchangeStrategy.RemoteManagementLocalReadWrite;
			case 5:
				return ParameterExchangeStrategy.LocalAndRemoteManagement;
			case 6:
				return ParameterExchangeStrategy.LocalAndRemoteManagementWithTrigger;
		}
		throw new IllegalArgumentException();
	}

	private static Direction getSetStrategy(final int dir) {
		switch(dir) {
			case 1:
				return Direction.LocalToRemote;
			case 2:
				return Direction.RemoteToLocal;
		}
		throw new IllegalArgumentException();
	}

	private Collection<ExchangeParameterParameter> parseParameterParameters(final Data.Array array) {
		final Collection<ExchangeParameterParameter> parameterParameters = new ArrayList<ExchangeParameterParameter>();
		for(int i = 0; i < array.getLength(); i++) {
			parameterParameters.add(parseParameterParameter(array.getItem(i)));
		}
		return parameterParameters;
	}

	private ExchangeParameterParameter parseParameterParameter(final Data data) {
		final List<ParameterExchangeIdentification> identificationList = new ArrayList<ParameterExchangeIdentification>();
		final Data.Array dataArray = data.getArray("Parameterdaten");
		for(int i = 0; i < dataArray.getLength(); i++) {
			identificationList.add(parseParameterIdentification(dataArray.getItem(i)));
		}
		return new ExchangeParameterParameter(
				new ObjectSet(this, _connection, data, false), identificationList
		);
	}

	private static ParameterExchangeIdentification parseParameterIdentification(final Data item) {
		return new ParameterExchangeIdentification(
				getAtgs(item.getArray("Attributgruppen")),
				item.getScaledValue("SimLokal").shortValue(),
				item.getScaledValue("SimRemote").shortValue(),
				item.getTextValue("Delta").getText().equals("Ja"),
				getParameterStrategy(item.getUnscaledValue("Strategie").intValue())
		);
	}

	private static List<CorrespondingAttributeGroup> getAtgs(final Data.Array item) {
		final List<CorrespondingAttributeGroup> attributeGroups = new ArrayList<CorrespondingAttributeGroup>();
		for(int i = 0; i < item.getLength(); i++) {
			attributeGroups.add(new CorrespondingAttributeGroup(item.getItem(i)));
		}
		return attributeGroups;
	}

	private static Collection<ObjectCollectionFactory> parseObjectParameters(final Data.Array array) {
		final Collection<ObjectCollectionFactory> objectsParameters = new ArrayList<ObjectCollectionFactory>();
		for(int i = 0; i < array.getLength(); i++) {
			objectsParameters.add(parseObjectParameter(array.getItem(i)));
		}
		return objectsParameters;
	}

	private static ObjectCollectionFactory parseObjectParameter(final Data data) {
		return new ExchangeObjectsParameter(data);
	}

	private static Collection<ExchangeSetParameter> parseSetParameters(final Data.Array array) {
		final Collection<ExchangeSetParameter> setParameters = new ArrayList<ExchangeSetParameter>();
		for(int i = 0; i < array.getLength(); i++) {
			setParameters.add(parseSetParameter(array.getItem(i)));
		}
		return setParameters;
	}

	private static ExchangeSetParameter parseSetParameter(final Data data) {
		final String localObject = getPid(data.getItem("DynamischeMengeLokal"), "Objekt");
		final String localSet = data.getItem("DynamischeMengeLokal").getTextValue("Menge").getText();
		final String remoteObject = getPid(data.getItem("DynamischeMengeRemote"), "Objekt");
		final String remoteSet = data.getItem("DynamischeMengeRemote").getTextValue("Menge").getText();
		final Direction strategy = getSetStrategy(data.getUnscaledValue("Richtung").intValue());
		return new ExchangeSetParameter(localObject, localSet, remoteObject, remoteSet, strategy);
	}


	@Override
	protected Collection<DataLoader> getChildObjects() {
		return new HashSet<DataLoader>(_regionMap.values());
	}

	@Override
	protected void deactivateInvalidChild(final DataLoader node) {
		// Implementierung nicht notwendig
	}

	public boolean isDisabled(final Region region) {
		return false;
	}

	public Region getRegion(final SystemObject regionObject) {
		Region region = _regionMap.get(regionObject);
		if(region != null) return region;
		region = new Region(regionObject, _connection, this);
		_regionMap.put(regionObject, region);
		return region;
	}

	public void objectChanged(final DataLoader object) {
		// Kann ignoriert werden.
	}

	public Object getUpdateLock() {
		return _lock;
	}

	public void blockChanged() {
		try {
			_kExDaV.setNewParameters(_parameters);
		}
		catch(MissingAreaException e) {
			_manager.addMessage(Message.newError(e));
		}
	}
}
