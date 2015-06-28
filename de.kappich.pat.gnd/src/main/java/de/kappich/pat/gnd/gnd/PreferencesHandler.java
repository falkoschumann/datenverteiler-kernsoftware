/*
 * Copyright 2012 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.kappich.pat.gnd.
 * 
 * de.kappich.pat.gnd is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.gnd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.gnd; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.pat.gnd.gnd;

import java.util.prefs.Preferences;

/**
 * Diese Klasse verwaltet den Speicherort für die Einstellungen der GND.
 *
 * @author Kappich Systemberatung
 * @version $Revision: x $
 */
public class PreferencesHandler {

	/** Verbindung zum Datenverteiler */
	private static String _kvPid;

	/** Instanz der Klasse */
	private static PreferencesHandler _instance;

	/**
	 * Es wird eine Instanz dieser Klasse zurückgeliefert.
	 *
	 * @return Instanz dieser Klasse
	 */
	public static PreferencesHandler getInstance() {
		if(_instance == null) {
			_instance = new PreferencesHandler();
		}
		return _instance;
	}

	/**
	 * Setzt die Pid des Konfigurationsverantwortlichen.
	 *
	 * @param kvPid Pid des Konfigurationsverantwortlichen
	 */
	public static void setKvPid(final String kvPid) {
		_kvPid = kvPid;
	}

	/**
	 * Diese Methode liefert den Startpfad zum Speichern der Einstellungen zurück.
	 *
	 * @return Startpfad der Einstellungen
	 */

	public Preferences getPreferenceStartPath() {
		Preferences node = Preferences.userRoot().node("de/kappich/pat/gnd");
		/**if(_kvPid != null) {
		 return node.node(_kvPid);
		 }*/
		return node;
	}
}
