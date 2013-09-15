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

package de.bsvrz.kex.kexdav.systemobjects;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataAndATGUsageInformation;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.DynamicObject;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.InvalidationListener;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.kex.kexdav.dataexchange.KExDaVReceiver;
import de.bsvrz.kex.kexdav.main.Constants;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.sys.funclib.losb.exceptions.FailureException;
import de.bsvrz.sys.funclib.losb.kernsoftware.ConnectionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Kapselt ein Systemobjekt auf einem Datenverteiler. Bietet allgemeine Funktionen wie Prüfung auf Existenz und das Senden und Empfangen von Daten. Im Gegensatz
 * zu normalen Systemobjekten kann man sich mit dieser Klasse auch als Sender/Empfänger anmelden, wenn das zugehörige Objekt nicht auf dem Datenverteiler
 * existiert. Die wirkliche Anmeldung wird dann nachgetragen, sobald das Objekt erstellt wird.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9658 $
 */
public class KExDaVObject {

	private SystemObject _wrappedObject = null;

	private final ClientDavInterface _connection;

	private final Map<Object, InnerSender> _senders = new HashMap<Object, InnerSender>();

	private final Map<KExDaVReceiver, InnerReceiver> _receivers = new HashMap<KExDaVReceiver, InnerReceiver>();

	private final Collection<ExistenceListener> _existenceListeners = new CopyOnWriteArraySet<ExistenceListener>();

	private final ObjectSpecification _objectSpecification;

	private final ManagerInterface _manager;

	/**
	 * Erstellt ein neues KExDavObject
	 *
	 * @param pid        Objekt-Pid
	 * @param connection Datenverteiler-Verbindung
	 * @param manager    KExDaV-Manager-Objekt, an das Benachrichtigungen gesendet werden können
	 */
	public KExDaVObject(final String pid, final ClientDavInterface connection, final ManagerInterface manager) {
		this(new PidSpecification(pid), connection, manager);
	}

	/**
	 * Erstellt ein neues KExDavObject
	 *
	 * @param objectSpecification Objekt-Spezifikation
	 * @param connection          Datenverteiler-Verbindung
	 * @param manager             KExDaV-Manager-Objekt, an das Benachrichtigungen gesendet werden können
	 */
	public KExDaVObject(
			final ObjectSpecification objectSpecification, final ClientDavInterface connection, final ManagerInterface manager) {
		_manager = manager;
		if(objectSpecification == null) throw new IllegalArgumentException("objectSpecification ist null");
		if(connection == null) throw new IllegalArgumentException("connection ist null");

		_objectSpecification = objectSpecification;
		_connection = connection;

		final SystemObject object = objectSpecification.getObject(_connection.getDataModel());
		setWrappedObject(object);

		// Anmeldung auf Erstellung/Löschung des Objektes (nicht nötig bei schon vorhandenen Konfigurationsobjekten)
		if(object == null || object.getType() instanceof DynamicObjectType) {
			/** Direkt beim typ.dynamischesObject anmelden. Das Anmelden beim eigentlichen Typ dieses Objektes funktioniert nicht immer zuverlässig, weil unter Umständen dieses
			 Objekt noch gar nicht vorhanden und deshalb kein Typ ermittelbar ist, oder weil evtl. jemand das Objekt mit diesem Typ löschen könnte und ein anderes
			 Objekt mit der gleichen Pid von einem anderen Typ erstellen könnte */
			final DynamicObjectType dynamicObjectType = (DynamicObjectType)connection.getDataModel().getType(Constants.Pids.TypeDynamicObject);
			if(dynamicObjectType == null) throw new IllegalStateException(Constants.Pids.TypeDynamicObject + " konnte nicht gefunden werden");
			final Listener objectCreateDeleteListener = new Listener();
			dynamicObjectType.addInvalidationListener(objectCreateDeleteListener);
			dynamicObjectType.addObjectCreationListener(objectCreateDeleteListener);
		}
	}

	/**
	 * Anmeldung als Empfänger/Senke
	 *
	 *
	 * @param atg               Attributgruppe
	 * @param asp               Aspekt
	 * @param simulationVariant Simulationsvariante
	 * @param receiverRole      (Empfänger oder Senke)
	 * @param receiveOptions    (Delta oder Nachgeliefert oder Normal)
	 * @param receiver          Objekt an das Empfangene Daten gesendet werden. Jedes Objekt ist einer Datenidentifikation fest zugeordnet und kann nur einmal
	 *                          angemeldet werden.
	 *
	 * @throws MissingObjectException Falls Attributgruppe oder Aspekt nicht vorhanden sind
	 */
	public boolean registerReceiver(
			final String atg,
			final String asp,
			final short simulationVariant,
			final ReceiverRole receiverRole,
			final ReceiveOptions receiveOptions,
			final KExDaVReceiver receiver) throws MissingObjectException {
		if(atg == null) throw new IllegalArgumentException("atg ist null");
		if(asp == null) throw new IllegalArgumentException("asp ist null");
		if(receiverRole == null) throw new IllegalArgumentException("receiverRole ist null");
		if(receiver == null) throw new IllegalArgumentException("receiver ist null");

		if(_receivers.containsKey(receiver)) throw new IllegalArgumentException("Der Empfänger " + receiver + " ist bereits angemeldet.");

		final DataDescription dataDescription = makeDataDescription(atg, asp, simulationVariant);
		final InnerReceiver innerReceiver = new InnerReceiver(receiver, receiverRole, dataDescription, receiveOptions);

		final SystemObject systemObject = getWrappedObject();
		if(systemObject == null) return true;

		if (!checkType(systemObject.getType(), dataDescription.getAttributeGroup())) return false;

		registerReceiver(innerReceiver, systemObject);
		return true;
	}

	private void registerReceiver(
			final InnerReceiver innerReceiver, final SystemObject systemObject) {
		try {
			ConnectionManager.subscribeReceiver(
					_connection,
					innerReceiver,
					systemObject,
					innerReceiver.getDataDescription(),
					innerReceiver.getReceiveOptions(),
					innerReceiver.getReceiverRole()
			);
		}
		catch(FailureException e) {
			_manager.addMessage(Message.newMajor("Kann nicht als Empfänger anmelden", e));
		}
	}

	/**
	 * Anmeldung als Sender oder Quelle
	 *
	 *
	 * @param atg               Attributgruppe
	 * @param asp               Aspekt
	 * @param simulationVariant Simulationsvariante
	 * @param senderRole        Sender oder Quelle
	 * @param senderObject      Beliebiges Objekt das zu diesem Sender gespeichert wird. Jedes Objekt ist einer Datenidentifikation fest zugeordnet und kann nur
	 *                          einmal angemeldet werden.
	 *
	 * @throws MissingObjectException Falls Attributgruppe oder Aspekt nicht vorhanden sind
	 * @return true wenn das Objekt angemeldet wurde, sonst false.
	 */
	public boolean registerSender(
			final String atg, final String asp, final short simulationVariant, final SenderRole senderRole, final Object senderObject)
			throws MissingObjectException {
		if(atg == null) throw new IllegalArgumentException("atg ist null");
		if(asp == null) throw new IllegalArgumentException("asp ist null");
		if(senderRole == null) throw new IllegalArgumentException("senderRole ist null");
		if(senderObject == null) throw new IllegalArgumentException("senderObject ist null");

		if(_senders.containsKey(senderObject)) throw new IllegalArgumentException("Der Sender " + senderObject + " ist bereits angemeldet.");

		final DataDescription dataDescription = makeDataDescription(atg, asp, simulationVariant);

		final InnerSender innerSender = new InnerSender(senderObject, dataDescription, senderRole);

		final SystemObject systemObject = getWrappedObject();
		if(systemObject == null) return true;

		if (!checkType(systemObject.getType(), dataDescription.getAttributeGroup())) return false;

		registerSender(innerSender, systemObject);
		return true;
	}

	private static boolean checkType(final SystemObjectType type, final AttributeGroup attributeGroup) {
		return type.getAttributeGroups().contains(attributeGroup);
	}

	private void registerSender(final InnerSender innerSender, final SystemObject systemObject) {
		try {
			ConnectionManager.subscribeSender(
					_connection, innerSender, systemObject, innerSender.getDataDescription(), innerSender.getSenderRole()
			);
		}
		catch(OneSubscriptionPerSendData oneSubscriptionPerSendData) {
			_manager.addMessage(Message.newMajor("Kann nicht als Sender anmelden", oneSubscriptionPerSendData));
		}
	}

	private DataDescription makeDataDescription(final String atg, final String asp, final short simulationVariant) throws MissingObjectException {
		final AttributeGroup attributeGroup = tryGetAttributeGroup(_connection, atg);
		final Aspect aspect = tryGetAspect(_connection, asp);
		if(!attributeGroup.getAspects().contains(aspect)) {
			throw new MissingObjectException("Der Aspekt " + aspect + " kann nicht der Attributgruppe " + attributeGroup + " zugeordnet werden.");
		}
		return new DataDescription(attributeGroup, aspect, simulationVariant);
	}

	/**
	 * Sendet Daten an das Objekt
	 *
	 * @param senderObject Sender-Objekt
	 * @param data         Daten
	 * @param dataTime     Zeit des Datensatzes
	 */
	public void sendData(final Object senderObject, final Data data, final long dataTime) {
		final SystemObject systemObject = getWrappedObject();
		if(systemObject == null) return;
		try {
			final InnerSender sender = _senders.get(senderObject);
			if(sender == null) throw new IllegalStateException("Sender wurde noch nicht angemeldet");
			sender.sendData(systemObject, data, dataTime);
		}
		catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
			_manager.addMessage(Message.newMajor("Kann derzeit nicht senden", sendSubscriptionNotConfirmed));
		}
	}

	/**
	 * Meldet einen Sender ab
	 *
	 * @param senderObject Sender-Objekt
	 */
	public void unsubscribeSender(
			final Object senderObject) {
		final InnerSender sender = _senders.remove(senderObject);
		final SystemObject systemObject = getWrappedObject();
		if(systemObject == null) return;
		ConnectionManager.unsubscribeSender(_connection, sender, systemObject, sender.getDataDescription());
	}

	/**
	 * Meldet einen Empfänger ab
	 *
	 * @param receiverObject Empfänger-Objekt
	 */
	public void unsubscribeReceiver(
			final KExDaVReceiver receiverObject) {
		final InnerReceiver receiver = _receivers.remove(receiverObject);
		final SystemObject systemObject = getWrappedObject();
		if(systemObject == null) return;
		ConnectionManager.unsubscribeReceiver(_connection, receiver, systemObject, receiver.getDataDescription());
	}

	/**
	 * Gibt die Datenverteiler-Verbindung zurück, zu der dieses Objekt gehört
	 *
	 * @return Datenverteiler-Verbindung
	 */
	public ClientDavInterface getConnection() {
		return _connection;
	}

	/**
	 * Gibt das SystemObject zurück
	 *
	 * @return SystemObject
	 */
	public synchronized SystemObject getWrappedObject() {
		return _wrappedObject;
	}

	private synchronized void setWrappedObject(final SystemObject wrappedObject) {
		if(_wrappedObject == wrappedObject) return;

		_wrappedObject = wrappedObject;
		if(wrappedObject != null) {
			for(final InnerReceiver receiver : _receivers.values()) {
				if (checkType(wrappedObject.getType(), receiver.getDataDescription().getAttributeGroup())){
					registerReceiver(receiver, wrappedObject);
				}
			}
			for(final InnerSender sender : _senders.values()) {
				if (checkType(wrappedObject.getType(), sender.getDataDescription().getAttributeGroup())){
					registerSender(sender, wrappedObject);
				}
			}
			for(final ExistenceListener listener : _existenceListeners) {
				listener.objectCreated(this);
			}
		}
		else {
			for(final ExistenceListener listener : _existenceListeners) {
				listener.objectInvalidated(this);
			}
		}
	}

	/**
	 * Erstellt einen Listener auf die Erstellung dieses Objekts
	 *
	 * @param e Callback
	 */
	public void addExistenceListener(final ExistenceListener e) {
		_existenceListeners.add(e);
	}

	/**
	 * Erstellt einen Listener auf die Löschung dieses Objekts
	 *
	 * @param e Callback
	 */
	public void removeExistenceListener(final ExistenceListener e) {
		_existenceListeners.remove(e);
	}

	/**
	 * Löscht dieses Objekt
	 *
	 * @param force Soll das Objekt auch gelöscht werden, wenn es nicht von KExDaV kopiert wurde?
	 *
	 * @return true wenn das Objekt nicht mehr existiert, sonst false
	 *
	 * @throws ConfigurationChangeException Falls das Ändern der Konfiguration fehlschlägt (z.B. keine Berechtigung)
	 */
	public boolean invalidate(final boolean force) throws ConfigurationChangeException, MissingKExDaVAttributeGroupException {
		final SystemObject wrappedObject = getWrappedObject();
		if(wrappedObject == null || !wrappedObject.isValid()) {
			return true; // Objekt existiert nicht mehr, es braucht nicht nochmal gelöscht zu werden. Daher ist auch eine Warnung unnötig.
		}
		if(wrappedObject instanceof ConfigurationObject) {
			throw new IllegalArgumentException("Versuch, ein Konfigurationsobjekt zu löschen.");
		}
		if(!force && !isCopy()) return false;
		_manager.addMessage(Message.newInfo("Lösche Objekt: " + _objectSpecification));
		wrappedObject.invalidate();
		setWrappedObject(null);
		return true;
	}

	/**
	 * Liest alle Konfigurationsdaten dieses Objekts
	 *
	 * @return Konfigurationsdaten
	 *
	 * @throws MissingObjectException Falls ein Objekt fehlt (entweder das Systemobjekt, oder die Attributgruppe oder der Aspekt)
	 */
	public Map<PidAttributeGroupUsage, KExDaVAttributeGroupData> getAllConfigurationData() throws MissingObjectException {
		final SystemObject wrappedObject = getWrappedObjectOrThrowException();
		final Map<PidAttributeGroupUsage, KExDaVAttributeGroupData> result = new HashMap<PidAttributeGroupUsage, KExDaVAttributeGroupData>();
		final List<AttributeGroup> attributeGroups = wrappedObject.getType().getAttributeGroups();
		for(final AttributeGroup attributeGroup : attributeGroups) {
			final Collection<Aspect> aspects = attributeGroup.getAspects();
			for(final Aspect aspect : aspects) {
				final Data data = wrappedObject.getConfigurationData(attributeGroup, aspect);
				if(data != null) {
					result.put(new PidAttributeGroupUsage(attributeGroup.getPid(), aspect.getPid()), new KExDaVAttributeGroupData(data, _manager));
				}
			}
		}
		return result;
	}

	private SystemObject getWrappedObjectOrThrowException() throws MissingObjectException {
		final SystemObject wrappedObject = getWrappedObject();
		if(wrappedObject == null) throw new MissingObjectException(_objectSpecification + " konnte nicht gefunden werden.");
		return wrappedObject;
	}

	@Override
	public String toString() {
		final SystemObject wrappedObject = getWrappedObject();
		if(wrappedObject != null) return wrappedObject.getPidOrNameOrId();
		return _objectSpecification.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final KExDaVObject other = (KExDaVObject)o;

		if(!_objectSpecification.equals(other._objectSpecification)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return _objectSpecification.hashCode();
	}

	static Aspect tryGetAspect(final ClientDavInterface connection, final String pid) throws MissingObjectException {
		final SystemObject object = connection.getDataModel().getObject(pid);
		if(object == null) throw new MissingObjectException(pid + " konnte nicht gefunden werden");
		if(object instanceof Aspect) {
			return (Aspect)object;
		}
		throw new MissingObjectException(pid + " ist kein Aspekt");
	}

	static AttributeGroup tryGetAttributeGroup(final ClientDavInterface connection, final String pid) throws MissingObjectException {
		final SystemObject object = connection.getDataModel().getObject(pid);
		if(object == null) throw new MissingObjectException(pid + " konnte nicht gefunden werden");
		if(object instanceof AttributeGroup) {
			return (AttributeGroup)object;
		}
		throw new MissingObjectException(pid + " ist keine Attributgruppe");
	}

	/**
	 * Erstellt dieses Objekt
	 *
	 * @param configurationArea    Konfigurationsbereich
	 * @param pid
	 * @param typePid              Objekt-Typ-Pid
	 * @param objectName           Objekt-name falls vorhanden
	 * @param allConfigurationData Konfigurationsdaten
	 * @param origId
	 * @param origConfigAuthority
	 *
	 * @throws MissingObjectException       Falls der angegebene Typ nicht existiert oder nicht vom Typ DynamicObjectType ist.
	 * @throws ConfigurationChangeException Falls die Konfigurationsänderung nicht durchgeführt werden konnte
	 */
	public void create(
			final ConfigurationArea configurationArea,
			final String pid,
			final String typePid,
			final String objectName,
			final Map<PidAttributeGroupUsage, Data> allConfigurationData,
			final long origId,
			final String origConfigAuthority) throws MissingObjectException, ConfigurationChangeException, MissingKExDaVAttributeGroupException {
		final SystemObject type = _connection.getDataModel().getObject(typePid);
		if(type == null) throw new MissingObjectException(type + " konnte nicht gefunden werden");
		if(!(type instanceof DynamicObjectType)) {
			throw new MissingObjectException(type + " ist kein Typ für dynamische Objekte");
		}
		final Map<PidAttributeGroupUsage, Data> map = new HashMap<PidAttributeGroupUsage, Data>(allConfigurationData);
		if(_connection.getDataModel().getAttributeGroup(Constants.Pids.AttributeGroupKExDaVConfigData) == null){
			throw new MissingKExDaVAttributeGroupException();
		}
		if(origConfigAuthority != null) {
			map.put(
					new PidAttributeGroupUsage(Constants.Pids.AttributeGroupKExDaVConfigData, Constants.Pids.AspectProperties),
					createProperties(origId, origConfigAuthority)
			);
		}
		final Collection<DataAndATGUsageInformation> dataList = convertConfigurationData(map);
		_manager.addMessage(Message.newInfo("Erstelle Objekt: " + (pid.length() == 0 ? '[' + origId + ']' : pid)));
		final DynamicObject dynamicObject = configurationArea.createDynamicObject((DynamicObjectType)type, pid, objectName, dataList);
		setWrappedObject(dynamicObject);
	}

	private Data createProperties(final long origId, final String origConfigAuthority) throws MissingObjectException {
		KExDaVAttributeGroupData data = new KExDaVAttributeGroupData(_connection, Constants.Pids.AttributeGroupKExDaVConfigData, _manager);
		data.getUnscaledValue("originalId").set(origId);
		data.getTextValue("konfigurationsVerantwortlicher").setText(origConfigAuthority);
		data.getTextValue("kexdavObjekt").setText(_manager.getKExDaVObject().getPidOrId());
		return data;
	}

	private Collection<DataAndATGUsageInformation> convertConfigurationData(final Map<PidAttributeGroupUsage, Data> allConfigurationData) {
		final Collection<DataAndATGUsageInformation> dataList = new ArrayList<DataAndATGUsageInformation>();
		for(final Map.Entry<PidAttributeGroupUsage, Data> entry : allConfigurationData.entrySet()) {
			final PidAttributeGroupUsage attributeGroupUsagePids = entry.getKey();
			final SystemObject attributeGroup = _connection.getDataModel().getObject(attributeGroupUsagePids.getAttributeGroupPid());
			final SystemObject aspect = _connection.getDataModel().getObject(attributeGroupUsagePids.getAspectPid());
			if(attributeGroup != null && attributeGroup instanceof AttributeGroup && aspect != null && aspect instanceof Aspect) {
				final AttributeGroupUsage attributeGroupUsage = ((AttributeGroup)attributeGroup).getAttributeGroupUsage((Aspect)aspect);
				if(attributeGroupUsage != null) {
					Data data = entry.getValue();
					if(!(data instanceof KExDaVAttributeGroupData)) {
						data = new KExDaVAttributeGroupData(data, _manager);
					}
					dataList.add(new DataAndATGUsageInformation(attributeGroupUsage, ((KExDaVAttributeGroupData)data).toData(_connection.getDataModel())));
				}
			}
		}
		return dataList;
	}

	/**
	 * Gibt den Typ dieses Objekts zurück
	 *
	 * @return Objekttyp als Pid
	 *
	 * @throws MissingObjectException Falls Objekt nicht existiert
	 */
	public String getType() throws MissingObjectException {
		return getWrappedObjectOrThrowException().getType().getPid();
	}

	/**
	 * Gibt die Spezifikation dieses Objekts zurück
	 *
	 * @return Objekt-Spezifikation
	 */
	public ObjectSpecification getObjectSpecification() {
		return _objectSpecification;
	}

	/**
	 * Gibt den Namen dieses Objekts zurück
	 *
	 * @return Objektname
	 *
	 * @throws MissingObjectException Falls Objekt nicht existiert
	 */
	public String getName() throws MissingObjectException {
		return getWrappedObjectOrThrowException().getName();
	}

	/**
	 * Prüft ob das Objekt existiert
	 *
	 * @return True wenn es existiert
	 */
	public boolean exists() {
		final SystemObject wrappedObject = getWrappedObject();
		return wrappedObject != null && wrappedObject.isValid();
	}

	/**
	 * Prüft, ob das Objekt ein Konfigurationsobjekt ist
	 *
	 * @return True wenn es ein Konfigurationsobjekt ist, false wenn es nicht existiert oder ein dynamisches Objekt ist.
	 */
	public boolean isConfigurationObject() {
		final SystemObject wrappedObject = getWrappedObject();
		return wrappedObject != null && wrappedObject instanceof ConfigurationObject;
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn das Objekt mit diesem KExDaV von einem anderen Datenverteilersystem kopiert wurde, d.h. wenn es also "im Besitz" dieses
	 * KExDaVs ist und damit z.B. auch gelöscht werden darf.
	 *
	 * @return <tt>true</tt>, wenn das Objekt mit diesem KExDaV von einem anderen Datenverteilersystem kopiert wurde, sonst <tt>false</tt>
	 */
	public boolean isCopy() throws MissingKExDaVAttributeGroupException {
		final ExchangeProperties exchangeProperties = getExchangeProperties();
		return exchangeProperties != null
		       && (_manager.getKExDaVObject() != null && _manager.getKExDaVObject().getPidOrId().equals(exchangeProperties.getKExDaVObject()));
	}

	public ExchangeProperties getExchangeProperties() throws MissingKExDaVAttributeGroupException {
		final SystemObject wrappedObject = getWrappedObject();
		if(wrappedObject == null) return null;
		if(wrappedObject instanceof ConfigurationObject) return null;
		return getExchangeProperties(wrappedObject);
	}

	public static ExchangeProperties getExchangeProperties(final SystemObject wrappedObject) throws MissingKExDaVAttributeGroupException {
		final AttributeGroup attributeGroup = wrappedObject.getDataModel().getAttributeGroup(Constants.Pids.AttributeGroupKExDaVConfigData);
		if(attributeGroup == null) throw new MissingKExDaVAttributeGroupException();
		Data data = wrappedObject.getConfigurationData(attributeGroup);
		if(data == null) return null;
		return new ExchangeProperties(data);
	}

	/**
	 * Setzt Konfigurationsdaten
	 *
	 * @param configurationData Konfigurationsdaten
	 *
	 * @throws MissingObjectException       Das Objekt existiert nicht
	 * @throws ConfigurationChangeException Die Konfiguration unterstützt die Änderung nicht
	 */
	public void setConfigurationData(final Map<PidAttributeGroupUsage, Data> configurationData) throws MissingObjectException, ConfigurationChangeException {
		final Collection<DataAndATGUsageInformation> data = convertConfigurationData(configurationData);
		for(final DataAndATGUsageInformation information : data) {
			getWrappedObjectOrThrowException().setConfigurationData(information.getAttributeGroupUsage(), information.getData());
		}
	}

	public String getPid() throws MissingObjectException {
		return getWrappedObjectOrThrowException().getPid();
	}

	public long getId() throws MissingObjectException {
		return getWrappedObjectOrThrowException().getId();
	}

	public String getConfigurationAuthority() throws MissingObjectException {
		return getWrappedObjectOrThrowException().getConfigurationArea().getConfigurationAuthority().getPid();
	}


	private class InnerSender implements ClientSenderInterface {

		private final Object _object;

		private final DataDescription _dataDescription;

		private byte _state = -1;

		private ResultData _lastData = null;

		private final SenderRole _senderRole;

		public InnerSender(final Object object, final DataDescription dataDescription, final SenderRole senderRole) {
			_object = object;
			_dataDescription = dataDescription;
			_senderRole = senderRole;
			_senders.put(object, this);
		}

		public void dataRequest(final SystemObject object, final DataDescription dataDescription, final byte state) {
			if(state == _state) return;
			_state = state;
			if(_state == 0 && _lastData != null) {
				try {
					_connection.sendData(_lastData);
					_lastData = null;
				}
				catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
					_manager.addMessage(Message.newMajor("Kann derzeit nicht senden", sendSubscriptionNotConfirmed));
				}
			}
		}

		public boolean isRequestSupported(final SystemObject object, final DataDescription dataDescription) {
			return true;
		}

		public void sendData(final SystemObject systemObject, Data data, final long dataTime) throws SendSubscriptionNotConfirmed {
			final ResultData resultData;
			if(data != null) {
				if(!(data instanceof KExDaVAttributeGroupData)) {
					data = new KExDaVAttributeGroupData(data, _manager);
				}
				resultData = ((KExDaVAttributeGroupData)data).toResultData(systemObject, _dataDescription, dataTime);
			}
			else {
				resultData = new ResultData(systemObject, _dataDescription, dataTime, null);
			}

			if(_state == 0) {
				_connection.sendData(resultData);
			}
			else {
				_lastData = resultData;
			}
		}

		public SenderRole getSenderRole() {
			return _senderRole;
		}

		public DataDescription getDataDescription() {
			return _dataDescription;
		}

		@Override
		public String toString() {
			return "InnerSender{" + "_object=" + _object + ", _dataDescription=" + _dataDescription + ", _state=" + _state + ", _lastData=" + _lastData
			       + ", _senderRole=" + _senderRole + '}';
		}
	}

	private class InnerReceiver implements ClientReceiverInterface {

		private final KExDaVReceiver _kExDaVReceiver;

		private final ReceiverRole _receiverRole;

		private final DataDescription _dataDescription;

		private final ReceiveOptions _receiveOptions;

		public InnerReceiver(
				final KExDaVReceiver receiver, final ReceiverRole receiverRole, final DataDescription dataDescription, final ReceiveOptions receiveOptions) {
			_kExDaVReceiver = receiver;
			_receiverRole = receiverRole;
			_dataDescription = dataDescription;
			_receiveOptions = receiveOptions;
			_receivers.put(receiver, this);
		}

		public void update(final ResultData[] results) {
			for(final ResultData result : results) {
				_kExDaVReceiver.update(
						result.getData() == null ? null : new KExDaVAttributeGroupData(result.getData(), _manager), result.getDataState(), result.getDataTime()
				);
			}
		}

		public ReceiverRole getReceiverRole() {
			return _receiverRole;
		}

		public DataDescription getDataDescription() {
			return _dataDescription;
		}

		public ReceiveOptions getReceiveOptions() {
			return _receiveOptions;
		}

		@Override
		public String toString() {
			return "InnerReceiver{" + "_kExDavReceiver=" + _kExDaVReceiver + ", _receiverRole=" + _receiverRole + ", _dataDescription=" + _dataDescription
			       + ", _receiveOptions=" + _receiveOptions + '}';
		}
	}

	private class Listener implements InvalidationListener, DynamicObjectType.DynamicObjectCreatedListener {

		public void invalidObject(final DynamicObject dynamicObject) {
			if(dynamicObject.equals(getWrappedObject())) {
				setWrappedObject(null);
			}
		}

		public void objectCreated(final DynamicObject createdObject) {
			if(_objectSpecification.matches(createdObject)) {
				setWrappedObject(createdObject);
			}
		}
	}
}
