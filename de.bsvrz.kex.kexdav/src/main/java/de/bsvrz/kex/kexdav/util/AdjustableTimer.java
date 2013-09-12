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

package de.bsvrz.kex.kexdav.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Timer mit einmaliger Ausführung und anpassbarem Delay. Beispiel: Der Timer wird mit 60 Sekunden Delay gestartet, dann triggert er nach 60 Sekunden. Wird nach
 * 5 Sekunden aber ein neuer Delay von 20 Sekunden gesetzt, dann wird der Timer nach weiteren 15 Sekunden triggern (also die angegebenen 20 Sekunden seit dem
 * Zeitpunkt, wo er gestartet wurde). Würde stattdessen nach 20 Sekunden ein Delay von 5 Sekunden gesetzt, wird der Timer sofort getriggert, da die 5 Sekunden
 * seit Aktivierung des Timers bereits vergangen sind.<p/> Der Timer macht keine Vorgaben darüber, in welchem Thread der Task ausgeführt wird. Insbesondere kann
 * bei einem resultierenden Zeitraum<=0 der Task sofort in dem Thread ausgeführt werden, in dem der Konstruktor oder die {@link #adjustDelay(long)}-Methode
 * ausgeführt wird.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9193 $
 */
public class AdjustableTimer {

	private final long _startTime;

	private final Runnable _task;

	private Timer _timer;

	private boolean _finished = false;

	/**
	 * Erstellt einen neuen Timer
	 *
	 * @param delay Dauer in Millisekunden, bis der Task gestartet wird (ab dem Zeitpunkt, wo dieser Konstruktor aufgerufen wird)
	 * @param task  Aufgabe, die ausgeführt werden soll
	 */
	public AdjustableTimer(final long delay, final Runnable task) {
		_startTime = System.currentTimeMillis();
		_task = task;

		setTrigger(delay);
	}

	/**
	 * Ändert die Dauer bis zur Ausführung des Tasks
	 *
	 * @param newDelay neue Dauer in ms. Die Dauer wird immer ab dem Initialisierungszeitpunkt des Timers angegeben, ist newDelay bspw. 12 und der Timer läuft
	 *                 schon 5 Sekunden, werden weitere 7 Sekunden gewartet. Falls diese Wartezeit negativ ist, wird der Task sofort gestartet
	 */
	public synchronized void adjustDelay(final long newDelay) {
		_timer.cancel();

		final long delay = _startTime + newDelay - System.currentTimeMillis();

		setTrigger(delay);
	}

	/** Stoppt den Timer und verhindert, dass der Task in Zukunft durch diesen Timer ausgeführt wird */
	public synchronized void cancel() {
		_finished = true;
		_timer.cancel();
	}

	/**
	 * Erstellt intern einen neuen Timer
	 * @param delay Wartezeit
	 */
	private synchronized void setTrigger(final long delay) {
		if(delay <= 0) {
			runTask();
			return;
		}

		_timer = new Timer(true);
		_timer.schedule(
				new TimerTask() {
					@Override
					public void run() {
						runTask();
					}
				}, delay
		);
	}

	/**
	 * Führt den angegebenen Task aus
	 */
	private synchronized void runTask() {
		if(_finished) return;
		_finished = true;
		_task.run();
	}

	@Override
	public String toString() {
		return "AdjustableTimer{" + "_task=" + _task + '}';
	}
}
