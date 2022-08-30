package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Objects;

public final class DVCApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;

  private static final String BASE_URL = "https://bucketing-api.devcycle.com/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


  private DVCApiClient() {
    okBuilder = new OkHttpClient.Builder();

    String bucketingApiUrl = System.getenv("BUCKETING_API_URL");
    String url = Objects.isNull(bucketingApiUrl) || Objects.equals(bucketingApiUrl, "") ? BASE_URL : bucketingApiUrl;
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);


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
