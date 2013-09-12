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
import de.kappich.pat.gnd.layerManagement.AddLayerDialog;
import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.layerManagement.LayerManager;
import de.kappich.pat.gnd.utils.SpringUtilities;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 * Ein Dialog zur Anzeige der Eigenschaften einer Ansicht.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9145 $
 */
public class ViewDialog {
	
	private static ArrayList<JFrame> _frames = new ArrayList<JFrame>();
	
	private final ViewManager _viewManager;
	
	private final View _view;
	
	private final boolean _changeable;	// Änderungen sind möglich oder nicht
	
	private final boolean _saveable; // Änderungen können in den Präferenzen gespeichert werden
	
	private final boolean _nameEditable; // der Name der Ansicht ist änderbar
	
	private final JTextField _nameTextField = new JTextField();
	
	private final JFrame _frame;
	
	private JTable _table;
	
	private AddLayerDialog _addLayerDialog = null;
	
	private final JButton _newLayerButton = new JButton("Layer hinzufügen");
	
	private final JButton _deleteLayerButton = new JButton("Layer entfernen");
	
	private final JButton _moveLayersUpwardsButton = new JButton("Layer aufwärts");
	
	private final JButton _moveLayersDownwardsButton = new JButton("Layer abwärts");
	
	private final JButton _saveViewButton = new JButton("Ansicht speichern");
	
	private final JButton _closeDialogButton = new JButton("Dialog schließen");
	
	private final JButton _helpButton = new JButton("Hilfe");
	
	/**
	 * Der ViewDialog zeigt die Eigenschaften einer Ansicht an, und wird auch dazu
	 * verwendet, diese zu bearbeiten oder eine neue ansicht anzulegen.
	 * 
	 * @param viewManager Die Ansichtsverwaltung
	 * @param view Die aktuell angezeigte Ansicht
	 * @param changeable Ist die Ansicht veränderbar?
	 * @param saveable Sind Veränderungen der Ansicht speiecherbar?
	 * @param nameEditable Ist der Name der Ansicht veränderbar?
	 */
	@SuppressWarnings("serial")
    private ViewDialog( final ViewManager viewManager, final View view, 
    		final boolean changeable, final boolean saveable, final boolean nameEditable,
    		final String title) {
		_viewManager = viewManager;
		_view = view;
		_changeable = changeable;
		_saveable = saveable;
		_nameEditable = nameEditable;
		
		if ( title != null) {
			_frame = new JFrame( title);
		} else {
			_frame = new JFrame( "GND: Ansichtsdialog");
		}
		_frame.setMinimumSize(new Dimension(782, 100));
		_frame.setPreferredSize(new Dimension(782, 350));
		_frame.setLocation(860, 50);
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new SpringLayout());
		JLabel nameLabel = new JLabel("Name: ");
		nameLabel.setPreferredSize( new Dimension(100, 20));
		_nameTextField.setText( _view.getName());
		_nameTextField.setEditable( _nameEditable);
		upperPanel.add(nameLabel);
		upperPanel.add(_nameTextField);
		upperPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		SpringUtilities.makeCompactGrid(upperPanel, 2, 5, 5);
		_frame.add( upperPanel, BorderLayout.NORTH);
			
		_table = new JTable(new ViewTableModelAdapter()) {
			//Implement table cell tool tips.
			@Override
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				int realColumnIndex = convertColumnIndexToModel(colIndex);
				ViewTableModelAdapter viewTableModel = (ViewTableModelAdapter) getModel();
				tip = viewTableModel.getTooltipAt(rowIndex, realColumnIndex);
				return tip;
			}
		};
		_table.setDefaultRenderer(Integer.class, new ViewTableIntegerCellRenderer());
		_table.setDefaultRenderer(Boolean.class, new ViewTableBooleanCellRenderer());
		_table.setRowHeight( 30);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_table.setEnabled(_changeable);	// Setzt man das nicht, so sind im Fall von _changeable = false, die Integer-Felder und die Checkboxen bearbeitbar, auch wenn für sie setEditable(false) aufgerufen wurde. Darauf wurde aber verzichtet, weil dan die Optik besser ist!
		addListSelectionListener();
		final Enumeration<TableColumn> columns = _table.getColumnModel().getColumns();
		int i = 0;
		while ( columns.hasMoreElements() ) {
			TableColumn column = columns.nextElement();
			if (i < 3) {
				column.setPreferredWidth(140);
			} else {
				column.setPreferredWidth(90);
			}
			i++;
		}
		_frame.add(new JScrollPane(_table), BorderLayout.CENTER);
		initLayerButtons();
		initControlButtons();
		_frame.pack();
		_frames.add( _frame);
		addFrameListener();
	}
	
	/**
	 * Diese Methode initialisiert einen neuen ViewDialog und öffnet ihn.
	 * 
	 * @param viewManager Die Ansichtsverwaltung
	 * @param view Die aktuell angezeigte Ansicht
	 * @param changeable Ist die Ansicht veränderbar?
	 * @param saveable Sind Veränderungen der Ansicht speiecherbar?
	 * @param nameEditable Ist der Name der Ansicht veränderbar?
	 */
	public static void runDialog( final ViewManager viewManager, final View view, 
			final boolean changeable, final boolean saveable, final boolean nameEditable,
			final String title) {
		ViewDialog dialog = new ViewDialog(viewManager, view, changeable, saveable, nameEditable, title);
		dialog.showDialog();
	}
	
	/**
	 * Diese Methode visualisiert den aufrufenden ViewDialog.
	 */
	public void showDialog () {
		_frame.setVisible(true);
	}
	
	/**
	 * Diese Methode schließt alle ViewDialog-Objekte.
	 */
	public static void closeAll() {
		for ( JFrame frame : _frames) {
			frame.dispose();
		}
	}
	
	@SuppressWarnings("serial")
	private class ViewTableIntegerCellRenderer extends JPanel implements TableCellRenderer {
		public ViewTableIntegerCellRenderer() {
			JLabel label = new JLabel("1:");
			add( label);
			JTextField textField = new JTextField();
			add( textField);
		}
		
		public Component getTableCellRendererComponent(JTable table, Object value, 
				boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setBackground(table.getSelectionBackground());
			} else {
				setBackground(table.getBackground());
			}
			JTextField textField = (JTextField) getComponent(1);
			if ( value == null) {
				textField.setText( "1");
				return this;
			} else if ( value instanceof Integer) {
				final Integer iValue = (Integer) value;
				if ( iValue<1) {
					textField.setText( iValue.toString());
					return this;
				}
			}
			textField.setText( value.toString());
			return this;
		}
	}
	
	@SuppressWarnings("serial")
	private class ViewTableBooleanCellRenderer extends JPanel implements TableCellRenderer {
		public ViewTableBooleanCellRenderer() {
			JCheckBox checkBox = new JCheckBox();
			this.add(checkBox);
		}
		public Component getTableCellRendererComponent(JTable table, Object value, 
				boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setBackground(table.getSelectionBackground());
			} else {
				setBackground(table.getBackground());
			}
			JCheckBox checkBox = (JCheckBox) getComponent(0);
			checkBox.setSelected((Boolean) value);
			return this;
		}
	}
	
	
	@SuppressWarnings("serial")
	private class ViewTableModelAdapter extends AbstractTableModel implements TableModel, View.ViewChangeListener {
		
		String[] columnNames = {"Layer-Name", "Einblenden", "Ausblenden", "Selektierbar", "Anzeigen"};
		public ViewTableModelAdapter() {
			_view.addChangeListener(this);
		}
		
		public int getRowCount() {
			return getViewEntries().size();
		}
		
		public int getColumnCount() {
			return columnNames.length;
		}
		
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0) {
				return getViewEntries().get(rowIndex).getLayer().getName();
			} else if (columnIndex == 1) {
				return getViewEntries().get(rowIndex).getZoomIn();
			} else if (columnIndex == 2) {
				return getViewEntries().get(rowIndex).getZoomOut();
			} else if (columnIndex == 3) {
				return getViewEntries().get(rowIndex).isSelectable();
			} else if (columnIndex == 4) {
				return getViewEntries().get(rowIndex).isVisible();
			}
			return null;
		}
		
		
		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}
		
		/*
		 * Achtung: JTable benutzt diese Methode, um Default-Renderer und Default-Editor der Zellen zu bestimmen;
		 * wird sie nicht überschrieben, so würde die Zellen mit Checkboxen für boolsche Werte durch die
		 * Textanzeige 'true' oder 'false' ersetzt, weil der falsche Renderer benutzt würde.
		 */
		@Override
		public Class<? extends Object> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}
		
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			ViewEntry viewEntry = getViewEntries().get(rowIndex);
			if (columnIndex == 0) {
				final Layer layer = viewEntry.getLayer();
				layer.setName((String)aValue);
				viewEntry.setLayer(layer);
			} else if (columnIndex == 1) {
				if ( aValue == null) {
					viewEntry.setZoomIn(Integer.MAX_VALUE);
				} else {
					final Integer iValue = (Integer) aValue;
					if ( iValue<1) {
						viewEntry.setZoomIn(Integer.MAX_VALUE);
					} else {
						viewEntry.setZoomIn((Integer)aValue);
					}
				}
			} else if (columnIndex == 2) {
				if ( aValue == null) {
					viewEntry.setZoomOut(1);
				} else {
					final Integer iValue = (Integer) aValue;
					if ( iValue<1) {
						viewEntry.setZoomOut(1);
					} else {
						viewEntry.setZoomOut((Integer)aValue);
					}
				}
			} else if (columnIndex == 3) {
				viewEntry.setSelectable((Boolean)aValue);
			} else if (columnIndex == 4) {
				viewEntry.setVisible((Boolean)aValue);
			}
		}
		
		public void viewEntriesSwitched(View view, int i, int j) {
			final ListSelectionModel selectionModel = _table.getSelectionModel();
			boolean iIsSelected = selectionModel.isSelectedIndex(i);
			boolean jIsSelected = selectionModel.isSelectedIndex(j);
			if (iIsSelected) {
				selectionModel.addSelectionInterval(j, j);
			} else {
				selectionModel.removeSelectionInterval(j, j);
			}
			if (jIsSelected) {
				selectionModel.addSelectionInterval(i, i);
			} else {
				selectionModel.removeSelectionInterval(i, i);
			}
		}
		
		public void viewEntryInserted(View view, final int newIndex) {
			fireTableRowsInserted(newIndex, newIndex);
		}
		
		public void viewEntryChanged(View view, int i) {
			// Das Selektionsmodel bleibt hier unangetastet!
			fireTableRowsUpdated(i, i);
		}
		
		public void viewEntryRemoved(View view, int i) {
			final ListSelectionModel selectionModel = _table.getSelectionModel();
			if (selectionModel.isSelectedIndex(i)) {
				selectionModel.removeSelectionInterval(i, i);
			}
			fireTableRowsDeleted(i, i);
		}
		
		public String getTooltipAt(int rowIndex, int colIndex) {
			if (colIndex == 0) {
				if (rowIndex >= 0 && rowIndex <= getViewEntries().size()) {
					return getViewEntries().get(rowIndex).getLayer().getInfo();
				} else {
					return null;
				}
			} else if (colIndex == 1) {
				return "Maßstab, ab dem der Layer eingeblendet wird";
			} else if (colIndex == 2) {
				return "Maßstab, ab dem der Layer ausgeblendet wird";
			} else if (colIndex == 3) {
				return "Objekte des Layers als selektierbar einstellen";
			} else if (colIndex == 4) {
				return "Layer anzeigen";
			}
			return null;
		}

		private List<ViewEntry> getViewEntries() {
			return _view.getViewEntries(false);
		}
	}
	
	private void initControlButtons() {
		if ( !_saveable) {
			_saveViewButton.setEnabled( false);
		}
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout( new SpringLayout());
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		buttonsPanel.add( _saveViewButton);
		buttonsPanel.add( _closeDialogButton);
		buttonsPanel.add( _helpButton);
		SpringUtilities.makeCompactGrid( buttonsPanel, 3, 20, 5);
		
		_frame.add(buttonsPanel, BorderLayout.SOUTH);
		
		ActionListener saveViewActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String viewName = _nameTextField.getText();
				if ( (viewName == null) || viewName.length() == 0) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte geben Sie einen Namen an!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if ( _viewManager.hasView( viewName)) {
					if ( _nameEditable) { // neuer oder kopierter View
						JOptionPane.showMessageDialog(
								new JFrame(),
								"Eine Ansicht mit dem Namen '" + viewName + "' existiert bereits!",
								"Fehler",
								JOptionPane.ERROR_MESSAGE);
						return;
					} else { // altes Zeug löschen
						_viewManager.removeView( _viewManager.getView(viewName));
					}
				}
				for ( ViewEntry entry : _view.getViewEntries(false)) {
					if ( entry.getZoomIn()<entry.getZoomOut()) {
						JOptionPane.showMessageDialog(
								new JFrame(),
								"Der Einblenden-Wert muss in jeder Zeile mindestens so groß wie der Ausblenden-Wert sein!",
								"Fehler",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				_view.setName( viewName);
				_viewManager.addView(_view);
				_view.setSomethingChanged( false);
			}
		};
		_saveViewButton.addActionListener( saveViewActionListener);
		
		ActionListener closeDialogActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( _view.hasSomethingChanged()) {
					askForSaveChanges();
				}
				_frame.setVisible( false);
				_frame.dispose();
			}
		};
		_closeDialogButton.addActionListener( closeDialogActionListener);
		
		ActionListener helpActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HelpPage.openHelp( "#viewDefinitionDialog");
			}
		};
		_helpButton.addActionListener( helpActionListener);
		
	}
	
	private void initLayerButtons() {
		_deleteLayerButton.setEnabled( false);
		_moveLayersUpwardsButton.setEnabled( false);
		_moveLayersDownwardsButton.setEnabled( false);
		if ( !_changeable) {
			_newLayerButton.setEnabled( false);
		}
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		Dimension d = new Dimension(40, 25);
		buttonsPanel.add(Box.createRigidArea(d));
		
		buttonsPanel.add(_newLayerButton);
		buttonsPanel.add(Box.createRigidArea(d));
		
		buttonsPanel.add(_deleteLayerButton);
		buttonsPanel.add(Box.createRigidArea(d));
		
		buttonsPanel.add(_moveLayersUpwardsButton);
		buttonsPanel.add(Box.createRigidArea(d));
		
		buttonsPanel.add(_moveLayersDownwardsButton);
		buttonsPanel.add(Box.createRigidArea(d));
		
		_frame.add(buttonsPanel, BorderLayout.EAST);
		
		// ActionListener für Neu, Rauf, Runter, Löschen und Ok
		ActionListener actionListenerNew = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (_addLayerDialog == null) {
					_addLayerDialog = new AddLayerDialog( LayerManager.getInstance(), _view);
				} else {
					_addLayerDialog.setVisible(true);
				}
			}
		};
		_newLayerButton.addActionListener(actionListenerNew);
		
		ActionListener actionListenerRaufRunterLoeschen = new ActionListener() {
			
			private void checkSelectedRow() {
				final int selectedRow = _table.getSelectedRow();
				if (selectedRow == -1) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
			private void actionPerformedRauf(ActionEvent e) {
				final int[] selectedRows = _table.getSelectedRows();
				if ((selectedRows.length == 0) || (selectedRows[0] == 0)) {
					return;
				}
				for ( int selected : selectedRows) {
					_view.switchTableLines(selected-1, selected);
				}
			}
			
			private void actionPerformedRunter(ActionEvent e) {
				final int[] selectedRows = _table.getSelectedRows();
				if (selectedRows[selectedRows.length-1] == _view.getViewEntries(false).size()-1) {
					return;
				}
				for ( int i = selectedRows.length-1; i >= 0; i--) {
					int selected = selectedRows[i];
					_view.switchTableLines(selected,selected+1);
				}
			}
			
			public void actionPerformedLoeschen(ActionEvent e) {
				final int[] selectedRows = _table.getSelectedRows();
				for ( int i =selectedRows.length-1; i >= 0; i--) {
					_view.remove( selectedRows[i]);
				}
			}
			
			public void actionPerformed(ActionEvent e) {
				checkSelectedRow();
				try {
					if(e.getSource() == _moveLayersUpwardsButton) {
						actionPerformedRauf(e);
					} else if(e.getSource() == _moveLayersDownwardsButton) {
						actionPerformedRunter(e);
					} else if(e.getSource() == _deleteLayerButton) {
						actionPerformedLoeschen(e);
					}
				}
				catch(ArrayIndexOutOfBoundsException e2) {
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Bitte wählen Sie eine Zeile aus der Liste aus!",
							"Fehler",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		_deleteLayerButton.addActionListener(actionListenerRaufRunterLoeschen);
		_moveLayersUpwardsButton.addActionListener(actionListenerRaufRunterLoeschen);
		_moveLayersDownwardsButton.addActionListener(actionListenerRaufRunterLoeschen);
	}
	
	private void addFrameListener() {
		class FrameListener extends WindowAdapter {
			@Override
            public void windowClosing(WindowEvent e) {
				if ( _view.hasSomethingChanged()) {
					askForSaveChanges();
				}
			}
		}
		_frame.addWindowListener( new FrameListener());
	}
	
	
	private void askForSaveChanges() {
		if ( _saveable) {
			Object[] options = {"Änderungen speichern", "Nicht speichern"};
			int n = JOptionPane.showOptionDialog(
					new JFrame(),
					"Änderungen speichern?",
					"Es wurden Änderungen an der Ansicht vorgenommen.",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);
			if ( n == 0) {
				_view.putPreferences(ViewManager.getPreferenceStartPath());
			}
		}
	}
	
	private void addListSelectionListener() {
		final ListSelectionModel selectionModel = _table.getSelectionModel();
		ListSelectionListener listSelectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = _table.getSelectedRow();
				if ( selectedRow == -1) {
					_deleteLayerButton.setEnabled( false);
					_moveLayersDownwardsButton.setEnabled( false);
					_moveLayersUpwardsButton.setEnabled( false);
				} else {
					_deleteLayerButton.setEnabled( _changeable);
					_moveLayersDownwardsButton.setEnabled( _changeable);
					_moveLayersUpwardsButton.setEnabled( _changeable);
				}
            }
		};
		selectionModel.addListSelectionListener( listSelectionListener);
	}
}
