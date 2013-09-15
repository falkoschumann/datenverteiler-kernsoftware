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
package de.kappich.pat.gnd.layerManagement;

import de.kappich.pat.gnd.displayObjectToolkit.DOTCollection;
import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.utils.SpringUtilities;

import de.bsvrz.dav.daf.main.ClientDavInterface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * Eine Dialog, der alle Layer auflistet und über Schaltflächen Möglichkeiten bietet, Layer zu betrachten,
 * zu bearbeiten, zu löschen oder neu einzuführen. Der LayerManagerDialog zeigt die Inhalte der 
 * {@link LayerManager Layerverwaltung}. 

 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public class LayerManagerDialog {
	
	final private ClientDavInterface _connection;
	
	private JFrame _frame = null;
	
	private LayerDefinitionDialog _layerDefinitionDialog = null;
	
	/**
	 * Konstruiert einen neuen LayerManagerDialog. Das ClientDavInterface ist z.B. notwendig, um über das
	 * DataModel Informationen zu den Geo-Referenz-Objekten bekommen zu können.
	 * 
	 * @param die Datenverteiler-Verbindung
	 */
	public LayerManagerDialog( ClientDavInterface connection) {
		
		_connection = connection;
		
		_frame = new JFrame("GND: Layer-Verwaltung");
		
		_frame.setLayout(new BorderLayout());
		
		final JButton buttonNeu = new JButton("Neuer Layer");
		buttonNeu.setActionCommand("Neuer Layer");
		
		final JButton buttonBearbeiten = new JButton("Layer bearbeiten");
		buttonBearbeiten.setActionCommand("Bearbeiten");
		
		final JButton buttonCopy = new JButton("Layer kopieren");
		buttonCopy.setActionCommand("Kopieren");
		
		final JButton buttonLoeschen = new JButton("Layer löschen");
		buttonLoeschen.setActionCommand("Löschen");
		
		final JButton buttonHilfe = new JButton("Hilfe");
		buttonHilfe.setActionCommand("Hilfe");
		
		buttonBearbeiten.setEnabled( false);
		buttonCopy.setEnabled( false);
		buttonLoeschen.setEnabled( false);
		
		JPanel buttonsPanelEast = new JPanel();
		buttonsPanelEast.setLayout(new BoxLayout(buttonsPanelEast, BoxLayout.Y_AXIS));
		
		Dimension d = new Dimension(15, 15);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.add(buttonNeu);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.add(buttonBearbeiten);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.add(buttonCopy);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.add(buttonLoeschen);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.add(buttonHilfe);
		buttonsPanelEast.add(Box.createRigidArea(d));
		
		buttonsPanelEast.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		JPanel buttonsPanelSouth = new JPanel();
		buttonsPanelSouth.setLayout(new SpringLayout());
		buttonsPanelSouth.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		JButton schliessenButton = new JButton("Dialog schließen");
		buttonsPanelSouth.add( schliessenButton);
		SpringUtilities.makeCompactGrid(buttonsPanelSouth, 1, 20, 5);
		
		final JTable table = new JTable(LayerManager.getInstance());
		
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);
		table.setDefaultRenderer(Object.class, new ComboTableCellRendererLayer());
		// vordefinierte Darstellungstypen dürfen nicht bearbeitet oder gelöscht werden
		final ListSelectionModel selectionModel = table.getSelectionModel();
		
		final List<Layer> layerList = LayerManager.getInstance().getLayers();
		
		ListSelectionListener listSelctionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = table.getSelectedRow();
				
				if(selectedRow == -1) {
					buttonBearbeiten.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					buttonBearbeiten.setEnabled( false);
					buttonCopy.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					buttonCopy.setEnabled( false);
					buttonLoeschen.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					buttonLoeschen.setEnabled( false);
				}
				else {
					final boolean changeable = LayerManager.getInstance().isChangeable(layerList.get(selectedRow));
					if(!changeable) {
						buttonBearbeiten.setText("Layer betrachten");
						buttonBearbeiten.setToolTipText("Details des augewählten Layers betrachten");
						buttonBearbeiten.setEnabled( true);
						buttonCopy.setToolTipText("Kopie des ausgewählten Layers erstellen und bearbeiten");
						buttonCopy.setEnabled( true);
						buttonLoeschen.setToolTipText("Der ausgewählte Layer ist nicht löschbar");
						buttonLoeschen.setEnabled( false);
					}
					else {
						buttonBearbeiten.setText("Layer bearbeiten");
						buttonBearbeiten.setToolTipText("Details des augewählten Layers bearbeiten");
						buttonBearbeiten.setEnabled( true);
						buttonCopy.setToolTipText("Kopie des ausgewählten Layers erstellen und bearbeiten");
						buttonCopy.setEnabled( true);
						buttonLoeschen.setToolTipText("Den ausgewählten Layer löschen");
						buttonLoeschen.setEnabled( true);
					}
				}
			}
		};
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(listSelctionListener);
		
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_DELETE) {
					final Layer layer = layerList.get(table.getSelectedRow());
					final LayerManager instance = LayerManager.getInstance();
					if(instance.isChangeable( layer)) {
						instance.removeLayer(layer);
					}
				}
			}
		});
		
		_frame.add(buttonsPanelEast, BorderLayout.EAST);
		_frame.add(buttonsPanelSouth, BorderLayout.SOUTH);
		_frame.add( new JScrollPane(table), BorderLayout.CENTER);
		
		// Neu, Bearbeiten, Kopieren, Löschen
		ActionListener actionListenerNew = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				Layer newLayer = new Layer("", "", "");
				final String title = "GND: neuen Layer bearbeiten";
				if ( _layerDefinitionDialog == null) {
					_layerDefinitionDialog = new LayerDefinitionDialog(_connection, newLayer, true, true, title);
				} else {
					_layerDefinitionDialog.setLayer( newLayer, true, true);
					_layerDefinitionDialog.setTitle( title);
					_layerDefinitionDialog.setVisible( true);
				}
			}
		};
		buttonNeu.addActionListener(actionListenerNew);
		
		
		ActionListener actionListenerLoeschen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( table.getSelectedRowCount() == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie mindestens eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final int selectedRows[] = table.getSelectedRows();
				boolean allLayersRemoved = true;
				final LayerManager layerManager = LayerManager.getInstance();
				for ( int i = selectedRows.length-1; i >= 0; i--) {
					final Layer layer = layerManager.getLayer(selectedRows[i]);
					if ( !layerManager.removeLayer(layer)) {
						allLayersRemoved = false;
					}
				}
				layerManager.fireTableDataChanged();
				if ( !allLayersRemoved) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Es konnten nicht alle ausgewählten Layer gelöscht werden!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		};
		buttonLoeschen.addActionListener(actionListenerLoeschen);
		
		ActionListener actionListenerKopieren = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( table.getSelectedRowCount() != 1) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie genau eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final int selectedRow = table.getSelectedRow();
				final Layer layer = LayerManager.getInstance().getLayer(selectedRow);
				Layer newLayer = new Layer(layer.getName() + " (Kopie)", layer.getInfo(), 
						layer.getGeoReferenceType());
				newLayer.setDotCollection( (DOTCollection) layer.getDotCollection().clone());
				final String title = "GND: kopierten Layer bearbeiten";
				if ( _layerDefinitionDialog == null) {
					_layerDefinitionDialog = new LayerDefinitionDialog(_connection, newLayer, true, true, title);
				} else {
					_layerDefinitionDialog.setLayer( newLayer, true, true);
					_layerDefinitionDialog.setTitle( title);
					_layerDefinitionDialog.setVisible( true);
				}
			}
		};
		buttonCopy.addActionListener(actionListenerKopieren);
		
		ActionListener actionListenerBearbeiten = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int selectedRow = table.getSelectedRow();
				if ( selectedRow == -1) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie mindestens eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final Layer layer = LayerManager.getInstance().getLayer(selectedRow);
				final boolean changeable = LayerManager.getInstance().isChangeable(layer);
				final String title;
				if ( changeable) {
					title = "GND: Layer bearbeiten";
				} else {
					title = "GND: Layer betrachten";
				}
				if ( _layerDefinitionDialog == null) {
					_layerDefinitionDialog = new LayerDefinitionDialog(_connection, layer, changeable, false, title);
				} else {
					_layerDefinitionDialog.setLayer( layer, changeable, false);
					_layerDefinitionDialog.setTitle( title);
					_layerDefinitionDialog.setVisible( true);
				}
			}
		};
		buttonBearbeiten.addActionListener(actionListenerBearbeiten);
		
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#theLayerManagerDialog");
			}
		};
		buttonHilfe.addActionListener(actionListenerHelp);
		
		ActionListener actionListenerDialogSchliessen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		};
		schliessenButton.addActionListener( actionListenerDialogSchliessen);
		
		class FrameListener extends WindowAdapter {
			@Override
            public void windowClosing(WindowEvent e) {
				/*
				 * wenn nur noch ein einziges Fenster geöffnet ist 
				 * beendet sich das Programm beim Schließen des Fensters
				 */
				final Frame[] frames = JFrame.getFrames();
				int length = frames.length - 1;
				
				for(Frame frame : frames) {
					if(frame.isVisible()) {
					}
					else {
						length--;
					}
				}
				
				if(length == 0) {
					_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				}
			}
		}
		_frame.addWindowListener( new FrameListener());
		
		_frame.setVisible(true);
		_frame.pack();
		_frame.setSize(520, 300);
		_frame.setLocation(860, 50);
	}
	
	/**
	 * CellRenderer für die JTable im Verwaltungsdialog
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 8080 $
	 * 
	 */
	private class ComboTableCellRendererLayer extends DefaultTableCellRenderer {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if(value instanceof Layer) {
				Layer layer = (Layer)value;
				
				if(column == 0) {
					setText(layer.getName());
				}
				else if(column == 1) {
					if(layer.getGeoReferenceType() != null) {
						setText(layer.getGeoReferenceType());
					}
					else {
						setText("-");
					}
				}
				else if(column == 2) {
					if(layer.getGeoReferenceType() != null) {
						setText(layer.getGeoReferenceType());
					}
					else {
						setText("-");
					}
					
				}
				else {
					setText("-");
				}
				
			}
			else {
				setText(value.toString());
			}
			
			return this;
			
		}
	}
	
	/**
	 * Macht die Layerverwaltungsdialog sichtbar.
	 */
	public void showDialog () {
		_frame.setVisible(true);
	}

	/**
	 * Beendet den Layerverwaltungsdialog.
	 */
	public void closeDialog() {
		_frame.setVisible( false);
		_frame.dispose();
	}
}
