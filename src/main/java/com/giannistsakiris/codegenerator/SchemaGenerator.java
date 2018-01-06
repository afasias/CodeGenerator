package com.giannistsakiris.codegenerator;

import java.io.File;
import java.io.PrintWriter;

public class SchemaGenerator {

	private final Project project;

	private SchemaGenerator(Project project) {
		this.project = project;
	}

	public static void generateSchema(Project project) throws Exception {
		new SchemaGenerator(project).run();
	}

	private boolean sqlite = false;

	private void run() throws Exception {
		System.out.println("Generating database schema");
		String path = project.getSourceDirectory() + "resources";
		new File(path).mkdirs();
		try (PrintWriter writer = new PrintWriter(path + '/' + project.getName() + ".mysql.sql", "UTF-8")) {
			writer.write(generateSqlTables(project));
			writer.close();
		}
		sqlite = true;
		try (PrintWriter writer = new PrintWriter(path + '/' + project.getName() + ".sqlite.sql", "UTF-8")) {
			writer.write(generateSqlTables(project));
			writer.close();
		}
	}

	private String generateSqlTables(ParentEntity parent) {
		StringBuilder sb = new StringBuilder();
		for (Lookup lookup : parent.getLookups()) {
			sb.append(generateSqlTable(lookup, ""));
		}
		for (Type type : parent.getTypes()) {
			sb.append(generateSqlTables(type, type.getInstanceName()));
		}
		for (Member member : parent.getMembers()) {
			if (member.isCollection()) {
				sb.append(generateSqlTable(member, ""));
			}
		}
		return sb.toString();
	}

	private String generateSqlTables(Type type, String tablePrefix) {
		StringBuilder sb = new StringBuilder();
		for (Lookup lookup : type.getLookups()) {
			sb.append(generateSqlTable(lookup, tablePrefix));
		}
		for (Type childType : type.getTypes()) {
			sb.append(generateSqlTables(childType, tablePrefix.isEmpty() ? childType.getInstanceName()
					: tablePrefix + "_" + childType.getInstanceName()));
		}
		return sb.toString();
	}

	private String generateSqlTable(Lookup lookup, String tablePrefix) {
		StringBuilder sb = new StringBuilder();
		String tableName = tablePrefix.isEmpty() ? lookup.getTableName() : tablePrefix + "_" + lookup.getTableName();
		// sb.append("\nDROP TABLE IF EXISTS ").append(tableName).append(";");
		sb.append("\nCREATE TABLE ").append(tableName).append(" (\n");
		sb.append("    id VARCHAR(").append(lookup.getLength()).append("),\n");
		sb.append("    PRIMARY KEY(id)\n");
		if (sqlite) {
			sb.append(");\n");
		} else {
			sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n");
		}
		return sb.toString();
	}

	private String generateSqlTable(Member member, String tablePrefix) {
		StringBuilder sb = new StringBuilder();
		String tableName = tablePrefix.isEmpty() ? member.getName() : tablePrefix + "_" + member.getName();
		// sb.append("\nDROP TABLE IF EXISTS ").append(tableName).append(";");
		sb.append("\nCREATE TABLE ").append(tableName).append(" (\n");
		if (sqlite) {
			sb.append("    id INTEGER PRIMARY KEY,\n");
		} else {
			sb.append("    id INTEGER AUTO_INCREMENT,\n");
		}
		Type type = (Type) member.getTypeEntity();
		for (Member childMember : type.getMembers()) {
			if (!childMember.isCollection()) {
				sb.append("    ").append(childMember.getSqlName()).append(" ").append(childMember.getSqlType());
				if (sqlite) {
					if (childMember.isUnique()) {
						sb.append(" UNIQUE");
					} else if (childMember.isKey()) {
						sb.append(" KEY");
					}
				}
				sb.append(",\n");
				if (childMember.getTypeEntity() instanceof Lookup) {
					((Lookup) childMember.getTypeEntity()).getReferences()
							.add(new Reference(tableName, childMember.getSqlName()));
				}
			}
		}
		if (member.getParentEntity() != project) {
			sb.append("    parent_id INTEGER,\n");
			sb.append("    FOREIGN KEY(parent_id) REFERENCES ").append(tablePrefix).append("(id) ON DELETE CASCADE,\n");
		}
		for (DbKey dbkey : type.getDbKeys()) {
			sb.append("    ").append(dbkey.isUnique() ? "UNIQUE" : "KEY").append("(").append(dbkey.getField())
					.append("),\n");
		}
		for (Member childMember : type.getMembers()) {
			if (!sqlite) {
				if (childMember.isUnique()) {
					sb.append("    UNIQUE(").append(childMember.getSqlName()).append("),\n");
				} else if (childMember.isKey()) {
					sb.append("    KEY(").append(childMember.getSqlName()).append("),\n");
				}
			}
			if (childMember.hasParent()) {
				sb.append("    FOREIGN KEY(").append(childMember.getSqlName()).append(") REFERENCES ")
						.append(childMember.getParent()).append("(id) ON DELETE RESTRICT ON UPDATE CASCADE,\n");
			} else if (childMember.getTypeEntity() instanceof Lookup) {
				Lookup lookup = (Lookup) childMember.getTypeEntity();
				sb.append("    FOREIGN KEY(").append(childMember.getSqlName()).append(") REFERENCES ")
						.append(lookup.getExpandedTableName()).append("(id) ON DELETE RESTRICT ON UPDATE CASCADE,\n");
			}
		}
		if (sqlite) {
			sb.deleteCharAt(sb.length() - 2);
			sb.append(");\n");
		} else {
			sb.append("    PRIMARY KEY(id)\n");
			sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8;\n");
		}
		for (Member childMember : type.getMembers()) {
			if (childMember.isCollection()) {
				sb.append(generateSqlTable(childMember,
						tablePrefix.isEmpty() ? member.getName() : tablePrefix + "_" + member.getName()));
			}
		}
		return sb.toString();
	}
}
