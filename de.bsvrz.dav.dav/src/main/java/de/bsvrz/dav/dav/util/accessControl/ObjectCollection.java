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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.Collection;
import java.util.List;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9100 $
 */
public interface ObjectCollection {

	/**
	 * Alle Objekte, die durch diesen Block ausgewählt werden
	 *
	 * @param types Systemobjekttypen die beachtet werden sollen
	 *
	 * @return Alle Objekte, die durch diesen Block ausgewählt werden
	 */
	List<SystemObject> getAllObjects(Collection<? extends SystemObjectType> types);

	/** Startet alle Listener
	 * @param listener*/
	void addChangeListener(final ObjectCollectionChangeListener listener);

	/** Stoppt alle Listener
	 * @param listener*/
	void removeChangeListener(final ObjectCollectionChangeListener listener);
}
