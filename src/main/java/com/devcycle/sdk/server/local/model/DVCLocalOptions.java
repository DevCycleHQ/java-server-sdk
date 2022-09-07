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

    public static class DVCLocalOptionsBuilder implements IDVCOptions { }
}
