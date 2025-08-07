package com.devcycle.sdk.server.common.model;

import java.util.Optional;

import com.devcycle.sdk.server.local.model.VariableMetadata;

public interface EvalHook<T> {

    default Optional<HookContext<T>> before(HookContext<T> ctx) {
        return Optional.empty();
    }
    default void after(HookContext<T> ctx, Variable<T> variable, VariableMetadata variableMetadata) {}
    default void error(HookContext<T> ctx, Throwable e) {}
    default void onFinally(HookContext<T> ctx, Optional<Variable<T>> variable, VariableMetadata variableMetadata) {}
}