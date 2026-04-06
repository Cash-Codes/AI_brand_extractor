package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record OcrEvidence(
        String id,
        String sourceType,
        String sourceReference,
        List<String> textBlocks,
        double confidence,
        Instant extractedAt) implements Evidence {}
