package manipulator;

import java.io.File;

import manipulator.types.EvaluatedType;

public class Manipulator {

	public static void main(String[] args) throws Exception {
    // for (File victim : FileUtils.listFiles(new File("victims/input"), new String[] { "java" },
    // true)) {
    // System.err.printf("%s\n", victim.getCanonicalPath());
    // EvaluatedType et = new EvaluatedType(victim);
    // String boilerPlatedEdition = et.getJavadocBoilerPlatedEdition();
    // System.out.println(boilerPlatedEdition);
    // String outputPath = et.filePath.replace("victims/input", "victims/output");
    // FileUtils.writeStringToFile(new File(outputPath), boilerPlatedEdition, false);
    // }
	  
    File victim =
        new File("/home/stephen/other/gwt/user/src/com/google/gwt/dom/client/IsElement.java");
    EvaluatedType et = new EvaluatedType(victim);
    String boilerPlatedEdition = et.getJavadocBoilerPlatedEdition();
    System.out.println(boilerPlatedEdition);
    // FileUtils.writeStringToFile(victim, boilerPlatedEdition);
	}


}
