package com.brandextractor.domain.evidence;

public record FlyerEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height,
        long sizeBytes) implements Evidence {}
