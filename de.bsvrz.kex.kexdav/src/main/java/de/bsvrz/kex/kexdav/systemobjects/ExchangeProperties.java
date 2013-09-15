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

import de.bsvrz.dav.daf.main.Data;

/**
 * Eigenschaften eines dynamischen Objektes betreffend des KExDavAustausches
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public class ExchangeProperties {

	private final Data _configurationData;

	/**
	 * Konstruktor
	 *
	 * @param configurationData Data-Objekt vom Typ {@link de.bsvrz.kex.kexdav.main.Constants.Pids#AttributeGroupKExDaVConfigData}
	 */
	public ExchangeProperties(final Data configurationData) {
		if(configurationData == null) throw new IllegalArgumentException("configurationData ist null");
		_configurationData = configurationData;
	}

	/**
	 * Gibt die Id des Original-Objektes zurück
	 *
	 * @return die Id des Original-Objektes
	 */
	public long getOriginalId() {
		return _configurationData.getUnscaledValue("originalId").longValue();
	}

	/**
	 * Gibt die Pid des originalen KV zurück
	 *
	 * @return die Pid des originalen KV
	 */
	public String getConfigurationAuthorityPid() {
		return _configurationData.getTextValue("konfigurationsVerantwortlicher").getText();
	}

	/**
	 * Gibt das KExDaV-Objekt zurück
	 *
	 * @return das KExDaV-Objekt mit dem das Objekt erstellt wurde
	 */
	public String getKExDaVObject() {
		return _configurationData.getTextValue("kexdavObjekt").getText();
	}

	@Override
	public String toString() {
		return "ExchangeProperties{" + getOriginalId() + "," + getConfigurationAuthorityPid() + '}';
	}
}
