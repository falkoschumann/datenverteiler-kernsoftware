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
package de.kappich.pat.gnd.pluginInterfaces;

import de.kappich.pat.gnd.displayObjectToolkit.DOTProperty;
import de.kappich.pat.gnd.displayObjectToolkit.DOTSubscriptionData;
import de.kappich.pat.gnd.gnd.LegendTreeNodes;

import de.bsvrz.dav.daf.main.DataState;

import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Das Interface für die Darstellungstypen der Plugins.
 * <p>
 * Der Name eines Darstellungstypen wird bei der Definition vom Benutzer festgelegt. Er wird als 
 * eindeutiges Erkennungszeichen in den Verwaltungsdialogen verwendet, und auch bei der Anzeige in der Legende.
 * Außerdem hat ein Darstellungstyp eine 'Info', die als Tooltipp etwa in der Legende benutzt wird.
 * <p>
 * Ein Darstellungstyp legt fest wie die {@link DisplayObject DisplayObjects} eines Layers dargestellt
 * werden. Darstellungstypen können {@link PrimitiveForm Grundfiguren} besitzen, müssen es aber nicht.
 * Beispiele für Darstellungstypen ohne Grundfiguren sind {@link DOTArea}, {@link DOTComplex} und 
 * {@link DOTLine}. Bei diesen Klassen hängen alle {@link DOTProperty Eigenschaften} der Visualisierung 
 * (z.B. die Farbe) direkt am Darstellungstypen. Anders verhält es sich bei der Klasse {@link DOTPoint}: 
 * dieser Darstellungstyp hat selbst keine Eigenschaften, sondern ausschließlich benutzerdefinierte 
 * Grundfiguren (z.B. ein Rechteck festgelegter Größe) und nur an diesen hängen die Eigenschaften.
 * Bei der Implementation sollte der Zugriff auf Eigenschaften, die direkt am Darstellungstypen hängen,
 * durch <code>null</code> als Wert für die Grundfigur geschehen.
 * <p>
 * Jede Grundfigur hat einen Typ, der einerseits definiernde Größen (z.B. den Radius bei dem Typ Kreis), 
 * aber auch die möglichen Visualisierungs-Eigenschaften festlegt (z.B. die Füllfarbe).
 * <p>
 * Eine Visualisierungs-Eigenschaft ist entweder statisch, d.h. unveränderbar, oder dynamisch, d.h.
 * sie verändert sich in Abhängigkeit von Online-Daten.
 *  
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public interface DisplayObjectType {
	
	
	/**
	 * Getter für den Namen.
	 * 
	 * @return der Name
	 */
	public String getName();
	
	/**
	 * Setter für den Namen.
	 * 
	 * @param der Name
	 */
	public void setName( String name);
	
	/**
	 * Getter für die Info.
	 * 
	 * @return die Kurzinfo
	 */
	public String getInfo();
	
	/**
	 *  Setter für die Info.
	 *  
	 * @param info die Kurzinfo
	 */
	public void setInfo( String info);
	
	/**
	 * Zugriff auf alle auftretenden Grundfigurnamen.
	 * 
	 * @return die Menge aller Grundfigurnamen
	 */
	public Set<String> getPrimitiveFormNames();
	
	/**
	 * Gibt den Grundfigurtyp der Grundfigur zurück.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur
	 * @return der Typ der Grundfigur 
	 */
	public String getPrimitiveFormType( String primitiveFormName);
	
	/**
	 * Gibt die Kurzinfo zu der Grundfigur zurück.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur
	 * @return die Kurzinfo zu der Grundfigur
	 */
	public String getPrimitiveFormInfo( String primitiveFormName);
	
	/**
	 * Löscht die entsprechende Grundfigur.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur
	 */
	public void removePrimitiveForm ( String primitiveFormName);
	
	/**
	 * Zugriff auf alle dynamischen Properties der Grundfigur.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur
	 * @return die Liste aller dynamischen Eigenschaften der Grundfigur
	 */
	public List<DOTProperty> getDynamicProperties( String primitiveFormName);
	
	/**
	 * Ist die DOTProperty zu der als Object übergebenen Grundfigur statisch, so erhält man
	 * <code>true</code> zurück; andernfalls ist die Eigenschaft dynamisch und man erhält
	 * <code>false</code>.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 * @param property eine Eigenschaft
	 * @return ist die Eigenschaft statisch?
	 */
	public Boolean isPropertyStatic( String primitiveFormName, DOTProperty property);
	
	/**
	 * Setzt die Eigenschaft statisch bzw dynamisch zu sein der übergebenen Eigenschaft, die gegebenenfalls 
	 * zu der genannten Grundfigur gehört. Diese Methode sollte so implementiert werden, dass sie beim Ändern 
	 * die nicht mehr gültigen Werte der Eigenschaft nicht löscht (dadurch wird es möglich, dass der Benutzer 
	 * diese zwischen statisch und dynamisch hin- und herschaltet ohne seine vorherigen Einstellungen zu
	 * verlieren).
	 * 
	 *  @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 *  @param property eine Eigenschaft
	 *  @param b der neue Wert
	 */
	public void setPropertyStatic( String primitiveFormName, DOTProperty property, boolean b);
	
	/**
	 * Gibt den Wert der übergebenen DOTProperty zurück, die gegebenenfalls zu der genannten Grundfigur gehört.
	 *  
	 *  @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 *  @param property eine Eigenschaft
	 *  @return der Wert der Eigenschaft
	 */
	public Object getValueOfStaticProperty( String primitiveFormName, DOTProperty property);
	
	/**
	 * Setzt den Wert der übergebenen DOTProperty, die gegebenenfalls zu der genannten Grundfigur gehört.
	 * Diese Methode sollte so implementiert werden, dass sie auch auch dann den übergebenen Wert behält, 
	 * wenn die DOTProperty aktuell nicht statisch ist (dadurch wird es möglich, dass der Benutzer 
	 * diese zwischen statisch und dynamisch hin- und herschaltet ohne seine vorherigen Einstellungen zu
	 * verlieren). 
	 * 
	 *  @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 *  @param property eine Eigenschaft
	 *  @param value der neue Wert
	 */
	public void setValueOfStaticProperty( String primitiveFormName, DOTProperty property, Object value);
	
	/**
	 * Setzt den Wert der übergebenen DOTProperty, die gegebenenfalls zu der genannten Grundfigur gehört, für 
	 * das übergebene Intervall auf das übergebene DisplayObjectTypeItem. Diese Methode sollte so implementiert 
	 * werden, dass sie auch auch dann den übergebenen Wert behält, wenn die DOTProperty aktuell nicht dynamisch ist
	 * (dadurch wird es möglich, dass der Benutzer diese zwischen statisch und dynamisch hin- und herschaltet 
	 * ohne seine vorherigen Einstellungen zu verlieren). 
	 * 
	 *  @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 *  @param property eine Eigenschaft
	 *  @param dItem ein Item
	 *  @param lowerBound die untere Schranke
	 *  @param upperBound die obere Schranke
	 */
	public void setValueOfDynamicProperty( String primitiveFormName, DOTProperty property, 
			DisplayObjectTypeItem dItem, Double lowerBound, Double upperBound);
	
	/**
	 * Macht eine tiefe Kopie des DisplayObjectTypes und setzt den Namen um, falls der übergebene String nicht
	 * <code>null</code> ist. Diese Methode wird beim Erstellen und Bearbeiten von Darstellungstypen verwendet: 
	 * dem Bearbeitungs-Dialog wird eine tiefe Kopie übergeben und alle Änderungen werden an diesem 
	 * Objekt durchgeführt.
	 * 
	 * @param name der neue Name oder <code>null</code>
	 * @return eine Kopie
	 */
	public DisplayObjectType getCopy( String name);
	
	/**
	 * Speichert die Informationen des DisplayObjectTypes unter dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Speicherung durchgeführt werden soll
	 */
	public void putPreferences(Preferences prefs);
	
	/**
	 * Initialisiert den DisplayObjectType aus dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Initialisierung durchgeführt werden soll
	 */
	public void initializeFromPreferences(Preferences prefs);
	
	/**
	 * Löscht den DisplayObjectType unter dem übergebenen Knoten.
	 * 
	 * @param prefs der Knoten, unter dem die Löschung durchgeführt werden soll
	 */
	public void deletePreferences(Preferences prefs);
	
	/**
	 * Gibt die Selebstbeschreibung des Plugins, zu dem dieser DisplayObjectType gehört, zurück.
	 * 
	 * @return die Selbstbeschreibung
	 */
	public DisplayObjectTypePlugin getDisplayObjectTypePlugin();
	
	/**
	 * Erzeugt den Teilbaum der Legende, für diesen Darstellungstyp.
	 * 
	 * @return der Teilbaum der Legende
	 */
	public LegendTreeNodes getLegendTreeNodes();
	
	/**
	 * Gibt alle Anmeldungen, die dieser DisplayObjectTyp benötigt, zurück.
	 * 
	 * @return alle Anmeldungen 
	 */
	public Set<DOTSubscriptionData> getSubscriptionData();
	
	/**
	 * Gibt die Attributnamen, für die Werte benötigt werden, zu der übergebenen Eigenschaft
	 * und der übergebenen Anmeldung zurück.
	 * 
	 *  @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 *  @param property eine Eigenschaft
	 *  @param subscriptionData eine Anmeldung
	 *  @return alle Attributname
	 */
	public List<String> getAttributeNames( String primitiveFormName, DOTProperty property, 
			DOTSubscriptionData subscriptionData);
	
	/**
	 * Gibt die Namen aller von diesem DisplayObject verwendeten Farben zurück.
	 * 
	 * @return Die Menge aller Namen aller benutzten Farben
	 */
	public Set<String> getUsedColors();
	
	/**
	 * Ein Interface für die kleinste Einheit beim Zuordnen von Anmeldedaten (Attributgruppe, Aspekt,
	 * Attribut) zu Eigenschaftswerten und deren Beschreibung.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8080 $
	 *
	 */
	public interface DisplayObjectTypeItem {
		/**
		 * Getter der Attributgruppe.
		 * 
		 * @return die Attributgruppe
		 */
		public String getAttributeGroup();
		/**
		 * Getter des Aspekts.
		 * 
		 * @return der Aspekt
		 */
		public String getAspect();
		/**
		 * Getter für den Attributnamen.
		 * 
		 * @return der Attributname
		 */
		public String getAttributeName();
		/**
		 * Getter für die Beschreibung.
		 * 
		 * @return die Beschreibung
		 */
		public String getDescription();
		/**
		 * Getter für den aktuellen Wert der Eigenschaft, für die dieser DisplayObjectTypeItem Daten verwaltet.
		 * 
		 * @return der Wert der Eigenscahft
		 */
		public Object getPropertyValue();
		/**
		 * Erstellt eine tiefe Kopie des DisplayObjectTypeItems.
		 * 
		 * @return die Kopie
		 */
		public DisplayObjectTypeItem getCopy();
	}
	
	/**
	 * Ist der Rückgabewert nicht null, so ist dieser DisplayObjectTypeItem für die übergebenen Daten anwendbar.
	 * Diese Methode wird von einem {@link DisplayObject} aufgerufen, wenn neue Online-Daten vorliegen, die
	 * eine Änderung der Visualisierungs-Eigenschaft zur Folge haben könnte. Der im Rückgabewert enthaltene
	 * Wert (z.B. eine Farbe) wird dann vom {@link DisplayObjectTypePainter Painter} zur Visualisierung verwendet.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur oder <code>null</code> 
	 * @param subscriptionData Attributgruppe und Aspekt
	 * @param attributeName Attribut
	 * @param value Wert des Attributs
	 */
	public DisplayObjectTypeItem isValueApplicable( 
			String primitiveFormName, DOTProperty property, 
			DOTSubscriptionData subscriptionData, 
			String attributeName, double value);
	
	/**
	 * Ist der Rückgabewert nicht null, so ist dieser DisplayObjectTypeItem für die übergebenen Daten anwendbar.
	 * Diese Methode wird von einem {@link DisplayObject} aufgerufen, wenn zur gegebenen {@link DOTSubscriptionData
	 * Anmeldung} neue Daten geschickt wurden, die aber keine Werte für die Attribute enthalten, sondern Informationen
	 * über den {@link DataState Zustand}. Der im Rückgabewert enthaltene Wert (z.B. eine Farbe) wird dann vom 
	 * {@link DisplayObjectTypePainter Painter} zur Visualisierung verwendet.
	 * 
	 * @param primitiveFormName der Name einer Grundfigur oder <code>null</code>
	 * @param property die Eigenschaft
	 * @param subscriptionData Attributgruppe und Aspekt
	 * @param dataState Zustand des Datensatzes
	 */
	public DisplayObjectTypeItem getDisplayObjectTypeItemForState(
			final String primitiveFormName, final DOTProperty property,
			final DOTSubscriptionData subscriptionData, final DataState dataState);
}
