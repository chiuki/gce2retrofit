package com.sqisland.gce2retrofit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

public class GradleTask extends DefaultTask {
  @TaskAction
  public void hello() throws IOException, URISyntaxException {
    Project project = getProject();

    String outputDir = project.getBuildDir() + "/generated/source/gce2retrofit";
    WriterFactory factory = new FileWriterFactory(outputDir);

    File configDir = new File(project.getProjectDir(), "/gce2retrofit");
    for (File dir : configDir.listFiles()) {
      File discoveryFile = new File(dir, "discovery.json");
      Reader reader = new FileReader(discoveryFile);

      Map<String, String> classMap = null;
      File classMapFile = new File(dir, "classmap.tsv");
      if (classMapFile.isFile()) {
        classMap = Generator.readClassMap(new FileReader(classMapFile));
      }

      String methodTypesString = null;
      File methodTypesFile = new File(dir, "methods.csv");
      if (methodTypesFile.isFile()) {
        methodTypesString = new String(
            java.nio.file.Files.readAllBytes(methodTypesFile.toPath()), "UTF8");
        methodTypesString = methodTypesString.replace("\n", "").replace("\r", "");
      }
      EnumSet<Generator.MethodType> methodTypes = Generator.getMethods(methodTypesString);

      Generator.generate(reader, factory, classMap, methodTypes);
    }
  }
}