package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.IDVCOptions;

import lombok.Builder;
import lombok.Data;

@Data
public class DVCLocalOptions implements IDVCOptions {
    private int configRequestTimeoutMs = 10000;

    private int configPollingIntervalMs = 30000;

    private String configCdnBaseUrl = "https://config-cdn.devcycle.com/";

    private String eventsApiBaseUrl = "https://events.devcycle.com/";

    private int eventFlushIntervalMS = 10000;

    private int flushEventQueueSize = 1000;

    private int maxEventQueueSize = 2000;

    private int eventRequestChunkSize = 100;

    private boolean disableAutomaticEventLogging = false;

    private boolean disableCustomEventLogging = false;

    @Builder()
    public DVCLocalOptions(
            int configRequestTimeoutMs,
            int configPollingIntervalMs,
            String configCdnBaseUrl,
            String eventsApiBaseUrl,
            int eventFlushIntervalMS,
            int flushEventQueueSize,
            int maxEventQueueSize,
            int eventRequestChunkSize,
            boolean disableAutomaticEventLogging,
            boolean disableCustomEventLogging
    ) {
        this.configRequestTimeoutMs = configRequestTimeoutMs > 0 ? configRequestTimeoutMs : this.configRequestTimeoutMs;
        this.configPollingIntervalMs = configPollingIntervalMs > 0 ? configPollingIntervalMs : this.configPollingIntervalMs;
        this.configCdnBaseUrl = configCdnBaseUrl != null ? configCdnBaseUrl : this.configCdnBaseUrl;
        this.eventsApiBaseUrl = eventsApiBaseUrl != null ? eventsApiBaseUrl : this.eventsApiBaseUrl;
        this.eventFlushIntervalMS = eventFlushIntervalMS > 0 ? eventFlushIntervalMS : this.eventFlushIntervalMS;
        this.flushEventQueueSize = flushEventQueueSize > 0 ? flushEventQueueSize : this.flushEventQueueSize;
        this.maxEventQueueSize = maxEventQueueSize > 0 ? maxEventQueueSize : this.maxEventQueueSize;
        this.eventRequestChunkSize = eventRequestChunkSize > 0 ? eventRequestChunkSize : this.eventRequestChunkSize;
        this.disableAutomaticEventLogging = disableAutomaticEventLogging;
        this.disableCustomEventLogging = disableCustomEventLogging;

        if (this.flushEventQueueSize >= this.maxEventQueueSize) {
            System.out.println("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.flushEventQueueSize = this.maxEventQueueSize - 1;
        }

        if (this.eventRequestChunkSize > this.flushEventQueueSize) {
            System.out.println("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than flushEventQueueSize: " + this.flushEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.eventRequestChunkSize > this.maxEventQueueSize) {
            System.out.println("eventRequestChunkSize: " + this.eventRequestChunkSize + " must be smaller than maxEventQueueSize: " + this.maxEventQueueSize);
            this.eventRequestChunkSize = 100;
        }

        if (this.flushEventQueueSize > 20000) {
            System.out.println("flushEventQueueSize: " + this.flushEventQueueSize + " must be smaller than 20,000");
            this.flushEventQueueSize = 20000;
        }

        if (this.maxEventQueueSize > 20000) {
            System.out.println("maxEventQueueSize: " + this.maxEventQueueSize + " must be smaller than 20,000");
            this.maxEventQueueSize = 20000;
        }
    }
}
