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

import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.utils.Interval;

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Ein Klasse zur Verwaltung der Darstellungstypen eines Layers.
 * <p>
 * Eine DOTCollection verkapselt die Darstellungstypen eines Layers. Jeder Darstellungstyp
 * eines Layers hat eine untere und obere Maßstabsgrenze, zwischen denen der Darstellungstyp
 * angewandt werden kann. Die kombinierten Informationen bestehend aus Darstellungstyp
 * und Maßstabsgrenzen werden im Hinblick auf schnellen Zugriff von der DOTCellection auf
 * zwei Arten verwaltet: als Liste und als Map.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8094 $
 *
 */
@SuppressWarnings("serial")
public class DOTCollection extends AbstractTableModel implements TableModel, Cloneable {
	
	/**
	 * Legt ein leeres Objekt an.
	 */
	public DOTCollection() {
		_dotList = new ArrayList<DOTCollectionItem>();
		_dotTreeMap = new TreeMap<Interval<Integer>, DisplayObjectType>();
	}
	/**
	 * Fügt den Darstellungstyp für die übergebenen Maßstabsgrenzen hinzu.
	 */
	public void addDisplayObjectType( DisplayObjectType type, int lowerScale, int upperScale) {
		if (type == null) {
			throw new IllegalArgumentException ("DOTCollection.addDisplayObjectType: type darf nicht null sein.");
		}
		if (lowerScale < upperScale) {
			throw new IllegalArgumentException ("DOTCollection.addDisplayObjectType: lowerScale darf nicht kleiner upperScale sein.");
		}
		DOTCollectionItem collectionItem = new DOTCollectionItem( type, lowerScale, upperScale);
		Interval<Integer> interval = new Interval<Integer> (upperScale, lowerScale);
		if ( _dotTreeMap.containsKey(interval)) {
			final DisplayObjectType dType = _dotTreeMap.get( interval);
			DOTCollectionItem dItem = new DOTCollectionItem( dType, lowerScale, upperScale);
			_dotList.remove( dItem);
		} 
		_dotList.add(collectionItem);
		_dotTreeMap.put(interval, type);
		fireTableDataChanged();
	}
	
	/**
	 * Entfernt den Darstellungstyp für die übergebenen Maßstabsgrenzen.
	 * 
	 * @param type der zu entfernende DisplayObjectType
	 * @param lowerScale die untere Intervallgrenze
	 * @param upperScale die obere Intervallgrenze
	 */
	public void removeDisplayObjectType( DisplayObjectType type, int lowerScale, int upperScale) {
		DOTCollectionItem collectionItem = new DOTCollectionItem( type, lowerScale, upperScale);
		if ( _dotList.contains( collectionItem)) {
			_dotList.remove( collectionItem);
			Interval<Integer> interval = new Interval<Integer> (upperScale, lowerScale);
			_dotTreeMap.remove( interval);
			fireTableDataChanged();
		} 
	}
	/**
	 * Leert die DOTCollection vollständig.
	 */
	public void clear( ) {
		_dotList.clear();
		_dotTreeMap.clear();
		fireTableDataChanged();
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn die DOTCollection leer ist, <code>false</code> sonst.
	 * 
	 * @return <code>true</code> genau dann, wenn die DOTCollection leer ist
	 */
	public boolean isEmpty() {
		return _dotList.isEmpty();
	}
	
	/**
	 * Erzeugt eine Kopie des aufrufenden Objekts
	 * 
	 * @return die Kopie 
	 */
	@Override
	public Object clone() {
		DOTCollection theClone = new DOTCollection();
		for ( DOTCollectionItem item : _dotList) {
			theClone.addDisplayObjectType(item.getDisplayObjectType(), 
					item.getLowerScale(), item.getUpperScale());
		}
		return theClone;
	}
	
	/**
	 * Gibt eine Kopie der DOTCollection zurück. 
	 * 
	 * @return eine Kopie
	 */
	public DOTCollection getCopy() {
		return (DOTCollection) clone();
	}
	
	/**
	 * Gibt einen Darstellungstypen für den mit scale angebenen Maßstabswert zurück, wenn 
	 * ein solcher existiert, sonst <code>null</code>.
	 * 
	 * @param scale ein Maßstabswert
	 * @return eine DisplayObjectType zum Maßstabswert oder <code>null</code>, wenn kein solcher existiert
	 */
	public DisplayObjectType getDisplayObjectType( int scale) {
		Interval<Integer> interval = new Interval<Integer>( scale, scale);
		final Entry<Interval<Integer>, DisplayObjectType> floorEntry = _dotTreeMap.floorEntry(interval);
		if (floorEntry == null || !interval.intersects(floorEntry.getKey())) {
			final Entry<Interval<Integer>, DisplayObjectType> ceilingEntry = _dotTreeMap.ceilingEntry(interval);
			if (ceilingEntry == null || !interval.intersects(ceilingEntry.getKey())) {
				return null;
			} else {
				return ceilingEntry.getValue();
			}
		} else {
			return floorEntry.getValue();
		}
	}
	
	/**
	 * Speichert die DOTCollection unter dem angebenen Knoten ab.
	 * 
	 * @param prefs der Knoten, unter dem gespeichert werden soll
	 */
	public void putPreferences( Preferences prefs) {
		int i = 0;
		for ( Interval<Integer> interval: _dotTreeMap.keySet()) {
			Preferences intervalPrefs = prefs.node( prefs.absolutePath() + "/interval" + i);
			intervalPrefs.putInt(LOWER_BOUND, interval.getLowerBound());
			intervalPrefs.putInt(UPPER_BOUND, interval.getUpperBound());
			intervalPrefs.put(DOT_NAME, _dotTreeMap.get(interval).getName());
			i++;
		}
	}
	
	/**
	 * Initialisiert die DOTCollection aus dem angebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Initialisierung beginnt
	 * @param dotManager die Darstellungstypenverwaltung
	 */
	public boolean initializeFromPreferences(Preferences prefs, DOTManager dotManager) {
		String[] intervals;
		try {
			intervals = prefs.childrenNames();
		}
		catch(BackingStoreException e) {
			_debug.error("Ein benutzer-definierter Layer kann nicht initialisiert werden, " + 
					"BackingStoreException: " + e.toString());
			return false;
		}
		for ( String interval: intervals) {
			if (!interval.startsWith("interval")) {	// im Moment ein Fehler, aber wer weiß was noch kommt
				continue;
			}
			Preferences intervalPrefs = prefs.node( prefs.absolutePath() + "/" + interval);
			Interval<Integer> iv = new Interval<Integer> (
					intervalPrefs.getInt(LOWER_BOUND, Integer.MIN_VALUE),
					intervalPrefs.getInt(UPPER_BOUND, Integer.MAX_VALUE));
			String dotName = intervalPrefs.get(DOT_NAME, "");
			final DisplayObjectType displayObjectType = dotManager.getDisplayObjectType(dotName);
			if ( displayObjectType != null) {
				_dotTreeMap.put(iv, displayObjectType);
			}
		}
		for ( Interval<Integer> interval : _dotTreeMap.keySet()) {
			DisplayObjectType displayObjectType = _dotTreeMap.get( interval);
			DOTCollectionItem collectionItem = new DOTCollectionItem(displayObjectType, 
					interval.getUpperBound(), interval.getLowerBound());
			_dotList.add( collectionItem);
		}
		return true;
	}
	
	@Override
	public String toString() {
		String s = new String();
		for ( Interval<Integer> interval : _dotTreeMap.keySet()) {
			s += "[" + interval.toString() + ", " + _dotTreeMap.get( interval).toString() + "] ";
		}
		return s;
	}
	
	/**
	 * Gibt eine Read-Only-Ansicht aller Darstellungstypen der DOTCollection zurück.
	 * 
	 * @return alle auftretenden DisplayObjectTypes
	 */
	public Collection<DisplayObjectType> values() {
		return Collections.unmodifiableCollection(_dotTreeMap.values());
	}
	
	/*
	 * Gehört zur Implementation von TableModel.
	 */
	public int getColumnCount() {
		return columnNames.length;
	}
	/*
	 * Gehört zur Implementation von TableModel.
	 */
	public int getRowCount() {
		return _dotTreeMap.size();
	}
	/*
	 * Gehört zur Implementation von TableModel.
	 */
	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}
	/*
	 * Gehört zur Implementation von TableModel.
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		if ( columnIndex == 0) {
			return _dotList.get( rowIndex).getDisplayObjectType().getName();
		} else if ( columnIndex == 1) {
			return _dotList.get( rowIndex).getLowerScale();
		} else if ( columnIndex == 2) {
			return _dotList.get( rowIndex).getUpperScale();
		}
		return null;
	}
	
	/**
	 * Ein DOTCollectionItem verkapselt die Information der DOTCollection bestehend aus einem
	 * Darstellungstypen und den Maßstabsgrenzen für die Listenverwaltung.
	 */
	private class DOTCollectionItem {
		private DisplayObjectType _displayObjectType;
		private int _lowerScale;
		private int _upperScale;
		
		/**
		 * Der Konstruktor.
		 * 
		 * @param displayObjectType ein DisplayObjectType
		 * @param lowerScale die untere Intervallgrenze
		 * @param upperScale die obere Intervallgrenze
		 */
		public DOTCollectionItem(DisplayObjectType displayObjectType, int lowerScale, int upperScale) {
			super();
			if ( displayObjectType == null) {
				throw new IllegalArgumentException("Ein DOTCollectionItem kann nicht ohne DisplayObjectType gebildet werden.");
			}
			_displayObjectType = displayObjectType;
			_lowerScale = lowerScale;
			_upperScale = upperScale;
		}
		/**
		 * Der Getter für den DisplayObjectType.
		 * 
		 * @return der DisplayObjectType
		 */
		public DisplayObjectType getDisplayObjectType() {
			return _displayObjectType;
		}
		/**
		 * Der Getter für die untere Intervallgrenze.
		 * 
		 * @return die untere Intervallgrenze
		 */
		public int getLowerScale() {
			return _lowerScale;
		}
		/**
		 * Der Getter für die obere Intervallgrenze.
		 * 
		 * @return die obere Intervallgrenze
		 */
		public int getUpperScale() {
			return _upperScale;
		}
		
		@Override
		public boolean equals(Object o) {
			if ( !(o instanceof DOTCollectionItem)) {
				return false;
			}
			DOTCollectionItem d = (DOTCollectionItem) o;
			if ( getDisplayObjectType().getName() != d.getDisplayObjectType().getName()) {
				return false;
			}
			if ( getLowerScale() != d.getLowerScale()) {
				return false;
			}
			return ( getUpperScale() == d.getUpperScale());
		}
		
		@Override
        public int hashCode() {
			return getDisplayObjectType().getName().hashCode() + getLowerScale() + getUpperScale();
		}
		
		@Override
		public String toString() {
			String s = "[DisplayObjectType: " + _displayObjectType.getName() + ", lower bound: " +
			             _lowerScale + ", upper bound: " + _upperScale + "]";
			return s;
		}
		
		public Set<String> getUsedColors() {
			return _displayObjectType.getUsedColors();
		}
		
	}
	
	/**
	 * Gibt eine Menge mit den Namen aller in den Darstellungstypen der DOTCollection verwendeten Farben zurück.
	 * 
	 * @return eine Menge mit den Namen aller in den Darstellungstypen der DOTCollection verwendeten Farben
	 */
	public Set<String> getUsedColors() {
		Set<String> usedColors = new HashSet<String>();
		for ( DOTCollectionItem dotItem : _dotList) {
			usedColors.addAll( dotItem.getUsedColors());
		}
		return usedColors;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn der Darstellungstyp mit dem übergebenen Namen in der DOTCollection
	 * auftritt.
	 * 
	 * @param displayObjectTypeName der Name eines DisplayObjectTypes
	 * @return <code>true</code> genau dann, wenn der Darstellungstyp in der DOTCollection auftritt
	 */
	public boolean displayObjectTypeIsUsed( final String displayObjectTypeName) {
		if ( displayObjectTypeName == null) {
			return false;
		}
		for ( DisplayObjectType displayObjectType : _dotTreeMap.values()) {
			if ( displayObjectTypeName.equals( displayObjectType.getName())) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Ausgabe auf dem Standardausgabekanal.
	 */
	@SuppressWarnings("unused")
	private void printAll() {
		System.out.println("Größe der Liste: " + _dotList.size());
		System.out.println("Größe der Map: " + _dotTreeMap.size());
		System.out.println("Listeneinträge");
		for ( DOTCollectionItem item : _dotList) {
			System.out.println( item.toString());
		}
		System.out.println("Mapeinträge");
		for ( Interval<Integer> interval: _dotTreeMap.keySet()) {
			System.out.println("Lower bound: " + interval.getLowerBound() + ", upper bound: " +
					interval.getUpperBound() + ", DisplayObjectType: " + _dotTreeMap.get(interval).getName());
		}
	}
	
	/*
	 * Interne Übrerprüfung mit Ausgabe der Ergebnisse auf dem Standardausgabekanal.
	 */
	@SuppressWarnings("unused")
	private void checkAll() {
		if ( _dotList.size() != _dotTreeMap.size() ) {
			System.out.println("Fehler: die Liste hat " + _dotList.size() + " Einträge, die Map " + _dotTreeMap.size());
		}
		for ( DOTCollectionItem item : _dotList) {
			Interval<Integer> interval = new Interval<Integer>( item.getUpperScale(), item.getLowerScale());
			if ( !_dotTreeMap.containsKey(interval)) {
				System.out.println("Fehler: die Liste hat einen Eintrag für das Interval [" + 
						interval.getLowerBound() + ", " + interval.getUpperBound() + "], die TreeMap aber nicht!");
			} else {
				if ( item.getDisplayObjectType().getName() != _dotTreeMap.get( interval).getName()) {
					System.out.println("Fehler: zum Interval [" + 
							interval.getLowerBound() + ", " + interval.getUpperBound() + 
							"] sind die DisplayObjectTypes verschieden. In der Liste: " +
							item.getDisplayObjectType().getName() + ", in der TreeMap: " +
							_dotTreeMap.get( interval).getName());
				}
			}
		}
		for ( Interval<Integer> interval : _dotTreeMap.keySet()) {
			DOTCollectionItem item = new DOTCollectionItem(_dotTreeMap.get(interval), 
					interval.getUpperBound(), interval.getLowerBound());
			if ( !_dotList.contains( item)) {
				System.out.println("Fehler: die TreeMap enthält einen Eintrag zu " + item.toString() +
				", der in der Liste nicht auftritt!");
			}
		}
	}
	
	private List<DOTCollectionItem> _dotList;
	private TreeMap<Interval<Integer>, DisplayObjectType> _dotTreeMap;
	
	private static String[] columnNames = {"Darstellungstyp", "Von 1:", "Bis 1:"};
	
	private static final String LOWER_BOUND = "LOWER_BOUND";
	private static final String UPPER_BOUND = "UPPER_BOUND";
	private static final String DOT_NAME = "DOT_NAME";
	
	private final static Debug _debug = Debug.getLogger();
}
