package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.devcycle.sdk.server.common.model.ProjectConfig;
import com.devcycle.sdk.server.local.api.DevCycleLocalApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_POLL_INTERVAL_MS = 30000;
    private static final int MIN_INTERVALS_MS = 1000;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());
    private final IDevCycleApi configApiClient;
    private final LocalBucketing localBucketing;
    private final EventQueueManager eventQueueManager;

    private ProjectConfig config;
    private String configETag = "";

    private String configLastModified = "";

    private final String sdkKey;
    private final int pollingIntervalMS;
    private boolean pollingEnabled = true;

    public EnvironmentConfigManager(String sdkKey, LocalBucketing localBucketing, EventQueueManager eventQueue, DevCycleLocalOptions options) {
        this.sdkKey = sdkKey;
        this.localBucketing = localBucketing;
        this.eventQueueManager = eventQueue;
        configApiClient = new DevCycleLocalApiClient(sdkKey, options).initialize();

        int configPollingIntervalMS = options.getConfigPollingIntervalMS();
        pollingIntervalMS = configPollingIntervalMS >= MIN_INTERVALS_MS ? configPollingIntervalMS
                : DEFAULT_POLL_INTERVAL_MS;

        setupScheduler();
    }

    private void setupScheduler() {
        Runnable getConfigRunnable = new Runnable() {
            public void run() {
                try {
                    if (pollingEnabled) {
                        getConfig();
                    }
                } catch (DevCycleException e) {
                    DevCycleLogger.error("Failed to load config: " + e.getMessage());
                }
            }
        };

        scheduler.scheduleAtFixedRate(getConfigRunnable, 0, this.pollingIntervalMS, TimeUnit.MILLISECONDS);
    }

    public boolean isConfigInitialized() {
        return config != null;
    }

    private ProjectConfig getConfig() throws DevCycleException {
        Call<ProjectConfig> config = this.configApiClient.getConfig(this.sdkKey, this.configETag, this.configLastModified);
        this.config = getResponseWithRetries(config, 1);
        return this.config;
    }

    private ProjectConfig getResponseWithRetries(Call<ProjectConfig> call, int maxRetries) throws DevCycleException {
        // attempt 0 is the initial request, attempt > 0 are all retries
        int attempt = 0;
        do {
            try {
                return getConfigResponse(call);
            } catch (DevCycleException e) {

                attempt++;

                // if out of retries or this is an unauthorized error, throw up exception
                if (!e.isRetryable() || attempt > maxRetries) {
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
        } while (attempt <= maxRetries && pollingEnabled);

        // getting here should not happen, but is technically possible
        ErrorResponse errorResponse = ErrorResponse.builder().build();
        errorResponse.setMessage("Out of retry attempts");
        throw new DevCycleException(HttpResponseCode.SERVER_ERROR, errorResponse);
    }

    private ProjectConfig getConfigResponse(Call<ProjectConfig> call) throws DevCycleException {
        ErrorResponse errorResponse = ErrorResponse.builder().build();
        Response<ProjectConfig> response = null;

        try {
            response = call.execute();

        } catch (JsonParseException badJsonExc) {
            // Got a valid status code but the response body was not valid json,
            // need to ignore this attempt and let the polling retry
            errorResponse.setMessage(badJsonExc.getMessage());
            throw new DevCycleException(HttpResponseCode.NO_CONTENT, errorResponse);
        } catch (IOException e) {
            errorResponse.setMessage(e.getMessage());
            throw new DevCycleException(HttpResponseCode.byCode(500), errorResponse);
        } finally {
            try {
                this.eventQueueManager.queueSDKConfigEvent(call.request(), response, errorResponse);
            } catch (Exception e) {
                // Explicitly ignore - best effort.
            }
        }

        HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());
        errorResponse.setMessage("Unknown error");
        if (response.isSuccessful()) {
            String currentETag = response.headers().get("ETag");
            String lastModified = response.headers().get("Last-Modified");

            ProjectConfig config = response.body();
            try {
                ObjectMapper mapper = new ObjectMapper();
                localBucketing.storeConfig(sdkKey, mapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                if (this.config != null) {
                    DevCycleLogger.error("Unable to parse config with etag: " + currentETag + ". Using cache, etag " + this.configETag);
                    return this.config;
                } else {
                    errorResponse.setMessage(e.getMessage());
                    throw new DevCycleException(HttpResponseCode.SERVER_ERROR, errorResponse);
                }
            }
            this.configETag = currentETag;
            this.configLastModified = lastModified;

            return response.body();
        } else if (httpResponseCode == HttpResponseCode.NOT_MODIFIED) {
            DevCycleLogger.debug("Config not modified, using cache, etag: " + this.configETag);
            return this.config;
        } else {
            if (response.errorBody() != null) {
                try {
                    errorResponse = OBJECT_MAPPER.readValue(response.errorBody().string(), ErrorResponse.class);
                } catch (JsonProcessingException e) {
                    errorResponse.setMessage("Unable to parse error response: " + e.getMessage());
                    throw new DevCycleException(httpResponseCode, errorResponse);
                } catch (IOException e) {
                    errorResponse.setMessage(e.getMessage());
                    throw new DevCycleException(httpResponseCode, errorResponse);
                }
                throw new DevCycleException(httpResponseCode, errorResponse);
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
                    throw new DevCycleException(httpResponseCode, errorResponse);
                }
            }

            throw new DevCycleException(httpResponseCode, errorResponse);
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