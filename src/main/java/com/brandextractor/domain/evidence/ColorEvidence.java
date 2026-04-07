package com.brandextractor.domain.evidence;

import java.time.Instant;

public record ColorEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String hexValue,
        double frequency,
        double confidence,
        Instant extractedAt) implements Evidence {}
