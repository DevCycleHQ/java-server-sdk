package com.devcycle.sdk.server.common.model;

import com.devcycle.sdk.server.local.model.Environment;
import com.devcycle.sdk.server.local.model.Project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfig {

    @Schema(description = "Project Settings")
    @JsonProperty("project")
    private Project project;

    @Schema(description = "Environment Key & ID")
    @JsonProperty("environment")
    private Environment environment;

    @Schema(description = "List of Features in this Project")
    private Object[] features;

    @Schema(description = "List of Variables in this Project")
    private Object[] variables;

    @Schema(description = "Audiences in this Project indexed by ID")
    private Object audiences;

    @Schema(description = "Variable Hashes for all Variables in this Project")
    private Object variableHashes;

    @Schema(description = "SSE Configuration")
    private SSE sse;

    public Project getProject() {
        return project;
    }

    public Environment getEnvironment() {
        return environment;
    }
}

