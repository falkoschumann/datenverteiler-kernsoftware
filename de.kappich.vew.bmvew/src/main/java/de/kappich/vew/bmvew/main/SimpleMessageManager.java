/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.vew.bmvew.
 * 
 * de.kappich.vew.bmvew is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.vew.bmvew is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.vew.bmvew; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package de.kappich.vew.bmvew.main;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Dies ist eine Stellvertreterapplikation für die zukünftige Betriebsmeldungsverwaltung, die sich als Senke für die
 * Betriebsmeldungen anmeldet und die Meldungen als Quelle publiziert. Damit können versandte Betriebsmeldungen auch
 * ohne Betriebsmeldungsverwaltung z.B. von der Bedienung empfangen und dargestellt werden. Zudem können die
 * Betriebsmeldungen auch archiviert werden.<p> Diese Klasse meldet sich als Senke (Objekt:
 * typ.betriebsMeldungsVerwaltung, Attributgruppe: atg.betriebsMeldung, Aspekt: asp.information) für die
 * Betriebsmeldungen an. Die Daten werden in einer Debug-Ausgabe ausgegeben. Die Klasse dient ansonsten nur zum Erzeugen
 * einer positiven Sendesteuerung und schickt den empfangenen Datensatz als Quelle (Objekt:
 * betriebsMeldung.informationsKanal, Attributgruppe: atg.betriebsMeldung, Aspekt: asp.information) wieder heraus.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5084 $
 */
public class SimpleMessageManager implements StandardApplication {
	/**
	 * DebugLogger für Debug-Ausgaben
	 */
	private static Debug _debug;

	/**
	 * Verbindung zum Datenverteiler.
	 */
	private ClientDavInterface _connection;

	/**
	 * Startet die Applikation.
	 *
	 * @param args übergebene Parameter
	 */
	public static void main(String[] args) {
		StandardApplicationRunner.run(new SimpleMessageManager(), args);
	}

	/**
	 * Hier können die übergebenen Parameter ausgewertet werden.
	 *
	 * @param argumentList
	 * @throws Exception
	 */
	public void parseArguments(ArgumentList argumentList) throws Exception {
	}

	/**
	 * Nach Erhalt der Verbindung zum Datenverteiler meldet sich die Applikation als Senke auf die Betriebsmeldungen an.
	 *
	 * @param connection Verbindung zum Datenverteiler
	 * @throws Exception
	 */
	public void initialize(ClientDavInterface connection) throws Exception {
		_debug = Debug.getLogger();
		_connection = connection;
		// Als Senke für die Betriebsmeldungen anmelden
		final DataModel configuration = _connection.getDataModel();
		SystemObject drainObject = _connection.getLocalConfigurationAuthority();
		if (drainObject == null || !drainObject.isOfType("typ.betriebsMeldungsVerwaltung")) {
			final String errorMessage = "Es gibt kein passendes BetriebsMeldungsVerwaltungs-Objekt (typ.betriebsMeldungsVerwaltung). Applikation kann nicht gestartet werden.";
			_debug.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}
		_debug.info("Die Verwaltung nutzt folgendes BetriebsMeldungsVerwaltungsObjekt", drainObject);

		final AttributeGroup atg = configuration.getAttributeGroup("atg.betriebsMeldung");
		final Aspect asp = configuration.getAspect("asp.information");
		final DataDescription dataDescription = new DataDescription(atg, asp);
		final SystemObject sourceObject = configuration.getObject("betriebsMeldung.informationsKanal");
		if (sourceObject == null) {
			final String errorMessage = "Es gibt kein passendes Objekt vom Typ (betriebsMeldung.informationsKanal). Applikation kann nicht gestartet werden.";
			_debug.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}
		final MessageSource source = new MessageSource();
		_connection.subscribeSender(source, sourceObject, dataDescription, SenderRole.source());
		_debug.config("Die Quelle für Betriebsmeldungen ist bereit.");

		_connection.subscribeReceiver(new MessageReceiver(sourceObject, source), drainObject, dataDescription, ReceiveOptions.normal(), ReceiverRole.drain());
		_debug.config("Die Senke für Betriebsmeldungen ist bereit.");
	}

	/**
	 * Diese Klasse ist für die Anmeldung als Senke erforderlich. Sie empfängt vom Datenverteiler die Betriebsmeldungen.
	 */
	private final class MessageReceiver implements ClientReceiverInterface {
		private final SystemObject _sourceObject;
		private final MessageSource _source;      // wird benötigt, falls auf eine Sendesteuerung gewartet werden soll

		public MessageReceiver(SystemObject sourceObject, MessageSource source) {
			_sourceObject = sourceObject;
			_source = source;
		}

		public void update(ResultData results[]) {
			for (int i = 0; i < results.length; i++) {
				ResultData result = results[i];
				final Data data = result.getData();
				if (data != null) {
					try {
						_debug.finest("Betriebsmeldung empfangen", data);
						final ResultData resultData = new ResultData(_sourceObject, result.getDataDescription(), result.getDataTime(), data);
						_connection.sendData(resultData);
					} catch (SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
						sendSubscriptionNotConfirmed.printStackTrace();
					} catch (ConfigurationException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Diese Klasse ist für die Sendesteuerung. Da sich die Applikation als Quelle anmeldet, wird nicht auf eine positive
	 * Sendesteuerung gewartet.
	 */
	private final class MessageSource implements ClientSenderInterface {
		private byte _state = 0;

		public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
			_debug.finest("Änderung der Sendesteuerung", state);
			_state = state;
		}

		public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
			return false;
		}

		public byte getState() {
			return _state;
		}
	}
}
