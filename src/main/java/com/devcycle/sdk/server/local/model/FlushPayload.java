package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.User;

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
        @Override
        public String toString() {
            return "Record{" +
                    "user=" + user +
                    ", events=" + Arrays.toString(events) +
                    '}';
        }

        public User user;
        public RequestEvent[] events;
    }
}

