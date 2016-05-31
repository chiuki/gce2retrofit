package com.appspot.post_response;

import com.appspot.post_response.model.*;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

public interface Registration {
  @POST("/registerDevice/{regId}")
  Response register(@Path("regId") String regId);
}
