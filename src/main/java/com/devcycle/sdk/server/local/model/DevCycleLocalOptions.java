package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.api.IRestOptions;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.logging.IDevCycleLogger;
import com.devcycle.sdk.server.common.model.IDevCycleOptions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
public class DevCycleLocalOptions implements IDevCycleOptions {
    private int configRequestTimeoutMs = 10000;

    private int configPollingIntervalMS = 30000;

    private String configCdnBaseUrl = "https://config-cdn.devcycle.com/";

    private String eventsApiBaseUrl = "https://events.devcycle.com/";

    private int eventFlushIntervalMS = 10000;

    private int flushEventQueueSize = 1000;

    private int maxEventQueueSize = 2000;

    private int eventRequestChunkSize = 100;

    private boolean disableAutomaticEventLogging = false;

    private boolean disableRealtimeUpdates = false;

    /**
     * @deprecated real time updates are enabled by default now
     */
    @Deprecated
    private boolean enableBetaRealtimeUpdates = false;

    @JsonIgnore
    private IDevCycleLogger customLogger = null;
    private boolean disableCustomEventLogging = false;
    @JsonIgnore
    private IRestOptions restOptions = null;

    @Builder()
    public DevCycleLocalOptions(
            int configRequestTimeoutMs,
            @Deprecated
            int configPollingIntervalMs,
            int configPollingIntervalMS,
            String configCdnBaseUrl,
            String eventsApiBaseUrl,
            int eventFlushIntervalMS,
            int flushEventQueueSize,
            int maxEventQueueSize,
            int eventRequestChunkSize,
            boolean disableAutomaticEventLogging,
            boolean disableCustomEventLogging,
            IDevCycleLogger customLogger,
            IRestOptions restOptions,
            @Deprecated 
            boolean enableBetaRealtimeUpdates,
            boolean disableRealtimeUpdates
    ) {
        this.configRequestTimeoutMs = configRequestTimeoutMs > 0 ? configRequestTimeoutMs : this.configRequestTimeoutMs;
        this.configPollingIntervalMS = getConfigPollingIntervalMS(configPollingIntervalMs, configPollingIntervalMS);
        this.configCdnBaseUrl = configCdnBaseUrl != null ? configCdnBaseUrl : this.configCdnBaseUrl;
        this.eventsApiBaseUrl = eventsApiBaseUrl != null ? eventsApiBaseUrl : this.eventsApiBaseUrl;
        this.eventFlushIntervalMS = eventFlushIntervalMS > 0 ? eventFlushIntervalMS : this.eventFlushIntervalMS;
        this.flushEventQueueSize = flushEventQueueSize > 0 ? flushEventQueueSize : this.flushEventQueueSize;
        this.maxEventQueueSize = maxEventQueueSize > 0 ? maxEventQueueSize : this.maxEventQueueSize;
        this.eventRequestChunkSize = eventRequestChunkSize > 0 ? eventRequestChunkSize : this.eventRequestChunkSize;
        this.disableAutomaticEventLogging = disableAutomaticEventLogging;
        this.disableCustomEventLogging = disableCustomEventLogging;
        this.customLogger = customLogger;
        this.restOptions = restOptions;
        this.enableBetaRealtimeUpdates = enableBetaRealtimeUpdates;
        this.disableRealtimeUpdates = disableRealtimeUpdates;

        if (this.flushEventQueueSize >= this.maxEventQueueSize) {
            DevCycleLogger.warning("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.flushEventQueueSize = this.maxEventQueueSize - 1;
        }

        if (this.eventRequestChunkSize > this.flushEventQueueSize) {
            DevCycleLogger.warning("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than flushEventQueueSize: " + this.flushEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.eventRequestChunkSize > this.maxEventQueueSize) {
            DevCycleLogger.warning("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.flushEventQueueSize > 20000) {
            DevCycleLogger.warning("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than 20,000");
            this.flushEventQueueSize = 20000;
        }

        if (this.maxEventQueueSize > 20000) {
            DevCycleLogger.warning("maxEventQueueSize: " + this.maxEventQueueSize + " must be smaller than 20,000");
            this.maxEventQueueSize = 20000;
        }
    }

    public int getConfigPollingIntervalMS(int configPollingIntervalMs, int configPollingIntervalMS) {
        if (configPollingIntervalMS > 0) {
            return configPollingIntervalMS;
        } else if (configPollingIntervalMs > 0) {
            return configPollingIntervalMs;
        } else {
            return this.configPollingIntervalMS;
        }
    }
}
