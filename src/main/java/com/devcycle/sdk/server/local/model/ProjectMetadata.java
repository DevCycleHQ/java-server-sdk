package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectMetadata {
    public final String id;
    public final String key;

    @JsonCreator
    public ProjectMetadata(@JsonProperty("id") String id, @JsonProperty("key") String key) {
        this.id = id;
        this.key = key;
    }
}
