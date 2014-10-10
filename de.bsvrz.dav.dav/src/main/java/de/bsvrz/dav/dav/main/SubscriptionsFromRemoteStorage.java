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

import java.util.*;

/**
 * Speichert alle Anmeldungen, die über eine Kommunikationsverbindung mit einer Applikation oder mit einem anderen Datenverteiler empfangen wurden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11561 $
 */
public abstract class SubscriptionsFromRemoteStorage {

	/** DAV - DAF Kommunikation */
	public static final int T_A = 0;

	/** DAV - DAV Kommunikation */
	public static final int T_T = 1;

	/** Verbindung zu einem Kommunikationspartner (Applikation oder Datenverteiler) von dem die hier verwalteten Anmeldungen stammen. */
	protected ServerHighLevelCommunication _connection;

	/** Die Sendeanmeldungen-Tabelle */
	protected Hashtable sendSubscriptionTable;

	/** Die Empfangsanmeldungen-Tabelle */
	protected Hashtable receiveSubscriptionTable;

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param connection Die verbindung zur Kommunikation
	 */
	public SubscriptionsFromRemoteStorage(ServerHighLevelCommunication connection) {
		sendSubscriptionTable = new Hashtable();
		receiveSubscriptionTable = new Hashtable();
		_connection = connection;
	}

	/**
	 * Gibt die Verbindungskomponente zurück.
	 *
	 * @return die Verbindungskomponente
	 */
	final ServerHighLevelCommunication getConnection() {
		return _connection;
	}

	/**
	 * Gibt den Typ der Komponente zurück <ul> <li>0: Verbindung mit einer Applikation</li> <li>1: Verbindung mit einem Datenverteiler</li> </ul>
	 *
	 * @return Typ der Komponente
	 */

	abstract int getType();

	/** Schreibt die Anmeldeinformationen in den Ausgabekanal. */
	public void printSubscriptions() {
		Set entries = receiveSubscriptionTable.entrySet();
		for(Iterator iterator = entries.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry)iterator.next();
			BaseSubscriptionInfo key = (BaseSubscriptionInfo)entry.getKey();
			Object value = entry.getValue();
			System.out.println("  key: " + key + ", value " + value);
		}
	}

	public void print(String initialIndent, String additionalIndent, String name) {
		System.out.println(initialIndent + name + ": " + getClassName() + "{");
		System.out.println(initialIndent + additionalIndent + "_connection = " + _connection.getClass().getSimpleName() + " von " + _connection.getId());
		//_connection.print(prefix + additionalIndent, additionalIndent, name);
		printHashtable(initialIndent + additionalIndent, additionalIndent, "sendSubscriptionTable", sendSubscriptionTable);
		printHashtable(initialIndent + additionalIndent, additionalIndent, "receiveSubscriptionTable", receiveSubscriptionTable);
		System.out.println(initialIndent + "}");
	}

	private void printHashtable(final String initialIndent, final String additionalIndent, String name, final Hashtable hashtable) {
		System.out.println(initialIndent + name + ": Hashtable{");
		final Set<Map.Entry> entries = hashtable.entrySet();
		for(Map.Entry entry : entries) {
			System.out.println(initialIndent + additionalIndent + "key: " + entry.getKey() + ", value: " + entry.getValue());
		}
		System.out.println(initialIndent + "}");
	}

	private String getClassName() {
		return getClass().getSimpleName();
	}


	@Override
	public String toString() {
		return "SubscriptionsFromRemoteStorage{" + "sendSubscriptionTable=" + sendSubscriptionTable + ", receiveSubscriptionTable=" + receiveSubscriptionTable
		       + '}';
	}
}
