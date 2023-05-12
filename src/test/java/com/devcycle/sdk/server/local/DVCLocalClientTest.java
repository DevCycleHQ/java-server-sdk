package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        Assert.assertNotNull(var);
        Assert.assertEquals("variationOff", var.getValue());

        user.setEmail("giveMeVariationOn@email.com");
        var = client.variable(user, "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertEquals("variationOn", var.getValue());
    }
    @Test
    public void variableTestNotInitialized(){
        // NOTE  - this test will generate some additional logging noise from the EventQueue
        // because it isn't initialized properly before the first call to variable()
        DVCLocalClient newClient = new DVCLocalClient(apiKey);
        Variable<String> var = newClient.variable(getUser(), "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals("default string", var.getValue());
    }

    @Test
    public void variableTestUnknownVariableKey(){
        Variable<Boolean> var = client.variable(getUser(), "some-var-that-doesnt-exist", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());
    }

    @Test
    public void variableTestTypeMismatch(){
        Variable<Boolean> var = client.variable(getUser(), "string-var", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());
    }

    @Test
    public void variableTestNoDefault() {
        User user = getUser();
        try {
            Variable<String> var = client.variable(user, "string-var", null);
            Assert.fail("Expected IllegalArgumentException for null default value");
        }catch(IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void variableTestNullUser() {
        try{
            client.variable(null, "string-var", "default string");
            Assert.fail("Expected IllegalArgumentException for null user");
        }catch(IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void variableTestBadUserID() {
        User badUser = User.builder().userId("").build();
        try {
            client.variable(badUser, "string-var", "default string");
            Assert.fail("Expected IllegalArgumentException for empty userID");
        }catch(IllegalArgumentException e) {
            // expected
        }
    }

    @Test
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