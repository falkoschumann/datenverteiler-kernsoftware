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
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.SendDataObject;
import de.bsvrz.dav.daf.communication.lowLevel.TelegramUtility;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.dav.dav.communication.accessControl.AccessControlPlugin;
import de.bsvrz.dav.dav.communication.accessControl.AccessControlUtil;
import de.bsvrz.dav.dav.subscriptions.CommunicationInterface;
import de.bsvrz.dav.dav.subscriptions.LocalReceivingSubscription;
import de.bsvrz.dav.dav.subscriptions.LocalSendingSubscription;
import de.bsvrz.dav.dav.subscriptions.SubscriptionInfo;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diese Klasse verarbeitet ankommende Datentelegramme, prüft ob der Absender erlaubt war die Daten zu verschicken (Anmeldung gültig),
 * wendet eventuelle {@link AccessControlPlugin}s auf die Telegramme an und gibt sie dann an das passende {@link SubscriptionInfo}-Objekt
 * weiter, welches die Telegramme an interessierte Empfänger weiterleitet.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11478 $
 */
public class TelegramManager implements TelegramManagerTransactionInterface {

	private static final Debug _debug = Debug.getLogger();

	private final HighLevelSubscriptionsManager _subscriptionsManager;

	private final HighLevelConnectionsManagerInterface _connectionsManager;

	private final TelegramAggregator<ApplicationDataTelegram> _localTelegramAggregator = new TelegramAggregator<ApplicationDataTelegram>();

	private SelfClientDavConnection _selfClientDavConnection;

	private DavTransactionManager _davTransactionManager;

	private DavDavRequester _davDavRequester = null;

	private final ServerDavParameters.UserRightsChecking _userRightsChecking;

	/** Liste mit Plugins, die den Datenverkehr abfangen und filtern um die Zugriffsrechte zu prüfen */
	private final Collection<AccessControlPlugin> _accessControlPlugins = new ArrayList<AccessControlPlugin>();

	private Map<Long, List<AccessControlPlugin>> _pluginFilterMap;

	private volatile boolean _accessControlPluginsInitialized = false;

	private volatile boolean _pluginFilterMapInitialized = false;


	/**
	 * Erstellt einen neuen TelegramManager
	 * @param connectionsManager Verbindungsverwaltung
	 * @param userRightsChecking  Welche Art von Rechteprüfung durchgeführt werden soll
	 */
	public TelegramManager(final HighLevelConnectionsManagerInterface connectionsManager, final ServerDavParameters.UserRightsChecking userRightsChecking) {
		_connectionsManager = connectionsManager;
		_userRightsChecking = userRightsChecking;
		_subscriptionsManager = new HighLevelSubscriptionsManager(this, _userRightsChecking);
	}

	/**
	 * Initialisiert Funktionen, die von der Konfiguration anhängen (z.B. Rechteprüfung)
	 *
	 * @param selfClientDavConnection eigene Datenverteiler-Verbindung
	 * @param applicationStatusUpdater Klasse, die angemeldete Datensätze verschickt
	 */
	public void setConfigurationAvailable(
			final SelfClientDavConnection selfClientDavConnection, final ApplicationStatusUpdater applicationStatusUpdater) {
		_selfClientDavConnection = selfClientDavConnection;
		_davTransactionManager = new DavTransactionManager(selfClientDavConnection.getConnection(), this);
		_davDavRequester = new DavDavRequester(_selfClientDavConnection.getConnection(), _davTransactionManager, _subscriptionsManager);
		_subscriptionsManager.setConfigurationAvailable(selfClientDavConnection, applicationStatusUpdater);
		initializeAccessControlPlugins(_connectionsManager.getAccessControlPluginsClassNames());
	}

	/**
	 * Initialisiert die angegebenen Zugriffssteuerungs-Plugins, die den Datenverkehr filtern und so zusätzliche Rechteprüfungen vornehmen können
	 *
	 * @param accessControlPlugins Liste mit Plugin-Klassen-Namen
	 */
	private void initializeAccessControlPlugins(final Iterable<String> accessControlPlugins) {
		for(final String pluginClassName : accessControlPlugins) {
			try {
				final Class<?> aClass = Class.forName(pluginClassName);
				final Class<? extends AccessControlPlugin> pluginClass = aClass.asSubclass(AccessControlPlugin.class);
				final AccessControlPlugin plugin = pluginClass.newInstance();
				plugin.initialize(_subscriptionsManager.getAccessControlManager(), _selfClientDavConnection.getConnection());
				_accessControlPlugins.add(plugin);
				_debug.info("Zugriffssteuerungs-Plugin wurde geladen: " + pluginClassName);
			}
			catch(ClassNotFoundException e) {
				_debug.warning("Konnte Plugin nicht finden: " + pluginClassName, e);
			}
			catch(ClassCastException e) {
				_debug.warning("Klasse implementiert Plugin-Interface nicht: " + pluginClassName, e);
			}
			catch(InstantiationException e) {
				_debug.warning("Konnte Plugin-Instanz nicht erstellen (Pluginklasse darf nicht abstrakt oder Interface sein): " + pluginClassName, e);
			}
			catch(IllegalAccessException e) {
				_debug.warning("Konnte Plugin-Instanz nicht erstellen (Konstruktor nicht öffentlich?): " + pluginClassName, e);
			}
		}
		_accessControlPluginsInitialized = true;
	}

	/**
	 * Gibt eine Map zurück, die als Key die AttributeGroupUsage-ID speichert und als Value alle zuständigen Plugins in einer Liste enthält
	 *
	 * @return eine unveränderliche Map vom Typ <code>Map<Long, List<AccessControlPluginInterface>></code> (leer falls die Rechteprüfung deaktivert ist).
	 */
	public Map<Long, List<AccessControlPlugin>> getPluginFilterMap() {
		if(_userRightsChecking != ServerDavParameters.UserRightsChecking.NewDataModel) return Collections.emptyMap();
		if(_accessControlPluginsInitialized && !_pluginFilterMapInitialized) {
			final HashMap<Long, List<AccessControlPlugin>> map = new HashMap<Long, List<AccessControlPlugin>>();
			for(final AccessControlPlugin plugin : _accessControlPlugins) {
				for(final AttributeGroupUsage attributeGroupUsage : plugin.getAttributeGroupUsagesToFilter()) {
					final List<AccessControlPlugin> entry = map.get(attributeGroupUsage.getId());
					if(entry == null) {
						final List<AccessControlPlugin> newEntry = new ArrayList<AccessControlPlugin>();
						newEntry.add(plugin);
						map.put(attributeGroupUsage.getId(), newEntry);
					}
					else {
						entry.add(plugin);
					}
				}
			}
			_pluginFilterMapInitialized = true;
			_pluginFilterMap = Collections.unmodifiableMap(map);
		}
		return _pluginFilterMap == null ? Collections.<Long, List<AccessControlPlugin>>emptyMap() : _pluginFilterMap;
	}

	public HighLevelSubscriptionsManager getSubscriptionsManager() {
		return _subscriptionsManager;
	}

	public HighLevelConnectionsManagerInterface getConnectionsManager() {
		return _connectionsManager;
	}

	/**
	 * Verarbeitet ein eingehendes Datentelegram von einem anderen Datenverteiler
	 * @param communication Verbindung über die das Telegramm eingeht
	 * @param transmitterDataTelegram Telegram
	 */
	public void handleDataTelegram(final CommunicationInterface communication, final TransmitterDataTelegram transmitterDataTelegram) {
		handleDataTelegram(communication, transmitterDataTelegram.getApplicationDataTelegram(), transmitterDataTelegram.getDirection() == 0);
	}

	/**
	 * Verarbeitet ein eingehendes Datentelegram
	 * @param communication Verbindung über die das Telegramm eingeht
	 * @param applicationDataTelegram Telegram
	 * @param toCentralDistributor wenn das Telegramm noch nicht beim Zentraldatenverteiler behandelt wurde, also der Datenindex noch nicht vernünftig gesetzt wurde
	 */
	public void handleDataTelegram(final CommunicationInterface communication, final ApplicationDataTelegram applicationDataTelegram, final boolean toCentralDistributor) {
		final BaseSubscriptionInfo baseSubscriptionInfo = applicationDataTelegram.getBaseSubscriptionInfo();
		final SubscriptionInfo subscriptionInfo = _subscriptionsManager.getSubscriptionInfo(baseSubscriptionInfo);

		// Wenn es zu dem BaseSubscriptionInfo keine Anmeldungen gibt, nichts tun.
		if(subscriptionInfo == null) return;

		// Für Anmeldeumleitungen den zuletzt empfangenen DatenIndex merken
		subscriptionInfo.updatePendingSubscriptionDataIndex(communication, applicationDataTelegram.getDataNumber());

		// Wenn das Telegramm von keinem gültigen Sender gesendet wurde, nichts machen (außer vorher die evtl. vorhandenen Umleitungen aktualisieren)
		if(!subscriptionInfo.isValidSender(communication)){
			return;
		}

		List<ApplicationDataTelegram> telegrams = _localTelegramAggregator.aggregate(applicationDataTelegram, subscriptionInfo);
		if(telegrams.size() == 0) return;

		final List<AccessControlPlugin> plugins = getPluginFilterMap().get(baseSubscriptionInfo.getUsageIdentification());
		if(plugins != null && isValidRemoteUser(communication.getRemoteUserId())) {
			telegrams = AccessControlUtil.handleApplicationDataTelegram(
					telegrams, plugins, communication.getRemoteUserId(), _selfClientDavConnection.getDataModel()
			);
			if(telegrams.size() == 0) return;
		}

		if(subscriptionInfo.isCentralDistributor()) {
			handleTelegramsAsCentralDistributor(telegrams, subscriptionInfo, communication);
		}
		else {
			subscriptionInfo.distributeTelegrams(telegrams, toCentralDistributor, communication);
		}
	}

	/**
	 * Prüft, ob eine userId zu einem "normalen" Benutzer gehört. Rechteprüfungs-Plugins haben u.U. sonst Probleme, weil zu der BenutzerId kein Benutzerobjekt
	 * gehört. Davon abgesehen ist Rechteprüfung für diese Benutzer sowieso normalerweise deaktiviert.
	 * @param remoteUserId Benutzer-Id
	 * @return True wenn normaler benutzer für den Rechte geprüft werden sollen.
	 */
	private boolean isValidRemoteUser(final long remoteUserId) {
		return remoteUserId != _connectionsManager.getTransmitterId() && remoteUserId != 0;
	}

	/**
	 * Verarbeitet Datentelegramem als Zentraldatenverteiler
	 * @param telegrams Aggregierte Liste mit zusammengehörigen Datentelegrammen
	 * @param subscriptionInfo Objekt, das die dazugehörigen Anmeldungen verwaltet und an das die Daten gesendet werden.
	 * @param communication Verbindung über die der Emfang erfolgt (zur Rechteprüfung)
	 */
	private void handleTelegramsAsCentralDistributor(
			final List<ApplicationDataTelegram> telegrams, final SubscriptionInfo subscriptionInfo, final CommunicationInterface communication) {

		//dumpTelegrams(telegrams, communication.getId(), _selfClientDavConnection != null ? _selfClientDavConnection.getDataModel() : null);

		synchronized(subscriptionInfo){
			// Datenindex setzen
			final long dataIndex = _subscriptionsManager.getNextDataIndex(subscriptionInfo);
			for(final ApplicationDataTelegram telegram : telegrams) {
				telegram.setDataIndex(dataIndex);
			}

			// An Empfänger verschicken
			if(_davTransactionManager != null) {
				// Der _davTransactionManager ist erst != null wenn eine Verbindung zur Konfiguration besteht. Vorher können keine Transaktionen benutzt werden.
				List<ApplicationDataTelegram> modifiedTransactionTelegram = _davTransactionManager.handleTelegrams(
						telegrams, subscriptionInfo.hasSource()
				);
				subscriptionInfo.distributeTelegrams(modifiedTransactionTelegram, false, communication);
			}
			else {
				subscriptionInfo.distributeTelegrams(telegrams, false, communication);
			}
		}
	}

	public static void dumpTelegrams(final List<ApplicationDataTelegram> telegrams,  final DafDataModel dataModel) {
		System.out.println("Verarbeite Daten-Telegramm. Länge: " + telegrams.size());
		if(dataModel == null) {
			System.out.println("Daten: Nicht verfügbar.");
			return;
		}
		final AttributeGroup atg = dataModel.getAttributeGroupUsage(
				telegrams.get(0).getBaseSubscriptionInfo().getUsageIdentification()
		).getAttributeGroup();
		System.out.println("Attributgruppe: " + atg);
		try {
			final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(telegrams.toArray(new ApplicationDataTelegram[telegrams.size()]));
			final byte[] dataBytes = sendDataObject.getData();
			if(dataBytes.length == 0) {
				System.out.println("Daten: <" + DataState.getInstance(telegrams.get(0).getErrorFlag() + 1) + ">");
			}
			else {
				String s = DataFactory.forVersion(1).createUnmodifiableData(atg, dataBytes).toString();
				System.out.println("Daten: " + s.substring(0, Math.min(s.length(), 50)));
			}
		}
		catch(Throwable e) {
			System.out.println("Daten: <Fehler: " + e + ">");
		}
		System.out.println();
	}

	@Override
	public void sendTelegramsFromTransaction(final boolean isSource, final ApplicationDataTelegram[] dataTelegrams) {
		final SubscriptionInfo subscriptionInfo = _subscriptionsManager.getSubscriptionInfo(dataTelegrams[0].getBaseSubscriptionInfo());
		if(subscriptionInfo == null){
			return;
		}
		/* Nicht #handleTelegramAsCentralDistributor aufrufen, damit der DatenIndex nicht noch einmal gesetzt wird */
		subscriptionInfo.distributeTelegrams(Arrays.asList(dataTelegrams), false, null);
	}

	/**
	 * Berechnet für eine Anmeldung den nächsten Datenindex und gibt diesen zurück
	 */
	@Override
	public long getNextDataIndex(final BaseSubscriptionInfo info) {
		return _subscriptionsManager.getNextDataIndex(info);
	}

	/**
	 * Benachrichtigt den _davTransactionManager dass eine lokale Anmeldung nicht mehr vorhanden ist. Der Transaktionsmanager meldet daraufhin eventuell vorhandene
	 * innere Datensätze ab.
	 * @param sendingSubscription Sender-Anmeldung
	 */
	public void notifySubscriptionRemoved(final LocalSendingSubscription sendingSubscription) {
		// Der _davTransactionManager ist erst != null wenn eine Verbindung zur Konfiguration besteht
		if(_davTransactionManager == null) return;
		_davTransactionManager.notifyUnsubscribe(sendingSubscription.getBaseSubscriptionInfo(), true);
	}

	/**
	 * Benachrichtigt den _davTransactionManager dass eine lokale Anmeldung nicht mehr vorhanden ist. Der Transaktionsmanager meldet daraufhin eventuell vorhandene
	 * innere Datensätze ab.
	 * @param receivingSubscription Empfänger-Anmeldung
	 */
	public void notifySubscriptionRemoved(final LocalReceivingSubscription receivingSubscription) {
		// Der _davTransactionManager ist erst != null wenn eine Verbindung zur Konfiguration besteht
		if(_davTransactionManager == null) return;
		_davTransactionManager.notifyUnsubscribe(receivingSubscription.getBaseSubscriptionInfo(), false);
	}

	public static byte[] convertTelegramsToBytes(final List<ApplicationDataTelegram> telegrams) {
			final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(telegrams.toArray(new ApplicationDataTelegram[telegrams.size()]));
		return sendDataObject.getData();
	}

	public void notifyIsNewCentralDistributor(final BaseSubscriptionInfo baseSubscriptionInfo) {
			_connectionsManager.updateListsNewLocalSubscription(baseSubscriptionInfo);
	}

	public void notifyWasCentralDistributor(final BaseSubscriptionInfo baseSubscriptionInfo) {
		_connectionsManager.updateListsRemovedLocalSubscription(baseSubscriptionInfo);
	}
}
