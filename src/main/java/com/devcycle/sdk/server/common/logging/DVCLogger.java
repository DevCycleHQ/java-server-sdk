package com.devcycle.sdk.server.common.logging;
/**
 * DVCLogger is a simple central entrypoint for the SDK to log messages without pinning the SDK to a
 * specific logging framework. By default it logs to stdout but can e overriden by calling setCustomLogger()
 */
public class DVCLogger {
    private static IDVCLogger logger = new SimpleDVCLogger(SimpleDVCLogger.Level.INFO);
    public static void setCustomLogger(IDVCLogger logger) {
        DVCLogger.logger = logger;
    }

    public static void debug(String message) {
        if(logger != null){
            logger.debug(message);
        }
    }

    public static void info(String message) {
        if(logger != null) {
            logger.info(message);
        }
    }

    public static void warning(String message) {
        if(logger != null) {
            logger.warning(message);
        }
    }

    public static void error(String message) {
            if(logger != null) {
                logger.error(message);
            }
    }

    public static void error(String message, Throwable t) {
            if(logger != null) {
                logger.error(message, t);
            }
    }
}

