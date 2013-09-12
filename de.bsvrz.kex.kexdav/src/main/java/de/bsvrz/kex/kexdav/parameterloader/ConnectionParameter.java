/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.parameterloader;

/**
 * Verbindungsparameter mit einem Remote-Datenverteiler
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9084 $
 */
public class ConnectionParameter {
	private final String _hostname;

	private final int _port;

	private final String _user;

	private final String _davPid;

	private final boolean _active;

	/**
	 * Erstellt Verbindungsparameter
	 * @param hostname Hostname oder IP-Adresse
	 * @param port Port
	 * @param user Benutzername
	 * @param davPid Dav-Pid
	 * @param active Ist die Verbindung aktiv?
	 */
	public ConnectionParameter(
			final String hostname, final int port, final String user, final String davPid, final boolean active) {
		_hostname = hostname;
		_port = port;
		_user = user;
		_davPid = davPid;
		_active = active;
	}

	/**
	 * Gibt die IP bzw. den Hostnamen zurück
	 * @return die IP bzw. den Hostnamen
	 */
	public String getIP() {
		return _hostname;
	}

	/**
	 * Gibt den Port zurück
	 * @return den Port
	 */
	public int getPort() {
		return _port;
	}

	/**
	 * Gibt den Benutzer zur Anmeldung zurück
	 * @return den Benutzer zur Anmeldung
	 */
	public String getUser() {
		return _user;
	}

	/**
	 * Gibt die Pid des Datenverteilers zurück
	 * @return die Pid des Datenverteilers
	 */
	public String getDavPid() {
		return _davPid;
	}

	/**
	 * Gibt true zurück, wenn die Verbindung aktiv ist, sonst false
	 * @return true oder false
	 */
	public boolean isActive() {
		return _active;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final ConnectionParameter other = (ConnectionParameter)o;

		if(_active != other._active) return false;
		if(_port != other._port) return false;
		if(_davPid != null ? !_davPid.equals(other._davPid) : other._davPid != null) return false;
		if(_hostname != null ? !_hostname.equals(other._hostname) : other._hostname != null) return false;
		if(_user != null ? !_user.equals(other._user) : other._user != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _hostname != null ? _hostname.hashCode() : 0;
		result = 31 * result + _port;
		result = 31 * result + (_user != null ? _user.hashCode() : 0);
		result = 31 * result + (_davPid != null ? _davPid.hashCode() : 0);
		result = 31 * result + (_active ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RemoteDavConnectionParameter{" + "_ip='" + _hostname + '\'' + ", _port=" + _port + ", _user='" + _user + '\'' + ", _davPid='" + _davPid + '\''
		       + ", _active=" + _active + '}';
	}
}
