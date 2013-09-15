/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * 
 * This file is part of de.kappich.pat.configBrowser.
 * 
 * de.kappich.pat.configBrowser is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.configBrowser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.configBrowser; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.pat.configBrowser.main;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Dimension;
import java.awt.Toolkit;

import de.bsvrz.dav.daf.main.config.DataModel;

/**
 * Realisiert ein Swing-JPanel zur Darstellung der Konfigurationsobjekte.
 * @author Kappich Systemberatung
 * @version $Revision: 5052 $
 */
public class DataModelTreePanel extends JPanel {

	private JTree _tree;

	public DataModelTreePanel(DataModel dm) {
		_tree = new JTree(new DataModelTreeModel(dm));
		JScrollPane scrollPane= new JScrollPane(_tree);
		setLayout(new java.awt.BorderLayout());
		_tree.setShowsRootHandles(true);
		add(scrollPane, java.awt.BorderLayout.CENTER);
    }

	public static JFrame showTreeFrame(DataModel dm) {
		JFrame frame = new JFrame("Datenmodell");
		frame.getContentPane().add("Center", new DataModelTreePanel(dm) );
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		int w = 400;
		int h = 600;
		frame.setSize(w, h);
		Dimension screenSize= Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(screenSize.width/3 - w/2, screenSize.height/2 - h/2);
		frame.setVisible(true);
		return frame;
	}
//    public static void main(String[] args) {
//        String xmlFileName="e:\\datenmodell\\meta.xml";
//        if(args.length>1) throw new Error("Zu viele Argumente");
//        if(args.length==1) xmlFileName= args[0];
//        DataModel dm= null;
//        try {
//            dm= new XmlDataModel(xmlFileName);
//        }
//        catch(Exception e) {
//            System.err.println("Datenmodell konnte nicht erzeugt werden");
//            return;
//        }
//        JFrame frame = new JFrame("Datenmodell");
//
//		frame.getContentPane().add("Center", new DataModelTreePanel(dm) );
//
//        frame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                System.exit(0);
//            }
//        });
//
//		int w = 400;
//		int h = 600;
//		frame.setSize(w, h);
//		Dimension screenSize= Toolkit.getDefaultToolkit().getScreenSize();
//		frame.setLocation(screenSize.width/3 - w/2, screenSize.height/2 - h/2);
//        frame.show();
//    }

}
