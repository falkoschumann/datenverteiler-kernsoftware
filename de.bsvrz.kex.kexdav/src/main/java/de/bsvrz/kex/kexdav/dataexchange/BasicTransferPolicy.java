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

package de.bsvrz.kex.kexdav.dataexchange;

/**
 * Einfache Datenübertragung. Die Daten werden wie sie sind ohne Verzögerung an das Zielsystem übertragen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9274 $
 */
class BasicTransferPolicy extends DataTransferPolicy {

	/**
	 * Erstellt eine neue BasicTransferPolicy
	 * @param lowLevelDataPipe Datenverbindung
	 */
	public BasicTransferPolicy(final LowLevelDataPipe lowLevelDataPipe) {
		super(lowLevelDataPipe);
	}

	@Override
	protected void handleData(final DataPackage dataPackage) {
		sendData(dataPackage);
	}
}
