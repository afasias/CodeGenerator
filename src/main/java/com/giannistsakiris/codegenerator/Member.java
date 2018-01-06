package com.giannistsakiris.codegenerator;

public class Member extends BaseEntity implements Comparable<Member> {

	private static final int DEFAULT_STRING_LENGTH = 64;

	private ParentEntity parentEntity;
	private TypeEntity typeEntity;
	private boolean collection;
	private String parent;
	private Integer length;
	private boolean unique;
	private boolean key;
	private boolean label;
	private boolean searchable;

	public TypeEntity getTypeEntity() {
		return typeEntity;
	}

	public void setTypeEntity(TypeEntity typeEntity) {
		this.typeEntity = typeEntity;
	}

	public boolean isCollection() {
		return collection;
	}

	public void setCollection(boolean collection) {
		this.collection = collection;
	}

	public String getSqlName() {
		if (typeEntity instanceof Type) {
			return getName() + "_id";
		} else {
			return getName();
		}
	}

	public String getSqlType() {
		if (typeEntity == BuiltInType.Date) {
			return "DATETIME";
		} else if (typeEntity == BuiltInType.Boolean) {
			return "TINYINT";
		} else if (typeEntity == BuiltInType.Double) {
			return "DOUBLE";
		} else if (typeEntity == BuiltInType.Integer) {
			return "INTEGER";
		} else if (typeEntity == BuiltInType.String) {
			return "VARCHAR(" + (length != null ? length : DEFAULT_STRING_LENGTH) + ")";
		} else if (typeEntity instanceof Enumeration) {
			return "VARCHAR(" + ((Enumeration) typeEntity).getMaxValueLength() + ")";
		} else if (typeEntity instanceof Lookup) {
			return "VARCHAR(" + ((Lookup) typeEntity).getLength() + ")";
		} else {
			return "INTEGER";
		}
	}

	public String getTypeName() {
		if (typeEntity instanceof Lookup) {
			return "String";
		}
		String typeName = typeEntity.getName();
		if (collection) {
			typeName = "Collection<" + typeName + ">";
		}
		return typeName;
	}

	public String getCamelCaseName() {
		return toCamelCase(getName());
	}

	public String getGetterMethodName() {
		if (typeEntity == BuiltInType.Boolean) {
			return toCamelCase("is_" + getName());
		} else {
			return toCamelCase("get_" + getName());
		}
	}

	public String getSetterMethodName() {
		return toCamelCase("set_" + getName());
	}

	public ParentEntity getParentEntity() {
		return parentEntity;
	}

	public void setParentEntity(ParentEntity parent) {
		this.parentEntity = parent;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	private static String toCamelCase(String snakeCaseString) {
		StringBuilder sb = new StringBuilder();
		for (String word : snakeCaseString.toLowerCase().split("_")) {
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		return sb.toString();
	}

	public String getDalClassName() {
		return Helper.upperCaseFirstLetter(getName());
	}

	public String getFullyQualifiedDalPackageName() {
		if (parentEntity instanceof Type) {
			return ((Type) parentEntity).getFullyQualifiedDalName().toLowerCase();
		} else {
			return "";
		}
	}

	public String getFullyQualifiedDalName() {
		return getFullyQualifiedDalPackageName() + "." + getDalClassName();
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public boolean hasParent() {
		return parent != null && !parent.isEmpty();
	}

	@Override
	public int compareTo(Member o) {
		return getName().compareTo(o.getName());
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isLabel() {
		return label;
	}

	public void setLabel(boolean label) {
		this.label = label;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public boolean isKey() {
		return key;
	}

	public void setKey(boolean key) {
		this.key = key;
	}
}
