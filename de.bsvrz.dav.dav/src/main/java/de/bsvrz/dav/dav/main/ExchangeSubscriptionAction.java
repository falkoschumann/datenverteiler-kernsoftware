/*
 * Copyright 2009 by Kappich Systemberatung, Aachen
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

/**
 * Diese Klasse führt den Austausch von Anmeldungen durch.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 7715 $
 */
public class ExchangeSubscriptionAction {


	/** alte Anmeldung */
	InAndOutSubscription oldSubscription;

	/** neue Anmeldung */
	InAndOutSubscription newSubscription;

	/** Id des DAV */
	long transmitterId;

	/** Id eines Dav, der nicht mehr auf dem Weg liegt */
	long oldRootingTransmitterId;

	/** Id des DAV, über den die Verbindung billiger ist. */
	long newRootingTransmitterId;

	/** alter Verbindungsdatenindex */
	long oldConnectionDataIndex;

	/** neuer Verbindungsdatenindex (wird mit -1 im Konstruktor initialisiert)*/
	long newConnectionDataIndex;

	/**
	 * Erstellt ein neues Objekt von ExchangeSubscriptionAction.
	 *
	 * @param _oldSubscription         alte Anmeldung
	 * @param _newSubscription         neue Anmeldung
	 * @param _oldRootingTransmitterId ID des alten Transmitters, über den der Weg führt(e)
	 * @param _newRootingTransmitterId ID des neuen Transmitters, über den der weg führen soll.
	 * @param _transmitterId           Ausgangstransmitter
	 * @param _oldDataIndex            alter Datensatzindex
	 */
	public ExchangeSubscriptionAction(
			InAndOutSubscription _oldSubscription,
			InAndOutSubscription _newSubscription,
			long _oldRootingTransmitterId,
			long _newRootingTransmitterId,
			long _transmitterId,
			long _oldDataIndex
	) {
		oldSubscription = _oldSubscription;
		newSubscription = _newSubscription;
		oldRootingTransmitterId = _oldRootingTransmitterId;
		newRootingTransmitterId = _newRootingTransmitterId;
		transmitterId = _transmitterId;
		oldConnectionDataIndex = _oldDataIndex;
		newConnectionDataIndex = -1L;
	}

	/**
	 * Prüft, ob die Verbindung in der Gegenrichtung "sicher" ist. Wenn es sich um eine Senderanmeldung handelt oder der Datensatzindex 0 ist, wird <code>true
	 * </code> zurückgegeben.
	 *
	 *
	 * @return <code>true </code> wenn Verbindung "sicher". <code>false </code>, wenn nicht.
	 */
	final synchronized boolean isRedirectionSafe() {
		if(oldSubscription._role == 0) {
			return true;
		}
		if(oldConnectionDataIndex == 0L) {
			return true;
		}

		if(newConnectionDataIndex != -1L && ((newConnectionDataIndex & 0xFFFFFFFFFFFFFFFCL) < ((oldConnectionDataIndex & 0xFFFFFFFFFFFFFFFCL) + 4))) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Setzt den Datensatzindex. Wenn die <code>connectedTransmitterId</code> gleich der <code>oldRootingTransmitterID</code> ist, wird
	 * <code>oldConnectionDataIndex</code> auf <code>dataIndex</code> gesetzt und <code>true</code> zurückgegeben. Wenn die <code>connectedTransmitterId</code>
	 * gleich der <code>newRootingTransmitterID</code> ist, wird <code>newConnectionDataIndex</code> auf <code>dataIndex</code> gesetzt und <code>false</code>
	 * zurückgegeben.
	 *
	 * @param connectedTransmitterId ID des Transmitters, zu dem die Verbindung aufgebaut werden soll.
	 * @param dataIndex              Datensatzindex
	 *
	 * @return <code>true</code>, wenn sich die ID des DAV nicht geändert hat, <code>false</code>, wenn sich die ID des DAV geändert hat.
	 */
	final synchronized boolean setDataIndex(long connectedTransmitterId, long dataIndex) {
		if(connectedTransmitterId == oldRootingTransmitterId) {
			oldConnectionDataIndex = dataIndex;
			return true;
		}
		else if(connectedTransmitterId == newRootingTransmitterId) {
			newConnectionDataIndex = dataIndex;
			return false;
		}
		else {
			throw new IllegalArgumentException("Falsche Datenverteilerverbindung als Parameter");
		}
	}
}
