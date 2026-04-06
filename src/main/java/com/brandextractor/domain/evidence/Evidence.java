package com.brandextractor.domain.evidence;

import java.time.Instant;

public sealed interface Evidence
        permits WebsiteEvidence, FlyerEvidence, OcrEvidence,
                VisualEvidence, ColorEvidence, LinkEvidence,
                TextEvidence, ScreenshotEvidence {

    // --- identity & provenance ---
    String id();
    String sourceType();
    String sourceReference();
    Instant extractedAt();

    // --- extractor confidence in this evidence item (0.0–1.0) ---
    double confidence();
}
