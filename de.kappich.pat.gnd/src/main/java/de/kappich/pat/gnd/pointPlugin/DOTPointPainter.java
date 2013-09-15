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
package de.kappich.pat.gnd.pointPlugin;

import de.kappich.pat.gnd.colorManagement.ColorManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObjectManager;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.PrimitiveFormPropertyPair;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObjectManager.PointWithAngle;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType.DisplayObjectTypeItem;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveForm;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveFormType;

import de.bsvrz.dav.daf.main.Data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Die Implementation von DisplayObjectPainter zum Zeichnen von Punktobjekten.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 *
 */
public class DOTPointPainter implements DisplayObjectPainter {

	public void paintDisplayObject(MapPane mapPane, 
			Graphics2D g2D, 
			DisplayObject displayObject, 
			boolean selected) {
		final DOTPoint dotPoint = (DOTPoint) displayObject.getDOTCollection().getDisplayObjectType(
				mapPane.getMapScale().intValue());
		if ( dotPoint != null) {
			List<Object> coor = displayObject.getCoordinates( 0);
			if ( coor.isEmpty()) {
				return;
			}
			final PointWithAngle pointWithAngle = (PointWithAngle) coor.get(0);
			AffineTransform angleTransformation = getAngleTransformation( 
					dotPoint.getTranslationFactor(),
					pointWithAngle.getAngle());
			final Point2D point = pointWithAngle.getPoint();
			final Double xAsDouble = point.getX();
			final Double yAsDouble = point.getY();
			List<Shape> drawnShapes = new ArrayList<Shape>();
			Ellipse2D.Double pointCircle = null;
			for ( PrimitiveForm primitiveForm : dotPoint.getPrimitiveForms()) {
				final PrimitiveFormType type = primitiveForm.getType();
				if ( type.equals( PrimitiveFormType.PUNKT)) {
					final Color color = determineColor(displayObject, dotPoint, primitiveForm.getName());
					final Double diameter = determineDiameter( displayObject, dotPoint, primitiveForm.getName());
					if ( (diameter == null) || (color == null)) {
						continue;
					}
					if ( !selected) {
						g2D.setColor( color);
					} else {
						g2D.setColor( color.darker());
					}
					final Double radius = diameter / 2.;
					pointCircle = new Ellipse2D.Double( xAsDouble-radius, yAsDouble-radius, 2*radius, 2*radius);
					g2D.fill( pointCircle);
				} else if ( type.equals( PrimitiveFormType.RECHTECK) ||
						type.equals( PrimitiveFormType.KREIS) ||
						type.equals( PrimitiveFormType.HALBKREIS)) {
					final Color fillColor = determineFillColor(displayObject, dotPoint, primitiveForm.getName());
					final Color fillColorWithTransparency;
					if ( fillColor == null ) {	// keine Farbe, deshalb transparent setzen
						fillColorWithTransparency = new Color(0, 0, 0, 0);
					} else if ( fillColor.getAlpha() == 0) { // Farbe ist schon transparent
						fillColorWithTransparency = fillColor;
					} else {
						final Integer transparency = determineTransparency(displayObject, dotPoint, primitiveForm.getName());
						if ( transparency == null) {
							fillColorWithTransparency = new Color(fillColor.getRed(), 
									fillColor.getGreen(), fillColor.getBlue());
						} else {
							fillColorWithTransparency = new Color(fillColor.getRed(), 
									fillColor.getGreen(), fillColor.getBlue(), transparency); 
						}
					}
					if ( !selected) {
						g2D.setColor( fillColorWithTransparency);
					} else {
						g2D.setColor( fillColorWithTransparency.brighter());
					}
					final ShapeWithReferencePoint shapeWithRefPoint = getShape( point, primitiveForm);
					if ( shapeWithRefPoint == null) {
						continue;
					}
					final Shape shape = angleTransformation.createTransformedShape( shapeWithRefPoint.getShape());
					g2D.fill( shape);
					g2D.setColor( Color.BLACK);
					final Double strokeWidth = determineStrokeWidth( displayObject, dotPoint, primitiveForm.getName());
					if ( strokeWidth != null && strokeWidth.floatValue()!=0.0f) {
						g2D.setStroke( new BasicStroke( strokeWidth.floatValue()));
						g2D.draw( shape);
					}
					drawnShapes.add( shape);
				} else if ( type.equals( PrimitiveFormType.TEXTDARSTELLUNG)) {
					final Color color = determineColor(displayObject, dotPoint, primitiveForm.getName());
					if ( !selected) {
						g2D.setColor( color);
					} else {
						g2D.setColor( color.brighter());
					}
					final int style = determineTextStyle(displayObject, dotPoint, primitiveForm.getName());
					final int size = determineTextSize(displayObject, dotPoint, primitiveForm.getName());
					Font font = new Font( null, style, size);
					g2D.setFont(font);
					String text = determineText(displayObject, dotPoint, primitiveForm.getName());
					if ( text == null) {
						continue;
					}
					Point2D.Double translation1 = primitiveForm.getTranslation();
					Point2D.Double translation2 = new Point2D.Double(xAsDouble + translation1.getX(), yAsDouble - translation1.getY());
					final Point2D.Double translation3 = new Point2D.Double();
					angleTransformation.transform(translation2, translation3);
					final Double x = translation3.getX();
					final Double y = translation3.getY();
					g2D.drawString( text, x.intValue(), y.intValue());
				}
			}
			if ( dotPoint.isJoinByLine() ) {
				Point2D.Double translation1 = new Point2D.Double(xAsDouble, yAsDouble);
				final Point2D.Double translation2 = new Point2D.Double();
				angleTransformation.transform(translation1, translation2);
				final Double x = translation2.getX();
				final Double y = translation2.getY();
				Line2D.Double line = new Line2D.Double(xAsDouble.intValue(), yAsDouble.intValue(), x.intValue(), y.intValue());
				if ( pointCircle != null) {
					List<Shape> shapes = new ArrayList<Shape>();
					shapes.add( pointCircle);
					int counter = 0;
					while ( lineIntersectsWithShapes( line, shapes) && counter<10) {
						stretchLineAtTheBeginning( line, 0.9);
						counter++;
					}
				}
				int counter = 0;
				while ( lineIntersectsWithShapes( line, drawnShapes) && counter<10) {
					stretchLineAtTheEnd( line, 0.9);
					counter++;
				}
				g2D.setStroke( new BasicStroke(1.f));
				g2D.draw( line);
			}
		}
	}
	
	/*
	 * Gibt true zurück, wenn die übergebene Linie mindestens eines der Rechtecke, die von
	 * Shape.getBounds() für dieübergebenen Shapes zurückgegeben werden, schneidet. 
	 */
	private boolean lineIntersectsWithShapes( final Line2D line, final List<Shape> shapes) {
		for ( Shape shape : shapes) {
			final Rectangle boundingRectangle = shape.getBounds();
			if ( line.intersects( boundingRectangle)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Verkürzt oder verlängert die übergebene Linie an ihrem Anfang um den übegebenen
	 * Stretchfaktor. Beispiel: ein Strechtfaktor von 0,95 führt zu einer Linie, an deren
	 * Anfang 5 % abgeschnitten wurden.
	 */
	private void stretchLineAtTheBeginning( final Line2D line, double stretchFactor) {
		final Point2D p1 = line.getP1();
		final Point2D p2 = line.getP2();
		line.setLine( p2.getX() + stretchFactor * (p1.getX()-p2.getX()), 
				p2.getY() + stretchFactor * (p1.getY()-p2.getY()),
				p2.getX(), p2.getY());
	}
	/*
	 * Verkürzt oder verlängert die übergebene Linie an ihrem Ende um den übegebenen
	 * Stretchfaktor. Beispiel: ein Strechtfaktor von 0,95 führt zu einer Linie, an deren
	 * Ende 5 % abgeschnitten wurden.
	 */
	private void stretchLineAtTheEnd( final Line2D line, double stretchFactor) {
		final Point2D p1 = line.getP1();
		final Point2D p2 = line.getP2();
		line.setLine( p1.getX(), p1.getY(), 
				p1.getX() + stretchFactor * (p2.getX()-p1.getX()),
				p1.getY() + stretchFactor * (p2.getY()-p1.getY()));
	}
	
	/*
	 * Liefert eine affine Transformation zurück, die eine Translation der Länge translationFactor
	 * in Richtung des Winkels angle, der zur x-Achse berechnet wird, durchführt.
	 */
	private AffineTransform getAngleTransformation( 
			final Double translationFactor,
			final Double angle) {
		AffineTransform angleTransformation = new AffineTransform();
		if ( angle.isNaN()) {
			return angleTransformation;
		}
		angleTransformation.translate( - translationFactor * Math.sin( angle), translationFactor * Math.cos( angle));
		return angleTransformation;
	}
	
	/*
	 * Ein ShapeWithReferencePoint kapselt ein Paar bestehend aus einem Shape und einem
	 * Referenzpunkt.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 9139 $
	 *
	 */
	private static class ShapeWithReferencePoint {
		
		/**
		 * Konstruiert ein ShapeWithReferencePoint aus den übergebenen Informationen 
         */
        public ShapeWithReferencePoint(Shape shape, Point2D.Double referencePoint) {
	        super();
	        _shape = shape;
	        _referencePoint = referencePoint;
        }
		
        /**
         * Gibt das Shape zurück. 
         */
        public Shape getShape() {
        	return _shape;
        }
        
		/**
         * Gibt den ReferenzPunkt zurück. 
         */
        @SuppressWarnings("unused")
        public Point2D.Double getReferencePoint() {
        	return _referencePoint;
        }
        private Shape _shape;
		private Point2D.Double _referencePoint;
	}
	
	/*
	 * Liefert eine ShapeWithReferencePoint-Objekt für die übergebenen Daten zurück, das
	 * in paintDisplayObject zum Zeichnen verwendet wird.
	 */
	private ShapeWithReferencePoint getShape( Point2D point, PrimitiveForm primitiveForm) {
		final PrimitiveFormType type = primitiveForm.getType();
		final Point2D.Double translation = primitiveForm.getTranslation();
		if ( type == PrimitiveFormType.RECHTECK) {
			double transX = 0;
			double transY = 0;
			if ( translation != null) {
				transX = translation.getX();
				transY = -translation.getY();
			}
			final Double width = (Double) primitiveForm.getSpecificInformation( "width");
			final Double height = (Double) primitiveForm.getSpecificInformation( "height");
			Rectangle2D.Double rectangle = new Rectangle2D.Double(
					point.getX() + transX - width/2., 
					point.getY() + transY - height/2., 
					width, height);
			return new ShapeWithReferencePoint( rectangle, 
					new Point2D.Double( point.getX()+transX, point.getY()+transY));	// Provisorium! ???
		} else if ( type == PrimitiveFormType.KREIS) {
			double centerX = point.getX();
			double centerY = point.getY();
			if ( translation != null) {
				centerX += translation.getX();
				centerY -= translation.getY();
			}
			double radius  = (Double) primitiveForm.getSpecificInformation( "radius");
			Ellipse2D.Double circle = new Ellipse2D.Double(
					centerX - radius, centerY - radius, 2*radius, 2*radius);
			return new ShapeWithReferencePoint( circle, 
					new Point2D.Double( centerX, centerY));
		} else if ( type == PrimitiveFormType.HALBKREIS) {
			double centerX = point.getX();
			double centerY = point.getY();
			if ( translation != null) {
				centerX += translation.getX();
				centerY -= translation.getY();
			}
			double radius  = (Double) primitiveForm.getSpecificInformation( "radius");
			String orientation  = (String) primitiveForm.getSpecificInformation( "orientation");
			return new ShapeWithReferencePoint( 
					getSemiCircle( centerX, centerY, radius, orientation), 
					new Point2D.Double( centerX, centerY));
		}
		return null;
	}
	
	/*
	 * Konstruiert einen Halbkreis zu den Aufrufparametern.
	 */
	private Shape getSemiCircle( double centerX, double centerY, Double radius, String orientation) {
		int[] take = new int[] { 1, 1, 1, 0, 0, 1};
		Ellipse2D.Double circle = new Ellipse2D.Double( -radius, -radius, 2*radius, 2*radius);
		GeneralPath gPath = new GeneralPath();
		AffineTransform affineTransform =  new AffineTransform();
		affineTransform.translate(centerX, centerY);
		if ( orientation.equals( OBERER_HALBKREIS)) {
			affineTransform.rotate( Math.PI);
		} else if ( orientation.equals( RECHTER_HALBKREIS)) {
			affineTransform.rotate( 3*Math.PI/2);
		} else if ( orientation.equals( LINKER_HALBKREIS)) {
			affineTransform.rotate( Math.PI/2);
		}
		final PathIterator pathIterator = circle.getPathIterator( affineTransform);
		for ( int i = 0; i < 6; i++) {
			if ( take[i] == 1) {
				double[] coords = new double[6];
				final int currentSegment = pathIterator.currentSegment(coords);
				if ( currentSegment == PathIterator.SEG_MOVETO) {
					gPath.moveTo( coords[0], coords[1]);
				} else if ( currentSegment == PathIterator.SEG_CUBICTO) {
					gPath.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
				} else if ( currentSegment == PathIterator.SEG_CLOSE) {
					gPath.closePath();
				}
			}
			pathIterator.next();
		}
		return gPath;
	}

	/*
	 * Gibt den Durchmesser zurück.
	 */
	private Double determineDiameter( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		final Double diameter;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.DURCHMESSER)) {
			final Object valueOfStaticProperty = dotPoint.getValueOfStaticProperty( primitiveFormName, 
					DOTProperty.DURCHMESSER);
			if ( valueOfStaticProperty instanceof Integer) {
				final Integer diameterAsInt = (Integer) valueOfStaticProperty;
				diameter = diameterAsInt.doubleValue();
			} else if ( valueOfStaticProperty instanceof Double) {
				diameter =  (Double) valueOfStaticProperty;
			} else {
				diameter = 0.;
			}
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.DURCHMESSER);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				if ( propertyValue instanceof Integer) {
					final Integer diameterAsInt = (Integer) propertyValue;
					diameter = diameterAsInt.doubleValue();
				} else if ( propertyValue instanceof Double) {
					diameter = (Double) displayObjectTypeItem.getPropertyValue();
				} else {
					diameter = 0.;
				}
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				diameter = 0.;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				diameter = 0.;
			} else {
				diameter = 0.;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung kann hier unterschieden werden.
			
		}
		return diameter;
	}
	
	/*
	 * Gibt die Strichbreite zurück.
	 */
	private Double determineStrokeWidth( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		final Double strokeWidth;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.STRICHBREITE)) {
			strokeWidth =  (Double) dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.STRICHBREITE);
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.STRICHBREITE);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				strokeWidth = (Double) displayObjectTypeItem.getPropertyValue();
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				strokeWidth = 0.;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				strokeWidth = 0.;
			} else {
				strokeWidth = 0.;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung muss hier unterschieden werden.
		}
		return strokeWidth;
	}
	
	/*
	 * Gibt die Farbe zurück.
	 */
	private Color determineFillColor( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		final Color color;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.FUELLUNG)) {
			final Object valueOfStaticProperty = dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.FUELLUNG);
			if ( valueOfStaticProperty instanceof Color) {
				color = (Color) valueOfStaticProperty;
			} else {
				color = ColorManager.getInstance().getColor((String) valueOfStaticProperty);
			}
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.FUELLUNG);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
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
		return color;
	}
	
	/*
	 * Gibt die Tranzparenz zurück.
	 */
	private Integer determineTransparency( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		Integer transparency;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.TRANSPARENZ)) {
			transparency = (Integer) dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.TRANSPARENZ);
			if ( transparency <= 100) { // skaliere von % auf 0...255
				transparency = 255 - (transparency * 255/100);
			}
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.TRANSPARENZ);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				transparency = (Integer) displayObjectTypeItem.getPropertyValue();
				if ( transparency <= 100) { // skaliere von % auf 0...255
					transparency = 255 - (transparency * 255/100);
				}
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				transparency = 0;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				transparency = 0;
			} else {
				transparency = 0;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung kann hier unterschieden werden.
		}
		return transparency;
	}
	
	/*
	 * Gibt den Textstil zurück.
	 */
	private int determineTextStyle( DisplayObject displayObject,
			DOTPoint dotPoint, String primitiveFormName) {
		final Integer style;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.TEXTSTIL)) {
			style = (Integer) dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.TEXTSTIL);
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.TEXTSTIL);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return Font.PLAIN;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				style = (Integer) propertyValue;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				style = Font.PLAIN;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				style = Font.PLAIN;
			} else {
				style = Font.PLAIN;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung muss hier unterschieden werden.
		}
		return style;
	}
	
	/*
	 * Gibt die Textgröße zurück.
	 */
	private int determineTextSize( DisplayObject displayObject,
			DOTPoint dotPoint, String primitiveFormName) {
		final Integer size;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.GROESSE)) {
			final Object valueOfStaticProperty = dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.GROESSE);
			if ( valueOfStaticProperty instanceof Double) {
				final Double d = (Double) valueOfStaticProperty;
				size = d.intValue();
			} else {
				size = (Integer) valueOfStaticProperty;
			}
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.GROESSE);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return 0;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				size = (Integer) displayObjectTypeItem.getPropertyValue();
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				size = 0;
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				size = 0;
			} else {
				size = 0;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung kann hier unterschieden werden.
		}
		return size;
	}
	
	/*
	 * Gibt die Farbe zurück.
	 */
	private Color determineColor( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		final Color color;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.FARBE)) {
			final Object valueOfStaticProperty = dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.FARBE);
			if ( valueOfStaticProperty instanceof Color) {
				color = (Color) valueOfStaticProperty;
			} else {
				color = ColorManager.getInstance().getColor((String) valueOfStaticProperty);
			}
		} else {
			final PrimitiveFormPropertyPair primitiveFormPropertyPair = 
				new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.FARBE);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
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
		return color;
	}
	
	/*
	 * Gibt den Text zurück.
	 */
	private String determineText( DisplayObject displayObject, 
			DOTPoint dotPoint, String primitiveFormName) {
		// Text hat eine Besonderheit: im Fall einer statischen Eigenschaft kommen die 'vordefinierten
		// Funktionen' und ein konstanter, vom Benutzer definierter Text in Frage, während im Fall
		// einer dynamischen Eigenschaft auch das angemeldete Attribut dargestellt werden kann.
		final String text;
		final PrimitiveFormPropertyPair primitiveFormPropertyPair;
		if ( dotPoint.isPropertyStatic( primitiveFormName, DOTProperty.TEXT)) {
			text = (String) dotPoint.getValueOfStaticProperty( primitiveFormName, DOTProperty.TEXT);
			primitiveFormPropertyPair = null;
			if ( text.equals( DYNAMIC_ATTRIBUTE_UNSCALED) ||
					text.equals( DYNAMIC_ATTRIBUTE_SCALED)) {	// das wäre ein Fehler
				return "";
			}
		} else {
			primitiveFormPropertyPair = new PrimitiveFormPropertyPair( primitiveFormName, DOTProperty.TEXT);
			final DisplayObjectTypeItem displayObjectTypeItem = displayObject.getDisplayObjectTypeItem( primitiveFormPropertyPair);
			if ( displayObjectTypeItem == null) {
				return null;
			}
			final Object propertyValue = displayObjectTypeItem.getPropertyValue();
			if ( propertyValue != null) {
				text = (String) displayObjectTypeItem.getPropertyValue();
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_DATA_ITEM) {
				return "Keine Daten";
			} else if ( displayObjectTypeItem == DynamicDOTItem.NO_SOURCE_ITEM) {
				return "Keine Quelle";
			} else {
				return null;
			}
			// Die letzten drei Fälle werden noch gleichbehandelt, aber bei einer kommenden
			// Erweiterung kann hier unterschieden werden.
		}
		if ( text.equals( DOTPointPainter.DYNAMIC_ATTRIBUTE_UNSCALED)) {
			final Data value = displayObject.getValue( primitiveFormPropertyPair);
			return value.asUnscaledValue().getValueText();
		} else if ( text.equals( DOTPointPainter.DYNAMIC_ATTRIBUTE_SCALED)) {
			final Data value = displayObject.getValue( primitiveFormPropertyPair);
			final String name = value.asScaledValue().getText();
			if ( name.length() != 0) {
				return name;
			} else {
				final Double doubleValue = value.asUnscaledValue().doubleValue();
				Integer intValue = doubleValue.intValue();
				if ( doubleValue.equals( intValue.doubleValue())) {
					return intValue.toString();
				} else {
					return doubleValue.toString();
				}
			}
		} else if ( text.equals( DOTPointPainter.GET_NAME)) {
			return displayObject.getSystemObject().getName();
		} else if ( text.equals( DOTPointPainter.GET_NAME_OR_PID_OR_ID)) {
			return displayObject.getSystemObject().getNameOrPidOrId();
		} else if ( text.equals( DOTPointPainter.GET_PID_OR_ID)) {
			return displayObject.getSystemObject().getPidOrId();
		} else if ( text.equals( DOTPointPainter.GET_INFO_GET_DESCRIPTION)) {
			return displayObject.getSystemObject().getInfo().getDescription();
		}
		return text;	// Greift im Fall eines konstanten benutzer-definierten Textes.
	}

	/*
	 * Gibt zu dem Darstellungsobjekt und dem Typ das umschließende achsen-parallele Rechteck
	 * zurück.
	 */
	public Rectangle getBoundingRectangle(DisplayObject displayObject, int type) {
		// Provisorische Implementation
		Rectangle rectangle = null;
		for ( Object o : displayObject.getCoordinates(type)) {
			if ( o instanceof DisplayObjectManager.PointWithAngle) {
				DisplayObjectManager.PointWithAngle pwa = (DisplayObjectManager.PointWithAngle) o;
				rectangle = new Rectangle ( (int)(pwa.getPoint().getX()-250), (int) (pwa.getPoint().getY()-150), 500, 300);
			} else if ( o instanceof Point2D.Double) {
				Point2D.Double p = (Point2D.Double) o;
				rectangle = new Rectangle ( (int)(p.getX()-250), (int) (p.getY()-150), 500, 300);
			}
		}
		return rectangle;
		
		// Anfang einer optimierten Implementation; optional besteht auch die Möglichkeit, den
		// Kode von paintDisplayObject() dahingehend zu gebrauchen, dass man aus all den dortigen
		// Shapes das umfassende Rechteck bereits berechnet, dem DisplayObject es verrät, und es
		// so wieder zurückbekommt.
		/*
		Rectangle primitiveFormsRect = null;
		for ( DisplayObjectType displayObjectType : displayObject.getDOTCollection().values()) {
			DOTPoint dotPoint = (DOTPoint) displayObjectType;
			final Double translationFactor = dotPoint.getTranslationFactor();
			for ( PrimitiveForm primitiveForm : dotPoint.getPrimitiveForms()) {
				// Hier muss die lokale Translation berechnet werden, die sich aus Translation,
				// TranslationsFaktor und gegebenfalls dem Winkel ergibt!
				final Point2D.Double translation = primitiveForm.getTranslation();
				final PrimitiveFormType pfType = primitiveForm.getType();
				if ( pfType == PrimitiveFormType.PUNKT) {
					if ( primitiveFormsRect == null) {
						primitiveFormsRect = new Rectangle();
					} else {
						Point p = new Point(0,0);
						primitiveFormsRect.add( p);
					}
				} else if ( pfType == PrimitiveFormType.RECHTECK) {
					// ToDo 
				}
				// Weitere Fälle ToDo
			}
		}*/
    }

	/**
	 * Gibt zu dem Darstellungsobjekt und dem Typ die Koordinaten zurück.
	 */
	public List<Object> getCoordinates(List<Object> coordinates, int type) {
		// Bisher gibt es beim Punkt-Plugin keine Typ-Unterscheidung.
		return coordinates;
    }
	
	public static final String OBERER_HALBKREIS = "Oberer Halbkreis";
	public static final String RECHTER_HALBKREIS = "Rechter Halbkreis";
	public static final String UNTERER_HALBKREIS = "Unterer Halbkreis";
	public static final String LINKER_HALBKREIS = "Linker Halbkreis";
	
	public static final String DYNAMIC_ATTRIBUTE_UNSCALED = "Vordefinierte Funktion: Attribut unskaliert anzeigen";
	public static final String DYNAMIC_ATTRIBUTE_SCALED = "Vordefinierte Funktion: Attribut skaliert anzeigen";
	public static final String GET_NAME = "Vordefinierte Funktion: Name";
	public static final String GET_NAME_OR_PID_OR_ID = "Vordefinierte Funktion: Name, PID oder Id";
	public static final String GET_PID_OR_ID = "Vordefinierte Funktion: PID oder Id";
	public static final String GET_INFO_GET_DESCRIPTION = "Vordefinierte Funktion: Beschreibung";
	
	final public static String[] STATIC_TEXT_ITEMS = { DOTPointPainter.GET_NAME_OR_PID_OR_ID,
		DOTPointPainter.GET_NAME, DOTPointPainter.GET_PID_OR_ID, DOTPointPainter.GET_INFO_GET_DESCRIPTION,
		"Ein veränderbarer Text"};
	
	final public static String[] DYNAMIC_TEXT_ITEMS = { 
		DYNAMIC_ATTRIBUTE_UNSCALED, DYNAMIC_ATTRIBUTE_SCALED, GET_NAME_OR_PID_OR_ID,
		GET_NAME, GET_PID_OR_ID, GET_INFO_GET_DESCRIPTION, "Ein veränderbarer Text"};
}
