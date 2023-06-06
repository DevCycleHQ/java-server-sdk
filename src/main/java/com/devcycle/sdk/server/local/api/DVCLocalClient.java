package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.devcycle.sdk.server.local.protobuf.SDKVariable_PB;
import com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB;
import com.devcycle.sdk.server.local.protobuf.VariableType_PB;
import com.devcycle.sdk.server.common.logging.DVCLogger;
import com.devcycle.sdk.server.local.utils.ProtobufUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public final class DVCLocalClient {

  private LocalBucketing localBucketing = new LocalBucketing();

  private EnvironmentConfigManager configManager;

  private final String sdkKey;

  private EventQueueManager eventQueueManager;

  public DVCLocalClient(String sdkKey) {
    this(sdkKey, DVCLocalOptions.builder().build());
  }

  public DVCLocalClient(String sdkKey, DVCLocalOptions dvcOptions) {
    if(sdkKey == null || sdkKey.equals("")) {
      throw new IllegalArgumentException("Missing SDK key! Call initialize with a valid SDK key");
    }
    if(!isValidServerKey(sdkKey)) {
      throw new IllegalArgumentException("Invalid SDK key provided. Please call initialize with a valid server SDK key");
    }

    if(dvcOptions.getCustomLogger() != null) {
      DVCLogger.setCustomLogger(dvcOptions.getCustomLogger());
    }

    if(!isValidRuntime()){
      DVCLogger.warning("Invalid architecture. The DVCLocalClient requires a 64-bit, x86 runtime environment.");
    }

    localBucketing.setPlatformData(PlatformData.builder().build().toString());

    configManager = new EnvironmentConfigManager(sdkKey, localBucketing, dvcOptions);
    this.sdkKey = sdkKey;
    try {
      eventQueueManager = new EventQueueManager(sdkKey, localBucketing, dvcOptions);
    } catch (Exception e) {
      DVCLogger.warning("Error creating event queue due to error: " + e.getMessage());
    }
  }

  /**
   * @return true if the DVCClient is fully initialized and has successfully loaded a configuration
   */
  public boolean isInitialized() {
    if(configManager != null) {
      return configManager.isConfigInitialized();
    }
    return false;
  }

  /**
   * Get all features for user data
   * 
   * @param user (required)
   */
  public Map<String, Feature> allFeatures(User user) {
    if(!isInitialized()) {
      return Collections.emptyMap();
    }

    validateUser(user);
    BucketedUserConfig bucketedUserConfig = null;

    try {
      bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
    } catch (JsonProcessingException e) {
      DVCLogger.info("Unable to parse JSON for allFeatures due to error: " + e.getMessage());
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

    TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
    Variable<T> defaultVariable = (Variable<T>) Variable.builder()
            .key(key)
            .type(variableType)
            .value(defaultValue)
            .defaultValue(defaultValue)
            .isDefaulted(true)
            .build();

    if (!isInitialized()) {
      DVCLogger.info("Variable called before DVCClient has initialized, returning default value");
      try {
        eventQueueManager.queueAggregateEvent(Event.builder().type("aggVariableDefaulted").target(key).build(), null);
      } catch (Exception e) {
        DVCLogger.error("Unable to parse aggVariableDefaulted event for Variable " + key + " due to error: " + e, e);
      }
      return defaultVariable;
    }

    VariableType_PB pbVariableType = ProtobufUtils.convertTypeEnumToVariableType(variableType);
    VariableForUserParams_PB params = VariableForUserParams_PB.newBuilder()
            .setSdkKey(sdkKey)
            .setUser(ProtobufUtils.createDVCUserPB(user))
            .setVariableKey(key)
            .setVariableType(pbVariableType)
            .setShouldTrackEvent(true)
            .build();

    try {
      byte[] paramsBuffer = params.toByteArray();
      byte[] variableData = localBucketing.getVariableForUserProtobuf(paramsBuffer);

      if (variableData == null || variableData.length == 0) {
        return defaultVariable;
      } else {
        SDKVariable_PB sdkVariable = SDKVariable_PB.parseFrom(variableData);
        if(sdkVariable.getType() != pbVariableType) {
          DVCLogger.info("Variable type mismatch, returning default value");
          return defaultVariable;
        }
        return ProtobufUtils.createVariable(sdkVariable, defaultValue);
      }
    } catch (Exception e) {
      DVCLogger.error("Unable to evaluate Variable " + key + " due to error: " + e, e);
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

    if (!isInitialized()) {
      return Collections.emptyMap();
    }

    BucketedUserConfig bucketedUserConfig = null;
    try {
      bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
    } catch (JsonProcessingException e) {
      DVCLogger.info("Unable to parse JSON for allVariables due to error: " + e.getMessage());
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
      DVCLogger.warning("Failed to queue event due to error: " + e.getMessage());
    }
  }

  public void setClientCustomData(Map<String,Object> customData) {
    if (!isInitialized())
    {
      DVCLogger.info("SetClientCustomData called before DVCClient has initialized");
     return;
    }

    if (customData != null && !customData.isEmpty()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        String customDataJSON = mapper.writeValueAsString(customData);
        localBucketing.setClientCustomData(this.sdkKey, customDataJSON);
      } catch(Exception e) {
        DVCLogger.error("Failed to set custom data due to error: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Gracefully close the client
   * 
   */
  public void close() {
    if (configManager != null) {
      configManager.cleanup();
    }
    if (eventQueueManager != null) {
      eventQueueManager.cleanup();
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

  private boolean isValidServerKey(String serverKey) {
    return serverKey.startsWith("server") || serverKey.startsWith("dvc_server");
  }

  private boolean isValidRuntime() {
    String arch = System.getProperty("os.arch");
    String model = System.getProperty("sun.arch.data.model");
    return arch.contains("x86") && model.contains("64");
  }
}
