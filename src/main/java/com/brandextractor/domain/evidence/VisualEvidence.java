package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record VisualEvidence(
        String id,
        String sourceType,
        String sourceReference,
        List<String> detectedLabels,
        String dominantTheme,
        double confidence,
        Instant extractedAt) implements Evidence {}
