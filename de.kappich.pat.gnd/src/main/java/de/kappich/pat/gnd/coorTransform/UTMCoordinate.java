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
package de.kappich.pat.gnd.coorTransform;

/**
 * Eine Klasse für UTM-Koordinaten.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8067 $
 *
 * Nach Vorlage von bitCtrl.
 * 
 */
public class UTMCoordinate {
	
	/**
	 * Eine Klasse zur Verkapselung der zwei Halbkugeln.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8067 $
	 *
	 */
	public static final class UTMHemisphere {
		
		public static final UTMHemisphere HORDHALBKUGEL = new UTMHemisphere(0);
		
		public static final UTMHemisphere SUEDHALBKUGEL = new UTMHemisphere(1);
		
		@SuppressWarnings("unused")
		private final int _hemisphere;
		
		private UTMHemisphere(int h) {
			_hemisphere = h;
		}
	}
	
	private static final int UTM_ZONE_MIN = 1;
	
	private static final int UTM_ZONE_MAX = 60;
	
	/**
	 * Der Rechtswert bzw. die x-Koordinate.
	 */
	private double _x;
	
	/**
	 * Der Hochwert bzw. die y-Koordinate.
	 */
	private double _y;
	
	/**
	 * die Zone (1-60).
	 */
	private int _utmZone;
	
	/**
	 * die Hemisphäre.
	 */
	private UTMHemisphere _utmHemisphere = UTMHemisphere.HORDHALBKUGEL;
	
	/**
	 * Konstruktor ohne Wertevorgaben.
	 */
	public UTMCoordinate () {}
	
	/**
	 * Konstruktor für eine UTM-Koordinate auf der nördlichen Erdhalbkugel.
	 * 
	 * @param x der x- oder Ostwert
	 * @param y der y- oder Nordwert
	 * @param zone die UTM-Zone
	 */
	public UTMCoordinate(double x, double y, int zone) {
		if ((zone < UTM_ZONE_MIN) || (zone > UTM_ZONE_MAX)) {
			throw new IllegalArgumentException("Der Wert für die Zone ist ungültig!");
		}
		
		_x = x;
		_y = y;
		_utmZone = zone;
		_utmHemisphere = UTMHemisphere.HORDHALBKUGEL;
	}
	
	/**
	 *  Konstruktor für eine UTM-Koordinate.
	 *  
	 * @param x der x- oder Ostwert
	 * @param y der y- oder Nordwert
	 * @param zone die UTM-Zone
	 * @param hemisphere die Hemisphäre
	 */
	public UTMCoordinate(double x, double y, int zone, UTMHemisphere hemisphere) {
		if ((zone < UTM_ZONE_MIN) || (zone > UTM_ZONE_MAX)) {
			throw new IllegalArgumentException("Der Wert für die Zone ist ungültig!");
		}
		_x = x;
		_y = y;
		_utmZone = zone;
		_utmHemisphere = hemisphere;
	}
	
	/**
	 * Gibt die Hemisphäre zurück.
	 * 
	 * @return gibt die Hemisphäre zurück
	 */
	public UTMHemisphere getHemisphere() {
		return _utmHemisphere;
	}
	
	/**
	 * Gibt die x-Koordinate zurück, die auch als Ostwert bezeichnet wird.
	 * 
	 * @return gibt die x-Koordinate oder Ostwert zurück
	 */
	public double getX() {
		return _x;
	}
	
	/**
	 * Setzt den x-Koordinate, die auch als Ostwert bezeichnet wird.
	 * 
	 * @param x die neue x-Koordinate bzw. Ostwert
	 */
	public void setX( double x) {
		_x = x;
	}
	
	/**
	 * Setzt den y-Koordinate, die auch als Nordwert bezeichnet wird.
	 * 
	 * @param y die neue y-Koordinate bzw. Nordwert 
	 */
	public void setY( double y) {
		_y = y;
	}
	
	/**
	 * Gibt die y-Koordinate zurück, die auch als Nordwert bezeichnet wird.
	 * 
	 * @return gibt die y-Koordinate bzw. Nordwert zurück
	 */
	public double getY() {
		return _y;
	}
	
	/**
	 * Setzt die Zone.
	 * 
	 * @param zone die neue UTM-Zone
	 */
	public void setZone( final int zone) {
		_utmZone = zone;
	}
	
	/**
	 * Gibt die UTM-Zone zurück.
	 * 
	 * @return gibt die UTM-Zone zurück
	 */
	public int getZone() {
		return _utmZone;
	}
	
	/**
	 * Setzt die Hemisphäre.
	 * 
	 * @param setzt die Hemisphäre
	 */
	public void setHemisphere(UTMHemisphere hemisphere) {
		this._utmHemisphere = hemisphere;
	}
}

