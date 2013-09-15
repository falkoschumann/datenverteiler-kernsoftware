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

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9274 $
 */
public class Constants {

	/** Remote-Anwendungs-Name */
	public static final String RemoteApplicationName = "KExDaVRemoteApplication";

	/**
	 * Zeit in ms, die mindestens gewartet wird, bis eine Parameterübertragung in Gegenrichtung zu der vorherigen Parameterübertragung vorgenommen wird. Dieser
	 * Wert sollte größer sein, als die Dauer, die eine solche Übertragung üblicherweise braucht. Wird dieser Wert zu klein gewählt, kann es passieren, dass auf
	 * beiden Systemen annährend gleichzeitig Parameter gesetzt werden und diese ständig Wechselseitig ausgetauscht werden. Ein großer Wert bremst die Übertragung
	 * aus, falls von beiden Systemen häufig wechselseitig Parameter gesetzt werden
	 */
	public static final int ParameterExchangeReverseDelay = 30000;
	/**
	 * Wenn insgesamt mehr als die angegebene Anzahl an Datensätzen auf das Versenden warten wird eine Warnung ausgegeben
	 */
	public static final int WarnSendQueueCapacity = 100000;

	/**
	 * Minimaler Abstand zwischen den Warnungen wenn die Queue voll ist
	 */
	public static final int WarnSendQueueInterval = 60000;

	/** Pids */
	public static class Pids {

		/** Dynamischer Objekt-Typ */
		public static final String TypeDynamicObject = "typ.dynamischesObjekt";

		/** KExDaV-Typ */
		public static final String TypeKExDaV = "typ.kexdav";

		/** KExDaV-Parameter-Attributgruppe */
		public static final String AtgSpecificationKExDaV = "atg.spezifikationKExDaV";

		/** KExDaV-Trigger-Attributgruppe */
		public static final String AtgTriggerKExDaV = "atg.triggerKExDaV";

		/** Soll-Aspekt */
		public static final String AspectParameterDesired = "asp.parameterSoll";

		/** Ist-Aspekt */
		public static final String AspectParameterActual = "asp.parameterIst";

		/** Vorgabe-Aspekt */
		public static final String AspectParameterTarget = "asp.parameterVorgabe";

		/** Anfrage-Aspekt */
		public static final String AspectRequest = "asp.anfrage";

		/** Eigenschaften-Aspekt */
		public static final String AspectProperties = "asp.eigenschaften";

		/** Konfigurationsdaten an dynamischen Objekten **/
		public static final String AttributeGroupKExDaVConfigData = "atg.kexdavAustauschObjekt";
	}
}
