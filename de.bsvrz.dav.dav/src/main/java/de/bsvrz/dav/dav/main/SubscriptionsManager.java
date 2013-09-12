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

import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.DataValue;
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.SendDataObject;
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.StreamFetcher;
import de.bsvrz.dav.daf.communication.lowLevel.TelegramUtility;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.AttributeGroupAspectCombination;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ReceiveSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SendSubscriptionInfo;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataSubscriptionReceipt;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionsConstants;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.sys.funclib.debug.Debug;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Diese Komponente ist für die Verwaltung der Anmeldungen von Applikationen und anderen Datenverteilern zuständig.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9180 $
 */
public class SubscriptionsManager implements SubscriptionsManagerMBean, SubscriptionsManagerTransactionInterface {

	private static final Debug _debug = Debug.getLogger();

	/**
	 * In dieser Tabelle werden die von außerhalb gestellten Anmeldungen je Kommunikationspartner gespeichert.
	 * In jedem Eintrag der Tabelle wird als Key die Objekt-Id des Kommunikationspartners verwendet. Für Datenverteiler ist das die ID des in der Topologie
	 * versorgten Datenverteilers (typ.datenverteiler), für Applikationen ist das die ID des dynamischen Objekts (typ.applikation), das vom Datenverteiler beim
	 * Verbindungsaufbau erzeugt wird. Für die Verbindung zur Konfiguration wird der Wert 0 verwendet. Für die interne Applikationsverbindung, die der
	 * Datenverteiler zu sich selbst aufbaut, wird die ID des eigenen Datenverteiler-Objekts verwendet.
	 * Über die Tabelleneinträge wird jedem Kommunikationspartner ein SubscriptionsFromRemoteStorage-Objekt zugeordnet, in dem alle Anmeldungen die von dem
	 * Kommunikationspartner beim lokalen Datenverteiler gemacht wurden, gespeichert werden.
	 * Für jede Applikationen wird ein Objekt der Klasse SubscriptionsFromApplicationStorage und für jeden anderen Datenverteiler ein Objekt der Klasse
	 * SubscriptionsFromDavStorage verwendet.
	 */
	private final Hashtable<Long, SubscriptionsFromRemoteStorage> _subscriptionsFromRemoteStorages;

	/**
	 * In dieser Tabelle werden die von außerhalb gestellten Anmeldungen je Datenidentifikation gespeichert.
	 * Als Key wird in dieser Tabelle ein BaseSubscriptionInfo-Objekt verwendet, in dem Objekt-Id, Attributgruppenverwendung (also Attributgruppe und Aspekt)
	 * und Simulationsvariante der jeweiligen Anmeldung enthalten sind.
	 * Jeder so spezifizierten Datenidentifikation wird über die Tabelle eine ArrayList zugeordnet, deren Einträge mit einer an den Datenverteiler gestellten
	 * Anmeldung korrespondieren. Jeder Eintrag dieser Liste enthält Informationen darüber, über den Zustand der Anmeldung, welcher Datenverteiler
	 * Zentraldatenverteiler ist und von wem die entsprechende Anmeldung gestellt wurde. Falls andere Datenverteiler als Zentraldatenverteiler in Frage
	 * kommen dann ist in einem der Listeneinträge eine Array mit Informationen zu Anmeldungen enthalten, die dieser Datenverteiler zu anderen Datenverteilern
	 * gesendet hat.
	 */
	private final Hashtable<BaseSubscriptionInfo, ArrayList<InAndOutSubscription>> _inOutSubscriptionsTable;

	/** Die Tabelle in der der Index der Datensätze verwaltet werden */
	private final Hashtable<BaseSubscriptionInfo, Integer> _dataIndexTable;

	/** Die Tabelle in der der Index der TeilDatensätze verwaltet werden */
	private final Hashtable<Long, Long> _usedDataIndexTable;

	/** Die verbindungsverwaltung */
	private final ConnectionsManager _connectionsManager;

	/** Der Zwichenspeicher der zerstückelten Telegramme */
	private final SplittedTransmitterTelegramsTable _splittedTelegramsTable;

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param connectionsManager Verbindungsverwaltung
	 */
	SubscriptionsManager(ConnectionsManager connectionsManager) {
		_connectionsManager = connectionsManager;
		_subscriptionsFromRemoteStorages = new Hashtable<Long, SubscriptionsFromRemoteStorage>();
		_inOutSubscriptionsTable = new Hashtable<BaseSubscriptionInfo, ArrayList<InAndOutSubscription>>();
		_dataIndexTable = new Hashtable<BaseSubscriptionInfo, Integer>();
		_usedDataIndexTable = new Hashtable<Long, Long>();
		_splittedTelegramsTable = new SplittedTransmitterTelegramsTable();
		registerMBean();
	}

	/**
	 * Registriert das Objekt als MBean zum Zugriff auf Debug-Informationen via jconsole
	 */
	private void registerMBean() {
		try {
			final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
			ObjectName helloName = new ObjectName("DAV:name=SubscriptionsManager");
			platformMBeanServer.registerMBean(this, helloName);
		}
		catch(Exception ignored) {
			_debug.warning("Wenn mehrere DAV in einer VM gestartet werden, dann wird nur der erste als MBean registriert", ignored);
		}
	}

	/** Dieser Konstruktor wird für Testzwecke benötigt. */
	public SubscriptionsManager() {
		_connectionsManager = null;
		_subscriptionsFromRemoteStorages = null;
		_inOutSubscriptionsTable = null;
		_dataIndexTable = null;
		_usedDataIndexTable = null;
		_splittedTelegramsTable = null;
	}

	/**
	 * Anmelden einer Anmeldekomponente
	 *
	 * @param subscriptionsFromRemoteStorage Die Anmeldekomponente
	 */
	public void subscribe(SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage) {
		if(subscriptionsFromRemoteStorage == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		ServerHighLevelCommunication connection = subscriptionsFromRemoteStorage.getConnection();
		if(connection != null) {
			Long key = new Long(connection.getId());
			_subscriptionsFromRemoteStorages.put(key, subscriptionsFromRemoteStorage);
		}
	}

	/**
	 * Löscht das Objekt mit allen Anmeldungen, die über eine Kommunikationsverbindung empfangen wurden, aus der entsprechenden Hash-Tabelle.
	 *
	 * @param subscriptionsFromRemoteStorage Das aus der Tabelle zu entfernende Objekt, in dem alle Anmeldungen der betroffenen Kommunikationsverbindung
	 * gespeichert wurden.
	 */
	public final void remove(SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage) {
		if(subscriptionsFromRemoteStorage == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		ServerHighLevelCommunication connection = subscriptionsFromRemoteStorage.getConnection();
		if(connection != null) {
			Long key = new Long(connection.getId());
			_subscriptionsFromRemoteStorages.remove(key);
		}
	}

	/**
	 * Gibt die Quelle der Applikationsverbindung zurück.
	 *
	 * @param info Anmeldeinformationen
	 *
	 * @return die Quelle der Applikationsverbindung
	 */
	final SubscriptionsFromApplicationStorage getSendingComponent(BaseSubscriptionInfo info) {
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		synchronized(_subscriptionsFromRemoteStorages) {
			ArrayList<SubscriptionsFromRemoteStorage> list = new ArrayList<SubscriptionsFromRemoteStorage>(_subscriptionsFromRemoteStorages.values());
			for(int i = 0; i < list.size(); ++i) {
				SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = list.get(i);
				if(subscriptionsFromRemoteStorage != null) {
					if(subscriptionsFromRemoteStorage.getType() == SubscriptionsFromRemoteStorage.T_A) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromApplicationStorage.canSend(info)) {
							return subscriptionsFromApplicationStorage;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Gibt die Senke für die übergene Anmeldeinformationen zurück.
	 *
	 * @param info Anmeldeinformationen
	 *
	 * @return Senke oder <code>null</code>, falls nicht vorhanden
	 */
	final SubscriptionsFromApplicationStorage getReceivingComponent(BaseSubscriptionInfo info) {
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		synchronized(_subscriptionsFromRemoteStorages) {
			ArrayList<SubscriptionsFromRemoteStorage> list = new ArrayList<SubscriptionsFromRemoteStorage>(_subscriptionsFromRemoteStorages.values());
			for(int i = 0; i < list.size(); ++i) {
				SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = list.get(i);
				if(subscriptionsFromRemoteStorage != null) {
					if(subscriptionsFromRemoteStorage.getType() == SubscriptionsFromRemoteStorage.T_A) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromApplicationStorage.canReceive(info)) {
							return subscriptionsFromApplicationStorage;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Überprüft ob eine ähnliche Anmeldung schon positiv abgeschlossen und vorhanden ist.
	 *
	 * @param key               Basisanmeldeinformationen
	 * @param subscriptionState Status der Anmeldung
	 *
	 * @return Eintrag mit den Anmeldungsinformationen
	 */
	final InAndOutSubscription isSuccessfullySubscribed(BaseSubscriptionInfo key, byte subscriptionState) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._role == subscriptionState) && (subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP)
						   && (subscription._outSubscriptions != null)) {
							return subscription;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Überprüft ob eine ähnliche Anmeldung schon positive abgeschlossen und vorhanden ist.
	 *
	 * @param key               Basisanmeldeinformationen
	 * @param subscriptionState Status der Anmeldung
	 * @param transmitterId     Id des DAV
	 * @param ids               die erreichbaren DAV, ausgehend vom lokalen DAV.
	 *
	 * @return Eintrag mit den Anmeldungsinformationen
	 */
	final InAndOutSubscription isSuccessfullySubscribed(
			BaseSubscriptionInfo key, byte subscriptionState, long transmitterId, long ids[]
	) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._role == subscriptionState) && (subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP)
						   && (subscription._outSubscriptions != null)) {
							for(int j = 0; j < ids.length; ++j) {
								if(transmitterId == ids[j]) {
									continue;
								}
								if(subscription._mainTransmitter == ids[j]) {
									return subscription;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Überprüft ob eine ähnliche Anmeldung gerade bearbeitet wird.
	 *
	 * @param key               Basisanmeldeinformationen
	 * @param subscriptionState Status der Anmeldung
	 *
	 * @return Eintrag mit den Anmeldungsinformationen
	 */
	final boolean isUnsuccessfullySubscribed(BaseSubscriptionInfo key, byte subscriptionState) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._role == subscriptionState) && (subscription._state == TransmitterSubscriptionsConstants.NEGATIV_RECEIP)
						   && (subscription._outSubscriptions != null)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Überprüft ob eine ähnliche Anmeldung gerade bearbeitet wird.
	 *
	 * @param key               Basisanmeldeinformationen
	 * @param subscriptionState Status der Anmeldung
	 * @param transmitterId     Id des DAV
	 * @param ids               die erreichbaren DAV, ausgehend vom lokalen DAV
	 *
	 * @return true: ähnliche Anmeldung gefunden false: keine Aähnliche Anmeldung gefunden
	 */
	final boolean isUnsuccessfullySubscribed(
			BaseSubscriptionInfo key, byte subscriptionState, long transmitterId, long ids[]
	) {
		if((key == null) || (ids == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._role == subscriptionState) && (subscription._state == TransmitterSubscriptionsConstants.NEGATIV_RECEIP)
						   && (subscription._outSubscriptions != null) && (subscription._currentTransmittersToConsider != null)) {
							boolean subSet = true;
							for(int j = 0; j < ids.length; ++j) {
								if(transmitterId == ids[j]) {
									continue;
								}
								boolean found = false;
								for(int k = 0; k < subscription._currentTransmittersToConsider.length; ++k) {
									if(ids[j] == subscription._currentTransmittersToConsider[k]) {
										found = true;
										break;
									}
								}
								if(!found) {
									subSet = false;
									break;
								}
							}
							if(subSet) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Überprüft ob eine ähnliche Anmeldung gerade bearbeitet wird
	 *
	 * @param transmitterId Id des DAV
	 * @param subscription  ein Vermerk in der Verwaltungstabelle für Anmeldungsinformationen
	 *
	 * @return true: ähnliche Anmeldung gefunden false: keine Aähnliche Anmeldung gefunden
	 */
	final boolean needToSendSubscription(long transmitterId, InAndOutSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		long ids[] = subscription._currentTransmittersToConsider;
		if(ids == null) {
			return false;
		}
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		byte subscriptionState = subscription._role;
		if(key == null) {
			throw new IllegalStateException("Argument ist inkonsistent");
		}

		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(_subscription == subscription) {
							continue;
						}
						if((_subscription._role == subscriptionState) && (_subscription._outSubscriptions != null) && (_subscription
								._currentTransmittersToConsider != null)) {
							if(_subscription._currentTransmittersToConsider.length == ids.length) {
								boolean subSet = true;
								for(int j = 0; j < ids.length; ++j) {
									boolean found = false;
									for(int k = 0; k < _subscription._currentTransmittersToConsider.length; ++k) {
										if(ids[j] == _subscription._currentTransmittersToConsider[k]) {
											found = true;
											break;
										}
									}
									if(!found) {
										subSet = false;
										break;
									}
								}
								if(subSet) {
									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Addiert einen Anmeldeantrag in der Tabelle.
	 *
	 * @param subscription  ein Vermerk in der Verwaltungstabelle für Anmeldungsinformationen
	 * @param transmitterId Id des DAV
	 *
	 * @return
	 */
	final InAndOutSubscription addSubscriptionRequest(InAndOutSubscription subscription, long transmitterId) {
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		if(key != null) {
			ArrayList<InAndOutSubscription> list;
			synchronized(_inOutSubscriptionsTable) {
				list = _inOutSubscriptionsTable.get(key);
				if(list == null) {
					list = new ArrayList<InAndOutSubscription>();
					_inOutSubscriptionsTable.put(key, list);
				}
			}
			synchronized(list) {
				if(subscription._fromTransmitter) {
					for(int i = list.size() - 1; i > -1; --i) {
						InAndOutSubscription _subscription = list.get(i);
						if((_subscription != null) && (_subscription._source == subscription._source) && (_subscription._role == subscription._role)) {
							if((_subscription._transmittersToConsider != null) && (subscription._transmittersToConsider != null)) {
								if(_subscription._transmittersToConsider.length == subscription._transmittersToConsider.length) {
									boolean subSet = true;
									for(int k = 0; k < _subscription._transmittersToConsider.length; ++k) {
										boolean found = false;
										for(int h = 0; h < subscription._transmittersToConsider.length; ++h) {
											if(subscription._transmittersToConsider[h] == _subscription._transmittersToConsider[k]) {
												found = true;
												break;
											}
										}
										if(!found) {
											subSet = false;
											break;
										}
									}
									if(subSet) {
										_subscription._count = _subscription._count + 1;
										return _subscription;
									}
								}
							}
						}
					}
				}
				else {
					for(int i = list.size() - 1; i > -1; --i) {
						InAndOutSubscription _subscription = list.get(i);
						if((_subscription != null) && (_subscription._source == subscription._source) && (_subscription._role == subscription._role)) {
							list.set(i, subscription);
							return _subscription;
						}
					}
				}
				list.add(subscription);
				return subscription;
			}
		}
		return null;
	}

	/**
	 * @param key               Basisanmeldeinformationen
	 * @param subscriptionState Status der Anmeldung
	 * @param sourceTransmitter Ausgangs - DAV
	 * @param ids               die erreichbaren DAV, ausgehend vom lokalen DAV.
	 *
	 * @return Eintrag mit der Anmeldungsinformationen
	 */
	final InAndOutSubscription[] getSameSubscriptionPostedTo(
			BaseSubscriptionInfo key, byte subscriptionState, long sourceTransmitter, long ids[]
	) {
		if((key == null) || (ids == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> result = new ArrayList<InAndOutSubscription>();
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._outSubscriptions != null) && (subscription._role == subscriptionState)) {
							for(int j = 0; j < subscription._outSubscriptions.length; ++j) {
								if((subscription._outSubscriptions[j] != null) && (sourceTransmitter == subscription._outSubscriptions[j]._targetTransmitter)) {
									if(subscription._outSubscriptions[j]._transmittersToConsider != null) {
										if(subscription._outSubscriptions[j]._transmittersToConsider.length == ids.length) {
											boolean subSet = true;
											for(int k = 0; k < ids.length; ++k) {
												boolean found = false;
												for(int h = 0; h < subscription._outSubscriptions[j]._transmittersToConsider.length; ++h) {
													if(subscription._outSubscriptions[j]._transmittersToConsider[h] == ids[k]) {
														found = true;
														break;
													}
												}
												if(!found) {
													subSet = false;
													break;
												}
											}
											if(subSet) {
												result.add(subscription);
												break;
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
		int size = result.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription array[] = new InAndOutSubscription[size];
			return (InAndOutSubscription[])result.toArray(array);
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um einen Anmeldungsvermerk zu quittieren. Anhand der Basisanmeldeinformation in receipt wird
	 * die Menge der Anmeldungsvermerke bestimmt, die für diese Basisanmeldeinformation bei dieser Komponente registriert sind. Danach wird in dieser Liste der
	 * Vermerk gesucht, der den gleichen Anmeldestatus und die gleichen zu berücksichtigenden Datenverteiler hat. Die Folgeanmeldung aus diesem Anmeldungsvermerk
	 * bei dem Datenverteiler mit der in targetId angegebenen Datenverteiler-ID wird mit dem Quittierzustand aus receipt versehen, und der Anmeldungsvermerk wird
	 * von der Methode zurückgegeben. Wenn kein Vermerk existiert, wird Null zurückgegeben.
	 *
	 * @param targetId Ziel - DAV
	 * @param receipt  Quittung der Anmeldung
	 *
	 * @return Eintrag mit der Anmeldungsinformationen
	 */
	final InAndOutSubscription[] getSubscriptionRequestToUpdate(long targetId, TransmitterDataSubscriptionReceipt receipt) {
		ArrayList<InAndOutSubscription> result = new ArrayList<InAndOutSubscription>();
		BaseSubscriptionInfo key = receipt.getBaseSubscriptionInfo();
		if(key != null) {
			ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
			if(list != null) {
				synchronized(list) {
					long transmitterIds[] = receipt.getTransmitters();
					if(transmitterIds != null) {
						byte subscriptionState = receipt.getSubscriptionState();
						int size = list.size();
						for(int i = 0; i < size; ++i) {
							InAndOutSubscription subscription = list.get(i);
							if(subscription != null) {
								if((subscription._outSubscriptions != null) && (subscription._role == subscriptionState)) {
									InAndOutSubscription.OutSubscription outSubscription = null;
									for(int j = 0; j < subscription._outSubscriptions.length; ++j) {
										if((subscription._outSubscriptions[j] != null) && (targetId == subscription._outSubscriptions[j]._targetTransmitter)) {
											outSubscription = subscription._outSubscriptions[j];
											break;
										}
									}
									if((outSubscription != null) && (outSubscription._transmittersToConsider != null)) {
										if(outSubscription._transmittersToConsider.length == transmitterIds.length) {
											boolean subSet = true;
											for(int k = 0; k < transmitterIds.length; ++k) {
												boolean found = false;
												for(int h = 0; h < outSubscription._transmittersToConsider.length; ++h) {
													if(outSubscription._transmittersToConsider[h] == transmitterIds[k]) {
														found = true;
														break;
													}
												}
												if(!found) {
													subSet = false;
													break;
												}
											}
											if(subSet) {
												if(outSubscription._state != receipt.getReceipt()) {
													outSubscription._state = receipt.getReceipt();
													outSubscription._mainTransmitter = receipt.getMainTransmitterId();
													result.add(subscription);
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
		int size = result.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription array[] = new InAndOutSubscription[size];
			return (InAndOutSubscription[])result.toArray(array);
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um einen Anmeldungsvermerk eines Umleitungsantrags zu quittieren. Anhand der Informationen in
	 * receip wird überprüft, ob der vorhandene Vermerk die gleiche Basisanmeldeinformation, den gleichen Anmeldestatus und die gleichen zu berücksichtigenden
	 * Datenverteiler hat. Ist dies der Fall, so wird die Folgeanmeldung aus diesem Anmeldungsvermerk bei dem Datenverteiler mit der in targetId angegebenen
	 * Datenverteiler-ID mit dem Quittierzustand aus receip versehen, und true wird von der Methode zurückgegeben. Wenn kein Vermerk existiert, wird false
	 * zurückgegeben.
	 *
	 * @param subscription      Eintrag mit der Anmeldungsinformationen
	 * @param targetId          Ziel - DAV
	 * @param key               Basisanmeldeinformationen
	 * @param transmitterIds    die erreichbaren DAV, ausgehend vom lokalen DAV
	 * @param mainTransmitterId Haupt - DAV
	 * @param subscriptionState Status der Anmeldung
	 * @param receip            Quittunngszustand
	 *
	 * @return true: Es existiert ein Vermerk in der Verbindungsverwaltung, false: Es existiert kein entsprechender Vermerk.
	 */
	final boolean updateExchangeSubscriptionRequest(
			InAndOutSubscription subscription,
			long targetId,
			BaseSubscriptionInfo key,
			long transmitterIds[],
			long mainTransmitterId,
			byte subscriptionState,
			byte receip
	) {
		if((subscription == null) || (key == null) || (transmitterIds == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		if(key.equals(subscription._baseSubscriptionInfo)) {
			if(subscription != null) {
				synchronized(subscription) {
					if((subscription._outSubscriptions != null) && (subscription._role == subscriptionState)) {
						InAndOutSubscription.OutSubscription outSubscription = null;
						for(int j = 0; j < subscription._outSubscriptions.length; ++j) {
							if((subscription._outSubscriptions[j] != null) && (targetId == subscription._outSubscriptions[j]._targetTransmitter)) {
								outSubscription = subscription._outSubscriptions[j];
								break;
							}
						}
						if((outSubscription != null) && (outSubscription._transmittersToConsider != null)) {
							if(outSubscription._transmittersToConsider.length == transmitterIds.length) {
								boolean subSet = true;
								for(int k = 0; k < transmitterIds.length; ++k) {
									boolean found = false;
									for(int h = 0; h < outSubscription._transmittersToConsider.length; ++h) {
										if(outSubscription._transmittersToConsider[h] == transmitterIds[k]) {
											found = true;
											break;
										}
									}
									if(!found) {
										subSet = false;
										break;
									}
								}
								if(subSet) {
									outSubscription._state = receip;
									outSubscription._mainTransmitter = mainTransmitterId;
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um einen Anmeldungsvermerk einer Applikation zu löschen. Zunächst wird die Liste der
	 * Anmeldungsvermerke der angegebenen Basisanmeldeinformation bestimmt. Dann werden alle Vermerke aus der Liste entfernt, die von der Applikation applicationId
	 * stammen und den gleichen Anmeldestatus wie subscriptionState haben.
	 *
	 * @param applicationId     Applikation
	 * @param key               Basisanmeldeinformation
	 * @param subscriptionState Anmeldestatus
	 *
	 * @return aktualisierter Eintrag der Anmeldungsinformationen
	 */
	final InAndOutSubscription removeSubscriptionRequest(
			long applicationId, BaseSubscriptionInfo key, byte subscriptionState
	) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._source == applicationId) && (subscription._role == subscriptionState)) {
							return removeSubscription(list.remove(i));
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um einen Anmeldungsvermerk eines Datenverteilers zu löschen. Zunächst wird die Liste der
	 * Anmeldungsvermerke mit der angegebenen Basisanmeldeinformation bestimmt. Dann werden alle Vermerke aus der Liste entfernt, die vom Datenverteiler
	 * transmitterId stammen, den gleichen Anmeldestatus wie subscriptionState haben und sich auch auf einen der in transmitterIds angegebenen Datenverteiler
	 * beziehen.
	 *
	 * @param sourceTransmitterId Ausgangs - DAV
	 * @param key                 Basisanmeldeinformation
	 * @param subscriptionState   Anmeldestatus
	 * @param transmitterIds      die erreichbaren DAV, ausgehend vom lokalen DAV
	 * @param transmitterId       Id des DAV
	 *
	 * @return aktualisierter Eintrag der Anmeldungsinformationen
	 */
	final InAndOutSubscription removeSubscriptionRequest(
			long sourceTransmitterId, BaseSubscriptionInfo key, byte subscriptionState, long transmitterIds[], long transmitterId
	) {
		if((key == null) || (transmitterIds == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if(subscription._transmittersToConsider != null) {
							if((subscription._source == sourceTransmitterId) && (subscription._role == subscriptionState)) {
								if(transmitterIds.length == subscription._transmittersToConsider.length) {
									boolean subSet = true;
									for(int k = 0; k < transmitterIds.length; ++k) {
										boolean found = false;
										for(int h = 0; h < subscription._transmittersToConsider.length; ++h) {
											if(subscription._transmittersToConsider[h] == transmitterIds[k]) {
												found = true;
												break;
											}
										}
										if(!found) {
											subSet = false;
											break;
										}
									}
									if(subSet) {
										subscription._count = subscription._count - 1;
										if(subscription._count <= 0) {
											return removeSubscription(list.remove(i));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um einen Anmeldungsvermerk aus der Verwaltung zu löschen. Zunächst wird die Liste der
	 * Anmeldungsvermerke mit der angegebenen Basisanmeldeinformation bestimmt. Dann werden alle Vermerke subscription aus der Liste entfernt. Dieser Wert wird von
	 * der Methode auch zurückgegeben. Existiert der Vermerk nicht, so wird Null zurückgegeben.
	 *
	 * @param subscription Eintrag mit der Anmeldungsinformation
	 *
	 * @return aktualisierter Eintrag der Anmeldungsinformationen
	 */
	final InAndOutSubscription removeSubscriptionRequest(InAndOutSubscription subscription) {
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(subscription._baseSubscriptionInfo);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription == subscription) {
						return removeSubscription(list.remove(i));
					}
				}
			}
		}
		return null;
	}

	private InAndOutSubscription removeSubscription(final InAndOutSubscription subscription) {
		final DavTransactionManager transactionManager = _connectionsManager.getDavTransactionManager();
		if(transactionManager != null){
			transactionManager.notifyUnsubscribe(subscription._baseSubscriptionInfo, subscription._role == 0);
		}
		return subscription;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Verbindung nicht mehr vorhanden ist. Alle Anmeldungsvermerke mit der sourceId als
	 * Anmeldeinstanz werden aus der Verwaltung entfernt. Dieser Wert wird von der Methode auch zurückgegeben.
	 *
	 * @param sourceId Ausgangs - DAV
	 *
	 * @return aktualisierter Eintrag der Anmeldungsinformationen
	 */
	final InAndOutSubscription[] removeSubscriptionRequests(long sourceId) {
		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						InAndOutSubscription subscription = null;
						for(int j = list.size() - 1; j > -1; --j) {
							subscription = list.get(j);
							if((subscription != null) && (subscription._source == sourceId)) {
								removeSubscription(list.remove(j));
								requests.add(subscription);
							}
						}
						if((list.size() == 0) && (subscription != null)) {
							_inOutSubscriptionsTable.remove(subscription._baseSubscriptionInfo);
						}
					}
				}
			}
		}
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, ob für die übergebene Basisanmeldeinformation ein Anmeldungsvermerk vorliegt.
	 * Wenn mindestens ein Anmeldungsvermerk existiert, dann wird true zurückgegeben, sonst false.
	 *
	 * @param key Basisanmeldeinformationen
	 *
	 * @return true: es existiert mind. ein Anmeldungsvermerk, false: es existiert keine Anmeldungsvermerk.
	 */
	final boolean isSubscriptionRequestAvaillable(BaseSubscriptionInfo key) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if((list != null) && (list.size() > 0)) {
			return true;
		}
		return false;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, ob ein positiv quittierter Anmeldungsvermerk für die gleiche
	 * Basisanmeldeinformation und den gleichen Anmeldstatus vorliegt, wie in subscription übergeben. Ist dies der Fall, so wird true zurückgegeben, sonst false.
	 *
	 * @param subscription Eintrag mit der Anmeldungsinformation
	 *
	 * @return true: es existiert ein Anmeldungsvermerk, false: es existiert keine Anmeldungsvermerk.
	 */
	final boolean isSubscriptionRequestAvaillable(InAndOutSubscription subscription) {
		if((subscription == null) || (subscription._baseSubscriptionInfo == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(subscription._baseSubscriptionInfo);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(subscription == _subscription) {
							continue;
						}
						if((_subscription._source == subscription._source)
						   && (_subscription._role == subscription._role) && (_subscription._state == TransmitterSubscriptionsConstants
								.POSITIV_RECEIP)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}


	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, ob ein positiv quittierter Anmeldungsvermerk für die gleiche
	 * Basisanmeldeinformation und den gleichen Anmeldstatus vorliegt, wie in subscription übergeben. Außerdem darf sie keine Folgeanmeldungen besitzen. Ist dies
	 * der Fall, so wird der Vermerk zurückgegeben, sonst wird Null zurückgegeben.
	 *
	 * @param subscription Anmeldung
	 *
	 * @return falls vorhanden, wird die Anmmeldung zurück gegeben, sonst NULL
	 */
	final InAndOutSubscription getAlternativeSubscriptionRequest(InAndOutSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		if(key == null) {
			throw new IllegalArgumentException("Argument ist Inkonsistent");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(_subscription == subscription) {
							continue;
						}
						else if(_subscription._outSubscriptions != null) {
							continue;
						}
						else if((_subscription._role == subscription._role) && (_subscription._state == subscription._state) && (
								_subscription._mainTransmitter == subscription._mainTransmitter)) {
							return _subscription;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, welche Anmeldungsvermerke von subscription abhängen. Dazu wird zunächst die
	 * Liste der Anmeldungsvermerke bestimmt, die die gleiche Basisanmeldeinformation wie subscription haben. Für die positiv quittierten aus dieser Liste wird
	 * bestimmt, welche zusätzlich den gleichen Anmeldestatus und eine leere Folgeanmeldungsliste besitzen. Alle Vermerke, die diese Bedingungen erfüllen, werden
	 * in einem Feld zurückgegeben. Wenn kein solcher Vermerk existiert, wird Null zurückgegeben.
	 *
	 * @param subscription Anmeldung
	 *
	 * @return Feld der vom Übergabeparameter abhängigen Anmeldungen.
	 */
	final InAndOutSubscription[] getInvolvedPositiveSubscriptionRequests(InAndOutSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		if(key == null) {
			throw new IllegalArgumentException("Argument ist Inkonsistent");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			ArrayList<InAndOutSubscription> array = new ArrayList<InAndOutSubscription>();
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(_subscription == subscription) {
							array.add(_subscription);
						}
						else if(_subscription._outSubscriptions != null) {
							continue;
						}
						else if((_subscription._role == subscription._role) && (_subscription._state
						                                                                                == TransmitterSubscriptionsConstants.POSITIV_RECEIP)) {
							array.add(_subscription);
						}
					}
				}
			}
			int size = array.size();
			if(size > 0) {
				InAndOutSubscription retValue[] = new InAndOutSubscription[size];
				for(int i = 0; i < size; ++i) {
					retValue[i] = array.get(i);
				}
				return retValue;
			}
		}
		return null;
	}


	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, welche Anmeldungsvermerke von subscription abhängen. Dazu wird zunächst die
	 * Liste der Anmeldungsvermerke bestimmt, die die gleiche Basisanmeldeinformation wie subscription haben. Für die positiv quittierten aus dieser Liste wird
	 * bestimmt, welche zusätzlich den gleichen Anmeldestatus und eine leere Folgeanmeldungsliste besitzen. Alle Vermerke, die diese Bedingungen erfüllen, werden
	 * in einem Feld zurückgegeben. Wenn kein solcher Vermerk existiert, wird Null zurückgegeben.<br>Ein Anmeldeantrag A ist abhängig von B wenn:<br>
	 * -Basisanmeldeinformation von A ist gleich von B<br> -Anmeldungsart von A ist gleich von B (Sender oder empfänger)<br>
	 *
	 * @param subscription Anmeldung
	 *
	 * @return vom Übergabeparameter abhängige Anmeldungsvermerke
	 */
	final InAndOutSubscription[] getInvolvedSubscriptionRequests(InAndOutSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		if(key == null) {
			throw new IllegalArgumentException("Argument ist Inkonsistent");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			ArrayList<InAndOutSubscription> array = new ArrayList<InAndOutSubscription>();
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(_subscription == subscription) {
							array.add(_subscription);
						}
						else if(_subscription._outSubscriptions != null) {
							continue;
						}
						else if(_subscription._role == subscription._role) {
							array.add(_subscription);
						}
					}
				}
			}
			int size = array.size();
			if(size > 0) {
				InAndOutSubscription retValue[] = new InAndOutSubscription[size];
				for(int i = 0; i < size; ++i) {
					retValue[i] = array.get(i);
				}
				return retValue;
			}
		}
		return null;
	}

	/**
	 * Gibt die Anmeldeantrage zurück, die vom übergebenen Anmeldeantrag abhängig sein könnten, zurück. <br>Ein Anmeldeantrag A ist abhängig von B wenn:<br>
	 * -Basisanmeldeinformation von A ist gleich von B<br>-Anmeldungsart von A ist gleich von B (Sender oder empfänger) <br>-Zu berucksichtigenden Datenverteiler
	 * von A sind gleich von B
	 */
	final InAndOutSubscription[] getInvolvedSubscriptionRequests(InAndOutSubscription subscription, boolean checkIds) {
		if(subscription == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo key = subscription._baseSubscriptionInfo;
		if(key == null) {
			throw new IllegalArgumentException("Argument ist Inkonsistent");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			ArrayList<InAndOutSubscription> array = new ArrayList<InAndOutSubscription>();
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription _subscription = list.get(i);
					if(_subscription != null) {
						if(_subscription == subscription) {
							array.add(_subscription);
						}
						else if(_subscription._outSubscriptions != null) {
							continue;
						}
						else if(_subscription._role == subscription._role) {
							if((_subscription._currentTransmittersToConsider != null) && (subscription._currentTransmittersToConsider != null)) {
								if(_subscription._currentTransmittersToConsider.length == subscription._currentTransmittersToConsider.length) {
									boolean subSet = true;
									for(int k = 0; k < _subscription._currentTransmittersToConsider.length; ++k) {
										boolean found = false;
										for(int h = 0; h < subscription._currentTransmittersToConsider.length; ++h) {
											if(subscription._currentTransmittersToConsider[h] == _subscription._currentTransmittersToConsider[k]) {
												found = true;
												break;
											}
										}
										if(!found) {
											subSet = false;
											break;
										}
									}
									if(subSet) {
										array.add(_subscription);
									}
								}
							}
						}
					}
				}
			}
			int size = array.size();
			if(size > 0) {
				InAndOutSubscription retValue[] = new InAndOutSubscription[size];
				for(int i = 0; i < size; ++i) {
					retValue[i] = array.get(i);
				}
				return retValue;
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um zu überprüfen, welche Anmeldungsvermerke von key abhängen. Dazu wird die Liste der
	 * Anmeldungsvermerke bestimmt, die die gleiche Basisanmeldeinformation wie key haben und in einem Feld zurückgegeben. Wenn kein solcher Vermerk existiert,
	 * wird Null zurückgegeben.
	 *
	 * @param key Basisanmeldeinformationen
	 *
	 * @return Anmeldeantrage, die vom gegebenen Anmeldeinformationen abhängig sein könnten
	 */
	final InAndOutSubscription[] getInvolvedSubscriptionRequests(BaseSubscriptionInfo key) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			ArrayList<InAndOutSubscription> array = new ArrayList<InAndOutSubscription>();
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						array.add(subscription);
					}
				}
			}
			int size = array.size();
			if(size > 0) {
				InAndOutSubscription retValue[] = new InAndOutSubscription[size];
				for(int i = 0; i < size; ++i) {
					retValue[i] = array.get(i);
				}
				return retValue;
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn neue Zuliefererinformationen eingetroffen sind.  Es werden alle nicht positiv quittierten
	 * Anmeldungsvermerke darauf überprüft, ob die neuen Objekt-IDs oder Attributgruppen-Aspekt-Kombinationen sie betreffen. Dazu werden die Basisanmeldeinformation der
	 * Vermerke mit den ids bzw. combinations verglichen. Ein Feld mit den betroffenen Vermerken wird gebildet und zurückgegeben. Wenn kein solcher Vermerk
	 * existiert, wird Null zurückgegeben.
	 *
	 * @param ids          Die Objekt-Ids
	 * @param combinations Die Attributgruppen-Aspekt-Kombinationen
	 *
	 * @return Feld mit den betroffenen Kombinationen
	 */
	final InAndOutSubscription[] getInvolvedSubscriptionRequests(
			long ids[], AttributeGroupAspectCombination combinations[]
	) {
		if((ids == null) && (combinations == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}

		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						int size = list.size();
						for(int j = 0; j < size; ++j) {
							InAndOutSubscription subscription = list.get(j);
							if(subscription != null) {
								if(subscription._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
									continue;
								}
								BaseSubscriptionInfo info = subscription._baseSubscriptionInfo;
								if(info != null) {
									if(ids != null) {
										long objectId = info.getObjectID();
										boolean found = false;
										for(int k = 0; k < ids.length; ++k) {
											if(ids[k] == objectId) {
												found = true;
												break;
											}
										}
										if(found) {
											requests.add(subscription);
											continue;
										}
									}
									if(combinations != null) {
										long atgUsageIdentification = info.getUsageIdentification();
										for(int k = 0; k < combinations.length; ++k) {
											if(combinations[k].getAtgUsageIdentification() == atgUsageIdentification) {
												requests.add(subscription);
												break;
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
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn neue Zuliefererinformationen eingetroffen sind.  Es werden alle nicht positiv quittierten
	 * Anmeldungsvermerke darauf überprüft, ob die neuen Objekt-IDs oder Attributgruppen-Aspekt-Kombinationen sie betreffen. Dazu werden die Basisanmeldeinformation der
	 * Vermerke mit den ids bzw. combinations verglichen. Ein Feld mit den betroffenen Vermerken wird gebildet und zurückgegeben. Wenn kein solcher Vermerk
	 * existiert, wird Null zurückgegeben.
	 *
	 * @param ids          Die Objekt-Ids
	 * @param combinations Die Attributgruppen-Aspekt-Kombinationen
	 */
	final InAndOutSubscription[] getInvolvedPositiveSubscriptionRequests(
			long ids[], AttributeGroupAspectCombination combinations[]
	) {
		if((ids == null) && (combinations == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}

		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						int size = list.size();
						for(int j = 0; j < size; ++j) {
							InAndOutSubscription subscription = list.get(j);
							if((subscription != null) && (subscription._outSubscriptions != null) && (subscription._state == TransmitterSubscriptionsConstants
									.POSITIV_RECEIP)) {
								BaseSubscriptionInfo info = subscription._baseSubscriptionInfo;
								if(info != null) {
									if(ids != null) {
										long objectId = info.getObjectID();
										boolean found = false;
										for(int k = 0; k < ids.length; ++k) {
											if(ids[k] == objectId) {
												found = true;
												break;
											}
										}
										if(found) {
											requests.add(subscription);
											continue;
										}
									}
									if(combinations != null) {
										final long atgUsageIdentification = info.getUsageIdentification();
										for(int k = 0; k < combinations.length; ++k) {
											if(combinations[k].getAtgUsageIdentification() == atgUsageIdentification) {
												requests.add(subscription);
												break;
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
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn neue Weginformationen eingetroffen sind. Es werden alle Anmeldungsvermerke gesucht, die
	 * den Datenverteiler transmitterId in ihrer Liste der zu berücksichtigenden Datenverteiler haben. Weiterhin müssen die Anmeldungsvermerke eine Folgeanmeldung
	 * beim Datenverteiler overTransmitterId haben. Diese gefundenen Anmeldungsvermerke werden umgeleitet, da der Datenverteiler transmitterId über einen neuen Weg
	 * erreichbar ist. Diese Anmeldungsvermerke werden als Feld zurückgegeben. Wenn kein solcher Eintrag existiert, wird Null zurückgegeben.
	 *
	 * @param transmitter       DAV
	 * @param overTransmitterId
	 *
	 * @return alle Anmeldungen, die positiv quittiert sind oder auf eine quittung warten
	 */
	final InAndOutSubscription[] getSubscriptionRequestsToRedirect(long transmitter, long overTransmitterId) {
		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						int size = list.size();
						for(int j = 0; j < size; ++j) {
							InAndOutSubscription subscription = list.get(j);
							if((subscription != null) && (subscription._outSubscriptions != null)) {
								if(subscription._currentTransmittersToConsider != null) {
									boolean found = false;
									for(int k = 0; k < subscription._currentTransmittersToConsider.length; ++k) {
										if(subscription._currentTransmittersToConsider[k] == transmitter) {
											found = true;
											break;
										}
									}
									if(found) {
										for(int k = 0; k < subscription._outSubscriptions.length; ++k) {
											if(subscription._outSubscriptions[k] != null) {
												if(subscription._outSubscriptions[k]._targetTransmitter == overTransmitterId) {
													requests.add(subscription);
													break;
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
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Verbindung zu einem Datenverteiler aufgehoben wurde. Es werden alle
	 * Anmeldungsvermerke gesucht, die eine Folgeanmeldung bei diesem Datenverteiler targetId besitzen. Ein Feld der betroffenen Anmeldungsvermerke wird erstellt
	 * und zurückgegeben. Wenn kein solcher Vermerk existiert, wird Null zurückgegeben.
	 *
	 * @param targetId DAV mit Folgeanmeldungen
	 *
	 * @return Feld der betroffenen Anmeldungsvermerke wird zurückgegeben. Wenn kein solcher Vermerk existiert, wird Null zurückgegeben.
	 */
	final InAndOutSubscription[] getAffectedOutgoingSubscriptions(long targetId) {
		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1 ; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						InAndOutSubscription subscription = null;
						int size = list.size();
						for(int j = 0; j < size; ++j) {
							subscription = list.get(j);
							if(subscription != null) {
								if(subscription._outSubscriptions != null) {
									for(int k = subscription._outSubscriptions.length - 1; k > -1; --k) {
										if((subscription._outSubscriptions[k] != null) && (subscription._outSubscriptions[k]._targetTransmitter == targetId)) {
											requests.add(subscription);
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	final InAndOutSubscription[] getSubscriptionsFromSource(long sourceId) {
		ArrayList<InAndOutSubscription> requests = new ArrayList<InAndOutSubscription>();
		synchronized(_inOutSubscriptionsTable) {
			ArrayList<ArrayList<InAndOutSubscription>> listList = new ArrayList<ArrayList<InAndOutSubscription>>(_inOutSubscriptionsTable.values());
			for(int i = listList.size() - 1; i > -1; --i) {
				ArrayList<InAndOutSubscription> list = listList.get(i);
				if(list != null) {
					synchronized(list) {
						int size = list.size();
						for(int j = 0; j < size; ++j) {
							InAndOutSubscription subscription = list.get(j);
							if(subscription != null) {
								if(subscription._source == sourceId) {
									requests.add(subscription);
								}
							}
						}
					}
				}
			}
		}
		int size = requests.size();
		if(size == 0) {
			return null;
		}
		else {
			InAndOutSubscription retValue[] = new InAndOutSubscription[size];
			for(int i = 0; i < size; ++i) {
				retValue[i] = requests.get(i);
			}
			return retValue;
		}
	}

	final InAndOutSubscription getSubscription(long sourceId, BaseSubscriptionInfo info) {
		if(info == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(info);
		if(list != null) {
			synchronized(list) {
				int size = list.size();
				for(int i = 0; i < size; ++i) {
					InAndOutSubscription subscription = list.get(i);
					if((subscription != null) && (subscription._source == sourceId)) {
						return subscription;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DAF aufgerufen, wenn von einer Applikation ein neuer Datensatz empfangen wurde. Zuerst wird überprüft ob
	 * dieser Datenverteiler der Zentraldatenverteiler der Datensätze, d. h., ob die Quelle oder Senke der Datensätze bei diesen Datenverteiler angemeldet ist. Ist
	 * dies nicht der Fall, so wird überprüft, ob ein positiv quittierter Anmeldungsvermerk für einen Sender vorhanden ist. In diesem Fall wird der Datensatz mit
	 * der ID des Senders versehen und dem zum Datenverteiler weitergeleitet, bei dem eine positiv quittierte Folgeanmeldung vorliegt. Erst der
	 * Zentraldatenverteiler sorgt für einen gültigen, eindeutigen Datensatzindex. Bis zu diesem Zeitpunkt muss der Sender eindeutig benannt werden. Dies ist
	 * gerade dann notwendig, wenn mehrere Sender ihre Datensätze über einen Datenverteiler im System verteilen und die Telegramme zerstückelt sind. Wenn dieser
	 * Datenverteiler der Zentraldatenverteiler für diese Datensätze ist, wird überprüft ob der Datensatz zerstückelt ist. In diesem Fall wird der Datensatzindex
	 * dieses Datensatzes bestimmt und an die Applikationen und Datenverteiler weitergeleitet, die positiv quittierte Anmeldungen initiiert haben. Wenn der
	 * Datensatz kein nachgelieferter Datensatz ist, wird er in der Datenbestand aufgenommen. Für den Fall das der Datensatz zerstückelt ist gibt es zwei
	 * Möglichkeiten: <UL><LI>Das empfangene Telegramm ist das erste. Dann wird der Datensatzindex bestimmt und die Relation zwischen altem und neuem
	 * Datensatzindex in einer Tabelle festgehalten. Diese Zuordnung ist notwendig, damit die anderen Teile des zerstückelten Datensatzes den gleichen Index
	 * erhalten können. Das Telegramm wird in die sogenannte Zerstückelungstabelle aufgenommen und an die Applikationen und Datenverteiler weitergeleitet, die
	 * positiv quittierte Anmeldungen initiiert haben. <LI>Das empfangene Telegramm ist nicht das erste. Dann wird es mit dem Datensatzindex versehen, der dem
	 * ersten Telegramm der Zerstückelung zugewiesen wurde. Das Telegramm wird in die Zerstückelungstabelle aufgenommen, und es wird überprüft, ob alle Teile der
	 * Zerstückelung angekommen sind. Sind alle vorhanden, und sind sie nicht als nachgeliefert markiert, so werden sie in den Datenbestand des DaVs aufgenommen.
	 * Nachgelieferte Telegramme hingegen werden nicht im DaV vorgehalten. Das empfangene Telegramm wird dann an die Applikationen und Datenverteiler
	 * weitergeleitet, die positiv quittierte Anmeldungen initiiert haben. </UL>Der Datensatzindex wird so gebildet, dass die oberen 32 Bits die Zeit und die
	 * unteren 30 Bits den um eins erhöhten Index der Basisanmeldeinformation enthalten. Die Zeit wird entweder aus dem Datensatzindex des im Datenbestand
	 * vorhandenen Datensatzes gewonnen oder ist die Zeit des Startens dieses Datenverteilers. Für jede Basisanmeldeinformation wird ein Index in einer Tabelle
	 * eingerichtet, der bei jedem neuen Datensatz um eins erhöht wird und in der Tabelle wieder abgespeichert wird. Wenn ein Telegramm an eine Applikation
	 * weitergeleitet wird, muss überprüft werden, ob sich die Applikation evtl. nur für Delta-Daten oder für einen Teil der Attribute (oder beides) angemeldet
	 * hat. Wenn keine Einschränkung vorliegt, wird das Telegramm an die Applikation weitergeleitet. Bei Einschränkungen hingegen wird zunächst gewartet, bis der
	 * Datensatz komplett verfügbar ist (Zerstückelung). Je nachdem, welche Einschränkungen vorliegen, wird der Datensatz wie folgt an die Applikation
	 * weitergeleitet: <UL><LI>Keine nachgelieferten Daten: Sind die Daten nachgeliefert, werden sie nicht gesendet. <LI>Delta-Daten: Es wird überprüft, ob die
	 * Änderungsindikatoren gesetzt sind. Nur dann wird der Datensatz zur Applikation weitergeleitet. <LI>Teilattribute: die angemeldeten Teilattribute werden aus
	 * dem Datensatz herausge- filtert und als neuer Datensatz zur Applikation weitergeleitet. <LI>Delta-Daten und Teilattribute: Es wird die Schnittmenge aus den
	 * Indikatoren der angemeldeten Attribute und den Indikatoren der geänderten Attribute gebildet. Aus den Attributen dieser Schnittmenge wird ein neuer
	 * Datensatz gebildet und an die Applikation weitergeleitet. </UL>Das Senden eines Datensatzes wird natürlich nur dann erfolgen, wenn der Empfänger be-
	 * rechtigt ist, die Daten zu empfangen. Andernfalls wird ein spezieller Datensatz generiert, welcher den Empfänger darüber informiert, dass er keine
	 * Berechtigung hat.
	 *
	 * @param inputApplicationConnection              Verbindung
	 * @param applicationDataTelegram Applikations-Daten-Telegramm
	 */
	public void sendData(T_A_HighLevelCommunication inputApplicationConnection, ApplicationDataTelegram applicationDataTelegram) {
		if(applicationDataTelegram == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo info = applicationDataTelegram.getBaseSubscriptionInfo();
		if(info == null) {
			throw new IllegalArgumentException("Argument ist inkonsistent");
		}

		InAndOutSubscription subscription = getSubscription(inputApplicationConnection.getId(), info);
		if((subscription == null) || (subscription._state != TransmitterSubscriptionsConstants.POSITIV_RECEIP)) {
			return;
		}

		boolean save = false;
		// (See if main transmitter)
		SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = getReceivingComponent(info);
		if(subscriptionsFromApplicationStorage == null) {
			// Es wurde keine Anmeldung als Senke gefunden
			save = true;
			// Gibt es eine Anmeldung als Quelle?
			subscriptionsFromApplicationStorage = getSendingComponent(info);
			if(subscriptionsFromApplicationStorage != null) {
				final SubscriptionsFromApplicationStorage applicationSubscriptions = (SubscriptionsFromApplicationStorage)inputApplicationConnection
						.getSubscriptionsFromRemoteStorage();
				final SendSubscriptionInfo applicationSubscription = applicationSubscriptions.getSendSubscription(info);
				if(!applicationSubscriptions.equals(subscriptionsFromApplicationStorage) && applicationSubscription.isSource()){
					_debug.info("Erhalte Daten von einer Applikation, die nicht als Sender auf diese Daten angemeldet ist.");
					return;
				}
			}
		}
		if(subscriptionsFromApplicationStorage == null) {
			// Weder Quelle noch Senke am lokalen DAV verbunden, dann an andere DAV weiterleiten
			InAndOutSubscription inAndOutSubscription = isSuccessfullySubscribed(
					info, TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION
			);
			if((inAndOutSubscription == null) || (inAndOutSubscription._outSubscriptions == null)) {
				return; // no successfull subscription for this data
			}
			// Sender no source send to main transmitter
			TransmitterDataTelegram transmitterDataTelegram = new TransmitterDataTelegram(applicationDataTelegram, (byte)0);
			transmitterDataTelegram.setDataIndex(inputApplicationConnection.getId());
			for(int i = inAndOutSubscription._outSubscriptions.length - 1; i > -1; --i) {
				if(inAndOutSubscription._outSubscriptions[i] == null) {
					continue;
				}
				if(inAndOutSubscription._outSubscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
					long transmitterId = inAndOutSubscription._outSubscriptions[i]._targetTransmitter;
					T_T_HighLevelCommunication _connection = _connectionsManager.getTransmitterConnection(transmitterId);
					if(_connection != null) {
						_connection.sendTelegram(transmitterDataTelegram);
					}
					break;
				}
			}
		}
		else {
			// Folgendes synchronized stellt sicher, dass Vergabe von Datensatzindex und Versand eines Telegramms zusammen durchgeführt werden, da eine
			// verzahnte Abarbeitung durch verschiedene Threads zu Vertauschungen im Datensatzindex führen könnten
			synchronized(subscriptionsFromApplicationStorage) {
				final int totalTelegramsCount = applicationDataTelegram.getTotalTelegramsCount();
				if(totalTelegramsCount == 1 || (save == true && subscriptionsFromApplicationStorage == inputApplicationConnection.getSubscriptionsFromRemoteStorage())) {
					// Nicht gestückelte Telegramme oder Telegramme von der Quelle sofort verarbeiten
					processApplicationDataTelegramAsCentralDistributor(applicationDataTelegram, save,
					                                                   subscriptionsFromApplicationStorage, inputApplicationConnection, info);
				}
				else {
					// Gestückelte Telegramme, die nicht von einer Quelle kommen
					// Es wird unterschieden nach dem ersten, einem mittleren und dem letzen Telegramm eines zerstückelten Datensatzes
					final int telegramNumber = applicationDataTelegram.getTelegramNumber();
					if(telegramNumber == 0) {
						// Das erste Telegramm eines zerstückelten Datensatzes
						List<ApplicationDataTelegram> stalledTelegramList = inputApplicationConnection.createStalledTelegramList(info, totalTelegramsCount);
						stalledTelegramList.add(applicationDataTelegram);
					}
					else {
						// Nicht das erste Telegramm eines zerstückelten Datensatzes
						if(telegramNumber + 1 != totalTelegramsCount) {
							// Ein mittleres Telegramm eines zerstückelten Datensatzes
							List<ApplicationDataTelegram> stalledTelegramList = inputApplicationConnection.getStalledTelegramList(info);
							if(stalledTelegramList == null) {
								_debug.error(
										"Ein mittleres Telegramm eines zerstückelten Datensatzes wurde nach Empfang eines Folgetelegramms nicht gefunden", info
								);
							}
							else {
								stalledTelegramList.add(applicationDataTelegram);
							}
						}
						else {
							// Das letzte Telegramm eines zerstückelten Datensatzes
							List<ApplicationDataTelegram> stalledTelegramList = inputApplicationConnection.deleteStalledTelegramList(info);
							if(stalledTelegramList == null) {
								_debug.error(
										"Das letzte Telegramm eines zerstückelten Datensatzes wurde nicht gefunden", info
								);
							}
							else {
								stalledTelegramList.add(applicationDataTelegram);
								for(ApplicationDataTelegram stalledApplicationDataTelegram : stalledTelegramList) {
									processApplicationDataTelegramAsCentralDistributor(
											stalledApplicationDataTelegram, save, subscriptionsFromApplicationStorage, inputApplicationConnection, info
									);
								}
							}
						}
					}
				}
//				processApplicationDataTelegramAsCentralDistributor(applicationDataTelegram, save, subscriptionsFromApplicationStorage, inputApplicationConnection, info);
			}
		}
	}

	private void processApplicationDataTelegramAsCentralDistributor(
			final ApplicationDataTelegram applicationDataTelegram,
			boolean save,
			final SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage,
			final T_A_HighLevelCommunication connection,
			final BaseSubscriptionInfo info
	) {
		// Wenn save==false, dann gibt es eine lokale Senke, sonst eine lokale Quelle
		long index = applicationDataTelegram.getDataNumber();
		Long originIndex = new Long(index);
		if(applicationDataTelegram.getTelegramNumber() == 0) {
			if(save == true && subscriptionsFromApplicationStorage == connection.getSubscriptionsFromRemoteStorage()) {
				index = getDataIndex(info, true, index);
			}
			else {
				index = getDataIndex(info, false, index);
			}
			if(applicationDataTelegram.getTotalTelegramsCount() != 1) {
				if(_usedDataIndexTable.put(originIndex, new Long(index)) != null) {
					_debug.error("Empfangener Datensatzindex nicht eindeutig");
				}
				;
			}
		}
		else {
			Long value = _usedDataIndexTable.get(originIndex);
			if(value == null) {
				throw new IllegalStateException("Inkonsistente Indexierung der Teiltelegramme");
			}
			index = value.longValue();
		}

		applicationDataTelegram.setDataIndex(index);

		final TransmitterDataTelegram transmitterDataTelegram = new TransmitterDataTelegram(applicationDataTelegram, (byte)1);

		final TransmitterDataTelegram[] t_telegrams;
		if(transmitterDataTelegram.getTotalTelegramsCount() == 1) {
			t_telegrams = new TransmitterDataTelegram[]{transmitterDataTelegram};
		}
		else {
			t_telegrams = _splittedTelegramsTable.put(transmitterDataTelegram);
			if(t_telegrams != null) {
				_usedDataIndexTable.remove(originIndex);
			}
			else {
				save = false;
			}
		}

		sendTelegrams(save, info, transmitterDataTelegram, t_telegrams);
	}


	public void sendTelegramsAsCentralDistributor(
			final boolean isSource, final TransmitterDataTelegram[] dataTelegrams){
		if(dataTelegrams.length == 0) return; 
		
		// Erst alle Telegramme senden bis auf das letzte
		for(int i = 0; i < dataTelegrams.length -1 ; i++) {
			final TransmitterDataTelegram telegram = dataTelegrams[i];
			sendTelegrams(isSource, dataTelegrams[0].getBaseSubscriptionInfo(), telegram, null);
		}
		// Nun das letzte Telegramm senden mit vollständiger Information
		sendTelegrams(isSource, dataTelegrams[0].getBaseSubscriptionInfo(), dataTelegrams[dataTelegrams.length - 1], dataTelegrams);
	}

	/**
	 * Sendet Telegramme
	 * @param save TBD ???
	 * @param info AnmeldeInfo TBD Eigentlich auch in den Telegrammen enthalten
	 * @param transmitterDataTelegram Telegramm zu senden
	 * @param t_telegrams Falls es ein letztes Telegram ist, Array mit allen Telegrammen
	 */
	private void sendTelegrams(
			final boolean save, final BaseSubscriptionInfo info,
			final TransmitterDataTelegram transmitterDataTelegram,
			final TransmitterDataTelegram[] t_telegrams) {
		ApplicationDataTelegram[] telegrams = null;

		if(t_telegrams != null) {
			telegrams = new ApplicationDataTelegram[t_telegrams.length];
			for(int i = 0; i < t_telegrams.length; ++i) {
				telegrams[i] = t_telegrams[i].getApplicationDataTelegram();
			}
		}

		if(telegrams != null) {
			final DavTransactionManager davTransactionManager = _connectionsManager.getDavTransactionManager();
			if(davTransactionManager != null) {
				if(davTransactionManager.handleTelegram(telegrams, save)) {
					for(int i = 0; i < t_telegrams.length; i++) {
						t_telegrams[i] = new TransmitterDataTelegram(telegrams[i], t_telegrams[i].getDirection());
					}
					saveAndSendToInterestedReceivingComponents(
							save, t_telegrams, t_telegrams[t_telegrams.length - 1], info, telegrams, telegrams[telegrams.length - 1]
					);
					return;
				}
			}
		}

		saveAndSendToInterestedReceivingComponents(
				save, t_telegrams, transmitterDataTelegram, info, telegrams, transmitterDataTelegram.getApplicationDataTelegram()
		);
	}

	/**
	 * Speichert Telegramme eines Datensatzes im Cache und leitet die Telegramme an Applikationen oder andere Datenverteiler weiter, wenn sie sich dafür interessieren.
	 * @param save <code>true</code> falls der Datensatz gespeichert werden soll.
	 * @param transmitterTelegrams Alle Datenverteilertelegramme eines in mehrere Teiltelegramme zerlegten Datensatzes wenn alle Telegramme vorliegen, sonst <code>null</code>.
	 * @param info Datenidentifikation des Telegramms
	 * @param applicationTelegrams Alle Applikationstelegramme eines in mehrere Teiltelegramme zerlegten Datensatzes wenn alle Telegramme vorliegen, sonst <code>null</code>.
	 * @param applicationTelegram Telegramm für Applikationen
	 * @param transmitterTelegram Telegramm für andere Datenverteiler
	 */
	void saveAndSendToInterestedReceivingComponents(
			final boolean save, final TransmitterDataTelegram[] transmitterTelegrams, final TransmitterDataTelegram transmitterTelegram, final BaseSubscriptionInfo info,
			final ApplicationDataTelegram[] applicationTelegrams,
			final ApplicationDataTelegram applicationTelegram) {
		boolean send = true;
		TransmitterDataTelegram[] previousTransmitterTelegrams = null;
		if(save) {
			final boolean result;
			synchronized(_connectionsManager.cacheManager) {
				long newDataIndex = transmitterTelegrams[0].getDataNumber();
				if((newDataIndex & 0x0000000000000003L) == 0) {
					CacheManager.CacheEntry cacheEntry = _connectionsManager.cacheManager.getCacheEntry(info);
					if(cacheEntry == null) {
						_connectionsManager.cacheManager.putNewCacheEntry(info, newDataIndex, transmitterTelegrams);
						//_cache.put(info, new CacheEntry(newDataIndex, transmitterTelegrams));
					}
					else {
						if(cacheEntry.getDataIndex() < newDataIndex) {
							cacheEntry.setDataIndex(newDataIndex);
							previousTransmitterTelegrams = cacheEntry.getTelegrams();
							if(!transmitterTelegram.getDelayedDataFlag()) cacheEntry.setTelegrams(transmitterTelegrams);
						}
						else if(cacheEntry.getDataIndex() == newDataIndex) {
							_debug.fine("Datensatz wird weitergeleitet, obwohl der Datensatzindex sich nicht verändert hat. Neuer Index: " + newDataIndex + ". "
							               + "Alter Index: " + cacheEntry.getDataIndex() + ". Info: " + info);
						}
						else {
							send = false;
							_debug.warning("Datensatz wird nicht weitergeleitet, weil der Datensatzindex nicht monoton ist. Neuer Index: " + newDataIndex + ". "
							               + "Alter Index: " + cacheEntry.getDataIndex() + ". Info: " + info);
						}
					}
				}
			}
		}
		if(send) {
			SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorages[] = getInterrestedReceivingComponent(info);
			if(subscriptionsFromRemoteStorages != null) {
				for(int j = 0; j < subscriptionsFromRemoteStorages.length; ++j) {
					if(subscriptionsFromRemoteStorages[j] != null) {
						if(subscriptionsFromRemoteStorages[j].getType() == SubscriptionsFromRemoteStorage.T_A) {
							try {
								sendDataToT_A_Component(
										(SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorages[j], applicationTelegrams, applicationTelegram,
										transmitterTelegrams, previousTransmitterTelegrams
								);
							}
							catch(ConfigurationException e) {
								e.printStackTrace();
							}
						}
						else {
							sendDataToT_T_Component((SubscriptionsFromDavStorage)subscriptionsFromRemoteStorages[j], transmitterTelegram);
						}
					}
				}
			}
		}
	}


	/**
	 * Diese Methode wird von der Protokollsteuerung DaV-DaV aufgerufen, wenn von einem Datenverteiler ein neuer Datensatz empfangen wurde. <UL><LI> Sind die Daten
	 * auf dem Weg von einem Sender zu einem Zentraldatenverteiler, so wird bei jedem Datenverteiler zunächst überprüft, ob dieser bereits der gesuchte
	 * Zentraldatenverteiler dieser Datensätze ist, d. h., ob die Quelle oder Senke der Datensätze an diesem Datenverteiler angemeldet ist. Ist der Datensatz nicht
	 * zerstückelt, wird der Datensatzindex dieses Datensatzes bestimmt, und er wird an die Applikationen und Datenverteiler weitergeleitet, für die bei diesem
	 * Datenverteiler positiv quittierte Anmeldungen vorliegen. Wenn der Datensatz nicht nachgeliefert ist, wird er in den Datenbestand aufgenommen.  <br>Ist der
	 * Datensatz zerstückelt, dann gibt es zwei Möglichkeiten: <UL><LI>Das empfangene Telegramm ist das erste. <LI>Das empfangene Telegramm ist nicht das
	 * erste.</UL>Wenn dieser Datenverteiler nicht der Zentraldatenverteiler für diese Datensätze ist dann wird überprüft, ob ein positiv quittierter
	 * Anmeldungsvermerk für einen Sender vorliegt. Ist dies der Fall, dann wird der Datensatz zum dem Datenverteiler weitergeleitet, an dem eine positiv
	 * quittierte Folgeanmeldung vorliegt. <LI> Sind die Daten auf dem Weg von einem Zentraldatenverteiler zu einem Empfänger, so wird bei jedem Datenverteiler
	 * überprüft, ob der Datensatz zerstückelt ist. Ist der Datensatz nicht zerstückelt, wird der Datensatzindex dieses Datensatzes bestimmt und an die
	 * Applikationen und Datenverteiler weitergeleitet, für die bei diesem Datenverteiler positiv quittierte Anmeldungen vorliegen. Wenn der Datensatz nicht
	 * nachgeliefert ist, wird er in den Datenbestand aufgenommen. Anschließend wird die updatePendingSubscription-Methode der Verbindungsverwaltung aufgerufen, um
	 * die Umleitungsanträge mit den Informationen der neuen Datensätze zu versehen.  <br>Ist der Datensatz zerstückelt, dann gibt es zwei Möglichkeiten:
	 * <UL><LI>Das empfangene Telegramm ist das erste.<LI>Das empfangener Telegramm ist nicht das erste. Dann wird es mit dem Datensatzindex versehen, der dem
	 * ersten Telegramm der Zerstückelung zugewiesen wurde. Das Telegramm wird in die Zerstückelungstabelle aufgenommen, und es wird überprüft, ob alle Teile der
	 * Zerstückelung angekommen sind. Sind alle vorhanden, und sind sie nicht als nachgeliefert markiert, so werden sie alle in den Datenbestand aufgenommen.
	 * Anschließend wird die updatePendingSubscription- Methode der Verbindungsverwaltung aufgerufen, um die Umleitungsanträge mit den Informationen der neuen
	 * Datensätze zu versehen. Das empfangene Telegramm wird dann an die Applikationen und Datenverteiler weitergeleitet, die positiv quittierte Anmeldungen
	 * initiiert haben.</UL></UL> Der Datensatzindex wird so gebildet, dass die oberen 32 Bits die Zeit und die unteren 30 Bits den um eins erhöhten Index der
	 * Basisanmeldeinformation enthalten. Die Zeit wird entweder aus dem Datensatzindex des im Datenbestand vorhandenen Datensatzes gewonnen oder ist die Zeit des
	 * Startens dieses Datenverteilers. Für jede Basisanmeldeinformation wird ein Index in einer Tabelle eingerichtet, der bei jedem neuen Datensatz um eins erhöht
	 * wird und in der Tabelle wieder abgespeichert wird. Wenn ein Telegramm an eine Applikation weitergeleitet wird, muss überprüft werden, ob sich die
	 * Applikation evtl. nur für Delta-Daten oder für einen Teil der Attribute (oder beides) angemeldet hat. Wenn keine Einschränkung vorliegt, wird das Telegramm
	 * an die Applikation weitergeleitet. Bei Einschränkungen hingegen wird zunächst gewartet, bis der Datensatz komplett verfügbar ist (Zerstückelung). Je
	 * nachdem, welche Einschränkungen vorliegen, wird der Datensatz wie folgt an die Applikation weitergeleitet: <UL><LI>Keine nachgelieferten Daten: Sind die
	 * Daten nachgeliefert, werden sie nicht gesendet.<LI>Delta-Daten: Es wird überprüft, ob die Änderungsindikatoren gesetzt sind. Nur dann wird der Datensatz zur
	 * Applikation weitergeleitet. <LI>Teilattribute: die angemeldeten Teilattribute werden aus dem Datensatz herausgefiltert und als neuer Datensatz zur
	 * Applikation weitergeleitet. <LI>Delta-Daten und Teilattribute: Es wird die Schnittmenge aus den Indikatoren der angemeldeten Attribute und den Indikatoren
	 * der geänderten Attribute gebildet. Aus den Attributen dieser Schnittmenge wird ein neuer Datensatz gebildet und an die Applikation weitergeleitet. </UL>Das
	 * Senden eines Datensatzes wird natürlich nur dann erfolgen, wenn der Empfänger berechtigt ist, die Daten zu empfangen. Andernfalls wird ein spezieller
	 * Datensatz generiert, welcher den Empfänger darüber informiert, dass er keine Berechtigung hat.
	 *
	 * @param inputTransmitterConnection              Verbindung
	 * @param transmitterDataTelegram Transmitter-Daten-Telegramm
	 */
	public final void sendData(T_T_HighLevelCommunication inputTransmitterConnection, TransmitterDataTelegram transmitterDataTelegram) {
		if(transmitterDataTelegram == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		BaseSubscriptionInfo info = transmitterDataTelegram.getBaseSubscriptionInfo();
		if(info == null) {
			throw new IllegalArgumentException("Argument ist inkonsistent");
		}

		SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = getReceivingComponent(info);
		boolean save = false;
		//(See if main transmitter)
		if(subscriptionsFromApplicationStorage == null) {
			save = true;
			subscriptionsFromApplicationStorage = getSendingComponent(info);
		}
		if(transmitterDataTelegram.getDirection() == 0) { // Send to main transmitter
			if(subscriptionsFromApplicationStorage == null) {
				InAndOutSubscription inAndOutSubscription = isSuccessfullySubscribed(
						info, TransmitterSubscriptionsConstants.SENDER_SUBSCRIPTION
				);
				if((inAndOutSubscription == null) || (inAndOutSubscription._outSubscriptions == null)) {
					return; // no successfull subscription for this data
				}
				for(int i = inAndOutSubscription._outSubscriptions.length - 1; i > -1; --i) {
					if(inAndOutSubscription._outSubscriptions[i] == null) {
						continue;
					}
					if(inAndOutSubscription._outSubscriptions[i]._state == TransmitterSubscriptionsConstants.POSITIV_RECEIP) {
						long transmitterId = inAndOutSubscription._outSubscriptions[i]._targetTransmitter;
						T_T_HighLevelCommunication _connection = _connectionsManager.getTransmitterConnection(transmitterId);
						if(_connection != null) {
							_connection.sendTelegram(transmitterDataTelegram);
						}
						break;
					}
				}
			}
			else {
				if(isInvalidSubscription(info, TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION)) {
					return;
				}
				// Folgendes synchronized stellt sicher, dass Vergabe von Datensatzindex und Versand eines Telegramms zusammen durchgeführt werden, da eine
				// verzahnte Abarbeitung durch verschiedene Threads zu Vertauschungen im Datensatzindex führen könnten
				synchronized(subscriptionsFromApplicationStorage) {
					ApplicationDataTelegram telegrams[] = null;
					final TransmitterDataTelegram t_telegrams[];
					boolean noNeedToWait = false;
					long index = transmitterDataTelegram.getDataNumber();
					Long originIndex = new Long(index);
					if(transmitterDataTelegram.getTelegramNumber() == 0) {
						index = getDataIndex(info, false, index);
						if(transmitterDataTelegram.getTotalTelegramsCount() == 1) {
							noNeedToWait = true;
						}
						else {
							_usedDataIndexTable.put(originIndex, new Long(index));
						}
					}
					else {
						Long value = _usedDataIndexTable.get(originIndex);
						if(value == null) {
							throw new IllegalStateException("Inkonsistente Indexierung der Teiltelegramme");
						}
						index = value.longValue();
					}
					transmitterDataTelegram.setDataIndex(index);
					transmitterDataTelegram.setDirection((byte)1);
					ApplicationDataTelegram applicationDataTelegram = transmitterDataTelegram.getApplicationDataTelegram();
					if(noNeedToWait) {
						t_telegrams = new TransmitterDataTelegram[]{transmitterDataTelegram};
						telegrams = new ApplicationDataTelegram[]{applicationDataTelegram};
					}
					else {
						t_telegrams = _splittedTelegramsTable.put(transmitterDataTelegram);
						if(t_telegrams != null) {
							_usedDataIndexTable.remove(originIndex);
							telegrams = new ApplicationDataTelegram[t_telegrams.length];
							for(int i = 0; i < t_telegrams.length; ++i) {
								telegrams[i] = t_telegrams[i].getApplicationDataTelegram();
							}
						}
						else {
							save = false;
						}
					}

					saveAndSendToInterestedReceivingComponents(save, t_telegrams, transmitterDataTelegram, info, telegrams, applicationDataTelegram);
				}
			}
		}
		else {
			if(isInvalidSubscription(info, TransmitterSubscriptionsConstants.RECEIVER_SUBSCRIPTION)) {
				return;
			}
			ApplicationDataTelegram telegrams[] = null;
			ApplicationDataTelegram applicationDataTelegram = transmitterDataTelegram.getApplicationDataTelegram();
			TransmitterDataTelegram[] t_telegrams = null;
			if((applicationDataTelegram.getTotalTelegramsCount() == 1) && (applicationDataTelegram.getTelegramNumber() == 0)) {
				if(_connectionsManager.updatePendingSubscription(info, inputTransmitterConnection.getId(), transmitterDataTelegram.getDataNumber())) {
					telegrams = new ApplicationDataTelegram[]{applicationDataTelegram};
				}
				if(save) {
					t_telegrams = new TransmitterDataTelegram[]{transmitterDataTelegram};
				}
			}
			else {
				t_telegrams = _splittedTelegramsTable.put(transmitterDataTelegram);
				if(t_telegrams != null) {
					if(_connectionsManager.updatePendingSubscription(
							info, inputTransmitterConnection.getId(), transmitterDataTelegram.getDataNumber()
					)) {
						telegrams = new ApplicationDataTelegram[t_telegrams.length];
						for(int i = 0; i < t_telegrams.length; ++i) {
							telegrams[i] = t_telegrams[i].getApplicationDataTelegram();
						}
					}
				}
				else {
					save = false;
				}
			}
			saveAndSendToInterestedReceivingComponents(save, t_telegrams, transmitterDataTelegram, info, telegrams, applicationDataTelegram);
		}
	}

	/**
	 * Leitet ein Telegramm an einen anderen Datenverteiler weiter.
	 *
	 * @param subscriptionsFromDavStorage
	 * @param transmitterDataTelegram
	 */
	private void sendDataToT_T_Component(
			SubscriptionsFromDavStorage subscriptionsFromDavStorage, TransmitterDataTelegram transmitterDataTelegram
	) {
		T_T_HighLevelCommunication _connection = (T_T_HighLevelCommunication)subscriptionsFromDavStorage.getConnection();
		if(_connection != null) {
			BaseSubscriptionInfo info = transmitterDataTelegram.getBaseSubscriptionInfo();
			_connection.sendTelegram(transmitterDataTelegram);
		}
	}

	/**
	 * Leitet ein Telegramm an eine Applikation weiter.
	 *
	 * @param subscriptionsFromApplicationStorage
	 * @param allAppTelegrams
	 * @param lastAppTelegram
	 * @param transmitterTelegrams
	 * @param previousTransmitterTelegrams
	 */
	private void sendDataToT_A_Component(
			SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage,
			ApplicationDataTelegram allAppTelegrams[],
			ApplicationDataTelegram lastAppTelegram,
			final TransmitterDataTelegram[] transmitterTelegrams,
			final TransmitterDataTelegram[] previousTransmitterTelegrams) {
		T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)subscriptionsFromApplicationStorage.getConnection();
		if(_connection != null) {
			BaseSubscriptionInfo info = lastAppTelegram.getBaseSubscriptionInfo();
			ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(info);
			if(receiveSubscriptionInfo != null) {
				if((receiveSubscriptionInfo.getDelayedDataFlag() == false) && (lastAppTelegram.getDelayedDataFlag() == true)) {
					return;
				}
				long lastDataIndex = receiveSubscriptionInfo.getLastDataIndex();
				receiveSubscriptionInfo.setLastDataIndex(lastAppTelegram.getDataNumber());
				lastAppTelegram.setAttributesIndicator(null);
				if(receiveSubscriptionInfo.getDeltaDataFlag()) {
					if((allAppTelegrams != null) && (allAppTelegrams.length > 0) && (allAppTelegrams[0] != null)) {
						boolean sendAll = true;
						if(isDeltaAllowed(info)) {
							if((lastDataIndex > 0) && ((lastDataIndex & 0x0000000000000003L) == 0)
							   && transmitterTelegrams != null && previousTransmitterTelegrams != null
							   && transmitterTelegrams.length == previousTransmitterTelegrams.length && !transmitterTelegrams[0].getDelayedDataFlag()
							   && transmitterTelegrams[0].getErrorFlag() == 0 && previousTransmitterTelegrams[0].getErrorFlag() == 0) {
								sendAll = false;
								for(int i = transmitterTelegrams.length - 1; i >= 0; i--) {
									TransmitterDataTelegram transmitterTelegram = transmitterTelegrams[i];
									TransmitterDataTelegram previousTransmitterTelegram = previousTransmitterTelegrams[i];
									if(!Arrays.equals(transmitterTelegram.getData(), previousTransmitterTelegram.getData())) {
										sendAll = true;
										break;
									}

								}
							}
						}
						if(sendAll) {
							// Alle Telegramme des Datensatzes weiterleiten
							_connection.sendData(allAppTelegrams);
						}
					}
				}
				else {
					_connection.sendData(lastAppTelegram);
				}
			}
		}
	}

	/**
	 * Leitet ein Telegramm an eine Applikation weiter.
	 *
	 * @param subscriptionsFromApplicationStorage
	 * @param info
	 */
	final void sendDataToT_A_Component(SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage, BaseSubscriptionInfo info) {
		if((subscriptionsFromApplicationStorage == null) && (info == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ApplicationDataTelegram telegrams[] = _connectionsManager.cacheManager.getCurrentDataForApplication(info);
		if((telegrams == null) || (telegrams.length == 0) || (telegrams[0] == null)) {
			return;
		}
		T_A_HighLevelCommunication _connection = (T_A_HighLevelCommunication)subscriptionsFromApplicationStorage.getConnection();
		if(_connection != null) {
			ReceiveSubscriptionInfo receiveSubscriptionInfo = subscriptionsFromApplicationStorage.getReceiveSubscription(info);
			if(receiveSubscriptionInfo != null) {
				if((receiveSubscriptionInfo.getDelayedDataFlag() == false) && (telegrams[0].getDelayedDataFlag() == true)) {
					return;
				}

				receiveSubscriptionInfo.setLastDataIndex(telegrams[0].getDataNumber());
				telegrams[0].setAttributesIndicator(null);
				_connection.sendData(telegrams);
			}
		}
	}


	/**
	 * Erzeugt ein Teildatensatz mit den spezifizierten und geänderten Attributen.
	 *
	 * @param telegrams
	 * @param indicators
	 * @param changes
	 *
	 * @return
	 */
	@Deprecated
	private ApplicationDataTelegram[] getDeltaSubDataObject(
			ApplicationDataTelegram telegrams[], byte[] indicators, byte[] changes
	) {
		if((telegrams == null) || (telegrams.length == 0) || (telegrams[0] == null)) {
			throw new IllegalArgumentException("Übergabeparameter ist null");
		}

		// Put all the bytes together
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		try {
			for(int i = 0; i < telegrams.length; ++i) {
				if(telegrams[i] != null) {
					byte byteArray[] = telegrams[i].getData();
					if(byteArray != null) {
						byteBuffer.write(byteArray);
					}
				}
			}
		}
		catch(IOException ex) {
			ex.printStackTrace();
			return null;
		}

		//Get the Datavalues out of the stream
		DataValue values[] = null;
		if(byteBuffer.size() > 0) {
			try {
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(byteBuffer.toByteArray()));
				final DataModel dataModel = _connectionsManager.dataModel;
				final BaseSubscriptionInfo baseSubscriptionInfo = telegrams[0].getBaseSubscriptionInfo();
				final AttributeGroupUsage attributeGroupUsage = dataModel.getAttributeGroupUsage(baseSubscriptionInfo.getUsageIdentification());
				final AttributeGroup atg = attributeGroupUsage.getAttributeGroup();
				values = StreamFetcher.getInstance().getDataValuesFromStream(
						_connectionsManager.dataModel, atg, in
				);
			}
			catch(IOException ex) {
				ex.printStackTrace();
				return null;
			}
		}

		// Empty telegram (no data inside)
		if(values == null) {
			byte errorFlag = telegrams[0].getErrorFlag();
			long dataIndex = telegrams[0].getDataNumber();
			if((telegrams.length == 1) && ((errorFlag != 0x00) || ((dataIndex & 0x0000000000000003) != 0))) { // only one error telegram
				if(telegrams[0] != null) {
					telegrams[0].setAttributesIndicator(indicators);
				}
				return telegrams;
			}
			else {
				return null;
			}
		}

		byte lastMask = (byte)0xFF;
		int length = (values.length / 8);
		int diff = values.length - (length * 8);
		if(diff > 0) {
			++length;
			lastMask = (byte)(Math.pow(2, diff) - 1);
		}
		// Gather the specified attributes
		byte _indicators[] = new byte[length];
		ArrayList<DataValue> subList = new ArrayList<DataValue>();
		for(int i = 0; i < length; ++i) {
			byte b1 = indicators == null ? (byte)0xFF : indicators[i];
			byte b2 = changes == null ? (byte)0xFF : changes[i];
			_indicators[i] = (byte)(b1 & b2);

			int index = i * 8;
			if(((b1 & 0x01) == 0x01) && ((b2 & 0x01) == 0x01) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x02) == 0x02) && ((b2 & 0x02) == 0x02) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x04) == 0x04) && ((b2 & 0x04) == 0x04) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x08) == 0x08) && ((b2 & 0x08) == 0x08) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x10) == 0x10) && ((b2 & 0x10) == 0x10) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x20) == 0x20) && ((b2 & 0x20) == 0x20) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x40) == 0x40) && ((b2 & 0x40) == 0x40) && (index < values.length)) {
				subList.add(values[index]);
			}
			++index;
			if(((b1 & 0x80) == 0x80) && ((b2 & 0x80) == 0x80) && (index < values.length)) {
				subList.add(values[index]);
			}
		}
		_indicators[length - 1] &= lastMask;
		// put them to one Object
		int size = subList.size();
		if(size == 0) {
			return null;
		}
		byteBuffer = new ByteArrayOutputStream();
		try {
			DataOutputStream out = new DataOutputStream(byteBuffer);
			if(size > 0) {
				for(int i = 0; i < size; ++i) {
					DataValue attribute = subList.get(i);
					if(attribute != null) {
						attribute.write(out);
					}
				}
			}
			out.flush();
			out.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
			return null;
		}
		byte data[] = byteBuffer.toByteArray();
		SendDataObject sendDataObject = new SendDataObject(
				telegrams[0].getBaseSubscriptionInfo(),
				telegrams[0].getDelayedDataFlag(),
				telegrams[0].getDataNumber(),
				telegrams[0].getDataTime(),
				telegrams[0].getErrorFlag(),
				_indicators,
				data
		);
		// splitt the new object to small telegrams
		return TelegramUtility.splitToApplicationTelegrams(sendDataObject);
	}

	/**
	 *  Gibt die fortlaufende Nummer der Daten zurück
	 *
	 * @param info
	 * @param source
	 * @param originIndex
	 *
	 * @return
	 */
	private synchronized long getDataIndex(BaseSubscriptionInfo info, boolean source, long originIndex) {
		if(info == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		long time;
		if(source) {
			time = originIndex & 0xFFFFFFFF00000000L;
		}
		else {
			time = _connectionsManager.cacheManager.getCurrentDataIndex(info);
			if(time == 0) {
				time = CommunicationConstant.START_TIME;
			}
			else {
				time = time & 0xFFFFFFFF00000000L;
			}
		}
		Object obj = _dataIndexTable.get(info);
		int sendIndex;
		if(obj == null) {
			sendIndex = (int)((originIndex >> 2) & 0x000000003FFFFFFFL);
			if(sendIndex > 0x3FFFFFFF) {
				sendIndex = 1;
			}
		}
		else {
			sendIndex = ((Integer)obj).intValue();
			if(++sendIndex > 0x3FFFFFFF) {
				sendIndex = 1;
			}
		}
		_dataIndexTable.put(info, new Integer(sendIndex));
		long dataNumber = ((time | (((long)sendIndex << 2) & 0x00000000FFFFFFFCL)) & 0xFFFFFFFFFFFFFFFCL);
		//System.out.println("originIndex = " + originIndex + " source = " + source + " dataNumber = " + dataNumber);
		return dataNumber;
	}

	/**
	 * Gibt die nächste laufende Datenindexnummer für eine gegebene Anmeldung zurück.
	 *
	 * @param info Anmeldungs-Info
	 *
	 * @return Eine Zahl von 1 bis 0x3FFFFFFF
	 *
	 * @throws ArithmeticException bei einem Überlauf der Nummer. In dem Fall sollte 1 als Ergebnis angenommen werden und der Zeitstempel des Datenindex erhöht
	 *                             werden, um Monotonie sicherzustellen
	 */
	public synchronized int getDataIndexIndex(final BaseSubscriptionInfo info) {
		final Integer indexFromTable = _dataIndexTable.get(info);
		final int index;
		if(indexFromTable == null) {
			index = 1;
		}
		else {
			index = indexFromTable + 1;
			if(index > 0x3FFFFFFF) {
				_dataIndexTable.put(info, 1);
				throw new ArithmeticException();
			}
		}
		_dataIndexTable.put(info, index);
		return index;
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung und von sich selbst aufgerufen, um festzustellen, welche Verbindungen am Empfang der Datensätze für die
	 * spezifizierte Basisanmeldeinformation interessiert sind. Alle Anmeldekomponenten der Verbindungen werden darauf untersucht, ob eine Senken- oder
	 * Empfangsanmeldung für das spezifizierte Datum existiert. Ist keine Senke und kein Empfänger angemeldet, so wird Null zurückgegeben.
	 *
	 * @param info Basisanmeldeinformationen
	 *
	 * @return Feld der interessierten Empfänger(Empfänger oder Senke)
	 */
	final SubscriptionsFromRemoteStorage[] getInterrestedReceivingComponent(BaseSubscriptionInfo info) {
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		ArrayList<SubscriptionsFromRemoteStorage> result = new ArrayList<SubscriptionsFromRemoteStorage>();
		synchronized(_subscriptionsFromRemoteStorages) {
			ArrayList<SubscriptionsFromRemoteStorage> list = new ArrayList<SubscriptionsFromRemoteStorage>(_subscriptionsFromRemoteStorages.values());
			for(int i = list.size() - 1; i > -1; --i) {
				SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = list.get(i);
				if(subscriptionsFromRemoteStorage != null) {
					if(subscriptionsFromRemoteStorage.getType() == SubscriptionsFromRemoteStorage.T_A) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromApplicationStorage.getReceiveSubscription(info) != null) {
							result.add(subscriptionsFromApplicationStorage);
						}
					}
					else {
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromDavStorage.getReceiveSubscription(info) != null) {
							result.add(subscriptionsFromDavStorage);
						}
					}
				}
			}
		}
		int size = result.size();
		if(size == 0) {
			return null;
		}
		else {
			SubscriptionsFromRemoteStorage array[] = new SubscriptionsFromRemoteStorage[size];
			for(int i = 0; i < size; ++i) {
				array[i] = result.get(i);
			}
			return array;
		}
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung und von sich selbst aufgerufen, um festzustellen, welche Verbindungen sich für das Senden von Datensätzen
	 * für die spezifizierte Basisanmeldeinformation angemeldet haben. Alle Anmeldekomponenten der Verbindungen werden darauf untersucht, ob eine Qell- oder
	 * Sendeanmeldung für das spezifizierten Datum existiert. Ist keine Quelle und kein Sender angemeldet, so wird Null zurückgegeben.
	 *
	 * @param info Basisanmeldeinformationen
	 *
	 * @return Feld der Sender(Sender oder Quelle) eines bestimmten Datums
	 */
	final SubscriptionsFromRemoteStorage[] getInterrestedSendingComponent(BaseSubscriptionInfo info) {
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		ArrayList<SubscriptionsFromRemoteStorage> result = new ArrayList<SubscriptionsFromRemoteStorage>();
		synchronized(_subscriptionsFromRemoteStorages) {
			ArrayList<SubscriptionsFromRemoteStorage> list = new ArrayList<SubscriptionsFromRemoteStorage>(_subscriptionsFromRemoteStorages.values());
			for(int i = list.size() - 1; i > -1; --i) {
				SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = list.get(i);
				if(subscriptionsFromRemoteStorage != null) {
					if(subscriptionsFromRemoteStorage.getType() == SubscriptionsFromRemoteStorage.T_A) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromApplicationStorage.getSendSubscription(info) != null) {
							result.add(subscriptionsFromApplicationStorage);
						}
					}
					else {
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromDavStorage.getSendSubscription(info) != null) {
							result.add(subscriptionsFromDavStorage);
						}
					}
				}
			}
		}
		int size = result.size();
		if(size == 0) {
			return null;
		}
		else {
			SubscriptionsFromRemoteStorage array[] = new SubscriptionsFromRemoteStorage[size];
			for(int i = 0; i < size; ++i) {
				array[i] = result.get(i);
			}
			return array;
		}
	}

	/**
	 * Delta ist zulässig wenn nicht mehr als ein Sender oder Quelle eines Datums vorhanden ist.
	 *
	 * @param info
	 *
	 * @return
	 */
	private boolean isDeltaAllowed(BaseSubscriptionInfo info) {
		if(_subscriptionsFromRemoteStorages == null) {
			throw new IllegalArgumentException("Anmeldemanager ist nicht fertig initiallisiert");
		}
		int senderNumber = 0;
		synchronized(_subscriptionsFromRemoteStorages) {
			ArrayList<SubscriptionsFromRemoteStorage> list = new ArrayList<SubscriptionsFromRemoteStorage>(_subscriptionsFromRemoteStorages.values());
			for(int i = list.size() - 1; i > -1; --i) {
				SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = list.get(i);
				if(subscriptionsFromRemoteStorage != null) {
					if(subscriptionsFromRemoteStorage.getType() == SubscriptionsFromRemoteStorage.T_A) {
						SubscriptionsFromApplicationStorage subscriptionsFromApplicationStorage = (SubscriptionsFromApplicationStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromApplicationStorage.getSendSubscription(info) != null) {
							++senderNumber;
						}
					}
					else {
						SubscriptionsFromDavStorage subscriptionsFromDavStorage = (SubscriptionsFromDavStorage)subscriptionsFromRemoteStorage;
						if(subscriptionsFromDavStorage.getSendSubscription(info) != null) {
							++senderNumber;
						}
					}
					if(senderNumber > 1) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Überprüft ob mehr als eine Quelle für ein Datum im System vorhanden ist.
	 *
	 * @param key
	 * @param subscriptionState
	 *
	 * @return
	 */
	private boolean isInvalidSubscription(BaseSubscriptionInfo key, byte subscriptionState) {
		if(key == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		ArrayList<InAndOutSubscription> list = _inOutSubscriptionsTable.get(key);
		if(list != null) {
			synchronized(list) {
				for(int i = list.size() - 1; i > -1; --i) {
					InAndOutSubscription subscription = list.get(i);
					if(subscription != null) {
						if((subscription._role == subscriptionState)
						   && (subscription._state == TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP) && (subscription._outSubscriptions
						                                                                                                  != null)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/** Gibt alle Anmeldungen zu Debug-Zwecken aus*/
	public void printSubscriptions() {
		final Enumeration<Long> keys = _subscriptionsFromRemoteStorages.keys();
		while(keys.hasMoreElements()) {
			Long id = keys.nextElement();
			SubscriptionsFromRemoteStorage subscriptionsFromRemoteStorage = _subscriptionsFromRemoteStorages.get(id);
			System.out.println("id " + id + ", subscriptionsFromRemoteStorage " + subscriptionsFromRemoteStorage.toString());
			subscriptionsFromRemoteStorage.printSubscriptions();
		}
	}

	public void dumpSubscriptions() {
		print("", "   ", "SubscriptionsManger");
	}

	public void dumpRoutingTable() {
		getConnectionsManager().dumpRoutingTable();
	}

	public void dumpSubscriptionLists() {
		getConnectionsManager().dumpSubscriptionsLists();
	}

	public long[] getPotentialCentralDavs(final long objectId, final long attributeUsageId) {
		final BaseSubscriptionInfo baseSubscriptionInfo = new BaseSubscriptionInfo(objectId, attributeUsageId, (short)0);
		final long[] davs = getConnectionsManager().getListsManager().getPotentialCentralDavs(baseSubscriptionInfo);
		System.out.println("SubscriptionsManager.getPotentialCentralDavs(" + objectId + ", " + attributeUsageId + "): " + Arrays.toString(davs));
		return davs;
	}

	public void print(String initialIndent, String additionalIndent, String name) {
		System.out.println("###########################################################################################");
		System.out.println(initialIndent + "_subscriptionsFromRemoteStorages: Hashtable{");
		synchronized(_subscriptionsFromRemoteStorages) {
			final Set<Map.Entry<Long, SubscriptionsFromRemoteStorage>> entries = _subscriptionsFromRemoteStorages.entrySet();
			for(Map.Entry<Long, SubscriptionsFromRemoteStorage> entry : entries) {
				System.out.println(initialIndent + additionalIndent + "key: " + entry.getKey());
				entry.getValue().print(initialIndent + additionalIndent + additionalIndent, additionalIndent, "value");
			}
		}
		System.out.println("}");
		System.out.println("===========================================================================================");
		System.out.println(initialIndent + "_inOutSubscriptionsTable: Hashtable{");
		synchronized(_inOutSubscriptionsTable) {
			final Set<Map.Entry<BaseSubscriptionInfo, ArrayList<InAndOutSubscription>>> entries = _inOutSubscriptionsTable.entrySet();
			for(Map.Entry<BaseSubscriptionInfo, ArrayList<InAndOutSubscription>> entry : entries) {
				System.out.println(initialIndent + additionalIndent + "key: " + entry.getKey());
				printList(initialIndent + additionalIndent, additionalIndent, "value", entry.getValue());
			}
		}

		System.out.println(initialIndent + "}");
		System.out.println("###########################################################################################");
	}

	private void printList(final String initialIndent, final String additionalIndent, String name, final List<InAndOutSubscription> list) {
		System.out.println(initialIndent + name + ": List{");
		for(InAndOutSubscription entry : list) {
			entry.print(initialIndent + additionalIndent, additionalIndent, "element");
		}
		System.out.println(initialIndent + "}");
	}


	public int getSubscriptionsTableSize() {
		return _subscriptionsFromRemoteStorages.size();
	}

	public int getInOutSubscriptionsTableSize() {
		return _inOutSubscriptionsTable.size();
	}

	public Hashtable getDataIndexTable() {
		return _dataIndexTable;
	}

	public Hashtable getUsedDataIndexTable() {
		return _usedDataIndexTable;
	}

	public ConnectionsManager getConnectionsManager() {
		return _connectionsManager;
	}

	public SplittedTransmitterTelegramsTable getSplittedTelegramsTable() {
		return _splittedTelegramsTable;
	}
}
