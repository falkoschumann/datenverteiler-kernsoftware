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

package de.bsvrz.kex.kexdav.management;

import de.bsvrz.sys.funclib.operatingMessage.*;

/**
 * Diese Klasse veröffentlicht Probleme und Warnungen, die in KExDaV auftreten, über die Betriebsmeldungsverwaltung
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9045 $
 */
public class MessageSenderObserver extends KExDaVObserver {

	private String _lastDescription = "";

	@Override
	protected void onNewWarning(final Observable source, final Message message) {

		final MessageSender sender = MessageSender.getInstance();

		// Nur wichtige Meldungen versenden
		if(message.getErrorLevel().isMinor()) return;

		final String description = message.getDescription();

		// Keine identischen Meldungen direkt nacheinander versenden
		if(_lastDescription.equals(description)) return;

		final MessageGrade grade = message.getErrorLevel() == Message.ErrorLevel.ERROR ? MessageGrade.ERROR : MessageGrade.WARNING;

		sender.sendMessage("", MessageType.SYSTEM_DOMAIN, "", grade, MessageState.MESSAGE, description);

		// Letzte Meldung merken
		_lastDescription = description;
	}
}
