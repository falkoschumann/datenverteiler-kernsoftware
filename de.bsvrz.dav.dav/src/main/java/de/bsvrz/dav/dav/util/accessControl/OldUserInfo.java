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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.dav.daf.communication.dataRepresentation.AttributeValue;
import de.bsvrz.dav.daf.communication.dataRepresentation.datavalue.ByteAttribute;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.BaseSubscriptionInfo;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.dav.daf.main.config.ObjectSet;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;

import java.util.*;

/**
 * Verwaltet die Rechte eines Benutzers.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12975 $
 */
public class OldUserInfo extends AbstractUserInfo {

	protected static final String USER_ATTRIBUTE_GROUP_PID = "atg.benutzerParameter";

	private static final String ROLE_ACTIVITIES_SET_NAME = "Aktivitäten";

	private static final String ACTIVITY_ATTRIBUTE_GROUP_SET_NAME = "Attributgruppen";

	private static final String ACTIVITY_ASPECT_SET_NAME = "Aspekte";

	private static final String ACTIVITY_RIGHTS_ATTRIBUTE_GROUP_PID = "atg.zugriffsRechte";

	private static final String REGION_OBJECT_SET_NAME = "Objekte";

	private static final String REGION_CONTAINER_SET_NAME = "Zusammenstellungen";

	private static final String AUTHENTIFICATION_CLASS_ATTRIBUTE_GROUP_PID = "atg.rollenRegionenPaareParameter";

	private static final String AUTHENTIFICATION_CLASS_ASPECT_PID = "asp.parameterSoll";

	/**
	 * Damit die Software auch mit älteren Versionen des Datenmodells arbeitet, wird hier neben dem korrekten Namen der Menge auch der ursprüngliche, falsch
	 * geschriebene Name der Menge unterstützt.
	 */
	private static final String REGION_CONTAINER_SET_BAD_NAME = "Zusammmenstellungen";

	private static final String REGION_TYPE_PID = "typ.zugriffsRegion";

	private static final String CONFIGURATION_SUBSET_TYPE_PID = "typ.konfigurationsBereich";

	private static final String CONFIGURATION_SUBSET_OBJECTS_SET_NAME = "Objekte";

	/** Der Verbindungsmanager */
	private UserRightsChangeHandler _userRightsChangeHandler;

	private ArrayList<AuthenticationUnit> _authentificationUnits;

	/** Erster Durchlauf */
	private boolean _firstTime = true;

	/** Eine interne Klasse zur Aktualisierung der Benutzerrechten */
	private AuthenticationClassUpdater _updater;

	private long _userId;

	private SystemObject _user;

	/**
	 * * @param userId              Id des Benutzers. Über die Id wird das Objekt des Benutzers vom Datenverteiler angefordert.
	 * @param connection         Verbindung zum Datenverteiler
	 * @param userRightsChangeHandler Wird zum an/abmelden von Daten benutzt.    @deprecated Klasse wurde durch {@link ExtendedUserInfo} ersetzt, wird aber noch bei alten Datenmodell-Versionen verwendet
	 * @param accessControlManager
	 */
	@Deprecated
	public OldUserInfo(
			long userId, ClientDavInterface connection, UserRightsChangeHandler userRightsChangeHandler, final AccessControlManager accessControlManager) {
		super(accessControlManager, connection, USER_ATTRIBUTE_GROUP_PID);
		_userRightsChangeHandler = userRightsChangeHandler;
		_authentificationUnits = new ArrayList<AuthenticationUnit>();

		_userId = userId;

		_user = connection.getDataModel().getObject(_userId);

		startDataListener(_user);
	}

	public final SystemObject getUser() {
		return _user;
	}

	/** Id des Benutzers */
	@Override
	public final long getUserId() {
		return _userId;
	}

	void updateAuthenticationUnit(
			final Data data) {
		try {
			final Data.Array roleRegionPairs = data.getArray("rollenRegionenPaare");
			final int numberOfPairs = roleRegionPairs.getLength();
			for(int i = 0; i < numberOfPairs; ++i) {
				final Data roleRegionPair = roleRegionPairs.getItem(i);
				ConfigurationObject role = (ConfigurationObject)roleRegionPair.getReferenceValue("rolle").getSystemObject();
				ConfigurationObject region = (ConfigurationObject)roleRegionPair.getReferenceValue("region").getSystemObject();
				if((role != null) && (region != null)) {
					Region regionWrapper = new Region(getRegionObjects(region));
					Activity[] activities = getRoleActivities(role);
					AuthenticationUnit unit = new AuthenticationUnit(regionWrapper, activities);
					_authentificationUnits.add(unit);
				}
			}
		}
		catch(ConfigurationException ex) {
			ex.printStackTrace();
		}
		finally {
			synchronized(_userRightsChangeHandler) {
				_userRightsChangeHandler.notifyAll();
				if(_firstTime) {
					_firstTime = false;
				}
				else {
					try {
						_userRightsChangeHandler.handleUserRightsChanged(getUserId());
					}
					catch(Exception e) {
						_debug.warning("Fehler beim Ändern der Zugriffsrechte des Benutzers " + getUser(), e);
					}
					finally {
						_debug.info("Zugriffsrechte des Benutzers " + getUser() + " haben sich geändert.");
					}
				}
			}
		}
	}

	private static boolean isValidResult(final ResultData resultData) {
		if(resultData == null) {
			return false;
		}
		if(!resultData.hasData()) {
			return false;
		}
		return true;
	}

	@Override
	public final boolean maySubscribeData(BaseSubscriptionInfo info, byte state) {
		// Die folgenden 2 Zeilen werden gebraucht, damit der Dav nicht in der folgenden Schleife hängenbleibt.
		// Mit dem alten Code war das kein größeres Problem, da damals _initComplete auch ohne Daten true war.
		waitForInitialization();
		if(getDataState() != DataState.DATA) return false;

		if(_updater != null) {
			_updater.waitForInitialization();
		}

		long id = info.getObjectID();
		AttributeGroupUsage atgUsage = getConnection().getDataModel().getAttributeGroupUsage(info.getUsageIdentification());
		if(atgUsage == null) return false;
		final AttributeGroup attributeGroup = atgUsage.getAttributeGroup();
		final Aspect aspect = atgUsage.getAspect();

		long attributeGroupId = attributeGroup.getId();
		long aspectId = aspect.getId();

		for(int i = _authentificationUnits.size() - 1; i > -1; --i) {
			AuthenticationUnit unit = _authentificationUnits.get(i);
			if(unit != null) {
				if(unit.isAllowed(id, attributeGroupId, aspectId, state)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean maySubscribeData(final SystemObject object, final AttributeGroup attributeGroup, final Aspect aspect, final UserAction action) {
		// Die folgenden 2 Zeilen werden gebraucht, damit der Dav nicht in der folgenden Schleife hängenbleibt.
		// Mit dem alten Code war das kein größeres Problem, da damals _initComplete auch ohne Daten true war.
		waitForInitialization();
		if(getDataState() != DataState.DATA) return false;

		if(_updater != null) {
			_updater.waitForInitialization();
		}

		long id = object.getId();
		long attributeGroupId = attributeGroup.getId();
		long aspectId = aspect.getId();
		byte state;

		if(action == UserAction.SENDER){
			state = 0;
		}else if(action == UserAction.RECEIVER){
			state = 1;
		}else if(action == UserAction.SOURCE){
			state = 2;
		}else if(action == UserAction.DRAIN){
			state = 3;
		}else{
			throw new IllegalArgumentException("Unbekannte Aktion: " + action);
		}


		for(int i = _authentificationUnits.size() - 1; i > -1; --i) {
			AuthenticationUnit unit = _authentificationUnits.get(i);
			if(unit != null) {
				if(unit.isAllowed(id, attributeGroupId, aspectId, state)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean maySubscribeData(final BaseSubscriptionInfo info, final UserAction action) {
		byte state;

		if(action == UserAction.SENDER){
			state = 0;
		}else if(action == UserAction.RECEIVER){
			state = 1;
		}else if(action == UserAction.SOURCE){
			state = 2;
		}else if(action == UserAction.DRAIN){
			state = 3;
		}else{
			throw new IllegalArgumentException("Unbekannte Aktion: " + action);
		}
		return maySubscribeData(info, state);
	}

	@Override
	public boolean mayCreateModifyRemoveObject(final ConfigurationArea area, final SystemObjectType type) {
		// Die alte Benutzerverwaltung erlaubt kein Beschränken von Objekt-Aktionen
		return true;
	}

	@Override
	public boolean mayModifyObjectSet(final ConfigurationArea area, final ObjectSetType type) {
		// Die alte Benutzerverwaltung erlaubt kein Beschränken von Veränderungen an Mengen
		return true;
	}

	public final int hashCode() {
		int result = 19;
		long id = getUser().getId();
		result = (11 * result) + (int)(id ^ (id >>> 32));
		return result;
	}

	public final boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		if(obj instanceof OldUserInfo) {
			OldUserInfo ui = (OldUserInfo)obj;
			if(getUser().getId() == ui.getUser().getId()) {
				return true;
			}
		}
		return false;
	}

	private Activity[] getRoleActivities(ConfigurationObject role) {
		Activity[] activities = null;
		if(role != null) {
			ObjectSet activitiesSet = role.getObjectSet(ROLE_ACTIVITIES_SET_NAME);
			if(activitiesSet != null) {
				List activitiesList = activitiesSet.getElements();
				if(activitiesList != null) {
					int size = activitiesList.size();
					if(size > 0) {
						activities = new Activity[size];
						for(int i = 0; i < size; ++i) {
							ConfigurationObject activity = (ConfigurationObject)activitiesList.get(i);
							if(activity != null) {
								ObjectSet attributeGroupSet = activity.getObjectSet(ACTIVITY_ATTRIBUTE_GROUP_SET_NAME);
								ArrayList attributegroups = new ArrayList();
								if(attributeGroupSet != null) {
									List atgs = attributeGroupSet.getElements();
									if(atgs != null) {
										for(int j = atgs.size() - 1; j > -1; --j) {
											AttributeGroup atg = (AttributeGroup)atgs.get(j);
											if(atg != null) {
												attributegroups.add(new Long(atg.getId()));
											}
										}
									}
								}
								ObjectSet aspectSet = activity.getObjectSet(ACTIVITY_ASPECT_SET_NAME);
								ArrayList aspects = new ArrayList();
								if(aspectSet != null) {
									List asps = aspectSet.getElements();
									if(asps != null) {
										for(int j = asps.size() - 1; j > -1; --j) {
											Aspect asp = (Aspect)asps.get(j);
											if(asp != null) {
												aspects.add(new Long(asp.getId()));
											}
										}
									}
								}
								byte mode = 0;
								AttributeGroup rightsAttributeGroup = (AttributeGroup)getConnection().getDataModel().getObject(
										ACTIVITY_RIGHTS_ATTRIBUTE_GROUP_PID
								);
								List rightsValues = ((DafDataModel)getConnection().getDataModel()).getObjectDataValues(
										activity, rightsAttributeGroup
								);
								if(rightsValues != null) {
									byte read = ((Byte)((ByteAttribute)((AttributeValue)rightsValues.get(0)).getValue()).getValue()).byteValue();
									byte write = ((Byte)((ByteAttribute)((AttributeValue)rightsValues.get(1)).getValue()).getValue()).byteValue();
									byte main = ((Byte)((ByteAttribute)((AttributeValue)rightsValues.get(2)).getValue()).getValue()).byteValue();
									if(read == 1) {
										mode |= 0x01;
									}
									if(write == 1) {
										mode |= 0x02;
									}
									if(main == 1) {
										mode |= 0x04;
									}
									activities[i] = new Activity(attributegroups, aspects, mode);
								}
							}
						}
					}
				}
			}
		}
		return activities;
	}

	private void collectIds(List ids, List objects) {
		if(objects == null) {
			return;
		}
		int size = objects.size();
		for(int i = 0; i < size; ++i) {
			SystemObject obj = (SystemObject)objects.get(i);
			if(obj != null) {
				ids.add(new Long(obj.getId()));
			}
		}
	}

	private ArrayList getRegionObjects(ConfigurationObject region) throws ConfigurationException {
		ArrayList objectList = new ArrayList();
		if(region != null) {
			ObjectSet objectsSet = region.getObjectSet(REGION_OBJECT_SET_NAME);
			if(objectsSet != null) {
				collectIds(objectList, objectsSet.getElements());
			}
			ObjectSet setsSet = region.getObjectSet(REGION_CONTAINER_SET_NAME);
			if(setsSet == null) {
				// Damit die Software auch mit älteren Versionen des Datenmodells arbeitet, wird hier neben dem
				// korrekten Namen der Menge auch der ursprüngliche, falsch geschriebene Name der Menge unterstützt.
				setsSet = region.getObjectSet(REGION_CONTAINER_SET_BAD_NAME);
				if(setsSet != null) {
					_debug.warning(
							"In der Region " + region + " wird die Menge 'Zusammmenstellungen' mit 3 'm' verwendet. Bitte kb.systemModellGlobal "
							+ "aktualisieren und Mengenname ändern! "
					);
				}
			}
			if(setsSet != null) {
				List<SystemObject> tmp = setsSet.getElements();
				if((tmp != null) && (tmp.size() > 0)) {
					for(int i = tmp.size() - 1; i > -1; --i) {
						SystemObject o = tmp.get(i);
						if(o != null) {
							collectIds(objectList, getContainerObjects(o));
						}
					}
				}
			}
		}
		return objectList;
	}

	private List getContainerObjects(SystemObject container) throws ConfigurationException {
		if(container != null) {
			SystemObjectType type = container.getType();
			if(type instanceof ObjectSetType) {
				return ((ObjectSet)container).getElements();
			}
			if(REGION_TYPE_PID.equals(type.getPid())) {
				return getRegionObjects((ConfigurationObject)container);
			}
			if(CONFIGURATION_SUBSET_TYPE_PID.equals(type.getPid())) {
				return getConfigurationSubsetObjects((ConfigurationObject)container);
			}
		}
		return new ArrayList();
	}

	private List<SystemObject> getConfigurationSubsetObjects(SystemObject container) throws ConfigurationException {
		List<SystemObject> objectList = new ArrayList<SystemObject>();
		if(container instanceof ConfigurationArea) {
			ConfigurationArea area = (ConfigurationArea)container;
			final ArrayList<ConfigurationArea> areas = new ArrayList<ConfigurationArea>();
			areas.add(area);
			objectList.addAll(container.getDataModel().getObjects(areas, null, ObjectTimeSpecification.valid()));
		}
		return objectList;
	}


	@Override
	public void stopDataListener() {
		super.stopDataListener();
		if(_updater != null) {
			_updater.stopDataListener();
		}
	}

	@Override
	public void deactivateInvalidChild(final DataLoader node) {
		// Alte BenutzerInfo-Klasse unterstützt keine Rekursion, Implementierung nicht notwendig.
		throw new UnsupportedOperationException("removeInvalidChild nicht implementiert");
	}

	/**
	 * Speichert unterschiedliche Objekte (ATG, Aspekte, Objekte) und stellt eine Methode zur Verfügung, mit der geprüft werden kann, ob ein bestimmtes Objekt
	 * vorhanden ist.
	 */
	private class InfoHolder {

		/** true, wenn die im Konstruktor übergebene Liste null oder leer ist; false, sonst. */
		private boolean all;

		// Diese Variable sollte durch ein HashSet ersetzt werden, da nur ein "Contains" benötigt wird.

		/** Speichert die Objekte der im Konstruktor übergebenen Liste. Als Schlüssel dient das Objekt, als Value wird ebenfalls das Objekt eingetragen. */
		private Hashtable<Object, Object> table;

		public InfoHolder(ArrayList list) {
			if((list == null) || (list.size() == 0)) {
				all = true;
			}
			else {
				all = false;
				table = new Hashtable<Object, Object>();
				for(int i = list.size() - 1; i > -1; --i) {
					Object o = list.get(i);
					if(o != null) {
						table.put(o, o);
					}
				}
			}
		}

		/**
		 * @param o Objekt, das vorhanden sein soll. Wurde im Konstruktor eine leere Liste bzw. <code>null</code> übergeben.
		 *
		 * @return true, wenn a) Das übergebene Objekt in der Liste vorhanden ist, die im Konstruktor übergeben wurde. b) Im Konstruktor eine leere Liste oder
		 *         <code>null</code> übergeben wurde. In allen anderen Fällen wird <code>false</code> zurückgegeben.
		 */
		public final boolean isAllowed(Object o) {
			if(o == null) {
				return false;
			}
			if(all) {
				return true;
			}
			return (table.get(o) != null);
		}
	}

	/**
	 * Diese Klasse stellt eine Rolle/Aktivität dar. Es wird eine Methode zur Verfügung gestellt, mit der geprüft werden kann, ob eine übergebene Rolle/Aktivität
	 * erlaubt ist.
	 */
	private class Activity {

		/** read  ==> bit index 0 is set (mode & 0x01) write ==> bit index 1 is set (mode & 0x02) main  ==> bit index 2 is set (mode & 0x04) */
		private final byte _mode;

		/** Alle erlaubten ATG´s dieser Rolle. */
		private final InfoHolder _attributeGroups;

		/** Alle erlaubten Aspekte dieser Rolle. */
		private final InfoHolder _aspects;

		/**
		 * @param atgs  Alle ATG´s, die in dieser Rolle erlaubt sind. Sollen alle ATG´s erlaubt sein, so kann <code>null</code> oder eine leere Liste übergeben
		 *              werden.
		 * @param asps  Alle erlaubten Aspekte dieser Rolle. Sollen alle Aspekte erlaubt sein, so kann <code>null</code> oder eine leere Liste übergeben werden.
		 * @param _mode 0: Als Sender 1: Als Empfänger 2: Als Quelle 3: Als Senke
		 */
		public Activity(ArrayList atgs, ArrayList asps, byte _mode) {
			_attributeGroups = new InfoHolder(atgs);
			_aspects = new InfoHolder(asps);
			this._mode = _mode;
		}

		/**
		 * Prüft, ob die übergebenen Parameter mit der Rolle/Aktivität erlaubt sind.
		 *
		 * @param atg  ATG
		 * @param asp  Aspekt
		 * @param mode 0: Als Sender 1: Als Empfänger 2: Als Quelle 3: Als Senke
		 *
		 * @return <code>true</code>, wenn die übergenen Parameter mit der Rolle/Aktivität erlaubt sind, sonst <code>false</code>.
		 */
		public final boolean isAllowed(Object atg, Object asp, byte mode) {
			switch(mode) {
				case 0: {
					if((_mode & 2) != 2) {
						return false;
					}
					break;
				}
				case 1: {
					if((_mode & 1) != 1) {
						return false;
					}
					break;
				}
				case 2: {
					if((_mode & 6) != 6) {
						return false;
					}
					break;
				}
				case 3: {
					if((_mode & 5) != 5) {
						return false;
					}
					break;
				}
				default: {
					return false;
				}
			}
			return (_attributeGroups.isAllowed(atg) && _aspects.isAllowed(asp));
		}
	}

	/** Diese Klasse stellt eine Region dar und stellt eine Methode zur Verfügung, mit der die Zugehörigkeit anderer Objekte zu der Region geprüft werden kann. */
	private class Region {

		private final InfoHolder _objects;

		/**
		 * Objekte, die eine Region definieren.
		 *
		 * @param objs Alle Objekte, die zu einer Region gehören. Eine leere Liste oder <code>null</code> wird als "Wildcard" interpretiert (alles ist erlaubt).
		 */
		public Region(ArrayList objs) {
			_objects = new InfoHolder(objs);
		}

		/**
		 * Diese Methode prüft, ob ein Objekt zu einer Region gehört.
		 *
		 * @param obj Objekt, das Teil einer Region sein soll.
		 *
		 * @return <code>true</code>, wenn das übergebene Objekt in der Liste, die im Konstruktor übergeben wurde, zu finden ist. Wurde im Konstruktor eine leere
		 *         Liste oder <code>null</code> übergeben, wird immer <code>true</code> zurückgegeben. In allen anderen Fällen wird <code>false</code> zurückgegeben.
		 */
		public final boolean isAllowed(Object obj) {
			return _objects.isAllowed(obj);
		}
	}

	/**
	 * Diese Klasse stellt eine Rollen/Regionen-Paar Kombination dar. Sie stellt eine Methode zur Verfügung, mit der geprüft werden kann ob ein Objekt, ein
	 * ATG+Aspekt und ein Anmeldung als Senke/Quelle/Empfänger/Sender erlaubt ist.
	 */
	private class AuthenticationUnit {

		/** Region */
		private Region _region;

		/** Rolle/Aktivität */
		private Activity[] _activities;

		/**
		 * @param region     Region
		 * @param activities Alle Aktivitäten/Rollen. Wird eine leere Liste übergeben, wird {@link #isAllowed} immer <code>false</code> zurück geben.
		 */
		public AuthenticationUnit(Region region, Activity[] activities) {
			if(region == null) {
				throw new IllegalArgumentException("Region Argument ist null");
			}
			if(activities == null) {
				throw new IllegalArgumentException("Aktivitäten Argument ist null");
			}
			_region = region;
			_activities = activities;
		}

		public final boolean isAllowed(Object obj, Object atg, Object asp, byte mode) {
			if(!_region.isAllowed(obj)) {
				return false;
			}
			for(int i = 0; i < _activities.length; ++i) {
				if(_activities[i].isAllowed(atg, asp, mode)) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	protected void update(final Data data) {
		final ConfigurationObject authenticationClass;
		if(data != null) {
			authenticationClass = (ConfigurationObject)data.getReferenceValue("berechtigungsklasse").getSystemObject();
		}
		else {
			authenticationClass = null;
		}
		if(authenticationClass != null) {
			if(_updater == null) {
				_updater = new AuthenticationClassUpdater(authenticationClass);
			}
			else {
				if(!authenticationClass.equals(_updater.getAuthenticationClass())) {
					_updater.stopDataListener();
					_updater = new AuthenticationClassUpdater(authenticationClass);
				}
			}
		}else {
			_debug.warning("Für den Benutzer " + getUser() + " liegen keine Daten vor", USER_ATTRIBUTE_GROUP_PID);
		}
	}

	@Override
	protected List<DataLoader> getChildObjects() {
		if(_updater != null) {
			return Collections.<DataLoader>singletonList(_updater);
		}
		else {
			return Collections.emptyList();
		}
	}


	private class AuthenticationClassUpdater extends DataLoader {

		/**
		 * Meldet sich als Empfänger auf das Objekt an, das die Berechtigungsklasse für den Benutzer darstellt. Sobald es Änderungen gibt (Regionen und/oder Aktionen
		 * ändern sich), werden diese Änderungen am Java-Objekt, das die Berechtigungsklasse darstellt, übernommen.
		 *
		 * @param _authenticationClass Datenverteilerobjekt, das eine Berechtigungsklasse darstellt.
		 */
		public AuthenticationClassUpdater(SystemObject _authenticationClass) {
			super(OldUserInfo.this.getConnection(), AUTHENTIFICATION_CLASS_ATTRIBUTE_GROUP_PID, AUTHENTIFICATION_CLASS_ASPECT_PID, OldUserInfo.this);
			startDataListener(_authenticationClass);
		}

		public final SystemObject getAuthenticationClass() {
			return getSystemObject();
		}

		@Override
		public void deactivateInvalidChild(final DataLoader node) {
			// Alte BenutzerInfo-Klasse unterstützt keine Rekursion, Implementierung nicht notwendig.
			throw new UnsupportedOperationException("removeInvalidChild nicht implementiert");
		}

		@Override
		protected void update(final Data data) {
			if(data == null){
				_debug.warning("Für die Bereichtigungsklasse " + getAuthenticationClass() + " liegen keine Daten vor", AUTHENTIFICATION_CLASS_ATTRIBUTE_GROUP_PID);
				return;
			}
			updateAuthenticationUnit(data);
		}

		@Override
		protected Collection<DataLoader> getChildObjects() {
			// Alle Kindobjekte werden direkt hier als Unterklassen gespeichert, keine Sonderbehandlung nötig
			return Collections.emptyList();
		}

		public final int hashCode() {
			int result = 19;
			long id = getUser().getId();
			result = (41 * result) + (int)(id ^ (id >>> 32));
			id = getAuthenticationClass().getId();
			result = (41 * result) + (int)(id ^ (id >>> 32));
			return result;
		}

		public final boolean equals(Object obj) {
			if(obj == null) {
				return false;
			}
			if(obj instanceof AuthenticationClassUpdater) {
				AuthenticationClassUpdater au = (AuthenticationClassUpdater)obj;
				if((getAssociatedUserId() == au.getAssociatedUserId()) && (getAuthenticationClass().getId() == au.getAuthenticationClass().getId())) {
					return true;
				}
			}
			return false;
		}

		public final long getAssociatedUserId() {
			return getUser().getId();
		}
	}
}
