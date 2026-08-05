package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.exception.DevCycleException;

/**
 * Callback for the lifecycle of the locally cached project configuration.
 * <p>
 * Implementations are invoked on the config polling thread, or on the SSE message thread when a
 * realtime update triggers a refetch, and must not block.
 */
public interface ConfigUpdateListener {

    /**
     * A config fetch completed successfully.
     *
     * @param etag      ETag of the config now in use
     * @param firstLoad true if this is the first config that has been loaded
     * @param changed   true if the fetched config differs from the previously stored one
     */
    void onConfigLoaded(String etag, boolean firstLoad, boolean changed);

    /**
     * A config fetch failed. A previously fetched config, if any, remains in use.
     *
     * @param error the failure
     * @param fatal true if the failure is unrecoverable, ie. the SDK key is unauthorized
     */
    void onConfigError(DevCycleException error, boolean fatal);
}
