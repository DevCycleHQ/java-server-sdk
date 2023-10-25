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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevCycleUser {
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
    private Map<String, Object> customData;

    @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
    @JsonProperty("privateCustomData")
    private Map<String, Object> privateCustomData;

    @Schema(description = "Date the user was created, Unix epoch timestamp format")
    @JsonProperty("createdDate")
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long createdDate;

    @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
    @JsonProperty("lastSeenDate")
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
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

    static void setCustomValue(Map<String, Object> customData, String key, Value value) {
        // Only support boolean, number, and string types for custom data values
        // ignore all other data
        if (customData != null && key != null && value != null) {
            if (value.isBoolean()) {
                customData.put(key, value.asBoolean());
            } else if (value.isNumber()) {
                customData.put(key, value.asDouble());
            } else if (value.isString()) {
                customData.put(key, value.asString());
            }
        }
    }

    /**
     * Create a DevCycleUser from an EvaluationContext
     *
     * @param ctx A context to load a targeting key and user data from
     * @return An initialized DevCycleUser with data from the context
     * @throws TargetingKeyMissingError if the targeting key or user_id attribute is not set
     */
    public static DevCycleUser createUserFromContext(EvaluationContext ctx) {
        String userId = "";

        if (ctx != null && ctx.getTargetingKey() != null) {
            userId = ctx.getTargetingKey();
        } else if (ctx != null && ctx.getValue("user_id") != null) {
            userId = ctx.getValue("user_id").asString();
        }

        if (userId == null || userId.isEmpty()) {
            throw new TargetingKeyMissingError();
        }

        DevCycleUser user = DevCycleUser.builder().userId(userId).build();

        Map<String, Object> customData = new LinkedHashMap<>();
        Map<String, Object> privateCustomData = new LinkedHashMap<>();

        for (String key : ctx.keySet()) {
            if (key.equals("user_id")) {
                continue;
            }

            Value value = ctx.getValue(key);

            if (key.equals("email") && value.isString()) {
                user.setEmail(value.asString());
            } else if (key.equals("name") && value.isString()) {
                user.setName(value.asString());
            } else if (key.equals("language") && value.isString()) {
                user.setLanguage(value.asString());
            } else if (key.equals("country") && value.isString()) {
                user.setCountry(value.asString());
            } else if (key.equals("appVersion") && value.isString()) {
                user.setAppVersion(value.asString());
            } else if (key.equals("appBuild") && value.isString()) {
                user.setAppBuild(value.asString());
            } else if (key.equals("customData") && value.isStructure()) {
                Structure customDataStructure = value.asStructure();
                for (String dataKey : customDataStructure.keySet()) {
                    setCustomValue(customData, dataKey, customDataStructure.getValue(dataKey));
                }
            } else if (key.equals("privateCustomData") && value.isStructure()) {
                Structure privateDataStructure = value.asStructure();
                for (String dataKey : privateDataStructure.keySet()) {
                    setCustomValue(privateCustomData, dataKey, privateDataStructure.getValue(dataKey));
                }
            } else {
                setCustomValue(customData, key, value);
            }
        }

        if (!customData.isEmpty()) {
            user.setCustomData(customData);
        }

        if (!privateCustomData.isEmpty()) {
            user.setPrivateCustomData(privateCustomData);
        }

        return user;
    }
}