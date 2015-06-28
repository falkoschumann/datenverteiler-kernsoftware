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
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.main.ConnectionException;
import de.bsvrz.dav.daf.main.InconsistentLoginException;
import de.bsvrz.dav.daf.main.MissingParameterException;
import de.bsvrz.dav.daf.main.NormalCloser;
import de.bsvrz.dav.daf.main.SystemTerminator;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9157 $
 */
public class SelfClientDavConnection {

	private static final Debug _debug = Debug.getLogger();

	private final DafDataModel _dataModel;

	private final ClientDavConnection _connection;

	public SelfClientDavConnection(final ClientDavParameters parameters) throws DavInitializationException {
		try {
			_connection = new ClientDavConnection(parameters);
			long waitingTime = 0;
			final long startTime = System.currentTimeMillis();
			final long maxWaitingTime = CommunicationConstant.MAX_WAITING_TIME_FOR_CONNECTION;
			DafDataModel dataModel = null;
			while(waitingTime < maxWaitingTime) {
				try {
					_connection.connect();
					_connection.setCloseHandler(new NormalCloser());

					_connection.login();
					dataModel = (DafDataModel)_connection.getDataModel();
					break;
				}
				catch(CommunicationError ex) {
					_debug.warning("Es konnte keine Verbindung zur Konfiguration hergestellt werden", ex);
					try {
						_connection.disconnect(false, "");
						Thread.sleep(CommunicationConstant.SLEEP_TIME_WAITING_FOR_CONNECTION);
						waitingTime = System.currentTimeMillis() - startTime;
					}
					catch(InterruptedException e) {
						throw new DavInitializationException("Unterbrechung beim Warten auf Konfiguration", e);
					}
				}
				catch(ConnectionException e) {
					throw new DavInitializationException("Fehler beim Verbinden mit der Konfiguration", e);
				}
			}
			if(waitingTime >= maxWaitingTime) {
				throw new DavInitializationException("Zeitgrenze überschritten. Es konnte keine Konfigurationsverbindung aufgebaut werden");
			}
			if(dataModel == null) throw new IllegalStateException("dataModel ist null");
			_dataModel = dataModel;
			_connection.setCloseHandler(new SystemTerminator());
		}
		catch(MissingParameterException e) {
			throw new DavInitializationException("Fehlender Parameter", e);
		}
		catch(InconsistentLoginException e) {
			throw new DavInitializationException("Ungültiger Login-Versuch bei lokalem Datenverteiler", e);
		}
	}

	public ClientDavConnection getConnection() {
		return _connection;
	}

	public DafDataModel getDataModel() {
		return _dataModel;
	}
}
