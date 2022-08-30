package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.api.DVCApi;
import com.devcycle.sdk.server.common.api.DVCApiClient;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
// import java.util.Map;
import java.util.Objects;

public final class DVCClient {

  private final DVCApi api;
  private final DVCOptions dvcOptions;

  private static final String DEFAULT_PLATFORM = "Java";
  private static final String DEFAULT_PLATFORM_VERSION = System.getProperty("java.version");
  private static final User.SdkTypeEnum DEFAULT_SDK_TYPE = User.SdkTypeEnum.SERVER;
  private final String DEFAULT_SDK_VERSION;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DVCClient(String serverKey) {
    this(serverKey, DVCOptions.builder().build());
  }

  public DVCClient(String serverKey, DVCOptions dvcOptions) {
    api = new DVCApiClient(serverKey).initialize();
    this.dvcOptions = dvcOptions;
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    DEFAULT_SDK_VERSION = "1.1.0";

    // TODO: Add Config Manager Initialization Code here
    // TODO: Add Local Bucketing Initialization Code here
  }

  /**
   * Get all features for user data
   * 
   * @param user (required)
   */
  // TODO: Original return type should match the line below, uncomment once implemented and delete the void return
  // public Map<String, Feature> allFeatures(User user) throws DVCException {
  public void allFeatures(User user) throws DVCException {
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
  // TODO: This method will always return the default value, update with local
  // bucketing variable call
  public <T> Variable<T> variable(User user, String key, T defaultValue) {
    validateUser(user);

    if (key == null || key.equals("")) {
      throw new IllegalArgumentException("key cannot be null or empty");
    }

    if (defaultValue == null) {
      throw new IllegalArgumentException("defaultValue cannot be null");
    }

    addDefaults(user);

    Variable<T> variable;

    variable = (Variable<T>) Variable.builder()
        .key(key)
        .value(defaultValue)
        .isDefaulted(true)
        .reasonUsingDefaultValue("Local Bucketing Not Implemented")
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
  public void allVariables(User user) throws DVCException {
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
  public void track(User user, Event event) throws DVCException {
    validateUser(user);

    addDefaults(user);

    UserAndEvents userAndEvents = UserAndEvents.builder()
        .user(user)
        .events(Collections.singletonList(event))
        .build();

    // Call track method to append custom event to queue
  }

  private <T> T getResponse(Call<T> call) throws DVCException {
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    Response<T> response;

    try {
      response = call.execute();
    } catch (IOException e) {
      errorResponse.setMessage(e.getMessage());
      throw new DVCException(HttpResponseCode.byCode(500), errorResponse);
    }

    HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());
    errorResponse.setMessage("Unknown error");

    if (response.errorBody() != null) {
      try {
        errorResponse = OBJECT_MAPPER.readValue(response.errorBody().string(), ErrorResponse.class);
      } catch (IOException e) {
        errorResponse.setMessage(e.getMessage());
        throw new DVCException(httpResponseCode, errorResponse);
      }
      throw new DVCException(httpResponseCode, errorResponse);
    }

    if (response.body() == null) {
      throw new DVCException(httpResponseCode, errorResponse);
    }

    if (response.isSuccessful()) {
      return response.body();
    } else {
      if (httpResponseCode == HttpResponseCode.UNAUTHORIZED) {
        errorResponse.setMessage("API Key is unauthorized");
      } else if (!response.message().equals("")) {
        try {
          errorResponse = OBJECT_MAPPER.readValue(response.message(), ErrorResponse.class);
        } catch (JsonProcessingException e) {
          errorResponse.setMessage(e.getMessage());
          throw new DVCException(httpResponseCode, errorResponse);
        }
      }

      throw new DVCException(httpResponseCode, errorResponse);
    }
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