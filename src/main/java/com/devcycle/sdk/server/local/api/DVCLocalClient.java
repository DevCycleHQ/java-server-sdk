package com.devcycle.sdk.server.local.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.devcycle.sdk.server.local.protobuf.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DVCLocalClient {

  private LocalBucketing localBucketing = new LocalBucketing();

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

    TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
    Variable<T> defaultVariable = (Variable<T>) Variable.builder()
            .key(key)
            .type(variableType)
            .value(defaultValue)
            .defaultValue(defaultValue)
            .isDefaulted(true)
            .build();

    if (!configManager.isConfigInitialized() || !isInitialized) {
      System.out.println("Variable called before DVCClient has initialized, returning default value");
      try {
        eventQueueManager.queueAggregateEvent(Event.builder().type("aggVariableDefaulted").target(key).build(), null);
      } catch (Exception e) {
        System.out.printf("Unable to parse aggVariableDefaulted event for Variable %s due to error: %s", key, e.toString());
      }
      return defaultVariable;
    }

    double appBuild = Double.NaN;
    try{
      appBuild = Double.parseDouble(user.getAppBuild());
    }catch(Exception e){}

    DVCUser_PB user_pb = DVCUser_PB.newBuilder()
            .setUserId( user.getUserId())
            .setEmail(createNullableString(user.getEmail()))
            .setName(createNullableString(user.getName()))
            .setLanguage(createNullableString(user.getLanguage()))
            .setCountry(createNullableString(user.getCountry()))
            .setAppBuild(createNullableDouble(appBuild))
            .setAppVersion(createNullableString(user.getAppVersion()))
            .setCustomData(createNullableCustomData(user.getCustomData()))
            .setCustomData(createNullableCustomData(user.getPrivateCustomData()))
            .build();

    VariableType_PB pbVariableType = typeEnumToVariableTypeProtobuf(variableType);

    VariableForUserParams_PB params = VariableForUserParams_PB.newBuilder()
            .setSdkKey(sdkKey)
            .setUser(user_pb)
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

        if(sdkVariable.getType() != pbVariableType)
        {
          System.out.printf("Variable type mismatch, returning default value");
          return defaultVariable;
        }

        Variable<T> variable;
        switch(sdkVariable.getType()) {
          case Boolean:
            variable = (Variable<T>) Variable.builder()
                    .key(key)
                    .type(TypeEnum.BOOLEAN)
                    .value(sdkVariable.getBoolValue())
                    .defaultValue(defaultValue)
                    .isDefaulted(false)
                    .build();
            break;
          case String:
            variable = (Variable<T>) Variable.builder()
                    .key(key)
                    .type(TypeEnum.STRING)
                    .value(sdkVariable.getStringValue())
                    .defaultValue(defaultValue)
                    .isDefaulted(false)
                    .build();
            break;
          case Number:
            variable = (Variable<T>) Variable.builder()
                    .key(key)
                    .type(TypeEnum.NUMBER)
                    .value(sdkVariable.getDoubleValue())
                    .defaultValue(defaultValue)
                    .isDefaulted(false)
                    .build();
            break;
          case JSON:
            ObjectMapper mapper = new ObjectMapper();
            LinkedHashMap<String,Object> jsonMap = mapper.readValue(sdkVariable.getStringValue(), new TypeReference<LinkedHashMap<String,Object>>() {});
            variable = (Variable<T>) Variable.builder()
                    .key(key)
                    .type(TypeEnum.JSON)
                    .value(jsonMap)
                    .defaultValue(defaultValue)
                    .isDefaulted(false)
                    .build();
            break;
            default:
                throw new IllegalArgumentException("Unknown variable type: "+sdkVariable.getType());
        }
        return variable;
      }
    } catch (Exception e) {
      System.out.printf("Unable to evaluate Variable %s due to error: %s", key, e);
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

  private NullableString createNullableString(String value)
  {
    return value == null 
        ? NullableString.newBuilder().setIsNull(true).build() 
        : NullableString.newBuilder().setIsNull(false).setValue(value).build();
  }

  private NullableDouble createNullableDouble(double value)
  {
    return !Double.isNaN(value) ? NullableDouble.newBuilder().setIsNull(false).setValue(value).build() : NullableDouble.newBuilder().setIsNull(true).build();
  }

  private NullableCustomData createNullableCustomData(Object temp)
  {
    if (temp == null)
    {
      return NullableCustomData.newBuilder().setIsNull(true).build();
    }
    else
    {
      Map<String, Object> customData = (Map<String, Object>)temp;

      Map<String,CustomDataValue> values = new HashMap();

      for(Map.Entry<String,Object> entry :  customData.entrySet())
      {
        if(entry.getValue() == null)
        {
          values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Null).build());

        }
        else if (entry.getValue() instanceof String)
        {
          String strValue = (String)entry.getValue();
          values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Str).setStringValue(strValue).build());

        }
        else if (entry.getValue() instanceof Number)
        {
          double numValue = ((Number)entry.getValue()).doubleValue();
          values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Num).setDoubleValue(numValue).build());
        }
        else if (entry.getValue() instanceof Boolean)
        {
          boolean boolValue = ((Boolean)entry.getValue()).booleanValue();
          values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Bool).setBoolValue(boolValue).build());
        }
      }
      return NullableCustomData.newBuilder().putAllValue(values).setIsNull(false).build();
    }
  }

  private VariableType_PB typeEnumToVariableTypeProtobuf(TypeEnum type)
  {
    switch (type)
    {
      case BOOLEAN:
        return VariableType_PB.Boolean;
      case STRING:
        return VariableType_PB.String;
      case NUMBER:
        return VariableType_PB.Number;
      case JSON:
        return VariableType_PB.JSON;
      default:
        throw new IllegalArgumentException("Unknown variable type: "+type);
    }
  }
}
