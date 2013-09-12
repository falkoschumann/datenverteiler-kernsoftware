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

package de.bsvrz.dav.dav.main;

/**
 * Definiert die über JMX zugänglichen Methoden des SubscriptionsManager
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8100 $
 */
public interface SubscriptionsManagerMBean {

	/** Gibt die verwalteten Anmeldungen auf die Standardausgabe aus. */
	void dumpSubscriptions();

	/** Gibt die Matrix der günstigsten Weg auf die Standardausgabe aus. */
	void dumpRoutingTable();

	/** Gibt die Anmeldelisten auf die Standardausgabe aus. */
	void dumpSubscriptionLists();

	/** Gibt die Anmeldelisten auf die Standardausgabe aus. */
	long[] getPotentialCentralDavs(long objectId, long attributeUsageId);

}
