/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.main.config.SystemObject;

import javax.swing.*;
import java.util.*;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 */
public class DynamicListModel extends AbstractListModel{

	final TreeSet<SystemObject> _systemObjects = new TreeSet<SystemObject>();

	public int getSize() {
		return _systemObjects.size();
	}

	public Object getElementAt(final int index) {
		return getObject(index).getNameOrPidOrId();
	}

	public void setElements(final Collection<SystemObject> newElements){
		final HashSet<SystemObject> addedElements = new HashSet<SystemObject>(newElements);
		final HashSet<SystemObject> removedElements = new HashSet<SystemObject>(_systemObjects);
		addedElements.removeAll(_systemObjects);
		removedElements.removeAll(newElements);
		for(final SystemObject element : addedElements) {
			if(_systemObjects.add(element)){
				notifyElementAdded(element);
			}
		}
		for(final SystemObject element : removedElements) {
			notifyElementRemoved(element);
			_systemObjects.remove(element);
		}
	}

	private void notifyElementAdded(final SystemObject element) {
		final int index = getElementIndex(element);
		fireIntervalAdded(this, index, index);
	}

	private void notifyElementRemoved(final SystemObject element) {
		final int index = getElementIndex(element);
		fireIntervalRemoved(this, index, index);
	}

	private int getElementIndex(final SystemObject element) {
		int i = 0;
		for(final SystemObject systemObject : _systemObjects) {
			if(element.equals(systemObject)) return i;
			i++;
		}
		return -1;
	}

	public int[] getElementIndizes(final Collection<SystemObject> selectedSystemObjects) {
		final List<Integer> list = new ArrayList<Integer>();
		int i = 0;

		for(final SystemObject systemObject : _systemObjects) {
			if(selectedSystemObjects.contains(systemObject)){
				list.add(i);
			}
			i++;
		}
		int[] result = new int[list.size()];
		for(int index = 0, listSize = list.size(); index < listSize; index++) {
			result[index] = list.get(index);
		}
		return result;
	}

	public SystemObject getObject(final int index) {
		int i = 0;
		for(final SystemObject systemObject : _systemObjects) {
			if(i == index) return systemObject;
			i++;
		}
		return null;
	}
}
