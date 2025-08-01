package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigMetadata {

    public final ProjectMetadata project;
    public final EnvironmentMetadata environment;

    @JsonCreator
    public ConfigMetadata(@JsonProperty("project") ProjectMetadata project, @JsonProperty("environment") EnvironmentMetadata environment) {
        this.project = project;
        this.environment = environment;
    }
}
