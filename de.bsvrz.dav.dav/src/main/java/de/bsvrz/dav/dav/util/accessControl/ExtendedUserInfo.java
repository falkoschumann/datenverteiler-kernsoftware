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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.ObjectSetType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.*;

/**
 * Kapselt für die Rechteverwaltung einen Benutzer und dessen Berechtigungsklassen. Diese Klasse wird von dem neuen Datenmodell verwendet, bei dem jeder Benutzer
 * mehrere Berechtigungsklassen zugewiesen bekommen kann. Andernfalls wird {@link de.bsvrz.dav.dav.util.accessControl.OldUserInfo} verwendet.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
class ExtendedUserInfo extends AbstractUserInfo {

	protected static final String USER_ATTRIBUTE_GROUP_PID = "atg.berechtigungsklassen";

	/** Liste mit den Berechtigungsklassen, denen der Benutzer angehört. */
	private final List<AccessControlUnit> _accessControlUnits = Collections.synchronizedList(new ArrayList<AccessControlUnit>());

	private final DataModel _dataModel;

	private final AccessControlManager _accessControlManager;

	private final long _userId;

	private final SystemObject _user;

	/**
	 * Erstellt eine neue ExtendedUserInfo-Klasse. Sollte nur im AccessControlManager benutzt werden.
	 * @param userId ID des Benutzers
	 * @param connection Verbindung zum Datenverteiler
	 * @param accessControlManager Klasse, die die Rechtesteuerungsklassen verwaltet
	 */
	ExtendedUserInfo(
			final long userId, final ClientDavInterface connection, final AccessControlManager accessControlManager) {
		super(accessControlManager, connection, USER_ATTRIBUTE_GROUP_PID);
		_userId = userId;

		_user = connection.getDataModel().getObject(_userId);

		_dataModel = connection.getDataModel();
		_accessControlManager = accessControlManager;

		// DataListener erst starten, wenn die Objekte alle da sind
		startDataListener(_user);
	}

	/**
	 * Gibt den referenzierten Benutzer als Systemobjekt zurück
	 * @return den referenzierten Benutzer
	 */
	public final SystemObject getUser() {
		return _user;
	}

	/** Gibt die ID des Benutzers zurück */
	@Override
	public final long getUserId() {
		return _userId;
	}

	@Override
	protected void update(final Data data) {
		_writeLock.lock();
		try{
			_accessControlUnits.clear();
			if(data != null){
				final Data.ReferenceArray authenticationClasses = data.getReferenceArray("Berechtigungsklassen");
				for(int i = 0; i < authenticationClasses.getLength(); i++) {
					final Data.ReferenceValue authenticationClass = authenticationClasses.getReferenceValue(i);
					_accessControlUnits.add(_accessControlManager.getAuthenticationClass(authenticationClass.getSystemObject()));
				}
			}
			_accessControlManager.userChanged(this) ;
		}
		finally {
			_writeLock.unlock();
		}
	}

	@Override
	protected List<DataLoader> getChildObjects() {
		return new ArrayList<DataLoader>(_accessControlUnits);
	}

	@Override
	public void deactivateInvalidChild(final DataLoader node) {
		// Implementierung nicht notwendig, Benutzer können sich nicht rekursiv referenzieren
		throw new UnsupportedOperationException("removeInvalidChild nicht implementiert");
	}

	@Override
	public boolean maySubscribeData(final BaseSubscriptionInfo info, final byte state) {
		final UserAction action;
		switch(state) {
			case 0:
				action = UserAction.SENDER;
				break;
			case 1:
				action = UserAction.RECEIVER;
				break;
			case 2:
				action = UserAction.SOURCE;
				break;
			case 3:
				action = UserAction.DRAIN;
				break;
			default:
				_debug.warning("Unbekannte Aktion: " + state);
				return false;
		}
		return maySubscribeData(info, action);
	}
	
	@Override
	public boolean maySubscribeData(final BaseSubscriptionInfo info, final UserAction action) {
		final long id = info.getObjectID();
		final SystemObject object = _dataModel.getObject(id);
		if(object == null){
			_debug.warning("Unbekanntes Objekt: " + id);
			return false;
		}
		final AttributeGroupUsage atgUsage = _dataModel.getAttributeGroupUsage(info.getUsageIdentification());
		if(atgUsage == null){
			_debug.warning("Unbekannte Attributgruppenverwendung: " + info.getUsageIdentification());
			return false;
		}		
		final AttributeGroup attributeGroup = atgUsage.getAttributeGroup();
		final Aspect aspect = atgUsage.getAspect();
		return maySubscribeData(object, attributeGroup, aspect, action);
	}

	@Override
	public boolean maySubscribeData(final SystemObject object, final AttributeGroup attributeGroup, final Aspect aspect, final UserAction action) {
		if(object == null) throw new IllegalArgumentException("object ist null");
		if(attributeGroup == null) throw new IllegalArgumentException("attributeGroup ist null");
		if(aspect == null) throw new IllegalArgumentException("aspect ist null");
		if(!isInitialized()) waitForInitialization();
		if(!getSystemObject().isValid()) return false; // das Benutzerobjekt kann zwischenzeitlich gelöscht worden sein
		_readLock.lock();
		try{
			// Falls die Rechte implizit erlaubt sind, erlauben wir die Aktion
			if(ImplicitAccessUnitManager.isAllowed(attributeGroup.getPid(), aspect.getPid(), action)) return true;

			// Sofern mindestens eine vergebene Berechtigungsklasse die Aktion erlaubt, erlauben wir dem Benutzer die Aktion.
			for(final AccessControlUnit authenticationClass : _accessControlUnits) {
				if(authenticationClass.isAllowed(object, attributeGroup, aspect, action)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public boolean mayCreateModifyRemoveObject(final ConfigurationArea area, final SystemObjectType type) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try{
			// Sofern mindestens eine vergebene Berechtigungsklasse die Aktion erlaubt, erlauben wir dem Benutzer die Aktion.
			for(final AccessControlUnit authenticationClass : _accessControlUnits) {
				if(authenticationClass.isObjectChangeAllowed(area, type)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public boolean mayModifyObjectSet(final ConfigurationArea area, final ObjectSetType type) {
		if(!isInitialized()) waitForInitialization();
		_readLock.lock();
		try{
			for(final AccessControlUnit authenticationClass : _accessControlUnits) {
				if(authenticationClass.isObjectSetChangeAllowed(area, type)) return true;
			}
			return false;
		}
		finally {
			_readLock.unlock();
		}
	}

	@Override
	public String toString() {
		return getUser().getPidOrId();
	}
}
