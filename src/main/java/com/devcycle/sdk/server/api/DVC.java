package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.exception.DVCException;
import com.devcycle.sdk.server.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DVC {

  private final DVCApi api;

  private static final String DEFAULT_PLATFORM = "Java";
  private static final String DEFAULT_PLATFORM_VERSION = Runtime.class.getPackage().getImplementationVersion();
  private static final User.SdkTypeEnum DEFAULT_SDK_TYPE = User.SdkTypeEnum.SERVER;
  private static final String DEFAULT_SDK_VERSION = "1.0.0";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DVC(String serverKey) {
    api = new DVCClient(serverKey).initialize();
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  /**
   * Get all features for user data
   * 
   * @param user  (required)
   * @return Map<String, Feature>
   */
  public Map<String, Feature> allFeatures(User user) throws DVCException, IOException {
    addDefaults(user);

    Call<Map<String, Feature>> response = api.getFeatures(user);
    return getResponse(response);
  }

  /**
   * Get variable by key for user data
   * 
   * @param user  (required)
   * @param key Variable key (required)
   * @return Variable
   */
  public <T> Variable variable(User user, String key, T defaultValue) {
    addDefaults(user);

    Variable variable;

    try {
      Call<Variable> response = api.getVariableByKey(user, key);
      variable = getResponse(response);
    } catch (Exception exception) {
      variable = Variable.builder()
              .key(key)
              .value(defaultValue)
              .build();
    }
    return variable;
  }

  /**
   * Get all variables by key for user data
   * 
   * @param user  (required)
   * @return Map<String, Variable>
   */
  public Map<String, Variable> allVariables(User user) throws DVCException, IOException {
    addDefaults(user);

    Call<Map<String, Variable>> response = api.getVariables(user);
    return getResponse(response);
  }

  /**
   * Post events to DevCycle for user
   * 
   * @param userAndEvents  (required)
   * @return DVCResponse
   */
  public DVCResponse track(User user, Event event) throws DVCException, IOException {
    addDefaults(user);

    UserAndEvents userAndEvents = UserAndEvents.builder()
            .user(user)
            .events(Collections.singletonList(event))
            .build();

    Call<DVCResponse> response = api.track(userAndEvents);
    return getResponse(response);
  }

  private <T> T getResponse(Call<T> call) throws DVCException, IOException {
    Response<T> response = call.execute();

    HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());

    ErrorResponse errorResponse = ErrorResponse.builder().build();
    errorResponse.setMessage("Unknown error");

    if (response.errorBody() != null) {
      errorResponse.setMessage(response.errorBody().string());
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
        errorResponse = OBJECT_MAPPER.readValue(response.message(), ErrorResponse.class);
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
}
