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
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ObjectSetType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.*;

/**
 * Kapselt eine Berechtigungsklasse aus dem Datenmodell
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
class AccessControlUnit extends DataLoader {

	private final List<RoleRegionPair> _roleRegionPairs = new ArrayList<RoleRegionPair>();

	private static final String ATG_ROLE_REGION_PAIRS = "atg.rollenRegionenPaare";

	private static final String ASPECT_PID = "asp.parameterSoll";

	private final AccessControlManager _accessControlManager;

	/**
	 * Erstellt eine neue Klasse, die eine Berechtigungsklasse verwaltet. Sollte außer in Ausnahmen nur im AccessControlManager aufgerufen werden, um unnötige
	 * Instanzen zu vermeiden
	 *
	 * @param systemObject         Das Systemobjekt, das die Berechtigungsklasse symbolisiert
	 * @param connection           Verbindung zum datenverteiler
	 * @param accessControlManager AccessControlManager
	 */
	public AccessControlUnit(
			final SystemObject systemObject, final ClientDavInterface connection, final AccessControlManager accessControlManager) {
		super(connection, ATG_ROLE_REGION_PAIRS, ASPECT_PID, accessControlManager.getUpdateLock());
		_accessControlManager = accessControlManager;
		startDataListener(systemObject);
	}

	@Override
	protected void update(final Data data) {
		_writeLock.lock();
		try {
			_roleRegionPairs.clear();
			if(data != null) {
				final Data.Array pairs = data.getArray("RollenRegionenPaare");
				for(int i = 0; i < pairs.getLength(); i++) {
					final Data pair = pairs.getItem(i);
					final SystemObject role = pair.getReferenceValue("Rolle").getSystemObject();
					final SystemObject region = pair.getReferenceValue("Region").getSystemObject();
					_roleRegionPairs.add(new RoleRegionPair(_accessControlManager.getRole(role), _accessControlManager.getRegion(region)));
				}
			}
			_accessControlManager.objectChanged(this);
		}
		finally {
			_writeLock.unlock();
		}
	}

	/**
	 * Gibt eine Liste mit den referenzierten Rollen und Regionen zurück
	 *
	 * @return eine Liste mit den referenzierten Rollen und Regionen
	 */
	@Override
	protected List<DataLoader> getChildObjects() {
		_readLock.lock();
		try {
			final List<DataLoader> list = new ArrayList<DataLoader>();
			for(final RoleRegionPair roleRegionPair : _roleRegionPairs) {
				list.add(roleRegionPair.getRole());
				list.add(roleRegionPair.getRegion());
			}
			return list;
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public void deactivateInvalidChild(final DataLoader node) {
		// Implementierung nicht notwendig, Berechtigungsklassen können sich nicht rekursiv referenzieren
		throw new UnsupportedOperationException("removeInvalidChild nicht implementiert");
	}

	/**
	 * Prüft den Berechtigungsstatus für eine angegebene Datenanmeldung
	 *
	 * @param object Objekt auf das Daten angemeldet werden sollen
	 * @param atg    Attributgruppe
	 * @param asp    Aspekt
	 * @param action Art der Datenanmeldung
	 *
	 * @return true wenn der Vorgang erlaubt ist
	 */
	public final boolean isAllowed(final SystemObject object, final AttributeGroup atg, final Aspect asp, final UserAction action) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try {

			for(final RoleRegionPair roleRegionPair : _roleRegionPairs) {
				if(roleRegionPair.getPermission(object, atg, asp, action)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	/**
	 * Prüft, ob ein Objekt im angegebenen Konfigurationsbereich mit dem angegeben Typ erstellt, bearbeitet oder gelöscht werden darf.
	 *
	 * @param area Konfigurationsbereich
	 * @param type Objekttyp
	 *
	 * @return true wenn Vorgang erlaubt
	 */
	public final boolean isObjectChangeAllowed(final ConfigurationArea area, final SystemObjectType type) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try {
			for(final RoleRegionPair roleRegionPair : _roleRegionPairs) {
				if(roleRegionPair.getPermissionObjectChange(area, type)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	/**
	 * Prüft, ob eine Menge im angegebenen Konfigurationsbereich mit dem angegeben Typ verändert werden darf.
	 *
	 * @param area Konfigurationsbereich
	 * @param type Mengentyp
	 *
	 * @return true wenn Vorgang erlaubt
	 */
	public final boolean isObjectSetChangeAllowed(final ConfigurationArea area, final ObjectSetType type) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try {
			for(final RoleRegionPair roleRegionPair : _roleRegionPairs) {
				if(roleRegionPair.getPermissionObjectSetChange(area, type)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	/** Stellt ein Rolle-Region-Paar dar */
	private static class RoleRegionPair {

		private final Role _role;

		private final Region _region;

		public Role getRole() {
			return _role;
		}

		public Region getRegion() {
			return _region;
		}

		public RoleRegionPair(final Role role, final Region region) {
			_region = region;
			_role = role;
		}

		/**
		 * Prüft den Berechtigungsstatus für eine angegebene Datenanmeldung
		 *
		 * @param object Objekt auf das Daten angemeldet werden sollen
		 * @param atg    Attributgruppe
		 * @param asp    Aspekt
		 * @param action Art der Datenanmeldung
		 *
		 * @return <ul> <li>{@link  Role.PermissionState#IMPLICIT_FORBIDDEN} wenn keine Aussage gemacht werden kann</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_ALLOWED} wenn die Aktion von dieser Rolle/Region erlaubt wird</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_FORBIDDEN} wenn die Aktion von dieser Rolle/Region explizit verboten wird</li> </ul>
		 */
		private boolean getPermission(final SystemObject object, final AttributeGroup atg, final Aspect asp, final UserAction action) {
			if(!_region.contains(object)) return false;
			return _role.getPermission(atg, asp, action) == Role.PermissionState.EXPLICIT_ALLOWED;
		}

		/**
		 * Prüft den Berechtigungsstatus für die Erstellung/Veränderung/Löschung von Objekten
		 *
		 * @param area Konfigurationsbereich
		 * @param type Objekttyp
		 *
		 * @return <ul> <li>{@link Role.PermissionState#IMPLICIT_FORBIDDEN} wenn keine Aussage gemacht werden kann</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_ALLOWED} wenn die Aktion von dieser Rolle/Region erlaubt wird</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_FORBIDDEN} wenn die Aktion von dieser Rolle/Region explizit verboten wird</li> </ul>
		 */
		public boolean getPermissionObjectChange(final ConfigurationArea area, final SystemObjectType type) {
			return _role.getPermissionObjectChange(area, type) == Role.PermissionState.EXPLICIT_ALLOWED;
		}

		/**
		 * Prüft den Berechtigungsstatus für die Veränderung von Mengen
		 *
		 * @param area Konfigurationsbereich
		 * @param type Objekttyp
		 *
		 * @return <ul> <li>{@link Role.PermissionState#IMPLICIT_FORBIDDEN} wenn keine Aussage gemacht werden kann</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_ALLOWED} wenn die Aktion von dieser Rolle/Region erlaubt wird</li> <li>{@link
		 *         Role.PermissionState#EXPLICIT_FORBIDDEN} wenn die Aktion von dieser Rolle/Region explizit verboten wird</li> </ul>
		 */
		public boolean getPermissionObjectSetChange(final ConfigurationArea area, final ObjectSetType type) {
			return _role.getPermissionObjectSetChange(area, type) == Role.PermissionState.EXPLICIT_ALLOWED;
		}

		@Override
		public String toString() {
			return "RoleRegionPair{" + "_role=" + _role + ", _region=" + _region + '}';
		}
	}
}

