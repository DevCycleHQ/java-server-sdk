package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.IDVCOptions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DVCLocalOptions {
    @Builder.Default
    private int configRequestTimeoutMs = 10000;

    @Builder.Default
    private int configPollingIntervalMs = 30000;

    @Builder.Default
    private String configCdnBaseUrl = "https://config-cdn.devcycle.com/";

    @Builder.Default
    private String eventsApiBaseUrl = "https://events.devcycle.com/";

    @Builder.Default
    private int eventFlushIntervalMS = 10000;

    @Builder.Default
    private int flushEventQueueSize = 1000;

    @Builder.Default
    private int maxEventQueueSize = 2000;

    @Builder.Default
    private boolean disableAutomaticEventLogging = false;

    @Builder.Default
    private boolean disableCustomEventLogging = false;

    public static class DVCLocalOptionsBuilder implements IDVCOptions { }
}
