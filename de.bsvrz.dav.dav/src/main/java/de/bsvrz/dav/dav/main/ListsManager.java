/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
 * Copyright 2008 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2006 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.*;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.sys.funclib.concurrent.DelayedTrigger;
import de.bsvrz.sys.funclib.concurrent.TriggerTarget;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Diese Klasse dient zur Verwaltung von Anmeldelistentelegrammen, die zwischen Datenverteilern zum Austausch von Informationen über angemeldete Objekte,
 * Attributgruppen und Aspekte verwendet werden. Jeder Datenverteiler hält grobe Informationen über die vorliegenden Quell- und Senkenanmeldungen der
 * Applikationen, die bei ihm angemeldet sind, vor. Diese Informationen werden zwischen den einzelnen Datenverteilern ausgetauscht, damit bei Anmeldungen von
 * Daten direkt geprüft werden kann, ob diese Daten potentiell im System vorhanden sind und welche Datenverteiler als Zentraldatenverteiler in Frage kommen. Um
 * das Datenaufkommen und die Anzahl der Aktualisierungen hier gering zu halten, werden die Anmeldungen nicht detailliert vorgehalten und weitergegeben, sondern
 * es werden nur Änderungen zweier Listen gepflegt und verteilt: Die erste enthält die Objekte zu denen Quell- oder Senkenanmeldungen bestehen (Objektliste);
 * die zweite Liste enthält die Attributgruppen/Aspekt-Kombinationen zu denen Quell- oder Senkenanmeldungen bestehen (Attributgruppenliste). Jeder
 * Datenverteiler verwaltet für jeden von ihm erreichbaren Datenverteiler eine Anmeldungsliste.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 13195 $
 */
public class ListsManager implements ListsManagerInterface {

	private static final Debug _debug = Debug.getLogger();

	/** Eigene Datenverteiler-Id */
	private final long _localTransmitterId;

	/** Map mit den Anmeldelisten der bekannten Datenverteiler.*/
	private final Map<Long, TransmitterSubscriptionInfos> _subscriptionInfos = Collections.synchronizedMap(new HashMap<Long, TransmitterSubscriptionInfos>());

	private final LocalSubscriptionInfos _localSubscriptionInfos;

	private final TransmitterSubscriptionInfos _localTransmitterSubscriptionInfos;

	private static final long DELAYED_SUBSCRIPTION_TIME_LIMIT = 60000; // 1 min

	private final HighLevelConnectionsManagerInterface _connectionsManager;

	private final ArrayList<DelayedSubscriptionHandle> _delayedSubscriptionList;

	private final Object _delayedSubscriptionSync = new Object();

	private final DelayedSubscriptionThread _delayedSubscriptionThread;

	private BestWayManagerInterface _bestWayManager = null;

	private final Object _localSubscriptionInfosSendLock = new Object();


	/**
	 * Erzeugt ein neues Objekt mit den übergebenen Parametern. Zu beachten ist, dass das Verwaltungsobjekt zur Bestimmung der günstigsten Wege nicht initialisiert
	 * wird, sondern im Anschluss mit einem Aufruf des entsprechenden Setters gesetzt werden muss, bevor die Dienste des ListsManagers in Anspruch genommen werden
	 * können.
	 *
	 * @param connectionsManager Verbindungsverwaltung
	 */
	public ListsManager(final HighLevelConnectionsManagerInterface connectionsManager) {
		_connectionsManager = connectionsManager;

		_localTransmitterId = _connectionsManager.getTransmitterId();

		_localSubscriptionInfos = new LocalSubscriptionInfos();

		_localTransmitterSubscriptionInfos = new TransmitterSubscriptionInfos(_localTransmitterId);
		_localTransmitterSubscriptionInfos._delivererId = _localTransmitterId;
		_subscriptionInfos.put(_localTransmitterId, _localTransmitterSubscriptionInfos);

		_delayedSubscriptionList = new ArrayList<DelayedSubscriptionHandle>();

		_delayedSubscriptionThread = new DelayedSubscriptionThread();
		_delayedSubscriptionThread.start();
	}


	/** Bestimmt den Thread, der für die verzögerte Durchführung von Anmeldungen zuständig ist.
	 * @return Thread, der für die verzögerte Durchführung von Anmeldungen zuständig ist. */
	Thread getDelayedSubscriptionThread() {
		return _delayedSubscriptionThread;
	}

	/**
	 * Getter für das Verwaltungsobjekt zur Bestimmung der günstigsten Wege.
	 *
	 * @return Verwaltungsobjekt zur Bestimmung der günstigsten Wege.
	 */
	public BestWayManagerInterface getBestWayManager() {
		return _bestWayManager;
	}

	/**
	 * Setter für das Verwaltungsobjekt zur Bestimmung der günstigsten Wege.
	 *
	 * @param bestWayManager Verwaltungsobjekt zur Bestimmung der günstigsten Wege.
	 */
	public void setBestWayManager(final BestWayManagerInterface bestWayManager) {
		_bestWayManager = bestWayManager;
	}


	@Override
	public final void addEntry(final long delivererId, final long transmitterId) {
		// System.out.println("addEntry _localTransmitterId = " + _localTransmitterId + ", delivererId = " + delivererId + ", transmitterId = " + transmitterId);
		if(transmitterId == -1) {
			throw new IllegalArgumentException("Argument ist ungültig");
		}
		TransmitterSubscriptionInfos entry;
		synchronized(_subscriptionInfos) {
			entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(transmitterId);
			if(entry == null) {
				entry = new TransmitterSubscriptionInfos(transmitterId);
				_subscriptionInfos.put(transmitterId, entry);
			}
		}
		synchronized(entry) {
			final List<Long> subscribers = entry._subscribers;
			if(delivererId == -1) {
				// Datenverteiler ist nicht mehr erreichbar
				final long[] _ids = {entry._transmitterId};
				cleanPendingDelayedSubscriptions(_ids);
				if(entry._delivererId != -1) {
					// Kündigung des Abos beim Lieferant der Anmeldelisten des nicht mehr erreichbaren Datenverteiler
					final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(entry._delivererId);
					if(connection != null) {
						final TransmitterListsUnsubscription transmitterListsUnsubscription = new TransmitterListsUnsubscription(_ids);
						// System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUnsubscription);
						connection.sendTelegram(transmitterListsUnsubscription);
					}
				}
				entry._delivererId = -1;
				final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(_ids);
				for(final Long subscriber : subscribers) {
					// Kündigung der Abos bei Abnehmern der Anmeldelisten des nicht mehr erreichbaren Datenverteiler
					if(subscriber != null) {
						final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
						if(connection != null) {
							// System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
							connection.sendTelegram(transmitterListsDeliveryUnsubscription);
						}
					}
				}
				subscribers.clear();
				entry._objectIdSet.clear();
				entry._atgUsageSet.clear();
			}
			else if((delivererId != _localTransmitterId) && (delivererId != entry._delivererId)) {
				// Datenverteiler ist über einen anderen Nachbarn erreichbar
				final long[] _ids = {entry._transmitterId};
				cleanPendingDelayedSubscriptions(_ids);
				if(entry._delivererId != -1) {
					// Kündigung des Abos beim bisherigen Lieferant der Anmeldelisten des über einen neuen Nachbarn erreichbaren Datenverteilers
					final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(entry._delivererId);
					if(connection != null) {
						final TransmitterListsUnsubscription transmitterListsUnsubscription = new TransmitterListsUnsubscription(_ids);
						// System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUnsubscription);
						connection.sendTelegram(transmitterListsUnsubscription);
					}
				}
				final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(_ids);
				if(subscribers.remove(delivererId)) {
					// Falls der neue Lieferant bisher als Abnehmer angemeldet war, dann wird diesem die Kündigung gesendet
					final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(delivererId);
					if(connection != null) {
						// System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
						connection.sendTelegram(transmitterListsDeliveryUnsubscription);
					}
				}
				entry._delivererId = delivererId;

				// Anmeldung beim neuen Lieferant
				final T_T_HighLevelCommunicationInterface highLevelCommunication = _connectionsManager.getTransmitterConnectionFromId(delivererId);
				if(highLevelCommunication != null) {
					final TransmitterListsSubscription transmitterListsSubscription = new TransmitterListsSubscription(_ids);
					// System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsSubscription);
					highLevelCommunication.sendTelegram(transmitterListsSubscription);
				}
			}
		}
	}

	@Override
	public final void handleWaysChanges(final long[] changedTransmitterIds) {

		if(changedTransmitterIds == null) {
			throw new IllegalArgumentException("Argument ist ungültig");
		}
		final Hashtable<Long, ArrayList<Long>> unsubscriptionTable = new Hashtable<Long, ArrayList<Long>>();
		final Hashtable<Long, ArrayList<Long>> deliveryUnsubscriptionTable = new Hashtable<Long, ArrayList<Long>>();
		final Hashtable<Long, ArrayList<Long>> subscriptionTable = new Hashtable<Long, ArrayList<Long>>();

		for(final long changedTransmitterId : changedTransmitterIds) {
			TransmitterSubscriptionInfos entry;
			synchronized(_subscriptionInfos) {
				entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(changedTransmitterId);
				if(entry == null) {
					entry = new TransmitterSubscriptionInfos(changedTransmitterId);
					_subscriptionInfos.put(changedTransmitterId, entry);
				}
			}
			synchronized(entry) {
				final long delivererId = _bestWayManager.getBestWay(changedTransmitterId);
				final List<Long> subscribers = entry._subscribers;
				if(delivererId == -1) {
					if(entry._delivererId != -1) {
						final Long _key = entry._delivererId;
						ArrayList<Long> _list = unsubscriptionTable.get(_key);
						if(_list == null) {
							_list = new ArrayList<Long>();
							unsubscriptionTable.put(_key, _list);
							_list.add(entry._transmitterId);
						}
						else {
							final Long value = entry._transmitterId;
							if(!_list.contains(value)) {
								_list.add(value);
							}
						}
					}
					entry._delivererId = -1;

					for(final Long subscriber : subscribers) {
						if(subscriber != null) {
							ArrayList<Long> _list = deliveryUnsubscriptionTable.get(subscriber);
							if(_list == null) {
								_list = new ArrayList<Long>();
								deliveryUnsubscriptionTable.put(subscriber, _list);
								_list.add(entry._transmitterId);
							}
							else {
								final Long value = entry._transmitterId;
								if(!_list.contains(value)) {
									_list.add(value);
								}
							}
						}
					}
					subscribers.clear();

					entry._objectIdSet.clear();
					entry._atgUsageSet.clear();
					cleanPendingDelayedSubscriptions(new long[]{changedTransmitterId});
				}
				else if((delivererId != _localTransmitterId) && (delivererId != entry._delivererId)) {
					if(entry._delivererId != -1) {
						final Long _key = entry._delivererId;
						ArrayList<Long> _list = unsubscriptionTable.get(_key);
						if(_list == null) {
							_list = new ArrayList<Long>();
							unsubscriptionTable.put(_key, _list);
							_list.add(entry._transmitterId);
						}
						else {
							final Long value = entry._transmitterId;
							if(!_list.contains(value)) {
								_list.add(value);
							}
						}
					}

					if(subscribers.remove(delivererId)) {
						ArrayList<Long> _list = deliveryUnsubscriptionTable.get(delivererId);
						if(_list == null) {
							_list = new ArrayList<Long>();
							deliveryUnsubscriptionTable.put(delivererId, _list);
							_list.add(entry._transmitterId);
						}
						else {
							final Long value = entry._transmitterId;
							if(!_list.contains(value)) {
								_list.add(value);
							}
						}
					}
					entry._delivererId = delivererId;

					ArrayList<Long> _list = subscriptionTable.get(delivererId);
					if(_list == null) {
						_list = new ArrayList<Long>();
						subscriptionTable.put(delivererId, _list);
						_list.add(entry._transmitterId);
					}
					else {
						final Long value = entry._transmitterId;
						if(!_list.contains(value)) {
							_list.add(value);
						}
					}
				}
			}
		}
		if(unsubscriptionTable.size() > 0) {
			final Enumeration<Long> enumeration = unsubscriptionTable.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> list = unsubscriptionTable.get(way);
					if(list != null) {
						final int length = list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = list.get(i);
							}
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(way);
							if(connection != null) {
								final TransmitterListsUnsubscription transmitterListsUnsubscription = new TransmitterListsUnsubscription(_ids);
//								System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUnsubscription);
								connection.sendTelegram(transmitterListsUnsubscription);
							}
						}
					}
				}
			}
		}
		if(deliveryUnsubscriptionTable.size() > 0) {
			final Enumeration<Long> enumeration = deliveryUnsubscriptionTable.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> list = deliveryUnsubscriptionTable.get(way);
					if(list != null) {
						final int length = list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = list.get(i);
							}
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(way);
							if(connection != null) {
								final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(_ids);
//								System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
								connection.sendTelegram(transmitterListsDeliveryUnsubscription);
							}
						}
					}
				}
			}
		}
		if(subscriptionTable.size() > 0) {
			final Enumeration<Long> enumeration = subscriptionTable.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> list = subscriptionTable.get(way);
					if(list != null) {
						final int length = list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = list.get(i);
							}
							cleanPendingDelayedSubscriptions(_ids);
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(way);
							if(connection != null) {
								final TransmitterListsSubscription transmitterListsSubscription = new TransmitterListsSubscription(_ids);
//								System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsSubscription);
								connection.sendTelegram(transmitterListsSubscription);
							}
						}
					}
				}
			}
		}
		//printContent();
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Anmeldung auf die Zuliefererinformationen eines bestimmten Datenverteilers
	 * eingetroffen ist. Zuerst wird der Eintrag jedes in ids spezifizierten Datenverteilers aus der Tabelle bestimmt. Wenn ein Eintrag vorhanden ist, dann wird
	 * überprüft, ob ein Zulieferer dafür definiert ist. Ist keiner vorhanden, so wird dem Datenverteiler, der hier ein Abonnement versucht, eine
	 * Zuliefererinformationsankündigung gesendet. Wenn aber ein Zulieferer existiert, wird überprüft ob dieser nicht schon abonniert ist. Ist dies nicht der Fall,
	 * wird er zur Abonnentenliste hinzugefügt, und alle registrierten Objekte und Kombinationen aus Attributgruppe und Aspekt werden zusammen in einer
	 * Zuliefereraktualisierung zu ihm gesendet.
	 *
	 * @param transmitterId ID des DAV
	 * @param ids           long Array mit den IDs enthält die Liste der DAVs
	 */

	final void subscribe(final long transmitterId, final long[] ids) {
		

		if(ids == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if((transmitterId == -1) || (transmitterId == _connectionsManager.getTransmitterId())) {
			throw new IllegalArgumentException("Argument ist inkonsistent");
		}

		final ArrayList<Long> transmitterListsDeliveryUnsubscriptionIds = new ArrayList<Long>();
		for(int i = ids.length - 1; i > -1; --i) {
			final Long key = ids[i];
			final TransmitterSubscriptionInfos entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(key);
			if(entry != null) {
				synchronized(entry) {
					if(entry._delivererId == transmitterId) {
						transmitterListsDeliveryUnsubscriptionIds.add(ids[i]);
						continue;
					}

					boolean notFound = true;
					for(int j = entry._subscribers.size() - 1; j > -1; --j) {
						if(transmitterId == entry._subscribers.get(j)) {
							notFound = false;
							break;
						}
					}
					if(!entry._subscribers.contains(transmitterId)) {
						entry._subscribers.add(transmitterId);
						if(entry._delivererId != -1) {
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(transmitterId);
							if(connection != null) {
								sendTransmitterUpdates(
										connection, entry._transmitterId, new ArrayList<Long>(entry._objectIdSet), new ArrayList<Long>(entry._atgUsageSet)
								);
							}
						}
					}
				}
			}
		}
		final int size = transmitterListsDeliveryUnsubscriptionIds.size();
		if(size > 0) {
			final long[] idsToUnsubscribe = new long[size];
			for(int i = 0; i < size; ++i) {
				idsToUnsubscribe[i] = transmitterListsDeliveryUnsubscriptionIds.get(i);
			}
			final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(idsToUnsubscribe);
			final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(transmitterId);
			if(connection != null) {
//				System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
				connection.sendTelegram(transmitterListsDeliveryUnsubscription);
			}
		}
		//printContent();
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Abmeldung auf die Zuliefererinformationen eines bestimmten Datenverteilers
	 * eingetroffen ist. Zuerst wird der Eintrag jedes in ids spezifizierten Datenverteilers aus der Tabelle bestimmt. Wenn ein Eintrag vorhanden ist, dann wird
	 * überprüft, ob der Datenverteiler, der hier die Abmeldung geschickt hat, als Abonnent existiert. Ist dies der Fall, wird er aus der Abonnentenliste
	 * entfernt.
	 *
	 * @param transmitterId ID des DAV
	 * @param ids           long Array mit den IDs, enthält die Liste der DAVs
	 */
	final void unsubscribe(final long transmitterId, final long[] ids) {
		if(ids == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if((transmitterId == -1) || (transmitterId == _connectionsManager.getTransmitterId())) {
			throw new IllegalArgumentException("Argument ist inkonsistent");
		}
		for(int i = ids.length - 1; i > -1; --i) {
			final Long key = ids[i];
			final TransmitterSubscriptionInfos entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(key);
			if(entry != null) {
				entry._subscribers.remove(transmitterId);
			}
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Kündigung eines Zulieferers auf die Zuliefererinformationen eines bestimmten
	 * Datenverteilers eingetroffen ist. Zuerst wird der Eintrag jedes in ids spezifizierten Datenverteilers aus der Tabelle bestimmt. Wenn ein Eintrag vorhanden
	 * ist, dann wird überprüft, ob der Datenverteiler, der hier die Kündigung geschickt hat, auch der eingetragene Zulieferer ist. Mit Hilfe des Wegmanagers wird
	 * überprüft, ob ein neuer Zulieferer in Frage kommt. Existiert ein anderer Zulieferer, dann wird ihm eine Zuliefererinformationsanmeldung gesendet. Wenn kein
	 * neuer Zulieferer existiert, dann wird eine Zuliefererinformationskündigung an die Abonnenten gesendet. Abonnentenliste, Objektliste und Kombinationsliste
	 * werden dann geleert.
	 *
	 * @param transmitterId ID des DAV
	 * @param ids           long Array mit den IDs, enthält die Liste der DAVs
	 */
	final void unsubscribeDeliverer(final long transmitterId, final long[] ids) {
		if(ids == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if((transmitterId == -1) || (transmitterId == _connectionsManager.getTransmitterId())) {
			throw new IllegalArgumentException("Argument ist inkonsistent");
		}
		final Hashtable<Long, ArrayList<Long>> tmp = new Hashtable<Long, ArrayList<Long>>();
		for(int i = ids.length - 1; i > -1; --i) {
			final Long key = ids[i];
			final TransmitterSubscriptionInfos entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(key);
			if(entry != null) {
				synchronized(entry) {
					if(entry._delivererId == transmitterId) {
						final long[] _ids = {entry._transmitterId};
						final long way = _bestWayManager.getBestWay(ids[i]);
						if((way != -1) && (way != _localTransmitterId) && (way != entry._delivererId)) {
							entry._delivererId = way;
							ArrayList<Long> list = tmp.get(way);
							if(list == null) {
								list = new ArrayList<Long>();
								tmp.put(way, list);
							}
							list.add(entry._transmitterId);
						}
						else {
							entry._delivererId = -1;
							final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(_ids);
							for(final Long subscriber : entry._subscribers) {
								if(subscriber != null) {
									final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
									if(connection != null) {
//										System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
										connection.sendTelegram(transmitterListsDeliveryUnsubscription);
									}
								}
							}
							entry._subscribers.clear();
							entry._objectIdSet.clear();
							entry._atgUsageSet.clear();
						}
					}
				}
			}
		}
		if(tmp.size() > 0) {
			final Enumeration<Long> enumeration = tmp.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> list = tmp.get(way);
					if(list != null) {
						final int length = list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = list.get(i);
							}
							final DelayedSubscriptionHandle delayedSubscriptionHandle = new DelayedSubscriptionHandle(way, _ids);
							synchronized(_delayedSubscriptionList) {
								_delayedSubscriptionList.add(delayedSubscriptionHandle);
							}
						}
					}
				}
				synchronized(_delayedSubscriptionSync) {
					_delayedSubscriptionSync.notifyAll();
				}
			}
		}
		//printContent();
	}


	@Override
	public final void handleDisconnection(final long transmitterId) {
		if(transmitterId == -1) {
			// Kann passieren, wenn eine Verbindung zu einem anderen Datenverteiler während der Initialisierungsphase unterbrochen wurde
			// In diesem Fall kann es noch keine Einträge in der Anmeldelistenverwaltung geben
			return;
		}
		final ArrayList<TransmitterSubscriptionInfos> transmitterSubscriptionInfoList;
		synchronized(_subscriptionInfos) {
			transmitterSubscriptionInfoList = new ArrayList<TransmitterSubscriptionInfos>(_subscriptionInfos.values());
		}
		final Hashtable<Long, ArrayList<Long>> tmp1 = new Hashtable<Long, ArrayList<Long>>();
		final Hashtable<Long, ArrayList<Long>> tmp2 = new Hashtable<Long, ArrayList<Long>>();
		for(final TransmitterSubscriptionInfos entry : transmitterSubscriptionInfoList) {
			if(entry != null) {
				synchronized(entry) {
					if(entry._transmitterId == transmitterId) {
						final long way = _bestWayManager.getBestWay(entry._transmitterId);
						if((way != -1) && (way != _connectionsManager.getTransmitterId()) && (way != entry._delivererId)) {
							ArrayList<Long> _list = tmp2.get(way);
							if(_list == null) {
								_list = new ArrayList<Long>();
								tmp2.put(way, _list);
							}
							_list.add(entry._transmitterId);
						}
						else {
							if(entry._delivererId != transmitterId) {
								final Long _key = entry._delivererId;
								ArrayList<Long> _list = tmp1.get(_key);
								if(_list == null) {
									_list = new ArrayList<Long>();
									tmp1.put(_key, _list);
								}
								_list.add(entry._transmitterId);
							}
							entry._delivererId = -1;
							final long[] ids = {entry._transmitterId};
							final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(ids);
							for(final Long subscriber : entry._subscribers) {
								if(subscriber != null) {
									final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
									if(connection != null) {
//										System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
										connection.sendTelegram(transmitterListsDeliveryUnsubscription);
									}
								}
							}
							entry._objectIdSet.clear();
							entry._atgUsageSet.clear();
							synchronized(_subscriptionInfos) {
								_subscriptionInfos.remove(entry._transmitterId);
							}
						}
					}
					else if(entry._delivererId == transmitterId) {
						// the transmitter with the id kann no longer deliver the list of the specified transmitters
						final long way = _bestWayManager.getBestWay(entry._transmitterId);
						if((way != -1) && (way != _connectionsManager.getTransmitterId()) && (way != entry._delivererId)) {
							ArrayList<Long> _list = tmp2.get(way);
							if(_list == null) {
								_list = new ArrayList<Long>();
								tmp2.put(way, _list);
							}
							_list.add(entry._transmitterId);
						}
						else {
							entry._delivererId = -1;
							final long[] ids = {entry._transmitterId};
							final TransmitterListsDeliveryUnsubscription transmitterListsDeliveryUnsubscription = new TransmitterListsDeliveryUnsubscription(ids);
							for(final Long subscriber : entry._subscribers) {
								if(subscriber != null) {
									final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
									if(connection != null) {
//										System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsDeliveryUnsubscription);
										connection.sendTelegram(transmitterListsDeliveryUnsubscription);
									}
								}
							}
							entry._subscribers.clear();
							entry._objectIdSet.clear();
							entry._atgUsageSet.clear();
						}
					}
					else {
						entry._subscribers.remove(transmitterId);
					}
				}
			}
		}
		if(tmp1.size() > 0) {
			final Enumeration<Long> enumeration = tmp1.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> _list = tmp1.get(way);
					if(_list != null) {
						final int length = _list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = _list.get(i);
							}
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(way);
							if(connection != null) {
								final TransmitterListsUnsubscription transmitterListsUnsubscription = new TransmitterListsUnsubscription(_ids);
//								System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUnsubscription);
								connection.sendTelegram(transmitterListsUnsubscription);
							}
						}
					}
				}
			}
		}
		if(tmp2.size() > 0) {
			final Enumeration<Long> enumeration = tmp2.keys();
			if(enumeration != null) {
				while(enumeration.hasMoreElements()) {
					final Long way = enumeration.nextElement();
					final ArrayList<Long> _list = tmp2.get(way);
					if(_list != null) {
						final int length = _list.size();
						if(length > 0) {
							final long[] _ids = new long[length];
							for(int i = 0; i < length; ++i) {
								_ids[i] = _list.get(i);
							}
							final DelayedSubscriptionHandle delayedSubscriptionHandle = new DelayedSubscriptionHandle(way, _ids);
							synchronized(_delayedSubscriptionList) {
								_delayedSubscriptionList.add(delayedSubscriptionHandle);
							}
						}
					}
				}
				synchronized(_delayedSubscriptionSync) {
					_delayedSubscriptionSync.notifyAll();
				}
			}
		}
		//printContent();
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Zuliefereraktualisierung eintrifft. Bei dem Eintrag des spezifizierten
	 * Datenverteilers werden die Objekte und die Kombinationen aus Attributgruppen und Aspekten aktualisiert. Dabei werden vier Listen aufgebaut: - Liste der
	 * hinzugefügten Objekte - Liste der gelöschten Objekte - Liste der hinzugefügten Kombinationen - Liste der gelöschten Kombinationen aus Attributgruppen und
	 * Aspekten Diese Listen werden dann in eine Zuliefereraktualisierungsnachricht verpackt und an die Abonnenten weitergeleitet. Am Ende werden diese Listen
	 * zurückgegeben.
	 */
	final void updateEntry(final TransmitterListsUpdate transmitterListsUpdate) {
		if(transmitterListsUpdate == null) {
			throw new IllegalArgumentException("Argument ist null");
		}

		final Long key = transmitterListsUpdate.getTransmitterId();
		final TransmitterSubscriptionInfos entry;
		synchronized(_subscriptionInfos) {
			entry = (TransmitterSubscriptionInfos)_subscriptionInfos.get(key);
		}
		if(entry != null) {
			synchronized(entry) {
				final long[] objectIdsToRemove = transmitterListsUpdate.getObjectsToRemove();
				final long[] objectIdsToAdd = transmitterListsUpdate.getObjectsToAdd();
				final AttributeGroupAspectCombination[] attributeGroupAspectToRemove = transmitterListsUpdate.getAttributeGroupAspectsToRemove();
				final AttributeGroupAspectCombination[] attributeGroupAspectToAdd = transmitterListsUpdate.getAttributeGroupAspectsToAdd();

				final ArrayList<AttributeGroupAspectCombination> removedAttributeGroupAspects = new ArrayList<AttributeGroupAspectCombination>();
				final ArrayList<AttributeGroupAspectCombination> addedAttributeGroupAspects = new ArrayList<AttributeGroupAspectCombination>();
				final ArrayList<Long> removedObjects = new ArrayList<Long>();
				final ArrayList<Long> addedObjects = new ArrayList<Long>();
				boolean changed = false;
				if(transmitterListsUpdate.isDeltaMessage()) {
					if(objectIdsToRemove != null) {
						for(int i = objectIdsToRemove.length - 1; i > -1; --i) {
							final Long object = objectIdsToRemove[i];
							if(entry._objectIdSet.remove(object)) {
								removedObjects.add(object);
								changed = true;
							}
						}
					}

					if(objectIdsToAdd != null) {
						for(int i = 0; i < objectIdsToAdd.length; ++i) {
							final Long object = objectIdsToAdd[i];
							entry._objectIdSet.add(object);
							addedObjects.add(object);
							changed = true;
						}
					}

					if(attributeGroupAspectToRemove != null) {
						for(int i = attributeGroupAspectToRemove.length - 1; i > -1; --i) {
							final AttributeGroupAspectCombination attributeGroupAspectCombination = attributeGroupAspectToRemove[i];
							final Long atgUsage = attributeGroupAspectCombination.getAtgUsageIdentification();
							if(entry._atgUsageSet.remove(atgUsage)) {
								removedAttributeGroupAspects.add(attributeGroupAspectCombination);
								changed = true;
							}
						}
					}

					if(attributeGroupAspectToAdd != null) {
						for(int i = 0; i < attributeGroupAspectToAdd.length; ++i) {
							final AttributeGroupAspectCombination attributeGroupAspectCombination = attributeGroupAspectToAdd[i];
							final Long atgUsage = attributeGroupAspectCombination.getAtgUsageIdentification();
							entry._atgUsageSet.add(atgUsage);
							addedAttributeGroupAspects.add(attributeGroupAspectCombination);
							changed = true;
						}
					}
				}
				else {
					entry._objectIdSet.clear();
					entry._atgUsageSet.clear();
					if(objectIdsToAdd != null) {
						for(int i = 0; i < objectIdsToAdd.length; ++i) {
							final Long object = objectIdsToAdd[i];
							entry._objectIdSet.add(object);
							addedObjects.add(object);
							changed = true;
						}
					}
					if(attributeGroupAspectToAdd != null) {
						for(int i = 0; i < attributeGroupAspectToAdd.length; ++i) {
							entry._atgUsageSet.add(attributeGroupAspectToAdd[i].getAtgUsageIdentification());
							addedAttributeGroupAspects.add(attributeGroupAspectToAdd[i]);
							changed = true;
						}
					}
				}

				if(changed) {
					for(final Long subscriber : entry._subscribers) {
						if(subscriber != null) {
							final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
							if(connection != null) {
								sendTransmitterUpdates(
										connection,
										transmitterListsUpdate.getTransmitterId(),
										transmitterListsUpdate.isDeltaMessage(),
										addedObjects,
										removedObjects,
										addedAttributeGroupAspects,
										removedAttributeGroupAspects
								);
							}
						}
					}
					return;
				}
			}
		}
	}

	/**
	 * Neue Anmeldung beim lokalen Datenverteiler in die Anmeldelistenverwaltung aufnehmen. Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Quell- oder Senkenanmeldung auf ein bestimmtes Datum erfolgt ist. Im Antrag des
	 * lokalen Datenverteilers wird überprüft, ob das Objekt und die Kombination aus Attributgruppe und Aspekt aus der spezifizierten Basisanmeldeinformation
	 * vorhanden sind. Wenn das Objekt oder die Kombination nicht vorhanden ist, wird eine Zuliefereraktualisierung mit den dazugekommenen Objekt und/oder der
	 * dazugekommenen Kombination gebildet und zu den Abonnenten gesendet. Wenn das Objekt oder die Kombination vorhanden ist, dann wird der jeweilige Objekt- bzw.
	 * Kombinationszähler um eins erhöht.
	 *
	 * @param info Anmeldeinformationen
	 */
	final void addInfo(final BaseSubscriptionInfo info) {
		if(info == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		_localSubscriptionInfos.addSubscribeInfo(info);
	}


	/**
	 * Anmeldung beim lokalen Datenverteiler aus der Anmeldelistenverwaltung entfernen. Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Quell- oder Senkenabmeldung auf ein bestimmtes Datum erfolgt ist. Im Antrag des
	 * lokalen Datenverteilers wird überprüft ob das Objekt und die Kombination aus Attributgruppe und Aspekt aus der spezifizierten Basisanmeldeinformation
	 * vorhanden sind. Wenn das Objekt oder die Kombination vorhanden ist, wird deren Referenzzähler um eins vermindert. Wenn der Referenzzähler null ist, wird
	 * eine Zuliefereraktualisierung mit dem gelöschten Objekt und/oder der gelöschten Kombination gebildet, und zu den Abonnenten gesendet.
	 *
	 * @param info Anmeldeinformationen
	 */
	final void removeInfo(final BaseSubscriptionInfo info) {
		if(info == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		_localSubscriptionInfos.removeSubscribeInfo(info);
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um die potentiellen Zentraldatenverteiler des spezifizierten Datums zu bestimmen. In den
	 * Anmeldelisten der erreichbaren Datenverteiler wird überprüft, ob gewünschte Objekt und die Kombination aus Attributgruppe und Aspekt enthalten ist.
	 *
	 * @param info Anmeldeinformationen
	 *
	 * @return Feld mit den potentiellen Zentraldatenverteilern. Wenn kein Datenverteiler gefunden wurde, dann wird Null zurückgegeben.
	 */
	final long[] getPotentialCentralDavs(final BaseSubscriptionInfo info) {
//		System.out.println("getPotentialTransmitters: " + info);
		if(info == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		final long requiredObjectId = info.getObjectID();
		final long requiredAtgUsageId = info.getUsageIdentification();

		final long[] potentialTransmitters = new long[_subscriptionInfos.size()];
		int numberOfPotentialTransmitters = 0;
		Collection<TransmitterSubscriptionInfos> values;
		synchronized(_subscriptionInfos) {
			values = new ArrayList<TransmitterSubscriptionInfos>(_subscriptionInfos.values());
		}
		// Nicht synchronisiert ausführen, Deadlock-Gefahr
		for(final TransmitterSubscriptionInfos transmitterSubscriptionInfos : values) {
			if(transmitterSubscriptionInfos == _localTransmitterSubscriptionInfos) continue;
			if(transmitterSubscriptionInfos.isPotentialCentralDav(requiredObjectId, requiredAtgUsageId)) {
				potentialTransmitters[numberOfPotentialTransmitters++] = transmitterSubscriptionInfos._transmitterId;
			}
		}
		if(numberOfPotentialTransmitters == 0) {
//			System.out.println("potentialTransmitters = " + null);
			return null;
		}
		final long[] result = new long[numberOfPotentialTransmitters];
		System.arraycopy(potentialTransmitters, 0, result, 0, numberOfPotentialTransmitters);
//		System.out.println("potentialTransmitters " + _localTransmitterId + ": " + potentialTransmitters.length);
		return result;

	}

	/**
	 * Diese Methode gibt die vom ListsManager beanspruchten Resourcen wieder frei. Sie wird von der Verbindungsverwaltung aufgerufen, wenn der Datenverteiler
	 * beendet werden soll. Die im Konstruktor gestarteten Threads werden terminiert.
	 */
	final void close() {
		if(_delayedSubscriptionThread != null) {
			_delayedSubscriptionThread.interrupt();
		}
		_localSubscriptionInfos.close();
	}

	/**
	 * Diese Methode sendet die Aktualisierungstelegramme, welche die Ergänzungen der Listen beinhalten.
	 *
	 * @param connection    Verbindung zwischen zwei DAV
	 * @param transmitterId ID des DAV
	 * @param objToAdd      Liste der zu addierenden Objekte
	 * @param combiToAdd    Liste der zu addierenden Attributgruppen-Aspekt-Kombinationen
	 */
	private void sendTransmitterUpdates(
			final T_T_HighLevelCommunicationInterface connection, final long transmitterId, final List<Long> objToAdd, final List<Long> combiToAdd) {
		int objToAddPos = 0;
		int combiToAddPos = 0;
		boolean processObjToAdd = true;
		boolean processCombiToAdd = true;

		// delta ist beim ersten Schleifendurchlauf false, weitere Durchläufe sind als updates zu versenden
		boolean delta = false;
		do {
			// Objects to add
			long[] objectsToAdd = null;
			if(processObjToAdd) {
				if(objToAdd != null) {
					final int size = objToAdd.size();
					if(size > 0) {
						if(size > 3600) {
							final int rest = size - objToAddPos;
							// maximal 3600 Objekte je Telegramm hinzufügen, damit die Telegrammgröße nicht überschritten wird
							final int length = (rest > 3600 ? 3600 : rest);
							objectsToAdd = new long[length];
							for(int j = objToAddPos, k = 0; k < length; ++j, ++k) {
								objectsToAdd[k] = objToAdd.get(j);
							}
							objToAddPos += length;
							if(objToAddPos >= size) {
								processObjToAdd = false;
							}
						}
						else {
							objectsToAdd = new long[size];
							for(int j = 0; j < size; ++j) {
								objectsToAdd[j] = objToAdd.get(j);
							}
							processObjToAdd = false;
						}
					}
					else {
						processObjToAdd = false;
					}
				}
				else {
					processObjToAdd = false;
				}
			}

			// Combinations to add
			AttributeGroupAspectCombination[] attributeGroupAspectsToAdd = null;
			if(processCombiToAdd) {
				if(combiToAdd != null) {
					final int size = combiToAdd.size();
					if(size > 0) {
						// maximal 300 Attributgruppen-Aspekt-Kombinationen je Telegramm hinzufügen, damit die Telegrammgröße nicht überschritten wird
						if(size > 300) {
							final int rest = size - combiToAddPos;
							final int length = (rest > 300 ? 300 : rest);
							attributeGroupAspectsToAdd = new AttributeGroupAspectCombination[length];
							for(int j = combiToAddPos, k = 0; k < length; ++j, ++k) {
								attributeGroupAspectsToAdd[k] = new AttributeGroupAspectCombination(combiToAdd.get(j));
							}
							combiToAddPos += length;
							if(combiToAddPos >= size) {
								processCombiToAdd = false;
							}
						}
						else {
							attributeGroupAspectsToAdd = new AttributeGroupAspectCombination[size];
							for(int j = 0; j < size; ++j) {
								attributeGroupAspectsToAdd[j] = new AttributeGroupAspectCombination(combiToAdd.get(j));
							}
							processCombiToAdd = false;
						}
					}
					else {
						processCombiToAdd = false;
					}
				}
				else {
					processCombiToAdd = false;
				}
			}

			if((objectsToAdd != null) || (attributeGroupAspectsToAdd != null)) {
				final TransmitterListsUpdate transmitterListsUpdate = new TransmitterListsUpdate(
						transmitterId, delta, objectsToAdd, null, attributeGroupAspectsToAdd, null
				);
//				System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUpdate);
				connection.sendTelegram(transmitterListsUpdate);
			}
			else {
				break;
			}
			delta = true;
		}
		while(processObjToAdd || processCombiToAdd);
	}

	/**
	 * * Diese Methode sendet die Aktualisierungstelegramme, welche die Ergänzungen und Löschungen der Listen beinhalten.
	 *
	 * @param connection    Verbindung zwischen zwei DAV
	 * @param transmitterId ID des DAV
	 * @param update        true,false
	 * @param objToAdd      Liste der zu addierenden Objekte
	 * @param objToRemove   Liste der zu entfernenden Objekte
	 * @param combiToAdd    Liste der zu addierenden Attributgruppen-Aspekt-Kombinationen
	 * @param combiToRemove Liste der zu entfernenden Attributgruppen-Aspekt-Kombinationen
	 */
	private void sendTransmitterUpdates(
			final T_T_HighLevelCommunicationInterface connection,
			final long transmitterId,
			boolean update,
			final List<Long> objToAdd,
			final List<Long> objToRemove,
			final List<AttributeGroupAspectCombination> combiToAdd,
			final List<AttributeGroupAspectCombination> combiToRemove) {
		int objToAddPos = 0;
		int objToRemovePos = 0;
		int combiToAddPos = 0;
		int combiToRemovePos = 0;
		boolean processObjToAdd = true;
		boolean processObjToRemove = true;
		boolean processCombiToAdd = true;
		boolean processCombiToRemove = true;

		do {
			// Objects to add
			long[] objectsToAdd = null;
			if(processObjToAdd) {
				if(objToAdd != null) {
					final int size = objToAdd.size();
					if(size > 0) {
						if(size > 1800) {
							final int rest = size - objToAddPos;
							// maximal 1800 Objekte je Telegramm hinzufügen, damit die Telegrammgröße nicht überschritten wird
							final int length = (rest > 1800 ? 1800 : rest);
							objectsToAdd = new long[length];
							for(int j = objToAddPos, k = 0; k < length; ++j, ++k) {
								final Long objectId = objToAdd.get(j);
								if(objectId != null) {
									objectsToAdd[k] = objectId;
								}
							}
							objToAddPos += length;
							if(objToAddPos >= size) {
								processObjToAdd = false;
							}
						}
						else {
							objectsToAdd = new long[size];
							for(int j = 0; j < size; ++j) {
								final Long objectId = objToAdd.get(j);
								if(objectId != null) {
									objectsToAdd[j] = objectId;
								}
							}
							processObjToAdd = false;
						}
					}
					else {
						processObjToAdd = false;
					}
				}
				else {
					processObjToAdd = false;
				}
			}
			// Objects to remove
			long[] objectsToRemove = null;
			if(processObjToRemove) {
				if(objToRemove != null) {
					final int size = objToRemove.size();
					if(size > 0) {
						if(size > 1800) {
							final int rest = size - objToRemovePos;
							// maximal 1800 Objekte je Telegramm entfernen, damit die Telegrammgröße nicht überschritten wird
							final int length = (rest > 1800 ? 1800 : rest);
							objectsToRemove = new long[length];
							for(int j = objToRemovePos, k = 0; k < length; ++j, ++k) {
								final Long objectId = objToRemove.get(j);
								if(objectId != null) {
									objectsToRemove[k] = objectId;
								}
							}
							objToRemovePos += length;
							if(objToRemovePos >= size) {
								processObjToRemove = false;
							}
						}
						else {
							objectsToRemove = new long[size];
							for(int j = 0; j < size; ++j) {
								final Long objectId = objToRemove.get(j);
								if(objectId != null) {
									objectsToRemove[j] = objectId;
								}
							}
							processObjToRemove = false;
						}
					}
					else {
						processObjToRemove = false;
					}
				}
				else {
					processObjToRemove = false;
				}
			}
			// Combinations to add
			AttributeGroupAspectCombination[] attributeGroupAspectsToAdd = null;
			if(processCombiToAdd) {
				if(combiToAdd != null) {
					final int size = combiToAdd.size();
					if(size > 0) {
						if(size > 150) {
							final int rest = size - combiToAddPos;
							// maximal 150 Attributgruppen-Aspekt-Kombinationen je Telegramm hinzufügen, damit die Telegrammgröße nicht überschritten wird
							final int length = (rest > 150 ? 150 : rest);
							attributeGroupAspectsToAdd = new AttributeGroupAspectCombination[length];
							for(int j = combiToAddPos, k = 0; k < length; ++j, ++k) {
								attributeGroupAspectsToAdd[k] = combiToAdd.get(j);
							}
							combiToAddPos += length;
							if(combiToAddPos >= size) {
								processCombiToAdd = false;
							}
						}
						else {
							attributeGroupAspectsToAdd = new AttributeGroupAspectCombination[size];
							for(int j = 0; j < size; ++j) {
								attributeGroupAspectsToAdd[j] = combiToAdd.get(j);
							}
							processCombiToAdd = false;
						}
					}
					else {
						processCombiToAdd = false;
					}
				}
				else {
					processCombiToAdd = false;
				}
			}
			// Combinations to remove
			AttributeGroupAspectCombination[] attributeGroupAspectsToRemove = null;
			if(processCombiToRemove) {
				if(combiToRemove != null) {
					final int size = combiToRemove.size();
					if(size > 0) {
						if(size > 150) {
							final int rest = size - combiToRemovePos;
							// maximal 150 Attributgruppen-Aspekt-Kombinationen je Telegramm entfernen, damit die Telegrammgröße nicht überschritten wird
							final int length = (rest > 150 ? 150 : rest);
							attributeGroupAspectsToRemove = new AttributeGroupAspectCombination[length];
							for(int j = combiToRemovePos, k = 0; k < length; ++j, ++k) {
								attributeGroupAspectsToRemove[k] = combiToRemove.get(j);
							}
							combiToRemovePos += length;
							if(combiToRemovePos >= size) {
								processCombiToRemove = false;
							}
						}
						else {
							attributeGroupAspectsToRemove = new AttributeGroupAspectCombination[size];
							for(int j = 0; j < size; ++j) {
								attributeGroupAspectsToRemove[j] = combiToRemove.get(j);
							}
							processCombiToRemove = false;
						}
					}
					else {
						processCombiToRemove = false;
					}
				}
				else {
					processCombiToRemove = false;
				}
			}

			if((objectsToAdd != null) || (objectsToRemove != null) || (attributeGroupAspectsToAdd != null) || (attributeGroupAspectsToRemove != null)) {
				final TransmitterListsUpdate transmitterListsUpdate = new TransmitterListsUpdate(
						transmitterId, update, objectsToAdd, objectsToRemove, attributeGroupAspectsToAdd, attributeGroupAspectsToRemove
				);
//				System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsUpdate);
				connection.sendTelegram(transmitterListsUpdate);
			}
			else {
				break;
			}
			// weitere Schleifendurchläufe sind unabhängig vom vorgegebenen delta-Flag als Updates zu verstehen:
			update = true;
		}
		while(processObjToAdd || processObjToRemove || processCombiToAdd || processCombiToRemove);
	}

	/**
	 * Diese Methode entfernt die wartenden verzögerten Anmeldungen.
	 *
	 * @param ids long Array mit den IDs der zu löschenden Datenverteiler
	 */
	private void cleanPendingDelayedSubscriptions(final long[] ids) {
		synchronized(_delayedSubscriptionList) {
			for(int i = 0; i < _delayedSubscriptionList.size(); ++i) {
				final DelayedSubscriptionHandle handle = _delayedSubscriptionList.get(i);
				if(handle != null) {
					int newLength = handle._ids.length;
					for(int j = 0; j < handle._ids.length; ++j) {
						final long id = handle._ids[j];
						for(int k = 0; k < ids.length; ++k) {
							if(ids[k] == id) {
								handle._ids[j] = -1;
								--newLength;
								break;
							}
						}
					}
					if(newLength != handle._ids.length) {
						if(newLength > 0) {
							final long[] newIds = new long[newLength];
							for(int j = 0, k = 0; j < handle._ids.length; ++j) {
								if(handle._ids[j] != -1) {
									newIds[k++] = handle._ids[j];
								}
							}
						}
						else {
							_delayedSubscriptionList.remove(i);
						}
					}
				}
			}
		}
	}

	public void dumpSubscriptionLists() {
		for(final Map.Entry<Long, TransmitterSubscriptionInfos> transmitterSubscriptionInfosEntry : _subscriptionInfos.entrySet()) {
			final TransmitterSubscriptionInfos subscriptionInfos = transmitterSubscriptionInfosEntry.getValue();
			System.out.println(subscriptionInfos.toShortString());
		}
	}

	/** Enthält die Anmeldungsliste eines erreichbaren Datenverteilers. */
	private class TransmitterSubscriptionInfos {

		/** Die ID des Datenverteilers auf den sich diese Anmeldeliste bezieht. */
		long _transmitterId;

		/** Die ID des Datenverteiler, der diese Anmeldeliste liefert. */
		long _delivererId = -1;

		/** Ids der Datenverteiler, an die die Anmeldungsliste weitergegeben werden soll */
		List<Long> _subscribers;

		Set<Long> _objectIdSet = new HashSet<Long>();
		Set<Long> _atgUsageSet = new HashSet<Long>();

		/**
		 * Erzeugt eine neue Anmeldungsliste für einen erreichbaren Datenverteiler
		 *
		 * @param transmitterId Die ID des Datenverteilers auf den sich diese Anmeldeliste bezieht.
		 */
		TransmitterSubscriptionInfos(final long transmitterId) {
			_transmitterId = transmitterId;
			_subscribers = new CopyOnWriteArrayList<Long>();
		}

		public boolean isPotentialCentralDav(final long requiredObjectId, final long requiredAtgUsageId) {
//			System.out.println("isPotentialTransmitter " + _localTransmitterId + ", requiredObjectId = " + requiredObjectId + ", requiredAtgUsageId = " + requiredAtgUsageId);
//			System.out.println("_objectIdSet = " + _objectIdSet);
//			System.out.println("_atgUsageSet = " + _atgUsageSet);
			synchronized(this) {
				return (requiredObjectId == 0 || _objectIdSet.contains(requiredObjectId)) && (requiredAtgUsageId == 0 || _atgUsageSet.contains(requiredAtgUsageId));
			}
		}

		public void updateInfosAndPublish(
				final List<Long> addedObjectIds, final List<Long> removedObjectIds, final List<Long> addedAtgUsageIds, final List<Long> removedAtgUsageIds) {
			if(addedObjectIds.isEmpty() && (removedObjectIds == null || removedObjectIds.isEmpty()) && addedAtgUsageIds.isEmpty() && (removedAtgUsageIds== null || removedObjectIds.isEmpty())) {
				return;
			}
			synchronized(this) {
				if(removedObjectIds != null) _objectIdSet.removeAll(removedObjectIds);
				_objectIdSet.addAll(addedObjectIds);
				if(removedAtgUsageIds != null) _atgUsageSet.removeAll(removedAtgUsageIds);
				_atgUsageSet.addAll(addedAtgUsageIds);
				
				for(final Long subscriber : _subscribers) {
					final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(subscriber);
					if(connection != null) {
						sendTransmitterUpdates(
								connection,
								_localTransmitterId,
								true,
								addedObjectIds,
								removedObjectIds,
								convertListOfLongsToListOfAttributeGroupAspectCombinations(addedAtgUsageIds),
								convertListOfLongsToListOfAttributeGroupAspectCombinations(removedAtgUsageIds)
						);
					}
				}
			}
		}

		private List<AttributeGroupAspectCombination> convertListOfLongsToListOfAttributeGroupAspectCombinations(final List<Long> atgUsageIds) {
			if(atgUsageIds== null) return null;
			final ArrayList<AttributeGroupAspectCombination> result = new ArrayList<AttributeGroupAspectCombination>(atgUsageIds.size());
			for(final Long atgUsageId : atgUsageIds) {
				result.add(new AttributeGroupAspectCombination(atgUsageId));
			}
			return result;
		}

		public final String toString() {
			String str = "Listeneintrag: \n";
			str += "Datenverteiler: " + _transmitterId + "\n";
			str += "Zulieferer: " + _delivererId + "\n";
			str += "Abonnenten: [";
			for(int i = 0; i < _subscribers.size(); ++i) {
				final Long abo = (Long)_subscribers.get(i);
				if(abo != null) {
					str += "\t" + abo;
				}
			}
			str += "]\n";
			str += "Objekte: " + _objectIdSet + "\n";
			str += "Attributesgruppen-Aspekt Kombinationen: " + _atgUsageSet + "\n";
			return str;
		}

		public String toShortString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("DAV ").append(_transmitterId);
			builder.append(" von: ").append(_delivererId);
			builder.append(" an { ");
			for(int i = 0; i < _subscribers.size(); ++i) {
				final Long abo = (Long)_subscribers.get(i);
				if(abo != null) {
					builder.append(abo.longValue()).append(" ");
				}
			}
			builder.append("}\n").append("  objs: ").append(_objectIdSet).append("\n").append("  atgu: ").append(_atgUsageSet);
			return builder.toString();
		}
	}

	private class DelayedSubscriptionHandle {

		long _way;

		long[] _ids;

		long _time;

		public DelayedSubscriptionHandle(final long way, final long[] ids) {
			_way = way;
			_ids = ids;
			_time = System.currentTimeMillis();
		}

		public final boolean isOverTime(final long t) {
			return (t - _time) > DELAYED_SUBSCRIPTION_TIME_LIMIT;
		}

		public final void work() {
			final T_T_HighLevelCommunicationInterface connection = _connectionsManager.getTransmitterConnectionFromId(_way);
			if(connection != null) {
				final TransmitterListsSubscription transmitterListsSubscription = new TransmitterListsSubscription(_ids);
//				System.out.println("Sende: " + _localTransmitterId + ", " + transmitterListsSubscription);
				connection.sendTelegram(transmitterListsSubscription);
			}
		}
	}

	/** TBD Beschreibung */
	private class DelayedSubscriptionThread extends Thread {

		public DelayedSubscriptionThread() {
			super("DelayedSubscriptionThread");
		}

		@Override
		public final void run() {
			while(!interrupted()) {
				try {
					synchronized(_delayedSubscriptionSync) {
						while(_delayedSubscriptionList.size() <= 0) _delayedSubscriptionSync.wait();
					}
					while(_delayedSubscriptionList.size() > 0) {
						synchronized(_delayedSubscriptionList) {
							final long time = System.currentTimeMillis();
							for(int i = 0; i < _delayedSubscriptionList.size(); ++i) {
								final DelayedSubscriptionHandle handle = _delayedSubscriptionList.get(i);
								if(handle != null) {
									if(handle.isOverTime(time)) {
										handle.work();
										_delayedSubscriptionList.remove(i);
									}
								}
							}
						}

						sleep(1000);
					}
				}
				catch(Exception ex) {
					break;
				}
			}
		}
	}

	private static class ReferenceCount {

		private int _referenceCount;


		public ReferenceCount(final int referenceCount) {
			_referenceCount = referenceCount;
		}

		public int getReferenceCount() {
			return _referenceCount;
		}

		public int incrementReferenceCount() {
			return ++_referenceCount;
		}

		public int decrementReferenceCount() {
			return --_referenceCount;
		}


		public String toString() {
			return "(" + _referenceCount + ")";
		}
	}

	private class LocalSubscriptionInfos {
		private final DelayedTrigger _localSubscriptionAddedTrigger;
		private final DelayedTrigger _localSubscriptionRemovedTrigger;
		private final Map<Long, ReferenceCount> _objectIds = new HashMap<Long, ReferenceCount>();
		private final Map<Long, ReferenceCount> _changedObjectIds = new HashMap<Long, ReferenceCount>();
		private final Map<Long, ReferenceCount> _atgUsageIds = new HashMap<Long, ReferenceCount>();
		private final Map<Long, ReferenceCount> _changedAtgUsageIds = new HashMap<Long, ReferenceCount>();

		public LocalSubscriptionInfos() {
			_localSubscriptionAddedTrigger = new DelayedTrigger("SubscriptionAddedTrigger", 1700, 1000, 2500);
			_localSubscriptionAddedTrigger.addTriggerTarget(new AddedTriggerTarget());
			_localSubscriptionRemovedTrigger = new DelayedTrigger("SubscriptionRemovedTrigger", 1000000, 30000, 60000);
			_localSubscriptionRemovedTrigger.addTriggerTarget(new RemovedTriggerTarget());
		}

		public void close() {
			_localSubscriptionAddedTrigger.close();
			_localSubscriptionRemovedTrigger.close();
		}

		public void addSubscribeInfo(final BaseSubscriptionInfo info) {
			final long objectId = info.getObjectID();
			final long atgUsageId = info.getUsageIdentification();
			synchronized(this) {
				if(incrementReferenceCount(_objectIds, objectId)) {
					incrementReferenceCount(_changedObjectIds, objectId);
					_localSubscriptionAddedTrigger.trigger();
				}
				if(incrementReferenceCount(_atgUsageIds, atgUsageId)) {
					incrementReferenceCount(_changedAtgUsageIds, atgUsageId);
					_localSubscriptionAddedTrigger.trigger();
				}
			}
		}

		public void removeSubscribeInfo(final BaseSubscriptionInfo info) {
			final long objectId = info.getObjectID();
			final long atgUsageId = info.getUsageIdentification();
			synchronized(this) {
				if(decrementReferenceCount(_objectIds, objectId)) {
					decrementReferenceCount(_changedObjectIds, objectId);
					_localSubscriptionRemovedTrigger.trigger();
				}
				if(decrementReferenceCount(_atgUsageIds, atgUsageId)) {
					decrementReferenceCount(_changedAtgUsageIds, atgUsageId);
					_localSubscriptionRemovedTrigger.trigger();
				}
			}
		}

		private boolean incrementReferenceCount(final Map<Long, ReferenceCount> objectIds, final long objectId) {
			final ReferenceCount objectReferenceCount = objectIds.get(objectId);
			if(objectReferenceCount == null) {
				objectIds.put(objectId, new ReferenceCount(1));
				return true;
			}
			else {
				final int newRefCount = objectReferenceCount.incrementReferenceCount();
				if(newRefCount == 1) return true;
				if(newRefCount == 0) objectIds.remove(objectId);
				return false;
			}
		}

		private boolean decrementReferenceCount(final Map<Long, ReferenceCount> objectIds, final long objectId) {
			final ReferenceCount objectReferenceCount = objectIds.get(objectId);
			if(objectReferenceCount == null) {
				objectIds.put(objectId, new ReferenceCount(-1));
				return false;
			}
			else {
				if(objectReferenceCount.decrementReferenceCount() == 0) {
					objectIds.remove(objectId);
					return true;
				}
				return false;
			}
		}

		private class AddedTriggerTarget implements TriggerTarget {

			@Override
			public void shot() {
				try {
					// System.out.println("ListsManager$LocalSubscriptionInfos$AddedTriggerTarget.shot");
					// System.out.println("Thread.currentThread().getName() = " + Thread.currentThread().getName());
					synchronized(LocalSubscriptionInfos.this) {
						// System.out.println("_changedObjectIds = " + _changedObjectIds.size());
						// System.out.println("_changedAtgUsageIds = " + _changedAtgUsageIds.size());
					}
					long t0 = System.currentTimeMillis();
					synchronized(_localSubscriptionInfosSendLock) {
						final List<Long> addedAtgUsageIds = new ArrayList<Long>();
						final List<Long> addedObjectIds = new ArrayList<Long>();
						synchronized(LocalSubscriptionInfos.this) {
							extractAdditions(_changedObjectIds, addedObjectIds);
							extractAdditions(_changedAtgUsageIds, addedAtgUsageIds);
						}
						_localTransmitterSubscriptionInfos.updateInfosAndPublish(addedObjectIds, null, addedAtgUsageIds, null);
					}
//					System.out.println("shot = " + (System.currentTimeMillis() - t0));
//					System.out.println("addedObjectIds = " + addedObjectIds.size());
//					System.out.println("addedAtgUsageIds = " + addedAtgUsageIds.size());
//					System.out.println("");
				}
				catch(RuntimeException e) {
					_debug.warning("Fehler bei der asynchronen Aktualisierung der lokalen Anmeldelisten", e);
				}
			}

			private void extractAdditions(final Map<Long, ReferenceCount> changedObjectIds, final List<Long> addedObjects) {
				for(final Map.Entry<Long, ReferenceCount> changedEntry : changedObjectIds.entrySet()) {
					final int referenceCount = changedEntry.getValue().getReferenceCount();
					if(referenceCount > 0) addedObjects.add(changedEntry.getKey());
				}
				for(final Long addedObject : addedObjects) {
					changedObjectIds.remove(addedObject);
				}
			}

			@Override
			public void close() {
			}

		}

		private class RemovedTriggerTarget implements TriggerTarget {

			@Override
			public void shot() {
				try {
//					System.out.println("ListsManager$LocalSubscriptionInfos.shot");
//					System.out.println("Thread.currentThread().getName() = " + Thread.currentThread().getName());
//					synchronized(LocalSubscriptionInfos.this) {
//						System.out.println("_changedObjectIds = " + _changedObjectIds.size());
//						System.out.println("_changedAtgUsageIds = " + _changedAtgUsageIds.size());
//					}
//					long t0 = System.currentTimeMillis();
					synchronized(_localSubscriptionInfosSendLock) {
						final List<Long> removedAtgUsageIds = new ArrayList<Long>();
						final List<Long> addedAtgUsageIds = new ArrayList<Long>();
						final List<Long> removedObjectIds = new ArrayList<Long>();
						final List<Long> addedObjectIds = new ArrayList<Long>();
						synchronized(LocalSubscriptionInfos.this) {
							extractChanges(_changedObjectIds, addedObjectIds, removedObjectIds);
							extractChanges(_changedAtgUsageIds, addedAtgUsageIds, removedAtgUsageIds);
						}
						_localTransmitterSubscriptionInfos.updateInfosAndPublish(addedObjectIds, removedObjectIds, addedAtgUsageIds, removedAtgUsageIds);
					}
//					System.out.println("shot = " + (System.currentTimeMillis() - t0));
//					System.out.println("addedObjectIds = " + addedObjectIds.size());
//					System.out.println("removedObjectIds = " + removedObjectIds.size());
//					System.out.println("addedAtgUsageIds = " + addedAtgUsageIds.size());
//					System.out.println("removedAtgUsageIds = " + removedAtgUsageIds.size());
//					System.out.println("");
				}
				catch(RuntimeException e) {
					_debug.warning("Fehler bei der asynchronen Aktualisierung der lokalen Anmeldelisten", e);
				}
			}

			private void extractChanges(final Map<Long, ReferenceCount> changedObjectIds, final List<Long> addedObjects, final List<Long> removedObjects) {
				for(final Map.Entry<Long, ReferenceCount> changedEntry : changedObjectIds.entrySet()) {
					final int referenceCount = changedEntry.getValue().getReferenceCount();
					if(referenceCount > 0) addedObjects.add(changedEntry.getKey());
					else if(referenceCount < 0) removedObjects.add(changedEntry.getKey());
				}
				changedObjectIds.clear();
			}

			@Override
			public void close() {
			}

		}
	}

}
