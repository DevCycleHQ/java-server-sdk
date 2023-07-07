package com.devcycle.sdk.server.cloud.model;

import com.devcycle.sdk.server.common.api.IRestOptions;
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

    @Builder.Default
    private IRestOptions restOptions = null;

    public static class DVCCloudOptionsBuilder implements IDVCOptions { }
}
