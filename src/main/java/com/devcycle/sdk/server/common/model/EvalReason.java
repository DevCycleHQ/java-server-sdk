package com.devcycle.sdk.server.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvalReason {
    @Schema(description = "Evaluation reason", required = true)
    @JsonProperty("reason")
    private String reason;
    
    @Schema(description = "Details")
    @JsonProperty("details")
    private String details;

    @Schema(description = "Target ID")
    @JsonProperty("target_id")
    private String targetId;

    private EvalReason(String reason, String details) {
        this.reason = reason;
        this.details = details;
    }

    public static EvalReason defaultReason(DefaultReasonDetailsEnum details) {
        return new EvalReason("DEFAULT", details.getValue());
    }

    public String getReason() {
        return reason == null ? "UNKNOWN" : reason;
    }

    public enum DefaultReasonDetailsEnum {
        MISSING_CONFIG("Missing Config"),
        USER_NOT_TARGETED("User Not Targeted"),
        VARIABLE_TYPE_MISMATCH("Type Mismatch"),
        ERROR("Error");

        private final String value;

        DefaultReasonDetailsEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
