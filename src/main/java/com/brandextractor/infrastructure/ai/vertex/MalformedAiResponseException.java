package com.brandextractor.infrastructure.ai.vertex;

/**
 * Thrown by {@link VertexAiResponseParser} when the model's JSON output fails
 * schema validation. Caught by {@link VertexAiExtractionClient} to trigger a retry.
 */
class MalformedAiResponseException extends RuntimeException {

    MalformedAiResponseException(String message) {
        super(message);
    }

    MalformedAiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
