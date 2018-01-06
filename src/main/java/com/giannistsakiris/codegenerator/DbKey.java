package com.giannistsakiris.codegenerator;

public class DbKey {

	private String field;
	private boolean unique;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}
}
