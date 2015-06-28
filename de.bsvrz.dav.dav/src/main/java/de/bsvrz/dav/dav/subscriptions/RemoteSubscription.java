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

package de.bsvrz.dav.dav.subscriptions;

import java.util.Collection;
import java.util.Set;

/**
 * Interface für eine Anmeldung zu einem anderen Datenverteiler (T_T)
 * @author Kappich Systemberatung
 * @version $Revision: 11461 $
 */
public interface RemoteSubscription extends Subscription {
	@Override
	public TransmitterCommunicationInterface getCommunication();

	Set<Long> getPotentialDistributors();

	void setPotentialDistributors(Collection<Long> value);

	void addPotentialDistributor(long transmitterId);

	void removePotentialDistributor(long transmitterId);
}
