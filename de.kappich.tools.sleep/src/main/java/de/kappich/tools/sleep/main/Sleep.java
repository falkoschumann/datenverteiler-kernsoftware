/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.tools.sleep.
 * 
 * de.kappich.tools.sleep is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.tools.sleep is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.tools.sleep; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.tools.sleep.main;

import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Programm, das eine vorgegebene Zeit schläft und sich dann beendet. Die Wartezeit kann mit dem Aufrufparameter -pause= angegeben werden. Defaultwert ist
 * -pause=2s500ms.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 13095 $
 */
public class Sleep {

	private static Debug _debug;

	public static void main(String[] args) {
		ArgumentList argumentList = new ArgumentList(args);
		Debug.init("Sleep", argumentList);
		_debug = Debug.getLogger();
		try {
			ArgumentList.Argument pauseArgument;
			if(argumentList.hasArgument("pause")) {
				pauseArgument = argumentList.fetchArgument("pause");
			}
			else {
				pauseArgument = argumentList.fetchArgument("-pause=2s500ms");
			}
			long pause = pauseArgument.asRelativeTime();
			System.out.print(pause + " Millisekunden Pause");
			System.out.flush();
			Thread.sleep(pause);
			System.out.println();
		}
		catch(InterruptedException e) {
			_debug.warning("Pause frühzeitig beendet");
		}
		catch(IllegalArgumentException e) {
			_debug.error("Benutzung: java de.kappich.tools.sleep.main.Sleep [[-]pause=<relativeZeitangabe>]");
		}
	}
}
