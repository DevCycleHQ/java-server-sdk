package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.model.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface DVCApi {
  /**
   * Get all features by key for user data
   *
   * @param user  (required)
   * @param enableEdgeDB  (required)
   * @return Call&lt;Map&lt;String, Feature&gt;&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/features")
  Call<Map<String, Feature>> getFeatures(@Body User user, @Query("enableEdgeDB") Boolean enableEdgeDB);

  /**
   * Get variable by key for user data
   *
   * @param user  (required)
   * @param key Variable key (required)
   * @param enableEdgeDB  (required)
   * @return Call&lt;Variable&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/variables/{key}")
  Call<Variable> getVariableByKey(@Body User user, @Path("key") String key, @Query("enableEdgeDB") Boolean enableEdgeDB);

  /**
   * Get all variables by key for user data
   *
   * @param user  (required)
   * @param enableEdgeDB  (required)
   * @return Call&lt;Map&lt;String, Variable&gt;&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/variables")
  Call<Map<String, Variable>> getVariables(@Body User user, @Query("enableEdgeDB") Boolean enableEdgeDB);

  /**
   * Post events to DevCycle for user
   *
   * @param userAndEvents  (required)
   * @param enableEdgeDB  (required)
   * @return Call&lt;DVCResponse&gt;
   */
  @Headers({"Content-Type:application/json"})
  @POST("v1/track")
  Call<DVCResponse> track(@Body UserAndEvents userAndEvents, @Query("enableEdgeDB") Boolean enableEdgeDB);

  /**
   * Get DevCycle project Config
   *
   * @param sdkToken  (required)
   * @return Call&lt;ProjectConfig&gt;
   */
  @Headers({"Content-Type:application/json"})
  @GET("config/v1/server/{sdkToken}.json")
  Call<ProjectConfig> getConfig(@Path("sdkToken") String sdkToken);
}
