package com.devcycle.sdk.server.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.devcycle.sdk.server.common.exception.AfterHookError;
import com.devcycle.sdk.server.common.exception.BeforeHookError;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;

/**
 * A class that manages evaluation hooks for the DevCycle SDK.
 * Provides functionality to add and clear hooks, storing them in an array.
 */
public class EvalHooksRunner<T> {
    private List<EvalHook<T>> hooks;

    /**
     * Default constructor initializes an empty list of hooks.
     */
    public EvalHooksRunner(List<EvalHook<T>> hooks) {
        if (hooks == null) {
            this.hooks = new ArrayList<>();
        } else {
            this.hooks = hooks;
        }
    }

    public EvalHooksRunner() {
        this.hooks = new ArrayList<>();
    }

    /**
     * Adds a single hook to the collection.
     * 
     * @param hook The hook to add
     */
    public void addHook(EvalHook<T> hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    /**
     * Clears all hooks from the collection.
     */
    public void clearHooks() {
        hooks.clear();
    }

    public List<EvalHook<T>> getHooks() {
        return this.hooks;
    }

    /**
     * Runs all before hooks in order.
     *
     * @param context The context to pass to the hooks
     * @param <T> The type of the variable value
     * @return The potentially modified context
     */
    public <T> HookContext<T> executeBefore(ArrayList<EvalHook<T>> hooks, HookContext<T> context) {
        HookContext<T> beforeContext = context;
        for (EvalHook<T> hook : hooks) {
            try {
                Optional<HookContext<T>> newContext = hook.before(beforeContext);
                if (newContext.isPresent()) {
                    beforeContext = beforeContext.merge(newContext.get());
                }
            } catch (Exception e) {
                throw new BeforeHookError("Before hook failed", e);
            }
        }
        return beforeContext;
    }

    /**
     * Runs all after hooks in reverse order.
     *
     * @param context The context to pass to the hooks
     * @param variable The variable result to pass to the hooks
     * @param <T> The type of the variable value
     */
    public void executeAfter(ArrayList<EvalHook<T>> hooks, HookContext<T> context, Variable<T> variable) {
        for (EvalHook<T> hook : hooks) {
            try {
                hook.after(context, variable);
            } catch (Exception e) {
                throw new AfterHookError("After hook failed", e);
            }
        }
    }

    /**
     * Runs all error hooks in reverse order.
     *
     * @param context The context to pass to the hooks
     * @param error The error that occurred
     * @param <T> The type of the variable value
     */
    public void executeError(ArrayList<EvalHook<T>> hooks, HookContext<T> context, Throwable error) {
        for (EvalHook<T> hook : hooks) {
            try {
                hook.error(context, error);
            } catch (Exception hookError) {
                // Log hook error but don't throw to avoid masking the original error
                DevCycleLogger.error("Error hook failed: " + hookError.getMessage(), hookError);
            }
        }
    }

    /**
     * Runs all finally hooks in reverse order.
     * 
     * @param context The context to pass to the hooks
     * @param variable The variable result to pass to the hooks (may be null)
     */
    public void executeFinally(ArrayList<EvalHook<T>> hooks, HookContext<T> context, Optional<Variable<T>> variable) {
        for (EvalHook<T> hook : hooks) {
            try {
                hook.onFinally(context, variable);
            } catch (Exception e) {
                // Log finally hook error but don't throw
                DevCycleLogger.error("Finally hook failed: " + e.getMessage(), e);
            }
        }
    }
}
