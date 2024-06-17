/*
 * DevCycle Bucketing API
 * Documents the DevCycle Bucketing API which provides and API interface to User Bucketing and for generated SDKs.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.devcycle.sdk.server.common.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DevCycleUserAndEvents {

    private List<DevCycleEvent> events;

    private DevCycleUser user;

    public DevCycleUserAndEvents(DevCycleUser user, List<DevCycleEvent> events) {
        this.user = user;
        this.events = events;
    }

    public static DevCycleUserAndEvents.Builder builder() {
        return new DevCycleUserAndEvents.Builder();
    }

    public DevCycleUserAndEvents addEventItem(DevCycleEvent eventItem) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        this.events.add(eventItem);
        return this;
    }

    public static class Builder {
        private DevCycleUser user;
        private List<DevCycleEvent> events;

        Builder() {
        }

        public DevCycleUserAndEvents.Builder user(DevCycleUser user) {
            this.user = user;
            return this;
        }

        public DevCycleUserAndEvents.Builder events(List<DevCycleEvent> events) {
            this.events = events;
            return this;
        }

        public DevCycleUserAndEvents build() {
            return new DevCycleUserAndEvents(user, events);
        }
    }
}
