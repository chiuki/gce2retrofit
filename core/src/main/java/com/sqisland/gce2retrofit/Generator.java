package com.sqisland.gce2retrofit;

import com.google.gson.Gson;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
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
    SYNC, ASYNC
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
        readClassMap(cmd.getOptionValue(OPTION_CLASS_MAP)) : null;

    EnumSet<MethodType> methodTypes = EnumSet.noneOf(MethodType.class);
    if (cmd.hasOption(OPTION_METHODS)) {
      String[] parts = cmd.getOptionValue(OPTION_METHODS).split(",");
      for (String part : parts) {
        if ("sync".equals(part) || "both".equals(part)) {
          methodTypes.add(MethodType.SYNC);
        }
        if ("async".equals(part) || "both".equals(part)) {
          methodTypes.add(MethodType.ASYNC);
        }
      }
    }
    if (methodTypes.isEmpty()) {
      methodTypes = EnumSet.allOf(MethodType.class);
    }

    generate(discoveryFile, outputDir, classMap, methodTypes);
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption(
        OPTION_CLASS_MAP, true, "Map fields to classes. Format: field_name\\tclass_name");
    options.addOption(
        OPTION_METHODS, true,
        "Methods to generate, either sync or async. Default is to generate both.");
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
      String discoveryFile, String outputDir,
      Map<String, String> classMap, EnumSet<MethodType> methodTypes)
      throws IOException, URISyntaxException {
    JsonReader jsonReader = new JsonReader(new FileReader(discoveryFile));

    Discovery discovery = gson.fromJson(jsonReader, Discovery.class);

    String packageName = getPackageName(discovery.baseUrl);
    String subdir = packageName.replace(".", "/");
    File dir = new File(outputDir, subdir);

    String modelPackage = packageName + ".model";
    File modelDir = new File(dir, "model");
    modelDir.mkdirs();

    for (Entry<String, JsonElement> entry : discovery.schemas.entrySet()) {
      generateModel(
          modelDir, modelPackage, entry.getValue().getAsJsonObject(), classMap);
    }

    for (Entry<String, JsonElement> entry : discovery.resources.entrySet()) {
      generateInterface(dir, packageName, entry, methodTypes);
    }
  }

  private static Map<String, String> readClassMap(String path) throws IOException {
    Map<String, String> classMap = new HashMap<String, String>();

    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] fields = line.split("\t");
      if (fields.length == 2) {
        classMap.put(fields[0], fields[1]);
      }
    }

    return classMap;
  }

  private static void generateModel(
      File dir, String modelPackage, JsonObject schema, Map<String, String> classMap)
      throws IOException {
    String id = schema.get("id").getAsString();

    File java = new File(dir, id + ".java");
    FileWriter fileWriter = new FileWriter(java);
    JavaWriter javaWriter = new JavaWriter(fileWriter);

    javaWriter.emitPackage(modelPackage)
        .emitImports("java.util.List")
        .emitEmptyLine();

    javaWriter.beginType(modelPackage + "." + id, "class", EnumSet.of(PUBLIC));

    JsonObject properties = schema.get("properties").getAsJsonObject();
    for (Entry<String, JsonElement> entry : properties.entrySet()) {
      String key = entry.getKey();
      PropertyType propertyType = gson.fromJson(
          entry.getValue(), PropertyType.class);
      String javaType = propertyType.toJavaType();
      if (classMap != null && classMap.containsKey(key)) {
        javaType = classMap.get(key);
      }
      javaWriter.emitField(javaType, key, EnumSet.of(PUBLIC));
    }

    javaWriter.endType();

    fileWriter.close();
  }

  private static void generateInterface(
      File dir, String packageName, Entry<String, JsonElement> resource,
      EnumSet<MethodType> methodTypes)
      throws IOException {
    String resourceName = resource.getKey();
    String capitalizedName = WordUtils.capitalizeFully(resourceName, '_');
    String className = capitalizedName.replaceAll("_", "");

    File java = new File(dir, className + ".java");
    FileWriter fileWriter = new FileWriter(java);
    JavaWriter javaWriter = new JavaWriter(fileWriter);

    javaWriter.emitPackage(packageName)
        .emitImports(packageName + ".model.*")
        .emitEmptyLine()
        .emitImports(
            "retrofit.Callback",
            "retrofit.http.GET",
            "retrofit.http.POST",
            "retrofit.http.PATCH",
            "retrofit.http.DELETE",
            "retrofit.http.Body",
            "retrofit.http.Path",
            "retrofit.http.Query")
        .emitEmptyLine();

    javaWriter.beginType(
        packageName + "." + className, "interface", EnumSet.of(PUBLIC));

    JsonObject methods = resource.getValue().getAsJsonObject()
        .get("methods").getAsJsonObject();
    for (Entry<String, JsonElement> entry : methods.entrySet()) {
      String methodName = entry.getKey();
      Method method = gson.fromJson(entry.getValue(), Method.class);

      if (methodTypes.contains(MethodType.SYNC)) {
        javaWriter.emitAnnotation(method.httpMethod, "\"/" + method.path + "\"");
        emitMethodSignature(fileWriter, methodName, method, true);
      }

      if (methodTypes.contains(MethodType.ASYNC)) {
        javaWriter.emitAnnotation(method.httpMethod, "\"/" + method.path + "\"");
        emitMethodSignature(fileWriter, methodName, method, false);
      }
    }

    javaWriter.endType();

    fileWriter.close();
  }

  // TODO: Use JavaWriter to emit method signature
  private static void emitMethodSignature(
      Writer writer, String methodName, Method method, boolean synchronous) throws IOException {
    ArrayList<String> params = new ArrayList<String>();

    if (method.request != null) {
      params.add("@Body " + method.request.$ref + " " +
          method.request.parameterName);
    }
    for (Entry<String, JsonElement> param : getParams(method)) {
      params.add(param2String(param));
    }

    String returnValue = "void";
    if (method.response != null) {
      if (synchronous) {
        returnValue = method.response.$ref;
      } else {
        params.add("Callback<" + method.response.$ref + "> cb");
      }
    }

    writer.append("  " + returnValue + " " + methodName + "(");
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
      type = primitiveToObject(type);
    }
    buf.append(type + " " + paramName);

    return buf.toString();
  }

  private static String primitiveToObject(String type) {
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

  private static String getPackageName(String baseUrl)
      throws URISyntaxException {
    URI uri = new URI(baseUrl);
    String domain = uri.getHost();
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
}