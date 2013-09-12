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
package de.kappich.pat.gnd.displayObjectToolkit;

/**
 * Die Klasse ist ein typsicheres Enum-Muster für die Visualisierungseigenschaften der darzustellenden
 * Objekte oder ihrer Grundfiguren wie Farbe, Transparenz, Abstand, Textstil usw.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 */


public class DOTProperty {

	private final String _name;

    private DOTProperty(String name) {
	    super();
	    _name = name;
    }
    
    /**
     * Gibt nur den Namen der Eigenschaft zurück.
     */
    @Override
    public String toString() {
    	return _name;
    }
    
    /**
     * Die Implementation hier verhindert versehentliches Überschreiben (nach J. Bloch).
     */
    @Override
    public final boolean equals( Object o) {
    	return super.equals( o);
    }
   
    /**
     * Die Implementation hier verhindert versehentliches Überschreiben (nach J. Bloch).
     */
    @Override
    public final int hashCode() {
    	return super.hashCode();
    }
    
    /**
     * Gibt die gewünschte Eigenschaft zurück, wenn sie existiert, und <code>null</code> sonst.
     */
    public static DOTProperty getProperty( String name) {
    	if ( name.equals( "Farbe") || name.equals( "color")) {
    		return FARBE;
    	} else if ( name.equals( "Abstand") || name.equals( "distance")) {
    		return ABSTAND;
    	} else if ( name.equals( "Strichbreite") || name.equals( "strokewidth")) {
    		return STRICHBREITE;
    	} else if ( name.equals( "Durchmesser") || name.equals( "diameter")) {
    		return DURCHMESSER;
    	} else if ( name.equals( "Füllung") || name.equals( "fill")) {
    		return FUELLUNG;
    	} else if ( name.equals( "Größe") || name.equals( "size")) {
    		return GROESSE;
    	} else if ( name.equals( "Transparenz") || name.equals( "transparency")) {
    		return TRANSPARENZ;
    	} else if ( name.equals( "Text") || name.equals( "text")) {
    		return TEXT;
    	} else if ( name.equals( "Textstil") || name.equals( "textstyle")) {
    		return TEXTSTIL;
    	}
    	return null;
    }
    
    public static final DOTProperty FARBE = new DOTProperty( "Farbe");
    public static final DOTProperty ABSTAND = new DOTProperty( "Abstand");
    public static final DOTProperty STRICHBREITE = new DOTProperty( "Strichbreite");
    public static final DOTProperty DURCHMESSER = new DOTProperty( "Durchmesser");
    public static final DOTProperty FUELLUNG = new DOTProperty( "Füllung");
    public static final DOTProperty GROESSE = new DOTProperty( "Größe");
    public static final DOTProperty TRANSPARENZ = new DOTProperty( "Transparenz");
    public static final DOTProperty TEXT = new DOTProperty("Text");
    public static final DOTProperty TEXTSTIL = new DOTProperty( "Textstil");
}
