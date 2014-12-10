package com.sqisland.gce2retrofit;

public class PropertyType {
  public String type;
  public String format;
  public String $ref;
  public PropertyType items;

  public String toJavaType() {
    if ($ref != null) {
      return $ref;
    }

    if ("string".equals(type)) {
      return "String";
    }

    if ("boolean".equals(type)) {
      return "boolean";
    }

    if ("integer".equals(type)) {
      return "int";
    }

    if ("number".equals(type)) {
      if ("double".equals(format)) {
        return "double";
      }
      if ("float".equals(format)) {
        return "float";
      }
      if ("int32".equals(format)) {
        return "int";
      }
    }

    if ("array".equals(type)) {
      String itemsType = items.toJavaType();
      return "List<" + itemsType + ">";
    }

    return "Object";
  }
}