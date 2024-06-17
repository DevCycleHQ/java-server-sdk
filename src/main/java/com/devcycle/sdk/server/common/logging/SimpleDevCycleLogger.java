package com.devcycle.sdk.server.common.logging;

/**
 * Basic implementation of IDevCycleLogger that logs to stdout with some basic log level filtering.
 */
public class SimpleDevCycleLogger implements IDevCycleLogger {
    private final Level level;

    public SimpleDevCycleLogger(Level level) {
        this.level = level;
    }

    @Override
    public void debug(String message) {
        if (this.level.ordinal() == Level.DEBUG.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void info(String message) {
        if (this.level.ordinal() <= Level.INFO.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void warning(String message) {
        if (this.level.ordinal() <= Level.WARNING.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void error(String message) {
        if (this.level.ordinal() <= Level.ERROR.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void error(String message, Throwable t) {
        if (this.level.ordinal() <= Level.ERROR.ordinal()) {
            System.out.println(message);
        }
    }

    public enum Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        OFF,
    }
}
