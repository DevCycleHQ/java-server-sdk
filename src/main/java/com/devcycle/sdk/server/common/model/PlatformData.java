package com.devcycle.sdk.server.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PlatformData {
    public PlatformData(String platform, String platformVersion, SdkTypeEnum sdkType, String sdkVersion, String hostname) {
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.sdkType = sdkType;
        this.sdkVersion = sdkVersion;
        try {
            this.hostname = hostname != null ? hostname : InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.out.println("Error getting hostname: " + e.getMessage());
            this.hostname = "";
        }
    }

    @Schema(description = "Platform the SDK is running on")
    @Builder.Default
    private String platform = "Java";

    @Schema(description = "Version of the platform the SDK is running on")
    @Builder.Default
    private String platformVersion = System.getProperty("java.version");

    @Schema(description = "DevCycle SDK type")
    @Builder.Default
    private PlatformData.SdkTypeEnum sdkType = PlatformData.SdkTypeEnum.SERVER;

    @Schema(description = "DevCycle SDK Version")
    @Builder.Default
    private String sdkVersion = "1.3.0";

    @Schema(description = "Hostname where the SDK is running")
    private String hostname;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode platformData = mapper.createObjectNode();
        platformData.put("platform", platform);
        platformData.put("platformVersion", platformVersion);
        platformData.put("sdkType", sdkType.toString());
        platformData.put("sdkVersion", sdkVersion);
        platformData.put("hostname", hostname);

        String platformDataString = null;
        try {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            platformDataString = mapper.writeValueAsString(platformData);
        } catch (JsonProcessingException e) {
            System.out.println("Error reading platformData: " + e.getMessage());
        }
        return platformDataString;
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
