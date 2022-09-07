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

package com.devcycle.sdk.server.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

  @JsonIgnoreProperties(ignoreUnknown = true)

  @NonNull
  @Schema(required = true, description = "Unique id to identify the user")
  @JsonProperty("user_id")
  private String userId;

  @Schema(description = "User's email used to identify the user on the dashboard / target audiences")
  private String email;

  @Schema(description = "User's name used to identify the user on the dashboard / target audiences")
  private String name;

  @Schema(description = "User's language in ISO 639-1 format")
  private String language;

  @Schema(description = "User's country in ISO 3166 alpha-2 format")
  private String country;

  @Schema(description = "App Version of the running application")
  private String appVersion;

  @Schema(description = "App Build number of the running application")
  private String appBuild;

  @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
  private Object customData;

  @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
  private Object privateCustomData;

  @Schema(description = "Date the user was created, Unix epoch timestamp format")
  private Long createdDate;

  @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
  private Long lastSeenDate;

  @Schema(description = "Platform the SDK is running on")
  @Builder.Default
  private String platform = "";

  @Schema(description = "Version of the platform the SDK is running on")
  @Builder.Default
  private String platformVersion = "";

  @Schema(description = "User's device model")
  @Builder.Default
  private String deviceModel = "";

  @Schema(description = "DevCycle SDK type")
  @Builder.Default
  private SdkTypeEnum sdkType = SdkTypeEnum.SERVER;

  @Schema(description = "DevCycle SDK Version")
  @Builder.Default
  private String sdkVersion = "";

  public String getPlatformDataString() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode platformData = mapper.createObjectNode();
    platformData.put("platform", platform);
    platformData.put("platformVersion", platformVersion);
    platformData.put("deviceModel", deviceModel);
    platformData.put("sdkType", sdkType.toString());
    platformData.put("sdkVersion", sdkVersion);

    return mapper.writeValueAsString(platformData);
  }

  public enum SdkTypeEnum {
    API("api"),
    SERVER("server");

    private final String value;

    SdkTypeEnum(String value) {
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

    public static SdkTypeEnum fromValue(String text) {
      for (SdkTypeEnum b : SdkTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
}
