package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.model.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

interface DVCApi {
  /**
   * Get all features by key for user data
   *
   * @param user  (required)
   * @return Call&lt;Map&lt;String, Feature&gt;&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/features")
  Call<Map<String, Feature>> getFeatures(@Body User user);

  /**
   * Get variable by key for user data
   *
   * @param user  (required)
   * @param key Variable key (required)
   * @return Call&lt;Variable&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/variables/{key}")
  Call<Variable> getVariableByKey(@Body User user, @Path("key") String key);

  /**
   * Get all variables by key for user data
   *
   * @param user  (required)
   * @return Call&lt;Map&lt;String, Variable&gt;&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/variables")
  Call<Map<String, Variable>> getVariables(@Body User user);

  /**
   * Post events to DevCycle for user
   *
   * @param userAndEvents  (required)
   * @return Call&lt;DVCResponse&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/track")
  Call<DVCResponse> track(@Body UserAndEvents userAndEvents);
}
