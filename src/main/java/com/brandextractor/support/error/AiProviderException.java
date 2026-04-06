package com.brandextractor.support.error;

/**
 * Thrown when an upstream AI provider (e.g. Vertex AI Gemini) fails.
 * Maps to HTTP 502 Bad Gateway.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
