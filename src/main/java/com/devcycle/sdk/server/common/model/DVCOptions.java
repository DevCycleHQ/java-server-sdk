package com.devcycle.sdk.server.common.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DVCOptions {
    @Builder.Default
    private Boolean enableCloudBucketing = false;

    private Boolean enableEdgeDB;
    
    private int configRequestTimeoutMs;
    
    private int configPollingIntervalMs;
    
    private String configCdnBaseUrl;
}
