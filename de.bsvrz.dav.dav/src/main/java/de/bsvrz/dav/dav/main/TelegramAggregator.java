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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.DataTelegramInterface;
import de.bsvrz.dav.dav.subscriptions.SubscriptionInfo;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Diese Klasse dient dazu, aufgeteilte Datentelegramme wieder zusammenzusetzen. Dazu ist für jedes ankommende Telegram die aggregate()-Funktion
 * auszuführen. Sobald alle Telegramm eingetroffen sind, wird eine Liste mit den Telegrammen zurückgegeben, sonst nur eine leere Liste.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11428 $
 */
public class TelegramAggregator<T extends DataTelegramInterface> {

	private static final Debug _debug = Debug.getLogger();

	private final Map<SubscriptionInfo, List<T>> _telegrams = new ConcurrentHashMap<SubscriptionInfo, List<T>>(256);

	/**
	 * Verarbeitet ein ankommendes Telegramm und gibt die Liste der Telegramme zurück, sobald alle Telegramme eingetroffen sind. Es ist daher wichtig,
	 * dass alle Telegramme einer Datenidentifikation in der richtigen Reihenfolge eintreffen. Um das zu erreichen sollte pro Anmeldung nur ein
	 * einzelner Thread Telegramme eintragen
	 * @param telegram
	 * @param subscription
	 * @return
	 */
	public List<T> aggregate(final T telegram, final SubscriptionInfo subscription) {
		final int totalTelegramsCount = telegram.getTotalTelegramsCount();
		if(totalTelegramsCount == 1) {
			// Nicht gestückeltes Telegram: Einfach zurückgeben.
			return Arrays.asList(telegram);
		}

		final BaseSubscriptionInfo info = telegram.getBaseSubscriptionInfo();

		final int telegramNumber = telegram.getTelegramNumber();
		if(telegramNumber == 0) {
			// Das erste Telegramm eines zerstückelten Datensatzes
			final List<T> stalledTelegramsList = new ArrayList<T>(totalTelegramsCount);
			_telegrams.put(subscription, stalledTelegramsList);
			stalledTelegramsList.add(telegram);
		}
		else if(telegramNumber + 1 != totalTelegramsCount) {
			// Ein mittleres Telegramm eines zerstückelten Datensatzes
			final List<T> stalledTelegramList = _telegrams.get(subscription);
			if(stalledTelegramList == null) {
				_debug.warning(
						"Ein mittleres Telegramm ist ohne erstes Telegramm eingetroffen", info
				);
			}
			else {
				if(telegramNumber == stalledTelegramList.size()){
					stalledTelegramList.add(telegram);
				}
				else{
					_debug.warning(
							"Die Telegramme sind nicht in der richtigen Reihenfolge eingetroffen", info
					);
				}
			}
		}
		else {
			// Das letzte Telegramm eines zerstückelten Datensatzes
			final List<T> stalledTelegramList = _telegrams.remove(subscription);
			if(stalledTelegramList == null) {
				_debug.warning(
						"Das letzte Telegramm ist ohne vorherige Telegramme eingetroffen", info
				);
			}
			else if(telegramNumber != stalledTelegramList.size()) {
				_debug.warning(
						"Die Telegramme sind nicht in der richtigen Reihenfolge eingetroffen", info
				);
			}
			else {
				stalledTelegramList.add(telegram);
				return stalledTelegramList;
			}
		}
		return Collections.emptyList();
	}
}
