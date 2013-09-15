/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.kappich.pat.gnd.gnd;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.pat.sysbed.main.ApplicationInterface;
import de.bsvrz.pat.sysbed.main.GenericTestMonitorApplication;
import de.bsvrz.pat.sysbed.plugins.api.ButtonBar;
import de.bsvrz.pat.sysbed.plugins.api.DataIdentificationChoice;
import de.bsvrz.pat.sysbed.plugins.api.DialogInterface;
import de.bsvrz.pat.sysbed.plugins.api.ExternalModuleAdapter;
import de.bsvrz.pat.sysbed.plugins.api.settings.KeyValueObject;
import de.bsvrz.pat.sysbed.plugins.api.settings.SettingsData;
import de.bsvrz.pat.sysbed.preselection.lists.PreselectionLists;
import de.bsvrz.sys.funclib.debug.Debug;
import de.kappich.pat.gnd.viewManagement.View;
import de.kappich.pat.gnd.viewManagement.ViewManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Eine Klasse, um die GND als GTM-Plugin zur Verfügung zu stellen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9286 $
 */
public class GNDPlugin extends ExternalModuleAdapter implements SelectionListener {

	/** speichert den Dialog des GND-Plugins */
	private static GNDPluginDialog _dialog;

	private String _tooltipText;

	private static final Debug _debug = Debug.getLogger();

	/** Kennzeichnet, ob eine neue Instanz des GND geöffnet werden soll. */
	private boolean _newInstanceOfGnd = false;

	private JCheckBox _checkInstance;

	public void change(SettingsData settingsData) {

		_dialog = new GNDPluginDialog();
		_dialog.setSettings(settingsData);
	}

	@Override
	public boolean isPreselectionValid(SettingsData settingsData) {

		SystemObjectType type = getApplication().getConnection().getDataModel().getType("typ.geoReferenzObjekt");

		final List<SystemObject> objects = settingsData.getObjects();

		if(objects != null) {
			for(SystemObject systemObject : objects) {
				if(! systemObject.getType().inheritsFrom(type)) {
					_tooltipText = "Es muss ein Georeferenzobjekt ausgewählt werden.";
					return false;
				}
			}
		}
		_tooltipText = "Auswahl übernehmen";
		return true;
	}

	public String getButtonText() {

		return "GND starten";
	}

	public String getModuleName() {

		return "GND-Plugin";
	}

	public String getTooltipText() {

		return _tooltipText;
	}

	public void startModule(SettingsData settingsData) {

		_dialog = new GNDPluginDialog();
		_dialog.setDataIdentification(settingsData);
	}

	public void startSettings(SettingsData settingsData) {

		_dialog = new GNDPluginDialog();
		_dialog.startSettings(settingsData);
	}

	private class GNDPluginDialog implements DialogInterface {

		ViewManager _viewManager = ViewManager.getInstance();

		/** speichert den Dialog */
		private JDialog _settingsDialog = null;

		/** speichert eine Instanz der Datenidentifikationsauswahl */
		private DataIdentificationChoice _dataIdentificationChoice;

		private JComboBox _displayViewsComboBox;

		/**
		 * Getter für die Ansicht.
		 *
		 * @return der Ansichtsname
		 */
		public String getView() {

			return (String)_displayViewsComboBox.getSelectedItem();
		}

		/**
		 * Setter für die Ansicht.
		 *
		 * @param view der Ansichtsname
		 */
		public void setView(String view) {

			_displayViewsComboBox.setSelectedItem(view);
		}

		/** diese Methode schließt den Dialog */
		public void doCancel() {

			_settingsDialog.setVisible(false);
			_settingsDialog.dispose();
		}

		/** Durch betätigen des "OK"-Buttons wird die GND gestartet und dieser Dialog wird geschlossen. Die Parameter werden gespeichert. */
		public void doOK() {

			SettingsData settingsData = getSettings("");
			startSettings(settingsData);
			doCancel();
			saveSettings(settingsData);
		}

		/**
		 * diese Methode speichert die Parameter
		 *
		 * @param title Titel dieser Konfiguration
		 */
		public void doSave(String title) {

			SettingsData settingsData = getSettings(title);
			saveSettings(settingsData);
		}

		/**
		 * Erstellt die Einstellungsdaten.
		 *
		 * @param title der Name für die Einstellungen
		 *
		 * @return die Einstellungsdaten
		 */
		private SettingsData getSettings(String title) {

			Class<GNDPlugin> moduleClass = GNDPlugin.class;
			List<SystemObjectType> objectTypes = _dataIdentificationChoice.getObjectTypes();
			AttributeGroup atg = _dataIdentificationChoice.getAttributeGroup();
			Aspect asp = _dataIdentificationChoice.getAspect();
			List<SystemObject> objects = _dataIdentificationChoice.getObjects();

			SettingsData settingsData = new SettingsData(getModuleName(), moduleClass, objectTypes, atg, asp, objects);
			settingsData.setTitle(title);
			settingsData.setSimulationVariant(_dataIdentificationChoice.getSimulationVariant());
			settingsData.setTreePath(_dataIdentificationChoice.getTreePath());
			settingsData.setKeyValueList(getKeyValueList());

			return settingsData;
		}

		/** Erstellt den Dialog. Bestandteil ist die Datenidentifikation und die Anmeldeoption, bestehend aus der Rolle und der Anmeldeart. */
		private void createDialog() {

			_settingsDialog = new JDialog();
			_settingsDialog.setTitle(getButtonText());
			_settingsDialog.setResizable(false);

			Container pane = _settingsDialog.getContentPane();
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

			// Datenidentifikationsauswahl-Panel
			final List<SystemObjectType> types = new LinkedList<SystemObjectType>();
			DataModel configuration = getConnection().getDataModel();
			types.add(configuration.getType("typ.konfigurationsObjekt"));
			types.add(configuration.getType("typ.dynamischesObjekt"));
			_dataIdentificationChoice = new DataIdentificationChoice(null, types);
			pane.add(_dataIdentificationChoice);

			// Darstellungsoptionen
			JPanel displayOptionsPanel = new JPanel();
			displayOptionsPanel.setLayout(new BoxLayout(displayOptionsPanel, BoxLayout.X_AXIS));
			displayOptionsPanel.setBorder(BorderFactory.createTitledBorder("Ansichtseinstellung"));
			JLabel displayOptionsLabel = new JLabel("Ansicht: ");

			Set<String> viewNames = _viewManager.getViewNames();
			String[] viewArray = viewNames.toArray(new String[viewNames.size()]);

			_displayViewsComboBox = new JComboBox(viewArray);
			final String startViewName = GenericNetDisplay.getStartViewNameFromPreferences();
			if((startViewName != null) && (_viewManager.hasView(startViewName))) {
				_displayViewsComboBox.setSelectedItem(startViewName);
			}
			else if(viewArray.length > 0) {
				_displayViewsComboBox.setSelectedIndex(0);
			}
			displayOptionsLabel.setLabelFor(_displayViewsComboBox);
			displayOptionsPanel.add(displayOptionsLabel);
			displayOptionsPanel.add(Box.createHorizontalStrut(5));
			displayOptionsPanel.add(_displayViewsComboBox);

			pane.add(displayOptionsPanel);

			// Instanzptionen
			JPanel gndOptionsPanel = new JPanel();

			//gndOptionsPanel.setLayout(new BoxLayout(gndOptionsPanel, BoxLayout.X_AXIS));
			gndOptionsPanel.setLayout(new BorderLayout());
			gndOptionsPanel.setBorder(BorderFactory.createTitledBorder("Fensteroptionen"));
			_checkInstance = new JCheckBox("GND im neuen Fenster öffnen.");

			if(_checkInstance.isSelected()) {
				_newInstanceOfGnd = true;
			}
			else {
				_newInstanceOfGnd = false;
			}

			GenericNetDisplay instance = GenericNetDisplay.getInstance();
			if(instance == null || (instance != null && ! instance.isVisible())) {
				_checkInstance.setSelected(true);
			}
			else {
				_checkInstance.setSelected(false);
			}

			_checkInstance.addActionListener(
					new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent e) {

							_newInstanceOfGnd = _checkInstance.isSelected();
						}
					}
			);

			gndOptionsPanel.add(Box.createHorizontalStrut(5));
			gndOptionsPanel.add(_checkInstance, BorderLayout.CENTER);

			pane.add(gndOptionsPanel);

			// untere Buttonleiste
			final ButtonBar buttonBar = new ButtonBar(this);
			_settingsDialog.getRootPane().setDefaultButton(buttonBar.getAcceptButton());
			pane.add(buttonBar);
		}

		/**
		 * Diese Methode zeigt den Dialog an und trägt die Einstellungsdaten in die entsprechenden Felder ein.
		 *
		 * @param data die Einstellungsdaten
		 */
		public void setSettings(final SettingsData data) {

			if(_settingsDialog == null) {
				createDialog();
			}
			_dataIdentificationChoice.setDataIdentification(
					data.getObjectTypes(), data.getAttributeGroup(), data.getAspect(), data.getObjects(), data.getSimulationVariant()
			);
			_dataIdentificationChoice.showTree(getApplication().getTreeNodes(), getApplication().getConnection(), data.getTreePath());

			List<KeyValueObject> keyValueList = data.getKeyValueList();
			for(KeyValueObject keyValueObject : keyValueList) {
				String key = keyValueObject.getKey();
				String value = keyValueObject.getValue();

				if(key.equals("view")) {
					setView(value);
				}
				else if(key.equals("instance")) {
					_checkInstance.setSelected(new Boolean(value));
				}
			}

			showDialog();
		}

		/** Durch diese Methode wird der Dialog angezeigt. */
		private void showDialog() {

			_settingsDialog.setLocation(70, 70);
			_settingsDialog.pack();
			_settingsDialog.setVisible(true);
		}

		/**
		 * Startet die GND anhand der Einstellungsdaten.
		 *
		 * @param settingsData die Einstellungsdaten
		 */
		public void startSettings(SettingsData settingsData) {

			final List<KeyValueObject> keyValueList = settingsData.getKeyValueList();
			final List<SystemObject> objects = settingsData.getObjects();

			Thread threadTest = new Thread(
					new Runnable() {

						public void run() {

							try {
								View view = new View();
								String viewName = null;
								if(keyValueList.size() != 0) {
									for(KeyValueObject keyValueObject : keyValueList) {
										String key = keyValueObject.getKey();
										if(key.equals("view")) {
											viewName = keyValueObject.getValue();
											view = _viewManager.getView(viewName);
											break;
										}
										else if(key.equals("instance")) {
											_newInstanceOfGnd = new Boolean(keyValueObject.getValue());
										}
									}
								}
								else {
									viewName = (String)_displayViewsComboBox.getSelectedItem();
									view = _viewManager.getView(viewName);
								}
								if(view == null) {
									if(viewName == null) {
										viewName = "???";
									}
									_debug.warning("GNDPlugin: eine Ansicht namens '" + viewName + "' kann nicht gefunden werden.");
									return;
								}

								final ClientDavInterface connection = getApplication().getConnection();

								GenericNetDisplay genericNetDisplay = GenericNetDisplay.getInstance();
								// wenn es noch keine Instanz der GND-Klasse gibt oder Ctrl gedrückt wird, wird ein neuer GND erzeugt
								if(genericNetDisplay == null || _newInstanceOfGnd || (genericNetDisplay != null && ! genericNetDisplay.isVisible())) {
									GenericNetDisplay gnd = new GenericNetDisplay(view, connection, objects, false);
									gnd.addSelectionListener(GNDPlugin.this);
									gnd.setVisible(true);
								}
								// existiert ein GND wird er mit der ausgewählten Ansicht in den Vordergrund geholt
								else {
									String nameOfActualView = genericNetDisplay.getView().getName();
									if(! nameOfActualView.equals(view.getName())) {
										genericNetDisplay.setSplitPaneFromView(view);
									}
									genericNetDisplay.toFront();
								}
							}
							catch(StopInitializationException e) {
								return;
							}
						}
					}

			);
			threadTest.start();
		}

		/**
		 * Sammelt alle Parameter des Dialogs.
		 *
		 * @return Liste aller Parameter des Dialogs
		 */
		private List<KeyValueObject> getKeyValueList() {

			List<KeyValueObject> keyValueList = new LinkedList<KeyValueObject>();
			keyValueList.add(new KeyValueObject("view", getView()));
			keyValueList.add(new KeyValueObject("instance", String.valueOf(_checkInstance.isSelected())));

			return keyValueList;
		}

		/**
		 * Mit dieser Methode können die Datenidentifikationsdaten übergeben werden. Der Dialog wird mit Default-Werten dargestellt.
		 *
		 * @param settingsData enthält die ausgewählte Datenidentifikation
		 */
		public void setDataIdentification(SettingsData settingsData) {

			if(_settingsDialog == null) {
				createDialog();
			}
			_dataIdentificationChoice.setDataIdentification(
					settingsData.getObjectTypes(),
					settingsData.getAttributeGroup(),
					settingsData.getAspect(),
					settingsData.getObjects(),
					settingsData.getSimulationVariant()
			);
			_dataIdentificationChoice.showTree(getApplication().getTreeNodes(), getApplication().getConnection(), settingsData.getTreePath());
			showDialog();
		}
	}

	public void setSelectedObjects(Collection<SystemObject> systemObjects) {

		ArrayList<SystemObject> arrayListSystemObjects = new ArrayList<SystemObject>(systemObjects);

		ApplicationInterface application = getApplication();
		if(application instanceof GenericTestMonitorApplication) {
			GenericTestMonitorApplication genericTestMonitorApplication = (GenericTestMonitorApplication)application;
			PreselectionLists preselectionLists = genericTestMonitorApplication.getPreselectionLists();
			preselectionLists.setPreselectedObjects(arrayListSystemObjects);
			preselectionLists.setPreselectedAspects(null);
			preselectionLists.setPreselectedAttributeGroups(null);
			preselectionLists.setPreselectedObjectTypes(null);
		}
	}

	@SuppressWarnings("serial")
	static class StopInitializationException extends RuntimeException {

		StopInitializationException() {

		}
	}
}
