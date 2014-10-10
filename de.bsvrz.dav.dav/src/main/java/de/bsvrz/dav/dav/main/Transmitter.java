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

import de.bsvrz.dav.daf.main.ApplicationCloseActionHandler;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Klasse zum Start des Datenverteilers ohne grafische Oberfläche.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 11549 $
 */
public class Transmitter {
	
	private static Debug _debug;
	
	public final static int _debugLevel = 0;
	
	private final LowLevelConnectionsManagerInterface _connectionsManager;
	
	public Transmitter(String[] args) throws Exception {
		ArgumentList arguments = new ArgumentList(args);
		String debugName = arguments.fetchArgument("-debugName=Datenverteiler").asString();
		// debugname = "" heißt, es handelt sich um einen Datenverteiler der zusammen mit anderem Code im selben Prozess gestartet
		// wird. Hier soll der Debug-Level, Debug-Name usw. nicht überschrieben werden.
		if(debugName.length() > 0){
			Debug.init(debugName, arguments);
		}
		_debug = Debug.getLogger();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
		ServerDavParameters serverDavParameters = new ServerDavParameters(arguments);
		if(arguments.hasArgument("-lockDateienLoeschenImVerzeichnis")) {
			File removeLockFiles = arguments.fetchArgument("-lockDateienLoeschenImVerzeichnis=").asDirectory();
			int tcpPort = serverDavParameters.getApplicationConnectionsSubAddress();
			try {
				Socket socket = new Socket("127.0.0.1", tcpPort);
				socket.close();
				// Verbindung zu einem vorher gestarteten Datenverteiler konnte aufgebaut werden.
				// Also dürfen die Lock-Dateien nicht gelöscht werden und der Datenverteiler wird beendet.
				throw new Exception("Es läuft bereits ein Server mit der Portnummer " + tcpPort + " auf diesem Rechner. Datenverteiler kann nicht gestartet werden.");
			}
			catch(ConnectException e) {
				// Verbindung zu einem vorher gestarteten Datenverteiler konnte nicht aufgebaut werden.
				// Also können eventuell vorhandene Lockdateien gelöscht werden.
				_debug.warning("Lock-Dateien werden gelöscht");
				File[] lockFiles = removeLockFiles.listFiles(
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
		}
		if(debugName.length() > 0) {
			arguments.ensureAllArgumentsUsed();
		}
		_debug.info("Datenverteiler wird gestartet");
		_connectionsManager = new LowLevelConnectionsManager(serverDavParameters);
		_debug.info("Datenverteiler bereit");
		if(_debugLevel > 0) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String inputLine;
			while(true) {
				inputLine = in.readLine();
				if(inputLine == null) {
					// nicht mehr weiter lesen, aber nicht das Programm terminieren
					break;
				}
				else if(inputLine.startsWith("q")) {
					//Programm kontrolliert terminieren
					System.exit(0);
				}
				else if(inputLine.startsWith("s")) {
//					_connectionsManager.printSubscriptions();
				}
			}
		}
	}
	
	LowLevelConnectionsManagerInterface getConnectionsManager() {
		return _connectionsManager;
	}
	
	/**
	 * Setzt den {@link de.bsvrz.dav.daf.main.ApplicationCloseActionHandler}.
	 * 
	 * @param closer
	 *            der CloseActionHandler
	 */
	public void setCloseHandler(ApplicationCloseActionHandler closer) {
		getConnectionsManager().getSelfClientDavConnection().getConnection().setCloseHandler(closer);
	}

	public void shutdown(final boolean error, final String message){
		getConnectionsManager().shutdown(error, message);
	}
	
	/**
	 * Start-Funktion des Datenverteilers.
	 * 
	 * @param arguments
	 *            Aufrufargumente
	 */
	public static void main(String[] arguments) {
		try {
			new Transmitter(arguments);
		}
		catch(Exception e) {
			if(_debug == null) _debug = Debug.getLogger();
			_debug.error("Fehler im Datenverteiler", e);
			e.printStackTrace();
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
