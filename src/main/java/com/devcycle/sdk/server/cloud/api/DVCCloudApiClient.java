package com.devcycle.sdk.server.cloud.api;

import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
import com.devcycle.sdk.server.common.api.IDVCApi;
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
  private String bucketingUrl;

  private DVCCloudApiClient(DVCCloudOptions options) {
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    okBuilder = new OkHttpClient.Builder();

    if (!isStringNullOrEmpty(options.getBaseURLOverride())) {
      bucketingUrl = options.getBaseURLOverride();
    } else {
      bucketingUrl = BUCKETING_URL;
    }

    bucketingUrl = bucketingUrl.endsWith("/") ? bucketingUrl : bucketingUrl + "/";

    adapterBuilder = new Retrofit.Builder()
        .baseUrl(bucketingUrl)
        .addConverterFactory(JacksonConverterFactory.create());
  }

  public DVCCloudApiClient(String apiKey, DVCCloudOptions options) {
    this(options);
    okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(apiKey));
  }

  public IDVCApi initialize() {
    return adapterBuilder
        .client(okBuilder.build())
        .build()
        .create(IDVCApi.class);
  }

  private Boolean isStringNullOrEmpty(String stringToCheck) {
    return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
  }
}
