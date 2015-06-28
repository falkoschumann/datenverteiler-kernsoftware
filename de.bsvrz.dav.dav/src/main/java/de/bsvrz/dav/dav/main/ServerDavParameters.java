/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
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

import de.bsvrz.dav.daf.communication.lowLevel.ParameterizedConnectionInterface;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.MissingParameterException;
import de.bsvrz.dav.daf.main.impl.ArgumentParser;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.main.impl.InvalidArgumentException;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Diese Klasse stellt die Parameter des Datenverteilers auf Server-Seite zur Verfügung. Diese Parameter werden durch den Konstruktor oder durch entsprechende
 * Setter-Methoden gesetzt und können durch entsprechende Getter-Methoden gelesen werden.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 12959 $
 */
public class ServerDavParameters {

	/** DebugLogger für Debug-Ausgaben */
	private static final Debug _debug = Debug.getLogger();

	/** Parameter Schlüssel */
	private static final String LOCAL_CONFIGURATION_DATA_KEY = "-lokaleKonfiguration=";

	private static final String REMOTE_CONFIGURATION_DATA_KEY = "-remoteKonfiguration=";

	private static final String SEND_KEEP_ALIVE_TIMEOUT_KEY = "-timeoutSendeKeepAlive=";

	private static final String RECEIVE_KEEP_ALIVE_TIMEOUT_KEY = "-timeoutEmpfangeKeepAlive=";

	private static final String USER_NAME_KEY = "-benutzer=";

	private static final String AUTHENTIFICATION_FILE_KEY = "-authentifizierung=";

	private static final String AUTHENTIFICATION_PROCESS_KEY = "-authentifizierungsVerfahren=";

	private static final String TRANSMITTER_ID_KEY = "-datenverteilerId=";

	private static final String DAV_DAV_PORT_KEY = "-davDavPort=";

	private static final String DAV_DAV_PORT_OFFSET_KEY = "-davDavPortOffset=";

	private static final String DAV_APP_PORT_KEY = "-davAppPort=";

	private static final String NEIBOUR_CONNECTION_TIMEOUT_KEY = "-timeoutNachbar=";

	private static final String SYNC_RESPONCE_TIMEOUT_KEY = "-timeoutAntwort=";

	private static final String CONFIGURATION_USER_NAME_KEY = "-konfigurationsBenutzer=";

	private static final String PARAMETER_USER_NAME_KEY = "-parametrierungsBenutzer=";

	private static final String ACCESS_CONTROL_PLUGIN_KEY = "-zugriffsRechtePlugins=";

	private static final String PARAMETER_SEPARATOR = ":";

	/** The ressource bundle of this server */
	private ResourceBundle resourceBundle = ResourceBundle.getBundle("de.bsvrz.dav.dav.main.serverResourceBundle", Locale.getDefault());

	/** lokale Konfiguration = true */
	private boolean _localConfiguration;

	
	/** Die Konfigurations-Id */
	private long _configurationId;

	/** Die Konfigurations-Pid */
	private String _configurationPid;

	/** Datenverteileradresse für die Konfigurationsanbindung */
	private String _configDataTransmitterAddress;

	/** Datenverteilersubadresse für die Konfigurationsanbindung */
	private int _configDataTransmitterSubAddress;

	/**
	 * Das Timeout zum Senden von KeepAlive-Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu verwendenden Wert
	 * festlegt.
	 */
	private long _receiveKeepAliveTimeout;

	/**
	 * Das KeepAlive-Timeout beim Empfang von Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu verwendenden Wert
	 * festlegt.
	 */
	private long _sendKeepAliveTimeout;

	/** Der Name des Kommunikationsprotokolls (Default: TCP-IP) */
	private String _lowLevelCommunicationName;

	/** Parameter für das Kommunikationsprotokoll */
	private String _lowLevelCommunicationParameters;

	/** Der Name des Authentifikationsprozesses (Default: HMAC-MD5) */
	private String _authentificationProcessName;

	/** Der Name des Benutzers */
	private String _userName;

	/** Das Benutzer-Passwort */
	private String _userPassword;

	/** Die lokale Datenverteiler-Id */
	private long _dataTransmitterId;

	/** Der Name des Datenverteilers (Default: Datenverteiler) */
	private String _dataTransmitterName;

	/** Die Pid des Datenverteilertyps (Default: typ.datenverteiler) */
	private String _dataTransmitterTypePid;

	/** Die Größe des Sendepuffers in Byte, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird. */
	private int _davCommunicationOutputBufferSize;

	/** Die Größe des Empfangspuffers in Byte, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird. */
	private int _davCommunicationInputBufferSize;

	/** Die Größe des Sendepuffers in Byte, der bei der Kommunikation mit einer Applikation eingesetzt wird. */
	private int _appCommunicationOutputBufferSize;

	/** Die Größe des Empfangspuffers in Byte, der bei der Kommunikation mit einer Applikation eingesetzt wird. */
	private int _appCommunicationInputBufferSize;

	/**
	 * Die Verzögerungszeit zur Übertragung von gepufferten und zu versendenden Telegrammen. Die Übertragung der gesammelten Daten im Sendepuffer findet erst
	 * statt, wenn die hier angegebene Zeit lang keine Daten mehr in der Puffer geschrieben wurden oder der Sendepuffer voll ist.
	 */
	private long _communicationSendFlushDelay;

	/**
	 * Die maximale Größe von Datentelegrammen. Größere Telegramme werden in mehrere Telegramme zerlegt.
	 *
	 * @param maxTelegramSize  Maximale Größe von versendeten Datentelegrammen.
	 */
	private int _maxTelegramSize;

	/** Die Subadresse auf der der Datenverteiler auf die Datenverteilerverbindungen wartet. */
	private int _transmitterConnectionsSubAddress;

	/** Offset für die Subadresse auf der der Datenverteiler auf die Datenverteilerverbindungen wartet. */
	private int _transmitterConnectionsSubAddressOffset = 0;

	/** Die Subadresse auf der der Datenverteiler auf die Applikationsverbindungen wartet. */
	private int _applicationConnectionsSubAddress;

	/** Der Benutzername der Konfiguration */
	private String _configurationUserName;

	/** Das Benutzerpasswort der Konfiguration */
	private String _configurationUserPassword;

	/** Der Benutzername der Parametrierung */
	private String _parameterUserName;

	/** Das Benutzerpasswort der Parametrierung */
	private String _parameterUserPassword;

	/** Benutzerpassworttabelle */
	private Properties _userProperties;

	/** Flag, das angibt, ob die Benutzerrechte durch diesen Datenverteiler geprüft werden sollen. */
	private UserRightsChecking _userRightsChecking = UserRightsChecking.Compatibility_Enabled;

	/** Liste mit den Plugins für die Kontrolle der Benutzerrechte über den Datenverteiler */
	private List<String> _accessControlPlugins = new ArrayList<String>();

	/** Zeit in Millisekunden, die gewartet werden soll bevor Verbindungen von anderen Datenverteilern akzeptiert werden dürfen. */
	private long _initialInterDavServerDelay;

	/** Zeit in Millisekunden, die gewartet werden soll bevor versucht wird, abgebrochene Verbindungen neu aufzubauen. */
	private long _reconnectInterDavDelay;

	/** Kennung, die (falls <code>true</code>) dafür sorgt, dass der Datenverteiler auf die Applikationsfertigmeldung der Parametrierung wartet. */
	private boolean _waitForParamApp = false;

	/** Inkarnationsname der Parametrierung auf deren Applikationsfertigmeldung gewartet werden soll oder <code>null</code> falls der Inkarnationsname egal ist. */
	private String _paramAppIncarnationName = null;

	/** Pid des Konfigurationsbereichs in dem Applikationsobjekte erzeugt werden sollen oder Leerstring falls der Default-Bereich der Konfiguration
	 * verwendet werden soll.
	 */
	private String _configAreaPidForApplicationObjects = "";

	/**
	 * Erzeugt einen neuen Parametersatz mit Defaultwerten für die einzelnen Parameter und setzt die in den übergebenen Aufrufargumenten angegebenen Parameter mit
	 * den angegebenen Werten. Unbekannte Aufrufargumente werden ignoriert. Bekannte Aufrufargumente werden nach der Umsetzung auf null gesetzt, um zu
	 * signalisieren, daß diese Argumente bereits interpretiert wurden.
	 *
	 * @param argumentList Die auszuwertenden Aufrufargumente.
	 *
	 * @throws de.bsvrz.dav.daf.main.MissingParameterException
	 *          Falls ein Argument nicht oder nicht korrekt angegeben wurde.
	 */
	public ServerDavParameters(ArgumentList argumentList) throws MissingParameterException {
		initialiseDavParameters(argumentList.getArgumentStrings());
		if(argumentList.hasArgument("-davTelegrammPuffer")) {
			try {
				final String[] davBufferValues = argumentList.fetchArgument("-davTelegrammPuffer").asNonEmptyString().split(":");
				if(davBufferValues.length < 1) {
					throw new IllegalArgumentException("Zu wenig Argumente.");
				}
				if(davBufferValues.length > 2) {
					throw new MissingParameterException("Zu viele Argumente.");
				}
				for(int i = 0; i < davBufferValues.length; i++) {
					String davBufferValue = davBufferValues[i];

					final Integer bufferSize = Integer.valueOf(davBufferValue);
					if(bufferSize < 100000) {
						throw new IllegalArgumentException("Puffergröße " + bufferSize + " ist nicht sinnvoll. Mindestwert: 100000.");
					}
					if(i==0) _davCommunicationOutputBufferSize = bufferSize;
					_davCommunicationInputBufferSize = bufferSize;
				}
			}
			catch(Exception e) {
				throw new MissingParameterException("Argument -davTelegrammPuffer sollte eine oder zwei mit Doppelpunkt getrennte Zahlen enthalten, die die Sendepuffergröße bzw. die Empfangspuffergröße in Bytes spezifizieren.");
			}
		}

		String waitForParamAppValue = argumentList.fetchArgument("-warteAufParametrierung=ja").asNonEmptyString();
		if(waitForParamAppValue.toLowerCase().trim().equals("ja")) {
			_waitForParamApp = true;
			_paramAppIncarnationName = null;
		}
		else if(waitForParamAppValue.toLowerCase().trim().equals("nein")) {
			_waitForParamApp = false;
			_paramAppIncarnationName = null;
		}
		else {
			_waitForParamApp = true;
			_paramAppIncarnationName = waitForParamAppValue;
		}

		_configAreaPidForApplicationObjects = argumentList.fetchArgument("-konfigurationsBereichFuerApplikationsobjekte=").asString();


		if(argumentList.hasArgument("-appTelegrammPuffer")) {
			try {
				final String[] appBufferValues = argumentList.fetchArgument("-appTelegrammPuffer").asNonEmptyString().split(":");
				if(appBufferValues.length < 1) {
					throw new IllegalArgumentException("Zu wenig Argumente.");
				}
				if(appBufferValues.length > 2) {
					throw new MissingParameterException("Zu viele Argumente.");
				}
				for(int i = 0; i < appBufferValues.length; i++) {
					String appBufferValue = appBufferValues[i];

					final Integer bufferSize = Integer.valueOf(appBufferValue);
					if(bufferSize < 100000) {
						throw new IllegalArgumentException("Puffergröße " + bufferSize + " ist nicht sinnvoll. Mindestwert: 100000.");
					}
					if(i==0) _appCommunicationOutputBufferSize = bufferSize;
					_appCommunicationInputBufferSize = bufferSize;
				}
			}
			catch(Exception e) {
				throw new MissingParameterException("Argument -appTelegrammPuffer sollte eine oder zwei mit Doppelpunkt getrennte Zahlen enthalten, die die Sendepuffergröße bzw. die Empfangspuffergröße in Bytes spezifizieren.");
			}
		}
		final String delayArgumentName = "-verzögerungFürAndereDatenverteiler";
		if(argumentList.hasArgument(delayArgumentName)) {
			_initialInterDavServerDelay = argumentList.fetchArgument(delayArgumentName).asRelativeTime();
		}
		else {
			final String alternateArgument = delayArgumentName.replaceAll("[ö]", "oe").replaceAll("[ü]", "ue");
			_initialInterDavServerDelay = argumentList.fetchArgument(alternateArgument + "=60s").asRelativeTime();
		}

		_reconnectInterDavDelay = argumentList.fetchArgument("-wiederverbindungsWartezeit=60s").asRelativeTime();
		if(_reconnectInterDavDelay < 1){
			throw new MissingParameterException("Die angegebene -wiederverbindungsWartezeit=" + _reconnectInterDavDelay + "ms ist ungültig: Muss > 0 sein.");
		}

		final String[] strings = argumentList.fetchArgument("-tcpKommunikationsModul=" + getLowLevelCommunicationName()).asNonEmptyString().split(":", 2);
		final String tcpCommunicationClassName = strings[0];
		final String tcpCommunicationParameters = (strings.length > 1) ? strings[1] : "";
		try {
			final Class<?> aClass = Class.forName(tcpCommunicationClassName);
			if(tcpCommunicationParameters.length() != 0 && !(aClass.newInstance() instanceof ParameterizedConnectionInterface)) {
				throw new MissingParameterException("Das angegebene Kommunikationsverfahren " + tcpCommunicationClassName + " unterstützt keine Parameter: " + tcpCommunicationParameters);
			}
		}
		catch(ClassNotFoundException e) {
			throw new MissingParameterException("Das angegebene Kommunikationsverfahren ist nicht verfügbar, Klassenname: " + tcpCommunicationClassName, e);
		}
		catch(InstantiationException e) {
			throw new MissingParameterException("Das angegebene Kommunikationsverfahren kann nicht instantiiert werden, Klassenname: " + tcpCommunicationClassName, e);
		}
		catch(IllegalAccessException e) {
			throw new MissingParameterException("Auf das angegebene Kommunikationsverfahren kann nicht zugegriffen werden, Klassenname: " + tcpCommunicationClassName, e);
		}
		_lowLevelCommunicationName = tcpCommunicationClassName;
		_lowLevelCommunicationParameters = tcpCommunicationParameters;
	}

	/**
	 * Wertet die Default-Argumente aus und liest sie ggf. aus der <code>serverResourceBundle.properties</code>-Datei aus.
	 *
	 * @param startArguments die Default-Argumente
	 *
	 * @throws MissingParameterException Falls ein Argument nicht oder nicht korrekt angegeben wurde.
	 */
	private void initialiseDavParameters(final String[] startArguments) throws MissingParameterException {
		try {
			String tmp, parameter;

			parameter = getParameter(startArguments, "-rechtePrüfung=");
			if(parameter == null) parameter = getParameter(startArguments, "-rechtePruefung=");
			if(parameter != null) {
				parameter = parameter.substring(parameter.indexOf('=') + 1).trim().toLowerCase();
				if(parameter.equals("no") || parameter.equals("nein") || parameter.equals("false")) {
					setUserRightsChecking(UserRightsChecking.Disabled);
				}
				else if(parameter.equals("yes") || parameter.equals("ja") || parameter.equals("wahr")) {
					setUserRightsChecking(UserRightsChecking.Compatibility_Enabled);
				}
				else if(parameter.equals("neu")) {
					setUserRightsChecking(UserRightsChecking.NewDataModel);
				}
				else if(parameter.equals("alt")) {
					setUserRightsChecking(UserRightsChecking.OldDataModel);
				}
				else {
					throw new MissingParameterException("Aufrufparameter zur Rechteprüfung sollte den Wert 'neu', 'alt', 'ja' oder 'nein' haben");
				}
			}


			// Rechteprüfungs-Plugins
			parameter = getParameter(startArguments, ACCESS_CONTROL_PLUGIN_KEY);
			if(parameter != null) {
				try {
					final String param = ArgumentParser.getParameter(parameter, ACCESS_CONTROL_PLUGIN_KEY);
					if((param == null) || (param.length() == 0)) {
						throw new MissingParameterException(
								"Der Parameter für die Plug-Ins zur Zugriffsrechteverwaltung muss folgende Formatierung besitzen: " + ACCESS_CONTROL_PLUGIN_KEY + "=Bezeichnung[,Bezeichnung,Bezeichnung,...]"
						);
					}
					final String[] plugins = param.split(",");
					for(String plugin : plugins) {
						try {
							plugin = plugin.trim();
							Class.forName(plugin);
							_accessControlPlugins.add(plugin);
						}
						catch(ClassNotFoundException ex) {
							throw new MissingParameterException("Das angegebene Plug-In existiert nicht: " + plugin);
						}
					}
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Der Parameter für die Plug-Ins zur Zugriffsrechteverwaltung muss folgende Formatierung besitzen: " + ACCESS_CONTROL_PLUGIN_KEY + "=Bezeichnung[,Bezeichnung,Bezeichnung,...]"
					);
				}
			}


			// Lokale Konfiguration?
			parameter = getParameter(startArguments, LOCAL_CONFIGURATION_DATA_KEY);
			if(parameter == null) {
				// Remote Konfiguration?
				parameter = getParameter(startArguments, REMOTE_CONFIGURATION_DATA_KEY);
				if(parameter == null) {
					tmp = resourceBundle.getString("LokaleKonfiguration");
					_localConfiguration = Integer.parseInt(tmp) == 1;
					if(_localConfiguration) {
						_configurationPid = resourceBundle.getString("Konfiguration-PID");
						tmp = resourceBundle.getString("Konfiguration-ID");
						_configurationId = Long.parseLong(tmp);
					}
					else {
						_configDataTransmitterAddress = resourceBundle.getString("Konfig-Datenverteiler-Adresse");
						tmp = resourceBundle.getString("Konfig-Datenverteiler-Subadresse");
						_configDataTransmitterSubAddress = Integer.parseInt(tmp);
						_configurationPid = resourceBundle.getString("Konfiguration-PID");
					}
				}
				else {
					try {
						String[] parameters = ArgumentParser.getParameters(
								parameter, REMOTE_CONFIGURATION_DATA_KEY, PARAMETER_SEPARATOR
						);
						if((parameters != null) && (parameters.length == 3)) {
							_localConfiguration = false;
							_configDataTransmitterAddress = parameters[0];
							_configDataTransmitterSubAddress = Integer.parseInt(parameters[1]);
							_configurationPid = parameters[2];
							if("null".equals(_configurationPid)) {
								_configurationPid = CommunicationConstant.LOCALE_CONFIGURATION_PID_ALIASE;
							}
						}
						else {
							throw new MissingParameterException(
									"Remote-Konfiguration-Parameter muss folgende Formatierung besitzen: -remoteKonfiguration=Zeichenkette:Zahl:Zeichenkette"
							);
						}
					}
					catch(InvalidArgumentException ex) {
						throw new MissingParameterException(
								"Remote-Konfiguration-Parameter muss folgende Formatierung besitzen: -remoteKonfiguration=Zeichenkette:Zahl:Zeichenkette"
						);
					}
					catch(NumberFormatException ex) {
						throw new MissingParameterException(
								"Remote-Konfiguration-Parameter muss folgende Formatierung besitzen: -remoteKonfiguration=Zeichenkette:Zahl:Zeichenkette"
						);
					}
				}
			}
			else {
				throw new IllegalArgumentException(
						"Aufrufargument " + LOCAL_CONFIGURATION_DATA_KEY + " wird nicht mehr unterstützt, da die Einstellung jetzt"
						+ " automatisch von der lokalen Konfiguration übernommen wird."
				);
			}

			//Send keep alive timeout
			parameter = getParameter(startArguments, SEND_KEEP_ALIVE_TIMEOUT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("Keepalive-Sendezeitgrenze");
				_sendKeepAliveTimeout = Long.parseLong(tmp);
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, SEND_KEEP_ALIVE_TIMEOUT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Sende-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutSendeKeepAlive=Zahl"
						);
					}
					_sendKeepAliveTimeout = Integer.parseInt(tmp) * 1000;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Sende-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutSendeKeepAlive=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Sende-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutSendeKeepAlive=Zahl"
					);
				}
			}
			if(_sendKeepAliveTimeout < 1000) {
				throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
			}

			//Receive keep alive timeout
			parameter = getParameter(startArguments, RECEIVE_KEEP_ALIVE_TIMEOUT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("Keepalive-Empfangszeitgrenze");
				_receiveKeepAliveTimeout = Long.parseLong(tmp);
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, RECEIVE_KEEP_ALIVE_TIMEOUT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Empfang-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutEmpfangeKeepAlive=Zahl"
						);
					}
					_receiveKeepAliveTimeout = Integer.parseInt(tmp) * 1000;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Empfang-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutEmpfangeKeepAlive=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Empfang-Keep-Alive-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutEmpfangeKeepAlive=Zahl"
					);
				}
			}
			if(_receiveKeepAliveTimeout < 1000) {
				throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
			}

			// User Passwort Tabelle
			parameter = getParameter(startArguments, AUTHENTIFICATION_FILE_KEY);
			if(parameter == null) {
				_userProperties = null;
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, AUTHENTIFICATION_FILE_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Authentificationsdatei-Parameter muss folgende Formatierung besitzen: -authentifizierung=Zeichenkette"
						);
					}
					else {
						_userProperties = new Properties();
						try {
							_userProperties.load(new BufferedInputStream(new FileInputStream(tmp)));
						}
						catch(IOException ex) {
							throw new MissingParameterException("Spezifizierte Authentifizierungsdatei nicht vorhanden");
						}
					}
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Authentificationsdatei-Parameter muss folgende Formatierung besitzen: -authentifizierung=Zeichenkette"
					);
				}
			}

			//User name
			parameter = getParameter(startArguments, USER_NAME_KEY);
			if(parameter == null) {
				_userName = resourceBundle.getString("Benutzername");
				_userPassword = resourceBundle.getString("Benutzerpasswort");
			}
			else {
				try {
					_userName = ArgumentParser.getParameter(parameter, USER_NAME_KEY);
					if((_userName == null) || (_userName.length() == 0)) {
						throw new MissingParameterException(
								"Benutzername-Parameter muss folgende Formatierung besitzen: -benutzer=Zeichenkette"
						);
					}
					_userPassword = null;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Benutzername-Parameter muss folgende Formatierung besitzen: -benutzer=Zeichenkette"
					);
				}
			}
			//Authentification file name
			if(_userPassword == null) {
				if(_userProperties == null) {
					throw new MissingParameterException("Keine Authentifizierungsdatei angegeben");
				}
				else {
					_userPassword = _userProperties.getProperty(_userName);
					if((_userPassword == null) || (_userPassword.length() == 0)) {
						throw new MissingParameterException(
								"Das Passwort für den Benutzer " + _userName + " ist in der Authentifizierungsdatei nicht vorhanden"
						);
					}
				}
			}
			//Authentification process
			parameter = getParameter(startArguments, AUTHENTIFICATION_PROCESS_KEY);
			if(parameter == null) {
				_authentificationProcessName = resourceBundle.getString("AuthentificationProcessName");
			}
			else {
				try {
					_authentificationProcessName = ArgumentParser.getParameter(parameter, AUTHENTIFICATION_PROCESS_KEY);
					if((_authentificationProcessName == null) || (_authentificationProcessName.length() == 0)) {
						throw new MissingParameterException(
								"Der Parameter für die Klasse des Authentifizierungsverfahren muss folgende Formatierung besitzen: -authentifizierungsVerfahren=Zeichenkette"
						);
					}
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Der Parameter für die Klasse des Authentifizierungsverfahren muss folgende Formatierung besitzen: -authentifizierungsVerfahren=Zeichenkette"
					);
				}
			}
			try {
				Class.forName(_authentificationProcessName);
			}
			catch(ClassNotFoundException ex) {
				throw new MissingParameterException(
						"Die Implementierung des Authentifizierungsverfahrens existiert nicht: " + _authentificationProcessName
				);
			}

			//Konfiguration User name
			parameter = getParameter(startArguments, CONFIGURATION_USER_NAME_KEY);
			if(parameter == null) {
				_configurationUserName = resourceBundle.getString("configurationUserName");
				_configurationUserPassword = resourceBundle.getString("configurationUserPassword");
			}
			else {
				try {
					_configurationUserName = ArgumentParser.getParameter(parameter, CONFIGURATION_USER_NAME_KEY);
					if((_configurationUserName == null) || (_configurationUserName.length() == 0)) {
						throw new MissingParameterException(
								"KonfigurationsbenutzerName-Parameter muss folgende Formatierung besitzen: -konfigurationsBenutzer=Zeichenkette"
						);
					}
					_configurationUserPassword = null;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"KonfigurationsbenutzerName-Parameter muss folgende Formatierung besitzen: -konfigurationsBenutzer=Zeichenkette"
					);
				}
			}

			//Konfig user password
			if(_configurationUserPassword == null) {
				if(_userProperties == null) {
					throw new MissingParameterException("Keine Authentifizierungsdatei angegeben");
				}
				else {
					_configurationUserPassword = _userProperties.getProperty(_configurationUserName);
					if((_configurationUserPassword == null) || (_configurationUserPassword.length() == 0)) {
						throw new MissingParameterException(
								"Das Passwort für den Konfigurationsbenutzer " + _configurationUserName + " ist in der Authentifizierungsdatei nicht vorhanden"
						);
					}
				}
			}

			//Parameter User name
			parameter = getParameter(startArguments, PARAMETER_USER_NAME_KEY);
			if(parameter == null) {
				_parameterUserName = resourceBundle.getString("parameterUserName");
				_parameterUserPassword = resourceBundle.getString("parameterUserPassword");
			}
			else {
				try {
					_parameterUserName = ArgumentParser.getParameter(parameter, PARAMETER_USER_NAME_KEY);
					if((_parameterUserName == null) || (_parameterUserName.length() == 0)) {
						throw new MissingParameterException(
								"ParametrierungsbenutzerName-Parameter muss folgende Formatierung besitzen: -parametrierungsBenutzer=Zeichenkette"
						);
					}
					_parameterUserPassword = null;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"parametrierungsbenutzerName-Parameter muss folgende Formatierung besitzen: -parametrierungsBenutzer=Zeichenkette"
					);
				}
			}

			//Parameter password
			if(_parameterUserPassword == null) {
				if(_userProperties == null) {
					throw new MissingParameterException("Keine Authentifizierungsdatei angegeben");
				}
				else {
					_parameterUserPassword = _userProperties.getProperty(_parameterUserName);
					if((_parameterUserPassword == null) || (_parameterUserPassword.length() == 0)) {
						throw new MissingParameterException(
								"Das Passwort für den Parametrierungsbenutzer " + _parameterUserName + " ist in der Authentifizierungsdatei nicht vorhanden"
						);
					}
				}
			}

			//Data transmitter Id
			parameter = getParameter(startArguments, TRANSMITTER_ID_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("Datenverteiler-ID");
				_dataTransmitterId = Long.parseLong(tmp);
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, TRANSMITTER_ID_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Datenverteiler-Id-Parameter muss folgende Formatierung besitzen: -datenverteilerId=Zeichenkette"
						);
					}
					_dataTransmitterId = Long.parseLong(tmp);
					if(_dataTransmitterId < 0) {
						throw new MissingParameterException("Datenverteiler-Id-Parameter muss groesser 0 sein");
					}
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Id-Parameter muss folgende Formatierung besitzen: -datenverteilerId=Zeichenkette"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Id-Parameter muss folgende Formatierung besitzen: -datenverteilerId=Zeichenkette"
					);
				}
			}

			//Dav-Dav-port
			parameter = getParameter(startArguments, DAV_DAV_PORT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("Dav-Dav-Subadresse");
				_transmitterConnectionsSubAddress = Integer.parseInt(tmp);
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, DAV_DAV_PORT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPort=Zahl"
						);
					}
					_transmitterConnectionsSubAddress = Integer.parseInt(tmp);
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPort=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPort=Zahl"
					);
				}
			}
			if(_transmitterConnectionsSubAddress < 0) {
				throw new MissingParameterException("Die Subadresse muss grösser gleich 0 sein");
			}

			//Dav-Dav-Port-Offset
			parameter = getParameter(startArguments, DAV_DAV_PORT_OFFSET_KEY);
			if(parameter != null) {
				try {
					tmp = ArgumentParser.getParameter(parameter, DAV_DAV_PORT_OFFSET_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPortOffset=Zahl"
						);
					}
					_transmitterConnectionsSubAddressOffset = Integer.parseInt(tmp);
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPortOffset=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Datenverteiler-Port-Parameter muss folgende Formatierung besitzen: -davDavPortOffset=Zahl"
					);
				}
			}

			//Dav-Daf-port
			parameter = getParameter(startArguments, DAV_APP_PORT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("Dav-Daf-Subadresse");
				_applicationConnectionsSubAddress = Integer.parseInt(tmp);
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, DAV_APP_PORT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Datenverteiler-Applikation-Port-Parameter muss folgende Formatierung besitzen: -davAppPort=Zahl"
						);
					}
					_applicationConnectionsSubAddress = Integer.parseInt(tmp);
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Applikation-Port-Parameter muss folgende Formatierung besitzen: -davAppPort=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Datenverteiler-Applikation-Port-Parameter muss folgende Formatierung besitzen: -davAppPort=Zahl"
					);
				}
			}
			if(_applicationConnectionsSubAddress < 0) {
				throw new MissingParameterException("Die Subadresse muss grösser gleich 0 sein");
			}

			//Connection to neighbours time out
			parameter = getParameter(startArguments, NEIBOUR_CONNECTION_TIMEOUT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("NeighbourConnectionTimeOut");
				long connectionTime = Long.parseLong(tmp);
				if(connectionTime != 0) {
					if(connectionTime < 1000) {
						throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
					}
					CommunicationConstant.MAX_WAITING_TIME_FOR_CONNECTION = connectionTime;
				}
				else {
					// es wird wieder der ursprüngliche Wert (10.000.000ms) gesetzt.
					CommunicationConstant.MAX_WAITING_TIME_FOR_CONNECTION = 10000000L;
				}
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, NEIBOUR_CONNECTION_TIMEOUT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Nachbardatenverteiler-Verbindung-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutNachbar=Zahl"
						);
					}
					long connectionTime = Integer.parseInt(tmp) * 1000;
					if(connectionTime < 1000) {
						throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
					}
					CommunicationConstant.MAX_WAITING_TIME_FOR_CONNECTION = connectionTime;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Nachbardatenverteiler-Verbindung-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutNachbar=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Nachbardatenverteiler-Verbindung-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutNachbar=Zahl"
					);
				}
			}

			//Sync answer time out
			parameter = getParameter(startArguments, SYNC_RESPONCE_TIMEOUT_KEY);
			if(parameter == null) {
				tmp = resourceBundle.getString("SyncAnswerTimeOut");
				long responceTime = Long.parseLong(tmp);
				// Wenn der Wert aus der Resource gleich 0 ist, dann wird der Defaultwert aus CommunicationConstant benutzt.
				if(responceTime != 0) {
					if(responceTime < 1000) {
						throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
					}
					CommunicationConstant.MAX_WAITING_TIME_FOR_SYNC_RESPONCE = responceTime;
				}
				else {
					// es wird wieder der ursprüngliche Wert (600.000ms = 10 min) gesetzt.
					CommunicationConstant.MAX_WAITING_TIME_FOR_SYNC_RESPONCE = 600000L;
				}
			}
			else {
				try {
					tmp = ArgumentParser.getParameter(parameter, SYNC_RESPONCE_TIMEOUT_KEY);
					if((tmp == null) || (tmp.length() == 0)) {
						throw new MissingParameterException(
								"Synchrone-Anwort-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutAntwort=Zahl"
						);
					}
					long responceTime = Integer.parseInt(tmp) * 1000;
					if(responceTime < 1000) {
						throw new MissingParameterException("Timeouts müssen grösser gleich als 1 Sekunde sein");
					}
					CommunicationConstant.MAX_WAITING_TIME_FOR_SYNC_RESPONCE = responceTime;
				}
				catch(InvalidArgumentException ex) {
					throw new MissingParameterException(
							"Synchrone-Anwort-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutAntwort=Zahl"
					);
				}
				catch(NumberFormatException ex) {
					throw new MissingParameterException(
							"Synchrone-Anwort-Timeout-Parameter muss folgende Formatierung besitzen: -timeoutAntwort=Zahl"
					);
				}
			}

			_dataTransmitterName = resourceBundle.getString("Datenverteilersname");
			_dataTransmitterTypePid = resourceBundle.getString("Datenverteilerstyp-PID");
			_lowLevelCommunicationName = resourceBundle.getString("KommunikationProtokollName");
			try {
				Class.forName(_lowLevelCommunicationName);
			}
			catch(ClassNotFoundException ex) {
				throw new MissingParameterException("Die Kommunikationsverfahrensklasse existiert nicht");
			}
			tmp = resourceBundle.getString("Sendepuffergrösse");
			_appCommunicationOutputBufferSize = Integer.parseInt(tmp);
			_davCommunicationOutputBufferSize = _appCommunicationOutputBufferSize * 2;
			tmp = resourceBundle.getString("Empfangspuffergrösse");
			_appCommunicationInputBufferSize = Integer.parseInt(tmp);
			_davCommunicationInputBufferSize = _appCommunicationInputBufferSize * 2;
			tmp = resourceBundle.getString("SendeVerzögerung");
			_communicationSendFlushDelay = Long.parseLong(tmp);
			if(_communicationSendFlushDelay > 0) {
				CommunicationConstant.MAX_SEND_DELAY_TIME = _communicationSendFlushDelay;
			}
			tmp = resourceBundle.getString("MaxTelegrammGrösse");
			_maxTelegramSize = Integer.parseInt(tmp);
			if(_maxTelegramSize > 0) {
				CommunicationConstant.MAX_SPLIT_THRESHOLD = _maxTelegramSize;
			}
			else {
				throw new MissingParameterException("Die maximale Telegrammlänge muss grösser 0 sein");
			}
		}
		catch(MissingResourceException ex) {
			ex.printStackTrace();
			throw new MissingParameterException(ex.getMessage());
		}
	}

	/**
	 * Bestimmt das in der Passwort-Datei gespeicherte Passwort eines bestimmten Benutzers.
	 * @param userName Name des Benutzers
	 * @return Passwort des Benutzers oder <code>null</code>, wenn kein Passwort für den Benutzer in der Passwort-Datei enthalten ist.
	 */
	public String getStoredPassword(String userName) {
		if(_userProperties == null) {
			_debug.warning("Lokale Passwort-Datei nicht verfügbar. Sie sollte mit dem Aufrufargument -authentifizierung= angegeben werden.");
			return null;
		}
		return _userProperties.getProperty(userName);
	}

	/**
	 * Sucht in dem angegebenen Feld nach dem Parameter, der mit dem Schlüssel anfängt.
	 *
	 * @param arguments Feld von Startargumenten
	 * @param key       der Schlüssel
	 *
	 * @return Der Wert zum angegebenen Schlüssel oder <code>null</code>, falls kein Wert hierzu existiert.
	 */
	private String getParameter(String[] arguments, String key) {
		String parameter = null;
		if((arguments == null) || (key == null)) {
			return null;
		}
		for(int i = 0; i < arguments.length; ++i) {
			String tmp = arguments[i];
			if(tmp == null) {
				continue;
			}
			if(tmp.startsWith(key)) {
				parameter = tmp;
				arguments[i] = null;
				break;
			}
		}
		return parameter;
	}

	/**
	 * Bestimmt die maximale Größe von Datentelegrammen. Größere Telegramme werden in mehrere Telegramme zerlegt.
	 *
	 * @return maxTelegramSize  Maximale Größe von versendeten Datentelegrammen.
	 */
	public final int getMaxDataTelegramSize() {
		return _maxTelegramSize;
	}

	/**
	 * Setzt die maximale Größe von Datentelegrammen. Größere Telegramme werden in mehrere Telegramme zerlegt.
	 *
	 * @param maxTelegramSize Maximale Größe von versendeten Datentelegrammen.
	 */
	public final void setMaxDataTelegramSize(int maxTelegramSize) {
		if(maxTelegramSize > 0) {
			_maxTelegramSize = maxTelegramSize;
		}
	}

	/**
	 * Liefert die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 * Dies entspricht bei TCP-Verbindungen der TCP-Portnummer des Server-Sockets.
	 *
	 * @return Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 */
	public final int getTransmitterConnectionsSubAddress() {
		return _transmitterConnectionsSubAddress;
	}

	/**
	 * Setzt die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 *
	 * @param port Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 */
	public final void setTransmitterConnectionsSubAddress(int port) {
		_transmitterConnectionsSubAddress = port;
	}

	/**
	 * Liefert die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 * Dies entspricht bei TCP-Verbindungen der TCP-Portnummer des Server-Sockets.
	 *
	 * @return Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 * @deprecated Statt dieser Methode sollte die Methode {@link #getTransmitterConnectionsSubAddress()} verwendet werden.
	 */
	@Deprecated
	public final int getTransmitterConnectionsSubAdress() {
		return getTransmitterConnectionsSubAddress();
	}

	/**
	 * Setzt die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 *
	 * @param port Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 * @deprecated Statt dieser Methode sollte die Methode {@link #setTransmitterConnectionsSubAddress(int)} verwendet werden.
	 */
	@Deprecated
	public final void setTransmitterConnectionsSubAdress(int port) {
		setTransmitterConnectionsSubAddress(port);
	}

	/**
	 * Liefert einen Offset für die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 *
	 * @return Offset für die Subadresse mit der dieser Datenverteiler auf Verbindungen von anderen Datenverteilern wartet.
	 */
	public final int getTransmitterConnectionsSubAddressOffset() {
		return _transmitterConnectionsSubAddressOffset;
	}

	/**
	 * Liefert die Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 * Dies entspricht bei TCP-Verbindungen der TCP-Portnummer des Server-Sockets.
	 *
	 * @return Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 */
	public final int getApplicationConnectionsSubAddress() {
		return _applicationConnectionsSubAddress;
	}

	/**
	 * Setzt die Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 *
	 * @param port Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 */
	public final void setApplicationConnectionsSubAddress(int port) {
		_applicationConnectionsSubAddress = port;
	}

	/**
	 * Liefert die Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 * Dies entspricht bei TCP-Verbindungen der TCP-Portnummer des Server-Sockets.
	 *
	 * @return Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 * @deprecated Statt dieser Methode sollte die Methode {@link #getApplicationConnectionsSubAddress} verwendet werden.
	 */
	@Deprecated
	public final int getApplicationConnectionsSubAdress() {
		return getApplicationConnectionsSubAddress();
	}

	/**
	 * Setzt die Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 *
	 * @param port Subadresse mit der dieser Datenverteiler auf Verbindungen von Applikationen wartet.
	 * @deprecated Statt dieser Methode sollte die Methode {@link #setApplicationConnectionsSubAddress(int)} verwendet werden.
	 */
	@Deprecated
	public final void setApplicationConnectionsSubAdress(int port) {
		setApplicationConnectionsSubAddress(port);
	}

	/**
	 * Gibt die Id des Datenverteilers zurück
	 *
	 * @return die Datenverteiler Id
	 */
	public final long getDataTransmitterId() {
		return _dataTransmitterId;
	}

	/**
	 * Setzt die Id der Datenverteiler auf den neuen Wert
	 *
	 * @param dvId neue Datenverteiler Id
	 */
	public final void setDataTransmitterId(long dvId) {
		_dataTransmitterId = dvId;
	}

	/**
	 * Bestimmt den Namen des Datenverteilers.
	 *
	 * @return applicationName  Name des zu erzeugenden Applikation-Objekts
	 */
	public final String getDataTransmitterName() {
		return _dataTransmitterName;
	}

	/**
	 * Setzt den Namen des Datenverteilers.
	 *
	 * @param dataTransmitterName Name des Datenverteilers
	 */
	public final void setDataTransmitterName(String dataTransmitterName) {
		if(dataTransmitterName != null) {
			_dataTransmitterName = dataTransmitterName;
		}
	}

	/**
	 * Bestimmt den Typ des Datenverteilers.
	 *
	 * @return dataTransmitterTypePid  PID, die den Typ des Datenverteilers.
	 */
	public final String getDataTransmitterTypePid() {
		return _dataTransmitterTypePid;
	}

	/**
	 * Setzt den Typ des Datenverteilers.
	 *
	 * @param dataTransmitterTypePid PID, die den Typ des zu erzeugenden Applikations-Objekts spezifiziert.
	 */
	public final void setDataTransmitterTypePid(String dataTransmitterTypePid) {
		if(dataTransmitterTypePid != null) {
			_dataTransmitterTypePid = dataTransmitterTypePid;
		}
	}

	/**
	 * Bestimmt das bei der Authentifizierung zu verwendende Verfahren.
	 *
	 * @return authentificationProcessName  Name des Verfahrens
	 */
	public final String getAuthentificationProcessName() {
		return _authentificationProcessName;
	}

	/**
	 * Setzt das bei der Authentifizierung zu verwendende Verfahren.
	 *
	 * @param authentificationProcessName Name des Verfahrens
	 */
	public final void setAuthentificationProcessName(String authentificationProcessName) {
		if(authentificationProcessName != null) {
			_authentificationProcessName = authentificationProcessName;
		}
	}

	/**
	 * Bestimmt das auf unterster Ebene einzusetzende Kommunikationsprotokoll.
	 *
	 * @return lowLevelCommunicationName  Name des Kommunikationsverfahrens.
	 */
	public final String getLowLevelCommunicationName() {
		return _lowLevelCommunicationName;
	}

	/**
	 * Setzt das auf unterster Ebene einzusetzende Kommunikationsprotokoll.
	 *
	 * @param lowLevelCommunicationName Name des Kommunikationsverfahrens.
	 */
	public final void setLowLevelCommunicationName(String lowLevelCommunicationName) {
		if(lowLevelCommunicationName != null) {
			_lowLevelCommunicationName = lowLevelCommunicationName;
		}
	}

	/**
	 * Bestimmt den bei der Authentifizierung zu verwendenden Benutzernamen.
	 *
	 * @return userName  Name des Benutzers.
	 */
	public final String getUserName() {
		return _userName;
	}

	/**
	 * Setzt den bei der Authentifizierung zu verwendenden Benutzernamen.
	 *
	 * @param userName Name des Benutzers.
	 */
	public final void setUserName(String userName) {
		if(userName != null) {
			_userName = userName;
		}
	}

	/**
	 * Bestimmt das bei der Authentifizierung zu verwendende Passwort.
	 *
	 * @return userPassword Passwort des Benutzers.
	 */
	public final String getUserPassword() {
		return _userPassword;
	}

	/**
	 * Setzt das bei der Authentifizierung zu verwendende Passwort.
	 *
	 * @param userPassword Passwort des Benutzers.
	 */
	public final void setUserPassword(String userPassword) {
		if(userPassword != null) {
			_userPassword = userPassword;
		}
	}

	/**
	 * Bestimmt das Timeout zum Senden von KeepAlive-Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu verwendenden
	 * Wert festlegt.
	 *
	 * @return timeout  Vorschlag für das Timeout zum Senden von KeepAlive-Telegrammen.
	 */
	public final long getSendKeepAliveTimeout() {
		return _sendKeepAliveTimeout;
	}

	/**
	 * Setzt das Timeout zum Senden von KeepAlive-Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu verwendenden
	 * Wert festlegt.
	 *
	 * @param timeout Vorschlag für das Timeout zum Senden von KeepAlive-Telegrammen.
	 */
	public final void setSendKeepAliveTimeout(long timeout) {
		if(timeout > 0) {
			_sendKeepAliveTimeout = timeout;
		}
	}

	/**
	 * Bestimmt das KeepAlive-Timeout beim Empfang von Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu
	 * verwendenden Wert festlegt.
	 *
	 * @return timeout  Vorschlag für das KeepAlive-Timeout beim Empfang Telegrammen.
	 */
	public final long getReceiveKeepAliveTimeout() {
		return _receiveKeepAliveTimeout;
	}

	/**
	 * Setzt das KeepAlive-Timeout beim Empfang von Telegrammen. Der Wert dient als Vorschlag für die Verhandlung mit dem Datenverteiler, der den zu verwendenden
	 * Wert festlegt.
	 *
	 * @param timeout Vorschlag für das KeepAlive-Timeout beim Empfang Telegrammen.
	 */
	public final void setReceiveKeepAliveTimeout(long timeout) {
		if(timeout > 0) {
			_receiveKeepAliveTimeout = timeout;
		}
	}

	/**
	 * Bestimmt die Verzögerungszeit zur Übertragung von gepufferten und zu versendenden Telegrammen. Die Übertragung der gesammelten Daten im Sendepuffer findet
	 * erst statt, wenn die hier angegebene Zeit lang keine Daten mehr in der Puffer geschrieben wurden oder der Sendepuffer voll ist.
	 *
	 * @return Verzögerungszeit
	 */
	public final long getCommunicationSendFlushDelay() {
		return _communicationSendFlushDelay;
	}

	/**
	 * Setzt die Verzögerungszeit zur Übertragung von gepufferten und zu versendenden Telegrammen. Die Übertragung der gesammelten Daten im Sendepuffer findet erst
	 * statt, wenn die hier angegebene Zeit lang keine Daten mehr in der Puffer geschrieben wurden oder der Sendepuffer voll ist.
	 *
	 * @param delay Verzögerungszeit
	 */
	public final void setCommunicationSendFlushDelay(long delay) {
		if(delay > 0) {
			_communicationSendFlushDelay = delay;
		}
	}

	/**
	 * Bestimmt die Größe des Sendepuffers, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird.
	 *
	 * @return bufferSize Größe des Sendepuffers in Byte.
	 */
	public final int getDavCommunicationOutputBufferSize() {
		return _davCommunicationOutputBufferSize;
	}

	/**
	 * Setzt die Größe des Sendepuffers, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird.
	 *
	 * @param bufferSize Größe des Sendepuffers in Byte.
	 */
	public final void setDavCommunicationOutputBufferSize(int bufferSize) {
		if(bufferSize > 0) {
			_davCommunicationOutputBufferSize = bufferSize;
		}
	}

	/**
	 * Bestimmt die Größe des Empfangspuffers, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird.
	 *
	 * @return bufferSize Größe des Empfangspuffers in Byte.
	 */
	public final int getDavCommunicationInputBufferSize() {
		return _davCommunicationInputBufferSize;
	}

	/**
	 * Setzt die Größe des Empfangspuffers, der bei der Kommunikation mit dem Datenverteiler eingesetzt wird.
	 *
	 * @param bufferSize Größe des Empfangspuffers in Byte.
	 */
	public final void setDavCommunicationInputBufferSize(int bufferSize) {
		if(bufferSize > 0) {
			_davCommunicationInputBufferSize = bufferSize;
		}
	}

	/**
	 * Bestimmt die Größe des Sendepuffers, der bei der Kommunikation mit einer Applikation eingesetzt wird.
	 *
	 * @return bufferSize Größe des Sendepuffers in Byte.
	 */
	public final int getAppCommunicationOutputBufferSize() {
		return _appCommunicationOutputBufferSize;
	}

	/**
	 * Setzt die Größe des Sendepuffers, der bei der Kommunikation mit einer Applikation eingesetzt wird.
	 *
	 * @param bufferSize Größe des Sendepuffers in Byte.
	 */
	public final void setAppCommunicationOutputBufferSize(int bufferSize) {
		if(bufferSize > 0) {
			_appCommunicationOutputBufferSize = bufferSize;
		}
	}

	/**
	 * Bestimmt die Größe des Empfangspuffers, der bei der Kommunikation mit einer Applikation eingesetzt wird.
	 *
	 * @return bufferSize Größe des Empfangspuffers in Byte.
	 */
	public final int getAppCommunicationInputBufferSize() {
		return _appCommunicationInputBufferSize;
	}

	/**
	 * Setzt die Größe des Empfangspuffers, der bei der Kommunikation mit einer Applikation eingesetzt wird.
	 *
	 * @param bufferSize Größe des Empfangspuffers in Byte.
	 */
	public final void setAppCommunicationInputBufferSize(int bufferSize) {
		if(bufferSize > 0) {
			_appCommunicationInputBufferSize = bufferSize;
		}
	}

	/**
	 * Gibt die Information zurück, ob der Datenverteiler auf eine lokale Anmeldung einer Konfigurationsapplikation warten muss.
	 *
	 * @return true  : im lokalen Konfigurationsbetrieb. false : implements remote Konfigurationsbetrieb.
	 */
	public final boolean isLocalMode() {
		return _localConfiguration;
	}

	/**
	 * Gibt die Konfigurationsparameter des Lokalen Modus zurück.
	 *
	 * @return die Pid und die Id der Konfigurationsapplikation
	 */
	public final Object[] getLocalModeParameter() {
		Object[] objs = null;
		if(_localConfiguration) {
			objs = new Object[2];
			objs[0] = _configurationPid;
			objs[1] = new Long(_configurationId);
		}
		return objs;
	}

	/**
	 * Setzt den Datenverteilersbetriebsmodus auf den Lokalen Modus.
	 *
	 * @param configPid die Pid der Konfigurationsapplikation
	 * @param configId  die Id der Konfigurationsapplikation
	 */
	public final void setLocalModeParameter(String configPid, long configId) {
		_localConfiguration = true;
		_configurationPid = configPid;
		if("null".equals(_configurationPid)) {
			_configurationPid = CommunicationConstant.LOCALE_CONFIGURATION_PID_ALIASE;
		}
		_configurationId = configId;
	}

	/**
	 * Gibt die Konfigurationsparameter des Remote-Modus zurück.
	 *
	 * @return die Konfigurationsparameter des Remote-Modus
	 */
	public final Object[] getRemoteModeParameter() {
		Object[] objs = null;
		if(!_localConfiguration) {
			objs = new Object[3];
			objs[0] = _configDataTransmitterAddress;
			objs[1] = new Integer(_configDataTransmitterSubAddress);
			objs[2] = _configurationPid;
		}
		return objs;
	}

	/**
	 * Setzt den Datenverteilersbetriebsmodus auf den Remote-Modus.
	 *
	 * @param configDataTransmitterAddress die Adresse des Datenverteilers wo die Konfiguration angemeldet ist.
	 * @param configDataTransmitterSubAddress
	 *                                     Datenverteilersubadresse für die Konfigurationsanbindung
	 * @param configurationPid             Pid der Konfiguration
	 */
	public final void setRemoteModeParameter(
			String configDataTransmitterAddress, int configDataTransmitterSubAddress, String configurationPid
	) {
		_localConfiguration = false;
		_configDataTransmitterAddress = configDataTransmitterAddress;
		_configDataTransmitterSubAddress = configDataTransmitterSubAddress;
		_configurationPid = configurationPid;
		if("null".equals(_configurationPid)) {
			_configurationPid = CommunicationConstant.LOCALE_CONFIGURATION_PID_ALIASE;
		}
	}

	
	/**
	 * Gibt die Konfigurationsid zurück
	 *
	 * @return die Konfigurationsid
	 */
	public final long getConfigurationId() {
		return _configurationId;
	}

	
	/**
	 * Setzt der Konfigurationsid auf den neuen Wert.
	 *
	 * @param configurationId Konfigurationsid
	 */
	public final void setConfigurationId(long configurationId) {
		_configurationId = configurationId;
	}

	/**
	 * Gibt der Konfigurationsbenutzername zurück
	 *
	 * @return der Konfigurationsbenutzername
	 */
	public final String getConfigurationUserName() {
		return _configurationUserName;
	}

	/**
	 * Setzt den Konfigurationsbenutzername auf den neuen Wert.
	 *
	 * @param configUserName Konfigurationsbenutzername
	 */
	public final void setConfigurationUserName(String configUserName) {
		_configurationUserName = configUserName;
	}

	/**
	 * Gibt der Konfigurationsbenutzerpasswort zurück
	 *
	 * @return der Konfigurationsbenutzerpasswort
	 */
	public final String getConfigurationUserPassword() {
		return _configurationUserPassword;
	}

	/**
	 * Setzt der Konfigurationsbenutzerpasswort auf den neuen Wert.
	 *
	 * @param configUserPassword das Konfigurationsbenutzerpasswort
	 */
	public final void setConfigurationUserPassword(String configUserPassword) {
		_configurationUserPassword = configUserPassword;
	}


	/**
	 * Gibt der Parametrierungsbenutzername zurück
	 *
	 * @return der Parametrierungsbenutzername
	 */
	public final String getParameterUserName() {
		return _parameterUserName;
	}

	/**
	 * Setzt der Parametrierungsbenutzername auf den neuen Wert.
	 *
	 * @param paramUserName der Parametrierungsbenutzername
	 */
	public final void setParameterUserName(String paramUserName) {
		_parameterUserName = paramUserName;
	}

	/**
	 * Gibt der Parametrierungsbenutzerpasswort zurück
	 *
	 * @return der Parametrierungsbenutzerpasswort
	 */
	public final String getParameterUserPassword() {
		return _parameterUserPassword;
	}

	/**
	 * Setzt der Parametrierungsbenutzerpasswort auf den neuen Wert.
	 *
	 * @param paramUserPassword das Parametrierungsbenutzerpasswort
	 */
	public final void setParameterUserPassword(String paramUserPassword) {
		_parameterUserPassword = paramUserPassword;
	}

	/** Gibt auf der Standardausgabe die möglichen Startargumente einer Datenverteilerapplikation aus. */
	public static void printArgumentsList() {
		System.out.println();
		System.out.println("----------Argumente des Datenverteilers----------");
		System.out.println();
		System.out.println("-rechtePrüfung=ja|nein|alt|neu");
		System.out.println("-zugriffsRechtePlugins=Plugin(Zeichenkette)[,Plugin(Zeichenkette)][,Plugin(Zeichenkette)]...");
		System.out.println("-remoteKonfiguration=Datenverteileradresse(Zeichenkette):Datenverteilersubadresse(Zahl):Konfigurationspid(Zeichenkette)");
		System.out.println("-datenverteilerId=datenverteilerId(Zeichenkette)");
		System.out.println("-benutzer=Benutzername(Zeichenkette)");
		System.out.println("-authentifizierung=Authentifizierungsdateiname(Zeichenkette)");
		System.out.println("-authentifizierungsVerfahren=Authentifizierungsverfahren(Zeichenkette)");
		System.out.println("-timeoutSendeKeepAlive=time(Zahl in Sekunden)");
		System.out.println("-timeoutEmpfangeKeepAlive=time(Zahl in Sekunden)");
		System.out.println("-timeoutNachbar=time(Zahl in Sekunden)");
		System.out.println("-timeoutAntwort=time(Zahl in Sekunden)");
		System.out.println("-davDavPort=port(Zahl)");
		System.out.println("-davAppPort=port(Zahl)");
		System.out.println("-warteAufParametrierung=wert (ja, nein oder Inkarnationsname der Parametrierung)");
		System.out.println("-konfigurationsBereichFuerApplikationsobjekte=konfigurationsbereich (Pid oder Leerstring)");
		System.out.println();
		System.out.println();
		System.out.println("Bemerkungen: ");
		System.out.println();
		System.out.println("Es gibt zwei Startmodi für den Datenverteiler:");
		System.out.println("Modus 1: wenn die Konfiguration sich lokal anmelden muss (ohne -remoteKonfiguration)");
		System.out.println("Modus 2: wenn die Konfiguration über einen anderen Datenverteiler erreichbar ist (-remoteKonfiguration=...)");
	}

	public List<String> getAccessControlPlugins() {
		return Collections.unmodifiableList(_accessControlPlugins);
	}

	public boolean isUserRightsCheckingEnabled() {
		return _userRightsChecking != UserRightsChecking.Disabled;
	}

	void setUserRightsChecking(final UserRightsChecking userRightsChecking) {
		_userRightsChecking = userRightsChecking;
	}

	public UserRightsChecking getUserRightsChecking() {
		return _userRightsChecking;
	}

	/** Zeit in Millisekunden, die gewartet werden soll bevor Verbindungen von anderen Datenverteilern akzeptiert werden dürfen.
	 * @return Zeit in Millisekunden
	 */
	public long getInitialInterDavServerDelay() {
		return _initialInterDavServerDelay;
	}

	/** Zeit in Millisekunden, die gewartet werden soll bevor versucht wird, abgebrochene Verbindungen zu anderen Datenverteilern neu aufzubauen.
	 * @return Zeit in Millisekunden
	 */
	public long getReconnectInterDavDelay() {
		return _reconnectInterDavDelay;
	}

	public void setReconnectInterDavDelay(final long reconnectInterDavDelay) {
		_reconnectInterDavDelay = reconnectInterDavDelay;
	}

	/**
	 * Erzeugt einen neuen Parametersatz für eine Applikationsverbindung.
	 *
	 * @throws MissingParameterException Bei formalen Fehlern beim Lesen der Aufrufargumente oder der Defaultwerte.
	 * @return Parameterobjekt zum Aufbau einer Applikationsverbindung
	 */
	public ClientDavParameters getClientDavParameters() throws MissingParameterException {
		final String configurationPid;
		final String address;
		final int subAddress;
		final String userName;
		final String userPassword;
		final String applicationName;
		final String authentificationProcessName;
		final int maxTelegramSize;
		final long receiveKeepAliveTimeout;
		final long sendKeepAliveTimeout;
		final int outputBufferSize;
		final int inputBufferSize;
		final String communicationProtocolName;
		if(isLocalMode()) {
			// If localmode set the pid of the configuration
			Object[] objects = getLocalModeParameter();
			if(objects == null) {
				throw new IllegalStateException("Inkonsistente Parameter.");
			}
			configurationPid = (String)objects[0];
			address = "127.0.0.1";  // localhost über loopback
			subAddress = getApplicationConnectionsSubAddress();
			userName = "TransmitterLocalApplication@" + System.currentTimeMillis();
			userPassword = "TransmitterLocalApplication";
			applicationName = "TransmitterLocalApplication@" + System.currentTimeMillis();
		}
		else {
			// If remotemode set the adress and sub adress of the destination transmitter
			Object[] objects = getRemoteModeParameter();
			if(objects == null) {
				throw new IllegalStateException("Inkonsistente Parameter.");
			}
			address = (String)objects[0];
			subAddress = ((Integer)objects[1]).intValue();
			configurationPid = (String)objects[2];
			userName = getUserName();
			userPassword = getUserPassword();
			applicationName = "TransmitterRemoteApplication@" + System.currentTimeMillis();
		}

		authentificationProcessName = getAuthentificationProcessName();
		maxTelegramSize = getMaxDataTelegramSize();
		receiveKeepAliveTimeout = getReceiveKeepAliveTimeout();
		sendKeepAliveTimeout = getSendKeepAliveTimeout();
		outputBufferSize = getAppCommunicationOutputBufferSize();
		inputBufferSize = getAppCommunicationInputBufferSize();
		communicationProtocolName = getLowLevelCommunicationName();
		ClientDavParameters clientDavParameters = new ClientDavParameters(
				configurationPid,
				address,
				subAddress,
				userName,
				userPassword,
				applicationName,
				authentificationProcessName,
				maxTelegramSize,
				receiveKeepAliveTimeout,
				sendKeepAliveTimeout,
				outputBufferSize,
				inputBufferSize,
				communicationProtocolName
		);
		// Interne Datenverteilerverbindung darf keine 2. Verbindung benutzen
		clientDavParameters.setUseSecondConnection(false);
		return clientDavParameters;
	}

	public String getLowLevelCommunicationParameters() {
		return _lowLevelCommunicationParameters;
	}

	/**
	 * Bestimmt, ob der Datenverteiler auf die Applikationsfertigmeldung der Parametrierung warten soll.
	 * @return <code>true</code>, falls der Datenverteiler auf die Applikationsfertigmeldung der Parametrierung warten soll
	 */
	public boolean getWaitForParamApp() {
		return _waitForParamApp;
	}

	/**
	 * Bestimmt den Inkarnationsnamen der Parametrierung auf deren Applikationsfertigmeldung gewartet werden soll.
	 * @return Inkarnationsnamen der Parametrierung auf deren Applikationsfertigmeldung gewartet werden soll oder <code>null</code> falls der
	 * Inkarnationsname egal ist oder nicht gewartet werden soll.
	 * @see #getWaitForParamApp()
	 */
	public String getParamAppIncarnationName() {
		return _paramAppIncarnationName;
	}

	/**
	 * Bestimmt die Pid des Konfigurationsbereichs in dem Applikationsobjekte erzeugt werden sollen.
	 * @return Pid des Konfigurationsbereichs in dem Applikationsobjekte erzeugt werden sollen oder Leerstring falls der Default-Bereich der Konfiguration
	 * verwendet werden soll.
	 */
	public String getConfigAreaPidForApplicationObjects() {
		return _configAreaPidForApplicationObjects;
	}

	/**
	 * @author Kappich Systemberatung
	 * @version $Revision: 12959 $
	 */
	public static enum UserRightsChecking {
		Disabled,
		Compatibility_Enabled,
		OldDataModel,
		NewDataModel
	}
}
