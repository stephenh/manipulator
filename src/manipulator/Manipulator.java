package manipulator;

import java.io.File;

import manipulator.types.EvaluatedType;

import org.apache.commons.io.FileUtils;

public class Manipulator {

	public static void main(String[] args) throws Exception {
		for (File victim : FileUtils.listFiles(new File("victims/input"), new String[] { "java" }, true)) {
			System.err.printf("%s\n", victim.getCanonicalPath());
			EvaluatedType et = new EvaluatedType(victim);
			String boilerPlatedEdition = et.getJavadocBoilerPlatedEdition();
			System.out.println(boilerPlatedEdition);
			String outputPath = et.filePath.replace("victims/input", "victims/output");
			FileUtils.writeStringToFile(new File(outputPath), boilerPlatedEdition, false);
		}
	}

}
