/*
 * DevCycle Bucketing API
 * Documents the DevCycle Bucketing API which provides and API interface to User Bucketing and for generated SDKs.
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.devcycle.sdk.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Feature {

  @JsonIgnoreProperties(ignoreUnknown = true)

  @Schema(required = true, description = "unique database id")
  @JsonProperty("_id")
  private String id;

  @Schema(required = true, description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id.")
  private String key;

  @Schema(required = true, description = "Feature type")
  private TypeEnum type;

  @Schema(required = true, description = "Bucketed feature variation")
  @JsonProperty("_variation")
  private String variation;

  @Schema(required = true, description = "Bucketed feature variation key")
  @JsonProperty("variationKey")
  private String variationKey;

  @Schema(required = true, description = "Bucketed feature variation name")
  @JsonProperty("variationName")
  private String variationName;

  @Schema(description = "Evaluation reasoning")
  private String evalReason;

  public enum TypeEnum {
    RELEASE("release"),
    EXPERIMENT("experiment"),
    PERMISSION("permission"),
    OPS("ops");

    private final String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static TypeEnum fromValue(String text) {
      for (TypeEnum b : TypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
   }
}
