package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.api.APIUtils;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DevCycleLocalEventsApiClient {

    private final OkHttpClient.Builder okBuilder;
    private final Retrofit.Builder adapterBuilder;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EVENTS_API_URL = "https://events.devcycle.com/";

    private String eventsApiUrl;

    public DevCycleLocalEventsApiClient(String sdkKey, DevCycleLocalOptions options) {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        okBuilder = new OkHttpClient.Builder();
        okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(sdkKey));
        APIUtils.applyRestOptions(options.getRestOptions(), okBuilder);

        String eventsApiUrlFromOptions = options.getEventsApiBaseUrl();

        eventsApiUrl = checkIfStringNullOrEmpty(eventsApiUrlFromOptions) ? EVENTS_API_URL : eventsApiUrlFromOptions;
        eventsApiUrl = eventsApiUrl.endsWith("/") ? eventsApiUrl : eventsApiUrl + "/";

        adapterBuilder = new Retrofit.Builder()
                .baseUrl(eventsApiUrl)
                .addConverterFactory(JacksonConverterFactory.create());


    }

    public IDevCycleApi initialize() {
        return adapterBuilder
                .client(okBuilder.build())
                .build()
                .create(IDevCycleApi.class);
    }

    private Boolean checkIfStringNullOrEmpty(String stringToCheck) {
        return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
    }
}
