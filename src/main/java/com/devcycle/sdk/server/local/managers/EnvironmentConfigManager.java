package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.devcycle.sdk.server.common.model.ProjectConfig;
import com.devcycle.sdk.server.local.api.DVCLocalApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class EnvironmentConfigManager {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DEFAULT_POLL_INTERVAL_MS = 30000;
  private static final int MIN_INTERVALS_MS = 1000;

  private IDVCApi configApiClient;
  private LocalBucketing localBucketing;

  private ProjectConfig config;
  private String configETag = "";

  private String sdkKey;
  private int pollingIntervalMS;
  private boolean pollingEnabled = true;

  public EnvironmentConfigManager(String sdkKey, LocalBucketing localBucketing, DVCLocalOptions options) {
    this.sdkKey = sdkKey;
    this.localBucketing = localBucketing;

    configApiClient = new DVCLocalApiClient(sdkKey, options).initialize();

    int configPollingIntervalMS = options.getConfigPollingIntervalMS();
    pollingIntervalMS = configPollingIntervalMS >= MIN_INTERVALS_MS ? configPollingIntervalMS
        : DEFAULT_POLL_INTERVAL_MS;

    setupScheduler();
  }

  private void setupScheduler() {
    Runnable getConfigRunnable = new Runnable() {
      public void run() {
        try {
          if(pollingEnabled){
            getConfig();
          }
        } catch (DVCException e) {
          System.out.println("Failed to load config: " + e.getMessage());
        }
      }
    };

    scheduler.scheduleAtFixedRate(getConfigRunnable, 0, this.pollingIntervalMS, TimeUnit.MILLISECONDS);
  }

  public boolean isConfigInitialized() {
    return config != null;
  }

  private ProjectConfig getConfig() throws DVCException {
    Call<ProjectConfig> config = this.configApiClient.getConfig(this.sdkKey, this.configETag);
    this.config = getResponseWithRetries(config, 1);
    return this.config;
  }

  private ProjectConfig getResponseWithRetries(Call<ProjectConfig> call, int maxRetries) throws DVCException {
    // attempt 0 is the initial request, attempt > 0 are all retries
    int attempt = 0;
    do {
      try {
        return getConfigResponse(call);
      } catch (DVCException e) {

        attempt++;

        // if out of retries or this is an unauthorized error, throw up exception
        if ( !e.isRetryable() || attempt > maxRetries) {
          throw e;
        }

        try {
          // exponential backoff
          long waitIntervalMS = (long) (10 * Math.pow(2, attempt));
          Thread.sleep(waitIntervalMS);
        } catch (InterruptedException ex) {
          // no-op
        }

        // prep the call for a retry
        call = call.clone();
      }
    }while (attempt <= maxRetries && pollingEnabled);

    // getting here should not happen, but is technically possible
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    errorResponse.setMessage("Out of retry attempts");
    throw new DVCException(HttpResponseCode.SERVER_ERROR, errorResponse);
  }

  private ProjectConfig getConfigResponse(Call<ProjectConfig> call) throws DVCException {
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    Response<ProjectConfig> response;

    try {
      response = call.execute();
    } catch(JsonParseException badJsonExc) {
      // Got a valid status code but the response body was not valid json,
      // need to ignore this attempt and let the polling retry
      errorResponse.setMessage(badJsonExc.getMessage());
      throw new DVCException(HttpResponseCode.NO_CONTENT, errorResponse);
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
        localBucketing.storeConfig(sdkKey, mapper.writeValueAsString(config));
      } catch (JsonProcessingException e) {
        if (this.config != null) {
          System.out.printf("Unable to parse config with etag: %s. Using cache, etag %s%n", currentETag, this.configETag);
          return this.config;
        } else {
          errorResponse.setMessage(e.getMessage());
          throw new DVCException(HttpResponseCode.SERVER_ERROR, errorResponse);
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
        } catch (JsonProcessingException e) {
          errorResponse.setMessage("Unable to parse error response: " + e.getMessage());
          throw new DVCException(httpResponseCode, errorResponse);
        } catch (IOException e) {
          errorResponse.setMessage(e.getMessage());
          throw new DVCException(httpResponseCode, errorResponse);
        }
        throw new DVCException(httpResponseCode, errorResponse);
      }

      if (httpResponseCode == HttpResponseCode.UNAUTHORIZED || httpResponseCode == HttpResponseCode.FORBIDDEN) {
        // SDK Key is no longer authorized or now blocked, stop polling for configs
        errorResponse.setMessage("API Key is unauthorized");
        stopPolling();
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

  private void stopPolling() {
    pollingEnabled = false;
    scheduler.shutdown();
  }

  public void cleanup() {
    stopPolling();
  }
}