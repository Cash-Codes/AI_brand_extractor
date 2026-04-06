package com.brandextractor.infrastructure.web.dto.evidence;

// imageBytes intentionally omitted — metadata only
public record ScreenshotEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String mimeType,
        int width,
        int height) implements EvidenceDto {}
