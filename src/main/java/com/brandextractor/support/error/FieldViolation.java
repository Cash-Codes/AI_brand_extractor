package com.brandextractor.support.error;

/**
 * A single field-level constraint violation included in 400 error responses.
 */
public record FieldViolation(
        String field,
        String message,
        String rejectedValue) {}
