package com.devcycle.sdk.server.local.model;

import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMetadata {
    @JsonProperty("id")
    public final String id;
    @JsonProperty("key")
    public final String key;
}
