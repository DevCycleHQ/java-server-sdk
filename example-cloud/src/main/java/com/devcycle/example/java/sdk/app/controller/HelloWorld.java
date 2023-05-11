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
    public String homePageActivatedFlagValue(Model model) {
        String variableKey = "string-var";
        // if the variable "string-var" doesn't exist or is not applicable for the user, the default value will be returned
        try {
            String updateHomePageValue = dvcCloud.variableValue(getUser(), variableKey, "default string");
            model.addAttribute("variableKey", variableKey);
            model.addAttribute("variationValue", updateHomePageValue);
        }catch (DVCException e){
            System.out.println("DVCException: " + e.getMessage());
        }
        return "fragments/flagData :: value ";
    }

    @GetMapping("/cloud/activateFlagDetails")
    public String homePageActivatedFlagDetails(Model model) {
        String variableKey = "string-var";
        try{
            Variable<String> updateHomePageVariable = dvcCloud.variable(getUser(), variableKey, "default string");

            // if the variable "string-var" doesn't exist isDefaulted will be true
            model.addAttribute("isDefaultValue", updateHomePageVariable.getIsDefaulted());
            model.addAttribute("variableKey", variableKey);
            model.addAttribute("variationValue", updateHomePageVariable.getValue());
        }catch (DVCException e){
            System.out.println("DVCException: " + e.getMessage());
        }
        return "fragments/flagDataDetails :: value ";
    }

    @GetMapping("/cloud/track")
    public String trackCloud(Model model) {
        String response = "";
        try {
            dvcCloud.track(getUser(), Event.builder().type("java-cloud-custom").build());
            response = "java-cloud-custom tracked!";
        } catch(DVCException e) {
            response = "Error tracking custom event: " + e.getMessage();
        }
        model.addAttribute("trackSuccessMessage", "Cloud custom event tracked!");
        model.addAttribute("trackResponse", response);
        return "fragments/trackData :: value ";
    }

    private User getUser() {
        return User.builder()
                .userId("java_example_cloud")
                .build();
    }
}
