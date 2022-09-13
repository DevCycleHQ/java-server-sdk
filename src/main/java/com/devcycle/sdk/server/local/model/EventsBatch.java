package com.devcycle.sdk.server.local.model;

import lombok.Builder;

@Builder
public class EventsBatch {
    public FlushPayload.Record[] batch;
}

