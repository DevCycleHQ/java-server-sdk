package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.api.DVCApi;
import com.devcycle.sdk.server.common.api.DVCApiClient;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;


public final class DVCLocalClient {

  private final DVCApi api;
  private final DVCOptions dvcOptions;

  private static LocalBucketing localBucketing = new LocalBucketing();

  private final String serverKey;

  private static final String DEFAULT_PLATFORM = "Java";
  private static final String DEFAULT_PLATFORM_VERSION = System.getProperty("java.version");
  private static final User.SdkTypeEnum DEFAULT_SDK_TYPE = User.SdkTypeEnum.SERVER;
  private final String DEFAULT_SDK_VERSION = "1.1.0";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


  public DVCLocalClient(String serverKey) {
    this(serverKey, DVCOptions.builder().build());
  }

  public DVCLocalClient(String serverKey, DVCOptions dvcOptions) {
    new EnvironmentConfigManager(serverKey, dvcOptions);
    this.serverKey = serverKey;
    api = new DVCApiClient(serverKey, dvcOptions).initialize();
    this.dvcOptions = dvcOptions;
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // TODO: handle this in config manager
    String testConfigString = "{\"project\":{\"_id\":\"61f97628ff4afcb6d057dbf0\",\"key\":\"emma-project\",\"a0_organization\":\"org_tPyJN5dvNNirKar7\",\"settings\":{\"edgeDB\":{\"enabled\":false},\"optIn\":{\"enabled\":true,\"title\":\"EarlyAccess\",\"description\":\"Getearlyaccesstobetafeaturesbelow!\",\"imageURL\":\"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR68cgQT_BTgnhWTdfjUXSN8zM9Vpxgq82dhw&usqp=CAU\",\"colors\":{\"primary\":\"#531cd9\",\"secondary\":\"#16dec0\"}}}},\"environment\":{\"_id\":\"61f97628ff4afcb6d057dbf2\",\"key\":\"development\"},\"features\":[{\"_id\":\"62fbf6566f1ba302829f9e32\",\"key\":\"a-cool-new-feature\",\"type\":\"release\",\"variations\":[{\"key\":\"variation-on\",\"name\":\"VariationOn\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":true},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"iamlocal\"}],\"_id\":\"62fbf6566f1ba302829f9e38\"},{\"key\":\"variation-off\",\"name\":\"VariationOff\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":false},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"iamcloud\"}],\"_id\":\"62fbf6566f1ba302829f9e39\"}],\"configuration\":{\"_id\":\"62fbf6576f1ba302829f9e4d\",\"targets\":[{\"_audience\":{\"_id\":\"63125321d31c601f992288b6\",\"filters\":{\"filters\":[{\"type\":\"user\",\"subType\":\"platform\",\"comparator\":\"=\",\"values\":[\"java-local\"],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e38\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bb\"},{\"_audience\":{\"_id\":\"63125321d31c601f992288b7\",\"filters\":{\"filters\":[{\"type\":\"all\",\"values\":[],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e39\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bc\"}],\"forcedUsers\":{}}}],\"variables\":[{\"_id\":\"62fbf6566f1ba302829f9e34\",\"key\":\"a-cool-new-feature\",\"type\":\"Boolean\"},{\"_id\":\"63125320a4719939fd57cb2b\",\"key\":\"string-var\",\"type\":\"String\"}],\"variableHashes\":{\"a-cool-new-feature\":1868656757,\"string-var\":2413071944}}";
    localBucketing.storeConfig(serverKey, testConfigString);
  }

  /**
   * Get all features for user data
   * 
   * @param user (required)
   */
  // TODO: Original return type should match the line below, uncomment once implemented and delete the void return
  // public Map<String, Feature> allFeatures(User user) throws DVCException {
  public void allFeatures(User user) {
    validateUser(user);

    addDefaults(user);

    System.out.printf("called allFeatures with user: %s", user.toString());
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
  @SuppressWarnings("unchecked")
  // TODO: This method will always return the default value, update with local bucketing variable call
  public <T> Variable<T> variable(User user, String key, T defaultValue) {
    validateUser(user);

    if (key == null || key.equals("")) {
      throw new IllegalArgumentException("key cannot be null or empty");
    }

    if (defaultValue == null) {
      throw new IllegalArgumentException("defaultValue cannot be null");
    }

    addDefaults(user);

    try {
      localBucketing.setPlatformData(user.getPlatformDataString());
      ObjectMapper mapper = new ObjectMapper();
      String userString = mapper.writeValueAsString(user);

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
  // TODO: Original return type should match the line below, uncomment once implemented and delete the void return
  // public Map<String, Variable> allVariables(User user) throws DVCException {
  public void allVariables(User user) {
    validateUser(user);

    addDefaults(user);

    // Return allVariables after Local Bucketing call
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

    addDefaults(user);

    UserAndEvents userAndEvents = UserAndEvents.builder()
        .user(user)
        .events(Collections.singletonList(event))
        .build();

    // Call track method to append custom event to queue
  }

  private void addDefaults(User user) {
    if (Objects.isNull(user.getPlatform()) || Objects.equals(user.getPlatform(), "")) {
      user.setPlatform(DEFAULT_PLATFORM);
    }
    if (Objects.isNull(user.getPlatformVersion()) || Objects.equals(user.getPlatformVersion(), "")) {
      user.setPlatformVersion(DEFAULT_PLATFORM_VERSION);
    }
    if (Objects.isNull(user.getSdkType())) {
      user.setSdkType(DEFAULT_SDK_TYPE);
    }
    if (Objects.isNull(user.getSdkVersion()) || Objects.equals(user.getSdkVersion(), "")) {
      user.setSdkVersion(DEFAULT_SDK_VERSION);
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
