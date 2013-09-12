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
import de.bsvrz.dav.daf.communication.dataRepresentation.AttributeBaseValueDataFactory;
import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.dav.daf.main.impl.config.DafDataModel;
import de.bsvrz.kex.kexdav.management.ManagerInterface;

import java.util.Iterator;

/**
 * Kapselt ein Datenobjekt für dem sicheren Umgang zwischen Datenverteilern. Dazu wird zusätzlich zu dem Data-Objekt das Datenmodell gespeichert, zu dem es
 * gehört, und es werden sämtliche Zugriffe auf Referenzwerte so korrigiert, dass die Zugriffe nur anhand der Pid stattfinden und nur Objekte aus dem korrekten
 * Datenmodell eingefügt werden können.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9232 $
 */
public class KExDaVAttributeGroupData extends AbstractData.ListData{

	private final Data _data;

	private final DataModel _dataModel;

	private final AttributeGroup _attributeGroup;

	private ManagerInterface _manager;

	/**
	 * Erstellt ein neues KExDavAttributeGroupData-Objekt anhand eines normalen Datenobjektes vom Datenverteiler
	 * @param data Daten-Objekt
	 * @param manager
	 */
	public KExDaVAttributeGroupData(Data data, final ManagerInterface manager) {
		_data = data;
		_manager = manager;
		if(!(data instanceof AttributeBaseValueDataFactory.AttributeGroupAdapter)) {
			data = data.createModifiableCopy();
		}

		_attributeGroup = ((AttributeBaseValueDataFactory.AttributeGroupAdapter)data)._attributeGroup;
		_dataModel = _attributeGroup.getDataModel();

	}

	/**
	 * Erstellt ein neues Datenobjekt für eine Attributgruppe
	 *
	 * @param connection Datenverteilerverbindung
	 * @param atg Attributgruppen-Pid
	 * @param manager
	 * @throws MissingObjectException Falls die Attributgruppe nicht gefunden werden konnte
	 */
	public KExDaVAttributeGroupData(final ClientDavInterface connection, final String atg, final ManagerInterface manager) throws MissingObjectException {
		_manager = manager;
		_dataModel = connection.getDataModel();
		_attributeGroup = KExDaVObject.tryGetAttributeGroup(connection, atg);
		_data = connection.createData(_attributeGroup);
	}

	/**
	 * Wandelt dieses Objekt in ein ResultData um
	 * @param wrappedObject Objekt
	 * @param dataDescription DataDescription
	 * @param dataTime Zeit
	 * @return ResultData
	 */
	public ResultData toResultData(final SystemObject wrappedObject, final DataDescription dataDescription, final long dataTime) {
		if(!dataDescription.getAttributeGroup().equals(_attributeGroup)){
			throw new IllegalStateException("Die AttributGruppe der DataDescription passt nicht zum Data-Objekt.");
		}
		return new ResultData(wrappedObject,  dataDescription,  dataTime, toData(wrappedObject.getDataModel()));
	}

	/**
	 * Gibt ein Data-Objekt zurück, das über den Datenverteiler verschickt werden kann
	 * @param dataModel Datenmodell des Datenverteilers, über den das Data-Objekt verschickt werden soll
	 * @return Data
	 */
	public Data toData(final DataModel dataModel) {
		if(!dataModel.equals(_dataModel)){
			throw new IllegalStateException("Das Datenmodell dieses Data-Objektes passt nicht zu dem Datenmodell des Objektes, zu dem die Daten gespeichert werden sollen oder das Data-Objekt ist nicht vollständig.");
		}
		return _data;
	}

	/**
	 * Gibt die Datenverteiler-Verbindung zurück, zu der das Data-Objekt gehört
	 * @return Datenverteiler-Verbindung
	 */
	public ClientDavInterface getConnection() {
		return ((DafDataModel)_dataModel).getConnection();
	}

	/**
	 * Gibt die Attributgruppe zurück, zu der das Data-Objekt gehört
	 * @return Attributgruppe
	 */
	public AttributeGroup getAttributeGroup() {
		return _attributeGroup;
	}

	public String getName() {
		return _data.getName();
	}

	public AttributeType getAttributeType() {
		return _data.getAttributeType();
	}

	public Iterator<Data> iterator() {
		return new Iter(_data, _dataModel, _manager);
	}

	@Override
	public Data createModifiableCopy() {
		throw new UnsupportedOperationException(KExDaVWrappedData.NO_COPY_MESSAGE);
	}

	@Override
	public Data createUnmodifiableCopy() {
		throw new UnsupportedOperationException(KExDaVWrappedData.NO_COPY_MESSAGE);
	}

	@Override
	public String toString() {
		return _data.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final KExDaVAttributeGroupData other = (KExDaVAttributeGroupData)o;

		if(!_attributeGroup.equals(other._attributeGroup)) return false;
		if(!_data.toString().equals(other._data.toString())) return false;
		if(!_dataModel.equals(other._dataModel)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _data.toString().hashCode();
		result = 31 * result + _dataModel.hashCode();
		result = 31 * result + _attributeGroup.hashCode();
		return result;
	}

	static class Iter implements Iterator<Data> {

		private final Iterator<Data> _iterator;

		private final Data _data;

		private DataModel _dataModel;

		private ManagerInterface _manager;

		public Iter(final Data data, final DataModel dataModel, final ManagerInterface manager){
			_data = data;
			_dataModel = dataModel;
			_manager = manager;
			_iterator = _data.iterator();
		}

		public boolean hasNext() {
			return _iterator.hasNext();
		}

		public Data next() {
			return new KExDaVWrappedData(_iterator.next(), _dataModel, _manager);
		}

		public void remove() {
			_iterator.remove();
		}
	}
}
