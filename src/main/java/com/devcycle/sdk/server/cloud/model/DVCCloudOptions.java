package com.devcycle.sdk.server.cloud.model;

import com.devcycle.sdk.server.common.logging.IDVCLogger;
import com.devcycle.sdk.server.common.model.IDVCOptions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DVCCloudOptions {
    @Builder.Default
    private Boolean enableEdgeDB = false;

    @Builder.Default
    private String baseURLOverride = null;

    @Builder.Default
    private IDVCLogger customLogger = null;

    public static class DVCCloudOptionsBuilder implements IDVCOptions { }
}
