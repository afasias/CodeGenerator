package com.giannistsakiris.codegenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DalGenerator {

	private final static String TAB = "    ";

	private final String srcDir;
	private final Project project;
	private final String modelsPackageName;
	private final String dalPackageName;
	private final String dalPath;

	private DalGenerator(Project project) {
		this.project = project;
		this.srcDir = project.getSourceDirectory();
		this.modelsPackageName = project.getPackageName() + ".model";
		this.dalPackageName = project.getPackageName() + ".dal";
		this.dalPath = srcDir + dalPackageName.replace('.', '/');
	}

	public static void generateDals(Project project) throws Exception {
		new DalGenerator(project).run();
	}

	private void run() throws Exception {
		System.out.println("Generating data access layer classes");
		resetDalPath();
		generateTemplatedClassFiles();
		generateLookupDals(project, "", "");
		generateTopLevelMemberDals();
	}

	private void generateLookupDals(ParentEntity parent, String tablePrefix, String packagePrefix) throws Exception {
		for (Lookup lookup : parent.getLookups()) {
			generateLookupDal(lookup, tablePrefix, packagePrefix);
		}
		for (Type type : parent.getTypes()) {
			generateLookupDals(type, tablePrefix + type.getInstanceName() + "_",
					packagePrefix + "." + type.getInstanceName());
		}
	}

	private void generateTopLevelMemberDals() throws Exception {
		for (Member member : project.getMembers()) {
			if (member.isCollection()) {
				generateTopLevelMemberDal(member);
			}
		}
	}

	private void generateTopLevelMemberDal(Member member) throws Exception {
		String path = srcDir + (dalPackageName + "." + Helper.upperCaseFirstLetter(member.getName())).replace('.', '/')
				+ ".java";
		String dirPath = new File(path).getParent().toLowerCase();
		new File(dirPath).mkdirs();
		try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
			Set<String> usedModels = new HashSet<>();
			Set<String> usedDals = new HashSet<>();
			String code = generateMemberDal(member, "", "", usedModels, usedDals);
			// generateMemberDal(member,usedModels,dals);
			writer.println("package " + dalPackageName + ";");
			writer.println();
			if (!usedDals.isEmpty() || !usedModels.isEmpty()) {
				for (String dal : usedDals) {
					if (!(Helper.packageOnly(dalPackageName + "." + dal).equals(dalPackageName))
							&& !code.contains("class " + Helper.classOnly(dal))) {
						if (!dal.equals(Helper.classOnly(dal))) {
							writer.println("import " + dalPackageName + "." + dal + ";");
						}
					}
				}
				for (String model : usedModels) {
					writer.println("import " + modelsPackageName + "." + model + ";");
				}
				writer.println();
			}
			writer.println("import java.sql.SQLException;");
			writer.println("import java.sql.PreparedStatement;");
			writer.println("import java.sql.ResultSet;");
			writer.println("import java.sql.Statement;");
			writer.println("import java.util.ArrayList;");
			writer.println("import java.util.Collection;");
			if (((Type) member.getTypeEntity()).containsDates()) {
				writer.println("import java.util.Date;");
			}
			writer.write(code);
			writer.close();
		}
	}

	private void generateLookupDal(Lookup lookup, String tablePrefix, String packagePrefix) throws Exception {
		String path = srcDir
				+ (dalPackageName + packagePrefix + "/" + Helper.upperCaseFirstLetter(lookup.getCollectionName()))
						.replace('.', '/')
				+ ".java";
		String tableName = tablePrefix + lookup.getTableName();
		new File(new File(path).getParent().toLowerCase()).mkdirs();
		try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
			writer.println("package " + dalPackageName + packagePrefix + ";");
			writer.println();
			if (!packagePrefix.isEmpty()) {
				writer.println("import " + dalPackageName + ".Database;");
				writer.println("import " + dalPackageName + ".LookupDal;");
				writer.println();
			}
			writer.println("import java.sql.SQLException;");
			writer.println("import java.sql.PreparedStatement;");
			writer.println("import java.sql.ResultSet;");
			writer.println("import java.sql.Statement;");
			writer.println("import java.util.ArrayList;");
			writer.println("import java.util.Collection;");
			writer.println();
			writer.println("final public class " + Helper.upperCaseFirstLetter(lookup.getCollectionName())
					+ " implements LookupDal {");
			writer.println();
			writer.println(TAB + "static private " + Helper.upperCaseFirstLetter(lookup.getCollectionName())
					+ " instance = null;");
			writer.println();
			writer.println(TAB + "private " + Helper.upperCaseFirstLetter(lookup.getCollectionName()) + "() {");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public static " + Helper.upperCaseFirstLetter(lookup.getCollectionName())
					+ " getInstance() {");
			writer.println(TAB + TAB + "if (instance == null) {");
			writer.println(TAB + TAB + TAB + "instance = new " + Helper.upperCaseFirstLetter(lookup.getCollectionName())
					+ "();");
			writer.println(TAB + TAB + "}");
			writer.println(TAB + TAB + "return instance;");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public String insert(String id) throws SQLException {");
			writer.println(TAB + TAB + "String sql = \"insert into " + tableName + " (id) values( ? )\";");
			writer.println(TAB + TAB + "PreparedStatement stmt = Database.getConnection().prepareStatement(sql);");
			writer.println(TAB + TAB + "stmt.setString(1,id);");
			writer.println(TAB + TAB + "stmt.execute();");
			writer.println(TAB + TAB + "return id;");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public String insertIgnore(String id) {");
			writer.println(TAB + TAB + "try {");
			writer.println(TAB + TAB + TAB + "insert(id);");
			writer.println(TAB + TAB + "} catch (SQLException ex) {");
			writer.println(TAB + TAB + TAB + "// ignore");
			writer.println(TAB + TAB + "}");
			writer.println(TAB + TAB + "return id;");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public void delete(String id) throws SQLException {");
			writer.println(TAB + TAB + "String sql = \"delete from " + tableName + " where id = ?\";");
			writer.println(TAB + TAB + "PreparedStatement stmt = Database.getConnection().prepareStatement(sql);");
			writer.println(TAB + TAB + "stmt.setString(1,id);");
			writer.println(TAB + TAB + "stmt.execute();");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public void update(String oldId, String newId) throws SQLException {");
			writer.println(TAB + TAB + "String sql = \"update " + tableName + " set id = ? where id = ?\";");
			writer.println(TAB + TAB + "PreparedStatement stmt = Database.getConnection().prepareStatement(sql);");
			writer.println(TAB + TAB + "stmt.setString(1,newId);");
			writer.println(TAB + TAB + "stmt.setString(2,oldId);");
			writer.println(TAB + TAB + "stmt.execute();");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public void merge(String idToMerge, String idToMergeWith) throws SQLException {");
			List<Reference> referenceList = new ArrayList<>(lookup.getReferences());
			Collections.sort(referenceList);
			for (Reference reference : referenceList) {
				writer.println(TAB + TAB + "{");
				writer.println(TAB + TAB + TAB + "String sql = \"update " + reference.getTable() + " set "
						+ reference.getField() + " = ? where " + reference.getField() + " = ?\";");
				writer.println(
						TAB + TAB + TAB + "PreparedStatement stmt = Database.getConnection().prepareStatement(sql);");
				writer.println(TAB + TAB + TAB + "stmt.setString(1,idToMergeWith);");
				writer.println(TAB + TAB + TAB + "stmt.setString(2,idToMerge);");
				writer.println(TAB + TAB + TAB + "stmt.execute();");
				writer.println(TAB + TAB + "}");
			}
			writer.println(TAB + TAB + "delete(idToMerge);");
			writer.println(TAB + "}");
			writer.println();
			writer.println(TAB + "public Collection<String> fetchAll() throws SQLException {");
			writer.println(TAB + TAB + "String sql = \"select id from " + tableName + " order by id asc\";");
			writer.println(TAB + TAB + "Statement stmt = Database.getConnection().createStatement();");
			writer.println(TAB + TAB + "ResultSet result = stmt.executeQuery(sql);");
			writer.println(TAB + TAB + "Collection<String> ids = new ArrayList<>();");
			writer.println(TAB + TAB + "while (result.next()) {");
			writer.println(TAB + TAB + TAB + "ids.add(result.getString(\"id\"));");
			writer.println(TAB + TAB + "}");
			writer.println(TAB + TAB + "return ids;");
			writer.println(TAB + "}");
			writer.println("}");
			writer.close();
		}
	}

	private String generateMemberDal(Member member, String tablePrefix, String indent, Set<String> usedModels,
			Set<String> usedDals) {
		StringBuilder sb = new StringBuilder();

		String tableName = tablePrefix + member.getName();
		Type type = (Type) member.getTypeEntity();
		sb.append("\n");

		sb.append(indent).append(tablePrefix.isEmpty() ? "" : "static ").append("final public class ")
				.append(member.getDalClassName()).append(" {\n");

		if (tablePrefix.isEmpty()) {
			sb.append("\n");
			sb.append(indent).append(TAB).append("static private ").append(member.getDalClassName())
					.append(" instance = null;\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("private ").append(member.getDalClassName()).append("() {\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static ").append(member.getDalClassName())
					.append(" getInstance() {\n");
			sb.append(indent).append(TAB).append(TAB).append("if (instance == null) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("instance = new ")
					.append(member.getDalClassName()).append("();\n");
			sb.append(indent).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append("return instance;\n");
			sb.append(indent).append(TAB).append("}\n");
		}

		sb.append("\n");
		usedModels.add(Helper.topMostClass(type.getFullyQualifiedName()));
		sb.append(indent).append(TAB).append("private static void assign(").append(type.getFullyQualifiedName())
				.append(" ").append(type.getInstanceName()).append(",PreparedStatement stmt) throws SQLException {\n");
		int fieldCount = appendFieldValueAssignments(sb, indent + TAB + TAB, type, usedDals);
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		usedModels.add(Helper.topMostClass(type.getFullyQualifiedName()));
		sb.append(indent).append(TAB).append("public static void insert(").append(type.getFullyQualifiedName())
				.append(" ").append(type.getInstanceName()).append(") throws SQLException {\n");
		sb.append(indent).append(TAB).append(TAB).append("String sql = \"")
				.append(generateInsertStatement(tableName, member)).append("\";\n");
		sb.append(indent).append(TAB).append(TAB).append(
				"PreparedStatement stmt = Database.getConnection().prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);\n");
		sb.append(indent).append(TAB).append(TAB).append("assign(").append(type.getInstanceName()).append(",stmt);\n");
		if (type.isSlave()) {
			sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(").append(fieldCount + 1).append(",")
					.append(type.getInstanceName()).append(".getParentId());\n");
		}
		sb.append(indent).append(TAB).append(TAB).append("stmt.execute();\n");
		sb.append(indent).append(TAB).append(TAB).append("ResultSet generatedKeys = stmt.getGeneratedKeys();\n");
		sb.append(indent).append(TAB).append(TAB).append("if (generatedKeys.next()) {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB).append(type.getInstanceName())
				.append(".setId(generatedKeys.getInt(1));\n");
		sb.append(indent).append(TAB).append(TAB).append("} else {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB)
				.append("throw new SQLException(\"No generated key obtained.\");\n");
		sb.append(indent).append(TAB).append(TAB).append("}\n");
		for (Member submember : type.getMembers()) {
			if (submember.isCollection()) {
				Type memberType = (Type) submember.getTypeEntity();
				usedModels.add(Helper.topMostClass(memberType.getFullyQualifiedName()));
				sb.append(indent).append(TAB).append(TAB).append("for (").append(memberType.getFullyQualifiedName())
						.append(" ").append(memberType.getInstanceName()).append(" : ").append(type.getInstanceName())
						.append(".").append(submember.getGetterMethodName()).append("()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(memberType.getInstanceName())
						.append(".setParentId(").append(type.getInstanceName()).append(".getId());\n");
				usedDals.add(submember.getFullyQualifiedDalName());
				sb.append(indent).append(TAB).append(TAB).append(TAB)
						.append(Helper.classOnly(submember.getFullyQualifiedDalName())).append(".insert(")
						.append(memberType.getInstanceName()).append(");\n");
				sb.append(indent).append(TAB).append(TAB).append("}\n");
			}
		}
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		sb.append(indent).append(TAB).append("public static void update(").append(type.getFullyQualifiedName())
				.append(" ").append(type.getInstanceName()).append(") throws SQLException {\n");
		sb.append(indent).append(TAB).append(TAB).append("String sql = \"")
				.append(generateUpdateStatement(tableName, member)).append("\";\n");
		sb.append(indent).append(TAB).append(TAB)
				.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
		// appendFieldValueAssignments(sb, TAB + TAB, type, usedDals);
		sb.append(indent).append(TAB).append(TAB).append("assign(").append(type.getInstanceName()).append(",stmt);\n");
		sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(").append(fieldCount + 1).append(",")
				.append(type.getInstanceName()).append(".getId());\n");
		sb.append(indent).append(TAB).append(TAB).append("stmt.execute();\n");
		for (Member submember : type.getMembers()) {
			if (submember.isCollection()) {
				Type memberType = (Type) submember.getTypeEntity();
				sb.append(indent).append(TAB).append(TAB).append("for (").append(memberType.getFullyQualifiedName())
						.append(" ").append(memberType.getInstanceName()).append(" : ").append(type.getInstanceName())
						.append(".").append(submember.getGetterMethodName()).append("()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append("if (")
						.append(memberType.getInstanceName()).append(".isPersistent()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append("if (")
						.append(memberType.getInstanceName()).append(".isMarkedForDeletion()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append(TAB)
						.append(Helper.classOnly(submember.getFullyQualifiedDalName())).append(".delete(")
						.append(memberType.getInstanceName()).append(");\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append("} else {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append(TAB)
						.append(Helper.classOnly(submember.getFullyQualifiedDalName())).append(".update(")
						.append(memberType.getInstanceName()).append(");\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append("}\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append("} else {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append(memberType.getInstanceName())
						.append(".setParentId(").append(type.getInstanceName()).append(".getId());\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB)
						.append(Helper.classOnly(submember.getFullyQualifiedDalName())).append(".insert(")
						.append(memberType.getInstanceName()).append(");\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append("}\n");
				sb.append(indent).append(TAB).append(TAB).append("}\n");
			}
		}
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		sb.append(indent).append(TAB).append("public static void save(").append(type.getFullyQualifiedName())
				.append(" ").append(type.getInstanceName()).append(") throws SQLException {\n");
		sb.append(indent).append(TAB).append(TAB).append("if (").append(type.getInstanceName())
				.append(".isPersistent()) {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB).append("update(").append(type.getInstanceName())
				.append(");\n");
		sb.append(indent).append(TAB).append(TAB).append("} else {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB).append("insert(").append(type.getInstanceName())
				.append(");\n");
		sb.append(indent).append(TAB).append(TAB).append("}\n");
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		sb.append(indent).append(TAB).append("public static void delete(").append(type.getFullyQualifiedName())
				.append(" ").append(type.getInstanceName()).append(") throws SQLException {\n");
		for (Member submember : type.getMembers()) {
			if (submember.isCollection()) {
				Type memberType = (Type) submember.getTypeEntity();
				sb.append(indent).append(TAB).append(TAB).append("for (").append(memberType.getFullyQualifiedName())
						.append(" ").append(memberType.getInstanceName()).append(" : ").append(type.getInstanceName())
						.append(".").append(submember.getGetterMethodName()).append("()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB)
						.append(Helper.classOnly(submember.getFullyQualifiedDalName())).append(".delete(")
						.append(memberType.getInstanceName()).append(");\n");
				sb.append(indent).append(TAB).append(TAB).append("}\n");
			}
		}
		sb.append(indent).append(TAB).append(TAB).append("String sql = \"delete from ").append(tableName)
				.append(" where id = ?\";").append("\n");
		sb.append(indent).append(TAB).append(TAB)
				.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
		sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,").append(type.getInstanceName())
				.append(".getId());\n");
		sb.append(indent).append(TAB).append(TAB).append("stmt.execute();\n");
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		sb.append(indent).append(TAB).append("public static ").append(type.getFullyQualifiedName())
				.append(" extract(ResultSet result) throws SQLException {\n");
		sb.append(generateCreateObjectFromResult(type, indent + TAB + TAB, usedDals));
		sb.append(indent).append(TAB).append("}\n");

		sb.append("\n");
		sb.append(indent).append(TAB).append("public static ").append(type.getFullyQualifiedName())
				.append(" fetchById(int id) throws SQLException {\n");
		sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
				.append(" where id = ?\";\n");
		sb.append(indent).append(TAB).append(TAB)
				.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
		sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,id);\n");
		sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
		sb.append(indent).append(TAB).append(TAB).append("if (result.next()) {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB).append("return extract(result);\n");
		sb.append(indent).append(TAB).append(TAB).append("} else {\n");
		sb.append(indent).append(TAB).append(TAB).append(TAB).append("return null;\n");
		sb.append(indent).append(TAB).append(TAB).append("}\n");
		sb.append(indent).append(TAB).append("}\n");

		if (member.getParentEntity() == project) {
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchByIds( int[] ids ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("return fetchByIds(ids,true,0);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchByIds( int[] ids, boolean asc ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("return fetchByIds(ids,asc,0);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchByIds( int[] ids, boolean asc, int limit ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("Collection<").append(type.getFullyQualifiedName())
					.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
			sb.append(indent).append(TAB).append(TAB).append("if (ids.length > 0) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("String sql = \"select * from ")
					.append(tableName)
					.append(" where id in (\"+DALHelper.stringRepeat(\"?,\",ids.length-1)+\"?) order by id \"+(asc ? \"asc\" : \"desc\")+(limit > 0 ? \" limit \"+limit : \"\");\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB)
					.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("for (int i = 0; i < ids.length; i++) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append("stmt.setInt(i+1,ids[i]);\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("while (result.next()) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
					.append(".add(extract(result));\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName()).append(";\n");
			sb.append(indent).append(TAB).append("}\n");

			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchAll() throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("return fetchAll(true,0);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchAll( boolean asc ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("return fetchAll(asc,0);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchAll( boolean asc, int limit ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("final Collection<").append(type.getFullyQualifiedName())
					.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
			sb.append(indent).append(TAB).append(TAB).append("fetchAllCallback(asc, limit, new Callback<")
					.append(type.getFullyQualifiedName()).append(">() {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("@Override\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("public void objectFetched(")
					.append(type.getFullyQualifiedName()).append(" object) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
					.append(".add(object);\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append("});\n");
			sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName()).append(";\n");
			sb.append(indent).append(TAB).append("}\n");

			sb.append("\n");
			sb.append(indent).append(TAB).append("public static void fetchAllCallback( Callback<")
					.append(type.getFullyQualifiedName()).append("> callback ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("fetchAllCallback(true,0,callback);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB).append("public static void fetchAllCallback( boolean asc, Callback<")
					.append(type.getFullyQualifiedName()).append("> callback ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("fetchAllCallback(asc,0,callback);\n");
			sb.append(indent).append(TAB).append("}\n");
			sb.append("\n");
			sb.append(indent).append(TAB)
					.append("public static void fetchAllCallback( boolean asc, int limit, Callback<")
					.append(type.getFullyQualifiedName()).append("> callback ) throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
					.append(" order by id \"+(asc ? \"asc\" : \"desc\")+(limit > 0 ? \" limit \"+limit : \"\");\n");
			sb.append(indent).append(TAB).append(TAB)
					.append("Statement stmt = Database.getConnection().createStatement();\n");
			sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery(sql);\n");
			sb.append(indent).append(TAB).append(TAB).append("while (result.next()) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append("callback.objectFetched(extract(result));\n");
			sb.append(indent).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append("}\n");

			for (Member childMember : type.getMembers()) {
				if (childMember.hasParent()) {
					sb.append("\n");
					usedModels.add("ID");
					sb.append(indent).append(TAB).append("public static Collection<")
							.append(type.getFullyQualifiedName()).append("> ")
							.append(Helper.toCamelCase("fetch_by_" + childMember.getName()))
							.append("(ID id) throws SQLException {\n");
					sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
							.append(" where ").append(childMember.getSqlName()).append(" = ?\";\n");
					sb.append(indent).append(TAB).append(TAB)
							.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
					sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,id.getId());\n");
					sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
					sb.append(indent).append(TAB).append(TAB).append("Collection<").append(type.getFullyQualifiedName())
							.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
					sb.append(indent).append(TAB).append(TAB).append("while (result.next()) {\n");
					sb.append(indent).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
							.append(".add(extract(result));\n");
					sb.append(indent).append(TAB).append(TAB).append("}\n");
					sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName())
							.append(";\n");
					sb.append(indent).append(TAB).append("}\n");
				} else if (childMember.isUnique()) {
					sb.append("\n");
					usedModels.add("ID");
					childMember.getTypeName();
					sb.append(indent).append(TAB).append("public static ").append(type.getFullyQualifiedName())
							.append(" ").append(Helper.toCamelCase("fetch_by_" + childMember.getName())).append("(")
							.append(childMember.getTypeName()).append(" ")
							.append(Helper.toCamelCase(childMember.getName())).append(") throws SQLException {\n");
					sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
							.append(" where ").append(childMember.getSqlName()).append(" = ?\";\n");
					sb.append(indent).append(TAB).append(TAB)
							.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
					if (childMember.getTypeEntity() == BuiltInType.Boolean) {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setBoolean(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					} else if (childMember.getTypeEntity() == BuiltInType.Integer) {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					} else {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setString(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					}
					sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
					sb.append(indent).append(TAB).append(TAB).append("if (result.next()) {\n");
					sb.append(indent).append(TAB).append(TAB).append(TAB).append("return extract(result);\n");
					sb.append(indent).append(TAB).append(TAB).append("} else {\n");
					sb.append(indent).append(TAB).append(TAB).append(TAB).append("return null;\n");
					sb.append(indent).append(TAB).append(TAB).append("}\n");
					sb.append(indent).append(TAB).append("}\n");
				} else if (childMember.isKey()) {
					sb.append("\n");
					sb.append(indent).append(TAB).append("public static Collection<")
							.append(type.getFullyQualifiedName()).append("> ")
							.append(Helper.toCamelCase("fetch_by_" + childMember.getName())).append("(")
							.append(childMember.getTypeName()).append(" ")
							.append(Helper.toCamelCase(childMember.getName())).append(") throws SQLException {\n");
					sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
							.append(" where ").append(childMember.getSqlName()).append(" = ?\";\n");
					sb.append(indent).append(TAB).append(TAB)
							.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
					if (childMember.getTypeEntity() == BuiltInType.Boolean) {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setBoolean(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					} else if (childMember.getTypeEntity() == BuiltInType.Integer) {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					} else {
						sb.append(indent).append(TAB).append(TAB).append("stmt.setString(1,")
								.append(Helper.toCamelCase(childMember.getName())).append(");\n");
					}
					sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
					sb.append(indent).append(TAB).append(TAB).append("Collection<").append(type.getFullyQualifiedName())
							.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
					sb.append(indent).append(TAB).append(TAB).append("while (result.next()) {\n");
					sb.append(indent).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
							.append(".add(extract(result));\n");
					sb.append(indent).append(TAB).append(TAB).append("}\n");
					sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName())
							.append(";\n");
					sb.append(indent).append(TAB).append("}\n");
				}
			}

			if (type.containsSearchables()) {
				sb.append("\n");
				sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
						.append("> search(String expr) throws SQLException {\n");
				sb.append(indent).append(TAB).append(TAB).append("return search(expr,true,0);\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append("\n");
				sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
						.append("> search(String expr,boolean asc) throws SQLException {\n");
				sb.append(indent).append(TAB).append(TAB).append("return search(expr,asc,0);\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append("\n");
				sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
						.append("> search(String expr,boolean asc,int limit) throws SQLException {\n");
				String where = "";
				int searchableCount = 0;
				for (Member childMember : type.getMembers()) {
					if (childMember.isSearchable()) {
						where += (where.isEmpty() ? " where" : " or") + " " + childMember.getName() + " like ?";
						searchableCount++;
					}
				}
				sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
						.append(where)
						.append(" order by id \"+(asc ? \"asc\" : \"desc\")+(limit > 0 ? \" limit \"+limit : \"\");\n");
				sb.append(indent).append(TAB).append(TAB)
						.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
				for (int i = 0; i < searchableCount; i++) {
					sb.append(indent).append(TAB).append(TAB).append("stmt.setString(").append(i + 1)
							.append(",'%'+expr+'%');\n");
				}
				sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
				sb.append(indent).append(TAB).append(TAB).append("Collection<").append(type.getFullyQualifiedName())
						.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
				sb.append(indent).append(TAB).append(TAB).append("while (result.next()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
						.append(".add(extract(result));\n");
				sb.append(indent).append(TAB).append(TAB).append("}\n");
				sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName())
						.append(";\n");
				sb.append(indent).append(TAB).append("}\n");
			}
		} else {
			sb.append("\n");
			usedModels.add(Helper.topMostClass(((Type) member.getParentEntity()).getFullyQualifiedName()));
			sb.append(indent).append(TAB).append("public static Collection<").append(type.getFullyQualifiedName())
					.append("> fetchByParent(").append(((Type) member.getParentEntity()).getFullyQualifiedName())
					.append(" ").append(((Type) member.getParentEntity()).getInstanceName())
					.append(") throws SQLException {\n");
			sb.append(indent).append(TAB).append(TAB).append("String sql = \"select * from ").append(tableName)
					.append(" where parent_id = ?\";\n");
			sb.append(indent).append(TAB).append(TAB)
					.append("PreparedStatement stmt = Database.getConnection().prepareStatement(sql);\n");
			sb.append(indent).append(TAB).append(TAB).append("stmt.setInt(1,")
					.append(((Type) member.getParentEntity()).getInstanceName()).append(".getId());\n");
			sb.append(indent).append(TAB).append(TAB).append("ResultSet result = stmt.executeQuery();\n");
			sb.append(indent).append(TAB).append(TAB).append("Collection<").append(type.getFullyQualifiedName())
					.append("> ").append(type.getCollectionName()).append(" = new ArrayList<>();\n");
			sb.append(indent).append(TAB).append(TAB).append("while (result.next()) {\n");
			sb.append(indent).append(TAB).append(TAB).append(TAB).append(type.getCollectionName())
					.append(".add(extract(result));\n");
			sb.append(indent).append(TAB).append(TAB).append("}\n");
			sb.append(indent).append(TAB).append(TAB).append("return ").append(type.getCollectionName()).append(";\n");
			sb.append(indent).append(TAB).append("}\n");
		}

		if (member.getTypeEntity() instanceof Type) {
			for (Member childMember : ((Type) member.getTypeEntity()).getMembers()) {
				if (childMember.isCollection()) {
					sb.append(generateMemberDal(childMember, tablePrefix + member.getName() + "_", indent + TAB,
							usedModels, usedDals));
				}
			}
		}

		sb.append(indent).append("}\n");

		return sb.toString();
	}

	private String generateCreateObjectFromResult(Type type, String indent, Set<String> usedDals) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent).append(type.getFullyQualifiedName()).append(" ").append(type.getInstanceName())
				.append(" = new ").append(type.getFullyQualifiedName()).append("();\n");
		sb.append(indent).append(type.getInstanceName()).append(".setId(result.getInt(\"id\"));\n");
		for (Member member : type.getMembers()) {
			TypeEntity memberTypeEntity = member.getTypeEntity();
			if (member.isCollection()) { /* Type */
				sb.append(indent).append(type.getInstanceName()).append(".").append(member.getSetterMethodName())
						.append("(").append(Helper.classOnly(member.getFullyQualifiedDalName()))
						.append(".fetchByParent(").append(type.getInstanceName()).append("));\n");
			} else if (memberTypeEntity instanceof Type) {
				usedDals.add(Helper.collectionPathToDal(member.getParent()));
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("int value = result.getInt(\"").append(member.getSqlName())
						.append("\");\n");
				sb.append(indent).append(TAB).append("if (!result.wasNull()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(")
						.append(Helper.collectionPathToDal(member.getParent())).append(".fetchById(value));\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append(indent).append("}\n");
			} else if (memberTypeEntity instanceof Enumeration) {
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("String value = result.getString(\"").append(member.getSqlName())
						.append("\");\n");
				sb.append(indent).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(value != null ? ")
						.append(memberTypeEntity.getFullyQualifiedName()).append(".valueOf(value) : null);\n");
				sb.append(indent).append("}\n");
			} else if (memberTypeEntity instanceof Lookup) {
				sb.append(indent).append(type.getInstanceName()).append(".").append(member.getSetterMethodName())
						.append("(result.getString(\"").append(member.getName()).append("\"));\n");
			} else if (memberTypeEntity == BuiltInType.String) {
				sb.append(indent).append(type.getInstanceName()).append(".").append(member.getSetterMethodName())
						.append("(result.getString(\"").append(member.getName()).append("\"));\n");
			} else if (memberTypeEntity == BuiltInType.Integer) {
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("int value = result.getInt(\"").append(member.getSqlName())
						.append("\");\n");
				sb.append(indent).append(TAB).append("if (!result.wasNull()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(value);\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append(indent).append("}\n");
			} else if (memberTypeEntity == BuiltInType.Double) {
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("double value = result.getDouble(\"").append(member.getSqlName())
						.append("\");\n");
				sb.append(indent).append(TAB).append("if (!result.wasNull()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(value);\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append(indent).append("}\n");
			} else if (memberTypeEntity == BuiltInType.Boolean) {
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("boolean value = result.getBoolean(\"").append(member.getSqlName())
						.append("\");\n");
				sb.append(indent).append(TAB).append("if (!result.wasNull()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(value);\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append(indent).append("}\n");
			} else if (memberTypeEntity == BuiltInType.Date) {
				sb.append(indent).append("{\n");
				sb.append(indent).append(TAB).append("java.sql.Timestamp value = result.getTimestamp(\"")
						.append(member.getSqlName()).append("\");\n");
				sb.append(indent).append(TAB).append("if (!result.wasNull()) {\n");
				sb.append(indent).append(TAB).append(TAB).append(type.getInstanceName()).append(".")
						.append(member.getSetterMethodName()).append("(new Date(value.getTime()));\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append(indent).append("}\n");
			}
		}
		if (type.isSlave()) {
			sb.append(indent).append(type.getInstanceName()).append(".setParentId(result.getInt(\"parent_id\"));\n");
		}
		sb.append(indent).append("return ").append(type.getInstanceName()).append(";\n");
		return sb.toString();
	}

	private String generateInsertStatement(String tableName, Member member) {
		Type type = (Type) member.getTypeEntity();
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tableName).append(" ( ");
		int totalTableFields = 0;
		{
			for (Member childMember : type.getMembers()) {
				if (!childMember.isCollection()) {
					if (totalTableFields > 0) {
						sb.append(", ");
					}
					sb.append(childMember.getSqlName());
					totalTableFields++;
				}
			}
			if (type.isSlave()) {
				if (totalTableFields > 0) {
					sb.append(", ");
				}
				sb.append("parent_id");
				totalTableFields++;
			}
		}
		sb.append(" ) values ( ");
		for (int i = 0; i < totalTableFields; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append("?");
		}
		sb.append(" )");
		return sb.toString();
	}

	private int appendFieldValueAssignments(StringBuilder sb, String indent, Type type, Set<String> usedDals) {
		int count = 0;
		for (Member member : type.getMembers()) {
			if (!member.isCollection()) {
				count++;
				TypeEntity typeEntity = member.getTypeEntity();
				if (typeEntity instanceof Enumeration) {
					sb.append(indent).append("stmt.setString(").append(count).append(",").append(type.getInstanceName())
							.append(".").append(member.getGetterMethodName()).append("() != null ? ")
							.append(type.getInstanceName()).append(".").append(member.getGetterMethodName())
							.append("().name() : null);\n");
				} else if (typeEntity instanceof Lookup) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					usedDals.add(((Lookup) member.getTypeEntity()).getFullyQualifiedDalName());
					sb.append(indent).append(TAB).append("stmt.setString(").append(count).append(",")
							.append(((Lookup) member.getTypeEntity()).getDalClassName())
							.append(".getInstance().insertIgnore(").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("()));\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.VARCHAR);\n");
					sb.append(indent).append("}\n");
				} else if (typeEntity == BuiltInType.String) {
					sb.append(indent).append("stmt.setString(").append(count).append(",").append(type.getInstanceName())
							.append(".").append(member.getGetterMethodName()).append("());\n");
				} else if (typeEntity == BuiltInType.Double) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					sb.append(indent).append(TAB).append("stmt.setDouble(").append(count).append(",")
							.append(type.getInstanceName()).append(".").append(member.getGetterMethodName())
							.append("());\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.REAL);\n");
					sb.append(indent).append("}\n");
				} else if (typeEntity == BuiltInType.Integer) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					sb.append(indent).append(TAB).append("stmt.setInt(").append(count).append(",")
							.append(type.getInstanceName()).append(".").append(member.getGetterMethodName())
							.append("());\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.INTEGER);\n");
					sb.append(indent).append("}\n");
				} else if (typeEntity == BuiltInType.Boolean) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					sb.append(indent).append(TAB).append("stmt.setBoolean(").append(count).append(",")
							.append(type.getInstanceName()).append(".").append(member.getGetterMethodName())
							.append("());\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.BOOLEAN);\n");
					sb.append(indent).append("}\n");
				} else if (typeEntity == BuiltInType.Date) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					sb.append(indent).append(TAB).append("stmt.setTimestamp(").append(count)
							.append(",new java.sql.Timestamp(").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("().getTime()));\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.TIMESTAMP);\n");
					sb.append(indent).append("}\n");
				} else if (typeEntity instanceof Type) {
					sb.append(indent).append("if (").append(type.getInstanceName()).append(".")
							.append(member.getGetterMethodName()).append("() != null) {\n");
					sb.append(indent).append(TAB).append("stmt.setInt(").append(count).append(",")
							.append(type.getInstanceName()).append(".").append(member.getGetterMethodName())
							.append("().getId());\n");
					sb.append(indent).append("} else {\n");
					sb.append(indent).append(TAB).append("stmt.setNull(").append(count)
							.append(",java.sql.Types.INTEGER);\n");
					sb.append(indent).append("}\n");
				}
			}
		}
		return count;
	}

	private String generateUpdateStatement(String tableName, Member member) {
		Type type = (Type) member.getTypeEntity();
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(tableName).append(" set ");
		{
			int i = 0;
			for (Member childMember : type.getMembers()) {
				if (!childMember.isCollection()) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(childMember.getSqlName()).append(" = ?");
					i++;
				}
			}
		}
		sb.append(" where id = ?");
		return sb.toString();
	}

	private void generateTemplatedClassFiles() throws Exception {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("[DAL_PACKAGE]", dalPackageName);
		generateTemplatedClassToFile("Database", replacements);
		generateTemplatedClassToFile("Callback", replacements);
		generateTemplatedClassToFile("LookupDal", replacements);
		generateTemplatedClassToFile("DALHelper", replacements);
	}

	private String generateTemplatedClass(String templateName, Map<String, String> replacements) throws Exception {
		String template = readFile("src/main/resources/templates/" + templateName + ".template");
		for (String target : replacements.keySet()) {
			template = template.replace(target, replacements.get(target));
		}
		return template;
	}

	private void generateTemplatedClassToFile(String templateName, Map<String, String> replacements) throws Exception {
		generateTemplatedClassToFile(templateName, replacements, templateName);
	}

	private void generateTemplatedClassToFile(String templateName, Map<String, String> replacements, String fileName)
			throws Exception {
		new File(dalPath).mkdirs();
		try (PrintWriter writer = new PrintWriter(dalPath + "/" + fileName + ".java", "UTF-8")) {
			writer.write(generateTemplatedClass(templateName, replacements));
			writer.close();
		}
	}

	private static String readFile(String path) throws IOException {
		return Charset.forName("UTF-8").decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(path)))).toString();
	}

	private void resetDalPath() {
		File dalsFile = new File(dalPath);
		if (dalsFile.exists()) {
			Helper.deleteDirectoryAndContents(dalsFile);
		}
		dalsFile.mkdirs();
	}
}
