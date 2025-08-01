package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.api.APIUtils;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.api.ObjectMapperUtils;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DevCycleLocalEventsApiClient {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtils.createEventObjectMapper();
    private static final String EVENTS_API_URL = "https://events.devcycle.com/";
    private final OkHttpClient.Builder okBuilder;
    private final Retrofit.Builder adapterBuilder;
    private String eventsApiUrl;

    public DevCycleLocalEventsApiClient(String sdkKey, DevCycleLocalOptions options) {
        okBuilder = new OkHttpClient.Builder();

        APIUtils.applyRestOptions(options.getRestOptions(), okBuilder);

        okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(sdkKey));

        String eventsApiUrlFromOptions = options.getEventsApiBaseUrl();

        eventsApiUrl = checkIfStringNullOrEmpty(eventsApiUrlFromOptions) ? EVENTS_API_URL : eventsApiUrlFromOptions;
        eventsApiUrl = eventsApiUrl.endsWith("/") ? eventsApiUrl : eventsApiUrl + "/";

        adapterBuilder = new Retrofit.Builder()
                .baseUrl(eventsApiUrl)
                .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER));
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
