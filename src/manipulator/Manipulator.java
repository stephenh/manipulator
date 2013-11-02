package manipulator;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import manipulator.types.EvaluatedType;

public class Manipulator {

  public static void main(String[] args) throws Exception {
    File base = new File("/home/stephen/other/gwt/user/src");
    List<File> scan = new ArrayList<>();
    scan.add(new File(base, "/com/google/gwt/dom/client"));
    scan.add(new File(base, "/com/google/gwt/user/client/ui"));
    scan.add(new File(base, "/com/google/gwt/user/datepicker/client"));
    for (File dir : scan) {
      for (File victim : FileUtils.listFiles(dir, new String[] { "java" }, false)) {
        if (victim.getName().contains("IsWidget") || victim.getName().contains("IsRenderable") || victim.getName().contains("IsTreeItem")) {
          continue;
        }
        if (!victim.getName().startsWith("Is")) {
          continue;
        }
        System.out.println("Updating " + victim.getName());
        EvaluatedType et = new EvaluatedType(victim);
        String boilerPlatedEdition = et.getJavadocBoilerPlatedEdition();
        FileUtils.writeStringToFile(victim, boilerPlatedEdition);
      }
    }
  }

}
