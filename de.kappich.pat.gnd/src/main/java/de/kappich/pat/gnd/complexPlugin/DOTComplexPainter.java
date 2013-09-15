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
package de.kappich.pat.gnd.complexPlugin;

import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.PrimitiveFormPropertyPair;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObjectManager.PointWithAngle;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JPanel;

/**
 * Der Painter des Plugins für komplexe Objekte.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8055 $
 *
 */

@SuppressWarnings("serial")
public class DOTComplexPainter extends JPanel implements DisplayObjectPainter {
	
	public void paintDisplayObject(MapPane mapPane, Graphics2D g2D, 
			DisplayObject displayObject, boolean selected) {
		final DOTComplex dotComplex = (DOTComplex) displayObject.getDOTCollection().getDisplayObjectType(
				mapPane.getMapScale().intValue());
		if ( dotComplex != null ) {
			final Color color;
			if ( dotComplex.isPropertyStatic( null, DOTProperty.FARBE)) {
				color = ColorManager.getInstance().getColor((String) dotComplex.getValueOfStaticProperty( null, DOTProperty.FARBE));
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
				// Erweiterung kann hier unterschieden werden.
			}
			
			if ( !selected ) {
				g2D.setColor( color);
				g2D.setStroke( new BasicStroke( 1.f));
			} else {
				g2D.setColor(color.darker());
				g2D.setStroke( new BasicStroke( 3.f));
			}
			for ( Object o : displayObject.getCoordinates( 0)) {
				if ( o instanceof PointWithAngle) {
					PointWithAngle pointWithAngle = (PointWithAngle) o;
					final Point2D point = pointWithAngle.getPoint();
					final Double  pointX = point.getX();
					final Double  pointY = point.getY();
					g2D.drawOval( pointX.intValue()-1, pointY.intValue()-1, 2, 2);
				} else if ( o instanceof Path2D.Double) {
					Path2D.Double polyline = (Path2D.Double) o;
					g2D.draw( polyline);
				} else if ( o instanceof Polygon) {
					Polygon polygon = (Polygon) o;
					g2D.fill( polygon);
				}
			}
			displayObject.setDefaultType( 0);
		}
    }

	public Rectangle getBoundingRectangle(DisplayObject displayObject, int type) {
		Rectangle rect = null;
		for ( Object o : displayObject.getCoordinates(type)) {
			if ( o instanceof PointWithAngle) {
				PointWithAngle pointWithAngle = (PointWithAngle) o;
				final Point2D point = pointWithAngle.getPoint();
				final Double x = point.getX();
				final Double y = point.getY();
				Point p = new Point( x.intValue(), y.intValue());
				if ( rect == null) {
					rect = new Rectangle( p);
				} else {
					rect.add( p);
				}
			} else if ( o instanceof Path2D.Double) {
				Path2D.Double polyline = (Path2D.Double) o;
				if ( rect == null) {
					rect = polyline.getBounds();
				} else {
					rect.add( polyline.getBounds());
				}
			} else if ( o instanceof Polygon) {
				Polygon polygon = (Polygon) o;
				if ( rect == null) {
					rect = polygon.getBounds();
				} else {
					rect.add( polygon.getBounds());
				}
			}
		}
		return rect;
    }

	public List<Object> getCoordinates(List<Object> coordinates, int type) {
		return coordinates;
    }
}
