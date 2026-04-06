package com.brandextractor.domain.evidence;

import java.util.List;

public record OcrEvidence(
        String id,
        String sourceType,
        String sourceReference,
        List<String> textBlocks) implements Evidence {}
