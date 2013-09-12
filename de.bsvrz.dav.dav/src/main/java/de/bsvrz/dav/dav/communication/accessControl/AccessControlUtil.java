/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.communication.accessControl;

import de.bsvrz.dav.daf.communication.dataRepresentation.data.DataFactory;
import de.bsvrz.dav.daf.communication.dataRepresentation.data.byteArray.ByteArrayData;
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.SendDataObject;
import de.bsvrz.dav.daf.communication.lowLevel.TelegramUtility;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.dav.main.ConnectionsManager;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasse, die Hilfsfunktionen zum Filtern von Datenpaketen bereitstellt. Mit {@link #processTelegramByPlugins(de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram,
 * java.util.Collection, long, de.bsvrz.dav.dav.main.ConnectionsManager) } soll ein ankommendes Telegram und eine Liste mit Plugins übergeben werden. Diese
 * Funktion setzt die Telegramme bei Bedarf zusammen, erstellt daraus ein Data-Objekt, übergibt dieses den Plugins, und macht aus der Rückgabe der Plugins
 * wieder ein Array aus Telegrammen. Diese Klasse verwendet Telegramme vom Typ ApplicationDataTelegram. Soll ein TransmitterDataTelegram benutzt werden ist
 * dieses vorher mit {@link de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram#getApplicationDataTelegram() } zu konvertieren.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public class AccessControlUtil {

	/** Map in der eine je Datenidentifikation eine Liste von empfangenen Telegrammen, die zu einem Datensatz gehören zwischengespeichert werden können */
	private final Map<BaseSubscriptionInfo, List<ApplicationDataTelegram>> _stalledTelegramListMap = new HashMap<BaseSubscriptionInfo, List<ApplicationDataTelegram>>();

	/** Debug-Klasse */
	private final Debug _debug = Debug.getLogger();

	/**
	 * Verarbeitet ankommende Telegramme. Siehe {@link AccessControlUtil}
	 *
	 * @param applicationDataTelegram    Ankommendes Telegramm. Für mehrere Telegramme muss die Funktion entsprechend mehrmals aufgerufen werden.
	 * @param controlPluginInterfaceList Liste mit Plugins, die für diesen Telegramtyp/Attributgruppenverwendung zuständig sind
	 * @param userId                     der absendende Benutzer
	 * @param connectionsManager         connectionsManager
	 *
	 * @return die von den Plugins verarbeiteten Telegramme (kann ein leeres Array sein falls applicationDataTelegram nur ein Teiltelegram war, oder die Plugins
	 *         das Löschen des Telegrams anforderten)
	 */
	public ApplicationDataTelegram[] processTelegramByPlugins(
			final ApplicationDataTelegram applicationDataTelegram,
			final Collection<AccessControlPlugin> controlPluginInterfaceList,
			final long userId,
			final ConnectionsManager connectionsManager) {
		final int totalTelegramsCount = applicationDataTelegram.getTotalTelegramsCount();
		if(totalTelegramsCount == 1) {
			// Nicht gestückelte Telegramme: Filtern und zurückgeben
			return filterApplicationDataTelegram(applicationDataTelegram, controlPluginInterfaceList, userId, connectionsManager);
		}
		else {
			final BaseSubscriptionInfo info = applicationDataTelegram.getBaseSubscriptionInfo();

			// Gestückelte Telegramme, die nicht von einer Quelle kommen
			// Es wird unterschieden nach dem ersten, einem mittleren und dem letzen Telegramm eines zerstückelten Datensatzes
			final int telegramNumber = applicationDataTelegram.getTelegramNumber();
			if(telegramNumber == 0) {
				// Das erste Telegramm eines zerstückelten Datensatzes
				final List<ApplicationDataTelegram> stalledTelegramList = createStalledTelegramList(info, totalTelegramsCount);
				stalledTelegramList.add(applicationDataTelegram);
			}
			else {
				// Nicht das erste Telegramm eines zerstückelten Datensatzes
				if(telegramNumber + 1 != totalTelegramsCount) {
					// Ein mittleres Telegramm eines zerstückelten Datensatzes
					final List<ApplicationDataTelegram> stalledTelegramList = getStalledTelegramList(info);
					if(stalledTelegramList == null) {
						_debug.warning(
								"Ein mittleres Telegramm ist ohne erstes Telegramm eingetroffen", info
						);
					}
					else {
						stalledTelegramList.add(applicationDataTelegram);
					}
				}
				else {
					// Das letzte Telegramm eines zerstückelten Datensatzes
					final List<ApplicationDataTelegram> stalledTelegramList = deleteStalledTelegramList(info);
					if(stalledTelegramList == null) {
						_debug.warning(
								"Das letzte Telegramm ist ohne vorherige Telegramme eingetroffen", info
						);
					}
					else {
						stalledTelegramList.add(applicationDataTelegram);
						return filterApplicationDataTelegram(
								stalledTelegramList.toArray(new ApplicationDataTelegram[stalledTelegramList.size()]),
								controlPluginInterfaceList,
								userId,
								connectionsManager
						);
					}
				}
			}
			return new ApplicationDataTelegram[0];
		}
	}

	/**
	 * Verarbeitet ein Telegramm durch die Plugins
	 *
	 * @param applicationDataTelegram Telegram
	 * @param accessControlPlugins    Plugins
	 * @param userId                  Benutzer
	 * @param connectionsManager      ConnectionsManager
	 *
	 * @return Verarbeitete Telegramme
	 */
	private ApplicationDataTelegram[] filterApplicationDataTelegram(
			final ApplicationDataTelegram applicationDataTelegram,
			final Collection<AccessControlPlugin> accessControlPlugins,
			final long userId,
			final ConnectionsManager connectionsManager) {
		final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(applicationDataTelegram);
		final BaseSubscriptionInfo baseSubscriptionInfo = applicationDataTelegram.getBaseSubscriptionInfo();
		final Data initialData = createData(sendDataObject, baseSubscriptionInfo, connectionsManager);
		final Data data = processDataByPlugins(initialData, accessControlPlugins, baseSubscriptionInfo, userId);
		if(initialData == data) {
			// unverändert, einfach wieder Parameter zurückgeben
			return new ApplicationDataTelegram[]{applicationDataTelegram};
		}
		// Daten-Objekt vom Plugin wieder in ApplicationData-Telegramme zerlegen
		return dataToTelegrams(sendDataObject, data);
	}

	/**
	 * Verarbeitet mehrere zusammengehörige Telegramme durch die Plugins
	 *
	 * @param telegrams            Telegramme
	 * @param accessControlPlugins Plugins
	 * @param userId               Benutzer
	 * @param connectionsManager   ConnectionsManager
	 *
	 * @return Verarbeitete Telegramme
	 */
	private ApplicationDataTelegram[] filterApplicationDataTelegram(
			final ApplicationDataTelegram[] telegrams,
			final Collection<AccessControlPlugin> accessControlPlugins,
			final long userId,
			final ConnectionsManager connectionsManager) {
		final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(telegrams);
		final BaseSubscriptionInfo baseSubscriptionInfo = telegrams[0].getBaseSubscriptionInfo();
		final Data initialData = createData(sendDataObject, baseSubscriptionInfo, connectionsManager);
		final Data data = processDataByPlugins(initialData, accessControlPlugins, baseSubscriptionInfo, userId);
		if(initialData == data) {
			// unverändert, einfach wieder Parameter zurückgeben
			return telegrams;
		}
		// Daten-Objekt vom Plugin wieder in ApplicationData-Telegramme zerlegen
		return dataToTelegrams(sendDataObject, data);
	}

	/**
	 * Verarbeitet ein Data-Objekt durch die Plugins
	 *
	 * @param initialData          Data-Objekt
	 * @param plugins              Plugins
	 * @param baseSubscriptionInfo BaseSubscriptionInfo
	 * @param userId               Benutzer-ID
	 *
	 * @return Verarbeitetes Data-Objekt oder null falls die Plugins anfordern, das Datenobjekt zu verwerfen
	 */
	private Data processDataByPlugins(
			final Data initialData, final Collection<AccessControlPlugin> plugins, final BaseSubscriptionInfo baseSubscriptionInfo, final long userId) {
		Data data = initialData;
		for(final AccessControlPlugin plugin : plugins) {
			data = plugin.handleData(userId, baseSubscriptionInfo, data);
			if(data == null) return null;
		}
		return data;
	}

	/**
	 * Wandelt eine Data-Objekt in Telegramme um
	 *
	 * @param originalSendDataObject Originales SendDataObject-Objekt
	 * @param data                   neues Data-Objekt
	 *
	 * @return Telegramme
	 */
	private ApplicationDataTelegram[] dataToTelegrams(final SendDataObject originalSendDataObject, final Data data) {
		if(data == null) {
			return new ApplicationDataTelegram[]{};
		}
		final Data unmodifiableCopy = data.createUnmodifiableCopy();
		if(unmodifiableCopy instanceof ByteArrayData) {
			final ByteArrayData byteArrayData = (ByteArrayData)unmodifiableCopy;
			final SendDataObject newSendDataObject = new SendDataObject(
					originalSendDataObject.getBaseSubscriptionInfo(),
					originalSendDataObject.getDalayedDataFlag(),
					originalSendDataObject.getDataNumber(),
					originalSendDataObject.getDataTime(),
					originalSendDataObject.getErrorFlag(),
					originalSendDataObject.getAttributesIndicator(),
					byteArrayData.getBytes()
			);
			return TelegramUtility.splitToApplicationTelegrams(newSendDataObject);
		}
		else {
			throw new IllegalArgumentException("Daten können nicht serialisiert werden: " + data.getClass().getName());
		}
	}

	/**
	 * Erstellt ein Data-Objekt aus einem SendData-Objekt
	 *
	 * @param dataObject           SendData-Objekt
	 * @param baseSubscriptionInfo BaseSubscriptionInfo
	 * @param connectionsManager   ConnectionsManager
	 *
	 * @return Data-Objekt
	 */
	private Data createData(final SendDataObject dataObject, final BaseSubscriptionInfo baseSubscriptionInfo, final ConnectionsManager connectionsManager) {
		final byte[] dataBytes = dataObject.getData();
		final AttributeGroup atg = connectionsManager.getDataModel().getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification()).getAttributeGroup();
		return DataFactory.forVersion(1).createUnmodifiableData(atg, dataBytes);
	}

	/**
	 * Erzeugt eine Liste für verzögerte Telegramme für eine Datenidentifikation und speichert sie in einer Map.
	 *
	 * @param info     Datenidentifikation der verzögerten Telegramme
	 * @param maxCount Maximale Anzahl der verzögerten Telegramme
	 *
	 * @return Neue Liste für verzögerte Telegramme
	 *
	 * @see #getStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)
	 * @see #deleteStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)
	 */
	private List<ApplicationDataTelegram> createStalledTelegramList(final BaseSubscriptionInfo info, final int maxCount) {
		final List<ApplicationDataTelegram> stalledTelegramsList = new ArrayList<ApplicationDataTelegram>(maxCount);
		_stalledTelegramListMap.put(info, stalledTelegramsList);
		return stalledTelegramsList;
	}

	/**
	 * Liefert eine vorher erzeugte Liste für verzögerte Telegramme für eine Datenidentifikation.
	 *
	 * @param info Datenidentifikation der verzögerten Telegramme
	 *
	 * @return Vorher erzeugte Liste für verzögerte Telegramme
	 *
	 * @see #createStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo,int)
	 * @see #deleteStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)
	 */
	private List<ApplicationDataTelegram> getStalledTelegramList(final BaseSubscriptionInfo info) {
		return _stalledTelegramListMap.get(info);
	}

	/**
	 * Liefert eine vorher erzeugte Liste für verzögerte Telegramme für eine Datenidentifikation und entfernt sie aus der Map.
	 *
	 * @param info Datenidentifikation der verzögerten Telegramme
	 *
	 * @return Vorher erzeugte Liste für verzögerte Telegramme
	 *
	 * @see #createStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo,int)
	 * @see #getStalledTelegramList(de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo)
	 */
	private List<ApplicationDataTelegram> deleteStalledTelegramList(final BaseSubscriptionInfo info) {
		return _stalledTelegramListMap.remove(info);
	}

	@Override
	public String toString() {
		return "AccessControlUtil{" + "_stalledTelegramListMap=" + _stalledTelegramListMap + '}';
	}
}
