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

package de.bsvrz.kex.kexdav.systemobjects;

import de.bsvrz.kex.kexdav.main.KExDaVException;

/**
 * Exception, die ausgelöst wird, wenn ein benötigtes Systemobjekt nicht gefunden wird
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9202 $
 */
public class MissingObjectException extends KExDaVException{

	/**
	 * Konstruktor
	 * @param message Beschreibung
	 */
	public MissingObjectException(final String message) {
		super(message);
	}

	/**
	 * Initialisiert eine MissingObjectException mit einer anderen Exception.
	 * @param cause Ursache
	 */
	public MissingObjectException(final Throwable cause) {
		super(cause);
	}
}
