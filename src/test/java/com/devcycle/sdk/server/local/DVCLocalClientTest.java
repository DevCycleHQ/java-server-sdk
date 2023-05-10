package com.devcycle.sdk.server.local;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;

@RunWith(MockitoJUnitRunner.class)
public class DVCLocalClientTest {
    private static DVCLocalClient client;
    private static LocalConfigServer localConfigServer;
    static final String apiKey = String.format("server-%s", UUID.randomUUID());

    @BeforeClass
    public static void setup() throws Exception {
        localConfigServer = new LocalConfigServer(TestDataFixtures.SmallConfig());
        localConfigServer.start();

        DVCLocalOptions options = DVCLocalOptions.builder().configCdnBaseUrl("http://localhost:8000/").configPollingIntervalMS(60000).build();
        client = new DVCLocalClient(apiKey, options);
        try {
            // wait one second for the config to be loaded by the client
            Thread.sleep(1000);
        }catch (Exception e) {
            System.out.println("Failed to sleep: " + e.getMessage());
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        client.close();
        localConfigServer.stop();
    }

    @Test
    public void variableTest() {
        User user = getUser();
        user.setEmail("giveMeVariationOff@email.com");
        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertEquals("variationOff", var.getValue());

        user.setEmail("giveMeVariationOn@email.com");
        var = client.variable(user, "string-var", "default string");
        Assert.assertEquals("variationOn", var.getValue());
    }

    public void variableValueTest() {
        User user = getUser();
        user.setEmail("giveMeVariationOff@email.com");
        Assert.assertEquals("variationOff", client.variableValue(user, "string-var", "default string"));

        user.setEmail("giveMeVariationOn@email.com");
        Assert.assertEquals("variationOn", client.variableValue(user, "string-var", "default string"));
    }

    @Test
    public void allFeaturesTest() {
        User user = getUser();
        Map<String, Feature> features = client.allFeatures(user);
        Assert.assertEquals(features.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(features.size(), 1);
    }

    @Test
    public void allVariablesTest() {
        User user = getUser();
        Map<String, BaseVariable> variables = client.allVariables(user);
        Assert.assertEquals(variables.get("string-var").getId(), "63125320a4719939fd57cb2b");
        Assert.assertEquals(variables.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e34");
        Assert.assertEquals(variables.size(), 2);
    }

    @Test
    public void setClientCustomDataWithBadMap(){
        // should be a no-op
        client.setClientCustomData(null);

        // should be a no-op
        Map<String, Object> testData = new HashMap();
        client.setClientCustomData(testData);
    }

    private User getUser() {
        return User.builder()
                .userId("j_test")
                .build();
    }
}