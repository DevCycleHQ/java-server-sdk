package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;

public interface IDevCycleClient {
    public boolean isInitialized();

    public <T> T variableValue(DevCycleUser user, String key, T defaultValue);

    public <T> Variable<T> variable(DevCycleUser user, String key, T defaultValue);

    public void close();
}
