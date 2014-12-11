package com.sqisland.gce2retrofit;

import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class StringUtilTest {
  @Test
  public void booleanToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject("boolean")).isEqualTo("Boolean");
  }

  @Test
  public void intToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject("int")).isEqualTo("Integer");
  }

  @Test
  public void floatToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject("float")).isEqualTo("Float");
  }

  @Test
  public void doubleToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject("double")).isEqualTo("Double");
  }

  @Test
  public void stringToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject("String")).isEqualTo("String");
  }

  @Test
  public void nullToObject() throws Exception {
    assertThat(StringUtil.primitiveToObject(null)).isNull();
  }

  @Test
  public void getPathNull() throws Exception {
    assertThat(StringUtil.getPath(null, null)).isNull();
  }

  @Test
  public void getPathExample() throws Exception {
    assertThat(StringUtil.getPath("com.example", "Hello.java"))
        .isEqualTo("com/example/Hello.java");
  }

  @Test
  public void getPackageName() throws Exception {
    assertThat(StringUtil.getPackageName("http://hello.example.com"))
        .isEqualTo("com.example.hello");
  }

  @Test
  public void getPackageNameNull() throws Exception {
    assertThat(StringUtil.getPackageName(null)).isNull();
  }

  @Test
     public void getPackageNameWithDash() throws Exception {
    assertThat(StringUtil.getPackageName("http://hello-world.example.com"))
        .isEqualTo("com.example.hello_world");
  }

  @Test
  public void getPackageNameEndWithNumber() throws Exception {
    assertThat(StringUtil.getPackageName("http://hello1.example.com"))
        .isEqualTo("com.example.hello1");
  }

  @Test
  public void getPackageNameStartWithNumber() throws Exception {
    assertThat(StringUtil.getPackageName("http://1hello.example.com"))
        .isEqualTo("com.example._1hello");
  }

  @Test
  public void getPackageNameNoDomain() throws Exception {
    assertThat(StringUtil.getPackageName("localhost")).isNull();
  }

  @Test(expected=URISyntaxException.class)
  public void getPackageNameBadCharacter() throws Exception {
    StringUtil.getPackageName("|");
  }
}