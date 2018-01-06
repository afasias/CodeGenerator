package com.giannistsakiris.codegenerator;

public interface ChildEntity {

	public ParentEntity getParent();

	public void setParent(ParentEntity parent);

	public TypeEntity getTopParentType();

}
