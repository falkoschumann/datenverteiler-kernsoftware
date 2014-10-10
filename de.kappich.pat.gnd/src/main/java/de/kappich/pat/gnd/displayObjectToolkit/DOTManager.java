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

import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.gnd.PreferencesHandler;
import de.kappich.pat.gnd.layerManagement.LayerManager;
import de.kappich.pat.gnd.linePlugin.DOTLine;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.pointPlugin.DOTPoint;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveForm;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveFormType;
import de.kappich.pat.gnd.pointPlugin.DOTPointPainter;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Der DOTManager verwaltet alle Darstellungstypen. Derzeit ist er als Singleton implementiert, um das
 * Problem der Kommunikation verschiedener Manager zu umgehen. Er ist auch ein TableModel, damit seine 
 * Inhalte im DOTManagerDialog angezeigt werden können.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 10225 $
 *
 */
@SuppressWarnings("serial")
public class DOTManager extends AbstractTableModel implements TableModel {
	// Ein Singleton, was aber nicht die ultimative Lösung sein muss; werden
	// mehrere benötigt, so müssen Änderungen zwischen diesen mitgeteilt 
	// werden, oder alternativ muss sichergestellt werden, dass überall,
	// wo solche Änderungen benötigt werden, derselbe Manager benutzt wird.
	
	private static final DOTManager _instance = new DOTManager();
	
	private boolean _initialized = false;
	
	private static final Map<String,String> _fullClassNames = new HashMap<String, String>();
	
	private final static Debug _debug = Debug.getLogger();
	
	/**
	 * Die für eine Singleton typische Methode.
	 * 
	 * @return den DOTManager
	 */
	public static DOTManager getInstance() {
		// Die Instanz wird nicht im Konstruktor initialisiert, damit sich DOTs
		// bereits als DOTManager.DOTChangeListener registrieren können.
		if ( !_instance._initialized) {
			_instance._initialized = true; // Das muss schon hier stehen, denn die Instanz wird in den folgenden Zeilen bereits benutzt!
			_instance.readFullClassNames();
			_instance.initDefaultDOTs();
			_instance.initUserDefinedDOTs();
		}
		return _instance;
	}
	
	/**
	 * Mit Hilfe dieser Methode kann man den DOTManager dazu zwingen, sich erneut zu
	 * konstruieren, was etwa nach dem Importieren von Präferenzen angezeigt ist.
	 */
	public static void refreshInstance() {
		_instance._dotList.clear();
		_instance._dotHash.clear();
		_instance._notChangables.clear();
		_instance.initDefaultDOTs();
		_instance.initUserDefinedDOTs();
	}
	
	private DOTManager () {
		// Die Instanz wird nicht im Konstruktor initialisiert, damit sich DOTs
		// bereits als DOTManager.DOTChangeListener registrieren können.
	}
	
	/**
	 * Gibt den Darstellungstypen zu dem übergebenen Namen zurück, falls ein solcher existiert, und
	 * <code>null</code> sonst.
	 * 
	 * @param eine Name
	 * @return ein Darstellungstyp mit diesem Namen oder <code>null</code>
	 */
	public DisplayObjectType getDisplayObjectType( String name) {
		return _dotHash.get(name);
	}
	
	/**
	 * Gibt den Darstellungstypen zu dem übergebenen Index zurück, falls ein solcher existiert, und
	 * wirft eine <code>IndexOutOfBoundsException</code> sonst.
	 * 
	 * @param ein Index zwischen 0 und der Anzahl der Darstellungstypen - 1
	 * @return den zugehörigen Darstellungstypen 
	 */
	public DisplayObjectType getDisplayObjectType( int index) {
		return _dotList.get( index);
	}
	
	/**
	 * Gibt alle Darstellungstypen zurück.
	 * 
	 * @return alle Darstellungstypen 
	 */
	public List<DisplayObjectType> getDisplayObjectTypes() {
		return _dotList;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn ein Darstellungstyp mit diesem Namen existiert.
	 * 
	 * @return <code>true</code> genau dann, wenn es einen Darstellungstyp mit diesem Namen gibt
	 */
	public boolean containsDisplayObjectType( String name) {
		return _dotHash.containsKey( name);
	}
	
	/**
	 * Speichert den übergebenen Darstellungstypen.
	 * 
	 * @param dot ein Darstellungstyp
	 */
	public void saveDisplayObjectType( DisplayObjectType dot) {
		if ( !_notChangables.contains(dot.getName())) {
			if ( containsDisplayObjectType( dot.getName())) {
				put( dot);
				notifyDOTChangeListenersDOTChanged(dot);
			} else {
				put( dot);
				notifyDOTChangeListenersDOTAdded(dot);
			}
			dot.putPreferences(DOTManager.getPreferenceStartPath());
			DOTManager.storeFullClassNameLookup( dot);
			fireTableDataChanged();
		}
	}
	/**
	 * Löscht den übergebenen Darstellungstypen.
	 * 
	 * @param dot ein Darstellungstyp
	 */
	public boolean deleteDisplayObjectType( DisplayObjectType dot) {
		if ( !_notChangables.contains(dot.getName()) &&
				!LayerManager.getInstance().displayObjectTypeIsUsed( dot.getName())) {
			remove( dot);
			notifyDOTChangeListenersDOTRemoved( dot.getName());
			dot.deletePreferences(DOTManager.getPreferenceStartPath());
			fireTableDataChanged();
			return true;
		}
		return false;
	}
	/**
	 * Löscht den Darstellungstypen mit dem übergebenen Namen, und gibt <code>true</code>
	 * zurück, wenn das Löschen erfolgreich war.
	 * 
	 * @param dotName ein Darstellungstypname
	 * @return <code>true</code> genau dann, wenn das Löschen erfolgreich war.
	 */
	public boolean deleteDisplayObjectType( String dotName) {
		if ( !_notChangables.contains(dotName)) {
			final DisplayObjectType dot = getDisplayObjectType( dotName);
			if ( dot != null) {
				return deleteDisplayObjectType( dot);
			}
		}
		return false;
	}
	/**
	 * 	Löscht alle Darstellungstypen.
	 */
	public void clearDisplayObjectTypes() {
		final Object[] array = _dotHash.keySet().toArray();
		for ( Object dotName : array) {
			deleteDisplayObjectType( _dotHash.get((String)dotName));
		}
	}
	
	private Class<?> getClass( final String className) {
		String fullPathName = "";
		if ( className.equals( "DOTArea")) {
			fullPathName = "de.kappich.pat.gnd.areaPlugin.DOTArea";
		} else if ( className.equals( "DOTComplex")) {
			fullPathName = "de.kappich.pat.gnd.complexPlugin.DOTComplex";
		} else if ( className.equals( "DOTLine")) {
			fullPathName = "de.kappich.pat.gnd.linePlugin.DOTLine";
		} else if ( className.equals( "DOTPoint")) {
			fullPathName = "de.kappich.pat.gnd.pointPlugin.DOTPoint";
		} else {	// eine andere als die vordefinierten Klassen muss gesucht werden
			fullPathName = _fullClassNames.get( className);
			if ( fullPathName == null || fullPathName.length() == 0) {
				_debug.error("DOTManager.getClass() wurde für die nicht unterstützte Klasse " + className + " aufgerufen.");
				return null;
			}
		}
		Class<?> c;
		try {
			c = Class.forName( fullPathName);
		}
		catch(ClassNotFoundException e) {
			_debug.error("DOTManager.getClass() wurde für die nicht auffindbare Klasse " + className + " aufgerufen.");
			return null;
		}
		return c;
	}
	
	/**
	 * Initialisiert die benutzerdefinierten Darstellungstypen.
	 */
	private boolean initUserDefinedDOTs() {
		boolean everythingFine = true;
		Preferences classPrefs = getPreferenceStartPath();
		String[] dotSubClasses;
		try {
			dotSubClasses = classPrefs.childrenNames();
		}
		catch(BackingStoreException e) {
			_debug.error("Die benutzer-definierten Darstellungstypen können nicht initialisiert werden, " + 
					"BackingStoreException: " + e.toString());
			return false;
		}
		for ( String subClassName : dotSubClasses) {
			Class<?> c;
			if ( subClassName.equals( "Classes")) {	// hier steht das Lookup für die vollständigen Klassennamen
				continue;
			}
			c = getClass( subClassName);
			if ( c == null) {
				_debug.error("Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden, " +
						"ClassNotFoundException: " + subClassName);
				everythingFine = false;
				continue;
			}
			Preferences subClassPrefs = classPrefs.node(classPrefs.absolutePath() + "/" + subClassName);
			String[] dotObjectNames;
			try {
				dotObjectNames = subClassPrefs.childrenNames();
			}
			catch(BackingStoreException e) {
				_debug.error("Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden, " +
						"BackingStoreException: " + e.toString());
				everythingFine = false;
				continue;
			}
			for ( String dotObjectName : dotObjectNames) {
				Preferences objectPrefs = subClassPrefs.node(subClassPrefs.absolutePath() + "/" + dotObjectName);
				Object object;
				try {
					object = c.newInstance();
				}
				catch(InstantiationException e) {
					_debug.error("Ein benutzer-definierter Darstellungstyp kann nicht vollständig initialisiert werden, " +
							"InstantiationException: " + e.toString());
					everythingFine = false;
					break;
				}
				catch(IllegalAccessException e) {
					_debug.error("Ein benutzer-definierter Darstellungstyp kann nicht vollständig initialisiert werden, " +
							"IllegalAccessException: " + e.toString());
					everythingFine = false;
					break;
				}
				final DisplayObjectType newDOT = (DisplayObjectType) object;
				newDOT.initializeFromPreferences( objectPrefs);
				put( newDOT);
			}
		}
		return everythingFine;
	}
	
	/**
	 * Gibt den Ausgangsknoten zum Abspeichern aller Präferenzen des DOTManagers an.
	 * 
	 * @return gibt den Ausgangsknoten zum Abspeichern aller Präferenzen des DOTManagers zurück
	 */
	public static Preferences getPreferenceStartPath() {
		// return Preferences.userRoot().node( "de/kappich/pat/gnd/DOT");	// Einzige Stelle dieses Strings!
		return PreferencesHandler.getInstance().getPreferenceStartPath().node("DOT");
	}
	
	private static void storeFullClassNameLookup( final DisplayObjectType dot) {
		final Preferences prefs = getPreferenceStartPath();
		final Preferences classesPrefs = prefs.node(prefs.absolutePath() + "/Classes");
		final String binaryClassName = dot.getClass().getName();
		final String shortClassName = binaryClassName.substring( binaryClassName.lastIndexOf('.')+1);
		classesPrefs.put(shortClassName, binaryClassName);
	}
	
	/**
	 * Gibt die Namen aller Darstellungstypen zurück.
	 * 
	 * @return gibt die Namen aller Darstellungstypen zurück
	 */
	public Object[] getDOTNames() {
		return _dotHash.keySet().toArray();
	}
	
	/*
	 * Initialisiert _fullClassNames
	 */
	private void readFullClassNames() {
		final Preferences prefs = getPreferenceStartPath();
		final Preferences classesPrefs = prefs.node( prefs.absolutePath() + "/Classes");
		String[] keys;
        try {
	        keys = classesPrefs.keys();
        }
        catch(BackingStoreException e) {
        	_debug.error("Die GND-paketfremden Darstellungstypen können nicht initialisiert werden, " + 
					"BackingStoreException: " + e.toString());
        	return;
        }
		for ( String key : keys) {
			final String value = classesPrefs.get(key, "");
			if ( value != null && (value.length() != 0)) {
				_fullClassNames.put(key, value);
			}
		}
	}
	
	/*
	 * Initialisiert alle Darstellungstypen.
	 */
	private void initDefaultDOTs() {
		initDefaultDOTsForLines();
		initDefaultDOTsForPoints();
	}
	
	private void initDefaultDOTsForLines() {
		DOTLine defaultDOT1 = new DOTLine( "Konfigurationslinie schwarz", "Eine einfache schwarze Linie");
		defaultDOT1.setPropertyStatic( null, DOTProperty.FARBE, true);
		defaultDOT1.setValueOfStaticProperty(null, DOTProperty.FARBE, "schwarz");
		defaultDOT1.setPropertyStatic( null, DOTProperty.ABSTAND, true);
		defaultDOT1.setValueOfStaticProperty(null, DOTProperty.ABSTAND, 0);
		defaultDOT1.setPropertyStatic( null, DOTProperty.STRICHBREITE, true);
		defaultDOT1.setValueOfStaticProperty(null, DOTProperty.STRICHBREITE, 1.);
		put(defaultDOT1);
		_notChangables.add(defaultDOT1.getName());
		
		DOTLine defaultDOT1_ = new DOTLine( "Konfigurationslinie hellgrau", "Eine etwas breitere graue Linie");
		defaultDOT1_.setPropertyStatic( null, DOTProperty.FARBE, true);
		defaultDOT1_.setValueOfStaticProperty(null, DOTProperty.FARBE, "hellgrau");
		defaultDOT1_.setPropertyStatic( null, DOTProperty.ABSTAND, true);
		defaultDOT1_.setValueOfStaticProperty(null, DOTProperty.ABSTAND, 0);
		defaultDOT1_.setPropertyStatic( null, DOTProperty.STRICHBREITE, true);
		defaultDOT1_.setValueOfStaticProperty(null, DOTProperty.STRICHBREITE, 5.);
		put(defaultDOT1_);
		_notChangables.add(defaultDOT1_.getName());
		
		DOTLine defaultDOT2 = new DOTLine( "Störfallzustand OLSIM 1 (grob)", "Zwei Zustände");
		defaultDOT2.setPropertyStatic( null, DOTProperty.FARBE, false);
		DisplayObjectTypeItem dItem1 = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Grün: frei bis dicht", "grün");
		defaultDOT2.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem1, 2., 4.);
		DisplayObjectTypeItem dItem2 = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Rot: zäh bis Stau", "rot");
		defaultDOT2.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem2, 5., 7.);
		defaultDOT2.setPropertyStatic( null, DOTProperty.ABSTAND, true);
		defaultDOT2.setValueOfStaticProperty(null, DOTProperty.ABSTAND, 20);
		defaultDOT2.setPropertyStatic( null, DOTProperty.STRICHBREITE, true);
		defaultDOT2.setValueOfStaticProperty(null, DOTProperty.STRICHBREITE, 5.);
		put(defaultDOT2);
		_notChangables.add(defaultDOT2.getName());
		
		DOTLine defaultDOT2_ = new DOTLine( "Störfallzustand OLSIM 1 (fein)", "Vier Zustände");
		defaultDOT2_.setPropertyStatic( null, DOTProperty.FARBE, false);
		DisplayObjectTypeItem dItem1_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Grün: frei/lebhaft", "grün");
		defaultDOT2_.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem1_, 2., 3.);
		DisplayObjectTypeItem dItem2_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Orange: dicht/zäh", "orange");
		defaultDOT2_.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem2_, 4., 5.);
		DisplayObjectTypeItem dItem3_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Rot: stockend/Stau", "rot");
		defaultDOT2_.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem3_, 6., 7.);
		DisplayObjectTypeItem dItem4_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenOLSIM1", "Situation", "Gelb: Störung/unbekannt", "gelb");
		defaultDOT2_.setValueOfDynamicProperty(null, DOTProperty.FARBE, dItem4_, 0., 1.);
		defaultDOT2_.setPropertyStatic( null, DOTProperty.ABSTAND, true);
		defaultDOT2_.setValueOfStaticProperty(null, DOTProperty.ABSTAND, 20);
		defaultDOT2_.setPropertyStatic( null, DOTProperty.STRICHBREITE, true);
		defaultDOT2_.setValueOfStaticProperty(null, DOTProperty.STRICHBREITE, 5.);
		put(defaultDOT2_);
		_notChangables.add(defaultDOT2_.getName());
	}
	
	private void initDefaultDOTsForPoints() {
		addDOTVerkehrsDatenanalyseKurz();
		addAdvancedDOTForDetectionSites();
		addTestDOTForDetectionSites();
	}
	
	private void addDOTVerkehrsDatenanalyseKurz() {
		DOTPoint newDOT = new DOTPoint( "Verkehrsdatenanalyse kurz", "Aspekt asp.agregation1Minute", 0, false);
		
		// Nur ein einfaches Rechteck
		Map<String, Object> specificInformation1 = new HashMap<String, Object>();
		specificInformation1.put( "height", new Double(16.));
		specificInformation1.put( "width", new Double(16.));
		PrimitiveForm centralRectangle = new PrimitiveForm("Rechteck: Verkehrsdatenanalyse kurz", 
				PrimitiveFormType.RECHTECK, "Aspekt asp.agregation1Minute", 
				new Point2D.Double( 0,0), specificInformation1);
		newDOT.addPrimitiveForm( centralRectangle);
		// Properties des zentralen Rechtecks: Füllung (dynamisch, Strichbreite, Tranzparenz (statisch)
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.FUELLUNG, false);
		DisplayObjectTypeItem dItem1 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Grün: über 80 km/h", "grün");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem1, 80., 255.);
		DisplayObjectTypeItem dItem2 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Orange: zwischen 30 und 80 km/h", "orange");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem2, 30., 79.);
		DisplayObjectTypeItem dItem3 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Rot: unter 30 km/h", "rot");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem3, 0., 29.);
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.STRICHBREITE, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.STRICHBREITE, new Double( 2.0));
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.TRANSPARENZ, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.TRANSPARENZ, new Integer(20));
		
		put(newDOT);
		_notChangables.add(newDOT.getName());
	}
	
	private void addAdvancedDOTForDetectionSites() {
		DOTPoint newDOT = new DOTPoint( "MQ, einfach kombinierte Darstellung", "MQDT", 30., true);
		
		// Zunächst ein einfaches Rechteck
		Map<String, Object> specificInformation1 = new HashMap<String, Object>();
		specificInformation1.put( "height", new Double(16.));
		specificInformation1.put( "width", new Double(16.));
		PrimitiveForm centralRectangle = new PrimitiveForm("Rechteck: Verkehrsdatenanalyse kurz", 
				PrimitiveFormType.RECHTECK, "Aspekt asp.agregation1Minute", 
				new Point2D.Double( 0,0), specificInformation1);
		newDOT.addPrimitiveForm( centralRectangle);
		// Properties: Füllung (dynamisch), Strichbreite, Tranzparenz (statisch)
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.FUELLUNG, false);
		DisplayObjectTypeItem dItem1 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Grün: über 80 km/h", "grün");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem1, 80., 255.);
		DisplayObjectTypeItem dItem2 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Orange: zwischen 30 und 80 km/h", "orange");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem2, 30., 79.);
		DisplayObjectTypeItem dItem3 = new DynamicDOTItem("atg.verkehrsDatenKurzZeitMq", "asp.agregation1Minute", "VKfz.Wert", "Rot: unter 30 km/h", "rot");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem3, 0., 29.);
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.STRICHBREITE, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.STRICHBREITE, new Double( 2.0));
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.TRANSPARENZ, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.TRANSPARENZ, new Integer(20));
		
		// Nun noch ein Halbkreis
		Map<String, Object> specificInformation3 = new HashMap<String, Object>();
		specificInformation3.put( "radius", new Double(8));
		specificInformation3.put( "orientation", DOTPointPainter.OBERER_HALBKREIS);
		PrimitiveForm semicircle = new PrimitiveForm("Halbkreis: Störfallzustand", 
				PrimitiveFormType.HALBKREIS, "Aspekt asp.störfallVerfahrenConstraint", 
				new Point2D.Double( 0,8), specificInformation3);
		newDOT.addPrimitiveForm( semicircle);
		// Properties: Füllung (dynamisch), Strichbreite, Tranzparenz (statisch)
		newDOT.setPropertyStatic( semicircle.getName(), DOTProperty.FUELLUNG, false);
		DisplayObjectTypeItem dItem1_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenConstraint", "Situation", "Grün: frei", "grün");
		newDOT.setValueOfDynamicProperty(semicircle.getName(), DOTProperty.FUELLUNG, dItem1_, 2., 4.);
		DisplayObjectTypeItem dItem2_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenConstraint", "Situation", "Rot: gestört", "rot");
		newDOT.setValueOfDynamicProperty(semicircle.getName(), DOTProperty.FUELLUNG, dItem2_, 5., 7.);
		DisplayObjectTypeItem dItem3_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenConstraint", "Situation", "Grau: unbekannt", "grau");
		newDOT.setValueOfDynamicProperty(semicircle.getName(), DOTProperty.FUELLUNG, dItem3_, 0., 1.);
		semicircle.setPropertyStatic( DOTProperty.STRICHBREITE, true);
		semicircle.setValueOfStaticProperty( DOTProperty.STRICHBREITE, new Double( 2.0));
		semicircle.setPropertyStatic( DOTProperty.TRANSPARENZ, true);
		semicircle.setValueOfStaticProperty( DOTProperty.TRANSPARENZ, new Integer(20));
		
		put(newDOT);
		_notChangables.add(newDOT.getName());
		
	}
	
	private void addTestDOTForDetectionSites() {
		DOTPoint newDOT = new DOTPoint( "MQ, Testdarstellung 1", "MQDT 1", 70., true);
		// Der originäre Punkt
		Map<String, Object> specificInformation0 = new HashMap<String, Object>();
		PrimitiveForm point = new PrimitiveForm("Der Punkt", 
				PrimitiveFormType.PUNKT, "Verortung", 
				new Point2D.Double( 0.,0.), specificInformation0);
		// Properties des Punktes: Durchmesser und Farbe, hier beide statisch
		newDOT.addPrimitiveForm( point);
		newDOT.setPropertyStatic( point.getName(), DOTProperty.DURCHMESSER, true);
		newDOT.setValueOfStaticProperty( point.getName(), DOTProperty.DURCHMESSER, new Double(5.));
		newDOT.setPropertyStatic( point.getName(), DOTProperty.FARBE, true);
		newDOT.setValueOfStaticProperty( point.getName(), DOTProperty.FARBE, Color.DARK_GRAY);
		
		// Das zentrale Rechteck
		Map<String, Object> specificInformation1 = new HashMap<String, Object>();
		specificInformation1.put( "height", new Double(30.));
		specificInformation1.put( "width", new Double(30.));
		PrimitiveForm centralRectangle = new PrimitiveForm("Zentrales Rechteck", 
				PrimitiveFormType.RECHTECK, "LOS-Rechteck", 
				new Point2D.Double( 0,0), specificInformation1);
		newDOT.addPrimitiveForm( centralRectangle);
		// Properties des zentralen Rechtecks: Füllung (dynamisch, Strichbreite, Tranzparenz (statisch)
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.FUELLUNG, false);
		DisplayObjectTypeItem dItem1_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenMARZ", "Situation", "Grün: frei", "grün");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem1_, 2., 4.);
		DisplayObjectTypeItem dItem2_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenMARZ", "Situation", "Rot: Stau", "ror");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem2_, 5., 7.);
		DisplayObjectTypeItem dItem3_ = new DynamicDOTItem("atg.störfallZustand", "asp.störfallVerfahrenMARZ", "Situation", "Grau: Störung/unbekannt", "grau");
		newDOT.setValueOfDynamicProperty(centralRectangle.getName(), DOTProperty.FUELLUNG, dItem3_, 0., 1.);
		
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.STRICHBREITE, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.STRICHBREITE, new Double( 2.0));
		newDOT.setPropertyStatic( centralRectangle.getName(), DOTProperty.TRANSPARENZ, true);
		newDOT.setValueOfStaticProperty( centralRectangle.getName(), DOTProperty.TRANSPARENZ, new Integer(20));
		
		// Der Kreis neben dem Rechteck
		Map<String, Object> specificInformation2 = new HashMap<String, Object>();
		specificInformation2.put( "radius", new Double(10));
		PrimitiveForm circle = new PrimitiveForm("Ein Kreis", 
				PrimitiveFormType.KREIS, "Güte-Kreis", 
				new Point2D.Double( 25,5), specificInformation2);
		newDOT.addPrimitiveForm( circle);
		// Properties des Kreises: Füllung, Strichbreite, Tranzparenz (erstmal statisch)
		newDOT.setPropertyStatic( circle.getName(), DOTProperty.FUELLUNG, true);
		newDOT.setValueOfStaticProperty( circle.getName(), DOTProperty.FUELLUNG, Color.GREEN);
		newDOT.setPropertyStatic( circle.getName(), DOTProperty.STRICHBREITE, true);
		newDOT.setValueOfStaticProperty( circle.getName(), DOTProperty.STRICHBREITE, new Double( 1.0));
		newDOT.setPropertyStatic( circle.getName(), DOTProperty.TRANSPARENZ, true);
		newDOT.setValueOfStaticProperty( circle.getName(), DOTProperty.TRANSPARENZ, new Integer(60));
		// Der Halbkreis über dem Rechteck
		Map<String, Object> specificInformation3 = new HashMap<String, Object>();
		specificInformation3.put( "radius", new Double(15));
		specificInformation3.put( "orientation", DOTPointPainter.OBERER_HALBKREIS);
		PrimitiveForm semicircle = new PrimitiveForm("Ein Halbkreis", 
				PrimitiveFormType.HALBKREIS, "Spannungshalbkreis", 
				new Point2D.Double( 0,15), specificInformation3);
		newDOT.addPrimitiveForm( semicircle);
		// Properties des Halbkreises: Füllung, Strichbreite, Tranzparenz (erstmal statisch)
		newDOT.setPropertyStatic( semicircle.getName(), DOTProperty.FUELLUNG, true);
		newDOT.setValueOfStaticProperty( semicircle.getName(), DOTProperty.FUELLUNG, Color.BLUE);
		newDOT.setPropertyStatic( semicircle.getName(), DOTProperty.STRICHBREITE, true);
		newDOT.setValueOfStaticProperty( semicircle.getName(), DOTProperty.STRICHBREITE, new Double( 2.0));
		newDOT.setPropertyStatic( semicircle.getName(), DOTProperty.TRANSPARENZ, true);
		newDOT.setValueOfStaticProperty( semicircle.getName(), DOTProperty.TRANSPARENZ, new Integer(0));
		// Der Text
		Map<String, Object> specificInformation4 = new HashMap<String, Object>();
		PrimitiveForm text = new PrimitiveForm("Name, PID oder Id", 
				PrimitiveFormType.TEXTDARSTELLUNG, "Name", 
				new Point2D.Double( -15,-35), specificInformation4);
		newDOT.addPrimitiveForm( text);
		// Properties des Textes: Text (dynamisch), Farbe, Größe und Textstil (statisch)
		newDOT.setPropertyStatic( text.getName(), DOTProperty.TEXT, false);
		DisplayObjectTypeItem textItem = new DynamicDOTItem(
				"atg.verkehrsDatenKurzZeitMq", 
				"asp.agregation1Minute", 
				"VKfz.Wert", "VekehrsdatenKurzZeitMq", 
				DOTPointPainter.DYNAMIC_ATTRIBUTE_SCALED);
		newDOT.setValueOfDynamicProperty(text.getName(), DOTProperty.TEXT, textItem, 0., 255.);
		
		newDOT.setPropertyStatic( text.getName(), DOTProperty.FARBE, true);
		newDOT.setValueOfStaticProperty( text.getName(), DOTProperty.FARBE, Color.BLUE);
		newDOT.setPropertyStatic( text.getName(), DOTProperty.GROESSE, true);
		newDOT.setValueOfStaticProperty( text.getName(), DOTProperty.GROESSE, new Integer( 20));
		newDOT.setPropertyStatic( text.getName(), DOTProperty.TEXTSTIL, true);
		newDOT.setValueOfStaticProperty( text.getName(), DOTProperty.TEXTSTIL, Font.ITALIC);
		put(newDOT);
		_notChangables.add(newDOT.getName());
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn der Darstellungstyp veränderbar ist, was genau dann der Fall ist,
	 * wenn er nicht im Programmcode, sondern von einem Benutzer definiert wurde, und <code>false</code> sonst.
	 * 
	 * @param dot ein Darstellungstyp
	 * @return <code>true</code> genau dann, wenn der Darstellungstyp veränderbar ist
	 */
	public boolean isChangeable ( DisplayObjectType dot) {
		return !(_notChangables.contains( dot.getName()));
	}
	
	private void put( DisplayObjectType dot) {
		remove( dot);
		_dotList.add( dot);
		_dotHash.put(dot.getName(), dot);
	}
	
	private void remove( DisplayObjectType dot) {
		final DisplayObjectType removed = _dotHash.remove(dot.getName());
		if ( removed != null ) {
			int index = 0;
			for ( DisplayObjectType dType : _dotList) {
				if ( dType.getName().equals( dot.getName())) {
					_dotList.remove( index);
					break;
				}
				index++;
			}
		}
		if ( _dotHash.size() != _dotList.size() ) {
			_debug.warning( "Der DOTManager ist intern durcheinander!");
		}
	}
	
	/*
	 * Gehört zur Implementaion des TableModel und gibt den Spaltennamen der Spalte mit dem übergebenen Index zurück.
	 */
	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}
	/*
	 * Gehört zur Implementaion des TableModel und gibt die Anzahl der Spalten an.
	 */
	public int getColumnCount() {
		return columnNames.length;
	}
	/*
	 * Gehört zur Implementaion des TableModel und gibt die Anzahl der Zeilen an.
	 */
	public int getRowCount() {
		return _dotList.size();
	}
	/*
	 * Gehört zur Implementaion des TableModel und gibt den Wert der durch die Indizes angebenen Zelle zurück.
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		return _dotList.get(rowIndex).getName();
	}
	
	/**
	 * Ein Interface für Listener, die über Änderungen von Darstellungstypen informiert werden wollen.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 10225 $
	 *
	 */
	public interface DOTChangeListener {
		/**
		 * Diese Methode wird aufgerufen, wenn der übergebene Darstellungstyp hinzugefügt wurde.
		 * 
		 * @param displayObjectType ein Darstellungstyp
		 */
		void displayObjectTypeAdded( final DisplayObjectType displayObjectType);
		/**
		 * Diese Methode wird aufgerufen, wenn der übergebene Darstellungstyp geändert wurde.
		 * 
		 * @param displayObjectType ein Darstellungstyp
		 */
		void displayObjectTypeChanged( final DisplayObjectType displayObjectType);
		/**
		 * Diese Methode wird aufgerufen, wenn der genannte Darstellungstyp gelöscht wurde.
		 * 
		 * @param displayObjectType ein Darstellungstyp
		 */
		void displayObjectTypeRemoved( final String displayObjectTypeName);
	}
	
	/**
	 * Fügt das übergebene Objekt zur Liste aller auf Darstellungstypänderungen angemeldeten Objekte hinzu.
	 * 
	 * @param listener ein DOTChangeListener
	 */
	public void addDOTChangeListener(DOTChangeListener listener) {
		_dotChangeListeners.add(listener);
	}
	/**
	 * Löscht das übergebene Objekt aus der Liste aller auf Darstellungstypänderungen angemeldeten Objekte.
	 * 
	 * @param listener ein DOTChangeListener
	 */
	public void removeDOTChangeListener(DOTChangeListener listener) {
		_dotChangeListeners.remove(listener);
	}
	
	/**
	 * Benachrichtigt alle auf Darstellungstypänderungen angemeldeten Objekte über das Hinzufügen eines 
	 * Darstellungstypen.
	 * 
	 * @param displayObjectType ein Darstellungstyp
	 */
	public void notifyDOTChangeListenersDOTAdded( final DisplayObjectType displayObjectType) {
		for ( DOTChangeListener changeListener : _dotChangeListeners) {
			changeListener.displayObjectTypeAdded( displayObjectType);
		}
	}
	
	/**
	 * Benachrichtigt alle auf Darstellungstypänderungen angemeldeten Objekte über das Verändern eines 
	 * Darstellungstypen.
	 * 
	 * @param displayObjectType ein Darstellungstyp
	 */
	public void notifyDOTChangeListenersDOTChanged( final DisplayObjectType displayObjectType) {
		for ( DOTChangeListener changeListener : _dotChangeListeners) {
			changeListener.displayObjectTypeChanged( displayObjectType);
		}
	}
	/**
	 * Benachrichtigt alle auf Darstellungstypänderungen angemeldeten Objekte über das
	 * Entfernen eines Darstellungstypen.
	 * 
	 * @param displayObjectType ein Darstellungstyp
	 */
	public void notifyDOTChangeListenersDOTRemoved( final String displayObjectTypeName) {
		for ( DOTChangeListener changeListener : _dotChangeListeners) {
			changeListener.displayObjectTypeRemoved( displayObjectTypeName);
		}
	}
	
	private static void addFullClassNames( final List<String> fullClassNames) {
		if ( fullClassNames == null || (fullClassNames.size() == 0)) {
			return;
		}
		for ( String fullClassName : fullClassNames) {
			final int lastIndexOf = fullClassName.lastIndexOf('.');
			if ( (lastIndexOf == -1) || (fullClassName.length()-1 == lastIndexOf)) { // kann nicht sein
				continue;
			}
			String className = fullClassName.substring( lastIndexOf+1);
			_fullClassNames.put( className, fullClassName);
		}
	}
	
	/**
	 * Der PluginManager ruft diese Methode auf, wenn externe Plugins hinzugefügt wurden. Dies wird
	 * statt einem Listener-Mechanismus bevorzugt, weil bei einem solchen sichergestellt werden müsste,
	 * dass der DOTManager bereits instanziiert ist.
	 * 
	 * @param plugins die Liste der hinzugefügten Plugins
	 */
	public static void pluginsAdded( final List<String> plugins) {
		final List<String> dotClasses = new ArrayList<String>();
		for ( String plugin : plugins) {
			final Class<?> pluginClass;
			try {
				pluginClass = Class.forName( plugin);
			}
			catch(ClassNotFoundException e) {
				continue;
			}
			final Object pluginObject;
			try {
				pluginObject = pluginClass.newInstance();
			}
			catch(InstantiationException e) {
				_debug.error( "Fehler im PluginManager: die Klasse '" + plugin + 
				"' kann nicht instanziiert werden.");
				continue;
			}
			catch(IllegalAccessException e) {
				_debug.error( "Fehler im PluginManager: auf die Klasse '" + plugin + 
				"' kann nicht zugegriffen werden.");
				continue;
			}
			if ( pluginObject instanceof DisplayObjectTypePlugin) {
				final DisplayObjectTypePlugin dotPlugin = (DisplayObjectTypePlugin) pluginObject;
				final DisplayObjectType displayObjectType = dotPlugin.getDisplayObjectType();
				dotClasses.add( displayObjectType.getClass().getName());
			}
		}
		DOTManager.addFullClassNames(dotClasses);
	}
	
	private static String[] columnNames = {"Name des Darstellungstyps"};
	private final List<DisplayObjectType> _dotList = new ArrayList<DisplayObjectType>();
	private final Hashtable<String, DisplayObjectType> _dotHash = new Hashtable<String, DisplayObjectType> ();
	private final Set<String> _notChangables = new HashSet<String>();
	private final List<DOTChangeListener> _dotChangeListeners= new ArrayList<DOTChangeListener>();
}
