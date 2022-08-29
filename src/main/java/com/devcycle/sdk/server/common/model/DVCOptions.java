package com.devcycle.sdk.server.common.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DVCOptions {
    private Boolean enableEdgeDB;
}
