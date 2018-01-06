package com.giannistsakiris.codegenerator;

public class Reference implements Comparable<Reference> {

	private String table;
	private String field;

	public Reference(String table, String field) {
		this.table = table;
		this.field = field;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	@Override
	public int compareTo(Reference o) {
		return combine().compareTo(o.combine());
	}

	@Override
	public int hashCode() {
		return combine().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return combine().equals(((Reference) obj).combine());
	}

	private String combine() {
		return table + "_" + field;
	}
}
