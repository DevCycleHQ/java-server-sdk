package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.interceptor.AuthorizationHeaderInterceptor;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

final class DVCApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;

  private static final String BASE_URL = "https://bucketing-api.devcycle.com/";

  private DVCApiClient() {
    okBuilder = new OkHttpClient.Builder();

    String bucketingApiUrl = System.getenv("BUCKETING_API_URL");
    String url = Objects.isNull(bucketingApiUrl) || Objects.equals(bucketingApiUrl, "") ? BASE_URL : bucketingApiUrl;

    adapterBuilder = new Retrofit
            .Builder()
            .baseUrl(url)
            .addConverterFactory(JacksonConverterFactory.create());
  }

  public DVCApiClient(String apiKey) {
    this();
    okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(apiKey));
  }

  public DVCApi initialize() {
    return adapterBuilder
            .client(okBuilder.build())
            .build()
            .create(DVCApi.class);
  }
}
