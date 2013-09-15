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

import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.kappich.pat.gnd.displayObjectToolkit.DOTDefinitionDialogFrame;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.pluginInterfaces.*;

/**
 * Die Selbstbeschreibung des Linien-Plugins.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 *
 */
public class DOTNeedlePlugin implements DisplayObjectTypePlugin {
	
	/**
	 * Konstruiert eine Selbstbeschreibung des Plugins.
	 */
	public DOTNeedlePlugin() {
		super();
	}
	
	public String getName() {
		return "Nadel";
    }

	public DOTDefinitionDialog getDialog( final DOTDefinitionDialogFrame dotDefinitionDialogFrame) {
		return new DOTNeedleDialog();
    }

	public DisplayObjectType getDisplayObjectType() {
	    return new DOTNeedle();
    }

	public DisplayObjectPainter getPainter() {
	    return new DOTNeedlePainter();
    }

	public String[] getPrimitiveFormTypes() {
		return new String[0];
    }

	public DOTProperty[] getProperties(Object o) {
		DOTProperty[] properties = null;
		if ( o == null) {
			properties = new DOTProperty[] {};
		}
		return properties;
    }
	
	public String getGeometryType() {
		return "typ.konfigurationsObjekt";
	}

	public boolean isSystemObjectTypeSupported(DataModel configuration, SystemObjectType systemObjectType) {
		return true;
    }
	
}
