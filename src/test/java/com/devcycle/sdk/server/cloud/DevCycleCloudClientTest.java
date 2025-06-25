package com.devcycle.sdk.server.cloud;

import com.devcycle.sdk.server.cloud.api.DevCycleCloudClient;
import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.common.api.DVCApiMock;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.helpers.WhiteBox;
import dev.openfeature.sdk.Hook;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.mock.Calls;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.*;

import static org.mockito.Mockito.when;

/**
 * API tests for DevcycleApi
 */
@RunWith(MockitoJUnitRunner.class)
public class DevCycleCloudClientTest {

    @Mock
    private IDevCycleApi apiInterface;

    private DevCycleCloudClient api;

    private DVCApiMock dvcApiMock;

    private DevCycleCloudOptions dvcOptions;

    @Before
    public void setup() {
        final String apiKey = String.format("server-%s", UUID.randomUUID());

        api = new DevCycleCloudClient(apiKey);

        WhiteBox.setInternalState(api, "api", apiInterface);

        dvcApiMock = new DVCApiMock();

        dvcOptions = DevCycleCloudOptions.builder().build();
    }

    @Test
    public void getFeaturesTest() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .country("US")
                .build();

        when(apiInterface.getFeatures(user, false)).thenReturn(dvcApiMock.getFeatures(user, false));

        Map<String, Feature> features = api.allFeatures(user);

        assertUserDefaultsCorrect(user);

        Assert.assertNotNull(features);
        Assert.assertEquals(5, features.size());
        Assert.assertNotNull(features.get("show-feature-history"));
    }

    @Test
    public void getVariableByKeyTest() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        String key = "show-quickstart";

        when(apiInterface.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB()));

        Variable<Boolean> variable;
        try {
            variable = api.variable(user, key, true);

            assertUserDefaultsCorrect(user);

            Assert.assertFalse(variable.getValue());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVariableValueByKeyTest() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        String key = "show-quickstart";

        when(apiInterface.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB()));

        try {
            boolean value = api.variableValue(user, key, true);

            assertUserDefaultsCorrect(user);

            Assert.assertFalse(value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVariablesTest() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        when(apiInterface.getVariables(user, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariables(user, dvcOptions.getEnableEdgeDB()));

        Map<String, BaseVariable> variables = api.allVariables(user);

        assertUserDefaultsCorrect(user);

        Assert.assertNotNull(variables);
    }

    @Test
    public void variable_nullUser_throwsException() {
        Assert.assertThrows("DevCycleUser cannot be null",
                IllegalArgumentException.class,
                () -> api.variable(null, "wibble", true));
    }

    @Test
    public void variable_nullUserId_throwsException() {
        Assert.assertThrows("userId is marked non-null but is null",
                NullPointerException.class, () -> DevCycleUser.builder().build());
    }

    @Test
    public void variable_emptyUserId_throwsException() {
        DevCycleUser user = DevCycleUser.builder().userId("").build();

        Assert.assertThrows("userId cannot be empty",
                IllegalArgumentException.class, () -> api.variable(user, "wibble", true));
    }

    @Test
    public void variable_emptyKey_throwsException() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        Assert.assertThrows("Missing parameter: key",
                IllegalArgumentException.class, () -> api.variable(user, null, true));
    }

    @Test
    public void variable_emptyDefaultValue_throwsException() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        Assert.assertThrows("Missing parameter: defaultValue",
                IllegalArgumentException.class, () -> api.variable(user, "wibble", null));
    }

    @Test
    public void postEventsTest() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        DevCycleEvent event = DevCycleEvent.builder()
                .date(Instant.now().toEpochMilli())
                .target("test target")
                .type("test event")
                .value(new BigDecimal(600))
                .metaData(Meta.builder()
                        .meta("data")
                        .build())
                .build();

        DevCycleUserAndEvents userAndEvents = DevCycleUserAndEvents.builder()
                .user(user)
                .events(Collections.singletonList(event))
                .build();

        when(apiInterface.track(userAndEvents, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.track(userAndEvents, dvcOptions.getEnableEdgeDB()));

        api.track(user, event);

        assertUserDefaultsCorrect(user);
    }

    @Test
    public void variable_withEvalHooks_callsHooksInOrder() throws DevCycleException {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] finallyCalled = {false};

        api.addHook(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                beforeCalled[0] = true;
                return Optional.empty();
            }

            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                afterCalled[0] = true;
                Assert.assertTrue(beforeCalled[0]);
            }

            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>> variable) {
                finallyCalled[0] = true;
                Assert.assertTrue(afterCalled[0]);
            }
        });

        when(apiInterface.getVariableByKey(user, "test-key", dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariableByKey(user, "test-key", dvcOptions.getEnableEdgeDB()));

        Variable<Boolean> result = api.variable(user, "test-key", true);

        Assert.assertTrue(beforeCalled[0]);
        Assert.assertTrue(afterCalled[0]);
        Assert.assertTrue(finallyCalled[0]);
    }

    @Test
    public void variable_withEvalHooks_callsErrorHookOnException() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        final boolean[] beforeCalled = {false};
        final boolean[] afterCalled = {false};
        final boolean[] errorCalled = {false};
        final boolean[] finallyCalled = {false};

        api.addHook(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                beforeCalled[0] = true;
                return Optional.of(ctx);
            }

            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<Boolean> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>>  variable) {
                finallyCalled[0] = true;
            }
        });

        when(apiInterface.getVariableByKey(user, "test-key", dvcOptions.getEnableEdgeDB())).thenThrow(new RuntimeException("Test error"));

        Variable<Boolean> result = api.variable(user, "test-key", true);

        Assert.assertTrue(beforeCalled[0]);
        Assert.assertFalse(afterCalled[0]);
        Assert.assertTrue(errorCalled[0]);
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

        api.addHook(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                throw new RuntimeException("Test before hook error");
            }

            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                afterCalled[0] = true;
            }

            @Override
            public void error(HookContext<Boolean> ctx, Throwable error) {
                errorCalled[0] = true;
            }

            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>>  variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<Boolean> expected = Variable.<Boolean>builder()
                .key("test-true")
                .value(true)
                .type(Variable.TypeEnum.BOOLEAN)
                .isDefaulted(false)
                .defaultValue(false)
                .build();

        when(apiInterface.getVariableByKey(user, "test-true", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<Boolean> result = api.variable(user, "test-true", false);

        Assert.assertEquals(expected, result);
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

        api.addHook(new EvalHook<String>() {
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

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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

        api.addHook(new EvalHook<String>() {
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

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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

        api.addHook(new EvalHook<String>() {
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

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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
        api.addHook(new EvalHook<String>() {
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
        api.addHook(new EvalHook<String>() {
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
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

        Assert.assertNotNull(result);
        Assert.assertFalse(result.getIsDefaulted());
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

        api.addHook(new EvalHook<String>() {
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
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                finallyCalled[0] = true;
            }
        });

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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

        api.addHook(new EvalHook<String>() {
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

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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

        api.addHook(new EvalHook<String>() {
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

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", dvcOptions.getEnableEdgeDB())).thenReturn(Calls.response(expected));

        Variable<String> result = api.variable(user, "test-string", "default string");

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

        DevCycleCloudOptions options = DevCycleCloudOptions.builder()
                .hooks(hooks)
                .build();

        final String apiKey = String.format("server-%s", UUID.randomUUID());
        DevCycleCloudClient client = new DevCycleCloudClient(apiKey, options);
        WhiteBox.setInternalState(client, "api", apiInterface);

        Variable<String> expected = Variable.<String>builder()
                .key("test-string")
                .value("test value")
                .type(Variable.TypeEnum.STRING)
                .isDefaulted(false)
                .defaultValue("default string")
                .build();

        when(apiInterface.getVariableByKey(user, "test-string", false)).thenReturn(Calls.response(expected));

        Variable<String> result = client.variable(user, "test-string", "default string");

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

    private void assertUserDefaultsCorrect(DevCycleUser user) {
        Assert.assertEquals("Java", user.getPlatform());
        Assert.assertEquals(PlatformData.SdkTypeEnum.SERVER, user.getSdkType());
        Assert.assertNotNull(user.getPlatformVersion());
    }
}