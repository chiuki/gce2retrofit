package com.sqisland.gce2retrofit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

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
  }

  private static String getExpectedString(String path) throws URISyntaxException, IOException {
    URL url = GeneratorTest.class.getResource(path);
    Path resPath = Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }
}