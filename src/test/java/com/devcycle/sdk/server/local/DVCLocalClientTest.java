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
        // spin up a lightweight http server to serve the config and properly initialize the client
        localConfigServer = new LocalConfigServer(TestDataFixtures.SmallConfig());
        localConfigServer.start();
        client = createClient(TestDataFixtures.SmallConfig());
    }

    private static DVCLocalClient createClient(String config){
        localConfigServer.setConfigData(config);
        DVCLocalOptions options = DVCLocalOptions.builder()
                .configCdnBaseUrl("http://localhost:8000/")
                .configPollingIntervalMS(60000)
                .build();
        DVCLocalClient client = new DVCLocalClient(apiKey, options);
        try {
            int loops = 0;
            while(!client.isInitialized())
            {
                // wait for the client to load the config and initialize
                Thread.sleep(100);
                loops++;
                // wait a max 10 seconds to initialize client before failing completely
                 if(loops >= 100){
                     throw new RuntimeException("Client failed to initialize in 10 seconds");
                 }
            }
        }catch (InterruptedException e) {
            // no-op
        }
        return client;
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
    public void variableBooleanValueTest() {
        User user = getUser();
        user.setEmail("giveMeVariationOn@email.com");
        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfig());
        Variable<Boolean> var = myClient.variable(user, "a-cool-new-feature", false);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());
    }

    @Test
    public void variableNumberValueTest() {
        User user = getUser();
        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfig());
        Variable<Double> var = myClient.variable(user, "num-var", 0.0);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals(12345.0, var.getValue().doubleValue(), 0.0);
    }

    @Test
    public void variableJsonValueTest() {
        User user = getUser();
        user.setEmail("giveMeVariationOn@email.com");
        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfig());

        Map<String,Object> defaultJSON = new HashMap();
        defaultJSON.put("displayText", "This variation is off");
        defaultJSON.put("showDialog", false);
        defaultJSON.put("maxUsers", 0);

        Variable<Object> var = myClient.variable(user, "json-var", defaultJSON);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Map<String,Object> variableData = (Map<String,Object>)var.getValue();
        Assert.assertEquals("This variation is on", variableData.get("displayText"));
        Assert.assertEquals(true, variableData.get("showDialog"));
        Assert.assertEquals(100, variableData.get("maxUsers"));
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
    public void variableTestWithCustomData(){
        User user = getUser();
        user.setEmail("giveMeVariationOn@email.com");

        Map<String,Object> customData = new HashMap();
        customData.put("boolProp", true);
        customData.put("intProp", 123);
        customData.put("stringProp", "abc");
        user.setCustomData(customData);

        Map<String,Object> privateCustomData = new HashMap();
        privateCustomData.put("boolProp", false);
        privateCustomData.put("intProp", 789);
        privateCustomData.put("stringProp", "xyz");
        user.setPrivateCustomData(privateCustomData);

        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("variationOn", var.getValue());
    }
    @Test
    public void variableTestBucketingWithCustomData(){
        // Make sure we are properly sending custom data to the WASM so the user is bucketed correctly

        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithCustomDataBucketing());
        User user = getUser();

        Map<String,Object> customData = new HashMap();
        customData.put("should-bucket", true);
        user.setCustomData(customData);

        Variable<String> var = myClient.variable(user, "unicode-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("↑↑↓↓←→←→BA 🤖", var.getValue());
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
    public void variableValueSpecialCharacters() {
        User user = getUser();
        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithSpecialCharacters());

        user.setEmail("giveMeVariationOn@email.com");
        Assert.assertEquals("öé \uD83D\uDC0D ¥ variationOn", myClient.variableValue(user, "string-var", "default string"));

        user.setEmail("giveMeVariationOff@email.com");
        Assert.assertEquals("öé \uD83D\uDC0D ¥ variationOff", myClient.variableValue(user, "string-var", "default string"));
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
        Assert.assertEquals(variables.get("num-var").getId(), "65272363125123fca69d3a7d");
        Assert.assertEquals(variables.get("json-var").getId(), "64372363125123fca69d3f7b");
        Assert.assertEquals(variables.size(), 4);
    }

    @Test
    public void setClientCustomDataWithBadMap(){
        // should be a no-op
        client.setClientCustomData(null);

        // should be a no-op
        Map<String, Object> testData = new HashMap();
        client.setClientCustomData(testData);
    }

    @Test
    public void setClientCustomDataWithBucketing() {
        DVCLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithCustomDataBucketing());

        // set the global custom data
        Map<String,Object> customData = new HashMap();
        customData.put("should-bucket", true);
        myClient.setClientCustomData(customData);

        // make sure the user get bucketed correctly based on the global custom data
        User user = getUser();
        Variable<String> var = myClient.variable(user, "unicode-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("↑↑↓↓←→←→BA 🤖", var.getValue());
    }

    private User getUser() {
        return User.builder()
                .userId("j_test")
                .build();
    }
}