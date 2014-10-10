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
package de.kappich.pat.gnd.viewManagement;

import de.kappich.pat.gnd.gnd.GenericNetDisplay;
import de.kappich.pat.gnd.gnd.PreferencesHandler;
import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.layerManagement.LayerManager;
import de.kappich.pat.gnd.layerManagement.LayerManager.LayerManagerChangeListener;

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Die Ansichtsverwaltung ist ein Singleton, das unter anderem das Interface TableModel implementiert,
 * damit es vom {@link ViewDialog Ansichtsdialog} angezeigt werden kann.
 * <p>
 * Diese Klasse ist wie auch die {@link DOTManager Darstellungstyp-} und die {@link LayerManager Layerverwaltung}
 * als Singleton implementiert, um den Programmieraufwand für die Kommunikation verschiedener Objekte dieser 
 * Klasse untereinader, der andernfalls notwendig wäre, einzusparen. Es kann in Zukunft notwendig sein, dies
 * zu ändern.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 10225 $
 *
 */
@SuppressWarnings("serial")
public class ViewManager extends AbstractTableModel implements TableModel, LayerManagerChangeListener {

	private static final ViewManager _instance = new ViewManager();

	/**
	 * Die übliche getInstance-Methode eines Singletons.
	 */
	public static ViewManager getInstance() {
		return _instance;
	}
	
	/**
	 * Mit Hilfe dieser Methode kann man den ViewManager dazu zwingen, sich erneut zu
	 * konstruieren, was etwa nach dem Importieren von Präferenzen sinnvoll ist.
	 */
	public static void refreshInstance() {
		_instance._views.clear();
		_instance._notChangables.clear();
		_instance._viewsHashMap.clear();
		_instance.initDefaultViews();
		_instance.initUserDefinedViews();
	}
	
	private ViewManager() {
		initDefaultViews();
		initUserDefinedViews();
		LayerManager.getInstance().addChangeListener( this);
	}
	
	private void initDefaultViews() {
		View defaultView = getView("Vordefinierte Ansicht 1");
		if ( defaultView == null) {
			List<ViewEntry> viewEntries = new ArrayList<ViewEntry>();
			defaultView = new View("Vordefinierte Ansicht 1", viewEntries);
		}
		
		initDefaultViewsForPoints( defaultView);
		initDefaultViewsForLines( defaultView);
		defaultView.setSomethingChanged( false);
		
		internalAdd(defaultView);
		_notChangables.add( defaultView.getName());
	}
	
	private void initDefaultViewsForLines( final View defaultView) {
		String s;
		s = "Störfällzustand OLSIM 1";
		final Layer layer1 = LayerManager.getInstance().getLayer(s);
		if ( layer1 == null) {
			_debug.warning( "ViewManager.initDefaultViewsForLines: ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		} else {
			defaultView.addLayer(layer1);
		}
		s = "Straßennetz";
		final Layer layer2 = LayerManager.getInstance().getLayer( s);
		if ( layer2 == null) {
			_debug.warning( "ViewManager.initDefaultViewsForLines(): ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		} else {
			defaultView.addLayer(layer2);
		}
		s = "Messquerschnitte (Testlayer)";
		final Layer layer3 = LayerManager.getInstance().getLayer( s);
		if ( layer3 == null) {
			_debug.warning( "ViewManager.initDefaultViewsForLines(): ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		}
	}
	
	private void initDefaultViewsForPoints( final View defaultView) {
		String s;
		Layer layer;
		
		s = "Messquerschnitte";
		layer = LayerManager.getInstance().getLayer(s);
		if ( layer == null) {
			_debug.warning( "ViewManager.initDefaultViewsForPoints: ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		} else {
			defaultView.addLayer(layer);
		}
		s = "Messquerschnitte (erweitert)";
		layer = LayerManager.getInstance().getLayer(s);
		if ( layer == null) {
			_debug.warning( "ViewManager.initDefaultViewsForPoints: ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		}
		
		s = "Messquerschnitte (Testlayer)";
		layer = LayerManager.getInstance().getLayer(s);
		if ( layer == null) {
			_debug.warning( "ViewManager.initDefaultViewsForPoints: ein Layer namens '" + s + "' konnte nicht gefunden werden.");
		}
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn die übergebene Ansicht eine benutzer-definierte Ansicht ist, 
	 * und demzufolge verändert werden kann; andernfalls ist der Rückgabewert <code>false</code>, wenn 
	 * nämlich die Ansicht im Programmkode festgelegt wurde, und deshalb unveränderbar ist.
	 * <p>
	 * Die aktuell eingeblendete Ansicht kann immer verändert werden, auch wenn sie im Programmkode festgelegt
	 * wurde; allerdings werden die Änderungen nicht gespeichert.
	 * 
	 * @param view eine Ansicht
	 * @return ist die Ansicht änderbar?
	 */
	public boolean isChangeable(final View view) {
		return !(_notChangables.contains( view.getName()));
	}
	
	/**
	 * Fügt die übergebene Ansicht hinzu, wenn noch keine Ansicht mit demselben Namen existiert.
	 * 
	 * @param view eine Ansicht
	 */
	public void addView(View view) {
		if ( _viewsHashMap.containsKey(view.getName())) {
			throw new IllegalArgumentException( "Ein View dieses Namens existiert bereits");
		}
		_views.add(view);
		_viewsHashMap.put( view.getName(), view);
		view.putPreferences(getPreferenceStartPath());
		final int index = _views.size()-1;
		fireTableRowsInserted(index, index);
	}
	
	private void internalAdd(View view) {
		_views.add(view);
		_viewsHashMap.put( view.getName(), view);
	}
	
	/**
	 * Entfernt die übergebene Ansicht aus der Ansichtverwaltung, die den Namen von view besitzt,
	 * falls eine solche existiert und diese nicht im Programmkode definiert wurde.
	 * 
	 * @param view eine Ansicht
	 */
	public boolean removeView( View view) {
		final String name = view.getName();
		if ( !_notChangables.contains(name)) {
			final int index = remove( name);
			if ( index > -1) {
				view.deletePreferences(getPreferenceStartPath());
				fireTableRowsDeleted(index, index);
				return true;
			}
		}
		return false;
	}
	/**
	 * Leert die Ansichtsverwaltung komplett, also inklusive der im Programmkode definierten Ansichten.
	 */
	public void clearViews() {
		for ( View view : _views) {
			removeView( view);
		}
	}
	
	private void initUserDefinedViews() {
		Preferences classPrefs = getPreferenceStartPath();
		String[] views;
		try {
			views = classPrefs.childrenNames();
        }
        catch(BackingStoreException e) {
        	_debug.error("Die benutzer-definierten Ansichten können nicht initialisiert werden, " + 
					"BackingStoreException: " + e.toString());
			return;
        }
        for ( String viewName : views) {
        	Preferences viewPrefs = classPrefs.node(classPrefs.absolutePath() + "/" + viewName);
        	View view = new View();
        	view.initializeFromPreferences(viewPrefs, LayerManager.getInstance());
        	internalAdd( view);
        }
	}
	
	/**
	 * Gibt <code>true</code> zurück, falls die Ansichtsverwaltung eine Ansicht mit diesem Namen besitzt,
	 * und <code>false</code> sonst.
	 * 
	 * @param viewName ein Ansichtsname
	 * @return <code>true</code> genau dann, wenn ein View mit dem übergebenen Namen existiert
	 */
	public boolean hasView( String viewName) {
		return _viewsHashMap.containsKey( viewName);
	}
	
	/**
	 * Gibt die Ansicht der Ansichtsverwaltung zurück, falls die Ansichtsverwaltung eine 
	 * Ansicht mit diesem Namen besitzt, und <code>null</code> sonst.
	 * 
	 * @param viewName ein Ansichtsname
	 * @return die Ansicht oder <code>null</code>
	 */
	public View getView( String viewName) {
		return _viewsHashMap.get(viewName);
	}
	
	/**
	 * Gibt die i-te Ansicht zurück (die Zählung beginnt bei 0).
	 * 
	 * @param i ein Index
	 * @return die i-te Ansicht
	 */
	public View getView( int i) {
		return _views.get( i);
	}
	
	/**
	 * Bestimmt den Index der Ansicht in der Ansichtsverwaltung.
	 * 
	 * @param viewName der Ansichtsname
	 * @return der Index der Ansicht
	 */
	public int getIndexOfView( final String viewName) {
		if ( viewName == null) {
			return -1;
		}
		int index = 0;
		for ( View view : _views) {
			if ( viewName.equals( view.getName())) {
				return index;
			}
			index++;
		}
		return -1;
	}
	
	/**
	 * Gibt eine Menge aller Ansichtsnamen zurück.
	 * 
	 * @return eine Menge aller Ansichtsnamen 
	 */
	public Set<String> getViewNames() {
		return _viewsHashMap.keySet();
	}
	/**
	 * Gibt die Anzahl der Spalten der Tabellendarstellung zurück.
	 */
	public int getColumnCount() {
		return columnNames.length;
	}
	/**
	 * Gibt die Anzahl der Zeilen der Tabellendarstellung zurück.
	 */
	public int getRowCount() {
		return _views.size();
	}
	
	/**
	 * Gibt den Spaltennamen der entsprechenden Spalte in der Tabellendarstellung zurück.
	 * 
	 * @param columnIndex ein Spaltenindex
	 */
	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	/**
	 * Gibt den Wert des Feldes in der Tabellendarstellung zurück.
	 * 
	 * @param rowIndex ein Zeilenindex
	 * @param columnIndex ein Spaltenindex
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		return _views.get(rowIndex).getName();
	}
	
	/**
	 * Gibt den Ausgangspunkt der Präferenzen der Ansichtsverwaltung zurück.
	 * 
	 * @return der Knoten, unter dem die Views gespeichert werden
	 */
	public static Preferences getPreferenceStartPath() {
//		return Preferences.userRoot().node("de/kappich/pat/gnd/").node(kvPid).node("View");
		return PreferencesHandler.getInstance().getPreferenceStartPath().node("View");
	}
	
	private int remove( final String viewName) {
		int index = 0;
		for ( View view : _views) {
			if ( viewName.equals( view.getName())) {
				_views.remove(index);
				_viewsHashMap.remove(viewName);
				return index;
			}
			index++;
		}
		return -1;
	}
	
	@Override
    public String toString() {
		String s = "";
		s = "[ViewManager: ";
		for ( View view : _views) {
			s += view.toString();
		}
		return s;
	}
	
	/**
	 * Schreibt eine kleine Statistik der Ansichtsverwaltung auf den Standardausgabekanal.
	 */
	public void getStatistics() {
		String s = "";
		for ( View view : _views) {
			s += "[" + view.getName() + "]";
		}
		System.out.println("Anzahl Views: " + _views.size() + "  " + s);
		System.out.println("Anzahl unveränderbarer Views: " + _notChangables.size());
		if ( _views.size() != _viewsHashMap.size()) {
			System.out.println("Interne Strukturen kaputt!");
		}
	}
	
	/**
	 * Gibt eine Menge mit allen Namen aller in den Ansichten verwendeten Farben zurück.
	 * 
	 * @return eine Menge mit allen benutzten Farben
	 */
	public Set<String> getUsedColors() {
		Set<String> usedColors = new HashSet<String>();
		for ( View view : _views) {
			usedColors.addAll( view.getUsedColors());
		}
		return usedColors;
	}
	
	/*
	 * Gehört zur Implementation von LayerManagerChangeListener und tut nichts.
	 */
	public void layerAdded(Layer layer) {}
	
	/*
	 * Gehört zur Implementation von LayerManagerChangeListener.
	 */
	public void layerChanged(Layer layer) {
		for ( View view : _views) {
			view.layerChanged( layer);
		}
    }
	
	/*
	 * Gehört zur Implementation von LayerManagerChangeListener.
	 */
	public void layerRemoved(String layerName) {
		for ( View view : _views) { // Löschen ohne Veränderung des something-has-changed Status.
			final boolean changed = view.hasSomethingChanged();
			view.removeLayer( layerName);
			view.setSomethingChanged( changed);
		}
    }
	
	private static String[] columnNames = {"Name der Ansicht"};
	private List<View> _views = new ArrayList<View>();
	private Hashtable<String, View> _viewsHashMap = new Hashtable<String, View> ();
	private Set<String> _notChangables = new HashSet<String> ();
	
	private static final Debug _debug = Debug.getLogger();
}
