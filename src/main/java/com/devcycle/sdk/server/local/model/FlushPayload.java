package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.DevCycleUser;

import java.util.Arrays;

public class FlushPayload {
    public Record[] records;
    public String payloadId;
    public int eventCount;

    @Override
    public String toString() {
        return "EventPayload{" +
                "records=" + Arrays.toString(records) +
                ", payloadId='" + payloadId + '\'' +
                ", eventCount=" + eventCount +
                '}';
    }

    public static class Record {
        public DevCycleUser user;
        public RequestEvent[] events;

        @Override
        public String toString() {
            return "Record{" +
                    "user=" + user +
                    ", events=" + Arrays.toString(events) +
                    '}';
        }
    }
}

