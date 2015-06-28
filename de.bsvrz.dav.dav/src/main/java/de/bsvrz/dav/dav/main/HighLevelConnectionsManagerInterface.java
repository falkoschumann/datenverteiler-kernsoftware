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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;

import java.util.Collection;
import java.util.List;

/**
 * Interface für die Verbindungsverwaltung (ehemals ConnectionsManager). Erlaubt Abfragen nach Verbindungen und ähnlichem.
 */
public interface HighLevelConnectionsManagerInterface {

	/**
	 * Beendet eine Verbindung zu einer Applikation
	 * @param communication Applikationsverbindung
	 */
	void removeConnection(T_A_HighLevelCommunication communication);

	/**
	 * Beendet eine Verbindung zu einem Datenverteiler
	 * @param communication Datenverteiler-Verbindung
	 */
	void removeConnection(T_T_HighLevelCommunication communication);

	/**
	 * Gibt die ID der Konfiguration mit der gegebenen Pid zurück
	 *
	 * @param configurationPid Die Pid der Konfiguration
	 *
	 * @return die Id der Konfiguration
	 */
	long getConfigurationId(String configurationPid);

	/**
	 * Gibt den Typ der lokalen ClientDav-Verbindung zurück
	 * @return pid der lokalen ClientDav-Verbindungs-Anwendung
	 */
	String getTransmitterTypePid();

	/**
	 * Gibt den Namen der lokalen Transmitter-Anwendung zurück
	 * @return Name der Anwendung
	 */
	String getTransmitterApplicationName();

	/**
	 * Gibt die ID des eigenen Transmitters zurück
	 * @return Transmitter-Id
	 */
	long getTransmitterId();

	/**
	 * Gibt den Benutzernamen zurück unter dem der Datenverteiler läuft
	 * @return Benutzername
	 */
	String getUserName();

	/**
	 * Gibt das Passwort zurück unter dem der Datenverteiler läuft
	 * @return Passwort
	 */
	String getUserPassword();

	/**
	 * Gibt das in der Passwort(passwd)-Datei gespeicherte Passwort für den angegebenen Benutzernamen zurück
	 * @param userName Benutzername
	 * @return Passwort oder null falls der Benutzername nicht gefunden werden konnte
	 */
	String getStoredPassword(String userName);

	/**
	 * Gibt das gewicht einer Verbindung zu einem anderen Datenverteiler zurück
	 * @param transmitterId ID des anderen Datenverteilers
	 * @return Gewicht
	 */
	short getWeight(long transmitterId);

	/**
	 * Gibt die Verbindungsinformation der Verbindung von diesem Datenverteiler zum angegeben zurück
	 * @param connectedTransmitterId Verbundener Datenverteiler
	 * @return Verbindungsinformation
	 */
	TransmitterConnectionInfo getTransmitterConnectionInfo(long connectedTransmitterId);

	/**
	 * Gibt die Verbindungsinformation der Verbindung vom angegebenen Datenverteiler zu diesem zurück
	 * @param connectedTransmitterId Verbundener Datenverteiler
	 * @return Verbindungsinformation
	 */
	TransmitterConnectionInfo getRemoteTransmitterConnectionInfo(long connectedTransmitterId);

	/**
	 * Gibt eine Referenz auf den Telegram-Manager zurück
	 * @return Telegram-Manager
	 */
	TelegramManager getTelegramManager();

	/**
	 * Ermittelt von einer Anwendungs-ID die zugehörige Applikations-Verbindung
	 * @param applicationId ID
	 * @return Verbindung
	 */
	T_A_HighLevelCommunicationInterface getApplicationConnectionFromId(long applicationId);

	/**
	 * Ermittelt von einer Transmitter-ID die zugehörige Applikations-Verbindung
	 * @param transmitterId ID
	 * @return Verbindung
	 */
	T_T_HighLevelCommunicationInterface getTransmitterConnectionFromId(long transmitterId);

	/**
	 * Gibt alle Anwendungsverbindungen zurück
	 * @return Anwendungsverbindungen
	 */
	Collection<T_A_HighLevelCommunication> getAllApplicationConnections();

	/**
	 * Gibt alle Datenverteilerverbindungen zurück
	 * @return Datenverteilerverbindungen
	 */
	Collection<T_T_HighLevelCommunication> getAllTransmitterConnections();

	/**
	 * Gibt die vom Anwender festgelegten Namen der Zugriffsrechte-Plugin-Klassen zurück
	 * @return Liste mit Zugriffsrechte-Plugin-Klassen-Namen
	 */
	List<String> getAccessControlPluginsClassNames();

	/**
	 * Beendet alle Verbindungen und Threads
	 * @param isError Zum signalisieren, dass ein Fehler aufgetreten ist: true, sonst false
	 * @param message Nach Bedarf eine Fehlermeldung o.ä. zur Ursache des Terminierungsbefehls
	 */
	void shutdown(boolean isError, String message);

	/**
	 * Gibt <tt>true</tt> zurück, wenn sich der Datenverteiler gerade beendet
	 * @return <tt>true</tt>, wenn sich der Datenverteiler gerade beendet, sonst <tt>false</tt>
	 */
	boolean isClosing();

	/**
	 * Gibt das Konfigurations-Objekt, das den datenverteiler repräsentiert zurück
	 * @return das Konfigurations-Objekt, das den datenverteiler repräsentiert
	 */
	ConfigurationObject getDavObject();

	/**
	 * Gibt potentielle Zentraldatenverteiler für die angegebene Anmeldeinformation zurück
	 * @param baseSubscriptionInfo Anmeldeinformation
	 * @return Potentielle Zentraldatenverteiler
	 */
	long[] getPotentialCentralDistributors(BaseSubscriptionInfo baseSubscriptionInfo);

	/**
	 * Gibt die beste Verbindung zum angegebenen (nicht notwendigerweise direkt verbundenen) Datenverteiler zurück
	 * @return die beste Verbindung zum angegebenen Datenverteiler
	 */
	T_T_HighLevelCommunicationInterface getBestConnectionToRemoteDav(long remoteDav);

	/**
	 * Informiert die Anmeldelisten, dass dieser Datenverteiler Zentraldatenverteiler für die angegebene Anmeldung geworden ist
	 * @param baseSubscriptionInfo Anmeldung
	 */
	void updateListsNewLocalSubscription(final BaseSubscriptionInfo baseSubscriptionInfo);

	/**
	 * Informiert die Anmeldelisten, dass dieser Datenverteiler nicht mehr Zentraldatenverteiler für die angegebene Anmeldung ist
	 * @param baseSubscriptionInfo Anmeldung
	 */
	void updateListsRemovedLocalSubscription(final BaseSubscriptionInfo baseSubscriptionInfo);

	/**
	 * Gibt den SubscriptionsManager zurück
	 * @return den SubscriptionsManager
	 */
	HighLevelSubscriptionsManager getSubscriptionsManager();
}
