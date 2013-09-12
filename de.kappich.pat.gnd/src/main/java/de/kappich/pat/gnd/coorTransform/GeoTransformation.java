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
 * Eine Klasse zur Koordinatentransformation.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8094 $
 * 
 * License: GPL. Copyright 2007 by Immanuel Scholz and others, see and taken from
 * http://josm.openstreetmap.de/browser/trunk/src/org/openstreetmap/josm/data/projection/UTM.java
 *
 */
public final class GeoTransformation {
	
	/**
	 * UTM-Skalierungsfaktor.
	 */
	private static final double UTM_SCALE_FACTOR = 0.9996;
	
	/** Ellipsoid Halbachse a. */
	private static final double WGS84_A = 6378137.0;
	/** Ellipsoid Halbachse b. */
	private static final double WGS84_B = 6356752.314;
	
	/**
	 * mittlerer Erdradius.
	 */
	private static final double R_M = 6371000.8;
	
	/**
	 * Einige Grˆﬂen des WGS84-Ellipsoiden
	 */
	private static final double n = (WGS84_A - WGS84_B) / (WGS84_A + WGS84_B);
	private static final double n_square = n * n;
	private static final double n_power_3 = n_square * n;
	private static final double n_power_4 = n_square * n_square;
	private static final double n_power_5 = n_power_4 * n;
	
	/**
	 * Einige Koeffizienten, die in arcLengthOfMeridian benutzt werden.
	 */
	private static final double alpha = ((WGS84_A + WGS84_B) / 2.0) * (1.0 + (n_square / 4.0) + (n_power_4 / 64.0));
	private static final double beta = (-3.0 * n / 2.0) + (9.0 * n_power_3 / 16.0) + (-3.0 * n_power_5 / 32.0);
	private static final double gamma = (15.0 * n_square / 16.0) + (-15.0 * n_power_4 / 32.0);
	private static final double delta = (-35.0 * n_power_3 / 48.0) + (105.0 * n_power_5 / 256.0);
	private static final double epsilon = (315.0 * n_power_4 / 512.0);
	
	/**
	 * Die Meridianbogenl‰nge
	 * 
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed. New York: Springer-Verlag Wien, 1994.
	 * 
	 * @param phi
	 *            Breite des Punkts im Bogenmaﬂ
	 * 
	 * @return  Die Distanz vom ƒquator bis zu einem Punkt gegebener Breite.
	 * 
	 */
	private static double arcLengthOfMeridian(double phi) {
		final double result;
		result = alpha * (phi + (beta * Math.sin(2.0 * phi))
				+ (gamma * Math.sin(4.0 * phi))
				+ (delta * Math.sin(6.0 * phi)) + (epsilon * Math.sin(8.0 * phi)));
		return result;
	}
	
	/**
	 * Einige Koeffizienten, die in footpointLatitude benutzt werden.
	 */
	/*  Gl. 10.22 */
	private static final double alphaF = ((WGS84_A + WGS84_B) / 2.0) * (1 + (n_square / 4) + (n_power_4 / 64));
	/* Gl. 10.22 */
	private static final double betaF = (3.0 * n / 2.0) + (-27.0 * n_power_3 / 32.0) + (269.0 * n_power_5 / 512.0);
	/* Gl. 10.22 */
	private static final double gammaF = (21.0 * n_square / 16.0) + (-55.0 * n_power_4 / 32.0);
	/* Gl. 10.22 */
	private static final double deltaF = (151.0 * n_power_3 / 96.0) + (-417.0 * n_power_5 / 128.0);
	/* Gl. 10.22 */
	private static final double epsilonF = (1097.0 * n_power_4 / 512.0);
	
	/**
	 * Die Fuﬂpunkt-Breite.
	 * 
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed. New York: Springer-Verlag Wien, 1994.
	 * 
	 * @param y
	 *            Der UTM-Hochwert.
	 * @return Die Fuﬂpunkt-Breite.
	 * 
	 */
	private static double footpointLatitude(double y) {
		/* Gl. 10.23 */
		final double yF = y / alphaF;
		
		/* Gl. 10.21 */
		final double result = yF + (betaF * Math.sin(2.0 * yF))
		+ (gammaF * Math.sin(4.0 * yF)) + (deltaF * Math.sin(6.0 * yF))
		+ (epsilonF * Math.sin(8.0 * yF));
		
		return result;
	}
	
	/**
	 * Umrechnung von WGS84-L‰nge und -Breite im Bogenmaﬂ in eine UTM-Koordinate.
	 * Der Zentralmeridian wird aus der Zone der UTM-Koordinate berechnet.
	 * 
	 * @param lat
	 *            Die Breite im Bogenmaﬂ.
	 * @param lon
	 *            Die L‰nge im Bogenmaﬂ.
	 * 
	 */
	private static void latLonToUTMXY(double lat, double lon, UTMCoordinate utm) {
		mapLatLonToXY(lat, lon, uTMCentralMeridian(utm.getZone()), utm);
		/* Adjust easting and northing for UTM system. */
		utm.setX( utm.getX() * UTM_SCALE_FACTOR + 500000.0);
		utm.setY( utm.getY() * UTM_SCALE_FACTOR);
		if (utm.getY() < 0.0) {
			utm.setY( utm.getY() + 10000000.0);
		}
	}
	
	/* ep2 */
	private static final double ep2 = (Math.pow(WGS84_A, 2.0) - Math.pow(WGS84_B, 2.0)) / Math.pow(WGS84_B, 2.0);
	
	/**
	 * Umrechnung von WGS84-L‰nge und -Breite im Bogenmaﬂ in eine UTM-Koordinate, wobei
	 * der Zentralmeridian angegeben wird.
	 * 
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed. New York: Springer-Verlag Wien, 1994.
	 * 
	 * @param phi
	 *             Die Breite im Bogenmaﬂ.
	 * @param lambda
	 *             Die L‰ngee im Bogenmaﬂ.
	 * @param lambda0
	 *            L‰nge des Zentralmeridians im Bogenmaﬂ.
	 * 
	 */
	private static void mapLatLonToXY(double phi, double lambda, double lambda0, UTMCoordinate utm) {
		final double nF, nu2, t, t2, t4, t6, deltaLambda, dL2, dL3, dL4, dL5, dL6, dL7, dL8, c2, c3, c4, c5, c6, c7, c8;
		final double l3coef, l4coef, l5coef, l6coef, l7coef, l8coef;
		
		/* nu2 */
		final double cosPhi = Math.cos(phi);
		nu2 = ep2 * cosPhi * cosPhi;
		
		/* N */
		nF = WGS84_A * WGS84_A / (WGS84_B * Math.sqrt(1 + nu2));
		
		/* t und Potenzen */
		t = Math.tan(phi);
		t2 = t * t;
		t4 = t2 * t2;
		t6 = t4 * t2;
		
		/* l und Potenzen */
		deltaLambda = lambda - lambda0;
		dL2 = deltaLambda * deltaLambda;
		dL3 = deltaLambda * dL2;
		dL4 = dL2 * dL2;
		dL5 = dL2 * dL3;
		dL6 = dL3 * dL3;
		dL7 = dL2 * dL5;
		dL8 = dL4 * dL4;
		
		/* Potenzen von cosPhi */
		c2 = cosPhi * cosPhi;
		c3 = cosPhi * c2;
		c4 = c2 * c2;
		c5 = c2 * c3;
		c6 = c3 * c3;
		c7 = c2 * c5;
		c8 = c4 * c4;
		
		/* Koeffizienten */
		l3coef = 1.0 - t2 + nu2;
		l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);
		l5coef = 5.0 - 18.0 * t2 + t4 + 14.0 * nu2 - 58.0 * t2 * nu2;
		l6coef = 61.0 - 58.0 * t2 + t4 + 270.0 * nu2 - 330.0 * t2 * nu2;
		l7coef = 61.0 - 479.0 * t2 + 179.0 * t4 - t6;
		l8coef = 1385.0 - 3111.0 * t2 + 543.0 * t4 - t6;
		
		/* Rechtswert */
		utm.setX( nF * cosPhi * deltaLambda
				+ (nF / 6.0 * c3 * l3coef * dL3)
				+ (nF / 120.0 * c5 * l5coef * dL5)
				+ (nF / 5040.0 * c7 * l7coef * dL7)
		);
		
		/* Hochwert */
		utm.setY( arcLengthOfMeridian(phi)
				+ (t / 2.0 * nF * c2 * dL2)
				+ (t / 24.0 * nF * c4 * l4coef * dL4)
				+ (t / 720.0 * nF * c6 * l6coef * dL6)
				+ (t / 40320.0 * nF * c8 * l8coef * dL8)
		);
		
		return;
	}
	
	/**
	 * Berechnung einer WGS84-Koordinate (L‰nge/Breite) aus einem x- und y-Wert
	 * einer UTM-Koordinate unter Angabe des Zentralmeridians.
	 * 
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed. New York: Springer-Verlag Wien, 1994.
	 * 
	 * @param x
	 *            Der Rechtswert in Metern.
	 * @param y
	 *            Der Hochwert in Metern.
	 * @param lambda0
	 *            L‰nge des Zentralmeridians im Bogenmaﬂ.
	 * 
	 */
	private static void mapXYToLatLon(double x, double y, double lambda0, WGS84Coordinate wgs) {
		double phif, nF, nfpow, nuf2, tf, tf2, tf4, cf;
		double x1frac, x2frac, x3frac, x4frac, x5frac, x6frac, x7frac, x8frac;
		double x2poly, x3poly, x4poly, x5poly, x6poly, x7poly, x8poly;
		
		phif = footpointLatitude(y);
		
		/* cos (phif) */
		cf = Math.cos(phif);
		
		/* nuf2 */
		nuf2 = ep2 * cf * cf;
		
		/* Nf und Nfpow */
		nF = (WGS84_A * WGS84_A)  / (WGS84_B * Math.sqrt(1 + nuf2));
		nfpow = nF;
		
		/* tf */
		tf = Math.tan(phif);
		tf2 = tf * tf;
		tf4 = tf2 * tf2;
		
		x1frac = 1.0 / (nfpow * cf);
		
		nfpow *= nF;
		x2frac = tf / (2.0 * nfpow);
		
		nfpow *= nF; 
		x3frac = 1.0 / (6.0 * nfpow * cf);
		
		nfpow *= nF; 
		x4frac = tf / (24.0 * nfpow);
		
		nfpow *= nF; 
		x5frac = 1.0 / (120.0 * nfpow * cf);
		
		nfpow *= nF; 
		x6frac = tf / (720.0 * nfpow);
		
		nfpow *= nF; 
		x7frac = 1.0 / (5040.0 * nfpow * cf);
		
		nfpow *= nF; 
		x8frac = tf / (40320.0 * nfpow);
		
		x2poly = -1.0 - nuf2;
		
		x3poly = -1.0 - 2 * tf2 - nuf2;
		
		final double nuf4 = nuf2 * nuf2;
		x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * nuf4 - 9.0 * tf2 * nuf4;
		x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
		x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2 + 162.0 * tf2 * nuf2;
		x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
		x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);
		
		final double x2 = x * x;
		final double x4 = x2 * x2;
		final double x6 = x4 * x2;
		final double x8 = x4 * x4;
		
		/* Breite */
		wgs.setBreite( phif + x2frac * x2poly * x2 + x4frac * x4poly
				* x4 + x6frac * x6poly * x6 + x8frac * x8poly * x8
		);
		
		/* L‰nge */
		wgs.setLaenge( lambda0 + x1frac * x + x3frac * x3poly * x * x2
				+ x5frac * x5poly * x* x4 + x7frac * x7poly	* x * x6
		);
		
		return;
	}
	
	/**
	 * Berechnung des UTM-Zentralmeridians der entsprechenden UTM-Zone.
	 * 
	 * @param zonep
	 *            Eine ganze Zahl aus [1,60].
	 * @return der Zentralmeridian der UTM-Zone
	 * 
	 */
	private static double uTMCentralMeridian(int zonep) {
		if ( _centralMeridianIsSetFromOutside) {
			return _centralMeridian;
		}
		return Math.toRadians(-183.0 + (zonep * 6.0));
	}
	
	private static boolean _centralMeridianIsSetFromOutside = false;
	
	private static double _centralMeridian;
	
	/**
	 * Setzt den Zentralmeridian von auﬂen und unterbindet damit die automatische interne Berechnung.
	 * 
	 * @param centralMeridian der neue Zentralmeridian
	 */
	public static void setCentralMeridianFromOutside( final double centralMeridian) {
		_centralMeridian = Math.toRadians(centralMeridian);
		_centralMeridianIsSetFromOutside = true;
	}
	
	/**
	 * Transformiert die UTM-Koordinaten nach WGS84.
	 * 
	 * @param utm
	 *            Koordinaten in UTM
	 * @return die transformierten Koordinaten
	 */
	public static WGS84Coordinate uTMToWGS84(UTMCoordinate utm) {
		boolean southhemi;
		if (utm.getHemisphere() == UTMCoordinate.UTMHemisphere.SUEDHALBKUGEL) {
			southhemi = true;
		} else {
			southhemi = false;
		}
		WGS84Coordinate wgs = new WGS84Coordinate(0., 0.);
		uTMXYToLatLon(utm, southhemi, wgs);
		return new WGS84Coordinate(Math.toDegrees(wgs.getLaenge()), Math.toDegrees(wgs.getBreite()));
	}
	
	/**
	 * Berechnet eine WGS84-Koordinate (L‰nge/Breite) aus einer UTM-Koordinate.
	 * 
	 * @param x
	 *            Der Rechtswert in Metern.
	 * @param y
	 *            Der Hochwert in Metern.
	 * @param utmzone
	 *            Die UTM-Zone des Punktes.
	 * @param southhemi
	 *            True, falls der Punkt auf der S¸dhalbkugel liegt.
	 * 
	 */
	private static void uTMXYToLatLon(UTMCoordinate utm, boolean southhemi, WGS84Coordinate wgs) {
		double cmeridian;
		double ly = utm.getY();
		double lx = utm.getX() - 500000.0;
		lx /= UTM_SCALE_FACTOR;
		if (southhemi) {
			ly -= 10000000.0;
		}
		ly /= UTM_SCALE_FACTOR;
		cmeridian = uTMCentralMeridian(utm.getZone());
		mapXYToLatLon(lx, ly, cmeridian, wgs);
	}
	
	/**
	 * Projeziert eine WGS84-Koordinate nach UTM.
	 * 
	 * @param wgs84laenge
	 *            geographische L&auml;nge in Dezimalgrad
	 * @param wgs84breite
	 *            geographische Breite in Dezimalgrad
	 * @return die transformierten Koordinaten
	 */
	public static void wGS84ToUTM(double wgs84laenge,
			double wgs84breite, UTMCoordinate utm) {
		double lon = Math.toRadians(wgs84laenge);
		double lat = Math.toRadians(wgs84breite);
		final double a = (wgs84laenge + 180.0) / 6;
		if ( a>0.) {
			utm.setZone( 1+ (int) a);
		} else {
			utm.setZone( (int) Math.floor(a) + 1);
		}
		latLonToUTMXY(lat, lon, utm);
	}
	
	/**
	 * Berechnet die L‰nge des Kreisbogens auf der Erdoberfl‰che zu dem Winkel.
	 * 
	 * @param arc
	 *            Winkel in Grad
	 * @return die L‰nge des Kreisbogens auf der Erdoberfl‰che zu
	 *            dem Winkel.
	 */
	public static double lengthOfCircleArc(double arc) {
		return (arc * R_M * 2 * Math.PI / 360);
	}
	
	/**
	 * privater Konstruktor.
	 */
	private GeoTransformation() {}
}

