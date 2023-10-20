package com.devcycle.examples;

import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenFeatureExample {
    public static String VARIABLE_KEY = "test-boolean-variable";

    public static void main(String[] args) throws InterruptedException {
        String server_sdk_key = System.getenv("DEVCYCLE_SERVER_SDK_KEY");
        if (server_sdk_key == null) {
            System.err.println("Please set the DEVCYCLE_SERVER_SDK_KEY environment variable");
            System.exit(1);
        }

        DevCycleLocalOptions options = DevCycleLocalOptions.builder().configPollingIntervalMs(60000)
                .disableAutomaticEventLogging(false).disableCustomEventLogging(false).build();

        // Initialize DevCycle Client
        DevCycleLocalClient devCycleClient = new DevCycleLocalClient(server_sdk_key, options);

        OpenFeatureAPI api = OpenFeatureAPI.getInstance();

        for (int i = 0; i < 10; i++) {
            if(devCycleClient.isInitialized()) {
                break;
            }
            Thread.sleep(500);
        }

        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("email", new Value("test-user@domain.com"));
        apiAttrs.put("country", new Value("US"));

        EvaluationContext ctx = new ImmutableContext("test-1234", apiAttrs);

        // The default value can be of type string, boolean, number, or JSON
        Boolean defaultValue = false;

        api.setProvider(new DevCycleProvider(devCycleClient));

        // Fetch variable values using the identifier key, with a default value and user
        // object. The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = api.getClient().getBooleanValue(VARIABLE_KEY, defaultValue, ctx);

        // Use variable value
        if (variableValue) {
            System.err.println("feature is enabled");
        } else {
            System.err.println("feature is NOT enabled");
        }
    }
}
