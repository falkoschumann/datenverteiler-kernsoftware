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
package de.kappich.pat.gnd.colorManagement;

import de.kappich.pat.gnd.viewManagement.ViewManager;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Die Farbenverwaltung verwaltet die verfügbaren Farben.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8094 $
 *
 */
public class ColorManager {
	
	/**
	 * Gibt die Instanz des Singletons zurück.
	 * @return gibt den ColorManager zurück
	 */
	public static ColorManager getInstance() {
		return _instance;
	}
	
	/**
	 * Zwingt den ColorManager seine Instanz neu zu konstruieren, was etwa nach
	 * dem Import von Preferences notwendig ist.
	 */
	public static void refreshInstance() {
		_instance._colorMap.clear();
		_instance._colorNameMap.clear();
		_instance._basicColorNames.clear();
		_instance.addBasicColors();
		_instance.initializeFromPreferences();
	}
	
	/**
	 * Fügt eine Farbe hinzu. Der Name wird nur kleingeschrieben verwendet. Mit dem Flag 
	 * storeInPreferences bestimmt man, ob die Farbe dauerhaft gespeichert wird.
	 * 
	 * @param name der Name der Farbe, der nur kleingeschrieben verwendet wird
	 * @param color die Farbe
	 * @param storeInPreferences soll die Farbe in den Präferenzen gespeichert werden 
	 */
	public void addColor( String name, Color color, boolean storeInPreferences) {
		final String lowerCasedName = name.toLowerCase();
		_colorMap.put( lowerCasedName, color);
		_colorNameMap.put( color, lowerCasedName);
		if ( storeInPreferences) {
			putPreference( getPreferenceStartPath(), name, color);
		}
	}
	
	/**
	 * Löscht die Farbe mit diesem Namen; gibt <code>true</code> zurück, wenn dies erfolgreich ist,
	 * und <code>false</code> sonst, was etwa dann möglich ist, wenn die Farbe in Benutzung ist.
	 * 
	 * @param name der Name der zu löschenden Farbe, keine Beachtung von Klein-/Großschreibung
	 * @return <code>true</code> genau dann, wenn das Löschen erfolgreich war.
	 */
	public boolean deleteColor ( String name) {
		final String lowerCaseName = name.toLowerCase();
		Set<String> usedColors = ViewManager.getInstance().getUsedColors();
		if ( usedColors.contains( lowerCaseName)) {
			return false;
		}
		final Color removedColor = _colorMap.remove( lowerCaseName);
		_colorNameMap.remove( removedColor);
		deletePreference( getPreferenceStartPath(), name);
		return true;
	}
	
	/**
	 * Gibt die Farbe des übergebenen Namens zurück, oder <code>null</code>, wenn eine solche Farbe
	 * nicht existiert.
	 * 
	 * @param name der Name der gesuchten Farbe, keine Beachtung von Klein-/Großschreibung
	 * @return die gesuchte Farbe oder <code>null</code>, wenn sie nicht existiert
	 */
	public Color getColor( String name) {
		final String lowerCase = name.toLowerCase();
		return _colorMap.get( lowerCase);
	}
	
	/**
	 * Gibt den Namen der übergebenen Farbe zurück.
	 * 
	 * @param color die Farbe
	 * @return der Name der farbe oder <code>null</code>, wenn die Farbe nicht existiert
	 */
	public String getColorName( Color color) {
		return _colorNameMap.get( color);
	}
	
	/**
	 * Man erhält die Namen aller Farben.
	 * 
	 * @return die Namen aller Farben
	 */
	public Object[] getColorNames() {
		return _colorMap.keySet().toArray();
	}
	
	/**
	 * Beantwortet die Frage, ob eine Farbe mit diesem Namen schon definiert ist. 
	 * 
	 * @param name der Name einer Farbe, keine Beachtung von Klein-/Großschreibung
	 * @return <code>true</code> wenn die Farbe existiert, <code>false</code> sonst
	 */
	public boolean hasColor( String name) {
		return _colorMap.containsKey( name.toLowerCase());
	}
	
	private static final ColorManager _instance = new ColorManager();
	
	private ColorManager () {
		addBasicColors();
		initializeFromPreferences();
	}
	
	private void addBasicColors() {
		addColor("schwarz", Color.black, false);
		_basicColorNames.add( "schwarz");
		addColor("blau", Color.blue, false);
		_basicColorNames.add( "blau");
		addColor("zyan", Color.cyan, false);
		_basicColorNames.add( "zyan");
		addColor("dunkelgrau", Color.darkGray, false);
		_basicColorNames.add( "dunkelgrau");
		addColor("grau", Color.gray, false);
		_basicColorNames.add( "grau");
		addColor("grün", Color.green, false);
		_basicColorNames.add( "grün");
		addColor("hellgrau", Color.lightGray, false);
		_basicColorNames.add( "hellgrau");
		addColor("magenta", Color.magenta, false);
		_basicColorNames.add( "magenta");
		addColor("orange", Color.orange, false);
		_basicColorNames.add( "orange");
		addColor("pink", Color.pink, false);
		_basicColorNames.add( "pink");
		addColor("rot", Color.red, false);
		_basicColorNames.add( "rot");
		addColor("weiß", Color.white, false);
		_basicColorNames.add( "weiß");
		addColor("gelb", Color.yellow, false);
		_basicColorNames.add( "gelb");
		final Color noColor = new Color(0.f,0.f,0.f,0.f);
		addColor("keine", noColor, false);
		_basicColorNames.add( "keine");
	}
	
	private static Preferences getPreferenceStartPath() {
		return Preferences.userRoot().node("de/kappich/pat/gnd/Color");
	}
	
	private void putPreference( Preferences prefs, String name, Color color) {
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + name);
		objectPrefs.putInt(COLOR_RED, color.getRed());
		objectPrefs.putInt(COLOR_BLUE, color.getBlue());
		objectPrefs.putInt(COLOR_GREEN, color.getGreen());
		objectPrefs.putInt(COLOR_ALPHA, color.getAlpha());
	}
	
	private void deletePreference( Preferences prefs, String name) {
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + name);
		try {
	        objectPrefs.removeNode();
        }
        catch(BackingStoreException e) {
        }
	}
	
	private void initializeFromPreferences() {
		Preferences classPrefs = getPreferenceStartPath();
		String[] childrenNames;
		try {
			childrenNames = classPrefs.childrenNames();
        }
        catch(BackingStoreException e) {
	        
	        throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
        }
        for ( String colorName : childrenNames) {
        	Preferences colorPrefs = classPrefs.node(classPrefs.absolutePath() + "/" + colorName);
        	int red = colorPrefs.getInt(COLOR_RED, -1);
        	int green = colorPrefs.getInt(COLOR_GREEN, -1);
    		int blue = colorPrefs.getInt(COLOR_BLUE, -1);
    		int alpha = colorPrefs.getInt(COLOR_ALPHA, -1);
    		if ( (red != -1) && (green != -1) && (blue != -1) && (alpha != -1)) {
    			Color color = new Color( red, green, blue, alpha);
    			addColor( colorName, color, false);
    		}
        }
	}
	
	/**
	 * Löscht alle Farben, die weder in {@link java.awt.Color} vordefiniert noch in Benutzung sind.
	 */
	public void clearColors() {
		Set<String> deleteTheseColors = new HashSet<String>();
		Set<String> usedColors = ViewManager.getInstance().getUsedColors();
		for ( String colorName : _colorMap.keySet()) {
			if ( !_basicColorNames.contains( colorName) && !usedColors.contains( colorName)) {
				deleteTheseColors.add( colorName);
			}
		}
		for ( String colorName : deleteTheseColors) {
			deleteColor( colorName);
		}
	}
	
	private final Map<String, Color> _colorMap  = new HashMap<String, Color>();
	private final Map<Color, String> _colorNameMap = new HashMap<Color, String>();
	private final Set<String> _basicColorNames = new HashSet<String>();
	
	private static final String COLOR_RED = "COLOR_RED";
	private static final String COLOR_BLUE = "COLOR_BLUE";
	private static final String COLOR_GREEN = "COLOR_GREEN";
	private static final String COLOR_ALPHA = "COLOR_ALPHA";
}
