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
package de.kappich.pat.gnd.areaPlugin;

import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.PrimitiveFormPropertyPair;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPanel;

/**
 * Der Painter für Flächenobjekte.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7809 $
 *
 */
@SuppressWarnings("serial")
public class DOTAreaPainter extends JPanel implements DisplayObjectPainter {
	
	public void paintDisplayObject(MapPane mapPane, Graphics2D g2D, 
			DisplayObject displayObject, boolean selected) {
		final DOTArea dotArea = (DOTArea) displayObject.getDOTCollection().getDisplayObjectType(
				mapPane.getMapScale().intValue());
		if ( dotArea != null ) {
			final Color color;
			if ( dotArea.isPropertyStatic( null, DOTProperty.FARBE)) {
				color = ColorManager.getInstance().getColor((String) dotArea.getValueOfStaticProperty( null, DOTProperty.FARBE));
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
			
			if ( !selected ) {
				g2D.setColor( color);
				g2D.setStroke( new BasicStroke( 1.f));
			} else {
				g2D.setColor(color.darker());
				g2D.setStroke( new BasicStroke( 3.f));
			}
			for ( Object o : displayObject.getCoordinates( 0)) {
				Polygon polygon = (Polygon) o;
				g2D.fill( polygon);
			}
			displayObject.setDefaultType( 0);
		}
    }

	public Rectangle getBoundingRectangle(DisplayObject displayObject, int type) {
		Rectangle rect = null;
		for ( Object o : displayObject.getCoordinates(type)) {
			Polygon polygon = (Polygon) o;
			if ( rect == null) {
				rect = polygon.getBounds();
			} else {
				rect.add( polygon.getBounds());
			}
		}
		return rect;
    }

	public List<Object> getCoordinates(List<Object> coordinates, int type) {
		return coordinates;
    }
}
