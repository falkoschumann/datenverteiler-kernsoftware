/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.impl.config.AttributeGroupUsageIdentifications;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.dav.util.accessControl.AccessControlManager;
import de.bsvrz.dav.dav.util.accessControl.UserAction;
import de.bsvrz.dav.dav.util.accessControl.UserInfo;
import de.bsvrz.sys.funclib.debug.Debug;

import static de.bsvrz.dav.dav.main.ServerDavParameters.UserRightsChecking.NewDataModel;

/**
 * Diese Klasse regelt die Rechteprüfung für Datenanmeldungen der Benutzer(DAV / DAF) und bindet dazu den {@link
 * de.bsvrz.dav.dav.util.accessControl.AccessControlManager} in den {@link de.bsvrz.dav.dav.main.ConnectionsManager} ein.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10793 $
 */
public class AuthentificationManager {

	private static final Debug _debug = Debug.getLogger();

	/** Der Verbindungsmanager */
	private final ConnectionsManager _connectionsManager;

	private final boolean _debugDenialEnabled;

	private volatile AccessControlManager _accessControlManager;

	AuthentificationManager(ConnectionsManager connectionsManager) {
		_connectionsManager = connectionsManager;
		final String property = System.getProperty("dav.debug.zugriffsrechte");
		_debugDenialEnabled = property != null && property.trim().toLowerCase().equals("ja");
		if(_debugDenialEnabled) _debug.info("Debugausgaben für fehlgeschlagene Rechteprüfung aktiviert");
	}

	/**
	 * Fügt eine Benutzerinformation zu der Benutzertabelle hinzu, wenn der Datenverteiler die Benutzerrechte prüfen soll. Existiert der Benutzer bereits, wird
	 * lediglich die interne Referenz inkrementiert.
	 *
	 * @param userId BenutzerID
	 */
	final synchronized void addUser(long userId) {
		if(_connectionsManager.getServerDavParameters().isUserRightsCheckingEnabled()) {
			if(_accessControlManager == null) throw new IllegalStateException("AccessControlManager wurde noch nicht initialisiert");
			_accessControlManager.addUser(userId);
		}
	}

	/**
	 * Löscht einen Benutzer aus der Benutzertabelle, wenn der Datenverteiler die Benutzerrechte prüfen soll. Wenn die interne Referenz eines Benutzers 0 ist, dann
	 * wird die Benutzerinformation aus der Tabelle enfernt.
	 *
	 * @param userId BenutzerID
	 */
	final synchronized void removeUser(long userId) {
		if(_connectionsManager.getServerDavParameters().isUserRightsCheckingEnabled()) {
			if(_accessControlManager == null) throw new IllegalStateException("AccessControlManager wurde noch nicht initialisiert");
			_accessControlManager.removeUser(userId);
		}
	}

	/**
	 * Überprüft, ob die Applikation oder der Datenverteiler mit der Id berechtigt ist diese Aktion durchzuführen.
	 *
	 * @param userId die Id der Interressanten (Applikation oder Datenverteiler)
	 * @param info   Die Basisanmeldeinformationen
	 * @param action Art der Anmeldung
	 *
	 * @return <ul> <li>true: Die Aktion ist berechtigt</li> <li>false: Die Aktion ist nicht berechtigt</li> </ul>
	 */
	final boolean isActionAllowed(long userId, BaseSubscriptionInfo info, UserAction action) {
		if(!_connectionsManager.getServerDavParameters().isUserRightsCheckingEnabled()) {
			return true;
		}
		// Konfiguration oder Parametrierung oder Interne Applikation der Datenverteiler beim lokalen Betrieb
		if(userId == 0 || userId == _connectionsManager.getTransmitterId()) {
			return true;
		}

		if(AttributeGroupUsageIdentifications.isConfigurationRequest(info.getUsageIdentification())) {
			if(action == UserAction.SENDER) {
				return true;
			}
//			debugDenial("Applikation darf Konfigurationsanfragen nicht als Sender anmelden", userId, info, action);
//			return false;
		}

		// Applikation darf Konfigurationsantworten als Senke anmelden
		if(AttributeGroupUsageIdentifications.isConfigurationReply(info.getUsageIdentification())) {
			if(action == UserAction.DRAIN) {
				return true;
			}
//			debugDenial("Applikation darf Konfigurationsantworten nicht als Senke anmelden", userId, info, action);
//			return false;
		}

		// Andere Anmeldungen
		if(_accessControlManager == null) throw new IllegalStateException("AccessControlManager wurde noch nicht initialisiert");
		UserInfo userInfo = _accessControlManager.getUser(userId);
		if(userInfo == null) {
			debugDenial("Benutzer nicht bekannt", userId, info, action);
			return false;
		}
		final boolean actionPermitted = userInfo.maySubscribeData(info, action);
		if(!actionPermitted) debugDenial("Benutzer hat nicht die notwendigen Rechte", userId, info, action);
		return actionPermitted;
	}

	private void debugDenial(final String message, final long userId, final BaseSubscriptionInfo info, final UserAction action) {
		if(_debugDenialEnabled) {
			_debug.warning("Rechteprüfung fehlgeschlagen, " + message + ", BenutzerID: " + userId + ", " + info + ", " + action);
		}
	}

	/**
	 * Diese Methode wird für automatisierte JUnit-Tests benötigt und gibt zu einer Benutzer Id die UserInfo zurück.
	 *
	 * @param userId Id, die das Objekt referenziert, das einen Benutzer darstellt.
	 *
	 * @return UserInfo des Benutzer oder <code>null</code>, falls zu der Id kein Objekt gefunden werden konnte.
	 */
	public UserInfo getUserInfo(final long userId) {
		if(_accessControlManager == null) throw new IllegalStateException("AccessControlManager wurde noch nicht initialisiert");
		return _accessControlManager.getUser(userId);
	}


	/**
	 * Initialisiert den {@link de.bsvrz.dav.dav.util.accessControl.AccessControlManager} und gibt diesen zurück
	 *
	 * @param davConnection Datenverteilerverbindung
	 *
	 * @return AccessControlManager
	 */
	public synchronized AccessControlManager initializeAccessControlManager(final ClientDavInterface davConnection) {
		return _accessControlManager = new AccessControlManager(
				davConnection, _connectionsManager, false, _connectionsManager.getServerDavParameters().getUserRightsChecking() == NewDataModel
		);
	}

	@Override
	public String toString() {
		return "AuthentificationManager{" + "_accessControlManager=" + _accessControlManager + ", _connectionsManager=" + _connectionsManager + '}';
	}
}
