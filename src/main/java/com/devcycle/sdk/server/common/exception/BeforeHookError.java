package com.devcycle.sdk.server.common.exception;

/**
 * Exception thrown when a before hook fails during variable evaluation.
 */
public class BeforeHookError extends RuntimeException {
    public BeforeHookError(String message) {
        super(message);
    }

    public BeforeHookError(String message, Throwable cause) {
        super(message, cause);
    }
} 