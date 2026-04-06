package com.brandextractor.support.error;

/**
 * Thrown by stub implementations that are intentionally not yet built.
 * Maps to HTTP 501 Not Implemented.
 */
public class FeatureNotAvailableException extends RuntimeException {

    public FeatureNotAvailableException(String message) {
        super(message);
    }
}
