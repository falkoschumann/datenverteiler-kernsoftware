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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.Data.ReferenceArray;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.coorTransform.GeoTransformation;
import de.kappich.pat.gnd.coorTransform.UTMCoordinate;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.gnd.MapPane.MapScaleListener;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.viewManagement.ViewEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Der DisplayObjectManager initialisiert die DisplayObjects und stellt sie zur Verfügung.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9278 $
 *
 */
public class DisplayObjectManager {
	
	private ClientDavInterface _connection;
	
	private DataModel _configuration;
	
	private MapPane _mapPane;
	
	private Deque<DisplayObject> _unsubscribedDisplayObjects;
	private Deque<MapScaleListener> _unaddedMapScaleListeners;
	
	private SystemObjectType _lineComposedOfLinesType;
	private SystemObjectType _lineWithCoordinatesType;
	private AttributeGroup _composedOfLinesAttributeGroup;
	private AttributeGroup _lineCoordinatesAttributeGroup;
	
	private SystemObjectType _pointOnLineType;
	private SystemObjectType _pointWithCoordinatesType;
	private AttributeGroup _pointOnLineAttributeGroup;
	private AttributeGroup _pointCoordinateAttributeGroup;
	
	private AttributeGroup _areaCoordinatesAttributeGroup;
	
	private AttributeGroup _complexCoordinatesAttributeGroup;
	
	private double _xMin = Double.POSITIVE_INFINITY;
	private double _yMin = Double.POSITIVE_INFINITY;
	private double _xMax = Double.NEGATIVE_INFINITY;
	private double _yMax = Double.NEGATIVE_INFINITY;
	
	static double _ignoreDistance = 0.3;
	
	final private static Object _configurationAccess = new Object();
	
	final private static Debug _debug = Debug.getLogger();
	
	/**
	 * Der Konstruktor der DisplayObject-Verwaltung.
	 * 
	 * @param connection die Datenverteiler-Verbindung
	 * @param mapPane die Kartenansicht
	 */
	public DisplayObjectManager( ClientDavInterface connection, MapPane mapPane) {
		_connection = connection;
		_configuration = _connection.getDataModel();
		_mapPane = mapPane;
		_unsubscribedDisplayObjects = new LinkedList<DisplayObject>();
		_unaddedMapScaleListeners = new LinkedList<MapScaleListener>();
	}
	
	/**
	 * Der DisplayObjectManager initialisiert beim Aufruf dieser Methode alle DisplayObjects
	 * zu dem in dem ViewEntry enthaltenen Layer.
	 * 
	 *
	 * @param entry ein ViewEntry
	 * @param progressBar
	 * @return die Liste aller DisplayObjects des Layers des ViewEntrys
	 */
	public List<DisplayObject> getDisplayObjects(ViewEntry entry, final JProgressBar progressBar) {
		List<DisplayObject> returnList = new ArrayList<DisplayObject>();
		
		final String geoReferenceType = entry.getLayer().getGeoReferenceType();
		final SystemObjectType systemObjectType = _configuration.getType( geoReferenceType);
		if ( systemObjectType == null) {
			_debug.warning( "Der Typ " + geoReferenceType + " ist in der Konfiguration unbekannt.");
			return returnList;
		}
		final List<SystemObject> systemObjects = systemObjectType.getElements();
		
		SystemObjectType pointType = _configuration.getType( "typ.punkt");
		SystemObjectType lineType = _configuration.getType( "typ.linie");
		SystemObjectType areaType = _configuration.getType( "typ.fläche");
		SystemObjectType complexType = _configuration.getType( "typ.komplex");
		
		if ( systemObjectType.inheritsFrom(pointType)) {
			initializePoints( entry, systemObjects, returnList, progressBar);
		} else if ( systemObjectType.inheritsFrom(lineType)) {
			initializeLines( entry, systemObjects, returnList, progressBar);
		} else if ( systemObjectType.inheritsFrom(areaType)) {
			initializeAreas( entry, systemObjects, returnList, progressBar);
		} else if ( systemObjectType.inheritsFrom(complexType)) {
			initializeComplexes( entry, systemObjects, returnList, progressBar);
		}
		synchronized(_unsubscribedDisplayObjects) {
			_unsubscribedDisplayObjects.addAll(returnList);
		}
		synchronized(_unaddedMapScaleListeners) {
			_unaddedMapScaleListeners.addAll(returnList);
		}
		return returnList;
	}
	
	private Rectangle getDisplayRectangle() {
		if ((_xMin == Double.MAX_VALUE) || (_yMin == Double.MAX_VALUE) ||
				(_xMax == Double.MIN_VALUE) || (_yMax == Double.MIN_VALUE)) {
			return null;
		}
		Rectangle rectangle = new Rectangle( (int)_xMin, (int)_yMin, (int)(_xMax-_xMin), (int)(_yMax-_yMin));
		return increaseRectangle(rectangle);
	}
	
	/**
	 * Gibt das die SystemObjects umgebende Rechteck zurück. Ist die Liste leer, so wird
	 * das Gesamtrechteck zurückgegeben.
	 * 
	 * @param systemObjects eine Liste von Systemobjekten oder <code>null</code>
	 * @return das anzuzeigende Rechteck 
	 */
	public Rectangle getDisplayRectangle( List<SystemObject> systemObjects) {
		if ( (systemObjects == null) || systemObjects.isEmpty()) {
			return getDisplayRectangle();
		}
		Rectangle rect = null;
		for ( SystemObject systemObject : systemObjects) {
			SystemObjectType systemObjectType = systemObject.getType();
			SystemObjectType pointType = _configuration.getType( "typ.punkt");
			SystemObjectType lineType = _configuration.getType( "typ.linie");
			SystemObjectType areaType = _configuration.getType("typ.fläche");
			SystemObjectType complexType = _configuration.getType( "typ.komplex");
			// ToDo Flächen und Kompexe
			if ( systemObjectType.inheritsFrom(pointType)) {
				List<Object> coordinatesList = getPointCoordinates( systemObject);
				if ( !coordinatesList.isEmpty()) {
					final PointWithAngle pointWithAngle = (PointWithAngle) coordinatesList.get(0);
					Point2D point = pointWithAngle.getPoint();
					if (rect == null) {
						rect = new Rectangle( new Point((int) point.getX(), (int) point.getY()));
					} else {
						rect.add(point);
					}
				}
			} else if ( systemObjectType.inheritsFrom(lineType)) {
				for ( Object o : getPolylines(systemObject)) {
					Path2D.Double polyline = (Path2D.Double) o;
					if (rect == null) {
						rect = polyline.getBounds();
					} else {
						rect.add( polyline.getBounds());
					}
				}
			} else if ( systemObjectType.inheritsFrom(areaType)) {
				for ( Object o : getAreaCoordinates(systemObject)) {
					Polygon polygon = (Polygon) o;
					if (rect == null) {
						rect = polygon.getBounds();
					} else {
						rect.add( polygon.getBounds());
					}
				}
			} else if ( systemObjectType.inheritsFrom(complexType)) {
				for ( Object o : getComplexCoordinates(systemObject)) {
					if ( o instanceof PointWithAngle) {
						final PointWithAngle pointWithAngle = (PointWithAngle) o;
						Point2D point = pointWithAngle.getPoint();
						if (rect == null) {
							rect = new Rectangle( new Point((int) point.getX(), (int) point.getY()));
						} else {
							rect.add(point);
						}
					} else if ( o instanceof Path2D.Double) {
						Path2D.Double polyline = (Path2D.Double) o;
						if (rect == null) {
							rect = polyline.getBounds();
						} else {
							rect.add( polyline.getBounds());
						}
					} else if ( o instanceof Polygon) {
						Polygon polygon = (Polygon) o;
						if (rect == null) {
							rect = polygon.getBounds();
						} else {
							rect.add( polygon.getBounds());
						}
					}
				}
			}
		}
		return increaseRectangle( rect);
	}
	
	private Rectangle increaseRectangle( Rectangle rectangle) {
		if ( rectangle == null) {
			return null;
		}
		Rectangle returnRectangle = null;
		if (rectangle.height > 100 && rectangle.width > 100) {
			int newWidth = (int) (1.03 * rectangle.width);
			int newHeight = (int) (1.03 * rectangle.height);
			
			Point p = new Point( (int) (rectangle.getMinX() + rectangle.width - newWidth),
					(int) (rectangle.getMinY() + rectangle.height - newHeight));
			returnRectangle = new Rectangle( p);
			p = new Point( (int) (rectangle.getMaxX() - rectangle.width + newWidth),
					(int) (rectangle.getMaxY() - rectangle.height + newHeight));
			returnRectangle.add(p);
		} else {
			returnRectangle = rectangle;
			returnRectangle.grow( 50, 50);
		}
		return returnRectangle;
	}
	
	private Map< DisplayObjectType, List<PrimitiveFormPropertyPair>> getPrimitiveFormPropertyPairs( 
			final DOTCollection dotCollection) {
		Map<DisplayObjectType, List<PrimitiveFormPropertyPair>> map = 
			new HashMap<DisplayObjectType, List<PrimitiveFormPropertyPair>>();
		for ( DisplayObjectType displayObjectType : dotCollection.values()) {
			List<PrimitiveFormPropertyPair> list = new ArrayList<PrimitiveFormPropertyPair>();
			final int length = displayObjectType.getDisplayObjectTypePlugin().getPrimitiveFormTypes().length;
			if ( length == 0) {	// bei Linien und Flächen
				final List<DOTProperty> dynamicProperties = displayObjectType.getDynamicProperties( null);
				for ( DOTProperty dynamicProperty: dynamicProperties) {
					PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair( null, dynamicProperty);
					list.add( pfpPair);
				}
			} else {	// bei Punkten
				final Set<String> primitiveFormNames = displayObjectType.getPrimitiveFormNames();
				for ( String primitiveFormName : primitiveFormNames) {
					final List<DOTProperty> dynamicProperties = displayObjectType.getDynamicProperties( primitiveFormName);
					for ( DOTProperty dynamicProperty: dynamicProperties) {
						PrimitiveFormPropertyPair pfpPair = new PrimitiveFormPropertyPair( primitiveFormName, dynamicProperty);
						list.add( pfpPair);
					}
				}
			}
			if ( !list.isEmpty()) {
				map.put( displayObjectType, list);
			}
		}
		return map;
	}
	
	private void initializePoints(
			ViewEntry entry, final List<SystemObject> systemObjects, List<DisplayObject> returnList, final JProgressBar progressBar) {
		_pointOnLineType = _configuration.getType("typ.punktLiegtAufLinienObjekt");
		_pointWithCoordinatesType = _configuration.getType("typ.punktXY");
		_pointOnLineAttributeGroup = _configuration.getAttributeGroup("atg.punktLiegtAufLinienObjekt");
		_pointCoordinateAttributeGroup = _configuration.getAttributeGroup("atg.punktKoordinaten");
		_lineComposedOfLinesType = _configuration.getType("typ.bestehtAusLinienObjekten");
		_lineWithCoordinatesType = _configuration.getType("typ.linieXY");
		_composedOfLinesAttributeGroup = _configuration.getAttributeGroup("atg.bestehtAusLinienObjekten");
		_lineCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.linienKoordinaten");

		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setIndeterminate(false);
						progressBar.setMaximum(systemObjects.size());
					}
				}
		);

		final int chunkSize = 100;
		for(int i = 0; i < systemObjects.size(); i += chunkSize) {
			final int val = i;
			SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							progressBar.setValue(val);
						}
					}
			);
			final List<SystemObject> subList = systemObjects.subList(i, Math.min(i + chunkSize, systemObjects.size()));
			_configuration.getConfigurationData(subList, _pointCoordinateAttributeGroup);
			final Data[] configurationData = _configuration.getConfigurationData(subList, _pointOnLineAttributeGroup);
			final Collection<Long> lines = new ArrayList<Long>();
			for(Data data : configurationData) {
				if(data != null) {
					final long line = data.getReferenceValue("LinienReferenz").getId();
					if(line != 0) {
						lines.add(line);
					}
				}
			}
			if(lines.size() > 0) {
				final Collection<SystemObject> objects = getObjects(lines);
				_configuration.getConfigurationData(objects, _composedOfLinesAttributeGroup);
				preloadLines(objects);
			}
		}
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setValue(systemObjects.size());
					}
				}
		);


		final DOTCollection dotCollection = entry.getLayer().getDotCollection();
		final Iterator<DisplayObjectType> iterator = dotCollection.values().iterator();
		if(!iterator.hasNext()) {
			return;
		}
		final DisplayObjectType dot = iterator.next();
		final DisplayObjectTypePlugin displayObjectTypePlugin = dot.getDisplayObjectTypePlugin();
		DisplayObjectPainter painter = displayObjectTypePlugin.getPainter();
		Map<DisplayObjectType, List<PrimitiveFormPropertyPair>> pfPropertyPairs = getPrimitiveFormPropertyPairs(dotCollection);
		for(SystemObject systemObject : systemObjects) {
			final List<Object> pointCoordinates = getPointCoordinates(systemObject);
			DisplayObject displayObject = new DisplayObject(
					systemObject, pointCoordinates, painter, dotCollection, pfPropertyPairs, _mapPane
			);
			returnList.add(displayObject);
		}
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setValue(0);
						progressBar.setIndeterminate(true);
					}
				}
		);
	}

	private Collection<SystemObject> getObjects(final Collection<Long> ids) {
//		return ((DafDataModel)_configuration).getObjects(lines);
		final ArrayList<SystemObject> result = new ArrayList<SystemObject>(ids.size());
		for(Long id : ids) {
			result.add(_configuration.getObject(id));
		}
		return result;
	}

	private void initializeLines(
			ViewEntry entry, final List<SystemObject> systemObjects, List<DisplayObject> returnList, final JProgressBar progressBar) {
		_lineComposedOfLinesType = _configuration.getType("typ.bestehtAusLinienObjekten");
		_lineWithCoordinatesType = _configuration.getType("typ.linieXY");
		_composedOfLinesAttributeGroup = _configuration.getAttributeGroup("atg.bestehtAusLinienObjekten");
		_lineCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.linienKoordinaten");

		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setIndeterminate(false);
						progressBar.setMaximum(systemObjects.size());
					}
				}
		);

		final int chunkSize = 100;
		for(int i = 0; i < systemObjects.size() - chunkSize; i += chunkSize) {

			final int val = i;
			SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							progressBar.setValue(val);
						}
					}
			);
			final List<SystemObject> subList = systemObjects.subList(i, i + chunkSize);
			preloadLines(subList);
		}

		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setValue(systemObjects.size());
					}
				}
		);


		final DOTCollection dotCollection = entry.getLayer().getDotCollection();
		final Iterator<DisplayObjectType> iterator = dotCollection.values().iterator();
		if(!iterator.hasNext()) {
			return;
		}
		final DisplayObjectType dot = iterator.next();
		final DisplayObjectTypePlugin displayObjectTypePlugin = dot.getDisplayObjectTypePlugin();
		DisplayObjectPainter painter = displayObjectTypePlugin.getPainter();
		final Map<DisplayObjectType, List<PrimitiveFormPropertyPair>> pfpPairs = getPrimitiveFormPropertyPairs(dotCollection);
		for(SystemObject systemObject : systemObjects) {
			final List<Object> polylines = getPolylines(systemObject);
			DisplayObject displayObject = new DisplayObject(
					systemObject, polylines, painter, dotCollection, pfpPairs, _mapPane
			);
			returnList.add(displayObject);
		}
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						progressBar.setValue(0);
						progressBar.setIndeterminate(true);
					}
				}
		);
	}

	private void preloadLines(final Collection<SystemObject> objectCollection) {
		_configuration.getConfigurationData(objectCollection, _lineCoordinatesAttributeGroup);
		final Data[] configurationData = _configuration.getConfigurationData(objectCollection, _composedOfLinesAttributeGroup);
		final Collection<Long> lines = new ArrayList<Long>();
		for(Data data : configurationData) {
			if(data != null){
				final Data.ReferenceValue[] linesReferenceValues = data.getReferenceArray("LinienReferenz").getReferenceValues();
				for(Data.ReferenceValue line : linesReferenceValues) {
					lines.add(line.getId());
				}
			}
		}
		if(lines.size() > 0) {
			final List<SystemObject> objects = new ArrayList<SystemObject>(getObjects(lines));
			final int chunkSize = 100;
			for(int i = 0; i < objects.size() - chunkSize; i += chunkSize) {
				final List<SystemObject> subList = objects.subList(i, i + chunkSize);
				_configuration.getConfigurationData(subList, _composedOfLinesAttributeGroup);
				preloadLines(subList);
			}
		}
	}

	@SuppressWarnings("unused")
	private void dumpPolylines( List<Object> polylines) {
		for ( Object o : polylines) {
			Path2D.Double polyline = (Path2D.Double) o;
			final PathIterator pathIterator = polyline.getPathIterator(null);
			double[] coordinates = new double[6];
			System.out.println("Polyline:");
			while ( !pathIterator.isDone()) {
				int type = pathIterator.currentSegment(coordinates);
				switch (type) {
					case PathIterator.SEG_MOVETO:
						System.out.println("X: " + coordinates[0] + ", Y: " + coordinates[1]);
						break;
					case PathIterator.SEG_LINETO:
						System.out.println("X: " + coordinates[0] + ", Y: " + coordinates[1]);
						break;
					default:
						break;
				}
				pathIterator.next();
			}
		}
	}
	
	/**
	 * Diese Klasse kapselt ein Paar bestehend aus einem Punkt und einem Winkel.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 9278 $
	 *
	 */
	public class PointWithAngle {
		
		/**
		 * Konstruiert ein Objekt mit den übergebenen Werten.
		 */
		public PointWithAngle(Point2D point, Double angle) {
			super();
			_point = point;
			_angle = angle;
		}
		
		/**
		 * Gibt den Punkt zurück.
		 */
		public Point2D getPoint() {
			return _point;
		}
		
		/**
		 * Gibt den Winkel zurück.
		 */
		public Double getAngle() {
			return _angle;
		}
		
		private final Point2D _point;
		private final Double _angle;
	}
	
	private List<Object> getPointCoordinates(SystemObject systemObject) {
		// Für jedes Systemobjekt wird höchstens eine Koordinate berechnet.
		// Existieren sowohl eine Linie+Offset-Information als auch eine
		// Koordinate, so wird die Linie+Offset-Information verwendet.
		// In diesem Fall wird der Winkel der Linie an diesem Punkt näherungs-
		// weise berechnet und auch in die Koordinatenliste gesteckt.
		List<Object> pointCoordinate = new ArrayList<Object>();
		if (systemObject.isOfType( _pointOnLineType)) {
			final Data pointOnLineData;
			synchronized(_configurationAccess) {
				pointOnLineData = systemObject.getConfigurationData( _pointOnLineAttributeGroup);
            }
			if ( pointOnLineData != null) {
				final SystemObject line = pointOnLineData.getReferenceValue("LinienReferenz").getSystemObject();
				List<Object> lineCoordinates = getPolylines( line);	// liefert schon UTM
				final Data.NumberValue scaledOffset = pointOnLineData.getScaledValue("Offset");
				if ( scaledOffset.isNumber()) {
					final double offset = scaledOffset.doubleValue();
					PointWithAngle newPoint = determinePointCoordinate( lineCoordinates, offset);
					if ( newPoint != null) {
						pointCoordinate.add( newPoint);
						return pointCoordinate;
					}
				}
			}
		}
		if (systemObject.isOfType( _pointWithCoordinatesType)) {
			final Data coordinatesData;
			synchronized(_configurationAccess) {
				coordinatesData = systemObject.getConfigurationData( _pointCoordinateAttributeGroup);
            }
			if (coordinatesData != null) {
				final Data.NumberValue x = coordinatesData.getScaledValue("x");
				final Data.NumberValue y = coordinatesData.getScaledValue("y");
				final UTMCoordinate utm = new UTMCoordinate();
				if(x.isNumber() && y.isNumber()) {
					GeoTransformation.wGS84ToUTM(x.doubleValue(), y.doubleValue(), utm);
					customiseUTM( utm);
					updateExtremeCoordinates( utm);
					Point2D.Double newPoint = new Point2D.Double( utm.getX(), utm.getY());
					PointWithAngle newPiontWithDummyAngle = new PointWithAngle( newPoint, Double.NaN);
					pointCoordinate.add( newPiontWithDummyAngle);
					return pointCoordinate;
				}
			}
		}
		return pointCoordinate;
	}
	
	private PointWithAngle determinePointCoordinate( 
			List<Object> lineCoordinates, // bereits in UTM
			final double offset) {
		if ( lineCoordinates.size() != 1) {
			return null;
		}
		if ( offset < 0.) {
			return null;
		}
		Object o = lineCoordinates.get(0);
		if ( !(o instanceof Path2D.Double)) {
			return null;
		}
		Path2D.Double polyline = (Path2D.Double) o;
		final PathIterator pathIterator = polyline.getPathIterator(null);
		double currentOffset = 0.;
		double currentX = Double.MAX_VALUE; 
		double currentY = Double.MAX_VALUE;
		while (pathIterator.isDone() == false) {
			double[] coordinates = new double[6];
			int type = pathIterator.currentSegment( coordinates);
			switch (type) {
				case PathIterator.SEG_MOVETO:
					currentX = coordinates[0];
					currentY = coordinates[1];
					break;
				case PathIterator.SEG_LINETO:
					double newX = coordinates[0];
					double newY = coordinates[1];
					if ( currentX != Double.MAX_VALUE) {
						double meters = getMeterDistance(currentX, currentY, newX, newY);
						if ( (offset >= currentOffset) && (currentOffset + meters >= offset) ) {	
							double lambda = (offset - currentOffset) / meters;
							double xUTM = currentX + lambda * (newX - currentX);
							double yUTM = currentY + lambda * (newY - currentY);
							return new PointWithAngle(
									new Point2D.Double( xUTM, yUTM), 
									orientedAngleWithXAxis(newX-currentX, newY-currentY));
							
						} else {
							currentOffset += meters;
						}
					}
					currentX = coordinates[0];
					currentY = coordinates[1];
					break;
				default:
					break;
			}
			pathIterator.next();	
		}
		return null;
	}
	
	private Double orientedAngleWithXAxis(double x, double y) {
		double norm = Math.sqrt( x*x + y*y);
		if ( norm == 0.) {
			return null;
		}
		Double cosinus = x / norm;
		if ( cosinus.isNaN() || cosinus.isInfinite()) {
			return null;
		}
		Double angle = Math.acos( cosinus);
		if ( angle.isNaN() || angle.isInfinite()) {
			return null;
		}
		if ( y < 0.) {
			angle = 2 * Math.PI - angle;
		}
		return angle;
	}
	
	private double getMeterDistance(double x1, double y1, double x2, double y2) {
		double dx = x1-x2;
		double dy = y1-y2;
		dx = dx*dx;
		dy = dy*dy;
		return Math.sqrt( dx+dy);
	}
	
	private List<Object> getPolylines(SystemObject systemObject) {
		List<Path2D.Double> rawPolylines = new ArrayList<Path2D.Double>();
		appendCoordinates(systemObject, rawPolylines);
		List<Object> polylines = new ArrayList<Object>();
		simplifyCoordinates(rawPolylines, polylines);
		return polylines;
	}
	
	private void appendCoordinates(SystemObject systemObject, List<Path2D.Double> polylines) {
		// Für jedes Systemobjekt werden entweder nur Koordinaten aus der Komposition
		// oder aus den Koordinatendaten übernommen. Besitzt eine Linie beiderlei Informationen,
		// so werden die Daten der Kompostion gewählt.
		if ( systemObject == null) {
			return;
		}
		int numberOfPolylines = polylines.size();
		if(systemObject.isOfType( _lineComposedOfLinesType)) {
			final Data linesData;
			synchronized(_configurationAccess) {
	            linesData = systemObject.getConfigurationData( _composedOfLinesAttributeGroup);
            }
			if(linesData != null) {
				final SystemObject[] linesArray = linesData.getReferenceArray("LinienReferenz").getSystemObjectArray();
				final java.util.List<SystemObject> lines = Arrays.asList(linesArray);
				for(SystemObject subSystemObject : lines) {
					appendCoordinates(subSystemObject, polylines);
				}
			}
		}
		if ( numberOfPolylines == polylines.size()) {
			if(systemObject.isOfType( _lineWithCoordinatesType)) {
				final Data coordinatesData;
				synchronized(_configurationAccess) {
					coordinatesData = systemObject.getConfigurationData( _lineCoordinatesAttributeGroup);
                }
				if(coordinatesData != null) {
					final Data.NumberArray xArray = coordinatesData.getScaledArray("x");
					final Data.NumberArray yArray = coordinatesData.getScaledArray("y");
					int length = Math.min(xArray.getLength(), yArray.getLength());
					Path2D.Double polyline = new Path2D.Double();
					final UTMCoordinate utm = new UTMCoordinate();
					for(int i = 0; i < length; i++) {
						final Data.NumberValue xValue = xArray.getValue(i);
						final Data.NumberValue yValue = yArray.getValue(i);
						if(xValue.isNumber() && yValue.isNumber()) {
							GeoTransformation.wGS84ToUTM(xValue.doubleValue(), yValue.doubleValue(), utm);
							customiseUTM( utm);
							updateExtremeCoordinates( utm);
							if(i == 0) {
								polyline.moveTo(utm.getX(), utm.getY());
							}
							else {
								polyline.lineTo(utm.getX(), utm.getY());
							}
						}
					}
					polylines.add( polyline);
				}
			}
		}
	}
	
	/**
	 * Gibt die kleinste bisher gefundene x-Koordinate zurück.
	 * 
	 * @return gibt die kleinste bisher gefundene x-Koordinate zurück
	 */
	public double getxMin() {
		return _xMin;
	}
	
	/**
	 * Gibt die kleinste bisher gefundene y-Koordinate zurück. 
	 * 
	 * @return gibt die kleinste bisher gefundene y-Koordinate zurück
	 */
	public double getyMin() {
		return _yMin;
	}
	
	/**
	 * Gibt die größte bisher gefundene x-Koordinate zurück.
	 * 
	 * @return gibt die größte bisher gefundene x-Koordinate zurück
	 */
	public double getxMax() {
		return _xMax;
	}
	
	/**
	 * Gibt die größte bisher gefundene y-Koordinate zurück.
	 * 
	 * @return gibt die größte bisher gefundene y-Koordinate zurück
	 */
	public double getyMax() {
		return _yMax;
	}
	
	@SuppressWarnings("unused")
	private void printExtremeCoordinates() {
		System.out.println("xMin: " + _xMin);
		System.out.println("yMin: " + _yMin);
		System.out.println("xMax: " + _xMax);
		System.out.println("yMax: " + _yMax);
	}
	
	private void updateExtremeCoordinates( UTMCoordinate utm) {
		if (_xMin > utm.getX()) {
			_xMin = utm.getX();
		}
		if (_xMax < utm.getX()) {
			_xMax = utm.getX();
		}
		if (_yMin > utm.getY()) {
			_yMin = utm.getY();
		}
		if (_yMax < utm.getY()) {
			_yMax = utm.getY();
		}
	}
	
	private void simplifyCoordinates(List<Path2D.Double> rawPolylines, List<Object> polylines) {
		// Die Koordinaten in rawPolylines werden folgendermaßen vereinfacht:
		// 1. Doppelte aufeinanderfolgende Punkte werden eliminiert.
		// 2. Leere Polylines werden ignoriert.
		Double lastX = Double.MIN_VALUE;
		Double lastY = Double.MAX_VALUE;
		// An dieser Stelle wurde auf Basis einer hprof-Analyse eine Optimierung mit Hilfe 
		// des Konstruktors Path2D.Double( int winding_rule, int initialCapacity) versucht;
		// das Ergebnis war nicht überzeugend: nach wie vor wurde der double[]-Konstruktor
		// um die 25% der Zeit aufgerufen, nur von 2 Stellen aus.
		Path2D.Double polyline = new Path2D.Double();
		boolean moveTo = true;
		double[] coords = new double[6];
		Double xCoor;
		Double yCoor;
		boolean lastIgnored = false;
		final AffineTransform dummyTransform = new AffineTransform();
		for ( Path2D.Double rawPolyline : rawPolylines) {
			PathIterator pi = rawPolyline.getPathIterator( dummyTransform);
			if ( pi.isDone()) {	// leere Linie
				continue;
			}
			pi.currentSegment(coords);
			xCoor = coords[0];
			yCoor = coords[1];
			if ( (Math.abs(lastX - xCoor) >_ignoreDistance) || (Math.abs(lastY - yCoor) >_ignoreDistance)) {
				if ( !moveTo ) {	// Nur im Sinne von: die letzte Polyline ist nicht leer!
					polylines.add( polyline);
					polyline = new Path2D.Double();
				}
				polyline.moveTo(xCoor, yCoor);
				moveTo = false;
				lastIgnored = false;
			} else {
				lastIgnored = true;
			}
			lastX = xCoor;
			lastY = yCoor;
			
			while ( !pi.isDone()) {
				pi.currentSegment(coords);
				xCoor = coords[0];
				yCoor = coords[1];
				if ( (Math.abs(lastX - xCoor) >_ignoreDistance) || (Math.abs(lastY - yCoor) >_ignoreDistance)) {
					polyline.lineTo(xCoor, yCoor);
					lastIgnored = false;
				} else {
					lastIgnored = true;
				}
				lastX = xCoor;
				lastY = yCoor;
				pi.next();
			}
			lastX = coords[0];
			lastY = coords[1];
		}
		if ( !moveTo || lastIgnored) {
			polylines.add(polyline);
		}
	}
	
	/**
	 * Mit dieser Methode werden alle Anmeldungen beim Datenverteiler vorgenommen,
	 * die sich auf seit dem letzten Aufruf dieser Methode durch Initialisierungen
	 * neuer DisplayObjects ergeben haben.
	 */
	public void subscribeDisplayObjects() {
		int i = 0;
		synchronized(_unsubscribedDisplayObjects) {
			for ( DisplayObject displayObject : _unsubscribedDisplayObjects) {
				final DOTCollection dotCollection = displayObject.getDOTCollection();
				for ( DisplayObjectType displayObjectType : dotCollection.values()) {
					Set<DOTSubscriptionData> allSubscriptionData = displayObjectType.getSubscriptionData();
					for ( DOTSubscriptionData subscriptionData : allSubscriptionData) {
						final AttributeGroup onlineAtg;
						onlineAtg = _configuration.getAttributeGroup(subscriptionData.getAttributeGroup());
						if ( onlineAtg == null) {
							continue;
						}
						final Aspect aspect = _configuration.getAspect(subscriptionData.getAspect());
						if ( aspect == null) {
							continue;
						}
						final DataDescription dataDescription = new DataDescription(onlineAtg, aspect);
						SystemObject[] systemobjects = new SystemObject[1];
						systemobjects[0] = displayObject.getSystemObject();
						_connection.subscribeReceiver(
								displayObject, systemobjects, dataDescription, ReceiveOptions.normal(),
								ReceiverRole.receiver()
						);
						i++;
						if(i % 10 == 0) {
							try {
								Thread.sleep(100);
							}
							catch(InterruptedException e) {
								throw new IllegalStateException(e);
							}
						}
					}
				}
			}
			_unsubscribedDisplayObjects.clear();
		}
	}
	
	/**
	 * Mit dieser Methode werden alle Anmeldungen beim Datenverteiler zurückgenommen,
	 * die sich vom DisplayObjectManager in der Methode subscribeDisplayObjects() für
	 * die übergebenen DisplayObjects gemacht wurden.
	 * 
	 * @param displayObjects eine Menge von DisplayObjects
	 */
	public void unsubscribeDisplayObjects( final Collection<DisplayObject> displayObjects) {
		Runnable unsubscriber = new Runnable() {
			public void run() {
				for ( DisplayObject displayObject : displayObjects) {
					final DOTCollection dotCollection = displayObject.getDOTCollection();
					for ( DisplayObjectType displayObjectType : dotCollection.values()) {
						Set<DOTSubscriptionData> allSubscriptionData = displayObjectType.getSubscriptionData();
						for ( DOTSubscriptionData subscriptionData : allSubscriptionData) {
							final AttributeGroup onlineAtg;
							onlineAtg = _configuration.getAttributeGroup(subscriptionData.getAttributeGroup());
							final Aspect aspect = _configuration.getAspect(subscriptionData.getAspect());
							if ( (onlineAtg != null) && (aspect != null)) {
								final DataDescription dataDescription = new DataDescription(onlineAtg, aspect);
								SystemObject[] systemobjects = new SystemObject[1];
								systemobjects[0] = displayObject.getSystemObject();
								_connection.unsubscribeReceiver( displayObject, systemobjects, dataDescription);
							}
						}
					}
				}
			}
		};
		Thread unsubscriberThread = new Thread( unsubscriber);
		unsubscriberThread.start();
	}
	
	/**
	 * Mit dieser Methode werden alle DisplayObjects, die als MapScaleListener
	 * zu registrieren sind, beim MapPane registriert.
	 */
	public void addMapScaleListeners() {
		synchronized(_unaddedMapScaleListeners) {
			_mapPane.addMapScaleListeners( _unaddedMapScaleListeners);
			_unaddedMapScaleListeners.clear();
		}
	}
	
	private void initializeComplexes(
			ViewEntry entry, List<SystemObject> systemObjects, List<DisplayObject> returnList, final JProgressBar progressBar) {
		_pointOnLineType = _configuration.getType("typ.punktLiegtAufLinienObjekt");
		_pointWithCoordinatesType = _configuration.getType("typ.punktXY");
		_pointOnLineAttributeGroup = _configuration.getAttributeGroup("atg.punktLiegtAufLinienObjekt");
		_pointCoordinateAttributeGroup = _configuration.getAttributeGroup("atg.punktKoordinaten");
		_lineComposedOfLinesType = _configuration.getType("typ.bestehtAusLinienObjekten");
		_lineWithCoordinatesType = _configuration.getType("typ.linieXY");
		_composedOfLinesAttributeGroup = _configuration.getAttributeGroup("atg.bestehtAusLinienObjekten");
		_lineCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.linienKoordinaten");
		_areaCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.flächenKoordinaten");
		_complexCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.komplexKoordinaten");

		final DOTCollection dotCollection = entry.getLayer().getDotCollection();
		final Iterator<DisplayObjectType> iterator = dotCollection.values().iterator();
		if ( !iterator.hasNext()) {
			return;
		}
		final DisplayObjectType dot = iterator.next();
		final DisplayObjectTypePlugin displayObjectTypePlugin = dot.getDisplayObjectTypePlugin();
		DisplayObjectPainter painter = displayObjectTypePlugin.getPainter();
		final Map<DisplayObjectType, List<PrimitiveFormPropertyPair>> pfpPairs = getPrimitiveFormPropertyPairs(dotCollection);
		for(SystemObject systemObject : systemObjects) {
			final List<Object> complexCoordinates = getComplexCoordinates(systemObject);
			DisplayObject displayObject = new DisplayObject(systemObject, complexCoordinates, painter, 
					dotCollection, pfpPairs, _mapPane);
			returnList.add( displayObject);
		}
	}
	
	private List<Object> getComplexCoordinates(SystemObject systemObject) {
		final Data coordinatesData;
		synchronized(_configurationAccess) {
			coordinatesData = systemObject.getConfigurationData( _complexCoordinatesAttributeGroup);
        }
		List<Object> complexCoordinates = new ArrayList<Object>();
		if(coordinatesData != null) {
			final ReferenceArray pointReferences = coordinatesData.getReferenceArray( "PunktReferenz");
			for ( int index = 0; index < pointReferences.getLength(); index++) {
				SystemObject pointSystemObject = pointReferences.getSystemObject( index);
				complexCoordinates.addAll( getPointCoordinates(pointSystemObject));
			}
			final ReferenceArray lineReferences = coordinatesData.getReferenceArray( "LinienReferenz");
			for ( int index = 0; index < lineReferences.getLength(); index++) {
				SystemObject lineSystemObject = lineReferences.getSystemObject( index);
				complexCoordinates.addAll( getPointCoordinates(lineSystemObject));
			}
			final ReferenceArray areaReferences = coordinatesData.getReferenceArray( "FlächenReferenz");
			for ( int index = 0; index < areaReferences.getLength(); index++) {
				SystemObject areaReference = areaReferences.getSystemObject( index);
				complexCoordinates.addAll( getAreaCoordinates(areaReference));
			}
			final ReferenceArray complexReferences = coordinatesData.getReferenceArray( "KomplexReferenz");
			for ( int index = 0; index < complexReferences.getLength(); index++) {
				SystemObject complexReference = complexReferences.getSystemObject( index);
				complexCoordinates.addAll( getAreaCoordinates(complexReference));
			}
		}
		return complexCoordinates;
	}
	
	private void initializeAreas(
			ViewEntry entry, List<SystemObject> systemObjects, List<DisplayObject> returnList, final JProgressBar progressBar) {
		_areaCoordinatesAttributeGroup = _configuration.getAttributeGroup("atg.flächenKoordinaten");
		
		final DOTCollection dotCollection = entry.getLayer().getDotCollection();
		final Iterator<DisplayObjectType> iterator = dotCollection.values().iterator();
		if ( !iterator.hasNext()) {
			return;
		}
		final DisplayObjectType dot = iterator.next();
		final DisplayObjectTypePlugin displayObjectTypePlugin = dot.getDisplayObjectTypePlugin();
		DisplayObjectPainter painter = displayObjectTypePlugin.getPainter();
		final Map<DisplayObjectType, List<PrimitiveFormPropertyPair>> pfpPairs = getPrimitiveFormPropertyPairs( dotCollection);
		for(SystemObject systemObject : systemObjects) {
			final List<Object> complexCoordinates = getAreaCoordinates(systemObject);
			DisplayObject displayObject = new DisplayObject(systemObject, complexCoordinates, painter, 
					dotCollection, pfpPairs, _mapPane);
			returnList.add( displayObject);
		}
	}
	
	private List<Object> getAreaCoordinates(SystemObject systemObject) {
		final Data coordinatesData;
		synchronized(_configurationAccess) {
			coordinatesData = systemObject.getConfigurationData( _areaCoordinatesAttributeGroup);
		}
		List<Object> areaCoordinates = new ArrayList<Object>();
		if(coordinatesData != null) {
			final Data.NumberArray xArray = coordinatesData.getScaledArray("x");
			final Data.NumberArray yArray = coordinatesData.getScaledArray("y");
			int length = Math.min(xArray.getLength(), yArray.getLength());
			Polygon polygon = new Polygon();
			final UTMCoordinate utm = new UTMCoordinate();
			// Im Moment unterscheiden wir hier noch nicht zwischen Punkten, Linien und Flächen.
			for(int i = 0; i < length; i++) {
				final Data.NumberValue xValue = xArray.getValue(i);
				final Data.NumberValue yValue = yArray.getValue(i);
				if(xValue.isNumber() && yValue.isNumber()) {
					GeoTransformation.wGS84ToUTM(xValue.doubleValue(), yValue.doubleValue(), utm);
					customiseUTM( utm);
					updateExtremeCoordinates( utm);
					polygon.addPoint( (int) utm.getX(), (int) utm.getY());
				}
			}
			areaCoordinates.add( polygon);
		}
		return areaCoordinates;
	}
	
	private static void customiseUTM ( UTMCoordinate utm) {
		utm.setY( -utm.getY());
	}
}
