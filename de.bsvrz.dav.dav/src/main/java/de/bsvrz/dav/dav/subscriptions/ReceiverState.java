/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.subscriptions;

/**
 * Status einer Senek oder eines Empfängers
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11461 $
 */
public enum ReceiverState {
	/** Unbekannt, Empfänger wurde gerade erst angemeldet */
	UNKNOWN(false),
	/** Es gibt keine Quellen/Sender */
	NO_SENDERS(true),
	/** Es gibt Quellen/Sender */
	SENDERS_AVAILABLE(true),
	/** Es ist unbekannt, ob Sender verfügbar sind */
	WAITING(true),
	/** Es fehlen Rechte */
	NOT_ALLOWED(false),
	/** Ungültige Anmeldung (z.B. mehrere Senken) */
	INVALID_SUBSCRIPTION(false),
	/** ungültiger Status einer entfernten Anmeldung, z.B. keine Rechte am entfernten Dav oder nicht verantwortlich */
	NO_REMOTE_DRAIN(false),
	/** es gibt mehrere mögliche Zentraldatenverteiler, Anmeldung daher deaktiviert */
	MULTIPLE_REMOTE_LOCK(false);

	private final boolean _validReceiver;

	ReceiverState(final boolean validReceiver) {
		_validReceiver = validReceiver;
	}

	public boolean isValidReceiver() {
		return _validReceiver;
	}
}
