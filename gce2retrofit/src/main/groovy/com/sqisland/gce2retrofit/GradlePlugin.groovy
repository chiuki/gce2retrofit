package com.sqisland.gce2retrofit;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryPlugin;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.plugins.JavaPlugin;

public class GradlePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    def hasAndroidApp = project.plugins.hasPlugin AppPlugin
    def hasAndroidLib = project.plugins.hasPlugin LibraryPlugin
    def hasJava = project.plugins.hasPlugin JavaPlugin
    if (!hasAndroidApp && !hasAndroidLib && !hasJava) {
      throw new IllegalStateException("'android' or 'android-library' or 'java' plugin required.")
    }

    if (hasJava) {
      def task = project.tasks.create("gce2retrofit-java", GradleTask)
      task.outputDir = new File("${project.buildDir}/generated/source/gce2retrofit")
      project.compileJava.dependsOn task
      project.compileJava.source += task.outputs.files
      return
    }

    def variants
    if (hasAndroidApp) {
      variants = project.android.applicationVariants
    } else {
      variants = project.android.libraryVariants
    }

    variants.all { variant ->
      def task = project.tasks.create("gce2retrofit-${variant.name}", GradleTask)
      task.outputDir = new File("${project.buildDir}/generated/source/gce2retrofit/${variant.name}")
      variant.javaCompile.dependsOn task
      variant.registerJavaGeneratingTask task, task.outputDir
    }
  }
}