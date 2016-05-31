package com.sqisland.gce2retrofit.discovery;

import com.googleapis.www.Apis;
import com.googleapis.www.model.DirectoryList;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Discovery {
  private static final String API_URL = "https://www.googleapis.com/discovery/v1/";

  public static void main(String args[]) {
    Retrofit restAdapter = new Retrofit.Builder()
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    Apis apis = restAdapter.create(Apis.class);
    try {
      Response<DirectoryList> response = apis.list(null, null).execute();
      System.out.println(response.body().kind);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}