package com.devcycle.sdk.server.common.interceptor;

import com.devcycle.sdk.server.common.api.IRestOptions;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Interceptor to inject custom headers into all requests based on an IRestOptions
 * implementation
 */
public final class CustomHeaderInterceptor implements Interceptor {
    private final IRestOptions restOptions;

    public CustomHeaderInterceptor(IRestOptions restOptions) {
        this.restOptions = restOptions;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        if (restOptions != null && chain.request().url().url().getHost().contains("devcycle")) {
            Request.Builder builder = request.newBuilder();

            for (Map.Entry<String, String> entry : restOptions.getHeaders().entrySet()) {
                if (entry.getValue() != null) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            request = builder.build();
        }
        return chain.proceed(request);
    }
}
