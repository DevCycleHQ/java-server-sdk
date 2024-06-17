package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleResponse;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.ProjectConfig;
import com.devcycle.sdk.server.local.api.DevCycleLocalEventsApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.local.model.EventsBatch;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventQueueManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String sdkKey;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());
    private final LocalBucketing localBucketing;
    private final IDevCycleApi eventsApiClient;
    private final int eventFlushIntervalMS;
    private boolean isFlushingEvents = false;
    private final int flushEventQueueSize;
    private final int maxEventQueueSize;

    public EventQueueManager(String sdkKey, LocalBucketing localBucketing, DevCycleLocalOptions options) throws Exception {
        this.localBucketing = localBucketing;
        this.sdkKey = sdkKey;
        eventFlushIntervalMS = options.getEventFlushIntervalMS();
        flushEventQueueSize = options.getFlushEventQueueSize();
        maxEventQueueSize = options.getMaxEventQueueSize();

        eventsApiClient = new DevCycleLocalEventsApiClient(sdkKey, options).initialize();

        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.localBucketing.initEventQueue(sdkKey, OBJECT_MAPPER.writeValueAsString(options));

        setupScheduler();
    }

    private void setupScheduler() {
        Runnable flushEventsRunnable = () -> {
            try {
                flushEvents();
            } catch (Exception e) {
                DevCycleLogger.error("DevCycle Error flushing events: " + e.getMessage(), e);
            }
        };

        scheduler.scheduleAtFixedRate(flushEventsRunnable, 0, this.eventFlushIntervalMS, TimeUnit.MILLISECONDS);
    }

    /**
     * Flush events in queue to DevCycle Events API. Requeue events if flush fails
     */
    public synchronized void flushEvents() throws Exception {
        if (isFlushingEvents) return;

        if (sdkKey == null || sdkKey.equals("")) {
            throw new Exception("DevCycle is not yet initialized to publish events.");
        }

        FlushPayload[] flushPayloads = new FlushPayload[0];
        try {
            flushPayloads = this.localBucketing.flushEventQueue(this.sdkKey);
        } catch (Exception e) {
            DevCycleLogger.error("DevCycle Error flushing event payloads: " + e.getMessage(), e);
        }

        if (flushPayloads.length == 0) return;

        DevCycleLogger.debug("DevCycle Flush Payloads: " + Arrays.toString(flushPayloads));

        int eventCount = 0;
        isFlushingEvents = true;
        for (FlushPayload payload : flushPayloads) {
            eventCount += payload.eventCount;
            publishEvents(this.sdkKey, payload);
        }
        isFlushingEvents = false;
        DevCycleLogger.debug(String.format("DevCycle Flush %d AS Events, for %d Users", eventCount, flushPayloads.length));
    }

    /**
     * Queue DevCycleAPIEvent for publishing to DevCycle Events API.
     */
    public void queueEvent(DevCycleUser user, DevCycleEvent event) throws Exception {
        if (checkEventQueueSize()) {
            DevCycleLogger.warning("Max event queue size reached, dropping event: " + event);
            return;
        }

        this.localBucketing.queueEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(user), OBJECT_MAPPER.writeValueAsString(event));
    }

    public void queueSDKConfigEvent(Request req, Response<ProjectConfig> response) throws Exception {
        DevCycleUser user = new DevCycleUser();
        user.setUserId(localBucketing.getClientUUID() + "@" + InetAddress.getLocalHost().getHostName());
        DevCycleEvent event = new DevCycleEvent();
        event.setType("sdkConfig");
        event.setTarget(req.url().toString());

        try (okhttp3.Response res = response.raw()) {

            if (res.networkResponse() != null
                    && res.networkResponse().sentRequestAtMillis() != 0
                    && res.networkResponse().receivedResponseAtMillis() != 0) {
                event.setValue(BigDecimal.valueOf(res.networkResponse().receivedResponseAtMillis() - res.networkResponse().sentRequestAtMillis()));
            }
            HashMap<String, Object> metaData = new HashMap<>();
            metaData.put("clientUUID", localBucketing.getClientUUID());
            metaData.put("reqEtag", req.header("If-None-Match"));
            metaData.put("reqLastModified", req.header("If-Modified-Since"));
            metaData.put("resEtag", res.header("etag"));
            metaData.put("resLastModified", res.header("Last-Modified"));
            metaData.put("resRayId", res.header("cf-ray"));
            metaData.put("resStatus", response.code());
            metaData.put("errMsg", response.code() != 200 && response.code() != 304 ? response.message() : null);
            metaData.put("sseConnected", null);
            event.setMetaData(metaData);


        } catch (Exception e) {
            event.setValue(BigDecimal.valueOf(-1));
        }
        queueEvent(user, event);
    }

    /**
     * Queue DevCycleEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    public void queueAggregateEvent(DevCycleEvent event, BucketedUserConfig bucketedConfig) throws Exception {
        if (checkEventQueueSize()) {
            DevCycleLogger.warning("Max event queue size reached, dropping aggregate event: " + event);
            return;
        }

        if (bucketedConfig != null) {
            this.localBucketing.queueAggregateEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(event), OBJECT_MAPPER.writeValueAsString(bucketedConfig.variableVariationMap));
        } else {
            this.localBucketing.queueAggregateEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(event), "{}");
        }
    }

    private void publishEvents(String sdkKey, FlushPayload flushPayload) throws InterruptedException {
        Call<DevCycleResponse> response = eventsApiClient.publishEvents(EventsBatch.builder().batch(flushPayload.records).build());
        int responseCode = getResponse(response);

        if (responseCode == 201) {
            localBucketing.onPayloadSuccess(sdkKey, flushPayload.payloadId);
        } else {
            DevCycleLogger.warning("DevCycle Error Publishing Events: " + responseCode);
            localBucketing.onPayloadFailure(sdkKey, flushPayload.payloadId, responseCode >= 500);
        }
    }

    private int getResponse(Call call) {
        Response response = null;

        try {
            response = call.execute();
        } catch (IOException e) {
            DevCycleLogger.error("DevCycle Events error: " + e.getMessage(), e);
        }

        if (response == null) {
            return 500;
        } else {
            return response.code();
        }
    }

    /**
     * Returns true if event queue size is greater or equal to flushEventQueueSize or maxEventQueueSize
     * Flushes events if event queue size is equal or greater to flushEventQueueSize
     */
    private boolean checkEventQueueSize() throws Exception {
        int queueSize = localBucketing.getEventQueueSize(sdkKey);

        if (queueSize >= flushEventQueueSize) {
            if (!isFlushingEvents) {
                flushEvents();
            }

            return queueSize >= maxEventQueueSize;
        }
        return false;
    }

    public void cleanup() {
        // Flush any remaining events
        try {
            flushEvents();
        } catch (Exception e) {
            DevCycleLogger.error("DevCycle Cleanup error: " + e.getMessage(), e);
        }
        scheduler.shutdown();
    }
}
