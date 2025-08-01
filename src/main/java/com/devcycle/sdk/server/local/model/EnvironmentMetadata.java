package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EnvironmentMetadata {
    public final String id;
    public final String key;

    @JsonCreator
    public EnvironmentMetadata(@JsonProperty("id") String id, @JsonProperty("key") String key) {
        this.id = id;
        this.key = key;
    }
}
