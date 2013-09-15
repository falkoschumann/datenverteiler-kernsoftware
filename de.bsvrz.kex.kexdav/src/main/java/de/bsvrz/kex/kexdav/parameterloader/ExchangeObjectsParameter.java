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

package de.bsvrz.kex.kexdav.parameterloader;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.dav.util.accessControl.*;
import de.bsvrz.kex.kexdav.systemobjects.MissingObjectException;

/**
 * Objektaustauschparameter
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9218 $
 */
public class ExchangeObjectsParameter implements ObjectCollectionFactory {

	private final Data _data;

	private final String _dataString;

	/**
	 * Ein Parameter für den Objektaustausch
	 * @param data Data-Objekt
	 */
	ExchangeObjectsParameter(final Data data) {
		_data = data;
		_dataString = _data.toString();
	}

	@Override
	public String toString() {
		return "ExchangeObjectsParameter{" + "_data=" + _dataString + '}';
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ExchangeObjectsParameter other = (ExchangeObjectsParameter)o;

		if(!_dataString.equals(other._dataString)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return _dataString.hashCode();
	}


	public ObjectCollection createObjectCollection(final ObjectCollectionParent objectCollectionParent, final ClientDavInterface connection)
			throws MissingObjectException {
		try {
			return new ObjectSet(objectCollectionParent, connection, _data, true);
		}
		catch(IllegalArgumentException e) {
			throw new MissingObjectException(e);
		}
	}
}
