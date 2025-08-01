package com.devcycle.sdk.server.cloud.model;

import com.devcycle.sdk.server.common.api.IRestOptions;
import com.devcycle.sdk.server.common.logging.IDevCycleLogger;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.IDevCycleOptions;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DevCycleCloudOptions {
    @Builder.Default
    private Boolean enableEdgeDB = false;

    @Builder.Default
    private String baseURLOverride = null;

    @Builder.Default
    private IDevCycleLogger customLogger = null;

    @Builder.Default
    private IRestOptions restOptions = null;

    @Builder.Default
    private List<EvalHook> hooks = new ArrayList<>();

    public static class DevCycleCloudOptionsBuilder implements IDevCycleOptions {
    }
}
