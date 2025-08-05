package com.devcycle.sdk.server.local.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigMetadata {

    public ProjectMetadata project;
    public EnvironmentMetadata environment;

}
