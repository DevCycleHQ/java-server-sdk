package com.devcycle.sdk.server.common.model;

import java.util.Optional;

public interface EvalHook<T> {

    default Optional<HookContext<T>> before(HookContext<T> ctx) {
        return Optional.empty();
    }
    default void after(HookContext<T> ctx, Variable<T> variable) {}
    default void error(HookContext<T> ctx, Throwable e) {}
    default void onFinally(HookContext<T> ctx, Optional<Variable<T>> variable) {}
}