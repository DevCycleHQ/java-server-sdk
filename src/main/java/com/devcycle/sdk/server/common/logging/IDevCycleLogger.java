package com.devcycle.sdk.server.common.logging;

/**
 * A simple interface for logging inside the SDK. Implement this interface and pass it to the SDK to override the
 * default behavior. Use this interface to integrate with an existing logging framework such as Java Logging, Log4j or SLF4J
 */
public interface IDevCycleLogger {
    void debug(String message);
    void info(String message);
    void warning(String message);
    void error(String message);
    void error(String message, Throwable t);
}
