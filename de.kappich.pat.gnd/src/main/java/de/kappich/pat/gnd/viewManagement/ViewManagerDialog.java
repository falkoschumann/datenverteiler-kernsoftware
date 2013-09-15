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
package de.kappich.pat.gnd.viewManagement;

import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.gnd.GenericNetDisplay;
import de.kappich.pat.gnd.utils.SpringUtilities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Eine Dialog, der alle Ansichten auflistet und über Schaltflächen Möglichkeiten bietet, diese zu betrachten,
 * zu bearbeiten, zu löschen oder neue einzuführen. Der ViewManagerDialog zeigt die Inhalte der 
 * {@link ViewManager Ansichtsverwaltung}. 
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8080 $
 *
 */
public class ViewManagerDialog {
	
	final private GenericNetDisplay _gnd;
	
	final private JDialog _dialog;
	
	final private JTable _table;
	
	private boolean _modal;
	
	private JButton _aktivierenButton = new JButton("Ansicht aktivieren");
	private JButton _startAnsichtButton = new JButton("Als Startansicht festlegen");
	private JButton _schliessenButton = new JButton("Dialog schließen");
	private JButton _neueAnsichtButton = new JButton("Neue Ansicht");
	private JButton _bearbeitenButton = new JButton("Ansicht bearbeiten");
	private JButton _kopierenButton = new JButton("Ansicht kopieren");
	private JButton _loeschenButton = new JButton("Ansicht löschen");
	private JButton _helpButton = new JButton("Hilfe");
	
	/**
	 * Zeigt den Dialog an.
	 */
	public void showDialog() {
		_dialog.setVisible(true);
	}
	
	/**
	 * Schließt den Dialog.
	 */
	public void closeDialog() {
		_dialog.setVisible( false);
		_dialog.dispose();
	}
	
	private void setDialogModalSettings( boolean modal) {
		_modal = modal;
		if ( _modal) {
			_dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			_neueAnsichtButton.setEnabled( false);
			_loeschenButton.setEnabled( false);
			_helpButton.setEnabled( false);
			class MyWindowListener extends WindowAdapter {
				public MyWindowListener() {
					super();
				}
				@Override
				public void windowClosing( WindowEvent e) {
					Object[] options = {"GND beenden", "Abbrechen"};
					int n = JOptionPane.showOptionDialog(
							new JFrame(),
							"Entweder Sie aktivieren eine Ansicht aus oder die GND wird beendet.",
							"Präferenzen importieren",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);
					if ( n == 0) {
						if ( _gnd.isStandAlone()) {
							System.exit(0);
						} else {
							_gnd.dispose();
						}
					}
				}
			}
			_dialog.addWindowListener( new MyWindowListener());
			
			for ( ActionListener listener : _schliessenButton.getActionListeners()) {
				_schliessenButton.removeActionListener( listener);
			}
			final ActionListener cancelActionListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Object[] options = {"GND beenden", "Abbrechen"};
					int n = JOptionPane.showOptionDialog(
							new JFrame(),
							"Entweder Sie aktivieren eine Ansicht aus oder die GND wird beendet.",
							"Präferenzen importieren",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);
					if ( n == 0) {
						if ( _gnd.isStandAlone()) {
							System.exit(0);
						} else {
							_gnd.dispose();
						}
					}
				}
			};
			_schliessenButton.addActionListener( cancelActionListener);

		} else {
			_dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			_neueAnsichtButton.setEnabled( true);
			_helpButton.setEnabled( true);
			for ( WindowListener listener : _dialog.getWindowListeners()) {
				_dialog.removeWindowListener(listener);	
			}
			for ( ActionListener listener : _schliessenButton.getActionListeners()) {
				_schliessenButton.removeActionListener( listener);
			}
			ActionListener actionListenerDialogSchliessen = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeDialog();
				}
			};
			_schliessenButton.addActionListener( actionListenerDialogSchliessen);
		}
	}
	
	/**
	 * Konstruiert den Dialog zur Anzeige bzw. Auswahl aller Ansichten. Der Dialog kennt das
	 * die {@link GenericNetDisplay Generische-Netzdarstellung}, zu der er gehört, und kann
	 * bei Bedarf modal gestartet werden.
	 * 
	 * @param gnd die Netzdarstellung
	 * @param modal ist der Dialog modal?
	 */
	public ViewManagerDialog( GenericNetDisplay gnd, boolean modal) {
		_gnd = gnd;
		_modal = modal;
		
		JPanel buttonPanelEast = new JPanel();
		buttonPanelEast.setLayout(new BoxLayout(buttonPanelEast, BoxLayout.Y_AXIS));
		buttonPanelEast.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		Dimension d = new Dimension(15, 15);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(_neueAnsichtButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(_bearbeitenButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(_kopierenButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(_loeschenButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		buttonPanelEast.add(_helpButton);
		buttonPanelEast.add(Box.createRigidArea(d));
		
		JPanel buttonPanelSouth = new JPanel();
		buttonPanelSouth.setLayout(new SpringLayout());
		buttonPanelSouth.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		buttonPanelSouth.add( _aktivierenButton);
		buttonPanelSouth.add( _startAnsichtButton);
		buttonPanelSouth.add( _schliessenButton);
		SpringUtilities.makeCompactGrid(buttonPanelSouth, 3, 10, 5);

		List<JButton> restrictedButtonList = new ArrayList<JButton>();
		restrictedButtonList.add( _neueAnsichtButton);
		restrictedButtonList.add( _loeschenButton);
		restrictedButtonList.add( _helpButton);
		
		_bearbeitenButton.setEnabled(false);
		_kopierenButton.setEnabled(false);
		_loeschenButton.setEnabled(false);
		_aktivierenButton.setEnabled(false);
		_startAnsichtButton.setEnabled(false);
		
		_dialog = new JDialog( _gnd, _modal);
		setDialogModalSettings( _modal);
		_dialog.setTitle("GND: Ansichtsverwaltung");
		_dialog.setPreferredSize( new Dimension( 520, 300));
		_dialog.setLocation(950, 50);
		
		_dialog.add(buttonPanelEast, BorderLayout.EAST);
		_dialog.add(buttonPanelSouth, BorderLayout.SOUTH);
		
		_table = new JTable( ViewManager.getInstance());
		_table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		_table.setFillsViewportHeight(true);
		final JScrollPane comp = new JScrollPane(_table);
		_dialog.add(comp, BorderLayout.CENTER);
		
		_dialog.pack();
		
		final ListSelectionModel selectionModel = _table.getSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// vordefinierte Darstellungsobjekttypen dürfen nicht bearbeitet oder gelöscht werden
		ListSelectionListener listSelectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = _table.getSelectedRow();
				if ( selectedRow == -1) {
					_bearbeitenButton.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					_bearbeitenButton.setEnabled(false);
					_kopierenButton.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					_kopierenButton.setEnabled(false);
					_loeschenButton.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					_loeschenButton.setEnabled(false);
					_aktivierenButton.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					_aktivierenButton.setEnabled(false);
					_startAnsichtButton.setToolTipText( "Es ist keine Ansicht ausgewählt worden");
					_startAnsichtButton.setEnabled(false);
				}
				else {
					final ViewManager instance = ViewManager.getInstance();
					final boolean changeable = instance.isChangeable( instance.getView(selectedRow));
					if ( !changeable) {
						_bearbeitenButton.setText( "Ansicht betrachten");
						_bearbeitenButton.setToolTipText( "Details der Ansicht betrachten");
						_loeschenButton.setToolTipText( "Die ausgewählte Ansicht ist nicht löschbar");
						_loeschenButton.setEnabled(false);
					} else {
						_bearbeitenButton.setText( "Ansicht bearbeiten");
						_bearbeitenButton.setToolTipText( "Details der ausgewählten Ansicht bearbeiten");
						_loeschenButton.setToolTipText( "Die ausgewählte Ansicht löschen");
						_loeschenButton.setEnabled(true && !_modal);
					}
					_bearbeitenButton.setEnabled(true && !_modal);
					_kopierenButton.setToolTipText( "Kopie der ausgewählten Ansicht erstellen und bearbeiten");
					_kopierenButton.setEnabled(true && !_modal);
					_aktivierenButton.setToolTipText( "Die ausgewählte Ansicht zur aktuellen Ansicht machen");
					_aktivierenButton.setEnabled(true);
					_startAnsichtButton.setToolTipText("Die ausgewählte Ansicht zur Startansicht machen");
					_startAnsichtButton.setEnabled(true && !_modal);
				}
			}
		};
		selectionModel.addListSelectionListener(listSelectionListener);
		
		_table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_DELETE) {
					final View view = ViewManager.getInstance().getView( _table.getSelectedRow());
					if(ViewManager.getInstance().isChangeable( view)) {
						ViewManager.getInstance().removeView(view);
					}
				}
			}
		});
		
		
		ActionListener actionListenerAnsichtAuswaehlen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int[] selectedRows = _table.getSelectedRows();
				if (selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				synchronized(this) {
					_dialog.setVisible(false);	// damit niemand auf die Idee kommt, nochmal zu drücken
					
					Runnable doSetSplitPaneFromView = new Runnable() {
						public void run() {
							final View view = ViewManager.getInstance().getView(selectedRows[0]);
							_gnd.setSplitPaneFromView(view);
						}
					};
					Thread setSplitPaneFromViewThread = new Thread( doSetSplitPaneFromView);
					setSplitPaneFromViewThread.start();
					
					setDialogModalSettings(false);
					_table.clearSelection();
                }
			}
		};
		_aktivierenButton.addActionListener(actionListenerAnsichtAuswaehlen);
		
		ActionListener actionListenerStartViewFestlegen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int[] selectedRows = _table.getSelectedRows();
				if (selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final View view = ViewManager.getInstance().getView(selectedRows[0]);
				_gnd.writeStartViewNamePreference( view.getName());
			}
		};
		_startAnsichtButton.addActionListener( actionListenerStartViewFestlegen);
		
		ActionListener actionListenerNeueAnsicht = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				View view = new View();
				final String title = "GND: neue Ansicht bearbeiten";
				ViewDialog.runDialog( ViewManager.getInstance(), view, true, true, true, title);
			}
		};
		_neueAnsichtButton.addActionListener(actionListenerNeueAnsicht);
		
		ActionListener actionListenerAnsichtBearbeiten = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = _table.getSelectedRows();
				if (selectedRows.length != 1) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie genau eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final View view = ViewManager.getInstance().getView( selectedRows[0]);
				final boolean changeable = ViewManager.getInstance().isChangeable(view);
				final String title;
				if ( changeable ) {
					title = "GND: Ansicht bearbeiten";
				} else {
					title = "GND: Ansicht betrachten";
				}
				ViewDialog.runDialog( ViewManager.getInstance(), view, changeable, changeable, false, title);
			}
		};
		_bearbeitenButton.addActionListener( actionListenerAnsichtBearbeiten);
		
		ActionListener actionListenerAnsichtKopieren = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = _table.getSelectedRows();
				if (selectedRows.length != 1) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie genau eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final View view = ViewManager.getInstance().getView( selectedRows[0]);
				final View copiedView = view.getCopy( view.getName() + " (Kopie)");
				final String title = "GND: kopierte Ansicht bearbeiten";
				ViewDialog.runDialog( ViewManager.getInstance(), copiedView, true, true, true, title);
			}
		};
		_kopierenButton.addActionListener( actionListenerAnsichtKopieren);
		
		ActionListener actionListenerAnsichtLoeschen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = _table.getSelectedRows();
				if (selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				for (int i = selectedRows.length-1; i >= 0; i--) {
					int selected = selectedRows[i];
					// Sicherheitshalber, obwohl im Moment ist das Selektionsmodell im Single-Selektions-Modus.
					if ( !ViewManager.getInstance().removeView(ViewManager.getInstance().getView(selected))) {
						JOptionPane.showMessageDialog(
								new JFrame(),
								"Der View " + ViewManager.getInstance().getView(selected).getName() + 
								" kann nicht gelöscht werden!",
								"Fehler",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		};
		_loeschenButton.addActionListener(actionListenerAnsichtLoeschen);
		
		ActionListener actionListenerHilfe = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#theViewManagerDialog");
			}
		};
		_helpButton.addActionListener( actionListenerHilfe);
	}
}
