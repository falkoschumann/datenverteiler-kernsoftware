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

import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataNotSubscribedException;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification;
import de.bsvrz.sys.funclib.dataIdentificationSettings.SettingsManager;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.File;
import java.util.*;

/**
 * Diese Klasse implementiert die Parametrierung, die sich über Aufrufargumente
 * gesteuert auf beliebige Vorgabe-Parameter-Daten des Systems anmelden kann,
 * und diese als Soll-Parameter publiziert.
 * <p/>
 * Datenfluss:
 * <p/>
 * Ein zu verwendender Paramtersatz wird (i.d.R. durch die Bedienung) als
 * Vorgabe-Parameter versandt und von der Paramterierung (die darauf als Senke
 * angemeldet ist) als Soll-Paramter publiziert. Der Soll-Parameter wird von den
 * zu parametrierenden Applikationen verwendet. Eine solche Applikation hat
 * zudem die Möglichkeit (wird i.d.R. aber nicht verwendet) den von ihr
 * tatsächlichen benutzten Paramtersatz als Ist-Parameter zu publizieren.
 * <p/>
 * Bespiel: Bedienung setzt Vorgabe-Parameter für Erfassungszyklus auf Wert 1,1
 * Minuten. Parameterierung publiziert dies an alle angemeldeten Applikationen
 * als Soll-Parameter. Die TLS-Applikation (als ein Abnehmer) sendet diesen Wert
 * an die Streckenstation. Da dort keine 1,1 Minuten eingestellt werden können,
 * meldet die Streckenstation als eingestellten Wert 1 Minute zurück. Die
 * TLS-Applikation publiziert daraufhin als Ist-Paramter den Wert 1-Minute. In
 * der Bedienung könnte z.B. über eine Gegenüberstellung von Vorgabe-, Soll- und
 * Ist-Parameter auf diese Abweichung hingewiesen werden.
 * <p/>
 * Aufruf:
 * <p/>
 * <code><pre>
 * java de.kappich.puk.param.main.ParamApp -parameterPfad=ParameterPfad -parametrierung=Pid
 * </code></pre>
 * mit:
 * <code><pre>
 * ParameterPfad: Pfadangabe, in der die Parameterdaten persistent gehalten
 * werden.
 *                Default: "./parameter".
 *                Der spezifizierte Zielordner muss existieren!
 * <p/>
 * Pid:           PID des Parametrierungsobjekts (typ.parametrierung) an dem
 * die
 *                Parameter für diese Parametrierung verwaltet werden.
 *                Default: parametrierung.global
 *                Die Beschreibung der Parameterattributgruppe siehe DatKat,
 * atg.parametrierung.
 * <p/>
 * </code></pre>
 * <p/>
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9144 $
 */
public class ParamApp implements ClientSenderInterface, ClientReceiverInterface {

	/**
	 * Die Pid des Aspekts, mit dem Parameter (z.B. durch die Bedienung) vorgegeben
	 * werden. Dieser Aspekt wird von der Parametrierung empfangen und dann mit dem
	 * Aspekt {@link #ASP_OUTPUT} publiziert werden.
	 */
	static final String ASP_INPUT = "asp.parameterVorgabe";
	/**
	 * Die Pid des Aspekts, mit dem die durch die Parametrierung verwalteten
	 * Parameter von der Parametrierung publiziert werden, nachdem sie (z.B. durch
	 * die Bedienung) vorgegeben wurden (siehe auch {@link #ASP_INPUT} .
	 */
	static final String ASP_OUTPUT = "asp.parameterSoll";
	/**
	 * ManagerObjekt, der die PersistenceHandler für die Paramtersätze verwaltet.
	 */
	private PersistanceHandlerManager _persistanceHandlerManager;
	/**
	 * Tabelle mit den aktuell verwalteten PersistenceHandlern. Als Schlüssel dient
	 * die DatenIdentifikation ({@link DataIdentification}). Damit kann auf die
	 * persistenten Parametersätze für eine DatanIdentifikation zugegriffen werden
	 * ({@link PersistanceHandlerManager} ).
	 */
	private Hashtable _paramObjects = new Hashtable();
	/**
	 * Verzeichnispfad, in dem die Parametersätze persistent gespeichert werden.
	 * Das Verzeichnis wird als Aufrufparameter übergeben und muss bereits
	 * existieren.
	 */
	private static File _paramPath;
	/**
	 * Pid des Parametrierungsobjekts mit den Parametern, über die sich die
	 * Parametrierung parametrieren läßt.
	 */
	private static String _pidParametrierung;
	/**
	 * Das Verbindungsobjekt zum Datenverteiler.
	 */
	private static ClientDavInterface _connection;
	/**
	 * Der Aspekt, mit dem die Parametersätze durch die Parametrierung publiziert
	 * werden.
	 */
	private Aspect _outputAspect;
	/**
	 * Der Aspekt, mit dem die Parametersätze von der Parametrierung empfangen
	 * werden.
	 */
	private Aspect _inputAspect;
	/**
	 * Zugriff auf die aktuelle Konfiguration.
	 */
	private DataModel _config;
	/**
	 * Aktueller DebugLogger.
	 */
	private static Debug _debug;

	/**
	 * Konstruiert ein ParamApp Objekt. Bei der Konstruktion werden folgende
	 * Schritte durchgeführt: <ul> <li>Anmeldung auf den Parametersatz
	 * <code>atg.parametrierung</code> des als Aufrufparameter übergebenen
	 * ParamApp-Objekts.</li> <li>Anmeldung beim {@link de.bsvrz.sys.funclib.dataIdentificationSettings.SettingsManager}
	 * zur Verarbeitung geänderter Parameterzuständigkeiten. </li> <li>Versendung
	 * des persistenten Parametersatzes <code>atg.parametrierung</code> der
	 * Parametrierung</li> </ul>
	 * <p/>
	 * Damit empfängt die Parametrierung als erstes "ihren" Parametersatz und
	 * wertet ihn aus, wodurch dann alle zu parametrierenden Datensätze angemeldet,
	 * eventuell vorhande persistente Daten versandt und geänderte Vorgaben
	 * verarbeitet werden (d.h. die Parameterierung zieht sich so beim Start selbst
	 * aus dem Sumpf.
	 */
	public ParamApp() {
		try {
			_config = _connection.getDataModel();
			_outputAspect = _config.getAspect(ASP_OUTPUT);
			_inputAspect = _config.getAspect(ASP_INPUT);

			// Zugriffsrechte für diverse User werden nicht mehr automatisch auf Vollzugriff eingestellt, da dies bei
			// Verwendung mehrerer Parametrierungen problematisch ist.
			// Die Zugriffsrechte müssen jetzt korrekt parametriert werden.
			// Alternativ kann die Rechteprüfung im Datenverteiler mit dem Aufrufparameter -rechtePruefung=nein abgeschaltet werden.
			//setStandardUserAccess();

			// Systemobjekt der Parameterierung ermitteln
			SystemObject paramApp = _config.getObject(_pidParametrierung);
			if (paramApp == null) {
				_debug.error("Angegebenes Parametrierungsobjekt mit der PID [" + _pidParametrierung +
				            "] existiert nicht!");
				System.exit(1);
			}
			// DatenIdentifikation für Parametrierung und Parametersatz der Parameterierung erzeugen
			DataDescription parameterDescriptionOut = new DataDescription(_config.getAttributeGroup("atg.parametrierung"),
			                                                              _outputAspect, (short) 0);
			DataDescription parameterDescriptionIn = new DataDescription(_config.getAttributeGroup("atg.parametrierung"),
			                                                             _inputAspect, (short) 0);
			DataIdentification parameterIdentification = new DataIdentification(paramApp, parameterDescriptionOut);

			// Manager für die PersistanceHandler instanzieren.
			_persistanceHandlerManager = new PersistanceHandlerManager(_paramPath);
			// PrsistenceHandler für diese DatenIdentifikation holen und merken...
			_paramObjects.put(parameterIdentification, _persistanceHandlerManager.getHandler(parameterIdentification));

			// ... und SettingsManager zur Verwaltung dieses Parametersatzes instanzieren.
			SettingsManager settingsManager = new SettingsManager(_connection, parameterIdentification);
			settingsManager.addUpdateListener(new ParameterObserver(this, _connection, paramApp, _paramObjects, _persistanceHandlerManager));
			settingsManager.start();

			// Parametersatz zum Senden anmelden (Parametrierung verwaltet auch ihre eigenen Paramter)
			_connection.subscribeSender(this,
			                            paramApp,
			                            parameterDescriptionOut,
			                            SenderRole.source());

			// Persistenten Parametersatz versenden (und damit schaukelt sich die Parameterierung dann hoch...)
			ResultData[] results = ((PersistanceHandler) _paramObjects.get(parameterIdentification)).getPersistanceData();
			for(final ResultData resultData : results) {
				if(resultData.getData().isDefined()) {
					_connection.sendData(resultData);
				}
				else {
					_debug.warning("Persistenter Datensatz kann nicht versendet werden: " + resultData.toString());
				}
			}

			// Parametersatz zum Empfangen anmelden (Parametrierung verwaltet auch ihre eigenen Paramter)
			_connection.subscribeReceiver(this,
			                              paramApp,
			                              parameterDescriptionIn,
			                              ReceiveOptions.normal(),
			                              ReceiverRole.drain());

			_debug.info("...warte auf zu verarbeitende Parameterdaten...");
		}
		catch (DataNotSubscribedException e) {
			_debug.error("Versuch, Daten ohne Anmeldung zu senden.");
			e.printStackTrace();
			throw new RuntimeException(e);

		}
		catch (SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
			_debug.error("Sendeversuch als Sender ohne Sendesteuerung.");
			sendSubscriptionNotConfirmed.printStackTrace();
			throw new RuntimeException(sendSubscriptionNotConfirmed);

		}
		catch (ConfigurationException e) {
			_debug.error("Zugriff auf Konfiguration fehlgeschlagen.");
			e.printStackTrace();
			throw new RuntimeException(e);

		}
		catch (OneSubscriptionPerSendData oneSubscriptieonPerSendData) {
			_debug.error("Versuch der Mehrfachanmeldung von Daten.");
			oneSubscriptieonPerSendData.printStackTrace();
			throw new RuntimeException(oneSubscriptieonPerSendData);

		}
		catch (Exception e) {
			_debug.error("Fehler beim setzten der Zugriffstechte.");
			e.printStackTrace();
			throw new RuntimeException(e);

		}
	}

	//	/**
	//	 * Setzt für aller User Berechtigungsklasse auf Vollzugriff. Methode entfernen, sobald Zugriffsrechterparameter von
	//	 * ParamApp korrekt verarbeitet werden.
	//	 *
	//	 * @throws Exception
	//	 */
	//	private void setStandardUserAccess() throws Exception {
	//		//Datensatz zur Zuordnung der berechtigungsklasse.operator zur rolle.alles und
	//		SystemObject operatorAccessObject = _config.getObject("berechtigungsklasse.operator");
	//		AttributeGroup atg = _config.getAttributeGroup("atg.rollenRegionenPaareParameter");
	//		Data data = _connection.createData(atg);
	//		Data.Array roleRegionPairs = data.getItem("rollenRegionenPaare").asArray();
	//		roleRegionPairs.setLength(1);
	//		roleRegionPairs.getItem(0).getReferenceValue("rolle").setSystemObject(_config.getObject("rolle.alles"));
	//		roleRegionPairs.getItem(0).getReferenceValue("region").setSystemObject(_config.getObject("region.alles"));
	//		publishParameter(operatorAccessObject, atg, data);
	//		System.out.println("Berechtigungsklasse '" + operatorAccessObject.getNameOrPidOrId() + "' wurde auf unbeschränkten Zugriff eingestellt");
	//
	//
	//		//Für alle Benutzer Zuordnung zur Berechtigungsklasse herstellen:
	//		atg = _config.getAttributeGroup("atg.benutzerParameter");
	//		data = _connection.createData(atg);
	//		data.getReferenceValue("berechtigungsklasse").setSystemObject(operatorAccessObject);
	//		Iterator allUserIterator = _config.getType("typ.benutzer").getElements().iterator();
	//		while (allUserIterator.hasNext()) {
	//			SystemObject user = (SystemObject) allUserIterator.next();
	//			publishParameter(user, atg, data);
	//			System.out.println("Berechtigungsklasse '" + operatorAccessObject.getNameOrPidOrId() + "' wurde für den Benutzer '" + user.getName() + "' parametriert.");
	//		}
	//	}
	//
	//	/**
	//	 * Hilffunktion für das Setzten der Zugriffsrechte. wird nur für {@link #setStandardUserAccess()} benöätigt.
	//	 *
	//	 * @param object Objek, für das Daten versandt werden.
	//	 * @param atg    Attributgruppe.
	//	 * @param data   zu versendende Daten.
	//	 *
	//	 * @throws Exception wenn beim versenden Fehler auftreten.
	//	 */
	//	private void publishParameter(SystemObject object, AttributeGroup atg, Data data) throws Exception {
	//		Aspect aspect = _config.getAspect("asp.parameterSoll");
	//		DataDescription dataDescription = new DataDescription(atg, aspect);
	//		_connection.subscribeSender(this, new SystemObject[]{object}, dataDescription, SenderRole.source());
	//		ResultData result = new ResultData(object, dataDescription, System.currentTimeMillis(), data);
	//		_connection.sendData(result);
	//	}

	/**
	 * Sendesteuerung des Datenverteilers an die Applikation. Diese Methode muss
	 * von der Applikation implementiert werden, um den Versand von Daten zu
	 * starten bzw. anzuhalten. Der Datenverteiler signalisiert damit einer Quelle
	 * oder einem Sender dass mindestens ein Abnehmer bzw. kein Abnehmer mehr für
	 * die zuvor angemeldeten Daten vorhanden ist. Die Quelle wird damit
	 * aufgefordert den Versand von Daten zu starten bzw. zu stoppen.
	 *
	 * @param object          Das in der zugehörigen Sendeanmeldung angegebene
	 *                        Objekt, auf das sich die Sendesteuerung bezieht.
	 * @param dataDescription Beschreibende Informationen zu den angemeldeten Daten
	 *                        auf die sich die Sendesteuerung bezieht.
	 * @param state           Status der Sendesteuerung. Kann einen der Werte
	 *                        <code>START_SENDING</code>, <code>STOP_SENDING</code>,
	 *                        <code>STOP_SENDING_NO_RIGHTS</code>,
	 *                        <code>STOP_SENDING_NOT_A_VALID_SUBSCRIPTION</code>
	 *                        enthalten.
	 *
	 * @see #START_SENDING
	 * @see #STOP_SENDING
	 * @see #STOP_SENDING_NO_RIGHTS
	 * @see #STOP_SENDING_NOT_A_VALID_SUBSCRIPTION
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		_debug.fine("dataRequest for " + object.getNameOrPidOrId() + ", state: " + state);
	}

	/**
	 * Liefert <code>false</code> zurück, um den Datenverteiler-Applikationsfunktionenen
	 * zu signalisieren, dass keine Sendesteuerung gewünscht wird.
	 *
	 * @param object          Wird ignoriert.
	 * @param dataDescription Wird ignoriert.
	 *
	 * @return <code>false</code>.
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}

	/**
	 * Aktualisierungsmethode, die nach Empfang eines angemeldeten Datensatzes von
	 * den Datenverteiler-Applikationsfunktionen aufgerufen wird.
	 *
	 * @param results Feld mit den empfangenen Ergebnisdatensätzen.
	 */
	public void update(ResultData[] results) {
		_debug.fine("Uddatedatensätze erhalten...");
		try {
			List outputList = new LinkedList();
			for (int i = 0; i < results.length; ++i) {
				ResultData input = results[i];
				DataDescription inputDataDescription = input.getDataDescription();
				DataDescription outputDataDescription = new DataDescription(inputDataDescription.getAttributeGroup(),
				                                                            _outputAspect,
				                                                            inputDataDescription.getSimulationVariant());
				_debug.fine("...." + inputDataDescription);
				if (input.hasData()) {
					ResultData output = new ResultData(input.getObject(), outputDataDescription, System.currentTimeMillis(), input.getData());
					outputList.add(output);

					// Paramtersatz persistent machen.
					DataIdentification dataIdentification = new DataIdentification(input.getObject(), outputDataDescription);
					((PersistanceHandler) _paramObjects.get(dataIdentification)).makeDataPersistance(output, false);
				}
			}
			ResultData[] outputResults = (ResultData[]) outputList.toArray(new ResultData[outputList.size()]);
			_connection.sendData(outputResults);

		}
		catch (Exception e) {
			_debug.warning("Datensatz konnte nicht von [" + ASP_INPUT + "] nach [" + ASP_OUTPUT + "] kopiert werden.");
		}
	}

	/**
	 * Main-Methode mit <ul> <li>Auswertung der Aufrufparamter</li> <li>Anmeldung
	 * an den Datenverteiler</li> <li>Erzeugen ein ParamApp</li> </ul>
	 *
	 * @param arguments Die Aufrufparameter der Applikation
	 */
	public static void main(String[] arguments) {

		try {
			ArgumentList argumentList = new ArgumentList(arguments);
			// DebugLogger für Debug-Ausgaben
			// Debug.setHandlerLevel("StdErr", Debug.INFO);
			Debug.init("ParameterApplication", argumentList);
			_debug = Debug.getLogger();
			Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

			ClientDavParameters clientDavParameters = new ClientDavParameters(argumentList);
			clientDavParameters.setApplicationTypePid("typ.parametrierungsApplikation");
			clientDavParameters.setApplicationName("ParameterApplikation");

			long pause = argumentList.fetchArgument("-sleep=3000").longValue();
			Thread.sleep(pause);

			_connection = new ClientDavConnection(clientDavParameters);
			_connection.connect();
			_connection.login();

			_paramPath = argumentList.fetchArgument("-parameterVerzeichnis=./parameter").asDirectory();

			// Jede Instanz der Parametrierung braucht seine eigenen Parameter. Diese hängen Defaultmäßig am
			// lokalen Konfigurationsverantwortlichen.
			_pidParametrierung = argumentList.fetchArgument("-parametrierung=").asString();
			if (_pidParametrierung.length() == 0) {
				_pidParametrierung = _connection.getLocalConfigurationAuthority().getPid();
			}

			argumentList.ensureAllArgumentsUsed();
			new ParamApp();
		}
		catch (IllegalStateException e) {
			e.printStackTrace(System.out);
			System.out.println("Fehler beim Auswerten der Argumente:");
			System.out.println("  " + e.getMessage());
			System.out.println("Benutzung: java de.kappich.puk.param.main.ParamApp -parameterPfad=ParameterPfad -parametrierung=Pid");
			System.out.println("ParameterPfad: Pfadangabe, in der die Parameterdaten persistent gehalten werden.");
			System.out.println("Pid:           PID des Parametrierungsobjekts (typ.parametrierung) an dem die)");
			System.out.println("               Parameter für diese Parametrierung verwaltet werden.");
			System.exit(1);
		}
		catch (Exception e) {
			_debug.error("Schwerer Fehler beim Ausführen der Parameterapplikation:");
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
				_debug.warning("Laufzeitfehler: " + t + " hat sich wegen einer Exception beendet", e);
			}
		}
	}

}
