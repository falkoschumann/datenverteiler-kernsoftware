/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

package de.kappich.pat.gnd.notice;

import de.bsvrz.dav.daf.main.config.ObjectLookup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 */
public class NoticeManager {

	private final Map<SystemObject, Notice> _noticeMap = new HashMap<SystemObject, Notice>();

	private final Preferences _preferences;

	private static final Debug _debug = Debug.getLogger();

	private SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public NoticeManager(final Preferences preferences, final ObjectLookup dataModel) {
		_preferences = preferences;
		try {
			for(String s : _preferences.childrenNames()) {
				SystemObject object = dataModel.getObject(s);
				if(object == null) {
					try {
						long l = Long.parseLong(s);
						object = dataModel.getObject(l);
					}
					catch(NumberFormatException ignored) {
					}
				}
				if(object == null) {
					_debug.info("Es ist eine Notiz zu " + object + " gespeichert, das zugehörige Objekt kann aber nicht gefunden werden.");
				}
				Preferences node = _preferences.node(s);
				String notice = node.get("text", "");
				long creation = node.getLong("creation", System.currentTimeMillis());
				long change = node.getLong("change", System.currentTimeMillis());
				if(!notice.equals("")) {
					_noticeMap.put(object, new Notice(notice, creation, change));
				}
			}
		}
		catch(BackingStoreException e) {
			_debug.warning("Fehler beim Laden der Notizen", e);
		}
	}

	public boolean setNotice(final SystemObject systemObject, final String text) {
		try {
			final String trimmedText = text.trim();
			if(trimmedText.length() == 0) {
				_noticeMap.remove(systemObject);
				return true;
			}
			final Notice notice = getNotice(systemObject);
			return null == _noticeMap.put(systemObject, new Notice(trimmedText, notice.getCreationTime(), System.currentTimeMillis()));
		}
		finally {
			saveNode(systemObject);
		}
	}

	private void saveNode(final SystemObject systemObject) {
		final Notice notice = _noticeMap.get(systemObject);
		try {
			if(notice == null) {
				_preferences.node(systemObject.getPidOrId()).removeNode();
			}
			else {
				final Preferences node = _preferences.node(systemObject.getPidOrId());
				node.put("text", notice.getMessage());
				node.putLong("creation", notice.getCreationTime());
				node.putLong("change", notice.getChangeTime());
			}
			_preferences.flush();
		}
		catch(BackingStoreException e) {
			_debug.warning("Fehler beim Speichern einer Notiz", e);
		}
	}

	public Collection<SystemObject> getObjectsWithNotice() {
		return Collections.unmodifiableSet(_noticeMap.keySet());
	}

	public Notice getNotice(final SystemObject object) {
		Notice notice = _noticeMap.get(object);
		if(notice == null) {
			notice = new Notice("");
		}
		return notice;
	}

	public boolean hasNotice(final SystemObject systemObject) {
		return _noticeMap.containsKey(systemObject);
	}

	public void exportToFile(final File file) throws IOException {
		final FileWriter fileWriter = new FileWriter(file);
		try {
			fileWriter.write("Objekt;Text;Erstellt;Geändert\n");
			for(final Map.Entry<SystemObject, Notice> noticeEntry : _noticeMap.entrySet()) {
				fileWriter.write(
						'"' + noticeEntry.getKey().getPidOrId() + "\";\"" + noticeEntry.getValue().getMessage().replaceAll("\"", "\"\"") + "\";\""
						+ _simpleDateFormat.format(new Date(noticeEntry.getValue().getCreationTime())) + "\";\""
						+ _simpleDateFormat.format(new Date(noticeEntry.getValue().getChangeTime())) + "\"\n"
				);
			}
		}
		finally {
			fileWriter.close();
		}
	}

	public void clear() {
		try {
			for(String s : _preferences.childrenNames()) {
				_preferences.node(s).removeNode();
			}
			_preferences.flush();
			_noticeMap.clear();
		}
		catch(BackingStoreException e) {
			_debug.warning("Fehler beim Löschen der Notizen", e);
		}
	}
}
