/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.communication.accessControl;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.dav.util.accessControl.AccessControlManager;

import java.util.Collection;

/**
 * Interface, das Datenverteiler-Zugriffssteuerungs-Plugins implementieren müssen. Diese Plugins diesen dazu den Datenverkehr nach bestimmten
 * Attributgruppenverwendungen zu filtern, sodass weitere Rechteprüfungen durchgeführt werden können (beispielsweise ob ein Benutzer berechtigt ist,
 * Konfigurationsänderungen auszuführen oder Archivanfragen zu stellen. Diese Prüfungen sind in der Konfiguration oder im Archivsystem mangels Authentifizierung
 * nicht möglich).
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public interface AccessControlPlugin {

	/**
	 * Wird aufgerufen, nachdem das Plugin instantiiert wurde. Hiermit wird dem Plugin eine Verbindung zum Datenverteiler übergeben.
	 *
	 * @param accessControlManager Klasse, die die Standard-Zugriffsrechte verwaltet.
	 * @param clientDavInterface   Verbindung zum Datenverteiler
	 */
	void initialize(final AccessControlManager accessControlManager, final ClientDavInterface clientDavInterface);

	/**
	 * Wird nach {@link #initialize(de.bsvrz.dav.dav.util.accessControl.AccessControlManager, de.bsvrz.dav.daf.main.ClientDavInterface)} aufgerufen. Die Funktion
	 * soll alle Attributgruppenverwendungen zurückgeben, dessen Daten es ansehen und gegebenenfalls verändern will.
	 *
	 * @return Liste mit Attributgruppenverwendungen
	 */
	Collection<AttributeGroupUsage> getAttributeGroupUsagesToFilter();

	/**
	 * Wird aufgerufen wenn ein Datenpaket eintrifft, dass den in {@link #getAttributeGroupUsagesToFilter()} angegebenen Attributgruppenverwendungen entspricht.
	 * Die Funktion kann <ul><li> das Datenpaket unverändert weitergeben</li><li>das Datenpaket modifizieren</li><li>ein neues Datenobjekt erstellen</li><li>das
	 * Datenpaket verwerfen</li></ul>
	 *
	 * @param userID               Benutzer-ID, von dem das Datenpaket stammt. Ist nicht zwingend der Benutzer, der das Datenpaket abgesendet hat, sondern kann
	 *                             auch der Benutzer des Datenverteilers sein, der das Paket zuletzt verarbeitet hat. Die Standard-Berechtigungen zu diesem
	 *                             Benutzer können mit {@link AccessControlManager#getUser(long)} gelesen werden.
	 * @param baseSubscriptionInfo Anmeldung für die das Datenpaket verschickt wurde.
	 * @param data                 Datenpaket, das gefiltert wurde.
	 *
	 * @return <dl> <dt><code>data</code></dt><dd>Wenn das Datenpaket unverändert weitergesendet werden soll, ist der Parameter <code>data</code>
	 *         zurückzugeben.</dd> <dt><code>data.createModifiableCopy()</code></dt><dd>Wenn das Datenpaket verändert werden soll, kann mit
	 *         <code>data.createModifiableCopy()</code> eine veränderbare Kopie erzeugt und entsprechend verändert werden. Diese Kopie ist dann zurückzugeben.</dd>
	 *         <dt><code>clientDavInterface.createData()</code></dt><dd>Mit der in {@link #initialize(de.bsvrz.dav.dav.util.accessControl.AccessControlManager,
	 *         de.bsvrz.dav.daf.main.ClientDavInterface)} angegebenen Datenverteilerverbindung kann auch ein neues Data-Objekt erstellt und zurückgegeben werden.
	 *         Zu beachten ist, dass es dennoch an die ursprüngliche Anmeldung verschickt wird und deshalb die gleiche Attributgruppe benutzen sollte, wie das
	 *         originale Datenpaket. Ist das nicht der Fall tritt möglicherweise undefiniertes Verhalten auf.</dd> <dt><code>null</code></dt><dd>Wird
	 *         <code>null</code> zurückgegeben wird das Datenpaket verworfen und nicht weitergesendet. Sollte nur verwendet werden, wenn das Plugin selbst eine
	 *         Antwort bzw. ein eigenes Datenpaket verschickt, oder wenn das Eintreffen des Datenpakets unwichtig ist und niemand auf eine eventuelle Antwort
	 *         wartet.</dd> </dl>
	 */
	Data handleData(final long userID, final BaseSubscriptionInfo baseSubscriptionInfo, final Data data);
}
