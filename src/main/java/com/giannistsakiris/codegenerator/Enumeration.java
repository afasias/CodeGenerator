package com.giannistsakiris.codegenerator;

import java.util.ArrayList;
import java.util.List;

public class Enumeration extends BaseEntity implements ChildEntity, TypeEntity, Comparable<Enumeration> {

	private ParentEntity parent;
	private List<String> values = new ArrayList<>();

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
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
		if (parent instanceof TypeEntity && parent != null) {
			return ((TypeEntity) parent).getFullyQualifiedName() + "." + getName();
		} else {
			return getName();
		}
	}

	public int getMaxValueLength() {
		int maxLength = 0;
		for (String value : values) {
			if (value.length() > maxLength) {
				maxLength = value.length();
			}
		}
		return maxLength;
	}

	@Override
	public TypeEntity getTopParentType() {
		if (parent instanceof ChildEntity) {
			return ((ChildEntity) parent).getTopParentType();
		} else {
			return this;
		}
	}

	@Override
	public int compareTo(Enumeration o) {
		return getName().compareTo(o.getName());
	}
}
