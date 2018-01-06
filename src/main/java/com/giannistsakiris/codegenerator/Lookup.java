package com.giannistsakiris.codegenerator;

import java.util.HashSet;
import java.util.Set;

public class Lookup extends BaseEntity implements TableEntity, ChildEntity, TypeEntity, Comparable<Lookup> {

	private Set<Reference> references = new HashSet<>();

	private String tableName;
	private ParentEntity parent;
	private int length = 32;

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public ParentEntity getParent() {
		return parent;
	}

	@Override
	public void setParent(ParentEntity parent) {
		this.parent = parent;
	}

	@Override
	public String getFullyQualifiedName() {
		return (parent instanceof TypeEntity && parent != null ? ((TypeEntity) parent).getFullyQualifiedName() + "."
				: "") + getName();
	}

	public String getFullyQualifiedDalPackageName() {
		return (parent instanceof Type && parent != null ? ((Type) parent).getFullyQualifiedDalName() + "." : "");
	}

	public String getFullyQualifiedDalName() {
		return getFullyQualifiedDalPackageName().toLowerCase() + getDalClassName();
	}

	@Override
	public String getDalClassName() {
		return Helper.upperCaseFirstLetter(getCollectionName());
	}

	@Override
	public String getInstanceName() {
		return Helper.toCamelCase(getName());
	}

	@Override
	public String getCollectionName() {
		return Helper.toCamelCase(tableName);
	}

	@Override
	public TypeEntity getTopParentType() {
		if (parent instanceof ChildEntity) {
			return ((ChildEntity) parent).getTopParentType();
		} else {
			return this;
		}
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Set<Reference> getReferences() {
		return references;
	}

	public void setReferences(Set<Reference> tableReferences) {
		this.references = tableReferences;
	}

	public String getExpandedTableName() {
		String tblName = getTableName();
		for (ParentEntity typeEntity = getParent(); typeEntity instanceof Type; typeEntity = ((Type) typeEntity)
				.getParent()) {
			tblName = ((Type) typeEntity).getInstanceName() + "_" + tblName;
		}
		return tblName;
	}

	@Override
	public int compareTo(Lookup o) {
		return getName().compareTo(o.getName());
	}

}
