package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.local.bucketing.LocalBucketing;

public class EventQueueManager {

    private final LocalBucketing localBucketing;

    public EventQueueManager(LocalBucketing localBucketing) {
        this.localBucketing = localBucketing;
    }

    // TODO copy https://github.com/DevCycleHQ/js-sdks/blob/629b1f0a0d2cbed36bfe364d69f1929f645046db/sdk/nodejs/src/eventQueueAS.ts#L53
    // to fill in these functions, then call from DVCLocalClient

    //flushEvents

    //queueEvent

    //queueAggregateEvent
}
