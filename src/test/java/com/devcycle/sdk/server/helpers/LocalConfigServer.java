package com.devcycle.sdk.server.helpers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LocalConfigServer {
    private final HttpServer server;
    private String configData = "";
    private String etag = "\"test-etag-12345\"";
    private int responseCode = 200;

    public LocalConfigServer(String configData, int port) throws IOException {
        this.configData = configData;
        InetSocketAddress address = new InetSocketAddress(port);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::handleConfigRequest);
        server.setExecutor(null); // use the default executor
        System.out.println("Starting config server on " + address);
    }

    public String getHostRootURL() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    public void handleConfigRequest(HttpExchange exchange) throws IOException {
        if (responseCode != 200) {
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
            return;
        }

        // Add required headers for ConfigMetadata creation
        String currentTime = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        exchange.getResponseHeaders().set("ETag", etag);
        exchange.getResponseHeaders().set("Last-Modified", currentTime);

        byte[] responseData = configData.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseData.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseData);
        outputStream.flush();
        outputStream.close();
    }

    public void setConfigData(String configData) {
        this.configData = configData;
    }

    /**
     * Change the ETag served with the config, so a subsequent poll is seen as a new config.
     */
    public void setETag(String etag) {
        this.etag = etag;
    }

    /**
     * Serve the given status code with an empty body instead of the config.
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void start() {
        this.server.start();
    }

    public void stop() {
        this.server.stop(0);
    }
}