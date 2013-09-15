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

import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.needlePlugin.DOTNeedlePainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;

import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein DisplayObject ist ein georeferenziertes SystemObject mit allen Informationen zu seiner Darstellung.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 *
 */
public class DisplayObject implements ClientReceiverInterface, MapPane.MapScaleListener {
	
	/**
	 * Gibt das zugrundeliegende {@link SystemObject} zurück.
	 * 
	 * @return das Systemobjekt
	 */
    public SystemObject getSystemObject() {
    	return _systemObject;
    }
    
    /**
     * Gibt das aktuell gültige {@link DisplayObjectTypeItem} für das {@link PrimitiveFormPropertyPair pair} zurück.
     * 
     * @param pair das Paar
     * @return das aktuell gültige DisplayObjectTypeItem oder <code>null</code>, wenn kein solches existiert
     */
    public DisplayObjectTypeItem getDisplayObjectTypeItem( PrimitiveFormPropertyPair pair) {
    	if ( _displayObjectTypeItems != null) {
    		return _displayObjectTypeItems.get( pair);
    	}
    	return null;
    }
    /**
     * Gibt den aktuell gültigen Wert für das {@link PrimitiveFormPropertyPair pair} zurück.
     * 
     * @param pair das Paar
     * @return der aktuell gültige Wert oder <code>null</code>, wenn kein solcher existiert
     */
    public Data getValue( PrimitiveFormPropertyPair pair) {
    	if ( _displayObjectTypeItems != null) {
    		return _values.get( pair);
    	}
    	return null;
    }

    /**
     * Gibt die Koordinaten zu dem dem übergebenen (Koordinaten-)Typ zurück. Bei Linien ist der 
     * Typ gleich dem Verschiebungswert.
     * 
     * @param type der Koordinatentyp 
     * @return die Koordinaten
     */
    public List<Object> getCoordinates( int type) {
    	if ( !_coordinates.containsKey( type)) {
			_coordinates.put( type, _painter.getCoordinates( _coordinates.get( 0), type));
    	}
    	return _coordinates.get( type);
    }
	
    /**
	 * Gibt die Koordinaten zum Default(-Koordinaten)-Typ zurück.
	 * 
	 * @return die Default-Koordinaten
	 */
    public List<Object> getCoordinates() {
    	return getCoordinates( _defaultType);
    }

	/**
	 * Gibt das Painter-Objekt {@link DisplayObjectPainter} zu dieses DisplayObject zurück.
     * 
     * @return den Painter
     */
    public DisplayObjectPainter getPainter() {
    	return _painter;
    }

	/**
	 * Gibt die {@link DOTCollection} zu diesem DisplayObject zurück.
	 * 
     * @return die DOTCollection
     */
    public DOTCollection getDOTCollection() {
    	return _dotCollection;
    }

	/**
	 * Gibt die umgebende Rechteck zu diesem DisplayObject für den angebenen (Koordinaten-)Typ zurück.
	 * 
     * @return das umgebende Rechteck
     */
    public Rectangle getBoundingRectangle( int type) {
	    if(_painter instanceof DOTNeedlePainter){
		    // Nadel-koordinaten nicht zwischenspeichern, da Nadeln in der Größe veränderlich sind
		    return _painter.getBoundingRectangle( this, type);
	    }
    	if ( !_boundingRectangles.containsKey( type)) {
    		_boundingRectangles.put(type, _painter.getBoundingRectangle( this, type));
    	}
    	return _boundingRectangles.get( type);
    }
    
	/**
	 * Gibt die umgebende Rechteck zu diesem DisplayObject für den Default-(Koordinaten-)Typ zurück.
	 * 
     * @return das umgebende Rechteck
     */
    public Rectangle getBoundingRectangle() {
    	return getBoundingRectangle( _defaultType);
    }
    
	/**
	 * Konstruiert ein DisplayObject. Hierzu müssen das zugehörige Systemobjekt, die Koordinaten,
	 * ein Painter, die {@link DOTCollection}, eine speziell-aufbereitete Map mit mit Informationen
	 * welche {@link PrimitiveFormPropertyPair}-Objekte zu welchen Darstellungstypen gehören, und
	 * schließlich die Kartenansicht, in der das Objekt gezeichnet werden soll, angegeben werden.
	 * Die Konstruktion dieser Objekte ist eine der Aufgaben der Klasse {@link DisplayObjectManager}.
	 * 
	 * @param systemObject ein Systemobjekt
	 * @param coordinates die Koordinaten zum Default-Type
	 * @param painter der Painter
	 * @param dotCollection die DOTCollcetion
	 * @param primitiveFormPropertyPairs die Paare zu den Darstellungstypen
	 * @param mapPane die Kartenansicht
	 */
    public DisplayObject(SystemObject systemObject,
    		List<Object> coordinates,
    		DisplayObjectPainter painter,
    		DOTCollection dotCollection,
    		Map< DisplayObjectType, List<PrimitiveFormPropertyPair>> primitiveFormPropertyPairs,
    		MapPane mapPane) {
	    super();
	    _systemObject = systemObject;
	    _coordinates.put(0,coordinates);
	    _painter = painter;
	    _dotCollection = dotCollection;
	    
	    _currentDisplayObjectType = null;
	    
	    _defaultType = 0;
	    _mapPane = mapPane;
	    _resultCache = new HashMap<DisplayObjectType, Map<PrimitiveFormPropertyPair, DisplayObjectTypeItem>> ();
	    for ( DisplayObjectType displayObjectType : primitiveFormPropertyPairs.keySet()) {
	    	for ( PrimitiveFormPropertyPair pfPropertyPair: primitiveFormPropertyPairs.get( displayObjectType)) {
	    		Map<PrimitiveFormPropertyPair, DisplayObjectTypeItem> currentDOTMap = _resultCache.get( displayObjectType);
	    		if ( currentDOTMap == null) {
	    			currentDOTMap = new HashMap<PrimitiveFormPropertyPair, DisplayObjectTypeItem>();
	    			_resultCache.put( displayObjectType, currentDOTMap);
	    		
	    		}
	    		currentDOTMap.put( pfPropertyPair, null);
		    }
	    }
    }

    /*
     * Dies ist die Methode, die für das ClientReceiverInterface implementiert wird.
     */
	public void update(ResultData[] results) {
		try {
			int mapScale = _mapPane.getMapScale().intValue();
			// Wen die folgende Schleife überrascht: sie sollte tatsächlich NIE ausgeführt
			// werden, denn das mapPane kennt seinen Maßstab bevor überhaupt Anmeldungen
			// durchgeführt werden. Man könnte auch eine Exception werfen, aber zu so
			// drastischen Mitteln besteht kein Grund. Den SubscriptionDeliveryThread
			// zu blockieren ist natürlich nur eine Ausnahmelösung.
			while ( mapScale == 0) {
				_debug.warning( "Warten auf die Kartenansicht.");
				Thread.sleep( 100);
				mapScale = _mapPane.getMapScale().intValue();
			}
			_currentDisplayObjectType = _dotCollection.getDisplayObjectType( mapScale);
			if ( _currentDisplayObjectType == null) {
				return;
			}
			if ( _displayObjectTypeItems == null) {
				_displayObjectTypeItems = _resultCache.get( _currentDisplayObjectType);
			}
			for (ResultData result : results) {
				final DataDescription dataDescription = result.getDataDescription();
				final AttributeGroup attributeGroup = dataDescription.getAttributeGroup();
				final Aspect aspect = dataDescription.getAspect();
				DOTSubscriptionData subscriptionData = new DOTSubscriptionData (attributeGroup.getPid(), aspect.getPid());
				update( result, subscriptionData);
				updateResultCache( result, subscriptionData);
			}
		} catch(Exception e) {
			_debug.warning("DisplayObject.update(): ein Update konnte nicht durchgeführt werden.", e);
		}
    }
	
	private DisplayObjectTypeItem getDOTItemForState( 
			DisplayObjectType displayObjectType,
			ResultData result, 
			DOTSubscriptionData subscriptionData,
			PrimitiveFormPropertyPair pfPropertyPair) {
		final DataState dataState = result.getDataState();
		final DisplayObjectTypeItem dItem = displayObjectType.getDisplayObjectTypeItemForState(
				pfPropertyPair.getPrimitiveFormName(), pfPropertyPair.getProperty(),
				subscriptionData, dataState);
		return dItem;
	}
	
	private void update ( ResultData result, DOTSubscriptionData subscriptionData) {
		final Data data = result.getData();
    	for ( PrimitiveFormPropertyPair pfPropertyPair : _displayObjectTypeItems.keySet()) {
    		if ( data == null) {
    			final DisplayObjectTypeItem dItem = getDOTItemForState( 
    					_currentDisplayObjectType, result, subscriptionData, pfPropertyPair);
    			_displayObjectTypeItems.put( pfPropertyPair, dItem);
				_mapPane.updateDisplayObject( this);
    		} else {
	    		List<String> attributeNames = _currentDisplayObjectType.getAttributeNames( 
	    				pfPropertyPair.getPrimitiveFormName(), 
	    				pfPropertyPair.getProperty(), subscriptionData);
	    		for ( String attributeName : attributeNames) {
	    			if ( DynamicDefinitionComponent.attributeNameIsState(attributeName)) {
	    				continue;
	    			}
	    			final Data subItem = getSubItem( data, attributeName);
	    			_values.put( pfPropertyPair, subItem);
	    			double value = subItem.asUnscaledValue().doubleValue();
	    			DisplayObjectTypeItem dItem = _currentDisplayObjectType.isValueApplicable( 
	    					pfPropertyPair.getPrimitiveFormName(), pfPropertyPair.getProperty(), 
	    					subscriptionData, attributeName, value);
	    			_displayObjectTypeItems.put( pfPropertyPair, dItem);	// auch bei null!
	    			_mapPane.updateDisplayObject( this);
	    		}
    		}
    	}
	}
	
	private void updateResultCache( ResultData result, DOTSubscriptionData subscriptionData) {
		final Data data = result.getData();
		for ( DisplayObjectType displayObjectType : _resultCache.keySet()) {
			for ( PrimitiveFormPropertyPair pfPropertyPair : _displayObjectTypeItems.keySet()) {
				if ( data == null) {
	    			DisplayObjectTypeItem dItem = getDOTItemForState( displayObjectType, result, 
	    					subscriptionData, pfPropertyPair); 
					_resultCache.get( displayObjectType).put( pfPropertyPair, dItem);
	    		} else {
		    		List<String> attributeNames = displayObjectType.getAttributeNames( 
		    				pfPropertyPair.getPrimitiveFormName(),
		    				pfPropertyPair.getProperty(), subscriptionData);
		    		for ( String attributeName : attributeNames) {
		    			if ( DynamicDefinitionComponent.attributeNameIsState(attributeName)) {
		    				continue;
		    			}
		    			final Data subItem = getSubItem( data, attributeName);
		    			_values.put( pfPropertyPair, subItem);
						double value = subItem.asUnscaledValue().doubleValue();
		    			DisplayObjectTypeItem dItem = displayObjectType.isValueApplicable( 
		    					pfPropertyPair.getPrimitiveFormName(), pfPropertyPair.getProperty(), 
		    					subscriptionData, attributeName, value);
		    			_resultCache.get( displayObjectType).put( pfPropertyPair, dItem);	// auch bei null!
		    		}
	    		}
	    	}
		}
	}
	
	private static Data getSubItem( final Data data, final String attributeName) {
		Data dataItem = data;
		String s = attributeName;
		int index = s.indexOf( '.');
		while ( index > 0) {
			String itemName = s.substring(0, index);
			dataItem = dataItem.getItem( itemName);
			s = s.substring( index+1);
			index = s.indexOf( '.');
		}
		return dataItem.getItem(s);
	}
	
	/**
	 * Setzt den Default-Type.
	 * 
	 * @param defaultType der Default-(Koordinaten-)Typ
	 */
    public void setDefaultType(int defaultType) {
    	_defaultType = defaultType;
    }

    /*
     * Das ist die Methode, die für den MapPane.MapScaleListener implementiert werden muss.
     */
	public void mapScaleChanged(double scale) {
		final DisplayObjectType displayObjectType = _dotCollection.getDisplayObjectType( _mapPane.getMapScale().intValue());
		if ( displayObjectType != _currentDisplayObjectType) {
			_currentDisplayObjectType = displayObjectType;
			_displayObjectTypeItems = _resultCache.get( _currentDisplayObjectType);
		}
    }
	
	/**
	 * Eine ausgabefreundliche Beschreibung des Objekts.
	 * 
	 * @return eine ausgabefreundliche Beschreibung
	 */
	@Override
    public String toString() {
		String s = "[DisplayObject: [" + _systemObject.toString() + "][Number of Coordinates:" + 
			_coordinates.size() + "]" + _dotCollection.toString() + "]";
		return s;
	}

	public MapPane getMapPane() {
		return _mapPane;
	}

	private final SystemObject _systemObject;
	
	private final Map<Integer, List<Object> > _coordinates = new HashMap<Integer, List<Object> >();
	
	private final DisplayObjectPainter _painter;

	private DisplayObjectType _currentDisplayObjectType;
	
	private Map<PrimitiveFormPropertyPair, DisplayObjectTypeItem> _displayObjectTypeItems = null;
	
	private final Map<PrimitiveFormPropertyPair, Data> _values = new HashMap<PrimitiveFormPropertyPair, Data>();
	
	private final DOTCollection _dotCollection;

	private final Map<Integer, Rectangle> _boundingRectangles = new HashMap<Integer, Rectangle>();
	
	private int _defaultType;
	
	private final MapPane _mapPane;
	
	private Map<DisplayObjectType, Map<PrimitiveFormPropertyPair, DisplayObjectTypeItem>> _resultCache;
	
	private static final Debug _debug = Debug.getLogger();
}
