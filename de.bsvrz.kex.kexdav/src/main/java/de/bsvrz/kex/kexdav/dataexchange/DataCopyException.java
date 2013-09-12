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

package de.bsvrz.kex.kexdav.dataexchange;

/**
 * Diese Exception kann von Plug-ins ausgelöst werden, wenn das Kopieren von Datenobjekten fehlschlägt. Es wird dann stattdessen ein leeres Datum gesendet und
 * eine entsprechende Warnung/Betriebsmeldung ausgegeben
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9167 $
 */
public class DataCopyException extends Exception {

	/**
	 * Erstellt eine neue Instanz
	 * @param message Nachricht
	 */
	public DataCopyException(final String message) {
		super(message);
	}

	/**
	 * Erstellt eine neue Instanz
	 * @param cause Fehlerursache
	 */
	public DataCopyException(final Throwable cause) {
		super(cause);
	}
}
