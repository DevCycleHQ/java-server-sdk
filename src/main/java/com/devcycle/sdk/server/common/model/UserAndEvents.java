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
public class UserAndEvents {

  private List<Event> events;

  private User user;

  public UserAndEvents(User user, List<Event> events) {
    this.user = user;
    this.events = events;
  }

  public UserAndEvents addEventItem(Event eventItem) {
    if (this.events == null) {
      this.events = new ArrayList<>();
    }
    this.events.add(eventItem);
    return this;
  }

  public static UserAndEvents.Builder builder() {
    return new UserAndEvents.Builder();
  }

  public static class Builder {
    private User user;
    private List<Event> events;

    Builder() {
    }

    public UserAndEvents.Builder user(User user) {
      this.user = user;
      return this;
    }

    public UserAndEvents.Builder events(List<Event> events) {
      this.events = events;
      return this;
    }

    public UserAndEvents build() {
      return new UserAndEvents(user, events);
    }
  }
}