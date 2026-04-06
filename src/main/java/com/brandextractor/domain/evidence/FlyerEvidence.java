package com.brandextractor.domain.evidence;

import java.time.Instant;

public record FlyerEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height,
        long sizeBytes,
        double confidence,
        Instant extractedAt) implements Evidence {}
