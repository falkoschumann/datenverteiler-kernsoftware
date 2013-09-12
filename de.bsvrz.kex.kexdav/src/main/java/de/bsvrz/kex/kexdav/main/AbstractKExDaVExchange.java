/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.main;

import de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.management.Message;
import de.bsvrz.kex.kexdav.parameterloader.RemoteDaVParameter;

import java.util.*;

/**
 * Abstrakte Klasse, die den Austausch von Objekten zwischen 2 Datenverteilern kapselt. Diese Klasse erhält Parameter, Und erstellt daraus eine beliebige anzahl
 * von Definitionen (generischer Parameter D). Beim erneuten Eintreffen von Parametern werden diese Definitionen aktualisiert und mit den alten Definitionen
 * verglichen. Für jede neue Definition wird ein neues Austausch-Objekt erstellt (generischer Parameter E). Für jede weggefallene Definition wird das zugehörige
 * Austauschobjekt entsorgt. Wenn eine Definition unverändert bleibt, wird auch am Austauschobjekt nichts gemacht. So werden nur die Austausche von
 * Parameteränderungen beeinflusst, wo auch wirklich die Parameter geändert werden. Hinweis: Definitionen und Austauschobjekte sollten weitgehend immutable
 * sein.
 * <p/>
 * Descriptions sollten sinnvolle implementierungen von equals und hashcode haben, da sie hier in einer HashMap gespeichert werden und bei bedarf verglichen werden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public abstract class AbstractKExDaVExchange<D, E> {

	private RemoteDaVParameter _parameter;

	private final Map<D, E> _exchangeMap = new HashMap<D, E>();

	private final ManagerInterface _manager;

	/**
	 * Abstrakte Klasse, die den Austausch von Objekten zwischen 2 Datenverteilern kapselt
	 *
	 * @param parameter Parameter
	 * @param manager   KExDaV-Verwaltung
	 */
	protected AbstractKExDaVExchange(
			final RemoteDaVParameter parameter, final ManagerInterface manager) {
		_parameter = parameter;
		_manager = manager;
	}

	/**
	 * Startet den Austausch, aktualisiert den Austausch falls bereits gestartet
	 *
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException
	 *          falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	protected void start() throws MissingAreaException {
		refreshExchanges(_parameter);
	}

	/** Stoppt den Austausch */
	protected void stop() {
		for(final E exchange : _exchangeMap.values()) {
			removeExchange(exchange);
		}
		_exchangeMap.clear();
	}

	/**
	 * Wird aufgerufen, falls neue Parameter eintreffen
	 *
	 * @param newParameters Neue Parameter, die die auszutauschenden Daten und Objekte festlegen
	 *
	 * @throws de.bsvrz.kex.kexdav.correspondingObjects.MissingAreaException
	 *          falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	public void setParameter(final RemoteDaVParameter newParameters) throws MissingAreaException {
		refreshExchanges(newParameters);
		_parameter = newParameters;
	}

	/**
	 * Aktualisiert die bestehenden Austauschmodule
	 * @param parameters Parameter
	 * @throws MissingAreaException falls kein gültiger Konfigurationsbereich zum Erstellen von Objekten angegeben wurde, aber einer benötigt wurde.
	 */
	private void refreshExchanges(final RemoteDaVParameter parameters) throws MissingAreaException {
		final Collection<D> obsoleteEntries = new ArrayList<D>();
		final Set<D> newExchangeDescriptions = getExchangeDescriptionsFromNewParameters(parameters);

		// Nicht mehr gebrauchte Daten-Austausch-Module entfernen
		for(final D entry : _exchangeMap.keySet()) {
			if(!newExchangeDescriptions.contains(entry)) {
				obsoleteEntries.add(entry);
			}
		}

		for(final D obsoleteEntry : obsoleteEntries) {
			final E exchange = _exchangeMap.remove(obsoleteEntry);
			removeExchange(exchange);
		}

		// Elemente, die schon enthalten sind, ignorieren
		newExchangeDescriptions.removeAll(_exchangeMap.keySet());

		notifyNewExchangeDescriptions(newExchangeDescriptions);

		// Neue Elemente hinzufügen
		for(final D description : newExchangeDescriptions) {
			try {
				final E exchange = createExchange(description);
				_exchangeMap.put(description, exchange);
			}
			catch(MissingAreaException e) {
				throw e;
			}
			catch(KExDaVException e) {
				_manager.addMessage(Message.newError(e));
			}
		}
	}

	/**
	 * Benachrichtigung über neue Asutausche, damit z.B. Systemobjekte geladen werden können
	 * @param newExchangeDescriptions neue Austauschbeschreibungen
	 */
	protected void notifyNewExchangeDescriptions(final Set<D> newExchangeDescriptions){
		
	}

	/**
	 * Gibt den KExDaV-Manager zurück
	 * @return Manager
	 */
	protected final ManagerInterface getManager() {
		return _manager;
	}

	/**
	 * Gibt die Descriptions und zugehörigen Austauschobjekte zurück, die zur Zeit in dieser Klasse gespeichert sind
	 * @return Map mit Descriptions und Austauschobjekten
	 */
	public Map<D, E> getExchangeMap() {
		return Collections.unmodifiableMap(_exchangeMap);
	}

	/**
	 * Template-Methode, die anhand einer Description ein Datenaustausch-Klasse erstellt
	 * @param description Description
	 * @return Datenaustausch-Klasse
	 * @throws KExDaVException Falls ein Fehler auftritt
	 */
	protected abstract E createExchange(final D description) throws KExDaVException;

	/**
	 * Template-Methode, die eine Datenaustauschklasse deaktiviert bzw. entfernt
	 * @param exchange Datenaustauschklasse
	 */
	protected abstract void removeExchange(final E exchange);

	/**
	 * Template-Methode, die für einen Parameter-Datensatz alle Datenbeschreibungen zurückgeben soll
	 * @param parameters Parameter
	 * @return Set mit Datenbeschreibungen
	 */
	protected abstract Set<D> getExchangeDescriptionsFromNewParameters(final RemoteDaVParameter parameters);
}
