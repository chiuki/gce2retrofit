package com.sqisland.gce2retrofit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Exception;
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
  public void testStringModel() throws Exception {
    InputStreamReader reader = new InputStreamReader(
        GeneratorTest.class.getResourceAsStream("/string-model/discovery.json"));
    StringWriterFactory factory = new StringWriterFactory();
    Generator.generate(reader, factory, null, EnumSet.of(Generator.MethodType.SYNC));
    assertThat(factory.getString("com/appspot/example/model/HelloGreeting.java"))
        .isEqualTo(getExpectedString("/string-model/HelloGreeting.java.expected"));
  }

  private static String getExpectedString(String path) throws URISyntaxException, IOException {
    URL url = GeneratorTest.class.getResource(path);
    Path resPath = Paths.get(url.toURI());
    return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
  }
}