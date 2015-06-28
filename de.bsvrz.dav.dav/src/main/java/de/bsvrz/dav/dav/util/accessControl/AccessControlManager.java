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
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.util.HashBagMap;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.operatingMessage.MessageGrade;
import de.bsvrz.sys.funclib.operatingMessage.MessageSender;
import de.bsvrz.sys.funclib.operatingMessage.MessageState;
import de.bsvrz.sys.funclib.operatingMessage.MessageType;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Klasse, die im Datenmodell Abfragen nach Benutzerberechtigungen erlaubt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public final class AccessControlManager implements RegionManager, Closeable {

	/** Debug */
	private static final Debug _debug = Debug.getLogger();

	/**
	 * Interval zwischen 2 Betriebsmeldungen wegen fehlenden Parametern. Außerdem die Zeit, die mindestens vergangen sein muss, bis ein fehlender
	 * Parameterdatensatz gemeldet wird. Bei der Anpassung der Zeit muss möglicherweise der Wortlaut der Betriebsmeldung geändert werden.
	 */
	public static final int MessageSenderInterval = 60 * 1000;

	/**
	 * Spezielles Long, dass das Töten des Threads bewirkt.
	 */
	@SuppressWarnings("UnnecessaryBoxing")
	private static final Long POISON = new Long(0);

	/** Map, die BenutzerIds den Benutzerobjekten zuordnet */
	private final HashMap<Long, UserInfoInternal> _userInfoHashMap = new HashMap<Long, UserInfoInternal>();

	/** Map, die Berechtigungsklassen den kapselnden AccessControlUnit-Klassen zuordnet */
	private final HashMap<SystemObject, AccessControlUnit> _authenticationClassHashMap = new HashMap<SystemObject, AccessControlUnit>();

	/** Map, die Rollen den kapselnden Role-Klassen zuordnet */
	private final HashMap<SystemObject, Role> _roleHashMap = new HashMap<SystemObject, Role>();

	/** Map, die Regionen den kapselnden Region-Klassen zuordnet */
	private final HashMap<SystemObject, Region> _regionHashMap = new HashMap<SystemObject, Region>();

	/** Datenverteilerverbindung */
	private final ClientDavInterface _connection;

	/** Ob das neue Datenmodell (siehe {@link de.bsvrz.dav.dav.util.accessControl.ExtendedUserInfo}) benutzt wird */
	private final boolean _isUsingNewDataModel;

	/** Callback, der aufgerufen wird, wenn sich die Rechte eines Benutzers ändern */
	private final UserRightsChangeHandler _userRightsChangeHandler;

	/** Ob implizite Benutzerverwaltung durchgeführt wird, oder Benutzer mit addUser erstellt werden müssen */
	private final boolean _useImplicitUserManagement;

	private final Object _updateLock = new Object();

	private final ReentrantReadWriteLock _userMapLock = new ReentrantReadWriteLock();

	private HashBagMap<DataState, DataLoader> _oldObjectsWithMissingParameters;

	private final LinkedBlockingQueue<Long> _notifyUserChangedQueue = new LinkedBlockingQueue<Long>();
	private Timer _parameterTimer;

	/**
	 * Erstellt eine neue Instanz des AccessControlManagers mit impliziter Benutzerverwaltung
	 *
	 * @param connection              Verbindung zum Datenverteiler
	 * @param userRightsChangeHandler Klasse, die über Änderungen an den Benutzerrechten informiert werden soll. Das ist im allgemeinen der {@link
	 *                                de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager}, der bei sich ändernden Rechten eventuell ungültig gewordene Anmeldungen
	 *                                deaktiviert, kann aber für Testfälle und andere Anwendungen auch ein anderes (möglicherweise deutlich kleineres) Objekt
	 *                                sein.
	 * @param useNewDataModel         Sollen die neuen Zugriffsrechte benutzt werden?
	 */
	public AccessControlManager(
			final ClientDavInterface connection, final UserRightsChangeHandler userRightsChangeHandler, final boolean useNewDataModel) {
		this(connection, userRightsChangeHandler, true, useNewDataModel);
	}

	/**
	 * Erstellt eine neue Instanz des AccessControlManagers
	 *
	 * @param connection                Verbindung zum Datenverteiler
	 * @param userRightsChangeHandler   Klasse, die über Änderungen an den Benutzerrechten informiert werden soll. Das ist im allgemeinen der {@link
	 *                                  de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager}, der bei sich ändernden Rechten eventuell ungültig gewordene Anmeldungen
	 *                                  deaktiviert, kann aber für Testfälle und andere Anwendungen auch ein anderes (möglicherweise deutlich kleineres) Objekt
	 *                                  sein.
	 * @param useImplicitUserManagement Wenn false, werden nur Benutzer berücksichtigt, die mit addUser und removeUser in diese Klasse eingefügt werden.<br> Wenn
	 *                                  true sind addUser und removeUser ohne Funktion und getUser ermittelt beliebige Benutzer, solange diese existieren.
	 * @param useNewDataModel           Sollen die neuen Zugriffsrechte benutzt werden?
	 */
	public AccessControlManager(
			final ClientDavInterface connection,
			final UserRightsChangeHandler userRightsChangeHandler,
			final boolean useImplicitUserManagement,
			final boolean useNewDataModel) {
		_connection = connection;
		_userRightsChangeHandler = userRightsChangeHandler;
		_useImplicitUserManagement = useImplicitUserManagement;
		if(useNewDataModel && _connection.getDataModel().getObject("atl.aktivitätObjekteNeu") == null) {
			_debug.error(
					"Das neue Datenmodell der Zugriffsrechte-Prüfung sollte verwendet werden, wurde aber nicht gefunden. Stattdessen wird das alte Datenmodell benutzt."
			);
			_isUsingNewDataModel = false;
		}
		else {
			_isUsingNewDataModel = useNewDataModel;
		}
		if(_isUsingNewDataModel) {
			createParameterTimer();
		}

		final Thread refreshThread = new Thread("Aktualisierung Benutzerrechte") {
			@Override
			public void run() {
				while(!interrupted()) {
					try {
						Long userId = _notifyUserChangedQueue.take();
						//noinspection NumberEquality
						if(userId == POISON) return;
						_userRightsChangeHandler.handleUserRightsChanged(userId);
					}
					catch(Exception e) {
						_debug.error("Fehler beim Ändern von Benutzerrechten", e);
					}
				}
			}
		};
		refreshThread.setDaemon(true);
		refreshThread.start();
	}

	private void createParameterTimer() {
		_parameterTimer = new Timer("Warnung über fehlende Parameter", true);
		_parameterTimer.schedule(
				new TimerTask() {
					@Override
					public void run() {
						sendMessagesAboutMissingParameters();
					}
				}, MessageSenderInterval, MessageSenderInterval
		);
	}

	@Override
	public void close() {
		_notifyUserChangedQueue.add(POISON);
		_parameterTimer.cancel();
		for(Role role : _roleHashMap.values()) {
			role.stopDataListener();
		}
		for(Region region : _regionHashMap.values()) {
			region.stopDataListener();
		}
		for(AccessControlUnit accessControlUnit : _authenticationClassHashMap.values()) {
			accessControlUnit.stopDataListener();
		}
		for(UserInfoInternal userInfoInternal : _userInfoHashMap.values()) {
			userInfoInternal.stopDataListener();
		}
	}

	private void sendMessagesAboutMissingParameters() {
		final HashBagMap<DataState, DataLoader> objectsWithMissingParameters = new HashBagMap<DataState, DataLoader>();
		final Collection<UserInfoInternal> values = _userInfoHashMap.values();
		final List<ExtendedUserInfo> users = new ArrayList<ExtendedUserInfo>(values.size());
		for(final UserInfoInternal value : values) {
			users.add((ExtendedUserInfo)value);
		}
		objectsWithMissingParameters.addAll(getObjectsWithMissingParameters(users));
		objectsWithMissingParameters.addAll(getObjectsWithMissingParameters(_authenticationClassHashMap.values()));
		objectsWithMissingParameters.addAll(getObjectsWithMissingParameters(_regionHashMap.values()));
		objectsWithMissingParameters.addAll(getObjectsWithMissingParameters(_roleHashMap.values()));
		final MessageSender sender = MessageSender.getInstance();
		final MessageState state;
		final String message;
		if(_oldObjectsWithMissingParameters == null || _oldObjectsWithMissingParameters.size() == 0) {
			if(objectsWithMissingParameters.size() == 0) return;
			state = MessageState.NEW_MESSAGE;
			message = "Der Rechteprüfung fehlen Parameterdaten:\n" + formatMap(objectsWithMissingParameters);
		}
		else {
			if(objectsWithMissingParameters.size() == 0) {
				state = MessageState.GOOD_MESSAGE;
				message = "Alle derzeit berücksichtigten Objekte besitzen jetzt Parameter.";
			}
			else {
				state = MessageState.REPEAT_MESSAGE;
				message = "Der Rechteprüfung fehlen Parameterdaten:\n" + formatMap(
						objectsWithMissingParameters
				);
			}
		}

		sender.sendMessage("Zugriffsrechte", MessageType.SYSTEM_DOMAIN, "Rechteprüfung", MessageGrade.WARNING, state, message);
		_debug.warning(message);
		_oldObjectsWithMissingParameters = objectsWithMissingParameters;
	}

	private static String formatMap(final HashBagMap<DataState, DataLoader> objectsWithMissingParameters) {
		final StringBuilder builder = new StringBuilder();
		for(final Map.Entry<DataState, Collection<DataLoader>> entry : objectsWithMissingParameters.entrySet()) {
			if(entry.getValue().size() == 0) continue;
			if(entry.getKey() == null) {
				builder.append("Kein Datensatz");
			}
			else {
				builder.append(entry.getKey().toString());
			}
			builder.append(" (");
			builder.append(entry.getValue().size());
			if(entry.getValue().size() == 1) {
				builder.append(" Objekt):\n");
			}
			else {
				builder.append(" Objekte):\n");
			}
			for(final DataLoader loader : entry.getValue()) {
				builder.append("\t").append(loader.getSystemObject().getPidOrNameOrId()).append("\n");
			}
		}
		return builder.toString();
	}

	private static HashBagMap<DataState, DataLoader> getObjectsWithMissingParameters(final Collection<? extends DataLoader> values) {
		final HashBagMap<DataState, DataLoader> result = new HashBagMap<DataState, DataLoader>();
		for(final DataLoader value : values) {
			if(value.getDataState() != DataState.DATA && value.getNoDataTime() > MessageSenderInterval) {
				result.add(value.getDataState(), value);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "AccessControlManager{" + "_useImplicitUserManagement=" + _useImplicitUserManagement + ", _isUsingNewDataModel=" + _isUsingNewDataModel + '}';
	}

	/**
	 * Fügt eine Benutzerinformation zu der Benutzertabelle hinzu, wenn der Datenverteiler die Benutzerrechte prüfen soll. Existiert der Benutzer bereits, wird
	 * lediglich die interne Referenz inkrementiert.
	 *
	 * @param userId BenutzerID
	 */
	public final void addUser(final long userId) {
		if(_useImplicitUserManagement) return;
		addUserInternal(userId);
	}

	private UserInfo addUserInternal(final long userId) {
		_userMapLock.writeLock().lock();
		try{
			UserInfoInternal userInfo = _userInfoHashMap.get(userId);
			if(userInfo == null) {
				userInfo = createUserInfo(userId);
				_userInfoHashMap.put(userId, userInfo);
			}
			else {
				userInfo.incrementReference();
			}
			return userInfo;
		}
		finally {
			_userMapLock.writeLock().unlock();
		}
	}

	/**
	 * Erstellt je nach Datenmodell-Version ein neues BenutzerInfo-Objekt das Abfragen auf die Berechtigungen eines Benutzers ermöglicht.
	 *
	 * @param userID benutzer-ID
	 *
	 * @return Das Benutzer-Info-Objekt
	 */
	@SuppressWarnings({"deprecation"})
	private UserInfoInternal createUserInfo(final long userID) {
		if(isUsingNewDataModel()) {
			return new ExtendedUserInfo(userID, _connection, this);
		}
		else {
			return new OldUserInfo(userID, _connection, _userRightsChangeHandler, this);
		}
	}

	/**
	 * Fragt ab, ob das neue Datenmodell benutzt wird. Das neue Datenmodell enthält eine neue Struktur der Region und Rollen-Objekten und ermöglicht Beschränkungen
	 * bei der Erstellung von dynamischen Objekten.
	 *
	 * @return True wenn das neue Modell benutzt wird, sonst false
	 */
	public boolean isUsingNewDataModel() {
		return _isUsingNewDataModel;
	}

	/**
	 * Wird aufgerufen, wenn eine Rekursion in den Systemobjekten gefunden wurde. Dabei wird eine _Debug-Meldung ausgegeben und das Elternelement angewiesen die
	 * Referenz auf das Kindobjekt zu deaktivieren.
	 *
	 * @param node   Der Knoten, der sich selbst referenziert
	 * @param parent Der Knoten, der den problematischen Knoten referenziert
	 * @param trace  Komplette Hierarchie vom Benutzer zum problematischen Objekt.
	 */
	protected void notifyInfiniteRecursion(final DataLoader node, final DataLoader parent, final List<DataLoader> trace) {
		String msg = "Ungültige Rekursion in den Systemobjekten. Die problematische Vererbung wird deaktiviert bis das Problem behoben wird.\n"
		             + "Objekt referenziert sich selbst: " + node + "\n" + "Vererbungskette: " + trace;
		MessageSender.getInstance().sendMessage("Zugriffsrechte", MessageType.SYSTEM_DOMAIN, "Rechteprüfung", MessageGrade.WARNING, MessageState.MESSAGE, msg);
		_debug.warning(msg);
		parent.deactivateInvalidChild(node);
	}

	/**
	 * Gibt die AuthenticationClass-Klasse zurück die zu dem angeforderten Systemobjekt gehört.
	 *
	 * @param systemObject Systemobjekt, das eine Berechtigungsklasse repräsentiert
	 *
	 * @return AuthenticationClass-Klasse die Abfragen auf eine Berechtigungsklasse ermöglicht
	 */
	AccessControlUnit getAuthenticationClass(final SystemObject systemObject) {
		synchronized(_authenticationClassHashMap) {
			AccessControlUnit authenticationClass = _authenticationClassHashMap.get(systemObject);
			if(null != authenticationClass) {
				return authenticationClass;
			}
			authenticationClass = new AccessControlUnit(systemObject, _connection, this);
			_authenticationClassHashMap.put(systemObject, authenticationClass);
			return authenticationClass;
		}
	}

	/**
	 * Gibt die Region-Klasse zurück die zu dem angeforderten Systemobjekt gehört.
	 *
	 * @param systemObject Systemobjekt, das eine Region repräsentiert
	 *
	 * @return Region-Klasse die Abfragen auf eine Region ermöglicht
	 */
	@Override
	public Region getRegion(final SystemObject systemObject) {
		synchronized(_regionHashMap) {
			Region region = _regionHashMap.get(systemObject);
			if(null != region) {
				return region;
			}
			region = new Region(systemObject, _connection, this);
			_regionHashMap.put(systemObject, region);
			return region;
		}
	}

	/**
	 * Gibt die Role-Klasse zurück die zu dem angeforderten Systemobjekt gehört.
	 *
	 * @param systemObject Systemobjekt, das eine Rolle repräsentiert
	 *
	 * @return Role-Klasse die Abfragen auf eine Rolle ermöglicht
	 */
	Role getRole(final SystemObject systemObject) {
		synchronized(_roleHashMap) {
			Role role = _roleHashMap.get(systemObject);
			if(null != role) {
				return role;
			}
			role = new Role(systemObject, _connection, this);
			_roleHashMap.put(systemObject, role);
			return role;
		}
	}

	/**
	 * Gibt das gespeicherte BenutzerObjekt mit der angegebenen ID zurück
	 *
	 * @param userId Angegebene BenutzerId
	 *
	 * @return Das geforderte UserInfo-Objekt
	 */
	public UserInfo getUser(final long userId) {
		UserInfoInternal userInfo;
		_userMapLock.readLock().lock();
		try {
			userInfo = _userInfoHashMap.get(userId);
		}
		finally {
			_userMapLock.readLock().unlock();
		}
		if(_useImplicitUserManagement && userInfo == null) {
			// addUserInternal verwendet _userMapLock.writeLock(). Daher muss das readLock hier freigegeben worden sein,
			return addUserInternal(userId);
		}
		return userInfo;
	}

	/**
	 * Prüft ob ein Objekt wie eine Rolle oder eine Region von einem übergeordnetem Objekt wie einem Benutzer
	 * oder einer Berechtigungsklasse referenziert wird.
	 *
	 * @param parent        Mögliches Vaterobjekt
	 * @param possibleChild Möglichen Kindobjekt
	 *
	 * @return True wenn das possibleChild ein Kind von parent ist.
	 */
	private boolean isChildOf(final DataLoader parent, final DataLoader possibleChild) {
		final List<DataLoader> children = enumerateChildren(parent);
		return children.contains(possibleChild);
	}

	/**
	 * Löscht einen Benutzer aus der Benutzertabelle, wenn der Datenverteiler die Benutzerrechte prüfen soll. Wenn die interne Referenz eines Benutzers 0 ist, dann
	 * wird die Benutzerinformation aus der Tabelle entfernt.
	 *
	 * @param userId BenutzerID
	 */
	public final void removeUser(final long userId) {
		if(_useImplicitUserManagement) return;
		_userMapLock.writeLock().lock();
		try {
			final UserInfoInternal user = _userInfoHashMap.get(userId);
			if(user != null) {
				user.decrementReference();
				if(user.canBeSafelyDeleted()) {
					user.stopDataListener();
					_userInfoHashMap.remove(userId);
				}
			}
		}
		finally {
			_userMapLock.writeLock().unlock();
		}
	}

	/**
	 * Wird aufgerufen un dem AccessControlManager zu informieren, dass ein verwaltetes Objekt sich geändert hat. Der AccessControlManager wird daraufhin nach
	 * Benutzer-Objekten suchen, die dieses Objekt verwenden und an den {@link de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager} eine Benachrichtigung senden, dass
	 * sich die Rechte des Benutzers geändert haben und eventuelle vorhandene Anmeldungen entfernt werden müssen.
	 *
	 * @param object Objekt das sich geändert hat
	 */
	@Override
	public void objectChanged(final DataLoader object) {
		final List<Long> affectedUserIds = new ArrayList<Long>();
		_userMapLock.readLock().lock();
		try {
			for(final UserInfoInternal userInfo : _userInfoHashMap.values()) {
				if(userInfo instanceof DataLoader) {
					final DataLoader userAsDataLoader = (DataLoader) userInfo;
					if(isChildOf(userAsDataLoader, object)) {
						affectedUserIds.add(userInfo.getUserId());
					}
				}
			}
		}
		finally {
			_userMapLock.readLock().unlock();
		}

		// Im Falle das _userRightsChangeHandler der ConnectionsManager ist, synchronisiert dieser auf sich selber.
		// Daher darf der folgende Code nicht im _userMapLock stehen, sonst wäre das als verschachteltes Locking sehr
		// DeadLock-anfällig.

		// Der Fall dass zwischenzeitlich die aktuellen Benutzer geändert worden sind, ist irrelevant
		// da der Parameterdatenempfang asynchron stattfindet und daher sowieso keine festen Aussagen bzgl.
		// der Reihenfolge der kritischen Aufrufe von addUser()/getUser()/removeUser() etc. und objectChanged() gemacht werden können.
		// Benutzer, die während der Auführung dieser Zeilen angelegt werden besitzen bereits die neuen Parameterdaten
		// und sind daher unkritisch. Benutzer die währenddessen gelöscht werden sind sowieso unerheblich,
		// da diese sowieso gezwungen sind alle Anmeldungen zu entfernen und eine Aktualisierung wg. geänderter Rechte sinnlos wäre
		for(Long affectedUserId : affectedUserIds) {
			notifyUserRightsChangedAsync(affectedUserId);
		}

	}

	private void notifyUserRightsChangedAsync(final Long affectedUserId) {
		_notifyUserChangedQueue.add(affectedUserId);
	}

	/**
	 * Wird aufgerufen un dem AccessControlManager zu informieren, dass ein Benutzer sich geändert hat. Der AccessControlManager wird daraufhin die referenzierten
	 * Kindobjekte (Rollen, Regionen etc.) auf Rekursion überprüfen und an den {@link de.bsvrz.dav.dav.main.HighLevelSubscriptionsManager} eine Benachrichtigung senden,
	 * dass sich die Rechte des Benutzers geändert haben und eventuelle vorhandene Anmeldungen entfernt werden müssen.
	 *
	 * @param userInfo Benutzerobjekt, das sich geändert hat
	 */
	void userChanged(final UserInfoInternal userInfo) {
		if(userInfo instanceof DataLoader) {
			final DataLoader userAsDataLoader = (DataLoader)userInfo;
			enumerateChildren(userAsDataLoader); // Prüft auf Rekursion
			long userId = userInfo.getUserId();
			notifyUserRightsChangedAsync(userId); // Nachricht an ConnectionsManager
		}
	}

	/**
	 * Gibt alle Kindelemente eines Objekts zurück
	 *
	 * @param node Objekt das nach Kindelementen gefragt wird
	 *
	 * @return Liste mit Kindelementen
	 */
	private List<DataLoader> enumerateChildren(final DataLoader node) {
		return new ChildrenTreeEnumerator(this, node).enumerateChildren();
	}

	/**
	 * Um immer einen konsistenten Zustand zu haben, darf immer nur ein DataLoader gleichzeitig pro AccessControlManager geupdatet werden. Dazu wird auf dieses
	 * dummy-Objekt synchronisiert
	 *
	 * @return Objekt auf das Synchronisiert werden soll
	 */
	@Override
	public Object getUpdateLock() {
		return _updateLock;
	}
}
