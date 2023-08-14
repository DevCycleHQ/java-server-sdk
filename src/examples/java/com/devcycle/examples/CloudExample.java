package com.devcycle.examples;

import com.devcycle.sdk.server.cloud.api.DVCCloudClient;
import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.User;

public class CloudExample {
    public static String VARIABLE_KEY = "test-boolean-variable";

    public static void main(String[] args) throws InterruptedException {
        String server_sdk_key = System.getenv("DEVCYCLE_SERVER_SDK_KEY");
        if (server_sdk_key == null) {
            System.err.println("Please set the DEVCYCLE_SERVER_SDK_KEY environment variable");
            System.exit(1);
        }

        // Create user object
        User user = User.builder()
                .userId("SOME_USER_ID")
                .build();

        // The default value can be of type string, boolean, number, or JSON
        Boolean defaultValue = false;

        DVCCloudOptions dvcOptions = DVCCloudOptions.builder().build();

        // Initialize DevCycle Client
        DVCCloudClient dvcClient = new DVCCloudClient(server_sdk_key, dvcOptions);

        // Fetch variable values using the identifier key, with a default value and user
        // object
        // The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = false;
        try {
            variableValue = dvcClient.variableValue(user, VARIABLE_KEY, defaultValue);
        } catch(DVCException e) {
            System.err.println("Error fetching variable value: " + e.getMessage());
            System.exit(1);
        }

        // Use variable value
        if (variableValue) {
            System.err.println("feature is enabled");
        } else {
            System.err.println("feature is NOT enabled");
        }
    }
}
