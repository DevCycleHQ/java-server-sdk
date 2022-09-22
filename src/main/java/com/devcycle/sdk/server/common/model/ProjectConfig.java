package com.devcycle.sdk.server.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectConfig {

  @JsonIgnoreProperties(ignoreUnknown = false)

  @Schema(description = "Project Settings")
  private Object project;

  @Schema(description = "Environment Key & ID")
  private Object environment;

  @Schema(description = "List of Features in this Project")
  private Object[] features;

  @Schema(description = "List of Variables in this Project")
  private Object[] variables;

  @Schema(description = "Variable Hashes for all Variables in this Project")
  private Object variableHashes;
}