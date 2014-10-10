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

import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TelegramTimeRequest;

/**
 * Dieses Interface deklariert Methoden, welche von der Verbindungsverwaltung aufgerufen werden. Die Methoden werden sowohl in DAV-DAF als auch in der DAV-DAV
 * Highlevelcommunication implementiert.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public interface ServerHighLevelCommunication {

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen. Ein Telegramm {@link TelegramTimeRequest} wird erzeugt und zur Applikation gesendet. Danach
	 * wird auf die Antwort {@link de.bsvrz.dav.daf.communication.lowLevel.telegrams.TelegramTimeAnswer} gewartet. Wenn die Antwort nicht innerhalb der angegebenen
	 * maximalen Wartezeit angekommen ist, wird eine {@link CommunicationError Ausnahme} erzeugt.
	 *
	 * @param maxWaitingTime Maximale Zeit, die auf eine Antwort gewartet wird.
	 *
	 * @return die Telegrammlaufzeit oder <code>-1</code>, wenn nicht innnerhalb der maximalen Wartezeit eine Antwort empfangen wurde.
	 *
	 * @throws CommunicationError Wenn bei der initialen Kommunikation mit dem Datenverteiler Fehler aufgetreten sind.
	 */
	public long getTelegramTime(final long maxWaitingTime) throws CommunicationError;

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, sobald die Konfiguration vorhanden ist, um zu signalisieren, dass eine blockierte
	 * Authentifizierung weiter bearbeitet werden kann: Steht eine Authentifizierungsschlüsselanfrage an, während die Konfiguration noch nicht vorhanden ist, wird
	 * die Antwort blockiert bis die Konfiguration bereit ist. Dies ist notwendig, da nur die Konfiguration bestimmen kann, ob die Authentifizierungsdaten korrekt
	 * sind. Auch für die Interpretation der Daten ist die Konfiguration notwendig.
	 */
	public void continueAuthentification();

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, um die ID der verbundenen Applikation zu erhalten.
	 *
	 * @return ID des Kommunikationpartners
	 */
	public long getId();

	/**
	 * Gibt die ID des verbundenen Benutzers zurück.
	 *
	 * @return die Benutzer ID
	 */
	public long getRemoteUserId();

	/**
	 * Terminiert die Kommunikationsverbindung.
	 *
	 * @param error   Ist <code>true</code>, wenn die Verbindung im Fehlerfall abgebrochen werden soll, ohne die noch gepufferten Telegramme zu versenden;
	 *                <code>false</code>, wenn versucht werden soll alle gepufferten Telegramme zu versenden.
	 * @param message Fehlermeldung, die die Fehlersituation näher beschreibt.
	 */
	void terminate(boolean error, final String message);
}
