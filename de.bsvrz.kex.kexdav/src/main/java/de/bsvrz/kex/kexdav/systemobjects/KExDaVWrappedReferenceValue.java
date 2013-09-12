/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.systemobjects;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.ObjectLookup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.management.ManagerInterface;

/**
 * Kapselt analog zu {@link KExDaVAttributeGroupData} ein Referenzwert, der den Zugriff auf Systemobjekte einschränkt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
class KExDaVWrappedReferenceValue implements Data.ReferenceValue {

	private final Data.ReferenceValue _referenceValue;

	private DataModel _dataModel;

	private ManagerInterface _manager;

	KExDaVWrappedReferenceValue(final Data.ReferenceValue referenceValue, final DataModel dataModel, final ManagerInterface manager) {
		_referenceValue = referenceValue;
		_dataModel = dataModel;
		_manager = manager;
	}

	public long getId() {
		return _referenceValue.getId();
	}

	public SystemObject getSystemObject() {
		return _referenceValue.getSystemObject();
	}

	public void setSystemObject(final SystemObject object) {
		if(object == null){
			_referenceValue.setSystemObject(null);
		}
		else{
			final ObjectSpecification objectSpecification = ObjectSpecification.create(object, _manager);
			_referenceValue.setSystemObject(objectSpecification.getObject(_dataModel));
		}
	}

	public void setSystemObjectPid(final String objectPid, final ObjectLookup datamodel) {
		setSystemObjectPid(objectPid);
	}

	public void setSystemObjectPid(final String objectPid) {
		// Das Setzen von unbekannten Pids führt hier zu keiner Exception, sondern nur dazu, dass ein Undefiniert-Verweis eingefügt wird
		try {
			_referenceValue.setSystemObjectPid(objectPid);
		}
		catch(IllegalArgumentException ignored) {
			_referenceValue.setSystemObject(null);
		}
	}

	public String getSystemObjectPid() {
		return _referenceValue.getSystemObjectPid();
	}

	public String getText() {
		return _referenceValue.getText();
	}

	public String getValueText() {
		return _referenceValue.getValueText();
	}

	public String getSuffixText() {
		return _referenceValue.getSuffixText();
	}

	public void setText(String text) {
			int startIndex;
			boolean tryPid = true;
			boolean tryId = true;
			final String lowercaseText = text.toLowerCase();
			startIndex = lowercaseText.lastIndexOf("pid:");
			if(startIndex >= 0) {
				startIndex += 4;
				tryId = false;
			}
			else {
				startIndex = lowercaseText.lastIndexOf("id:");
				if(startIndex >= 0) {
					startIndex += 3;
					tryPid = false;
				}
				else {
					startIndex = 0;
				}
			}
			text = text.substring(startIndex).trim();
			if(tryId) {
				final String numberText = text.split("\\D", 2)[0];
				if(numberText.length() > 0) {
					final long id = Long.parseLong(numberText);
					if(id == 0) {
						setSystemObject(null);
						return;
					}
					SystemObject object;
					try {
						object = _dataModel.getObject(id);
					}
					catch(Exception e) {
						object = null;
					}
					if(object != null) {
						setSystemObject(object);
						return;
					}
				}
			}
			if(tryPid) {
				final String pid = text.split("[\\s\\Q[]{}():\\E]", 2)[0];
				if(pid.equals("null") || pid.equals("undefiniert")) {
					setSystemObject(null);
					return;
				}
				SystemObject object;
				try {
					object = _dataModel.getObject(pid);
				}
				catch(Exception e) {
					object = null;
				}
				if(object != null) {
					setSystemObject(object);
					return;
				}
			}
			throw new IllegalArgumentException("Der Text '" + text + "' kann nicht als Objektreferenz interpretiert werden.");
	}

	@Override
	public String toString() {
		return _referenceValue.toString();
	}
}
