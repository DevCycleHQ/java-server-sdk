package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import dev.openfeature.sdk.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration tests for the OpenFeature provider that uses the Local SDK client to verify that
 * data conversions are working as expected
 */
public class DevCycleProviderLocalSDKTest {
    static final String apiKey = String.format("server-%s", UUID.randomUUID());
    private static DevCycleLocalClient client;
    private static LocalConfigServer localConfigServer;

    @BeforeClass
    public static void setup() throws Exception {
        // spin up a lightweight http server to serve the config and properly initialize the client
        localConfigServer = new LocalConfigServer(TestDataFixtures.SmallConfig(), 9000);
        localConfigServer.start();
        client = createClient(TestDataFixtures.SmallConfig());
    }

    private static DevCycleLocalClient createClient(String config) {
        localConfigServer.setConfigData(config);

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configCdnBaseUrl(localConfigServer.getHostRootURL())
                .configPollingIntervalMS(60000)
                .build();

        DevCycleLocalClient client = new DevCycleLocalClient(apiKey, options);
        try {
            int loops = 0;
            while (!client.isInitialized()) {
                // wait for the client to load the config and initialize
                Thread.sleep(100);
                loops++;
                // wait a max 10 seconds to initialize client before failing completely
                if (loops >= 100) {
                    throw new RuntimeException("Client failed to initialize in 10 seconds");
                }
            }
        } catch (InterruptedException e) {
            // no-op
        }
        return client;
    }

    @AfterClass
    public static void cleanup() throws Exception {
        client.close();
        localConfigServer.stop();
    }

    private EvaluationContext getContext() {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("email", new Value("giveMeVariationOn@email.com"));
        return new ImmutableContext("j_test", attributes);
    }

    @Test
    public void testGetObjectEvaluation() {
        EvaluationContext ctx = getContext();
        String key = "json-var";
        Map<String, Object> defaultJSON = new LinkedHashMap<>();
        defaultJSON.put("displayText", "This variation is off");
        defaultJSON.put("showDialog", false);
        defaultJSON.put("maxUsers", 0);

        Value defaultValue = new Value(Structure.mapToStructure(defaultJSON));

        ProviderEvaluation<Value> result = client.getOpenFeatureProvider().getObjectEvaluation(key, defaultValue, ctx);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertTrue(result.getValue().isStructure());
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());

        Map<String, Object> variableData = result.getValue().asStructure().asObjectMap();
        Assert.assertEquals("This variation is on", variableData.get("displayText"));
        Assert.assertEquals(true, variableData.get("showDialog"));
        Assert.assertEquals(100, variableData.get("maxUsers"));
    }

    @Test
    public void testGetBooleanEvaluation() {
        EvaluationContext ctx = getContext();
        String key = "a-cool-new-feature";
        Boolean defaultValue = false;
        Boolean expectedValue = true;
        ProviderEvaluation<Boolean> result = client.getOpenFeatureProvider().getBooleanEvaluation(key, defaultValue, ctx);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertEquals(expectedValue, result.getValue());
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
    }

    @Test
    public void testGetIntegerEvaluation() {
        EvaluationContext ctx = getContext();
        String key = "num-var";
        Integer defaultValue = 0;
        Integer expectedValue = 12345;
        ProviderEvaluation<Integer> result = client.getOpenFeatureProvider().getIntegerEvaluation(key, defaultValue, ctx);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertEquals(expectedValue, result.getValue());
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
    }

    @Test
    public void testGetDoubleEvaluation() {
        EvaluationContext ctx = getContext();
        String key = "num-var";
        Double defaultValue = 0.0;
        Double expectedValue = 12345.0;
        ProviderEvaluation<Double> result = client.getOpenFeatureProvider().getDoubleEvaluation(key, defaultValue, ctx);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertEquals(expectedValue, result.getValue());
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
    }

    @Test
    public void testGetStringEvaluation() {
        EvaluationContext ctx = getContext();
        String key = "string-var";
        String defaultValue = "default string";
        String expectedValue = "variationOn";
        ProviderEvaluation<String> result = client.getOpenFeatureProvider().getStringEvaluation(key, defaultValue, ctx);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertEquals(expectedValue, result.getValue());
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
    }

    @Test
    public void testTrackEvent() {
        EvaluationContext ctx = getContext();
        MutableTrackingEventDetails eventDetails = new MutableTrackingEventDetails(123.456);
        eventDetails.add("test-key", "test-value");
        client.getOpenFeatureProvider().track("test-event", ctx, eventDetails);

        Number value = eventDetails.getValue().orElse(null);
        Assert.assertEquals(123.456, value.doubleValue(), 0.0001);
        Assert.assertEquals("test-value", eventDetails.getValue("test-key").asString());
    }
}
