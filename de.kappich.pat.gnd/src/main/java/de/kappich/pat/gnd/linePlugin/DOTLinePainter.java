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
package de.kappich.pat.gnd.linePlugin;

import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.PrimitiveFormPropertyPair;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Der Painter für Linienobjekte.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9143 $
 *
 */
@SuppressWarnings("serial")
public class DOTLinePainter extends JPanel implements DisplayObjectPainter {
	
	static float [] _dashes = {10.0F, 3.0F, 5.0F, 3.0F};
	
	public void paintDisplayObject(MapPane mapPane, Graphics2D g2D, 
			DisplayObject displayObject, boolean selected) {
		final DOTLine dotLine = (DOTLine) displayObject.getDOTCollection().getDisplayObjectType(
				mapPane.getMapScale().intValue());
		if ( dotLine != null ) {
			final Color color;
			if ( dotLine.isPropertyStatic( null, DOTProperty.FARBE)) {
				color = ColorManager.getInstance().getColor((String) dotLine.getValueOfStaticProperty( null, DOTProperty.FARBE));
			} else {
				final PrimitiveFormPropertyPair primitiveFormPropertyPair = new PrimitiveFormPropertyPair( null, DOTProperty.FARBE);
				if ( primitiveFormPropertyPair == null) {
					return;
				}
				final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
				if ( displayObjectTypeItem == null) {
					return;
				}
				final Object propertyValue = displayObjectTypeItem.getPropertyValue();
				if ( propertyValue != null) {
					color = ColorManager.getInstance().getColor((String) propertyValue);
				} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
					color = ColorManager.getInstance().getColor( "keine");
				} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
					color = ColorManager.getInstance().getColor( "keine");
				} else {
					color = ColorManager.getInstance().getColor( "keine");
				}
				// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
				// Erweiterung muss hier unterschieden werden.
			}
			
			final Integer distance;
			if ( dotLine.isPropertyStatic( null, DOTProperty.ABSTAND)) {
				distance = (Integer) dotLine.getValueOfStaticProperty( null, DOTProperty.ABSTAND);
			} else {
				final PrimitiveFormPropertyPair primitiveFormPropertyPair = new PrimitiveFormPropertyPair( null, DOTProperty.ABSTAND);
				final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
				if ( displayObjectTypeItem == null) {
					return;
				}
				distance = (Integer) displayObjectTypeItem.getPropertyValue();
			}
			
			final Double strokeWidth;
			if ( dotLine.isPropertyStatic( null, DOTProperty.STRICHBREITE)) {
				strokeWidth = (Double) dotLine.getValueOfStaticProperty( null, DOTProperty.STRICHBREITE);
			} else {
				final PrimitiveFormPropertyPair primitiveFormPropertyPair = new PrimitiveFormPropertyPair( null, DOTProperty.STRICHBREITE);
				final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
				if ( displayObjectTypeItem == null) {
					return;
				}
				strokeWidth = (Double) displayObjectTypeItem.getPropertyValue();
			}
			if ( !selected ) {
				g2D.setColor( color);
				g2D.setStroke( new BasicStroke( strokeWidth.floatValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
			} else {
				g2D.setColor(color.darker());
				g2D.setStroke( new BasicStroke( 2 * strokeWidth.floatValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f));
			}
			for ( Object o : displayObject.getCoordinates( distance)) {
				Path2D.Double polyline = (Path2D.Double) o;
				g2D.draw( polyline);
			}
			displayObject.setDefaultType( distance);
		}
	}
	
	/*
	 * Gibt die Polylines aus dem Standardausgabekanal aus.
	 */
	@SuppressWarnings("unused")
	private void dumpPolyline( final Path2D.Double polyline, final Path2D.Double newPolyline) {
		PathIterator pi = polyline.getPathIterator(new AffineTransform());
		PathIterator npi = newPolyline.getPathIterator(new AffineTransform());
		while ( !pi.isDone() && !npi.isDone() ) {
			double[] coords = new double[6];
			pi.currentSegment(coords);
			double[] ncoords = new double[6];
			npi.currentSegment(ncoords);
			System.out.println("x: " + (int) coords[0] + " y: " + (int) coords[1]+ "  x: " + (int) ncoords[0] + " y: " + (int) ncoords[1]);
			pi.next();
			npi.next();
		}
		while ( !pi.isDone()) {
			double[] coords = new double[6];
			pi.currentSegment(coords);
			System.out.println("x: " + (int) coords[0] + " y: " + (int) coords[1]+ "  x:  y: ");
			pi.next();
		}
		while ( !npi.isDone()) {
			double[] ncoords = new double[6];
			npi.currentSegment(ncoords);
			System.out.println("x:  y:  x: " + (int) ncoords[0] + " y: " + (int) ncoords[1]);
			npi.next();
		}
	}
	
	/*
	 * Berechnet einen um distance verschobenen Polygonzug. 
	 */
	private Path2D.Double getMovedPolyline( Path2D.Double polyline, int distance) {
		final PathIterator pathIterator = polyline.getPathIterator( new AffineTransform());
		if ( pathIterator.isDone()) {
			return new Path2D.Double();
		}
		double[] coords = new double[6];
		int type = pathIterator.currentSegment(coords);
		if ((type != PathIterator.SEG_MOVETO) && (type != PathIterator.SEG_LINETO)) {
			return new Path2D.Double();
		}
		Point2D.Double p1 = new Point2D.Double( (int) coords[0], (int) coords[1]);
		pathIterator.next();
		if ( pathIterator.isDone() ) {
			return new Path2D.Double();
		}
		type = pathIterator.currentSegment(coords);
		if ((type != PathIterator.SEG_MOVETO) && (type != PathIterator.SEG_LINETO)) {
			return new Path2D.Double();
		}
		Point2D.Double p2 = new Point2D.Double( (int) coords[0], (int) coords[1]);
		while ( p1.distance(p2) < 10.) {	// "vorwärts spulen"
			pathIterator.next();
			if ( pathIterator.isDone()) {
				break;
			}
			type = pathIterator.currentSegment(coords);
			if ((type != PathIterator.SEG_MOVETO) && (type != PathIterator.SEG_LINETO)) {
				break;
			}
			p2.setLocation(coords[0], coords[1]);
		}
		if ( (p1.getX() == p2.getX()) && (p1.getY() == p2.getY()) ) {
			return new Path2D.Double();
		}
		Point2D.Double p = getFirstPoint(p1, p2, distance);
		Point2D.Double lastP1 = p1;
		Point2D.Double lastP2 = p2;		
		Path2D.Double resultPolyline = new Path2D.Double();
		resultPolyline.moveTo(p.getX(),p.getY());
		while ( !pathIterator.isDone()) {
			while ( p1.distance(p2) < 10.) {	// "vorwärts spulen"
				pathIterator.next();
				if ( pathIterator.isDone()) {
					break;
				}
				type = pathIterator.currentSegment(coords);
				if ((type != PathIterator.SEG_MOVETO) && (type != PathIterator.SEG_LINETO)) {
					break;
				}
				p2.setLocation(coords[0], coords[1]);
			}
			if ( p1.distance(p2) > 10.) {
				p = getNextPoint( p1, p2, distance);
			}
			resultPolyline.lineTo(p.getX(),p.getY());
			lastP1 = p1;
			lastP2 = p2;
			
			if ( !pathIterator.isDone()) {
				pathIterator.next();
				if ( pathIterator.isDone()) {
					break;
				}
				type = pathIterator.currentSegment(coords);
				if ((type != PathIterator.SEG_MOVETO) && (type != PathIterator.SEG_LINETO)) {
					break;
				}
				p1.setLocation( p2);
				p2.setLocation(coords[0], coords[1]);
			}
		}
		p = getLastPoint(lastP1,lastP2,distance);
		resultPolyline.lineTo(p.getX(),p.getY());
		
		return resultPolyline;
	}
	
	@SuppressWarnings("unused")
    private static int sizeOf( final Path2D.Double polyline) {
		int i = 0;
		final PathIterator pathIterator = polyline.getPathIterator( new AffineTransform());
		while ( !pathIterator.isDone()) {
			++i;
			pathIterator.next();
		}
		return i;
	}
	
	/*
	 * Berechnet den ersten Punkt einer verschobenen Linie.
	 */
	private Point2D.Double getFirstPoint( Point2D.Double p1, Point2D.Double p2, int distance) {
		return calculateTranslatedPoint( p1, p2, distance);
	}
	
	/*
	 * Berechnet einen inneren Punkt einer verschobenen Linie.
	 */
	private Point2D.Double getNextPoint( Point2D.Double p1, Point2D.Double p2, int distance) {
		Point2D.Double p = new Point2D.Double( (p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2);
		return calculateTranslatedPoint( p, p2, distance);
	}
	/*
	 * Berechnet den letzten Punkt einer verschobenen Linie.
	 */
	private Point2D.Double getLastPoint( Point2D.Double p1, Point2D.Double p2, int distance) {
		return calculateTranslatedPoint( p2, p1, -distance);
	}
	
	/**
	 * Berechnet in der Ebene einen um <code>|d|</code> Einheiten verschobenen Punkt, den man erhält, wenn 
	 * man den Punkt <code>a</code> senkrecht zu der durch die Punkte <code>a</code> und <code>b</code> 
	 * gegebenen Linie verschiebt. Das Vorzeichen von <code>d</code> entscheidet über die Richtung der 
	 * Verschiebung: bei positiven Vorzeichen wird der Punkt nach rechts verschoben, wenn man von <code>a</code> 
	 * nach <code>b</code> blickt, sonst nach links.
	 * <p>
	 * Die Punkte <code>a</code> und <code>b</code> müssen verschieden sein, damit die Verbindungslinie
	 * eine Richtung und eine nicht verschwindende Länge besitzt.
	 */
	
	private Point2D.Double calculateTranslatedPoint(Point2D.Double a, Point2D.Double b, double d) {
		double dy = b.y - a.y;
		double dx = b.x - a.x;
		
		double length =  Math.sqrt((dx * dx) + (dy * dy));
		double x = a.x - ((dy * d) / length);
		double y = a.y + ((dx * d) / length);
		return new Point2D.Double(x,y);
	}
	
	public Rectangle getBoundingRectangle(DisplayObject displayObject, int type) {
		Rectangle rect = null;
		for ( Object o : displayObject.getCoordinates(type)) {
			Path2D.Double polyline = (Path2D.Double) o;
			if ( rect == null) {
				rect = polyline.getBounds();
			} else {
				rect.add( polyline.getBounds());
			}
		}
		if ( rect != null) {
			rect.grow(5, 5);	// Wegen der Selektion!
		}
		return rect;
	}
	
	public List<Object> getCoordinates(List<Object> coordinates, int type) {
		List<Object> returnList = new ArrayList<Object>();
		for ( Object o : coordinates) {
			Path2D.Double polyline = (Path2D.Double) o;
			returnList.add( getMovedPolyline( polyline, type));
		}
		return returnList;
	}
}
