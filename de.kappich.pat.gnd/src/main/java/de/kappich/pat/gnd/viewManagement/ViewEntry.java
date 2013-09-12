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

import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.layerManagement.LayerManager;

import java.awt.Component;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JComponent;

/**
 * Eine Klasse für einen Layer einer Ansicht mit dessen ansicht-spezifischen Eigenschaften.
 * <p>
 * Ein ViewEntry ist ein Objekt, das einen {@link Layer} und seine Einstellungen innerhalb 
 * einer {@link View Ansicht} beschreibt. Dementsprechend ergeben sich seine Bestandteile 
 * (der Layer, zwei Maßstabsgrenzen zum Ein- und Ausblenden, und zwei Flags, die anzeigen,
 * ob die Objekte des Layers sichtbar sind und ob der Layer aktuell eingeblendet ist) und
 * Methoden.
 * <p>
 * Zwei Implementationsdeteils:
 * Obwohl ein ViewEntry ein Bestandteil seiner Ansicht ist, kennt er diese auch, um deren 
 * ChangeListener mit Hilfe der notify-Methoden über Änderungen zu benachrichtigen. Außerdem 
 * wird dem ViewEntry die Swing-Komponente, in der er visualisiert wird, mit einem Setter 
 * bekannt gemacht, wenn er visiualisiert wird.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 */
public class ViewEntry {

	/**
	 * Konstruiert ein leeres Objekt.
	 */
	public ViewEntry() {}

	/**
	 * Konstruiert einen ViewEntry aus den übergebenen Informationen
	 * 
	 * @param layer Der Layer des ViewEntry's
	 * @param zoomIn Der Maßstab, ab dem der Layer des ViewEntry's eingeblendet wird
	 * @param zoomOut Der Maßstab, ab dem der Layer des ViewEntry's ausgeblendet wird
	 * @param selectable Sind die Objekte des Layers selektierbar? 
	 * @param visible Wird der Layer aktuell angezeigt?
	 */
	public ViewEntry(Layer layer, int zoomIn, int zoomOut, boolean selectable, boolean visible) {
		_layer = layer;
		_zoomIn = zoomIn;
		_zoomOut = zoomOut;
		_selectable = selectable;
		_visible = visible;
		if(_zoomIn < _zoomOut) {
			throw new IllegalArgumentException("zoomIn muss größer zoomOut sein.");
		}
	}

	
	/**
	 * Setzt die Ansicht, zu der der ViewEntry gehört.
	 * 
	 * @param view die Ansicht
	 */
	public void setView(final View view) {
		_view = view;
	}

	/**
	 * Gibt den Layer zurück.
	 * 
	 * @return den Layer
	 */
	public Layer getLayer() {
		return _layer;
	}

	/**
	 * Setzt den Layer und informiert die ChangeListener.
	 * 
	 * @param der neue Layer
	 */
	public void setLayer(Layer layer) {
		_layer = layer;
		if (_view != null) {
			_view.notifyChangeListenersViewEntryChanged(_view.getIndex(this));
		}
	}

	/**
	 * Gibt den Wert des Maßstabs zurück, ab dem der Layer eingeblendet wird.
	 * 
	 * @return den Zoom-In-Wert
	 */
	public int getZoomIn() {
		return _zoomIn;
	}

	/**
	 * Gibt den Wert des Maßstabs zurück, ab dem der Layer ausgeblendet wird.
	 * 
	 * @return den Zoom-Out-wert
	 */
	public int getZoomOut() {
		return _zoomOut;
	}

	/**
	 * Setzt den Wert des Maßstabs, ab dem der Layer eingeblendet wird.
	 * 
	 * @param zoomIn der neue Zoom-In-Wert 
	 */
    public void setZoomIn(int zoomIn) {
    	_zoomIn = zoomIn;
    	if(_view != null) {
    		_view.notifyChangeListenersViewEntryChanged(_view.getIndex(this));
    	}
    }

    /**
	 * Setzt den Wert des Maßstabs, ab dem der Layer eingeblendet wird.
	 * 
	 * @param zoomIn der neue Zoom-Out-Wert
	 */
    public void setZoomOut(int zoomOut) {
    	_zoomOut = zoomOut;
    	if (_view != null) {
    		_view.notifyChangeListenersViewEntryChanged(_view.getIndex(this));
    	}
    }

	/**
	 * Gibt <code>true</code> zurück, wenn die Objekte des Layers selektierbar sind.
	 * 
	 * @return sind die Objekte selektierbar?
	 */
	public boolean isSelectable() {
		return _selectable;
	}

	/**
	 * Macht die Objekte des Layers selektierbar oder nicht. Nur Objekte selektierbarer
	 * Layer erscheinen auch im Kartentooltipp.
	 * 
	 * @param selectable der neue Wert der Selektierbarkeits-Eigenschaft
	 */
	public void setSelectable(boolean selectable) {
		_selectable = selectable;
		if (_view != null) {
			_view.notifyChangeListenersViewEntryChanged(_view.getIndex(this));
		}
	}

	/**
	 * Gibt <code>true</code> zurück, wenn der Layer aktuell sichtbar ist. Diese Methode 
	 * überprüft nur das Sichtbarkeitsflag, nicht aber die Zoomstufe (die es
	 * möglicherweise nicht gibt, wenn nämlich die Ansicht nicht angezeigt wird).
	 * 
	 * @return ist der Layer sichtbar?
	 */
	public boolean isVisible() {
		return _visible;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn der Layer sichtbar ist und der übergebene Wert zwischen
	 * den Werten von getZoomOut und getZoomIn liegt, d.h. wenn der Layer in einer  Karte mit dem 
	 * entsprechenden Maßstab angezeigt werden soll.
	 * 
	 * @param scale ein Maßstab
	 * @return ist der Layer für den übergebenen Maßstab sichtbar?
	 */
	public boolean isVisible( int scale) {
		if ((getZoomIn() >= scale) && (scale >= getZoomOut())) {
			return _visible;
		} else {
			return false;
		}
	}

	/**
	 * Setzt den Wert, der entscheidet, ob der Layer aktuell angezeigt werden soll.
	 * 
	 * @param visible der neue Sichtbarkeitswert
	 */
	public void setVisible(boolean visible) {
		_visible = visible;
		if (_view != null) {
			_view.notifyChangeListenersViewEntryChanged(_view.getIndex(this));
		}
	}
	
	/**
	 * Speichert den ViewEntry unter dem angebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Speicherung beginnt
	 */
	public void putPreferences( Preferences prefs) {
		prefs.put(LAYER, _layer.getName());
		prefs.putInt(ZOOM_IN, _zoomIn);
		prefs.putInt(ZOOM_OUT, _zoomOut);
		prefs.putBoolean(SELECTABLE, _selectable);
		prefs.putBoolean(VISIBLE, _visible);
	}
	
	/**
	 * Initialisiert einen ViewEntry aus dem angebenen Knoten.
	 * 
	 * @param der Knoten, unter dem die Initialisierung beginnt
	 */
	public boolean initializeFromPreferences(Preferences prefs, LayerManager layerManager) {
		_layer = layerManager.getLayer( prefs.get(LAYER, ""));
		if ( _layer == null) {
			return false;
		}
		_zoomIn = prefs.getInt(ZOOM_IN, Integer.MAX_VALUE);
		_zoomOut = prefs.getInt(ZOOM_OUT, 0);
		_selectable = prefs.getBoolean(SELECTABLE, true);
		_visible = prefs.getBoolean(VISIBLE, true);
		return true;
	}
		
	@Override
    public String toString() {
		String s = new String();
		s = "[ViewEntry:" + _layer.toString() + "[ZoomIn: " + _zoomIn + "][ZoomOut: " + _zoomOut + "]" +
			"[Selektierbar: " + _selectable + "][Visualisierbar: " + _visible + "]]";
		return s;
	}
	
	/**
	 * Gibt die Swing-Komponente, die den ViewEntry aktuell visualisiert, zurück.
	 * 
	 * @return eine Swing-Komponente oder <code>null</code>
     */
    public JComponent getComponent() {
    	return _component;
    }

	/**
	 * Setzt die Swing-Komponente, die den ViewEntry aktuell visualisiert.
	 * 
	 * @param die neue Swing-Komponente
     */
    public void setComponent(JComponent component) {
    	_component = component;
    }
    
    /**
     * Gibt einen neuen ViewEntry zurück, der dem aufrufenden Objekt gleicht, abgesehen
     * davon, dass dieser noch nicht zu einer Ansicht gehört und keine Swing-Komponente,
     * die ihn visualisiert, kennt.
     * 
     * @return die Kopie
     */
    public ViewEntry getCopy() {
    	ViewEntry copy = new ViewEntry( _layer, _zoomIn, _zoomOut, _selectable, _visible);
    	return copy;
    }
    
    /**
     * Gibt die Menge aller von zugehörigen Layer verwendeten Farben zurück.
     * 
     * @return die Menge der benutzten Farben
     */
    public Set<String> getUsedColors() {
		return _layer.getUsedColors();
	}
    

	private View _view;
	private Layer _layer;
	/** Für den Maßstab m zwischen 1:_zoomIn und 1:_zoomOut wird der layer angezeigt */
	private int _zoomIn;
	private int _zoomOut;
	private boolean _selectable;
	private boolean _visible;
	private JComponent _component = null;
	
	private static final String LAYER = "LAYER";
	private static final String ZOOM_IN = "ZOOM_IN";
	private static final String ZOOM_OUT = "ZOOM_OUT";
	private static final String SELECTABLE = "SELECTABLE";
	private static final String VISIBLE = "VISIBLE";
	
}
