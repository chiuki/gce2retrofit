package com.sqisland.gce2retrofit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.squareup.javawriter.JavaWriter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.text.WordUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;

public class Generator {
  private static final String OPTION_CLASS_MAP = "classmap";
  private static final String OPTION_METHODS = "methods";
  private static final String OPTION_PACKAGE_MAP = "packagemap";
  private static final String OPTION_ROOM_ANNOTATION_MAP = "room";

  private static Gson gson = new Gson();

  private static final List<String> roomImports = new ArrayList<String>() {
    {
      add("androidx.room.ColumnInfo");
      add("androidx.room.ColumnInfo.Collate");
      add("androidx.room.ColumnInfo.SQLiteTypeAffinity");
      add("androidx.room.Embedded");
      add("androidx.room.Entity");
      add("androidx.room.ForeignKey");
      add("androidx.room.ForeignKey.Action");
      add("androidx.room.Ignore");
      add("androidx.room.Index");
      add("androidx.room.PrimaryKey");
      add("androidx.room.Relation");
      add("androidx.annotation.NonNull");
    }
  };

  public enum MethodType {
    SYNC, ASYNC, REACTIVE, V2
  }

  public static void main(String... args)
      throws IOException, URISyntaxException {
    Options options = getOptions();


    CommandLine cmd = getCommandLine(options, args);
    if (cmd == null) {
      return;
    }

    String[] arguments = cmd.getArgs();
    if (arguments.length != 2) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar gce2retrofit.jar discovery.json output_dir", options);
      System.exit(1);
    }

    String discoveryFile = arguments[0];
    String outputDir = arguments[1];

    Map<String, String> classMap = cmd.hasOption(OPTION_CLASS_MAP)?
        readStringToStringMap(new FileReader(cmd.getOptionValue(OPTION_CLASS_MAP))) : null;

    EnumSet<MethodType> methodTypes = getMethods(cmd.getOptionValue(OPTION_METHODS));

    Map<String, String> packageMap = cmd.hasOption(OPTION_PACKAGE_MAP)?
        readStringToStringMap(new FileReader(cmd.getOptionValue(OPTION_PACKAGE_MAP))) : null;

    Map<String, List<AnnotationType>> roomAnnotationMap = cmd.hasOption(OPTION_ROOM_ANNOTATION_MAP)?
      readAnnotationMap(new FileReader(cmd.getOptionValue(OPTION_ROOM_ANNOTATION_MAP))) : null;

    generate(new FileReader(discoveryFile), new FileWriterFactory(new File(outputDir)),
        classMap, methodTypes, packageMap, roomAnnotationMap);
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(
        OPTION_CLASS_MAP, true, "Map fields to classes. Format: field_name\\tclass_name");
    options.addOption(
        OPTION_METHODS, true,
        "Methods to generate, either sync, async, reactive or v2. Default is to generate sync & async.");
    options.addOption(
        OPTION_PACKAGE_MAP, true, "Map class prefix to package directory. Format: prefix\\tdirectory");
    options.addOption(
        OPTION_ROOM_ANNOTATION_MAP, true, "Map room library annotations to classes/fields.");

    return options;
  }

  private static CommandLine getCommandLine(Options options, String... args) {
    CommandLineParser parser = new BasicParser();
    try {
      CommandLine cmd = parser.parse(options, args);
      return cmd;
    } catch (ParseException e) {
      System.out.println("Unexpected exception:" + e.getMessage());
    }
    return null;
  }

  public static void generate(
      Reader discoveryReader, WriterFactory writerFactory,
      Map<String, String> classMap, EnumSet<MethodType> methodTypes)
      throws IOException, URISyntaxException {
    generate(discoveryReader, writerFactory, classMap, methodTypes, new HashMap<String, String>(), new HashMap<>());
  }

  public static void generate(
      Reader discoveryReader, WriterFactory writerFactory,
      Map<String, String> classMap, EnumSet<MethodType> methodTypes,
      Map<String, String> packageMap,
      Map<String, List<AnnotationType>> roomAnnotationMap)
      throws IOException, URISyntaxException {
    JsonReader jsonReader = new JsonReader(discoveryReader);

    Discovery discovery = gson.fromJson(jsonReader, Discovery.class);

    String packageName = StringUtil.getPackageName(discovery.baseUrl);
    if (packageName == null || packageName.isEmpty()) {
      packageName = StringUtil.getPackageName(discovery.rootUrl);
    }
    String modelPackageName = packageName + ".model";

    for (Entry<String, JsonElement> entry : discovery.schemas.entrySet()) {
      generateModel(
          writerFactory, modelPackageName, entry.getValue().getAsJsonObject(),
          classMap, packageMap, roomAnnotationMap);
    }

    if (discovery.resources != null) {
      generateInterfaceFromResources(
          writerFactory, packageName, "", discovery.resources, methodTypes, packageMap);
    }

    if (discovery.name != null && discovery.methods != null) {
      generateInterface(
          writerFactory, packageName, discovery.name, discovery.methods, methodTypes, packageMap);
    }
  }

  public static Map<String, List<AnnotationType>> readAnnotationMap(Reader reader) throws IOException {
    final Type type = new TypeToken<Map<String, List<AnnotationType>>>(){}.getType();
    final Map<String, List<AnnotationType>> annotationMap = gson.fromJson(reader, type);
    return annotationMap;
  }

  public static Map<String, String> readStringToStringMap(Reader reader) throws IOException {
    Map<String, String> map = new HashMap<>();

    String line;
    BufferedReader bufferedReader = new BufferedReader(reader);
    while ((line = bufferedReader.readLine()) != null) {
      String[] fields = line.split("\t");
      if (fields.length == 2) {
        map.put(fields[0], fields[1]);
      }
    }

    return map;
  }

  public static EnumSet<MethodType> getMethods(String input) {
    EnumSet<MethodType> methodTypes = EnumSet.noneOf(MethodType.class);
    if (input != null) {
      String[] parts = input.split(",");
      for (String part : parts) {
        if ("sync".equals(part) || "both".equals(part)) {
          methodTypes.add(MethodType.SYNC);
        }
        if ("async".equals(part) || "both".equals(part)) {
          methodTypes.add(MethodType.ASYNC);
        }
        if ("reactive".equals(part)) {
          methodTypes.add(MethodType.REACTIVE);
        }
        if ("v2".equals(part)) {
          methodTypes.add(MethodType.V2);
        }
      }
    }
    if (methodTypes.isEmpty()) {
      methodTypes = EnumSet.of(Generator.MethodType.ASYNC, Generator.MethodType.SYNC);
    }
    return methodTypes;
  }

  private static void generateModel(
      WriterFactory writerFactory, String modelPackageName,
      JsonObject schema, Map<String, String> classMap, Map<String, String> packageMap,
      Map<String, List<AnnotationType>> roomAnnotationMap)
      throws IOException {
    String id = schema.get("id").getAsString();

    ClassInfo classInfo = new ClassInfo(modelPackageName, id);
    classInfo.movePackage(packageMap);

    String path = StringUtil.getPath(classInfo.packageName, classInfo.className + ".java");
    Writer writer = writerFactory.getWriter(path);
    JavaWriter javaWriter = new JavaWriter(writer);

    javaWriter.emitPackage(classInfo.packageName);

    if (packageMap != null) {
      ArrayList<String> dirs = new ArrayList<>(packageMap.values());
      Collections.sort(dirs);
      if (!modelPackageName.equals(classInfo.packageName)) {
        javaWriter.emitImports(modelPackageName + ".*");
      }
      for (String dir : dirs) {
        String packageName = modelPackageName + "." + dir;
        if (!packageName.equals(classInfo.packageName)) {
          javaWriter.emitImports(packageName + ".*");
        }
      }
      if (!dirs.isEmpty()) {
        javaWriter.emitEmptyLine();
      }
    }

    if (roomAnnotationMap != null && !roomAnnotationMap.isEmpty()) {
      javaWriter.emitImports(roomImports);
    }

    javaWriter
        .emitImports("com.google.gson.annotations.SerializedName")
        .emitEmptyLine()
        .emitImports("java.util.List")
        .emitEmptyLine();

    String type = schema.get("type").getAsString();
    if (type.equals("object")) {
      if (roomAnnotationMap != null && roomAnnotationMap.containsKey(id)) {
        final List<AnnotationType> annotations = roomAnnotationMap.get(id);
        for (AnnotationType annotationType : annotations) {
          if (annotationType.attributes == null) {
            javaWriter.emitAnnotation(annotationType.annotation);
          } else {
            javaWriter.emitAnnotation(annotationType.annotation, annotationType.attributes);
          }
        }
      }
      javaWriter.beginType(
          classInfo.packageName + "." + classInfo.className, "class", EnumSet.of(PUBLIC));
      generateObject(javaWriter, schema, classMap, modelPackageName, packageMap, roomAnnotationMap);
      javaWriter.endType();
    } else if (type.equals("string")) {
      javaWriter.beginType(
          classInfo.packageName + "." + classInfo.className, "enum", EnumSet.of(PUBLIC));
      generateEnum(javaWriter, schema);
      javaWriter.endType();
    }

    writer.close();
  }

  private static void generateObject(
      JavaWriter javaWriter, JsonObject schema,
      Map<String, String> classMap, String packageName, Map<String, String> packageMap,
      Map<String, List<AnnotationType>> roomAnnnotationMap)
      throws IOException {
    JsonElement element = schema.get("properties");
    if (element == null) {
      return;
    }
    String id = schema.get("id").getAsString();
    JsonObject properties = element.getAsJsonObject();
    for (Entry<String, JsonElement> entry : properties.entrySet()) {
      String key = entry.getKey();
      String variableName = key;
      if (StringUtil.isReservedWord(key)) {
        javaWriter.emitAnnotation("SerializedName(\"" + key + "\")");
        variableName += "_";
      }
      final String annotationKey = id + "." + key;
      if (roomAnnnotationMap != null && roomAnnnotationMap.containsKey(annotationKey)) {
        final List<AnnotationType> annotations = roomAnnnotationMap.get(annotationKey);
        for (AnnotationType annotationType : annotations) {
          if (annotationType.attributes == null) {
            javaWriter.emitAnnotation(annotationType.annotation);
          } else {
            javaWriter.emitAnnotation(annotationType.annotation, annotationType.attributes);
          }
        }
      }
      PropertyType propertyType = gson.fromJson(
          entry.getValue(), PropertyType.class);
      String javaType = propertyType.toJavaType();
      if (classMap != null && classMap.containsKey(key)) {
        javaType = classMap.get(key);
      }

      boolean isList = javaType.startsWith("List<");
      if (isList) {
        javaType = javaType.substring(5, javaType.length() - 1);
      }
      ClassInfo classInfo = new ClassInfo(packageName, javaType);
      classInfo.movePackage(packageMap);
      javaWriter.emitField(isList ? "List<" + classInfo.className + ">" : classInfo.className,
          variableName, EnumSet.of(PUBLIC));
    }
  }

  private static void generateEnum(JavaWriter javaWriter, JsonObject schema) throws IOException {
    JsonArray enums = schema.get("enum").getAsJsonArray();
    for (int i = 0; i < enums.size(); ++i) {
      javaWriter.emitEnumValue(enums.get(i).getAsString());
    }
  }

  private static void generateInterfaceFromResources(
      WriterFactory writerFactory, String packageName,
      String resourceName, JsonObject resources,
      EnumSet<MethodType> methodTypes, Map<String, String> packageMap)
      throws IOException {
    for (Entry<String, JsonElement> entry : resources.entrySet()) {
      JsonObject entryValue = entry.getValue().getAsJsonObject();

      if (entryValue.has("methods")) {
        generateInterface(writerFactory, packageName,
            resourceName + "_" + entry.getKey(),
            entryValue.get("methods").getAsJsonObject(),
            methodTypes, packageMap);
      }

      if (entryValue.has("resources")) {
        generateInterfaceFromResources(writerFactory, packageName,
            resourceName + "_" + entry.getKey(),
            entryValue.get("resources").getAsJsonObject(),
            methodTypes, packageMap);
      }
    }
  }

  private static String getPrefix(Map<String, String> packageMap, String className) {
    if (packageMap == null) {
      return null;
    }
    for (String prefix : packageMap.keySet()) {
      if (className.startsWith(prefix)) {
        return prefix;
      }
    }
    return null;
  }

  private static class ClassInfo {
    public String packageName;
    public String className;

    public ClassInfo(String packageName, String className) {
      this.packageName = packageName;
      this.className = className;
    }

    public void movePackage(Map<String, String> packageMap) {
      String prefix = getPrefix(packageMap, className);
      if (prefix != null) {
        packageName += "." + packageMap.get(prefix);
        className = className.substring(prefix.length());
      }
    }
  }

  private static void generateInterface(
      WriterFactory writerFactory, String packageName,
      String resourceName, JsonObject methods,
      EnumSet<MethodType> methodTypes, Map<String, String> packageMap)
      throws IOException {
    String capitalizedName = WordUtils.capitalizeFully(resourceName, '_');
    String className = capitalizedName.replaceAll("_", "");

    ClassInfo classInfo = new ClassInfo(packageName, className);
    classInfo.movePackage(packageMap);

    String path = StringUtil.getPath(classInfo.packageName, classInfo.className + ".java");
    Writer fileWriter = writerFactory.getWriter(path);
    JavaWriter javaWriter = new JavaWriter(fileWriter);

    javaWriter.emitPackage(packageName);

    javaWriter.emitImports(packageName + ".model.*");

    if (packageMap != null) {
      Set<String> models = getModels(methods);
      Set<String> subdirectories = new HashSet<>(models.size());
      for (String model : models) {
        String dir = packageMap.get(getPrefix(packageMap, model));
        if (dir != null) {
          subdirectories.add(dir);
        }
      }
      ArrayList<String> dirs = new ArrayList<>(subdirectories);
      Collections.sort(dirs);
      for (String dir : dirs) {
        javaWriter.emitImports(packageName + ".model." + dir + ".*");
      }
    }

    if (methodTypes.contains(MethodType.V2)) {
      javaWriter.emitEmptyLine()
          .emitImports(
              "retrofit2.Call",
              "retrofit2.http.GET",
              "retrofit2.http.POST",
              "retrofit2.http.PATCH",
              "retrofit2.http.PUT",
              "retrofit2.http.DELETE",
              "retrofit2.http.Body",
              "retrofit2.http.Path",
              "retrofit2.http.Query");
    } else {
      javaWriter.emitEmptyLine()
          .emitImports(
              "retrofit.Callback",
              "retrofit.client.Response",
              "retrofit.http.GET",
              "retrofit.http.POST",
              "retrofit.http.PATCH",
              "retrofit.http.PUT",
              "retrofit.http.DELETE",
              "retrofit.http.Body",
              "retrofit.http.Path",
              "retrofit.http.Query");
    }

    if (methodTypes.contains(MethodType.REACTIVE)) {
      javaWriter.emitImports("rx.Observable");
    }

    javaWriter.emitEmptyLine();

    javaWriter.beginType(
        packageName + "." + className, "interface", EnumSet.of(PUBLIC));

    for (Entry<String, JsonElement> entry : methods.entrySet()) {
      String methodName = entry.getKey();
      Method method = gson.fromJson(entry.getValue(), Method.class);

      for (MethodType methodType : methodTypes) {
        String prefix = methodType.equals(MethodType.V2) ? "" : "/";
        javaWriter.emitAnnotation(method.httpMethod, "\"" + prefix + method.path + "\"");
        emitMethodSignature(fileWriter, methodName, method, methodType, packageMap);
      }
    }

    javaWriter.endType();

    fileWriter.close();
  }

  private static Set<String> getModels(JsonObject methods) {
    Set<String> models = new HashSet<>();
    for (Entry<String, JsonElement> entry : methods.entrySet()) {
      Method method = gson.fromJson(entry.getValue(), Method.class);
      if (method.request != null && method.request.$ref != null) {
        models.add(method.request.$ref);
      }
      if (method.response != null && method.response.$ref != null) {
        models.add(method.response.$ref);
      }
      for (Entry<String, JsonElement> param : getParams(method)) {
        ParameterType paramType = gson.fromJson(
            param.getValue(), ParameterType.class);
        String type = paramType.toJavaType();
        if (!paramType.required) {
          type = StringUtil.primitiveToObject(type);
        }
        models.add(type);
      }
    }
    return models;
  }

  // TODO: Use JavaWriter to emit method signature
  private static void emitMethodSignature(
      Writer writer, String methodName, Method method, MethodType methodType,
      Map<String, String> packageMap) throws IOException {
    ArrayList<String> params = new ArrayList<>();

    if (method.request != null) {
      ClassInfo classInfo = new ClassInfo("", method.request.$ref);
      classInfo.movePackage(packageMap);
      params.add("@Body " + classInfo.className + " " +
          (method.request.parameterName != null ? method.request.parameterName : "resource"));
    }
    for (Entry<String, JsonElement> param : getParams(method)) {
      ClassInfo classInfo = new ClassInfo("", param2String(param));
      classInfo.movePackage(packageMap);
      params.add(classInfo.className);
    }

    String returnValue = "void";
    if (methodType == MethodType.SYNC && "POST".equals(method.httpMethod)) {
      returnValue = "Response";
    }


    String className = "Void";
    if (method.response != null) {
      ClassInfo classInfo = new ClassInfo("", method.response.$ref);
      classInfo.movePackage(packageMap);
      className = classInfo.className;
    }

    if (methodType == MethodType.V2) {
      returnValue = "Call<" + className + ">";
    } else {
      if (method.response != null) {
        if (methodType == MethodType.SYNC) {
          returnValue = className;
        } else if (methodType == MethodType.REACTIVE) {
          returnValue = "Observable<" + className + ">";
        }
      }
    }

    if (methodType == MethodType.ASYNC) {
      if (method.response == null) {
        params.add("Callback<Void> cb");
      } else {
        ClassInfo classInfo = new ClassInfo("", method.response.$ref);
        classInfo.movePackage(packageMap);
        params.add("Callback<" + classInfo.className + "> cb");
      }
    }

    writer.append("  " + returnValue + " " + methodName + (methodType == MethodType.REACTIVE ? "Rx" : "") + "(");
    for (int i = 0; i < params.size(); ++i) {
      if (i != 0) {
        writer.append(", ");
      }
      writer.append(params.get(i));
    }

    writer.append(");\n");
  }

  /**
   * Assemble a list of parameters, with the first entries matching the ones
   * listed in parameterOrder
   *
   * @param method The method containing parameters and parameterOrder
   * @return Ordered parameters
   */
  private static List<Entry<String, JsonElement>> getParams(Method method) {
    List<Entry<String, JsonElement>> params
        = new ArrayList<Entry<String, JsonElement>>();
    if (method.parameters == null) {
      return params;
    }

    // Convert the entry set into a map, and extract the keys not listed in
    // parameterOrder
    HashMap<String, Entry<String, JsonElement>> map
        = new HashMap<String, Entry<String, JsonElement>>();
    List<String> remaining = new ArrayList<String>();
    for (Entry<String, JsonElement> entry : method.parameters.entrySet()) {
      String key = entry.getKey();
      map.put(key, entry);
      if (method.parameterOrder == null ||
          !method.parameterOrder.contains(key)) {
        remaining.add(key);
      }
    }

    // Add the keys in parameterOrder
    if (method.parameterOrder != null) {
      for (String key : method.parameterOrder) {
        params.add(map.get(key));
      }
    }

    // Then add the keys not in parameterOrder
    for (String key : remaining) {
      params.add(map.get(key));
    }

    return params;
  }

  private static String param2String(Entry<String, JsonElement> param) {
    StringBuffer buf = new StringBuffer();
    String paramName = param.getKey();
    ParameterType paramType = gson.fromJson(
        param.getValue(), ParameterType.class);
    if ("path".equals(paramType.location)) {
      buf.append("@Path(\"" + paramName + "\") ");
    }
    if ("query".equals(paramType.location)) {
      buf.append("@Query(\"" + paramName + "\") ");
    }

    String type = paramType.toJavaType();
    if (!paramType.required) {
      type = StringUtil.primitiveToObject(type);
    }
    buf.append(type + " " + paramName);

    return buf.toString();
  }
}
