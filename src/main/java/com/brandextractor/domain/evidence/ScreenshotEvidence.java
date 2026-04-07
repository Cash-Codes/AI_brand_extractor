package com.brandextractor.domain.evidence;

import java.time.Instant;

// Note: imageBytes is intentionally excluded from ScreenshotEvidenceDto (API serialisation).
public record ScreenshotEvidence(
        String id,
        String sourceType,
        String sourceReference,
        byte[] imageBytes,
        String mimeType,
        int width,
        int height,
        double confidence,
        Instant extractedAt) implements Evidence {}
