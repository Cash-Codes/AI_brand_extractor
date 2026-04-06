package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record FlyerEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height,
        long sizeBytes,
        List<String> dominantColors,  // normalised hex values, ordered by frequency
        double confidence,
        Instant extractedAt) implements Evidence {}
