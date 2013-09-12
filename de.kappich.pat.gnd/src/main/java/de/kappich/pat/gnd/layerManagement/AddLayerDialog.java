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

import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.viewManagement.View;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Ein Dialog zum Hinzufügen eines Layers zu einer Ansicht.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 8058 $
 *
 */
@SuppressWarnings("serial")
public class AddLayerDialog extends JFrame {
	private View _view;
	
	/**
	 * Konstruktor zum Anlegen eines neuen AddLayerDialog
	 * 
	 * @param layerManagement
	 *            Layerverwaltung
	 * @param systemObjectTypes
	 *            Array von SystemObjektTypen
	 * @param representationTypeObjectManagement
	 *            DOT - Verwaltung
	 */
	public AddLayerDialog( final LayerManager layerManager, View view) {
		super("GND: Layer zur Ansicht hinzufügen");

		_view = view;
		
		setLayout(new BorderLayout());
		
		JButton hinzufuegenButton = new JButton("Layer hinzufügen");
		
		JButton helpButton = new JButton("Hilfe");
		
		JPanel panelButtons = new JPanel();
		panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.Y_AXIS));
		panelButtons.setBorder( BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		Dimension d = new Dimension(15, 15);
		panelButtons.add(Box.createRigidArea(d));
		
		panelButtons.add(hinzufuegenButton);
		panelButtons.add(Box.createRigidArea(d));
		
		panelButtons.add(helpButton);
		panelButtons.add(Box.createRigidArea(d));
		
		final JTable table = new JTable(layerManager) {
	        //Implement table cell tool tips.
            @Override
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);
                if (realColumnIndex == 0) {
                	LayerManager lm = (LayerManager) getModel();
                    tip = lm.getTooltipAt(rowIndex, colIndex);
                }
                return tip;
            }
        };

		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);
		
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_F1) {
					HelpPage.openHelp("#addLayer");
				}
			}
		});
		
		JButton closeButton = new JButton("Dialog schließen");
		
		JPanel closeButtonPanel = new JPanel();
		closeButtonPanel.setLayout(new BoxLayout(closeButtonPanel, BoxLayout.Y_AXIS));
		closeButtonPanel.setBorder( BorderFactory.createEmptyBorder(5, 10, 5, 10));
		closeButtonPanel.add( closeButton);
		
		
		add(panelButtons, BorderLayout.EAST);
		final JScrollPane comp = new JScrollPane(table);
		add(comp, BorderLayout.CENTER);
		add( closeButtonPanel, BorderLayout.SOUTH);
		
		ActionListener actionListenerHinzufuegen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = table.getSelectedRows();
				if (selectedRows.length == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie mindestens eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				setCursor( new Cursor(Cursor.WAIT_CURSOR));
				for ( int i : selectedRows) {
					_view.addLayer( layerManager.getLayer( i));
				}
				setCursor( new Cursor( Cursor.DEFAULT_CURSOR));
			}
		};
		hinzufuegenButton.addActionListener(actionListenerHinzufuegen);
		
		
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp("#addLayer");
			}
		};
		helpButton.addActionListener(actionListenerHelp);
		
		ActionListener actionListenerClose = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		closeButton.addActionListener(actionListenerClose);
		
		WindowListener windowListener = new WindowListener() {
			
			public void windowActivated(WindowEvent e) {
			}
			
			public void windowClosed(WindowEvent e) {
			}
			
			public void windowClosing(WindowEvent e) {
				/* wenn nur noch ein einziges Fenster geöffnet ist 
				 * beendet sich das Programm beim Schließen des Fensters */
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
					setDefaultCloseOperation(EXIT_ON_CLOSE);
				}
			}
			
			public void windowDeactivated(WindowEvent e) {
			}
			
			public void windowDeiconified(WindowEvent e) {
			}
			
			public void windowIconified(WindowEvent e) {
			}
			
			public void windowOpened(WindowEvent e) {
			}
			
		};
		
		addWindowListener(windowListener);
		
		setVisible(true);
		pack();
		setSize(500, 255);
		setLocation(new Point((int)(getBounds().getHeight() / 1.5 + 600), (int)(getBounds().getWidth() / 1.5)));
	}
}
