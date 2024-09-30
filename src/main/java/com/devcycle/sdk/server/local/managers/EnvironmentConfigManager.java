package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.api.DevCycleLocalApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.eventsource.FaultEvent;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.StartedEvent;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class EnvironmentConfigManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_POLL_INTERVAL_MS = 30000;
    private static final int MIN_INTERVALS_MS = 1000;
    private ScheduledExecutorService scheduler;
    private final IDevCycleApi configApiClient;
    private final LocalBucketing localBucketing;
    private SSEManager sseManager;
    private boolean isSSEConnected = false;
    private final DevCycleLocalOptions options;

    private ProjectConfig config;
    private String configETag = "";
    private String configLastModified = "";

    private final String sdkKey;
    private final int pollingIntervalMS;
    private static final int pollingIntervalSSEMS = 15 * 60 * 60 * 1000;
    private boolean pollingEnabled = true;

    public EnvironmentConfigManager(String sdkKey, LocalBucketing localBucketing, DevCycleLocalOptions options) {
        this.sdkKey = sdkKey;
        this.localBucketing = localBucketing;
        this.options = options;

        configApiClient = new DevCycleLocalApiClient(sdkKey, options).initialize();

        int configPollingIntervalMS = options.getConfigPollingIntervalMS();
        pollingIntervalMS = configPollingIntervalMS >= MIN_INTERVALS_MS ? configPollingIntervalMS
                : DEFAULT_POLL_INTERVAL_MS;

        scheduler = setupScheduler();
        scheduler.scheduleAtFixedRate(getConfigRunnable, 0, this.pollingIntervalMS, TimeUnit.MILLISECONDS);
    }

    private ScheduledExecutorService setupScheduler() {
        return Executors.newScheduledThreadPool(1, new DaemonThreadFactory());
    }

    private Runnable getConfigRunnable = new Runnable() {
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

    public boolean isConfigInitialized() {
        return config != null;
    }

    private ProjectConfig getConfig() throws DevCycleException {
        Call<ProjectConfig> config = this.configApiClient.getConfig(this.sdkKey, this.configETag, this.configLastModified);
        this.config = getResponseWithRetries(config, 1);
        if (this.options.isEnableBetaRealtimeUpdates()) {
            try {
                URI uri = new URI(this.config.getSse().getHostname() + this.config.getSse().getPath());
                if (sseManager == null) {
                    sseManager = new SSEManager(uri);
                }
                sseManager.restart(uri, this::handleSSEMessage, this::handleSSEError, this::handleSSEStarted);
            } catch (URISyntaxException e) {
                DevCycleLogger.warning("Failed to create SSEManager: " + e.getMessage());
            }
        }
        return this.config;
    }

    private Void handleSSEMessage(MessageEvent messageEvent) {
        DevCycleLogger.debug("Received message: " + messageEvent.getData());
        if (!isSSEConnected)
        {
            handleSSEStarted(null);
        }

        String data = messageEvent.getData();
        if (data == null || data.isEmpty() || data.equals("keepalive")) {
            return null;
        }
        try {
            SSEMessage message = OBJECT_MAPPER.readValue(data, SSEMessage.class);
            if (message.getType() == null || message.getType().equals("refetchConfig") || message.getType().isEmpty()) {
                DevCycleLogger.debug("Received refetchConfig message, fetching new config");
                getConfigRunnable.run();
            }
        } catch (JsonProcessingException e) {
            DevCycleLogger.warning("Failed to parse SSE message: " + e.getMessage());
        }
        return null;
    }

    private Void handleSSEError(FaultEvent faultEvent) {
        DevCycleLogger.warning("Received error: " + faultEvent.getCause());
        return null;
    }

    private Void handleSSEStarted(StartedEvent startedEvent) {
        isSSEConnected = true;
        DevCycleLogger.debug("SSE Connected - setting polling interval to " + pollingIntervalSSEMS);
        scheduler.close();
        scheduler = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());
        scheduler.scheduleAtFixedRate(getConfigRunnable, 0, pollingIntervalSSEMS, TimeUnit.MILLISECONDS);
        return null;
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
        Response<ProjectConfig> response;

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
        }

        HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());
        errorResponse.setMessage("Unknown error");

        if (response.isSuccessful()) {
            String currentETag = response.headers().get("ETag");
            String headerLastModified = response.headers().get("Last-Modified");

            if (!this.configLastModified.isEmpty() && headerLastModified != null && !headerLastModified.isEmpty()) {
                ZonedDateTime parsedLastModified = ZonedDateTime.parse(
                        headerLastModified,
                        DateTimeFormatter.RFC_1123_DATE_TIME
                );
                ZonedDateTime configLastModified = ZonedDateTime.parse(
                        this.configLastModified,
                        DateTimeFormatter.RFC_1123_DATE_TIME
                );

                if (parsedLastModified.isBefore(configLastModified)) {
                    DevCycleLogger.warning("Received a config with last-modified header before the current stored timestamp. Not saving config.");
                    return this.config;
                }
            }

            ProjectConfig config = response.body();
            try {
                ObjectMapper mapper = new ObjectMapper();
                localBucketing.storeConfig(sdkKey, mapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                if (this.config != null) {
                    DevCycleLogger.error("Unable to parse config with etag: " + currentETag + ". Using cache, etag " + this.configETag + " last-modified: " + this.configLastModified);
                    return this.config;
                } else {
                    errorResponse.setMessage(e.getMessage());
                    throw new DevCycleException(HttpResponseCode.SERVER_ERROR, errorResponse);
                }
            }
            this.configETag = currentETag;
            this.configLastModified = headerLastModified;
            return response.body();
        } else if (httpResponseCode == HttpResponseCode.NOT_MODIFIED) {
            DevCycleLogger.debug("Config not modified, using cache, etag: " + this.configETag + " last-modified: " + this.configLastModified);
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
        sseManager.close();
        stopPolling();
    }
}