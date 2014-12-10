package com.sqisland.gce2retrofit;

import com.google.gson.JsonObject;

import java.util.List;

public class Method {
  public String id;
  public String path;
  public String httpMethod;
  public String description;
  public JsonObject parameters;
  public List<String> parameterOrder;
  public RequestOrResponse request;
  public RequestOrResponse response;

  public static class RequestOrResponse {
    public String $ref;
    public String parameterName;
  }
}