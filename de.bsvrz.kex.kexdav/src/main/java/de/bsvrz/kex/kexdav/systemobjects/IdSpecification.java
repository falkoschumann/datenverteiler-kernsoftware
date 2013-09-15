/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.systemobjects;

import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.kex.kexdav.main.Constants;
import de.bsvrz.kex.kexdav.management.ManagerInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die IdSpecification wählt Systemobjekte aus, die entweder die angenommene Id haben oder von einem Objekt kopiert wurden, das diese Id hat.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9238 $
 */
public class IdSpecification extends ObjectSpecification {

	private final long _id;

	/**
	 * Dieser Cache ist static und wird für alle Verbindungen benutzt, kann also höchstwahrscheinlich viel Unsinn enthalten. Daher muss alles was damit ermittelt
	 * wurde extra überprüft werden. Auf der anderen Seite sollte der Geschwindigkeitsvorteil enorm sein, weil sonst ständig alle dynamischen Objekte durchsucht
	 * werden müssten.
	 * <p/>
	 * Enthalten sind Originale Objekt-Ids zu Echten Objekt-Ids
	 */
	private static final Map<Long, Long> _idCache = new HashMap<Long, Long>();

	private ManagerInterface _manager;

	/**
	 * Erstellt eine IdSpecification
	 *
	 * @param id      Id
	 * @param manager
	 */
	public IdSpecification(final long id, final ManagerInterface manager) {
		_id = id;
		_manager = manager;
	}

	@Override
	public SystemObject getObject(final DataModel dataModel) {
		final SystemObject object = dataModel.getObject(_id);
		if(object != null) {
			return object;
		}
		final SystemObjectType dynamicObjectType = dataModel.getType(Constants.Pids.TypeDynamicObject);
		final List<SystemObject> objects = dynamicObjectType.getObjects();

		try {

			// versuchen, etwas aus dem Cache zu lesen. Die Informationen darin können falsch oder veraltet sein, müssen also exakt geprüft werden
			final Long maybeObjectId = _idCache.get(_id);
			if(maybeObjectId != null) {
				final SystemObject maybeCachedObject = dataModel.getObject(maybeObjectId);
				if(maybeCachedObject != null && maybeCachedObject.isValid()) {
					final ExchangeProperties props = KExDaVObject.getExchangeProperties(maybeCachedObject);
					if(props != null && matchesProperties(props)) return maybeCachedObject;
				}
			}

			// Notfalls alle dynamischen Objekte durchsuchen
			SystemObject result = null;
			for(final SystemObject systemObject : objects) {
				final ExchangeProperties properties = KExDaVObject.getExchangeProperties(systemObject);
				if(properties != null) {
					_idCache.put(properties.getOriginalId(), systemObject.getId());
					if(matchesProperties(properties)) {
						result = systemObject;
					}
				}
			}

			return result;
		}
		catch(MissingKExDaVAttributeGroupException ignored) {
			// Die Attributgruppe mit den Konfigurationsdaten wurde nicht gefunden. Folglich wurde kein Objekt übertragen.
			return null;
		}
	}

	private boolean matchesProperties(final ExchangeProperties props) {
		final SystemObject kExDaVObject = _manager.getKExDaVObject();
		return props.getOriginalId() == _id && (kExDaVObject == null || kExDaVObject.getPidOrId().equals(props.getKExDaVObject()));
	}

	@Override
	public boolean matches(final SystemObject object) {
		if(object == null) throw new IllegalArgumentException("object ist null");
		if(object.getId() == _id) return true;

		final ExchangeProperties properties;
		try {
			properties = KExDaVObject.getExchangeProperties(object);
		}
		catch(MissingKExDaVAttributeGroupException ignored) {
			// Die Attributgruppe mit den Konfigurationsdaten wurde nicht gefunden. Folglich gibt es kein Übertragenes Objekt.
			return false;
		}
		return properties != null && matchesProperties(properties);
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		if(_id != ((IdSpecification)o)._id) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return (int)(_id ^ (_id >>> 32));
	}

	@Override
	public String toString() {
		return "[" + _id + "]";
	}
}
