package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.interceptor.AuthorizationHeaderInterceptor;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;

public final class DVCLocalEventsApiClient {

    private final OkHttpClient.Builder okBuilder;
    private final Retrofit.Builder adapterBuilder;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EVENTS_API_URL = "https://events.devcycle.com/";

    private String eventsApiUrl;

    private DVCLocalEventsApiClient(DVCLocalOptions options) {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        okBuilder = new OkHttpClient.Builder();

        String eventsApiUrlFromOptions = options.getEventsApiBaseUrl();

        eventsApiUrl = checkIfStringNullOrEmpty(eventsApiUrlFromOptions) ? EVENTS_API_URL : eventsApiUrlFromOptions;
        eventsApiUrl = eventsApiUrl.endsWith("/") ? eventsApiUrl : eventsApiUrl + "/";

        adapterBuilder = new Retrofit.Builder()
                .baseUrl(eventsApiUrl)
                .addConverterFactory(JacksonConverterFactory.create());
    }

    public DVCLocalEventsApiClient(String sdkKey, DVCLocalOptions options) {
        this(options);
        okBuilder.addInterceptor(new AuthorizationHeaderInterceptor(sdkKey));
    }

    public IDVCApi initialize() {
        return adapterBuilder
                .client(okBuilder.build())
                .build()
                .create(IDVCApi.class);
    }

    private Boolean checkIfStringNullOrEmpty(String stringToCheck) {
        return Objects.isNull(stringToCheck) || Objects.equals(stringToCheck, "");
    }
}
