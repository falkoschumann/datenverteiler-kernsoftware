/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
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
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscription;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataUnsubscription;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;

import java.util.*;

/**
 * Diese Klasse enthält Hilfsmethoden zur Behandlung von durchzuführenden Anmeldungen und Abmeldungen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 7717 $
 */
public class OutSubscriptionsHelper {

	/** Die Tabelle, in der die rausgehenden An-Abmeldungen gehalten werden */

	/**
	 * Key: Id(Long) des Datenverteilers, zu dem eine Verbindung aufgebaut werden soll. Value: Hashtable (im folgenden Hash2 genannt).
	 * <p/>
	 * Die Hashtable Hash2 besitzt folgenden Aufbau: Key: BaseSubscriptionInfo. Value: Eine Liste. Die Liste speicherte Objekte vom Typ
	 * <code>OutGoingSubscription</code>.
	 */
	private final Hashtable _outSubscriptionsTable = new Hashtable();

	/**
	 * Diese Methode führt die austehenden Anmeldungen durch.
	 *
	 * @param involvedSubscription betroffene Anmeldungen
	 * @param outSubscription      Anmeldung an dem DAV
	 */
	final void handleOutSubscription(
			InAndOutSubscription involvedSubscription, InAndOutSubscription.OutSubscription outSubscription
	) {
		if((involvedSubscription == null) || (outSubscription == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}

		OutGoingSubscription _outGoingSubscription = null;
		final Long key = new Long(outSubscription._targetTransmitter);
		Hashtable tmp = (Hashtable)_outSubscriptionsTable.get(key);
		if(tmp == null) {
			tmp = new Hashtable();
			_outSubscriptionsTable.put(key, tmp);
		}
		ArrayList list = (ArrayList)tmp.get(involvedSubscription._baseSubscriptionInfo);
		if(list == null) {
			list = new ArrayList();
			tmp.put(involvedSubscription._baseSubscriptionInfo, list);
		}
		final long transmitterToConsider[] = outSubscription._transmittersToConsider;
		if(transmitterToConsider == null) {
			throw new IllegalStateException("Inkonsistenzfehler");
		}
		final int size = list.size();
		for(int i = 0; i < size; ++i) {
			OutGoingSubscription outGoingSubscription = (OutGoingSubscription)list.get(i);
			if(outGoingSubscription != null) {
				if(outGoingSubscription._subscriptionState != involvedSubscription._role) {
					continue;
				}
				long ids[] = outGoingSubscription._transmitterToConsider;
				if(ids != null) {
					if(ids.length != transmitterToConsider.length) {
						continue;
					}
					boolean subset = true;
					for(int j = 0; j < transmitterToConsider.length; ++j) {
						boolean found = false;
						for(int k = 0; k < ids.length; ++k) {
							if(ids[k] == transmitterToConsider[j]) {
								found = true;
								break;
							}
						}// for über alle ids
						if(!found) {
							subset = false;
							break;
						}
					}// for über alle zu berücksichtigenden DaV´s
					if(subset) {
						_outGoingSubscription = outGoingSubscription;
						break;
					}
				}
			}
		} // for
		if(_outGoingSubscription == null) {
			OutGoingSubscription outGoingSubscription = new OutGoingSubscription();
			outGoingSubscription._baseSubscriptionInfo = involvedSubscription._baseSubscriptionInfo;
			outGoingSubscription._subscriptionState = involvedSubscription._role;
			outGoingSubscription._targetTransmitterId = outSubscription._targetTransmitter;
			outGoingSubscription._transmitterToConsider = outSubscription._transmittersToConsider;
			outGoingSubscription._receip = outSubscription._state;
			outGoingSubscription._mainTransmitterId = outSubscription._mainTransmitter;
			outGoingSubscription._count = 1;
			list.add(outGoingSubscription);
		}
		else {
			_outGoingSubscription._count += 1;
			outSubscription._mainTransmitter = _outGoingSubscription._mainTransmitterId;
			outSubscription._state = _outGoingSubscription._receip;
		}
	}

	/**
	 * Diese Methode wird aufgerufen, wenn Daten abgemeldet werden sollen. Die Abmeldung wird nicht sofort ausgeführt sondern erst, wenn
	 * {@link #flushOutUnSubscription(ConnectionsManagerInterface)} aufgerufen wird.
	 * <p/>
	 * Wird für eine Anmeldung diese Methode n-Fach aufgerufen (ohne den Aufruf von flush) und dazu die Methode
	 * {@link #handleOutSubscription(InAndOutSubscription, de.bsvrz.dav.dav.main.InAndOutSubscription.OutSubscription)} m-Fach, so werden nur
	 * (m-n) viele Telegramme verschickt.<br>
	 *<p/>
	 * m > n = Es werden m-n viele Anmeldungen verschickt.<br>
	 * m < n = Es werden n-m viele Abmeldungen verschickt.<br>
	 * m == n = Es wird kein Telegramm verschickt.<br>
	 *
	 * @param involvedSubscription betroffene Anmeldungen
	 * @param outSubscription      Anmeldung an dem DAV
	 */
	final void handleOutUnsubscription(
			InAndOutSubscription involvedSubscription, InAndOutSubscription.OutSubscription outSubscription
	) {
		if(Transmitter._debugLevel > 20) {
			System.err.println("OutSubscriptionsHelper.handleOutUnsubscription");
		}
		if((involvedSubscription == null) || (outSubscription == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}

		OutGoingSubscription _outGoingSubscription = null;
		Long key = new Long(outSubscription._targetTransmitter);
		Hashtable tmp = (Hashtable)_outSubscriptionsTable.get(key);
		if(tmp == null) {
			tmp = new Hashtable();
			_outSubscriptionsTable.put(key, tmp);
		}
		ArrayList list = (ArrayList)tmp.get(involvedSubscription._baseSubscriptionInfo);
		if(list == null) {
			list = new ArrayList();
			tmp.put(involvedSubscription._baseSubscriptionInfo, list);
		}
		long transmitterToConsider[] = outSubscription._transmittersToConsider;
		if(transmitterToConsider == null) {
			throw new IllegalStateException("Inkonsistenzfehler");
		}
		int size = list.size();
		for(int i = 0; i < size; ++i) {
			OutGoingSubscription outGoingSubscription = (OutGoingSubscription)list.get(i);
			if(outGoingSubscription != null) {
				if(outGoingSubscription._subscriptionState != involvedSubscription._role) {
					continue;
				}
				long ids[] = outGoingSubscription._transmitterToConsider;
				if(ids != null) {
					if(ids.length != transmitterToConsider.length) {
						continue;
					}
					boolean subset = true;
					for(int j = 0; j < transmitterToConsider.length; ++j) {
						boolean found = false;
						for(int k = 0; k < ids.length; ++k) {
							if(ids[k] == transmitterToConsider[j]) {
								found = true;
								break;
							}
						}
						if(!found) {
							subset = false;
							break;
						}
					}
					if(subset) {
						_outGoingSubscription = outGoingSubscription;
						break;
					}
				}
			}
		}
		if(_outGoingSubscription == null) {
			OutGoingSubscription outGoingSubscription = new OutGoingSubscription();
			outGoingSubscription._baseSubscriptionInfo = involvedSubscription._baseSubscriptionInfo;
			outGoingSubscription._subscriptionState = involvedSubscription._role;
			outGoingSubscription._targetTransmitterId = outSubscription._targetTransmitter;
			outGoingSubscription._transmitterToConsider = outSubscription._transmittersToConsider;
			outGoingSubscription._receip = outSubscription._state;
			outGoingSubscription._mainTransmitterId = outSubscription._mainTransmitter;
			outGoingSubscription._count = -1;
			list.add(outGoingSubscription);
		}
		else {
			_outGoingSubscription._count -= 1;
			_outGoingSubscription._receip = outSubscription._state;
			_outGoingSubscription._mainTransmitterId = outSubscription._mainTransmitter;
		}
	}

	/**
	 * Verschickt alle An/Abmeldungen, die mit {@link #handleOutSubscription} und {@link #handleOutUnsubscription} gemacht wurden.
	 *
	 * @param connectionsManager Objekt, über das die An/Abmeldungen verschickt werden.
	 */
	final void flushOutUnSubscription(ConnectionsManagerInterface connectionsManager) {
		if(Transmitter._debugLevel > 20) {
			System.err.println("OutSubscriptionsHelper.flushOutUnSubscription");
			System.err.println("outSubscriptionsTable.size() = " + _outSubscriptionsTable.size());
			Thread.dumpStack();
		}
		Enumeration enumeration = _outSubscriptionsTable.keys();
		if(enumeration != null) {
			while(enumeration.hasMoreElements()) {
				Long key = (Long)enumeration.nextElement();
				Hashtable _tmp = (Hashtable)_outSubscriptionsTable.remove(key);
				if(_tmp != null) {
					Enumeration _enumeration = _tmp.keys();
					if(_enumeration != null) {
						while(_enumeration.hasMoreElements()) {
							BaseSubscriptionInfo info = (BaseSubscriptionInfo)_enumeration.nextElement();
							if(info != null) {
								ArrayList _list = (ArrayList)_tmp.get(info);
								if(_list != null) {
									int size = _list.size();
									for(int i = 0; i < size; ++i) {
										OutGoingSubscription outGoingSubscription = (OutGoingSubscription)_list.get(i);
										if(outGoingSubscription != null) {
											if(outGoingSubscription._count == 0) {
												continue;
											}
											while(outGoingSubscription._count < 0) {
												T_T_HighLevelCommunicationInterface connection = connectionsManager.getTransmitterConnection(
														outGoingSubscription._targetTransmitterId
												);
												if(connection != null) {
													TransmitterDataUnsubscription unsubscription = new TransmitterDataUnsubscription(
															outGoingSubscription._baseSubscriptionInfo,
															outGoingSubscription._subscriptionState,
															outGoingSubscription._transmitterToConsider
													);
													if(Transmitter._debugLevel > 20) {
														System.err.println(
																"OutSubscriptionsHelper.flushOutUnSubscription A connection.sendTelegram(unsubscription);"
														);
													}
													connection.sendTelegram(unsubscription);
												}
												outGoingSubscription._count += 1;
											}
											while(outGoingSubscription._count > 0) {
												final T_T_HighLevelCommunicationInterface connection = connectionsManager.getTransmitterConnection(
														outGoingSubscription._targetTransmitterId
												);
												if(connection != null) {
													final TransmitterDataSubscription subscription = new TransmitterDataSubscription(
															outGoingSubscription._baseSubscriptionInfo,
															outGoingSubscription._subscriptionState,
															outGoingSubscription._transmitterToConsider
													);
													if(Transmitter._debugLevel > 20) {
														System.err.println(
																"OutSubscriptionsHelper.flushOutUnSubscription B connection.sendTelegram(unsubscription);"
														);
													}
													connection.sendTelegram(subscription);
												}
												outGoingSubscription._count -= 1;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private class OutGoingSubscription {

		BaseSubscriptionInfo _baseSubscriptionInfo;

		long _targetTransmitterId;

		long _transmitterToConsider[];

		long _mainTransmitterId;

		byte _subscriptionState;

		byte _receip;

		/**
		 * Dieser Zähler gibt an, ob eine Verbindung abgemeldet werden soll (_count < 0) oder ob eine Verbindung angemeldet werden soll (_count > 0).
		 * <p/>
		 * Ist _count == 0, so muss nichts gemacht werden. Der Betrag von _count gibt an, wie oft an/abgemeldet werden muss.
		 */
		int _count;

		OutGoingSubscription() {
		}
	}
}
