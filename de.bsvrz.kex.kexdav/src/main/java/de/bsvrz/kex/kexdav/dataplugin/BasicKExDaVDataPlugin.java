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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.kex.kexdav.correspondingObjects.ObjectManagerInterface;
import de.bsvrz.kex.kexdav.dataexchange.DataCopyException;
import de.bsvrz.kex.kexdav.management.ManagerInterface;
import de.bsvrz.kex.kexdav.systemobjects.KExDaVAttributeGroupData;
import de.bsvrz.kex.kexdav.systemobjects.ObjectSpecification;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Standard-Plugin, für die Übertragung von Daten von einem Datenverteiler/Data-Objekt zu einem anderen Datenverteiler/Data-Objekt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public class BasicKExDaVDataPlugin implements KExDaVDataPlugin {

	public void process(
			final KExDaVAttributeGroupData input,
			final KExDaVAttributeGroupData output,
			final ObjectManagerInterface objectManager,
			final ManagerInterface manager) throws DataCopyException {
		copyDataTree(input, output, objectManager, manager);
	}

	/**
	 * Hilfsfunktion. Diese Funktion kopiert ein Datum oder eine Datenliste in ein anderes Data-Objekt. Bei Datenlisten werden die Daten ausgelassen, die im
	 * Zieldatum nicht existieren. Attribute, die nur im Zieldatum existieren, werden auf dem Standardwert belassen.
	 *
	 * @param source        Objekt, das als Vorlage zum kopieren benutzt wird
	 * @param target        Objekt, das mit dem Daten aus source befüllt wird
	 * @param objectManager Verwaltung korrespondierender Objekte oder null
	 * @param manager       KExDaV-Verwaltung
	 *
	 * @throws DataCopyException Wenn die Daten/Typen inkompatibel sind, z.B. versucht wird von einem Array in ein einzelnes Datum zu kopieren, oder von einem
	 *                           Referenzwert in ein Zeitattribut
	 */
	public static void copyDataTree(
			final KExDaVAttributeGroupData source,
			final KExDaVAttributeGroupData target,
			final ObjectManagerInterface objectManager,
			final ManagerInterface manager) throws DataCopyException {
		try {
			copyDataTree(source, target, source.getConnection(), target.getConnection(), objectManager, manager);
		}
		catch(IllegalArgumentException e) {
			// Einige Data-Funktionen generieren bei ungültigen Parametern eine IllegalArgumentException, die wird hier mit abgefangen
			throw new DataCopyException(e);
		}
	}

	/**
	 * Hilfsfunktion. Diese Funktion kopiert ein Datum oder eine Datenliste in ein anderes Data-Objekt. Bei Datenlisten werden die Daten ausgelassen, die im
	 * Zieldatum nicht existieren. Attribute, die nur im Zieldatum existieren, werden auf dem Standardwert belassen.
	 *
	 * @param source           Objekt, das als Vorlage zum kopieren benutzt wird
	 * @param target           Objekt, das mit dem Daten aus source befüllt wird
	 * @param sourceConnection Datenverteiler-Verbindung des Source-objekts
	 * @param targetConnection Datenverteiler-Verbindung ges Target-Objekts
	 * @param objectManager    Verwaltung korrespondierender Objekte oder null
	 * @param manager          KExDaV-Verwaltung
	 *
	 * @throws DataCopyException Wenn die Daten/Typen inkompatibel sind, z.B. versucht wird von einem Array in ein einzelnes Datum zu kopieren, oder von einem
	 *                           Referenzwert in ein Zeitattribut
	 */
	private static void copyDataTree(
			final Data source,
			final Data target,
			final ClientDavInterface sourceConnection,
			final ClientDavInterface targetConnection,
			final ObjectManagerInterface objectManager,
			final ManagerInterface manager) throws DataCopyException {
		if(source.isList() && target.isList()) {
			final Iterator<Data> src = source.iterator();
			// Erstmal nicht durch foreach ersetzen, DAF-Kompatibilität!
			// noinspection WhileLoopReplaceableByForEach
			while(src.hasNext()) {
				final Data sourceItem = src.next();
				try {
					copyDataTree(sourceItem, target.getItem(sourceItem.getName()), sourceConnection, targetConnection, objectManager, manager);
				}
				catch(NoSuchElementException ignored) {

				}
			}
		}
		else if(source.isArray() && target.isArray()) {
			final Data.Array src = source.asArray();
			final Data.Array tgt = target.asArray();
			final int length = src.getLength();
			tgt.setLength(length);
			for(int i = 0; i < length; i++) {
				copyDataTree(src.getItem(i), tgt.getItem(i), sourceConnection, targetConnection, objectManager, manager);
			}
		}
		else if(source.isPlain() && target.isPlain()) {
			copyDataValue(source, target, sourceConnection, targetConnection, objectManager, manager);
		}
	}

	/**
	 * Kopiert ein Attribut
	 *
	 * @param source           Quelle
	 * @param target           Ziel
	 * @param sourceConnection Quell-Datenverteilerverbindung
	 * @param targetConnection Ziel-Datenverteilerverbindung
	 * @param objectManager    Objektverwaltung
	 * @param manager          KExDaV-Verwaltung
	 *
	 * @throws DataCopyException Falls ein Fehler auftritt
	 */
	private static void copyDataValue(
			final Data source,
			final Data target,
			final ClientDavInterface sourceConnection,
			final ClientDavInterface targetConnection,
			final ObjectManagerInterface objectManager,
			final ManagerInterface manager) throws DataCopyException {
		final AttributeType sourceAttributeType = source.getAttributeType();
		final AttributeType targetAttributeType = target.getAttributeType();

		if(sourceAttributeType instanceof StringAttributeType && targetAttributeType instanceof StringAttributeType) {
			target.asTextValue().setText(source.asTextValue().getText());
		}
		else if(sourceAttributeType instanceof IntegerAttributeType && targetAttributeType instanceof IntegerAttributeType) {
			target.asUnscaledValue().set(source.asUnscaledValue().longValue());
		}
		else if(sourceAttributeType instanceof DoubleAttributeType && targetAttributeType instanceof DoubleAttributeType) {
			target.asScaledValue().set(source.asScaledValue().doubleValue());
		}
		else if(sourceAttributeType instanceof TimeAttributeType && targetAttributeType instanceof TimeAttributeType) {
			target.asTimeValue().setMillis(source.asTimeValue().getMillis());
		}
		else if(sourceAttributeType instanceof ReferenceAttributeType && targetAttributeType instanceof ReferenceAttributeType) {
			final SystemObject systemObject = source.asReferenceValue().getSystemObject();
			if(objectManager != null && systemObject instanceof DynamicObject) {
				// Falls die Objektreferenz auf ein dynamisches Objekt zeigt, das dynamische Objekt rüberkopieren
				objectManager.copyObjectIfNecessary(ObjectSpecification.create(systemObject, manager), sourceConnection, targetConnection);
			}
			try {
				target.asReferenceValue().setSystemObject(systemObject);
			}
			catch(IllegalArgumentException e) {
				// TAnf 4.1.3.2.2
				// Falls das Systemobjekt auf dem Zielsystem nicht gefunden werden kann, prüfen, ob der Undefiniert-Wert erlaubt ist.
				if(((ReferenceAttributeType)targetAttributeType).isUndefinedAllowed()) {
					// Falls ja, auf undefiniert setzen.
					target.asReferenceValue().setSystemObject(null);
				}
				else {
					// Falls nein, Exception werfen (Wird später zu Warnung und Betriebsmeldung)
					throw new DataCopyException(e);
				}
			}
		}
	}
}
