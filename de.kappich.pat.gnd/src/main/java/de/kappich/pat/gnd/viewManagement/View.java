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

import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.layerManagement.LayerManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Die Klasse für die Ansichten der Generischen Netzdarstellung.
 * <p>
 * Eine Ansicht hat einen eindeutigen Namen unter dem sie von der {@link ViewManager Ansichtsverwaltung}
 * geführt wird. Sie besteht aus einer Liste von {@link ViewEntry ViewEntries}, also Layern mit
 * ansichts-spezifischen Einstellungen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9145 $
 */
public class View {


	/* Eine kurze Beschreibung, wie das Update eines Views funktioniert. Klassen, die an
		 * den Änderungen interesiert sind, implementieren das Interface View.ChangeListener.
		 * Dies sind z.B. das MapPane, das LegendPane, aber auch der ViewTableModelAdapter
		 * in ViewDialog. Dieser ist gleichzeitig aber auch TableModel und ändert seinerseits
		 * den View in seiner setValueAt-Methode.
		 */

	/**
	 * Ein Getter für den Namen.
	 *
	 * @return den Namen
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Ein Setter für den Namen.
	 *
	 * @param der neue Name
     */
    public void setName(String name) {
    	_name = name;
    }

	/**
	 * Gibt <code>true</code> zurück, wenn seit dem Konstruieren der Ansicht oder seit dem letzten Aufruf
	 * von setSomethingChanged() mit dem Wert false, eine Veränderung an der Ansicht vorgenommen wurde
	 * (ob durch eine verändernde Methode oder durch setSomethingChanged() mit dem Wert <code>true</code>).
	 *
	 * @return hat sich etwas geändert?
     */
    public boolean hasSomethingChanged() {
    	return _somethingChanged;
    }

    /**
     * Setzt den Wert der internen Variablen, die zum Verwalten von Änderungen seit der letzten Speicherung
     * dient.
     *
     * @param b setzt den Wert des Änderungsstatus
     */
    public void setSomethingChanged( final boolean b) {
    	_somethingChanged = b;
    }

	/**
	 * Ein Interface für Listener, die über Änderungen der Ansicht informiert werden wollen.
	 *
	 * @author Kappich Systemberatung
	 * @version $Revision: 9145 $
	 */
	public interface ViewChangeListener {
		/**
		 * Der Ansicht wurde ein Layer am Ende angehängt.
		 *
		 * @param view die Ansicht
		 * @param newIndex
		 */
		void viewEntryInserted(View view, final int newIndex);
		/**
		 * Der Layer an der i-ten Stelle der Ansicht wurde geändert.
		 *
		 * @param view die Ansicht
		 * @param i ein Index
		 */
		void viewEntryChanged(View view, int i);
		/**
		 * Der Layer an der i-ten Stelle der Ansicht wurde gelöscht.
		 *
		 *  @param view die Ansicht
		 * @param i ein Index
		 */
		void viewEntryRemoved(View view, int i);
		/**
		 * Die Layer an der i-ten und j-ten Stelle der Ansicht wurden miteinander vertauscht.
		 *
		 * @param view die Ansicht
		 * @param i ein Index
		 * @param j ein Index
		 */
		void viewEntriesSwitched(View view, int i, int j);
	}
	/**
	 * Konstruiert eine leere Ansicht.
	 */
	public View() {}

	/**
	 * Konstruiert eine Ansicht mit Namen und ViewEntries.
	 *
	 * @param name der Name der Ansicht
	 * @param viewEntries die ViewEntries
	 */
	public View(String name, List<ViewEntry> viewEntries) {
		_name = name;
		for(ViewEntry viewEntry : viewEntries) {
			ViewEntry newViewEntry = new ViewEntry( viewEntry.getLayer(),
					viewEntry.getZoomIn(), viewEntry.getZoomOut(), viewEntry.isSelectable(), viewEntry.isVisible());
			newViewEntry.setView(this);
			_viewEntries.add(newViewEntry);
		}
	}

	/**
	 * Gibt die Menge der ViewEntries inklusive Notiz-Layern zurück.
	 *
	 * @return die Menge der ViewEntries
	 */
	public List<ViewEntry> getViewEntries() {
		return getViewEntries(true);
	}

	/**
	 * Gibt die Menge der ViewEntries zurück.
	 *
	 * @param withNoticeLayers Ob auch Notiz-Layer berücksichtigt werden sollen
	 * @return die Menge der ViewEntries
	 */
	public List<ViewEntry> getViewEntries(final boolean withNoticeLayers) {
		if(withNoticeLayers) {
			final List<ViewEntry> viewEntries = new ArrayList<ViewEntry>(_viewEntries.size() * 2);
			int viewEntriesSize = _viewEntries.size();
			for(int i = viewEntriesSize-1; i >= 0; i--) {
				final ViewEntry viewEntry = _viewEntries.get(i);
				final ViewEntry noticeViewEntry = NoticeViewEntry.create(viewEntry);
				noticeViewEntry.setView(this);
				viewEntries.add(0, noticeViewEntry);
			}
			viewEntries.addAll(_viewEntries);
			return viewEntries;
		}
		else {
			return Collections.unmodifiableList(_viewEntries);
		}
	}

	/**
	 * Gibt den Index des ViewEntries innerhalb der Liste der ViewEntries zurück. Gehört
	 * der ViewEntry gemäß Object.equals() nicht zu dieser Ansicht, so ist der Rückgabewert -1.
	 *
	 * @param viewEntry
	 * @return der Index des Entrys
	 */
	public int getIndex(ViewEntry viewEntry) {
		int i = 0;
		for (ViewEntry entry : _viewEntries) {
			if (entry.equals(viewEntry)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	/**
	 * Fügt einen ViewEntry mit dem übergebenen Layer und Default-Einstellungen am Ende
	 * der Liste der ViewEntries hinzu.
	 *
	 * @param layer ein Layer
	 */
	public void addLayer(final Layer layer) {
		for ( ViewEntry viewEntry : _viewEntries) {
			if (viewEntry.getLayer().getName().equals(layer.getName())) {
				return;
			}
		}
		ViewEntry viewEntry = new ViewEntry (layer, Integer.MAX_VALUE, 1, true, true);
		viewEntry.setView(this);
		_viewEntries.add( viewEntry);
		try {
			notifyChangeListenersViewEntryAppended();
		} catch ( RuntimeException e) {
			_debug.error(  "Fehler in View.addLayer(): " + e.toString());
		}
		_somethingChanged = true;
	}

	/**
	 * Informiert die {@link View.ChangeListener View.ChangeListeners} über einen geänderte Layer.
	 * Kann von außen z.B. von der {@link ViewManager Ansichtsverwaltung} aufgerufen werden.
	 *
	 * @param layer ein Layer
	 */
	public void layerChanged( final Layer layer) {
		final String layerName = layer.getName();
		for ( int i = 0; i < _viewEntries.size(); i++) {
			final ViewEntry entry = _viewEntries.get( i);
			if ( entry.getLayer().getName().equals( layerName)) {
				notifyChangeListenersViewEntryChanged(i);
			}
		}
	}

	/**
	 * Entfernt alle ViewEntries aus der Ansicht, die den Layer mit dem übergebenen Namen verwenden.
	 *
	 * @param layerName ein Layername
	 */
	public void removeLayer( final String layerName) {
		final int size = _viewEntries.size();
		for ( int i = size-1; i >= 0; i--) {
			if ( _viewEntries.get( i).getLayer().getName().equals( layerName)) {
				remove( i);
			}
		}
	}

	/**
	 * Fügt den Listener den auf Änderungen angemeldeten Listenern hinzu.
	 *
	 * @param listener ein Listener
	 */
	public void addChangeListener(ViewChangeListener listener) {
		_listeners.add(listener);
	}

	/**
	 * Entfernt den Listener aus der Menge der auf Änderungen angemeldeten Listenern.
	 *
	 *  @param listener ein Listener
	 */
	public void removeChangeListener(ViewChangeListener listener) {
		_listeners.remove(listener);
	}

	/**
	 * Entfernt den ViewEntry, der an der Stelle <code>index</code> in der Liste der ViewEntries steht.
	 *
	 * @param index ein Index
	 */
	public void remove(int index) {
		if ((index >= 0) && (index < _viewEntries.size())) {
			_viewEntries.get(index).setView(null);
			_viewEntries.remove(index);
			notifyChangeListenersViewEntryRemoved(index);
			_somethingChanged = true;
		}
	}

	/**
	 * Vertauscht die durch die Indexe i und j angegebenen ViewEntries in der Liste aller ViewEntries.
	 *
	 * @param i ein Index
	 * @param j ein Index
	 */
	public void switchTableLines(int i, int j) {
		if ((i >= 0) && (i < _viewEntries.size()) && (j>=0) && (j < _viewEntries.size())) {
			final ViewEntry iRow = _viewEntries.set(i, _viewEntries.get(j));
			_viewEntries.set(j, iRow);
			notifyChangeListenersViewEntriesSwitched( i, j);
			_somethingChanged = true;
		}
	}

	/**
	 * Informiert die ChangeListener darüber, dass ein neuer ViewEntry angehängt wurde.
	 */
	public void notifyChangeListenersViewEntryAppended() {
		for ( ViewChangeListener changeListener : _listeners) {
			changeListener.viewEntryInserted(this, _viewEntries.size() - 1);
			changeListener.viewEntryInserted(this, _viewEntries.size() * 2 - 1);
		}
	}

	/**
	 * Informiert die ChangeListener darüber, dass ViewEntry i geändert wurde.
	 *
	 * @param i ein Index
	 */
	public void notifyChangeListenersViewEntryChanged(int i) {
		_somethingChanged = true;
		for ( ViewChangeListener changeListener : _listeners) {
			changeListener.viewEntryChanged(this, i);
			changeListener.viewEntryChanged(this, i + _viewEntries.size());
		}
	}

	/**
	 * Informiert die ChangeListener darüber, dass ViewEntry i entfrnt wurde.
	 *
	 * @param i ein Index
	 */
	public void notifyChangeListenersViewEntryRemoved(int i) {
		for ( ViewChangeListener changeListener : _listeners) {
			changeListener.viewEntryRemoved(this, i);
			changeListener.viewEntryRemoved(this, i + _viewEntries.size());
		}
	}

	/**
	 * Informiert die ChangeListener darüber, dass ViewEntries i und j vertauscht wurden.
	 *
	 * @param i ein Index
	 * @param j ein Index
	 */
	public void notifyChangeListenersViewEntriesSwitched( int i, int j) {
		for ( ViewChangeListener changeListener : _listeners) {
			changeListener.viewEntriesSwitched(this, i, j);
			changeListener.viewEntriesSwitched(this, i + _viewEntries.size(), j + _viewEntries.size());
		}
	}

	/**
	 * Speichert die Ansicht in den übergebenen Knoten.
	 *
	 * @param prefs der Knoten, unter dem die Speicherung beginnt
	 */
	public void putPreferences ( Preferences prefs) {
		deletePreferences(prefs);
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
		int i = 0;
		for ( ViewEntry viewEntry : _viewEntries) {
			Preferences entryPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/entry" + i);
			viewEntry.putPreferences( entryPrefs);
			i++;
		}
		_somethingChanged = false;
	}

	/**
	 * Initialisiert die Ansicht aus dem übergebenen Knoten.
	 *
	 * @param prefs der Knoten, unter dem die Initialisierung beginnt
	 */
	public void initializeFromPreferences( Preferences prefs, LayerManager layerManager) {
		_name = prefs.name();
		String[] entries;
		try {
	        entries = prefs.childrenNames();
        }
        catch(BackingStoreException e) {
	        
	        throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
        }
        final Map<Integer, ViewEntry> orderedViewEntries = new TreeMap<Integer, ViewEntry>();
		for ( String entryName : entries) {
			if ( !entryName.startsWith("entry")) {	// jetzt ein Fehler, aber später vielleicht okay
				continue;
			}
			Preferences entryPrefs = prefs.node(prefs.absolutePath() + "/" + entryName);
			ViewEntry entry = new ViewEntry();
			if ( entry.initializeFromPreferences(entryPrefs, layerManager)) {
				final Integer index = Integer.valueOf( entryName.replace("entry", "")).intValue();
				orderedViewEntries.put( index, entry);
				entry.setView( this);
			}
		}
		for ( ViewEntry entry : orderedViewEntries.values()) {
			_viewEntries.add( entry);
		}
	}

	/**
	 * Entfernt die Ansicht unterhalb des übergebenen Knotens.
	 *
	 * @param prefs der Knoten, unter dem gelöscht wird
	 */
	public void deletePreferences( Preferences prefs) {
		Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
		try {
			objectPrefs.removeNode();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
	}

	/**
	 * Gibt die Menge aller Farben, die von allen ViewEntries der Ansicht benutzt werden, zurück.
	 *
	 * @return die Menge aller benutzten Farben
	 */
	public Set<String> getUsedColors() {
		Set<String> usedColors = new HashSet<String>();
		for ( ViewEntry entry : _viewEntries) {
			usedColors.addAll( entry.getUsedColors());
		}
		return usedColors;
	}

	@Override
	public String toString() {
		String s = new String();
		s = "[View: " + _name;
		for (ViewEntry viewEntry : _viewEntries) {
			s += viewEntry.toString();
		}
		s += "]";
		return s;
	}

	/**
	 * Erzeugt eine tiefe Kopiedes aufrufenden Objekts.
	 *
	 * @return die Kopie
	 */
	public View getCopy( final String name) {
		View copy = new View();
		if ( name != null) {
			copy._name = name;
		} else {
			copy._name = _name;
		}
		for ( ViewEntry viewEntry : _viewEntries) {
			final ViewEntry viewEntryCopy = viewEntry.getCopy();
			viewEntryCopy.setView( copy);
			copy._viewEntries.add( viewEntryCopy);
		}
		return copy;
	}


	final private List<ViewEntry> _viewEntries = new ArrayList<ViewEntry>();

	final private List<ViewChangeListener> _listeners = new CopyOnWriteArrayList<ViewChangeListener>();

	private String _name = null;

	private boolean _somethingChanged = false;	// true, wenn seit dem letzten Speichern etwas geändert wurde

	private static final Debug _debug = Debug.getLogger();

}
