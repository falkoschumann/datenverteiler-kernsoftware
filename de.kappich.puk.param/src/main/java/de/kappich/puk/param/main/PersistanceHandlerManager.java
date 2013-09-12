/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.puk.param.
 * 
 * de.kappich.puk.param is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.puk.param is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.puk.param; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.puk.param.main;

import de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Verwaltet {@link PersistanceHandler} Objekte
 *
 * @author Kappich Systemberatung
 * @version $Revision: 6228 $
 */

public final class PersistanceHandlerManager {

	/**
	 * DebugLogger für Debug-Ausgaben
	 */
	private static final Debug debug = Debug.getLogger();

	private final File _path;
	private Hashtable _handler;
	private DataIdentification _dataIdentification;

	/**
	 * Erzeugt ein Objekt vom Typ PersistanceHandlerManager. Der Manager verwaltet alle {@link PersistanceHandler}, so
	 * dass je {@link de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification} immer nur ein {@link PersistanceHandler} existiert.
	 *
	 * @param path Verzeichnispfad, indem die Daten für die einzelnen Handler persistent gehalten werden.
	 */
	public PersistanceHandlerManager(final File path) {
		_path = path;
		_handler = new Hashtable();
	}

	/**
	 * Gibt für eine DatenIdentifikation einen {@link PersistanceHandler} zurück. Existiert ein solcher Handler noch nicht,
	 * wird er angelegt. Existiert bereits ein Handler, so wird dieser zurückgegeben.
	 *
	 * @param dataIdentification DatenIdentifikation, für die ein PersistanceHandler Objekt zurückgegben wird.
	 *
	 * @return Der {@link PersistanceHandler} für diese DatenIdentifikation.
	 */
	public PersistanceHandler getHandler(final DataIdentification dataIdentification) {
		_dataIdentification = dataIdentification;

		if (!_handler.containsKey(_dataIdentification)) {
			try {
				if (!_path.exists()) {
					if (!_path.mkdir()) {
						debug.error("Anlegen der Persistenzdatei für DatenIdentifikation " + _dataIdentification +
						            " fehlgeschlagen, da Verzeichnis [" + _path + "] nicht angelegt werden konnte!");
					}
				}

				final String originalName = fileName();
				final String asciiFileName = replaceSpecialCharacters(originalName);
				final File persistantData = new File(_path, asciiFileName);
				if (!persistantData.exists()) {
					boolean renamed = false;
					// Hier wird kein equals() gebraucht, da replaceSpecialCharacters das selbe Objekt zurück gibt, wenn kein Zeichen ersetzt wurde.
					if(asciiFileName != originalName) {
						// Name hat Umlaute enthalten, eventuell vorhandene Datei mit Umlauten wird umbenannt
						final File badPersistantData = new File(_path, originalName);
						if(badPersistantData.exists()) {
							debug.info("Umbenennen der Parameterdatei von '" + originalName + "' nach '" + asciiFileName + "'");
							renamed = badPersistantData.renameTo(persistantData);
							if(!renamed) debug.warning("Umbenennen der Parameterdatei von '" + originalName + "' nach '" + asciiFileName + "' hat nicht funktioniert");
						}

					}
					// Wenn Datei nicht umbenannt wurde, wird sie angelegt
					if (!renamed) {
						if (!persistantData.createNewFile()) {
							debug.error("Anlegen der Persistenzdatei für DatenIdentifikation " + _dataIdentification +
							            " fehlgeschlagen, da Datei [" + persistantData + "] nicht angelegt werden konnte!");
						}
					}
				}
				final PersistanceHandler persistanceHandler = new PersistanceHandler(_dataIdentification, persistantData);
				_handler.put(_dataIdentification, persistanceHandler);
			}
			catch (IOException e) {
				debug.error("I/O-Fehler beim Versuch, die Persistenzdatei für DatenidenDatenIdentifikation "
				            + dataIdentification + " anzulegen.");
				throw new RuntimeException(e);
			}
		}
		return (PersistanceHandler) _handler.get(dataIdentification);
	}

//	private String fileName() {
//		final String fileName =
//		        _dataIdentification.getDataDescription().getSimulationVariant() + "_" +
//		        _dataIdentification.getObject().getPid() + "_" +
//		        _dataIdentification.getDataDescription().getAttributeGroup().getPid() + "_" +
//		        _dataIdentification.getDataDescription().getAspect().getPid() + ".param";
//
//		return fileName;
//	}

	private String fileName() {
		final String fileName =
		        _dataIdentification.getDataDescription().getSimulationVariant() + "_" +
		        _dataIdentification.getObject().getPid() + "_" +
		        _dataIdentification.getDataDescription().getAttributeGroup().getPid() + "_" +
		        _dataIdentification.getDataDescription().getAspect().getPid() + "_" +
		        _dataIdentification.getObject().getId() + "_" +
		        _dataIdentification.getDataDescription().getAttributeGroup().getId() + "_" +
		        _dataIdentification.getDataDescription().getAspect().getId() +
		        ".param";

		return fileName;
	}

	/**
	 * Ersetzt Umlaute und andere problematische Zeichen wie z.B. '/' im übergebenen String durch ASCII-Entsprechungen
	 *
	 * @param text Zu ersetzender Text
	 * @return Wenn keine Umlaute oder andere problematische Zeichen in <<code>text</code> enthalten sind, wird <<code>text</code> zurückgegeben (das selbe
	 *         Objekt), ansonsten wird ein neuer String zurückgegeben in dem die Umlaute durch die üblichen
	 *         ASCII-Entsprechungen ersetzt wurden.
	 */
	private String replaceSpecialCharacters(String text) {
		char[] chars = text.toCharArray();
		StringBuilder textBuilder = new StringBuilder(text.length());
		boolean modified = false;
		for(char c : chars) {
			switch(c) {
			case '\u00E4':
				textBuilder.append("ae");
				modified = true;
				break;
			case '\u00F6':
				textBuilder.append("oe");
				modified = true;
				break;
			case '\u00FC':
				textBuilder.append("ue");
				modified = true;
				break;
			case '\u00DF':
				textBuilder.append("ss");
				modified = true;
				break;
			case '\u00C4':
				textBuilder.append("Ae");
				modified = true;
				break;
			case '\u00D6':
				textBuilder.append("Oe");
				modified = true;
				break;
			case '\u00DC':
				textBuilder.append("Ue");
				modified = true;
				break;
			case '/':
				textBuilder.append("SLASH");
				modified = true;
				break;
			case '\\':
				textBuilder.append("BACKSLASH");
				modified = true;
				break;
			case ':':
				textBuilder.append("COLON");
				modified = true;
				break;
			default:
				textBuilder.append(c);
				break;
			}
		}
		if(modified) {
			return textBuilder.toString();
		} else {
			return text;
		}
	}

}
