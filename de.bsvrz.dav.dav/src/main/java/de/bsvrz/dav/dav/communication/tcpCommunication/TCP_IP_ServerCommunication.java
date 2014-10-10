/*
 * Copyright 2007 by Kappich Systemberatung Aachen
 * Copyright 2004 by Kappich+Kniﬂ Systemberatung, Aachen
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

package de.bsvrz.dav.dav.communication.tcpCommunication;

import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.dav.daf.communication.tcpCommunication.TCP_IP_Communication;
import de.bsvrz.dav.daf.communication.lowLevel.ConnectionInterface;
import de.bsvrz.dav.daf.main.CommunicationError;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import de.bsvrz.dav.daf.communication.lowLevel.ServerConnectionInterface;

/**
 * TCP/IP-Implementierung des Interfaces {@link de.bsvrz.dav.daf.communication.lowLevel.ServerConnectionInterface}.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11481 $
 */
public class TCP_IP_ServerCommunication implements ServerConnectionInterface {

	/** Der Debug-Logger. */
	private static final Debug _debug = Debug.getLogger();

	/** Der Server-Socket des Datenverteilers */
	protected ServerSocket _socket;

	/** Erzeugt eine Instanz dieser Klasse. */
	public TCP_IP_ServerCommunication() {
	}

	@Override
	public void connect(int subAdressNumber) throws CommunicationError {
		try {
			_socket = new ServerSocket();
			_socket.setReuseAddress(true);
			_socket.bind(new InetSocketAddress(subAdressNumber));

			_debug.info("TCP-Server erwartet Verbindungen, " + _socket.getLocalSocketAddress());
		}
		catch(IOException ex) {
			final String msg = "Fehler beim anlegen eines TCP-Server-Sockets auf Port " + subAdressNumber;
			_debug.error(msg, ex);
			throw new CommunicationError(msg + ": " + ex);
		}
	}

	@Override
	public void disconnect() {
		try {
			final ServerSocket mySocket = _socket;
			if(mySocket != null) {
				_debug.info("TCP-Server wird beendet, " + mySocket.getLocalSocketAddress());
				_socket.close();
			}
		}
		catch(IOException ex) {
			ex.printStackTrace();
			_socket = null;
		}
	}

	@Override
	public ConnectionInterface accept() {
		try {
			if(_socket != null) {
				// Blocks until a connection occurs:
				Socket socket = _socket.accept();
				if(socket != null) {
					_debug.info("TCP-Verbindung passiv aufgebaut, " + socket.getLocalSocketAddress() + " <-- " + socket.getRemoteSocketAddress());
					return getConnectionTo(socket);
				}
			}
		}
		catch(IOException e) {
			try {
				_socket.close();
				return null;
			}
			catch(IOException ex) {
				return null;
			}
		}
		return null;
	}

	@Override
	public ConnectionInterface getPlainConnection() {
		return new TCP_IP_Communication();
	}

	public ConnectionInterface getConnectionTo(Socket socket) {
		return new TCP_IP_Communication(socket);
	}

	@Override
	public String getPlainConnectionName() {
		return TCP_IP_Communication.class.getName();
	}
}
