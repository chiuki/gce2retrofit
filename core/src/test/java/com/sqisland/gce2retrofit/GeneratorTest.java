package com.sqisland.gce2retrofit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class GeneratorTest {
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
    doTestHelloGreeting(EnumSet.allOf(Generator.MethodType.class), ".both");
  }

  @Test
  public void testClassMapJodaTime() throws IOException, URISyntaxException {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/joda-time/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();

    Map<String, String> classMap = Generator.readClassMap(new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/joda-time/classmap.tsv")));
    assertThat(classMap).containsEntry("start_time", "org.joda.time.DateTime");
    assertThat(classMap).containsEntry("end_time", "org.joda.time.DateTime");
    assertThat(classMap).hasSize(2);

    Generator.generate(reader, factory, classMap, EnumSet.noneOf(Generator.MethodType.class));

    assertThat(factory.getString("com/appspot/joda_time/model/Party.java"))
        .isEqualTo(getExpectedString("/joda-time/Party.java.model"));
    assertThat(factory.getCount()).isEqualTo(1);
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

  private static String getExpectedString(String path) throws URISyntaxException, IOException {
    URL url = GeneratorTest.class.getResource(path);
    Path resPath = Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }
}