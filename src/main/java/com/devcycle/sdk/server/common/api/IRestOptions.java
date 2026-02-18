package com.devcycle.sdk.server.common.api;

import okhttp3.Authenticator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Map;

/**
 * An interface for customizing how the DevCycle SDK makes external requests, allowing for the
 * injection of custom headers or SSL configuration.
 */
public interface IRestOptions {
    /**
     * @return A set of HTTP request headers that should be incorporated into all outgoing requests. return null if no headers are needed.
     */
    Map<String, String> getHeaders();

    /**
     * @return A custom SSLSocketFactory to use when making requests. Return null if the default SSLSocket factory can be used
     */
    SSLSocketFactory getSocketFactory();

    /**
     * @return Provide a trust manager to handle custom certificates. Return null if the default trust manager can be used
     */
    X509TrustManager getTrustManager();

    /**
     * @return A custom HostnameVerifier to use when making requests. Return null if the default HostnameVerifier can be used
     */
    HostnameVerifier getHostnameVerifier();

    /**
     * @return a Proxy to use when making requests. Return null if the default Proxy selector can be used
     */
    default Proxy getProxy() {
        return null;
    }

    /**
     * @return a ProxySelector to use when making requests. Return null if the default Proxy selector can be used.
     */
    default ProxySelector getProxySelector() {
        return null;
    }

    /**
     * @return an Authenticator to use when making requests. Return null if the default Authenticator can be used.
     */
    default Authenticator getProxyAuthenticator(){
        return null;
    }
}
