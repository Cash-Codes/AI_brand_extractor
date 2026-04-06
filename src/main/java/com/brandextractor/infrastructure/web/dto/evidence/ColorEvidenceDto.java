package com.brandextractor.infrastructure.web.dto.evidence;

public record ColorEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String hexValue,
        double frequency) implements EvidenceDto {}
