package com.devcycle.sdk.server.helpers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

public class LocalConfigServer {
    private HttpServer server;
    private String configData = "";
    public LocalConfigServer(String configData) throws IOException{
        this.configData = configData;
        InetSocketAddress address = new InetSocketAddress(8000);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::handleConfigRequest);
        server.setExecutor(null); // use the default executor
        System.out.println("Starting config server on " + address);
    }

    public void handleConfigRequest(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, configData.length());
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(configData.getBytes());
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