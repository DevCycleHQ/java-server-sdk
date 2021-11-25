package com.devcycle.sdk.server.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public final class AuthorizationHeaderInterceptor implements Interceptor {

    private final static String AUTHORIZATION_HEADER = "Authorization";

    private final String apiKey;

    public AuthorizationHeaderInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        request = request.newBuilder()
                .addHeader(AUTHORIZATION_HEADER, apiKey)
                .build();

        return chain.proceed(request);
    }
}
