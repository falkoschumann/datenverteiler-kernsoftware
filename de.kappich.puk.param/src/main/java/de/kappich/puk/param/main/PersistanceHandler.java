/**
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

import de.bsvrz.sys.funclib.dataSerializer.Deserializer;
import de.bsvrz.sys.funclib.dataSerializer.Serializer;
import de.bsvrz.sys.funclib.dataSerializer.SerializingFactory;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Ermöglicht, {@link de.bsvrz.dav.daf.main.ResultData} zu einer {@link de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification} persistent zu
 * schreiben und die zu {@link de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification} gespeicherten Datensätze komplett zu lesen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5058 $
 */

public final class PersistanceHandler {

	/**
	 * DebugLogger für Debug-Ausgaben
	 */
	private static final Debug debug = Debug.getLogger();

	private final DataIdentification _dataIdentification;
	private final File _persistantData;
	private Serializer _serializer;
	private Deserializer _deserializer;

	/**
	 * Erzeugt ein Objekt vom Typ PersistanceHandler
	 *
	 * @param dataIdentification {@link de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification}, für die Datensätze persistent verwaltet werden sollen.
	 * @param persistantData	 Dateispezifikation, in der die Daten persistent gespeichert werden.
	 */
	PersistanceHandler(final DataIdentification dataIdentification, final File persistantData) {
		_dataIdentification = dataIdentification;
		_persistantData = persistantData;
	}

	/**
	 * Speichert die Daten persistent. Der Datensatz wird angehängt, bisher geschriebene Datensätze bleiben erhalten.
	 *
	 * @param result Ergebnisdatensatz, der gespeichert werden soll.
	 */
	public void makeDataPersistance(final ResultData result) {
		makeDataPersistance(result, true);
	}

	/**
	 * Speichert die Daten persistent. Der Datensatz wird angehängt, bisher geschriebene Datensätze bleiben erhalten.
	 *
	 * @param result Ergebnisdatensatz, der gespeichert werden soll.
	 * @param append Legt fest, ob die Daten angehängt werden
	 *               <p/>
	 *               (<code>true</code>: Daten werden angehängt, entspricht dann der Methode {@link
	 *               #makeDataPersistance(de.bsvrz.dav.daf.main.ResultData)})
	 *               <p/>
	 *               (<code>false</code>: Datensatz überschreibt aktuelle Einträge. Es wird also nur dieser eine Datensatz persistent
	 *               gehalten.
	 */

	public void makeDataPersistance(final ResultData result, final boolean append) {

		try {
			final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(_persistantData, append));
			_serializer = SerializingFactory.createSerializer(out);
			_serializer.writeData(result.getData());
			out.close();

		}
		catch (FileNotFoundException e) {
			debug.error("Datei " + _persistantData + " existiert nicht");
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			debug.error("I/O-Fehler beim Versuch, auf die Persistenzdatei für DatenIdentifikation "
					+ _dataIdentification + " zu schreiben.");
			throw new RuntimeException(e);
		}
	}

	/**
	 * Liefert die persistent gespeicherten Daten.
	 *
	 * @return Array mit Ergebnisdaten der persistenten Daten für diese {@link de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification}.
	 */
	public ResultData[] getPersistanceData() {

		try {
			final BufferedInputStream in = new BufferedInputStream(new FileInputStream(_persistantData));
			final HashSet<ResultData> resultSet = new HashSet<ResultData>();
			Data data;
			ResultData result;

			final AttributeGroup atg = _dataIdentification.getDataDescription().getAttributeGroup();
			_deserializer = SerializingFactory.createDeserializer(in);

			try {
				while (in.available() > 0) {

					data = _deserializer.readData(atg);

					result = new ResultData(_dataIdentification.getObject(),
							_dataIdentification.getDataDescription(),
							System.currentTimeMillis(),
							data);

					resultSet.add(result);
				}
			}
			catch (Exception ex) {
				final String errorMessage = "I/O-Fehler beim Versuch, die Persistenzdatei für DatenIdentifikation "
						+ _dataIdentification + " zu lesen. Die dazugehörige Datei wird umbenannt in '"
						+ _persistantData.getName() + ".old'";
				debug.warning(errorMessage, ex);
				// Datei umbenennen
				final File oldFile = new File(_persistantData.getParent(), _persistantData.getName() + ".old");
				in.close();	// erst Stream schließen, damit die Datei umbenannt werden kann!
				_persistantData.renameTo(oldFile);
			} finally {
				// InputStream muss geschlossen werden
				in.close();
			}
			final ResultData[] results = new ResultData[resultSet.size()];
			final Iterator iterator = resultSet.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				results[i++] = (ResultData) iterator.next();
			}

			return results;
		}

		catch (FileNotFoundException e) {
			debug.error("Datei " + _persistantData + " existiert nicht");
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			debug.error("Fehlerhafte Parametrierungsdatei (Struktur ATG geändert?): " + _persistantData.getName());
			throw new RuntimeException(e);
		}
	}
}

