package com.sqisland.gce2retrofit.hello;

import com.appspot._2_dot_test_pont.Greetings;
import com.appspot._2_dot_test_pont.model.HelloGreeting;
import com.appspot._2_dot_test_pont.model.HelloGreetingCollection;

import retrofit.RestAdapter;

public class HelloWorld {
  private static final String API_URL
      = "https://2-dot-test-pont.appspot.com/_ah/api/helloworld/v1/";

  public static void main(String args[]) {
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_URL)
        .build();

    Greetings greetings = restAdapter.create(Greetings.class);
    HelloGreetingCollection collection = greetings.listGreeting();
    for (int i = 0; i < collection.items.size(); ++i) {
      HelloGreeting item = collection.items.get(i);
      System.out.println(item.message.append(" [").append(i).append("]"));
    }
  }
}