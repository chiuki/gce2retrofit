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
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

public class GradleTask extends DefaultTask {
  @InputDirectory
  File inputDir = new File(project.getProjectDir(), "src/main/gce2retrofit")
  @OutputDirectory
  File outputDir

  @TaskAction
  public void execute(IncrementalTaskInputs inputs) {
    WriterFactory factory = new FileWriterFactory(outputDir);

    for (File dir : inputDir.listFiles()) {
      if (dir.name.startsWith(".")) {
        continue;
      }
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