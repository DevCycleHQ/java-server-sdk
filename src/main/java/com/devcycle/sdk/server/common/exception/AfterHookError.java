package com.devcycle.sdk.server.common.exception;

/**
 * Exception thrown when an after hook fails during variable evaluation.
 */
public class AfterHookError extends RuntimeException {
    public AfterHookError(String message) {
        super(message);
    }

    public AfterHookError(String message, Throwable cause) {
        super(message, cause);
    }
} 