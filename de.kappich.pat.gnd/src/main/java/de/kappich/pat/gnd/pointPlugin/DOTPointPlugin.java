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
package de.kappich.pat.gnd.pointPlugin;

import de.kappich.pat.gnd.displayObjectToolkit.DOTDefinitionDialogFrame;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.pluginInterfaces.DOTDefinitionDialog;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectPainter;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.pointPlugin.DOTPoint.PrimitiveFormType;

import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObjectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Die Selbstbeschreibung des Punkte-Plugins.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8091 $
 *
 */
public class DOTPointPlugin implements DisplayObjectTypePlugin {
	
	/**
	 * Konstruiert eine Selbstbeschreibung des Plugins.
	 */
	public DOTPointPlugin() {
		super();
	}
	
	public String getName() {
		return new String("Punkt");
    }

	public DOTDefinitionDialog getDialog( final DOTDefinitionDialogFrame dotDefinitionDialogFrame) {
	    return new DOTPointDialog(dotDefinitionDialogFrame);
    }

	public DisplayObjectType getDisplayObjectType() {
		return new DOTPoint();
    }

	public DisplayObjectPainter getPainter() {
		return new DOTPointPainter();
    }

	public String[] getPrimitiveFormTypes() {
	    String[] o = new String[] {"Punkt", "Rechteck", "Kreis", "Halbkreis", "Textdarstellung" };
	    return o;
    }

	public DOTProperty[] getProperties(Object o) {
		if ( o == null) {
			return new DOTProperty[] {};
		}
		if ( o instanceof PrimitiveFormType) {
			PrimitiveFormType pft = (PrimitiveFormType) o;
			if ( pft.equals( PrimitiveFormType.PUNKT)) {
				return new DOTProperty[] { DOTProperty.DURCHMESSER, DOTProperty.FARBE};
			} else if ( pft.equals( PrimitiveFormType.RECHTECK)) {
				return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
			} else if ( pft.equals( PrimitiveFormType.KREIS)) {
				return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
			} else if ( pft.equals( PrimitiveFormType.HALBKREIS)) {
				return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
			} else if ( pft.equals( PrimitiveFormType.TEXTDARSTELLUNG)) {
				return new DOTProperty[] { DOTProperty.FARBE, DOTProperty.GROESSE, DOTProperty.TEXT, DOTProperty.TEXTSTIL};
			} 
		}
		if ( !(o instanceof String)) {
			return null;
		}
		String s = (String) o;
		if ( s.equals( PrimitiveFormType.PUNKT.getName())) {
			return new DOTProperty[] { DOTProperty.DURCHMESSER, DOTProperty.FARBE};
		} else if ( s.equals( PrimitiveFormType.RECHTECK.getName())) {
			return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
		} else if ( s.equals( PrimitiveFormType.KREIS.getName())) {
			return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
		} else if ( s.equals( PrimitiveFormType.HALBKREIS.getName())) {
			return new DOTProperty[] { DOTProperty.FUELLUNG, DOTProperty.STRICHBREITE, DOTProperty.TRANSPARENZ};
		} else if ( s.equals( PrimitiveFormType.TEXTDARSTELLUNG.getName())) {
			return new DOTProperty[] { DOTProperty.FARBE, DOTProperty.GROESSE, DOTProperty.TEXT, DOTProperty.TEXTSTIL};
		} 
		return null;
    }
	
	public String getGeometryType() {
		return "typ.punkt";
	}
	
	public boolean isSystemObjectTypeSupported(DataModel configuration, SystemObjectType systemObjectType) {
		if ( _supportedSystemObjectTypePIDs == null) {
			initSupportedSystemObjectTypePIDs( configuration);
		}
		return _supportedSystemObjectTypePIDs.contains( systemObjectType.getPid());
    }
	
	private void initSupportedSystemObjectTypePIDs(DataModel configuration) {
		final List<SystemObjectType> geoReferenceObjectTypes = new ArrayList<SystemObjectType>();
		final SystemObjectType geoReferenceObjectType = configuration.getType("typ.punkt");
		geoReferenceObjectTypes.add( geoReferenceObjectType);
		for( int i = 0; i < geoReferenceObjectTypes.size(); i++) {
			geoReferenceObjectTypes.addAll( geoReferenceObjectTypes.get(i).getSubTypes());
		}
		_supportedSystemObjectTypePIDs = new HashSet<String>();
		for ( SystemObjectType systemObjectType : geoReferenceObjectTypes) {
			_supportedSystemObjectTypePIDs.add( systemObjectType.getPid());
		}
    }
	
	private static Set<String> _supportedSystemObjectTypePIDs = null;

}
