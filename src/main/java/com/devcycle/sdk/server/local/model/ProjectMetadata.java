package com.devcycle.sdk.server.local.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMetadata {
    @JsonProperty("id")
    public String id;
    @JsonProperty("key")
    public String key;
}
