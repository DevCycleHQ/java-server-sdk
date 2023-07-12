package com.devcycle.sdk.server.helpers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class LocalConfigServer {
    private HttpServer server;
    private String configData = "";
    public LocalConfigServer(String configData, int port) throws IOException{
        this.configData = configData;
        InetSocketAddress address = new InetSocketAddress(port);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::handleConfigRequest);
        server.setExecutor(null); // use the default executor
        System.out.println("Starting config server on " + address);
    }

    public String getHostRootURL() {
        return "http://localhost:" + server.getAddress().getPort() +"/";
    }

    public void handleConfigRequest(HttpExchange exchange) throws IOException {
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

    public void start() {
        this.server.start();
    }

    public void stop() {
        this.server.stop(0);
    }
}