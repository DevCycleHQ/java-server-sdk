package com.devcycle.sdk.server.common.model;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

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
public class Variable<T> {
    @Schema(required = true, description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id.")
    private String key;

    @Schema(required = true, description = "Variable value can be a string, number, boolean, or JSON")
    private T value;

    @Schema(required = true, description = "Variable type")
    private TypeEnum type;

    @Schema(required = true, description = "Variable default value")
    private T defaultValue;

    @Builder.Default
    private Boolean isDefaulted = false;

    @Builder.Default
    @Deprecated()
    private String evalReason = null;

    @Schema(description = "Evaluation reason")
    private EvalReason eval;

    public enum TypeEnum {
        STRING("String"),
        BOOLEAN("Boolean"),
        NUMBER("Number"),
        JSON("JSON");

        private final String value;

        TypeEnum(String value) {
            this.value = value;
        }

        public static TypeEnum fromClass(Class<?> clazz) {
            if (clazz == LinkedHashMap.class || clazz == HashMap.class) {
                return JSON;
            } else if (clazz == Boolean.class) {
                return BOOLEAN;
            } else if (clazz == Integer.class || clazz == Double.class || clazz == Float.class) {
                return NUMBER;
            } else if (clazz == String.class) {
                return STRING;
            } else {
                return null;
            }
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
