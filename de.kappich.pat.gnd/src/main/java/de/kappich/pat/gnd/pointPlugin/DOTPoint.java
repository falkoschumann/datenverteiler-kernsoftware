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

import de.kappich.pat.gnd.displayObjectToolkit.DOTManager;
import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DOTSubscriptionData;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItem;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDOTItemManager;
import de.kappich.pat.gnd.displayObjectToolkit.DynamicDefinitionComponent;
import de.kappich.pat.gnd.displayObjectToolkit.DOTItemManager.DisplayObjectTypeItemWithInterval;
import de.kappich.pat.gnd.gnd.LegendTreeNodes;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
import de.kappich.pat.gnd.utils.Interval;

import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.sys.funclib.debug.Debug;

import java.lang.Double;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.table.TableModel;

/**
 * Der Darstellungstyp für Punktobjekte.
 * <p>
 * Ein DOTPoint implementiert das Interface DisplayObjectType für das Plugin für Punktobjekte. 
 * Dieser GND-interne Darstellungstyp ist bei weitem der umfangreichste und seine Implementation 
 * beruht NICHT auf DefaultDisplayObjectType.
 * 
 * Jeder DOTPoint hat einen Namen, einen Infotext, einen Verschiebungsfaktor (-länge) und
 * eine interne Variable, die anzeigt, ob eine Verbingslinie zwischen der Lage in der Karte
 * und dem verschobenen Objekt gezeichnet werden soll. Weiterhin kann er beliebig viele
 * {@link PrimitiveForm Grundfiguren} enthalten, die je nach ihrem Typ statische oder
 * dynamische Eigenschaften besitzen. Der DOTPoint besitzt selbst keine Visualisierungs-Eigenschaften.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public class DOTPoint implements DisplayObjectType, DOTManager.DOTChangeListener {
	
	/**
	 * Ein Konstruktor ohne Vorgaben.
	 */
	public DOTPoint() {
		super();
		_name = "";
		_info = "";
		_translationFactor = 0.;
		_joinByLine = false;
		DOTManager.getInstance().addDOTChangeListener( this);
	}
	
	/**
	 * Ein Konstruktor mit punkt-spezifischen Vorgaben.
	 * 
	 * @param name der Name des Darstellungstyp-Objekts
	 * @param info die Kurzinfo des Darstellungstyp-Objekts
	 * @param translationFactor der globale Verschiebungsfaktor
	 * @param joinByLine <code>true</code> genau dann, wenn eine Verbindungslinie gezeichnet werden soll
	 */
	public DOTPoint(String name, String info, double translationFactor, boolean joinByLine) {
		super();
		_name = name;
		_info = info;
		_translationFactor = translationFactor;
		_joinByLine = joinByLine;
		DOTManager.getInstance().addDOTChangeListener( this);
	}
	public String getName() {
		return _name;
	}
	public void setName( String name) {
		_name = name;
	}
	public String getInfo() {
		return _info;
	}
	public void setInfo( String info) {
		_info = info;
	}
	
	/**
	 * Der Getter für den Verschiebungsfaktor bzw. -länge.
	 * 
	 * @return der Verschiebungsfaktor
	 */
	public Double getTranslationFactor() {
		return _translationFactor;
	}
	
	/**
	 * Der Setter für den Verschiebungsfaktor bzw. -länge.
	 * 
	 * @param translationFactor der Verschiebungsfaktor
	 */
	public void setTranslationFactor(Double translationFactor) {
		_translationFactor = translationFactor;
	}
	
	/**
	 * Gibt <code>true</code> zurück, wenn die Lage in der Karte mit dem verschobenen Objekt durch
	 * eine Verbindungslinie verbunden werden soll.
	 * 
	 * @return soll eine Verbindungslinie gezeichnet werden
	 */
	public boolean isJoinByLine() {
		return _joinByLine;
	}
	
	/**
	 * Setzt die interne Variable, die bestimmt, ob die Lage in der Karte mit dem 
	 * verschobenen Objekt durch eine Verbindungslinie verbunden werden soll.
	 * 
	 * @param legt fest, ob eine Verbindungslinie gezeichnet werden soll
	 */
	public void setJoinByLine(boolean joinByLine) {
		_joinByLine = joinByLine;
	}
	
	public Boolean isPropertyStatic( String primitiveFormName, DOTProperty property) {
		PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "isPropertyStatic");
		final Boolean isStatic = primitiveForm.isPropertyStatic( property);
		if ( isStatic == null) {
			throw new IllegalArgumentException("DOTPoint.isPropertyStatic wurde für eine unbekannte Eigenschaft aufgerufen.");
		}
		return isStatic;
	}
	
	public void setPropertyStatic( String primitiveFormName, DOTProperty property, boolean b) {
		PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "setPropertyStatic");
		primitiveForm.setPropertyStatic( property, b);
	}
	
	public Object getValueOfStaticProperty( String primitiveFormName, DOTProperty property) {
		PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "getValueOfStaticProperty");
		return primitiveForm.getValueOfStaticProperty( property);
	}
	
	public void setValueOfStaticProperty( String primitiveFormName, DOTProperty property, Object value) {
		PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "setValueOfStaticProperty");
		primitiveForm.setValueOfStaticProperty( property, value);
	}
	
	public void setValueOfDynamicProperty( String primitiveFormName, 
			DOTProperty property, DisplayObjectTypeItem dItem, 
			Double lowerBound, Double upperBound) {
		PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "setValueOfDynamicProperty");
		primitiveForm.setValueOfDynamicProperty( property, dItem, lowerBound, upperBound);
	}
	
	public void initializeFromPreferences(Preferences prefs) {
		_name = prefs.name();
		Preferences infoPrefs = prefs.node( prefs.absolutePath() + "/info");
		_info = infoPrefs.get(INFO, "");
		Preferences translationFactorPrefs = prefs.node( prefs.absolutePath() + "/translationFactor");
		_translationFactor = translationFactorPrefs.getDouble( TRANSLATION_FACTOR, 0.);
		Preferences joinByLinePrefs = prefs.node( prefs.absolutePath() + "/joinByLine");
		_joinByLine = joinByLinePrefs.getBoolean( JOIN_BY_LINE, false);
		
		Preferences primitiveFormsPrefs = prefs.node( prefs.absolutePath() + "/primitiveForms");
		String[] primitiveFormChilds;
		try {
			primitiveFormChilds = primitiveFormsPrefs.childrenNames();
		}
		catch(BackingStoreException e) {
			
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
		for ( String primitiveFormName : primitiveFormChilds) {
			PrimitiveForm primitiveForm = new PrimitiveForm();
			Preferences primitiveFormPrefs = primitiveFormsPrefs.node( primitiveFormsPrefs.absolutePath() + "/" + primitiveFormName);
			primitiveForm.initializeFromPreferences( primitiveFormPrefs);
			addPrimitiveForm( primitiveForm);
		}
	}
	
	public void deletePreferences(Preferences prefs) {
		Preferences classPrefs = prefs.node(prefs.absolutePath() + "/DOTPoint");
		Preferences objectPrefs = classPrefs.node( classPrefs.absolutePath() + "/" + getName());
		try {
			objectPrefs.removeNode();
		}
		catch(BackingStoreException e) {
			throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
		}
	}
	
	public void putPreferences(Preferences prefs) {
		deletePreferences(prefs);
		Preferences classPrefs = prefs.node(prefs.absolutePath() + "/DOTPoint");
		Preferences objectPrefs = classPrefs.node( classPrefs.absolutePath() + "/" + getName());
		Preferences infoPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/info");
		infoPrefs.put(INFO, getInfo());
		Preferences translationFactorPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/translationFactor");
		translationFactorPrefs.putDouble( TRANSLATION_FACTOR, _translationFactor);
		Preferences joinByLinePrefs = objectPrefs.node( objectPrefs.absolutePath() + "/joinByLine");
		joinByLinePrefs.putBoolean( JOIN_BY_LINE, _joinByLine);
		
		Preferences primitiveFormPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/primitiveForms");
		for ( PrimitiveForm primitiveForm : _primitiveForms.values()) {
			primitiveForm.putPreferences( primitiveFormPrefs);
		}
	}
	
	public DisplayObjectTypePlugin getDisplayObjectTypePlugin() {
		return new DOTPointPlugin();
	}
	
	public LegendTreeNodes getLegendTreeNodes() {
		LegendTreeNodes legendTreeNodes = new LegendTreeNodes();
		LegendTreeNodes.LegendTreeNode node = null;
		int depth = 0;
		int newDepth;
		for ( PrimitiveForm primitiveForm : _primitiveForms.values()) {
			if ( !primitiveForm.hasDynamicProperties()) {
				continue;
			}
			newDepth = 0;
			if ( node != null) {
				legendTreeNodes.add(node, depth-newDepth);
			}
			node = new LegendTreeNodes.LegendTreeNode( primitiveForm.getName(), primitiveForm.getInfo());
			depth = newDepth;
			for ( DOTProperty property : primitiveForm.getDynamicProperties()) {
				newDepth = 1; 
				legendTreeNodes.add(node, depth-newDepth);
				node = new LegendTreeNodes.LegendTreeNode( property.toString(), null);
				depth = newDepth;
				final DynamicDOTItemManager dynamicDOTItemManager = primitiveForm.getDynamicDOTItemManager(property);
				final int size = dynamicDOTItemManager.size();
				for ( int rowIndex = 0; rowIndex < size; rowIndex++) {
					newDepth = 2;
					legendTreeNodes.add(node, depth-newDepth);
					String description = dynamicDOTItemManager.get( rowIndex).getItem().getDescription();
					node = new LegendTreeNodes.LegendTreeNode( description, null);
					depth = newDepth;
				}
			}
		}
		newDepth = 0;
		if ( node != null) {
			legendTreeNodes.add(node, depth-newDepth);
		}
		return legendTreeNodes;
	}
	
	public Set<DOTSubscriptionData> getSubscriptionData() {
		Set<DOTSubscriptionData> sdSet = new HashSet<DOTSubscriptionData>();
		for ( PrimitiveForm primitiveForm : _primitiveForms.values()) {
			for ( DOTProperty property : primitiveForm.getDynamicProperties()) {
				final DynamicDOTItemManager dynamicDOTItemManager = primitiveForm.getDynamicDOTItemManager(property);
				sdSet.addAll( dynamicDOTItemManager.getSubscriptionData());
			}
		}
		return sdSet;
	}
	
	public List<String> getAttributeNames( String primitiveFormName, DOTProperty property, 
			DOTSubscriptionData subscriptionData) {
		final PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "getAttributeNames");
		return primitiveForm.getAttributeNames( property, subscriptionData);
	}
	
	public DisplayObjectTypeItem isValueApplicable( 
			String primitiveFormName, DOTProperty property, 
			DOTSubscriptionData subscriptionData, 
			String attributeName, double value) {
		final PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "isValueApplicable");
		return primitiveForm.isValueApplicable(property,subscriptionData, attributeName, value);
	}
	
	public DisplayObjectTypeItem getDisplayObjectTypeItemForState(
			final String primitiveFormName, final DOTProperty property,
			final DOTSubscriptionData subscriptionData, final DataState dataState) {
		final PrimitiveForm primitiveForm = getPrimitiveForm( primitiveFormName, "getDisplayObjectTypeItemForState");
		if ( primitiveForm.isPropertyStatic(property)) {
			return null;
		}
		final DynamicDOTItemManager dynamicDOTItemManager = primitiveForm.getDynamicDOTItemManager(property);
		final String keyString1;
		if ( dataState.equals( DataState.NO_DATA)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_DATEN_STATUS);
		} else if ( dataState.equals( DataState.NO_SOURCE)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_QUELLE_STATUS);
		} else if ( dataState.equals( DataState.NO_RIGHTS)) {
			keyString1 = dynamicDOTItemManager.getKeyString( subscriptionData, 
					DynamicDefinitionComponent.KEINE_RECHTE_STATUS);
		} else {
			keyString1 = null;
		}
		if ( keyString1 != null) {	// einer der Substati hat gezogen ...
			final TreeMap<Interval<Double>, DynamicDOTItem> treeMap1 = 
				dynamicDOTItemManager.get( keyString1);
			if ( (treeMap1 != null) && (treeMap1.size() == 1)) { // ... und ist definiert
				return treeMap1.values().toArray( new DisplayObjectTypeItem[1])[0];
			}
		}
		// den übergreifenden Status überprüfen
		final String keyString2 = dynamicDOTItemManager.getKeyString( subscriptionData, 
				DynamicDefinitionComponent.LEERE_DATEN_STATUS);
		final TreeMap<Interval<Double>, DynamicDOTItem> treeMap2 = 
			dynamicDOTItemManager.get( keyString2);
		if ( (treeMap2 != null) && (treeMap2.size() == 1)) { // ... er ist definiert
			return treeMap2.values().toArray( new DisplayObjectTypeItem[1])[0];
		} else {	// ... dann bleibt nur der Default
			return DynamicDOTItem.NO_DATA_ITEM;
		}
	}
	
	public DisplayObjectType getCopy(String name) {
		DOTPoint copy;
		if ( name == null) {
			copy = new DOTPoint ( _name, _info, new Double(_translationFactor), new Boolean(_joinByLine));
		} else {
			copy = new DOTPoint ( name, _info, new Double(_translationFactor), new Boolean(_joinByLine));
		}
		for ( PrimitiveForm primitiveForm : _primitiveForms.values()) {
			PrimitiveForm copyOfPrimitiveForm = primitiveForm.getCopy();
			copy._primitiveForms.put( copyOfPrimitiveForm.getName(), copyOfPrimitiveForm);
		}
		return copy;
	}
	
	/**
	 * Gibt für jede bei Grunndfiguren von DOTPoint verwendeten DOTProperty einen Default-Wert zurück.
	 * 
	 * @param property die Eigenschaft
	 * @return der Default-Wert
	 */
	public static Object getDefaultValue( DOTProperty property) {
		if ( property.equals( DOTProperty.DURCHMESSER)) {
			return new Double(0.);
		} else if ( property.equals( DOTProperty.FARBE)) {
			return Color.BLACK;
		} else if ( property.equals( DOTProperty.FUELLUNG)) {
			return Color.WHITE;
		} else if ( property.equals( DOTProperty.GROESSE)) {
			return new Double(0.);
		} else if ( property.equals( DOTProperty.STRICHBREITE)) {
			return new Double(0.);
		} else if ( property.equals( DOTProperty.TEXT)) {
			return DOTPointPainter.GET_NAME_OR_PID_OR_ID;
		} else if ( property.equals( DOTProperty.TEXTSTIL) ) {
			return Font.ITALIC;
		} else if ( property.equals( DOTProperty.TRANSPARENZ)) {
			return new Integer(0);
		}
		return null;
	}
	
	/**
	 * Fügt eine Grundfigur hinzu.
	 * 
	 * @param primitiveForm die Grundfigur
	 */
	public void addPrimitiveForm( PrimitiveForm primitiveForm) {
		if ( _primitiveForms.containsKey( primitiveForm.getName())) {
			throw new IllegalArgumentException( "DOTPoint.addPrimitiveForm(): eine Grundfigur mit dem Namen '" 
					+ primitiveForm.getName() + "' existiert bereits.");
		}
		_primitiveForms.put( primitiveForm.getName(), primitiveForm);
	}
	
	/**
	 * Macht ein Update auf die bereits vorhandene Grundfigur oder fügt sie andernfalls hinzu.
	 * 
	 * @param primitiveForm die Grundfigur
	 */
	public void putPrimitiveForm( PrimitiveForm primitiveForm) {
		_primitiveForms.put( primitiveForm.getName(), primitiveForm);
	}
	
	/**
	 * Gibt die Grundfigur zurück.
	 * 
	 * @param primitiveFormName der Name der Grundfigur
	 * @param methodName der Name der aufrufenden Methode (ursprünglich für eine Fehlermeldung vorgesehen)
	 * @return die Grundfigur oder <code>null</code>
	 */
	private PrimitiveForm getPrimitiveForm( String primitiveFormName, final String methodName) {
		final PrimitiveForm primitiveForm = _primitiveForms.get( primitiveFormName);
		return primitiveForm;
	}
	
	/**
	 * Gibt die genannte Grundfigur zurück.
	 * 
	 * @param primitiveFormName der Name der Grundfigur
	 * @return die Grundfigur oder <code>null</code>
	 */
	public PrimitiveForm getPrimitiveForm( String primitiveFormName) {
		return _primitiveForms.get( primitiveFormName);
	}
	
	/**
	 * Gibt alle Grundfiguren zurück.
	 * 
	 * @return alle Grundfiguren
	 */
	public Collection<PrimitiveForm> getPrimitiveForms() {
		return Collections.unmodifiableCollection( _primitiveForms.values());
	}
	
	/**
	 * Gibt die Namen aller Grundfiguren zurück.
	 * 
	 * @return alle Grundfigurnamen
	 */
	public Set<String> getPrimitiveFormNames() {
		return Collections.unmodifiableSet( _primitiveForms.keySet());
	}
	
	/**
	 * Gibt den Typ der genannten Grundfigur zurück, oder aber einen leeren String.
	 * 
	 * @param primitiveFormName der Grundfigurname
	 * @return der Grundfigurtyp
	 */
	public String getPrimitiveFormType( final String primitiveFormName) {
		final PrimitiveForm primitiveForm = _primitiveForms.get( primitiveFormName);
		if ( primitiveForm == null) {
			return "";
		}
		return primitiveForm.getType().getName();
	}
	
	/**
	 * Gibt den Infotext der benannten Grundfigur zurück, oder aber einen leeren String.
	 * 
	 * @param primitiveFormName der Grundfigurname
	 * @return die Kurzinfo
	 */
	public String getPrimitiveFormInfo( final String primitiveFormName) {
		final PrimitiveForm primitiveForm = _primitiveForms.get( primitiveFormName);
		if ( primitiveForm == null) {
			return "";
		}
		return primitiveForm.getInfo();
	}
	
	/**
	 * Entfernt die benannte Grundfigur.
	 * 
	 * @param primitiveFormName der Grundfigurname
	 */
	public void removePrimitiveForm ( final String primitiveFormName) {
		_primitiveForms.remove( primitiveFormName);
	}
	
	/**
	 * Gibt alle dynamischen Eigenschaften der benannten Grundfigur zurück.
	 * 
	 * @param primitiveFormName der Grundfigurname
	 * @return alle dynamischen Eigenschaften der Grundfigur
	 */
	public List<DOTProperty> getDynamicProperties( final String primitiveFormName) {
		final PrimitiveForm primitiveForm = _primitiveForms.get( primitiveFormName);
		if ( primitiveForm == null) {
			return new ArrayList<DOTProperty>();
		}
		return primitiveForm.getDynamicProperties();
	}
	
	/**
	 * Gibt das Tabellenmodel der durch die übergebenen Werte beschriebenen Eigenschaft zurück.
	 * 
	 * @param primitiveForm die Grundfigur
	 * @param property die Eigenschaft
	 * @return das TableModel
	 */
	public TableModel getTableModel( PrimitiveForm primitiveForm, DOTProperty property) {
		DynamicDOTItemManager dynamicDOTItemManager = primitiveForm._dynamicDOTItemManagers.get( property);
		return dynamicDOTItemManager;
	}
	
	/**
	 * Gibt die Indizes aller in Konflikt stehenden Zeilen des Tabellenmodells an. Ein Konflikt
	 * besteht, wenn zwei Zeilen sich hinsichtlich der Wertebereiche überlappen.
	 * 
	 * @param primitiveForm die Grundfigur
	 * @param property die Eigenschaft
	 * @return die Indizes von in Konflikten stehenden Zeilen
	 */
	public Set<Integer> getConflictingRows( PrimitiveForm primitiveForm, DOTProperty property) {
		final DynamicDOTItemManager dynamicDOTItemManager = primitiveForm._dynamicDOTItemManagers.get( property);
		if ( dynamicDOTItemManager == null) {
			return null;
		}
		return dynamicDOTItemManager.getConflictingRows();
	}
	
	/**
	 * PrimitiveForm-Objekte sind die Grundfiguren in der Darstellung der DOTPoints. 
	 * <p>
	 * Jede Grundfigur hat einen Namen, einen von fünf vorgegebenen Typen (Rechteck, Kreis, Halbkreis,
	 * Textdarstellung oder Punkt), einen Infotext, einen Punkt in der Ebene, der einen Verschiebungvektor 
	 * beschreibt, und abhängig vom Typ spezifische definierende Eigenschaften (Höhe, Breite, Radius, 
	 * Orientierung, Durchmesser usw.
	 * <p>
	 * Die Klasse ist statisch, damit sie statische Methoden haben kann (s. {@link #getDefaultSpecificInformation).
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8080 $
	 *
	 */
	public static class PrimitiveForm {
		
		/**
		 * Legt eine leere Grundfigur an. 
		 */
		public PrimitiveForm() {
			super();
		}
		
		/**
		 * Legt eine Grundfigur mit den vorgegebenen Informationen an.
		 * 
		 * @param name der Name
		 * @param type der Typ
		 * @param info die Kurzinfo
		 * @param translation der lokale Verschiebungsvektor
		 * @param specificInformation spezifische Informationen
		 */
		public PrimitiveForm(String name, 
				PrimitiveFormType type, 
				String info, 
				Point2D.Double translation, 
				Map<String,Object> specificInformation) {
			super();
			_nameOfPrimitiveForm = name;
			_typeOfPrimitiveForm = type;
			_infoOfPrimitiveForm = info;
			_translation = translation;
			if ( specificInformation != null) {
				_specificInformation = specificInformation;
			}
			checkSpecificInformation();
			initCollections();
		}
		
		/**
		 * Gibt den Namen der Grundfigur zurück.
		 * 
		 * @return den Namen
		 */
		public String getName() {
			return _nameOfPrimitiveForm;
		}
		
		/**
		 * Setzt den Namen der Grundfigur.
		 * 
		 * @param name der neue Name
		 */
		public void setName(String name) {
			_nameOfPrimitiveForm = name;
		}
		
		/**
		 * Gibt den Typ der Grundfigur zurück.
		 * 
		 * @return der Grundfigurtyp
		 */
		public PrimitiveFormType getType() {
			return _typeOfPrimitiveForm;
		}
		
		/**
		 * Setzt den typ der Grundfigur.
		 * 
		 * @param type der neue Grundfigurtyp
		 */
		public void setType(PrimitiveFormType type) {
			_typeOfPrimitiveForm = type;
		}
		
		/**
		 * Gibt den Infotext zurück.
		 * 
		 * @return die Kurzinfo
		 */
		public String getInfo() {
			return _infoOfPrimitiveForm;
		}
		
		/**
		 * Setzt den Infotext.
		 * 
		 * @param die neue Kurzinfo
		 */
		public void setInfo(String info) {
			_infoOfPrimitiveForm = info;
		}
		
		/**
		 * Gibt den die lokale Verschiebung beschreibenden Vektor zurück.
		 * 
		 * @return den Verschiebungsvektor
		 */
		public Point2D.Double getTranslation() {
			return _translation;
		}
		
		/**
		 * Setzt den die lokale Verschiebung beschreibenden Vektor.
		 * 
		 * @param der neue Verschiebungsvektor
		 */
		public void setTranslation(Point2D.Double translation) {
			_translation = translation;
		}
		
		/**
		 * Gibt die spezifische definierende Eigenschaft mit dem übergebenen Namen zurück.
		 * 
		 * @param name der Name der spezifischen Eigenschaft
		 * @return die spezifischen Eigenschaft
		 */
		public Object getSpecificInformation( String name) {
			return _specificInformation.get(name);
		}
		
		/**
		 * Setzt die spezifische definiernde Eigenschaft mit dem übergebenen Namen.
		 * 
		 * @param name der Name
		 * @param o die Eigenschaft
		 */
		public void setSpecificInformation ( String name, Object o) {
			if ( _specificInformation.containsKey( name)) {
				_specificInformation.put( name, o);
			}
		}
		
		/**
		 * Gibt den Wert <code>true</true> zurück, wenn die übergebene Eigenschaft statisch ist,
		 * <code>false</code>, wenn sie dynamisch ist, und <code>null</code> wenn sie nicht
		 * bei dieser Grundfigur auftritt. 
		 * 
		 * @return <code>true</code> genau dann, wenn die Eigenschaft statisch ist
		 */
		public Boolean isPropertyStatic( DOTProperty property) {
			return _isStaticMap.get( property);
		}
		
		/**
		 * Legt fest, ob die übergebene Eigenschaft statisch oder dynamisch ist.
		 * 
		 * @param property die Eigenschaft
		 * @param b der neue Wert
		 */
		public void setPropertyStatic(DOTProperty property, boolean b) {
			if ( b) {
				final boolean wasDynamic = _dynamicDOTItemManagers.containsKey( property);
				if ( wasDynamic) {
					Object value = DOTPoint.getDefaultValue(property);
					if ( value == null) {
						throw new IllegalArgumentException();
					}
					_staticPropertyValues.put( property, value);
				}
			}
			_isStaticMap.put( property, b);
		}
		
		/**
		 * Gibt <code>true</code> zurück, wenn die Grundfigur mindestens ein dynamische Eigenschaft besitzt.
		 * 
		 * @return gibt es dynamische Eigenschaften?
		 */
		public boolean hasDynamicProperties() {
			for ( boolean isStatic : _isStaticMap.values()) {
				if ( !isStatic) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Gibt eine Liste aller dynamischen Eigenschaften der Grundfigur zurück.
		 * 
		 * @return alle dynamischen Eigenschaften
		 */
		public List<DOTProperty> getDynamicProperties() {
			List<DOTProperty> dynamicPropertyList = new ArrayList<DOTProperty>();
			for ( DOTProperty property : _isStaticMap.keySet()) {
				if ( !_isStaticMap.get( property)) {
					dynamicPropertyList.add( property);
				}
			}
			return dynamicPropertyList;
		}
		
		/**
		 * Gibt den Wert (Farbe, Tranzparens, Textstil etc.) der statischen Eigenschaft der Grundfigur zurück.
		 * 
		 * @param property die Eigenschaft
		 * @return den Wert der statischen Eigenschaft
		 */
		public Object getValueOfStaticProperty( DOTProperty property) {
			if ( _isStaticMap.containsKey(property)) {
				return _staticPropertyValues.get( property);
			} else {
				return DOTPoint.getDefaultValue(property);
			}
		}
		
		/**
		 * Setzt den Wert (Farbe, Tranzparens, Textstil etc.) der statischen Eigenschaft der Grundfigur.
		 * 
		 * @param property die Eigenschaft
		 * @param value der Wert der Eigenschaft
		 */
		public void setValueOfStaticProperty( DOTProperty property, Object value) {
			if ( property.equals( DOTProperty.TEXTSTIL)) {
				if ( value instanceof String) {
					final String s = (String) value;
					if ( s.equals( DOTPointDialog.BOLD_FONT)) {
						_staticPropertyValues.put( property, Font.BOLD);
						return;
					} else if ( s.equals( DOTPointDialog.ITALIC_FONT)) {
						_staticPropertyValues.put( property, Font.ITALIC);
						return;
					} else if ( s.equals( DOTPointDialog.STANDARD_FONT)) {
						_staticPropertyValues.put( property, Font.PLAIN);
						return;
					} else {
						throw new IllegalArgumentException( "DOTPoint.setValueOfStaticProperty(): unbekannter Fonttyp " + s);
					}
				}
			} else if ( property.equals( DOTProperty.GROESSE)) {
				if ( value instanceof Double) {
					final Double d = (Double) value;
					_staticPropertyValues.put( property, d.intValue());
				}
			}
			_staticPropertyValues.put( property, value);
		}
		
		/**
		 * Setzt den Wert (Farbe, Tranzparens, Textstil etc.) der dynamsichen Eigenschaft der Grundfigur.
		 * 
		 * @param property die Eigenschaft
		 * @param dItem eine Item
		 * @param lowerBound die untere Schranke
		 * @param upperBound die obere Schranke
		 */
		public void setValueOfDynamicProperty( DOTProperty property, 
				DisplayObjectTypeItem dItem, Double lowerBound, Double upperBound) {
			DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
			if ( dynamicDOTItemManager == null) {
				dynamicDOTItemManager = new DynamicDOTItemManager();
				_dynamicDOTItemManagers.put( property, dynamicDOTItemManager);
				
			}
			if ( !(dItem instanceof DynamicDOTItem)) {
				return;
			}
			DynamicDOTItem ldItem = (DynamicDOTItem) dItem;
			final Interval<Double> interval;
			if ( lowerBound == null || upperBound == null) {
				interval = new Interval<Double> ( Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
			} else {
				interval = new Interval<Double> ( lowerBound, upperBound);
			}
			dynamicDOTItemManager.insert( interval, ldItem);
		}
		
		/**
		 * Initialisiert die Grundfigur aus den Präferenzen.
		 * 
		 * @param prefs der Knoten, unter dem die Initialisierung beginnt
		 */
		public void initializeFromPreferences ( Preferences prefs) {
			_nameOfPrimitiveForm = prefs.name();
			Preferences typePrefs = prefs.node( prefs.absolutePath() + "/type");
			_typeOfPrimitiveForm = PrimitiveFormType.getPrimitiveFormType( typePrefs.get(TYPE, ""));
			Preferences infoPrefs = prefs.node( prefs.absolutePath() + "/info");
			_infoOfPrimitiveForm = infoPrefs.get( INFO, "");
			Preferences translationPrefs = prefs.node( prefs.absolutePath() + "/translation");
			double x = translationPrefs.getDouble( TRANSLATION_X, 0.);
			double y = translationPrefs.getDouble( TRANSLATION_Y, 0.);
			_translation = new Point2D.Double( x, y);
			
			initPreferencesOfSpecificInformation( prefs);
			initPreferencesOfStaticProperties( prefs);
			initPreferencesOfDynamicProperties( prefs);
		}
		
		private void initPreferencesOfSpecificInformation( Preferences prefs) {
			Preferences specificPrefs = prefs.node( prefs.absolutePath() + "/specific");
			String[] specificChilds;
			try {
				specificChilds = specificPrefs.childrenNames();
			}
			catch(BackingStoreException e) {
				
				throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
			}
			for ( String specificChild : specificChilds) {
				Preferences valuePrefs = specificPrefs.node( specificPrefs.absolutePath() + "/" + specificChild);
				String type = valuePrefs.get( TYPE, "");
				Object value = null;
				if ( type.equals( "Integer")) {
					value = valuePrefs.getInt( VALUE, 0);
				} else if ( type.equals( "Double")) {
					value = valuePrefs.getDouble( VALUE, 0.);
				} else if ( type.equals( "String")) {
					value = valuePrefs.get( VALUE, "");
				}
				if ( value != null) {
					_specificInformation.put( specificChild, value);
				}
			}
			checkSpecificInformation();
		}
		
		private void initPreferencesOfStaticProperties( Preferences prefs) {
			Preferences staticPrefs = prefs.node( prefs.absolutePath() + "/static");
			String[] staticChilds;
			try {
				staticChilds = staticPrefs.childrenNames();
			}
			catch(BackingStoreException e) {
				_debug.error( "Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden, " + 
						"BackingStoreException:" + e.toString());
				return;
			}
			for ( String staticChild : staticChilds) {
				final DOTProperty property = DOTProperty.getProperty( staticChild);
				if ( property == null) {
					continue;
				}
				Preferences propertyPrefs = staticPrefs.node( staticPrefs.absolutePath() + "/" + staticChild);
				_isStaticMap.put( property, true);
				final DynamicDOTItem dynamicDOTItem = new DynamicDOTItem( propertyPrefs);
				_staticPropertyValues.put( property, dynamicDOTItem.getPropertyValue());
			}
		}
		
		private void initPreferencesOfDynamicProperties( Preferences prefs) {
			Preferences dynamicPrefs = prefs.node( prefs.absolutePath() + "/dynamic");
			String[] dynamicChilds;
			try {
				dynamicChilds = dynamicPrefs.childrenNames();
			}
			catch(BackingStoreException e) {
				_debug.error( "Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden, " + 
						"BackingStoreException:" + e.toString());
				return;
			}
			for ( String dynamicChild : dynamicChilds) {
				DOTProperty property = DOTProperty.getProperty( dynamicChild);
				if ( property == null) {
					_debug.error( "Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden.");
					continue;
				}
				Preferences propertyPrefs = dynamicPrefs.node( dynamicPrefs.absolutePath() + "/" + dynamicChild);
				_isStaticMap.put( property, false);
				DynamicDOTItemManager dynamicDOTItemManager = new DynamicDOTItemManager();
				_dynamicDOTItemManagers.put( property, dynamicDOTItemManager);
				
				String[] intervalNames;
				try {
					intervalNames = propertyPrefs.childrenNames();
				}
				catch(BackingStoreException e) {
					_debug.error( "Ein benutzer-definierter Darstellungstyp kann nicht initialisiert werden, " + 
							"BackingStoreException:" + e.toString());
					continue;
				}
				for ( String child: intervalNames) {
					if ( child.startsWith("interval")) {
						Preferences objectItemPrefs = propertyPrefs.node( propertyPrefs.absolutePath() + "/" + child);
						final DynamicDOTItem dynamicItem;
						try {
							dynamicItem = new DynamicDOTItem( objectItemPrefs);
						} catch (IllegalAccessError iae) {
							_debug.error( "Ein benutzer-definierter Darstellungstyp kann nicht vollständig initialisiert werden.");
							continue;
						}
						if ( dynamicItem.isValid()) {
							setValueOfDynamicProperty( property, dynamicItem, 
									objectItemPrefs.getDouble(LOWER_BOUND, Double.MAX_VALUE),
									objectItemPrefs.getDouble(UPPER_BOUND, Double.MIN_VALUE));
						}
					}
				}
			}
		}
		
		/**
		 * Löscht die Präferenzen der Grundfigur.
		 * 
		 * @param prefs der Knoten, unter dem gelöscht wird
		 */
		public void deletePreferences( Preferences prefs) {
			Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
			try {
				objectPrefs.removeNode();
			}
			catch(BackingStoreException e) {
				
				throw new UnsupportedOperationException("Catch-Block nicht implementiert - BackingStoreException", e);
			}
		}
		
		/**
		 * Speichert die Präferenzen der Grundfigur.
		 * 
		 * @param prefs der Knoten, unter dem die Speicherung beginnt
		 */
		public void putPreferences ( Preferences prefs) {
			deletePreferences( prefs);
			Preferences objectPrefs = prefs.node( prefs.absolutePath() + "/" + getName());
			Preferences typePrefs = objectPrefs.node(objectPrefs.absolutePath() + "/type");
			typePrefs.put(TYPE, getType().getName());
			Preferences infoPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/info");
			infoPrefs.put( INFO, getInfo());
			Preferences translationPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/translation");
			translationPrefs.putDouble( TRANSLATION_X, _translation.getX());
			translationPrefs.putDouble( TRANSLATION_Y, _translation.getY());
			
			Preferences specificPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/specific");
			putPreferencesForSpecificInformation( specificPrefs);
			
			Preferences staticPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/static");
			putPreferencesOfStaticProperties( staticPrefs);
			
			Preferences dynamicPrefs = objectPrefs.node( objectPrefs.absolutePath() + "/dynamic");
			putPreferencesOfDynamicProperties( dynamicPrefs);
		}
		
		private void putPreferencesForSpecificInformation( Preferences prefs) {
			for ( String specific : _specificInformation.keySet()) {
				Preferences specificPrefs = prefs.node( prefs.absolutePath() + "/" + specific);
				Object object = _specificInformation.get( specific);
				if ( object instanceof Integer) {
					specificPrefs.put( TYPE, "Integer");
					specificPrefs.putInt( VALUE, (Integer) object);
				} else if ( object instanceof Double) {
					specificPrefs.put( TYPE, "Double");
					specificPrefs.putDouble( VALUE, (Double) object);
				} else if ( object instanceof String) {
					specificPrefs.put( TYPE, "String");
					specificPrefs.put( VALUE, (String) object);
				} else {
					_debug.warning("PrimitiveForm: eine spezifische Information konnte nicht gespeichert werden.");
				}
			}
		}
		
		private void putPreferencesOfStaticProperties ( Preferences prefs) {
			for ( DOTProperty property : _isStaticMap.keySet()) {
				if ( _isStaticMap.get( property)) {
					// Eine Statische Property wird als dynamische ohne Anmeldungsdaten weggeschrieben. 
					Preferences propertyPrefs = getPropertyPreferences(prefs, property);
					final DynamicDOTItem dynamicDOTItem = new DynamicDOTItem("", "", "", "",
							_staticPropertyValues.get( property));
					dynamicDOTItem.putPreferences(propertyPrefs);
				}
			}
		}
		
		private void putPreferencesOfDynamicProperties ( Preferences prefs) {
			for ( DOTProperty property : _isStaticMap.keySet()) {
				if ( !_isStaticMap.get( property)) {
					Preferences propertyPrefs = getPropertyPreferences(prefs, property);
					DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
					int i = 0;
					for ( TreeMap<Interval<Double>, DynamicDOTItem> treeMap : dynamicDOTItemManager.getTreeMaps()) {
						for ( Interval<Double> interval: treeMap.keySet()) {
							Preferences objectForItemPrefs = propertyPrefs.node( propertyPrefs.absolutePath() + "/interval" + i);
							objectForItemPrefs.putDouble(LOWER_BOUND, interval.getLowerBound());
							objectForItemPrefs.putDouble(UPPER_BOUND, interval.getUpperBound());
							DynamicDOTItem dynamicItem = (DynamicDOTItem)treeMap.get(interval);
							if ( (dynamicItem == null) && (treeMap.size() >= 1)) { 
								// weil es mit Double.NEGATIVE_INFINITY nicht geht, das get(); >= ist wichtig!
								dynamicItem = treeMap.values().toArray( new DynamicDOTItem[1])[0];
							}
							dynamicItem.putPreferences( objectForItemPrefs);
							i++;
						}
					}
				}
			}
		}
		
		private Preferences getPropertyPreferences( Preferences prefs, DOTProperty property) {
			Preferences propertyPrefs;
			if ( property == DOTProperty.FARBE) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/color");
			} else if ( property == DOTProperty.ABSTAND) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/distance");
			} else if ( property == DOTProperty.STRICHBREITE) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/strokewidth");
			} else if ( property == DOTProperty.DURCHMESSER) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/diameter");
			} else if ( property == DOTProperty.FUELLUNG) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/fill");
			} else if ( property == DOTProperty.GROESSE) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/size");
			} else if ( property == DOTProperty.TRANSPARENZ) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/transparency");
			} else if ( property == DOTProperty.TEXT) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/text");
			} else if ( property == DOTProperty.TEXTSTIL) {
				propertyPrefs = prefs.node( prefs.absolutePath() + "/textstyle");
			} else {
				throw new IllegalArgumentException();
			}
			return propertyPrefs;
		}
		
		/**
		 * Gibt eine Liste mit allen Attributnamen zurück, die für die Eigenschaft und die
		 * durch DOTSubscriptionData gekapselte Attributgruppe und den Aspekt für diese
		 * Grundfigur relevant sind.
		 * 
		 * @param property die Eigenschaft
		 * @param subscriptionData eine Anmeldung
		 * @return alle relevanten Attributnamen
		 */
		public List<String> getAttributeNames( DOTProperty property, 
				DOTSubscriptionData subscriptionData) {
			final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
			if ( dynamicDOTItemManager == null) {
				return new ArrayList<String>();
			}
			final List<String> list = dynamicDOTItemManager.getAttributeNames( subscriptionData);
			if ( list != null) {
				return list;
			}
			return new ArrayList<String>();
		}
		
		/**
		 * Gibt das Item zurück, das für die übergebenen Werte verwendet werden kann, oder <code>null</code>,
		 * wenn ein solches nicht existiert.
		 * 
		 * @param property die Eigenschaft
		 * @param subscriptionData eine Anmeldung
		 * @param attributeName ein Attributname
		 * @param value der Wert
		 */
		public DisplayObjectTypeItem isValueApplicable( 
				DOTProperty property, 
				DOTSubscriptionData subscriptionData, 
				String attributeName, double value) {
			if ( _isStaticMap.get( property)) {
				return null;
			}
			final DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
			if ( dynamicDOTItemManager == null) {
				return null;
			}
			final List<String> list = dynamicDOTItemManager.getAttributeNames( subscriptionData);
			if ( list == null) {
				return null;
			}
			if ( !list.contains( attributeName)) {
				return null;
			}
			final TreeMap<Interval<Double>, DynamicDOTItem> treeMap = 
				dynamicDOTItemManager.get( dynamicDOTItemManager.getKeyString( subscriptionData, attributeName));
			if ( treeMap == null) {
				return null;
			}
			Interval<Double> valInterval = new Interval<Double>( value, value);
			final Entry<Interval<Double>, DynamicDOTItem> floorEntry = treeMap.floorEntry(valInterval);
			if ( floorEntry != null) {
				final Interval<Double> floorKey = floorEntry.getKey();
				if ((floorKey.getLowerBound() <= value) && (value <= floorKey.getUpperBound())) {
					return floorEntry.getValue();
				}
			}
			final Entry<Interval<Double>, DynamicDOTItem> ceilingEntry = treeMap.ceilingEntry(valInterval);
			if ( ceilingEntry != null) {
				final Interval<Double> ceilingKey = ceilingEntry.getKey();
				if ((ceilingKey.getLowerBound() <= value) && (value <= ceilingKey.getUpperBound())) {
					return ceilingEntry.getValue();
				}
			}
			return null;
		}
		
		/**
		 * Erzeugt eine tiefe Kopie des Objekts.
		 * 
		 * @return die Kopie
		 */
		public PrimitiveForm getCopy() {
			Map<String,Object> specificInformation = new HashMap<String, Object>();
			for ( String s : _specificInformation.keySet()) {
				final Object object = _specificInformation.get( s);
				Object newObject = null;
				if ( object instanceof Integer) {
					newObject = new Integer( (Integer) object);
				} else if (object instanceof Double) {
					newObject = new Double ( (Double) object);
				} else if (object instanceof String) {
					newObject = new String( (String) object);
				}
				specificInformation.put( s, newObject);
			}
			PrimitiveForm copy = new PrimitiveForm ( _nameOfPrimitiveForm, 
					_typeOfPrimitiveForm, _infoOfPrimitiveForm, 
					new Point2D.Double( _translation.getX(), _translation.getY()), 
					specificInformation);
			for ( DOTProperty property : _isStaticMap.keySet()) {
				Boolean newBoolean = _isStaticMap.get( property);
				copy._isStaticMap.put( property, newBoolean);
			}
			for ( DOTProperty property : _staticPropertyValues.keySet()) {
				Object object = _staticPropertyValues.get( property);
				Object newObject = null;
				if ( object instanceof Integer) {
					newObject = new Integer( (Integer) object);
				} else if (object instanceof Double) {
					newObject = new Double ( (Double) object);
				} else if (object instanceof String) {
					newObject = new String( (String) object);
				} else if (object instanceof Color) {
					final Color color = (Color) object;
					newObject = new Color (color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
				}
				copy._staticPropertyValues.put( property, newObject);
			}
			for ( DOTProperty property : _dynamicDOTItemManagers.keySet()) {
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
				DynamicDOTItemManager newDynamicDOTItemManager = dynamicDOTItemManager.getCopy();
				copy._dynamicDOTItemManagers.put( property, newDynamicDOTItemManager);
			}
			return copy;
		}
		
		/**
		 * Gibt Default-Werte für die spezifisch definiernden Informationen zurück.
		 * 
		 * @param primitiveFormName der Name einer Grundfigur
		 * @return die Default-Werte
		 */
		public static Map<String,Object> getDefaultSpecificInformation( String primitiveFormName) {
			Map<String, Object> map = new HashMap<String, Object>();
			if ( primitiveFormName.equals( PrimitiveFormType.RECHTECK.getName())) {
				map.put( HEIGHT, new Double(0.));
				map.put( WIDTH, new Double(0.));
			} else if ( primitiveFormName.equals( PrimitiveFormType.KREIS.getName())) {
				map.put( RADIUS, new Double(0.));
			} else if ( primitiveFormName.equals( PrimitiveFormType.HALBKREIS.getName())) {
				map.put( RADIUS, new Double(0.));
				map.put( ORIENTATION, DOTPointPainter.OBERER_HALBKREIS);
			}
			return map;
		}
		
		private void checkSpecificInformation() {
			if ( _typeOfPrimitiveForm == PrimitiveFormType.RECHTECK) {
				if ( _specificInformation.size() != 2) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein ungültiges Rechteck.");
				}
				if ( !_specificInformation.containsKey( HEIGHT)) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein Rechteck ohne Höhe.");
				}
				if ( !_specificInformation.containsKey( WIDTH)) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein Rechteck ohne Breite.");
				}
			} else if ( _typeOfPrimitiveForm == PrimitiveFormType.KREIS) {
				if ( _specificInformation.size() != 1) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein ungültiger Kreis.");
				}
				if ( !_specificInformation.containsKey( RADIUS)) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein Kreis ohne Radius.");
				}
			} else if ( _typeOfPrimitiveForm == PrimitiveFormType.HALBKREIS) {
				if ( _specificInformation.size() != 2) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein ungültiger Halbkreis.");
				}
				if ( !_specificInformation.containsKey( RADIUS)) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein Halbkreis ohne Radius.");
				}
				if ( !_specificInformation.containsKey( ORIENTATION)) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein Halbkreis ohne Orientation.");
				}
			} else if ( _typeOfPrimitiveForm == PrimitiveFormType.TEXTDARSTELLUNG) {
				if ( _specificInformation.size() != 0) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": eine ungültige Textdarstellung.");
				}
			} else if ( _typeOfPrimitiveForm == PrimitiveFormType.PUNKT) {
				if ( _specificInformation.size() != 0) {
					_debug.warning("PrimitiveForm.checkSpecificInformation() für " + getName() + ": ein ungültiger Punkt.");
				}
			}
		}
		
		private void initCollections() {
			final DisplayObjectTypePlugin displayObjectTypePlugin = new DOTPointPlugin();
			final DOTProperty[] properties = displayObjectTypePlugin.getProperties( _typeOfPrimitiveForm);
			for(DOTProperty dotProperty : properties) {
				_isStaticMap.put( dotProperty, true);
				_staticPropertyValues.put(dotProperty, DOTPoint.getDefaultValue( dotProperty));
				_dynamicDOTItemManagers.put( dotProperty, new DynamicDOTItemManager());
			}
		}
		
		/**
		 * Gibt den Item-Manager der Eigenschaft zurück.
		 * 
		 * @param property die Eigenschaft
		 * @return den ItemManager
		 */
		public DynamicDOTItemManager getDynamicDOTItemManager( final DOTProperty property) {
			return _dynamicDOTItemManagers.get( property);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public String toString() {
			String s = "[PrimitiveForm:[Name:" + _nameOfPrimitiveForm + "][Type:" + _typeOfPrimitiveForm +
			"][Info:" + _infoOfPrimitiveForm + "][Verschiebung:(" + _translation.getX() + "," +
			_translation.getY() + ")]";
			s += "[Definition:";
			for ( String key : _specificInformation.keySet()) {
				s += "[" + key + ":" + _specificInformation.get(key) + "]";
			}
			s += "]";
			s += "[Statische Eigenschaften:";
			for ( DOTProperty property : _isStaticMap.keySet()) {
				if ( _isStaticMap.get( property)) {
					s += "[" + property.toString() + ":" + _staticPropertyValues.get( property) + "]";
				}
			}
			s += "]";
			s += "[Dynamische Eigenschaften:";
			for ( DOTProperty property : _isStaticMap.keySet()) {
				if ( !_isStaticMap.get( property)) {
					s += "[" + property.toString() + ":";
					DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
					final int size = dynamicDOTItemManager.size();
					for ( int i = 0; i < size; i++) {
						final DisplayObjectTypeItemWithInterval displayObjectTypeItemWithInterval = dynamicDOTItemManager.get(i);
						s += displayObjectTypeItemWithInterval.toString();
					}
					s += "]";
				}
			}
			s += "]]";
			return s;
		}
		
		/**
		 * Gibt eine Menge mit allen von dieser Grundfigur für die übergebene Eigenschaft benutzten Farben zurück.
		 * 
		 * @param property die Eigenschaft
		 * @return die Menge der benutzten Farben
		 */
		@SuppressWarnings("unchecked")
		public Set<String> getUsedColors( final DOTProperty property) {
			Set<String> usedColors = new HashSet<String>();
			if ( isPropertyStatic( property)) {
				final String colorName = (String) getValueOfStaticProperty( property);
				usedColors.add( colorName.toLowerCase());
			} else {
				DynamicDOTItemManager dynamicDOTItemManager = _dynamicDOTItemManagers.get( property);
				final int size = dynamicDOTItemManager.size();
				for ( int index = 0; index < size; index++) {
					final DisplayObjectTypeItemWithInterval dotItemWithInterval = dynamicDOTItemManager.get(index);
					final String colorName = (String) dotItemWithInterval.getItem().getPropertyValue();
					usedColors.add( colorName.toLowerCase());
				}
			}
			return usedColors;
		}
		
		/**
		 * Gibt eine Menge mit allen von dieser Grundfigur benutzten Farben zurück.
		 * 
		 * @return die Menge der benutzten Farben
		 */
		public Set<String> getUsedColors() {
			Set<String> usedColors = new HashSet<String>();
			if ( _isStaticMap.containsKey( DOTProperty.FARBE)) {
				usedColors.addAll( getUsedColors( DOTProperty.FARBE));
			}
			if ( _isStaticMap.containsKey( DOTProperty.FUELLUNG)) {
				usedColors.addAll( getUsedColors( DOTProperty.FUELLUNG));
			}
			return usedColors;
		}
		
		// Members von PrimitiveForm:
		private String _nameOfPrimitiveForm;
		private PrimitiveFormType _typeOfPrimitiveForm;
		private String _infoOfPrimitiveForm;
		private Point2D.Double _translation;
		private Map<String,Object> _specificInformation = new HashMap<String, Object>();
		protected Map<DOTProperty, Boolean> _isStaticMap = new HashMap<DOTProperty, Boolean>();
		protected Map<DOTProperty, Object> _staticPropertyValues = new HashMap<DOTProperty, Object>();
		protected Map<DOTProperty, DynamicDOTItemManager> _dynamicDOTItemManagers = 
			new HashMap<DOTProperty, DynamicDOTItemManager>();
		
		public static final String HEIGHT = "height";
		public static final String WIDTH = "width";
		public static final String RADIUS = "radius";
		public static final String ORIENTATION = "orientation";
	}
	
	/**
	 * Eine Enumeration aller Grundfigurtypen. Jeder Grundfigurtyp hat nur einen eindeutigen Namen.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8080 $
	 *
	 */
	static public class PrimitiveFormType {
		private final String _name;
		
		private PrimitiveFormType(String name) {
			super();
			_name = name;
		}
		
		/**
		 * Gibt den Namen zurück.
		 * 
		 * @return den Namen
		 */
		public String getName() {
			return _name;
		}
		
		@Override
		public String toString() {
			return _name;
		}
		
		@Override
		public final boolean equals( Object o) {
			return super.equals( o);
		}
		
		@Override
		public final int hashCode() {
			return super.hashCode();
		}
		
		/**
		 * Wandelt den String in ein PrimitiveFormType-Objekt.
		 * 
		 * @param name der Name des Types
		 * @return der Typ
		 */
		public static PrimitiveFormType getPrimitiveFormType( final String name) {
			if ( name.equals( "Punkt")) {
				return PUNKT;
			} else if ( name.equals( "Rechteck")) {
				return RECHTECK;
			} else if ( name.equals( "Kreis")) {
				return KREIS;
			} else if ( name.equals( "Halbkreis")) {
				return HALBKREIS;
			} else if ( name.equals( "Textdarstellung")) {
				return TEXTDARSTELLUNG;
			} 
			return null;
		}
		
		public static final PrimitiveFormType PUNKT = new PrimitiveFormType( "Punkt");
		public static final PrimitiveFormType RECHTECK = new PrimitiveFormType( "Rechteck");
		public static final PrimitiveFormType KREIS = new PrimitiveFormType( "Kreis");
		public static final PrimitiveFormType HALBKREIS = new PrimitiveFormType( "Halbkreis");
		public static final PrimitiveFormType TEXTDARSTELLUNG = new PrimitiveFormType( "Textdarstellung");
	}
	
	@Override
	public final boolean equals( Object o) {	// Nicht ändern!
		return super.equals(o);
	}
	
	@Override
	public String toString() {
		String s = "[DOTPoint: [Name:" + _name + "][Info:" + _info + "][Verschiebungsfaktor:" + _translationFactor +
		"][Verbindungslinie:" + _joinByLine + "]";
		final Set<String> keySet = _primitiveForms.keySet();
		// Wir müssen die Reihenfolge normieren, damit auch der String-Vergleich geht!
		final SortedSet<String> sortedKeySet = new TreeSet<String>();
		sortedKeySet.addAll( keySet);
		for ( String key : sortedKeySet) {
			s += _primitiveForms.get( key).toString();
		}
		s += "]";
		return s;
	}
	
	public Set<String> getUsedColors() {
		Set<String> usedColors = new HashSet<String>();
		for ( PrimitiveForm primitiveForm : _primitiveForms.values()) {
			usedColors.addAll( primitiveForm.getUsedColors());
		}
		return usedColors;
	}
	
	/*
	 * Implementiert diese Methode von DOTManager.DOTChangeListener leer.
	 */
	public void displayObjectTypeAdded(DisplayObjectType displayObjectType) {
	}
	
	/* 
	 * Implementiert diese Methode von DOTManager.DOTChangeListener.
	 */
	public void displayObjectTypeChanged(DisplayObjectType displayObjectType) {
		if ( displayObjectType.equals( this)) {
			return;
		}
		if ( displayObjectType.getName().equals( _name)) {
			DOTPoint dotPoint = (DOTPoint) displayObjectType;
			_info = dotPoint.getInfo();
			_translationFactor = dotPoint.getTranslationFactor();
			_joinByLine = dotPoint.isJoinByLine();
			_primitiveForms.clear();
			for ( String s : dotPoint._primitiveForms.keySet()) {
				_primitiveForms.put( s, dotPoint._primitiveForms.get( s));
			}
		}
	}
	
	/*
	 * Implementiert diese Methode von DOTManager.DOTChangeListener leer.
	 */
	public void displayObjectTypeRemoved(String displayObjectTypeName) {
	}
	
	// Members von DOTPoint
	private String _name;
	private String _info;
	private Double _translationFactor;
	private Boolean _joinByLine;
	private Map<String,PrimitiveForm> _primitiveForms = new HashMap<String,PrimitiveForm>();
	
	final static Debug _debug = Debug.getLogger();
	
	private static final String LOWER_BOUND = "LOWER_BOUND";
	private static final String UPPER_BOUND = "UPPER_BOUND";
	private static final String TYPE = "TYPE";
	private static final String VALUE = "VALUE";
	private static final String INFO = "INFO";
	private static final String TRANSLATION_X = "TRANSLATION_X";
	private static final String TRANSLATION_Y = "TRANSLATION_Y";
	private static final String JOIN_BY_LINE = "JOIN_BY_LINE";
	private static final String TRANSLATION_FACTOR = "TRANSLATION_FACTOR";
	
}
