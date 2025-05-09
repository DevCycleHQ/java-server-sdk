package com.devcycle.sdk.server.helpers;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;

public class MockDevCycleClient implements IDevCycleClient {
    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public <T> T variableValue(DevCycleUser user, String key, T defaultValue) {
        return null;
    }

    @Override
    public <T> Variable<T> variable(DevCycleUser user, String key, T defaultValue) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void track(DevCycleUser user, DevCycleEvent event) throws DevCycleException { return; }

    @Override
    public DevCycleProvider getOpenFeatureProvider() {
        return null;
    }

    @Override
    public String getSDKPlatform() {
        return "Mock";
    }
}
