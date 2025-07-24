package com.devcycle.sdk.server.local.api;

import com.devcycle.sdk.server.common.api.APIUtils;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.api.ObjectMapperUtils;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class DevCycleLocalApiClient {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtils.createDefaultObjectMapper();
    private static final String CONFIG_URL = "https://config-cdn.devcycle.com/";
    private static final int DEFAULT_TIMEOUT_MS = 10000;
    private static final int MIN_INTERVALS_MS = 1000;
    private final OkHttpClient.Builder okBuilder;
    private final Retrofit.Builder adapterBuilder;
    private String configUrl;
    private final int requestTimeoutMs;

    private DevCycleLocalApiClient(DevCycleLocalOptions options) {

        okBuilder = new OkHttpClient.Builder();

        APIUtils.applyRestOptions(options.getRestOptions(), okBuilder);

        String cdnUrlFromOptions = options.getConfigCdnBaseUrl();
        int configRequestTimeoutMs = options.getConfigRequestTimeoutMs();

        configUrl = checkIfStringNullOrEmpty(cdnUrlFromOptions) ? CONFIG_URL : cdnUrlFromOptions;
        requestTimeoutMs = configRequestTimeoutMs >= MIN_INTERVALS_MS ? configRequestTimeoutMs : DEFAULT_TIMEOUT_MS;

        okBuilder.callTimeout(this.requestTimeoutMs, TimeUnit.MILLISECONDS);

        configUrl = configUrl.endsWith("/") ? configUrl : configUrl + "/";

        adapterBuilder = new Retrofit.Builder()
                .baseUrl(configUrl)
                .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER));
    }

    public DevCycleLocalApiClient(String sdkKey, DevCycleLocalOptions options) {
        this(options);
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
