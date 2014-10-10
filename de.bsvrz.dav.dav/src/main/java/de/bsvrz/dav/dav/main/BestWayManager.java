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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.RoutingUpdate;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterBestWayUpdate;

/**
 * Diese Komponente verwaltet die aktuellen Weginformationen zu den anderen Datenverteilern. Diese Komponente hat verschiedene Methoden, so dass die
 * Verbindungsverwaltung darauf zugreifen kann. Die eigentliche Verwaltung wird in der Klasse RoutingTable realisiert.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class BestWayManager implements BestWayManagerInterface {

	/** Verwaltung von Anmeldelistentelegrammen */
	private final ListsManagerInterface _listsManager;

	/** Die Tabelle der Weginformationen */
	private RoutingTable _routingTable;

	/**
	 * Erzeugt eine Instanz dieser Komponente und hält für die interne Kommunikation eine Referenz auf die Verbindungsverwaltung fest. Eine Instanz der
	 * RoutingTable wird erzeugt und für die Weginformationsverwaltung bereitgestellt
	 *
	 * @param transmitterId Eigene Id des Datenverteilers
	 * @param distribution Die Verbindungsverwaltung des Datenverteilers
	 * @param listsManager Verwaltung von Anmeldelistentelegrammen
	 */
	public BestWayManager(long transmitterId, DistributionInterface distribution, final ListsManagerInterface listsManager) {
		_listsManager = listsManager;
		_routingTable = new RoutingTable(distribution, transmitterId);
	}

	/**
	 * {@inheritDoc}
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um den besten Weg zu einem Datenverteiler zu bestimmen. Sie ruft die findBestConnection-
	 * Methode der RoutingTable auf und gibt die ID des Datenverteilers zurück, über den der optimale Weg läuft. Wenn kein Weg zum spezifizierten Datenverteiler
	 * existiert, wird <code>-1</code> zurückgegeben.
	 */
	@Override
	public final long getBestWay(long destinationDavId) {
		final RoutingConnectionInterface connection = _routingTable.findBestConnection(destinationDavId);
		return ((connection == null) ? -1 : connection.getRemoteNodeId());
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine neue Verbindung zu einem Datenverteiler aufgebaut wurde. Zuerst wird die
	 * addConnection-Methode der RoutingTable aufgerufen, um einen neuen Eintrag in der Wegverwaltungstabelle zu erzeugen. Danach wird die addEntry-Methode der
	 * Anmeldelistenverwaltung aufgerufen, um für den neuen Datenverteiler eine entsprechende Anmeldungsliste anzulegen.
	 *
	 * @param connection Verbindung, repräsentiert einen Eintrag in der RoutingTable
	 */
	public void addWay(RoutingConnectionInterface connection) {
		if(connection == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		_routingTable.addConnection(connection);
		long transmitterId = connection.getRemoteNodeId();
		long bestWay = getBestWay(transmitterId);
		_listsManager.addEntry(bestWay, transmitterId);
	}

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine bestehende Verbindung nicht mehr vorhanden ist. Zuerst wird die removeConnection-
	 * Methode der RoutingTable aufgerufen, um den Eintrag der Verbindung connection aus der Wegverwaltungstabelle zu entfernen. Danach wird die
	 * handleDisconnection-Methode der Zuliefererdatenverwaltung aufgerufen, um aus der Zuliefererdatenverwaltungstabelle den Zulieferereintrag der spezifizierten
	 * Verbindung zu entfernen.
	 *
	 * @param connection Verbindung, repräsentiert einen Eintrag in der RoutingTable
	 */
	final void handleDisconnection(RoutingConnectionInterface connection) {
		if(connection == null) {
			throw new IllegalArgumentException("Argument ist null");
		}
		_routingTable.removeConnection(connection);
		_listsManager.handleDisconnection(connection.getRemoteNodeId());
	}

	/**
	 * Diese Methode wird von der Protokollsteuerung aufgerufen, wenn eine neue Weginformationsnachricht angekommen ist. Zuerst wird die update-Methode der
	 * RoutingTable aufgerufen, um die besten Wege der spezifizierten Verbindungen in der Wegverwaltungstabelle zu aktualisieren. Da eine Änderung der besten Wege
	 * auch eine Änderung der Zulieferer eines Antrags bei der Zuliefererdatenverwaltung hervorrufen kann, wird pro Änderung die addEntry-Methode der
	 * Zuliefererdatenverwaltung aufgerufen.
	 *
	 * @param connection               Verbindung, repräsentiert durch Eintrag in der routingTable
	 * @param transmitterBestWayUpdate Telegramm zur Aktualisierung der Matrix der günstigsten Wege
	 */
	public final void update(RoutingConnectionInterface connection, TransmitterBestWayUpdate transmitterBestWayUpdate) {
		if((connection == null) || (transmitterBestWayUpdate == null)) {
			throw new IllegalArgumentException("Argument ist null");
		}
		RoutingUpdate[] routingUpdates = transmitterBestWayUpdate.getRoutingUpdates();
		if(routingUpdates != null) {
			_routingTable.update(connection.getRemoteNodeId(), routingUpdates);
			long[] ids = new long[routingUpdates.length];
			for(int i = 0; i < routingUpdates.length; ++i) {
				ids[i] = routingUpdates[i].getTransmitterId();
			}
			_listsManager.handleWaysChanges(ids);
		}
	}

	public void dumpRoutingTable() {
		_routingTable.dumpRoutingTable();
	}
}
