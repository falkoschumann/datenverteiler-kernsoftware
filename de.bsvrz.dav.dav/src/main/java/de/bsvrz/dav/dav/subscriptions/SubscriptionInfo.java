/*
 * Copyright 2013 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.subscriptions;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.dav.main.ConnectionState;
import de.bsvrz.dav.dav.main.SubscriptionsManager;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Diese Klasse kapselt eine Anmeldungsinformation zu einer Objekt/Attributgruppe/Aspekt/Simulationsvariante-Kombination. Enthalten sind die
 * eigentlichen Anmeldungen von Applikationen und Datenverteilern auf diese BaseSubscriptionInfo. Diese Klasse kümmert sich darum, die
 * Anmeldungen zu verwalten und je nach Verfügbarkeit von Sendern, Empfängern, Quellen und Senken und je nach vorhandenen Rechten den
 * einzelnen Verbindungen per Sendesteuerung oder leeren Datensätzen den Zustand der Anmeldung zu übermitteln. Zusätzlich übernimmt diese
 * Klasse das Verteilen von Datensätzen an interessierte und gültige Empfangsanmeldungen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11476 $
 */
public class SubscriptionInfo implements Closeable {

	private static final Debug _debug = Debug.getLogger();

	/**
	 * Liste mit Anmeldungen
	 */
	private final SubscriptionList _subscriptionList = new SubscriptionList();
	/**
	 * Referenz auf die Anmeldungsverwaltung
	 */
	private final SubscriptionsManager _subscriptionsManager;
	/**
	 * Datenidentifikation
	 */
	private final BaseSubscriptionInfo _baseSubscriptionInfo;
	/**
	 * Laufende Anmeldeumleitungen, enthalt eine Map mit Zuordnung ZentralverteilerId->Neue Verbindung. In dieser Map sind die neuen
	 * Verbindungen gespeichert, während sie noch aufgebaut werden. Nachdem die verbindung erfolgreich aufgebaut wurde, wird dann die
	 * eigentliche Anmeldung in der {@link SubscriptionList} umgebogen und der eintrag aus dieser Map entfernt.
	 */
	private final HashMap<Long, PendingSubscription> _pendingSubscriptions = new HashMap<Long, PendingSubscription>();
	/**
	 * Soll versucht werden, sich zu einem entfernten Datenverteiler zu verbinden? (True wenn keine lokale Quelle oder Senke vorhanden ist)
	 */
	private boolean _connectToRemoteCentralDistributor = false;
	/**
	 * Zwischenspeicher für die zuletzt gesendeten Telegramme einer Quelle
	 */
	private List<ApplicationDataTelegram> _lastSendTelegrams = null;
	/**
	 * Letzter gesendeter/weitergeleiteter Datenindex (1 = kein oder nur ein künstlicher Datensatz vorher gesendet). Die eigentliche
	 * Datenindexgenerierung im Zentraldatenverteiler findet in der {@link SubscriptionList}-Klasse statt.
	 */
	private long _lastSendDataIndex = 1;
	/**
	 * Sind Anmeldungen gesperrt, weil es mehrere Remote-Datenverteiler mit positiven Rückmeldungen gibt?
	 */
	private boolean _multiRemoteLockActive = false;
	/**
	 * Ist true während die Remote-Anmeldungen aktualisiert werden. Verhindert, dass {@link #setConnectToRemoteCentralDistributor(boolean)}
	 * rekursiv aufgerufen wird, wodurch störende Effekte entstehen können.
	 */
	private boolean _remoteUpdateLockActive = false;
	private int _referenceCounter = 0;

	/**
	 * Erstellt eine neue SubscriptionInfo
	 *
	 * @param subscriptionsManager Anmeldungsverwaltung
	 * @param baseSubscriptionInfo Datenidentifikation
	 */
	public SubscriptionInfo(final SubscriptionsManager subscriptionsManager, final BaseSubscriptionInfo baseSubscriptionInfo) {
		_subscriptionsManager = subscriptionsManager;
		_baseSubscriptionInfo = baseSubscriptionInfo;
	}

	/**
	 * Prüft ob 2 Telegrammlisten im Sinne der Anmeldung auf Delta-Datensätze gleich sind.
	 *
	 * @param telegrams1 Daten-Telegramme 1
	 * @param telegrams2 Daten-Telegramme 2
	 * @return True wenn Daten vorhanden und identisch sind
	 */
	private static boolean telegramsAreEqual(final List<ApplicationDataTelegram> telegrams1, final List<ApplicationDataTelegram> telegrams2) {
		if(telegrams1 == null || telegrams2 == null) return false;

		if(telegrams1.size() != telegrams2.size()) return false;

		if(telegrams1.get(0).getErrorFlag() != 0 || telegrams2.get(0).getErrorFlag() != 0) return false;

		for(int i = 0, size = telegrams1.size(); i < size; i++) {
			final ApplicationDataTelegram telegram1 = telegrams1.get(i);
			final ApplicationDataTelegram telegram2 = telegrams2.get(i);
			if(!Arrays.equals(telegram1.getData(), telegram2.getData())) return false;
		}
		return true;
	}

	/**
	 * Prüft ob eine Anmeldung lokal ist
	 *
	 * @param subscription Anmeldung
	 * @return true wenn lokal
	 */
	private static boolean isLocalSubscription(final Subscription subscription) {
		return subscription != null && subscription instanceof LocalSubscription;
	}

	/**
	 * Fügt eine sendende Anmeldung hinzu
	 *
	 * @param sendingSubscription neue sendende Anmeldung
	 */
	public synchronized void addSendingSubscription(final SendingSubscription sendingSubscription) {
		_subscriptionList.addSender(sendingSubscription);
		refreshSubscriptionsOnNewSender(sendingSubscription);
	}

	/**
	 * Fügt eine empfangende Anmeldung hinzu
	 *
	 * @param receivingSubscription neue empfangende Anmeldung
	 */
	public synchronized void addReceivingSubscription(final ReceivingSubscription receivingSubscription) {
		_subscriptionList.addReceiver(receivingSubscription);
		refreshSubscriptionsOnNewReceiver(receivingSubscription);
	}

	/**
	 * Aktualisiert die Anmeldungszustände wenn ein neuer Sender/eine neue Quelle angemeldet wird
	 *
	 * @param sendingSubscription neue sendende Anmeldung
	 */
	private void refreshSubscriptionsOnNewSender(final SendingSubscription sendingSubscription) {
		if(!sendingSubscription.getConnectionState().isValid()) return;
		if(_multiRemoteLockActive) {
			sendingSubscription.setState(SenderState.MULTIPLE_REMOTE_LOCK, getCentralDistributorId());
			return;
		}
		if(!sendingSubscription.isAllowed()) {
			sendingSubscription.setState(SenderState.NOT_ALLOWED, getCentralDistributorId());
			return;
		}
		if(sendingSubscription.isSource()) {
			if(isLocalSubscription(sendingSubscription)) setConnectToRemoteCentralDistributor(false);
			if(_subscriptionList.canSetSource(sendingSubscription)) {
				setSource(sendingSubscription);
			}
			else {
				sendingSubscription.setState(SenderState.INVALID_SUBSCRIPTION, getCentralDistributorId());
				return;
			}
		}
		sendingSubscription.setState(SenderState.WAITING, getCentralDistributorId());
		updateSenderReceiverStatus();
	}

	/**
	 * Aktualisiert die Anmeldungszustände wenn eine neue Senke oder ein Empfänger angemeldet wird
	 *
	 * @param receivingSubscription neue empfangende Anmeldung
	 */
	private void refreshSubscriptionsOnNewReceiver(final ReceivingSubscription receivingSubscription) {
		if(!receivingSubscription.getConnectionState().isValid()) return;
		if(_multiRemoteLockActive) {
			receivingSubscription.setState(ReceiverState.MULTIPLE_REMOTE_LOCK, getCentralDistributorId());
			return;
		}
		if(!receivingSubscription.isAllowed()) {
			receivingSubscription.setState(ReceiverState.NOT_ALLOWED, getCentralDistributorId());
			receivingSubscription.sendStateTelegram(ReceiverState.NOT_ALLOWED);
			return;
		}
		if(receivingSubscription.isDrain()) {
			if(isLocalSubscription(receivingSubscription)) setConnectToRemoteCentralDistributor(false);
			if(_subscriptionList.canSetDrain(receivingSubscription)) {
				setDrain(receivingSubscription);
			}
			else {
				receivingSubscription.setState(ReceiverState.INVALID_SUBSCRIPTION, getCentralDistributorId());
				receivingSubscription.sendStateTelegram(ReceiverState.INVALID_SUBSCRIPTION);
				return;
			}
		}
		receivingSubscription.setState(ReceiverState.WAITING, getCentralDistributorId());
		updateSenderReceiverStatus();
	}

	/**
	 * Aktualisiert den Anmeldestatus von den angemeldeten gültigen (d.h. nicht-verbotenen und nicht ungültigen) Anmeldungen
	 */
	private void updateSenderReceiverStatus() {
		updateRemoteConnectionsNecessary();

		if(hasPendingRemoteSubscriptions()) return;

		long centralDistributorId = getCentralDistributorId();
		final List<SendingSubscription> sendingSubscriptions = getValidSenderSubscriptions();
		final List<ReceivingSubscription> receivingSubscriptions = getValidReceiverSubscriptions();
		if(sendingSubscriptions.isEmpty() || !_subscriptionList.hasDrainOrSource()) {
			// Es gibt keine Sender, oder es sind nur Sender und Empfänger vorhanden
			// -> Statusmeldung "Keine Quelle" an Empfänger
			for(ReceivingSubscription subscription : receivingSubscriptions) {
				ReceiverState prevState = subscription.getState();
				if(prevState != ReceiverState.NO_SENDERS) {
					subscription.setState(ReceiverState.NO_SENDERS, centralDistributorId);

					// Status-Datensatz "keine Quelle" nur an Senken senden, wenn die Senke vorher einen ungültigen Status hatte.
					// Senken brauchen ansonsten nicht über das (nicht) vorhandensein von Sendern informiert zu werden.
					if(!subscription.isDrain() || prevState != ReceiverState.SENDERS_AVAILABLE) {
						subscription.sendStateTelegram(ReceiverState.NO_SENDERS);
					}
				}
			}
			// -> Sendesteuerung keine Empfänger an evtl. vorhandene Sender
			for(SendingSubscription sendingSubscription : sendingSubscriptions) {
				sendingSubscription.setState(SenderState.NO_RECEIVERS, centralDistributorId);
			}
		}
		else if(receivingSubscriptions.isEmpty()) {
			// -> Sendesteuerung keine Empfänger an evtl. vorhandene Quellen/Sender
			for(SendingSubscription sendingSubscription : sendingSubscriptions) {
				sendingSubscription.setState(SenderState.NO_RECEIVERS, centralDistributorId);
			}
		}
		else {
			// Es gibt Quelle und Empfänger oder Senke und Sender

			// Falls es eine Quelle gibt, den evtl. gespeicherten Datensatz an alle Empfänger weiterleiten.
			if(hasSource()) {
				for(ReceivingSubscription subscription : receivingSubscriptions) {
					if(subscription.getState() != ReceiverState.SENDERS_AVAILABLE) {
						subscription.setState(ReceiverState.SENDERS_AVAILABLE, centralDistributorId);
						if(_lastSendTelegrams != null) {
							for(final ApplicationDataTelegram telegram : _lastSendTelegrams) {
								subscription.sendDataTelegram(telegram);
							}
						}
						else if(_subscriptionList.isCentralDistributor()) {
							// Es kann Situationen geben, in dem kein Datensatz gespeichert ist, z.B. falls die Quelle
							// beim Versenden des initialen Datensatzes keine Rechte hatte und aufgrund fehlender
							// Sendesteuerung beim wieder gültig werden keinen neuen Datensatz verschickt.
							//
							// Daher wird hier mangels besserer Alternativen an die Empfänger ein
							// "Keine Quelle"-Datensatz geschickt
							subscription.sendStateTelegram(ReceiverState.NO_SENDERS);
						}
					}
				}
			}
			else {
				// Falls es eine Senke gibt, Senke benachrichtigen, dass alles funktioniert hat (NO_SENDERS senden),
				// das gleiche mit Empfängern machen, die neben einer Senke angemeldet wurden
				// Wenn es weder eine Quelle oder Senke gibt, ebenfalls den Status auf NO_SENDERS setzen
				for(ReceivingSubscription subscription : receivingSubscriptions) {
					ReceiverState prevState = subscription.getState();
					if(prevState != ReceiverState.SENDERS_AVAILABLE) {
						subscription.setState(ReceiverState.SENDERS_AVAILABLE, centralDistributorId);
						// hier wird "keine Quelle" an Senke gesendet, obwohl Sender vorhanden sind
						// das ist notwendig, weil es kein "Es gibt Sender"-telegramm gibt bzw. eine Senke sich nicht
						// dafür interessieren sollte ob es sender gibt oder nicht.
						if(prevState != ReceiverState.NO_SENDERS) {
							subscription.sendStateTelegram(ReceiverState.NO_SENDERS);
						}
					}
				}
			}

			for(final SendingSubscription sendingSubscription : sendingSubscriptions) {
				sendingSubscription.setState(SenderState.RECEIVERS_AVAILABLE, centralDistributorId);
			}
		}
	}

	/**
	 * Gibt zurück, ob noch Anmeldungen bei anderen Datenverteilern laufen und daher derzeit keine Aktualisierungen von Anmeldungen erfolgen
	 * sollten. Zum beispiel wein ein lokaler Empfänger angemeldet wird liefert diese Funktion true zurück, bis es entweder eine lokale
	 * Quelle gibt, ein Datenverteiler die Anmeldung positiv quittiert hat, oder alle in Frage kommenden Datenverteiler eine negative
	 * Quittung gesendet haben.
	 *
	 * @return true wenn derzeit noch Anmeldungen im Gange sind und es keine positive Rückmeldung gibt.
	 */
	private boolean hasPendingRemoteSubscriptions() {
		int numWaiting = 0;
		int numPositive = 0;
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(!(sendingSubscription instanceof RemoteSourceSubscription)) continue;
			if(sendingSubscription.getConnectionState() == ConnectionState.TO_REMOTE_WAITING) numWaiting++;
			else if(sendingSubscription.getConnectionState().isValid()) numPositive++;
		}
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(!(receivingSubscription instanceof RemoteDrainSubscription)) continue;
			if(receivingSubscription.getConnectionState() == ConnectionState.TO_REMOTE_WAITING) numWaiting++;
			else if(receivingSubscription.getConnectionState().isValid()) numPositive++;
		}
		return numPositive == 0 && numWaiting > 0;
	}

	/**
	 * Aktualisiert den Anmeldestatus von allen Anmeldungen wenn sich ein Sender abmeldet
	 *
	 * @param toRemove Abmeldender Sender/Quelle
	 */
	private void refreshSubscriptionsOnSenderRemoval(final SendingSubscription toRemove) {
		long centralDistributorId = getCentralDistributorId();
		List<SendingSubscription> validSenderSubscriptions = getValidSenderSubscriptions();
		if(validSenderSubscriptions.size() == 0) {
			for(final ReceivingSubscription receivingSubscription : getValidReceiverSubscriptions()) {
				receivingSubscription.setState(ReceiverState.NO_SENDERS, centralDistributorId);
				if(!_subscriptionList.hasDrain()) {
					// Senken werden nicht informiert, wenn der letzte Sender sich abgemeldet hat, das gleiche trifft auf daneben angemeldete Empfänger zu.
					receivingSubscription.sendStateTelegram(ReceiverState.NO_SENDERS);
				}
			}
		}

		updateSenderReceiverStatus();
		if(toRemove == _subscriptionList.getSource()) setSource(null);
		refreshSubscriptions(toRemove);

		// updateReceiverStatus am Ende aufrufen, damit die Quelle dann schon entfernt ist und potentiell bei anderen Datenverteilern
		// nach Quellen gesucht wird
		updateSenderReceiverStatus();
	}

	/**
	 * Aktualisiert den Anmeldestatus von allen Anmeldungen wenn sich eine empfangende Anmeldung abmeldet
	 *
	 * @param toRemove Abmeldender Empfänger/Senke
	 */
	private void refreshSubscriptionsOnReceiverRemoval(final ReceivingSubscription toRemove) {
		long centralDistributorId = getCentralDistributorId();
		List<ReceivingSubscription> validReceiverSubscriptions = getValidReceiverSubscriptions();
		if(validReceiverSubscriptions.size() == 0) {
			for(final SendingSubscription sendingSubscription : getValidSenderSubscriptions()) {
				sendingSubscription.setState(SenderState.NO_RECEIVERS, centralDistributorId);
			}
		}

		updateSenderReceiverStatus();
		if(toRemove == _subscriptionList.getDrain()) setDrain(null);
		refreshSubscriptions(toRemove);

		// updateReceiverStatus am Ende aufrufen, damit die Senke dann schon entfernt ist und potentiell bei anderen Datenverteilern
		// nach Senken gesucht wird
		updateSenderReceiverStatus();
	}

	/**
	 * Aktualisiert den Anmeldestatus von bisherigen Anmeldungen. Wenn z.B. eine Senke abgemeldet wird, wird hier versucht, eventuelle
	 * andere Senken oder Quellen zu "aktivieren" (auf gültig zu setzen)
	 *
	 * @param toIgnore Anmeldung die gerade abgemeldet wird und folglich eben nicht aktiviert werden soll
	 */
	private void refreshSubscriptions(final Subscription toIgnore) {
		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription != toIgnore && sendingSubscription.getState() == SenderState.INVALID_SUBSCRIPTION) {
				refreshSubscriptionsOnNewSender(sendingSubscription);
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription != toIgnore && receivingSubscription.getState() == ReceiverState.INVALID_SUBSCRIPTION) {
				refreshSubscriptionsOnNewReceiver(receivingSubscription);
			}
		}
	}

	/**
	 * Prüft ob mehrere Remote-Zentraldatenverteiler eine positive Rückmeldung auf eine Datenanmeldung gesendet haben. Falls ja, entsteht
	 * ein ungültiger Zustand, welcher durch {@link #_multiRemoteLockActive} dargestellt wird.
	 */
	private void updateMultiRemoteConnectionsLock() {
		setMultiRemoteLockActive(getMultipleRemoteConnectionsSubscribed());
	}

	/**
	 * Prüft ob mehrere Remote-Zentraldatenverteiler eine positive Rückmeldung auf eine Datenanmeldung gesendet haben.
	 *
	 * @return true fall es mehrere Positive Rückmeldungen gibt.
	 */
	private boolean getMultipleRemoteConnectionsSubscribed() {
		int numRemoteSubscriptions = 0;
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteCentralSubscription) {
				RemoteCentralSubscription subscription = (RemoteCentralSubscription) sendingSubscription;
				if(!subscription.isAllowed()) {
					// Status von verbotenen Anmeldungen ignorieren
					continue;
				}
				ConnectionState connectionState = subscription.getConnectionState();
				if(connectionState == ConnectionState.TO_REMOTE_MULTIPLE) return true;
				if(connectionState.isValid()) {
					numRemoteSubscriptions++;
				}
			}
		}
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteCentralSubscription) {
				RemoteCentralSubscription subscription = (RemoteCentralSubscription) receivingSubscription;
				if(!subscription.isAllowed()) {
					// Status von verbotenen Anmeldungen ignorieren
					continue;
				}
				ConnectionState connectionState = subscription.getConnectionState();
				if(connectionState == ConnectionState.TO_REMOTE_MULTIPLE) return true;
				if(connectionState.isValid()) {
					numRemoteSubscriptions++;
				}
			}
		}
		return numRemoteSubscriptions > 1;
	}

	/**
	 * Prüft, ob Anmeldungen zu anderen Zentraldatenverteilern versendet werden sollen und führt diese Anmeldungen durch. Das ist der Fall,
	 * wenn es lokale Sender oder Empfänger-Anmeldungen gibt, aber der aktuelle Datenverteiler nicht der Zentraldatenverteiler ist.
	 */
	private void updateRemoteConnectionsNecessary() {
		setConnectToRemoteCentralDistributor(needsToConnectToRemoteCentralDav());
		removeNegativeRemoteSubscriptions();
	}

	/**
	 * Gibt zurück, ob versucht werden soll, sich an einem anderen ZentralDatenverteiler anzumelden. <p> Das ist der Fall, falls es sich
	 * lokal um keinen ZentralDatenverteiler handelt, also keine lokale Quelle oder Senke angemeldet ist, und es aber gültige Sender oder
	 * Empfänger-Anmeldungen gibt. </p>
	 *
	 * @return Ob versucht werden soll, sich an einem entfernten Zentraldatenverteiler anzumelden, bzw. ob solche Verbindung aufrecht
	 * erhalten werden
	 */
	private boolean needsToConnectToRemoteCentralDav() {
		if(isCentralDistributor()) return false;
		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(!sendingSubscription.isSource()) {
				SenderState state = sendingSubscription.getState();
				if(state.isValidSender() || state == SenderState.MULTIPLE_REMOTE_LOCK) {
					return true;
				}
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(!receivingSubscription.isDrain()) {
				ReceiverState state = receivingSubscription.getState();
				if(state.isValidReceiver() || state == ReceiverState.MULTIPLE_REMOTE_LOCK) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Setzt, ob Anmeldungen bei entfernten Datenverteilern durhgeführt werden sollen und führt die An- bzw. Abmeldungen durch.
	 *
	 * @param newValue Soll zu anderen Zentraldatenverteilern verbunden werden?
	 */
	private void setConnectToRemoteCentralDistributor(final boolean newValue) {
		if(_remoteUpdateLockActive) return;
		_remoteUpdateLockActive = true;
		try {
			if(_connectToRemoteCentralDistributor == newValue) return;
			_connectToRemoteCentralDistributor = newValue;
			if(newValue) {
				// Sich bei entfernten Zentraldatenverteilern anmelden (falls vorhanden)
				createRemoteCentralDistributorSubscriptions();
			}
			else {
				// Sich bei entfernten Zentraldatenverteilern abmelden (falls vorhanden)
				removeRemoteSubscriptions();
			}
		}
		finally {
			_remoteUpdateLockActive = false;
		}
	}

	/**
	 * Führt Anmeldungen bei anderen Datenverteilern durch
	 */
	private void createRemoteCentralDistributorSubscriptions() {
		if(hasSource() || hasDrain()) return;

		// Potentielle Zentraldatenverteiler ermitteln
		final List<Long> distributors = _subscriptionsManager.getPotentialCentralDistributors(_baseSubscriptionInfo);
		if(distributors == null || distributors.size() == 0) return;

		List<ReceivingSubscription> validReceiverSubscriptions = getValidReceiverSubscriptions();
		List<SendingSubscription> validSenderSubscriptions = getValidSenderSubscriptions();

		// Zu berücksichtigende Datenverteiler
		final Set<Long> distributorsToUse = new HashSet<Long>();

		for(SendingSubscription sendingSubscription : validSenderSubscriptions) {
			if(sendingSubscription instanceof RemoteSubscription) {
				// Datenverteiler berücksichtigen, die bei eingehenden Anmeldungen angegeben wurden
				RemoteSubscription remoteSubscription = (RemoteSubscription) sendingSubscription;
				Set<Long> transmitterList = remoteSubscription.getPotentialDistributors();
				for(long l : transmitterList) {
					distributorsToUse.add(l);
				}
			}

			else {
				// Für lokale Anmeldungen alle möglichen Datenverteiler berücksichtigen
				distributorsToUse.addAll(distributors);
			}
		}
		for(ReceivingSubscription receivingSubscription : validReceiverSubscriptions) {
			if(receivingSubscription instanceof RemoteSubscription) {
				// Datenverteiler berücksichtigen, die bei eingehenden Anmeldungen angegeben wurden
				RemoteSubscription remoteSubscription = (RemoteSubscription) receivingSubscription;
				Set<Long> transmitterList = remoteSubscription.getPotentialDistributors();
				for(long l : transmitterList) {
					distributorsToUse.add(l);
				}
			}
			else {
				// Für lokale Anmeldungen alle möglichen Datenverteiler berücksichtigen
				distributorsToUse.addAll(distributors);
			}
		}

		if(validReceiverSubscriptions.size() > 0) {
			// wenn Empfänger vorhanden, nach Quellen suchen
			_subscriptionsManager.connectToRemoteSources(this, distributorsToUse);
		}
		if(validSenderSubscriptions.size() > 0) {
			// wenn Sender vorhanden, nach Senken suchen
			_subscriptionsManager.connectToRemoteDrains(this, distributorsToUse);
		}
	}

	/**
	 * Entfernt alle Anmeldungen bei entfernten Zentraldatenverteilern
	 */
	private void removeRemoteSubscriptions() {
		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteCentralSubscription) {
				final RemoteSubscription subscription = (RemoteSubscription) sendingSubscription;
				subscription.unsubscribe();
				removeSendingSubscription(sendingSubscription);
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteCentralSubscription) {
				final RemoteSubscription subscription = (RemoteSubscription) receivingSubscription;
				subscription.unsubscribe();
				removeReceivingSubscription(receivingSubscription);
			}
		}
	}

	/**
	 * Meldet überflüssige Anmeldungen bei Remote-Datenverteilern ab. Anmeldungen sind überflüssig, wenn es genau eine andere Anmeldung mit
	 * positiver Rückmeldung gibt und der Datenverteiler dieser Anmeldung signalisiert hat, dass er nicht zuständig ist.
	 */
	private void removeNegativeRemoteSubscriptions() {
		if(!_connectToRemoteCentralDistributor) return;

		// Nur negative Anmeldungen entfernen wenn es genau eine positive Anmeldung gibt. Dazu positive und negative Anmeldungen zählen
		int numPositiveResponses = 0;
		int numNegativeResponses = 0;
		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteCentralSubscription) {
				if(sendingSubscription.getConnectionState().isValid()
						&& sendingSubscription.isAllowed()) {
					numPositiveResponses++;
				}
				else {
					numNegativeResponses++;
				}
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteCentralSubscription) {
				if(receivingSubscription.getConnectionState().isValid()
						&& receivingSubscription.isAllowed()) {
					numPositiveResponses++;
				}
				else {
					numNegativeResponses++;
				}
			}
		}

		// Falls es genau eine positive Anmeldung und mehr als eine negative Anmeldung gibt, die negativen Anmeldungen entfernen
		if(numNegativeResponses == 0 || numPositiveResponses != 1) return;

		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteCentralSubscription) {
				if(sendingSubscription.getConnectionState() == ConnectionState.TO_REMOTE_NOT_RESPONSIBLE) {
					final RemoteSubscription subscription = (RemoteSubscription) sendingSubscription;
					subscription.unsubscribe();
					removeSendingSubscription(sendingSubscription);
				}
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteCentralSubscription) {
				if(receivingSubscription.getConnectionState() == ConnectionState.TO_REMOTE_NOT_RESPONSIBLE) {
					final RemoteSubscription subscription = (RemoteSubscription) receivingSubscription;
					subscription.unsubscribe();
					removeReceivingSubscription(receivingSubscription);
				}
			}
		}
	}

	/**
	 * Entfernt eine empfangende Anmeldung
	 *
	 * @param receivingSubscription empfangende Anmeldung
	 */
	public synchronized void removeReceivingSubscription(final ReceivingSubscription receivingSubscription) {
		receivingSubscription.setState(ReceiverState.UNKNOWN, getCentralDistributorId());
		refreshSubscriptionsOnReceiverRemoval(receivingSubscription);
		_subscriptionList.removeReceiver(receivingSubscription);
		receivingSubscription.unsubscribe();
	}

	/**
	 * Entfernt eine sendende Anmeldung
	 *
	 * @param sendingSubscription sendende Anmeldung
	 */
	public synchronized void removeSendingSubscription(final SendingSubscription sendingSubscription) {
		sendingSubscription.setState(SenderState.UNKNOWN, getCentralDistributorId());
		refreshSubscriptionsOnSenderRemoval(sendingSubscription);
		_subscriptionList.removeSender(sendingSubscription);
		sendingSubscription.unsubscribe();
	}

	/**
	 * Entfernt alle sendende Anmedungen, die über die angegebene Verbindung angemeldet sind
	 *
	 * @param communication Verbindung
	 * @return Liste mit entfernten Sendern und Quellen
	 */
	public synchronized List<SendingSubscription> removeSendingSubscriptions(final CommunicationInterface communication) {
		final List<SendingSubscription> result = new ArrayList<SendingSubscription>();
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription.getCommunication() == communication) {
				removeSendingSubscription(sendingSubscription);
				result.add(sendingSubscription);
			}
		}
		return result;
	}

	/**
	 * Entfernt alle empfangende Anmedungen, die über die angegebene Verbindung angemeldet sind
	 *
	 * @param communication Verbindung
	 * @return Liste mit entfernten Empfängern und Senken
	 */
	public synchronized List<ReceivingSubscription> removeReceivingSubscriptions(final CommunicationInterface communication) {
		final List<ReceivingSubscription> result = new ArrayList<ReceivingSubscription>();
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription.getCommunication() == communication) {
				removeReceivingSubscription(receivingSubscription);
				result.add(receivingSubscription);
			}
		}
		return result;
	}

	/**
	 * Gibt alle gültigen sendenden Anmeldungen zurück
	 *
	 * @return alle gültigen sendenden Anmeldungen (Quellen und Sender)
	 */
	public synchronized List<SendingSubscription> getValidSenderSubscriptions() {
		Collection<SendingSubscription> sendingSubscriptions = _subscriptionList.getSendingSubscriptions();
		final ArrayList<SendingSubscription> list = new ArrayList<SendingSubscription>(sendingSubscriptions.size());
		for(final SendingSubscription sendingSubscription : sendingSubscriptions) {
			if(sendingSubscription.getState().isValidSender()) list.add(sendingSubscription);
		}
		return list;
	}

	/**
	 * Gibt alle gültigen empfangenden Anmeldungen zurück
	 *
	 * @return alle gültigen empfangenden Anmeldungen (Senken und Empfänger)
	 */
	public synchronized List<ReceivingSubscription> getValidReceiverSubscriptions() {
		Collection<ReceivingSubscription> receivingSubscriptions = _subscriptionList.getReceivingSubscriptions();
		final ArrayList<ReceivingSubscription> list = new ArrayList<ReceivingSubscription>(receivingSubscriptions.size());
		for(final ReceivingSubscription receivingSubscription : receivingSubscriptions) {
			if(receivingSubscription.getState().isValidReceiver()) list.add(receivingSubscription);
		}
		return list;
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn es keine Anmeldungen gibt
	 *
	 * @return <tt>true</tt>, wenn es keine Anmeldungen gibt, sonst <tt>false</tt>
	 */
	public synchronized boolean isEmpty() {
		return _subscriptionList.isEmpty();
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn dieser Datenverteiler Zentraldatenverteiler für diese Anmeldung ist
	 *
	 * @return <tt>true</tt>, wenn dieser Datenverteiler Zentraldatenverteiler für diese Anmeldung ist, sonst <tt>false</tt>
	 */
	public synchronized boolean isCentralDistributor() {
		return _subscriptionList.isCentralDistributor();
	}

	/**
	 * Berechnet den nächsten Datenindex und gibt diesen zurück
	 *
	 * @param runningNumber Laufende Nummer, wird vom SubscriptionsManager bereitgestellt, da diese Objekte gelöscht werden sobas keine
	 *                      Anmeldungen mehr vorhanden sind
	 * @return nächsten Datenindex, "0" falls dieser Datenverteiler nicht der Zentraldatenverteiler ist.
	 */
	public synchronized long getNextDataIndex(final long runningNumber) {
		return _subscriptionList.getDataIndex(runningNumber);
	}

	/**
	 * Gibt den zuletzt berechneten Datenindex zurück
	 *
	 * @param runningNumber Laufende Nummer, wird vom SubscriptionsManager bereitgestellt, da diese Objekte gelöscht werden sobas keine
	 *                      Anmeldungen mehr vorhanden sind
	 * @return zuletzt berechneten Datenindex, "0" falls dieser Datenverteiler nicht der Zentraldatenverteiler ist.
	 */
	public synchronized long getCurrentDataIndex(final long runningNumber) {
		return _subscriptionList.getDataIndex(runningNumber);
	}

	/**
	 * Verschickt ein einzelnes Datentelegramm an alle interessierten und korrekt angemeldeten Empfänger
	 *
	 * @param applicationDataTelegram Datentelegramm
	 * @param toCentralDistributor    wenn das Telegramm noch nicht beim Zentraldatenverteiler behandelt wurde, also der Datenindex noch
	 *                                nicht vernünftig gesetzt wurde
	 * @param communication           Verbindung über die der Emfang erfolgt ist (zur Rechteprüfung), bei null findet keine Prüfung statt
	 */
	public void distributeTelegram(final ApplicationDataTelegram applicationDataTelegram, final boolean toCentralDistributor, final CommunicationInterface communication) {
		distributeTelegrams(Collections.singletonList(applicationDataTelegram), toCentralDistributor, communication);
	}

	/**
	 * Verschickt eine Liste von zusammengehörigen Datentelegrammen an alle interessierten und korrekt angemeldeten Empfänger
	 *
	 * @param applicationDataTelegrams Datentelegramme
	 * @param toCentralDistributor     wenn das Telegramm noch nicht beim Zentraldatenverteiler behandelt wurde, also der Datenindex noch
	 *                                 nicht vernünftig gesetzt wurde
	 * @param communication            Verbindung über die der Emfang erfolgt ist (zur Rechteprüfung), bei null findet keine Prüfung statt
	 */
	public synchronized void distributeTelegrams(final List<ApplicationDataTelegram> applicationDataTelegrams, final boolean toCentralDistributor, final CommunicationInterface communication) {
		final List<ReceivingSubscription> receivingSubscriptions = getValidReceiverSubscriptions();

		long dataIndex = applicationDataTelegrams.get(0).getDataNumber();

		if(!toCentralDistributor && _lastSendDataIndex > 1 && dataIndex <= _lastSendDataIndex) {
			// Kein monoton steigender Datenindex
			return;
		}

		// Die Anmeldung, die diese Daten sendet herausfinden
		SendingSubscription sendingSubscription = null;

		if(communication != null) {
			// Wenn es sich um keinen künstlichen Transaktionsdatensatz handelt, absender prüfen
			for(SendingSubscription tmp : _subscriptionList.getSendingSubscriptions()) {
				if(tmp.getCommunication() == communication) {
					sendingSubscription = tmp;
				}
			}

			if(sendingSubscription == null) {
				_debug.warning("Empfange Daten ohne bekannten Absender", applicationDataTelegrams.get(0).getBaseSubscriptionInfo());
				// Kein Absender ermittelbar
				return;
			}


			if(!sendingSubscription.getState().isValidSender()) {
				// Absender hat keine Rechte zum senden
				return;
			}
		}

		for(final ReceivingSubscription receivingSubscription : receivingSubscriptions) {
			if(!receivingSubscription.getReceiveOptions().withDelayed() && applicationDataTelegrams.get(0).getDelayedDataFlag()) {
				// Datensatz ist als nachgeliefert markiert, der Empfänger will aber nur aktuelle Daten
				continue;
			}
			if(receivingSubscription.getReceiveOptions().withDelta() && telegramsAreEqual(applicationDataTelegrams, _lastSendTelegrams)) {
				// Datensatz ist unverändert, der Empfänger will aber nur geänderte Daten
				continue;
			}
			if(toCentralDistributor && !(receivingSubscription instanceof RemoteDrainSubscription)) {
				// Datensätze, deren Datenindex noch nicht gesetzt wurde,
				// dürfen nur an andere Zentraldatenverteiler gesendet werden, die die Senke sind.
				// hierdurch wird z.B. verhindert, dass lokale Sender mit lokalen Empfängern kommunizieren
				// ohne dass es eine lokale Quelle gibt
				continue;
			}
			for(final ApplicationDataTelegram telegram : applicationDataTelegrams) {
				receivingSubscription.sendDataTelegram(telegram);
			}
		}

		if(hasSource() && !applicationDataTelegrams.get(0).getDelayedDataFlag()) {
			if(dataIndex != 1) _lastSendDataIndex = dataIndex;
			_lastSendTelegrams = new ArrayList<ApplicationDataTelegram>(applicationDataTelegrams);
		}
	}

	public synchronized void updatePendingSubscriptionDataIndex(final CommunicationInterface communication, final long dataIndex) {
		for(Map.Entry<Long, PendingSubscription> entry : _pendingSubscriptions.entrySet()) {
			PendingSubscription pendingSubscription = entry.getValue();
			if(pendingSubscription.getNewSubscription().getCommunication() == communication) {
				pendingSubscription.setLastReceivedDataIndex(dataIndex);
				handlePendingSubscriptions(
						entry.getKey(),
						(TransmitterCommunicationInterface) communication,
						pendingSubscription.getNewSubscription().getConnectionState()
				);
			}
		}
	}

	/**
	 * Aktualisert die Rechte von Anmeldungen und macht diese dadurch gültig/ungültig
	 *
	 * @param userId geänderter Benutzer, dessen Anmeldungen zu aktualisieren sind
	 */
	public synchronized void handleUserRightsChanged(final long userId) {
		for(final ReceivingSubscription subscription : _subscriptionList.getReceivingSubscriptions()) {
			if(subscription.getUserId() != userId) continue;
			final boolean isAllowed = subscription.isAllowed();
			if(isAllowed && subscription.getState() == ReceiverState.NOT_ALLOWED) {
				// Anmeldung wird gültig, Anmeldung "hinzufügen" und Sender informieren
				refreshSubscriptionsOnNewReceiver(subscription);
			}
			else if(!isAllowed && subscription.getState() != ReceiverState.NOT_ALLOWED) {
				// Anmeldung wird ungültig
				subscription.setState(ReceiverState.NOT_ALLOWED, getCentralDistributorId());
				subscription.sendStateTelegram(ReceiverState.NOT_ALLOWED);
				refreshSubscriptionsOnReceiverRemoval(subscription);
				if(subscription instanceof RemoteCentralSubscription) {
					// Anmeldungen bei anderen Zentraldatenverteilern ohne Rechte werden sofort abgemeldet
					removeReceivingSubscription(subscription);
				}
			}
		}

		for(final SendingSubscription subscription : _subscriptionList.getSendingSubscriptions()) {
			if(subscription.getUserId() != userId) continue;
			final boolean isAllowed = subscription.isAllowed();
			if(isAllowed && subscription.getState() == SenderState.NOT_ALLOWED) {
				// Anmeldung wird gültig, Anmeldung "hinzufügen" und Empfänger informieren
				refreshSubscriptionsOnNewSender(subscription);
			}
			else if(!isAllowed && subscription.getState() != SenderState.NOT_ALLOWED) {
				// Anmeldung wird ungültig
				subscription.setState(SenderState.NOT_ALLOWED, getCentralDistributorId());
				refreshSubscriptionsOnSenderRemoval(subscription);
				if(subscription instanceof RemoteCentralSubscription) {
					// Anmeldungen bei anderen Zentraldatenverteilern ohne Rechte werden sofort abgemeldet
					removeSendingSubscription(subscription);
				}
			}
		}

		// Die Sperre bei mehreren Remote-Zentraldatenverteilern ist von den Rechten der Anmeldungen ahängig
		// (verbotene Anmeldungen werden ignoriert), also den Status ggf. aktualisieren
		updateMultiRemoteConnectionsLock();

		// Eventuell sind jetzt neue Anmeldungen bei Remote-Datenverteilern möglich.
		// Solche Anmeldungen werden nur durchgeführt wenn entprechende Rechte vorhanden sind.
		// Da sich die Rechte geändert haben können sich daher neue Anmeldungen ergeben haben
		createRemoteCentralDistributorSubscriptions();
	}

	/**
	 * Verarbeitet eine Anmeldungsquittung von einem anderen Datenverteiler, aktualisert den Status der entsprechenden ausgehenden
	 * Anmeldung
	 *
	 * @param communication     Kommunikation
	 * @param state             neuer Status
	 * @param mainTransmitterId Id des Zentraldatenverteilers
	 */
	public synchronized void setRemoteSourceSubscriptionStatus(
			final TransmitterCommunicationInterface communication, final ConnectionState state, final long mainTransmitterId) {
		handlePendingSubscriptions(mainTransmitterId, communication, state);
		RemoteSourceSubscription remoteSubscription = null;
		for(final SendingSubscription subscription : _subscriptionList.getSendingSubscriptions()) {
			if(subscription instanceof RemoteSourceSubscription) {
				final RemoteSourceSubscription remoteSourceSubscription = (RemoteSourceSubscription) subscription;
				if(remoteSourceSubscription.getCommunication() == communication) {
					remoteSubscription = remoteSourceSubscription;
					break;
				}
			}
		}
		if(remoteSubscription == null) {
			return;
		}
		remoteSubscription.setRemoteState(mainTransmitterId, state);
		updateMultiRemoteConnectionsLock();
		if(remoteSubscription.getConnectionState().isValid() && !remoteSubscription.getState().isValidSender()) {
			// Anmeldung wird gültig
			refreshSubscriptionsOnNewSender(remoteSubscription);
		}
		else if(!remoteSubscription.getConnectionState().isValid() && remoteSubscription.getState().isValidSender()) {
			// Anmeldung wird ungültig
			remoteSubscription.setState(SenderState.NO_REMOTE_SOURCE, getCentralDistributorId());
			refreshSubscriptionsOnSenderRemoval(remoteSubscription);
		}
		updateSenderReceiverStatus();
		removeNegativeRemoteSubscriptions();
	}

	/**
	 * Verarbeitet eine Anmeldungsquittung von einem anderen Datenverteiler, aktualisert den Status der entsprechenden ausgehenden
	 * Anmeldung
	 *
	 * @param communication     Kommunikation
	 * @param state             neuer Status
	 * @param mainTransmitterId Id des Zentraldatenverteilers
	 */
	public synchronized void setRemoteDrainSubscriptionStatus(
			final TransmitterCommunicationInterface communication, final ConnectionState state, final long mainTransmitterId) {
		handlePendingSubscriptions(mainTransmitterId, communication, state);
		RemoteDrainSubscription remoteSubscription = null;
		for(final ReceivingSubscription subscription : _subscriptionList.getReceivingSubscriptions()) {
			if(subscription instanceof RemoteDrainSubscription) {
				final RemoteDrainSubscription remoteDrainSubscription = (RemoteDrainSubscription) subscription;
				if(remoteDrainSubscription.getCommunication() == communication) {
					remoteSubscription = remoteDrainSubscription;
					break;
				}
			}
		}
		if(remoteSubscription == null) {
			return;
		}
		remoteSubscription.setRemoteState(mainTransmitterId, state);
		updateMultiRemoteConnectionsLock();
		if(remoteSubscription.getConnectionState().isValid() && !remoteSubscription.getState().isValidReceiver()) {
			// Anmeldung wird gültig
			refreshSubscriptionsOnNewReceiver(remoteSubscription);
		}
		else if(!remoteSubscription.getConnectionState().isValid() && remoteSubscription.getState().isValidReceiver()) {
			// Anmeldung wird ungültig
			remoteSubscription.setState(ReceiverState.NO_REMOTE_DRAIN, getCentralDistributorId());
			refreshSubscriptionsOnReceiverRemoval(remoteSubscription);
		}
		updateSenderReceiverStatus();
		removeNegativeRemoteSubscriptions();
	}

	/**
	 * Aktualisiert Anmeldeumleitungen, ersetzt die alte Anmeldung falls Umleitung erfolgreich oder entfernt die neue Verbindung falls nicht
	 * erfolgreich.
	 *
	 * @param mainTransmitterId                 Zentraldatenverteiler-Id
	 * @param transmitterCommunicationInterface Kommunikation der neuen Anmeldung
	 * @param state                             neuer Status der Anmeldung
	 */
	private void handlePendingSubscriptions(
			final long mainTransmitterId, final TransmitterCommunicationInterface transmitterCommunicationInterface, final ConnectionState state) {

		// Gibt es Umleitungen?
		PendingSubscription pendingSubscriptionInfo = _pendingSubscriptions.get(mainTransmitterId);
		if(pendingSubscriptionInfo == null) return;
		RemoteCentralSubscription pendingSubscription = pendingSubscriptionInfo.getNewSubscription();

		if(pendingSubscription.getCommunication() == transmitterCommunicationInterface) {

			// Neuen Status setzen
			pendingSubscription.setRemoteState(mainTransmitterId, state);

			if(pendingSubscription.getConnectionState().isValid()) {

				if(pendingSubscription instanceof SendingSubscription
						&& pendingSubscriptionInfo.getLastReceivedDataIndex() != _lastSendDataIndex
						&& pendingSubscriptionInfo.getLastReceivedDataIndex() - 1 != _lastSendDataIndex
						&& _lastSendDataIndex != 1) {
					// Datenindex noch nicht synchron, erstmal nichts tun
					return;
				}

				// Neue Anmeldung erfolgreich, alte Anmeldung(en) abmelden.
				if(pendingSubscription instanceof RemoteDrainSubscription) {
					RemoteDrainSubscription oldSubscription = null;
					for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
						if(receivingSubscription instanceof RemoteDrainSubscription) {
							RemoteDrainSubscription other = (RemoteDrainSubscription) receivingSubscription;
							if(other.getCommunication() != pendingSubscription.getCommunication() && other.getCentralDistributorId() == mainTransmitterId) {
								oldSubscription = other;
								break;
							}
						}
					}
					replaceReceiver(oldSubscription, (RemoteDrainSubscription) pendingSubscription);
				}
				else if(pendingSubscription instanceof RemoteSourceSubscription) {
					RemoteSourceSubscription oldSubscription = null;
					for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
						if(sendingSubscription instanceof RemoteSourceSubscription) {
							RemoteSourceSubscription other = (RemoteSourceSubscription) sendingSubscription;
							if(other.getCommunication() != pendingSubscription.getCommunication() && other.getCentralDistributorId() == mainTransmitterId) {
								oldSubscription = other;
								break;
							}
						}
					}
					replaceSender(oldSubscription, (RemoteSourceSubscription) pendingSubscription);
				}
			}
			else {
				// Umleitung nicht erfolgreich, wieder abmelden
				pendingSubscription.unsubscribe();
			}

			// Umleitungseintrag entfernen
			_pendingSubscriptions.remove(mainTransmitterId);
		}
	}

	/**
	 * Ersetzt eine Anmeldung wegen einer Anmeldeumleitung
	 *
	 * @param oldSubscription alte Anmeldung
	 * @param newSubscription neue Anmeldung
	 */
	private void replaceReceiver(final RemoteDrainSubscription oldSubscription, final RemoteDrainSubscription newSubscription) {
		_subscriptionList.addReceiver(newSubscription);
		if(oldSubscription != null) {
			if(_subscriptionList.getDrain() == oldSubscription) {
				_subscriptionList.setDrain(newSubscription);
			}
			_subscriptionList.removeReceiver(oldSubscription);
			newSubscription.setState(oldSubscription.getState(), oldSubscription.getCentralDistributorId());
			oldSubscription.unsubscribe();
		}
	}

	/**
	 * Ersetzt eine Anmeldung wegen einer Anmeldeumleitung
	 *
	 * @param oldSubscription alte Anmeldung
	 * @param newSubscription neue Anmeldung
	 */
	private void replaceSender(final RemoteSourceSubscription oldSubscription, final RemoteSourceSubscription newSubscription) {
		_subscriptionList.addSender(newSubscription);
		if(oldSubscription != null) {
			if(_subscriptionList.getSource() == oldSubscription) {
				_subscriptionList.setSource(newSubscription);
			}
			_subscriptionList.removeSender(oldSubscription);
			newSubscription.setState(oldSubscription.getState(), oldSubscription.getCentralDistributorId());
			oldSubscription.unsubscribe();
		}
	}

	/**
	 * Gibt die Zentraldatenverteiler-ID zurück
	 *
	 * @return die Zentraldatenverteiler-ID, sofern bekannt. Sonst -1
	 */
	private long getCentralDistributorId() {
		return _subscriptionList.getCentralDistributorId();
	}

	/**
	 * Setzt eine neue Senke
	 *
	 * @param drain neue Senke
	 */
	private void setDrain(final ReceivingSubscription drain) {
		ReceivingSubscription oldDrain = _subscriptionList.getDrain();
		if(oldDrain == drain) return;
		_lastSendTelegrams = null;
		_lastSendDataIndex = 1;
		if(!isLocalSubscription(oldDrain) && isLocalSubscription(drain)) {
			_subscriptionsManager.notifyIsNewCentralDistributor(_baseSubscriptionInfo);
		}
		if(isLocalSubscription(oldDrain) && !isLocalSubscription(drain)) {
			_subscriptionsManager.notifyWasCentralDistributor(_baseSubscriptionInfo);
		}
		_subscriptionList.setDrain(drain);
	}

	/**
	 * setzt eine neue Quelle
	 *
	 * @param source neue Quelle
	 */
	private void setSource(final SendingSubscription source) {
		SendingSubscription oldSource = _subscriptionList.getSource();
		if(oldSource == source) return;
		_lastSendTelegrams = null;
		_lastSendDataIndex = 1;
		if(!isLocalSubscription(oldSource) && isLocalSubscription(source)) {
			_subscriptionsManager.notifyIsNewCentralDistributor(_baseSubscriptionInfo);
		}
		if(isLocalSubscription(oldSource) && !isLocalSubscription(source)) {
			_subscriptionsManager.notifyWasCentralDistributor(_baseSubscriptionInfo);
		}
		_subscriptionList.setSource(source);
	}

	/**
	 * Gibt das BaseSubscriptionInfo zurück
	 *
	 * @return das BaseSubscriptionInfo
	 */
	public BaseSubscriptionInfo getBaseSubscriptionInfo() {
		return _baseSubscriptionInfo;
	}

	/**
	 * Wird aufgerufen, wenn im ListsManager ein Update stattfand und so eventuell neue oder bessere Wege für die Remote-Anmeldungen
	 * existieren
	 */
	public synchronized void updateRemoteConnections() {
		// Falls kein Bedarf an entfernten Anmeldungen besteht, nichts tun
		if(!_connectToRemoteCentralDistributor) return;

		
		for(final SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteCentralSubscription) {
				final RemoteCentralSubscription remoteCentralSubscription = (RemoteCentralSubscription) sendingSubscription;
				long centralDistributorId = remoteCentralSubscription.getCentralDistributorId();
				updateBestWay(centralDistributorId, remoteCentralSubscription.getCommunication(), _subscriptionsManager.getBestConnectionToRemoteDav(centralDistributorId));
			}
		}
		for(final ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteCentralSubscription) {
				final RemoteCentralSubscription remoteCentralSubscription = (RemoteCentralSubscription) receivingSubscription;
				long centralDistributorId = remoteCentralSubscription.getCentralDistributorId();
				updateBestWay(
						centralDistributorId,
						remoteCentralSubscription.getCommunication(),
						_subscriptionsManager.getBestConnectionToRemoteDav(centralDistributorId)
				);
			}
		}

		// Hier eventuelle neue Anmeldungen durchführen
		createRemoteCentralDistributorSubscriptions();
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn eine Quelle verbunden ist (entweder lokal oder über eine Transmitterverbindung)
	 *
	 * @return <tt>true</tt>, wenn eine Quelle verbunden ist, sonst <tt>false</tt>
	 */
	public synchronized boolean hasSource() {
		return _subscriptionList.hasSource();
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn eine Senke verbunden ist (entweder lokal oder über eine Transmitterverbindung)
	 *
	 * @return <tt>true</tt>, wenn eine Senke verbunden ist, sonst <tt>false</tt>
	 */
	public synchronized boolean hasDrain() {
		return _subscriptionList.hasDrain();
	}

	/**
	 * Prüft, ob die angegebene Kommunikationsklasse senden darf (also als gültiger Sender angemeldet ist)
	 *
	 * @param communication Kommunikation
	 * @return true wenn gültig
	 */
	public synchronized boolean isValidSender(final CommunicationInterface communication) {
		// Normale Sendeanmeldungen prüfen
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription.getCommunication() == communication) {
				return sendingSubscription.getState().isValidSender();
			}
		}
		return false;
	}

	/**
	 * Setzt, ob Anmeldung ungültig gemacht werden sollen, weil mehrere remote-Zzentraldatenverteiler positive Rückmeldungen verschickt
	 * haben
	 *
	 * @param multiRemoteLockActive ob die Sperre {@link #_multiRemoteLockActive} aktiv sein soll.
	 */
	public void setMultiRemoteLockActive(final boolean multiRemoteLockActive) {
		if(multiRemoteLockActive == _multiRemoteLockActive) return;
		_multiRemoteLockActive = multiRemoteLockActive;
		if(multiRemoteLockActive) {
			// Wenn aktiv alle Anmeldungen ungültig machen und entsprechend markieren
			setDrain(null);
			setSource(null);
			for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
				if(sendingSubscription.getState() == SenderState.NO_REMOTE_SOURCE) continue;
				if(sendingSubscription instanceof RemoteCentralSubscription) {
					sendingSubscription.setState(SenderState.MULTIPLE_REMOTE_LOCK, -1);
				}
				else {
					sendingSubscription.setState(SenderState.MULTIPLE_REMOTE_LOCK, -1);
				}
			}
			for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
				if(receivingSubscription.getState() == ReceiverState.NO_REMOTE_DRAIN) continue;
				if(receivingSubscription instanceof RemoteCentralSubscription) {
					receivingSubscription.setState(ReceiverState.MULTIPLE_REMOTE_LOCK, -1);
				}
				else {
					receivingSubscription.setState(ReceiverState.MULTIPLE_REMOTE_LOCK, -1);
					receivingSubscription.sendStateTelegram(ReceiverState.INVALID_SUBSCRIPTION);
				}
			}
		}
		else {
			// Wenn wieder inaktiv alle lokalen Anmeldungen neu gültig machen

			// Von bisherigen Zentraldatenverteilern abmelden um einen konsitenten Zustand und neue initiale Telegramme zu erhalten
			setConnectToRemoteCentralDistributor(false);

			// Alle lokalen Anmeldungen neu initialisieren
			for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
				if(sendingSubscription.getState() == SenderState.NO_REMOTE_SOURCE) continue;
				sendingSubscription.setState(SenderState.UNKNOWN, -1);
				refreshSubscriptionsOnNewSender(sendingSubscription);
			}
			for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
				if(receivingSubscription.getState() == ReceiverState.NO_REMOTE_DRAIN) continue;
				receivingSubscription.setState(ReceiverState.UNKNOWN, -1);
				refreshSubscriptionsOnNewReceiver(receivingSubscription);
			}
		}
	}


	/**
	 * Gibt alle Sende-Anmeldungen zu einer Verbindung zurück
	 *
	 * @param communicationInterface Verbindung
	 * @return Alle Quellen und Sender hinter dieser Verbindung (evtl. eine leere Liste falls nicht vorhanden)
	 */
	public List<SendingSubscription> getSendingSubscriptions(final CommunicationInterface communicationInterface) {
		final List<SendingSubscription> result = new ArrayList<SendingSubscription>();
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription.getCommunication() == communicationInterface) result.add(sendingSubscription);
		}
		return result;
	}

	/**
	 * Gibt alle Empfangs-Anmeldungen zu einer Verbindung zurück
	 *
	 * @param communicationInterface Verbindung
	 * @return Alle Senken und Empfänger hinter dieser Verbindung (evtl. eine leere Liste falls nicht vorhanden)
	 */
	public List<ReceivingSubscription> getReceivingSubscriptions(final CommunicationInterface communicationInterface) {
		final List<ReceivingSubscription> result = new ArrayList<ReceivingSubscription>();
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription.getCommunication() == communicationInterface) result.add(receivingSubscription);
		}
		return result;
	}

	/**
	 * Gibt alle sendenden Anmeldungen zurück
	 *
	 * @return alle Sender und Quellen dieser Datenidentifikation
	 */
	public Collection<SendingSubscription> getSendingSubscriptions() {
		return _subscriptionList.getSendingSubscriptions();
	}

	/**
	 * Gibt alle Empfangs-Anmeldungen zurück
	 *
	 * @return alle Senken und Empfänger dieser Datenidentifikation
	 */
	public Collection<ReceivingSubscription> getReceivingSubscriptions() {
		return _subscriptionList.getReceivingSubscriptions();
	}

	/**
	 * Wird von bestWaymanager aufgerufen, wenn es eine neue beste lokale Verbindung zu einem Zentraldatenverteiler gibt
	 *
	 * @param transmitterId Zentraldatenverteiler-ID
	 * @param oldConnection Alte Verbindung
	 * @param newConnection Neue Verbindung
	 */
	public synchronized void updateBestWay(
			final long transmitterId, final TransmitterCommunicationInterface oldConnection, final TransmitterCommunicationInterface newConnection) {
		updateBestWaySource(transmitterId, oldConnection, newConnection);
		updateBestWayDrain(transmitterId, oldConnection, newConnection);
	}

	/**
	 * Sorgt für eine Anmeldungsumleitung bei Remote-Quell-Anmeldungen
	 *
	 * @param transmitterId Zentraldatenverteiler-ID
	 * @param oldConnection Alte Verbidnung
	 * @param newConnection Neue Verbindung
	 */
	private void updateBestWaySource(
			final long transmitterId, final TransmitterCommunicationInterface oldConnection, final TransmitterCommunicationInterface newConnection) {
		if(oldConnection == null || newConnection == null) return;
		if(oldConnection == newConnection) return;
		RemoteSourceSubscription oldSub = null;
		RemoteSourceSubscription newSub = null;
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription instanceof RemoteSourceSubscription) {
				RemoteSourceSubscription subscription = (RemoteSourceSubscription) sendingSubscription;
				if(subscription.getCentralDistributorId() == transmitterId) {
					if(subscription.getCommunication() == oldConnection) {
						oldSub = subscription;
					}
					else if(subscription.getCommunication() == newConnection) {
						newSub = subscription;
					}
				}
			}
		}
		if(oldSub == null) return;
		if(newSub == null) {
			newSub = new RemoteSourceSubscription(_subscriptionsManager, _baseSubscriptionInfo, newConnection);
			addReplacementSubscription(transmitterId, newSub);
			newSub.setState(SenderState.WAITING, -1);
			newSub.setPotentialDistributors(Arrays.asList(transmitterId));
			newSub.subscribe();
		}
		else if(!_subscriptionList.hasDrainOrSource()) {
			// Wenn schon eine neue Verbindung besteht, passiert das gewöhnlich wenn noch kein Zentraldatenverteiler gefunden wurde.
			// Hier einfach die (nicht erfolgreichen) Anmeldungen umbiegen.
			oldSub.removePotentialDistributor(transmitterId);
			if(oldSub.getPotentialDistributors().isEmpty()) {
				removeSendingSubscription(oldSub);
			}
			else {
				oldSub.subscribe();
			}
			newSub.addPotentialDistributor(transmitterId);
			newSub.subscribe();
		}
	}

	/**
	 * Sorgt für eine Anmeldungsumleitung bei Remote-Senken-Anmeldungen
	 *
	 * @param transmitterId Zentraldatenverteiler-ID
	 * @param oldConnection Alte Verbidnung
	 * @param newConnection Neue Verbindung
	 */
	private void updateBestWayDrain(
			final long transmitterId, final TransmitterCommunicationInterface oldConnection, final TransmitterCommunicationInterface newConnection) {
		if(oldConnection == null || newConnection == null) return;
		if(oldConnection == newConnection) return;
		RemoteDrainSubscription oldSub = null;
		RemoteDrainSubscription newSub = null;
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription instanceof RemoteDrainSubscription) {
				RemoteDrainSubscription subscription = (RemoteDrainSubscription) receivingSubscription;
				if(subscription.getCentralDistributorId() == transmitterId) {
					if(subscription.getCommunication() == oldConnection) {
						oldSub = subscription;
					}
					else if(subscription.getCommunication() == newConnection) {
						newSub = subscription;
					}
				}
			}
		}
		if(oldSub == null) return;
		if(newSub == null) {
			newSub = new RemoteDrainSubscription(_subscriptionsManager, _baseSubscriptionInfo, newConnection);
			addReplacementSubscription(transmitterId, newSub);
			newSub.setPotentialDistributors(Arrays.asList(transmitterId));
			newSub.subscribe();
		}
		else if(!_subscriptionList.hasDrainOrSource()) {
			// Wenn schon eine neue Verbindung besteht, passiert das gewöhnlich wenn noch kein Zentraldatenverteiler gefunden wurde.
			// Hier einfach die (nicht erfolgreichen) Anmeldungen umbiegen.
			oldSub.removePotentialDistributor(transmitterId);
			if(oldSub.getPotentialDistributors().isEmpty()) {
				removeReceivingSubscription(oldSub);
			}
			else {
				oldSub.subscribe();
			}
			newSub.addPotentialDistributor(transmitterId);
			newSub.subscribe();
		}
	}

	/**
	 * Erstellt eine (zuerst wartende) Umleitungsanmeldung
	 *
	 * @param transmitterId Zentraldatenverteiler-Id
	 * @param newSub        Anmeldung beim Zentraldatenverteiler
	 */
	private void addReplacementSubscription(final long transmitterId, final RemoteCentralSubscription newSub) {
		PendingSubscription old = _pendingSubscriptions.put(transmitterId, new PendingSubscription(newSub));
		if(old != null) {
			// Alte Umleitung entfernen (wieder abmelden)
			old.getNewSubscription().unsubscribe();
		}
	}

	/**
	 * Serialisiert die Anmelde-Informationen in Bytes um sie über den Datenverteiler zu Testzwecken abrufen zu können.
	 *
	 * @return Byte-Array
	 * @throws IOException
	 */
	public synchronized byte[] serializeToBytes() throws IOException {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
		try {
			Collection<SendingSubscription> sendingSubscriptions = _subscriptionList.getSendingSubscriptions();
			dataOutputStream.writeInt(sendingSubscriptions.size());
			for(final SendingSubscription sendingSubscription : sendingSubscriptions) {
				dataOutputStream.writeBoolean(sendingSubscription instanceof LocalSubscription);
				dataOutputStream.writeLong(sendingSubscription.getCommunication().getId());
				dataOutputStream.writeLong(sendingSubscription.getUserId());
				dataOutputStream.writeBoolean(sendingSubscription.isSource());
				dataOutputStream.writeBoolean(sendingSubscription.isRequestSupported());
				dataOutputStream.writeInt(sendingSubscription.getState().ordinal());
				dataOutputStream.writeInt(sendingSubscription.getConnectionState().ordinal());
			}
			Collection<ReceivingSubscription> receivingSubscriptions = _subscriptionList.getReceivingSubscriptions();
			dataOutputStream.writeInt(receivingSubscriptions.size());
			for(final ReceivingSubscription receivingSubscription : receivingSubscriptions) {
				dataOutputStream.writeBoolean(receivingSubscription instanceof LocalSubscription);
				dataOutputStream.writeLong(receivingSubscription.getCommunication().getId());
				dataOutputStream.writeLong(receivingSubscription.getUserId());
				dataOutputStream.writeBoolean(receivingSubscription.isDrain());
				dataOutputStream.writeBoolean(receivingSubscription.getReceiveOptions().withDelayed());
				dataOutputStream.writeBoolean(receivingSubscription.getReceiveOptions().withDelta());
				dataOutputStream.writeInt(receivingSubscription.getState().ordinal());
				dataOutputStream.writeInt(receivingSubscription.getConnectionState().ordinal());
			}
			List<Long> potentialCentralDistributors = _subscriptionsManager.getPotentialCentralDistributors(_baseSubscriptionInfo);
			dataOutputStream.writeInt(potentialCentralDistributors.size());
			for(Long potentialCentralDistributor : potentialCentralDistributors) {
				dataOutputStream.writeLong(potentialCentralDistributor);
				TransmitterCommunicationInterface connection = _subscriptionsManager.getBestConnectionToRemoteDav(
						potentialCentralDistributor
				);
				long id = connection.getId();
				int resistance = connection.getThroughputResistance();
				long remoteUserId = connection.getRemoteUserId();
				dataOutputStream.writeLong(id);
				dataOutputStream.writeInt(resistance);
				dataOutputStream.writeLong(remoteUserId);
			}
		}
		finally {
			dataOutputStream.close();
		}
		return byteArrayOutputStream.toByteArray();
	}

	@Override
	public String toString() {
		return _subscriptionsManager.subscriptionToString(_baseSubscriptionInfo);
	}

	/**
	 * Erstellt eine Remote-Senken-Anmeldung über eine angegebene Verbindung bzw. gibt diese zurück wenn sie schon besteht
	 *
	 * @param connection Verbindung
	 * @return Senkenanmeldung
	 */
	public synchronized RemoteDrainSubscription getOrCreateRemoteDrainSubscription(final TransmitterCommunicationInterface connection) {
		List<ReceivingSubscription> subscriptions = getReceivingSubscriptions(connection);
		for(ReceivingSubscription subscription : subscriptions) {
			if(subscription instanceof RemoteDrainSubscription) {
				return (RemoteDrainSubscription) subscription;
			}
		}
		RemoteDrainSubscription subscription = new RemoteDrainSubscription(_subscriptionsManager, _baseSubscriptionInfo, connection);
		addReceivingSubscription(subscription);
		return subscription;
	}

	/**
	 * Erstellt eine Remote-Quellen-Anmeldung über eine angegebene Verbindung bzw. gibt diese zurück wenn sie schon besteht
	 *
	 * @param connection Verbindung
	 * @return Quellenanmeldung
	 */
	public synchronized RemoteSourceSubscription getOrCreateRemoteSourceSubscription(final TransmitterCommunicationInterface connection) {
		List<SendingSubscription> subscriptions = getSendingSubscriptions(connection);
		for(SendingSubscription subscription : subscriptions) {
			if(subscription instanceof RemoteSourceSubscription) {
				return (RemoteSourceSubscription) subscription;
			}
		}
		RemoteSourceSubscription subscription = new RemoteSourceSubscription(_subscriptionsManager, _baseSubscriptionInfo, connection);
		addSendingSubscription(subscription);
		return subscription;
	}

	/**
	 * Erstellt eine eingehende Anmeldung von einem anderen Datenverteiler als interessierter Empfänger (dieser Datenverteiler ist dann
	 * typischerweise potentiell Quell-Datenverteiler bzw. agiert als Proxy zum eigentlichen Zentraldatenverteiler). Wenn es schon eine
	 * bestehende Anmeldung gibt wird diese stattdessen um die potentiellen Zentraldatenverteiler erweitert.
	 *
	 * @param communication Verbindung
	 * @param ids           Liste mit Zentaldatenverteiler-Ids, die berücksichtigt werden sollen
	 * @return Anmeldung als entfernter Empfänger
	 */
	public synchronized void updateOrCreateRemoteReceiverSubscription(
			final TransmitterCommunicationInterface communication, final Collection<Long> ids) {
		for(ReceivingSubscription receivingSubscription : _subscriptionList.getReceivingSubscriptions()) {
			if(receivingSubscription.getCommunication() == communication && receivingSubscription instanceof RemoteReceiverSubscription) {
				((RemoteReceiverSubscription) receivingSubscription).setPotentialDistributors(ids);

				// Status neu setzen, damit eine eventuelle Anmeldung beim anderen Datenverteiler aktualisiert wird
				receivingSubscription.setState(receivingSubscription.getState(), getCentralDistributorId());

				updateRemoteConnections();
				return;
			}
		}
		final RemoteSubscription remoteSubscription;
		remoteSubscription = new RemoteReceiverSubscription(_subscriptionsManager, communication, _baseSubscriptionInfo, ids);
		addReceivingSubscription((ReceivingSubscription) remoteSubscription);
	}

	/**
	 * Erstellt eine eingehende Anmeldung von einem anderen Datenverteiler als interessierter Sender (dieser Datenverteiler ist dann
	 * typischerweise potentiell eine Senke bzw. agiert als Proxy zum eigentlichen Zentraldatenverteiler). Wenn es schon eine bestehende
	 * Anmeldung gibt wird diese stattdessen um die potentiellen Zentraldatenverteiler erweitert.
	 *
	 * @param communication Verbindung
	 * @param ids           Liste mit Zentaldatenverteiler-Ids, die berücksichtigt werden sollen
	 * @return Anmeldung als entfernter Sender
	 */
	public synchronized void updateOrCreateRemoteSenderSubscription(
			final TransmitterCommunicationInterface communication, final Collection<Long> ids) {
		for(SendingSubscription sendingSubscription : _subscriptionList.getSendingSubscriptions()) {
			if(sendingSubscription.getCommunication() == communication && sendingSubscription instanceof RemoteSenderSubscription) {
				((RemoteSenderSubscription) sendingSubscription).setPotentialDistributors(ids);

				// Status neu setzen, damit eine eventuelle Anmeldung beim anderen Datenverteiler aktualisiert wird
				sendingSubscription.setState(sendingSubscription.getState(), getCentralDistributorId());

				updateRemoteConnections();
				return;
			}
		}
		final RemoteSubscription remoteSubscription;
		remoteSubscription = new RemoteSenderSubscription(_subscriptionsManager, communication, _baseSubscriptionInfo, ids);
		addSendingSubscription((SendingSubscription) remoteSubscription);
	}

	/**
	 * Markiert das SubscriptionInfo als offen, sodass Änderungen an den Anmeldungen durchgeführt werden dürfen.
	 * <p/>
	 * Wird auf den SubscriptionsManager synchronisiert ausgeführt
	 */
	public void open() {
		_referenceCounter++;
	}

	/**
	 * Markiert das SubscriptionInfo als geschlossen, nachdem Änderungen an den Anmeldungen durchgeführt wurden. Falls das Objekt leer ist
	 * und von keinem mehr offen ist, wird geprüft ob Anmeldungen vorhanden sind. Falls nicht, wird das Objekt aus dem SubscriptionsManager
	 * entfernt.
	 * <p/>
	 * Synchronisiert auf den _subscriptionsManager, daher keine Synchronisation von _referenceCounter notwendig.
	 */
	public void close() {
		synchronized(_subscriptionsManager) {
			_referenceCounter--;
			if(_referenceCounter == 0 && isEmpty()) {
				_subscriptionsManager.removeSubscriptionInfo(this);
			}
		}
	}

	/**
	 * Wrapper-Klasse für eine wartende Umleitungsanmeldung
	 */
	private static class PendingSubscription {

		/**
		 * Eigentliche Anmeldung, die wartet bis die Umleitung komplett ist und dann die bisherige Anmeldung ersetzt
		 */
		private final RemoteCentralSubscription _newSubscription;

		/**
		 * Der letzte empfangene Datenindex zur Synchronisation
		 */
		private long _lastReceivedDataIndex = 1;

		private PendingSubscription(final RemoteCentralSubscription newSubscription) {
			_newSubscription = newSubscription;
		}

		private long getLastReceivedDataIndex() {
			return _lastReceivedDataIndex;
		}

		private void setLastReceivedDataIndex(final long lastReceivedDataIndex) {
			_lastReceivedDataIndex = lastReceivedDataIndex;
		}

		public RemoteCentralSubscription getNewSubscription() {
			return _newSubscription;
		}

		@Override
		public String toString() {
			return "PendingSubscription{" +
					"_newSubscription=" + _newSubscription +
					", _lastReceivedDataIndex=" + _lastReceivedDataIndex +
					'}';
		}
	}
}
