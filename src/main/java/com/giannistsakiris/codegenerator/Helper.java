package com.giannistsakiris.codegenerator;

import java.io.File;

public class Helper {

	public static String upperCaseFirstLetter(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

	public static String lowerCaseFirstLetter(String string) {
		return Character.toLowerCase(string.charAt(0)) + string.substring(1);
	}

	public static String toCamelCase(String snakeCaseString) {
		StringBuilder sb = new StringBuilder();
		for (String word : snakeCaseString./* toLowerCase(). */split("_")) {
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		return sb.toString();
	}

	public static String toSnakeCase(String camelCaseString) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < camelCaseString.length(); i++) {
			char ch = camelCaseString.charAt(i);
			if (i > 0 && Character.isUpperCase(ch)) {
				sb.append("_");
			}
			sb.append(Character.toLowerCase(ch));
		}
		return sb.toString();
	}

	public static String topMostClass(String fullyQualifiedName) {
		int index = fullyQualifiedName.indexOf('.');
		if (index == -1) {
			return fullyQualifiedName;
		} else {
			return fullyQualifiedName.substring(0, index);
		}
	}

	static String classOnly(String fullyQualifiedDalName) {
		String[] parts = fullyQualifiedDalName.split("\\.");
		String classOnly = "";
		for (int i = parts.length - 1; i >= 0; i--) {
			if (Character.isUpperCase(parts[i].charAt(0))) {
				classOnly = parts[i] + (classOnly.isEmpty() ? "" : ".") + classOnly;
			}
		}
		return classOnly;
	}

	static String packageOnly(String dalPackageName) {
		int index = dalPackageName.lastIndexOf('.');
		if (index == -1) {
			return dalPackageName;
		} else {
			return dalPackageName.substring(0, index);
		}
	}

	static String collectionPathToDal(String parent) {
		String dal = "";
		for (String collectionName : parent.split("_")) {
			dal += (dal.isEmpty() ? "" : ".") + Helper.upperCaseFirstLetter(Helper.toCamelCase(collectionName));
		}
		return dal;
	}

	static void deleteDirectoryAndContents(File file) {
		for (File subfile : file.listFiles()) {
			if (subfile.isDirectory()) {
				deleteDirectoryAndContents(subfile);
			} else {
				subfile.delete();
			}
		}
		file.delete();
	}
}
