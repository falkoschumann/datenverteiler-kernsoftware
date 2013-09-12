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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.kex.kexdav.main.KExDaVSpecification;

/**
 * Triggerspezifikation anhand eines Data-Objektes der Trigger-Attributgruppe
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9084 $
 */
public class TriggerSpecification implements KExDaVSpecification {

	private final Data _data;

	/**
	 * Erstellt einen Trigger
	 * @param data data
	 */
	public TriggerSpecification(final Data data) {
		_data = data;
	}

	public boolean matches(final ConnectionParameter connectionParameter) {
		final Data.Array pidArray = _data.getArray("Datenverteiler-Pid");
		final Data.Array ipArray = _data.getArray("Datenverteiler-IP");
		if(pidArray.getLength() == 1) {
			if(!pidArray.getItem(0).asTextValue().getText().equals(connectionParameter.getDavPid())) {
				return false;
			}
		}
		if(ipArray.getLength() == 1) {
			if(!ipArray.getItem(0).getTextValue("IP-Adresse").getText().equals(connectionParameter.getIP())) {
				return false;
			}
			if(!(ipArray.getItem(0).getUnscaledValue("Portnummer").longValue() == connectionParameter.getPort())) {
				return false;
			}
		}
		return true;
	}
}
