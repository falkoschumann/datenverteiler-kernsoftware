/*
 * Copyright 2007 by Kappich Systemberatung Aachen
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
 * Schnittstelle die von der Verwaltung der günstigsten Wege benutzt wird, um die notwendigen Informationen zur Verwaltung der Anmeldelisten
 * weiterzugeben.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public interface ListsManagerInterface {

	/**
	 * Diese Methode wird von der Wegverwaltung aufgerufen, wenn ein Datenverteiler als erreichbar festgestellt wird. Sie wird aber auch aufgerufen, wenn
	 * festgestellt wird, dass ein Datenverteiler über einen neuen Weg erreichbar ist und nicht mehr über den alten Weg. Ein Datenverteiler kann über eine direkte
	 * Verbindung oder über einen anderen Datenverteiler erreichbar sein. Zuerst wird überprüft ob ein Zulieferereintrag für den spezifizierten Datenverteiler
	 * existiert. Existiert keiner, so wird ein Eintrag erstellt und in die Tabelle eingefügt. Wenn kein Zulieferer mehr existiert (delivererId = -1), dann wird
	 * eine  Zuliefererinformationskündigung wird zu jedem eingetragenen Abonnenten gesendet. Die Liste der Abonnenten wird dann geleert. Die
	 * Objekt- und Kombinationslisten werden auch geleert, da keine Verbindung mehr zum Datenverteiler existiert und die Informationen nicht mehr aktualisiert
	 * werden können. Wenn der neue Zulieferer ein anderer als der alte ist, wird eine Zuliefererinformationsabmeldung zum alten Zulieferer gesendet. Wenn der neue
	 * Zulieferer in der Abonnentenliste eingetragen ist, wird dieser Eintrag aus der Liste entfernt und an ihn eine Zuliefererinformationskündigung gesendet.
	 * Danach wird eine Zuliefererinformationsanmeldung zum neuen Zulieferer gesendet.
	 *
	 * @param delivererId ID des Zulieferers oder -1, wenn der kein Zulieferer mehr existiert.
	 * @param transmitterId          ID des Datenverteilers
	 */
	void addEntry(long delivererId, long transmitterId);

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn eine Datenverteilerverbindung nicht mehr existiert. Zunächst wird der Eintrag des
	 * Datenverteilers transmitterId aus der Zulieferertabelle entfernt. Der Zulieferer dieses Eintrags wird durch eine Zuliefererinformationsabmeldung, die
	 * Abonnenten dieses Eintrags durch eine Zuliefererinformationskündigung benachrichtigt. Danach wird überprüft bei welchen Einträgen der spezifizierte
	 * Datenverteiler als Zulieferer auftaucht. Dort wird überprüft, ob der Antragsdatenverteiler über einen anderen Datenverteiler erreichbar ist. In diesem
	 * Falle, wird ihm eine Zuliefererinformationsanmeldung gesendet. Wenn kein neuer Zulieferer existiert, werden die Abonnenten durch eine
	 * Zuliefererinformationskündigung informiert und die Abonnentenliste, Objektliste und Kombinationsliste geleert. Zuletzt wird überprüft, bei welchen Einträgen
	 * der spezifizierte Datenverteiler als Abonnent eingetragen ist und wird als Abonnent aus diesen Listen gestrichen.
	 *
	 * @param transmitterId ID des Datenverteilers
	 */
	void handleDisconnection(long transmitterId);

	/**
	 * Diese Methode wird von der Verbindungsverwaltung aufgerufen, wenn sich neue günstigste Wege für bestimmte Datenverteiler ergeben haben.
	 *
	 * @param ids Array mit den IDs der betroffenen Datenverteiler.
	 */
	void handleWaysChanges(long[] ids);
}
