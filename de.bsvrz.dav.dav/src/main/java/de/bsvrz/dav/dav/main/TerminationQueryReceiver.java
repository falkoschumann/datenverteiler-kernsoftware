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

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.dav.communication.appProtocol.T_A_HighLevelCommunicationInterface;
import de.bsvrz.dav.dav.communication.davProtocol.T_T_HighLevelCommunicationInterface;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Verarbeitet Anfragen, Anwendungs-Verbindungen oder Dav-Verbindungen zu terminieren, oder den Datenverteiler zu beenden
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11409 $
 */
class TerminationQueryReceiver implements ClientReceiverInterface {

	private static final Debug _debug = Debug.getLogger();

	private final HighLevelConnectionsManagerInterface _highLevelConnectionsManager;

	public TerminationQueryReceiver(final HighLevelConnectionsManagerInterface highLevelConnectionsManager) {
		_highLevelConnectionsManager = highLevelConnectionsManager;
	}

	@Override
	public void update(final ResultData[] results) {
		for(final ResultData result : results) {
			try {
				final Data data = result.getData();
				if(data != null) {
					final Data.ReferenceArray apps = data.getReferenceArray("Applikationen");
					final Data.ReferenceArray davs = data.getReferenceArray("Datenverteiler");
					for(final Data.ReferenceValue app : apps.getReferenceValues()) {
						removeAppConnection(app.getId());
					}
					for(final Data.ReferenceValue dav : davs.getReferenceValues()) {
						removeDavConnection(dav.getId());
					}
				}
			}
			catch(Exception e) {
				_debug.error("Fehler beim Ausführen einer Terminierungsanfrage:", e);
			}
		}
	}

	private void removeAppConnection(final long applicationId) {
		_debug.info("Erhalte Terminierungsanweisung für Anwendung-ID: " + applicationId);
		final T_A_HighLevelCommunicationInterface connection = _highLevelConnectionsManager.getApplicationConnectionFromId(applicationId);
		if(connection != null) {
			connection.terminate(false, "Terminierungsanfrage über Datenverteiler");
			return;
		}
		_debug.warning("Es konnte zur Terminierung keine verbundene Anwendung mit der ID " + applicationId + " gefunden werden.");
	}

	private void removeDavConnection(final long transmitterId) {
		_debug.info("Erhalte Terminierungsanweisung für Datenverteiler-ID: " + transmitterId);
		if(transmitterId == _highLevelConnectionsManager.getTransmitterId()) {
			_highLevelConnectionsManager.shutdown(false, "Terminierungsanfrage über Datenverteiler");
			return;
		}
		final T_T_HighLevelCommunicationInterface transmitterConnection = _highLevelConnectionsManager.getTransmitterConnectionFromId(transmitterId);
		if(transmitterConnection != null) {
			transmitterConnection.terminate(false, "Terminierungsanfrage über Datenverteiler");
			return;
		}
		_debug.warning("Es konnte zur Terminierung kein verbundener Datenverteiler mit der ID " + transmitterId + " gefunden werden.");
	}
}
