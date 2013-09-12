/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.areaPlugin.DOTAreaPlugin;
import de.kappich.pat.gnd.complexPlugin.DOTComplexPlugin;
import de.kappich.pat.gnd.displayObjectToolkit.DOTManager;
import de.kappich.pat.gnd.linePlugin.DOTLinePlugin;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.pointPlugin.DOTPointPlugin;

import java.util.*;

/**
 * Diese Klasse dient zur Verwaltung externer Plugins.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 *
 */
public class PluginManager {
	
	/* Hier werden die externen Plugins gespeichert. Jeder Name muss der vollständige Klassenname
	 * einer Klasse sein, die das Interface DisplayObjectTypePlugin implementiert. */
	final private static Set<String> _pluginNames = new HashSet<String>();
	/* In dieser Map sind die Schlüssel die Rückgabewerte von DisplayObjectTypePlugin.getName() für externe
	 * Implementationen von DisplayObjectTypePlugin und die Werte sind die Implementationen selber. */
	final private static Map<String, DisplayObjectTypePlugin> _pluginsMap = new HashMap<String, DisplayObjectTypePlugin>();
	
	final private static Debug _debug = Debug.getLogger();
	
	private PluginManager() {}
	
	/**
	 * Diese Methode macht die externen Plugins bekannt, indem die vollständigen Namen der Klassen,
	 * die DisplayObjectTypePlugin implementieren, übergeben werden. Sie muss vor dem ersten Zugriff
	 * auf Teile dieser Plugins aufgerufen werden; der beste Moment dafür ist, bevor der erste
	 * Konstruktor von {@link GenericNetDisplay} aufgerufen wird, denn sonst könnte schon die 
	 * Initialisierung aus den Präferenzen scheitern; man beachte, dass GenericNetDisplay eine 
	 * {@link GenericNetDisplay.#addPlugins gleichnamige} und ebenfalls statische Methode 
	 * anbietet, die die Arbeit an diese Methode delegiert.
	 * 
	 * @param plugins die hinzuzufügenden Externen Plugins
	 */
	public static void addPlugins( final List<String> plugins) {
		if ( plugins == null || (plugins.size() == 0)) {
			return;
		}
		final List<String> newPlugins = new ArrayList<String>();
		for ( String pluginName : plugins) {
			if ( _pluginNames.contains( pluginName)) {
				continue;
			}
			final Class<?> pluginClass;
			try {
				pluginClass = Class.forName( pluginName);
			}
			catch(ClassNotFoundException e) {
				_debug.error( "Fehler im PluginManager: die Klasse '" + pluginName + "' kann nicht instanziiert werden.");
				continue;
			}
			Class<?> dotPluginClass;
			try {
				dotPluginClass = Class.forName( "de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin");
			}
			catch(ClassNotFoundException e) {
				throw new UnsupportedOperationException("Schwerer interner Fehler - ClassNotFoundException", e);
			}
			final Class<?>[] interfaces = pluginClass.getInterfaces();
			boolean interfaceFound = false;
			for ( Class<?> iface : interfaces) {
				if ( iface.equals( dotPluginClass)) {
					interfaceFound = true;
					break;
				}
			}
			if ( interfaceFound) {
				final Object pluginObject;
				try {
					pluginObject = pluginClass.newInstance();
                }
                catch(InstantiationException e) {
                	_debug.error( "Fehler im PluginManager: es kann kein Objekt der Klasse '" + pluginName + "' instanziiert werden.");
    				continue;
                }
                catch(IllegalAccessException e) {
                	_debug.error( "Fehler im PluginManager: es kann nicht auf ein Objekt der Klasse '" + pluginName + "' zugegriffen werden.");
    				continue;
                }
                final DisplayObjectTypePlugin displayObjectTypePlugin = (DisplayObjectTypePlugin) pluginObject;
                final String name = displayObjectTypePlugin.getName();
                if ( name.equals( "Fläche") || name.equals( "Komplex") || name.equals( "Linie") || name.equals( "Punkt")) {
                	_debug.error( "Fehler im PluginManager: ein Plugin mit dem Namen '" + name + "' kann nicht hinzugefüht werden.");
                } else if ( _pluginsMap.containsKey( name)) {
                	_debug.error( "Fehler im PluginManager: ein Plugin mit dem Namen '" + name + "' wurde bereits hinzugefügt.");
                } else {
                	_pluginNames.add( pluginName);
                	_pluginsMap.put( name, displayObjectTypePlugin);
                	newPlugins.add( pluginName);
                }
			} else {
				_debug.error( "Fehler im PluginManager: die Klasse '" + pluginName + 
				"' implementiert nicht das Interface DisplayObjectTypePlugin.");
			}
		}
		
		/* Das Folgende wird nicht über einen Listener-Mechanismus implementiert, weil sonst
		 * sichergestellt werden müsste, dass der/die Listener bereits registriert sind. */
		DOTManager.pluginsAdded( newPlugins);
	}

	/**
	 * Gibt die Namen aller Plugins zurück. Dazu zählen die 4 internen Plugins und alle 
	 * externen Plugins.
	 * 
	 * @return alle Plugin-Namen
	 */
	public static Vector<String> getAllPluginNames() {
		final Vector<String> names = new Vector<String>();
		names.add( "Fläche");
		names.add( "Komplex");
		names.add( "Linie");
		names.add( "Punkt");
		names.addAll( _pluginsMap.keySet());
		return names;
	}
	
	/** 
	 * Gibt das DisplayObjectTypePlugin-Objekt zurück, dessen getName-Implementation den
	 * übergebenen Namen zurückgibt, und <code>null</code>, wenn kein solches Objekt existiert.
	 * 
	 * @param name ein Plugin-Name
	 * @return das Plugin oder <code>null</code>
	 */
	public static DisplayObjectTypePlugin getPlugin( final String name) {
		if ( name.equals( "Fläche")) {
			return new DOTAreaPlugin();
		} else if ( name.equals( "Komplex")) {
			return new DOTComplexPlugin();
		} else if ( name.equals( "Linie")) {
			return new DOTLinePlugin();
		} else if ( name.equals( "Punkt")) {
			return new DOTPointPlugin();
		} else {
			return _pluginsMap.get( name);
		}
	}
}
