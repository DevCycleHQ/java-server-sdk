package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.User;

public class EventPayload {
    public Record[] records;
    public String payloadId;
    public int eventCount;

    public static class Record {
        public User user;
        public RequestEvent[] events;
    }
}

