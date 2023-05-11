package com.devcycle.sdk.server.local.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DVCLocalClient {

  private static LocalBucketing localBucketing = new LocalBucketing();

  private EnvironmentConfigManager configManager;

  private final String sdkKey;

  private EventQueueManager eventQueueManager;

  private Boolean isInitialized = false;

  public DVCLocalClient(String sdkKey) {
    this(sdkKey, DVCLocalOptions.builder().build());
  }

  public DVCLocalClient(String sdkKey, DVCLocalOptions dvcOptions) {
    if(sdkKey == null || sdkKey.equals("")) {
      throw new IllegalArgumentException("Missing sdk key! Call initialize with a valid sdk key");
    }
    if(!isValidServerKey(sdkKey)) {
      throw new IllegalArgumentException("Invalid sdk key provided. Please call initialize with a valid server sdk key");
    }

    localBucketing.setPlatformData(PlatformData.builder().build().toString());

    configManager = new EnvironmentConfigManager(sdkKey, localBucketing, dvcOptions);
    this.sdkKey = sdkKey;
    try {
      eventQueueManager = new EventQueueManager(sdkKey, localBucketing, dvcOptions);
    } catch (Exception e) {
      System.out.printf("Error creating event queue due to error: %s%n", e.getMessage());
    }
    isInitialized = true;
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
      bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
    } catch (JsonProcessingException e) {
      System.out.printf("Unable to parse JSON for allFeatures due to error: %s%n", e.getMessage());
      return Collections.emptyMap();
    }
    return bucketedUserConfig.features;
  }

  /**
   * Get variable value by key for user data
   *
   * @param user (required)
   * @param key  Feature key (required)
   * @param defaultValue Default value to use if the variable could not be fetched
   *                     (required)
   * @return Variable value
   */
  public <T> T variableValue(User user, String key, T defaultValue) {
    return variable(user, key, defaultValue).getValue();
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
      throw new IllegalArgumentException("Missing parameter: key");
    }

    if (defaultValue == null) {
      throw new IllegalArgumentException("Missing parameter: defaultValue");
    }

    if (!configManager.isConfigInitialized()) {
      System.out.println("Variable called before DVCClient has initialized, returning default value");
    }

    TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
    Variable<T> defaultVariable = (Variable<T>) Variable.builder()
            .key(key)
            .type(variableType)
            .value(defaultValue)
            .defaultValue(defaultValue)
            .isDefaulted(true)
            .build();

    if (!isInitialized) {
      return defaultVariable;
    }

    try {
      BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
      if (bucketedUserConfig.variables.containsKey(key)) {
        BaseVariable baseVariable = bucketedUserConfig.variables.get(key);
        Variable<T> variable = (Variable<T>) Variable.builder()
          .key(key)
          .type(baseVariable.getType())
          .value(baseVariable.getValue())
          .defaultValue(defaultValue)
          .isDefaulted(false)
          .build();
        if (variable.getType() != variableType) {
          throw new IllegalArgumentException("Variable type mismatch, returning default value");
        }
        variable.setDefaultValue(defaultValue);
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
  public Map<String, BaseVariable> allVariables(User user) {
    validateUser(user);

    if (!isInitialized) {
      return Collections.emptyMap();
    }

    BucketedUserConfig bucketedUserConfig = null;
    try {
      bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
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

    if (event == null || event.getType().equals("")) {
      throw new IllegalArgumentException("Invalid Event");
    }

    try {
      eventQueueManager.queueEvent(user, event);
    } catch (Exception e) {
      System.out.printf("Failed to queue event due to error: %s%n", e.getMessage());
    }
  }

  public void setClientCustomData(Map<String,Object> customData) {
    if (!isInitialized || !configManager.isConfigInitialized())
    {
     System.out.println("SetClientCustomData called before DVCClient has initialized");
     return;
    }

    if (customData != null && !customData.isEmpty()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        String customDataJSON = mapper.writeValueAsString(customData);
        localBucketing.setClientCustomData(this.sdkKey, customDataJSON);
      } catch(Exception e) {
        System.out.printf("Failed to set custom data: %s%n", e.getMessage());
      }
    }
  }

  /**
   * Gracefully close the client
   * 
   */
  public void close() {
    if (!isInitialized) {
      return;
    }
    configManager.cleanup();
    eventQueueManager.cleanup();
  }

  private void validateUser(User user) {
    if (user == null) {
      throw new IllegalArgumentException("User cannot be null");
    }
    if (user.getUserId().equals("")) {
      throw new IllegalArgumentException("userId cannot be empty");
    }
  }

  private boolean isValidServerKey(String serverKey) {
    return serverKey.startsWith("server") || serverKey.startsWith("dvc_server");
  }
}
