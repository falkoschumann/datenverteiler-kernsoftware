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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataNotSubscribedException;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
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
import de.bsvrz.sys.funclib.dataIdentificationSettings.DataIdentification;
import de.bsvrz.sys.funclib.dataIdentificationSettings.UpdateListener;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Verwaltet die An- und Abmeldungen beim Umparametrieren der Parametrierung.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9144 $
 */

final class ParameterObserver implements UpdateListener {

	/**
	 * DebugLogger für Debug-Ausgaben
	 */
	private static final Debug debug = Debug.getLogger();

	private final ParamApp _paramStandardApp;
	private final SystemObject _paramApp;
	private final ClientDavInterface _connection;
	private final DataModel _config;

	private Aspect _inputAspect;
	private Aspect _outputAspect;
	private final PersistanceHandlerManager _persistanceHandlerManager;
	private final Map _paramObjects;
	private DataIdentification _dataIdentification;
	private AttributeGroup _atgParametrierung;

	/**
	 * Erzeugt ein Objekt vom Typ ParameterObserver
	 */

	public ParameterObserver(ParamApp paramAppStandard,
	                         ClientDavInterface connection,
	                         SystemObject paramApp,
	                         Map paramObjects,
	                         PersistanceHandlerManager persistanceHandlerManager) {
		_paramStandardApp = paramAppStandard;
		_connection = connection;
		_paramApp = paramApp;
		_paramObjects = paramObjects;
		_persistanceHandlerManager = persistanceHandlerManager;
		_config = connection.getDataModel();
		try {
			_atgParametrierung = _config.getAttributeGroup("atg.parametrierung");
		}
		catch (ConfigurationException e) {
			debug.error("Konnte Attributgruppe atg.parametrierung nicht anlegen.");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wird bei Änderung des Parameters für jede Datenidentifikation aufgerufen für
	 * die es einen Eintrag gab oder gibt.
	 *
	 * @param dataIdentification Betroffene Datenidentifikation.
	 * @param oldParameter       Zur Datenidentifikation gehörender Parameterwert
	 *                           vor der Änderung oder <code>null</code> wenn es
	 *                           vor der Änderung keinen spezifischen Eintrag gab.
	 * @param newParameter       Zur Datenidentifikation gehörender Parameterwert
	 *                           nach der Änderung oder <code>null</code> wenn es
	 *                           nach der Änderung keinen spezifischen Eintrag mehr
	 *                           gibt.
	 */
	public void update(final DataIdentification dataIdentification, final Data oldParameter, final Data newParameter) {

		boolean isNewEntry = false; // Übergebener Eintrag ist neu (true) oder wurde geändert (false);

		boolean toParameterNew = false; // Soll für diese DatenIdentifikation jetzt (neu) parametriert werden ?
		boolean toParameterOld = false; // Wurde für diese DatenIdentifikation bisher (alt) parametriert ?

		debug.fine("Parameterupdate (beim Empfang): {(" + dataIdentification + "),(" + newParameter + "),(" + oldParameter + ")}");
		try {
			_inputAspect = _config.getAspect(ParamApp.ASP_INPUT);
			_outputAspect = _config.getAspect(_paramStandardApp.ASP_OUTPUT);

			AttributeGroup atg = dataIdentification.getDataDescription().getAttributeGroup();
			short simVariante = dataIdentification.getDataDescription().getSimulationVariant();

			final DataDescription inputDescription = new DataDescription(atg, _inputAspect, simVariante);
			final DataDescription outputDescription = new DataDescription(atg, _outputAspect, simVariante);

			// Für die weitere Verarbeitung DataIdentifikation um Aspekt ergänzen.
			_dataIdentification = new DataIdentification(dataIdentification.getObject(), outputDescription);
			debug.fine("Parameterupdate (nach Anpassung): {(" + _dataIdentification + "),(" + newParameter + "),(" + oldParameter + ")}");

			if (newParameter == null) {
				// Datensatz ist alt (sonst gäbe es neue Eigenschaften)
				isNewEntry = false;
			}
			else {
				if (newParameter.getUnscaledValue("Parametrieren").getState().getName().equals("Ja")) {
					toParameterNew = true;
				}
			}

			if (oldParameter == null) {
				// Datensatz ist neu (sonst gäbe es bereits alte Eigenschaften)
				isNewEntry = true;
			}
			else {

				if (oldParameter.getUnscaledValue("Parametrieren").getState().getName().equals("Ja")) {
					toParameterOld = true;
				}
			}

			// (Neuer Eintrag UND neue Vorgabe Parametrieren = Ja ) ODER
			// (Alter Eintrag UND neue Vorgabe Parametrieren = Ja UND bisherige Vorgabe Parametrieren = Nein)
			// dann zum Senden- und Empfangen anmelden und (falls vorhanden) persistente Daten versenden.
			// Ausnahme: Parametersatz der Parametrierung ist schon angemeldet und darf nicht ein zweites Mal
			// angemeldet werden.
			if (!(atg.equals(_atgParametrierung) && _paramApp.equals(_dataIdentification.getObject()))) {
				if ((isNewEntry && toParameterNew) || (!isNewEntry && toParameterNew && !toParameterOld)) {
					// Parametersatz zum Senden anmelden
					_connection.subscribeSender(_paramStandardApp,
					                            _dataIdentification.getObject(),
					                            outputDescription,
					                            SenderRole.source());

					// PersistenceHandler für diese DatenIdentifikation holen und merken...
					_paramObjects.put(_dataIdentification, _persistanceHandlerManager.getHandler(_dataIdentification));

					// Persistente Daten versenden
					final ResultData[] results = ((PersistanceHandler) _paramObjects.get(_dataIdentification)).getPersistanceData();
					for(final ResultData resultData : results) {
						if(resultData.getData().isDefined()){
							_connection.sendData(resultData);
						}
						else{
							debug.warning("Persistenter Datensatz kann nicht versendet werden: " + resultData.toString());
						}
					}


					// Parametersatz zum Empfang anmelden
					_connection.subscribeReceiver(_paramStandardApp,
					                              _dataIdentification.getObject(),
					                              inputDescription,
					                              ReceiveOptions.normal(),
					                              ReceiverRole.drain());
				}
			}
			// (Neuer Eintrag UND neue Vorgabe Parametrieren = nein) ODER
			// (Alter Eintrag UND neue Vorgabe Parametrieren = nein UND bisherige Vorgabe Parametrieren = ja)
			// dann zum Senden- und Empfangen abmelden.
			// Ausnahme: Parametersatz der Parametrierung darf nicht abgemeldet werden, sonst ist Umparametrierung nicht
			// mehr möglich.
			if (!(atg.equals(_atgParametrierung) && _paramApp.equals(_dataIdentification.getObject()))) {
				if ((isNewEntry && !toParameterNew) || (!isNewEntry && !toParameterNew && toParameterOld)) {
					// Parametersatz zum Senden abmelden
					_connection.unsubscribeSender(_paramStandardApp,
					                              _dataIdentification.getObject(),
					                              outputDescription);
					// PersistenceHandler für diese DatenIdentifikation freigeben...
					_paramObjects.remove(_dataIdentification);

					// Parametersatz zum Empfang abmelden
					_connection.unsubscribeReceiver(_paramStandardApp,
					                                _dataIdentification.getObject(),
					                                inputDescription);
				}
			}
		}

		catch (DataNotSubscribedException e) {
			debug.error("Versuch, Daten ohne Anmeldung zu senden{(" +
			            _dataIdentification + "),(" + newParameter + "),(" + oldParameter + ")}");
			e.printStackTrace();
			throw new RuntimeException(e);

		}
		catch (SendSubscriptionNotConfirmed sendSubscriptionNotConfirmed) {
			debug.error("Sendeversuch als Sender ohne Sendesteuerung.");
			sendSubscriptionNotConfirmed.printStackTrace();
			throw new RuntimeException(sendSubscriptionNotConfirmed);

		}
		catch (ConfigurationException e) {
			debug.error("Zugriff auf Konfiguration fehlgeschlagen.");
			e.printStackTrace();
			throw new RuntimeException(e);

		}
		catch (OneSubscriptionPerSendData oneSubscriptieonPerSendData) {
			debug.error("Versuch der Mehrfachanmeldung von Daten:{(" +
			            _dataIdentification + "),(" + newParameter + "),(" + oldParameter + ")}");
			oneSubscriptieonPerSendData.printStackTrace();
			throw new RuntimeException(oneSubscriptieonPerSendData);
		}
	}

	public String toString() {
		return "de.kappich.puk.param.main.ParameterObserver{" +
		        "debug=" + debug +
		        ", _paramStandardApp=" + _paramStandardApp +
		        ", _connection=" + _connection +
		        ", _config=" + _config +
		        ", _inputAspect=" + _inputAspect +
		        ", _outputAspect=" + _outputAspect +
		        ", _persistanceHandlerManager=" + _persistanceHandlerManager +
		        ", _paramObjects=" + (_paramObjects == null ? null : "size:" + _paramObjects.size() + _paramObjects) +
		        ", _dataIdentification=" + _dataIdentification +
		        "}";
	}

}
