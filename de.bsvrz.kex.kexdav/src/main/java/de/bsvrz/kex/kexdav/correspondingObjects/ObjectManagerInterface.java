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

package de.bsvrz.kex.kexdav.correspondingObjects;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.kex.kexdav.dataplugin.KExDaVDataPlugin;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

/**
 * Interface zum Callback von Aktionen die das kopieren von dynamischen Objekten bewirken können.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public interface ObjectManagerInterface {

	/**
	 * Wird aufgerufen, wenn im Zielsystem ein dynamisches Objekt gebraucht wird. Diese Funktion sollte, falls nötig und möglich, das Objekt kopieren. Es gibt
	 * derzeit keine Rückmeldung, ob dies auch geklappt hat.
	 *
	 * @param objectSpecification              Spezifikation zur Ermittlung des Objektes
	 * @param sourceConnection Quellverbindung
	 * @param targetConnection Zielverbindung
	 */
	void copyObjectIfNecessary(ObjectSpecification objectSpecification, ClientDavInterface sourceConnection, ClientDavInterface targetConnection);

	/**
	 * Gibt die lokale Verbindung zurück
	 *
	 * @return Lokale Verbindung
	 */
	ClientDavInterface getLocalConnection();

	/**
	 * Gibt die Remote-Verbindung zurück
	 *
	 * @return die Remote-Verbindung
	 */
	ClientDavInterface getRemoteConnection();

	/**
	 * Sucht den passenden Konfigurationsbereich um ein Objekt im Remote-System abzuspeichern
	 *
	 * @param typePid Objekt-Typ
	 *
	 * @return Konfigurationsbereich
	 *
	 * @throws MissingAreaException Falls kein Konfigurationsbereich gefunden werden konnte
	 */
	ConfigurationArea getConfigurationAreaRemote(String typePid) throws MissingAreaException;

	/**
	 * Sucht den passenden Konfigurationsbereich um ein Objekt im Lokal-System abzuspeichern
	 *
	 * @param typePid Objekt-Typ
	 *
	 * @return Konfigurationsbereich
	 *
	 * @throws MissingAreaException Falls kein Konfigurationsbereich gefunden werden konnte
	 */
	ConfigurationArea getConfigurationAreaLocal(String typePid) throws MissingAreaException;

	/**
	 * Gibt ein Plugin zurück um von atgSource nach atgTarget zu konvertieren. Wenn atgSource und atgTarget gleich sind wird ein{@link
	 * de.bsvrz.kex.kexdav.dataplugin.BasicKExDaVDataPlugin} zurückgegeben
	 *
	 * @param atgSource Quell-Attributgruppe (Pid)
	 * @param atgTarget Ziel-Attributgruppe (Pid)
	 *
	 * @return ein KExDaVDataPlugin
	 *
	 * @throws MissingPluginException Falls kein Plugin gefunden werden kann
	 */
	KExDaVDataPlugin getPlugIn(String atgSource, String atgTarget) throws MissingPluginException;

	CorrespondingObject getObject(ObjectSpecification objectSpecification);
}
