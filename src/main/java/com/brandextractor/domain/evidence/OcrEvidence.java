package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record OcrEvidence(
        String id,
        String sourceType,
        String sourceReference,
        List<TextBlock> blocks,   // text regions with bounding-box metadata
        double confidence,
        Instant extractedAt) implements Evidence {}
