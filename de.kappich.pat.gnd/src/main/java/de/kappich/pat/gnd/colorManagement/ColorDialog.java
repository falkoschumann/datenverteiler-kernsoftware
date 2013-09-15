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
package de.kappich.pat.gnd.colorManagement;

import de.kappich.pat.gnd.documentation.HelpPage;
import de.kappich.pat.gnd.utils.SpringUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/**
 * Dieser Dialog zeigt alle verfügbaren Farben der Farbenverwaltung an.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7864 $
 *
 */
@SuppressWarnings("serial")
public class ColorDialog extends JFrame {
	
	private final JComboBox _comboBox;
	
	private final JColorChooser _colorChooser;
	
	/**
	 * Ein ColorDialog dient zur grafischen Bearbeitung der vom ColorManager verwalteten Farben.
	 */
	public ColorDialog () {
		super( "GND: Farbenverwaltung");
		
		JLabel comboLabel = new JLabel("Existierende Farben: ");
		comboLabel.setPreferredSize( new Dimension(200, 20));
		_comboBox = new JComboBox( ColorManager.getInstance().getColorNames());
		
		ActionListener comboBoxListener = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				final Object item = _comboBox.getSelectedItem();
				if ( item != null && (item instanceof String)) {
					String colorName = (String) item;
					final Color color = ColorManager.getInstance().getColor( colorName);
					_colorChooser.setColor(color);
				}
			}
		};
		_comboBox.addActionListener( comboBoxListener);
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout( new SpringLayout());
		upperPanel.setBorder( BorderFactory.createEmptyBorder(20, 20, 20, 20));
		upperPanel.add( comboLabel);
		upperPanel.add( _comboBox);
		SpringUtilities.makeCompactGrid( upperPanel, 2, 2, 2);
		
		setLayout( new BorderLayout());
		add( new JScrollPane( upperPanel), BorderLayout.NORTH);
		_colorChooser = new JColorChooser();
		final Object item = _comboBox.getSelectedItem();
		if ( item != null && (item instanceof String)) {
			String colorName = (String) item;
			final Color color = ColorManager.getInstance().getColor( colorName);
			_colorChooser.setColor(color);
		}
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new SpringLayout());
		centerPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
		centerPanel.add( new JScrollPane( _colorChooser));
		add( centerPanel, BorderLayout.CENTER);
		
		JButton newColorButton = new JButton("Neue Farbe");
		JButton deleteColorButton = new JButton("Farbe löschen");
		JButton cancelButton = new JButton("Dialog beenden");
		JButton helpButton = new JButton("Hilfe");
		addButtonListeners( newColorButton, deleteColorButton, cancelButton, helpButton);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new SpringLayout());
		
		buttonsPanel.add(newColorButton);
		buttonsPanel.add(deleteColorButton);
		buttonsPanel.add(cancelButton);
		buttonsPanel.add(helpButton);
		
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 5, 20));
		SpringUtilities.makeCompactGrid(buttonsPanel, 4, 20, 5);
		add(new JScrollPane( buttonsPanel), BorderLayout.SOUTH);
		
		setLocation( 100, 100);
		pack();
		setSize(540, 580);
		setVisible(true);
	}
	
	private void addButtonListeners( JButton newColorButton,
			JButton deleteColorButton,
			JButton cancelButton, 
			JButton helpButton) {
		
        class NewColorDialog extends JDialog {
			
			private final JTextField _nameTextField; 
			
			private final JColorChooser _dialogsColorChooser;
			
			NewColorDialog () {
				super(ColorDialog.this, true);
				setTitle("GND: neue Farbe anlegen");
				setLayout( new BorderLayout());
				
				JLabel nameLabel = new JLabel("Name der Farbe: ");
				nameLabel.setPreferredSize( new Dimension(100, 20));
				_nameTextField = new JTextField();
				
				JPanel upperPanel = new JPanel();
				upperPanel.setLayout( new SpringLayout());
				upperPanel.setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20));
				upperPanel.add( nameLabel);
				upperPanel.add( _nameTextField);
				SpringUtilities.makeCompactGrid( upperPanel, 2, 2, 2);
				add( new JScrollPane( upperPanel), BorderLayout.NORTH);
				
				JPanel centerPanel = new JPanel();
				centerPanel.setLayout( new SpringLayout());
				centerPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
				_dialogsColorChooser = new JColorChooser();
				centerPanel.add( new JScrollPane( _dialogsColorChooser));
				add( centerPanel, BorderLayout.CENTER);
				
				JButton saveButton = new JButton( "Farbe speichern");
				JButton closeButton = new JButton( "Dialog beenden");
				JButton localHelpButton = new JButton( "Hilfe");
				
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new SpringLayout());
				buttonPanel.setBorder( BorderFactory.createEmptyBorder(20, 20, 20, 20));
				
				buttonPanel.add(saveButton);
				buttonPanel.add(closeButton);
				buttonPanel.add(localHelpButton);
				
				SpringUtilities.makeCompactGrid(buttonPanel, 3, 20, 5);
				add( new JScrollPane( buttonPanel), BorderLayout.SOUTH);
				
				setLocation( 200, 100);
				pack();
				setSize(500, 560);
				
				ActionListener actionListenerSave = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						final String colorName = _nameTextField.getText();
						if ( colorName.length() == 0) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte geben Sie einen Namen für die Farbe an!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						final String lowerCaseColorName = colorName.toLowerCase();
						_nameTextField.setText( lowerCaseColorName);
						if ( ColorManager.getInstance().hasColor( lowerCaseColorName)) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Eine Farbe mit diesem Namen existiert bereits!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						final Color newColor = _dialogsColorChooser.getColor();
						if ( newColor == null ) {
							JOptionPane.showMessageDialog(
									new JFrame(),
									"Bitte wählen Sie eine Farbe aus!",
									"Fehler",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						ColorManager.getInstance().addColor(lowerCaseColorName, newColor, true);
						_comboBox.addItem( lowerCaseColorName);
						_comboBox.setSelectedItem( lowerCaseColorName);
					}
				};
				saveButton.addActionListener( actionListenerSave);
				
				ActionListener actionListenerClose = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible( false);
					}
				};
				closeButton.addActionListener( actionListenerClose);
				
				ActionListener actionListenerHelp = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						HelpPage.openHelp( "#newColorDialog");
					}
				};
				localHelpButton.addActionListener( actionListenerHelp);
				
			}
			
			/**
			 * Macht den Dialog sichtbar.
			 */
			public void runDialog() {
				final Color color = _colorChooser.getColor();
				if ( color != null) {
					_dialogsColorChooser.setColor(color);
				} 
				setVisible( true);
			}
		}
		
		
		ActionListener actionListenerNewColor = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NewColorDialog newColorDialog = new NewColorDialog();
				newColorDialog.runDialog();
			}
		};
		newColorButton.addActionListener( actionListenerNewColor);
		
		ActionListener actionListenerDeleteColor = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Object item = _comboBox.getSelectedItem();
				if ( (item != null) && ( item instanceof String)) {
					String colorName = (String) item;
					if ( !ColorManager.getInstance().deleteColor( colorName)) {
						JOptionPane.showMessageDialog(
								new JFrame(),
								"Die Farbe kann nicht gelöscht werden, weil sie verwendet wird!",
								"Fehler",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				_comboBox.removeItem( item);
			}
		};
		deleteColorButton.addActionListener( actionListenerDeleteColor);
			
		ActionListener actionListenerCancel = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		cancelButton.addActionListener( actionListenerCancel);
		
		ActionListener actionListenerHelp = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#colorManagerDialog");
			}
		};
		helpButton.addActionListener( actionListenerHelp);
	}
}
