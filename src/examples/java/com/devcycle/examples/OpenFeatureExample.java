package com.devcycle.examples;

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

        DevCycleLocalOptions options = DevCycleLocalOptions.builder().configPollingIntervalMS(60000)
                .disableAutomaticEventLogging(false).disableCustomEventLogging(false).build();

        // Initialize DevCycle Client
        DevCycleLocalClient devCycleClient = new DevCycleLocalClient(server_sdk_key, options);

        for (int i = 0; i < 10; i++) {
            if (devCycleClient.isInitialized()) {
                break;
            }
            Thread.sleep(500);
        }

        // Setup OpenFeature with the DevCycle Provider
        OpenFeatureAPI api = OpenFeatureAPI.getInstance();
        api.setProvider(devCycleClient.getOpenFeatureProvider());

        Client openFeatureClient = api.getClient();

        // Create the evaluation context to use for fetching variable values
        Map<String, Value> attributes = new LinkedHashMap<>();
        attributes.put("email", new Value("test-user@domain.com"));
        attributes.put("name", new Value("Test User"));
        attributes.put("language", new Value("en"));
        attributes.put("country", new Value("CA"));
        attributes.put("appVersion", new Value("1.0.0"));
        attributes.put("appBuild", new Value("1"));
        attributes.put("deviceModel", new Value("Macbook"));

        EvaluationContext context = new ImmutableContext("test-1234", attributes);

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
        Map<String,Object> defaultJsonData = new LinkedHashMap<>();
        defaultJsonData.put("default", "value");

        // Fetch a JSON object variable
        Value jsonObject = openFeatureClient.getObjectValue("test-json-variable", new Value(Structure.mapToStructure(defaultJsonData)), context );
        System.out.println(jsonObject.toString());


        // Retrieving a string variable along with the resolution details
        FlagEvaluationDetails<String> details = openFeatureClient.getStringDetails("doesnt-exist", "default", context);
        System.out.println("Value: " + details.getValue());
        System.out.println("Reason: " + details.getReason());

    }
}
