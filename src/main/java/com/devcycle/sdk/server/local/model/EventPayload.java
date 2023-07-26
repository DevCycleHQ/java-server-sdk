package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;

public class EventPayload {
    private DevCycleUser user;
    private DevCycleEvent[] events;
}
