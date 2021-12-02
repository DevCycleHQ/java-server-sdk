package com.devcycle.example.java.sdk.app.configuration;

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

    private String serverKey;

    @Bean("devcycleServerKey")
    public String getServerKey() {
        return serverKey;
    }
}
