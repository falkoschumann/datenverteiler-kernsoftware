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
 * Richtung eines Online-Daten-Austausches
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9087 $
 */
public enum DataExchangeStrategy {
	/**
	 * Übertragung von einer Quelle auf dem Lokalsystem zu einem Empfänger auf dem Remotesystem. KExDaV meldet sich folglich als Empfänger und Quelle an.
	 */
	SourceReceiver,
		/**
	 * Übertragung von einem Sender auf dem Lokalsystem zu einer Senke auf dem Remotesystem. KExDaV meldet sich folglich als Senke und Sender an.
	 */
	SenderDrain,
		/**
	 * Übertragung von einer Quelle auf dem Remotesystem zu einem Empfänger auf dem Lokalsystem. KExDaV meldet sich folglich als Quelle und Empfänger an.
	 */
	DrainSender,
		/**
	 * Übertragung von einer Quelle auf dem Remotesystem zu einem Empfänger auf dem Lokalsystem. KExDaV meldet sich folglich als Sender und Senke an.
	 */
	ReceiverSource
}
