package com.devcycle.examples;

import com.devcycle.sdk.server.cloud.api.DevCycleCloudClient;
import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.common.model.DevCycleUser;

public class CloudExample {
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

        DevCycleCloudOptions options = DevCycleCloudOptions.builder().build();

        // Initialize DevCycle Client
        DevCycleCloudClient client = new DevCycleCloudClient(server_sdk_key, options);

        // Fetch variable values using the identifier key, with a default value and user
        // object
        // The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = false;
        try {
            variableValue = client.variableValue(user, VARIABLE_KEY, defaultValue);
        } catch (IllegalArgumentException e) {
            System.err.println("Error fetching variable value: " + e.getMessage());
            System.exit(1);
        }

        // Use variable value
        if (variableValue) {
            System.out.println("feature is enabled");
        } else {
            System.out.println("feature is NOT enabled");
        }
    }
}
