package com.devcycle.sdk.server.common.api;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.Map;

/**
 * An interface for customizing how the DevCycle SDK makes external requests, allowing for the
 * injection of custom headers or SSL configuration.
 */
public interface IRestOptions {
    /**
     * @return A set of HTTP request headers that should be incorporated into all outgoing requests. A null map and
     * null values will be ignored.
     */
    Map<String,String> getHeaders();

    /**
     * @return Optional. A custom SSLSocketFactory to use when making requests. Return null if the default SSLSocket factory can be used
     */
    SSLSocketFactory getSocketFactory();

    /**
     *
     * @return Optional. Provide a trust manager to handle custom certificates. Return null if the default trust manager can be used
     */
    X509TrustManager getTrustManager();

    /**
     * @return Optional. A custom HostnameVerifier to use when making requests. Return null if the default HostnameVerifier can be used
     */
    HostnameVerifier getHostnameVerifier();
}
