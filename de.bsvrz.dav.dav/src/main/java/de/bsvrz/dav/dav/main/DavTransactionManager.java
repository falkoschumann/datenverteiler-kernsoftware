/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dav.dav.
 * 
 * de.bsvrz.dav.dav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.dav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dav.dav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.communication.dataRepresentation.data.DataFactory;
import de.bsvrz.dav.daf.communication.dataRepresentation.data.byteArray.ByteArrayData;
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.SendDataObject;
import de.bsvrz.dav.daf.communication.lowLevel.TelegramUtility;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;
import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Klasse, die Transaktionen auf Dav-Seite verwaltet (Anmeldungen und Abmeldungen der inneren Datenidentifikationen, Einfügen von DataIndizes in innere
 * Datensätze)
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12414 $
 */
public class DavTransactionManager {

	private static final Debug _debug = Debug.getLogger();

	private final ClientDavConnection _connection;

	private final TelegramManagerTransactionInterface _telegramManager;

	private final Map<Long, AttributeGroupUsage> _transactionAttributeGroupUsages;

	/**
	 * Map Transaktionsanmeldung->Innere Anmeldungen für Quellen
	 */
	private final Map<Subscription, List<Subscription>> _sourceSubscriptions = new HashMap<Subscription, List<Subscription>>();

	/**
	 * Map Transaktionsanmeldung->Innere Anmeldungen für Senken
	 */
	private final Map<Subscription, List<Subscription>> _drainSubscriptions = new HashMap<Subscription, List<Subscription>>();

	private final boolean _disabled;

	private final Map<Subscription, InnerTransactionDataSender> _myOwnDataSources = new HashMap<Subscription, InnerTransactionDataSender>();

	private final Map<Subscription, InnerTransactionDataReceiver> _myOwnDataDrains = new HashMap<Subscription, InnerTransactionDataReceiver>();

	/**
	 * Erstellt einen neuen DavTransactionManager
	 * @param connection Verbindung zum Datenverteiler
	 * @param telegramManager SubscriptionsManager
	 */
	public DavTransactionManager(final ClientDavConnection connection, final TelegramManagerTransactionInterface telegramManager) {
		_connection = connection;
		_telegramManager = telegramManager;
		final SystemObjectType transactionType = _connection.getDataModel().getType("typ.transaktion");
		if(transactionType == null){
			_debug.warning("Der Typ für Transaktionsattributgruppen wurde nicht gefunden. Transaktionen sind deaktiviert.");
			_disabled = true;
			_transactionAttributeGroupUsages = null;
			return;
		}
		_disabled = false;
		final Collection<AttributeGroupUsage> list = new ArrayList<AttributeGroupUsage>();
		for(final SystemObject object : transactionType.getObjects()) {
			final AttributeGroup transactionAttributeGroup = (AttributeGroup)object;
			list.addAll(transactionAttributeGroup.getAttributeGroupUsages());
		}
		_transactionAttributeGroupUsages = new HashMap<Long, AttributeGroupUsage>(list.size());
		for(final AttributeGroupUsage attributeGroupUsage : list) {
			_transactionAttributeGroupUsages.put(attributeGroupUsage.getId(), attributeGroupUsage);
		}
	}

	/**
	 * Wird vom Subscriptionsmanager aufgerufen, wenn ein Telegram im Zentraldatenverteiler verarbeitet wird. Hier wird geprüft, ob es sich um ein
	 * Transaktionstelegramm handelt und es werden entsprechende Aktionen durchgeführt (Datenindex eintragen, innere Telegramme an herkömmliche Empfänger
	 * verschicken)
	 *
	 * @param telegrams Liste mit zusammengehörigen Telegrammen, die einen Datensatz darstellen
	 * @param isSource  Kommt der Datensatz von der lokalen Quelle? (Sonst lokale Senke)
	 *
	 * @return eine neue Liste mit Telegrammen wenn diese verändert wurden, sonst der telegrams-parameter.
	 */
	public List<ApplicationDataTelegram> handleTelegrams(final List<ApplicationDataTelegram> telegrams, final boolean isSource) {
		if(_disabled) return telegrams;
		final BaseSubscriptionInfo baseSubscriptionInfo = telegrams.get(0).getBaseSubscriptionInfo();
		final AttributeGroupUsage attributeGroupUsage = _transactionAttributeGroupUsages.get(baseSubscriptionInfo.getUsageIdentification());
		if(attributeGroupUsage == null) {
			// Nicht in HashMap, es ist also keine Transaktion
			return telegrams;
		}
		final AttributeGroup attributeGroup = attributeGroupUsage.getAttributeGroup();
		final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(telegrams.toArray(new ApplicationDataTelegram[telegrams.size()]));
		if(sendDataObject.getErrorFlag() == 0) {
			final Data data = DataFactory.forVersion(1).createUnmodifiableData(attributeGroup, sendDataObject.getData()).createModifiableCopy();
			for(final Data dataset : data.getItem("Transaktion").getItem("Datensatz")) {
				handleDataset(dataset, baseSubscriptionInfo.getSimulationVariant(), isSource);
			}
			final ApplicationDataTelegram[] changedTelegrams = dataToTelegrams(sendDataObject, data);
			return Collections.unmodifiableList(Arrays.asList(changedTelegrams));
		}
		return telegrams;
	}

	/**
	 * Verarbeitet einen inneren Datensatz als Zentraldatenverteiler
	 * @param dataset Inneren Datensatz
	 * @param simulationVariant Simulationsvariante der Übertragung
	 * @param isSource Handelt es sich um eine Quelle? (Sonst Senke)
	 */
	private void handleDataset(final Data dataset, final short simulationVariant, final boolean isSource) {
		final Data dataIdentification = dataset.getItem("Datenidentifikation");
		final SystemObject object = dataIdentification.getReferenceValue("Objekt").getSystemObject();
		final AttributeGroup attributeGroup = (AttributeGroup)dataIdentification.getReferenceValue("Attributgruppe").getSystemObject();
		final Aspect aspect = (Aspect)dataIdentification.getReferenceValue("Aspekt").getSystemObject();
		final long dataTime = dataset.getTimeValue("Datenzeit").getMillis();
		final byte[] dataBytes = dataset.getUnscaledArray("Daten").getByteArray();

		final long dataIndex = _telegramManager.getNextDataIndex(
				new Subscription(object, attributeGroup, aspect, simulationVariant).getBaseSubscriptionInfo()
		);
		dataset.getUnscaledValue("Datenindex").set(dataIndex);
		if(isSource) {
			// Wenn Quelle, innere Datensätze zusätzlich selbst verschicken
			final BaseSubscriptionInfo info = new BaseSubscriptionInfo(
					object.getId(), attributeGroup.getAttributeGroupUsage(aspect).getId(), simulationVariant
			);

			_telegramManager.sendTelegramsFromTransaction(
					true,
					createTelegramsFromBytes(
							dataBytes.length == 0 ? null : dataBytes, info, false, dataIndex, dataTime,
							dataBytes.length == 0 ? (byte) 1 : (byte) 0, null
					)
			);
		}
	}

	/**
	 * Wird von der Senke aufgerufen, die sich im Datenverteiler auf die inneren Datensätze von einer Transaktions-Senken-Anmeldung angemeldet hat. Die Funktion
	 * sorgt dafür, dass der innere Datensatz in einem Transaktionsdatensatz verpackt wird und an die Transaktionssenke übermittelt wird.
	 *
	 * @param result                  ResultData aus dem inneren Datensatz
	 * @param transactionSubscription Anmeldung der Transaktionssenke
	 */
	public void handleIncomingDrainData(final ResultData result, final Subscription transactionSubscription) {
		try {
			// Hier an der Transaktions-API vorbei senden, da auch bei der Anmeldung die API umgangen wurde.
			// Daher wird hier nicht mehr auf enthaltene Datensätze usw. geprüft.
			_connection.sendData(
					new TransactionResultData(
							new TransactionDataDescription(transactionSubscription.getObject(), transactionSubscription.getDataDescription()),
							Arrays.asList(result),
							false,
							result.getDataTime()
					).getResultData(_connection)
			);
		}
		catch(SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
			_debug.warning("Konnte Transaktionsdatensatz nicht senden", sendSubscriptionNotConfirmed);
		}
	}

	/**
	 * Hilfsfunktion, die ein ApplicationDataTelegram-Array in ein TransmitterDataTelegram-Array umwandelt
	 * @param applicationDataTelegrams ApplicationDataTelegram-Array
	 * @return TransmitterDataTelegram-Array
	 */
	private TransmitterDataTelegram[] applicationTelegramsToTransmitterTelegrams(final ApplicationDataTelegram[] applicationDataTelegrams) {
		final TransmitterDataTelegram[] transmitterDataTelegrams = new TransmitterDataTelegram[applicationDataTelegrams.length];
		for(int i = 0; i < applicationDataTelegrams.length; i++) {
			transmitterDataTelegrams[i] = new TransmitterDataTelegram(applicationDataTelegrams[i], (byte)1);
		}
		return transmitterDataTelegrams;
	}

	/**
	 * Hinfsfunktion, die ein Data-Objekt in Telgramme zerlegt.
	 * @param sendDataObject Ursprüngliches SendData-Objekt zur Rekonstruktion der Informationen
	 * @param data Data-Objekt mit den neuen Daten
	 * @return ApplicationDataTelegram-Array
	 */
	private static ApplicationDataTelegram[] dataToTelegrams(final SendDataObject sendDataObject, final Data data) {
		final Data unmodifiableCopy = data.createUnmodifiableCopy();
		if(unmodifiableCopy instanceof ByteArrayData) {

			final ByteArrayData byteArrayData = (ByteArrayData)unmodifiableCopy;
			final byte[] bytes = byteArrayData.getBytes();
			
			final BaseSubscriptionInfo baseSubscriptionInfo = sendDataObject.getBaseSubscriptionInfo();
			final boolean dalayedDataFlag = sendDataObject.getDalayedDataFlag();
			final long dataIndex = sendDataObject.getDataNumber();
			final long dataTime = sendDataObject.getDataTime();
			final byte errorFlag = sendDataObject.getErrorFlag();
			final byte[] attributesIndicator = sendDataObject.getAttributesIndicator();
			
			return createTelegramsFromBytes(bytes, baseSubscriptionInfo, dalayedDataFlag, dataIndex, dataTime, errorFlag, attributesIndicator);
		}
		else {
			throw new IllegalArgumentException("Daten können nicht serialisiert werden: " + data.getClass().getName());
		}
	}

	/**
	 * Erstellt aus Daten-Bytes ein ApplicationDataTelegram-Array
	 * @param bytes Daten-Bytes
	 * @param baseSubscriptionInfo Anmelde-info
	 * @param delayedDataFlag Flag für nachgelieferte Daten
	 * @param dataIndex DatenIndex
	 * @param dataTime DatenZeit
	 * @param errorFlag Fehler-Flag
	 * @param attributesIndicator attributesIndicator oder null
	 * @return ApplicationDataTelegram-Array
	 */
	private static ApplicationDataTelegram[] createTelegramsFromBytes(
			final byte[] bytes,
			final BaseSubscriptionInfo baseSubscriptionInfo,
			final boolean delayedDataFlag,
			final long dataIndex,
			final long dataTime,
			final byte errorFlag,
			final byte[] attributesIndicator) {
		final SendDataObject newSendDataObject = new SendDataObject(
				baseSubscriptionInfo, delayedDataFlag, dataIndex, dataTime, errorFlag, attributesIndicator, bytes
		);
		return TelegramUtility.splitToApplicationTelegrams(newSendDataObject);
	}

	/**
	 * Wird vom DavRequester aufgerufen um eine Transaktionsquelle anzumelden
	 * @param bytes Serialisierte Info über Datenanmeldung
	 * @throws IOException
	 * @throws OneSubscriptionPerSendData
	 */
	public synchronized void handleSubscribeTransactionSource(final byte[] bytes) throws IOException, OneSubscriptionPerSendData {
		final ClientSubscriptionInformation subscriptions = deserializeSubscriptionBytes(bytes);
		handleSubscribeTransactionSource(subscriptions);
	}

	/**
	 * Wird vom DavRequester aufgerufen um eine Transaktionsquelle anzumelden
	 * @param subscriptions Anmeldeinformation
	 * @throws OneSubscriptionPerSendData  Fehler bei der Anmeldung (Z.B. es ist schon eine Senke vorhanden)
	 */
	public void handleSubscribeTransactionSource(final ClientSubscriptionInformation subscriptions) throws OneSubscriptionPerSendData {
		if(_disabled) throw new IllegalStateException("Der Datenverteiler unterstützt keine Transaktionen");
		final Subscription transactionSubscription = subscriptions.getTransactionSubscription();
		addSubscription(_sourceSubscriptions, transactionSubscription, subscriptions.getInnerSubscriptions());

		// Empfänger anmelden um Sendesteuerung beim Client zu starten
		final InnerTransactionDataReceiver receiver = new InnerTransactionDataReceiver(null);
		_connection.subscribeReceiver(
				receiver, transactionSubscription.getObject(), transactionSubscription.getDataDescription(), ReceiveOptions.normal(), ReceiverRole.receiver()
		);
		_myOwnDataDrains.put(transactionSubscription, receiver);

		try {
			for(final Subscription subscription : subscriptions.getInnerSubscriptions()) {
				final InnerTransactionDataSender sender = new InnerTransactionDataSender();
				_connection.subscribeSender(
						sender, subscription.getObject(), subscription.getDataDescription(), SenderRole.source()
				);
				_myOwnDataSources.put(subscription, sender);
			}
		}
		catch(OneSubscriptionPerSendData e) {
			unsubscribeMySources(subscriptions.getInnerSubscriptions());
			throw e;
		}
	}

	/**
	 * Wird vom DavRequester aufgerufen um eine Transaktionssenke anzumelden
	 * @param bytes Serialisierte Info über Datenanmeldung
	 * @throws IOException
	 * @throws OneSubscriptionPerSendData
	 */
	public synchronized void handleSubscribeTransactionDrain(final byte[] bytes) throws IOException, OneSubscriptionPerSendData {
		final ClientSubscriptionInformation subscriptions = deserializeSubscriptionBytes(bytes);
		handleSubscribeTransactionDrain(subscriptions);
	}

	/**
	 * Wird vom DavRequester aufgerufen um eine Transaktionssenke anzumelden
	 * @param subscriptions Anmeldeinformation

	 * @throws OneSubscriptionPerSendData Fehler bei der Anmeldung (Z.B. es ist schon eine Senke vorhanden)
	 */
	public void handleSubscribeTransactionDrain(final ClientSubscriptionInformation subscriptions) throws OneSubscriptionPerSendData {
		if(_disabled) throw new IllegalStateException("Der Datenverteiler unterstützt keine Transaktionen");
		final Subscription transactionSubscription = subscriptions.getTransactionSubscription();
		addSubscription(_drainSubscriptions, transactionSubscription, subscriptions.getInnerSubscriptions());

		// Sender anmelden um eigene Transaktionen zu verschicken
		final InnerTransactionDataSender sender = new InnerTransactionDataSender();
		_connection.subscribeSender(
				sender, transactionSubscription.getObject(), transactionSubscription.getDataDescription(), SenderRole.sender()
		);
		_myOwnDataSources.put(transactionSubscription, sender);

		// Senken auf innere Daten anmelden
		try {
			for(final Subscription subscription : subscriptions.getInnerSubscriptions()) {
				final InnerTransactionDataReceiver receiver = new InnerTransactionDataReceiver(transactionSubscription);
				_connection.subscribeReceiver(
						receiver, subscription.getObject(), subscription.getDataDescription(), ReceiveOptions.normal(), ReceiverRole.drain()
				);
				_myOwnDataDrains.put(subscription, receiver);
			}
		}
		catch(Exception e) {
			unsubscribeMySources(subscriptions.getInnerSubscriptions());
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Fügt entweder den Quell oder den Senken-Anmeldungen eine Transaktions-Anmeldung hinzu
	 * @param subscriptionListMap Liste, in die die Anmeldung hinzugefügt werden soll
	 * @param transactionSubscription Transaktionsanmeldung
	 * @param innerSubscriptions Innere Anmeldungen
	 * @throws OneSubscriptionPerSendData Es gibt bereits eine Anmeldung für diese Transaktion
	 */
	private void addSubscription(
			final Map<Subscription, List<Subscription>> subscriptionListMap, final Subscription transactionSubscription, final List<Subscription> innerSubscriptions)
			throws OneSubscriptionPerSendData {
		final List<Subscription> item = subscriptionListMap.get(transactionSubscription);
		if(item != null) throw new OneSubscriptionPerSendData("Es gibt bereits eine Anmeldung für diese Transaktion.");
		subscriptionListMap.put(transactionSubscription, innerSubscriptions);
	}

	/**
	 * Hilfsfunktion, die das Bytearray, was vom Client kommt und die Anmeldedaten für eine Transaktion enthält, deserialisiert
	 * @param bytes Bytearray
	 * @return Subscriptions-Objekt
	 * @throws IOException Sollte nicht auftreten
	 */
	private ClientSubscriptionInformation deserializeSubscriptionBytes(final byte[] bytes) throws IOException {
		final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
		try{
			final int size = dataInputStream.readInt();
			final long objectId = dataInputStream.readLong();
			final long atgUsage = dataInputStream.readLong();
			final short simulationVariant = dataInputStream.readShort();
			final List<InnerDataSubscription> list = new ArrayList<InnerDataSubscription>(size);
			for(int i = 0; i < size; i++){
				final DataModel dataModel = _connection.getDataModel();
				list.add(new InnerDataSubscription(
						dataModel.getObject(dataInputStream.readLong()),
				        (AttributeGroup)dataModel.getObject(dataInputStream.readLong()),
				        (Aspect)dataModel.getObject(dataInputStream.readLong())
						));
			}
			return new ClientSubscriptionInformation(new Subscription(new BaseSubscriptionInfo(objectId, atgUsage, simulationVariant)), list);
		}
		catch(IOException e) {
			throw new IllegalArgumentException("Ungültiges Byte-Array", e);
		}
		finally {
			dataInputStream.close();
		}
	}

	/**
	 * Wird vom Subscriptionsmanager aufgerufen, um zu signalisieren, dass eine Datenanmeldung ungültig geworden ist. Daraufhin werden hier alle
	 * Transaktionsanmeldungen und Anmeldungen der inneren Daten zu dieser Transaktion entfernt.
	 *
	 * @param baseSubscriptionInfo Datenanmelde-Information
	 * @param isSender handelt es sich um eine sendende Anmeldung?
	 */
	public synchronized void notifyUnsubscribe(final BaseSubscriptionInfo baseSubscriptionInfo, final boolean isSender) {
		if(_disabled) return;
		final long usageIdentification = baseSubscriptionInfo.getUsageIdentification();
		if(_transactionAttributeGroupUsages.containsKey(usageIdentification)) {
			final Subscription subscription = new Subscription(baseSubscriptionInfo);
			if(isSender) {
				// Transaktionsempfänger für Sendesteuerung abmelden
				final InnerTransactionDataReceiver receiver = _myOwnDataDrains.remove(subscription);
				if(receiver != null) {
					_connection.unsubscribeReceiver(receiver, subscription.getObject(), subscription.getDataDescription());
				}
				
				// Quellen für innere Daten abmelden
				final List<Subscription> subscriptions = _sourceSubscriptions.remove(subscription);
				if(subscriptions != null) unsubscribeMySources(subscriptions);
			}
			else {
				// Transaktionssender abmelden
				final InnerTransactionDataSender sender = _myOwnDataSources.remove(subscription);
				if(sender != null) {
					_connection.unsubscribeSender(sender, subscription.getObject(), subscription.getDataDescription());
				}

				// Senken für innere Daten abmelden
				final List<Subscription> subscriptions = _drainSubscriptions.remove(subscription);
				if(subscriptions != null) unsubscribeMyDrains(subscriptions);
			}
		}
	}

	/**
	 * Meldet alle Quellen ab, die in der Liste sind
	 * @param subscriptions Liste mit Quell-Anmeldungen
	 */
	private void unsubscribeMySources(final List<Subscription> subscriptions) {
		for(final Subscription subscription : subscriptions) {
			final InnerTransactionDataSender sender = _myOwnDataSources.remove(subscription);
			if(sender != null) {
				_connection.unsubscribeSender(sender, subscription.getObject(), subscription.getDataDescription());
			}
		}
	}

	/**
	 * Meldet alle Senken ab, die in der Liste sind
	 * @param subscriptions Liste mit Senken-Anmeldungen
	 */
	private void unsubscribeMyDrains(final List<Subscription> subscriptions) {
		for(final Subscription subscription : subscriptions) {
			final InnerTransactionDataReceiver receiver = _myOwnDataDrains.remove(subscription);
			if(receiver != null) {
				_connection.unsubscribeReceiver(receiver, subscription.getObject(), subscription.getDataDescription());
			}
		}
	}

	/**
	 * Objekt dass die Anmeldeinformation für Transaktionssenken/quellen vom Client kapselt
	 */
	private final class ClientSubscriptionInformation {

		private final List<Subscription> _list = new ArrayList<Subscription>();

		private final DavTransactionManager.Subscription _transactionSubscription;

		/**
		 * Ersteltl eine neue ClientSubscriptionInformation
		 * @param transactionSubscription Transaktionsanmeldung
		 * @param list Innere Anmeldungen
		 */
		public ClientSubscriptionInformation(final Subscription transactionSubscription, final List<InnerDataSubscription> list) {
			_transactionSubscription = transactionSubscription;
			for(final InnerDataSubscription innerDataSubscription : list) {
				_list.add(
						new Subscription(
								innerDataSubscription.getObject(),
								innerDataSubscription.getAttributeGroup(),
								innerDataSubscription.getAspect(),
								transactionSubscription.getSimulationVariant()
						)
				);
			}
		}

		/**
		 * Gibt eine Liste mit inneren Anmeldungen zurück
		 * @return eine Liste mit inneren Anmeldungen
		 */
		public List<Subscription> getInnerSubscriptions() {
			return Collections.unmodifiableList(_list);
		}

		/**
		 * Gibt die Transaktionsanmeldung zurück
		 * @return Transaktionsanmeldung
		 */
		public Subscription getTransactionSubscription() {
			return _transactionSubscription;
		}

		@Override
		public String toString() {
			return "Subscriptions{" + "_list=" + _list + ", _transactionSubscription=" + _transactionSubscription + '}';
		}
	}

	/**
	 * Kapselt eine Datenanmeldungs-Beschreibung
	 */
	final class Subscription {
		private final SystemObject _object;
		private final AttributeGroup _attributeGroup;
		private final Aspect _aspect;
		private final short _simulationVariant;

		/**
		 * Erstellt eine neue Anmeldungs-Beschreibung
		 * @param baseSubscriptionInfo Anmelde-Information
		 */
		private Subscription(final BaseSubscriptionInfo baseSubscriptionInfo) {
			_object = _connection.getDataModel().getObject(baseSubscriptionInfo.getObjectID());
			final AttributeGroupUsage attributeGroupUsage = _connection.getDataModel().getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification());
			_attributeGroup = attributeGroupUsage.getAttributeGroup();
			_aspect = attributeGroupUsage.getAspect();
			_simulationVariant = baseSubscriptionInfo.getSimulationVariant();
		}

		/**
		 * Erstellt eine neue Anmeldungs-Beschreibung
		 * @param object Objekt
		 * @param attributeGroup Attributgruppe
		 * @param aspect Aspekt
		 * @param simulationVariant Simulationsvariante
		 */
		private Subscription(final SystemObject object, final AttributeGroup attributeGroup, final Aspect aspect, final short simulationVariant) {
			_object = object;
			_attributeGroup = attributeGroup;
			_aspect = aspect;
			_simulationVariant = simulationVariant;
		}

		/**
		 * Gibt das Objekt zurück
		 * @return Objekt
		 */
		public SystemObject getObject() {
			return _object;
		}

		/**
		 * Gibt die Attributgruppe zurück
		 * @return Attributgruppe
		 */
		public AttributeGroup getAttributeGroup() {
			return _attributeGroup;
		}

		/**
		 * Gibt den Aspekt zurück
		 * @return Aspekt
		 */
		public Aspect getAspect() {
			return _aspect;
		}

		/**
		 * Gibt die Simulationsvariante zurück
		 * @return Simulationsvariante
		 */
		public short getSimulationVariant() {
			return _simulationVariant;
		}

		/**
		 * Gibt die Attributgruppenverwendung zurück
		 * @return Attributgruppenverwendung
		 */
		public AttributeGroupUsage getAttributeGroupUsage() {
			return _attributeGroup.getAttributeGroupUsage(_aspect);
		}

		/**
		 * Gibt die DataDescription zurück
		 * @return DataDescription
		 */
		public DataDescription getDataDescription() {
			return new DataDescription(_attributeGroup, _aspect, _simulationVariant);
		}

		/**
		 * Gibt ein äquivalentes BaseSubscriptionInfo zurück
		 * @return BaseSubscriptionInfo
		 */
		public BaseSubscriptionInfo getBaseSubscriptionInfo() {
			return new BaseSubscriptionInfo(_object.getId(),  getAttributeGroupUsage().getId(), _simulationVariant);
		}

		@Override
		public String toString() {
			return "Subscription{" + "_object=" + _object + ", _attributeGroup=" + _attributeGroup + ", _aspect=" + _aspect + ", _simulationVariant="
			       + _simulationVariant + '}';
		}

		@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "RedundantIfStatement"})
		@Override
		public boolean equals(final Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			final Subscription other = (Subscription)o;

			if(_simulationVariant != other._simulationVariant) return false;
			if(_aspect != null ? !_aspect.equals(other._aspect) : other._aspect != null) return false;
			if(_attributeGroup != null ? !_attributeGroup.equals(other._attributeGroup) : other._attributeGroup != null) return false;
			if(_object != null ? !_object.equals(other._object) : other._object != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = _object != null ? _object.hashCode() : 0;
			result = 31 * result + (_attributeGroup != null ? _attributeGroup.hashCode() : 0);
			result = 31 * result + (_aspect != null ? _aspect.hashCode() : 0);
			result = 31 * result + (int)_simulationVariant;
			return result;
		}
	}

	/**
	 * Sender macht nichts relevates, Sendersteuerung interessiert nicht, da die Daten ohnehin manuell gesendet werden. Dient nur zur Anmeldung.
	 */
	private static class InnerTransactionDataSender implements ClientSenderInterface {

		@Override
		public void dataRequest(final SystemObject object, final DataDescription dataDescription, final byte state) {
		}

		@Override
		public boolean isRequestSupported(final SystemObject object, final DataDescription dataDescription) {
			return false;
		}
	}

	/**
	 * Wird benutzt um sich als Senke auf einen inneren Datensatz einer Transaktions-Senke anzumelden. Ankommende Datensätze werden an den DavTransactionManager
	 * weitergegeben und dort in einen Transaktionsdatensatz verpackt. Falls _transactionSubscription null ist wird der Empfänger nur für das Triggern der
	 * Sendesteuerung gebraucht und ankommende Daten interessieren nicht.
	 */
	private class InnerTransactionDataReceiver implements ClientReceiverInterface {

		private final Subscription _transactionSubscription;

		public InnerTransactionDataReceiver(final Subscription transactionSubscription) {
			_transactionSubscription = transactionSubscription;
		}

		@Override
		public void update(final ResultData[] results) {
			if(_transactionSubscription == null) return;
			for(final ResultData result : results) {
				// Es ergibt keinen Sinn, "Keine Quelle"-Datensätze in Transaktiosndatensätze zu verpacken und an die Senke zu senden.
				if(result.getDataState() != DataState.NO_SOURCE) handleIncomingDrainData(result, _transactionSubscription);
			}
		}

		@Override
		public String toString() {
			return "InnerTransactionDataReceiver{" + "_transactionSubscription=" + _transactionSubscription + '}';
		}
	}
}
