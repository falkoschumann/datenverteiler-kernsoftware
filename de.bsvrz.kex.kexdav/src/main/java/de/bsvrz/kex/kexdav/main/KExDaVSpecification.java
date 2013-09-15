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

package de.bsvrz.kex.kexdav.main;

import de.bsvrz.kex.kexdav.parameterloader.ConnectionParameter;

/**
 * Dieses Interface bietet eine Schnittstelle um KExDaV-Verbindungen auszuwählen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9074 $
 */
public interface KExDaVSpecification {

	/**
	 * Prüft, ob eine KExDaV-Verbindung zu dieser Spezifikation passt
	 * @param connectionParameter Verbindungs-Parameter
	 * @return true wenn es passt, sonst false
	 */
	boolean matches(ConnectionParameter connectionParameter);
}
