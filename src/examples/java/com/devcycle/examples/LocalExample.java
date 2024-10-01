package com.devcycle.examples;

import com.devcycle.sdk.server.common.logging.SimpleDevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;

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

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configPollingIntervalMS(60000)
                .customLogger(new SimpleDevCycleLogger(SimpleDevCycleLogger.Level.DEBUG))
                .enableBetaRealtimeUpdates(true)
                .build();

        // Initialize DevCycle Client
        DevCycleLocalClient client = new DevCycleLocalClient(server_sdk_key, options);

        for (int i = 0; i < 10; i++) {
            if (client.isInitialized()) {
                break;
            }
            Thread.sleep(500);
        }

        // Fetch variable values using the identifier key, with a default value and user
        // object
        // The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = client.variableValue(user, VARIABLE_KEY, defaultValue);

        // Use variable value
        if (variableValue) {
            System.out.println("feature is enabled");
        } else {
            System.out.println("feature is NOT enabled");
        }
        Thread.sleep(10000);
    }
}
