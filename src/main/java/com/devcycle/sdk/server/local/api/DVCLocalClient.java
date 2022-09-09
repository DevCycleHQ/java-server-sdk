package com.devcycle.sdk.server.local.api;

import java.util.Collections;
import java.util.Map;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DVCLocalClient {

  private static LocalBucketing localBucketing = new LocalBucketing();

  private EnvironmentConfigManager configManager;

  private final String serverKey;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DVCLocalClient(String serverKey) {
    this(serverKey, DVCLocalOptions.builder().build());
  }

  public DVCLocalClient(String serverKey, DVCLocalOptions dvcOptions) {
    configManager = new EnvironmentConfigManager(serverKey, localBucketing, dvcOptions);
    this.serverKey = serverKey;
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  /**
   * Get all features for user data
   * 
   * @param user (required)
   */
  public Map<String, Feature> allFeatures(User user) throws JsonProcessingException {
    validateUser(user);
    localBucketing.setPlatformData(user.getPlatformData().toString());
    String userString = OBJECT_MAPPER.writeValueAsString(user);

    BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, userString);
    return bucketedUserConfig.features;
  }

  /**
   * Get variable by key for user data
   * 
   * @param user         (required)
   * @param key          Variable key (required)
   * @param defaultValue Default value to use if the variable could not be fetched
   *                     (required)
   * @return Variable
   */
  public <T> Variable<T> variable(User user, String key, T defaultValue) {
    validateUser(user);
    localBucketing.setPlatformData(user.getPlatformData().toString());

    if (key == null || key.equals("")) {
      throw new IllegalArgumentException("key cannot be null or empty");
    }

    if (defaultValue == null) {
      throw new IllegalArgumentException("defaultValue cannot be null");
    }

    if (!configManager.isConfigInitialized()) {
      System.out.println("Variable called before DVCClient has initialized, returning default value");
    }

    try {
      String userString = OBJECT_MAPPER.writeValueAsString(user);

      BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, userString);
      if (bucketedUserConfig.variables.containsKey(key)) {
        Variable<T> variable = bucketedUserConfig.variables.get(key);
        variable.setIsDefaulted(false);
        return variable;
      }
    } catch (JsonProcessingException e) {
      System.out.printf("Unable to parse JSON for Variable %s due to error: %s", key, e.toString());
    }

    Variable<T> variable;

    variable = (Variable<T>) Variable.builder()
        .key(key)
        .value(defaultValue)
        .isDefaulted(true)
        .reasonUsingDefaultValue("Variable not found")
        .build();

    // TODO queue events
    // eventQueue.queueAggregateEvent(
    // user,
    // new Event(type: EventTypes.variableDefaulted, target: key),
    // null
    // );

    return variable;
  }

  /**
   * Get all variables by key for user data
   * 
   * @param user (required)
   */
  public Map<String, Variable> allVariables(User user) throws JsonProcessingException {
    validateUser(user);
    localBucketing.setPlatformData(user.getPlatformData().toString());
    String userString = OBJECT_MAPPER.writeValueAsString(user);

    BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, userString);
    return bucketedUserConfig.variables;
  }

  /**
   * Post events to DevCycle for user
   * 
   * @param user  (required)
   * @param event (required)
   */
  // TODO: Original return type should match the line below, uncomment once
  // implemented and delete the void return
  // public DVCResponse track(User user, Event event) throws DVCException {
  public void track(User user, Event event) {
    validateUser(user);
    localBucketing.setPlatformData(user.getPlatformData().toString());

    UserAndEvents userAndEvents = UserAndEvents.builder()
        .user(user)
        .events(Collections.singletonList(event))
        .build();

    // Call track method to append custom event to queue
  }

  private void validateUser(User user) {
    if (user == null) {
      throw new IllegalArgumentException("User cannot be null");
    }
    if (user.getUserId().equals("")) {
      throw new IllegalArgumentException("userId cannot be empty");
    }
  }
}
