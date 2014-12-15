package com.sqisland.gce2retrofit;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class StringUtil {
  public static String primitiveToObject(String type) {
    if ("boolean".equals(type)) {
      return "Boolean";
    }

    if ("int".equals(type)) {
      return "Integer";
    }

    if ("float".equals(type)) {
      return "Float";
    }

    if ("double".equals(type)) {
      return "Double";
    }

    return type;
  }

  public static String getPackageName(String baseUrl)
      throws URISyntaxException {
    if (baseUrl == null) {
      return null;
    }

    URI uri = new URI(baseUrl);
    String domain = uri.getHost();
    if (domain == null) {
      return null;
    }

    String[] parts = domain.split("\\.");

    StringBuffer buf = new StringBuffer();
    for (int i = parts.length - 1; i >= 0; --i) {
      // Package name cannot have dashes. Replace with underscores.
      String part = parts[i].replace('-', '_');

      // Package name cannot start with a digit. Prepend with an underscore.
      if (part.charAt(0) >= '0' && part.charAt(0) <= '9') {
        buf.append('_');
      }

      buf.append(part);
      if (i != 0) {
        buf.append('.');
      }
    }
    return buf.toString();
  }

  public static String getPath(String packageName, String fileName) {
    if (packageName == null || fileName == null) {
      return null;
    }
    return packageName.replace(".", "/") + File.separator + fileName;
  }

  private static final String[] RESERVED_WORDS = {
      "abstract", "continue", "for", "new", "switch",
      "assert", "default", "goto", "package", "synchronized",
      "boolean", "do", "if", "private", "this",
      "break", "double", "implements", "protected", "throw",
      "byte", "else", "import", "public", "throws",
      "case", "enum", "instanceof", "return", "transient",
      "catch", "extends", "int", "short", "try",
      "char", "final", "interface", "static", "void",
      "class", "finally", "long", "strictfp", "volatile",
      "const", "float", "native", "super", "while"
  };
  public static boolean isReservedWord(String name) {
    if (name == null || name.length() == 0) {
      return false;
    }
    for (String word : RESERVED_WORDS) {
      if (word.equals(name)) {
        return true;
      }
    }
    return false;
  }
}