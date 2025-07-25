package com.devcycle.sdk.server.local;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.common.api.IRestOptions;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.IDevCycleLogger;
import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.ConfigMetadata;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.local.model.Environment;
import com.devcycle.sdk.server.local.model.Project;

@RunWith(MockitoJUnitRunner.class)
public class DevCycleLocalClientTest {
    static final String apiKey = String.format("server-%s", UUID.randomUUID());
    static IDevCycleLogger testLoggingWrapper = new IDevCycleLogger() {
        @Override
        public void debug(String message) {
            System.out.println("DEBUG TEST: " + message);
        }

        @Override
        public void info(String message) {
            System.out.println("INFO TEST: " + message);
        }

        @Override
        public void warning(String message) {
            System.out.println("WARN TEST: " + message);
        }

        @Override
        public void error(String message) {
            System.out.println("ERROR TEST: " + message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            System.out.println("ERROR TEST: " + message);
        }
    };
    static IRestOptions restOptions = new IRestOptions() {

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Oauth-Token", "test-token");
            headers.put("Custom-Meta-Data", "some information the developer wants send");
            return headers;
        }

        @Override
        public SSLSocketFactory getSocketFactory() {
            return null;
        }

        @Override
        public X509TrustManager getTrustManager() {
            return null;
        }

        @Override
        public HostnameVerifier getHostnameVerifier() {
            return null;
        }
    };
    private static DevCycleLocalClient client;
    private static LocalConfigServer localConfigServer;

    @BeforeClass
    public static void setup() throws Exception {
        // spin up a lightweight http server to serve the config and properly initialize the client
        localConfigServer = new LocalConfigServer(TestDataFixtures.SmallConfig(), 9000);
        localConfigServer.start();
        client = createClient(TestDataFixtures.SmallConfig());
    }

    private static void waitForClient(DevCycleLocalClient client) {
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
    }

    private static DevCycleLocalClient createClient(String config) {
        localConfigServer.setConfigData(config);

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configCdnBaseUrl(localConfigServer.getHostRootURL())
                .configPollingIntervalMS(60000)
                .customLogger(testLoggingWrapper)
                .restOptions(restOptions)
                .build();

        DevCycleLocalClient client = new DevCycleLocalClient(apiKey, options);

        waitForClient(client);
        return client;
    }

    @AfterClass
    public static void cleanup() throws Exception {
        client.close();
        localConfigServer.stop();
    }

    @Test
    public void variableTest() {
        DevCycleUser user = getUser();
        user.setEmail("giveMeVariationOff@email.com");
        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertEquals("variationOff", var.getValue());

        user.setEmail("giveMeVariationOn@email.com");
        var = client.variable(user, "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertEquals("variationOn", var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("All Users", varEval.getDetails());
        Assert.assertEquals("63125321d31c601f992288bc", varEval.getTargetId());
    }

    @Test
    public void variableBooleanValueTest() {
        DevCycleUser user = getUser();
        user.setEmail("giveMeVariationOn@email.com");
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfig());
        Variable<Boolean> var = myClient.variable(user, "a-cool-new-feature", false);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("All Users", varEval.getDetails());
        Assert.assertEquals("63125321d31c601f992288bc", varEval.getTargetId());
    }

    @Test
    public void variableNumberValueTest() {
        DevCycleUser user = getUser();
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfig());
        Variable<Double> var = myClient.variable(user, "num-var", 0.0);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals(12345.0, var.getValue().doubleValue(), 0.0);

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("All Users", varEval.getDetails());
        Assert.assertEquals("63125321d31c601f992288bc", varEval.getTargetId());
    }

    @Test
    public void variableJsonValueTest() {
        DevCycleUser user = getUser();
        user.setEmail("giveMeVariationOn@email.com");
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfig());

        Map<String, Object> defaultJSON = new HashMap();
        defaultJSON.put("displayText", "This variation is off");
        defaultJSON.put("showDialog", false);
        defaultJSON.put("maxUsers", 0);

        Variable<Object> var = myClient.variable(user, "json-var", defaultJSON);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Map<String, Object> variableData = (Map<String, Object>) var.getValue();
        Assert.assertEquals("This variation is on", variableData.get("displayText"));
        Assert.assertEquals(true, variableData.get("showDialog"));
        Assert.assertEquals(100, variableData.get("maxUsers"));

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("All Users", varEval.getDetails());
        Assert.assertEquals("63125321d31c601f992288bc", varEval.getTargetId());
    }

    @Test
    public void variableTestNotInitialized() {
        // NOTE  - this test will generate some additional logging noise from the EventQueue
        // because it isn't initialized properly before the first call to variable()
        DevCycleLocalClient newClient = new DevCycleLocalClient(apiKey);
        Variable<String> var = newClient.variable(getUser(), "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals("default string", var.getValue());
        Assert.assertEquals(EvalReason.DefaultReasonDetailsEnum.MISSING_CONFIG.getValue(), var.getEval().getDetails());
    }

    @Test
    public void variableTestWithCustomData() {
        DevCycleUser user = getUser();
        user.setEmail("giveMeVariationOn@email.com");

        Map<String, Object> customData = new HashMap();
        customData.put("boolProp", true);
        customData.put("intProp", 123);
        customData.put("stringProp", "abc");
        user.setCustomData(customData);

        Map<String, Object> privateCustomData = new HashMap();
        privateCustomData.put("boolProp", false);
        privateCustomData.put("intProp", 789);
        privateCustomData.put("stringProp", "xyz");
        user.setPrivateCustomData(privateCustomData);

        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("variationOn", var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("All Users", varEval.getDetails());
        Assert.assertEquals("63125321d31c601f992288bc", varEval.getTargetId());
    }

    @Test
    public void variableTestBucketingWithCustomData() {
        // Make sure we are properly sending custom data to the WASM so the user is bucketed correctly

        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithCustomDataBucketing());
        DevCycleUser user = getUser();

        Map<String, Object> customData = new HashMap();
        customData.put("should-bucket", true);
        user.setCustomData(customData);

        Variable<String> var = myClient.variable(user, "unicode-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("↑↑↓↓←→←→BA 🤖", var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("Custom Data -> should-bucket", varEval.getDetails());
        Assert.assertEquals("638680d659f1b81cc9e6c5ab", varEval.getTargetId());
    }

    @Test
    public void variableTestUnknownVariableKey() {
        Variable<Boolean> var = client.variable(getUser(), "some-var-that-doesnt-exist", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("DEFAULT", varEval.getReason());
        Assert.assertEquals("User Not Targeted", varEval.getDetails());
        Assert.assertNull(varEval.getTargetId());
    }

    @Test
    public void variableTestTypeMismatch() {
        Variable<Boolean> var = client.variable(getUser(), "string-var", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("DEFAULT", varEval.getReason());
        Assert.assertEquals("User Not Targeted", varEval.getDetails());
        Assert.assertNull(varEval.getTargetId());
    }

    @Test
    public void variableTestNoDefault() {
        DevCycleUser user = getUser();
        try {
            Variable<String> var = client.variable(user, "string-var", null);
            Assert.fail("Expected IllegalArgumentException for null default value");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void variableTestNullUser() {
        try {
            client.variable(null, "string-var", "default string");
            Assert.fail("Expected IllegalArgumentException for null user");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void variableTestBadUserID() {
        DevCycleUser badUser = DevCycleUser.builder().userId("").build();
        try {
            client.variable(badUser, "string-var", "default string");
            Assert.fail("Expected IllegalArgumentException for empty userID");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void variableValueTest() {
        DevCycleUser user = getUser();
        user.setEmail("giveMeVariationOff@email.com");
        Assert.assertEquals("variationOff", client.variableValue(user, "string-var", "default string"));

        user.setEmail("giveMeVariationOn@email.com");
        Assert.assertEquals("variationOn", client.variableValue(user, "string-var", "default string"));
    }

    @Test
    public void variableValueSpecialCharacters() {
        DevCycleUser user = getUser();
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithSpecialCharacters());

        user.setEmail("giveMeVariationOn@email.com");
        Assert.assertEquals("öé \uD83D\uDC0D ¥ variationOn", myClient.variableValue(user, "string-var", "default string"));

        user.setEmail("giveMeVariationOff@email.com");
        Assert.assertEquals("öé \uD83D\uDC0D ¥ variationOff", myClient.variableValue(user, "string-var", "default string"));
    }

    @Test
    public void allFeaturesTest() {
        DevCycleUser user = getUser();
        Map<String, Feature> features = client.allFeatures(user);
        Assert.assertEquals(features.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(features.size(), 1);
    }

    @Test
    public void allVariablesTest() {
        DevCycleUser user = getUser();
        Map<String, BaseVariable> variables = client.allVariables(user);
        Assert.assertEquals(variables.get("string-var").getId(), "63125320a4719939fd57cb2b");
        Assert.assertEquals(variables.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e34");
        Assert.assertEquals(variables.get("num-var").getId(), "65272363125123fca69d3a7d");
        Assert.assertEquals(variables.get("json-var").getId(), "64372363125123fca69d3f7b");
        Assert.assertEquals(variables.size(), 4);

        Assert.assertEquals(variables.get("string-var").getFeatureId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(variables.get("a-cool-new-feature").getFeatureId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(variables.get("num-var").getFeatureId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(variables.get("json-var").getFeatureId(), "62fbf6566f1ba302829f9e32");
    }

    @Test
    public void trackTest() {
        DevCycleUser user = getUser();
        DevCycleEvent event = DevCycleEvent.builder()
                .type("test-event")
                .value(BigDecimal.valueOf(123.45))
                .metaData(Map.of("test-key", "test-value"))
                .build();

        client.track(user, event);
    }

    @Test
    public void setClientCustomDataWithBadMap() {
        // should be a no-op
        client.setClientCustomData(null);

        // should be a no-op
        Map<String, Object> testData = new HashMap();
        client.setClientCustomData(testData);
    }

    @Test
    public void SetClientCustomDataWithBucketingTest() {
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithCustomDataBucketing());

        // set the global custom data
        Map<String, Object> customData = new HashMap();
        customData.put("should-bucket", true);
        myClient.setClientCustomData(customData);

        // make sure the user get bucketed correctly based on the global custom data
        DevCycleUser user = getUser();
        Variable<String> var = myClient.variable(user, "unicode-var", "default string");
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals("↑↑↓↓←→←→BA 🤖", var.getValue());

        EvalReason varEval = var.getEval();
        Assert.assertEquals("TARGETING_MATCH", varEval.getReason());
        Assert.assertEquals("Custom Data -> should-bucket", varEval.getDetails());
        Assert.assertEquals("638680d659f1b81cc9e6c5ab", varEval.getTargetId());
    }

    @Test
    public void allFeaturesWithSpecialCharsTest() {
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfigWithSpecialCharacters());
        // make sure the user get bucketed correctly based on the global custom data
        DevCycleUser user = getUser();
        Map<String, Feature> features = myClient.allFeatures(user);
        Assert.assertNotNull(features);
        Assert.assertEquals(features.size(), 1);
    }

    @Test
    public void variable_withEvalHooks_callsHooksInOrder() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
                Assert.assertTrue(beforeCalled[0]);
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
                Assert.assertTrue(afterCalled[0]);
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertTrue(beforeCalled[0]);
        Assert.assertTrue(afterCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenBeforeHookThrows() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                throw new RuntimeException("Test before hook error");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertFalse(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenAfterHookThrows() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
                throw new RuntimeException("Test after hook error");
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertTrue(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenFinallyHookThrows() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
                throw new RuntimeException("Test finally hook error");
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertTrue(afterCalled[0]);
        Assert.assertFalse(errorCalled[0]); // No error hook should be called for finally errors
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenErrorHookThrows() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                throw new RuntimeException("Test before hook error");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
                throw new RuntimeException("Test error hook error");
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertFalse(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenMultipleHooksThrow() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .email("giveMeVariationOn@email.com")
                .build();

        final boolean[] hook1BeforeCalled = {false};
        final boolean[] hook1AfterCalled = {false};
        final boolean[] hook1ErrorCalled = {false};
        final boolean[] hook1FinallyCalled = {false};
        final boolean[] hook2BeforeCalled = {false};
        final boolean[] hook2AfterCalled = {false};
        final boolean[] hook2ErrorCalled = {false};
        final boolean[] hook2FinallyCalled = {false};

        // First hook throws in before
        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                hook1BeforeCalled[0] = true;
                throw new RuntimeException("Test hook1 before error");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                hook1AfterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                hook1ErrorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                hook1FinallyCalled[0] = true;
            }
        });

        // Second hook throws in after
        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                hook2BeforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                hook2AfterCalled[0] = true;
                throw new RuntimeException("Test hook2 after error");
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                hook2ErrorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                hook2FinallyCalled[0] = true;
            }
        });

        Variable<String> expected = Variable.<String>builder()
                .key("string-var")
                .value("variationOn")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .eval(new EvalReason("TARGETING_MATCH", "All Users", "63125321d31c601f992288bc"))
                .build();

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertEquals(expected, result);
        // First hook should be called and throw
        Assert.assertTrue(hook1BeforeCalled[0]);
        Assert.assertFalse(hook1AfterCalled[0]);
        Assert.assertTrue(hook1ErrorCalled[0]);
        Assert.assertTrue(hook1FinallyCalled[0]);
        // Second hook should not be called due to first hook error
        Assert.assertFalse(hook2BeforeCalled[0]);
        Assert.assertFalse(hook2AfterCalled[0]);
        Assert.assertTrue(hook2ErrorCalled[0]);
        Assert.assertTrue(hook2FinallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenHookThrowsCheckedException() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                throw new RuntimeException("Test checked exception in before hook");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
                Assert.assertTrue(error instanceof RuntimeException);
                Assert.assertEquals("Test checked exception in before hook", error.getMessage());
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenHookThrowsNullPointerException() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                throw new NullPointerException("Test NPE in before hook");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
                Assert.assertTrue(error instanceof NullPointerException);
                Assert.assertEquals("Test NPE in before hook", error.getMessage());
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenHookThrowsInFinallyAfterError() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                throw new RuntimeException("Test before hook error");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
                throw new RuntimeException("Test finally hook error after previous error");
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_returnsVariableWhenHookThrowsInErrorAfterFinally() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                throw new RuntimeException("Test before hook error");
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                errorCalled[0] = true;
                throw new RuntimeException("Test error hook error after before error");
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertNotNull(result);
        Assert.assertTrue(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void client_withHooksInOptions_addsHooksToEvalRunner() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false, false};
        final boolean[] afterCalled = {false, false};
        final boolean[] finallyCalled = {false, false};
        final boolean[] errorCalled = {false, false};

        List<EvalHook> hooks = new ArrayList<>();
        hooks.add(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable e) {
                errorCalled[0] = true;
            }
        });
        hooks.add(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                beforeCalled[1] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                afterCalled[1] = true;
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[1] = true;
            }

            @Override
            public void error(HookContext<String> ctx, Throwable e) {
                errorCalled[1] = true;
            }
        });

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configCdnBaseUrl(localConfigServer.getHostRootURL())
                .configPollingIntervalMS(60000)
                .customLogger(testLoggingWrapper)
                .restOptions(restOptions)
                .hooks(hooks)
                .build();
        localConfigServer.setConfigData(TestDataFixtures.SmallConfig());

        DevCycleLocalClient client = new DevCycleLocalClient(apiKey, options);
        waitForClient(client);

        Variable<String> expected = Variable.<String>builder()
                .key("string-var")
                .value("variationOn")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .eval(new EvalReason("TARGETING_MATCH", "All Users", "63125321d31c601f992288bc"))
                .build();

        Variable<String> result = client.variable(user, "string-var", "default string");

        Assert.assertEquals(expected, result);

        Assert.assertTrue(beforeCalled[0]);
        Assert.assertTrue(afterCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
        Assert.assertFalse(errorCalled[0]);

        Assert.assertTrue(beforeCalled[1]);
        Assert.assertTrue(afterCalled[1]);
        Assert.assertTrue(finallyCalled[1]);
        Assert.assertFalse(errorCalled[1]);
    }

    @After
    public void clearHooks() {
        client.clearHooks();
    }

    private DevCycleUser getUser() {
        return DevCycleUser.builder()
                .userId("j_test")
                .build();
    }

    @Test
    public void variable_withEvalHooks_metadataIsAccessibleInAfterHook() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] metadataChecked = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                // Verify metadata is accessible and properly populated
                Assert.assertNotNull("Metadata should not be null", ctx.getMetadata());
                
                // Check that config metadata has the expected structure
                ConfigMetadata metadata = ctx.getMetadata();
                Assert.assertNotNull("Config ETag should not be null", metadata.configETag);
                Assert.assertNotNull("Config last modified should not be null", metadata.configLastModified);
                Assert.assertNotNull("Project metadata should not be null", metadata.project);
                Assert.assertNotNull("Environment metadata should not be null", metadata.environment);
                
                // Verify basic metadata structure is present
                Assert.assertFalse("Config ETag should not be empty", metadata.configETag.isEmpty());
                Assert.assertFalse("Config last modified should not be empty", metadata.configLastModified.isEmpty());
                
                metadataChecked[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");
        
        Assert.assertTrue("Metadata check should have been executed", metadataChecked[0]);
        Assert.assertNotNull("Variable should not be null", result);
    }

    @Test
    public void variable_withEvalHooks_metadataConsistentAcrossHooks() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final ConfigMetadata[] capturedMetadata = {null, null, null}; // before, after, finally

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                capturedMetadata[0] = ctx.getMetadata();
                return Optional.empty();
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                capturedMetadata[1] = ctx.getMetadata();
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                capturedMetadata[2] = ctx.getMetadata();
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");
        
        // Verify all hook stages received metadata
        Assert.assertNotNull("Before hook should have metadata", capturedMetadata[0]);
        Assert.assertNotNull("After hook should have metadata", capturedMetadata[1]);
        Assert.assertNotNull("Finally hook should have metadata", capturedMetadata[2]);
        
        // Verify metadata is consistent across all hook stages
        Assert.assertEquals("Before and after metadata should be the same", 
                capturedMetadata[0].configETag, capturedMetadata[1].configETag);
        Assert.assertEquals("Before and finally metadata should be the same", 
                capturedMetadata[0].configETag, capturedMetadata[2].configETag);
        Assert.assertEquals("Metadata timestamps should be consistent", 
                capturedMetadata[0].configLastModified, capturedMetadata[1].configLastModified);
    }

    @Test
    public void variable_withEvalHooks_metadataAccessibleInErrorHook() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] metadataCheckedInError = {false};

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                throw new RuntimeException("Test error to trigger error hook");
            }

            @Override
            public void error(HookContext<String> ctx, Throwable error) {
                // Verify metadata is accessible even in error hook
                Assert.assertNotNull("Metadata should be accessible in error hook", ctx.getMetadata());
                ConfigMetadata metadata = ctx.getMetadata();
                Assert.assertNotNull("Config ETag should not be null in error hook", metadata.configETag);
                Assert.assertNotNull("Config last modified should not be null in error hook", metadata.configLastModified);
                Assert.assertNotNull("Project metadata should not be null in error hook", metadata.project);
                Assert.assertNotNull("Environment metadata should not be null in error hook", metadata.environment);
                metadataCheckedInError[0] = true;
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");
        
        Assert.assertTrue("Metadata should have been checked in error hook", metadataCheckedInError[0]);
        Assert.assertNotNull("Variable should not be null even after error", result);
    }

    @Test
    public void variable_withEvalHooks_metadataReflectsCurrentConfig() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final ConfigMetadata[] capturedMetadata = {null};

        client.addHook(new EvalHook<String>() {
            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                capturedMetadata[0] = ctx.getMetadata();
            }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");
        
        Assert.assertNotNull("Metadata should be captured", capturedMetadata[0]);
        
        // Verify metadata reflects current config state
        ConfigMetadata directMetadata = client.getMetadata();
        Assert.assertNotNull("Direct metadata should not be null", directMetadata);
        
        // The metadata in hooks should match the current client metadata
        Assert.assertEquals("Hook metadata ETag should match current metadata", 
                directMetadata.configETag, capturedMetadata[0].configETag);
        Assert.assertEquals("Hook metadata timestamp should match current metadata", 
                directMetadata.configLastModified, capturedMetadata[0].configLastModified);
        Assert.assertEquals("Hook metadata project should match current metadata", 
                directMetadata.project, capturedMetadata[0].project);
        Assert.assertEquals("Hook metadata environment should match current metadata", 
                directMetadata.environment, capturedMetadata[0].environment);
    }

    @Test
    public void variable_withMultipleHooks_allReceiveMetadata() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] metadataChecked = {false, false}; // Two hooks

        // First hook
        client.addHook(new EvalHook<String>() {
                         @Override
             public void after(HookContext<String> ctx, Variable<String> variable) {
                 Assert.assertNotNull("First hook should receive metadata", ctx.getMetadata());
                 Assert.assertNotNull("First hook metadata should have project", ctx.getMetadata().project);
                 Assert.assertNotNull("First hook metadata should have config ETag", ctx.getMetadata().configETag);
                 metadataChecked[0] = true;
             }
         });

         // Second hook
         client.addHook(new EvalHook<String>() {
             @Override
             public void after(HookContext<String> ctx, Variable<String> variable) {
                 Assert.assertNotNull("Second hook should receive metadata", ctx.getMetadata());
                 Assert.assertNotNull("Second hook metadata should have environment", ctx.getMetadata().environment);
                 Assert.assertNotNull("Second hook metadata should have last modified", ctx.getMetadata().configLastModified);
                 metadataChecked[1] = true;
             }
        });

        Variable<String> result = client.variable(user, "string-var", "default string");
        
        Assert.assertTrue("First hook should have checked metadata", metadataChecked[0]);
        Assert.assertTrue("Second hook should have checked metadata", metadataChecked[1]);
    }

    @Test
    public void configMetadata_canBeConstructedWithMockData() {
        // Create mock project and environment data for testing
        Project mockProject = new Project();
        mockProject._id = "mock-project-id";
        mockProject.key = "mock-project-key";

        Environment mockEnvironment = new Environment();
        mockEnvironment._id = "mock-env-id";
        mockEnvironment.key = "mock-env-key";

        // Test ConfigMetadata construction
        ConfigMetadata metadata = new ConfigMetadata(
                "test-etag-12345",
                "2023-10-01T12:00:00Z",
                mockProject,
                mockEnvironment
        );

        // Verify metadata is properly constructed
        Assert.assertNotNull("Metadata should not be null", metadata);
        Assert.assertEquals("Config ETag should match", "test-etag-12345", metadata.configETag);
        Assert.assertEquals("Config last modified should match", "2023-10-01T12:00:00Z", metadata.configLastModified);
        Assert.assertNotNull("Project metadata should not be null", metadata.project);
        Assert.assertNotNull("Environment metadata should not be null", metadata.environment);

        // Verify that metadata can be used in HookContext
        DevCycleUser testUser = DevCycleUser.builder().userId("test-user").build();
        HookContext<String> contextWithMetadata = new HookContext<>(testUser, "test-key", "default", metadata);
        
        Assert.assertNotNull("HookContext should not be null", contextWithMetadata);
        Assert.assertEquals("Metadata should be accessible from context", metadata, contextWithMetadata.getMetadata());
        Assert.assertEquals("Config ETag should be accessible", "test-etag-12345", contextWithMetadata.getMetadata().configETag);
    }
}