package com.devcycle.example.local.java.sdk.app.controller;

import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.DVCResponse;
import com.devcycle.sdk.server.common.model.Event;
import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloWorld {

    DVCLocalClient dvcClient;

    private DVCLocalOptions dvcLocalOptions = DVCLocalOptions.builder()
        .configPollingIntervalMS(10000)
        .configRequestTimeoutMs(5000)
        .eventFlushIntervalMS(5000)
        .build();

    public HelloWorld(@Qualifier("devcycleSDKKey") String sdkKey) {
        dvcClient = new DVCLocalClient(sdkKey, dvcLocalOptions);
    }

    @Value("${spring.application.name}")
    String appName;

    @Value("${spring.application.oops}")
    String defaultValue;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("appName", appName);
        return "home";
    }

    @GetMapping("/local/activateFlag")
    public String homePageActivatedFlagValue(Model model) {
        String variableKey = "string-var";
        // if the variable "string-var" doesn't exist or is not applicable for the user, the default value will be returned
        String updateHomePage = dvcClient.variableValue(getUser(), variableKey, "default string");
        model.addAttribute("variableKey", variableKey);
        model.addAttribute("variationValue", updateHomePage);
        return "fragments/flagData :: value ";
    }

    @GetMapping("/local/activateFlagDetails")
    public String homePageActivatedFlagDetails(Model model) {
        String variableKey = "string-var";
        Variable<String> updateHomePageVariable = dvcClient.variable(getUser(), variableKey, "default string");

        // if the variable "string-var" doesn't exist isDefaulted will be true

        model.addAttribute("isDefaultValue", updateHomePageVariable.getIsDefaulted());
        model.addAttribute("variableKey", variableKey);
        model.addAttribute("variationValue", updateHomePageVariable.getValue());
        return "fragments/flagDataDetails :: value ";
    }

    @GetMapping("/local/track")
    public String trackLocal(Model model) {
        dvcClient.track(getUser(), Event.builder().type("java-local-custom").build());
        model.addAttribute("trackSuccessMessage", "local custom event tracked!");
        model.addAttribute("trackResponse", "java-local-custom tracked!");
        return "fragments/trackData :: value ";
    }

    private User getUser() {
        return User.builder()
                .userId("java_example_local_app")
                .build();
    }
}
