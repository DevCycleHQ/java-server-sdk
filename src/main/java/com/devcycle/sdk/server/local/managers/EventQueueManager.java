package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.api.DVCLocalEventsApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.*;
import com.devcycle.sdk.server.common.logging.DVCLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventQueueManager {

    private LocalBucketing localBucketing;
    private final String sdkKey;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private IDVCApi eventsApiClient;
    private int eventFlushIntervalMS;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isFlushingEvents = false;
    private int flushEventQueueSize;
    private int maxEventQueueSize;

    public EventQueueManager(String sdkKey, LocalBucketing localBucketing, DVCLocalOptions options) throws Exception {
        this.localBucketing = localBucketing;
        this.sdkKey = sdkKey;
        eventFlushIntervalMS = options.getEventFlushIntervalMS();
        flushEventQueueSize = options.getFlushEventQueueSize();
        maxEventQueueSize = options.getMaxEventQueueSize();

        eventsApiClient = new DVCLocalEventsApiClient(sdkKey, options).initialize();

        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.localBucketing.initEventQueue(sdkKey, OBJECT_MAPPER.writeValueAsString(options));

        setupScheduler();
    }

    private void setupScheduler() {
        Runnable flushEventsRunnable = () -> {
            try {
                flushEvents();
            } catch (Exception e) {
                DVCLogger.error( "DVC Error flushing events: " + e.getMessage(), e);
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
            DVCLogger.error( "DVC Error flushing event payloads: " + e.getMessage(), e);
        }

        if (flushPayloads.length == 0) return;

        DVCLogger.info("DVC Flush Payloads: " + Arrays.toString(flushPayloads));

        int eventCount = 0;
        isFlushingEvents = true;
        for (FlushPayload payload: flushPayloads) {
            eventCount += payload.eventCount;
            publishEvents(this.sdkKey, payload);
        }
        isFlushingEvents = false;
        DVCLogger.info(String.format("DVC Flush %d AS Events, for %d Users", eventCount, flushPayloads.length));
    }

    /**
     * Queue DVCAPIEvent for publishing to DevCycle Events API.
     */
    public void queueEvent(User user, Event event) throws Exception {
        if (checkEventQueueSize()) {
            DVCLogger.warning("Max event queue size reached, dropping event: " + event);
            return;
        }

        this.localBucketing.queueEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(user), OBJECT_MAPPER.writeValueAsString(event));
    }

    /**
     * Queue DVCEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    public void queueAggregateEvent(Event event, BucketedUserConfig bucketedConfig) throws Exception {
        if (checkEventQueueSize()) {
            DVCLogger.warning("Max event queue size reached, dropping aggregate event: " + event);
            return;
        }

        if (bucketedConfig != null) {
            this.localBucketing.queueAggregateEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(event), OBJECT_MAPPER.writeValueAsString(bucketedConfig.variableVariationMap));
        } else {
            this.localBucketing.queueAggregateEvent(this.sdkKey, OBJECT_MAPPER.writeValueAsString(event), "{}");
        }
    }

    private void publishEvents(String sdkKey, FlushPayload flushPayload) throws InterruptedException {
        Call<DVCResponse> response = eventsApiClient.publishEvents(EventsBatch.builder().batch(flushPayload.records).build());
        int responseCode = getResponse(response);

        if (responseCode == 201) {
            localBucketing.onPayloadSuccess(sdkKey, flushPayload.payloadId);
        } else {
            DVCLogger.warning("DVC Error Publishing Events: " + responseCode);
            localBucketing.onPayloadFailure(sdkKey, flushPayload.payloadId, responseCode >= 500);
        }
    }

    private int getResponse(Call call) {
        Response response = null;

        try {
            response = call.execute();
        } catch (IOException e) {
            DVCLogger.error( "DVC Events error: " + e.getMessage(), e);
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

            if (queueSize >= maxEventQueueSize) {
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        // Flush any remaining events
        try {
            flushEvents();
        } catch (Exception e) {
            DVCLogger.error("DVC Cleanup error: " + e.getMessage(), e);
        }
        scheduler.shutdown();
    }
}
