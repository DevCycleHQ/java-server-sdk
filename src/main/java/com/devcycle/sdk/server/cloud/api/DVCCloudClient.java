package com.devcycle.sdk.server.cloud.api;

import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DVCCloudClient {

  private final IDVCApi api;
  private final DVCCloudOptions dvcOptions;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DVCCloudClient(String sdkKey) {
    this(sdkKey, DVCCloudOptions.builder().build());
  }

  public DVCCloudClient(String sdkKey, DVCCloudOptions options) {
    if(sdkKey == null || sdkKey.equals("")) {
      throw new IllegalArgumentException("Missing environment key! Call initialize with a valid environment key");
    }

    if(!isValidServerKey(sdkKey)) {
      throw new IllegalArgumentException("Invalid environment key provided. Please call initialize with a valid server environment key");
    }

    this.dvcOptions = options;
    api = new DVCCloudApiClient(sdkKey, options).initialize();
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  /**
   * Get all features for user data
   * 
   * @param user  (required)
   * @return Map&gt;String, Feature&lt;
   */
  public Map<String, Feature> allFeatures(User user) throws DVCException {
    validateUser(user);

    Call<Map<String, Feature>> response = api.getFeatures(user, dvcOptions.getEnableEdgeDB());
    return getResponse(response);
  }

  /**
   * Get variable value by key for user data
   *
   * @param user (required)
   * @param key  Feature key (required)
   * @param defaultValue Default value to use if the variable could not be fetched
   *                     (required)
   * @return Variable value
   * @throws DVCException If there are any uses with the data provided
   */
  public <T> T variableValue(User user, String key, T defaultValue)  throws DVCException {
    return variable(user, key, defaultValue).getValue();
  }

  /**
   * Get variable by key for user data
   * 
   * @param user  (required)
   * @param key Variable key (required)
   * @param defaultValue Default value to use if the variable could not be fetched (required)
   * @return Variable
   * @throws DVCException If there are any uses with the data provided
   */
  @SuppressWarnings("unchecked")
  public <T> Variable<T> variable(User user, String key, T defaultValue) throws DVCException {
    validateUser(user);

    if (key == null || key.equals("")) {
      ErrorResponse errorResponse = new ErrorResponse(500, "Missing parameter: key", null);
      throw new DVCException(HttpResponseCode.byCode(500), errorResponse);
    }

    if (defaultValue == null) {
      ErrorResponse errorResponse = new ErrorResponse(500, "Missing parameter: defaultValue", null);
      throw new DVCException(HttpResponseCode.byCode(500), errorResponse);
    }

    TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
    Variable<T> variable;

    try {
      Call<Variable> response = api.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB());
      variable = getResponseWithRetries(response, 5);
      if (variable.getType() != variableType) {
        throw new IllegalArgumentException("Variable type mismatch, returning default value");
      }
      variable.setIsDefaulted(false);
    } catch (Exception exception) {
      variable = (Variable<T>) Variable.builder()
          .key(key)
          .type(variableType)
          .value(defaultValue)
          .defaultValue(defaultValue)
          .isDefaulted(true)
          .build();
    }
    return variable;
  }

  /**
   * Get all variables by key for user data
   * 
   * @param user  (required)
   * @return Map&gt;String, BaseVariable&lt;
   */
  public Map<String, BaseVariable> allVariables(User user) throws DVCException {
    validateUser(user);

    Call<Map<String, BaseVariable>> response = api.getVariables(user, dvcOptions.getEnableEdgeDB());
    try {
      Map<String, BaseVariable> variablesResponse = getResponse(response);
      return variablesResponse;
    } catch (DVCException exception) {
      if (exception.getHttpResponseCode() == HttpResponseCode.SERVER_ERROR) {
        return new HashMap<>();
      }
      throw exception;
    }
  }

  /**
   * Post events to DevCycle for user
   * 
   * @param user  (required)
   * @param event  (required)
   */
  public void track(User user, Event event) throws DVCException {
    validateUser(user);

    if (event == null || event.getType() == null || event.getType().equals("")) {
      throw new IllegalArgumentException("Invalid Event");
    }

    UserAndEvents userAndEvents = UserAndEvents.builder()
            .user(user)
            .events(Collections.singletonList(event))
            .build();

    Call<DVCResponse> response = api.track(userAndEvents, dvcOptions.getEnableEdgeDB());
    getResponseWithRetries(response, 5);
  }


  private <T> T getResponseWithRetries(Call<T> call, int maxRetries) throws DVCException {
    // attempt 0 is the initial request, attempt > 0 are all retries
    int attempt = 0;
    do {
      try {
        return getResponse(call);
      } catch (DVCException e) {
        attempt++;

        // if out of retries or this is an unauthorized error, throw up exception
        if (!e.isRetryable() || attempt > maxRetries) {
          throw e;
        }

        try {
          // exponential backoff
          long waitIntervalMS = (long) (10 * Math.pow(2, attempt));
          Thread.sleep(waitIntervalMS);
        } catch (InterruptedException ex) {
          // no-op
        }

        // prep the call for a retry
        call = call.clone();
      }
    }while (attempt <= maxRetries);

    // getting here should not happen, but is technically possible
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    errorResponse.setMessage("Out of retry attempts");
    throw new DVCException(HttpResponseCode.SERVER_ERROR, errorResponse);
  }


  private <T> T getResponse(Call<T> call) throws DVCException {
    ErrorResponse errorResponse = ErrorResponse.builder().build();
    Response<T> response;

    try {
      response = call.execute();
    } catch(MismatchedInputException mie) {
      // got a badly formatted JSON response from the server
      errorResponse.setMessage(mie.getMessage());
      throw new DVCException(HttpResponseCode.NO_CONTENT, errorResponse);
    } catch (IOException e) {
      // issues reaching the server or reading the response
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
        errorResponse.setMessage("Invalid SDK Key");
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

  private boolean isValidServerKey(String serverKey) {
    return serverKey.startsWith("server") || serverKey.startsWith("dvc_server");
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
