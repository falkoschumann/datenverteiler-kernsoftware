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

package de.kappich.pat.gnd.layerManagement;

import de.kappich.pat.gnd.displayObjectToolkit.DOTCollection;
import de.kappich.pat.gnd.displayObjectToolkit.DOTManager;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;

import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Ein Klasse, die für einen geo-referenzierten Objekttyp festgelegt, wie dessen Objekte dargestellt werden sollen.
 * <p>
 * Ein Layer hat vier wesentliche Bestandteile: seinen Namen, der als eindeutige Kennung
 * verwendet wird, einen Infotext, den Namen einer von GeoReferenzObjekt abgeleiteten Klasse
 * und eine DOTCollection, die die Informationen über die Darstellung des Layers beinhaltet.
 *  
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 */
public class Layer {
	
	/**
	 * Ein Default-Konstruktor, der z.B. für die Initialisierung mit initializeFromPreferences()
	 * benötigt wird.
	 */
	public Layer() {}

	/**
	 * Im folgenden Konstruktor werden dem Layer seine ersten drei Bestandteile mitgegeben, während die
	 * DOTCollection über verschiedene Methoden später bearbeitet werden kann.
	 * 
	 * @param layerName der Name des Layers
	 * @param info der Infotext zum Layer
	 * @param geoReferenceType der Namen einer von GeoReferenzObjekt abgeleiteten Klasse
	 */
	public Layer(String layerName, String info, String geoReferenceType) {
		_name = layerName;
		_info = info;
		_geoReferenceType = geoReferenceType;
	}
	
	/**
	 * Der Getter für den Namen.
	 * 
	 * @return den Namen
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Der Setter für den Namen.
	 * 
	 * @param layerName der Name des Layers
	 */
	public void setName(final String layerName) {
		_name = layerName;
	}
	
	/**
	 * Der Getter für den Infotext.
	 * 
	 * @return der Infotext des Layers
	 */
	public String getInfo() {
		return _info;
	}
	
	/**
	 * Der Setter für den Infotext.
	 * 
	 * @param der Infotext
	 */
	public void setInfo(String info) {
		_info = info;
	}
	
	/**
	 * Der Getter für den Namen der von GeoReferenzObject abgeleiteten Klasse, deren Objekte
	 * der Layer darstellt.
	 * 
	 * @return der Name der von GeoReferenzObject abgeleiteten Klasse
	 */
	public String getGeoReferenceType() {
		return _geoReferenceType;
	}
	
	/**
	 * Der Setter für den Namen der von GeoReferenzObject abgeleiteten Klasse, deren Objekte
	 * der Layer darstellt.
	 * 
	 * @param geoReferenceType der Name der von GeoReferenzObject abgeleiteten Klasse
	 */
	public void setGeoReferenceType(String geoReferenceType) {
		_geoReferenceType = geoReferenceType;
	}
	
	/**
	 * Mit dieser Methode kann man der DOTCollection des Layers einen Darstellungstypen
	 * hinzufügen, und muss dabei das Intervall für das er gelten soll, angeben. Der
	 * Darstellungstyp muss von <code>null</code> verschieden sein, und lowerScale muss mindestens
	 * so groß wie upperScale sein (es handelt sich um die Xe der 1:X-Werte von Maßstäben).
	 */
	public void addDisplayObjectType( DisplayObjectType type, int lowerScale, int upperScale) {
		_dotCollection.addDisplayObjectType(type, lowerScale, upperScale);
	}
	
	/**
	 * Durch Aufruf dieser Methode wird die DOTCollection des Layers geleert.
	 */
	public void clearDisplayObjectTypes() {
		_dotCollection.clear();
	}

	/**
	 * Gibt die DOTCollection des Layers zurück.
	 * 
	 * @return die DOTCollection des Layers
	 */
    public DOTCollection getDotCollection() {
    	return _dotCollection;
    }
    /**
     * Setzt die DOTCollection des Layers.
     * 
     * @param dotCollection die neue DOTCollection des Layers
     */
    public void setDotCollection(DOTCollection dotCollection) {
    	_dotCollection = dotCollection;
    }
    
    /**
     * Gibt einen Darstellungstypen für den übergebenen 1:X-Maßstabswert zurück, falls es in der 
     * DOTCollection einen solchen für diesen Wert gibt. Gibt es mehr als einen, so ist nicht 
     * festgelegt, welchen man erhält. Gibt es keinen, so erhält man null als Rückgabewert.
     * 
     * @param scale ein Maßstabswert
     * @return ein zugehöriger Darstellungstyp
     */
	public DisplayObjectType getDisplayObjectType( int scale) {
		return _dotCollection.getDisplayObjectType(scale);
	}
	
	/**
	 * Speichert die Präferenzen des Layers unter dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Präferenzen gespeichert werden
	 */
	public void putPreferences ( Preferences prefs) {
		deletePreferences( prefs);
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
		if ( _info == null) {
			objectPrefs.put(INFO, "");
		} else {
			objectPrefs.put(INFO, _info);
		}
		objectPrefs.put(GEO_REFERENCE_TYP, _geoReferenceType);
		Preferences collectionPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/" + DOT_COLLECTION);
		_dotCollection.putPreferences( collectionPrefs);
	}
	
	/**
	 * Löscht die Präferenzen des Layers unter dem Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Präferenzen gelöscht werden
	 */
	public void deletePreferences (Preferences prefs) {
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
		try {
	        objectPrefs.removeNode();
        }
        catch(BackingStoreException e) {
	        
	        throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
        }
	}
	/**
	 * Initialisiert den Layer aus den Präferenzen unter dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Präferenzen gesucht werden
	 */
	public boolean initializeFromPreferences(Preferences prefs) {
		_name = prefs.name();
		_info = prefs.get(INFO, "");
		_geoReferenceType = prefs.get(GEO_REFERENCE_TYP, "");
		Preferences collectionPrefs = prefs.node( prefs.absolutePath() + "/" + DOT_COLLECTION);
		return _dotCollection.initializeFromPreferences(collectionPrefs, DOTManager.getInstance());
	}
	
	/**
	 * Gibt die Menge aller Namen aller Farben, die von den Darstellungstypen in der DOTCollection
	 * des Layers verwendet werden, zurück.
	 * 
	 * @return die Namen aller benutzten Farben
	 */
	public Set<String> getUsedColors() {
		return _dotCollection.getUsedColors();
	}
	
	@Override
    public String toString() {
		String s = new String();
		s += "[Layer: " + _name + ", Info: " + _info + ", GeoReferenzTyp: " + _geoReferenceType + 
			", DOTCollection: " + _dotCollection.toString() + "]";
		return s;
	}
	
	/**
	 * Diese Methode gibt eine Kopie des Layers zurück.
	 * 
	 * @return die Kopie
	 */
	public Layer getCopy() {
		final Layer copy = new Layer(_name, _info, _geoReferenceType);
		copy.setDotCollection( _dotCollection.getCopy());
		return copy;
	}
	
	private String _name;
	private String _info;
	private String _geoReferenceType;
	private DOTCollection _dotCollection = new DOTCollection();
	
	private static final String INFO = "INFO";
	private static final String GEO_REFERENCE_TYP = "GEO_REFERENCE_TYP";
	private static final String DOT_COLLECTION = "DOT_COLLECTION";
	
}
