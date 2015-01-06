package com.sqisland.gce2retrofit;

import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class GradlePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getTasks().create("gce2retrofit", GradleTask.class);
  }
}