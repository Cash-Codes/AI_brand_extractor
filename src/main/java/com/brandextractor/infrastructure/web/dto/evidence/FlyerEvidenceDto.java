package com.brandextractor.infrastructure.web.dto.evidence;

public record FlyerEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height,
        long sizeBytes) implements EvidenceDto {}
