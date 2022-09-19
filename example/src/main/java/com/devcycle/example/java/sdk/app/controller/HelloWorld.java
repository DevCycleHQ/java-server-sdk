package com.devcycle.example.java.sdk.app.controller;

import com.devcycle.sdk.server.cloud.api.DVCCloudClient;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.common.model.Event;
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
        Variable<String> updateHomePage = dvcCloud.variable(getUser(false), "string-var", "default string");

        String variationValue = updateHomePage.getValue();

        // if the variable "activate-flag" doesn't exist isDefaulted will be true
        model.addAttribute("isDefaultValue", updateHomePage.getIsDefaulted());
        model.addAttribute("variationValue", variationValue);
        return "fragments/flagData :: value ";
    }

    @GetMapping("/local/activateFlag")
    public String homePageActivatedFlagLocal(Model model) {
        Variable<String> updateHomePage = dvcLocal.variable(getUser(true), "string-var", "default string");

        String variationValue = updateHomePage.getValue();

        // if the variable "activate-flag" doesn't exist isDefaulted will be true
        model.addAttribute("isDefaultValue", updateHomePage.getIsDefaulted());
        model.addAttribute("variationValue", variationValue);
        return "fragments/flagData :: value ";
    }

    @GetMapping("/local/track")
    public String trackLocal(Model model) {
        dvcLocal.track(getUser(true), Event.builder().type("java-local-custom").build());
        model.addAttribute("trackSuccessMessage", "Custom event tracked!");
        return "fragments/trackData :: value ";
    }

    private User getUser(boolean isLocal) {
        return isLocal ?
                User.builder()
                        .userId("j_test")
                        .platform("java-local")
                        .build() :
                User.builder()
                        .userId("j_test")
                        .build();
    }
}
