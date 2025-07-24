package com.devcycle.sdk.server.common.model;

import java.util.Map;

import com.devcycle.sdk.server.local.model.ConfigMetadata;

/**
 * Context object passed to hooks during variable evaluation.
 * Contains the user, variable key, default value, and additional context data.
 */
public class HookContext<T> {
    private DevCycleUser user;
    private final String key;
    private final T defaultValue;
    private final ConfigMetadata metadata;
    private Variable<T> variableDetails;

    public HookContext(DevCycleUser user, String key, T defaultValue, ConfigMetadata metadata) {
        this.user = user;
        this.key = key;
        this.defaultValue = defaultValue;
        this.metadata = metadata;
    }

    public HookContext(DevCycleUser user, String key, T defaultValue, Variable<T> variable, ConfigMetadata metadata) {
        this.user = user;
        this.key = key;
        this.defaultValue = defaultValue;
        this.variableDetails = variable;
        this.metadata = metadata;
    }

    public DevCycleUser getUser() {
        return user;
    }

    public String getKey() {
        return key;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Variable<T> getVariableDetails() { return variableDetails; }

    public ConfigMetadata getMetadata() {
        return metadata;
    }

    public HookContext<T> merge(HookContext<T> other) {
        if (other == null) {
            return this;
        }
        return new HookContext<>(other.getUser(), key, defaultValue, variableDetails, metadata);
    }
}