package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.api.DVCApi;
import com.devcycle.sdk.server.common.api.DVCApiClient;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;


public final class DVCLocalClient {

  private final DVCOptions dvcOptions;

  private static LocalBucketing localBucketing = new LocalBucketing();

  private final String serverKey;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


  public DVCLocalClient(String serverKey) {
    this(serverKey, DVCOptions.builder().build());
  }

  public DVCLocalClient(String serverKey, DVCOptions dvcOptions ) {
    new EnvironmentConfigManager(serverKey, dvcOptions);
    this.serverKey = serverKey;
    this.dvcOptions = dvcOptions;
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
    
    try {
      String userString = OBJECT_MAPPER.writeValueAsString(user);

      BucketedUserConfig bucketedUserConfig = localBucketing.generateBucketedConfig(serverKey, userString);
      if (bucketedUserConfig.variables.containsKey(key)) {
        Variable variable = bucketedUserConfig.variables.get(key);
        variable.setIsDefaulted(false);
        return variable;
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    Variable<T> variable;

    variable = (Variable<T>) Variable.builder()
        .key(key)
        .value(defaultValue)
        .isDefaulted(true)
        .reasonUsingDefaultValue("Variable not found")
        .build();

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
  // TODO: Original return type should match the line below, uncomment once implemented and delete the void return
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
