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

import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.viewManagement.View;
import de.kappich.pat.gnd.viewManagement.ViewEntry;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.HashMap;

/**
 * Die Legende der Kartendarstellung.
 * <p>
 * Das LegendPane steht für den Legendenbaum in der Kartendarstellung der GND. Es ist als
 * JTree realisiert und besitzt die von DefaultTreeModel abgeleitete Klasse LegendTreeModel,
 * die aus den LegendTreeNodes der Layer bei jeder Änderungkomplett neu zusammengebaut werden.
 * Dies geschieht im Konstruktor von LegendTreeModel. Ein Update wird durch eine Änderung
 * einer Ansicht oder des Anzeigemaßstabs augelöst.
 * <p>
 * Beim Neuaufbau des Legendenbaums bleiben alle nicht-expandierten Knoten in diesem Zustand,
 * während alle anderen, also insbesondere neu hinzugefügte Knoten, expandiert werden. 
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 9145 $
 *
 */
@SuppressWarnings("serial")
public class LegendPane extends JTree implements View.ViewChangeListener, MapPane.MapScaleListener {
	
	private View _view;
	
	private Double _mapScale = 0.;
	
	private TreeModel _treeModel;
	
	/**
	 * Konstruiert ein Objekt aus der übergebenen Ansicht, wobei allerdings die Initialisierung
	 * noch ausbleibt (s. {@link #init}).
	 * 
	 * @param view eine Ansicht
	 */
	public LegendPane( final View view) {
		super( new DefaultTreeModel( new DefaultMutableTreeNode()));
		setRootVisible( false);
		_view = view;
	}
	
	/**
	 * Initialisiert das Objekt.
	 * 
	 * @param mapScale der Maßstab für die Kartendarstellung
	 */
	public void init( final Double mapScale) {
		_mapScale = mapScale;
		
		setRootVisible( false);
		setPreferredSize( new Dimension( 145, 100));
		setEditable(false);
		_view.addChangeListener(this);
		_treeModel = new LegendTreeModel();
		setModel(_treeModel);
		expandAll();
		setCellRenderer(new LegendCellRenderer());
		ToolTipManager.sharedInstance().registerComponent(this);
		
		TreeModelListener modelListener = new TreeModelListener() {
			
			public void treeNodesChanged(TreeModelEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());
				try {
					int index = e.getChildIndices()[0];
					node = (DefaultMutableTreeNode) (node.getChildAt(index));
				} catch (NullPointerException exc) {}
			}
			
			public void treeNodesInserted(TreeModelEvent e) {
			}
			
			public void treeNodesRemoved(TreeModelEvent e) {
			}
			
			public void treeStructureChanged(TreeModelEvent e) {
			}
			
		};
		_treeModel.addTreeModelListener( modelListener);
	}
	
	/**
	 * Diese Klasse unterscheidet sich von einem DefaultTreeModel allein durch den Konstruktor,
	 * in dem die Daten aus den Membern von LegendPane ermittelt werden.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 9145 $
	 */
	public class LegendTreeModel extends DefaultTreeModel {
		/**
		 * Konstruiert ein TreeModel aus den Daten von LegendPane.
		 */
		public LegendTreeModel() {
			super(new DefaultMutableTreeNode());
			DefaultMutableTreeNode ourRoot = (DefaultMutableTreeNode) getRoot();
			for (ViewEntry viewEntry : _view.getViewEntries(false)) {
				if (viewEntry.isVisible(_mapScale.intValue())) {
					final Layer layer = viewEntry.getLayer();
					DefaultMutableTreeNode currentNode = 
						new LegendTreeNodes.LegendTreeNode( layer.getName(),layer.getInfo());
					ourRoot.add( currentNode);
					final DisplayObjectType displayObjectType = layer.getDisplayObjectType(_mapScale.intValue());
					if ( displayObjectType == null) {
						continue;
					}
					LegendTreeNodes nodes = displayObjectType.getLegendTreeNodes();
					for ( LegendTreeNodes.LegendTreeNode newNode : nodes.getOrderedNodes()) {
						currentNode.add( newNode);
						Integer levelChange = nodes.getLevelChange( newNode);
						if ( levelChange < 0) {
							currentNode = newNode;
						}
						while ( levelChange > 0) {
							currentNode = (DefaultMutableTreeNode) currentNode.getParent();
							levelChange--;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Der LegendCellRenderer legt fest wie die Teile der Legende angezeigt werden.
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 9145 $
	 */
	public class LegendCellRenderer extends DefaultTreeCellRenderer {
		public LegendCellRenderer(){
			// Versichern, dass der Hintergrund gezeichnet wird
			setOpaque( true );
		}
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,  boolean sel,
				boolean expanded, boolean leaf, int row, boolean hasFocus1) {
			
			// Die Originalmethode die Standardeinstellungen machen lassen
			super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus1 );
			
			// Tooltipp setzen
			if ( value instanceof LegendTreeNodes.LegendTreeNode ) {
				LegendTreeNodes.LegendTreeNode actual = (LegendTreeNodes.LegendTreeNode) value;
				setToolTipText( actual.getInfo());
			}
			
			// Den Wert des Knotens abfragen
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object inside = node.getUserObject();
			
			// Wenn der Knoten eine Farbe ist: den Hintergrund entsprechend setzen
			// Andernfalls soll der Hintergrund zum Baum passen.
			if( inside instanceof Color ){
				setBackground( (Color)inside );
				setForeground( Color.BLACK );
			}
			else{
				setBackground( tree.getBackground() );
				setForeground( tree.getForeground() );
			}
			return this;
		}
	}
	
	public void viewEntriesSwitched(View view, int i, int j) {
		recreateLegendWithRespectToExpansion();
	}
	
	public void viewEntryChanged(View view, int i) {
		recreateLegendWithRespectToExpansion();
	}
	
	public void viewEntryRemoved(View view, int i) {
		recreateLegendWithRespectToExpansion();
	}
	
	public void viewEntryInserted(View view, final int newIndex) {
		recreateLegendWithRespectToExpansion();
	}
	
	
	private void recreateLegendWithRespectToExpansion() {
		HashMap<String, Boolean> expansionMap = new HashMap<String, Boolean>(getRowCount());
		for (int i = 0; i < getRowCount(); i++) {
			TreePath path = getPathForRow(i);
			final Object lastPathComponent = path.getLastPathComponent();
			if (lastPathComponent instanceof LegendTreeNodes.LegendTreeNode) {
				LegendTreeNodes.LegendTreeNode node = (LegendTreeNodes.LegendTreeNode)lastPathComponent;
				expansionMap.put(node.getNameOrText(), isExpanded(path) || node.isLeaf());
			}
		}
		LegendTreeModel newTreeModel = new LegendTreeModel();
		setModel(newTreeModel);
		for (int i = 0; i < getRowCount(); i++) {
			TreePath path = getPathForRow(i);
			final Object lastPathComponent = path.getLastPathComponent();
			if (lastPathComponent instanceof LegendTreeNodes.LegendTreeNode) {
				LegendTreeNodes.LegendTreeNode node = (LegendTreeNodes.LegendTreeNode)lastPathComponent;
				final String string = node.getNameOrText();
				if (expansionMap.containsKey(string)) {
					setExpandedState(path, expansionMap.get(string));
				} else {	// neue Layer expandiert
					setExpandedState(path, true);
				}
			}
		}
	}
	
	private void expandAll() {
		for ( int row = 0; row < getRowCount(); row++) {
			expandRow(row);
		}
	}
	
	/**
	 * Setzt den Maßstabsfaktor.
	 * 
	 * @param mapScale den neue Maßstabsfaktor
	 */
	public void setMapScale(double mapScale) {
		_mapScale = mapScale;
	}
	
	/*
	 * Die Implementation von MapPane.MapScaleListener
	 */
	public void mapScaleChanged(double scale) {
		_mapScale = scale;
		recreateLegendWithRespectToExpansion();
	}
}
