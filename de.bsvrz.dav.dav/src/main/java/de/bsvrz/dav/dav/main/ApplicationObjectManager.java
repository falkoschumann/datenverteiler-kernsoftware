/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.List;

/**
 * Klasse, die Applikationsobjekte für die verbundenen Anwendungen erstellt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11477 $
 */
public class ApplicationObjectManager {

	private static final Debug _debug = Debug.getLogger();

	private final DataModel _dataModel;

	private final MutableSet _applicationsSet;

	private ConfigurationArea _appObjectConfigurationArea;

	private Boolean _canWriteApplicationSet = null; // null = unbekannt, true = schreiben erlaubt, false = schreiben verboten

	/**
	 * Konstruktor
	 *
	 * @param connectionsManager HighLevelConnectionsManagerInterface
	 * @param connection Loakle Datenverteilerverbindung
	 * @param configAreaPidForApplicationObjects Pid des Konfigurationsbereichs für Applikationsobjekte wie in ServerDavParameters angegeben
	 */
	public ApplicationObjectManager(
			final HighLevelConnectionsManagerInterface connectionsManager,
			final ClientDavConnection connection,
			final String configAreaPidForApplicationObjects) {
		_dataModel = connection.getDataModel();
		_applicationsSet = connectionsManager.getDavObject().getMutableSet("Applikationen");

		String defaultAreaPid = connection.getLocalConfigurationAuthority()
				.getConfigurationData(_dataModel.getAttributeGroup("atg.konfigurationsVerantwortlicherEigenschaften"))
				.getTextArray("defaultBereich")
				.getText(0);

		final ConfigurationArea defaultConfigArea = (ConfigurationArea) _dataModel.getObject(defaultAreaPid);

		if(configAreaPidForApplicationObjects == null || configAreaPidForApplicationObjects.length() == 0){
			_appObjectConfigurationArea = defaultConfigArea;
		}
		else {
			_appObjectConfigurationArea = _dataModel.getConfigurationArea(configAreaPidForApplicationObjects);
			if(_appObjectConfigurationArea == null) {
				_debug.warning("Angegebener Konfigurationsbereich für Applikationsobjekte '" + configAreaPidForApplicationObjects + "' nicht gefunden. "
				               + "Es wird der Defaultbereich der Konfiguration verwendet");
				_appObjectConfigurationArea = defaultConfigArea;
			}
			if(!_appObjectConfigurationArea.getConfigurationAuthority().equals(connection.getLocalConfigurationAuthority())) {
				_debug.warning("Angegebener Konfigurationsbereich für Applikationsobjekte '" + configAreaPidForApplicationObjects + "' ist nicht änderbar. "
				               + "Es wird der Defaultbereich der Konfiguration verwendet");
				_appObjectConfigurationArea = defaultConfigArea;
			}
		}
		if(_applicationsSet != null){
			if(canNotWriteApplicationSet()) return;
			try {
				// Menge leeren
				List<SystemObject> elements;
				while((elements = _applicationsSet.getElements()).size() > 0){
					_applicationsSet.remove(elements.toArray(new SystemObject[elements.size()]));
				}
			}
			catch(ConfigurationChangeException e){
				showApplicationSetErrorMessage();
				e.printStackTrace();
			}
		}
	}

	/** Erstellt ein Applikations-Objekt und gibt die Id zurück
	 *
	 * @param typePid Pid des Typs der Applikation
	 * @param name Name der Applikation
	 * @return Applikations-Id oder -1 bei Fehler
	 * @throws ConfigurationChangeException Fehler bei Konfigurationsänderung
	 */
	public long createApplication(final String typePid, final String name) throws ConfigurationChangeException {
		if(_dataModel != null) {
			final DynamicObjectType type = (DynamicObjectType)_dataModel.getType(typePid);
			if(type != null) {
				DynamicObject application = _appObjectConfigurationArea.createDynamicObject(type, "", name);
				if(application != null) {
					addApplicationToObjectSet(application);
					return application.getId();
				}
			}
		}
		return -1L;
	}

	private void addApplicationToObjectSet(final SystemObject applicationObject) {
		if(canNotWriteApplicationSet()) return;
		if(_applicationsSet != null){
			try {
				_applicationsSet.add(applicationObject);
			}
			catch(ConfigurationChangeException e) {
				showApplicationSetErrorMessage();
				e.printStackTrace();
			}
		}
	}

	private void removeApplicationFromObjectSet(final SystemObject applicationObject) {
		if(canNotWriteApplicationSet()) return;
		if(_applicationsSet != null){
			try {
				_applicationsSet.remove(applicationObject);
			}
			catch(ConfigurationChangeException e) {
				showApplicationSetErrorMessage();
				e.printStackTrace();
			}
		}
	}

	private boolean canNotWriteApplicationSet() {
		// Logik analog zu de.bsvrz.puk.config.configFile.datamodel.ConfigMutableSet.loadElementAccessProperties()
		if (_canWriteApplicationSet  != null) return !_canWriteApplicationSet;

		boolean elementChangesAllowed = _dataModel.getConfigurationAuthority().equals(_applicationsSet.getConfigurationArea().getConfigurationAuthority());

		final AttributeGroup atg = _dataModel.getAttributeGroup("atg.dynamischeMenge");
		if(atg != null) {
			final Data data = _applicationsSet.getConfigurationData(atg);
			if(data != null) {
				String managementPid = data.getTextValue("verwaltung").getValueText();
				elementChangesAllowed = _dataModel.getConfigurationAuthority().getPid().equals(managementPid);
			}
		}

		_canWriteApplicationSet = elementChangesAllowed;
		return !_canWriteApplicationSet;
	}

	private void showApplicationSetErrorMessage() {
		if(_canWriteApplicationSet == false){
			return;
		}
		_canWriteApplicationSet = false;
		_debug.warning("Beim Ändern der Menge \"Applikationen\" am Datenverteiler trat ein Problem auf. Bitte verwaltung=\"" + _dataModel.getConfigurationAuthority().getPid() + "\" bei der Menge eintragen.");
	}

	/**
	 * Löscht ein Applikationsobjekt
	 * @param applicationId Applikations-Id
	 */
	public void removeApplication(final long applicationId) {
		final SystemObject applicationObject = _dataModel.getObject(applicationId);
		if(applicationObject != null && applicationObject instanceof ClientApplication) {
			try {
				applicationObject.invalidate();
				removeApplicationFromObjectSet(applicationObject);
			}
			catch(Exception e) {
				_debug.fine("Applikationsobjekt " + applicationObject + " konnte nicht nicht gelöscht werden", e);
			}
		}
	}
}
