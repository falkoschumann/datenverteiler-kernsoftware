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

import de.bsvrz.dav.daf.communication.dataRepresentation.AbstractData;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.ObjectLookup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.kex.kexdav.management.ManagerInterface;

/**
 * Kapselt analog zu {@link KExDaVAttributeGroupData} ein inneres Datenarray, das den Zugriff auf Systemobjekte einschränkt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
class KExDaVWrappedDataArray extends AbstractData.Array{

	private final Data.Array _array;

	private DataModel _dataModel;

	private ManagerInterface _manager;

	KExDaVWrappedDataArray(final Data.Array array, final DataModel dataModel, final ManagerInterface manager) {
		_array = array;
		_dataModel = dataModel;
		_manager = manager;
	}

	public boolean isCountLimited() {
		return _array.isCountLimited();
	}

	public boolean isCountVariable() {
		return _array.isCountVariable();
	}

	public int getMaxCount() {
		return _array.getMaxCount();
	}

	public int getLength() {
		return _array.getLength();
	}

	public void setLength(final int newLength) {
		_array.setLength(newLength);
	}

	public Data getItem(final int itemIndex) {
		return new KExDaVWrappedData(_array.getItem(itemIndex), _dataModel, _manager);
	}

	@Override
	public Data.NumberArray asUnscaledArray() {
		return _array.asUnscaledArray();
	}

	@Override
	public Data.TimeArray asTimeArray() {
		return _array.asTimeArray();
	}

	@Override
	public Data.TextArray asTextArray() {
		return _array.asTextArray();
	}

	@Override
	public Data.NumberArray asScaledArray() {
		return _array.asScaledArray();
	}

	@Override
	public Data.ReferenceArray asReferenceArray() {
		return new RefArray(_array.asReferenceArray(), _manager);
	}

	@Override
	public String toString() {
		return _array.toString();
	}

	private class RefArray implements Data.ReferenceArray	{

		private final Data.ReferenceArray _referenceArray;

		private ManagerInterface _manager;

		public RefArray(final Data.ReferenceArray referenceArray, final ManagerInterface manager) {
			_referenceArray = referenceArray;
			_manager = manager;
		}

		public int getLength() {
			return _referenceArray.getLength();
		}

		public void setLength(final int newLength) {
			_referenceArray.setLength(newLength);
		}

		public Data.ReferenceValue getReferenceValue(final int itemIndex) {
			return new KExDaVWrappedReferenceValue(_referenceArray.getReferenceValue(itemIndex), _dataModel, _manager);
		}

		public Data.ReferenceValue[] getReferenceValues() {
			final Data.ReferenceValue[] referenceValues = new Data.ReferenceValue[getLength()];
			for(int i = 0; i < referenceValues.length; i++) {
				referenceValues[i] = getReferenceValue(i);
			}
			return referenceValues;
		}

		public void set(final SystemObject... systemObjects) {
			setLength(systemObjects.length);
			for(int i = 0; i < systemObjects.length;i++){
				getReferenceValue(i).setSystemObject(systemObjects[i]);
			}
		}

		public void set(final String... systemObjectPids) {
			setLength(systemObjectPids.length);
			for(int i = 0; i < systemObjectPids.length;i++){
				getReferenceValue(i).setSystemObjectPid(systemObjectPids[i]);
			}
		}

		public void set(final ObjectLookup dataModel, final String... systemObjectPids) {
			set(systemObjectPids);
		}

		public SystemObject getSystemObject(final int itemIndex) {
			return _referenceArray.getSystemObject(itemIndex);
		}

		public SystemObject[] getSystemObjectArray() {
			return _referenceArray.getSystemObjectArray();
		}
	}
}
