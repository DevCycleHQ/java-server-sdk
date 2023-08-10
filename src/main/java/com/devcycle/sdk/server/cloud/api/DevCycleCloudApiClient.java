package com.devcycle.sdk.server.cloud.api;

import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.common.api.APIUtils;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DevCycleCloudApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String BUCKETING_URL = "https://bucketing-api.devcycle.com/";
  private String bucketingUrl;

  public DevCycleCloudApiClient(String apiKey, DevCycleCloudOptions options) {
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    okBuilder = new OkHttpClient.Builder();

    APIUtils.applyRestOptions(options.getRestOptions(), okBuilder);

    okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(apiKey));

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

  public IDevCycleApi initialize() {
    return adapterBuilder
        .client(okBuilder.build())
        .build()
        .create(IDevCycleApi.class);
  }

  private Boolean isStringNullOrEmpty(String stringToCheck) {
    return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
  }
}
