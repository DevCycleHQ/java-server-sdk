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

import com.devcycle.sdk.server.local.utils.LongTimestampDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

  @JsonIgnoreProperties(ignoreUnknown = true)

  @NonNull
  @Schema(required = true, description = "Unique id to identify the user")
  @JsonProperty("user_id")
  private String userId;

  @Schema(description = "User's email used to identify the user on the dashboard / target audiences")
  @JsonProperty("email")
  private String email;

  @Schema(description = "User's name used to identify the user on the dashboard / target audiences")
  @JsonProperty("name")
  private String name;

  @Schema(description = "User's language in ISO 639-1 format")
  @JsonProperty("language")
  private String language;

  @Schema(description = "User's country in ISO 3166 alpha-2 format")
  @JsonProperty("country")
  private String country;

  @Schema(description = "App Version of the running application")
  @JsonProperty("appVersion")
  private String appVersion;

  @Schema(description = "App Build number of the running application")
  @JsonProperty("appBuild")
  private String appBuild;

  @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
  @JsonProperty("customData")
  private Object customData;

  @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
  @JsonProperty("privateCustomData")
  private Object privateCustomData;

  @Schema(description = "Date the user was created, Unix epoch timestamp format")
  @JsonProperty("createdDate")
  @JsonDeserialize(using = LongTimestampDeserializer.class)
  private Long createdDate;

  @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
  @JsonProperty("lastSeenDate")
  @JsonDeserialize(using = LongTimestampDeserializer.class)
  private Long lastSeenDate;

  @Schema(description = "Platform the SDK is running on")
  @Builder.Default
  @JsonProperty("platform")
  private String platform = getPlatformData().getPlatform();

  @Schema(description = "Version of the platform the SDK is running on")
  @Builder.Default
  @JsonProperty("platformVersion")
  private String platformVersion = getPlatformData().getPlatformVersion();

  @Schema(description = "DevCycle SDK type")
  @Builder.Default
  @JsonProperty("sdkType")
  private PlatformData.SdkTypeEnum sdkType = getPlatformData().getSdkType();

  @Schema(description = "DevCycle SDK Version")
  @Builder.Default
  @JsonProperty("sdkVersion")
  private String sdkVersion = getPlatformData().getSdkVersion();

  @Schema(description = "Hostname where the SDK is running")
  @Builder.Default
  @JsonProperty("hostname")
  private String hostname = getPlatformData().getHostname();

  public static PlatformData getPlatformData() {
    return PlatformData.builder().build();
  }
}