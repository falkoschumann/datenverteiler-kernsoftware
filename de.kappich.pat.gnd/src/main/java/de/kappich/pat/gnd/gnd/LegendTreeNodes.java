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
package de.kappich.pat.gnd.gnd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Eine Klasse für eine Liste von Objekten in der Legende der Kartendarstellung. Die einzelnen
 * Knoten in dieser Liste haben Informationen über die Tiefe innerhalb der Legendenbaumes, die
 * jeweils relativ zum Vorgänger berechnet ist. Der allgemeineren Verständnis: jeder Darstellungstyp
 * muss die Methode getLegendTreeNodes implementieren, die ein Objekt von LegendTreeNodes
 * zurückliefert, das gerade für den Teilbaum der Legende, den dieser Darstellungstyp festlegt, steht.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public class LegendTreeNodes {
	
	/**
	 * Ein LegendTreeNodes-Objekt verwaltet die Knoten des Legendenbaums als Liste. Jeder solche
	 * Knoten ist ein LegendTreeNode-Objekt mit der Information, in welchem Umfang sich der
	 * Level des aktuellen Knoten von dem seines Vorgängers unterscheidet. Dabei bedeutet eine
	 * 0, dass sich der Knoten auf demselben Level wie sein Vorgänger befindet, während -1
	 * bedeutet, dass er einen Level tiefer angeordnet wird, und eine Positive Zahl i bedeutet,
	 * dass der Knoten i Level höher anzuordnen ist. 
	 */
	public LegendTreeNodes () {}
	
	/**
	 * Fügt einen neuen Knoten mit dem übergebenen Level-Änderung relativ zum Vorgänger hinzu.
	 * 
	 * @param node der neue Knoten
	 * @param levelChange die Level-Änderung
	 */
	public void add( LegendTreeNode node, Integer levelChange) {
		_legendTreeNodes.add( node);
		_levelChanges.put( node, levelChange);
	}
	
	/**
	 * Gibt die Liste der Knoten zurück.
	 * 
	 * @return die Liste aller Knoten
	 */
	public List<LegendTreeNode> getOrderedNodes() {
		return _legendTreeNodes;
	}
	
	/**
	 * Gibt für übergebenen Knoten die Leveländerung relativ zu seinem Vorgänger zurück.
	 * 
	 * @return die Leveländerung relativ zu seinem Vorgänger
	 */
	public Integer getLevelChange( LegendTreeNode node) {
		return _levelChanges.get( node);
	}
	
	/**
	 * Eine Klasse für einzelne Objekte in der Legende der Kartendarstellung.
	 * <p>
	 * Ein LegendTreeNode verkörpert einen Knoten im Legendenbaum.
	 * Jeder Knoten hat einen Namen, d.i. der Text, der in der Legende angezeigt wird,
	 * und einen Infotext, der als Tooltipp verwendet wird.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8080 $
	 *
	 */
	@SuppressWarnings("serial")
	public static class LegendTreeNode extends DefaultMutableTreeNode {
		
		private final String nameOrText;
		private final String _info;
		
		/**
		 * Konstruiert ein Objekt aus den gegebenen Informationen.
		 */
		public LegendTreeNode( String name, String info) {
			super(name);
			nameOrText = name;
			_info = info;
		}
		
		/**
		 * Gibt den Namen oder Text des Knoten zurück.
		 * 
		 * @return den Namen des Knoten
		 */
		public String getNameOrText() {
			return nameOrText;
		}
		
		/**
		 * Gibt den Infotext des Knotens zurück.
		 * 
		 * @return den Infotext
		 */
		public String getInfo() {
			return _info;
		}
	}
	
	final private List<LegendTreeNode> _legendTreeNodes = new ArrayList<LegendTreeNode>();
	final private Map<LegendTreeNode, Integer> _levelChanges = new HashMap<LegendTreeNode, Integer>();
	// Die Logik ist hier wie folgt zu verstehen:
	// Im der Liste werden die Nodes gemäß ihrer Reihenfolge aufgelistet. In der Map wird die Änderung
	// des Levls vermerkt: eine -1 oder jede andere negative Zahl bedeutet, dass nach diesem Node
	// ein neuer Children-Level unterhalb des jetzigen Nodes beginnt. Eine 0 bedeutet, dass sich der 
	// Level und damit auch der Parent-Node für den nächsten Node nicht verändert.
	// Eine positive Zahl n bedeutet, dass der Parent-Node des nächsten Nodes der n-te Parent des
	// aktuellen Nodes ist.
	// Beispiel:
	// Root-Node: (existiert)
	//		Layer1: 		 		-1
	//			PrimitiveForm1:		-1
	//				Property1:		0
	//				Property2:		0
	//				Property3:		+1
	//			PrimitiveForm2:		-1
	//				Property4:		0
	//				Property5:		+2
	//		Layer2:					-1
	//			PrimitiveForm6:		+1 (zurück zu Root!)
}
