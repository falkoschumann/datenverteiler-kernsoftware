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
package de.kappich.pat.gnd.gnd;

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.kappich.pat.gnd.displayObjectToolkit.*;
import de.kappich.pat.gnd.displayObjectToolkit.DisplayObjectManager.PointWithAngle;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.viewManagement.View;
import de.kappich.pat.gnd.viewManagement.ViewEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Die Kartenansicht der Kartendarstellung.
 * <p>
 * Ein MapPane steht für die Kartenansicht der GND. Um die einzelnen Layer darzustellen, ist
 * MapPane von JLayeredPane abgeleitet. Jeder nicht-leere Layer des JLayeredPane enthält genau
 * eine Komponente der Klasse {@link MapPane.LayerPanel}, das die Objekte eines GND-Layers
 * darstellt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9288 $
 *
 */
@SuppressWarnings("serial")
public class MapPane extends JLayeredPane implements View.ViewChangeListener, Printable, GenericNetDisplay.ResolutionListener, DOTManager.DOTChangeListener {

	final private GenericNetDisplay _gnd;
	final private View _view;

	final private DisplayObjectManager _displayObjectManager;
	final private Set<DisplayObject> _selectedDisplayObjects = new HashSet<DisplayObject>();

	final private List<MapScaleListener> _mapScaleListeners = new CopyOnWriteArrayList<MapScaleListener>();

	private AffineTransform _mapTransform = null;
	private Double _mapScale = 0.;

	private double _zoomTranslateX;
	private double _zoomTranslateY;
	private double _zoomScale;

	private boolean _antialising;

	private boolean _isTooltipOn;

	private boolean _showNothing;

	final public static String _newline = System.getProperty("line.separator");

	/**
	 * Konstruiert eine neue Kartenansicht für das übergebene GenericNetDisplay mit
	 * der übergebenen Ansicht. Das Objekt wird zunächst aber nur konstruiert, die
	 * eigentliche Initialisierung muss mit {@link #init} noch ausgeführt werden.
	 *
	 * @param gnd die Netzdarstellung
	 * @param view die aktuelle Ansicht
	 */

	public MapPane(GenericNetDisplay gnd, View view) {
		super();
		_gnd = gnd;
		_view = view;
		_displayObjectManager = new DisplayObjectManager( _gnd.getConnection(), this);
	}

	/**
	 * Der Konstruktor dient der Klasses GenericNetDisplay dazu, das MapPane schon anordnen
	 * zu können. In der folgenden init-Methode und ihren Initialisierungen wird JComponent.getBounds()
	 * aufgerufen, was erst sinnvoll ist, wenn das MapPane schon in im GenericNetDisplay mit
	 * pack() gepackt wurde.
	 */
	public void init() {
		setMinimumSize( new Dimension(300, 300));
		initTheLayerPanels();
		_view.addChangeListener(this);
		addListeners();
		_zoomTranslateX = 0;
		_zoomTranslateY = 0;
		_zoomScale = 1.;

		setDoubleBuffered( _gnd.isDoubleBuffered());
		setAntialising( _gnd.isAntiAliasingOn());

		_showNothing = false;

		ToolTipManager.sharedInstance().setInitialDelay( 200);
		ToolTipManager.sharedInstance().registerComponent( this);	// Registrieren ist notwendig, unregister bewirkt nichts, solange getTooltiptext einen nicht-leeren String zurückliefert.
		setTooltip( _gnd.isMapsTooltipOn());

		// Vor der Anmeldung sollte man mal den Maßstab berechnen, wozu auch eine Initialisierung der AT gehört.
		initAffineMapTransform();
		determineCurrentScale();

		new Thread(){
			@Override
			public void run() {
				_displayObjectManager.subscribeDisplayObjects();
			}
		}.start();
		_displayObjectManager.addMapScaleListeners();
		_gnd.addResolutionListener( this);
		DOTManager.getInstance().addDOTChangeListener( this);
	}

	public Point2D getCenterPoint() {
		final Point2D.Double input = new Point2D.Double(getWidth() / 2, getHeight() / 2);
		final AffineTransform affineTransform = new AffineTransform();
		modifyAffineTransform(affineTransform);
		try {
			return affineTransform.createInverse().transform(input, new Point2D.Double());
		}
		catch(NoninvertibleTransformException ignored) {
			return new Point2D.Double();
		}
	}

	public GenericNetDisplay getGnd() {
		return _gnd;
	}

	public void redraw() {
		repaint();
		visibleObjectsChanged();
	}

	private class LayerPanel extends JPanel {

		private MapPane _mapPane;
		private Map<SystemObject, DisplayObject>		_displayObjects;

		LayerPanel ( MapPane mapPane, List<DisplayObject> displayObjects) {
			_mapPane = mapPane;
			_displayObjects = new HashMap<SystemObject, DisplayObject>( displayObjects.size());
			for ( DisplayObject displayObject : displayObjects) {
				_displayObjects.put( displayObject.getSystemObject(), displayObject);
			}
		}

		/**
		 * Gibt die DisplayObjects des LayerPanels zurück.
		 *
		 * @return die DisplayObjects
		 */
		public Collection<DisplayObject> getDisplayObjects() {
			return _displayObjects.values();
		}

		private boolean intersect( Rectangle rectangle, List<Object> coordinates) {
			for ( Object object : coordinates) {
				if ( object instanceof Path2D.Double) {
					Path2D.Double polyline = (Path2D.Double) object;
					final PathIterator pathIterator = polyline.getPathIterator(null);
					double[] coors = new double[6];
					double oldX = Double.MAX_VALUE;
					double oldY = Double.MAX_VALUE;
					while (pathIterator.isDone() == false) {
						int type = pathIterator.currentSegment(coors);
						switch (type) {
							case PathIterator.SEG_MOVETO:
								oldX = coors[0];
								oldY = coors[1];
								break;
							case PathIterator.SEG_LINETO:
								if ( oldX != Double.MAX_VALUE) {
									Line2D.Double line = new Line2D.Double(oldX, oldY, coors[0], coors[1]);
									if (line.intersects( rectangle)) {
										return true;
									}
								}
								oldX = coors[0];
								oldY = coors[1];
								break;
							default:
								break;
						}
						pathIterator.next();
					}
				}
			}
			return false;
		}

		/**
		 * Erzeugt eine Liste von DisplayObjects, die in der Nähe des Punktes p liegen.
		 * Mit preferredUpperSize kann man eine angestrebte obere Grenze für die Anzahl
		 * der Objekte angeben. Ist die Anzahl der Objekte zunächst zu groß, so wird
		 * versucht eine kleinere Liste zu erzeugen, bis die angestrebte obere Grenze
		 * unterschritten wird. Gelingt aber keine Verkleinerung der Liste, so erhält
		 * man die größere Liste.
		 *
		 * @param p der Punkt
		 * @param preferredUpperSize die angestrebte obere Grenze der Rückgabeliste
		 * @return eine Liste mit DisplayObjects
		 */
		public List<DisplayObject> getDisplayObjectsCloseToPoint( Point p, int preferredUpperSize) {
			double factor = 30.;
			List<DisplayObject> returnList = getDisplayObjectsCloseToPoint( p, factor);
			int size = returnList.size();
			factor *= 0.65;
			while ( size >= preferredUpperSize) {
				returnList = getDisplayObjectsCloseToPoint( p, factor);
				if ( size == returnList.size()) {
					break;
				}
				factor *= 0.65;
				size = returnList.size();
			}
			return returnList;
		}

		private List<DisplayObject> getDisplayObjectsCloseToPoint( Point p, double factor) {
			List<DisplayObject> returnList = new ArrayList<DisplayObject> ();
			double mpp = factor * meterProPixel();
			Rectangle rectangle = new Rectangle( (int) (p.getX()-mpp/2.),
					(int) (p.getY()-mpp/2.), (int) mpp, (int)mpp);
			for ( DisplayObject displayObject: _displayObjects.values()) {
				final List<Object> coordinates = displayObject.getCoordinates();
				if ( !coordinates.isEmpty()) {
					Object firstCoordinate = coordinates.get(0);
					if ( (firstCoordinate instanceof Path2D.Double) && intersect( rectangle, coordinates)) {
						returnList.add( displayObject);
					}  else if ( firstCoordinate instanceof PointWithAngle) {
						PointWithAngle pwa = (PointWithAngle) firstCoordinate;
						if ( rectangle.contains( pwa.getPoint())) {
							returnList.add( displayObject);
						}
					}
				}
			}
			return returnList;
		}

		/**
		 * Malt einen LayerPanel.
		 *
		 * @param g das Graphics-Objekt
		 */
		public void paintLayer(Graphics g) {
			final Graphics2D g2D = (Graphics2D)g;
			if ( isAntialising()) {
				g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			} else {
				g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			}
//			g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//			g2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
//			g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

			Rectangle filterRectangle = getUTMBounds();

			for(DisplayObject displayObject : _displayObjects.values()) {
				if(displayObject.getBoundingRectangle() == null || displayObject.getBoundingRectangle().intersects(filterRectangle)) {
					if(_selectedDisplayObjects.contains(displayObject)) {
						displayObject.getPainter().paintDisplayObject(MapPane.this, g2D, displayObject, true);
					}
					else {
						displayObject.getPainter().paintDisplayObject(MapPane.this, g2D, displayObject, false);
					}
//					Drawing bounding box
//					g2D.setColor(Color.red);
//					if(displayObject.getBoundingRectangle() != null) {
//						g2D.draw(displayObject.getBoundingRectangle());
//					}
				}
			}
		}
	}

	public Rectangle getUTMBounds() {
		final Rectangle bounds = getBounds();
		Point p = new Point(0, 0);
		Point utmPoint = new Point();
		getUTMPoint(p, utmPoint);
		Rectangle filterRectangle = new Rectangle(utmPoint);
		p = new Point((int)bounds.getWidth(), (int)bounds.getHeight());
		getUTMPoint(p, utmPoint);
		filterRectangle.add(utmPoint);
		return filterRectangle;
	}

	private void initALayerPanel(ViewEntry entry, int i, final JProgressBar progressBar) {
		LayerPanel layerPanel = new LayerPanel(this, _displayObjectManager.getDisplayObjects(entry, progressBar));
		setLayer( layerPanel, i);	// setLayer before add according to documentation
		add( layerPanel);
		entry.setComponent(layerPanel);
	}

	private void initTheLayerPanels() {
		final JDialog progressDialog = new JDialog();
		progressDialog.setTitle("Die GND wird initialisiert");
		progressDialog.setLayout( new BorderLayout());
		final JLabel textLabel = new JLabel("Die Layer werden initialisiert.");
		textLabel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
		progressDialog.add( textLabel, BorderLayout.NORTH);
		final JLabel counterLabel = new JLabel();
		counterLabel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
		progressDialog.add( counterLabel, BorderLayout.WEST);
		final JProgressBar progressBar = new JProgressBar();
		progressBar.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
		progressBar.setIndeterminate( true);
		progressDialog.add( progressBar, BorderLayout.CENTER);
		final JButton cancelButton = new JButton("Abbrechen");
		ActionListener cancelButtonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				progressDialog.dispose();
				if ( _gnd.isStandAlone()) {
					System.exit(0);
				} else {
					throw new GNDPlugin.StopInitializationException();
				}
			}
		};
		cancelButton.addActionListener( cancelButtonListener);
		final JPanel cancelPanel = new JPanel();
		cancelPanel.add( cancelButton);
		progressDialog.add(cancelPanel, BorderLayout.SOUTH);
		progressDialog.setPreferredSize( new Dimension(300, 150));
		progressDialog.pack();
		progressDialog.setLocation( _gnd.getLocation());
		progressDialog.setVisible( true);
		int counter = 0;
		final List<ViewEntry> viewEntries = _view.getViewEntries(true);
		int n = viewEntries.size();
		for(ViewEntry entry : viewEntries) {
			counter++;
			// Hiermit wird bei 4 Layern und 4 Notizlayern zweimal von 1 bis 4 gezählt. Das ist vermutlich besser als von 1 bis 8, weil
			// der Benutzer sich wundern könnte, dass er gar nicht 8 Layer definiert hat.
			if(counter > viewEntries.size() / 2) {
				counter = 1;
				// Warnung: Diese Ausgabe ist nicht hundertprozentig korrekt, weil die Notizen eigentlich zuerst initialisiert werden,
				// es macht aber für den Benutzer keinen Unterschied!
				textLabel.setText("Die Notizen werden initialisiert.");
			}
			counterLabel.setText("Layer " + counter + " von " + viewEntries.size() / 2);
			progressDialog.pack();
			initALayerPanel(entry, --n, progressBar);
		}
		progressDialog.dispose();
	}

	private void initAffineMapTransform() {
		final List<SystemObject> systemObjects = _gnd.getSystemObjects();
		final Rectangle displayRectangle = _displayObjectManager.getDisplayRectangle( systemObjects);
		if (displayRectangle == null) {
			_showNothing = true;
			return;
		}

		final Rectangle bounds = getBounds();

		_mapTransform = new AffineTransform();
		double scaleX = bounds.getWidth() / ( displayRectangle.getMaxX()-displayRectangle.getMinX());
		double scaleY = bounds.getHeight() / ( displayRectangle.getMaxY()-displayRectangle.getMinY());
		if ( scaleX < scaleY) {
			_mapTransform.scale(scaleX, scaleX);
		} else {
			_mapTransform.scale(scaleY, scaleY);
		}
		_mapTransform.translate(-displayRectangle.getMinX(), -displayRectangle.getMinY());
	}

	/*
	 * Gehört zur Implementation des View.ChangeListeners.
	 */
	public void viewEntriesSwitched(View view, int i, int j) {
		if (i == j) {
			return;
		}
		int h = highestLayer();
		Component[] iComponents = getComponentsInLayer(h-i);
		Component[] jComponents = getComponentsInLayer(h-j);
		for ( Component component : iComponents) {
			setLayer(component, h-j);
		}
		for ( Component component : jComponents) {
			setLayer(component, h-i);
		}
		visibleObjectsChanged();
		repaint();
	}

	private void visibleObjectsChanged() {
		_gnd.setVisibleObjects(getVisibleObjects());
	}

	private Set<SystemObject> getVisibleObjects() {

		final Set<SystemObject> result = new HashSet<SystemObject>();
		final Rectangle filterRectangle = getUTMBounds();

		for(Component component : getComponents()) {
			if(component instanceof LayerPanel) {
				final LayerPanel layerPanel = (LayerPanel)component;
				if(layerPanel.isVisible()) {
					for(final DisplayObject displayObject : layerPanel.getDisplayObjects()) {
						if(displayObject == null){
							System.err.println("displayobject ist null");
							Thread.dumpStack();
							continue;
						}
						if(displayObject.getBoundingRectangle() != null && displayObject.getBoundingRectangle().intersects(filterRectangle)) {
							result.add(displayObject.getSystemObject());
						}
					}
				}
			}
		}
		return result;
	}

	public void selectObject(final SystemObject systemObject) {
		final Set<DisplayObject> displayObjects = getDisplayObjectsForSystemObject(systemObject);
		setSelection(displayObjects);
	}
	public void focusOnObject(final SystemObject systemObject) {
		final Set<DisplayObject> displayObjects = getDisplayObjectsForSystemObject(systemObject);
		Rectangle2D rect = null;
		for(DisplayObject displayObject : displayObjects) {
			if(rect == null){
				rect = displayObject.getBoundingRectangle();
				if(rect != null){
					rect = (Rectangle2D)rect.clone();
				}
			}
			else{
				rect.add(displayObject.getBoundingRectangle());
			}
		}
		if(rect == null) return;
		Point center = new Point((int)rect.getCenterX(), (int)rect.getCenterY());
		Point utmPoint = new Point();
		AffineTransform affineTransform = new AffineTransform();
		modifyAffineTransform(affineTransform);
		affineTransform.transform(center, utmPoint);
		_zoomTranslateX -= (utmPoint.getX() - getWidth() / 2);
		_zoomTranslateY -= (utmPoint.getY() - getHeight() / 2);
		repaint();
		visibleObjectsChanged();
	}


	private Set<DisplayObject> getDisplayObjectsForSystemObject(final SystemObject systemObject) {
		final Set<DisplayObject> displayObjects = new HashSet<DisplayObject>();
		for(Component component : getComponents()) {
			if(component instanceof LayerPanel) {
				final LayerPanel layerPanel = (LayerPanel)component;
				DisplayObject object = layerPanel._displayObjects.get(systemObject);
				if(object != null) {
					displayObjects.add(object);
				}
			}
		}
		return displayObjects;
	}


	/*
		 * Gehört zur Implementation des View.ChangeListeners.
		 */
	public void viewEntryInserted(View view, final int newIndex) {
		int max = view.getViewEntries().size() - 2;
		for ( int i = max; i > max - newIndex; i--) {
			for ( Component component : getComponentsInLayer(i)) {
				setLayer(component, i + 1);
			}
		}
		ViewEntry entry = view.getViewEntries().get(newIndex);
		initALayerPanel( entry, max - newIndex + 1, new JProgressBar());
		_displayObjectManager.subscribeDisplayObjects();
		_displayObjectManager.addMapScaleListeners();
		visibleObjectsChanged();
		repaint();
	}

	/*
	 * Gehört zur Implementation des View.ChangeListeners.
	 */
	public void viewEntryChanged(View view, int i) {
		final int j = view.getViewEntries().size()-1-i;
		for ( Component component : getComponentsInLayer(j)) {
			component.setVisible(view.getViewEntries().get(i).isVisible(getMapScale().intValue()));
		}
		visibleObjectsChanged();
		repaint();
	}

	/*
	 * Gehört zur Implementation des View.ChangeListeners.
	 */
	public void viewEntryRemoved(View view, int i) {
		final int j = view.getViewEntries().size()-i;
		for ( Component component : getComponentsInLayer(j)) {
			if ( component instanceof LayerPanel) {
				final LayerPanel layerPanel = (LayerPanel) component;
				final Collection<DisplayObject> displayObjects = layerPanel.getDisplayObjects();
				_displayObjectManager.unsubscribeDisplayObjects( displayObjects);
				final Collection<MapScaleListener> mapScaleListeners = new ArrayList<MapScaleListener>();
				for ( DisplayObject displayObject : displayObjects) {
					mapScaleListeners.add( displayObject);
				}
				MapPane.this.removeMapScaleListeners( mapScaleListeners);
			}
			remove(component);
		}
		for ( int k = j+1; k <= highestLayer(); k++) {
			for ( Component component : getComponentsInLayer(k)) {
				setLayer(component, k-1);
			}
		}
		visibleObjectsChanged();
		repaint();
	}

	/*
	 * print-Methode zum Drucken schreiben.
	 */
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {

		if(pageIndex >= 1) {
			return NO_SUCH_PAGE;
		}

		Graphics2D g2d = (Graphics2D)g;
		// Seitenformat anpassen
		final Rectangle bounds = getBounds();
		double scaleWidth = pageFormat.getImageableWidth() / bounds.width;
		double scaleHeight = pageFormat.getImageableHeight() / bounds.height;
		double scale = Math.min(scaleWidth, scaleHeight);

		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		g2d.scale(scale, scale);
		disableDoubleBuffering(this);
		paint(g2d);
		enableDoubleBuffering(this);

		return PAGE_EXISTS;
	}

	/**
	 * Methode zur besseren Auflösung beim Drucken
	 *
	 * @param c eine Component
	 */
	public static void disableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	/**
	 * Methode zum Zurücksetzen der Auflösung für die Ausgabe in der Oberfläche
	 *
	 * @param c eine Component
	 */
	public static void enableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}

	/**
	 * Diese Methode berechnet den Maßstab der Kartenansicht in Metern pro Pixel.
	 */
	public double meterProPixel() {
		if ( _showNothing ) {
			return -1;
		}
		final Rectangle displayRectangle = _displayObjectManager.getDisplayRectangle( _gnd.getSystemObjects());
		int minX = (int)displayRectangle.getMinX();
		int maxX = (int)displayRectangle.getMaxX();
		int minY = (int)displayRectangle.getMinY();
		int meterDistance = maxX -minX;

		AffineTransform affineTransform = (AffineTransform)_mapTransform.clone();
		affineTransform.scale(_zoomScale, _zoomScale);

		Point p1 = new Point ( minX, minY);
		affineTransform.transform(p1, p1);
		Point p2 = new Point ( maxX, minY);
		affineTransform.transform(p2, p2);
		final double pixelDistance = p2.getX() - p1.getX();

		return meterDistance / pixelDistance;
	}

	private void determineCurrentScale() {
		final double meterProPixel = meterProPixel();
		Double dpi = _gnd.getScreenResolution();
		if ( dpi == null) {
			dpi = new Double(Toolkit.getDefaultToolkit().getScreenResolution());
		}
		setMapScale((int) (meterProPixel * 100 * dpi / 2.54));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2D = (Graphics2D) g;

		AffineTransform affineTransform = g2D.getTransform();
		if ( _showNothing || (affineTransform == null)) {
			return;
		}
		modifyAffineTransform(affineTransform);
		AffineTransform oldTransform = g2D.getTransform();
		g2D.setTransform(affineTransform);

		// zur besseren Auflösung
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// Rechteck vor dem Neuzeichnen leeren.
		g2D.setBackground(Color.WHITE);
		Rectangle bounds = getBounds();
		g2D.clearRect(0, 0, (int)bounds.getWidth(), (int)bounds.getHeight());

		for ( int i = 0; i <= highestLayer(); i++ ) {
			Component[] components = getComponentsInLayer(i);
			for ( Component component : components) {
				if ( component.isVisible()) {
					if ( component instanceof LayerPanel ) {
						LayerPanel la = (LayerPanel) component;
						la.paintLayer(g2D);
					}
				}
			}
		}

		g2D.setTransform(oldTransform);
		g2D.setColor(Color.black);
		g2D.setStroke(new BasicStroke(1));
		g2D.setFont(new Font("Default", Font.PLAIN, 10));

		drawScaling(g2D, 1 / meterProPixel());
	}

	private void drawScaling(final Graphics2D g, final double pixelPerMeter) {

		double lineLength = pixelPerMeter;
		if(lineLength <= 0) return;
		int factor = 10;
		while(lineLength < 20) {
			lineLength *= 10;
			factor *= 10;
		}
		if(lineLength > 100) {
			lineLength /= 5;
			factor /= 5;
		}
		if(lineLength > 40) {
			lineLength /= 2;
			factor /= 2;
		}

		if(factor >= 10000) {
			factor /= 10000;
			g.drawString(factor + " km", 10, getHeight() - 15);
		}
		else if(factor >= 10) {
			factor /= 10;
			g.drawString(factor + " m", 10, getHeight() - 15);
		}
		else {
			g.drawString(factor + "0 cm", 10, getHeight() - 15);
		}
		g.draw(new Line2D.Double(10, getHeight() - 10, 10 + lineLength, getHeight() - 10));
		g.draw(new Line2D.Double(10, getHeight() - 11, 10, getHeight() - 9));
		g.draw(new Line2D.Double(10 + lineLength, getHeight() - 11, 10 + lineLength, getHeight() - 9));
	}

	private void addToSelection(DisplayObject displayObject) {
		if(displayObject != null) {
			_selectedDisplayObjects.add(displayObject);
			redrawObject(displayObject);
		}
	}

	public void clearSelection() {
		List<Rectangle> rectangles = new ArrayList<Rectangle>();
		for(DisplayObject displayObject : _selectedDisplayObjects) {
			final Rectangle boundingRectangle = displayObject.getBoundingRectangle();
			if(boundingRectangle != null) {
				rectangles.add(transformedRectangle(boundingRectangle));
			}
		}
		_selectedDisplayObjects.clear();
		for(Rectangle rectangle : rectangles) {
			repaint(rectangle);
		}
		_gnd.selectionChanged();
	}

	private void setSelection(DisplayObject displayObject) {
		clearSelection();
		addToSelection(displayObject);
		_gnd.selectionChanged();
	}

	private void setSelection(Collection<DisplayObject> displayObjects) {
		if(displayObjects.equals(_selectedDisplayObjects)) return;
		clearSelection();
		for(DisplayObject displayObject : displayObjects) {
			addToSelection(displayObject);
		}
		_gnd.selectionChanged();
	}

	private void addListeners() {
		MouseListener mouseListener = new MouseAdapter () {
			private JPopupMenu _popup = null;
			private HashMap<String, DisplayObject> _displayObjectHash;
			private int _lastOffsetX;
			private int _lastOffsetY;

			class MenuItemSelector implements ActionListener {
				public void actionPerformed(ActionEvent ae) {
					clearSelection();
					final String actionCommand = ae.getActionCommand();
					addToSelection( _displayObjectHash.get(actionCommand));
					_gnd.selectionChanged();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if ( e.isAltDown() ) {	// Selektion unter Alt-Taste
					clearSelection();
					if ( _popup != null ) {
						_popup.setVisible(false);
					}
					Point point = e.getPoint();
					Point utmPoint = new Point();
					if ( !getUTMPoint( point, utmPoint)) {
						return;
					}
					Map<String, DisplayObject> displayObjectsHash= new HashMap<String,DisplayObject>();
					for ( ViewEntry entry : _view.getViewEntries()) {
						if ( entry.isVisible(getMapScale().intValue()) && entry.isSelectable()) {
							Component component = entry.getComponent();
							if ( component instanceof LayerPanel) {
								LayerPanel layerPanel = (LayerPanel) component;
								final List<DisplayObject> displayObjectsCloseToPoint = layerPanel.getDisplayObjectsCloseToPoint(utmPoint, 5);
								for ( DisplayObject displayObject : displayObjectsCloseToPoint) {
									displayObjectsHash.put(entry.getLayer().getName() + ": " + displayObject.getSystemObject().getNameOrPidOrId(), displayObject);
								}
							}
						}
					}
					if (displayObjectsHash.size() == 0) {
						return;	// vermeidet ein Artefakt
					} else if (displayObjectsHash.size() == 1) {
						setSelection(displayObjectsHash.values().iterator().next());
					} else {
						if ( e.isControlDown() ) {
							setSelection(displayObjectsHash.values());
						} else {
							_popup = new JPopupMenu ();
							_displayObjectHash = new HashMap<String, DisplayObject>();
							for ( String key : displayObjectsHash.keySet()) {
								JMenuItem menuItem = new JMenuItem( key);
								_popup.add(menuItem);
								menuItem.addActionListener(new MenuItemSelector());
								_displayObjectHash.put(key, displayObjectsHash.get(key));
							}
							_popup.show(e.getComponent(), point.x, point.y);
							_popup.setVisible(true);
						}
					}
				}
			}

			@Override
			public void mouseDragged (MouseEvent e) {
				int newX = e.getX() - _lastOffsetX;
				int newY = e.getY() - _lastOffsetY;
				_lastOffsetX += newX;
				_lastOffsetY += newY;
				_zoomTranslateX += newX;
				_zoomTranslateY += newY;
				visibleObjectsChanged();
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				_lastOffsetX = e.getX();
				_lastOffsetY = e.getY();
			}

			@Override
			public void mouseReleased(MouseEvent e) { }
		};
		addMouseListener( mouseListener);
		addMouseMotionListener( (MouseMotionListener)mouseListener);

		class ScaleHandler implements MouseWheelListener {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					Double _mysticalFactor = 0.1 * _zoomScale;
//					if ( _mysticalFactor <= 0.03 * _zoomScale ) {
//						_mysticalFactor = 0.03 * _zoomScale;
//					}
					int wheelRotation = e.getWheelRotation();
					Double c = - _mysticalFactor * wheelRotation;
					Double factor = _zoomScale / (_zoomScale+c);
					if ( factor > 2. || factor < 0. || factor.isInfinite() || factor.isNaN()) {	// Beschränkung des Herauszoomen auf das Doppelte
						factor = 2.;
						c = - _zoomScale / 2.;
					}
					if ( factor <.5) {	// Beschränkung des Hineinzoomen auf die Hälfte
						factor = .5;
						c = _zoomScale;
					}
					final double nextMapScale = getMapScale() * factor;
					// Weiteres rein- oder rauszoomen unterbinden!
					// Die Grenzen sind etwas willkürlich gewählt. Tatsächlich wurden Probleme bei Werten
					// um die 30 beobachtet (der Mechanismus geht dort kaputt!) ... wahrscheinlich, weil dann 
					// beim Integer-Runden immer wieder derselbe Wert kommt. 1 : 100 ist aber okay, weil dabei 
					// 1 cm auf dem Bildschirm genau 1 Meter in der Realität entspricht.
					if ( (nextMapScale < 200000000.)&& (nextMapScale > 100.)) {
						Point2D p = new Point2D.Double(e.getX(), e.getY());
						AffineTransform at = new AffineTransform();
						at.translate(_zoomTranslateX, _zoomTranslateY);
						at.scale( _zoomScale, _zoomScale);
						try {
							at.inverseTransform(p, p);
						}
						catch(NoninvertibleTransformException e1) {
							return;
						}
						_zoomScale += c;
						_zoomTranslateX += -c*p.getX();
						_zoomTranslateY += -c*p.getY();
						setMapScale(nextMapScale);
					}
				}
			}
		}
		addMouseWheelListener(  new ScaleHandler());
	}

	public void redrawObject(final DisplayObject displayObject) {
		final Rectangle boundingRectangle = displayObject.getBoundingRectangle();
		if ( boundingRectangle != null) {
			repaint( transformedRectangle( boundingRectangle));
		}
	}

	/**
	 * Ein Interface für Listener, die an Maßstabs-Änderungen der Kartenansicht interessiert sind.
	 *
	 * @author Kappich Systemberatung
	 * @version $Revision: 9288 $
	 *
	 */
	public interface MapScaleListener {
		/**
		 * Diese Methode wird für die Listener aufgerufen, wenn eine Maßstabsänderung mitgeteilt werden muss.
		 *
		 * @param scale der neue Maßstabsfaktor
		 */
		void mapScaleChanged( double scale);
	};

	/**
	 * Aktualisiert den Maßstab der Kartenansicht, informiert alle MapScaleListeners und
	 * veranlaßt ein Neuzeichnen der Kartenansicht.
	 *
	 * @param scale der neue Maßstabsfaktor
	 */
	private void setMapScale(double mapScale) {
		_mapScale = mapScale;
		for ( MapScaleListener mapScaleListener : _mapScaleListeners) {
			mapScaleListener.mapScaleChanged(mapScale);
		}
		final int h = highestLayer();
		for ( int i = 0; i <= h; i++) {
			for ( Component component : getComponentsInLayer(i)) {
				component.setVisible(_view.getViewEntries().get(h-i).isVisible(getMapScale().intValue()));
			}
		}
		visibleObjectsChanged();
		repaint();
	}

	/**
	 * Gibt den aktuellen Maßstab zurück.
	 *
	 * @return der Maßstabsfaktor
	 */
	public Double getMapScale() {
		return _mapScale;
	}

	/**
	 * Fügt die übergebenen Objekte der Menge der auf Änderungen des Maßstabs angemeldeten Objekte hinzu.
	 *
	 * @param listeners die neuen Listener
	 */
	public void addMapScaleListeners( final Collection<MapScaleListener> listeners) {
		if ( listeners != null) {
			_mapScaleListeners.addAll( listeners);
		}
	}

	/**
	 * Entfernt die übergebenen Objekte aus der Menge der auf Änderungen des Maßstabs angemeldeten Objekte.
	 *
	 * @param listeners die zu löschenden Listener
	 */
	public void removeMapScaleListeners( final Collection<MapScaleListener> listeners) {
		if ( listeners == null) {
			return;
		}
		Runnable remover = new Runnable() {
			public void run() {
				_mapScaleListeners.removeAll( listeners);
			}
		};
		Thread removerThread = new Thread( remover);
		removerThread.start();
	}

	private void removeAllMapScaleListeners() {
		_mapScaleListeners.clear();
	}

	private boolean getUTMPoint( Point p, Point utmP) {
		AffineTransform affineTransform = new AffineTransform();
		modifyAffineTransform(affineTransform);
		AffineTransform inverseT;
		try {
			inverseT = affineTransform.createInverse();
		}
		catch(NoninvertibleTransformException e1) {
			return false;
		}
		inverseT.transform( p, utmP);
		return true;
	}

	/**
	 * Erzeugt den Tooltipp auf der Kartenansicht.
	 *
	 * @param e der Mouse-Event
	 */
	@Override
	public String getToolTipText(MouseEvent e) {
		if ( !_isTooltipOn) {
			return null;
		}
		Point p = e.getPoint();
		Point utmPoint = new Point();
		if ( !getUTMPoint( p, utmPoint)) {
			return "";
		}
		String s = new String("<html>");
		boolean stringIsEmpty = true;
		final int maxNumberOfObjects = 24;
		int countNumberOfObjects = 0;
		final int maxNumberOfObjectsPerPanel = 5;
		for ( ViewEntry entry : _view.getViewEntries()) {
			if ( entry.isVisible(getMapScale().intValue()) && entry.isSelectable()) {
				Component component = entry.getComponent();
				if ( component instanceof LayerPanel) {
					int countTheObjectsOfThisPanel = 0;
					LayerPanel layerPanel = (LayerPanel) component;
					final List<DisplayObject> displayObjectsCloseToPoint = layerPanel.getDisplayObjectsCloseToPoint(utmPoint, 2);
					for ( DisplayObject displayObject : displayObjectsCloseToPoint) {
						stringIsEmpty = false;
						countTheObjectsOfThisPanel++;
						if ( countTheObjectsOfThisPanel > maxNumberOfObjectsPerPanel) {
							s += "..."	+ "<br></br>";
							break;
						}
						s += entry.getLayer().getName() + ": " + displayObject.getSystemObject().getNameOrPidOrId() + "<br></br>";
					}
					countNumberOfObjects += countTheObjectsOfThisPanel;
					if ( countNumberOfObjects > maxNumberOfObjects) {
						break;
					}
				}
			}
		}
		s += "</html>";
		if ( stringIsEmpty ) {
			return null;
		}
		return s;
	}

	private void modifyAffineTransform( AffineTransform affinTransform) {
		affinTransform.translate(_zoomTranslateX, _zoomTranslateY);
		affinTransform.scale(_zoomScale, _zoomScale);
		if (_mapTransform == null) {
			initAffineMapTransform();
			if ( _showNothing) {
				return;
			}
			determineCurrentScale();
		}
		affinTransform.concatenate(_mapTransform);
	}

	private Rectangle transformedRectangle( Rectangle rectangle) {
		if ( rectangle == null) {
			return null;
		}
		Graphics2D g2D = (Graphics2D)getGraphics();
		if ( g2D == null) {
			return null;
		}
		AffineTransform affineTransform = g2D.getTransform();
		modifyAffineTransform(affineTransform);
		Point p1 = new Point ( (int) rectangle.getMinX(), (int) rectangle.getMinY());
		affineTransform.transform(p1, p1);
		Rectangle transformedRect = new Rectangle( p1);
		Point p2 = new Point ( (int) rectangle.getMaxX(), (int)rectangle.getMaxY());
		affineTransform.transform(p2, p2);
		transformedRect.add(p2);
		return transformedRect;
	}

	/**
	 * Gibt <code>true</code> zurück, wenn die Kartenansicht mit Anti-Aliasing gezeichnet wird.
	 *
	 * @return <code>true</code> genau dann, wenn die Kartenansicht mit Anti-Aliasing gezeichnet wird
	 */
	public boolean isAntialising() {
		return _antialising;
	}

	/**
	 * Setzt die interne Variable, die bestimmt, ob die Kartenansicht mit Anti-Aliasing gezeichnet wird.
	 *
	 * @param antialising die neue Einstellung von Anti-Aliasing
	 */
	public void setAntialising(boolean antialising) {
		_antialising = antialising;
	}

	/**
	 * Gibt <code>true</code> zurück, falls der Tooltipp auf der Kartenansicht aktiviert ist.
	 *
	 * @return <code>true</code> genau dann, wenn der Tooltipp auf der Kartenansicht aktiviert ist
	 */
	public boolean isTooltipOn() {
		return _isTooltipOn;
	}

	/**
	 * Schaltet den Tooltipp auf der Kartenansicht ab oder an.
	 *
	 * @param tooltip der neue Wert für die Tooltipp-Aktivität
	 */
	public void setTooltip( boolean tooltip) {
		_isTooltipOn = tooltip;
	}

	/**
	 * Veranlaßt eine Aktualisierung der Darstellung des übergebenen DisplayObjects.
	 *
	 * @param displayObject das DisplayObject
	 */
	public void updateDisplayObject ( DisplayObject displayObject) {
		if (displayObject != null) {
			final Rectangle boundingRectangle = displayObject.getBoundingRectangle();
			if ( boundingRectangle != null) {
				final Rectangle rectangle = transformedRectangle( boundingRectangle);
				if ( rectangle != null) {
					repaint( rectangle);
				}
			}
		}
	}

	/*
	 * Implementiert die Methode des Interfaces GenericNetDisplay.ResolutionListener.
	 */
	public void resolutionChanged(Double newValue, Double oldValue) {
		final Double mapScale = (_mapScale*newValue)/oldValue;
		setMapScale( mapScale.intValue());
	}

	/**
	 * Gibt die Menge der aktuell in der Kartenansicht selektierten Objekte zurück.
	 *
	 * @return die Menge der aktuell in der Kartenansicht selektierten Objekte
	 */
	public Collection<SystemObject> getSelectedSystemObjects() {
		final Set<SystemObject> systemObjects = new HashSet<SystemObject>();
		for ( DisplayObject displayObject : _selectedDisplayObjects) {
			systemObjects.add( displayObject.getSystemObject());
		}
		return systemObjects;
	}

	/**
	 * Gibt alles frei, so dass der Garbage-Collector zuschlagen kann.
	 */
	public void clearEverything() {
		// Abmelden beim Datenverteiler
		final Component[] components = getComponents();
		for ( Component component : components) {
			if ( component instanceof LayerPanel) {
				final LayerPanel layerPanel = (LayerPanel) component;
				final Collection<DisplayObject> displayObjects = layerPanel.getDisplayObjects();
				_displayObjectManager.unsubscribeDisplayObjects( displayObjects);
			}
			remove(component);
		}
		// ViewEntries den Rückwärtsverweis auf die Component nehmen
		for ( ViewEntry entry : _view.getViewEntries()) {
			entry.setComponent( null);
		}
		// Entferne die MapScaleListener
		removeAllMapScaleListeners();
		_mapScaleListeners.clear();
	}

	/*
	 * Implementiert die Methode des Interfaces DOTManager.DOTChangeListener
	 */
	public void displayObjectTypeAdded(DisplayObjectType displayObjectType) {
		visibleObjectsChanged();
		repaint();
	}
	/*
	 * Implementiert die Methode des Interfaces DOTManager.DOTChangeListener
	 */
	public void displayObjectTypeChanged(DisplayObjectType displayObjectType) {
		visibleObjectsChanged();
		repaint();
	}
	/*
	 * Implementiert die Methode des Interfaces DOTManager.DOTChangeListener
	 */
	public void displayObjectTypeRemoved(String displayObjectTypeName) {
		visibleObjectsChanged();
		repaint();
	}
}
