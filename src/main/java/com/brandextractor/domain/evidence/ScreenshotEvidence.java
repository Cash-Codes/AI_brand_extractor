package com.brandextractor.domain.evidence;

public record ScreenshotEvidence(
        String id,
        String sourceType,
        String sourceReference,
        byte[] imageBytes,
        String mimeType,
        int width,
        int height) implements Evidence {}
