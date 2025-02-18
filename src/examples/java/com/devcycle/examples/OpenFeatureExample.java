package com.devcycle.examples;

import com.devcycle.sdk.server.common.logging.SimpleDevCycleLogger;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import dev.openfeature.sdk.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenFeatureExample {
    public static void main(String[] args) throws InterruptedException {
        String server_sdk_key = System.getenv("DEVCYCLE_SERVER_SDK_KEY");
        if (server_sdk_key == null) {
            System.err.println("Please set the DEVCYCLE_SERVER_SDK_KEY environment variable");
            System.exit(1);
        }

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .customLogger(new SimpleDevCycleLogger(SimpleDevCycleLogger.Level.DEBUG))
                .build();

        // Initialize DevCycle Client
        DevCycleLocalClient devCycleClient = new DevCycleLocalClient(server_sdk_key, options);

        // Setup OpenFeature with the DevCycle Provider
        OpenFeatureAPI api = OpenFeatureAPI.getInstance();
        api.setProviderAndWait(devCycleClient.getOpenFeatureProvider());

        Client openFeatureClient = api.getClient();

        // Create the evaluation context to use for fetching variable values
        MutableContext context = new MutableContext("test-1234");
        context.add("email", "test-user@domain.com");
        context.add("name", "Test User");
        context.add("language", "en");
        context.add("country", "CA");
        context.add("appVersion", "1.0.0");
        context.add("appBuild", 1.0);
        context.add("deviceModel", "Macbook");

        // Add Devcycle Custom Data values
        Map<String, Object> customData = new LinkedHashMap<>();
        customData.put("custom", "value");
        context.add("customData", Structure.mapToStructure(customData));

        // Add Devcycle Private Custom Data values
        Map<String, Object> privateCustomData = new LinkedHashMap<>();
        privateCustomData.put("private", "data");
        context.add("privateCustomData", Structure.mapToStructure(privateCustomData));

        // The default value can be of type string, boolean, number, or JSON
        Boolean defaultValue = false;

        // Fetch variable values using the identifier key, with a default value and user
        // object. The default value can be of type string, boolean, number, or JSON
        Boolean variableValue = openFeatureClient.getBooleanValue("test-boolean-variable", defaultValue, context);

        // Use variable value
        if (variableValue) {
            System.out.println("feature is enabled");
        } else {
            System.out.println("feature is NOT enabled");
        }

        // Default JSON objects must be a map of string to primitive values
        Map<String, Object> defaultJsonData = new LinkedHashMap<>();
        defaultJsonData.put("default", "value");

        // Fetch a JSON object variable
        Value jsonObject = openFeatureClient.getObjectValue("test-json-variable", new Value(Structure.mapToStructure(defaultJsonData)), context);
        System.out.println(jsonObject.toString());

        // Retrieving a string variable along with the resolution details
        FlagEvaluationDetails<String> details = openFeatureClient.getStringDetails("doesnt-exist", "default", context);
        System.out.println("Value: " + details.getValue());
        System.out.println("Reason: " + details.getReason());

        MutableTrackingEventDetails eventDetails = new MutableTrackingEventDetails(610.1);
        eventDetails.add("test-string", "test-value");
        eventDetails.add("test-number", 123.456);
        eventDetails.add("test-boolean", true);
        eventDetails.add("test-json", new Value(Structure.mapToStructure(defaultJsonData)));

        openFeatureClient.track("test-of-event", context, eventDetails);

        Thread.sleep(20000);
    }
}
