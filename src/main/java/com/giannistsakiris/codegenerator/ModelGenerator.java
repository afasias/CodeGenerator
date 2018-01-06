package com.giannistsakiris.codegenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ModelGenerator {

	private final static String TAB = "    ";

	private final Project project;
	private final String modelsPackageName;
	private final String modelsPath;

	private ModelGenerator(Project project) {
		this.project = project;
		this.modelsPackageName = project.getPackageName() + ".model";
		this.modelsPath = project.getSourceDirectory() + modelsPackageName.replace('.', '/');
	}

	public static void generateModels(Project project) throws Exception {
		new ModelGenerator(project).run();
	}

	private void run() throws Exception {
		System.out.println("Generating model classes");
		resetModelsPath();
		generateTopLevelModels(project);
		generateTemplatedClasses();
	}

	private void generateTopLevelModels(ParentEntity parent) throws Exception {
		for (Enumeration enumeration : parent.getEnumerations()) {
			generateTopLevelEnumeration(enumeration);
		}
		for (Type type : parent.getTypes()) {
			generateTopLevelType(type);
		}
	}

	private void generateTopLevelEnumeration(Enumeration enumeration) throws Exception {
		try (PrintWriter writer = new PrintWriter(modelsPath + '/' + enumeration.getName() + ".java", "UTF-8")) {
			writer.println("package " + modelsPackageName + ";");
			writer.write(generateEnumeration(enumeration, ""));
			writer.close();
		}
	}

	private void generateTopLevelType(Type type) throws Exception {
		try (PrintWriter writer = new PrintWriter(modelsPath + '/' + type.getName() + ".java", "UTF-8")) {
			writer.println("package " + modelsPackageName + ";");
			if (type.containsCollections() || type.containsDates()) {
				writer.println();
				if (type.containsCollections()) {
					writer.println("import java.util.ArrayList;");
					writer.println("import java.util.Collection;");
				}
				if (type.containsDates()) {
					writer.println("import java.util.Date;");
				}
			}
			writer.write(generateType(type, ""));
			writer.close();
		}
	}

	private String generateEnumeration(Enumeration enumeration, String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append(indent).append(indent.isEmpty() ? "" : "static ").append("public enum ").append(enumeration.getName())
				.append(" {\n");
		for (String value : enumeration.getValues()) {
			sb.append(indent).append(TAB).append(value).append(",\n");
		}
		sb.append(indent).append("}\n");
		return sb.toString();
	}

	private String generateType(Type type, String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append(indent).append(indent.isEmpty() ? "" : "static ").append("public class ").append(type.getName())
				.append(" extends ").append(type.isSlave() ? "ChildModel" : "BaseModel").append(" {\n");
		for (Enumeration childEnumeration : type.getEnumerations()) {
			sb.append(generateEnumeration(childEnumeration, indent + TAB));
		}
		for (Type childType : type.getTypes()) {
			sb.append(generateType(childType, indent + TAB));
		}
		if (!type.getMembers().isEmpty()) {
			sb.append("\n");
			for (Member member : type.getMembers()) {
				sb.append(indent).append(TAB).append("protected ").append(member.getTypeName()).append(" ")
						.append(member.getCamelCaseName());
				if (member.isCollection()) {
					sb.append(" = new ArrayList<>()");
				}
				sb.append(";\n");
			}
			for (Member member : type.getMembers()) {
				sb.append("\n");
				sb.append(indent).append(TAB).append("public ").append(member.getTypeName()).append(" ")
						.append(member.getGetterMethodName()).append("() {\n");
				sb.append(indent).append(TAB).append(TAB).append("return ").append(member.getCamelCaseName())
						.append(";\n");
				sb.append(indent).append(TAB).append("}\n");
				sb.append("\n");
				sb.append(indent).append(TAB).append("public void ").append(member.getSetterMethodName()).append("( ")
						.append(member.getTypeName()).append(" ").append(member.getCamelCaseName()).append(" ) {\n");
				sb.append(indent).append(TAB).append(TAB).append("this.").append(member.getCamelCaseName())
						.append(" = ").append(member.getCamelCaseName()).append(";\n");
				sb.append(indent).append(TAB).append("}\n");
			}
		}
		for (Member member : type.getMembers()) {
			if (member.isLabel()) {
				sb.append("\n");
				sb.append(indent).append(TAB).append("@Override\n");
				sb.append(indent).append(TAB).append("public String toString() {\n");
				sb.append(indent).append(TAB).append(TAB).append("return ").append(member.getCamelCaseName())
						.append(";\n");
				sb.append(indent).append(TAB).append("}\n");
				break;
			}
		}
		sb.append(indent).append("}\n");
		return sb.toString();
	}

	private void generateTemplatedClasses() throws Exception {
		generateTemplatedClass("ID");
		generateTemplatedClass("BaseModel");
		generateTemplatedClass("ChildModel");
	}

	private void generateTemplatedClass(String className) throws Exception {
		String lookupItemTemplate = readFile("src/main/resources/templates/" + className + ".template");
		try (PrintWriter writer = new PrintWriter(modelsPath + "/" + className + ".java", "UTF-8")) {
			writer.write(lookupItemTemplate.replace("[MODELS_PACKAGE]", modelsPackageName));
			writer.close();
		}
	}

	private static String readFile(String path) throws IOException {
		return Charset.forName("UTF-8").decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(path)))).toString();
	}

	private void resetModelsPath() {
		File modelsFile = new File(modelsPath);
		if (modelsFile.exists()) {
			Helper.deleteDirectoryAndContents(modelsFile);
		}
		modelsFile.mkdirs();
	}
}
