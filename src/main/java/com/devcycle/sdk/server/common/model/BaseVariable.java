package com.devcycle.sdk.server.common.model;

import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
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
public class BaseVariable {
    @Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    private String id;

    @Schema(required = true, description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id.")
    private String key;

    @Schema(required = true, description = "Variable type")
    private TypeEnum type;

    @Schema(required = true, description = "Variable value can be a string, number, boolean, or JSON")
    private Object value;

    @Schema(description = "Evaluation reason")
    private EvalReason eval;
}
