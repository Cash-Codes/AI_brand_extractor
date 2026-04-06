package com.brandextractor.domain.model;

public record Confident<T>(T value, double confidence) {
    public Confident {
        if (confidence < 0.0 || confidence > 1.0)
            throw new IllegalArgumentException("confidence must be in [0.0, 1.0], was: " + confidence);
    }
}
