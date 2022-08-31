package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.devcycle.sdk.server.common.model.DVCOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DVCApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String BUCKETING_URL = "https://bucketing-api.devcycle.com/";
  private static final String CONFIG_URL = "https://config-cdn.devcycle.com/";

  private Boolean cloudBucketing = false;
  private String configUrl;

  private DVCApiClient(DVCOptions options) {
    String url;

    String bucketingApiUrl = System.getenv("BUCKETING_API_URL");
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    okBuilder = new OkHttpClient.Builder();

    String cdnUrlFromOptions = options.getConfigCdnBaseUrl();
    Boolean enableCloudBucketing = options.getEnableCloudBucketing();

    this.cloudBucketing = Objects.isNull(enableCloudBucketing) ? false : enableCloudBucketing;

    if (this.cloudBucketing) {
      url = checkIfStringNullOrEmpty(bucketingApiUrl) ? BUCKETING_URL : bucketingApiUrl;
    } else {
      configUrl = checkIfStringNullOrEmpty(cdnUrlFromOptions) ? CONFIG_URL : cdnUrlFromOptions;

      url = this.configUrl;
    }

    adapterBuilder = new Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(JacksonConverterFactory.create());
  }

  public DVCApiClient(String apiKey, DVCOptions options) {
    this(options);
    if (this.cloudBucketing) {
      okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(apiKey));
    }
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
