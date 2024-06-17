package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.common.interceptor.CustomHeaderInterceptor;
import okhttp3.OkHttpClient;

public class APIUtils {
    public static void applyRestOptions(IRestOptions restOptions, OkHttpClient.Builder builder) {
        if (restOptions != null) {
            if (restOptions.getHostnameVerifier() != null) {
                builder.hostnameVerifier(restOptions.getHostnameVerifier());
            }

            if (restOptions.getSocketFactory() != null && restOptions.getTrustManager() != null) {
                builder.sslSocketFactory(restOptions.getSocketFactory(), restOptions.getTrustManager());
            }
            builder.addInterceptor(new CustomHeaderInterceptor(restOptions));
        }
    }
}
