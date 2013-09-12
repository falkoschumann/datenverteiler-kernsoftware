/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2006 by Kappich+Kniﬂ Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.pat.configBrowser.
 * 
 * de.kappich.pat.configBrowser is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.configBrowser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.configBrowser; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.pat.configBrowser.main;

import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.puk.config.configFile.datamodel.ConfigDataModel;

import java.io.File;

/**
 * Dieser Konfigurations-Browser stellt das {@link de.bsvrz.puk.config.configFile.datamodel.ConfigDataModel neue Datenmodell} grafisch aufbereitet
 * dar.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5074 $
 */
public class ConfigConfigurationViewer {
	private static Debug _debug;

	public static void main(String[] args) {
		ArgumentList argumentList = new ArgumentList(args);
		Debug.init("KonfigurationsBrowserNeu", argumentList);
		_debug = Debug.getLogger();
		File configurationFile;
		if (argumentList.hasArgument("-konfiguration")) {
			configurationFile = argumentList.fetchArgument("-konfiguration").asFile();
		} else {
			throw new IllegalArgumentException("Die Verwaltungsdatei der Konfiguration wurde nicht angegeben (-konfiguration=...).");
		}
		new ConfigConfigurationViewer(configurationFile);
	}

	public ConfigConfigurationViewer(File configurationFile) {
		final ConfigDataModel configuration = new ConfigDataModel(configurationFile);
		_debug.info("Das Datenmodell wurde geladen!");
		final Runnable autoCloser = new Runnable() {
			public void run() {
				configuration.close();
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(autoCloser));

		DataModelTreePanel.showTreeFrame(configuration);
		_debug.info("fertig");
		System.out.println("fertig");     // falls eins nicht erscheint
	}


}
