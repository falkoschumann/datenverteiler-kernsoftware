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

package de.bsvrz.dav.dav.subscriptions;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterSubscriptionType;
import de.bsvrz.dav.dav.main.ConnectionState;

/**
 * Basis-Interface für eine Kommunikation zwischen zwei Datenverteilern
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public interface TransmitterCommunicationInterface extends CommunicationInterface {

	/**
	 * Sorgt für eine Datenanmeldung bei einem entfernten Zentraldatenverteiler. Wird von diesem Datenverteiler selbstständig ausgelöst, wenn es keine lokale Quelle/Senke
	 * gibt und es potentielle remote-Datenverteiler über diese Verbindung gibt.
	 * @param remoteCentralSubscription Anmeldeinformation auf einen Zentraldatenverteiler
	 */
	void subscribeToRemote(RemoteCentralSubscription remoteCentralSubscription);

	/**
	 * Sorgt für Datenabmeldung bei einem entfernten Zentraldatenverteiler. Wird von diesem Datenverteiler selbstständig ausgelöst, wenn kein
	 * Bedarf mehr an einer solchen Verbindung besteht.
	 * @param remoteCentralSubscription Anmeldeinformation auf einen Zentraldatenverteiler
	 */
	void unsubscribeToRemote(RemoteCentralSubscription remoteCentralSubscription);

	/**
	 * Sendet eine Quittung auf eine Datenanmeldung zurück bzw. informiert darüber, ob es wirklich einen Zentraldatenverteiler
	 * auf, bzw hinter diesem Datenverteiler gibt.
	 * @param centralTransmitterId ID des zentraldatenverteilers, sofern vorhanden. Typischerweise die ID dieses Datenverteilers oder eines nachgelagerten Datenverteilers.
	 * @param state Verbindungszustand, siehe {@link de.bsvrz.dav.dav.main.ConnectionState}
	 * @param receiver Art der Anmeldung Quelle-Empfänger oder Sender-Senke
	 * @param remoteReceiverSubscription Entweder eine {@link RemoteSenderSubscription} oder eine {@link de.bsvrz.dav.dav.subscriptions.RemoteReceiverSubscription},
	 *                                   die die zugehörige eingehende Anmeldung von eine manderen Datenverteiler darstellt.
	 */
	void sendReceipt(
			final long centralTransmitterId,
			final ConnectionState state,
			final TransmitterSubscriptionType receiver,
			RemoteSubscription remoteReceiverSubscription);

	/**
	 * Gibt den Widerstand/die Gewichtung dieser Verbindung zurück. Wird bei der Bestimmung der besten Wege verwendet.
	 * @return Positive-Integer-Zahl. Je größer die Zahl, desto eher werden andere Routen mit kleiner Zahl bevorzugt.
	 */
	int getThroughputResistance();
}
