/*
  * Copyright 2010 by Kappich
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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.configObjectAcquisition.ConfigurationHelper;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.List;

/**
 * Stoppt den Datenverteiler- bzw. Applikation-Verbindung mit der übergebene Objekt Spezifikation.
 * -objekt ID oder IDs durch Kommata getrennt, ist die Angabe welche Prozesse Terminiert werden soll.
 * Bsp : "-objekt=1466766103639706224 -benutzer=Tester -authentifizierung=passwd -debugLevelStdErrText=INFO"
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8582 $
 */
public class TerminateConnection implements StandardApplication {

	private static Debug _debug;
	private String _objectSpec;

	public static void main(String[] args) {
		StandardApplicationRunner.run(new TerminateConnection(), args);
		disconnect();
	}

	public void parseArguments(ArgumentList argumentList) throws Exception {
		_debug = Debug.getLogger();
		_debug.fine("argumentList = " + argumentList);
		_objectSpec = argumentList.fetchArgument("-objekt=").asNonEmptyString();
	}

	public void initialize(ClientDavInterface connection) throws Exception {
		List<SystemObject> systemObjectList = ConfigurationHelper.getObjects(_objectSpec, connection.getDataModel());
		sendTerminationData(connection, systemObjectList);
	}

	public static void sendTerminationData(final ClientDavInterface connection, final List<SystemObject> systemObjectList) throws InterruptedException {
		System.out.println("systemObjectList = " + systemObjectList);

		DataModel configuration = connection.getDataModel();
		SystemObject object = connection.getLocalDav();

		_debug.info("Objekt: " + object);
		AttributeGroup atg = configuration.getAttributeGroup("atg.terminierung");
		_debug.info("Attributgruppe: " + atg);
		if(atg == null){
			throw new IllegalStateException("Aktualisieren Sie die Konfiguration der Kernsoftware.\nDie benötigte Attributgruppe (atg.terminierung) ist unbekannt.");
		}

		Aspect aspect = configuration.getAspect("asp.anfrage");
		_debug.info("Aspekt: " + aspect);
		if(aspect == null){
			throw new IllegalStateException("Aktualisieren Sie die Konfiguration der Kernsoftware.\nDer benötigte Aspekt (asp.anfrage) ist unbekannt.");
		}


		Data data = connection.createData(atg);
		Data.ReferenceArray referenceArrayApplication = data.getReferenceArray("Applikationen");
		referenceArrayApplication.setLength(systemObjectList.size());

		Data.ReferenceArray referenceArrayDataDistributor = data.getReferenceArray("Datenverteiler");
		referenceArrayDataDistributor.setLength(systemObjectList.size());

		int application = 0;
		int dataDistributor = 0;

		for(SystemObject systemObject : systemObjectList) {
			if(systemObject == null){
				_debug.error("Die Übergebene Objekt-Spezifikation ist ungültig. Bitte überprüfen Sie die Übergabeparameter(-objekt=...).\nBeachten Sie das nach jedem Neustart der Kernsoftware die bisherigen Objekt-Spezifikation gelöscht werden.\n");
				continue;
			}

			if(systemObject.isOfType("typ.datenverteiler")){
				Data.ReferenceValue referenceValue = referenceArrayDataDistributor.getReferenceValue(dataDistributor++);
				referenceValue.setSystemObject(systemObject);
			}else if(systemObject.isOfType("typ.applikation")){
				Data.ReferenceValue referenceValue = referenceArrayApplication.getReferenceValue(application++);
				referenceValue.setSystemObject(systemObject);
			}else{
				_debug.error("Die Übergebene Objekt-Spezifikation ist ungültig, es sind nur Objekte für den Typ typ.datenverteiler und typ.applikation definiert. Bitte überprüfen Sie die Übergabeparameter(-objekt=...).\nBeachten Sie das nach jedem Neustart der Kernsoftware die bisherigen Objekt-Spezifikation gelöscht werden.\n");
				continue;
			}
		}

		referenceArrayApplication.setLength(application);
		referenceArrayDataDistributor.setLength(dataDistributor);

		DataDescription dataDescription = new DataDescription(atg, aspect);
		ResultData dataTel = new ResultData(object, dataDescription, System.currentTimeMillis(), data);

		final Transmitter sender = new Transmitter();
		try {
			connection.subscribeSender(sender, object, dataDescription, SenderRole.sender());
			Thread.sleep(1000);
			System.out.println("Verbindung zum Datenverteiler wird angemeldet.");
		}
		catch(OneSubscriptionPerSendData e) {
			System.out.println("Datenidentifikation ist bereits angemeldet.");
		}

		try {
			System.out.println("Gewählte Applikation-/Datenverteilerverbindung wird terminiert...");
			connection.sendData(dataTel);
		}
		catch(Exception e) {
			System.out.println("Fehler: " + e.getMessage());
		}
		finally {
			connection.unsubscribeSender(sender, object, dataDescription);
		}
	}

	public static void disconnect() {
		System.out.println("Die Verbindung zum Datenverteiler wird nun getrennt.");
	}

	private static class Transmitter implements ClientSenderInterface {

		public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
			//System.out.println("state = " + state);
		}

		public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
			return false;
		}
	}
}
