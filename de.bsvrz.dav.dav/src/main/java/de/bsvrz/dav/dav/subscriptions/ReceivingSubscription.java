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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.ApplicationDataTelegram;
import de.bsvrz.dav.daf.main.ReceiveOptions;

/**
 * Interface für eine Anmeldung, die Daten empfängt
 * @author Kappich Systemberatung
 * @version $Revision: 11447 $
 */
public interface ReceivingSubscription extends Subscription {

	/**
	 * Gibt <tt>true</tt> zurück, wenn es sich um eine Senke handelt
	 * @return <tt>true</tt>, wenn es sich um eine Senke handelt, sonst (Empfänger) <tt>false</tt>
	 */
	public boolean isDrain();

	/**
	 * Gibt die Empfangsoptionen zurück, die der Empfänger bei der Anmeldung spezifiziert hat
	 * @return die Empfangsoptionen
	 */
	public ReceiveOptions getReceiveOptions();

	/**
	 * Sendet Daten an den Empfänger
	 * @param applicationDataTelegram Telegramm
	 */
	public void sendDataTelegram(ApplicationDataTelegram applicationDataTelegram);

	/**
	 * Gibt den Status des Empfängers zurück
	 * @return den Status des Empfängers
	 */
	public ReceiverState getState();

	/**
	 * Setzt den Status des Empfängers
	 * @param receiverState neuer Status
	 * @param centralTransmitterId
	 */
	public void setState(ReceiverState receiverState, final long centralTransmitterId);

	void sendStateTelegram(ReceiverState receiverState);
}
