package com.sqisland.android.gce2retrofit;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.appspot._2_dot_test_pont.Greetings;
import com.appspot._2_dot_test_pont.model.HelloGreeting;
import com.appspot._2_dot_test_pont.model.HelloGreetingCollection;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends ListActivity {
  private static final String API_URL
      = "https://2-dot-test-pont.appspot.com/_ah/api/helloworld/v1/";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final GreetingAdapter adapter = new GreetingAdapter(this);
    getListView().setAdapter(adapter);

    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_URL)
        .build();
    Greetings greetings = restAdapter.create(Greetings.class);

    Callback<HelloGreetingCollection> callback = new Callback<HelloGreetingCollection>() {
      @Override
      public void success(HelloGreetingCollection collection, Response response) {
        adapter.clear();
        for (int i = 0; i < collection.items.size(); ++i) {
          HelloGreeting item = collection.items.get(i);
          adapter.add(item);
        }
      }

      @Override
      public void failure(RetrofitError error) {
        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
      }
    };
    greetings.listGreeting(callback);
  }

  private static class GreetingAdapter extends ArrayAdapter<HelloGreeting> {
    public GreetingAdapter(Context context) {
      super(context, android.R.layout.simple_list_item_1, new ArrayList<HelloGreeting>());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TextView textView = (TextView) super.getView(position, convertView, parent);
      HelloGreeting item = getItem(position);
      textView.setText(item.message);
      return textView;
    }
  }
}