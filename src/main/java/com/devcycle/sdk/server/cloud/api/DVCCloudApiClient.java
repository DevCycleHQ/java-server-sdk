package com.devcycle.sdk.server.cloud.api;

import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
import com.devcycle.sdk.server.common.api.DVCApi;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DVCCloudApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String BUCKETING_URL = "https://bucketing-api.devcycle.com/";
  

  private DVCCloudApiClient(DVCCloudOptions options) {
    String url;

    String bucketingApiUrl = System.getenv("BUCKETING_API_URL");
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    okBuilder = new OkHttpClient.Builder();

    url = checkIfStringNullOrEmpty(bucketingApiUrl) ? BUCKETING_URL : bucketingApiUrl;

    adapterBuilder = new Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(JacksonConverterFactory.create());
  }

  public DVCCloudApiClient(String apiKey, DVCCloudOptions options) {
    this(options);
    okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(apiKey));
  }

  public DVCApi initialize() {
    return adapterBuilder
        .client(okBuilder.build())
        .build()
        .create(DVCApi.class);
  }

  private Boolean checkIfStringNullOrEmpty(String stringToCheck) {
    return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
  }
}
