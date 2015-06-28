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

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.Collection;

/**
 * Kapselt einen Block wie "AuswahlBereich", "AuswahlObjekte" etc. Entspricht aber nicht direkt den DatenmodellBlöcken, da je nach gesetzten Parametern
 * optimierte Klassen benutzt werden
  *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public interface ObjectSelectionBlock {

	/**
	 * Prüft ob das übergebene Objekt in diesem Block enthalten ist
	 *
	 * @param object Testobjekt
	 *
	 * @return true wenn enthalten
	 */
	boolean contains(SystemObject object);

	/**
	 * Gibt alle Objekte in dem Block zurück. Der Aufruf sollte, falls möglich, vermieden werden, da der Vorgang je nach Definition sehr lange dauern kann
	 *
     * @param type Liste mit Systemobjekttypen die beachtet werden sollen.
	 * @return Liste mit Systemobjekten
	 */
	Collection<SystemObject> getAllObjects(final Collection<? extends SystemObjectType> type);

	/**
	 * Gibt alle Objekttypen zurück, die in diesem Block betrachtet werden, bzw. nach denen gefiltert wird. Alle mit {@link #getAllObjects(java.util.Collection)}
	 * zurückgelieferten Objekte sind zwingend von diesen Typen, umgekehrt ist allerdings nicht sichergestellt, dass zu allen hier zurückgelieferten Typen auch
	 * Objekte vorhanden sind.
	 *
	 * @return Liste mit allen Typen
	 */
	Collection<SystemObjectType> getAllObjectTypes();

	/**
	 * Erstellt einen Listener, der Objekte über das Ändern dieses Blocks benachrichtigt
	 *
	 * @param object Listener
	 */
	void addChangeListener(ObjectCollectionChangeListener object);

	/**
	 * Entfernt einen Listener, der Objekte über das Ändern dieses Blocks benachrichtigt
	 *
	 * @param object Listener
	 */
	void removeChangeListener(ObjectCollectionChangeListener object);

	/**
	 * Markiert das Objekt als unbenutzt, sodass angemeldete Listener etc. abgemeldet werden können
	 */
	void dispose();
}
