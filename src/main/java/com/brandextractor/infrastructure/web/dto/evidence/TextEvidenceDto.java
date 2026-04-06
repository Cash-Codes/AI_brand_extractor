package com.brandextractor.infrastructure.web.dto.evidence;

public record TextEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String text,
        String textType) implements EvidenceDto {}
