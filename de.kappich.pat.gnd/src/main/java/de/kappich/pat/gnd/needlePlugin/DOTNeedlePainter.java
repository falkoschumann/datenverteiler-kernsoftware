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

package de.kappich.pat.gnd.needlePlugin;

import de.kappich.pat.gnd.displayObjectToolkit.DisplayObject;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObjectManager;
import de.kappich.pat.gnd.gnd.MapPane;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Der Painter für Linienobjekte.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9288 $
 */
@SuppressWarnings("serial")
public class DOTNeedlePainter extends JPanel implements DisplayObjectPainter {

	public void paintDisplayObject(final MapPane mapPane, final Graphics2D g2D, final DisplayObject displayObject, final boolean selected) {
		if(!mapPane.getGnd().getNoticeManager().hasNotice(displayObject.getSystemObject())) return;
		if(displayObject.getCoordinates().size() == 0) return;
		final Collection<Point2D> drawPoint = getPointLocations(mapPane, displayObject);
		if(drawPoint == null) return;
		for(final Point2D point2D : drawPoint) {
			drawPoint(g2D, point2D, mapPane, displayObject, selected);
		}
	}

	private Collection<Point2D> getPointLocations(final MapPane mapPane, final DisplayObject displayObject) {
		final Collection<Point2D> result = new ArrayList<Point2D>();
		for(final Object o : displayObject.getCoordinates()) {
			result.addAll(getPointLocations(mapPane, o));
		}
		return result;
	}

	private Collection<Point2D> getPointLocations(final MapPane mapPane, final Object o) {
		if(o instanceof DisplayObjectManager.PointWithAngle) {
			final DisplayObjectManager.PointWithAngle pointWithAngle = (DisplayObjectManager.PointWithAngle)o;
			final Point2D drawPoint = pointWithAngle.getPoint();
			return Arrays.asList(drawPoint);
		}
		if(o instanceof Path2D) {
			final Path2D path2D = (Path2D)o;

			final List<Path2D.Double> paths = cutPathToRectangle(path2D, mapPane.getUTMBounds());

			final List<Point2D> result = new ArrayList<Point2D>();
			for(final Path2D.Double path : paths) {
				result.add(getPathMidPoint(path));
			}
			return result;
		}
		return Collections.emptyList();
	}

	private Point2D.Double getPathMidPoint(final Path2D.Double path) {
		final double length = getPathLength(path);

		final PathIterator pathIterator = path.getPathIterator(null);
		final double[] coords = new double[6];

		double currentLength = 0;

		pathIterator.currentSegment(coords);
		Point2D.Double point0 = new Point2D.Double(coords[0], coords[1]);
		pathIterator.next();

		Point2D.Double point1 = null;
		while(!pathIterator.isDone()) {
			pathIterator.currentSegment(coords);
			pathIterator.next();
			point1 = new Point2D.Double(coords[0], coords[1]);

			final double dx = point0.getX() - point1.getX();
			final double dy = point0.getY() - point1.getY();

			currentLength += Math.sqrt(dx * dx + dy * dy);

			if(currentLength > length / 2) break;

			point0 = point1;
		}

		final double delta = currentLength - length / 2;

		if(point1 == null) return point0;

		double dx = point0.getX() - point1.getX();
		double dy = point0.getY() - point1.getY();

		final double l = Math.sqrt(dx * dx + dy * dy);

		dx /= l;
		dy /= l;

		return new Point2D.Double(point1.getX() + delta * dx, point1.getY() + delta * dy);
	}

	private double getPathLength(final Path2D.Double path) {
		final PathIterator pathIterator = path.getPathIterator(null);
		final double[] coords = new double[6];

		double length = 0;

		pathIterator.currentSegment(coords);
		Point2D.Double point0 = new Point2D.Double(coords[0], coords[1]);
		pathIterator.next();

		while(!pathIterator.isDone()) {
			pathIterator.currentSegment(coords);
			pathIterator.next();
			final Point2D.Double point1 = new Point2D.Double(coords[0], coords[1]);

			final double dx = point0.getX() - point1.getX();
			final double dy = point0.getY() - point1.getY();

			length += Math.sqrt(dx * dx + dy * dy);

			point0 = point1;
		}
		return length;
	}

	private List<Path2D.Double> cutPathToRectangle(final Path2D path2D, final Rectangle bounds) {
//		bounds.grow(-1000, -1000);
		final List<Path2D.Double> result = new ArrayList<Path2D.Double>();
		final double[] coords = new double[6];

		final PathIterator pathIterator = path2D.getPathIterator(null);
		pathIterator.currentSegment(coords);
		Point2D.Double point0 = new Point2D.Double(coords[0], coords[1]);
		pathIterator.next();

		Path2D.Double outPath = null;
		if(bounds.contains(point0)) {
			outPath = new Path2D.Double();
			outPath.moveTo(point0.getX(), point0.getY());
		}
		while(!pathIterator.isDone()) {
			pathIterator.currentSegment(coords);
			pathIterator.next();
			final Point2D.Double point1 = new Point2D.Double(coords[0], coords[1]);

			if(bounds.contains(point1)) {
				// neuer Punkt liegt innerhalb des rechtecks
				if(outPath != null) {
					// Der alte auch -> Linie verlängern
					outPath.lineTo(point1.getX(), point1.getY());
				}
				else {
					// Der alte nicht, neue Linie beginnen
					outPath = new Path2D.Double();
					final Point2D.Double intersectionPoint = getIntersectionPoint(point1, point0, bounds);
					outPath.moveTo(intersectionPoint.getX(), intersectionPoint.getY());
					outPath.lineTo(point1.getX(), point1.getY());
				}
			}
			else {
				// neuer Punkt liegt außerhalb des rechtecks
				if(outPath != null) {
					// Der alte liegt innerhalb, Linie beenden
					final Point2D.Double intersectionPoint = getIntersectionPoint(point0, point1, bounds);
					outPath.lineTo(intersectionPoint.getX(), intersectionPoint.getY());
					result.add(outPath);
					outPath = null;
				}
				else {
					final List<Point2D.Double> points = getIntersectionPoints(bounds, point0, point1);
					if(points.size() == 2) {
						outPath = new Path2D.Double();
						outPath.moveTo(points.get(0).getX(), points.get(0).getY());
						outPath.lineTo(points.get(1).getX(), points.get(1).getY());
						result.add(outPath);
						outPath = null;
					}
				}
			}

			point0 = point1;
		}

		if(outPath != null) {
			result.add(outPath);
		}

		return result;
	}

	private List<Point2D.Double> getIntersectionPoints(final Rectangle bounds, final Point2D.Double point0, final Point2D.Double point1) {
		double t;
		double s;
		final List<Point2D.Double> points = new ArrayList<Point2D.Double>();

		t = t(point0, point1, new Point2D.Double(bounds.getX(), bounds.getY()), new Point2D.Double(bounds.getX(), bounds.getMaxY()));
		s = s(point0, point1, new Point2D.Double(bounds.getX(), bounds.getY()), new Point2D.Double(bounds.getX(), bounds.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			points.add(new Point2D.Double(bounds.getX(), bounds.getY() + bounds.getHeight() * t));
		}
		t = t(point0, point1, new Point2D.Double(bounds.getMaxX(), bounds.getY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
		s = s(point0, point1, new Point2D.Double(bounds.getMaxX(), bounds.getY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			points.add(new Point2D.Double(bounds.getMaxX(), bounds.getY() + bounds.getHeight() * t));
		}
		t = t(point0, point1, new Point2D.Double(bounds.getX(), bounds.getY()), new Point2D.Double(bounds.getMaxX(), bounds.getY()));
		s = s(point0, point1, new Point2D.Double(bounds.getX(), bounds.getY()), new Point2D.Double(bounds.getMaxX(), bounds.getY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			points.add(new Point2D.Double(bounds.getX() + bounds.getWidth() * t, bounds.getY()));
		}
		t = t(point0, point1, new Point2D.Double(bounds.getX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
		s = s(point0, point1, new Point2D.Double(bounds.getX(), bounds.getMaxY()), new Point2D.Double(bounds.getMaxX(), bounds.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			points.add(new Point2D.Double(bounds.getX() + bounds.getWidth() * t, bounds.getMaxY()));
		}
		return points;
	}

	private Point2D.Double getIntersectionPoint(final Point2D.Double pointInRect, final Point2D.Double pointOutsideRect, final Rectangle rect) {
		double t;
		double s;
		t = t(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getY()), new Point2D.Double(rect.getX(), rect.getMaxY()));
		s = s(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getY()), new Point2D.Double(rect.getX(), rect.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			return new Point2D.Double(rect.getX(), rect.getY() + rect.getHeight() * t);
		}
		t = t(pointInRect, pointOutsideRect, new Point2D.Double(rect.getMaxX(), rect.getY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
		s = s(pointInRect, pointOutsideRect, new Point2D.Double(rect.getMaxX(), rect.getY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			return new Point2D.Double(rect.getMaxX(), rect.getY() + rect.getHeight() * t);
		}
		t = t(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getY()), new Point2D.Double(rect.getMaxX(), rect.getY()));
		s = s(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getY()), new Point2D.Double(rect.getMaxX(), rect.getY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			return new Point2D.Double(rect.getX() + rect.getWidth() * t, rect.getY());
		}
		t = t(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getMaxY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
		s = s(pointInRect, pointOutsideRect, new Point2D.Double(rect.getX(), rect.getMaxY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
		if(t >= 0 && t <= 1 && s >= 0 && s <= 1) {
			return new Point2D.Double(rect.getX() + rect.getWidth() * t, rect.getMaxY());
		}
		return null;
	}

	private double t(final Point2D.Double p1, final Point2D.Double p2, final Point2D.Double p3, final Point2D.Double p4) {
		return (p1.x * p2.y + p2.x * p3.y + p3.x * p1.y - p2.x * p1.y - p3.x * p2.y - p1.x * p3.y) / n(p1, p2, p3, p4);
	}

	private double s(final Point2D.Double p1, final Point2D.Double p2, final Point2D.Double p3, final Point2D.Double p4) {
		return (p1.x * p4.y + p4.x * p3.y + p3.x * p1.y - p4.x * p1.y - p3.x * p4.y - p1.x * p3.y) / n(p1, p2, p3, p4);
	}

	private double n(final Point2D.Double p1, final Point2D.Double p2, final Point2D.Double p3, final Point2D.Double p4) {
		return p1.x * p4.y + p2.x * p3.y + p3.x * p1.y + p4.x * p2.y - p4.x * p1.y - p3.x * p2.y - p2.x * p4.y - p1.x * p3.y;
	}

	private Point2D.Double nearestPointOnLine(final Point2D point0, final Point2D point1, final Point2D centerPoint) {
		// Siehe http://paulbourke.net/geometry/pointline/
		final double dx = point1.getX() - point0.getX();
		final double dy = point1.getY() - point0.getY();
		double u = ((centerPoint.getX() - point0.getX()) * dx + (centerPoint.getY() - point0.getY()) * dy) / (point1.distanceSq(point0));
		u = Math.max(u, 0);
		u = Math.min(u, 1);
		
		return new Point2D.Double(point0.getX() + u * dx, point0.getY() + u * dy);
	}

	private void drawPoint(final Graphics2D g2D, final Point2D point, final MapPane mapPane, final DisplayObject displayObject, final boolean selected) {
		final double xOffset = (displayObject.hashCode() % 256 - 127) / 30;
		final double yOffset = ((displayObject.hashCode() / 256) % 256 - 127) / 30;
		final AffineTransform oldTransform = g2D.getTransform();
		g2D.translate(point.getX(), point.getY());
		final double scale = getScale(mapPane);
		g2D.scale(scale, scale);
		final Point2D.Double nullPoint = new Point2D.Double(0, 0);
		g2D.setStroke(new BasicStroke(0.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		final Point2D.Double p = new Point2D.Double(9, -6);
		g2D.setPaint(new GradientPaint(p, new Color(0, 0, 0, 0.0f), nullPoint, new Color(0, 0, 0, 0.2f)));
		g2D.draw(new Line2D.Double(p, nullPoint));
		g2D.setColor(Color.gray);
		g2D.draw(new Line2D.Double(xOffset, -27.5 + yOffset, 0, 0));
		if(selected || mapPane.getSelectedSystemObjects().contains(displayObject.getSystemObject())) {
			g2D.setColor(Color.red.darker());
		}
		else {
			g2D.setColor(Color.green.darker());
		}
		g2D.fill(new Ellipse2D.Double(-2.5 + xOffset, -30 + yOffset, 5, 5));
		g2D.setColor(new Color(1, 1, 1, 0.3f));
		g2D.fill(new Ellipse2D.Double(-2.1 + xOffset, -29.6 + yOffset, 2.5, 2.5));

		g2D.setTransform(oldTransform);
	}

	private double getScale(final MapPane mapPane) {
		double scale = mapPane.getMapScale() / 4000;
		if(scale < 1) scale = (scale + 1) / 2;
		return scale;
	}

	public List<Object> getCoordinates(final List<Object> coordinates, final int type) {
		return coordinates;
	}

	public Rectangle getBoundingRectangle(final DisplayObject displayObject, final int type) {
		if(!displayObject.getMapPane().getGnd().getNoticeManager().hasNotice(displayObject.getSystemObject())) return null;

		if(displayObject.getCoordinates().size() == 0) return null;
		final Collection<Point2D> p = getPointLocations(displayObject.getMapPane(), displayObject);
		if(p.size() == 0) return null;
		final Rectangle rectangle = new Rectangle(new Point((int)p.iterator().next().getX(), (int)p.iterator().next().getY()));
		for(final Point2D point2D : p) {
			rectangle.add(point2D);
		}
		final double scale = getScale(displayObject.getMapPane());
		rectangle.grow((int)(10 * scale), (int)(40 * scale));
		return rectangle;
	}
}
