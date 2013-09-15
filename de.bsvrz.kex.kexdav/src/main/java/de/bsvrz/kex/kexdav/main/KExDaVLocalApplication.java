/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.main;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.dataplugin.*;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Main-Klasse, mit der KExDav als alleinstehende Anwendung gestartet werden kann
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9222 $
 */
public class KExDaVLocalApplication implements StandardApplication {

	private static final Pattern COMMAND_LINE_SEPARATOR = Pattern.compile(",");

	private String _kexDavObjectPid = null;

	private File _authenticationFile = null;

	private final Map<AttributeGroupPair, KExDaVDataPlugin> _plugins = new HashMap<AttributeGroupPair, KExDaVDataPlugin>();

	private static final Debug _debug = Debug.getLogger();

	private boolean _exit = false;

	@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
	public void parseArguments(final ArgumentList argumentList) throws Exception {
		try {
			_authenticationFile = argumentList.fetchArgument("-authentifizierung").asExistingFile();

			// verschiedene Schreibweisen des KExDaV-Parameters akzeptieren
			if(argumentList.hasArgument("-kexDaV")) {
				final ArgumentList.Argument argument = argumentList.fetchArgument("-kexDaV");
				_kexDavObjectPid = argument.asString();
			}
			else if(argumentList.hasArgument("-kexdav")) {
				final ArgumentList.Argument argument = argumentList.fetchArgument("-kexdav");
				_kexDavObjectPid = argument.asString();
			}
			else if(argumentList.hasArgument("-KExDaV")) {
				final ArgumentList.Argument argument = argumentList.fetchArgument("-KExDaV");
				_kexDavObjectPid = argument.asString();
			}
			if(argumentList.hasArgument("-plugin")) {
				createPluginMap(argumentList.fetchArgument("-plugin").asString());
			}
			if(argumentList.hasUnusedArguments()) {
				printUsageAndExit();
			}
		}
		catch(IllegalArgumentException e){
			System.out.println(e.getMessage());
			System.out.println();
			printUsageAndExit();
		}

	}

	/**
	 * Schreibt die möglichen Kommandozeilenargumente auf die Standardausgabe
	 */
	@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
	private void printUsageAndExit() {
		System.out.println("Benutzung: java de.bsvrz.kex.kexdav.main.KExDaVLocalApplication [ARGUMENTE]");
		System.out.println("Startet KExDaV");
		System.out.println();
		System.out.println("Erforderliche Argumente:");
		System.out.println("  -benutzer=[Name]            Benutzers zur Authentifizierung mit dem lokalen Datenverteiler");
		System.out.println("  -authentifizierung=[Datei]  Passwortdatei zur Authentifizierung mit dem lokalen und dem Remote-Datenverteiler");
		System.out.println("                                Eine Textdatei mit Einträgen wie:");
		System.out.println("                                LokalerBenutzerName=geheim");
		System.out.println("                                RemoteBenutzerName@dav.remoteDatenverteilerPid=secret");
		System.out.println();
		System.out.println("Optionale Argumente:");
		System.out.println("  -kexDaV=[SystemObjektPid]   Pid des KExDaV-Objektes von dem die Parameter geladen werden sollen");
		System.out.println("                                Falls keine Angabe erfolgt, wird der lokale Konfigurationsverantwortliche benutzt");
		System.out.println("  -plugin=[Plugins]           Kommagetrennte Liste mit Plugins, die Daten konvertieren");
		System.out.println("                                Anzugeben sind Klassen, die das Interface");
		System.out.println("                                de.bsvrz.kex.kexdav.dataplugin.ExternalKExDaVDataPlugin");
		System.out.println("                                implementieren.");
		_exit = true;
	}

	/**
	 * Erstellt die Map mit den Plugins und den zugehörigen Attributgruppen
	 * @param plugins String von der Kommandozeile, der geparst werden muss
	 */
	private void createPluginMap(final CharSequence plugins) {
		final String[] split = COMMAND_LINE_SEPARATOR.split(plugins);
		for(final String s : split) {
			addPlugIn(s.trim());
		}
	}

	/**
	 * Fügt ein Plugin hinzu
	 * @param pluginClassName Plugin-Klasse
	 */
	private void addPlugIn(final String pluginClassName) {
		if(pluginClassName.length() == 0) return;
		try {
			final Class<?> aClass = Class.forName(pluginClassName);
			final Class<? extends ExternalKExDaVDataPlugin> pluginClass = aClass.asSubclass(ExternalKExDaVDataPlugin.class);
			final ExternalKExDaVDataPlugin plugin = pluginClass.newInstance();
			final Collection<AttributeGroupPair> attributeGroupPairs = plugin.getAttributeGroupPairs();
			for(final AttributeGroupPair pair : attributeGroupPairs) {
				final KExDaVDataPlugin old = _plugins.put(pair, plugin);
				if(old != null) {
					_debug.warning("Mehrere Plugins sind für die " + pair + " zuständig: \n" + old.getClass().getName() + "\n" + plugin.getClass().getName());
				}
			}
		}
		catch(Exception e) {
			_debug.error("Konnte das Plugin " + pluginClassName + " nicht laden", e);
		}
	}


	public void initialize(final ClientDavInterface connection) throws Exception {
		if(_exit){
			System.exit(1);
		}

		final SystemObject kexDavObject;
		if(_kexDavObjectPid == null) {
			kexDavObject = connection.getLocalConfigurationAuthority();
		}
		else {
			kexDavObject = connection.getDataModel().getObject(_kexDavObjectPid);
		}
		if(kexDavObject == null) {
			throw new IllegalArgumentException("Kann das angegebene Objekt nicht finden: " + _kexDavObjectPid);
		}
		if(!kexDavObject.isOfType(Constants.Pids.TypeKExDaV)) {
			throw new IllegalArgumentException("Objekt ist nicht vom Typ " + Constants.Pids.TypeKExDaV + ": " + kexDavObject);
		}
		new KExDaV(connection, kexDavObject, _authenticationFile, _plugins).start();
	}

	/**
	 * Methode, die beim Starten der Anwendung gestartet wird
	 *
	 * @param args Argumente
	 */
	public static void main(final String[] args) {
		StandardApplicationRunner.run(new KExDaVLocalApplication(), args);
	}

	/**
	 * Gibt die Pid des KExDaV-Objektes zurück
	 * @return Pid des KExDaV-Objektes
	 */
	public String getKexDavObjectPid() {
		return _kexDavObjectPid;
	}

	/**
	 * Gibt die passwd-Datei zurück
	 * @return passwd-Datei
	 */
	public File getAuthenticationFile() {
		return _authenticationFile;
	}

	/**
	 * Gibt eine unveränderliche Map mit Plugins zurück
	 * @return Map mit Plugins
	 */
	public Map<AttributeGroupPair, KExDaVDataPlugin> getPlugins() {
		return Collections.unmodifiableMap(_plugins);
	}

	/**
	 * Gibt zurück ob die Argumente gültig sind und KExDaV beim Aufruf der {@link #initialize(de.bsvrz.dav.daf.main.ClientDavInterface)}-Methode gestartet wird.
	 * @return true wenn die Argumente gültig sind
	 */
	public boolean hasValidArguments() {
		return !_exit;
	}

	@Override
	public String toString() {
		return "KExDaVLocalApplication{" + "_kexDavObjectPid='" + _kexDavObjectPid + '\'' + ", _authenticationFile=" + _authenticationFile + '}';
	}
}
