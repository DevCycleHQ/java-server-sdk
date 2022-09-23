package com.devcycle.sdk.server.local.managers;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.devcycle.sdk.server.common.model.ProjectConfig;
import com.devcycle.sdk.server.local.api.DVCLocalApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import retrofit2.Call;
import retrofit2.Response;

public final class EnvironmentConfigManager {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DEFAULT_POLL_INTERVAL_MS = 30000;
  private static final int MIN_INTERVALS_MS = 1000;

  private IDVCApi configApiClient;
  private LocalBucketing localBucketing;

  private ProjectConfig config;
  private String configETag = "";

  private String serverKey;
  private int pollingIntervalMS;

  public EnvironmentConfigManager(String serverKey, LocalBucketing localBucketing, DVCLocalOptions options) {
    this.serverKey = serverKey;
    this.localBucketing = localBucketing;

    configApiClient = new DVCLocalApiClient(serverKey, options).initialize();

    int configPollingIntervalMs = options.getConfigPollingIntervalMs();
    pollingIntervalMS = configPollingIntervalMs >= MIN_INTERVALS_MS ? configPollingIntervalMs
        : DEFAULT_POLL_INTERVAL_MS;

    setupScheduler();
  }

  private void setupScheduler() {
    Runnable getConfigRunnable = new Runnable() {
      public void run() {
        try {
          getConfig();
        } catch (DVCException | JsonProcessingException e) {
          e.printStackTrace();
        }
      }
    };

    scheduler.scheduleAtFixedRate(getConfigRunnable, 0, this.pollingIntervalMS, TimeUnit.MILLISECONDS);
  }

  public boolean isConfigInitialized() {
    return config != null;
  }

  private ProjectConfig getConfig() throws DVCException, JsonProcessingException {
    Call<ProjectConfig> config = this.configApiClient.getConfig(this.serverKey, this.configETag);

    this.config = getConfigResponse(config);
    return this.config;
  }

  private ProjectConfig getConfigResponse(Call<ProjectConfig> call) throws DVCException, JsonProcessingException {
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    Response<ProjectConfig> response;

    try {
      response = call.execute();
    } catch (IOException e) {
      errorResponse.setMessage(e.getMessage());
      throw new DVCException(HttpResponseCode.byCode(500), errorResponse);
    }

    HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());
    errorResponse.setMessage("Unknown error");

    if (response.isSuccessful()) {
      String currentETag = response.headers().get("ETag");
      ProjectConfig config = response.body();
      try {
        ObjectMapper mapper = new ObjectMapper();
        localBucketing.storeConfig(serverKey, mapper.writeValueAsString(config));
      } catch (JsonProcessingException e) {
        if (this.config != null) {
          System.out.printf("Unable to parse config with etag: %s. Using cache, etag %s%n", currentETag, this.configETag);
          return this.config;
        } else {
          throw e;
        }
      }
      this.configETag = currentETag;
      return response.body();
    } else if (httpResponseCode == HttpResponseCode.NOT_MODIFIED) {
      System.out.printf("Config not modified, using cache, etag: %s%n", this.configETag);
      return this.config;
    } else {
      if (response.errorBody() != null) {
        try {
          errorResponse = OBJECT_MAPPER.readValue(response.errorBody().string(), ErrorResponse.class);
        } catch (IOException e) {
          errorResponse.setMessage(e.getMessage());
          throw new DVCException(httpResponseCode, errorResponse);
        }
        throw new DVCException(httpResponseCode, errorResponse);
      }

      if (httpResponseCode == HttpResponseCode.UNAUTHORIZED) {
        errorResponse.setMessage("API Key is unauthorized");
      } else if (!response.message().equals("")) {
        try {
          errorResponse = OBJECT_MAPPER.readValue(response.message(), ErrorResponse.class);
        } catch (JsonProcessingException e) {
          errorResponse.setMessage(e.getMessage());
          throw new DVCException(httpResponseCode, errorResponse);
        }
      }

      throw new DVCException(httpResponseCode, errorResponse);
    }
  }
}