package com.giannistsakiris.codegenerator;

public interface TableEntity {

	public String getTableName();

	public void setTableName(String tableName);

	public String getDalClassName();

	public String getInstanceName();

	public String getCollectionName();
}
