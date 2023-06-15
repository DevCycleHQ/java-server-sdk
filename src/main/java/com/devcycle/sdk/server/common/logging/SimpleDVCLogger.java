package com.devcycle.sdk.server.common.logging;

/**
 * Basic implementation of IDVCLogger that logs to stdout with some basic log level filtering.
 */
public class SimpleDVCLogger implements IDVCLogger {
    public enum Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        OFF,
    }
    private Level level;
    public SimpleDVCLogger(Level level) {
        this.level = level;
    }

    @Override
    public void debug(String message) {
        if(this.level.ordinal() == Level.DEBUG.ordinal())
        {
            System.out.println(message);
        }
    }

    @Override
    public void info(String message) {
        if(this.level.ordinal() <= Level.INFO.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void warning(String message) {
        if(this.level.ordinal() <= Level.WARNING.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void error(String message) {
        if(this.level.ordinal() <= Level.ERROR.ordinal()) {
            System.out.println(message);
        }
    }

    @Override
    public void error(String message, Throwable t) {
        if(this.level.ordinal() <= Level.ERROR.ordinal()) {
            System.out.println(message);
        }
    }
}
