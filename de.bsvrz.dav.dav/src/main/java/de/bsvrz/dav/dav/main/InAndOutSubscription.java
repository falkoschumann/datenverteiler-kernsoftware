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

import java.util.*;


/**
 * Zu jeder Datenidentifikation wird in der Anmeldungsverwaltung (SubscriptionsManager) eine Liste mit Objekten dieser Klasse gespeichert. Jedes Objekt enthält
 * Information zu einer Anmeldung, die an den Datenverteiler gestellt wurde. Neben der Dateindentifikation und dem Ursprung und Art der Anmeldung, wird auch
 * gespeichert, ob bereits Anmeldungen an andere Datenverteilern gestellt wurden und wenn ja an welche. Außerdem wird verwaltet ob und wenn ja, wie diese
 * anderen Datenverteiler auf die Anmeldung geantwortet haben und welcher Datenverteiler der Zentraldatenverteiler ist.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8100 $
 */
public class InAndOutSubscription {

	/** Speichert eine an einen bestimmten Datenverteiler weitergeleitete Anmeldung. */
	static class OutSubscription {

		/** Datenverteiler, an den die Anmeldung weitergeleitet wird. */
		final long _targetTransmitter;

		/** Potentielle Zentraldatenverteiler, an die die Anmeldung weitergegeben werden soll. */
		final long[] _transmittersToConsider;

		/** Zentraldatenverteiler (nur nach Empfang einer positiven Quittung gültig) */
		long _mainTransmitter = 0;

		/**
		 * Zustand der Anmeldung. Folgende Werte sind möglich: <ul> <li>-1: Anmeldung beim anderen Datenverteilern wurde noch nicht quittiert.</li> <li> 0: Anmeldung
		 * beim anderen Datenverteilern wurde negativ quittiert.</li> <li> 1: Anmeldung beim anderen Datenverteilern wurde positiv quittiert.</li> <li> 2: Anmeldung
		 * beim anderen Datenverteilern wurde positiv quittiert, allerdings sind die notwendigen Rechte nicht vorhanden.</li> <li> 3: Dem anderen Datenverteiler
		 * liegen unzulässigerweise mehrere positive Quittungen von potentiellen Zentraldatenverteilernvor.</li> </ul>
		 */
		byte _state;

		/**
		 * Erzeugt ein neues Objekt mit den angegebenen Werten
		 *
		 * @param targetTransmitter      Datenverteiler, an den die Anmeldung weitergeleitet wird.
		 * @param transmittersToConsider Potentielle Zentraldatenverteiler, an die die Anmeldung weitergegeben werden soll.
		 * @param state                  Initialer Zustand der Anmeldung
		 */
		OutSubscription(final long targetTransmitter, final long[] transmittersToConsider, final byte state) {
			_targetTransmitter = targetTransmitter;
			_transmittersToConsider = transmittersToConsider;
			_state = state;
		}

		@Override
		public String toString() {
			return "OutSubscription{" + "_targetTransmitter=" + _targetTransmitter + ", _transmittersToConsider=" + Arrays.toString(_transmittersToConsider)
			       + ", _mainTransmitter=" + _mainTransmitter + ", _state=" + _state + '}';
		}
	}

	/**
	 * Objekt-Id des Kommunikationspartner von dem diese Anmeldung gesendet wurde. Für Datenverteiler ist das die ID des in der Topologie versorgten
	 * Datenverteiler-Objekts (typ.datenverteiler), für Applikationen ist das die ID des dynamischen Objekts (typ.applikation), das vom Datenverteiler beim
	 * Verbindungsaufbau erzeugt wird. Für die Verbindung zur Konfiguration wird der Wert 0 verwendet. Für die interne Applikationsverbindung, die der
	 * Datenverteiler zu sich selbst aufbaut, wird die ID des eigenen Datenverteiler-Objekts verwendet.
	 */
	final long _source;

	/** Die Datenidentifikation auf die sich die Anmeldung bezieht */
	final BaseSubscriptionInfo _baseSubscriptionInfo;

	/**
	 * Unterscheidet verschiedene Rollen des Anmeldenden. Der Wert <code>0</code> entspricht einer Anmeldung als Sender. Der Wert <code>1</code> entspricht einer
	 * Anmeldung als Empfänger.
	 */
	final byte _role;

	/**
	 * Zustand der Anmeldung. Wenn die Anmeldung an andere potentielle Zentraldatenverteiler weitergeleitet wurde, oder dann werden in diesem Feld die
	 * verschiedenen Reaktionen der anderen Datenverteiler aggregiert. Folgende Werte sind möglich: <ul> <li>-1: Anmeldungen bei anderen Datenverteilern wurden
	 * noch nicht quittiert</li> <li> 0: Es konnte kein Zentraldatenverteiler ermittelt werden.</li> <li> 1: Der im Feld <code>_mainTransmitter</code>
	 * spezifizierte Datenverteiler ist Zentraldatenverteiler.</li> <li> 2: Der im Feld <code>_mainTransmitter</code> spezifizierte Datenverteiler ist
	 * Zentraldatenverteiler, allerdings sind die notwendigen Rechte nicht vorhanden.</li> <li> 3: Es sind unzulässigerweise von mehreren Datenverteilern positive
	 * Quittungen empfangen worden.</li> </ul>
	 */
	byte _state;

	/** Die Id des Zentraldatenverteilers, wenn dieser bereits ermittelt wurde, sonst -1 */
	long _mainTransmitter;

	/** Die Liste der zu berücksichtigenden Datenverteiler, die beim Erzeugen dieses Objektes übergegeben wurde (dient auch zur Identifikation der Anmeldung). */
	long[] _transmittersToConsider;

	/** Die Liste der aktuellen zu berücksichtigenden Datenverteiler */
	long[] _currentTransmittersToConsider;

	/** An andere potentielle Zentraldatenverteiler gesendete Anmeldungen */
	OutSubscription[] _outSubscriptions;

	/** Information, ob diese Anmeldungsanfrage von einer Applikation oder von einem Datenverteiler gestartet wurde. */
	final boolean _fromTransmitter;

	

	/** Anzahl der gleichen Anmeldungen */
	int _count = 1;

	public String toString() {
		return "InAndOutSubscription[" + "_source:" + _source + ", " + "_baseSubscriptionInfo:" + _baseSubscriptionInfo + ", " + "_role:" + _role + ", "
		       + "_state:" + _state + ", " + "_mainTransmitter:" + _mainTransmitter + ", " + "_outSubscriptions:" + _outSubscriptions + ", "
		       + "_fromTransmitter:" + _fromTransmitter + ", " + "_count:" + _count + "]";
	}

	public void print(String initialIndent, String additionalIndent, String name) {
		System.out.format(
				"%s%s = InAndOutSubscription[_source:%21d, _role:%d, _state:%d, _mainTransmitter:%21d, _fromTransmitter:%b, _count:%d, _outSubscriptions %s ]\n",
				initialIndent,
				name,
				_source,
				_role,
				_state,
				_mainTransmitter,
				_fromTransmitter,
				_count,
				Arrays.toString(_outSubscriptions)
		);
	}

	InAndOutSubscription(long sourceId, TransmitterDataSubscription subscription) {
		if(subscription == null) {
			throw new IllegalArgumentException("Null Argumente");
		}
		_source = sourceId;
		_baseSubscriptionInfo = subscription.getBaseSubscriptionInfo();
		_role = subscription.getSubscriptionState();
		_transmittersToConsider = subscription.getTransmitters();
		_currentTransmittersToConsider = _transmittersToConsider;
		_state = (byte)-1;
		_mainTransmitter = -1;
		_fromTransmitter = true;
	}

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param sourceId                Id der Applikation
	 * @param info                    Anmeldeinformationen
	 * @param role                    0: Senderanmeldung 1: Empfängeranmeldung
	 * @param __transmitterToConsider Id´s von zu berücksichtigenden Datenverteilern
	 */
	InAndOutSubscription(long sourceId, BaseSubscriptionInfo info, byte role, long __transmitterToConsider[]) {
		if(info == null) {
			throw new IllegalArgumentException("Null Argumente");
		}
		_source = sourceId;
		_baseSubscriptionInfo = info;
		_role = role;
		_transmittersToConsider = __transmitterToConsider;
		_currentTransmittersToConsider = _transmittersToConsider;
		_state = (byte)-1;
		_mainTransmitter = -1;
		_fromTransmitter = false;
	}

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param origin Objekt dieser Klasse
	 */
	InAndOutSubscription(InAndOutSubscription origin) {
		if(origin == null) {
			throw new IllegalArgumentException("Null Argumente");
		}
		_source = origin._source;
		_baseSubscriptionInfo = origin._baseSubscriptionInfo;
		_role = origin._role;
		_transmittersToConsider = origin._transmittersToConsider;
		_currentTransmittersToConsider = _transmittersToConsider;
		_fromTransmitter = origin._fromTransmitter;
		_state = (byte)-1;
		_mainTransmitter = -1;
	}

	/**
	 * Erzeugt eine Instanz der inneren Klasse, die eine ausgehende Anmeldung repräsentiert.
	 *
	 * @param targetTransmitter      ID des Zieldatenverteilers
	 * @param transmittersToConsider Die Liste der aktuellen zu berücksichtigenden Datenverteiler.
	 * @param state                  Quittungszustand <ul> <li>-1: noch nicht quittiert</li> <li> 0: Keiner der angemeldeten Zentraldatenverteiler ist für die
	 *                               Daten zuständig.</li> <li> 1: Der spezifizierte Datenverteiler ist der Zuständige für die Daten.</li> <li> 2: Der
	 *                               spezifizierte Datenverteiler ist der Zuständige für die Daten, allerdings sind die notwendigen Rechte nicht vorhanden.</li>
	 *                               <li> 3: Mehr als eine Quelle im System angemeldet.</li> </ul>
	 *
	 * @return eine ausgehende Anmeldung
	 */
	OutSubscription newOutSubscription(long targetTransmitter, long[] transmittersToConsider, byte state) {
		return new OutSubscription(targetTransmitter, transmittersToConsider, state);
	}
}
