package com.devcycle.sdk.server.common.api;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.Map;

public interface IRestOptions {
    Map<String,String> getHeaders();

    SSLSocketFactory getSocketFactory();

    X509TrustManager getTrustManager();
}
