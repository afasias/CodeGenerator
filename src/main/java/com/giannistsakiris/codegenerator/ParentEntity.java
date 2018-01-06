package com.giannistsakiris.codegenerator;

import java.util.List;

public interface ParentEntity {

	public List<Enumeration> getEnumerations();

	public void setEnumerations(List<Enumeration> enumerations);

	public List<Lookup> getLookups();

	public void setLookups(List<Lookup> lookups);

	public List<Type> getTypes();

	public void setTypes(List<Type> types);

	public List<Member> getMembers();

	public List<DbKey> getDbKeys();

	public void setMembers(List<Member> members);

	public TypeEntity findTypeEntityByName(String name);

	public List<TypeEntity> getAllTypeEntities();
}
