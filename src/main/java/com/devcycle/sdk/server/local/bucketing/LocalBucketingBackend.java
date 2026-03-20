package com.devcycle.sdk.server.local.bucketing;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * WASM local bucketing implementation (Wasmtime JNI or Chicory). Selected by {@link LocalBucketing}
 * from environment variable {@link LocalBucketing#ENV_USE_CHICORY}.
 */
interface LocalBucketingBackend {

    void storeConfig(String sdkKey, String config);

    void setPlatformData(String platformData);

    void setClientCustomData(String sdkKey, String customData);

    BucketedUserConfig generateBucketedConfig(String sdkKey, DevCycleUser user)
            throws JsonProcessingException;

    byte[] getVariableForUserProtobuf(byte[] serializedParams);

    void initEventQueue(String sdkKey, String clientUUID, String options);

    void queueEvent(String sdkKey, String user, String event);

    void queueAggregateEvent(String sdkKey, String event, String variableVariationMap);

    FlushPayload[] flushEventQueue(String sdkKey) throws JsonProcessingException;

    void onPayloadFailure(String sdkKey, String payloadId, boolean retryable);

    void onPayloadSuccess(String sdkKey, String payloadId);

    int getEventQueueSize(String sdkKey);

    String getConfigMetadata(String sdkKey);
}
