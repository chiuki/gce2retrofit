package com.sqisland.gce2retrofit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class GeneratorTest {
  private static final EnumSet<Generator.MethodType> BOTH_METHOD_TYPES
      = EnumSet.of(Generator.MethodType.SYNC, Generator.MethodType.ASYNC);

  @Test
  public void testHelloGreetingSync() throws IOException, URISyntaxException {
    doTestHelloGreeting(EnumSet.of(Generator.MethodType.SYNC), ".sync");
  }

  @Test
  public void testHelloGreetingAsync() throws IOException, URISyntaxException {
    doTestHelloGreeting(EnumSet.of(Generator.MethodType.ASYNC), ".async");
  }

  @Test
  public void testHelloGreetingBoth() throws IOException, URISyntaxException {
    doTestHelloGreeting(BOTH_METHOD_TYPES, ".both");
  }

  @Test
  public void testHelloGreetingReactive() throws IOException, URISyntaxException {
    doTestHelloGreeting(EnumSet.of(Generator.MethodType.REACTIVE), ".reactive");
  }

  @Test
  public void testHelloGreetingV2() throws IOException, URISyntaxException {
    doTestHelloGreeting(EnumSet.of(Generator.MethodType.V2), ".v2");
  }

  @Test
  public void testClassMapJodaTime() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/joda-time/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Map<String, String> classMap = Generator.readStringToStringMap(new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/joda-time/classmap.tsv")));
    assertThat(classMap).containsEntry("start_time", "org.joda.time.DateTime");
    assertThat(classMap).containsEntry("end_time", "org.joda.time.DateTime");
    assertThat(classMap).hasSize(2);

    Generator.generate(reader, factory, classMap, EnumSet.noneOf(Generator.MethodType.class));

    assertThat(factory.getString("com/appspot/joda_time/model/Party.java"))
        .isEqualTo(getExpectedString("/joda-time/Party.java.model"));
    assertThat(factory.getCount()).isEqualTo(1);
  }

  @Test
  public void testReservedWords() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/reserved-words/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Generator.generate(reader, factory, null, EnumSet.noneOf(Generator.MethodType.class));

    assertThat(factory.getString("com/appspot/reserved_words/model/Naughty.java"))
        .isEqualTo(getExpectedString("/reserved-words/Naughty.java.model"));
    assertThat(factory.getCount()).isEqualTo(1);
  }

  @Test
  public void testNestedResources() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/nested-resources/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();
    Generator.generate(reader, factory, null, BOTH_METHOD_TYPES);

    assertThat(factory.getString("com/appspot/nested_resources/model/TestObject.java"))
        .isEqualTo(getExpectedString("/nested-resources/TestObject.java.model"));
    assertThat(factory.getString("com/appspot/nested_resources/NestedTest.java"))
        .isEqualTo(getExpectedString("/nested-resources/NestedTest.java.both"));
    assertThat(factory.getCount()).isEqualTo(2);
  }

  @Test
  public void testNamelessParameter() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/nameless-parameter/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();
    Generator.generate(reader, factory, null, BOTH_METHOD_TYPES);

    assertThat(factory.getString("com/appspot/nameless_parameter/model/TestObject.java"))
        .isEqualTo(getExpectedString("/nameless-parameter/TestObject.java.model"));
    assertThat(factory.getString("com/appspot/nameless_parameter/Nameless.java"))
        .isEqualTo(getExpectedString("/nameless-parameter/Nameless.java.both"));
    assertThat(factory.getCount()).isEqualTo(2);
  }

  private void doTestHelloGreeting(EnumSet<Generator.MethodType> methodTypes, String suffix)
      throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/helloworld/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();
    Generator.generate(reader, factory, null, methodTypes);

    assertThat(factory.getString("com/appspot/example/model/HelloGreeting.java"))
        .isEqualTo(getExpectedString("/helloworld/HelloGreeting.java.model"));
    assertThat(factory.getString("com/appspot/example/model/HelloGreetingCollection.java"))
        .isEqualTo(getExpectedString("/helloworld/HelloGreetingCollection.java.model"));
    assertThat(factory.getString("com/appspot/example/Greetings.java"))
        .isEqualTo(getExpectedString("/helloworld/Greetings.java" + suffix));
    assertThat(factory.getCount()).isEqualTo(3);
  }

  @Test
  public void testEnum() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/enum/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC));

    assertThat(factory.getString("com/appspot/kyatest_kfkb/model/CONTENT_TYPE.java"))
        .isEqualTo(getExpectedString("/enum/CONTENT_TYPE.java.model"));
    assertThat(factory.getString("com/appspot/kyatest_kfkb/model/MediaGetDTO.java"))
        .isEqualTo(getExpectedString("/enum/MediaGetDTO.java.model"));
    assertThat(factory.getString("com/appspot/kyatest_kfkb/Media.java"))
        .isEqualTo(getExpectedString("/enum/Media.java.sync"));
    assertThat(factory.getCount()).isEqualTo(3);
  }

  @Test
  // https://github.com/chiuki/gce2retrofit/issues/9
  public void testIssue9() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/issue9/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC));

    assertThat(factory.getString("com/appspot/kyadev_kfkb/model/JsonMap.java"))
        .isEqualTo(getExpectedString("/issue9/JsonMap.java.model"));
    assertThat(factory.getCount()).isEqualTo(1);
  }

  @Test
  // https://github.com/chiuki/gce2retrofit/issues/11
  public void testPostResponse() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/post-response/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC));

    assertThat(factory.getString("com/appspot/post_response/Registration.java"))
        .isEqualTo(getExpectedString("/post-response/Registration.java"));
    assertThat(factory.getCount()).isEqualTo(1);
  }

  @Test
  public void testPackagePrefix() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/package-prefix/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Map<String, String> packageMap = Generator.readStringToStringMap(new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/package-prefix/packagemap.tsv")));
    assertThat(packageMap).containsEntry("MyCompanyDatabase", "db");
    assertThat(packageMap).hasSize(1);

    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC), packageMap, null);

    assertThat(factory.getString("com/appspot/package_prefix/model/db/User.java"))
        .isEqualTo(getExpectedString("/package-prefix/User.java.model"));
    assertThat(factory.getString("com/appspot/package_prefix/Greetings.java"))
        .isEqualTo(getExpectedString("/package-prefix/Greetings.java.sync"));
    assertThat(factory.getCount()).isEqualTo(2);
  }

  @Test
  public void testRoomAnnotations() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
      GeneratorTest.class.getResourceAsStream("/room/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Map<String, List<AnnotationType>> annotationMap = Generator.readAnnotationMap(new InputStreamReader(
      GeneratorTest.class.getResourceAsStream("/room/room.json")));
    assertThat(annotationMap).hasSize(2);

    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC), null, annotationMap);
    assertThat(factory.getString("com/appspot/example/model/HelloGreeting.java"))
      .isEqualTo(getExpectedString("/room/HelloGreeting.java.model"));
  }

  private static String getExpectedString(String path) throws URISyntaxException, IOException {
    URL url = GeneratorTest.class.getResource(path);
    Path resPath = Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }
}