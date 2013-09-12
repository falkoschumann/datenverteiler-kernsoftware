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

package de.bsvrz.kex.kexdav.dataplugin;

import de.bsvrz.kex.kexdav.correspondingObjects.ObjectManagerInterface;
import de.bsvrz.kex.kexdav.dataexchange.DataCopyException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public interface KExDaVDataPlugin {

	/**
	 * Kopiert Daten von einem Data-Objekt in ein anderes Data-Objekt, welches auch zu einer anderen Konfiguration gehören darf.
	 *
	 * @param input         Eingabedaten
	 * @param output        Leeres Datenobjekt für die Daten, die im Zielsystem verschickt werden sollen (sollen von dieser Funktion modifiziert werden)
	 * @param objectManager Callback-Objekt, das damit beauftragt werden kann, dynamische Objekte zu kopieren. (Kann null sein)
	 * @param manager       KExDaV-Verwaltung
	 *
	 * @throws de.bsvrz.kex.kexdav.dataexchange.DataCopyException
	 *          Falls das Kopieren der Daten fehlschlägt
	 */
	void process(KExDaVAttributeGroupData input, KExDaVAttributeGroupData output, ObjectManagerInterface objectManager, final ManagerInterface manager)
			throws DataCopyException;
}
