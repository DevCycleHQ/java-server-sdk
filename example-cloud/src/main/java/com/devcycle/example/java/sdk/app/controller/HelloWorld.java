package com.devcycle.example.cloud.java.sdk.app.controller;

import com.devcycle.sdk.server.cloud.api.DVCCloudClient;
import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
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

    DVCCloudClient dvcCloud;

    private DVCCloudOptions dvcCloudOptions = DVCCloudOptions.builder().enableEdgeDB(false).build();

    public HelloWorld(@Qualifier("devcycleSDKKey") String sdkKey) {
        dvcCloud = new DVCCloudClient(sdkKey, dvcCloudOptions);
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

    @GetMapping("/cloud/activateFlag")
    public String homePageActivatedFlag(Model model) {
        Variable<String> updateHomePage = dvcCloud.variable(getUser(), "string-var", "default string");

        String variationValue = updateHomePage.getValue();

        // if the variable "activate-flag" doesn't exist isDefaulted will be true
        model.addAttribute("isDefaultValue", updateHomePage.getIsDefaulted());
        model.addAttribute("variationValue", variationValue);
        return "fragments/flagData :: value ";
    }

    @GetMapping("/cloud/track")
    public String trackCloud(Model model) {
        DVCResponse response = null;
        try {
            response = dvcCloud.track(getUser(), Event.builder().type("java-cloud-custom").build());
        } catch(DVCException e) {
            System.out.println("Error tracking custom event: " + e.getMessage());
        }
        model.addAttribute("trackSuccessMessage", "Cloud custom event tracked!");
        model.addAttribute("trackResponse", response.getMessage());
        return "fragments/trackData :: value ";
    }

    private User getUser() {
        return User.builder()
                .userId("java_example_cloud")
                .build();
    }
}
