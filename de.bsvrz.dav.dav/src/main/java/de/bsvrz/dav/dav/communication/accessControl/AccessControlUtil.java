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
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.DataModel;

import java.util.*;

/**
 * Klasse, die Hilfsfunktionen zum Filtern von Datenpaketen bereitstellt. Mit {@link #handleApplicationDataTelegram} soll ein ankommendes Telegram und eine
 * Liste mit Plugins übergeben werden. Diese Funktion setzt die Telegramme bei Bedarf zusammen, erstellt daraus ein Data-Objekt, übergibt dieses den Plugins,
 * und macht aus der Rückgabe der Plugins wieder ein Array aus Telegrammen. Diese Klasse verwendet Telegramme vom Typ ApplicationDataTelegram. Soll ein
 * TransmitterDataTelegram benutzt werden ist dieses vorher mit {@link de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram#getApplicationDataTelegram()
 * } zu konvertieren.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8953 $
 */
public final class AccessControlUtil {

	/**
	 * Verarbeitet zusammengehörige Telegramme durch die Plugins
	 *
	 * @param telegrams            Telegramme
	 * @param accessControlPlugins Plugins
	 * @param userId               Benutzer
	 * @param dataModel
	 *
	 * @return Verarbeitete Telegramme
	 */
	public static List<ApplicationDataTelegram> handleApplicationDataTelegram(
			final List<ApplicationDataTelegram> telegrams,
			final Collection<AccessControlPlugin> accessControlPlugins,
			final long userId,
			final DataModel dataModel) {
		ApplicationDataTelegram[] array = telegrams.toArray(new ApplicationDataTelegram[telegrams.size()]);
		final SendDataObject sendDataObject = TelegramUtility.getSendDataObject(array);
		final BaseSubscriptionInfo baseSubscriptionInfo = array[0].getBaseSubscriptionInfo();
		final Data initialData = createData(sendDataObject, baseSubscriptionInfo, dataModel);
		final Data data = processDataByPlugins(initialData, accessControlPlugins, baseSubscriptionInfo, userId);
		if(initialData == data) {
			// unverändert, einfach wieder Parameter zurückgeben
			return telegrams;
		}
		// Daten-Objekt vom Plugin wieder in ApplicationData-Telegramme zerlegen
		return Arrays.asList(dataToTelegrams(sendDataObject, data));
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
	private static Data processDataByPlugins(
			final Data initialData, final Collection<AccessControlPlugin> plugins, final BaseSubscriptionInfo baseSubscriptionInfo, final long userId) {
		Data data = initialData;
		if(data == null) return null;
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
	private static ApplicationDataTelegram[] dataToTelegrams(final SendDataObject originalSendDataObject, final Data data) {
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
	 * @param dataModel
	 *
	 * @return Data-Objekt oder null falls nicht erstellbar
	 */
	private static Data createData(final SendDataObject dataObject, final BaseSubscriptionInfo baseSubscriptionInfo, final DataModel dataModel) {
		AttributeGroupUsage attributeGroupUsage = dataModel.getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification());
		if(attributeGroupUsage == null) return null;
		final AttributeGroup atg = attributeGroupUsage.getAttributeGroup();
		return DataFactory.forVersion(1).createUnmodifiableData(atg, dataObject.getData());
	}
}
