/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.Attribute;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.AttributeListDefinition;
import de.bsvrz.dav.daf.main.config.AttributeSet;
import de.bsvrz.dav.daf.main.config.AttributeType;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationAuthority;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DavApplication;
import de.bsvrz.dav.daf.main.config.DoubleAttributeType;
import de.bsvrz.dav.daf.main.config.DynamicObject;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.IntegerAttributeType;
import de.bsvrz.dav.daf.main.config.IntegerValueRange;
import de.bsvrz.dav.daf.main.config.IntegerValueState;
import de.bsvrz.dav.daf.main.config.MutableSet;
import de.bsvrz.dav.daf.main.config.ObjectSet;
import de.bsvrz.dav.daf.main.config.ObjectSetType;
import de.bsvrz.dav.daf.main.config.ObjectSetUse;
import de.bsvrz.dav.daf.main.config.ReferenceAttributeType;
import de.bsvrz.dav.daf.main.config.StringAttributeType;
import de.bsvrz.dav.daf.main.config.SystemConfigurationAuthority;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectCollection;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.dav.daf.main.config.TimeAttributeType;

import java.util.*;

/**
 * Modell für ein JTree-Objekt zur Darstellung der Konfiguration.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5762 $
 */
public class DataModelTreeModel implements javax.swing.tree.TreeModel {

	private DataModel _dm = null;

	/** Creates new DataModelTreeModel */
	public DataModelTreeModel(DataModel dm) {
		_dm = dm;
	}

	public java.lang.Object getRoot() {
		return new DataModelAdapter(_dm);
	}

	public Object getChild(Object parent, int index) {
		Object o = ((Adapter)parent).getChild(index);
		if(o == null) return new ConstantAdapter("BAD TREE ITEM", o);
		return o;
	}

	public int getChildCount(Object parent) {
		return ((Adapter)parent).getChildCount();
	}

	public int getIndexOfChild(Object parent, Object child) {
		return ((Adapter)parent).getIndex((Adapter)child);
	}

	public boolean isLeaf(Object node) {
		return ((Adapter)node).isLeaf();
	}

	public void removeTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
	}

	public void addTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
	}

	public void valueForPathChanged(javax.swing.tree.TreePath treePath, java.lang.Object obj) {
	}

	static interface Adapter {

		public int getIndex(Adapter child);

		public Adapter getChild(int searchIndex);

		public int getChildCount();

		public boolean isLeaf();
	}

	static abstract class AbstractAdapter implements Adapter {

		String _tag = null;

		Adapter[] _childs = null;

		public abstract int getChildCount();

		static Adapter createAdapter(String nodeTag, Object object) {
			if(object instanceof ObjectSetType) return new ObjectSetTypeAdapter(nodeTag, (ObjectSetType)object);
			if(object instanceof DynamicObjectType) return new DynamicObjectTypeAdapter(nodeTag, (DynamicObjectType)object);
			if(object instanceof SystemObjectType) return new SystemObjectTypeAdapter(nodeTag, (SystemObjectType)object);
			if(object instanceof ObjectSet) return new ObjectSetAdapter(nodeTag, (ObjectSet)object);
			if(object instanceof ObjectSetUse) return new ObjectSetUseAdapter(nodeTag, (ObjectSetUse)object);
			if(object instanceof Aspect) return new AspectAdapter(nodeTag, (Aspect)object);
			if(object instanceof AttributeGroup) return new AttributeGroupAdapter(nodeTag, (AttributeGroup)object);
			if(object instanceof AttributeGroupUsage) {
				return new AttributeGroupUsageAdapter(nodeTag, (AttributeGroupUsage)object);
			}
			if(object instanceof AttributeListDefinition) {
				return new AttributeListDefinitionAdapter(nodeTag, (AttributeListDefinition)object);
			}
			if(object instanceof Attribute) return new AttributeAdapter(nodeTag, (Attribute)object);
			if(object instanceof IntegerAttributeType) {
				return new IntegerAttributeTypeAdapter(nodeTag, (IntegerAttributeType)object);
			}
			if(object instanceof StringAttributeType) {
				return new StringAttributeTypeAdapter(nodeTag, (StringAttributeType)object);
			}
			if(object instanceof TimeAttributeType) return new TimeAttributeTypeAdapter(nodeTag, (TimeAttributeType)object);
			if(object instanceof ReferenceAttributeType) {
				return new ReferenceAttributeTypeAdapter(nodeTag, (ReferenceAttributeType)object);
			}
			if(object instanceof DoubleAttributeType) {
				return new DoubleAttributeTypeAdapter(nodeTag, (DoubleAttributeType)object);
			}
			if(object instanceof IntegerValueState) return new IntegerValueStateAdapter(nodeTag, (IntegerValueState)object);
			if(object instanceof IntegerValueRange) return new IntegerValueRangeAdapter(nodeTag, (IntegerValueRange)object);
			if(object instanceof ConfigurationAuthority) {
				return new ConfigurationAuthorityAdapter(nodeTag, (ConfigurationAuthority)object);
			}
			if(object instanceof ConfigurationArea) return new ConfigurationAreaAdapter(nodeTag, (ConfigurationArea)object);
			if(object instanceof DavApplication) return new DavApplicationAdapter(nodeTag, (DavApplication)object);
			if(object instanceof ConfigurationObject) {
				return new ConfigurationObjectAdapter(nodeTag, (ConfigurationObject)object);
			}
			if(object instanceof DynamicObject) return new DynamicObjectAdapter(nodeTag, (DynamicObject)object);
			if(object instanceof SystemObject) return new SystemObjectAdapter(nodeTag, (SystemObject)object);
			if(object instanceof Class) return new ClassAdapter(nodeTag, (Class)object);
			if(object instanceof Object[]) return new ListAdapter(nodeTag, (Object[])object);
			if(object instanceof List) return new ListAdapter(nodeTag, (List)object);
			if(object instanceof Collection) return new ListAdapter(nodeTag, new ArrayList((Collection)object));
			return new ConstantAdapter(nodeTag, object);
		}

		public AbstractAdapter(String nodeTag) {
			_tag = nodeTag;
			//System.out.println("AbstractAdapter("+nodeTag+")");
		}

		private AbstractAdapter() {
			throw new UnsupportedOperationException();
		}

		public final int getIndex(Adapter child) {
			//System.err.println("Looking for index of " + child);
			if(_childs != null) {
				int childCount = getChildCount();
				for(int i = 0; i < childCount; i++) {
					if(_childs[i] == child) return i;
				}
			}
			return -1;
		}

		public final Adapter getChild(int searchIndex) {
			//System.err.println("Looking for index " + searchIndex);
			int childCount = getChildCount();
			if(searchIndex < 0 || searchIndex >= childCount) return null;
			if(_childs == null) _childs = new Adapter[childCount];
			if(_childs[searchIndex] == null) {
				_childs[searchIndex] = createChild(searchIndex);
				//System.err.println("Creating index " + searchIndex);
			}
			return _childs[searchIndex];
		}

		public Adapter createChild(int searchIndex) {
			throw new UnsupportedOperationException("createChild() not implemented in derived adapter class");
		}

		public boolean isLeaf() {
			return getChildCount() == 0;
		}

		public String getTag() throws Exception {
			return _tag;
		}

		public abstract String getValue() throws Exception;

		public String toString() {
			String tag;
			try {
				tag = getTag();
			}
			catch(Exception e) {
				tag = "[" + e.toString() + "]";
			}
			if(tag == null) tag = "";
			if(!tag.equals("")) tag += ": ";
			String value;
			try {
				value = getValue();
			}
			catch(Exception e) {
				value = e.toString();
			}
			if(value == null) value = "[null]";
			return "<html><b>" + tag + "</b>" + value + "</html>";
		}
	}

	static class DataModelAdapter extends AbstractAdapter implements Adapter {

		private DataModel _dm;

		private static final String[] LABELS = {"getTypeTypeObject()", "getBaseTypes()", "type hierarchy", "Netze",};

		DataModelAdapter(DataModel dm) {
			super("getDataModel()");
			_dm = dm;
		}

		public Adapter createChild(int searchIndex) {
			try {
				switch(searchIndex) {
					case 0:
						return AbstractAdapter.createAdapter(LABELS[searchIndex], _dm.getTypeTypeObject());
					case 1:
						return new TypeHierarchyListAdapter(LABELS[searchIndex], _dm.getBaseTypes());
					case 2:
						return new SimpleTypeHierarchyListAdapter(LABELS[searchIndex], _dm.getBaseTypes(), true);
					case 3:
						return new ObjectHierarchyListAdapter(_dm.getType("typ.netz"));
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[searchIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return LABELS.length;
		}

		public String getValue() {
			return _dm.toString();
		}
	}

	static class SystemObjectAdapter extends AbstractAdapter implements Adapter {

		private SystemObject _object;

		private List _configurationDataList = null;

		SystemObjectAdapter(String nodeTag, SystemObject object) {
			super(nodeTag);
			_object = object;
		}

		public SystemObject getObject() {
			return _object;
		}

		private static final String[] LABELS = {"getClass()", "getNameOrPidOrId()", "toString()", "getId()", "getPid()", "getName()", "isValid()", "getType()",
		                                        "getConfigurationData(...)", "getInfo()", "getConfigurationArea()",};

		public Adapter createChild(int searchIndex) {
			//System.out.println(_object.name() + "SystemObjectAdapter.createChild(" + searchIndex + ")");
			try {
				switch(searchIndex) {
					case 0:
						return createAdapter(LABELS[searchIndex], _object.getClass());
					case 1:
						return new ConstantAdapter(LABELS[searchIndex], _object.getNameOrPidOrId());
					case 2:
						return new ConstantAdapter(LABELS[searchIndex], _object.toString());
					case 3:
						return new ConstantAdapter(LABELS[searchIndex], _object.getId());
					case 4:
						return new ConstantAdapter(LABELS[searchIndex], _object.getPid());
					case 5:
						return new ConstantAdapter(LABELS[searchIndex], _object.getName());
					case 6:
						return new ConstantAdapter(LABELS[searchIndex], _object.isValid());
					case 7:
						return createAdapter(LABELS[searchIndex], _object.getType());
					case 8:
						return new ListAdapter(LABELS[searchIndex], getConfigurationDataList());
					case 9:
						return new SystemObjectInfoAdapter(LABELS[searchIndex], _object);
					case 10:
						return createAdapter(LABELS[searchIndex], _object.getConfigurationArea());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[searchIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			//System.out.println(_object.name() + "SystemObjectAdapter.getChildCount");
			return LABELS.length;
		}

		public String getValue() {
			return _object.getNameOrPidOrId();
		}

		public boolean isLeaf() {
			return false;
		}

		private List getConfigurationDataList() throws Exception {
			if(_configurationDataList == null) {
				_configurationDataList = new LinkedList();
				Iterator i = _object.getType().getAttributeGroups().iterator();
				while(i.hasNext()) {
					AttributeGroup atg = (AttributeGroup)i.next();
					if(atg.isConfigurating()) {
						try {
							Data data = _object.getConfigurationData(atg);
							if(data != null) _configurationDataList.add(data);
						}
						catch(Exception e) {
							e.printStackTrace();
							_configurationDataList.add(atg.getNameOrPidOrId() + ": " + e.getMessage());
						}
					}
				}
			}
			return _configurationDataList;
		}
	}

	static class SystemObjectInfoAdapter extends AbstractAdapter implements Adapter {

		private SystemObject _object;

		SystemObjectInfoAdapter(String nodeTag, SystemObject object) {
			super(nodeTag);
			_object = object;
		}

		public SystemObject getObject() {
			return _object;
		}

		private static final String[] LABELS = {"getShortInfo()", "getDescription()",};

		public Adapter createChild(int searchIndex) {
			//System.out.println(_object.name() + "SystemObjectAdapter.createChild(" + searchIndex + ")");
			try {
				switch(searchIndex) {
					case 0:
						return new ConstantAdapter(LABELS[searchIndex], _object.getInfo().getShortInfo());
					case 1:
						return new ConstantAdapter(LABELS[searchIndex], _object.getInfo().getDescription());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[searchIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			//System.out.println(_object.name() + "SystemObjectAdapter.getChildCount");
			return LABELS.length;
		}

		public String getValue() {
			return "";
		}

		public boolean isLeaf() {
			return false;
		}
	}

	static class ClassAdapter extends AbstractAdapter implements Adapter {

		private Class _class;

		ClassAdapter(String nodeTag, Class aClass) {
			super(nodeTag);
			_class = aClass;
		}

		private List getAllInterfaces() {
			return new ArrayList(getAllInterfaces(_class.getInterfaces()));
		}

		private Set getAllInterfaces(Class[] interfaces) {
			Set result = new LinkedHashSet();
			for(int i = 0; i < interfaces.length; ++i) {
				result.add(interfaces[i]);
				result.addAll(getAllInterfaces(interfaces[i].getInterfaces()));
			}
			return result;
		}

		private static final String[] LABELS = {"getInterfaces()", "getSuperclass()",};

		public Adapter createChild(int searchIndex) {
			try {
				switch(searchIndex) {
					//case 0: return new InterfaceListAdapter(LABELS[searchIndex], _class.getInterfaces(), true);
					case 0:
						return new ListAdapter(LABELS[searchIndex], getAllInterfaces());
					case 1:
						return createAdapter(LABELS[searchIndex], _class.getSuperclass());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[searchIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return LABELS.length;
		}

		public String getValue() {
			return _class.getName();
		}

		public boolean isLeaf() {
			return _class.isInterface();
		}
	}

	static class TypeHierarchyNodeAdapter extends SystemObjectAdapter implements Adapter {

		TypeHierarchyNodeAdapter(String nodeTag, SystemObjectType object) {
			super(nodeTag, object);
		}

		SystemObjectType getType() {
			return (SystemObjectType)getObject();
		}

		private static final String[] LABELS = {"type info", "getSuperTypes()", "getSubTypes()"};

		public Adapter createChild(int searchIndex) {
			try {
				switch(searchIndex) {
					case 0:
						return createAdapter(LABELS[searchIndex], getType());
					case 1:
						return new TypeHierarchyListAdapter(LABELS[searchIndex], getType().getSuperTypes());
					case 2:
						return new TypeHierarchyListAdapter(LABELS[searchIndex], getType().getSubTypes());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[searchIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return LABELS.length;
		}
	}

	static class TypeHierarchyListAdapter extends SortingListAdapter implements Adapter {

		TypeHierarchyListAdapter(String nodeTag, List types) {
			super(nodeTag, types);
		}

		public Adapter createChild(int searchIndex) {
			try {
				return new TypeHierarchyNodeAdapter(null, (SystemObjectType)_list.get(searchIndex));
			}
			catch(Exception e) {
				return new ConstantAdapter(null, e);
			}
		}
	}

//	static class InterfaceHierarchyListAdapter extends ListAdapter implements Adapter {
//		private final boolean _root;
//
//		InterfaceHierarchyListAdapter(String nodeTag, Class[] interfaces, boolean root) {
//			super(nodeTag, interfaces);
//			_root= root;
//		}
//
//		public Adapter createChild(int searchIndex) {
//			try {
//				return new InterfaceHierarchyListAdapter(((Class)_list.get(searchIndex)).getName(), ((Class)_list.get(searchIndex)).getInterfaces(), false);
//			}
//			catch(Exception e) {
//				return new ConstantAdapter(null,e);
//			}
//		}
//
//		public String getValue() {
//			return getChildCount() + (_root ? "" : " sub") + " interface" + (getChildCount()==1 ? "" : "s");
//		}
//
//		public boolean isLeaf() {
//			return getChildCount() == 0;
//		}
//	}

	static class SimpleTypeHierarchyListAdapter extends SortingListAdapter implements Adapter {

		private boolean _root;

		SimpleTypeHierarchyListAdapter(String nodeTag, List types, boolean root) {
			super(nodeTag, types);
			_root = root;
		}

		public Adapter createChild(int searchIndex) {
			String typeName = null;
			try {
				SystemObjectType type = (SystemObjectType)_list.get(searchIndex);
				typeName = type.getNameOrPidOrId();
				return new SimpleTypeHierarchyListAdapter(typeName, type.getSubTypes(), false);
			}
			catch(Exception e) {
				return new ConstantAdapter(typeName, e);
			}
		}

		public String getValue() {
			return getChildCount() + (_root ? " base types" : " sub types");
		}

		public boolean isLeaf() {
			return getChildCount() == 0;
		}
	}

	static class ObjectHierarchyListAdapter extends SortingListAdapter implements Adapter {

		static List getItemList(SystemObject object) throws Exception {
			if(object instanceof SystemObjectCollection) return ((SystemObjectCollection)object).getElements();
			if(object instanceof ConfigurationObject) {
				List sets = ((ConfigurationObject)object).getObjectSets();
				if(sets == null) return new LinkedList();
				return sets;
			}
			return new LinkedList();
		}

		SystemObject _object;

		ObjectHierarchyListAdapter(SystemObject object) throws Exception {
			super(object.getNameOrPidOrId(), getItemList(object));
			_object = object;
		}

		public Adapter createChild(int searchIndex) {
			try {
				SystemObject element = (SystemObject)_list.get(searchIndex);
				return new ObjectHierarchyListAdapter(element);
			}
			catch(Exception e) {
				return new ConstantAdapter("???", e);
			}
		}

		public String getValue() {
			int count = getChildCount();
			if(_object instanceof SystemObjectType) {
				return "Von diesem Typ gibt es " + count + " Objekt" + (count != 1 ? "e" : "");
			}
			if(_object instanceof ObjectSet) return "In dieser Menge gibt es " + count + " Element" + (count != 1 ? "e" : "");
			if(_object instanceof ConfigurationObject) return "Dieses Objekt hat " + count + " Menge" + (count != 1 ? "n" : "");
			return "";
		}

		public boolean isLeaf() {
			return getChildCount() == 0;
		}
	}

	static class ConfigurationObjectAdapter extends SystemObjectAdapter implements Adapter {

		ConfigurationObjectAdapter(String nodeTag, SystemObject object) {
			super(nodeTag, object);
		}


		private static final String[] LABELS = {"getValidSince()", "getNotValidSince()", "getObjectSets()"};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ConfigurationObject object = (ConfigurationObject)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], object.getValidSince());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], object.getNotValidSince());
					case 2:
						return createAdapter(LABELS[localIndex], object.getObjectSets());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class DynamicObjectAdapter extends SystemObjectAdapter implements Adapter {

		DynamicObjectAdapter(String nodeTag, SystemObject object) {
			super(nodeTag, object);
		}


		private static final String[] LABELS = {"getValidSince()", "getNotValidSince()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			DynamicObject object = (DynamicObject)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], object.getValidSince());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], object.getNotValidSince());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class SystemObjectTypeAdapter extends ConfigurationObjectAdapter implements Adapter {

		SystemObjectTypeAdapter(String nodeTag, SystemObject object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"isBaseType()", "isConfigurating()", "isNameOfObjectsPermanent()", "getSuperTypes()", "getSubTypes()",
		                                        "getDirectAttributeGroups()", "getAttributeGroups()", "getDirectObjectSetUses()", "getObjectSetUses()",
		                                        "getObjects()", "getElements()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			SystemObjectType type = (SystemObjectType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], type.isBaseType());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], type.isConfigurating());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], type.isNameOfObjectsPermanent());
					case 3:
						return new SortingListAdapter(LABELS[localIndex], type.getSuperTypes());
					case 4:
						return new SortingListAdapter(LABELS[localIndex], type.getSubTypes());
					case 5:
						return new SortingListAdapter(LABELS[localIndex], type.getDirectAttributeGroups());
					case 6:
						return new SortingListAdapter(LABELS[localIndex], type.getAttributeGroups());
					case 7:
						return new SortingListAdapter(LABELS[localIndex], type.getDirectObjectSetUses());
					case 8:
						return new SortingListAdapter(LABELS[localIndex], type.getObjectSetUses());
					case 9:
						return new SortingListAdapter(LABELS[localIndex], type.getObjects());
					case 10:
						return new SortingListAdapter(LABELS[localIndex], type.getElements());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class DynamicObjectTypeAdapter extends SystemObjectTypeAdapter implements Adapter {

		DynamicObjectTypeAdapter(String nodeTag, SystemObject object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getPersistenceMode()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			DynamicObjectType type = (DynamicObjectType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], type.getPersistenceMode().toString());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class DavApplicationAdapter extends ConfigurationObjectAdapter implements Adapter {

		DavApplicationAdapter(String nodeTag, DavApplication object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getClientApplications()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			try {
				switch(localIndex) {
					case 0:
						MutableSet applications = ((DavApplication)getObject()).getClientApplicationSet();
						return new SortingListAdapter(
								LABELS[localIndex], (applications == null ? new LinkedList() : applications.getElements())
						);
//					return new ListAdapter(LABELS[localIndex], ((DavApplication)getObject()).getClientApplications());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ConfigurationAuthorityAdapter extends ConfigurationObjectAdapter implements Adapter {

		ConfigurationAuthorityAdapter(String nodeTag, ConfigurationAuthority object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			try {
				switch(localIndex) {
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ConfigurationAreaAdapter extends ConfigurationObjectAdapter implements Adapter {

		ConfigurationAreaAdapter(String nodeTag, ConfigurationArea object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getConfigurationAuthority()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ConfigurationArea object = (ConfigurationArea)getObject();
			try {
				switch(localIndex) {
					case 0:
						return createAdapter(LABELS[localIndex], object.getConfigurationAuthority());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ObjectSetTypeAdapter extends SystemObjectTypeAdapter implements Adapter {

		ObjectSetTypeAdapter(String nodeTag, ObjectSetType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getMinimumElementCount()", "getMaximumElementCount()", "isMutable()", "getObjectTypes()",
		                                        "getReferenceType()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ObjectSetType type = (ObjectSetType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], type.getMinimumElementCount());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], type.getMaximumElementCount());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], type.isMutable());
					case 3:
						return new SortingListAdapter(LABELS[localIndex], type.getObjectTypes());
					case 4:
						return new ConstantAdapter(LABELS[localIndex], type.getReferenceType());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ObjectSetUseAdapter extends ConfigurationObjectAdapter implements Adapter {

		ObjectSetUseAdapter(String nodeTag, ObjectSetUse object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getObjectSetName()", "getObjectSetType()", "isRequired()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ObjectSetUse use = (ObjectSetUse)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], use.getObjectSetName());
					case 1:
						return createAdapter(LABELS[localIndex], use.getObjectSetType());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], use.isRequired());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public String getValue() {
			try {
				return ((ObjectSetUse)getObject()).getObjectSetName();
			}
			catch(Exception e) {
				return super.getValue();
			}
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AttributeSetAdapter extends ConfigurationObjectAdapter implements Adapter {

		AttributeSetAdapter(String nodeTag, AttributeSet object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getAttributes()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			try {
				switch(localIndex) {
					case 0:
						return createAdapter(LABELS[localIndex], ((AttributeSet)getObject()).getAttributes());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AttributeGroupAdapter extends AttributeSetAdapter implements Adapter {

		AttributeGroupAdapter(String nodeTag, AttributeGroup object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"isConfigurating()", "getAspects()", "getAttributeGroupUsages()"};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			AttributeGroup atg = (AttributeGroup)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], atg.isConfigurating());
					case 1:
						return createAdapter(LABELS[localIndex], atg.getAspects());
					case 2:
						return createAdapter(LABELS[localIndex], atg.getAttributeGroupUsages());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AttributeGroupUsageAdapter extends ConfigurationObjectAdapter implements Adapter {

		AttributeGroupUsageAdapter(String nodeTag, AttributeGroupUsage object) {
			super(nodeTag, object);
		}

		/*
		AttributeGroup getAttributeGroup();
		Aspect getAspect();
		boolean isConfigurating();
		boolean isExplicitDefined();
		Usage getUsage();
		*/
		private static final String[] LABELS = {"getAttributeGroup()", "getAspect()", "isConfigurating()", "isExplicitDefined()", "getUsage()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			AttributeGroupUsage atgUsage = (AttributeGroupUsage)getObject();
			try {
				switch(localIndex) {
					case 0:
						return createAdapter(LABELS[localIndex], atgUsage.getAttributeGroup());
					case 1:
						return createAdapter(LABELS[localIndex], atgUsage.getAspect());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], atgUsage.isConfigurating());
					case 3:
						return new ConstantAdapter(LABELS[localIndex], atgUsage.isExplicitDefined());
					case 4:
						return new ConstantAdapter(LABELS[localIndex], atgUsage.getUsage());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AttributeListDefinitionAdapter extends AttributeSetAdapter implements Adapter {

		AttributeListDefinitionAdapter(String nodeTag, AttributeListDefinition object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			AttributeListDefinition atl = (AttributeListDefinition)getObject();
			try {
				switch(localIndex) {
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AspectAdapter extends ConfigurationObjectAdapter implements Adapter {

		AspectAdapter(String nodeTag, Aspect object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			try {
				switch(localIndex) {
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class IntegerValueRangeAdapter extends ConfigurationObjectAdapter implements Adapter {

		IntegerValueRangeAdapter(String nodeTag, IntegerValueRange object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getMinimum()", "getMaximum()", "getConversionFactor()", "getUnit()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			IntegerValueRange range = (IntegerValueRange)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], range.getMinimum());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], range.getMaximum());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], range.getConversionFactor());
					case 3:
						return new ConstantAdapter(LABELS[localIndex], range.getUnit());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}

		public String getValue() {
			IntegerValueRange range = (IntegerValueRange)getObject();
			try {
				return range.getMinimum() + " bis " + range.getMaximum() + (range.getConversionFactor() == 1.0 ? "" : " * " + range.getConversionFactor()) + " "
				       + range.getUnit();
			}
			catch(Exception e) {
				return "bad range [" + e + "]";
			}
		}
	}

	static class IntegerValueStateAdapter extends ConfigurationObjectAdapter implements Adapter {

		IntegerValueStateAdapter(String nodeTag, IntegerValueState object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getValue()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], ((IntegerValueState)getObject()).getValue());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}

		public String getTag() throws Exception {
			return Long.toString(((IntegerValueState)getObject()).getValue());
		}
	}

	static class AttributeTypeAdapter extends ConfigurationObjectAdapter implements Adapter {

		AttributeTypeAdapter(String nodeTag, AttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getDefaultAttributeValue()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			AttributeType att = (AttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], att.getDefaultAttributeValue());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class IntegerAttributeTypeAdapter extends AttributeTypeAdapter implements Adapter {

		IntegerAttributeTypeAdapter(String nodeTag, IntegerAttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getByteCount()", "getRange()", "getStates()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			IntegerAttributeType att = (IntegerAttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], att.getByteCount());
					case 1:
						return createAdapter(LABELS[localIndex], att.getRange());
					case 2:
						return createAdapter(LABELS[localIndex], att.getStates());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class StringAttributeTypeAdapter extends AttributeTypeAdapter implements Adapter {

		StringAttributeTypeAdapter(String nodeTag, StringAttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getMaxLength()", "isLengthLimited()", "getEncodingName()", "getEncodingValue()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			StringAttributeType att = (StringAttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], att.getMaxLength());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], att.isLengthLimited());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], att.getEncodingName());
					case 3: {
						switch(att.getEncodingValue()) {
							case StringAttributeType.ISO_8859_1:
								return new ConstantAdapter(LABELS[localIndex], "ISO_8859_1 (" + StringAttributeType.ISO_8859_1 + ")");
							default:
								return new ConstantAdapter(LABELS[localIndex], att.getEncodingValue());
						}
					}
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class DoubleAttributeTypeAdapter extends AttributeTypeAdapter implements Adapter {

		DoubleAttributeTypeAdapter(String nodeTag, DoubleAttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getUnit()", "getAccuracy()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			DoubleAttributeType att = (DoubleAttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], att.getUnit());
					case 1: {
						switch(att.getAccuracy()) {
							case DoubleAttributeType.FLOAT:
								return new ConstantAdapter(LABELS[localIndex], "FLOAT (" + DoubleAttributeType.FLOAT + ")");
							case DoubleAttributeType.DOUBLE:
								return new ConstantAdapter(LABELS[localIndex], "DOUBLE (" + DoubleAttributeType.DOUBLE + ")");
							default:
								return new ConstantAdapter(LABELS[localIndex], att.getAccuracy());
						}
					}
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class TimeAttributeTypeAdapter extends AttributeTypeAdapter implements Adapter {

		TimeAttributeTypeAdapter(String nodeTag, TimeAttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"isRelative()", "getAccuracy()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			TimeAttributeType att = (TimeAttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], att.isRelative());
					case 1: {
						switch(att.getAccuracy()) {
							case TimeAttributeType.SECONDS:
								return new ConstantAdapter(LABELS[localIndex], "SECONDS (" + TimeAttributeType.SECONDS + ")");
							case TimeAttributeType.MILLISECONDS:
								return new ConstantAdapter(LABELS[localIndex], "MILLISECONDS (" + TimeAttributeType.MILLISECONDS + ")");
							default:
								return new ConstantAdapter(LABELS[localIndex], att.getAccuracy());
						}
					}
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ReferenceAttributeTypeAdapter extends AttributeTypeAdapter implements Adapter {

		ReferenceAttributeTypeAdapter(String nodeTag, ReferenceAttributeType object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getReferencedObjectType()", "isUndefinedAllowed()", "getReferenceType()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ReferenceAttributeType att = (ReferenceAttributeType)getObject();
			try {
				switch(localIndex) {
					case 0:
						return createAdapter(LABELS[localIndex], att.getReferencedObjectType());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], att.isUndefinedAllowed());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], att.getReferenceType());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class AttributeAdapter extends ConfigurationObjectAdapter implements Adapter {

		AttributeAdapter(String nodeTag, Attribute object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getPosition()", "isCountLimited()", "isCountVariable()", "getMaxCount()", "isArray()", "getAttributeType()",
		                                        "getDefaultAttributeValue()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			Attribute attribute = (Attribute)getObject();
			try {
				switch(localIndex) {
					case 0:
						return new ConstantAdapter(LABELS[localIndex], attribute.getPosition());
					case 1:
						return new ConstantAdapter(LABELS[localIndex], attribute.isCountLimited());
					case 2:
						return new ConstantAdapter(LABELS[localIndex], attribute.isCountVariable());
					case 3:
						return new ConstantAdapter(LABELS[localIndex], attribute.getMaxCount());
					case 4:
						return new ConstantAdapter(LABELS[localIndex], attribute.isArray());
					case 5:
						return createAdapter(LABELS[localIndex], attribute.getAttributeType());
					case 6:
						return new ConstantAdapter(LABELS[localIndex], attribute.getDefaultAttributeValue());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ObjectSetAdapter extends ConfigurationObjectAdapter implements Adapter {

		ObjectSetAdapter(String nodeTag, ObjectSet object) {
			super(nodeTag, object);
		}

		private static final String[] LABELS = {"getObjectSetType()", "getElements()",};

		public Adapter createChild(int searchIndex) {
			int localIndex = searchIndex - super.getChildCount();
			if(localIndex < 0) return super.createChild(searchIndex);
			ObjectSet set = (ObjectSet)getObject();
			try {
				switch(localIndex) {
					case 0:
						return createAdapter(LABELS[localIndex], set.getObjectSetType());
					case 1:
						return createAdapter(LABELS[localIndex], set.getElements());
				}
			}
			catch(Exception e) {
				return new ConstantAdapter(LABELS[localIndex], e);
			}
			return null;
		}

		public int getChildCount() {
			return super.getChildCount() + LABELS.length;
		}
	}

	static class ListAdapter extends AbstractAdapter implements Adapter {

		protected List _list;

		ListAdapter(String nodeTag, List list) {
			super(nodeTag);
			_list = list;
		}

		ListAdapter(String nodeTag, Object[] array) {
			super(nodeTag);
			_list = Arrays.asList(array);
		}

		public Adapter createChild(int searchIndex) {
			//System.out.println("returning itemAdapter");
			return createAdapter(null, _list.get(searchIndex));
		}

		public int getChildCount() {
			return _list.size();
		}

		public boolean isLeaf() {
			return false;
		}

		public String getValue() {
			return getChildCount() + " object" + (getChildCount() == 1 ? "" : "s");
		}
	}

	static class SortingListAdapter extends AbstractAdapter implements Adapter, Comparator<SystemObject> {
		Map<SystemObject, String> _compareStrings = new HashMap<SystemObject, String>();
		protected ArrayList<SystemObject> _list;

		SortingListAdapter(String nodeTag, List list) {
			super(nodeTag);
			_list = new ArrayList<SystemObject>(list);
			Collections.sort(_list, this);
		}

		public Adapter createChild(int searchIndex) {
			//System.out.println("returning itemAdapter");
			return createAdapter(null, _list.get(searchIndex));
		}

		public int getChildCount() {
			return _list.size();
		}

		public boolean isLeaf() {
			return false;
		}

		public String getValue() {
			return getChildCount() + " object" + (getChildCount() == 1 ? "" : "s");
		}

		/**
		 * Compares its two arguments for order.  Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater
		 * than the second.<p>
		 * <p/>
		 * The implementor must ensure that <tt>sgn(compare(x, y)) == -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This implies that <tt>compare(x,
		 * y)</tt> must throw an exception if and only if <tt>compare(y, x)</tt> throws an exception.)<p>
		 * <p/>
		 * The implementor must also ensure that the relation is transitive: <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies <tt>compare(x,
		 * z)&gt;0</tt>.<p>
		 * <p/>
		 * Finally, the implementer must ensure that <tt>compare(x, y)==0</tt> implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all <tt>z</tt>.<p>
		 * <p/>
		 * It is generally the case, but <i>not</i> strictly required that <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking, any comparator that
		 * violates this condition should clearly indicate this fact.  The recommended language is "Note: this comparator imposes orderings that are inconsistent with
		 * equals."
		 *
		 * @param o1 the first object to be compared.
		 * @param o2 the second object to be compared.
		 *
		 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		 *
		 * @throws ClassCastException if the arguments' types prevent them from being compared by this Comparator.
		 */
		public int compare(final SystemObject o1, final SystemObject o2) {
			return getCompareString(o1).compareToIgnoreCase(getCompareString(o2));
		}

		/**
		 * Liefert einen String der in Vergleichsfunktionen benutzt werden kann um die Sortierreihenfolge festzulegen. Der String enthält den Namen, die Pid und die
		 * Id des Objekts mit zwei bzw. einem Leerzeichen getrennt. Die Leerzeichen zwischen Name, Pid und Id sorgen dafür, dass zuerst nach Name und nur bei Namensgleichheit
		 * nach Pid respektive Id sortiert wird. Zwischen Name und Pid werden zwei Leerzeichen benutzt, um eine falsche Sortierung zu vermeiden, wenn in den Namen selbst
		 * auch Leerzeichen vorkommen. Anstelle von leeren Namen und Pids wird das Pipezeichen "|" benutzt, das dafür sorgt, dass die Objekte mit leeren
		 * Namen bzw. leeren Pids hinter denen mit nicht leeren Namen respektive nicht leeren Pids einsortiert werden.
		 * Umlaute werden durch ihre Zweibuchstaben-Äquivalente ersetzt, damit Objekte die Umlaute enthalten an einer sinnvollen Stelle einsortiert werden.
		 * Die einmal berechneten Vergleichsstrings werden in einer HashMap gespeichert um die Sortierzeit dramatisch zu verbessern.
		 * @param object Systemobjekt für das ein Vergleichstring bestimmt werden soll.
		 *
		 * @return Vergleichsstring
		 */
		private String getCompareString(final SystemObject object) {
			String compareString = _compareStrings.get(object);
			if(compareString == null) {
				final StringBuilder stringBuilder = new StringBuilder();
				final String name = object.getName();
				final String pid = object.getPid();
				stringBuilder.append(name.equals("") ? "|" : name);
				stringBuilder.append("  ");
				stringBuilder.append(object.getPid());
				stringBuilder.append("  ");
				stringBuilder.append(object.getId());
				//System.out.println("stringBuilder = " + stringBuilder);
				compareString = stringBuilder.toString().replace("Ä","Ae").replace("Ö", "Oe").replace("Ü", "Ue").replace("ä","ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
				_compareStrings.put(object, compareString);
			}
			return compareString;
		}
	}

	static class ConstantAdapter extends AbstractAdapter implements Adapter {

		private String _string;

		ConstantAdapter(String nodeTag, Object object) {
			super(nodeTag);
			if(object == null) {
				_string = "[" + null + "]";
			}
			else if(object instanceof Exception) {
				_string = "[" + ((Exception)object).toString() + "]";
			}
			else {
				_string = object.toString();
			}
		}

//		ConstantAdapter(String nodeTag, String string) {
//			super(nodeTag);
//			_string= string;
//		}

		ConstantAdapter(String nodeTag, long number) {
			super(nodeTag);
			_string = Long.toString(number);
		}

		ConstantAdapter(String nodeTag, double number) {
			super(nodeTag);
			_string = Double.toString(number);
		}

		ConstantAdapter(String nodeTag, boolean flag) {
			super(nodeTag);
			_string = Boolean.toString(flag);
		}

		public int getChildCount() {
			return 0;
		}

		public String getValue() {
			return _string;
		}
	}
}
