package com.sqisland.gce2retrofit

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

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
        classMap = Generator.readStringToStringMap(new FileReader(classMapFile));
      }

      String methodTypesString = null;
      File methodTypesFile = new File(dir, "methods.csv");
      if (methodTypesFile.isFile()) {
        methodTypesString = new String(
            java.nio.file.Files.readAllBytes(methodTypesFile.toPath()), "UTF8");
        methodTypesString = methodTypesString.replace("\n", "").replace("\r", "");
      }
      EnumSet<Generator.MethodType> methodTypes = Generator.getMethods(methodTypesString);

      Map<String, String> packageMap = null;
      File packageMapFile = new File(dir, "packagemap.tsv");
      if (packageMapFile.isFile()) {
        packageMap = Generator.readStringToStringMap(new FileReader(packageMapFile));
      }

      Map<String, List<AnnotationType>> annotationMap = null;
      File annotationMapFile = new File(dir, "room.json");
      if (annotationMapFile.isFile()) {
        annotationMap = Generator.readAnnotationMap(new FileReader(annotationMapFile));
      }

      Generator.generate(reader, factory, classMap, methodTypes, packageMap, annotationMap);
    }
  }
}