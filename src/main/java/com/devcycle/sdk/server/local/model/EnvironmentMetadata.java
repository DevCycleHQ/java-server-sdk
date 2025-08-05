package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentMetadata {
    @JsonProperty("id")
    public final String id;
    @JsonProperty("key")
    public final String key;
}
