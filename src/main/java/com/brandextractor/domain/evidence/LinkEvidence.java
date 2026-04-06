package com.brandextractor.domain.evidence;

import java.time.Instant;

public record LinkEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String href,
        String rel,
        String context,
        double confidence,
        Instant extractedAt) implements Evidence {}
