/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.samples.operatingMessage.
 * 
 * de.kappich.samples.operatingMessage is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.samples.operatingMessage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.samples.operatingMessage; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package de.kappich.samples.operatingMessage.main;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.operatingMessage.MessageSender;
import de.bsvrz.sys.funclib.operatingMessage.MessageType;
import de.bsvrz.sys.funclib.operatingMessage.MessageGrade;
import de.bsvrz.sys.funclib.operatingMessage.MessageState;
import de.bsvrz.sys.funclib.operatingMessage.MessageCauser;

/**
 * Diese Applikation dient als Testapplikation für das Versenden von {@link de.bsvrz.sys.funclib.operatingMessage.MessageSender Betriebsmeldungen} und dem
 * Test der {@link de.kappich.vew.bmvew.main.SimpleMessageManager Betriebsmeldungsverwaltung}. Das Versenden
 * von Betriebsmeldungen ähnelt der Benutzung der Klasse {@link Debug}.
 * <p/>
 * Im {@link StandardApplicationRunner} wird der {@link de.bsvrz.sys.funclib.operatingMessage.MessageSender} initialisiert. Damit nun Betriebsmeldungen
 * abgesetzt werden können, muss als erstes eines Instanz des MessageSenders geholt werden. Damit können nun
 * Betriebsmeldungen mit der Methode {@link de.bsvrz.sys.funclib.operatingMessage.MessageSender#sendMessage} übertragen werden. Damit nicht immer alle
 * Parameter gesetzt werden müssen, gibt es diese Methode in verschiedenen Versionen.
 *
 * @author Kappich Systemberatung
 * @version $Revision:5019 $
 */
public class OperatingMessageSampleApplication implements StandardApplication {
	/**
	 * DebugLogger für Debug-Ausgaben
	 */
	private static Debug _debug;

	/**
	 * Betriebsmeldungs-Objekt zum Senden von Betriebsmeldungen
	 */
	private static MessageSender _ms;


	public static void main(String[] args) {
		// Initialisierung von Debug und des MessageSenders.
		// Beim MessageSender werden die Attribute ApplikationsID, LaufendeNummer,
		// ApplikationsTyp und die ApplikationsKennung gesetzt bzw. initialisiert.
		StandardApplicationRunner.run(new OperatingMessageSampleApplication(), args);
	}

	public void parseArguments(ArgumentList argumentList) throws Exception {
	}

	public void initialize(ClientDavInterface connection) throws Exception {
		_debug = Debug.getLogger();
		_ms = MessageSender.getInstance(); // Die Instanz des MessageSenders holen.

		
		// Mit der Methode setApplicationLabel() kann eine neue eindeutige Applikationskennung gesetzt werden. Z.B.:
		//_ms.setApplicationLabel("Neue eindeutige ApplikationsKennung");


		_debug.finer("1. Betriebsmeldung wird verschickt");
		// Die einfachste Betriebsmeldung erfordert den Meldungstyp, die Meldungsklasse und den Meldungstext.
		// Die anderen Attribute erhalten folgende Werte:
		// 	ID = ""
		// 	Referenz = null
		// Gutmeldung = false
		// Urlasser = (angemeldeter Benutzer, keine Ursache, kein Veranlasser)
		_ms.sendMessage(MessageType.APPLICATION_DOMAIN, MessageGrade.ERROR, "Fehler beim Objekt (id: 12345)");

		_debug.finer("2. Betriebsmeldung wird verschickt");
		// siehe obige Betriebsmeldung
		_ms.sendMessage(MessageType.SYSTEM_DOMAIN, MessageGrade.INFORMATION, "Test-Fehlermeldung");

		_debug.finer("3. Betriebsmeldung wird verschickt");
		// Diese Betriebsmeldung setzt außerdem die ID der Meldung und dass es sich um eine Gutmeldung handelt.
		// Alle anderen Werte werden wie oben beschrieben gesetzt.
		// Wird der MeldungsTypZusatz nicht gesetzt (-> ""), dann wird die Aufrufposition der Betriebsmeldung ermittelt (Klassenname, Methode, Zeilennummer).
		_ms.sendMessage("123", MessageType.SYSTEM_DOMAIN, "", MessageGrade.WARNING, MessageState.GOOD_MESSAGE, "Eine Gutmeldung!");

		_debug.finer("4. Betriebsmeldung wird verschickt");
		// Bei dieser Betriebsmeldung werden alle möglichen Parameter übergeben.
		// ID 				 = "id-12345"
		// MeldungsTyp 		 = MessageType.SYSTEM -> System
		// MeldungsTypZusatz = "4. Betriebsmeldung in der Beispielapplikation"
		// Meldungsklasse 	 = MessageGrade.FATAL -> Fatal
		// Referenz			 = connection.getLocalUser() -> Referenz auf das Benutzerobjekt
		// Gutmeldung		 = MessageState.NEW_MESSAGE -> "Neue Meldung"
		// Urlasser 		 = MessageCauser(connection.getLocalUser(), "", "") -> (Benutzer, keine Ursache, keinen Veranlasser)
		// Meldungstext		 = "Vollständige Betriebsmeldung"
		final MessageCauser messageCauser = new MessageCauser(connection.getLocalUser(), "", "");	// der Urlasser
		_ms.sendMessage("id-12345", MessageType.SYSTEM_DOMAIN, "4. Betriebsmeldung in der Beispielapplikation", MessageGrade.FATAL, connection.getLocalUser(), MessageState.NEW_MESSAGE, messageCauser, "Vollständige Betriebsmeldung");
	}
}
