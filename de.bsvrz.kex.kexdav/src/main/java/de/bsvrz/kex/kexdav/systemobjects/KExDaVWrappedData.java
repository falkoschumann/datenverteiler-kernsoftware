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
import de.bsvrz.dav.daf.main.config.AttributeType;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.kex.kexdav.management.ManagerInterface;

import java.util.Iterator;

/**
 * Kapselt analog zu {@link KExDaVAttributeGroupData} ein inneres Datenobjekt, das den Zugriff auf Systemobjekte einschränkt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
class KExDaVWrappedData extends AbstractData {

	static final String NO_COPY_MESSAGE = "KExDav-Daten können nicht direkt versendet oder kopiert werden.";

	private final Data _data;

	private DataModel _dataModel;

	private ManagerInterface _manager;

	/**
	 * Erstellt ein neues KExDavWrappedData
	 * @param data Inneres Datenobjekt
	 * @param dataModel
	 * @param manager
	 */
	KExDaVWrappedData(final Data data, final DataModel dataModel, final ManagerInterface manager) {
		_data = data;
		_dataModel = dataModel;
		_manager = manager;
	}

	public String getName() {
		return _data.getName();
	}

	public String valueToString() {
		return _data.valueToString();
	}

	public AttributeType getAttributeType() {
		return _data.getAttributeType();
	}

	public boolean isDefined() {
		return _data.isDefined();
	}

	public void setToDefault() {
		_data.setToDefault();
	}

	public boolean isList() {
		return _data.isList();
	}

	public boolean isArray() {
		return _data.isArray();
	}

	public boolean isPlain() {
		return _data.isPlain();
	}

	public Data.TextValue asTextValue() {
		return _data.asTextValue();
	}

	public Iterator<Data> iterator() {
		return new KExDaVAttributeGroupData.Iter(_data, _dataModel, _manager);
	}

	@Override
	public Data.NumberValue asUnscaledValue() {
		return _data.asUnscaledValue();
	}

	@Override
	public Data.TimeValue asTimeValue() {
		return _data.asTimeValue();
	}

	@Override
	public Data.NumberValue asScaledValue() {
		return _data.asScaledValue();
	}

	@Override
	public Data.ReferenceValue asReferenceValue() {
		return new KExDaVWrappedReferenceValue(_data.asReferenceValue(), _dataModel, _manager);
	}

	@Override
	public Data.Array asArray() {
		return new KExDaVWrappedDataArray(_data.asArray(), _dataModel, _manager);
	}

	@Override
	public String toString() {
		return _data.toString();
	}
}
