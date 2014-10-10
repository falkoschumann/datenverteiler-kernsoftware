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

import de.kappich.pat.gnd.viewManagement.View;
import de.kappich.pat.gnd.viewManagement.ViewManager;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.application.AbstractGUIApplication;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList.Argument;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Die Start-Klasse für die GND im Stand-Alone-Betrieb.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11347 $
 */
public class RunGND extends AbstractGUIApplication implements StandardApplication {

	final List<String> _plugins = new ArrayList<String>();

	/** @param args die Aufrufparemeter */

	public static void main(String[] args) {
		System.getProperties().put("apple.laf.useScreenMenuBar", "true");
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
		StandardApplicationRunner.run(new RunGND(), args);
	}

	public void parseArguments(ArgumentList argumentList) throws Exception {
		_debug = Debug.getLogger();
		_debug.fine("argumentList = " + argumentList);
		final Argument pluginArgument = argumentList.fetchArgument("-plugins=");
		if(pluginArgument != null && pluginArgument.hasValue()) {
			final String value = pluginArgument.getValue();
			if((value != null) && (value.length() > 0)) {
				final String[] valueParts = value.split(",");
				for(String valuePart : valueParts) {
					if((valuePart != null) && (valuePart.length() > 0)) {
						_plugins.add(valuePart);
					}
				}
			}
		}
	}

	public void initialize(ClientDavInterface connection) throws Exception {
		PreferencesHandler.setKvPid(connection.getLocalConfigurationAuthority().getPid());

		ViewManager viewManager = ViewManager.getInstance();
		List<SystemObject> systemObjects = getSystemObjects(connection);
		String viewName = "Vordefinierte Ansicht 1";
		final View view = viewManager.getView(viewName);
		if(view == null) {
			final String x = "RunGND: ein View namens '" + viewName + "' kann nicht gefunden werden.";
			_debug.error(x);
			System.err.println(x);
			System.exit(1); // RunGND darf beendet werden!
		}
		if(_plugins.size() > 0) {
			GenericNetDisplay.addPlugins(_plugins);
		}
		GenericNetDisplay gnd = new GenericNetDisplay(view, connection, systemObjects, true);
		gnd.setVisible(true);
	}

	private List<SystemObject> getSystemObjects(ClientDavInterface connection) {
		List<SystemObject> systemObjects = new ArrayList<SystemObject>();
		return systemObjects;
	}


	private static Debug _debug;

	private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		public void uncaughtException(Thread t, Throwable e) {
			System.err.println("Laufzeitfehler: Ein Thread hat sich wegen einer Exception beendet:");
			System.err.println(t);
			e.printStackTrace(System.err);
			_debug.error("Laufzeitfehler: " + t + " hat sich wegen einer Exception beendet", e);
			System.exit(1);    // RunGND darf beendet werden!
		}
	}

	@Override
	protected String getApplicationName() {
		return "Kappich Systemberatung - Generische Netzdarstellung";
	}
}
