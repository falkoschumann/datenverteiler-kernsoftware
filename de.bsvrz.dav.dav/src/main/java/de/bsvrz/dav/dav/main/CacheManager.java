/*
 * Copyright 2009 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;

import java.util.*;

/**
 * Speichert die Telegramme des zuletzt versendeten aktuellen Datensatzes und den zuletzt verwendeten Datensatzindex von aktuellen oder nachgelieferten
 * Datensätzen je Datenidentifikation ab.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 6333 $
 */
public class CacheManager {

	/**
	 * Map in der je Datenidentifikation ein Eintrag mit den Telegrammen des zuletzt versendeten aktuellen Datensatzes und dem zuletzt verwendeten Datensatzindex
	 * von aktuellen oder nachgelieferten Datensätzen enthalten ist.
	 */
	private final HashMap<BaseSubscriptionInfo, CacheEntry> _cache;

	/**
	 * Klasse für die je Datenidentifikation gespeicherten Einträge mit den Telegrammen des zuletzt versendeten aktuellen Datensatzes und dem zuletzt verwendeten
	 * Datensatzindex von aktuellen oder nachgelieferten Datensätzen.
	 */
	static class CacheEntry {

		private long _dataIndex;

		private TransmitterDataTelegram[] _telegrams;

		public CacheEntry(long dataIndex, TransmitterDataTelegram[] telegrams) {
			_dataIndex = dataIndex;
			_telegrams = telegrams;
		}

		public long getDataIndex() {
			return _dataIndex;
		}

		public void setDataIndex(long dataIndex) {
			_dataIndex = dataIndex;
		}

		public TransmitterDataTelegram[] getTelegrams() {
			return _telegrams;
		}

		public void setTelegrams(TransmitterDataTelegram[] telegrams) {
			_telegrams = telegrams;
		}
	}

	/** Erzeugt ein neues Objekt ohne Parameter. */
	CacheManager() {
		_cache = new HashMap<BaseSubscriptionInfo, CacheEntry>();
	}

	/**
	 * Liefert ein Array mit den Applikations-Telegrammen des zuletzt versendeten aktuellen Datensatzes der angegebenen Datenidentifikation zurück.
	 *
	 * @param dataIdentification Datenidentifikation des gewünschten Datensatzes
	 *
	 * @return Array mit den ApplikationsTelegrammen oder <code>null</code> wenn keine gespeicherten Telegramme für die angegebene Datenidentifikation vorliegen.
	 */
	final synchronized ApplicationDataTelegram[] getCurrentDataForApplication(BaseSubscriptionInfo dataIdentification) {
		TransmitterDataTelegram transmitterDataTelegrams[] = getCurrentDataForTransmitter(dataIdentification);
		if(transmitterDataTelegrams == null) {
			return null;
		}
		else {
			ApplicationDataTelegram applicationDataTelegrams[] = new ApplicationDataTelegram[transmitterDataTelegrams.length];
			for(int i = 0; i < transmitterDataTelegrams.length; ++i) {
				applicationDataTelegrams[i] = transmitterDataTelegrams[i].getApplicationDataTelegram();
			}
			return applicationDataTelegrams;
		}
	}

	/**
	 * Liefert ein Array mit den Datenverteiler-Telegrammen des zuletzt versendeten aktuellen Datensatzes der angegebenen Datenidentifikation zurück.
	 *
	 * @param dataIdentification Datenidentifikation des gewünschten Datensatzes
	 *
	 * @return Array mit den Datenverteiler-Telegrammen oder <code>null</code> wenn keine gespeicherten Telegramme für die angegebene Datenidentifikation
	 *         vorliegen.
	 */
	final synchronized TransmitterDataTelegram[] getCurrentDataForTransmitter(BaseSubscriptionInfo dataIdentification) {
		final CacheEntry cacheEntry = _cache.get(dataIdentification);
		return cacheEntry == null ? null : cacheEntry.getTelegrams();
	}

	/**
	 * Bestimmt den zuletzt verwendeten Datensatzindex von aktuellen oder nachgelieferten Datensätzen der angegebenen Datenidentifikation.
	 *
	 * @param dataIdentification Datenidentifikation des gewünschten Datensatzindex
	 *
	 * @return Zuletzt verwendeter Datensatzindex oder <code>0</code> falls noch kein Datensatz mit der angegebenen Datenidentifikation versendet wurde.
	 */
	final synchronized long getCurrentDataIndex(BaseSubscriptionInfo dataIdentification) {
		CacheEntry cacheEntry = _cache.get(dataIdentification);
		return cacheEntry == null ? 0L : cacheEntry.getDataIndex();
	}

	/**
	 * Speichert den übergebenen Datensatz wenn er nicht als nachgeliefert markiert ist und den darin enthaltenen Datensatzindex (auch wenn der Datensatz als
	 * nachgeliefert markiert ist).
	 *
	 * @param transmitterTelegrams Array mit den Telegrammen des zu speichernden Datensatzes
	 * @param delayedData          Flag das angibt, ob der Datensatz nachgeliefert ist.
	 *
	 * @return <code>true</code> im Normalfall und <code>false</code>, wenn der Datensatz einen nicht monoton steigenden Datensatzindex enthält und deshalb nicht
	 *         versendet werden sollte.
	 */
	final synchronized boolean update(TransmitterDataTelegram[] transmitterTelegrams, boolean delayedData) {
		BaseSubscriptionInfo baseSubscriptionInfo = transmitterTelegrams[0].getBaseSubscriptionInfo();
		long newDataIndex = transmitterTelegrams[0].getDataNumber();
		boolean monotone = true;
		if((newDataIndex & 0x0000000000000003L) == 0) {
			CacheEntry cacheEntry = getCacheEntry(baseSubscriptionInfo);
			if(cacheEntry == null) {
				putNewCacheEntry(baseSubscriptionInfo, newDataIndex, transmitterTelegrams);
				//_cache.put(baseSubscriptionInfo, new CacheEntry(newDataIndex, transmitterTelegrams));
			}
			else {
				if(cacheEntry.getDataIndex() < newDataIndex) {
					cacheEntry.setDataIndex(newDataIndex);
					if(!delayedData) cacheEntry.setTelegrams(transmitterTelegrams);
				}
				else {
					monotone = false;
				}
			}
		}
		return monotone;
	}

	CacheEntry getCacheEntry(final BaseSubscriptionInfo baseSubscriptionInfo) {
		return _cache.get(baseSubscriptionInfo);
	}

	void putNewCacheEntry(final BaseSubscriptionInfo baseSubscriptionInfo, long newDataIndex, TransmitterDataTelegram[] transmitterTelegrams) {
			_cache.put(baseSubscriptionInfo, new CacheEntry(newDataIndex, transmitterTelegrams));
	}

	/**
	 * Löscht den zur angegebenen Datenidentifikation gespeicherten Datensatz.
	 *
	 * @param dataIdentification Datenidentifikation des zu löschenden Datensatzes
	 */
	final synchronized void clean(BaseSubscriptionInfo dataIdentification) {
		_cache.remove(dataIdentification);
	}
}
