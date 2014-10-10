/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;

import java.util.*;

/**
 * Diese Klasse stellt eine Methode zur Verfügung, mit der alle Teiltelegramme eines Datensatzes gesammelt werden können. Wurden alle Teiltelegramme empfangen,
 * so werden diese zurückgegeben und der Datensatz kann rekonstruiert werden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class SplittedTransmitterTelegramsTable {


	/**
	 * Sammelt alle Teiltelegramme einer <code>BaseSubscriptionInfo</code>.
	 * <p/>
	 * Als Key dient die <code>BaseSubscriptionInfo</code>, der Value ist eine Hashtable (im folgenden als Hashtable' bezeichnet).
	 * <p/>
	 * Die Hashtable' benutzt als Key die laufende Nummer des Datensatzes, als Value wird eine Liste<TransmitterDataTelegram> gespeichert. In dieser Liste werden
	 * die Teiltelegramme gespeichert.
	 */
	private Hashtable<BaseSubscriptionInfo, Hashtable<Long, TransmitterDataTelegram[]>> dataTable;

	/** Erstellt ein neues Objekt */
	public SplittedTransmitterTelegramsTable() {
		dataTable = new Hashtable<BaseSubscriptionInfo, Hashtable<Long, TransmitterDataTelegram[]>>();
	}

	/**
	 * Diese Methode sammelt alle Teiltelegramme. Wurden alle Teiltelegramme empfangen, werden diese zurückgegeben.
	 *
	 * @param telegram Teiltelegramm, das ein Telegramm vervollständigen soll oder ein komplettes Telegramm, das als Ganzes übergeben wurde und somit nicht
	 *                 zusammengebaut werden muss.
	 *
	 * @return Alle Teiltelegramme, aus denen ein vollständiges Telegramm rekonstruiert werden kann (und damit ein Datenatz) oder aber <code>null</code>.
	 *         <code>null</code> bedeutet, dass noch nicht alle Teiltelegramme empfangen wurden die nötig sind, um das gesamte Telegramm zusammen zu bauen.
	 *
	 * @throws IllegalArgumentException Das übergebene Telegramm konnte keinem bisher empfangenen Teil zugeordnet werden oder war <code>null</code>.
	 */
	final TransmitterDataTelegram[] put(TransmitterDataTelegram telegram) {
		if(telegram == null) {
			throw new IllegalArgumentException("Der Parameter ist null");
		}
		int totalTelegramCount = telegram.getTotalTelegramsCount();
		int index = telegram.getTelegramNumber();
		if(index >= totalTelegramCount) {
			throw new IllegalArgumentException("Der Telegramm-Index ist grösser als die maximale Anzahl der zerstückelten Telegramme dieses Datensatzes");
		}
		if((index == 0) && (totalTelegramCount == 1)) {
			return (new TransmitterDataTelegram[]{telegram});
		}
		BaseSubscriptionInfo key = telegram.getBaseSubscriptionInfo();
		if(key == null) {
			throw new IllegalArgumentException("Das Telegramm ist inkonsistent");
		}
		Hashtable<Long, TransmitterDataTelegram[]> table = dataTable.get(key);
		Long subKey = new Long(telegram.getDataNumber());
		if(table == null) {
			table = new Hashtable<Long, TransmitterDataTelegram[]>();
			TransmitterDataTelegram[] list = new TransmitterDataTelegram[totalTelegramCount];
			list[index] = telegram;
			table.put(subKey, list);
			dataTable.put(key, table);
			return null;
		}
		else {
			TransmitterDataTelegram[] list = (TransmitterDataTelegram[])table.get(subKey);
			if(list == null) {
				list = new TransmitterDataTelegram[totalTelegramCount];
				list[index] = telegram;
				table.put(subKey, list);
				return null;
			}
			else {
				synchronized(list) {
					list[index] = telegram;
					for(int i = 0; i < list.length; ++i) {
						TransmitterDataTelegram tmpTelegram = list[i];
						if(tmpTelegram == null) {
							return null;
						}
						if(i != tmpTelegram.getTelegramNumber()) {
							throw new IllegalArgumentException("Falsche Daten in der Cache-Tabelle der zerstückelten Telegramme");
						}
					}
					table.remove(subKey);
					if(table.size() == 0) {
						dataTable.remove(key);
					}
				}
				return list;
			}
		}
	}
}
