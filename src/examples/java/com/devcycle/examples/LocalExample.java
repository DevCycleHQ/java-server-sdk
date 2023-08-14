package com.devcycle.examples;

import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.common.model.DevCycleUser;

public class LocalExample {
    public static String VARIABLE_KEY = "test-boolean-variable";

    public static void main(String[] args) throws InterruptedException {
        String server_sdk_key = System.getenv("DEVCYCLE_SERVER_SDK_KEY");
        if (server_sdk_key == null) {
            System.err.println("Please set the DEVCYCLE_SERVER_SDK_KEY environment variable");
            System.exit(1);
        }

        // Create user object
        DevCycleUser user = DevCycleUser.builder()
                .userId("SOME_USER_ID")
                .build();

        // The default value can be of type string, boolean, number, or JSON
        Boolean defaultValue = false;

        DevCycleLocalOptions dvcOptions = DevCycleLocalOptions.builder().configPollingIntervalMs(60000)
                .disableAutomaticEventLogging(false).disableCustomEventLogging(false).build();

        // Initialize DevCycle Client
        DevCycleLocalClient dvcClient = new DevCycleLocalClient(server_sdk_key, dvcOptions);

        for (int i = 0; i < 10; i++) {
            if(dvcClient.isInitialized()) {
                break;
            }
            Thread.sleep(500);
        }

        // Fetch variable values using the identifier key, with a default value and user
        // object
        // The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = dvcClient.variableValue(user, VARIABLE_KEY, defaultValue);

        // Use variable value
        if (variableValue) {
            System.err.println("feature is enabled");
        } else {
            System.err.println("feature is NOT enabled");
        }
    }
}
