package com.devcycle.sdk.server.local.bucketing;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Locale;

/**
 * Local bucketing via WebAssembly. The runtime is selected at JVM startup from the environment
 * variable {@value LocalBucketing#ENV_USE_CHICORY}:
 *
 * <ul>
 *   <li>If set to {@code 1}, {@code true}, or {@code yes} (case-insensitive), uses Chicory (pure
 *       Java WASM, suitable for Alpine/musl).
 *   <li>Otherwise uses wasmtime-java (default, JNI + glibc-linked natives on Linux).
 * </ul>
 */
public class LocalBucketing {

    /** When truthy, selects Chicory; otherwise Wasmtime JNI. Read once at construction. */
    public static final String ENV_USE_CHICORY = "DEVCYCLE_USE_CHICORY";

    private final LocalBucketingBackend backend;

    public LocalBucketing() {
        this.backend = useChicoryFromEnv() ? new ChicoryLocalBucketing() : new WasmtimeLocalBucketing();
    }

    /**
     * Local bucketing with wasmtime-java (JNI). For benchmarks and tests; production normally uses
     * {@link #LocalBucketing()} and {@link #ENV_USE_CHICORY}.
     */
    public static LocalBucketing forWasmtime() {
        return new LocalBucketing(new WasmtimeLocalBucketing());
    }

    /**
     * Local bucketing with Chicory (pure Java WASM). For benchmarks and tests; production normally
     * uses {@link #LocalBucketing()} and {@link #ENV_USE_CHICORY}.
     */
    public static LocalBucketing forChicory() {
        return new LocalBucketing(new ChicoryLocalBucketing());
    }

    private LocalBucketing(LocalBucketingBackend backend) {
        this.backend = backend;
    }

    static boolean useChicoryFromEnv() {
        String v = System.getenv(ENV_USE_CHICORY);
        if (v == null || v.isEmpty()) {
            return false;
        }
        v = v.trim().toLowerCase(Locale.ROOT);
        return v.equals("1") || v.equals("true") || v.equals("yes");
    }

    public synchronized void storeConfig(String sdkKey, String config) {
        backend.storeConfig(sdkKey, config);
    }

    public synchronized void setPlatformData(String platformData) {
        backend.setPlatformData(platformData);
    }

    public synchronized void setClientCustomData(String sdkKey, String customData) {
        backend.setClientCustomData(sdkKey, customData);
    }

    public synchronized BucketedUserConfig generateBucketedConfig(String sdkKey, DevCycleUser user)
            throws JsonProcessingException {
        return backend.generateBucketedConfig(sdkKey, user);
    }

    public synchronized byte[] getVariableForUserProtobuf(byte[] serializedParams) {
        return backend.getVariableForUserProtobuf(serializedParams);
    }

    public synchronized void initEventQueue(String sdkKey, String clientUUID, String options) {
        backend.initEventQueue(sdkKey, clientUUID, options);
    }

    public synchronized void queueEvent(String sdkKey, String user, String event) {
        backend.queueEvent(sdkKey, user, event);
    }

    public synchronized void queueAggregateEvent(String sdkKey, String event, String variableVariationMap) {
        backend.queueAggregateEvent(sdkKey, event, variableVariationMap);
    }

    public synchronized FlushPayload[] flushEventQueue(String sdkKey) throws JsonProcessingException {
        return backend.flushEventQueue(sdkKey);
    }

    public synchronized void onPayloadFailure(String sdkKey, String payloadId, boolean retryable) {
        backend.onPayloadFailure(sdkKey, payloadId, retryable);
    }

    public synchronized void onPayloadSuccess(String sdkKey, String payloadId) {
        backend.onPayloadSuccess(sdkKey, payloadId);
    }

    public synchronized int getEventQueueSize(String sdkKey) {
        return backend.getEventQueueSize(sdkKey);
    }

    public String getConfigMetadata(String sdkKey) {
        return backend.getConfigMetadata(sdkKey);
    }
}
