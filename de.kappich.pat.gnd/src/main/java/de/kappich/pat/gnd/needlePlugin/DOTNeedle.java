/*
 * Copyright 2009 by Kappich Systemberatung Aachen
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

package de.kappich.pat.gnd.needlePlugin;

import de.kappich.pat.gnd.pluginInterfaces.*;

/**
 * Der Darstellungstyp für Linienobjekte.
 * <p>
 * DOTLine erweitert DefaultDisplayObjectType ausschließlich um die Implementation der abstrakten Methode(n).
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 *
 */

public class DOTNeedle extends DefaultDisplayObjectType implements DisplayObjectType {
	
	/**
	 * Konstruktor mit leerem Namen und leerer Info. Namen und Info können später noch gesetzt werden;
	 * ein Speichern ohne diese Daten ist nicht sinnvoll. 
	 */
	public DOTNeedle() {
		super();
	}
	
	/**
	 * Konstruktor mit vorgegebenem Namen und Info.
	 * 
	 * @param name der unter allen DisplayObjectTypes eindeutige Name
	 * @param info eine Kurzinformation, die z.B. als Tooltipp verwendet wird
	 */
	public DOTNeedle(String name, String info) {
		super();
		_name = name;
		_info = info;
	}
	
	@Override
    public DisplayObjectTypePlugin getDisplayObjectTypePlugin() {
		return new DOTNeedlePlugin();
	}


}
