package com.devcycle.sdk.server.common.interceptor;

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

        String headerName = AUTHORIZATION_HEADER;

        // Is there already an authorization header on the request?
        // If so, we need to rename ours to avoid conflicts
        if(request.header(AUTHORIZATION_HEADER) != null) {
            headerName = "DevCycle-" + AUTHORIZATION_HEADER;
        }

        request = request.newBuilder()
                .addHeader(headerName, apiKey)
                .build();

        return chain.proceed(request);
    }
}
