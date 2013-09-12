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
package de.kappich.pat.gnd.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Enthält nur eine Methode, um die Komponenten eines Containers mit Hilfe des SpringLayouts anzuordnen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 8058 $
 */
public class SpringUtilities {

	/**
	 * Ermittelt die Constraints für eine bestimmte Komponente in einem Container.
	 *
	 * @param row       Zeile der Komponente
	 * @param column    Spalte der Komponente
	 * @param container Zu betrachtender Container
	 * @param columns   Anzahl Spalten
	 * @return Constraints der spezifizierten Komponente
	 */
	private static SpringLayout.Constraints getConstraints(int row, int column, Container container, int columns) {
		SpringLayout layout = (SpringLayout)container.getLayout();
		Component c = container.getComponent(row * columns + column);
		return layout.getConstraints(c);
	}

	/**
	 * Ordnet die Komponenten in einem Container in einem Grid so an, dass die Spaltenbreiten der maximalen bevorzugten
	 * Breite der enthaltenen Komponenten entspricht und die Zeilenhöhen der maximalen bevorzugten Höhe der jeweils
	 * enthalten Komponenten entspricht. Die Containergröße wird so eingestellt, dass alle Komponenten reinpassen.
	 *
	 * @param columns   Anzahl Spalten
	 * @param columnGap Gewünschter Abstand zwischen den Spalten
	 * @param rowGap    Gewünschter Abstand zwischen den Zeilen
	 * @throws ClassCastException    wenn der Container kein SpringLayout verwendet.
	 * @throws IllegalStateException wenn die Anzahl der im Container enthaltenen Komponenten nicht ohne Rest durch die
	 *                               Anzahl Spalten teilbar ist.
	 */
	public static void makeCompactGrid(Container container,
	                                   int columns,
	                                   int columnGap, int rowGap) {
		SpringLayout layout = (SpringLayout)container.getLayout();
		int numberOfComponents = container.getComponentCount();
		if(numberOfComponents % columns != 0) {
			throw new IllegalStateException(
					"Anzahl der Komponenten " + numberOfComponents +
					" ist nicht durch die Anzahl Spalten " + columns + " teilbar"
			);
		}
		int rows = numberOfComponents / columns;
		Spring x = Spring.constant(0);
		Spring y = Spring.constant(0);
		for(int column = 0; column < columns; ++column) {
			Spring width = Spring.constant(0);
			for(int row = 0; row < rows; ++row) {
				width = Spring.max(width, getConstraints(row, column, container, columns).getWidth());
			}
			for(int row = 0; row < rows; ++row) {
				SpringLayout.Constraints constraints =
						getConstraints(row, column, container, columns);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(columnGap)));
		}
		for(int row = 0; row < rows; ++row) {
			Spring height = Spring.constant(0);
			for(int column = 0; column < columns; ++column) {
				height = Spring.max(height, getConstraints(row, column, container, columns).getHeight());
			}
			for(int column = 0; column < columns; ++column) {
				SpringLayout.Constraints constraints =
						getConstraints(row, column, container, columns);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(rowGap)));
		}
		SpringLayout.Constraints containerConstraints = layout.getConstraints(container);
		containerConstraints.setConstraint(SpringLayout.SOUTH, y);
		containerConstraints.setConstraint(SpringLayout.EAST, x);
	}
}

