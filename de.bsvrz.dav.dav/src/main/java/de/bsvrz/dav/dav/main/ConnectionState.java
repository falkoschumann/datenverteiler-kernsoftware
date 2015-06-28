/*
 * Copyright 2013 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionsConstants;

/**
 * Stellt einen Verbindungsstatus einer Anmeldung dar
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public enum ConnectionState {
	/** Lokale Verbindung, Status immer OK */
	FROM_LOCAL_OK(true),
	/** Eingehende Verbindung, Status immer OK */
	FROM_REMOTE_OK(true),
	/** Ausgehende Verbindung, warten auf Antwort (receipt/Quittung) des Kommunikationspartners */
	TO_REMOTE_WAITING(false),
	/** Ausgehende Verbindung, anderer Datenverteiler ist zuständig (OK) */
	TO_REMOTE_OK(true),
	/** Ausgehende Verbindung, anderer Datenverteiler ist nicht zuständig */
	TO_REMOTE_NOT_RESPONSIBLE(false),
	/** Ausgehende Verbindung, anderer Datenverteiler ist zuständig, aber es sind keine Rechte vorhanden */
	TO_REMOTE_NOT_ALLOWED(true),
	/** Ausgehende Verbindung, hinter dem verbundenen Datenverteiler gibt es mehrere Zentraldatenverteiler */
	TO_REMOTE_MULTIPLE(true);

	private final boolean _valid;

	ConnectionState(final boolean valid) {
		_valid = valid;
	}

	/**
	 * Gibt zurück, ob die Anmeldung gültig ist
	 * @return True bei gültiger Anmeldung, siehe Definitionen der einzelnen Enums
	 */
	public boolean isValid() {
		return _valid;
	}

	/**
	 * Wandelt ein TransmitterSubscriptionsConstants-Byte in einen Status um
	 * @param b byte
	 * @return Status
	 */
	public static ConnectionState fromByte(byte b){
		switch((int)b){
			case TransmitterSubscriptionsConstants.NEGATIV_RECEIP:
				return TO_REMOTE_NOT_RESPONSIBLE;
			case TransmitterSubscriptionsConstants.POSITIV_RECEIP:
				return TO_REMOTE_OK;
			case TransmitterSubscriptionsConstants.POSITIV_RECEIP_NO_RIGHT:
				return TO_REMOTE_NOT_ALLOWED;
			case TransmitterSubscriptionsConstants.MORE_THAN_ONE_POSITIV_RECEIP:
				return TO_REMOTE_MULTIPLE;
			default:
				throw new IllegalArgumentException("Ungültiger Status: " + b);
		}
	}
}
