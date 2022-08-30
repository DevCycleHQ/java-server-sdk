package com.devcycle.sdk.server.local;

import java.io.IOException;

import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.api.DVCApi;
import com.devcycle.sdk.server.common.model.*;

import okhttp3.OkHttpClient;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public final class EnvironmentConfigManager {
  private static final int minimumPollingIntervalMs = 1000;
  // private final String environmentKey; 
  private final int pollingIntervalMS;
  // private final int requestTimeoutMs;
  private final OkHttpClient.Builder okBuilder;
  private final Retrofit.Builder restClient;
  public ProjectConfig Config;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public EnvironmentConfigManager(String environmentKey, DVCOptions dvcOptions) {
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    // this.environmentKey = environmentKey;

    int configPollingIntervalMs = dvcOptions.getConfigPollingIntervalMs();
    pollingIntervalMS = configPollingIntervalMs >= minimumPollingIntervalMs ? configPollingIntervalMs : minimumPollingIntervalMs;

    String url = "https://config-cdn.devcycle.com";
    okBuilder = new OkHttpClient.Builder();

    restClient = new Retrofit
    .Builder()
    .baseUrl(url)
    .addConverterFactory(JacksonConverterFactory.create());

    DVCApi api = restClient
    .client(okBuilder.build())
    .build()
    .create(DVCApi.class);

    Call<ProjectConfig> config = api.getConfig(environmentKey);
    
    try {
      Config = getResponse(config);
      System.out.printf(Config.getProject().toString());
      System.out.println();
      System.out.printf(Config.getEnvironment().toString());
      System.out.println();
    } catch (DVCException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
}