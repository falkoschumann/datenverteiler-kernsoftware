/*
 * Copyright 2010 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.dav.util.accessControl;

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Klasse, die Basisberechtigungen enthält, die jeder Benutzer implizit zugewiesen bekommt. Es gibt keine Möglichkeit, einem Benutzer über die Parametrierung
 * diese Rechte zu nehmen. Bei Bedarf können weitere Berechtigungen hier hinzugefügt werden. Damit eine Aktion erlaubt ist, müssen Attributgruppe und Aspekt
 * übereinstimmen und die jeweilige Aktion muss über den Boolean-Parameter erlaubt sein. Die Angabe einer leeren Liste für Attributgruppen oder Aspekte bedeutet
 * wie üblich, dass alle Attributgruppen bzw. Aspekte ausgewählt sind.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
class ImplicitAccessUnitManager {

	private static final Debug _debug = Debug.getLogger();

	private static final List<AccessUnit> _accessUnits = new ArrayList<AccessUnit>();

	static {
		// Neue Berechtigungen können ab hier eingefügt werden.

		// Basis Anfragen (aus kb.objekteZugriffsrechteBasis)
		_accessUnits.add(
				new AccessUnit(
						Arrays.asList(
								// Attributgruppen
								"atg.konfigurationsBenutzerverwaltungsAnfrageSchnittstelle",
								"atg.konfigurationsAnfrageSchnittstelleSchreibend",
								"atg.betriebsMeldung",
								"atg.konfigurationsSchreibAnfrage",
								"atg.konfigurationsAnfrage",
								"atg.konfigurationsAnfrageSchnittstelleLesend",
								"atg.konfigurationsBereichsverwaltungsAnfrageSchnittstelle"
						), Arrays.asList(
								// Aspekte
								"asp.anfrage",
								"asp.information"
						),
						true,   // Sender
						false,  // Empfänger
						false,  // Quelle
						true   // Senke
				)
		);
		// Basis Antworten (aus kb.objekteZugriffsrechteBasis)
		_accessUnits.add(
				new AccessUnit(
						Arrays.asList(
								// Attributgruppen
								"atg.konfigurationsBenutzerverwaltungsAnfrageSchnittstelle",
								"atg.konfigurationsAnfrageSchnittstelleSchreibend",
								"atg.konfigurationsAntwort",
								"atg.konfigurationsAnfrageSchnittstelleLesend",
								"atg.konfigurationsBereichsverwaltungsAnfrageSchnittstelle",
								"atg.konfigurationsSchreibAntwort"
						), Arrays.asList(
								// Aspekte
								"asp.antwort"
						),
						true,   // Sender
						true,    // Empfänger 
						false,   // Quelle
						true     // Senke
				)
		);
		// Basis Fertigmeldung (aus kb.objekteZugriffsrechteBasis)
		_accessUnits.add(
				new AccessUnit(
						Arrays.asList(
								// Attributgruppen
								"atg.applikationsFertigmeldung"
						), Arrays.asList(
								// Aspekte
								"asp.standard"
						),
						true,   // Sender
						true,   // Empfänger
						true,   // Quelle
						true    // Senke
				)
		);				
	}

	public static boolean isAllowed(final String atg, final String asp, final UserAction action) {
		for(AccessUnit accessUnit : _accessUnits) {
			if(accessUnit.isAllowed(atg,asp,action)) return true;
		}
		return false;
	}

	private static class AccessUnit {

		private List<String> _attributeGroups;

		private List<String> _aspects;

		private boolean _allowSender;

		private boolean _allowReceiver;

		private boolean _allowSource;

		private boolean _allowDrain;

		private AccessUnit(
				final List<String> attributeGroups,
				final List<String> aspects,
				final boolean allowSender,
				final boolean allowReceiver, final boolean allowSource, final boolean allowDrain) {
			_attributeGroups = attributeGroups;
			_aspects = aspects;
			_allowSender = allowSender;
			_allowReceiver = allowReceiver;
			_allowSource = allowSource;
			_allowDrain = allowDrain;
		}

		public boolean isAllowed(final String atg, final String asp, final UserAction action) {
			if(_attributeGroups.size() != 0 && !_attributeGroups.contains(atg)) {
				return false; // Keine Aussage, weil ATG nicht passt.
			}
			if(_aspects.size() != 0 && !_aspects.contains(asp)) {
				return false; // Keine Aussage, weil ASP nicht passt.
			}
			if(action == UserAction.RECEIVER) {
				return _allowReceiver;
			}
			else if(action == UserAction.SENDER) {
				return _allowSender;
			}
			else if(action == UserAction.SOURCE) {
				return _allowSource;
			}
			else if(action == UserAction.DRAIN) {
				return _allowDrain;
			}
			else {
				_debug.error("Unbekannte Aktion: " + action);
				return false;
			}
		}
	}
}
