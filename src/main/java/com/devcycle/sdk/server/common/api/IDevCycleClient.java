package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import dev.openfeature.sdk.FeatureProvider;

/**
 * Base interface for DevCycle clients that can be used to evaluate Features and retrieve variables values.
 */
public interface IDevCycleClient {
    /**
     * @return true if the client is initialized and ready to be used. Clients should
     * return a default value if they are not initialized.
     */
    boolean isInitialized();

    /**
     * @param user         (required) The user context for the evaluation.
     * @param key          (required) The key of the feature variable to evaluate.
     * @param defaultValue (required) The default value to return if the feature variable is not found or the user
     *                     does not segment into the feature
     * @return the value of the variable for the given user, or the default value if the variable is not found.
     */
    <T> T variableValue(DevCycleUser user, String key, T defaultValue);

    /**
     * @param user         (required) The user context for the evaluation.
     * @param key          (required) The key of the feature variable to evaluate.
     * @param defaultValue (required) The default value to return if the feature variable is not found or the user
     *                     does not segment into the feature
     * @return the variable for the given user, or the default variable if the variable is not found.
     */
    <T> Variable<T> variable(DevCycleUser user, String key, T defaultValue);

    void track(DevCycleUser user, DevCycleEvent event) throws DevCycleException;

    /**
     * Close the client and release any resources.
     */
    void close();

    /**
     * @return the OpenFeature provider for this client.
     */
    FeatureProvider getOpenFeatureProvider();

    String getSDKPlatform();
}
