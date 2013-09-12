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
 *  Eine Klasse für WGS84-Koordinaten. Könnte auch für andere Ellipsoid-Koordinaten verwendet werden.
 *  
 * @author Kappich Systemberatung
 * @version $Revision: 7809 $
 *
 * @author BitCtrl Systems GmbH, Gieseler
 * 
 */
public class WGS84Coordinate {

	/**
	 * kleinster zulässiger Wert für die Länge.
	 */
	public static final double MIN_LAENGE = -180;

	/**
	 * größter zulässiger Wert für die Länge.
	 */
	public static final double MAX_LAENGE = 180;

	/**
	 * kleinster zulässiger Wert für die Breite.
	 */
	public static final double MIN_BREITE = -90;

	/**
	 * größter zulässiger Wert für die Breite.
	 */
	public static final double MAX_BREITE = 90;

	/**
	 * Länge.
	 */
	private double laenge;

	/**
	 * Breite.
	 */
	private double breite;

	/**
	 * Konstruktor für eine WGS84-Koordinate.
	 * 
	 * @param laenge
	 *            geographische Länge in Dezimalgrad
	 * @param breite
	 *            geographische Breite in Dezimalgrad
	 */
	public WGS84Coordinate(double laenge, double breite) {
		if (testBreite(breite)) {
			throw new IllegalArgumentException(
					"Der Wert für die Breite ist ungültig!"); //$NON-NLS-1$
		}

		if (testLaenge(laenge)) {
			throw new IllegalArgumentException(
					"Der Wert für die Länge ist ungültig!"); //$NON-NLS-1$
		}

		this.laenge = laenge;
		this.breite = breite;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final WGS84Coordinate other = (WGS84Coordinate) obj;
		if (Double.doubleToLongBits(breite) != Double.doubleToLongBits(other.breite)) {
			return false;
		}
		if (Double.doubleToLongBits(laenge) != Double.doubleToLongBits(other.laenge)) {
			return false;
		}
		return true;
	}

	/**
	 * Gibt die geographische Breite zurück.
	 * 
	 * @return geographische Breite in Dezimalgrad
	 */
	public double getBreite() {
		return breite;
	}

	/**
	 * Gibt die geographische Länge zurück.
	 * 
	 * @return geographische Länge in Dezimalgrad
	 */
	public double getLaenge() {
		return laenge;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(breite);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(laenge);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * Setzt die geographische Breite.
	 * 
	 * @param neuebreite
	 *            neue geographische Breite in Dezimalgrad
	 */
	public void setBreite(double neuebreite) {
		if (testBreite(neuebreite)) {
			throw new IllegalArgumentException(
					"Der Wert für die Breite ist ungültig!"); //$NON-NLS-1$
		}

		this.breite = neuebreite;
	}

	/**
	 * Setzt die geographische Länge.
	 * 
	 * @param neuelaenge
	 *            neue geographische Länge in Dezimalgrad
	 */
	public void setLaenge(double neuelaenge) {
		if (testLaenge(neuelaenge)) {
			throw new IllegalArgumentException(
					"Der Wert für die Länge ist ungültig!"); //$NON-NLS-1$
		}

		this.laenge = neuelaenge;
	}

	/**
	 * Testet die Breite auf Gültigkeit.
	 * 
	 * @param tbreite
	 *            Breite
	 * @return true, wenn ok sonst false
	 */
	private boolean testBreite(double tbreite) {
		return ((tbreite < MIN_BREITE) || (tbreite > MAX_BREITE));
	}

	/**
	 * Testet die Länge auf Gültigkeit.
	 * 
	 * @param tlaenge
	 *            Länge
	 * @return true, wenn ok sonst false
	 */
	private boolean testLaenge(double tlaenge) {
		return ((tlaenge < MIN_LAENGE) || (tlaenge > MAX_LAENGE));
	}
}


