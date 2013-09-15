/*
 * Copyright 2009 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.kappich.pat.gnd.
 * 
 * de.kappich.pat.gnd is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.gnd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.gnd; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package de.kappich.pat.gnd.pluginInterfaces;

import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.gnd.MapPane;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Das Interface, das von der Klasse eines Plugins implementiert werden muss, die für das Zeichnen verantwortlich ist.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public interface DisplayObjectPainter {
	
	/**
	 * Implementationen dieser Methode werden aufgerufen, wenn das DisplayObject gezeichnet werden soll.
	 * Innerhalb seiner Implemantation sollte für <code>displayObject</code> die Methode 
	 * {@link de.kappich.pat.gnd.displayObjectToolkit.DisplayObject.#getCoordinates 
	 * DisplayObject.getCoordinates(int type)} 
	 * aufgerufen werden. Diese bewerkstelligt einen lazy Cache; sind die Daten nicht vorhanden, so wird die 
	 * Erzeugung an die Methode {@link #getCoordinates} delegiert, wo auch die Auswertung des Integer-Wertes 
	 * stattfindet.
	 * 
	 * @param mapPane die Kartenansicht
	 * @param g2D ein Graphics2D-Objekt zum Zeichnen
	 * @param displayObject das DisplayObject
	 * @param selected <code>true</true> genau dann, wenn das DisplayObject selektiert ist
	 */
	public void paintDisplayObject( MapPane mapPane, Graphics2D g2D, 
			DisplayObject displayObject, boolean selected);
	
	/**
	 * Diese Methode berechnet aus den übergebenen Koordinaten die Koordinaten zu dem übergebenen Typ.
	 * Dieser Typ erlaubt es dem Programmierer unterschiedliche Koordinaten für das Objekt zu bekommen.
	 * Diese Methode wird nur in DisplayObject.getCoordinates( int type) aufgerufen, und die Ergebnisse
	 * werden dort gecached. Damit ergeben sich folgende Randbedingungen für den Plugin-Programmierer:
	 * einerseits muss er keinen eigenen Cache implementieren und andererseits kann er selber entscheiden, 
	 * wofür die unterschiedlichen Integer-Werte stehen. Eine denkbare Anwendung wären unterschiedlich 
	 * genaue Koordinaten für verschiedene Zoom-Stufen zwecks Optimierung der Zeichengeschwindigkeit.
	 * <p>
	 * In der 4 Standard-Plugins wird nur bei Linien von verschiedenen Typen Gebrauch gemacht: der Typ 
	 * stellt hier die Verschiebung der Koordinaten dar. Alle anderen Implementation rufen die Methode 
	 * nur mit dem Wert 0 auf.
	 * 
	 * @param coordinates die Originalkoordinaten
	 * @param type der gewünschte Koordinatentyp
	 * @return die gewünschten Koordinaten
	 */
	public List<Object> getCoordinates( List<Object> coordinates, int type);
	
	/**
	 * Diese Methode gibt das umgebende Rechteck des DisplayObjects für den angebenen Typen zurück.
	 * Über die Bedeutung des Integer-Wertes <code>type</code> wird in {@link #getCoordinates} informiert.
	 * Diese Methode wird nur aus DisplayObject.getBoundingRectangle( int type) heraus aufgerufen,
	 * wo bereits ein lazy Cache für die Rückgabewerte installiert ist.  
	 * 
	 * @param displayObject das DisplayObject
	 * @return das umgebende Rechteck
	 */
	public Rectangle getBoundingRectangle( DisplayObject displayObject, int type);
}
