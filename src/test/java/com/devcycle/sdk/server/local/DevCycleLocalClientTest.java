package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.api.IRestOptions;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.IDevCycleLogger;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    private static DevCycleLocalClient createClient(String config) {
        localConfigServer.setConfigData(config);

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configCdnBaseUrl(localConfigServer.getHostRootURL())
                .configPollingIntervalMS(60000)
                .customLogger(testLoggingWrapper)
                .restOptions(restOptions)
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
    }

    @Test
    public void variableNumberValueTest() {
        DevCycleUser user = getUser();
        DevCycleLocalClient myClient = createClient(TestDataFixtures.SmallConfig());
        Variable<Double> var = myClient.variable(user, "num-var", 0.0);
        Assert.assertNotNull(var);
        Assert.assertFalse(var.getIsDefaulted());
        Assert.assertEquals(12345.0, var.getValue().doubleValue(), 0.0);
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
    }

    @Test
    public void variableTestUnknownVariableKey() {
        Variable<Boolean> var = client.variable(getUser(), "some-var-that-doesnt-exist", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());
    }

    @Test
    public void variableTestTypeMismatch() {
        Variable<Boolean> var = client.variable(getUser(), "string-var", true);
        Assert.assertNotNull(var);
        Assert.assertTrue(var.getIsDefaulted());
        Assert.assertEquals(true, var.getValue());
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

    @After
    public void clearHooks() {
        client.clearHooks();
    }

    private DevCycleUser getUser() {
        return DevCycleUser.builder()
                .userId("j_test")
                .build();
    }
}