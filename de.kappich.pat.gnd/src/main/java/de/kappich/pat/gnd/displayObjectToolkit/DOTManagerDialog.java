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
package de.kappich.pat.gnd.displayObjectToolkit;

import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.gnd.PluginManager;
import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.layerManagement.LayerManager;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectType;
import de.kappich.pat.gnd.pluginInterfaces.DisplayObjectTypePlugin;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
 * Der Verwaltungsdialog für alle Darstellungstypen.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8076 $
 *
 */
public class DOTManagerDialog {
	
	final private DOTManager _dotManager;
	
	final private ClientDavInterface _connection;
	
	private JFrame _frame = null;
	
	// Im Moment landen alle Frames, die zum Betrachten/Bearbeiten bestehender DOTs geöffnet
	// werden, in der folgenden Liste. Falls zu einem solchen DOT wieder ein Frame geöffnet werden
	// soll, so bekommt der bereits geöffnete den Fokus.
	final private Map<String, DOTDefinitionDialogFrame> _dotDefinitionDialogFrames = 
		new HashMap<String, DOTDefinitionDialogFrame>();
	
	/**
	 * Der DOTManagerDialog dient zur grafischen Verwaltung und Bearbeitung aller vom
	 * DOTManager verwalteten Darstellungstypen.
	 * 
	 * @param dotManager die Darstellungstypenverwaltung
	 * @param connection die Datenverteiler-Verbindung
	 */
	public DOTManagerDialog(DOTManager dotManager, ClientDavInterface connection) {
		
		_dotManager = dotManager;
		
		_connection = connection;
		
		_frame = new JFrame("GND: Darstellungstypenverwaltung");
		
		_frame.setLayout(new BorderLayout());
		
		JButton newDOTButton = new JButton("Neuer Darstellungstyp");
		
		final JButton editButton = new JButton("Darstellungstyp bearbeiten");
		final JButton copyButton = new JButton("Darstellungstyp kopieren");
		final JButton deleteButton = new JButton("Darstellungstyp löschen");
		final JButton helpButton = new JButton("Hilfe");
		
		JPanel buttonPanelEast = new JPanel();
		buttonPanelEast.setLayout(new BoxLayout(buttonPanelEast, BoxLayout.Y_AXIS));
		
		Dimension d = new Dimension(15, 15);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(newDOTButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(editButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(copyButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(deleteButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(helpButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		editButton.setEnabled(false);
		copyButton.setEnabled(false);
		deleteButton.setEnabled(false);
		
		JPanel buttonsPanelSouth = new JPanel();
		buttonsPanelSouth.setLayout(new SpringLayout());
		buttonsPanelSouth.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		JButton schliessenButton = new JButton("Dialog schließen");
		buttonsPanelSouth.add( schliessenButton);
		SpringUtilities.makeCompactGrid(buttonsPanelSouth, 1, 20, 5);
		
		final JTable table = new JTable(_dotManager);
		
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);
		table.setDefaultRenderer(Object.class, new ComboTableCellRendererLayer());
		// vordefinierte Darstellungsobjekttypen dürfen nicht bearbeitet oder gelöscht werden
		final ListSelectionModel selectionModel = table.getSelectionModel();
		
		ListSelectionListener listSelctionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = table.getSelectedRow();
				if(selectedRow == -1) {
					editButton.setEnabled(false);
					editButton.setToolTipText( "Es ist kein Darstellungstyp ausgewählt worden");
					copyButton.setEnabled(false);
					copyButton.setToolTipText( "Es ist kein Darstellungstyp ausgewählt worden");
					deleteButton.setEnabled(false);
					deleteButton.setToolTipText( "Es ist kein Darstellungstyp ausgewählt worden");
				}
				else {
					final boolean changeable = _dotManager.isChangeable( _dotManager.getDisplayObjectType(selectedRow));
					if(!changeable) {
						editButton.setText("Darstellungstyp betrachten");
						editButton.setEnabled(true);
						editButton.setToolTipText("Details des ausgewählten Darstellungstypen betrachten");
						copyButton.setEnabled(true);
						copyButton.setToolTipText("Kopie des ausgewählten Darstellungstypen erstellen und bearbeiten");
						deleteButton.setEnabled(false);
						deleteButton.setToolTipText("Der ausgewählte Darstellungstyp kann nicht gelöscht werden");
					}
					else {
						editButton.setText("Darstellungstypen bearbeiten");
						editButton.setEnabled(true);
						editButton.setToolTipText("Den ausgewählten Darstellungstypen bearbeiten");
						copyButton.setEnabled(true);
						copyButton.setToolTipText("Kopie des ausgewählten Darstellungstypen erstellen und bearbeiten");
						deleteButton.setEnabled(true);
						deleteButton.setToolTipText("Den ausgewählten Darstellungstypen löschen");
					}
					deleteButton.setEnabled(changeable);
				}
			}
		};
		
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_DELETE) {
					final DisplayObjectType type = _dotManager.getDisplayObjectType(table.getSelectedRow());
					
					if(_dotManager.isChangeable( type)) {
						_dotManager.deleteDisplayObjectType( type);
					}
				}
			}
		});
		
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(listSelctionListener);
		
		_frame.add(buttonPanelEast, BorderLayout.EAST);
		_frame.add(buttonsPanelSouth, BorderLayout.SOUTH);
		_frame.add( new JScrollPane(table), BorderLayout.CENTER);
		
		// Neu, Bearbeiten, Kopieren, Löschen
		ActionListener actionListenerNew = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				final JLabel label = new JLabel( "Bitte wählen Sie eine Art aus:");
				final JPanel labelPanel = new JPanel();
				labelPanel.setLayout( new SpringLayout());
				labelPanel.add( label);
				SpringUtilities.makeCompactGrid(labelPanel, 1, 20, 5);
				final Vector<String> items = PluginManager.getAllPluginNames();
				final JComboBox pluginComboBox = new JComboBox( items);
				final JButton chooseButton = new JButton( "Auswählen");
				final JButton localHelpButton = new JButton("Hilfe");
				final JPanel choicePanel = new JPanel();
				choicePanel.setLayout( new SpringLayout());
				choicePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				choicePanel.add( pluginComboBox);
				choicePanel.add( chooseButton);
				choicePanel.add( localHelpButton);
				SpringUtilities.makeCompactGrid(choicePanel, 3, 20, 5);
				final JPanel allPanel = new JPanel();
				allPanel.setLayout( new BoxLayout( allPanel, BoxLayout.Y_AXIS));
				allPanel.add( labelPanel);
				allPanel.add( choicePanel);
				allPanel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
				final JDialog newDialog = new JDialog(_frame, true);
				newDialog.setTitle( "Art des Darstellungstyps auswählen");
				newDialog.add( allPanel);
				newDialog.pack();
				newDialog.setLocation( 900, 80);
				
				ActionListener okayListener = new ActionListener() {
					public void actionPerformed(ActionEvent f) {
						final Object selectedItem = pluginComboBox.getSelectedItem();
						if ( selectedItem == null) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte wählen Sie eine Zeile aus der Liste aus!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						final String s = (String) selectedItem;
						final DisplayObjectTypePlugin plugin = PluginManager.getPlugin( s);
						if ( plugin == null) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Es wurde kein Plugin zu '" + s + "' gefunden!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						final DisplayObjectType type = plugin.getDisplayObjectType();
						newDialog.dispose();
						final String title = "GND: neuen Darstellungstyp bearbeiten";
						DOTDefinitionDialogFrame dotDDFrame = new DOTDefinitionDialogFrame( 
								DOTManagerDialog.this, _connection, _dotManager, type, true, false, title);
						dotDDFrame.setVisible( true);
					}
				};
				chooseButton.addActionListener( okayListener);
				
				newDialog.setVisible( true);
			}
		};
		newDOTButton.addActionListener(actionListenerNew);
		
		
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
				boolean allDOTsRemoved = true;
				DisplayObjectType notRemovableDOT = null;
				for ( int i = selectedRows.length-1; i >= 0; i--) {
					final DisplayObjectType type = _dotManager.getDisplayObjectType(selectedRows[i]);
					if ( !_dotManager.deleteDisplayObjectType(type)) {
						allDOTsRemoved = false;
						notRemovableDOT = _dotManager.getDisplayObjectType(selectedRows[i]);
					}
				}
				if ( !allDOTsRemoved) {
					List<String> layerNames = LayerManager.getInstance().getLayersUsingTheDisplayObjectType( 
							notRemovableDOT.getName());
					String infoString = "Der Darstellungstyp '" + notRemovableDOT.getName() + 
						"' kann nicht gelöscht werden, weil er in folgenden Layern verwendet wird: ";
					boolean commaNeeded = false;
					for ( String layerName : layerNames) {
						if ( commaNeeded ) {
							infoString += ", ";
						} else {
							commaNeeded = true;
						}
						infoString += layerName;
					}
					JOptionPane.showMessageDialog(
							new JFrame(),
							infoString,
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		};
		deleteButton.addActionListener(actionListenerLoeschen);
		
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
				final DisplayObjectType type = _dotManager.getDisplayObjectType(selectedRow);
				final DisplayObjectType newType = type.getCopy( type.getName() + " (Kopie)");
				final String title = "GND: kopierten Darstellungstyp bearbeiten";
				DOTDefinitionDialogFrame dotDDFrame = new DOTDefinitionDialogFrame( 
						DOTManagerDialog.this, _connection, _dotManager, newType, true, false, title);
				dotDDFrame.setVisible( true);
			}
		};
		copyButton.addActionListener(actionListenerKopieren);
		
		ActionListener actionListenerBearbeiten = new ActionListener() {
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
				final DisplayObjectType type = _dotManager.getDisplayObjectType(selectedRow);
				final String typeName = type.getName();
				final boolean changeable = _dotManager.isChangeable( type);
				final String title;
				if ( changeable) {
					title = "GND: Darstellungstyp bearbeiten";
				} else {
					title = "GND: Darstellungstyp betrachten";
				}
				if ( !_dotDefinitionDialogFrames.containsKey( typeName)) {
					DOTDefinitionDialogFrame dotDDFrame = new DOTDefinitionDialogFrame( 
							DOTManagerDialog.this, _connection, _dotManager, 
							type, changeable, true, title);
					dotDDFrame.setVisible( true);
					_dotDefinitionDialogFrames.put( typeName, dotDDFrame);
				} else {
					DOTDefinitionDialogFrame dotDDFrame = _dotDefinitionDialogFrames.get( typeName);
					dotDDFrame.setTitle( title);
					dotDDFrame.setVisible( true);
					dotDDFrame.requestFocus();
				}
			}
		};
		editButton.addActionListener(actionListenerBearbeiten);
		
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#theDisplayObjectTypeManagerDialog");
			}
		};
		helpButton.addActionListener(actionListenerHelp);
		
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
		
		_frame.pack();
		_frame.setSize(520, 300);
		_frame.setLocation(860, 50);
		_frame.setVisible(true);
	}
	
	/*
	 * CellRenderer für die JTable im Verwaltungsdialog
	 * 
	 */
	private class ComboTableCellRendererLayer extends DefaultTableCellRenderer {
		
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
	 * Macht den DOTManagerDialog sichtbar.
	 */
	public void showDialog () {
		_frame.setVisible(true);
	}
	
	/**
	 * Schließt den DOTManagerDialog.
	 */
	public void closeDialog() {
		_frame.setVisible( false);
		_frame.dispose();
	}
	
	/**
	 * Der übergebene DOTDefinitionDialogFrame wurde geschlossen und muss aus der Verwaltung gelöscht werden.
	 * 
	 * @param dotDDFrame der geschlossene DOTDefinitionDialogFrame
	 */
	public void dialogDisposed( DOTDefinitionDialogFrame dotDDFrame) {
		final String dotName = dotDDFrame.getDisplayObjectType().getName();
		if ( _dotDefinitionDialogFrames.containsKey( dotName)) {
			_dotDefinitionDialogFrames.remove(dotName);
		}
	}
	
	/**
	 * Der Darstellungstyp wurde verändert.
	 * 
	 * @param formerDisplayObjectType der alte Darstellungstyp
	 * @param newDisplayObjectType der neue Darstellungstyp
	 */
	public void dialogChanged( DisplayObjectType formerDisplayObjectType, 
			DisplayObjectType newDisplayObjectType) {
		if ( formerDisplayObjectType == null) {
			return;
		}
		final String dotName = formerDisplayObjectType.getName();
		if ( _dotDefinitionDialogFrames.containsKey( dotName)) {
			DOTDefinitionDialogFrame dotDDFrame = _dotDefinitionDialogFrames.remove(dotName);
			if ( newDisplayObjectType != null) {
				_dotDefinitionDialogFrames.put( newDisplayObjectType.getName(), dotDDFrame);
			}
		}
	}
}
