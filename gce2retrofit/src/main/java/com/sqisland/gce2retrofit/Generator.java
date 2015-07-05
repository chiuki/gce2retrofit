package com.sqisland.gce2retrofit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static javax.lang.model.element.Modifier.PUBLIC;

public class Generator {
  private static final String OPTION_CLASS_MAP = "classmap";
  private static final String OPTION_METHODS = "methods";

  private static Gson gson = new Gson();

  public enum MethodType {
    SYNC, ASYNC, REACTIVE
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
        readClassMap(new FileReader(cmd.getOptionValue(OPTION_CLASS_MAP))) : null;

    EnumSet<MethodType> methodTypes = getMethods(cmd.getOptionValue(OPTION_METHODS));

    generate(new FileReader(discoveryFile), new FileWriterFactory(new File(outputDir)),
        classMap, methodTypes);
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(
        OPTION_CLASS_MAP, true, "Map fields to classes. Format: field_name\\tclass_name");
    options.addOption(
        OPTION_METHODS, true,
        "Methods to generate, either sync, async or reactive. Default is to generate sync & async.");
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
    JsonReader jsonReader = new JsonReader(discoveryReader);

    Discovery discovery = gson.fromJson(jsonReader, Discovery.class);

    String packageName = StringUtil.getPackageName(discovery.baseUrl);
    if (packageName == null || packageName.isEmpty()) {
      packageName = StringUtil.getPackageName(discovery.rootUrl);
    }
    String modelPackageName = packageName + ".model";

    for (Entry<String, JsonElement> entry : discovery.schemas.entrySet()) {
      generateModel(
          writerFactory, modelPackageName, entry.getValue().getAsJsonObject(), classMap);
    }

    if (discovery.resources != null) {
      generateInterfaceFromResources(
          writerFactory, packageName, "", discovery.resources, methodTypes);
    }

    if (discovery.name != null && discovery.methods != null) {
      generateInterface(
          writerFactory, packageName, discovery.name, discovery.methods, methodTypes);
    }
  }

  public static Map<String, String> readClassMap(Reader reader) throws IOException {
    Map<String, String> classMap = new HashMap<String, String>();

    String line;
    BufferedReader bufferedReader = new BufferedReader(reader);
    while ((line = bufferedReader.readLine()) != null) {
      String[] fields = line.split("\t");
      if (fields.length == 2) {
        classMap.put(fields[0], fields[1]);
      }
    }

    return classMap;
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
      }
    }
    if (methodTypes.isEmpty()) {
      methodTypes = EnumSet.of(Generator.MethodType.ASYNC, Generator.MethodType.SYNC);
    }
    return methodTypes;
  }

  private static void generateModel(
      WriterFactory writerFactory, String modelPackageName,
      JsonObject schema, Map<String, String> classMap)
      throws IOException {
    String id = schema.get("id").getAsString();

    String path = StringUtil.getPath(modelPackageName, id + ".java");
    Writer writer = writerFactory.getWriter(path);
    JavaWriter javaWriter = new JavaWriter(writer);

    javaWriter.emitPackage(modelPackageName)
        .emitImports("com.google.gson.annotations.SerializedName")
        .emitEmptyLine()
        .emitImports("java.util.List")
        .emitEmptyLine();

    String type = schema.get("type").getAsString();
    if (type.equals("object")) {
      javaWriter.beginType(modelPackageName + "." + id, "class", EnumSet.of(PUBLIC));
      generateObject(javaWriter, schema, classMap);
      javaWriter.endType();
    } else if (type.equals("string")) {
      javaWriter.beginType(modelPackageName + "." + id, "enum", EnumSet.of(PUBLIC));
      generateEnum(javaWriter, schema);
      javaWriter.endType();
    }

    writer.close();
  }

  private static void generateObject(
      JavaWriter javaWriter, JsonObject schema, Map<String, String> classMap)
      throws IOException {
    JsonElement element = schema.get("properties");
    if (element == null) {
      return;
    }
    JsonObject properties = element.getAsJsonObject();
    for (Entry<String, JsonElement> entry : properties.entrySet()) {
      String key = entry.getKey();
      String variableName = key;
      if (StringUtil.isReservedWord(key)) {
        javaWriter.emitAnnotation("SerializedName(\"" + key + "\")");
        variableName += "_";
      }
      PropertyType propertyType = gson.fromJson(
          entry.getValue(), PropertyType.class);
      String javaType = propertyType.toJavaType();
      if (classMap != null && classMap.containsKey(key)) {
        javaType = classMap.get(key);
      }
      javaWriter.emitField(javaType, variableName, EnumSet.of(PUBLIC));
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
      EnumSet<MethodType> methodTypes)
      throws IOException {
    for (Entry<String, JsonElement> entry : resources.entrySet()) {
      JsonObject entryValue = entry.getValue().getAsJsonObject();
      
      if (entryValue.has("methods")) {
        generateInterface(writerFactory, packageName,
            resourceName + "_" + entry.getKey(),
            entryValue.get("methods").getAsJsonObject(),
            methodTypes);
      }
    
      if (entryValue.has("resources")) {
        generateInterfaceFromResources(writerFactory, packageName,
            resourceName + "_" + entry.getKey(),
            entryValue.get("resources").getAsJsonObject(),
            methodTypes);
      }
    }
  }

  private static void generateInterface(
      WriterFactory writerFactory, String packageName,
      String resourceName, JsonObject methods,
      EnumSet<MethodType> methodTypes)
      throws IOException {
    String capitalizedName = WordUtils.capitalizeFully(resourceName, '_');
    String className = capitalizedName.replaceAll("_", "");

    String path = StringUtil.getPath(packageName, className + ".java");
    Writer fileWriter = writerFactory.getWriter(path);
    JavaWriter javaWriter = new JavaWriter(fileWriter);

    javaWriter.emitPackage(packageName)
        .emitImports(packageName + ".model.*")
        .emitEmptyLine()
        .emitImports(
            "retrofit.Callback",
            "retrofit.client.Response",
            "retrofit.http.GET",
            "retrofit.http.POST",
            "retrofit.http.PATCH",
            "retrofit.http.DELETE",
            "retrofit.http.Body",
            "retrofit.http.Path",
            "retrofit.http.Query");

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
        javaWriter.emitAnnotation(method.httpMethod, "\"/" + method.path + "\"");
        emitMethodSignature(fileWriter, methodName, method, methodType);
      }
    }

    javaWriter.endType();

    fileWriter.close();
  }

  // TODO: Use JavaWriter to emit method signature
  private static void emitMethodSignature(
      Writer writer, String methodName, Method method, MethodType methodType) throws IOException {
    ArrayList<String> params = new ArrayList<String>();

    if (method.request != null) {
      params.add("@Body " + method.request.$ref + " " +
          (method.request.parameterName != null ? method.request.parameterName : "resource"));
    }
    for (Entry<String, JsonElement> param : getParams(method)) {
      params.add(param2String(param));
    }

    String returnValue = "void";
    if (methodType == MethodType.SYNC && "POST".equals(method.httpMethod)) {
      returnValue = "Response";
    }
    if (method.response != null) {
      if (methodType == MethodType.SYNC) {
        returnValue = method.response.$ref;
      } else if (methodType == MethodType.REACTIVE) {
        returnValue = "Observable<" + method.response.$ref + ">";
      }
    }

    if (methodType == MethodType.ASYNC) {
      if (method.response == null) {
        params.add("Callback<Void> cb");
      } else {
        params.add("Callback<" + method.response.$ref + "> cb");
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
