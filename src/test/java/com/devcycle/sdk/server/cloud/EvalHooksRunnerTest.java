package com.devcycle.sdk.server.cloud;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.devcycle.sdk.server.common.exception.AfterHookError;
import com.devcycle.sdk.server.common.exception.BeforeHookError;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.EvalHooksRunner;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.VariableMetadata;

public class EvalHooksRunnerTest {

    private EvalHooksRunner hookRunner;
    private DevCycleUser testUser;
    private Variable<Boolean> testVariable;
    private VariableMetadata testVariableMetadata;

    @Before
    public void setup() {
        hookRunner = new EvalHooksRunner();
        testUser = DevCycleUser.builder().userId("test-user").build();
        testVariable = Variable.<Boolean>builder()
                .key("test-var")
                .value(true)
                .type(Variable.TypeEnum.BOOLEAN)
                .build();
        testVariableMetadata = new VariableMetadata("test-feature-id");
    }

    @Test
    public void testBeforeHook() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);
        
        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                DevCycleUser modifiedUser = DevCycleUser.builder()
                    .userId("modified-user")
                    .build();
                return Optional.of(new HookContext<>(modifiedUser, ctx.getKey(), ctx.getDefaultValue(), null));
            }
        });

        HookContext<Boolean> result = hookRunner.executeBefore(hooks, context);
        Assert.assertEquals("modified-user", result.getUser().getUserId());
    }

    @Test(expected = BeforeHookError.class)
    public void testBeforeHookError() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);
        
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
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                hookCalled[0] = true;
                Assert.assertEquals("test-var", variable.getKey());
            }
        });

        hookRunner.executeAfter(hooks, context, testVariable, null);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test
    public void testAfterHookWithMetadata() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                hookCalled[0] = true;
                Assert.assertEquals("test-var", variable.getKey());
                Assert.assertEquals("test-feature-id", variableMetadata.featureId);
            }
        });

        hookRunner.executeAfter(hooks, context, testVariable, testVariableMetadata);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test(expected = AfterHookError.class)
    public void testAfterHookError() {
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);
        
        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                throw new RuntimeException("Test error");
            }
        });

        hookRunner.executeAfter(hooks, context, testVariable, null);
    }

    @Test
    public void testErrorHook() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);
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
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>>  variable, VariableMetadata variableMetadata) {
                hookCalled[0] = true;
            }
        });

        hookRunner.executeFinally(hooks, context, Optional.ofNullable(testVariable), null);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test
    public void testFinallyHookWithMetadata() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>>  variable, VariableMetadata variableMetadata) {
                hookCalled[0] = true;
            }
        });

        hookRunner.executeFinally(hooks, context, Optional.ofNullable(testVariable), testVariableMetadata);
        Assert.assertTrue(hookCalled[0]);
    }

    @Test
    public void testClearHooks() {
        final boolean[] hookCalled = {false};
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                hookCalled[0] = true;
            }
        });

        // Test that empty hooks array doesn't call any hooks
        ArrayList<EvalHook<Boolean>> emptyHooks = new ArrayList<>();
        hookRunner.executeAfter(emptyHooks, context, testVariable, null);
        Assert.assertFalse(hookCalled[0]);
    }

    @Test
    public void testMetadataPassedThroughHooks() {
        final Object[] capturedMetadata = {null, null, null}; // before, after, finally
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                capturedMetadata[0] = ctx.getMetadata();
                return Optional.empty();
            }

            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                capturedMetadata[1] = ctx.getMetadata();
            }

            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>> variable, VariableMetadata variableMetadata) {
                capturedMetadata[2] = ctx.getMetadata();
            }
        });

        // Execute hooks and verify metadata is consistently passed through
        HookContext<Boolean> beforeResult = hookRunner.executeBefore(hooks, context);
        hookRunner.executeAfter(hooks, beforeResult, testVariable, null);
        hookRunner.executeFinally(hooks, beforeResult, Optional.of(testVariable), null);

        // Verify metadata is consistently null (as passed in the context)
        Assert.assertNull("Before hook should receive null metadata", capturedMetadata[0]);
        Assert.assertNull("After hook should receive null metadata", capturedMetadata[1]);
        Assert.assertNull("Finally hook should receive null metadata", capturedMetadata[2]);
    }

    @Test
    public void testMetadataPassedThroughHooksWithVariableMetadata() {
        final Object[] capturedMetadata = {null, null, null, null, null}; // before, after, finally
        HookContext<Boolean> context = new HookContext<>(testUser, "test-key", false, null);

        ArrayList<EvalHook<Boolean>> hooks = new ArrayList<>();
        hooks.add(new EvalHook<Boolean>() {
            @Override
            public Optional<HookContext<Boolean>> before(HookContext<Boolean> ctx) {
                capturedMetadata[0] = ctx.getMetadata();
                return Optional.empty();
            }

            @Override
            public void after(HookContext<Boolean> ctx, Variable<Boolean> variable, VariableMetadata variableMetadata) {
                capturedMetadata[1] = ctx.getMetadata();
                capturedMetadata[2] = variableMetadata;
            }

            @Override
            public void onFinally(HookContext<Boolean> ctx, Optional<Variable<Boolean>> variable, VariableMetadata variableMetadata) {
                capturedMetadata[3] = ctx.getMetadata();
                capturedMetadata[4] = variableMetadata;
            }
        });

        // Execute hooks and verify metadata is consistently passed through
        HookContext<Boolean> beforeResult = hookRunner.executeBefore(hooks, context);
        hookRunner.executeAfter(hooks, beforeResult, testVariable, testVariableMetadata);
        hookRunner.executeFinally(hooks, beforeResult, Optional.of(testVariable), testVariableMetadata);

        // Verify metadata is consistently null (as passed in the context)
        Assert.assertNull("Before hook should receive null metadata", capturedMetadata[0]);
        Assert.assertNull("After hook should receive null metadata", capturedMetadata[1]);
        Assert.assertEquals("After hook should receive variable metadata", testVariableMetadata, capturedMetadata[2]);
        Assert.assertNull("Finally hook should receive null metadata", capturedMetadata[3]);
        Assert.assertEquals("Finally hook should receive variable metadata", testVariableMetadata, capturedMetadata[4]);
    }
}
