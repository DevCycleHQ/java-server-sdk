package com.devcycle.example.java.sdk.app.controller;

import com.devcycle.sdk.server.cloud.api.DVCCloudClient;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
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
    DVCLocalClient dvcLocal;

    public HelloWorld(@Qualifier("devcycleServerKey") String serverKey) {
        dvcCloud = new DVCCloudClient(serverKey);
        dvcLocal = new DVCLocalClient(serverKey);
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

    @GetMapping("/local/activateFlag")
    public String homePageActivatedFlagLocal(Model model) {
        Variable<String> updateHomePage = dvcLocal.variable(getUser(), "string-var", "default string");

        String variationValue = updateHomePage.getValue();

        // if the variable "activate-flag" doesn't exist isDefaulted will be true
        model.addAttribute("isDefaultValue", updateHomePage.getIsDefaulted());
        model.addAttribute("variationValue", variationValue);
        return "fragments/flagData :: value ";
    }

    @GetMapping("/local/track")
    public String trackLocal(Model model) {
        dvcLocal.track(getUser(), Event.builder().type("java-local-custom").build());
        model.addAttribute("trackSuccessMessage", "Local custom event tracked!");
        return "fragments/trackData :: value ";
    }

    private User getUser() {
        return User.builder()
                .userId("j_test")
                .build();
    }
}
