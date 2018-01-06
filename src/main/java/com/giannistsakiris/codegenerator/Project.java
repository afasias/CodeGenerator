package com.giannistsakiris.codegenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Project extends BaseEntity implements ParentEntity {

	private String sourceDirectory;
	private String packageName;
	protected List<Enumeration> enumerations = new ArrayList<>();
	protected List<Lookup> lookups = new ArrayList<>();
	protected List<Type> types = new ArrayList<>();
	protected List<Member> members = new ArrayList<>();
	protected List<DbKey> dbkeys = new ArrayList<>();

	public Project(String path) throws Exception {
		File file = new File(path);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		Element projectElement = getChildByName(doc, "project");
		this.setName(projectElement.getAttribute("name"));
		this.setSourceDirectory(projectElement.getAttribute("src"));
		this.setPackageName(projectElement.getAttribute("package"));
		loadChildrenFromElement(this, projectElement);
	}

	private void loadChildrenFromElement(ParentEntity loader, Element element) {
		for (Element lookupElement : getChildrenByName(element, "lookup")) {
			Lookup lookup = new Lookup();
			loader.getLookups().add(lookup);
			lookup.setName(lookupElement.getAttribute("name"));
			lookup.setTableName(lookupElement.getAttribute("table"));
			lookup.setLength(Integer.parseInt(lookupElement.getAttribute("length")));
			lookup.setParent(loader);
		}
		;
		for (Element enumerationElement : getChildrenByName(element, "enumeration")) {
			Enumeration enumeration = new Enumeration();
			loader.getEnumerations().add(enumeration);
			enumeration.setName(enumerationElement.getAttribute("name"));
			for (Element valueElement : getChildrenByName(enumerationElement, "value")) {
				enumeration.getValues().add(valueElement.getTextContent());
			}
			enumeration.setParent(loader);
		}
		;
		for (Element typeElement : getChildrenByName(element, "type")) {
			Type type = new Type();
			loader.getTypes().add(type);
			type.setName(typeElement.getAttribute("name"));
			type.setTableName(typeElement.getAttribute("table"));
			type.setParent(loader);
			type.setSlave(typeElement.getAttribute("slave").equals("true"));
			loadChildrenFromElement(type, typeElement);
		}
		for (Element memberElement : getChildrenByName(element, "member")) {
			Member member = new Member();
			loader.getMembers().add(member);
			member.setName(memberElement.getAttribute("name"));
			TypeEntity memberTypeEntity = loader.findTypeEntityByName(memberElement.getAttribute("type"));
			member.setTypeEntity(memberTypeEntity);
			member.setSearchable(memberElement.getAttribute("searchable").equals("true"));
			member.setCollection(memberElement.getAttribute("collection").equals("true"));
			member.setUnique(memberElement.getAttribute("unique").equals("true"));
			member.setKey(memberElement.getAttribute("key").equals("true"));
			member.setLabel(memberElement.getAttribute("label").equals("true"));
			member.setParent(memberElement.getAttribute("parent"));
			member.setLength(!memberElement.getAttribute("length").isEmpty()
					? Integer.valueOf(memberElement.getAttribute("length")) : null);
			member.setParentEntity(loader);
		}
		;
		for (Element dbKeyElement : getChildrenByName(element, "dbkey")) {
			DbKey dbkey = new DbKey();
			loader.getDbKeys().add(dbkey);
			dbkey.setField(dbKeyElement.getAttribute("field"));
			dbkey.setUnique(dbKeyElement.getAttribute("unique").equals("true"));
		}
		;
	}

	private static List<Element> getChildrenByName(Node parentNode, String name) {
		NodeList nodeList = parentNode.getChildNodes();
		List<Element> elements = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
				elements.add((Element) node);
			}
		}
		return elements;
	}

	private static Element getChildByName(Node parentNode, String name) {
		NodeList nodeList = parentNode.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
				return (Element) node;
			}
		}
		return null;
	}

	@Override
	public List<Enumeration> getEnumerations() {
		return enumerations;
	}

	@Override
	public void setEnumerations(List<Enumeration> enumerations) {
		this.enumerations = enumerations;
	}

	@Override
	public List<Lookup> getLookups() {
		return lookups;
	}

	@Override
	public void setLookups(List<Lookup> lookups) {
		this.lookups = lookups;
	}

	@Override
	public List<Type> getTypes() {
		return types;
	}

	@Override
	public void setTypes(List<Type> types) {
		this.types = types;
	}

	private Map<String, TypeEntity> getTypeEntitiesMap() {
		Map<String, TypeEntity> map = new HashMap<>();
		for (Lookup lookup : lookups) {
			map.put(lookup.getName(), lookup);
		}
		for (Enumeration enumeration : enumerations) {
			map.put(enumeration.getName(), enumeration);
		}
		for (Type type : types) {
			map.put(type.getName(), type);
		}
		return map;
	}

	@Override
	public TypeEntity findTypeEntityByName(String name) {
		Map<String, TypeEntity> map = getTypeEntitiesMap();
		if (map.containsKey(name)) {
			return map.get(name);
		} else {
			return BuiltInType.valueOf(name);
		}
	}

	@Override
	public List<TypeEntity> getAllTypeEntities() {
		List<TypeEntity> typeEntities = new ArrayList<>();
		typeEntities.addAll(enumerations);
		typeEntities.addAll(lookups);
		typeEntities.addAll(types);
		return typeEntities;
	}

	@Override
	public List<Member> getMembers() {
		return members;
	}

	@Override
	public void setMembers(List<Member> members) {
		this.members = members;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public List<DbKey> getDbKeys() {
		return dbkeys;
	}
}
