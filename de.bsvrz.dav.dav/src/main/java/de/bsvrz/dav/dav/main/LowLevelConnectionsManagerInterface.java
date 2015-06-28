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

import de.bsvrz.dav.daf.communication.lowLevel.AuthentificationProcess;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.impl.config.telegrams.TransmitterConnectionInfo;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunication;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunication;

import java.util.Collection;

/**
 * Interface für die Verwaltung der Verbindungen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11475 $
 */
public interface LowLevelConnectionsManagerInterface {

	/**
	 * Gibt die eigene Datenverteiler-id zurück
	 * @return die eigene Datenverteiler-id
	 */
	long getTransmitterId();

	/**
	 * @param error   True: ein Fehler ist aufgetreten
	 * @param message genauere Beschreibung des Fehlers
	 */
	void shutdown(boolean error, String message);

	/**
	 * Gibt die eigene ClientDavConnection zurück
	 * @return die eigene ClientDavConnection
	 */
	SelfClientDavConnection getSelfClientDavConnection();

	/**
	 * Gibt das eigene Datenverteiler-Objekt zurück
	 * @return das eigene Datenverteiler-Objekt
	 */
	SystemObject getMyTransmitter();

	/**
	 * entfernt die angegebene Verbindung
	 * @param connection
	 */
	void removeConnection(T_A_HighLevelCommunication connection);
	/**
	 * entfernt die angegebene Verbindung
	 * @param connection
	 */
	void removeConnection(T_T_HighLevelCommunication connection);

	/**
	 * Gibt die Pid der lokalen Konfiguration zurück
	 * @return die Pid der lokalen Konfiguration
	 */
	String getLocalModeConfigurationPid();

	/**
	 * Gibt die Id der lokalen Konfiguration zurück
	 * @return die Id der lokalen Konfiguration
	 */
	long getLocalModeConfigurationId();

	LowLevelAuthenticationInterface getLowLevelAuthentication();

	/**
	 * Setzt die Parameter für eien lokale Konfiguration
	 * @param configurationPid Pid der Konfiguration
	 * @param configurationId Id der Konfiguration
	 */
	void setLocalModeParameter(String configurationPid, long configurationId);

	/**
	 * Wird aufgerufen, wenn die lokale konfiguration verfügbar ist
	 */
	void setLocalConfigurationAvailable();

	/**
	 * Gibt die ServerDavParameters zurück
	 * @return die ServerDavParameters
	 */
	ServerDavParameters getServerDavParameters();

	/**
	 * Gibt die ClientDavParameters für die lokale Dav-Applikation zurück
	 * @return die ClientDavParameters
	 */
	ClientDavParameters getClientDavParameters();

	/**
	 * Gibt die angegebene Dav-Dav-Verbindung zurück
	 * @param transmitterId Id den verbundenen Transmitters
	 * @return Existierende Verbindung mit dieser Id oder null falls nicht vorhanden
	 */
	T_T_HighLevelCommunication getTransmitterConnection(long transmitterId);
	/**
	 * Gibt die angegebene Applikationsverbindung zurück
	 * @param applicationId Id der verbundenen Applikation
	 * @return Existierende Verbindung mit dieser Id oder null falls nicht vorhanden
	 */
	T_A_HighLevelCommunication getApplicationConnection(long applicationId);
	/**
	 * Diese Methode wird von der Protokollsteuerung aufgerufen, um einer Verbindung ein Gewicht zuzuweisen. Die Information wird von der Wegverwaltung benutzt,
	 * wenn eine Verbindung bewertet wird.
	 *
	 * @param transmitterId ID des DAV
	 *
	 * @return Gewichtung der Verbindung
	 */
	short getWeight(long transmitterId);

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung von diesem Datenverteiler zum angegebenen Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	TransmitterConnectionInfo getTransmitterConnectionInfo(long connectedTransmitterId);

	/**
	 * Bestimmt die Verbindungsinformationen für eine Verbindung vom angegebenen Datenverteiler zu diesem Datenverteiler.
	 *
	 * @param connectedTransmitterId ID des DAV
	 *
	 * @return Verbindungsinformationen
	 */
	TransmitterConnectionInfo getRemoteTransmitterConnectionInfo(long connectedTransmitterId);

	/**
	 * Wird aufgerufen, sobald die ID einer Verbindung bekannt ist
	 * @param communication Verbindung
	 */
	void updateApplicationId(T_A_HighLevelCommunication communication);
	/**
	 * Wird aufgerufen, sobald die ID einer Verbindung bekannt ist
	 * @param communication Verbindung
	 */
	void updateTransmitterId(T_T_HighLevelCommunication communication);

	/**
	 * Gibt alle Appliaktionsverbindungen zurück
	 * @return alle Appliaktionsverbindungen
	 */
	Collection<T_A_HighLevelCommunication> getApplicationConnections();

	/**
	 * Gibt alle Datenverteilerverbindungen zurück
	 * @return alle Datenverteilerverbindungen
	 */
	Collection<T_T_HighLevelCommunication> getTransmitterConnections();

	/**
	 * Gibt <tt>true</tt> zurück, wenn der Datenverteiler sich gerade beendet
	 * @return <tt>true</tt>, wenn der Datenverteiler sich gerade beendet, sonst <tt>false</tt>
	 */
	boolean isClosing();

	/**
	 * Loggt einen Benutzer ein, bzw. prüft die übergebenen Daten.
	 * @param userName Benutzername
	 * @param userPassword Benutzerpasswort
	 * @param authentificationText
	 * @param authentificationProcess
	 * @param applicationTypePid Applikations-Typ-Pid
	 * @return Benutzerid, falls erfolgreich eingeloggt, sonst -1. (0 ist der spezielle Benutzer für Datenverteiler und Konfiguration)
	 */
	long login(String userName, byte[] userPassword, String authentificationText, AuthentificationProcess authentificationProcess, String applicationTypePid);
}
