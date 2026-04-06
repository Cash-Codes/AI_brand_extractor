package com.brandextractor.infrastructure.web.dto.evidence;

public record LinkEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String href,
        String rel,
        String context) implements EvidenceDto {}
