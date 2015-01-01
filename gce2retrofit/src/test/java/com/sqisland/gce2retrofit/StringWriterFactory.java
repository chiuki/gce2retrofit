package com.sqisland.gce2retrofit;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

public class StringWriterFactory implements WriterFactory {
  private final HashMap<String, StringWriter> writers = new HashMap<String, StringWriter>();

  public Writer getWriter(String path) throws IOException {
    StringWriter writer = new StringWriter();
    writers.put(path, writer);
    return writer;
  }

  public String getString(String path) {
    return writers.containsKey(path) ? writers.get(path).toString() : null;
  }

  public int getCount() {
    return writers.size();
  }
}