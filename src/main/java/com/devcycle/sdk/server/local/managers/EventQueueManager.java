package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.api.DVCLocalEventsApiClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final String serverKey;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private IDVCApi eventsApiClient;
    private int eventFlushIntervalMS;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isFlushingEvents = false;

    public EventQueueManager(String serverKey, LocalBucketing localBucketing, DVCLocalOptions options) {
        this.localBucketing = localBucketing;
        this.serverKey = serverKey;
        eventFlushIntervalMS = options.getEventFlushIntervalMS();

        this.localBucketing.initEventQueue(serverKey, "{}");

        this.localBucketing.setPlatformData(User.builder().userId("java-server-sdk").build().getPlatformData().toString());
        eventsApiClient = new DVCLocalEventsApiClient(serverKey, options).initialize();

        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        setupScheduler();
    }

    private void setupScheduler() {
        Runnable getConfigRunnable = new Runnable() {
            public void run() {
                try {
                    flushEvents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        scheduler.scheduleAtFixedRate(getConfigRunnable, 0, this.eventFlushIntervalMS, TimeUnit.MILLISECONDS);
    }

    /**
     * Flush events in queue to DevCycle Events API. Requeue events if flush fails
     */
    //flushEvents
    public void flushEvents() throws Exception {
        if (isFlushingEvents) return;

        if (serverKey == null || serverKey.equals("")) {
            throw new Exception("DevCycle is not yet initialized to publish events.");
        }

        FlushPayload[] flushPayloads = new FlushPayload[0];
        try {
            flushPayloads = this.localBucketing.flushEventQueue(this.serverKey);
        } catch (Exception e) {
            System.out.printf("DVC Error Flushing Events: %s%n", e.getMessage());
        }

        if (flushPayloads.length == 0) return;
        System.out.printf("AS Flush Payloads: %s%n", Arrays.toString(flushPayloads));

        int eventCount = 0;
        isFlushingEvents = true;
        for (FlushPayload payload: flushPayloads) {
            eventCount += payload.eventCount;
            publishEvents(this.serverKey, payload);
        }
        isFlushingEvents = false;
        System.out.printf("DVC Flush %d AS Events, for %d Users%n", eventCount, flushPayloads.length);
    }

    /**
     * Queue DVCAPIEvent for publishing to DevCycle Events API.
     */
    public void queueEvent(User user, Event event) throws JsonProcessingException {
        this.localBucketing.queueEvent(this.serverKey, OBJECT_MAPPER.writeValueAsString(user), OBJECT_MAPPER.writeValueAsString(event));
    }

    /**
     * Queue DVCEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    public void queueAggregateEvent(Event event, BucketedUserConfig bucketedConfig) throws JsonProcessingException {
        if (bucketedConfig != null) {
            this.localBucketing.queueAggregateEvent(this.serverKey, OBJECT_MAPPER.writeValueAsString(event), OBJECT_MAPPER.writeValueAsString(bucketedConfig.variableVariationMap));
        } else {
            this.localBucketing.queueAggregateEvent(this.serverKey, OBJECT_MAPPER.writeValueAsString(event), "{}");
        }
    }

    private void publishEvents(String serverKey, FlushPayload flushPayload) throws InterruptedException {
        Thread publishEventsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Call<DVCResponse> response = eventsApiClient.publishEvents(EventsBatch.builder().batch(flushPayload.records).build());
                int responseCode = getResponse(response);

                if (responseCode == 201) {
                    localBucketing.onPayloadSuccess(serverKey, flushPayload.payloadId);
                } else {
                    System.out.printf("DVC Error Publishing Events: %d%n", responseCode);
                    localBucketing.onPayloadFailure(serverKey, flushPayload.payloadId, responseCode >= 500);
                }
            }
        });

        publishEventsThread.start();
        publishEventsThread.join();
    }

    private int getResponse(Call call) {
        Response response = null;

        try {
            response = call.execute();
        } catch (IOException e) {
            System.out.printf("DVC Events error: %s%n", e.getMessage());
        }

        if (response == null) {
            return 500;
        } else {
            return response.code();
        }
    }
}
