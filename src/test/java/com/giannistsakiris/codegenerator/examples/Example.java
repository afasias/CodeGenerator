package com.giannistsakiris.codegenerator.examples;

import com.giannistsakiris.codegenerator.DalGenerator;
import com.giannistsakiris.codegenerator.ModelGenerator;
import com.giannistsakiris.codegenerator.Project;
import com.giannistsakiris.codegenerator.SchemaGenerator;

public class Example {

	public static void main(String[] args) throws Exception {
		Project project = new Project("src/test/resources/example.xml");
		SchemaGenerator.generateSchema(project);
		ModelGenerator.generateModels(project);
		DalGenerator.generateDals(project);
	}
}
