package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.IDVCOptions;
import com.devcycle.sdk.server.common.logging.DVCLogger;
import com.devcycle.sdk.server.common.logging.IDVCLogger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
public class DVCLocalOptions implements IDVCOptions {
    private int configRequestTimeoutMs = 10000;

    private int configPollingIntervalMS = 30000;

    private String configCdnBaseUrl = "https://config-cdn.devcycle.com/";

    private String eventsApiBaseUrl = "https://events.devcycle.com/";

    private int eventFlushIntervalMS = 10000;

    private int flushEventQueueSize = 1000;

    private int maxEventQueueSize = 2000;

    private int eventRequestChunkSize = 100;

    private boolean disableAutomaticEventLogging = false;

    @JsonIgnore
    private IDVCLogger customLogger = null;

    public int getConfigPollingIntervalMS(int configPollingIntervalMs, int configPollingIntervalMS) {
        if (configPollingIntervalMS > 0) {
            return configPollingIntervalMS;
        } else if (configPollingIntervalMs > 0) {
            return configPollingIntervalMs;
        } else {
            return this.configPollingIntervalMS;
        }
    }

    private boolean disableCustomEventLogging = false;

    @Builder()
    public DVCLocalOptions(
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
            IDVCLogger customLogger
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

        if (this.flushEventQueueSize >= this.maxEventQueueSize) {
            DVCLogger.warning("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.flushEventQueueSize = this.maxEventQueueSize - 1;
        }

        if (this.eventRequestChunkSize > this.flushEventQueueSize) {
            DVCLogger.warning("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than flushEventQueueSize: " + this.flushEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.eventRequestChunkSize > this.maxEventQueueSize) {
            DVCLogger.warning("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.flushEventQueueSize > 20000) {
            DVCLogger.warning("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than 20,000");
            this.flushEventQueueSize = 20000;
        }

        if (this.maxEventQueueSize > 20000) {
            DVCLogger.warning("maxEventQueueSize: " + this.maxEventQueueSize + " must be smaller than 20,000");
            this.maxEventQueueSize = 20000;
        }
    }
}
