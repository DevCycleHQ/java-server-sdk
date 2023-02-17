package com.devcycle.example.local.java.sdk.app.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "devcycle")
public class HelloWorldConfiguration {

    private String sdkKey;

    @Bean("devcycleSDKKey")
    public String getSdkKey() {
        return sdkKey;
    }
}
