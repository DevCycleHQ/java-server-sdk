package com.devcycle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MockServer {

    private HttpServer server;
    static class ConfigHandler implements HttpHandler {
        String configData = "";
        ConfigHandler(String config) {
            configData = config;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(200, configData.length());
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(configData.getBytes());
            outputStream.flush();
            outputStream.close();
            exchange.close();
        }
    }

    static class EventHandler implements HttpHandler {
        private static final String EVENT_RESPONSE = "{\"message\":\"success\"}";
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // read in the body data
            new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines();
            exchange.sendResponseHeaders(201, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(EVENT_RESPONSE.getBytes());
            outputStream.flush();
            outputStream.close();
            exchange.close();
        }
    }
    public MockServer() throws IOException{
        String configData = "";

        String fileName = "fixture_large_config.json";
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                configData += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/config/", new ConfigHandler(configData));
        server.createContext("/event/", new EventHandler());
        server.setExecutor(null); // use the default executor
        System.out.println("Starting mock server on port 8000");
        server.start();
        Thread shutdownHook = new Thread(() -> server.stop(0));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void close() {
        this.server.stop(0);
    }
}
