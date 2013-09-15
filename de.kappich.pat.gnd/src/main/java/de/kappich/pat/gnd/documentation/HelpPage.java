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
package de.kappich.pat.gnd.documentation;

import de.bsvrz.sys.funclib.debug.Debug;

import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

/**
 * Das Fenster zur Darstellung der Online-Hilfe.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public class HelpPage {
	
	final private static Debug _debug = Debug.getLogger();
	
	private void openHelpPage(final URL url) {
		final JEditorPane helpPane = new JEditorPane();
		helpPane.setEditorKit( new MyEditorKit());
		try {
			helpPane.setPage(url);
		}
		catch(IOException e1) {
			throw new UnsupportedOperationException("IOException", e1);
		}
		helpPane.setEditable(false);
		
		helpPane.addHyperlinkListener(
				new HyperlinkListener() {
					public void hyperlinkUpdate(HyperlinkEvent event) {
						if(event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
							try {
								helpPane.setPage(event.getURL());
							}
							catch(IOException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					}
				}
		);
		final JFrame browserFrame = new JFrame("Bedienungsanleitung");
		browserFrame.setSize(950, 800);
		browserFrame.setLocationRelativeTo(null);
		browserFrame.add(new JScrollPane(helpPane));
		browserFrame.setVisible(true);
		
		helpPane.fireHyperlinkUpdate( new HyperlinkEvent( helpPane, HyperlinkEvent.EventType.ACTIVATED, url));
	}
	
	/**
	 * Diese Methode öffnet die Dokumentation an der übergebenen Stelle.
	 * 
	 * @param href eine Hypertext-Referenz
	 */
	public static void openHelp( final String href) {
		HelpPage helpPage = new HelpPage();
		URL url;
		final URL documentationPage = HelpPage.class.getResource("documentation.html");
		try {
			url = new URL( documentationPage, href);
		}
		catch(MalformedURLException e) {
			final Throwable cause = e.getCause();
			if ( cause != null && cause.getMessage() != null) {
				_debug.error("HelpPage.openHelp(): MalformedURLException mit Ursache: " + e.getMessage());
			} else {
				_debug.error("HelpPage.openHelp(): MalformedURLException ohne detailliere Ursache.");
			}
			if ( documentationPage == null) {
				_debug.error("HelpPage.openHelp(): die Variable für die HTML-Dokumentationsdatei ist leer.");
			} else {
				_debug.info("HelpPage.openHelp(): die Variable für die HTML-Dokumentationsdatei ist okay.");
			}
			if ( href == null) {
				_debug.warning("HelpPage.openHelp(): die Hypertext-Referenz in der HTML-Dokumentationsdatei ist leer.");
			} else {
				_debug.info("HelpPage.openHelp(): die Hypertext-Referenz ist: " + href);
			}
			return;
		}
		helpPage.openHelpPage( url);
	}
	
	@SuppressWarnings("serial")
	private class MyEditorKit extends HTMLEditorKit {
		MyEditorKit() {
			super();
		}
		
		@Override
		public ViewFactory getViewFactory() {
			return new MyViewFactory();
		}
	}
	
	private class MyViewFactory extends HTMLEditorKit.HTMLFactory {
		
		MyViewFactory() {
			super();
		}
		
		@Override
		public View create(Element elem) {
			View view = _factory.create( elem);
			if ( view instanceof ImageView) {
				return new MyImageView(elem);
			}
			return view;
		}
	}
	
	private class MyImageView extends ImageView {
		
		public MyImageView (Element elem) {
			super (elem);
			setLoadsSynchronously( true);		
		}
	}
	
	private static final HTMLEditorKit.HTMLFactory _factory = new HTMLEditorKit.HTMLFactory();
	
	/**
	 * Liefert eine Map zurück, deren Schlüssel die Dateinamen und Werte die PNG-Dateien
	 * des lokalen Verzeichnisses sind.
	 * 
	 * @return die oben beschriebene Map
	 */
	public static Map<String, BufferedImage> getImages() {
		HashMap<String, BufferedImage> images = new HashMap<String, BufferedImage>();
		final List<String> imageNames = getPngNames();
		if ( imageNames == null) {
			return images;
		} else { 
			for ( String fileName : imageNames) { 
				BufferedImage image;
				try {
					image = ImageIO.read(HelpPage.class.getResourceAsStream( fileName));
				}
				catch(IOException e) {
					throw new UnsupportedOperationException("IOException", e);
				}
				images.put( fileName, image);
			}
			
			final JFrame aFrame = new JFrame();
			MediaTracker mediaTracker = new MediaTracker( aFrame);
			int id = 0;
			for ( BufferedImage image : images.values()) {
				mediaTracker.addImage(image, id);
				id++;
			}
			try {
				mediaTracker.waitForAll();
			}
			catch(InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		return images;
	}
	
	@SuppressWarnings("unchecked")
	private static List<String> getPngNames() {
		List<String> result = new ArrayList<String>();
		URL url = HelpPage.class.getResource("");
		if (url == null) {
			return result;
		}
		File directory;
		try {
			directory = new File(url.toURI());
		}
		catch (Exception e) {
			directory = null;
		}
		if( (directory != null) && directory.exists()) {
			String[] files = directory.list();
			for(int i = 0; i < files.length; i++) {
				if( files[i].endsWith(".png")) {
					result.add( files[i]);
				}
			}
		} else {
			try {
				final URLConnection connection = url.openConnection();
				if ( connection == null) {
					_debug.error("HelpPage.getPngNames(): URL.openConnection() scheiterte für " + url.getPath() + ".");
					return result;
				}
				if ( !(connection instanceof JarURLConnection)) {
					_debug.error("HelpPage.getPngNames(): URL.openConnection() lieferte keine JarURLConnection.");
					return result;
				}
				JarURLConnection jarConnection = (JarURLConnection) connection;
				String starts = jarConnection.getEntryName();
				if ( starts == null) {
					_debug.error("HelpPage.getPngNames(): die JarURLConnection lieferte keinen Entry-Namen.");
					return result;
				}
				JarFile jarFile = jarConnection.getJarFile();
				if ( jarFile == null) {
					_debug.error("HelpPage.getPngNames(): die JarURLConnection lieferte kein JarFile.");
					return result;
				}
				Enumeration e = jarFile.entries();
				if ( e == null) {
					_debug.error("HelpPage.getPngNames(): das JarFile lieferte kein Entries.");
					return result;
				}
				while(e.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					String entryName = entry.getName();
					if( entryName.startsWith(starts) && 
							(entryName.lastIndexOf('/') <= starts.length()) && 
							entryName.endsWith(".png")) {
						if ( entryName.contains("/")) {
							entryName = entryName.substring( entryName.lastIndexOf('/')+1);
						}
						result.add( entryName);
					}
				}
			}
			catch(IOException ioex) {
				System.err.println(ioex);
			}
		}
		return result;
	}
}
