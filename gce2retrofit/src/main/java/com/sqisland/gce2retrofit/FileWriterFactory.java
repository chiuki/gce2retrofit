package com.sqisland.gce2retrofit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class FileWriterFactory implements WriterFactory {
  private final File parentDir;

  public FileWriterFactory(File parentDir) {
    this.parentDir = parentDir;
  }

  @Override
  public Writer getWriter(String path) throws IOException {
    File fullPath = new File(parentDir,  path);
    File dir = new File(fullPath.getParent());
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return new FileWriter(fullPath);
  }
}