package com.devcycle.sdk.server.cloud;

import com.devcycle.sdk.server.common.exception.AfterHookError;
import com.devcycle.sdk.server.common.exception.BeforeHookError;
import com.devcycle.sdk.server.common.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

public class EvalHooksRunnerTest {

    private EvalHooksRunner hookRunner;
    private DevCycleUser testUser;
    private Variable<Boolean> testVariable;

    @Before
    public void setup() {
        hookRunner = new EvalHooksRunner();
        testUser = DevCycleUser.builder().userId("test-user").build();
        testVariable = Variable.<Boolean>builder()
                .key("test-var")
                .value(true)
                .type(Variable.TypeEnum.BOOLEAN)
                .build();
    }

    @Test
    public void testBeforeHook() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);
        
        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                DevCycleUser modifiedUser = DevCycleUser.builder()
                    .userId("modified-user")
                    .build();
                return Optional.of(new HookContext<>(modifiedUser, ctx.getKey(), ctx.getDefaultValue()));
            }
        });

        HookContext<Boolean> result = hookRunner.executeBefore(hooks, context);
        Assert.assertEquals("modified-user", result.getUser().getUserId());
    }

    @Test(expected = BeforeHookError.class)
    public void testBeforeHookError() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);
        
        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                throw new RuntimeException("Test error");
            }
        });

        hookRunner.executeBefore(hooks, context);
    }

    @Test
    public void testAfterHook() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                hookCalled[0] = true;
                Assert.assertEquals("test-var", variable.getKey());
            }
        });

        hookRunner.executeAfter(hooks, context, testVariable);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test(expected = AfterHookError.class)
    public void testAfterHookError() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);
        
        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                throw new RuntimeException("Test error");
            }
        });

        hookRunner.executeAfter(hooks, context, testVariable);
    }

    @Test
    public void testErrorHook() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);
        Exception testError = new Exception("Test error");

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void error(HookContext<Boolean> ctx, Throwable error) {
                hookCalled[0] = true;
                Assert.assertEquals("Test error", error.getMessage());
            }
        });

        hookRunner.executeError(hooks, context, testError);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test
    public void testFinallyHook() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>>  variable) {
                hookCalled[0] = true;
            }
        });

        hookRunner.executeFinally(hooks, context, Optional.ofNullable(testVariable));
        Assert.assertTrue(hookCalled[0]);
    }

    @Test
    public void testClearHooks() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable) {
                hookCalled[0] = true;
            }
        });

        // Test that empty hooks array doesn't call any hooks
        ArrayList<EvalHook<Boolean>> emptyHooks = new ArrayList<>();
        hookRunner.executeAfter(emptyHooks, context, testVariable);
        Assert.assertFalse(hookCalled[0]);
    }
}
