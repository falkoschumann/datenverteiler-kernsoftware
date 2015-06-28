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
 * Interface für eine Anmeldung, die Daten sendet
 * @author Kappich Systemberatung
 * @version $Revision: 11435 $
 */
public interface SendingSubscription extends Subscription {

	/**
	 * Gibt <tt>true</tt> zurück, wenn es sich um eine Quelle handelt
	 * @return <tt>true</tt>, wenn es sich um eine Quelle handelt, sonst (Sender) <tt>false</tt>
	 */
	public boolean isSource();

	/**
	 * Gibt <tt>true</tt> zurück, wenn der Sender spezifiziert hat, dass er Sendesteuerung benutzen möchte. Hat nur eine Wirkung bei lokalen Anmeldungen.
	 * @return <tt>true</tt>, wenn der Sender spezifiziert hat, dass er Sendesteuerung benutzen möchte, sonst <tt>false</tt>
	 */
	public boolean isRequestSupported();

	/**
	 * Gibt den Status der Anmeldung zurück
	 * @return den Status der Anmeldung
	 */
	public SenderState getState();

	/**
	 * Setzt den Status der Anmeldung
	 * @param senderState neuer Status
	 * @param centralTransmitterId
	 */
	public void setState(SenderState senderState, final long centralTransmitterId);
}
