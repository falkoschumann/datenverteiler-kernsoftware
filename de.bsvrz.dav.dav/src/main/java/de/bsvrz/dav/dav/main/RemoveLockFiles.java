/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * 
 * This file is part of de.bsvrz.dav.dav.
 * 
 * de.bsvrz.dav.dav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.dav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dav.dav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.dav.dav.main;

import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.File;
import java.io.FilenameFilter;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Klasse zum Löschen von vergessenen Lockdatei
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class RemoveLockFiles {

	private static Debug _debug;

	public static void checkAndRemoveLockFiles(ArgumentList arguments) throws Exception {
		//ServerDavParameters serverDavParameters = new ServerDavParameters(arguments);
		int davPort = arguments.fetchArgument("-davAppPort=8083").intValueBetween(0, 65535);
		int arsPort = arguments.fetchArgument("-arsPort=4242").intValueBetween(0, 65535);

		File configDirectory = null;
		if(arguments.hasArgument("-konfiguration")) {
			configDirectory = arguments.fetchArgument("-konfiguration").asDirectory();
		}
		File arsDirectory = null;
		if(arguments.hasArgument("-archiv")) {
			arsDirectory = arguments.fetchArgument("-archiv").asDirectory();
		}
		arguments.ensureAllArgumentsUsed();

		if(davPort != 0 && isServerRunning("127.0.0.1", davPort)) {
			// Verbindung zu einem vorher gestarteten Datenverteiler konnte aufgebaut werden.
			// Also dürfen die Lock-Dateien nicht gelöscht werden und der Datenverteiler wird beendet.
			throw new Exception("Es läuft bereits ein Datenverteiler auf der Portnummer " + davPort + " auf diesem Rechner.");
		}
		if(arsDirectory!= null && arsPort != 0 && isServerRunning("127.0.0.1", arsPort)) {
			// Verbindung zu einem vorher gestarteten Datenverteiler konnte aufgebaut werden.
			// Also dürfen die Lock-Dateien nicht gelöscht werden und der Datenverteiler wird beendet.
			throw new Exception("Es läuft bereits ein Archivsystem auf der Portnummer " + arsPort + " auf diesem Rechner.");
		}

		if(configDirectory != null) {
			_debug.info("Lock-Dateien der Konfiguration werden gelöscht");
			File[] lockFiles = configDirectory.listFiles(
					new FilenameFilter() {
						@Override
						public boolean accept(final File dir, final String name) {
							return name.endsWith(".lock");
						}
					}
			);
			for(File lockFile : lockFiles) {
				// Zur Sicherheit werden nur leere Dateien gelöscht
				if(lockFile.isFile() && lockFile.length() == 0) {
					_debug.warning("- Lock-Datei " + lockFile.getCanonicalPath() + " wird gelöscht");
					lockFile.delete();
				}
			}
		}
		if(arsDirectory != null) {
			_debug.info("Lock-Datei des Archivsystems wird gelöscht");
			File lockFile = new File(arsDirectory, "_isActive.flag");
			if(lockFile.isFile() && lockFile.length() == 0) {
				_debug.warning("- Lock-Datei " + lockFile.getCanonicalPath() + " wird gelöscht");
				lockFile.delete();
			}
		}
	}

	private static boolean isServerRunning(String host, final int tcpPort) throws Exception {
		try {
			Socket socket = new Socket(host, tcpPort);
			socket.close();
			return true;
		}
		catch(ConnectException e) {
			return false;
		}
	}

	/**
	 * Start-Funktion des Datenverteilers.
	 *
	 * @param args Aufrufargumente
	 */
	public static void main(String[] args) {
		try {
			ArgumentList arguments = new ArgumentList(args);
			Debug.init("RemoveLockFiles", arguments);
			_debug = Debug.getLogger();
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
			checkAndRemoveLockFiles(arguments);
			System.exit(0);
		}
		catch(Exception e) {
			System.out.println("Fehler beim Löschen der Lockdateien: " + e);
			System.exit(1);
		}
	}

	/**
	 * Implementierung eines UncaughtExceptionHandlers, der bei nicht abgefangenen Exceptions und Errors entsprechende Ausgaben macht und im Falle eines Errors den
	 * Prozess terminiert.
	 */
	private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		/** Speicherreserve, die freigegeben wird, wenn ein Error auftritt, damit die Ausgaben nach einem OutOfMemoryError funktionieren */
		private volatile byte[] _reserve = new byte[20000];

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if(e instanceof Error) {
				// Speicherreserve freigeben, damit die Ausgaben nach einem OutOfMemoryError funktionieren
				_reserve = null;
				try {
					System.err.println("Schwerwiegender Laufzeitfehler: Ein Thread hat sich wegen eines Errors beendet, Prozess wird terminiert");
					System.err.println(t);
					e.printStackTrace(System.err);
					_debug.error("Schwerwiegender Laufzeitfehler: " + t + " hat sich wegen eines Errors beendet, Prozess wird terminiert", e);
				}
				catch(Throwable ignored) {
					// Weitere Fehler während der Ausgaben werden ignoriert, damit folgendes exit() auf jeden Fall ausgeführt wird.
				}
				System.exit(1);
			}
			else {
				System.err.println("Laufzeitfehler: Ein Thread hat sich wegen einer Exception beendet:");
				System.err.println(t);
				e.printStackTrace(System.err);
				_debug.error("Laufzeitfehler: " + t + " hat sich wegen einer Exception beendet", e);
			}
		}
	}
}
