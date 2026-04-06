package com.brandextractor.domain.evidence;

import java.time.Instant;

public record TextEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String text,
        String textType,
        double confidence,
        Instant extractedAt) implements Evidence {}
