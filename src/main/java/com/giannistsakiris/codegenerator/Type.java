package com.giannistsakiris.codegenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Type extends BaseEntity implements ChildEntity, ParentEntity, TypeEntity, TableEntity, Comparable<Type> {

	private ParentEntity parent;
	private List<Enumeration> enumerations = new ArrayList<>();
	private List<Lookup> lookups = new ArrayList<>();
	private List<Type> types = new ArrayList<>();
	private List<Member> members = new ArrayList<>();
	private List<DbKey> dbKeys = new ArrayList<>();
	private Set<Type> referecingTypes = new HashSet<>();
	private boolean slave;
	private String tableName;

	@Override
	public ParentEntity getParent() {
		return parent;
	}

	@Override
	public void setParent(ParentEntity parent) {
		this.parent = parent;
	}

	@Override
	public List<Enumeration> getEnumerations() {
		return enumerations;
	}

	@Override
	public void setEnumerations(List<Enumeration> enumerations) {
		this.enumerations = enumerations;
	}

	@Override
	public List<Lookup> getLookups() {
		return lookups;
	}

	@Override
	public void setLookups(List<Lookup> lookups) {
		this.lookups = lookups;
	}

	@Override
	public List<Type> getTypes() {
		return types;
	}

	@Override
	public void setTypes(List<Type> types) {
		this.types = types;
	}

	public List<Member> getMembers() {
		return members;
	}

	public void setMembers(List<Member> members) {
		this.members = members;
	}

	public boolean isSlave() {
		return slave;
	}

	public void setSlave(boolean slave) {
		this.slave = slave;
	}

	private Map<String, TypeEntity> getTypeEntitiesMap() {
		Map<String, TypeEntity> map = new HashMap<>();
		for (Lookup lookup : lookups) {
			map.put(lookup.getName(), lookup);
		}
		for (Enumeration enumeration : enumerations) {
			map.put(enumeration.getName(), enumeration);
		}
		for (Type type : types) {
			map.put(type.getName(), type);
		}
		return map;
	}

	@Override
	public TypeEntity findTypeEntityByName(String name) {
		Map<String, TypeEntity> map = getTypeEntitiesMap();
		if (map.containsKey(name)) {
			return map.get(name);
		}
		return parent.findTypeEntityByName(name);
	}

	@Override
	public String getFullyQualifiedName() {
		return (parent instanceof TypeEntity && parent != null ? ((TypeEntity) parent).getFullyQualifiedName() + "."
				: "") + getName();
	}

	public String getFullyQualifiedDalName() {
		return (parent instanceof Type && parent != null ? ((Type) parent).getFullyQualifiedDalName() + "." : "")
				+ getDalClassName();
	}

	boolean containsCollections() {
		for (Member member : members) {
			if (member.isCollection()) {
				return true;
			}
		}
		for (Type type : types) {
			if (type.containsCollections()) {
				return true;
			}
		}
		return false;
	}

	boolean containsDates() {
		for (Member member : members) {
			if (member.getTypeName().equals("Date")) {
				return true;
			}
		}
		for (Type type : types) {
			if (type.containsDates()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDalClassName() {
		return getName();
	}

	@Override
	public String getInstanceName() {
		return Character.toLowerCase(getName().charAt(0)) + getName().substring(1);
	}

	@Override
	public String getCollectionName() {
		return tableName;
	}

	@Override
	public TypeEntity getTopParentType() {
		if (parent instanceof ChildEntity) {
			return ((ChildEntity) parent).getTopParentType();
		} else {
			return this;
		}
	}

	public Set<Type> getReferecingTypes() {
		return referecingTypes;
	}

	public void setReferecingTypes(Set<Type> referecingTypes) {
		this.referecingTypes = referecingTypes;
	}

	@Override
	public List<TypeEntity> getAllTypeEntities() {
		List<TypeEntity> typeEntities = new ArrayList<>();
		typeEntities.addAll(enumerations);
		typeEntities.addAll(lookups);
		typeEntities.addAll(types);
		return typeEntities;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public int compareTo(Type o) {
		return getName().compareTo(o.getName());
	}

	boolean containsSearchables() {
		for (Member member : members) {
			if (member.isSearchable()) {
				return true;
			}
		}
		return false;
	}

	public List<DbKey> getDbKeys() {
		return dbKeys;
	}

	public void setDbKeys(List<DbKey> dbKeys) {
		this.dbKeys = dbKeys;
	}
}
