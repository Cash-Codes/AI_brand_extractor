package com.brandextractor.infrastructure.web.dto.evidence;

import java.util.List;

public record FlyerEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height,
        long sizeBytes,
        List<String> dominantColors) implements EvidenceDto {}
