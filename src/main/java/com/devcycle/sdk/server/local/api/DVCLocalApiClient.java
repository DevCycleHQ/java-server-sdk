package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class DVCLocalApiClient {

  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder adapterBuilder;
  private final Retrofit.Builder eventsBuilder;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String CONFIG_URL = "https://config-cdn.devcycle.com/";
  private static final int DEFAULT_TIMEOUT_MS = 10000;
  private static final int MIN_INTERVALS_MS = 1000;
  
  private String configUrl;
  private int requestTimeoutMs;

  private DVCLocalApiClient(DVCLocalOptions options) {
    String url;

    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    okBuilder = new OkHttpClient.Builder();

    String cdnUrlFromOptions = options.getConfigCdnBaseUrl();
    int configRequestTimeoutMs = options.getConfigRequestTimeoutMs();

    configUrl = checkIfStringNullOrEmpty(cdnUrlFromOptions) ? CONFIG_URL : cdnUrlFromOptions;
    requestTimeoutMs = configRequestTimeoutMs >= MIN_INTERVALS_MS ? configRequestTimeoutMs : DEFAULT_TIMEOUT_MS;
  
    url = this.configUrl;
    okBuilder.callTimeout(this.requestTimeoutMs, TimeUnit.MILLISECONDS);

    adapterBuilder = new Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(JacksonConverterFactory.create());

    eventsBuilder = new Retrofit.Builder()
            .baseUrl("https://events.devcycle.com")
            .addConverterFactory(JacksonConverterFactory.create());
  }

  public DVCLocalApiClient(String serverKey, DVCLocalOptions options) {
    this(options);
  }

  public IDVCApi initialize() {
    return adapterBuilder
        .client(okBuilder.build())
        .build()
        .create(IDVCApi.class);
  }

  private Boolean checkIfStringNullOrEmpty(String stringToCheck) {
    return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
  }
}
