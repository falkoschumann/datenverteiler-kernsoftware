/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;

/**
 * Abstrakte Klasse, die gemeinsame Funktionalitäten von {@link de.bsvrz.dav.dav.util.accessControl.OldUserInfo} und {@link
 * de.bsvrz.dav.dav.util.accessControl.ExtendedUserInfo} beinhaltet und, da sie {@link de.bsvrz.dav.dav.util.accessControl.DataLoader} erweitert, auch für das
 * laden der BenutzerParameter (also die referenzierten Berechtigungsklassen) verantwortlich ist.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
abstract class AbstractUserInfo extends DataLoader implements UserInfoInternal {

	protected static final String USER_ASPECT_PID = "asp.parameterSoll";

	/** Zählt wie oft ein UserInfo-Objekt referenziert wurde. */
	private int _referenceCounter = 1;

	/**
	 * Initialisiert die BenutzerInfo-Klasse, indem einem DataLoader Aspekt und Attributgruppe übergeben wird um die BenutzerParameter (Berechtigungsklassen) zu
	 * laden.
	 *
	 * @param accessControlManager
	 * @param connection Verbindung zum Datenverteiler
	 * @param userAttributeGroupPid
	 */
	public AbstractUserInfo(final AccessControlManager accessControlManager, ClientDavInterface connection, final String userAttributeGroupPid) {
		super(connection, userAttributeGroupPid, USER_ASPECT_PID, accessControlManager.getUpdateLock());
	}

	@Override
	public final void incrementReference() {
		++_referenceCounter;
	}

	@Override
	public final void decrementReference() {
		--_referenceCounter;
	}

	@Override
	public final boolean canBeSafelyDeleted() {
		return _referenceCounter <= 0;
	}

	/**
	 * Wird aufgerufen, wenn sich die BenutzerParameter für den aktuellen Benutzer ändern.
	 *
	 * @param data Datenobjekt mit den Daten der Attributgruppe atg.benutzerParameter für den aktuellen Benutzer.
	 */
	@Override
	protected abstract void update(final Data data);
}

