package com.sqisland.android.gce2retrofit;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.googleapis.www.Apis;
import com.googleapis.www.model.DirectoryList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {
  private static final String API_URL = "https://www.googleapis.com/discovery/v1/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final TextView textView = new TextView(this);
    setContentView(textView);

    Retrofit restAdapter = new Retrofit.Builder()
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    Apis apis = restAdapter.create(Apis.class);
    Call<DirectoryList> call = apis.list(null, null);
    call.enqueue(new Callback<DirectoryList>() {
      @Override
      public void onResponse(Call<DirectoryList> call, Response<DirectoryList> response) {
        textView.setText(response.body().kind);
      }
      @Override
      public void onFailure(Call<DirectoryList> call, Throwable t) {
        String msg = t.getMessage();
        if (TextUtils.isEmpty(msg)) {
          textView.setText(t.getClass().getSimpleName());
        }
      }
    });
  }
}