package com.devcycle.sdk.server.local.api;

import java.util.Collections;
import java.util.Map;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class DVCLocalClient {

  private static LocalBucketing localBucketing = new LocalBucketing();

  private EnvironmentConfigManager configManager;

  private final String serverKey;

  private EventQueueManager eventQueueManager;

  public DVCLocalClient(String serverKey) {
    this(serverKey, DVCLocalOptions.builder().build());
  }

  public DVCLocalClient(String serverKey, DVCLocalOptions dvcOptions) {
    localBucketing.setPlatformData(PlatformData.builder().build().toString());

    configManager = new EnvironmentConfigManager(serverKey, localBucketing, dvcOptions);
    this.serverKey = serverKey;
    try {
      eventQueueManager = new EventQueueManager(serverKey, localBucketing, dvcOptions);
    } catch (Exception e) {
      System.out.printf("Error creating event queue due to error: %s%n", e.getMessage());
    }
  }

  /**
   * Get all features for user data
   * 
   * @param user (required)
   */
  public Map<String, Feature> allFeatures(User user) {
    validateUser(user);
    BucketedUserConfig bucketedUserConfig = null;

    try {
      bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, user);
    } catch (JsonProcessingException e) {
      System.out.printf("Unable to parse JSON for allFeatures due to error: %s%n", e.getMessage());
      return Collections.emptyMap();
    }
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

    if (key == null || key.equals("")) {
      throw new IllegalArgumentException("key cannot be null or empty");
    }

    if (defaultValue == null) {
      throw new IllegalArgumentException("defaultValue cannot be null");
    }

    if (!configManager.isConfigInitialized()) {
      System.out.println("Variable called before DVCClient has initialized, returning default value");
    }

    Variable<T> defaultVariable = (Variable<T>) Variable.builder()
            .key(key)
            .value(defaultValue)
            .isDefaulted(true)
            .reasonUsingDefaultValue("Variable not found")
            .build();

    try {
      BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, user);
      if (bucketedUserConfig.variables.containsKey(key)) {
        Variable<T> variable = bucketedUserConfig.variables.get(key);
        variable.setIsDefaulted(false);
        eventQueueManager.queueAggregateEvent(Event.builder().type("aggVariableEvaluated").target(key).build(), bucketedUserConfig);
        return variable;
      } else {
        eventQueueManager.queueAggregateEvent(Event.builder().type("aggVariableDefaulted").target(key).build(), bucketedUserConfig);
        return defaultVariable;
      }
    } catch (Exception e) {
      System.out.printf("Unable to parse JSON for Variable %s due to error: %s", key, e.toString());
    }

    try {
      eventQueueManager.queueAggregateEvent(Event.builder().type("aggVariableDefaulted").target(key).build(), null);
    } catch (Exception e) {
      System.out.printf("Unable to parse aggVariableDefaulted event for Variable %s due to error: %s", key, e.toString());
    }
    return defaultVariable;
  }

  /**
   * Get all variables by key for user data
   * 
   * @param user (required)
   */
  public Map<String, Variable> allVariables(User user) {
    validateUser(user);

    BucketedUserConfig bucketedUserConfig = null;
    try {
      bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, user);
    } catch (JsonProcessingException e) {
      System.out.printf("Unable to parse JSON for allVariables due to error: %s%n", e.getMessage());
      return Collections.emptyMap();
    }
    return bucketedUserConfig.variables;
  }

  /**
   * Post events to DevCycle for user
   * 
   * @param user  (required)
   * @param event (required)
   */
  public void track(User user, Event event) {
    validateUser(user);

    try {
      eventQueueManager.queueEvent(user, event);
    } catch (Exception e) {
      System.out.printf("Failed to queue event due to error: %s%n", e.getMessage());
    }
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