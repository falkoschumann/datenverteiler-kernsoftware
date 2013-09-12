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

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DavRequester;
import de.bsvrz.dav.daf.main.config.SystemObject;

/**
 * Implementiert die Schnittstelle Applikation-Dav (siehe {@link #DavRequester}) auf Datenverteiler-Seite
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8953 $
 */

public class DavDavRequester extends DavRequester {

	/** Verweis auf DavTransactionManager */
	private final DavTransactionManager _davTransactionManager;

	/**
	 * Erstellt einen neuen DavDavRequester
	 * @param connection Verbindung zum Datenverteiler
	 * @param davTransactionManager DavTransactionManager
	 */
	public DavDavRequester(final ClientDavConnection connection, final DavTransactionManager davTransactionManager) {
		super(
				connection,
				connection.getDataModel().getAspect("asp.antwort"),
				connection.getDataModel().getAspect("asp.anfrage")
		);
		_davTransactionManager = davTransactionManager;
		subscribeDrain(_connection.getLocalDav());
	}

	@Override
	protected void onReceive(final Data data) {
		final SystemObject sender = data.getReferenceValue("Absender").getSystemObject();
		final long requestId = data.getUnscaledValue("AnfrageIndex").longValue();
		final int requestKind = (int)data.getUnscaledValue("AnfrageTyp").longValue();
		final byte[] bytes = data.getUnscaledArray("Daten").getByteArray();
		long answerKind = ANSWER_OK;
		String errorString = null;
		try {
			switch(requestKind) {
				// je nach Anfragetyp die entsprechende Aktion durchführen
				case SUBSCRIBE_TRANSMITTER_SOURCE:
					// Anmeldung einer Transaktionsquelle
					_davTransactionManager.handleSubscribeTransactionSource(bytes);
					break;
				case SUBSCRIBE_TRANSMITTER_DRAIN:
					// Anmeldung einer Transaktionssenke
					_davTransactionManager.handleSubscribeTransactionDrain(bytes);
					break;
				default:
					answerKind = ANSWER_ERROR;
					errorString = "Ungültige Anfrage: " + requestKind;
			}
		}
		catch(Exception e){
			_debug.info("Beim Verarbeiten einer Nachricht trat ein Fehler auf", e);
			errorString = "Fehler beim Verarbeiten der Anfrage: " + e;
			answerKind = ANSWER_ERROR;
		}

		// Antwort senden
		if(answerKind == ANSWER_ERROR) {
			sendError(sender, requestId, errorString, _connection.getLocalDav());
		}
		else {
			sendBytes(sender, requestId, ANSWER_OK, new byte[0], _connection.getLocalDav());
		}
	}
}
