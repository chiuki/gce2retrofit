package com.sqisland.gce2retrofit;

import java.io.IOException;
import java.io.Writer;

public interface WriterFactory {
  public Writer getWriter(String path) throws IOException;
}