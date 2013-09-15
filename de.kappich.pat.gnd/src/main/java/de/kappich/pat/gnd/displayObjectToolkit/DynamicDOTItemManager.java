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

import de.kappich.pat.gnd.utils.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Ein DynamicDOTItemManager ist ein DOTItemManager<DynamicDOTItem>, der das Interface
 * TableModel implementiert. Er wird in allen internen Implementationen von DisplayObjectType
 * als Verwalter der dynamischen Informationen zu einer Eigenschaft oder zu einem
 * Paar bestehend aus einer Grundfigur und einer Eigenschaft, eingesetzt.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
public class DynamicDOTItemManager extends DOTItemManager<DynamicDOTItem> implements TableModel {
	
	/**
	 * Konstruiert ein leeres Objekt.
	 */
	public DynamicDOTItemManager () {
		super();
	}
	
	protected List<TableModelListener> _listenerList = new ArrayList<TableModelListener>();
	
	/**
	 * Fügt den Listener hinzu.
	 * 
	 * @param l der Listener
	 */
	public void addTableModelListener(TableModelListener l) {
		_listenerList.add( l);
	}
	
	/**
	 * Fügt das Item für das Intervall im Sinne von {@link DOTItemManager.#put} hinzu und informiert alle
	 * TableModelListener über die Änderung.
	 * 
	 * @param interval das Intervall
	 * @param item das Item
	 */
	@Override
	public void put ( Interval<Double> interval, DynamicDOTItem item) {
		super.put(interval, item);
		for ( TableModelListener tableModelListener : _listenerList) {
			tableModelListener.tableChanged( new TableModelEvent( this));
		}
	}
	
	/**
	 * Fügt das Item für das Intervall im Sinne von {@link DOTItemManager.#insert} hinzu und informiert alle
	 * TableModelListener über die Änderung.
	 *
	 * @param interval das Intervall
	 * @param item das Item
	 */
	@Override
	public void insert ( Interval<Double> interval, DynamicDOTItem item) {
		super.insert(interval, item);
		for ( TableModelListener tableModelListener : _listenerList) {
			tableModelListener.tableChanged( new TableModelEvent( this));
		}
	}
	
	/**
	 * Entfernt das durch den Index angegebene Item und Intervall aus der Verwaltung und informiert alle
	 * TableModelListener über die Änderung.
	 * 
	 * @param index der Index
	 */
	@Override
	public void remove( int index) {
		super.remove(index);
		for ( TableModelListener tableModelListener : _listenerList) {
			tableModelListener.tableChanged( new TableModelEvent( this));
		}
	}
	
	/**
	 * Gibt die Klasse der Spaltenobjekte an. Wenn man für die die Spalten vom Typ 'Number' nicht
	 * diese Klasse zurückgibt, wird der falsche Renderer benutzt.
	 */
	public Class<?> getColumnClass(int columnIndex) {
		if ( columnIndex == 4 || columnIndex == 5) {
			return Number.class;
		}
		return Object.class;
	}
	
	/**
	 * Gibt die Anzahl der Spalten zurück.
	 * 
	 * @return die Anzahl der Spalten
	 */
	public int getColumnCount() {
		return _columnNames.length;
	}
	
	/**
	 * Gibt den Spaltennamen zurück.
	 * 
	 * @return den Spaltennamen
	 */
	public String getColumnName(int columnIndex) {
		return _columnNames[columnIndex];
	}
	
	/**
	 * Gibt die Zeilenzahl zurück.
	 * 
	 * @return die Zeilenzahl
	 */
	public int getRowCount() {
		return size();
	}
	
	/**
	 * Gibt den Wert der durch die Indexe angebenen Zelle zurück.
	 * 
	 * @param rowIndex der Zeileindex
	 * @param columnIndex der Spaltenindex
	 * @return der Wert der Zelle
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		DisplayObjectTypeItemWithInterval itemWithInterval = get( rowIndex);
		if ( itemWithInterval != null) {
			if ( columnIndex == 0) {
				return itemWithInterval.getItem().getPropertyValue();
			} else if ( columnIndex == 1) {
				return itemWithInterval.getItem().getAttributeGroup();
			} else if ( columnIndex == 2) {
				return itemWithInterval.getItem().getAspect();
			} else if ( columnIndex == 3) {
				return itemWithInterval.getItem().getAttributeName();
			} else if ( columnIndex == 4) {	// Vermeidung der Exponentialschreibweise bei Integern
				final Double lowerBound = itemWithInterval.getInterval().getLowerBound();
				if ( lowerBound.equals( Double.NEGATIVE_INFINITY)) {
					return null;
				}
				final Integer intValue = lowerBound.intValue();
				if ( intValue.doubleValue() == lowerBound) {
					return intValue;
				}
				return lowerBound;
			} else if ( columnIndex == 5) { // Vermeidung der Exponentialschreibweise bei Integern
				final Double upperBound = itemWithInterval.getInterval().getUpperBound();
				if ( upperBound.equals( Double.NEGATIVE_INFINITY)) {
					return null;
				}
				final Integer intValue = upperBound.intValue();
				if ( intValue.doubleValue() == upperBound) {
					return intValue;
				}
				return upperBound;
			} else if ( columnIndex == 6) {
				return itemWithInterval.getItem().getDescription();
			}
		}
		return null;
	}
	
	/**
	 * Gibt <code>false</code> zurück, da die Zellen nicht editierbar sein sollen.
	 * 
	 * @return <code>false</code>
	 */
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	/**
	 * Entfernt den übergebenen TableModelListener aus der Menge aller Listener.
	 * 
	 * @param l der zu entfernende Listener
	 */
	public void removeTableModelListener(TableModelListener l) {
		_listenerList.remove( l);
	}
	
	/**
	 * Nicht implementiert; wirft bei Aufruf deshalb eine UnsupportedOperationException.
	 * 
	 * @param aValue ein Wert
	 * @param rowIndex der Zeileindex
	 * @param columnIndex der Spaltenindex
	 */
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("Methode setValueAt nicht implementiert!");
	}
	
	/**
	 * Die Methode vergleicht die einzelnen TreeMaps im Detail und gibt nur bei exakter Gleichheit
	 * auch <code>true</code> zurück.
	 * 
	 * @return <code>true</code> genau dann, wenn die Manager gleich sind
	 */
	@Override
	public boolean equals(Object o) {
		if ( !(o instanceof DynamicDOTItemManager)) {
			return false;
		}
		DynamicDOTItemManager d = (DynamicDOTItemManager) o;
		if ( _displayObjectTypesItemMap.size() != d._displayObjectTypesItemMap.size()) {
			return false;
		}
		for ( String key : _displayObjectTypesItemMap.keySet()) {
			final TreeMap<Interval<Double>, DynamicDOTItem> dTreeMap = d._displayObjectTypesItemMap.get(key);
			if ( dTreeMap == null) {
				return false;
			}
			final TreeMap<Interval<Double>, DynamicDOTItem> myTreeMap = _displayObjectTypesItemMap.get(key);
			if ( myTreeMap.size() != dTreeMap.size()) {
				return false;
			}
			for ( Interval<Double> interval : myTreeMap.keySet()) {
				final DynamicDOTItem dDynamicDOTItem = dTreeMap.get( interval);
				final DynamicDOTItem myDynamicDOTItem = myTreeMap.get( interval);
				if ( !myDynamicDOTItem.equals( dDynamicDOTItem)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}
	
	/**
	 * Überschrieben, weil <code>equals</code> überschrieben wurde.
	 * 
	 * @return ein trivialer Hashcode
	 */
	@Override
    public int hashCode() {
		return _displayObjectTypesItemMap.size();
	}
	
	/**
	 * Gibt eine Kopie des Objekts zurück.
	 * 
	 * @return die Kopie
	 */
	public DynamicDOTItemManager getCopy() {
		DynamicDOTItemManager copy = new DynamicDOTItemManager();
		for ( int index = 0; index < size(); index++) {
			final DisplayObjectTypeItemWithInterval dotItemWithInterval = get( index);
			copy.insert(dotItemWithInterval.getInterval(), dotItemWithInterval.getItem());
		}
		return copy;
	}
	
	private String[] _columnNames = {"Wert", "Attributgruppe", "Aspekt", "Attributname", "Von", "Bis", "Info"};
	
}
