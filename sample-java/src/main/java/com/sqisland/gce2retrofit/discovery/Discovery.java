package com.sqisland.gce2retrofit.discovery;

import com.googleapis.www.Apis;
import com.googleapis.www.model.DirectoryList;

import retrofit.RestAdapter;

public class Discovery {
  private static final String API_URL = "https://www.googleapis.com/discovery/v1/";

  public static void main(String args[]) {
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_URL)
        .build();

    Apis apis = restAdapter.create(Apis.class);
    DirectoryList directoryList = apis.list(null, null);
    System.out.println(directoryList.kind);
  }
}