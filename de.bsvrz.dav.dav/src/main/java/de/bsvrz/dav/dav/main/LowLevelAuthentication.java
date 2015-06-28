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

import de.bsvrz.dav.daf.communication.lowLevel.AuthentificationProcess;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;
import de.bsvrz.dav.daf.main.impl.ConfigurationManager;

import java.util.Arrays;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 11409 $
 */
public class LowLevelAuthentication implements LowLevelAuthenticationInterface {

	private final ServerDavParameters _serverDavParameters;

	private final ClientDavParameters _clientDavParameters;

	private final long _transmitterId;

	private SelfClientDavConnection _selfClientDavConnection;

	private AuthentificationComponent _authenticationComponent;


	public LowLevelAuthentication(
			final ServerDavParameters serverDavParameters,
			final ClientDavParameters clientDavParameters,
			final long transmitterId,
			final AuthentificationComponent authenticationComponent) {
		_serverDavParameters = serverDavParameters;
		_clientDavParameters = clientDavParameters;
		_transmitterId = transmitterId;
		_authenticationComponent = authenticationComponent;
		_selfClientDavConnection = null;
	}

	/**
	 * @return die Benutzerid wenn er berechtigt ist sonst -1
	 */
	@Override
	public long isValidUser(
			final String userName,
			final byte[] encryptedPassword,
			final String text,
			final AuthentificationProcess authenticationProcess,
			final String userTypePid) {
		// if local configuration
		if(CommunicationConstant.CONFIGURATION_TYPE_PID.equals(userTypePid)) {
			if(userName.equals(_serverDavParameters.getConfigurationUserName())) {
				final String password = _serverDavParameters.getConfigurationUserPassword();
				final byte[] _encriptedPassword = authenticationProcess.encrypt(password, text);
				if(Arrays.equals(encryptedPassword, _encriptedPassword)) {
					return 0;
				}
			}
		}
		else if(CommunicationConstant.PARAMETER_TYPE_PID.equals(userTypePid)) {
			if(userName.equals(_serverDavParameters.getParameterUserName())) {
				final String password = _serverDavParameters.getParameterUserPassword();
				final byte[] _encriptedPassword = authenticationProcess.encrypt(password, text);
				if(Arrays.equals(encryptedPassword, _encriptedPassword)) {
					return 0;
				}
			}
		}
		else if(_serverDavParameters.isLocalMode()) {
			if(userName.equals(_clientDavParameters.getUserName())) {
				final String password = _clientDavParameters.getUserPassword();
				final byte[] _encriptedPassword = authenticationProcess.encrypt(password, text);
				if(Arrays.equals(encryptedPassword, _encriptedPassword)) {
					return _transmitterId;
				}
			}
		}
		// Ask the configuration
		if(_selfClientDavConnection != null) {
			final ConfigurationManager configurationManager = _selfClientDavConnection.getDataModel().getConfigurationManager();
			try {
				return configurationManager.isValidUser(
						userName, encryptedPassword, text, authenticationProcess.getName()
				);

			}catch(RuntimeException e){
				e.printStackTrace();
				return -1;
			}


		}
		return -1L;
	}

	public void setSelfClientDavConnection(final SelfClientDavConnection selfClientDavConnection) {
		_selfClientDavConnection = selfClientDavConnection;
	}

	@Override
	public AuthentificationComponent getAuthenticationComponent() {
		return _authenticationComponent;
	}
}
