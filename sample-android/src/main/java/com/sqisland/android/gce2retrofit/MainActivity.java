package com.sqisland.android.gce2retrofit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.googleapis.www.Apis;
import com.googleapis.www.model.DirectoryList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity {
  private static final String API_URL = "https://www.googleapis.com/discovery/v1/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final TextView textView = new TextView(this);
    setContentView(textView);

    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_URL)
        .build();

    Apis apis = restAdapter.create(Apis.class);

    Callback<DirectoryList> callback = new Callback<DirectoryList>() {
      @Override
      public void success(DirectoryList directoryList, Response response) {
        textView.setText(directoryList.kind);
      }

      @Override
      public void failure(RetrofitError error) {
        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
      }
    };
    apis.list(null, null, callback);
  }
}